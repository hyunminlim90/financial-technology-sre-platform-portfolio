# protocols/reliability-research-governance-contract.md

## 1. 목적

이 문서는 AI-SRE 플랫폼의 Reliability Research, Experimentation, Quantitative Analysis, Operational Governance 연구 방향과 데이터셋 거버넌스 규칙을 정의한다.

> 이 플랫폼의 목적은 단순 운영 자동화가 아니다.
> 플랫폼은 "운영 안정성을 연구 가능한 형태로 축적하고 검증하는 Reliability Research Platform" 이다.

---

## 2. 핵심 개념

플랫폼은 다음을 통합한다.

```
Operational Knowledge
  + Observability Evidence
  + Governance Timeline
  + Experiment Validation
  + Systems-Math
  + Human Governance
```

목표: **설명 가능하고, 검증 가능하며, 재현 가능한 운영 안정성 연구 플랫폼**

---

## 3. Research Boundary Rule

플랫폼은 운영 연구 플랫폼이지 자율 운영 시스템이 아니다.

금지:
- ❌ uncontrolled autonomous remediation
- ❌ human replacement
- ❌ unsafe infrastructure automation
- ❌ unverifiable AI action generation

> **원칙:** Human Governance는 제거되지 않는다.

---

## 4. Reliability Research Rule

플랫폼은 다음 영역의 Reliability 연구를 지원할 수 있다.

예:
- queue stabilization
- retry amplification reduction
- tail latency reduction
- rollback effectiveness
- recovery time optimization
- availability improvement
- backpressure propagation
- incident response optimization

---

## 5. Operational Knowledge Graph Rule

플랫폼의 운영 문서는 Knowledge Graph로 연결될 수 있다.

```
Scenario
  ↔ Runbook
  ↔ Improvement
  ↔ Preventive Design
  ↔ Experiment
  ↔ Systems-Math
  ↔ Postmortem
  ↔ Governance Timeline
  ↔ Observability Evidence
```

> **원칙:** 운영 지식은 관계 기반으로 연결되어야 한다.

---

## 6. Systems-Math Research Rule

Systems-Math는 Reliability 현상의 정량 분석 계층이다.

예:
- Little's Law
- retry amplification
- queue utilization
- tail latency propagation
- availability modeling

> **원칙:** 수학 자체보다 운영 안정성과의 연결이 우선된다.

---

## 7. Experiment Validation Rule

모든 연구 가설은 Experiment로 검증 가능해야 한다.

Experiment는 다음을 포함할 수 있다:
- failure injection
- traffic spike
- retry storm
- consumer lag simulation
- rollback validation
- recovery measurement

> **원칙:** 정량 모델은 실험으로 검증 가능해야 한다.

---

## 8. Observability Research Rule

Observability는 연구용 Evidence Dataset으로 사용될 수 있다.

대상:
- metrics
- logs
- traces
- alerts
- deployment events
- SLO signals
- governance events

목표: **propagation analysis / recovery analysis / failure correlation / recommendation evaluation**

---

## 9. Recommendation Evaluation Rule

AI Recommendation은 연구 대상이 될 수 있다.

예:
- recommendation accuracy
- rollback effectiveness
- verification quality
- approval latency
- recovery improvement

> **원칙:** Recommendation은 설명 가능하고 검증 가능해야 한다.

---

## 10. Governance Timeline Research Rule

Governance Timeline은 운영 의사결정 연구 데이터셋으로 사용될 수 있다.

예:
- recommendation replay
- incident reconstruction
- approval flow analysis
- rollback timing analysis

---

## 11. SLO-aware Research Rule

모든 Reliability 연구는 SLO 영향을 고려해야 한다.

예:
- availability
- latency
- error budget burn
- recovery time
- tail latency

---

## 12. Human-in-the-loop Research Rule

Human Governance는 연구 대상에서도 유지된다.

> **원칙:** Human approval은 제거되지 않는다.

연구 목적:

| 질문 | 답 |
|------|-----|
| AI가 Human을 대체하는가? | ❌ |
| AI가 Human decision quality를 향상시키는가? | ✔ |

---

## 13. FinTech Safety Rule

결제 안정성은 모든 연구보다 우선된다.

최우선 보호 대상:
- payment integrity
- idempotency
- duplicate payment prevention
- settlement consistency

> **원칙:** 공격적 실험보다 결제 안정성이 우선된다.

---

## 14. Sandbox Rule

실험은 bounded environment에서 수행되어야 한다.

**허용:**
- sandbox
- isolated environment
- bounded blast radius
- human-approved experiment

**금지:**
- ❌ uncontrolled production chaos
- ❌ unsafe experiment propagation
- ❌ human bypass

---

## 15. Research Dataset Rule

플랫폼은 Reliability Research Dataset을 생성할 수 있다.

예:
- incident dataset
- rollback dataset
- retry amplification dataset
- queue saturation dataset
- recovery dataset
- recommendation dataset

---

## 16. Explainability Rule

연구 결과는 설명 가능해야 한다.

포함 대상:
- why the recommendation worked
- why rollback failed
- why latency propagated
- why queue collapsed

---

## 17. Replay Compatibility Rule

연구는 replay 가능한 구조를 유지해야 한다.

예:
- incident replay
- recommendation replay
- experiment replay
- timeline replay

---

## 18. Measurement Rule

모든 연구는 측정 가능해야 한다.

필수:
- baseline
- during
- after
- comparison
- verification

---

## 19. Statistical Governance Rule

정량 분석은 statistical interpretation을 포함할 수 있다.

예:
- percentile
- variance
- distribution
- sampling
- confidence interval

> **원칙:** 평균값만으로 운영 안정성을 설명하지 않는다.

---

## 20. Research Ethics Rule

플랫폼은 unsafe experimentation을 금지한다.

금지:
- ❌ production destructive testing
- ❌ unsafe customer impact
- ❌ payment integrity violation
- ❌ human approval bypass

---

## 21. Publication-ready Research Rule

플랫폼은 학술 연구 수준의 재현성을 지향할 수 있다.

포함 대상:
- experiment reproducibility
- measurement reproducibility
- evidence preservation
- timeline replay
- explainable recommendation

---

## 22. Academic Progression Rule

플랫폼은 다음 수준으로 확장될 수 있다.

| 수준 | 방향 |
|------|------|
| 학사 | SRE / Platform / Observability |
| 석사 | AI Recommendation / Reliability Modeling |
| 박사 | Quantitative Reliability Governance / AI-assisted Operational Reasoning |

---

## 23. Operational-first Rule

연구는 실제 운영 안정성 문제를 우선한다.

금지:
- ❌ operationally irrelevant theory
- ❌ pure theoretical optimization
- ❌ benchmark-only research

> **원칙:** 운영 현실과 연결되지 않는 연구는 지양한다.

---

## 24. Anti-Pattern Rule

금지:
- ❌ unverifiable research
- ❌ evidence 없는 recommendation analysis
- ❌ replay 불가능 연구
- ❌ observability 없는 reliability claim
- ❌ human-free governance
- ❌ uncontrolled autonomous experimentation

---

## 25. Non-Goals

플랫폼은 다음을 목표로 하지 않는다.

- AGI operator replacement
- autonomous production mutation
- unsafe self-healing systems
- human elimination

---

## 26. 핵심 원칙

| 계층 | 역할 |
|------|------|
| Operational Knowledge | 운영 지식 |
| Observability | Evidence |
| Systems-Math | 정량 설명 |
| Experiment | 검증 |
| Governance Timeline | Auditability |
| AI Recommendation | 의사결정 지원 |
| Human | 최종 책임 |

---

> 🎯 **한 줄 핵심**
>
> Reliability Research의 목적은 AI가 운영자를 대체하는 것이 아니다.
> → 운영 안정성을 더 안전하고, 설명 가능하며, 검증 가능하게 만드는 것이다.