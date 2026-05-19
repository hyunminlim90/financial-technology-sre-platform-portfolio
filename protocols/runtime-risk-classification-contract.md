# Runtime Risk Classification Contract

`protocols/runtime-risk-classification-contract.md`

---

## 1. 목적

이 문서는 FIN-SRE 플랫폼의 **Runtime Risk Classification Layer**가 운영 위험(Operational Risk)을 정량적이고 explainable한 형태로 분류, 평가, 전파, escalation 하기 위한 **canonical runtime contract**를 정의한다.

Runtime Risk Classification의 목적은 단순 severity tagging이 아니다.

> 목적은 **"운영 위험을 formalized runtime object로 정의"** 하는 것이다.

---

## 2. 핵심 개념

Risk는 Recommendation Runtime 전체의 **기반 계층**이다.

**구조:**

```
Evidence
→ Risk Classification
→ Policy Evaluation
→ Guardrail Evaluation
→ Approval Governance
→ Rollback/Verification
→ Human Decision
```

> 모든 운영 판단은 **Risk-aware** 해야 한다.

---

## 3. Canonical Risk Definition

Runtime Risk는 다음을 포함한다.

| Risk Type | 설명 |
|-----------|------|
| Availability Risk | 서비스 가용성 위험 |
| Consistency Risk | 데이터 정합성 위험 |
| Payment Risk | 결제 무결성 위험 |
| Propagation Risk | 장애 전파 위험 |
| Recovery Risk | 복구 실패 위험 |
| Rollback Risk | rollback 실패 위험 |
| Verification Risk | 검증 불가능 위험 |
| Operational Risk | 운영 조치 위험 |
| Drift Risk | GitOps/Runtime drift 위험 |
| Experiment Risk | 실험 기반 운영 위험 |

---

## 4. Risk Classification Model

모든 Runtime Risk는 canonical severity를 가져야 한다.

| Severity | 의미 |
|----------|------|
| LOW | 제한적 영향 |
| MEDIUM | partial degradation |
| HIGH | 운영 escalation 필요 |
| CRITICAL | 결제/정합성/전역 장애 위험 |

---

## 5. FinTech Priority Rule (최상위)

FinTech 시스템에서는 다음 우선순위를 따른다.

```
payment integrity > consistency > availability > performance
```

> 성능보다 **결제 안전성**이 우선된다.

---

## 6. Payment Risk Rule

다음은 항상 **HIGH 이상**으로 간주된다.

- `duplicate payment possibility`
- `idempotency violation`
- `settlement inconsistency`
- `unverified replay execution`

> Payment uncertainty → **HIGH or CRITICAL**

---

## 7. Risk Propagation Rule

Risk는 propagation graph를 가져야 한다.

```
Redis timeout
→ retry amplification
→ queue saturation
→ DB overload
→ payment timeout
→ duplicate retry
```

> Risk는 **isolated object가 아니다.**

---

## 8. Retry Amplification Rule

Retry는 **risk amplification source**로 간주된다.

```
retry increase
→ downstream saturation
→ queue growth
→ tail latency explosion
```

> Retry amplification은 **system-wide risk propagation** 가능성이 있다.

---

## 9. Queue Collapse Rule

Queue saturation은 **collapse precursor**로 간주된다.

```
consumer lag explosion → queue collapse risk
```

관련 Systems-Math: Little's Law, Queue Utilization, Tail Latency Propagation

---

## 10. Tail Latency Risk Rule

Tail latency는 **high-priority operational signal**이다.

```
P99 latency explosion
→ retry amplification risk
→ timeout propagation risk
```

---

## 11. Availability Risk Rule

Availability degradation은 **SLO-aware** 해야 한다.

- `5xx increase`
- `timeout increase`
- `error budget burn`

---

## 12. Consistency Risk Rule

Consistency Risk는 **availability보다 우선**된다.

- `partial write possibility`
- `event replay inconsistency`
- `cache/database divergence`

> availability sacrifice 허용 가능  
> **consistency sacrifice 불가**

---

## 13. Drift Risk Rule

Runtime drift는 **operational instability source**로 간주된다.

- `kubectl direct mutation`
- `ArgoCD drift`
- `configuration divergence`

> **GitOps is source of truth**

---

## 14. Unknown Risk Rule

Unknown은 **위험한 것**으로 간주한다.

- `missing metrics`
- `missing traces`
- `stale evidence`
- `partial observability`
- `projection lag`

> Unknown → **restrictive governance**

---

## 15. Degraded Runtime Risk Rule

Runtime degradation 시 risk level은 **증가**해야 한다.

- `projection unavailable`
- `retrieval degradation`
- `timeline inconsistency`

> Runtime uncertainty → **risk escalation**

---

## 16. Rollback Risk Rule

Rollback 불가능 상태는 **CRITICAL risk**다.

- `rollback unavailable`
- `rollback unverified`
- `rollback ambiguity`

> **No Rollback → No Risky Recommendation**

---

## 17. Verification Risk Rule

Verification 불가능 상태는 **escalation 대상**이다.

- `verification impossible`
- `verification incomplete`
- `verification stale`

---

## 18. Approval Risk Rule

Approval governance는 **risk-aware** 해야 한다.

| Risk | Approval |
|------|----------|
| LOW | optional |
| MEDIUM | recommended |
| HIGH | mandatory |
| CRITICAL | escalation mandatory |

---

## 19. Risk Weighting Rule

Risk weighting은 canonical priority를 따라야 한다.

```
Payment Integrity
> Consistency
> Rollback Availability
> Verification Capability
> Availability
> Performance
```

---

## 20. Risk Explainability Rule

모든 Runtime Risk는 **explainable** 해야 한다.

설명 가능 대상:

- 왜 HIGH risk인가
- 왜 escalation 되었는가
- 왜 recommendation blocked 되었는가
- 왜 rollback mandatory인가

> **Opaque risk classification 금지**

---

## 21. Systems-Math Integration Rule

Risk Classification은 **Systems-Math 기반 reasoning**을 사용할 수 있다.

- Little's Law
- Queue Utilization
- Retry Amplification
- Tail Latency Propagation
- Backpressure Saturation

> Systems-Math는 **risk explanation layer**다.

---

## 22. SLO-aware Risk Rule

Runtime Risk는 **SLO-aware** 해야 한다.

- `availability SLO violation`
- `latency SLO burn`
- `error budget exhaustion`

---

## 23. Evidence-aware Risk Rule

Risk Classification은 **EvidenceContext 기반**이어야 한다.

**사용 가능:** metrics, traces, logs, alerts, verification state, timeline state

**금지:** hallucinated evidence, unverified assumptions

---

## 24. Experiment-aware Risk Rule

Experiment 결과는 **risk weighting에 영향**을 줄 수 있다.

- historical rollback failure → rollback risk increase
- historical false positive recommendation → stricter approval governance

---

## 25. Recommendation Risk Rule

Recommendation 자체도 **risk object**다.

- `aggressive scale-out risk`
- `unsafe replay risk`
- `unverified mitigation risk`

---

## 26. Runtime DTO Rule

Risk Runtime은 canonical DTO를 가져야 한다.

- `RiskClassification`
- `RiskFactor`
- `RiskPropagation`
- `RiskEvaluationResult`
- `RiskEscalationDecision`

---

## 27. Risk Propagation Graph Rule

Risk는 **graph structure**를 가질 수 있다.

```
Retry Storm
→ Queue Saturation
→ DB Overload
→ Payment Failure
```

> Risk는 **topology-aware** 해야 한다.

---

## 28. Runtime Metrics Governance Rule

Risk metrics는 **low-cardinality** 유지해야 한다.

**허용:** `risk_type`, `risk_level`, `service`, `domain`

**금지:** `incident_id`, `full recommendation`, `raw evidence payload`

---

## 29. Governance Timeline Integration Rule

Risk lifecycle은 **timeline replay 가능**해야 한다.

- risk escalation history
- rollback failure history
- approval history
- verification history

---

## 30. Runtime Replay Rule

Risk Runtime은 **replay 가능**해야 한다.

- risk replay
- risk propagation replay
- incident replay
- verification replay

---

## 31. Human-in-the-loop Rule

Risk Runtime은 **Human Governance를 제거하지 않는다.**

> AI는 risk를 **설명**한다.  
> Human이 **최종 판단**한다.

---

## 32. Runtime Failure Rule

Risk Classification failure는 **explicit** 해야 한다.

- `classification unavailable`
- `evidence inconsistency`
- `projection corruption`

> **silent risk degradation 금지**

---

## 33. Runtime Security Rule

Risk Runtime은 **내부 운영 계층**이다.

**필수:** authenticated runtime access, internal governance boundary, audit logging

**금지:**
- ❌ public risk mutation
- ❌ external runtime override

---

## 34. Quantitative Validation Rule

Risk Classification은 **quantitative validation 가능**해야 한다.

- MTTR
- false positive rate
- rollback success rate
- propagation reduction

---

## 35. Research Compatibility Rule

Risk Runtime은 **Reliability Research를 지원**해야 한다.

- risk propagation analysis
- guardrail effectiveness
- rollback reliability analysis
- approval latency impact

---

## 36. Academic Extension Rule

장기적으로 Runtime Risk는 다음 연구 방향으로 발전할 수 있다.

- adaptive runtime risk weighting
- context-aware risk governance
- risk-aware recommendation topology

---

## 37. Anti-Pattern Rule

**금지:**

- ❌ availability-first governance
- ❌ rollback 없는 recommendation
- ❌ opaque risk evaluation
- ❌ unverifiable mitigation
- ❌ payment-risking recommendation
- ❌ hallucinated evidence

---

## 38. Non-Goals

Runtime Risk Classification의 목표는 다음이 **아니다.**

- AGI decision replacement
- autonomous operational authority
- human-free governance
- opaque AI scoring

---

## 39. 핵심 원칙

| 계층 | 역할 |
|------|------|
| Evidence | 운영 증거 |
| Risk | 위험 formalization |
| Policy | 운영 규칙 |
| Guardrail | runtime 안전 경계 |
| Approval | Human Governance |
| Rollback | 안전 복구 |
| Verification | 결과 검증 |
| Timeline | replay/audit |

---

## 40. Reliability Research Direction

현재 방향의 핵심은 단순 자동화가 아니다.

**목표:**

> 운영 위험을  
> **설명 가능하고**  
> **재현 가능하고**  
> **정량 검증 가능한 형태로**  
> **formalization** 하는 것이다.

---

## 한 줄 핵심

> Runtime Risk Classification의 목적은 단순 severity tagging이 아니다.  
> → 운영 위험을 **explainable**하고 **replayable**한 **runtime governance object**로 formalization 하는 것이다.