# 실행 시간 (Run Time / Execution Time)

> 정독: 0회

## 1. 이 기술이 무엇인가

실행 시간(Runtime)은:

> 프로그램이 실제로 메모리에 적재되어 CPU 연산과 메모리 사용이 발생하는 **동적 실행 상태**를 의미

핵심: **code becomes active execution**

| 상태 | 특징 |
|------|------|
| Compile Time | 정적 분석 |
| Run Time | 실제 실행 |

실행 시간에는 다음이 실제로 발생합니다:

- CPU 연산
- 메모리 할당
- 스레드 실행
- I/O 처리
- 객체 생성/회수
- 네트워크 통신

> 실행 시간은 소프트웨어 논리가 **실제 시스템 자원을 소비하며 동작하는 운영 단계**입니다.

---

## 2. 시스템 어디에서 등장하는가

실행 시간은 프로그램이 시작되는 순간부터 종료될 때까지 **전체 시스템에 걸쳐** 등장합니다.

| 영역 | 역할 |
|------|------|
| Process Runtime | 프로세스 실행 |
| Thread Runtime | 스레드 흐름 |
| Heap/Stack | 메모리 사용 |
| Execution Engine | 명령 실행 |
| I/O System | 외부 통신 |
| Scheduler | CPU 분배 |

**전체 흐름:**

```
program start
→ process creation
→ memory allocation
→ instruction execution
→ runtime interaction
→ process termination
```

---

## 3. 어떤 자원에 가장 영향이 큰가

실행 시간은 모든 컴퓨팅 자원에 영향을 줍니다. 특히 **CPU / Memory** 영향이 가장 큽니다.

### CPU

런타임 동안 CPU는 다음을 수행합니다:

- instruction execution
- thread scheduling
- branch execution
- arithmetic operation

### Memory

실행 중 메모리에는 다음이 생성됩니다:

- stack frame
- heap object
- runtime metadata
- instruction cache

### Network / Disk

실행 중 다음도 발생합니다:

- file read/write
- socket communication
- database I/O

> 런타임은 **전체 시스템 자원을 동시에 움직이는 단계**입니다.

---

## 4. 왜 중요한가

**프로그램의 실제 품질은 실행 시간에 결정**됩니다.

| 이유 | 설명 |
|------|------|
| 실제 성능 | runtime throughput |
| 안정성 | crash/OOM 여부 |
| 동시성 | thread behavior |
| 메모리 효율 | allocation/reclaim |
| 실시간 반응성 | latency |

컴파일 성공은 **correct syntax**만 의미합니다. 하지만 런타임은 **actual system behavior**를 결정합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

**실무 장애 대부분은 런타임에서 발생**합니다.

| 장애 | 원인 |
|------|------|
| OOM | runtime memory exhaustion |
| CPU Spike | excessive execution |
| Deadlock | runtime synchronization |
| Race Condition | concurrent execution |
| GC Pause | runtime memory reclaim |
| Latency Spike | blocking I/O |
| Crash | invalid runtime state |

특히 **runtime unpredictability** 문제가 중요합니다.

컴파일 시에는 정상이어도, 실행 중 다음은 예측 불가능합니다:

- 실제 데이터 크기
- 실제 동시 사용자
- 실제 네트워크 상태
- 실제 메모리 압박

---

## 6. 핵심 메커니즘

### (1) Process Creation

프로그램 시작 시 OS가 프로세스를 생성합니다:

- virtual memory
- process metadata
- thread context

### (2) Runtime Memory

실행 시간에는 메모리가 동적으로 사용됩니다:

| 영역 | 역할 |
|------|------|
| Stack | call frame |
| Heap | dynamic object |
| Code Area | executable instruction |
| Metadata Area | runtime info |

### (3) Instruction Execution

실행 엔진은 다음을 반복 수행합니다:

```
fetch → decode → execute
```

### (4) Dynamic Allocation

실행 중 runtime allocation이 지속 발생합니다:

- object creation
- buffer allocation
- thread creation

### (5) Runtime State

런타임은 현재 상태를 유지합니다:

- PC register
- stack frame
- heap reference
- thread state

### (6) Concurrency

현대 런타임의 핵심인 **multi-thread execution**을 포함합니다:

- synchronization
- scheduling
- memory visibility

### (7) Runtime Optimization

실행 중 런타임은 최적화를 수행합니다:

- hot path optimization
- adaptive compilation
- instruction optimization

### (8) Garbage Collection

런타임은 메모리 회수도 수행합니다:

```
execution + memory management  →  동시 진행
```

---

## 7. Linux / Runtime / K8s에서 관측 방법

### Runtime

| 항목 | 의미 |
|------|------|
| thread state | 실행 흐름 |
| heap usage | 메모리 사용 |
| allocation rate | 객체 생성 |
| GC activity | 메모리 회수 |
| execution latency | 처리 시간 |

**대표 도구:** `jstack`, `jstat`, `jcmd`, runtime profiler, thread dump

**대표 관측 신호:** thread contention, heap growth, execution stall, GC pause

### Linux

```bash
top
htop
vmstat
pidstat
sar
perf
strace
```

| 현상 | 의미 |
|------|------|
| CPU spike | execution overload |
| RSS 증가 | heap growth |
| high context switch | thread contention |
| blocked syscall | I/O wait |

### Kubernetes

**대표 현상:** OOMKilled, restart, probe timeout, CPU throttling, latency spike

```
runtime overload
→ resource saturation
→ degraded response
→ orchestration recovery
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*