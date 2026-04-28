# Runbook: Database Connection Pool Exhaustion

## 1. 개요

DB connection pool 고갈 시 빠르게 원인을 분석하고 서비스 장애를 완화하기 위한 절차입니다.

---

## 2. 증상

- API latency 증가
- timeout 증가
- HikariPool timeout 로그

```
HikariPool - Connection is not available
timeout after 30000ms
```

---

## 3. 영향도

- 모든 DB 의존 API 영향
- 결제 실패 증가

---

## 4. 즉시 확인 (5분 내)

```
Grafana → DB Connection Pool
```

**확인 항목**

- `active` / `max` 비율
- `pending` 수
- `timeout count`

---

## 5. 진단 절차

### Step 1. Connection 상태 확인

```sql
SELECT count(*) FROM pg_stat_activity;
```

### Step 2. Slow Query 확인

```sql
SELECT query, mean_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;
```

### Step 3. Lock 확인

```sql
SELECT * FROM pg_locks WHERE NOT granted;
```

### Step 4. 트랜잭션 확인

```sql
SELECT pid, state, query, now() - query_start AS duration
FROM pg_stat_activity
WHERE state != 'idle'
ORDER BY duration DESC;
```

### Step 5. 애플리케이션 로그 확인

```bash
kubectl logs <payment-api-pod>
```

---

## 6. 원인별 대응

### 6.1 Slow Query

**대응**

- 인덱스 추가
- 쿼리 수정

### 6.2 Connection Leak

**대응**

- 코드 수정
- connection close 보장

### 6.3 트래픽 급증

**대응**

```bash
kubectl scale deployment payment-api --replicas=<n>
```

### 6.4 Pool 설정 부족

**대응**

```
maxPoolSize 증가
```

### 6.5 Lock Contention

**대응**

- 문제 트랜잭션 종료
- 쿼리 개선

---

## 7. 즉시 완화 조치 (Mitigation)

- scale-out
- pool size 증가
- 트래픽 제한

---

## 8. 근본 해결 (Resolution)

- slow query 제거
- connection leak 제거
- 적절한 pool sizing

---

## 9. 롤백 기준

배포 이후 발생 시 즉시 rollback

```bash
argocd app rollback <app-name>
```

---

## 10. 재발 방지

- connection pool alert 설정
- slow query 모니터링
- load test 수행

---

## 11. 핵심 명령어 요약

```sql
-- 현재 connection 수 확인
SELECT count(*) FROM pg_stat_activity;

-- Slow query 확인
SELECT query, mean_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;

-- Lock 확인
SELECT * FROM pg_locks WHERE NOT granted;

-- Long transaction 확인
SELECT pid, state, query, now() - query_start AS duration
FROM pg_stat_activity
WHERE state != 'idle'
ORDER BY duration DESC;
```

```bash
# Pod 로그 확인
kubectl logs <payment-api-pod>

# Scale-out
kubectl scale deployment payment-api --replicas=<n>
```

---

## 12. 요약

**이 Runbook의 핵심 절차**

1. pool 사용률 확인
2. slow query 확인
3. lock 확인
4. 빠르게 scale-out

---

## 13. 핵심 메시지

> **"Connection pool 고갈은 대부분 DB 문제가 아니라 애플리케이션 문제다."**