# Experiment Runtime Contract

## 1. 목적

이 문서는 FIN-SRE 플랫폼의 **Experiment Runtime Layer**를 정의한다.

Experiment Runtime의 목적은 단순 chaos engineering 실행이 아니다.

목적은:

> 운영 정책 + 장애 시나리오 + Rollback + Verification + Quantitative Validation

을 기반으로:

- 재현 가능하고
- 검증 가능하며
- 연구 가능한

**Operational Reliability Experiment Runtime**을 formalization 하는 것이다.

---

## 2. 핵심 개념

Experiment는 단순 테스트가 아니다.

Experiment는:

> **운영 가설 검증(runtime hypothesis validation)**

이다.

**예:**
- Human Approval이 false-positive operational action을 감소시키는가?
- Guardrail이 retry amplification propagation을 감소시키는가?

---

## 3. Canonical Experiment Definition

Experiment는 다음을 포함 가능.

| 요소 | 설명 |
|---|---|
| Failure Injection | 장애 주입 |
| Policy Comparison | 정책 비교 |
| Recommendation Evaluation | 추천 평가 |
| Rollback Validation | 복구 검증 |
| Verification Validation | 검증 평가 |
| Propagation Analysis | 장애 전파 분석 |
| Quantitative Validation | 정량 검증 |

---

## 4. Human-in-the-loop Rule

Experiment Runtime은 Human Governance 제거 금지.

**원칙:**

> Experiment는 human-approved only

**금지:**
- ❌ autonomous production chaos
- ❌ unapproved failure injection
- ❌ uncontrolled blast radius

---

## 5. Canonical Experiment Lifecycle

Experiment Runtime은 canonical lifecycle 가져야 한다.

```
DRAFT → APPROVAL_PENDING → APPROVED → RUNNING → VERIFYING → COMPLETED
```

또는:

```
RUNNING → FAILED → ROLLBACK_REQUIRED
```

---

## 6. Experiment Scope Rule

Experiment는 blast radius awareness 가져야 한다.

**구분:**
- sandbox
- staging
- isolated production
- partial production

**원칙:** Production-wide uncontrolled experiment 금지

---

## 7. Failure Injection Rule

Failure injection은 bounded 해야 한다.

**허용:**
- latency injection
- packet loss
- consumer lag
- dependency timeout
- retry amplification simulation

**금지:**
- payment corruption
- unrecoverable DB mutation
- destructive irreversible mutation

---

## 8. Rollback Mandatory Rule

모든 Experiment는 rollback capability 가져야 한다.

**필수:**
- rollback plan
- rollback trigger
- rollback verification
- rollback timeout

**원칙:**

```
No Rollback → No Experiment
```

---

## 9. Verification Mandatory Rule

모든 Experiment는 verification requirement 가져야 한다.

**포함:**
- recovery verification
- queue stabilization
- latency normalization
- payment consistency verification

---

## 10. FinTech Safety Rule

FinTech 환경에서는 다음 금지.

**금지:**
- duplicate payment experiment
- settlement corruption
- payment replay uncertainty

**허용 가능:**
- isolated replay simulation
- synthetic payment flow
- sanitized experiment environment

---

## 11. Policy Comparison Rule

Experiment Runtime은 policy comparison 지원 가능해야 한다.

| 그룹 | 정책 |
|---|---|
| A | Guardrail OFF |
| B | Guardrail ON |

**비교:**
- MTTR
- rollback success
- propagation reduction
- false positive rate

---

## 12. Human Approval Comparison Rule

Experiment Runtime은 approval governance 비교 가능해야 한다.

| 그룹 | Approval |
|---|---|
| A | Human Approval OFF |
| B | Human Approval ON |

---

## 13. Recommendation Evaluation Rule

Experiment는 recommendation effectiveness 평가 가능해야 한다.

**예:**
- recommendation accuracy
- unsafe recommendation reduction
- rollback effectiveness

---

## 14. Propagation Analysis Rule

Experiment는 propagation analysis 지원 가능해야 한다.

```
retry storm → queue saturation → DB overload → timeout propagation
```

---

## 15. Systems-Math Integration Rule

Experiment Runtime은 Systems-Math 기반이어야 한다.

**예:**
- Little's Law
- Queue Utilization
- Tail Latency Propagation
- Retry Amplification

**원칙:** Systems-Math는 실험 결과 설명 계층이다.

---

## 16. Quantitative Validation Rule

Experiment는 정량 검증 가능해야 한다.

**예:**
- MTTR
- rollback success rate
- error recovery latency
- queue recovery latency
- approval delay impact

---

## 17. Evidence-backed Rule

Experiment 결과는 Evidence 기반이어야 한다.

**허용:**
- metrics
- traces
- logs
- timeline
- verification result

**금지:**
- hallucinated result
- fabricated metric
- invented validation

---

## 18. Reproducibility Rule

Experiment는 재현 가능해야 한다.

**포함:**
- environment
- failure injection method
- policy configuration
- traffic pattern
- rollback condition
- verification condition

---

## 19. Experiment Isolation Rule

Experiment는 isolation boundary 가져야 한다.

**예:**
- sandbox namespace
- isolated queue
- synthetic workload
- shadow traffic

---

## 20. Runtime DTO Rule

Experiment Runtime은 canonical DTO 가져야 한다.

**예:**
- `ExperimentDefinition`
- `ExperimentRun`
- `ExperimentEvidence`
- `ExperimentValidation`
- `ExperimentRollback`
- `ExperimentResult`

---

## 21. Timeline Rule

Experiment lifecycle은 timeline replay 가능해야 한다.

**예:**
- experiment start
- failure injection
- recommendation generation
- rollback execution
- verification result

---

## 22. Runtime Replay Rule

Experiment Runtime은 replayable 해야 한다.

**예:**
- experiment replay
- policy replay
- verification replay
- rollback replay

---

## 23. Unknown State Rule

Unknown 상태는 restrictive governance 적용.

**예:**
- missing metrics
- partial observability
- projection inconsistency
- timeline corruption

**원칙:**

```
Unknown → experiment invalidation
```

---

## 24. Blast Radius Escalation Rule

Blast radius 증가 시 stricter governance 요구.

```
local experiment          → operator approval
cross-service experiment  → senior approval
production experiment     → escalation mandatory
```

---

## 25. GitOps Governance Rule

Experiment Runtime은 GitOps-aware 해야 한다.

**허용:**
- Git-based rollback
- ArgoCD sync recovery
- desired-state restoration

**금지:**
- manual runtime drift
- untracked mutation

---

## 26. Experiment Auditability Rule

모든 Experiment lifecycle은 audit 가능해야 한다.

**포함:**
- who approved
- what injected
- what policy used
- what rollback executed
- what verified

---

## 27. Immutable Audit Rule

Experiment audit는 append-only 해야 한다.

**금지:**
- ❌ audit overwrite
- ❌ hidden experiment mutation
- ❌ evidence deletion

---

## 28. Research Asset Integration Rule

Experiment Runtime은 Research Asset 생성 가능해야 한다.

**예:**
- experiment report
- quantitative validation report
- research note
- paper candidate

---

## 29. Reliability Dataset Rule

Experiment Runtime은 dataset accumulation 지원 가능해야 한다.

**예:**
- rollback dataset
- verification dataset
- policy effectiveness dataset
- incident propagation dataset

---

## 30. Recommendation Safety Rule

Experiment는 unsafe recommendation 탐지 가능해야 한다.

**예:**
- aggressive scale-out
- unsafe replay
- unverified mitigation

---

## 31. Verification Failure Rule

Verification failure는 canonical outcome이어야 한다.

**예:**
- partial recovery
- retry persistence
- queue instability

**원칙:**

```
verification failure → experiment failed
```

---

## 32. Runtime Security Rule

Experiment Runtime은 privileged operational layer다.

**필수:**
- authenticated access
- RBAC
- internal-only routing
- audit logging

**금지:**
- ❌ anonymous experiment execution
- ❌ public failure injection
- ❌ unrestricted production experiment

---

## 33. Visibility Classification Rule

Experiment artifact는 visibility classification 가져야 한다.

**허용:**
- `PUBLIC_PORTFOLIO`
- `PRIVATE_RESEARCH`
- `INTERNAL_OPERATION`
- `PAPER_CANDIDATE`
- `SANITIZED_EXPORT`

---

## 34. Sanitization Rule

Experiment export는 sanitization 가능해야 한다.

**제거 대상:**
- internal topology
- raw customer payload
- secret
- token
- internal IP

---

## 35. Research Compatibility Rule

Experiment Runtime은 Reliability Research 지원 가능해야 한다.

**예:**
- Human Approval effectiveness
- Guardrail effectiveness
- rollback effectiveness
- policy comparison

---

## 36. Academic Extension Rule

장기적으로 다음 연구 방향 지원 가능.

**예:**
- adaptive reliability governance
- policy-aware rollback systems
- experiment-driven operational safety

---

## 37. Runtime Failure Rule

Experiment Runtime failure는 explicit 해야 한다.

**예:**
- experiment corruption
- rollback unavailable
- verification unavailable
- timeline inconsistency

**금지:** silent experiment degradation

---

## 38. Anti-Pattern Rule

**금지:**
- ❌ rollback 없는 experiment
- ❌ verification 없는 experiment
- ❌ payment-risking experiment
- ❌ opaque chaos execution
- ❌ fabricated experiment evidence
- ❌ uncontrolled blast radius

---

## 39. Non-Goals

Experiment Runtime의 목표는 다음이 아니다.

- autonomous chaos platform
- uncontrolled production mutation
- synthetic-only reliability claim
- opaque benchmark generation

---

## 40. Canonical Runtime Layers

| Layer | 역할 |
|---|---|
| Failure Injection | 장애 주입 |
| Recommendation | AI 대응 |
| Approval | Human Governance |
| Rollback | 안전 복구 |
| Verification | 결과 검증 |
| Validation | 정량 분석 |
| Research | 연구 자산 |
| Dataset | reliability dataset |

---

## 41. Reliability Research Runtime Direction

현재 방향의 핵심은 단순 chaos engineering이 아니다.

**목표:**

> 운영 정책과 장애 대응을 재현 가능하고, 검증 가능하며, 정량 분석 가능하고, 논문화 가능한 **Operational Reliability Experiment Runtime**으로 formalization 하는 것이다.

---

## 한 줄 핵심

Experiment Runtime의 목적은 단순 장애 주입이 아니다.

> 운영 정책, rollback, verification, propagation을 **재현 가능하고 정량 검증 가능한 Reliability Experiment Runtime**으로 formalization 하는 것이다.