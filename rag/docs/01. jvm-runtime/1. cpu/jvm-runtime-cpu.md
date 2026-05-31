# JVM Runtime — FinTech 결제 시스템 SRE 관점 CPU 자원 E2E 분석

> 정독: 0회

**대상 스택**: JVM Runtime (OpenJDK 21/25 LTS)  
**분석 기준**: FinTech 결제 시스템 SRE · CPU 자원 · Hardware → OS Kernel → Runtime → Framework → Application  
**기준 일자**: 2026년 05월

---

## 목차

1. [물리/가상 CPU 스펙 확인 지표](#1-물리가상-cpu-스펙-확인-지표)
2. [JVM Runtime CPU 실행 흐름 (E2E Execution Path)](#2-jvm-runtime-cpu-실행-흐름-e2e-execution-path)
3. [JVM Runtime CPU 사용 메커니즘 및 실행 모델 분석](#3-jvm-runtime-cpu-사용-메커니즘-및-실행-모델-분석)
4. [CPU 병목 발생 지점](#4-cpu-병목-발생-지점)
5. [SRE 관점 모니터링 지표](#5-sre-관점-모니터링-지표)
6. [장애 시나리오](#6-장애-시나리오)
7. [튜닝 포인트](#7-튜닝-포인트)
8. [관련 Linux 명령어 및 분석 도구](#8-관련-linux-명령어-및-분석-도구)

---

## 1. 물리/가상 CPU 스펙 확인 지표

CPU를 분석할 때 "사용률"만 보는 것은 위험하다. JVM이 CPU를 **연산(Calculation)**, **대기(Stall/Wait)**, **관리 오버헤드(Administration)** 중 어디에 소모하는지 먼저 물리 스펙의 체급을 파악하고 시작한다.

### 1.1 물리 CPU 구조 확인

```bash
# 전체 CPU 토폴로지 확인
lscpu

# 캐시 계층 구조 확인 (L1d / L1i / L2 / L3 크기, 공유 단위)
lscpu -C

# NUMA 노드 구성 확인 (소켓 수, 노드별 메모리 크기)
numactl -H

# CPU ↔ NIC PCIe 버스 물리적 거리 확인
lstopo
```

| 확인 항목 | 명령어 | JVM SRE 분석 이유 |
|---|---|---|
| 소켓 / 물리 코어 / 하이퍼스레딩(HT) | `lscpu` | HT 활성화 시 L1/L2 캐시 경합 발생 가능. JVM GC 멀티스레드가 같은 물리 코어를 공유하는지 판단 |
| L1d / L2 / L3 캐시 크기 | `lscpu -C` | JVM의 결제 로직 핫 메서드가 L3 캐시에 수용되는지 여부. Context Switch 비용 민감도 판단 |
| NUMA 노드 구성 | `numactl -H` | JVM 힙 영역이 로컬 NUMA 노드에 할당되는지 확인. 원격 메모리 참조 시 레이턴시 2~3배 폭증 |
| CPU-NIC PCIe 거리 | `lstopo` | Netty 기반 네트워크 수신 인터럽트가 JVM과 다른 NUMA 노드에서 처리되는지 확인 |

### 1.2 가상화 / 컨테이너 CPU 구조

```bash
# Cgroup v2 CPU 쿼터 확인 (컨테이너 CPU Limit)
cat /sys/fs/cgroup/cpu.max
# 출력 예시: 200000 100000 → 100ms 주기에 200ms 허용 = 2 vCPU

# CPU Throttling 누적 통계 확인
cat /sys/fs/cgroup/cpu.stat

# CPU Steal Time 확인 (하이퍼바이저가 vCPU를 뺏어간 시간)
vmstat 1 | awk '{print $17}'   # st 컬럼
```

| 확인 항목 | 확인 방법 | 의미 |
|---|---|---|
| vCPU 수 | `lscpu` (Kubernetes Pod 내부) | 물리 코어가 아닌 컨테이너에 배정된 논리 코어 수 |
| CPU Limit (Quota/Period) | `cat /sys/fs/cgroup/cpu.max` | JVM 프로세스가 물리 CPU를 소비할 수 있는 최대치 |
| CPU Throttling 여부 | `cat /sys/fs/cgroup/cpu.stat` | `nr_throttled`, `throttled_usec` 값이 지속 상승 시 Limit 부족 |
| CPU Steal Time | `vmstat`, `mpstat` `%st` | 하이퍼바이저(AWS EC2 등)가 CPU를 다른 VM에 할당하는 시간. JVM 응답 지연의 숨은 원인 |

> **핀테크 SRE 핵심 주의사항**  
> 클라우드 환경(AWS/GCP)에서는 `top`의 CPU 사용률이 50%여도 Steal Time이 높으면 JVM이 실제로 받는 CPU는 그보다 훨씬 적다. `vmstat`의 `st` 컬럼을 반드시 함께 확인한다.

---

## 2. JVM Runtime CPU 실행 흐름 (E2E Execution Path)

결제 요청이 NIC에서 수신되어 Java 비즈니스 로직이 실행되고 응답이 반환되기까지, CPU가 어느 계층에서 어떻게 소비되는지 E2E 흐름으로 추적한다.

### 2.1 전체 E2E CPU 실행 흐름

```
[외부 결제 요청 패킷 수신]
        │
        ▼
┌─────────────────────────────────────────────────────────┐
│  1. Hardware Layer — NIC 수신 & DMA                      │
│     NIC 패킷 수신 → DMA로 커널 Ring Buffer에 직접 기록         │
│     → CPU에 하드웨어 인터럽트(IRQ) 발생                       │
└─────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────┐
│  2. Kernel Interrupt Layer — IRQ → SoftIRQ              │
│     CPU의 IRQ Handler 실행 (최소 처리)                      │
│     → ksoftirqd 또는 NET_RX SoftIRQ 발동                  │
│     → TCP/IP Stack 처리 (패킷 파싱, 소켓 버퍼 적재)           │
│     → epoll Wait Queue의 JVM 스레드 Wake-up 트리거          │
└─────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────┐
│  3. Kernel Scheduler Layer — CFS                        │
│     Wake-up된 JVM 스레드(task_struct)를 Run Queue 등록      │
│     → CFS의 vruntime 기준으로 스케줄링                       │
│     → CPU 코어에 할당 → Context Switch 발생                 │
│     → Cgroup CPU Quota 소비 시작 (Throttling 감시)          │
└─────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────┐
│  4. JVM Runtime Layer                                   │
│     ┌─────────────────┐   ┌──────────────────────────┐  │
│     │  JIT Compiler   │   │  GC (ZGC / G1)           │  │
│     │  - Tier 0~4     │   │  - Load Barrier (ZGC)    │  │
│     │  - C1/C2/Graal  │   │  - Concurrent Marking    │  │
│     │  - Code Cache   │   │  - STW (Safepoint)       │  │
│     └─────────────────┘   └──────────────────────────┘  │
│     ┌──────────────────────────────────────────────────┐ │
│     │  Thread / Virtual Thread Scheduler               │ │
│     │  - Carrier Thread → OS Thread 매핑                │ │
│     │  - Continuation Mount/Unmount                    │ │
│     └──────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────┐
│  5. Framework Layer — Netty / Spring / HikariCP         │
│     Netty Event Loop: I/O 이벤트 디스패치 (CPU bound)       │
│     Spring Dispatcher: 핸들러 매핑, AOP 체인 실행            │
│     HikariCP: 커넥션 획득 대기 (Lock/CAS 기반)               │
└─────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────┐
│  6. Application Layer — 결제 비즈니스 로직                  │
│     인증/암호화 (AES/RSA → CPU 집약 연산)                    │
│     DB 쿼리 직렬화/역직렬화                                  │
│     결제 승인 로직 (객체 할당 → GC 압박)                       │
└─────────────────────────────────────────────────────────┘
        │
        ▼
[응답 반환: System Call write() → Socket Buffer → NIC]
```

### 2.2 계층별 CPU 소비 성격 분류

| 계층 | CPU 소비 유형 | 핵심 CPU 비용 |
|---|---|---|
| NIC → IRQ → SoftIRQ | **%irq / %soft** (커널 인터럽트 처리) | 결제 트래픽 폭증 시 특정 코어에 집중되는 SoftIRQ 병목 |
| CFS Scheduler | **%sys** (Context Switch 오버헤드) | Platform Thread 수 과다 시 Context Switch 폭증 |
| JVM JIT Compiler | **%usr** (컴파일 연산) | 초기 기동 시 Tier 0→4 컴파일 CPU 급등 / Code Cache 전소 시 재발 |
| JVM GC | **%usr** (GC 스레드 병렬 실행) | STW 구간의 애플리케이션 스레드 정지 → Throttling 소진 |
| Virtual Thread | **%usr** (Continuation 스케줄링) | Pinning 발생 시 Carrier Thread 블로킹 → %sys 증가 |
| Business Logic | **%usr** (암호화, 직렬화) | AES/RSA 연산, JSON 파싱이 CPU 핫스팟 |

---

## 3. JVM Runtime CPU 사용 메커니즘 및 실행 모델 분석

### 3.1 Platform Thread vs Virtual Thread — CPU 스케줄링 모델

**Platform Thread (기존 OS Thread 1:1 매핑)**

```
Java Thread (수백 개)
      │  1:1 매핑
      ▼
OS Native Thread (LWP)
      │  CFS 스케줄링
      ▼
CPU 코어 할당 → Context Switch 오버헤드 발생
```

- Java Thread 수 = OS Thread 수 → Thread 수 증가 시 Context Switch(`nvcswch/s`) 폭발적 증가
- 결제 요청 폭증 시 Thread Pool이 고갈되거나 OS 스케줄링 경합이 극심해짐

**Virtual Thread (Project Loom, JDK 21+)**

```
Virtual Thread (수만~수백만 개, M)
      │  M:N 매핑 (Continuation 기반)
      ▼
Carrier Thread (= Platform Thread, N개 ≈ vCPU 수)
      │  1:1 매핑
      ▼
OS Native Thread → CPU 코어 할당
```

- I/O 블로킹 발생 시 Virtual Thread는 Carrier Thread에서 **Unmount** (CPU 반납)
- Continuation(호출 스택 스냅샷)을 힙에 저장 → I/O 완료 후 다른 Carrier Thread에 **Mount**
- CPU 소비: Context Switch 최소화, Carrier Thread 수 = vCPU 수에 근접하게 유지

> **Pinning 위험**: `synchronized` 블록 또는 JNI 내부에서 I/O 블로킹 발생 시 Carrier Thread까지 블로킹 → 사실상 Platform Thread와 동일한 CPU 낭비 발생

### 3.2 JIT 컴파일러 — CPU 소비 메커니즘

JVM의 JIT 컴파일러는 **CPU 사용의 성격을 시간축에 따라 완전히 변화**시킨다.

```
기동 초기: Tier 0 (Interpreter) → CPU 낮음, 처리량 낮음
         ↓ 실행 빈도 임계치 초과 (Hot Spot 탐지)
Tier 1~3: C1 컴파일 → 컴파일 스레드가 CPU를 순간 점유
         ↓ 프로파일링 데이터 충분히 수집
Tier 4:   C2 / Graal 컴파일 → 최대 CPU 소비 컴파일, 이후 최고 처리량
         ↓ 만약 Code Cache 전소 시
Fallback: 모든 스레드 → Tier 0 강제 전락 → CPU 100% 고착 + TPS 급락
```

**JIT가 CPU에 미치는 핵심 영향**

| 상황 | CPU 영향 | 결제 시스템 리스크 |
|---|---|---|
| 기동 직후 (Warm-up 미완료) | Tier 0 인터프리터로 CPU 비효율 | 배포 직후 레이턴시 스파이크 |
| C2 컴파일 진행 중 | 컴파일 스레드가 CPU 버스트 점유 | Cgroup CPU Quota 순간 소진 → Throttling |
| Code Cache 전소 | 모든 스레드 Tier 0 전락 → CPU 100% 고착 | 결제 TPS 10배 이상 급락 |
| Escape Analysis 작동 | Stack Allocation → GC 빈도 감소 → CPU 절감 | 정상 동작 시 CPU 효율 최대화 |

**탐지 명령어**

```bash
# Code Cache 잔여 용량 확인
jcmd <PID> VM.codecache

# JIT 컴파일 활동 실시간 확인
jcmd <PID> Compiler.directives_print
```

### 3.3 GC (Garbage Collector) — CPU 소비 메커니즘

GC는 JVM에서 가장 예측 불가능한 **CPU 버스트 소비원**이다.

**G1 GC CPU 소비 패턴**

```
Minor GC (Young GC):
  - 멀티스레드 병렬 실행 (GC 스레드 수 = ParallelGCThreads)
  - STW 동안 GC 스레드만 CPU 소비 → 애플리케이션 스레드 전원 정지
  - CPU 버스트 후 Cgroup Quota 소진 가능성 높음

Major GC / Full GC:
  - 단일 스레드 직렬 실행 (최악의 경우)
  - 수 초간 CPU 독점 → 결제 시스템 완전 마비
```

**ZGC CPU 소비 패턴 (핀테크 권장)**

```
Concurrent Marking:
  - 애플리케이션 스레드와 동시 실행 (CPU 경합 발생)
  - CPU 사용량 일정하게 증가하나, 버스트 없음

Load Barrier:
  - 모든 힙 객체 참조 시 CPU 레지스터 레벨에서 실행
  - 미세한 지속적 CPU 오버헤드 (약 2~5%)
  - STW는 1ms 미만 → CPU Quota 순간 소진 없음
```

**Safepoint & TTSP — 보이지 않는 CPU 낭비**

```
GC 요청 → JVM이 Safepoint 플래그 상승
        → 모든 스레드가 Safepoint 도달까지 대기
        → Counted Loop 처리 중인 스레드: 루프 완료 전까지 Safepoint 폴링 안 함
        → 이미 멈춘 스레드들 전원 대기 (CPU 할당받았지만 아무것도 못 함)
        → GC 실 연산 5ms, 총 지연 500ms → 결제 타임아웃 유발
```

```bash
# TTSP 지연 확인 (GC 로그에서 Time-To-Safepoint 추출)
grep -E 'Safepoint|Application time' /var/log/jvm_dumps/gc.log

# 해결: Counted Loop 내 Safepoint 폴링 강제 활성화
# JVM 플래그: -XX:+UseCountedLoopSafepoints
```

### 3.4 Lock / CAS / Spin — 동기화 CPU 소비

결제 시스템에서 동기화 메커니즘은 **CPU 낭비의 주요 원인**이다.

| 동기화 방식 | CPU 소비 방식 | 핀테크 리스크 |
|---|---|---|
| `synchronized` (Monitor Enter) | OS 레벨 Mutex (CPU를 반납하고 대기) | 경합 심화 시 `%sys` 증가, Context Switch 폭증 |
| `ReentrantLock` (AQS 기반) | CAS(Compare-And-Swap) 스핀 후 Park | `synchronized` 보다 유연하나 스핀 구간 CPU 점유 |
| Busy Spin (SpinLock) | CPU를 반납하지 않고 반복 체크 | `%usr` 100% 고착, 다른 스레드 CPU 기아 유발 |
| Virtual Thread + `synchronized` | Pinning → Carrier Thread 블로킹 | 가상 스레드 풀 전체 마비 가능 |

```bash
# Lock 경합 탐지 (JFR 이벤트)
jcmd <PID> JFR.start duration=60s filename=lock-profile.jfr
jcmd <PID> JFR.stop

# jstack으로 BLOCKED 스레드 확인
jstack <PID> | grep -A 5 "BLOCKED"

# 특정 LWP의 CPU 점유 → 어떤 Java 메서드인지 추적
top -H -p <PID>
# → CPU 높은 LWP ID 추출
printf '%x\n' <LWP_ID>
# → jstack 결과에서 nid=0x<hex> 매칭
```

### 3.5 Netty Event Loop — 프레임워크 레이어 CPU 모델

핀테크 결제 시스템에서 Netty는 JVM 내부의 핵심 CPU 소비 컴포넌트다.

```
NioEventLoopGroup (Boss)       NioEventLoopGroup (Worker)
        │                               │
  accept() 처리                  I/O 이벤트 디스패치
  (CPU: %usr 낮음)               (CPU: %usr 높음)
                                        │
                             ┌──────────┴──────────┐
                             │   epoll_wait()      │
                             │   (블로킹 없음)        │
                             │   I/O Ready 이벤트    │
                             │   → Handler 실행     │
                             └─────────────────────┘
```

**Event Loop CPU 병목 패턴**

- Worker Thread의 Handler 내부에서 **동기 블로킹 I/O** (DB 호출, 동기 HTTP) 실행 시 → Event Loop Thread 자체가 블로킹 → 해당 코어 CPU를 독점하면서 다른 이벤트 처리 불가
- 결제 요청이 몰릴 때 일부 Worker Thread만 CPU 100%이고 나머지는 Idle인 불균형 발생

---

## 4. CPU 병목 발생 지점

### 4.1 병목 유형 분류 및 진단 매핑

| 병목 유형 | 발생 계층 | 주요 지표 | 진단 명령어 |
|---|---|---|---|
| **User CPU 과부하** | Application / JIT | `%usr` > 80% 지속 | `mpstat -P ALL 1` |
| **System CPU 과부하** | Context Switch / Syscall | `%sys` > 15% | `mpstat`, `vmstat` |
| **Scheduler Delay** | CFS Run Queue 포화 | Run Queue 길이 > vCPU 수 × 2 | `vmstat 1` (r 컬럼) |
| **CPU Throttling** | Cgroup Quota 소진 | `nr_throttled` 상승 | `cat /sys/fs/cgroup/cpu.stat` |
| **L3 Cache Miss** | 메모리 접근 패턴 비효율 | `cache-misses` 높음 | `perf stat -e cache-misses` |
| **Lock Contention** | Monitor / AQS 경합 | BLOCKED 스레드 증가 | `jstack`, `JFR` |
| **GC Pause (STW)** | GC Safepoint | GC Pause > 200ms | `gc.log`, `JFR` |
| **JIT Code Cache 전소** | JIT Compiler | Code Cache 잔여 = 0 | `jcmd VM.codecache` |
| **TTSP 오버헤드** | Safepoint 집결 지연 | GC 실 시간 < 총 Pause | `gc.log` Safepoint 분석 |
| **SoftIRQ 병목** | 네트워크 IRQ 불균형 | `%soft` 특정 코어 집중 | `mpstat -P ALL`, `sar -I ALL` |
| **NUMA Remote Access** | 메모리 버스 경합 | Remote 메모리 접근 레이턴시 | `numastat`, `perf stat` |

### 4.2 Tail Latency 증가의 복합 원인 분석

핀테크 결제 시스템에서 P99 레이턴시 급등은 단일 원인이 아닌 **복합 연쇄 반응**에서 비롯된다.

```
[결제 트래픽 스파이크]
        │
        ├─► GC 스레드 동시 가동 (CPU Quota 순간 소진)
        │         │
        │         └─► Cgroup CPU Throttling 진입
        │                   │
        │                   └─► 모든 JVM 스레드 강제 대기
        │                             │
        ├─► Safepoint 집결 지연 (TTSP)  └─► 커넥션 풀 획득 대기 폭증
        │         │
        │         └─► 이미 멈춘 스레드들 추가 대기
        │
        └─► JIT 컴파일 스레드 CPU 버스트 (Quota 이중 소진)
                  │
                  └─► 타임아웃 도미노 → P99 수백 ms 폭등
```

> **핵심 인사이트**: `top`에서 CPU 사용률 50%여도, 마이크로 초 단위 Micro-burst 구간에는 Quota를 순간 초과하여 Throttling이 발생할 수 있다. **PSI**(`/proc/pressure/cpu`)를 반드시 병행 확인해야 한다.

---

## 5. SRE 관점 모니터링 지표

### 5.1 필수 CPU 모니터링 지표 매트릭스

| 지표 그룹 | 수집 지표 | 경보 기준 | 수집 방법 |
|---|---|---|---|
| **CPU 사용률** | `%usr`, `%sys`, `%soft`, `%wait`, `%steal` | `%usr+%sys` > 85% 지속 / `%steal` > 5% | `mpstat -P ALL 1` |
| **Load Average** | `load1`, `load5`, `load15` | load1 > vCPU 수 × 1.5 | `uptime`, `cat /proc/loadavg` |
| **PSI (Pressure Stall)** | `cpu.some`, `cpu.full` | `cpu.some` 10초 avg > 10% | `cat /proc/pressure/cpu` |
| **Context Switch** | `cswch/s` (자발적), `nvcswch/s` (비자발적) | `nvcswch/s` 급증 (Thread Pool 과다) | `pidstat -w 1` |
| **Run Queue** | Run Queue 길이 (`r` 컬럼) | r > vCPU 수 × 2 지속 | `vmstat 1` |
| **CPU Steal** | `%steal` | > 5% (클라우드 환경 노이지 네이버) | `vmstat`, `mpstat` |
| **SoftIRQ** | `%soft` per CPU | 특정 코어만 > 30% (인터럽트 불균형) | `mpstat -P ALL 1` |
| **Cgroup Throttling** | `nr_throttled`, `throttled_usec` | 초당 증가율 > 15% | `cat /sys/fs/cgroup/cpu.stat` |
| **GC CPU Time** | GC 스레드 CPU 점유율 | GC CPU Time / 총 CPU > 10% | `JFR`, `gc.log` |
| **JIT 컴파일 활동** | Code Cache 사용률 | Code Cache 잔여 < 10% | `jcmd VM.codecache` |
| **JVM BLOCKED Thread** | BLOCKED 상태 스레드 수 | BLOCKED > 30개 | `jcmd Thread.print`, `JFR` |
| **P95 / P99 Latency** | API 응답 레이턴시 분위수 | P99 > SLA 임계치 (예: 200ms) | Prometheus, Micrometer |
| **Event Loop Delay** | Netty Worker Event Loop 처리 지연 | > 50ms 지속 | Micrometer, JFR |

### 5.2 JVM 특화 CPU 지표 수집 설정

```bash
# GC 로그 기반 CPU 시간 추적 (gc.log 분석)
-Xlog:gc*,safepoint=info:file=/var/log/jvm_dumps/gc.log:time,uptime,pid:filecount=5,filesize=100M

# NMT + Code Cache 상태 주기적 덤프
jcmd <PID> VM.codecache
jcmd <PID> VM.native_memory summary

# Virtual Thread Pinning CPU 낭비 탐지
-Djdk.tracePinnedThreads=short
```

---

## 6. 장애 시나리오

### 시나리오 A: Code Cache 전소 → CPU 100% 고착

**발생 흐름**

```
배포 후 트래픽 증가
→ 동적 프록시(Spring AOP, Reflection) 클래스 폭증
→ JIT Tier 4 컴파일 코드가 Code Cache 채움
→ -XX:ReservedCodeCacheSize 한계 도달
→ JVM: C2 컴파일 중단, 모든 스레드 Tier 0(Interpreter) 전락
→ CPU %usr 100% 고착, TPS 10배 이상 급락
→ 결제 API 타임아웃 전파
```

**진단 및 조치**

```bash
# 1. Code Cache 상태 확인
jcmd <PID> VM.codecache
# Non-profiled nmethods 잔여 용량 = 0 확인

# 2. CPU 사용 패턴 확인 (Interpreter 실행 시 %usr 급등)
mpstat -P ALL 1

# 3. 조치: Code Cache 크기 2배 상향 후 블루-그린 롤링 재시작
# -XX:ReservedCodeCacheSize=1g
```

---

### 시나리오 B: Cgroup CPU Throttling → 결제 타임아웃 연쇄

**발생 흐름**

```
대규모 결제 스파이크 트래픽 유입
→ GC 멀티스레드 STW 스캔 + JIT 컴파일 스레드 동시 CPU 소비
→ 100ms 주기 내 CPU Quota(cpu.max) 순간 소진
→ 커널: 컨테이너 프로세스 강제 유휴 상태(Throttling) 진입
→ JVM 내부 시계 기준 10ms 연산이 실제 수백 ms로 지연
→ HikariCP 커넥션 획득 대기 → getConnection timeout
→ 결제 승인 실패율 급등
```

**진단 및 조치**

```bash
# 1. Throttling 여부 즉시 확인
watch -n 1 'cat /sys/fs/cgroup/cpu.stat | grep throttled'
# nr_throttled, throttled_usec 가 빠르게 증가하는지 확인

# 2. PSI로 실제 CPU 압박 확인
cat /proc/pressure/cpu
# some avg10 > 10% 이면 CPU 압박 실증

# 3. 조치 옵션
# - CPU Limit 상향 (kubectl patch)
# - GC 스레드 수 조정: -XX:ParallelGCThreads, -XX:ConcGCThreads
# - ZGC 전환 (STW CPU 버스트 제거)
```

---

### 시나리오 C: TTSP(Time To Safepoint) → GC 지연 5ms인데 결제 Pause 500ms

**발생 흐름**

```
ZGC Safepoint 요청 발생
→ 대부분의 스레드 즉시 Safepoint 도달
→ 단 1개의 스레드: Counted Loop (대량 결제 내역 집계 배치) 실행 중
→ 루프 완료 전까지 Safepoint 폴링 안 함 (수백 ms 소요)
→ 이미 멈춘 모든 스레드: CPU를 할당받았지만 아무 연산도 못 하고 대기
→ GC 실 연산 5ms, 총 Pause 500ms → 결제 P99 급등
```

**진단 및 조치**

```bash
# GC 로그에서 Safepoint 지연 분석
grep 'Entering safepoint\|Leaving safepoint\|Total time' gc.log

# JFR Safepoint 이벤트 상세 확인
jcmd <PID> JFR.start duration=60s filename=safepoint.jfr
# → JMC에서 SafepointWaitTime, ApplicationStoppedTime 분석

# 조치: Counted Loop Safepoint 폴링 강제 활성화
# -XX:+UseCountedLoopSafepoints
```

---

### 시나리오 D: Virtual Thread Pinning → Carrier Thread 전원 블로킹

**발생 흐름**

```
Virtual Thread 도입 후 대량 결제 동시 처리 시작
→ 레거시 코드의 synchronized 블록 내부에서 DB I/O 블로킹
→ Virtual Thread Unmount 불가 (Pinning 발생)
→ Carrier Thread (= vCPU 수와 동일, 예: 8개) 전원 순차 블로킹
→ 8개 모두 점유되면 신규 가상 스레드 실행 불가
→ 결제 처리 큐 적체 → CPU는 낮은데 처리량 0에 수렴
```

**진단 및 조치**

```bash
# Pinning 탐지 로그 확인 (JVM 기동 시 플래그 필요)
# -Djdk.tracePinnedThreads=short
grep "pinned" /var/log/application.log

# JFR VirtualThreadPinned 이벤트 확인
jcmd <PID> JFR.start duration=60s filename=pinning.jfr

# 조치: synchronized → ReentrantLock 마이그레이션
# Carrier Thread 수 조정: -Djdk.virtualThreadScheduler.parallelism=32
```

---

### 시나리오 E: SoftIRQ 폭증 → 특정 CPU 코어 독점

**발생 흐름**

```
결제 트래픽 급증 → NIC에 초당 수백만 패킷 수신
→ IRQ 처리 후 SoftIRQ (NET_RX) 처리 부하 폭증
→ IRQ Affinity 미설정 시 단일 CPU 코어(Core 0)에 SoftIRQ 집중
→ Core 0: %soft 80% 이상 → JVM GC 스레드 또는 결제 스레드가 해당 코어에 스케줄링되면 CPU 경합
→ Tail Latency 급등 (P99 폭주)
```

**진단 및 조치**

```bash
# SoftIRQ CPU 분포 확인 (특정 코어 쏠림 탐지)
mpstat -P ALL 1 | grep -v CPU | awk '{print $2, $10}'  # %soft 컬럼

# IRQ 처리 코어 분포 확인
cat /proc/interrupts | grep eth

# 조치: IRQ Affinity 분산 설정
# RPS(Receive Packet Steering)로 SoftIRQ를 여러 코어에 분산
echo "ff" > /sys/class/net/eth0/queues/rx-0/rps_cpus
```

---

## 7. 튜닝 포인트

### 7.1 JVM Runtime 튜닝

```bash
# ── GC 관련 CPU 튜닝 ─────────────────────────────────────────────
# ZGC 채택 (STW CPU 버스트 제거)
-XX:+UseZGC

# ZGC 트래픽 스파이크 대응 (마킹 스케줄 가중치 5배)
-XX:ZAllocationSpikeTolerance=5

# GC 병렬 스레드 수 제한 (CPU Quota 보호)
-XX:ParallelGCThreads=4
-XX:ConcGCThreads=2

# ── JIT 컴파일러 CPU 튜닝 ─────────────────────────────────────────
# Code Cache 공간 확충 (전소 방지)
-XX:ReservedCodeCacheSize=512m

# JIT 컴파일 스레드 수 제한 (CPU Quota 보호)
-XX:CICompilerCount=2

# ── Safepoint TTSP 제거 ───────────────────────────────────────────
-XX:+UseCountedLoopSafepoints
-XX:SafepointTimeoutDelay=100

# ── Virtual Thread Pinning 탐지 ───────────────────────────────────
-Djdk.tracePinnedThreads=short
-Djdk.virtualThreadScheduler.parallelism=<vCPU 수>

# ── 힙 고정 크기 (동적 확장 CPU 블로킹 차단) ──────────────────────
-Xms12g -Xmx12g
```

### 7.2 OS Scheduler / Kernel 튜닝

```bash
# CFS 스케줄러 타임 슬라이스 조정 (레이턴시 민감 환경)
echo 1000000 > /proc/sys/kernel/sched_min_granularity_ns
echo 10000000 > /proc/sys/kernel/sched_wakeup_granularity_ns

# 스왑 완전 비활성화 (GC Page Fault 방지)
echo 0 > /proc/sys/vm/swappiness

# Transparent HugePage 비활성화 (GC 지연 방지)
echo never > /sys/kernel/mm/transparent_hugepage/enabled
```

### 7.3 IRQ Affinity / CPU Pinning

```bash
# NIC IRQ를 특정 코어 집합에만 처리 (JVM 코어와 분리)
# JVM 전용 코어: 0-7, NIC IRQ 전용 코어: 8-11
for irq in $(cat /proc/interrupts | grep eth0 | awk '{print $1}' | tr -d ':'); do
    echo "f00" > /proc/irq/$irq/smp_affinity
done

# JVM 프로세스를 특정 코어에 핀 (NUMA 로컬 메모리와 일치)
taskset -c 0-7 java -XX:+UseNUMA ... -jar payment-core.jar

# NUMA 노드 바인딩 (원격 메모리 접근 차단)
numactl --cpunodebind=0 --membind=0 java ... -jar payment-core.jar
```

### 7.4 Cgroup / Container CPU 튜닝

```yaml
# Kubernetes Pod CPU 설정 (Request = Limit으로 Throttling 최소화)
resources:
  requests:
    cpu: "4"
  limits:
    cpu: "4"   # Request와 동일하게 설정 → Guaranteed QoS Class

# CPU Burst 허용 (Cgroup v2, 순간 스파이크 완충)
# /sys/fs/cgroup/cpu.max.burst 설정 검토
```

### 7.5 Thread Pool / Event Loop 튜닝

```bash
# Netty Worker Thread 수 = vCPU 수 × 2 (I/O 혼합 결제 시스템 기준)
# NettyServerConfig: workerCount = Runtime.getRuntime().availableProcessors() * 2

# HikariCP 커넥션 수 = (vCPU 수 × 2) + 효과적 스핀들 수
# (결제 DB가 SSD NVMe 기준, 커넥션 수 과다는 Lock 경합 유발)

# Virtual Thread 사용 시 Platform Thread Pool 제거
# (Spring Boot 3.2+: virtual-threads.enabled=true)
```

### 7.6 NUMA 튜닝

```bash
# JVM NUMA 인식 활성화 (Young Eden을 NUMA 노드별 분할)
-XX:+UseNUMA

# NUMA 메모리 통계 확인
numastat -p <PID>
# numa_miss 높으면 JVM 스레드와 메모리 노드 불일치 → taskset/numactl 재조정
```

---

## 8. 관련 Linux 명령어 및 분석 도구

### 8.1 시스템 레벨 CPU 분석 도구

| 도구 | 주요 사용 목적 | 핵심 명령어 예시 |
|---|---|---|
| `mpstat` | CPU 모드별 사용률 분해 (`%usr/%sys/%soft/%wait/%steal`) | `mpstat -P ALL 1` |
| `vmstat` | Run Queue 길이, Context Switch, CPU Steal | `vmstat 1` |
| `pidstat` | 프로세스별 CPU + Context Switch 분리 분석 | `pidstat -w -u -p <PID> 1` |
| `top` / `htop` | 전체 프로세스 CPU 순위, 스레드별 분석 (`-H`) | `top -H -p <PID>` |
| `sar` | 시계열 CPU 통계 (사후 분석) | `sar -u -P ALL 1 60` |
| `ps` | 스레드 상태 스냅샷 | `ps -eLo pid,lwp,state,pcpu,comm` |
| `perf stat` | 하드웨어 카운터 (Cache Miss, IPC, Branch Miss) | `perf stat -e cache-misses,context-switches -p <PID> sleep 10` |
| `perf top` | 함수 레벨 CPU 핫스팟 실시간 확인 | `perf top -p <PID>` |
| `perf sched` | 스케줄러 지연, Wakeup Latency 분석 | `perf sched record -p <PID>; perf sched latency` |
| `strace` | System Call 빈도 및 시간 분석 | `strace -c -p <PID>` |
| `numactl` / `numastat` | NUMA 바인딩 설정, Remote 메모리 접근 통계 | `numastat -p <PID>` |
| `taskset` | CPU 코어 핀 설정 | `taskset -c 0-7 java ...` |
| `lstopo` | CPU-NIC-메모리 NUMA 물리 토폴로지 시각화 | `lstopo --output-format ascii` |

### 8.2 JVM 특화 CPU 분석 도구

| 도구 | 주요 사용 목적 | 사용 방법 |
|---|---|---|
| **Async-Profiler** | Safepoint Bias 없는 CPU 샘플링 프로파일링 → Flame Graph | `./profiler.sh -e cpu -d 60 -f flamegraph.html <PID>` |
| **JFR (Java Flight Recorder)** | GC, JIT, Lock, Safepoint, Virtual Thread Pinning 이벤트 기록 (오버헤드 < 1%) | `jcmd <PID> JFR.start duration=60s filename=rec.jfr` |
| **jcmd** | Code Cache 상태, VM 플래그, 스레드 덤프, GC 강제 실행 | `jcmd <PID> VM.codecache` |
| **jstack** | 스레드 상태 스냅샷 (BLOCKED/WAITING/RUNNABLE 분류) | `jstack <PID>` |
| **JMC (Java Mission Control)** | JFR 파일 GUI 분석 (Method Profiling, GC 분석, Lock 분석) | JFR 파일 열기 |
| **eBPF / bpftrace** | JVM 프로세스의 커널 레벨 시스템 콜, 스케줄링 지연 추적 | `bpftrace -e 'tracepoint:sched:sched_switch { @[comm] = count(); }'` |
| **Flame Graph** | Async-Profiler 출력 → CPU 핫스팟 시각적 계층 분석 | Async-Profiler `--output flamegraph` |

### 8.3 SRE 초동 진단 5단계 체크리스트

```bash
# ① 부하 추이 파악 (가장 먼저)
uptime

# ② CPU 용도 분해 (어느 모드에서 CPU를 쓰는지)
mpstat -P ALL 1 5

# ③ 실제 CPU 압박 확인 (PSI - 가장 정확한 포화도 지표)
cat /proc/pressure/cpu

# ④ Cgroup Throttling 확인 (K8s 환경 필수)
cat /sys/fs/cgroup/cpu.stat | grep throttled

# ⑤ JVM 스레드 레벨 CPU 점유자 확인
# top에서 CPU 높은 LWP 확인 후 jstack 매핑
top -H -p <PID>
printf '%x\n' <LWP_ID>
jstack <PID> | grep "nid=0x<hex>" -A 10
```

---

## 참고: JVM Runtime CPU 자원 분석 요약 맵

```
[물리 CPU 스펙]                [Cgroup v2 가상 CPU]
  lscpu / numactl               cpu.max / cpu.stat
       │                              │
       └──────────┬───────────────────┘
                  │
                  ▼
    ┌─────────────────────────────────┐
    │  CFS Scheduler (Linux Kernel)   │
    │  Run Queue / Context Switch     │
    │  PSI / Load Average             │
    └─────────────┬───────────────────┘
                  │
                  ▼
    ┌─────────────────────────────────┐
    │  JVM Runtime                    │
    │  ├─ JIT: Tier 0→4, Code Cache   │
    │  ├─ GC: STW, Concurrent, TTSP   │
    │  ├─ Virtual Thread: M:N 매핑     │
    │  └─ Lock: Monitor/AQS/CAS       │
    └─────────────┬───────────────────┘
                  │
                  ▼
    ┌─────────────────────────────────┐
    │  Framework (Netty/Spring)       │
    │  Event Loop / Thread Pool       │
    │  HikariCP Connection Pool       │
    └─────────────┬───────────────────┘
                  │
                  ▼
    ┌─────────────────────────────────┐
    │  Application                    │
    │  결제 로직 / 암호화 / 직렬화      │
    └─────────────────────────────────┘
```

---

*본 문서는 JVM Runtime 스택을 FinTech 결제 시스템 SRE 관점에서 CPU 자원 기준으로 E2E 분석한 기술 명세서입니다.*  
*기준 스택: OpenJDK 21/25 LTS · ZGC · Virtual Threads (Project Loom) · Netty · Spring Boot 3.x*

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*