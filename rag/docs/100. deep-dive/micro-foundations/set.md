# Set (세트 / 세팅)
## Micro Foundations — Bit Slot / CPU Flags / 상태 활성화 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

Set은:

> **특정 비트 슬롯(Bit Slot)의 상태를 1(True / Enabled / Active)로 만드는 행위**

핵심은 **"의미 있는 상태가 활성화되었다"** 를 선언하는 것이다.

| bit | 의미 |
|-----|------|
| 0 | interrupt enabled |
| 1 | dirty page |
| 2 | overflow occurred |

여기서 `bit = 1` 이 되면 해당 상태가 **활성(active)** 되었다는 뜻이다.

> **Set = 상태 활성화**

---

## 2. 시스템 어디에서 등장하는가

거의 모든 시스템 내부에서 등장한다.

### CPU 내부
CPU Flags, Status Register, Interrupt Enable Bit, Cache Dirty Bit

### Kernel
process state, page dirty flag, scheduler bitmap, lock state

### Network
TCP SYN set, ACK set, FIN set

### Storage
valid bit set, journal committed bit, cache dirty bit

> **시스템 상태 변화 대부분은 bit set / clear 형태로 관리**된다.

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **CPU + Memory**이다.

CPU가 set instruction을 수행하고, register/cache/memory 내부 상태가 변경되며, cache coherence에 영향이 발생하기 때문이다.

| set 되는 bit | 영향 |
|--------------|------|
| interrupt enable | CPU 인터럽트 허용 |
| dirty bit | flush 필요 |
| lock bit | 다른 CPU 접근 차단 |

> **1비트만 바꾸지만 시스템 전체 흐름이 바뀔 수 있다.**

---

## 4. 왜 중요한가

컴퓨터는 **상태(state)를 기반으로 동작**한다. Set은 그 상태를 활성화하는 **가장 원자적인 제어 행위**이다.

다음이 모두 **bit set** 으로 표현된다:

- interrupt 가능
- overflow 발생
- cache dirty
- packet valid
- lock acquired

> **핵심:** Set은 **하드웨어 상태 전환의 핵심 이벤트**이다.

---

## 5. 실제 장애와의 관련성

### 1) Dirty Bit 미세트
dirty bit가 set 되지 않으면 → cache flush 누락 → **데이터 유실** 가능.

### 2) Lock Bit 미해제
lock bit set 후 clear 되지 않으면 → **deadlock, spin lock 무한 대기** 가능.

### 3) Interrupt Bit 오류
interrupt enable bit 잘못 set 시 → **interrupt storm, kernel instability** 가능.

### 4) Concurrent Set Race
멀티코어 환경에서 동일 bit 동시 set 시 → **race condition, inconsistent state** 발생 가능.
따라서 다음이 매우 중요하다:

- atomic set
- memory barrier
- cache coherency

---

## 6. 핵심 메커니즘

### A. Set은 "1로 만드는 행위"

특정 슬롯만 활성화된다:

| 이전 | 이후 |
|------|------|
| `0000` | `0100` |

### B. Set ≠ 숫자 계산

Set은 숫자 계산 목적이 아니라 **상태 활성화 목적**이다.
`1` 이라는 수치 자체보다 **"상태가 켜졌다"** 가 핵심이다.

### C. Flip-Flop / Latch와의 연결

하드웨어 레벨에서 Set은 **전압 상태를 유지**하는 행위다:

- High voltage 유지
- transistor state 유지
- latch 상태 고정

### D. CPU Flags의 Set

| 상황 | set 되는 flag |
|------|---------------|
| 결과 = 0 | `ZF` set |
| overflow 발생 | `OF` set |
| carry 발생 | `CF` set |

ALU가 연산을 마치면 **특정 flag slot만 set**된다.

### E. Set 이후 Branch 가능

`ZF` 가 set 되면 `Jump if Zero` 분기가 가능하다.
즉, **Set 상태가 프로그램 흐름 자체를 바꾼다.**

### F. Atomic Set의 중요성

멀티코어에서는 **set 자체도 경쟁 자원**이다. 따라서 다음을 사용한다:

- atomic bit set
- lock prefix
- compare-and-swap

---

## 7. Linux / Runtime / K8s에서의 관측

### Linux Permission

```bash
chmod +x    # execute bit set
```

### Kernel

page dirty bit, interrupt mask, process state, CPU affinity mask

### Network

TCP SYN set, ACK set, FIN set

### Storage

cache dirty set, filesystem journal flag, valid block flag

### Virtualization

VM running flag, EPT permission bit, hypervisor state bit

### CPU 관측

`perf`, `/proc`, `vmstat` 은 내부적으로 수많은 bit set state 기반으로 동작한다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*