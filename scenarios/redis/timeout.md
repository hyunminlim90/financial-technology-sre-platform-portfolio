# scenarios/redis/timeout.md

---

title: Redis Timeout Scenario
knowledge_type: scenario
domain: redis
failure_mode: redis-timeout
services:

* payment-api
* redis
* postgresql
  related_scenarios: []
  related_runbooks:
* runbooks/redis/timeout.md
  related_postmortems: []
  related_improvements:
* improvements/redis-timeout-idempotency-hardening.md
  related_preventive_designs:
* preventive-designs/redis-timeout-idempotency-fallback.md
  tags:
* redis
* timeout
* latency
* idempotency
* duplicate-payment
* cache-failure

---

# Scenario: Redis Timeout

## 1. 개요

Redis 요청이 지연되거나 timeout이 발생하여 애플리케이션이 정상적으로 응답하지 못하는 장애 상황을 정의한다.

특히 결제 시스템에서는 Redis가 다음 역할을 수행하므로 영향이 크다:

```text
- Idempotency Key 저장
- Rate Limiting
- Cache
- Distributed Lock
```

---

## 2. 장애 정의

다음 조건 중 하나 이상 만족 시 Redis Timeout 장애로 정의한다.

```text
- Redis command latency 급증
- Redis 요청 timeout 발생 증가
- Redis connection pool saturation
- Redis 요청 실패율 증가
```

### 실무 기준 (권장)

```text
redis_command_latency_seconds p95 > 50ms (지속)
OR
redis_timeout_total 증가율 급증
OR
application redis timeout exception 증가
```

---

## 3. 주요 증상

```text
- API latency 증가
- timeout 증가
- 5xx 에러 증가
- retry 증가
```

결제 시스템 특화 증상:

```text
- Idempotency 체크 실패
- 동일 결제 요청 재시도 증가
- 중복 결제 위험 증가
```

---

## 4. 영향 범위

| 영역               | 영향          |
| ---------------- | ----------- |
| Payment API      | 응답 지연 / 실패  |
| Idempotency      | 중복 요청 발생 가능 |
| Rate Limit       | 제한 실패       |
| Cache            | DB 부하 증가    |
| Distributed Lock | 동시성 제어 실패   |

---

## 5. 시스템 영향 흐름

```text
Redis latency 증가
→ API 요청 지연
→ client retry 증가
→ duplicate request 증가
→ Idempotency 실패 시
→ 중복 결제 발생 가능
```

---

## 6. 주요 원인

### 6.1 Redis 자체 성능 문제

```text
- CPU saturation
- Memory pressure
- eviction 발생
```

---

### 6.2 네트워크 문제

```text
- Redis 노드와 애플리케이션 간 latency 증가
- packet loss
- DNS 문제
```

---

### 6.3 Connection 문제

```text
- connection pool 부족
- connection leak
```

---

### 6.4 트래픽 급증

```text
- 요청 폭증
- cache miss 증가
- retry storm
```

---

### 6.5 외부 장애 전파

```text
- DB 장애 → cache miss 증가 → Redis 부하 증가
- External API 장애 → retry 증가 → Redis hit 증가
```

---

## 7. 탐지 방법

### Metrics

```text
- redis_command_latency_seconds
- redis_timeout_total
- redis_connections_active
- redis_cpu_usage
```

### PromQL 예시

```promql
histogram_quantile(0.95, sum(rate(redis_command_duration_seconds_bucket[5m])) by (le))
```

```promql
sum(rate(redis_timeout_total[1m]))
```

---

### Logs

```text
RedisTimeoutException
ReadTimeoutException
```

---

### Traces

```text
- Redis span latency 증가
- API → Redis 구간 지연
```

---

## 8. 재현 방법 (Simulation)

### 패턴 A: Redis latency 증가

```text
tc qdisc 또는 proxy를 이용해 Redis 응답 지연
```

---

### 패턴 B: connection saturation

```text
동시 요청 증가 → connection pool 고갈 유도
```

---

### 패턴 C: retry storm

```text
client retry 증가 → Redis 요청 폭증
```

---

## 9. 중요 리스크 (FinTech 관점)

```text
- Idempotency 실패 → 중복 결제 발생
- Retry amplification → 시스템 전체 부하 증가
- Cache failure → DB overload
```

---

## 10. SRE 관점 핵심 통찰

```text
Redis 장애는 단순 성능 문제가 아니라
"데이터 정합성과 결제 안전성 문제"로 이어질 수 있다
```

```text
Latency 자체보다
Idempotency 실패 여부가 더 중요하다
```

---

## 11. 연관 문서

```text
runbooks/redis/timeout.md
preventive-designs/redis-timeout-idempotency-fallback.md
improvements/redis-timeout-idempotency-hardening.md
```

---

## 12. 요약

```text
Redis Timeout
→ API latency 증가
→ retry 증가
→ Idempotency 실패 가능
→ 중복 결제 위험
```

반드시 “결제 안전성 관점”으로 분석해야 한다.
