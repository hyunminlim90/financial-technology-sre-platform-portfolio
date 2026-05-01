# Postmortem Generation Flow

> Human-in-the-loop 기반  
> AI Postmortem 생성 및 학습 워크플로우

---

## 1. 목적

이 문서는 Incident 종료 후  
AI가 Postmortem을 생성하고,  
Human 검증을 통해 RAG Knowledge로 축적되는 과정을 정의한다.

> Postmortem은 기록이 아니라  
> **AI를 학습시키는 데이터다.**

---

## 2. 핵심 원칙

> AI는 Draft를 생성한다  
> Human이 검증한다  
> **검증된 것만 학습된다**

---

## 3. 전체 흐름

```
[Incident 종료]
        ↓
[데이터 수집]
        ↓
[AI 분석]
        ↓
[Postmortem Draft 생성]
        ↓
[파일명 추천]
        ↓
[Human 검증 / 수정]
        ↓
[Root Cause 확정]
        ↓
[Git Commit]
        ↓
[RAG Knowledge 반영]
```

---

## 4. Step-by-Step 상세 흐름

### 4.1 Incident 종료 조건

- Alert 해소
- 서비스 정상화
- 추가 대응 필요 없음

### 4.2 Data Collection

AI는 다음 데이터를 수집한다:

- Alert timeline
- Metrics (Prometheus)
- Logs (Loki)
- Traces (Jaeger)
- 수행된 Action 기록
- 이전 Recommendation 로그

### 4.3 Timeline Reconstruction

AI는 장애 흐름을 재구성한다.

**예:**

```
10:01 Redis latency 증가
10:02 API latency 증가
10:03 retry 증가
10:05 DB connection saturation
10:07 오류 증가
```

### 4.4 Root Cause Analysis (중요)

> AI는 Root Cause를 **"확정하지 않는다."**

**수행:**
- 원인 후보 제시
- causal chain 분석
- propagation 기반 설명

### 4.5 Action Evaluation

- 어떤 Action이 수행되었는가
- 효과는 어땠는가
- 문제를 악화시켰는가

### 4.6 Improvement Candidate Extraction

AI는 개선 후보를 도출한다.

**예:**

```
retry 증가 상태에서 scale-out 수행됨
→ 잘못된 대응
→ Improvement 필요
```

### 4.7 Preventive Design Candidate Extraction

```
구조적 문제 발견 시:
→ Preventive Design 제안
```

---

## 5. AI Output Structure (Draft)

AI는 반드시 다음 구조로 Postmortem을 생성한다.

| 섹션 | 내용 |
|------|------|
| **5.1 Overview** | 장애 요약, 영향 범위, Severity |
| **5.2 Impact** | 사용자 영향, 시스템 영향, 결제 영향 |
| **5.3 Timeline** | 시간 순서 기반 장애 흐름 |
| **5.4 Symptoms** | Metrics, Logs, Traces |
| **5.5 Root Cause** | 원인 후보 (확정 아님) |
| **5.6 What Went Well** | 효과적인 대응 |
| **5.7 What Went Wrong** | 잘못된 대응, 판단 오류 |
| **5.8 Action Items** | Improvement 후보, Preventive Design 후보 |
| **5.9 Lessons Learned** | SRE 관점 핵심 통찰 |

---

## 6. File Naming Rule (중요)

AI는 파일명을 추천한다.

```
postmortems/<domain>-<failure-mode>-<date>.md
```

**예:**

```
postmortems/redis-timeout-2026-05-01.md
```

---

## 7. Human Validation (핵심)

Human은 반드시 다음을 수행해야 한다:

- ✔ Root Cause 확정
- ✔ 잘못된 분석 수정
- ✔ 불필요한 내용 제거
- ✔ Action Item 검증
- ✔ 최종 승인

---

## 8. Knowledge Integration

검증된 Postmortem만 RAG에 반영된다.

### 8.1 영향

- AI 판단 정확도 증가
- Improvement 생성 근거 제공
- Preventive Design 트리거

### 8.2 연결 구조

```
postmortems/
→ scenarios/
→ runbooks/
→ improvements/
→ preventive-designs/
```

---

## 9. Learning Loop (핵심)

```
Incident
→ Postmortem
→ Improvement
→ Preventive Design
→ RAG 업데이트
→ 다음 Incident 개선
```

---

## 10. Safety Rule

> 검증되지 않은 Postmortem은 **절대 학습 금지**

---

## 11. Anti-Pattern

- ❌ AI가 Root Cause 확정
- ❌ 검증 없이 commit
- ❌ 감정 기반 서술
- ❌ 재현 불가능한 분석

---

## 12. 핵심 원칙

| 구분 | 원칙 |
|------|------|
| Runbook | 바꾸지 않는다 |
| Postmortem | 계속 쌓는다 |
| AI | 점점 더 정확해진다 |

---

## 중요한 한 줄

> **Garbage Postmortem → Garbage AI 판단**