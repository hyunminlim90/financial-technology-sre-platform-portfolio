# arrayOop (Array Ordinary Object Pointer)

> 정독: 0회

## 1. 이 기술이 무엇인가

arrayOop는:

> JVM 내부 런타임 구현 계층에서 배열 객체(Array Object)를 표현하고 제어하는 내부 객체 표현 구조

**핵심 개념: runtime-level array object representation**

배열은 단순 데이터 블록이 아니라 다음 특성을 가진 런타임 객체입니다.

- 객체 헤더 보유
- 타입 메타데이터 보유
- 길이(length) 정보 보유
- GC 관리 대상
- 힙 메모리 상주

arrayOop는 JVM 내부에서 **array memory structure**를 직접 다루는 객체 표현 체계입니다.

### 핵심 구성

| 구성 요소 | 역할 |
|---|---|
| Mark Metadata | GC / Lock 상태 |
| Type Metadata | 배열 타입 |
| Length Metadata | 배열 크기 |
| Element Region | 실제 데이터 |

즉, arrayOop는 **array object + runtime metadata + contiguous memory layout**을 통합 관리하는 런타임 메모리 구조입니다.

---

## 2. 시스템 어디에서 등장하는가

arrayOop는 런타임 내부 메모리 계층에서 등장합니다.

### 대표 위치

| 계층 | 역할 |
|---|---|
| Heap Runtime | 배열 객체 저장 |
| GC Engine | 배열 스캔 |
| Runtime Allocator | 배열 메모리 할당 |
| Type System | 배열 타입 판별 |
| Bounds Check Engine | 인덱스 검증 |
| JIT Compiler | 배열 최적화 |

특히 **runtime object layout layer**에서 핵심 역할을 수행합니다.

운영체제 관점에서는 virtual memory, page mapping, heap allocation, memory compaction과 연결됩니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Memory**입니다.

배열은 **large contiguous allocation**을 요구하기 때문입니다.

### 자원별 영향

| 자원 | 영향 |
|---|---|
| CPU | bounds check / traversal |
| Memory | heap occupancy |
| Cache | locality optimization |
| GC | scan cost |

특히 **CPU cache locality**와 강하게 연결됩니다. 연속 메모리 구조이므로 sequential scan, prefetch optimization, vectorized access 효율이 높아집니다.

---

## 4. 왜 중요한가

배열은 현대 런타임에서 가장 중요한 데이터 구조 중 하나입니다.

### 중요 이유

| 이유 | 설명 |
|---|---|
| contiguous memory | 고속 접근 |
| predictable layout | 주소 계산 단순 |
| GC optimization | 효율적 스캔 |
| JIT optimization | 루프 최적화 |
| memory efficiency | compact storage |

대부분의 런타임 최적화는 **array access optimization**과 깊게 연결됩니다.

### 대표 사례

| 시스템 | 내부 사용 |
|---|---|
| network buffer | byte array |
| database page | fixed array |
| runtime queue | array-backed |
| serialization | contiguous buffer |
| AI tensor | multidimensional array |

---

## 5. 실제 장애와 어떤 관련이 있는가

배열 객체는 메모리 장애와 매우 강하게 연결됩니다.

### 대표 문제

| 장애 | 설명 |
|---|---|
| OOM | huge array allocation |
| Fragmentation | contiguous space 부족 |
| GC Pause | large array scan |
| Allocation Failure | large block unavailable |
| Cache Miss | poor access pattern |
| Bounds Exception | invalid index |

특히 중요한 것은 **large contiguous memory requirement**입니다.

메모리는 남아 있어도 **contiguous free region unavailable**이면 allocation 실패가 발생할 수 있습니다.

또한 대형 배열은 다음 비용을 유발합니다.

- GC traversal 비용 증가
- memory copy 비용 증가
- compaction 비용 증가

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Object-Based Array

배열은 런타임 내부에서 **full-fledged object**입니다. 즉 배열도 object header, metadata, GC tracking 대상입니다.

### Length Metadata

배열 핵심 특징은 **embedded length metadata**입니다. length 정보가 메모리 내부에 존재하므로 **O(1) length access**가 가능합니다.

### Bounds Checking

배열 접근 시 `index < length` 검사가 런타임에서 수행됩니다.

- memory corruption 방지
- invalid access 차단
- runtime safety 보장

### Contiguous Elements

배열 요소는 **sequential memory cells**에 저장됩니다. 따라서 주소 계산은 다음 형태입니다.

```
base + (index × element_size)
```

### Primitive vs Reference Array

| 배열 종류 | 저장 내용 |
|---|---|
| primitive array | actual value bits |
| reference array | object addresses |

참조 배열은 실제 객체를 저장하지 않고 **references/pointers**를 저장합니다.

### GC Traversal

GC는 배열 객체를 만나면 header scan, metadata read, reference traversal, compaction을 수행합니다. 특히 참조 배열은 **reference graph traversal** 비용이 커질 수 있습니다.

### Memory Layout Alignment

배열 메모리는 alignment 규칙 영향을 받습니다.

- 8-byte alignment
- cache-line alignment
- compressed pointer alignment

따라서 실제 사용 메모리는 `logical size ≠ physical allocated size`일 수 있습니다.

### Runtime Metadata Governance

arrayOop 수준에서는 GC state, synchronization state, type metadata, allocation metadata가 통합 관리됩니다.

즉, **배열은 단순 값 나열이 아니라 runtime-governed memory object**입니다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Runtime

| 항목 | 의미 |
|---|---|
| large array allocation | 대형 배열 |
| heap occupancy | 배열 점유 |
| GC scan cost | 배열 순회 |
| allocation churn | 반복 생성 |
| fragmentation | 연속 공간 부족 |

**대표 도구:** heap dump, runtime profiler, allocation profiler, memory analyzer

**대표 현상:** large object retention, heap expansion, GC latency

### Linux

**대표 관측 명령어:** `top`, `htop`, `vmstat`, `pmap`, `smem`

| 현상 | 의미 |
|---|---|
| RSS growth | 실제 RAM 증가 |
| Page Fault | memory touch |
| Swap Usage | memory pressure |
| NUMA imbalance | allocation skew |

### Kubernetes

**대표 현상:** OOMKilled, memory spike, container eviction, GC storm

**대표 관측:** `kubectl top pod`, `kubectl describe pod`, Prometheus, Grafana

특히 **large heap object allocation**은 컨테이너 memory limit와 직접 충돌할 수 있습니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*