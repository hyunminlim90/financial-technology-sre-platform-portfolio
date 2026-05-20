# Runtime Operational Knowledge Graph Contract

`protocols/runtime-operational-knowledge-graph-contract.md`

---

## 1. 목적

이 문서는 FIN-SRE 플랫폼의 **Runtime Operational Knowledge Graph Layer**를 정의한다.

Operational Knowledge Graph의 목적은 단순 문서 연결이 아니다.

목적은 **Scenario + Runbook + Improvement + Preventive Design + Experiment + Evidence + Policy + Rollback + Verification**을 기반으로:

- 설명 가능하고
- 재현 가능하며
- 정량 검증 가능하고
- 운영 현실 기반이며
- 논문화 가능한

**Operational Reliability Knowledge Graph Runtime**을 formalization 하는 것이다.

---

## 2. 핵심 개념

Operational Knowledge Graph Runtime은 단순 RAG index가 아니다.

Knowledge Graph Runtime은 다음을 갖춘 **operational reliability knowledge runtime**이다.

- Evidence-aware
- Propagation-aware
- Rollback-aware
- Verification-aware
- Research-aware
- Human-governed

---

## 3. Canonical Knowledge Graph Definition

Knowledge Graph Runtime은 다음을 포함 가능.

| Runtime | 역할 |
|---------|------|
| Scenario Runtime | 장애 정의 |
| Runbook Runtime | 대응 전략 |
| Improvement Runtime | 제한 규칙 |
| Preventive Design Runtime | 구조적 예방 |
| Experiment Runtime | 실험 orchestration |
| Evidence Runtime | observability evidence |
| Research Runtime | 연구 orchestration |

---

## 4. Human Governance Rule

Knowledge Graph Runtime은 **Human Governance 제거 금지**.

**원칙:**
- AI는 knowledge relationship을 분석할 수 있다.
- Human이 operational interpretation과 governance를 승인한다.

**금지:**
- ❌ autonomous knowledge mutation
- ❌ AI-only operational truth declaration
- ❌ unreviewed knowledge override

---

## 5. Canonical Knowledge Lifecycle

Knowledge Graph Runtime은 canonical lifecycle을 가져야 한다.

```
KNOWLEDGE_CREATED
  → INDEXED
  → LINKED
  → RESOLVED
  → VALIDATED
  → VERSIONED
  → RESEARCH_ASSETIZED
  → ARCHIVED
```

---

## 6. Knowledge Relationship Rule

모든 Knowledge는 **explicit relationship**을 가져야 한다.

```
Scenario → Runbook → Improvement → Preventive Design → Experiment → Postmortem
```

**금지:** orphan operational knowledge

---

## 7. Canonical Knowledge Hierarchy Rule

Knowledge Graph Runtime은 **canonical hierarchy**를 따라야 한다.

**우선순위:**

```
Preventive Design
  > Improvement
  > Postmortem
  > Runbook
  > Scenario
  > rag/docs
```

**원칙:** 가장 restrictive하고 가장 안전한 knowledge가 우선된다.

---

## 8. Scenario Rule

Scenario는 **operational failure definition** 역할을 수행한다.

**필수 포함:** failure_mode, severity, impact_scope, propagation, metrics

---

## 9. Runbook Rule

Runbook은 **operational decision** 역할을 수행한다.

**필수 포함:** Action, Risk, Rollback, Verification, Decision Rule

---

## 10. Improvement Rule

Improvement는 **operational restriction** 역할을 수행한다.

예: retry amplification 발생 시 scale-out 금지

---

## 11. Preventive Design Rule

Preventive Design은 **structural prevention** 역할을 수행한다.

예: Redis-only idempotency 제거 → Redis + DB Unique Constraint

---

## 12. Experiment Rule

Experiment는 **operational validation** 역할을 수행한다.

포함: failure injection, propagation observation, rollback validation, verification validation

---

## 13. Evidence Rule

Evidence는 **operational truth source** 역할을 수행한다.

**허용:** metrics, logs, traces, timeline, verification result, rollback result

**금지:**
- fabricated operational evidence
- hallucinated runtime state

---

## 14. Knowledge Resolution Rule

Knowledge Runtime은 **cross-knowledge resolution** 가능해야 한다.

```
failure_mode → scenario → runbook → improvement → preventive design
```

---

## 15. Failure Mode Rule

Knowledge Graph Runtime은 **canonical failure_mode semantics**를 가져야 한다.

예: redis-timeout, kafka-consumer-lag, db-connection-pool-exhaustion, payment-api-high-latency

---

## 16. Propagation-aware Rule

Knowledge Graph Runtime은 **propagation-aware** 해야 한다.

예: dependency cascade, tail latency propagation, queue backlog propagation, retry amplification

---

## 17. Retry Amplification Rule

Knowledge Graph Runtime은 **retry amplification relationship**을 이해 가능해야 한다.

```
timeout → retry storm → queue overload → propagation expansion
```

---

## 18. Rollback-aware Rule

Knowledge Graph Runtime은 **rollback-aware** 해야 한다.

포함: rollback trigger, rollback timeout, rollback verification, rollback blast radius

---

## 19. Verification-aware Rule

Knowledge Graph Runtime은 **verification-aware** 해야 한다.

포함: queue stabilization validation, latency recovery validation, payment consistency validation

---

## 20. Research-aware Rule

Knowledge Graph Runtime은 **research-aware** 해야 한다.

포함: hypothesis, experiment, validation, quantitative result, paper candidate

---

## 21. Dataset-aware Rule

Knowledge Graph Runtime은 **dataset accumulation**을 지원 가능해야 한다.

예: incident dataset, rollback dataset, verification dataset, experiment dataset, propagation dataset

---

## 22. Research Assetization Rule

Knowledge Graph Runtime은 **research assetization**을 지원 가능해야 한다.

예: Experiment Report, Research Note, Quantitative Validation, Paper Draft

---

## 23. Reproducibility Rule

Knowledge Graph Runtime은 **reproducibility-aware** 해야 한다.

포함: experiment replay, policy replay, rollback replay, verification replay

> **원칙:** 재현 불가능한 knowledge lineage는 연구 자산이 아니다.

---

## 24. Knowledge Lineage Rule

Knowledge Graph Runtime은 **lineage-aware** 해야 한다.

```
Incident → Evidence → Postmortem → Improvement → Preventive Design
```

---

## 25. Timeline Governance Rule

Knowledge Graph Runtime은 **canonical operational timeline**을 유지해야 한다.

```
failure → propagation → mitigation → rollback → verification → stabilization → postmortem → improvement
```

---

## 26. Systems-Math Integration Rule

Knowledge Graph Runtime은 **Systems-Math 연결** 가능해야 한다.

예: Little's Law, queue utilization, retry amplification, tail latency propagation

**원칙:** Systems-Math는 knowledge interpretation layer다.

---

## 27. Quantitative Validation Rule

Knowledge Graph Runtime은 **정량 검증 가능**해야 한다.

예: MTTR, rollback success rate, verification latency, propagation reduction, stabilization latency

---

## 28. FinTech Safety Rule

FinTech 환경에서는 **payment consistency가 최우선**이다.

**금지:**
- unsafe payment replay
- duplicate payment corruption
- settlement inconsistency

**허용 가능:**
- sanitized operational evidence
- verified payment-safe experimentation

---

## 29. Blast Radius Rule

Knowledge Graph Runtime은 **blast radius awareness**를 가져야 한다.

범위: local / partial / cross-service / global

**원칙:** blast radius 증가 → stricter governance

---

## 30. SLO-aware Rule

Knowledge Graph Runtime은 **SLO-aware** 해야 한다.

포함: error budget burn, availability degradation, P99 latency degradation

---

## 31. Context-awareness Rule

Knowledge Graph Runtime은 **context-aware** 해야 한다.

포함: service, environment, traffic pattern, impact scope

---

## 32. Environment-aware Rule

Knowledge Graph Runtime은 **environment-aware** 해야 한다.

예: production / staging / sandbox

**원칙:** production → strictest knowledge governance

---

## 33. Severity-aware Rule

Knowledge Graph Runtime은 **severity-aware** 해야 한다.

예: SEV-1 / SEV-2 / SEV-3

**원칙:** higher severity → stricter knowledge governance

---

## 34. Policy-aware Rule

Knowledge Graph Runtime은 **policy-aware** 해야 한다.

예: approval policy, rollback policy, verification policy, visibility policy

---

## 35. Guardrail Rule

Knowledge Graph Runtime은 **Guardrail Runtime**을 통합해야 한다.

예: payment safety guardrail, rollback requirement guardrail, retry amplification guardrail

---

## 36. Unknown State Rule

Unknown 상태는 **restrictive governance** 적용.

예: missing metrics, partial observability, verification unavailable, rollback unavailable

**원칙:** Unknown → unsupported knowledge resolution blocked

---

## 37. Runtime Replay Rule

Knowledge Graph Runtime은 **replayable** 해야 한다.

예: experiment replay, rollback replay, verification replay, knowledge replay

---

## 38. Timeline Replay Rule

Knowledge lifecycle은 **replay 가능**해야 한다.

예: policy replay, verification replay, dataset replay, stabilization replay

---

## 39. Reliability State Rule

Knowledge Graph Runtime은 **reliability-aware state**를 가져야 한다.

```
HEALTHY / DEGRADED / UNSTABLE / STABILIZING / CONVERGED / FAILED
```

---

## 40. Confidence-aware Rule

Knowledge Graph Runtime은 **confidence-awareness**를 가져야 한다.

```
HIGH_CONFIDENCE / MEDIUM_CONFIDENCE / LOW_CONFIDENCE / UNKNOWN
```

**원칙:** LOW_CONFIDENCE → risky operational recommendation 제한

---

## 41. Runtime DTO Rule

Knowledge Graph Runtime은 **canonical DTO**를 가져야 한다.

예: KnowledgeNode, KnowledgeEdge, KnowledgeLineage, OperationalEvidence, ResearchArtifact

---

## 42. Explainability Rule

Knowledge Graph Runtime은 **explainable** 해야 한다.

포함:
- why recommendation selected
- why propagation inferred
- why rollback required
- why preventive design prioritized

**금지:** opaque knowledge resolution

---

## 43. Runtime Security Rule

Knowledge Graph Runtime은 **privileged operational layer**다.

**필수:** authenticated access, RBAC, audit logging, visibility control

**금지:**
- ❌ anonymous knowledge mutation
- ❌ unrestricted operational evidence access
- ❌ public raw operational exposure

---

## 44. Auditability Rule

Knowledge lifecycle은 **audit 가능**해야 한다.

포함:
- what knowledge linked
- what evidence resolved
- what policy applied
- what recommendation generated

---

## 45. Immutable Audit Rule

Knowledge audit는 **append-only** 해야 한다.

**금지:**
- ❌ audit overwrite
- ❌ hidden knowledge mutation
- ❌ invisible operational override

---

## 46. Runtime Failure Rule

Knowledge Graph Runtime failure는 **explicit** 해야 한다.

예: knowledge inconsistency, lineage corruption, verification unavailable, timeline desynchronization

**금지:** silent knowledge corruption

---

## 47. Visibility Classification Rule

Knowledge Artifact는 **visibility classification**을 가져야 한다.

허용: PUBLIC_PORTFOLIO / PRIVATE_RESEARCH / INTERNAL_OPERATION / PAPER_CANDIDATE / SANITIZED_EXPORT

---

## 48. Sanitization Rule

Knowledge export는 **sanitization 가능**해야 한다.

제거 대상: internal topology, customer payload, secret, token, internal IP

---

## 49. Runtime Metrics Governance Rule

Knowledge metric은 **low-cardinality**를 유지해야 한다.

**허용:** service, domain, severity, failure_mode, knowledge_type

**금지:** customer identifier, payment payload, trace payload dump

---

## 50. Operational Reality Rule

Knowledge Graph Runtime은 **현실 운영 기반**이어야 한다.

**허용:** real incident, real rollback, real observability, real verification, real propagation

**금지:** toy-only operational knowledge, synthetic-only operational claim

---

## 51. Academic Compatibility Rule

Knowledge Graph Runtime은 **학술 확장 가능**해야 한다.

지원 가능: knowledge lineage appendix, experiment reproducibility appendix, dataset reproducibility appendix, operational evidence appendix

---

## 52. Research Integrity Rule

Knowledge Graph Runtime은 **research integrity**를 보장해야 한다.

**금지:**
- fabricated operational evidence
- fabricated lineage
- unsupported research conclusion
- hidden negative experiment

---

## 53. Academic Extension Rule

장기적으로 다음 연구 방향을 지원 가능.

- Operational Reliability Knowledge Graph Systems
- rollback-aware operational graphs
- verification-aware reliability knowledge systems
- Human-in-the-loop operational knowledge governance

---

## 54. Anti-Pattern Rule

**금지:**
- ❌ orphan operational knowledge
- ❌ rollback 없는 recommendation
- ❌ verification 없는 operational claim
- ❌ opaque knowledge lineage
- ❌ unsupported propagation inference

---

## 55. Non-Goals

Knowledge Graph Runtime의 목표는 다음이 아니다.

- autonomous operational truth declaration
- opaque RAG automation
- ungoverned operational mutation
- unverifiable knowledge resolution

---

## 56. Canonical Runtime Layers

| Layer | 역할 |
|-------|------|
| Scenario | 장애 정의 |
| Runbook | 대응 전략 |
| Improvement | 제한 규칙 |
| Preventive Design | 구조적 예방 |
| Experiment | 실험 orchestration |
| Evidence | observability evidence |
| Research | 연구 orchestration |

---

## 57. Reliability Research Formalization Direction

현재 방향의 핵심은 단순 RAG 구축이 아니다.

목표: 운영 observability와 operational lineage를 다음 조건을 갖춘 **Operational Reliability Knowledge Graph Runtime**으로 formalization 하는 것이다.

- 설명 가능하고
- 재현 가능하며
- 정량 검증 가능하고
- 논문화 가능한

---

## 한 줄 핵심

> Runtime Operational Knowledge Graph의 목적은 단순 문서 연결이 아니다.
> → scenario, runbook, improvement, preventive design, experiment, evidence lineage를 통합하여 **재현 가능하고 검증 가능한 Reliability Knowledge Graph Runtime**으로 formalization 하는 것이다.