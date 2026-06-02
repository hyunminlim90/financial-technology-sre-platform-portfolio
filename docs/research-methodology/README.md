# FIN-SRE Platform: Research Methodology

> **목적**: FIN-SRE 플랫폼의 연구 설계, 실험 전략, 논문화 방향을 정의하는 방법론 문서 모음입니다.
> 이 디렉터리는 `rag/docs`의 학습 자산과 별개로, 플랫폼 연구의 **설계도(blueprint)** 역할을 합니다.

---

## 문서 목록 및 읽는 순서

| 순서 | 파일명 | 핵심 주제 | 대응 방향성 |
|------|--------|-----------|-------------|
| 01 | `01-research-scope-and-stack-model.md` | 9개 핵심 스택, 90개 장애 유형 카탈로그 전략 | 방향성 정리 1 |
| 02 | `02-knowledge-and-document-hierarchy.md` | rag/docs 역할, 7종 운영 문서 계층, 우선순위 | 방향성 정리 2~3 |
| 03 | `03-scenario-lifecycle-and-document-growth.md` | 7종 문서 성장 모델, 생명주기 전략 | 방향성 정리 4 |
| 04 | `04-failure-injection-research-strategy.md` | 장애 주입 Phase 전략, 복합 장애 설계 | 방향성 정리 5 |
| 05 | `05-observability-and-deep-evidence-strategy.md` | 1차/2차 Evidence 구분, 관측 도구 역할 분리 | 방향성 정리 6 |
| 06 | `06-diagnostic-evidence-and-tooling.md` | eBPF/perf/profiler 문서화 표준 | 방향성 정리 7 |
| 07 | `07-human-in-the-loop-operational-model.md` | 장애 Lifecycle, 승인 프로세스, 종료 조건 | 방향성 정리 8~9 |
| 08 | `08-paper-data-pipeline.md` | 논문 데이터 파이프라인, papers/ 구조 | 방향성 정리 10 |
| 09 | `09-evidence-reliability-research-roadmap.md` | 논문 주제, Evidence Reliability 연구 방향 | 방향성 정리 10 |

---

## 연구 목적 한 줄 요약

> **Evidence-Based Human-in-the-Loop Operational Reliability Platform**을 설계·구현·실험하여,
> 복합 장애 상황에서 단일 알람 기반 대응 대비 SRE 플랫폼의 권장 조치 신뢰성과 서비스 안정성을 정량적으로 증명한다.

---

## 관련 폴더 구조 (포트폴리오 루트 기준)

```
fin-tech-sre-platform-portfolio/
│
├── docs/
│   └── research-methodology/        ← 현재 위치 (연구 설계도)
│
├── rag/
│   └── docs/                        ← 학습 자산 (이론, 메커니즘 문서)
│
├── scenarios/                       ← 90개 내외 장애 시나리오 (정적)
├── runbooks/                        ← 장애 대응 런북
├── experiments/                     ← 실험 결과 축적 (지속 증가)
├── postmortems/                     ← 장애 종료 후 회고 (지속 증가)
├── improvements/                    ← 개선 사항 (지속 증가)
├── preventive-designs/              ← 예방 설계 (지속 증가)
├── systems-math/                    ← 정량 연구 데이터 (지속 증가)
└── papers/                          ← 논문 초안 및 데이터셋 (미래)
```

---

## 핵심 원칙 (변하지 않는 기반)

```
AI는 Git 저장소를 직접 수정하지 않는다   (읽기 전용)
AI는 Root Cause를 확정하지 않는다
AI는 권장/보고/근거 제시만 수행한다
모든 조치는 사용자 승인 이후에 실행된다
장애 종료는 사용자 승인 + 모니터링 정상화 + Verification 통과 조건을 모두 충족해야 한다
```

---

## 최종 연구 성과 목표

```
90개 Scenario / Runbook
    ↓
수백 회 Experiment (단일 → 복합 → 논문용 반복)
    ↓
수백 개 Postmortem / Improvement / Preventive Design
    ↓
수백 개 Systems-Math 데이터셋
    ↓
석사 논문 → 박사 논문 → 산업 적용 연구
```