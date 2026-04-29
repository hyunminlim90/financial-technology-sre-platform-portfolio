# protocols/rag-knowledge-layering-protocol.md

# RAG Knowledge Layering Protocol

---

## 1. Knowledge Structure

RAG는 다음 4개의 Knowledge Source로 구성된다.

### Base Knowledge (고정)

1. Scenario
2. Runbook

특징:

* 표준화된 장애 정의 및 대응 절차
* 거의 수정하지 않음

---

### Learning Knowledge (누적)

3. Postmortem (이전 장애 사례)
4. Improvement / Preventive Design

특징:

* 장애 경험 기반으로 지속적으로 추가
* 기존 문서를 수정하지 않고 누적

---

## 2. 문서 변경 원칙

### Base Knowledge 수정 기준

다음 경우에만 수정 가능:

* 완전히 잘못된 내용
* 치명적인 오류
* 시스템 구조 변경 (예: Kafka → SQS)

그 외:

```text
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

---

## 4. 판단 로직

각 레이어의 정보를 종합하여 최종 권장안을 생성한다.

예:

```text
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

---

## 5. 문서 작성 원칙

모든 문서는 사람이 작성하고 검증한다.

```text
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

```text
Runbook은 "정답"
Postmortem은 "경험"
Improvement는 "진화"
```

---

## 7. Repository Path Mapping

RAG Knowledge Source는 실제 프로젝트에서 다음 경로와 매핑된다.

| Knowledge Source                | Repository Path                        | 역할                   |
| ------------------------------- | -------------------------------------- | -------------------- |
| Scenario                        | `scenarios/`                           | 장애 상황 정의             |
| Runbook                         | `runbooks/`                            | 표준 대응 절차             |
| Postmortem                      | `postmortems/`                         | 이전 장애 사례             |
| Improvement / Preventive Design | `improvements/`, `preventive-designs/` | 재발 방지 및 예방 설계        |
| Protocol                        | `protocols/`                           | RAG 해석 규칙 및 문서 운영 원칙 |

---

## 8. RAG Retrieval Rule

AI Agent는 장애 분석 시 다음 우선순위로 문서를 검색한다.

```text
1. protocols/             → 해석 규칙 확인
2. scenarios/             → 장애 유형 식별
3. runbooks/              → 표준 대응 절차 확인
4. postmortems/           → 유사 장애 사례 확인
5. improvements/          → 개선 이력 확인
6. preventive-designs/    → 예방 설계 확인
```

AI Agent는 `runbooks/`의 내용을 기본 대응 기준으로 삼되,
`postmortems/`, `improvements/`, `preventive-designs/`에서 더 안전한 제약 조건이 발견되면
최종 권장안에 반드시 반영한다.

---

## 9. Document Metadata & Linking Rule

RAG 문서는 파일명만으로 연결하지 않는다.  
모든 Knowledge Source 문서는 다음 기준으로 연결한다.

1. repository path
2. filename
3. document title
4. front matter metadata
5. body keywords

---

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

---

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

**예:**

```
scenarios/redis/timeout.md
runbooks/redis/timeout.md
preventive-designs/redis-timeout-idempotency-fallback.md
improvements/redis-timeout-idempotency-hardening.md
```

---

### 9.3 Linking Rule

새 문서를 추가할 때는 관련 문서를 명시적으로 연결해야 한다.

**예:**

```yaml
related_scenarios:
  - scenarios/redis/timeout.md

related_runbooks:
  - runbooks/redis/timeout.md

related_preventive_designs:
  - preventive-designs/redis-timeout-idempotency-fallback.md
```

---

### 9.4 Retrieval Rule

AI Agent는 장애 분석 시 다음 순서로 문서를 연결한다.

1. `failure_mode` 일치
2. `domain` 일치
3. `related_*` 경로 일치
4. `tags` 일치
5. 본문 키워드 유사도

> 파일명만으로 연관성을 판단하지 않는다.

---

### 9.5 Safety Rule

`runbooks/`의 대응 절차보다 `postmortems/`, `improvements/`, `preventive-designs/`에서 더 안전한 제약 조건이 발견되면 최종 권장안에 반드시 반영한다.

**예:**

| 출처 | 내용 |
|------|------|
| Runbook | worker scale-out 가능 |
| Postmortem | 이전 scale-out 이후 DB connection pool 고갈 발생 |
| Preventive Design | downstream 상태 확인 전 scale-out 금지 |
| **Final Recommendation** | scale-out 보류 → external provider latency와 DB connection pool pending 먼저 확인 |

---

### 9.6 Knowledge Priority Rule

동일한 상황에서 Knowledge Source 간 내용이 충돌할 경우 다음 우선순위를 따른다.

| 우선순위 | Knowledge Source | 특성 |
|------|------|------|
| 1 | Preventive Design / Improvement | 가장 보수적 |
| 2 | Postmortem | 실제 장애 경험 |
| 3 | Runbook | 기본 대응 |
| 4 | Scenario | 장애 정의 |

---

### 원칙

> 더 안전한 방향이 항상 우선된다.

---

### 예시

| 출처 | 내용 |
|------|------|
| Runbook | scale-out 가능 |
| Postmortem | scale-out 후 DB 장애 발생 |
| Improvement | downstream 확인 전 scale-out 금지 |
| **최종** | **scale-out 금지** |

---

### 9.7 Time Awareness Rule

Learning Knowledge는 시간 순서에 따라 중요도가 달라진다.

> 최신 Postmortem / Improvement가 더 높은 우선순위를 가진다.

---

#### 적용 기준

- `updated_at`
- `created_at`

---

### 원칙

> 최근 장애 경험이 더 현실적인 판단 기준이다.

---

### 9.8 Context Matching Rule

AI Agent는 단순 키워드 매칭이 아닌 컨텍스트 기반으로 문서를 선택한다.

---

### 주요 컨텍스트

| 컨텍스트 | 예시 |
|------|------|
| `service` | payment-api, worker 등 |
| `environment` | prod, staging |
| `traffic pattern` | spike, steady |
| `failure scope` | partial, global |

---

### 원칙

> 동일 `failure_mode`라도 컨텍스트가 다르면 다른 판단을 할 수 있다.

---

### 10. Human Override Rule

AI Agent의 모든 권장 사항은 참고용이며 최종 판단은 사람이 수행한다.

---

### 원칙

> `AI Recommendation ≠ Final Decision`

---

### 적용 범위

- 결제 관련 변경
- 데이터 변경
- 트래픽 제어
- scale-out / scale-in

---

### 필수

> **Human Approval Required**

---

> 이 규약을 프로토콜에 포함하면 RAG가 훨씬 안정적으로 동작한다.

이 문서는 RAG 시스템이 반드시 참조해야 하는 핵심 정책 문서이다.
