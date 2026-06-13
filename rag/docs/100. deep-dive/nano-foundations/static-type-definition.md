# 정적 타입 정의 (Static Type Definition)

> 정독: 0회

## 1. 이 기술이 무엇인가

정적 타입 정의는:

> 프로그램 실행 전에 데이터 형태, 메모리 구조, 허용 연산, 크기, 정렬 규칙, 접근 규약을 미리 확정하는 메커니즘

**핵심:** type constraints are validated before runtime execution

정적 타입 시스템은 런타임 이전 단계에서 타입 불일치, 잘못된 연산, 잘못된 메모리 접근 가능성을 사전 차단합니다.

---

## 2. 시스템 어디에서 등장하는가

| 계층 | 역할 |
|---|---|
| parser | 타입 선언 인식 |
| semantic analyzer | 타입 검증 |
| compiler frontend | 타입 추론/체크 |
| optimizer | 타입 기반 최적화 |
| runtime metadata | 타입 정보 저장 |
| allocator | 객체 크기 계산 |

**type metadata drives runtime memory layout**이 특히 중요합니다. field offset, object size, alignment, calling convention 모두 타입 정의 기반으로 계산됩니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 영향 큰 자원: **CPU + Memory subsystem**

정적 타입 정보는 메모리 배치, 레지스터 사용, 캐시 locality, SIMD/vectorization, branch optimization에 직접 영향을 줍니다:

| 자원 | 영향 |
|---|---|
| CPU | optimized instruction generation |
| Memory | layout/alignment |
| Cache | locality optimization |
| Disk | metadata size |
| Network | serialization format 영향 가능 |

> **type certainty enables aggressive optimization**

---

## 4. 왜 중요한가

정적 타입 정의는 **runtime uncertainty reduction**의 핵심입니다.

컴파일러는 타입을 알면 다음 최적화가 가능합니다:

| 가능 최적화 | 설명 |
|---|---|
| fixed-size allocation | 객체 크기 확정 |
| direct field offset | 빠른 접근 |
| register allocation | CPU 효율 증가 |
| vectorization | SIMD 최적화 |
| inline expansion | 함수 호출 제거 |
| devirtualization | 동적 dispatch 감소 |

> **type information is optimization infrastructure**

안정성 측면에서도 invalid memory access 감소, undefined behavior 감소, ABI consistency 유지, serialization consistency 확보가 가능합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

| 장애 | 원인 |
|---|---|
| type confusion | 잘못된 타입 해석 |
| memory corruption | layout mismatch |
| ABI incompatibility | binary interface 충돌 |
| serialization failure | schema mismatch |
| runtime crash | invalid casting |
| performance regression | dynamic dispatch 증가 |

**incorrect type assumptions can break runtime memory safety**가 특히 중요합니다.

| 문제 | 영향 |
|---|---|
| oversized structures | memory amplification |
| poor alignment | cache inefficiency |
| excessive polymorphism | branch misprediction |
| unstable type layout | JIT deoptimization |

대규모 시스템에서는 **type instability increases runtime unpredictability**입니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Compile-Time Type Resolution

**type meaning resolved before runtime**이 핵심 시작점입니다. 컴파일러는 변수 타입, 함수 시그니처, 데이터 구조, 메모리 크기를 미리 확정합니다.

### Type Metadata Generation

정적 타입 정의는 런타임 메타데이터 생성으로 이어집니다:

| 정보 | 설명 |
|---|---|
| field list | 필드 정의 |
| field offset | 메모리 위치 |
| alignment | 정렬 규칙 |
| inheritance/interface | 타입 관계 |
| method table | dispatch 정보 |

### Fixed Memory Layout

**static types enable deterministic memory layout**이 핵심입니다. 런타임 전에 object size, field offsets, alignment boundaries를 계산할 수 있으며, 이를 통해 빠른 field access, predictable allocation, cache-friendly layout이 가능합니다.

### Type Safety

정적 타입 정의의 핵심 목적 중 하나는 **prevent invalid operations before execution**입니다. integer + invalid pointer, incompatible assignment, invalid function call 등을 사전 차단합니다.

### Type-Driven Optimization

**compiler optimization depends heavily on type certainty**가 매우 중요합니다:

| 최적화 | 타입 필요 여부 |
|---|---|
| inlining | 필요 |
| vectorization | 필요 |
| escape analysis | 필요 |
| register allocation | 필요 |
| alias analysis | 필요 |

### ABI Compatibility

**type layout defines binary compatibility**입니다. function calling convention, structure alignment, stack layout 모두 타입 정의 기반입니다.

### Runtime Type Metadata

정적 타입은 런타임에도 reflection, dynamic dispatch, RTTI, interface lookup, JIT optimization에 사용됩니다.

> **compile-time types continue influencing runtime behavior**

### Object Size Computation

인스턴스화 직전 **runtime allocator computes object size from static type definition**이 수행됩니다:

| 요소 | 설명 |
|---|---|
| header | control metadata |
| fields | actual state |
| padding | alignment |

### Alignment Rules

**static type definition determines alignment requirements**입니다. 정렬은 CPU access efficiency, cache efficiency, SIMD compatibility에 영향을 줍니다.

### Type Erasure vs Reification

| 전략 | 특징 |
|---|---|
| type erasure | 런타임 타입 제거 |
| reified type | 런타임 타입 유지 |

이는 memory overhead, runtime flexibility, JIT optimization에 영향을 줍니다.

### Static vs Dynamic Typing

| 구분 | 정적 타입 | 동적 타입 |
|---|---|---|
| 검증 시점 | compile-time | runtime |
| 레이아웃 | fixed | flexible |
| 최적화 | predictable | runtime adaptation |
| 런타임 오버헤드 | lower | higher |

### Hardware-Level Impact

**static type information shapes actual machine code generation**입니다. 타입 정의는 결국 register usage, memory access pattern, instruction selection, cache behavior까지 영향을 줍니다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**관측 도구:** `perf`, `objdump`, `readelf`, `nm`, `pahole`

| 도구 | 확인 가능 |
|---|---|
| pahole | structure padding/alignment |
| perf | branch/cache behavior |
| objdump | generated machine code |
| readelf | symbol/type metadata |

### Runtime

| 항목 | 의미 |
|---|---|
| object layout | memory structure |
| type metadata | runtime type info |
| JIT specialization | type-based optimization |
| deoptimization | type assumption failure |
| polymorphic call sites | dispatch instability |

> 특히 중요: **runtime optimization quality strongly depends on stable type information**

### Kubernetes

직접 노출되지는 않지만 간접 영향이 큽니다:

| 현상 | 원인 |
|---|---|
| CPU spike | poor optimization |
| memory inflation | oversized layouts |
| GC pressure | inefficient object structures |
| latency jitter | polymorphic instability |

대규모 서비스에서는 **type-driven memory layout affects cluster efficiency**까지 연결됩니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*