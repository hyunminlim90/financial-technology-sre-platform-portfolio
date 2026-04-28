# Scenario: Payment API High Latency

## 1. 시나리오 개요

이 시나리오는 결제 API의 응답 시간이 비정상적으로 증가하는 상황을 가정합니다.

결제 시스템에서 latency 증가는 단순 성능 문제가 아니라 다음으로 이어질 수 있습니다.

- 결제 실패 증가
- 사용자 이탈
- 중복 결제 요청 증가 (retry)
- 외부 API 부하 증가
- 장애 확산

---

## 2. 장애 정의

### 조건

```
p95 latency > 300ms
또는
p99 latency > 1s
```

### 사용자 영향

- 결제 버튼 클릭 후 응답 지연
- 일부 결제 timeout 발생
- retry로 인해 중복 요청 증가

---

## 3. 시스템 영향 범위

```
Payment API
Redis
PostgreSQL
Kafka
External Payment Provider
```

---

## 4. 주요 증상

### 4.1 Metrics

- `http.server.requests.duration` 증가
- `http.server.requests.active` 증가
- `payment_request_duration_seconds` 증가

### 4.2 Logs

- API response time 증가
- timeout error 증가
- connection timeout 로그

### 4.3 Traces

특정 span latency 증가

```
예:
- postgres.query      → 느림
- redis.command       → 느림
- external.api.call   → 느림
```

---

## 5. 원인 후보

### 5.1 Application 레벨

- Event Loop Blocking (WebFlux)
- Thread starvation
- GC pause
- 코드 비효율 (N+1, sync 호출)

### 5.2 Database

- Slow Query
- Connection Pool 부족
- Lock contention

### 5.3 Redis

- latency 증가
- connection saturation
- memory pressure

### 5.4 Kafka

- producer send delay
- broker 응답 지연

### 5.5 External Provider

- API latency 증가
- timeout

### 5.6 Infrastructure

- CPU saturation
- memory pressure
- network latency
- pod restart

---

## 6. 탐지 방법

### 6.1 Prometheus Alert

Alert 이름: `HighPaymentApiLatency`

```promql
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 0.3
```

### 6.2 Dashboard 확인

- API latency p50 / p95 / p99
- request rate
- error rate
- pod CPU / memory

---

## 7. 진단 흐름 (핵심)

```
1. Latency 증가 확인
2. 전체 서비스인지 특정 API인지 구분
3. Trace로 병목 구간 확인
4. DB / Redis / External 중 어디인지 판별
5. 리소스 문제인지 확인
6. 최근 배포 여부 확인
```

---

## 8. 상세 진단 단계

### Step 1. API 수준 확인

- 특정 endpoint인지 확인
- 전체 서비스 영향인지 확인

### Step 2. Trace 분석

Jaeger에서 가장 긴 span 찾기

```
redis.command    → 느림
postgres.query   → 느림
external.api     → 느림
```

### Step 3. DB 확인

```sql
SELECT query, mean_time, calls
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;
```

**확인 포인트**

- slow query 존재 여부
- lock 여부

### Step 4. Redis 확인

- redis latency
- connected clients
- blocked clients

### Step 5. External API 확인

- external API latency 증가 여부
- timeout 증가 여부

### Step 6. 리소스 확인

```bash
kubectl top pod
kubectl describe pod
```

**확인 포인트**

- CPU 100%
- OOM 발생
- restart 증가

### Step 7. 최근 배포 확인

```
ArgoCD → 최근 deploy 확인
```

---

## 9. 재현 방법 (테스트)

### 방법 1. External API 지연

```
External Provider 응답을 1~2초 지연
```

### 방법 2. DB Slow Query

```sql
SELECT pg_sleep(2);
```

### 방법 3. Redis Delay

```
Redis latency artificial delay
```

### 방법 4. CPU 부하

```bash
stress-ng --cpu 4
```

---

## 10. 기대 결과

- latency 증가 감지
- trace에서 병목 구간 확인 가능
- alert 발생
- dashboard에서 문제 확인 가능

---

## 11. SRE 관점 핵심 포인트

> **"어디가 느린지 빠르게 찾을 수 있는가?"**

원인을 좁히는 흐름:

```
Metric → Trace → Log
```

---

## 12. Runbook 연결

이 시나리오는 다음 Runbook과 연결됩니다.

```
runbooks/payment-api/high-latency.md
```

---

## 13. 요약

결제 API latency 증가는 가장 흔하면서도 영향이 큰 장애입니다.

**핵심 대응 전략**

1. 전체 vs 부분 문제 구분
2. Trace로 병목 구간 식별
3. DB / Redis / External 중 원인 판별
4. 빠른 완화 조치 수행

---

## 14. 포트폴리오 포인트

이 시나리오를 통해 보여줄 수 있는 역량:

- WebFlux 이해
- Observability 활용 능력
- Trace 기반 문제 분석
- DB / Redis / External 병목 분석
- 실무형 장애 대응 사고방식