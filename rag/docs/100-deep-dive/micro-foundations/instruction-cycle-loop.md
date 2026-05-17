# Instruction Cycle Loop (명령어 사이클 루프)
## Micro Foundations — CPU / 시스템 실행 / 성능 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

> Instruction Cycle Loop는 **CPU가 명령어를 계속 읽고 실행하는 하드웨어 영구 실행 루프**

즉 CPU는 전원이 켜진 순간부터 다음 과정을 끝없이 반복한다:

```
Fetch → Decode → Execute → Write-back(Store)
```

**핵심:**
컴퓨터는 사실상 이 루프 하나로 움직인다.
운영체제, 데이터베이스, Kafka, Kubernetes도 결국 **CPU의 명령어 사이클 루프 위에서만 존재 가능**하다.

---

## 2. 시스템 어디에서 등장하는가

### CPU Core
각 코어는 독립적으로 **자기만의 instruction cycle loop**를 가진다.

### 운영체제
OS scheduler는 어떤 프로세스가 **다음 instruction cycle을 사용할지** 결정한다.

### 멀티스레드 서버
웹 서버, DB, Kafka broker 전부 **CPU instruction cycle time을 서로 경쟁**한다.

### GPU
GPU도 유사한 **SIMT execution loop** 구조를 가진다.

> Instruction Cycle Loop는 모든 소프트웨어 실행의 물리적 기반이다.

---

## 3. 어떤 자원에 가장 영향이 큰가

압도적으로 **CPU**이다. 하지만 실제로는 **Memory latency 영향이 매우 크다.**

CPU는 빠른데 RAM, cache miss, disk I/O는 느리기 때문이다.
즉 instruction cycle은 **대부분 memory waiting 때문에 느려진다.**

특히 영향이 큰 요소:

- cache miss
- branch miss
- context switch
- pipeline stall
- store buffer full

> **핵심:** Instruction cycle 성능은 CPU 자체보다 **memory hierarchy 영향을 더 많이 받는다.**

---

## 4. 왜 중요한가

CPU 성능의 본질 자체이기 때문이다.

모든 처리량은 결국 **얼마나 많은 instruction cycle을 낭비 없이 돌릴 수 있나**로 귀결된다.

| 영역 | Instruction Cycle 관련성 |
|------|--------------------------|
| API 처리량 | instruction cycle 소비 결과 |
| Kafka throughput | instruction cycle 소비 결과 |
| DB TPS | instruction cycle 소비 결과 |
| packet forwarding | instruction cycle 소비 결과 |
| encryption | instruction cycle 소비 결과 |

고성능 시스템은 결국 **instruction cycle 낭비 최소화** 게임이다:

- cache hit 증가
- branch miss 감소
- stall 제거
- context switch 감소

전부 이 루프 보호 목적이다.

---

## 5. 실제 장애와의 관련성

### 1) Pipeline Stall
메모리 응답이 늦어지면 → **instruction cycle 정지**

### 2) Cache Miss Storm
L1/L2 miss 증가 → RAM wait 증가 → **IPC 급감, latency 폭증**

### 3) Context Switching 폭증
OS가 프로세스를 계속 교체 → instruction cycle 상당 부분이 **실제 작업 대신 상태 저장/복구에 사용**

### 4) Branch Misprediction
CPU가 잘못 예측 → **pipeline flush** 발생

### 5) CPU Throttling
Kubernetes/container quota 제한 → **instruction cycle 자체 중단**

### 6) Interrupt Storm
IRQ 과다 발생 → **정상 루프 지속 붕괴**

> **핵심:** 대부분의 성능 문제는 instruction cycle이 **끊기거나 대기**하면서 발생한다.

---

## 6. 핵심 메커니즘

### 기본 루프

```
Fetch → Decode → Execute → Write-back(Store) → 다음 Fetch → (무한 반복)
```

| 단계 | 설명 |
|------|------|
| **Fetch** | RAM/cache에서 명령어 가져옴 |
| **Decode** | 무슨 명령인지 해석 |
| **Execute** | ALU/FPU/SIMD 등 실제 연산 수행 |
| **Write-back** | 결과를 register/cache/RAM에 반영 |

### Program Counter (PC)
다음 명령 주소를 가리키며, **PC가 계속 다음 명령어를 가리키기 때문에 루프가 영구 지속**된다.

### Pipeline
현대 CPU는 **여러 instruction cycle을 겹쳐 실행**한다.

```
Instruction A: [Fetch] [Decode] [Execute] [Write-back]
Instruction B:         [Fetch]  [Decode]  [Execute]   [Write-back]
Instruction C:                  [Fetch]   [Decode]    [Execute]   [Write-back]
```

### Stall
어딘가 막히면 **전체 pipeline이 대기** 상태가 된다.

> **핵심:** 현대 CPU는 instruction cycle을 끊김 없이 유지하는 것이 핵심 목표다.

---

## 7. Linux / Runtime / K8s에서의 관측

직접 instruction cycle 자체를 보진 못하지만, **stall과 wait 형태로 간접 관측**이 가능하다.

### Linux 도구

| 도구 | 용도 |
|------|------|
| `top` | CPU 사용률 전반 |
| `vmstat` | 메모리 및 CPU 상태 |
| `mpstat` | 코어별 CPU 통계 |
| `perf` | 저수준 CPU 이벤트 관측 |
| `sar` | 시스템 활동 리포트 |

### perf (가장 중요)

```bash
perf stat    # IPC, cache miss, branch miss 등 요약
perf top     # 실시간 hot function 관측
perf record  # 샘플링 기록
```

관측 가능한 지표: IPC, cache miss, branch miss, stalled cycles

### Kubernetes

증상 형태로 관측:

- CPU throttling
- pod latency 증가
- run queue 증가

### Scheduler

```bash
pidstat -w   # context switch 관측
```

### NUMA

원격 메모리 access로 **instruction cycle stall 증가** 가능.

> **핵심:** 운영 환경에서는 CPU 사용률보다 **stall cycle과 wait이 더 중요**할 때가 많다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*