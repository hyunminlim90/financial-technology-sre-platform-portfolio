# 메모리 적재 (Memory Loading)

> 정독: 0회

## 1. 이 기술이 무엇인가

메모리 적재(Memory Loading)는:

> 프로그램 코드나 데이터가 실제 실행 가능한 상태가 되도록 메모리(RAM)에 배치되는 과정

**핵심: `logical program data` → `physical memory residency` 전환**

실행 엔진은 다음 요소들을 메모리에 적재합니다.

- 코드
- 객체
- 배열
- 스택 프레임
- 버퍼
- 캐시 데이터

중요한 점은 메모리 적재는 단순 복사가 아니라 **virtual address mapping + physical page allocation + runtime metadata initialization**까지 포함하는 런타임 동작이라는 점입니다.

---

## 2. 시스템 어디에서 등장하는가

메모리 적재는 거의 모든 실행 계층에서 등장합니다.

### 대표 위치

| 계층 | 역할 |
|---|---|
| OS Loader | 실행 파일 적재 |
| Virtual Memory | 페이지 매핑 |
| Runtime Engine | 객체 적재 |
| Heap Allocator | 동적 메모리 배치 |
| Stack Manager | 함수 호출 스택 생성 |
| CPU Cache | 캐시 라인 적재 |
| DMA Engine | 장치 메모리 전송 |

### 대표 흐름

```
Disk → Page Cache → RAM → CPU Cache → Register
```

즉 메모리 적재는 **storage → memory → execution** 연결의 핵심 단계입니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Memory**입니다. 하지만 실제로는 모두와 연결됩니다.

| 자원 | 영향 |
|---|---|
| Memory | RAM occupancy |
| CPU | page translation / copy |
| Disk | page-in |
| Network | remote data loading |
| Cache | cache fill |

특히 **memory bandwidth**와 **page fault latency**가 중요합니다.

대규모 적재 시 다음 문제가 발생할 수 있습니다.

- memory bus saturation
- NUMA imbalance
- cache miss
- TLB pressure

---

## 4. 왜 중요한가

프로그램은 메모리에 적재되지 않으면 실행될 수 없습니다. CPU는 **memory-resident data**만 연산할 수 있기 때문입니다.

### 중요 이유

| 이유 | 설명 |
|---|---|
| executable residency | 실행 가능 상태 |
| runtime allocation | 객체 생성 |
| cache efficiency | 성능 |
| memory locality | 접근 최적화 |
| virtual memory operation | 주소 공간 관리 |

실제 성능 문제 상당수는 **memory loading behavior**와 연결됩니다.

### 대표 사례

| 현상 | 원인 |
|---|---|
| startup delay | excessive loading |
| high latency | page fault |
| OOM | excessive residency |
| slow GC | heap expansion |
| cache miss storm | poor locality |

---

## 5. 실제 장애와 어떤 관련이 있는가

메모리 적재는 운영 장애와 매우 직접적으로 연결됩니다.

### 대표 장애

| 장애 | 설명 |
|---|---|
| Out Of Memory | 메모리 부족 |
| Major Page Fault | 디스크 기반 로딩 |
| Swap Thrashing | excessive paging |
| Slow Startup | large loading |
| Fragmentation | allocation failure |
| NUMA Remote Access | cross-node latency |

특히 중요한 것은 **page fault amplification**입니다. 메모리에 없는 페이지 접근 시 `disk → RAM` 적재가 발생하며, 이 과정은 매우 느립니다.

또한 대형 객체 적재는 다음을 유발할 수 있습니다.

- heap pressure
- GC 증가
- cache eviction
- memory bandwidth saturation

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Virtual Address Mapping

현대 시스템에서 프로그램은 **virtual address**를 사용합니다. 실제 물리 메모리는 **MMU + page table**을 통해 매핑됩니다.

> 프로그램이 보는 주소와 실제 RAM 주소는 다릅니다.

### Page-Based Loading

메모리 적재는 보통 **page unit** 기준으로 수행됩니다.

- 4KB
- 2MB huge page
- 1GB huge page

### Demand Paging

모든 데이터를 즉시 적재하지 않습니다. 필요 시 `page fault trigger → page load`가 발생합니다.

즉, 메모리 적재는 **lazy loading** 형태로 동작할 수 있습니다.

### Heap Residency

런타임 객체는 **heap-resident objects** 형태로 적재됩니다.

- object
- array
- runtime metadata
- dynamic buffer

### Cache Hierarchy Loading

메모리 적재는 RAM에서 끝나지 않습니다. 실행 직전 다음 경로로 이동합니다.

```
RAM → L3 Cache → L2 Cache → L1 Cache → Register
```

실제 CPU 성능은 **cache residency quality** 영향을 매우 크게 받습니다.

### Memory Alignment

메모리 적재 시 alignment, cache line boundary, page boundary 영향을 받습니다. 잘못된 alignment는 **cache inefficiency**를 유발합니다.

### Runtime Metadata Initialization

객체 적재 시 단순 데이터만 기록되지 않습니다. 함께 적재되는 것은 다음과 같습니다.

- type metadata
- synchronization metadata
- GC metadata
- allocation metadata

즉 런타임은 **data + governance metadata**를 동시에 메모리에 적재합니다.

### Residency vs Allocation

| 개념 | 의미 |
|---|---|
| allocation | 주소 공간 확보 |
| loading/residency | 실제 RAM 상주 |

가상 메모리 시스템에서는 `allocated ≠ physically resident`일 수 있습니다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**대표 관측 명령어:** `top`, `htop`, `free -h`, `vmstat`, `sar`, `pmap`, `smem`, `numastat`

| 지표 | 의미 |
|---|---|
| RSS | 실제 RAM 상주 |
| VSZ | 가상 주소 공간 |
| Page Fault | 메모리 적재 발생 |
| Swap In/Out | 디스크 paging |
| Cache Hit Ratio | 캐시 효율 |

**대표 파일:**

```
/proc/meminfo
/proc/<pid>/maps
/proc/<pid>/smaps
```

### Runtime

| 항목 | 의미 |
|---|---|
| heap usage | 객체 적재량 |
| allocation rate | 생성 속도 |
| GC pressure | 메모리 압박 |
| object residency | 생존 객체 |
| page residency | 실제 상주 |

**대표 도구:** runtime profiler, heap analyzer, allocation tracker, memory profiler

### Kubernetes

**대표 현상:** OOMKilled, memory throttling, eviction, container restart

**대표 관측:** `kubectl top pod`, `kubectl describe pod`

**Observability:** Prometheus, Grafana, cAdvisor, node-exporter

특히 **container memory limit** 초과 시 다음이 발생할 수 있습니다.

- OOM Kill
- page reclaim storm
- swap pressure

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*