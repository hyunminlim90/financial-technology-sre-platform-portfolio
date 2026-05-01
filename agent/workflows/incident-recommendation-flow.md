# Incident Recommendation Flow

> AI Agent 기반 장애 대응 판단 워크플로우  
> (Human-in-the-loop / RAG Decision Engine)

---

## 1. 목적

이 문서는 Alert 발생 시  
AI Agent가 **어떻게 장애를 분석하고 대응을 추천하는지** 정의한다.

> 이 문서는 **"AI 판단 로직"** 이다.

---

## 2. 핵심 원칙

> AI는 실행하지 않는다  
> AI는 추천한다  
> Human이 실행한다

---

## 3. 전체 흐름

```
[Alert 발생]
        ↓
[Context 수집]
        ↓
[Scenario 매칭]
        ↓
[Runbook 후보 생성]
        ↓
[Improvement 필터링]
        ↓
[Preventive Design 고려]
        ↓
[Postmortem 보정]
        ↓
[rag/docs 보조 분석]
        ↓
[Recommendation 생성]
        ↓
[Human 승인 요청]
```

---

## 4. Step-by-Step 상세 흐름

### 4.1 Context Collection

AI는 먼저 현재 상태를 수집한다.

**수집 대상:**

- Alert 정보
- Metrics (Prometheus)
- Logs (Loki)
- Traces (Jaeger)
- 최근 배포 여부
- 트래픽 변화

### 4.2 Observability Query Rule (필수)

AI는 모든 판단에 대해 다음 데이터를 기반으로 해야 한다:

```
- PromQL (metrics)
- Log Query (Loki)
- Trace Query (Jaeger)
```

각 판단은 반드시 다음을 포함해야 한다:

```
- 어떤 metric을 봤는가
- 어떤 query로 조회했는가
- 어떤 기준으로 이상 판단했는가
```

예:

```
p95_latency > 300ms (5분)
→ PromQL:
histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))
```

### 4.3 RAG Retrieval

AI는 다음 기준으로 문서를 검색한다:

```
- failure_mode 기반 검색
- domain 기반 필터링
- severity / impact_scope 고려
- related_* 경로 확장 검색
```

우선순위:

```
1. exact match (failure_mode 일치)
2. same domain
3. similar failure pattern
```

결과:

```
- 관련 Scenario
- Runbook
- Improvement
- Preventive Design
- Postmortem
- rag/docs (보조)
```

### 4.4 Scenario Matching

**입력:** metric 상태, alert 조건

**수행:**
- `failure_mode` 식별
- severity 판단
- impact_scope 판단

**결과:** `"이 장애는 무엇인가?"`

### 4.5 Runbook Candidate Selection

**수행:**
- 해당 Scenario와 연결된 Runbook 검색
- 가능한 Action 목록 생성

**결과:** `"무엇을 할 수 있는가?"`

### 4.6 Improvement Filtering (핵심)

**수행:**
- Improvement 문서 조회
- Constraint Rule 적용
- 금지된 Action 제거
- 조건 기반 제한 적용

**예:**

```
retry 증가 상태
→ scale-out 금지
```

**결과:** `"무엇을 하면 안 되는가?"`

### 4.7 Preventive Design Evaluation

**수행:**
- 구조적 해결 존재 여부 확인
- 적용 조건 만족 여부 판단
- `adoption_level` 확인

**결과:** `"구조적으로 해결할 수 있는가?"`

### 4.8 Postmortem Adjustment

**수행:**
- 과거 유사 장애 검색
- 실패했던 대응 제거
- 성공했던 대응 가중치 증가

**결과:** `"현실에서는 어떻게 동작했는가?"`

### 4.9 rag/docs Analysis (보조)

**수행:**
- metric 해석
- 시스템 동작 이해
- 원인 후보 강화

> ⚠️ **중요:** `rag/docs`는 Action을 결정하지 않는다.

### 4.10 Decision Rule

AI는 다음 기준으로 Action을 선택한다:

```
- severity 기준
- impact_scope 기준
- metric 상태
- retry / error 증가 여부
- downstream 상태
```

예:

```
- retry_rate 증가 + DB latency 증가
  → scale-out 금지
  → fallback / rate limit 우선

- CPU saturation + queue backlog 증가
  → scale-out 고려
```

금지:

```
- 동일 metric만 보고 단일 원인 판단
- improvement 조건 무시
```

### 4.11 Final Decision Synthesis

모든 정보를 종합하여 최종 Recommendation 생성

---

## 5. Decision Priority

```
Preventive Design
> Improvement
> Postmortem
> Runbook
> Scenario
> rag/docs
```

---

## 6. AI Output Format (필수)

AI는 반드시 다음 구조로 출력해야 한다.

### 6.1 Incident Summary

- 장애 유형
- 영향 범위
- Severity

### 6.2 Most Likely Cause

- 원인 후보 (확정 X)

### 6.3 Evidence

- metric 근거
- log 근거
- trace 근거

### 6.4 Recommended Action


AI는 반드시 "순서"를 포함해야 한다.

```
Step 1:
Action:
Expected Effect:
Risk:
Rollback Plan:
Verification:

Step 2:
...
```

### 6.5 Alternative Actions

- 후보 Action 목록
- 선택 이유

### 6.6 Forbidden Actions (중요)

- Improvement 기반 금지 Action 목록

### 6.7 Human Approval Required

```
YES (항상)
```

### 6.8 Confidence Level (추가)

AI는 자신의 판단에 대해 신뢰도를 제공해야 한다.

예:

```
- HIGH: Scenario + Postmortem 일치
- MEDIUM: Scenario만 일치
- LOW: rag/docs 기반 추론 포함
```

목적:

```
Human이 판단 리스크를 이해하도록 한다.
```

---

## 7. Execution Rule

AI는 다음을 절대 수행하지 않는다:

```
- kubectl 실행
- 인프라 변경
- 설정 변경
```

AI의 역할:

```
✔ 분석
✔ 추천
✔ 리스크 설명
```

Human의 역할:

```
✔ 실행
✔ 최종 판단
✔ rollback 결정
```

> `AI Recommendation ≠ Execution`

반드시 Human 승인이 필요한 항목:

- scale-out / scale-in
- timeout 변경
- retry 정책 변경
- circuit breaker 변경
- DB / Redis 설정 변경
- 전체 시스템 대상으로 지속적인 추가..

---

## 8. Verification Failure Handling

Action 실패 시:

1. 동일 Action 반복 금지
2. Rollback 수행
3. 다음 Action으로 전환
4. Risk 증가 Action 금지

---

## 9. Safety Rule

| 우선순위 | 기준 |
|------|------|
| 1 | 데이터 보호 (결제) |
| 2 | 시스템 안정성 |
| 3 | 성능 |

특히 다음 리스크를 최우선으로 방지해야 한다:

```
- duplicate payment
- idempotency violation
- retry amplification
```

---

## 10. Anti-Pattern (중요)

- ❌ 무조건 scale-out
- ❌ retry 증가 상태에서 thread 증가
- ❌ `latency = CPU 문제`로 단정
- ❌ `rag/docs` 기반으로 Action 결정

---

## 11. Failure Mode 대응 원칙

> **No Scenario → No Action**

---

## 12. Escalation Rule

다음 조건에서는 Human 즉시 개입:

```
- SEV-1
- duplicate payment 위험
- root cause 불명확
- recommendation 충돌 발생
```

---

## 13. Input Contract (필수)

AI Recommendation API는 다음 입력을 받는다:

```
- alert_name
- service
- environment
- timestamp
- metrics_snapshot
- logs_sample
- trace_id
- deployment_info (optional)
```

원칙:

```
입력 데이터가 부족하면 판단 신뢰도 LOW로 설정한다.
```

---
