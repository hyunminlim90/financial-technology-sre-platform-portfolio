# Runbook Authoring Protocol

## 1. 목적

이 문서는 모든 Runbook이 동일한 구조와 품질을 유지하도록 하기 위한 작성 규칙을 정의한다.

```
Runbook은 사람이 읽는 문서가 아니라
AI가 실행 판단을 내리는 기준 문서이다
```

---

## 2. 필수 구조

모든 Runbook은 반드시 다음 구조를 포함해야 한다.

```
1. 개요
2. 증상
3. 영향도
4. 즉시 확인 (Metrics / Logs / Traces)
5. 진단 절차
6. 원인별 대응
7. 즉시 완화 조치
8. Scale-out / Restart 판단 기준
9. Action / Rollback / Verification Plan
10. 롤백 기준
11. 근본 해결
12. 재발 방지
13. Dashboard
14. Alert
15. Query
16. Postmortem 체크리스트
17. 핵심 메시지
```

---

## 3. Action Rule (필수)

모든 대응은 다음 구조를 따라야 한다.

```
Action
Expected Effect
Risk
Rollback Plan
Verification
```

---

## 4. Decision Rule (필수)

모든 Runbook에는 반드시 AI 판단 기준이 포함되어야 한다.

```
- 언제 어떤 Action을 선택하는가
- 어떤 Action을 금지하는가
```

---

## 5. Sequencing Rule (필수)

여러 Action이 있을 경우 반드시 순서를 정의해야 한다.

```
Step 1 → Step 2 → Step 3
```

---

## 6. Safety Priority (필수)

모든 판단은 다음 순서를 따른다.

```
1. Safety (데이터 / 결제 보호)
2. Stability (시스템 보호)
3. Performance (성능)
```

### 6.1 Severity Rule (필수)

모든 Runbook은 장애 Severity 기준을 포함해야 한다.

| Severity | 조건 |
|----------|------|
| SEV-1 | 결제 실패 / 중복 결제 |
| SEV-2 | latency 증가 / 일부 장애 |
| SEV-3 | 부분 기능 장애 |

원칙:

```
SEV-1 → 즉시 완화 (fallback 강제)
SEV-2 → 원인 분석 후 대응
SEV-3 → 관찰 중심 대응
```

---

## 7. Scale-out Rule (필수)

모든 Runbook은 scale-out 조건을 명확히 정의해야 한다.

```
- 언제 가능
- 언제 금지
```

---

## 8. Observability Rule

모든 판단은 반드시 데이터 기반이어야 한다.

```
- PromQL 포함
- SQL 포함 (필요 시)
- kubectl / 로그 포함
- Trace 확인 포함
```

---

## 9. FinTech Safety Rule

결제 시스템에서는 반드시 다음을 포함해야 한다.

```
- duplicate payment 위험
- idempotency 보호
- fallback 전략
```

---

## 10. RAG Integration Rule

Runbook은 다음과 연결되어야 한다.

```
related_scenarios
related_improvements
related_preventive_designs
```

### 10.1 Front Matter Requirement (필수)

모든 Runbook은 반드시 YAML Front Matter를 포함해야 한다.

### 필수 형식

```yaml
---
title: Redis Timeout Runbook
knowledge_type: runbook
domain: redis
failure_mode: redis-timeout

related_scenarios:
  - scenarios/redis/timeout.md

related_improvements:
  - improvements/redis-timeout-idempotency-hardening.md

related_preventive_designs:
  - preventive-designs/redis-timeout-idempotency-fallback.md
---
```

### 원칙

> Front matter가 없는 Runbook은 RAG에서 사용되지 않는다.

---

## 11. 금지 사항

```
❌ Action만 있는 Runbook
❌ Rollback 없는 대응
❌ Decision Rule 없는 문서
❌ Observability 없는 문서
```

---

## 12. 핵심 원칙

```
Runbook은 설명 문서가 아니다
Runbook은 "판단 기준"이다
```

---

## 13. Execution Safety Rule

AI Agent는 모든 Action에 대해 실행 권한을 갖지 않는다.

다음 Action은 반드시 Human 승인 후 실행해야 한다:

- scale-out / scale-in
- retry 정책 변경
- timeout 변경
- circuit breaker 설정 변경
- DB / Redis 설정 변경
- 트래픽 제어 (rate limit 등)

원칙:

```
AI Recommendation ≠ Execution

AI는 "권장"만 한다
실행은 사람이 승인한다
```

---

## 14. Verification Failure Rule

Action 수행 후 Verification이 실패하면 다음을 수행한다.

```
1. 동일 Action 반복 금지
2. Rollback 수행
3. 다음 단계 Action으로 전환
4. 상위 Risk Action 금지
```

원칙:

```
같은 Action을 반복하면 장애는 악화된다
```

---

## 15. Time Constraint Rule

각 Severity에 따라 대응 시간 기준을 정의한다.

| Severity | 대응 시작 | 완화 목표 |
|----------|-----------|----------|
| SEV-1 | 즉시 (1분 내) | 5분 내 |
| SEV-2 | 5분 내 | 15분 내 |
| SEV-3 | 15분 내 | 30분 내 |

원칙:

```
지연된 대응은 장애를 확대시킨다
```

---

## Systems-Math Integration Rule

Runbook은 관련 Systems-Math 문서를 연결할 수 있다.

```yaml
related_systems_math:
  - systems-math/retry-amplification.md
```

Systems-Math는 다음을 설명한다:

- queue saturation
- retry amplification
- tail latency
- failure propagation

Runbook은 **정량적 운영 현상을 고려하여 Action을 선택**해야 한다.

---

## Experiment Integration Rule

Runbook은 관련 Experiment 문서를 연결할 수 있다.

```yaml
related_experiments:
  - experiments/retry-storm-validation.md
```

Experiment 결과는 다음 검증에 사용된다:

- rollback effectiveness
- recovery time
- verification effectiveness
- recommendation safety

---

## Risk Classification Rule

모든 Action은 Risk Level을 정의해야 한다.

```
LOW / MEDIUM / HIGH / CRITICAL
```

**원칙:**

- `HIGH` 이상은 반드시 Human Approval 필요
- `CRITICAL`은 staged rollout 또는 sandbox validation 권장

---

## Idempotency Rule

Runbook은 Action 반복 시 다음 가능성을 고려해야 한다.

- duplicate execution
- retry amplification
- idempotency violation

**동일 Action 반복은 동일 결과를 보장하지 않을 수 있다.**

---

## Evidence Correlation Rule

Runbook 판단은 반드시 다음 간의 correlation 기반이어야 한다.

- metrics / logs / traces / deployment events

**단일 metric만으로 root cause를 확정하지 않는다.**

---

## Degraded Recommendation Rule

partial observability, missing metrics, trace sampling loss, retrieval failure 상황에서는:

AI는 certainty를 낮추고, **degraded recommendation 상태를 명시**해야 한다.

Unknown을 추정으로 대체해서는 안 된다.

---

## Blast Radius Rule

Runbook은 Action의 예상 영향 범위를 설명해야 한다.

```
- local
- partial
- global
```

**원칙:** blast radius가 큰 Action일수록 rollback, verification, human approval 요구사항이 강화된다.

---

## Governance Timeline Rule

다음 이벤트들은 append-only governance timeline으로 기록될 수 있다.

- recommendation / approval / execution result / rollback / verification

**Timeline 용도:** auditability / replay compatibility / operator-facing governance

---

## Non-Goals

Runbook은 다음을 목표로 하지 않는다.

- automatic execution
- kubectl auto mutation
- GitOps direct mutation
- destructive remediation
- human bypass