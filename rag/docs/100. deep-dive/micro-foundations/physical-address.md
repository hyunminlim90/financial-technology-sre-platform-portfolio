# Physical Address (물리 주소, PA)
## 1. 물리 주소(Physical Address, PA)란 무엇인가

> 정독: 0회

물리 주소(Physical Address, PA)는:

> 실제 DRAM 하드웨어 메모리 셀의 **절대 위치**를 가리키는 하드웨어 수준의 메모리 주소입니다.

실제 RAM 칩 위의 **진짜 위치**

CPU가 최종적으로 메모리에 접근할 때는 반드시 물리 주소를 사용합니다.

**핵심 구조:**

```
Virtual Address (VA)
→ MMU 변환
→ Physical Address (PA)
→ 실제 DRAM 접근
```

> 소프트웨어는 **Virtual Address** 사용  
> 하드웨어는 **Physical Address** 사용

<details>
<summary>Deep Dive</summary></br>

Physical Memory Cell Location(물리적 메모리 셀 위치) [[M]](../../100-deep-dive/micro-foundations/physical-memory-cell-location.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

물리 주소는 CPU와 DRAM 사이의 실제 메모리 접근 단계에서 등장합니다.

**전체 흐름:**

```
Application
→ Virtual Address 생성
→ MMU
→ Physical Address 변환
→ Memory Bus
→ DRAM Cell 접근
```

즉 물리 주소는 CPU Cache, MMU, Memory Controller, DRAM 영역에서 핵심적으로 사용됩니다.

**대표 등장 위치:**

| 구성 요소 | 물리 주소 사용 |
|-----------|--------------|
| MMU | 주소 변환 |
| Page Table Mapping | PA 연결 |
| CPU Cache | Cache Line Tag |
| DRAM Controller | 메모리 접근 |
| DMA | 직접 메모리 접근 |
| IOMMU | 장치 메모리 보호 |

> Physical Address는 **실제 하드웨어 메모리 세계의 주소 체계**입니다.

---

## 3. 어떤 자원(CPU / Memory / Network / Disk)에 가장 영향이 큰가

물리 주소는 특히 **CPU**와 **Memory**에 매우 큰 영향을 줍니다.

| 자원 | 영향도 |
|------|--------|
| Memory | 매우 큼 |
| CPU | 매우 큼 |
| Disk | 중간 |
| Network | 낮음 |

### Memory 영향

물리 주소는 실제 DRAM 접근의 기준입니다. DRAM Row/Column, Memory Channel, NUMA Node, Memory Controller와 밀접하게 연결되어 실제 메모리 성능은 PA 배치와 직결됩니다.

### CPU 영향

CPU는 `VA → PA 변환 → Cache Lookup → DRAM Access` 과정을 수행합니다. 즉 **TLB**, **Cache**, **Memory Access Latency** 전체와 연결됩니다.

### Disk 영향

Physical Memory 부족 → Swap 사용 → Disk IO 증가로 이어집니다. 물리 메모리 부족은 디스크 병목으로 연결될 수 있습니다.

---

## 4. 왜 중요한가

컴퓨터의 실제 메모리 접근은 결국 **물리 주소 기준**으로 이루어지기 때문입니다.

> CPU가 최종적으로 읽고 쓰는 대상은 항상 **Physical Address 기반**이다.

가상 주소만으로는 실제 메모리 접근이 불가능합니다.

| 기능 | 중요성 |
|------|--------|
| 실제 DRAM 접근 | 필수 |
| Cache 동작 | 핵심 |
| DMA | 필수 |
| NUMA 최적화 | 중요 |
| Memory Isolation | 기반 |
| Hypervisor | 중요 |

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 서버 장애 중 상당수가 물리 메모리 압박과 연결됩니다.

**대표 사례:**

| 장애 | 원인 |
|------|------|
| OOM | Physical Memory 부족 |
| Swap Thrashing | RAM 부족 |
| NUMA Imbalance | 특정 노드 메모리 편중 |
| High Memory Latency | Remote Memory Access |
| Cache Miss 증가 | PA Locality 문제 |
| DMA Failure | 물리 메모리 매핑 실패 |
| Kernel Panic | Memory Corruption |

> Virtual Memory는 충분해 보여도,  
> 실제 **Physical Memory가 부족하면 시스템은 느려진다.**

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 6-1. CPU는 최종적으로 Physical Address를 사용한다

```
CPU Instruction 실행
→ VA 생성
→ MMU 변환
→ PA 획득
→ DRAM 접근
```

실제 하드웨어 메모리 접근은 항상 **PA 기반**입니다.

### 6-2. MMU가 VA → PA 변환 핵심이다

MMU(Memory Management Unit)는 Virtual Address ↔ Physical Address 매핑을 수행합니다.

이 과정에서 **TLB**, **Page Table**, **Page Walk** 등이 사용됩니다.

### 6-3. Physical Address는 실제 RAM 크기에 제한된다

| RAM 크기 | 물리 주소 범위 |
|----------|--------------|
| 8GB | 약 8GB 범위 |
| 16GB | 약 16GB 범위 |
| 64GB | 약 64GB 범위 |

> Physical Address는 **실제 장착된 메모리 용량 한계**를 가진다.

### 6-4. Cache도 Physical Address 기반으로 동작한다

```
Physical Address → Cache Line Mapping
```

Cache Hit, Cache Miss, Memory Locality 등이 모두 PA와 연결됩니다.

### 6-5. DMA와 장치 접근도 Physical Address 기반이다

```
NIC / NVMe Device
→ DMA Engine
→ Physical Memory 직접 접근
```

장치들은 일반적으로 VA를 이해하지 못하고, **PA를 기준으로** 메모리에 접근합니다.

---

## 7. Linux / Runtime / Kubernetes에서 어디서 관측되는가

### Linux Memory 상태

**대표 확인 명령:**

```bash
free -h
vmstat
numactl --hardware
```

관측 가능: 실제 RAM 사용량, NUMA 상태, Swap 사용량

**Physical Memory Mapping 확인:**

```bash
cat /proc/iomem
cat /proc/meminfo
```

확인 가능: 물리 메모리 영역, Reserved Memory, DMA 영역

**NUMA 관측:**

```bash
numastat
lscpu
```

확인 가능: NUMA Node, Remote Access, Memory Imbalance

### Kubernetes

K8s Node 메모리 압박은 결국 **Physical Memory 문제**입니다.

```bash
kubectl top node
kubectl describe node
```

관측 가능: Memory Pressure, OOM, Eviction

### Observability

현대 시스템에서는 eBPF, perf, Prometheus, NUMA metrics 등으로 물리 메모리 상태를 추적합니다.

**대표 메트릭:**

| 메트릭 | 의미 |
|--------|------|
| RSS | 실제 물리 메모리 사용 |
| Page Cache | DRAM 캐시 |
| Swap Usage | RAM 부족 |
| NUMA Hit/Miss | 메모리 지역성 |
| Memory Bandwidth | DRAM 처리량 |
| Cache Miss Rate | 메모리 병목 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*