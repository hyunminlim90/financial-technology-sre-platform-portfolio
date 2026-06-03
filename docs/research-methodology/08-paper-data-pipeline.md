# 08. Paper Data Pipeline

> 정독: 0회

**목적**: 장애 주입 실험 데이터가 논문 데이터셋으로 변환되는 전체 파이프라인을 정의합니다.

papers/ 폴더 구조, 논문 섹션과 실험 데이터의 연결 방식, 단계별 논문화 전략을 명세합니다.

## 1. 논문 데이터 파이프라인 전체 구조

```
장애 주입 실험
        │
        ▼
experiments/          ← 실험 원시 데이터 (도구, 명령, 결과)
        │
        ▼
postmortems/          ← 장애 패턴, Timeline, 조치 이력
        │
        ├──────────────────────────────────┐
        ▼                                  ▼
improvements/                      preventive-designs/
        │                                  │
        └──────────────────┬───────────────┘
                           ▼
                    systems-math/     ← 정량 데이터셋 (통계 분석)
                           │
                           ▼
                      papers/         ← 논문 초안 및 최종본
                           │
                           ▼
              학위 논문 심사 / 학회 제출 / 오픈소스 공개
```

## 2. papers/ 폴더 구조

```
papers/
│
├── README.md                         ← 논문 연구 목적 및 현재 상태
│
├── notes/                            ← 연구 노트 및 아이디어 정리
│   ├── evidence-reliability-notes.md
│   ├── scenario-matching-analysis.md
│   └── related-works.md
│
├── drafts/                           ← 논문 초안 (섹션별)
│   ├── 01-abstract.md
│   ├── 02-introduction.md
│   ├── 03-methodology.md
│   ├── 04-experiments.md
│   ├── 05-results.md
│   ├── 06-discussion.md
│   └── 07-conclusion.md
│
├── datasets/                         ← 논문에 사용되는 정제된 데이터셋
│   ├── phase1-single-failure/
│   ├── phase2-multi-layer-failure/
│   ├── phase3-payment-domain-failure/
│   ├── phase4-observability-gap/
│   ├── phase5-rollback-stability/
│   └── phase6-repeated-experiments/
│
├── figures/                          ← 논문 삽입용 그래프 / 다이어그램
│   ├── architecture-diagram.svg
│   ├── mttr-comparison-chart.png
│   ├── evidence-reliability-heatmap.png
│   └── flame-graph-cpu-saturation.html
│
└── published/                        ← 최종 제출본 / 게재 확정본
    ├── master-thesis-draft-v1.pdf
    └── conference-paper-draft-v1.pdf
```

## 3. 논문 섹션과 실험 데이터 연결

### 3-1. Abstract

```
연구 질문:
    Evidence Reliability 기반 Human-in-the-Loop SRE 플랫폼이
    복합 장애 상황에서 단일 알람 기반 대응보다 더 신뢰 가능한
    권장 조치를 생성하는가?

핵심 결과 요약:
    systems-math/ 데이터셋에서 MTTD / MTTR / Rollback Success Rate
    비교 결과 요약
```

### 3-2. Introduction

```
연구 배경:
    현대 금융 시스템의 복합 장애 발생 패턴
    기존 단일 알람 기반 대응의 한계
    Human-in-the-Loop 접근 방식의 필요성

연구 목표:
    FIN-SRE 플랫폼의 설계 및 구현
    복합 장애 실험을 통한 정량적 검증

연구 기여:
    Evidence Reliability Framework
    Scenario Matching 알고리즘
    Human Approval 기반 Lifecycle 모델
```

### 3-3. Methodology (연구 방법론)

```
연결 문서:
    docs/research-methodology/ (현재 위치한 전체 문서)
    scenarios/ (90개 장애 유형 카탈로그)
    runbooks/ (대응 절차 명세)

포함 내용:
    실험 환경 명세 (Kubernetes, JDK, Spring Boot 버전)
    장애 주입 도구 및 절차
    관측 도구 체계 (1차 / 2차)
    Evidence 평가 방법
    Scenario Matching 알고리즘 설명
    Human-in-the-Loop Lifecycle 설명
```

### 3-4. Experiments (실험)

```
연결 문서:
    experiments/ (전체 실험 기록)

포함 내용:
    Phase 1~6 실험 설계 및 결과 요약
    단일 장애 vs 복합 장애 비교
    관측 불완전성 실험 결과
    롤백 안정성 실험 결과
    반복 실험 통계 (30회 이상)
```

### 3-5. Results (결과)

```
연결 문서:
    systems-math/ (정량 데이터셋)

주요 측정 결과:
    MTTD 비교 (SRE 플랫폼 vs 수동 대응)
    MTTR 비교
    Scenario Match 정확도
    False Positive / Negative Rate
    Rollback Success Rate
    Verification Success Rate
    P99 Latency 회복 시간
    결제 무결성 유지율
```

### 3-6. Discussion (논의)

```
포함 내용:
    예상치 못한 실험 결과 분석
    Evidence Reliability의 한계
    Human Approval 프로세스의 효율성 트레이드오프
    복합 장애에서의 플랫폼 행동 분석
    관측 불완전성 상황에서의 안전 판단 유보 검증
```

### 3-7. Conclusion (결론)

```
포함 내용:
    연구 질문에 대한 최종 답변
    FIN-SRE 플랫폼의 학문적 / 산업적 기여
    향후 연구 방향 (박사 연구 주제 연결)
```

## 4. 논문 데이터셋 구성 전략

### 4-1. datasets/ 폴더 데이터 정제 기준

```
원시 데이터 (experiments/, systems-math/)
    ↓
이상치 제거 및 데이터 정제
    ↓
논문 삽입용 테이블 형태로 변환
    ↓
datasets/{phase}/ 에 CSV / JSON 형태로 저장
    ↓
논문 drafts/ 에서 참조
```

### 4-2. figures/ 생성 기준

```
포함해야 하는 그래프:
    MTTD / MTTR 비교 바 차트 (SRE 플랫폼 vs 수동 대응)
    Evidence Reliability 점수 히트맵 (장애 유형별)
    Latency 분포 박스플롯 (단일 vs 복합 장애)
    Rollback Success Rate 추이
    Phase별 실험 결과 비교 레이더 차트
    CPU Saturation 실험 Flame Graph

생성 도구:
    Python matplotlib / seaborn → 통계 그래프
    async-profiler → Flame Graph
    Grafana → 대시보드 스크린샷
```

## 5. 논문화 단계별 타임라인

```
현재 단계:
    FIN-SRE 플랫폼 구축 중
    rag/docs 학습 문서 축적 중
    docs/research-methodology/ 방법론 문서 작성 중

단계 1 (플랫폼 완성):
    Scenario Matcher / Runbook Resolver 구현 완료
    90개 장애 유형 문서 생성

단계 2 (Phase 1~3 실험):
    단일 / 복합 장애 실험 수행
    experiments / postmortems / systems-math 축적

단계 3 (Phase 4~6 실험):
    관측 불완전성 / 롤백 / 반복 실험 수행
    논문 데이터셋 정제

단계 4 (논문 초안 작성):
    papers/drafts/ 섹션별 초안 작성
    figures/ 그래프 생성
    datasets/ 데이터셋 정제

단계 5 (대학원 진학):
    석사 논문 심사
    데이터셋 공개 (재현 가능성 보장)
    박사 연구 주제 확장
```

## 6. 대학원 진학 전 준비 가능한 연구 자산

```
대학원 입학 전에 독립적으로 준비 가능한 항목:

    papers/notes/          ← 연구 아이디어 및 관련 연구 정리
    papers/drafts/         ← 논문 섹션별 초안 작성
    papers/datasets/       ← 실험 데이터셋 정제
    papers/figures/        ← 그래프 / 시각화

대학원 입학 후 교수 지도 하에 진행:
    논문 심사 통과 전략
    Related Works 보완
    학술 용어 정제
    학회 제출 논문 작성
    석사 학위 논문 최종본

핵심:
    연구 = 대학원 없이도 가능
    학위 논문 심사 = 대학원 필수
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*