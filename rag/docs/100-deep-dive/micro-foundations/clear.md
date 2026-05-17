# Clear (클리어 / 클리어링)
## Micro Foundations — Bit Slot / CPU Flags / 상태 비활성화 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

Clear는:

> **특정 비트 슬롯(Bit Slot)의 상태를 0(False / Disabled / Inactive)으로 만드는 행위**

핵심은 **"이 상태는 더 이상 활성 상태가 아니다"** 를 선언하는 것이다.

| bit 상태 | 의미 |
|----------|------|
| interrupt bit = 0 | 인터럽트 비활성 |
| dirty bit = 0 | flush 필요 없음 |
| lock bit = 0 | lock 해제 |

> **Clear = 상태 비활성화**

---

## 2. 시스템 어디에서 등장하는가

거의 모든 시스템 계층에서 등장한다.

### CPU 내부
Zero Flag clear, Carry Flag clear, interrupt disable, pipeline valid bit clear

### Kernel
lock release, process wait flag clear, interrupt pending clear

### Network
TCP FIN clear, ACK clear, NIC interrupt clear

### Storage
dirty bit clear, cache valid bit clear, filesystem journal clear

> **상태 종료 / 비활성화 대부분은 bit clear 로 표현**된다.

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **CPU + Memory**이다.

register state 변경, cache line 상태 변경, synchronization 상태 변경이 발생하기 때문이다.

| clear 되는 bit | 영향 |
|----------------|------|
| interrupt enable clear | 인터럽트 차단 |
| valid bit clear | 데이터 무효화 |
| lock bit clear | 다른 CPU 진입 허용 |

> **1비트 clear가 전체 시스템 흐름을 바꿀 수 있다.**

---

## 4. 왜 중요한가

컴퓨터 시스템은 상태를 생성(Set)만 하는 것이 아니라 **반드시 제거(Clear)도 해야 한다.**

Set만 있고 Clear가 없으면 다음 문제가 발생한다:

- lock 영구 유지
- interrupt 영구 활성
- dirty state 영구 유지
- stale cache 유지

> **핵심:** Clear는 **시스템 상태 정리(cleanup)의 핵심**이다.

---

## 5. 실제 장애와의 관련성

### 1) Interrupt Pending 미클리어
interrupt pending bit clear 실패 시 → **interrupt storm, CPU soft lockup** 가능.

### 2) Lock Bit 미클리어
spinlock clear 실패 시 → **deadlock, CPU busy spin, throughput 급락** 가능.

### 3) Dirty Bit 미클리어
dirty bit clear 누락 시 → **unnecessary flush 반복, storage latency 증가** 가능.

### 4) Cache Valid Bit 미클리어
invalid cache line clear 실패 시 → **stale data read, consistency corruption** 가능.

> **Set 오류보다 Clear 누락이 더 치명적인 경우가 많다.**

---

## 6. 핵심 메커니즘

### A. Clear는 "0으로 만드는 행위"

특정 상태가 제거된다:

| 이전 | 이후 |
|------|------|
| `0100` | `0000` |

### B. Clear는 "비활성화"

Clear의 핵심 의미는 **"해당 상태는 더 이상 유효하지 않다"** 이다.
`0` 이라는 숫자보다 **"상태 종료"** 가 핵심이다.

### C. Flip-Flop / Latch 상태 복귀

하드웨어 레벨에서 Clear는 **저장된 전압 상태를 Low 상태로 되돌리는** 행위이다:

- stored charge 제거
- latch reset
- transistor state reset

### D. CPU Flags의 Clear

| 상황 | clear 되는 flag |
|------|-----------------|
| 결과 ≠ 0 | `ZF` clear |
| overflow 없음 | `OF` clear |
| carry 없음 | `CF` clear |

ALU가 연산을 마치면 **특정 flag slot을 clear**한다.

### E. Branch 흐름 제어

`ZF` 가 clear 되면 `Jump if Zero` 가 실패한다.
따라서 **프로그램 카운터(PC)가 다음 순차 명령으로 진행**한다.

즉, **Clear 상태도 프로그램 흐름을 결정**한다.

### F. Atomic Clear의 중요성

멀티코어에서는 **clear도 경쟁 자원**이다.

- concurrent flag clear
- lock release race
- interrupt clear timing

따라서 다음이 중요하다: atomic clear, memory barrier, cache coherency

---

## 7. Linux / Runtime / K8s에서의 관측

### Linux Permission

```bash
chmod -x    # execute bit clear
```

### Kernel

interrupt mask clear, scheduler bitmap clear, page dirty clear

### Network

TCP flag clear, NIC interrupt acknowledge, packet ownership clear

### Storage

dirty page clear, cache invalidation, journal completion clear

### Virtualization

VM exit reason clear, EPT accessed bit clear, shadow page state clear

### CPU 관측

`perf`, `/proc/interrupts`, `vmstat` 은 내부적으로 수많은 clear / set state 변화를 기반으로 동작한다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*