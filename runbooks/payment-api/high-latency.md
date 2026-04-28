# Runbook: Payment API High Latency

## 1. 개요

이 Runbook은 결제 API의 응답 시간이 비정상적으로 증가하는 상황에서  
SRE 또는 운영자가 빠르게 원인을 분석하고 대응하기 위한 절차를 정의합니다.

---

## 2. 증상

다음과 같은 현상이 관측됩니다.

- API 응답 지연 증가
- p95 latency > 300ms
- timeout 증가
- 사용자 결제 실패 증가
- retry 증가

---

## 3. 영향도

- 결제 실패율 증가
- 매출 손실 가능성
- 사용자 경험 저하
- 중복 결제 요청 증가

---

## 4. 즉시 확인 (5분 내)

### 4.1 전체 상태 확인

Grafana Dashboard → Payment API

**확인 항목**

- request rate
- error rate
- latency p95 / p99

### 4.2 서비스 범위 판단

전체 API vs 특정 API 구분

```
GET  /api/v1/payments
POST /api/v1/payments
```

### 4.3 최근 배포 여부

```
ArgoCD → Application History
```

---

## 5. 진단 절차

### Step 1. Trace 분석 (가장 중요)

Jaeger에서 latency 높은 trace 확인 → 가장 오래 걸리는 span 식별

```
postgres.query    → 느림
redis.command     → 느림
external.api.call → 느림
```

### Step 2. DB 확인

**Slow Query 조회**

```sql
SELECT query, mean_time, calls
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;
```

**확인 포인트**

- slow query 존재 여부
- lock 발생 여부
- connection pool 포화 여부

**Connection 상태 조회**

```sql
SELECT count(*) FROM pg_stat_activity;
```
<br/>

### SRE Tip: Connection Pool Saturation 확인 (PromQL 포함)

### 개요

단순 connection 개수보다 중요한 것은 connection을 얻기 위해 대기 중인 요청 수이다.

---

### 확인 지표

```
r2dbc.pool.pending
r2dbc.pool.acquired
db.connections.pending
db.lock.wait
```

---

### 해석

```
pending 증가
→ DB connection pool saturation 가능성 높음

lock.wait 증가
→ connection 자체보다 lock contention으로 인한 대기 가능성 높음
```

---

### PromQL: DB Connection 상태

**현재 acquired connection 수**

```promql
r2dbc_pool_acquired_connections
```

**현재 pending connection 수**

```promql
r2dbc_pool_pending_connections
```

**Connection 대기 발생 비율**

```promql
rate(db_connections_wait_count[1m])
```

### Step 3. Redis 확인

Redis Dashboard에서 다음 항목 확인:

- latency
- connected clients
- blocked clients
- evicted keys

### Step 4. External API 확인

- timeout 증가 여부
- error rate 증가 여부
<br/>

### SRE Tip: Circuit Breaker 상태 확인

### 개요

External Provider 지연 시 Circuit Breaker가 Open 상태인지 확인한다.

---

### 확인 대상

```
Resilience4j Circuit Breaker
Istio outlier detection / circuit breaking
Gateway 503 증가 여부
```

---

### 해석

```
Circuit Open
→ latency는 줄어들 수 있지만 503 / error rate가 증가할 수 있음

Circuit Closed
→ 외부 지연이 그대로 API latency로 전파될 수 있음

Half-Open
→ 일부 요청만 통과하므로 성공률과 latency를 함께 확인해야 함
```

### PromQL: External API Latency

### p95 External API 응답 시간

```promql
histogram_quantile(0.95, sum(rate(external_api_duration_seconds_bucket[5m])) by (le))
```

### External API 에러 발생 비율

```promql
rate(external_api_error_total[1m])
```

### Step 5. Kubernetes 리소스 확인

```bash
kubectl top pod -n payment
```

**확인 포인트**

- CPU saturation
- memory 부족

**Pod 상태 확인**

```bash
kubectl get pods -n payment
kubectl describe pod <pod-name>
```

### Step 6. 로그 확인

```bash
kubectl logs <payment-api-pod>
```

**확인 항목**

- timeout
- connection error
- slow query log

---

### SRE Tip: Latency 증가 시 중복 결제 위험 확인

### 개요

Latency 증가 시 client / gateway retry가 증가할 수 있다.  
이때 Redis idempotency 저장 실패 또는 지연이 함께 발생하면 중복 결제 위험이 커진다.

---

### 확인 지표

```
payment_duplicate_request_total
payment_idempotency_conflict_total
redis.command.latency
redis.timeout.count
```

---

### 확인 포인트

- 동일 `merchantId` + `orderId` 중복 요청 증가 여부
- `Idempotency-Key` 누락 또는 conflict 증가 여부
- Redis 장애로 idempotency check가 지연되는지 여부

## 6. 원인별 대응 방법

### 6.1 DB Slow Query

**증상:** `postgres.query` latency 증가

**대응**

- 인덱스 추가
- query 수정
- 임시 read replica 활용

**긴급 대응**

- 트래픽 제한
- API rate limit 적용

### 6.2 DB Connection Pool 고갈

**증상:** connection pool active = max

**대응**

- pool size 증가
- slow query 제거
- scale-out

### 6.3 Redis Latency 증가

**증상:** `redis.command` latency 증가

**대응**

- Redis 재시작 (필요 시)
- connection 수 감소
- fallback 로직 확인

### 6.4 External API 지연

**증상:** `external.api.call` latency 증가

**대응**

- timeout 설정 확인
- circuit breaker 적용
- fallback 처리

### 6.5 CPU / Memory 부족

**증상:** CPU 100%, OOM 발생

**대응**

```bash
kubectl scale deployment payment-api --replicas=<n>
```

또는 HPA scale-out

### 6.6 Kafka Producer 지연

**증상:** kafka send latency 증가

**대응**

- broker 상태 확인
- network 확인

### 6.7 배포 문제

**증상:** 배포 이후 latency 증가

**대응**

```bash
argocd app rollback <app-name>
```

---

## 7. 즉시 완화 조치 (Mitigation)

- scale-out (pod 증가)
- rate limit 적용
- fallback 활성화
- external API timeout 단축
- rollback

---

## 8. 근본 해결 (Resolution)

- slow query 제거
- connection pool 튜닝
- Redis 성능 개선
- external API retry 전략 개선
- 코드 비동기 처리 개선

---

## 9. 롤백 기준

다음 조건이면 즉시 롤백합니다.

- error rate 증가
- latency 급증
- 특정 버전에서만 발생

---

## 10. 재발 방지

- SLO 기반 alert 설정
- p95 latency alert 강화
- load test 수행
- chaos test 추가
- circuit breaker 도입

---

## 11. 관련 Dashboard

```
Grafana:
  - Payment API Latency
  - DB Connection Pool
  - Redis Latency
  - External API Latency
```

---

## 12. 관련 Alert

- `HighPaymentApiLatency`
- `HighErrorRate`
- `DBConnectionPoolExhaustion`
- `RedisLatencyHigh`
- `ExternalApiTimeoutHigh`

---

## 13. 명령어 요약

```bash
# Pod 리소스 확인
kubectl top pod -n payment

# Pod 상태 확인
kubectl get pods -n payment
kubectl describe pod <pod-name>

# 로그 확인
kubectl logs <payment-api-pod>
```

```sql
-- Slow query 확인
SELECT query, mean_time, calls
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;

-- Connection 수 확인
SELECT count(*) FROM pg_stat_activity;
```

### PromQL 요약: Payment API 핵심 쿼리

### 결제 API Latency

**p95 latency**

```promql
histogram_quantile(0.95, sum(rate(payment_request_duration_seconds_bucket[5m])) by (le))
```

**p99 latency**

```promql
histogram_quantile(0.99, sum(rate(payment_request_duration_seconds_bucket[5m])) by (le))
```

---

### 중복 결제

**중복 요청 발생 비율**

```promql
sum(rate(payment_duplicate_request_total[5m]))
```

**Idempotency conflict 발생 비율**

```promql
sum(rate(payment_idempotency_conflict_total[5m]))
```

---

### DB Connection Pool

**현재 pending connection 수**

```promql
r2dbc_pool_pending_connections
```

---

### External API Latency

**p95 External API 응답 시간**

```promql
histogram_quantile(0.95, sum(rate(external_api_duration_seconds_bucket[5m])) by (le))
```

---

## 14. 타임라인 예시

| 시각 | 행동 |
|------|------|
| 10:00 | Alert 발생 |
| 10:02 | Dashboard 확인 |
| 10:05 | Trace 분석 |
| 10:10 | DB slow query 확인 |
| 10:15 | scale-out |
| 10:20 | latency 정상화 |

---

## 15. 포스트모템 체크리스트

- [ ] root cause 명확한가?
- [ ] detection delay 있었는가?
- [ ] alert 적절했는가?
- [ ] 대응 시간은 적절했는가?
- [ ] 자동화 가능한 부분은 무엇인가?

---

## 16. 요약

**이 Runbook의 핵심 절차**

1. Trace로 병목을 찾는다
2. DB / Redis / External 중 원인을 빠르게 좁힌다
3. 즉시 완화 조치를 수행한다
4. 근본 원인을 제거한다

---

## 17. 핵심 메시지

> 결제 API latency 장애는 단순히 느린 구간을 찾는 문제가 아니라,  
> retry, idempotency, circuit breaker 상태까지 함께 확인해야 하는 장애다.