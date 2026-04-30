# protocols/rag-docs-authoring-protocol.md

---

## 1. 목적

이 문서는 모든 `rag/docs` 문서를 동일한 구조와 기준으로 작성하기 위한 규칙을 정의한다.

> `rag/docs`는 설명 문서가 아니다.  
> `rag/docs`는 **"AI의 기술 이해를 위한 보조 지식"** 이다.

---

## 2. 역할 정의 (Primary Knowledge와 차이)

| 구분 | 역할 |
|------|------|
| **Scenario** | 장애 정의 |
| **Runbook** | 대응 방법 |
| **Improvement** | 행동 제한 |
| **Preventive Design** | 구조적 해결 |
| **Postmortem** | 실제 경험 |
| **rag/docs** | 기술 원리 / 메커니즘 이해 |

---

## 3. 핵심 개념

`rag/docs`는 다음을 설명한다:

- 시스템 내부 동작 원리
- 장애 발생 메커니즘
- 성능 병목 구조
- 기술 간 상호작용

**❗ 중요한 차이:**

```
Primary Knowledge = "무엇을 할 것인가"
rag/docs          = "왜 그런가"
```

---

## 4. 필수 구조

모든 `rag/docs` 문서는 반드시 다음 구조를 포함해야 한다.

1. 개요
2. 문제 상황 (Context)
3. 핵심 개념 (Core Concept)
4. 동작 원리 (How it works)
5. 장애 발생 메커니즘
6. 병목 발생 지점
7. 관련 Metrics
8. 관련 Logs / Traces
9. Primary Knowledge 연결
10. SRE 관점 해석
11. 요약

---

## 5. 핵심 규칙

### 5.1 Non-Action Rule (핵심)

`rag/docs`는 절대 **Action을 정의하지 않는다.**

| 금지 | 허용 |
|------|------|
| ❌ scale-out 수행 | ✔ latency 증가 원인 설명 |
| ❌ retry 증가 | ✔ retry amplification 구조 설명 |
| ❌ 설정 변경 | ✔ DB saturation 발생 이유 설명 |

> `rag/docs`는 **"판단"** 이 아니라 **"이해"** 를 제공한다.

### 5.2 No Override Rule

`rag/docs`는 Primary Knowledge를 **override 할 수 없다.**

```
Primary Knowledge > rag/docs
```

| 출처 | 내용 |
|------|------|
| rag/docs | scale-out 가능 |
| Improvement | scale-out 금지 |
| **Final** | **scale-out 금지** |

### 5.3 Mechanism Rule (필수)

모든 문서는 반드시 **내부 동작 메커니즘**을 포함해야 한다.

**예:**

```
Redis latency 증가 이유:
→ network RTT 증가
→ connection pool starvation
→ event loop blocking
```

### 5.4 Causal Chain Rule

장애는 반드시 **원인 → 결과 흐름**으로 설명해야 한다.

```
Redis timeout
→ retry 증가
→ duplicate request 증가
→ DB write 증가
→ DB lock 증가
→ latency 증가
```

### 5.5 Observability Mapping Rule

모든 개념은 반드시 **관측 데이터와 연결**되어야 한다.

```
개념 → metric → 해석
```

**예:**

```
connection pool saturation
→ r2dbc.pool.pending
→ 요청 대기 발생
```

### 5.6 Multi-System Interaction Rule

여러 시스템 간 상호작용을 설명해야 한다.

**예:**

```
Redis 실패
→ API retry 증가
→ DB write 증가
→ DB saturation
```

### 5.7 FinTech Insight Rule (필수)

결제 시스템에서는 반드시 포함:

- duplicate payment 발생 구조
- idempotency 실패 원인
- retry amplification 영향

### 5.8 Deep Diagnosis Rule

`rag/docs`는 다음 상황에서 사용된다:

- 원인 후보가 여러 개일 때
- metric 해석이 어려울 때
- 시스템 동작 이해가 필요할 때

### 5.9 Anti-Pattern Rule

잘못된 해석을 명시적으로 금지해야 한다.

- ❌ `latency 증가 = CPU 문제`
- ❌ `Kafka lag = consumer 문제`
- ❌ `timeout = 네트워크 문제`

### 5.10 Context Binding Rule

rag/docs의 내용은 반드시 특정 컨텍스트에서만 해석되어야 한다.

AI Agent는 다음 조건이 일치할 때만 rag/docs를 활용한다:

```
- domain 일치
- failure_mode 연관성 존재
- service context 일치
- environment (prod / staging) 일치
```

원칙:

```
동일한 기술 개념이라도
컨텍스트가 다르면 잘못된 해석이 될 수 있다
```

이유:

```
Redis latency (prod) → critical
Redis latency (staging) → 무시 가능
```

### 5.11 Misleading Prevention Rule

rag/docs의 일반적인 기술 설명은 실제 장애 상황을 정확히 반영하지 않을 수 있다.

AI Agent는 다음을 반드시 확인해야 한다:

```
- 실제 metric 데이터와 일치하는가
- Scenario 정의와 일치하는가
- 현재 failure_mode와 연관성이 있는가
```

금지:

```
- 일반적인 기술 이론을 그대로 장애 원인으로 단정
- rag/docs 기반으로 root cause 확정
```

원칙:

```
이론보다 실제 데이터가 항상 우선이다
```

---

## 6. Front Matter Required Fields (필수)

```yaml
---
title: Redis Latency Internals
knowledge_type: rag-doc
domain: redis
topic: latency-internals
environment: production

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
  - latency
  - event-loop
  - network
  - performance
---
```

---

## 7. RAG Integration Rule

`rag/docs`는 다음 기준으로 연결된다:

- `domain`
- `topic`
- `related_*` 경로
- `tags`
- 본문 키워드

---

## 8. Usage Rule (AI 사용 규칙)

AI Agent는 `rag/docs`를 다음 경우에만 사용한다:

- metric 해석이 필요한 경우
- 원인 분석이 필요한 경우
- 시스템 동작 이해가 필요한 경우

> ❌ `rag/docs` 기반으로 Action 결정 금지

---

## 9. Naming Rule

```
rag/docs/<domain>/<topic>.md
```

**예:**

```
rag/docs/redis/latency-internals.md
rag/docs/kafka/consumer-rebalance.md
rag/docs/database/connection-pool-mechanism.md
rag/docs/webflux/event-loop-model.md
```

---

## 10. 금지 사항

| 금지 | 이유 |
|------|------|
| ❌ Action 정의 | Runbook 영역 |
| ❌ 대응 방법 포함 | 역할 침범 |
| ❌ Primary override | 안전성 위반 |
| ❌ 추상 설명만 | RAG 활용 불가 |
| ❌ 관측 데이터 없음 | 분석 불가 |

---

## 11. 핵심 원칙

| 문서 | 역할 |
|------|------|
| **Scenario** | 문제 정의 |
| **Runbook** | 해결 |
| **Improvement** | 제한 |
| **Preventive Design** | 제거 |
| **rag/docs** | 이해 |

---

## 🎯 한 줄 핵심

> `rag/docs`는 **"어떻게 고칠까"** 가 아니라  
> **"왜 이런 문제가 생기는가"** 를 설명한다.

---

## 🔥 진짜 중요한 한 줄

> Primary Knowledge 없이 `rag/docs`만 있으면 **사고 난다.**  
> `rag/docs` 없이 Primary Knowledge만 있으면 **오판한다.**
>
> 👉 **둘 다 있어야 정확 + 안전**