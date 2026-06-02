# 04. Failure Injection Research Strategy

> 정독: 1회

**목적**: 논문 데이터 수준의 실험을 위한 장애 주입 전략을 Phase별로 정의합니다.  

단일 장애에서 복합 장애, 관측 불완전성, 롤백 실험, 논문용 반복 실험까지 단계적으로 진화하는 실험 설계를 명세합니다.

---

## 1. 장애 주입 연구 환경

```
장애 주입 대상:
    결제 시스템 앱 (연구용으로 구축된 실제 결제 시스템 모방 앱)

모니터링 주체:
    SRE 플랫폼 (Prometheus / Loki / Tempo 기반 상시 수집 및 판단)

장애 주입 도구:
    stress-ng          → CPU / Memory / I/O 부하 주입
    chaos-mesh         → Kubernetes 레벨 장애 주입
    tc (traffic control) → 네트워크 지연 / 패킷 손실
    kill / SIGKILL     → 프로세스 강제 종료
    직접 설정 변경     → ThreadPool 축소, MaxMetaspaceSize 제한 등

Git 저장소 접근 원칙:
    SRE 플랫폼은 Git 저장소를 읽기 전용으로만 접근
    실험 결과 문서는 사용자가 직접 반영
```

---

## 2. 단일 장애가 아닌 복합 장애가 논문 가치를 결정하는 이유

```
단순 기록:
    JVM CPU Saturation 발생 → 대응 완료

연구 가치: 낮음

---

복합 장애 기록:
    JVM CPU Saturation
    + Redis Latency 증가
    + Kafka Consumer Lag
    + API Timeout 증가
    + Payment Confirmation Delay
    + Partial Trace Missing

연구 가치: 높음

이유:
    복합 장애 상황에서 단일 알람 기반 대응보다
    Evidence Reliability + Scenario Matching + Guardrail 기반
    SRE 플랫폼이 더 정확하고 안정적인 권장 조치를 생성함을 증명 가능
```

---

## 3. Phase별 실험 전략

### Phase 1: 단일 장애 주입

```
목적:
    기초 실험 카탈로그 구축
    각 장애 유형의 단독 감지/대응 성능 측정

대상 예시:
    JVM CPU Saturation
    Redis Latency Spike
    Kafka Consumer Lag
    Container OOM Kill
    Observability Pipeline Delay

실험 수:
    90개 장애 유형 × 1회 이상
    → 최소 90회

측정 항목:
    MTTD (Mean Time to Detect)
    MTTR (Mean Time to Resolve)
    Scenario Match 정확도
    권장 조치 생성 정확도

결과 문서:
    experiments/ 90개 이상
    postmortems/ 90개 이상
    systems-math/ 기초 데이터셋
```

---

### Phase 2: 2계층 복합 장애

```
목적:
    2개 스택에 걸친 복합 장애에서 플랫폼의 Scenario Matching 정확도 측정
    단일 알람 기반 대응과 성능 비교 시작

복합 장애 예시:
    JVM CPU Saturation + DB Connection Timeout
    Redis Degradation + API Timeout
    Kafka Consumer Lag + Worker Thread Saturation
    Container OOM + Retry Storm

측정 항목:
    Scenario Matching 정확도 (복합 vs 단일 비교)
    False Positive Rate
    False Negative Rate
    MTTR 비교 (SRE 플랫폼 vs 수동 대응)
```

---

### Phase 3: 결제 도메인 복합 장애

```
목적:
    결제 무결성과 연결된 복합 장애에서 플랫폼의 안전성 측정

복합 장애 예시:
    Payment Timeout + Duplicate Transaction Risk
    Payment Confirmation Delay + Redis Inconsistency
    Idempotency Failure + Retry Storm
    Partial Payment Completion + Rollback Failure

측정 항목:
    결제 무결성 유지율
    권장 조치 안전성 (Guardrail 작동 여부)
    Rollback 성공률
    Human Approval 프로세스 정확성
```

---

### Phase 4: 관측 불완전성 실험

```
목적:
    Evidence가 불완전한 상황에서 플랫폼이 안전하게 판단 유보 / 심층 진단 권고를 하는지 측정

시나리오 예시:
    Metrics 있음 / Logs 없음
    Logs 있음 / Traces 없음
    Payment Consistency Evidence Missing
    Partial Metrics (일부 Prometheus Scrape 실패)
    Trace Sampling Loss (Tempo 트레이스 누락)

측정 항목:
    Evidence Reliability 점수 분포
    판단 유보율 (Low Evidence → Defer 선택)
    심층 진단 권고 정확성
    관측 불완전성 상황에서의 False Positive Rate
```

---

### Phase 5: 롤백 안정성 실험

```
목적:
    롤백 시나리오에서 플랫폼의 Rollback 안전성 측정
    논문에서 "롤백 후 재발" / "롤백 실패" 시나리오 데이터 확보

시나리오 예시:
    Rollback Successful + Verification Failed
    Rollback Successful + Regression Detected
    Rollback Failed + Service Partially Recovered
    Rollback After Rollback (재롤백 필요 상황)

측정 항목:
    Rollback 성공률
    Rollback 후 Verification 통과율
    재악화율 (Rollback 후 장애 재발)
    평균 Rollback 소요 시간
```

---

### Phase 6: 논문용 반복 실험

```
목적:
    동일 장애 유형을 30회 이상 반복하여 통계적 유의성 확보
    SRE 플랫폼 vs Baseline(수동 대응) 정량 비교

반복 실험 대상:
    가장 대표적인 복합 장애 유형 5~10개 선정

반복 횟수:
    동일 조건 × 30회 이상

비교 지표:
    MTTD: SRE 플랫폼 vs 수동 대응
    MTTR: SRE 플랫폼 vs 수동 대응
    False Positive Rate
    Rollback Success Rate
    Verification Success Rate
    P99 Latency 회복 시간
    결제 무결성 유지율

결과 저장:
    systems-math/ → 논문 결과 섹션 데이터셋
    experiments/ → 반복 실험 상세 기록
```

---

## 4. 논문 가치를 높이는 실험 설계 원칙

### 4-1. 재현 가능성 (Reproducibility)

```
모든 장애 주입 실험은 재현 가능해야 한다.
    → 장애 주입 명령 전체 기록
    → 환경 설정 (Kubernetes 버전, JDK 버전, 앱 버전) 명세
    → 동일 조건 반복 시 동일 결과 재현 가능 여부 검증
```

### 4-2. 비교 가능성 (Comparability)

```
SRE 플랫폼 대응 결과와 수동 대응 결과를 비교 가능한 형태로 기록한다.
    → Baseline (수동 대응): 동일 장애 × 일정 횟수 수동 대응 기록
    → SRE Platform: 동일 장애 × 동일 횟수 플랫폼 대응 기록
    → 비교 지표: MTTD, MTTR, 정확도, 안전성
```

### 4-3. 정량화 가능성 (Quantifiability)

```
모든 실험 결과는 수치로 기록된다.
    CPU Utilization: 97%
    P99 Latency: 3.1s
    Error Rate: 12.4%
    MTTD: 35s
    MTTR: 17min
    Rollback Success Rate: 94%
```

### 4-4. 복합성 (Complexity Escalation)

```
단일 → 2계층 복합 → 결제 도메인 복합 → 관측 불완전성 → 롤백 → 반복
으로 실험이 점진적으로 복잡해지도록 설계한다.
```

---

## 5. 논문 증명 목표와 Phase 매핑

| 논문 증명 목표 | 관련 Phase |
|----------------|-----------|
| 단일 장애 감지 정확도 | Phase 1 |
| 복합 장애 Scenario Matching 정확도 | Phase 2~3 |
| 결제 무결성 보장 능력 | Phase 3 |
| 관측 불완전 상황에서의 안전한 판단 유보 | Phase 4 |
| Rollback 안정성 | Phase 5 |
| 통계적 유의성 확보 (SRE 플랫폼 vs 수동 대응) | Phase 6 |