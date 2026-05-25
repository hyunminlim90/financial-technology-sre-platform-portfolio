# 실행 엔진 (Execution Engine)

> 정독: 0회

## 1. 이 기술이 무엇인가

실행 엔진은(**runtime instruction execution subsystem**):

> 바이트코드, 중간 표현(IR), 가상 명령어를 실제 CPU가 실행 가능한 기계어 흐름으로 변환하고 실행

**핵심 역할: virtual instructions → native machine execution 변환**

### 핵심 기능

| 기능 | 설명 |
|---|---|
| instruction execution | 명령 수행 |
| interpretation | 명령 해석 |
| native compilation | 기계어 생성 |
| runtime optimization | 실행 최적화 |
| memory coordination | 메모리 관리 |
| thread coordination | 동시성 실행 |

즉, 실행 엔진은 **runtime-controlled machine execution coordinator**입니다.

Virtual Machine Bytecode(가상 머신 바이트코드) [[M]](../../100-deep-dive/micro-foundations/virtual-machine-bytecode.md)  
Compiler Intermediate Representation(컴파일러 중간 표현) [[M]](../../100-deep-dive/micro-foundations/compiler-intermediate-representation.md)  
Virtual Instruction Set Architecture(가상 명령어 집합 아키텍처) [[M]](../../100-deep-dive/micro-foundations/virtual-instruction-set-architecture.md)  
Native Machine Code(네이티브 기계어 코드) [[M]](../../100-deep-dive/micro-foundations/native-machine-code.md)  
Just-In-Time Compilation(JIT 컴파일) [[M]](../../100-deep-dive/micro-foundations/just-in-time-compilation.md)  
Instruction Execution Cycle(명령어 실행 사이클) [[M]](../../100-deep-dive/micro-foundations/instruction-execution-cycle.md)  

## 2. 시스템 어디에서 등장하는가

### 대표 위치

| 영역 | 역할 |
|---|---|
| Virtual Machine | 코드 실행 |
| Runtime System | 런타임 제어 |
| JIT Compiler | native code 생성 |
| Interpreter | 명령 해석 |
| GC System | 메모리 회수 |
| Thread Runtime | 스레드 실행 |

### 전체 흐름

```
program → intermediate instructions → execution engine → native machine instructions → CPU execution
```

즉, 실행 엔진은 **abstraction-to-hardware execution bridge**입니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **CPU + Memory**입니다.

| 자원 | 영향 |
|---|---|
| CPU | instruction execution |
| Memory | allocation & GC |
| Cache | execution locality |
| Thread Scheduler | concurrency overhead |
| Disk | code loading |
| Network | distributed runtime latency |

특히 **execution throughput**이 중요합니다. 실행 엔진은 CPU 사용률, latency, throughput, pause time에 직접 영향합니다.

---

## 4. 왜 중요한가

실행 엔진이 없으면 **virtual instructions are non-executable** 상태입니다. 추상 코드, 중간 표현, 바이트코드는 CPU가 직접 이해할 수 없습니다. 실행 엔진이 **runtime execution realization**을 수행해야 실제 하드웨어 연산이 발생합니다.

| 요소 | 이유 |
|---|---|
| portability | 플랫폼 독립 |
| optimization | 성능 향상 |
| memory safety | 안전한 메모리 관리 |
| concurrency | 다중 실행 |
| runtime adaptability | 동적 최적화 |

즉, 실행 엔진은 **dynamic runtime execution infrastructure**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 대표 장애

| 장애 | 설명 |
|---|---|
| GC pause | 실행 정지 |
| JIT compilation storm | CPU 급등 |
| interpreter bottleneck | 처리량 감소 |
| allocation contention | thread 경쟁 |
| deoptimization | 성능 급락 |
| code cache exhaustion | native cache 부족 |
| thread starvation | 실행 지연 |
| runtime crash | VM abort |

대표 증상은 **runtime execution instability**입니다.

특히 **high allocation churn**은 다음을 유발합니다.

- GC amplification
- allocator contention
- cache inefficiency

또한 **stop-the-world pause**는 실무 latency 문제의 핵심 원인 중 하나입니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Instruction Dispatch

```
fetch → decode → execute
```

즉, 명령 읽기 → 의미 해석 → CPU 실행 흐름입니다.

### Interpreter

인터프리터는 **instruction-by-instruction execution** 방식입니다.

| 특징 | 설명 |
|---|---|
| startup 빠름 | 즉시 실행 |
| 반복 비용 큼 | 매번 해석 |
| 단순 구조 | 구현 단순 |

### JIT Compilation

JIT는 **runtime native code generation**입니다.

```
profiling → hotspot detection → native compilation → optimized execution
```

자주 실행되는 코드를 **CPU-optimized machine code**로 변환합니다.

### Runtime Profiling

실행 엔진은 계속 **execution statistics collection**을 수행합니다.

예: invocation count, branch frequency, allocation rate, loop intensity

이 데이터를 기반으로 **adaptive optimization**을 수행합니다.

### Memory Allocation Coordination

실행 엔진은 **runtime heap allocation control**을 담당합니다.

```
allocation request → heap reservation → header initialization → object publication
```

### Garbage Collection Integration

실행 엔진은 **object lifecycle supervision**을 수행합니다.

포함: reachability analysis, heap scanning, compaction, reclamation

### Thread Coordination

실행 엔진은 **managed thread execution**을 수행합니다.

포함: synchronization, execution suspension, safepoint coordination, memory visibility

### Runtime Metadata Usage

실행 엔진은 **runtime object metadata access**를 수행합니다.

예: `object header`, `type metadata`, `method table`, `runtime descriptors`

### Native Transition

실행 엔진은 필요 시 **managed-to-native transition**을 수행합니다. syscall, native library, hardware interface와 연결됩니다.

### Safepoint Mechanism

GC나 런타임 재구성 시 **global execution synchronization**이 필요합니다. 여러 실행 흐름을 잠시 정지시켜 heap consistency, metadata consistency를 확보합니다.

### Code Cache

JIT 생성 기계어는 **native executable cache**에 저장됩니다.

효과: recompilation 감소, execution acceleration, cache locality 향상

### Deoptimization

실행 엔진은 필요 시 **optimized code rollback**을 수행합니다. 즉, **speculative optimization recovery** 메커니즘이 존재합니다.

### Runtime Adaptation

실행 엔진은 고정 시스템이 아닙니다. 계속 **dynamic execution adaptation**을 수행합니다.

예: recompilation, inlining, allocation tuning, runtime scheduling

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**대표 관측 명령어:** `top`, `perf`, `vmstat`, `pidstat`, `strace`, `numastat`

| 현상 | 의미 |
|---|---|
| CPU spike | JIT/GC 증가 |
| context switch 증가 | thread contention |
| syscall 증가 | native transition |
| RSS 증가 | heap growth |
| cache miss 증가 | locality 저하 |

### Runtime

| 항목 | 의미 |
|---|---|
| compilation count | JIT activity |
| GC pause | runtime suspension |
| allocation rate | 객체 생성량 |
| safepoint duration | execution synchronization |
| thread state | runnable/waiting |
| code cache usage | native code density |

**중요 지표:** execution throughput, GC latency, allocation throughput, JIT activity

### Kubernetes

| 현상 | 원인 |
|---|---|
| pod CPU burst | JIT/GC |
| memory explosion | allocation 증가 |
| latency spike | GC pause |
| OOMKill | heap exhaustion |
| startup delay | runtime warmup |

특히 **runtime warmup behavior**가 중요합니다. 초기 실행 단계에서 interpretation, profiling, JIT compilation이 동시에 발생합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*