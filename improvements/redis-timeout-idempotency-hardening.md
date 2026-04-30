# improvements/redis-timeout-idempotency-hardening.md

---

title: Redis Timeout Idempotency Hardening
knowledge_type: improvement
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
  related_improvements: []
  related_preventive_designs:
* preventive-designs/redis-timeout-idempotency-fallback.md
  tags:
* redis
* timeout
* idempotency
* fallback
* duplicate-payment
* retry
* hardening

---

# Improvement: Redis Timeout Idempotency Hardening

---

## 1. 개요

Redis timeout 장애를 경험한 이후, 결제 시스템의 **idempotency 안정성을 강화하기 위한 개선 사항**을 정의한다.

목표:

```text id="k9h4r1"
Redis 장애 상황에서도 중복 결제가 절대 발생하지 않도록 시스템을 강화한다
```

---

## 2. 문제 상황 (Before)

장애 발생 시 관찰된 문제:

```text id="zj5c5v"
- Redis timeout 증가
- idempotency check 지연
- client retry 증가
- 동일 요청 반복 발생
```

결과:

```text id="zffxnm"
- duplicate request 증가
- idempotency conflict 증가
- DB fallback 부하 증가
```

---

## 3. Root Cause 요약

```text id="6f9kcu"
1. Redis 의존도가 너무 높음
2. retry 정책이 공격적으로 설정됨
3. fallback 경로는 있었지만 부하 고려 부족
4. idempotency 상태 관리 미흡 (PROCESSING stuck)
```

---

## 4. 개선 목표

```text id="4cy6p3"
1. Redis 장애 시에도 안정적인 fallback 보장
2. retry로 인한 장애 증폭 방지
3. idempotency 상태 관리 강화
4. duplicate request 조기 탐지
```

---

## 5. 개선 사항

---

### 5.1 Retry 정책 개선

기존:

```text id="h7pm6p"
- fixed retry
- retry 횟수 많음
```

개선:

```text id="5yq7sv"
- exponential backoff 적용
- max retry 제한
- jitter 적용
```

효과:

```text id="7qntqt"
retry storm 방지
```

---

### 5.2 Redis Timeout 전략 변경

기존:

```text id="z9p6w2"
timeout 길게 설정
→ 응답 대기 증가
```

개선:

```text id="9r7i5s"
timeout 단축
→ 빠른 fallback 전환
```

효과:

```text id="1h13tg"
Redis 장애 시 빠르게 DB fallback 수행
```

---

### 5.3 Idempotency Fallback 강화

기존:

```text id="6e8v4j"
Redis 실패 시 일부 요청 skip 가능성
```

개선:

```text id="bxycbx"
Redis 실패 시 무조건 DB fallback 수행
```

효과:

```text id="7o5i2l"
중복 결제 방어 강화
```

---

### 5.4 Idempotency 상태 관리 개선

문제:

```text id="wnajy3"
PROCESSING 상태에서 장애 발생 시 stuck
```

개선:

```text id="2n2k5h"
- TTL 기반 상태 만료
- 일정 시간 후 재시도 허용
- 상태 cleanup job 추가
```

효과:

```text id="lznkqy"
stuck request 제거
```

---

### 5.5 Request Hash 검증 강화

문제:

```text id="zx8a9r"
동일 key + 다른 payload 가능
```

개선:

```text id="p5jktx"
request_hash 비교 필수화
```

효과:

```text id="lm0fw4"
잘못된 재시도 차단
```

---

### 5.6 Duplicate Detection 강화

추가 지표:

```text id="2hmjcv"
payment_duplicate_request_total
payment_idempotency_conflict_total
```

추가 알림:

```text id="1sptg5"
duplicate request 급증 시 alert
```

효과:

```text id="3w4mso"
중복 결제 위험 조기 탐지
```

---

### 5.7 DB 부하 보호

문제:

```text id="w6b3ut"
fallback 증가 → DB overload
```

개선:

```text id="wrt5q6"
- fallback rate 제한
- circuit breaker 적용
- DB connection pool 모니터링 강화
```

효과:

```text id="z0j6h1"
DB 장애 전파 방지
```

---

## 6. 적용 후 변화 (After)

```text id="1t0yrc"
Redis timeout 발생
→ 빠른 fallback
→ retry 제한
→ duplicate request 감소
→ DB fallback 안정 처리
→ 중복 결제 없음
```

---

## 7. 관측 지표 개선

```text id="7j4t2m"
redis_timeout_total 감소
payment_duplicate_request_total 감소
payment_idempotency_conflict_total 안정화
fallback_success_rate 증가
```

---

## 8. 추가 모니터링

```text id="3iyjv9"
fallback_rate
fallback_latency
idempotency_processing_stuck_count
retry_rate
```

---

## 9. 교훈 (Lessons Learned)

```text id="b47i08"
Redis는 신뢰할 수 없는 컴포넌트로 가정해야 한다
```

```text id="s2k0sz"
Retry는 장애를 증폭시킬 수 있다
```

```text id="cql0yb"
Idempotency는 cache가 아니라 DB에서 보장해야 한다
```

---

## 10. SRE 핵심 통찰

```text id="7h7l8z"
Fallback이 없는 시스템은 장애가 아니라 사고로 이어진다
```

```text id="q3m7sj"
성능보다 정합성이 우선이다 (FinTech)
```

---

## 11. 향후 개선

```text id="x1x9tb"
- multi-region Redis 구성 검토
- Redis cluster failover 테스트 강화
- chaos test 자동화
- idempotency system 분리 검토
```

---

## 12. 요약

```text id="z7vxqz"
Before:
Redis 의존 → 장애 시 위험

After:
Fallback + Retry 제어 + 상태 관리
→ 안정성 확보
```

핵심은 “Redis 없이도 안전해야 한다”
