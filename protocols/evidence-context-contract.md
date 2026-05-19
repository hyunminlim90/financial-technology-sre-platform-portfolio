# protocols/evidence-context-contract

## 1. 목적

이 문서는 AI-SRE 플랫폼의 Agent Runtime이 운영 판단(reasoning)을 수행하기 위해 사용하는 Runtime Evidence Context의 canonical structure, governance rule, correlation rule, observability integration rule을 정의한다.

> **EvidenceContext의 목적은 단순 observability aggregation이 아니다.**  
> 목적은 "운영 판단의 근거(evidence)를 설명 가능하고 replay 가능한 형태로 보존하는 것" 이다.

---

## 2. 핵심 개념

EvidenceContext는 Runtime Reasoning의 canonical evidence object이다.

**구조:**

```
metrics
+
logs
+
traces
+
alerts
+
SLO
+
deployment events
+
governance timeline
+
experiment evidence

↓

EvidenceContext
```

---

## 3. Evidence-first Runtime Rule

모든 Recommendation은 Evidence 기반이어야 한다.

**금지:**

- ❌ evidence 없는 recommendation
- ❌ intuition-only reasoning
- ❌ retrieval-only reasoning
- ❌ rag/docs-only reasoning

**원칙:**

Evidence 없는 운영 판단 금지

---

## 4. Canonical Evidence Structure Rule

EvidenceContext는 canonical structure를 가져야 한다.

**포함 가능:**

- `MetricEvidence`
- `TraceEvidence`
- `LogEvidence`
- `AlertEvidence`
- `DeploymentEvidence`
- `SLOEvidence`
- `GovernanceEvidence`
- `ExperimentEvidence`
- `SystemsMathEvidence`

---

## 5. Evidence Priority Rule

Evidence는 priority 기반으로 평가된다.

**기본 우선순위:**

```
metrics
→ traces
→ logs
→ governance history
→ inferred reasoning
```

**원칙:**

추론보다 관측 evidence가 우선된다.

---

## 6. Metrics Evidence Rule

Metrics Evidence는 runtime state의 정량 증거다.

**예:**

- latency
- error rate
- consumer lag
- queue depth
- retry rate
- connection pool saturation

**필수 속성:**

- metric name
- value
- timestamp
- window
- source
- confidence

---

## 7. Trace Evidence Rule

Trace Evidence는 request propagation evidence다.

**예:**

- distributed trace
- span latency
- downstream timeout
- retry propagation

**원칙:**

Trace는 propagation analysis에 사용된다.

---

## 8. Log Evidence Rule

Log Evidence는 contextual runtime evidence다.

**예:**

- timeout exception
- circuit breaker open
- connection refused
- rebalance event

**원칙:**

Log는 단독 root cause evidence가 아니다.

---

## 9. Alert Evidence Rule

Alert Evidence는 operational signal이다.

**예:**

- Prometheus Alertmanager
- SLO burn alert
- availability alert

**필수 속성:**

- severity
- firing state
- duration
- source

---

## 10. Deployment Evidence Rule

Deployment Event는 중요한 runtime evidence다.

**예:**

- new deployment
- config rollout
- feature toggle change
- traffic shift

**원칙:**

deployment correlation을 무시하면 안 된다.

---

## 11. SLO Evidence Rule

SLO는 runtime reliability evidence다.

**예:**

- availability
- error budget burn
- tail latency
- recovery time

**원칙:**

SLO degradation은 high-priority operational evidence다.

---

## 12. Governance Timeline Evidence Rule

Governance Timeline은 historical operational evidence다.

**예:**

- recommendation history
- rollback history
- approval history
- verification history

---

## 13. Experiment Evidence Rule

Experiment 결과는 validation evidence다.

**예:**

- chaos experiment
- failure injection
- rollback validation
- recommendation effectiveness

**원칙:**

Experiment는 confidence correction에 사용될 수 있다.

---

## 14. Systems-Math Evidence Rule

Systems-Math는 operational interpretation evidence다.

**예:**

- queue utilization
- Little's Law
- retry amplification
- tail latency propagation

**원칙:**

Systems-Math는 설명 계층이다.

---

## 15. Evidence Freshness Rule

Evidence는 freshness를 가져야 한다.

**예:**

- `fresh`
- `stale`
- `delayed`
- `partial`

**원칙:**

stale evidence는 confidence를 감소시킨다.

---

## 16. Evidence Confidence Rule

Evidence는 confidence classification을 가져야 한다.

| Confidence | 의미 |
|---|---|
| `HIGH` | complete evidence |
| `MEDIUM` | 일부 uncertainty |
| `LOW` | 제한된 observability |
| `DEGRADED` | observability degradation |
| `UNKNOWN` | evidence unavailable |

---

## 17. Correlation Rule

Runtime은 evidence correlation을 지원해야 한다.

**예:**

```
metric spike
+
trace latency
+
timeout log
+
deployment event

↓

deployment-induced latency hypothesis
```

---

## 18. Contradicting Evidence Rule

Evidence 충돌은 explicit 해야 한다.

**예:**

```
metrics healthy
logs failing
```

**원칙:**

충돌 evidence를 숨기면 안 된다.

---

## 19. Unknown Handling Rule

Unknown Evidence는 추정으로 대체하면 안 된다.

**예:**

- missing traces
- sampling loss
- partial metrics
- projection lag

**원칙:**

Unknown은 Unknown으로 유지한다.

---

## 20. Degraded Evidence Rule

Runtime은 degraded evidence mode를 지원할 수 있다.

**예:**

- partial observability
- stale metrics
- trace sampling degradation
- projection unavailable

**출력:**

- `degraded recommendation`
- `low confidence reasoning`

---

## 21. Runtime Evidence Merge Rule

Runtime은 여러 evidence source를 merge 할 수 있다.

**입력:**

- observability
- retrieval context
- timeline
- SLO
- deployment events

**출력:**

- `RuntimeEvidenceContext`

---

## 22. Recommendation Compatibility Rule

EvidenceContext는 RecommendationContext와 호환되어야 한다.

**지원 대상:**

- risk classification
- rollback reasoning
- verification reasoning
- constraint evaluation

---

## 23. Runtime DTO Rule

Evidence는 canonical runtime DTO로 표현될 수 있어야 한다.

**예:**

- `EvidenceContext`
- `MetricEvidence`
- `TraceEvidence`
- `LogEvidence`
- `DeploymentEvidence`
- `AlertEvidence`

---

## 24. Explainability Rule

Runtime은 explainable evidence reasoning을 지원해야 한다.

**설명 가능 대상:**

- 왜 이 metric이 중요했는가
- 왜 deployment event가 correlation 되었는가
- 왜 특정 evidence가 confidence를 낮췄는가

**원칙:**

설명 불가능한 evidence merge 금지

---

## 25. Replay Compatibility Rule

EvidenceContext는 replay 가능해야 한다.

**예:**

- incident replay
- timeline replay
- recommendation replay
- experiment replay

---

## 26. Runtime Auditability Rule

Evidence lifecycle은 audit 가능해야 한다.

**예:**

- which evidence triggered recommendation
- which metric caused escalation
- which deployment correlated

---

## 27. Runtime Metrics Governance Rule

Evidence metrics는 low-cardinality를 유지해야 한다.

**허용:**

- `status`
- `confidence`
- `severity`
- `result`

**금지:**

- `incident_id`
- `trace_id`
- `raw query`
- `full log payload`

---

## 28. FinTech Safety Evidence Rule

EvidenceContext는 FinTech Safety를 우선한다.

**최우선 보호 대상:**

- payment integrity
- idempotency
- duplicate payment prevention
- settlement consistency

**예:**

- duplicate payment signal
- settlement mismatch
- retry amplification

---

## 29. Human-in-the-loop Rule

EvidenceContext는 Human Governance를 제거하지 않는다.

**원칙:**

Evidence는 recommendation을 지원한다.  
Human이 최종 판단한다.

---

## 30. Security Rule

Evidence Runtime은 내부 전용이어야 한다.

**필수:**

- authenticated evidence ingestion
- internal-only evidence access
- audit-protected observability linkage

**금지:**

- ❌ public evidence mutation
- ❌ unauthenticated observability access

---

## 31. Runtime Failure Handling Rule

Evidence Runtime failure는 explicit 해야 한다.

**예:**

- metric ingestion failure
- trace retrieval failure
- timeline projection failure
- deployment event loss

**원칙:**

silent observability failure 금지

---

## 32. Research Compatibility Rule

EvidenceContext는 Reliability Research를 지원해야 한다.

**예:**

- failure propagation analysis
- rollback effectiveness
- recommendation evaluation
- tail latency correlation

---

## 33. Future Runtime Rule

현재 EvidenceContext는 observability 중심이다.

**장기적으로:**

```
Operational Evidence Graph
```

로 발전할 수 있다.

**예:**

- adaptive correlation
- runtime causal graph
- dynamic evidence weighting

---

## 34. Anti-Pattern Rule

**금지:**

- ❌ evidence-free recommendation
- ❌ stale evidence ignored
- ❌ hidden contradiction
- ❌ trace-only root cause certainty
- ❌ deployment correlation omission
- ❌ unverifiable evidence merge

---

## 35. Non-Goals

EvidenceContext의 목표는 다음이 아니다.

- AGI root cause oracle
- perfect observability
- autonomous operational control
- hallucinated certainty

---

## 36. 핵심 원칙

| 계층 | 역할 |
|---|---|
| Metrics | 정량 runtime evidence |
| Traces | propagation evidence |
| Logs | contextual evidence |
| Alerts | operational signals |
| SLO | reliability evidence |
| Deployment | change correlation |
| Governance Timeline | historical operational evidence |
| Experiment | validation evidence |
| Systems-Math | operational interpretation |
| Human | 최종 판단 |

---

> 🎯 **한 줄 핵심**
>
> 운영 판단의 핵심은 "무엇을 아는가"가 아니다.  
> → "무엇을 evidence로 판단했는가"를 설명 가능하게 만드는 것이다.