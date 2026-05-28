# 산술 연산 (Arithmetic Operation)

> 정독: 0회

## 1. 이 기술이 무엇인가

**산술 연산(Arithmetic Operation)** 은:

> 정수나 부동소수점 데이터를 하나의 숫자 값으로 취급하여 계산하는 연산

### 대표 연산

| 연산 | 명령어 예시 |
|------|-------------|
| 덧셈 | `ADD` |
| 뺄셈 | `SUB` |
| 곱셈 | `MUL` |
| 나눗셈 | `DIV` |
| 증가 | `INC` |
| 감소 | `DEC` |

### 핵심 특징

비트들이 서로 독립이 아니라, **하나의 숫자를 구성하는 연결된 값**으로 처리됩니다.

### 비트별 논리 연산과의 차이

| 구분 | 산술 연산 | 비트별 연산 |
|------|-----------|-------------|
| 데이터 관점 | 숫자 전체 | 비트 개별 |
| Carry 발생 | 있음 | 없음 |
| 목적 | 수치 계산 | 상태 제어 |
| 핵심 회로 | Adder | Logic Gate |

### 예시

```
0001 + 0001 = 0010
```

하위 비트의 carry가 상위 비트로 전파됩니다.

---

## 2. 시스템 어디에서 등장하는가

산술 연산은 거의 모든 계산 시스템의 기본입니다.

### 대표 영역

| 영역 | 사용 사례 |
|------|-----------|
| CPU | instruction execution |
| Runtime | object allocation size |
| OS Kernel | scheduler accounting |
| Memory Manager | address calculation |
| Filesystem | block offset |
| Network | checksum |
| Database | aggregation |
| Hypervisor | page calculation |
| Cryptography | modular arithmetic |
| AI/ML | tensor computation |

### 대표 예시

- `pointer offset`
- `loop counter`
- `memory size`
- `latency calculation`
- `hash computation`

---

## 3. 어떤 자원에 가장 영향이 큰가

### 1순위: CPU

ALU, FPU, instruction pipeline, execution unit 과 직접 연결됩니다.

### 2순위: Memory

address arithmetic, offset calculation, index computation 등이 메모리 접근과 직접 연결됩니다.

---

## 4. 왜 중요한가

산술 연산은 **컴퓨터가 실제 계산을 수행하게 만드는 핵심 엔진**입니다.

### 산술 연산이 없다면

- 주소 계산 불가
- 배열 접근 불가
- 반복문 불가
- 메모리 오프셋 계산 불가
- 통계 계산 불가

> 시스템 레벨에서는 모든 메모리 접근조차 결국 산술 연산 기반입니다.
> 예: `base address + offset`

---

## 5. 실제 장애와 어떤 관련이 있는가

### Integer Overflow

```
2147483647 + 1  →  overflow
```

발생 가능한 문제:

- wrong allocation
- corrupted size
- security vulnerability

### Division by Zero

대표 예외 상황 발생 가능:

- process crash
- trap
- signal fault

### Floating Point Precision

```
0.1 + 0.2 != 0.3
```

발생 가능한 문제:

- financial mismatch
- scientific calculation error

### Address Arithmetic Bug

offset 계산 오류 시:

- invalid memory access
- segmentation fault
- corruption

### Counter Overflow

scheduler/network/runtime counter overflow 시:

- metric corruption
- timeout bug
- rate-limit failure

---

## 6. 핵심 메커니즘

핵심은 **Carry Propagation** 입니다.

비트별 연산과 달리, 하위 비트 결과가 상위 비트에 영향을 줍니다.

### A. Addition (ADD)

```
  0011
+ 0001
──────
  0100
```

`1 + 1 = 10` 발생:

- 현재 비트 결과 = `0`
- carry → 상위 비트로 전달

> carry가 연쇄적으로 전파됩니다.

---

### B. Subtraction (SUB)

뺄셈은 **borrow** 개념을 사용합니다.

```
0010 - 0001
```

하위 비트 부족 시 상위 비트에서 borrow가 발생합니다.

---

### C. Multiplication (MUL)

핵심: **shift + add** 조합.

CPU 내부에서는 partial product → accumulation 형태로 처리됩니다.

---

### D. Division (DIV)

가장 비싼 연산 중 하나입니다.

이유:

- iterative calculation 필요
- quotient/remainder tracking 필요

---

### E. Floating Point Arithmetic

부동소수는 **sign / exponent / mantissa** 기반으로 정수 연산보다 훨씬 복잡합니다.

---

### 핵심 흐름

```
Operand Load
     ↓
ALU/FPU Arithmetic
     ↓
Carry/Borrow Propagation
     ↓
Result Register
     ↓
Store Back
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux Kernel

매우 광범위하게 사용:

- `scheduler counter`
- `page calculation`
- `timer arithmetic`
- `virtual memory offset`

### Runtime

runtime 내부:

- `heap size`
- `object offset`
- `allocation pointer`
- `GC counter`

### Networking

- `checksum`
- `sequence number`
- `congestion window`

### Filesystem

- `inode offset`
- `block number`
- `page cache index`

### Kubernetes

직접보다는 내부 시스템에서 사용:

- `cgroup accounting`
- `memory quota`
- `scheduler scoring`
- `resource calculation`

### CPU 관측 도구

- `perf`
- hardware counters
- flame graph
- instruction profiling

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*