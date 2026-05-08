# lscpu로 읽는 CPU 구조 — SRE 실무 가이드

**분류** : Infrastructure > Compute > CPU Analysis  
**작성자** : SRE Team  
**최종 수정** : 2026-05-06  
**대상 환경** : Intel i9-13900 / Microsoft Hyper-V / vm-01, vm-02

## 이 문서를 읽어야 하는 사람

- VM을 생성할 때 vCPU를 얼마나 할당해야 할지 기준이 없는 분
- lscpu 결과를 보고 어떤 항목이 중요한지 모르겠는 분
- SRE 관점에서 CPU 자원을 어떻게 설계하고 모니터링해야 하는지 궁금한 분
- [Kubernetes 환경에서 CPU Throttling 장애](#k8s-cpu-throttling-incident)를 겪어본 분
- WebFlux / Netty 기반 서비스에서 Latency 원인을 찾고 있는 분
- "CPU 사용률은 낮은데 왜 느리지?" 라는 상황을 경험한 분

</br>

<details>
  <summary>Kubernetes CPU Throttling 장애란?</summary>

<br/>

## Kubernetes CPU Throttling 장애의 본질

Kubernetes에서 CPU **limit**를 설정하면  
Linux Kernel의 CFS(Completely Fair Scheduler)가
Container의 CPU 사용량을 일정한 "Quota(예산)" 기준으로 관리합니다.

## Linux CFS의 동작 방식

Linux CFS는 일반적으로:

```bash
cat /sys/fs/cgroup/cpu/cpu.cfs_period_us

100ms (cpu.cfs_period_us = 100000)
```

단위로 CPU 사용량을 계산합니다.

예를 들어:

```yaml
resources:
  requests:
    cpu: "500m"

  limits:
    cpu: "500m"
```

이면,

Container는:

```text
100ms Period 안에서
50ms 분량의 CPU 연산 예산(Quota)
```

을 부여받습니다.

#### CPU 단위 (m) 와 Quota 의미

Kubernetes에서:

```text
1000m = 1 CPU
500m = 0.5 CPU
````

를 의미합니다.

여기서 1 CPU는 일반적으로:

```text
Host Linux가 인식하는
논리 CPU(Logical CPU) 1개
= vCPU 1개
= Hyper-thread(CPU 하드웨어의 논리 실행 단위) 1개
```

에 해당하는 연산 능력을 의미합니다.

왜 Logical CPU 기준으로 설명하는가:

```text
Linux Scheduler(CFS)는 
Logical CPU 단위로 Software Thread를 스케줄링하고
CPU Time을 계산하기 때문에,

일반적으로 Logical CPU(vCPU) 기준으로 이해하는 것이 가장 정확합니다.
```

중요한 점은:

```text
50ms 분량의 CPU 연산 예산(Quota)은

CPU 점유량(CPU Time Resource)
에 가까운 개념입니다.
```

멀티 코어 / 멀티 스레드 / HT(SMT) 환경에서  
이 Quota가 실제 흐르는 시간보다 훨씬 빠르게 소모될 수 있습니다.

예:

```text
실제 시간 25ms 만에
50ms Quota 소진 가능
```

합니다.

즉:

```text
CPU 연산 처리 속도가 빠르거나,
병렬 실행량이 높을수록

Quota를 더 빠르게 소진하여
Throttling이 발생할 수 있습니다.
```

## Kubernetes CPU Limit와의 관계

예:

```yaml
resources:
  limits:
    cpu: "1000m"
```

이면,

일반적으로:

```text
Logical CPU 1개
분량의 CPU 실행 예산
```

을 의미합니다.

하지만 Container 내부에서는:

* Netty Event Loop Thread
* Kafka Consumer Thread
* GC Thread
* Application Worker Thread (내부 작업 처리용)

등 여러 Software Thread가:

```text
동일한 CPU Quota Pool

CPU Quota Pool: Container 내부 Software Thread들이 함께 공유하는 CPU 실행 예산(CPU Time Resource)
```

을 공유합니다.

즉:

```text
GC가 CPU Quota를 많이 사용하면
Event Loop가 Throttle
```

될 수도 있습니다.

## 그러면 무슨 일이 발생하나?

Linux CFS는:

```text
"이번 주기의 CPU 예산을 모두 사용했으므로
다음 주기까지 실행 금지"
```

상태로 Container를 잠시 멈춥니다.

이것이:

```text
CPU Throttling
```

입니다.

중요한 점은:

```text
CPU가 실제로 유휴(Idle) 상태여도
Container는 CPU Quota(연산 예산)이 없어서 멈출 수 있음
```

입니다.

즉:

```text
CPU 부족
≠
반드시 CPU Throttling
```

입니다.

## 장애의 연쇄 반응 (Chain Reaction)

짧은 CPU 중단도 Runtime 전체에 영향을 줄 수 있습니다.

```text
CPU Throttling
→ Event Loop 지연
→ Request Queue 증가
→ Timeout 증가
→ Retry 증가
→ Kafka Lag 증가
→ Latency Spike
```

특히:

* Event Loop 기반 Runtime (적은 Thread 기반 구조)
* Latency-sensitive 시스템

에서는 영향이 훨씬 큽니다.

## 특히 위험한 Runtime 구조

다음과 같은 시스템은 짧은 [CPU Stall](#cpu-stall)에도 민감합니다.

<details>
  <summary>CPU Stall이란?</summary>

</br>

```
CPU가 명령어를 계속 실행하지 못하고,

데이터 준비 지연,
메모리 접근(Cache Miss),
분기 처리(Branch Prediction 실패) 등을

기다리면서

실제 연산이 일시적으로 멈추거나 지연되는 상태
```

대표적인 원인:

| 원인                       | 설명                                                        |
| ------------------------ | --------------------------------------------------------- |
| **Data Dependency**      | 이전 명령어의 연산 결과가 아직 준비되지 않아 다음 연산이 대기하는 상태                  |
| **Cache Miss**           | 필요한 데이터가 CPU Cache(L1/L2/L3)에 없어 RAM에서 데이터를 가져오느라 지연되는 상태 |
| **Branch Misprediction** | CPU의 분기 예측이 실패하여 잘못 실행한 명령어를 폐기하고 다시 실행하는 상태              |
| **Memory Latency**       | 메모리 접근 자체가 느려 CPU가 데이터를 기다리는 상태                           |
| [**I/O Wait**](#io-task)             | 디스크·네트워크·파일 시스템 응답을 기다리며 CPU 작업 진행이 지연되는 상태               |
| [**Lock Contention**](#lock-contention)      | 여러 Software Thread가 동일 Lock(Mutex/Spinlock)을 경쟁하면서 대기하는 상태         |
| **CPU Throttling**       | Linux CFS Quota 제한으로 Container 실행이 일시적으로 제한되는 상태          |
| **Context Switch**       | Scheduler가 실행 Thread를 교체하면서 발생하는 CPU 전환 비용                |
| **NUMA Remote Access**   | 다른 NUMA Node의 메모리에 접근하면서 메모리 지연이 증가하는 상태                  |

<details>
  <summary>Lock Contention(락 경합) 이란?</summary>

</br>

```
여러 Software Thread가
동일한 공유 자원(shared resource)을 보호하는
Lock(Mutex/Spinlock)을 경쟁하면서
대기하는 상태

Mutex: Lock 대기 중 Thread를 Sleep(Blocked) 상태로 전환하여 CPU 사용을 줄이는 방식
Spinlock: Lock 획득 전까지 반복 확인(Busy Waiting)하며 CPU를 계속 사용하는 방식
````

를 의미합니다.

## 왜 Lock이 필요한가?

Application / Runtime / OS 계층에서는 데이터 무결성(Data Integrity)을 위해:

```text
동일한 공유 자원(shared resource)
```

을 동시에 여러 Software Thread가 수정하지 못하도록
Lock을 사용합니다.

예를 들어 다음과 같은 자원들이 존재할 수 있습니다.

| 계층 (Layer) | 공유 자원 예시 | 설명 |
|---|---|---|
| Business Domain | [Session / Payment 상태](#session-payment) | 동일 사용자·결제 상태 데이터를 여러 요청이 동시에 변경할 수 있는 구조 |
| Application | Heap 영역 내 객체 | Singleton Bean, 공유 객체 등 여러 Software Thread가 상태를 함께 사용하는 구조 |
| Application | Task / Work Queue | 실행 대기 중인 Task를 Queue에 저장하고, 여러 Worker Thread가 이를 공유하며 가져가 처리하는 구조 |
| Application | Local Cache (Map) | ConcurrentHashMap 등을 이용해 애플리케이션 수준에서 데이터를 공유·캐싱하는 구조 |
| Runtime / Reactive | Event Loop Queue | Netty/WebFlux 환경에서 Event Loop가 비동기 이벤트·Callback을 처리하는 Queue 구조 |
| Runtime / I/O | Network Buffer | 네트워크 패킷 데이터를 읽고 쓰기 위해 Runtime이 사용하는 메모리 Buffer |
| Middleware | Kafka Partition 상태 | Consumer Offset, Commit 상태, Fetch Buffer 등을 여러 Thread가 함께 관리하는 구조 |
| Infrastructure | DB Connection Pool | 여러 요청 Thread가 제한된 DB Connection 자원을 공유하는 구조 |
| OS / Kernel | File Handle / Buffer | 운영체제가 관리하는 파일 접근 권한 및 파일 I/O Buffer 구조 |
| Logging | Log Buffer / Async Appender | 여러 Thread의 로그 데이터를 비동기적으로 수집·기록하는 Buffer 구조 |

<details>
  <summary>Business Domain 계층의 Session / Payment 상태</summary>

<br/>

## 왜 중요한가?

Lock(Mutex/Spinlock)은 단순한 [CPU 동기화 기술](#cpu-synchronization)`이 아니라 **비즈니스 데이터 무결성(Data Integrity)을 보호하기 위한 수단**입니다.

특히 아래와 같은 Business Domain 상태는 동시에 여러 요청(Request)이 접근할 수 있기 때문에 동시성 제어가 매우 중요합니다:

- Session
- Payment
- Order
- Balance
- Inventory

</br>

## 대표적인 예시

동일 결제(`paymentId`)에 대해 다음이 동시에 들어올 수 있습니다:

```text
Request A → 승인 처리
Request B → 승인 취소
Request C → 중복 승인 요청
```

적절한 동시성 제어가 없으면:

- 중복 결제
- 이중 승인
- 상태 불일치
- 데이터 손상

등이 발생할 수 있습니다.

## 왜 Lock이 필요한가?

Business Domain 계층에서는 **"동일 상태를 동시에 변경하지 못하도록"** 보호해야 합니다.

예:

```text
현재 결제 상태: PENDING

Thread A → APPROVED 변경
Thread B → FAILED 변경

→ 동시 발생 시 최종 상태 불일치 가능
```

따라서 다음을 사용하여 동일 Business 상태 변경 순서를 제어합니다:

- DB Row Lock
- Distributed Lock
- Optimistic Lock
- `synchronized`
- CAS (Compare-And-Set)

## SRE 관점에서 왜 중요한가?

Business Domain 계층의 Lock Contention은 단순 CPU 문제가 아니라 **서비스 신뢰성 문제**로 이어질 수 있습니다.

| 계층 | 영향 |
|---|---|
| Queue / Buffer 경합 | Throughput 저하 |
| DB Connection Pool 경합 | 응답 지연 |
| Payment 상태 경합 | 결제 실패 / 중복 승인 위험 |

> **어떤 공유 자원에서 Lock Contention이 발생했는가**에 따라 장애 심각도가 달라집니다.

## Business Domain 락의 특징

이 계층의 Lock은 외부 API / DB Transaction / 결제 승인 / 정산 처리 등과 연결되는 경우가 많습니다.

따라서 **Lock 유지 시간이 상대적으로 길어질 수 있습니다.**

예:

```text
결제 승인 요청
→ PG 응답 대기
→ DB Commit 대기
→ 이 동안 동일 상태를 보호하기 위해 Lock 유지
```

이 경우:

```text
Lock Contention 증가
→ Request Queue 증가
→ Timeout 증가
→ 사용자 Latency 증가
```

## 실무적으로 중요한 이유

결제/세션 계층의 동시성 문제는 성능 저하보다 **데이터 정합성(Data Consistency) 문제**가 더 치명적입니다.

CPU를 조금 더 쓰는 것보다 다음을 막는 것이 훨씬 중요합니다:

- 중복 결제
- 이중 승인
- 상태 불일치

특히 FinTech / Payment 환경에서는 다음이 최우선 원칙입니다:

```text
No Duplicate Payment
No Double Approval
No Inconsistent State
```

## 핵심 요약

> Business Domain 계층의 Lock은 단순 성능 제어가 아니라,  
> Session / Payment 상태의 **무결성과 신뢰성을 보호하기 위한 동시성 제어 메커니즘**입니다.

---

</details>

<details>
  <summary>Task/Work Queue와 Worker Thread 구조</summary>

<br/>

## Task / Work Queue란?

Task/Work Queue는 **실행 대기 중인 작업(Task)을 저장하고, Worker Thread가 이를 가져가 처리하는 Queue 구조**입니다.

| 개념 | 의미 |
|---|---|
| **Task** | 실행되어야 하는 작업 단위 |
| **Work Queue** | Task가 대기하는 Queue |
| **Worker Thread** | Queue에서 Task를 가져와 실행하는 Software Thread |

Java 기준으로 Task는 보통 `Runnable` / `Callable` 같은 실행 가능한 객체입니다.

## 기본 Thread Pool 구조

일반적인 Thread Pool 구조에서는 여러 Worker Thread가 하나의 Queue를 함께 바라봅니다.

```text
Producer
  ↓
Task / Work Queue
  ↓
Worker Thread 1
Worker Thread 2
Worker Thread 3
```

여러 Worker Thread가 공유 Queue에서 작업을 하나씩 가져가 실행합니다.

Queue는 공유 자원이므로, 여러 Worker Thread가 동시에 작업을 꺼내려 하면 내부적으로 **Lock / CAS / Atomic 연산** 등이 사용될 수 있습니다.

## Lock Contention이 발생할 수 있는 이유

Worker Thread 수가 많아지면 작업을 처리하는 시간보다 **Queue에서 Task를 가져오기 위한 경쟁**이 증가할 수 있습니다.

```text
Worker Thread 증가
→ Queue 접근 경쟁 증가
→ Lock Contention 증가
→ Context Switch / CPU Cache Miss 증가
→ Throughput 저하
```

> Worker Thread를 무작정 늘린다고 항상 성능이 좋아지는 것은 아닙니다.

## Netty / WebFlux는 왜 다른가?

Netty / WebFlux 같은 Event Loop 기반 Runtime은 일반적인 단일 공유 Queue 구조와 다르게 동작합니다.

Netty는 보통 **Event Loop Thread마다 자신의 전용 Task Queue**를 가집니다.

```text
Event Loop Thread 1 ─── Task Queue 1
Event Loop Thread 2 ─── Task Queue 2
Event Loop Thread 3 ─── Task Queue 3
```

이 구조는 여러 Thread가 하나의 Queue를 두고 경쟁하는 상황을 줄여 **Lock Contention을 낮추는 데 유리**합니다.

## Work-Stealing 구조

일부 Runtime은 Work-Stealing 구조를 사용합니다.

**대표 예:** Java `ForkJoinPool`

기본적으로 각 Worker Thread가 자신의 Queue를 가지고, 어떤 Thread가 일이 없으면 다른 Thread의 Queue에서 작업을 가져와 처리할 수 있습니다.

```text
Worker 1 ─── Queue 1  (작업 많음)
Worker 2 ─── Queue 2  (작업 없음)
                ↓
Worker 2가 Queue 1의 작업 일부를 가져와 처리
```

이 방식의 목표:
- 평소에는 Queue 경쟁 감소
- 부하 불균형 시 작업 분산

## Queue 구조 비교 (SRE 관점)

| 구조 | 특징 | 위험 |
|---|---|---|
| **Single Shared Queue** | 구조 단순 | Worker 간 Queue 경합 가능 |
| **Per-thread Queue** | Lock Contention 감소 | 특정 Thread Queue 쏠림 가능 |
| **Work-Stealing Queue** | 부하 분산 가능 | 구현 복잡도 증가 |

특히 Event Loop 기반 Runtime에서는 특정 Event Loop Queue에 무거운 작업이 몰리면:

```text
전체 CPU 사용률은 낮아 보여도
특정 요청의 Tail Latency가 증가
```

할 수 있습니다.

## 핵심 요약

- **Task**는 실행 단위 객체이고, **Worker Thread**는 이를 실행하는 Software Thread입니다.
- **Queue**는 Task를 대기시키는 공유 구조이며, Runtime에 따라 Single Queue / Per-thread Queue / Work-Stealing Queue로 구현될 수 있습니다.

---

</details>

즉:

```text
공유 자원을 동시에 변경하면
Race Condition(데이터 충돌)
```

이 발생할 수 있기 때문에:

```text
Lock으로 접근 순서를 제어
```

하는 것입니다.

---

## Lock Contention은 언제 발생하나?

정상 상태에서는:

```text
Thread가 아주 짧게 Lock 사용
→ 작업 완료
→ 즉시 반환
```

됩니다.

하지만:

* Lock을 오래 점유하거나
* 너무 많은 Thread가 동시에 접근하거나
* 느린 I/O 작업 중에도 Lock을 유지하거나
* 과도한 synchronized / Mutex 사용

등이 발생하면:

```text
다른 Thread들이 Lock 획득 대기
```

상태가 됩니다.

이것이:

```text
Lock Contention
```

입니다.

---

## Lock Contention이 발생하면?

### 1. Thread Waiting 증가

```text
Runnable / Blocked Thread 증가
```

합니다.

즉:

```text
Thread가 실제 작업 대신
Lock 해제를 기다림
```

상태가 됩니다.

---

### 2. Context Switch 증가

Linux Scheduler(CFS)는:

```text
실행 가능한 다른 Thread 탐색
```

을 반복하게 됩니다.

이 과정에서:

```text
Context Switch 증가
Scheduler Overhead 증가
CPU Cache Miss 증가
```

등이 발생할 수 있습니다.

---

### 3. Throughput 감소

CPU 사용률은 높게 보일 수 있지만:

```text
실제 비즈니스 처리량(Throughput)
```

은 감소할 수 있습니다.

즉:

```text
CPU가 실제 연산보다
대기/전환 비용에 더 많은 시간 사용
```

할 수 있습니다.

---

### 4. Latency 증가

요청 처리 지연으로 인해:

```text
Request Queue 증가
Timeout 증가
Retry 증가
Latency Spike
```

등이 발생할 수 있습니다.

---

## 실무 예시

| 시스템      | 예시                                  |
| -------- | ----------------------------------- |
| Database | 동일 Row 업데이트 경쟁(Row Lock Contention) |
| JVM      | synchronized / ReentrantLock 과도 사용  |
| Kafka    | Partition 상태/Buffer 접근 경쟁           |
| Logging  | 다수 Thread의 동시 로그 기록                 |
| Cache    | 공유 Cache(Map) 업데이트 경쟁               |

---

## WebFlux / Event Loop 환경에서 왜 위험한가?

Event Loop 기반 Runtime은:

```text
적은 수의 Software Thread
```

로 동작합니다.

따라서:

```text
Lock 대기 시간 증가
→ Event Loop Stall
→ 전체 Runtime Latency 증가
```

로 이어질 수 있습니다.

특히:

* synchronized Block
* Blocking I/O inside Lock
* Shared Mutable State
* Global Lock 구조

등은 주의가 필요합니다.

---

## SRE 관점 핵심

Lock Contention은:

```text
CPU 성능 부족 문제가 아니라,

공유 자원 접근 경쟁으로 인해
병렬 처리 효율이 감소하는 문제
```

입니다.

즉:

```text
Scale-out을 해도
핵심 Lock 지점이 병목이면
전체 처리량은 제한될 수 있습니다.
```

```
```











</details>


즉:

```
연산 진행이 중간에 계속 끊기는 상태
```

에 가깝습니다.

</details>

| Runtime           | 영향                 |
| ----------------- | ------------------ |
| Spring WebFlux    | Event Loop 지연      |
| Netty             | Connection 처리 지연   |
| Kafka Consumer    | Consumer Lag 증가    |
| JVM               | GC 지연 / STW 증가     |
| Redis Client      | Connection Timeout |
| Reactive Pipeline | Backpressure 증가    |

이들은:

```text
적은 수의 Thread
+
빠른 Event Loop 처리
```

를 기반으로 동작하기 때문입니다.

---

## CPU Usage는 낮은데 왜 느린가?

CPU Throttling의 가장 위험한 특징은:

```text
CPU Usage는 낮게 보일 수 있음
```

에도,

```text
Runtime Latency는 급격히 증가
```

할 수 있다는 점입니다.

즉:

```text
CPU Idle ≠ 서비스 정상
```

일 수 있습니다.

실제로는:

* Runnable Queue 증가
* Event Loop Stall
* Context Switch 증가
* Scheduler Delay
* Request Queue 증가

가 동시에 발생할 수 있습니다.

---

## Kubernetes는 무엇을 하는가?

중요한 점은:

```text
Kubernetes 자체가 CPU를 제한하는 것이 아님
```

입니다.

실제 Enforcement(강제 제한)는:

```text
Host Linux Kernel
+
cgroup
+
CFS Scheduler
```

가 수행합니다.

즉 흐름은:

```text
Kubernetes
→ kubelet
→ containerd / CRI
→ cgroup 설정 생성
→ Linux Kernel CFS Enforcement
```

입니다.

---

## 실제 Linux 제어 파일

### cgroup v1

```bash
cpu.cfs_period_us
cpu.cfs_quota_us
```

### cgroup v2

```bash
cpu.max
```

실제 경로 예시:

```bash
/sys/fs/cgroup/
/sys/fs/cgroup/kubepods.slice/
```

---

## CFS Period / Quota 조정 가능 여부

조정 가능합니다.

예:

```bash
cpu.cfs_period_us
```

기본값:

```text
100000 = 100ms
```

예:

```bash
echo 200000 > cpu.cfs_period_us
```

이면:

```text
Period = 200ms
```

가 됩니다.

즉:

```text
더 긴 CPU Burst 허용
```

효과가 발생할 수 있습니다.

---

## 하지만 왜 실무에서는 잘 안 바꾸나?

실무에서는 보통:

```text
Period 변경
```

보다,

```text
CPU limit 자체 완화
```

를 더 선호합니다.

왜냐면:

* kubelet 전체 영향
* Fairness 변화
* Noisy Neighbor 위험
* 특정 Container CPU 독점 가능성

이 생길 수 있기 때문입니다.

---

## SRE 관점 핵심

특히:

* Spring WebFlux
* Netty
* Kafka
* Reactive Runtime

환경에서는:

```text
짧은 CPU Stall
=
전체 Latency Spike
```

로 이어질 수 있습니다.

따라서 중요한 것은:

* 너무 타이트한 CPU limit 지양
* 충분한 Request 보장
* Burst 여유 확보
* P99 / P999 Latency 관측
* Event Loop Stall 모니터링
* CPU Throttling Metric 관측

입니다.

---

## 대표적인 모니터링 지표

### CPU Throttling 시간

```promql
container_cpu_cfs_throttled_seconds_total
```

### CPU Throttling 발생 횟수

```promql
container_cpu_cfs_throttled_periods_total
```

### CPU 사용률

```promql
container_cpu_usage_seconds_total
```

---

## 실무에서 자주 발생하는 오해

| 오해                | 실제                  |
| ----------------- | ------------------- |
| CPU Usage 낮음 = 정상 | Latency Spike 가능    |
| CPU Idle = 여유 있음  | Throttling 가능       |
| Container가 CPU 제한 | 실제론 Host Kernel CFS |
| limit은 안전장치       | 너무 타이트하면 장애 유발 가능   |

---

## 한 줄 요약

```text
Kubernetes CPU Throttling은
CPU 부족 자체보다,

"짧은 주기 안에서
CPU 실행 예산(Quota)을
너무 빠르게 소진"

해서 발생하는 Runtime Latency 문제에 가깝습니다.
```

</details>

## 먼저 이해해야 하는 CPU 계층 구조

<details>
  <summary>1. Physical Core — CPU 내부의 실제 연산 하드웨어</summary>

<br/>

## Physical Core 란

Physical Core는 CPU 칩 내부에 존재하는 **실제 연산 하드웨어**입니다.

단순한 논리 개념이 아니라, CPU 칩 안에 실제로 존재하는 물리 회로입니다.

쉽게 말하면:

```text
물리 Core 8개 CPU
= CPU 칩 안에 실제로 연산 가능한 두뇌 8개 존재
```

---

## Physical Core 내부 구성

Physical Core 내부에는 실제로 다음이 존재합니다:

| 구성 요소 | 역할 |
|---|---|
| **ALU** (Arithmetic Logic Unit) | 정수 연산 |
| **FPU** (Floating Point Unit) | 부동소수점 연산 |
| **Load/Store Unit** | 메모리 접근 |
| **Branch Predictor** | 분기 처리 |
| **L1 / L2 Cache** | 고속 메모리 캐시 |
| **Pipeline** | 명령어 실행 파이프라인 |
| **Register** | 연산 임시 저장 |

즉 Physical Core는:

```text
CPU 명령어를 실제 실행하는 하드웨어 회로
```

입니다.

---

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

---

## Physical Core vs Logical CPU — 핵심 구분

| 개념 | 의미 |
|---|---|
| **Physical Core** | 실제 연산 하드웨어 (ALU, Cache 등 포함) |
| **Logical CPU** | OS/Linux Kernel이 인식하는 CPU 실행 단위 |

> **중요:** Physical Core = Hardware Thread 1개 라고 표현하면 오해가 생깁니다.  
> Physical Core와 Hyper-thread(Logical CPU)는 서로 다른 계층의 개념입니다.

---

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

---

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

---

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

---

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

→ 물리 Core 8개 + Logical CPU 16개 구조

---

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

---

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

---

## HT ON = 성능 2배가 아닌 이유

HT/SMT는 Physical Core 내부 자원(ALU, Cache, Execution Unit 등)을 **공유**합니다.

따라서:

```text
HT ON = 성능 2배 (X)
HT ON = 특정 워크로드에서 처리량 향상 (O)
```

실제로는 워크로드 특성에 따라 성능 향상 폭이 다릅니다.

---

## 개념 정리

| 개념 | 계층 | 의미 |
|---|---|---|
| **Physical Core** | 하드웨어 | 실제 연산 하드웨어 |
| **Hyper-thread / SMT** | 하드웨어 | Logical CPU 논리 실행 단위 |
| **Logical CPU** | OS 인식 | Linux Kernel이 인식하는 CPU |
| **Multi-core** | 하드웨어 | Physical Core 증가 |
| **HT/SMT** | 하드웨어 기술 | Logical CPU 증가 |

</details>

<details>
  <summary>3. Software Thread 와 Linux Scheduler(CFS) — OS / JVM / Application 실행 흐름</summary>

<br/>

## Software Thread 란

Software Thread는 **프로그램 실행 흐름**입니다.

CPU 내부 구조가 아니라 OS / JVM / Application 계층의 개념입니다.

대표적인 Software Thread:

| 종류 | 예시 |
|---|---|
| **Java Thread** | `new Thread(...)` |
| **Netty Event Loop Thread** | Netty I/O 처리 흐름 |
| **Kafka Consumer Thread** | Kafka 메시지 소비 흐름 |
| **GC Thread** | JVM Garbage Collector 흐름 |
| **Worker Thread** | Thread Pool 내 처리 흐름 |
| **POSIX Thread (pthread)** | Linux 기반 스레드 |
| **Go Routine 스케줄 대상** | Go Runtime 실행 흐름 |

이들은 전부 **Software 계층 실행 흐름**입니다.

---

## Hyper-thread vs Software Thread 핵심 구분

`Thread`라는 단어가 두 계층에 모두 사용되기 때문에 혼동이 생길 수 있습니다.

| 종류 | 실제 의미 | 계층 |
|---|---|---|
| **Hyper-thread** | CPU 하드웨어 논리 실행 단위 | 하드웨어 |
| **Software Thread** | 프로그램 실행 흐름 | OS / JVM / Application |

---

## Linux 에서의 Thread

Linux Kernel은 실제로 Thread와 Process를 꽤 비슷하게 취급합니다.

내부적으로는 `task_struct` 기반입니다.

즉 Scheduler(CFS)는:

```text
Software 실행 단위(task)
```

를 관리합니다.

---

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

---

## 핵심 요약 (TL;DR)

> SRE 설계 기준은 **CPU 사용률 60% 이하 유지**, 즉 여유를 설계 단계에서 계산해야 합니다.  
> **CPU 사용률이 낮아도 Saturation은 발생할 수 있습니다.** 사용률 하나만 보는 모니터링은 불완전합니다.

---

## 목차

1. [CPU 계층 구조](#1-cpu-계층-구조)
2. [용어 사전](#2-용어-사전)
3. [lscpu 항목별 해석](#3-lscpu-항목별-해석)
4. [vCPU 할당과 스레드의 관계](#4-vcpu-할당과-스레드의-관계)
5. [vm-01 vs vm-02 비교](#5-vm-01-vs-vm-02-비교)
6. [CPU Bound vs I/O Bound](#6-cpu-bound-vs-io-bound)
7. [SRE 관점 분석](#7-sre-관점-분석)
8. [빠른 참조 공식](#8-빠른-참조-공식)

---

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

---

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

---

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

---

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

---

## 5. vm-01 vs vm-02 비교

| 항목 | vm-01 | vm-02 |
|------|-------|-------|
| 논리 CPU (vCPU) | 1개 | **2개** |
| 물리 코어 | 1개 | 1개 (동일) |
| HT 노출 여부 | 미노출 | **노출 (Thread×2)** |
| 현재 클럭 | ~1,971 MHz | ~2,758 MHz |
| L3 캐시 | 36MB | 36MB (동일) |
| 가상화 플랫폼 | Hyper-V | Hyper-V (동일) |

---

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

---

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

---

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

---

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

---

# Appendix — Deep Dive

이 섹션은 본문 흐름을 방해하지 않는 심화 설명 모음입니다.

</br>

<a id="thread-cpu-ram-flow"></a>

### Java Thread → CPU Core → RAM 까지의 전체 처리 흐름과 동작 구조

<details>
  <summary>설명</summary>

<br/>

## 개요

```java
counter.value++;
```

이 한 줄의 코드는 내부적으로 다음 전체 계층을 거칩니다:

```text
Java 코드
  ↓
JVM Runtime
  ↓
OS Scheduler
  ↓
Physical CPU Core
  ↓
CPU Cache (L1 / L2 / L3)
  ↓
Memory Controller
  ↓
Physical RAM
```

## 1. 전체 계층 구조

| 계층 | 역할 |
|---|---|
| **Application Code** | 논리적 의도 생성 |
| **Software Thread** | 코드 실행 주체 |
| **JVM Heap** | 논리적 공유 메모리 |
| **OS Scheduler** | Thread ↔ CPU Core 매핑 |
| **CPU Core** | 실제 계산 수행 |
| **CPU Cache** | 고속 임시 저장 (L1/L2/L3) |
| **Memory Barrier** | 실행 순서 강제 |
| **Physical RAM** | 최종 데이터 저장 |

## 2. Software Thread와 JVM Heap

코드를 실행하는 주체는 **Software Thread**입니다.

```java
class Counter {
    int value = 0;
}
Counter counter = new Counter();
```

이 객체는 **JVM Heap**에 저장되며, 여러 Thread가 동시에 접근 가능합니다.

```text
Thread A ─┐
Thread B ─┼──→ Shared JVM Heap (counter 객체)
Thread C ─┘
```

## 3. Java Thread와 CPU Core의 관계

Java Thread는 **논리적인 실행 단위**입니다.  
실제 CPU Core를 할당하는 것은 **OS Scheduler**입니다.

```text
Java Thread
    ↓
Native Thread (OS Thread)
    ↓
CPU Core 배치 (OS Scheduler 결정)
```

OS Scheduler 배치 예시:

```text
Thread A → CPU Core 1
Thread B → CPU Core 3
Thread C → 대기
```

## 4. `count++`의 실제 CPU 처리 과정

```java
count++;
```

이 단순해 보이는 코드는 실제로 다음 단계로 분리됩니다:

```text
1. RAM에서 값 읽기
2. CPU Register에 적재
3. +1 계산 (ALU)
4. 결과를 Register에 저장
5. Cache에 반영
6. RAM에 반영
```

## 5. CPU Cache 구조와 문제

현대 CPU는 RAM 접근 속도가 느리기 때문에 **L1/L2/L3 Cache**를 사용합니다.

```text
CPU Core
   ↓
L1 Cache  (~1ns)
   ↓
L2 Cache  (~5ns)
   ↓
L3 Cache  (~20ns)
   ↓
RAM       (~100ns)
```

**문제:** CPU Core마다 Cache가 독립적으로 존재합니다.

```text
CPU Core 1 Cache → value = 5
CPU Core 2 Cache → value = 3   ← 같은 객체인데 다른 값
```

이것이 **Visibility Problem(메모리 가시성 문제)** 입니다.

## 6. 동기화가 필요한 이유

| 문제 | 설명 |
|---|---|
| **Race Condition** | 여러 Thread가 동시에 같은 값을 읽고 수정해 최종값이 잘못됨 |
| **Visibility Problem** | Core별 Cache 불일치로 Thread마다 다른 값을 보는 상태 |
| **Atomicity 부재** | `count++` 같은 연산이 중간에 끊길 수 있음 |

## 7. `synchronized` 내부 실행 흐름

```java
synchronized(lock) {
    count++;
}
```

실제 내부 처리 순서:

```text
1. Lock 획득
2. 다른 Thread 접근 차단
3. Memory Barrier 실행 (실행 순서 강제)
4. CPU Cache 동기화
5. count 읽기 → 계산 → 저장
6. Cache Flush (RAM 반영)
7. Memory Barrier 실행
8. Lock 해제
```

## 8. 동기화 기술의 핵심 역할

| 역할 | 의미 |
|---|---|
| **실행 순서 제어** | 임계 영역 내 순차 실행 보장 |
| **메모리 가시성 보장** | 모든 Thread가 동일한 최신 값을 보도록 강제 |
| **원자성 보장** | 연산이 중간에 끊기지 않도록 보장 |
| **Cache 동기화** | Core별 Cache 불일치 해소 |
| **Happens-Before 보장** | 작업 간 시간적 선후 관계 강제 |

**주요 동기화 기술:** `synchronized` / `volatile` / `Lock` / `CAS` / `AtomicOperation` / `Memory Barrier`

## 9. 전체 처리 흐름 요약

```text
[Step 1] 개발자 코드 작성
         counter.value++

[Step 2] Software Thread 실행
         Thread가 JVM Heap의 counter 객체 접근

[Step 3] OS Scheduling
         OS가 Thread를 물리 CPU Core에 배치

[Step 4] CPU 연산
         CPU가 Cache / RAM에서 값 읽기

[Step 5] 값 계산 및 저장
         ALU 연산 후 Cache에 반영

[Step 6] 메모리 계층 동기화
         Cache Flush → RAM 반영
         Memory Barrier로 순서 강제
```

## 핵심 요약

`counter.value++` 한 줄은 실제로 다음 전체 파이프라인을 의미합니다:

```text
Software Thread가 코드를 실행
  → OS가 CPU Core를 할당
  → CPU가 Cache / RAM에서 값을 읽고
  → ALU가 계산하고
  → Cache → RAM 순서로 값을 반영
  → 동기화 기술이 순서와 가시성을 보장
```

---

</details>

<a id="shared-memory"></a>

### 공유 메모리(Shared Memory)와 동기화 정리

<details>
  <summary>설명</summary>
<br/>

공유 메모리(Shared Memory)는 **여러 개의 프로세스(Process)나 스레드(Software Thread)가 공유해서 읽고 쓸 수 있는 공용 메모리 공간**을 의미합니다.

</br>

## 1. 계층별 공유 메모리의 실체

### 1-1. 하드웨어 관점 (Main Memory / RAM)

하드웨어 관점에서 공유 메모리는 **여러 CPU 코어가 동시에 접근 가능한 물리적인 메인 메모리(RAM)** 입니다.

예시: DDR4 / DDR5 / Main Memory

```text
CPU Core 1
CPU Core 2
CPU Core 3
     ↓
  Shared RAM
```

### 1-2. 소프트웨어 관점 (JVM Heap)

소프트웨어 관점에서는 **JVM Heap 영역**이 대표적인 공유 메모리입니다.

모든 Java Thread가 동일한 Heap 객체를 함께 읽고 수정할 수 있기 때문입니다.

```java
class Counter {
    int value = 0;
}
```

위 객체가 Heap에 생성되면:

```text
Thread A → Counter.value 접근
Thread B → Counter.value 접근
Thread C → Counter.value 접근

→ 모든 스레드가 동일 객체를 공유
```

## 2. 왜 "공유 메모리"가 중요한가?

동기화(Synchronization)가 필요한 이유는 바로 이 공유 메모리 때문입니다.

### Stack 메모리 — 개별 공간 (동기화 불필요)

각 스레드는 자기만의 Stack 메모리를 가집니다.

```text
Thread A Stack  (자기 자신만 접근 가능)
Thread B Stack  (자기 자신만 접근 가능)
Thread C Stack  (자기 자신만 접근 가능)
```

`Local Variable` / `Method Parameter` / `Method Frame` 등은 안전합니다.

### Heap / Static 메모리 — 공유 공간 (동기화 필요)

반면 Heap이나 Static 영역은 **모든 Thread가 함께 접근 가능**합니다.

```text
Shared Object
Static Variable
Singleton Object
Cache
Connection Pool
```

등은 모두 공유 메모리이며, 여기서 문제가 발생합니다.

## 3. Race Condition (데이터 오염)

공유 메모리는 여러 스레드가 동시에 수정할 수 있기 때문에 Race Condition이 발생할 수 있습니다.

```java
count++;
```

이 코드는 실제로 3단계 작업입니다:

```text
1. count 읽기
2. +1 계산
3. 다시 저장
```

Thread A와 Thread B가 동시에 실행하면:

```text
A가 읽음 → 5
B가 읽음 → 5
A 저장  → 6
B 저장  → 6   ← 최종값이 7이 아니라 6
```

이것이 **Race Condition**입니다.

## 4. CPU 동기화와의 관계

공유 메모리에 여러 CPU Core가 접근하면 다음 두 가지 문제가 발생합니다.

### 4-1. [가시성 문제 (Visibility)](#thread-cpu-ram-flow)

```text
Thread A가 값을 변경했지만
Thread B CPU Cache에는 이전 값이 남아 있음

→ RAM 값 ≠ CPU Cache 값
```

### 4-2. 원자성 문제 (Atomicity)

```java
count++;  // 원자적(Atomic) 작업이 아님
```

```text
읽기 → 계산 → 저장
      ↑
  중간에 다른 스레드가 끼어들 수 있음
```

## 5. 동기화 기술이 필요한 이유

이 문제들을 해결하기 위해 CPU 동기화 기술과 Lock 메커니즘이 필요합니다.

**대표 기술:**

| 기술 | 분류 |
|---|---|
| `synchronized` | Java 내장 Lock |
| `ReentrantLock` | 명시적 Lock |
| `CAS` (Compare And Swap) | Lock-free 원자 연산 |
| `volatile` | 메모리 가시성 보장 |
| `AtomicInteger` | 원자적 정수 연산 |
| `Semaphore` | 접근 수 제한 |
| `Mutex` | 상호 배제 Lock |
| `Spin Lock` | Busy Waiting Lock |

## 6. Lock Contention (락 경합)

모든 스레드가 공유 메모리를 동시에 접근하려 하면 **"누가 먼저 사용할 것인가?"** 문제가 발생합니다.

Lock을 얻기 위해 경쟁하는 상황을 **Lock Contention**이라고 합니다.

```text
Thread A → Lock 대기
Thread B → Lock 사용 중
Thread C → Lock 대기
```

## 7. 전체 흐름 정리

```text
CPU Core
   ↓
Software Thread
   ↓
Shared Memory (Heap / RAM)
   ↓
Synchronization
   ↓
Lock / CAS / Atomic Operation
```

## 핵심 요약

공유 메모리란 **모든 스레드가 함께 사용하는 거대한 공용 데이터 저장소**입니다.

대표적으로 RAM / JVM Heap / Static 영역 등이 있으며,  
`Lock Contention` / `Race Condition` / `Visibility` / `Atomicity` 문제들이 모두 여기서 발생합니다.

> **공유 메모리 = 멀티스레드 전쟁터**

---

</details>

### I/O 작업이란?

<a id="io-task"></a>

<details>
  <summary>설명</summary>

</br>

**CPU 외부 자원(디스크·네트워크·DB·파일 시스템 등)과 데이터를 주고받거나 응답을 기다리는 작업**을 의미합니다.

## 핵심 정의

CPU가 직접 계산하지 않고, **외부 장치 / 외부 자원과의 데이터 입출력**을 수행하는 작업입니다.

"외부 연산"이 아니라 **외부 자원과의 데이터 입출력**이 핵심입니다.

## I/O 여부 판단 기준

| 작업 | I/O 여부 | 이유 |
|---|---|---|
| DB Query 응답 대기 | I/O | 외부 DB 자원과 통신 |
| HTTP API 호출 | I/O | 네트워크를 통한 외부 자원 접근 |
| Kafka Broker 통신 | I/O | 네트워크를 통한 외부 자원 접근 |
| 파일 읽기 | I/O | 디스크 자원과 데이터 입출력 |
| Redis 요청 | I/O | 네트워크를 통한 외부 자원 접근 |
| AES 암호화 | I/O 아님 | CPU 연산 |
| JSON 직렬화 | I/O 아님 | CPU 연산 |
| GC | I/O 아님 | JVM 내부 CPU 연산 |
| AI 모델 추론 계산 | I/O 아님 | CPU / GPU 연산 |
| 데이터 압축 | I/O 아님 | CPU 연산 |
| 이미지 처리 | I/O 아님 | CPU 연산 |

## 주의사항

외부 시스템과 관련 있어 보여도, **실제 CPU 계산 중심 작업은 I/O가 아닐 수 있습니다.**

예:
- 압축 / 암호화 / AI 추론 / 이미지 처리 → CPU Bound 연산

--- 

</details>

### CPU 동기화에서 "실행 순서" 제어의 의미

<a id="execution-order"></a>

<details>
  <summary>설명</summary>

<br/>

## CPU 동기화에서 실행 순서가 중요한 이유

CPU 동기화 기술은 단순히 **"한 번에 한 스레드만 접근하게 막는 기술"** 이 아닙니다.

진짜 핵심은:

```text
공유 메모리에 대한 실행 순서와 시간적 선후 관계를 강제하는 것
```

즉 동기화는 다음을 제어합니다:

- 누가 먼저 실행되는가
- 어떤 작업이 먼저 메모리에 반영되는가
- 어떤 결과를 다른 스레드가 언제 볼 수 있는가

## 1. 실행 순서(Execution Order)란 무엇인가?

멀티스레드 환경에서 Thread A / B / C가 동시에 공유 메모리(Heap / RAM)를 수정하면 데이터가 꼬일 수 있습니다.

```java
count++;
```

이 연산은 실제로 3단계 작업입니다:

```text
1. 값 읽기
2. 계산
3. 저장
```

여러 스레드가 동시에 실행되면:

```text
A 읽기 → 5
B 읽기 → 5
A 저장 → 6
B 저장 → 6   ← 최종 결과가 7이 아니라 6
```

**실행 순서가 섞인 것**이 문제의 본질입니다.

## 2. 동기화가 제어하는 두 가지 실행 순서

### 2-1. 상호 배제(Mutual Exclusion)에 의한 순서

여러 스레드가 동시에 실행되더라도 **임계 영역(Critical Section)** 만큼은 반드시 한 줄로 서서 순차 실행되도록 강제합니다.

```text
Thread A → Lock 획득
Thread B → 대기
Thread C → 대기
```

결과적으로 다음 순서가 만들어집니다:

```text
A 작업 완료
    ↓
B 작업 시작
    ↓
C 작업 시작
```

> 이 순서를 강제하는 목적은 **Race Condition 방지**입니다.

### 2-2. 메모리 가시성과 Happens-Before 순서

**CPU와 컴파일러의 최적화 문제:**

CPU와 컴파일러는 성능 향상을 위해 **Instruction Reordering(명령어 재정렬)** 을 수행합니다.  
개발자가 작성한 코드 순서를 임의로 바꿔 실행할 수 있습니다.

```java
data = new Object();
ready = true;
```

개발자의 기대 순서:
```text
1. data 생성
2. ready = true
```

CPU가 최적화하면:
```text
1. ready = true   ← 순서 역전
2. data 생성
```

그 결과 다른 Thread가 `ready=true`를 먼저 보고 아직 생성되지 않은 `data`에 접근하는 상황이 발생할 수 있습니다.

## 3. Happens-Before 관계

동기화 기술은 이런 문제를 막기 위해 **Happens-Before** 관계를 강제합니다.

```text
"A 작업은 반드시 B 작업보다 먼저 메모리에 반영되어야 한다"
```

**예시:**

```text
Thread A              Thread B
1. 데이터 쓰기
2. Lock 해제
                      1. Lock 획득
                      2. 데이터 읽기
```

동기화 메커니즘은 다음 순서를 절대적으로 보장합니다:

```text
[데이터 쓰기]
     ↓
[Lock 해제]
     ↓
[Lock 획득]
     ↓
[데이터 읽기]
```

## 4. 메모리 배리어(Memory Barrier)

Happens-Before 관계를 실제 CPU 수준에서 강제하는 것이 **Memory Barrier / Memory Fence**입니다.

```text
CPU에게 "이 순서는 절대로 바꾸지 마라"
라고 명령하는 하드웨어 레벨 동기화 장치
```

## 5. 락 획득 전후의 차이

### 락 획득 전 — 경합(Competition) 상태

Thread A / B / C가 동시에 Lock을 요청하면 OS Scheduler / CPU Timing / Interrupt 등에 따라 먼저 획득하는 스레드가 달라질 수 있습니다.

```text
락 획득 전까지는 경합(Contention) 상태
```

### 락 획득 후 — 엄격한 순서 강제

일단 Lock을 획득한 순간부터는 엄격한 실행 순서가 강제됩니다:

```text
A 작업 완료
    ↓
B 작업 시작   ← 절대적인 선후 관계 형성
```

이것이 **Synchronization의 본질**입니다.

## 6. 동기화의 핵심 목표 — 결정성(Determinism)

| 상태 | 설명 |
|---|---|
| **동기화 없음** | 실행 결과가 매번 달라짐 → Non-Deterministic |
| **동기화 있음** | 누가 먼저 실행될지는 몰라도 최종 결과는 항상 동일 → Deterministic |

## 7. SRE 관점에서의 실행 순서

로그(Log) / 트레이스(Trace) / 이벤트(Event)들이 바로 실행 순서의 기록입니다.

**문제 상황:**

```text
결제 요청(A)이 먼저 들어왔는데
취소 요청(B)이 먼저 처리됨
→ 데이터 정합성 오류 발생
```

**원인:**

- 동기화 실패
- 메시지 순서 역전
- 메모리 가시성 문제
- 비동기 처리 순서 오류

→ **논리적인 실행 순서가 깨진 것**

## 핵심 요약

CPU 동기화에서 **"실행 순서를 제어한다"** 는 의미는 단순히 "한 명씩 들어가게 한다"를 넘어서:

```text
1. 공유 메모리 접근을 순차화하고
2. 시간적 선후 관계를 보장하며
3. 메모리 반영 순서를 강제하고
4. 결과를 결정적 상태로 유지하는 것
```

을 의미합니다.

> **동기화의 본질은 혼란스러운 동시 실행을 논리적인 시간 순서로 변환하는 것입니다.**

---

</details>

### CPU 동기화 기술(Thread Synchronization)이란?

<a id="cpu-synchronization"></a>

<details>
  <summary>설명</summary>

<br/>

**여러 CPU Core 또는 Software Thread가 동일한 [공유 메모리(shared memory)](#shared-memory)에 접근할 때, 데이터 무결성(Data Integrity)을 유지하기 위해 [실행 순서와 접근을 제어](#execution-order)하는 메커니즘**입니다.

즉:

```text
동시에 같은 데이터를 수정하여
상태가 꼬이지 않도록 제어하는 기술
```

</br>

## 왜 필요한가?

현대 시스템은 다음 환경으로 동작합니다:

- Multi-core CPU
- Multi-thread Runtime
- Concurrent Processing

예를 들어 CPU Core A와 Core B가 동시에 **동일 메모리 주소(shared memory)** 를 수정하려 하면:

- Race Condition
- 데이터 손상
- 상태 불일치

가 발생할 수 있습니다.

따라서 **누가 먼저 접근하고, 누가 기다릴지**를 제어해야 합니다.

## CPU 동기화 기술의 대표 예시

| 기술 | 설명 |
|---|---|
| **Mutex** | Lock 획득 실패 시 Thread를 Sleep(Blocked) 상태로 전환 |
| **Spinlock** | Lock 획득 전까지 CPU를 계속 사용하며 반복 확인 (Busy Waiting) |
| **CAS** (Compare-And-Swap) | 특정 값 비교 후 변경을 CPU가 원자적으로 수행 |
| **Atomic Operation** | 중간에 끊기지 않는 단일 연산 보장 |
| **Memory Barrier** | CPU 명령어 재정렬(Reordering) 방지 및 메모리 가시성 보장 |

## Atomic Operation (원자적 연산)

Atomic Operation은 **중간에 끊기지 않는 하나의 연산 단위**입니다.

예: "현재 값이 10이면 11로 변경" 작업을 읽기 → 계산 → 쓰기로 나누지 않고, **CPU가 하나의 연산처럼 처리**합니다.

이를 통해 동시 수정 충돌(Race Condition)을 줄일 수 있습니다.

## Memory Barrier (메모리 배리어)

현대 CPU는 성능 최적화를 위해 **명령어 순서 재배치(Reordering)** 를 수행할 수 있습니다.

하지만 동시성 환경에서는 **다른 Core가 최신 메모리 상태를 보지 못하는 문제**가 발생할 수 있습니다.

Memory Barrier는 다음을 강제하는 메커니즘입니다:

```text
메모리 기록 순서 보장
+
다른 CPU Core에 메모리 변경 내용 전파
```

## 왜 "CPU 동기화"라고 부르나?

동기화 문제는 단순히 Java 문법 수준의 문제가 아니라, 실제로는 다음과 연결되기 때문입니다:

- CPU Cache
- Memory Ordering
- Atomic Instruction
- Core 간 메모리 가시성

즉 **Software Thread 동기화**는 결국 **CPU / Memory 하드웨어 동기화** 위에서 동작합니다.

## Business Domain과 연결되는 이유

동기화 기술은 단순 성능 최적화 도구가 아닙니다. 최종 목적은 **비즈니스 데이터 무결성 보호**입니다.

예:
- 중복 결제 방지
- 이중 승인 방지
- 잔액 불일치 방지
- Session 상태 보호

| 계층 | 역할 |
|---|---|
| **CPU 동기화 기술** | 실행 순서와 메모리 접근 제어 |
| **Business Domain** | 실제 서비스 데이터 무결성 보호 |

## 실무 관점 핵심

Lock / CAS / Atomic 연산 등은 CPU 자원을 효율적으로 쓰기 위한 기술인 동시에, 결국 **서비스 신뢰성(Reliability)과 데이터 정합성(Consistency)** 을 지키기 위한 핵심 메커니즘입니다.

특히 FinTech / Payment 시스템에서는 다음이 최우선 원칙입니다:

```text
No Duplicate Payment
No Double Approval
No Inconsistent State
```

## 핵심 요약

> CPU 동기화 기술(Thread Synchronization)은  
> 여러 CPU Core와 Software Thread가 공유 데이터를 동시에 처리하는 환경에서,  
> **데이터 무결성과 서비스 신뢰성을 유지하기 위해**  
> 실행 순서와 메모리 접근을 제어하는 기술입니다.

---

</details>













---

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*