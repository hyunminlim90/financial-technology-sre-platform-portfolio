# Runtime Approval Governance Contract

`protocols/runtime-approval-governance-contract.md`

---

## 1. 목적

이 문서는 FIN-SRE 플랫폼의 **Runtime Approval Governance Layer**를 정의한다.

Approval Governance의 목적은 단순 승인 UI 제공이 아니다.

> 목적은 **AI Recommendation → Human Governance → Operational Accountability** 를  
> formalized runtime chain으로 보장하는 것이다.

---

## 2. 핵심 개념

Approval Runtime은 **Human-in-the-loop 운영 거버넌스의 핵심 계층**이다.

**구조:**

```
Evidence
→ Risk Classification
→ Policy Evaluation
→ Guardrail Evaluation
→ Recommendation
→ Approval Governance
→ Execution Plan
→ Verification
```

> **AI Recommendation ≠ Execution**

---

## 3. Canonical Approval Definition

Approval은 다음을 의미한다.

| Approval Type | 의미 |
|---------------|------|
| Human Review | 운영 검토 |
| Operational Authorization | 운영 승인 |
| Risk Acceptance | 위험 수용 |
| Execution Authorization | 실행 허가 |
| Escalation Decision | 상위 escalation |
| Research Approval | 실험 승인 |

---

## 4. Human-in-the-loop Rule

FIN-SRE 플랫폼은 **Human-in-the-loop를 제거하지 않는다.**

**원칙:**

- AI는 추천한다.
- Human이 승인한다.
- Human이 실행한다.
- AI는 결과를 분석한다.

**금지:**

- ❌ autonomous operational execution
- ❌ self-approved AI execution
- ❌ human bypass governance

---

## 5. Approval Mandatory Rule

다음은 반드시 **Human Approval 필요**:

- `scale-out` / `scale-in`
- `retry policy mutation`
- `timeout mutation`
- `traffic shift`
- `circuit breaker mutation`
- `DB operation`
- `Redis operation`
- `payment-impacting operation`

---

## 6. Payment Safety Rule

FinTech 환경에서는 **결제 안전성이 최우선**이다.

다음은 HIGH 이상 approval mandatory:

- `duplicate payment possibility`
- `idempotency uncertainty`
- `settlement inconsistency`
- `payment replay uncertainty`

> Payment ambiguity → **approval mandatory**

---

## 7. Approval Severity Rule

Approval은 **runtime risk severity 기반**이어야 한다.

| Risk | Approval |
|------|----------|
| LOW | optional |
| MEDIUM | recommended |
| HIGH | mandatory |
| CRITICAL | escalation mandatory |

---

## 8. Approval Escalation Rule

다음은 **escalation mandatory**:

- `CRITICAL risk`
- `rollback unavailable`
- `verification impossible`
- `payment consistency uncertainty`
- `cross-service propagation risk`

---

## 9. Multi-stage Approval Rule

일부 operation은 **multi-stage approval** 요구 가능.

```
AI Recommendation
→ Primary Operator Approval
→ Senior SRE Approval
→ Execution Authorization
```

---

## 10. Approval Explainability Rule

모든 approval request는 **explainable** 해야 한다.

**포함 필수:** risk reason, policy reason, guardrail result, rollback availability, verification requirement, propagation possibility

**금지:** opaque approval request

---

## 11. Recommendation Context Rule

Approval Runtime은 **RecommendationContext 기반**이어야 한다.

포함 가능:

- `EvidenceContext`
- `RiskClassification`
- `PolicyEvaluation`
- `GuardrailDecision`
- `RollbackRequirement`
- `VerificationRequirement`

---

## 12. Approval Rejection Rule

Human은 recommendation을 **reject 가능**해야 한다.

- `unsafe mitigation`
- `insufficient evidence`
- `rollback uncertainty`
- `business risk concern`

> Human rejection → **canonical governance outcome**

---

## 13. Approval State Machine Rule

Approval Runtime은 **canonical lifecycle**을 가져야 한다.

```
PENDING → APPROVED → EXECUTION_PENDING → EXECUTED → VERIFIED

또는

PENDING → REJECTED
```

---

## 14. Execution Boundary Rule

Approval은 execution 자체를 의미하지 않는다.

> **Approval ≠ automatic execution**  
> Execution은 항상 **external operational action**이다.

---

## 15. Dry-run Execution Rule

Execution Plan은 **dry-run artifact**여야 한다.

**포함 가능:** rollback requirement, verification requirement, risk summary, execution scope, blast radius

**금지:** direct runtime mutation

---

## 16. Blast Radius Rule

Approval Runtime은 **blast radius awareness**를 가져야 한다.

- `local` / `partial` / `global` / `cross-domain`

> Blast radius 증가 → **stricter approval**

---

## 17. Rollback-aware Approval Rule

Rollback 없는 operation은 **approval 불가능**.

- `rollback unavailable`
- `rollback ambiguous`
- `rollback unverified`

> **No Rollback → No Approval**

---

## 18. Verification-aware Approval Rule

Verification 없는 operation은 **approval 제한 대상**.

- `verification impossible`
- `verification partial`
- `verification stale`

---

## 19. Runtime Drift Rule

Approval Runtime은 **GitOps drift-aware** 해야 한다.

- `manual kubectl mutation`
- `configuration divergence`
- `ArgoCD inconsistency`

> **GitOps source-of-truth 유지**

---

## 20. Approval Auditability Rule

모든 approval lifecycle은 **audit 가능**해야 한다.

포함: who approved, when approved, what risk existed, what recommendation existed, what verification existed

---

## 21. Immutable Audit Rule

Approval audit log는 **append-only** 해야 한다.

**금지:**

- ❌ approval overwrite
- ❌ audit mutation
- ❌ approval deletion

---

## 22. Runtime DTO Rule

Approval Runtime은 canonical DTO를 가져야 한다.

- `ApprovalRequest`
- `ApprovalDecision`
- `ApprovalEscalation`
- `ApprovalContext`
- `ApprovalAuditRecord`

---

## 23. Runtime Replay Rule

Approval lifecycle은 **replay 가능**해야 한다.

- approval replay
- decision replay
- verification replay
- incident replay

---

## 24. Evidence Integrity Rule

Approval Runtime은 **hallucinated evidence 사용 금지**.

**허용:** verified metrics, verified traces, verified logs, verified alert state

**금지:** hallucinated evidence, speculative operational mutation

---

## 25. Unknown State Rule

Unknown 상태는 **restrictive governance** 적용.

- `missing evidence`
- `projection lag`
- `timeline inconsistency`
- `retrieval degradation`

> Unknown → **stricter approval**

---

## 26. Operational Safety Priority Rule

Approval Governance는 다음 **우선순위** 유지.

```
payment integrity
> consistency
> rollback safety
> verification capability
> availability
> performance
```

---

## 27. Guardrail Integration Rule

Approval Runtime은 **Guardrail 결과를 존중**해야 한다.

- `BLOCKED`
- `RESTRICTED`
- `REQUIRES_ESCALATION`

> **Guardrail block override 금지**

---

## 28. Policy Integration Rule

Approval Runtime은 **Policy Engine 결과 기반**이어야 한다.

- `ApprovalPolicy`
- `RollbackPolicy`
- `VerificationPolicy`
- `PaymentSafetyPolicy`

---

## 29. Experiment Approval Rule

Experiment Runtime도 **approval governance 필요**.

- failure injection
- traffic experiment
- chaos execution

> Experiment → **human-approved only**

---

## 30. Research Governance Rule

Research candidate artifact도 **approval 대상** 가능.

- paper candidate
- experiment publication
- sanitized export

---

## 31. Visibility Governance Rule

Approval artifact는 **visibility classification**을 가질 수 있다.

- `PUBLIC_PORTFOLIO`
- `PRIVATE_RESEARCH`
- `INTERNAL_OPERATION`
- `PAPER_CANDIDATE`
- `SANITIZED_EXPORT`

---

## 32. Quantitative Validation Rule

Approval Governance는 **정량 검증 가능**해야 한다.

- approval latency
- false positive reduction
- rollback success improvement
- propagation reduction

---

## 33. Systems-Math Integration Rule

Approval Runtime은 **Systems-Math 기반 reasoning** 가능.

- queue collapse risk
- retry amplification
- tail latency propagation
- Little's Law

---

## 34. Research Compatibility Rule

Approval Runtime은 **Reliability Research 지원** 가능해야 한다.

- Human Approval effectiveness
- approval latency impact
- false positive reduction
- guardrail effectiveness

---

## 35. Runtime Failure Rule

Approval Runtime failure는 **explicit** 해야 한다.

- `approval unavailable`
- `approval inconsistency`
- `audit corruption`
- `timeline inconsistency`

> **silent approval degradation 금지**

---

## 36. Runtime Security Rule

Approval Runtime은 **내부 운영 계층**이다.

**필수:** authenticated access, RBAC, audit logging, internal-only routing

**금지:**

- ❌ public approval mutation
- ❌ anonymous approval
- ❌ external approval override

---

## 37. Anti-Pattern Rule

**금지:**

- ❌ AI self-approval
- ❌ rollback 없는 approval
- ❌ verification 없는 approval
- ❌ payment-risking approval
- ❌ opaque approval chain
- ❌ auditless approval

---

## 38. Non-Goals

Approval Governance의 목표는 다음이 **아니다.**

- autonomous execution
- human-free operation
- opaque AI governance
- self-mutating infrastructure

---

## 39. 핵심 Runtime Chain

| 계층 | 역할 |
|------|------|
| Evidence | 운영 증거 |
| Risk | 위험 분류 |
| Policy | 운영 정책 |
| Guardrail | runtime 안전 경계 |
| Recommendation | 대응 추천 |
| Approval | Human Governance |
| Rollback | 복구 안전성 |
| Verification | 결과 검증 |
| Timeline | replay/audit |

---

## 40. Reliability Research Direction

장기적으로 Approval Governance는 다음 연구 방향 지원 가능.

- Human-in-the-loop effectiveness
- approval delay vs safety tradeoff
- guardrail-driven operational governance
- approval-aware reliability systems

---

## 한 줄 핵심

> Runtime Approval Governance의 목적은 단순 승인 UI가 아니다.  
> → AI Recommendation을 Human Governance 아래에 두는 **explainable operational authorization runtime**을 formalization 하는 것이다.