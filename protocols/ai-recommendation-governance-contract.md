# protocols/ai-recommendation-governance-contract.md

## 1. 목적

이 문서는 AI-SRE 플랫폼에서 생성되는 모든 AI Recommendation의 생성, 제한, 승인, 검증, rollback, governance 규칙을 정의한다.

> AI Recommendation은 자동 실행 명령이 아니다.
> Recommendation은 "Human-reviewed operational guidance" 이다.

---

## 2. 핵심 개념

플랫폼은 다음 원칙 위에서 동작한다.

```
AI가 추천한다
Human이 승인한다
Human이 실행한다
AI가 결과를 분석한다
```

> **원칙:** AI Recommendation ≠ Execution

---

## 3. Recommendation-only Rule (핵심)

AI는 Recommendation만 생성할 수 있다.

**허용:**
- ✔ analysis
- ✔ recommendation
- ✔ rollback suggestion
- ✔ verification suggestion
- ✔ risk explanation
- ✔ evidence explanation

**금지:**
- ❌ kubectl execution
- ❌ ArgoCD mutation
- ❌ Terraform apply
- ❌ Kubernetes resource mutation
- ❌ DB mutation
- ❌ payment data mutation
- ❌ infrastructure auto-remediation

---

## 4. Human Approval Rule

모든 위험 Action은 Human Approval이 필요하다.

예:
- scale-out
- scale-in
- retry policy change
- timeout adjustment
- traffic shifting
- circuit breaker change
- fallback activation
- rate limiting

> **원칙:** Human approval 없는 운영 변경은 금지된다.

---

## 5. No Scenario → No Action Rule

운영 Recommendation은 반드시 Scenario 기반이어야 한다.

필수 연결:
```
Scenario
  → Runbook
  → Improvement
  → Preventive Design
```

금지:
- ❌ rag/docs-only recommendation
- ❌ LLM-only action generation
- ❌ observability-only mutation

---

## 6. Recommendation Evidence Rule

모든 Recommendation은 evidence-backed 이어야 한다.

필수 Evidence:
- metrics
- logs
- traces
- alerts
- deployment events
- governance events

> **원칙:** Evidence 없는 Recommendation은 생성 금지.

---

## 7. Recommendation Structure Rule

모든 Recommendation은 다음 구조를 포함해야 한다.

- Action
- Expected Effect
- Risk
- Rollback Plan
- Verification
- Approval Requirement
- Confidence
- Evidence

---

## 8. Confidence Rule

Recommendation은 confidence를 포함해야 한다.

| 상태 | 의미 |
|------|------|
| HIGH | 충분한 evidence |
| MEDIUM | 일부 불확실성 |
| LOW | 제한된 evidence |
| DEGRADED | observability 부족 |

> **원칙:** Low confidence Recommendation은 고위험 Action을 생성해서는 안 된다.

---

## 9. Risk Classification Rule

Recommendation은 risk level을 포함해야 한다.

| Risk | 의미 |
|------|------|
| LOW | read-only / safe |
| MEDIUM | reversible |
| HIGH | production impact |
| CRITICAL | payment integrity risk |

---

## 10. Rollback Mandatory Rule

모든 Recommendation은 rollback을 포함해야 한다.

필수:
- rollback steps
- rollback trigger condition
- rollback verification

> **원칙:** Rollback 없는 Recommendation은 금지된다.

---

## 11. Verification Mandatory Rule

모든 Recommendation은 verification plan을 포함해야 한다.

예:
- P99 latency recovery
- error rate normalization
- queue stabilization
- consumer lag reduction

> **원칙:** Verification 없는 Recommendation은 완료 상태가 될 수 없다.

---

## 12. Improvement Override Rule

Improvement는 Recommendation을 제한할 수 있다.

예:
```
Runbook:
  scale-out 가능

Improvement:
  retry amplification 발생 시 scale-out 금지

최종:
  scale-out 금지
```

> **원칙:** 더 restrictive한 안전 규칙이 우선된다.

---

## 13. Preventive Design Priority Rule

Preventive Design은 Recommendation보다 우선된다.

예:
```
fallback architecture 존재
  → emergency scale-out보다 우선
```

---

## 14. Systems-Math Integration Rule

Recommendation은 Systems-Math 설명을 사용할 수 있다.

예:
- queue saturation analysis
- retry amplification reasoning
- tail latency propagation

> **단:** Systems-Math 단독으로 Action을 결정해서는 안 된다.

---

## 15. Experiment Integration Rule

Experiment 결과는 Recommendation validation evidence로 사용될 수 있다.

예:
```
과거 experiment에서:
  retry limit 적용 시
  recovery time 40% 감소
```

---

## 16. Observability Integration Rule

Recommendation은 observability evidence와 연결되어야 한다.

대상:
- metrics
- logs
- traces
- alerts
- SLO
- deployment events

---

## 17. Governance Timeline Rule

다음 Recommendation lifecycle은 governance timeline에 기록될 수 있다.

- recommendation generated
- approval granted
- approval rejected
- execution recorded
- rollback executed
- verification completed

---

## 18. Degraded Recommendation Rule

다음 상황에서는 degraded recommendation 상태를 생성해야 한다.

- missing metrics
- trace sampling loss
- partial observability
- conflicting evidence
- projection unavailable

> **원칙:** Unknown을 추정으로 대체해서는 안 된다.

---

## 19. FinTech Safety Rule

결제 시스템에서는 다음을 최우선으로 한다.

- payment integrity
- idempotency
- duplicate payment prevention
- settlement consistency

> **원칙:** 공격적 자동화보다 결제 안전성이 우선된다.

---

## 20. SLO-aware Recommendation Rule

Recommendation은 SLO 영향을 고려해야 한다.

예:
- availability
- latency
- error budget burn
- recovery time

---

## 21. Explainability Rule

AI는 다음을 설명 가능해야 한다.

- 왜 이 Recommendation이 생성되었는가
- 왜 특정 Action이 금지되었는가
- 왜 rollback이 필요한가

> **원칙:** 설명 불가능한 Recommendation은 위험하다.

---

## 22. Human-in-the-loop Rule

최종 운영 책임은 Human에게 있다.

> **원칙:** AI는 운영자를 대체하지 않는다.

---

## 23. Auditability Rule

Recommendation lifecycle은 audit 가능해야 한다.

포함 대상:
- evidence
- approval
- execution
- rollback
- verification

---

## 24. Anti-Pattern Rule

금지:
- ❌ autonomous remediation
- ❌ rollback 없는 recommendation
- ❌ unverifiable recommendation
- ❌ evidence 없는 recommendation
- ❌ human approval bypass
- ❌ recommendation overwrite

---

## 25. Non-Goals

플랫폼은 다음을 목표로 하지 않는다.

- AGI operator replacement
- autonomous infrastructure operation
- human-free remediation
- uncontrolled AI automation

---

## 26. 핵심 원칙

| 계층 | 역할 |
|------|------|
| Scenario | 문제 정의 |
| Runbook | 대응 전략 |
| Improvement | 제한 |
| Preventive Design | 구조적 제거 |
| Observability | Evidence |
| Governance Timeline | Auditability |
| Human | 최종 승인 |

---

> 🎯 **한 줄 핵심**
>
> AI Recommendation의 목적은 자동 실행이 아니다.
> → 더 안전하고 설명 가능한 운영 판단을 지원하는 것이다.