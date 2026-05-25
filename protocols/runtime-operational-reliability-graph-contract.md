# protocols/runtime-operational-reliability-graph-contract

## 1. 목적

이 문서는 FIN-SRE 플랫폼의 Runtime Operational Reliability Graph Layer를 정의한다.

Operational Reliability Graph Runtime의 목적은 단순 노드/엣지 시각화가 아니다.

목적은 다음을 기반으로:

- Incident
- Evidence
- Propagation
- Rollback
- Verification
- Knowledge Set
- Research Runtime
- Operational Lineage

설명 가능하고, 재현 가능하며, 정량 검증 가능하고, 운영 현실 기반이며, 논문화 가능한 **Operational Reliability Graph Runtime**을 formalization 하는 것이다.

---

## 2. 핵심 개념

Operational Reliability Graph Runtime은 단순 graph database가 아니다.

Operational Reliability Graph Runtime은 다음 속성을 가진 **operational reliability semantic graph runtime**이다.

- Propagation-aware
- Rollback-aware
- Verification-aware
- Causality-aware
- Research-aware
- Human-governed

---

## 3. Canonical Reliability Graph Definition

Reliability Graph는 다음 관계를 표현 가능해야 한다.

```
Incident
→ Evidence
→ Propagation
→ Recommendation
→ Rollback
→ Verification
→ Stabilization
→ Learning
→ Research Asset
```

---

## 4. Canonical Graph Node Types

| Node Type | 역할 |
|---|---|
| Incident | 장애 이벤트 |
| Evidence | metrics/logs/traces |
| Scenario | 장애 시나리오 |
| Runbook | 운영 절차 |
| Experiment | 실험 |
| Rollback | rollback action |
| Verification | 검증 |
| Recommendation | AI recommendation |
| Guardrail | runtime safety boundary |
| Policy | governance policy |
| Research Asset | 연구 자산 |

---

## 5. Canonical Graph Edge Types

| Edge Type | 의미 |
|---|---|
| CAUSED_BY | 원인 관계 |
| PROPAGATES_TO | 장애 확산 |
| VALIDATED_BY | 검증 |
| ROLLED_BACK_BY | rollback |
| GOVERNED_BY | policy/guardrail |
| DERIVED_FROM | lineage |
| EXPERIMENTED_BY | 실험 |
| REFERENCES | 문서 연결 |

---

## 6. Human Governance Rule

Reliability Graph Runtime은 Human Governance 제거 금지.

**원칙**

- AI는 graph inference와 recommendation 가능
- Human이 operational interpretation과 governance approval 수행

**금지**

- ❌ AI-only causality declaration
- ❌ unreviewed operational graph mutation
- ❌ unsupported propagation assertion

---

## 7. Canonical Reliability Flow Rule

Graph Runtime은 canonical operational flow 표현 가능해야 한다.

```
Signal
→ Evidence
→ Correlation
→ Incident
→ Propagation
→ Recommendation
→ Rollback
→ Verification
→ Convergence
→ Research Assetization
```

---

## 8. Incident Node Rule

Incident Node는 operational reliability 중심이어야 한다.

포함: severity, blast radius, propagation scope, rollback state, verification state

---

## 9. Evidence Node Rule

Evidence Node는 observability artifact 기반이어야 한다.

**허용:** metrics, logs, traces, timeline, events, verification result, rollback result

**금지:** fabricated evidence, hallucinated metrics

---

## 10. Propagation Edge Rule

Propagation Edge는 causality-aware 해야 한다.

```
retry storm → queue overload → DB saturation → payment degradation
```

**원칙:** Propagation은 단순 temporal sequence가 아니라, operational causality relation이다.

---

## 11. Retry Amplification Rule

Retry amplification은 canonical propagation graph 대상이다.

```
timeout → retry amplification → queue backlog → dependency cascade
```

---

## 12. Rollback Graph Rule

Rollback은 graph primitive다.

포함: rollback trigger, rollback validation, rollback convergence, rollback lineage

---

## 13. Verification Graph Rule

Verification은 correctness validation graph다.

포함: queue stabilization validation, latency validation, payment consistency validation

---

## 14. Convergence Graph Rule

Convergence는 graph state transition으로 표현 가능해야 한다.

```
UNSTABLE → STABILIZING → CONVERGED
```

**금지:** unstable recovery를 converged state로 기록

---

## 15. Reliability-aware Rule

Reliability Graph Runtime은 reliability-aware 해야 한다.

예: rollback reliability, verification reliability, propagation containment reliability

---

## 16. FinTech Safety Rule

FinTech 환경에서는 payment correctness 우선.

**금지:** duplicate payment normalization, unsafe rollback relation, verification 없는 recovery linkage

---

## 17. Human-in-the-loop Rule

고위험 operational edge는 Human Approval relation 필요 가능해야 한다.

예: DB failover, payment reconciliation, cross-region traffic shift

---

## 18. Guardrail-aware Rule

Reliability Graph Runtime은 Guardrail-aware 해야 한다.

예: payment safety guardrail, rollback mandatory guardrail, retry amplification guardrail

---

## 19. Systems-Math Rule

Reliability Graph Runtime은 Systems-Math 연결 가능해야 한다.

예: Little's Law, queue utilization, retry amplification, tail latency propagation

**원칙:** Systems-Math는 graph quantitative formalization layer다.

---

## 20. Knowledge Set Integration Rule

Reliability Graph Runtime은 Knowledge Set과 연결되어야 한다.

```
Failure Mode → Scenario → Runbook → Experiment → Preventive Design
```

---

## 21. Research-aware Rule

Reliability Graph Runtime은 Research Runtime과 연결되어야 한다.

포함: hypothesis, experiment, validation, paper candidate

---

## 22. Dataset-aware Rule

Reliability Graph Runtime은 dataset accumulation 지원 가능해야 한다.

예: rollback dataset, verification dataset, propagation dataset, reliability graph dataset

---

## 23. Research Assetization Rule

Reliability Graph 결과는 research asset으로 연결 가능해야 한다.

예: Experiment Report, Reliability Analysis, Research Note, Paper Draft

---

## 24. Knowledge Graph Integration Rule

Operational Reliability Graph는 Knowledge Graph와 연결되어야 한다.

```
Scenario → Runbook → Experiment → Improvement → Preventive Design
```

---

## 25. Operational Memory Integration Rule

Reliability Graph Runtime은 Operational Memory와 연결되어야 한다.

예: historical rollback pattern, historical propagation pattern, historical false recovery

---

## 26. Operational Consistency Integration Rule

Reliability Graph Runtime은 Consistency Runtime과 연결되어야 한다.

```
verification mismatch → consistency degradation
```

---

## 27. Operational Topology Integration Rule

Reliability Graph Runtime은 Topology Runtime과 연결되어야 한다.

```
high dependency density → propagation amplification
```

---

## 28. Operational Lineage Integration Rule

Reliability Graph Runtime은 Lineage Runtime과 연결되어야 한다.

```
incident lineage → rollback lineage → verification lineage → recommendation lineage
```

---

## 29. Causal Analysis Integration Rule

Reliability Graph Runtime은 Causal Analysis와 연결되어야 한다.

```
retry storm causality → retry governance evolution
```

---

## 30. Runtime Replay Rule

Reliability Graph Runtime은 replayable 해야 한다.

예: incident replay, rollback replay, verification replay, propagation replay

---

## 31. Reproducibility Rule

Reliability Graph Runtime은 reproducible 해야 한다.

```
same topology + same evidence + same policy → same graph relation
```

---

## 32. Timeline Governance Rule

Reliability Graph Runtime은 chronology-aware 해야 한다.

```
failure → propagation → rollback → verification → stabilization → convergence
```

---

## 33. Context-awareness Rule

Reliability Graph Runtime은 context-aware 해야 한다.

포함: service, environment, traffic pattern, impact scope

---

## 34. Environment-aware Rule

Reliability Graph Runtime은 environment-aware 해야 한다.

예: production, staging, sandbox

**원칙:** production → strictest graph governance

---

## 35. Severity-aware Rule

Reliability Graph Runtime은 severity-aware 해야 한다.

예: SEV-1, SEV-2, SEV-3

**원칙:** higher severity → stricter causality governance

---

## 36. Policy-aware Rule

Reliability Graph Runtime은 policy-aware 해야 한다.

예: approval policy, rollback policy, verification policy, visibility policy

---

## 37. Unknown State Rule

Unknown 상태는 restrictive governance 적용.

예: missing metrics, partial observability, verification unavailable, rollback unavailable

**원칙:** Unknown → operational certainty 제한

---

## 38. Runtime DTO Rule

Reliability Graph Runtime은 canonical DTO를 가져야 한다.

예: `IncidentNode`, `PropagationEdge`, `RollbackNode`, `VerificationNode`, `ReliabilityRelation`

---

## 39. Explainability Rule

Reliability Graph Runtime은 explainable 해야 한다.

**포함:** why propagation occurred, why rollback required, why verification mandatory, why convergence failed

**금지:** opaque graph inference

---

## 40. Runtime Security Rule

Reliability Graph Runtime은 privileged operational layer다.

**필수:** authenticated access, RBAC, audit logging, visibility control

**금지**

- ❌ anonymous graph mutation
- ❌ unrestricted operational graph exposure
- ❌ public raw incident evidence exposure

---

## 41. Auditability Rule

Reliability Graph lifecycle은 audit 가능해야 한다.

포함: what evidence analyzed, what rollback validated, what verification completed, what graph relation inferred

---

## 42. Immutable Audit Rule

Reliability Graph audit는 append-only 해야 한다.

**금지**

- ❌ audit overwrite
- ❌ hidden graph mutation
- ❌ invisible causality override

---

## 43. Runtime Failure Rule

Reliability Graph Runtime failure는 explicit 해야 한다.

예: graph inconsistency, timeline inconsistency, verification unavailable, rollback unavailable

**금지:** silent operational graph corruption

---

## 44. Visibility Classification Rule

Reliability Graph Artifact는 visibility classification을 가져야 한다.

허용 분류: `PUBLIC_PORTFOLIO`, `PRIVATE_RESEARCH`, `INTERNAL_OPERATION`, `PAPER_CANDIDATE`, `SANITIZED_EXPORT`

---

## 45. Sanitization Rule

Reliability Graph export는 sanitization 가능해야 한다.

**제거 대상:** internal topology, customer payload, secret, token, internal IP, financially sensitive evidence

---

## 46. Runtime Metrics Governance Rule

Reliability Graph metric은 low-cardinality 유지해야 한다.

**허용:** service, domain, severity, failure_mode, graph_relation

**금지:** customer identifier, payment payload, trace payload dump

---

## 47. Academic Compatibility Rule

Reliability Graph Runtime은 학술 확장 가능해야 한다.

지원 가능: reproducibility appendix, experiment appendix, dataset appendix, operational evidence appendix

---

## 48. Research Integrity Rule

Reliability Graph Runtime은 research integrity 보장해야 한다.

**금지:** fabricated graph relation, fabricated propagation model, unsupported causality conclusion, hidden contradictory evidence

---

## 49. Long-term Graph Evolution Rule

Reliability Graph Runtime은 장기 graph evolution 지원 가능해야 한다.

예: rollback lineage evolution, verification evolution, propagation evolution, Human Approval evolution

---

## 50. Academic Extension Rule

장기적으로 다음 연구 방향 지원 가능.

예: Operational Reliability Graph Systems, causality-aware operational graphs, verification-aware reliability graphs, Human-in-the-loop operational graph governance

---

## 51. Anti-Pattern Rule

**금지**

- ❌ propagation 없는 graph relation
- ❌ rollback 없는 recovery relation
- ❌ verification 없는 convergence declaration
- ❌ systems-math 없는 quantitative relation
- ❌ evidence 없는 causality assertion

---

## 52. Non-Goals

Reliability Graph Runtime의 목표는 다음이 아니다.

- 단순 graph visualization
- AI-only causality inference
- unverifiable operational graph
- toy-level topology graphing

---

## 53. Canonical Runtime Layers

| Layer | 역할 |
|---|---|
| Incident Layer | 장애 노드 |
| Evidence Layer | observability linkage |
| Propagation Layer | 장애 확산 |
| Governance Layer | policy/guardrail linkage |
| Verification Layer | correctness validation |
| Research Layer | experiment/research linkage |

---

## 54. Reliability Research Formalization Direction

현재 방향의 핵심은 단순 graph visualization이 아니다.

목표는 운영 observability와 operational lineage를 다음 속성을 갖춘 **Operational Reliability Graph Runtime**으로 formalization 하는 것이다.

- 설명 가능하고
- 재현 가능하며
- 정량 검증 가능하고
- 논문화 가능한

---

**한 줄 핵심**

> Runtime Operational Reliability Graph의 목적은 단순 노드/엣지 시각화가 아니다.
> Incident / Evidence / Propagation / Rollback / Verification / Research Asset 관계를 causality, reproducibility, operational lineage 기반으로 formalization 하여 Operational Reliability 자체를 **semantic graph runtime**으로 구축하는 것이다.