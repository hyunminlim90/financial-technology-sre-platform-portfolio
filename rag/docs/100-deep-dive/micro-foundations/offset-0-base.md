# 오프셋 0 기점 (Offset 0 Base)

> 정독: 0회

## 1. 이 기술이 무엇인가

오프셋 0 기점(Offset 0 Base)은:

> 특정 메모리 블록이 시작되는 최초 주소를 의미

**핵심: base address + relative offset addressing**

메모리 시스템은 보통 **absolute address**를 매번 직접 저장하지 않습니다. 대신 **base address + offset** 방식을 사용합니다.

즉, 오프셋 0 기점은 **reference origin point**입니다.

---

## 2. 시스템 어디에서 등장하는가

오프셋 0 개념은 거의 모든 메모리 시스템에서 등장합니다.

### 대표 위치

| 계층 | 사용 목적 |
|---|---|
| Heap Object | 객체 시작 주소 |
| Array Layout | 배열 시작점 |
| Stack Frame | 함수 프레임 기준 |
| CPU Register Addressing | base+offset |
| File Mapping | mapped base |
| Virtual Memory | page base |
| DMA Buffer | device memory base |

### 대표 흐름

```
base address + relative displacement = target address
```

즉, 현대 시스템 대부분은 **offset-based memory addressing** 위에서 동작합니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Memory**입니다. 하지만 실제 핵심은 **CPU memory addressing efficiency**입니다.

| 자원 | 영향 |
|---|---|
| CPU | address calculation |
| Memory | layout structure |
| Cache | locality |
| TLB | virtual translation |
| Bus | memory access |

특히 **cache locality**와 **pointer arithmetic** 효율에 매우 중요합니다.

---

## 4. 왜 중요한가

컴퓨터는 모든 데이터를 **memory address**로 접근합니다. 하지만 모든 필드의 절대 주소를 저장하면 다음 문제가 발생합니다.

- 메모리 낭비
- relocation 문제
- 이동 불가능 객체

따라서 시스템은 **base-relative addressing**을 사용합니다.

### 장점

| 장점 | 설명 |
|---|---|
| compact metadata | 주소 저장 최소화 |
| relocation support | 메모리 이동 가능 |
| fast field lookup | 빠른 접근 |
| efficient object layout | 메모리 효율 |
| runtime portability | 주소 재배치 가능 |

즉, 오프셋 0 기점은 **runtime memory coordinate system**의 기준점입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

주소 계산 오류는 매우 치명적입니다.

### 대표 문제

| 문제 | 설명 |
|---|---|
| invalid offset | 잘못된 메모리 접근 |
| buffer overflow | 경계 초과 |
| memory corruption | 데이터 오염 |
| segmentation fault | 접근 금지 주소 |
| stale pointer | 해제된 주소 참조 |

특히 중요한 것은 **out-of-bounds access**입니다. 오프셋 계산이 잘못되면 `base + invalid displacement`가 발생하며, 이는 다음으로 이어질 수 있습니다.

- crash
- corrupted memory
- undefined behavior

또한 잘못된 메모리 alignment는 다음을 유발할 수 있습니다.

- cache miss 증가
- bus inefficiency
- false sharing

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Base + Offset Addressing

```
target address = base address + offset
```

예시: `object_base + field_offset` — CPU는 실제로 이런 방식으로 메모리에 접근합니다.

### Offset 0

오프셋 0은 **memory block starting point**, 즉 `base + 0` 위치입니다. 모든 상대 주소 계산의 기준점입니다.

### Relative Addressing

현대 메모리 시스템은 대부분 **relative displacement**를 사용합니다.

| 구조 | 방식 |
|---|---|
| Object Field | base + field offset |
| Array Element | base + index × size |
| Stack Frame | stack base + local offset |
| Page Table | page base + displacement |

### Memory Layout

객체 내부는 보통 `header → field → padding` 순으로 배치됩니다. 각 필드는 **fixed offset**을 가지므로, 런타임은 `base + known offset`만으로 즉시 접근할 수 있습니다.

### Pointer Arithmetic

저수준 시스템에서는 **pointer arithmetic**이 핵심입니다.

```
address_of_element = array_base + (index × element_size)
```

배열 접근 대부분이 이 원리입니다.

### Alignment

메모리 효율을 위해 **alignment boundary**가 적용됩니다.

- 4-byte alignment
- 8-byte alignment
- cache-line alignment

이는 CPU fetch efficiency 및 bus access optimization을 위한 것입니다.

### Cache Locality

연속 메모리 배치는 **cache-friendly access**를 가능하게 합니다. 따라서 **base-relative contiguous layout**은 성능에 매우 중요합니다.

### Virtual Address Translation

실제 접근 과정은 다음과 같습니다.

```
virtual base address → page translation → physical address
```

즉, 오프셋 계산 이후에도 **MMU translation**이 수행됩니다.

### Metadata Traversal

런타임은 객체 시작점 기준으로 header access, type metadata lookup, synchronization state lookup 등을 수행합니다. 즉, **offset 0**은 런타임 메타데이터 탐색의 시작점입니다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

직접 주소 계산은 보이지 않지만, 다음에서 간접 관측됩니다.

**대표 관측 명령어:** `pmap`, `cat /proc/<pid>/maps`, `perf`, `vmstat`

| 현상 | 의미 |
|---|---|
| cache miss | poor layout |
| alignment issue | inefficient access |
| page fault | invalid residency |
| segmentation fault | invalid offset |

### Runtime

| 항목 | 의미 |
|---|---|
| object layout | 필드 배치 |
| field offset | 상대 위치 |
| allocation alignment | 메모리 정렬 |
| pointer compression | 주소 최적화 |
| heap traversal | 메모리 순회 |

**대표 도구:** memory profiler, heap analyzer, object layout analyzer, runtime introspection tool

### Kubernetes

| 증상 | 원인 |
|---|---|
| memory inefficiency | poor layout |
| GC overhead | object density |
| CPU overhead | cache inefficiency |
| latency increase | memory traversal |

**Observability:** Prometheus, runtime metrics, eBPF, perf

특히 **cache miss metrics**가 중요합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*