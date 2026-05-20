# Runtime Knowledge Resolution Contract

`protocols/runtime-knowledge-resolution-contract.md`

---

## 1. 목적

이 문서는 FIN-SRE 플랫폼의 **Runtime Knowledge Resolution Layer**를 정의한다.

Knowledge Resolution의 목적은 단순 RAG retrieval이 아니다.

목적은:

- Scenario
- Runbook
- Improvement
- Preventive Design
- Postmortem
- rag/docs

를 기반으로:

> **충돌 해결 가능하고, 설명 가능하며, 안전 우선이고, 재현 가능하며, 정량 검증 가능한**
> **Operational Reliability Knowledge Resolution Runtime**을 formalization 하는 것이다.

---

## 2. 핵심 개념

Knowledge Resolution은 단순 semantic search가 아니다.

Knowledge Runtime은:

- Priority-aware
- Conflict-aware
- Safety-aware
- Context-aware
- Evidence-aware
- Human-governed

**operational knowledge orchestration runtime**이다.

---

## 3. Canonical Knowledge Definition

Knowledge Runtime은 다음을 포함 가능.

| Knowledge Type | 역할 |
|----------------|------|
| Scenario | 장애 정의 |
| Runbook | 대응 절차 |
| Improvement | 제한 규칙 |
| Preventive Design | 구조적 해결 |
| Postmortem | 실제 경험 |
| rag/docs | 메커니즘 설명 |

---

## 4. Human Governance Rule

Knowledge Runtime은 **Human Governance 제거 금지**.

**원칙:**
- AI는 knowledge resolution을 수행할 수 있다.
- Human이 최종 operational action을 승인한다.

**금지:**
- ❌ autonomous knowledge override
- ❌ AI-only operational mutation
- ❌ unreviewed policy bypass

---

## 5. Canonical Knowledge Lifecycle

Knowledge Runtime은 canonical lifecycle을 가져야 한다.

```
INGESTED
    → INDEXED
    → RESOLVED
    → PRIORITIZED
    → DECISION_READY
```

또는:

```
RESOLVED
    → CONFLICTED
    → BLOCKED
```

---

## 6. Knowledge Priority Rule

Knowledge Runtime은 **strict priority hierarchy**를 따라야 한다.

```
Preventive Design
    > Improvement
    > Postmortem
    > Runbook
    > Scenario
    > rag/docs
```

**원칙:** 가장 restrictive 하고 가장 안전한 knowledge가 우선된다.

---

## 7. Preventive Design Dominance Rule

Preventive Design은 **최상위 authority**를 가진다.

```
Runbook       → scale-out 허용
Preventive Design → fallback architecture 사용 / scale-out 금지
Final         → scale-out 금지
```

---

## 8. Improvement Override Rule

Improvement는 **Runbook override 가능**해야 한다.

```
Runbook       → retry 증가 가능
Improvement   → retry amplification 위험 / retry 증가 금지
Final         → retry 증가 금지
```

---

## 9. Postmortem Correction Rule

Postmortem은 **operational correction signal**을 제공 가능해야 한다.

```
Runbook       → restart 권장
Postmortem    → restart 반복 시 propagation 발생
Final         → restart recommendation downgrade
```

---

## 10. rag/docs Limitation Rule

rag/docs는 **operational authority 가지지 않는다**.

**허용:**
- mechanism explanation
- metric interpretation
- failure analysis support

**금지:**
- direct action generation
- rollback override
- policy override

---

## 11. No Scenario → No Action Rule

**Scenario 없는 action resolution 금지.**

```
No Scenario → No Action
```

---

## 12. Conflict Resolution Rule

Knowledge conflict 발생 시 **restrictive governance** 적용.

```
Runbook     → restart 가능
Improvement → restart propagation 위험
Final       → restart blocked
```

**원칙:** safest resolution wins

---

## 13. Safety-first Rule

Knowledge Runtime은 **safety-first** 해야 한다.

```
Data Consistency
    > Stability
    > Availability
    > Performance
```

---

## 14. Context-awareness Rule

Knowledge Resolution은 **context-aware** 해야 한다.

포함: service, environment, severity, traffic pattern, impact scope

```
same failure_mode
    + different blast radius
    → different resolution
```

---

## 15. Environment-aware Rule

Knowledge Resolution은 **environment-aware** 해야 한다.

분류 예: `production` / `staging` / `sandbox`

**원칙:** production → stricter resolution

---

## 16. Severity-aware Rule

Knowledge Runtime은 **severity-aware** 해야 한다.

분류 예: `SEV-1` / `SEV-2` / `SEV-3`

**원칙:** higher severity → stricter governance

---

## 17. Blast Radius Rule

Knowledge Resolution은 **blast radius awareness**를 가져야 한다.

분류 예: `local` / `partial` / `cross-service` / `global`

**원칙:** blast radius 증가 → more restrictive resolution

---

## 18. Retry Amplification Rule

Knowledge Runtime은 **retry amplification**을 이해 가능해야 한다.

```
retry storm
    → queue saturation
    → DB overload
    → latency propagation
```

---

## 19. Propagation-aware Rule

Knowledge Resolution은 **propagation-aware** 해야 한다.

예:
- dependency cascade
- tail latency propagation
- queue backlog propagation

---

## 20. Rollback-aware Rule

Knowledge Resolution은 **rollback-aware** 해야 한다.

필수 포함:
- rollback requirement
- rollback trigger
- rollback verification
- rollback timeout

**원칙:** `No Rollback → risky resolution blocked`

---

## 21. Verification-aware Rule

Knowledge Resolution은 **verification-aware** 해야 한다.

포함:
- latency recovery validation
- queue stabilization validation
- payment consistency validation

---

## 22. FinTech Safety Rule

FinTech 환경에서는 **payment consistency가 최우선**이다.

**금지:**
- unsafe replay
- duplicate payment risk
- settlement corruption

**허용 가능:**
- idempotent-safe mitigation
- verified fallback
- verified rollback

---

## 23. Evidence-backed Rule

Knowledge Resolution은 **Evidence 기반**이어야 한다.

**허용:**
- metrics
- logs
- traces
- timeline
- verification result
- rollback result

**금지:**
- hallucinated operational context
- fabricated failure state
- unsupported knowledge linkage

---

## 24. Systems-Math Integration Rule

Knowledge Runtime은 **Systems-Math**와 연결 가능해야 한다.

예:
- Little's Law
- retry amplification
- queue utilization
- tail latency propagation

**원칙:** Systems-Math는 knowledge interpretation layer다.

---

## 25. SLO-aware Rule

Knowledge Resolution은 **SLO-aware** 해야 한다.

포함:
- error budget
- availability degradation
- P99 recovery

---

## 26. Quantitative Validation Rule

Knowledge Runtime은 **정량 검증 가능**해야 한다.

예:
- rollback success rate
- verification latency
- propagation reduction
- recommendation precision

---

## 27. Confidence-aware Rule

Knowledge Runtime은 **confidence-awareness**를 가져야 한다.

분류 예: `HIGH_CONFIDENCE` / `MEDIUM_CONFIDENCE` / `LOW_CONFIDENCE` / `UNKNOWN`

**원칙:** `LOW_CONFIDENCE → risky resolution 제한`

---

## 28. Runtime DTO Rule

Knowledge Runtime은 **canonical DTO**를 가져야 한다.

예:
- `KnowledgeContext`
- `KnowledgeResolution`
- `KnowledgeConflict`
- `KnowledgePriority`
- `ResolvedKnowledge`

---

## 29. Timeline Replay Rule

Knowledge lifecycle은 **replay 가능**해야 한다.

예:
- knowledge replay
- policy replay
- rollback replay
- verification replay

---

## 30. Runtime Replay Rule

Knowledge Runtime은 **replayable** 해야 한다.

예:
- incident replay
- knowledge replay
- policy replay
- research replay

---

## 31. Knowledge Explainability Rule

Knowledge Resolution은 **explainable** 해야 한다.

포함:
- why knowledge selected
- why knowledge blocked
- why knowledge overridden
- why restrictive rule applied

**금지:** opaque resolution

---

## 32. Runtime Security Rule

Knowledge Runtime은 **privileged operational layer**다.

**필수:**
- authenticated access
- RBAC
- audit logging
- visibility control

**금지:**
- ❌ anonymous knowledge override
- ❌ unrestricted operational mutation
- ❌ public operational evidence exposure

---

## 33. Auditability Rule

Knowledge lifecycle은 **audit 가능**해야 한다.

포함:
- what knowledge selected
- what policy applied
- what override occurred
- what rollback required

---

## 34. Immutable Audit Rule

Knowledge audit는 **append-only** 해야 한다.

**금지:**
- ❌ audit overwrite
- ❌ hidden knowledge mutation
- ❌ invisible override

---

## 35. Runtime Failure Rule

Knowledge Runtime failure는 **explicit** 해야 한다.

예:
- knowledge conflict unresolved
- policy resolution failure
- verification unavailable
- rollback unavailable

**금지:** silent knowledge degradation

---

## 36. Reliability Dataset Rule

Knowledge Runtime은 **dataset accumulation**을 지원 가능해야 한다.

예:
- knowledge conflict dataset
- rollback dataset
- verification dataset
- policy effectiveness dataset

---

## 37. Research Compatibility Rule

Knowledge Runtime은 **Reliability Research**를 지원 가능해야 한다.

예:
- Human Approval effectiveness
- guardrail effectiveness
- rollback effectiveness
- knowledge conflict analysis

---

## 38. Visibility Classification Rule

Knowledge Artifact는 **visibility classification**을 가져야 한다.

허용:
- `PUBLIC_PORTFOLIO`
- `PRIVATE_RESEARCH`
- `INTERNAL_OPERATION`
- `PAPER_CANDIDATE`
- `SANITIZED_EXPORT`

---

## 39. Sanitization Rule

Knowledge export는 **sanitization 가능**해야 한다.

제거 대상:
- internal topology
- customer payload
- secret
- token
- internal IP

---

## 40. Runtime Metrics Governance Rule

Knowledge metric은 **low-cardinality** 유지해야 한다.

**허용:**
- service
- domain
- severity
- risk_level
- policy_type

**금지:**
- customer identifier
- payment payload
- trace payload dump

---

## 41. Operational Reality Rule

Knowledge Runtime은 **현실 운영 기반**이어야 한다.

**허용:**
- real incident
- real rollback
- real observability
- real verification

**금지:**
- toy-only knowledge resolution
- synthetic-only operational claim

---

## 42. Academic Extension Rule

장기적으로 다음 연구 방향 지원 가능.

예:
- policy-aware knowledge orchestration
- rollback-aware knowledge systems
- verification-aware operational governance
- Human-in-the-loop AI-SRE knowledge runtime

---

## 43. Anti-Pattern Rule

**금지:**
- ❌ rag/docs-only decision
- ❌ rollback 없는 knowledge resolution
- ❌ verification 없는 knowledge resolution
- ❌ opaque override
- ❌ unsupported operational linkage
- ❌ unsafe conflict resolution

---

## 44. Non-Goals

Knowledge Runtime의 목표는 다음이 **아니다**.

- autonomous AGI operations
- ungoverned knowledge override
- opaque semantic search
- unverifiable operational resolution

---

## 45. Canonical Runtime Layers

| Layer | 역할 |
|-------|------|
| Evidence | observability evidence |
| Knowledge | knowledge resolution |
| Policy | governance/policy |
| Guardrail | safety restriction |
| Risk | risk classification |
| Recommendation | 대응 orchestration |
| Rollback | 안전 복구 |
| Verification | 결과 검증 |

---

## 46. Reliability Research Formalization Direction

현재 방향의 핵심은 단순 semantic retrieval system이 아니다.

**목표:**

> 운영 observability와 knowledge hierarchy를
> 설명 가능하고, 재현 가능하며, 정량 검증 가능하고, 논문화 가능한
> **Operational Reliability Knowledge Resolution Runtime으로 formalization** 하는 것이다.

---

## 한 줄 핵심

> Runtime Knowledge Resolution의 목적은 단순 RAG retrieval이 아니다.
> → Scenario, Runbook, Improvement, Preventive Design, Postmortem 간 충돌을 안전 우선으로 해결하는
> **Reliability Knowledge Runtime으로 formalization** 하는 것이다.