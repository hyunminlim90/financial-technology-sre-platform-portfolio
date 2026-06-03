# 07. Human-in-the-Loop Operational Model

> 정독: 1회

**목적**: FIN-SRE 플랫폼의 장애 대응 Lifecycle 전체를 정의합니다.

권장 조치 1회 승인으로 종료하는 것이 아니라, 장애 종료 선언 조건이 충족될 때까지 Recommendation → Approval → Execution → Verification → Re-Assessment 사이클이 반복됨을 명세합니다.

## 1. 플랫폼 정체성 정의

FIN-SRE 플랫폼은 일반적인 **AIOps 시스템이 아니다.**

```
AIOps 일반 정의:
    AI가 자동으로 장애를 감지하고 복구를 실행하는 시스템

FIN-SRE 플랫폼 정의:
    Evidence-Based Human-in-the-Loop Operational Reliability Platform

핵심 차이:
    AI 자동 실행     X
    AI 자동 복구     X
    AI Root Cause 확정  X
    AI 직접 변경     X

    Evidence 수집    O
    Assessment       O
    Recommendation   O
    Human Approval   O (필수)
    Verification     O
    Re-Assessment    O
```

## 2. 불변 원칙 (Core Principles)

```
AI는 Git 저장소를 직접 수정하지 않는다   (읽기 전용)
AI는 Root Cause를 확정하지 않는다       (가설 제시만)
AI는 권장 / 보고 / 근거 제시만 수행한다
모든 조치는 Human Approval 이후에만 실행된다
장애 종료는 아래 세 조건을 모두 충족해야 한다:
    (1) 모니터링 정상화 (설정된 정상 수치 회복)
    (2) Verification 통과
    (3) 사용자 장애 종료 승인
```

## 3. 장애 대응 Lifecycle 전체 흐름

```
장애 발생 (장애 주입 또는 실제 장애)
        │
        ▼
[1단계] Evidence 수집
    Prometheus / Loki / Tempo 자동 수집
        │
        ▼
[2단계] Evidence 평가
    Evidence Reliability 점수 산출
    충분 → 다음 단계
    부족 → 심층 진단 권고 (Human Approval → eBPF/perf 실행 → Evidence 재평가)
        │
        ▼
[3단계] Scenario Matching
    수집된 Evidence와 scenarios/ 문서 매칭
    신뢰도 점수 산출 (0~100%)
        │
        ▼
[4단계] Runbook Mapping
    매칭된 Scenario에 대응하는 runbooks/ 조회
    권장 조치 후보 목록 생성
        │
        ▼
[5단계] Recommendation 생성
    권장 조치 보고서 생성 (아래 포함):
        - 어느 계층인가
        - 어떤 컴포넌트인가
        - 어떤 설정/런타임 상태를 조정하는가
        - 왜 이 조치가 후보인가
        - Rollback 절차는 무엇인가
        - Verification 기준은 무엇인가
        │
        ▼
[6단계] Human Approval / Rejection
    승인 → 조치 수행
    거절 → 다른 권장 조치 후보 요청 또는 추가 Evidence 수집
        │
        ▼
[7단계] 조치 수행 (운영자 직접 실행)
        │
        ▼
[8단계] Verification
    정의된 Verification 기준 충족 여부 확인
    Prometheus / Loki / Tempo 수치 정상화 확인
        │
        ├─ Verification 통과 + 정상화 확인
        │       ↓
        │   [9단계] Re-Assessment
        │       장애 상태 지속 여부 판단
        │       ├─ 장애 지속 → [4단계]로 복귀 (새로운 Recommendation 생성)
        │       └─ 장애 해소 → [10단계]로 진행
        │
        └─ Verification 실패
                ↓
            Re-Assessment → 새로운 Recommendation 생성 → [6단계]로 복귀

        ↓
[10단계] 장애 종료 조건 확인
    (1) 모니터링 정상 수치 회복
    (2) Verification 통과
    (3) 사용자 장애 종료 승인
    세 조건 모두 충족 시 → [11단계]
        │
        ▼
[11단계] Incident Close
    Postmortem 생성 초안 제공
    Improvement 초안 제공
    Preventive Design 초안 제공
    Experiment 결과 저장 가이드 제공
    Systems-Math 초안 제공
    → 사용자가 직접 문서 반영
```

## 4. Recommendation 보고서 표준 내용

```
권장 조치 보고서 포함 항목:

1. 현재 Scenario Match 결과
    - 매칭된 장애 시나리오명
    - 신뢰도 점수 (0~100%)
    - 매칭 근거 Evidence 목록

2. 권장 조치 상세
    - 대상 계층 (JVM / Network / DB / Kubernetes 등)
    - 대상 컴포넌트 (ThreadPool / Connection Pool / GC 설정 등)
    - 조치 내용 (설정값 변경 / 재시작 / 스케일 아웃 등)
    - 조치 근거 (어떤 Evidence가 이 조치를 뒷받침하는가)
    - 예상 효과 (조치 후 어떤 지표가 어느 수준으로 개선될 것인가)

3. Rollback 계획
    - Rollback 트리거 조건 (Verification 실패 기준)
    - Rollback 절차 (단계별 명세)
    - Rollback 후 Verification 기준

4. Verification 기준
    - 정상 판정 수치 (예: CPU < 60%, P99 Latency < 200ms)
    - 측정 대기 시간 (예: 조치 후 5분 안정화 대기)
    - Verification 도구 (Prometheus / Loki / Tempo 지표)
```

## 5. Recommendation 순환 예시 (JVM CPU Saturation)

```
초기 상태:
    CPU 97%, P99 Latency 3.1s, Error Rate 12.4%

[1차 Recommendation]
    권장 조치: Thread Pool 상태 점검 및 Active Thread 수 확인
    승인
    실행 결과:
        CPU 95%
        Latency 2.8s
    Verification: 미통과 (기준: CPU < 60%)
    Re-Assessment: 장애 지속

[2차 Recommendation]
    권장 조치: Long-running Query 확인 및 DB Connection Pool 상태 점검
    승인
    실행 결과:
        CPU 80%
        Latency 1.2s
    Verification: 미통과 (기준: CPU < 60%)
    Re-Assessment: 부분 개선, 장애 지속

[3차 Recommendation]
    권장 조치: DB Connection Pool 최대 크기 +50% 조정, ThreadPool 재설정
    승인
    실행 결과:
        CPU 38%
        Latency 115ms
        Error Rate 0.1%
    Verification: 통과
    Re-Assessment: 장애 해소

사용자 장애 종료 승인 → Incident Close
```

## 6. 장애 종료 조건 체크리스트

```
□ 모니터링 정상화 확인
    □ CPU Utilization < 설정 임계값
    □ P99 Latency < 설정 임계값
    □ Error Rate < 설정 임계값
    □ 결제 관련 지표 정상 (Payment Success Rate, Timeout Rate 등)

□ Verification 통과 확인
    □ 조치 후 안정화 대기 시간 경과
    □ 정상 수치 일정 시간 이상 유지

□ 사용자 장애 종료 승인
    □ 운영자가 최종 정상화 확인
    □ 장애 종료 선언
```

## 7. Incident Close 이후 문서화 플로우

```
Incident Closed
    │
    ├── postmortem 초안 생성 (AI 제공)
    │       사용자 검토 및 보완
    │       사용자 Git 반영
    │
    ├── improvement 초안 생성 (AI 제공)
    │       사용자 Git 반영
    │
    ├── preventive-design 초안 생성 (AI 제공)
    │       사용자 Git 반영
    │
    ├── experiment 결과 기록 가이드 (AI 제공)
    │       사용자 Git 반영
    │
    └── systems-math 정량 데이터 초안 (AI 제공)
            사용자 Git 반영
            논문 데이터셋 축적
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*