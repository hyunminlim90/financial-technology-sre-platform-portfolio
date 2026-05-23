# Runtime Knowledge Set Generation Contract

## 1. 목적

이 문서는 FIN-SRE 플랫폼의 Runtime Knowledge Set Generation Layer를 정의한다.

Knowledge Set Generation Runtime의 목적은 단순 markdown 자동 생성이 아니다.

목적은:

```
Stack
+ Representative Failure Mode
+ Operational Reliability Theory
+ Scenario
+ Runbook
+ Systems-Math
+ Experiment
+ Operational Learning
```

를 기반으로:

- 설명 가능하고
- 재현 가능하며
- 정량 검증 가능하고
- 운영 현실 기반이며
- 논문화 가능한

**Operational Reliability Knowledge Set Runtime**을 formalization 하는 것이다.

---

## 2. 핵심 개념

Knowledge Set Generation Runtime은 단순 template engine이 아니다.

Knowledge Set Generation Runtime은:

- Failure-aware
- Propagation-aware
- Rollback-aware
- Verification-aware
- Research-aware
- Human-governed

**operational reliability knowledge formalization runtime**이다.

---

## 3. Canonical Knowledge Set Definition

Knowledge Set은 다음 canonical artifact 집합이다.

| Artifact | 역할 |
|----------|------|
| Scenario | 장애 시나리오 |
| Runbook | 운영 대응 절차 |
| Systems-Math | 정량 분석 모델 |
| Experiment | 재현/실험 |
| Improvement | 개선안 |
| Preventive Design | 예방 설계 |

---

## 4. Human Governance Rule

Knowledge Set Generation Runtime은 **Human Governance 제거 금지**.

**원칙:**

- AI는 Knowledge Set draft를 생성할 수 있다.
- Human이 operational validity와 publication suitability를 승인한다.

**금지:**

- ❌ AI-only production knowledge publishing
- ❌ unreviewed operational recommendation
- ❌ unsupported failure-mode formalization

---

## 5. Canonical Generation Input Rule

Knowledge Set Generation Runtime의 최소 입력은 다음이다.

**필수:**

```
Stack + Representative Failure Mode
```

**예:**

```
Stack: Kafka
Representative Failure Mode: kafka-consumer-lag
```

---

## 6. Canonical Generation Output Rule

Generation Runtime은 canonical artifact 생성 가능해야 한다.

**출력:**

- Scenario
- Runbook
- Systems-Math
- Experiment
- Improvement
- Preventive Design

---

## 7. Scenario Generation Rule

Scenario는 **장애 propagation 중심**이어야 한다.

**포함:**

- failure trigger
- symptom
- blast radius
- dependency propagation
- impact scope

**금지:** 단순 에러 메시지 나열

---

## 8. Runbook Generation Rule

Runbook은 **rollback/verification 중심**이어야 한다.

**필수:**

- detection
- validation
- rollback
- verification
- stabilization
- convergence

**금지:** verification 없는 recovery completion 선언

---

## 9. Systems-Math Generation Rule

Systems-Math는 **operational quantitative model**이어야 한다.

**예:**

- Little's Law
- queue utilization
- retry amplification
- tail latency propagation

**원칙:** Systems-Math는 운영 현실 기반이어야 한다.

---

## 10. Experiment Generation Rule

Experiment는 **reproducible** 해야 한다.

**필수:**

- failure injection
- measurement
- rollback validation
- verification validation
- comparison target

---

## 11. Improvement Generation Rule

Improvement는 **benchmark 기반**이어야 한다.

**예:**

- retry timeout tuning
- consumer scaling
- queue partition rebalance

**필수:** expected reliability impact

---

## 12. Preventive Design Generation Rule

Preventive Design는 **구조적 안정성 중심**이어야 한다.

**예:**

- dependency isolation
- backpressure
- idempotency
- circuit breaker

---

## 13. Representative Failure Mode Rule

Representative Failure Mode는 stack의 핵심 propagation risk여야 한다.

| Stack | Failure Mode |
|-------|-------------|
| Kafka | consumer lag |
| Redis | cache stampede |
| Kubernetes | pod crash loop |
| MySQL | replication lag |
| Istio | retry storm |

---

## 14. Failure-aware Rule

Knowledge Set은 **failure-aware** 해야 한다.

**포함:**

- failure trigger
- failure amplification
- failure containment
- failure recovery

---

## 15. Propagation-aware Rule

Knowledge Set은 **propagation-aware** 해야 한다.

**예:**

- retry amplification
- queue backlog
- dependency cascade
- tail latency propagation

---

## 16. Retry Amplification Rule

Retry amplification은 canonical analysis 대상이다.

```
timeout → retry storm → queue overload → DB saturation
```

---

## 17. Rollback-aware Rule

Knowledge Set은 **rollback-aware** 해야 한다.

**포함:**

- rollback trigger
- rollback validation
- rollback stabilization
- rollback convergence

---

## 18. Verification-aware Rule

Knowledge Set은 **verification-aware** 해야 한다.

**포함:**

- queue stabilization validation
- latency recovery validation
- payment consistency validation

---

## 19. Convergence-aware Rule

Knowledge Set은 **convergence-aware** 해야 한다.

**금지:** unstable recovery를 successful recovery로 기록

---

## 20. Reliability-aware Rule

Knowledge Set은 **reliability-aware** 해야 한다.

**예:**

- rollback reliability
- verification reliability
- propagation containment reliability

---

## 21. FinTech Safety Rule

FinTech 환경에서는 **payment correctness 우선**.

**금지:**

- duplicate payment normalization
- unsafe rollback
- verification 없는 recovery

---

## 22. Human-in-the-loop Rule

Human Approval은 Knowledge Set에 포함 가능해야 한다.

**예:**

```
high-risk operational action → human approval mandatory
```

---

## 23. Guardrail-aware Rule

Knowledge Set은 **Guardrail-aware** 해야 한다.

**예:**

- payment safety guardrail
- rollback mandatory guardrail
- retry amplification guardrail

---

## 24. Evidence-backed Rule

Knowledge Set은 **Evidence 기반**이어야 한다.

**허용:**

- metrics
- logs
- traces
- timeline
- verification result / rollback result / experiment result

**금지:**

- fabricated operational knowledge
- hallucinated propagation path
- unsupported recovery claim

---

## 25. Operational Reality Rule

Knowledge Set은 **현실 운영 기반**이어야 한다.

**허용:**

- real propagation pattern
- real retry behavior
- real queue saturation
- real rollback failure

**금지:** toy-only operational theory

---

## 26. Quantitative Validation Rule

Knowledge Set은 **정량 검증 가능**해야 한다.

**예:**

- MTTR
- rollback success rate
- propagation reduction
- verification mismatch reduction

---

## 27. Statistical Validation Rule

Knowledge Set은 **statistical validation** 지원 가능해야 한다.

**예:**

- confidence interval
- variance
- baseline comparison
- repeated experiment

---

## 28. Experiment-aware Rule

Knowledge Set은 **Experiment Runtime과 연결**되어야 한다.

**포함:**

- failure injection
- policy comparison
- rollback validation
- verification validation

---

## 29. Benchmark-aware Rule

Knowledge Set은 **Benchmark Runtime과 연결**되어야 한다.

**예:**

- rollback benchmark
- verification benchmark
- propagation containment benchmark

---

## 30. Research-aware Rule

Knowledge Set은 **Research Runtime과 연결**되어야 한다.

**포함:**

- hypothesis
- experiment
- validation
- paper candidate

---

## 31. Dataset-aware Rule

Knowledge Set은 **dataset accumulation** 지원 가능해야 한다.

**예:**

- rollback dataset
- verification dataset
- propagation dataset
- knowledge dataset

---

## 32. Research Assetization Rule

Knowledge Set 결과는 **research asset으로 연결** 가능해야 한다.

**예:**

- Experiment Report
- Reliability Analysis
- Research Note
- Paper Draft

---

## 33. Knowledge Graph Integration Rule

Knowledge Set은 **Knowledge Graph와 연결**되어야 한다.

```
Scenario → Runbook → Experiment → Improvement → Preventive Design
```

---

## 34. Operational Memory Integration Rule

Knowledge Set은 **Operational Memory와 연결**되어야 한다.

**예:**

- historical rollback pattern
- historical propagation pattern
- historical false recovery

---

## 35. Operational Consistency Integration Rule

Knowledge Set은 **Consistency Runtime과 연결**되어야 한다.

**예:**

```
verification mismatch → runbook consistency correction
```

---

## 36. Operational Topology Integration Rule

Knowledge Set은 **Topology Runtime과 연결**되어야 한다.

**예:**

```
high dependency density → propagation risk 증가
```

---

## 37. Operational Lineage Integration Rule

Knowledge Set은 **Lineage Runtime과 연결**되어야 한다.

**예:**

```
incident lineage → rollback lineage → verification lineage → knowledge lineage
```

---

## 38. Causal Analysis Integration Rule

Knowledge Set은 **Causal Analysis와 연결**되어야 한다.

**예:**

```
retry storm causality → retry prevention design
```

---

## 39. Systems-Math Integration Rule

Knowledge Set은 **Systems-Math와 연결**되어야 한다.

**예:**

- Little's Law
- queue utilization
- retry amplification
- tail latency propagation

**원칙:** Systems-Math는 knowledge quantitative formalization layer다.

---

## 40. Runtime Replay Rule

Knowledge Set은 **replayable** 해야 한다.

**예:**

- incident replay
- rollback replay
- verification replay
- experiment replay

---

## 41. Reproducibility Rule

Knowledge Set은 **reproducible** 해야 한다.

**필수:**

```
same topology + same policy + same experiment → same result
```

---

## 42. Timeline Governance Rule

Knowledge Set은 **chronology-aware** 해야 한다.

```
failure → propagation → rollback → verification → stabilization → convergence
```

---

## 43. Context-awareness Rule

Knowledge Set은 **context-aware** 해야 한다.

**포함:**

- service
- environment
- traffic pattern
- impact scope

---

## 44. Environment-aware Rule

Knowledge Set은 **environment-aware** 해야 한다.

**예:** production / staging / sandbox

**원칙:** production → strictest operational governance

---

## 45. Severity-aware Rule

Knowledge Set은 **severity-aware** 해야 한다.

**예:** SEV-1 / SEV-2 / SEV-3

**원칙:** higher severity → stricter knowledge governance

---

## 46. Policy-aware Rule

Knowledge Set은 **policy-aware** 해야 한다.

**예:**

- approval policy
- rollback policy
- verification policy
- visibility policy

---

## 47. Unknown State Rule

Unknown 상태는 **restrictive governance** 적용.

**예:**

- missing metrics
- partial observability
- verification unavailable
- rollback unavailable

**원칙:** Unknown → operational certainty 제한

---

## 48. Runtime DTO Rule

Knowledge Set Runtime은 **canonical DTO** 가져야 한다.

**예:**

- KnowledgeSet
- ScenarioDefinition
- RunbookDefinition
- ExperimentDefinition
- PreventiveDesignDefinition

---

## 49. Explainability Rule

Knowledge Set은 **explainable** 해야 한다.

**포함:**

- why propagation occurs
- why rollback required
- why verification mandatory
- why preventive design necessary

**금지:** opaque operational explanation

---

## 50. Runtime Security Rule

Knowledge Set Runtime은 **privileged operational layer**다.

**필수:**

- authenticated access
- RBAC
- audit logging
- visibility control

**금지:**

- ❌ anonymous knowledge mutation
- ❌ unrestricted operational publication
- ❌ public raw operational evidence exposure

---

## 51. Auditability Rule

Knowledge Set lifecycle은 **audit 가능**해야 한다.

**포함:**

- what evidence analyzed
- what rollback validated
- what verification completed
- what benchmark compared

---

## 52. Immutable Audit Rule

Knowledge Set audit는 **append-only** 해야 한다.

**금지:**

- ❌ audit overwrite
- ❌ hidden knowledge mutation
- ❌ invisible lineage corruption

---

## 53. Runtime Failure Rule

Knowledge Set Runtime failure는 **explicit** 해야 한다.

**예:**

- knowledge inconsistency
- timeline inconsistency
- verification unavailable
- rollback unavailable

**금지:** silent operational knowledge corruption

---

## 54. Visibility Classification Rule

Knowledge Set Artifact는 **visibility classification** 가져야 한다.

**허용:**

- `PUBLIC_PORTFOLIO`
- `PRIVATE_RESEARCH`
- `INTERNAL_OPERATION`
- `PAPER_CANDIDATE`
- `SANITIZED_EXPORT`

---

## 55. Sanitization Rule

Knowledge Set export는 **sanitization 가능**해야 한다.

**제거 대상:**

- internal topology
- customer payload
- secret / token
- internal IP
- financially sensitive evidence

---

## 56. Runtime Metrics Governance Rule

Knowledge Set metric은 **low-cardinality** 유지해야 한다.

**허용:**

- service
- domain
- severity
- failure_mode
- knowledge_type

**금지:**

- customer identifier
- payment payload
- trace payload dump

---

## 57. Academic Compatibility Rule

Knowledge Set Runtime은 **학술 확장 가능**해야 한다.

**지원 가능:**

- knowledge reproducibility appendix
- experiment reproducibility appendix
- dataset reproducibility appendix
- operational evidence appendix

---

## 58. Research Integrity Rule

Knowledge Set Runtime은 **research integrity** 보장해야 한다.

**금지:**

- fabricated operational knowledge
- fabricated propagation model
- unsupported reliability conclusion
- hidden contradictory evidence

---

## 59. Long-term Knowledge Evolution Rule

Knowledge Set Runtime은 **장기 knowledge evolution** 지원 가능해야 한다.

**예:**

- rollback knowledge evolution
- verification knowledge evolution
- propagation knowledge evolution
- Human Approval knowledge evolution

---

## 60. Canonical Prompt Rule

사용자는 다음 **minimal prompt**만으로 generation 가능해야 한다.

```
스택: Kafka
대표 장애 유형: kafka-consumer-lag
```

**원칙:** 추가 operational context가 없어도, Runtime Contract Layer 기반으로 Knowledge Set generation 가능해야 한다.

---

## 61. Anti-Pattern Rule

**금지:**

- ❌ rollback 없는 runbook
- ❌ verification 없는 recovery
- ❌ propagation 없는 scenario
- ❌ systems-math 없는 quantitative reasoning
- ❌ evidence 없는 operational claim

---

## 62. Non-Goals

Knowledge Set Generation Runtime의 목표는 다음이 **아니다**:

- 단순 markdown generator
- AI-only operational documentation
- unverifiable reliability explanation
- toy-level failure documentation

---

## 63. Canonical Runtime Layers

| Layer | 역할 |
|-------|------|
| Scenario | 장애 시나리오 |
| Runbook | 운영 대응 절차 |
| Systems-Math | 정량 분석 모델 |
| Experiment | 재현/실험 |
| Improvement | 개선안 |
| Preventive Design | 예방 설계 |

---

## 64. Reliability Research Formalization Direction

현재 방향의 핵심은 단순 문서 생성이 아니다.

**목표:** 운영 observability와 operational lineage를

- 설명 가능하고
- 재현 가능하며
- 정량 검증 가능하고
- 논문화 가능한

**Operational Reliability Knowledge Set Runtime**으로 formalization 하는 것이다.

---

**한 줄 핵심**

> Runtime Knowledge Set Generation의 목적은 단순 markdown 자동 생성이 아니다.
> → Stack + Representative Failure Mode를 기반으로 Scenario / Runbook / Systems-Math / Experiment / Improvement / Preventive Design를 **재현 가능하고 검증 가능한 Operational Reliability Knowledge Set Runtime**으로 formalization 하는 것이다.