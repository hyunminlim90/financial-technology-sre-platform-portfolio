# protocols/recommendation-context-contract

## 1. 목적

이 문서는 AI-SRE 플랫폼의 Agent Runtime이 생성하는 RecommendationContext의 canonical runtime structure, governance lifecycle, rollback/verification requirement, explainability contract를 정의한다.

> **RecommendationContext의 목적은 단순 AI 응답 생성이 아니다.**  
> 목적은 "운영자가 신뢰 가능하고 replay 가능한 운영 판단(Runtime Recommendation Object)"을 생성하는 것 이다.

---

## 2. 핵심 개념

RecommendationContext는 다음 Runtime 요소들을 결합한 canonical recommendation object다.

```
Evidence
+
Knowledge Retrieval
+
Constraint Evaluation
+
Policy Evaluation
+
Guardrail Evaluation
+
Rollback Planning
+
Verification Planning

↓

RecommendationContext
```

---

## 3. Recommendation-only Rule

RecommendationContext는 recommendation만 표현할 수 있다.

**허용:**

- ✔ operational recommendation
- ✔ mitigation proposal
- ✔ rollback proposal
- ✔ verification proposal
- ✔ risk explanation
- ✔ evidence interpretation

**금지:**

- ❌ autonomous execution
- ❌ infrastructure mutation
- ❌ kubectl execution
- ❌ Terraform apply
- ❌ payment mutation
- ❌ uncontrolled remediation

**원칙:**

`Recommendation ≠ Execution`

---

## 4. Canonical Recommendation Structure Rule

RecommendationContext는 canonical runtime structure를 가져야 한다.

**포함 가능:**

- `RecommendationCandidate`
- `EvidenceContext`
- `RiskClassification`
- `RollbackPlan`
- `VerificationPlan`
- `PolicyEvaluation`
- `GuardrailEvaluation`
- `ApprovalRequirement`
- `ConfidenceClassification`

---

## 5. Recommendation Candidate Rule

Runtime은 여러 candidate recommendation을 생성할 수 있다.

**예:**

- scale-out candidate
- retry reduction candidate
- fallback enable candidate
- traffic shift candidate

**원칙:**

candidate는 evaluation 이전 상태다.

---

## 6. Constraint Merge Rule

Recommendation은 constraint merge를 수행해야 한다.

**입력:**

```
Runbook
+
Improvement
+
Preventive Design
+
Policy
+
Guardrail
```

**출력:**

- `Governed Recommendation`

---

## 7. Recommendation Priority Rule

Recommendation은 governance priority를 따라야 한다.

**우선순위:**

```
Preventive Design
→ Improvement
→ Runbook
→ Scenario
→ rag/docs
```

**원칙:**

더 안전한 recommendation이 우선된다.

---

## 8. Risk Classification Rule

모든 Recommendation은 risk classification을 포함해야 한다.

| Risk | 의미 |
|---|---|
| `LOW` | read-only |
| `MEDIUM` | reversible |
| `HIGH` | production impact |
| `CRITICAL` | payment integrity risk |

**원칙:**

`HIGH` 이상 recommendation은 Human approval mandatory

---

## 9. Confidence Classification Rule

Recommendation은 confidence를 포함해야 한다.

| Confidence | 의미 |
|---|---|
| `HIGH` | 충분한 evidence |
| `MEDIUM` | 일부 uncertainty |
| `LOW` | 제한된 observability |
| `DEGRADED` | observability degradation |
| `UNKNOWN` | evidence unavailable |

**원칙:**

`LOW` confidence 상태에서는 고위험 recommendation 금지

---

## 10. Rollback Mandatory Rule

모든 Recommendation은 rollback plan을 포함해야 한다.

**필수:**

- rollback steps
- rollback trigger
- rollback verification
- rollback safety boundary

**원칙:**

Rollback 없는 recommendation 금지

---

## 11. Verification Mandatory Rule

모든 Recommendation은 verification plan을 포함해야 한다.

**예:**

- latency normalization
- error rate recovery
- queue stabilization
- trace recovery
- SLO normalization

**원칙:**

Verification 없는 recommendation 금지

---

## 12. Human Approval Rule

Recommendation은 Human Approval requirement를 포함해야 한다.

**포함 가능:**

- approval required
- approval scope
- approval reason
- risk explanation

**원칙:**

Human approval bypass 금지

---

## 13. Recommendation Lifecycle Rule

Recommendation은 lifecycle을 가진다.

**상태 예시:**

- `GENERATED`
- `REVIEW_PENDING`
- `APPROVED`
- `REJECTED`
- `EXECUTED_EXTERNALLY`
- `VERIFIED`
- `ROLLED_BACK`
- `SUPERSEDED`
- `CLOSED`

---

## 14. Recommendation Replay Rule

Recommendation은 replay 가능해야 한다.

**예:**

- recommendation replay
- rollback replay
- verification replay
- approval replay

**원칙:**

운영 판단은 reconstructable 해야 한다.

---

## 15. Governance Timeline Integration Rule

Recommendation은 Governance Timeline과 연결될 수 있어야 한다.

**예:**

- approval history
- rollback history
- verification history
- recommendation supersede history

---

## 16. Evidence Integration Rule

Recommendation은 EvidenceContext와 연결되어야 한다.

**예:**

- metrics
- logs
- traces
- alerts
- deployment events
- SLO

**원칙:**

Evidence 없는 recommendation 금지

---

## 17. Systems-Math Integration Rule

Recommendation은 Systems-Math reasoning을 사용할 수 있다.

**예:**

- queue utilization reasoning
- retry amplification reasoning
- tail latency propagation

**원칙:**

Systems-Math는 설명 계층이다.

---

## 18. Experiment Validation Rule

Recommendation은 Experiment evidence를 사용할 수 있다.

**예:**

```
과거 fallback experiment
→ confidence correction
```

---

## 19. SLO-aware Recommendation Rule

Recommendation은 SLO 영향을 고려해야 한다.

**예:**

- availability degradation
- error budget burn
- latency SLO violation

---

## 20. Explainability Rule (핵심)

Recommendation은 explainable 해야 한다.

**설명 가능 대상:**

- 왜 recommendation이 생성되었는가
- 왜 특정 action이 차단되었는가
- 왜 rollback이 필요한가
- 왜 preventive design이 우선되었는가

**원칙:**

설명 불가능한 recommendation 금지

---

## 21. Unknown Handling Rule

Recommendation은 Unknown을 추정으로 대체하면 안 된다.

**예:**

- missing traces
- partial metrics
- stale retrieval
- degraded observability

**원칙:**

Unknown은 Unknown으로 유지한다.

---

## 22. Degraded Recommendation Rule

Runtime은 degraded recommendation mode를 지원할 수 있다.

**예:**

- partial observability
- embedding unavailable
- projection lag
- retrieval degradation

**출력:**

- `low confidence recommendation`
- `degraded recommendation`

---

## 23. Runtime DTO Rule

Recommendation은 canonical runtime DTO로 표현될 수 있어야 한다.

**예:**

- `RecommendationContext`
- `RecommendationCandidate`
- `RollbackPlan`
- `VerificationPlan`
- `RiskClassification`
- `ConfidenceClassification`

---

## 24. Runtime Metrics Governance Rule

Recommendation Runtime metrics는 low-cardinality를 유지해야 한다.

**허용:**

- `status`
- `risk_level`
- `confidence`
- `result`

**금지:**

- `incident_id`
- `event_id`
- `raw recommendation`
- `full prompt`

---

## 25. FinTech Safety Rule

Recommendation은 FinTech Safety를 최우선으로 해야 한다.

**최우선 보호 대상:**

- payment integrity
- idempotency
- duplicate payment prevention
- settlement consistency

**예:**

```
retry amplification detected
→ aggressive retry recommendation 금지
```

---

## 26. Human-in-the-loop Rule

RecommendationContext는 Human Governance를 제거하지 않는다.

**원칙:**

AI는 recommendation만 생성한다.  
Human이 최종 판단한다.

---

## 27. Runtime Auditability Rule

Recommendation lifecycle은 audit 가능해야 한다.

**예:**

- who approved
- which policy blocked
- why recommendation rejected
- why rollback triggered

---

## 28. Runtime Security Rule

Recommendation Runtime은 내부 전용이어야 한다.

**필수:**

- authenticated recommendation access
- internal-only recommendation mutation
- audit-protected lifecycle

**금지:**

- ❌ public recommendation mutation
- ❌ external policy bypass

---

## 29. Runtime Failure Handling Rule

Recommendation Runtime failure는 explicit 해야 한다.

**예:**

- policy evaluation failure
- guardrail evaluation failure
- rollback generation failure
- verification planning failure

**원칙:**

silent recommendation failure 금지

---

## 30. Research Compatibility Rule

RecommendationContext는 Reliability Research를 지원해야 한다.

**예:**

- recommendation effectiveness analysis
- rollback effectiveness
- operator trust evaluation
- reasoning explainability

---

## 31. Future Runtime Rule

현재 Recommendation Runtime은 recommendation 중심이다.

**장기적으로:**

```
Operational Decision Runtime
```

으로 발전할 수 있다.

**예:**

- adaptive recommendation
- dynamic policy weighting
- context-aware mitigation planning

---

## 32. Anti-Pattern Rule

**금지:**

- ❌ rollback 없는 recommendation
- ❌ unverifiable recommendation
- ❌ opaque recommendation
- ❌ evidence-free recommendation
- ❌ human approval bypass
- ❌ unsafe autonomous remediation
- ❌ rag/docs-only recommendation

---

## 33. Non-Goals

RecommendationContext의 목표는 다음이 아니다.

- AGI operations replacement
- autonomous production control
- human-free remediation
- unsafe self-healing

---

## 34. 핵심 원칙

| 계층 | 역할 |
|---|---|
| Evidence | 운영 증거 |
| Retrieval | 운영 지식 검색 |
| Constraint | 안전 제한 |
| Policy | 운영 규칙 |
| Guardrail | 위험 차단 |
| Recommendation | 운영 판단 |
| Rollback | 안전 복구 |
| Verification | 결과 검증 |
| Human | 최종 책임 |

---

> 🎯 **한 줄 핵심**
>
> Recommendation의 목적은 자동 실행이 아니다.  
> → 운영자가 신뢰 가능하고 replay 가능한 운영 판단 객체를 생성하는 것이다.