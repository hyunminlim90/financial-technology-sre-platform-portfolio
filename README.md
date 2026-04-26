# 🚀 Financial Technology SRE Platform Portfolio

> SRE + Platform Engineering + AI Agent + RAG 기반의 **차세대 운영 자동화 플랫폼**

---

## 🧭 Project Overview

이 프로젝트는 다음을 목표로 합니다:

* 장애를 **자동으로 감지하고 분석**
* AI Agent가 **원인 분석 → 대응 실행 → 실패 시 자동 롤백**, 
  그리고 사전에 정의된 정책 기반으로 **실행 권한을 통제**
* RAG 기반으로 **Runbook / Docs 활용**
* SRE 관점에서 **신뢰성(SLO) 관리 자동화**

---

## 🔄 End-to-End Flow

```plaintext
[Client Request]
        ↓
Nginx / Reverse Proxy
        ↓
api-server (Spring Boot WebFlux API)
        ↓
Queue (비동기 처리)
        ↓
worker
        ↓
AI Agent + RAG
        ↓
Runbook 기반 대응 수행
        ↓
(필요 시) 자동 복구 / 롤백
```

> 요청 처리부터 장애 대응까지 전 과정을 자동화한 구조입니다.

---

## 📚 Documentation Strategy

이 프로젝트에서는 문서를 세 가지 역할로 분리하여 관리합니다:

- `docs/`  
  → 시스템 설계 및 포트폴리오 설명을 위한 문서 (사람용)

- `runbooks/`  
  → 실제 장애 대응을 위한 실행 매뉴얼 (SRE 운영 기준)

- `rag/docs/`  
  → AI Agent가 사용하는 RAG 기반 지식 데이터 (Chunking / Embedding 대상)

### 🔁 Single Source of Truth

모든 문서는 `docs/`에서 한 번만 작성됩니다.

이후:

* `runbooks/` → 운영 매뉴얼로 활용
* `rag/` → Chunking / Embedding → AI Agent 활용

즉,
**하나의 문서를 기반으로 사람과 AI가 함께 사용하는 구조입니다.**

---

## 🏗️ Architecture Layers

### 1️⃣ Application Layer

```plaintext
apps/
├── web-console/     # React 기반 운영 콘솔
├── api-server/      # Spring Boot WebFlux / Netty 기반 논블로킹 API 서버 (Event Loop)
├── agent-server/    # Python AI Agent / LLM Orchestrator
└── worker/          # Queue 기반 백그라운드 처리 (RAG, LLM, 분석, 인프라 작업)
```
> ⚡ Architecture Principle

- **api-server는 요청을 빠르게 처리하고 절대 오래 걸리는 작업을 직접 수행하지 않는다**
- 모든 무거운 작업은 **Queue를 통해 Worker로 위임**
- Worker는 독립적으로 확장되며, LLM / RAG / 분석 / 인프라 작업을 수행

---

### 2️⃣ Infrastructure Layer

```plaintext
infra/
├── hyperv/          # 로컬 VM 기반 실습 환경
├── opentofu/        # IaC (Terraform 대체)
├── kubernetes/      # k8s manifest
├── helm/            # Helm Chart
└── gitops/          # ArgoCD 기반 GitOps
```

---

### 3️⃣ Platform Engineering (SRE)

```plaintext
platform/
├── observability/       # Prometheus, Grafana, Loki, Tempo
├── incident-response/   # 장애 대응 자동화
├── deployment/          # CI/CD, Canary, Blue-Green
├── security/            # RBAC, Secret 관리
├── traffic-control/     # Istio (Retry, Timeout, CB)
└── reliability/         # SLO / SLA / Error Budget
```

---

### 4️⃣ AI & RAG Pipeline

```plaintext
rag/
├── sources/         # SRE 운영 지식, 분산 시스템 분석, SaaS 플랫폼 개선(자체 개발), AI Agent 도입 전략 문서
├── docs/            # AI Agent가 참조하는 구조화된 RAG 문서
├── metadata/        # 문서 메타데이터
├── chunks/          # Chunk 분리 결과
├── embeddings/      # 벡터 데이터
├── prompts/         # RAG / Agent Prompt
└── pipelines/       # Indexing Pipeline
```

> 📌 Sources 설명

- SRE 관점에서 **요청 흐름, 병목, 장애 원인 분석, 복구 전략**을 다루는 운영 지식
- 기존 Spring 기반 SaaS 시스템의 **Thread 기반 구조 한계와 Non-blocking 전환(WebFlux) 분석**
- Redis, Kafka, DB, Network 등 **실제 병목 지점과 튜닝 전략**
- Kubernetes, Terraform, Observability 기반의 **플랫폼 운영 구조**
- AI Agent 도입 시 필요한 **권한 통제, 검증, 롤백, 감사 로그 설계**
- 단순 자동화가 아닌 **“안전하게 실행 가능한 운영 자동화 시스템”을 위한 설계 문서들**

⤷ 이 영역의 문서는 AI Agent가 장애를 이해하고 판단하기 위한 **운영 지식 베이스 (Operational Intelligence)** 역할을 합니다.

---

### 5️⃣ AI Agent System

```plaintext
agent/
├── tools/               # 메트릭/트레이스/로그 분석
├── guardrails/          # 실행 제어 및 안전장치
├── workflows/           # 장애 대응 자동화 흐름
├── memory/              # Agent Memory 구조
└── evaluations/         # 응답 품질 평가
```

---

### 6️⃣ Incident Scenarios

```plaintext
scenarios/
├── payment-api-latency/
├── db-connection-exhaustion/
├── redis-cache-failure/
├── kafka-consumer-lag/
├── deployment-rollback/
├── traffic-spike-autoscaling/
└── disaster-recovery/
```

---

### 7️⃣ Runbooks (운영 매뉴얼)

```plaintext
runbooks/
├── sre/
├── aws/
├── kubernetes/
├── database/
├── observability/
├── security/
└── ai-agent/
```

---

### 8️⃣ Documentation (포트폴리오 핵심)

```plaintext
docs/
├── 00-overview/
├── 01-system-design/
├── 02-infra-design/
├── 03-sre-design/
├── 04-rag-design/
├── 05-ai-agent-design/
├── 06-incident-scenarios/
├── 07-postmortem/
└── 08-interview-defense/
```

---

### 9️⃣ Automation & Testing

```plaintext
scripts/
├── setup-local.sh
├── setup-hyperv.ps1
├── import-rag-docs.py
├── generate-embeddings.py
├── seed-demo-data.py
└── chaos-test.sh

tests/
├── api/
├── agent/
├── rag/
└── infra/
```

---

### 🔟 CI/CD (GitHub Actions)

```plaintext
.github/workflows/
├── ci-api.yml
├── ci-web.yml
├── ci-agent.yml
├── rag-indexing.yml
└── infra-plan.yml
```

---

## 🔥 Key Highlights

* ✅ **SRE + Platform Engineering 통합 설계**
* 🤖 **AI Agent 기반 장애 대응 자동화**
* 📚 **RAG 기반 지식 활용 (Runbook + Docs)**
* ☸️ **Kubernetes + GitOps 기반 운영**
* 📈 **Observability + SLO 중심 운영 모델**

---

## 🎯 What This Portfolio Shows

이 프로젝트는 단순 CRUD가 아니라:

* "운영을 코드로 만든다"
* "장애 대응을 자동화한다"
* "AI를 실제 운영에 연결한다"

를 증명하는 **플랫폼 레벨 포트폴리오**입니다.

---
