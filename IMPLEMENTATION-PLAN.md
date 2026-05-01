# Financial Technology SRE Platform Implementation Plan

> Human-in-the-loop 기반  
> AI Agent + RAG + Postmortem Learning Loop 구현 계획

---

## 1. 구현 목표

이 프로젝트는 단순 결제 API 구현이 아니라,  
**장애를 감지하고, AI가 분석하며, Human이 실행하고, 시스템이 학습하는 SRE 운영 플랫폼**을 구현하는 것을 목표로 한다.

**핵심 목표:**

1. 결제 시스템 구축
2. Observability 기반 장애 감지
3. AI Agent 기반 장애 분석
4. RAG 기반 대응 권장
5. Human-in-the-loop 실행 구조
6. Postmortem 기반 학습 루프

---

## 2. 구현 원칙

> AI는 실행하지 않는다  
> AI는 추천한다  
> Human이 판단한다  
> 시스템은 학습한다

---

## 3. Phase 1 — Core Payment System

**목표:** 결제 도메인의 기본 시스템을 구축한다.

### 구현 항목

**apps/api-server:**
- Spring WebFlux 기반 Payment API
- 결제 요청 생성
- 결제 상태 조회
- Idempotency Key 처리

**apps/worker:**
- 비동기 결제 이벤트 처리
- Kafka Consumer
- 외부 결제 Provider Mock 연동

### 주요 기술

`Spring WebFlux` `R2DBC` `PostgreSQL` `Redis` `Kafka`

---

## 4. Phase 2 — Observability Platform

**목표:** 장애를 감지하고 분석할 수 있는 관측 기반을 구축한다.

### 구현 항목

**Metrics:**
- Prometheus / Grafana Dashboard
- Payment latency, Error rate, Redis timeout
- DB connection pool, Kafka consumer lag

**Logs:**
- Loki
- `traceId` / `paymentId` / `requestId` 포함

**Traces:**
- OpenTelemetry / Jaeger
- API → Redis → DB → Kafka 흐름 추적

---

## 5. Phase 3 — Scenario / Runbook 기반 장애 대응 지식 구축

**목표:** AI가 장애를 판단할 수 있는 Primary Knowledge를 구축한다.

### 구현 항목

**scenarios/:** 장애 정의, 증상, 영향 범위, propagation, detection rule

**runbooks/:** 진단 절차, 대응 전략, Action / Rollback / Verification, Decision Rule

### 우선순위

| 순위 | 대상 |
|------|------|
| 1 | Redis Timeout |
| 2 | DB Connection Pool Exhaustion |
| 3 | Kafka Consumer Lag |
| 4 | Payment API High Latency |

---

## 6. Phase 4 — RAG Knowledge Layer 구축

**목표:** AI Agent가 문서를 검색하고 판단에 활용할 수 있도록 RAG 구조를 구축한다.

### Knowledge Layer

| 구분 | 경로 |
|------|------|
| **Primary Knowledge** | `scenarios/`, `runbooks/`, `improvements/`, `preventive-designs/`, `postmortems/` |
| **Secondary Knowledge** | `rag/docs/` |

### 구현 항목

```
rag/
├── docs/
├── metadata/
├── chunks/
├── embeddings/
├── prompts/
└── pipelines/
```

### 원칙

```
Primary Knowledge = 판단 기준
rag/docs          = 기술 이해 보조
```

---

## 7. Phase 5 — AI Agent Recommendation Workflow

**목표:** Alert 발생 시 AI가 장애 대응 권장 문서를 생성한다.

### 흐름

```
Alert 발생
→ AI Agent 수신
→ Scenario 검색
→ Runbook 검색
→ Improvement / Preventive Design / Postmortem 검색
→ rag/docs 보조 조회
→ 대응 권장 생성
→ Human 검토
```

### AI 출력 필수 항목

| # | 항목 |
|---|------|
| 1 | Incident Summary |
| 2 | Most Likely Cause |
| 3 | Evidence |
| 4 | Recommended Action |
| 5 | Expected Effect |
| 6 | Risk |
| 7 | Rollback Plan |
| 8 | Verification |
| 9 | Human Approval Required |

### 제한

> AI는 인프라 / 시스템 변경을 직접 수행하지 않는다.  
> **AI는 추천만 한다.**

---

## 8. Phase 6 — Improvement / Preventive Design Layer 구축

**목표:** 단순 대응을 넘어 장애 재발을 줄이는 학습형 구조를 만든다.

### Improvement

역할: 위험한 대응 제한, Runbook 보정, 행동 제약 정의

**예:**

```
Redis timeout + retry 증가
→ scale-out 금지
→ retry 제한 우선
```

### Preventive Design

역할: 장애를 구조적으로 제거, 시스템 설계 변경 방향 정의

**예:**

```
Redis 단일 idempotency 의존 제거
→ Redis + DB fallback + Unique Constraint
```

---

## 9. Phase 7 — Postmortem Learning Loop

**목표:** 장애 종료 후 AI와 Human이 함께 Postmortem을 생성하고, 다음 장애 대응 품질을 개선한다.

### 흐름

```
Incident 종료 선언
→ AI가 장애 기간 데이터 수집
→ AI가 대응 과정 분석
→ Postmortem Draft 생성
→ 파일명 추천
→ Human 검증
→ Root Cause 확정
→ postmortems/ 저장
→ 다음 RAG 분석에 반영
```

### 역할 분담

| 구분 | 역할 |
|------|------|
| **AI** | Timeline 초안, Alert / Metric / Log / Trace 요약, 대응 과정 정리, Root Cause 후보 / Action Item 후보 제시 |
| **Human** | Root Cause 최종 확정, 잘못된 해석 수정, Postmortem 승인, Git commit |

### 핵심 원칙

- Scenario / Runbook은 수정하지 않는다
- Postmortem은 계속 추가한다
- AI는 과거 경험을 기반으로 더 안전하게 판단한다

---

## 10. Phase 8 — Protocol 기반 문서 표준화

**목표:** 모든 문서가 동일한 수준과 구조를 유지하도록 프로토콜을 정의한다.

### 프로토콜

```
protocols/
├── rag-knowledge-layering-protocol.md
├── scenario-authoring-protocol.md
├── runbook-authoring-protocol.md
├── improvement-authoring-protocol.md
├── preventive-design-authoring-protocol.md
├── postmortem-protocol.md
└── rag-docs-authoring-protocol.md
```

### 효과

- 문서 품질 표준화
- AI 판단 일관성 확보
- 다른 채팅 / 다른 작성자 환경에서도 동일 품질 유지

---

## 11. Phase 9 — Chaos / Failure Simulation

**목표:** 의도적으로 장애를 발생시켜 AI 판단과 RAG 구조를 검증한다.

### 시뮬레이션 대상

- Redis timeout
- DB connection pool exhaustion
- Kafka consumer lag
- External payment provider latency
- Payment API high latency

### 검증 항목

- Alert 발생 여부
- Scenario 매칭 정확도
- Runbook 추천 정확도
- Improvement / Preventive 제약 반영 여부
- Rollback Plan 포함 여부
- Human 승인 필요 여부

---

## 12. Phase 10 — Portfolio Completion

**목표:** 실제 SRE 포지션에서 설명 가능한 포트폴리오로 정리한다.

### 산출물

```
README.md
PORTFOLIO.md
ARCHITECTURE.md
IMPLEMENTATION-PLAN.md
ROADMAP.md
scenarios/
runbooks/
improvements/
preventive-designs/
postmortems/
protocols/
rag/docs/
agent/workflows/
```

---

## 13. 최종 구현 순서

| 순서 | 항목 |
|------|------|
| 1 | Core Payment API |
| 2 | Observability |
| 3 | Redis / DB / Kafka 장애 시나리오 |
| 4 | Runbook 작성 |
| 5 | RAG Knowledge Layer 구축 |
| 6 | AI Recommendation Workflow |
| 7 | Improvement / Preventive Design 추가 |
| 8 | Postmortem Learning Loop |
| 9 | Chaos Test |
| 10 | Portfolio 정리 |

---

## 14. 핵심 리스크

| 리스크 | 대응 |
|------|------|
| AI가 Action을 과도하게 추천할 위험 | Human-in-the-loop |
| 잘못된 RAG 문서가 판단을 오염시킬 위험 | Primary Knowledge 우선 |
| Postmortem 검증 없이 학습될 위험 | 검증된 Postmortem만 RAG 반영 |
| Primary Knowledge 부족 시 오판 위험 | No Scenario → No Action |
| rag/docs override 위험 | rag/docs override 금지 |

---

## 15. 핵심 메시지

> 이 프로젝트는 장애를 자동으로 고치는 시스템이 아니다.
>
> AI가 판단을 돕고,  
> Human이 실행하며,  
> Postmortem이 시스템을 학습시키는  
> **Human-in-the-loop SRE 운영 플랫폼**이다.