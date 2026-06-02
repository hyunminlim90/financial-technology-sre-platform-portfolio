# 02. Knowledge and Document Hierarchy

> 정독: 1회

**목적**: rag/docs(학습 자산)와 7종 운영 문서(판단 자산)의 계층 구조, 역할, 우선순위를 정의합니다.

## 1. 전체 문서 계층 구조

```
FIN-SRE 플랫폼 문서 체계
│
├── [학습 자산] rag/docs/
│   └── 이론 학습, 메커니즘 문서
│       판단 로직 영향도: 최후순위
│
└── [판단 자산] 7종 운영 문서
    ├── scenarios/           ← 장애 시나리오 정의
    ├── runbooks/            ← 대응 절차 명세
    ├── postmortems/         ← 장애 종료 후 회고
    ├── improvements/        ← 개선 사항
    ├── preventive-designs/  ← 예방 설계
    ├── experiments/         ← 실험 결과 기록
    └── systems-math/        ← 정량 연구 데이터
```

## 2. rag/docs 역할 정의

### 2-1. 목적

`rag/docs`는 **SRE 플랫폼의 추론 과정에서 직접 판단 로직에 사용되지 않는다.**
대신, 사용자의 학습 자산이자 시스템 전반 이해의 기반 문서이다.

```
rag/docs의 역할:

    학습 공간 (Study Space)
    =
    SRE 엔지니어가 각 스택의 내부 메커니즘을 이해하기 위한 문서

    ≠

    판단 로직 (Decision Logic)
    ≠
    권장 조치 근거
```

### 2-2. 학습 범위

```
Bit 단위 / CPU 동작 원리
    ↓
Linux Kernel (System Call, Interrupt, Memory Management)
    ↓
OS 계층 (Process, Thread, Scheduler, Cgroup)
    ↓
JVM Runtime (GC, ClassLoader, Metaspace, Thread Model)
    ↓
Distributed Cache (Redis internals, Cluster, Replication)
    ↓
Distributed Messaging (Kafka internals, Consumer Group, Lag)
    ↓
Container Orchestration (Kubernetes, CRI, CNI, Scheduler)
    ↓
Observability (Prometheus, Loki, Tempo, eBPF, perf)
    ↓
LLM / RAG / AI-Agent / Guardrail (각 스택 인터페이스 및 장애 메커니즘)
```

### 2-3. 플랫폼 추론 시 참조 우선순위

```
1순위: scenarios
2순위: runbooks 
3순위: postmortems
4순위: improvements
5순위: preventive-designs
6순위: experiments
7순위: systems-math
최후순위: rag/docs
```

> rag/docs는 근거 보완 용도로만 참조된다.
> 직접적인 권장 조치 생성에는 사용되지 않는다.

## 3. 7종 운영 문서 역할 정의

### scenarios/

```
정의: 특정 장애 유형의 발생 조건, 관측 지표, 영향 범위, 감지 패턴을 정의한 문서
역할: Scenario Matcher의 매칭 기준
생명주기: 상대적으로 정적 (장애 유형 재정의 시에만 수정)
총 목표 수량: 90개 내외
```

포함 내용 예시:

```
- 장애 유형 이름 및 스택
- 발생 조건 (Trigger Conditions)
- 감지 지표 (Detection Metrics): 임계값 포함
- 영향 범위 (Blast Radius)
- 연관 장애 유형 (Related Scenarios)
- 문서 버전 및 최종 수정일
```

### runbooks/

```
정의: 특정 장애 시나리오에 대응하는 단계별 조치 절차
역할: Runbook Resolver의 조치 후보 소스
생명주기: 실험 결과 반영 시 가끔 수정
총 목표 수량: 90개 내외 (시나리오와 1:1 대응)
```

포함 내용 예시:

```
- 전제 조건 (Prerequisites)
- 단계별 조치 절차 (Step-by-step Actions)
    - 어느 계층인가
    - 어떤 컴포넌트인가
    - 어떤 설정/런타임 상태를 조정하는가
    - 왜 이 조치가 후보인가
- Rollback 절차
- Verification 기준
- 예상 소요 시간
```

### postmortems/

```
정의: 장애 종료 이후 작성하는 사후 회고 문서
역할: 과거 장애 패턴 학습, 미래 예방 설계 입력
생명주기: 장애 발생마다 추가 (지속 증가)
총 목표 수량: 수백 개 (실험 반복에 따라 증가)
```

### improvements/

```
정의: 장애 대응 이후 도출된 개선 사항
역할: 시스템 설계 및 운영 프로세스 개선 입력
생명주기: 지속 증가
총 목표 수량: 수백 개
```

### preventive-designs/

```
정의: 특정 장애 유형의 재발을 방지하기 위한 예방 설계 명세
역할: 사전 방어 아키텍처 설계 기반
생명주기: 지속 증가
총 목표 수량: 수백 개
```

### experiments/

```
정의: 장애 주입 실험의 환경, 절차, 도구, 결과, 결론을 기록한 문서
역할: 논문 실험 데이터의 원천 (Primary Source)
생명주기: 실험마다 추가 (가장 빠르게 증가)
총 목표 수량: 수백 개
```

포함 내용 (표준 형식 → 06번 문서 참조):

```
- 실험 환경 (Kubernetes, JDK, Spring Boot 버전 등)
- 장애 주입 명령 (stress-ng, chaos-mesh 등)
- 관측 도구 및 실행 명령
- 측정 결과 (CPU, Latency, Error Rate 등)
- 결론 및 추가 조사 항목
```

### systems-math/

```
정의: 실험에서 수집된 정량 데이터를 통계적으로 정리한 연구 데이터셋
역할: 논문의 결과 섹션 데이터 원천, Evidence Reliability 근거
생명주기: 연구 데이터 축적 (지속 증가)
총 목표 수량: 수백 개
```

포함 내용 예시:

```
- Primary Evidence (Prometheus / Loki / Tempo 수치)
- Secondary Evidence (eBPF / perf / profiler 수치)
- Baseline vs Injection 비교 테이블
- MTTD / MTTR / Rollback 성공률 / Verification 성공률
- 통계 분석 (평균, 중앙값, P95, P99, 표준편차)
```

## 4. 문서 간 관계 흐름

```
장애 발생 (장애 주입)
    ↓
scenarios/ → Scenario Match
    ↓
runbooks/ → 권장 조치 후보 생성
    ↓
사용자 승인 → 조치 수행 → Verification
    ↓
experiments/ → 실험 결과 기록
    ↓
postmortems/ → 사후 회고
    ↓
improvements/ → 개선 사항 도출
preventive-designs/ → 예방 설계 도출
systems-math/ → 정량 데이터 축적
    ↓
논문 데이터셋
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*