# 로직 (Logic)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**로직(Logic)** 은:

> 데이터를 어떤 조건과 규칙으로 처리할지 결정하는 **판단 흐름**

즉, 무엇을 검사하고 / 어떤 조건에서 / 어떤 행동을 수행할지 정의한 **실행 규칙**이다.

### 가장 기본 형태

```
IF condition THEN action
```

예시:
```
IF balance >= payment_amount
THEN approve_payment
```

컴퓨터 시스템 대부분은 **산술 연산 + 논리 연산 + 조건 분기** 조합으로 로직을 구성한다.

<details>
<summary>Deep Dive</summary></br>

Data(데이터) [[M]](../../100-deep-dive/micro-foundations/data.md)

</details></br>

## 2. 시스템 어디에서 등장하는가

로직은 **시스템 전체**에 존재한다.

### Hardware Layer
- ALU Logic Gate
- Branch Logic
- Pipeline Control

### OS Kernel
- Scheduler Decision
- TCP Retransmission
- Memory Reclaim Logic

### Runtime
- JVM GC Logic
- JIT Optimization Logic
- Event Loop Logic

### Middleware
- Retry Logic
- Circuit Breaker
- Connection Pool Logic

### Application
- 결제 승인
- Fraud Detection
- 정산
- 인증/인가

### Distributed System
- Consensus Logic
- Failover Logic
- Leader Election

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

로직은 **CPU** 영향이 가장 크다.

> 로직은 결국 조건 판단, 분기, 상태 변경, 데이터 처리 흐름을 CPU가 수행하기 때문.

### 추가 영향

| 자원 | 영향 |
|------|------|
| CPU | Branch/Execution |
| Memory | Object State |
| Network | Retry/Replication |
| Disk | Transaction Commit |

### 복잡한 로직의 특징

복잡한 조건문 증가 시 아래가 증가할 수 있다:
- Branch Misprediction
- Cache Miss
- Pipeline Stall

---

## 4. 왜 중요한가

로직은 **시스템의 실제 행동 자체를 결정**한다.

### FinTech에서 특히 중요한 이유

결제 시스템의 정합성, 순서 보장, 장애 복구, 중복 방지 모두 **로직 기반**이다.

잘못된 로직은 아래를 유발할 수 있다:

> ⚠️ 중복 결제 / Ledger Corruption / Retry Storm / Race Condition

---

## 5. 실제 장애와 어떤 관련이 있는가

### Infinite Retry Logic
재시도 로직 오류 시 Retry Storm → DB Saturation → Cascading Failure 발생 가능

### Race Condition
동시성 로직 문제 시 Double Spending / Duplicate Payment 발생 가능

### Incorrect Validation Logic
검증 로직 오류 시 Fraud Bypass / Invalid Transaction Approval 가능

### Blocking Logic
동기식 로직 과다 시:
- Event Loop Blocking
- Thread Pool Exhaustion 발생 가능

### Deep Branching
복잡한 분기 증가 시 CPU Branch Misprediction → IPC 감소 발생 가능

### Rollback Logic Failure
예외 처리 로직 오류 시 Distributed Inconsistency / Partial Commit 발생 가능

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

### Condition Branch
대표: `IF / ELSE`. CPU 분기 처리 핵심.

### State Transition
결제 상태 변화: `PENDING → APPROVED → SETTLED`

### Validation Logic
입력 검증: Hash Check, Signature Validation, Authorization

### Exception Logic
오류 처리: Retry, Rollback, Fallback

### Branch Prediction
CPU는 다음 분기를 미리 예측함. 복잡한 로직은 예측 실패 증가.

### Synchronization Logic
멀티스레드 환경에서 `Lock`, `CAS`, `Atomic Operation` 필요 가능

### Workflow Logic
분산 환경에서 Saga, Orchestration, Compensation 등 존재

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### CPU Branch/Execution
```bash
perf stat
```
대표 지표: `branch-misses`, `instructions`, `cycles`

### Hot Logic Function
```bash
perf top
async-profiler
```

### JVM
```bash
jfr
jstack
```

### Lock Contention
```bash
perf lock
```

### Event Loop Blocking
Netty/Reactor에서 아래 지표 관측:
- `event loop delay`
- `blocked thread`

### Kubernetes
```bash
kubectl top pod
```

### Application Metrics
대표 지표: `P95/P99 Latency`, `Error Rate`, `Retry Count`, `Timeout Count`

### eBPF
```bash
# bcc-tools 대표 도구
offcputime
runqlat
profile
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*