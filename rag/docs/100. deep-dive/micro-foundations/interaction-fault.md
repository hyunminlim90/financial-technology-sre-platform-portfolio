# Interaction Fault (인터랙션 결함)
## **Micro Foundations — 범용 분산 시스템 관점**

> 정독: 0회

## 1. 이 기술이 무엇인가

**Interaction Fault(인터랙션 결함)**는:

> 개별 컴포넌트는 정상인데, 컴포넌트들 사이의 상호작용 방식 때문에 **시스템 전체가 비정상 상태로 발산하는 구조적 결함**

핵심은:

- 문제는 "컴포넌트 내부"가 아니라,
- **"컴포넌트 사이의 연결과 흐름"**

이라는 점이다.

즉, 서비스·DB·Queue·네트워크가 모두 정상인데, **"상호작용 방식"** 때문에 전체 시스템이 무너진다.

**대표 예시:**

| 유형 | 예시 |
|------|------|
| 재시도 문제 | 무제한 retry, retry storm |
| 대기 문제 | 무한 timeout, distributed deadlock |
| 결합 문제 | tight coupling, circular dependency |
| 전파 문제 | synchronous chain explosion, cascading failure |

> **핵심:** Interaction Fault는 **"관계(Relationship)의 결함"**이다.

---

## 2. 시스템 어디에서 등장하는가

Interaction Fault는 분산 시스템 **전역**에서 등장한다.

### 서비스 간 호출
- API A → API B → API C

### 메시지 브로커
- producer/consumer imbalance
- queue feedback loop

### 데이터베이스 연동
- distributed transaction mismatch
- lock propagation

### 네트워크 계층
- retry amplification
- connection storm

### 오케스트레이션 계층
- service mesh policy conflict
- unhealthy failover interaction

### 클라우드/쿠버네티스
- autoscaling feedback instability
- readiness/liveness interaction issue

> **결론:** Interaction Fault는 "단일 시스템 내부"보다 **"시스템 경계(System Boundary)"**에서 주로 발생한다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Network + Thread/Connection Resource**이지만, 실제로는 모든 자원을 연쇄적으로 오염시킨다.

### CPU 영향
- **원인:** retry loop, serialization storm, polling explosion
- **결과:** scheduler overload, CPU saturation

### Memory 영향
- **원인:** request accumulation, queue buildup, unbounded buffering
- **결과:** memory pressure, OOM

### Network 영향
- **원인:** retry amplification, duplicated request flood, cascading timeout traffic
- **결과:** bandwidth exhaustion, retransmission increase

### Disk 영향
- **원인:** queue persistence overload, excessive logging, replication amplification
- **결과:** I/O bottleneck, fsync delay, storage queue saturation

> **핵심:** Interaction Fault는 "한 자원만" 고갈시키지 않는다. **흐름 전체를 연쇄적으로 오염**시킨다.

---

## 4. 왜 중요한가

현대 시스템은 대부분 분산 시스템이다. 따라서 문제의 대부분은 컴포넌트 자체보다 **컴포넌트 간 상호작용**에서 발생한다.

**특히 위험한 이유 — 개별 테스트에서는 잘 안 드러난다:**

```
service A 단독 테스트 → 정상
service B 단독 테스트 → 정상
service C 단독 테스트 → 정상

  ↓

A ↔ B ↔ C가 실제 부하와 장애 상황에서 상호작용할 때만 문제가 폭발
```

> **결론:** Interaction Fault는 **"통합된 실제 운영 환경"**에서만 드러나는 경우가 많다.

---

## 5. 실제 장애와 어떤 관련이 있는가

Interaction Fault는 대규모 장애의 **핵심 원인** 중 하나다.

### 1) Cascading Failure
```
A → B → C  (C가 느려짐)
  ↓
B thread exhaustion → A connection exhaustion → 전체 서비스 장애
```

### 2) Retry Storm
```
gateway retry + client retry + SDK retry 동시 발생
  ↓
실패한 요청보다 재시도 요청이 더 많아짐
```

### 3) Distributed Deadlock
```
A waits B → B waits C → C waits A
  ↓
distributed stall → global timeout
```

### 4) Event Inconsistency
```
비동기 이벤트 처리 실패
  ↓
duplicated event → missing compensation → inconsistent state
```

### 5) Connection Collapse
```
connection pool exhaustion
  ↓
socket exhaustion → thread starvation → accept queue overflow
```

> **핵심:** Interaction Fault는 **"한 컴포넌트의 장애"를 "시스템 전체 장애"로 증폭**시킨다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

Interaction Fault를 막기 위한 핵심은:

> **"상대 시스템은 언제든 느려지고, 죽고, 불안정해질 수 있다"를 기본 전제로 설계하는 것**

따라서 현대 분산 시스템은 isolation · timeout · retry limit · circuit breaker · bulkhead · queue buffering · backpressure를 사용한다.

### 핵심 메커니즘

| 메커니즘 | 의미 |
|----------|------|
| **Timeout** | 무한 대기 차단 |
| **Retry with Backoff** | 재시도 폭주 방지 |
| **Circuit Breaker** | 장애 전염 차단 |
| **Bulkhead Isolation** | 자원 격리 |
| **Queue Buffering** | 충격 흡수 |
| **Async Decoupling** | 서비스 간 결합 완화 |
| **Idempotency** | 중복 실행 안정성 확보 |
| **Backpressure** | 과부하 역전파 |

> **핵심 개념:** 좋은 시스템은 컴포넌트가 실패하지 않는 시스템이 아니라, **컴포넌트 실패가 전염되지 않는 시스템**이다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

Interaction Fault는 observability 전체에 **흔적을 남긴다.**

### Linux

**Connection 상태**
```bash
ss -ant
netstat -an
```
관찰: connection explosion, TIME_WAIT growth, SYN_RECV accumulation

**Queue 상태**
```bash
ss -lnt
```
관찰: backlog overflow, accept queue saturation

**CPU/Load**
```bash
top
vmstat
uptime
```
관찰: thread starvation, scheduler overload

---

### Runtime

관찰 포인트:
- timeout rate
- retry count
- queue depth
- thread pool exhaustion
- connection pool saturation

---

### Kubernetes

| 관찰 영역 | 명령어 | 관찰 |
|-----------|--------|------|
| **Pod Restart** | `kubectl get pods` | crash loop, OOM restart |
| **Service Interaction** | `kubectl logs` / `kubectl describe svc` | retry propagation, upstream timeout, DNS issue |
| **Mesh / Gateway** | — | circuit breaker open, retry amplification, upstream overflow |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*