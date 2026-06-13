# 배열 인스턴스 (Array Instance)

> 정독: 0회

## 1. 이 기술이 무엇인가

배열 인스턴스(Array Instance)는:

> 실행 시간(Runtime)에 메모리상에 실제로 생성되는 **배열 객체**

**핵심 특징:**

- 동일한 타입 요소 저장
- 연속된 메모리 구조
- 런타임 동적 할당
- 인덱스 기반 접근
- 힙(Heap) 메모리 상주
- 런타임 관리 대상

배열은 단순 데이터 묶음이 아니라 **runtime-managed object**입니다.

**배열 인스턴스 내부 구성:**

```
[ Object Header  ]  ← 런타임 메타데이터
[ Length Field   ]  ← 배열 크기
[ Elements Area  ]  ← 실제 데이터 저장
```

> 배열 인스턴스는 **metadata + length + contiguous element memory**로 구성된 런타임 객체입니다.

<details>
<summary>Deep Dive</summary></br>

Array Ordinary Object Pointer(arrayOop) [[M]](../../100-deep-dive/micro-foundations/array-ordinary-object-pointer.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

배열 인스턴스는 거의 모든 런타임 시스템에서 등장합니다.

| 영역 | 사용 목적 |
|------|----------|
| Collection Backend | 내부 저장소 |
| Network Buffer | packet storage |
| File Buffer | I/O chunk |
| Serialization | binary layout |
| Runtime Stack Data | temporary structures |
| Cache System | indexed lookup |
| Numeric Processing | vectorized data |

특히 다음은 내부적으로 배열 기반 구조를 자주 사용합니다:

- packet buffer
- memory page table
- event queue
- thread scheduling table

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **Memory**입니다. 배열은 **contiguous memory allocation**이 필요하기 때문입니다.

| 자원 | 영향 |
|------|------|
| CPU | index calculation |
| Cache | cache locality |
| Memory | heap occupancy |
| GC | large object scan |

특히 **CPU Cache 효율**과 강하게 연결됩니다. 연속 메모리 구조이므로 **sequential access optimization**이 가능합니다.

---

## 4. 왜 중요한가

배열은 **가장 기본적인 런타임 데이터 구조** 중 하나입니다.

| 이유 | 설명 |
|------|------|
| O(1) index access | 즉시 접근 |
| contiguous layout | cache 효율 |
| low overhead | 구조 단순 |
| predictable memory | 메모리 계산 용이 |
| runtime efficiency | 고속 처리 |

| 시스템 | 내부 구조 |
|--------|----------|
| network buffer | byte array |
| runtime stack | contiguous memory |
| database page | fixed-size array |
| image processing | pixel array |
| AI tensor | multidimensional array |

---

## 5. 실제 장애와 어떤 관련이 있는가

배열 인스턴스는 **메모리 장애와 직접 연결**됩니다.

| 장애 | 설명 |
|------|------|
| Large Allocation Failure | 큰 연속 공간 부족 |
| OutOfMemory | oversized array |
| Fragmentation | contiguous block unavailable |
| GC Pause | huge array scan |
| Cache Miss Storm | poor access pattern |
| Index Overflow | boundary error |

특히 **large contiguous allocation** 문제가 중요합니다. 배열은 연속된 메모리 블록이 필요하므로:

```
total free memory exists  ≠  allocation success
```

메모리는 남아 있어도 **contiguous free block unavailable** 상태로 allocation failure가 발생할 수 있습니다.

또한 대형 배열은 다음을 유발합니다:

- GC scanning cost 증가
- memory copy 비용 증가
- cache pressure 증가

---

## 6. 핵심 메커니즘

### (1) Runtime Allocation

배열은 런타임 중 동적 생성됩니다:

```
size determination
→ heap allocation
→ metadata initialization
→ element initialization
```

### (2) Contiguous Memory Layout

배열 핵심 특징: **elements stored sequentially**

이 구조 덕분에 `base_address + offset` 계산만으로 즉시 접근 가능합니다.

### (3) Length Metadata

배열은 길이 정보를 내부적으로 유지합니다:

- bounds checking
- memory safety
- runtime validation

### (4) Index Access

배열 접근 계산 구조:

```
address = base + (index × element_size)
```

- 매우 빠름
- predictable access 가능
- CPU prefetch 효율 높음

### (5) Primitive vs Reference Array

| 배열 타입 | 저장 내용 |
|----------|----------|
| primitive array | actual bit value |
| reference array | pointer/reference |

> 참조 배열은 실제 객체 자체가 아니라 **memory addresses**를 저장합니다.

### (6) Memory Alignment

배열은 alignment 규칙 영향을 받습니다:

- 4-byte alignment
- 8-byte alignment
- cache-line alignment

```
logical size  ≠  physical allocated size
```

### (7) GC Interaction

배열도 일반 객체처럼 GC 관리 대상입니다:

- reference traversal
- object reachability
- array scan

특히 **reference array**는 GC root traversal cost에 영향이 큽니다.

### (8) Multidimensional Array

다차원 배열은 실제로는 **array of array references** 구조인 경우가 많습니다.

> 완전한 물리 연속 메모리가 **아닐 수 있습니다**.

---

## 7. Linux / Runtime / K8s에서 관측 방법

### Runtime

| 항목 | 의미 |
|------|------|
| large object allocation | 대형 배열 |
| allocation rate | 배열 생성 속도 |
| heap occupancy | 메모리 사용 |
| GC scan cost | 배열 순회 비용 |

**대표 도구:** heap dump, runtime profiler, allocation profiler, memory analyzer

**대표 증상:** large array retention, GC pressure, heap expansion, allocation failure

### Linux

```bash
top
htop
pmap
vmstat
smem
```

| 현상 | 의미 |
|------|------|
| RSS increase | 실제 RAM 증가 |
| page fault | large memory touch |
| swap usage | heap overflow |
| NUMA imbalance | large allocation skew |

### Kubernetes

**대표 현상:** OOMKilled, container eviction, memory spike, GC latency

```bash
kubectl top pod
kubectl describe pod
```

**관측 도구:** Prometheus, Grafana

> 대형 배열 생성 시 **sudden heap growth**가 자주 관측됩니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*