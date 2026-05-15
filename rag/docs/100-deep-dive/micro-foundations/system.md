# 시스템 (System)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**시스템(System)** 은:

> 특정 목적을 달성하기 위해 여러 구성 요소가 **상호작용하는 구조적 집합**

컴퓨팅 관점에서는 Hardware, OS Kernel, Runtime, Framework, Application, Network, Storage, Data, Logic이 함께 동작하는 **전체 구조**를 의미한다.

### 기본 구조

```
Input → Processing → Output → Feedback
```

예시:
```
결제 요청 → 승인/검증 처리 → 결제 결과 → 로그/모니터링/재시도
```

---

## 2. 시스템 어디에서 등장하는가

시스템은 특정 계층 하나가 아니라 **전체 구조**를 의미한다.

### Hardware System
- CPU
- Memory
- Disk
- NIC

### OS System
- Linux Kernel
- Scheduler
- cgroup
- Network Stack
- Filesystem

### Runtime System
- JVM
- GC
- Thread
- JIT

### Application System
- API Server
- Payment Service
- Ledger Service
- Fraud Detection

### Distributed System
- Kubernetes
- Kafka
- Redis
- Database Cluster
- Service Mesh

### Observability System
- Metrics
- Logs
- Traces
- Alerts

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

시스템은 특정 자원 하나가 아니라 **CPU / Memory / Network / Disk 전체 자원 조합**에 영향을 받는다.

### 자원별 역할

| 자원 | 시스템 내 역할 |
|------|-------------|
| CPU | 연산과 로직 실행 |
| Memory | 상태와 데이터 보관 |
| Network | 시스템 간 연결 |
| Disk | 데이터 영속성 |
| Scheduler | 실행 순서 결정 |

> 시스템 장애는 보통 하나의 자원 문제가 아니라 **자원 간 상호작용 실패**로 발생한다.

---

## 4. 왜 중요한가

SRE는 개별 컴포넌트보다 **전체 시스템이 목적을 계속 달성하는가**를 본다.

### FinTech에서 시스템 목적

- 결제 승인
- 중복 결제 방지
- 원장 정합성 유지
- 장애 시 복구
- 고객 응답 보장

> 시스템 관점은 **부분 최적화보다 전체 신뢰성 최적화**를 위한 기준이다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### Cascading Failure
하나의 서비스 장애가 다른 서비스로 전파

예: `DB 지연 → API Timeout → Retry 증가 → 전체 트래픽 폭증`

### Resource Saturation
특정 자원 포화가 전체 시스템 장애로 확산:
- CPU Saturation
- Thread Pool Exhaustion
- Connection Pool Exhaustion
- Disk I/O Saturation

### Feedback Loop Failure
재시도, 오토스케일링, 큐 적체가 잘못 결합되면 장애 증폭

### Consistency Failure
일부 컴포넌트만 성공하고 일부 실패 시:
- Partial Commit
- 중복 승인
- 정산 불일치 발생 가능

### Observability Gap
시스템 일부만 관측하면 원인 계층을 잘못 판단할 수 있다

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

### Component
시스템을 구성하는 개별 단위. 예: API Server, DB, Kafka, Redis

### Boundary
시스템 내부와 외부를 구분하는 경계

예: `User ↔ API Gateway`, `Payment Service ↔ PG/VAN`, `Pod ↔ Node`

### Interface
구성 요소 간 통신 규약. 예: `HTTP`, `TCP`, `JDBC`, `Kafka Protocol`

### State
시스템이 유지하는 현재 상태

예: `PENDING → APPROVED → FAILED → SETTLED`

### Feedback
출력 결과가 다시 시스템 동작에 영향을 주는 구조

예: Retry, Autoscaling, Circuit Breaker, Alert

### Dependency
다른 컴포넌트에 의존하는 관계

예: `Payment API → DB → Ledger → External PG`

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux 전체 상태
```bash
top
vmstat 1
iostat -x 1
ss -s
```

### Runtime / JVM
```bash
jfr
jstack
jstat -gc
```

### Kubernetes
```bash
kubectl get pods -A
kubectl describe pod <pod>
kubectl top pod
kubectl get events -A
```

### Distributed System 대표 관측 지표
- `Request Rate`
- `Error Rate`
- `P95/P99 Latency`
- `Queue Depth`
- `Consumer Lag`
- `Retry Count`
- `Saturation`

### Observability 대표 도구
- Prometheus
- Grafana
- OpenTelemetry
- Loki
- Jaeger
- eBPF

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*