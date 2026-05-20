# Runtime Failure Propagation Contract

## 1. 목적

이 문서는 FIN-SRE 플랫폼의 **Runtime Failure Propagation Layer**를 정의한다.

Failure Propagation Runtime의 목적은 단순 장애 탐지가 아니다.

목적은:

```
Failure Signal
+ Dependency Runtime
+ Retry Runtime
+ Queue Runtime
+ Latency Runtime
+ Recommendation Runtime
```

을 기반으로:

> 장애 확산을  
> **설명 가능하고**  
> **재현 가능하며**  
> **정량 검증 가능하고**  
> **예측 가능하게** 만드는  
> **Operational Failure Propagation Runtime**

을 formalization 하는 것이다.

---

## 2. 핵심 개념

Failure Propagation은 단순 장애 연쇄가 아니다.

Propagation Runtime은:

- Dependency-aware
- Queue-aware
- Latency-aware
- Retry-aware
- Blast-radius-aware
- Human-governed

operational propagation analysis runtime이다.

---

## 3. Canonical Failure Definition

Propagation Runtime은 다음을 포함 가능.

| Component | 역할 |
|---|---|
| Root Failure | 최초 장애 |
| Dependency Chain | 의존성 흐름 |
| Retry Runtime | retry propagation |
| Queue Runtime | backlog propagation |
| Latency Runtime | tail latency propagation |
| Blast Radius | 영향 범위 |
| Recovery Runtime | 복구 흐름 |

---

## 4. Human Governance Rule

Propagation Runtime은 **Human Governance 제거 금지**.

**원칙:**
- AI는 propagation을 분석할 수 있다.
- Human이 operational mitigation을 승인한다.

**금지:**
- ❌ autonomous infrastructure mutation
- ❌ AI-only mitigation execution
- ❌ unreviewed propagation action

---

## 5. Canonical Propagation Lifecycle

Propagation Runtime은 canonical lifecycle 가져야 한다.

```
FAILURE_DETECTED
→ DEPENDENCY_ANALYZED
→ PROPAGATION_ESTIMATED
→ RISK_CLASSIFIED
→ MITIGATION_RECOMMENDED
→ VERIFICATION_PENDING
```

또는:

```
PROPAGATION_EXPANDING
→ ESCALATION_REQUIRED
```

---

## 6. Dependency-aware Rule

Propagation Runtime은 dependency-aware 해야 한다.

```
API → Redis → PostgreSQL → Kafka
```

**원칙:** dependency chain 없는 propagation analysis 금지

---

## 7. Retry Amplification Rule

Propagation Runtime은 retry amplification 이해 가능해야 한다.

```
Redis timeout
→ retry increase
→ queue overload
→ DB saturation
→ latency explosion
```

**원칙:** retry amplification은 propagation multiplier다.

---

## 8. Tail Latency Propagation Rule

Propagation Runtime은 tail latency propagation 분석 가능해야 한다.

```
P99 latency increase
→ queue backlog
→ timeout increase
→ retry storm
→ cross-service degradation
```

---

## 9. Queue Propagation Rule

Queue Runtime은 propagation-aware 해야 한다.

```
Kafka lag
→ backlog accumulation
→ processing delay
→ downstream timeout
```

---

## 10. Blast Radius Rule

Propagation Runtime은 blast radius awareness 가져야 한다.

범위 단계: `local` → `partial` → `cross-service` → `global`

**원칙:** blast radius 증가 → stricter governance

---

## 11. Cascading Failure Rule

Propagation Runtime은 cascading failure 분석 가능해야 한다.

```
Redis degradation
→ API timeout
→ retry amplification
→ DB overload
→ global instability
```

---

## 12. FinTech Safety Rule

FinTech 환경에서는 **payment consistency가 최우선**이다.

**금지:**
- unsafe retry propagation
- duplicate payment propagation
- settlement inconsistency propagation

**허용 가능:**
- idempotent-safe mitigation
- verified fallback
- verified rollback

---

## 13. No Blind Retry Rule

Propagation Runtime은 blind retry 금지해야 한다.

```
retry increase + dependency degraded
→ propagation acceleration
```

**원칙:** retry는 recovery mechanism이 아니라 propagation trigger가 될 수 있다.

---

## 14. Queue Saturation Rule

Propagation Runtime은 queue saturation 이해 가능해야 한다.

```
consumer slowdown
→ backlog accumulation
→ timeout increase
→ propagation
```

---

## 15. Thread Pool Saturation Rule

Propagation Runtime은 thread/resource saturation 분석 가능해야 한다.

```
blocking I/O
→ thread starvation
→ queue buildup
→ latency propagation
```

---

## 16. Connection Pool Propagation Rule

Propagation Runtime은 connection pool propagation 이해 가능해야 한다.

```
DB connection exhaustion
→ request queueing
→ timeout propagation
→ retry amplification
```

---

## 17. Latency-aware Rule

Propagation Runtime은 latency-aware 해야 한다.

포함: `P95` · `P99` · `tail latency` · `timeout propagation`

---

## 18. Propagation Estimation Rule

Propagation Runtime은 propagation estimation 가능해야 한다.

- expected queue growth
- expected retry amplification
- expected latency degradation

---

## 19. Recovery-aware Rule

Propagation Runtime은 recovery-aware 해야 한다.

포함: stabilization detection · queue recovery · latency recovery · dependency recovery

---

## 20. Rollback-aware Rule

Propagation Runtime은 rollback-aware 해야 한다.

**필수:**
- rollback trigger
- rollback timeout
- rollback verification
- rollback blast radius

**원칙:** No Rollback → risky propagation mitigation blocked

---

## 21. Verification-aware Rule

Propagation Runtime은 verification-aware 해야 한다.

포함: queue stabilization validation · latency recovery validation · payment consistency validation

---

## 22. Propagation Mitigation Rule

Propagation mitigation은 propagation-aware 해야 한다.

**허용:** fallback activation · load shedding · rate limiting · circuit breaker

**금지:** unsafe retry increase · blind scale-out

---

## 23. Recommendation-aware Rule

Propagation Runtime은 recommendation runtime과 연결 가능해야 한다.

```
propagation expansion
→ mitigation recommendation
→ rollback recommendation
```

---

## 24. Policy-aware Rule

Propagation Runtime은 policy-aware 해야 한다.

정책 예시: blast radius policy · rollback policy · verification policy · retry policy

---

## 25. Guardrail Rule

Propagation Runtime은 Guardrail Runtime 통합해야 한다.

예: retry amplification guardrail · payment safety guardrail · rollback requirement guardrail

---

## 26. Severity-aware Rule

Propagation Runtime은 severity-aware 해야 한다.

레벨: `SEV-1` · `SEV-2` · `SEV-3`

**원칙:** higher severity → stricter propagation governance

---

## 27. Context-awareness Rule

Propagation Runtime은 context-aware 해야 한다.

포함: service · environment · traffic pattern · impact scope

---

## 28. Environment-aware Rule

Propagation Runtime은 environment-aware 해야 한다.

환경: `production` · `staging` · `sandbox`

**원칙:** production → stricter propagation governance

---

## 29. Evidence-backed Rule

Propagation Runtime은 Evidence 기반이어야 한다.

**허용:** metrics · logs · traces · timeline · verification result · rollback result

**금지:**
- ❌ hallucinated propagation
- ❌ fabricated dependency chain
- ❌ unsupported operational claim

---

## 30. Systems-Math Integration Rule

Propagation Runtime은 Systems-Math 연결 가능해야 한다.

예: Little's Law · queue utilization · retry amplification · tail latency propagation

**원칙:** Systems-Math는 propagation interpretation layer다.

---

## 31. SLO-aware Rule

Propagation Runtime은 SLO-aware 해야 한다.

포함: error budget burn · availability degradation · P99 propagation

---

## 32. Quantitative Validation Rule

Propagation Runtime은 정량 검증 가능해야 한다.

지표 예시: propagation reduction · MTTR · rollback success rate · queue stabilization latency

---

## 33. Confidence-aware Rule

Propagation Runtime은 confidence-awareness 가져야 한다.

레벨: `HIGH_CONFIDENCE` · `MEDIUM_CONFIDENCE` · `LOW_CONFIDENCE` · `UNKNOWN`

**원칙:** LOW_CONFIDENCE → risky mitigation 제한

---

## 34. Runtime DTO Rule

Propagation Runtime은 canonical DTO 가져야 한다.

```
PropagationContext
DependencyChain
PropagationEstimate
BlastRadius
RecoveryState
```

---

## 35. Timeline Replay Rule

Propagation lifecycle은 replay 가능해야 한다.

예: propagation replay · rollback replay · verification replay · dependency replay

---

## 36. Runtime Replay Rule

Propagation Runtime은 replayable 해야 한다.

예: incident replay · propagation replay · policy replay · research replay

---

## 37. Propagation Explainability Rule

Propagation analysis는 explainable 해야 한다.

**포함:**
- why propagation expanded
- why queue saturated
- why retry amplified
- why blast radius increased

**금지:** ❌ opaque propagation analysis

---

## 38. Runtime Security Rule

Propagation Runtime은 **privileged operational layer**다.

**필수:** authenticated access · RBAC · audit logging · visibility control

**금지:**
- ❌ anonymous propagation mutation
- ❌ unrestricted dependency mutation
- ❌ public operational evidence exposure

---

## 39. Auditability Rule

Propagation lifecycle은 audit 가능해야 한다.

포함: what dependency analyzed · what propagation estimated · what rollback required · what mitigation recommended

---

## 40. Immutable Audit Rule

Propagation audit는 **append-only** 해야 한다.

**금지:**
- ❌ audit overwrite
- ❌ hidden propagation mutation
- ❌ invisible dependency override

---

## 41. Runtime Failure Rule

Propagation Runtime failure는 explicit 해야 한다.

```
dependency graph unavailable
queue metrics unavailable
verification unavailable
rollback unavailable
```

**금지:** ❌ silent propagation degradation

---

## 42. Reliability Dataset Rule

Propagation Runtime은 dataset accumulation 지원 가능해야 한다.

예: propagation dataset · retry amplification dataset · queue saturation dataset · rollback dataset

---

## 43. Research Compatibility Rule

Propagation Runtime은 Reliability Research 지원 가능해야 한다.

예: retry amplification effectiveness · guardrail effectiveness · rollback effectiveness · propagation mitigation effectiveness

---

## 44. Visibility Classification Rule

Propagation Artifact는 visibility classification 가져야 한다.

허용:
- `PUBLIC_PORTFOLIO`
- `PRIVATE_RESEARCH`
- `INTERNAL_OPERATION`
- `PAPER_CANDIDATE`
- `SANITIZED_EXPORT`

---

## 45. Sanitization Rule

Propagation export는 sanitization 가능해야 한다.

**제거 대상:** internal topology · customer payload · secret · token · internal IP

---

## 46. Runtime Metrics Governance Rule

Propagation metric은 **low-cardinality** 유지해야 한다.

**허용:** service · domain · severity · risk_level · dependency_type

**금지:** customer identifier · payment payload · trace payload dump

---

## 47. Operational Reality Rule

Propagation Runtime은 현실 운영 기반이어야 한다.

**허용:** real incident · real rollback · real observability · real verification

**금지:** toy-only propagation analysis · synthetic-only operational claim

---

## 48. Academic Extension Rule

장기적으로 다음 연구 방향 지원 가능.

- retry-aware propagation governance
- queue-aware reliability systems
- tail-latency propagation analysis
- Human-in-the-loop propagation mitigation

---

## 49. Anti-Pattern Rule

**금지:**
- ❌ blind retry amplification
- ❌ rollback 없는 mitigation
- ❌ verification 없는 propagation recovery
- ❌ opaque dependency analysis
- ❌ unsupported propagation claim

---

## 50. Non-Goals

Propagation Runtime의 목표는 다음이 아니다.

- autonomous AGI operations
- opaque dependency prediction
- ungoverned retry orchestration
- unverifiable propagation estimation

---

## 51. Canonical Runtime Layers

| Layer | 역할 |
|---|---|
| Failure | 장애 감지 |
| Dependency | 의존성 분석 |
| Queue | backlog/runtime |
| Retry | retry propagation |
| Risk | blast radius/risk |
| Recommendation | mitigation orchestration |
| Rollback | 안전 복구 |
| Verification | 결과 검증 |

---

## 52. Reliability Research Formalization Direction

현재 방향의 핵심은 단순 장애 탐지 시스템이 아니다.

**목표:**

> 운영 observability와 dependency runtime을  
> **설명 가능하고**  
> **재현 가능하며**  
> **정량 검증 가능하고**  
> **논문화 가능한**  
> **Operational Failure Propagation Runtime으로 formalization**  

하는 것이다.

---

## 한 줄 핵심

> Runtime Failure Propagation의 목적은 단순 장애 탐지가 아니다.  
> → retry, queue, latency, dependency propagation을 분석하여 장애 확산을  
> **재현 가능하고 검증 가능한 Reliability Propagation Runtime으로 formalization** 하는 것이다.