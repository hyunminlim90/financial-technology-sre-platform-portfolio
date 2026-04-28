# Kafka Event Design

## 1. 목적

이 문서는 결제 도메인에서 Kafka를 활용한 이벤트 설계를 정의합니다.

Kafka는 다음을 위한 핵심 구성 요소입니다.

- 결제 승인 비동기 처리
- 서비스 간 결합도 감소
- 확장 가능한 처리 구조
- 장애 격리
- 이벤트 기반 아키텍처 구성

---

## 2. Kafka 사용 목적

### 2.1 비동기 결제 승인 처리

```
Payment API
→ Kafka publish
→ Worker consume
→ External Provider 호출
```

### 2.2 트래픽 흡수 (Buffer 역할)

- API burst traffic 대응
- Worker scale-out 가능
- External API 보호

### 2.3 장애 격리

- API와 승인 처리 분리
- External provider 장애 시 API 영향 최소화

---

## 3. Topic 설계

### 3.1 Topic 목록

```
payment.requested
payment.approved
payment.failed
payment.cancelled
payment.dlq
```

### 3.2 Topic 목적

| Topic | 설명 |
|-------|------|
| `payment.requested` | 결제 승인 요청 |
| `payment.approved` | 결제 승인 완료 |
| `payment.failed` | 결제 실패 |
| `payment.cancelled` | 결제 취소 |
| `payment.dlq` | 처리 실패 메시지 |

### 3.3 Partition 전략

**Partition Key:** `paymentId`

**이유**

- 동일 결제 이벤트 순서 보장
- ordering 유지
- consumer 병렬 처리 가능

### 3.4 Replication

```
replication.factor = 3
min.insync.replicas = 2
```

**운영 관점**

- durability 확보
- broker 장애 대비

---

## 4. Event Payload 표준

모든 이벤트는 공통 필드를 포함합니다.

```json
{
  "eventId": "evt_01HWABCDE12345",
  "eventType": "payment.requested",
  "version": "v1",
  "occurredAt": "2026-04-27T10:15:30+09:00",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "source": "payment-api",
  "data": {}
}
```

---

## 5. payment.requested

**목적**

결제 승인 요청 이벤트

**Payload**

```json
{
  "eventId": "evt_01HWABCDE12345",
  "eventType": "payment.requested",
  "version": "v1",
  "occurredAt": "2026-04-27T10:15:30+09:00",
  "traceId": "trace-123",
  "source": "payment-api",
  "data": {
    "paymentId": "pay_01HWABCDE12345",
    "merchantId": "merchant-001",
    "orderId": "order-001",
    "amount": 15000,
    "currency": "KRW",
    "paymentMethod": "CARD"
  }
}
```

**Consumer 동작**

1. 이벤트 수신
2. Payment 상태 → `APPROVING`
3. External Provider 호출
4. 결과에 따라 `approved` / `failed` 이벤트 발행

---

## 6. payment.approved

**목적**

결제 승인 완료 이벤트

**Payload**

```json
{
  "eventId": "evt_approved_01",
  "eventType": "payment.approved",
  "version": "v1",
  "occurredAt": "2026-04-27T10:15:33+09:00",
  "traceId": "trace-123",
  "source": "payment-worker",
  "data": {
    "paymentId": "pay_01HWABCDE12345",
    "merchantId": "merchant-001",
    "approvedAt": "2026-04-27T10:15:33+09:00"
  }
}
```

**활용**

- 후처리 (정산, 알림)
- Webhook
- Analytics

---

## 7. payment.failed

**목적**

결제 실패 이벤트

**Payload**

```json
{
  "eventId": "evt_failed_01",
  "eventType": "payment.failed",
  "version": "v1",
  "occurredAt": "2026-04-27T10:15:35+09:00",
  "traceId": "trace-123",
  "source": "payment-worker",
  "data": {
    "paymentId": "pay_01HWABCDE12345",
    "merchantId": "merchant-001",
    "errorCode": "PROVIDER_TIMEOUT",
    "failureReason": "External API timeout"
  }
}
```

---

## 8. payment.cancelled

**목적**

결제 취소 이벤트

**Payload**

```json
{
  "eventId": "evt_cancelled_01",
  "eventType": "payment.cancelled",
  "version": "v1",
  "occurredAt": "2026-04-27T10:30:00+09:00",
  "traceId": "trace-123",
  "source": "payment-api",
  "data": {
    "paymentId": "pay_01HWABCDE12345",
    "merchantId": "merchant-001",
    "cancelledAt": "2026-04-27T10:30:00+09:00"
  }
}
```

---

## 9. DLQ (Dead Letter Queue)

**목적**

처리 실패 메시지를 별도로 저장합니다.

**DLQ 전송 조건**

- retry 횟수 초과
- non-retryable error
- schema validation 실패
- poison message

**DLQ Payload**

```json
{
  "originalEvent": { "..." : "..." },
  "errorType": "DESERIALIZATION_ERROR",
  "errorMessage": "Invalid schema",
  "failedAt": "2026-04-27T10:16:00+09:00"
}
```

**운영 포인트**

- DLQ message 증가 → 장애 신호
- manual replay 필요

---

## 10. Retry 정책

### Retry 대상

- `TIMEOUT`
- `NETWORK ERROR`
- `TEMPORARY FAILURE`

### Retry 전략

- Exponential Backoff
- max retry = 3

### Retry 간격

```
1s → 5s → 15s
```

### Retry 구현 방식

**Option 1 (초기)**

- Consumer 내부 retry

**Option 2 (확장)**

- retry topic 사용

```
payment.retry.1s
payment.retry.5s
payment.retry.15s
```

---

## 11. Consumer 설계

### 11.1 Consumer Group

```
payment-worker-group
```

### 11.2 병렬 처리

- partition 기반 병렬 처리

### 11.3 Offset Commit

처리 성공 후 commit

> **주의:** commit 전에 실패 시 중복 처리 가능 → idempotency 필요

---

## 12. Consumer Lag 관리

### 정의

```
lag = latest offset - committed offset
```

### 위험 기준

| lag | 상태 |
|-----|------|
| > 1,000 | 경고 |
| > 5,000 | 장애 수준 |

### 대응

- consumer scale-out
- processing 속도 개선
- partition 증가

---

## 13. 장애 시나리오

### 13.1 Consumer Lag 증가

**원인**

- consumer 성능 부족
- downstream 장애
- retry 폭증

### 13.2 Producer 실패

**원인**

- broker 장애
- network issue

### 13.3 DLQ 증가

**원인**

- schema mismatch
- poison message
- bug

### 13.4 Ordering 문제

**원인**

- key 미설정
- partition 변경

---

## 14. Observability

### Metrics

- `kafka.consumer.lag`
- `kafka.consumer.records-consumed`
- `kafka.producer.error.rate`
- `kafka.consumer.error.rate`
- `kafka.dlq.message.count`

### Logs

- `eventId`
- `paymentId`
- `topic`
- `partition`
- `offset`
- `status`
- `error`

### Trace

- API → Kafka publish
- Kafka → Consumer
- Consumer → External Provider

---

## 15. SRE 관점 핵심 포인트

### 반드시 모니터링해야 할 것

- Consumer Lag
- DLQ 증가
- Retry 횟수
- Producer error rate

### 위험 신호

- lag 지속 증가
- DLQ 급증
- retry 폭증
- consumer 재시작 반복

---

## 16. 설계 요약

> 결제 승인 처리를 비동기화하고,  
> 장애를 격리하며,  
> **확장 가능하고 복구 가능한 이벤트 흐름을 만드는 것.**