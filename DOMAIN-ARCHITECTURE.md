# Domain Architecture

## 1. 목적

이 문서는 토스페이먼츠와 같은 FinTech 결제 도메인을 기준으로, 실무형 SRE 플랫폼의 도메인 아키텍처를 정의합니다.

목표는 단순 결제 API 구현이 아니라, 결제 시스템을 안정적으로 운영하기 위한 다음 요소를 함께 설계하는 것입니다.

- 결제 요청 처리
- 결제 승인 처리
- 결제 상태 관리
- 중복 결제 방지
- 비동기 이벤트 처리
- 장애 감지
- 장애 대응
- 운영 지표 수집

---

## 2. 핵심 도메인

### 2.1 Merchant

가맹점 정보를 나타냅니다.

**주요 속성**

```
merchantId
name
status
apiKey
callbackUrl
createdAt
updatedAt
```

**역할**

- 결제 요청 주체
- API 인증 대상
- Webhook 수신 대상

### 2.2 Payment

결제 트랜잭션의 중심 도메인입니다.

**주요 속성**

```
paymentId
merchantId
orderId
amount
currency
status
idempotencyKey
requestedAt
approvedAt
failedAt
```

**주요 상태**

- `REQUESTED`
- `APPROVING`
- `APPROVED`
- `FAILED`
- `CANCELLED`

### 2.3 PaymentAttempt

외부 결제 승인 API 호출 시도를 기록합니다.

**주요 속성**

```
attemptId
paymentId
provider
requestPayload
responsePayload
status
latencyMs
errorCode
createdAt
```

**역할**

- 외부 PG 호출 이력 보관
- 실패 원인 분석
- 재시도 판단 근거

### 2.4 PaymentEvent

결제 상태 변경 이벤트를 저장합니다.

**주요 속성**

```
eventId
paymentId
eventType
payload
published
createdAt
publishedAt
```

**역할**

- 이벤트 발행 이력
- 재처리 근거
- Outbox 패턴 확장 기반

### 2.5 IdempotencyKey

중복 결제 방지를 위한 도메인입니다.

**주요 속성**

```
key
merchantId
paymentId
status
expiresAt
createdAt
```

**역할**

- 동일 요청 중복 처리 방지
- 네트워크 재시도 안전성 확보
- 결제 API 안정성 보장

---

## 3. 서비스 구성

```
[Client / Merchant]
        |
        v
[Payment API]
        |
        +--> [Redis]
        |
        +--> [PostgreSQL]
        |
        +--> [Kafka]
        |
        v
[Payment Worker]
        |
        v
[External Payment Provider Mock]
```

---

## 4. 주요 컴포넌트

### 4.1 Payment API

**역할**

- 결제 요청 수신
- API 인증
- Idempotency 검증
- 결제 데이터 저장
- Kafka 이벤트 발행
- 결제 상태 조회

**주요 API**

```
POST /api/v1/payments
GET  /api/v1/payments/{paymentId}
POST /api/v1/payments/{paymentId}/cancel
```

### 4.2 Payment Worker

**역할**

- Kafka 이벤트 소비
- 외부 결제 승인 API 호출
- 승인 결과 저장
- 실패 시 재시도
- DLQ 전송

### 4.3 Redis

**사용 목적**

- Idempotency Key 저장
- Rate Limit
- 임시 결제 상태 캐싱
- Distributed Lock

**장애 영향**

- 중복 결제 방지 약화
- Rate Limit 실패
- API latency 증가
- fallback 필요

### 4.4 PostgreSQL

**사용 목적**

- 결제 원장 데이터 저장
- 결제 상태 저장
- 승인 시도 이력 저장
- 이벤트 발행 이력 저장

**장애 영향**

- 결제 생성 실패
- 상태 조회 실패
- 승인 결과 저장 실패

### 4.5 Kafka

**사용 목적**

- 결제 승인 요청 비동기 처리
- 결제 상태 변경 이벤트 발행
- Worker 확장성 확보

**주요 Topic**

- `payment.requested`
- `payment.approved`
- `payment.failed`
- `payment.cancelled`
- `payment.dlq`

**장애 영향**

- 결제 승인 지연
- Consumer lag 증가
- 후처리 지연

### 4.6 External Payment Provider Mock

외부 결제 승인 시스템을 시뮬레이션합니다.

**지원 시나리오**

- 정상 승인
- 응답 지연
- 일시적 실패
- 지속적 실패
- timeout
- partial failure

---

## 5. 결제 요청 흐름

1. Merchant가 결제 요청을 보냄
2. Payment API가 API Key 인증 수행
3. Idempotency Key 확인
4. Redis에 요청 키 저장
5. PostgreSQL에 Payment 생성
6. Kafka에 `payment.requested` 이벤트 발행
7. Client에 `paymentId`와 `REQUESTED` 상태 반환
8. Worker가 이벤트 소비
9. External Provider 승인 요청
10. 승인 결과 저장
11. Payment 상태 `APPROVED` 또는 `FAILED`로 변경

---

## 6. 상태 전이

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

**잘못된 상태 전이 예시**

- `FAILED` → `APPROVED`
- `CANCELLED` → `APPROVED`
- `APPROVED` → `REQUESTED`

---

## 7. 중복 결제 방지 전략

결제 API는 반드시 Idempotency Key를 요구합니다.

```
Idempotency-Key: merchant-001-order-123
```

**처리 전략**

1. Redis에서 key 확인
2. key가 없으면 저장 후 신규 결제 생성
3. key가 있으면 기존 `paymentId` 반환
4. 처리 중 상태면 동일 응답 반환
5. 실패 상태면 정책에 따라 재시도 허용 여부 판단

**운영 관점**

- Redis 장애 시 DB unique constraint로 2차 방어
- 동일 `merchantId` + `orderId` unique index 구성
- Idempotency key TTL 설정

---

## 8. 데이터 정합성 전략

결제 도메인은 정합성이 중요합니다. 따라서 다음 기준을 적용합니다.

- Payment 상태 변경은 DB transaction으로 처리
- 상태 변경 이력은 `PaymentEvent`에 기록
- 외부 API 호출 결과는 `PaymentAttempt`에 저장
- 중복 요청은 `IdempotencyKey`로 방어
- Kafka 발행 실패 대비 Outbox 패턴 확장 가능

---

## 9. 장애 가능 지점

### 9.1 Payment API 장애

**원인**

- Event loop blocking
- DB connection pool 고갈
- Redis timeout
- Kafka producer timeout
- CPU / memory pressure

**영향**

- 결제 요청 실패
- 응답 지연
- 5xx 증가

### 9.2 PostgreSQL 장애

**원인**

- Slow query
- Connection pool exhaustion
- Lock contention
- Disk pressure

**영향**

- 결제 생성 실패
- 상태 조회 실패
- 승인 결과 저장 실패

### 9.3 Redis 장애

**원인**

- Memory pressure
- Eviction
- Connection timeout
- Pod restart

**영향**

- 중복 결제 방지 약화
- Rate limit 실패
- latency 증가

### 9.4 Kafka 장애

**원인**

- Broker 장애
- Consumer lag
- Partition imbalance
- Poison message
- Producer timeout

**영향**

- 결제 승인 지연
- 이벤트 후처리 지연
- DLQ 증가

### 9.5 External Provider 장애

**원인**

- 외부 API latency 증가
- timeout
- 5xx 응답
- rate limit

**영향**

- 결제 승인 지연
- 결제 실패 증가
- Worker retry 증가

---

## 10. 운영 지표

### Payment API

- `http.server.requests.count`
- `http.server.requests.duration`
- `payment.request.count`
- `payment.success.count`
- `payment.failure.count`
- `payment.timeout.count`

### Redis

- `redis.commands.latency`
- `redis.connected.clients`
- `redis.used_memory`
- `redis.evicted_keys`
- `redis.keyspace_hits`
- `redis.keyspace_misses`

### PostgreSQL

- `db.connections.active`
- `db.connections.idle`
- `db.connections.pending`
- `db.query.duration`
- `db.lock.wait`

### Kafka

- `kafka.consumer.lag`
- `kafka.producer.error.count`
- `kafka.consumer.error.count`
- `kafka.dlq.message.count`

### External Provider

- `provider.request.count`
- `provider.latency`
- `provider.error.rate`
- `provider.timeout.count`

---

## 11. SLO 후보

| 항목 | SLI | 목표 |
|------|-----|------|
| 결제 요청 API Availability | 정상 응답 수 / 전체 요청 수 | 99.9% |
| 결제 요청 API Latency | p95 latency | p95 < 300ms |
| 결제 승인 처리 지연 | `payment.requested` 생성 시각 ~ `APPROVED`/`FAILED` 도달 시간 | p95 < 5s |
| 중복 결제 방지 성공률 | 중복 요청 중 기존 `paymentId` 반환 비율 | 99.99% |

---

## 12. 장애 시나리오 우선순위

1. 결제 API latency 증가
2. DB connection pool 고갈
3. Kafka consumer lag 증가
4. Redis timeout 증가
5. External Provider timeout 증가
6. 배포 후 5xx 증가

---

## 13. Runbook 연결

각 장애 시나리오는 Runbook으로 연결됩니다.

```
runbooks/payment-api/high-latency.md
runbooks/database/connection-pool-exhaustion.md
runbooks/kafka/consumer-lag.md
runbooks/redis/timeout.md
runbooks/external-provider/timeout.md
runbooks/deployment/high-error-rate-after-deploy.md
```

---

## 14. 설계 요약

이 도메인 아키텍처의 핵심은 다음과 같습니다.

- 결제 요청은 빠르게 수신하고 비동기 처리한다.
- 결제 상태는 PostgreSQL에서 정합성 있게 관리한다.
- Redis로 중복 결제와 rate limit을 방어한다.
- Kafka로 승인 처리와 후처리를 분리한다.
- 모든 주요 흐름은 Metrics, Logs, Traces로 관측 가능해야 한다.
- 장애 시나리오는 Runbook으로 대응 가능해야 한다.

---

## 15. 최종 목표

이 도메인 아키텍처는 단순 결제 API 구현이 아니라, 실무에서 운영 가능한 결제 시스템을 목표로 합니다.

> 결제 기능을 만드는 것이 아니라,  
> **장애가 발생해도 원인을 찾고 복구할 수 있는 결제 플랫폼을 만드는 것.**