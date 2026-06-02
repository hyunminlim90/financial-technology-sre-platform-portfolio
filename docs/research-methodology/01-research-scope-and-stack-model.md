# 01. Research Scope and Stack Model

> 정독: 1회

**목적**: FIN-SRE 플랫폼 연구의 대상 범위를 9개 핵심 스택으로 한정하고, 스택별 장애 유형 카탈로그 전략을 정의합니다.

## 1. 연구 범위 결정 배경

초기 연구 후보군에는 Java, ArgoCD, Jenkins, Terraform 등 광범위한 스택이 포함되어 있었다.  

그러나 아래의 이유로 최종 연구 대상을 **9개 핵심 스택**으로 축소·확정하였다.

- SRE 실무에서 실제로 반복 발생하는 장애 유형에 집중
- 현재 구축 중인 SRE 플랫폼이 실제 사용하는 스택과의 일치
- 논문 실험의 재현성과 범위 통제를 위한 카탈로그 관리 필요성

## 2. 9개 핵심 스택 확정

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

각 스택은 독립적인 장애 유형 카탈로그를 보유하며, 스택 간 복합 장애 실험의 기반 단위가 된다.

## 3. 장애 유형 카탈로그 전략

### 3-1. 카탈로그 규모

| 구분 | 수량 |
|------|------|
| 핵심 스택 수 | 9개 |
| 스택별 대표 장애 유형 | 10개 내외 |
| **총 장애 유형 (목표)** | **90개 내외** |

### 3-2. 장애 유형 문서 생성 시점

```
현재 단계:
    Knowledge Document Specification v1 작성 중
    Scenario Matcher / Runbook Resolver 코드 경계 미확정

→ 장애 유형 프롬프트 제공 시점:
    Scenario Matcher / Runbook Resolver 코드 경계가 확정된 이후

→ 진행 방식:
    사용자가 9개 스택별 장애 유형 목록을 제공
    → 7종 문서 초안 생성 (scenario, runbook, improvement, postmortem,
                           preventive-design, experiment, systems-math)
    → 사용자가 직접 Git 저장소에 반영
```

### 3-3. 장애 유형 카탈로그의 역할

```
90개 장애 유형 카탈로그
=
실험 카탈로그 (Experiment Catalog)

≠

논문 데이터

논문 데이터는:
    experiments/
    systems-math/
    postmortems/
    에서 나온다
```

## 4. rag/docs와 운영 문서의 역할 구분

연구 자산은 **학습 자산**과 **운영 자산**으로 명확히 분리된다.

### 학습 자산 (rag/docs)

```
위치: portfolio/rag/docs/*
역할: SRE 플랫폼의 이론적 기반 학습
내용 범위:
    Bit → CPU → Kernel → OS
    → JVM Runtime
    → Distributed Cache
    → Distributed Messaging
    → Container Orchestration
    → Observability
    → LLM / RAG / AI-Agent / Guardrail
    (각 스택의 내부 메커니즘 전체)
판단 로직 영향도: 낮음 (최후순위 참조)
```

### 운영 자산 (7종 문서)

```
위치: portfolio/scenarios, runbooks, experiments, ...
역할: SRE 플랫폼의 실제 판단 근거
판단 로직 영향도: 높음 (최우선 참조)

참조 우선순위 (정의된 프로토콜 기준):
    1순위: runbooks
    2순위: postmortems
    3순위: preventive-designs
    ...
    최후순위: rag/docs
```

## 5. 최종 연구 목표와의 연결

```
9개 스택 × 10개 장애 유형
    ↓
90개 Scenario (실험 카탈로그)
    ↓
수백 회 장애 주입 실험
    ↓
실험 데이터 (experiments / systems-math) 축적
    ↓
석사 논문 → 박사 논문
    ↓
산업 적용 가능한 SRE 플랫폼 연구
```

논문의 최종 증명 명제:

> Evidence Reliability + Scenario Matching + Guardrail 기반 SRE 플랫폼이
> 복합 장애 상황에서 단일 알람 기반 대응보다 더 안정적이고 신뢰 가능한
> 권장 조치를 생성하며, 서비스 안정성 99P를 보장한다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*