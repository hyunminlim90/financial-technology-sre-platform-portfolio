# Operational Dataset Governance Contract

## 1. 목적

이 문서는 FIN-SRE 플랫폼의 **Operational Dataset Governance Layer**를 정의한다.

Operational Dataset Governance의 목적은 단순 데이터 저장이 아니다.

목적은:

> 운영 이벤트 + Experiment Runtime + Rollback/Verification Runtime + Observability Signal + Policy Runtime

을 기반으로:

- 재현 가능하고
- 검증 가능하며
- 정량 분석 가능하고
- 논문화 가능한

**Operational Reliability Dataset Runtime**을 formalization 하는 것이다.

---

## 2. 핵심 개념

Operational Dataset은 단순 raw dump가 아니다.

Operational Dataset은:

- Operationally-grounded
- Evidence-backed
- Governance-aware
- Research-ready

**runtime dataset**이다.

---

## 3. Canonical Dataset Definition

Operational Dataset은 다음을 포함 가능.

| Dataset Type | 설명 |
|---|---|
| Incident Dataset | 장애 이벤트 |
| Recommendation Dataset | AI recommendation |
| Approval Dataset | Human approval |
| Rollback Dataset | rollback 결과 |
| Verification Dataset | verification 결과 |
| Experiment Dataset | experiment 결과 |
| Policy Dataset | policy evaluation |
| Quantitative Validation Dataset | 정량 검증 |

---

## 4. Human Governance Rule

Operational Dataset은 Human Governance 아래 있어야 한다.

**원칙:**
- AI는 dataset을 생성/분석할 수 있다.
- Human이 dataset validity를 검증한다.

**금지:**
- ❌ fabricated dataset
- ❌ unverifiable dataset mutation
- ❌ AI-only dataset validation

---

## 5. Canonical Dataset Lifecycle

Dataset Runtime은 canonical lifecycle 가져야 한다.

```
COLLECTED → NORMALIZED → VERIFIED → LINKED → RESEARCH_READY
```

또는:

```
COLLECTED → INVALIDATED
```

---

## 6. Evidence-backed Rule

모든 Dataset은 Evidence 기반이어야 한다.

**허용:**
- verified metrics
- verified traces
- verified logs
- verified rollback result
- verified experiment result

**금지:**
- fabricated metric
- hallucinated dataset
- invented operational evidence

---

## 7. Dataset Integrity Rule

Dataset integrity는 최우선이다.

**원칙:**

```
dataset corruption → research invalidation
```

---

## 8. Context-awareness Rule

Dataset은 context-aware 해야 한다.

**포함 가능:**
- service
- environment
- severity
- failure_mode
- timeline
- policy configuration
- risk classification

---

## 9. Experiment Linkage Rule

Dataset은 Experiment Runtime과 연결 가능해야 한다.

**예:**
- failure injection
- policy comparison
- rollback validation
- verification validation

---

## 10. Policy-aware Dataset Rule

Dataset은 policy-aware 해야 한다.

**포함 가능:**
- guardrail decision
- approval result
- policy rejection reason
- risk classification

---

## 11. Replayability Rule

Dataset은 replayable 해야 한다.

**예:**
- incident replay
- experiment replay
- rollback replay
- verification replay
- recommendation replay

---

## 12. Reproducibility Rule

Dataset은 재현 가능해야 한다.

**포함:**
- environment
- policy configuration
- experiment condition
- traffic pattern
- verification condition

**원칙:** 재현 불가능한 dataset 금지

---

## 13. FinTech Safety Rule

FinTech 환경에서는 payment consistency가 최우선이다.

**Dataset 포함 가능:**
- duplicate payment evidence
- idempotency consistency
- settlement consistency
- rollback safety

**금지:**
- unsafe payment replay
- raw payment payload exposure

---

## 14. Systems-Math Integration Rule

Dataset은 Systems-Math 연결 가능해야 한다.

**예:**
- Little's Law
- retry amplification
- tail latency propagation
- queue utilization

**원칙:** Systems-Math는 dataset 해석 계층이다.

---

## 15. Quantitative Validation Rule

Dataset은 정량 검증 가능해야 한다.

**예:**
- MTTR
- rollback success rate
- verification latency
- propagation reduction
- recommendation precision

---

## 16. Reliability Research Rule

Dataset은 Reliability Research 지원 가능해야 한다.

**예:**
- Human Approval effectiveness
- guardrail effectiveness
- rollback effectiveness
- verification effectiveness

---

## 17. Timeline Linkage Rule

Dataset은 timeline replay 가능해야 한다.

**예:**
- alert timeline
- approval timeline
- rollback timeline
- verification timeline
- experiment timeline

---

## 18. Runtime Replay Rule

Dataset Runtime은 replayable 해야 한다.

**예:**
- dataset replay
- experiment replay
- policy replay
- research replay

---

## 19. Unknown State Rule

Unknown 상태는 restrictive governance 적용.

**예:**
- missing metrics
- partial observability
- projection inconsistency
- timeline corruption

**원칙:**

```
Unknown → dataset invalidation
```

---

## 20. Visibility Classification Rule

Dataset은 visibility classification 가져야 한다.

**허용:**
- `PUBLIC_PORTFOLIO`
- `PRIVATE_RESEARCH`
- `INTERNAL_OPERATION`
- `PAPER_CANDIDATE`
- `SANITIZED_EXPORT`

---

## 21. Sanitization Rule

Dataset export는 sanitization 가능해야 한다.

**제거 대상:**
- internal IP
- secret
- token
- customer payload
- payment payload
- sensitive topology

---

## 22. Runtime DTO Rule

Dataset Runtime은 canonical DTO 가져야 한다.

**예:**
- `OperationalDataset`
- `DatasetEvidence`
- `DatasetReference`
- `DatasetValidation`
- `DatasetSnapshot`

---

## 23. Runtime Security Rule

Dataset Runtime은 privileged governance layer다.

**필수:**
- authenticated access
- RBAC
- audit logging
- visibility control

**금지:**
- ❌ anonymous dataset access
- ❌ unrestricted export
- ❌ public raw dataset exposure

---

## 24. Auditability Rule

Dataset lifecycle은 audit 가능해야 한다.

**포함:**
- who collected
- what source used
- what normalization applied
- what validation performed

---

## 25. Immutable Audit Rule

Dataset audit는 append-only 해야 한다.

**금지:**
- ❌ dataset overwrite
- ❌ silent mutation
- ❌ hidden normalization

---

## 26. Runtime Metrics Governance Rule

Dataset metrics는 low-cardinality 유지해야 한다.

**허용:**
- `service`
- `domain`
- `severity`
- `policy_type`
- `risk_level`

**금지:**
- customer identifier
- payment payload
- full trace payload

---

## 27. Operational Reality Rule

Dataset은 현실 운영 기반이어야 한다.

**허용:**
- real incident
- real rollback
- real observability
- real verification

**금지:**
- synthetic-only operational claim
- fabricated operational event

---

## 28. Cross-document Linkage Rule

Dataset은 Knowledge Set과 연결 가능해야 한다.

**포함:**
- Scenario
- Runbook
- Improvement
- Preventive Design
- Experiment
- Systems-Math
- Postmortem

---

## 29. Research Asset Integration Rule

Dataset은 Research Asset 생성 가능해야 한다.

**예:**
- experiment report
- validation report
- research note
- paper candidate

---

## 30. Statistical Explainability Rule

Dataset 기반 분석은 explainable 해야 한다.

**포함:**
- why metric improved
- why rollback failed
- why propagation reduced

**금지:** opaque score-only interpretation

---

## 31. Academic Extension Rule

장기적으로 다음 연구 방향 지원 가능.

**예:**
- dataset-aware reliability governance
- rollback-aware dataset analysis
- verification-aware operational dataset

---

## 32. Runtime Failure Rule

Dataset Runtime failure는 explicit 해야 한다.

**예:**
- dataset corruption
- projection inconsistency
- timeline corruption
- verification mismatch

**금지:** silent dataset degradation

---

## 33. Research-aware Reliability Runtime Rule

현재 방향의 핵심은 단순 observability archive가 아니다.

**목표:**

> 운영 이벤트와 observability signal을 설명 가능하고, 재현 가능하며, 정량 검증 가능하고, 논문화 가능한 **Operational Reliability Dataset Runtime**으로 formalization 하는 것이다.

---

## 34. Anti-Pattern Rule

**금지:**
- ❌ fabricated dataset
- ❌ unverifiable operational evidence
- ❌ opaque normalization
- ❌ raw dataset public exposure
- ❌ auditless dataset mutation
- ❌ unsupported statistical claim

---

## 35. Non-Goals

Operational Dataset Runtime의 목표는 다음이 아니다.

- raw log dumping
- autonomous dataset fabrication
- opaque benchmark generation
- unverifiable operational archive

---

## 36. Canonical Runtime Layers

| Layer | 역할 |
|---|---|
| Collection | dataset 수집 |
| Normalization | dataset 정규화 |
| Validation | dataset 검증 |
| Timeline | replay/audit |
| Research | 연구 자산 |
| Dataset | reliability dataset |
| Export | visibility governance |

---

## 37. Reliability Research Formalization Direction

현재 방향의 핵심은 단순 운영 로그 저장이 아니다.

**목표:**

> 운영 이벤트와 observability signal을 설명 가능하고, 재현 가능하며, 정량 검증 가능하고, 연구 가능한 **Operational Reliability Dataset Runtime**으로 formalization 하는 것이다.

---

## 한 줄 핵심

Operational Dataset Governance의 목적은 단순 데이터 저장이 아니다.

> 운영 이벤트와 observability signal을 **재현 가능하고 검증 가능한 Reliability Dataset Runtime**으로 formalization 하는 것이다.