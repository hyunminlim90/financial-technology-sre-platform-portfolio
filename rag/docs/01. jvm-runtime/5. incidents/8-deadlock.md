# Fin-Tech 결제 시스템 SRE 관점의 JVM Deadlock 장애 분석 가이드

> 정독: 0회

## 목차

1. [개요](#1-개요)
2. [Deadlock의 기본 조건 및 발생 구조](#2-deadlock의-기본-조건-및-발생-구조)
3. [하부 인프라, OS 커널 및 분산 환경의 Deadlock 연동 기전](#3-하부-인프라-os-커널-및-분산-환경의-deadlock-연동-기전)
4. [JVM 내부 메모리 및 모니터 서브시스템 정밀 역학](#4-jvm-내부-메모리-및-모니터-서브시스템-정밀-역학)
5. [기술 발전 및 핀테크 아키텍처 채택 동향 (2026년 기준)](#5-기술-발전-및-핀테크-아키텍처-채택-동향-2026년-기준)
6. [관측성(Observability) 및 트러블슈팅 런북](#6-관측성observability-및-트러블슈팅-런북)
7. [2026 Production Baseline JVM Runtime Manifest](#7-2026-production-baseline-jvm-runtime-manifest)
8. [운영 원칙](#8-운영-원칙)

---

## 1. 개요

**Deadlock(교착 상태)** 은 둘 이상의 스레드 또는 프로세스가 서로가 보유한 자원을 기다리면서 영구적으로 진행할 수 없는 상태를 의미한다.

Deadlock은 CPU 사용률 증가 없이도 서비스 기능 일부 또는 전체를 중단시킬 수 있는 **고위험 장애 유형**이다. 단순 JVM 레벨의 고착을 넘어 리눅스 커널의 프로세스 스케줄링 상태 및 글로벌 분산 트랜잭션 레이어까지 상호 연동되어 인프라 전체를 마비시킨다.

### 대표 증상

| 증상 | 설명 |
|------|------|
| 특정 API 응답 정지 | 특정 엔드포인트의 TPS가 0으로 수렴 |
| 처리량(TPS) 감소 | 데드락 스레드가 커넥션 풀 점유 |
| Thread 대기 증가 | BLOCKED / WAITING 상태 스레드 폭증 |
| 트랜잭션 타임아웃 증가 | DB 락 대기 및 분산 트랜잭션 지연 |
| 일부 기능 영구 정지 | 자발적 해제가 불가능한 설계 결함 |

---

## 2. Deadlock의 기본 조건 및 발생 구조

### 발생의 4가지 필요 조건

Deadlock은 다음 네 가지 조건이 **동시에** 만족될 때 발생한다.

| 조건 | 설명 | 예시 |
|------|------|------|
| **Mutual Exclusion** | 자원을 동시에 하나의 스레드만 사용 가능 | `synchronized(lock) { }` |
| **Hold and Wait** | 보유 자원을 유지한 채 다른 자원 대기 | Lock-A 보유 + Lock-B 대기 |
| **No Preemption** | 보유 중인 자원을 강제로 회수 불가 | OS/JVM이 락을 강제 해제 불가 |
| **Circular Wait** | 스레드들이 서로 상대방 자원을 기다리는 순환 구조 | A→B→A 순환 대기 |

### 기본 발생 패턴

```
Thread A                    Thread B
─────────────────           ─────────────────
Lock-1 획득                 Lock-2 획득
    │                           │
    ▼                           ▼
Lock-2 대기 ◄─── 교착 ───► Lock-1 대기
    │                           │
 영구 정지                   영구 정지
```

### 코드 수준 발생 예시

```java
// Thread A 실행 경로
synchronized(lockA) {
    synchronized(lockB) {   // Lock-B 대기
        // 임계 영역
    }
}

// Thread B 실행 경로 (동시 실행 시 Deadlock 발생)
synchronized(lockB) {
    synchronized(lockA) {   // Lock-A 대기
        // 임계 영역
    }
}
```

---

## 3. 하부 인프라, OS 커널 및 분산 환경의 Deadlock 연동 기전

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ [Fin-Tech E2E Deadlock 연동 구조]                                              │
│                                                                              │
│  [MSA 인그레스 노드]                           [결제 코어 DB 노드]                  │
│                                                                              │
│  Virtual Thread A ──(1) ReentrantLock 획득──► Account Row 1 (Tx 1 Lock)       │
│       │                                              │                       │
│       │ (2) 락 획득 실패 (순환 대기)             (3) 역방향 락 시도                  │
│       ▼                                              ▼                       │
│  Virtual Thread B ◄─────────────────────────  Account Row 2 (Tx 2 Lock)      │
│                                                                              │
│       └─────────── (4) 분산 트랜잭션 타임아웃 지연 ──────────────┘                  │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 3-1. 리눅스 커널 Task State 고착화

**Task State 영구 전이**

데드락에 진입한 스레드는 모니터 구조체의 대기 큐에서 타임아웃 없이 무한 대기 상태로 전이된다. 리눅스 커널 관점에서 이 LWP(Light-Weight Process)들은 `TASK_INTERRUPTIBLE` 또는 `TASK_UNINTERRUPTIBLE` 상태로 CPU 스케줄러의 실행 큐에서 **영구 제외**된다.

**커널 리소스 누수**

데드락 스레드는 CPU 사이클을 소모하지 않으나, 다음 자원을 영구 점유한다.

| 점유 자원 | 설명 |
|---------|------|
| Thread Stack | 네이티브 스택 메모리 (기본 1MB/스레드) |
| Heap 객체 | 락 객체 및 관련 비즈니스 객체 |
| DB Connection | 커넥션 풀 슬롯 점유 |
| File Descriptor | 소켓 및 파일 핸들 |
| Futex 관리 자원 | 커널 내부 동기화 구조체 |

### 3-2. 가상 스레드(Virtual Threads) 스케줄러 내부의 고착 기전

**Carrier Thread 영구 점유**

JDK 21 이후 가상 스레드 환경에서 `synchronized` 블록 내부에서 데드락이 발생하면, **Thread Pinning 메커니즘과 결합**하여 물리 Carrier Thread(ForkJoinPool Worker)가 영구 고착된다.

```
Virtual Thread A (Deadlocked)
        │  synchronized 블록 내 교착
        ▼
Carrier Thread 1  ← 영구 점유 (Pinned)

Virtual Thread B (Deadlocked)
        │  synchronized 블록 내 교착
        ▼
Carrier Thread 2  ← 영구 점유 (Pinned)

수만 개의 신규 Virtual Thread
        │
        ▼
    실행 대기 큐에 적체  →  인스턴스 전체 마비
```

ForkJoinPool 물리 스레드가 데드락으로 순차 고갈되면, **데드락과 무관한 단순 잔액 조회, 헬스체크 API까지** 실행 기회를 얻지 못하고 Downstream 마비가 발생한다.

### 3-3. 분산 환경으로의 락 확산 (Distributed Deadlock)

**애플리케이션과 DB의 락 체인 결착**

핀테크 결제 시스템에서 데드락은 단일 JVM 내부에서 끝나지 않는다.

```
JVM 스레드 A                     JVM 스레드 B
─────────────────                ─────────────────
Application Lock-X 보유           DB Row-2 락 보유
        │                                │
        ▼                                ▼
DB Row-1 (SELECT FOR UPDATE) 대기    Application Lock-X 대기
        │                                │
        └──────────── 순환 교착 ──────────┘
```

**분산 교착 확산 과정**

데이터베이스의 락 매니저(Lock Manager)는 JVM 내부의 락 상황을 알 수 없으므로 DB 레벨의 즉각적인 `DeadlockLoserDataAccessException`을 방출하지 못한다. 결과적으로 데이터베이스와 JVM 양단이 **서로의 타임아웃 임계치까지 커넥션을 점유한 채 대기**하는 글로벌 분산 교착 상태로 확산된다.

---

## 4. JVM 내부 메모리 및 모니터 서브시스템 정밀 역학

### 4-1. ObjectMonitor의 락 소유권 영구 고착 구조

```
ObjectMonitor (Lock-A)          ObjectMonitor (Lock-B)
┌──────────────────┐             ┌──────────────────┐
│ _owner: Thread A │             │ _owner: Thread B │
│ _cxq:            │             │ _cxq:            │
│   └─ Thread B    │◄── 대기      │   └─ Thread A    │◄── 대기
│     (futex WAIT) │             │     (futex WAIT) │
└──────────────────┘             └──────────────────┘
       Thread A이 보유                  Thread B이 보유
       Thread B이 대기                  Thread A이 대기
```

중량 락(Heavyweight Lock) 체제에서 데드락이 성립되면, JVM 네이티브 메모리에 할당된 두 개 이상의 ObjectMonitor 구조체 내의 `_owner` 필드가 상대 스레드의 네이티브 스레드 ID를 가리킨 채 고정된다.

**인터럽트 불가 조건**: 외부의 `Thread.interrupt()` 신호가 인입되더라도, 비즈니스 코드가 `lockInterruptibly()` 계열 인터페이스 대신 전통적인 `synchronized` 구조를 채택했다면 **인터럽트 플래그만 마킹될 뿐 락 대기 상태를 파괴하지 못한다.**

### 4-2. ThreadMXBean 내장 그래프 스캔 역학

JVM은 자체적으로 데드락을 탐지하는 `ThreadMXBean` 인터페이스를 런타임에 구동한다.

**데드락 탐지 알고리즘 (Cycle Detection)**

`ThreadMXBean.findDeadlockedThreads()` 호출 시 JVM은 다음 절차로 순환 탐지를 수행한다.

```
1. 유저 스페이스 모니터 락 구조체 전체 수집
        │
        ▼
2. AQS(AbstractQueuedSynchronizer) 대기 그래프 수집
        │
        ▼
3. 스레드-자원 간 Directed Graph 구성
        │
        ▼
4. DFS(Depth-First Search) 기반 순환 루프(Cycle) 탐지
        │
        ▼
5. 교착 스레드 ID 목록 반환
```

**STW 오버헤드 주의**: 이 그래프 스캔 연산은 JVM 내부 스레드 테이블 전체를 락킹하고 수행된다. 수천 개의 스레드가 구동 중인 대규모 트래픽 환경에서 탐지 메서드를 빈번히 호출하면 **글로벌 세이프포인트(Safepoint) STW를 유발**하여 Tail Latency를 제어 불능 상태로 빠뜨린다.

---

## 5. 기술 발전 및 핀테크 아키텍처 채택 동향 (2026년 기준)

### 5-1. 구세대 무조건적 대기 모델의 도태

**JDK 8~11 레거시 핀테크 시스템의 설계 결함**

- 복수 레이어에서 `synchronized` 블록을 중첩 배치(Nested Lock)하는 설계 전개
- 자원 획득 순서 정형화(Lock Ordering) 프로토콜이 코드 수정 과정에서 파괴될 때마다 무방비로 데드락 노출
- 타임아웃 메커니즘 없이 무한 대기하는 구조적 안전성 결함

→ 현대 분산 결제 인프라에서 **전면 도태**

### 5-2. JDK 중간기: 타임아웃 기반 명시적 락 구조의 표준화

`synchronized`를 배제하고 `ReentrantLock.tryLock()` 인터페이스를 전사 표준으로 채택했다.

```java
// 구세대: 무한 대기 (Deadlock 위험)
synchronized(lockA) {
    synchronized(lockB) { ... }
}

// 표준화: Bounded-Wait (Deadlock 방지)
if (lockA.tryLock(100, TimeUnit.MILLISECONDS)) {
    try {
        if (lockB.tryLock(100, TimeUnit.MILLISECONDS)) {
            try {
                // 임계 영역
            } finally { lockB.unlock(); }
        }
    } finally { lockA.unlock(); }
}
```

락을 획득하지 못해도 지정된 밀리초 이내에 **포기(Abandon)하고 페일백 처리**함으로써 단일 인스턴스가 데드락으로 전소되는 파국을 저지할 수 있게 되었다.

### 5-3. Actor 모델, EDA 및 가상 스레드 시대의 동시성 패러다임 (2026년 표준)

**2026년 아키텍처 동향**

현대 핀테크 코어 아키텍처는 공유 메모리 기반의 복잡한 락 구조를 최소화하기 위해 **Event-Driven Architecture(EDA)** 및 **단일 스레드 기반 링 버퍼(LMAX Disruptor)** 구조를 적극 채택하고 있다. 메모리 격리를 통해 스레드 간 자원 공유 자체를 원천 차단하여 **데드락이 발생할 수 있는 대지(Soil)를 제거**하는 방식이다.

**가상 스레드 환경의 ReentrantLock 규격화**

불가피하게 메모리 락을 사용해야 하는 도메인에서는, Carrier Thread Pinning을 회피하기 위해 `synchronized` 대신 **가상 스레드 친화적인 `ReentrantLock` 및 `StampedLock`으로 100% 교체 완결**하는 것이 2026년 현재 금융 시스템의 프로덕션 표준 규격이다.

| 기술 | 2026년 위상 |
|------|-----------|
| `synchronized` 중첩 사용 | 전면 금지 (Carrier Thread Pinning 위험) |
| `ReentrantLock.tryLock()` | 전사 표준 (Bounded-Wait 보장) |
| `lockInterruptibly()` | 운영 중 강제 중단 가능, 복구 유연성 향상 |
| EDA / LMAX Disruptor | 공유 메모리 자체를 제거하는 최고 수준의 방어 |

---

## 6. 관측성(Observability) 및 트러블슈팅 런북

### 6-1. SRE 실시간 Deadlock 탐지 임계 지표 매트릭스

#### Prometheus / Grafana 핵심 수집 매트릭

| 수집 메트릭 식별자 | 감시 대상 | 크리티컬 알람 발령선 |
|------------------|-----------|---------------------|
| `jvm_threads_deadlocked_threads` | JVM 내부 DFS 알고리즘으로 확정된 데드락 스레드 수 | **Critical**: 카운터 > 0 기록 즉시 PagerDuty 자동 발령 |
| `jvm_threads_states_threads{state="blocked"}` | 모니터 락 획득 대기 스레드 수 | **Warning**: 급격한 증가 추세 감지 시 |
| `jvm_threads_live` | 활성 스레드 총 수 | 임계치 기준으로 알람 구성 |

#### 경고 기준 요약

| 항목 | 기준 |
|------|------|
| Deadlocked Threads | **1 이상 → 즉각 Critical** |
| BLOCKED Threads | 급격한 증가 추세 |
| 특정 API TPS | 0에 근접 |
| Transaction Timeout | 지속적 증가 |

#### Virtual Thread 특화 메트릭

| 메트릭 | 설명 |
|--------|------|
| Pinned Thread Count | Thread Pinning 발생 수 |
| Carrier Thread Usage | Carrier Thread 사용량 |

### 6-2. jcmd를 통한 고성능 무정지 Deadlock 분석

데드락 경보 감지 즉시, 대상 인스턴스의 런타임에 최소 부담으로 스레드 동기화 락 그래프 아웃풋을 확보한다.

**수집 명령**

```bash
# 대상 프로세스(PID 707)의 스레드 덤프 및 데드락 자동 분석 수행
jcmd 707 Thread.print > /var/log/jvm_dumps/deadlock_analysis_report.tdump
```

**결과 판독: 자원 순환 고리 적발**

`.tdump` 파일 최하단에 JVM이 DFS 알고리즘으로 추출한 데드락 섹션이 인쇄된다.

```
======================================================================
Found one Java-level deadlock:

"http-nio-8080-exec-5":
  waiting to lock object monitor 0x0000000712345678
    (object 0x00000007a8b9c123, a java.lang.Object),
  which is held by "http-nio-8080-exec-9"

"http-nio-8080-exec-9":
  waiting to lock object monitor 0x0000000789abcdef
    (object 0x00000007b9c8d456, a java.lang.Object),
  which is held by "http-nio-8080-exec-5"

Java stack information for the threads listed above:
"http-nio-8080-exec-5":
  at com.fintech.core.v1.AccountService.transfer(AccountService.java:45)
  - waiting to lock <0x00000007a8b9c123> (a java.lang.Object)
  - locked <0x00000007b9c8d456> (a java.lang.Object)

"http-nio-8080-exec-9":
  at com.fintech.core.v1.AccountService.transfer(AccountService.java:42)
  - waiting to lock <0x00000007b9c8d456> (a java.lang.Object)
  - locked <0x00000007a8b9c123> (a java.lang.Object)
```

**SRE 정밀 분석 순서**

1. `Found one Java-level deadlock:` 섹션에서 교착 스레드 쌍 확인
2. 메모리 주소 `<0x...>` 상호 교차 잠금 관계 도식화
3. `AccountService.java:42`, `AccountService.java:45` 소스코드 라인을 **수정 대상으로 확정**
4. 두 락의 획득 순서가 역전된 비즈니스 로직 수정

### 6-3. JFR 기반 Virtual Thread Deadlock 탐지 (2026년 환경 특화)

가상 스레드가 복잡한 AQS 동기화 객체(`ReentrantLock`)를 교차 대기하다 데드락에 빠지면, 고전적인 스레드 덤프에 명확히 인쇄되지 않을 수 있다. **JFR 실시간 프로파일링**으로 보완한다.

**수집 명령**

```bash
# JFR 실시간 프로파일러 60초 가동
jcmd 707 JFR.start name=deadlock_detect settings=profile duration=60s \
    filename=/var/log/jvm_dumps/deadlock_detect.jfr
```

**결과 판독**

JDK Mission Control(JMC)으로 `.jfr` 바이너리를 파싱하여 `jdk.VirtualThreadDeadlock` 이벤트를 추출한다. 가상 스레드가 영구 격리된 지점을 **소스코드 라인 단위로 적발**한다.

| JFR 분석 대상 이벤트 | 목적 |
|---------------------|------|
| `jdk.VirtualThreadDeadlock` | 가상 스레드 간 교착 발생 위치 |
| `Java Monitor Blocked` | 모니터 락 대기 시간 분석 |
| `Java Lock` | 락 획득 실패 빈도 및 패턴 |

### 6-4. 장애 대응 절차 런북

```
1단계  Deadlock 탐지
       jvm_threads_deadlocked_threads > 0 확인
              │
              ▼
2단계  Thread Dump 수집
       jcmd <pid> Thread.print
              │
              ▼
3단계  락 순환 구조 확인
       Found one Java-level deadlock 섹션 분석
              │
              ▼
4단계  영향 범위 확인
       API / DB / Message Queue 영향도 파악
              │
              ▼
5단계  트래픽 차단 및 인스턴스 격리
       카나리 배포 인터페이스 또는 k8s 서비스 엔드포인트 수정으로
       장애 노드 즉각 Deregister
              │
              ▼
6단계  프로세스 강제 종료 및 재기동
       Graceful Shutdown 불가 → kill -9 집행 전 덤프 확보 완료 확인
       Pod 강제 재기동 (k8s Liveness Probe Failure 유도)
              │
              ▼
7단계  원인 수정 후 재배포
       Lock Ordering 또는 tryLock() Timeout 적용
```

> **주의**: Deadlock은 소프트웨어 설계 결함으로 **자발적 해제가 불가능**하다. 락 타임아웃 규격이 누락된 레거시 모듈에서 데드락이 확정되면 인스턴스를 무정지로 살려낼 수 있는 운영 공학적 방법은 존재하지 않는다.

---

## 7. 2026 Production Baseline JVM Runtime Manifest

2026년 현재 고빈도 분산 결제 인프라에서 Deadlock 예방, 실시간 탐지 및 인프라 자가 치유(Self-Healing) 구동을 위한 최종 검증 기동 명세서다.

```bash
java \
  # [힙 및 물리 메모리 페이지 선점 사양]
  -Xms16g -Xmx16g \
  -XX:+AlwaysPreTouch \
  \
  # [초저지연 고가용 ZGC 컬렉터]
  -XX:+UseZGC \
  -XX:+ZGenerational \
  \
  # [가상 스레드 락 트래킹 인프라 강제 활성화]
  -Djdk.trackAllThreads=true \
  \
  # [가상 스레드 Pinning 발생 시 즉각 추적 인쇄]
  -Djdk.tracePinnedThreads=short \
  \
  # [OOM 및 치명적 교착 상태 발생 시 커널 레벨 자가 치유]
  -XX:+CrashOnOutOfMemoryError \
  -XX:OnError="jcmd %p Thread.print > /var/log/jvm_dumps/deadlock_auto_capture_%p.tdump; kill -9 %p" \
  \
  # [오프힙 메모리 상한 제약]
  -XX:MetaspaceSize=512m \
  -XX:MaxMetaspaceSize=512m \
  \
  # [런타임 진단 서브시스템 활성화 및 비동기 파일 인쇄]
  -XX:+UnlockDiagnosticVMOptions \
  -Xlog:gc*,safepoint=info:file=/var/log/jvm_dumps/jvm_deadlock_core.log:time,uptime,pid:filecount=5,filesize=50M \
  -jar fintech-payment-gateway.jar
```

### 핵심 기동 옵션 분석

| 옵션 | 공학적 조항 |
|------|------------|
| `-XX:OnError="jcmd %p Thread.print > ...; kill -9 %p"` | JVM 비정상 상태 감지 시 **자동으로 스레드 덤프를 수집한 후 프로세스를 강제 종료**. 데드락 증거 파일을 자동 각인하고, k8s Liveness Probe Failure를 유도하여 컨테이너 자동 재기동(Self-Healing) 발동 |
| `-Djdk.trackAllThreads=true` | 수만 개의 가상 스레드 동기화 상태 및 AQS 락 획득 대기 그래프를 JVM 진단 메트릭 서버에 상시 기록. 가상 스레드 간 복합 데드락의 순환 고리를 **완벽한 Directed Graph 형태**로 jcmd 아웃풋에 묘사하여 MTTR을 분 단위 → 초 단위로 단축 |
| `-Djdk.tracePinnedThreads=short` | 가상 스레드 Pinning 현상 발발 즉시 stderr에 Stack Trace 강제 인쇄. Deadlock과 Pinning의 복합 장애 상황을 즉각 구분 및 적발 가능 |
| `-XX:+CrashOnOutOfMemoryError` | OOM 발생 시 JVM이 핵심 덤프를 남기고 즉각 종료하여 k8s 재기동 유도 |

---

## 8. 운영 원칙

Deadlock은 CPU, 메모리, GC 사용량만으로 탐지하기 어려운 고위험 장애 유형이다. 다음 항목을 **종합적으로** 관찰해야 한다.

```
분석 체크리스트
├── 락 획득 순서 (Lock Ordering 준수 여부)
├── Thread State 분포 (BLOCKED / WAITING)
├── Deadlocked Thread Count (jvm_threads_deadlocked_threads)
├── Transaction Timeout 추이
├── DB Lock Wait 시간
├── Carrier Thread 사용률
└── Thread Dump / JFR 이벤트 분석
```

**Fin-Tech 결제 시스템의 핵심 운영 원칙**

1. **설계 단계에서 Lock Ordering을 강제**한다: 모든 스레드가 동일한 순서로 락을 획득하도록 코드 리뷰 및 아키텍처 규격을 수립한다.
2. **`ReentrantLock.tryLock()`으로 Bounded-Wait를 보장**한다: 락을 지정 시간 내 획득하지 못하면 즉시 포기하고 페일백 처리한다.
3. **`synchronized` 중첩 사용을 전면 금지**한다: 특히 가상 스레드 환경에서 Carrier Thread Pinning과 결합 시 인프라 전체 마비로 확산된다.
4. **DB, 외부 API, Message Queue 모두에 타임아웃을 필수 적용**한다: 분산 데드락의 발화점을 원천 차단한다.
5. **Deadlock은 런타임에서 해결하려 하지 않는다**: 자발적 해제가 불가능한 설계 결함이므로, 예방(설계) > 탐지(모니터링) > 격리(Kill & Restart) 순서로 대응 체계를 구축한다.

---

*본 문서는 2026년 05월 기준 금융 결제 시스템 SRE 운영 가이드라인에 근거하여 작성되었습니다.*

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*