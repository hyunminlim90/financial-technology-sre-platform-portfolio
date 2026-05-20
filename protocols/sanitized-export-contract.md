# Sanitized Export Contract

## 1. 목적

이 문서는 FIN-SRE 플랫폼의 **Sanitized Export Governance Layer**를 정의한다.

Sanitized Export의 목적은 단순 데이터 masking이 아니다.

목적은:

> 운영 이벤트 + Experiment Runtime + Research Evidence + Reliability Dataset + Paper Generation Runtime

을 기반으로:

- 민감정보를 제거하면서도
- 재현 가능성과
- 연구 가능성과
- 기술 설명 가능성을 유지하는

**Operational Reliability Sanitized Export Runtime**을 formalization 하는 것이다.

---

## 2. 핵심 개념

Sanitized Export는 단순 anonymization이 아니다.

Sanitized Export는:

- Operationally-safe
- Research-preserving
- Evidence-aware
- Governance-controlled

**artifact transformation runtime**이다.

---

## 3. Canonical Sanitized Export Definition

Sanitized Export는 다음을 포함 가능.

| Export Type | 설명 |
|---|---|
| Portfolio Export | 공개 포트폴리오 |
| Research Export | 연구 제출용 |
| Paper Export | 논문화 export |
| Sanitized Postmortem | 민감정보 제거 회고 |
| Technical Review Export | 기술 리뷰 |
| Experiment Report Export | 실험 보고서 |

---

## 4. Human Governance Rule

Sanitized Export는 Human Governance 아래 있어야 한다.

**원칙:**
- AI는 sanitization draft를 생성할 수 있다.
- Human이 공개 가능 여부를 승인한다.

**금지:**
- ❌ autonomous public export
- ❌ AI-only sanitization approval
- ❌ unreviewed evidence publication

---

## 5. Canonical Export Lifecycle

Sanitized Export Runtime은 canonical lifecycle 가져야 한다.

```
COLLECTED → SANITIZING → VALIDATING → REVIEW_PENDING → EXPORT_READY
```

또는:

```
SANITIZING → REJECTED
```

---

## 6. Confidentiality Rule

민감정보는 export 대상에서 제거되어야 한다.

**제거 대상:**
- internal IP
- customer payload
- payment payload
- secret
- token
- credential
- kubeconfig
- internal topology

---

## 7. Research Preservation Rule

Sanitization은 연구 가능성을 유지해야 한다.

**허용:**
- metric trend preservation
- timeline preservation
- rollback/verification relationship preservation
- policy comparison preservation

**금지:** meaningless fully-redacted export

---

## 8. Evidence Integrity Rule

Sanitization 이후에도 evidence integrity 유지되어야 한다.

**원칙:**

> sanitization must not distort operational truth

**금지:**
- fabricated sanitization
- metric falsification
- timeline distortion

---

## 9. Replayability Rule

Sanitized Export는 replayability 유지 가능해야 한다.

**허용:**
- incident replay
- experiment replay
- rollback replay
- verification replay

단, 민감정보 제거 상태 유지.

---

## 10. Reproducibility Rule

Sanitized Export는 재현 가능성 유지해야 한다.

**포함:**
- experiment condition
- policy configuration
- failure mode
- verification condition
- rollback condition

**원칙:** sanitization 이후에도 research reproducibility 유지

---

## 11. FinTech Safety Rule

FinTech 환경에서는 payment consistency 보호가 최우선이다.

**금지:**
- customer payment exposure
- settlement payload exposure
- payment replay leakage

**허용:**
- synthetic payment abstraction
- aggregated payment metric
- sanitized transaction pattern

---

## 12. Visibility Classification Rule

모든 Export Artifact는 visibility classification 가져야 한다.

**허용:**
- `PUBLIC_PORTFOLIO`
- `PRIVATE_RESEARCH`
- `INTERNAL_OPERATION`
- `PAPER_CANDIDATE`
- `SANITIZED_EXPORT`

---

## 13. Export Boundary Rule

Visibility boundary를 넘는 export는 stricter governance 요구.

```
INTERNAL_OPERATION  → SANITIZED_EXPORT
PRIVATE_RESEARCH    → PUBLIC_PORTFOLIO
```

**원칙:**

```
cross-boundary export → mandatory review
```

---

## 14. Operational Reality Preservation Rule

Sanitization은 operational reality 유지해야 한다.

**허용:**
- latency trend
- rollback sequence
- verification outcome
- policy effect

**금지:** fake operational narrative

---

## 15. Systems-Math Preservation Rule

Sanitized Export는 Systems-Math 해석 가능성 유지해야 한다.

**예:**
- Little's Law interpretation
- retry amplification analysis
- queue utilization analysis
- tail latency propagation

**원칙:** sanitization이 Systems-Math 분석을 파괴하면 안 된다.

---

## 16. Quantitative Validation Preservation Rule

정량 검증 가능성은 유지되어야 한다.

**포함:**
- MTTR
- rollback success rate
- verification latency
- propagation reduction
- recommendation precision

---

## 17. Dataset Preservation Rule

Sanitized Export는 dataset consistency 유지해야 한다.

**허용:**
- aggregated metric export
- anonymized timeline export
- policy comparison export

**금지:**
- dataset corruption
- semantic inconsistency

---

## 18. Policy-aware Export Rule

Policy relationship은 유지 가능해야 한다.

**포함:**
- guardrail decision
- approval result
- rollback requirement
- verification requirement

---

## 19. Timeline Preservation Rule

Timeline semantics는 유지되어야 한다.

**포함:**
- alert timeline
- rollback timeline
- verification timeline
- experiment timeline

---

## 20. Runtime DTO Rule

Sanitized Export Runtime은 canonical DTO 가져야 한다.

**예:**
- `SanitizedExport`
- `SanitizationRule`
- `VisibilityClassification`
- `ExportValidation`
- `ExportArtifact`

---

## 21. Sanitization Policy Rule

Sanitization은 rule-based 해야 한다.

**예:**
- IP masking
- payload abstraction
- service anonymization
- namespace sanitization

**금지:** ad-hoc manual mutation

---

## 22. Runtime Replay Rule

Sanitized Export Runtime은 replayable 해야 한다.

**예:**
- export replay
- timeline replay
- dataset replay
- research replay

---

## 23. Unknown State Rule

Unknown 상태는 restrictive governance 적용.

**예:**
- unknown payload sensitivity
- partial sanitization
- unverified export

**원칙:**

```
Unknown → export blocked
```

---

## 24. Auditability Rule

Sanitized Export lifecycle은 audit 가능해야 한다.

**포함:**
- who exported
- what sanitized
- what removed
- what preserved
- who approved

---

## 25. Immutable Audit Rule

Export audit는 append-only 해야 한다.

**금지:**
- ❌ export overwrite
- ❌ hidden sanitization mutation
- ❌ invisible export modification

---

## 26. Runtime Security Rule

Sanitized Export Runtime은 privileged governance layer다.

**필수:**
- authenticated access
- RBAC
- audit logging
- visibility control

**금지:**
- ❌ anonymous export
- ❌ unrestricted publication
- ❌ raw evidence exposure

---

## 27. Research Compatibility Rule

Sanitized Export는 Reliability Research 지원 가능해야 한다.

**예:**
- Human Approval effectiveness
- guardrail effectiveness
- rollback effectiveness
- verification effectiveness

---

## 28. Academic Extension Rule

장기적으로 다음 연구 방향 지원 가능.

**예:**
- privacy-preserving reliability research
- sanitized observability dataset
- governed operational publication

---

## 29. Cross-document Linkage Rule

Sanitized Export는 Knowledge Set과 연결 가능해야 한다.

**포함:**
- Scenario
- Runbook
- Improvement
- Preventive Design
- Experiment
- Systems-Math
- Postmortem

---

## 30. Runtime Failure Rule

Sanitized Export Runtime failure는 explicit 해야 한다.

**예:**
- incomplete sanitization
- export corruption
- dataset inconsistency
- timeline mismatch

**금지:** silent sanitization degradation

---

## 31. Public Portfolio Rule

PUBLIC_PORTFOLIO export는 다음만 허용 가능.

**허용:**
- architecture
- generalized scenario
- sanitized runbook
- aggregated experiment result
- technical review

**금지:**
- raw incident evidence
- internal operational topology
- customer-sensitive metric

---

## 32. Paper Candidate Rule

PAPER_CANDIDATE export는 research reproducibility 유지해야 한다.

**포함:**
- experiment methodology
- policy configuration
- verification condition
- rollback condition

---

## 33. Runtime Metrics Governance Rule

Export metric은 low-cardinality 유지해야 한다.

**허용:**
- `service`
- `domain`
- `policy_type`
- `risk_level`
- `experiment_type`

**금지:**
- customer identifier
- payment payload
- trace payload dump

---

## 34. Research-aware Reliability Runtime Rule

현재 방향의 핵심은 단순 masking system이 아니다.

**목표:**

> 운영 evidence와 연구 자산을 민감정보를 제거하면서도 재현 가능성과, 정량 검증 가능성과, 논문화 가능성을 유지하는 **Operational Reliability Sanitized Export Runtime**으로 formalization 하는 것이다.

---

## 35. Anti-Pattern Rule

**금지:**
- ❌ raw evidence publication
- ❌ unverifiable sanitization
- ❌ fabricated export
- ❌ opaque masking
- ❌ auditless publication
- ❌ broken reproducibility

---

## 36. Non-Goals

Sanitized Export Runtime의 목표는 다음이 아니다.

- raw observability publication
- uncontrolled research export
- opaque anonymization
- evidence destruction

---

## 37. Canonical Runtime Layers

| Layer | 역할 |
|---|---|
| Collection | export source 수집 |
| Sanitization | 민감정보 제거 |
| Validation | export 검증 |
| Visibility | 공개 범위 통제 |
| Research | 연구 자산 유지 |
| Export | publication/export |

---

## 38. Reliability Research Formalization Direction

현재 방향의 핵심은 단순 anonymization이 아니다.

**목표:**

> 운영 evidence와 reliability dataset을 민감정보를 제거하면서도 설명 가능하고, 재현 가능하며, 정량 검증 가능하고, 논문화 가능한 **Operational Reliability Sanitized Export Runtime**으로 formalization 하는 것이다.

---

## 한 줄 핵심

Sanitized Export Governance의 목적은 단순 masking이 아니다.

> 운영 evidence와 연구 자산을 민감정보를 제거하면서도 **재현 가능성과 정량 검증 가능성을 유지하는 Reliability Sanitized Export Runtime**으로 formalization 하는 것이다.