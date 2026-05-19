# protocols/runtime-guardrail-contract

## 1. 목적

이 문서는 AI-SRE 플랫폼의 Runtime Guardrail Layer가 Recommendation Runtime의 위험한 운영 판단을 차단, 제한, 격리(quarantine), escalation 처리하기 위한 runtime governance contract를 정의한다.

> **Runtime Guardrail의 목적은 Recommendation을 생성하는 것이 아니다.**  
> 목적은 "운영 안전 경계를(runtime safety boundary) 강제하는 것" 이다.

---

## 2. 핵심 개념

Runtime Guardrail은 Recommendation Runtime의 마지막 안전 경계다.

**구조:**

```
Evidence
→ Retrieval
→ Recommendation
→ Policy Evaluation
→ Guardrail Evaluation
→ Human Review Boundary
→ External Human Execution
```

**원칙:**

`Recommendation ≠ Execution`

Guardrail은 AI recommendation이 실행 경계를 넘지 못하게 한다.

---

## 3. Guardrail Canonical Role

Runtime Guardrail은 다음 역할을 가진다.

**허용:**

- unsafe recommendation block
- runtime quarantine
- approval enforcement
- execution boundary enforcement
- runtime escalation
- risk isolation

**금지:**

- ❌ infrastructure mutation
- ❌ kubectl execution
- ❌ Terraform apply
- ❌ production self-healing
- ❌ autonomous remediation

---

## 4. Policy vs Guardrail Rule

Policy와 Guardrail은 다르다.

| 계층 | 역할 |
|---|---|
| Policy | 운영 판단 규칙 |
| Guardrail | runtime 안전 차단 |

**예:**

```
Policy:
retry amplification 상태에서 scale-out 위험

Guardrail:
실제 runtime에서 scale-out recommendation hard-block
```

---

## 5. Runtime Guardrail Lifecycle

Guardrail Runtime은 canonical lifecycle을 따라야 한다.

```
RecommendationCandidate
→ Policy Evaluation
→ Guardrail Evaluation
→ Risk Isolation
→ Approval Enforcement
→ Governed Recommendation
```

---

## 6. Runtime Blocking Rule

Guardrail은 위험한 Recommendation을 차단할 수 있어야 한다.

**예:**

```
rollback unavailable       → HARD_BLOCK
verification impossible    → HARD_BLOCK
duplicate payment risk     → QUARANTINE
```

---

## 7. Guardrail Severity Rule

Guardrail은 severity를 가져야 한다.

| Severity | 의미 |
|---|---|
| `WARNING` | operator warning |
| `SOFT_BLOCK` | approval required |
| `HARD_BLOCK` | runtime deny |
| `QUARANTINE` | isolated recommendation |
| `ESCALATION_REQUIRED` | escalation mandatory |

---

## 8. Human Approval Enforcement Rule

Guardrail은 Human Approval을 강제해야 한다.

**예:**

```
HIGH risk     → approval mandatory
CRITICAL risk → escalation mandatory
```

**원칙:**

Human approval bypass 금지

---

## 9. Runtime Safety Boundary Rule (핵심)

AI Recommendation은 execution boundary를 넘으면 안 된다.

**금지:**

- ❌ direct kubectl execution
- ❌ ArgoCD mutation
- ❌ Terraform mutation
- ❌ payment replay execution
- ❌ database mutation

**원칙:**

AI는 recommendation만 생성한다.  
Human이 시스템 외부에서 실행한다.

---

## 10. Unsafe Recommendation Rule

Guardrail은 unsafe recommendation을 격리할 수 있어야 한다.

**예:**

```
retry storm detected
+
consumer lag explosion
→ aggressive scale-out quarantine
```

---

## 11. Quarantine Rule

Guardrail은 recommendation quarantine을 지원해야 한다.

**Quarantine 대상 예:**

- payment-risking recommendation
- unknown rollback recommendation
- unverified mitigation
- high-risk replay operation

**원칙:**

Quarantine recommendation은 승인 전 operator review mandatory

---

## 12. Escalation Rule

Guardrail은 escalation을 강제할 수 있어야 한다.

**예:**

```
SEV-1
+
payment integrity risk
→ escalation mandatory
```

---

## 13. Unknown Handling Rule

Unknown은 안전하지 않은 것으로 간주된다.

**예:**

- missing traces
- partial metrics
- projection lag
- stale retrieval
- unknown rollback capability

**원칙:**

Unknown → unsafe recommendation deny

---

## 14. Degraded Runtime Rule

Runtime degradation 시 Guardrail은 더 restrictive해져야 한다.

**예:**

```
retrieval degradation      → recommendation confidence reduction
projection unavailable     → risky recommendation hard-block
```

---

## 15. FinTech Safety Rule (최상위)

Guardrail은 FinTech Safety를 최우선으로 해야 한다.

**최우선 보호 대상:**

- payment integrity
- duplicate payment prevention
- idempotency
- settlement consistency

**예:**

```
duplicate execution possibility
→ recommendation hard-block
```

---

## 16. Rollback Mandatory Rule

Rollback 없는 Recommendation은 허용되지 않는다.

**필수:**

- rollback plan
- rollback verification
- rollback boundary

**원칙:**

Rollback 없는 recommendation reject

---

## 17. Verification Mandatory Rule

Verification 불가능 Recommendation은 허용되지 않는다.

**필수:**

- metrics verification
- trace verification
- SLO verification

**원칙:**

Verification 없는 recommendation reject

---

## 18. Recommendation Replay Rule

Guardrail Runtime은 replay 가능해야 한다.

**예:**

- guardrail replay
- recommendation replay
- rollback replay
- approval replay

---

## 19. Governance Timeline Integration Rule

Guardrail은 Governance Timeline과 연결되어야 한다.

**예:**

- guardrail violation history
- approval history
- rollback history
- quarantine history

---

## 20. Evidence-aware Guardrail Rule

Guardrail은 EvidenceContext 기반으로 평가되어야 한다.

**예:**

```
retry amplification
+
error budget burn
+
tail latency explosion
→ aggressive mitigation block
```

---

## 21. Systems-Math Integration Rule

Guardrail은 Systems-Math reasoning을 사용할 수 있다.

**예:**

- queue collapse risk
- retry amplification risk
- tail latency propagation

**원칙:**

Systems-Math는 위험 설명 계층이다.

---

## 22. Experiment Integration Rule

Experiment 결과는 Guardrail strictness에 영향을 줄 수 있다.

**예:**

```
historical rollback failure
→ stricter guardrail
```

---

## 23. SLO-aware Guardrail Rule

Guardrail은 SLO 영향을 고려해야 한다.

**예:**

```
error budget exhaustion
→ risky mitigation hard-block
```

---

## 24. Explainability Rule (핵심)

Guardrail은 explainable 해야 한다.

**설명 가능 대상:**

- 왜 recommendation이 차단되었는가
- 왜 quarantine 되었는가
- 왜 escalation이 필요한가
- 왜 rollback이 mandatory인가

**원칙:**

설명 불가능한 guardrail 금지

---

## 25. Runtime DTO Rule

Guardrail Runtime은 canonical DTO를 가져야 한다.

**예:**

- `GuardrailEvaluationResult`
- `GuardrailViolation`
- `GuardrailAction`
- `QuarantineDecision`
- `EscalationDecision`

---

## 26. Runtime Metrics Governance Rule

Guardrail metrics는 low-cardinality를 유지해야 한다.

**허용:**

- `guardrail_type`
- `result`
- `severity`
- `risk_level`

**금지:**

- `incident_id`
- `event_id`
- `raw recommendation`
- `full LLM output`

---

## 27. Runtime Auditability Rule

Guardrail lifecycle은 audit 가능해야 한다.

**예:**

- who approved
- why recommendation blocked
- why escalation triggered
- why quarantine applied

---

## 28. Runtime Security Rule

Guardrail Runtime은 내부 전용이어야 한다.

**필수:**

- authenticated guardrail access
- internal-only governance mutation
- audit-protected lifecycle

**금지:**

- ❌ public guardrail mutation
- ❌ external governance bypass

---

## 29. Runtime Failure Handling Rule

Guardrail failure는 explicit 해야 한다.

**예:**

- guardrail evaluation failure
- timeline projection failure
- rollback validation failure
- verification planning failure

**원칙:**

silent guardrail failure 금지

---

## 30. Human-in-the-loop Rule

Guardrail은 Human Governance를 제거하지 않는다.

**원칙:**

AI는 recommendation만 생성한다.  
Human이 최종 승인한다.  
Human이 외부 시스템에서 실행한다.

---

## 31. Research Compatibility Rule

Guardrail Runtime은 Reliability Research를 지원해야 한다.

**예:**

- guardrail effectiveness analysis
- unsafe recommendation prevention rate
- operator trust analysis
- runtime governance explainability

---

## 32. Operational Research Direction Rule

장기적으로 Guardrail Runtime은 다음 방향으로 발전할 수 있다.

**예:**

- adaptive guardrail strictness
- context-aware runtime governance
- dynamic risk isolation

---

## 33. Anti-Pattern Rule

**금지:**

- ❌ guardrail-free recommendation
- ❌ rollback 없는 recommendation
- ❌ unverifiable mitigation
- ❌ unsafe autonomous remediation
- ❌ opaque runtime governance
- ❌ payment-risking recommendation
- ❌ human approval bypass

---

## 34. Non-Goals

Guardrail Runtime의 목표는 다음이 아니다.

- AGI operations replacement
- autonomous production remediation
- human-free governance
- unsafe self-healing

---

## 35. 핵심 원칙

| 계층 | 역할 |
|---|---|
| Evidence | 운영 증거 |
| Policy | 운영 규칙 |
| Guardrail | runtime 안전 차단 |
| Rollback | 안전 복구 |
| Verification | 결과 검증 |
| Timeline | audit / replay |
| Human | 최종 책임 |

---

## 36. Reliability Research Integration

현재 방향의 핵심은 단순 자동화가 아니다.

**목표:**

운영 안정성을 다음과 같은 형태로 formalization 하는 것이다.

- 실험 가능
- 관측 가능
- 정량 분석 가능
- 설명 가능

---

> 🎯 **한 줄 핵심**
>
> Guardrail의 목적은 Recommendation을 생성하는 것이 아니다.  
> → 위험한 운영 판단이 실행 경계를 넘지 못하도록 runtime 안전 경계를 강제하는 것이다.