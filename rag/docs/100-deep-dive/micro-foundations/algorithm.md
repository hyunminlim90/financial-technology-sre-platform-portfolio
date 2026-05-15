# 알고리즘 (Algorithm)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**알고리즘(Algorithm)** 은:

> 입력 데이터를 원하는 결과값으로 변환하기 위한 **명확한 절차와 규칙의 집합**

즉, 무엇을 / 어떤 순서로 / 어떤 조건으로 / 몇 번 반복해서 처리할지를 정의한 **실행 로직**이다.

### 시스템 관점

컴퓨터는 스스로 판단하지 않는다.

> CPU/GPU는 **알고리즘이 정의한 순서대로만 연산 수행**한다.

### 대표 예시

| 영역 | 알고리즘 예시 |
|------|-------------|
| 보안 | SHA-256 |
| DB | B-Tree |
| 네트워크 | TCP Congestion Control |
| 분산 시스템 | Raft |
| 캐시 | LRU |
| 결제 | 승인/정산 로직 |
| AI/FDS | Fraud Detection Model |

<details>
<summary>Deep Dive</summary></br>

Logic(로직) [[M]](../../100-deep-dive/micro-foundations/logic.md)

</details></br>

## 2. 시스템 어디에서 등장하는가

알고리즘은 **시스템 전체**에 존재한다.

### CPU Layer
- Branch Prediction
- Scheduler
- Cache Replacement

### OS Kernel
- CFS Scheduler
- TCP Retransmission
- Memory Reclaim

### JVM Runtime
- GC Algorithm
- JIT Optimization
- Thread Scheduling

### Database
- Index Search
- Query Planner
- Locking

### Distributed System
- Consensus
- Retry
- Load Balancing

### FinTech Business
- 결제 승인
- Fraud Detection
- 정산 계산
- 중복 결제 방지

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

알고리즘은 **CPU + Memory** 영향이 가장 크다.

> 알고리즘은 결국 얼마나 많은 연산을 수행하는가, 얼마나 많은 데이터를 저장/이동하는가를 결정하기 때문.

### 대표 영향

| 자원 | 영향 |
|------|------|
| CPU | 연산량 증가 |
| Memory | 객체/버퍼 사용 |
| Network | Retry/Replication |
| Disk | Index/WAL I/O |

### 좋은 알고리즘 특징

- 적은 CPU Cycle
- 적은 Memory Allocation
- 적은 Lock Contention
- 적은 Network Hop

---

## 4. 왜 중요한가

같은 하드웨어라도 **알고리즘 차이만으로 성능이 수십~수백 배 차이**날 수 있다.

### FinTech에서 특히 중요한 이유

결제 시스템은 Low Latency, High Throughput, Consistency, Availability 모두를 요구한다.

잘못된 알고리즘은 아래를 유발할 수 있다:

> ⚠️ Tail Latency 증가 / DB Lock 증가 / Retry Storm / CPU Saturation

---

## 5. 실제 장애와 어떤 관련이 있는가

### O(N²) 알고리즘 문제
데이터 증가 시 CPU 폭증 발생

예: 비효율적 중복 검사, Nested Loop

### Lock Contention
잘못된 동기화 알고리즘 사용 시 Context Switch 증가 → Throughput 감소 발생

### Retry Algorithm 문제
Backoff 없는 Retry는 Retry Storm → Cascading Failure 유발

### GC Algorithm 영향
JVM GC 알고리즘 문제 시 STW 증가 → P99 Latency 증가 발생

### Load Balancing Algorithm 문제
잘못된 분산 정책 사용 시 특정 노드 Hotspot 발생 가능

### Cache Algorithm 문제
비효율적 Cache Eviction 시 Cache Miss 증가 → DB 부하 증가 발생

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

### Sequence
정해진 순서 실행. 예: `인증 → 승인 → 정산`

### Selection
조건 분기. 예: `IF balance >= amount`

### Iteration
반복 처리. 예: Batch Processing, Queue Consumer Loop

### Time Complexity
입력 증가 시 연산 시간이 얼마나 증가하는가

### Space Complexity
메모리를 얼마나 사용하는가

### Branch Prediction
조건문 많은 알고리즘은 Pipeline Flush / CPU Stall 유발 가능

### Parallelism
병렬 가능한 알고리즘인지 여부 중요. 예: GPU Friendly, Multi-core Friendly

### Synchronization
멀티스레드 환경에서 `Lock`, `CAS`, `Atomic Operation` 필요 가능

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### CPU Usage
```bash
top
mpstat -P ALL 1
```

### Scheduler
```bash
pidstat -w
perf sched
```

### Flamegraph
어떤 알고리즘 함수가 CPU를 많이 사용하는지 분석
```bash
async-profiler
perf
```

### JVM
```bash
jfr
jstack
```

### GC
```bash
jstat -gc
```

### Lock Contention
```bash
perf lock
```

### Kubernetes
```bash
kubectl top pod
```

### eBPF
```bash
# bcc-tools 대표 도구
offcputime
runqlat
profile
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*