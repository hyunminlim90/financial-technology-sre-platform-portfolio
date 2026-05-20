# Runtime Decision Engine Contract

`protocols/runtime-decision-engine-contract.md`

---

## 1. 목적

이 문서는 FIN-SRE 플랫폼의 **Runtime Decision Engine Layer**를 정의한다.

Runtime Decision Engine의 목적은 단순 추천 생성이 아니다.

목적은:

- Evidence
- Knowledge Resolution
- Policy Runtime
- Risk Classification
- Guardrail
- Rollback/Verification

을 기반으로:

> **설명 가능하고, 재현 가능하며, 안전하고, 정량 검증 가능한**
> **Operational Reliability Decision Runtime**을 formalization 하는 것이다.

---

## 2. 핵심 개념

Decision Engine은 단순 inference engine이 아니다.

Decision Engine은:

- Evidence-aware
- Policy-aware
- Risk-aware
- Rollback-aware
- Verification-aware
- Human-governed

**operational decision orchestration runtime**이다.

---

## 3. Canonical Decision Definition

Decision Engine은 다음을 포함 가능.

| Component | 역할 |
|-----------|------|
| Evidence Resolution | observability/evidence 해석 |
| Knowledge Resolution | Scenario/Runbook 연결 |
| Policy Evaluation | 정책 평가 |
| Risk Classification | 위험 분류 |
| Guardrail Evaluation | 제한 규칙 |
| Recommendation Generation | 대응 생성 |
| Rollback Planning | rollback 설계 |
| Verification Planning | verification 설계 |

---

## 4. Human Governance Rule

Decision Engine은 **Human Governance 제거 금지**.

**원칙:**
- AI는 recommendation을 생성할 수 있다.
- Human이 실행 여부를 승인한다.

**금지:**
- ❌ autonomous infrastructure mutation
- ❌ AI-only production execution
- ❌ unreviewed operational action

---

## 5. Canonical Decision Lifecycle

Decision Runtime은 canonical lifecycle을 가져야 한다.

```
EVIDENCE_RECEIVED
    → KNOWLEDGE_RESOLVED
    → POLICY_EVALUATED
    → RISK_CLASSIFIED
    → RECOMMENDATION_GENERATED
    → APPROVAL_PENDING
```

또는:

```
RISK_CLASSIFIED
    → BLOCKED
```

---

## 6. Evidence-first Rule

Decision은 **Evidence 기반**이어야 한다.

**허용:**
- metrics
- logs
- traces
- timeline
- verification result
- rollback result

**금지:**
- hallucinated diagnosis
- fabricated incident state
- unsupported recommendation

---

## 7. Knowledge Resolution Rule

Decision Engine은 **Knowledge hierarchy**를 따라야 한다.

**우선순위:**

```
Preventive Design
    > Improvement
    > Postmortem
    > Runbook
    > Scenario
    > rag/docs
```

**원칙:** 가장 restrictive 하고 가장 안전한 규칙이 우선된다.

---

## 8. No Scenario → No Action Rule

**Scenario 없는 recommendation 금지.**

```
No Scenario → No Action
```

---

## 9. rag/docs Limitation Rule

rag/docs는 **action 결정 금지**.

**허용:**
- mechanism explanation
- metric interpretation
- failure analysis support

**금지:**
- direct action decision
- rollback override
- policy override

---

## 10. Risk-aware Decision Rule

Decision Runtime은 **risk-aware** 해야 한다.

Risk 분류 예:

- `LOW`
- `MEDIUM`
- `HIGH`
- `CRITICAL`
- `UNKNOWN`

**원칙:** HIGH 이상 → mandatory approval

---

## 11. Unknown State Rule

Unknown 상태는 **restrictive governance** 적용.

해당 상황 예:
- partial observability
- projection inconsistency
- missing metrics
- verification unavailable

**원칙:** `Unknown → risky recommendation blocked`

---

## 12. Rollback-aware Rule

모든 recommendation은 **rollback-aware** 해야 한다.

필수 포함:
- rollback requirement
- rollback trigger
- rollback verification
- rollback timeout

**원칙:** `No Rollback → No Risky Action`

---

## 13. Verification-aware Rule

모든 recommendation은 **verification-aware** 해야 한다.

포함:
- latency recovery validation
- queue stabilization validation
- payment consistency validation

---

## 14. FinTech Safety Rule

FinTech 환경에서는 **payment consistency가 최우선**이다.

**금지:**
- unsafe replay
- duplicate payment risk
- settlement corruption

**허용 가능:**
- idempotent-safe mitigation
- verified rollback
- verified fallback

---

## 15. Guardrail Rule

Decision Engine은 **Guardrail Runtime**을 통합해야 한다.

예:
- retry amplification guardrail
- payment safety guardrail
- rollback requirement guardrail

---

## 16. Policy-aware Rule

Decision Runtime은 **policy-aware** 해야 한다.

예:
- approval policy
- rollback policy
- verification policy
- blast radius policy

---

## 17. Blast Radius Rule

Decision Runtime은 **blast radius awareness**를 가져야 한다.

분류 예:
- `local`
- `partial`
- `cross-service`
- `global`

**원칙:** blast radius 증가 → stricter governance

---

## 18. Recommendation Sequencing Rule

Recommendation은 **sequencing awareness**를 가져야 한다.

```
Step 1 → verification
Step 2 → mitigation
Step 3 → escalation
```

---

## 19. Recommendation Downgrade Rule

Verification 실패 시 **safer recommendation으로 downgrade** 가능해야 한다.

```
aggressive scale-out → blocked
fallback recommendation → allowed
```

---

## 20. Retry Amplification Rule

Decision Runtime은 **retry amplification**을 이해 가능해야 한다.

```
retry storm
    → queue saturation
    → DB overload
    → latency propagation
```

---

## 21. Propagation-aware Rule

Decision Runtime은 **propagation-aware** 해야 한다.

예:
- dependency cascade
- tail latency propagation
- queue backlog propagation

---

## 22. Systems-Math Integration Rule

Decision Runtime은 **Systems-Math**와 연결 가능해야 한다.

예:
- Little's Law
- retry amplification
- queue utilization
- tail latency propagation

**원칙:** Systems-Math는 운영 현상 해석 계층이다.

---

## 23. SLO-aware Rule

Decision Runtime은 **SLO-aware** 해야 한다.

포함:
- error budget
- availability degradation
- P99 recovery

---

## 24. Quantitative Validation Rule

Decision Runtime은 **정량 검증 가능**해야 한다.

예:
- MTTR
- rollback success rate
- verification latency
- propagation reduction
- recommendation precision

---

## 25. Confidence-aware Rule

Decision Runtime은 **confidence-awareness**를 가져야 한다.

분류 예:
- `HIGH_CONFIDENCE`
- `MEDIUM_CONFIDENCE`
- `LOW_CONFIDENCE`
- `UNKNOWN`

**원칙:** `LOW_CONFIDENCE → risky recommendation 제한`

---

## 26. Runtime DTO Rule

Decision Runtime은 **canonical DTO**를 가져야 한다.

예:
- `DecisionContext`
- `EvidenceContext`
- `RecommendationContext`
- `RiskClassification`
- `RollbackPlan`
- `VerificationPlan`

---

## 27. Timeline Replay Rule

Decision lifecycle은 **replay 가능**해야 한다.

예:
- evidence replay
- policy replay
- decision replay
- rollback replay
- verification replay

---

## 28. Runtime Replay Rule

Decision Runtime은 **replayable** 해야 한다.

예:
- incident replay
- decision replay
- policy replay
- research replay

---

## 29. Recommendation Explainability Rule

Recommendation은 **explainable** 해야 한다.

포함:
- why recommendation generated
- why recommendation blocked
- why rollback required
- why approval required

**금지:** opaque recommendation

---

## 30. Research Compatibility Rule

Decision Runtime은 **Reliability Research**를 지원 가능해야 한다.

예:
- Human Approval effectiveness
- guardrail effectiveness
- rollback effectiveness
- recommendation precision

---

## 31. Reliability Dataset Rule

Decision Runtime은 **dataset accumulation**을 지원 가능해야 한다.

예:
- decision dataset
- rollback dataset
- verification dataset
- policy effectiveness dataset

---

## 32. Visibility Classification Rule

Decision Artifact는 **visibility classification**을 가져야 한다.

허용:
- `PUBLIC_PORTFOLIO`
- `PRIVATE_RESEARCH`
- `INTERNAL_OPERATION`
- `PAPER_CANDIDATE`
- `SANITIZED_EXPORT`

---

## 33. Sanitization Rule

Decision export는 **sanitization 가능**해야 한다.

제거 대상:
- internal topology
- customer payload
- secret
- token
- internal IP

---

## 34. Runtime Security Rule

Decision Runtime은 **privileged operational layer**다.

필수:
- authenticated access
- RBAC
- audit logging
- visibility control

**금지:**
- ❌ anonymous recommendation mutation
- ❌ unrestricted production decision
- ❌ public operational evidence exposure

---

## 35. Auditability Rule

Decision lifecycle은 **audit 가능**해야 한다.

포함:
- who approved
- what evidence used
- what policy evaluated
- what rollback generated
- what verification required

---

## 36. Immutable Audit Rule

Decision audit는 **append-only** 해야 한다.

**금지:**
- ❌ audit overwrite
- ❌ hidden recommendation mutation
- ❌ invisible policy mutation

---

## 37. Runtime Failure Rule

Decision Runtime failure는 **explicit** 해야 한다.

예:
- projection inconsistency
- policy resolution failure
- verification unavailable
- rollback unavailable

**금지:** silent decision degradation

---

## 38. Cross-document Linkage Rule

Decision Runtime은 **Knowledge Set**과 연결 가능해야 한다.

포함:
- Scenario
- Runbook
- Improvement
- Preventive Design
- Experiment
- Systems-Math
- Postmortem

---

## 39. Operational Reality Rule

Decision Runtime은 **현실 운영 기반**이어야 한다.

**허용:**
- real incident
- real rollback
- real observability
- real verification

**금지:**
- toy-only recommendation
- synthetic-only operational claim

---

## 40. Runtime Metrics Governance Rule

Decision metric은 **low-cardinality** 유지해야 한다.

**허용:**
- service
- domain
- severity
- risk_level
- policy_type

**금지:**
- customer identifier
- payment payload
- trace payload dump

---

## 41. Academic Extension Rule

장기적으로 다음 연구 방향 지원 가능.

예:
- policy-aware reliability orchestration
- rollback-aware decision systems
- verification-aware operational governance
- Human-in-the-loop AI-SRE systems

---

## 42. Anti-Pattern Rule

**금지:**
- ❌ rollback 없는 recommendation
- ❌ verification 없는 recommendation
- ❌ opaque recommendation
- ❌ autonomous production mutation
- ❌ fabricated diagnosis
- ❌ unsupported operational action

---

## 43. Non-Goals

Decision Runtime의 목표는 다음이 **아니다**.

- autonomous AGI operations
- ungoverned infrastructure mutation
- opaque operational orchestration
- unverifiable recommendation generation

---

## 44. Canonical Runtime Layers

| Layer | 역할 |
|-------|------|
| Evidence | observability evidence |
| Knowledge | RAG/knowledge resolution |
| Policy | governance/policy |
| Guardrail | safety restriction |
| Risk | risk classification |
| Recommendation | 대응 생성 |
| Rollback | 안전 복구 |
| Verification | 결과 검증 |

---

## 45. Reliability Research Formalization Direction

현재 방향의 핵심은 단순 AI recommendation system이 아니다.

**목표:**

> 운영 observability와 knowledge runtime을
> 설명 가능하고, 재현 가능하며, 정량 검증 가능하고, 논문화 가능한
> **Operational Reliability Decision Runtime으로 formalization** 하는 것이다.

---

## 한 줄 핵심

> Runtime Decision Engine의 목적은 단순 추천 생성이 아니다.
> → observability, policy, rollback, verification, guardrail을 통합하여
> **재현 가능하고 검증 가능한 Reliability Decision Runtime으로 formalization** 하는 것이다.