# 인스턴스화 (Instantiation)

> 정독: 0회

## 1. 이 기술이 무엇인가

인스턴스화는:

> 정적인 타입 정의, 클래스 메타데이터, 구조 명세를 기반으로 실제 런타임 메모리에 **동적인 실행 실체를 생성**하는 행위

```
abstract type definition
  → runtime memory allocation
  → live object creation
```

### 핵심 특징

| 특징 | 설명 |
|---|---|
| runtime activity | 실행 중 발생 |
| memory allocation | 메모리 확보 필요 |
| state creation | 상태 저장 공간 생성 |
| identity creation | 객체 식별성 생성 |
| lifecycle management | 생성~소멸 관리 |

> **핵심 정의:** runtime materialization of executable data structures

<details>
<summary>Deep Dive</summary></br>



</details></br>

## 2. 시스템 어디에서 등장하는가

| 계층 | 역할 |
|---|---|
| compiler/runtime | type metadata interpretation |
| allocator | memory reservation |
| heap manager | object placement |
| execution engine | creation execution |
| CPU/memory subsystem | actual memory mutation |

### 대표 위치

| 위치 | 설명 |
|---|---|
| heap memory | 객체 저장 |
| runtime metadata area | 타입 정의 |
| allocator subsystem | 메모리 확보 |
| garbage collector | lifecycle 관리 |

> **instantiation is a runtime memory materialization event**

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 큰 영향: **Memory subsystem + CPU cache hierarchy**

| 자원 | 영향 |
|---|---|
| Heap memory | allocation pressure |
| CPU cache | locality |
| Memory bandwidth | allocation throughput |
| GC subsystem | object lifetime cost |
| TLB/cache | address translation/locality |

Network/Disk 영향은 직접적이지 않으며, 핵심은 **allocation rate and memory access locality**입니다.

---

## 4. 왜 중요한가

현대 소프트웨어 대부분은 **continuous runtime object instantiation** 위에서 동작합니다.

request processing, serialization, collections, async execution, stream pipelines, ORM/data mapping 모두 인스턴스화 빈도가 높습니다.

**high allocation rate directly impacts GC and CPU efficiency**이므로 객체 생성 자체가 성능 비용입니다. 특히 대규모 시스템에서는:

| 문제 | 결과 |
|---|---|
| excessive allocation | GC pressure |
| fragmented locality | cache miss 증가 |
| short-lived objects | GC churn |
| oversized objects | memory amplification |

---

## 5. 실제 장애와 어떤 관련이 있는가

| 장애 | 원인 |
|---|---|
| GC pause spike | excessive allocation |
| memory exhaustion | heap saturation |
| CPU spike | allocation + GC overhead |
| latency increase | cache locality degradation |
| allocation contention | allocator synchronization |
| OOM | uncontrolled instantiation |

request당 수천 객체 생성, serialization 폭증, JSON parsing churn, temporary object flood 등으로 발생하는 **allocation storm**이 특히 중요합니다:

```
high allocation rate → GC amplification → tail latency spike
```

메모리 위치가 분산되면 cache miss 증가, memory stall 증가, NUMA penalty 증가가 발생하는 **cache locality degradation**도 중요합니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Type Metadata Reference

인스턴스화 시작점은 **runtime type metadata lookup**입니다. 런타임은 먼저 field layout, object size, alignment requirement, method metadata를 확인합니다.

### Size Calculation

**total object memory size computation** 핵심 단계:

| 구성 | 설명 |
|---|---|
| object header | control metadata |
| fields | actual state |
| alignment padding | hardware alignment |

### Heap Allocation

**heap memory reservation** 대표 전략:

| 전략 | 설명 |
|---|---|
| bump pointer | contiguous allocation |
| free list | reusable fragmented space |
| region allocation | segmented memory |
| slab/pool allocation | preallocated chunks |

### Object Header Initialization

생성 직후 **runtime control metadata initialization**이 발생합니다:

| 메타데이터 | 역할 |
|---|---|
| type pointer | runtime type |
| lock state | synchronization |
| GC metadata | lifecycle tracking |
| hash/identity | object identity |

### Field Initialization

field memory initialization 수행: zero initialization, default values, constructor assignment

### Memory Alignment

현대 CPU는 aligned access, cache line alignment, word-size alignment가 중요합니다. 정렬이 맞지 않으면 extra memory cycles, unaligned access penalty, cache inefficiency가 발생합니다.

### Object Identity

인스턴스화는 **runtime identity creation**이기도 합니다: unique memory location, runtime ownership, independent state 생성

### Heap Locality

**allocation locality strongly affects performance**입니다.

좋은 locality: cache hit 증가, prefetch 효율 증가, branch efficiency 증가

나쁜 locality: memory stall 증가, random access amplification, NUMA latency 증가

### Allocation Fast Path

현대 런타임은 **fast allocation paths**를 적극적으로 최적화합니다. 예: thread-local allocation buffer, bump allocation, region allocator

> 목표: **allocation without global lock contention**

### Allocation vs Lifetime

| 유형 | 영향 |
|---|---|
| short-lived objects | young GC pressure |
| long-lived objects | heap growth |
| medium-lived objects | fragmentation risk |

> **allocation cost depends heavily on object lifetime distribution**

### Escape Analysis

**determine whether object truly requires heap allocation** 최적화를 통해 가능하면 stack allocation, scalar replacement로 최적화합니다.

### Physical Memory Mutation

최종적으로 인스턴스화는 **actual RAM state mutation**입니다. 실제로 cache line allocation, RAM cell mutation, TLB update, memory bus activity가 발생합니다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**관측 도구:** `perf`, `vmstat`, `top`, `numastat`, `sar`

| 항목 | 의미 |
|---|---|
| page faults | allocation pressure |
| cache misses | locality 문제 |
| memory bandwidth | allocation churn |
| NUMA imbalance | remote access |
| CPU cycles | allocation overhead |

### Runtime

| 항목 | 의미 |
|---|---|
| allocation rate | 객체 생성량 |
| heap usage | 메모리 소비 |
| GC frequency | allocation pressure |
| object lifetime | 생존 패턴 |
| promotion rate | old generation 이동 |

> 특히 중요: **allocation rate per second**

### Kubernetes

| 현상 | 원인 |
|---|---|
| pod OOMKill | excessive heap growth |
| CPU throttling | GC amplification |
| latency spike | allocation churn |
| noisy neighbor | memory bandwidth contention |
| node pressure | heap overcommit |

> 특히 중요: **high object churn causing GC-driven latency instability**