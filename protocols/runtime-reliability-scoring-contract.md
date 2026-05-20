# protocols/runtime-reliability-scoring-contract.md

## 1. 목적

이 문서는 FIN-SRE 플랫폼의 Runtime Reliability Scoring Layer를 정의한다.

Reliability Scoring Runtime의 목적은 단순 health score 계산이 아니다.

목적은:

```
Incident Runtime
+ Rollback Runtime
+ Verification Runtime
+ Propagation Runtime
+ Experiment Runtime
+ Observability Runtime
+ Research Runtime
```

를 기반으로:

- 설명 가능하고
- 재현 가능하며
- 정량 검증 가능하고
- 연구 가능하며
- 운영 현실 기반인

**Operational Reliability Scoring Runtime**을 formalization 하는 것이다.

---

## 2. 핵심 개념

Reliability Scoring Runtime은 단순 metric aggregation engine이 아니다.

Scoring Runtime은:

- Evidence-aware
- Propagation-aware
- Rollback-aware
- Verification-aware
- Convergence-aware
- Human-governed

operational reliability evaluation runtime이다.

---

## 3. Canonical Reliability Definition

Reliability Runtime은 다음을 평가 가능해야 한다.

| Runtime | 역할 |
|---|---|
| Incident Reliability | 장애 안정성 |
| Rollback Reliability | rollback 신뢰성 |
| Verification Reliability | 검증 신뢰성 |
| Propagation Reliability | 확산 제어 |
| Experiment Reliability | 실험 안정성 |
| Operational Reliability | 전체 운영 안정성 |

---

## 4. Human Governance Rule

Reliability Scoring Runtime은 Human Governance 제거 금지.

**원칙:**

- AI는 reliability score recommendation을 생성할 수 있다.
- Human이 operational interpretation을 승인한다.

**금지:**

- ❌ autonomous operational ranking
- ❌ AI-only production reliability declaration
- ❌ unreviewed reliability override

---

## 5. Canonical Reliability Lifecycle

Reliability Runtime은 canonical lifecycle 가져야 한다.

**정상 흐름:**

```
SIGNAL_RECEIVED
→ EVIDENCE_COLLECTED
→ SCORE_COMPUTED
→ VALIDATION_COMPLETED
→ RELIABILITY_CLASSIFIED
→ ARCHIVED
```

**비정상 흐름:**

```
INSUFFICIENT_EVIDENCE
→ SCORE_BLOCKED
```

---

## 6. Reliability Score Rule

모든 Reliability Score는 explicit evidence 기반이어야 한다.

**허용:** metrics, logs, traces, timeline, verification result, rollback result

**금지:**

- fabricated score
- hallucinated operational confidence
- unsupported reliability classification

---

## 7. Reliability Classification Rule

Reliability Runtime은 canonical reliability classification 가져야 한다.

- HIGH_RELIABILITY
- MEDIUM_RELIABILITY
- LOW_RELIABILITY
- UNKNOWN_RELIABILITY

**원칙:** UNKNOWN → risky operational recommendation 제한

---

## 8. Confidence-aware Rule

Reliability Runtime은 confidence-awareness 가져야 한다.

- HIGH_CONFIDENCE
- MEDIUM_CONFIDENCE
- LOW_CONFIDENCE
- UNKNOWN

**원칙:** LOW_CONFIDENCE → high-risk operational action 금지

---

## 9. Rollback Reliability Rule

Rollback Runtime은 reliability score 계산 가능해야 한다.

- rollback success rate
- rollback latency
- rollback stabilization success
- rollback propagation containment

---

## 10. Verification Reliability Rule

Verification Runtime은 reliability score 계산 가능해야 한다.

- verification success rate
- verification latency
- verification consistency
- verification convergence

---

## 11. Propagation Reliability Rule

Propagation Runtime은 reliability score 계산 가능해야 한다.

- blast radius containment
- retry amplification suppression
- queue stabilization success
- dependency isolation effectiveness

---

## 12. Convergence Reliability Rule

Convergence Runtime은 reliability score 계산 가능해야 한다.

- stabilization latency
- oscillation frequency
- recovery convergence success

---

## 13. Experiment Reliability Rule

Experiment Runtime은 reliability score 계산 가능해야 한다.

- experiment reproducibility
- rollback reproducibility
- verification reproducibility

---

## 14. Quantitative Validation Rule

Reliability Runtime은 정량 검증 가능해야 한다.

- MTTR
- rollback success rate
- verification latency
- propagation reduction
- stabilization latency

---

## 15. Statistical Validation Rule

Reliability Runtime은 statistical validation 지원 가능해야 한다.

- confidence interval
- variance
- baseline comparison
- repeated experiment

**원칙:** single-run reliability claim 금지

---

## 16. Comparative Reliability Rule

Reliability Runtime은 comparative evaluation 가능해야 한다.

- Guardrail ON/OFF
- Human Approval ON/OFF
- Rollback Verification ON/OFF

---

## 17. SLO-aware Rule

Reliability Runtime은 SLO-aware 해야 한다.

- error budget burn
- availability degradation
- P99 latency degradation

---

## 18. Blast Radius Rule

Reliability Runtime은 blast radius awareness 가져야 한다.

예: local / partial / cross-service / global

**원칙:** blast radius 증가 → lower reliability score 가능

---

## 19. Propagation-aware Rule

Reliability Runtime은 propagation-aware 해야 한다.

- dependency cascade
- tail latency propagation
- queue backlog propagation
- retry amplification

---

## 20. Retry Amplification Rule

Reliability Runtime은 retry amplification risk 평가 가능해야 한다.

```
retry storm
→ queue overload
→ DB saturation
→ propagation expansion
```

---

## 21. FinTech Safety Rule

FinTech 환경에서는 payment consistency가 최우선이다.

**Reliability scoring 필수 포함:**

- duplicate payment risk
- idempotency stability
- settlement consistency

**금지:** unsafe payment reliability classification

---

## 22. Verification-aware Rule

Reliability Runtime은 verification-aware 해야 한다.

필수:

- queue stabilization validation
- latency recovery validation
- payment consistency validation

---

## 23. Rollback-aware Rule

Reliability Runtime은 rollback-aware 해야 한다.

필수:

- rollback trigger
- rollback timeout
- rollback verification
- rollback stabilization

---

## 24. Evidence-backed Rule

Reliability Runtime은 Evidence 기반이어야 한다.

**허용:** metrics, logs, traces, timeline, verification result, rollback result, experiment result

**금지:**

- fabricated reliability score
- hallucinated operational confidence

---

## 25. Research-aware Rule

Reliability Runtime은 research-aware 해야 한다.

- hypothesis
- validation
- experiment result
- policy comparison
- quantitative evaluation

---

## 26. Dataset-aware Rule

Reliability Runtime은 dataset accumulation 지원 가능해야 한다.

- reliability dataset
- rollback dataset
- verification dataset
- propagation dataset

---

## 27. Research Assetization Rule

Reliability 결과는 research asset으로 연결 가능해야 한다.

- Reliability Report
- Quantitative Validation
- Research Note
- Paper Draft

---

## 28. Reproducibility Rule

Reliability Runtime은 reproducibility-aware 해야 한다.

- experiment replay
- policy replay
- rollback replay
- verification replay

**원칙:** 재현 불가능한 reliability score는 신뢰 불가

---

## 29. Knowledge Graph Integration Rule

Reliability Runtime은 Knowledge Graph 연결 가능해야 한다.

```
Scenario
→ Runbook
→ Improvement
→ Preventive Design
→ Reliability Score
```

---

## 30. Systems-Math Integration Rule

Reliability Runtime은 Systems-Math 연결 가능해야 한다.

- Little's Law
- queue utilization
- retry amplification
- tail latency propagation

**원칙:** Systems-Math는 reliability interpretation layer다.

---

## 31. Reliability State Rule

Reliability Runtime은 reliability-aware state 가져야 한다.

- HEALTHY
- DEGRADED
- UNSTABLE
- STABILIZING
- CONVERGED
- FAILED

---

## 32. Context-awareness Rule

Reliability Runtime은 context-aware 해야 한다.

- service
- environment
- traffic pattern
- impact scope

---

## 33. Environment-aware Rule

Reliability Runtime은 environment-aware 해야 한다.

예: production / staging / sandbox

**원칙:** production → strictest reliability governance

---

## 34. Severity-aware Rule

Reliability Runtime은 severity-aware 해야 한다.

예: SEV-1 / SEV-2 / SEV-3

**원칙:** higher severity → stricter scoring governance

---

## 35. Policy-aware Rule

Reliability Runtime은 policy-aware 해야 한다.

- approval policy
- rollback policy
- verification policy
- visibility policy

---

## 36. Guardrail Rule

Reliability Runtime은 Guardrail Runtime 통합해야 한다.

- payment safety guardrail
- rollback requirement guardrail
- retry amplification guardrail

---

## 37. Unknown State Rule

Unknown 상태는 restrictive governance 적용.

예: missing metrics / partial observability / verification unavailable / rollback unavailable

**원칙:** Unknown → high-risk scoring blocked

---

## 38. Runtime Replay Rule

Reliability Runtime은 replayable 해야 한다.

- incident replay
- rollback replay
- verification replay
- reliability replay

---

## 39. Timeline Replay Rule

Reliability lifecycle은 replay 가능해야 한다.

- policy replay
- verification replay
- dataset replay
- stabilization replay

---

## 40. Runtime DTO Rule

Reliability Runtime은 canonical DTO 가져야 한다.

- ReliabilityScore
- ReliabilityClassification
- ReliabilityEvidence
- ReliabilityValidation
- ReliabilityResult

---

## 41. Explainability Rule

Reliability Runtime은 explainable 해야 한다.

**포함:**

- why reliability decreased
- why propagation risk increased
- why rollback improved stability
- why convergence failed

**금지:** opaque reliability interpretation

---

## 42. Runtime Security Rule

Reliability Runtime은 privileged operational layer다.

**필수:**

- authenticated access
- RBAC
- audit logging
- visibility control

**금지:**

- ❌ anonymous scoring mutation
- ❌ unrestricted operational evidence exposure
- ❌ public raw operational reliability dump

---

## 43. Auditability Rule

Reliability lifecycle은 audit 가능해야 한다.

- what evidence used
- what score calculated
- what validation performed
- what classification generated

---

## 44. Immutable Audit Rule

Reliability audit는 append-only 해야 한다.

**금지:**

- ❌ audit overwrite
- ❌ hidden score mutation
- ❌ invisible reliability override

---

## 45. Runtime Failure Rule

Reliability Runtime failure는 explicit 해야 한다.

예: verification unavailable / dataset inconsistency / timeline desynchronization / score inconsistency

**금지:** silent reliability corruption

---

## 46. Visibility Classification Rule

Reliability Artifact는 visibility classification 가져야 한다.

- PUBLIC_PORTFOLIO
- PRIVATE_RESEARCH
- INTERNAL_OPERATION
- PAPER_CANDIDATE
- SANITIZED_EXPORT

---

## 47. Sanitization Rule

Reliability export는 sanitization 가능해야 한다.

제거 대상: internal topology, customer payload, secret, token, internal IP

---

## 48. Runtime Metrics Governance Rule

Reliability metric은 low-cardinality 유지해야 한다.

**허용:** service, domain, severity, failure_mode, score_type

**금지:** customer identifier, payment payload, trace payload dump

---

## 49. Operational Reality Rule

Reliability Runtime은 현실 운영 기반이어야 한다.

**허용:** real incident, real rollback, real observability, real verification, real propagation

**금지:**

- toy-only reliability evaluation
- synthetic-only operational claim

---

## 50. Academic Compatibility Rule

Reliability Runtime은 학술 확장 가능해야 한다.

- reliability reproducibility appendix
- experiment reproducibility appendix
- dataset reproducibility appendix
- operational evidence appendix

---

## 51. Research Integrity Rule

Reliability Runtime은 research integrity 보장해야 한다.

**금지:**

- fabricated reliability evidence
- fabricated operational lineage
- unsupported reliability conclusion
- hidden negative experiment

---

## 52. Academic Extension Rule

장기적으로 다음 연구 방향 지원 가능.

- Operational Reliability Scoring Systems
- rollback-aware reliability scoring
- verification-aware operational scoring
- Human-in-the-loop reliability evaluation

---

## 53. Anti-Pattern Rule

**금지:**

- ❌ single-metric reliability score
- ❌ rollback 없는 reliability evaluation
- ❌ verification 없는 operational classification
- ❌ opaque reliability interpretation
- ❌ unsupported propagation inference

---

## 54. Non-Goals

Reliability Runtime의 목표는 다음이 아니다.

- autonomous operational ranking
- opaque scoring automation
- ungoverned operational evaluation
- unverifiable reliability conclusion

---

## 55. Canonical Runtime Layers

| Layer | 역할 |
|---|---|
| Incident Reliability | 장애 안정성 |
| Rollback Reliability | rollback 신뢰성 |
| Verification Reliability | 검증 신뢰성 |
| Propagation Reliability | 확산 제어 |
| Experiment Reliability | 실험 안정성 |
| Operational Reliability | 전체 운영 안정성 |

---

## 56. Reliability Research Formalization Direction

현재 방향의 핵심은 단순 health scoring이 아니다.

**목표:**

운영 observability와 operational evidence lineage를

- 설명 가능하고
- 재현 가능하며
- 정량 검증 가능하고
- 논문화 가능한

**Operational Reliability Scoring Runtime**으로 formalization 하는 것이다.

---

**한 줄 핵심**

> Runtime Reliability Scoring의 목적은 단순 점수 계산이 아니다.
> → rollback, verification, propagation, convergence, evidence lineage를 기반으로 운영 안정성을 정량 평가 가능한 **Reliability Scoring Runtime**으로 formalization 하는 것이다.