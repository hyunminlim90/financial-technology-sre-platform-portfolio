# Implementation Plan

## 1. 목적

이 문서는 토스페이먼츠 기술 스택 기반 실무형 SRE 플랫폼을 어떤 순서로 구현할지 정의합니다.

목표는 모든 기능을 한 번에 만드는 것이 아니라, 포트폴리오 완성도를 높이는 순서대로 핵심 기능과 운영 구조를 단계적으로 구축하는 것입니다.

---

## 2. 구현 원칙

### 2.1 기능보다 운영 가능성 우선

단순히 API가 동작하는 것보다 다음을 함께 구현합니다.

- Metric
- Log
- Trace
- Alert
- Dashboard
- Runbook
- 장애 시나리오

### 2.2 작은 단위로 완성

각 단계는 독립적으로 설명 가능해야 합니다.

```
결제 API 구현
→ Metric 수집
→ 장애 시나리오 작성
→ Runbook 작성
→ Dashboard 연결
```

### 2.3 AI Agent는 후순위

AI Agent / RAG는 SRE 플랫폼 기반이 잡힌 뒤 확장합니다.

```
결제 도메인
→ Observability
→ GitOps
→ 장애 시나리오
→ Runbook
→ AI Agent
```

---

## 3. 전체 단계

| Phase | 내용 |
|-------|------|
| Phase 1 | 결제 도메인 애플리케이션 |
| Phase 2 | 로컬 실행 환경 |
| Phase 3 | Observability 기본 연동 |
| Phase 4 | Kubernetes 배포 |
| Phase 5 | GitOps 배포 |
| Phase 6 | 장애 시나리오 / Runbook |
| Phase 7 | SLO / Alert / Dashboard |
| Phase 8 | AI Agent / RAG 확장 |

---

## Phase 1. 결제 도메인 애플리케이션

**목표**

토스페이먼츠와 유사한 결제 도메인 API와 Worker를 구현합니다.

**구현 대상**

- `apps/payment-api`
- `apps/payment-worker`

**주요 기능**

Payment API:

```
POST /api/v1/payments
GET  /api/v1/payments/{paymentId}
POST /api/v1/payments/{paymentId}/cancel
```

Payment Worker:

- `payment.requested` 이벤트 소비
- 외부 결제 승인 Mock 호출
- 결제 상태 업데이트
- 실패 시 retry 또는 DLQ 처리

**주요 도메인**

- `Merchant`
- `Payment`
- `PaymentAttempt`
- `PaymentEvent`
- `IdempotencyKey`

**완료 기준**

- [ ] 결제 요청 생성 가능
- [ ] 결제 상태 조회 가능
- [ ] 중복 요청 방지 가능
- [ ] Kafka 이벤트 발행 가능
- [ ] Worker가 이벤트 소비 가능
- [ ] 승인 성공 / 실패 상태 반영 가능

---

## Phase 2. 로컬 실행 환경

**목표**

개발자가 로컬에서 전체 시스템을 실행할 수 있도록 구성합니다.

**구성 요소**

- PostgreSQL
- Redis
- Kafka
- Payment API
- Payment Worker
- External Provider Mock

**구현 파일**

- `docker-compose.yml`
- `scripts/setup-local.sh`
- `scripts/reset-local.sh`

**완료 기준**

`docker compose up` 명령으로 다음이 실행되어야 합니다.

- [ ] PostgreSQL
- [ ] Redis
- [ ] Kafka
- [ ] Payment API
- [ ] Payment Worker

---

## Phase 3. Observability 기본 연동

**목표**

서비스 운영에 필요한 metric, log, trace를 기본 연동합니다.

**Metrics 수집 대상**

- HTTP request count
- HTTP request duration
- HTTP error rate
- JVM memory
- DB connection pool
- Kafka producer / consumer
- Redis command latency
- Payment success / failure count

**Logs 필드**

- `traceId`
- `spanId`
- `requestId`
- `paymentId`
- `merchantId`
- `status`
- `errorCode`
- `latencyMs`

**Traces 추적 구간**

- Client → Payment API
- Payment API → Redis
- Payment API → PostgreSQL
- Payment API → Kafka
- Kafka → Payment Worker
- Payment Worker → External Provider Mock

**구현 파일**

- `docs/observability/metrics.md`
- `docs/observability/logging.md`
- `docs/observability/tracing.md`

**완료 기준**

- [ ] Prometheus scrape 가능
- [ ] Grafana dashboard에서 API latency 확인 가능
- [ ] traceId로 log와 trace 연결 가능
- [ ] Jaeger에서 결제 요청 trace 확인 가능

---

## Phase 4. Kubernetes 배포

**목표**

결제 플랫폼을 Kubernetes에 배포합니다.

**Kubernetes 리소스**

- Namespace
- Deployment
- Service
- ConfigMap
- Secret
- HPA
- ServiceMonitor
- PrometheusRule

**구현 대상**

- `k8s/payment-api`
- `k8s/payment-worker`
- `k8s/postgresql`
- `k8s/redis`
- `k8s/kafka`

**완료 기준**

- [ ] payment-api pod 정상 실행
- [ ] payment-worker pod 정상 실행
- [ ] Service로 내부 통신 가능
- [ ] HPA 설정 가능
- [ ] ServiceMonitor로 metric 수집 가능

---

## Phase 5. GitOps 배포

**목표**

ArgoCD 기반 GitOps 배포 구조를 완성합니다.

**구성**

```
gitops/
  bootstrap/
  apps/
  platform/
  observability/
```

**ArgoCD 구성**

- App of Apps
- AppProject
- Application

**완료 기준**

- [ ] ArgoCD에서 payment-api Application 확인 가능
- [ ] Git 변경 시 자동 Sync 가능
- [ ] OutOfSync / Healthy 상태 확인 가능
- [ ] AppProject 권한 문제 없이 배포 가능

---

## Phase 6. 장애 시나리오 / Runbook

**목표**

실무형 장애 시나리오를 작성하고 Runbook으로 대응 절차를 표준화합니다.

**우선 구현 시나리오**

1. **결제 API Latency 증가**
   - `scenarios/payment-api/high-latency.md`
   - `runbooks/payment-api/high-latency.md`

2. **DB Connection Pool 고갈**
   - `scenarios/database/connection-pool-exhaustion.md`
   - `runbooks/database/connection-pool-exhaustion.md`

3. **Kafka Consumer Lag 증가**
   - `scenarios/kafka/consumer-lag.md`
   - `runbooks/kafka/consumer-lag.md`

4. **Redis Timeout 증가**
   - `scenarios/redis/timeout.md`
   - `runbooks/redis/timeout.md`

5. **배포 후 5xx 증가**
   - `scenarios/deployment/high-error-rate-after-deploy.md`
   - `runbooks/deployment/high-error-rate-after-deploy.md`

**Runbook 공통 구조**

1. 증상
2. 영향도
3. 탐지 지표
4. 원인 후보
5. 진단 절차
6. 즉시 완화 조치
7. 근본 해결
8. 롤백
9. 재발 방지
10. 관련 Dashboard / Query

**완료 기준**

- [ ] 최소 5개 장애 시나리오 문서 작성
- [ ] 각 시나리오별 Runbook 작성
- [ ] metric / log / trace 기반 진단 흐름 포함
- [ ] kubectl / PromQL / LogQL 예시 포함

---

## Phase 7. SLO / Alert / Dashboard

**목표**

SLO 기반 운영 체계를 구성합니다.

**SLO**

| 항목 | 목표 |
|------|------|
| Payment API Availability | 99.9% |
| Payment API Latency | p95 < 300ms |
| Payment Approval Delay | p95 < 5s |
| Duplicate Payment Prevention | 99.99% |

**Alert Rule 구현 대상**

- `HighPaymentApiLatency`
- `HighPaymentApiErrorRate`
- `DatabaseConnectionPoolExhaustion`
- `KafkaConsumerLagHigh`
- `RedisTimeoutHigh`
- `PaymentApprovalDelayHigh`
- `ArgoCDSyncFailure`

**Dashboard 구성 패널**

- Request Rate
- Error Rate
- Latency p50 / p95 / p99
- Payment Success / Failure
- DB Connection Pool
- Kafka Consumer Lag
- Redis Latency
- Pod Restart
- Deployment Status

**완료 기준**

- [ ] PrometheusRule 작성
- [ ] Grafana dashboard JSON 작성
- [ ] Alert별 Runbook 링크 연결
- [ ] SLO 문서 작성

---

## Phase 8. AI Agent / RAG 확장

**목표**

Runbook 기반 진단 보조 Agent를 구현합니다.

> 초기 범위는 자동 복구가 아니라 진단 보조에 집중합니다.

```
Alert 입력
→ 관련 Runbook 검색
→ 주요 원인 후보 제시
→ 확인할 metric / log / trace 추천
→ 조치 방법 추천
```

**구현 대상**

- `agent/`
- `rag/`
- `runbooks/`

**단계**

1. **Runbook Indexing**: Markdown → Chunking → Embedding → Vector Store
2. **Retrieval**: Alert name / Metric context / Error log → 관련 Runbook 검색
3. **Diagnosis**: 원인 후보 / 확인할 지표 / 진단 명령 / 권장 조치 / 위험도
4. **Approval Workflow**: 조치 제안 → 사용자 승인 → 명령 실행 → 결과 기록

**완료 기준**

- [ ] Alert 입력 시 관련 Runbook 검색 가능
- [ ] Agent가 원인 후보 3개 이상 제시 가능
- [ ] 위험한 조치는 승인 없이는 실행하지 않음
- [ ] 실행 로그 저장 가능

---

## 4. 우선순위 체크리스트

### 가장 먼저 해야 할 것

- [ ] Payment API 기본 구현
- [ ] PostgreSQL schema 작성
- [ ] Redis idempotency 처리
- [ ] Kafka `payment.requested` 발행
- [ ] Payment Worker 구현
- [ ] docker-compose 작성
- [ ] high-latency scenario 작성
- [ ] high-latency runbook 작성

### 포트폴리오 완성도를 빠르게 올리는 것

- [ ] `DOMAIN-ARCHITECTURE.md`
- [ ] `IMPLEMENTATION-PLAN.md`
- [ ] `runbooks/payment-api/high-latency.md`
- [ ] `scenarios/payment-api/high-latency.md`
- [ ] `docs/observability/tracing.md`
- [ ] `docs/slo/payment-slo.md`

### 후순위

- [ ] AI Agent 자동 조치
- [ ] RAG Vector Store
- [ ] Chaos Engineering 자동화
- [ ] Multi-region
- [ ] FinOps

---

## 5. 권장 진행 순서

현재 상태에서 권장하는 진행 순서는 다음과 같습니다.

1. `DOMAIN-ARCHITECTURE.md` 작성
2. `IMPLEMENTATION-PLAN.md` 작성
3. Payment API 설계
4. DB Schema 설계
5. Kafka Topic 설계
6. Redis Idempotency 설계
7. high-latency scenario 작성
8. high-latency runbook 작성
9. Observability 문서 작성
10. GitOps 리소스 보완

---

## 6. Definition of Done

이 프로젝트가 "실무형 SRE 플랫폼"으로 보이기 위한 최소 완료 기준은 다음과 같습니다.

- [ ] 결제 요청 API가 있다
- [ ] 결제 상태 조회 API가 있다
- [ ] 결제 이벤트가 Kafka로 발행된다
- [ ] Worker가 이벤트를 소비한다
- [ ] PostgreSQL에 결제 상태가 저장된다
- [ ] Redis로 중복 결제를 방지한다
- [ ] Prometheus metric이 수집된다
- [ ] Grafana dashboard가 있다
- [ ] Jaeger trace가 확인된다
- [ ] 최소 5개 장애 Runbook이 있다
- [ ] ArgoCD로 배포된다
- [ ] README에서 전체 흐름이 설명된다

---

## 7. 요약

> 결제 기능을 먼저 만들고,  
> 그 결제 기능을 운영 가능한 시스템으로 확장하며,  
> **마지막에 AI Agent가 Runbook을 활용하도록 만든다.**