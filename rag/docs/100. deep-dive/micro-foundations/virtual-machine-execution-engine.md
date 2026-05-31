# 가상 머신 실행 엔진 (Virtual Machine Execution Engine)

> 정독: 0회

## 1. 이 기술이 무엇인가

가상 머신 실행 엔진은(**virtual instruction execution subsystem**):

> 실행 엔진은 바이트코드, 중간 표현(IR), 가상 명령어 같은 추상 실행 단위를 실제 CPU가 실행 가능한 형태로 변환하고 수행

**핵심 역할: abstract intermediate instructions → native machine execution 변환**

### 핵심 기능

| 기능 | 설명 |
|---|---|
| instruction dispatch | 명령 해석 |
| native execution | 기계어 실행 |
| memory allocation | 런타임 메모리 관리 |
| optimization | 실행 최적화 |
| garbage collection | 메모리 회수 |
| thread scheduling interaction | 동시성 실행 |

즉, 실행 엔진은 **runtime-controlled execution coordinator**입니다.

---

## 2. 시스템 어디에서 등장하는가

### 대표 등장 위치

| 영역 | 역할 |
|---|---|
| Runtime System | 코드 실행 |
| Managed Runtime | 메모리 관리 |
| JIT Compiler | native code 생성 |
| Interpreter | instruction 해석 |
| GC Engine | heap 정리 |
| Thread Runtime | execution control |

### 전체 흐름

```
program → intermediate representation → execution engine → CPU instructions
```

실행 엔진은 **runtime abstraction layer**로 볼 수 있습니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **CPU + Memory**입니다.

| 자원 | 영향 |
|---|---|
| CPU | instruction execution |
| Memory | allocation & GC |
| Cache | compiled code locality |
| Thread Scheduler | execution concurrency |
| Disk | code loading |
| Network | distributed runtime interaction |

특히 **instruction throughput**이 중요합니다. 실행 엔진 성능은 latency, throughput, pause time, CPU efficiency에 직접 연결됩니다.

---

## 4. 왜 중요한가

프로그램은 단순 파일 상태로는 실행되지 않습니다. 실행 엔진이 **runtime realization**을 수행해야 실제 연산이 발생합니다.

| 요소 | 필요 이유 |
|---|---|
| portability | 플랫폼 독립 |
| optimization | 실행 성능 향상 |
| memory safety | 안전한 메모리 관리 |
| concurrency | 다중 실행 흐름 |
| runtime adaptability | 동적 최적화 |

즉, 실행 엔진은 **dynamic execution infrastructure**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 대표 장애

| 장애 | 설명 |
|---|---|
| excessive GC | pause 증가 |
| JIT warmup delay | 초기 latency |
| allocation contention | thread 경쟁 |
| interpreter overhead | CPU 낭비 |
| deoptimization storm | 성능 급락 |
| memory fragmentation | allocation 실패 |
| thread starvation | 실행 지연 |
| runtime crash | VM abort |

대표 현상은 **runtime instability**입니다. 특히 **high allocation rate**는 다음을 유발합니다.

- GC amplification
- allocator contention
- CPU spike

또한 **stop-the-world pauses**는 실무에서 매우 중요합니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Instruction Execution

```
instruction fetch → decode → execute
```

즉, 명령 읽기 → 의미 해석 → CPU 실행 흐름입니다.

### Interpreter

인터프리터는 **instruction-by-instruction execution** 방식입니다.

| 특징 | 설명 |
|---|---|
| startup 빠름 | 즉시 실행 |
| 반복 실행 느림 | 반복 해석 비용 |
| 단순 구조 | 구현 쉬움 |

### JIT Compilation

JIT는 **runtime native compilation**입니다.

```
hot code detection → native compilation → optimized execution
```

자주 실행되는 코드를 **CPU-native optimized code**로 변환합니다.

### Runtime Profiling

실행 엔진은 **execution statistics**를 수집합니다.

예: hot path, branch frequency, allocation rate, method invocation count

이 데이터를 기반으로 **adaptive optimization**을 수행합니다.

### Memory Allocation

실행 엔진은 **runtime heap allocation**을 담당합니다.

```
size calculation → heap reservation → header initialization → reference publication
```

### Garbage Collection Coordination

실행 엔진은 **object lifecycle supervision**을 수행합니다.

포함: object tracking, reachability analysis, reclamation, compaction

### Thread Execution Control

실행 엔진은 **managed thread coordination**을 수행합니다.

포함: synchronization, safepoint coordination, execution suspension, memory visibility

### Runtime Metadata Access

실행 엔진은 **runtime object metadata**를 사용합니다.

예: `object header`, `type metadata`, `vtable metadata`, `method descriptors`

### Native Transition

실행 엔진은 필요 시 **managed-to-native transition**을 수행합니다. OS syscall, native library, hardware interface와 연결됩니다.

### Safepoint Mechanism

GC나 런타임 변경 시 **global execution synchronization point**가 필요합니다. 여러 스레드를 잠시 정지시켜 heap consistency, metadata consistency를 확보합니다.

### Code Cache

JIT 생성 기계어는 **native code cache region**에 저장됩니다. 이는 repeated compilation 감소, CPU locality 향상 효과를 만듭니다.

### Deoptimization

실행 엔진은 잘못된 최적화 발생 시 **optimized code rollback**을 수행할 수 있습니다. 즉, **speculative optimization recovery** 메커니즘이 존재합니다.

### Runtime Adaptation

실행 엔진은 정적이지 않습니다. 실행 중 계속 **dynamic runtime adaptation**을 수행합니다.

예: recompilation, inline expansion, allocation tuning, GC tuning

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**대표 관측 명령어:** `top`, `perf`, `vmstat`, `pidstat`, `numastat`, `strace`

| 현상 | 의미 |
|---|---|
| CPU spike | JIT/GC 증가 |
| context switch 증가 | thread contention |
| memory growth | allocation 증가 |
| syscall 증가 | runtime-native interaction |

### Runtime

| 항목 | 의미 |
|---|---|
| JIT compilation count | native compile 빈도 |
| GC pause time | stop-the-world |
| allocation rate | 객체 생성량 |
| thread state | runnable/waiting |
| safepoint duration | runtime synchronization |
| code cache usage | compiled code density |

**대표 메트릭:** execution throughput, allocation throughput, GC latency, JIT activity

### Kubernetes

| 현상 | 원인 |
|---|---|
| pod CPU burst | JIT/GC |
| memory limit 초과 | heap expansion |
| latency spike | GC pause |
| OOMKill | allocation overflow |
| startup latency | runtime warmup |

특히 **runtime warmup behavior**가 중요합니다. 실행 초기에는 interpretation, profiling, compilation이 동시에 발생합니다.