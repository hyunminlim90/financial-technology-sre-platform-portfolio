# Research Evidence Contract

## 1. 목적

이 문서는 FIN-SRE 플랫폼의 **Research Evidence Governance Layer**를 정의한다.

Research Evidence의 목적은 단순 로그 저장이 아니다.

목적은:

> 운영 이벤트 + 실험 결과 + Rollback/Verification 결과 + Observability Signal

을 기반으로:

- 설명 가능하고
- 검증 가능하며
- 재현 가능하고
- 논문화 가능한

**Operational Reliability Evidence Runtime**을 formalization 하는 것이다.

---

## 2. 핵심 개념

Research Evidence는 단순 raw data가 아니다.

Research Evidence는:

- Evidence-backed
- Context-aware
- Experiment-linked
- Research-governed

**runtime artifact**다.

---

## 3. Canonical Research Evidence Definition

Research Evidence는 다음을 포함 가능.

| Evidence Type | 설명 |
|---|---|
| Metrics Evidence | metrics snapshot |
| Trace Evidence | distributed trace |
| Log Evidence | structured log |
| Timeline Evidence | runtime timeline |
| Rollback Evidence | rollback execution result |
| Verification Evidence | verification result |
| Experiment Evidence | failure injection evidence |
| Policy Evidence | governance/policy result |

---

## 4. Human Governance Rule

Research Evidence는 Human Governance 아래 있어야 한다.

**원칙:**
- AI는 evidence를 분석할 수 있다.
- Human이 evidence validity를 검증한다.

**금지:**
- ❌ fabricated evidence
- ❌ unverifiable evidence mutation
- ❌ AI-only evidence validation

---

## 5. Canonical Evidence Lifecycle

Research Evidence는 canonical lifecycle 가져야 한다.

```
COLLECTED → NORMALIZED → VERIFIED → LINKED → RESEARCH_READY
```

또는:

```
COLLECTED → CORRUPTED → REJECTED
```

---

## 6. Evidence-backed Rule

모든 연구 자산은 Evidence 기반이어야 한다.

**허용:**
- verified metrics
- verified traces
- verified logs
- verified rollback result
- verified verification result

**금지:**
- hallucinated evidence
- invented metric
- fabricated experiment result

---

## 7. Evidence Integrity Rule

Evidence integrity는 최우선이다.

**원칙:**

```
evidence corruption → research invalidation
```

---

## 8. Context-awareness Rule

Evidence는 context-aware 해야 한다.

**포함 가능:**
- service
- environment
- severity
- failure_mode
- timeline
- risk classification

---

## 9. Experiment Linkage Rule

Evidence는 Experiment Runtime과 연결 가능해야 한다.

**예:**
- failure injection
- policy comparison
- rollback validation
- verification validation

---

## 10. Timeline Linkage Rule

Evidence는 timeline replay 가능해야 한다.

**예:**
- alert timeline
- recommendation timeline
- rollback timeline
- verification timeline
- experiment timeline

---

## 11. Replayability Rule

Research Evidence는 replayable 해야 한다.

**예:**
- incident replay
- experiment replay
- rollback replay
- verification replay

---

## 12. Reproducibility Rule

Evidence는 재현 가능해야 한다.

**포함:**
- collection source
- sampling condition
- environment
- time range
- experiment condition

---

## 13. FinTech Safety Rule

FinTech 환경에서는 다음 Evidence가 중요하다.

**포함 가능:**
- duplicate payment detection
- idempotency verification
- settlement consistency evidence
- rollback safety evidence

---

## 14. Systems-Math Integration Rule

Evidence는 Systems-Math 연결 가능해야 한다.

**예:**
- Little's Law
- retry amplification
- tail latency propagation
- queue utilization

**원칙:** Systems-Math는 Evidence 해석 계층이다.

---

## 15. Quantitative Validation Rule

Evidence는 정량 검증 가능해야 한다.

**예:**
- MTTR
- rollback success rate
- verification latency
- propagation reduction
- error recovery latency

---

## 16. Reliability Dataset Rule

Evidence는 dataset accumulation 지원 가능해야 한다.

**예:**
- incident dataset
- rollback dataset
- verification dataset
- policy effectiveness dataset

---

## 17. Policy Evidence Rule

Research Evidence는 policy-aware 해야 한다.

**포함 가능:**
- guardrail decision
- approval result
- risk classification
- policy rejection reason

---

## 18. Rollback Evidence Rule

Rollback Evidence는 canonical structure 가져야 한다.

**포함:**
- rollback trigger
- rollback scope
- rollback execution
- rollback verification
- rollback latency

---

## 19. Verification Evidence Rule

Verification Evidence는 explainable 해야 한다.

**포함:**
- why verification passed
- why verification failed
- what remains degraded
- what evidence supports recovery

---

## 20. Unknown State Rule

Unknown 상태는 restrictive governance 적용.

**예:**
- missing metrics
- partial observability
- timeline inconsistency
- projection lag

**원칙:**

```
Unknown → evidence invalidation
```

---

## 21. Sanitization Rule

Evidence export는 sanitization 가능해야 한다.

**제거 대상:**
- internal IP
- secret
- token
- customer payload
- payment payload
- sensitive topology

---

## 22. Visibility Classification Rule

Research Evidence는 visibility classification 가져야 한다.

**허용:**
- `PUBLIC_PORTFOLIO`
- `PRIVATE_RESEARCH`
- `INTERNAL_OPERATION`
- `PAPER_CANDIDATE`
- `SANITIZED_EXPORT`

---

## 23. Runtime DTO Rule

Research Evidence Runtime은 canonical DTO 가져야 한다.

**예:**
- `ResearchEvidence`
- `EvidenceContext`
- `EvidenceReference`
- `EvidenceValidation`
- `TimelineEvidence`

---

## 24. Runtime Replay Rule

Evidence Runtime은 replayable 해야 한다.

**예:**
- evidence replay
- timeline replay
- rollback replay
- verification replay

---

## 25. Auditability Rule

Evidence lifecycle은 audit 가능해야 한다.

**포함:**
- who collected
- what source used
- what normalization applied
- what validation performed

---

## 26. Immutable Audit Rule

Evidence audit는 append-only 해야 한다.

**금지:**
- ❌ evidence overwrite
- ❌ silent mutation
- ❌ hidden normalization

---

## 27. Runtime Metrics Governance Rule

Evidence metrics는 low-cardinality 유지해야 한다.

**허용:**
- `service`
- `domain`
- `severity`
- `experiment_type`
- `risk_level`

**금지:**
- customer identifier
- payment payload
- full trace payload

---

## 28. Operational Reality Rule

Evidence는 현실 운영 기반이어야 한다.

**허용:**
- real incident
- real rollback
- real observability
- real verification

**금지:** fabricated operational evidence

---

## 29. Runtime Security Rule

Research Evidence Runtime은 privileged governance layer다.

**필수:**
- authenticated access
- RBAC
- audit logging
- visibility control

**금지:**
- ❌ anonymous evidence access
- ❌ unrestricted export
- ❌ public raw evidence exposure

---

## 30. Runtime Failure Rule

Evidence Runtime failure는 explicit 해야 한다.

**예:**
- evidence corruption
- projection inconsistency
- timeline corruption
- verification mismatch

**금지:** silent evidence degradation

---

## 31. Research Compatibility Rule

Research Evidence는 Reliability Research 지원 가능해야 한다.

**예:**
- rollback effectiveness analysis
- Human Approval effectiveness
- guardrail effectiveness
- propagation analysis

---

## 32. Academic Extension Rule

장기적으로 다음 연구 방향 지원 가능.

**예:**
- evidence-aware reliability systems
- verification-aware observability
- rollback-aware incident governance

---

## 33. Cross-document Linkage Rule

Evidence는 Knowledge Set과 연결 가능해야 한다.

**포함:**
- Scenario
- Runbook
- Improvement
- Preventive Design
- Experiment
- Systems-Math
- Postmortem

---

## 34. Research-aware Reliability Runtime Rule

현재 방향의 핵심은 단순 observability 저장이 아니다.

**목표:**

> 운영 이벤트와 observability signal을 설명 가능하고, 재현 가능하며, 정량 검증 가능하고, 논문화 가능한 **Operational Reliability Evidence Runtime**으로 formalization 하는 것이다.

---

## 35. Anti-Pattern Rule

**금지:**
- ❌ fabricated evidence
- ❌ unverifiable metric
- ❌ opaque normalization
- ❌ raw evidence public exposure
- ❌ auditless evidence mutation
- ❌ speculative verification

---

## 36. Non-Goals

Research Evidence Runtime의 목표는 다음이 아니다.

- raw log dumping
- autonomous evidence fabrication
- unverifiable observability generation
- opaque metric mutation

---

## 37. Canonical Runtime Layers

| Layer | 역할 |
|---|---|
| Collection | evidence 수집 |
| Normalization | evidence 정규화 |
| Validation | evidence 검증 |
| Timeline | replay/audit |
| Research | 연구 자산 |
| Dataset | reliability dataset |
| Export | visibility governance |

---

## 38. Reliability Research Formalization Direction

현재 방향의 핵심은 단순 observability aggregation이 아니다.

**목표:**

> 운영 observability signal을 설명 가능하고, 재현 가능하며, 정량 검증 가능하고, 연구 가능한 **Operational Reliability Evidence Asset**으로 formalization 하는 것이다.

---

## 한 줄 핵심

Research Evidence Governance의 목적은 단순 로그 저장이 아니다.

> 운영 observability signal을 **재현 가능하고 검증 가능한 Reliability Research Evidence Runtime**으로 formalization 하는 것이다.