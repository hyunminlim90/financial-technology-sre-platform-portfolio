# CPU Flags (Status Register)
## Micro Foundations — 조건 분기 / CPU 상태 / 파이프라인 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

CPU Flags는:

> **CPU가 직전 연산 결과 상태를 1비트 단위로 기록해 두는 특수 목적 상태 레지스터**

즉 CPU는 연산 직후 다음 상태를 즉시 기록한다:

- 결과가 0인지
- 음수인지
- overflow인지
- carry가 발생했는지

> **핵심:** CPU Flags는 "방금 연산 결과가 어땠는가"를 기억하는 **하드웨어 상태 원장**이다.

<details>
<summary>Deep Dive</summary></br>

Condition Code(조건 코드) [[M]](../../100-deep-dive/micro-foundations/condition-code.md)  
Bit Marking(비트 마킹) [[M]](../../100-deep-dive/micro-foundations/bit-marking.md)

</details></br>

## 2. 시스템 어디에서 등장하는가

가장 직접적으로는 **CPU 내부 ALU 뒤쪽**에서 등장한다.

연산 완료 직후 다음 흐름으로 갱신된다:

```
ALU 결과 → Flags Register 갱신
```

특히 중요한 곳:

- 조건문 (`if`)
- loop
- branch / jump
- compare
- interrupt control
- kernel critical section

> **모든 제어 흐름(Control Flow)의 물리적 기준점**이 된다.

---

## 3. 어떤 자원에 가장 영향이 큰가

압도적으로 **CPU pipeline**이다.

Flags 자체는 작은 레지스터지만 **분기(Branch) 성능 전체를 좌우**한다.

특히 영향이 큰 것:

- branch prediction
- speculative execution
- pipeline stall
- instruction dependency

> **핵심:** Flags는 작은 비트지만 **CPU 전체 처리량에 매우 큰 영향**을 준다.

---

## 4. 왜 중요한가

CPU가 **다음에 어디로 실행 흐름을 보낼지 결정**하기 때문이다.

예시: `if (a == b)` 는 CPU 내부에서 다음과 같이 동작한다:

```
1. ALU가 a - b 계산
2. 결과가 0이면 ZF = 1
3. branch instruction이 ZF 확인
4. PC 변경 (실행 흐름 이동)
```

즉, **상위 언어의 모든 조건문은 결국 CPU Flags 기반**이다.

Flags 없이는 다음이 전부 불가능하다:

- 조건문
- 반복문
- 예외 흐름
- 인터럽트 처리

---

## 5. 실제 장애와의 관련성

### 1) Branch Misprediction
CPU가 flags 결과 예측 실패 → **pipeline flush** 발생.
고성능 서버에서는 tail latency 증가, IPC 감소의 원인이 된다.

### 2) Pipeline Dependency Stall
다음 명령이 이전 flags 갱신 완료까지 대기 → **pipeline bubble, execution stall** 발생.

### 3) Interrupt Mask 문제
IF(Interrupt Flag) 잘못 제어 시:

- interrupt storm
- deadlock
- kernel freeze

가 발생 가능하다.

### 4) Overflow 처리 실패
OF/CF 무시 시:

- 금융 계산 오류
- 정수 overflow
- 주소 계산 오류

가 발생 가능하다.

> **핵심:** Flags는 단순 상태 비트가 아니라 **CPU 제어 흐름의 핵심 기준점**이다.

---

## 6. 핵심 메커니즘

### A. ALU 연산 후 즉시 갱신

```
5 - 5 = 0  →  ZF = 1
```

### B. Branch Instruction이 참조

`JE (Jump if Equal)` 는 실제로 `ZF == 1` 을 검사한다.

### C. Program Counter 변경

조건이 참이면 **PC 주소 변경** → 실행 흐름 점프.

### D. 대표 Flags

| Flag | 의미 |
|------|------|
| `ZF` | 결과가 0 |
| `SF` | 음수 여부 |
| `OF` | signed overflow |
| `CF` | carry / borrow |
| `IF` | interrupt 허용 여부 |

### E. Flags는 매우 짧게 유지됨

다음 연산이 들어오면 **flags는 즉시 덮어쓰기**된다. 즉 임시 상태 정보다.

### F. Branch Prediction과의 연결

현대 CPU는 flags 결과가 나오기 전에 **미리 다음 명령 실행을 예측**한다.
예측이 틀리면 **pipeline flush**가 발생한다.

> **핵심:** 현대 CPU 성능은 **flags 기반 분기를 얼마나 효율적으로 처리하느냐**와 매우 깊게 연결된다.

---

## 7. Linux / Runtime / K8s에서의 관측

Flags 자체는 직접 거의 보이지 않는다. 하지만 영향은 간접적으로 매우 많이 관측된다.

### perf (가장 중요)

```bash
perf stat    # branch misses, stalled cycles, IPC 요약
perf top     # 실시간 hot function 관측
```

관측 가능한 지표: branch misses, stalled cycles, IPC 감소

### CPU Pipeline 분석

고성능 환경에서는 **branch miss rate**가 매우 중요한 지표다.

### Kernel

Interrupt Flag 관련 영역에서 등장:

- IRQ disable section
- spinlock
- scheduler critical section

### NUMA / SMP 환경

멀티코어에서는 **flags dependency + memory ordering** 문제가 성능에 영향을 준다.

> **핵심:** 운영 환경에서는 flags 자체보다 **branch miss와 pipeline stall 형태**로 관측된다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*