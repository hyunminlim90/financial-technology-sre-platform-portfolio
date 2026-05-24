# 객체 제어 메타데이터 (Object Control Metadata)

> 정독: 0회

## 1. 이 기술이 무엇인가

객체 제어 메타데이터(Object Control Metadata)는:

> 런타임이 메모리에 생성된 객체를 추적·관리·동기화·회수하기 위해 객체 시작 주소에 저장하는 제어용 메타데이터입니다.

**핵심: runtime-managed object governance metadata**

일반적인 구조는 다음과 같습니다.

```
[ control metadata ][ object payload ]
```

### 포함되는 정보

| 메타데이터 | 역할 |
|---|---|
| type metadata | 객체 타입 식별 |
| GC metadata | 생존/추적 상태 |
| synchronization metadata | lock 상태 |
| identity metadata | object identity |
| size metadata | 객체 크기 |
| runtime flags | 런타임 상태 |

즉, 객체 제어 메타데이터는 **object runtime control surface**입니다.

---

## 2. 시스템 어디에서 등장하는가

### 대표 등장 위치

| 위치 | 역할 |
|---|---|
| Managed Heap | 객체 관리 |
| Runtime Engine | 객체 제어 |
| Garbage Collector | 객체 추적 |
| Synchronization Layer | lock 관리 |
| Allocator | allocation bookkeeping |
| Type System | dynamic type resolution |

### 대표 메모리 구조

```
object base address → control metadata → instance payload
```

특히 managed runtime, tracing GC, object-oriented runtime, concurrent runtime에서 핵심입니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Memory + CPU**입니다.

| 자원 | 영향 |
|---|---|
| Memory | object footprint 증가 |
| CPU | metadata inspection |
| Cache | cache locality 변화 |
| GC | traversal overhead |
| Synchronization | contention tracking |

특히 **metadata access frequency**가 중요합니다. 모든 객체 접근 시 런타임은 메타데이터를 참조할 가능성이 높습니다.

---

## 4. 왜 중요한가

런타임은 객체를 단순 데이터로 다루지 않습니다. 다음 정보를 즉시 판단해야 합니다.

- 무엇인지
- 살아있는지
- 락이 걸렸는지
- 이동 가능한지
- GC 대상인지
- 배열인지
- 크기가 얼마인지

따라서 객체 생성 시 **control metadata attachment**가 필수입니다. 이 메타데이터가 없으면 다음이 불가능합니다.

- GC
- synchronization
- type dispatch
- runtime inspection

즉, 객체 제어 메타데이터는 **runtime object governability foundation**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 대표 장애

| 문제 | 설명 |
|---|---|
| corrupted metadata | 객체 상태 붕괴 |
| invalid type metadata | 타입 판별 실패 |
| synchronization corruption | lock state 오류 |
| GC traversal failure | 객체 추적 실패 |
| heap corruption | 힙 전체 손상 |
| invalid object relocation | GC 이동 실패 |

특히 위험한 것은 **metadata inconsistency**입니다. 대표 결과는 다음과 같습니다.

- crash
- undefined behavior
- deadlock
- memory leak
- runtime corruption

또한 메타데이터 크기 증가 시 다음이 발생할 수 있습니다.

- object density 감소
- heap pressure 증가
- cache miss 증가

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Metadata-First Object Model

객체 생성 순서는 다음과 같습니다.

```
allocation → control metadata initialization → payload initialization
```

즉, **metadata precedes payload**입니다.

### Offset 0 Anchoring

객체 제어 메타데이터는 보통 **offset 0**에 위치합니다.

```
object base address = metadata base address
```

### Runtime Type Resolution

메타데이터는 **type metadata pointer**를 포함합니다. 이를 통해 런타임은 다음을 수행합니다.

- type checking
- dynamic dispatch
- interface resolution
- polymorphism

### GC State Tracking

메타데이터는 **object lifecycle state**를 유지합니다.

대표 정보: `mark state`, `generation age`, `forwarding state`, `evacuation state`

GC는 이 정보를 사용해 **reachability management**를 수행합니다.

### Synchronization Metadata

멀티스레드 환경에서는 **lock ownership metadata**가 저장될 수 있습니다. 이를 통해 monitor ownership, contention management, thread coordination이 수행됩니다.

### Identity Metadata

객체는 런타임 수준에서 **runtime identity**를 가질 수 있습니다.

대표: `object hash`, `monitor identity`, `relocation tracking`

### Metadata Mutation

객체 메타데이터는 정적이지 않습니다. 런타임 중 **dynamic metadata mutation**이 발생합니다.

| 상황 | 변경 내용 |
|---|---|
| synchronization | lock bits 변경 |
| GC | mark bits 변경 |
| relocation | forwarding metadata 변경 |
| aging | generation age 증가 |

### Object Traversal Entry Point

GC와 런타임은 객체 순회 시 항상 **metadata inspection first**를 수행합니다.

```
metadata → payload → reference graph
```

### Metadata Compression

현대 런타임은 **compressed metadata representation**을 자주 사용합니다. 목적은 memory reduction, cache efficiency, pointer optimization입니다.

### Memory Alignment

메타데이터 포함 객체는 **alignment boundary**에 맞춰 정렬됩니다. 이는 atomic access, cache optimization, bus efficiency를 위한 것입니다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

직접 메타데이터는 보이지 않지만, 다음 도구로 간접 관측됩니다.

**대표 관측 명령어:** `perf`, `vmstat`, `pmap`, `cat /proc/<pid>/maps`

| 현상 | 의미 |
|---|---|
| heap corruption | metadata 손상 |
| abnormal crash | invalid object state |
| lock contention | synchronization metadata 경쟁 |
| GC anomaly | metadata traversal 문제 |

### Runtime

| 항목 | 의미 |
|---|---|
| heap dump | 객체 메타데이터 |
| object layout | header structure |
| GC trace | lifecycle metadata |
| synchronization trace | lock metadata |
| allocation profile | metadata overhead |

**대표 도구:** heap analyzer, runtime profiler, memory inspector, allocation tracer

### Kubernetes

| 현상 | 원인 |
|---|---|
| memory usage 증가 | metadata overhead |
| GC pause 증가 | metadata traversal |
| CPU spike | synchronization inspection |
| pod OOM | object expansion |

**관측 지표:** heap usage, allocation rate, GC latency, object count, synchronization contention

특히 **object allocation pressure**가 중요합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*