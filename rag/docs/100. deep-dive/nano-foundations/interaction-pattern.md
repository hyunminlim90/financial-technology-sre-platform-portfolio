# Interaction Pattern (인터랙션 패턴)
## **Micro Foundations — 범용 시스템/분산 아키텍처 관점**

> 정독: 0회

## 1. 이 기술이 무엇인가

**Interaction Pattern**은:

> 컴포넌트들이 서로 어떻게 대화하고, 누가 언제 기다리고, 누가 제어권을 가지고, 메시지가 어떤 순서로 흐르는가를 정의하는 **시스템의 동적 행동 규칙**

쉽게 말하면:

- **Connector**는 "통로"
- **Interaction Pattern**은 "통로를 사용하는 방식"

이다.

예를 들어:

- A가 B에게 요청 후 기다리는가?
- A가 메시지만 던지고 바로 다음 작업으로 가는가?
- 한 명에게 보내는가?
- 여러 명에게 방송하는가?

이런 행동 모델 전체가 인터랙션 패턴이다.

> **핵심:** Interaction Pattern은 **컴포넌트 간의 시간적 관계와 제어 흐름**을 정의한다.

---

## 2. 시스템 어디에서 등장하는가

인터랙션 패턴은 **모든 분산 시스템**에서 등장한다.

### API 통신
- Client → API → Service → DB

### 서비스 간 통신
- Order Service → Payment Service

### 이벤트 시스템
- Producer → Kafka → Consumer

### 데이터 동기화
- CDC, replication, cache invalidation, event propagation

### 운영 자동화
- orchestration, workflow engine, scheduler, distributed coordination

> **결론:** 둘 이상의 컴포넌트가 협력하면 **반드시 인터랙션 패턴이 존재**한다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 큰 영향은 **Network + Memory + CPU**이다.

### Network 영향
- **인터랙션은 결국 메시지 이동이다**
- 영향: RTT, latency, packet count, throughput, retransmission

### Memory 영향
- **메시지를 버퍼링한다**
- 예: queue, socket buffer, event buffer, inflight request, stream buffer

### CPU 영향
- **메시지를 처리한다**
- 예: serialization, deserialization, protocol parsing, encryption, scheduling

### Disk 영향
- **비동기 이벤트 시스템에서는 중요하다**
- 예: durable queue, commit log, replay log, event store

> **핵심:** 인터랙션 패턴은 **시스템 자원 사용 방식 자체를 바꾼다.**

---

## 4. 왜 중요한가

시스템 장애 대부분은 **컴포넌트 자체보다 컴포넌트 간 상호작용**에서 발생한다.

특히 대규모 시스템에서의 핵심 위험:

- latency propagation
- retry storm
- timeout cascade
- queue explosion
- distributed inconsistency

**좋은 인터랙션 패턴은:**
- 결합도를 낮춘다
- 장애 전파를 차단한다
- Spike를 흡수한다
- 자원 고갈을 늦춘다
- 확장을 쉽게 만든다

**잘못된 패턴은:** 하나의 느린 컴포넌트가 전체 시스템을 마비시킨다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 1) Cascading Failure
```
A → B → C → D  (D가 느려짐)
  ↓
C thread 대기 → B thread 대기 → A thread 대기
  ↓
전체 서비스 자원 고갈
```

### 2) Retry Storm
```
실패 시 모든 컴포넌트가 동시에 retry
  ↓
실제 트래픽보다 retry traffic이 커짐 → 시스템 붕괴
```

### 3) Queue Explosion
```
consumer 처리 속도 < producer 유입 속도
  ↓
queue accumulation → memory pressure → disk pressure
```

### 4) Distributed Inconsistency
```
비동기 이벤트 일부 실패
  ↓
서비스 간 상태 불일치 → 데이터 무결성 파괴
```

### 5) Head-of-Line Blocking
```
하나의 느린 요청이 뒤 요청들을 차단
  ↓
latency spike → throughput collapse
```

> **핵심:** Interaction Pattern 장애는 대부분 **"전염성"**을 가진다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심은:

> **"누가 기다리고, 누가 버퍼링하고, 누가 실패를 흡수하는가"**

### Synchronous vs Asynchronous

| 구분 | 특징 | 장점 | 단점 |
|------|------|------|------|
| **Synchronous** | caller waits, direct dependency | 단순함, 강한 정합성, 직관적 흐름 | blocking, cascading failure 위험 |
| **Asynchronous** | caller does not wait, buffering | spike absorption, fault isolation, scalability | eventual consistency, 복잡한 상태 관리 |

### 핵심 패턴

| 패턴 | 의미 |
|------|------|
| **Request-Response** | 가장 전통적인 1:1 패턴 |
| **Publish-Subscribe** | 1:N 이벤트 전파 패턴 |
| **Event-Driven Pattern** | 이벤트 발생 기반 처리 흐름 |
| **Streaming Pattern** | 데이터를 지속적으로 흘림 |
| **Saga Pattern** | 분산 트랜잭션 보상 흐름 |
| **Backpressure** | 하류 시스템 처리 속도 초과 시 흐름 제한 |
| **Circuit Breaker** | 상대방 장애 시 연결 차단 |

> **핵심 개념:** 좋은 인터랙션 패턴은 정상 흐름뿐 아니라 **실패 흐름도 설계**한다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**네트워크 연결 상태**
```bash
ss -ant
netstat -ant
```
관찰: ESTABLISHED, TIME_WAIT, SYN backlog, connection saturation

**네트워크 지연**
```bash
ping
traceroute
mtr
```
관찰: RTT, packet loss, latency spike

**큐 상태**
```bash
sar -n DEV
iostat
vmstat
```
관찰: queue depth, network backlog, IO wait

---

### Runtime

관찰 포인트:
- inflight request
- retry count
- timeout count
- queue depth
- consumer lag
- event backlog
- thread waiting
- circuit breaker state

---

### Kubernetes

| 관찰 영역 | 명령어 | 관찰 |
|-----------|--------|------|
| **서비스 상호작용** | `kubectl get svc` / `kubectl get endpoints` | 서비스 연결 상태 |
| **Pod 간 흐름** | `kubectl logs` / `kubectl top pod` | timeout, retry, upstream error, connection refused |
| **이벤트 흐름** | `kubectl get events` | 클러스터 이벤트 |
| **메시지 시스템** | 메시징 시스템 모니터링 | consumer lag, partition imbalance, queue retention, rebalance |

> **핵심:** 인터랙션 패턴 문제는 "CPU 100%"보다 먼저 **latency·timeout·queue 증가**로 나타나는 경우가 많다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*