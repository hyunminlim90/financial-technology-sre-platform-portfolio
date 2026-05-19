# RAG Knowledge System

> AI Agent + Human-in-the-loop 기반 SRE 운영 판단을 위한
> **RAG Knowledge Layer**

---

## 1. 목적

이 디렉터리는 AI Agent가 장애를 분석하고 대응을 추천하기 위해 사용하는 RAG 지식 체계를 관리한다.

> **RAG = 운영 판단을 위한 Knowledge Layer**

---

## 2. 핵심 원칙

```
Primary Knowledge   = 판단 기준
Secondary Knowledge = 이해 보조
```

> AI Agent는 항상 **Primary Knowledge를 먼저** 참고한다.

---

## 3. Knowledge 구조

### 3.1 Primary Knowledge

Primary Knowledge는 AI의 장애 대응 **판단 기준**이다.

```
scenarios/
runbooks/
improvements/
preventive-designs/
postmortems/
```

| Knowledge | 역할 |
|-----------|------|
| `scenarios/` | 장애 정의 |
| `runbooks/` | 표준 대응 절차 |
| `improvements/` | 위험 Action 제한 |
| `preventive-designs/` | 구조적 예방 설계 |
| `postmortems/` | 실제 장애 경험 |

### 3.2 Secondary Knowledge

Secondary Knowledge는 기술 이해를 위한 **보조 지식**이다.

```
rag/docs/
```

**역할:** 기술 원리 설명 / 장애 메커니즘 설명 / metric / log / trace 해석 보조

**주의:**
- `rag/docs`는 Action을 결정하지 않는다
- `rag/docs`는 Primary Knowledge를 override 할 수 없다

---

## 4. RAG 디렉터리 구조

```
rag/
├── docs/          # 기술 이해용 보조 지식
├── sources/       # 원천 자료
├── metadata/      # 문서 메타데이터
├── chunks/        # chunking 결과
├── embeddings/    # vector embedding 결과
├── prompts/       # RAG / Agent prompt
└── pipelines/     # indexing / ingestion pipeline
```

---

## 5. 문서 연결 기준

RAG는 파일명만으로 문서를 연결하지 않는다. 다음 기준으로 연결한다:

1. front matter metadata
2. `failure_mode`
3. `domain`
4. `related_*` 경로
5. `tags`
6. 본문 키워드

---

## 6. Front Matter 예시

```yaml
---
title: Redis Timeout Scenario
knowledge_type: scenario
domain: redis
failure_mode: redis-timeout
environment: production
severity: SEV-2
impact_scope: partial

services:
  - payment-api
  - redis
  - postgresql

related_runbooks:
  - runbooks/redis/timeout.md

related_improvements:
  - improvements/redis-timeout-idempotency-hardening.md

related_preventive_designs:
  - preventive-designs/redis-timeout-idempotency-fallback.md

tags:
  - redis
  - timeout
  - latency
  - idempotency

systems_math:
  - systems-math/kafka-consumer-saturation.md

related_experiments:
  - experiments/kafka-lag-recovery-validation.md

risk_level: HIGH
rollback_required: true
verification_required: true
human_approval_required: true
---
```

---

## 7. Retrieval Flow

AI Agent는 장애 분석 시 다음 순서로 검색한다:

| 순서 | 대상 |
|------|------|
| 1 | `protocols/` |
| 2 | `scenarios/` |
| 3 | `runbooks/` |
| 4 | `postmortems/` |
| 5 | `improvements/` |
| 6 | `preventive-designs/` |
| 7 | `rag/docs/` (필요 시) |

---

## 8. Decision Priority

Knowledge 간 충돌 시 다음 우선순위를 따른다:

```
Preventive Design
> Improvement
> Postmortem
> Runbook
> Scenario
> rag/docs
```

> 가장 안전한 규칙이 항상 우선된다.

---

## 9. AI Agent 사용 원칙

AI Agent는 RAG 결과를 기반으로 다음을 생성한다:

- Incident Summary
- Most Likely Cause 후보
- Evidence
- Recommended Action
- Risk
- Rollback Plan
- Verification

> **AI Agent는 실행하지 않는다.**
> `AI Recommendation ≠ Execution`

---

## 10. Human-in-the-loop

| 구분 | 역할 |
|------|------|
| **AI** | 분석, 추천, 위험 설명, rollback 제안 |
| **Human** | 판단, 실행, 검증, 승인 |

---

## 11. Postmortem Learning Loop

```
Incident 종료
→ AI Postmortem Draft 생성
→ Human 검증
→ postmortems/ 추가
→ RAG 반영
→ 다음 장애 대응 개선
```

- Runbook은 바꾸지 않는다
- Postmortem은 쌓는다
- AI는 경험을 기반으로 더 안전하게 판단한다

---

## 12. rag/docs 작성 원칙

`rag/docs`는 기술 이해 보조 문서다.

**작성 대상 예시:**

```
rag/docs/redis/latency-internals.md
rag/docs/database/connection-pool-mechanism.md
rag/docs/kafka/consumer-lag-mechanism.md
rag/docs/webflux/event-loop-model.md
rag/docs/retry/retry-amplification.md
```

**금지:**

- ❌ Action 정의
- ❌ Runbook 역할 침범
- ❌ Primary Knowledge override

---

## 13. Safety Rules

| 규칙 | |
|------|------|
| No Scenario | → No Action |
| Low Confidence | → No Risky Action |
| Rollback 없는 Action | → 추천 금지 |
| rag/docs 기반 Action 결정 | → 금지 |

---

## 14. 관련 프로토콜

```
protocols/rag-knowledge-layering-protocol.md
protocols/scenario-authoring-protocol.md
protocols/runbook-authoring-protocol.md
protocols/improvement-authoring-protocol.md
protocols/preventive-design-authoring-protocol.md
protocols/postmortem-protocol.md
protocols/rag-docs-authoring-protocol.md
```

---

## Operational Knowledge Graph

이 플랫폼의 핵심은 단순 Vector Search가 아니다.

핵심은 다음 계층 간의 연결 구조를 유지하는 **Operational Knowledge Graph**이다.

```
Stack
↕
Scenario
↕
Runbook
↕
Systems-Math
↕
Experiment
↕
Postmortem
↕
Preventive Design
↕
Improvement
```

AI는 단일 문서가 아니라, **연결된 운영 Knowledge 집합을 기반으로 reasoning** 해야 한다.

---

## Systems-Math Layer

Systems-Math는 단순 수학 학습 문서가 아니다.

**역할:**

- queue saturation explanation
- retry amplification analysis
- tail latency interpretation
- failure propagation reasoning
- reliability interpretation

Systems-Math는 다음과 연결된다.

```
Scenario ↕ Runbook ↕ Experiment ↕ Observability
```

**예시 개념:** Little's Law / queue utilization / percentile / variance / latency distribution / retry amplification

---

## Experiment Knowledge Layer

Experiment는 Recommendation과 Recovery 전략을 검증하기 위한 **Reliability Validation Layer**이다.

Experiment는 다음과 연결된다.

- Scenario / Runbook / Systems-Math / Observability / Postmortem

**Experiment 결과 평가 항목:**

- recovery time
- rollback effectiveness
- verification effectiveness
- recommendation safety

---

## Retrieval Governance

AI Retrieval은 반드시 다음 순서로 reasoning 해야 한다.

```
Scenario
→ Runbook
→ Improvement
→ Preventive Design
→ Postmortem
→ Systems-Math
→ Evidence
```

RAG/docs는 메커니즘 grounding 용도로만 사용된다.

**rag/docs-only reasoning은 운영 Action을 생성할 수 없다.**

---

## Evidence Correlation

AI Recommendation은 반드시 다음 간의 correlation 기반으로 생성되어야 한다.

- metrics / logs / traces / alert state / deployment events

**단일 metric만으로 root cause를 확정하지 않는다.**

---

## Degraded Retrieval Semantics

partial retrieval, embedding failure, metadata inconsistency, observability 부족 상황에서는:

AI는 certainty를 낮추고, **degraded recommendation 상태를 명시**해야 한다.

Unknown을 추정으로 대체해서는 안 된다.

---

## Non-Goals

이 플랫폼은 다음을 목표로 하지 않는다.

- autonomous operations
- LLM-only operational decisions
- automatic destructive remediation
- human bypass
- uncontrolled chaos execution