# 연속 메모리 블록 (Contiguous Memory Block)

> 정독: 0회

## 1. 이 기술이 무엇인가

연속 메모리 블록은:

> 시작 주소(Base Address)부터 일정 크기의 메모리 영역이 중간에 끊기지 않고 연속된 주소 공간으로 배치된 메모리 영역

예시:

| 주소 | 데이터 |
|------|--------|
| 0x1000 | data |
| 0x1001 | data |
| 0x1002 | data |
| 0x1003 | data |

**핵심:**

> 연속적인 주소 증가가 보장되는 메모리 영역

Base Address(베이스 주소) [[M]](../../100-deep-dive/micro-foundations/base-address.md)  

## 2. 시스템 어디에서 등장하는가

연속 메모리 블록은 거의 모든 시스템 계층에서 등장합니다.

| 영역 | 사용 사례 |
|------|-----------|
| 배열(Array) | 연속 원소 저장 |
| 버퍼(Buffer) | 네트워크/파일 I/O |
| 힙 할당 | 객체/데이터 저장 |
| 스택 프레임 | 함수 호출 데이터 |
| 페이지(Page) | 가상 메모리 |
| DMA | 장치 메모리 접근 |
| 캐시 라인 | CPU 캐시 처리 |

> CPU가 가장 효율적으로 처리 가능한 메모리 형태

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**가장 영향이 큰 자원: CPU + Memory**

| 자원 | 영향 |
|------|------|
| CPU Cache | locality 향상 |
| Memory Bus | sequential access |
| TLB | page locality |
| Prefetcher | 예측 가능성 |
| SIMD/Vector | 연속 데이터 처리 |

> memory locality directly determines CPU efficiency

---

## 4. 왜 중요한가

연속 메모리는 성능 최적화의 핵심입니다.

| 이유 | 설명 |
|------|------|
| 캐시 효율 | cache hit 증가 |
| 순차 접근 | memory latency 감소 |
| prefetch 가능 | 다음 데이터 예측 |
| 단순 주소 계산 | offset arithmetic 가능 |
| SIMD 최적화 | vector processing 가능 |

CPU는 다음 데이터가 바로 옆 주소에 있을 것이라 가정하고 동작합니다.

연속 메모리가 깨지면: cache miss 증가 / pointer chasing 증가 / random memory access 증가 / memory latency 증가가 발생합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

| 문제 | 원인 |
|------|------|
| 메모리 단편화(Fragmentation) | 연속 영역 부족 |
| allocation failure | 큰 contiguous block 확보 실패 |
| cache miss 폭증 | locality 붕괴 |
| NUMA 성능 저하 | 원격 메모리 접근 |
| GC compaction 증가 | 연속성 복구 필요 |

**대표 현상:** high memory latency / cache thrashing / allocation stall / fragmentation pressure

특히 대용량 시스템에서는 메모리가 충분해도 contiguous block 확보 실패로 allocation이 실패할 수 있습니다.

예: huge page allocation 실패 / DMA buffer allocation 실패 / large object allocation stall

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Base Address + Offset

연속 메모리의 핵심 접근 방식:

```
실제 주소 = 시작 주소 + 오프셋
```

| index | 계산 |
|-------|------|
| 0 | base + 0 |
| 1 | base + size |
| 2 | base + size×2 |

> pointer arithmetic이 가능하려면 연속성이 필요

### Cache Locality

CPU 캐시는 cache line 단위로 메모리를 로딩합니다.

```
cache line = 64 bytes
연속 데이터 존재
→ 한 번 로딩 시 여러 데이터 동시 확보
```

> contiguous memory dramatically improves cache hit ratio

### Sequential Access

CPU prefetcher는 순차 접근 패턴을 자동 감지합니다.

```
0x1000
0x1004
0x1008
0x100C
```

처럼 증가하면 다음 주소를 미리 가져옵니다. 연속성이 깨지면 prefetch 실패 / memory stall 증가가 발생합니다.

### Fragmentation

연속 메모리의 최대 문제입니다.

```
[free][used][free][used][free]
```

총 free memory는 충분해도 큰 contiguous block 확보가 불가능합니다.

| 종류 | 의미 |
|------|------|
| external fragmentation | 외부 단편화 |
| internal fragmentation | 내부 낭비 |

메모리 관리자는 compaction / relocation / page migration 등으로 연속성 회복을 시도합니다.

### Virtual Memory

| 관점 | 상태 |
|------|------|
| Virtual Address | contiguous |
| Physical Memory | fragmented 가능 |

가상 주소 공간에서는 논리적으로 contiguous일 수 있지만, 실제 물리 RAM은 비연속 physical pages일 수 있습니다. MMU와 Page Table이 이를 추상화합니다.

### Large Object Allocation

큰 객체는 contiguous block을 요구합니다.

예: 대형 배열 / huge page / DMA memory / GPU buffer

> large contiguous allocation becomes difficult under fragmentation

### Pointer Chasing

연속성이 깨진 대표적인 문제입니다.

- **연속 메모리:** `base + offset`만으로 접근 가능
- **비연속 구조:** `pointer → pointer → pointer` 추적 필요

결과: cache miss 증가 / latency 증가 / branch misprediction 증가

### Alignment

| 정렬 | 효과 |
|------|------|
| 8-byte | 64bit aligned |
| 64-byte | cache line aligned |
| 4KB | page aligned |

CPU는 aligned contiguous memory access를 매우 선호합니다.

### O(1) Indexed Access

배열이 빠른 이유는 contiguous layout이 direct offset calculation을 보장하기 때문입니다.

```
address = base + (index × element_size)
```

만으로 즉시 접근 가능합니다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**대표 관측 도구:** `cat /proc/<pid>/maps` / `pmap` / `numastat` / `vmstat` / `slabtop` / `perf`

```bash
# 메모리 상태 확인
cat /proc/meminfo
```

| 항목 | 의미 |
|------|------|
| HugePages_Free | contiguous huge pages |
| Fragmentation | 메모리 단편화 |
| Buddy Allocator | contiguous allocation 상태 |

Buddy Allocator가 연속 메모리 확보의 핵심 allocator입니다.

### Runtime

| 영역 | 연속성 중요 |
|------|-------------|
| heap allocation | object locality |
| arrays | indexed access |
| buffers | sequential IO |
| GC compaction | contiguous recovery |

GC compacting은 fragmented heap을 다시 contiguous하게 재배치합니다.

### Kubernetes

| 사례 | 영향 |
|------|------|
| HugePages | contiguous physical memory 필요 |
| DPDK | DMA contiguous memory |
| GPU | large buffer allocation |
| High-performance networking | locality 중요 |

> node memory fragmentation can break hugepage allocation

### Observability

| 항목 | 의미 |
|------|------|
| cache miss rate | locality 상태 |
| page fault | memory mapping |
| fragmentation | allocation 상태 |
| NUMA locality | remote memory 여부 |
| allocation latency | contiguous 확보 비용 |