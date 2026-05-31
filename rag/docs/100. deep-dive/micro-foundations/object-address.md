# 객체 주소 (Object Address)

> 정독: 0회

## 1. 이 기술이 무엇인가

**객체 주소(Object Address)** 는:

> 메모리 상에서 객체가 시작되는 위치를 가리키는 주소 값

일반적으로 **Reference**, **Pointer**, **Base Address** 라고도 부릅니다.

**예시:**

```
obj -> 0x7ffe12ab3400
```

여기서 `0x7ffe12ab3400` 가 객체 주소입니다.

**핵심:**
> 객체 주소는 객체 레이아웃의 시작 위치

---

## 2. 시스템 어디에서 등장하는가

객체 주소는 거의 모든 런타임 시스템에서 등장합니다.

| 영역 | 사용 사례 |
|------|-----------|
| Heap Object | object reference |
| Stack Frame | local references |
| Function Call | pointer parameter |
| Runtime GC | object graph traversal |
| CPU | memory addressing |
| Kernel | virtual memory mapping |
| Container Runtime | process memory space |

> 객체 접근(Object Access)의 출발점입니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Memory** 입니다.

| 자원 | 영향 |
|------|------|
| Virtual Memory | address space |
| MMU | address translation |
| TLB | translation cache |
| CPU Cache | memory locality |
| RAM | physical storage |

특히 **address translation**, **pointer dereference**, **cache locality** 와 매우 밀접합니다.

---

## 4. 왜 중요한가

CPU는:

> 주소를 기반으로만 메모리 접근 가능

합니다. 즉, 객체 존재 자체보다 **어디에 있는지**, **어떻게 접근할지** 가 더 중요합니다.

객체 주소가 있어야 field read, field write, method dispatch, object traversal이 가능합니다.

> 객체 주소는 메모리 접근의 기준점

---

## 5. 실제 장애와 어떤 관련이 있는가

### Invalid Pointer Access
잘못된 주소 접근 시 segmentation fault, access violation이 발생할 수 있습니다.

### Dangling Pointer
이미 해제된 메모리 주소 접근 시 use-after-free, memory corruption이 발생할 수 있습니다.

### Null Dereference
대표 장애인 null pointer dereference가 발생할 수 있습니다.

### Address Space Fragmentation
메모리 단편화가 심하면 locality 저하, cache efficiency 감소가 발생할 수 있습니다.

### TLB Pressure
주소 변환이 과도하면 TLB miss 증가, page walk 증가가 발생할 수 있습니다.

### Cache Miss 증가
랜덤 객체 주소 접근이 많으면 spatial locality 감소, memory latency 증가가 발생할 수 있습니다.

### GC Relocation
moving GC 환경에서는 객체 주소가 변경되어 pointer update가 필요할 수 있습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 핵심 흐름

```
1) 객체 생성
   └─ heap allocation 수행

2) 시작 주소 생성
   └─ allocator가 base address 반환
      예: 0x1000

3) 참조 변수 저장
   └─ 프로그램 변수는 객체 자체가 아니라 주소를 저장
      예: obj = 0x1000

4) 객체 접근 시 주소 사용
   └─ field access 수행 시
      effective address = base address + field offset 계산

5) MMU 주소 변환
   └─ virtual address → physical address 변환

6) Cache/TLB 조회
   └─ CPU가 TLB lookup → cache lookup 수행

7) Memory Access 수행
   └─ 최종적으로 load / store 수행

8) 객체 이동 가능성
   └─ GC/allocator 환경에서 object relocation 가능
      → runtime이 reference 업데이트 수행
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Process Address Space 확인

```bash
cat /proc/<pid>/maps
```

### Virtual Memory Layout 확인

```bash
pmap <pid>
```

### Heap 영역 확인

```bash
cat /proc/<pid>/smaps
```

### Pointer/Address 분석

```bash
gdb
lldb
```

### Memory Translation 분석

```bash
perf mem
perf stat
```

### NUMA 주소 분석

```bash
numastat
```

### Kubernetes 환경

container 내부 process도 독립 virtual address space, isolated heap memory를 사용합니다.

OOM/debugging 시 아래 항목 분석이 중요합니다.

- heap dump
- pointer graph
- memory map

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*