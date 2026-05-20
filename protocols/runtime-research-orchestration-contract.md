# Runtime Research Orchestration Contract

`protocols/runtime-research-orchestration-contract.md`

---

## 1. 목적

이 문서는 FIN-SRE 플랫폼의 **Runtime Research Orchestration Layer**를 정의한다.

Research Orchestration의 목적은 단순 논문 생성이 아니다.

목적은 **Operational Runtime + Experiment Runtime + Observability Runtime + Quantitative Validation + Evidence Runtime + Dataset Governance**를 기반으로:

- 설명 가능하고
- 재현 가능하며
- 정량 검증 가능하고
- 논문화 가능하며
- 실제 운영 기반인

**Operational Reliability Research Runtime**을 formalization 하는 것이다.

---

## 2. 핵심 개념

Research Runtime은 단순 document generation engine이 아니다.

Research Runtime은 다음을 갖춘 **operational reliability research runtime**이다.

- Evidence-aware
- Experiment-aware
- Verification-aware
- Dataset-aware
- Reproducibility-aware
- Human-governed

---

## 3. Canonical Research Definition

Research Runtime은 다음을 포함 가능.

| Runtime | 역할 |
|---------|------|
| Experiment Runtime | 실험 orchestration |
| Evidence Runtime | 연구 근거 |
| Validation Runtime | 정량 검증 |
| Dataset Runtime | dataset governance |
| Paper Runtime | 논문화 |
| Research Runtime | 연구 orchestration |

---

## 4. Human Governance Rule

Research Runtime은 **Human Governance 제거 금지**.

**원칙:**
- AI는 research recommendation을 생성할 수 있다.
- Human이 research interpretation과 publication을 승인한다.

**금지:**
- ❌ autonomous paper publication
- ❌ AI-only operational conclusion
- ❌ unreviewed research claim

---

## 5. Canonical Research Lifecycle

Research Runtime은 canonical lifecycle을 가져야 한다.

```
RESEARCH_DEFINED
  → HYPOTHESIS_FORMED
  → EXPERIMENT_EXECUTED
  → EVIDENCE_COLLECTED
  → VALIDATION_COMPLETED
  → RESEARCH_ASSETIZED
  → PAPER_DRAFTED
  → REVIEW_PENDING
  → ARCHIVED
```

---

## 6. Research Question Rule

모든 연구는 **explicit research question**을 가져야 한다.

예:
- Does Human Approval reduce false-positive operational action?
- Does Guardrail reduce retry amplification propagation?

**금지:** vague operational claim

---

## 7. Hypothesis Rule

Research Runtime은 **hypothesis-aware** 해야 한다.

예:
- Human Approval decreases operational risk propagation.
- Rollback Verification improves recovery reliability.

---

## 8. Evidence-backed Rule

Research Runtime은 **Evidence 기반**이어야 한다.

**허용:** metrics, logs, traces, timeline, verification result, rollback result, experiment result

**금지:**
- fabricated experiment result
- hallucinated operational claim
- unsupported research conclusion

---

## 9. Reproducibility Rule

Research Runtime은 **reproducibility-aware** 해야 한다.

**필수:** experiment replay, policy replay, rollback replay, verification replay

> **원칙:** 재현 불가능한 연구는 신뢰 가능한 연구가 아니다.

---

## 10. Quantitative Validation Rule

Research Runtime은 **정량 검증 가능**해야 한다.

예: MTTR, rollback success rate, verification latency, propagation reduction, stabilization latency

---

## 11. Statistical Validation Rule

Research Runtime은 **statistical validation**을 지원 가능해야 한다.

예: confidence interval, variance, baseline comparison, repeated trial

> **원칙:** single-run conclusion 금지

---

## 12. Comparative Research Rule

Research Runtime은 **comparative evaluation** 가능해야 한다.

예: Guardrail ON/OFF, Human Approval ON/OFF, Rollback Verification ON/OFF, GitOps Drift Detection ON/OFF

---

## 13. Research Assetization Rule

Research 결과는 **canonical research asset으로 연결** 가능해야 한다.

예: Experiment Report, Quantitative Validation, Research Note, Paper Draft, Technical Review

---

## 14. Dataset-aware Rule

Research Runtime은 **dataset accumulation**을 지원 가능해야 한다.

예: incident dataset, rollback dataset, verification dataset, propagation dataset, experiment dataset

---

## 15. Operational Reality Rule

Research Runtime은 **현실 운영 기반**이어야 한다.

**허용:** real rollback, real observability, real verification, real propagation, real incident

**금지:** toy-only experimentation, synthetic-only operational claim

---

## 16. FinTech Safety Rule

FinTech 환경에서는 **payment consistency가 최우선**이다.

**금지:**
- unsafe payment replay
- duplicate payment corruption
- settlement inconsistency

**허용 가능:**
- sanitized operational evidence
- verified payment-safe experimentation

---

## 17. Visibility Classification Rule

Research Artifact는 **visibility classification**을 가져야 한다.

허용: PUBLIC_PORTFOLIO / PRIVATE_RESEARCH / INTERNAL_OPERATION / PAPER_CANDIDATE / SANITIZED_EXPORT

---

## 18. Sanitization Rule

Research export는 **sanitization 가능**해야 한다.

제거 대상: internal topology, customer payload, secret, token, internal IP

---

## 19. Timeline Governance Rule

Research Runtime은 **canonical research timeline**을 유지해야 한다.

```
hypothesis → experiment → propagation observation → rollback → verification → validation → research conclusion
```

---

## 20. Experiment-aware Rule

Research Runtime은 **experiment-aware** 해야 한다.

포함: failure injection, propagation observation, rollback validation, stabilization verification

---

## 21. Verification-aware Rule

Research Runtime은 **verification-aware** 해야 한다.

**필수:** queue stabilization validation, latency recovery validation, payment consistency validation

---

## 22. Rollback-aware Rule

Research Runtime은 **rollback-aware** 해야 한다.

**필수:** rollback trigger, rollback timeout, rollback verification, rollback blast radius

---

## 23. Propagation-aware Rule

Research Runtime은 **propagation-aware** 해야 한다.

예: dependency cascade, tail latency propagation, queue backlog propagation, retry amplification

---

## 24. Convergence-aware Rule

Research Runtime은 **convergence-aware** 해야 한다.

목표: safe stabilization

**금지:** oscillation, recovery thrashing, unstable mitigation loop

---

## 25. SLO-aware Rule

Research Runtime은 **SLO-aware** 해야 한다.

포함: error budget burn, availability degradation, P99 latency degradation

---

## 26. Blast Radius Rule

Research Runtime은 **blast radius awareness**를 가져야 한다.

범위: local / partial / cross-service / global

**원칙:** blast radius 증가 → stricter research governance

---

## 27. Context-awareness Rule

Research Runtime은 **context-aware** 해야 한다.

포함: service, environment, traffic pattern, impact scope

---

## 28. Environment-aware Rule

Research Runtime은 **environment-aware** 해야 한다.

예: production / staging / sandbox

**원칙:** production → strictest research governance

---

## 29. Severity-aware Rule

Research Runtime은 **severity-aware** 해야 한다.

예: SEV-1 / SEV-2 / SEV-3

**원칙:** higher severity → stricter research governance

---

## 30. Policy-aware Rule

Research Runtime은 **policy-aware** 해야 한다.

예: approval policy, rollback policy, verification policy, visibility policy

---

## 31. Guardrail Rule

Research Runtime은 **Guardrail Runtime**을 통합해야 한다.

예: payment safety guardrail, rollback requirement guardrail, retry amplification guardrail

---

## 32. Unknown State Rule

Unknown 상태는 **restrictive governance** 적용.

예: missing metrics, partial observability, verification unavailable, rollback unavailable

**원칙:** Unknown → unsupported research conclusion blocked

---

## 33. Systems-Math Integration Rule

Research Runtime은 **Systems-Math 연결** 가능해야 한다.

예: Little's Law, queue utilization, retry amplification, tail latency propagation

**원칙:** Systems-Math는 research interpretation layer다.

---

## 34. Runtime Replay Rule

Research Runtime은 **replayable** 해야 한다.

예: experiment replay, rollback replay, verification replay, research replay

---

## 35. Timeline Replay Rule

Research lifecycle은 **replay 가능**해야 한다.

예: policy replay, verification replay, stabilization replay, dataset replay

---

## 36. Reliability State Rule

Research Runtime은 **reliability-aware state**를 가져야 한다.

```
HEALTHY / DEGRADED / UNSTABLE / STABILIZING / CONVERGED / FAILED
```

---

## 37. Confidence-aware Rule

Research Runtime은 **confidence-awareness**를 가져야 한다.

```
HIGH_CONFIDENCE / MEDIUM_CONFIDENCE / LOW_CONFIDENCE / UNKNOWN
```

**원칙:** LOW_CONFIDENCE → publication claim 제한

---

## 38. Runtime DTO Rule

Research Runtime은 **canonical DTO**를 가져야 한다.

예: ResearchQuestion, Hypothesis, ExperimentResult, QuantitativeValidation, ResearchConclusion, PaperDraft

---

## 39. Explainability Rule

Research Runtime은 **explainable** 해야 한다.

포함:
- why propagation expanded
- why rollback improved stability
- why convergence failed
- why policy reduced risk

**금지:** opaque research interpretation

---

## 40. Runtime Security Rule

Research Runtime은 **privileged operational layer**다.

**필수:** authenticated access, RBAC, audit logging, visibility control

**금지:**
- ❌ anonymous research mutation
- ❌ unrestricted operational evidence access
- ❌ public raw operational exposure

---

## 41. Auditability Rule

Research lifecycle은 **audit 가능**해야 한다.

포함:
- what experiment executed
- what evidence collected
- what validation performed
- what conclusion generated

---

## 42. Immutable Audit Rule

Research audit는 **append-only** 해야 한다.

**금지:**
- ❌ audit overwrite
- ❌ hidden research mutation
- ❌ invisible conclusion override

---

## 43. Runtime Failure Rule

Research Runtime failure는 **explicit** 해야 한다.

예: verification unavailable, dataset inconsistency, timeline desynchronization, rollback evidence missing

**금지:** silent research corruption

---

## 44. Runtime Metrics Governance Rule

Research metric은 **low-cardinality**를 유지해야 한다.

**허용:** service, domain, severity, failure_mode, experiment_type

**금지:** customer identifier, payment payload, trace payload dump

---

## 45. Academic Compatibility Rule

Research Runtime은 **학술 확장 가능**해야 한다.

지원 가능: IEEE format, ACM format, LaTeX export, research reproducibility appendix, quantitative validation appendix

---

## 46. Paper Generation Rule

Research Runtime은 **논문화**를 지원 가능해야 한다.

```
Abstract → Introduction → Methodology → Experiment → Results → Limitations → Conclusion
```

**원칙:** 논문은 evidence-backed 해야 한다.

---

## 47. Research Integrity Rule

Research Runtime은 **research integrity**를 보장해야 한다.

**금지:**
- fabricated metric
- fabricated experiment
- unsupported conclusion
- hidden negative result

---

## 48. Academic Extension Rule

장기적으로 다음 연구 방향을 지원 가능.

- Human-in-the-loop reliability systems
- rollback-aware orchestration systems
- verification-aware stabilization systems
- propagation-aware operational governance

---

## 49. Anti-Pattern Rule

**금지:**
- ❌ single-run conclusion
- ❌ rollback 없는 experiment
- ❌ verification 없는 conclusion
- ❌ toy-only operational claim
- ❌ opaque research interpretation

---

## 50. Non-Goals

Research Runtime의 목표는 다음이 아니다.

- autonomous paper publication
- fabricated academic automation
- ungoverned operational experimentation
- unverifiable research conclusion

---

## 51. Canonical Runtime Layers

| Layer | 역할 |
|-------|------|
| Experiment | 실험 orchestration |
| Evidence | 연구 근거 |
| Validation | 정량 검증 |
| Dataset | dataset governance |
| Paper | 논문화 |
| Research | 연구 orchestration |

---

## 52. Reliability Research Formalization Direction

현재 방향의 핵심은 단순 AI 논문 생성이 아니다.

목표: 운영 observability와 experimentation lifecycle을 다음 조건을 갖춘 **Operational Reliability Research Runtime**으로 formalization 하는 것이다.

- 설명 가능하고
- 재현 가능하며
- 정량 검증 가능하고
- 논문화 가능한

---

## 한 줄 핵심

> Runtime Research Orchestration의 목적은 단순 논문 생성이 아니다.
> → experiment, evidence, validation, reproducibility, dataset governance를 통합하여 **재현 가능하고 검증 가능한 Reliability Research Runtime**으로 formalization 하는 것이다.