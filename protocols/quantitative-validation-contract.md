# Quantitative Validation Contract

## 1. 목적

이 문서는 FIN-SRE 플랫폼의 **Quantitative Validation Governance Layer**를 정의한다.

Quantitative Validation의 목적은 단순 metric 비교가 아니다.

목적은:

> 운영 정책 + Experiment Runtime + Rollback/Verification Runtime + Reliability Dataset

을 기반으로:

- 정량 검증 가능하고
- 재현 가능하며
- 설명 가능하고
- 논문화 가능한

**Operational Reliability Validation Runtime**을 formalization 하는 것이다.

---

## 2. 핵심 개념

Quantitative Validation은 단순 benchmark가 아니다.

Quantitative Validation은:

> **Operational hypothesis validation**

이다.

**예:**
- Human Approval이 false-positive operational action을 감소시키는가?
- Guardrail이 retry propagation을 감소시키는가?

---

## 3. Canonical Validation Definition

Quantitative Validation은 다음을 포함 가능.

| Validation Type | 설명 |
|---|---|
| MTTR Validation | 복구 시간 검증 |
| Rollback Validation | rollback 효과 검증 |
| Verification Validation | verification 정확도 검증 |
| Propagation Validation | 장애 전파 감소 검증 |
| Policy Validation | 정책 효과 검증 |
| Recommendation Validation | 추천 품질 검증 |
| SLO Validation | SLO 회복 검증 |

---

## 4. Human Governance Rule

Validation Runtime은 Human Governance 제거 금지.

**원칙:**
- AI는 validation draft를 생성할 수 있다.
- Human이 validation 의미를 검증한다.

**금지:**
- ❌ autonomous research conclusion
- ❌ AI-only quantitative interpretation
- ❌ unverifiable statistical claim

---

## 5. Canonical Validation Lifecycle

Validation Runtime은 canonical lifecycle 가져야 한다.

```
COLLECTED → NORMALIZED → VERIFIED → VALIDATED → RESEARCH_READY
```

또는:

```
COLLECTED → INVALIDATED
```

---

## 6. Evidence-backed Rule

모든 Quantitative Validation은 Evidence 기반이어야 한다.

**허용:**
- verified metrics
- verified traces
- verified rollback result
- verified verification result
- verified experiment result

**금지:**
- fabricated metric
- hallucinated validation
- invented statistical result

---

## 7. Reproducibility Rule

Validation은 재현 가능해야 한다.

**포함:**
- experiment condition
- environment
- policy configuration
- traffic pattern
- verification condition

**원칙:** 재현 불가능한 validation 금지

---

## 8. Operational Reality Rule

Validation은 현실 운영 기반이어야 한다.

**허용:**
- real incident
- real rollback
- real verification
- real observability

**금지:**
- toy-only benchmark
- synthetic-only unsupported claim

---

## 9. Quantitative Comparison Rule

Validation은 비교 가능해야 한다.

| 그룹 | 정책 |
|---|---|
| A | Human Approval OFF |
| B | Human Approval ON |

**비교 항목:**
- MTTR
- rollback success
- propagation reduction
- false positive reduction

---

## 10. Statistical Interpretation Rule

Validation은 explainable statistical interpretation 가져야 한다.

**포함:**
- why metric improved
- why propagation reduced
- why rollback failed

**금지:** opaque score-only interpretation

---

## 11. Systems-Math Integration Rule

Validation은 Systems-Math 기반이어야 한다.

**예:**
- Little's Law
- retry amplification
- tail latency propagation
- queue utilization

**원칙:** Systems-Math는 정량 결과 설명 계층이다.

---

## 12. FinTech Safety Rule

FinTech 환경에서는 payment consistency가 최우선이다.

**Validation 포함 가능:**
- duplicate payment reduction
- idempotency consistency
- settlement consistency
- rollback safety

---

## 13. Recommendation Validation Rule

Recommendation Runtime은 quantitative evaluation 가능해야 한다.

**예:**
- unsafe recommendation reduction
- recommendation precision
- recommendation rollback success

---

## 14. Rollback Validation Rule

Rollback effectiveness는 정량 검증 가능해야 한다.

**예:**
- rollback latency
- rollback success rate
- rollback stabilization latency

---

## 15. Verification Validation Rule

Verification accuracy는 정량 검증 가능해야 한다.

**예:**
- false recovery reduction
- verification precision
- verification latency

---

## 16. Propagation Validation Rule

Propagation reduction은 정량 검증 가능해야 한다.

**예:**
- retry storm reduction
- queue saturation reduction
- tail latency reduction

---

## 17. SLO-aware Validation Rule

Validation은 SLO-aware 해야 한다.

**예:**
- P99 recovery
- availability recovery
- error budget stabilization

---

## 18. Reliability Dataset Rule

Validation은 dataset accumulation 지원 가능해야 한다.

**예:**
- incident dataset
- rollback dataset
- verification dataset
- policy effectiveness dataset

---

## 19. Timeline Replay Rule

Validation은 timeline replay 가능해야 한다.

**예:**
- incident replay
- experiment replay
- rollback replay
- verification replay

---

## 20. Runtime Replay Rule

Validation Runtime은 replayable 해야 한다.

**예:**
- validation replay
- policy replay
- experiment replay
- research replay

---

## 21. Unknown State Rule

Unknown 상태는 restrictive governance 적용.

**예:**
- missing metrics
- partial observability
- timeline inconsistency
- projection lag

**원칙:**

```
Unknown → validation invalidation
```

---

## 22. Confidence Rule

Validation은 confidence-awareness 가져야 한다.

**예:**
- `HIGH_CONFIDENCE`
- `MEDIUM_CONFIDENCE`
- `LOW_CONFIDENCE`
- `UNKNOWN`

**원칙:**

```
LOW_CONFIDENCE → research conclusion 제한
```

---

## 23. Runtime DTO Rule

Validation Runtime은 canonical DTO 가져야 한다.

**예:**
- `ValidationContext`
- `ValidationResult`
- `ValidationEvidence`
- `ValidationSummary`
- `ResearchValidation`

---

## 24. Research Compatibility Rule

Validation Runtime은 Reliability Research 지원 가능해야 한다.

**예:**
- Human Approval effectiveness
- guardrail effectiveness
- rollback effectiveness
- verification effectiveness

---

## 25. Academic Extension Rule

장기적으로 다음 연구 방향 지원 가능.

**예:**
- policy-aware reliability validation
- rollback-aware operational validation
- verification-aware runtime analysis

---

## 26. Visibility Classification Rule

Validation Artifact는 visibility classification 가져야 한다.

**허용:**
- `PUBLIC_PORTFOLIO`
- `PRIVATE_RESEARCH`
- `INTERNAL_OPERATION`
- `PAPER_CANDIDATE`
- `SANITIZED_EXPORT`

---

## 27. Sanitization Rule

Validation export는 sanitization 가능해야 한다.

**제거 대상:**
- internal IP
- customer payload
- payment payload
- secret
- token

---

## 28. Runtime Security Rule

Validation Runtime은 privileged governance layer다.

**필수:**
- authenticated access
- RBAC
- audit logging
- visibility control

**금지:**
- ❌ anonymous validation mutation
- ❌ unrestricted export
- ❌ public raw evidence exposure

---

## 29. Auditability Rule

Validation lifecycle은 audit 가능해야 한다.

**포함:**
- who validated
- what evidence used
- what methodology applied
- what policy compared

---

## 30. Immutable Audit Rule

Validation audit는 append-only 해야 한다.

**금지:**
- ❌ audit overwrite
- ❌ hidden statistical mutation
- ❌ invisible validation change

---

## 31. Runtime Metrics Governance Rule

Validation metrics는 low-cardinality 유지해야 한다.

**허용:**
- `service`
- `domain`
- `policy_type`
- `risk_level`
- `experiment_type`

**금지:**
- customer identifier
- payment payload
- full trace payload

---

## 32. Research Evidence Integrity Rule

Validation은 Evidence integrity 의존한다.

**원칙:**

```
evidence corruption → validation invalidation
```

---

## 33. Cross-document Linkage Rule

Validation은 Knowledge Set과 연결 가능해야 한다.

**포함:**
- Scenario
- Runbook
- Improvement
- Preventive Design
- Experiment
- Systems-Math
- Postmortem

---

## 34. Research-aware Reliability Runtime Rule

현재 방향의 핵심은 단순 metric dashboard가 아니다.

**목표:**

> 운영 정책과 장애 대응을 재현 가능하고, 설명 가능하며, 정량 검증 가능하고, 논문화 가능한 **Operational Reliability Validation Runtime**으로 formalization 하는 것이다.

---

## 35. Anti-Pattern Rule

**금지:**
- ❌ fabricated benchmark
- ❌ unverifiable statistical claim
- ❌ opaque metric interpretation
- ❌ unsupported operational conclusion
- ❌ auditless validation mutation
- ❌ synthetic-only reliability assertion

---

## 36. Non-Goals

Validation Runtime의 목표는 다음이 아니다.

- synthetic benchmark theater
- autonomous scientific conclusion
- opaque score generation
- unverifiable metric aggregation

---

## 37. Canonical Runtime Layers

| Layer | 역할 |
|---|---|
| Evidence | observability evidence |
| Experiment | 장애 실험 |
| Rollback | 안전 복구 |
| Verification | 결과 검증 |
| Validation | 정량 분석 |
| Research | 연구 자산 |
| Dataset | reliability dataset |

---

## 38. Reliability Research Formalization Direction

현재 방향의 핵심은 단순 metric collection이 아니다.

**목표:**

> 운영 observability와 정책 효과를 설명 가능하고, 재현 가능하며, 정량 검증 가능하고, 논문화 가능한 **Operational Reliability Quantitative Validation Runtime**으로 formalization 하는 것이다.

---

## 한 줄 핵심

Quantitative Validation Governance의 목적은 단순 metric 비교가 아니다.

> 운영 정책과 장애 대응 효과를 **재현 가능하고 정량 검증 가능한 Reliability Validation Runtime**으로 formalization 하는 것이다.