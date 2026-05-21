# Runtime Operational Topology Contract

`protocols/runtime-operational-topology-contract.md`

---

## 1. 목적

이 문서는 FIN-SRE 플랫폼의 Runtime Operational Topology Layer를 정의한다.

Operational Topology Runtime의 목적은 단순 infrastructure diagram 생성이 아니다.

목적은 다음을 기반으로:

- Service Dependency
- Propagation Path
- Rollback Boundary
- Verification Scope
- Traffic Flow
- Queue Flow
- Payment Flow
- Research Runtime

**설명 가능하고, 재현 가능하며, 정량 검증 가능하고, 운영 현실 기반이며, 논문화 가능한 Operational Reliability Topology Runtime을 formalization 하는 것이다.**

---

## 2. 핵심 개념

Operational Topology Runtime은 단순 네트워크 맵이 아니다.

Operational Topology Runtime은 다음을 갖춘 **operational relationship graph runtime**이다:

- Dependency-aware
- Propagation-aware
- Rollback-aware
- Verification-aware
- Research-aware
- Human-governed

---

## 3. Canonical Operational Topology Definition

Operational Topology Runtime은 다음 topology domain을 지원 가능해야 한다.

| Topology Domain | 역할 |
|---|---|
| Service Topology | 서비스 의존 관계 |
| Queue Topology | queue 흐름 |
| Payment Flow Topology | 결제 흐름 |
| Rollback Boundary Topology | rollback 경계 |
| Propagation Topology | 장애 전파 경로 |
| Verification Scope Topology | verification 범위 |

---

## 4. Human Governance Rule

Operational Topology Runtime은 **Human Governance 제거 금지**.

**원칙:**
- AI는 operational topology inference를 생성할 수 있다.
- Human이 topology interpretation과 operational governance를 승인한다.

**금지:**
- ❌ autonomous topology mutation
- ❌ AI-only dependency declaration
- ❌ unreviewed operational topology rewrite

---

## 5. Canonical Topology Lifecycle

Operational Topology Runtime은 canonical lifecycle을 가져야 한다.

```
TOPOLOGY_DISCOVERED
→ EVIDENCE_CORRELATED
→ DEPENDENCY_VALIDATED
→ PROPAGATION_LINKED
→ VERSIONED
→ RESEARCH_ASSETIZED
→ ARCHIVED
```

---

## 6. Service Dependency Rule

Operational Topology Runtime은 service dependency graph를 지원 가능해야 한다.

```
API Gateway
→ Payment API
→ Kafka
→ Settlement Service
→ Database
```

---

## 7. Queue Topology Rule

Operational Topology Runtime은 queue topology를 지원 가능해야 한다.

```
Producer
→ Kafka Topic
→ Consumer Group
→ DLQ
```

---

## 8. Payment Flow Topology Rule

Operational Topology Runtime은 payment flow topology를 지원 가능해야 한다.

```
Payment Request
→ Authorization
→ Idempotency Validation
→ Ledger Update
→ Settlement
```

---

## 9. Rollback Boundary Rule

Rollback boundary는 **topology-aware** 해야 한다.

```
Application rollback boundary ≠ Database rollback boundary
```

**원칙:** rollback boundary는 topology dependency 기반이어야 한다.

---

## 10. Verification Scope Rule

Verification scope는 **topology-aware** 해야 한다.

- queue stabilization validation
- dependency health validation
- payment consistency validation

---

## 11. Propagation Topology Rule

Operational Topology Runtime은 propagation path 분석 가능해야 한다.

```
API latency
→ retry storm
→ queue backlog
→ DB saturation
→ payment propagation
```

---

## 12. Retry Amplification Rule

Retry amplification topology 분석 가능해야 한다.

```
client retry
→ gateway retry
→ consumer retry
→ DB overload
```

---

## 13. Blast Radius Rule

Operational Topology Runtime은 blast radius topology 분석 가능해야 한다.

범위: `local` / `partial` / `cross-service` / `global`

**원칙:** dependency density 증가 → blast radius 증가 가능

---

## 14. Dependency Density Rule

Operational Topology Runtime은 dependency density 평가 가능해야 한다.

- high fan-out dependency
- high retry dependency
- shared DB dependency

---

## 15. Critical Path Rule

Operational Topology Runtime은 critical operational path 분석 가능해야 한다.

- payment authorization critical path
- settlement critical path
- verification critical path

---

## 16. Convergence-aware Rule

Operational Topology Runtime은 **convergence-aware** 해야 한다.

**목표:** safe stabilization topology

**금지:** oscillation-prone dependency topology

---

## 17. Reliability-aware Rule

Operational Topology Runtime은 **reliability-aware** 해야 한다.

- single point of failure
- dependency isolation effectiveness
- rollback containment reliability

---

## 18. Evidence-backed Rule

Operational Topology Runtime은 **Evidence 기반**이어야 한다.

**허용:**
- metrics / logs / traces / timeline
- verification result / rollback result / experiment result

**금지:**
- fabricated dependency graph
- hallucinated propagation path
- unsupported topology inference

---

## 19. Timeline Governance Rule

Operational Topology Runtime은 **chronology-aware** 해야 한다.

```
failure
→ propagation
→ mitigation
→ rollback
→ verification
→ stabilization
```

---

## 20. Knowledge Graph Integration Rule

Operational Topology Runtime은 Knowledge Graph 연결 가능해야 한다.

```
Scenario
→ Runbook
→ Improvement
→ Preventive Design
→ Operational Topology
```

---

## 21. Operational Memory Integration Rule

Operational Topology Runtime은 Operational Memory 연결 가능해야 한다.

- historical propagation topology
- historical rollback boundary
- historical dependency failure

---

## 22. Causal Analysis Integration Rule

Operational Topology Runtime은 Causal Analysis 연결 가능해야 한다.

- retry storm topology
- queue overload topology
- payment propagation topology

---

## 23. Comparative Topology Rule

Operational Topology Runtime은 comparative evaluation 가능해야 한다.

- before preventive design **vs** after preventive design
- before guardrail **vs** after guardrail

---

## 24. Quantitative Validation Rule

Operational Topology Runtime은 정량 검증 가능해야 한다.

- MTTR
- rollback success rate
- verification latency
- propagation reduction
- dependency isolation improvement

---

## 25. Statistical Validation Rule

Operational Topology Runtime은 statistical validation을 지원 가능해야 한다.

- confidence interval / variance / baseline comparison / repeated experiment

**원칙:** single-event topology certainty 금지

---

## 26. Experiment-aware Rule

Operational Topology Runtime은 **experiment-aware** 해야 한다.

- failure injection
- dependency isolation validation
- rollback boundary validation
- verification topology validation

---

## 27. Research-aware Rule

Operational Topology Runtime은 **research-aware** 해야 한다.

- hypothesis / experiment / validation / paper candidate

---

## 28. Dataset-aware Rule

Operational Topology Runtime은 dataset accumulation을 지원 가능해야 한다.

- dependency dataset
- rollback topology dataset
- verification topology dataset
- propagation topology dataset

---

## 29. Research Assetization Rule

Topology 결과는 research asset으로 연결 가능해야 한다.

- Topology Report / Propagation Analysis / Research Note / Paper Draft

---

## 30. Reproducibility Rule

Operational Topology Runtime은 **reproducibility-aware** 해야 한다.

- experiment replay / policy replay / rollback replay / verification replay

**원칙:** 재현 불가능한 topology inference는 신뢰 불가

---

## 31. Runtime Replay Rule

Operational Topology Runtime은 **replayable** 해야 한다.

- incident replay / rollback replay / verification replay / topology replay

---

## 32. Systems-Math Integration Rule

Operational Topology Runtime은 Systems-Math 연결 가능해야 한다.

- Little's Law / queue utilization / retry amplification / tail latency propagation

**원칙:** Systems-Math는 topology interpretation layer다.

---

## 33. FinTech Safety Rule

FinTech 환경에서는 **payment consistency가 최우선**이다.

**금지:**
- unsafe payment dependency topology
- duplicate payment propagation
- settlement inconsistency

**허용:**
- verified payment-safe topology
- sanitized operational topology

---

## 34. SLO-aware Rule

Operational Topology Runtime은 **SLO-aware** 해야 한다.

- error budget burn
- availability degradation
- P99 latency degradation

---

## 35. Context-awareness Rule

Operational Topology Runtime은 **context-aware** 해야 한다.

- service / environment / traffic pattern / impact scope

---

## 36. Environment-aware Rule

Operational Topology Runtime은 **environment-aware** 해야 한다.

환경: `production` / `staging` / `sandbox`

**원칙:** production → strictest topology governance

---

## 37. Severity-aware Rule

Operational Topology Runtime은 **severity-aware** 해야 한다.

심각도: `SEV-1` / `SEV-2` / `SEV-3`

**원칙:** higher severity → stricter topology governance

---

## 38. Policy-aware Rule

Operational Topology Runtime은 **policy-aware** 해야 한다.

- approval policy / rollback policy / verification policy / visibility policy

---

## 39. Guardrail Rule

Operational Topology Runtime은 **Guardrail Runtime을 통합**해야 한다.

- payment safety guardrail
- rollback requirement guardrail
- retry amplification guardrail

---

## 40. Unknown State Rule

Unknown 상태는 **restrictive governance** 적용.

상태: missing metrics / partial observability / verification unavailable / rollback unavailable

**원칙:** Unknown → topology certainty 제한

---

## 41. Reliability State Rule

Operational Topology Runtime은 reliability-aware state를 가져야 한다.

`HEALTHY` / `DEGRADED` / `UNSTABLE` / `STABILIZING` / `CONVERGED` / `FAILED`

---

## 42. Confidence-aware Rule

Operational Topology Runtime은 **confidence-awareness**를 가져야 한다.

`HIGH_CONFIDENCE` / `MEDIUM_CONFIDENCE` / `LOW_CONFIDENCE` / `UNKNOWN`

**원칙:** LOW_CONFIDENCE → topology inference 제한

---

## 43. Runtime DTO Rule

Operational Topology Runtime은 canonical DTO를 가져야 한다.

- OperationalTopology / DependencyGraph / PropagationGraph / RollbackBoundary / VerificationScope

---

## 44. Explainability Rule

Operational Topology Runtime은 **explainable** 해야 한다.

**포함:**
- why propagation expanded
- why rollback boundary failed
- why dependency isolation degraded
- why blast radius increased

**금지:** opaque topology inference

---

## 45. Runtime Security Rule

Operational Topology Runtime은 **privileged operational layer**다.

**필수:** authenticated access / RBAC / audit logging / visibility control

**금지:**
- ❌ anonymous topology mutation
- ❌ unrestricted dependency exposure
- ❌ public raw operational topology exposure

---

## 46. Auditability Rule

Topology lifecycle은 **audit 가능**해야 한다.

- what topology discovered
- what dependency validated
- what propagation linked
- what benchmark executed

---

## 47. Immutable Audit Rule

Topology audit는 **append-only** 해야 한다.

**금지:**
- ❌ audit overwrite
- ❌ hidden topology mutation
- ❌ invisible operational override

---

## 48. Runtime Failure Rule

Operational Topology Runtime failure는 **explicit** 해야 한다.

상태: dependency inconsistency / timeline inconsistency / verification unavailable / rollback unavailable

**금지:** silent topology corruption

---

## 49. Visibility Classification Rule

Topology Artifact는 **visibility classification**을 가져야 한다.

`PUBLIC_PORTFOLIO` / `PRIVATE_RESEARCH` / `INTERNAL_OPERATION` / `PAPER_CANDIDATE` / `SANITIZED_EXPORT`

---

## 50. Sanitization Rule

Topology export는 **sanitization 가능**해야 한다.

**제거 대상:** internal topology / customer payload / secret / token / internal IP

---

## 51. Runtime Metrics Governance Rule

Topology metric은 **low-cardinality** 유지해야 한다.

**허용:** service / domain / severity / failure_mode / topology_type

**금지:** customer identifier / payment payload / trace payload dump

---

## 52. Operational Reality Rule

Operational Topology Runtime은 **현실 운영 기반**이어야 한다.

**허용:** real incident / real rollback / real observability / real verification / real propagation

**금지:** toy-only topology / synthetic-only operational graph

---

## 53. Academic Compatibility Rule

Operational Topology Runtime은 **학술 확장 가능**해야 한다.

- topology reproducibility appendix
- experiment reproducibility appendix
- dataset reproducibility appendix
- operational evidence appendix

---

## 54. Research Integrity Rule

Operational Topology Runtime은 **research integrity를 보장**해야 한다.

**금지:**
- fabricated dependency graph
- fabricated propagation topology
- unsupported operational topology conclusion
- hidden contradictory topology

---

## 55. Long-term Operational Topology Evolution Rule

Operational Topology Runtime은 **장기 topology evolution**을 지원 가능해야 한다.

- dependency evolution
- rollback boundary evolution
- verification topology evolution
- propagation containment evolution

**원칙:** Operational topology는 장기 operational learning 기반이어야 한다.

---

## 56. Academic Extension Rule

장기적으로 다음 연구 방향을 지원 가능하다:

- Operational Reliability Topology Systems
- rollback-aware dependency topology
- verification-aware operational topology
- Human-in-the-loop topology governance

---

## 57. Anti-Pattern Rule

**금지:**
- ❌ undocumented dependency graph
- ❌ rollback boundary 없는 topology
- ❌ verification scope 없는 topology
- ❌ opaque dependency inference
- ❌ unsupported propagation graph

---

## 58. Non-Goals

Operational Topology Runtime의 목표는 다음이 **아니다**:

- simple infrastructure diagram
- opaque dependency mapping
- ungoverned topology mutation
- unverifiable operational graph

---

## 59. Canonical Runtime Layers

| Layer | 역할 |
|---|---|
| Service Topology | 서비스 의존 관계 |
| Queue Topology | queue 흐름 |
| Payment Flow Topology | 결제 흐름 |
| Rollback Boundary Topology | rollback 경계 |
| Propagation Topology | 장애 전파 경로 |
| Verification Scope Topology | verification 범위 |

---

## 60. Reliability Research Formalization Direction

현재 방향의 핵심은 단순 dependency diagram이 아니다.

**목표:** 운영 observability와 operational lineage를 설명 가능하고, 재현 가능하며, 정량 검증 가능하고, 논문화 가능한 **Operational Reliability Topology Runtime으로 formalization** 하는 것이다.

---

**한 줄 핵심**

> Runtime Operational Topology의 목적은 단순 인프라 다이어그램이 아니다.
> → dependency, propagation, rollback boundary, verification scope를 topology 기반으로 formalization 하여 재현 가능하고 검증 가능한 **Operational Reliability Topology Runtime**으로 구축하는 것이다.