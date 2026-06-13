# 스레드 (Thread)

> 정독: 0회

## 1. 이 기술이 무엇인가

스레드는:

> **CPU 스케줄러가 실제로 실행시키는 최소 실행 단위**

프로세스 내부에는 하나 이상의 스레드가 존재할 수 있으며, 각 스레드는 독립 실행 흐름을 보유합니다.

### 핵심 특징

| 공유 | 독립 |
|------|------|
| 코드 세그먼트 | Program Counter |
| 데이터 세그먼트 | Register Set |
| Heap | Stack |
| 열린 파일 | Execution Context |

> **메모리는 공유하지만 실행 흐름은 독립**

---

## 2. 시스템 어디에서 등장하는가

스레드는 거의 모든 현대 시스템의 기본 실행 모델입니다.

| 영역 | 사용 목적 |
|------|-----------|
| 운영체제 | scheduler execution |
| 웹 서버 | request concurrency |
| 데이터베이스 | parallel query |
| 게임 엔진 | rendering/input |
| 런타임 | GC/background task |
| 네트워크 서버 | socket handling |
| Kubernetes container | application execution |

현대 시스템 대부분은 **멀티스레드 기반 동시성 구조**로 동작합니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

스레드는 **CPU와 Memory에 가장 직접적 영향**을 줍니다.

| 자원 | 영향 |
|------|------|
| CPU | scheduling/context switch |
| Memory | stack allocation/shared heap |
| Cache | cache contention |
| Lock | synchronization overhead |

**간접적 영향:**

| 자원 | 연결 방식 |
|------|-----------|
| Network | async I/O |
| Disk | blocking syscall |

---

## 4. 왜 중요한가

**현대 서버의 병렬 처리 단위가 대부분 스레드**이기 때문입니다.

| 작업 | 스레드 활용 |
|------|-------------|
| HTTP 요청 처리 | worker thread |
| DB connection 처리 | execution thread |
| background job | scheduler thread |
| logging | async thread |
| runtime GC | collector thread |

멀티코어 CPU 환경에서 **스레드 수와 CPU 활용률은 직접 연결**됩니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

| 문제 | 설명 |
|------|------|
| Thread Explosion | 과도한 thread 생성 |
| Context Switch Storm | switching 과다 |
| Deadlock | lock 상호 대기 |
| Livelock | 계속 재시도만 수행 |
| Starvation | 특정 thread 실행 못함 |
| Stack Overflow | stack exhaustion |
| CPU Saturation | runnable overload |
| Lock Contention | synchronization bottleneck |

> **스레드 수 증가 ≠ 성능 증가**

과도한 스레드는 오히려 scheduler pressure, cache miss, memory overhead, lock contention을 증가시킵니다.

---

## 6. 핵심 메커니즘

### 스레드는 실행 흐름이다

- **프로세스**: 자원 소유 단위
- **스레드**: 실제 CPU 실행 단위

CPU scheduler는 대부분 프로세스가 아니라 **스레드를 스케줄링**합니다.

### Shared Memory

같은 프로세스의 스레드들은 다음을 공유합니다:
- heap
- global variable
- file descriptor
- mmap
- code segment

스레드 간 통신은 매우 빠르지만, race condition, memory corruption, synchronization bug가 발생할 수 있습니다.

### Independent Stack

각 스레드는 자신만의 stack을 보유합니다.

| 요소 | 설명 |
|------|------|
| stack frame | 함수 실행 문맥 |
| local variable | 지역 변수 |
| return address | 복귀 위치 |
| saved register | context |

함수 호출 흐름은 **스레드별 독립**입니다.

### Program Counter

각 스레드는 독립 PC를 보유합니다. 동일 코드라도 각기 다른 위치를 동시에 실행할 수 있습니다.

```
Thread A → line 100
Thread B → line 300  (동시 실행 가능)
```

### Context Switch

스레드 전환 시 저장되는 요소:

| 요소 | 설명 |
|------|------|
| PC | instruction position |
| SP | stack pointer |
| registers | CPU state |
| flags | condition |

스레드 전환은 프로세스 전환보다 **상대적으로 저렴**합니다. address space, page table, heap을 유지할 수 있기 때문입니다.

### User Thread vs Kernel Thread

| 종류 | 특징 |
|------|------|
| User Thread | runtime 관리 |
| Kernel Thread | OS scheduler 관리 |

현대 Linux 대부분은 **1:1 kernel thread 모델**을 사용합니다.

### Thread State

| 상태 | 설명 |
|------|------|
| Running | CPU 실행 중 |
| Runnable | 실행 가능 |
| Sleeping | 이벤트 대기 |
| Blocked | lock/I/O wait |
| Terminated | 종료 |

### Synchronization

공유 메모리 접근 시 필요한 대표 메커니즘:

| 기술 | 목적 |
|------|------|
| mutex | mutual exclusion |
| spinlock | busy wait |
| semaphore | count synchronization |
| rwlock | read/write separation |
| atomic | lock-free update |
| condition variable | event signaling |

### Race Condition

동일 메모리를 동시에 수정할 때 발생합니다.

`x = x + 1` 은 실제로 read → add → write 3단계로 수행됩니다. 동시 실행 시 데이터 손상이 발생할 수 있습니다.

### Thread Pool

현대 서버의 핵심 패턴으로, 매 요청마다 thread를 생성하지 않습니다.

| 비용 | 설명 |
|------|------|
| stack allocation | memory 사용 |
| scheduler registration | kernel overhead |
| context switch | CPU cost |

미리 생성한 **worker thread를 재사용**합니다.

### Blocking vs Non-Blocking

blocking thread는 I/O 동안 CPU를 반납합니다. 대규모 서버는 event loop, async runtime, coroutine 등으로 thread 수를 감소시킵니다.

### Hyper-Threading

동시에 실행 가능한 스레드 수는 **logical CPU 수**와 강하게 연결됩니다.

### NUMA

대규모 서버에서 thread가 어느 CPU, 어느 memory node를 사용하는지가 성능에 영향을 줍니다.

### Thread Affinity

특정 CPU에 thread를 고정할 수 있으며, cache locality 유지, latency 감소, NUMA 최적화를 위해 사용됩니다.

### False Sharing

다른 thread가 같은 cache line을 수정하면 cache invalidation이 폭증합니다. 멀티코어 scalability 저하의 핵심 원인입니다.

---

## 7. Linux/Runtime/K8s에서 관측 방법

### Linux

```bash
# 스레드 확인
ps -eLf

# top thread view
top -H

# 특정 process thread
ps -T -p <PID>

# thread count
cat /proc/<pid>/status

# stack 확인
cat /proc/<pid>/maps

# scheduler 상태
pidstat -t

# context switch
pidstat -w

# kernel scheduler trace
perf sched
```

### Runtime

| 현상 | 의미 |
|------|------|
| runnable thread 증가 | CPU pressure |
| blocked thread 증가 | lock/I/O bottleneck |
| waiting thread 폭증 | synchronization issue |
| stack 증가 | deep recursion/thread explosion |

**thread dump 분석**이 매우 중요합니다.

| 문제 | 현상 |
|------|------|
| deadlock | 서로 대기 |
| starvation | thread 미실행 |
| busy spin | CPU 100% |
| lock contention | throughput 감소 |

### Kubernetes

container 내부도 결국 **Linux thread scheduling** 위에서 동작합니다.

| 현상 | 의미 |
|------|------|
| CPU throttling | thread runnable delay |
| high latency | scheduler backlog |
| pod freeze | blocked thread |
| noisy neighbor | shared CPU contention |

container CPU 제한이 thread execution을 직접 제한할 수 있습니다.

### Observability 도구

| 도구 | 목적 |
|------|------|
| top -H | thread CPU |
| pidstat -t | thread stat |
| perf | scheduler profiling |
| eBPF | thread tracing |
| flamegraph | CPU hotspot |
| strace | syscall tracing |

### 핵심 지표

| 지표 | 의미 |
|------|------|
| thread count | concurrency scale |
| runnable thread | CPU pressure |
| blocked thread | contention |
| context switch/sec | scheduler overhead |
| stack memory | thread memory usage |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*