# protocols/scenario-authoring-protocol.md

---

## 1. 목적

이 문서는 모든 Scenario 문서를 동일한 구조와 판단 기준으로 작성하기 위한 규칙을 정의한다.

> Scenario는 설명 문서가 아니다.  
> Scenario는 **"장애 상황 정의"** 이다.

---

## 2. 역할 정의 (Runbook과 차이)

| 구분 | 역할 |
|------|------|
| **Scenario** | 무엇이 문제인가 정의 |
| **Runbook** | 어떻게 해결할 것인가 정의 |

---

## 3. 필수 구조

모든 Scenario는 반드시 다음 구조를 포함해야 한다.

1. 개요
2. 장애 정의 (Definition)
3. 사용자 영향
4. 시스템 영향 범위
5. 주요 증상 (Metrics / Logs / Traces)
6. 영향 흐름 (Propagation)
7. 원인 후보 (Hypothesis)
8. 탐지 방법 (Alert / PromQL)
9. 진단 흐름 (High-level)
10. 재현 방법 (Simulation)
11. FinTech 리스크
12. SRE 핵심 통찰
13. Runbook 연결
14. 요약

---

## 4. 핵심 규칙 (중요)

### 4.1 Definition Rule (필수)

> 장애는 반드시 **"정량 조건"** 으로 정의한다.

**예:**

- `p95 latency > 300ms` (5분 이상)
- `r2dbc.pool.pending > 0` (10초 이상)
- `kafka lag` 지속 증가 (5분 이상)

### 4.2 Time Condition Rule

> 모든 장애 정의에는 **"지속 시간"** 이 포함되어야 한다.

| 구분 | 의미 |
|------|------|
| 스파이크 | 장애 아님 |
| 지속 | 장애 |

### 4.3 Symptom Rule

> Metrics / Logs / Traces 반드시 포함

### 4.4 Propagation Rule (핵심)

Scenario는 반드시 장애 전파 흐름을 포함해야 한다.

**예:**

```
Redis timeout
→ API latency 증가
→ retry 증가
→ duplicate request 증가
→ DB overload
```

> 이게 없으면 AI는 **"확산 위험"** 을 모른다.

### 4.5 Hypothesis Rule

> 원인은 확정하지 않는다.  
> **"가능한 원인 후보"** 만 제시한다.

### 4.6 Detection Rule

> PromQL / Alert 기준 포함 필수

### 4.7 Diagnosis Rule

> High-level 흐름만 제공 (Runbook 수준 X)

### 4.8 Simulation Rule

> 반드시 재현 방법 포함

테스트 가능한 시나리오만 가치 있음.

### 4.9 FinTech Safety Rule

결제 시스템에서는 반드시 포함:

- duplicate payment 위험
- idempotency 영향
- retry amplification

### 4.10 SRE Insight Rule

> 단순 설명이 아니라 **"해석"** 포함

**예:**

```
Latency 문제 ≠ Lag 문제
```

### 4.11 Severity Rule (필수)

Scenario는 장애 Severity를 정의해야 한다.

기준:

```
SEV-1: 결제 실패 / 중복 결제 / 전체 장애
SEV-2: latency 증가 / 일부 장애
SEV-3: 부분 기능 문제
```

원칙:

```
- Scenario는 기본 Severity를 정의한다
- 실제 Incident에서는 상황에 따라 조정 가능
```

효과:

```
AI가 대응 우선순위 판단 가능
```

### 4.12 Metrics Priority Rule

Scenario는 주요 판단 지표의 우선순위를 정의해야 한다.

예:

```
Kafka Consumer Lag:

1. kafka_consumer_lag (Lag 상태)
2. kafka_consumer_rate (처리량)
3. kafka_producer_rate (유입량)
4. rebalance_latency (재배치 여부)
```

원칙:

```
AI는 우선순위 기준으로 분석해야 한다.
```

각 지표는 다음 질문을 해결하기 위한 것이다:

```
- lag → backlog 존재 여부
- consumer_rate → 처리 병목 여부
- producer_rate → 유입 폭증 여부
- rebalance_latency → 리밸런싱 영향 여부
```

효과:

```
AI가 "지표 해석 방향"을 틀리지 않음
```

---

### 5 Front Matter Required Fields (필수 확장)

모든 Scenario는 다음 필드를 반드시 포함해야 한다.

```
---
title: Redis Timeout Scenario
knowledge_type: scenario
domain: redis
failure_mode: redis-timeout
environment: production

severity: SEV-2
impact_scope: partial   # local | partial | global

services:
  - payment-api
  - redis
  - postgresql

related_runbooks:
  - runbooks/redis/timeout.md

related_postmortems: []

related_improvements:
  - improvements/redis-timeout-idempotency-hardening.md

related_preventive_designs:
  - preventive-designs/redis-timeout-idempotency-fallback.md

tags:
  - redis
  - timeout
  - latency
  - idempotency
---
```

효과:

```
AI가 "얼마나 급한 장애인지" + "영향 범위"를 즉시 판단
```

### 5.1 RAG Integration Rule

Scenario는 반드시 다음 기준으로 RAG와 연결된다:

```
- failure_mode
- domain
- severity
- impact_scope
- related_* 경로
- tags
```

### 5.2 Failure Mode Rule (필수)

모든 Scenario는 하나의 명확한 failure_mode를 가져야 한다.

형식:

<domain>-<failure-type>

예:

```
redis-timeout
db-connection-pool-exhaustion
kafka-consumer-lag
payment-api-high-latency
```

원칙:

```
- 하나의 Scenario는 하나의 failure_mode만 정의한다
- 여러 장애를 섞지 않는다
```

이유:

```
RAG 연결 기준 = failure_mode
```

### 5.3 Context Rule

Scenario는 다음 컨텍스트를 고려해야 한다.

```
- service (payment-api, worker 등)
- environment (prod, staging)
- traffic pattern (spike, steady)
- failure scope (partial, global)
```

원칙:

```
동일 failure_mode라도
컨텍스트에 따라 영향과 판단이 달라질 수 있다
```

이유:

```
예:

Redis timeout (prod)
→ 치명적

Redis timeout (staging)
→ 무시 가능
```

### 5.4 Impact Scope Rule

Scenario는 장애 영향 범위를 명확히 정의해야 한다.

구분:

```
- local (특정 서비스)
- partial (일부 기능)
- global (전체 시스템)
```

원칙:

```
동일 failure_mode라도
impact scope에 따라 대응 전략이 달라진다
```

---

## 6. Naming Rule

```
scenarios/<domain>/<failure-mode>.md
```

**예:**

```
scenarios/redis/timeout.md
scenarios/kafka/consumer-lag.md
scenarios/database/connection-pool-exhaustion.md
```

---

## 7. 금지 사항

| 금지 | 이유 |
|------|------|
| ❌ 원인 확정 | Hypothesis Rule 위반 |
| ❌ 해결 방법 포함 | Runbook 영역 |
| ❌ 정량 조건 없는 장애 정의 | Definition Rule 위반 |
| ❌ propagation 없는 문서 | Propagation Rule 위반 |
| ❌ FinTech 리스크 없는 문서 | FinTech Safety Rule 위반 |

---

## 8. 핵심 원칙

| 문서 | 역할 |
|------|------|
| **Scenario** | 문제 정의 |
| **Runbook** | 해결 전략 |
| **Postmortem** | 현실 |

---

## 🔥 지금 해야 할 것 (중요)

### 1. 이 프로토콜 파일 생성

```
protocols/scenario-authoring-protocol.md
```

### 2. 기존 Scenario 리팩토링 대상

| Scenario | 작업 |
|------|------|
| payment latency | metadata 추가 |
| DB pool | propagation 추가 |
| kafka lag | 구조 정리 |
| redis | 기준으로 삼기 |

---

## 🎯 한 줄 핵심

> Scenario가 흔들리면  
> Runbook도 흔들리고  
> **AI 판단도 틀린다.**