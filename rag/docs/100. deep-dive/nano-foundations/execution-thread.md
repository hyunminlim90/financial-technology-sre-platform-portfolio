# 실행 스레드 (Execution Thread)

> 정독: 0회

## 1. 이 기술이 무엇인가

실행 스레드는:

> 프로세스 내부에서 독립적인 실행 흐름(Control Flow)을 가지며 실제 명령어를 수행하는 **최소 실행 단위**

| 요소 | 설명 |
|------|------|
| independent execution flow | 독립 실행 흐름 |
| shared process resources | 프로세스 자원 공유 |
| own stack | 독립 스택 보유 |
| own PC register | 독립 실행 위치 보유 |
| schedulable unit | CPU 스케줄링 단위 |

> CPU가 실제로 실행 대상으로 삼는 **런타임 실행 단위**입니다.

---

## 2. 시스템 어디에서 등장하는가

**대표 등장 위치:**

```
Process → Thread → Instruction Execution
```

**계층별 위치:**

| 계층 | 역할 |
|------|------|
| Operating System | kernel thread scheduling |
| Runtime | managed thread control |
| CPU Scheduler | execution dispatch |
| Application | concurrent task execution |
| Container | multithreaded workload |

---

## 3. 어떤 자원에 가장 영향이 큰가

### CPU (가장 직접적)

스레드는 실제 CPU 실행 단위입니다. 대표 영향:

- scheduling
- context switching
- parallel execution
- CPU utilization

### Memory (매우 중요)

각 스레드는 **own stack / own register context**를 가집니다.

하지만 **heap / shared object**는 공유하므로 synchronization, lock, memory visibility 문제가 발생합니다.

### Network (고성능 서버에서 중요)

- network worker thread
- event processing thread
- async I/O thread

### Disk (I/O 스레드에서 영향)

- file processing thread
- logging thread
- storage worker

---

## 4. 왜 중요한가

**현대 시스템의 병렬성과 처리량을 결정**하기 때문입니다.

스레드가 없으면 다음이 발생합니다:

- 동시 처리 불가
- blocking 처리 비효율
- CPU utilization 저하

| 항목 | 영향 |
|------|------|
| concurrency | 동시성 |
| throughput | 처리량 |
| responsiveness | 응답성 |
| scalability | 확장성 |
| resource efficiency | 자원 효율 |

---

## 5. 실제 장애와 어떤 관련이 있는가

**대표 장애:**

| 장애 | 원인 |
|------|------|
| thread starvation | scheduler imbalance |
| deadlock | lock dependency |
| livelock | endless state transition |
| excessive context switching | too many threads |
| thread leak | unbounded thread creation |
| stack overflow | deep call stack |
| CPU saturation | runaway thread execution |
| race condition | shared memory conflict |

특히 다음 환경에서 중요합니다:

- 금융 시스템
- 고트래픽 서버
- 분산 시스템
- 실시간 처리 시스템

---

## 6. 핵심 메커니즘

### (1) Thread는 독립 실행 흐름이다

각 스레드는 **own execution path**를 가집니다. 동시에 서로 다른 코드를 실행할 수 있습니다.

### (2) PC Register 독립 보유

각 스레드는 **current instruction location**을 따로 유지합니다. 그래야 실행 위치가 충돌하지 않습니다.

### (3) Stack 독립 보유

각 스레드는 자신만의 다음을 가집니다:

- stack frame
- local variable
- return address

### (4) Heap은 공유된다

프로세스 내부 스레드들은 **shared heap memory**를 사용합니다. 따라서 다음 문제가 발생합니다:

- synchronization
- lock
- memory visibility

### (5) Context Switching 발생

OS scheduler는 CPU를 번갈아 배정합니다. 전환 시:

```
register save → stack pointer save → PC save
```

이것이 **context switch**입니다.

### (6) Thread Scheduling

스레드는 CPU를 독점하지 못합니다. scheduler가 다음 기준으로 실행 순서를 결정합니다:

- priority
- timeslice
- CPU availability

### (7) Synchronization 필요

공유 메모리 접근 시 필수입니다:

- mutex
- semaphore
- monitor
- rwlock
- atomic operation

### (8) Blocking vs Non-blocking Thread

| 방식 | 특징 |
|------|------|
| blocking thread | I/O 대기 중 정지 |
| non-blocking thread | event 기반 처리 |

현대 고성능 시스템은 **non-blocking 구조**를 선호합니다.

### (9) Kernel Thread vs User Thread

| 유형 | 특징 |
|------|------|
| kernel thread | OS scheduler 직접 관리 |
| user thread | runtime 내부 관리 |

---

## 7. Linux / Runtime / K8s에서 관측 방법

### Linux

```bash
top -H
ps -eLf
pidstat -t
```

**관측 대상:** thread count, CPU usage per thread, context switch, blocked thread

### Runtime 도구

| 도구 | 역할 |
|------|------|
| thread dump | thread state inspection |
| async-profiler | thread execution profile |
| flame graph | thread hotspot |
| JFR | runtime thread event |
| perf | kernel scheduling analysis |

### Kubernetes

| 현상 | 의미 |
|------|------|
| CPU throttling | excessive runnable threads |
| pod latency increase | thread contention |
| OOMKill | excessive stack/thread memory |
| high restart count | unstable thread execution |
| node load spike | thread explosion |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*