# protocols/preventive-design-authoring-protocol.md

---

## 1. 목적

이 문서는 모든 Preventive Design 문서를 동일한 구조와 판단 기준으로 작성하기 위한 규칙을 정의한다.

> Preventive Design은 단순 설계 문서가 아니다.  
> Preventive Design은 **"장애를 구조적으로 제거하는 시스템 설계 규칙"** 이다.

---

## 2. 역할 정의 (다른 문서와 차이)

| 문서 | 역할 |
|------|------|
| **Scenario** | 문제 정의 |
| **Runbook** | 기본 대응 |
| **Improvement** | 행동 제한 |
| **Postmortem** | 실제 경험 |
| **Preventive Design** | 문제를 구조적으로 제거 |

---

## 3. 핵심 개념

Preventive Design은 다음을 정의한다:

1. 왜 이 장애가 구조적으로 발생하는가
2. 기존 구조의 한계는 무엇인가
3. 어떤 설계로 문제를 원천 차단할 수 있는가
4. 어떤 조건에서 이 구조를 도입해야 하는가

---

## 4. 필수 구조

모든 Preventive Design 문서는 반드시 다음 구조를 포함해야 한다.

1. 개요
2. 문제 정의 (Structural Problem)
3. 기존 구조 한계
4. 설계 원칙 (Design Principles)
5. 목표 구조 (Target Architecture)
6. Layer별 설계
7. 처리 흐름 (Flow)
8. 실패 시 동작 (Failure Handling)
9. 적용 조건 (Applicability)
10. 도입 전략 (Migration Strategy)
11. Trade-off
12. Observability
13. 검증 방법 (Verification)
14. SRE 핵심 통찰
15. 요약

---

## 5. 핵심 규칙

### 5.1 Structural Rule (핵심)

Preventive Design은 반드시 **구조 변경**을 포함해야 한다.

| 금지 | 허용 |
|------|------|
| ❌ 단순 튜닝 | ✔ 아키텍처 변경 |
| ❌ 파라미터 변경 | ✔ 데이터 흐름 변경 |
| | ✔ 책임 분리 |

### 5.2 Applicability Rule (필수)

Preventive Design은 반드시 **언제 적용해야 하는지** 정의해야 한다.

AI는 다음 기준으로 적용 여부를 판단한다:

1. `failure_mode` 일치 여부
2. 반복 장애 여부 (Postmortem 기반)
3. severity 수준 (SEV-1 / SEV-2)
4. business risk (결제 / 데이터 정합성)
5. 기존 대응 실패 여부

**예:**

```
redis-timeout + duplicate payment risk 증가 시

형식:

<failure_mode> + <metric condition> + <duration>

예:

redis-timeout 
+ retry_rate > 20% (1분 이상)
+ duplicate_request_detected > 0

→ Preventive Design 적용
```

### 5.3 Replacement Rule

Preventive Design은 기존 구조를 어떻게 대체하는지 명확히 해야 한다.

| 구분 | 내용 |
|------|------|
| 기존 | Redis 단일 idempotency |
| 변경 | Redis + DB + Unique Constraint |

### 5.4 Migration Strategy Rule (필수)

Preventive Design은 반드시 **도입 전략**을 포함해야 한다.

- 단계적 rollout
- dual write
- shadow traffic 검증
- rollback 가능 구조

### 5.5 Trade-off Rule (필수)

모든 설계는 비용을 가진다. 반드시 명시:

- latency 증가
- 비용 증가
- 복잡도 증가

### 5.6 Safety Rule (FinTech 필수)

결제 시스템에서는 반드시 포함:

- duplicate payment 방지
- idempotency 보장
- 데이터 정합성 유지

### 5.7 Failure Handling Rule

> 구조 자체도 실패할 수 있다.

각 layer 실패 시 동작 정의 필수

### 5.8 Observability Rule

설계는 반드시 **측정 가능**해야 한다.

- 어떤 metric으로 검증하는가
- 어떤 alert로 감지하는가

### 5.9 Verification Rule

설계가 실제로 동작하는지 검증 방법 포함:

- chaos test
- load test
- failure injection

### 5.10 Anti-Pattern Rule

잘못된 구조를 명시적으로 금지한다.

- ❌ Redis만으로 idempotency 처리
- ❌ cache에 정합성 의존
- ❌ retry uncontrolled

### 5.11 Runbook Override Rule (필수)

```
Preventive Design은 기존 Runbook을 override 할 수 있다.

출처                     내용
Runbook              scale-out 가능
Preventive Design    fallback 구조 사용 → scale-out 금지
Final                scale-out 금지

원칙:

구조적 해결이 항상 우선된다
```

### 5.12 Adoption Level Rule

Preventive Design은 적용 수준을 정의해야 한다:

```
- optional (권장)
- recommended (강력 권장)
- mandatory (필수)
```

원칙:

```
SEV-1 + 반복 장애 → mandatory
```

---

## 6. Front Matter Required Fields (필수)

```yaml
---
title: Redis Timeout Idempotency Fallback Design
knowledge_type: preventive-design
domain: redis
failure_mode: redis-timeout
environment: production

severity: SEV-1
impact_scope: global

adoption_level: mandatory   # optional | recommended | mandatory

services:
  - payment-api
  - redis
  - postgresql

related_scenarios:
  - scenarios/redis/timeout.md

related_runbooks:
  - runbooks/redis/timeout.md

related_postmortems: []

related_improvements:
  - improvements/redis-timeout-idempotency-hardening.md

related_preventive_designs: []

tags:
  - redis
  - timeout
  - idempotency
  - fallback
  - payment-safety
---
```

---

## 7. RAG Integration Rule

Preventive Design은 다음 기준으로 연결된다:

- `failure_mode`
- `domain`
- `severity`
- `impact_scope`
- `related_*` 경로
- `tags`

---

## 8. Priority Rule (중요)

Preventive Design은 모든 Knowledge Source 중 **최상위 우선순위**를 가진다.

```
Preventive Design > Improvement > Postmortem > Runbook > Scenario
```

> 가장 안전한 설계가 항상 우선된다.

---

## 9. Naming Rule

```
preventive-designs/<domain>-<failure-mode>-<topic>.md
```

**예:**

```
preventive-designs/redis-timeout-idempotency-fallback.md
preventive-designs/kafka-consumer-lag-backpressure-control.md
```

---

## 10. 금지 사항

| 금지 | 이유 |
|------|------|
| ❌ 구조 변경 없는 문서 | Preventive Design 아님 |
| ❌ 적용 조건 없음 | AI 판단 불가 |
| ❌ migration 전략 없음 | 현실 적용 불가 |
| ❌ observability 없음 | 검증 불가 |
| ❌ trade-off 없음 | 위험 판단 불가 |

---

## 11. 핵심 원칙

| 문서 | 역할 |
|------|------|
| **Runbook** | 장애를 "대응"한다 |
| **Improvement** | 행동을 "제한"한다 |
| **Preventive Design** | 장애를 "없앤다" |

---

## 🎯 한 줄 핵심

> 가장 좋은 장애 대응은 대응하지 않는 것이다.  
> → **구조적으로 장애를 제거하라.**

---

## Systems-Math Structural Rule

Preventive Design은 관련 Systems-Math 문서를 연결할 수 있다.

예:

```yaml
related_systems_math:
  - systems-math/retry-amplification.md
```

Systems-Math는:

- queue saturation
- retry amplification
- tail latency
- cascading failure
- backpressure propagation

등의 운영 현상을 정량적으로 설명한다.

```text
Preventive Design은:
정량 분석 기반 구조 변경을 목표로 한다.
```

---

## Experiment Validation Rule

Preventive Design은 관련 Experiment 결과를 연결할 수 있다.

예:

```yaml
related_experiments:
  - experiments/payment-idempotency-fallback-validation.md
```

Experiment 결과는:

- rollback effectiveness
- recovery time
- architecture resilience
- failure isolation effectiveness

평가에 사용된다.

---

## Failure Isolation Rule

Preventive Design은 가능한 경우:

- blast radius reduction
- fault isolation
- dependency isolation
- graceful degradation

전략을 포함해야 한다.

```text
원칙:

부분 장애가 전체 장애로 확산되는 것을 방지한다.
```

---

## Resilience Pattern Rule

Preventive Design은 다음 resilience pattern을 사용할 수 있다.

- backpressure
- circuit breaker
- bulkhead
- fallback
- timeout isolation
- idempotency guard
- retry budget
- dead letter queue

```text
선택 이유와 trade-off를 설명해야 한다.
```

---

## Governance Timeline Rule

다음 이벤트들은 append-only governance timeline으로 기록될 수 있다.

- architecture recommendation
- preventive-design adoption
- migration execution
- rollback
- verification
- resilience validation

Timeline은:

- auditability
- replay compatibility
- architecture governance

용도로 사용된다.

---

## SLO-Aware Rule

Preventive Design은 다음 SLO 영향을 설명해야 한다.

- latency
- availability
- durability
- recovery time
- error budget consumption

```text
구조 변경은:
SLO 기반으로 평가되어야 한다.
```

---

## Degraded Mode Rule

Preventive Design은 dependency failure 상황에서:

- degraded read-only mode
- fallback mode
- partial availability

전략을 정의할 수 있다.

```text
원칙:

완전 장애보다 제한적 서비스 지속을 우선할 수 있다.
```

---

## Research Validation Rule

Preventive Design은 다음 연구 항목 평가에 사용될 수 있다.

- recovery time reduction
- retry amplification reduction
- queue stabilization
- blast radius reduction
- recommendation safety improvement

```text
이 데이터는 Reliability Engineering 연구에 사용될 수 있다.
```

---

## Non-Goals

Preventive Design은 다음을 목표로 하지 않는다.

- uncontrolled automation
- destructive remediation
- human bypass
- performance-only optimization
- architecture without rollback