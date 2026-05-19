# protocols/runtime-policy-engine-contract

## 1. 목적

이 문서는 AI-SRE 플랫폼의 Runtime Policy Engine이 Recommendation Candidate를 검증, 제한, 차단, 승인 요구하는 governance lifecycle과 policy evaluation contract를 정의한다.

> **Runtime Policy Engine의 목적은 Recommendation을 생성하는 것이 아니다.**  
> 목적은 "위험한 운영 판단을 제한하고 안전한 운영 거버넌스를 강제하는 것" 이다.

---

## 2. 핵심 개념

Runtime Policy Engine은 Recommendation Runtime 앞단의 governance layer다.

**구조:**

```
RecommendationCandidate
        ↓
Constraint Evaluation
        ↓
Policy Evaluation
        ↓
Risk Governance
        ↓
Approval Governance
        ↓
Governed Recommendation
```

---

## 3. Policy-first Governance Rule

모든 Recommendation은 Policy Evaluation을 통과해야 한다.

**금지:**

- ❌ policy bypass
- ❌ direct recommendation execution
- ❌ governance-free action

**원칙:**

모든 Recommendation은 governance 대상이다.

---

## 4. Canonical Policy Engine Role

Policy Engine은 다음 역할을 가진다.

**허용:**

- recommendation validation
- constraint evaluation
- approval governance
- rollback enforcement
- verification enforcement
- risk governance

**금지:**

- ❌ infrastructure mutation
- ❌ kubectl execution
- ❌ Terraform execution
- ❌ autonomous remediation

---

## 5. Policy Evaluation Lifecycle Rule

Runtime Policy Evaluation은 canonical lifecycle을 따라야 한다.

**단계:**

```
RecommendationCandidate
→ Constraint Merge
→ Policy Evaluation
→ Guardrail Evaluation
→ Risk Classification
→ Approval Requirement
→ Governed Recommendation
```

---

## 6. Constraint Merge Rule

Policy Engine은 constraint를 merge 해야 한다.

**입력:**

```
Runbook
+
Improvement
+
Preventive Design
+
Operational Policies
```

**출력:**

- `ConstraintEvaluationResult`

---

## 7. Policy Priority Rule

Policy는 우선순위를 가진다.

**기본 우선순위:**

```
PaymentSafetyPolicy
→ RollbackPolicy
→ VerificationPolicy
→ ApprovalPolicy
→ OperationalPolicy
```

**원칙:**

더 restrictive한 policy가 우선된다.

---

## 8. Payment Safety Policy Rule

Payment Safety는 최상위 Policy다.

**보호 대상:**

- payment integrity
- duplicate payment prevention
- idempotency
- settlement consistency

**금지 예시:**

- ❌ unsafe retry amplification
- ❌ uncontrolled replay
- ❌ duplicate execution risk

---

## 9. Rollback Policy Rule

모든 Recommendation은 rollback 가능해야 한다.

**필수:**

- rollback plan
- rollback trigger
- rollback verification

**원칙:**

Rollback 없는 Recommendation reject

---

## 10. Verification Policy Rule

모든 Recommendation은 verification 가능해야 한다.

**필수:**

- verification metrics
- verification traces
- verification alerts

**원칙:**

Verification 없는 Recommendation reject

---

## 11. Approval Policy Rule

고위험 Recommendation은 Human Approval을 요구해야 한다.

| Risk | Approval |
|---|---|
| `LOW` | optional |
| `MEDIUM` | recommended |
| `HIGH` | mandatory |
| `CRITICAL` | mandatory + escalation |

**원칙:**

Human approval bypass 금지

---

## 12. Constraint Evaluation Rule

Constraint Evaluation은 unsafe action을 제한해야 한다.

**예:**

```
retry amplification detected
→ aggressive scale-out block

duplicate payment risk
→ replay action block
```

---

## 13. Conflict Resolution Rule

Constraint 충돌 시 가장 restrictive한 rule이 우선된다.

**우선순위:**

```
Preventive Design
→ Improvement
→ Operational Policy
→ Runbook
```

**원칙:**

가장 안전한 제한이 우선된다.

---

## 14. Runtime Blocking Rule

Policy Engine은 unsafe recommendation을 차단할 수 있어야 한다.

**예:**

```
rollback unavailable
→ recommendation blocked

verification impossible
→ recommendation blocked
```

---

## 15. Risk Governance Rule

Policy Engine은 Risk Classification을 수행해야 한다.

| Risk | 의미 |
|---|---|
| `LOW` | read-only |
| `MEDIUM` | reversible |
| `HIGH` | production impact |
| `CRITICAL` | payment integrity risk |

---

## 16. Confidence Governance Rule

Confidence가 낮으면 Recommendation은 제한되어야 한다.

**예:**

```
LOW confidence
+
HIGH risk
→ recommendation reject
```

**원칙:**

uncertain high-risk operation 금지

---

## 17. Unknown Handling Rule

Unknown 상태는 안전하지 않은 것으로 간주된다.

**예:**

- missing metrics
- partial traces
- projection unavailable
- stale retrieval

**원칙:**

Unknown → unsafe recommendation 금지

---

## 18. Degraded Governance Rule

Runtime degradation 시 Policy는 더 restrictive해져야 한다.

**예:**

```
observability degradation
→ risky action block

retrieval degradation
→ recommendation confidence reduction
```

---

## 19. Governance Timeline Integration Rule

Policy Engine은 Governance Timeline과 연결될 수 있어야 한다.

**예:**

- approval history
- rollback history
- policy violation history
- verification history

---

## 20. Evidence-aware Policy Rule

Policy는 EvidenceContext 기반으로 평가되어야 한다.

**예:**

```
SLO burn detected
+
retry amplification
→ scale-out block
```

---

## 21. Systems-Math Governance Rule

Policy Engine은 Systems-Math reasoning을 사용할 수 있다.

**예:**

- queue collapse risk
- retry amplification risk
- tail latency propagation

**원칙:**

Systems-Math는 governance 설명 계층이다.

---

## 22. Experiment Governance Rule

Experiment 결과는 Policy Governance에 사용될 수 있다.

**예:**

```
historical rollback failure
→ recommendation confidence reduction
```

---

## 23. Explainability Rule (핵심)

Policy Evaluation은 explainable 해야 한다.

**설명 가능 대상:**

- 왜 recommendation이 차단되었는가
- 왜 approval이 필요한가
- 왜 rollback이 mandatory인가
- 왜 policy violation이 발생했는가

**원칙:**

설명 불가능한 governance 금지

---

## 24. Recommendation Compatibility Rule

Policy Engine은 Recommendation Runtime과 호환되어야 한다.

**지원 대상:**

- `RecommendationContext`
- `RollbackPlan`
- `VerificationPlan`
- `RiskClassification`
- `ApprovalRequirement`

---

## 25. Runtime DTO Rule

Policy Runtime은 canonical DTO를 가져야 한다.

**예:**

- `PolicyEvaluationResult`
- `ConstraintViolation`
- `ApprovalRequirement`
- `GovernanceDecision`

---

## 26. Runtime Metrics Governance Rule

Policy metrics는 low-cardinality를 유지해야 한다.

**허용:**

- `policy_name`
- `result`
- `risk_level`
- `confidence`

**금지:**

- `incident_id`
- `recommendation_id`
- `raw recommendation`

---

## 27. Recommendation Replay Rule

Policy Governance는 replay 가능해야 한다.

**예:**

- policy replay
- approval replay
- constraint replay
- recommendation replay

---

## 28. Runtime Auditability Rule

Policy lifecycle은 audit 가능해야 한다.

**예:**

- who approved
- which policy blocked
- why recommendation rejected
- why escalation occurred

---

## 29. FinTech Safety Rule

Policy Engine은 FinTech Safety를 최우선으로 해야 한다.

**최우선 보호 대상:**

- payment integrity
- duplicate payment prevention
- settlement consistency
- idempotency

**원칙:**

공격적 자동화보다 결제 안전성이 우선된다.

---

## 30. Human-in-the-loop Rule

Policy Engine은 Human Governance를 제거하지 않는다.

**원칙:**

AI는 recommendation만 생성한다.  
Human이 최종 승인한다.

---

## 31. Runtime Security Rule

Policy Runtime은 내부 전용이어야 한다.

**필수:**

- authenticated policy evaluation
- internal-only governance access
- audit-protected approval lifecycle

**금지:**

- ❌ external policy bypass
- ❌ public approval mutation

---

## 32. Runtime Failure Handling Rule

Policy Runtime failure는 explicit 해야 한다.

**예:**

- policy evaluation failure
- constraint merge failure
- approval routing failure
- timeline projection failure

**원칙:**

silent governance failure 금지

---

## 33. Research Compatibility Rule

Policy Engine은 Reliability Research를 지원해야 한다.

**예:**

- policy effectiveness analysis
- rollback effectiveness
- operator trust analysis
- governance explainability evaluation

---

## 34. Future Runtime Rule

현재 Policy Engine은 recommendation governance 중심이다.

**장기적으로:**

```
Operational Governance Runtime
```

으로 발전할 수 있다.

**예:**

- adaptive governance
- dynamic policy weighting
- runtime risk prediction

---

## 35. Anti-Pattern Rule

**금지:**

- ❌ policy-free recommendation
- ❌ rollback 없는 recommendation
- ❌ unverifiable recommendation
- ❌ unsafe autonomous remediation
- ❌ opaque governance
- ❌ human approval bypass
- ❌ payment-risking recommendation

---

## 36. Non-Goals

Policy Engine의 목표는 다음이 아니다.

- AGI operations replacement
- autonomous production control
- human-free governance
- unsafe self-healing

---

## 37. 핵심 원칙

| 계층 | 역할 |
|---|---|
| Constraint | 운영 제한 |
| Policy | 운영 규칙 |
| Risk | 위험 분류 |
| Approval | Human Governance |
| Rollback | 안전 복구 |
| Verification | 결과 검증 |
| Timeline | 감사 / replay |
| Human | 최종 책임 |

---

> 🎯 **한 줄 핵심**
>
> Policy Engine의 목적은 Recommendation을 생성하는 것이 아니다.  
> → 위험한 운영 판단을 제한하고 안전한 운영 거버넌스를 강제하는 것이다.