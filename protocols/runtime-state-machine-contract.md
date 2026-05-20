# Runtime State Machine Contract

## 1. 목적

이 문서는 FIN-SRE 플랫폼의 **Runtime State Machine Layer**를 정의한다.

State Machine Runtime의 목적은 단순 상태 관리가 아니다.

목적은:

```
Incident
+ Evidence
+ Recommendation
+ Rollback
+ Verification
+ Reliability Runtime
```

을 기반으로:

> 설명 가능하고  
> 재현 가능하며  
> 감사 가능하고  
> 정량 검증 가능하며  
> 논문화 가능한  
> **Operational Reliability State Machine Runtime**

을 formalization 하는 것이다.

---

## 2. 핵심 개념

State Machine Runtime은 단순 enum transition이 아니다.

Runtime State Machine은:

- Evidence-aware
- Policy-aware
- Rollback-aware
- Verification-aware
- Convergence-aware
- Human-governed

operational reliability lifecycle runtime이다.

---

## 3. Canonical Runtime Definition

State Machine Runtime은 다음을 포함 가능.

| Runtime | 역할 |
|---|---|
| Incident Runtime | incident lifecycle |
| Evidence Runtime | observability evidence |
| Recommendation Runtime | 대응 orchestration |
| Rollback Runtime | 안전 복구 |
| Verification Runtime | 결과 검증 |
| Reliability Runtime | stabilization/convergence |

---

## 4. Human Governance Rule

State Machine Runtime은 **Human Governance 제거 금지**.

**원칙:**
- AI는 state transition recommendation을 생성할 수 있다.
- Human이 operational transition을 승인한다.

**금지:**
- ❌ autonomous production transition
- ❌ AI-only operational escalation
- ❌ unreviewed runtime mutation

---

## 5. Canonical State Lifecycle

Runtime은 canonical lifecycle 가져야 한다.

```
DETECTED
→ TRIAGING
→ ANALYZING
→ MITIGATING
→ STABILIZING
→ VERIFIED
→ RESOLVED
```

또는:

```
STABILIZING
→ REGRESSED
→ ESCALATED
```

---

## 6. Explicit Transition Rule

모든 상태 전이는 **explicit** 해야 한다.

```
ANALYZING → MITIGATING
```

**금지:** implicit hidden transition

---

## 7. Transition Evidence Rule

모든 transition은 evidence-backed 해야 한다.

**허용:** metrics · logs · traces · timeline · verification result · rollback result

**금지:**
- ❌ hallucinated state transition
- ❌ fabricated reliability state
- ❌ unsupported operational claim

---

## 8. Transition Reason Rule

모든 transition은 reason 포함해야 한다.

예: `queue stabilized` · `latency recovered` · `verification passed` · `rollback completed`

---

## 9. Reliability State Rule

Runtime은 reliability-aware state 가져야 한다.

```
HEALTHY
DEGRADED
UNSTABLE
STABILIZING
RECOVERED
FAILED
```

---

## 10. Incident State Rule

Incident Runtime은 incident lifecycle 가져야 한다.

```
OPEN
ACKNOWLEDGED
INVESTIGATING
MITIGATING
MONITORING
RESOLVED
POSTMORTEM_PENDING
CLOSED
```

---

## 11. Recommendation State Rule

Recommendation Runtime은 recommendation lifecycle 가져야 한다.

```
GENERATED
REVIEW_PENDING
APPROVED
REJECTED
EXECUTION_PENDING
VERIFICATION_PENDING
COMPLETED
ROLLED_BACK
```

---

## 12. Rollback State Rule

Rollback Runtime은 rollback lifecycle 가져야 한다.

```
ROLLBACK_REQUIRED
ROLLBACK_PENDING
ROLLBACK_RUNNING
ROLLBACK_VERIFIED
ROLLBACK_FAILED
```

---

## 13. Verification State Rule

Verification Runtime은 verification lifecycle 가져야 한다.

```
VERIFICATION_PENDING
VERIFYING
VERIFIED
FAILED
UNKNOWN
```

---

## 14. Stabilization State Rule

Runtime은 stabilization-aware 해야 한다.

```
UNSTABLE
STABILIZING
STABLE
REGRESSED
```

---

## 15. Regression Rule

State Machine Runtime은 regression detection 가능해야 한다.

```
latency recovered
→ retry amplification 재발
→ REGRESSED
```

**원칙:** `recovery != stabilization`

---

## 16. Convergence Rule

State Machine Runtime은 convergence-aware 해야 한다.

**목표:** `safe stabilization`

**금지:** oscillation · recovery thrashing · unstable mitigation loop

---

## 17. Propagation-aware Rule

State Machine Runtime은 propagation-aware 해야 한다.

예: dependency cascade · tail latency propagation · queue backlog propagation

---

## 18. Retry Amplification Rule

State Machine Runtime은 retry amplification 이해 가능해야 한다.

```
retry storm
→ queue overload
→ DB saturation
→ propagation expansion
```

---

## 19. Blast Radius Rule

State Machine Runtime은 blast radius awareness 가져야 한다.

범위 단계: `local` → `partial` → `cross-service` → `global`

**원칙:** blast radius 증가 → stricter transition governance

---

## 20. Severity-aware Rule

State Machine Runtime은 severity-aware 해야 한다.

레벨: `SEV-1` · `SEV-2` · `SEV-3`

**원칙:** higher severity → stricter transition governance

---

## 21. Context-awareness Rule

State Machine Runtime은 context-aware 해야 한다.

포함: service · environment · traffic pattern · impact scope

---

## 22. Environment-aware Rule

State Machine Runtime은 environment-aware 해야 한다.

환경: `production` · `staging` · `sandbox`

**원칙:** production → strictest transition governance

---

## 23. FinTech Safety Rule

FinTech 환경에서는 **payment consistency가 최우선**이다.

**금지:** unsafe replay · duplicate payment risk · settlement corruption

**허용 가능:** verified fallback · verified rollback · idempotent-safe mitigation

---

## 24. Rollback-aware Rule

State Machine Runtime은 rollback-aware 해야 한다.

**필수:**
- rollback trigger
- rollback timeout
- rollback verification
- rollback blast radius

**원칙:** No Rollback → risky transition blocked

---

## 25. Verification-aware Rule

State Machine Runtime은 verification-aware 해야 한다.

포함: queue stabilization validation · latency recovery validation · payment consistency validation

---

## 26. Policy-aware Rule

State Machine Runtime은 policy-aware 해야 한다.

정책 예시: approval policy · rollback policy · verification policy · blast radius policy

---

## 27. Guardrail Rule

State Machine Runtime은 Guardrail Runtime 통합해야 한다.

예: retry amplification guardrail · payment safety guardrail · rollback requirement guardrail

---

## 28. Unknown State Rule

Unknown 상태는 **restrictive governance** 적용.

해당 상황: missing metrics · partial observability · projection inconsistency · verification unavailable

**원칙:** Unknown → risky transition blocked

---

## 29. Transition Consistency Rule

상태 전이는 consistency 보장해야 한다.

**금지:**
```
VERIFIED → rollback pending  (논리 모순 전이)
```

---

## 30. Runtime Synchronization Rule

Cross-runtime synchronization 가능해야 한다.

```
Incident Runtime
↔ Recommendation Runtime
↔ Rollback Runtime
↔ Verification Runtime
```

---

## 31. Runtime Replay Rule

State Machine Runtime은 replayable 해야 한다.

예: incident replay · transition replay · rollback replay · verification replay

---

## 32. Timeline Replay Rule

State lifecycle은 timeline replay 가능해야 한다.

예: state replay · policy replay · verification replay · research replay

---

## 33. Systems-Math Integration Rule

State Machine Runtime은 Systems-Math 연결 가능해야 한다.

예: Little's Law · queue utilization · retry amplification · tail latency propagation

**원칙:** Systems-Math는 transition interpretation layer다.

---

## 34. SLO-aware Rule

State Machine Runtime은 SLO-aware 해야 한다.

포함: error budget burn · availability degradation · P99 latency degradation

---

## 35. Quantitative Validation Rule

State Machine Runtime은 정량 검증 가능해야 한다.

지표 예시: MTTR · rollback success rate · verification latency · stabilization latency · regression frequency

---

## 36. Confidence-aware Rule

State Machine Runtime은 confidence-awareness 가져야 한다.

레벨: `HIGH_CONFIDENCE` · `MEDIUM_CONFIDENCE` · `LOW_CONFIDENCE` · `UNKNOWN`

**원칙:** LOW_CONFIDENCE → risky transition 제한

---

## 37. Runtime DTO Rule

State Machine Runtime은 canonical DTO 가져야 한다.

```
RuntimeState
StateTransition
TransitionReason
ReliabilityState
TransitionEvidence
```

---

## 38. Explainability Rule

State transition은 explainable 해야 한다.

**포함:**
- why transition occurred
- why escalation triggered
- why rollback required
- why stabilization failed

**금지:** ❌ opaque transition

---

## 39. Runtime Security Rule

State Machine Runtime은 **privileged operational layer**다.

**필수:** authenticated access · RBAC · audit logging · visibility control

**금지:**
- ❌ anonymous transition mutation
- ❌ unrestricted operational mutation
- ❌ public operational evidence exposure

---

## 40. Auditability Rule

State lifecycle은 audit 가능해야 한다.

포함: what transition occurred · what evidence used · what rollback generated · what verification performed

---

## 41. Immutable Audit Rule

State audit는 **append-only** 해야 한다.

**금지:**
- ❌ audit overwrite
- ❌ hidden transition mutation
- ❌ invisible runtime override

---

## 42. Runtime Failure Rule

State Machine Runtime failure는 explicit 해야 한다.

```
projection inconsistency
verification unavailable
rollback unavailable
runtime desynchronization
```

**금지:** ❌ silent transition degradation

---

## 43. Reliability Dataset Rule

State Machine Runtime은 dataset accumulation 지원 가능해야 한다.

예: transition dataset · rollback dataset · verification dataset · stabilization dataset

---

## 44. Research Compatibility Rule

State Machine Runtime은 Reliability Research 지원 가능해야 한다.

예: Human Approval effectiveness · guardrail effectiveness · rollback effectiveness · runtime convergence effectiveness

---

## 45. Visibility Classification Rule

State Artifact는 visibility classification 가져야 한다.

허용:
- `PUBLIC_PORTFOLIO`
- `PRIVATE_RESEARCH`
- `INTERNAL_OPERATION`
- `PAPER_CANDIDATE`
- `SANITIZED_EXPORT`

---

## 46. Sanitization Rule

State export는 sanitization 가능해야 한다.

**제거 대상:** internal topology · customer payload · secret · token · internal IP

---

## 47. Runtime Metrics Governance Rule

State metric은 **low-cardinality** 유지해야 한다.

**허용:** service · domain · severity · risk_level · state_type

**금지:** customer identifier · payment payload · trace payload dump

---

## 48. Operational Reality Rule

State Machine Runtime은 현실 운영 기반이어야 한다.

**허용:** real incident · real rollback · real observability · real verification

**금지:** toy-only state transition · synthetic-only operational claim

---

## 49. Academic Extension Rule

장기적으로 다음 연구 방향 지원 가능.

- runtime convergence systems
- rollback-aware state machines
- verification-aware reliability state runtime
- Human-in-the-loop operational state governance

---

## 50. Anti-Pattern Rule

**금지:**
- ❌ rollback 없는 transition
- ❌ verification 없는 recovery
- ❌ opaque state mutation
- ❌ unstable transition oscillation
- ❌ unsupported escalation

---

## 51. Non-Goals

State Machine Runtime의 목표는 다음이 아니다.

- autonomous AGI operations
- opaque transition engine
- ungoverned runtime mutation
- unverifiable operational convergence

---

## 52. Canonical Runtime Layers

| Layer | 역할 |
|---|---|
| Evidence | observability/evidence |
| State | runtime state |
| Transition | lifecycle transition |
| Recommendation | orchestration |
| Rollback | safe recovery |
| Verification | result validation |
| Reliability | stabilization/convergence |

---

## 53. Reliability Research Formalization Direction

현재 방향의 핵심은 단순 workflow state 관리가 아니다.

**목표:**

> 운영 observability와 runtime transition lifecycle을  
> **설명 가능하고**  
> **재현 가능하며**  
> **정량 검증 가능하고**  
> **논문화 가능한**  
> **Operational Reliability State Machine Runtime으로 formalization**

하는 것이다.

---

## 한 줄 핵심

> Runtime State Machine의 목적은 단순 상태 관리가 아니다.  
> → incident, rollback, verification, stabilization lifecycle을  
> **재현 가능하고 검증 가능한 Reliability State Runtime으로 formalization** 하는 것이다.