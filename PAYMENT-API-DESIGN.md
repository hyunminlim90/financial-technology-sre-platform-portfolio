# Payment API Design

## 1. 목적

이 문서는 FinTech 결제 도메인의 핵심 API 설계를 정의합니다.

목표는 실제 결제 시스템 운영에서 중요한 다음 요소를 함께 고려하는 것입니다.

- 중복 결제 방지
- 결제 상태 관리
- 비동기 승인 처리
- 장애 추적 가능성
- 운영 지표 수집
- 장애 시 Runbook 연결

---

## 2. API 설계 원칙

### 2.1 결제 요청은 멱등해야 한다

결제 요청은 네트워크 timeout, client retry, gateway retry 등으로 중복 호출될 수 있습니다.

따라서 모든 결제 생성 요청은 `Idempotency-Key`를 필수로 요구합니다.

```http
Idempotency-Key: merchant-001-order-20260427-001
```

### 2.2 결제 생성과 승인은 분리한다

API 서버는 결제 요청을 빠르게 수신하고, 실제 외부 승인 처리는 Worker가 비동기로 수행합니다.

```
Payment API
→ PostgreSQL 저장
→ Kafka 이벤트 발행
→ 즉시 응답

Payment Worker
→ Kafka 이벤트 소비
→ External Provider 호출
→ 상태 업데이트
```

### 2.3 모든 요청은 추적 가능해야 한다

모든 API 요청은 다음 식별자를 가져야 합니다.

- `requestId`
- `traceId`
- `paymentId`
- `merchantId`
- `orderId`

---

## 3. API 목록

```
POST /api/v1/payments
GET  /api/v1/payments/{paymentId}
POST /api/v1/payments/{paymentId}/cancel
GET  /api/v1/payments?merchantId={merchantId}&orderId={orderId}
```

---

## 4. 결제 생성 API

### 4.1 Endpoint

```
POST /api/v1/payments
```

### 4.2 Headers

```http
Authorization: Bearer {merchant-api-token}
Idempotency-Key: {unique-request-key}
Content-Type: application/json
X-Request-Id: {request-id}
```

### 4.3 Request Body

```json
{
  "merchantId": "merchant-001",
  "orderId": "order-20260427-001",
  "amount": 15000,
  "currency": "KRW",
  "paymentMethod": "CARD",
  "customer": {
    "customerId": "customer-001",
    "email": "user@example.com"
  },
  "metadata": {
    "productName": "SRE 실전",
    "source": "web"
  }
}
```

### 4.4 Validation

| 필드 | 조건 |
|------|------|
| `merchantId` | 필수 |
| `orderId` | 필수 |
| `amount` | 1 이상 |
| `currency` | KRW |
| `paymentMethod` | CARD / TRANSFER |
| `Idempotency-Key` | 필수 |

### 4.5 Response - 신규 결제 요청

```http
202 Accepted
```

```json
{
  "paymentId": "pay_01HWABCDE12345",
  "merchantId": "merchant-001",
  "orderId": "order-20260427-001",
  "amount": 15000,
  "currency": "KRW",
  "status": "REQUESTED",
  "message": "Payment request accepted",
  "requestedAt": "2026-04-27T10:15:30+09:00"
}
```

### 4.6 Response - 중복 요청

동일한 `Idempotency-Key` 요청이 들어오면 기존 결제 정보를 반환합니다.

```http
200 OK
```

```json
{
  "paymentId": "pay_01HWABCDE12345",
  "merchantId": "merchant-001",
  "orderId": "order-20260427-001",
  "amount": 15000,
  "currency": "KRW",
  "status": "REQUESTED",
  "message": "Duplicate request. Existing payment returned.",
  "requestedAt": "2026-04-27T10:15:30+09:00"
}
```

### 4.7 처리 흐름

1. `X-Request-Id` 생성 또는 검증
2. Merchant 인증
3. `Idempotency-Key` 확인
4. Redis에 key 조회
5. 기존 key가 있으면 기존 payment 반환
6. 신규 요청이면 PostgreSQL transaction 시작
7. Payment 생성
8. PaymentEvent 생성
9. Kafka `payment.requested` 발행
10. Redis에 `Idempotency-Key` 저장
11. `202 Accepted` 반환

---

## 5. 결제 조회 API

### 5.1 Endpoint

```
GET /api/v1/payments/{paymentId}
```

### 5.2 Response

```json
{
  "paymentId": "pay_01HWABCDE12345",
  "merchantId": "merchant-001",
  "orderId": "order-20260427-001",
  "amount": 15000,
  "currency": "KRW",
  "status": "APPROVED",
  "requestedAt": "2026-04-27T10:15:30+09:00",
  "approvedAt": "2026-04-27T10:15:33+09:00",
  "failureReason": null
}
```

### 5.3 상태 코드

| 상황 | HTTP Status |
|------|-------------|
| 정상 조회 | 200 |
| 결제 없음 | 404 |
| 인증 실패 | 401 |
| 권한 없음 | 403 |

---

## 6. 결제 취소 API

### 6.1 Endpoint

```
POST /api/v1/payments/{paymentId}/cancel
```

### 6.2 Request Body

```json
{
  "reason": "CUSTOMER_REQUEST"
}
```

### 6.3 Response

```json
{
  "paymentId": "pay_01HWABCDE12345",
  "status": "CANCELLED",
  "cancelledAt": "2026-04-27T10:30:00+09:00"
}
```

### 6.4 취소 가능 상태

취소 가능: `APPROVED` → `CANCELLED`

취소 불가 상태:

- `REQUESTED`
- `APPROVING`
- `FAILED`
- `CANCELLED`

---

## 7. 결제 상태

### 7.1 상태 정의

| 상태 | 설명 |
|------|------|
| `REQUESTED` | 결제 요청 접수 |
| `APPROVING` | 외부 승인 처리 중 |
| `APPROVED` | 결제 승인 완료 |
| `FAILED` | 결제 실패 |
| `CANCELLED` | 결제 취소 완료 |

### 7.2 상태 전이

```
REQUESTED
   |
   v
APPROVING
   |
   +--> APPROVED
   |
   +--> FAILED

APPROVED
   |
   v
CANCELLED
```

---

## 8. Error Response

### 8.1 공통 에러 형식

```json
{
  "errorCode": "PAYMENT_INVALID_REQUEST",
  "message": "Invalid payment request",
  "requestId": "req_01HWXYZ",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "timestamp": "2026-04-27T10:15:30+09:00"
}
```

### 8.2 주요 에러 코드

| Error Code | HTTP Status | 설명 |
|------------|-------------|------|
| `PAYMENT_INVALID_REQUEST` | 400 | 요청 값 오류 |
| `PAYMENT_DUPLICATE_ORDER` | 409 | 중복 주문 |
| `PAYMENT_NOT_FOUND` | 404 | 결제 없음 |
| `PAYMENT_INVALID_STATUS` | 409 | 상태 전이 불가 |
| `MERCHANT_UNAUTHORIZED` | 401 | 인증 실패 |
| `MERCHANT_FORBIDDEN` | 403 | 권한 없음 |
| `IDEMPOTENCY_KEY_REQUIRED` | 400 | 멱등 키 누락 |
| `IDEMPOTENCY_CONFLICT` | 409 | 같은 키로 다른 요청 |
| `PAYMENT_INTERNAL_ERROR` | 500 | 내부 오류 |
| `PAYMENT_TEMPORARILY_UNAVAILABLE` | 503 | 일시적 장애 |

---

## 9. Idempotency 정책

### 9.1 Key 저장 전략

**Redis Key**

```
idempotency:{merchantId}:{idempotencyKey}
```

**Value**

```json
{
  "paymentId": "pay_01HWABCDE12345",
  "requestHash": "sha256-request-body",
  "status": "REQUESTED",
  "createdAt": "2026-04-27T10:15:30+09:00"
}
```

**TTL:** `24h`

### 9.2 Conflict 처리

동일한 `Idempotency-Key`로 다른 요청 본문이 들어오면 실패 처리합니다.

```http
409 Conflict
```

```json
{
  "errorCode": "IDEMPOTENCY_CONFLICT",
  "message": "Same idempotency key was used with different request body",
  "requestId": "req_01HWXYZ",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "timestamp": "2026-04-27T10:15:30+09:00"
}
```

---

## 10. Kafka 이벤트

### 10.1 payment.requested

**Topic:** `payment.requested`

**Payload**

```json
{
  "eventId": "evt_01HWABCDE12345",
  "eventType": "payment.requested",
  "paymentId": "pay_01HWABCDE12345",
  "merchantId": "merchant-001",
  "orderId": "order-20260427-001",
  "amount": 15000,
  "currency": "KRW",
  "occurredAt": "2026-04-27T10:15:30+09:00",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736"
}
```

### 10.2 Event Key

**Kafka message key:** `paymentId`

**이유**

- 동일 결제의 이벤트 순서 보장
- partition 내 ordering 유지

---

## 11. Database 설계 연결

Payment API는 다음 테이블을 사용합니다.

- `merchants`
- `payments`
- `payment_attempts`
- `payment_events`
- `idempotency_keys`

> 상세 schema는 `DATABASE-SCHEMA.md`에서 정의합니다.

---

## 12. Observability 요구사항

### 12.1 Metrics

- `payment_request_total`
- `payment_success_total`
- `payment_failure_total`
- `payment_duplicate_request_total`
- `payment_idempotency_conflict_total`
- `payment_request_duration_seconds`
- `payment_approval_delay_seconds`

### 12.2 Logs

필수 필드:

- `timestamp`
- `level`
- `service`
- `requestId`
- `traceId`
- `spanId`
- `merchantId`
- `paymentId`
- `orderId`
- `status`
- `errorCode`
- `latencyMs`

### 12.3 Traces

```
POST /api/v1/payments
  ├── merchant.authenticate
  ├── redis.idempotency.get
  ├── postgres.payment.insert
  ├── kafka.payment.requested.publish
  └── redis.idempotency.set
```

---

## 13. 장애 시나리오 연결

이 API 설계는 다음 장애 시나리오와 연결됩니다.

```
scenarios/payment-api/high-latency.md
scenarios/database/connection-pool-exhaustion.md
scenarios/redis/timeout.md
scenarios/kafka/producer-timeout.md
scenarios/deployment/high-error-rate-after-deploy.md
```

---

## 14. 구현 우선순위

**Step 1**

- `POST /api/v1/payments`
- `GET /api/v1/payments/{paymentId}`

**Step 2**

- Redis idempotency
- PostgreSQL 저장
- Kafka `payment.requested` 발행

**Step 3**

- Payment Worker
- External Provider Mock
- 상태 업데이트

**Step 4**

- Metric / Log / Trace 추가

---

## 15. 요약

Payment API의 핵심은 단순히 결제를 생성하는 것이 아닙니다.

> 중복 결제를 방지하면서,  
> 결제 요청을 빠르게 수신하고,  
> 비동기로 승인 처리하며,  
> **장애 발생 시 추적 가능한 구조를 만드는 것.**