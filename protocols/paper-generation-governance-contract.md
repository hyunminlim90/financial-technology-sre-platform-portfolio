# Paper Generation Governance Contract

## 1. 목적

이 문서는 FIN-SRE 플랫폼의 **Paper Generation Governance Layer**를 정의한다.

Paper Generation Governance의 목적은 단순 논문 자동 생성이 아니다.

목적은:

> 운영 이벤트 + 실험 결과 + 정량 검증 + Reliability Dataset

을 기반으로:

- 재현 가능하고
- 검증 가능하며
- 설명 가능한

**Operational Reliability Research Paper Runtime**을 formalization 하는 것이다.

---

## 2. 핵심 개념

Paper Generation은 text generation이 아니다.

Paper Generation은:

- Evidence-backed
- Experiment-grounded
- Research-governed
- Human-reviewed

**research formalization runtime**이다.

---

## 3. Canonical Paper Definition

Paper Candidate는 다음 요소 포함 가능.

| 요소 | 설명 |
|---|---|
| Research Question | 연구 질문 |
| Hypothesis | 가설 |
| Methodology | 방법론 |
| Experiment Design | 실험 설계 |
| Quantitative Validation | 정량 검증 |
| Systems-Math Analysis | 수학 기반 분석 |
| Reliability Dataset | 운영 데이터셋 |
| Limitation | 한계 |
| Future Work | 향후 연구 |

---

## 4. Human Governance Rule

Paper Runtime은 Human Governance 제거 금지.

**원칙:**
- AI는 draft를 생성할 수 있다.
- Human이 검증하고 승인한다.

**금지:**
- ❌ autonomous publication
- ❌ AI-only conclusion
- ❌ unreviewed paper export

---

## 5. Canonical Paper Lifecycle

Paper Runtime은 canonical lifecycle 가져야 한다.

```
DRAFT → REVIEW_PENDING → VALIDATED → PAPER_CANDIDATE → EXPORT_READY
```

또는:

```
DRAFT → REJECTED
```

---

## 6. Research Question Rule

모든 Paper Candidate는 명확한 Research Question 가져야 한다.

**예:**
- Does Human Approval reduce operational risk propagation?
- Do rollback-aware guardrails improve payment reliability?

---

## 7. Hypothesis Rule

모든 논문화는 검증 가능한 hypothesis 기반이어야 한다.

**예:**

> Human Approval reduces unsafe operational actions.

---

## 8. Quantitative Validation Rule

모든 논문화는 정량 검증 기반이어야 한다.

**예:**
- MTTR
- rollback success rate
- propagation reduction
- false positive reduction
- verification latency

---

## 9. Experiment-backed Rule

Paper Candidate는 experiment-grounded 해야 한다.

**허용:**
- failure injection
- policy comparison
- rollback validation
- verification comparison

**금지:**
- fabricated experiment
- synthetic-only unsupported claim

---

## 10. Evidence-backed Rule

모든 논문화는 Evidence 기반이어야 한다.

**허용:**
- verified metrics
- verified traces
- verified logs
- verified timeline
- verified experiment result

**금지:**
- hallucinated evidence
- invented metric
- speculative validation

---

## 11. Reproducibility Rule

Paper Candidate는 재현 가능해야 한다.

**포함:**
- environment
- policy configuration
- experiment setup
- verification method
- rollback method

**원칙:** 재현 불가능한 논문화 금지

---

## 12. Systems-Math Integration Rule

Paper Runtime은 Systems-Math 연결 가능해야 한다.

**예:**
- Little's Law
- retry amplification
- tail latency propagation
- queue utilization

**원칙:** Systems-Math는 운영 현상 설명 계층이다.

---

## 13. Reliability Dataset Rule

논문화는 reliability dataset 기반 가능해야 한다.

**예:**
- incident dataset
- rollback dataset
- verification dataset
- policy effectiveness dataset

---

## 14. Policy Comparison Rule

Paper Runtime은 정책 비교 가능해야 한다.

| 정책 | 비교 |
|---|---|
| Human Approval OFF | 위험 증가 |
| Human Approval ON | false positive 감소 |

---

## 15. FinTech Safety Rule

FinTech 환경에서는 payment consistency가 최우선이다.

**포함 가능:**
- duplicate payment prevention
- idempotency consistency
- settlement consistency
- rollback safety

---

## 16. Methodology Governance Rule

Methodology는 explainable 해야 한다.

**포함:**
- experiment condition
- policy configuration
- approval requirement
- rollback strategy
- verification strategy

---

## 17. Limitation Rule

모든 논문화는 limitation 명시해야 한다.

**예:**
- limited production scope
- partial observability
- synthetic workload bias

---

## 18. Future Work Rule

모든 논문화는 future work 정의 가능해야 한다.

**예:**
- adaptive approval governance
- risk-aware rollback orchestration
- verification-aware recommendation systems

---

## 19. Visibility Classification Rule

Paper Artifact는 visibility classification 가져야 한다.

**허용:**
- `PUBLIC_PORTFOLIO`
- `PRIVATE_RESEARCH`
- `INTERNAL_OPERATION`
- `PAPER_CANDIDATE`
- `SANITIZED_EXPORT`

---

## 20. Sanitization Rule

Paper export는 sanitization 가능해야 한다.

**제거 대상:**
- internal IP
- secret
- token
- raw customer payload
- sensitive topology

---

## 21. Runtime DTO Rule

Paper Runtime은 canonical DTO 가져야 한다.

**예:**
- `ResearchQuestion`
- `HypothesisDefinition`
- `PaperDraft`
- `ExperimentReference`
- `ValidationSummary`
- `PaperCandidate`

---

## 22. Timeline Replay Rule

논문화는 timeline replay 가능해야 한다.

**예:**
- incident replay
- experiment replay
- rollback replay
- validation replay

---

## 23. Unknown State Rule

Unknown 상태는 restrictive governance 적용.

**예:**
- missing metrics
- missing traces
- timeline corruption
- projection inconsistency

**원칙:**

```
Unknown → paper validation blocked
```

---

## 24. Operational Reality Rule

논문화는 현실 운영 기반이어야 한다.

**허용:**
- real incident
- real rollback
- real observability
- real verification

**금지:**
- toy-only benchmark
- pure synthetic operational claim

---

## 25. Runtime Replay Rule

Paper Runtime은 replayable 해야 한다.

**예:**
- experiment replay
- policy replay
- validation replay
- research replay

---

## 26. Academic Style Export Rule

장기적으로 다음 export 지원 가능.

**예:**
- IEEE format
- ACM format
- LaTeX export
- Markdown export

---

## 27. Draft Generation Rule

AI는 다음 draft generation 가능.

**허용:**
- abstract draft
- methodology draft
- experiment summary
- technical summary
- future work summary

**금지:**
- fabricated citation
- invented result
- fake experiment

---

## 28. Research Evidence Integrity Rule

Evidence integrity는 최우선이다.

**원칙:**

```
evidence corruption → paper invalidation
```

---

## 29. Runtime Security Rule

Paper Runtime은 privileged governance layer다.

**필수:**
- authenticated access
- RBAC
- audit logging
- visibility control

**금지:**
- ❌ anonymous publication
- ❌ unrestricted export
- ❌ public raw evidence exposure

---

## 30. Auditability Rule

Paper lifecycle은 audit 가능해야 한다.

**포함:**
- who created
- who reviewed
- what evidence used
- what experiment validated

---

## 31. Immutable Audit Rule

Paper audit는 append-only 해야 한다.

**금지:**
- ❌ audit overwrite
- ❌ hidden evidence mutation
- ❌ invisible methodology mutation

---

## 32. Research-aware Runtime Rule

Paper Runtime은 research-aware 해야 한다.

**예:**
- policy effectiveness analysis
- rollback effectiveness analysis
- Human Approval analysis

---

## 33. Reliability Research Direction Rule

현재 방향의 핵심은 단순 AI 논문 생성이 아니다.

**목표:**

> 실제 운영 이벤트와 실험 데이터를 기반으로 재현 가능하고, 검증 가능하며, 정량 분석 가능한 **Operational Reliability Research Formalization**이다.

---

## 34. Academic Extension Rule

장기적으로 다음 연구 방향 지원 가능.

**예:**
- Human-in-the-loop operational governance
- rollback-aware distributed systems
- verification-aware reliability runtime
- policy-aware AI-SRE systems

---

## 35. Cross-document Linkage Rule

Paper Runtime은 Knowledge Set과 연결 가능해야 한다.

**포함:**
- Scenario
- Runbook
- Improvement
- Preventive Design
- Experiment
- Systems-Math
- Postmortem

---

## 36. Runtime Failure Rule

Paper Runtime failure는 explicit 해야 한다.

**예:**
- validation unavailable
- dataset corruption
- timeline inconsistency
- evidence mismatch

**금지:** silent research degradation

---

## 37. Anti-Pattern Rule

**금지:**
- ❌ fabricated research
- ❌ unverifiable conclusion
- ❌ opaque methodology
- ❌ raw evidence publication
- ❌ AI-only publication
- ❌ unsupported operational claim

---

## 38. Non-Goals

Paper Runtime의 목표는 다음이 아니다.

- autonomous academic publication
- AGI research replacement
- fabricated benchmark generation
- unverifiable operational paper generation

---

## 39. Canonical Runtime Layers

| Layer | 역할 |
|---|---|
| Incident | 운영 이벤트 |
| Experiment | 장애 실험 |
| Validation | 정량 검증 |
| Dataset | reliability dataset |
| Paper Draft | 논문화 초안 |
| Review | Human Governance |
| Export | publication governance |

---

## 40. Reliability Research Formalization Direction

현재 방향의 핵심은 단순 논문 자동화가 아니다.

**목표:**

> 운영 이벤트와 실험 데이터를 설명 가능하고, 재현 가능하며, 정량 검증 가능하고, 논문화 가능한 **Operational Reliability Research Runtime**으로 formalization 하는 것이다.

---

## 한 줄 핵심

Paper Generation Governance의 목적은 단순 논문 자동 생성이 아니다.

> 실제 운영 이벤트와 실험 데이터를 기반으로 **재현 가능하고 검증 가능한 Reliability Research Formalization Runtime**을 구축하는 것이다.