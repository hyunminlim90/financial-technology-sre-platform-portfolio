# Hyper-thread와 Logical CPU의 관계

## 1. 개요

Hyper-thread와 Logical CPU는 서로 다른 계층에서 사용하는 표현이지만, 실제로는 **동일한 실행 컨텍스트(Hardware Context)를 서로 다른 관점에서 설명하는 개념**입니다.

```
Hardware 관점  →  Hyper-thread
OS 관점        →  Logical CPU
```

CPU 제조사가 하드웨어 수준에서 제공한 실행 컨텍스트를, 운영체제가 Logical CPU라는 이름으로 관리하는 구조입니다.

---

## 2. 계층별 관점 차이

### Hardware 관점

Hyper-Threading(SMT)이 활성화된 경우, 하나의 Physical Core 내부에 여러 개의 Hardware Context가 존재합니다. 각 Hardware Context는 다음 상태 정보를 독립적으로 가집니다.

- Program Counter
- Register Set
- Instruction State
- Interrupt State
- APIC ID

이 독립적인 실행 컨텍스트를 **Hyper-thread**라고 부릅니다.

### OS 관점

Linux Kernel은 CPU가 제공한 Hardware Context를 각각 독립적인 CPU 실행 단위로 인식하고 번호를 할당합니다.

```
cpu0, cpu1, cpu2, ...
```

이 운영체제 관점의 실행 단위를 **Logical CPU**라고 부릅니다.

### 계층별 비교

| 구분 | Hyper-thread | Logical CPU |
|------|-------------|-------------|
| 계층 | Hardware | OS Kernel |
| 의미 | 실행 컨텍스트 | 관리 단위 |
| 생성 주체 | CPU 제조사 | OS Kernel |
| 역할 | 물리 실행 상태 제공 | 스케줄링 대상 관리 |

---

## 3. Physical Core와의 관계

```
1 Physical Core
├── Hyper-thread 0  →  Logical CPU 0 (cpu0)
└── Hyper-thread 1  →  Logical CPU 1 (cpu1)
```

운영체제는 두 개의 Logical CPU를 독립적인 실행 단위처럼 다루지만, 실제 실행 자원은 동일 Physical Core 내부에서 공유됩니다.

운영체제가 Hyper-thread를 발견하면 반드시 하나의 Logical CPU 번호를 할당하므로, 운영 관점에서는 다음 관계가 성립합니다.

```
Hyper-thread 1개  ≒  Logical CPU 1개
```

---

## 4. 독립 요소와 공유 요소

| 구분 | 항목 |
|------|------|
| **독립** | Register Set, Program Counter, Interrupt State, APIC ID, 일부 Front-end 상태 |
| **공유** | ALU, FPU, Pipeline, Execution Unit, L1/L2 Cache, Branch Predictor 일부, Load/Store Unit 일부 |

---

## 5. HT ON / OFF 구조

### HT OFF

```
1 Physical Core = 1 Hardware Context = 1 Logical CPU

Physical Core : Hardware Context : Logical CPU = 1 : 1 : 1
```

Logical CPU는 Physical Core 전체 자원과 거의 동일한 의미를 가집니다.

### HT ON

```
1 Physical Core = 2 Hardware Context = 2 Logical CPU
```

실제 연산 장치는 공유되므로 다음 현상이 발생할 수 있습니다.

- Execution Unit Contention
- Cache Contention
- Pipeline Resource Competition
- Variable Latency

### HT ON / OFF 성능 특성 비교

| 항목 | HT ON | HT OFF |
|------|-------|--------|
| Throughput | 향상 가능 | 상대적으로 낮음 |
| Idle Execution Slot 활용 | 가능 | 불가 |
| I/O Bound 워크로드 | 유리 | 불리 |
| Latency 예측 가능성 | 낮음 (변동성 존재) | 높음 |
| Resource Contention | 발생 가능 | 감소 |
| 병렬 Hardware Context | 많음 | 적음 |

---

## 6. CFS와 Hardware Context

Linux CFS 스케줄러는 Logical CPU 단위로 Software Thread를 스케줄링합니다.

```
Software Thread → Kernel Thread → CFS Scheduler → Logical CPU → Hardware Context → Physical Core → Execution Units
```

CFS가 스케줄링하는 대상인 Logical CPU는 단순 번호가 아니라, 다음 구성 요소를 가진 Hardware Context와 연결된 커널 관리 단위입니다.

| 구성 요소 | 설명 |
|-----------|------|
| Register Set | 현재 실행 상태 저장 |
| Program Counter | 다음 명령어 위치 |
| APIC ID | CPU 식별 번호 |
| Runqueue | 실행 대기 Thread 목록 |
| Scheduling State | 실행 상태 정보 |

Kernel은 Hyper-thread 여부와 관계없이 각 Logical CPU에 대해 Runqueue와 Scheduling Context를 유지합니다.

---

## 7. Kubernetes 관점

Kubernetes는 Logical CPU 기준으로 CPU 자원을 계산합니다.

```yaml
resources:
  limits:
    cpu: "1"   # Logical CPU 1개 기준
```

HT ON 환경에서는 `cpu: "1"` 설정이 Physical Core 전체를 독점하는 것이 아닐 수 있습니다.

```
1 vCPU  ≠  1 Physical Core 전체 성능  (HT ON 환경)
```

---

## 8. SRE 관점

이 구조를 이해해야 다음 현상을 정확히 분석할 수 있습니다.

| 현상 | 원인 |
|------|------|
| CPU Throttling | Logical CPU 실행 시간 제한 |
| Cache Miss 증가 | SMT 내부 자원 경쟁 |
| Latency Variance | Sibling Thread 간 자원 경쟁 |
| Context Switch 증가 | Runnable Thread 과다 |
| Throughput 변화 | HT ON/OFF 차이 |
| CPU Saturation | Execution Unit 포화 |

---

## 9. 전체 구조

```
Physical Core
├── Hardware Context (Hyper-thread)
│       └── Logical CPU (OS 관리 단위)
│
└── Shared Execution Resources
        ├── ALU
        ├── FPU
        ├── Pipeline
        ├── Cache
        └── Execution Units
```

---

## 10. 핵심 정리

| 항목 | 의미 |
|------|------|
| Hyper-thread | Hardware가 제공한 실행 컨텍스트 |
| Logical CPU | OS Kernel이 관리하는 논리적 실행 단위 |
| 관계 | 동일 실행 컨텍스트를 서로 다른 계층에서 표현 |
| Hyper-Threading | 하나의 Physical Core에 여러 Hardware Context 제공 |
| CFS 스케줄링 대상 | Logical CPU |
| 실제 실행 자원 | ALU / FPU / Pipeline은 Hyper-thread 간 공유 |
| HT OFF | Physical Core : Hardware Context : Logical CPU = 1 : 1 : 1 |
| HT ON | 하나의 Core에 여러 Logical CPU 존재, 자원 공유 |

### 결론

```
Hyper-thread  ≒  Logical CPU
```

엄밀하게는 다음과 같이 구분됩니다.

```
Hyper-thread  =  Hardware가 제공한 실행 컨텍스트 (물리적 실행 상태 강조)
Logical CPU   =  OS Kernel이 관리하는 논리적 실행 단위 (스케줄링 관리 강조)
```

두 개념은 동일한 Hardware Context를 서로 다른 계층에서 바라보는 표현입니다.

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*