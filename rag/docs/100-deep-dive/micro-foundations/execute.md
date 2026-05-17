# Execute (실행)
## **Micro Foundations — 범용 컴퓨터 구조 / 시스템 관점**

> 정독: 0회

## 1. 이 기술이 무엇인가

**Execute**는:

> CPU가 해독된 명령을 **실제로 수행하는 단계**

명령어 사이클에서:

```
Fetch → 가져오기
Decode → 해석하기
Execute → 실제 행동
Write-back → 결과 저장
```

Execute는 **실제 연산이 발생하는 중심 단계**다.

쉽게 말하면:

- **Fetch와 Decode** → 준비
- **Execute** → 실제 작업 수행

**예시:** `ADD R1, R2` 라면 Execute 단계에서 ALU가 실제 덧셈 회로를 가동하고 결과 비트를 생성한다.

> **핵심:** Execute는 **명령어가 실제 물리 연산으로 변환되는 순간**이다.

---

## 2. 시스템 어디에서 등장하는가

Execute는 **CPU/GPU 내부 거의 모든 연산**에서 등장한다.

**대표 사례:** 산술 연산, 논리 연산, 주소 계산, branch 판단, 비교 연산, 메모리 주소 생성, vector/SIMD 연산, floating point 연산

**예시 — 모두 Execute 단계 필요:**

```
R1 + R2
R1 AND R2
if (x > y)
```

현대 CPU에서는 다음 execution unit들이 **병렬로** 존재한다:

| Execution Unit | 담당 |
|----------------|------|
| **Integer ALU** | 정수 연산 |
| **Floating Point Unit** | 부동소수점 연산 |
| **Vector Unit** | SIMD/벡터 연산 |
| **Branch Unit** | 분기 판단 |

> **결론:** 현대 CPU는 **여러 Execute 엔진을 동시에 가동**한다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

압도적으로 **CPU**이다.

### CPU 영향
- 특히 ALU, Execution Unit, Pipeline, Register File, Scheduler에 영향이 매우 크다

### Memory 영향 (간접)
- Execute 전에 데이터를 가져와야 하기 때문에 간접적으로 매우 큼
- cache miss, memory latency는 Execute 효율에 **직접 영향**을 준다

### Network/Disk 영향 (간접)
- 직접 영향은 적지만, packet parsing · encryption · compression · checksum은 결국 **CPU Execute 단계에서 수행**된다

> **핵심:** Execute는 **CPU 계산 자원이 실제 소비되는 순간**이다.

---

## 4. 왜 중요한가

**CPU 존재 이유 자체가 Execute**이기 때문이다. Fetch만 하고, 해석만 하고, 저장만 하면 아무 계산도 일어나지 않는다.

실제 시스템의 다음 모두 Execute 단계에서 수행된다:

- 암호화
- 압축
- 데이터베이스 계산
- 네트워크 처리
- AI 추론

현대 성능 핵심:

> **"얼마나 많은 Execute를 병렬로 효율적으로 처리하느냐"**

> **결론:** CPU 성능은 **Execute 처리량 경쟁**이다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 1) Pipeline Stall
```
데이터 준비 안 됨
  ↓
Execute Unit idle
```

### 2) Branch Misprediction
```
잘못 예측된 branch → 실행 중 pipeline flush
  ↓
엄청난 성능 손실
```

### 3) ALU Saturation
```
연산량 폭증
  ↓
CPU utilization 100% → latency 증가
```

### 4) Floating Point Bottleneck
```
부동소수점 연산 과다 (AI, scientific computing)
  ↓
FPU 포화 → throughput 저하
```

### 5) Thermal Throttling
```
Execute unit 과열
  ↓
CPU clock 감소
```

### 6) Context Switching
```
task 전환 많아짐
  ↓
Execute 흐름 자주 중단
```

### 7) Dependency Hazard
```
앞 연산 결과 대기
  ↓
execute delay → stall 증가
```

> **핵심:** Execute 병목은 **시스템 처리량 감소와 latency 증가**로 직결된다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

| 메커니즘 | 의미 |
|----------|------|
| **ALU** | 실제 정수 연산 수행 |
| **FPU** | 부동소수점 연산 |
| **Execution Unit** | 명령 종류별 실행 엔진 |
| **Pipeline Execution** | 여러 명령 동시 실행 |
| **Branch Execution** | 조건 판단 및 흐름 변경 |
| **Address Calculation** | 메모리 주소 계산 |
| **Forwarding** | 결과를 즉시 다음 실행으로 전달 |
| **Out-of-Order Execution** | 순서 바꿔 병렬 실행 |
| **Superscalar Execution** | 여러 Execute 동시 수행 |
| **Micro-ops** | 복잡 명령 분해 후 실행 |

> **핵심:** Execute는 **데이터 패스를 실제로 움직여 물리 연산을 발생시키는 단계**다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

직접 보이지 않지만 **결과는 관측 가능**하다.

### Linux

**perf**
```bash
perf stat
perf top
```
관찰: instructions, cycles, IPC, branch-misses, stalled-cycles

**CPU Utilization**
```bash
top
htop
mpstat
```

**Pipeline 문제**
```bash
perf record
perf report
```

**Thermal**
```bash
sensors
turbostat
```

---

### Runtime

관찰 포인트:
- IPC 저하
- branch miss 빈도
- stalled cycles 증가

---

### Kubernetes

| 관찰 영역 | 명령어 | 관찰 |
|-----------|--------|------|
| **CPU throttling** | `kubectl top pod` | Execute throughput 감소, latency spike, request backlog |

> **핵심:** 운영 환경에서는 **IPC·stall·branch miss·throttling** 형태로 관측된다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*