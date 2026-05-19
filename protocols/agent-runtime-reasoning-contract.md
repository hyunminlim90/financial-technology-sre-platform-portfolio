# protocols/agent-runtime-reasoning-contract

## 1. 목적

이 문서는 AI-SRE 플랫폼의 Agent Runtime이 운영 Evidence, Knowledge Retrieval, Governance Constraint, Human Approval 조건을 기반으로 Recommendation을 생성하는 reasoning lifecycle과 runtime governance 규칙을 정의한다.

> **Agent Runtime의 목적은 자동 실행이 아니다.**  
> 목적은 "설명 가능하고 검증 가능한 운영 판단(reasoning)을 생성하는 것" 이다.

---

## 2. 핵심 개념

Agent Runtime은 다음 흐름으로 동작한다.

```
Evidence Ingestion
        ↓
Knowledge Retrieval
        ↓
Runtime Context Assembly
        ↓
Constraint Filtering
        ↓
Recommendation Candidate Generation
        ↓
Policy Evaluation
        ↓
Guardrail Evaluation
        ↓
Risk Classification
        ↓
Rollback Validation
        ↓
Verification Planning
        ↓
Human-reviewable Recommendation
```

---

## 3. Recommendation-only Rule

Agent Runtime은 Recommendation만 생성할 수 있다.

**허용:**

- ✔ analysis
- ✔ recommendation
- ✔ rollback suggestion
- ✔ verification suggestion
- ✔ risk explanation
- ✔ evidence interpretation

**금지:**

- ❌ kubectl execution
- ❌ infrastructure mutation
- ❌ ArgoCD modification
- ❌ Terraform apply
- ❌ DB mutation
- ❌ payment mutation
- ❌ autonomous remediation

**원칙:**

`AI Recommendation ≠ Execution`

---

## 4. Evidence-first Rule (핵심)

Reasoning은 반드시 Evidence 기반이어야 한다.

**포함 대상:**

- metrics
- logs
- traces
- alerts
- SLO
- deployment events
- governance timeline

**원칙:**

Evidence 없는 Recommendation 금지

---

## 5. Runtime Evidence Ingestion Rule

Evidence는 Runtime Context로 수집될 수 있어야 한다.

**입력:**

- `Prometheus`
- `Loki`
- `Jaeger`
- `Alertmanager`
- `Kubernetes Events`
- `Governance Timeline`

**출력:**

- `EvidenceContext`

---

## 6. Retrieval-aware Reasoning Rule

Reasoning은 retrieval ordering을 따라야 한다.

**우선순위:**

```
protocol
→ scenario
→ runbook
→ improvement
→ preventive-design
→ postmortem
→ experiment
→ systems-math
→ rag/docs
```

**원칙:**

`rag/docs`는 보조 reasoning 계층이다.

---

## 7. Constraint-first Rule

Recommendation 생성 전 constraint evaluation이 먼저 수행되어야 한다.

**포함 대상:**

- Improvement
- Preventive Design
- Policy
- Guardrail

**원칙:**

위험 Action filtering이 먼저 수행된다.

---

## 8. Recommendation Candidate Rule

Runtime은 여러 candidate recommendation을 생성할 수 있다.

**예:**

- candidate action
- candidate rollback
- candidate verification
- candidate mitigation

**원칙:**

candidate는 evaluation 이전 상태이다.

---

## 9. Policy Evaluation Rule

Recommendation candidate는 policy evaluation을 통과해야 한다.

**예:**

- `PaymentSafetyPolicy`
- `RollbackPolicy`
- `VerificationPolicy`
- `ApprovalPolicy`
- `DuplicateExecutionPolicy`

**원칙:**

Policy violation Recommendation은 reject된다.

---

## 10. Guardrail Rule

Guardrail은 runtime recommendation을 제한한다.

**예:**

- high-risk action block
- unsafe retry prevention
- duplicate execution prevention
- rollback mandatory enforcement

---

## 11. Risk Classification Rule

모든 Recommendation은 risk classification을 포함해야 한다.

| Risk | 의미 |
|---|---|
| `LOW` | read-only |
| `MEDIUM` | reversible |
| `HIGH` | production impact |
| `CRITICAL` | payment integrity risk |

---

## 12. Confidence Classification Rule

Recommendation은 confidence를 포함해야 한다.

| Confidence | 의미 |
|---|---|
| `HIGH` | 충분한 evidence |
| `MEDIUM` | 일부 uncertainty |
| `LOW` | 제한된 evidence |
| `DEGRADED` | observability 부족 |

**원칙:**

`LOW` confidence 상태에서는 고위험 Action 금지

---

## 13. Rollback Mandatory Rule

모든 Recommendation은 rollback plan을 포함해야 한다.

**필수:**

- rollback steps
- rollback trigger
- rollback verification

**원칙:**

Rollback 없는 Recommendation 금지

---

## 14. Verification Mandatory Rule

모든 Recommendation은 verification plan을 포함해야 한다.

**예:**

- latency recovery
- error rate normalization
- queue stabilization
- trace recovery

**원칙:**

Verification 없는 Recommendation 금지

---

## 15. Systems-Math Integration Rule

Reasoning은 Systems-Math를 사용할 수 있다.

**예:**

- queue utilization
- Little's Law
- retry amplification
- tail latency propagation

**원칙:**

Systems-Math는 설명 계층이지 Action 결정 계층이 아니다.

---

## 16. Experiment Validation Rule

Experiment 결과는 Recommendation validation evidence로 사용될 수 있다.

**예:**

```
retry limit 적용 후
recovery time 감소
```

**원칙:**

실험 결과는 confidence correction에 사용될 수 있다.

---

## 17. Governance Timeline Integration Rule

Reasoning은 Governance Timeline을 사용할 수 있다.

**예:**

- historical rollback
- approval history
- recommendation replay
- verification history

---

## 18. Recommendation Context Rule

Runtime은 RecommendationContext를 생성할 수 있다.

**포함 대상:**

- retrieved knowledge
- evidence
- risk classification
- rollback plan
- verification plan

---

## 19. Explainability Rule (핵심)

Runtime은 explainable reasoning을 지원해야 한다.

**설명 가능 대상:**

- 왜 recommendation이 생성되었는가
- 왜 특정 action이 차단되었는가
- 왜 rollback이 필요한가
- 왜 preventive design이 우선되었는가

**원칙:**

설명 불가능한 reasoning 금지

---

## 20. Unknown Handling Rule

Runtime은 Unknown을 추정으로 대체해서는 안 된다.

**예:**

- missing metrics
- partial traces
- stale retrieval
- projection lag

**원칙:**

Unknown은 Unknown으로 유지한다.

---

## 21. Degraded Reasoning Rule

Runtime은 degraded reasoning mode를 지원할 수 있다.

**예:**

- partial observability
- retrieval degradation
- projection unavailable
- embedding unavailable

**출력:**

- `degraded recommendation`
- `low confidence recommendation`

---

## 22. SLO-aware Reasoning Rule

Runtime은 SLO 영향을 고려해야 한다.

**예:**

- availability
- latency
- error budget burn
- recovery time

---

## 23. FinTech Safety Rule

모든 reasoning은 FinTech Safety를 우선한다.

**최우선 보호 대상:**

- payment integrity
- idempotency
- duplicate payment prevention
- settlement consistency

**원칙:**

공격적 자동화보다 결제 안전성이 우선된다.

---

## 24. Human-in-the-loop Rule

Runtime은 Human Governance를 제거하지 않는다.

**원칙:**

Human approval mandatory

---

## 25. Recommendation Replay Rule

Reasoning은 replay 가능해야 한다.

**예:**

- recommendation replay
- incident replay
- rollback replay
- verification replay

---

## 26. Runtime Replayability Rule

Runtime reasoning은 reconstructable 해야 한다.

**포함 대상:**

- retrieved context
- evidence
- constraint evaluation
- policy evaluation
- final recommendation

---

## 27. Runtime Auditability Rule

Reasoning lifecycle은 audit 가능해야 한다.

**예:**

- who approved
- which policy blocked
- why rollback triggered
- why recommendation rejected

---

## 28. Runtime Security Rule

Runtime은 내부 전용 reasoning system이어야 한다.

**필수:**

- authenticated reasoning
- internal-only runtime
- audit-protected context

**금지:**

- ❌ public recommendation mutation
- ❌ external policy bypass

---

## 29. Runtime Failure Handling Rule

Runtime failure는 explicit 해야 한다.

**예:**

- retrieval failure
- policy evaluation failure
- graph projection failure
- observability failure

**원칙:**

silent reasoning failure 금지

---

## 30. Runtime Metrics Rule

Runtime metrics는 low-cardinality를 유지해야 한다.

**허용:**

- `result`
- `status`
- `risk_level`
- `confidence`

**금지:**

- `incident_id`
- `event_id`
- `raw query`
- `full prompt`

---

## 31. Runtime Research Compatibility Rule

Reasoning runtime은 Reliability Research를 지원할 수 있어야 한다.

**예:**

- recommendation quality evaluation
- rollback effectiveness
- reasoning explainability
- tail latency reasoning

---

## 32. Future Runtime Rule

현재 Runtime은 recommendation 중심이다.

**장기적으로:**

```
Operational Reasoning Runtime
```

으로 발전할 수 있다.

**예:**

- adaptive retrieval
- dynamic evidence merge
- context-aware recommendation

---

## 33. Anti-Pattern Rule

**금지:**

- ❌ autonomous remediation
- ❌ rollback 없는 recommendation
- ❌ unverifiable recommendation
- ❌ opaque reasoning
- ❌ evidence 없는 action
- ❌ human approval bypass
- ❌ uncontrolled AI mutation

---

## 34. Non-Goals

Runtime의 목표는 다음이 아니다.

- AGI operator replacement
- autonomous infrastructure control
- human-free operation
- unsafe self-healing

---

## 35. 핵심 원칙

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
> Agent Runtime의 목적은 자동 실행이 아니다.  
> → 설명 가능하고 검증 가능한 운영 판단(reasoning)을 생성하는 것이다.