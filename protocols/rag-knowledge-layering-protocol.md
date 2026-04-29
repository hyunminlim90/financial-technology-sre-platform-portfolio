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

## 9. 요약

```text
프로토콜 문서 = RAG의 해석 규칙
Scenario / Runbook = 기본 지식
Postmortem / Improvement = 경험 기반 보정 지식
```

이 문서는 RAG 시스템이 반드시 참조해야 하는 핵심 정책 문서이다.
