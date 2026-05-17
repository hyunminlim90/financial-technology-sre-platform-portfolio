# Bit Marking (비트 마킹)
## Micro Foundations — CPU Flags / Bit Field / 상태 제어 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

Bit Marking은:

> **특정 비트 위치(Bit Position)에 의미 있는 상태를 1(Set) 또는 0(Clear)으로 기록하는 방식**

중요한 점은 전체 값을 하나의 숫자로 보는 것이 아니라, **각 비트를 독립 상태 슬롯으로 취급**한다는 것이다.

| 비트 위치 | 의미 |
|-----------|------|
| bit 0 | interrupt enabled |
| bit 1 | zero detected |
| bit 2 | overflow occurred |

> **1비트 = 하나의 상태 증명 공간**

---

## 2. 시스템 어디에서 등장하는가

매우 광범위하게 등장한다.

### CPU 내부
Status Register, CPU Flags, Interrupt Mask, Page Table Flag, Cache State Bit

### OS / Kernel
permission bit, process state flag, scheduler state, filesystem metadata

### Network / Device
NIC descriptor flag, packet state, DMA control bit, TCP option flag

### Storage
dirty bit, valid bit, inode permission, filesystem journal flag

> **Bit Marking은 컴퓨터 전체 상태 제어의 공통 언어**이다.

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **CPU + Memory**이다.

비트 연산은 CPU가 직접 수행하고, 상태 저장은 메모리/레지스터에 존재한다.

| 방식 | 메모리 사용 |
|------|-------------|
| boolean 32개 | 최소 수십 바이트 |
| bit field 32개 | 단 4바이트 |

아주 적은 메모리로 엄청 많은 상태를 표현할 수 있기 때문에, kernel, NIC, scheduler, filesystem, hypervisor 같은 저수준 시스템은 비트 마킹을 극도로 많이 사용한다.

---

## 4. 왜 중요한가

컴퓨터는 **상태(state)를 추적해야 동작 가능**하다.

추적이 필요한 상태의 예:

- 인터럽트 가능 여부
- 읽기 권한 여부
- dirty 상태 여부
- overflow 여부
- packet processed 여부

비트 마킹은 이 상태를 **최소 비용으로 표현하는 가장 효율적인 방식**이다.

> **핵심:** 비트 마킹은 **하드웨어와 커널의 상태 압축 기술**이다.

---

## 5. 실제 장애와의 관련성

### 1) 잘못된 Flag Marking
dirty bit 누락 시 → 데이터 flush가 되지 않아 **데이터 유실** 가능.

### 2) Permission Bit 오류
execute bit 잘못 marking 시 → **보안 취약점, privilege escalation** 가능.

### 3) Interrupt Flag 문제
IF bit 상태 오류 시 → **interrupt lost, deadlock, scheduler freeze** 가능.

### 4) Concurrent Bit Update 문제
멀티코어 환경에서 동일 비트를 동시에 수정하면 **race condition, lost update** 가 발생할 수 있다.
따라서 다음이 매우 중요하다:

- atomic bit operation
- compare-and-swap
- memory barrier

---

## 6. 핵심 메커니즘

### A. 비트는 독립 상태 슬롯

`10110100` 이라는 값은 숫자 하나일 수도 있지만, **8개의 독립 상태 집합**일 수도 있다.

| bit | 의미 |
|-----|------|
| 7 | enabled |
| 6 | dirty |
| 5 | locked |
| 4 | error |

### B. Set / Clear

| 동작 | 의미 |
|------|------|
| Set | bit = 1 (상태 활성화) |
| Clear | bit = 0 (상태 비활성화) |

### C. Bit Field 구조

Bit Marking은 보통 **bit field** 형태로 설계된다.

| bits | 의미 |
|------|------|
| 0~3 | permission |
| 4~7 | status |
| 8~15 | device type |

하나의 레지스터 안에 **여러 의미를 압축 저장**한다.

### D. CPU Flags도 Bit Marking

| bit | 의미 |
|-----|------|
| `ZF` | zero |
| `SF` | sign |
| `OF` | overflow |
| `CF` | carry |

ALU 결과에 따라 **특정 bit만 set/clear**된다.

### E. Bitmask와의 연결

비트 마킹은 보통 **bitmask**와 함께 사용된다.

```
00000100  ← mask
```

mask를 사용하면 **특정 bit만 검사**할 수 있다.

### F. Atomic Operation의 중요성

멀티코어에서는 **bit update 자체도 경쟁 자원**이다. 따라서 다음이 중요하다:

- atomic set / atomic clear
- lock-prefixed instruction

---

## 7. Linux / Runtime / K8s에서의 관측

### Linux Permission

```bash
chmod 755
```

실제로는 `111 101 101` 비트 마킹 체계이다.

### Kernel Flags

대표적인 사례: page flags, inode flags, task state, interrupt mask

### Networking

TCP SYN / ACK / FIN flags, NIC descriptor state, DMA status

### CPU 관측

`perf`, `/proc`, `top` 등은 내부적으로 CPU state flag 기반으로 동작하는 경우가 많다.

### Filesystem

dirty page, cache valid bit, journal committed bit

### Virtualization

VM state, shadow page table flags, EPT permissions

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*