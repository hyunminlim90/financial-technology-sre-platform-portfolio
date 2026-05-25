# protocols/runtime-portfolio-publication-contract

## 1. 목적

이 문서는 FIN-SRE 플랫폼의 Runtime Portfolio Publication Layer를 정의한다.

Portfolio Publication Runtime의 목적은 단순 GitHub 업로드 관리가 아니다.

목적은 다음을 기반으로:

- Operational Knowledge
- Reliability Theory
- Experiment
- Operational Learning
- Research Asset
- Sanitized Publication
- Academic Extension

설명 가능하고, 재현 가능하며, 정량 검증 가능하고, 운영 현실 기반이며, 논문화 가능한 **Operational Reliability Portfolio Publication Runtime**을 formalization 하는 것이다.

---

## 2. 핵심 개념

Portfolio Publication Runtime은 단순 포트폴리오 정리 시스템이 아니다.

Portfolio Publication Runtime은 다음 속성을 가진 **operational reliability publication governance runtime**이다.

- Evidence-aware
- Visibility-aware
- Sanitization-aware
- Research-aware
- Reliability-aware
- Human-governed

---

## 3. Canonical Portfolio Definition

Portfolio는 단순 기술 블로그가 아니다. Portfolio는 다음을 포함하는 **Operational Reliability Publication Surface**다.

- 운영 reliability knowledge
- 실험 가능성
- 정량 검증 가능성
- 재현 가능성
- research extension 가능성

---

## 4. Canonical Publication Domains

| Domain | 목적 |
|---|---|
| Public Portfolio | 공개 기술 포트폴리오 |
| Sanitized Operational Knowledge | 민감정보 제거 운영 지식 |
| Reliability Research Candidate | 연구 후보 |
| Experiment Publication | 실험 결과 공개 |
| Academic Publication | 논문화/학회 확장 |
| Internal Research Archive | 내부 연구 보관 |

---

## 5. Human Governance Rule

Portfolio Publication Runtime은 Human Governance 제거 금지.

**원칙**

- AI는 publication draft 생성 가능
- Human이 publication suitability와 sanitization completeness 승인

**금지**

- ❌ AI-only operational publication
- ❌ unreviewed incident evidence exposure
- ❌ unsupported operational claim publishing

---

## 6. Canonical Publication Lifecycle

Portfolio Publication은 canonical lifecycle을 가져야 한다.

```
DRAFT → SANITIZING → REVIEWING → VALIDATED → PUBLISHED → DEPRECATED → ARCHIVED
```

---

## 7. Public Portfolio Rule

공개 포트폴리오는 generalized operational knowledge 중심이어야 한다.

**허용:** architecture, generalized scenario, runbook, preventive design, technical review, sanitized postmortem

**금지:** raw operational evidence, customer payload, internal topology dump, financially sensitive incident detail

---

## 8. Private Research Rule

Private Research는 raw operational detail 포함 가능.

예: raw metrics, detailed propagation analysis, rollback failure evidence, experiment raw data

**원칙:** external publication 이전 sanitization 필수

---

## 9. Publication Visibility Rule

모든 publication artifact는 visibility classification을 가져야 한다.

허용 분류: `PUBLIC_PORTFOLIO`, `PRIVATE_RESEARCH`, `INTERNAL_OPERATION`, `PAPER_CANDIDATE`, `SANITIZED_EXPORT`

---

## 10. Sanitized Export Rule

Sanitized Export는 generalized operational insight 중심이어야 한다.

**제거 대상:** internal IP, token, secret, customer payload, payment identifier, kubeconfig

**허용:** generalized propagation pattern, generalized rollback strategy, generalized verification flow

---

## 11. Portfolio Knowledge Set Rule

Portfolio Publication은 Knowledge Set 기반이어야 한다.

포함 가능: `Scenario`, `Runbook`, `Systems-Math`, `Experiment`, `Improvement`, `Preventive Design`

---

## 12. Reliability-aware Rule

Portfolio Publication은 reliability-aware 해야 한다.

예: rollback reliability, verification reliability, propagation containment reliability

---

## 13. Propagation-aware Rule

Portfolio Publication은 propagation-aware 해야 한다.

예: retry amplification, queue backlog, dependency cascade, tail latency propagation

---

## 14. Retry Amplification Rule

Retry amplification은 canonical publication 대상이다.

```
timeout → retry storm → queue overload → DB saturation
```

---

## 15. Rollback-aware Rule

Portfolio Publication은 rollback-aware 해야 한다.

포함: rollback trigger, rollback safety, rollback convergence, rollback verification

---

## 16. Verification-aware Rule

Portfolio Publication은 verification-aware 해야 한다.

포함: queue stabilization validation, latency recovery validation, payment consistency validation

---

## 17. Convergence-aware Rule

Portfolio Publication은 convergence-aware 해야 한다.

**금지:** unstable recovery를 successful recovery로 publication

---

## 18. FinTech Safety Rule

FinTech 환경에서는 payment correctness 우선.

**금지:** duplicate payment normalization, unsafe rollback recommendation, verification 없는 recovery publication

---

## 19. Human-in-the-loop Rule

고위험 operational action은 Human Approval requirement 포함 가능해야 한다.

예: cross-region failover, payment reconciliation, DB failover

---

## 20. Guardrail-aware Rule

Portfolio Publication은 Guardrail-aware 해야 한다.

예: payment safety guardrail, rollback mandatory guardrail, retry amplification guardrail

---

## 21. Systems-Math Rule

Portfolio Publication은 Systems-Math 연결 가능해야 한다.

예: Little's Law, queue utilization, retry amplification, tail latency propagation

**원칙:** Systems-Math는 operational reasoning formalization layer다.

---

## 22. Evidence-backed Rule

Portfolio Publication은 Evidence 기반이어야 한다.

**허용:** metrics, logs, traces, timeline, verification result, rollback result, experiment result

**금지:** fabricated operational knowledge, hallucinated propagation model, unsupported operational conclusion

---

## 23. Operational Reality Rule

Portfolio Publication은 현실 운영 기반이어야 한다.

**허용:** real propagation pattern, real rollback failure, real dependency cascade, real queue saturation

**금지:** toy-only infrastructure narrative

---

## 24. Quantitative Validation Rule

Portfolio Publication은 정량 검증 가능해야 한다.

예: MTTR, rollback success rate, propagation reduction, verification mismatch reduction

---

## 25. Statistical Validation Rule

Portfolio Publication은 statistical validation 지원 가능해야 한다.

예: confidence interval, variance, baseline comparison, repeated experiment

---

## 26. Experiment-aware Rule

Portfolio Publication은 Experiment Runtime과 연결되어야 한다.

포함: failure injection, rollback validation, verification validation, policy comparison

---

## 27. Benchmark-aware Rule

Portfolio Publication은 Benchmark Runtime과 연결되어야 한다.

예: rollback benchmark, verification benchmark, propagation containment benchmark

---

## 28. Research-aware Rule

Portfolio Publication은 Research Runtime과 연결되어야 한다.

포함: hypothesis, experiment, validation, paper candidate

---

## 29. Dataset-aware Rule

Portfolio Publication은 dataset accumulation 지원 가능해야 한다.

예: rollback dataset, verification dataset, propagation dataset, publication dataset

---

## 30. Research Assetization Rule

Portfolio Publication 결과는 research asset으로 연결 가능해야 한다.

예: Experiment Report, Reliability Analysis, Research Note, Paper Draft

---

## 31. Academic Publication Rule

Academic Publication은 reproducibility requirement를 가져야 한다.

필수: methodology, experiment reproducibility, quantitative validation, limitation

---

## 32. Academic Compatibility Rule

Portfolio Publication은 academic extension 지원 가능해야 한다.

지원 가능: IEEE format, ACM format, LaTeX export, appendix generation

---

## 33. Knowledge Graph Integration Rule

Portfolio Publication은 Knowledge Graph와 연결되어야 한다.

```
Scenario → Runbook → Experiment → Improvement → Preventive Design
```

---

## 34. Operational Memory Integration Rule

Portfolio Publication은 Operational Memory와 연결되어야 한다.

예: historical rollback pattern, historical propagation pattern, historical false recovery

---

## 35. Operational Consistency Integration Rule

Portfolio Publication은 Consistency Runtime과 연결되어야 한다.

```
verification mismatch → consistency correction
```

---

## 36. Operational Topology Integration Rule

Portfolio Publication은 Topology Runtime과 연결되어야 한다.

```
high dependency density → propagation risk documentation
```

---

## 37. Operational Lineage Integration Rule

Portfolio Publication은 Lineage Runtime과 연결되어야 한다.

```
incident lineage → rollback lineage → verification lineage → publication lineage
```

---

## 38. Causal Analysis Integration Rule

Portfolio Publication은 Causal Analysis와 연결되어야 한다.

```
retry storm causality → retry prevention design
```

---

## 39. Runtime Replay Rule

Portfolio Publication은 replayable 해야 한다.

예: incident replay, rollback replay, verification replay, experiment replay

---

## 40. Reproducibility Rule

Portfolio Publication은 reproducible 해야 한다.

```
same topology + same policy + same experiment → same operational conclusion
```

---

## 41. Timeline Governance Rule

Portfolio Publication은 chronology-aware 해야 한다.

```
failure → propagation → rollback → verification → stabilization → convergence
```

---

## 42. Context-awareness Rule

Portfolio Publication은 context-aware 해야 한다.

포함: service, environment, traffic pattern, impact scope

---

## 43. Environment-aware Rule

Portfolio Publication은 environment-aware 해야 한다.

예: production, staging, sandbox

**원칙:** production → strictest publication governance

---

## 44. Severity-aware Rule

Portfolio Publication은 severity-aware 해야 한다.

예: SEV-1, SEV-2, SEV-3

**원칙:** higher severity → stricter publication governance

---

## 45. Policy-aware Rule

Portfolio Publication은 policy-aware 해야 한다.

예: approval policy, rollback policy, verification policy, visibility policy

---

## 46. Unknown State Rule

Unknown 상태는 restrictive governance 적용.

예: missing metrics, partial observability, verification unavailable, rollback unavailable

**원칙:** Unknown → publication certainty 제한

---

## 47. Runtime DTO Rule

Portfolio Publication Runtime은 canonical DTO를 가져야 한다.

예: `PublicationDefinition`, `VisibilityDefinition`, `ResearchCandidateDefinition`, `SanitizedExportDefinition`

---

## 48. Explainability Rule

Portfolio Publication은 explainable 해야 한다.

**포함:** why rollback required, why propagation occurred, why verification mandatory, why preventive design necessary

**금지:** opaque operational narrative

---

## 49. Runtime Security Rule

Portfolio Publication Runtime은 privileged operational layer다.

**필수:** authenticated access, RBAC, audit logging, visibility control

**금지**

- ❌ anonymous publication
- ❌ unrestricted operational exposure
- ❌ public raw operational evidence exposure

---

## 50. Auditability Rule

Portfolio Publication lifecycle은 audit 가능해야 한다.

포함: what evidence analyzed, what benchmark validated, what verification completed, what human approved

---

## 51. Immutable Audit Rule

Portfolio Publication audit는 append-only 해야 한다.

**금지**

- ❌ audit overwrite
- ❌ hidden publication mutation
- ❌ invisible lineage corruption

---

## 52. Runtime Failure Rule

Portfolio Publication Runtime failure는 explicit 해야 한다.

예: publication inconsistency, timeline inconsistency, verification unavailable, rollback unavailable

**금지:** silent operational publication corruption

---

## 53. Runtime Metrics Governance Rule

Portfolio metric은 low-cardinality 유지해야 한다.

**허용:** service, domain, severity, failure_mode, publication_type

**금지:** customer identifier, payment payload, trace payload dump

---

## 54. Research Integrity Rule

Portfolio Publication Runtime은 research integrity 보장해야 한다.

**금지:** fabricated operational evidence, fabricated propagation model, unsupported reliability conclusion, hidden contradictory evidence

---

## 55. Long-term Publication Evolution Rule

Portfolio Publication Runtime은 장기 publication evolution 지원 가능해야 한다.

예: rollback publication evolution, verification publication evolution, propagation publication evolution, research publication evolution

---

## 56. Academic Extension Rule

장기적으로 다음 연구 방향 지원 가능.

예: Operational Reliability Publication Systems, rollback-aware reliability publication, verification-aware operational research publication, Human-in-the-loop operational publication governance

---

## 57. Anti-Pattern Rule

**금지**

- ❌ rollback 없는 runbook publication
- ❌ verification 없는 recovery conclusion
- ❌ propagation 없는 scenario publication
- ❌ systems-math 없는 quantitative reasoning
- ❌ evidence 없는 operational publication

---

## 58. Non-Goals

Portfolio Publication Runtime의 목표는 다음이 아니다.

- 단순 GitHub 업로드 자동화
- AI-only technical blogging
- unverifiable operational storytelling
- toy-level infrastructure portfolio

---

## 59. Canonical Runtime Layers

| Layer | 역할 |
|---|---|
| Drafting | 초안 생성 |
| Sanitization | 민감정보 제거 |
| Validation | 검증 |
| Publication Governance | 공개 정책 |
| Academic Extension | 논문화 확장 |
| Archival | 장기 보관 |

---

## 60. Reliability Research Formalization Direction

현재 방향의 핵심은 단순 기술 포트폴리오가 아니다.

목표는 운영 observability와 operational lineage를 다음 속성을 갖춘 **Operational Reliability Portfolio Publication Runtime**으로 formalization 하는 것이다.

- 설명 가능하고
- 재현 가능하며
- 정량 검증 가능하고
- 논문화 가능한

---

**한 줄 핵심**

> Runtime Portfolio Publication의 목적은 단순 GitHub 업로드 관리가 아니다.
> Operational Knowledge / Reliability Theory / Experiment / Research Asset을 sanitization, reproducibility, publication governance 기반으로 관리하여 Operational Reliability 자체를 장기 연구·포트폴리오·논문화 가능한 **Runtime Publication System**으로 formalization 하는 것이다.