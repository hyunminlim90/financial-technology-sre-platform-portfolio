# Financial Technology SRE Platform Architecture

> Knowledge-Governed Human-in-the-loop AIOps Architecture
> for FinTech Reliability Engineering and Operational Decision Systems

---

## Architecture Overview

이 시스템은 단순 검색 시스템 또는 자동화 시스템이 아니다.

```
Operational Decision System
+ Operational Knowledge Governance System
+ Reliability Validation System
+ Human-in-the-loop AIOps Platform
```

### System Identity

| 영역 | 역할 |
|------|------|
| SRE Platform | 장애 대응 / 운영 안정성 |
| Governed AIOps | 정책 기반 AI 판단 |
| Operational Knowledge System | 운영 지식 축적 / 검색 |
| Reliability Research Platform | 실험 / 검증 / 정량 분석 |

---

## Core Architecture Flow

```
[Alert]
    ↓
[Observability Evidence]
(metrics / logs / traces)
    ↓
[AI Agent]
    ↓
[RAG Knowledge Retrieval]
    ↓
[Decision Engine]
    ↓
[Policy / Guardrail Validation]
    ↓
[Recommendation]
    ↓
[Human Approval]
    ↓
[Human Execution]
    ↓
[Verification]
    ↓
[Incident Closure]
    ↓
[AI Postmortem Draft]
    ↓
[Human Validation]
    ↓
[Knowledge Accumulation]
    ↓
[Next Incident Improvement]
```

---

## Architecture Philosophy

### 1. AI Recommendation ≠ Execution

AI는 실행 엔진이 아니다.

```
AI    = 분석 / 판단 보조 / 근거 제공 / 추천
Human = 승인 / 실행 / 책임
```

**AI가 수행할 수 있는 것:** 장애 분석 / RAG 검색 / 위험도 평가 / rollback 제안 / verification 제안 / postmortem draft 생성 / experiment 결과 해석

**AI가 수행하지 않는 것:** kubectl 실행 / ArgoCD sync / GitOps mutation / scale-out 실행 / 데이터 변경 / 결제 상태 변경 / 자동 remediation

### 2. No Scenario → No Action

AI는 반드시 다음 계층을 통해 판단해야 한다.

```
Scenario → Runbook → Policy / Guardrail → Evidence → Human Approval
```

Scenario 없는 Action은 금지된다.

### 3. RAG/docs Alone Cannot Decide Action

RAG/docs는 기술 개념 설명용이다. 메커니즘 설명, metric 해석, 원인 분석 보조, systems-math grounding 용도로만 사용된다.

절대 불가:

```
rag/docs만 기반으로 Action 결정
rag/docs만 기반으로 Root Cause 확정
rag/docs만 기반으로 운영 조치 추천
```

### 4. Human-in-the-loop

모든 고위험 조치는 Human 승인 필요. (traffic shift / scale operation / retry policy 변경 / circuit breaker 변경 / cache bypass / rollback execution / experiment execution)

### 5. Safety First

```
결제 무결성
> 데이터 일관성
> rollback 가능성
> 장애 전파 방지
> 성능 최적화
```

---

## Operational Knowledge Architecture

이 플랫폼의 핵심은 **Operational Knowledge Graph**이다.

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

모든 문서는 서로 연결된다.

```
Stack ↕ Scenario ↕ Runbook ↕ Systems-Math ↕ Experiment ↕ Postmortem ↕ Preventive Design ↕ Improvement
```

### Primary Knowledge System

AI의 운영 판단 기준.

```
scenarios/             → 장애 정의
runbooks/              → 대응 절차
systems-math/          → 운영 정량 분석
experiments/           → 재현 / 검증 / 측정
postmortems/           → 실제 경험
preventive-designs/    → 구조적 예방
improvements/          → 개선 / 제한 / 정책 보완
```

### Secondary Knowledge System

기술 원리 / 학습 보조 계층.

```
rag/docs/00~24-stack/
rag/docs/100-deep-dive/micro-*
rag/docs/100-deep-dive/core-*
rag/docs/100-deep-dive/systems-math/
```

역할: 기술 개념 grounding / 메커니즘 설명 / systems-math 이해 보조 / metric 해석 보조

### Knowledge Priority

```
Policy / Guardrail
> Preventive Design
> Improvement
> Postmortem
> Runbook
> Scenario
> Systems-Math
> Experiment
> rag/docs
```

---

## 5-Layer Operational RAG Architecture

| Layer | Knowledge | 역할 | 핵심 질문 |
|-------|-----------|------|----------|
| Layer 1 | Scenario | 장애 정의 / propagation | 무슨 장애인가? |
| Layer 2 | Runbook | 대응 절차 / rollback | 무엇을 할 수 있는가? |
| Layer 3 | Improvement | 위험 행동 제한 | 무엇을 하면 안 되는가? |
| Layer 4 | Preventive Design | 구조적 예방 | 이 문제를 없애려면? |
| Layer 5 | Postmortem | 실제 경험 | 실제에서는 어떻게 되었는가? |
| Quantitative | Systems-Math | 정량 모델 | 왜 이런 현상이 발생하는가? |
| Validation | Experiment | 재현 / 검증 | 실제로 효과 있었는가? |
| Secondary | rag/docs | 기술 grounding | 메커니즘은 무엇인가? |

---

## Decision Engine

여러 운영 지식을 통합하여 **안전한 Recommendation**을 생성하는 계층.

### Decision Flow

```
1. Scenario 식별
2. Evidence 분석
3. Runbook 후보 생성
4. Policy / Guardrail 검증
5. Improvement 제한 적용
6. Preventive Design 우선 여부 판단
7. Systems-Math 기반 정량 해석
8. Experiment / Postmortem 기반 보정
9. 최종 Recommendation 생성
10. Human Approval 요청
```

### Decision Logic

| Step | 역할 | 설명 |
|------|------|------|
| 1 | Scenario | failure mode / severity / impact |
| 2 | Evidence | metrics / logs / traces |
| 3 | Runbook | 가능한 Action 후보 |
| 4 | Guardrail | 위험 Action 차단 |
| 5 | Improvement | 제한 규칙 적용 |
| 6 | Preventive Design | 구조적 해결 우선 여부 |
| 7 | Systems-Math | queue / latency / retry 분석 |
| 8 | Experiment / Postmortem | 실제 효과 기반 보정 |
| 9 | Recommendation | 최종 권고 생성 |
| 10 | Human Approval | 승인 / 실행 여부 |

---

## Systems-Math Architecture

Systems-Math는 단순 수학 문서가 아니라 **Operational Quantitative Analysis Layer**이다.

**연결 예시:**

```
Kafka Consumer Saturation
→ arrival rate > service rate
→ queue depth 증가
→ consumer lag 증가
→ tail latency 증가
→ payment delay 증가
```

**주요 개념:** Little's Law / queue utilization / percentile / variance / moving average / latency distribution / retry amplification / tail latency / arrival rate / service rate / exponential backoff

### Systems-Math Folder Structure

**Root Systems-Math** (`systems-math/`) — 운영 장애 설명 중심

```
kafka-consumer-saturation-math.md
retry-storm-math.md
queue-saturation-math.md
```

**Deep-dive Systems-Math** (`rag/docs/100-deep-dive/systems-math/`) — 개념 학습 중심

```
little-law.md / queue-utilization.md / percentile.md
variance.md / latency-distribution.md
```

---

## Experiment Architecture

`experiments/`는 **Reliability Validation Layer**이다.

**역할:** 장애 재현 / AI Recommendation 검증 / rollback 검증 / verification 검증 / recovery time 측정 / systems-math 검증

### Experiment Flow

```
Scenario
→ Failure Injection
→ Metrics / Logs / Traces 수집
→ AI Recommendation 평가
→ Rollback
→ Verification
→ Recovery Time 측정
→ Postmortem 축적
```

### Experiment Safety Boundary

**필수 조건:** Human-approved / sandboxed / bounded blast radius / rollback available / verification required

**금지:** production direct execution / unbounded chaos / automatic destructive remediation

---

## AI Agent Architecture

```
agent/
├── workflows/
├── tools/
├── guardrails/
├── policy/
├── retrieval/
└── evidence/
```

**AI Agent 책임:** 장애 분석 / RAG 검색 / Evidence correlation / Decision Engine 실행 / Recommendation 생성 / rollback 제안 / verification 제안 / Postmortem Draft 생성 / Experiment 결과 해석

**Guardrails:** 위험 Action 차단 / 고위험 Approval 강제 / 중복 Action 방지 / unsafe recommendation 차단 / rollback 없는 권고 차단 / verification 없는 권고 차단

---

## Governance Timeline Architecture

`agent-server`는 Governance Timeline read-model을 제공한다.

**현재 원칙:** read-only / append-only audit semantics / operator-facing / best-effort degraded / mutation prohibited

**Timeline 추적 항목:** recommendation / approval / execution plan / verification / incident lifecycle / postmortem / knowledge promotion / experiment result

### Projection-backed Timeline Architecture

```
RUNTIME_FAN_OUT    → production default
PROJECTION_BACKED  → future explicit mode
```

**Projection-backed Principles:** read-only / append-only / cursor compatible / projection-backed future mode / runtime fan-out default

---

## Learning Architecture

### Learning Loop

```
Incident
→ Postmortem
→ Improvement
→ Preventive Design
→ Experiment
→ Knowledge Accumulation
→ Next Incident Improvement
```

### Learning Principles

| Knowledge | 특성 |
|-----------|------|
| Runbook | 기준 절차 |
| Postmortem | 지속 축적 |
| Experiment | 검증 데이터 |
| Systems-Math | 정량 모델 |
| AI | 지속 보정 |

---

## Reliability Research Architecture

장기적으로 이 플랫폼은 **Reliability Engineering Research Platform** 방향으로 확장된다.

### Research Loop

```
Scenario
→ Experiment
→ Failure Injection
→ Observability Collection
→ AI Recommendation
→ Human Approval
→ Mitigation / Rollback
→ Result Measurement
→ Postmortem
→ Preventive Design
→ Next Experiment
```

### Research Questions

- Scenario / Runbook / Postmortem 기반 AI가 rag/docs-only AI보다 더 안전한가?
- rollback / verification 강제가 위험 Recommendation을 줄이는가?
- Postmortem 축적이 MTTR을 줄이는가?
- Systems-Math 기반 정량 분석이 장애 propagation 예측 정확도를 높이는가?

---

## System Layers

```
apps/        → Application Layer
infra/       → Infrastructure Layer
platform/    → SRE / Reliability Layer
rag/         → Knowledge / RAG Layer
agent/       → AI Agent / Governance Layer
```

---

## Safety Model

**Safety Priority:**

```
결제 무결성
> 데이터 일관성
> rollback 가능성
> 안정성
> 성능
```

**Execution Rule:** 모든 인프라 변경은 Human만 수행

**AI Restriction:** AI = Recommendation only

---

## Final Architecture Principle

이 플랫폼은 단순 AI 운영 자동화 시스템이 아니다.

```
Knowledge
+ Governance
+ Human Approval
+ Reliability Validation
+ Operational Safety
```

기반의:

> **Knowledge-Governed Human-in-the-loop AIOps Platform**

을 구축하는 것이다.