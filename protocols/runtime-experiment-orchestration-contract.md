# Runtime Experiment Orchestration Contract

`protocols/runtime-experiment-orchestration-contract.md`

---

## 1. 목적

이 문서는 FIN-SRE 플랫폼의 **Runtime Experiment Orchestration Layer**를 정의한다.

Experiment Orchestration의 목적은 단순 실험 실행이 아니다.

목적은 **Experiment Planning + Failure Injection + Propagation Analysis + Rollback Runtime + Verification Runtime + Quantitative Validation + Research Runtime**을 기반으로:

- 설명 가능하고
- 재현 가능하며
- 정량 검증 가능하고
- 논문화 가능하며
- 운영 정책 검증 가능한

**Operational Reliability Experiment Orchestration Runtime**을 formalization 하는 것이다.

---

## 2. 핵심 개념

Experiment Orchestration Runtime은 단순 workflow engine이 아니다.

Experiment Runtime은 다음을 갖춘 **operational reliability experimentation runtime**이다.

- Evidence-aware
- Propagation-aware
- Rollback-aware
- Verification-aware
- Research-aware
- Human-governed

---

## 3. Canonical Experiment Definition

Experiment Runtime은 다음을 포함 가능.

| Runtime | 역할 |
|---------|------|
| Planning Runtime | 실험 설계 |
| Injection Runtime | 장애 주입 |
| Propagation Runtime | 확산 분석 |
| Rollback Runtime | 안전 복구 |
| Verification Runtime | 결과 검증 |
| Validation Runtime | 정량 검증 |
| Research Runtime | 연구 orchestration |

---

## 4. Human Governance Rule

Experiment Runtime은 **Human Governance 제거 금지**.

**원칙:**
- AI는 experiment orchestration recommendation을 생성할 수 있다.
- Human이 operational experiment execution을 승인한다.

**금지:**
- ❌ autonomous production experimentation
- ❌ AI-only operational mutation
- ❌ unreviewed failure injection

---

## 5. Canonical Experiment Lifecycle

Experiment Runtime은 canonical lifecycle을 가져야 한다.

```
EXPERIMENT_DEFINED
  → APPROVAL_PENDING
  → INJECTION_RUNNING
  → PROPAGATION_OBSERVING
  → MITIGATION_RUNNING
  → ROLLBACK_RUNNING
  → VERIFICATION_RUNNING
  → VALIDATION_COMPLETED
  → ARCHIVED
```

또는 (abort 경로):

```
PROPAGATION_EXPANDING
  → EXPERIMENT_ABORTED
```

---

## 6. Experiment Planning Rule

모든 experiment는 **explicit planning**을 가져야 한다.

포함: hypothesis, failure_mode, expected propagation, rollback plan, verification plan, success criteria, abort criteria

**금지:** ad-hoc uncontrolled experimentation

---

## 7. Hypothesis Rule

Experiment Runtime은 **hypothesis-aware** 해야 한다.

예:
- Human Approval은 false-positive operational action을 감소시키는가?
- Guardrail은 retry amplification propagation을 감소시키는가?

---

## 8. Comparative Experiment Rule

Experiment Runtime은 **policy comparison**을 지원 가능해야 한다.

예: Guardrail ON/OFF, Human Approval ON/OFF, Rollback Verification ON/OFF, GitOps Drift Detection ON/OFF

---

## 9. Controlled Failure Rule

Experiment Runtime은 **controlled failure만 허용**해야 한다.

**허용:** latency injection, packet loss, consumer slowdown, dependency timeout, retry delay

**금지:**
- unsafe payment corruption
- irreversible infrastructure mutation
- unbounded destructive injection

---

## 10. Blast Radius Rule

Experiment Runtime은 **blast radius awareness**를 가져야 한다.

범위: local / partial / cross-service / global

**원칙:** blast radius 증가 → stricter experiment governance

---

## 11. Production Safety Rule

Production experiment는 **strict governance** 필요.

Production experiment는 rollback, verification, approval이 모두 필수입니다.

---

## 12. Rollback Mandatory Rule

Experiment Runtime은 **rollback-aware** 해야 한다.

**필수:** rollback trigger, rollback timeout, rollback verification, rollback blast radius

> **원칙:** No Rollback → No Experiment

---

## 13. Verification Mandatory Rule

Experiment Runtime은 **verification-aware** 해야 한다.

**필수:** queue stabilization validation, latency recovery validation, payment consistency validation

> **원칙:** No Verification → invalid experiment

---

## 14. Propagation-aware Rule

Experiment Runtime은 **propagation-aware** 해야 한다.

예: dependency cascade, tail latency propagation, queue backlog propagation, retry amplification

---

## 15. Retry Amplification Rule

Experiment Runtime은 **retry amplification 분석** 가능해야 한다.

```
timeout injection → retry storm → queue overload → DB saturation → propagation expansion
```

---

## 16. Stabilization Rule

Experiment Runtime은 **stabilization-aware** 해야 한다.

**필수:** latency stabilized, queue stabilized, retry stabilized, dependency stabilized

---

## 17. Convergence Rule

Experiment Runtime은 **convergence-aware** 해야 한다.

목표: safe stabilization

**금지:** oscillation, recovery thrashing, unstable mitigation loop

---

## 18. Abort Rule

Experiment Runtime은 **abort criteria**를 가져야 한다.

예: propagation beyond threshold, SLO destruction, rollback unavailable, verification failure

**원칙:** unsafe propagation → immediate experiment abort

---

## 19. SLO-aware Rule

Experiment Runtime은 **SLO-aware** 해야 한다.

포함: error budget burn, availability degradation, P99 latency degradation

---

## 20. Quantitative Validation Rule

Experiment Runtime은 **정량 검증 가능**해야 한다.

예: MTTR, rollback success rate, verification latency, propagation reduction, stabilization latency

---

## 21. Statistical Validation Rule

Experiment Runtime은 **statistical validation**을 지원 가능해야 한다.

예: confidence interval, variance, baseline comparison, repeated trial

> **원칙:** single-run conclusion 금지

---

## 22. Reproducibility Rule

Experiment Runtime은 **reproducibility-aware** 해야 한다.

포함: experiment replay, policy replay, rollback replay, verification replay

> **원칙:** 재현 불가능한 실험은 연구 자산이 아니다.

---

## 23. Timeline Governance Rule

Experiment Runtime은 **canonical timeline**을 유지해야 한다.

```
injection → propagation → mitigation → rollback → verification → stabilization
```

---

## 24. Evidence-backed Rule

Experiment Runtime은 **Evidence 기반**이어야 한다.

**허용:** metrics, logs, traces, timeline, verification result, rollback result

**금지:**
- fabricated experiment result
- hallucinated propagation
- unsupported operational claim

---

## 25. Research-aware Rule

Experiment Runtime은 **research-aware** 해야 한다.

포함: hypothesis, comparison group, metric baseline, experiment condition, quantitative result

---

## 26. Dataset-aware Rule

Experiment Runtime은 **dataset accumulation**을 지원 가능해야 한다.

예: experiment dataset, rollback dataset, verification dataset, propagation dataset

---

## 27. Research Assetization Rule

Experiment 결과는 **research asset으로 연결** 가능해야 한다.

예: Experiment Report, Quantitative Validation, Research Note, Paper Draft

---

## 28. Systems-Math Integration Rule

Experiment Runtime은 **Systems-Math 연결** 가능해야 한다.

예: Little's Law, queue utilization, retry amplification, tail latency propagation

**원칙:** Systems-Math는 experiment interpretation layer다.

---

## 29. Reliability State Rule

Experiment Runtime은 **reliability-aware state**를 가져야 한다.

```
HEALTHY / DEGRADED / UNSTABLE / STABILIZING / CONVERGED / FAILED
```

---

## 30. Confidence-aware Rule

Experiment Runtime은 **confidence-awareness**를 가져야 한다.

```
HIGH_CONFIDENCE / MEDIUM_CONFIDENCE / LOW_CONFIDENCE / UNKNOWN
```

**원칙:** LOW_CONFIDENCE → risky experiment 제한

---

## 31. Context-awareness Rule

Experiment Runtime은 **context-aware** 해야 한다.

포함: service, environment, traffic pattern, impact scope

---

## 32. Environment-aware Rule

Experiment Runtime은 **environment-aware** 해야 한다.

예: production / staging / sandbox

**원칙:** production → strictest experiment governance

---

## 33. Severity-aware Rule

Experiment Runtime은 **severity-aware** 해야 한다.

예: SEV-1 / SEV-2 / SEV-3

**원칙:** higher severity → stricter experiment governance

---

## 34. Policy-aware Rule

Experiment Runtime은 **policy-aware** 해야 한다.

예: approval policy, rollback policy, verification policy, blast radius policy

---

## 35. Guardrail Rule

Experiment Runtime은 **Guardrail Runtime**을 통합해야 한다.

예: payment safety guardrail, rollback requirement guardrail, retry amplification guardrail

---

## 36. Unknown State Rule

Unknown 상태는 **restrictive governance** 적용.

예: missing metrics, partial observability, verification unavailable, rollback unavailable

**원칙:** Unknown → risky experiment blocked

---

## 37. FinTech Safety Rule

FinTech 환경에서는 **payment consistency가 최우선**이다.

**금지:**
- duplicate payment injection
- settlement corruption
- unsafe replay

**허용 가능:**
- verified fallback
- verified rollback
- idempotent-safe degradation

---

## 38. Runtime Replay Rule

Experiment Runtime은 **replayable** 해야 한다.

예: experiment replay, rollback replay, verification replay, research replay

---

## 39. Timeline Replay Rule

Experiment lifecycle은 **replay 가능**해야 한다.

예: policy replay, verification replay, stabilization replay

---

## 40. Runtime DTO Rule

Experiment Runtime은 **canonical DTO**를 가져야 한다.

예: ExperimentDefinition, ExperimentCondition, FailureInjection, RollbackResult, VerificationResult, QuantitativeValidation

---

## 41. Explainability Rule

Experiment Runtime은 **explainable** 해야 한다.

포함:
- why experiment executed
- why propagation expanded
- why rollback triggered
- why stabilization failed
- why experiment aborted

**금지:** opaque experimentation

---

## 42. Runtime Security Rule

Experiment Runtime은 **privileged operational layer**다.

**필수:** authenticated access, RBAC, audit logging, visibility control

**금지:**
- ❌ anonymous experiment execution
- ❌ unrestricted failure injection
- ❌ public operational evidence exposure

---

## 43. Auditability Rule

Experiment lifecycle은 **audit 가능**해야 한다.

포함:
- what injection executed
- what rollback triggered
- what verification completed
- what quantitative validation performed

---

## 44. Immutable Audit Rule

Experiment audit는 **append-only** 해야 한다.

**금지:**
- ❌ audit overwrite
- ❌ hidden experiment mutation
- ❌ invisible rollback override

---

## 45. Runtime Failure Rule

Experiment Runtime failure는 **explicit** 해야 한다.

예: rollback unavailable, verification unavailable, timeline inconsistency, runtime desynchronization

**금지:** silent experiment corruption

---

## 46. Visibility Classification Rule

Experiment Artifact는 **visibility classification**을 가져야 한다.

허용: PUBLIC_PORTFOLIO / PRIVATE_RESEARCH / INTERNAL_OPERATION / PAPER_CANDIDATE / SANITIZED_EXPORT

---

## 47. Sanitization Rule

Experiment export는 **sanitization 가능**해야 한다.

제거 대상: internal topology, customer payload, secret, token, internal IP

---

## 48. Runtime Metrics Governance Rule

Experiment metric은 **low-cardinality**를 유지해야 한다.

**허용:** service, domain, severity, failure_mode, experiment_type

**금지:** customer identifier, payment payload, trace payload dump

---

## 49. Operational Reality Rule

Experiment Runtime은 **현실 운영 기반**이어야 한다.

**허용:** real rollback, real observability, real verification, real propagation

**금지:** toy-only experimentation, synthetic-only operational claim

---

## 50. Academic Extension Rule

장기적으로 다음 연구 방향을 지원 가능.

- rollback-aware experimentation systems
- verification-aware reliability experimentation
- Human-in-the-loop reliability experimentation
- propagation-aware operational experimentation

---

## 51. Anti-Pattern Rule

**금지:**
- ❌ rollback 없는 experiment
- ❌ verification 없는 experiment
- ❌ uncontrolled propagation
- ❌ single-run conclusion
- ❌ opaque experiment interpretation

---

## 52. Non-Goals

Experiment Runtime의 목표는 다음이 아니다.

- autonomous AGI experimentation
- destructive chaos automation
- ungoverned operational mutation
- unverifiable experimentation

---

## 53. Canonical Runtime Layers

| Layer | 역할 |
|-------|------|
| Planning | 실험 설계 |
| Injection | 장애 주입 |
| Propagation | 장애 확산 |
| Mitigation | 대응 orchestration |
| Rollback | 안전 복구 |
| Verification | 결과 검증 |
| Validation | 정량 검증 |
| Research | 연구 orchestration |

---

## 54. Reliability Research Formalization Direction

현재 방향의 핵심은 단순 실험 자동화가 아니다.

목표: 운영 observability와 experimentation lifecycle을 다음 조건을 갖춘 **Operational Reliability Experiment Orchestration Runtime**으로 formalization 하는 것이다.

- 설명 가능하고
- 재현 가능하며
- 정량 검증 가능하고
- 논문화 가능한

---

## 한 줄 핵심

> Runtime Experiment Orchestration의 목적은 단순 실험 실행이 아니다.
> → planning, propagation, rollback, verification, validation, research assetization을 통합하여 **재현 가능하고 검증 가능한 Reliability Experimentation Runtime**으로 formalization 하는 것이다.