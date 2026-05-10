# lscpu로 읽는 CPU 구조 — SRE 실무 가이드

**분류** : Infrastructure > Compute > CPU Analysis  
**작성자** : SRE Team  
**최종 수정** : 2026-05-06  
**대상 환경** : Intel i9-13900 / Microsoft Hyper-V / vm-01, vm-02

</br>

## 이 문서를 읽어야 하는 사람

- VM을 생성할 때 vCPU를 얼마나 할당해야 할지 기준이 없는 분
- lscpu 결과를 보고 어떤 항목이 중요한지 모르겠는 분
- SRE 관점에서 CPU 자원을 어떻게 설계하고 모니터링해야 하는지 궁금한 분
- [Kubernetes 환경에서 CPU Throttling 장애](../20-deep-dive/lscpu-runtime-analysis-guide/kubernetes-cpu-throttling.md)를 겪어본 분
- WebFlux / Netty 기반 서비스에서 Latency 원인을 찾고 있는 분
- "CPU 사용률은 낮은데 왜 느리지?" 라는 상황을 경험한 분

</br>

## 먼저 이해해야 하는 CPU 계층 구조

<details>
  <summary>1. Physical Core — CPU 내부의 실제 연산 하드웨어</summary>

<br/>

## Physical Core 란

Physical Core는 CPU 칩 내부에 존재하는 **실제 연산 하드웨어**입니다.

CPU 칩 안에 실제로 존재하는 물리 회로입니다.

쉽게 말하면:

```text
물리 Core 8개 CPU
= CPU 칩 안에 실제로 연산 가능한 두뇌 8개 존재
```

## Physical Core 내부 구성

Physical Core 내부에는 실제로 다음이 존재합니다:

| 구성 요소 | 역할 |
|---|---|
| **ALU** (Arithmetic Logic Unit) | 정수 연산 |
| **FPU** (Floating Point Unit) | 부동소수점 연산 |
| **[Load/Store Unit](../20-deep-dive/lscpu-runtime-analysis-guide/lsu-and-memory-access.md)** | 메모리 접근 |
| **[Branch Predictor](../20-deep-dive/lscpu-runtime-analysis-guide/branch-predictor-and-execution-flow.md)** | 분기 처리 |
| **[L1 / L2 Cache](../20-deep-dive/lscpu-runtime-analysis-guide/cpu-cache-and-memory-hierarchy.md)** | 고속 메모리 캐시 |
| **[Pipeline](../20-deep-dive/lscpu-runtime-analysis-guide/cpu-pipeline-and-instruction-flow.md)** | 명령어 실행 파이프라인 |
| **[Register](../20-deep-dive/lscpu-runtime-analysis-guide/cpu-register-and-execution-flow.md)** | 연산 임시 저장 |

즉 Physical Core는:

```text
CPU 명령어를 실제 실행하는 하드웨어 회로
```

입니다.

## Multi-core 란

Multi-core는 **Physical Core 자체가 증가**하는 것입니다.

```text
Physical Core 4개 = 동시에 실제 연산 가능한 하드웨어 4개
```

예:

```text
4 Core CPU
→ 동시에 실제 연산 가능한 물리 코어 4개 존재
```

## Physical Core vs Logical CPU — 핵심 구분

| 개념 | 의미 |
|---|---|
| **Physical Core** | 실제 연산 하드웨어 (ALU, Cache 등 포함) |
| **Logical CPU** | OS/Linux [Kernel](../20-deep-dive/lscpu-runtime-analysis-guide/kernel-and-cpu-topology.md)이 인식하는 CPU 실행 단위 |

> **중요:** Physical Core = [Hardware Thread](../20-deep-dive/lscpu-runtime-analysis-guide/hardware-thread-and-cpu-topology.md) 1개 라고 표현하면 오해가 생깁니다.  
> [Physical Core와 Hyper-thread](../20-deep-dive/lscpu-runtime-analysis-guide/physical-core-and-logical-cpu.md)(≈ Logical CPU)는 서로 다른 계층의 개념입니다.
>
> 하이퍼스레딩(Hyper-threading)은 논리적 분할 '기술'이고, 논리 CPU(Logical CPU)는 그 결과로 생성된 실체적 '작업 단위'입니다.

## HT/SMT 비활성화 시 구조

HT/SMT가 비활성화된 경우:

```text
Physical Core 1개
→ Logical CPU 1개
```

즉:

```text
Physical Core = Logical CPU
```

가 됩니다.

```bash
# lscpu 출력 예
Thread(s) per core: 1
```

## Multi-core + HT OFF 구조

```text
Physical Core 4개
HT OFF
→ Logical CPU 4개
```

| 항목 | 증가 대상 |
|---|---|
| **Multi-core** | Physical Core 증가 |
| **HT/SMT** | Logical CPU 증가 |

</br>

</details>

<details>
  <summary>2. HT/SMT 와 Logical CPU — 물리 코어 효율을 높이는 하드웨어 기술</summary>

<br/>

## HT/SMT 란

HT(Hyper-Threading, Intel) / SMT(Simultaneous Multithreading, AMD)는:

```text
물리 Core 1개를 Logical CPU 2개처럼 보이게 하는 기술
```

입니다.

중요한 점:

```text
새로운 Physical Core가 추가되는 것이 아닙니다.
```

HT/SMT는 Physical Core 내부 자원을 논리적으로 분할해서 동시에 더 효율적으로 사용하려는 기술입니다.

## HT/SMT 활성화 구조

```text
Physical Core 1개
├── Hyper-thread 0  (Logical CPU 0)
└── Hyper-thread 1  (Logical CPU 1)
```

즉:

```text
물리 Core 1개 안에 Logical CPU 2개 존재
```

Linux는 이를 Logical CPU 2개로 인식합니다.

## HT/SMT 활성화 예시

```bash
lscpu
```

출력:

```text
CPU(s):              16
Thread(s) per core:  2
Core(s) per socket:  8
Socket(s):           1
```
[Core(s) per socket](../20-deep-dive/lscpu-runtime-analysis-guide/cpu-socket-and-multi-socket-architecture.md)

</br>

→ 물리 Core 8개 기반의 Logical CPU 16개 구조

## HT OFF vs HT ON 비교

**HT OFF:**

```text
Physical Core 1개
└── Logical CPU 1개
```

**HT ON:**

```text
Physical Core 1개
├── Hyper-thread 0
└── Hyper-thread 1
```

## Multi-core + HT ON 구조

```text
Physical Core 4개
HT ON
→ Logical CPU 8개
```

상세 구조:

```text
Core 0
├── HT 0  (Logical CPU 0)
└── HT 1  (Logical CPU 1)

Core 1
├── HT 0  (Logical CPU 2)
└── HT 1  (Logical CPU 3)

Core 2
├── HT 0  (Logical CPU 4)
└── HT 1  (Logical CPU 5)

Core 3
├── HT 0  (Logical CPU 6)
└── HT 1  (Logical CPU 7)
```

## HT ON = 성능 2배가 아닌 이유

HT/SMT는 Physical Core 내부 자원(ALU, Cache, Execution Unit 등)을 **공유**합니다.

따라서:

```text
HT ON = 성능 2배 (X)
HT ON = 특정 워크로드에서 처리량 향상 (O)
```

실제로는 [워크로드 특성에 따라 성능 향상](../20-deep-dive/lscpu-runtime-analysis-guide/hyper-threading-and-throughput-optimization.md) 폭이 다릅니다.

## 개념 정리

| 개념 | 계층 | 의미 |
|---|---|---|
| **Physical Core** | 하드웨어 | 실제 연산 하드웨어 |
| **Hyper-thread / SMT** | 하드웨어 | 하나의 코어를 논리적으로 분할하는 기술 |
| **Logical CPU** | 논리/OS | Linux Kernel이 인식하는 CPU 실행 단위 |
| **Multi-core** | 하드웨어 | Physical Core 증가 |

</br>

</details>

<details>
  <summary>3. Software Thread 와 Linux Scheduler(CFS) — OS / JVM / Application 실행 흐름</summary>

<br/>

## Software Thread 란

Software Thread는 [프로그램 실행 흐름](../20-deep-dive/lscpu-runtime-analysis-guide/software-thread-and-execution-flow.md)입니다.

CPU 내부 구조가 아니라 OS / JVM / Application 계층의 개념입니다.

JVM 환경의 주요 Software Thread:

| 종류 | 예시 |
|---|---|
| [Java Main Thread](../20-deep-dive/lscpu-runtime-analysis-guide/java-main-thread-and-cfs-scheduler.md) | public static void main(...) |
| **Java Thread** | [new Thread(...)](../20-deep-dive/lscpu-runtime-analysis-guide/java-thread-pool-and-kernel-thread.md) |
| **Netty Event Loop Thread** | [Netty I/O 처리 흐름](../20-deep-dive/lscpu-runtime-analysis-guide/netty-event-loop-and-network-io.md) |
| **Kafka Consumer Thread** | [Kafka 메시지 소비 흐름](../20-deep-dive/lscpu-runtime-analysis-guide/kafka-consumer-thread-and-message-flow.md) |
| **GC Thread** | [JVM Garbage Collector 흐름](../20-deep-dive/lscpu-runtime-analysis-guide/jvm-gc-thread-and-garbage-collection-flow.md) |
| [Worker Thread](../20-deep-dive/lscpu-runtime-analysis-guide/worker-thread-and-thread-pool-flow.md) | Thread Pool 내 처리 흐름 |
| [POSIX Thread (pthread)](../20-deep-dive/lscpu-runtime-analysis-guide/pthread-jvm-and-kernel-virtualization-stack.md) | Linux 기반 스레드 |

[Java Thread와 Netty Event Loop Thread 구조 비교](../20-deep-dive/lscpu-runtime-analysis-guide/java-thread-vs-netty-event-loop.md)

</br>

> Go Runtime의 Go routine은 Java Thread와 다른 실행 모델을 사용합니다.

| 종류 | 예시 |
|---|---|
| [Go Routine 스케줄 대상](../20-deep-dive/lscpu-runtime-analysis-guide/goroutine-and-go-runtime-scheduler.md) | Go Runtime 실행 흐름 |

</br>

이들은 전부 **Software 계층 실행 흐름**입니다.

## Hyper-thread vs Software Thread 핵심 구분

Thread라는 단어가 두 계층에 모두 사용되기 때문에 혼동이 생길 수 있습니다.

| 종류 | 계층 | 실제 의미 |
|------|------|-----------|
| **Hyper-thread** | 하드웨어 | Physical Core 하나를 두 개의 실행 통로로 나누는 기술, 또는 그 물리적 통로 자체 |
| **Logical CPU** | OS Kernel | [Linux Kernel이 스케줄링 가능한 CPU 단위](../20-deep-dive/lscpu-runtime-analysis-guide/linux-cfs-scheduler-and-hardware-context.md)로 인식할 때 부르는 명칭 |
| **Software Thread** | OS / JVM / Application | 프로그램 실행 흐름 (Java Thread, Go Routine, pthread 등) |

> Hyper-thread(하드웨어)와 Logical CPU(OS)는 같은 대상을 가리키며, '[누가 무엇으로 보는가](../20-deep-dive/lscpu-runtime-analysis-guide/hyper-thread-and-logical-cpu.md)'라는 관점의 차이만 있습니다. </br>
> HT가 활성화된 Physical Core 하나는 OS에 Logical CPU 2개로 노출됩니다.

</br>

## Linux 에서의 [Thread와 task_struct](../20-deep-dive/lscpu-runtime-analysis-guide/java-thread-to-linux-task-struct.md)

Linux Kernel은 [Process와 Thread](../20-deep-dive/lscpu-runtime-analysis-guide/linux-task-struct-and-pid-tid-model.md)를 내부적으로 동일한 실행 단위(Task) 관점에서 관리합니다.

Linux에서 [OS Thread(Linux Thread)](../20-deep-dive/lscpu-runtime-analysis-guide/os-thread-and-linux-thread.md)의 실체는 task_struct 기반의 LWP(Lightweight Process)이며, CFS Scheduler는 이러한 task 단위를 실제 스케줄링 대상으로 관리합니다.












> 

start() 호출 이후 실제 실행 단위가 생성된다. JVM Native Layer → pthread_create() → clone() 경로를 거쳐 Linux Kernel 내부에 task_struct/LWP로 실체화된다.

```
new Thread()   →  JVM Heap에 Thread 객체 생성 (OS Thread 없음)
thread.start() →  JVM Native Layer → pthread_create() → clone() → task_struct 생성
```

Linux Kernel은 각 Thread를 task_struct로 관리하며, Thread의 실체는 **자원을 공유하는 task_struct**, 즉 LWP(Lightweight Process)이다.

CFS Scheduler는 task_struct가 Runnable 상태일 때 Runqueue에 등록하고, vruntime 기준으로 실행 대상을 선택해 Logical CPU에 배치한다. 이후 Physical Core에서 실제 연산이 수행된다.

```
task_struct (LWP)
  ↓ CFS Runqueue 등록 (Runnable 상태)
  ↓ vruntime 기준 선택
  ↓ Logical CPU 배치
  ↓ Physical Core 실행
```

Java Thread, Worker Thread, Kafka Consumer Thread 같은 고수준 Software Thread는 모두 커널 수준에서 CFS가 스케줄링하는 task_struct로 연결된다.

이 구조를 이해하면 아래 운영 이슈를 Linux 실행 단위 관점에서 분석할 수 있다.

| 현상 | 원인 |
|------|------|
| Context Switching 증가 | Runnable task_struct 수 과다 |
| Load Average 증가 | Runqueue 적체 |
| unable to create new native thread | task_struct 생성 한도 도달 (threads-max, ulimit -u, cgroup pids.max) |
| CPU Throttling | cgroup CPU Quota 소진으로 Runnable task 실행 제한 |

















## Software Thread 실행 계층 구조

Software Thread는 결국 Linux Scheduler(CFS)에 의해 Logical CPU 위에서 실행됩니다.

```text
Application
└── Java Thread
    └── Linux Thread(Task)
        └── Scheduler(CFS)
            └── Logical CPU(Hyper-thread)
                └── Physical Core
```

---

## Logical CPU 1개 위의 Software Thread 구조

```text
Hyper-thread(Logical CPU) 1개
└── Scheduler(CFS)
     ├── Java Thread
     ├── Netty Thread
     ├── Kafka Thread
     ├── GC Thread
     └── Worker Thread
```

여러 Software Thread가 동일한 Logical CPU 위에서 실행될 수 있습니다.

---

## Linux Scheduler(CFS) 역할

Linux Scheduler(CFS, Completely Fair Scheduler)는:

```text
Software Thread들을 Logical CPU 위에 배치하고,
CPU Time을 분배/계산한다
```

즉:

- 어떤 Thread를 실행할지 결정
- CPU Time 분배
- Running 상태 전환 관리
- Runnable Queue 기반 스케줄링

---

## Running / Runnable / Context Switch

| 상태 | 의미 |
|---|---|
| **Running** | 현재 Logical CPU 위에서 실행 중 |
| **Runnable** | 실행 대기 중 (CPU 할당 기다리는 상태) |
| **Blocked** | I/O 대기 등으로 대기 중 |

**핵심:**

```text
Hyper-thread(Logical CPU) 1개
= 동시에 Running 가능한 Thread 1개
```

Runnable 상태의 Software Thread는 훨씬 많이 존재할 수 있습니다.

예:

```text
Logical CPU 1개
Software Thread 100개

→ 1개 Running
→ 99개 Runnable (실행 대기)
```

Linux Scheduler(CFS)는 Runnable Queue 기반으로 실행 대상이 변경될 때:

```text
Context Switch
```

가 발생할 수 있습니다.

---

## CPU Saturation 과 Scheduler Overhead

Runnable Thread가 과도하게 증가하면:

- Runnable Queue 증가
- Context Switch 증가
- CPU Cache Miss 증가
- Scheduler Overhead 증가
- CPU Saturation 발생

특히 다음 구조에서 이러한 현상이 심해집니다:

- Thread-per-request 구조
- Blocking I/O 사용
- 과도한 Thread Pool 설정

---

## 멀티코어 / 멀티스레드 / HT 개념 최종 정리

| 개념 | 계층 | 의미 |
|---|---|---|
| **Physical Core** | 하드웨어 | 실제 물리 연산 하드웨어 |
| **HT/SMT** | 하드웨어 기술 | 물리 코어 효율을 높이는 논리적 병렬 처리 |
| **Logical CPU** | OS 인식 | Linux Kernel이 인식하는 CPU 실행 단위 |
| **Multi-core** | 하드웨어 구조 | 물리적인 병렬 처리 |
| **Multi-thread** | 소프트웨어 구조 | 소프트웨어 실행 흐름의 병렬 처리 |
| **Software Thread** | OS / JVM / App | 프로그램 실행 흐름 |

</details>

<details>
  <summary>4. WebFlux / Netty Event Loop 와 CPU 효율 — Software Thread 수와 Logical CPU 관계</summary>

<br/>

## Thread-per-request vs Event Loop

전통적인 Blocking 구조:

```text
Thread-per-request
→ 요청마다 Thread 1개 할당
→ I/O 대기 중에도 Thread 점유
→ Thread 수 증가 → Context Switch 증가
→ CPU Saturation 위험
```

Spring WebFlux / Netty 구조:

```text
적은 수의 Software Thread
→ Logical CPU(Hyper-thread)를 효율적으로 사용
→ Thread 수 감소
→ Context Switch 감소
→ CPU Cache 효율 증가
→ 높은 처리량 유지
```

---

## Netty Event Loop Thread 구조

Netty는 기본적으로:

```text
Event Loop Thread 수 = Logical CPU 수 × 2
```

로 설정됩니다.

구조:

```text
Logical CPU 8개
→ Event Loop Thread 16개
```

각 Event Loop Thread는:

```text
1개의 Event Loop Thread
└── 다수의 Channel(Connection) 처리
    → Non-blocking I/O
    → Callback 기반
```

---

## Event Loop Block 문제

Event Loop Thread에서 Blocking 작업이 발생하면:

```text
Event Loop Block
→ 해당 Thread가 담당하는 모든 Channel 처리 지연
→ Latency 급증
→ Timeout 증가
```

따라서 WebFlux / Netty 환경에서는:

```text
Event Loop Thread 내에서 Blocking 작업 금지
```

가 원칙입니다.

Blocking 작업이 필요한 경우:

```text
Schedulers.boundedElastic()
→ 별도 Blocking Worker Thread Pool로 위임
```

---

## CPU 관점 성능 지표

Runtime Latency에 직접 영향을 주는 CPU 관련 지표:

| 지표 | 의미 | 위험 신호 |
|---|---|---|
| **CPU Saturation** | Runnable Queue 증가 | `vmstat r` 컬럼 지속 증가 |
| **CPU Throttling** | cgroup CPU Quota 초과 | `container_cpu_cfs_throttled_seconds_total` 증가 |
| **Context Switch** | Thread 전환 빈도 | `vmstat cs` 컬럼 급증 |
| **Event Loop Block** | Event Loop Thread 점유 | Latency 급증, Thread Dump 확인 |
| **Runnable Queue** | 실행 대기 Thread 수 | `vmstat r` 또는 `top` 확인 |

---

## WebFlux 환경 핵심 원칙

```text
Thread 수 감소
→ Context Switch 감소
→ CPU Cache 효율 증가
→ 높은 처리량 유지
```

따라서:

- CPU Saturation 발생 시 → Thread Pool 크기, Blocking 여부 확인
- CPU Throttling 발생 시 → Kubernetes CPU Limit 확인
- Event Loop Block 발생 시 → Thread Dump, Blocking 코드 확인
- Runnable Queue 증가 시 → Thread 수, Blocking I/O 확인

</details>

<details>
  <summary>5. Kubernetes CPU Quota 와 Logical CPU — cgroup 기반 CPU 제어</summary>

<br/>

## Kubernetes CPU 단위

Kubernetes는 일반적으로 **Logical CPU(vCPU)** 기준으로 동작합니다.

```yaml
resources:
  requests:
    cpu: "500m"
  limits:
    cpu: "2000m"
```

| 단위 | 의미 |
|---|---|
| `1000m` (1 core) | Logical CPU(vCPU) 1개 분량 Quota |
| `2000m` (2 core) | Logical CPU(vCPU) 2개 분량 Quota |
| `500m` | Logical CPU(vCPU) 0.5개 분량 Quota |

---

## cgroup CPU Throttling

CPU Limit을 초과하면 cgroup이 CPU 사용을 제한합니다.

```text
CPU Limit 초과
→ cgroup CPU Throttling
→ Pod/Container CPU 사용 제한
→ Latency 증가
→ Timeout 위험
```

확인 지표:

```text
container_cpu_cfs_throttled_seconds_total
container_cpu_cfs_periods_total
```

Throttling 비율:

```text
throttled_seconds / periods × 100 = Throttling %
```

---

## CPU Requests vs Limits 실무 전략

| 항목 | 역할 | 실무 고려사항 |
|---|---|---|
| **CPU Requests** | Scheduling 기준 (Node 배치 결정) | 실제 평균 사용량 기준으로 설정 |
| **CPU Limits** | cgroup Throttling 기준 | 너무 낮으면 Throttling 위험 |

> FinTech / 결제 시스템에서는 CPU Throttling이 결제 Latency 증가로 직결될 수 있습니다.

---

## Kubernetes CPU Manager (static policy)

일부 환경에서는 CPU Pinning이 필요합니다.

```yaml
# kubelet 설정
cpuManagerPolicy: static
```

CPU Pinning / CPU Affinity 사용 환경:

- DPDK
- HFT (High Frequency Trading)
- RT System (Real-time)
- Kubernetes CPU Manager (static policy)

이 경우:

```text
특정 Logical CPU를 특정 Pod/Thread에 고정
→ Context Switch 최소화
→ Cache Locality 향상
```

---

## Kubernetes 관점 계층 구조

```text
Kubernetes Pod
└── Container
    └── cgroup CPU Quota
        └── Logical CPU(vCPU)
            └── Linux Scheduler(CFS)
                └── Software Thread
                    └── Physical Core
```

</details>

<details>
  <summary>6. Cloud(vCPU) 와 최신 CPU 구조 — AWS EC2 / Hybrid Core / NUMA</summary>

<br/>

## AWS EC2 vCPU

Cloud에서는 `vCPU` 표현을 사용합니다.

**AWS 기준 일반적으로:**

```text
vCPU 1개 ≈ Hyper-thread(Logical CPU) 1개
```

즉:

```text
2 vCPU 인스턴스 = Logical CPU 2개
```

를 의미합니다.

---

## AWS Nitro Hypervisor 추상화

AWS에서는 실제 물리 구조를 Nitro Hypervisor가 추상화합니다.

```text
사용자가 실제 어느 Physical Core 쓰는지 알 수 없음
```

Cloud에서는:

```text
vCPU 단위 계약
```

이 중요합니다.

---

## Hyper-thread / Logical CPU / vCPU 관계

일반적인 HT/SMT 활성화 환경에서는:

```text
Logical CPU ≒ vCPU ≒ Hyper-thread
```

처럼 동작합니다.

| 구분 | 의미 |
|---|---|
| **Hyper-thread / SMT** | CPU 하드웨어의 논리 실행 단위 |
| **Logical CPU** | Linux Kernel이 인식하는 CPU 실행 단위 |
| **vCPU** | VM / Kubernetes에서 사용하는 CPU 단위 |
| **Software Thread** | JVM / OS / Application의 실행 흐름 |

---

## 최신 CPU — P-core / E-core Hybrid 구조

Intel Core i9-13900K 같은 최신 Intel CPU는 **Hybrid Architecture**를 사용합니다.

| 코어 종류 | 특징 |
|---|---|
| **P-Core** (Performance Core) | HT 지원 → P-Core 1개 = Logical CPU 2개 |
| **E-Core** (Efficiency Core) | HT 미지원 → E-Core 1개 = Logical CPU 1개 |

**Intel Core i9-13900K 예시:**

```text
P-Core 8개 × HT2  = Logical CPU 16개
E-Core 16개 × HT1 = Logical CPU 16개
                  ↓
총 Logical CPU 32개
```

> 최신 CPU에서는 **모든 Core가 동일하지 않습니다.**

---

## NUMA (Non-Uniform Memory Access)

서버급 환경에서는 NUMA 구조도 중요합니다.

```text
CPU Socket 2개
→ NUMA Node 2개 가능
```

각 CPU가:
- 자체 Memory Controller 보유
- 자체 Local RAM 접근 보유

즉:

```text
Logical CPU 증가
+ 메모리 접근 topology 복잡화 발생
```

NUMA 구조:

```text
NUMA Node 0
└── CPU Socket 0
    ├── Physical Core 0~N
    └── Local Memory

NUMA Node 1
└── CPU Socket 1
    ├── Physical Core 0~N
    └── Local Memory
```

Cross-NUMA 접근(Remote Memory)은 Local 접근보다 지연이 발생합니다.

---

## CPU 구조 기술 전체 정리

| 기술 | 증가/변화 대상 | 특징 |
|---|---|---|
| **Multi-core** | Physical Core 증가 | 실제 연산 하드웨어 증가 |
| **HT/SMT** | Logical CPU 증가 | Physical Core 효율 향상 |
| **Hybrid(P/E Core)** | Core 종류 분리 | 성능/효율 Core 혼합 |
| **NUMA** | CPU/Memory topology 분리 | 소켓 간 메모리 접근 비용 |
| **Hypervisor(vCPU)** | Logical CPU 가상화 | Cloud 추상화 |
| **CPU Pinning** | 특정 Logical CPU 고정 | Context Switch 최소화 |

</details>

<details>
  <summary>7. 전체 계층 구조 정리 — Physical Core 부터 Runtime 까지</summary>

<br/>

## 전체 계층 구조

```text
Application (Java / Netty / Kafka)
└── Software Thread (Java Thread / Event Loop / Worker)
    └── Linux Thread (task_struct)
        └── Linux Scheduler (CFS)
            ├── Runnable Queue 관리
            ├── CPU Time 분배
            └── Context Switch
                └── Logical CPU (Hyper-thread / vCPU)
                    └── Physical Core
                        ├── ALU / FPU
                        ├── Pipeline
                        ├── L1 / L2 Cache
                        └── Register
```

---

## Kubernetes 포함 전체 구조

```text
Kubernetes Pod
└── Container
    └── cgroup CPU Quota (Requests / Limits)
        └── vCPU (Logical CPU)
            └── Linux Scheduler (CFS)
                └── Software Thread
                    └── Logical CPU (Hyper-thread)
                        └── Physical Core
```

---

## Cloud 포함 전체 구조

```text
AWS EC2 Instance
└── Nitro Hypervisor
    └── vCPU (≒ Logical CPU ≒ Hyper-thread)
        └── Physical Core (Nitro 추상화)
```

---

## 계층별 문제 진단 가이드

| 계층 | 문제 현상 | 확인 방법 |
|---|---|---|
| **Physical Core** | 물리 CPU 부족 | `lscpu`, 인스턴스 스펙 확인 |
| **Logical CPU / HT** | Logical CPU 수 부족 | `lscpu` Thread(s) per core 확인 |
| **Linux Scheduler** | Context Switch 급증, Runnable Queue 증가 | `vmstat`, `pidstat` |
| **Software Thread** | Thread 수 과다, Blocking I/O | Thread Dump, `jstack` |
| **Kubernetes cgroup** | CPU Throttling | `container_cpu_cfs_throttled_seconds_total` |
| **Event Loop** | Event Loop Block | Thread Dump, Latency 급증 |
| **vCPU / Cloud** | vCPU 할당 부족 | 인스턴스 타입 확인, CloudWatch |

---

## SRE 실무 판단 흐름

```text
Latency 증가 / Timeout 발생
        ↓
CPU 관련 여부 확인
        ↓
┌──────────────────────────────────────┐
│ CPU Throttling?                      │
│ → Kubernetes CPU Limit 확인          │
│ → container_cpu_cfs_throttled 지표   │
└──────────────────────────────────────┘
        ↓
┌──────────────────────────────────────┐
│ Runnable Queue 증가?                 │
│ → vmstat r 컬럼 확인                 │
│ → Thread 수, Blocking I/O 확인       │
└──────────────────────────────────────┘
        ↓
┌──────────────────────────────────────┐
│ Event Loop Block?                    │
│ → Thread Dump 확인                   │
│ → Blocking 코드 위치 확인            │
└──────────────────────────────────────┘
        ↓
┌──────────────────────────────────────┐
│ Context Switch 급증?                 │
│ → Thread Pool 크기 확인              │
│ → Thread-per-request 구조 여부       │
└──────────────────────────────────────┘
```

---

## 계층별 개념 최종 정리

| 개념 | 계층 | 의미 |
|---|---|---|
| **Physical Core** | 하드웨어 | 실제 물리 연산 하드웨어 (ALU, Cache 등) |
| **HT/SMT** | 하드웨어 기술 | 물리 코어 효율을 높이는 논리적 병렬 처리 |
| **Logical CPU** | OS 인식 | Linux Kernel이 인식하는 CPU 실행 단위 |
| **Multi-core** | 하드웨어 구조 | 물리적인 병렬 처리 |
| **Multi-thread** | 소프트웨어 구조 | 소프트웨어 실행 흐름의 병렬 처리 |
| **Software Thread** | OS / JVM / App | 프로그램 실행 흐름 |
| **Linux Scheduler(CFS)** | OS | Software Thread → Logical CPU 배치 및 CPU Time 분배 |
| **vCPU** | Cloud / VM | Logical CPU의 가상화 단위 |
| **CPU Throttling** | Kubernetes | cgroup CPU Quota 초과 시 CPU 사용 제한 |
| **Context Switch** | OS | Logical CPU 위 실행 Thread 교체 |
| **Event Loop Block** | Runtime | Event Loop Thread 점유로 인한 처리 지연 |
| **NUMA** | 서버 하드웨어 | CPU Socket 간 메모리 접근 topology |

</details>

</br>

## 핵심 요약 (TL;DR)

> SRE 설계 기준은 **CPU 사용률 60% 이하 유지**, 즉 여유를 설계 단계에서 계산해야 합니다.  
> **CPU 사용률이 낮아도 Saturation은 발생할 수 있습니다.** 사용률 하나만 보는 모니터링은 불완전합니다.

</br>

## 목차

1. [CPU 계층 구조](#1-cpu-계층-구조)
2. [용어 사전](#2-용어-사전)
3. [lscpu 항목별 해석](#3-lscpu-항목별-해석)
4. [vCPU 할당과 스레드의 관계](#4-vcpu-할당과-스레드의-관계)
5. [vm-01 vs vm-02 비교](#5-vm-01-vs-vm-02-비교)
6. [CPU Bound vs I/O Bound](#6-cpu-bound-vs-io-bound)
7. [SRE 관점 분석](#7-sre-관점-분석)
8. [빠른 참조 공식](#8-빠른-참조-공식)

</br>

## 1. CPU 계층 구조

CPU를 이야기할 때 "CPU 1개"라는 표현은 문맥에 따라 전혀 다른 의미를 가집니다.  
아래 계층 구조를 먼저 이해하면 모든 혼란이 정리됩니다.

```
물리 CPU (프로세서 / 소켓)          ← 메인보드에 꽂혀있는 칩 1개
└── 물리 코어 (Core)                ← 칩 안의 독립적인 연산 단위
    ├── 스레드 0  =  논리 CPU  =  vCPU
    └── 스레드 1  =  논리 CPU  =  vCPU   ← 하이퍼스레딩(HT) 활성화 시
```

### 실제 사례 — Intel i9-13900

| 단위 | 수량 |
|------|------|
| 물리 CPU (소켓) | 1개 |
| 물리 코어 | 24개 |
| 스레드 (최대) | 48개 |

> **착각 주의** : "CPU 1개 할당했으니 물리 코어 1개다"는 틀린 이해입니다.  
> 물리 CPU 1개 안에 코어가 24개 들어있고, 각 코어가 스레드 2개를 가질 수 있습니다.

### 멀티코어 환경 예시

실제 서버 환경에서는 `Core(s) per socket`이 1보다 훨씬 큰 경우가 일반적입니다.

```bash
# 8코어 HT 서버 예시
Socket(s)          : 1
Core(s) per socket : 8
Thread(s) per core : 2
→ 1 × 8 × 2 = 16 vCPU

# 듀얼 소켓 서버 예시
Socket(s)          : 2
Core(s) per socket : 12
Thread(s) per core : 2
→ 2 × 12 × 2 = 48 vCPU
```

</br>

## 2. 용어 사전

| 용어 | 동의어 | 한 줄 설명 |
|------|--------|-----------|
| CPU | 프로세서, 소켓 | 물리적으로 메인보드에 꽂혀있는 칩 |
| 코어 (Core) | 물리 코어 | CPU 칩 안의 독립적인 연산 단위 |
| 스레드 (Thread) | 논리 CPU, vCPU | 하이퍼스레딩(HT)으로 나눈 실행 단위 |
| vCPU | 논리 CPU = 스레드 | 클라우드 VM에 할당되는 CPU 단위 |
| HT / SMT | 하이퍼스레딩 | 물리 코어 1개를 논리 CPU 2개로 노출하는 기술 |
| NUMA | Non-Uniform Memory Access | CPU 소켓별 메모리 접근 구역 |
| Load Average | - | 실행 중 + 대기 + I/O Wait 프로세스 수의 평균 |
| Runnable Queue | r | CPU 할당을 기다리는 프로세스 수 (`vmstat`의 `r` 컬럼) |
| Steal Time | %steal | VM이 CPU를 원했지만 하이퍼바이저에 의해 대기한 시간 비율 |
| CPU Throttling | CFS Throttling | Kubernetes가 CPU Limit 초과 시 강제로 CPU를 제한하는 동작 |
| Context Switch | CS | CPU가 현재 작업을 저장하고 다른 작업으로 전환하는 행위 |
| IRQ / SoftIRQ | 인터럽트 | 하드웨어 이벤트(네트워크, 디스크)가 CPU에 처리를 요청하는 메커니즘 |
| PSI | Pressure Stall Information | CPU·Memory·I/O 자원 압박 정도를 나타내는 Linux 커널 지표 |
| CPU Affinity | CPU Pinning | 특정 프로세스를 특정 CPU 코어에 고정하는 설정 |

</br>

## 3. lscpu 항목별 해석

`lscpu`를 실행하면 많은 항목이 나오지만, SRE 관점에서 실제로 중요한 항목은 다음과 같습니다.

### 3-1. 핵심 항목

| 항목 | 예시값 | 의미 |
|------|--------|------|
| `CPU(s)` | 2 | **논리 CPU 총 수** = 스레드 수 = vCPU 수 |
| `Thread(s) per core` | 2 | 코어당 스레드 수. **2이면 HT 활성화** |
| `Core(s) per socket` | 1 | 소켓당 물리 코어 수 |
| `Socket(s)` | 1 | 물리 CPU(소켓) 수 |
| `CPU MHz` | 2758.728 | 현재 동작 클럭. 낮으면 절전 상태 또는 하이퍼바이저 제한 |
| `Hypervisor vendor` | Microsoft | 가상화 플랫폼. Microsoft = Hyper-V = Azure 환경 |
| `Virtualization type` | full | 완전 가상화. 하드웨어 수준 격리 |

### 3-2. 캐시 항목

| 항목 | 예시값 | 접근 속도 | 설명 |
|------|--------|----------|------|
| `L1d cache` | 48K | ~1ns | 데이터 캐시. 가장 빠름 |
| `L1i cache` | 32K | ~1ns | 명령어 캐시 |
| `L2 cache` | 2MB | ~5ns | L1 미스 시 조회 |
| `L3 cache` | 36MB | ~20ns | 코어 간 공유 캐시 |

**캐시 접근 속도 순서**

```
L1 → L2 → L3 → RAM  → Disk
빠름                        느림
~1ns  ~5ns  ~20ns  ~100ns   ~ms
```

> L1 캐시 히트율이 응답 속도(Latency)에 직결됩니다.  
> 캐시 미스가 잦으면 RAM 접근이 늘고 응답 속도가 크게 나빠집니다.  
> Context Switch가 증가하면 CPU Cache가 오염(Cache Pollution)되어 Cache Miss율이 함께 올라갑니다.

</br>

## 4. vCPU 할당과 스레드의 관계

### 핵심 원칙

> **VM 생성 시 vCPU 할당 = 스레드 몇 개를 이 VM에 노출할 것인가**

### vm-01 (vCPU 1개 할당)

```bash
CPU(s)             : 1
Thread(s) per core : 1
Core(s) per socket : 1
```

물리 코어의 스레드 1개만 VM에 노출한 상태입니다.  
HT가 꺼진 것이 아니라, 하이퍼바이저가 스레드 1개만 보여주는 것입니다.

### vm-02 (vCPU 2개 할당)

```bash
CPU(s)             : 2
Thread(s) per core : 2
Core(s) per socket : 1
```

같은 물리 코어의 스레드 2개 모두를 VM에 노출한 상태입니다.  
HT가 활성화된 것처럼 보이지만, 실제로는 할당 방식의 차이입니다.

### ⚠️ 주의사항

```
vCPU 2개 ≠ 물리 코어 2개
```

같은 물리 코어를 두 스레드가 공유하기 때문에,  
CPU 집약적인 작업이 동시에 몰리면 **실질 성능은 물리 코어 2개보다 낮습니다.**

> **HT 성능 향상 참고** : HT(SMT)는 워크로드 유형에 따라 다르지만,  
> 일반적으로 **15~30% 수준의 처리량 향상**을 기대할 수 있습니다.  
> 단, I/O Bound 워크로드에서 효과가 크고 CPU Bound에서는 제한적입니다.

</br>

## 5. vm-01 vs vm-02 비교

| 항목 | vm-01 | vm-02 |
|------|-------|-------|
| 논리 CPU (vCPU) | 1개 | **2개** |
| 물리 코어 | 1개 | 1개 (동일) |
| HT 노출 여부 | 미노출 | **노출 (Thread×2)** |
| 현재 클럭 | ~1,971 MHz | ~2,758 MHz |
| L3 캐시 | 36MB | 36MB (동일) |
| 가상화 플랫폼 | Hyper-V | Hyper-V (동일) |

</br>

## 6. CPU Bound vs I/O Bound

워크로드 유형을 이해해야 vCPU 할당 전략이 달라집니다.

| 유형 | 특징 | 병목 지점 | HT 효과 |
|------|------|----------|---------|
| **CPU Bound** | 연산량이 많아 CPU가 병목 | 물리 코어 수가 핵심 | 제한적 (15% 이하) |
| **I/O Bound** | 디스크·네트워크 대기가 병목 | 스레드 수(vCPU)가 더 중요 | 효과적 (15~30%) |

### CPU Bound 워크로드 예시

- 암복호화 (TLS handshake, JWT 검증)
- JSON serialization / deserialization
- 데이터 압축
- 이미지·동영상 처리

### I/O Bound 워크로드 예시

- DB 조회 대기
- Kafka 메시지 대기
- Redis 네트워크 호출
- 외부 API 응답 대기

### HT와 워크로드 유형의 관계

> HT(하이퍼스레딩)는 **I/O Bound 워크로드에서 효과가 크고**,  
> **CPU Bound 워크로드에서는 물리 코어 수가 더 중요합니다.**

I/O 대기 중 유휴 상태가 된 스레드 자리를 다른 스레드가 활용할 수 있기 때문입니다.  
반면 CPU 집약적 작업은 두 스레드가 같은 물리 코어를 두고 경쟁하게 되어 효과가 제한적입니다.

</br>

## 7. SRE 관점 분석

### 7-1. CPU Usage ≠ CPU Saturation

> **CPU 사용률이 낮아도 Saturation은 발생할 수 있습니다.**

평균 CPU 사용률만 보는 모니터링은 불완전합니다.  
아래 상황에서는 사용률이 낮아도 실제 병목이 발생합니다.

| 상황 | 사용률 | 실제 상태 |
|------|--------|----------|
| I/O Wait 증가 | 낮음 | 디스크/네트워크 대기로 전체 처리 지연 |
| Runnable Queue 증가 | 낮음처럼 보임 | CPU를 기다리는 작업이 누적됨 |
| CPU Throttling | 낮음 | Kubernetes가 강제로 제한 중 |
| Steal Time 증가 | 낮음 | 하이퍼바이저가 다른 VM에 CPU를 할당 중 |
| Context Switch 폭증 | 낮음처럼 보임 | Scheduler Overhead로 실제 처리량 감소 |

반드시 **사용률 + Load Average + Runnable Queue + Steal Time + Throttling + Context Switch** 를 함께 확인해야 합니다.

### 7-2. 병목 위험 포인트

- vCPU 1~2개 환경에서 CPU bound 작업이 집중되면 즉시 포화 상태에 도달합니다.
- `Thread(s) per core: 2`이더라도 물리 코어는 1개를 공유하므로, 두 스레드가 동시에 무거운 연산을 수행하면 실질 성능이 저하됩니다.
- 모니터링 지표 : `top`, `mpstat`, `perf` 명령어로 per-CPU 사용률을 확인하세요.

```bash
perf stat -p <pid>
perf top
```

### 7-3. Load Average와 Runnable Queue

Linux의 `Load Average`는 단순 CPU 사용률이 아닙니다.  
아래 세 가지 상태의 프로세스 수를 **1분 / 5분 / 15분** 평균으로 나타낸 값입니다.

- **Running** : 현재 CPU에서 실행 중
- **Runnable** : 실행 대기 중 (CPU 할당을 기다리는 중)
- **Uninterruptible Sleep** : 주로 I/O wait 상태

```bash
# Load Average 확인
$ uptime
load average: 1.00, 0.90, 0.85
#             1분    5분   15분

# Runnable Queue 실시간 확인
$ vmstat 1
procs -----------memory---------- ---swap-- -----io---- -system-- ------cpu-----
 r  b   swpd   free   buff  cache   si   so    bi    bo   in   cs us sy id wa st
 3  0      0 1024000  12000 800000    0    0     0     0  500 1200  20  5 70  5  0
 ^
 └─ Runnable Queue : CPU를 기다리는 프로세스 수
```

**Runnable Queue 해석**

> `r` 값이 vCPU 수보다 지속적으로 높다면,  
> CPU를 기다리는 작업이 누적되고 있다는 의미입니다.  
> 이 상태가 지속되면 Latency가 선형이 아닌 급격하게 증가합니다.

```
load average: 1.00 의 의미

1 vCPU 시스템 → CPU 꽉 참 (100% 포화)
2 vCPU 시스템 → 50% 수준 (여유 있음)
4 vCPU 시스템 → 25% 수준 (매우 여유)
```

| Load Average 수준 | vmstat r 값 | 상태 |
|------------------|------------|------|
| < vCPU 수 | ≤ vCPU 수 | 안정 |
| ≈ vCPU 수 | ≈ vCPU 수 | 포화 직전. 모니터링 강화 필요 |
| > vCPU 수 | > vCPU 수 | Saturation 가능성. 즉시 확인 |
| >> vCPU 수 | >> vCPU 수 | Latency 급증. 장애 대응 필요 |

> **주의** : Load Average가 높아도 I/O Wait 비중이 크면 CPU 문제가 아닐 수 있습니다.  
> `top`에서 `wa` (I/O wait)와 `vmstat`의 `b` (blocked) 항목을 함께 확인해야 합니다.

### 7-4. 골든 시그널 기준

SRE의 4가지 골든 시그널(Latency / Traffic / Errors / Saturation) 관점에서 CPU를 해석하면 다음과 같습니다.

| 골든 시그널 | CPU 관점 적용 |
|------------|--------------|
| **Saturation** | CPU 사용률 60% 이하 유지. 버스트 트래픽 대응 여유 확보 |
| **Latency** | L1/L2/L3 캐시 히트율이 응답속도에 직결. 캐시 미스율 모니터링 필수 |
| **Traffic** | vCPU 수 기준으로 동시 처리 가능 스레드 수 계산 후 용량 설계 |
| **Errors** | CPU 스로틀링, OOM 등 자원 포화로 인한 오류 감지 |

### 7-5. 설계 철학 — 2x Work, 60% Load

> 컴퓨터 2대가 할 일을 1대가 CPU 60% 이하로 처리하는 것을 설계 목표로 삼습니다.

**왜 60%인가?**

CPU는 Memory나 Disk와 달리 순간 Burst가 매우 크기 때문입니다.  
평균 80~90%로 운영하면 아래와 같은 순간적인 CPU Spike에도 Saturation이 발생합니다.

- JVM GC
- TLS Handshake
- Kafka Rebalance
- 데이터 압축 / Serialization
- 인증 처리 (JWT 검증 등)
- Context Switch 폭증 구간

SRE에서는 일반적으로 **60~70% 수준을 안전 영역**으로 보며,  
이 여유가 있어야 Spike 발생 시에도 Latency 급증 없이 처리할 수 있습니다.

이 철학을 실현하려면 다음이 전제되어야 합니다.

1. **vCPU 할당 시 스레드 구조 이해** — 논리 CPU 수가 아닌 물리 코어 기준 처리 용량을 산정
2. **HT 노출 여부에 따른 전략 분리** — I/O bound는 HT 효과가 크지만, CPU bound는 물리 코어 수가 핵심
3. **여유 용량을 설계 단계에서 계산** — 운영 중 스케일업이 아니라 설계 시점에 60% 기준을 내재화

### 7-6. Context Switching과 CPU Overhead

CPU는 동시에 여러 스레드를 실행하는 것처럼 보이지만,  
실제로는 매우 빠르게 작업 간 전환(Context Switch)을 수행합니다.

**Context Switch 발생 시 일어나는 일**

```
현재 스레드 상태 저장 (레지스터, 스택 포인터 등)
    ↓
CPU Cache 일부 무효화 (Cache Pollution)
    ↓
다음 스레드 상태 복원
    ↓
Cache Miss 증가 → 메모리 접근 증가 → Latency 증가
```

Context Switch가 과도하게 증가하면 다음 문제가 발생합니다.

- CPU Cache Miss 증가 → 메모리 접근 증가
- Scheduler Overhead 증가 → 실제 처리량 감소
- Event Loop 지연
- Latency P99 증가

**Context Switch가 많아지는 상황**

- Thread-per-Request 모델에서 스레드풀이 과도하게 큰 경우
- JVM Thread 수 증가
- Kafka Consumer 스레드 증가
- 과도한 동기 블로킹 처리

**확인 방법**

```bash
# Context Switch + Runnable Queue 동시 확인
vmstat 1
# r  컬럼 : Runnable Queue (vCPU 수보다 지속적으로 높으면 Saturation 의심)
# cs 컬럼 : 초당 Context Switch 횟수

# 프로세스별 Context Switch 확인
pidstat -w 1
# cswch/s  : 자발적 Context Switch (I/O 대기 등)
# nvcswch/s: 비자발적 Context Switch (CPU 선점 등)
```

> `nvcswch/s` (비자발적 Context Switch)가 높다면  
> CPU 자원이 부족하여 스레드가 강제로 선점당하고 있다는 신호입니다.  
> `r` 값이 vCPU 수를 지속적으로 초과한다면 CPU Saturation이 이미 시작된 것입니다.

### 7-7. PSI — CPU 자원 압박 지표 (Linux 4.20+)

PSI(Pressure Stall Information)는 CPU · Memory · I/O 자원의 압박 정도를  
**실제 작업이 대기한 시간 비율**로 측정하는 Linux 커널 지표입니다.  
최신 Kubernetes, cgroup v2 환경에서 점점 중요해지고 있습니다.

```bash
# CPU 압박 확인
cat /proc/pressure/cpu

# 출력 예시
some avg10=5.23 avg60=3.11 avg300=1.05 total=12345678
full avg10=0.00 avg60=0.00 avg300=0.00 total=0

# some : 일부 작업이 CPU를 기다린 시간 비율 (%)
# full : 모든 작업이 CPU를 기다린 시간 비율 (%) 
# avg10/avg60/avg300 : 10초/60초/300초 평균

# full 값이 증가한다는 것은 "모든 Runnable Task가 CPU를 기다린 시간" 이 존재했다는 의미입니다.
```

| 지표 | 의미 | 기준 |
|------|------|------|
| `some avg10` | 최근 10초간 일부 작업이 CPU 대기한 비율 | 10% 이상이면 주의 |
| `full avg10` | 최근 10초간 전체 작업이 멈춘 비율 | 0% 이상이면 즉시 확인 |

> Load Average나 CPU 사용률로는 잡히지 않는 **미세한 CPU 압박**을 감지할 수 있습니다.  
> Kubernetes cgroup v2 환경에서는 Pod 단위 PSI도 확인 가능합니다.

```bash
# Kubernetes Pod의 CPU PSI 확인 (cgroup v2)
cat /sys/fs/cgroup/kubepods/pod<pod-uid>/.../cpu.pressure
```

### 7-8. Steal Time — 가상화 환경 주의사항

`top`, `vmstat`, `iostat` 등에서 보이는 `%steal`은 가상화 환경에서만 나타나는 지표입니다.

> **Steal Time** : VM이 CPU를 사용하고 싶었지만,  
> 하이퍼바이저가 다른 VM에 CPU를 할당하면서 기다린 시간의 비율

```bash
# top 명령어에서 확인
%Cpu(s): 10.0 us, 2.0 sy, 0.0 ni, 85.0 id, 1.5 wa, 0.0 hi, 0.5 si, 1.0 st
#   us: 유저 영역    sy: 커널 영역    id: 유휴    wa: I/O Wait    st: Steal
```

| %steal 수준 | 의미 |
|------------|------|
| 0~1% | 정상 |
| 1~5% | 경미한 경합. 모니터링 권장 |
| 5% 이상 | 하이퍼바이저 자원 경쟁 가능성. 호스트 점검 필요 |

**높은 Steal Time의 영향**

- 응답 Latency 증가
- JVM GC 지연
- Kafka Consumer Lag 증가
- 타임아웃 증가

> VM 내부 CPU 사용률이 낮은데도 Latency가 높다면, `%steal`을 먼저 확인하세요.  
> VM 외부(하이퍼바이저 레벨) 문제일 수 있습니다.

### 7-9. IRQ / SoftIRQ — 네트워크 고트래픽 환경

네트워크 패킷 처리와 디스크 I/O 완료는 **Interrupt(인터럽트)** 기반으로 CPU에 전달됩니다.

```bash
# top에서 확인
%Cpu(s): 10.0 us, 2.0 sy, 0.0 ni, 85.0 id, 1.5 wa, 0.5 hi, 1.0 si, 0.0 st
#                                                      hi: IRQ   si: SoftIRQ
```

고트래픽 환경(Netty, 결제 API)에서는 다음 문제가 발생할 수 있습니다.

- **IRQ imbalance** : 특정 CPU Core에 인터럽트가 집중되어 해당 코어 과부하
- **SoftIRQ 증가** : 네트워크 패킷 처리가 많아지며 SoftIRQ 처리 시간 증가
- **NIC Queue 편중** : 네트워크 카드의 수신 큐가 특정 코어에만 매핑

**확인 방법**

```bash
# 코어별 인터럽트 분포 확인
cat /proc/interrupts

# SoftIRQ 상세 확인
cat /proc/softirqs

# 특정 코어 과부하 확인
mpstat -P ALL 1
```

> Kubernetes Node에서 `si` (SoftIRQ) 수치가 지속적으로 높다면  
> NIC Queue를 여러 코어에 분산하는 RSS(Receive Side Scaling) 설정을 검토하세요.

### 7-10. Kubernetes CPU Throttling

Kubernetes 환경에서 CPU Limit를 설정하면 Linux CFS(Completely Fair Scheduler)가 동작합니다.  
Limit를 초과할 경우 **강제로 CPU를 제한(Throttling)** 하며, 이것이 실무 장애의 주요 원인 중 하나입니다.

```yaml
# Kubernetes request/limit 예시값

resources:
  requests:
    cpu: "500m"
  limits:
    cpu: "1000m"
```

**Throttling이 Latency에 미치는 영향**

```
CPU Limit 초과
    ↓
CFS Throttling 발생
    ↓
스레드 실행 지연
    ↓
Event Loop 지연 / GC 지연 / Request Queue 증가
    ↓
응답 Latency 증가 → Timeout 발생
```

특히 **Spring WebFlux / Netty** 환경에서는 짧은 CPU 지연도 전체 응답 시간 증가로 이어질 수 있습니다.  
Event Loop 스레드가 블로킹되면 해당 Loop에 묶인 모든 요청이 함께 지연되기 때문입니다.

**확인 방법**

```bash
# Pod CPU 사용량 확인
kubectl top pod

# Throttling 발생 여부 확인
kubectl describe pod <pod-name>

# Prometheus 메트릭
container_cpu_cfs_throttled_seconds_total
container_cpu_cfs_periods_total

# Throttling 비율 계산 (25% 이상이면 조치 필요)
rate(container_cpu_cfs_throttled_periods_total[5m])
  / rate(container_cpu_cfs_periods_total[5m])
```

**실무 권장 설정**

| 설정 | 권장 방향 |
|------|----------|
| CPU Request | 실제 평균 사용량 기준으로 설정 |
| CPU Limit | Throttling 비율 모니터링 후 여유 있게 설정 |
| Throttling 비율 | 25% 이하 유지 권장. 초과 시 Limit 상향 검토 |

> CPU Limit을 너무 낮게 설정하면 Throttling으로 Latency가 급증합니다.  
> CPU Limit을 너무 높게 설정하면 노드 자원 보장이 어려워집니다.  
> **Request와 Limit의 차이를 최소화하는 것이 안정적인 운영의 기본**입니다.

### 7-11. Event Loop와 CPU Saturation

Spring WebFlux / Netty는 Event Loop 기반으로 동작합니다.

```
전통적인 Thread-per-Request 모델
요청 1 → Thread 1 (I/O 대기 중에도 점유)
요청 2 → Thread 2 (I/O 대기 중에도 점유)
→ 스레드 수 증가 → Context Switch 증가 → CPU Overhead 증가

Event Loop 모델
Event Loop Thread → 요청 1 처리 → I/O 위임 → 요청 2 처리 → 요청 1 콜백 처리 → ...
→ 적은 스레드 → 낮은 Context Switch → 높은 처리량
```

**적은 스레드로 높은 처리량**을 목표로 하는 구조이기 때문에,  
CPU Saturation이 발생하면 그 영향이 일반 모델보다 훨씬 광범위합니다.

| 상황 | 영향 |
|------|------|
| CPU 평균 사용률 높음 | Event Loop 처리 속도 저하 |
| 순간 CPU Spike | 짧아도 Event Loop 지연 → 전체 요청 Latency 증가 |
| CPU Throttling 발생 | Event Loop 정지 → Timeout 급증 |
| Context Switch 증가 | Cache Miss 증가 → Event Loop 처리 지연 |

> WebFlux 환경에서는 **CPU 평균 사용률보다 순간 Spike가 더 중요합니다.**  
> P99, P999 Latency를 꼭 함께 모니터링하세요.

#### Blocking 호출과 Event Loop 지연

Event Loop 내부에서

- JDBC
- `Thread.sleep()`
- 파일 I/O
- Blocking HTTP Client
- 동기 Redis / DB 호출

같은 Blocking 작업이 발생하면

```text
Blocking I/O
→ Event Loop Block
→ Request Queue 증가
→ Timeout 증가
→ Latency Spike
```

가 발생할 수 있습니다.

이 경우

* CPU Usage는 낮게 보일 수 있음
* 하지만 Runtime Latency는 급증 가능

> CPU Idle ≠ 서비스 정상

일 수 있습니다.

##### WebFlux 실무 원칙

| 원칙                     | 설명                         |
| ---------------------- | -------------------------- |
| Blocking 작업 분리         | 별도 Scheduler 사용            |
| Non-blocking Driver 사용 | R2DBC 등 사용                 |
| Event Loop 보호          | Event Loop Thread Block 방지 |
| Latency 모니터링           | P99 / P999 함께 관측           |

##### SRE 관점 핵심

WebFlux 환경에서는

* CPU 평균 사용률
* 단순 Thread 수

보다도,

```text
Event Loop Stall
```

이 실제 사용자 Latency에 더 직접적인 영향을 줄 수 있습니다.

특히 다음 상황에서 주의가 필요합니다

* Blocking JDBC 호출
* 외부 API 지연
* 파일 시스템 접근
* Redis / DB Connection Pool 대기
* 동기 라이브러리 사용

이러한 문제는 CPU Usage만으로는 잘 드러나지 않을 수 있으므로,
반드시

* P99 / P999 Latency
* Event Loop Delay
* Thread Dump
* Reactor BlockHound
* Timeout Metric

등을 함께 관측해야 합니다.

### 7-12. CPU Affinity / Pinning

특정 프로세스 또는 스레드를 특정 CPU 코어에 고정하는 것을 **CPU Affinity(CPU Pinning)** 라고 합니다.  
IRQ Affinity, NUMA Locality, Kafka Consumer 성능 최적화에서 함께 활용됩니다.

**CPU Pinning이 효과적인 상황**

- **IRQ Affinity** : 네트워크 인터럽트를 특정 코어에 고정하여 캐시 효율 향상
- **Kafka Consumer** : 특정 파티션 처리 스레드를 코어에 고정하여 Context Switch 감소
- **Netty Boss/Worker Thread** : 네트워크 I/O 스레드를 고정하여 캐시 지역성 향상
- **NUMA Locality** : 특정 NUMA 노드의 코어와 메모리를 함께 고정하여 Remote 접근 제거

**확인 및 설정 방법**

```bash
# 현재 프로세스의 CPU Affinity 확인
taskset -p <pid>

# 특정 코어에 프로세스 고정 (0번, 1번 코어)
taskset -c 0,1 <command>

# 실행 중인 프로세스의 Affinity 변경
taskset -p 0x3 <pid>   # 0x3 = 코어 0,1 사용

# IRQ Affinity 설정 (NIC를 코어 2,3에 고정)
echo "c" > /proc/irq/<irq-number>/smp_affinity
```

> CPU Pinning은 캐시 효율과 Context Switch를 줄이는 강력한 도구지만,  
> 잘못 설정하면 특정 코어 과부하로 오히려 성능이 저하될 수 있습니다.  
> **적용 전후 반드시 지표를 비교 측정하세요.**

### 7-13. HT / SMT 보안 고려사항

일부 보안 민감 환경(금융, 결제 시스템)에서는  
Side Channel Attack 완화를 위해 **SMT(HT)를 비활성화**하기도 합니다.

**관련 취약점**

| 취약점 | 설명 |
|--------|------|
| Spectre | 분기 예측을 악용한 메모리 정보 유출 |
| Meltdown | 커널 메모리 접근 우회 |
| L1TF (Foreshadow) | L1 캐시 데이터를 다른 VM에서 접근 가능 |
| MDS (RIDL, Fallout) | CPU 내부 버퍼를 통한 데이터 유출 |

> HT를 비활성화하면 위 취약점 공격면을 줄일 수 있으나,  
> **처리 성능이 최대 30~50% 감소**할 수 있습니다.  
> 보안 강화와 성능 사이의 Trade-off를 명확히 인식하고 결정해야 합니다.

`lscpu`의 `IBRS`, `IBPB`, `STIBP`, `md_clear` 등의 Flags는  
HT를 유지하면서 소프트웨어 수준으로 이를 완화하는 패치가 적용되어 있다는 의미입니다.

### 7-14. NUMA Locality와 GC 성능

NUMA(Non-Uniform Memory Access) 환경에서는  
메모리 접근 위치가 GC 성능과 Latency에 직접적인 영향을 줍니다.

```
NUMA Node 0              NUMA Node 1
[CPU Socket 0]           [CPU Socket 1]
[Local Memory]           [Local Memory]
      ↑ 빠름 (~100ns)          ↑ 빠름 (~100ns)
      ↓ 느림 (~300ns, Remote 접근) ──────┘
```

**Remote NUMA 접근이 증가할 때 나타나는 증상**

- Cache Miss 증가
- Memory Latency 2~3배 증가
- JVM GC Pause 증가 (특히 Full GC)
- 전반적인 처리량 감소

**확인 방법**

```bash
# NUMA 구성 확인
numactl --hardware

# NUMA 접근 통계 확인 (remote 접근 비율 모니터링)
numastat -p <pid>

# NUMA 정책 설정 (특정 노드에 메모리·CPU 고정)
numactl --membind=0 --cpunodebind=0 java -jar app.jar

# JVM NUMA 최적화 옵션
java -XX:+UseNUMA -XX:+UseParallelGC -jar app.jar
```

> 현재 vm-01, vm-02 환경은 `NUMA node(s): 1`로 단일 NUMA 구성입니다.  
> Remote 접근 문제는 없지만, 멀티소켓 서버로 확장 시 반드시 고려해야 합니다.

### 7-15. 하드웨어 가속 기능 (CPU Flags)

결제 시스템 특성상 암호화·해시 연산이 많은 환경에서 특히 중요한 플래그입니다.

| Flag | 기능 | 실무 적용 |
|------|------|----------|
| `AES` | TLS/HTTPS 암복호화 하드웨어 처리 | HTTPS 트래픽이 많은 결제 API 서버 CPU 오버헤드 절감 |
| `SHA-NI` | SHA 해시 하드웨어 처리 | 로그 해시, Git, 무결성 검증 가속 |
| `AVX/AVX2` | SIMD 256-bit 병렬 연산 | 모니터링 데이터 집계, 대용량 데이터 처리 가속 |
| `IBRS/IBPB` | 스펙터/멜트다운 보안 패치 | 보안 강화. 소량의 성능 오버헤드 존재 (측정 권장) |
| `VAES` | 벡터 AES 가속 | AVX-512 기반 암호화 처리 가속 (대용량 암호화) |

</br>

## 8. 빠른 참조 공식

### vCPU 수 계산

```
논리 CPU(vCPU) 수 = 소켓 × 코어/소켓 × 스레드/코어
```

**예시**

```
vm-01       : 1 × 1  × 1 = 1  vCPU
vm-02       : 1 × 1  × 2 = 2  vCPU
8코어 HT    : 1 × 8  × 2 = 16 vCPU
듀얼소켓    : 2 × 12 × 2 = 48 vCPU
```

### VM 생성 시 판단 기준

| 질문 | 공식 |
|------|------|
| 이 VM에 vCPU를 몇 개 할당할까? | 노출할 스레드 수 = vCPU 수 |
| 실제 물리 코어는 몇 개가 쓰이나? | 물리 코어 수 = vCPU ÷ Thread/core |
| CPU bound 작업의 실제 처리 한계는? | 물리 코어 수 기준으로 계산 (스레드 수 아님) |
| 워크로드가 I/O bound라면? | vCPU 수(스레드 수) 기준으로 동시성 설계 |

### 장애 상황별 확인 순서

```
Latency 급증 발생
    ↓
1. CPU 사용률 확인          top, mpstat
2. Load Average 확인        uptime
3. Runnable Queue 확인      vmstat → r 컬럼
4. Steal Time 확인          top → %st
5. I/O Wait 확인            top → %wa
6. Context Switch 확인      vmstat → cs / pidstat -w
7. K8s Throttling 확인      Prometheus 쿼리
8. IRQ 편중 확인            /proc/interrupts
9. PSI 확인                 /proc/pressure/cpu
```

### 모니터링 체크리스트

```bash
# 1. CPU 전체 상태 한눈에 확인 (us/sy/wa/st/hi/si)
top -1

# 2. 코어별 상세 통계
mpstat -P ALL 1

# 3. Load Average
uptime

# 4. Runnable Queue + Context Switch
vmstat 1          # r(queue), cs(context switch)
pidstat -w 1      # cswch/s, nvcswch/s

# 5. I/O Wait 상세
iostat -x 1

# 6. NUMA 접근 통계
numastat

# 7. IRQ 분포 확인
cat /proc/interrupts
cat /proc/softirqs

# 8. PSI (CPU 압박 지표)
cat /proc/pressure/cpu

# 9. CPU Affinity 확인
taskset -p <pid>

# 10. Kubernetes Throttling 확인
kubectl top pod
rate(container_cpu_cfs_throttled_periods_total[5m])
  / rate(container_cpu_cfs_periods_total[5m])
```

</br>

## 관련 문서

- `Infrastructure > Compute > VM 생성 가이드`
- `Infrastructure > Monitoring > 골든 시그널 대시보드 설정`
- `Infrastructure > Network > NIC RSS / IRQ Affinity 설정`
- `SRE > Runbook > CPU Saturation 대응 절차`
- `SRE > Runbook > Kubernetes CPU Throttling 대응 절차`
- `SRE > Runbook > High Load Average 대응 절차`
- `Platform > WebFlux > Event Loop 성능 튜닝 가이드`
- `Platform > JVM > GC Tuning과 NUMA Locality`
- `Platform > Kafka > Consumer Thread 최적화`

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*