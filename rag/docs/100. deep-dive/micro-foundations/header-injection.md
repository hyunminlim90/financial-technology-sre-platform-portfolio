# 헤더 주입 (Header Injection)

> 정독: 0회

## 1. 이 기술이 무엇인가

헤더 주입(Header Injection)은:

> 런타임이 새로운 메모리 객체를 생성할 때, 실제 데이터(payload)보다 먼저 객체 제어용 메타데이터(header)를 메모리 시작 지점에 기록하는 과정

**핵심: metadata-first object materialization**

일반적으로 객체 메모리는 다음 형태로 배치됩니다.

```
[ header ][ payload ]
```

헤더에는 보통 다음 메타데이터가 포함됩니다.

- 타입 정보
- 크기 정보
- 참조 정보
- 동기화 상태
- GC 상태
- 참조 카운트
- 런타임 플래그

즉, 헤더 주입은 **runtime governance metadata placement** 과정입니다.

---

## 2. 시스템 어디에서 등장하는가

헤더 주입은 거의 모든 런타임 시스템에서 등장합니다.

### 대표 위치

| 계층 | 등장 위치 |
|---|---|
| Managed Runtime | object allocation |
| Heap Allocator | memory block initialization |
| Garbage Collector | metadata tracking |
| VM Runtime | type identification |
| Synchronization System | lock state management |
| Memory Allocator | allocation bookkeeping |

### 대표 구조

```
allocated memory block → header placement → payload initialization
```

특히 managed heap, object-oriented runtime, tracing GC runtime에서 매우 중요합니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Memory**입니다.

| 자원 | 영향 |
|---|---|
| CPU | metadata access |
| Memory | object layout |
| Cache | object locality |
| GC | object traversal |
| Synchronization | lock coordination |

특히 **memory layout efficiency**가 중요합니다. 헤더 크기 증가 시 다음이 발생할 수 있습니다.

- object density 감소
- cache efficiency 감소
- GC scan overhead 증가

---

## 4. 왜 중요한가

런타임은 단순 데이터만으로 객체를 제어할 수 없습니다. 다음 정보를 즉시 알아야 합니다.

- 이 객체가 무엇인지
- 누가 사용 중인지
- GC 대상인지
- 배열인지
- 크기가 얼마인지
- 동기화 상태인지

따라서 객체 생성 시 **runtime metadata attachment**가 필수입니다. 헤더가 없으면 다음이 불가능합니다.

- 타입 식별
- GC 추적
- synchronization
- dynamic dispatch

즉, 헤더 주입은 **object controllability establishment**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

헤더 손상은 매우 치명적입니다.

### 대표 문제

| 문제 | 설명 |
|---|---|
| corrupted metadata | 객체 상태 손상 |
| invalid type pointer | 타입 식별 실패 |
| synchronization corruption | 락 상태 붕괴 |
| GC traversal failure | 메모리 추적 실패 |
| heap corruption | 힙 전체 손상 |

특히 위험한 것은 **invalid header state**입니다. 대표 결과는 다음과 같습니다.

- crash
- undefined behavior
- deadlock
- GC failure
- memory leak

또한 헤더 크기 과다 시 다음이 발생합니다.

- memory amplification
- cache inefficiency
- allocation slowdown

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Allocation Before Initialization

객체 생성 순서는 다음과 같습니다.

```
memory allocation → header injection → payload initialization
```

헤더는 항상 실제 데이터보다 먼저 배치됩니다.

### Base Address Anchoring

헤더는 보통 **offset 0**에 위치합니다.

```
object base address = header starting point
```

### Metadata Embedding

| 메타데이터 | 목적 |
|---|---|
| type id | 타입 식별 |
| GC metadata | 추적/세대 관리 |
| synchronization bits | 락 상태 |
| size metadata | 객체 크기 |
| array length | 배열 길이 |
| runtime flags | 실행 상태 |

### Runtime Object Identification

런타임은 객체 접근 시 먼저 **header inspection**을 수행합니다.

대표 작업: `type check`, `dispatch`, `synchronization`, `GC mark`, `reference traversal`

### Object Traversal

GC는 객체 순회 시 `header → payload → reference fields` 순서로 탐색합니다. 즉, 헤더는 **heap graph traversal anchor** 역할을 합니다.

### Lock Metadata

멀티스레드 환경에서는 **lock ownership state**가 헤더 내부에 저장될 수 있습니다. 이를 통해 lightweight synchronization, monitor inflation, contention tracking 등이 수행됩니다.

### Type Metadata Pointer

헤더는 보통 **type metadata reference**를 포함합니다. 런타임은 이를 통해 method dispatch, polymorphism, RTTI, interface resolution 등을 처리합니다.

### Memory Alignment

헤더 포함 후 전체 객체는 **alignment boundary**에 맞춰 정렬됩니다. 이는 cache efficiency, bus alignment, atomic access를 위한 것입니다.

### Allocation Fast Path

현대 런타임은 **fast-path allocation** 최적화를 사용합니다.

```
allocate + inject minimal header + return reference
```

이 과정을 매우 빠르게 수행합니다.

### Array Specialization

배열 객체는 추가 메타데이터로 **array length metadata**를 가집니다. 배열 접근 시 **bounds checking**에 사용됩니다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

직접 헤더는 보이지 않지만, 다음에서 간접 관측됩니다.

**대표 관측 명령어:** `perf`, `pmap`, `vmstat`, `cat /proc/<pid>/maps`

| 현상 | 의미 |
|---|---|
| heap corruption | 메타데이터 손상 |
| segmentation fault | invalid header access |
| abnormal GC | 객체 탐색 실패 |
| lock contention | synchronization overhead |

### Runtime

| 항목 | 의미 |
|---|---|
| object layout | 헤더 구조 |
| heap dump | 객체 메타데이터 |
| GC trace | 객체 추적 |
| allocation trace | 생성 흐름 |
| synchronization state | 락 상태 |

**대표 도구:** heap analyzer, runtime profiler, memory introspection tool, allocation tracer

### Kubernetes

| 현상 | 원인 |
|---|---|
| memory overhead 증가 | object header inflation |
| GC latency 증가 | metadata traversal |
| CPU overhead 증가 | synchronization metadata |
| pod OOM | object density 감소 |

**Observability:** Prometheus, runtime metrics, heap metrics, GC pause metrics

특히 **allocation rate**와 **heap occupancy**가 중요합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*