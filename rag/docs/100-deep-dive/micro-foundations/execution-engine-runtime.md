# 실행 엔진 런타임 (Execution Engine Runtime)

> 정독: 0회

## 1. 이 기술이 무엇인가

실행 엔진 런타임은:

> 프로그램의 명령어를 실제 실행 가능한 상태로 유지하면서, CPU·메모리·스레드·I/O 같은 시스템 자원을 동적으로 관리하는 **실행 환경**

| 영역 | 역할 |
|------|------|
| instruction execution | 명령 실행 |
| runtime state management | 실행 상태 관리 |
| memory lifecycle | 메모리 생명주기 관리 |
| scheduling | 실행 순서 조정 |
| optimization | 실행 최적화 |

> 정적인 코드가 실제 시스템 프로세스로 살아 움직이게 만드는 **실행 운영 계층**입니다.

<details>
<summary>Deep Dive</summary></br>

Bytecode Instruction(바이트코드 인스트럭션) [[M]](../../100-deep-dive/micro-foundations/bytecode-instruction.md)  
Executable State Management(실행 가능 상태 관리) [[M]](../../100-deep-dive/micro-foundations/executable-state-management.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

**실행 위치:**

```
Source Code
→ Compiler
→ Bytecode / Intermediate Representation
→ Runtime
→ Execution Engine
→ CPU
```

**대표 등장 위치:**

| 계층 | 역할 |
|------|------|
| Virtual Machine | managed runtime |
| Language Runtime | execution environment |
| OS Process Runtime | process execution |
| JIT Runtime | dynamic compilation |
| Container Runtime | isolated execution |

---

## 3. 어떤 자원에 가장 영향이 큰가

### CPU (가장 직접적)

다음이 모두 CPU 중심입니다:

- instruction execution
- JIT compilation
- thread scheduling
- synchronization

### Memory (매우 중요)

대표 관리 대상:

- heap / stack
- object lifecycle
- buffer / cache

> 런타임 품질은 **메모리 관리 품질**과 거의 직결됩니다.

### Disk (간접 연결)

- class/module loading
- executable loading
- swap / logging

### Network (분산 시스템에서 중요)

- RPC runtime
- async event loop
- socket runtime
- service mesh interaction

---

## 4. 왜 중요한가

**실행 안정성과 성능의 핵심 계층**이기 때문입니다.

동일 코드라도 runtime 구조, scheduler, GC, optimizer, execution strategy에 따라 성능이 완전히 달라집니다.

| 항목 | 영향 |
|------|------|
| latency | 응답속도 |
| throughput | 처리량 |
| memory efficiency | 메모리 효율 |
| scalability | 확장성 |
| stability | 안정성 |

---

## 5. 실제 장애와 어떤 관련이 있는가

**대표 장애:**

| 장애 | 원인 |
|------|------|
| memory leak | runtime memory lifecycle failure |
| GC pause | runtime memory reclamation |
| CPU spike | excessive execution |
| thread starvation | scheduling imbalance |
| deadlock | synchronization failure |
| OOM | runtime allocation exhaustion |
| execution stall | blocking runtime path |
| context switching overhead | excessive thread activity |

특히 다음 환경에서 중요합니다:

- 고트래픽 시스템
- JVM/CLR runtime
- container runtime
- async runtime
- distributed runtime

---

## 6. 핵심 메커니즘

### (1) Runtime은 실행 중 상태를 유지한다

정적 코드와 가장 큰 차이입니다. 실행 중 runtime은 다음을 계속 유지합니다:

- 현재 instruction 위치
- thread 상태
- memory allocation 상태
- stack frame 상태

### (2) Execution Engine이 핵심이다

runtime 내부 핵심으로, 다음 실행 루프를 지속 수행합니다:

```
instruction fetch → decode → dispatch → execute
```

### (3) Memory Lifecycle 관리

runtime은 다음 전체를 관리합니다:

```
allocation → retention → reclamation
```

대표: heap allocation, object reachability, garbage collection

### (4) Thread Runtime 관리

runtime은 다음을 관리합니다:

- thread creation
- scheduling
- synchronization
- blocking / wake-up

### (5) Dynamic Optimization

| 기술 | 역할 |
|------|------|
| JIT | runtime compilation |
| profiling | hotspot detection |
| inline optimization | call reduction |
| escape analysis | allocation optimization |

> runtime은 단순 실행기가 아니라 **실행 최적화 시스템**입니다.

### (6) Runtime State Tracking

runtime은 지속적으로 다음 상태를 추적합니다:

- PC register
- stack pointer / frame pointer
- call stack
- monitor/lock state

### (7) Runtime Isolation

runtime은 다음을 통해 안정성을 유지합니다:

- memory isolation
- process isolation
- thread isolation

### (8) Runtime ↔ OS 연동

runtime은 독립 시스템이 아닙니다:

```
runtime ↔ system call ↔ kernel ↔ hardware
```

### (9) Managed Runtime vs Native Runtime

| 유형 | 특징 |
|------|------|
| managed runtime | GC, safety, abstraction |
| native runtime | direct hardware control |

---

## 7. Linux / Runtime / K8s에서 관측 방법

### Linux

```bash
top
htop
ps -ef
perf top
```

**관측 대상:** CPU usage, memory usage, thread count, context switching, runtime stalls

### Runtime 도구

| 도구 | 역할 |
|------|------|
| JFR | runtime event analysis |
| async-profiler | execution profiling |
| flame graph | execution path |
| heap dump | memory analysis |
| thread dump | runtime thread state |

### Kubernetes

| 지표 | 의미 |
|------|------|
| CPU throttling | runtime execution pressure |
| memory limit hit | allocation pressure |
| OOMKill | runtime exhaustion |
| pod restart | runtime instability |
| high latency | runtime contention |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*