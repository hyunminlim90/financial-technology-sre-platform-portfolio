# protocols/systems-math-authoring-protocol.md

---

## 1. 목적

이 문서는 모든 Systems-Math 문서를 동일한 구조와 정량 분석 기준으로 작성하기 위한 규칙을 정의한다.

> Systems-Math는 단순 수학 문서가 아니다.  
> Systems-Math는 **"운영 안정성 현상을 정량적으로 설명하는 분석 계층"** 이다.

---

## 2. 역할 정의 (다른 문서와 차이)

| 문서 | 역할 |
|------|------|
| **Scenario** | 문제 정의 |
| **Runbook** | 대응 전략 |
| **Improvement** | 행동 제한 |
| **Preventive Design** | 구조 제거 |
| **Postmortem** | 실제 경험 |
| **Experiment** | 검증 |
| **Systems-Math** | 정량 설명 |

---

## 3. 핵심 개념

Systems-Math는 다음을 설명한다.

- 왜 장애가 발생하는가
- 왜 latency가 증가하는가
- 왜 queue가 포화되는가
- 왜 retry amplification이 발생하는가
- 왜 cascading failure가 발생하는가
- 왜 특정 구조가 더 안정적인가

---

## 4. 핵심 원칙

### 4.1 Operational Meaning Rule (핵심)

Systems-Math는 수학 자체를 목표로 하지 않는다.

```text
수식 자체보다
운영 현상과의 연결이 우선이다.
```

금지:

```text
❌ 순수 수학 설명만 존재
❌ 운영 연결 없는 증명
❌ academic-only derivation
```

허용:

```text
✔ latency 설명
✔ queue saturation 설명
✔ retry amplification 설명
✔ backpressure propagation 설명
```

### 4.2 Quantitative Rule

Systems-Math는 반드시 정량 모델을 포함해야 한다.

예:

```text
- Little's Law
- queue-utilization
- percentile
- variance
- tail latency
- availability
- retry growth
```

### 4.3 Observability Rule

Systems-Math는 반드시 observability와 연결되어야 한다.

필수 연결 대상:

```text
- metrics
- logs
- traces
- alerts
- SLI/SLO
```

예:

```text
- P99 latency
- consumer lag
- retry rate
- queue depth
- timeout rate
```

### 4.4 Scenario Integration Rule

Systems-Math는 반드시 Scenario와 연결되어야 한다.

예:

```yaml
related_scenarios:
  - scenarios/kafka/consumer-lag.md
```

원칙:

```text
수학은 반드시 실제 장애 현상을 설명해야 한다.
```

### 4.5 Runbook Integration Rule

Systems-Math는 Runbook 판단 근거를 설명할 수 있다.

예:

```text
- 왜 scale-out이 위험한가?
- 왜 retry 제한이 필요한가?
- 왜 fallback이 필요한가?
```

### 4.6 Experiment Validation Rule

Systems-Math는 Experiment 결과와 연결될 수 있다.

예:

```text
예상 queue utilization
vs
실제 관측 queue utilization
```

원칙:

```text
정량 모델은 실험으로 검증 가능해야 한다.
```

### 4.7 SLO Rule

Systems-Math는 SLO 영향을 설명할 수 있어야 한다.

예:

```text
- availability
- latency
- recovery time
- error budget
- tail latency
```

### 4.8 Failure Propagation Rule

Systems-Math는 장애 전파를 설명할 수 있다.

예:

```text
Redis timeout
→ retry 증가
→ queue saturation
→ DB overload
→ cascading failure
```

### 4.9 Tail-Latency Rule

Systems-Math는 평균보다 percentile 설명을 우선할 수 있다.

원칙:

```text
평균 latency는
운영 위험을 숨길 수 있다.
```

설명 대상:

```text
- P95
- P99
- tail amplification
```

### 4.10 Queueing Rule

Queue 기반 시스템은 반드시 queueing behavior를 설명할 수 있어야 한다.

예:

```text
- arrival rate
- service rate
- queue depth
- utilization
```

### 4.11 Retry Amplification Rule

Systems-Math는 retry amplification을 설명할 수 있어야 한다.

예:

```text
- retry storm
- feedback loop
- cascading retry
```

원칙:

```text
retry는 recovery를 돕기도 하지만
시스템을 붕괴시키기도 한다.
```

### 4.12 Human-Centric Rule

Systems-Math는 운영자가 이해 가능해야 한다.

금지:

```text
❌ 논문 수준 수식만 존재
❌ operational meaning 없는 notation
```

원칙:

```text
운영자가 실제로 사용할 수 있어야 한다.
```

---

## 5. 필수 구조

모든 Systems-Math 문서는 반드시 다음 구조를 포함해야 한다.

1. 개요
2. 운영 현상 정의
3. 핵심 정량 개념
4. 관련 수식
5. Operational Meaning
6. 장애 전파 설명
7. Observability 연결
8. Metrics 해석
9. Scenario 연결
10. Runbook 연결
11. Experiment 연결
12. SLO 영향
13. Trade-off
14. 실제 운영 사례
15. 핵심 통찰
16. 요약

---

## 6. Systems-Math Category Rule

Systems-Math는 다음 카테고리를 가질 수 있다.

```text
- queue-theory
- latency-analysis
- retry-analysis
- availability-analysis
- backpressure-analysis
- throughput-analysis
- concurrency-analysis
- probability-analysis
- resilience-analysis
```

---

## 7. Front Matter Required Fields (필수)

```yaml
---
title: Retry Amplification Analysis
knowledge_type: systems-math

math_category: retry-analysis

domain: distributed-system
failure_mode: retry-amplification

related_scenarios:
  - scenarios/redis/timeout.md

related_runbooks:
  - runbooks/redis/timeout.md

related_improvements:
  - improvements/redis-timeout-idempotency-hardening.md

related_preventive_designs:
  - preventive-designs/redis-timeout-idempotency-fallback.md

related_experiments:
  - experiments/retry-storm-validation.md

tags:
  - retry
  - latency
  - queue
  - backpressure
  - reliability
---
```

---

## 8. Deep-Dive Link Rule

Systems-Math는 deep-dive 개념 문서와 연결될 수 있다.

예:

```yaml
related_deep_dive_docs:
  - rag/docs/100-deep-dive/systems-math/little-law.md
  - rag/docs/100-deep-dive/systems-math/queue-utilization.md
```

원칙:

```text
루트 systems-math는 운영 현상 설명,
deep-dive systems-math는 개념 학습 역할을 가진다.
```

---

## 9. RAG Integration Rule

Systems-Math는 다음 기준으로 연결된다.

- `failure_mode`
- `domain`
- `math_category`
- `related_*` 경로
- `tags`

---

## 10. AI Retrieval Rule

AI는 Systems-Math를 다음 용도로 retrieval 할 수 있다.

```text
- metric interpretation
- queue analysis
- retry analysis
- latency reasoning
- propagation analysis
- recommendation explanation
```

단:

```text
Systems-Math 단독으로 Action을 결정해서는 안 된다.
```

---

## 11. Research Dataset Rule

Systems-Math는 Reliability Engineering 연구 데이터셋 생성에 사용될 수 있다.

예:

```text
- queue stabilization
- retry amplification reduction
- recovery time analysis
- availability modeling
- tail latency reduction
```

---

## 12. Governance Rule

Systems-Math는 다음 운영 지식과 연결된다.

```text
Scenario
↔ Runbook
↔ Improvement
↔ Preventive Design
↔ Experiment
↔ Postmortem
```

---

## 13. Anti-Pattern Rule

금지:

```text
❌ 운영 연결 없는 수학
❌ metric 없는 설명
❌ observability 없는 모델
❌ experiment 검증 불가능 모델
❌ action-only 설명
```

---

## 14. Non-Goals

Systems-Math는 다음을 목표로 하지 않는다.

```text
- pure mathematics research
- theorem-only derivation
- operationally irrelevant proofs
- LLM-only decision making
- autonomous operational execution
```

---

## 15. 핵심 원칙

| 문서 | 역할 |
|------|------|
| **Scenario** | 문제 정의 |
| **Runbook** | 대응 |
| **Improvement** | 제한 |
| **Preventive Design** | 제거 |
| **Experiment** | 검증 |
| **Systems-Math** | 정량 설명 |

---

## 🎯 한 줄 핵심

> Systems-Math의 목적은 수학 자체가 아니다.  
> → 운영 안정성 현상을 정량적으로 설명하는 것이다.