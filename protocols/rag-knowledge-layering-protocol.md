# protocols/rag-knowledge-layering-protocol.md

# RAG Knowledge Layering Protocol

---

## 1. Knowledge Structure

RAG는 다음 5개의 Primary Knowledge Source로 구성된다.

### Base Knowledge (고정)

1. Scenario
2. Runbook

특징:

- 표준화된 장애 정의 및 대응 절차
- 거의 수정하지 않음

---

### Learning Knowledge (누적)

3. Postmortem (이전 장애 사례)
4. Improvement / Preventive Design

특징:

- 장애 경험 기반으로 지속적으로 추가
- 기존 문서를 수정하지 않고 누적

---

## 2. 문서 변경 원칙

### Base Knowledge 수정 기준

다음 경우에만 수정 가능:

- 완전히 잘못된 내용
- 치명적인 오류
- 시스템 구조 변경 (예: Kafka → SQS)

그 외:

```
수정 ❌
추가 ✔
```

---

## 3. AI Agent 동작 방식

장애 발생 시 AI는 다음 순서로 분석한다:

1. Scenario 검색
2. Runbook 검색
3. Postmortem 검색
4. Improvement / Preventive Design 검색

### 3.1 Secondary Knowledge Rule

AI Agent는 장애 대응 판단 시 운영 판단 레이어를 우선 조회한다.

### Knowledge 계층

| 구분 | 경로 |
|------|------|
| **Primary Knowledge** | `scenarios/`, `runbooks/`, `improvements/`, `preventive-designs/`, `postmortems/` |
| **Secondary Knowledge** | `rag/docs/` |

### 조회 원칙

AI Agent는 먼저 **Primary Knowledge**를 기반으로 장애 유형, 대응 기준, 제한 규칙, 과거 사례를 판단한다.

`rag/docs/`는 다음 경우에만 보조로 참고한다:

- 지표 의미 해석이 필요한 경우
- 원인 후보가 여러 개이고 구분이 어려운 경우
- 장애 메커니즘 이해가 필요한 경우
- Primary Knowledge 간 판단이 충돌하는 경우
- 추가 설명 또는 deep diagnosis가 필요한 경우

### 우선순위 원칙

`rag/docs/`는 운영 판단을 보조할 수 있지만 **Primary Knowledge를 override 할 수 없다.**

```
Primary Knowledge = 판단 기준
rag/docs          = 이해 보조
```

### Safety Rule

`rag/docs/`의 일반 기술 설명이 `runbooks/`, `improvements/`, `preventive-designs/`의 안전 제약과 충돌할 경우, 반드시 **운영 문서의 안전 제약을 우선**한다.

### 한 줄 핵심

> **5개 문서 (scenarios, runbooks, improvements, preventive-designs, postmortems) = 판단 기준**
> **rag/docs = 기술 이해 보조**

### 3.2 Missing Primary Knowledge Rule

해당 failure_mode에 대한 Primary Knowledge가 존재하지 않을 경우:

```
AI Agent는 자동 대응을 수행하지 않는다.

대신 다음을 수행한다:
- Unknown Scenario로 분류
- Human에게 escalation
- 관련 로그 / metric / context 제공
```

원칙:

```
No Scenario → No Action
```

---

## 4. 판단 로직

각 레이어의 정보를 종합하여 최종 권장안을 생성한다.

예:

```
Runbook:
→ scale-out 가능

Postmortem:
→ 이전에 scale-out 후 DB 장애 발생 이력 있음

Improvement:
→ downstream 먼저 확인 필요

최종 판단:
→ scale-out 금지
→ external latency 확인 우선
```

### 4.1 Confidence-Based Safety Rule

AI Agent는 판단 신뢰도가 낮을 경우 보수적으로 행동해야 한다.

조건:

```
- failure_mode 매칭 불확실
- conflicting knowledge 존재
- metric 해석 불명확
```

행동:

```
- 자동 Action 제한
- Read-only 분석 우선 수행
- Human escalation
```

원칙:

```
Low Confidence → No Risky Action
```

---

## 5. 문서 작성 원칙

모든 문서는 사람이 작성하고 검증한다.

```
AI:
- 초안 생성
- 분석 보조

Human:
- 사실 검증
- 최종 승인
```

승인된 문서만 RAG에 포함한다.

---

## 6. 핵심 원칙

```
Runbook은 "정답"
Postmortem은 "경험"
Improvement는 "진화"
```

---

## 7. Repository Path Mapping

| Knowledge Source | Repository Path | 역할 |
|------|------|------|
| Scenario | `scenarios/` | 장애 상황 정의 |
| Runbook | `runbooks/` | 표준 대응 절차 |
| Postmortem | `postmortems/` | 이전 장애 사례 |
| Improvement / Preventive Design | `improvements/`, `preventive-designs/` | 재발 방지 및 예방 설계 |
| Protocol | `protocols/` | RAG 해석 규칙 및 문서 운영 원칙 |

---

## 8. RAG Retrieval Rule

AI Agent는 장애 분석 시 다음 우선순위로 문서를 검색한다.

```
1. protocols/             → 해석 규칙 확인
2. scenarios/             → 장애 유형 식별
3. runbooks/              → 표준 대응 절차 확인
4. postmortems/           → 유사 장애 사례 확인
5. improvements/          → 개선 이력 확인
6. preventive-designs/    → 예방 설계 확인
```

AI Agent는 `runbooks/`의 내용을 기본 대응 기준으로 삼되, `postmortems/`, `improvements/`, `preventive-designs/`에서 더 안전한 제약 조건이 발견되면 최종 권장안에 반드시 반영한다.

---

## 9. Document Metadata & Linking Rule

RAG 문서는 파일명만으로 연결하지 않는다. 모든 Knowledge Source 문서는 다음 기준으로 연결한다.

1. repository path
2. filename
3. document title
4. front matter metadata
5. body keywords

### 9.1 Required Front Matter

모든 RAG 대상 문서는 문서 상단에 YAML front matter를 포함해야 한다.

```yaml
---
title: Redis Timeout Scenario
knowledge_type: scenario
domain: redis
failure_mode: redis-timeout
services:
  - payment-api
  - redis
  - postgresql
related_scenarios: []
related_runbooks:
  - runbooks/redis/timeout.md
related_postmortems: []
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

### 9.2 Naming Rule

파일명은 가능한 한 다음 패턴을 따른다.

```
<domain>-<failure-mode>-<topic>.md
```

단, `scenarios/`와 `runbooks/`는 도메인 하위 디렉터리를 사용하므로 다음 패턴을 허용한다.

```
scenarios/<domain>/<failure-mode>.md
runbooks/<domain>/<failure-mode>.md
```

예:

```
scenarios/redis/timeout.md
runbooks/redis/timeout.md
preventive-designs/redis-timeout-idempotency-fallback.md
improvements/redis-timeout-idempotency-hardening.md
```

### 9.3 Linking Rule

새 문서를 추가할 때는 관련 문서를 명시적으로 연결해야 한다.

```yaml
related_scenarios:
  - scenarios/redis/timeout.md
related_runbooks:
  - runbooks/redis/timeout.md
related_preventive_designs:
  - preventive-designs/redis-timeout-idempotency-fallback.md
```

### 9.4 Retrieval Rule

AI Agent는 장애 분석 시 다음 순서로 문서를 연결한다.

1. `failure_mode` 일치
2. `domain` 일치
3. `related_*` 경로 일치
4. `tags` 일치
5. 본문 키워드 유사도

> 파일명만으로 연관성을 판단하지 않는다.

### 9.5 Safety Rule

`runbooks/`의 대응 절차보다 `postmortems/`, `improvements/`, `preventive-designs/`에서 더 안전한 제약 조건이 발견되면 최종 권장안에 반드시 반영한다.

| 출처 | 내용 |
|------|------|
| Runbook | worker scale-out 가능 |
| Postmortem | 이전 scale-out 이후 DB connection pool 고갈 발생 |
| Preventive Design | downstream 상태 확인 전 scale-out 금지 |
| **Final Recommendation** | scale-out 보류 → external provider latency와 DB connection pool pending 먼저 확인 |

### 9.6 Knowledge Priority Rule

| 우선순위 | Knowledge Source | 특성 |
|------|------|------|
| 1 | Preventive Design / Improvement | 가장 보수적 |
| 2 | Postmortem | 실제 장애 경험 |
| 3 | Runbook | 기본 대응 |
| 4 | Scenario | 장애 정의 |

> 더 안전한 방향이 항상 우선된다.

| 출처 | 내용 |
|------|------|
| Runbook | scale-out 가능 |
| Postmortem | scale-out 후 DB 장애 발생 |
| Improvement | downstream 확인 전 scale-out 금지 |
| **최종** | **scale-out 금지** |

### 9.7 Time Awareness Rule

Learning Knowledge는 시간 순서에 따라 중요도가 달라진다.

> 최신 Postmortem / Improvement가 더 높은 우선순위를 가진다.

적용 기준: `updated_at` / `created_at`

> 최근 장애 경험이 더 현실적인 판단 기준이다.

### 9.8 Context Matching Rule

AI Agent는 단순 키워드 매칭이 아닌 컨텍스트 기반으로 문서를 선택한다.

| 컨텍스트 | 예시 |
|------|------|
| `service` | payment-api, worker 등 |
| `environment` | prod, staging |
| `traffic pattern` | spike, steady |
| `failure scope` | partial, global |

> 동일 `failure_mode`라도 컨텍스트가 다르면 다른 판단을 할 수 있다.

---

## 10. Human Override Rule

AI Agent의 모든 권장 사항은 참고용이며 최종 판단은 사람이 수행한다.

> `AI Recommendation ≠ Final Decision`

적용 범위: 결제 관련 변경 / 데이터 변경 / 트래픽 제어 / scale-out / scale-in

**Human Approval Required**

### 10.1 Execution Responsibility Rule

AI Agent는 인프라 및 시스템 변경을 직접 수행하지 않는다. 모든 실행은 반드시 Human이 수행한다.

AI는 다음만 제공한다:

```
- Recommended Action
- Risk
- Rollback Plan
- Verification
```

Human은 다음을 수행한다:

```
- 실행 여부 판단
- 실제 Action 수행
- 결과 확인 및 Rollback 수행
```

원칙:

```
AI Recommendation ≠ Execution
```

---

## 11. Postmortem-Driven Learning Rule

본 시스템은 Postmortem 중심으로 학습한다.

### 11.1 Core Principle

```
Scenario / Runbook은 수정하지 않는다
Postmortem은 계속 추가한다
AI는 두 데이터를 비교하여 판단한다
```

### 11.2 Document Update Policy

| 문서 유형 | 수정 여부 | 정책 |
|------|------|------|
| scenarios/ | ❌ | 절대 수정 금지 (예외적 상황 제외) |
| runbooks/ | ❌ | 절대 수정 금지 (예외적 상황 제외) |
| improvements/ | ❌ | 기존 문서 수정 금지, 신규 문서 추가 |
| preventive-designs/ | ❌ | 기존 문서 수정 금지, 신규 문서 추가 |
| postmortems/ | ❌ | 장애 발생 시 지속적으로 신규 문서 추가 |

### 11.3 Postmortem Naming Rule

```
<failure-mode>-<core-issue>-<date>.md
```

예:

```
redis-timeout-scaleout-failure-2026-05-01.md
db-connection-pool-leak-2026-05-03.md
kafka-consumer-lag-rebalance-2026-05-05.md
```

### 11.4 RAG Linking Requirement

모든 Postmortem 문서는 반드시 다음을 포함해야 한다.

```
failure_mode / domain / related_scenarios / related_runbooks
related_improvements / related_preventive_designs / tags
```

이 조건을 만족해야 RAG에서 자동으로 연결된다.

### 11.5 Learning Mechanism

```
1. Scenario → 장애 정의 확인
2. Runbook → 기본 대응 확인
3. Postmortem → 과거 실패 사례 확인
4. Improvement → 개선된 대응 확인
5. Preventive Design → 구조적 제한 확인
```

### 11.6 Decision Override Rule

```
Postmortem / Improvement / Preventive Design이
Runbook보다 더 안전한 제약을 제시할 경우
→ 반드시 해당 제약을 우선 적용한다
```

### 11.7 Safety Rule

```
검증되지 않은 Postmortem은 RAG에 포함하지 않는다
```

### 11.8 System Behavior Summary

```
Runbook은 바뀌지 않는다
Postmortem은 쌓인다
AI는 경험을 기반으로 더 안전한 판단을 한다
```

### 11.9 Ultimate Goal

```
장애 대응 시스템 → 학습 시스템 → 사고 예방 시스템
```

---

## 12. Action & Rollback Pair Rule

AI Agent는 모든 대응 권장 시 반드시 다음을 함께 제공해야 한다.

```
1. Recommended Action (권장 조치)
2. Expected Effect (기대 효과)
3. Risk (리스크)
4. Rollback Plan (되돌리는 방법)
```

### 12.1 Rule

```
모든 Action에는 반드시 Rollback이 존재해야 한다
Rollback이 없는 Action은 권장하지 않는다
```

### 12.2 Example

```
[Action]
payment-api scale-out

[Expected Effect]
처리량 증가

[Risk]
DB connection pool saturation 가능

[Rollback Plan]
- scale-in to previous replica count
- HPA 비활성화
```

### 12.3 High-Risk Action Policy

다음 Action은 반드시 Rollback 포함:

```
- scale-out / scale-in
- retry 정책 변경
- timeout 변경
- circuit breaker 설정 변경
- DB / cache 설정 변경
```

### 12.4 AI Safety Rule

```
Rollback이 정의되지 않은 경우
→ AI는 해당 Action을 권장하지 않는다
```

### 12.5 Verification Rule

AI Agent는 각 Action에 대해 반드시 검증 방법을 함께 제공해야 한다.

검증 항목:

- 어떤 metric을 확인해야 하는가
- 정상 상태의 기준은 무엇인가
- 얼마나 기다려야 하는가

```
[Verification]
- API latency p95 < 300ms 확인
- error rate < 1% 확인
- 2~3분 관찰
```

### 12.6 Action Sequencing Rule

AI Agent는 여러 Action을 제시할 경우 실행 순서를 명확히 정의해야 한다.

```
Step 1 → Step 2 → Step 3
병렬 실행 가능 여부 명시

Example:
Step 1. External API latency 확인
Step 2. Retry rate 확인
Step 3. scale-out 여부 판단
※ scale-out은 Step 1~2 확인 후 수행
```

> 이 문서는 RAG 시스템이 반드시 참조해야 하는 핵심 정책 문서이다.

---

## Systems-Math Governance Rule

Systems-Math는 단순 수학 문서가 아니다.

**역할:**

- queue saturation explanation
- retry amplification reasoning
- tail latency interpretation
- failure propagation modeling
- reliability interpretation

AI Agent는 Scenario / Runbook / Experiment와 연결된 Systems-Math 문서를 retrieval 할 수 있다.

단, **Systems-Math는 운영 Action을 직접 결정하지 않는다.**

Systems-Math는 운영 현상을 정량적으로 설명하기 위한 **Operational Quantitative Analysis Layer**이다.

---

## Experiment Knowledge Rule

Experiment는 Recommendation과 Recovery 전략을 검증하기 위한 **Reliability Validation Layer**이다.

Experiment는 다음과 연결된다.

- Scenario / Runbook / Systems-Math / Observability / Postmortem

**Experiment 결과 평가 항목:**

- recovery time
- rollback effectiveness
- verification effectiveness
- recommendation safety

**Experiment 필수 조건:** Human-approved / sandboxed / bounded blast radius / rollback available / verification required

---

## Evidence Correlation Rule

AI Recommendation은 반드시 다음 간의 correlation 기반으로 생성되어야 한다.

- metrics / logs / traces / alert state / deployment events

**단일 metric만으로 root cause를 확정하지 않는다.**

---

## Degraded Retrieval Rule

partial retrieval, metadata inconsistency, embedding failure, observability 부족 상황에서는:

AI는 certainty를 낮추고, **degraded recommendation 상태를 명시**해야 한다.

Unknown을 추정으로 대체해서는 안 된다.

---

## Governance Timeline Rule

다음 이벤트들은 append-only governance timeline으로 기록될 수 있다.

- alert / recommendation / approval
- execution result / verification / incident resolution
- postmortem / experiment result

**Timeline 용도:** operator-facing audit / replay compatibility / governance observability

Timeline은 **운영 변경을 직접 실행하지 않는다.**

---

## Non-Goals

이 시스템은 다음을 목표로 하지 않는다.

- autonomous operations
- LLM-only operational decisions
- automatic destructive remediation
- human bypass
- uncontrolled chaos execution