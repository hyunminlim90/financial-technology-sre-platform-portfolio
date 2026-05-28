# 워드 (Word)

> 정독: 0회

## 1. 이 기술이 무엇인가

**워드(Word)** 는:

> CPU가 한 번의 기본 연산 단위로 처리하도록 설계된 데이터 크기

### 대표 예시

| 아키텍처 | 워드 크기 |
|----------|-----------|
| 8-bit | 8비트 |
| 16-bit | 16비트 |
| 32-bit | 32비트 |
| 64-bit | 64비트 |

현대 시스템에서는 대부분 **64-bit word** 기반입니다.

### CPU와의 연결

워드는 CPU의 다음 요소와 강하게 연결됩니다:

- 레지스터 크기
- 데이터 버스 폭
- 주소 계산 단위
- ALU 처리 단위

---

## 2. 시스템 어디에서 등장하는가

워드는 시스템 전체 구조의 기준 단위입니다.

### 등장 영역

| 영역 | 관련 요소 |
|------|-----------|
| CPU | register |
| Memory | aligned access |
| OS | virtual address |
| Compiler | ABI |
| Runtime | object alignment |
| Kernel | syscall ABI |
| Network | packet parsing |
| Hypervisor | guest architecture |

### 대표 예시

- `64-bit register`
- `64-bit pointer`
- `64-bit address calculation`
- `word-aligned memory access`

---

## 3. 어떤 자원에 가장 영향이 큰가

### 1순위: CPU + Memory

워드는 **CPU가 가장 효율적으로 처리 가능한 데이터 폭**을 정의하기 때문입니다.

### 영향 영역

| 자원 | 영향 |
|------|------|
| CPU | register width |
| Memory | alignment/access efficiency |
| Cache | cache line utilization |
| Bus | transfer width |
| MMU | address translation |

---

## 4. 왜 중요한가

워드는 **시스템 성능의 기본 기준**입니다.

CPU는 일반적으로 **word 단위 접근을 가장 빠르게 처리**합니다.

### 64-bit CPU에서의 효율적 연산

- `64-bit load/store`
- `64-bit arithmetic`
- `64-bit address calculation`

### misaligned/partial access의 비용

- extra memory cycle
- cache penalty
- pipeline stall

### 워드가 결정하는 것들

- pointer size
- address space
- ABI
- syscall interface
- binary compatibility

---

## 5. 실제 장애와 어떤 관련이 있는가

### Alignment Fault

워드 경계에 맞지 않는 접근 시:

- bus error
- alignment trap

> 특히 ARM 계열에서 중요합니다.

### 32-bit Overflow

32-bit 환경에서 address space exhaustion 발생 가능:

- memory limitation
- large file limitation

### ABI Mismatch

32-bit binary와 64-bit library 혼용 시:

- struct layout mismatch
- pointer corruption
- syscall failure

### Atomicity 문제

word size 초과 데이터는 atomic access 보장이 불가할 수 있습니다. 멀티스레드 환경에서 중요합니다.

### Cache Inefficiency

word alignment가 깨지면:

- extra cache access
- memory bandwidth waste

---

## 6. 핵심 메커니즘

핵심은 **CPU는 워드 단위 데이터를 가장 효율적으로 처리한다**는 것입니다.

### A. Register 기반 처리

64-bit CPU의 register는 **64bit width**를 보유합니다.

load, arithmetic, store 모두 64비트 기준으로 최적화됩니다.

---

### B. Memory Access

CPU는 **word-aligned access**를 선호합니다.

```
64-bit CPU  →  8-byte aligned 접근 최적
```

alignment가 맞지 않으면:

- multiple memory read
- microcode assist
- stall

---

### C. Address Calculation

64-bit CPU는 **64-bit virtual address register** 기반으로 주소를 계산합니다.

워드는 단순 데이터 크기가 아니라 **CPU 전체 주소 체계의 기준**입니다.

---

### D. ALU 처리

| 연산 종류 | 처리 방식 |
|-----------|-----------|
| 산술 연산 | word 전체를 하나의 scalar value로 처리 |
| 비트 연산 | word 내부 비트를 독립 제어 |

워드는 arithmetic, bitwise, addressing **모든 계산의 기본 단위**입니다.

---

### E. 현대 시스템 특징

현대 CPU는 SIMD, AVX, NEON 등으로 word보다 더 큰 벡터 처리가 가능합니다.

하지만 기본 ISA 기준 단위는 여전히 **word**입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
uname -m
getconf LONG_BIT
lscpu
```

대표 출력: `x86_64`, `64-bit`

### ELF Binary

```bash
readelf -h binary
```

pointer size 및 아키텍처 확인 가능.

### Kernel

kernel ABI는 모두 word 기반:

- syscall register convention
- page table format
- virtual address size

### Runtime

runtime 내부에서 word가 영향을 주는 요소:

- pointer width
- object alignment
- stack frame layout

### Kubernetes

간접적으로 영향:

- container image architecture
- node architecture
- multi-arch scheduling

```
amd64  vs  arm64
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*