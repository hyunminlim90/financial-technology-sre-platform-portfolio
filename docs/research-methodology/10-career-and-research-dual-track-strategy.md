# 10. Career and Research Dual-Track Strategy

> **목적**: FIN-SRE Platform 프로젝트에서 정의한 9개 핵심 기술 축에 대하여
> 산업(취업) 관점과 연구(석사/박사) 관점을 구분하고,
> 장기적인 기술 성장 및 논문화 방향을 정의한다.

---

## 1. 9개 핵심 기술 축

본 프로젝트는 다음 9개 기술 축을 연구 대상으로 정의한다.

```
Stack 01: JVM Runtime
Stack 02: Distributed Cache
Stack 03: Distributed Messaging
Stack 04: Container Orchestration
Stack 05: Observability
Stack 06: LLM
Stack 07: RAG
Stack 08: AI-Agent
Stack 09: Guardrail
```

각 축은 독립적인 학습 대상인 동시에, FIN-SRE Platform 내에서 상호 연결된 구조로 동작한다.

---

## 2. 취업 관점: 핵심 축

핀테크 SRE 및 Reliability Engineering 포지션에서 가장 중요한 영역은 다음 5개 축이다.

```
JVM Runtime
Distributed Cache
Distributed Messaging
Container Orchestration
Observability
```

**대표 기술 스택**

```
JVM (GC, ThreadModel, ClassLoader, Metaspace)
Redis (Cluster, Replication, Eviction, Latency)
Kafka (Consumer Group, Lag, Partition, Rebalance)
Kubernetes (Scheduler, CGroup, OOM, Resource Limit)
Prometheus / Loki / Tempo (메트릭 / 로그 / 트레이스)
```

**산업 현장의 주요 요구사항**

```
장애 대응 및 사후 회고
운영 자동화
성능 분석 및 병목 진단
분산 시스템 운영
모니터링 및 관측성 확보
장애 복구 및 검증
```

> LLM / RAG는 현재 대부분의 SRE 채용공고 기준에서 **가산점** 수준이다.
> 취업 준비의 중심은 위 5개 축이다.

---

## 3. 연구 관점: 추가 핵심 축

연구 관점에서는 다음 4개 축이 추가된다.

```
LLM
RAG
AI-Agent
Guardrail
```

이 영역은 일반적인 SRE 운영 범위를 넘어서는 **연구 주제**를 제공한다.

**대표 연구 대상**

```
Evidence 기반 장애 판단
Assessment Reliability
Recommendation 생성 신뢰성
Human-in-the-Loop 구조
Agent Safety
Decision Reliability
Operational Reliability
```

---

## 4. FIN-SRE Platform 내 역할 분리

플랫폼의 기본 흐름은 다음과 같다.

```
Evidence 생성
    ↓
Assessment
    ↓
Recommendation
    ↓
Human Approval
    ↓
Verification
```

**계층별 기술 축 매핑**

```
[Evidence 생성 계층]
    JVM Runtime
    Distributed Cache (Redis)
    Distributed Messaging (Kafka)
    Container Orchestration (Kubernetes)
    Observability (Prometheus / Loki / Tempo)

        ↓ 수집된 Evidence 전달

[Assessment / Recommendation 계층]
    LLM       → 자연어 기반 장애 판단 및 권장 조치 생성
    RAG       → Scenario / Runbook / Postmortem 문서 탐색 및 참조
    AI-Agent  → Lifecycle 내 단계별 자율 판단 보조
    Guardrail → 안전하지 않은 권장 조치 차단 및 Human Approval 강제
```

이 구조가 취업 핵심 5개 축과 연구 핵심 4개 축이 분리되지 않고 하나의 플랫폼으로 통합되는 이유다.

---

## 5. LLM / RAG의 위치와 연구 범위

본 프로젝트에서 LLM과 RAG는 **연구 도구**이지, **연구의 최종 목적이 아니다.**

**연구 범위에서 제외**

```
LLM 자체 성능 연구          X
Foundation Model 구조 연구   X
모델 파인튜닝 연구           X
```

**연구 범위에 포함**

```
Evidence Reliability        O  (수집된 증거가 얼마나 신뢰 가능한가)
Assessment Reliability      O  (평가 결과가 얼마나 정확한가)
Recommendation Reliability  O  (권장 조치가 얼마나 안전하고 효과적인가)
Decision Reliability        O  (Human Approval 과정의 판단 품질)
Operational Reliability     O  (전체 플랫폼이 서비스 안정성에 기여하는 정도)
```

**핵심 구분**

```
LLM 연구
    ≠
LLM을 활용한 운영 신뢰성 연구
```

본 프로젝트의 연구 방향은 후자다.

---

## 6. 산업 경험과 연구의 선순환 구조

산업 현장에서 얻는 실무 경험은 연구 질문을 생성한다.

```
산업 현장 경험
    └── 복합 장애 상황
    └── 오탐지 (False Positive)
    └── Alert Fatigue
    └── 운영자 의사결정 오류
    └── 장애 판단 불확실성
        ↓
    연구 질문 생성
        ↓
    FIN-SRE Platform 실험 재현
        ↓
    논문화
        ↓
    산업 적용
        ↓
    새로운 산업 경험 → 새로운 연구 질문 (선순환)
```

따라서 산업과 연구를 분리하지 않고,
**산업에서 문제를 발견하고 → 플랫폼에서 재현하며 → 논문으로 검증하는 선순환 구조**를 구축하는 것이 장기 전략의 핵심이다.

---

## 7. 취업 전략과 연구 전략의 현실적 분리

### 취업 전략

```
목표 포지션:
    핀테크 SRE / Platform Engineer / Reliability Engineer

핵심 준비 영역:
    JVM + Redis + Kafka + Kubernetes + Observability

LLM / RAG 포지셔닝:
    가산점 (SRE 포지션 기준)
    핵심 경쟁력 (AI 서비스 기업 / AI Agent 기업 기준)

취업 후 연구 연결:
    회사에서 LLM / RAG를 사용한다면 연구와 자연스럽게 연결
    그렇지 않더라도 FIN-SRE Platform 자체가 연구 환경을 제공
```

### 연구 전략

```
핵심 연구 주제:
    Evidence Reliability 기반
    Human-in-the-Loop Operational Reliability

사용 기술 전체:
    JVM + Redis + Kafka + Kubernetes + Observability (Evidence 생성)
    LLM + RAG + AI-Agent + Guardrail (Assessment / Recommendation)

연구 차별점:
    9개 기술 축을 하나의 운영 신뢰성 플랫폼으로 통합하여
    복합 장애 상황에서의 판단 신뢰성을 정량 검증
```

---

## 8. 장기 연구 로드맵

### 현재 연구 단계

```
Evidence Governance
Evidence Lineage
Evidence Trust
Evidence Confidence
Evidence Reliability
```

### 향후 연구 단계

```
Assessment Reliability       ← LLM 기반 평가의 신뢰도 정량화
Decision Reliability         ← Human Approval 품질 측정
Recommendation Reliability   ← 권장 조치 성공률 예측 모델
Verification Reliability     ← 검증 프로세스 안정성 측정
Operational Safety Reliability ← 전체 플랫폼 안전성 통합 모델
```

### 논문 주제 발전 방향

```
석사:
    Evidence Reliability Framework를 활용한
    Human-in-the-Loop SRE 플랫폼의 복합 장애 대응 신뢰성 검증

박사:
    Evidence Reliability가 LLM Recommendation 품질에
    미치는 영향에 대한 정량적 분석
    (관측 불완전성 조건에서의 안전한 판단 유보 모델 포함)

산업 적용:
    Evidence-Based Operational Reliability Platform의
    실제 금융 시스템 적용 및 효과 검증
```

---

## 9. 최종 목표

```
SRE 취업
    ↓
산업 경험 축적 (복합 장애, 운영 자동화, 분산 시스템)
    ↓
FIN-SRE Platform 실험 (장애 주입 → 재현 → 데이터 축적)
    ↓
석사 연구 (Evidence Reliability 정량 검증)
    ↓
박사 연구 (Operational Reliability 확장 연구)
    ↓
지속적인 Operational Reliability 연구 및 산업 적용
```

**핵심 원칙**

```
취업 = JVM + Redis + Kafka + Kubernetes + Observability 중심
연구 = 위 5개 축 + LLM + RAG + AI-Agent + Guardrail 통합

둘은 분리된 것이 아니라,
Evidence 생성 계층(취업 핵심 5개 축)과
Assessment/Recommendation 계층(연구 핵심 4개 축)으로
하나의 플랫폼 안에서 연결된다.
```