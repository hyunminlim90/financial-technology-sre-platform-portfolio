# 컴퓨팅 자원 (Computing Resources)

> 정독: 0회

## 1. 이 기술이 무엇인가

컴퓨팅 자원은:

> 프로그램 실행과 시스템 운영에 사용되는 **모든 연산·저장·통신 능력의 총합**

| 자원 | 역할 |
|------|------|
| CPU | 연산 수행 |
| Memory | 실행 상태 저장 |
| Disk | 영속 저장 |
| Network | 데이터 통신 |
| GPU | 병렬 연산 |
| File Descriptor | I/O 핸들 |
| Thread | 실행 흐름 |
| Socket | 네트워크 연결 |

```
Software → Resource Consumption → Runtime Behavior
```

> 모든 소프트웨어는 결국 **컴퓨팅 자원을 소비**합니다.

---

## 2. 시스템 어디에서 등장하는가

컴퓨팅 자원은 시스템 전체에서 등장합니다.

| 계층 | 사용 자원 |
|------|----------|
| Application | CPU, memory, socket |
| Runtime | heap, thread |
| OS Kernel | scheduler, page cache |
| Database | disk, buffer |
| Container | cgroup resource |
| Kubernetes | resource scheduling |

**대표 흐름:**

```
Application Request → Runtime → OS → Hardware Resource Usage
```

---

## 3. 어떤 자원에 가장 영향이 큰가

컴퓨팅 자원은 특정 하나가 아니라 **CPU / Memory / Disk / Network 전체의 균형**이 핵심입니다.

### CPU

연산 처리 능력입니다. 영향: request processing, encryption, serialization, scheduling

### Memory

실행 상태 유지 공간입니다. 영향: object retention, cache, buffering, runtime stability

### Disk

영속 저장 자원입니다. 영향: database, logging, filesystem, persistence

### Network

분산 시스템 핵심 자원입니다. 영향: API latency, service communication, replication, streaming

> 현대 시스템에서는 **특정 자원 하나의 포화 → 전체 시스템 장애**로 이어질 수 있습니다.

---

## 4. 왜 중요한가

컴퓨팅 자원은 시스템의 **성능 / 안정성 / 확장성 / 가용성**을 결정합니다.

| 문제 | 결과 |
|------|------|
| CPU 부족 | latency 증가 |
| memory 부족 | OOM |
| disk saturation | timeout |
| network congestion | packet loss |

> 시스템 운영 = **자원 경쟁 관리**에 가깝습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

대부분의 운영 장애는 **자원 문제와 연결**됩니다.

| 장애 | 원인 |
|------|------|
| CPU 100% | overload |
| OOMKilled | memory exhaustion |
| disk full | storage exhaustion |
| socket exhaustion | fd leak |
| thread starvation | thread pool exhaustion |
| network timeout | congestion |
| cascading failure | shared resource contention |

**대표 패턴:**

```
resource saturation
→ queue buildup
→ latency increase
→ timeout
→ retry storm
→ cascading failure
```

---

## 6. 핵심 메커니즘

### (1) Resource Allocation

OS는 프로세스에 자원을 할당합니다:

- memory allocation
- CPU scheduling
- socket allocation
- file descriptor allocation

### (2) Scheduling

CPU는 공유 자원입니다. OS scheduler가 **which thread runs next**를 결정합니다.

### (3) Isolation

현대 시스템은 자원 격리를 매우 중요하게 다룹니다:

- process isolation
- virtual memory
- container cgroup
- namespace

### (4) Resource Limit

무한 자원은 존재하지 않습니다.

| 자원 | 제한 |
|------|------|
| CPU | quota |
| Memory | limit |
| File Descriptor | ulimit |
| Network | bandwidth |
| Disk | IOPS |

### (5) Contention

여러 프로세스가 동일 자원을 경쟁합니다:

- CPU contention
- lock contention
- memory contention
- I/O contention

### (6) Backpressure

자원 포화를 방지하기 위한 흐름 제어입니다:

- queue limit
- rate limit
- load shedding
- circuit breaker

### (7) Reclaim

OS/runtime는 부족한 자원을 회수합니다:

- page reclaim
- GC
- cache eviction
- process kill

---

## 7. Linux / Runtime / K8s에서 관측 방법

### Linux

```bash
top
htop
vmstat
iostat
sar
free -h
ulimit -a
ss
lsof
```

**핵심 관측 대상:**

| 자원 | 지표 |
|------|------|
| CPU | load average |
| Memory | RSS/swap |
| Disk | await/iops |
| Network | rx/tx |
| FD | open files |
| Thread | runnable queue |

### Runtime

관측 대상:

- heap usage
- thread pool
- event loop delay
- GC pressure
- connection pool saturation

### Kubernetes

| 개념 | 의미 |
|------|------|
| request | 최소 보장 |
| limit | 최대 허용 |
| quota | namespace 제한 |
| eviction | 자원 부족 제거 |

```bash
kubectl top pod
kubectl top node
kubectl describe pod
kubectl describe node
```

> **CPU throttling / OOMKilled / Eviction**은 Kubernetes 핵심 운영 장애입니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*