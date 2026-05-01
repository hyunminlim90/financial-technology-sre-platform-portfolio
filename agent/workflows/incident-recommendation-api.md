# Incident Recommendation API

> AI Agent 기반 장애 대응 권장 API  
> (Human-in-the-loop / RAG Decision Engine Entry Point)

---

## 1. 목적

이 API는 장애 발생 시  
AI Agent가 상황을 분석하고 대응 권장안을 생성하기 위한 진입점이다.

> 이 API는 장애를 해결하지 않는다.  
> Human이 판단할 수 있도록 **"추천"을 생성한다.**

---

## 2. 핵심 원칙

> AI는 실행하지 않는다  
> AI는 추천한다  
> **Human이 실행한다**

---

## 3. API Overview

**Endpoint:**

```
POST /incident/analyze
```

**역할:**

1. 장애 상황 입력 받기
2. Observability 데이터 기반 분석
3. RAG 문서 검색
4. Decision Engine 실행
5. 대응 권장 문서 생성
6. Human에게 반환

---

## 4. Request Schema

```json
{
  "incident_id": "string",
  "alert_name": "string",
  "service": "string",
  "environment": "production | staging | dev",
  "timestamp": "ISO8601",

  "metrics_snapshot": {
    "p95_latency": 420,
    "error_rate": 0.12,
    "retry_rate": 0.25,
    "cpu_usage": 0.65,
    "db_connection_usage": 0.92
  },

  "logs_sample": [
    "Redis timeout error",
    "connection pool exhausted"
  ],

  "trace_ids": [
    "trace-123",
    "trace-456"
  ],

  "deployment_info": {
    "recent_deploy": true,
    "deploy_time": "ISO8601"
  }
}
```

**입력 원칙:**

- ✔ Observability 기반 입력 필수
- ✔ metrics / logs / traces 최소 1개 이상 필요
- ✔ 데이터 부족 시 confidence `LOW`

---

## 5. Processing Flow (내부 동작)

이 API는 다음 순서로 동작한다:

| 순서 | 단계 |
|------|------|
| 1 | Context Collection |
| 2 | Observability Query 수행 |
| 3 | RAG Retrieval |
| 4 | Scenario Matching |
| 5 | Runbook Candidate 생성 |
| 6 | Improvement Filtering (금지 Action 제거) |
| 7 | Preventive Design 적용 여부 판단 |
| 8 | Postmortem 기반 보정 |
| 9 | rag/docs 기반 해석 (보조) |
| 10 | Decision Engine 실행 |
| 11 | Recommendation 생성 |

---

## 6. Response Schema

```json
{
  "incident_summary": {
    "failure_mode": "redis-timeout",
    "severity": "SEV-2",
    "impact_scope": "partial"
  },

  "most_likely_causes": [
    "Redis latency 증가",
    "connection pool saturation"
  ],

  "evidence": {
    "metrics": [
      "p95_latency > 300ms (5분)",
      "retry_rate > 20%"
    ],
    "logs": [
      "Redis timeout error"
    ],
    "traces": [
      "slow downstream call"
    ]
  },

  "recommended_actions": [
    {
      "step": 1,
      "action": "Retry rate 제한 (rate limiting 적용)",
      "expected_effect": "DB overload 방지",
      "risk": "일부 요청 실패 증가",
      "rollback_plan": "rate limit 해제",
      "verification": "retry_rate 감소 확인"
    },
    {
      "step": 2,
      "action": "Redis 상태 점검 및 fallback 활성화",
      "expected_effect": "latency 감소",
      "risk": "cache miss 증가",
      "rollback_plan": "fallback 비활성화",
      "verification": "p95 latency 감소"
    }
  ],

  "alternative_actions": [
    "scale-out",
    "connection pool 증가"
  ],

  "forbidden_actions": [
    "retry 증가 상태에서 scale-out",
    "thread pool 확장"
  ],

  "confidence_level": "HIGH",

  "human_approval_required": true
}
```

---

## 7. Response 필드 설명

### 7.1 Incident Summary

- `failure_mode`
- `severity`
- `impact_scope`

### 7.2 Most Likely Causes

- ✔ Root Cause **후보**
- ❌ 확정 금지

### 7.3 Evidence

- ✔ metrics 근거
- ✔ logs 근거
- ✔ traces 근거

> Observability 기반 필수

### 7.4 Recommended Actions

- ✔ 반드시 Step 순서 포함
- ✔ Rollback 포함
- ✔ Verification 포함

### 7.5 Forbidden Actions

- ✔ Improvement 기반 금지 항목
- ✔ Safety 핵심

### 7.6 Confidence Level

| 수준 | 조건 |
|------|------|
| `HIGH` | Scenario + Postmortem 일치 |
| `MEDIUM` | Scenario 기반 |
| `LOW` | rag/docs 추론 포함 |

### 7.7 Human Approval

```
항상 true
```

---

## 8. Decision Rule

AI는 다음 기준으로 Action을 선택한다:

- severity
- impact_scope
- retry 증가 여부
- downstream 상태
- 데이터 정합성 리스크

---

## 9. Safety Rule (최우선)

| 우선순위 | 기준 |
|------|------|
| 1 | 데이터 보호 (결제) |
| 2 | 시스템 안정성 |
| 3 | 성능 |

**특히 방지해야 하는 것:**

- duplicate payment
- idempotency violation
- retry amplification

---

## 10. Execution Rule

> **AI는 절대 실행하지 않는다.**

- ❌ kubectl 실행
- ❌ scale-out 실행
- ❌ config 변경
- ❌ DB 변경

---

## 11. Error Handling

### 11.1 입력 부족

```json
{
  "error": "INSUFFICIENT_DATA",
  "message": "metrics/logs/traces 부족"
}
```

### 11.2 Scenario 미매칭

```json
{
  "error": "NO_SCENARIO_MATCH",
  "message": "No Scenario → No Action"
}
```

---

## 12. Anti-Pattern

- ❌ 단일 metric 기반 판단
- ❌ `rag/docs` 기반 Action 결정
- ❌ 무조건 scale-out
- ❌ Root Cause 확정