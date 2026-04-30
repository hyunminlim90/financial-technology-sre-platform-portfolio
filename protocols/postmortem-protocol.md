# protocols/postmortem-protocol.md

# Postmortem Protocol (AI + Human Collaborative)

---

## 1. 목적

이 문서는 장애 발생 이후 **모든 상황을 빠짐없이 기록하고, 재발 방지를 위한 학습 시스템을 구축하기 위한 표준 프로토콜**을 정의한다.

핵심 목표:

```text
1. 장애를 정확히 기록한다
2. 원인을 명확히 규명한다
3. 재발 방지 설계를 만든다
4. RAG Learning Knowledge로 축적한다
```

---

## 2. 전체 프로세스

```text
[1] Incident 종료
        ↓
[2] AI 자동 데이터 수집
        ↓
[3] AI Postmortem Draft 생성
        ↓
[4] Human 검증 및 수정
        ↓
[5] Root Cause 확정
        ↓
[6] Action Item 정의
        ↓
[7] postmortems/ 저장
        ↓
[8] improvements/ / preventive-designs/ 확장
```

---

### 2.1 Postmortem 생성 트리거

다음 조건 중 하나 이상 만족 시 Postmortem을 반드시 생성한다.

### 트리거 조건

- SEV-1 장애 발생
- SEV-2 장애 발생
- 결제 실패 발생
- 중복 결제 발생
- 5xx error rate 급증
- latency SLO 초과
- rollback 수행된 경우
- 운영자가 수동으로 필요하다고 판단한 경우

### 원칙

> 모든 중요한 장애는 반드시 기록한다.

---

### 2.2 AI 입력 데이터 정의

AI Agent는 Postmortem 생성 시 다음 입력을 기반으로 동작한다.

### 입력 필드

- `incident_id`
- `failure_mode`
- `service`
- `environment` (prod / staging)
- `alert_name`
- `incident_time_range` (start ~ end)

### 예시

```json
{
  "incident_id": "INC-2026-05-01-001",
  "failure_mode": "redis-timeout",
  "service": "payment-api",
  "environment": "production",
  "alert_name": "RedisLatencyHigh",
  "incident_time_range": "2026-05-01T10:00:00Z ~ 2026-05-01T10:15:00Z"
}
```

### 원칙

> AI는 반드시 명확한 incident context를 입력으로 받아야 한다.

---

### 2.3 Postmortem Generation Trigger

Postmortem 초안은 다음 이벤트 발생 시 생성된다.

```text
- Incident 종료 선언 (Human)
```

AI는 다음을 수행한다:

```text
1. 장애 기간 데이터 수집
2. 대응 과정 분석 (RAG 기반)
3. Postmortem Draft 생성
4. 파일명 추천
```

Human은 다음을 수행한다:

```text
1. Draft 검토 및 수정
2. Root Cause 확정
3. 문서 승인 및 Git commit
```

---

## 3. AI 역할 정의

AI는 다음을 자동 수행한다.

### 3.1 데이터 수집

```text
- Alert 발생 시각
- 장애 시작 / 종료 시각
- Metrics (latency, error rate, throughput)
- Logs (error, exception)
- Traces (span latency, dependency)
- Deployment 이벤트
- Scale 이벤트
- Retry 발생 여부
```

### 3.2 자동 분석

```text
- Timeline 생성
- 영향 범위 분석
- 이벤트 흐름 정리
- 주요 anomaly 탐지
- 원인 후보 제시 (NOT final)
```

### 3.3 Draft 생성

```text
- Postmortem 초안 생성
- 모든 데이터 기반으로 작성
```

### 3.4 Failure Mode별 데이터 선택 규칙

AI는 `failure_mode`에 따라 수집할 지표를 다르게 선택해야 한다.

### 예시

### `redis-timeout`

- `redis_command_latency_seconds`
- `redis_timeout_total`
- `payment_duplicate_request_total`

### `db-connection-pool-exhaustion`

- `r2dbc.pool.pending`
- `db.connections.active`
- `db.connection.timeout`

### `kafka-consumer-lag`

- `kafka_consumer_lag`
- `kafka_consumer_rate`
- `rebalance_latency`

### 원칙

> 모든 데이터를 수집하지 않는다.  
> → 장애 유형에 맞는 **핵심 지표만** 선택한다.

### 3.5 Incident Response History 수집 규칙

AI는 장애 기간 동안의 "대응 히스토리"를 반드시 수집해야 한다.

수집 대상:

```text
- 초기 Alert 발생 시점
- 1차 AI 대응 가이드
- Human 실행 여부
- 추가 Alert 발생 여부
- 2차 / 3차 AI 대응 가이드
- 각 단계별 시스템 상태 변화
- 실패한 대응
- 성공한 대응
- 최종 해결 방법
```

원칙:

```text
단일 대응이 아니라 "전체 대응 흐름"을 기록한다
```

### 3.6 Alert Selection Rule

AI는 장애 기간 동안 발생한 모든 Alert를 기록하지 않는다.

다음 기준에 따라 Alert를 선택적으로 포함한다.

#### 포함 대상

```text
1. Trigger Alert (장애 최초 감지)
2. Escalation Alert (장애 확산)
3. Decision-driving Alert (대응 방향 변경에 영향)
```

#### 제외 대상

```text
- 중복 Alert
- 영향 없는 Alert
- 장애와 직접 관련 없는 Alert
```

#### 원칙

```text
Alert는 “나열”이 아니라
“장애 흐름을 설명하기 위한 도구”이다
```

---

## 4. Human 역할 정의

```text
- Root Cause 최종 확정
- 잘못된 AI 해석 수정
- 영향 범위 검증
- 대응 과정 검증
- 개선안 승인
```

---

## 5. Postmortem 문서 구조

모든 Postmortem 문서는 아래 구조를 따른다.

### 5.1 Front Matter (필수)

### Severity 기준

| Severity | 기준 |
|------|------|
| **SEV-1** | 결제 실패, 중복 결제 발생, 전체 서비스 장애 |
| **SEV-2** | 일부 API 장애, latency 증가 |
| **SEV-3** | 부분 기능 장애 |

## 원칙

> Severity가 높을수록 → 더 강한 개선 (`preventive-design`) 필요

```yaml
---
title: Redis Timeout Incident Postmortem
knowledge_type: postmortem
domain: redis
failure_mode: redis-timeout
severity: SEV-2
environment: production
services:
  - payment-api
  - redis
  - postgresql
incident_start: 2026-05-01T10:01:00Z
incident_end: 2026-05-01T10:15:00Z
duration_minutes: 14
related_scenarios:
  - scenarios/redis/timeout.md
related_runbooks:
  - runbooks/redis/timeout.md
related_improvements:
  - improvements/redis-timeout-idempotency-hardening.md
related_preventive_designs:
  - preventive-designs/redis-timeout-idempotency-fallback.md
tags:
  - redis
  - timeout
  - idempotency
  - duplicate-payment
---
```

### 5.2 Incident Summary

```text
- 무엇이 발생했는가
- 어떤 시스템에 영향이 있었는가
- 사용자 영향은 무엇인가
```

### 5.3 Impact

```text
- 영향을 받은 API
- 실패율 (%)
- latency 증가
- 중복 결제 발생 여부
- 재시도 증가 여부
```

### 5.4 Timeline (AI 자동 생성 + Human 보정)

```text
10:01 Redis latency 증가
10:02 API latency 증가
10:03 error rate 증가
10:05 scale-out 수행
10:06 DB connection pool saturation 발생
10:10 rollback 수행
10:15 정상화
```

### 5.5 Detection

```text
- 어떤 alert로 탐지되었는가
- detection delay (얼마나 늦게 감지했는가)
```

### 5.6 Root Cause (Human 확정)

```text
- 실제 원인 1개 또는 복합 원인
- 기술적 원인
- 시스템 설계 문제
```

예:

```text
Redis timeout 발생 시 scale-out을 수행했으나
retry 증가로 DB connection pool 고갈 발생
```

### 5.7 Contributing Factors

```text
- retry 정책 과도
- fallback 부하 고려 부족
- alert 늦음
```

### 5.8 What Went Well

```text
- fallback 정상 동작
- duplicate 결제 없음
```

### 5.9 What Went Wrong

```text
- 잘못된 대응 (scale-out)
- 판단 지연
```

### 5.10 대응 과정 평가

```text
- Runbook 준수 여부
- AI 권장과 실제 대응 비교
- 대응 시간
```

### 5.11 Action Items (필수)

```text
- retry 정책 수정
- Redis timeout 단축
- alert threshold 조정
- fallback 성능 개선
```

### 5.12 Prevention (중요)

```text
- 재발 방지 설계
- 개선 문서 연결
```

### 5.13 Metrics Analysis

```text
- latency 변화
- error rate 변화
- retry rate 변화
```

### 5.14 Lessons Learned

```text
- scale-out은 항상 해결책이 아니다
- Redis 장애는 DB로 전파된다
```

### 5.15 Incident Response History (중요)

장애 대응 과정 전체를 기록한다.

```text
[1] 10:01 Alert 발생 (Redis latency)
→ AI 가이드: scale-out 권장

[2] 10:05 scale-out 수행
→ 결과: DB connection pool saturation 발생

[3] 10:06 추가 Alert 발생 (DB pool exhaustion)
→ AI 가이드: scale-out 중단 + DB 상태 확인

[4] 10:10 rollback 수행

[5] 10:15 정상화
```

포함 내용:

```text
- 각 단계별 AI 권장 내용
- Human 실제 실행 내용
- 결과 (성공 / 실패)
```

---

## 6. 자동 생성 규칙 (AI)

AI는 반드시 다음 규칙을 따른다.

```text
1. 모든 데이터 기반 작성
2. 추측 금지
3. Root Cause는 후보로만 제시
4. Timeline은 반드시 포함
5. Metrics 기반 분석 포함
```

---

## 7. Human 검증 규칙

```text
1. Root Cause 반드시 수정/확정
2. 잘못된 해석 제거
3. Action Item 추가
4. 재발 방지 포함
```

---

## 8. RAG Integration Rule

Postmortem 문서는 다음에 사용된다:

```text
- AI 판단 보정
- 동일 장애 재발 시 참고
- 잘못된 대응 방지
```

---

### 8.1 RAG 반영 시점

Postmortem 문서는 다음 조건을 만족해야 RAG에 포함된다.

### 포함 조건

1. Human 검증 완료
2. Root Cause 확정
3. Action Item 정의 완료
4. 문서 승인 상태 (`approved`)

### 원칙

> 검증되지 않은 Postmortem은 RAG에 포함하지 않는다.

---

## 9. Improvement 연계

Postmortem 완료 후 반드시 수행:

```text
1. improvements/ 문서 생성
2. preventive-designs/ 업데이트
```

---

### 9.1 Action Item 분류 규칙

Action Item은 다음 기준으로 분리한다.

| 우선순위 | 기준 | 대상 경로 |
|------|------|------|
| 1 | 구조 변경 필요 | `preventive-designs/` |
| 2 | 설정 / 튜닝 개선 | `improvements/` |
| 3 | Runbook 수정 필요 | `runbooks/` (예외적 수정) |

### 예시

| Action Item | 분류 |
|------|------|
| retry 정책 변경 | `improvements/` |
| idempotency 구조 변경 | `preventive-designs/` |
| 잘못된 대응 절차 | `runbooks/` |

---

## 10. 핵심 원칙

```text
Postmortem은 기록이 아니라 학습이다
```

```text
같은 장애는 두 번 발생하면 안 된다
```

```text
AI는 기록하고
Human은 판단한다
```

---

## 11. 요약

```text
AI:
- 데이터 수집
- 분석
- 초안 작성

Human:
- 검증
- 판단
- 개선 설계
```

---

## 12. 최종 목표

```text
장애 대응 시스템 → 장애 학습 시스템
```

이 문서는 RAG Learning Knowledge 생성의 핵심 프로토콜이다.
