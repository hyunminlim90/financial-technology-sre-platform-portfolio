# Parallel Processing (병렬 처리)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**Parallel Processing(병렬 처리)** 는 하나의 작업을 여러 단위로 분할한 뒤 **여러 연산 장치가 동시에 처리하는 실행 방식**이다.

### 핵심 목표

```
Latency 감소  +  Throughput 증가
```

> **시간 문제를 추가 연산 자원(Core / Thread / Node)으로 해결하는 구조**

---

## 2. 시스템 어디에서 등장하는가

병렬 처리는 현대 시스템 거의 **모든 계층**에서 사용된다.

### CPU

```
Multi-core  /  SMT / Hyper-Threading  /  SIMD
```

### GPU

```
SIMT  /  Massive Parallel Compute
```

### OS Kernel

```
Multi Queue NIC  /  blk-mq  /  Parallel Scheduler
```

### JVM / Runtime

```
ForkJoinPool  /  Virtual Thread  /  Parallel GC
```

### Framework

```
Reactor  /  Netty Event Loop  /  Kafka Consumer Parallelism
```

### Distributed System

```
Kubernetes  /  Spark  /  Distributed Queue  /  Sharded Database
```

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 영향이 큰 자원: **CPU + Memory Bandwidth**

병렬성이 증가할수록 다음 항목의 영향도 증가한다.

- **Context Switch**
- **Cache Coherency**
- **NUMA Traffic**
- **Lock Contention**

| 자원 | 영향 |
|------|------|
| CPU | 매우 큼 |
| Memory | Cache / NUMA 중요 |
| Network | Distributed Parallelism 시 중요 |
| Disk | Parallel I/O 증가 |

---

## 4. 왜 중요한가

### FinTech 시스템의 핵심 처리 요구사항

- **Burst Traffic** — 순간 폭증 트래픽 처리
- **실시간 승인** — 수백 ms 이내 응답
- **대량 정산** — 수천만 건 배치 처리
- **AI 기반 FDS** — 실시간 병렬 Inference

### 직렬 처리만 사용할 경우

```
직렬 처리 한계
 → 처리량 상한선 빠르게 도달
 → Latency 증가
 → TPS 정체
```

> **병렬 처리는 동시성 확장의 핵심 메커니즘**

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. Lock Contention

병렬 Worker 증가 시 Lock 경합 증가.

| Lock 종류 | 사용처 |
|-----------|--------|
| synchronized | Java 동기화 블록 |
| mutex | POSIX 스레드 |
| futex | Linux 커널 기반 Lock |

```
병렬 Worker 증가
 → Lock 경합 증가
 → 직렬 구간 병목 발생
 → 병렬 효과 상쇄
```

---

### 5-2. CPU Saturation

병렬 Worker 과도 증가 시 Scheduler 부하 급증.

```
Worker 수 과다
 → Run Queue 폭증
 → Context Switch 증가
 → Scheduler Delay
 → Throughput 감소
```

---

### 5-3. Cache Thrashing

멀티 코어 간 Cache Line 공유 증가 시 성능 급락.

```
다수 Core가 동일 Cache Line 접근
 → Cache Invalidation 반복
 → Cache Thrashing
 → Memory Access 급증
```

---

### 5-4. NUMA Penalty

병렬 Worker가 Remote Memory / 다른 CPU Socket에 접근 시 지연 증가.

```
Worker (Socket 1)  →  Memory (Socket 0)
 → UPI / Infinity Fabric Cross Traffic 증가
 → Access Latency 증가
 → P99 악화
```

---

### 5-5. Tail Latency 증가

병렬 작업 중 일부 느린 Worker로 인한 전체 지연.

```
Worker A: 10ms
Worker B: 10ms
Worker C: 500ms  ← 지연
 → 전체 응답 = Worker C 완료까지 대기
 → P99 급증
```

---

### 5-6. Retry Storm

분산 병렬 처리에서 일부 Node 지연 시 발생.

```
Node 지연
 → Timeout
 → Retry 증가
 → 부하 집중
 → Cascading Failure
```

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Task Parallelism

서로 다른 작업을 병렬 수행.

```
Network Thread  ‖  DB Thread  ‖  Encryption Thread
 (각각 독립 실행)
```

---

### Data Parallelism

동일 작업을 여러 데이터에 병렬 적용. GPU가 대표적.

```
100만 거래 데이터
 → Worker 1: 1~25만 건
 → Worker 2: 26~50만 건
 → Worker 3: 51~75만 건
 → Worker 4: 76~100만 건
 (동시 처리)
```

---

### Multi-core Execution

병렬 처리의 물리적 기반. 여러 CPU Core를 동시에 사용.

---

### Context Switch

Worker 수 증가 시 Scheduler Overhead 증가 가능. 병렬 Worker 수는 적정 수준 유지 필요.

---

### Cache Coherency

멀티 코어는 동일 메모리 데이터 동기화 비용 발생. **MESI Protocol** 기반으로 관리됨.

| MESI 상태 | 의미 |
|-----------|------|
| Modified | 해당 Core만 최신 데이터 보유 |
| Exclusive | 하나의 Core만 캐시 보유 |
| Shared | 여러 Core가 동일 캐시 공유 |
| Invalid | 캐시 무효 — 메모리 재접근 필요 |

---

### NUMA

멀티 소켓 시스템에서 **Memory Locality**가 성능에 직결. Worker와 Memory를 동일 Socket에 배치하는 것이 중요.

---

### Queue Parallelism

Kafka / RabbitMQ 등에서 **Partition + Consumer Group** 구조로 병렬 처리.

```
Topic
 ├── Partition 0  →  Consumer A
 ├── Partition 1  →  Consumer B
 └── Partition 2  →  Consumer C
```

---

### Amdahl's Law

병렬 처리의 핵심 제약.

```
직렬 구간 비율이 클수록
 → 병렬화 효과 한계 존재
 → 코어 무한 증가해도 성능 상한 존재
```

---

### Backpressure

병렬 처리 속도가 다운스트림보다 빠를 때 발생.

```
생산 속도 > 소비 속도
 → Queue 증가
 → Memory 증가
 → OOM 위험
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### CPU 사용률 확인

```bash
mpstat -P ALL 1
```

### Run Queue 확인

```bash
vmstat 1
```

### Thread 상태 확인

```bash
top -H
```

### Context Switch 확인

```bash
pidstat -w
```

### NUMA 상태 확인

```bash
numactl --hardware
```

### CPU Affinity 확인

```bash
taskset -cp <pid>
```

### Scheduler 분석

```bash
perf sched
```

### JVM Thread Dump

```bash
jstack <pid>
```

### Kubernetes Resource 확인

```bash
kubectl top pod
```

### PSI (Pressure Stall Information) 확인

```bash
cat /proc/pressure/cpu
```

---

## 요약

```
Parallel Processing
 ├── Task Parallelism     → 서로 다른 작업 동시 실행
 ├── Data Parallelism     → 동일 작업 × 대량 데이터 동시 처리
 ├── Cache Coherency      → MESI Protocol — 멀티 코어 데이터 동기화 비용
 ├── NUMA                 → Memory Locality — Socket 간 접근 지연 최소화
 ├── Queue Parallelism    → Partition + Consumer Group 병렬 처리
 ├── Amdahl's Law         → 직렬 구간 비율이 병렬 확장 한계 결정
 └── Backpressure         → 생산 > 소비 → Queue 누적 → OOM 위험
```

> FinTech 결제 시스템에서 병렬 처리는 단순히 빠르게 하는 기술이 아니라,  
> **Burst Traffic · 실시간 FDS · 대량 정산을 동시에 처리하기 위한 동시성 확장의 핵심 설계 원칙**이다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*