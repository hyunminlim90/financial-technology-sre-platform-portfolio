# Runbook Authoring Protocol

## 1. 목적

이 문서는 모든 Runbook이 동일한 구조와 품질을 유지하도록 하기 위한 작성 규칙을 정의한다.

```text
Runbook은 사람이 읽는 문서가 아니라
AI가 실행 판단을 내리는 기준 문서이다
```

---

## 2. 필수 구조

모든 Runbook은 반드시 다음 구조를 포함해야 한다.

```text
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

```text
Action
Expected Effect
Risk
Rollback Plan
Verification
```

---

## 4. Decision Rule (필수)

모든 Runbook에는 반드시 AI 판단 기준이 포함되어야 한다.

```text
- 언제 어떤 Action을 선택하는가
- 어떤 Action을 금지하는가
```

---

## 5. Sequencing Rule (필수)

여러 Action이 있을 경우 반드시 순서를 정의해야 한다.

```text
Step 1 → Step 2 → Step 3
```

---

## 6. Safety Priority (필수)

모든 판단은 다음 순서를 따른다.

```text
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

```text
SEV-1 → 즉시 완화 (fallback 강제)
SEV-2 → 원인 분석 후 대응
SEV-3 → 관찰 중심 대응
```

---

## 7. Scale-out Rule (필수)

모든 Runbook은 scale-out 조건을 명확히 정의해야 한다.

```text
- 언제 가능
- 언제 금지
```

---

## 8. Observability Rule

모든 판단은 반드시 데이터 기반이어야 한다.

```text
- PromQL 포함
- SQL 포함 (필요 시)
- kubectl / 로그 포함
- Trace 확인 포함
```

---

## 9. FinTech Safety Rule

결제 시스템에서는 반드시 다음을 포함해야 한다.

```text
- duplicate payment 위험
- idempotency 보호
- fallback 전략
```

---

## 10. RAG Integration Rule

Runbook은 다음과 연결되어야 한다.

```text
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

```text
❌ Action만 있는 Runbook
❌ Rollback 없는 대응
❌ Decision Rule 없는 문서
❌ Observability 없는 문서
```

---

## 12. 핵심 원칙

```text
Runbook은 설명 문서가 아니다
Runbook은 “판단 기준”이다
```
