# Bit Slot (비트 슬롯)
## Micro Foundations — Register / CPU Flags / Bit Field 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

Bit Slot은:

> **레지스터나 메모리 내부에서 단 하나의 비트(0 또는 1)만 저장하는 독립된 위치(position)** 

쉽게 말하면 **레지스터 내부의 가장 작은 방 한 칸**이다.

예시: `10110010` 이라는 8비트 값에서 각 자리 하나하나가 모두 독립 비트 슬롯이다.

| 위치 | 값 |
|------|----|
| bit 7 | 1 |
| bit 6 | 0 |
| bit 5 | 1 |
| ... | ... |

> **핵심:** Bit Slot = **비트가 저장되는 물리적 위치**

---

## 2. 시스템 어디에서 등장하는가

거의 모든 컴퓨터 시스템에 존재한다.

### CPU 내부
Status Register, CPU Flags, Program Counter, Control Register

### 메모리 구조
page table entry, cache metadata, ECC memory bit, bitmap allocator

### OS / Kernel
permission bit, scheduler state, interrupt mask, filesystem inode flag

### Network / Device
TCP flag slot, NIC descriptor state, DMA control bit

> **컴퓨터의 상태 제어 대부분은 비트 슬롯 위에서 동작**한다.

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **CPU + Memory**이다.

bit slot은 register/memory 내부 구조이며, CPU가 직접 읽고 수정하고, cache coherence의 영향을 받는다.

| 저장 방식 | 공간 |
|-----------|------|
| boolean 64개 | 수십~수백 바이트 |
| 64 bit slots | 단 8바이트 |

극도로 작은 공간으로 매우 많은 상태를 표현할 수 있기 때문에, kernel, hypervisor, filesystem, network stack은 bit slot 활용이 매우 많다.

---

## 4. 왜 중요한가

컴퓨터는 결국 **상태(state)를 저장하고 추적**해야 움직인다. 그 상태의 최소 단위가 **1 bit**이다.

| bit slot | 의미 |
|----------|------|
| interrupt enabled | yes / no |
| cache dirty | yes / no |
| packet valid | yes / no |
| overflow occurred | yes / no |

> **핵심:** 복잡한 시스템도 결국 **bit slot 상태들의 조합**으로 움직인다.

---

## 5. 실제 장애와의 관련성

### 1) 잘못된 Bit Slot Update
dirty bit slot 미갱신 시 → cache flush 누락 → **데이터 손실** 가능.

### 2) Permission Slot 오류
execute permission bit 잘못 set 시 → **보안 취약점, 권한 상승** 가능.

### 3) Race Condition
멀티코어 환경에서 동일 bit slot 동시 수정 시 → **lost update, inconsistent state** 발생 가능.

### 4) Hardware Fault
메모리 bit slot 손상 시 → **ECC error, corrupted page table, kernel panic** 가능.

---

## 6. 핵심 메커니즘

### A. Bit Slot은 독립 공간

`10101100` 에서 bit 0, bit 1, bit 2는 각각 **완전히 독립된 저장 위치**이다.
bit 하나를 변경해도 **다른 slot은 영향 없음**이 핵심 원칙이다.

### B. Bit Index와의 연결

각 bit slot에는 번호가 존재한다.

| index | 의미 |
|-------|------|
| bit 0 | LSB |
| bit 31 | MSB |

즉, **bit slot = 위치 기반 제어**이다.

### C. CPU Flags는 Bit Slot 집합

| bit slot | 의미 |
|----------|------|
| `ZF` | zero |
| `SF` | sign |
| `OF` | overflow |
| `CF` | carry |

ALU가 연산을 끝내면 **특정 slot만 set/clear**된다.

### D. 물리적으로는 Flip-Flop

하드웨어 레벨에서 **bit slot = flip-flop / latch 회로**이다.

- 전압 유지
- 상태 기억
- 클록 동기화

를 수행한다.

### E. Bit Field 구조

여러 bit slot의 묶음이 **bit field**가 된다.

| bits | 의미 |
|------|------|
| 0~3 | permission |
| 4~7 | state |
| 8~15 | type |

Bit Slot들이 모여 **상태 구조체를 형성**한다.

### F. Atomic Update의 중요성

멀티코어에서는 **bit slot 하나도 경쟁 자원**이다. 따라서 다음이 중요하다:

- atomic bit set
- compare-and-swap
- lock prefix
- memory barrier

---

## 7. Linux / Runtime / K8s에서의 관측

### Linux Permission

```bash
chmod 755
```

실제로는 `111 101 101` 비트 슬롯 조합이다.

### Kernel

page flags, inode state, scheduler bitmap, CPU affinity mask

### Networking

TCP SYN / ACK / FIN, NIC ring descriptor, DMA ownership bit

### Filesystem

dirty page bit, valid block bit, journal state bit

### Virtualization

page permission bit, EPT flag, VM state flag

### CPU 관측

`perf`, `/proc`, `vmstat` 등은 내부적으로 bit state 기반 구조를 많이 사용한다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*