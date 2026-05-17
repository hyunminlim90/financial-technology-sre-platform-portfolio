# Status Register (상태 레지스터 / CPU Flags)

## Micro Foundations — ALU 결과 상태 / 분기 제어 / CPU 실행 흐름 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

상태 레지스터(Status Register)는:

> **직전 ALU 연산 결과의 상태를 1비트 단위 플래그로 저장하는 특수 목적 레지스터**

핵심은 **"CPU가 방금 무슨 상태를 만들었는가"** 를 기록하는 것이다.

| Flag | 의미 |
|------|------|
| `ZF` | 결과가 0 |
| `SF` | 결과가 음수 |
| `OF` | signed overflow |
| `CF` | unsigned carry |

> **상태 레지스터 = CPU의 현재 상태 원장**

---

## 2. 시스템 어디에서 등장하는가

매우 광범위하게 등장한다.

### CPU 내부
branch execution, compare instruction, arithmetic result tracking

### Kernel
interrupt enable / disable, syscall return condition, privilege mode state

### Network Stack
checksum calculation branch, packet validation path, fast-path / slow-path 분기

### Storage
error condition, parity check, DMA completion state

> **조건 분기와 상태 판단이 있는 모든 곳**에서 간접적으로 등장한다.

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **CPU**이다.

특히 다음과 매우 강하게 연결된다:

- ALU
- branch unit
- pipeline control
- program counter (PC)

상태 레지스터가 **다음 실행 경로를 결정**하기 때문이다.

---

## 4. 왜 중요한가

CPU는 단순 계산기가 아니라 **"조건에 따라 흐름을 바꾸는 장치"** 이다.

예시: `if (a == b)` 는 최종적으로 다음과 같이 동작한다:

```
1. a - b 수행
2. ZF 확인
3. branch 여부 결정
```

> **모든 조건문은 결국 상태 레지스터 검사**이다.

---

## 5. 실제 장애와의 관련성

### 1) Branch Misprediction
상태 플래그 결과 예측 실패 시 → **pipeline flush, CPU stall, latency spike** 발생 가능.

### 2) Interrupt Flag 문제
IF(Interrupt Flag) 잘못 제어 시 → **interrupt storm, system freeze, deadlock** 가능.

### 3) Overflow 처리 실패
OF 관리 실패 시 → **signed integer corruption, 금융 계산 오류, checksum mismatch** 가능.

### 4) Carry Flag 처리 오류
암호화 / 멀티워드 연산에서 carry propagation 실패 → **arithmetic corruption** 발생 가능.

> **핵심:** 상태 레지스터는 **CPU 제어 안정성의 핵심**이다.

---

## 6. 핵심 메커니즘

### A. ALU 연산 후 즉시 갱신

```
ALU 연산 → 상태 신호 생성 → 상태 레지스터 비트 갱신
```

예시: `5 - 5 = 0` 이면 `ZF = 1 (Set)` 된다.

### B. 상태 레지스터는 "비트 필드"

일반 숫자 저장 레지스터가 아니라 **독립된 플래그 비트 집합**이다.

| Bit | 의미 |
|-----|------|
| bit 0 | `CF` |
| bit 6 | `ZF` |
| bit 7 | `SF` |
| bit 11 | `OF` |

### C. 조건 분기의 핵심 입력

```
CMP 수행 → ZF 갱신 → JZ(Jump if Zero)
```

**상태 레지스터가 프로그램 카운터(PC) 흐름을 결정**한다.

### D. Set / Clear 구조

| 상태 | 의미 |
|------|------|
| Set (1) | 활성 |
| Clear (0) | 비활성 |

`ZF = 1` → 결과가 0 / `ZF = 0` → 결과가 0이 아님

### E. Pipeline과의 연결

현대 CPU는 상태 플래그 결과가 나오기 전에 **다음 명령어를 미리 실행**하려 한다.
실제 flag 결과가 다르면 **pipeline flush**가 발생한다.

> **상태 레지스터는 분기 예측 성능의 핵심**이다.

### F. Control Flag의 존재

상태 레지스터는 단순 결과 저장 외에 **CPU 실행 모드 자체도 관리**한다.

| Flag | 역할 |
|------|------|
| `IF` | interrupt enable |
| `DF` | string direction |
| `TF` | debug trap |

---

## 7. Linux / Runtime / K8s에서의 관측

직접 보이지 않지만 간접적으로 어디서나 영향을 준다.

### Linux Kernel

interrupt enable / disable, scheduler branch path, syscall fast-path

### CPU 분석 도구

```bash
perf stat    # branch-misses, branch-instructions, stalled-cycles 측정
```

### eBPF / perf tracing

조건 분기 실패 증가 시 → **IPC 감소, CPU pipeline stall 증가** 관측 가능.

### Virtualization

guest flag state save / restore, VM exit condition, interrupt injection

### Networking

checksum validation branch, packet filter path, TCP state transition

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*