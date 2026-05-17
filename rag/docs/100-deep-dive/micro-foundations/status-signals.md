# Status Signals (상태 신호)

## Micro Foundations — ALU 출력 / 전기 신호 / Flags 생성 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

Status Signals는:

> **ALU 연산 직후 하드웨어 회로가 즉시 생성하는 전기적 상태 출력 신호**

중요한 점은 **Condition Code보다 더 아래 물리 레벨 개념**이라는 것이다.

```
ALU 연산 → Status Signals 발생 → Flags Register 저장 → Condition Code 의미 형성
```

> **핵심:** Status Signals는 **실제 실리콘 회로와 전선에서 흐르는 전압 신호 자체**이다.

---

## 2. 시스템 어디에서 등장하는가

가장 직접적으로는 **ALU 출력부(Output Logic)** 에서 등장한다.

특히 다음 조합 논리 회로 주변:

- adder
- comparator
- XOR logic
- NOR logic
- carry chain

| 위치 | 역할 |
|------|------|
| ALU 결과 출력 | 결과 데이터 생성 |
| 상태 판별 회로 | Zero / Carry / Overflow 판별 |
| 상태 신호선 | High / Low 전압 전달 |
| Flags Register 입력부 | 상태 저장 |

즉 **데이터 버스와 별개인 독립 상태 신호선 레이어**가 존재한다.

---

## 3. 어떤 자원에 가장 영향이 큰가

거의 전부 **CPU timing path**에 영향을 준다.

특히 다음과 직접 연결된다:

- propagation delay
- critical path
- clock frequency
- pipeline timing

Memory/Disk보다 **CPU 전기 신호 전달 속도**에 훨씬 중요하다.

> **핵심:** Status Signals는 **CPU 클록 한계와 파이프라인 안정성을 결정하는 핵심 전기 신호**이다.

---

## 4. 왜 중요한가

CPU는 **연산 결과 상태를 즉시 알아야 다음 실행 흐름을 결정**할 수 있다.

예시: `5 - 5 = 0` 에서 ALU는 결과 데이터뿐 아니라 **Zero 상태 신호**를 동시에 생성한다.

이 신호가 없으면 CPU는 다음이 불가능하다:

- 조건문 분기
- loop 제어
- compare / jump
- interrupt 판단

> **Status Signals는 CPU 제어 흐름의 가장 원초적 물리 신호**이다.

---

## 5. 실제 장애와의 관련성

### 1) Propagation Delay 문제
상태 신호가 레지스터 도착 전에 다음 클록이 오면 잘못된 값이 latch될 수 있다.
결과: **timing violation, metastability, pipeline corruption** 발생 가능.

### 2) Critical Path 증가
상태 신호 판별 회로가 길어질수록 **CPU 최대 클록이 감소**할 수 있다.
상태 신호 생성 경로는 **CPU 설계의 핵심 임계 경로**이다.

### 3) Branch Prediction 실패
상태 신호 확정 전까지 실제 branch 결과가 미확정 상태.
예측 실패 시 **pipeline flush** 발생.

### 4) High Frequency CPU 설계 문제
클록이 높아질수록 **상태 신호 전달 시간 마진이 감소**한다. 따라서 다음이 매우 중요하다:

- wire optimization
- pipeline stage 분할
- logic depth 축소

---

## 6. 핵심 메커니즘

### A. 상태 신호는 "데이터"가 아니다

Status Signals는 연산 결과 데이터 자체가 아니라 **연산 결과의 상태를 의미하는 전기 신호**이다.

| 종류 | 의미 |
|------|------|
| 결과 데이터 | 64비트 계산 결과 |
| 상태 신호 | zero / carry / overflow 여부 |

### B. ALU와 동시에 생성

ALU가 결과를 출력하는 순간 다음 신호들이 **동시에 생성**된다:

- Zero signal
- Carry signal
- Overflow signal
- Sign signal

연산 후 따로 계산하는 것이 아니라 **회로가 병렬로 즉시 생성**한다.

### C. 상태 신호 → Flags Register

상태 신호는 **상태 레지스터 입력선**으로 전달된다.
클록 에지 시 **latch**되어 CPU Flags가 된다.

### D. Condition Code와의 차이

| 개념 | 본질 |
|------|------|
| Status Signals | 실제 전압 신호 |
| Condition Code | 그 신호의 논리적 의미 |
| CPU Flags | 저장된 상태 비트 |

> **Status Signals는 Condition Code의 물리적 실체**이다.

### E. 데이터 버스와 분리됨

상태 신호는 **별도 control/status line**을 사용한다.
데이터 흐름과 상태 흐름은 **하드웨어적으로 역할이 다르기 때문**이다.

---

## 7. Linux / Runtime / K8s에서의 관측

Status Signals는 **완전히 하드웨어 내부 레벨**이라 직접 관측되지 않는다.
하지만 간접 영향은 매우 크다.

### perf

```bash
perf stat    # branch-misses, stalled-cycles, IPC 감소 형태로 나타남
```

### CPU 설계 / RTL / VLSI

실제 관측 영역:

- timing analysis
- critical path analysis
- gate-level simulation
- Verilog / VHDL waveform

### Kernel

간접 영향이 나타나는 영역:

- branch-heavy scheduler path
- interrupt handling
- spinlock contention

### 고성능 서버

상태 신호 관련 최적화는 **CPU 클록과 파이프라인 안정성**의 핵심 요소다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*