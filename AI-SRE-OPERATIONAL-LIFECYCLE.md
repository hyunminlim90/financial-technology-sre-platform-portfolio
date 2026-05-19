# AI-SRE 운영 라이프사이클과 Human-in-the-Loop 장애 관리

---

## 1. 목적

이 문서는 FinTech 중심 AI-SRE 플랫폼의 운영 라이프사이클을 정의합니다.

이 플랫폼은 다음을 기반으로 설계되었습니다:

- 운영 안전성
- Human Approval
- 감사 추적 (Auditability)
- Rollback-first 원칙
- SLO 인식 장애 처리
- 추천 기반 AI 지원

이 시스템은 **자율 복구 플랫폼이 아닙니다.**

AI 추천은 다음을 절대 우회할 수 없습니다:

- Human Review
- Policy 검증
- Rollback 확인
- 운영 승인

---

## 2. 핵심 원칙

### Human-in-the-Loop

```
AI가 추천한다
Human이 결정한다
Human이 실행한다
AI가 결과를 분석한다
```

플랫폼은 다음을 자동으로 수행하면 안 됩니다:

- `kubectl` 실행
- ArgoCD 변경
- Kubernetes 리소스 수정
- Cloudflare API 호출
- 결제 데이터 변경
- 복구 액션 직접 실행

### No Scenario → No Action

운영 추천은 반드시 다음에서 발생해야 합니다:

- `scenarios/`
- `runbooks/`
- `improvements/`
- `preventive-designs/`

RAG 문서만으로는 운영 액션을 트리거할 수 없습니다.

### Rollback-first 설계

모든 운영 추천은 다음을 보존해야 합니다:

- rollback 요구사항
- rollback 가용성
- verification 요구사항

Rollback 또는 Verification 능력이 없는 추천은 차단되어야 합니다.

### 결제 안전 우선

플랫폼은 다음을 최우선으로 합니다:

- 결제 정합성
- 중복 결제 방지
- 정산 일관성
- 운영 안정성

공격적인 자동화보다 위 원칙이 항상 우선합니다.

---

## 3. Knowledge 아키텍처

AI-SRE 플랫폼은 Knowledge를 다음과 같이 분리합니다.

### Primary Operational Knowledge

운영 추천에 직접 사용됩니다.

```
scenarios/
runbooks/
improvements/
preventive-designs/
protocols/
policies/
```

### Learning Knowledge

분석 및 학습에 사용됩니다.

```
postmortems/
rag/docs/
```

> Learning Knowledge는 운영 액션을 직접 트리거해서는 안 됩니다.

---

## 4. End-to-End 운영 라이프사이클

```
Alert 발생
        ↓
AI Agent (RAG 기반 분석)
        ↓
Primary Knowledge 기반 판단
        ↓
대응 권장 생성
        ↓
Human Approval
        ↓
Dry-run Execution Plan 생성
        ↓
Human이 시스템 외부에서 실행
        ↓
Execution Result 기록
        ↓
Verification Result 기록
        ↓
Incident 상태 판단
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

## 5. Alert과 Evidence 흐름

```
Prometheus Alertmanager
→ AlertEvent
→ EvidenceSignal
→ EvidenceContext
→ Recommendation Engine
```

시스템은 다음을 보존합니다:

- alert 증거
- severity
- service
- domain
- incident 연결 정보
- recommendation 이력

---

## 6. Recommendation 라이프사이클

### Recommendation 생성

AI 추천은 다음으로부터 생성됩니다:

- 운영 Knowledge
- Evidence Context
- Policy
- Guardrail

AI 시스템이 제공하는 것:

- 추천 요약
- 액션 후보
- Rollback 예상
- Verification 요구사항

### Recommendation 승인

추천은 반드시 명시적인 Human Review가 필요합니다.

| 상태 | 설명 |
|------|------|
| `PENDING` | 검토 대기 |
| `APPROVED` | 승인 완료 |
| `REJECTED` | 거절 |

> 승인은 운영 액션을 직접 실행하지 않습니다.

### Execution Plan 경계

Execution Plan은:

- dry-run 전용
- 실행 불가 형태
- Human Review 아티팩트

Execution Plan은 다음을 반드시 보존해야 합니다:

- rollback 요구사항
- verification 요구사항
- 최종 승인 요구사항

### Human Execution Result

운영자는 AI 플랫폼 외부에서 변경을 직접 실행합니다.

시스템이 기록하는 것:

- 실행 상태
- 운영자 정보
- 실행 요약
- 타임스탬프
- 안전한 메타데이터

> 시스템은 인프라 변경을 직접 실행해서는 안 됩니다.

---

## 7. Verification과 Incident 해결

Verification은 운영 액션 이후 시스템이 복구되었는지를 판단합니다.

Verification이 평가할 수 있는 항목:

- alert 복구 여부
- latency 정상화
- error rate 복구
- queue 안정화
- trace 복구
- 새로운 증상 발생 여부

> Verification은 자동으로 Incident를 종료하지 않습니다.

---

## 8. SLI / SLO 인식

플랫폼은 SLO 인식 기반 운영 의사결정 시스템으로 발전하도록 설계되었습니다.

**신호 종류:** metrics / traces / logs / 비즈니스 KPI

**예시 지표:** P99 latency / error rate / 중복 결제 비율 / Kafka consumer lag / availability / timeout rate

### SLO 기반 Verification 예시

```
Before:
  P99 = 3.2s
  5xx = 8%

After:
  P99 = 320ms
  5xx = 0.1%
```

---

## 9. 재분석 루프

운영 장애는 반복적인 분석이 필요할 수 있습니다.

**재분석이 필요한 경우:**

- 완화 조치 이후에도 alert이 지속
- 새로운 증상 발생
- 부분적인 복구만 발생
- 회귀(Regression) 감지

플랫폼은 업데이트된 Evidence를 기반으로 새로운 추천을 생성할 수 있습니다.

> 재분석 시에도 Human Approval, Policy 검증, 운영 Review는 반드시 유지됩니다.

---

## 10. 내부 운영 보안

운영 API는 내부 전용 인터페이스입니다.

**보호 경로:**

```
/internal/admin/**
/internal/alerts/**
/internal/recommendations/**
/internal/execution-plans/**
/internal/execution-results/**
```

외부 노출은 금지됩니다.

**보안 원칙:** 내부 라우팅 전용 / 인증 필수 / 감사 추적 필수 / wildcard ingress 금지 / 공개 운영 노출 금지

---

## 11. 감사 추적 (Auditability)

플랫폼은 다음에 대해 append-only 방식의 운영 감사 기록을 유지합니다:

- ingestion / alert / recommendation / 승인
- execution plan / human execution result / verification result

**감사 기록에 저장해서는 안 되는 것:** 결제 페이로드 / 고객 데이터 / 시크릿 / raw 로그 / 전체 LLM 프롬프트

---

## 12. 장기 방향

플랫폼은 다음을 향해 발전합니다:

- SLO 인식 기반
- Evidence 중심
- Human 승인 전제
- 운영 의사결정 지원 플랫폼

**목표는 자율 운영이 아닙니다.**

목표는 **더 안전하고 설명 가능한 운영 의사결정 지원**입니다.

---

## Operational Knowledge Retrieval Order

AI Recommendation은 반드시 다음 순서로 retrieval / reasoning 해야 한다.

```
Scenario
→ Runbook
→ Improvement
→ Preventive Design
→ Postmortem
→ Systems-Math
→ Evidence
→ Human Approval
```

RAG/docs는 메커니즘 설명과 grounding 용도로만 사용된다.

**rag/docs-only reasoning은 운영 Action을 생성할 수 없다.**

---

## Systems-Math Integration

AI Recommendation은 단순 symptom matching이 아니라, 운영 현상을 정량적으로 설명 가능한 방향으로 발전한다.

**예시:**

```
Kafka Consumer Lag 증가
→ queue depth 증가
→ arrival rate > service rate
→ retry amplification
→ tail latency 증가
```

관련 Systems-Math 문서를 retrieval 하여 다음을 수행한다.

- saturation explanation
- propagation reasoning
- reliability interpretation

---

## Experiment Lifecycle

Experiment는 Recommendation과 Recovery 전략의 안전성을 검증하기 위한 구조이다.

**Flow:**

```
Scenario
→ Failure Injection
→ Metrics / Logs / Traces 수집
→ Recommendation 평가
→ Rollback 검증
→ Verification 검증
→ Recovery Time 측정
→ Postmortem 축적
```

**Experiment 필수 조건:** Human-approved / sandboxed / bounded blast radius / rollback available / verification required

---

## Degraded Recommendation Semantics

Evidence 부족, partial observability, retrieval failure 상황에서는:

AI는 certainty를 낮추고, **degraded recommendation 상태를 명시**해야 한다.

Unknown을 추정으로 대체해서는 안 된다.

> degraded recommendation 상태에서도 Human Approval, Rollback, Verification 요구사항은 유지된다.

---

## Governance Timeline Integration

다음 이벤트들은 append-only governance timeline으로 기록된다.

- alert / recommendation / approval
- execution plan / execution result / verification
- incident resolution / postmortem / experiment result

Timeline은 operator-facing audit 용도로 사용되며, **운영 변경을 직접 실행하지 않는다.**

---

## Recommendation Boundary

AI Recommendation은 다음을 수행하지 않는다.

- infra mutation
- payment mutation
- automatic remediation
- GitOps direct mutation
- ArgoCD direct synchronization
- destructive execution

Recommendation은 **operator-facing decision support artifact** 역할만 수행한다.