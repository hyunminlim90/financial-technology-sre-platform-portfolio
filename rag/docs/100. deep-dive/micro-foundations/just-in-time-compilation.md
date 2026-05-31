# JIT 컴파일 (Just-In-Time Compilation)

> 정독: 0회

## 1. 이 기술이 무엇인가

JIT 컴파일은: 

> 프로그램 실행 중(Runtime) 가상 명령어(Bytecode / IR)를 현재 시스템 CPU용 네이티브 기계어로 **실시간 변환**하는 컴파일 기술

### 핵심 특징

| 특징 | 설명 |
|---|---|
| runtime compilation | 실행 중 컴파일 |
| dynamic optimization | 동적 최적화 |
| CPU-aware optimization | 현재 CPU 기반 최적화 |
| adaptive execution | 실행 패턴 적응 |
| hotspot optimization | 빈번 실행 코드 집중 최적화 |

> **핵심 정의:** runtime native code generation and optimization engine

---

## 2. 시스템 어디에서 등장하는가

| 위치 | 역할 |
|---|---|
| virtual machine runtime | runtime execution |
| execution engine | bytecode execution |
| JIT compiler subsystem | native code generation |
| code cache | generated machine code storage |
| runtime profiler | hotspot detection |

### 전체 흐름

```
source code
  → bytecode / IR
  → runtime profiling
  → JIT compilation
  → native machine code
  → CPU execution
```

JIT는 정확히 **virtual execution layer and physical CPU execution layer boundary**에 위치합니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접 영향: **CPU + Memory**

| 자원 | 영향 |
|---|---|
| CPU | compilation + optimization workload |
| Memory | code cache + profiling metadata |
| Instruction Cache | generated native locality |
| Heap | runtime optimization metadata |
| Disk | 간접 영향 |
| Network | 거의 영향 없음 |

JIT는 실행 중 분석, 프로파일링, 최적화, 기계어 생성을 수행합니다. 즉 **application execution and compilation occur simultaneously**하므로 **runtime CPU consumption for optimization**이 특히 중요합니다.

---

## 4. 왜 중요한가

JIT의 핵심 목표: **platform independence with near-native execution performance**

| 방식 | 장점 | 단점 |
|---|---|---|
| interpreter | 높은 이식성 | 느린 실행 |
| AOT | 빠른 실행 | 플랫폼 종속 |
| JIT | 둘 다 확보 | runtime overhead 존재 |

JIT는 플랫폼 독립성 유지, 런타임 기반 최적화, 실제 실행 패턴 기반 튜닝을 동시에 수행합니다.

실제 실행 데이터를 기반으로 branch prediction optimization, inline expansion, loop optimization, dead code elimination 등이 가능한 **runtime-aware optimization**이 특히 중요합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

| 문제 | 설명 |
|---|---|
| warm-up latency | 초기 실행 지연 |
| CPU spike | JIT compilation 부하 |
| code cache exhaustion | executable cache 부족 |
| deoptimization storm | speculative optimization rollback |
| recompilation overhead | 반복 재컴파일 |
| memory growth | metadata 증가 |
| latency jitter | runtime optimization 변동 |

서비스 초기에는 native optimization 미완료, interpreter fallback 존재, hotspot 미확정 상태이므로 **runtime instability during optimization phase**가 특히 중요합니다. startup latency 증가, throughput 불안정, tail latency 증가가 발생할 수 있습니다.

JIT는 **assume current runtime behavior** 방식으로 최적화하지만, 실제 패턴이 바뀌면 optimized native code 폐기 → deoptimization → interpreter rollback → recompilation이 발생하는 **speculative optimization failure**도 중요합니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Hotspot Detection

JIT는 모든 코드를 즉시 컴파일하지 않고, 먼저 **runtime execution profiling**을 수행합니다:

| 항목 | 설명 |
|---|---|
| method invocation count | 호출 빈도 |
| loop backedge count | 루프 반복 |
| branch frequency | 분기 패턴 |
| type profile | 타입 분포 |

자주 실행되는 코드만 **hotspot**으로 승격됩니다.

### Tiered Execution

많은 런타임은 단계별 실행을 사용합니다:

| 단계 | 설명 |
|---|---|
| interpreter | 초기 실행 |
| baseline JIT | 빠른 1차 컴파일 |
| optimized JIT | 고도 최적화 |

즉, **execution quality improves over runtime**입니다.

### Compiler IR Transformation

JIT 내부에서는 다음 변환이 발생합니다:

```
bytecode → compiler IR → optimized IR → native machine code
```

IR 단계에서 CFG optimization, SSA transformation, data-flow analysis, escape analysis 등이 수행됩니다.

### Speculative Optimization

JIT는 실행 패턴을 가정합니다 (예: assume monomorphic callsite). 이를 통해 inline call, branch simplification, type specialization이 가능하지만, 가정 실패 시 **deoptimization**이 발생합니다.

### Code Cache

생성된 네이티브 코드는 **runtime executable memory cache**에 저장됩니다:

| 항목 | 설명 |
|---|---|
| executable pages | 실행 가능 메모리 |
| native instruction storage | 기계어 저장 |
| recompilation target | 재최적화 대상 |

캐시 부족 시 recompilation churn, eviction, performance degradation이 발생할 수 있습니다.

### Runtime Adaptation

JIT는 정적 컴파일러와 달리 **continuously adapts to runtime behavior**합니다. 실제 데이터, 실제 호출 패턴, 실제 branch behavior를 기반으로 최적화합니다.

### Memory Interaction

JIT-generated native code는 heap allocation, stack manipulation, synchronization, pointer arithmetic을 직접 수행합니다. 즉 **JIT output directly manipulates physical runtime state**입니다.

### Inline Optimization

**function call elimination through inlining**은 핵심 최적화입니다: call overhead 감소, branch 감소, register locality 증가, optimization scope 확대

### Escape Analysis

**object lifetime and visibility analysis**는 중요한 런타임 최적화입니다. 객체가 외부로 escape 하지 않으면 stack allocation, allocation elimination, synchronization 제거가 가능합니다.

### Register Allocation

최종 native generation 단계에서 **mapping virtual variables to physical CPU registers**가 수행됩니다. 실패 시 spilling 증가, memory traffic 증가, CPU efficiency 감소가 발생합니다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**대표 도구:** `perf`, `perf top`, `perf record`, `objdump`, `gdb`

| 항목 | 의미 |
|---|---|
| JIT CPU usage | compilation overhead |
| branch misses | optimization quality |
| cache misses | code locality |
| stalled cycles | pipeline inefficiency |
| instruction throughput | execution quality |

> 특히 중요: **runtime-generated executable code behavior**

### Runtime

| 메트릭 | 의미 |
|---|---|
| compilation count | 컴파일 횟수 |
| deoptimization count | rollback 발생 |
| code cache usage | native cache 사용량 |
| compilation time | JIT 비용 |
| hotspot profile | 자주 실행 코드 |

### Kubernetes

| 현상 | 원인 |
|---|---|
| startup latency | warm-up |
| CPU spike | JIT compilation |
| memory growth | code cache |
| latency jitter | recompilation |
| autoscaling noise | transient CPU burst |

짧은 생명주기의 Pod에서는 충분한 JIT 최적화 전에 종료, warm-up 비용 반복, throughput 안정화 실패가 발생할 수 있으므로 **container cold-start amplification**이 특히 중요합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*