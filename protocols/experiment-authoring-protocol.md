# protocols/experiment-authoring-protocol.md

---

## 1. 목적

이 문서는 모든 Experiment 문서를 동일한 구조와 판단 기준으로 작성하기 위한 규칙을 정의한다.

> Experiment는 단순 장애 재현 문서가 아니다.  
> Experiment는 **"운영 안정성과 AI Recommendation을 검증하는 실험 계약(Validation Contract)"** 이다.

---

## 2. 역할 정의 (다른 문서와 차이)

| 문서 | 역할 |
|------|------|
| **Scenario** | 문제 정의 |
| **Runbook** | 대응 전략 |
| **Improvement** | 행동 제한 |
| **Preventive Design** | 구조적 제거 |
| **Postmortem** | 실제 경험 |
| **Systems-Math** | 정량 모델 |
| **Experiment** | 실험 기반 검증 |

---

## 3. 핵심 개념

Experiment는 다음을 검증한다.

- 장애가 실제로 재현되는가
- Recommendation이 안전한가
- Rollback이 실제로 가능한가
- Verification이 신뢰 가능한가
- Preventive Design이 효과적인가
- Systems-Math 설명이 실제 관측과 일치하는가

---

## 4. 핵심 원칙

### 4.1 Human Approval Rule (필수)

모든 Experiment는 반드시 Human Approval이 필요하다.

```text
Experiment Automation ≠ Autonomous Chaos
```

금지:

- uncontrolled injection
- production destructive execution
- human bypass

### 4.2 Sandbox Rule (필수)

Experiment는 반드시 다음 중 하나에서 수행해야 한다.

```text
- sandbox
- staging
- isolated environment
- bounded production test
```

원칙:

```text
blast radius는 항상 제한되어야 한다.
```

### 4.3 Rollback Mandatory Rule

모든 Experiment는 반드시 rollback 계획을 포함해야 한다.

```text
Rollback 없는 Experiment 금지
```

### 4.4 Verification Rule

Experiment는 반드시 검증 가능해야 한다.

검증 대상:

```text
- alert recovery
- latency recovery
- queue stabilization
- retry 감소
- trace 정상화
- error budget 회복
```

### 4.5 Observability Rule

Experiment는 반드시 observability 기반이어야 한다.

필수 Evidence:

```text
- metrics
- logs
- traces
- deployment events
- scaling events
```

### 4.6 FinTech Safety Rule

결제 시스템에서는 반드시 다음을 포함해야 한다.

```text
- duplicate payment 영향
- idempotency 영향
- retry amplification 영향
- settlement consistency 영향
```

### 4.7 Recommendation Validation Rule

Experiment는 AI Recommendation을 평가할 수 있다.

평가 대상:

```text
- recommendation safety
- rollback effectiveness
- verification correctness
- recovery time
- unintended side effect
```

원칙:

```text
AI Recommendation은 항상 옳다고 가정하지 않는다.
```

### 4.8 Systems-Math Validation Rule

Experiment는 Systems-Math 모델을 검증할 수 있다.

예:

```text
- queue-utilization
- Little's Law
- retry amplification
- tail latency propagation
```

실험 결과는:

```text
예상 모델
vs
실제 관측
```

비교를 포함할 수 있다.

### 4.9 Measurement Rule (핵심)

Experiment는 반드시 정량 결과를 포함해야 한다.

필수 항목:

```text
Before
During
After
```

예:

```text
- P99 latency
- error rate
- consumer lag
- retry rate
- queue depth
- recovery time
```

### 4.10 Blast Radius Rule

Experiment는 예상 영향 범위를 정의해야 한다.

구분:

```text
- local
- partial
- global
```

원칙:

```text
blast radius가 클수록:
approval,
rollback,
verification 요구사항이 강화된다.
```

---

## 5. 필수 구조

모든 Experiment 문서는 반드시 다음 구조를 포함해야 한다.

1. 개요
2. 실험 목적
3. 대상 Scenario
4. 대상 Systems-Math
5. 가설 (Hypothesis)
6. 실험 환경
7. 장애 주입 방식
8. 예상 증상
9. 예상 Metrics / Logs / Traces
10. Recommendation 평가 기준
11. Rollback 계획
12. Verification 절차
13. Blast Radius
14. Before Metrics
15. During Metrics
16. After Metrics
17. 결과 분석
18. Systems-Math 비교 분석
19. 교훈
20. 후속 Improvement / Preventive Design
21. 요약

---

## 6. Experiment Type Rule

Experiment는 유형을 정의해야 한다.

구분:

```text
- fault-injection
- load-test
- rollback-validation
- resilience-validation
- recommendation-validation
- observability-validation
- migration-validation
```

---

## 7. Front Matter Required Fields (필수)

```yaml
---
title: Redis Timeout Recommendation Validation
knowledge_type: experiment

experiment_type: recommendation-validation

domain: redis
failure_mode: redis-timeout
environment: staging

severity: SEV-2
impact_scope: partial

sandbox_only: true
approval_required: true

services:
  - payment-api
  - redis
  - postgresql

related_scenarios:
  - scenarios/redis/timeout.md

related_runbooks:
  - runbooks/redis/timeout.md

related_improvements:
  - improvements/redis-timeout-idempotency-hardening.md

related_preventive_designs:
  - preventive-designs/redis-timeout-idempotency-fallback.md

related_systems_math:
  - systems-math/retry-amplification.md

related_postmortems: []

tags:
  - redis
  - timeout
  - retry
  - validation
  - rollback
  - experiment
---
```

---

## 8. RAG Integration Rule

Experiment는 다음 기준으로 연결된다.

- `failure_mode`
- `domain`
- `severity`
- `impact_scope`
- `experiment_type`
- `related_*` 경로
- `tags`

---

## 9. AI Retrieval Rule

AI는 다음 상황에서 Experiment를 retrieval 할 수 있다.

```text
- recommendation validation
- rollback evaluation
- repeated incident
- migration verification
- preventive design effectiveness
- systems-math validation
```

단:

```text
Experiment 단독으로 Action을 생성해서는 안 된다.
```

---

## 10. Governance Rule

Experiment는 append-only governance timeline에 기록될 수 있다.

예:

```text
- experiment planned
- approval granted
- injection started
- rollback executed
- verification completed
- experiment closed
```

목적:

```text
- auditability
- replay compatibility
- operational governance
```

---

## 11. Research Dataset Rule

Experiment는 Reliability Engineering 연구 데이터셋 생성에 사용될 수 있다.

예:

```text
- recovery time
- retry amplification reduction
- queue stabilization
- blast radius reduction
- recommendation accuracy
- rollback effectiveness
```

---

## 12. Anti-Pattern Rule

금지:

```text
❌ production uncontrolled chaos
❌ rollback 없는 injection
❌ observability 없는 experiment
❌ hypothesis 없는 실험
❌ metrics 없는 실험
❌ human approval 없는 execution
```

---

## 13. Non-Goals

Experiment는 다음을 목표로 하지 않는다.

```text
- autonomous chaos engineering
- uncontrolled production mutation
- human bypass
- destructive remediation
- LLM-only operational decision
```

---

## 14. 핵심 원칙

| 문서 | 역할 |
|------|------|
| **Runbook** | 대응 |
| **Improvement** | 제한 |
| **Preventive Design** | 제거 |
| **Systems-Math** | 설명 |
| **Experiment** | 검증 |

---

## 🎯 한 줄 핵심

> Experiment의 목적은 장애를 만드는 것이 아니다.  
> → 운영 안정성과 Recommendation의 안전성을 검증하는 것이다.