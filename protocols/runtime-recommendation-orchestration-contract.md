# Runtime Recommendation Orchestration Contract

`protocols/runtime-recommendation-orchestration-contract.md`

---

## 1. 목적

이 문서는 FIN-SRE 플랫폼의 **Runtime Recommendation Orchestration Layer**를 정의한다.

Recommendation Orchestration의 목적은 단순 recommendation list 생성이 아니다.

목적은:

- Evidence
- Decision Runtime
- Policy Runtime
- Risk Classification
- Rollback/Verification
- Knowledge Resolution

을 기반으로:

> **안전하고, 설명 가능하며, 순차적이며, 재현 가능하고, 정량 검증 가능한**
> **Operational Recommendation Orchestration Runtime**을 formalization 하는 것이다.

---

## 2. 핵심 개념

Recommendation Orchestration은 단순 inference chaining이 아니다.

Recommendation Runtime은:

- Evidence-aware
- Policy-aware
- Rollback-aware
- Verification-aware
- Propagation-aware
- Human-governed

**operational recommendation sequencing runtime**이다.

---

## 3. Canonical Recommendation Definition

Recommendation Runtime은 다음을 포함 가능.

| Component | 역할 |
|-----------|------|
| Recommendation Resolution | 대응 후보 생성 |
| Recommendation Prioritization | 우선순위 결정 |
| Recommendation Sequencing | 단계 orchestration |
| Recommendation Downgrade | safer fallback |
| Rollback Planning | rollback orchestration |
| Verification Planning | verification orchestration |
| Escalation Planning | escalation orchestration |

---

## 4. Human Governance Rule

Recommendation Runtime은 **Human Governance 제거 금지**.

**원칙:**
- AI는 recommendation orchestration을 생성할 수 있다.
- Human이 execution 여부를 승인한다.

**금지:**
- ❌ autonomous production mutation
- ❌ AI-only infrastructure orchestration
- ❌ unapproved operational escalation

---

## 5. Canonical Recommendation Lifecycle

Recommendation Runtime은 canonical lifecycle을 가져야 한다.

```
GENERATED
    → PRIORITIZED
    → SEQUENCED
    → APPROVAL_PENDING
    → VERIFIED
```

또는:

```
GENERATED
    → BLOCKED
```

---

## 6. Recommendation Sequencing Rule

Recommendation은 **sequencing-aware** 해야 한다.

```
Step 1 → observability validation
Step 2 → mitigation recommendation
Step 3 → rollback preparation
Step 4 → escalation
```

**원칙:** unordered operational action 금지

---

## 7. Recommendation Prioritization Rule

Recommendation은 **priority-aware** 해야 한다.

```
Safety
    > Stability
    > Data Consistency
    > Availability
    > Performance
```

---

## 8. Recommendation Downgrade Rule

Verification 실패 시 **safer recommendation으로 downgrade** 가능해야 한다.

```
aggressive scale-out → blocked
fallback activation  → allowed
```

---

## 9. Recommendation Escalation Rule

Recommendation Runtime은 **escalation-aware** 해야 한다.

| 상황 | 결과 |
|------|------|
| local mitigation failure | service escalation |
| cross-service degradation | operational escalation |
| payment consistency risk | mandatory escalation |

---

## 10. No Scenario → No Recommendation Rule

**Scenario 없는 recommendation 금지.**

```
No Scenario → No Recommendation
```

---

## 11. Knowledge-aware Rule

Recommendation Runtime은 **Knowledge hierarchy**를 따라야 한다.

```
Preventive Design
    > Improvement
    > Postmortem
    > Runbook
    > Scenario
    > rag/docs
```

**원칙:** 가장 restrictive 하고 가장 안전한 recommendation이 우선된다.

---

## 12. rag/docs Limitation Rule

rag/docs는 **orchestration override 금지**.

**허용:**
- metric interpretation
- failure mechanism explanation
- propagation analysis support

**금지:**
- direct orchestration override
- rollback override
- policy override

---

## 13. Evidence-first Rule

Recommendation은 **Evidence 기반**이어야 한다.

**허용:**
- metrics
- logs
- traces
- timeline
- verification result
- rollback result

**금지:**
- hallucinated orchestration
- fabricated runtime state
- unsupported escalation

---

## 14. Risk-aware Rule

Recommendation Runtime은 **risk-aware** 해야 한다.

Risk 분류 예: `LOW` / `MEDIUM` / `HIGH` / `CRITICAL` / `UNKNOWN`

**원칙:** HIGH 이상 → mandatory approval

---

## 15. Blast Radius Rule

Recommendation Runtime은 **blast radius awareness**를 가져야 한다.

분류 예: `local` / `partial` / `cross-service` / `global`

**원칙:** blast radius 증가 → stricter orchestration governance

---

## 16. Rollback-aware Rule

모든 recommendation orchestration은 **rollback-aware** 해야 한다.

필수 포함:
- rollback requirement
- rollback trigger
- rollback verification
- rollback timeout

**원칙:** `No Rollback → No Risky Recommendation`

---

## 17. Verification-aware Rule

모든 recommendation orchestration은 **verification-aware** 해야 한다.

포함:
- latency recovery validation
- queue stabilization validation
- payment consistency validation

---

## 18. Retry Amplification Rule

Recommendation Runtime은 **retry amplification**을 이해 가능해야 한다.

```
retry storm
    → queue saturation
    → DB overload
    → latency propagation
```

---

## 19. Propagation-aware Rule

Recommendation Runtime은 **propagation-aware** 해야 한다.

예:
- dependency cascade
- tail latency propagation
- queue backlog propagation

---

## 20. Recommendation Retry Rule

동일 recommendation 반복은 **제한**되어야 한다.

```
same restart recommendation
    → repeated failure
    → recommendation blocked
```

**원칙:** 같은 recommendation 반복 → 장애 증폭 가능

---

## 21. Recommendation Convergence Rule

Recommendation Runtime은 **convergence-aware** 해야 한다.

**목표:** safe stabilization

**금지:**
- orchestration oscillation
- aggressive recommendation loop

---

## 22. Recommendation Conflict Resolution Rule

충돌 recommendation은 **restrictive governance** 적용.

```
scale-out recommendation
    vs
rollback recommendation
    → safest recommendation wins
```

---

## 23. Recommendation Explainability Rule

Recommendation orchestration은 **explainable** 해야 한다.

포함:
- why recommendation generated
- why recommendation sequenced
- why recommendation blocked
- why escalation required

**금지:** opaque orchestration

---

## 24. Systems-Math Integration Rule

Recommendation Runtime은 **Systems-Math**와 연결 가능해야 한다.

예:
- Little's Law
- retry amplification
- queue utilization
- tail latency propagation

**원칙:** Systems-Math는 recommendation reasoning layer다.

---

## 25. SLO-aware Rule

Recommendation Runtime은 **SLO-aware** 해야 한다.

포함:
- error budget
- availability degradation
- P99 recovery

---

## 26. Quantitative Validation Rule

Recommendation Runtime은 **정량 검증 가능**해야 한다.

예:
- recommendation precision
- rollback success rate
- verification latency
- propagation reduction

---

## 27. Confidence-aware Rule

Recommendation Runtime은 **confidence-awareness**를 가져야 한다.

분류 예: `HIGH_CONFIDENCE` / `MEDIUM_CONFIDENCE` / `LOW_CONFIDENCE` / `UNKNOWN`

**원칙:** `LOW_CONFIDENCE → risky recommendation 제한`

---

## 28. Runtime DTO Rule

Recommendation Runtime은 **canonical DTO**를 가져야 한다.

예:
- `RecommendationContext`
- `RecommendationSequence`
- `RecommendationStep`
- `RecommendationEscalation`
- `RecommendationVerification`

---

## 29. Timeline Replay Rule

Recommendation lifecycle은 **replay 가능**해야 한다.

예:
- recommendation replay
- rollback replay
- verification replay
- policy replay

---

## 30. Runtime Replay Rule

Recommendation Runtime은 **replayable** 해야 한다.

예:
- incident replay
- recommendation replay
- policy replay
- research replay

---

## 31. FinTech Safety Rule

FinTech 환경에서는 **payment consistency가 최우선**이다.

**금지:**
- unsafe replay
- duplicate payment risk
- settlement corruption

**허용 가능:**
- idempotent-safe mitigation
- verified fallback
- verified rollback

---

## 32. Policy-aware Rule

Recommendation Runtime은 **policy-aware** 해야 한다.

예:
- approval policy
- rollback policy
- verification policy
- blast radius policy

---

## 33. Guardrail Rule

Recommendation Runtime은 **Guardrail Runtime**을 통합해야 한다.

예:
- payment safety guardrail
- retry amplification guardrail
- rollback requirement guardrail

---

## 34. Unknown State Rule

Unknown 상태는 **restrictive governance** 적용.

해당 상황 예:
- missing metrics
- partial observability
- projection inconsistency
- verification unavailable

**원칙:** `Unknown → risky orchestration blocked`

---

## 35. Runtime Security Rule

Recommendation Runtime은 **privileged operational layer**다.

**필수:**
- authenticated access
- RBAC
- audit logging
- visibility control

**금지:**
- ❌ anonymous recommendation mutation
- ❌ unrestricted orchestration execution
- ❌ public operational evidence exposure

---

## 36. Auditability Rule

Recommendation lifecycle은 **audit 가능**해야 한다.

포함:
- who approved
- what evidence used
- what policy evaluated
- what rollback generated
- what verification required

---

## 37. Immutable Audit Rule

Recommendation audit는 **append-only** 해야 한다.

**금지:**
- ❌ audit overwrite
- ❌ hidden orchestration mutation
- ❌ invisible policy mutation

---

## 38. Runtime Failure Rule

Recommendation Runtime failure는 **explicit** 해야 한다.

예:
- projection inconsistency
- policy resolution failure
- verification unavailable
- rollback unavailable

**금지:** silent orchestration degradation

---

## 39. Reliability Dataset Rule

Recommendation Runtime은 **dataset accumulation**을 지원 가능해야 한다.

예:
- recommendation dataset
- rollback dataset
- verification dataset
- policy effectiveness dataset

---

## 40. Research Compatibility Rule

Recommendation Runtime은 **Reliability Research**를 지원 가능해야 한다.

예:
- Human Approval effectiveness
- guardrail effectiveness
- rollback effectiveness
- recommendation precision

---

## 41. Visibility Classification Rule

Recommendation Artifact는 **visibility classification**을 가져야 한다.

허용:
- `PUBLIC_PORTFOLIO`
- `PRIVATE_RESEARCH`
- `INTERNAL_OPERATION`
- `PAPER_CANDIDATE`
- `SANITIZED_EXPORT`

---

## 42. Sanitization Rule

Recommendation export는 **sanitization 가능**해야 한다.

제거 대상:
- internal topology
- customer payload
- secret
- token
- internal IP

---

## 43. Cross-document Linkage Rule

Recommendation Runtime은 **Knowledge Set**과 연결 가능해야 한다.

포함:
- Scenario
- Runbook
- Improvement
- Preventive Design
- Experiment
- Systems-Math
- Postmortem

---

## 44. Runtime Metrics Governance Rule

Recommendation metric은 **low-cardinality** 유지해야 한다.

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

## 45. Operational Reality Rule

Recommendation Runtime은 **현실 운영 기반**이어야 한다.

**허용:**
- real incident
- real rollback
- real observability
- real verification

**금지:**
- toy-only orchestration
- synthetic-only operational claim

---

## 46. Academic Extension Rule

장기적으로 다음 연구 방향 지원 가능.

예:
- policy-aware recommendation orchestration
- rollback-aware orchestration systems
- verification-aware operational sequencing
- Human-in-the-loop AI-SRE orchestration

---

## 47. Anti-Pattern Rule

**금지:**
- ❌ rollback 없는 orchestration
- ❌ verification 없는 orchestration
- ❌ opaque orchestration
- ❌ autonomous production mutation
- ❌ orchestration oscillation
- ❌ unsupported escalation

---

## 48. Non-Goals

Recommendation Runtime의 목표는 다음이 **아니다**.

- autonomous AGI operations
- ungoverned orchestration execution
- opaque recommendation chaining
- unverifiable escalation runtime

---

## 49. Canonical Runtime Layers

| Layer | 역할 |
|-------|------|
| Evidence | observability evidence |
| Knowledge | RAG/knowledge resolution |
| Policy | governance/policy |
| Guardrail | safety restriction |
| Risk | risk classification |
| Recommendation | 대응 orchestration |
| Rollback | 안전 복구 |
| Verification | 결과 검증 |

---

## 50. Reliability Research Formalization Direction

현재 방향의 핵심은 단순 AI recommendation pipeline이 아니다.

**목표:**

> 운영 observability와 knowledge runtime을
> 설명 가능하고, 재현 가능하며, 정량 검증 가능하고, 논문화 가능한
> **Operational Reliability Recommendation Orchestration Runtime으로 formalization** 하는 것이다.

---

## 한 줄 핵심

> Recommendation Orchestration의 목적은 단순 recommendation chaining이 아니다.
> → observability, rollback, verification, propagation, guardrail을 통합하여
> **재현 가능하고 검증 가능한 Reliability Recommendation Runtime으로 formalization** 하는 것이다.