# 동적 자원 관리 (Dynamic Resource Management)

> 정독: 0회

## 1. 이 기술이 무엇인가

동적 자원 관리는:

> 실행 중인 시스템 상태를 기반으로 CPU, 메모리, I/O, 스레드 등의 자원을 **실시간으로 할당·조정·회수하는 운영 메커니즘**

```
Runtime State
→ Resource Observation
→ Adaptive Allocation
→ Reclaim / Optimization
```

> 시스템은 실행 중 변화하는 부하와 상태를 감시하면서 **자원 사용을 계속 조절**합니다.

**대표 관리 대상:**

| 자원 | 관리 내용 |
|------|----------|
| CPU | scheduling/throttling |
| Memory | allocation/reclaim |
| Thread | pooling/scheduling |
| I/O | buffering/queueing |
| Network | flow control |
| Connection | pool management |

---

## 2. 시스템 어디에서 등장하는가

동적 자원 관리는 거의 모든 현대 시스템에 존재합니다.

| 계층 | 역할 |
|------|------|
| OS Kernel | scheduler/memory reclaim |
| Runtime | heap/thread management |
| Database | buffer pool |
| Container Runtime | cgroup control |
| Kubernetes | autoscaling/resource limit |
| Network Stack | congestion control |

**대표 흐름:**

```
Workload Increase
→ Resource Pressure
→ Runtime/OS Adjustment
→ Stability Maintenance
```

---

## 3. 어떤 자원에 가장 영향이 큰가

동적 자원 관리는 모든 자원에 영향을 미칩니다. 실제 운영에서는 **CPU / Memory** 영향이 가장 큽니다.

### CPU

대표 관리: scheduler, core allocation, throttling, load balancing

> CPU saturation 발생 시 **scheduling 전략**이 매우 중요해집니다.

### Memory

대표 관리: allocation, reclaim, page eviction, garbage collection, cache control

> 메모리 압박 시 **시스템 전체 안정성**에 직접 영향을 줍니다.

### Disk

대표 관리: write buffering, flush timing, I/O scheduling

### Network

대표 관리: socket buffer, congestion control, connection pool, backpressure

---

## 4. 왜 중요한가

현대 시스템 부하는 고정되지 않습니다. **resource demand = dynamic**입니다.

정적 자원 배분만으로는 효율성 / 안정성 / 확장성을 유지할 수 없습니다.

| 이유 | 설명 |
|------|------|
| scalability | 부하 증가 대응 |
| stability | 자원 고갈 방지 |
| efficiency | unused resource 최소화 |
| latency control | overload 완화 |
| concurrency | 동시 실행 유지 |

---

## 5. 실제 장애와 어떤 관련이 있는가

동적 자원 관리 실패는 **운영 장애로 직결**됩니다.

| 장애 | 원인 |
|------|------|
| OOM | reclaim 실패 |
| CPU starvation | scheduler imbalance |
| GC storm | excessive allocation |
| thread exhaustion | uncontrolled concurrency |
| queue explosion | backpressure 부재 |
| swap storm | memory pressure |
| cascading failure | adaptive control 실패 |

**대표 패턴:**

```
traffic spike
→ resource saturation
→ queue accumulation
→ latency increase
→ timeout
→ retry amplification
→ cascading failure
```

---

## 6. 핵심 메커니즘

### (1) Allocation

필요 시 자원을 할당합니다:

- memory allocation
- thread allocation
- socket allocation
- buffer allocation

### (2) Reclaim

사용하지 않는 자원을 회수합니다:

- garbage collection
- page reclaim
- cache eviction
- connection cleanup

### (3) Scheduling

자원 사용 우선순위를 조정합니다:

- CPU scheduler
- I/O scheduler
- thread scheduler

### (4) Pooling

자원을 재사용합니다:

| Pool | 목적 |
|------|------|
| Thread Pool | thread reuse |
| Connection Pool | DB/network reuse |
| Buffer Pool | allocation 감소 |

### (5) Backpressure

과부하 시 입력 속도를 제한합니다:

- queue limit
- rate limiting
- load shedding
- circuit breaker

### (6) Autoscaling

부하에 따라 자원을 확장/축소합니다:

- horizontal scaling
- vertical scaling
- pod autoscaling

### (7) Adaptive Optimization

런타임은 실행 패턴을 관찰하며 최적화를 수행합니다:

- cache optimization
- scheduler balancing
- runtime tuning
- hot path optimization

---

## 7. Linux / Runtime / K8s에서 관측 방법

### Linux

```bash
top
htop
vmstat
iostat
sar
pidstat
free -h
```

**핵심 관측:**

| 항목 | 의미 |
|------|------|
| load average | CPU pressure |
| swap activity | memory pressure |
| run queue | scheduler pressure |
| iowait | I/O saturation |
| context switch | scheduling overhead |

### Runtime

관측 대상:

- heap usage
- allocation rate
- GC pause
- thread pool saturation
- queue size
- event loop delay

### Kubernetes

| 기능 | 역할 |
|------|------|
| HPA | pod autoscaling |
| Resource Limit | usage control |
| Eviction | pressure handling |
| QoS Class | priority management |

```bash
kubectl top pod
kubectl top node
kubectl describe pod
kubectl get hpa
```

> **OOMKilled / CPU throttling / Eviction / Pending Pod**은 자원 관리 실패 신호입니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*