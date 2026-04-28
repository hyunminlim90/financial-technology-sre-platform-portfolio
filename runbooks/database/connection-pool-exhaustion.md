# Runbook: Database Connection Pool Exhaustion (R2DBC / JDBC Hybrid)

---

## 1. 개요

이 Runbook은 Payment 시스템에서 발생하는 **Database Connection Pool 고갈** 상황을 빠르게 진단하고 완화하기 위한 절차를 정의합니다.

본 시스템은 다음 구조를 가집니다:

```text
Payment API: Spring WebFlux + R2DBC
Worker / 일부 컴포넌트: JDBC (HikariCP) 가능
```

따라서 본 런북은 **R2DBC Pool과 JDBC Pool(HikariCP)을 모두 고려**합니다.

---

## 2. 핵심 이해 (중요)

```text
WebFlux = Non-blocking thread 모델
R2DBC = Non-blocking DB I/O
Connection Pool = 물리적 DB connection 제한
```

즉,

> WebFlux는 thread 고갈을 막지만
> DB connection 고갈은 막아주지 않는다.
<br/>

### SRE Tip: R2DBC Pool 특성 (중요)

### 개요

R2DBC는 JDBC와 다르게 비동기 방식으로 동작한다.

```
JDBC:
Thread 1 : Connection 1  (1:1 매핑)

R2DBC:
Thread N (event loop) : Connection M (shared)  (N:M 매핑)
```

즉, 적은 수의 connection으로 더 많은 요청을 처리할 수 있다.

---

### 설정 기준

| 종류 | maxPoolSize 예시 |
|------|-----------------|
| JDBC (HikariCP) | `50 ~ 200` |
| R2DBC | `10 ~ 50` |

---

### ⚠️ 운영 포인트

```
R2DBC에서 pool size를 과도하게 늘리면
→ DB connection 부담 증가
→ 오히려 성능 저하 및 장애 유발 가능
```

---

## 3. 증상

### 공통 증상

```text
- API latency 증가
- timeout 증가
- 요청 대기 증가
- throughput 감소
```

---

### R2DBC 환경

```text
ConnectionAcquisitionTimeoutException
r2dbc.pool.pending 증가
```

---

### JDBC 환경 (HikariCP)

```text
HikariPool - Connection is not available
timeout after 30000ms
```

---

## 4. 영향도

```text
- 모든 DB 의존 API 지연 또는 실패
- 결제 실패 증가
- retry 증가 → 장애 증폭
```

---

## 5. 즉시 확인 (5분 내)

### 5.1 가장 먼저 확인 (핵심)

```text
Grafana → Application Pool Metrics
```

---

### R2DBC 확인 지표

```text
r2dbc.pool.acquired
r2dbc.pool.allocated
r2dbc.pool.idle
r2dbc.pool.pending   ← 중요
```

---

### JDBC 확인 지표

```text
hikaricp.connections.active
hikaricp.connections.pending
hikaricp.connections.timeout
```

---

## ⚠️ 중요 판단

```text
pending 증가 = 실제 pool 고갈
```

---

## 6. 진단 절차 (중요 순서)

---

## Step 1. Pool 종류 확인

```text
Payment API → R2DBC
Worker / 일부 → JDBC
```

---

## Step 2. Application Pool 상태 확인 (가장 중요)

```text
r2dbc.pool.pending 증가 여부
r2dbc.pool.acquired ≈ max 여부
```

---

### 해석

```text
pending ↑ + acquired max 근접
→ 실제 pool 고갈
```

---

### 중요한 케이스

```text
DB connection 여유 있음
+ app pending 높음
→ app pool 설정 문제 또는 acquisition 지연
```

---

## Step 3. DB 상태 확인

```sql
SELECT state, count(*) 
FROM pg_stat_activity 
GROUP BY state;
```

---

### 확인 포인트

```text
active connection 수
idle connection 수
long running query
```

---

## Step 4. Slow Query 분석

```sql
SELECT query, mean_time, calls
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;
```

### SRE Tip: Trace 연계 (Correlation)

### 개요

DB에서 발견된 문제 쿼리 또는 transaction을 애플리케이션 요청과 연결해야 한다.

---

### 확인 방법

**DB 레벨**

```
pid
query
transaction duration
```

**Application 레벨**

```
traceId
requestId
paymentId
```

---

### 매핑 전략

```
1. pg_stat_activity에서 문제 pid 확인
2. 해당 query / timestamp 기준으로 로그 조회
3. traceId 추출
4. traceId → paymentId 매핑
```

---

### 목적

```
어떤 결제 요청이 DB connection을 점유하고 있는지 식별
```

---

### 효과

```
DB 문제 → 결제 건 식별 → 영향 범위 파악 가능
```

---

## Step 5. Long Transaction 확인

```sql
SELECT pid, state, now() - query_start AS duration, query
FROM pg_stat_activity
WHERE state != 'idle'
ORDER BY duration DESC;
```

---

## Step 6. Lock 분석 (중요)

```sql
SELECT * FROM pg_locks WHERE NOT granted;
```

---

### 심화 (Blocking 분석)

```sql
-- blocking / blocked 관계 확인
SELECT
    blocked_locks.pid     AS blocked_pid,
    blocking_locks.pid    AS blocking_pid
FROM pg_locks blocked_locks
JOIN pg_locks blocking_locks
ON blocking_locks.locktype = blocked_locks.locktype;
```

---

## Step 7. 애플리케이션 구조 확인 (매우 중요)

```text
@Transactional 내부에 외부 API 호출 존재 여부
```

---

### 위험 구조

```text
DB transaction 시작
→ 외부 결제 API 호출
→ 응답 대기
→ commit
```

---

### 결과

```text
외부 API 느림 → connection 장시간 점유 → pool 고갈
```

---

## Step 8. 리소스 확인

```bash
kubectl top pod
```

```text
CPU saturation
memory 부족
```

---

## 7. 원인별 대응

---

### 7.1 Slow Query

```text
원인:
- 인덱스 없음
- 비효율 쿼리
```

대응:

```text
- 인덱스 추가
- query 튜닝
```

---

### 7.2 Long Transaction / 외부 API 호출

대응:

```text
- transaction 범위 축소
- 외부 호출을 transaction 밖으로 이동
```

---

### 7.3 Connection Leak (특히 WebFlux)

징후:

```text
트래픽 감소 후에도 acquired 감소 없음
```

원인:

```text
- reactive chain 종료 안됨
- subscribe() misuse
```
<br/>

### SRE Tip: WebFlux Connection Leak 패턴

### 개요

WebFlux 환경에서는 reactive chain이 정상적으로 종료되지 않으면  
connection이 반환되지 않고 유지될 수 있다.

---

### Leak이 발생하는 주요 패턴

특히 다음과 같은 경우 leak이 발생할 수 있다.

- `flatMap` 내부에서 에러 발생 후 적절한 처리(`onErrorResume` 등) 누락
- `subscribe()`를 직접 호출하면서 lifecycle 관리 누락
- timeout 없이 외부 API 호출

---

### 관찰 포인트

```
트래픽 감소 이후에도 r2dbc.pool.acquired가 감소하지 않는다
pending이 점진적으로 증가한다
```

**특징적인 그래프 패턴**

```
"계단식으로 우상향하는 그래프"
```

> → 이는 connection leak 가능성이 매우 높다

---

### 7.4 Lock Contention

대응:

```text
- blocking transaction 종료
- query 개선
```

---

### 7.5 Pool 설정 부족

대응:

```text
r2dbc pool max-size 증가
HikariCP maxPoolSize 증가
```

---

### 7.6 트래픽 급증

⚠️ 중요:

```text
DB 상태 먼저 확인
```

---

## ✔ 조건부 Scale-out

```text
DB CPU 여유 있음 → scale-out 가능
DB 포화 상태 → scale-out 금지
```

---

## ❌ 잘못된 대응

```text
DB 느린 상태에서 scale-out
→ connection 증가 → DB 다운
```

---

## 8. 즉시 완화 조치 (Mitigation)

우선순위:

```text
1. rate limit / traffic shedding
2. scale-out (조건부)
3. pool size 증가 (임시)
4. rollback
```

---

## 9. 근본 해결 (Resolution)

```text
- slow query 제거
- transaction 구조 개선
- connection leak 제거
- pool sizing 재조정
```

---

## 10. 롤백 기준

```text
배포 이후 발생
latency 급증
error 증가
```

```bash
argocd app rollback <app-name>
```

---

## 11. 재발 방지

```text
- pool pending alert 설정
- long transaction monitoring
- DB lock monitoring
- load test 수행
```

### R2DBC Pool Graceful Shutdown

배포 또는 scale-in 시 connection이 정상적으로 반환되지 않으면 순간적인 latency 증가가 발생할 수 있다.

---

### 확인 포인트

- connection drain이 정상적으로 수행되는가
- shutdown 시 active connection이 강제 종료되는가
- in-flight request 처리 완료 후 종료되는가

---

### 권장 설정

- graceful shutdown timeout 설정
- readiness probe로 traffic 차단 후 종료
- connection close 대기 설정

---

### 운영 리스크

> 비정상 종료 시 connection leak 또는 DB 부하 증가 가능

---

## 12. 핵심 명령어

```sql
-- connection 상태
SELECT state, count(*) FROM pg_stat_activity GROUP BY state;

-- slow query
SELECT query, mean_time FROM pg_stat_statements ORDER BY mean_time DESC;

-- long transaction
SELECT now() - query_start FROM pg_stat_activity;

-- lock
SELECT * FROM pg_locks WHERE NOT granted;
```

---

## 13. 핵심 절차 요약

```text
1. r2dbc pending 확인
2. DB 상태 확인
3. slow query / lock 확인
4. 구조 문제 확인
5. 조건부 scale-out
```

---

## 14. 핵심 메시지 (개정판)

> Connection pool 고갈은 단순히 DB capacity 문제가 아니라
> **connection 점유 시간, transaction 구조, slow query, lock 문제의 결과이다.** <br/>
> R2DBC는 connection을 덜 쓰는 것이지, connection이 필요 없는 것이 아니다.
