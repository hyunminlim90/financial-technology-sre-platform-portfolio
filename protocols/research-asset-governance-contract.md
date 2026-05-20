# Research Asset Governance Contract

## 1. 목적

이 문서는 FIN-SRE 플랫폼의 **Research Asset Governance Layer**를 정의한다.

Research Asset Governance의 목적은 단순 문서 분류가 아니다.

목적은:

> 운영 이벤트 + 실험 결과 + 정량 검증 데이터 + 운영 지식

를 장기적으로:

- 재현 가능하고
- 검증 가능하며
- 논문화 가능한

**Reliability Research Asset**으로 formalization 하는 것이다.

---

## 2. 핵심 개념

Research Asset은 단순 문서가 아니다.

Research Asset은:

- Operationally-grounded
- Evidence-backed
- Experiment-verifiable
- Research-ready

artifact다.

---

## 3. Canonical Research Asset Definition

Research Asset은 다음을 포함 가능.

| Asset Type | 설명 |
|---|---|
| Experiment Report | 실험 보고서 |
| Quantitative Validation | 정량 검증 결과 |
| Incident Research Note | 장애 분석 연구 노트 |
| Policy Comparison | 정책 비교 |
| Reliability Dataset | 운영 데이터셋 |
| Paper Candidate | 논문화 후보 |
| Sanitized Postmortem | 공개 가능한 회고 |
| Systems-Math Analysis | 정량 모델 분석 |

---

## 4. Research-aware Runtime Rule

Research Asset은 runtime-generated 가능해야 한다.

```
Incident → Experiment → Validation → Research Asset
```

---

## 5. Human Governance Rule

Research Asset은 Human Governance 아래 있어야 한다.

**원칙:**
- AI는 초안을 생성할 수 있다.
- Human이 검증하고 승인한다.

**금지:**
- ❌ autonomous paper publication
- ❌ unreviewed experiment export
- ❌ AI-only research conclusion

---

## 6. Canonical Research Lifecycle

Research Asset은 canonical lifecycle 가져야 한다.

```
DRAFT → REVIEW_PENDING → VALIDATED → PAPER_CANDIDATE → SANITIZED_EXPORT
```

또는:

```
DRAFT → REJECTED
```

---

## 7. Visibility Classification Rule

모든 Research Asset은 visibility classification 가져야 한다.

**허용:**
- `PUBLIC_PORTFOLIO`
- `PRIVATE_RESEARCH`
- `INTERNAL_OPERATION`
- `PAPER_CANDIDATE`
- `SANITIZED_EXPORT`

---

## 8. Public/Private Separation Rule

Research Asset은 공개/비공개 분리되어야 한다.

**공개 가능:**
- architecture
- generalized scenario
- sanitized postmortem
- technical review

**비공개:**
- raw logs
- raw traces
- raw metrics
- internal topology
- sensitive evidence
- paper draft

---

## 9. Evidence-backed Rule

모든 Research Asset은 Evidence 기반이어야 한다.

**허용:**
- verified metrics
- verified traces
- verified logs
- verified timeline
- verified experiment result

**금지:**
- hallucinated evidence
- speculative validation
- fabricated experiment

---

## 10. Reproducibility Rule

Research Asset은 재현 가능해야 한다.

**포함 가능:**
- experiment condition
- policy configuration
- environment
- metric snapshot
- rollback condition
- verification method

**원칙:** 재현 불가능한 연구 자산 금지

---

## 11. Quantitative Validation Rule

Research Asset은 정량 검증 가능해야 한다.

**예:**
- MTTR
- rollback success rate
- false positive reduction
- propagation reduction
- approval latency

---

## 12. FinTech Safety Rule

FinTech 환경에서는 결제 안전성이 최우선이다.

연구 자산은 반드시 포함 가능해야 한다:
- duplicate payment risk
- idempotency consistency
- settlement consistency
- rollback safety

---

## 13. Experiment Integration Rule

Research Asset은 Experiment Runtime과 연결 가능해야 한다.

**예:**
- failure injection
- policy comparison
- rollback comparison
- guardrail comparison

---

## 14. Policy Comparison Rule

Research Asset은 정책 비교 가능해야 한다.

| 정책 | 비교 |
|---|---|
| Human Approval OFF | 위험 증가 |
| Human Approval ON | false positive 감소 |

---

## 15. Reliability Dataset Rule

Research Asset은 dataset accumulation 지원 가능해야 한다.

**예:**
- incident dataset
- rollback dataset
- verification dataset
- policy effectiveness dataset

---

## 16. Systems-Math Integration Rule

Research Asset은 Systems-Math 연결 가능해야 한다.

**예:**
- Little's Law
- retry amplification
- queue utilization
- tail latency propagation

**원칙:** Systems-Math는 운영 현상 설명 계층이다.

---

## 17. Research Timeline Rule

Research Asset은 timeline replay 가능해야 한다.

**예:**
- incident replay
- approval replay
- rollback replay
- experiment replay
- validation replay

---

## 18. Auditability Rule

모든 Research Asset lifecycle은 audit 가능해야 한다.

**포함:**
- who created
- who validated
- what evidence used
- what policy applied
- what experiment executed

---

## 19. Immutable Audit Rule

Research audit log는 append-only 해야 한다.

**금지:**
- ❌ audit overwrite
- ❌ evidence mutation
- ❌ hidden experiment mutation

---

## 20. Sanitization Rule

Research Asset export는 sanitization 가능해야 한다.

**제거 대상:**
- internal IP
- secret
- token
- raw customer data
- raw payment payload
- sensitive topology

---

## 21. Research Evidence Integrity Rule

Evidence integrity는 최우선이다.

**원칙:**

```
evidence corruption → research invalidation
```

---

## 22. Unknown State Rule

Unknown 상태는 restrictive governance 적용.

**예:**
- missing metrics
- missing traces
- projection inconsistency
- timeline corruption

**원칙:**

```
Unknown → research validation blocked
```

---

## 23. Runtime DTO Rule

Research Runtime은 canonical DTO 가져야 한다.

**예:**
- `ResearchArtifact`
- `ResearchEvidence`
- `ExperimentReference`
- `ValidationSummary`
- `PaperCandidate`

---

## 24. Paper Candidate Rule

Paper Candidate는 stricter governance 요구.

**필수:**
- validated evidence
- reproducible experiment
- reviewed methodology
- quantitative validation

---

## 25. Draft Generation Rule

AI는 draft generation 가능.

**허용:**
- abstract draft
- methodology draft
- experiment summary
- technical summary

**금지:**
- autonomous publication
- fabricated citation
- invented experiment result

---

## 26. Research Security Rule

Research Runtime은 privileged governance layer다.

**필수:**
- authenticated access
- RBAC
- audit logging
- visibility control

**금지:**
- ❌ anonymous research mutation
- ❌ unrestricted export
- ❌ public raw evidence access

---

## 27. Runtime Replay Rule

Research Asset Runtime은 replayable 해야 한다.

**예:**
- experiment replay
- validation replay
- policy replay
- research replay

---

## 28. Runtime Metrics Governance Rule

Research metrics는 low-cardinality 유지해야 한다.

**허용:**
- `experiment_type`
- `policy_type`
- `risk_level`
- `domain`

**금지:**
- raw payload
- full prompt
- customer identifier

---

## 29. Operational Reality Rule

Research Asset은 현실 운영 기반이어야 한다.

**허용:**
- real incident
- real rollback
- real verification
- real observability

**금지:**
- toy-only evaluation
- pure synthetic hallucination

---

## 30. Academic Extension Rule

장기적으로 다음 연구 방향 지원 가능.

**예:**
- Human-in-the-loop reliability systems
- risk-aware operational governance
- rollback-aware distributed systems
- approval-aware reliability runtime

---

## 31. Cross-document Linkage Rule

Research Asset은 Knowledge Set과 연결 가능해야 한다.

**포함:**
- Scenario
- Runbook
- Improvement
- Preventive Design
- Experiment
- Systems-Math
- Postmortem

---

## 32. Reliability Research Direction Rule

현재 방향의 핵심은 단순 논문 생성이 아니다.

**목표:**

> 실제 운영 데이터를 기반으로 재현 가능하고, 검증 가능하며, 정량 분석 가능한 Reliability Research Asset을 장기적으로 축적하는 것이다.

---

## 33. Anti-Pattern Rule

**금지:**
- ❌ fabricated experiment
- ❌ unverifiable research claim
- ❌ opaque methodology
- ❌ raw evidence public exposure
- ❌ auditless publication
- ❌ AI-only conclusion

---

## 34. Non-Goals

Research Asset Governance의 목표는 다음이 아니다.

- autonomous academic publication
- AGI research replacement
- unverifiable benchmark generation
- fabricated operational dataset

---

## 35. Canonical Runtime Layers

| Layer | 역할 |
|---|---|
| Incident | 운영 이벤트 |
| Experiment | 장애 실험 |
| Validation | 정량 검증 |
| Research Asset | 연구 자산 |
| Dataset | reliability dataset |
| Paper Candidate | 논문화 후보 |
| Export | 공개/비공개 governance |

---

## 36. Research-aware Reliability Runtime Direction

현재 방향의 핵심은 단순 AI 논문화가 아니다.

**목표:**

> 운영 이벤트를 설명 가능하고, 재현 가능하며, 정량 검증 가능하고, 논문화 가능한 **Operational Reliability Research Asset**으로 formalization 하는 것이다.

---

## 한 줄 핵심

Research Asset Governance의 목적은 단순 문서 저장이 아니다.

> 운영 이벤트와 실험 결과를 **재현 가능하고 검증 가능한 Reliability Research Asset**으로 formalization 하는 것이다.