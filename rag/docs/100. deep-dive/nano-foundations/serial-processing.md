# Serial Processing (직렬 처리)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**Serial Processing(직렬 처리)** 는 작업을 동시에 수행하지 않고 **정해진 순서대로 하나씩 처리하는 실행 방식**이다.

### 핵심 특징

- **Sequential Execution** — 순차 실행
- **Ordered Processing** — 순서 보장
- **Single Execution Flow** — 단일 실행 흐름
- **Dependency Preservation** — 작업 의존성 유지

```
앞 작업 완료  →  다음 작업 실행
```

---

## 2. 시스템 어디에서 등장하는가

직렬 처리는 시스템 거의 **모든 계층**에 존재한다.

### CPU 내부

```
Instruction Pipeline  /  Branch Execution  /  Dependency Resolution
```

### OS Kernel

```
특정 Kernel Lock 구간  /  Scheduler Queue 처리  /  Filesystem Metadata Update
```

### Runtime / JVM

```
Safepoint 진입  /  GC 일부 단계  /  Class Loading
```

### Application

```
결제 원장 (Ledger)  /  트랜잭션 순서 보장  /  Ordered Event Processing
```

### Distributed System

```
Kafka Partition Ordered Consume  /  WAL (Write Ahead Log)  /  Replication Ordering
```

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 영향이 큰 자원: **CPU + Scheduler**

직렬 처리 특징상 **동시성 제한**이 핵심 문제.

| 자원 | 영향 |
|------|------|
| CPU | 단일 코어 병목 |
| Scheduler | Run Queue 증가 |
| Memory | 상대적으로 낮음 |
| Network | Ordered Queue 영향 |
| Disk | fsync 직렬화 가능 |

---

## 4. 왜 중요한가

직렬 처리는 **정합성(Consistency) 보장의 핵심**이다.

### FinTech에서 순서 보장이 필수인 영역

- **잔액 계산** — 차감 전 승인 불가
- **원장 기록** — 순서 역전 시 데이터 불일치
- **승인 순서** — 중복 결제 방지
- **중복 결제 방지** — Idempotency 보장

### 순서가 깨질 경우

```
결제 승인 → 잔액 차감 → 거래 기록
         ↕ 순서 역전
잔액 차감 → 결제 승인  →  정합성 붕괴
```

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. Single Thread Bottleneck

가장 대표적인 직렬 처리 장애 유형.

```
CPU 128코어 존재
 BUT 실제 코드는 단일 스레드 직렬 처리
 → 특정 코어만 100%
 → 전체 CPU 사용률은 낮음
 → P99 Latency 급증
```

---

### 5-2. Queue Saturation

직렬 처리 구간 앞단 Queue 누적 발생.

```
직렬 구간 처리 지연
 → Kafka Lag 증가
 → Request Queue 증가
 → Backpressure 전파
```

---

### 5-3. Lock Contention

직렬 보장을 위한 Lock 사용 시 경합 발생.

| Lock 종류 | 사용처 |
|-----------|--------|
| synchronized | Java 동기화 블록 |
| mutex | POSIX 스레드 |
| futex | Linux 커널 기반 Lock |

```
Lock 경합 증가
 → Context Switch 급증
 → Scheduler Overhead 증가
 → Tail Latency 증가
```

---

### 5-4. Tail Latency 증가

앞 작업이 느려지면 뒤 모든 작업이 대기.

```
작업 A 지연
 → 작업 B 대기
 → 작업 C 대기
 → P99 / P999 급증
```

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Sequential Dependency

직렬 처리의 핵심은 **작업 의존성**.

```
결제 승인
 → 잔액 차감
 → 거래 기록
```

순서가 바뀌면 정합성이 깨진다.

---

### Single Execution Path

실행 흐름이 하나. 병렬 Worker 없음.

```
[Task A] → [Task B] → [Task C]
 (동시 실행 불가)
```

---

### CPU Core Utilization

직렬 처리 시스템에서는 코어 수 증가가 성능 증가로 이어지지 않을 수 있다.

```
코어 수 증가  ≠  성능 증가  (직렬 구간 존재 시)
```

> **암달의 법칙(Amdahl's Law)**: 직렬 구간 비율이 전체 병렬화 한계를 결정

---

### Queueing Delay

직렬 처리에서는 **실행 시간보다 대기 시간**이 더 큰 병목이 될 수 있음.

```
총 지연 = 대기 시간 + 실행 시간
          ↑ 직렬 구간에서 대기가 지배적
```

---

### Context Switch

직렬 처리 보호를 위한 Lock 사용 시 스케줄러 부하 증가.

```
futex wait
 → Scheduler Sleep
 → Thread Wakeup
 → Context Switch 반복
```

---

### Ordered Commit

FinTech에서는 **순서 보장이 성능보다 우선**인 경우가 많다.

| 시스템 | 직렬 순서 보장 이유 |
|--------|---------------------|
| Ledger | 잔액 정합성 |
| WAL | 장애 복구 순서 |
| Replication Log | 복제 일관성 |

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### CPU Core 편중 확인

```bash
top -H
```

### Run Queue 확인

```bash
vmstat 1
```

### Scheduler Delay 확인

```bash
pidstat -wt 1
```

### Context Switch 확인

```bash
pidstat -w
```

### Thread 상태 확인

```bash
ps -eLf
```

### JVM Thread Dump

```bash
jstack <pid>
```

### Lock 경합 분석

```bash
perf lock
```

### Off-CPU 분석

```bash
perf sched
```

### Kubernetes CPU Throttling 확인

```bash
kubectl top pod
```

---

## 요약

```
Serial Processing
 ├── Sequential Dependency   → 작업 의존성 — 순서 역전 시 정합성 붕괴
 ├── Single Execution Path   → 병렬 Worker 없음 — 코어 증가 ≠ 성능 증가
 ├── Queueing Delay          → 실행보다 대기가 병목이 될 수 있음
 ├── Lock Contention         → synchronized / mutex / futex → Context Switch 증가
 ├── Tail Latency            → 앞 작업 지연 → 뒤 전체 대기
 └── Ordered Commit          → Ledger / WAL / Replication — 성능보다 순서 우선
```

> FinTech 결제 시스템에서 직렬 처리는 피해야 할 구조가 아니라,  
> **잔액 정합성 · 원장 순서 · 중복 결제 방지를 보장하는 신뢰성의 물리적 기반**이다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*