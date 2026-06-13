# 메모리 시작 주소 (Memory Base Address)

> 정독: 0회

## 1. 이 기술이 무엇인가

메모리 시작 주소(Memory Base Address)는:

> 객체, 배열, 버퍼, 스택 프레임, 메모리 세그먼트 같은 메모리 블록이 시작되는 첫 번째 주소

**핵심:**

> all internal memory access is calculated relative to the base address

CPU는 다음 형태로 접근합니다:

```
actual address = base address + offset
```

Instance(인스턴스) [[M]](../../100-deep-dive/micro-foundations/instance.md)  
Contiguous Memory Block(연속 메모리 블록) [[M]](../../100-deep-dive/micro-foundations/contiguous-memory-block.md)  
Data Buffer(데이터 버퍼) [[M]](../../100-deep-dive/micro-foundations/data-buffer.md)  
Stack Frame(스택 프레임) [[M]](../../100-deep-dive/micro-foundations/stack-frame.md)  
Memory Segment(메모리 세그먼트) [[M]](../../100-deep-dive/micro-foundations/memory-segment.md)  
Memory Allocation Unit(메모리 할당 단위) [[M]](../../100-deep-dive/micro-foundations/memory-allocation-unit.md)  

## 2. 시스템 어디에서 등장하는가

메모리 시작 주소는 시스템 전체에서 등장합니다.

| 영역 | 사용 |
|------|------|
| heap allocation | 객체 시작점 |
| stack frame | 함수 호출 프레임 |
| executable loading | segment placement |
| virtual memory | page mapping |
| shared memory | mapped region |
| DMA buffers | hardware memory access |

프로세스 메모리의 text segment / data segment / heap / stack / memory-mapped region 모두 base address를 가집니다.

> object references ultimately resolve to memory base addresses

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**가장 영향 큰 자원: Memory + CPU**

| 자원 | 영향 |
|------|------|
| Memory | allocation layout |
| CPU | address calculation |
| Cache | locality/access efficiency |
| TLB | virtual address translation |

> base address alignment directly affects cache and memory efficiency

---

## 4. 왜 중요한가

메모리 시작 주소는:

> the reference origin for all structured memory access

| 이유 | 설명 |
|------|------|
| address calculation | offset computation |
| object access | field lookup |
| pointer dereference | memory traversal |
| cache efficiency | aligned access |
| memory safety | valid boundary tracking |

> without a base address the runtime cannot interpret memory structure boundaries

---

## 5. 실제 장애와 어떤 관련이 있는가

메모리 오류 대부분이 base address와 연결됩니다.

**대표 장애:**

| 장애 | 원인 |
|------|------|
| segmentation fault | invalid base address |
| null dereference | zero/null base |
| memory corruption | invalid offset |
| buffer overflow | boundary violation |
| dangling pointer | freed base address reuse |

**대표 현상:** invalid memory access / heap corruption / access violation / bus error

**실무 상황별 결과:**

| 상황 | 결과 |
|------|------|
| stale pointer | corrupted object access |
| GC relocation mismatch | invalid references |
| use-after-free | undefined behavior |
| unaligned base address | performance degradation |

> incorrect base address management can destabilize entire runtime processes

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Contiguous Memory Region

메모리 시작 주소는 contiguous memory allocation의 origin point입니다.

| 주소 | 의미 |
|------|------|
| 0x1000 | base address |
| 0x1004 | field A |
| 0x1008 | field B |

> memory structure is interpreted through offset arithmetic

### Base + Offset Addressing

CPU의 핵심 메커니즘:

```
effective address = base + offset
```

예시:
```
base address = 0x1000
field offset = 16
actual field address = 0x1010
```

객체 / 배열 / 구조체 / 페이지 모두 동일 원리입니다.

### Object Layout

| 영역 | 위치 |
|------|------|
| metadata/header | base + 0 |
| field 1 | base + offset |
| field 2 | base + offset |
| alignment padding | adjusted offsets |

> object layout is a deterministic offset map relative to base address

### Virtual Addressing

현대 OS에서 base address는 대부분 virtual memory address입니다.

```
virtual address
  → page table lookup
  → physical memory address
```

실제 RAM 주소가 아닌 프로세스 가상 주소이며, MMU 변환이 필요합니다.

### Pointer Semantics

> a pointer fundamentally represents a memory base address

런타임은 pointer dereference → offset calculation → memory traversal을 수행합니다.

### Alignment

| 정렬 | 효과 |
|------|------|
| 8-byte aligned | 64-bit efficient |
| cache-line aligned | cache optimal |
| page aligned | VM efficient |

정렬 실패 시: cache miss 증가 / memory cycle 증가 / bus penalty 발생 가능

### Heap Allocation

allocator의 흐름:

```
find free memory region
  → assign base address
  → return pointer/reference
```

base address는 allocation의 결과물입니다.

### GC Relocation

GC가 object moving 수행 시:

> base addresses may change during compaction

따라서 runtime은 reference update / pointer fixup / relocation tracking을 수행합니다.

### Memory Safety Boundary

> memory safety depends on valid base address boundaries

잘못된 offset 계산 시: overflow / corruption / illegal access 발생.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**대표 관측 도구:** `pmap` / `cat /proc/<pid>/maps` / `gdb` / `perf` / `valgrind`

```bash
# 프로세스 memory region base addresses 확인
cat /proc/1234/maps

# virtual memory mapping 확인
pmap <pid>
```

`gdb`로 pointer/base address 직접 확인 가능.

### Runtime

| 구성 | base address 사용 |
|------|-------------------|
| heap objects | object origin |
| stack frames | frame origin |
| code cache | executable code |
| metadata regions | runtime structures |

> every runtime object access begins from a base address resolution

### Kubernetes

K8s 자체보다 container runtime / process memory / allocator / runtime VM에서 중요합니다.

| 관측 영역 | 의미 |
|-----------|------|
| OOM events | heap growth |
| memory fragmentation | allocator pressure |
| container crashes | invalid access |
| GC pauses | relocation activity |

> segmentation faults often originate from invalid base address dereference

### Observability

| 항목 | 의미 |
|------|------|
| heap layout | allocation structure |
| pointer values | address tracking |
| memory maps | region bases |
| page alignment | VM efficiency |
| cache locality | access optimization |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*