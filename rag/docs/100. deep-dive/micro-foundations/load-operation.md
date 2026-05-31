# 로드 (Load)

> 정독: 0회

## 1. 이 기술이 무엇인가

**로드(Load)** 는:

> 메모리(RAM)에 저장된 데이터를 CPU 레지스터로 읽어오는 연산

즉, **Memory → CPU Register** 방향의 데이터 이동입니다.

**예시:**

```java
y = x;
value = object.field;
flag = status;
```

이 코드는 최종적으로 **load instruction → memory read** 로 변환됩니다.

**대표 기계어:**

| ISA | 예시 |
|-----|------|
| x86 | `MOV` |
| ARM | `LDR` |
| RISC-V | `LW` / `LD` |

---

## 2. 시스템 어디에서 등장하는가

로드는 거의 모든 실행 흐름에서 등장합니다.

| 영역 | 사용 사례 |
|------|-----------|
| Stack | local variable read |
| Heap | object field read |
| CPU Cache | cache line fetch |
| Runtime | object dereference |
| Kernel | page table access |
| Network Stack | packet parsing |
| Database | buffer page read |

> CPU가 어떤 계산을 하기 전에는 대부분 load가 먼저 수행됩니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

로드는 특히 **memory latency** 영향을 크게 받습니다.

| 자원 | 영향 |
|------|------|
| CPU Cache | cache hit/miss |
| RAM | memory latency |
| Memory Bus | read traffic |
| CPU Pipeline | load stall |
| TLB | address translation |

> **cache miss 여부**가 성능에 매우 큰 영향을 줍니다.

---

## 4. 왜 중요한가

CPU는:

> 레지스터 안의 데이터만 직접 연산 가능

합니다. 즉, 메모리에 데이터가 있어도 register로 load되지 않으면 ALU 계산이 불가능합니다.

> load는 메모리 데이터를 실제 연산 가능한 상태로 가져오는 핵심 단계

**중요 이유:**

| 이유 | 설명 |
|------|------|
| 계산 시작 | ALU 입력 공급 |
| 조건 분기 | state read |
| 객체 접근 | field dereference |
| 함수 실행 | parameter read |
| 캐시 활용 | locality optimization |

---

## 5. 실제 장애와 어떤 관련이 있는가

### Cache Miss
대표적인 성능 병목입니다.

```
L1 miss → L2 miss → L3 miss → DRAM access
```

지연이 급증할 수 있습니다.

### Memory Stall
CPU가 load 완료를 기다리며 pipeline stall이 발생할 수 있습니다.

### NUMA Remote Access
다른 NUMA node의 메모리를 읽으면 latency 증가, throughput 감소가 발생할 수 있습니다.

### Pointer Chasing
linked structure / tree traversal 시 cache locality가 낮아 random load가 증가할 수 있습니다.

### TLB Miss
가상 주소 변환 실패 시 page table walk가 발생하여 성능이 급감할 수 있습니다.

### Page Fault
로드 시 page가 absent하면 disk I/O가 발생하는 major fault가 발생할 수 있습니다.

### False Sharing 영향
읽기 자체보다, 다른 코어의 write로 인해 cache invalidation이 발생하고 load 재수행이 증가할 수 있습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 핵심 흐름

```
1) 주소 계산
   └─ CPU AGU(Address Generation Unit)가
      effective address = base + offset 계산

2) Load Instruction 실행
   └─ MOV R1, [addr]  (x86)
      LDR R1, [addr]  (ARM)

3) TLB 조회
   └─ 가상 주소 → 물리 주소 변환 확인
      TLB miss 시 page table walk 수행

4) Cache Lookup
   └─ 순차 탐색: L1 → L2 → L3 → DRAM

5) Cache Hit 여부 결정
   ├─ cache hit  → 매우 빠름
   └─ cache miss → DRAM 접근 필요, latency 급증

6) Memory Read 수행
   └─ 메모리 controller가 physical memory read 수행

7) Register 적재
   └─ 읽은 데이터가 CPU register에 적재됨

8) ALU 연산 수행
   └─ compare / arithmetic / branch 가능
```

> **load는 계산의 시작점**입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Cache/Memory 분석

```bash
perf stat
perf mem
perf record
```

**대표 지표:**

```
cache-misses
LLC-load-misses
stalled-cycles
```

### NUMA 분석

```bash
numastat
numactl --hardware
```

### Page Fault 관측

```bash
vmstat
sar -B
```

**대표 지표:**

```
pgfault
majflt
```

### CPU Stall 분석

```bash
perf top
perf report
```

### Memory Access 패턴 분석

```bash
valgrind --tool=cachegrind
```

### Kubernetes 환경

메모리 접근 비효율 발생 시:

```
CPU usage 증가 → latency 증가 → throughput 감소
```

특히 아래 워크로드에서 중요합니다.

- object-heavy service
- graph traversal
- cache-unfriendly workload

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*