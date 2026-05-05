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

## 12. Logging

로그는 `key=value` 형태의 structured console logging을 사용합니다.

> **주의:** Spring Boot 4 / Logback 환경에서 지원하지 않는 conversion word를 사용하면 안 됩니다.

**수정된 이슈:**
```
[notEmpty] is not a valid conversion word
```

---

## 13. Safety Rules

이 서버에서 **절대 구현하면 안 되는 것:**

- AI automatic execution
- Direct kubectl execution
- Direct payment mutation action
- Rollback 없는 recommendation
- Verification 없는 recommendation
- `rag/docs`만으로 action decision
- Root cause 확정
- Human approval bypass

---

## 14. Current Status

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

**검증 완료:**

- `./gradlew test`
- Docker image build
- Private registry push
- Kubernetes image pull
- ArgoCD Synced / Healthy
- Kubernetes rollout success
- External health check UP

---

## 15. Next Work

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

---

## 16. Related Documents

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

## 17. Failure Modes & Troubleshooting

### 17.1 ImagePullBackOff

**증상:**
- Pod 상태: ImagePullBackOff

**원인:**
- containerd가 HTTP registry를 HTTPS로 접근

**해결:**
- /etc/containerd/certs.d hosts.toml 설정
- config_path 설정

---

### 17.2 Jenkins Git Push 실패

**증상:**
- Permission denied (publickey)

**원인:**
- Jenkins user SSH key 없음
- known_hosts 미설정

---

### 17.3 External Health Check 실패

**체크 순서:**

1. Pod 상태
2. Service 연결
3. VirtualService host
4. Gateway
5. cloudflared 상태

---

## 18. Operational Entry Points

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

## 19. System Boundaries (Critical)

이 서비스는 다음을 하지 않습니다:

- Kubernetes 리소스 직접 변경
- 외부 시스템 직접 mutation
- 자동 장애 복구 실행
- 결제 데이터 직접 수정

이 서비스는 반드시 다음을 전제로 동작합니다:

- GitOps 기반 배포
- Human-in-the-loop 승인
- Policy/Guardrail 검증