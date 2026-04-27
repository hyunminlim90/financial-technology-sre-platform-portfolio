# Architecture

## 1. 개요

이 문서는 FinTech 결제 시스템을 기반으로 한 SRE 플랫폼의 전체 아키텍처를 설명합니다.

이 아키텍처는 단순한 서비스 구조가 아니라 다음을 포함합니다.

- 서비스 요청 흐름
- 데이터 흐름
- 비동기 처리 구조
- Observability 수집 구조
- GitOps 기반 배포 흐름
- 장애 탐지 및 대응 흐름

---

## 2. 전체 아키텍처 개요

### 2.1 서비스 아키텍처

```
[Client]
   |
   v
[Ingress / Gateway]
   |
   v
[Payment API (Spring WebFlux)]
   |
   +--> [Redis]
   |
   +--> [PostgreSQL]
   |
   +--> [Kafka Producer]
   |
   v
[Kafka Cluster]
   |
   v
[Consumer / Worker]
   |
   v
[External Payment Provider Mock]
```

### 2.2 운영 아키텍처

```
[Application Pods]
   |
   +--> Metrics ----> Prometheus ----> Grafana
   |
   +--> Logs ------> Loki ----------> Grafana
   |
   +--> Traces ----> OpenTelemetry Collector ----> Jaeger
   |
   +--> Events ----> Alertmanager
```

### 2.3 배포 아키텍처 (GitOps)

```
[Application Source Repo]
        |
        v
[GitOps Repo]
        |
        v
[ArgoCD]
        |
        v
[Kubernetes Cluster]
```

---

## 3. 서비스 구성 요소

### 3.1 Payment API

- Spring WebFlux 기반 비동기 API
- Non-blocking 구조
- Event Loop 기반 처리

**주요 역할**

- 결제 요청 수신
- 결제 승인 처리
- 결제 상태 조회
- 이벤트 발행

### 3.2 Redis

**용도**

- Idempotency key 저장
- 캐시
- Rate limit
- 분산 락

**특징**

- Low latency
- 장애 시 fallback 필요

### 3.3 PostgreSQL

**용도**

- 결제 트랜잭션 저장
- 이벤트 기록
- 정합성 보장

**특징**

- ACID 보장
- Connection pool 관리 중요

### 3.4 Kafka

**용도**

- 결제 이벤트 비동기 처리
- 이벤트 기반 아키텍처

**이벤트 예시**

- `payment.requested`
- `payment.approved`
- `payment.failed`

### 3.5 Consumer / Worker

**역할**

- Kafka 메시지 소비
- 외부 결제 승인 처리
- 후처리 수행

### 3.6 External Payment Provider (Mock)

- 외부 API 의존성 시뮬레이션
- latency / error injection 가능

---

## 4. 요청 처리 흐름

### 4.1 결제 요청 흐름

1. Client → Payment API 요청
2. Idempotency key 검증 (Redis)
3. DB 저장 (PostgreSQL)
4. Kafka 이벤트 발행
5. 응답 반환

### 4.2 결제 승인 흐름

1. Kafka Consumer 메시지 수신
2. External Payment Provider 호출
3. 결과 DB 저장
4. 상태 이벤트 발행

---

## 5. 비동기 처리 구조

비동기 처리는 Kafka를 기반으로 구성됩니다.

**장점**

- 서비스 간 결합도 감소
- 처리 지연 허용
- 확장성 확보

**리스크**

- Consumer lag
- 메시지 유실
- 중복 처리

**대응 전략**

- DLQ 구성
- Idempotency 보장
- Retry 정책 설정

---

## 6. Observability 아키텍처

### 6.1 Metrics

수집 대상:

- HTTP 요청 수
- HTTP latency
- Error rate
- JVM 상태
- DB connection pool
- Kafka lag
- Redis latency

```
Application → Prometheus → Grafana
```

### 6.2 Logs

```
Application → Loki → Grafana
```

로그 필드:

- `traceId`
- `requestId`
- `paymentId`
- `errorCode`
- `latency`

### 6.3 Traces

```
Application → OpenTelemetry → Collector → Jaeger
```

추적 대상:

- API 요청
- DB 호출
- Redis 호출
- Kafka 처리
- External API 호출

### 6.4 Correlation 전략

모든 요청은 다음을 포함합니다.

- `traceId`
- `spanId`
- `requestId`

이를 통해 Metric / Log / Trace를 연결합니다.

---

## 7. Alerting 구조

```
Prometheus → Alert Rule → Alertmanager → Notification
```

**알림 대상**

- Slack
- Email

**주요 Alert**

- High latency
- High error rate
- DB connection pool exhaustion
- Kafka consumer lag
- Pod restart 증가
- ArgoCD sync failure

---

## 8. GitOps 아키텍처

### 8.1 구조

```
GitOps Repo
  ├── apps/
  ├── infra/
  ├── observability/
  ├── argocd/
```

### 8.2 ArgoCD 구성

- App of Apps 패턴
- Application 단위 배포
- Namespace 분리
- AppProject로 권한 관리

### 8.3 배포 흐름

1. 코드 변경
2. 이미지 빌드
3. GitOps repo 업데이트
4. ArgoCD sync
5. Kubernetes 적용

---

## 9. Kubernetes 구성

### 주요 리소스

- Deployment
- Service
- Ingress / Gateway (Istio)
- ConfigMap
- Secret
- HPA

### 네트워크

- Istio Service Mesh
- Traffic control
- Circuit breaker
- Retry 정책

---

## 10. 장애 대응 아키텍처

```
[Alert 발생]
   |
   v
[Dashboard 확인]
   |
   v
[Trace / Log 분석]
   |
   v
[Runbook 조회]
   |
   v
[원인 분석]
   |
   v
[조치 수행]
   |
   v
[Postmortem]
```

---

## 11. SLO 기반 운영

### 핵심 SLO

| 항목 | 목표 |
|------|------|
| Availability | 99.9% |
| Latency | p95 < 300ms |
| Event 처리 지연 | p95 < 5s |

### Error Budget

```
Error Budget = 1 - SLO
```

**운영 전략**

- Error budget 초과 시 배포 제한
- 안정성 개선 우선

---

## 12. 확장 아키텍처 (AI Agent)

초기에는 포함되지 않지만, 다음 구조로 확장합니다.

```
[Alert]
   |
   v
[AI Agent]
   |
   +--> Runbook (RAG)
   |
   +--> Metrics / Logs / Traces
   |
   v
[Diagnosis]
   |
   v
[Recommendation]
   |
   v
[Approval]
   |
   v
[Action]
```

---

## 13. 설계 결정 요약

| 영역 | 선택 | 이유 |
|------|------|------|
| API | WebFlux | 높은 동시성 |
| DB | PostgreSQL | 정합성 |
| Cache | Redis | 저지연 |
| Messaging | Kafka | 비동기 처리 |
| Deploy | ArgoCD | GitOps |
| Observability | Prometheus + Loki + Jaeger | 표준 스택 |
| Mesh | Istio | 트래픽 제어 |
| Tracing | OpenTelemetry | 표준화 |

---

## 14. 핵심 트레이드오프

### WebFlux

**장점**
- 높은 처리량
- 적은 thread

**단점**
- 디버깅 어려움
- blocking 코드 위험

### Kafka

**장점**
- 확장성
- 비동기 처리

**단점**
- 운영 복잡도
- lag 관리 필요

### Istio

**장점**
- 트래픽 제어
- observability

**단점**
- 리소스 사용량 증가
- 운영 난이도 상승

---

## 15. 요약

이 아키텍처는 단순 서비스 구조가 아니라, 다음을 목표로 설계되었습니다.

- 장애를 빠르게 탐지
- 원인을 정확히 분석
- 영향을 최소화
- 자동화 가능한 구조로 확장

> 즉, "서비스 개발"이 아니라  
> **"운영 가능한 시스템"을 만드는 것이 핵심입니다.**