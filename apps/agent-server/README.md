# Agent Server

Human-in-the-loop 기반 FinTech SRE AI Agent 판단 서버입니다.

이 애플리케이션은 자동 복구 시스템이 아닙니다.  
AI는 운영 액션을 직접 실행하지 않고, 장애 상황에 대한 근거 기반 추천을 생성합니다.  
최종 실행 여부는 항상 운영자가 판단합니다.

---

## 1. Role

`agent-server`는 FinTech SRE Platform의 AI 판단 계층입니다.

주요 역할:

- 장애 상황 분석
- Scenario / Runbook / Postmortem / Improvement / Preventive Design 기반 추천
- ActionCommand 기반 구조화된 대응 후보 생성
- PolicyEngine 기반 위험 액션 차단
- Guardrail 기반 안전성 검증
- EvidenceContext 기반 판단 근거 구성
- Human approval 전제의 Operator Review
- Postmortem draft 생성
- Knowledge update review 생성
- LLM explanation 제공

---

## 2. Core Principles

이 서버는 다음 원칙을 반드시 지킵니다.

```text
AI does not execute.
AI recommends.
Human approves and executes.
Postmortem drives learning.
```

**운영 안전 원칙:**

- No Scenario → No Action
- `rag/docs` 기반 Action 결정 금지
- AI는 Root Cause를 확정하지 않음
- rollback 없는 Action 추천 금지
- verification 없는 Action 추천 금지
- Human approval은 항상 필요
- 결제 정합성, idempotency, duplicate payment 방지가 최우선

### Internal Operational Boundary

`/internal/admin/**` APIs are operational control interfaces.

They are NOT public product APIs.

These endpoints:
- must never be exposed publicly
- must never bypass Policy/Guardrail
- must remain human-triggered operational actions
- must require restricted operator access

Current operational policy:
- internal operator manual execution only
- no public browser direct access
- no wildcard ingress exposure

Forbidden:
- public exposure of `/internal/alerts/**`
- public exposure of `/internal/recommendations/**`
- public exposure of `/internal/admin/**`
- public exposure of `/internal/execution-plans/**`
- public exposure of `/internal/execution-results/**`
- public exposure of `/internal/verification-results/**`
- public exposure of `/internal/postmortem-drafts/**`
- public exposure of `/internal/learning-candidates/**`
- public exposure of `/internal/knowledge-promotion-plans/**`
- public exposure of `/internal/knowledge-updates/**`
- unauthenticated alert webhook ingestion
- unauthenticated recommendation history query
- unauthenticated internal operational API access
- alert-triggered automatic remediation

### Internal Operational API Security

Protected paths:

```text
/internal/admin/**
/internal/alerts/**
/internal/recommendations/**
/internal/incidents/**
/internal/execution-plans/**
/internal/execution-results/**
/internal/verification-results/**
/internal/postmortem-drafts/**
/internal/learning-candidates/**
/internal/knowledge-promotion-plans/**
/internal/knowledge-updates/**
```

All internal operational APIs are protected by a shared application-level WebFlux filter.

Default behavior:

`agent.internal.security.enabled=false` → protected paths return `404`

`enabled + missing/invalid secret` → `403`

`enabled + valid X-FIN-SRE-INTERNAL or Authorization: Bearer token` → pass

Secrets must be injected by environment variables.

They must never be committed to Git.

### Internal Alert APIs

```text
/internal/alerts/**
```

These APIs:
- are internal operational ingestion APIs
- are used by Alertmanager/Grafana/internal alert systems
- must not be exposed publicly
- must never trigger automatic remediation

### Internal Recommendation Query APIs

```text
/internal/recommendations/**
```

These APIs:
- are internal operational query APIs
- expose recommendation history and operational decision records
- must not be exposed publicly
- must require restricted operator access
- must not contain full alert payloads or sensitive customer data

### Internal Execution Plan APIs

```text
/internal/execution-plans/**
```

These APIs:
- are internal dry-run planning APIs
- expose execution plan documents for final human review
- must not be exposed publicly
- must not execute any operational action
- must require restricted operator access

### Internal Execution Result APIs

```text
/internal/execution-results/**
```

These APIs:
- are internal operator record APIs
- store what a human executed outside agent-server
- must not be exposed publicly
- must not execute any operational action
- must require restricted operator access

Verification results are human-reviewed operational observations.

Verification does not automatically resolve incidents.

Incident lifecycle management and re-analysis are separate boundaries.

Incident lifecycle transitions are human-controlled.

Allowed incident states:

```text
OPEN
MITIGATING
STABILIZING
RESOLVED
REOPENED
ESCALATED
```

Re-analysis candidates represent operational signals
that additional investigation may be required.

Re-analysis candidates do not automatically:
- re-run DecisionEngine
- generate recommendations
- reopen incidents
- execute remediation

Postmortem drafts are generated as human-review artifacts.

They do not represent final root cause analysis.

Human verification is required before postmortem knowledge can become learning knowledge.

Postmortem drafts require explicit human review.

Approved drafts may become learning candidates,
but are not automatically ingested into RAG systems.

AI-generated postmortem drafts must not become
operational truth without human validation.

Learning candidates are operational review artifacts.

They represent approved knowledge-update candidates,
but are not automatically merged into:
- scenarios
- runbooks
- preventive-designs
- RAG systems
- Git repositories

Knowledge promotion review is a final human review boundary before knowledge update planning.

APPROVED_FOR_PROMOTION does not:
- modify portfolio repository files
- create Git commits
- create pull requests
- trigger RAG ingestion
- update Qdrant

Knowledge promotion plans describe how humans should update portfolio knowledge.

They do not:
- modify files
- create Git commits
- create pull requests
- trigger RAG ingestion
- update Qdrant

Knowledge update application records are operational audit records.

They track:
- which knowledge files were updated
- which Git commit/PR applied the changes
- which incident triggered the update

agent-server never directly modifies portfolio repositories.

The operational lifecycle is validated by end-to-end governance tests.

The E2E lifecycle verifies:
- human approval boundaries
- append-only operational records
- dry-run execution planning
- no automatic remediation
- no automatic Git modification
- no automatic RAG ingestion

## Governance Observability

The agent-server exposes governance lifecycle metrics through Micrometer.

Metrics are intended for operational visibility only.

They must not trigger automatic remediation.

Examples:

- `fin_sre_recommendation_created_total`
- `fin_sre_recommendation_approval_decision_total`
- `fin_sre_execution_plan_created_total`
- `fin_sre_human_execution_result_total`
- `fin_sre_verification_result_total`
- `fin_sre_incident_lifecycle_transition_total`
- `fin_sre_learning_candidate_created_total`
- `fin_sre_knowledge_update_applied_total`

Prometheus scraping must be internal-only.

## Governance Dashboard Query Model

The governance dashboard query model provides internal read-only summaries for the SRE Console.

It summarizes:
- recommendations
- approval decisions
- execution plans
- human execution results
- verification results
- incident lifecycle states
- postmortem reviews
- learning candidates
- knowledge promotion plans
- knowledge update applications

This API is read-only and must not:
- trigger remediation
- query Prometheus directly
- modify GitOps state
- update RAG or vector stores

### Governance Dashboard Time Window

The governance dashboard summary supports time-window based filtering.

Examples:

```text
GET /internal/governance/dashboard/summary?window=1h
GET /internal/governance/dashboard/summary?window=24h
GET /internal/governance/dashboard/summary?window=7d
GET /internal/governance/dashboard/summary?from=2026-05-08T00:00:00Z&to=2026-05-08T23:59:59Z
```

The dashboard query model remains read-only and does not query Prometheus directly.

## Governance Dashboard Backlog

The governance dashboard backlog summarizes operational work queues.

Examples:
- pending recommendation approvals
- approved recommendations without execution plans
- execution results awaiting verification
- unresolved incidents
- postmortem drafts awaiting review
- learning candidates awaiting promotion review
- promotion plans awaiting application

This backlog model is read-only and operational only.

## Governance Dashboard Trends

The governance dashboard trend API provides time-bucketed operational governance trends.

Example:

```text
GET /internal/governance/dashboard/trends?window=24h&bucket=1h
GET /internal/governance/dashboard/trends?window=7d&bucket=1d
GET /internal/governance/dashboard/trends?from=2026-05-01T00:00:00Z&to=2026-05-08T00:00:00Z&bucket=1d
```

Trend API is read-only.

It does not:
- query Prometheus directly
- create Grafana dashboards
- trigger remediation
- modify GitOps state

## Governance Dashboard Risk Indicators

The governance dashboard risk indicator API provides operational governance risk signals.

Examples:
- approval reject rate
- verification regression rate
- incident reopen rate
- learning review backlog
- promotion plan backlog
- postmortem revision rate

This API is rule-based and read-only.

It does not:
- invoke LLMs
- trigger remediation
- query Prometheus directly
- modify GitOps or RAG state

## Governance Dashboard Overview

The overview API combines dashboard summary, backlog, trends, and risk indicators into a single internal read-only response for the SRE Console.

```text
GET /internal/governance/dashboard/overview?window=24h&bucket=1h
GET /internal/governance/dashboard/overview?window=7d&bucket=1d
```

The overview API does not:
- query Prometheus directly
- create Grafana dashboards
- trigger remediation
- modify GitOps state
- update RAG or Qdrant

### Governance Dashboard UI Contract

The UI contract for the React SRE Console is documented in:

```text
docs/governance-dashboard-ui-contract.md
```

The dashboard APIs are internal, read-only governance views.

## R2DBC Governance Persistence

The reactive persistence boundary for governance records is documented in:

```text
docs/r2dbc-governance-persistence.md
```

Recommendation governance persistence now supports a profile-based R2DBC boundary.

In-memory persistence remains available for non-R2DBC profiles.

### R2DBC Governance Schema

The governance persistence schema and index strategy are documented in:

```text
docs/r2dbc-governance-schema.md
src/main/resources/db/schema-governance.sql
```

The agent-server does not automatically mutate production database schema.

### Governance Retention and Archival Policy

Governance persistence retention policy is documented in:

```text
docs/governance-retention-archival-policy.md
```

The current phase does not introduce automatic deletion or archival jobs.

### Governance Query Failure Resilience Policy

Governance query resilience policy is documented in:

```text
docs/governance-query-resilience-policy.md
```

The current phase does not introduce timeout enforcement, circuit breakers, or automatic query behavior changes.

## Governance Query Optimization Metrics

The dashboard query optimization layer emits Micrometer metrics.

Metrics:
- `fin_sre_governance_query_optimized_total`
- `fin_sre_governance_query_fallback_total`
- `fin_sre_governance_query_failure_total`

These metrics are observability-only.

They do not:
- trigger remediation
- change query behavior automatically
- create alert rules
- mutate GitOps or RAG state

## Governance Query Timeout and Degraded Fallback

When enabled, optimized dashboard queries may be bounded by timeout.

If fallback is enabled, the dashboard may return degraded read-only responses.

Degraded dashboard responses must not:
- trigger remediation
- modify GitOps state
- update RAG or Qdrant
- hide query failure metrics

Alert webhook responses and audit logs may include batch-level summaries.

They must not store full webhook payloads.

Allowed summary fields:
- severity counts
- service counts
- domain counts
- status counts
- generated recommendation count
- duplicate suppression count
- rate-limit count

---

## 3. Architecture Position

**전체 플랫폼 내 위치:**

```
web-console / operator
        ↓
api-server
        ↓
agent-server
        ↓
Decision Engine
Policy Engine
Guardrail
RAG Knowledge Layer
LLM Explanation Layer
ActionLog / Postmortem / Review
```

**배포 흐름:**

```
portfolio/apps/agent-server
→ Jenkins CI
→ Docker image build
→ Private Registry
→ GitOps repo image tag update
→ ArgoCD sync
→ Kubernetes Deployment
→ Istio Gateway
→ Cloudflare Tunnel
```

---

## 4. Repository Boundary

이 디렉토리는 애플리케이션 소스 루트입니다.

```
apps/agent-server
```

**포함하는 것:**
- Spring WebFlux application source
- Gradle wrapper
- Dockerfile
- Application config
- Tests
- Application-specific docs

**포함하지 않는 것:**
- Kubernetes manifest
- ArgoCD Application
- GitOps deployment state
- Jenkins system configuration
- Registry data

> GitOps 리소스는 별도 레포에서 관리합니다.
> ```
> fin-tech-sre-platform-gitops/apps/agent-server
> ```

---

## 5. Main Components

### 5.1 Decision
- `DecisionEngine`
- `ScenarioMatcher`
- `RunbookCandidateSelector`
- `RecommendationAssembler`
- `DecisionReport`

### 5.2 Action DSL
- `ActionCommand`
- `ActionType`
- `ActionTarget`
- `RollbackCommand`
- `VerificationCommand`

### 5.3 Policy
- `PolicyEngine`
- `PaymentSafetyPolicyRule`
- `RollbackRequiredPolicyRule`
- `VerificationRequiredPolicyRule`
- `HumanApprovalRequiredPolicyRule`

### 5.4 Guardrail
- `ActionCommandGuardrail`
- `ExecutionBoundaryGuardrail`
- `RollbackVerificationGuardrail`
- `KnowledgeSourceGuardrail`
- `FinTechRiskGuardrail`

### 5.5 Evidence
- `EvidenceContext`
- `Evidence`
- `EvidenceSource`
- `EvidenceLayer`
- `StubEvidenceContextProvider`

### 5.6 Knowledge / RAG
- `KnowledgeSearchClient`
- `KnowledgeContextAssembler`
- `KnowledgeConsumerPolicyGuardrail`
- `KnowledgeLayeringValidator`
- `VectorKnowledgeSearchClient`
- `QdrantVectorSearchAdapter`
- `StubKnowledgeSearchClient`

### 5.7 Explanation
- `ExplanationService`
- `ExplanationPort`
- `LlmExplanationAdapter`
- `StubExplanationAdapter`

### 5.8 Operations
- `ActionLog`
- `IncidentLifecycle`
- `OperatorReview`
- `PostmortemDraft`
- `ImprovementCandidate`
- `KnowledgeUpdateReview`

---

## 6. Local Development

### 6.1 Requirements
- Java 17
- Gradle Wrapper
- Network access for dependency download

### 6.2 Test
```bash
./gradlew clean test
```

권장:
```bash
GRADLE_USER_HOME=.gradle-home ./gradlew clean test
```

### 6.3 Run Locally
```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

### 6.4 Health Check
```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness
```

정상 응답:
```json
{"groups":["liveness","readiness"],"status":"UP"}
{"status":"UP"}
{"status":"UP"}
```

---

## 7. Profiles

### `local`

로컬 개발용 프로파일입니다.

특징:
- 외부 Qdrant / OpenAI 호출 최소화
- stub 기반 동작
- 로컬 검증 중심

### `prod`

Kubernetes 배포용 프로파일입니다.

특징:
- Actuator readiness/liveness 활성화
- graceful shutdown 활성화
- structured logging
- 환경 변수 기반 설정

### `qdrant`

Enables:
- Qdrant retrieval client
- Qdrant vector upsert client

Purpose:
- real vector retrieval
- real vector ingestion

### `local-embedding`

Enables:
- local OpenAI-compatible embedding provider

Purpose:
- local open-source embedding runtime
- no commercial provider dependency

Examples:
- text-embeddings-inference
- Ollama embedding endpoint
- local embedding gateway

### `stub-embedding`

Development-only deterministic embedding provider.

Purpose:
- local testing
- deterministic embedding validation
- provider-independent tests

---

## 8. Docker Image

Dockerfile은 multi-stage build 구조입니다.

```dockerfile
FROM eclipse-temurin:17-jdk AS build
FROM eclipse-temurin:17-jre
```

**이미지 빌드:**
```bash
docker build -t agent-server:local .
```

**운영 이미지 태그 정책:**
```
172.30.1.105:5000/fin-tech-sre/agent-server:ci-<BUILD_NUMBER>
172.30.1.105:5000/fin-tech-sre/agent-server:<GIT_SHA>
```

> 배포에는 `GIT_SHA` 태그를 사용합니다.

---

## 9. CI/CD

**현재 CI/CD 흐름:**

```
portfolio push
→ Jenkins agent-server-ci
→ ./gradlew clean test
→ docker build
→ docker push
→ GitOps repo kustomization.yaml image tag update
→ GitOps commit/push
→ ArgoCD sync
→ Kubernetes rollout
```

Jenkins는 Kubernetes에 직접 배포하지 않습니다.

| 구분 | 내용 |
|---|---|
| **금지** | `kubectl apply` from Jenkins |
| | `kubectl rollout` from Jenkins |
| | manual deployment from Jenkins |
| **허용** | test |
| | docker build |
| | docker push |
| | gitops repo image tag update |

---

## 10. Kubernetes Runtime

현재 Kubernetes 배포는 GitOps repo에서 관리합니다.

| 항목 | 값 |
|---|---|
| Runtime namespace | `sre-agent` |
| Service | `agent-server.sre-agent.svc.cluster.local` |
| External domain | `https://ft-sre-agent.opentofu.click` |

**Health check:**
```bash
curl https://ft-sre-agent.opentofu.click/actuator/health
curl https://ft-sre-agent.opentofu.click/actuator/health/liveness
curl https://ft-sre-agent.opentofu.click/actuator/health/readiness
```

---

## 11. Observability

**Actuator endpoints:**

```
/actuator/health
/actuator/health/liveness
/actuator/health/readiness
/actuator/prometheus
```

> **주의:** `/actuator/prometheus`는 외부 공개 대상이 아닙니다.  
> Prometheus 내부 scrape 대상으로만 사용해야 합니다.

**현재 운영 보강 예정:**
- ServiceMonitor
- Grafana dashboard
- JVM metrics
- WebFlux HTTP metrics
- RED metrics
- alert rules

---

## 12. Runtime API vs Internal Admin API (Critical)

This service contains two different API categories.

### 12.1 Runtime Recommendation APIs

Public runtime APIs used by:
- web-console
- operator UI
- incident workflows

Examples:
```text
/api/recommendations
/api/incidents
/api/alerts
```

These APIs:
- may be externally routed
- are recommendation-oriented
- never execute operational actions directly

### 12.2 Internal Admin APIs

Operational control APIs:
```text
/internal/admin/**
```

Examples:
- knowledge ingestion
- vector reindex
- operational maintenance
- ingestion dry-run

These APIs:
- are NOT public APIs
- must remain internal-only
- must never be exposed through public wildcard ingress
- must require restricted operator access
- must always preserve Human-in-the-loop approval

Current operational direction:
- internal operator manual execution
- future internal SRE console backend integration
- no browser direct admin API access

---

## 13. Logging

로그는 `key=value` 형태의 structured console logging을 사용합니다.

> **주의:** Spring Boot 4 / Logback 환경에서 지원하지 않는 conversion word를 사용하면 안 됩니다.

**수정된 이슈:**
```
[notEmpty] is not a valid conversion word
```

---

## 14. Knowledge Ingestion Runtime

Knowledge ingestion is a controlled operational workflow.

**Current ingestion flow:**

```text
portfolio knowledge
→ scan
→ validation
→ chunking
→ embedding preparation
→ embedding provider
→ vector upsert (Qdrant)
```

**Supported knowledge sources:**

```text
scenarios/
runbooks/
postmortems/
improvements/
preventive-designs/
policies/
protocols/
rag/docs/
```

### Safety Rules

The ingestion pipeline enforces:

- `rag/docs` must never become actionable commands
- actionable knowledge requires scenario linkage
- runbooks require evidence metadata
- invalid knowledge is quarantined
- ingestion failures must fail safely
- vector failures must not crash runtime recommendation APIs

### Operational Principle

Knowledge ingestion:
- is NOT automatic runtime mutation
- is NOT AI-generated knowledge rewriting
- must remain human-triggered
- must preserve Git as source of truth

---

## 15. Safety Rules

이 서버에서 **절대 구현하면 안 되는 것:**

- AI automatic execution
- Direct kubectl execution
- Direct payment mutation action
- Rollback 없는 recommendation
- Verification 없는 recommendation
- `rag/docs`만으로 action decision
- Root cause 확정
- Human approval bypass
- public exposure of `/internal/admin/**`
- browser direct access to internal admin APIs
- public wildcard ingress exposing admin paths
- automatic knowledge ingestion execution
- AI-triggered ingestion/reindex execution

---

## 16. Current Status

**완료된 항목:**

| 영역 | 항목 |
|---|---|
| Core | ActionCommand DSL, PolicyEngine, Guardrail |
| Evidence | EvidenceContext |
| Knowledge | Knowledge Layering, RAG Consumer interfaces, Qdrant adapter structure |
| LLM | LLM explanation layer |
| Operations | ActionLog, IncidentLifecycle, OperatorReview, PostmortemDraft, ImprovementCandidate, KnowledgeUpdateReview |
| Infra | Actuator, Dockerfile, Jenkins CI, Private Registry push |
| Deployment | GitOps CD, ArgoCD deployment, Istio external routing, Cloudflare external health check |
| Knowledge Runtime | ingestion scanner, chunking, embedding preparation, vector upsert |
| Embedding | local embedding provider boundary, stub embedding provider |
| Vector | Qdrant retrieval boundary, Qdrant upsert boundary |
| Admin Runtime | internal admin ingestion API, audit logging, admin API hardening |

**검증 완료:**

- `./gradlew test`
- Docker image build
- Private registry push
- Kubernetes image pull
- ArgoCD Synced / Healthy
- Kubernetes rollout success
- External health check UP

---

## 17. Next Work

| 우선순위 | 작업 |
|---|---|
| 1 | Jenkins Freestyle → Jenkinsfile 전환 |
| 2 | GitOps direct push → PR 기반 승인 구조 전환 |
| 3 | Actuator 외부 노출 제한 |
| 4 | Prometheus ServiceMonitor 추가 |
| 5 | Grafana dashboard 추가 |
| 6 | Rollback runbook 작성 |
| 7 | Harbor 또는 HTTPS registry 검토 |
| 8 | Argo Rollouts 기반 canary 배포 검토 |
| 9 | Qdrant real integration |
| 10 | LLM provider real integration |
| 11 | Prometheus Alert → IncidentRecommendationRequest adapter |
| 12 | Alert ingestion operational flow |
| 13 | Internal SRE console backend |
| 14 | Cloudflare Access / Zero Trust integration |
| 15 | Operator approval workflow |

---

## 18. Related Documents

**Operational Security:**
- `policies/admin-api-hardening.md`
- `docs/admin-api-hardening.md`
- `policies/gitops-admin-api-exposure-policy.md`

**Portfolio:**
- `PORTFOLIO.md`
- `ARCHITECTURE.md`
- `DOMAIN-ARCHITECTURE.md`
- `IMPLEMENTATION-PLAN.md`
- `PAYMENT-API-DESIGN.md`

**Knowledge:**
```
scenarios/
runbooks/
postmortems/
improvements/
preventive-designs/
rag/docs/
protocols/
```

**GitOps:**
```
fin-tech-sre-platform-gitops/apps/agent-server
fin-tech-sre-platform-gitops/bootstrap/apps/agent-server.yaml
```

---

## 19. Failure Modes & Troubleshooting

### 19.1 ImagePullBackOff

**증상:**
- Pod 상태: ImagePullBackOff

**원인:**
- containerd가 HTTP registry를 HTTPS로 접근

**해결:**
- `/etc/containerd/certs.d` hosts.toml 설정
- config_path 설정

### 19.2 Jenkins Git Push 실패

**증상:**
- Permission denied (publickey)

**원인:**
- Jenkins user SSH key 없음
- known_hosts 미설정

### 19.3 External Health Check 실패

**체크 순서:**

1. Pod 상태
2. Service 연결
3. VirtualService host
4. Gateway
5. cloudflared 상태

---

## 20. Operational Entry Points

### 장애 발생 시 확인 순서

```bash
kubectl -n sre-agent get pod
kubectl -n sre-agent describe pod <pod>
kubectl -n sre-agent logs <pod>
kubectl -n sre-agent rollout status deploy/agent-server
```

### 외부 접근 확인

```bash
curl https://ft-sre-agent.opentofu.click/actuator/health
```

### 이미지 확인

```bash
kubectl -n sre-agent describe deploy agent-server | grep Image
```

---

## 21. System Boundaries (Critical)

이 서비스는 다음을 하지 않습니다:

- Kubernetes 리소스 직접 변경
- 외부 시스템 직접 mutation
- 자동 장애 복구 실행
- 결제 데이터 직접 수정

이 서비스는 반드시 다음을 전제로 동작합니다:

- GitOps 기반 배포
- Human-in-the-loop 승인
- Policy/Guardrail 검증

---

## Governance Dashboard Health

The dashboard health API provides a read-only health contract for the SRE Console.

```text
GET /internal/governance/dashboard/health
```

Possible states:

- `HEALTHY`
- `DEGRADED`
- `UNAVAILABLE`

The health API does not:

- query Prometheus directly
- trigger remediation
- modify GitOps state
- update RAG or Qdrant

## Governance Dashboard Health Metrics

The governance dashboard health status is exposed as a Micrometer gauge.

```text
fin_sre_governance_dashboard_health_status
```

Value mapping:

| Status | Value |
|---|---|
| HEALTHY | 0 |
| DEGRADED | 1 |
| UNAVAILABLE | 2 |

This metric is observability-only.

It does not:

- trigger remediation
- create Prometheus alert rules
- modify GitOps state
- update RAG or Qdrant

### Governance Dashboard Navigation Contract

The React SRE Console navigation contract is documented in:

```text
docs/governance-dashboard-navigation-contract.md
```

The contract defines read-only navigation from dashboard overview, backlog, trends, risk indicators, and health views to detail views.

### Governance Detail Query Contract

The governance detail query contract is documented in:

```text
docs/governance-detail-query-contract.md
```

The contract defines read-only detail payloads for incident, recommendation, learning, and knowledge update views.

### Governance Detail Query Resilience Policy

The governance detail query resilience policy is documented in:

```text
docs/governance-detail-query-resilience-policy.md
```

The current phase defines read-only resilience boundaries only and does not introduce degraded detail execution behavior yet.

## Governance Detail Aggregation

The governance detail APIs provide read-only aggregate views for the SRE Console.

Examples:

```text
GET /internal/governance/details/incidents/{incidentId}
GET /internal/governance/details/recommendations/{recommendationRecordId}
GET /internal/governance/details/knowledge-updates/{knowledgeUpdateApplicationId}
```

These APIs do not:

- approve recommendations
- execute plans
- trigger remediation
- modify GitOps state
- update RAG or Qdrant
- expose sensitive payloads

## Governance Detail Partial Response

When detail query resilience is enabled, child component query failures may produce partial degraded responses.

Primary record absence still returns `404`.

Partial detail responses are read-only and must not:

- trigger remediation
- approve recommendations
- execute plans
- modify GitOps state
- update RAG or Qdrant

## Governance Detail Health

The governance detail health API exposes the current detail query resilience mode.

```text
GET /internal/governance/details/health
```

Possible states:

- `HEALTHY`
- `DEGRADED_CAPABLE`
- `STRICT`

This API is read-only and does not query governance records.

## Governance Detail Health Metrics

The governance detail health status is exposed as a Micrometer gauge.

```text
fin_sre_governance_detail_health_status
```

Value mapping:

| Status | Value |
|---|---|
| HEALTHY | 0 |
| DEGRADED_CAPABLE | 1 |
| STRICT | 2 |

This metric is observability-only.

It does not:

- trigger remediation
- execute recommendations
- modify GitOps state
- update RAG or Qdrant

## Governance Detail Overview APIs

Lightweight governance detail preview APIs are available for the React SRE Console.

Examples:

```text
/internal/governance/details/overview/incidents/{incidentId}
/internal/governance/details/overview/recommendations/{recommendationRecordId}
```

These APIs are optimized for:

- preview panels
- overview cards
- search result previews
- lightweight rendering

They do not expose sensitive payloads or metadata.

## Governance Detail Overview Metrics

The lightweight governance detail overview APIs emit low-cardinality Micrometer metrics.

Metrics:

```text
fin_sre_governance_detail_overview_query_total
fin_sre_governance_detail_overview_degraded_total
```

Tags:

- `detailType`
- `result`
- `reason`

These metrics are observability-only and do not trigger remediation.

### Governance Console API Contract Summary

The full React SRE Console API contract summary is documented in:

```text
docs/governance-console-api-contract-summary.md
```

### Governance Console Search Contract

The governance console search contract is documented in:

```text
docs/governance-console-search-contract.md
```

Search is read-only and does not use LLM, vector search, or Qdrant in this phase.

## Governance Search Metrics

The governance search API emits low-cardinality Micrometer metrics.

Metrics:

```text
fin_sre_governance_search_query_total
fin_sre_governance_search_result_count
```

Tags:

- `type`
- `result`

The search query text and record identifiers must never be used as metric tags.

## Governance Search Partial Results

When search resilience is enabled, `type=ALL` search may return partial degraded results if one search component fails.

Single-type searches remain strict.

Partial search responses are read-only and must not:

- trigger remediation
- approve recommendations
- execute plans
- modify GitOps state
- update RAG or Qdrant

## Governance Search Degraded Metrics

Partial degraded search responses emit component-level metrics.

Metric:

```text
fin_sre_governance_search_degraded_total
```

Tags:

- `type`
- `reason`
- `component`

This metric is observability-only and does not trigger remediation.

## Governance Search Health

The governance search health API exposes the current search resilience mode.

```text
GET /internal/governance/search/health
```

Possible states:

- `HEALTHY`
- `DEGRADED_CAPABLE`
- `STRICT`

This API is read-only and does not execute search queries or scan governance records.

## Governance Search Health Metrics

The governance search health status is exposed as a Micrometer gauge.

```text
fin_sre_governance_search_health_status
```

Value mapping:

| Status | Value |
|---|---|
| `HEALTHY` | `0` |
| `DEGRADED_CAPABLE` | `1` |
| `STRICT` | `2` |

This metric is observability-only.

It does not:

- execute search queries
- query governance records
- trigger remediation
- modify GitOps state
- update RAG or Qdrant

## Governance Console Health

The Governance Console health API combines dashboard, detail, and search health into one read-only response.

```text
GET /internal/governance/console/health
```

Overall states:

- `HEALTHY`
- `DEGRADED`
- `ATTENTION_REQUIRED`

This API does not:

- query governance records
- execute search
- trigger remediation
- modify GitOps state
- update RAG or Qdrant

## Governance Console Health Metrics

The governance console overall health status is exposed as a Micrometer gauge.

```text
fin_sre_governance_console_health_status
```

Value mapping:

| Status | Value |
|---|---|
| `HEALTHY` | `0` |
| `DEGRADED` | `1` |
| `ATTENTION_REQUIRED` | `2` |

This metric is observability-only.

It does not:

- query governance records
- execute search
- trigger remediation
- modify GitOps state
- update RAG or Qdrant

## Governance Console Runtime Summary

The runtime summary API provides a read-only banner contract for the React SRE Console.

```text
GET /internal/governance/console/runtime-summary
```

Runtime modes:

- `NORMAL`
- `DEGRADED_READ_ONLY`
- `ATTENTION_REQUIRED`

This API also includes timeline runtime status through `timelineRuntime`.

This API does not query governance records, execute search, execute timeline aggregation, run cursor queries, or trigger remediation.

## Governance Console Runtime Summary Metrics

The governance console runtime mode is exposed as a Micrometer gauge.

```text
fin_sre_governance_console_runtime_mode
```

Value mapping:

| Runtime Mode | Value |
|---|---|
| `NORMAL` | `0` |
| `DEGRADED_READ_ONLY` | `1` |
| `ATTENTION_REQUIRED` | `2` |

This metric is observability-only.

It does not:

- query governance records
- execute search
- trigger remediation
- modify GitOps state
- update RAG or Qdrant

### Governance Console Runtime Banner Contract

The React SRE Console runtime banner contract is documented in:

```text
docs/governance-console-runtime-banner-contract.md
```

### Governance Console Frontend Integration Contract

The React SRE Console frontend integration contract is documented in:

```text
docs/governance-console-frontend-integration-contract.md
```

### Governance Timeline APIs

The Governance Console API contract includes Timeline APIs:

```text
/internal/governance/timeline
/internal/governance/timeline/health
/internal/governance/timeline/runtime-summary
```

### Governance Console Cursor Pagination Contract

The cursor pagination contract for append-only governance records is documented in:

```text
docs/governance-console-cursor-pagination-contract.md
```

### Governance Timeline Pagination Contract

The timeline-specific cursor pagination contract is documented in:

```text
docs/governance-timeline-pagination-contract.md
```

### Governance Timeline Query Contract

The governance timeline query/filter/event taxonomy contract is documented in:

```text
docs/governance-timeline-query-contract.md
```

### Governance Timeline Read Model Contract

The normalized governance timeline read model contract is documented in:

```text
docs/governance-timeline-read-model-contract.md
```

### Governance Timeline Mapping Contract

The governance timeline source-to-event mapping contract is documented in:

```text
docs/governance-timeline-mapping-contract.md
```

### Governance Timeline Aggregation Contract

The governance timeline aggregation contract is documented in:

```text
docs/governance-timeline-aggregation-contract.md
```

### Governance Timeline Resilience Contract

The governance timeline resilience contract is documented in:

```text
docs/governance-timeline-resilience-contract.md
```

### Governance Timeline Resilience Policy

The governance timeline resilience policy is documented in:

```text
docs/governance-timeline-resilience-policy.md
```

### Governance Timeline Metrics Contract

The governance timeline metrics contract is documented in:

```text
docs/governance-timeline-metrics-contract.md
```

### Governance Timeline Health Contract

The governance timeline health contract is documented in:

```text
docs/governance-timeline-health-contract.md
```

### Governance Timeline Runtime Contract

The governance timeline runtime contract is documented in:

```text
docs/governance-timeline-runtime-contract.md
```

### Governance Timeline Query Store Contract

See:

```text
docs/governance-timeline-query-store-contract.md
```

### Governance Timeline Projection Store Contract

See:

```text
docs/governance-timeline-projection-store-contract.md
```

### Governance Timeline R2DBC Projection Schema Contract

See:

```text
docs/governance-timeline-r2dbc-projection-schema-contract.md
```

### Governance Timeline Projection Writer Contract

See:

```text
docs/governance-timeline-projection-writer-contract.md
```

### Governance Timeline Projection Replay Contract

See:

```text
docs/governance-timeline-projection-replay-contract.md
```

### Governance Timeline Projection Retention Contract

See:

```text
docs/governance-timeline-projection-retention-contract.md
```

### Governance Timeline Projection Observability Contract

See:

```text
docs/governance-timeline-projection-observability-contract.md
```

### Governance Timeline Projection Consistency Contract

See:

```text
docs/governance-timeline-projection-consistency-contract.md
```

### Governance Timeline Projection Recovery Contract

See:

```text
docs/governance-timeline-projection-recovery-contract.md
```

### Governance Timeline Projection Bootstrap Contract

See:

```text
docs/governance-timeline-projection-bootstrap-contract.md
```

### Governance Timeline Projection Failure Taxonomy Contract

See:

```text
docs/governance-timeline-projection-failure-taxonomy-contract.md
```

### Governance Timeline Projection Evolution Contract

See:

```text
docs/governance-timeline-projection-evolution-contract.md
```

### Governance Timeline Projection Governance Boundary Contract

See:

```text
docs/governance-timeline-projection-governance-boundary-contract.md
```

### Governance Timeline Projection Final Consistency Checklist

See:

```text
docs/governance-timeline-projection-final-consistency-checklist.md
```

### Governance Timeline Projection-backed Aggregation Final Consistency Checklist

See:

```text
docs/governance-timeline-projection-backed-aggregation-final-consistency-checklist.md
```

### Governance Timeline Projection-backed Aggregation Architecture Phase Closure

See:

```text
docs/governance-timeline-projection-backed-aggregation-architecture-phase-closure.md
```

### Governance Timeline Architecture Phase Closure

See:

```text
docs/governance-timeline-architecture-phase-closure.md
```

### Governance Timeline Aggregation Routing Contract

See:

```text
docs/governance-timeline-aggregation-routing-contract.md
```

### Governance Timeline Operator Query Guide

See:

```text
docs/governance-timeline-operator-query-guide.md
```

For operator-level details, see:

```text
docs/governance-timeline-operator-query-guide.md
```

## Governance Timeline API Usage

The Governance Timeline API provides an internal, read-only, append-only operational audit view.

### Query timeline

```http
GET /internal/governance/timeline?limit=50
```

### Query older events

```http
GET /internal/governance/timeline?cursor={opaqueCursor}&direction=NEXT
```

### Query newer events

```http
GET /internal/governance/timeline?cursor={opaqueCursor}&direction=PREVIOUS
```

### Filter by event type

```http
GET /internal/governance/timeline?eventType=INCIDENT_TRANSITIONED
```

### Timeline health

```http
GET /internal/governance/timeline/health
```

### Timeline runtime summary

```http
GET /internal/governance/timeline/runtime-summary
```

Notes:

- Timeline APIs are internal-only.
- Timeline APIs are read-only and append-only audit views.
- Timeline API mutation methods are not supported.
- Cursors are opaque and must not be parsed by clients.
- Degraded timeline responses may return partial read-only data.
- Timeline APIs must not trigger remediation, approval execution, Kubernetes mutation, ArgoCD mutation, GitOps mutation, RAG ingestion, or Qdrant updates.
- Timeline metrics must use low-cardinality tags only.

### Governance Timeline Frontend Integration Contract

The React SRE Console timeline frontend integration contract is documented in:

```text
docs/governance-timeline-frontend-integration-contract.md
```

### React Governance Timeline Panel Contract

The React Governance Timeline panel contract is documented in:

```text
docs/react-governance-timeline-panel-contract.md
```

### React Governance Timeline Types Contract

The React Governance Timeline TypeScript contract is documented in:

```text
docs/react-governance-timeline-types-contract.md
```

### React Governance Timeline API Client Contract

See:

```text
docs/react-governance-timeline-api-client-contract.md
```

### React Governance Timeline State Contract

See:

```text
docs/react-governance-timeline-state-contract.md
```

### React Governance Timeline Rendering Contract

See:

```text
docs/react-governance-timeline-rendering-contract.md
```

### React Governance Timeline Interaction Contract

See:

```text
docs/react-governance-timeline-interaction-contract.md
```

### React Governance Timeline Accessibility Contract

See:

```text
docs/react-governance-timeline-accessibility-contract.md
```

### React Governance Timeline Implementation Readiness Checklist

See:

```text
docs/react-governance-timeline-implementation-readiness-checklist.md
```

### Governance Timeline API Contract

The governance timeline HTTP API surface contract is documented in:

```text
docs/governance-timeline-api-contract.md
```

### Governance Timeline Implementation Readiness Checklist

The governance timeline implementation readiness checklist is documented in:

```text
docs/governance-timeline-implementation-readiness-checklist.md
```

### Governance Timeline Final Consistency Checklist

See:

```text
docs/governance-timeline-final-consistency-checklist.md
```

### Runtime Operational Reliability Semantic Runtime Phase Closure

See:

```text
docs/runtime-operational-reliability-semantic-runtime-phase-closure.md
```

### Governance Search Resilience Policy

The governance search resilience policy is documented in:

```text
docs/governance-search-resilience-policy.md
```

The resilience policy now allows `type=ALL` search to return partial degraded results when enabled.
Single-type searches remain strict.

## Governance Detail Query Metrics

The governance detail APIs emit low-cardinality Micrometer metrics.

Metrics:

```text
fin_sre_governance_detail_query_total
fin_sre_governance_detail_query_not_found_total
```

Tags:

- `detailType`
- `result`

Supported detail types:

- `incident`
- `recommendation`
- `learningCandidate`
- `knowledgeUpdate`

These metrics are observability-only and do not trigger remediation.

## Governance Detail Degradation Metrics

Partial degraded detail responses emit Micrometer metrics.

Metric:

```text
fin_sre_governance_detail_degraded_total
```

Tags:

- `detailType`
- `reason`
- `component`

These metrics are observability-only.

Degraded detail responses do not:

- trigger remediation
- approve recommendations
- execute plans
- modify GitOps state
- update RAG or Qdrant
