# 베이스 주소 (Base Address)

> 정독: 0회

## 1. 이 기술이 무엇인가

베이스 주소는:

> 배열, 객체, 버퍼, 세그먼트, 페이지 같은 데이터 구조가 메모리에 배치될 때, 그 메모리 영역의 **첫 번째 주소**가 베이스 주소

특정 메모리 블록이 시작되는 기준 주소

**핵심:**

> 모든 메모리 접근의 기준점

예시:

| 주소 | 데이터 |
|------|--------|
| 0x1000 | 시작 |
| 0x1004 | 다음 |
| 0x1008 | 다음 |

여기서 `0x1000 = base address`입니다.

---

## 2. 시스템 어디에서 등장하는가

베이스 주소는 거의 모든 메모리 시스템에서 등장합니다.

| 영역 | 의미 |
|------|------|
| 배열(Array) | 첫 원소 주소 |
| 객체(Object) | 객체 시작 위치 |
| 스택(Stack Frame) | 프레임 시작 위치 |
| 힙(Heap) | allocation 시작점 |
| 페이지(Page) | page 시작 주소 |
| 세그먼트(Segment) | segment 시작 위치 |
| 메모리 매핑 파일 | mapped region 시작 주소 |
| DMA Buffer | device-access 시작점 |

운영체제와 CPU는 `base + offset` 형태로 대부분의 메모리 접근을 수행합니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**가장 영향이 큰 자원: CPU + Memory**

| 자원 | 영향 |
|------|------|
| CPU Address Calculation | 주소 계산 |
| MMU | 가상 주소 변환 |
| Cache | locality 판단 |
| Memory Bus | 실제 접근 |
| TLB | page mapping |

> effective address calculation의 핵심 기준값

---

## 4. 왜 중요한가

베이스 주소가 중요한 이유는 모든 메모리 접근이 base-relative 방식으로 동작하기 때문입니다.

CPU는 메모리 전체를 직접 기억하지 않고, 대신 기준 주소 + 상대 거리(offset)로 계산합니다.

```
address = base + offset
```

배열 접근 `arr[i]`는 내부적으로:

```
base + (i × element_size)
```

> base address 없이는 구조화된 메모리 접근 자체가 불가능

---

## 5. 실제 장애와 어떤 관련이 있는가

| 문제 | 원인 |
|------|------|
| segmentation fault | invalid base access |
| null pointer dereference | base=0 접근 |
| buffer overflow | base boundary 초과 |
| use-after-free | 해제된 base 접근 |
| dangling pointer | invalid base 유지 |
| memory corruption | 잘못된 base arithmetic |

잘못된 `base + offset` 계산은 다른 객체 침범 / 커널 패닉 / 프로세스 crash / 데이터 손상으로 이어질 수 있습니다.

> incorrect base address calculation = catastrophic memory corruption

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Base + Offset

CPU 메모리 접근의 핵심 공식:

```
effective_address = base_address + offset
```

| 요소 | 값 |
|------|----|
| base | 0x1000 |
| offset | 0x20 |
| 결과 | 0x1020 |

모든 구조화된 데이터 접근은 이 원리 기반입니다.

### Effective Address

CPU가 실제 사용하는 최종 주소 구성:

| 요소 | 의미 |
|------|------|
| Base | 기준점 |
| Offset | 상대 거리 |
| Index | 배열 인덱스 |
| Scale | 데이터 크기 |

```
base + (index × scale) + displacement
```

### Object Layout

객체 내부도 동일한 원리입니다.

| offset | 필드 |
|--------|------|
| +0 | header |
| +8 | field A |
| +16 | field B |

```
field access = object_base + field_offset
```

### Stack Frame

| 레지스터 | 역할 |
|----------|------|
| BP / RBP | frame base |
| SP / RSP | stack top |

지역 변수 접근은 `base pointer + offset` 형태입니다.

### Array Addressing

`arr[i]`는 내부적으로:

```
base + (i × element_size)
```

연속 메모리 구조가 필요한 이유도 이것 때문입니다.

### Virtual Memory

현대 시스템의 base address는 대부분 virtual address입니다.

| 주소 종류 | 의미 |
|-----------|------|
| Virtual Base | 프로세스 관점 |
| Physical Base | 실제 RAM 위치 |

실제 물리 주소는 MMU / Page Table / TLB를 거쳐 변환됩니다.

### Relocation

프로그램은 로딩 시 새로운 base address를 부여받을 수 있습니다.

예: PIE / ASLR / shared library

따라서 absolute address 대신 base-relative 접근을 사용합니다.

### Base Register

| 레지스터 | 의미 |
|----------|------|
| RIP/EIP | instruction base |
| RBP | stack frame base |
| segment base | memory segment base |

CPU는 이 값을 기반으로 주소 계산을 수행합니다.

### Memory Protection

베이스 주소는 보호 경계의 시작점입니다. 운영체제는 `base ~ limit` 영역만 접근을 허용하며, 초과 시 segmentation fault / access violation이 발생합니다.

### Pointer Arithmetic

```
ptr + 1  →  base + sizeof(type)
```

### Alignment

| alignment | 의미 |
|-----------|------|
| 4-byte | 32bit aligned |
| 8-byte | 64bit aligned |
| 64-byte | cache line aligned |

잘못된 alignment는 성능 저하 또는 일부 CPU에서 fault를 발생시킬 수 있습니다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**대표 관측 도구:** `cat /proc/<pid>/maps` / `pmap` / `gdb` / `readelf` / `objdump`

```bash
cat /proc/self/maps
```

| 영역 | base address 존재 |
|------|-------------------|
| text | code base |
| heap | heap base |
| stack | stack base |
| mmap | mapping base |

### Runtime

| 영역 | base usage |
|------|------------|
| heap allocator | allocation base |
| GC | object relocation |
| object access | field offset |
| arrays | indexed addressing |

GC compacting은 object base address를 변경할 수 있어 참조 업데이트가 필요합니다.

### Kubernetes

| 요소 | 관련 |
|------|------|
| container process | virtual memory base |
| shared library | relocation |
| ASLR | randomized base |
| huge pages | aligned base |

container도 결국 Linux process이므로 동일한 base address 구조를 따릅니다.

### Observability

**대표 분석 도구:** `perf` / `gdb` / `valgrind` / `strace` / `ltrace`

| 현상 | 의미 |
|------|------|
| invalid address | 잘못된 base |
| SIGSEGV | 접근 불가 |
| corrupted pointer | base 손상 |
| heap corruption | allocator 문제 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*