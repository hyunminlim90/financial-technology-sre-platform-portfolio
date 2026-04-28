# Runbook: Kafka Consumer Lag

---

## 1. 개요

이 Runbook은 Kafka Consumer Lag이 증가했을 때 원인을 빠르게 진단하고, 결제 이벤트 처리 지연을 완화하기 위한 절차를 정의합니다.

Kafka Consumer Lag은 단순히 Kafka가 느리다는 의미가 아니라, **유입되는 이벤트 속도를 Consumer 처리 속도가 따라가지 못하고 있다는 신호**입니다.

---

## 2. 증상

- `payment.requested` 이벤트 처리 지연
- 결제 승인 상태 업데이트 지연
- 사용자 결제 상태가 `REQUESTED` 또는 `APPROVING`에 오래 머무름
- webhook / 알림 / 정산 이벤트 지연
- Kafka consumer lag 증가
- retry / DLQ 메시지 증가

---

## 3. 영향도

- 결제 승인 결과 반영 지연
- 사용자 문의 증가
- 중복 조회 / 재시도 증가
- downstream 시스템 지연
- 이벤트 처리 순서 지연
- 장애가 장기화되면 정산 / 알림 / 후처리 전체에 영향

---

## 4. 즉시 확인

### 4.1 Consumer Lag 확인

```promql
sum(kafka_consumer_lag) by (group, topic)
max(kafka_consumer_records_lag_max) by (group, topic)
```

**확인 포인트:**

- 특정 topic만 증가하는가?
- 특정 consumer group만 증가하는가?
- lag이 감소하지 않고 계속 증가하는가?

### 4.2 유입량과 처리량 비교

```promql
sum(rate(kafka_producer_record_send_total[1m])) by (topic)
sum(rate(kafka_consumer_records_consumed_total[1m])) by (topic, group)
```

**해석:**

- `producer rate > consumer rate` → backlog 증가
- `consumer rate`가 0에 가까움 → consumer 장애 또는 poison message 가능성

### 4.3 Worker 상태 확인

```bash
kubectl get pods -n payment
kubectl top pod -n payment
kubectl logs -n payment deploy/payment-worker
```

**확인 포인트:**

- worker pod 재시작 여부
- CPU / memory saturation
- exception 반복 여부
- external API timeout 증가 여부

---

## 5. 진단 절차

### Step 1. Lag 범위 확인

- 전체 topic 문제인가?
- 특정 topic 문제인가?
- 특정 partition 문제인가?
- 특정 consumer group 문제인가?

**Partition별 lag 확인:**

```promql
sum(kafka_consumer_lag) by (topic, partition)
```

**해석:**

- 특정 partition만 lag 증가 → key skew / partition imbalance 가능성
- 모든 partition lag 증가 → consumer 처리 성능 부족 또는 downstream 병목 가능성

### Step 2. Consumer 정상 동작 확인

```bash
kubectl get pods -n payment -l app=payment-worker
kubectl describe pod -n payment <payment-worker-pod>
kubectl logs -n payment <payment-worker-pod>
```

**확인:**

- `CrashLoopBackOff`
- `OOMKilled`
- readiness 실패
- retry loop
- deserialization error
- timeout error

### Step 3. Downstream 병목 확인

Kafka lag의 가장 흔한 원인은 Kafka 자체가 아니라 Consumer가 호출하는 downstream 지연입니다.

**확인 대상:**

- PostgreSQL write latency
- External Payment Provider latency
- Redis latency

**External Provider latency:**

```promql
histogram_quantile(0.95, sum(rate(external_api_duration_seconds_bucket[5m])) by (le))
```

**External Provider error:**

```promql
sum(rate(external_api_error_total[1m]))
```

**DB write latency:**

```promql
histogram_quantile(0.95, sum(rate(db_query_duration_seconds_bucket[5m])) by (le))
```

### Step 4. Retry 폭증 여부 확인

```promql
sum(rate(payment_worker_retry_total[1m])) by (reason)
sum(rate(payment_worker_error_total[1m])) by (errorCode)
```

**해석:**

- retry 증가 + lag 증가 → 실패 이벤트가 재처리되면서 backlog를 증폭시키는 상황

> **주의:** 무제한 retry는 lag 장애를 더 키운다. retry는 backoff + max retry + DLQ와 함께 설계되어야 한다.

### Step 5. DLQ 증가 확인

```promql
sum(rate(kafka_dlq_message_total[5m])) by (topic)
```

**확인:**

- schema validation 실패
- poison message
- non-retryable error
- deserialization error

DLQ가 증가한다면 처리 실패 메시지를 샘플링해서 원인을 확인합니다.

```bash
kubectl logs -n payment deploy/payment-worker | grep DLQ
```

### Step 6. Poison Message 여부 확인

**징후:**

- consumer rate가 거의 0
- 같은 error log가 반복
- 특정 offset에서 진행 멈춤
- lag이 줄지 않음

**확인:**

- topic / partition / offset
- eventId / paymentId
- errorCode

**대응:**

- 문제 메시지를 DLQ로 이동
- offset skip은 최후 수단
- 반드시 eventId / paymentId 기록

### Step 7. Consumer Group / Rebalance 확인

**확인 포인트:**

- consumer rebalance 반복
- pod restart 반복
- session timeout
- heartbeat 실패

**로그 예시:**

```
Rebalance in progress
Member removed from group
Heartbeat failed
```

**대응:**

- `max.poll.interval.ms` 확인
- `session.timeout.ms` 확인
- 처리 시간이 poll interval보다 긴지 확인
- long processing은 별도 worker pool로 분리

---

## 6. 원인별 대응

### 6.1 Consumer 처리 속도 부족

**증상:**

- `consumer rate < producer rate`
- CPU 높음
- lag 지속 증가

**대응:**

- `payment-worker` scale-out
- partition 수 확인
- 처리 로직 최적화

```bash
kubectl scale deployment payment-worker -n payment --replicas=<n>
```

> **주의:** consumer 수는 partition 수보다 많아도 추가 병렬성이 생기지 않는다.

### 6.2 Partition 수 부족

**증상:**

- consumer pod를 늘려도 lag 감소 없음
- consumer 수 > partition 수

**대응:**

- topic partition 증가 검토
- partition key 재검토

> **주의:** partition 증가는 ordering과 key distribution에 영향을 줄 수 있다. paymentId 기준 ordering이 필요한 이벤트는 partition key 변경에 주의한다.

### 6.3 Downstream 지연

**증상:**

- `external.api.call` latency 증가
- DB write latency 증가
- consumer 처리 시간 증가

**대응:**

- external provider timeout 단축
- circuit breaker 확인
- DB slow query 개선
- worker concurrency 제한

> **중요:** Downstream이 이미 포화 상태라면 consumer scale-out은 장애를 악화시킬 수 있다.

### 6.4 Retry 폭증

**증상:**

- `retry_total` 증가
- lag 증가
- DLQ 증가 가능

**대응:**

- exponential backoff 적용
- max retry 제한
- retryable / non-retryable error 분리
- DLQ 전송

**권장:**

- `TIMEOUT` / `NETWORK_ERROR` → retry
- `VALIDATION_ERROR` / `INVALID_STATUS` → DLQ

### 6.5 Poison Message

**증상:**

- 특정 offset에서 처리 반복 실패
- consumer 진행 멈춤

**대응:**

1. 문제 메시지 eventId / paymentId 기록
2. DLQ로 이동
3. 원인 분석
4. 필요 시 replay

> **주의:** offset skip은 데이터 유실 위험이 있으므로 승인 절차 후 수행한다.

### 6.6 Worker Pod 장애

**증상:**

- `CrashLoopBackOff`
- `OOMKilled`
- pod restart 증가

**대응:**

```bash
kubectl describe pod -n payment <pod>
kubectl logs -n payment <pod> --previous
```

**완화:**

- memory limit 조정
- batch size 조정
- max poll records 조정

---

## 7. 즉시 완화 조치

**우선순위:**

1. 원인 범위 확인
2. downstream 포화 여부 확인
3. 안전한 경우 worker scale-out
4. retry 폭증 시 backoff / DLQ 전환
5. poison message 격리
6. 필요 시 producer rate limit

---

## 8. Scale-out 판단 기준

| 구분 | 조건 |
|------|------|
| **Scale-out 가능** | consumer CPU 부족, downstream 여유 있음, partition 수 충분함, retry 폭증 없음 |
| **Scale-out 금지 또는 주의** | external provider timeout 증가, DB write latency 증가, DB connection pool pending 증가, retry 폭증, DLQ 급증 |

**이 경우 먼저 해야 할 일:**

- traffic shedding
- retry 제한
- circuit breaker 확인
- downstream 보호

---

## 9. 결제 도메인 관점 확인

Kafka lag이 증가하면 결제 상태가 오래 머무를 수 있습니다.

**확인 SQL:**

```sql
SELECT status, count(*)
FROM payments
GROUP BY status;
```

**오래된 REQUESTED / APPROVING 결제 확인:**

```sql
SELECT payment_id, merchant_id, order_id, status, created_at, updated_at
FROM payments
WHERE status IN ('REQUESTED', 'APPROVING')
  AND created_at < now() - interval '5 minutes'
ORDER BY created_at ASC
LIMIT 100;
```

**확인 포인트:**

- 특정 merchant에 집중되는가?
- 특정 paymentId에서 반복 실패하는가?
- APPROVING 상태가 장시간 유지되는가?

---

## 10. Trace / Log Correlation

Consumer lag 장애에서도 eventId, paymentId, traceId 연결이 중요합니다.

**로그 필수 필드:**

- `topic` / `partition` / `offset`
- `eventId` / `paymentId`
- `traceId`
- `consumerGroup`
- `retryCount`
- `errorCode`

**분석 흐름:**

1. lag 증가 topic 확인
2. 문제 partition / offset 확인
3. 로그에서 eventId / paymentId 확인
4. traceId로 worker 처리 구간 추적
5. payment_attempts / payments 상태 확인

---

## 11. 근본 해결

- consumer 처리 로직 최적화
- downstream timeout / circuit breaker 정리
- retry 정책 개선
- DLQ / replay 프로세스 정립
- partition key / partition 수 재검토
- worker resource request / limit 조정
- batch size / concurrency 튜닝

---

## 12. 재발 방지

- consumer lag alert 설정
- lag 증가율 alert 추가
- DLQ 증가 alert 추가
- retry rate alert 추가
- producer rate vs consumer rate dashboard 구성
- poison message replay 절차 문서화
- load test / failure injection 수행

---

## 13. 관련 Dashboard

**Grafana:**

- Kafka Consumer Lag
- Producer Rate
- Consumer Rate
- Worker CPU / Memory
- External Provider Latency
- DB Write Latency
- Retry / DLQ

---

## 14. 관련 Alert

- `KafkaConsumerLagHigh`
- `KafkaConsumerLagIncreasing`
- `KafkaConsumerStopped`
- `KafkaDLQMessageHigh`
- `PaymentWorkerRetryHigh`
- `ExternalProviderLatencyHigh`
- `PaymentApprovalDelayHigh`

---

## 15. 명령어 / Query 요약

### PromQL

```promql
sum(kafka_consumer_lag) by (group, topic)
max(kafka_consumer_records_lag_max) by (group, topic)
sum(rate(kafka_consumer_records_consumed_total[1m])) by (group, topic)
sum(rate(payment_worker_retry_total[1m])) by (reason)
sum(rate(kafka_dlq_message_total[5m])) by (topic)
histogram_quantile(0.95, sum(rate(external_api_duration_seconds_bucket[5m])) by (le))
```

### kubectl

```bash
kubectl get pods -n payment -l app=payment-worker
kubectl top pod -n payment
kubectl logs -n payment deploy/payment-worker
kubectl describe pod -n payment <payment-worker-pod>
```

### SQL

```sql
SELECT status, count(*)
FROM payments
GROUP BY status;

SELECT payment_id, merchant_id, order_id, status, created_at, updated_at
FROM payments
WHERE status IN ('REQUESTED', 'APPROVING')
  AND created_at < now() - interval '5 minutes'
ORDER BY created_at ASC
LIMIT 100;
```

---

## 16. 타임라인 예시

| 시각 | 내용 |
|------|------|
| 10:00 | `KafkaConsumerLagHigh` alert 발생 |
| 10:03 | Grafana에서 `payment.requested` lag 증가 확인 |
| 10:05 | consumer rate가 producer rate보다 낮은 것 확인 |
| 10:08 | worker 로그에서 external provider timeout 증가 확인 |
| 10:12 | retry 폭증 확인 |
| 10:15 | retry backoff 강화 및 DLQ 전환 |
| 10:20 | lag 증가 멈춤 |
| 10:35 | backlog 감소 시작 |

---

## 17. 포스트모템 체크리스트

- [ ] lag 증가를 언제 탐지했는가?
- [ ] producer rate와 consumer rate 차이를 확인했는가?
- [ ] downstream 병목을 확인했는가?
- [ ] retry가 장애를 증폭시켰는가?
- [ ] DLQ 전환 기준은 적절했는가?
- [ ] poison message 처리 절차는 안전했는가?
- [ ] 결제 상태 지연 영향 범위는 파악했는가?
- [ ] replay가 필요한 메시지가 있는가?

---

## 18. 핵심 메시지

> Kafka Consumer Lag은 Kafka 자체의 문제가 아니라,
> Consumer 처리 속도, downstream 지연, retry 폭증, poison message가 드러나는 **결과 지표**다.
>
> Lag 대응의 핵심은 consumer를 무조건 늘리는 것이 아니라,
> **유입 속도와 처리 속도, downstream 상태를 함께 비교**하는 것이다.