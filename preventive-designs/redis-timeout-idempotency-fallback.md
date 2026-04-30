# preventive-designs/redis-timeout-idempotency-fallback.md

---

title: Redis Timeout Idempotency Fallback Design
knowledge_type: preventive-design
domain: redis
failure_mode: redis-timeout
services:

* payment-api
* redis
* postgresql
  related_scenarios:
* scenarios/redis/timeout.md
  related_runbooks:
* runbooks/redis/timeout.md
  related_postmortems: []
  related_improvements:
* improvements/redis-timeout-idempotency-hardening.md
  tags:
* redis
* timeout
* idempotency
* fallback
* duplicate-payment
* payment-safety

---

# Preventive Design: Redis Timeout Idempotency Fallback

---

## 1. 개요

Redis 장애 발생 시에도 **중복 결제를 방지하기 위한 다중 방어 구조**를 정의한다.

핵심 목표:

```text
Redis가 죽어도 중복 결제는 절대 발생하지 않는다
```

---

## 2. 문제 정의

일반적인 구조:

```text
Client Request
→ Redis (Idempotency Key 체크)
→ DB 저장
```

문제:

```text
Redis timeout 발생
→ idempotency check 실패
→ 동일 요청 재시도
→ 중복 결제 발생 가능
```

---

## 3. 설계 원칙

```text
1. Single Point of Failure 제거
2. Multi-layer Idempotency
3. Fail-safe 설계
4. DB를 최종 진실(Source of Truth)로 사용
```

---

## 4. 전체 구조

```text
Client
   ↓
Payment API
   ↓
[1] Redis Idempotency Check (Primary)
   ↓ (실패 시)
[2] DB Idempotency Table (Fallback)
   ↓
[3] Payment Table Unique Constraint (Final Guard)
```

---

## 5. Layer별 방어 전략

---

### 5.1 Layer 1: Redis (Primary Fast Path)

```text
목적:
- 빠른 idempotency check
- 낮은 latency

문제:
- timeout
- eviction
- network failure
```

---

### 5.2 Layer 2: DB Idempotency Table (Fallback)

```text
table: idempotency_keys
```

구조:

```sql
CREATE TABLE idempotency_keys (
    id SERIAL PRIMARY KEY,
    merchant_id VARCHAR(64),
    idempotency_key VARCHAR(128),
    request_hash TEXT,
    status VARCHAR(20),
    created_at TIMESTAMP,
    UNIQUE (merchant_id, idempotency_key)
);
```

역할:

```text
- Redis 실패 시 fallback
- 중복 요청 차단
- 상태 기반 처리 (PROCESSING / SUCCESS / FAILED)
```

---

### 5.3 Layer 3: Payment Table Unique Constraint (Final Guard)

```sql
UNIQUE (merchant_id, order_id)
```

역할:

```text
최후의 방어선
→ 중복 결제 물리적 차단
```

---

## 6. 처리 흐름

---

### 정상 흐름

```text
Redis hit
→ 기존 요청 반환
```

---

### Redis timeout 발생

```text
Redis 실패
→ DB idempotency_keys 조회
→ 존재하면 기존 결과 반환
→ 없으면 INSERT 시도
```

---

### 동시 요청 발생

```text
두 요청 동시에 들어옴

Request A:
→ INSERT 성공

Request B:
→ UNIQUE constraint 충돌
→ 기존 요청 결과 반환
```

---

## 7. 상태 기반 처리

```text
PROCESSING
SUCCESS
FAILED
```

문제:

```text
PROCESSING 상태에서 장애 발생 시
→ stuck 상태 발생
```

대응:

```text
- TTL 적용
- 일정 시간 후 retry 허용
```

---

## 8. Request Hash 검증

```text
동일 idempotency key + 다른 payload
→ 위험한 요청
```

대응:

```text
request_hash 비교

불일치 시:
→ 400 Bad Request
→ 요청 거부
```

---

## 9. Redis 실패 시 동작 정책

```text
Redis timeout
→ fallback 무조건 수행
→ 실패 시 request reject
```

절대 금지:

```text
idempotency check skip ❌
```

---

## 10. Retry 정책

문제:

```text
timeout → retry → duplicate 요청 증가
```

대응:

```text
- retry 제한
- exponential backoff
- idempotency key 필수화
```

---

## 11. Observability 설계

### 필수 지표

```text
payment_duplicate_request_total
payment_idempotency_conflict_total
idempotency_fallback_total
redis_timeout_total
```

---

### 핵심 모니터링

```text
fallback 증가
→ Redis 장애 신호

duplicate 증가
→ idempotency 실패 위험
```

---

## 12. 장애 시 판단 기준

```text
Redis 장애 발생

→ DB fallback 정상 동작?
→ duplicate request 증가?
→ payment 중복 발생?
```

---

## 13. 핵심 통찰

```text
Redis는 "성능 최적화 레이어"일 뿐이다
```

```text
Idempotency의 진짜 책임은 DB가 져야 한다
```

---

## 14. SRE 관점 핵심 메시지

```text
Cache는 항상 깨질 수 있다
하지만 데이터 정합성은 깨지면 안 된다
```

```text
Redis 장애 대응의 핵심은
Redis 복구가 아니라
중복 결제 방지이다
```

---

## 15. 요약

```text
Layer 1: Redis (빠름, 불안정)
Layer 2: DB Idempotency (느림, 안정)
Layer 3: Payment Unique Constraint (최종 보호)
```

3단 방어 구조 필수
