# System Architecture (시스템 아키텍처)
## **Micro Foundations — 범용 시스템 관점**

> 정독: 0회

## 1. 이 기술이 무엇인가

**System Architecture(시스템 아키텍처)**  는:

> 시스템을 구성하는 모든 요소를 어떻게 배치하고, 어떻게 연결하며, 어떤 규칙으로 상호작용하게 만들 것인지 정의하는 **전체 구조 설계**

핵심은:

- "무엇을 만들 것인가"보다,
- **"어떻게 살아 움직이게 할 것인가"**

를 정의하는 것이다.

아키텍처는 단순 서버 배치도가 아니다. 포함되는 것:

| 영역 | 내용 |
|------|------|
| 구조 | 컴포넌트 구조, 자원 분배 |
| 통신 | 데이터 흐름, 통신 방식 |
| 안정성 | 장애 처리, 복구 전략 |
| 성장 | 확장 전략, 제약 조건 |
| 보안 | 보안 경계 |

> **핵심:** 시스템 아키텍처는 **시스템의 생존 방식 자체**를 설계한다.

<details>
<summary>Deep Dive</summary></br>

System(시스템) [[M]](../../100-deep-dive/micro-foundations/system.md)  
System Component(시스템 컴포넌트) [[M]](../../100-deep-dive/micro-foundations/system-component.md)  
Deployment Topology(배치 토폴로지) [[M]](../../100-deep-dive/micro-foundations/deployment-topology.md)  
Connector(커넥터) [[M]](../../100-deep-dive/micro-foundations/connector.md)  
Constraints(제약 조건) [[M]](../../100-deep-dive/micro-foundations/constraints.md)  
Interaction Pattern [[M]](../../100-deep-dive/micro-foundations/interaction-pattern.md)

</details></br>

## 2. 시스템 어디에서 등장하는가

시스템 **전체 모든 계층**에 등장한다.

### 하드웨어 계층
- CPU topology
- NUMA layout
- storage architecture
- network fabric

### 운영체제 계층
- process isolation
- scheduling model
- virtual memory layout

### 네트워크 계층
- routing topology
- load balancing
- gateway structure

### 데이터 계층
- replication
- sharding
- persistence strategy

### 애플리케이션 계층
- service decomposition
- request flow
- async/sync model

### 분산 시스템 계층
- cluster topology
- consensus model
- failover architecture

> **결론:** 아키텍처는 특정 기술 하나가 아니라 **시스템 전체 계층을 관통하는 구조적 질서**다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

시스템 아키텍처는 **모든 자원의 소비 방식 자체**를 결정한다.

### CPU 영향
- **결정하는 것:** parallelism, scheduling pressure, contention, concurrency model
- **잘못 설계되면:** CPU saturation, lock contention, excessive context switching

### Memory 영향
- **결정하는 것:** cache strategy, buffering, queue size, state management
- **잘못 설계되면:** memory pressure, GC storm, OOM

### Network 영향
- **결정하는 것:** request path, service communication, replication traffic, retry flow
- **잘못 설계되면:** network amplification, latency explosion, packet congestion

### Disk 영향
- **결정하는 것:** persistence strategy, write pattern, storage consistency, replication model
- **잘못 설계되면:** I/O bottleneck, write amplification, storage collapse

> **핵심:** 아키텍처는 자원을 얼마나 쓰는지가 아니라, **"어떤 방식으로" 쓰게 될지**를 결정한다.

---

## 4. 왜 중요한가

아키텍처는 **시스템의 한계와 생존 가능성**을 결정한다.

- 코드는 수정 가능하다.
- 버그는 패치 가능하다.
- 하지만 **잘못된 아키텍처는 시스템 전체를 지속적으로 불안정하게 만든다.**

다음과 같은 구조적 문제는 부하가 낮을 때는 정상처럼 보이지만, 비정상 상황에서 급격히 붕괴한다:

- SPOF 존재
- unbounded queue
- no isolation
- no backpressure
- shared DB contention

비정상 상황의 예:

- spike
- fault
- concurrency increase
- partial outage

> **결론:** 아키텍처는 정상 상황보다 **비정상 상황에서 진짜 실력이 드러난다.**

---

## 5. 실제 장애와 어떤 관련이 있는가

대규모 장애 상당수는 **아키텍처 문제**에서 시작된다.

### 1) Single Point of Failure
```
단일 DB → DB 장애 → entire system unavailable
```

### 2) Shared Resource Contention
```
모든 서비스가 하나의 자원 공유 → traffic spike → resource starvation → system-wide latency
```

### 3) Unbounded Queue Architecture
```
무제한 요청 적재 → memory growth → OOM → cascading restart
```

### 4) Synchronous Dependency Chain
```
서비스들이 동기 호출로 직렬 연결 → 하나 느려짐 → 전체 latency propagation → timeout cascade
```

### 5) No Isolation
```
fault containment 없음 → one component failure → global instability
```

> **결론:** 아키텍처는 장애의 "발생 여부"보다, **장애의 "전파 범위"**를 결정한다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

시스템 아키텍처의 핵심은:

> **"구조가 곧 동작 방식이 된다"**

즉, topology · dependency · flow · boundary · isolation · constraints 설계가 그대로 **런타임 behavior**가 된다.

### 핵심 메커니즘

| 메커니즘 | 의미 |
|----------|------|
| **Topology** | 무엇이 어디에 존재하는가 |
| **Dependency** | 누가 누구에게 의존하는가 |
| **Flow** | 데이터와 제어가 어디로 흐르는가 |
| **Isolation** | 문제가 어디까지 퍼질 수 있는가 |
| **Redundancy** | 무언가 죽어도 계속 동작 가능한가 |
| **Constraints** | 시스템 폭주를 어디서 차단하는가 |
| **Scalability** | 부하 증가를 어떻게 흡수하는가 |
| **Recovery** | 실패 후 어떻게 복구되는가 |

> **핵심 개념:** 좋은 아키텍처는 정상 상황에서는 **효율적**이고, 비정상 상황에서는 **예측 가능**하다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

아키텍처는 시스템 전체 **observability 패턴**으로 드러난다.

### Linux

**CPU / Scheduling**
```bash
top
vmstat
pidstat
```
관찰: contention, load imbalance, excessive switching

**Memory**
```bash
free -h
sar -r
```
관찰: buffering strategy, memory pressure, queue accumulation

**Disk**
```bash
iostat
iotop
```
관찰: write bottleneck, persistence contention

**Network**
```bash
ss -s
iftop
ip -s link
```
관찰: communication pattern, retry storm, packet congestion

---

### Runtime

관찰 포인트:
- request latency distribution
- queue depth
- dependency chain
- retry amplification
- timeout propagation

---

### Kubernetes

| 관찰 영역 | 명령어 | 관찰 |
|-----------|--------|------|
| **Pod Distribution** | `kubectl get pods -o wide` | topology spread, node concentration, workload isolation |
| **Resource Behavior** | `kubectl top pod` / `kubectl top node` | hotspot, imbalance, throttling |
| **Failure Pattern** | `kubectl describe pod` / `kubectl describe node` | cascading failure, restart propagation, dependency instability |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*