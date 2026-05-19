# protocols/governance-timeline-contract.md

## 1. 목적

이 문서는 AI-SRE 플랫폼에서 사용하는 Governance Timeline의 저장, 조회, 정렬, replay, auditability, projection, operational governance 규칙을 정의한다.

> Governance Timeline은 단순 이벤트 로그가 아니다.
> Governance Timeline은 "운영 의사결정 이력과 안전 거버넌스를 보존하는 append-only governance system" 이다.

---

## 2. 핵심 개념

플랫폼은 모든 운영 판단과 실행 이력을 timeline 기반으로 기록한다.

대상:
- recommendation
- approval
- execution
- rollback
- verification
- postmortem
- experiment

> **원칙:** 운영 이력은 설명 가능하고, 재현 가능하며, audit 가능해야 한다.

---

## 3. Append-only Rule (핵심)

Governance Timeline은 append-only 구조를 따른다.

**허용:**
- ✔ append
- ✔ replay
- ✔ projection rebuild
- ✔ immutable history

**금지:**
- ❌ mutation
- ❌ overwrite
- ❌ delete-based audit
- ❌ hidden rewrite

> **원칙:** 과거 운영 이력은 수정하지 않는다.

---

## 4. Governance Event Classification Rule

모든 Governance Event는 명확한 event type을 가져야 한다.

예:
- `ALERT_RECEIVED`
- `RECOMMENDATION_GENERATED`
- `APPROVAL_GRANTED`
- `APPROVAL_REJECTED`
- `EXECUTION_RECORDED`
- `ROLLBACK_TRIGGERED`
- `ROLLBACK_COMPLETED`
- `VERIFICATION_COMPLETED`
- `POSTMORTEM_GENERATED`
- `POSTMORTEM_APPROVED`
- `EXPERIMENT_EXECUTED`

---

## 5. Ordering Rule

Timeline ordering은 deterministic 해야 한다.

기본 ordering:
```
occurredAt DESC
eventId DESC
```

> **원칙:** 동일 timestamp에서도 stable ordering이 유지되어야 한다.

---

## 6. Cursor Pagination Rule

Timeline query는 cursor 기반 pagination을 사용한다.

지원 방향:
- `NEXT`
- `PREVIOUS`

> **원칙:** cursor pagination은 stable replay semantics를 유지해야 한다.

---

## 7. Projection-backed Query Rule

Timeline 조회는 projection-backed query를 지원할 수 있다.

지원 모드:
- `RUNTIME_FAN_OUT`
- `PROJECTION_BACKED`

> **원칙:** projection query는 runtime semantics와 호환되어야 한다.

---

## 8. Runtime Compatibility Rule

Projection-backed query는 기존 runtime fan-out semantics를 유지해야 한다.

포함 대상:
- ordering
- cursor semantics
- filter semantics
- degraded handling

---

## 9. Projection Health Rule

Projection query path는 health 상태를 제공할 수 있다.

| 상태 | 의미 |
|------|------|
| `HEALTHY` | 정상 |
| `DEGRADED` | 부분 저하 |
| `UNAVAILABLE` | 사용 불가 |

> **원칙:** projection 상태는 operator에게 설명 가능해야 한다.

---

## 10. Runtime Summary Rule

Projection runtime 상태는 operator-facing summary를 제공할 수 있다.

- `NORMAL`
- `DEGRADED_READ_ONLY`
- `ATTENTION_REQUIRED`

---

## 11. Degraded Read Rule

Projection unavailable 상황에서는 degraded read mode를 지원할 수 있다.

예:
- stale projection
- partial replay
- projection lag

> **원칙:** projection failure는 운영 이력을 숨겨서는 안 된다.

---

## 12. Recommendation Governance Rule

Recommendation lifecycle은 timeline에 기록된다.

예:
- recommendation generated
- recommendation updated
- recommendation rejected

> **원칙:** Recommendation은 추적 가능해야 한다.

---

## 13. Approval Governance Rule

Approval lifecycle은 반드시 기록된다.

예:
- approval requested
- approval granted
- approval rejected
- approval expired

> **원칙:** Human approval은 audit 가능해야 한다.

---

## 14. Execution Governance Rule

Execution 결과는 timeline에 기록된다.

예:
- execution started
- execution completed
- execution failed

> **중요:** 플랫폼은 execution을 직접 수행하지 않는다. Human execution result만 기록한다.

---

## 15. Rollback Governance Rule

Rollback lifecycle은 반드시 기록된다.

포함 대상:
- rollback trigger
- rollback execution
- rollback verification
- rollback failure

> **원칙:** Rollback은 recommendation만큼 중요하다.

---

## 16. Verification Governance Rule

Verification 결과는 timeline에 기록된다.

예:
- verification success
- verification failure
- partial recovery
- regression detected

---

## 17. Experiment Governance Rule

Experiment lifecycle은 timeline에 기록될 수 있다.

예:
- experiment started
- experiment rollback
- experiment completed
- experiment verification

---

## 18. Postmortem Governance Rule

Postmortem lifecycle은 governance timeline과 연결된다.

예:
- draft generated
- human approved
- revision created

---

## 19. Auditability Rule

Governance Timeline은 운영 감사 기록을 보존해야 한다.

포함 대상:
- who approved
- who rejected
- who executed
- when verified
- which rollback occurred

---

## 20. Low Cardinality Metrics Rule

Governance metrics는 low-cardinality를 유지해야 한다.

**허용:**
- result
- direction
- status

**금지:**
- eventId
- sourceId
- cursor
- incidentId
- exceptionMessage
- metadata

> **원칙:** metrics cardinality explosion은 금지된다.

---

## 21. Replay Compatibility Rule

Governance Timeline은 replay 가능해야 한다.

예:
- incident replay
- recommendation replay
- timeline reconstruction

---

## 22. Explainability Rule

Timeline은 운영 의사결정 흐름을 설명 가능해야 한다.

예:
- 왜 recommendation이 생성되었는가
- 왜 approval이 거절되었는가
- 왜 rollback이 수행되었는가

---

## 23. Security Rule

Governance Timeline은 내부 전용 시스템이다.

원칙:
- internal-only
- audit-protected
- append-only
- authenticated access

금지:
- ❌ public governance exposure
- ❌ mutable audit history

---

## 24. Research Dataset Rule

Governance Timeline은 Reliability Research dataset으로 사용될 수 있다.

예:
- recommendation accuracy
- rollback effectiveness
- approval latency
- recovery effectiveness
- verification quality

---

## 25. Human-in-the-loop Rule

최종 운영 책임은 Human에게 있다.

> **원칙:** AI Recommendation ≠ Execution

---

## 26. Anti-Pattern Rule

금지:
- ❌ mutable governance history
- ❌ approval bypass
- ❌ unverifiable execution
- ❌ rollback 없는 execution
- ❌ hidden recommendation rewrite
- ❌ audit 삭제

---

## 27. Non-Goals

Governance Timeline은 다음을 목표로 하지 않는다.

- autonomous remediation
- human replacement
- mutable operational history
- uncontrolled infrastructure automation

---

## 28. 핵심 원칙

| 계층 | 역할 |
|------|------|
| Governance Timeline | 운영 이력 |
| Projection Query | 조회 최적화 |
| Recommendation | 운영 판단 |
| Approval | Human Governance |
| Verification | 결과 검증 |
| Rollback | 안전 복구 |
| Human | 최종 책임 |

---

> 🎯 **한 줄 핵심**
>
> Governance Timeline의 목적은 로그 저장이 아니다.
> → 운영 의사결정과 안전 거버넌스를 끝까지 추적 가능하게 만드는 것이다.