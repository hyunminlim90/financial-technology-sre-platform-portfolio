# protocols/observability-evidence-contract.md

## 1. 목적

이 문서는 AI-SRE 플랫폼에서 사용하는 모든 운영 Evidence의 수집, 해석, 상관관계 분석, Governance 규칙을 정의한다.

> Observability는 단순 모니터링이 아니다.
> Observability는 "운영 판단을 위한 Evidence 계층" 이다.

---

## 2. 핵심 개념

플랫폼은 다음 신호들을 운영 Evidence로 사용한다.

- metrics
- logs
- traces
- alerts
- deployment events
- scaling events
- governance timeline
- SLO/SLA signals

> **원칙:** 단일 신호만으로 Root Cause를 확정하지 않는다.

---

## 3. Evidence Hierarchy Rule

Evidence는 다음 우선순위로 상관 분석될 수 있다.

```
metrics
  + logs
  + traces
  + deployment correlation
  + governance timeline
```

> **원칙:** Evidence는 correlation 기반으로 해석되어야 한다.

---

## 4. Signal Classification Rule

모든 Evidence는 다음 유형으로 분류된다.

| 유형 | 설명 |
|------|------|
| metric | 시계열 정량 데이터 |
| log | 이벤트 기록 |
| trace | request flow |
| alert | threshold 기반 신호 |
| deployment | 배포 이벤트 |
| scaling | scaling 이벤트 |
| governance | recommendation / rollback / verification |

---

## 5. Metrics Rule

Metrics는 시스템 상태를 정량적으로 표현한다.

예:
- P95 latency
- P99 latency
- error rate
- retry rate
- queue depth
- consumer lag
- availability
- timeout rate

> **원칙:** 평균값보다 percentile 기반 해석을 우선할 수 있다.

---

## 6. Tail-Latency Rule

Tail latency는 운영 위험의 핵심 신호로 간주될 수 있다.

대상:
- P95
- P99
- P99.9

> **원칙:** 평균 latency는 운영 위험을 숨길 수 있다.

---

## 7. Queue Saturation Rule

Queue 기반 시스템은 반드시 saturation evidence를 수집해야 한다.

예:
- arrival rate
- service rate
- queue depth
- consumer lag
- utilization

목적: **backpressure / queue growth / throughput collapse** 분석.

---

## 8. Retry Amplification Rule

Retry 관련 시스템은 amplification evidence를 수집해야 한다.

예:
- retry rate
- timeout rate
- queue depth
- request amplification

> **원칙:** retry는 recovery를 돕기도 하지만 cascading failure를 유발할 수 있다.

---

## 9. Trace Correlation Rule

Trace는 request propagation 분석에 사용된다.

예:
- upstream latency
- downstream timeout
- retry path
- cross-service propagation

> **원칙:** trace는 propagation 분석의 핵심 Evidence다.

---

## 10. Deployment Correlation Rule

Deployment events는 Incident 분석에 반드시 연결될 수 있어야 한다.

```
deployment
  → latency spike
  → retry increase
  → queue saturation
```

> **원칙:** 배포 직후 장애는 항상 deployment correlation을 우선 검토한다.

---

## 11. Governance Timeline Rule

다음 이벤트는 governance evidence로 기록될 수 있다.

- recommendation
- approval
- execution result
- rollback
- verification
- postmortem generation
- experiment execution

목적: **auditability / replay compatibility / operational governance**

---

## 12. Evidence Correlation Rule

AI는 다음 Evidence를 상관 분석할 수 있다.

예:
```
Redis timeout
  + retry spike
  + deployment event
  + trace latency increase
  + consumer lag growth
```

> **원칙:** 단일 metric만으로 Root Cause를 확정하지 않는다.

---

## 13. Confidence Rule

다음 상황에서는 degraded confidence 상태를 생성해야 한다.

- missing metrics
- partial observability
- trace sampling loss
- inconsistent evidence
- delayed ingestion

> **원칙:** Unknown을 추정으로 대체해서는 안 된다.

---

## 14. SLO / Error Budget Rule

Observability는 SLO 기반 운영 판단을 지원해야 한다.

예:
- availability
- latency
- error budget burn
- recovery time

> **원칙:** 운영 판단은 SLO 영향을 고려해야 한다.

---

## 15. Systems-Math Integration Rule

Observability는 Systems-Math와 연결될 수 있다.

예:
- Little's Law
- queue-utilization
- retry amplification
- tail latency propagation

> **원칙:** 정량 모델은 실제 observability evidence와 연결되어야 한다.

---

## 16. Experiment Integration Rule

Experiment는 Before / During / After observability evidence를 포함해야 한다.

```
Before:  P99 latency = 120ms
During:  P99 latency = 2.1s
After:   P99 latency = 180ms
```

---

## 17. Recommendation Evidence Rule

AI Recommendation은 반드시 evidence-backed 이어야 한다.

포함 대상:
- related metrics
- related traces
- related alerts
- related deployment events
- related governance events

> **원칙:** 설명 불가능한 recommendation은 위험하다.

---

## 18. Explainability Rule

AI는 다음을 설명 가능해야 한다.

- 어떤 Evidence를 사용했는가
- 어떤 correlation을 발견했는가
- 어떤 signal이 recommendation을 제한했는가

---

## 19. Retention Rule

Evidence retention 정책은 정의되어야 한다.

대상:
- metrics retention
- trace retention
- sampling strategy
- aggregation policy

> **원칙:** sampling은 운영 해석 능력을 파괴해서는 안 된다.

---

## 20. Human-in-the-loop Rule

최종 운영 판단은 Human이 수행한다.

> **원칙:** AI Recommendation ≠ Execution

금지:
- ❌ autonomous remediation
- ❌ uncontrolled infrastructure mutation
- ❌ human bypass

---

## 21. Research Dataset Rule

Observability Evidence는 Reliability Engineering 연구 데이터셋으로 사용될 수 있다.

예:
- retry amplification reduction
- queue stabilization
- recovery time
- propagation pattern
- recommendation accuracy
- rollback effectiveness

---

## 22. Anti-Pattern Rule

금지:
- ❌ metric-only root cause
- ❌ trace 없는 propagation 분석
- ❌ observability 없는 recommendation
- ❌ deployment correlation 무시
- ❌ evidence 없는 recommendation

---

## 23. Non-Goals

Observability 시스템은 다음을 목표로 하지 않는다.

- autonomous remediation
- LLM-only diagnosis
- human replacement
- uncontrolled automation

---

## 24. 핵심 원칙

| 계층 | 역할 |
|------|------|
| Metrics | 정량 상태 |
| Logs | 이벤트 기록 |
| Traces | propagation 분석 |
| Governance Timeline | 운영 이력 |
| Systems-Math | 정량 설명 |
| Human | 최종 판단 |

---

> 🎯 **한 줄 핵심**
>
> Observability의 목적은 모니터링이 아니다.
> → 운영 판단을 위한 신뢰 가능한 Evidence를 제공하는 것이다.