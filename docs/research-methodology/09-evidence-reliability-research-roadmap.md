# 09. Evidence Reliability Research Roadmap

> **목적**: FIN-SRE 플랫폼 연구의 핵심 학문적 주제인 Evidence Reliability Framework를 정의하고,
> 석사 → 박사 → 산업 적용까지의 장기 연구 로드맵을 명세합니다.

---

## 1. 핵심 연구 주제

### 1-1. 연구 주제 명칭

```
Evidence-Based Human-in-the-Loop Operational Reliability Platform
for Distributed Financial Systems
```

또는 축약 형태:

```
Evidence Reliability Framework for SRE in Fintech Systems
```

### 1-2. 연구 주제 선정 근거

```
기존 연구의 한계:
    단일 알람 기반 대응 → 복합 장애 상황에서 낮은 정확도
    자동 복구 시스템 → 금융 시스템에서 Human Oversight 부재
    AIOps 연구 → Evidence Reliability 정량 평가 미흡

FIN-SRE 플랫폼의 차별점:
    Evidence Reliability 점수 기반 판단 신뢰도 정량화
    Human-in-the-Loop 모델로 금융 시스템 안전성 보장
    Scenario Matching + Runbook Mapping 통합 Lifecycle
    복합 장애 실험을 통한 정량적 검증
```

---

## 2. Evidence Reliability Framework 정의

### 2-1. Evidence란 무엇인가

```
Evidence:
    장애 상황에서 수집된 관측 데이터의 총체

Primary Evidence:
    Prometheus (메트릭)
    Loki (로그)
    Tempo (트레이스)

Secondary Evidence (Deep Evidence):
    eBPF (커널 레벨 관측)
    perf (CPU 프로파일)
    async-profiler (JVM 프로파일)
    jstack (스레드 덤프)
    tcpdump (네트워크 패킷)
```

### 2-2. Evidence Reliability란 무엇인가

```
Evidence Reliability:
    수집된 Evidence가 특정 장애 시나리오와 얼마나
    신뢰 가능하게 연결되는지를 나타내는 점수

산출 요소:
    Evidence 완전성 (Completeness)
        → 필요한 지표가 모두 수집되었는가?
        → Metrics / Logs / Traces 모두 존재하는가?

    Evidence 일관성 (Consistency)
        → 수집된 지표들이 동일한 방향을 가리키는가?
        → 상충되는 Evidence가 존재하는가?

    Evidence 신선도 (Freshness)
        → 수집 시각과 분석 시각의 시간 차이
        → 오래된 Evidence는 신뢰도 감소

    Evidence 정밀도 (Precision)
        → 2차 Deep Evidence 포함 여부
        → 커널/JVM 레벨 증거가 있는가?

Evidence Reliability 점수 범위: 0 ~ 100%
    70% 미만: 심층 진단 권고
    70~85%: 조건부 Recommendation 생성 (낮은 신뢰도 명시)
    85% 이상: Recommendation 생성 (일반)
    95% 이상: High Confidence Recommendation
```

### 2-3. Assessment Reliability

```
Assessment Reliability:
    Evidence Reliability를 기반으로 생성된 Assessment(평가)의
    신뢰도를 나타내는 점수

영향 요소:
    Evidence Reliability 점수
    Scenario Match 신뢰도
    과거 동일 장애 유형 Postmortem 수 (경험 데이터)
    현재 관측 완전성 수준
```

### 2-4. Recommendation Reliability

```
Recommendation Reliability:
    생성된 권장 조치가 실제 장애 해소에 기여할
    가능성의 신뢰도 점수

영향 요소:
    Assessment Reliability
    Runbook 과거 성공률 (Historical Success Rate)
    현재 시스템 상태와 Runbook 전제 조건의 일치도
    Rollback 가능 여부
```

### 2-5. Decision Reliability

```
Decision Reliability:
    Human Approval 과정에서 운영자가 합리적인 판단을 내릴 수 있도록
    플랫폼이 제공하는 정보의 신뢰도

포함 항목:
    Evidence 근거 명시
    Recommendation Reliability 점수 표시
    대안 권장 조치 목록 제공
    예상 영향 범위 명시
    Rollback 계획 포함
```

---

## 3. 연구 로드맵

### 3-1. 석사 연구 (Master's Thesis)

```
연구 제목 (안):
    Evidence Reliability Framework를 활용한
    Human-in-the-Loop SRE 플랫폼의 금융 시스템 장애 대응 신뢰성 검증

연구 목표:
    FIN-SRE 플랫폼 설계 및 구현
    90개 장애 유형 카탈로그 구축
    Phase 1~3 실험 수행 (단일 → 복합 장애)
    MTTD / MTTR / 정확도 정량 비교

핵심 증명 명제:
    Evidence Reliability 기반 Human-in-the-Loop SRE 플랫폼이
    단일 알람 기반 대응보다 복합 장애 상황에서
    더 신뢰 가능한 권장 조치를 생성한다.

예상 논문 구조:
    Abstract
    Introduction (연구 배경, 연구 질문)
    Related Works (AIOps, SRE, Evidence-Based Systems)
    Methodology (플랫폼 설계, 실험 환경, 측정 방법)
    Experiments (Phase 1~3 실험 설계 및 결과)
    Results (정량 비교 데이터)
    Discussion (한계, 예상치 못한 결과)
    Conclusion (기여, 향후 연구)
```

---

### 3-2. 박사 연구 (PhD Dissertation)

```
연구 주제 확장 방향:

확장 1: 관측 불완전성 조건에서의 Evidence Reliability
    Phase 4 실험 데이터 기반
    Metrics / Logs / Traces 결측 조건별 신뢰도 모델링
    불완전 Evidence 조건에서 안전한 판단 유보 알고리즘

확장 2: 복합 계층 전파 장애 감지 알고리즘
    JVM → Redis → Kafka → API 전파 패턴 자동 감지
    Causal Graph 기반 장애 전파 추론

확장 3: Rollback 안정성 자동 평가 모델
    Phase 5 실험 데이터 기반
    Rollback 성공 예측 모델
    재악화 위험도 사전 평가

확장 4: Evidence Reliability와 MTTR 상관관계 모델링
    Phase 6 반복 실험 데이터 기반
    Evidence Reliability 점수와 MTTR의 통계적 관계 증명
    최적 Evidence 수집 전략 도출
```

---

### 3-3. 산업 적용 연구

```
목표:
    FIN-SRE 플랫폼을 실제 금융 기업 SRE 환경에 적용

연구 방향:
    실제 결제 시스템 데이터로 플랫폼 검증
    연구실 환경 vs 실제 프로덕션 환경 비교
    산업 SRE 팀의 Human-in-the-Loop 수용도 연구
    규제 환경(금융 컴플라이언스)에서의 Human Approval 모델 적용

기대 결과:
    지산학연 협력 논문
    오픈소스 FIN-SRE Platform 공개
    금융 SRE 플랫폼 표준 방법론 제안
```

---

## 4. 관련 연구 분야 (Related Works 기반)

```
연구 접점 분야:

AIOps:
    Lim et al., "AIOps: Real-World Challenges and Research Innovations"
    관련성: Evidence 기반 판단 모델과의 차별점 명시 필요

Observability:
    OpenTelemetry 기반 분산 관측 연구
    관련성: Evidence 수집 인프라 설계 근거

SRE:
    Google SRE Book, Betsy Beyer et al.
    관련성: Human-in-the-Loop 모델의 SRE 원칙과의 정렬

Human-in-the-Loop ML:
    Monarch: Google's Planet-Scale In-Memory Time Series Database
    관련성: 자동화와 인간 판단의 협업 모델

Evidence-Based Medicine:
    의학 분야의 Evidence 신뢰도 분류 체계
    관련성: Evidence Reliability Framework 설계 참조
```

---

## 5. 연구 자산 현황 및 로드맵

```
현재 보유 자산:
    FIN-SRE Platform 코드베이스 (진행 중)
    docs/research-methodology/ (현재 작성 중)
    rag/docs/ (학습 문서 축적 중)
    AI-SRE-OPERATIONAL-LIFECYCLE.md
    ARCHITECTURE.md
    FIN-SRE-RESEARCH-ROADMAP.md

대학원 입학 전 목표 자산:
    90개 scenarios / runbooks (완성)
    Phase 1~3 experiments (수백 개)
    systems-math 데이터셋 (기초 구성)
    papers/drafts/ 논문 초안 (섹션별)
    papers/notes/ 연구 노트 (관련 연구 정리)

대학원 입학 후 목표:
    Phase 4~6 experiments (심층 실험)
    논문 초안 완성
    석사 논문 심사 통과
    학회 논문 제출
```