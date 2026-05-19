# Financial Technology SRE Platform Roadmap

> Knowledge-Governed Human-in-the-loop AIOps Platform
> for FinTech Reliability Engineering and Operational Research

---

## 1. Roadmap Goal

이 로드맵은 단순 구현 계획이 아니다. 핵심 목표는 다음 루프를 구축하는 것이다.

```
설계
→ 구현
→ 관측
→ 검증
→ 학습
→ 정량 분석
→ Reliability Validation
→ 연구
```

이 프로젝트는 단순 포트폴리오가 아니라 다음 방향을 목표로 한다.

```
FinTech SRE Platform
+ Governed AIOps Platform
+ Operational Knowledge Platform
+ Reliability Engineering Research Platform
```

---

## 2. Long-term Direction

| 단계 | 방향 |
|------|------|
| 학사 | Computer Science / Software Engineering 기반 강화 |
| 석사 | Distributed Systems / Cloud / Reliability / AIOps 연구 |
| 박사 | Knowledge-Governed Human-in-the-loop AIOps 모델 제안 및 검증 |

---

## 3. Current Status

### Architecture / Governance

- ✔ Knowledge-Governed Architecture
- ✔ Human-in-the-loop Governance
- ✔ Decision Engine Architecture
- ✔ Policy / Guardrail Architecture
- ✔ Governance Timeline Architecture
- ✔ Projection-backed Timeline Architecture
- ✔ Runtime / Projection Routing Architecture

### Operational Knowledge Architecture

- ✔ Scenario / Runbook / Improvement / Preventive Design / Postmortem Layer
- ✔ Systems-Math Layer / Experiment Layer
- ✔ Knowledge Priority Rules / Retrieval Order Rules

### AI / RAG Architecture

- ✔ RAG Pipeline Architecture / Retrieval Flow
- ✔ Evidence-based Recommendation Flow / Postmortem Draft Flow
- ✔ Knowledge Governance Rules / rag/docs-only decision prohibition

### Reliability / Research Direction

- ✔ Systems-Math Quantitative Analysis Direction
- ✔ Experiment Validation Direction / Reliability Research Loop
- ✔ Human-approved Experiment Boundary
- ✔ Operational Knowledge Graph Direction

---

## 4. Strategic Roadmap

| Phase | 목표 |
|-------|------|
| Phase 1 | Core FinTech Platform |
| Phase 2 | Observability & Evidence |
| Phase 3 | Operational Knowledge System |
| Phase 4 | RAG / Knowledge Retrieval |
| Phase 5 | AI Recommendation Engine |
| Phase 6 | Governance & Timeline |
| Phase 7 | Learning / Postmortem |
| Phase 8 | Systems-Math Layer |
| Phase 9 | Experiment / Reliability Validation |
| Phase 10 | Operator Console |
| Phase 11 | Research / Quantitative Validation |
| Phase 12 | Portfolio / Thesis Expansion |

---

### Phase 1 — Core FinTech Platform

실제 장애와 운영 상황이 발생 가능한 FinTech 기반 시스템 구축.

**구성:** Payment API (Spring WebFlux) / PostgreSQL (R2DBC) / Redis / Kafka / Kubernetes / Istio / GitOps / Jenkins CI / ArgoCD CD

**핵심 원칙:** 결제 무결성 / idempotency / duplicate payment prevention / non-blocking architecture

---

### Phase 2 — Observability & Evidence

운영 상태를 관측 가능한 Evidence로 수집.

**구성:** Prometheus / Grafana / Loki / Jaeger / Alert Pipeline / Metrics / Logs / Traces Correlation

AI는 반드시 metrics, logs, traces 기반으로 판단한다. (**Evidence-based Recommendation**)

---

### Phase 3 — Operational Knowledge System

AI 판단의 기준이 되는 운영 지식 구축.

**Primary Knowledge:**

```
scenarios/ / runbooks/ / systems-math/
experiments/ / postmortems/ / preventive-designs/ / improvements/
```

**Secondary Knowledge:**

```
rag/docs/00~24-stack/
rag/docs/100-deep-dive/
```

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

> **핵심 제한:** 문서 하나당 대표 장애 유형 1개. duplicate scenario / retrieval ambiguity / rag chunk explosion 방지 목적.

---

### Phase 4 — RAG / Knowledge Retrieval

Operational Knowledge를 AI가 안전하게 검색 가능하도록 구성.

**구성:** Chunking / Embedding / Vector Search / Metadata / Retrieval Pipeline / Knowledge Priority / Retrieval Order / Evidence Linkage

**Retrieval Rule:**

```
Scenario → Runbook → Policy / Guardrail → Evidence → Human Approval
```

**금지 규칙:** rag/docs-only decision 금지 / No Scenario → No Action / rollback 없는 recommendation 금지 / verification 없는 recommendation 금지

---

### Phase 5 — AI Recommendation Engine

운영 지식 기반 Recommendation 생성.

**구성:** incident-recommendation-flow / RAG Retrieval / Decision Engine / Recommendation API / Guardrails / Risk Analysis / Rollback Proposal / Verification Proposal

**AI Boundary:** Recommendation only. kubectl execution / GitOps mutation / ArgoCD sync / automatic remediation / payment mutation 절대 수행하지 않음.

---

### Phase 6 — Governance & Timeline

운영 판단과 승인 흐름 추적.

**구성:** Governance Timeline / Recommendation Audit / Approval Tracking / Verification Tracking / Projection-backed Timeline / Runtime / Projection Routing / Operator-facing Runtime Summary

**핵심 원칙:** read-only / append-only / operator-facing / best-effort degraded / mutation prohibited

---

### Phase 7 — Learning / Postmortem

실제 장애 경험 축적.

**구성:** AI Postmortem Draft / Human Validation / Postmortem Accumulation / Learning Knowledge Expansion / Improvement / Preventive Design Derivation

**Learning Loop:**

```
Incident
→ Postmortem
→ Improvement
→ Preventive Design
→ Knowledge Expansion
→ Next Incident Improvement
```

---

### Phase 8 — Systems-Math Layer

운영 장애를 정량적으로 설명 가능한 계층 구축.

**핵심 개념:** Little's Law / queue utilization / arrival rate / service rate / latency distribution / tail latency / retry amplification / percentile / variance

**역할:** 운영 현상 정량 분석 / metrics interpretation / failure propagation explanation / reliability reasoning

**구조:**

```
Root Systems-Math     → systems-math/                          (운영 장애 설명 문서)
Deep-dive Systems-Math → rag/docs/100-deep-dive/systems-math/ (개념 학습 문서)
```

---

### Phase 9 — Experiment / Reliability Validation

운영 대응 효과 검증.

**구성:** Failure Injection / Reliability Validation / Rollback Validation / Recovery Time Measurement / Experiment Dataset / AI Recommendation Evaluation

**Experiment 필수 조건:** Human-approved / sandboxed / bounded blast radius / rollback available / verification required

**금지:** production destructive execution / unbounded chaos / automatic high-risk remediation

---

### Phase 10 — Operator Console

Human-in-the-loop 운영 인터페이스 제공.

**구성:** Recommendation UI / Evidence UI / Risk UI / Rollback UI / Verification UI / Approval UI / Postmortem Review UI / Timeline UI / Runtime Summary UI

---

### Phase 11 — Research / Quantitative Validation

Reliability Engineering / AIOps 연구 수행.

**Research Loop:**

```
Scenario
→ Experiment
→ Observability Collection
→ AI Recommendation
→ Human Approval
→ Mitigation / Rollback
→ Result Measurement
→ Postmortem
→ Preventive Design
→ Next Experiment
```

**핵심 연구 방향:** Knowledge-Governed AIOps / Human-in-the-loop Safety / Systems-Math Reliability Modeling / Experiment-based Validation / Postmortem Learning Effectiveness / Guardrail Effectiveness

**Research Questions:**

- Scenario / Runbook 기반 AI가 rag/docs-only AI보다 더 안전한가?
- rollback / verification 강제가 위험 Recommendation을 줄이는가?
- Postmortem 축적이 MTTR을 줄이는가?
- Systems-Math 기반 분석이 failure propagation 예측 정확도를 높이는가?

---

### Phase 12 — Portfolio / Thesis Expansion

실무 플랫폼 + 연구 플랫폼 + 논문 방향 연결.

**Portfolio 방향:** SRE / Platform Engineering / AIOps / Distributed Systems / Governance / Reliability Engineering

**Thesis 방향:**

```
Knowledge-Governed Human-in-the-loop AIOps
for FinTech Reliability Engineering
```

---

## 5. Implementation Priority

| 우선순위 | 항목 |
|---------|------|
| 1 | Core FinTech Platform |
| 2 | Observability |
| 3 | Scenario / Runbook |
| 4 | RAG Retrieval |
| 5 | Recommendation API |
| 6 | Governance Timeline |
| 7 | Postmortem |
| 8 | Systems-Math |
| 9 | Experiment |
| 10 | Operator Console |

---

## 6. Core Risks

| 리스크 | 결과 |
|--------|------|
| RAG 품질 부족 | 오판 |
| Evidence 부족 | 잘못된 Recommendation |
| Postmortem 품질 부족 | 잘못된 학습 |
| Improvement 부족 | 위험 행동 발생 |
| Human Approval 누락 | 사고 발생 |
| Systems-Math 부정확 | 잘못된 정량 해석 |
| Experiment Boundary 부족 | 위험한 검증 |

---

## 7. Mitigation Strategy

- Human-in-the-loop 강제
- No Scenario → No Action
- rag/docs-only decision 금지
- rollback 없는 recommendation 금지
- verification 없는 recommendation 금지
- high-risk approval 강제
- experiment sandbox 강제

---

## 8. Success Criteria

- 장애 발생 시 안전한 Recommendation 생성
- 위험 Recommendation 차단
- Evidence 기반 판단
- rollback / verification 포함
- Postmortem 기반 개선
- Experiment 기반 검증
- Systems-Math 기반 설명 가능성
- Governance Timeline 기반 감사 가능성

---

## 9. Final Roadmap Principle

이 프로젝트의 핵심은 **AI 자동 실행**이 아니다.

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

을 구축하는 것이다.
