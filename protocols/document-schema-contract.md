# protocols/document-schema-contract

## 1. 목적

이 문서는 AI-SRE 플랫폼의 모든 운영 문서를 Runtime Knowledge Object로 해석하기 위한 canonical schema, parsing, relationship, ingestion, retrieval, governance 규칙을 정의한다.

> **Markdown 문서는 단순 텍스트가 아니다.**  
> 문서는 "운영 판단 가능한 구조화된 Runtime Knowledge Object" 로 해석되어야 한다.

---

## 2. 핵심 개념

플랫폼은 문서를 다음 계층으로 변환한다.

```
Markdown Document
        ↓
Front Matter Parsing
        ↓
Structured Knowledge Object
        ↓
Chunking
        ↓
Embedding
        ↓
Knowledge Graph
        ↓
Retrieval
        ↓
AI Reasoning Context
```

---

## 3. Canonical Knowledge Rule

모든 운영 문서는 canonical schema를 따라야 한다.

**대상:**

- `scenario`
- `runbook`
- `improvement`
- `preventive-design`
- `postmortem`
- `experiment`
- `systems-math`
- `protocol`

**원칙:**

문서마다 다른 schema를 허용하지 않는다.

---

## 4. Canonical Front Matter Rule (핵심)

모든 Runtime Knowledge Document는 YAML Front Matter를 포함해야 한다.

**기본 형식:**

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

related_scenarios: []
related_runbooks: []
related_improvements: []
related_preventive_designs: []
related_postmortems: []
related_experiments: []
related_systems_math: []

tags:
  - redis
  - timeout
  - latency
---
```

---

## 5. Required Canonical Fields

모든 문서는 다음 필드를 기본적으로 포함해야 한다.

| Field | 설명 |
|---|---|
| `title` | 문서 제목 |
| `knowledge_type` | 문서 유형 |
| `domain` | 기술 도메인 |
| `failure_mode` | 장애 유형 |
| `environment` | prod/staging/dev |
| `severity` | 기본 severity |
| `impact_scope` | local/partial/global |
| `services` | 관련 서비스 |
| `related_*` | 관계 문서 |
| `tags` | 검색/연결 태그 |

---

## 6. Knowledge Type Enumeration Rule

**허용되는 `knowledge_type`:**

- `scenario`
- `runbook`
- `improvement`
- `preventive-design`
- `postmortem`
- `experiment`
- `systems-math`
- `protocol`
- `rag-doc`

**금지:**

- ❌ undocumented custom type
- ❌ ambiguous type

---

## 7. Failure Mode Normalization Rule

`failure_mode`는 canonical naming을 따라야 한다.

**형식:**

```
<domain>-<failure-type>
```

**예:**

- `redis-timeout`
- `db-connection-pool-exhaustion`
- `kafka-consumer-lag`
- `payment-api-high-latency`

**원칙:**

동일 현상에 여러 이름을 사용하지 않는다.

---

## 8. Runtime Parsing Rule

플랫폼은 문서를 Runtime Object로 parsing 할 수 있어야 한다.

**대상:**

- front matter
- markdown sections
- relationships
- embedded metadata

**출력 예시:**

- `ScenarioDocument`
- `RunbookDocument`
- `ExperimentDocument`
- `SystemsMathDocument`

---

## 9. Runtime DTO Mapping Rule

문서는 Runtime DTO로 매핑될 수 있어야 한다.

```
ScenarioDocument
  - failureMode
  - severity
  - propagation
  - metrics
  - alerts

RunbookDocument
  - actions
  - rollbackPlan
  - verificationPlan

ExperimentDocument
  - injectedFailure
  - rollbackProcedure
  - verificationProcedure
```

---

## 10. Relationship Extraction Rule

문서 관계는 Runtime Graph로 변환되어야 한다.

**예:**

```
Scenario
→ related_runbooks
→ related_experiments
→ related_systems_math
```

**출력:**

- `Knowledge Node`
- `Knowledge Edge`

---

## 11. Knowledge Graph Projection Rule

플랫폼은 문서를 Knowledge Graph로 projection 할 수 있다.

```
Scenario
↔ Runbook
↔ Improvement
↔ Preventive Design
↔ Experiment
↔ Systems-Math
↔ Postmortem
```

**원칙:**

운영 지식은 isolated document가 아니다.

---

## 12. Chunking Contract Rule

Chunking은 section-aware 해야 한다.

**허용:**

- section-aware chunking
- semantic chunking
- relationship-preserving chunking

**금지:**

- ❌ blind fixed-size chunking
- ❌ section boundary destruction
- ❌ relationship loss

---

## 13. Embedding Boundary Rule

Embedding은 문서 관계를 보존해야 한다.

**포함 가능:**

- `title`
- `section`
- `summary`
- `failure_mode`
- `domain`
- `tags`

**원칙:**

embedding 과정에서 운영 의미가 손실되면 안 된다.

---

## 14. Retrieval Compatibility Rule

Retrieval은 canonical schema 기반으로 수행되어야 한다.

**지원 기준:**

- `failure_mode`
- `domain`
- `severity`
- `impact_scope`
- `services`
- `related_*`
- `tags`

---

## 15. Retrieval Ordering Rule

**AI Retrieval 우선순위:**

```
protocol
→ scenario
→ runbook
→ improvement
→ preventive-design
→ postmortem
→ experiment
→ systems-math
→ rag/docs
```

**원칙:**

`rag/docs`는 판단 보조 계층이다.

---

## 16. RAG/docs Boundary Rule

`rag/docs`는 runtime operational action source가 아니다.

**허용:**

- ✔ mechanism explanation
- ✔ observability interpretation
- ✔ systems understanding

**금지:**

- ❌ direct action generation
- ❌ runbook override
- ❌ operational mutation decision

---

## 17. Systems-Math Schema Rule

Systems-Math는 canonical schema를 따른다.

```yaml
knowledge_type: systems-math
math_category: queue-theory
related_scenarios:
related_runbooks:
related_experiments:
```

---

## 18. Experiment Schema Rule

Experiment는 실행 가능한 validation contract여야 한다.

```yaml
knowledge_type: experiment

target_failure_mode:
rollback_required:
verification_required:
sandbox_only:
blast_radius:
```

---

## 19. Postmortem Schema Rule

Postmortem은 실제 incident evidence를 포함해야 한다.

```yaml
incident_id:
severity:
affected_services:
timeline_reference:
related_recommendations:
```

---

## 20. Governance Timeline Compatibility Rule

문서는 Governance Timeline과 연결될 수 있어야 한다.

**예:**

- `recommendation`
- `approval`
- `execution`
- `rollback`
- `verification`

---

## 21. Observability Link Rule

문서는 observability evidence와 연결될 수 있어야 한다.

**대상:**

- `metrics`
- `logs`
- `traces`
- `alerts`
- `SLO`
- `dashboard`

---

## 22. Evidence Link Rule

문서는 Evidence Context와 연결될 수 있어야 한다.

**예:**

- `related_metrics`
- `related_alerts`
- `related_traces`
- `related_logs`

---

## 23. Ingestion Pipeline Compatibility Rule

문서는 ingestion pipeline과 호환되어야 한다.

**지원 단계:**

- `parse`
- `normalize`
- `chunk`
- `embed`
- `index`
- `graph-link`
- `retrieve`

---

## 24. Graph DB Compatibility Rule

Schema는 future graph database 확장을 지원해야 한다.

**예:**

- `Neo4j`
- `JanusGraph`
- `PostgreSQL JSONB`
- `Qdrant metadata`

---

## 25. Projection Compatibility Rule

문서는 projection 기반 retrieval과 호환되어야 한다.

**예:**

- projection-backed query
- runtime fan-out
- relationship projection

---

## 26. Low Cardinality Rule

Metadata는 cardinality explosion을 유발하면 안 된다.

**금지:**

- ❌ random metadata explosion
- ❌ high-cardinality tags
- ❌ unstable identifiers

---

## 27. Explainability Rule

문서는 explainable reasoning을 지원해야 한다.

AI는 설명 가능해야 한다:

- 왜 recommendation이 생성되었는가
- 왜 rollback이 필요한가
- 왜 preventive design이 우선되는가

---

## 28. Replay Compatibility Rule

Knowledge Retrieval은 replay 가능해야 한다.

**예:**

- incident replay
- recommendation replay
- timeline reconstruction
- experiment replay

---

## 29. Human-in-the-loop Rule

문서 schema는 Human Governance를 제거하지 않는다.

**원칙:**

AI는 recommendation만 생성한다.

---

## 30. FinTech Safety Rule

Schema는 FinTech Safety를 우선한다.

**최우선 보호 대상:**

- payment integrity
- idempotency
- duplicate payment prevention
- settlement consistency

---

## 31. Security Rule

Knowledge ingestion은 내부 전용이어야 한다.

**원칙:**

- authenticated ingestion
- audit-protected pipeline
- internal-only indexing

---

## 32. Research Compatibility Rule

Schema는 Reliability Research를 지원할 수 있어야 한다.

**예:**

- experiment validation
- recommendation analysis
- rollback effectiveness
- queue saturation analysis
- tail latency analysis

---

## 33. Anti-Pattern Rule

**금지:**

- ❌ free-form undocumented schema
- ❌ ambiguous failure_mode
- ❌ relationship-less document
- ❌ observability 없는 operational document
- ❌ rollback 없는 experiment
- ❌ unstructured postmortem
- ❌ rag/docs-only operational action

---

## 34. Non-Goals

Schema의 목표는 다음이 아니다.

- uncontrolled autonomous AI
- human-free operation
- ungoverned ingestion
- opaque recommendation system

---

## 35. 핵심 원칙

| 계층 | 역할 |
|---|---|
| Markdown | 원천 문서 |
| Front Matter | canonical metadata |
| Runtime DTO | runtime knowledge object |
| Knowledge Graph | 관계 연결 |
| Embedding | semantic retrieval |
| Governance Timeline | auditability |
| Human | 최종 책임 |

---

> 🎯 **한 줄 핵심**
>
> 운영 문서는 단순 Markdown이 아니다.  
> → AI가 안전하게 reasoning 가능한 **Runtime Knowledge Object**여야 한다.