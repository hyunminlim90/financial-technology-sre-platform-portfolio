# Runtime Causal Analysis Contract

`protocols/runtime-causal-analysis-contract.md`

---

## 1. 목적

이 문서는 FIN-SRE 플랫폼의 **Runtime Causal Analysis Layer**를 정의한다.

Causal Analysis Runtime의 목적은 단순 correlation 분석이 아니다.

목적은 다음 7가지 요소를 기반으로:

- Incident
- Propagation
- Rollback
- Verification
- Experiment
- Operational Memory
- Knowledge Graph

**설명 가능하고, 재현 가능하며, 정량 검증 가능하고, 운영 현실 기반이며, 논문화 가능한 Operational Reliability Causal Analysis Runtime**을 formalization 하는 것이다.

---

## 2. 핵심 개념

Causal Analysis Runtime은 단순 metric correlation engine이 아니다.

Causal Analysis Runtime은:

- Evidence-aware
- Propagation-aware
- Rollback-aware
- Verification-aware
- Research-aware
- Human-governed

operational causal reasoning runtime이다.

---

## 3. Canonical Causal Definition

Causal Analysis Runtime은 다음 causal relationship을 분석할 수 있어야 한다.

| Causal Type | 역할 |
|-------------|------|
| Failure Causality | 장애 인과 |
| Propagation Causality | 확산 인과 |
| Retry Causality | retry amplification 인과 |
| Rollback Causality | rollback 효과 인과 |
| Verification Causality | stabilization 검증 인과 |
| Policy Causality | 정책 효과 인과 |

---

## 4. Human Governance Rule

Causal Analysis Runtime은 **Human Governance 제거 금지**.

**원칙:**

- AI는 causal hypothesis를 생성할 수 있다.
- Human이 operational causal interpretation을 승인한다.

**금지:**

- ❌ AI-only root cause certainty declaration
- ❌ autonomous operational blame assignment
- ❌ unsupported causal truth declaration

---

## 5. Canonical Causal Lifecycle

Causal Analysis Runtime은 canonical lifecycle을 가져야 한다.

```
SIGNAL_DETECTED
→ EVIDENCE_CORRELATED
→ CAUSAL_HYPOTHESIS_GENERATED
→ PROPAGATION_ANALYZED
→ VALIDATION_EXECUTED
→ CONFIDENCE_CLASSIFIED
→ RESEARCH_ASSETIZED
→ ARCHIVED
```

---

## 6. Correlation vs Causation Rule

Causal Analysis Runtime은 correlation과 causation을 구분해야 한다.

> **원칙:** `correlation ≠ causation`

**금지:** metric correlation만으로 root cause 확정

---

## 7. Causal Hypothesis Rule

모든 causal conclusion은 hypothesis 기반이어야 한다.

예시:
- Kafka consumer lag가 retry amplification의 primary cause candidate인가?
- DB saturation이 payment latency propagation을 유발했는가?

---

## 8. Evidence-backed Rule

Causal Analysis Runtime은 Evidence 기반이어야 한다.

**허용:**
- metrics, logs, traces
- timeline
- verification result, rollback result, experiment result

**금지:**
- fabricated causal evidence
- hallucinated root cause
- unsupported propagation inference

---

## 9. Timeline-aware Rule

Causal Analysis Runtime은 chronology-aware 해야 한다.

```
retry spike
→ queue backlog
→ DB saturation
→ latency propagation
```

> **원칙:** causality는 timeline dependency를 가진다.

---

## 10. Propagation-aware Rule

Causal Analysis Runtime은 propagation-aware 해야 한다.

- dependency cascade
- tail latency propagation
- queue backlog propagation
- retry amplification

---

## 11. Retry Amplification Rule

Retry amplification은 canonical causal chain으로 분석 가능해야 한다.

```
timeout
→ retry storm
→ queue overload
→ DB saturation
→ payment degradation
```

---

## 12. Rollback-aware Rule

Causal Analysis Runtime은 rollback-aware 해야 한다.

포함:
- rollback trigger
- rollback effect
- rollback stabilization
- rollback propagation reduction

---

## 13. Verification-aware Rule

Causal Analysis Runtime은 verification-aware 해야 한다.

포함:
- queue stabilization validation
- latency recovery validation
- payment consistency validation

---

## 14. Convergence-aware Rule

Causal Analysis Runtime은 convergence-aware 해야 한다.

**목표:** safe stabilization

**금지:**
- oscillation normalization
- unstable recovery misclassification

---

## 15. Policy Causality Rule

Causal Analysis Runtime은 policy effect를 분석할 수 있어야 한다.

```
Human Approval  →  false-positive 감소
Guardrail       →  retry propagation 감소
```

---

## 16. Comparative Causality Rule

Causal Analysis Runtime은 comparative analysis가 가능해야 한다.

- Guardrail ON/OFF
- Human Approval ON/OFF
- Rollback Verification ON/OFF

---

## 17. Quantitative Validation Rule

Causal Analysis Runtime은 정량 검증 가능해야 한다.

- MTTR
- rollback success rate
- verification latency
- propagation reduction
- stabilization latency

---

## 18. Statistical Validation Rule

Causal Analysis Runtime은 statistical validation을 지원해야 한다.

- confidence interval
- variance
- baseline comparison
- repeated experiment

> **원칙:** single-event causal certainty 금지

---

## 19. Experiment-aware Rule

Causal Analysis Runtime은 experiment-aware 해야 한다.

- failure injection
- policy comparison
- rollback validation
- verification validation

---

## 20. Research-aware Rule

Causal Analysis Runtime은 research-aware 해야 한다.

- hypothesis
- experiment
- validation
- paper candidate

---

## 21. Dataset-aware Rule

Causal Analysis Runtime은 dataset accumulation을 지원해야 한다.

- causal dataset
- rollback dataset
- verification dataset
- propagation dataset

---

## 22. Knowledge Graph Integration Rule

Causal Analysis Runtime은 Knowledge Graph와 연결 가능해야 한다.

```
Scenario
→ Runbook
→ Improvement
→ Preventive Design
→ Causal Inference
```

---

## 23. Operational Memory Integration Rule

Causal Analysis Runtime은 Operational Memory와 연결 가능해야 한다.

- past propagation pattern
- past rollback effectiveness
- past stabilization failure

---

## 24. Reproducibility Rule

Causal Analysis Runtime은 reproducibility-aware 해야 한다.

포함:
- experiment replay
- policy replay
- rollback replay
- verification replay

> **원칙:** 재현 불가능한 causal inference는 신뢰 불가

---

## 25. Runtime Replay Rule

Causal Analysis Runtime은 replayable 해야 한다.

- incident replay
- rollback replay
- verification replay
- causal replay

---

## 26. Systems-Math Integration Rule

Causal Analysis Runtime은 Systems-Math와 연결 가능해야 한다.

- Little's Law
- queue utilization
- retry amplification
- tail latency propagation

> **원칙:** Systems-Math는 causal interpretation layer다.

---

## 27. FinTech Safety Rule

FinTech 환경에서는 **payment consistency가 최우선**이다.

**금지:**
- unsafe payment causal replay
- duplicate payment corruption
- settlement inconsistency

**허용:**
- verified payment-safe causal analysis
- sanitized operational causality

---

## 28. Blast Radius Rule

Causal Analysis Runtime은 blast radius awareness를 가져야 한다.

범위: `local` → `partial` → `cross-service` → `global`

> **원칙:** blast radius 증가 → stricter causal governance

---

## 29. SLO-aware Rule

Causal Analysis Runtime은 SLO-aware 해야 한다.

- error budget burn
- availability degradation
- P99 latency degradation

---

## 30. Context-awareness Rule

Causal Analysis Runtime은 context-aware 해야 한다.

- service
- environment
- traffic pattern
- impact scope

---

## 31. Environment-aware Rule

Causal Analysis Runtime은 environment-aware 해야 한다.

환경: `production` / `staging` / `sandbox`

> **원칙:** production → strictest causal governance

---

## 32. Severity-aware Rule

Causal Analysis Runtime은 severity-aware 해야 한다.

심각도: `SEV-1` / `SEV-2` / `SEV-3`

> **원칙:** higher severity → stricter causal governance

---

## 33. Policy-aware Rule

Causal Analysis Runtime은 policy-aware 해야 한다.

- approval policy
- rollback policy
- verification policy
- visibility policy

---

## 34. Guardrail Rule

Causal Analysis Runtime은 Guardrail Runtime을 통합해야 한다.

- payment safety guardrail
- rollback requirement guardrail
- retry amplification guardrail

---

## 35. Unknown State Rule

Unknown 상태는 restrictive governance를 적용한다.

**해당 상황:**
- missing metrics
- partial observability
- verification unavailable
- rollback unavailable

> **원칙:** Unknown → causal certainty 제한

---

## 36. Reliability State Rule

Causal Analysis Runtime은 reliability-aware state를 가져야 한다.

`HEALTHY` / `DEGRADED` / `UNSTABLE` / `STABILIZING` / `CONVERGED` / `FAILED`

---

## 37. Confidence-aware Rule

Causal Analysis Runtime은 confidence-awareness를 가져야 한다.

`HIGH_CONFIDENCE` / `MEDIUM_CONFIDENCE` / `LOW_CONFIDENCE` / `UNKNOWN`

> **원칙:** LOW_CONFIDENCE → root cause certainty 금지

---

## 38. Runtime DTO Rule

Causal Analysis Runtime은 canonical DTO를 가져야 한다.

- `CausalHypothesis`
- `CausalChain`
- `PropagationInference`
- `RollbackEffectInference`
- `VerificationInference`

---

## 39. Explainability Rule

Causal Analysis Runtime은 explainable 해야 한다.

**포함:**
- why propagation expanded
- why rollback reduced instability
- why convergence failed
- why retry storm amplified

**금지:** opaque causal inference

---

## 40. Runtime Security Rule

Causal Analysis Runtime은 **privileged operational layer**다.

**필수:**
- authenticated access
- RBAC
- audit logging
- visibility control

**금지:**
- ❌ anonymous causal mutation
- ❌ unrestricted operational evidence exposure
- ❌ public raw operational causality exposure

---

## 41. Auditability Rule

Causal lifecycle은 audit 가능해야 한다.

- what evidence analyzed
- what causal hypothesis generated
- what validation executed
- what confidence assigned

---

## 42. Immutable Audit Rule

Causal audit는 **append-only** 해야 한다.

**금지:**
- ❌ audit overwrite
- ❌ hidden causal mutation
- ❌ invisible operational override

---

## 43. Runtime Failure Rule

Causal Analysis Runtime failure는 explicit 해야 한다.

**해당 상황:**
- timeline inconsistency
- evidence inconsistency
- verification unavailable
- rollback unavailable

**금지:** silent causal corruption

---

## 44. Visibility Classification Rule

Causal Artifact는 visibility classification을 가져야 한다.

- `PUBLIC_PORTFOLIO`
- `PRIVATE_RESEARCH`
- `INTERNAL_OPERATION`
- `PAPER_CANDIDATE`
- `SANITIZED_EXPORT`

---

## 45. Sanitization Rule

Causal export는 sanitization 가능해야 한다.

**제거 대상:**
- internal topology
- customer payload
- secret / token
- internal IP

---

## 46. Runtime Metrics Governance Rule

Causal metric은 **low-cardinality** 유지해야 한다.

**허용:** `service` / `domain` / `severity` / `failure_mode` / `causal_type`

**금지:** customer identifier / payment payload / trace payload dump

---

## 47. Operational Reality Rule

Causal Analysis Runtime은 현실 운영 기반이어야 한다.

**허용:**
- real incident, real rollback
- real observability, real verification
- real propagation

**금지:**
- toy-only causal analysis
- synthetic-only operational claim

---

## 48. Academic Compatibility Rule

Causal Analysis Runtime은 학술 확장 가능해야 한다.

- causal reproducibility appendix
- experiment reproducibility appendix
- dataset reproducibility appendix
- operational evidence appendix

---

## 49. Research Integrity Rule

Causal Analysis Runtime은 research integrity를 보장해야 한다.

**금지:**
- fabricated causal evidence
- fabricated operational lineage
- unsupported causal conclusion
- hidden contradictory evidence

---

## 50. Long-term Operational Learning Rule

Causal Analysis Runtime은 장기 causal learning을 지원해야 한다.

- 5년간 retry amplification pattern
- 3년간 rollback effectiveness evolution
- verification convergence trend

> **원칙:** Operational causality는 장기 operational learning 기반이어야 한다.

---

## 51. Academic Extension Rule

장기적으로 다음 연구 방향 지원 가능:

- Operational Reliability Causal Systems
- rollback-aware causal reasoning
- verification-aware operational causality
- Human-in-the-loop causal governance

---

## 52. Anti-Pattern Rule

**금지:**

- ❌ metric correlation만으로 root cause 확정
- ❌ rollback 없는 causal interpretation
- ❌ verification 없는 operational causality
- ❌ opaque causal lineage
- ❌ unsupported propagation certainty

---

## 53. Non-Goals

Causal Analysis Runtime의 목표는 다음이 **아니다**:

- autonomous root cause declaration
- opaque AI causality
- ungoverned operational blame assignment
- unverifiable causal reasoning

---

## 54. Canonical Runtime Layers

| Layer | 역할 |
|-------|------|
| Failure Causality | 장애 인과 |
| Propagation Causality | 확산 인과 |
| Retry Causality | retry amplification 인과 |
| Rollback Causality | rollback 효과 인과 |
| Verification Causality | stabilization 검증 인과 |
| Policy Causality | 정책 효과 인과 |

---

## 55. Reliability Research Formalization Direction

현재 방향의 핵심은 단순 root-cause analysis가 아니다.

**목표:** 운영 observability와 operational lineage를, 설명 가능하고, 재현 가능하며, 정량 검증 가능하고, 논문화 가능한 **Operational Reliability Causal Analysis Runtime**으로 formalization 하는 것이다.

---

## 🎯 한 줄 핵심

> Runtime Causal Analysis의 목적은 단순 correlation 분석이 아니다.
> → propagation, rollback, verification, operational memory lineage를 기반으로 **재현 가능하고 검증 가능한 Operational Causal Reasoning Runtime**으로 formalization 하는 것이다.