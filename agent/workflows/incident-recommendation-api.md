# Incident Recommendation API

> AI Agent 기반 장애 대응 권장 API  
> Human-in-the-loop / RAG Decision Engine Entry Point

---

## 1. 목적

이 API는 Alert 발생 시 AI Agent가 장애 상황을 분석하고, Human SRE가 실행 여부를 판단할 수 있도록 대응 권장안을 생성한다.

> 이 API는 장애를 직접 해결하지 않는다.  
> **`AI Recommendation ≠ Execution`**

---

## 2. Endpoint

```
POST /api/v1/incidents/analyze
```

---

## 3. 역할

이 API는 다음을 수행한다:

1. Incident Context 수신
2. Observability 데이터 확인
3. RAG Knowledge 검색
4. Decision Engine 실행
5. 대응 권장안 생성
6. Human 검토용 결과 반환

---

## 4. Request Schema

```json
{
  "incident_id": "INC-2026-05-01-001",
  "alert_name": "RedisLatencyHigh",
  "service": "payment-api",
  "environment": "production",
  "severity_hint": "SEV-2",
  "occurred_at": "2026-05-01T10:01:00Z",

  "labels": {
    "domain": "redis",
    "cluster": "prod-apne2",
    "namespace": "payments",
    "pod": "payment-api-xxx"
  },

  "metrics_snapshot": {
    "p95_latency_ms": 420,
    "error_rate": 0.031,
    "retry_rate": 0.24,
    "redis_timeout_count": 128,
    "db_connection_usage": 0.91,
    "db_connection_pending": 17,
    "cpu_usage": 0.62
  },

  "logs_sample": [
    {
      "timestamp": "2026-05-01T10:01:12Z",
      "level": "ERROR",
      "message": "Redis command timed out",
      "trace_id": "trace-123"
    }
  ],

  "trace_ids": [
    "trace-123",
    "trace-456"
  ],

  "deployment_info": {
    "recent_deploy": true,
    "deploy_time": "2026-05-01T09:45:00Z",
    "version": "payment-api:1.3.7"
  },

  "operator_note": "checkout latency increased after Redis timeout alert"
}
```

---

## 5. Required Fields

| Field | Required | 설명 |
|------|------|------|
| `incident_id` | Yes | Incident 추적 ID |
| `alert_name` | Yes | Alert 이름 |
| `service` | Yes | 영향 서비스 |
| `environment` | Yes | production / staging / dev |
| `occurred_at` | Yes | Alert 발생 시각 |
| `metrics_snapshot` | Recommended | 판단 근거 |
| `logs_sample` | Optional | 로그 근거 |
| `trace_ids` | Optional | Trace 조회 기준 |
| `deployment_info` | Optional | 배포 영향 판단 |
| `operator_note` | Optional | 운영자 추가 설명 |

**원칙:**

- metrics / logs / traces 중 최소 하나 이상의 근거가 있어야 한다
- 근거가 부족하면 `confidence_level`은 `LOW`로 설정한다

---

## 6. Internal Processing Flow

| 순서 | 단계 |
|------|------|
| 1 | Validate Request |
| 2 | Normalize Incident Context |
| 3 | Observability Query 수행 |
| 4 | RAG Retrieval 수행 |
| 5 | Scenario Matching |
| 6 | Runbook Candidate Selection |
| 7 | Improvement Filtering |
| 8 | Preventive Design Evaluation |
| 9 | Postmortem Adjustment |
| 10 | rag/docs 보조 해석 |
| 11 | Decision Synthesis |
| 12 | Recommendation Response 생성 |

---

## 7. RAG Retrieval Rule

**검색 기준:**

1. `failure_mode` exact match
2. `domain` match
3. `related_*` path match
4. `tags` match
5. body keyword similarity

**검색 대상:**

| 구분 | 경로 |
|------|------|
| **Primary Knowledge** | `scenarios/`, `runbooks/`, `improvements/`, `preventive-designs/`, `postmortems/` |
| **Secondary Knowledge** | `rag/docs/` |

> **주의:** `rag/docs`는 기술 이해 보조이며 Action 결정에 사용하지 않는다.

---

## 8. Decision Priority

```
Preventive Design
> Improvement
> Postmortem
> Runbook
> Scenario
> rag/docs
```

> 더 안전한 규칙이 항상 우선된다.

---

## 9. Response Schema

```json
{
  "incident_id": "INC-2026-05-01-001",
  "status": "RECOMMENDATION_CREATED",

  "incident_summary": {
    "failure_mode": "redis-timeout",
    "domain": "redis",
    "service": "payment-api",
    "environment": "production",
    "severity": "SEV-2",
    "impact_scope": "partial"
  },

  "most_likely_causes": [
    {
      "cause": "Redis command latency 증가",
      "confidence": "HIGH",
      "reason": "Redis timeout count 증가와 API latency 증가가 동일 시간대에 발생"
    },
    {
      "cause": "Retry amplification으로 인한 DB connection pressure",
      "confidence": "MEDIUM",
      "reason": "retry_rate 증가와 db_connection_pending 증가가 함께 관측됨"
    }
  ],

  "evidence": {
    "metrics": [
      {
        "name": "p95_latency_ms",
        "value": 420,
        "threshold": 300,
        "status": "abnormal"
      },
      {
        "name": "retry_rate",
        "value": 0.24,
        "threshold": 0.2,
        "status": "abnormal"
      }
    ],
    "logs": [
      "Redis command timed out"
    ],
    "traces": [
      "trace-123",
      "trace-456"
    ]
  },

  "recommended_actions": [
    {
      "step": 1,
      "action": "Retry rate 제한 또는 rate limit 적용 검토",
      "expected_effect": "DB connection pressure 완화",
      "risk": "일부 요청 실패 또는 지연 증가 가능",
      "rollback_plan": "rate limit 설정을 이전 값으로 복원",
      "verification": [
        "retry_rate < 10%",
        "db_connection_pending == 0",
        "error_rate < 1%"
      ],
      "requires_human_approval": true
    },
    {
      "step": 2,
      "action": "Redis timeout 구간에서 fallback 경로 활성화 여부 확인",
      "expected_effect": "결제 요청 지연 완화",
      "risk": "cache miss 증가 및 DB 부하 증가 가능",
      "rollback_plan": "fallback 설정을 이전 상태로 복원",
      "verification": [
        "p95_latency_ms < 300",
        "redis_timeout_count 감소",
        "duplicate_payment_count == 0"
      ],
      "requires_human_approval": true
    }
  ],

  "alternative_actions": [
    {
      "action": "payment-api scale-out",
      "reason_not_selected": "retry 증가와 DB connection pressure가 있어 scale-out 시 DB 부하가 악화될 수 있음"
    }
  ],

  "forbidden_actions": [
    {
      "action": "retry 증가 상태에서 scale-out",
      "reason": "Improvement 문서에서 금지된 패턴"
    },
    {
      "action": "rag/docs 기반 단독 Action 결정",
      "reason": "rag/docs는 기술 이해 보조 문서"
    }
  ],

  "confidence_level": "HIGH",

  "human_approval_required": true,

  "referenced_knowledge": {
    "scenarios": ["scenarios/redis/timeout.md"],
    "runbooks": ["runbooks/redis/timeout.md"],
    "improvements": ["improvements/redis-timeout-idempotency-hardening.md"],
    "preventive_designs": ["preventive-designs/redis-timeout-idempotency-fallback.md"],
    "postmortems": [],
    "rag_docs": ["rag/docs/redis/latency-internals.md"]
  }
}
```

---

## 10. Response Field Rules

### 10.1 most_likely_causes

- ✔ Root Cause **후보**만 제시한다
- ❌ Root Cause 확정 금지
- 최종 Root Cause는 Postmortem에서 Human이 확정한다

### 10.2 recommended_actions

모든 Action은 반드시 다음을 포함해야 한다:

- Action
- Expected Effect
- Risk
- Rollback Plan
- Verification
- Human Approval Required

### 10.3 forbidden_actions

Improvement / Preventive Design에 의해 금지된 Action을 반드시 명시한다.

### 10.4 referenced_knowledge

AI가 어떤 문서를 근거로 판단했는지 반드시 반환한다.

---

## 11. Confidence Level

| Level | 기준 |
|------|------|
| `HIGH` | Scenario + Runbook + Improvement 또는 Postmortem이 명확히 일치 |
| `MEDIUM` | Scenario / Runbook은 일치하지만 Learning Knowledge 부족 |
| `LOW` | Primary Knowledge 부족, metric 불충분, rag/docs 의존도 높음 |

> **원칙:** `LOW` confidence → No Risky Action

---

## 12. Error Response

### 12.1 No Scenario Match

```json
{
  "status": "NO_RECOMMENDATION",
  "error_code": "NO_SCENARIO_MATCH",
  "message": "No Scenario → No Action",
  "human_escalation_required": true
}
```

### 12.2 Insufficient Evidence

```json
{
  "status": "NO_RECOMMENDATION",
  "error_code": "INSUFFICIENT_EVIDENCE",
  "message": "metrics/logs/traces evidence is insufficient",
  "human_escalation_required": true
}
```

### 12.3 Conflicting Knowledge

```json
{
  "status": "REQUIRES_HUMAN_REVIEW",
  "error_code": "CONFLICTING_KNOWLEDGE",
  "message": "Multiple knowledge sources provide conflicting recommendations",
  "human_escalation_required": true
}
```

---

## 13. Safety Rules

| 규칙 | |
|------|------|
| No Scenario | → No Action |
| Low Confidence | → No Risky Action |
| Rollback 없는 Action | → 추천 금지 |
| rag/docs 기반 Action 결정 | → 금지 |
| AI Recommendation | ≠ Execution |

**FinTech 최우선 보호 대상:**

- duplicate payment
- idempotency violation
- retry amplification

---

## 14. Execution Boundary

> **AI Agent는 절대 다음을 수행하지 않는다.**

- ❌ kubectl 실행
- ❌ scale-out / scale-in 실행
- ❌ DB 변경
- ❌ Redis 설정 변경
- ❌ traffic routing 변경
- ❌ GitOps commit / sync

> AI는 추천만 생성한다.  
> Human은 실행 여부를 판단하고 실제 Action을 수행한다.

---

## 15. Observability Query Requirement

AI는 가능한 경우 판단 근거에 Query를 포함해야 한다.

**p95 latency 확인:**

```promql
histogram_quantile(
  0.95,
  rate(http_server_requests_seconds_bucket{service="payment-api"}[5m])
)
```

**error rate 확인:**

```promql
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
/
sum(rate(http_server_requests_seconds_count[5m]))
```

**DB pending 확인:**

```promql
r2dbc_pool_pending_connections{service="payment-api"}
```

---

## 16. Idempotency Requirement

결제 시스템에서는 모든 권장안이 다음을 침해하지 않아야 한다:

- idempotency key 보장
- duplicate payment 방지
- 결제 상태 정합성

> **성능 개선보다 결제 정합성이 우선한다.**

---

## 17. Example Use Case

**상황:**

- Redis timeout 증가
- payment-api latency 증가
- retry_rate 증가
- DB connection pending 증가

**AI 판단:**

> scale-out은 처리량을 늘릴 수 있지만,  
> retry 증가와 DB pending이 이미 발생 중이므로 DB 부하를 악화시킬 수 있다.

**권장:**

1. retry 제한
2. fallback 상태 확인
3. DB pending 감소 확인
4. 이후 필요 시 scale-out 재평가