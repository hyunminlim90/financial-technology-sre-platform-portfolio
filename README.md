# 🚀 Financial Technology SRE Platform

> SRE + Platform Engineering + AI Agent + RAG 기반의  
> Human-in-the-loop 운영 지능형 장애 대응 & 학습 플랫폼

---

## 🧭 Project Overview

이 프로젝트는 단순 장애 대응 시스템이 아닙니다.

> 👉 AI + SRE + Human 협업 기반의 **"운영 지능 시스템"** 입니다.

---

## 🎯 목표

1. 장애를 자동으로 감지하고 분석한다
2. AI Agent가 대응을 **"추천"** 한다 (자동 실행 ❌)
3. Human이 최종 판단하고 실행한다
4. 모든 장애는 Postmortem으로 학습된다
5. 시스템은 RAG 기반으로 지속 진화한다

---

## 🔥 Core Principle

> AI는 실행하지 않는다  
> AI는 추천한다  
> Human이 판단한다  
> 시스템은 학습한다

---

## 🧠 System Philosophy

이 플랫폼은 다음 3가지 철학 위에서 동작합니다.

### 1️⃣ Safety First

```
데이터 정합성 > 시스템 안정성 > 성능
```

### 2️⃣ Human-in-the-loop

```
AI Recommendation ≠ Execution
```

> 모든 인프라 변경은 반드시 사람이 수행

### 3️⃣ Learning System

> 장애는 끝나는 것이 아니라  
> 시스템을 더 똑똑하게 만든다

---

## 🔄 End-to-End Flow

```
[Alert 발생]
        ↓
AI Agent (RAG 기반 분석)
        ↓
Primary Knowledge 기반 판단
        ↓
대응 권장 (Runbook + Improvement 반영)
        ↓
Human 검토 및 실행
        ↓
(필요 시 반복 분석)
        ↓
Incident 종료
        ↓
AI Postmortem Draft 생성
        ↓
Human 검증 / 승인
        ↓
Learning Knowledge 축적
        ↓
다음 장애 대응 개선
```

---

## 🧠 RAG Knowledge System (핵심 구조)

### 🔴 Primary Knowledge (판단 기준)

```
scenarios/             → 장애 정의
runbooks/              → 대응 방법
improvements/          → 행동 제한
preventive-designs/    → 구조적 해결
postmortems/           → 실제 경험
```

- AI의 **"판단 기준"**
- 절대 override 대상 아님

### 🔵 Secondary Knowledge (보조 지식)

```
rag/docs/              → 기술 원리 / 메커니즘
```

- 원인 분석 보조
- metric 해석 보조
- 절대 Action 결정에 사용 금지

### ⚖️ Knowledge Priority (중요)

```
Preventive Design
> Improvement
> Postmortem
> Runbook
> Scenario
> rag/docs
```

> 가장 안전한 규칙이 항상 우선된다.

---

## 🤖 AI Agent 역할

**AI Agent는 다음만 수행합니다:**

- ✔ 장애 분석
- ✔ RAG 기반 문서 검색
- ✔ 대응 전략 추천
- ✔ 위험도 판단
- ✔ Postmortem Draft 생성

**❌ AI가 하지 않는 것:**

- 인프라 변경
- scale-out 실행
- 설정 변경
- 데이터 변경

---

## 🧑‍💻 Human 역할

- ✔ 대응 실행
- ✔ AI 판단 검증
- ✔ Root Cause 확정
- ✔ Postmortem 승인
- ✔ 시스템 개선 방향 결정

---

## 🏗️ System Architecture

### 1️⃣ Application Layer

```
apps/
├── web-console/
├── api-server/ (WebFlux)
├── agent-server/
└── worker/
```

### 2️⃣ Infrastructure Layer

```
infra/
├── terraform/
├── kubernetes/
├── helm/
└── gitops/
```

### 3️⃣ Platform (SRE)

```
platform/
├── observability/
├── incident-response/
├── reliability/
├── traffic-control/
└── security/
```

### 4️⃣ RAG Pipeline

```
rag/
├── docs/
├── embeddings/
├── chunks/
├── metadata/
├── prompts/
└── pipelines/
```

### 5️⃣ AI Agent System

```
agent/
├── workflows/
├── tools/
└── guardrails/
```

---

## 🚨 Incident Handling Model

```
Alert
→ AI 분석
→ 권장 생성
→ Human 실행
→ 반복
→ 종료
```

---

## 🧠 Decision System (중요)

AI는 다음 순서로 판단합니다:

| 순서 | Knowledge | 역할 |
|------|------|------|
| 1 | Scenario | 장애 정의 |
| 2 | Runbook | 가능한 대응 |
| 3 | Improvement | 제한 규칙 |
| 4 | Preventive Design | 구조적 우선 |
| 5 | Postmortem | 경험 보정 |
| 6 | rag/docs | 이해 보조 |

---

## 📝 Postmortem Learning Loop

```
Incident 종료
→ AI Draft 생성
→ Human 검증
→ postmortems/ 추가
→ RAG 반영
→ 다음 장애 대응 개선
```

**핵심 원칙:**

- Runbook은 수정하지 않는다
- Postmortem은 누적한다
- AI는 둘을 비교해 판단한다

---

## 📚 Documentation System

```
scenarios/             → 문제 정의
runbooks/              → 대응 전략
improvements/          → 행동 제한
preventive-designs/    → 구조적 해결
postmortems/           → 경험
rag/docs/              → 기술 이해
protocols/             → 규칙
```

---

## 🎯 What This Platform Proves

이 프로젝트는 다음을 증명합니다:

- ✔ SRE 사고방식
- ✔ 장애 대응 체계 설계
- ✔ AI + 운영 통합
- ✔ RAG 기반 학습 시스템
- ✔ Human-in-the-loop 안정성
- ✔ FinTech 수준 안전 설계