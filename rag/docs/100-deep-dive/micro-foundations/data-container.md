# 데이터 컨테이너 (Data Container)

> 정독: 0회

## 1. 이 기술이 무엇인가

**데이터 컨테이너(Data Container)** 는:

> CPU와 메모리가 데이터를 이동·처리할 때 사용하는 고정 크기의 물리적 데이터 단위

### 대표 형태

| 형태 | 예시 |
|------|------|
| Byte Container | 8bit |
| Word Container | 32bit / 64bit |
| Register Container | CPU register |
| Memory Container | aligned memory block |

### 핵심

**CPU는 데이터를 항상 일정 크기의 덩어리(container) 단위로 처리합니다.**

load, store, arithmetic, bitwise operation 모두 컨테이너 단위로 수행됩니다.

---

## 2. 시스템 어디에서 등장하는가

데이터 컨테이너는 시스템 전체에서 등장합니다.

### 대표 영역

| 영역 | 컨테이너 형태 |
|------|---------------|
| CPU | register |
| Memory | aligned word |
| Cache | cache line |
| Network | packet field |
| Filesystem | block/page |
| Runtime | object slot |
| Kernel | page/frame |
| Device | hardware register |

### 대표 예시

- `64-bit register`
- `8-byte aligned memory`
- `64-byte cache line`
- `page table entry`
- `packet header field`

---

## 3. 어떤 자원에 가장 영향이 큰가

### 1순위: CPU + Memory

데이터 컨테이너는 **CPU와 메모리 간 데이터 이동의 기본 단위**이기 때문입니다.

### 영향 영역

| 자원 | 영향 |
|------|------|
| CPU | register access |
| Memory | alignment/access efficiency |
| Cache | cache locality |
| Bus | transfer width |
| Disk | block I/O |
| Network | packet transfer unit |

---

## 4. 왜 중요한가

CPU는 **컨테이너 단위 데이터 처리에 최적화**되어 있습니다.

### 가장 빠른 접근 방식

- aligned access
- register-sized operation
- cache-aligned transfer

### 컨테이너 경계가 깨질 때의 비용

- extra memory cycle
- cache miss
- bus split
- pipeline stall

### 핵심 원칙

비트 조작도 실제로는 **컨테이너 전체를 로드한 후 내부 비트를 수정**합니다.

> 비트 단위 조작조차 물리적으로는 컨테이너 단위로 처리됩니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### Misaligned Access

컨테이너 정렬이 깨지면:

- alignment fault
- performance degradation

### Partial Write 문제

멀티스레드 환경에서 컨테이너 일부만 수정 시:

- torn write
- race condition

### Cache Inefficiency

컨테이너 경계가 cache line을 넘으면:

- extra cache fetch
- memory bandwidth waste

### Atomicity 문제

CPU는 일반적으로 **word-sized aligned container** 에 대해서만 atomic 접근을 보장합니다.

### ABI/Layout 문제

컴파일러/아키텍처 차이로 padding, alignment, container packing 차이가 발생할 수 있습니다.

---

## 6. 핵심 메커니즘

핵심은 **CPU는 데이터를 개별 비트가 아니라 컨테이너 단위로 이동·처리한다**는 것입니다.

### A. Allocation

컴파일러/runtime이 고정 크기 메모리 공간을 확보합니다.

```
32bit int
64bit word
```

---

### B. Load

CPU는 **컨테이너 전체를 register로 복사**합니다.

```
64bit field 하나 읽기  →  64bit 전체 load 발생
```

---

### C. Manipulation

register 내부에서 수행:

- bit masking
- arithmetic
- shift
- compare

> 비트 연산도 register 내부 컨테이너 기반으로 수행됩니다.

---

### D. Store

수정 후 **컨테이너 전체를 다시 memory에 저장**합니다.

```
비트 하나 수정  →  실제 store는 전체 container 단위
```

---

### E. Alignment

현대 CPU는 **aligned container access** 에 최적화되어 있습니다.

```
64bit CPU  →  8-byte aligned access 선호
```

alignment가 깨지면:

- extra load/store
- split transaction
- cache penalty

---

### 핵심 흐름

```
Allocation (고정 크기 공간 확보)
        ↓
Load (container 전체를 register로)
        ↓
Manipulation (register 내부 연산)
        ↓
Store (container 전체를 memory로)
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

관측 가능 영역: page, cache line, register width, alignment

```bash
lscpu
getconf LONG_BIT
```

### Compiler/ABI

```bash
pahole
readelf
objdump
```

### Runtime

runtime 내부에서 컨테이너가 영향을 주는 요소:

- object layout
- field alignment
- stack frame
- pointer slot

### Kernel

kernel 내부 container 기반 구조:

- `page frame`
- `page table entry`
- `inode structure`
- `scheduler structure`

### Kubernetes

간접적으로 영향:

- container image architecture
- node architecture
- memory alignment behavior

### Hardware/Driver

매우 중요한 영역:

- `MMIO register`
- `DMA buffer`
- `NIC descriptor`

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*