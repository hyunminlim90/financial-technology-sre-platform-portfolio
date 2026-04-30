# 🚀 Financial Technology SRE Platform Portfolio

> SRE + Platform Engineering + AI Agent + RAG 기반의
> **Human-in-the-loop 운영 지능형 장애 대응 플랫폼**

---

## 🧭 Project Overview

이 프로젝트는 단순 자동화 시스템이 아니라
**AI + SRE + Human 협업 기반 운영 플랫폼**을 구축하는 것을 목표로 합니다.

핵심 목표:

```text
1. 장애를 자동으로 감지하고 분석한다
2. AI Agent가 대응 “권장”을 제공한다 (자동 실행 ❌)
3. 사람(Human)이 최종 판단하고 실행한다
4. 장애 이후 Postmortem 기반으로 지속 학습한다
5. RAG 기반으로 운영 지식을 누적하고 진화시킨다
```

---

## 🔥 Core Principle

```text
AI는 실행하지 않는다
AI는 추천한다
Human이 판단한다
시스템은 학습한다
```

---

## 🔄 End-to-End Flow

```plaintext
[Alert 발생]
        ↓
AI Agent (RAG 기반 분석)
        ↓
장애 대응 권장 문서 제공
        ↓
Human 판단 및 실행
        ↓
(필요 시 반복 분석)
        ↓
Incident 종료 선언
        ↓
AI Postmortem Draft 생성 + 파일명 추천
        ↓
Human 검증 / 수정 / 승인 / 커밋
        ↓
RAG Learning Knowledge 축적
```

---

## 🧠 RAG Knowledge Layer (핵심 구조)

이 프로젝트의 핵심은 **5-layer RAG 구조**입니다.

### 🔹 Base Knowledge (고정)

```text
1. scenarios/  → 장애 정의
2. runbooks/   → 표준 대응 절차
```

👉 특징:

```text
- 거의 수정하지 않음
- 표준 대응 기준
```

---

### 🔹 Learning Knowledge (누적)

```text
3. postmortems/         → 실제 장애 경험
4. improvements/        → 개선된 대응
5. preventive-designs/  → 구조적 예방 설계
```

👉 특징:

```text
- 계속 추가됨 (append-only)
- 기존 문서 수정 ❌
- 경험 기반으로 AI 판단 보정
```

---

## 🤖 AI Agent 역할

AI Agent는 다음 역할만 수행합니다:

```text
✔ 장애 분석
✔ RAG 기반 문서 검색
✔ 대응 가이드 생성
✔ Postmortem Draft 생성
✔ 파일명 추천
```

❌ 하지 않는 것:

```text
- 자동 대응 실행
- 인프라 변경
- 데이터 변경
```

---

## 🧑‍💻 Human 역할

```text
✔ 장애 대응 실행
✔ AI 권장 판단
✔ Root Cause 확정
✔ Postmortem 검증 및 승인
✔ 개선 방향 결정
```

---

## 🏗️ Architecture Layers

### 1️⃣ Application Layer

```plaintext
apps/
├── web-console/     # React 기반 운영 콘솔
├── api-server/      # Spring WebFlux (Non-blocking API)
├── agent-server/    # AI Agent / LLM Orchestrator
└── worker/          # 비동기 작업 처리
```

---

### 2️⃣ Infrastructure Layer

```plaintext
infra/
├── hyperv/
├── terraform/
├── kubernetes/
├── helm/
└── gitops/
```

---

### 3️⃣ Platform Engineering (SRE)

```plaintext
platform/
├── observability/
├── incident-response/
├── deployment/
├── security/
├── traffic-control/
└── reliability/
```

---

### 4️⃣ RAG Pipeline

```plaintext
rag/
├── sources/
├── docs/
├── metadata/
├── chunks/
├── embeddings/
├── prompts/
└── pipelines/
```

---

### 5️⃣ AI Agent System

```plaintext
agent/
├── tools/
├── guardrails/
├── workflows/
```

---

## 🚨 Incident Handling Model

```text
Alert → AI 분석 → 권장 문서 → Human 실행 → 반복 → 종료
```

👉 특징:

```text
- 자동 실행 없음
- Human-in-the-loop
- 반복적 분석 가능
```

---

## 📝 Postmortem Learning Loop

```text
Incident 종료
→ AI Draft 생성
→ Human 검증
→ postmortems/ 추가
→ RAG 반영
→ 다음 장애 대응 개선
```

👉 핵심:

```text
Runbook은 바꾸지 않는다
Postmortem은 쌓는다
AI는 둘을 비교해서 판단한다
```

---

## 📚 Documentation Strategy

```text
scenarios/             → 장애 정의
runbooks/              → 대응 매뉴얼
postmortems/           → 장애 경험
improvements/          → 개선
preventive-designs/    → 예방 설계
protocols/             → 시스템 규칙
```

---

## 🎯 What This Portfolio Shows

이 프로젝트는 단순 서비스가 아니라:

```text
✔ SRE 사고방식
✔ 장애 대응 구조 설계
✔ AI + 운영 결합
✔ RAG 기반 학습 시스템
✔ Human-in-the-loop 안정성 확보
```

을 증명하는 **플랫폼 레벨 포트폴리오**입니다.

---

## 🔥 핵심 한 줄

```text
Runbook은 정답이고
Postmortem은 현실이며
AI는 그 사이에서 판단한다
```
