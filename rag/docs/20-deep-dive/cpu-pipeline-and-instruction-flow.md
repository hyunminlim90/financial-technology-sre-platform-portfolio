# CPU Pipeline(파이프라인)과 명령어 실행 구조

## 1. CPU Pipeline이란?

CPU Pipeline은 **명령어 실행 과정을 여러 단계(Stage)로 분할하여 동시에 처리하는 CPU 내부 실행 구조**입니다.

하나의 명령어를 완료한 후 다음 명령어를 처리하는 방식이 아니라, 서로 다른 단계에 여러 명령어를 동시에 배치하여 처리량(Throughput)을 증가시킵니다.

| 목적 | 설명 |
|---|---|
| **Throughput 향상** | 단위 시간당 처리 명령어 증가 |
| **CPU 자원 활용 극대화** | 유휴 실행 유닛 감소 |
| **IPC 증가** | 사이클당 명령 처리량 증가 |
| **병렬 처리** | 여러 명령어 동시 진행 |

---

## 2. Classic 5-Stage Pipeline 구조

전통적인 RISC CPU는 다음 5단계 Pipeline 구조를 사용합니다.

| 단계 | 이름 | 역할 |
|---|---|---|
| **IF** | Instruction Fetch | 명령어 읽기 |
| **ID** | Instruction Decode | 명령어 해석 |
| **EX** | Execute | 연산 수행 |
| **MEM** | Memory Access | 메모리 접근 |
| **WB** | Write Back | 결과 저장 |

### 단계별 상세

**IF — Instruction Fetch**

PC(Program Counter)를 기반으로 Instruction Cache 또는 메모리에서 다음 명령어를 가져옵니다. Branch Predictor가 다음 명령어 주소 결정에 관여합니다.

**ID — Instruction Decode**

읽어온 명령어를 해석하여 어떤 연산인지, 어떤 Register를 사용하는지, 메모리 접근이 필요한지 분석합니다.

**EX — Execute**

실제 연산이 수행됩니다.

| 실행 유닛 | 역할 |
|---|---|
| **ALU** | 정수 연산 |
| **FPU** | 부동소수점 연산 |
| **Branch Unit** | 분기 계산 |
| **Address Generator** | 주소 계산 |

**MEM — Memory Access**

Load/Store 명령은 LSU(Load/Store Unit)를 통해 처리됩니다. Cache Hit 여부에 따라 실행 속도가 크게 달라집니다.

**WB — Write Back**

최종 연산 결과를 Register에 기록합니다. 이후 다음 명령어가 해당 값을 사용할 수 있습니다.

---

## 3. Pipeline 병렬 처리 구조

Pipeline은 여러 명령어를 서로 다른 단계에서 동시에 처리합니다.

```text
Cycle 1:  IF
Cycle 2:  ID  | IF
Cycle 3:  EX  | ID  | IF
Cycle 4:  MEM | EX  | ID  | IF
Cycle 5:  WB  | MEM | EX  | ID  | IF
```

CPU는 각 사이클마다 새로운 명령어를 투입할 수 있습니다.

---

## 4. IPC (Instructions Per Cycle)

Pipeline 성능의 핵심 지표입니다.

```text
IPC = 사이클당 처리된 명령어 수
```

높은 IPC는 CPU 자원 활용률이 높다는 의미입니다.

---

## 5. Pipeline Hazard

Pipeline의 실행 흐름을 방해하는 요소를 **Hazard**라고 합니다.

| Hazard 종류 | 설명 |
|---|---|
| **Data Hazard** | 데이터 의존성으로 인한 대기 |
| **Control Hazard** | 분기 예측 실패로 인한 경로 불확실 |
| **Structural Hazard** | 하드웨어 자원 충돌 |

### 5-1. Data Hazard

앞선 명령어의 결과가 아직 준비되지 않았는데 뒤 명령어가 해당 값을 필요로 하는 상황입니다.

```text
Instruction 1: R1 = R2 + R3
Instruction 2: R4 = R1 + R5   ← R1 결과가 아직 없음

→ Result Not Ready → Pipeline Stall
```

**Data Forwarding:** 현대 CPU는 연산 결과를 Register Write 이전에 직접 다음 단계로 전달하여 Stall을 줄입니다.

```text
EX Result → Direct Forward → Next Instruction
```

### 5-2. Control Hazard

분기 명령으로 인해 다음 실행 경로가 불확실한 상황입니다.

```text
if (x > 0)
→ 실제 결과가 나오기 전까지 다음 명령어 확정 불가
```

현대 CPU는 **Branch Predictor**를 사용하여 Pipeline 중단을 최소화합니다.  
예측 실패 시:

```text
Wrong Prediction
→ Pipeline Flush
→ Re-fetch
→ Restart
```

깊은 Pipeline 구조에서는 Flush 비용이 매우 커집니다.

### 5-3. Structural Hazard

여러 명령어가 동시에 동일 하드웨어 자원을 요구하는 상황입니다.

```text
Instruction A → Memory Access
Instruction B → Memory Access
→ 동일 LSU / 메모리 포트를 동시에 사용할 수 없음
```

---

## 6. Deep Pipeline

현대 CPU는 20~30 Stage 이상의 긴 Pipeline 구조를 사용합니다.

| 구분 | 내용 |
|---|---|
| **장점** | 높은 Clock Frequency 달성, 세분화된 병렬 처리 |
| **단점** | Branch Penalty 증가, Stall 영향 증가, 설계 복잡성 증가 |

---

## 7. 현대 CPU 고급 기술

### Superscalar Architecture

여러 Pipeline을 동시에 운영하여 여러 명령어를 병렬로 실행합니다.

```text
Pipeline A
Pipeline B
Pipeline C
Pipeline D
```

### Out-of-Order Execution

CPU가 명령어 순서를 일부 재배치하여 Stall을 줄이고 실행 유닛 활용률을 높입니다.

```text
Instruction A Stall
→ Independent Instruction B 먼저 실행
```

---

## 8. Pipeline과 Cache / LSU 관계

Pipeline의 MEM 단계는 LSU와 직접 연결됩니다.

```text
Pipeline MEM Stage → LSU → Cache / RAM Access
```

| 상태 | 영향 |
|---|---|
| **Cache Hit** | 빠른 메모리 접근 → Pipeline 유지 |
| **Cache Miss** | RAM 접근 지연 → Pipeline Stall |

---

## 9. CPU 성능 결정 요소

```text
Clock Frequency × IPC × Pipeline Efficiency = CPU 성능
```

Pipeline 효율이 낮으면 높은 GHz에서도 성능이 저하될 수 있습니다.

---

## 10. JVM 및 서버 애플리케이션 관점

| 영역 | Pipeline 영향 |
|---|---|
| **복잡한 조건문** | Branch Prediction 실패 → Pipeline Flush 증가 |
| **비효율적 객체 접근** | LSU Stall 유발 |
| **Lock 경합** | 동기화 비용 → Pipeline 진행 방해 |

---

## 11. Pipeline 최적화 전략

| 전략 | 목적 |
|---|---|
| **Branch 단순화** | Control Hazard 감소 |
| **Cache 친화적 데이터 구조** | Memory Stall 감소 |
| **연속 메모리 사용** | Cache Hit 증가 |
| **Lock 최소화** | Stall 감소 |
| **Hot Path 최적화** | IPC 향상 |
| **Branchless 코드** | Prediction Miss 감소 |

---

## 12. CPU 내부 구성 요소 연결 구조

```text
Instruction Fetch
        ↓
Branch Predictor
        ↓
Pipeline (IF → ID → EX → MEM → WB)
        ↓
ALU / FPU 실행
        ↓
LSU 메모리 접근
        ↓
Register Write Back
```

---

## 13. 전체 개념 정리

| 구성 요소 | 역할 |
|---|---|
| **Pipeline** | 명령어 병렬 실행 구조 |
| **IF / ID / EX / MEM / WB** | 5단계 실행 단계 |
| **Stall** | Pipeline 중단 |
| **Hazard** | 실행 충돌 요소 |
| **IPC** | 사이클당 명령 처리량 |
| **Superscalar** | 다중 Pipeline 구조 |
| **Out-of-Order** | 실행 순서 재배치 |
| **Data Forwarding** | Stall 감소를 위한 결과 직접 전달 |

---

## 핵심 결론

현대 CPU는 단순 순차 실행 장치가 아니라 **다단계 병렬 실행 구조를 기반으로 동작하는 고성능 처리 시스템**입니다.

```text
Branch Prediction Accuracy
  + Cache Hit Ratio
  + Memory Access Efficiency
  + Low Stall Rate
  = High IPC
  = High CPU Performance
```

이는 대규모 트래픽 처리 / JVM 기반 서버 / Kubernetes 환경 / 고성능 네트워크 처리 / 금융 시스템의 Latency 및 처리량에 직접적인 영향을 미칩니다.

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*