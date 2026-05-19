# Financial Technology SRE Platform

> FinTech 결제 시스템을 대상으로 하는
> **Human-in-the-loop 기반 Knowledge-Governed AIOps / SRE 플랫폼**
>
> 이 프로젝트는 단순 장애 대응 도구가 아니라,
> 운영 지식, 관측 데이터, 정책, 실험, 정량 분석을 연결하여
> 금융권 수준의 안전한 AI 운영 판단을 지원하는 플랫폼이다.

---

## Project Overview

```
결제 시스템
→ Observability
→ Alert Pipeline
→ AI Agent
→ RAG / Operational Knowledge
→ Scenario / Runbook / Policy / Evidence 기반 판단
→ Human Approval
→ 장애 조치 권고
→ Postmortem 축적
→ 다음 장애 판단 개선
```

핵심은 AI 자동 실행이 아니라, AI가 운영자에게 안전한 근거 기반 권고를 제공하고 최종 판단과 실행은 사람이 수행하는 구조이다.

---

## Project Identity

이 플랫폼은 다음 네 가지 정체성을 가진다.

1. FinTech SRE Platform
2. Human-in-the-loop AIOps Platform
3. Operational Knowledge Governance Platform
4. Reliability Engineering / Research Platform

포트폴리오 구현을 넘어서 장기적으로 **학사 → 석사 → 박사 연구까지 확장 가능한 운영 안정성 기반 AI Ops 연구 플랫폼**을 지향한다.

---

## Core Goals

- 장애를 관측 가능한 형태로 감지한다.
- AI Agent가 장애 상황을 분석하고 대응을 추천한다.
- AI는 자동 실행하지 않는다.
- Human이 최종 판단하고 실행한다.
- 모든 장애는 Postmortem으로 축적된다.
- Scenario / Runbook / Policy / Evidence 기반으로 다음 판단을 개선한다.
- Systems-Math와 Experiment를 통해 장애 대응 효과를 정량적으로 검증한다.

---

## System Philosophy

### 1. Safety First

```
결제 무결성
> 중복 결제 방지
> 데이터 일관성
> rollback 가능성
> 장애 전파 방지
> 성능 최적화
```

금융권 시스템에서는 빠른 조치보다 **안전한 조치**가 우선이다.

### 2. Human-in-the-loop

```
AI Recommendation ≠ Auto Execution
```

**AI가 수행하는 것:**

- 분석 / 근거 제시 / 위험도 판단
- 대응 전략 추천 / rollback 제안 / verification 제안
- postmortem draft 작성

**AI가 수행하지 않는 것:**

- 인프라 변경 / scale-out 실행
- kubectl 실행 / ArgoCD sync 실행
- GitOps mutation / 데이터 변경
- 결제 상태 변경 / 자동 remediation

### 3. No Scenario → No Action

다음 지식이 없으면 AI는 운영 조치를 추천할 수 없다.

```
Scenario / Runbook / Rollback / Verification / Risk / Approval
```

### 4. RAG/docs Alone Cannot Decide Action

RAG/docs는 기술 이해를 돕는 보조 지식이다. AI의 운영 판단은 반드시 다음 계층을 함께 사용해야 한다.

```
Scenario + Runbook + Policy / Guardrail + Evidence + Metrics / Logs / Traces + Human Approval
```

### 5. Learning System

장애는 종료되는 것이 아니라 운영 지식으로 축적된다.

```
Incident
→ Postmortem
→ Preventive Design
→ Improvement
→ Scenario / Runbook 개선
→ 다음 장애 판단 개선
```

---

## End-to-End Flow

```
[Alert 발생]
      ↓
Observability Evidence 수집 (metrics / logs / traces)
      ↓
AI Agent 분석
      ↓
Scenario 검색
      ↓
Runbook 검색
      ↓
Policy / Guardrail 검증
      ↓
Systems-Math / Experiment / Postmortem 참고
      ↓
대응 권고 생성
      ↓
Human 검토 및 승인
      ↓
Human 실행
      ↓
Verification
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

## Operational Knowledge Architecture

이 프로젝트의 핵심은 단순 RAG 문서 저장소가 아니라 **운영 지식 계층 구조**이다.

```
Stack Documents
→ Scenario
→ Runbook
→ Systems-Math
→ Experiment
→ Postmortem
→ Preventive Design
→ Improvement
```

각 문서는 독립적으로 존재하지 않고 서로 연결된다.

```
Stack ↔ Scenario ↔ Runbook ↔ Systems-Math ↔ Experiment ↔ Postmortem
```

---

## RAG Knowledge System

### Primary Knowledge

AI 판단의 기준이 되는 운영 지식이다.

```
scenarios/             → 장애 정의
runbooks/              → 대응 절차
systems-math/          → 장애 현상 정량 분석
experiments/           → 재현 / 검증 / 측정
postmortems/           → 실제 장애 회고
preventive-designs/    → 구조적 예방 설계
improvements/          → 개선안 / 행동 제한 / 운영 보완
```

Primary Knowledge는 AI의 운영 판단 근거이며, 임의로 override 되어서는 안 된다.

### Secondary Knowledge

기술 원리와 개념 이해를 돕는 보조 지식이다.

```
rag/docs/00~24-stack/                  → 메인 기술 스택 문서
rag/docs/100-deep-dive/micro-*         → Micro Foundations
rag/docs/100-deep-dive/core-*          → Core Foundations
rag/docs/100-deep-dive/systems-math/   → Systems-Math 하위 개념
```

Secondary Knowledge는 기술 개념 이해, 장애 메커니즘 설명, metrics 해석 보조, systems-math grounding에 사용된다. **단독으로 Action을 결정할 수 없다.**

### Knowledge Priority

```
Policy / Guardrail
> Preventive Design
> Improvement
> Postmortem
> Runbook
> Scenario
> Systems-Math
> Experiment
> rag/docs
```

대응 권고 생성 시 최소 필수 흐름:

```
Scenario → Runbook → Policy / Guardrail → Evidence → Human Approval
```

---

## Systems-Math Layer

`systems-math/`는 단순 수학 학습 문서가 아니라, 운영 장애 현상을 정량적으로 설명하는 **Operational Quantitative Analysis Layer**이다.

**주요 개념:** percentile / variance / moving average / arrival rate / service rate / queue utilization / latency distribution / retry amplification / exponential backoff / Little's Law / tail latency

**연결 예시:**

```
Kafka Consumer Saturation
→ arrival rate > service rate
→ queue depth 증가
→ consumer lag 증가
→ P99 latency 증가
→ payment delay 증가
```

### Systems-Math Folder Strategy

**1. Root Systems-Math** (`systems-math/`)

Scenario / Runbook과 직접 연결되는 운영 장애 정량 분석 문서.

```
systems-math/kafka-consumer-saturation-math.md
systems-math/retry-storm-math.md
systems-math/queue-saturation-math.md
```

**2. Deep-dive Systems-Math** (`rag/docs/100-deep-dive/systems-math/`)

Root Systems-Math를 이해하기 위한 하위 수학 개념 문서.

```
little-law.md / queue-utilization.md / latency-distribution.md
percentile.md / variance.md
```

구조:

```
Scenario ↕ Runbook ↕ Systems-Math ↕ Deep-dive Systems-Math
```

---

## Experiment Layer

`experiments/`는 장애 재현과 대응 효과 검증을 위한 계층이다.

**Experiment 사용 목적:** 장애 재현 / AI Recommendation 검증 / Runbook 효과 측정 / Rollback 검증 / Recovery Time 측정 / Systems-Math 모델 검증 / Postmortem 근거 보강

**현재 원칙:**

```
Human-approved / sandboxed / bounded blast radius
rollback available / verification required
production direct execution prohibited
```

Experiment는 자동 장애 주입 시스템이 아니라, **안전하게 통제된 Reliability Validation 계층**이다.

---

## System Architecture

```
apps/
├── web-console/
├── api-server/
├── agent-server/
└── worker/

infra/
├── terraform/
├── kubernetes/
├── helm/
└── gitops/

platform/
├── observability/
├── incident-response/
├── reliability/
├── traffic-control/
└── security/

rag/
├── docs/
├── embeddings/
├── chunks/
├── metadata/
├── prompts/
└── pipelines/

agent/
├── workflows/
├── tools/
└── guardrails/
```

---

## Governance Timeline

`agent-server`는 운영 판단과 지식 흐름을 추적하기 위해 Governance Timeline read-model을 제공한다.

**현재 원칙:** read-only / append-only audit semantics / operator-facing / best-effort degraded / mutation prohibited

**Timeline 추적 대상:** recommendation / approval / execution plan / human execution result / verification / incident lifecycle / postmortem / learning candidate / knowledge promotion

Timeline은 실행 엔진이 아니다.

---

## Decision System

| 순서 | Knowledge | 역할 |
|------|-----------|------|
| 1 | Scenario | 장애 정의 |
| 2 | Runbook | 가능한 대응 |
| 3 | Policy / Guardrail | 허용 / 차단 기준 |
| 4 | Evidence | metrics / logs / traces |
| 5 | Systems-Math | 정량 분석 |
| 6 | Experiment | 재현 / 검증 결과 |
| 7 | Postmortem | 실제 경험 보정 |
| 8 | Preventive Design | 구조적 예방 |
| 9 | Improvement | 개선안 / 행동 제한 |
| 10 | rag/docs | 이해 보조 |

**절대 원칙:**

```
rag/docs 단독 판단 금지
Scenario 없는 Action 금지
Rollback 없는 권고 금지
Verification 없는 권고 금지
High risk 조치는 Human Approval 필수
```

---

## AI Agent Role

**수행 가능:** 장애 분석 / RAG 기반 문서 검색 / Evidence 기반 판단 보조 / 대응 전략 추천 / 위험도 판단 / rollback 제안 / verification 제안 / Postmortem Draft 생성 / Experiment 결과 해석 보조

**수행 불가:** 인프라 변경 / scale-out 직접 실행 / 설정·데이터·결제 상태 변경 / GitOps mutation / Kubernetes mutation / ArgoCD sync / 자동 remediation

## Human Role

Human Operator / SRE 수행 항목: AI 판단 검증 / Root Cause 확정 / 조치 실행 / Approval 결정 / Rollback 실행 여부 판단 / Verification 확인 / Postmortem 승인 / Preventive Design 결정 / Experiment 실행 승인

---

## Postmortem Learning Loop

```
Incident 종료
→ AI Postmortem Draft 생성
→ Human 검증
→ postmortems/ 추가
→ Preventive Design / Improvement 파생
→ 다음 장애 대응 개선
```

- Runbook은 절차 기준
- Postmortem은 경험 축적
- AI는 둘을 비교해 판단

---

## Reliability Research Loop

```
Scenario 정의
→ Experiment 설계
→ Failure Injection / Load Injection
→ Observability 수집
→ AI Recommendation 생성
→ Human Approval
→ 조치 / Rollback / Verification
→ 결과 측정
→ Postmortem 작성
→ Preventive Design / Improvement 도출
→ 다음 Experiment 개선
```

이 루프는 석사 / 박사 연구에서 다음 질문으로 확장될 수 있다.

- RAG/docs만 사용하는 AI보다 Scenario / Runbook / Postmortem 계층을 함께 사용하는 AI가 장애 대응 권고 정확도를 높이는가?
- rollback / verification / guardrail 강제가 위험한 권고 비율을 줄이는가?
- Postmortem 축적이 반복 장애 대응 시간을 줄이는가?
- 결제 시스템에서 idempotency / duplicate payment / consistency risk를 AI 권고 단계에서 얼마나 효과적으로 차단하는가?

---

## Documentation System

```
rag/docs/                     → 기술 원리 / 메커니즘
rag/docs/100-deep-dive/       → Concept Graph Expansion
scenarios/                    → 장애 정의
runbooks/                     → 대응 전략
systems-math/                 → 운영 안정성 정량 분석
experiments/                  → 실험 / 재현 / 검증
postmortems/                  → 실제 장애 회고
preventive-designs/           → 구조적 예방 설계
improvements/                 → 개선안 / 행동 제한
protocols/                    → 규칙 / 운영 프로토콜
```

---

## Long-term Direction

| 단계 | 목표 |
|------|------|
| 학사 | Computer Science / Software Engineering 기반 강화 |
| 석사 | Distributed Systems / Cloud / Reliability / AIOps 연구 기반 확보 |
| 박사 | Knowledge-Governed Human-in-the-loop AIOps 모델 제안 및 검증 |

핵심은 단순히 플랫폼을 구현하는 것이 아니라:

```
새로운 운영 모델
+ 실험
+ 측정 지표
+ 기존 방식 대비 개선 증명
```

까지 연결하는 것이다.

---

## Final Principle

이 플랫폼의 핵심 가치는 **AI 자동 실행**이 아니다.

> **운영 안정성을 최우선으로 하는**
> **정책 기반 AI 판단 보조 플랫폼을 만드는 것이다.**