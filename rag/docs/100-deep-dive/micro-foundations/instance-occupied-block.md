# 인스턴스 점유 블록 (Instance Occupied Block)

> 정독: 0회

## 1. 이 기술이 무엇인가

인스턴스 점유 블록은(**single object contiguous memory region**):

> 런타임이 객체를 생성할 때 객체 헤더, 필드 데이터, 정렬 패딩까지 포함하여 하나의 객체가 실제 메모리에서 점유하는 연속된 바이트 영역 전체를 의미합니다.

### 일반적 구조

```
[ object header    ]
[ instance fields  ]
[ alignment padding]
```

핵심은 **contiguous object memory layout**입니다. 즉, 객체는 논리적 개념 이전에 **physically allocated memory block**입니다.

---

## 2. 시스템 어디에서 등장하는가

### 대표 등장 위치

| 영역 | 역할 |
|---|---|
| Heap Allocator | 객체 메모리 확보 |
| Runtime Object Model | 객체 레이아웃 |
| GC | 객체 이동/압축 |
| CPU Cache | 객체 접근 최적화 |
| Pointer Arithmetic | field offset 계산 |
| Memory Alignment | 하드웨어 정렬 |

### 핵심 연결 구조

```
base address + field offset = target field address
```

즉, 객체는 **offset-addressable memory region**으로 동작합니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Memory + CPU Cache**입니다.

| 자원 | 영향 |
|---|---|
| Memory | 객체 크기 증가 |
| CPU Cache | locality 영향 |
| GC | object traversal 비용 |
| Memory Bus | contiguous access 효율 |
| TLB | address translation 영향 |

특히 **memory locality**가 중요합니다. 객체가 연속 배치될수록 다음 효과가 발생합니다.

- cache hit 증가
- pointer chasing 감소
- fetch 효율 증가

---

## 4. 왜 중요한가

런타임은 객체를 **stable contiguous unit**으로 다뤄야 합니다. 그래야 다음이 가능합니다.

- field access
- synchronization
- GC traversal
- object copy
- memory compaction

핵심 이유는 **predictable memory layout**입니다. CPU는 `base address + offset`만으로 객체 내부 데이터에 즉시 접근합니다.

따라서 인스턴스 점유 블록은 **runtime-accessible object embodiment**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 대표 문제

| 문제 | 설명 |
|---|---|
| heap fragmentation | 연속 공간 부족 |
| cache inefficiency | locality 붕괴 |
| oversized objects | 메모리 낭비 |
| object inflation | GC 압박 |
| alignment waste | padding 증가 |
| memory corruption | 레이아웃 붕괴 |

특히 중요한 것은 **fragmented heap layout**입니다. 연속 블록 확보 실패 시 다음이 발생합니다.

- allocation latency 증가
- compaction 증가
- GC pause 증가

또한 **large object density**는 다음을 유발합니다.

- memory pressure
- cache pollution
- allocation stall

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Contiguous Allocation

객체는 보통 **single contiguous memory block**으로 생성됩니다. CPU가 **sequential memory access**를 가장 효율적으로 수행하기 때문입니다.

### Object Layout

```
header → fields → padding
```

### Base Address Access Model

객체 참조는 보통 **base address pointer**입니다. 필드 접근은 `base + offset` 방식이며, field address calculation은 매우 단순한 산술 연산입니다.

### Header Region

객체 최전방에는 **runtime governance metadata**가 존재합니다.

대표 정보: `synchronization state`, `GC metadata`, `type metadata`, `hash identity`

### Field Region

헤더 뒤에는 **instance payload data**가 배치됩니다.

포함: primitive value, pointer/reference, embedded runtime value

### Alignment & Padding

현대 CPU는 **aligned memory access**를 선호합니다. 따라서 런타임은 **padding bytes**를 삽입합니다.

목적: cache efficiency, bus alignment, atomic access optimization

### Heap Placement

인스턴스 점유 블록은 **heap-resident runtime object region**입니다.

- stack 아님
- register 아님
- code segment 아님

### Allocation Path

```
size calculation → heap reservation → header initialization → field initialization → reference return
```

### GC Interaction

GC는 객체를 **movable memory block**으로 취급합니다. 따라서 copy, compact, relocate가 가능합니다.

### Object Density

객체가 많아질수록 **metadata overhead accumulation**이 발생합니다. 작은 객체가 너무 많으면 다음이 발생합니다.

- header overhead 증가
- GC cost 증가
- cache miss 증가

### Memory Locality

런타임 성능에서 **spatial locality**는 매우 중요합니다. 연속 배치된 객체는 다음 효과가 있습니다.

- prefetch 효율 증가
- cache line 활용 증가
- memory latency 감소

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**대표 관측 명령어:** `pmap`, `smaps`, `perf`, `numastat`, `vmstat`

| 현상 | 의미 |
|---|---|
| RSS 증가 | heap object 증가 |
| cache miss 증가 | locality 저하 |
| page fault 증가 | heap pressure |
| NUMA imbalance | remote memory access |

### Runtime

| 항목 | 의미 |
|---|---|
| object size histogram | 객체 크기 분포 |
| heap dump | 객체 배치 |
| allocation profile | 생성 패턴 |
| GC compaction | 블록 이동 |
| fragmentation ratio | 힙 단편화 |

**중요 메트릭:** allocation rate, live object size, fragmentation level, object density

### Kubernetes

| 현상 | 원인 |
|---|---|
| pod memory explosion | excessive object allocation |
| GC pause 증가 | heap density 증가 |
| CPU spike | allocation churn |
| OOMKill | heap exhaustion |

특히 **high allocation throughput**이 중요합니다. 객체 생성/파괴가 지나치면 다음이 발생합니다.

- allocator contention
- GC amplification
- memory churn