# Database Schema

## 1. 목적

이 문서는 결제 도메인의 PostgreSQL schema를 정의합니다.

목표는 단순 저장이 아니라 다음을 보장하는 것입니다.

- 결제 상태 정합성
- 중복 결제 방지
- 외부 승인 시도 이력 보관
- 이벤트 발행 이력 관리
- 장애 분석 가능성 확보

---

## 2. 테이블 목록

```
merchants
payments
payment_attempts
payment_events
idempotency_keys
```

---

## 3. merchants

**역할**

가맹점 정보를 저장합니다.

```sql
CREATE TABLE merchants (
    merchant_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    api_key_hash VARCHAR(255) NOT NULL,
    callback_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

**주요 제약**

```sql
ALTER TABLE merchants
ADD CONSTRAINT merchants_status_check
CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'));
```

---

## 4. payments

**역할**

결제 트랜잭션의 중심 테이블입니다.

```sql
CREATE TABLE payments (
    payment_id VARCHAR(64) PRIMARY KEY,
    merchant_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(128) NOT NULL,
    amount BIGINT NOT NULL,
    currency VARCHAR(8) NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    failure_reason VARCHAR(500),
    idempotency_key VARCHAR(255) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    approved_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_payments_merchant
        FOREIGN KEY (merchant_id)
        REFERENCES merchants (merchant_id)
);
```

**제약 조건**

```sql
ALTER TABLE payments
ADD CONSTRAINT payments_status_check
CHECK (status IN (
    'REQUESTED',
    'APPROVING',
    'APPROVED',
    'FAILED',
    'CANCELLED'
));

ALTER TABLE payments
ADD CONSTRAINT payments_amount_check
CHECK (amount > 0);

ALTER TABLE payments
ADD CONSTRAINT payments_currency_check
CHECK (currency IN ('KRW'));

ALTER TABLE payments
ADD CONSTRAINT payments_payment_method_check
CHECK (payment_method IN ('CARD', 'TRANSFER'));
```

**중복 결제 방지 인덱스**

```sql
CREATE UNIQUE INDEX ux_payments_merchant_order
ON payments (merchant_id, order_id);

CREATE UNIQUE INDEX ux_payments_merchant_idempotency
ON payments (merchant_id, idempotency_key);
```

**조회 인덱스**

```sql
CREATE INDEX ix_payments_merchant_created_at
ON payments (merchant_id, created_at DESC);

CREATE INDEX ix_payments_status_created_at
ON payments (status, created_at DESC);
```

---

## 5. payment_attempts

**역할**

외부 결제 승인 API 호출 이력을 저장합니다. 장애 분석 시 매우 중요합니다.

```sql
CREATE TABLE payment_attempts (
    attempt_id VARCHAR(64) PRIMARY KEY,
    payment_id VARCHAR(64) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    latency_ms BIGINT,
    error_code VARCHAR(128),
    request_payload JSONB,
    response_payload JSONB,
    attempted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_payment_attempts_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments (payment_id)
);
```

**제약 조건**

```sql
ALTER TABLE payment_attempts
ADD CONSTRAINT payment_attempts_status_check
CHECK (status IN (
    'SUCCESS',
    'FAILED',
    'TIMEOUT',
    'RETRYABLE_ERROR',
    'NON_RETRYABLE_ERROR'
));
```

**인덱스**

```sql
CREATE INDEX ix_payment_attempts_payment_id
ON payment_attempts (payment_id);

CREATE INDEX ix_payment_attempts_status_attempted_at
ON payment_attempts (status, attempted_at DESC);

CREATE INDEX ix_payment_attempts_provider_attempted_at
ON payment_attempts (provider, attempted_at DESC);
```

---

## 6. payment_events

**역할**

결제 상태 변경 이벤트와 Kafka 발행 상태를 저장합니다. Outbox 패턴으로 확장할 수 있습니다.

```sql
CREATE TABLE payment_events (
    event_id VARCHAR(64) PRIMARY KEY,
    payment_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    published BOOLEAN NOT NULL DEFAULT false,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_payment_events_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments (payment_id)
);
```

**이벤트 타입**

```sql
ALTER TABLE payment_events
ADD CONSTRAINT payment_events_event_type_check
CHECK (event_type IN (
    'payment.requested',
    'payment.approving',
    'payment.approved',
    'payment.failed',
    'payment.cancelled'
));
```

**인덱스**

```sql
CREATE INDEX ix_payment_events_payment_id
ON payment_events (payment_id);

CREATE INDEX ix_payment_events_published_created_at
ON payment_events (published, created_at);

CREATE INDEX ix_payment_events_event_type_created_at
ON payment_events (event_type, created_at DESC);
```

---

## 7. idempotency_keys

**역할**

Redis 장애 또는 TTL 만료 상황에서도 DB 레벨에서 중복 결제를 방어하기 위한 테이블입니다.

```sql
CREATE TABLE idempotency_keys (
    idempotency_key VARCHAR(255) NOT NULL,
    merchant_id VARCHAR(64) NOT NULL,
    payment_id VARCHAR(64),
    request_hash VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (merchant_id, idempotency_key),

    CONSTRAINT fk_idempotency_keys_merchant
        FOREIGN KEY (merchant_id)
        REFERENCES merchants (merchant_id),

    CONSTRAINT fk_idempotency_keys_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments (payment_id)
);
```

**제약 조건**

```sql
ALTER TABLE idempotency_keys
ADD CONSTRAINT idempotency_keys_status_check
CHECK (status IN (
    'PROCESSING',
    'COMPLETED',
    'FAILED'
));
```

**인덱스**

```sql
CREATE INDEX ix_idempotency_keys_expires_at
ON idempotency_keys (expires_at);

CREATE INDEX ix_idempotency_keys_payment_id
ON idempotency_keys (payment_id);
```

---

## 8. 상태 전이 제어

애플리케이션 레벨에서 상태 전이를 제어합니다.

**허용 상태 전이**

```
REQUESTED  → APPROVING
APPROVING  → APPROVED
APPROVING  → FAILED
APPROVED   → CANCELLED
```

**허용하지 않는 상태 전이**

```
FAILED     → APPROVED
CANCELLED  → APPROVED
APPROVED   → REQUESTED
```

---

## 9. 트랜잭션 설계

### 결제 생성 트랜잭션

```
1. idempotency_keys INSERT
2. payments INSERT
3. payment_events INSERT
4. transaction commit
5. Kafka publish
6. publish 성공 시 payment_events.published = true
```

> 초기 구현에서는 Kafka publish를 transaction 이후 수행합니다.  
> 장기적으로는 Outbox relay 구조로 확장합니다.

### 승인 성공 트랜잭션

```
1. payments 상태 APPROVING 확인
2. payment_attempts INSERT
3. payments 상태 APPROVED 변경
4. payment_events INSERT (payment.approved)
5. transaction commit
```

### 승인 실패 트랜잭션

```
1. payments 상태 APPROVING 확인
2. payment_attempts INSERT
3. payments 상태 FAILED 변경
4. failure_reason 기록
5. payment_events INSERT (payment.failed)
6. transaction commit
```

---

## 10. 운영 관점 중요 쿼리

### 최근 실패 결제 조회

```sql
SELECT payment_id, merchant_id, order_id, status, failure_reason, created_at
FROM payments
WHERE status = 'FAILED'
ORDER BY created_at DESC
LIMIT 50;
```

### 특정 결제의 승인 시도 이력

```sql
SELECT attempt_id, provider, status, latency_ms, error_code, attempted_at
FROM payment_attempts
WHERE payment_id = :payment_id
ORDER BY attempted_at DESC;
```

### Kafka 미발행 이벤트 조회

```sql
SELECT event_id, payment_id, event_type, created_at
FROM payment_events
WHERE published = false
ORDER BY created_at ASC
LIMIT 100;
```

### 오래된 PROCESSING idempotency key 조회

```sql
SELECT merchant_id, idempotency_key, payment_id, created_at
FROM idempotency_keys
WHERE status = 'PROCESSING'
  AND created_at < now() - interval '10 minutes'
ORDER BY created_at ASC;
```

---

## 11. 장애 시나리오 연결

이 schema는 다음 장애 시나리오와 연결됩니다.

- DB Connection Pool 고갈
- Slow Query 증가
- Lock Contention
- Kafka publish 실패
- 중복 결제 요청 폭증
- 외부 Provider timeout 증가

---

## 12. SRE 관점 핵심 포인트

### 12.1 payments

운영에서 가장 중요한 테이블입니다.

**확인 포인트**

- `status`별 건수 추이
- `FAILED` 증가 여부
- `REQUESTED` 상태에서 오래 머무는 결제
- `APPROVING` 상태에서 오래 머무는 결제

### 12.2 payment_attempts

외부 Provider 장애 분석에 중요합니다.

**확인 포인트**

- `latency_ms` 증가
- `TIMEOUT` 증가
- `provider`별 `error_code` 증가
- retry 증가

### 12.3 payment_events

Kafka 발행 장애 분석에 중요합니다.

**확인 포인트**

- `published=false` 이벤트 증가
- `event_type`별 적체
- `created_at` 기준 오래된 이벤트 존재 여부

### 12.4 idempotency_keys

중복 결제 방지와 장애 복구에 중요합니다.

**확인 포인트**

- `PROCESSING` 상태 장기 지속
- 동일 merchant의 중복 요청 증가
- `expires_at` 관리 상태

---

## 13. 초기 Seed Data

```sql
INSERT INTO merchants (
    merchant_id,
    name,
    status,
    api_key_hash,
    callback_url
) VALUES (
    'merchant-001',
    'Demo Merchant',
    'ACTIVE',
    'demo-api-key-hash',
    'https://merchant.example.com/callback'
);
```

---

## 14. 향후 개선

### Outbox Pattern

현재는 `payment_events` 테이블을 기반으로 Outbox 패턴으로 확장할 수 있습니다.

```
Payment transaction commit
→ Outbox relay가 payment_events 조회
→ Kafka publish
→ published = true 업데이트
```

### Partitioning

결제 데이터가 많아질 경우 `payments.created_at` 기준 월별 partitioning을 고려합니다.

### Audit Table

운영 감사 요구사항이 강해질 경우 별도 감사 테이블을 추가합니다.

```
payment_audit_logs
```

---

## 15. 요약

> 결제 상태를 정합성 있게 관리하고,  
> 외부 승인 시도와 이벤트 발행 이력을 남기며,  
> **장애 발생 시 원인 분석이 가능한 데이터를 저장하는 것.**