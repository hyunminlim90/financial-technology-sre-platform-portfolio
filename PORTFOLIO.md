# Financial Technology SRE Platform Portfolio

> Knowledge-Governed Human-in-the-loop AIOps Platform
> for FinTech Reliability Engineering and Operational Research

---

## 1. 프로젝트 개요

이 프로젝트는 단순한 결제 API 서버 구현 프로젝트가 아닙니다.

FinTech 결제 시스템을 기준으로 전체 운영 흐름을 구축하는 플랫폼입니다.

```
장애 탐지
→ 관측
→ 원인 분석
→ 대응
→ rollback
→ verification
→ postmortem
→ 재발 방지
→ 운영 지식 축적
```

```
FinTech SRE Platform
+ Governed AIOps Platform
+ Operational Knowledge Platform
+ Reliability Engineering Platform
```

---

## 2. 프로젝트 핵심 방향

이 플랫폼의 핵심 목표는 **AI 자동 실행**이 아닙니다.

```
Knowledge
+ Governance
+ Human Approval
+ Operational Safety
```

기반의 **Knowledge-Governed Human-in-the-loop AIOps** 플랫폼 구축입니다.

---

## 3. 핵심 철학

### 3.1 AI Recommendation ≠ Execution

AI는 실행 엔진이 아닙니다.

**AI가 수행하는 것:** 장애 분석 / 근거 제시 / 위험도 판단 / rollback 제안 / verification 제안 / postmortem draft 생성

**AI가 수행하지 않는 것:** kubectl 실행 / ArgoCD sync / GitOps mutation / payment state 변경 / 자동 remediation

모든 운영 조치는 **Human이 승인하고 실행**합니다.

### 3.2 No Scenario → No Action

AI는 반드시 다음 흐름을 따라야 합니다.

```
Scenario → Runbook → Policy / Guardrail → Evidence → Human Approval
```

Scenario 없는 Action은 금지됩니다.

### 3.3 RAG/docs Alone Cannot Decide Action

RAG/docs는 기술 개념 설명 계층입니다. 메커니즘 설명 / metric 해석 / systems-math grounding / 원인 분석 보조 용도로만 사용됩니다.

**금지:** rag/docs-only recommendation / rag/docs-only root cause decision / rag/docs-only operational action

---

## 4. Operational Knowledge Architecture

이 프로젝트의 핵심은 단순 RAG 저장소가 아니라 **Operational Knowledge Graph**입니다.

### Knowledge Layer Hierarchy

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

모든 문서는 서로 연결됩니다.

```
Stack ↕ Scenario ↕ Runbook ↕ Systems-Math ↕ Experiment ↕ Postmortem ↕ Preventive Design ↕ Improvement
```

---

## 5. Systems-Math Layer

`systems-math/`는 단순 수학 문서가 아니라 **Operational Quantitative Analysis Layer**입니다.

**연결 예시:**

```
Kafka Consumer Saturation
→ arrival rate > service rate
→ queue depth 증가
→ consumer lag 증가
→ tail latency 증가
→ payment delay 증가
```

**주요 개념:** Little's Law / queue utilization / arrival rate / service rate / latency distribution / tail latency / retry amplification / percentile / variance

### Folder Strategy

**Root Systems-Math** (`systems-math/`) — 운영 장애 설명 문서

```
kafka-consumer-saturation-math.md
retry-storm-math.md
queue-saturation-math.md
```

**Deep-dive Systems-Math** (`rag/docs/100-deep-dive/systems-math/`) — 하위 개념 학습 문서

```
little-law.md / queue-utilization.md / percentile.md / variance.md
```

---

## 6. Experiment / Reliability Validation

`experiments/`는 장애 재현 및 대응 효과 검증 계층입니다.

**역할:** Failure Injection / Rollback Validation / Recovery Time Measurement / AI Recommendation Evaluation / Systems-Math Validation / Postmortem Evidence

**Experiment 필수 조건:** Human-approved / sandboxed / bounded blast radius / rollback available / verification required

**금지:** production destructive execution / unbounded chaos / automatic destructive remediation

---

## 7. Governance Timeline

`agent-server`는 Governance Timeline read-model을 제공합니다.

**원칙:** read-only / append-only / operator-facing / best-effort degraded / mutation prohibited

**Timeline 추적 항목:** recommendation / approval / verification / incident lifecycle / postmortem / experiment result / knowledge promotion

---

## 8. Reliability Engineering 방향

이 프로젝트는 단순 백엔드 구현을 넘어 **Reliability Engineering** 관점에서 설계되었습니다.

**주요 방향:** SLI / SLO / tail latency / queue saturation / failure propagation / retry amplification / rollback safety / observability evidence

---

## 9. Observability Architecture

AI는 반드시 Evidence 기반으로 판단합니다.

**Evidence:** metrics / logs / traces

**수집 대상:** Prometheus / Grafana / Loki / OpenTelemetry / Jaeger / Tempo / Kafka Lag / Redis Latency / DB Pool Saturation

---

## 10. AI Agent Architecture

**핵심 구성:** RAG Retrieval / Decision Engine / Policy / Guardrail / Evidence Correlation / Recommendation Engine / Postmortem Draft

**AI 제한:** Recommendation only. infra mutation / payment mutation / GitOps mutation / automatic remediation 절대 수행하지 않음.

---

## 11. Research / Thesis Direction

이 프로젝트는 장기적으로 **학사 → 석사 → 박사** 방향까지 확장됩니다.

**연구 방향:** Knowledge-Governed AIOps / Human-in-the-loop Safety / Systems-Math Reliability Modeling / Experiment-based Validation / Postmortem Learning / Operational Governance

**Research Questions:**

- Scenario / Runbook 기반 AI가 rag/docs-only AI보다 더 안전한가?
- rollback / verification 강제가 위험 Recommendation을 줄이는가?
- Postmortem 축적이 MTTR을 줄이는가?
- Systems-Math 기반 분석이 failure propagation 예측 정확도를 높이는가?

---

## 12. 포트폴리오에서 보여주고 싶은 역량

- FinTech Reliability Engineering
- Kubernetes / GitOps 운영
- Observability 기반 운영 분석
- Governed AIOps
- Operational Knowledge Architecture
- Systems-Math 기반 정량 분석
- Experiment 기반 Reliability Validation
- Human-in-the-loop 운영 안정성
- Projection-backed Governance Timeline
- RAG 기반 운영 지식 구조

---

## 13. 최종 목표

최종 목표는 단순 API 서버 구축이 아닙니다.

```
운영 지식
+ 정책 기반 판단
+ Human Approval
+ 정량 분석
+ 실험 검증
+ 지속 학습
```

기반의:

> **Knowledge-Governed Human-in-the-loop AIOps Platform**

을 구축하는 것입니다.