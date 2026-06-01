# Fin-Tech 결제 시스템 SRE 관점의 JVM Context Switching Overhead 장애 분석 가이드

> 정독: 0회

## 목차

1. [개요](#1-개요)
2. [Context Switching의 기본 구조 및 유형](#2-context-switching의-기본-구조-및-유형)
3. [하부 인프라 및 OS 커널 계층의 연동 기전](#3-하부-인프라-및-os-커널-계층의-연동-기전)
4. [JVM 내부 서브시스템별 Context Switching Overhead 유발 역학](#4-jvm-내부-서브시스템별-context-switching-overhead-유발-역학)
5. [기술 발전 및 핀테크 아키텍처 채택 동향 (2026년 기준)](#5-기술-발전-및-핀테크-아키텍처-채택-동향-2026년-기준)
6. [관측성(Observability) 및 트러블슈팅 런북](#6-관측성observability-및-트러블슈팅-런북)
7. [2026 Production Baseline JVM Runtime Manifest](#7-2026-production-baseline-jvm-runtime-manifest)
8. [운영 원칙](#8-운영-원칙)

---

## 1. 개요

**Context Switching(문맥 전환)** 은 CPU가 현재 실행 중인 스레드의 실행 상태를 저장하고, 다른 스레드의 실행 상태를 복원하여 CPU 제어권을 전환하는 과정이다.

운영체제는 멀티태스킹을 위해 지속적으로 Context Switching을 수행한다. 그러나 과도한 Context Switching은 실제 비즈니스 로직 처리보다 **스케줄링 비용이 더 많은 CPU 사이클을 소모**하여 전체 시스템 성능을 파괴하는 장애 유형으로 전이된다.

Context Switching Overhead는 JVM뿐 아니라 **Linux Scheduler → CPU Cache → Memory Subsystem** 계층 전반에 걸쳐 연쇄 영향을 미친다.

### 대표 증상

| 증상 | 설명 |
|------|------|
| CPU 사용률 증가 | 실제 연산이 아닌 스케줄링 비용으로 CPU 소모 |
| TPS 감소 | 유효 처리 사이클 감소 |
| 응답시간 증가 | 스레드 재개 대기 지연 누적 |
| Tail Latency 증가 | P99.9 구간에서 스케줄링 지연 집중 |
| 시스템 처리 효율 감소 | CPU Saturation 상태에서 처리량 정체 |

---

## 2. Context Switching의 기본 구조 및 유형

### 전환 시 저장 및 복원되는 정보

```
Thread A 실행
      │
      ▼
CPU 상태 저장 (Program Counter, Stack Pointer, CPU Registers, Processor State)
      │
      ▼
Thread B 상태 복원
      │
      ▼
Thread B 실행
```

운영체제는 스레드 전환 시마다 다음 정보를 저장하고 복원한다.

| 저장 항목 | 설명 |
|---------|------|
| Program Counter | 다음 실행할 명령어 주소 |
| Stack Pointer | 현재 스택 프레임 위치 |
| CPU Registers | 범용 레지스터 세트 전체 |
| Processor State | 상태 레지스터 및 플래그 |
| Scheduling Metadata | 커널 스케줄러 관리 메타데이터 |

### Voluntary vs Involuntary Context Switching

| 유형 | 정의 | 대표 원인 | 진단 지표 |
|------|------|---------|---------|
| **Voluntary CS** | 스레드가 스스로 CPU 제어권을 반납 | Socket Read, DB Query, Lock Wait, `Object.wait()` | `voluntary_context_switches` |
| **Involuntary CS** | 커널 스케줄러가 강제로 CPU 제어권 회수 | CPU Saturation, Time Slice 만료, 우선순위 높은 IRQ 처리 | `involuntary_context_switches` |

**SRE 임계 징후**: `involuntary_context_switches`의 폭증은 비즈니스 로직 비효율이 아닌 **시스템이 물리적 한계(CPU Saturation)에 도달했음**을 의미한다. 전체 CPU 사용량(`%user + %sys`)이 90%를 초과하는 상황에서 비자발적 교체 건수가 초당 수십만 건을 돌파하면, CPU 사이클의 30% 이상이 실제 결제 로직이 아닌 **커널 스케줄링 행정 비용으로 전소**되는 System Stall 상태로 귀결된다.

---

## 3. 하부 인프라 및 OS 커널 계층의 연동 기전

### 3-1. 리눅스 CFS(Completely Fair Scheduler)와 CPU Runqueue 가동 기전

**vruntime 기반 스케줄링**

리눅스 커널의 CFS는 각 태스크(LWP/스레드)의 가상 실행 시간인 `vruntime`을 추적하여 **가중치가 부여된 레드-블랙 트리(Red-Black Tree)** 구조의 CPU별 Runqueue에서 스레드를 관리한다.

```
CPU Core 0 Runqueue         CPU Core 1 Runqueue
┌─────────────────┐         ┌─────────────────┐
│  Red-Black Tree │         │  Red-Black Tree │
│  (vruntime 정렬) │         │  (vruntime 정렬) │
│                 │         │                 │
│  Thread A ──┐  │         │  Thread D ──┐  │
│  Thread B   │  │         │  Thread E   │  │
│  Thread C   │  │         │  Thread F   │  │
└─────────────┼──┘         └─────────────┼──┘
              │ 실행 중                   │ 실행 중
              ▼                           ▼
           CPU Core 0                  CPU Core 1
```

**스케줄링 타임슬라이스 붕괴**

JVM 내 플랫폼 스레드가 급증하여 CPU 코어 수 대비 실행 가능 스레드 수가 임계점을 넘어서면, 커널 스케줄러가 각 스레드에 배정하는 최소 실행 시간 단위인 `min_granularity_ns` 장벽이 무너진다. 스레드가 유효한 비즈니스 연산을 완결하기도 전에 타임슬라이스가 만료되어 런큐에서 축출되는 **고빈도 강제 교체 파이프라인**이 형성된다.

| 이상적인 구조 | 과도한 스레드 상황 |
|------------|---------------|
| CPU Core = 8, Runnable Thread ≈ 8 | CPU Core = 8, Runnable Thread = 500 |
| 각 스레드 충분한 타임슬라이스 보장 | Context Switching 기하급수적 증가 |
| CPU Cache 효율 유지 | Run Queue 포화 및 Cache 무효화 반복 |

### 3-2. 가상 메모리 주소 공간(VMA) 전환과 TLB 플러시

**하드웨어 캐시 무효화**

스레드가 전환될 때마다 CPU 레지스터 세트가 현재 스레드의 커널 스택에 저장(Save)되고 차기 스레드의 아키텍처 컨텍스트가 하드웨어로 복원(Restore)된다.

**TLB 멸실 오버헤드**

스레드가 다른 CPU 코어로 마이그레이션되거나 커널 sys 모드 시스템 콜 처리를 위해 물리적 주소 변환 캐시인 **TLB(Translation Lookaside Buffer)가 플러시 및 무효화**되면, 가상 주소를 물리 주소로 매핑하기 위해 커널 메모리의 다단계 페이지 테이블을 직접 쿼리하는 **MMU 워킹(Page Table Walking) 레이턴시**가 중첩된다.

```
Context Switch 발생
        │
        ▼
TLB 무효화 (Flush)
        │
        ▼
다음 메모리 접근 시 TLB Miss 발생
        │
        ▼
MMU가 Page Table Walking 수행
  (물리 주소를 직접 다단계 테이블에서 조회)
        │
        ▼
조회 완료 후 TLB 재적재
  → 고빈도 전환 시 이 사이클이 끝없이 반복됨
```

**CPU Cache 영향**: 스레드 교체 시 이전 스레드가 사용하던 L1/L2/L3 Cache 데이터가 무효화되며 Cache Miss가 증가하여 메모리 접근 레이턴시가 상승한다.

---

## 4. JVM 내부 서브시스템별 Context Switching Overhead 유발 역학

### 4-1. 플랫폼 스레드 폭증과 스택 메모리의 물리적 반비례 관계

**LWP 1:1 매핑 오버헤드**

자바의 표준 플랫폼 스레드는 생성될 때마다 리눅스 커널의 `pthread_create` 시스템 콜을 거쳐 독립적인 LWP를 주조한다. JVM 기동 플래그 `-Xss` 옵션은 각 스레드가 독점하는 **네이티브 스택 메모리 크기(디폴트 1MB)** 를 규정한다.

```java
// 처리량 향상 목적의 과도한 스레드 풀 설정 예시
Executors.newFixedThreadPool(1000);
// → 네이티브 스택 메모리 최소 1GB 점유
// → 커널 스케줄러 관리 대상 LWP 1,000개
// → Involuntary Context Switching 기하급수적 증가
```

**컨텍스트 밀도 포화**: 톰캣 워커 스레드나 ForkJoinPool 상한선을 수천 개 단위로 증설하면, 힙 외곽의 네이티브 RSS 풋프린트가 임계 청산될 뿐만 아니라 커널 스케줄러가 관리해야 할 대상 객체의 밀도가 포화되어 **Context Switching 카운터가 기하급수적 포물선**을 그리며 상승한다.

### 4-2. Lock Contention과 Futex 대기의 Context Switching 연쇄

```
synchronized(lock) {
    // 락 획득 실패 시 반복되는 사이클
}
       │
       ▼
  락 획득 실패
       │
       ▼
  BLOCKED / WAITING 상태 전이
  → futex(WAIT) 시스템 콜 호출
       │
       ▼
  커널 Wait Queue 대기
  → Voluntary Context Switch 발생
       │
       ▼
  락 해제 → futex(WAKE) 신호
  → 커널 Run Queue 복귀
  → Voluntary Context Switch 재발생
       │
       └── [고빈도 락 경합 시 이 사이클이 무한 반복됨]
```

### 4-3. 가상 스레드(Virtual Threads) 환경의 신종 고갈 리스크

**가상 스레드의 Context Switching 혁신**

JDK 21+ 환경의 가상 스레드는 유저 스페이스 내 자바 힙 영역에 **Continuation 객체** 형태로 존재한다. 가상 스레드 간 컨텍스트 스위칭은 커널 시스템 콜 없이 자바 런타임 자체 연산만으로 종결되어 **오버헤드가 제로(0)에 수렴**한다.

```
플랫폼 스레드 전환                    가상 스레드 전환
─────────────────────               ─────────────────────
커널 모드 진입                        유저 스페이스 내 처리
CPU 레지스터 저장/복원                  Continuation 객체 주소 교체
TLB 플러시 가능성                      힙 내 객체 참조 변경만 수행
스케줄러 Run Queue 조작                JVM 내부 스케줄러만 관여
→ 수 마이크로초 오버헤드               → 수 나노초 오버헤드
```

**Thread Pinning으로 인한 물리 커널 CS의 역습**

가상 스레드 내부에서 레거시 `synchronized` 블록을 통과하는 도중 외부 금융 I/O 블로킹을 만나면, 가상 스레드가 하부 Carrier Thread에서 언마운트되지 못하고 잠기는 **Thread Pinning**이 발생한다.

```
정상 동작 (ReentrantLock 사용)         비정상 동작 (synchronized + I/O 블로킹)
────────────────────────────          ────────────────────────────────────
Virtual Thread → I/O 블로킹            Virtual Thread (Pinned)
       │                                      │
       ▼                                      ▼ ← Carrier Thread 점유 유지
Carrier Thread 언마운트                  Carrier Thread 1 고착
       │                                      │
       ▼                                 트래픽 폭증 시
다른 Virtual Thread 실행                 JVM이 백업 OS Thread 추가 생성
(OS Context Switch 없음)                      │
                                              ▼
                                    Involuntary Context Switching 폭증
                                    → 가상 스레드 도입 효과 완전 역전
```

### 4-4. Spin Lock 튜닝과 커널 전환의 임계 변곡점

JVM 내부의 Heavyweight Lock은 경합 발생 시 즉시 `futex(WAIT)`으로 가지 않고 잠시 CPU 코어를 태우며 대기하는 **Adaptive Spinning** 기전을 탑재하고 있다.

**최악의 중첩 시나리오**: 금융 트랜잭션의 원장 데이터 경합 시간이 스핀 임계 제한선(`-XX:OnSpinDuration`)보다 미세하게 길어질 경우, 다음 두 오버헤드가 한 트랜잭션 라인 내에서 **동시다발적으로 중첩** 발생한다.

```
Spin 구간 (OnSpinDuration 사이클)
  → CPU 코어 100% 전소 (물리 CPU 자원 낭비)
        │
        ▼ (스핀 실패)
futex(WAIT) 시스템 콜 호출
  → 커널 모드 강제 전이 (Context Switching 오버헤드)
        │
        ▼
[CPU 자원 전소] + [Context Switching 오버헤드] 동시 발생
= 단일 트랜잭션 처리 경로에서의 임계 파괴 현상
```

---

## 5. 기술 발전 및 핀테크 아키텍처 채택 동향 (2026년 기준)

### 5-1. 구세대 Thread-per-Request 모델의 물리적 한계

**Tomcat 8/9 계열 1요청 1스레드 바인딩 구조의 제약**

- 대외계 통신 지연 시 스레드가 `SOCKET_READ` 상태로 멈추며 Voluntary Context Switching 난발
- 처리량 방어를 위해 풀을 늘리면 Involuntary 스위칭 오버헤드가 발생
- 인스턴스당 처리량(TPS) 장벽이 수천 건 대에서 물리적으로 완전히 막히는 한계 노출

→ 현대 분산 결제 인프라에서 **전면 도태**

### 5-2. Reactive Architecture (WebFlux / Netty) Event Loop 모델 (중기~현재)

**커널 Context Switching의 혁신적 소거**

CPU 코어 수와 일치하는 극소수의 전용 Event Loop 스레드(Netty `EventLoopGroup`)만 기동하는 방식이다. 모든 네트워크 I/O를 리눅스 커널의 논블로킹 멀티플렉싱 인터페이스인 `epoll(7)` 시스템 콜로 처리하여 **스레드가 대기 상태로 전이되는 현상을 원천 차단**했다.

**핀테크 채택 장벽**

| 장점 | 장벽 |
|------|------|
| Context Switching 오버헤드 완전 제로화 | 함수형 리액티브 스트림 API(Mono/Flux) 재설계 러닝 커브 |
| 극소수 스레드로 고처리량 달성 | `ThreadLocal` 기반 금융 트레이싱 컨텍스트 매핑 파괴 |
| CPU 효율 극대화 | 동기 코드 패턴과의 이질적 혼재 |

→ 가용성 혁신을 이루었으나 코어 금융 시스템 전체로의 확산은 제한적

### 5-3. 가상 스레드(Virtual Threads) 표준 안착 (2026년 현재)

2026년 현재 핀테크 가상화 인프라는 **가독성과 초저자원 고성능을 양립**한 JDK 21+ 가상 스레드 아키텍처를 코어 백본으로 완전히 안착시켰다.

스레드 교체 연산이 유저 레벨 힙 공간의 가벼운 **Continuation 객체 주소 변환**으로 수렴되면서 커널 레벨의 가상 메모리 전환 오버헤드가 근본적으로 소거되었다.

| 기술 | 2026년 위상 |
|------|-----------|
| Thread-per-Request (Tomcat) | 도태 (고부하 시 CS 오버헤드 한계) |
| WebFlux / Netty Event Loop | 특수 목적 채택 (코어 금융 전면 확산 제한) |
| Virtual Threads (JDK 21+) | **표준 백본** (가독성 + 초저 CS 오버헤드 양립) |
| `synchronized` 중첩 | **전면 금지** (Pinning으로 인한 CS 역전 위험) |
| `ReentrantLock` / `StampedLock` | **전사 표준** (Carrier Thread 정상 언마운트 보장) |

---

## 6. 관측성(Observability) 및 트러블슈팅 런북

### 6-1. SRE 실시간 Context Switching 탐지 임계 지표 매트릭스

#### Linux / Node Exporter 핵심 수집 매트릭

| 수집 메트릭 식별자 | 감시 대상 | 크리티컬 알람 발령선 |
|------------------|-----------|---------------------|
| `node_context_switches_total` | 호스트 OS 커널 전체 초당 문맥 전환 총량 | **Critical**: 가용 CPU 코어 수 대비 초당 총 스위칭 건수 > 150,000 cs/sec 돌파 즉시 발령 |
| `process_cpu_seconds_total (sys)` | JVM 프로세스의 커널 모드 CPU 사이클 비율 | **Critical**: 전체 CPU 소모량 중 `sys` 비율 > 25% 연속 초과 시 System Stall 확정 |

#### JVM 핵심 수집 매트릭

| 수집 메트릭 식별자 | 감시 대상 |
|------------------|-----------|
| `jvm_threads_live` | 활성 플랫폼 스레드 수 |
| `jvm_threads_peak` | 최고 스레드 수 워터마크 |
| `jvm_threads_states_threads{state="runnable"}` | 현재 실행 가능 상태 스레드 수 |

#### 경고 기준 요약

| 항목 | 기준 |
|------|------|
| CPU Usage | 85% 이상 지속 |
| Involuntary CS | 급격한 증가 추세 감지 |
| Runnable Thread 수 | CPU Core 수의 5배 이상 |
| BLOCKED Thread 수 | 지속적 증가 |

### 6-2. Step-by-Step 진단 절차

**Step 1 — 시스템 전체 CPU 상태 확인**

```bash
top
# 확인 항목: %us (user), %sy (system), %id (idle)
# %sy 가 20% 이상이면 커널 오버헤드 의심
```

**Step 2 — 호스트 레벨 Context Switching 수치 확인**

```bash
vmstat 1
# 확인 항목: cs 컬럼 (초당 Context Switch 수)
# cs 값이 수만 건 이상이면 스레드 과부하 의심
```

**Step 3 — JVM 프로세스 스레드별 문맥 전환 분석 (pidstat)**

가상머신 전체 CPU 사용률이 치솟는 비상 상황에서 어떤 JVM 내부 스레드 그룹이 커널 Context Switching 경합을 주도하는지 무정지 실시간 필터링을 가동한다.

```bash
# 대상 JVM(PID 808)의 하부 스레드(LWP)별 문맥 전환 지표를 1초 간격으로 5회 샘플링
pidstat -w -t -p 808 1 5 > /var/log/jvm_dumps/kernel_cs_profile.txt
```

**pidstat 출력 분석 예시 및 원인 판별**

```
┌───────────────────────────────────────────────────────────────────┐
│ TGID    TID   cswch/s    nvcswch/s   Cmd                          │
│ 808     -     12450.20   48500.10    java                         │
│ 808     815   42.10      5210.40     http-nio-8080-exec-1         │
│ 808     816   38.40      5420.10     http-nio-8080-exec-2         │
└───────────────────────────────────────────────────────────────────┘
  ↑ nvcswch/s (Involuntary) 폭증 → CPU Saturation 구조 결함 확정
```

| 분석 패턴 | 진단 결론 |
|---------|---------|
| `cswch/s` (자발적) 압도적 우세 | 내부 락 경합 또는 다운스트림 외부 API 타임아웃 레이턴시로 스레드가 홀딩-반납 무한 반복 중 |
| `nvcswch/s` (비자발적) 압도적 우세 | 가용 CPU 코어 수 대비 플랫폼 스레드 개수가 비정상적으로 과생성되어 물리 스케줄러 런큐 파괴 |

**Step 4 — pidstat TID와 Thread Dump 결착 분석**

pidstat에서 적발된 고부하 네이티브 스레드 ID(TID)를 16진수로 변환하여 스레드 덤프와 매핑한다.

```bash
# TID 815 → 16진수 변환: 815 = 0x32f
jcmd 808 Thread.print > /var/log/jvm_dumps/vt_cs_evidence.tdump

# 덤프 내에서 nid=0x32f 를 검색하여 원인 소스코드 컴포넌트 저격
grep "nid=0x32f" /var/log/jvm_dumps/vt_cs_evidence.tdump
```

**Step 5 — JFR 심층 분석 (가상 스레드 환경)**

```bash
jcmd 808 JFR.start name=cs_detect settings=profile duration=60s \
    filename=/var/log/jvm_dumps/cs_detect.jfr
```

JDK Mission Control(JMC)에서 다음 이벤트를 분석한다.

| JFR 분석 대상 이벤트 | 목적 |
|---------------------|------|
| `Thread Scheduling` | 스레드 스케줄링 패턴 및 대기 시간 |
| `Java Monitor Blocked` | 락 경합으로 인한 Voluntary CS 발생 위치 |
| `jdk.VirtualThreadPinned` | Thread Pinning 원인 소스코드 라인 |

### 6-3. 장애 복구 런북 (Remediation)

**1단계 — 트래픽 스로틀링 강제 가동**

고빈도 Context Switching으로 인해 전체 노드가 응답 불능에 빠지는 임계 전이를 막기 위해, API 게이트웨이 레이어에서 **Rate Limiter를 즉각 가동**하여 타깃 인스턴스로의 부하 인입선을 강제 급제동한다.

**2단계 — 수평 가상화 분산 확장**

Kubernetes 오토스케일러 상한선을 일시적으로 해제하고 **강제 Scale-Out** 명령을 투사하여 물리 호스트 단위의 CPU 스케줄러 가용 Runqueue 공간을 신속하게 분산 확보한다.

**3단계 — 근본 원인 수정**

| 진단 결과 | 수정 방안 |
|---------|---------|
| 플랫폼 스레드 과생성 | Thread Pool 크기를 CPU Core 수 기반으로 재조정 |
| Lock 경합으로 인한 Voluntary CS 폭증 | `ConcurrentHashMap`, `AtomicLong`, `ReentrantLock`으로 교체 |
| Virtual Thread Pinning 탐지 | `synchronized` → `ReentrantLock` 100% 전환 |
| 순수 I/O 바운드 워크로드 | Virtual Thread 또는 WebFlux 전환 검토 |

---

## 7. 2026 Production Baseline JVM Runtime Manifest

2026년 현재 분산 마이크로서비스 결제 노드 환경에서 커널 문맥 전환 오버헤드를 물리적으로 최소화하고 유효 CPU 연산 능력을 극대화하기 위해 조율 완료된 최종 기동 규격서다.

```bash
java \
  # [힙 가상 주소 공간의 물리 페이지 지연 바인딩 오버헤드 원천 제거]
  -Xms12g -Xmx12g \
  -XX:+AlwaysPreTouch \
  \
  # [2026년 초저지연 세대별 ZGC 컬렉터 엔진]
  -XX:+UseZGC \
  -XX:+ZGenerational \
  \
  # [플랫폼 스레드 스택 자원 최적화]
  -Xss1m \
  \
  # [가상 스레드 가시성 확보 및 물리 스케줄러 코어 풀 하드 코딩]
  -Djdk.trackAllThreads=true \
  -Djdk.virtualThreadScheduler.parallelism=16 \
  -Djdk.virtualThreadScheduler.maxPoolSize=16 \
  \
  # [가상 스레드 Pinning 발생 시 즉각 추적 인쇄]
  -Djdk.tracePinnedThreads=short \
  \
  # [세이프포인트 루프 폴링 지연 블로킹 방어]
  -XX:+UseCountedLoopSafepoints \
  -XX:SafepointTimeoutDelay=100 \
  -XX:+SafepointTimeout \
  \
  # [치명적 고달 시점 자동 데이터 주조]
  -XX:+CrashOnOutOfMemoryError \
  -XX:+UnlockDiagnosticVMOptions \
  -Xlog:gc*,safepoint=info:file=/var/log/jvm_dumps/jvm_cs_core.log:time,uptime,pid:filecount=5,filesize=50M \
  -jar fintech-payment-api.jar
```

### 핵심 기동 옵션 분석

| 옵션 | 공학적 조항 |
|------|------------|
| `-Djdk.virtualThreadScheduler.parallelism=16` `-Djdk.virtualThreadScheduler.maxPoolSize=16` | ForkJoinPool의 물리 OS 스레드 가동 본수를 **가용 물리 CPU 코어 수와 1:1로 하드 코딩 고정**. 누락 시 I/O 지연·락 경합 돌출 상황에서 네이티브 LWP가 한계 없이 증식하여 Involuntary Context Switching 폭증 및 인프라 전멸 리스크를 아키텍처 레벨에서 격리 차단 |
| `-XX:+AlwaysPreTouch` | JVM 기동 시 힙 전역에 **물리 0 바이트 데이터를 강제 매핑**하여 런타임 중 힙 확장 과정에서 발생하는 Page Fault 인터럽트 → 커널 모드 전이 → TLB 플러시 Context Switching 오버헤드를 기동 단계에서 원천 방제 |
| `-Djdk.tracePinnedThreads=short` | 가상 스레드 Pinning 발생 즉시 stderr에 Stack Trace 강제 인쇄. 원인 불명의 Involuntary CS 급증 시 `synchronized` 임계 영역을 소스코드 라인 단위로 즉각 적발 |
| `-Djdk.trackAllThreads=true` | 수만 개의 가상 스레드 동기화 상태를 JVM 진단 메트릭 서버에 상시 기록. jcmd Thread Dump에서 가상 스레드의 락 대기 관계가 완전한 그래프 형태로 묘사되어 MTTR 단축 |
| `-XX:+UseCountedLoopSafepoints` | 카운티드 루프 내 세이프포인트 삽입으로 TTSP(Time To SafePoint) 지연 방어. 세이프포인트 대기 중 발생하는 불필요한 스레드 정지 및 CS 오버헤드 감소 |

---

## 8. 운영 원칙

Context Switching은 운영체제의 정상 기능이지만, **과도하게 증가하면 CPU 자원의 상당 부분이 실제 비즈니스 처리 대신 스케줄링 작업에 소모**된다. 단순 CPU 사용률 수치만으로는 원인을 판별하기 어려우므로 다음 항목을 종합적으로 관찰해야 한다.

```
분석 체크리스트
├── CPU Usage 절대값 및 %sys 비율
├── Runnable Thread 수 (CPU Core 수 대비 비율)
├── Context Switching 수 및 유형 (Voluntary / Involuntary)
├── Lock Contention 빈도
├── Thread Pool 크기 적정성
├── Carrier Thread 사용률
└── Virtual Thread Pinning 발생 여부
```

**Fin-Tech 결제 시스템의 핵심 운영 원칙**

1. **Thread Pool 크기를 CPU Core 수 기반으로 설계**한다: 가용 코어 수 대비 과도한 스레드 생성은 Involuntary CS를 기하급수적으로 증폭시킨다.
2. **Lock 경합을 최소화**한다: `ConcurrentHashMap`, `AtomicLong`, `ReentrantLock`으로 Voluntary CS 유발 지점을 줄인다.
3. **Virtual Thread 환경에서 `synchronized`를 완전 제거**한다: Pinning 발생 시 가상 스레드 도입 효과가 완전히 역전되어 Involuntary CS가 폭증한다.
4. **`-Djdk.virtualThreadScheduler.maxPoolSize`를 물리 코어 수에 고정**한다: 네이티브 백업 스레드 무한 증식을 아키텍처 레벨에서 차단한다.
5. **`-XX:+AlwaysPreTouch`로 런타임 Page Fault를 기동 단계에서 선제 방제**한다: Tail Latency 구간의 TLB 플러시 CS 노이즈를 원천 제거한다.

---

*본 문서는 2026년 05월 기준 금융 결제 시스템 SRE 운영 가이드라인에 근거하여 작성되었습니다.*

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*