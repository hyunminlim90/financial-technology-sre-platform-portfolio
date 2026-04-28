# Scenario: Database Connection Pool Exhaustion

## 1. 시나리오 개요

이 시나리오는 애플리케이션에서 사용하는 DB connection pool이 고갈되는 상황을 가정합니다.

Connection pool 고갈은 다음과 같은 치명적인 문제를 유발합니다.

- API 요청 대기 (blocking)
- 응답 지연 증가
- timeout 발생
- 전체 서비스 장애로 확산

---

## 2. 장애 정의

### 조건

```
r2dbc.pool.pending > 0 이 10초 이상 지속
또는
r2dbc.pool.acquired >= r2dbc.pool.max 에 근접
또는
ConnectionAcquisitionTimeoutException 발생
또는
JDBC 기반 컴포넌트에서 HikariCP pending / timeout 증가
```

---

## 3. 사용자 영향

- 결제 요청 응답 지연
- 일부 요청 timeout
- 5xx 증가
- retry 증가

---

## 4. 시스템 영향 범위

```
Payment API (Spring WebFlux)
r2dbc-pool
PostgreSQL
Payment Worker / JDBC 기반 컴포넌트
HikariCP
```

---

## 5. 주요 증상

### Metrics

- `hikaricp.connections.active` 증가
- `hikaricp.connections.pending` 증가
- `hikaricp.connections.timeout` 증가

### Logs

```
HikariPool - Connection is not available
timeout after 30000ms
```

### Traces

```
postgres.query span 대기 증가
```

---

## 6. 원인 후보

### 6.1 Slow Query

- 쿼리 실행 시간이 길다
- 인덱스 없음

### 6.2 Connection Leak

- connection 반환 누락
- transaction 미종료

### 6.3 트래픽 급증

- 갑작스러운 요청 증가

### 6.4 Pool 설정 부족

- max pool size 너무 작음

### 6.5 Lock Contention

- row lock
- table lock

### 6.6 Long Transaction

- transaction 오래 유지

---

## 7. 탐지 방법

### PromQL

### R2DBC Pool

```promql
sum(r2dbc_pool_pending_connections) > 0
```

```promql
sum(r2dbc_pool_acquired_connections)
```

```promql
sum(rate(r2dbc_pool_acquire_seconds_count[1m]))
```

---

### JDBC 컴포넌트 확인 (HikariCP)

```promql
sum(hikaricp_connections_pending)
```

```promql
sum(rate(hikaricp_connections_timeout_total[1m]))
```

**Prometheus Alert**

```promql
hikaricp_connections_active / hikaricp_connections_max > 0.9
```

---

## 8. 진단 흐름

```
1. pool 사용률 확인
2. slow query 확인
3. active connection 확인
4. lock 여부 확인
5. 트래픽 증가 여부 확인
```

---

## 9. 재현 방법 (Simulation)

---

### 패턴 A. Slow Query

```sql
SELECT pg_sleep(10);
```

대량 요청 상황에서 slow query가 connection을 오래 점유하도록 만든다.

---

### 패턴 B. Connection Leak

WebFlux reactive chain에서 에러 발생 시 connection 반환이 지연되거나 누락되는 상황을 만든다.

**예:**

- `flatMap` 내부 예외 발생
- `onErrorResume` 누락
- `timeout` 미설정

**관찰 포인트:**

- 트래픽 감소 이후에도 `r2dbc.pool.acquired`가 줄어들지 않음
- `r2dbc.pool.pending`이 계단식으로 증가

---

### 패턴 C. Transaction Block

외부 Mock API 응답을 10초 이상 지연시킨 뒤, DB transaction이 유지된 상태에서 Payment API를 호출한다.

**예시 흐름:**

```
DB transaction 시작
→ Payment 상태 변경
→ External Provider Mock 호출
→ 10초 대기
→ transaction commit
```

**기대 결과:**

- connection 점유 시간 증가
- `r2dbc.pool.pending` 증가
- API latency 증가
- timeout 발생

---

## 10. 기대 결과

- connection pool 포화
- API latency 증가
- timeout 발생

---

## 11. Runbook 연결

```
runbooks/database/connection-pool-exhaustion.md
```

---

## 12. 핵심 포인트

> **"DB가 느린 게 아니라 connection이 부족한 것"**

---

## 13. 포트폴리오 포인트

이 시나리오를 통해 보여줄 수 있는 역량:

- HikariCP 이해
- DB connection lifecycle 이해
- slow query 분석 능력
- lock 분석 경험