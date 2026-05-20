# SRE Console Runtime Contract

`protocols/sre-console-runtime-contract.md`

---

## 1. 목적

이 문서는 FIN-SRE 플랫폼의 **SRE Console Runtime Layer**를 정의한다.

SRE Console의 목적은 단순 운영 UI 제공이 아니다.

> 목적은 **Operational Governance + Reliability Runtime + Research-aware Operational Knowledge Runtime**을  
> 하나의 runtime chain으로 formalization 하는 것이다.

---

## 2. 핵심 개념

SRE Console은 다음 역할을 가진다.

| 역할 | 설명 |
|------|------|
| Incident Governance | 장애 운영 거버넌스 |
| Recommendation Runtime | AI Recommendation review |
| Approval Runtime | Human approval |
| Verification Runtime | 운영 검증 |
| Research Runtime | 연구 자산 축적 |
| Knowledge Runtime | 운영 지식 lifecycle |
| Timeline Runtime | replay/audit |
| Experiment Runtime | 장애 실험/검증 |

---

## 3. Canonical Console Definition

SRE Console은 다음 runtime을 orchestration 한다.

```
Evidence Runtime
→ Risk Runtime
→ Recommendation Runtime
→ Approval Runtime
→ Rollback Runtime
→ Verification Runtime
→ Timeline Runtime
→ Research Runtime
```

---

## 4. Human-in-the-loop Rule

SRE Console은 **Human Governance 제거 금지**.

**원칙:**

- AI는 추천한다.
- Human이 승인한다.
- Human이 실행한다.
- Console은 기록하고 검증한다.

**금지:**

- ❌ autonomous operational execution
- ❌ human bypass runtime
- ❌ AI-only governance

---

## 5. Operational Governance Rule

SRE Console은 **운영 거버넌스 runtime**이다.

포함: recommendation review, approval governance, rollback governance, verification governance, incident lifecycle

---

## 6. Research-aware Runtime Rule

SRE Console은 **research-aware runtime**이어야 한다.

- experiment accumulation
- policy comparison
- quantitative validation
- research artifact generation

---

## 7. Reliability Research Rule

장기적으로 SRE Console은 **Reliability Research Runtime 지원** 가능해야 한다.

- MTTR comparison
- rollback effectiveness
- guardrail effectiveness
- Human Approval effectiveness
- risk propagation analysis

---

## 8. Canonical Runtime Lifecycle

SRE Console은 **canonical lifecycle**을 지원해야 한다.

```
Incident
→ Evidence
→ Recommendation
→ Approval
→ Execution
→ Rollback
→ Verification
→ Postmortem
→ Experiment
→ Quantitative Validation
→ Research Asset
```

---

## 9. Visibility Classification Rule

모든 artifact는 **visibility classification**을 가져야 한다.

- `PUBLIC_PORTFOLIO`
- `PRIVATE_RESEARCH`
- `INTERNAL_OPERATION`
- `PAPER_CANDIDATE`
- `SANITIZED_EXPORT`

---

## 10. Public/Private Separation Rule

Public/Private governance는 **runtime enforced** 가능해야 한다.

**공개 가능:** architecture, sanitized postmortem, generalized scenario, technical review

**비공개:** raw evidence, raw logs, internal topology, sensitive metrics, research draft, private experiment

---

## 11. Incident Runtime Rule

SRE Console은 **canonical incident lifecycle**을 관리해야 한다.

```
DETECTED
→ INVESTIGATING
→ MITIGATING
→ VERIFYING
→ RESOLVED
→ POSTMORTEM_PENDING
```

---

## 12. Recommendation Runtime Rule

Console은 **recommendation review runtime**을 제공해야 한다.

포함: risk explanation, policy explanation, guardrail result, rollback requirement, verification requirement

---

## 13. Approval Runtime Rule

Console은 **approval governance runtime**을 제공해야 한다.

포함: approval request, approval escalation, approval audit, approval replay

---

## 14. Rollback Runtime Rule

Console은 **rollback governance runtime**을 제공해야 한다.

포함: rollback requirement, rollback execution tracking, rollback verification, rollback audit

---

## 15. Verification Runtime Rule

Console은 **verification governance runtime**을 제공해야 한다.

포함: recovery verification, payment consistency verification, queue stabilization verification, SLO recovery verification

---

## 16. Timeline Runtime Rule

Console은 **canonical timeline runtime**을 제공해야 한다.

- alert timeline
- recommendation timeline
- approval timeline
- rollback timeline
- verification timeline
- experiment timeline

---

## 17. Timeline Replay Rule

Timeline은 **replay 가능**해야 한다.

- incident replay
- approval replay
- rollback replay
- experiment replay

---

## 18. Experiment Runtime Rule

Console은 **experiment orchestration**을 지원 가능해야 한다.

- failure injection
- policy comparison
- approval comparison
- rollback comparison

> Experiment → **human-approved only**

---

## 19. Quantitative Validation Rule

Console은 **quantitative validation**을 지원 가능해야 한다.

- MTTR
- rollback success rate
- false positive reduction
- propagation reduction

---

## 20. Research Asset Rule

Console은 **research artifact accumulation**을 지원 가능해야 한다.

- experiment report
- research note
- paper candidate
- quantitative validation report

---

## 21. Knowledge Runtime Rule

Console은 **knowledge lifecycle awareness**를 가져야 한다.

포함: Scenario, Runbook, Improvement, Preventive Design, Postmortem, Experiment, Systems-Math

---

## 22. RAG-aware Runtime Rule

Console은 **RAG governance awareness**를 가져야 한다.

포함: knowledge layering, retrieval governance, evidence-aware retrieval, policy-aware retrieval

---

## 23. Systems-Math Runtime Rule

Console은 **Systems-Math integration**을 지원 가능해야 한다.

- Little's Law
- retry amplification
- tail latency propagation
- queue utilization

> Systems-Math는 **운영 현상 설명 계층**이다.

---

## 24. Evidence Runtime Rule

Console은 **EvidenceContext 기반**이어야 한다.

**허용:** metrics, traces, logs, alerts, timeline state, verification state

**금지:** hallucinated evidence, speculative incident mutation

---

## 25. Unknown State Rule

Unknown state는 **restrictive governance** 적용.

- `projection lag`
- `missing metrics`
- `partial observability`
- `timeline inconsistency`

> Unknown → **restrictive governance**

---

## 26. FinTech Safety Rule

FinTech 환경에서는 다음 **우선순위** 유지.

```
payment integrity
> consistency
> rollback capability
> verification capability
> availability
> performance
```

---

## 27. GitOps Governance Rule

Console은 **GitOps source-of-truth**를 유지해야 한다.

**허용:** Git revert, ArgoCD sync, desired-state recovery

**금지:** manual runtime mutation, kubectl direct mutation, drift-causing operation

---

## 28. Auditability Rule

Console은 **append-only audit lifecycle**을 유지해야 한다.

포함: recommendation audit, approval audit, rollback audit, verification audit, experiment audit

---

## 29. Runtime DTO Rule

Console Runtime은 canonical DTO를 가져야 한다.

- `IncidentContext`
- `TimelineEvent`
- `RecommendationContext`
- `ApprovalContext`
- `ExperimentContext`
- `ResearchArtifact`

---

## 30. Runtime Replay Rule

Console Runtime은 **replayable** 해야 한다.

- incident replay
- recommendation replay
- verification replay
- experiment replay
- research replay

---

## 31. Runtime Metrics Governance Rule

Console metrics는 **low-cardinality** 유지해야 한다.

**허용:** `incident_state`, `risk_level`, `service`, `domain`, `experiment_type`

**금지:** `incident_id`, `raw prompt`, `full evidence payload`

---

## 32. Runtime Failure Rule

Console Runtime failure는 **explicit** 해야 한다.

- `timeline unavailable`
- `projection inconsistency`
- `audit corruption`
- `verification unavailable`

> **silent governance degradation 금지**

---

## 33. Runtime Security Rule

Console Runtime은 **privileged internal governance layer**다.

**필수:** authenticated access, RBAC, internal-only routing, audit logging

**금지:**

- ❌ public operational mutation
- ❌ anonymous governance access
- ❌ external approval override

---

## 34. Research Governance Rule

Research artifact는 **governance-aware** 해야 한다.

포함: paper candidate review, sanitized export review, private/public separation, research evidence retention

---

## 35. Academic Extension Rule

장기적으로 Console Runtime은 다음 방향 지원 가능.

- research hypothesis generation
- policy effectiveness analysis
- experiment comparison
- paper draft generation

---

## 36. Reliability Dataset Rule

Console은 **reliability dataset accumulation**을 지원 가능해야 한다.

- incident dataset
- rollback dataset
- verification dataset
- policy comparison dataset

---

## 37. Experiment Safety Rule

Console은 **unsafe experiment 금지**해야 한다.

**금지:**

- unbounded blast radius
- rollbackless experiment
- payment-risking experiment

---

## 38. Anti-Pattern Rule

**금지:**

- ❌ autonomous operational authority
- ❌ opaque governance runtime
- ❌ auditless operation
- ❌ rollback 없는 recommendation
- ❌ verification 없는 resolution
- ❌ raw evidence public exposure

---

## 39. Non-Goals

SRE Console Runtime의 목표는 다음이 **아니다.**

- AGI operation replacement
- human-free governance
- blind self-healing
- opaque autonomous operation

---

## 40. Canonical Runtime Layers

| Layer | 역할 |
|-------|------|
| Evidence | 운영 증거 |
| Risk | 위험 분류 |
| Recommendation | 대응 권장 |
| Approval | Human Governance |
| Rollback | 안전 복구 |
| Verification | 결과 검증 |
| Timeline | replay/audit |
| Research | 연구 자산 |
| Dataset | reliability dataset |

---

## 41. Research-aware Reliability Runtime Direction

현재 방향의 핵심은 단순 운영 콘솔이 아니다.

**목표:**

> 운영 이벤트를 **설명 가능하고, 재현 가능하고, 정량 검증 가능하며, 논문화 가능한**  
> Reliability Research Asset으로 축적 가능한 **runtime formalization**

---

## 한 줄 핵심

> SRE Console Runtime의 목적은 단순 장애 대응 UI가 아니다.  
> → 운영 거버넌스, 실험, 정량 검증, 연구 자산 축적을 하나의  
> **explainable operational research runtime**으로 formalization 하는 것이다.