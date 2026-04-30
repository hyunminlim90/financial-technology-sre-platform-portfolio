# protocols/improvement-authoring-protocol.md

---

## 1. 목적

이 문서는 모든 Improvement 문서를 동일한 구조와 판단 기준으로 작성하기 위한 규칙을 정의한다.

> Improvement는 단순 개선 문서가 아니다.  
> Improvement는 **"AI 판단을 제한하는 안전 제약(Safety Constraint)"** 이다.

---

## 2. 역할 정의 (다른 문서와 차이)

| 문서 | 역할 |
|------|------|
| **Scenario** | 문제 정의 |
| **Runbook** | 기본 대응 전략 |
| **Postmortem** | 실제 장애 경험 |
| **Improvement** | 재발 방지 + 대응 제한 규칙 정의 |

---

## 3. 핵심 개념

Improvement는 다음을 정의한다:

1. 무엇이 잘못되었는가
2. 왜 기존 Runbook이 위험했는가
3. 어떤 조건에서 특정 Action을 제한해야 하는가
4. 시스템을 어떻게 더 안전하게 만들 것인가

---

## 4. 필수 구조

모든 Improvement 문서는 반드시 다음 구조를 포함해야 한다.

1. 개요
2. 문제 상황 (Before)
3. Root Cause 요약
4. 개선 목표
5. 개선 사항 (구체적 변경)
6. 적용 조건 (When to Apply)
7. 제한 규칙 (Constraint Rule)
8. 리스크 (Trade-off)
9. 적용 후 변화 (After)
10. 관측 지표 변화
11. 추가 모니터링
12. 교훈 (Lessons Learned)
13. SRE 핵심 통찰
14. 향후 개선
15. 요약

---

## 5. 핵심 규칙

### 5.1 Constraint Rule (핵심)

Improvement는 반드시 **AI 행동 제한 규칙**을 포함해야 한다.

- 어떤 Action을 금지하는가
- 어떤 조건에서 제한하는가

**예:**

```
Redis timeout 발생 + retry 증가 시

→ scale-out 금지
→ 먼저 downstream 상태 확인
```

### 5.2 Condition Rule (필수)

모든 Improvement는 반드시 **적용 조건**을 정의해야 한다.

> 언제 이 개선이 적용되는가?

**형식:**

```
<failure_mode> + <condition>
```

**예:**

```
redis-timeout + retry_rate 증가
kafka-consumer-lag + consumer_rate 감소
```

### 5.3 Safety Priority Rule

모든 개선은 다음 기준을 따른다:

| 우선순위 | 기준 |
|------|------|
| 1 | 데이터 정합성 (결제 보호) |
| 2 | 시스템 안정성 |
| 3 | 성능 |

### 5.4 Trade-off Rule (필수)

모든 Improvement는 반드시 다음을 명시해야 한다:

- 성능 감소 가능성
- 비용 증가
- latency 증가

### 5.5 No Blind Optimization Rule

- ❌ 단순 성능 개선 금지
- ❌ 이유 없는 튜닝 금지

> 반드시 **문제 → 원인 → 개선** 구조 유지

### 5.6 FinTech Safety Rule (필수)

결제 시스템에서는 반드시 포함:

- duplicate payment 영향
- idempotency 영향
- retry amplification 영향

### 5.7 Observability Rule

개선 사항은 반드시 **측정 가능**해야 한다.

- 어떤 metric이 개선되는가
- 어떤 alert로 검증하는가

### 5.8 Action Structure Rule

```
Improvement 문서의 모든 변경 사항은 반드시 다음 구조를 포함해야 한다.

Action
Expected Effect
Risk
Rollback Plan
Verification
```

이유:

```
Improvement도 결국 "행동"을 정의하는 문서다
→ 실행 가능한 형태여야 한다
```

### 5.9 Rollback Mandatory Rule

```
모든 Improvement는 반드시 Rollback Plan을 포함해야 한다.

Rollback이 없는 개선은 적용 금지
```

이유:

```
개선도 장애를 만들 수 있다
```

### 5.10 Applicability Rule

```
Improvement는 반드시 "적용 여부 판단 기준"을 포함해야 한다.

AI는 다음 순서로 적용 여부를 판단한다:

1. failure_mode 일치 여부
2. 적용 조건 (Condition Rule) 만족 여부
3. severity / impact_scope 고려
4. context (environment, traffic, scope) 일치 여부

위 조건이 충족될 때만 Improvement를 적용한다.
```

### 5.11 Anti-Pattern Rule

```
Improvement는 반드시 "금지해야 할 잘못된 패턴"을 명시할 수 있다.

예:

❌ Redis timeout 발생 시 무조건 scale-out
❌ retry 증가 상황에서 thread pool 확장
❌ latency 문제를 CPU 문제로 오판
```

---

## 6. Front Matter Required Fields (필수)

```yaml
---
title: Redis Timeout Idempotency Hardening
knowledge_type: improvement
domain: redis
failure_mode: redis-timeout
environment: production   # production | staging | dev

severity: SEV-2
impact_scope: partial     # local | partial | global

services:
  - payment-api
  - redis
  - postgresql

related_scenarios:
  - scenarios/redis/timeout.md

related_runbooks:
  - runbooks/redis/timeout.md

related_postmortems: []

related_improvements: []

related_preventive_designs:
  - preventive-designs/redis-timeout-idempotency-fallback.md

tags:
  - redis
  - timeout
  - idempotency
  - retry
  - fallback
  - hardening
---
```

---

## 7. RAG Integration Rule

Improvement는 다음 기준으로 RAG와 연결된다:

- `failure_mode`
- `domain`
- `severity`
- `impact_scope`
- `related_*` 경로
- `tags`

---

## 8. Decision Override Rule (핵심)

Improvement는 Runbook을 override 할 수 있다.

| 출처 | 내용 |
|------|------|
| Runbook | scale-out 가능 |
| Improvement | retry 증가 시 scale-out 금지 |
| **Final** | **scale-out 금지** |

> 더 안전한 규칙이 항상 우선된다.

### 8.1 Conflict Resolution Rule

```
여러 Improvement가 충돌할 경우 다음 우선순위를 따른다:

1. Safety (데이터 보호)
2. 최신 Postmortem 기반 개선
3. severity 높은 개선
4. 더 restrictive한 규칙
```

---

## 9. Naming Rule

```
improvements/<domain>-<failure-mode>-<topic>.md
```

**예:**

```
improvements/redis-timeout-idempotency-hardening.md
improvements/kafka-consumer-lag-rebalance-optimization.md
```

---

## 10. 금지 사항

| 금지 | 이유 |
|------|------|
| ❌ 적용 조건 없는 개선 | Condition Rule 위반 |
| ❌ 제한 규칙 없는 문서 | Constraint Rule 위반 |
| ❌ Root Cause 없는 개선 | 근거 없음 |
| ❌ 측정 불가능한 개선 | Observability 위반 |
| ❌ Runbook 복붙 | 역할 중복 |

---

## 11. 핵심 원칙

> Improvement는 **"개선"** 이 아니라  
> AI의 판단을 제한하는 **"안전 규칙"** 이다.

---

## 🎯 한 줄 핵심

> **Runbook**은 "할 수 있는 것"  
> **Improvement**는 "하면 안 되는 것"