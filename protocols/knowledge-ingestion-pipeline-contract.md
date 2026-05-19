# protocols/knowledge-ingestion-pipeline-contract

## 1. 목적

이 문서는 AI-SRE 플랫폼에서 운영 문서를 Runtime Knowledge Context로 변환하기 위한 ingestion, normalization, chunking, embedding, indexing, graph-linking, retrieval pipeline 규칙을 정의한다.

> **Ingestion Pipeline의 목적은 단순 indexing이 아니다.**  
> 목적은 "운영 지식을 AI reasoning 가능한 구조로 안전하게 변환하는 것" 이다.

---

## 2. 핵심 개념

플랫폼은 문서를 다음 lifecycle로 처리한다.

```
Markdown
        ↓
Parse
        ↓
Normalize
        ↓
Chunk
        ↓
Embed
        ↓
Index
        ↓
Knowledge Graph Link
        ↓
Retrieval
        ↓
Runtime Reasoning Context
```

---

## 3. Canonical Ingestion Rule

모든 문서는 canonical ingestion lifecycle을 따라야 한다.

**단계:**

- `DISCOVERED`
- `PARSED`
- `NORMALIZED`
- `CHUNKED`
- `EMBEDDED`
- `INDEXED`
- `GRAPH_LINKED`
- `RETRIEVAL_READY`

**원칙:**

중간 상태를 숨기지 않는다.

---

## 4. Discovery Rule

Pipeline은 문서를 deterministic 하게 발견해야 한다.

**대상:**

- `scenarios/`
- `runbooks/`
- `improvements/`
- `preventive-designs/`
- `postmortems/`
- `experiments/`
- `systems-math/`
- `protocols/`
- `rag/docs/`

**원칙:**

운영 문서는 ingestion 대상에서 누락되면 안 된다.

---

## 5. Parsing Rule

Pipeline은 문서를 structured object로 parsing 해야 한다.

**포함 대상:**

- front matter
- markdown sections
- headings
- lists
- relationships
- embedded metadata

**출력 예시:**

- `ParsedKnowledgeDocument`

---

## 6. Canonical Normalization Rule

모든 metadata는 canonical form으로 normalize 되어야 한다.

**포함 대상:**

- `failure_mode`
- `severity`
- `impact_scope`
- `environment`
- `knowledge_type`
- `tags`

**예:**

```
Redis Timeout
redis_timeout
REDIS-TIMEOUT

→ redis-timeout
```

**원칙:**

동일 의미의 metadata fragmentation 금지

---

## 7. Schema Validation Rule

문서는 canonical schema validation을 통과해야 한다.

**검증 대상:**

- required fields
- `knowledge_type`
- `failure_mode`
- relationship paths
- `severity`
- `impact_scope`

**원칙:**

invalid operational knowledge는 indexing 금지

---

## 8. Chunking Rule (핵심)

Chunking은 section-aware 해야 한다.

**허용:**

- semantic chunking
- section-aware chunking
- relationship-preserving chunking

**금지:**

- ❌ blind fixed-size chunking
- ❌ heading destruction
- ❌ relationship loss
- ❌ rollback section fragmentation

---

## 9. Semantic Boundary Rule

Chunk는 semantic meaning을 유지해야 한다.

**예:**

- `Action`
- `Rollback`
- `Verification`

는 서로 분리되면 안 된다.

**원칙:**

운영 의미 손실 금지

---

## 10. Embedding Rule

Embedding은 operational semantics를 보존해야 한다.

**포함 가능:**

- `title`
- `summary`
- `failure_mode`
- `domain`
- `tags`
- `section title`

**원칙:**

embedding 과정에서 운영 의미가 사라지면 안 된다.

---

## 11. Embedding Boundary Rule

Embedding 대상은 canonical boundary를 가져야 한다.

**예:**

- `Scenario Boundary`
- `Runbook Boundary`
- `Experiment Boundary`
- `Systems-Math Boundary`

---

## 12. Vector Index Rule

Vector Index는 metadata filtering을 지원해야 한다.

**예:**

- `failure_mode`
- `knowledge_type`
- `severity`
- `impact_scope`
- `environment`

**지원 가능 예시:**

- `Qdrant`
- `pgvector`
- `OpenSearch`

---

## 13. Knowledge Graph Projection Rule

Ingestion은 Knowledge Graph projection을 생성할 수 있다.

```
Scenario
↔ Runbook
↔ Improvement
↔ Preventive Design
↔ Experiment
↔ Systems-Math
```

**출력:**

- `Knowledge Node`
- `Knowledge Relationship Edge`

---

## 14. Relationship Preservation Rule

문서 관계는 ingestion 과정에서 보존되어야 한다.

**예:**

- `related_runbooks`
- `related_experiments`
- `related_systems_math`

**원칙:**

관계 손실은 reasoning 품질 저하를 유발한다.

---

## 15. Retrieval Ordering Rule

Retrieval은 governance priority를 따라야 한다.

**우선순위:**

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

`rag/docs`는 보조 계층이다.

---

## 16. Runtime Context Assembly Rule

Retrieval 결과는 Runtime Context로 조립될 수 있어야 한다.

**입력:**

```
retrieved documents
+
observability evidence
+
governance timeline
+
SLO signals
```

**출력:**

- `RecommendationContext`
- `EvidenceContext`
- `ExperimentContext`

---

## 17. Observability Link Rule

문서는 observability evidence와 연결될 수 있어야 한다.

**대상:**

- `metrics`
- `logs`
- `traces`
- `alerts`
- `dashboards`
- `SLO`

---

## 18. Systems-Math Integration Rule

Systems-Math는 runtime reasoning에 연결될 수 있다.

**예:**

- queue saturation reasoning
- retry amplification reasoning
- tail latency propagation

**원칙:**

Systems-Math는 Action 결정 계층이 아니다.

---

## 19. Experiment Integration Rule

Experiment retrieval은 validation evidence로 사용될 수 있다.

**예:**

```
과거 retry experiment 결과
→ recommendation confidence 보정
```

---

## 20. Recommendation Compatibility Rule

Ingestion pipeline은 Recommendation Engine과 호환되어야 한다.

**지원 대상:**

- `Scenario`
- `Runbook`
- `Improvement`
- `Preventive Design`
- `Experiment`
- `Systems-Math`

---

## 21. Governance Timeline Compatibility Rule

Retrieval 결과는 governance timeline과 연결될 수 있어야 한다.

**예:**

- recommendation replay
- rollback replay
- incident replay

---

## 22. Replay Compatibility Rule

Pipeline은 replay 가능한 구조를 유지해야 한다.

**예:**

- retrieval replay
- timeline replay
- experiment replay
- reasoning replay

**원칙:**

AI reasoning은 재현 가능해야 한다.

---

## 23. Explainability Rule

Pipeline은 explainable retrieval을 지원해야 한다.

**설명 가능 대상:**

- 왜 이 문서가 retrieval 되었는가
- 왜 이 experiment가 연결되었는가
- 왜 preventive design이 우선되었는가

---

## 24. Low Cardinality Rule

Metadata는 cardinality explosion을 유발하면 안 된다.

**금지:**

- ❌ random metadata
- ❌ unstable tags
- ❌ user-generated high-cardinality fields

---

## 25. FinTech Safety Rule

Pipeline은 FinTech Safety를 우선한다.

**최우선 보호 대상:**

- payment integrity
- idempotency
- duplicate payment prevention
- settlement consistency

**원칙:**

unsafe retrieval보다 결제 안전성이 우선된다.

---

## 26. Human-in-the-loop Rule

Pipeline은 Human Governance를 제거하지 않는다.

**원칙:**

AI는 recommendation만 생성한다.

---

## 27. Security Rule

Ingestion Pipeline은 내부 전용이어야 한다.

**필수:**

- authenticated ingestion
- audit-protected indexing
- internal-only retrieval

**금지:**

- ❌ public ingestion endpoint
- ❌ unauthenticated indexing

---

## 28. Failure Handling Rule

Pipeline 실패는 추적 가능해야 한다.

**예:**

- parse failure
- normalization failure
- embedding failure
- graph projection failure

**원칙:**

silent ingestion failure 금지

---

## 29. Degraded Retrieval Rule

부분 ingestion failure 상황에서도 degraded retrieval을 지원할 수 있다.

**예:**

- embedding unavailable
- partial graph projection
- stale chunk index

**원칙:**

Unknown을 추정으로 대체하지 않는다.

---

## 30. Research Compatibility Rule

Pipeline은 Reliability Research를 지원할 수 있어야 한다.

**예:**

- recommendation evaluation
- rollback analysis
- queue propagation analysis
- incident replay research

---

## 31. Future Runtime Rule

현재 pipeline은 retrieval 중심이다.

**장기적으로:**

```
Operational Knowledge Runtime
```

으로 발전할 수 있다.

**예:**

- runtime reasoning graph
- dynamic evidence merge
- adaptive retrieval

---

## 32. Anti-Pattern Rule

**금지:**

- ❌ blind chunking
- ❌ relationship destruction
- ❌ rag/docs-only action reasoning
- ❌ unverifiable retrieval
- ❌ opaque embedding
- ❌ ungoverned ingestion
- ❌ mutable indexing history

---

## 33. Non-Goals

Pipeline의 목표는 다음이 아니다.

- uncontrolled autonomous AI
- human-free operation
- opaque retrieval
- unsafe operational automation

---

## 34. 핵심 원칙

| 계층 | 역할 |
|---|---|
| Parse | 구조 해석 |
| Normalize | canonical metadata |
| Chunk | semantic boundary 유지 |
| Embedding | semantic retrieval |
| Knowledge Graph | 관계 연결 |
| Retrieval | runtime context 생성 |
| Governance Timeline | replay / auditability |
| Human | 최종 책임 |

---

> 🎯 **한 줄 핵심**
>
> Knowledge Ingestion의 목적은 문서를 저장하는 것이 아니다.  
> → 운영 지식을 reasoning 가능한 **Runtime Context**로 안전하게 변환하는 것이다.