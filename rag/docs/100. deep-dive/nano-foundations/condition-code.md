# Condition Code (조건 코드)
## Micro Foundations — ALU 결과 상태 / 분기 제어 / CPU 흐름 제어 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

Condition Code는:

> **ALU 연산 직후 생성되는 "연산 결과 상태 신호"**

즉 CPU는 연산을 수행한 뒤 다음 상태를 즉시 판별한다:

- 결과가 0인지
- 음수인지
- overflow인지
- carry가 발생했는지

이 판별 결과가 Condition Code이며, 이 신호들은 **CPU Flags(Status Register)** 에 저장된다.

> **핵심:** Condition Code는 직전 연산 결과의 상태를 의미하는 **하드웨어 판별 신호**이다.

<details>
<summary>Deep Dive</summary></br>

Status Signals(상태 신호) [[M]](../../100-deep-dive/micro-foundations/status-signals.md)

</details></br>

## 2. 시스템 어디에서 등장하는가

가장 직접적으로는 **ALU 실행 직후** 등장한다.

```
ALU 연산 → Condition Code 생성 → Flags Register 저장 → 다음 분기 명령이 참조
```

즉 다음 CPU 제어 흐름 전체와 연결된다:

- 조건문 (`if`)
- loop
- branch / jump
- compare
- interrupt 판단

| 위치 | 역할 |
|------|------|
| ALU | 상태 생성 |
| Status Register | 상태 저장 |
| Branch Unit | 상태 참조 |
| Program Counter | 실행 흐름 변경 |

---

## 3. 어떤 자원에 가장 영향이 큰가

거의 전부 **CPU Pipeline**이다.

Condition Code는 다음에 직접 영향을 준다:

- branch prediction
- pipeline flow
- speculative execution
- instruction scheduling

메모리/Disk보다 **CPU 제어 흐름(Control Flow)** 영향이 훨씬 크다.

> **핵심:** Condition Code는 **CPU가 다음 명령을 어떻게 실행할지 결정하는 기준점**이다.

---

## 4. 왜 중요한가

CPU는 **다음 실행 경로를 Condition Code 기반으로 결정**한다.

예시: `if (a == b)` 는 하드웨어에서 다음과 같이 동작한다:

```
1. ALU가 a - b 계산
2. 결과가 0인지 검사
3. Zero Condition 생성
4. ZF(Zero Flag) 저장
5. branch instruction이 ZF 확인
6. PC 변경 여부 결정
```

즉, **상위 언어의 모든 조건문은 Condition Code 기반**이다.

Condition Code 없이는 다음이 전부 불가능하다:

- `if` / `while` / `for` / `switch`
- exception flow
- syscall branching

---

## 5. 실제 장애와의 관련성

### 1) Branch Misprediction
CPU가 Condition Code 결과를 잘못 예측하면 **pipeline flush** 발생.
결과: IPC 감소, latency 증가, throughput 저하.

### 2) Pipeline Stall
다음 branch 명령이 flags 갱신 완료까지 대기 → **execution stall** 발생 가능.

### 3) Tail Latency 증가
예측 불가능한 데이터 패턴 증가 시 branch predictor 실패 증가 → CPU pipeline 효율 저하.
대규모 서버에서는 **p99 / p999 latency** 악화 가능.

### 4) Overflow 처리 실패
OF/CF 무시 시:

- 금융 계산 오류
- 주소 계산 오류
- integer wraparound

가 발생 가능하다.

> **핵심:** Condition Code는 **CPU 성능과 제어 흐름 안정성의 핵심 축**이다.

---

## 6. 핵심 메커니즘

### A. ALU가 연산과 동시에 생성

```
7 - 7 = 0  →  Zero Condition 즉시 발생
```

### B. 상태 레지스터에 저장

생성된 Condition Code는 **Flags Register**에 latch된다.

### C. 다음 branch instruction이 참조

`JZ (Jump if Zero)` 는 실제로 `ZF == 1` 을 검사한다.

### D. Program Counter 변경

조건이 참이면 `PC ← target address` → **실행 흐름 자체가 바뀐다.**

### E. 대표 Condition Code

| Condition | 의미 |
|-----------|------|
| `Zero` | 결과가 0 |
| `Sign` | 음수 |
| `Overflow` | signed overflow |
| `Carry` | carry / borrow 발생 |

### F. Flags와의 관계

| 개념 | 의미 |
|------|------|
| Condition Code | 상태 판별 결과 (생성되는 신호) |
| CPU Flags | 상태 저장 비트 (저장하는 장소) |

### G. Branch Prediction과의 연결

현대 CPU는 Condition Code가 나오기 전에 **미리 다음 branch를 예측**한다.
예측이 틀리면 **pipeline 전체 flush**가 발생한다.

---

## 7. Linux / Runtime / K8s에서의 관측

Condition Code 자체는 직접 보이지 않는다. 대신 **간접적으로** 관측된다.

### perf (가장 중요)

```bash
perf stat    # branch-misses, stalled-cycles, IPC 요약
perf top     # 실시간 hot function 관측
```

관측 가능한 지표: `branch-misses`, `stalled-cycles`, IPC, speculative execution failure

### CPU Profiling

고성능 서버에서 **branch-heavy code**는 latency 증가의 원인이 될 수 있다.

### Kernel

다음 영역에서 매우 중요:

- scheduler
- interrupt handling
- syscall branch
- spinlock path

### 멀티코어

Condition Code 자체보다 **branch prediction failure**가 전체 CPU 효율에 큰 영향을 준다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*