# Financial Technology SRE Platform Roadmap

> AI Agent + RAG + Human-in-the-loop 기반  
> **SRE 운영 지능 플랫폼 구현 로드맵**

---

## 1. 목표

이 로드맵은 설계된 시스템을 실제로 구현하기 위한 단계별 실행 계획을 정의한다.

```
설계 → 구현 → 검증 → 학습 → 진화
```

---

## 2. 현재 상태

- ✔ Architecture 설계 완료
- ✔ RAG Knowledge Layer 정의 완료
- ✔ Protocol System 정의 완료
- ✔ Decision Engine 정의 완료
- ✔ Learning Engine 정의 완료

> 현재 단계: **"구현 시작 직전 상태"**

---

## 3. 전체 로드맵

| Phase | 목표 |
|------|------|------|
| Phase 1 | Core System 구축 |
| Phase 2 | Observability 구축 |
| Phase 3 | Scenario / Runbook 구축 |
| Phase 4 | RAG Pipeline 구축 |
| Phase 5 | AI Agent 구현 |
| Phase 6 | Learning System 구축 |
| Phase 7 | Improvement / Preventive Design 적용 |
| Phase 8 | Chaos Engineering |
| Phase 9 | UI / 운영 콘솔 |
| Phase 10 | Portfolio 완성 |

---

### Phase 1 — Core System 구축

**목표:** 실제 동작 가능한 결제 시스템 + 기본 인프라 구축

- ✔ Payment API (WebFlux)
- ✔ PostgreSQL (R2DBC)
- ✔ Redis
- ✔ Kafka
- ✔ 기본 Kubernetes 배포

---

### Phase 2 — Observability 구축

**목표:** 장애를 감지하고 분석할 수 있는 기반 구축

- ✔ Prometheus (Metrics)
- ✔ Grafana (Dashboard)
- ✔ Loki (Logs)
- ✔ Jaeger (Traces)

---

### Phase 3 — Scenario / Runbook 구축

**목표:** AI 판단을 위한 Primary Knowledge 구축

- ✔ Redis Timeout
- ✔ DB Connection Pool Exhaustion
- ✔ Kafka Consumer Lag
- ✔ Payment API Latency

---

### Phase 4 — RAG Pipeline 구축

**목표:** AI Agent가 문서를 검색할 수 있도록 구성

- ✔ 문서 Chunking
- ✔ Embedding 생성
- ✔ Vector DB 구성
- ✔ Retrieval Pipeline

---

### Phase 5 — AI Agent 구현

**목표:** Decision Engine 기반 장애 대응 추천 시스템 구현

- ✔ `incident-recommendation-flow` 적용
- ✔ RAG 기반 문서 검색
- ✔ Recommendation 생성 API
- ✔ Guardrails 적용

---

### Phase 6 — Learning System 구축

**목표:** Postmortem 기반 학습 시스템 구현

- ✔ `postmortem-generation-flow` 적용
- ✔ Postmortem Draft 자동 생성
- ✔ Human 검증 UI
- ✔ RAG 반영

---

### Phase 7 — Improvement / Preventive Design 적용

**목표:** 단순 대응 → 안전한 대응 → 구조적 해결

- ✔ Improvement 문서 작성
- ✔ Preventive Design 문서 작성
- ✔ Decision Engine 반영

---

### Phase 8 — Chaos Engineering

**목표:** AI 판단 정확도 검증

- ✔ Redis 장애 주입
- ✔ DB saturation
- ✔ Kafka lag 증가
- ✔ 외부 API latency 증가

---

### Phase 9 — UI / 운영 콘솔

**목표:** Human-in-the-loop 인터페이스 제공

- ✔ AI Recommendation 표시
- ✔ Risk / Rollback 표시
- ✔ 승인 버튼
- ✔ Postmortem 검증 UI

---

### Phase 10 — Portfolio 완성

**목표:** SRE / Platform / AI 결합 포트폴리오 완성

```
✔ README.md
✔ ARCHITECTURE.md
✔ IMPLEMENTATION-PLAN.md
✔ ROADMAP.md
✔ agent/workflows/
✔ rag/
✔ protocols/
```

---

## 4. 구현 우선순위 (현실 기준)

| 순서 | 항목 |
|------|------|
| 1 | Core Payment API |
| 2 | Observability |
| 3 | Scenario / Runbook |
| 4 | AI Recommendation API |
| 5 | Postmortem 생성 |
| 6 | Improvement / Preventive Design |
| 7 | UI |
| 8 | Chaos Test |

---

## 5. 핵심 리스크

| 리스크 | 결과 |
|------|------|
| RAG 품질 부족 | 오판 |
| Postmortem 검증 부족 | 잘못된 학습 |
| Improvement 부족 | 위험 행동 발생 |
| Human 승인 누락 | 사고 발생 |

---

## 6. 대응 전략

- ✔ Human-in-the-loop 강제
- ✔ Primary Knowledge 우선
- ✔ `rag/docs` override 금지
- ✔ Rollback 없는 Action 금지
- ✔ No Scenario → No Action

---

## 7. 성공 기준

- ✔ 장애 발생 시 AI가 올바른 Action 추천
- ✔ 위험한 Action 자동 차단
- ✔ Postmortem 기반으로 판단 개선
- ✔ 동일 장애 재발 시 더 빠른 대응