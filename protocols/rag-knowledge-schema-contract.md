# protocols/rag-knowledge-schema-contract.md

---

## 1. 목적

이 문서는 AI-SRE 플랫폼에서 사용하는 모든 운영 지식의 RAG Retrieval 구조와 Knowledge Governance 규칙을 정의한다.

> RAG는 단순 검색 시스템이 아니다.  
> RAG는 **"운영 판단을 위한 지식 거버넌스 계층"** 이다.

---

## 2. 핵심 개념

플랫폼은 다음 두 계층의 Knowledge를 구분한다.

| 구분 | 역할 |
|------|------|
| **Primary Knowledge** | 운영 판단 기준 |
| **Secondary Knowledge** | 기술 이해 및 해석 보조 |

---

## 3. Primary Knowledge Rule (핵심)

Primary Knowledge는 운영 Recommendation 생성에 직접 사용된다.

대상:

```text
scenarios/
runbooks/
improvements/
preventive-designs/
protocols/
```

원칙:

```text
Primary Knowledge만이
Action reasoning에 사용될 수 있다.
```

---

## 4. Secondary Knowledge Rule

Secondary Knowledge는 기술 해석과 분석 보조에 사용된다.

대상:

```text
postmortems/
systems-math/
experiments/
rag/docs/
```

역할:

```text
- mechanism explanation
- observability interpretation
- recommendation explanation
- quantitative analysis
- validation evidence
```

중요:

```text
Secondary Knowledge 단독으로
Action을 생성해서는 안 된다.
```

---

## 5. Knowledge Priority Rule

Knowledge 간 충돌 시 다음 우선순위를 따른다.

```text
Preventive Design
> Improvement
> Runbook
> Scenario
> Experiment
> Postmortem
> Systems-Math
> rag/docs
```

원칙:

```text
가장 안전한 규칙이 항상 우선된다.
```

---

## 6. Retrieval Order Rule

AI는 다음 순서로 Retrieval 해야 한다.

```text
1. protocols/
2. scenarios/
3. runbooks/
4. improvements/
5. preventive-designs/
6. experiments/
7. postmortems/
8. systems-math/
9. rag/docs/
```

원칙:

```text
설명보다 안전 규칙이 우선된다.
```

---

## 7. No rag/docs-only Action Rule

```text
rag/docs 단독으로
운영 Action을 생성하는 것은 금지된다.
```

이유:

```text
rag/docs는:
mechanism explanation 계층이지
operational governance 계층이 아니다.
```

---

## 8. Metadata Contract Rule

모든 운영 문서는 다음 metadata를 포함해야 한다.

필수 항목:

```text
- knowledge_type
- failure_mode
- domain
- severity
- impact_scope
- environment
- tags
- related_*
```

---

## 9. Relationship Rule

RAG는 단순 파일명이 아니라 관계 기반으로 연결된다.

연결 기준:

```text
- failure_mode
- domain
- severity
- impact_scope
- tags
- related_* paths
- operational keywords
```

---

## 10. Failure Mode Rule

모든 Retrieval은 failure_mode를 중심으로 수행된다.

예:

```text
- redis-timeout
- kafka-consumer-lag
- db-connection-pool-exhaustion
- retry-amplification
```

원칙:

```text
failure_mode는
운영 지식 그래프의 핵심 연결 키이다.
```

---

## 11. Context-Aware Retrieval Rule

동일 failure_mode라도 context를 고려해야 한다.

컨텍스트:

```text
- production vs staging
- payment vs batch
- local vs global impact
- traffic spike vs steady traffic
```

원칙:

```text
동일 장애라도 context에 따라
추천 전략이 달라질 수 있다.
```

---

## 12. Retrieval Safety Rule

다음 상황에서는 degraded recommendation 상태를 생성해야 한다.

예:

```text
- low confidence
- partial observability
- missing metrics
- trace sampling loss
- inconsistent evidence
- retrieval conflict
```

원칙:

```text
Unknown을 추정으로 대체해서는 안 된다.
```

---

## 13. Systems-Math Retrieval Rule

Systems-Math는 다음 용도로 Retrieval 될 수 있다.

```text
- queue analysis
- retry amplification analysis
- latency reasoning
- propagation analysis
- SLO reasoning
```

중요:

```text
Systems-Math는 설명 계층이다.
Action 결정 계층이 아니다.
```

---

## 14. Experiment Retrieval Rule

Experiment는 validation evidence로 Retrieval 될 수 있다.

예:

```text
- rollback validation
- recommendation evaluation
- resilience verification
- migration safety
```

중요:

```text
Experiment 단독으로
Action을 생성해서는 안 된다.
```

---

## 15. Postmortem Retrieval Rule

Postmortem은 operational learning evidence로 Retrieval 될 수 있다.

예:

```text
- repeated incident
- failed rollback
- historical propagation pattern
- recommendation side effect
```

원칙:

```text
Postmortem은 경험 보정 계층이다.
```

---

## 16. Chunking Rule

Chunking은 관계 보존을 우선해야 한다.

필수:

```text
- front matter preservation
- relationship preservation
- failure_mode-aware chunking
- section-aware chunking
```

금지:

```text
❌ 의미 단절 chunk
❌ 관계 손실 chunk
❌ metadata 제거
```

---

## 17. Embedding Governance Rule

Embedding은 metadata를 보존해야 한다.

필수 포함:

```text
- failure_mode
- knowledge_type
- severity
- domain
- tags
- related_*
```

---

## 18. Retrieval Explainability Rule

AI는 다음을 설명 가능해야 한다.

```text
- 왜 이 문서를 선택했는가
- 어떤 evidence와 연결되었는가
- 어떤 rule이 recommendation을 제한했는가
```

원칙:

```text
설명 불가능한 recommendation은 위험하다.
```

---

## 19. Governance Timeline Integration Rule

다음 이벤트들은 governance timeline과 연결될 수 있다.

```text
- recommendation
- approval
- execution result
- rollback
- verification
- postmortem generation
- experiment execution
```

목적:

```text
- auditability
- replay compatibility
- operational governance
```

---

## 20. Observability Integration Rule

RAG는 observability evidence와 연결되어야 한다.

대상:

```text
- metrics
- logs
- traces
- alerts
- SLO/SLA
- deployment events
```

---

## 21. Human-in-the-loop Rule

최종 Recommendation은 반드시 Human Approval을 요구한다.

원칙:

```text
AI Recommendation ≠ Execution
```

금지:

```text
❌ autonomous remediation
❌ uncontrolled infrastructure mutation
❌ human bypass
```

---

## 22. Research Dataset Rule

RAG Knowledge Graph는 Reliability Engineering 연구 데이터셋으로 사용될 수 있다.

예:

```text
- recommendation accuracy
- rollback effectiveness
- recovery time
- propagation pattern
- SLO recovery
- retry amplification reduction
```

---

## 23. Anti-Pattern Rule

금지:

```text
❌ rag/docs-only recommendation
❌ observability 없는 retrieval
❌ metadata 없는 document
❌ relationship 없는 chunking
❌ explanation 불가능 recommendation
❌ human approval bypass
```

---

## 24. Non-Goals

RAG 시스템은 다음을 목표로 하지 않는다.

```text
- autonomous operation
- uncontrolled automation
- LLM-only decision making
- infrastructure auto mutation
- human replacement
```

---

## 25. 핵심 원칙

| 계층 | 역할 |
|------|------|
| **Primary Knowledge** | 운영 판단 |
| **Secondary Knowledge** | 설명 / 검증 / 학습 |
| **Observability** | Evidence |
| **Governance Timeline** | Auditability |
| **Human** | 최종 승인 |

---

## 🎯 한 줄 핵심

> RAG의 목적은 검색이 아니다.  
> → 운영 판단을 안전하게 연결하는 것이다.