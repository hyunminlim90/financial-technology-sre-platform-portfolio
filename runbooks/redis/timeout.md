# runbooks/redis/timeout.md

---

title: Redis Timeout Runbook
knowledge_type: runbook
domain: redis
failure_mode: redis-timeout
services:
  - payment-api
  - redis
  - postgresql
related_scenarios:
  - scenarios/redis/timeout.md
related_runbooks: []
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
  - duplicate-payment
  - payment-safety

---

# Runbook: Redis Timeout

## 1. 개요

이 Runbook은 Redis timeout 또는 latency 증가로 인해 Payment API 장애가 발생했을 때, 원인을 진단하고 결제 안전성을 보호하기 위한 대응 절차를 정의한다.

Redis 장애는 단순 캐시 장애가 아니라 다음 위험으로 이어질 수 있다.

```text
Redis timeout
→ Idempotency check 실패
→ client retry 증가
→ 중복 결제 위험 증가
```

---

## 2. 증상

```text
- Payment API latency 증가
- RedisTimeoutException / ReadTimeoutException 증가
- 5xx 증가
- retry 증가
- payment_duplicate_request_total 증가
- payment_idempotency_conflict_total 증가
```

---

## 3. 영향도

| 영역               | 영향          |
| ---------------- | ----------- |
| Payment API      | 요청 지연 / 실패  |
| Idempotency      | 중복 결제 방어 약화 |
| Rate Limit       | 제한 실패 또는 우회 |
| Cache            | DB 부하 증가    |
| Distributed Lock | 동시성 제어 실패   |

---

## 4. 즉시 확인

### 4.1 Redis Latency 확인

```promql
histogram_quantile(0.95, sum(rate(redis_command_duration_seconds_bucket[5m])) by (le))
```

```promql
sum(rate(redis_timeout_total[1m]))
```

확인 포인트:

```text
- Redis latency가 전체적으로 증가했는가?
- 특정 command만 느린가?
- timeout이 지속적으로 증가하는가?
```

---

### 4.2 Payment API 영향 확인

```promql
histogram_quantile(0.95, sum(rate(payment_request_duration_seconds_bucket[5m])) by (le))
```

```promql
sum(rate(http_server_requests_seconds_count{status=~"5.."}[1m]))
```

확인 포인트:

```text
- Redis latency와 API latency 증가 시점이 일치하는가?
- 5xx가 함께 증가하는가?
```

---

### 4.3 중복 결제 위험 확인

```promql
sum(rate(payment_duplicate_request_total[5m]))
```

```promql
sum(rate(payment_idempotency_conflict_total[5m]))
```

확인 포인트:

```text
- 동일 merchantId + orderId 중복 요청이 증가했는가?
- Idempotency-Key conflict가 증가했는가?
- Redis 장애로 인해 idempotency check가 실패했는가?
```

---

## 5. 진단 절차

### Step 1. Redis 장애 범위 확인

```text
전체 Redis 장애인가?
특정 command 장애인가?
특정 service에서만 발생하는가?
```

확인 대상:

```text
- GET / SET latency
- connection timeout
- command timeout
- Redis node restart
```

---

### Step 2. Redis 리소스 확인

```promql
redis_connected_clients
```

```promql
redis_memory_used_bytes
```

```promql
rate(redis_evicted_keys_total[5m])
```

```promql
redis_cpu_usage_percent
```

확인 포인트:

```text
- memory pressure가 있는가?
- eviction이 증가하는가?
- connected clients가 급증했는가?
- CPU saturation이 있는가?
```

---

### Step 3. Connection Pool 상태 확인

```promql
redis_connections_active
```

```promql
redis_connections_pending
```

해석:

```text
pending 증가
→ Redis connection pool saturation 가능성

active 증가 + timeout 증가
→ connection 점유 시간이 길어졌을 가능성
```

---

### Step 4. 네트워크 문제 확인

```bash
kubectl exec -n payment <payment-api-pod> -- ping <redis-service>
```

```bash
kubectl exec -n payment <payment-api-pod> -- nc -vz <redis-service> 6379
```

확인 포인트:

```text
- Redis service DNS 확인
- network latency 증가 여부
- connection refused 여부
```

---

### Step 5. Application 로그 확인

```bash
kubectl logs -n payment deploy/payment-api | grep -i redis
```

확인 키워드:

```text
RedisTimeoutException
ReadTimeoutException
Connection refused
Command timed out
```

필수 식별자:

```text
traceId
requestId
paymentId
merchantId
idempotencyKey
```

---

### Step 6. Trace 확인

Jaeger에서 Redis span을 확인한다.

확인 포인트:

```text
- redis.get span latency
- redis.set span latency
- idempotency check 구간 지연
- API 전체 latency 중 Redis 비중
```

---

### Step 7. DB Fallback 동작 여부 확인

Redis idempotency check가 실패한 경우 DB fallback 또는 unique constraint가 중복 결제를 방어했는지 확인한다.

```sql
SELECT merchant_id, order_id, count(*)
FROM payments
GROUP BY merchant_id, order_id
HAVING count(*) > 1;
```

```sql
SELECT merchant_id, idempotency_key, status, created_at
FROM idempotency_keys
WHERE created_at > now() - interval '30 minutes'
ORDER BY created_at DESC
LIMIT 100;
```

확인 포인트:

```text
- 중복 payment row가 생성되었는가?
- idempotency_keys 상태가 PROCESSING에 오래 머무는가?
- 동일 key로 다른 request_hash가 들어왔는가?
```

---

## 6. 원인별 대응

### 6.1 Redis Latency 증가

대응:

```text
- Redis CPU / memory 확인
- slow command 확인
- timeout 설정 확인
- 불필요한 Redis 호출 감소
```

즉시 완화:

```text
- read cache 의존 기능 degraded mode 적용
- 필수 idempotency check는 DB fallback 사용
```

---

### 6.2 Redis Connection Saturation

대응:

```text
- Redis connection pool size 확인
- timeout / max pending 설정 확인
- connection leak 여부 확인
```

주의:

```text
pool size를 무조건 늘리면 Redis 부하가 더 커질 수 있다.
```

---

### 6.3 Memory Pressure / Eviction

대응:

```text
- TTL 정책 확인
- 큰 key 제거
- eviction policy 확인
- memory limit 조정 검토
```

위험:

```text
idempotency key eviction
→ 중복 결제 방어 약화
```

---

### 6.4 Network Issue

대응:

```text
- Redis service endpoint 확인
- pod 간 네트워크 확인
- node 문제 확인
- service mesh / network policy 확인
```

---

### 6.5 Retry Storm

증상:

```text
Redis timeout
→ application retry
→ Redis 요청 증가
→ timeout 증가
```

대응:

```text
- retry 횟수 제한
- exponential backoff 적용
- timeout 단축
- circuit breaker 검토
```

---

### 6.6 Idempotency Check Failure

대응:

```text
- Redis 실패 시 DB idempotency_keys fallback 확인
- payments unique constraint 확인
- 동일 merchantId + orderId 중복 여부 확인
- 중복 결제 가능 건 즉시 식별
```

관련 문서:

```text
preventive-designs/redis-timeout-idempotency-fallback.md
```

---

## 7. 즉시 완화 조치

우선순위:

```text
1. 중복 결제 위험 확인
2. Redis timeout 범위 확인
3. Payment API degraded mode 검토
4. Redis 의존 기능 중 비필수 기능 우회
5. DB fallback 활성 여부 확인
6. retry 제한 / rate limit 적용
7. 필요 시 배포 rollback
```

---

## 8. Scale-out / Restart 판단 기준

### Payment API scale-out 가능 조건

```text
- Redis가 정상이고 API pod 리소스만 부족한 경우
- Redis timeout이 API pod 일부에서만 발생하는 경우
```

### Scale-out 주의 또는 금지

```text
- Redis CPU / memory 포화
- Redis connection timeout 전체 증가
- retry storm 발생
- DB fallback으로 DB 부하가 증가 중
```

이 경우 scale-out은 Redis와 DB에 더 큰 부하를 줄 수 있다.

---

## 9. Action / Rollback / Verification Plan

AI Agent는 Redis Timeout 대응 권장 시 아래 형식으로 Action, Risk, Rollback, Verification을 함께 제시해야 한다.

| Action | Expected Effect | Risk | Rollback Plan | Verification |
|---|---|---|---|---|
| Redis 의존 비필수 기능 우회 | Redis 부하 감소 | 일부 기능 degraded | 우회 설정 비활성화 | Redis latency p95 정상화, API 5xx 감소 |
| DB idempotency fallback 사용 | 중복 결제 방어 유지 | DB 부하 증가 | Redis 정상화 후 fallback 비율 축소 | duplicate request / conflict 증가 여부 확인 |
| retry 제한 / backoff 강화 | retry storm 완화 | 일시 실패 응답 증가 가능 | 이전 retry 설정 복구 | redis_timeout_total, retry rate 감소 |
| Payment API rate limit 적용 | Redis / DB 보호 | 일부 요청 제한 | rate limit threshold 원복 | API error rate, Redis latency 안정화 |
| 최근 배포 rollback | 신규 코드 영향 제거 | 이전 버전 이슈 재노출 가능 | rollback 전 revision으로 재배포 | Redis timeout, API latency, idempotency conflict 감소 |
| Payment API scale-out | API pod 리소스 부족 완화 | Redis / DB 부하 증폭 가능 | 이전 replica 수로 scale-in | scale-out 후 Redis timeout / DB fallback 부하 증가 여부 확인 |

### 9.1 Action Sequencing Rule

Redis Timeout 장애에서는 다음 순서로 대응한다.

1. 중복 결제 위험 확인
2. Redis timeout 범위 확인
3. DB fallback 정상 동작 여부 확인
4. retry / rate limit 조정
5. Redis 의존 비필수 기능 우회
6. 최근 배포 영향 확인 후 rollback 검토
7. scale-out은 Redis / DB 상태 확인 후 마지막으로 판단

### 9.2 Scale-out Safety Rule

**다음 조건에서는 scale-out을 권장하지 않는다:**

- Redis CPU / memory 포화
- Redis timeout이 전체적으로 증가
- retry storm 발생
- DB fallback으로 DB 부하 증가
- DB connection pool pending 증가

**Scale-out은 다음 조건에서만 검토한다:**

- Redis 자체는 정상
- 일부 payment-api pod 리소스만 부족
- DB fallback 부하가 증가하지 않음
- retry storm 없음

### 9.3 Verification Window

Action 수행 후 최소 2~5분간 다음 지표를 확인한다.

```promql
histogram_quantile(0.95, sum(rate(redis_command_duration_seconds_bucket[5m])) by (le))
```

```promql
sum(rate(redis_timeout_total[1m]))
```

```promql
histogram_quantile(0.95, sum(rate(payment_request_duration_seconds_bucket[5m])) by (le))
```

```promql
sum(rate(payment_duplicate_request_total[5m]))
```

```promql
sum(rate(payment_idempotency_conflict_total[5m]))
```

### 정상화 기준

- Redis timeout 증가 중단
- Payment API latency 안정화
- 5xx 감소
- duplicate request 급증 없음
- idempotency conflict 급증 없음

---

## 10. 롤백 기준

다음 조건이면 최근 배포 rollback을 검토한다.

```text
- 배포 직후 Redis timeout 증가
- 신규 코드에서 Redis 호출 수 증가
- timeout / retry 설정 변경 이후 장애 발생
- idempotency conflict 증가
```

```bash
argocd app rollback <app-name>
```

---

## 11. 근본 해결

```text
- Redis timeout / retry 정책 재설계
- Idempotency DB fallback 강화
- Redis key TTL / eviction 정책 재검토
- Redis connection pool sizing 조정
- 불필요한 Redis 호출 제거
- Redis 장애 시 degraded mode 설계
```

---

## 12. 재발 방지

```text
- Redis latency alert 설정
- Redis timeout alert 설정
- idempotency conflict alert 설정
- duplicate request alert 설정
- Redis eviction alert 설정
- Redis 장애 simulation 수행
- fallback path load test 수행
```

---

## 13. 관련 Dashboard

```text
Grafana:
- Redis Latency
- Redis Timeout
- Redis Connection Pool
- Redis Memory / Eviction
- Payment API Latency
- Duplicate Request
- Idempotency Conflict
```

---

## 14. 관련 Alert

```text
RedisLatencyHigh
RedisTimeoutHigh
RedisConnectionPoolSaturation
RedisEvictionHigh
PaymentDuplicateRequestHigh
PaymentIdempotencyConflictHigh
PaymentApiLatencyHigh
```

---

## 15. Query 요약

### PromQL

```promql
histogram_quantile(0.95, sum(rate(redis_command_duration_seconds_bucket[5m])) by (le))
```

```promql
sum(rate(redis_timeout_total[1m]))
```

```promql
redis_connected_clients
```

```promql
rate(redis_evicted_keys_total[5m])
```

```promql
sum(rate(payment_duplicate_request_total[5m]))
```

```promql
sum(rate(payment_idempotency_conflict_total[5m]))
```

### SQL

```sql
SELECT merchant_id, order_id, count(*)
FROM payments
GROUP BY merchant_id, order_id
HAVING count(*) > 1;
```

```sql
SELECT merchant_id, idempotency_key, status, created_at
FROM idempotency_keys
WHERE created_at > now() - interval '30 minutes'
ORDER BY created_at DESC
LIMIT 100;
```

### kubectl

```bash
kubectl logs -n payment deploy/payment-api | grep -i redis
```

```bash
kubectl exec -n payment <payment-api-pod> -- nc -vz <redis-service> 6379
```

---

## 16. 포스트모템 체크리스트

```text
- Redis timeout이 언제부터 증가했는가?
- API latency와 Redis latency 증가 시점이 일치하는가?
- 중복 결제 위험이 있었는가?
- idempotency fallback이 정상 동작했는가?
- retry가 장애를 증폭했는가?
- Redis eviction이 발생했는가?
- DB fallback으로 DB 부하가 증가했는가?
- alert가 충분히 빨랐는가?
```

---

## 17. 핵심 메시지

> Redis Timeout은 단순 캐시 장애가 아니다.
> 결제 시스템에서는 Idempotency 실패와 중복 결제 위험으로 이어질 수 있다.

> Redis 장애 대응의 핵심은 Redis 복구보다 먼저
> **중복 결제 위험을 차단하는 것**이다.
