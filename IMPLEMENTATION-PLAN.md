# Financial Technology SRE Platform Implementation Plan

> Knowledge-Governed Human-in-the-loop AIOps
> Implementation Plan for FinTech Reliability Engineering

---

## 1. 구현 목표

이 프로젝트의 목표는 단순 결제 API 서버 구현이 아닙니다.

핵심 목표는 다음 루프를 구축하는 것입니다.

```
장애 감지
→ Evidence 수집
→ AI 분석
→ Recommendation
→ Human Approval
→ Rollback / Verification
→ Postmortem
→ Experiment
→ Reliability Learning
```

```
FinTech SRE Platform
+ Governed AIOps Platform
+ Operational Knowledge Platform
+ Reliability Validation Platform
```

---

## 2. 구현 핵심 원칙

### 2.1 AI Recommendation ≠ Execution

**AI가 수행하는 것:** 분석 / 추천 / 설명 / 위험도 평가 / rollback 제안 / verification 제안

**절대 수행하지 않는 것:** kubectl execution / ArgoCD sync / GitOps mutation / payment state mutation / automatic remediation

모든 변경은 **Human 승인 후** 수행됩니다.

### 2.2 No Scenario → No Action

```
Scenario → Runbook → Policy / Guardrail → Evidence → Human Approval
```

Scenario 없는 Recommendation은 금지됩니다.

### 2.3 RAG/docs Alone Cannot Decide Action

RAG/docs는 기술 grounding / 메커니즘 설명 / systems-math 이해 보조 / metric 해석 용도입니다.

**금지:** rag/docs-only recommendation / rag/docs-only root cause / rag/docs-only operational action

### 2.4 Rollback / Verification Required

모든 Recommendation은 반드시 rollback / verification / risk / approval requirement를 포함해야 합니다.

---

## 3. Operational Knowledge Architecture

현재 구현의 핵심은 **Operational Knowledge Graph**입니다.

### Knowledge Hierarchy

```
Stack Documents
→ Scenario
→ Runbook
→ Systems-Math
→ Experiment
→ Postmortem
→ Preventive Design
→ Improvement
```

**Primary Knowledge** (AI의 운영 판단 기준)

```
scenarios/ / runbooks/ / systems-math/
experiments/ / postmortems/ / preventive-designs/ / improvements/
```

**Secondary Knowledge** (기술 개념 및 메커니즘 설명)

```
rag/docs/
rag/docs/100-deep-dive/
```

---

## 4. Phase 1 — Core FinTech Platform

실제 장애와 운영 상황이 발생 가능한 FinTech 기반 시스템 구축.

**apps/api-server** (Spring WebFlux Payment API)

- 결제 요청 생성 / 결제 상태 조회 / Idempotency Key 처리

**apps/worker** (Kafka Consumer)

- 비동기 결제 처리 / 외부 결제 Provider Mock 연동

**주요 기술:** Spring WebFlux / R2DBC / PostgreSQL / Redis / Kafka

**핵심 방향:** non-blocking / idempotency / duplicate payment prevention / event-driven

---

## 5. Phase 2 — Observability & Evidence Platform

Evidence 기반 운영 분석 환경 구축.

**Metrics:** Prometheus / Grafana / SLI / SLO / Kafka lag / Redis latency / DB pool saturation

**Logs:** Loki — traceId / paymentId / requestId

**Traces:** OpenTelemetry / Jaeger / Tempo — API → Redis → DB → Kafka tracing

> **핵심 방향:** Evidence-based Recommendation

---

## 6. Phase 3 — Scenario / Runbook / Systems-Math

AI가 운영 판단에 사용할 Operational Knowledge 구축.

**문서 생성 흐름:**

```
Stack 문서 정독
→ 대표 장애 유형 1개 선정
→ Scenario 작성
→ Runbook 작성
→ Systems-Math 작성
→ 하위 수학 개념 문서 작성
→ 문서 링크 연결
```

| 문서 | 역할 |
|------|------|
| Scenario | 장애 정의 / propagation / detection rule / impact scope |
| Runbook | 진단 절차 / 완화 절차 / rollback / verification / approval requirement |
| Systems-Math | 운영 현상 정량 분석 / failure propagation / queue / latency / retry amplification 분석 |

**핵심 개념:** Little's Law / queue utilization / percentile / variance / tail latency / arrival rate / service rate

---

## 7. Phase 4 — RAG Knowledge Layer

Operational Knowledge를 AI가 안전하게 retrieval 가능하도록 구성.

```
rag/
├── docs/
├── chunks/
├── embeddings/
├── metadata/
├── prompts/
└── pipelines/
```

**Retrieval Order:**

```
Scenario → Runbook → Policy / Guardrail → Evidence → Human Approval
```

**금지 규칙:** No Scenario → No Action / rag/docs-only decision 금지 / rollback 없는 recommendation 금지 / verification 없는 recommendation 금지

---

## 8. Phase 5 — AI Recommendation Workflow

Evidence + Operational Knowledge 기반 Recommendation 생성.

**흐름:**

```
Alert
→ Evidence Collection
→ Scenario Matching
→ Runbook Retrieval
→ Improvement / Preventive Retrieval
→ Systems-Math Retrieval
→ Experiment / Postmortem Retrieval
→ Recommendation Generation
→ Human Approval
```

**AI 출력 필수 항목:**

| 항목 | 설명 |
|------|------|
| Incident Summary | 장애 요약 |
| Evidence | metrics / logs / traces |
| Most Likely Cause | 가능성 높은 원인 |
| Recommended Action | 권장 조치 |
| Expected Effect | 기대 효과 |
| Risk | 위험도 |
| Rollback | 롤백 계획 |
| Verification | 검증 절차 |
| Approval Requirement | 승인 필요 여부 |

**AI 제한:** Recommendation only

---

## 9. Phase 6 — Governance Timeline Architecture

운영 판단 흐름 추적 및 operator-facing audit 제공.

**현재 구현 방향:** runtime fan-out + future projection-backed query path

**Timeline Principles:** read-only / append-only / operator-facing / best-effort degraded / mutation prohibited

**Projection-backed 방향:** cursor pagination / ordering semantics / projection store / routing mode / metrics / health summary / runtime summary

**Timeline 추적 항목:** recommendation / approval / verification / incident lifecycle / postmortem / experiment result

---

## 10. Phase 7 — Postmortem Learning Loop

운영 경험 축적 및 Recommendation 품질 개선.

**흐름:**

```
Incident 종료
→ Evidence 수집
→ AI Draft 생성
→ Human Validation
→ Root Cause 확정
→ Postmortem 저장
→ RAG 반영
→ 다음 Incident 개선
```

- Scenario / Runbook은 기준 문서
- Postmortem은 계속 축적
- AI는 과거 경험 기반 보정

---

## 11. Phase 8 — Experiment / Reliability Validation

AI Recommendation과 운영 대응 전략 검증.

**Experiment 역할:** Failure Injection / Recovery Measurement / Rollback Validation / Verification Validation / Recommendation Evaluation / Systems-Math Validation

**Experiment Flow:**

```
Scenario
→ Failure Injection
→ Metrics / Logs / Traces
→ Recommendation Evaluation
→ Rollback
→ Verification
→ Recovery Time 측정
→ Postmortem
```

**Experiment 필수 조건:** Human-approved / sandboxed / bounded blast radius / rollback available / verification required

---

## 12. Phase 9 — Protocol / Schema Governance

Operational Knowledge Schema 표준화.

```
protocols/
├── scenario-authoring-protocol.md
├── runbook-authoring-protocol.md
├── systems-math-authoring-protocol.md
├── experiment-authoring-protocol.md
└── postmortem-protocol.md
```

**효과:** 문서 품질 표준화 / AI retrieval consistency / RAG schema consistency / operational governance consistency

---

## 13. Phase 10 — Operator Console

Human-in-the-loop 운영 인터페이스 제공.

**구성:** Recommendation UI / Evidence UI / Rollback UI / Verification UI / Approval UI / Timeline UI / Runtime Summary UI

---

## 14. Phase 11 — Reliability Research Direction

Reliability Engineering / Governed AIOps 연구 기반 확보.

**연구 방향:** Knowledge-Governed AIOps / Human-in-the-loop Safety / Systems-Math Reliability Modeling / Experiment-based Validation / Postmortem Learning / Operational Governance

**연구 루프:**

```
Scenario
→ Experiment
→ Observability Collection
→ Recommendation
→ Human Approval
→ Rollback / Verification
→ Result Measurement
→ Postmortem
→ Preventive Design
→ 다음 Experiment
```

---

## 15. 핵심 리스크

| 리스크 | 대응 |
|--------|------|
| 과도한 AI Recommendation | Human-in-the-loop |
| 잘못된 RAG 오염 | Primary Knowledge 우선 |
| rollback 없는 Action | Guardrail 차단 |
| verification 없는 Recommendation | Guardrail 차단 |
| Scenario 부족 | No Scenario → No Action |
| Systems-Math 오해석 | Human Validation |
| Experiment 위험 확산 | sandbox / bounded blast radius |

---

## 16. 핵심 메시지

이 프로젝트는 **AI 자동 운영 시스템**이 아닙니다.

```
Knowledge
+ Governance
+ Human Approval
+ Systems-Math
+ Experiment Validation
+ Operational Safety
```

기반의:

> **Knowledge-Governed Human-in-the-loop AIOps Platform**

을 구현하는 것입니다.