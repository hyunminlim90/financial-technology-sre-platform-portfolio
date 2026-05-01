# Financial Technology SRE Platform Architecture

> AI Agent + RAG + Human-in-the-loop 기반 운영 판단 시스템 (Operational Decision System)

---

## Architecture Overview

이 시스템은 

| 구분 | |
|------|------|
| 검색 시스템 | ❌ |
| 자동화 시스템 | ❌ |
| **판단 시스템** | ✅ |

---

## 핵심 구조

```
[Alert]
   ↓
[AI Agent]
   ↓
[RAG Retrieval]
   ↓
[Decision Engine]
   ↓
[Recommendation]
   ↓
[Human Execution]
   ↓
[Postmortem Learning]
```

---

## 핵심 개념

### 1. RAG = 판단 기반

> RAG는 **판단 기준을 구성하는 시스템**이다.

### 2. AI는 실행하지 않는다

```
AI Recommendation ≠ Execution
```

### 3. Knowledge Priority 기반 판단

```
Preventive Design
> Improvement
> Postmortem
> Runbook
> Scenario
> rag/docs
```

---

## 5-Layer RAG Architecture

| Layer | 구분 | 역할 | 출력 |
|------|------|------|------|
| Layer 1 | Scenario | 장애 정의, 증상, 영향 범위, propagation | "무슨 장애인가?" |
| Layer 2 | Runbook | 표준 대응 절차, Action / Rollback, 즉시 완화 | "무엇을 할 수 있는가?" |
| Layer 3 | Improvement | 위험한 행동 제한, 조건 기반 금지 규칙 | "무엇을 하면 안 되는가?" |
| Layer 4 | Preventive Design | 구조적 해결, 시스템 설계 변경 | "이 문제를 없애려면 어떻게 해야 하는가?" |
| Layer 5 | Postmortem | 실제 장애 경험, 판단 보정 | "실제에서는 어떻게 되었는가?" |
| Secondary | rag/docs | 메커니즘 설명, metric 해석 | "왜 이런 일이 발생하는가?" |

---

## Decision Engine (핵심)

### 역할

> 여러 Knowledge를 통합하여  
> **최종 Action을 결정하는 시스템**

### Decision Flow

```
1. Scenario 매칭
2. Runbook 후보 생성
3. Improvement 필터링
4. Preventive Design 우선 적용 여부 판단
5. Postmortem 기반 보정
6. rag/docs 기반 해석 (보조)
7. 최종 Recommendation 생성
```

### Decision Logic

| Step | 단계 | 내용 |
|------|------|------|
| 1 | Scenario | `failure_mode` 식별, severity 판단, impact_scope 판단 |
| 2 | Runbook | 가능한 Action 목록 생성 |
| 3 | Improvement | 금지 Action 제거, 위험 Action 제한 |
| 4 | Preventive Design | 구조적 해결이 존재하면 우선 적용 판단 |
| 5 | Postmortem | 과거 실패 사례 반영 |
| 6 | rag/docs | metric 해석, 원인 분석 보조 |

### 핵심 Rule

> `rag/docs`는 절대 **Action을 결정하지 않는다.**

---

## AI Agent Architecture

### 구성

```
agent/
├── workflows/
├── tools/
└── guardrails/
```

### 주요 역할

- 장애 분석
- RAG 검색
- Decision Engine 실행
- Recommendation 생성
- Postmortem Draft 생성

### Guardrails (중요)

- 위험 Action 차단
- Human 승인 요구
- 반복 Action 방지

---

## Incident Workflow

```
Alert 발생
↓
AI 분석
↓
RAG 검색
↓
Decision Engine 판단
↓
Recommendation 생성
↓
Human 실행
↓
반복 (필요 시)
↓
Incident 종료
↓
Postmortem 생성
↓
RAG 학습
```

---

## Learning Architecture

### Learning Loop

```
Incident
→ Postmortem
→ Improvement
→ Preventive Design
→ RAG 업데이트
→ 다음 Incident 개선
```

### 특징

| 구분 | 내용 |
|------|------|
| Runbook | 변하지 않는다 |
| Postmortem | 계속 추가된다 |
| AI | 점점 더 정확해진다 |

---

## System Layers

```
apps/        → Application
infra/       → Infrastructure
platform/    → Platform (SRE)
rag/         → RAG
agent/       → AI Agent
```

---

## Safety Model

### 기본 원칙

| 우선순위 | 기준 |
|------|------|
| 1 | 데이터 보호 |
| 2 | 시스템 안정성 |
| 3 | 성능 |

### Execution Rule

> 모든 인프라 변경은 **Human만 수행**

### AI 제한

> AI는 **Recommendation만 생성**한다.

---

## 핵심 요약

| Knowledge | 역할 |
|------|------|
| **Scenario** | 문제 정의 |
| **Runbook** | 가능한 행동 |
| **Improvement** | 제한 |
| **Preventive Design** | 제거 |
| **Postmortem** | 현실 |
| **rag/docs** | 이해 |