# Incident Lifecycle Workflow

---

## 1. 목적

이 문서는 장애 발생부터 학습까지의  **전체 운영 흐름(End-to-End Lifecycle)** 을 정의한다.

> 이 문서는 
> **AI Agent + Human + RAG 시스템이 따라야 할 실행 기준이다.**

---

## 2. 핵심 원칙

> AI는 실행하지 않는다  
> AI는 판단을 보조한다  
> Human이 최종 실행한다  
> 시스템은 학습한다

---

## 3. 전체 흐름

```
[1]  Alert 발생
        ↓
[2]  AI Recommendation API 호출
        ↓
[3]  AI Agent 분석 (RAG 기반)
        ↓
[4]  대응 권장 문서 생성
        ↓
[5]  Human 판단 및 실행
        ↓
[6]  결과 검증 (Observability)
        ↓
[7]  (필요 시) 반복 분석
        ↓
[8]  Incident 종료 선언
        ↓
[9]  Postmortem Draft 생성 (AI)
        ↓
[10] Human 검증 / 승인
        ↓
[11] RAG Learning Knowledge 반영
```

---

## 4. 단계별 상세 정의

### 4.1 Alert 발생

**출처:**

- Prometheus Alertmanager
- SLO 기반 Alert
- 로그 기반 Alert
- 수동 감지

**예:**

- Redis timeout 증가
- DB connection pool exhaustion
- Kafka consumer lag 증가

### 4.2 AI Recommendation API 호출

```
POST /incident/analyze
```

**입력:**

```json
{
  "alert": "...",
  "service": "payment-api",
  "environment": "production",
  "timestamp": "...",
  "metrics_snapshot": {},
  "logs_sample": "...",
  "trace_id": "..."
}
```

### 4.3 AI Agent 분석 (핵심 단계)

AI는 다음 순서로 판단한다:

**1) Context 수집**

- alert 내용 / metrics / logs / traces
- 서비스 / 환경

**2) RAG 검색 우선순위**

| 순서 | 대상 |
|------|------|
| 1 | `scenarios/` |
| 2 | `runbooks/` |
| 3 | `improvements/` |
| 4 | `preventive-designs/` |
| 5 | `postmortems/` |
| 6 | `rag/docs` (보조) |

**3) Scenario 매칭**

- `failure_mode` 식별
- severity 판단
- impact_scope 판단

**4) Runbook 후보 선택**

- 가능한 대응 전략 도출

**5) Improvement 적용**

- 위험한 Action 제거
- 제한 규칙 적용

**6) Preventive Design 검토**

- 구조적 해결 필요 여부 판단
- runbook override 가능

**7) Postmortem 참고**

- 과거 실패 / 성공 사례 반영

**8) rag/docs 보조 해석**

사용 조건: metric 해석 필요, 원인 후보 다수, 메커니즘 이해 필요

> ❗ **제한:** `rag/docs`는 Action 결정에 사용 금지

### 4.4 대응 권장 문서 생성

AI는 다음 구조로 결과를 생성한다:

| # | 항목 |
|---|------|
| 1 | Incident Summary |
| 2 | 추정 원인 (Hypothesis) |
| 3 | 근거 (Metrics / Logs / Traces) |
| 4 | Recommended Actions (Sequenced) |
| 5 | Risk 분석 |
| 6 | Rollback Plan |
| 7 | Verification 방법 |
| 8 | 금지 Action (Improvement 기반) |
| 9 | Human Approval Required |

### 4.5 Human 판단 및 실행

**Human 역할:**

- ✔ AI 권장 검토
- ✔ 실행 여부 판단
- ✔ kubectl / config 변경 수행
- ✔ rollback 실행

> ❗ **원칙:** `AI Recommendation ≠ Execution`

### 4.6 결과 검증 (Observability)

**검증 기준:**

- Metrics 정상화
- Error rate 감소
- Latency 감소
- Retry 감소

**사용 도구:** Prometheus / Grafana, Loki, Jaeger

### 4.7 반복 분석 (Loop)

**조건:**

- 문제 미해결
- 새로운 증상 발생
- 원인 불명확

**동작:** → 다시 AI Recommendation API 호출

### 4.8 Incident 종료 선언

**조건:**

- 서비스 정상화
- 주요 지표 정상 범위
- 추가 확산 없음

### 4.9 Postmortem Draft 생성 (AI)

```
POST /postmortem/generate
```

**AI 생성 내용:**

- Incident Summary
- Timeline
- Root Cause 후보
- Impact
- 대응 과정
- 개선 제안

**파일명 추천:**

```
postmortems/redis-timeout-2026-xx-xx.md
```

### 4.10 Human 검증 / 승인

- ✔ Root Cause 확정
- ✔ 내용 수정
- ✔ 과도한 추론 제거
- ✔ 최종 승인
- ✔ Git commit

### 4.11 RAG Learning Knowledge 반영

**추가되는 지식:**

```
postmortems/
improvements/
preventive-designs/
```

| 구분 | 처리 |
|------|------|
| Runbook 수정 | ❌ |
| Learning Knowledge 추가 | ✅ |

---

## 5. Knowledge Usage Rule

| 구분 | 경로 |
|------|------|
| **Primary Knowledge** | `scenarios/`, `runbooks/`, `improvements/`, `preventive-designs/`, `postmortems/` |
| **Secondary Knowledge** | `rag/docs/` |

> **우선순위:** Primary > rag/docs

---

## 6. Safety Rule

- AI는 절대 실행하지 않는다
- 모든 변경은 Human 승인 필요
- Improvement / Preventive Design이 Runbook보다 우선

---

## 7. Failure Handling

AI 판단 실패 시:

- Human이 override
- Postmortem에 기록
- 다음 판단에 반영

---

## 8. 핵심 구조 요약

```
Detection → Analysis → Recommendation → Human Execution → Verification → Learning
```