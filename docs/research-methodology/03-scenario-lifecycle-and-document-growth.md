# 03. Scenario Lifecycle and Document Growth

> 정독: 1회

> **목적**: 7종 운영 문서 각각의 생명주기, 성장 모델, 버전 관리 전략을 정의합니다.

---

## 1. 문서 성장 모델 개요

7종 운영 문서는 성장 속도와 역할에 따라 두 가지 유형으로 분류된다.

```
[정적 문서] 장애 유형 재정의 시에만 수정
    scenarios/
    runbooks/

[동적 문서] 실험 및 장애 발생마다 지속 증가
    experiments/
    postmortems/
    improvements/
    preventive-designs/
    systems-math/
```

---

## 2. 문서별 생명주기 상세

### 2-1. scenarios/ (정적)

```
생성 시점:
    장애 유형 카탈로그 확정 후 초기 일괄 생성
    (Scenario Matcher / Runbook Resolver 코드 경계 확정 이후)

수정 시점:
    - 실험 반복을 통해 감지 임계값이 변경될 때
    - 연관 장애 유형이 추가될 때
    - 새로운 관측 지표가 발굴될 때

삭제 시점:
    해당 장애 유형이 아키텍처 변경으로 구조적으로 소멸할 때

목표 파일 수:
    90개 내외 (9개 스택 × 10개 장애 유형)

파일 명명 규칙 예시:
    jvm-runtime-cpu-saturation.md
    distributed-cache-redis-latency-spike.md
    distributed-messaging-kafka-consumer-lag.md
```

**버전 관리 전략**:

```
scenario 문서는 버전 히스토리를 문서 내부에 기록한다.

예:
    ## 버전 히스토리
    | 버전 | 수정일 | 수정 내용 |
    |------|--------|-----------|
    | v1.0 | 2025-05-01 | 최초 작성 |
    | v1.1 | 2025-08-15 | CPU 임계값 90% → 92%로 조정 (실험 #047 반영) |
```

---

### 2-2. runbooks/ (상대적으로 정적)

```
생성 시점:
    해당 scenario 문서와 동시에 생성 (1:1 대응)

수정 시점:
    - 실험에서 더 효과적인 조치 절차가 발견될 때
    - Rollback 절차가 개선될 때
    - Verification 기준이 변경될 때
    - 인프라 환경 변경으로 조치 방법이 바뀔 때

목표 파일 수:
    90개 내외 (scenario와 1:1 대응)

파일 명명 규칙 예시:
    jvm-runtime-cpu-saturation-runbook.md
```

---

### 2-3. experiments/ (지속 증가 - 가장 빠름)

```
생성 시점:
    장애 주입 실험을 1회 수행할 때마다 1개 추가

성장 패턴:
    Phase 1 (단일 장애): 90회 기본 실험
    Phase 2 (복합 장애): 수백 회 추가
    Phase 6 (논문용 반복): 동일 장애 30회 이상 반복
    → 최종 수백 개 이상

파일 명명 규칙:
    EXP-{YYYYMMDD}-{순번}-{장애유형}.md
    예: EXP-20260115-001-jvm-cpu-saturation.md

포함 내용 (표준 포맷):
    실험 ID
    실험 날짜
    스택 / 장애 유형
    실험 Phase (Phase 1~6)
    실험 환경
    장애 주입 명령
    1차 관측 도구 및 결과 (Prometheus / Loki / Tempo)
    2차 심층 진단 도구 및 결과 (eBPF / perf / async-profiler)
    측정 지표 테이블
    Assessment 결과
    권장 조치 및 승인 여부
    Verification 결과
    결론 및 추가 조사 항목
    연결 문서 (postmortem, systems-math 링크)
```

---

### 2-4. postmortems/ (지속 증가)

```
생성 시점:
    장애 종료 선언 이후 (사용자 승인 + 모니터링 정상화 + Verification 통과)
    실험 1회 당 1개 생성

파일 명명 규칙:
    PM-{YYYYMMDD}-{순번}-{장애유형}.md
    예: PM-20260115-001-jvm-cpu-saturation.md

포함 내용:
    사건 요약 (Timeline 포함)
    Root Cause 후보 (AI 확정 아님, 사용자 승인 기반)
    영향 범위 (Blast Radius)
    조치 이력 (Recommendation → Approval → Execution 반복 횟수 포함)
    MTTD / MTTR
    Rollback 성공 여부
    Verification 통과 여부
    재발 방지 항목 → improvements, preventive-designs 연결
```

---

### 2-5. improvements/ (지속 증가)

```
생성 시점:
    postmortem에서 개선 항목이 도출될 때마다 추가

포함 내용:
    개선 대상 (코드 / 설정 / 프로세스 / 인프라)
    개선 배경 (연결 postmortem ID)
    개선 방향 및 우선순위
    완료 기준
    적용 여부 및 결과
```

---

### 2-6. preventive-designs/ (지속 증가)

```
생성 시점:
    postmortem 또는 실험에서 예방 설계 필요성이 도출될 때

포함 내용:
    예방 대상 장애 유형
    현재 취약 포인트 분석
    예방 설계 방안 (아키텍처 / 설정 / 모니터링 추가)
    도입 우선순위
    도입 후 검증 방법
```

---

### 2-7. systems-math/ (지속 증가 - 논문 핵심)

```
생성 시점:
    experiments 결과를 정량화하여 통계 분석이 필요할 때
    논문 데이터셋 구성 시

포함 내용:
    Primary Evidence 테이블 (Prometheus / Loki / Tempo)
    Secondary Evidence 테이블 (eBPF / perf / async-profiler)
    Baseline vs Injection 비교
    MTTD / MTTR / Rollback 성공률 / Verification 성공률
    P50 / P95 / P99 Latency 분포
    통계 분석 (평균, 표준편차, 신뢰구간)
    논문 섹션 연결 (어떤 실험 결과가 논문 어느 절에 사용되는지)
```

---

## 3. 문서 성장 시뮬레이션

| 단계 | 활동 | 증가 문서 |
|------|------|-----------|
| 초기 구축 | 90개 장애 유형 정의 | scenarios × 90, runbooks × 90 |
| Phase 1 실험 | 90개 단일 장애 주입 | experiments × 90, postmortems × 90 |
| Phase 2~3 실험 | 복합 장애 수백 회 | experiments 수백 개 추가 |
| Phase 6 반복 실험 | 동일 장애 30회 반복 | experiments 수백 개 추가 |
| 논문 준비 | 데이터 정량화 | systems-math 수백 개 |
| 지속 운영 | 개선 및 예방 설계 반영 | improvements, preventive-designs 수백 개 |

---

## 4. 논문 시점에서 문서별 중요도

```
논문 시점에 가장 많이 커지고 논문 가치가 높은 문서:

    experiments        ← 실험 재현 근거
    systems-math       ← 정량 데이터셋
    postmortems        ← 장애 패턴 축적 근거
    improvements       ← 플랫폼 신뢰성 개선 근거

논문 심사 시 제출 가능한 보조 자료:
    scenarios, runbooks → 연구 방법론 섹션 근거
    preventive-designs  → 예방 효과 증명 근거
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*