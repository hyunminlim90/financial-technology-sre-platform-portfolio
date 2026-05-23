# Runtime Operational Learning Contract

`protocols/runtime-operational-learning-contract.md`

---

## 1. 목적

이 문서는 FIN-SRE 플랫폼의 **Runtime Operational Learning Layer**를 정의한다.

Operational Learning Runtime의 목적은 단순 postmortem 축적이 아니다.

목적은 다음을 기반으로:

- Incident
- Evidence
- Rollback
- Verification
- Operational Memory
- Benchmark
- Governance Evolution
- Research Runtime

**설명 가능하고, 재현 가능하며, 정량 검증 가능하고, 운영 현실 기반이며, 논문화 가능한 Operational Reliability Learning Runtime**을 formalization 하는 것이다.

---

## 2. 핵심 개념

Operational Learning Runtime은 단순 장애 회고 저장소가 아니다.

Operational Learning Runtime은 다음 특성을 갖는 **operational reliability learning system**이다.

- Evidence-aware
- Rollback-aware
- Verification-aware
- Propagation-aware
- Trust-aware
- Human-governed

---

## 3. Canonical Operational Learning Definition

Operational Learning Runtime은 다음 learning domain을 지원 가능해야 한다.

| Learning Domain | 역할 |
|----------------|------|
| Incident Learning | 장애 경험 학습 |
| Rollback Learning | rollback 효과 학습 |
| Verification Learning | 검증 신뢰성 학습 |
| Propagation Learning | 장애 확산 패턴 학습 |
| Policy Learning | 정책 효과 학습 |
| Research Learning | 연구 자산 기반 학습 |

---

## 4. Human Governance Rule

Operational Learning Runtime은 **Human Governance 제거 금지**.

**원칙:**
- AI는 learning insight와 improvement recommendation을 생성할 수 있다.
- Human이 learning interpretation과 governance adoption을 승인한다.

**금지:**
- ❌ autonomous learning-to-action execution
- ❌ AI-only operational truth declaration
- ❌ unreviewed learning mutation

---

## 5. Canonical Learning Lifecycle

Operational Learning Runtime은 canonical lifecycle을 가져야 한다.

```
INCIDENT_OBSERVED
    → EVIDENCE_COLLECTED
    → POSTMORTEM_VALIDATED
    → LEARNING_EXTRACTED
    → BENCHMARK_VALIDATED
    → GOVERNANCE_RECOMMENDED
    → HUMAN_APPROVED
    → KNOWLEDGE_UPDATED
    → ARCHIVED
```

---

## 6. Incident Learning Rule

Incident는 learning source가 될 수 있다.

**포함:** failure_mode, severity, impact_scope, timeline, recommendation, approval, rollback, verification

**원칙:** Incident Learning은 Evidence와 Verification이 연결되어야 한다.

---

## 7. Postmortem Learning Rule

Postmortem은 learning artifact다.

**필수:** human validation, evidence linkage, rollback result, verification result, follow-up action

**금지:** AI-only postmortem learning

---

## 8. Rollback Learning Rule

Rollback 결과는 learning 대상이다.

**학습 대상:** rollback success/failure, rollback latency, rollback convergence

**학습 가능 항목:**
- 어떤 rollback이 안전했는가
- 어떤 rollback이 propagation을 줄였는가
- 어떤 rollback이 verification에 실패했는가

---

## 9. Verification Learning Rule

Verification 결과는 learning 대상이다.

**학습 대상:** verification success/failure, false recovery, partial recovery

**원칙:** Verification 실패는 Runbook/Policy/Guardrail 개선 후보가 된다.

---

## 10. Propagation Learning Rule

Propagation pattern은 learning 대상이다.

**학습 대상:** retry storm, queue backlog, DB saturation, dependency cascade

**학습 목표:** 장애가 어디서 시작되어 어디로 확산되었고, 어떤 guardrail이 확산을 줄였는지 기록.

---

## 11. Policy Learning Rule

Policy 효과는 learning 가능해야 한다.

- Human Approval 적용 후 false-positive 감소
- Guardrail 적용 후 propagation 감소
- Rollback Verification 적용 후 false recovery 감소

---

## 12. Guardrail Learning Rule

Guardrail은 operational evidence 기반으로 학습되어야 한다.

**예:** retry amplification guardrail, payment safety guardrail, rollback mandatory guardrail

**원칙:** Guardrail은 완화 기준이 아니라 **안전 제약**으로 학습되어야 한다.

---

## 13. Governance Learning Rule

Operational Learning은 Governance Evolution으로 연결되어야 한다.

- repeated rollback failure → rollback policy evolution
- repeated false recovery → verification policy evolution

---

## 14. Trust Learning Rule

Operational Trust는 learning 결과로 재보정 가능해야 한다.

- recommendation failure 증가 → recommendation trust 감소
- rollback success 증가 → rollback trust 증가

---

## 15. Preventive Design Learning Rule

반복 장애는 Preventive Design 학습으로 연결되어야 한다.

예: Redis timeout 반복 → Redis-only idempotency 구조 한계 학습 → DB unique constraint 기반 구조 개선

---

## 16. Knowledge Evolution Rule

Operational Learning은 Knowledge Evolution으로 연결되어야 한다.

**대상:** Scenario, Runbook, Improvement, Preventive Design, Experiment, Systems-Math

**금지:** 검증 없는 Knowledge overwrite

---

## 17. Evidence-backed Rule

Operational Learning은 Evidence 기반이어야 한다.

**허용:** metrics, logs, traces, timeline, verification result, rollback result, experiment result

**금지:** fabricated learning, hallucinated lesson, unsupported operational insight

---

## 18. Human Validation Rule

Learning은 Human 검증을 거쳐야 한다.

```
AI Learning Draft
    → Human Review
    → Approved Learning
    → Knowledge Update
```

**금지:** AI-only knowledge mutation

---

## 19. Learning Confidence Rule

Learning 결과는 confidence-aware 해야 한다.

| 수준 | 설명 |
|------|------|
| HIGH_CONFIDENCE | 정상 adoption 가능 |
| MEDIUM_CONFIDENCE | 조건부 adoption |
| LOW_CONFIDENCE | governance adoption 제한 |
| UNKNOWN | adoption 제한 |

---

## 20. Single-event Learning Rule

단일 incident 기반 학습은 제한되어야 한다.

**원칙:** single incident → hypothesis 가능 / definitive learning 금지

---

## 21. Statistical Learning Rule

반복 실험/반복 장애는 statistical learning 가능해야 한다.

**예:** confidence interval, variance, baseline comparison, repeated experiment

---

## 22. Comparative Learning Rule

Operational Learning은 비교 기반이어야 한다.

- Guardrail ON/OFF
- Human Approval ON/OFF
- Rollback Verification ON/OFF
- Old Runbook vs New Runbook

---

## 23. Quantitative Validation Rule

Operational Learning은 정량 검증 가능해야 한다.

**예:** MTTR reduction, rollback success improvement, verification mismatch reduction, propagation reduction, false-positive reduction

---

## 24. Experiment-aware Rule

Operational Learning은 Experiment Runtime과 연결되어야 한다.

**포함:** failure injection, policy comparison, rollback validation, verification validation, learning validation

---

## 25. Benchmark-aware Rule

Learning은 Benchmark 결과와 연결되어야 한다.

**예:** rollback benchmark improvement, verification benchmark improvement, propagation containment benchmark improvement

---

## 26. Research-aware Rule

Operational Learning은 Research Runtime과 연결되어야 한다.

**포함:** hypothesis, experiment, validation, paper candidate, research note

---

## 27. Dataset-aware Rule

Operational Learning은 dataset accumulation을 지원 가능해야 한다.

**예:** learning dataset, rollback dataset, verification dataset, propagation dataset, policy effectiveness dataset

---

## 28. Research Assetization Rule

Learning 결과는 research asset으로 연결 가능해야 한다.

**예:** Learning Report, Policy Effectiveness Report, Research Note, Paper Draft

---

## 29. Operational Memory Integration Rule

Operational Learning은 Operational Memory와 연결되어야 한다.

**예:** historical rollback pattern, historical propagation pattern, historical approval decision, historical verification mismatch

---

## 30. Knowledge Graph Integration Rule

Operational Learning은 Knowledge Graph와 연결되어야 한다.

```
Incident → Evidence → Postmortem → Improvement → Preventive Design → Learning
```

---

## 31. Governance Evolution Integration Rule

Operational Learning은 Governance Evolution의 입력이어야 한다.

```
learning insight → governance recommendation → human approval → policy evolution
```

---

## 32. Trust Integration Rule

Operational Learning은 Trust Runtime과 연결되어야 한다.

historical recommendation accuracy → recommendation trust recalibration

---

## 33. Causal Analysis Integration Rule

Operational Learning은 Causal Analysis와 연결되어야 한다.

예: retry storm causality → retry policy learning

---

## 34. Operational Consistency Integration Rule

Operational Learning은 Consistency Runtime과 연결되어야 한다.

verification mismatch → false recovery learning → verification rule 강화

---

## 35. Operational Topology Integration Rule

Operational Learning은 Topology Runtime과 연결되어야 한다.

high dependency density → propagation learning → dependency isolation improvement

---

## 36. Operational Lineage Integration Rule

Operational Learning은 Lineage Runtime과 연결되어야 한다.

```
incident lineage → rollback lineage → verification lineage → learning lineage
```

---

## 37. Systems-Math Integration Rule

Operational Learning은 Systems-Math와 연결되어야 한다.

**예:** Little's Law, queue utilization, retry amplification, tail latency propagation

**원칙:** Systems-Math는 learning interpretation layer다.

---

## 38. Runtime Replay Rule

Operational Learning은 replayable 해야 한다.

**예:** incident replay, rollback replay, verification replay, learning replay

---

## 39. Reproducibility Rule

Learning 결과는 reproducible 해야 한다.

same evidence + same policy + same benchmark + same validation → **same learning result**

---

## 40. Timeline Governance Rule

Operational Learning은 chronology-aware 해야 한다.

```
incident → evidence → rollback → verification → postmortem → learning → governance evolution
```

---

## 41. Propagation-aware Rule

Operational Learning은 propagation-aware 해야 한다.

**예:** dependency cascade, tail latency propagation, queue backlog propagation, retry amplification

---

## 42. Retry Amplification Learning Rule

Retry amplification은 핵심 learning target이다.

```
timeout → retry storm → queue overload → DB saturation → payment degradation
```

**학습 목표:** retry가 회복을 돕는 조건과 장애를 증폭시키는 조건을 분리한다.

---

## 43. Rollback-aware Rule

Operational Learning은 rollback-aware 해야 한다.

**포함:** rollback trigger, rollback verification, rollback stabilization, rollback convergence

---

## 44. Verification-aware Rule

Operational Learning은 verification-aware 해야 한다.

**포함:** queue stabilization validation, latency recovery validation, payment consistency validation

---

## 45. Convergence-aware Rule

Operational Learning은 convergence-aware 해야 한다.

**금지:** unstable recovery를 successful learning으로 반영

---

## 46. FinTech Safety Rule

FinTech 환경에서는 **payment consistency가 최우선**이다.

**금지:**
- payment consistency unknown 상태에서 positive learning 반영
- duplicate payment risk normalization
- settlement inconsistency tolerance

**허용:** verified payment-safe learning, sanitized operational learning

---

## 47. Blast Radius Rule

Operational Learning은 blast radius awareness를 가져야 한다.

| Blast Radius | Governance |
|-------------|------------|
| local | 기본 governance |
| partial | 강화 governance |
| cross-service | stricter governance |
| global | strictest governance |

---

## 48. SLO-aware Rule

Operational Learning은 SLO-aware 해야 한다.

**포함:** error budget burn, availability degradation, P99 latency degradation

---

## 49. Context-awareness Rule

Operational Learning은 context-aware 해야 한다.

**포함:** service, environment, traffic pattern, impact scope

---

## 50. Environment-aware Rule

Operational Learning은 environment-aware 해야 한다.

| 환경 | Governance |
|------|------------|
| production | strictest learning governance |
| staging | 표준 governance |
| sandbox | 완화 가능 |

---

## 51. Severity-aware Rule

Operational Learning은 severity-aware 해야 한다.

higher severity → stricter learning governance (SEV-1 > SEV-2 > SEV-3)

---

## 52. Policy-aware Rule

Operational Learning은 policy-aware 해야 한다.

**예:** approval policy, rollback policy, verification policy, visibility policy

---

## 53. Guardrail Rule

Operational Learning은 Guardrail Runtime과 통합되어야 한다.

**예:** payment safety guardrail, rollback requirement guardrail, retry amplification guardrail

---

## 54. Unknown State Rule

Unknown 상태는 restrictive governance 적용.

**해당 상황:** missing metrics, partial observability, verification unavailable, rollback unavailable

**원칙:** Unknown → learning adoption 제한

---

## 55. Reliability State Rule

Operational Learning은 reliability-aware state를 가져야 한다.

`HEALTHY` / `DEGRADED` / `UNSTABLE` / `STABILIZING` / `CONVERGED` / `FAILED`

---

## 56. Runtime DTO Rule

Operational Learning Runtime은 canonical DTO를 가져야 한다.

**예:** OperationalLearning, LearningInsight, LearningEvidence, LearningValidation, LearningAdoption

---

## 57. Explainability Rule

Operational Learning은 explainable 해야 한다.

**포함:** why rollback learning changed / why verification rule strengthened / why propagation learning created / why governance evolution recommended

**금지:** opaque learning insight

---

## 58. Runtime Security Rule

Operational Learning Runtime은 **privileged operational layer**다.

**필수:** authenticated access, RBAC, audit logging, visibility control

**금지:**
- ❌ anonymous learning mutation
- ❌ unrestricted learning adoption
- ❌ public raw operational learning exposure

---

## 59. Auditability Rule

Operational Learning lifecycle은 audit 가능해야 한다.

**포함:** what evidence analyzed / what learning extracted / what human approved / what knowledge updated

---

## 60. Immutable Audit Rule

Operational Learning audit는 **append-only** 해야 한다.

**금지:**
- ❌ audit overwrite
- ❌ hidden learning mutation
- ❌ invisible knowledge update

---

## 61. Runtime Failure Rule

Operational Learning Runtime failure는 explicit 해야 한다.

**예:** learning inconsistency, timeline inconsistency, verification unavailable, rollback unavailable

**금지:** silent learning corruption

---

## 62. Visibility Classification Rule

Learning Artifact는 visibility classification을 가져야 한다.

`PUBLIC_PORTFOLIO` / `PRIVATE_RESEARCH` / `INTERNAL_OPERATION` / `PAPER_CANDIDATE` / `SANITIZED_EXPORT`

---

## 63. Sanitization Rule

Learning export는 sanitization 가능해야 한다.

**제거 대상:** internal topology, customer payload, secret, token, internal IP, financially sensitive evidence

---

## 64. Runtime Metrics Governance Rule

Learning metric은 **low-cardinality** 유지해야 한다.

**허용:** service, domain, severity, failure_mode, learning_type

**금지:** customer identifier, payment payload, trace payload dump

---

## 65. Operational Reality Rule

Operational Learning Runtime은 **현실 운영 기반**이어야 한다.

**허용:** real incident, real rollback, real observability, real verification, real propagation

**금지:** toy-only operational learning, synthetic-only learning claim

---

## 66. Academic Compatibility Rule

Operational Learning Runtime은 학술 확장 가능해야 한다.

**지원 가능:** learning reproducibility appendix, experiment reproducibility appendix, dataset reproducibility appendix, operational evidence appendix

---

## 67. Research Integrity Rule

Operational Learning Runtime은 research integrity를 보장해야 한다.

**금지:** fabricated learning evidence, fabricated operational insight, unsupported learning conclusion, hidden contradictory evidence

---

## 68. Long-term Operational Learning Evolution Rule

Operational Learning Runtime은 장기 learning evolution을 지원 가능해야 한다.

**예:** rollback learning evolution, verification learning evolution, propagation learning evolution, Human Approval learning evolution

---

## 69. Academic Extension Rule

장기적으로 다음 연구 방향을 지원 가능하다.

- Operational Reliability Learning Systems
- rollback-aware operational learning
- verification-aware learning governance
- Human-in-the-loop operational learning systems

---

## 70. Anti-Pattern Rule

**금지:**
- ❌ evidence 없는 learning adoption
- ❌ single incident만으로 definitive learning 확정
- ❌ verification 없는 positive learning
- ❌ rollback failure 무시
- ❌ opaque learning mutation

---

## 71. Non-Goals

Operational Learning Runtime의 목표는 다음이 **아니다**.

- 단순 postmortem 저장소
- AI-only 자동 학습 시스템
- ungoverned knowledge mutation
- unverifiable operational lesson

---

## 72. Canonical Runtime Layers

| Layer | 역할 |
|-------|------|
| Incident Learning | 장애 경험 학습 |
| Rollback Learning | rollback 효과 학습 |
| Verification Learning | 검증 신뢰성 학습 |
| Propagation Learning | 장애 확산 패턴 학습 |
| Policy Learning | 정책 효과 학습 |
| Research Learning | 연구 자산 기반 학습 |

---

## 73. Reliability Research Formalization Direction

현재 방향의 핵심은 단순 장애 회고가 아니다.

**목표:** 운영 observability와 operational learning lineage를 **설명 가능하고, 재현 가능하며, 정량 검증 가능하고, 논문화 가능한** Operational Reliability Learning Runtime으로 formalization 하는 것이다.

---

## 한 줄 핵심

> Runtime Operational Learning의 목적은 단순 postmortem 축적이 아니다.
> → incident, rollback, verification, propagation, benchmark, governance evolution을 기반으로 운영 경험을 **재현 가능하고 검증 가능한 Operational Reliability Learning Runtime**으로 formalization 하는 것이다.