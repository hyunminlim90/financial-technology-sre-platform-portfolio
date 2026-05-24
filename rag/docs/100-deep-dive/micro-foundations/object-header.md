# 객체 헤더 (Object Header)

> 정독: 0회

## 1. 이 기술이 무엇인가

객체 헤더(Object Header)는:

> 런타임이 메모리에 생성된 객체를 추적·관리·제어하기 위해 객체 메모리의 시작 부분에 배치하는 메타데이터 영역입니다.

**핵심 역할: object identity + runtime control + synchronization state + type metadata linkage**

객체 자체 데이터 이전에 존재하며 다음 정보를 저장합니다.

- 객체 상태
- 타입 정보
- 동기화 정보
- GC 상태

즉, 객체 헤더는 **runtime-governed object metadata region**입니다.

<details>
<summary>Deep Dive</summary></br>

Offset 0 Base(오프셋 0 기점) [[M]](../../100-deep-dive/micro-foundations/offset-0-base.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

객체 헤더는 런타임 기반 메모리 시스템 전반에서 등장합니다.

### 대표 위치

| 계층 | 역할 |
|---|---|
| Runtime Heap | 객체 메타데이터 |
| Garbage Collector | 객체 추적 |
| Synchronization Engine | lock state |
| Type System | runtime type identification |
| Object Allocator | allocation metadata |
| Concurrent Runtime | thread coordination |

### 대표 흐름

```
object allocation → header initialization → runtime tracking
```

즉, 객체 헤더는 **runtime object governance**의 시작점입니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적 영향은 **Memory**입니다.

| 요소 | 영향 |
|---|---|
| object density | heap occupancy |
| alignment | cache efficiency |
| lock metadata | synchronization overhead |
| GC metadata | collector traversal |
| pointer metadata | memory footprint |

객체 수가 많아질수록 **header overhead accumulation**이 발생합니다. 작은 객체가 매우 많으면 다음이 증가할 수 있습니다.

- heap inflation
- cache inefficiency
- GC pressure

---

## 4. 왜 중요한가

런타임은 단순 메모리 블록만으로 객체를 관리할 수 없습니다. 다음 정보가 필요하며, 이를 담당하는 것이 객체 헤더입니다.

- 이 객체 타입은 무엇인가
- 누가 사용 중인가
- GC 대상인가
- lock 상태는 무엇인가
- hash 상태는 무엇인가

즉, 객체 헤더는 **runtime object identity layer**입니다.

### 중요 이유

| 이유 | 설명 |
|---|---|
| synchronization | 락 제어 |
| garbage collection | 객체 추적 |
| runtime type lookup | 타입 판별 |
| memory management | 객체 관리 |
| concurrent coordination | 동시성 통제 |

---

## 5. 실제 장애와 어떤 관련이 있는가

객체 헤더는 성능 및 메모리 문제와 매우 직접적으로 연결됩니다.

### 대표 문제

| 문제 | 설명 |
|---|---|
| excessive object count | 헤더 오버헤드 증가 |
| lock contention | synchronization 비용 |
| GC traversal explosion | 객체 탐색 증가 |
| heap fragmentation | 메모리 비효율 |
| cache miss | poor object locality |

특히 중요한 것은 **small-object explosion**입니다. 수억 개의 작은 객체 생성 시 `payload size < header overhead` 상황이 발생할 수 있습니다. 즉, 실제 데이터보다 metadata, alignment, pointer space가 더 커질 수 있습니다.

또한 락 경쟁 증가 시 **header mutation frequency**가 증가하며, 이는 다음으로 이어질 수 있습니다.

- cache invalidation
- synchronization slowdown
- memory traffic 증가

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Object Layout

일반적 메모리 구조는 다음과 같습니다.

```
[ Object Header ]
[ Instance Data ]
[ Padding       ]
```

즉, 객체 헤더는 **offset 0**에 존재합니다.

### Runtime Metadata

객체 헤더는 **runtime control metadata**를 저장합니다.

| 메타데이터 | 역할 |
|---|---|
| lock state | synchronization |
| GC age | generational tracking |
| hash state | identity |
| type pointer | runtime type lookup |

### Type Metadata Pointer

객체는 자신의 타입 메타데이터를 가리킵니다. `object → type metadata` 관계가 존재하며, 런타임은 이를 통해 다음을 수행합니다.

- virtual dispatch
- polymorphism
- type checking

### Synchronization State

동시성 시스템에서는 객체 자체가 lock 대상이 될 수 있습니다. 따라서 헤더에는 **monitor / lock metadata**가 저장될 수 있습니다.

대표 상태: `unlocked`, `lightweight lock`, `heavyweight lock`, `biased ownership`

### GC Metadata

GC는 객체 헤더를 통해 survival age, mark state, forwarding state 등을 추적합니다. 즉, 객체 헤더는 **GC traversal anchor** 역할을 수행합니다.

### Object Identity

일부 런타임에서는 **identity hash**가 헤더 내부에 저장됩니다. 즉, 객체 헤더는 **identity persistence layer** 역할도 수행합니다.

### Array Header Extension

배열 객체는 일반 객체보다 추가 메타데이터를 가집니다. 대표 추가 항목은 **array length**이며, 배열은 **specialized object layout**을 사용합니다.

### Alignment & Padding

CPU 효율을 위해 **memory alignment**이 적용됩니다. 따라서 객체 끝에는 **padding bytes**가 추가될 수 있으며, 이는 cache alignment 및 memory bus efficiency 최적화를 위한 것입니다.

### Shared Runtime Governance

객체 헤더는 공유 메모리 시스템에서 synchronization, visibility, coordination의 핵심 메타데이터 역할을 합니다. 즉, **shared object governance**의 물리적 기반입니다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

직접 객체 헤더는 보이지 않지만, 다음 도구를 통해 간접 관측됩니다.

**대표 관측 명령어:** `top`, `perf`, `vmstat`, `pmap`

| 현상 | 의미 |
|---|---|
| high RSS | 객체 증가 |
| cache miss | poor object layout |
| high CPU | synchronization |
| memory fragmentation | allocation inefficiency |

### Runtime

| 항목 | 의미 |
|---|---|
| object count | 객체 수 |
| object size | 헤더 포함 크기 |
| lock contention | 객체 lock 경쟁 |
| GC traversal | 객체 스캔 |
| allocation rate | 객체 생성량 |

**대표 도구:** heap dump, memory profiler, allocation profiler, runtime analyzer

특히 **object histogram**이 중요합니다. 객체 수가 지나치게 많으면 다음이 발생할 수 있습니다.

- GC 증가
- heap pressure
- cache inefficiency

### Kubernetes

| 증상 | 원인 |
|---|---|
| OOMKilled | excessive heap |
| GC pause spike | object explosion |
| CPU saturation | synchronization |
| latency increase | heap traversal |

**대표 관측:** `kubectl top pod`, `kubectl describe pod`

**Observability:** Prometheus, Grafana, runtime metrics, heap analysis

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*