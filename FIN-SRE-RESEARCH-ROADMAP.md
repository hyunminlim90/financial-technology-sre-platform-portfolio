# FIN-SRE / AI Ops / 박사 트랙 최종 로드맵

> **목적:**
> 현재 구축 중인 FIN-SRE 플랫폼과 학습 구조를 유지하면서, 장기적으로:
> - 운영 안정성 중심 플랫폼 구축
> - 금융권 AI Ops / SRE 플랫폼 완성
> - 학은제 → 석사 → 박사
> - 연구/논문화 가능 구조 확보
>
> 까지 이어지는 장기 방향성을 흔들리지 않도록 유지하기 위한 최종 기준 문서.

---

## 최종 목표

다음 구조를 실제로 구축 가능한 수준까지 완성.

```
결제 시스템 (장애 발생 대상)
→ Observability
→ Alert Pipeline
→ AI Agent
→ RAG/docs
→ Scenario / Runbook / Improvements / Preventive Designs / Policy 기반 판단
→ Human Approval
→ 장애 조치 권고 보고서
→ Postmortem 축적
→ 다음 장애 판단 개선
```

---

## 절대 원칙

### 1. AI는 실행자가 아니다

```
AI Recommendation ≠ Auto Execution
```

AI는 판단 보조, 위험 분석, 장애 조치 권고, 근거 제시, rollback / verification 제안만 수행.

실행은 반드시 **Human Approval** 기반.

### 2. 금융권 안정성 우선

최우선 항목:

- 결제 무결성
- 중복 결제 방지
- 데이터 일관성
- rollback 가능성
- 장애 전파 방지

### 3. No Scenario → No Action

다음이 없으면 권고 금지.

- scenario
- runbook
- rollback
- verification
- improvements
- preventive-designs

### 4. RAG/docs 단독 판단 금지

RAG/docs는 **판단 보조 자료**일 뿐. 실제 판단은 다음 기반으로 수행.

```
scenario + runbook + policy + postmortem + metrics + evidence
```

---

## 현재 학습 방향 유지 원칙

현재 00~24 stack 구조 유지. 새로운 stack 무한 추가 금지.

```
Stack Entry
→ Micro Foundations
→ Core Foundations
→ Concept Graph Expansion
```

구조 유지.

---

## 현재 기술 스택

```
00-java              01-spring-framework    02-spring-boot
03-netty             04-webflux             05-mysql
06-kafka             07-redis               08-kubernetes
09-istio             10-jpa-hibernate       11-jenkins
12-git               13-argo-cd             14-elk
15-aws               16-kotlin              17-gradle
18-spring-cloud      19-elastic-search      20-oracle
21-spring-batch      22-node.js             23-open-stack
24-go-cd
```

---

## 추가 Stack 확장 중단

추가 프레임워크/언어보다 **기존 Stack Depth 강화** 우선.

핵심 흐름 완전 연결:

```
CPU → Kernel → JVM → Netty → WebFlux
→ Kafka → Kubernetes → Istio → Reliability → AI Ops
```

---

## 추가되어야 할 핵심 개념들

### Reliability / Distributed / Performance

```
backpressure        retry-storm          cascading-failure
fault-domain        blast-radius         queue-saturation
tail-latency        load-shedding        idempotency
consistency         quorum               leader-election
eventual-consistency                     circuit-breaker
```

### Systems Math

수학은 독립 입시 수학이 아니라, **운영 안정성을 설명하는 개념**으로 학습.

```
probability         percentile           variance
moving-average      arrival-rate         service-rate
queue-utilization   latency-distribution exponential-backoff
little-law
```

모두 **실제 장애 현상**과 연결해서 이해.

---

# Research / Knowledge Expansion Workflow

> **현재 단계 목표:**
> 논문 작성이 아니라, **논문으로 발전 가능한 운영 안정성 지식 자산을 지속 축적**하는 것이다.

---

## 전체 흐름

```
Stack 문서 정독
→ 대표 장애 유형 선정
→ Scenario 문서 작성
→ Runbook 문서 작성
→ Systems-Math 문서 작성
→ 하위 수학 개념 문서 작성
→ 문서 상호 링크 연결
→ (플랫폼 완성 후) 실험 / 검증 / 연구
```

---

## Step 1. 00~24 Stack 문서 정독

현재 기술 스택 문서를 기반으로 학습 시작.

---

## Step 2. 대표 장애 유형 선정

- 가장 관련 깊은 장애 유형 **1개** 우선 선정
- 문서 폭발 방지를 위해 **장애 1개 중심** 유지

---

## Step 3. Scenario 문서 작성

| 항목 | 내용 |
|------|------|
| 장애 현상 정의 | 무엇이 어떻게 발생하는가 |
| Detection Signals | metrics / logs / traces |
| 영향 범위 | 어디까지 영향이 퍼지는가 |
| 장애 전파 가능성 | cascading failure 경로 |

---

## Step 4. Runbook 문서 작성

| 항목 | 내용 |
|------|------|
| 진단 절차 | 장애 원인 확인 순서 |
| 완화 절차 | 피해 최소화 방법 |
| Rollback | 되돌리기 절차 |
| Verification | 조치 완료 확인 방법 |
| Approval / Risk | 승인 필요 여부 및 위험도 |

---

## Step 5. Systems-Math 문서 작성

장애 현상을 설명하는 **정량 모델** 작성. 연결 개념:

```
percentile
queue-utilization
Little's Law
latency distribution
retry amplification
```

수식보다 **운영 안정성 현상과의 연결** 중심으로 서술.

---

## Step 6. 하위 수학 개념 문서 작성

- Systems-Math 이해에 필요한 **최소 수학 개념**만 정리
- 수학 자체가 목적이 아니라 **운영 안정성 현상과의 연결**이 핵심

---

## Step 7. 문서 상호 링크 연결

모든 문서는 서로 연결되어야 한다.

```
Stack ↔ Scenario ↔ Runbook ↔ Systems-Math
```

---

## Step 8. 플랫폼 구축 완료 이후

플랫폼이 실제로 동작하는 단계에서 수행.

- 실제 Metrics / Logs / Traces 기반 검증
- AI Recommendation 평가
- Experiment 수행
- Postmortem 축적
- Reliability / AIOps 연구 진행

---

## 플랫폼 구축 방향

### 1단계 — 결제 시스템 구축

장애 발생 가능한 데모 결제 시스템 구축. 반드시 포함:

- idempotency / duplicate prevention
- retry / timeout / rollback / consistency

### 2단계 — Observability 구축

장애를 관측 가능한 상태 구축. 포함:

- metrics / logs / traces
- alert pipeline / dashboard
- latency / saturation / error rate

### 3단계 — 장애 유형 수집

장애를 실제로 계속 생성.

```
CPU saturation       retry storm          Kafka lag
GC pause             connection pool exhaustion
cascading failure    pod crash            network partition
throttling           slow query
```

### 4단계 — RAG/docs 구축

AI 판단 보조 자료. 포함:

- 개념 / 장애 설명 / 원인 / 대응 전략
- architecture / failure propagation

### 5단계 — Scenarios 구축

장애 유형 정형화. 예시:

```
Kafka Lag 증가
→ Consumer Saturation
→ Queue Explosion
→ Payment Delay
```

### 6단계 — Runbooks 구축

실제 대응 절차 정리. 반드시 포함:

- rollback / verification / risk / approval

### 7단계 — Policy / Guardrail 구축

AI가 위험한 권고를 하지 못하도록 제한.

```
No Rollback          → Reject
No Verification      → Reject
High Risk            → Human Approval Required
```

### 8단계 — SRE Console 구축

운영자 중심 장애 조치 플랫폼. 포함:

- 장애 현황 / evidence / metrics
- AI recommendation / risk analysis
- rollback proposal / verification proposal / approval flow

### 9단계 — AI Agent / LLM 구축

금융권 특성상 외부 AI API 의존 최소화. 목표:

- internal llm gateway / local inference
- evidence-based recommendation / policy-aware recommendation

### 10단계 — Postmortem 축적

모든 장애 조치 결과를 postmortems으로 축적.

> 장애 경험이 시간이 지날수록 다음 판단 정확도 향상으로 연결.

---

## 박사 방향성

이 플랫폼은 단순 구현 포트폴리오가 아니라, **운영 안정성 기반 AI Ops 연구 플랫폼**으로 발전 가능.

박사는 "플랫폼을 만들었다"가 아니라, **새로운 운영 모델을 제안하고 효과를 검증했다**가 핵심.

### 가능성 있는 연구 방향

**1. Safe AIOps**
```
Human-in-the-loop + Policy Guardrail + AI Recommendation 모델
```

**2. Operational Knowledge Governance**
```
rag/docs → scenarios → runbooks → postmortems 지식 계층 연구
```

**3. Reliability-aware AI Operations**

안정성 우선 AI 운영 모델.

**4. FinTech Operational Safety**

결제 시스템에서 duplicate prevention, rollback, consistency, risk isolation 연구.

---

## 박사 논문 연구 주제 프레이밍

### 구현 vs 연구의 차이

박사학위는 "플랫폼을 만들었다"만으로는 성립하지 않는다. 반드시 다음이 있어야 한다.

```
새로운 모델
+ 실험
+ 측정 지표
+ 기존 방식 대비 개선 증명
```

| 단순 구현 | 박사 연구 |
|----------|----------|
| 금융권 SRE AI Agent 플랫폼을 만들었다 | 금융권 결제 시스템에서 장애 지식 계층과 Human-in-the-loop AI 판단 구조가 장애 대응 정확도, 대응 시간, 재발 방지에 어떤 효과를 주는지 검증했다 |

---

### 가능성 높은 박사 논문 주제

**방향 1**

```
금융권 결제 시스템을 위한
Knowledge-Governed Human-in-the-loop AIOps 플랫폼 연구
```

**방향 2**

```
결제 시스템 장애 대응을 위한
RAG 기반 운영 지식 계층화와 정책 기반 AI 권고 모델
```

---

### 핵심 연구 질문

**RQ1. 지식 계층의 효과**

> RAG/docs만 사용하는 AI보다 scenarios / runbooks / postmortems 계층을 함께 사용하는 AI가 장애 대응 권고 정확도가 더 높은가?

**RQ2. Policy Guardrail의 효과**

> 장애 조치 권고에 rollback / verification / policy guardrail을 강제하면 위험한 권고 비율이 줄어드는가?

**RQ3. Postmortem 축적의 효과**

> Postmortem 축적이 반복 장애 대응 시간을 단축시키는가?

**RQ4. 금융 안전성 차단 효과**

> 결제 시스템에서 idempotency, duplicate payment, consistency risk를 AI 권고 단계에서 얼마나 효과적으로 차단하는가?

---

## 학은제 → 석사 → 박사 로드맵

### 1단계 — 현재

최우선: 현재 학습 구조 유지.

- micro-foundations 확장
- core-foundations 확장
- concept graph 강화
- 플랫폼 지속 구축
- 장애 유형 축적

### 2단계 — 학은제 학사 완료

컴퓨터공학 기반 확보. 우선 강화 항목:

- 자료구조 / 알고리즘 / OS / Network / DB / Distributed Systems 감각

### 3단계 — 석사

추천 방향: 컴퓨터공학 / 시스템 / 플랫폼 / 분산시스템 / 클라우드 / AI Ops / Reliability 관련 연구실.

석사에서 해야 하는 것:

- **논문 읽기** — 영어 논문 포함
- **실험 구조 습득** — benchmark, latency measurement, chaos testing, failure injection, evaluation metric
- **현재 플랫폼 연구화** — 구현 → 실험 → 검증 → 논문화 전환

### 4단계 — 박사

박사는 **연구 적성 확인 후 진입** 원칙.

```
문제 정의
→ 기존 방식 한계
→ 새로운 모델 제안
→ 실험
→ 검증
```

### 박사 이후 방향

```
FinTech SRE          Platform Architect      Reliability Engineer
AI Ops Engineer      Distributed Systems Engineer
Research Engineer    Platform Research       Technical Advisory
```

---

## 절대 흔들리지 말아야 할 방향

현재 방향의 핵심 가치는 **AI 자동 실행**이 아니라,

> **운영 안정성을 최우선으로 하는 정책 기반 AI 판단 보조 플랫폼**

이라는 점.

---

## 최종 결론

현재 방향은 **운영 안정성 기반 FinTech AI Ops / SRE 연구 플랫폼** 방향.

앞으로 중요한 것은 **새 stack 추가**보다:

```
개념 연결
장애 실험
관측
정책
운영 안정성
postmortem 축적
```

이 방향은 실무, 연구, 석사, 박사, 플랫폼 엔지니어링, AI Ops, Reliability를 **하나의 장기 축**으로 연결할 가능성이 있다.

---

## Operational Knowledge Governance

이 플랫폼은 단순 Vector Search 기반 RAG 시스템이 아니다.

핵심은:

```
Scenario
→ Runbook
→ Systems-Math
→ Experiment
→ Postmortem
→ Preventive Design
→ Improvement
```

계층 기반의 운영 지식 거버넌스 구조이다.

AI는 반드시 다음 순서로 retrieval / reasoning 해야 한다.

```
Scenario → Runbook → Policy / Guardrail → Evidence → Human Approval
```

**금지:**

- rag/docs-only recommendation
- rollback 없는 recommendation
- verification 없는 recommendation
- No Scenario 상태에서 Action recommendation

---

## Systems-Math Quantitative Layer

Systems-Math는 단순 수학 학습 계층이 아니다.

**핵심 역할:**

- queue saturation explanation
- retry amplification analysis
- tail latency reasoning
- failure propagation modeling
- reliability interpretation

운영 장애 현상을 정량적으로 설명하기 위한 **Operational Quantitative Analysis Layer** 역할을 수행한다.

**예시:**

```
Kafka Consumer Saturation
→ arrival rate > service rate
→ queue depth 증가
→ consumer lag 증가
→ tail latency 증가
→ payment delay 증가
```

**관련 개념:** Little's Law / queue utilization / percentile / variance / latency distribution / retry amplification

---

## Experiment / Reliability Validation

Experiment는 단순 Chaos Test가 아니다.

**핵심 목적:**

- Recommendation Validation
- Rollback Validation
- Verification Validation
- Recovery Time Measurement
- Systems-Math Validation
- Reliability Measurement

**Experiment Flow:**

```
Scenario
→ Failure Injection
→ Metrics / Logs / Traces 수집
→ AI Recommendation 평가
→ Rollback
→ Verification
→ Recovery Time 측정
→ Postmortem 축적
```

**Experiment 필수 조건:** Human-approved / sandboxed / bounded blast radius / rollback available / verification required

**금지:** production destructive execution / unbounded chaos / automatic destructive remediation

---

## Governance Timeline Research

이 플랫폼은 recommendation / approval / verification / postmortem / experiment 결과를 append-only timeline으로 기록한다.

**Governance Timeline 제공 기능:**

- operator-facing audit
- replay compatibility
- projection-backed query
- degraded read semantics
- cursor consistency
- governance observability

**현재 방향:**

```
runtime fan-out
+
future projection-backed query architecture
```

**Timeline Principles:** read-only / append-only / operator-facing / mutation prohibited / best-effort degraded

---

## Evaluation Metrics

이 플랫폼은 단순 구현이 아니라, **측정 가능한 Reliability Validation 구조**를 목표로 한다.

**주요 평가 지표:**

- MTTR
- Recommendation Precision
- Unsafe Recommendation Ratio
- Rollback Success Rate
- Verification Completion Rate
- Duplicate Recommendation Reduction
- Failure Recurrence Rate
- Tail Latency Reduction
- Queue Recovery Time
- Alert Noise Reduction

---

## Comparative Research Direction

이 플랫폼은 다음 구조들의 비교 연구 방향을 가진다.

- LLM only
- rag/docs-only
- scenario/runbook 기반
- policy-governed recommendation
- postmortem-enhanced recommendation
- systems-math grounded recommendation

**핵심 연구 질문:**

> 어떤 운영 지식 구조와 거버넌스 모델이 가장 안전하고 설명 가능한 Recommendation을 생성하는가?

---

## Human-in-the-loop Operational Safety

이 플랫폼은 AI 자동 실행보다 다음을 우선한다.

- approval
- rollback
- verification
- bounded risk
- operational governance

**핵심 목표:**

```
AI autonomy maximization  ← 이 아니라
Operational Safety Maximization  ← 이것이다
```