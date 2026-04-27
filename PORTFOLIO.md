# Financial Technology SRE Platform Portfolio

## 1. 프로젝트 개요

이 프로젝트는 토스페이먼츠와 같은 FinTech 결제 도메인을 기준으로 설계한 실무형 SRE 플랫폼 포트폴리오입니다.

단순한 백엔드 애플리케이션 구현이 아니라, 실제 운영 환경에서 발생할 수 있는 장애를 탐지하고, 분석하고, 복구하며, 재발을 방지하는 전체 운영 체계를 구축하는 것을 목표로 합니다.

### 핵심 목표

- 결제 API의 안정적인 운영
- 장애 탐지 및 원인 분석 체계 구축
- Kubernetes 기반 서비스 운영
- GitOps 기반 배포 자동화
- Observability 기반 모니터링 환경 구축
- Runbook 기반 장애 대응 표준화
- 향후 AI Agent / RAG 기반 운영 자동화로 확장

---

## 2. 프로젝트 배경

결제 시스템은 일반적인 웹 서비스보다 높은 신뢰성이 요구됩니다.

결제 요청은 단순 API 호출이 아니라 사용자, 가맹점, PG, 은행, 카드사 등 여러 시스템이 연결된 트랜잭션입니다. 따라서 작은 지연이나 부분 장애도 사용자 경험과 비즈니스 손실로 이어질 수 있습니다.

이 프로젝트는 다음과 같은 운영 문제를 해결하기 위해 설계되었습니다.

- 결제 API 지연 증가
- 외부 결제 승인 API 장애
- DB Connection Pool 고갈
- Redis 장애로 인한 인증/캐시 실패
- Kafka Consumer Lag 증가
- 배포 후 오류율 증가
- Kubernetes 리소스 부족
- ArgoCD Sync 실패
- Trace / Metric / Log 수집 장애

---

## 3. 핵심 설계 방향

### 3.1 장애는 반드시 발생한다

장애를 완전히 없애는 것이 아니라, 빠르게 감지하고 영향을 줄이며 재발을 방지하는 구조를 목표로 합니다.

### 3.2 운영 가능성이 구현보다 중요하다

기능 구현뿐 아니라 다음 요소를 함께 설계합니다.

- Metric
- Log
- Trace
- Alert
- Dashboard
- Runbook
- Rollback
- Postmortem

### 3.3 자동화는 통제 가능해야 한다

AI Agent나 자동 복구 시스템은 무제한 권한을 가지면 위험합니다.

따라서 자동화는 다음 단계를 거칩니다.

1. Read-only 진단
2. 원인 후보 제시
3. Runbook 기반 대응 추천
4. 승인 기반 조치
5. 제한된 자동 복구
6. 감사 로그 기록

---

## 4. 기술 스택

### Backend

- Java
- Spring Boot
- Spring WebFlux
- Spring Security
- R2DBC / JDBC
- Gradle

### Data

- PostgreSQL
- Redis
- Kafka

### Infrastructure

- Kubernetes
- Istio
- ArgoCD
- Helm
- Terraform / OpenTofu
- AWS

### Observability

- Prometheus
- Grafana
- Loki
- Tempo / Jaeger
- OpenTelemetry
- Kiali

### Reliability Engineering

- SLI / SLO
- Alert Rule
- Incident Scenario
- Runbook
- Postmortem
- Chaos Test

### AI / Automation 확장

- AI Agent
- RAG
- Vector Store
- Runbook Retrieval
- Guardrail
- Approval Workflow

---

## 5. 시스템 아키텍처

### 서비스 흐름

```
[Client]
   |
   v
[API Gateway / Ingress]
   |
   v
[Payment API - Spring WebFlux]
   |
   +--> [Redis]
   |
   +--> [PostgreSQL]
   |
   +--> [Kafka]
   |
   v
[Worker / Consumer]
   |
   v
[External Payment Provider Mock]
```

### 운영 관점의 흐름

```
[Service]
   |
   +--> Metrics --> Prometheus --> Grafana
   |
   +--> Logs -----> Loki -------> Grafana
   |
   +--> Traces ---> OpenTelemetry --> Jaeger / Tempo
   |
   +--> Deploy ---> GitOps Repo --> ArgoCD --> Kubernetes
```

### 장애 대응 흐름

```
[Alert 발생]
   |
   v
[Metric / Log / Trace 확인]
   |
   v
[Runbook 기반 진단]
   |
   v
[원인 후보 분석]
   |
   v
[완화 조치]
   |
   v
[Rollback or Scaling or Config Fix]
   |
   v
[Postmortem 작성]
```

---

## 6. 주요 구현 범위

### 6.1 결제 API

결제 도메인을 기준으로 API를 구성합니다.

주요 기능은 다음과 같습니다.

- 결제 요청 생성
- 결제 승인 처리
- 결제 상태 조회
- 결제 실패 처리
- 결제 이벤트 발행
- 비동기 후처리

### 6.2 비동기 처리 구조

Kafka를 사용하여 결제 후처리를 비동기화합니다.

예시 이벤트:

- `payment.requested`
- `payment.approved`
- `payment.failed`
- `payment.cancelled`

### 6.3 캐시 및 세션 구조

Redis는 다음 용도로 사용합니다.

- 결제 요청 idempotency key 관리
- 임시 결제 상태 캐싱
- Rate limit
- Distributed lock

### 6.4 데이터 저장소

PostgreSQL은 결제 트랜잭션의 원장성 데이터를 저장합니다.

주요 테이블:

- `payments`
- `payment_events`
- `payment_attempts`
- `merchants`
- `idempotency_keys`

---

## 7. SRE 관점 핵심 시나리오

### Scenario 1. 결제 API Latency 증가

**증상**

- p95 latency 증가
- 결제 승인 응답 지연
- 사용자 timeout 증가

**주요 원인 후보**

- 외부 결제 승인 API 지연
- DB slow query
- Connection pool 부족
- Event loop blocking
- Redis 응답 지연

**대응**

- Trace 확인
- Slow query 확인
- Connection pool metric 확인
- 외부 API fallback 여부 확인
- 필요 시 traffic 제한 또는 rollback

---

### Scenario 2. DB Connection Pool 고갈

**증상**

- API timeout 증가
- HikariCP active connection 포화
- DB wait event 증가

**주요 원인 후보**

- Connection leak
- Long transaction
- Slow query
- 트래픽 급증
- Pool size 설정 오류

**대응**

- active / idle connection 확인
- slow query 확인
- transaction duration 확인
- 애플리케이션 thread dump 확인
- 임시 scale-out 또는 pool 조정

---

### Scenario 3. Kafka Consumer Lag 증가

**증상**

- 결제 후처리 지연
- 알림 / 정산 / 이벤트 처리 지연
- consumer lag 증가

**주요 원인 후보**

- consumer 처리 속도 부족
- partition 수 부족
- downstream 장애
- poison message
- retry 폭증

**대응**

- consumer lag 확인
- topic / partition 확인
- error log 확인
- DLQ 여부 확인
- consumer scale-out

---

### Scenario 4. Redis 장애

**증상**

- idempotency check 실패
- rate limit 실패
- cache miss 증가
- API latency 증가

**주요 원인 후보**

- Redis memory pressure
- connection timeout
- eviction 증가
- network issue
- Redis pod 재시작

**대응**

- Redis availability 확인
- memory / eviction metric 확인
- connection count 확인
- fallback 로직 확인
- 필요 시 Redis 재시작 또는 failover

---

### Scenario 5. 배포 후 오류율 증가

**증상**

- 5xx 증가
- 특정 API 실패율 증가
- 신규 버전 pod에서만 에러 발생

**주요 원인 후보**

- 코드 버그
- 설정 누락
- DB migration 문제
- secret / configmap 오류
- dependency version 문제

**대응**

- ArgoCD 배포 이력 확인
- pod log 확인
- trace 비교
- canary metric 확인
- rollback 수행

---

### Scenario 6. ArgoCD Sync Failure

**증상**

- Application OutOfSync
- Health Degraded
- Sync Failed

**주요 원인 후보**

- CRD 누락
- AppProject 권한 문제
- namespace 미존재
- Helm values 오류
- Kubernetes API validation 실패

**대응**

- ArgoCD event 확인
- Application manifest 확인
- AppProject destination/sourceRepos 확인
- CRD 설치 여부 확인
- server-side apply 필요 여부 확인

---

## 8. Observability 설계

### Metrics

Prometheus를 사용하여 다음 지표를 수집합니다.

- HTTP request count
- HTTP request duration
- HTTP error rate
- JVM memory
- JVM GC
- CPU / Memory
- DB connection pool
- Kafka consumer lag
- Redis latency
- Kubernetes pod status

### Logs

Loki를 사용하여 애플리케이션과 인프라 로그를 수집합니다.

로그에는 다음 필드를 포함합니다.

- `traceId`
- `spanId`
- `requestId`
- `paymentId`
- `merchantId`
- `errorCode`
- `latency`
- `status`

### Traces

OpenTelemetry를 사용하여 분산 추적을 구성합니다.

추적 대상:

- Client → API
- API → Redis
- API → PostgreSQL
- API → Kafka
- Consumer → External Provider Mock

---

## 9. SLI / SLO

### 결제 API Availability

| 항목 | 내용 |
|------|------|
| SLI | 성공한 결제 API 요청 수 / 전체 결제 API 요청 수 |
| SLO | 99.9% |

### 결제 API Latency

| 항목 | 내용 |
|------|------|
| SLI | p95 latency |
| SLO | p95 < 300ms |

### 결제 이벤트 처리 지연

| 항목 | 내용 |
|------|------|
| SLI | Kafka event created time과 consumed time의 차이 |
| SLO | p95 < 5s |

### 배포 안정성

| 항목 | 내용 |
|------|------|
| SLI | 배포 후 30분 내 rollback 없는 배포 비율 |
| SLO | 95% |

---

## 10. GitOps 설계

이 프로젝트는 GitOps 방식으로 Kubernetes 리소스를 관리합니다.

### 구성

```
Application Source Repo
        |
        v
GitOps Repo
        |
        v
ArgoCD
        |
        v
Kubernetes Cluster
```

### 관리 대상

- Namespace
- Application
- ConfigMap
- Secret reference
- Ingress / Gateway
- Service
- Deployment
- HPA
- ServiceMonitor
- PrometheusRule
- Grafana Dashboard

---

## 11. Runbook 전략

Runbook은 장애 대응을 표준화하기 위한 문서입니다.

각 Runbook은 다음 구조를 따릅니다.

1. 증상
2. 영향도
3. 주요 지표
4. 원인 후보
5. 진단 절차
6. 즉시 완화 조치
7. 근본 해결
8. 롤백 방법
9. 재발 방지
10. 관련 대시보드 / 쿼리

> Runbook은 향후 AI Agent와 RAG가 참조할 수 있도록 구조화합니다.

---

## 12. AI Agent 확장 방향

AI Agent는 초기부터 자동 복구를 수행하지 않으며, 단계적으로 확장합니다.

### Phase 1. Diagnosis Assistant

- Alert 입력
- 관련 Runbook 검색
- 원인 후보 제시
- 확인해야 할 지표 추천

### Phase 2. Read-only Command Assistant

- `kubectl get`
- `kubectl describe`
- log query
- metric query
- trace query

### Phase 3. Approval-based Remediation

- scale-out 제안
- rollback 제안
- config 수정 제안
- 승인 후 실행

### Phase 4. Limited Auto Remediation

- 사전에 허용된 조치만 자동 수행
- 모든 실행은 audit log 기록
- 실패 시 rollback

---

## 13. 포트폴리오에서 보여주고 싶은 역량

이 프로젝트를 통해 보여주고 싶은 역량은 다음과 같습니다.

- Spring Boot 기반 결제 도메인 설계
- WebFlux 기반 고성능 API 구조 이해
- Redis / Kafka / PostgreSQL 운영 이해
- Kubernetes 기반 서비스 배포
- ArgoCD 기반 GitOps 운영
- Prometheus / Grafana 기반 모니터링
- OpenTelemetry 기반 tracing 설계
- 장애 시나리오 기반 Runbook 작성
- SLI / SLO 기반 신뢰성 관리
- 운영 자동화와 AI Agent 확장 설계

---

## 14. 현재 진행 상태

### ✅ 완료

- 프로젝트 전체 방향 정의
- SRE 학습 데이터 정리
- GitOps 기본 구조 작성
- Observability Stack 일부 구성
- ArgoCD / AppProject / Application 구조 작성
- Jaeger / OpenTelemetry / Elasticsearch 구성 경험 정리

### 🔄 진행 중

- 결제 도메인 API 구현
- SRE Runbook 작성
- 장애 시나리오 문서화
- Grafana Dashboard 설계
- PrometheusRule 작성
- AI Agent MVP 설계

### 📋 예정

- 결제 API 부하 테스트
- 장애 주입 테스트
- Postmortem 작성
- RAG 기반 Runbook 검색
- AI Agent 진단 Assistant 구현
- 승인 기반 자동 조치 구현

---

## 15. 최종 목표

최종 목표는 단순한 결제 API 서버가 아닙니다.

> FinTech 결제 시스템을 운영한다고 가정하고,  
> 장애 탐지, 원인 분석, 대응, 복구, 재발 방지까지 포함한  
> **실무형 SRE 플랫폼을 구축하는 것.**

이 프로젝트는 백엔드 개발, 인프라 운영, Kubernetes, GitOps, Observability, SRE, AI Agent 자동화를 하나의 흐름으로 연결하는 포트폴리오입니다.