# Arithmetic / Logical Operation (산술 / 논리 연산)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**산술 연산(Arithmetic Operation)** 과 **논리 연산(Logical Operation)** 은 CPU / GPU 내부 연산 장치(ALU)가 수행하는 **가장 기본적인 실행 단위**이다.

### 산술 연산 (Arithmetic Operation)

숫자 계산 수행.

| 연산 | 예시 |
|------|------|
| 덧셈 | 잔액 + 입금액 |
| 뺄셈 | 잔액 - 결제액 |
| 곱셈 | 환율 × 금액 |
| 나눗셈 | 총액 ÷ 건수 |

> **대표 목적**: 값 계산

---

### 논리 연산 (Logical Operation)

조건 판별 수행.

| 연산 | 예시 |
|------|------|
| AND | 잔액 충분 AND 한도 이내 |
| OR | 블랙리스트 OR 이상패턴 |
| NOT | NOT 승인 = 거절 |
| 비교 연산 | 잔액 >= 결제금액 |

> **대표 목적**: 조건 판단 및 흐름 제어

---

## 2. 시스템 어디에서 등장하는가

산술 / 논리 연산은 시스템 **전체 계층**에서 등장한다.

### CPU 내부

```
ALU에서 직접 수행  →  모든 연산의 물리적 기반
```

### JVM Runtime

```
조건문  /  반복문  /  객체 비교  /  Hash 계산
 → 모두 논리 / 산술 연산 기반
```

### DB Engine

```
WHERE 조건  /  JOIN 비교  /  Aggregation
 → 대량 논리 / 산술 연산 수행
```

### 암호화 처리

```
SHA  /  AES  /  Checksum
 → 모두 대량 산술 연산 포함
```

### GPU / FDS

```
벡터 연산  /  행렬 곱셈  /  딥러닝 추론
 → 대규모 병렬 산술 연산
```

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 영향이 큰 자원: **CPU**

실제 연산 수행 주체가 CPU / GPU의 ALU이기 때문. 하지만 메모리 영향도 매우 크다.

| 자원 | 영향 |
|------|------|
| CPU | 연산 수행 주체 — ALU 직접 실행 |
| Cache | Hit / Miss — IPC에 직접 영향 |
| Memory | Latency — Cache Miss 시 노출 |
| NUMA | Socket 간 접근 — 추가 지연 발생 |

---

## 4. 왜 중요한가

결제 시스템 전체가 결국 **산술 + 논리 연산의 조합**으로 동작하기 때문이다.

| 시스템 동작 | 연산 유형 |
|-------------|-----------|
| 잔액 계산 | 산술 |
| 승인 여부 판단 | 논리 |
| 암호화 | 산술 |
| FDS 룰 검사 | 논리 |
| TPS 집계 | 산술 |
| Retry 판단 | 논리 |

> **결제 시스템의 모든 판단과 계산 = 산술 + 논리 연산**

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. CPU Saturation

복잡한 계산 증가 시 ALU 사용률 급증.

```
복잡 연산 증가
 → ALU 사용률 증가
 → CPU 사용률 증가
 → Run Queue 증가
 → 응답 지연
```

---

### 5-2. Branch Misprediction

논리 조건이 복잡하거나 예측 불가능할 때 발생.

```
if / else 분기 예측 실패
 → Pipeline Flush
 → IPC 감소
 → Latency 증가
```

> 조건 분기가 많을수록, 패턴이 불규칙할수록 Misprediction 증가

---

### 5-3. Floating Point 연산 폭증

대규모 암호화 / AI 연산 시 발생.

```
대량 FP 연산
 → CPU Vector Unit 포화
 → Thermal 증가
 → Thermal Throttling
 → Clock 감소
```

---

### 5-4. Infinite Loop

논리 조건 오류 시 발생.

```
조건 오류
 → Busy Loop
 → Core 100%
 → Event Loop Stall
 → Thread Starvation
```

---

### 5-5. Integer Overflow

금액 계산에서 정수 범위 초과 시 발생.

```
금액 누적 계산
 → Integer 범위 초과
 → 값 역전 / 오류
 → 잘못된 잔액 계산
```

> **FinTech에서 Integer Overflow는 금융 사고로 직결될 수 있음**

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### ALU (Arithmetic Logic Unit)

실제 연산을 수행하는 핵심 장치. 산술 / 논리 / 비교 연산을 직접 실행.

```
입력 데이터  →  ALU  →  연산 결과
```

---

### Branch Prediction

논리 연산의 조건 분기를 미리 예측하여 Pipeline을 유지.

```
if (잔액 >= 결제금액)  →  CPU가 결과 예측 후 선제 실행
 → 예측 성공: 그대로 진행
 → 예측 실패: Pipeline Flush → 재실행
```

---

### Pipeline

명령어 실행을 여러 단계로 분리하여 겹쳐 처리.

```
Fetch → Decode → Execute → Memory → Write-back
 ↑ 논리 분기 실패 시 전체 Pipeline Flush 발생
```

---

### SIMD / Vectorization

동일 산술 연산을 여러 데이터에 동시에 수행.

| 명령어 셋 | 처리 폭 | 주요 용도 |
|-----------|---------|-----------|
| AVX | 256 bit | 부동소수점 벡터 연산 |
| AVX2 | 256 bit | 정수 포함 확장 |
| AVX-512 | 512 bit | AI / HPC 대규모 연산 |

---

### FPU (Floating Point Unit)

소수점 연산 전용 유닛. 금융 계산 / AI Inference에서 중요.

```
환율 계산  /  이자 계산  /  확률 Score 계산
 → 모두 FPU 기반
```

---

### Register

연산 직전 데이터를 저장하는 CPU 내부 최고속 저장소.

```
Register  (ns 이하)
 → L1 Cache  (~1ns)
 → L2 Cache  (~5ns)
 → L3 Cache  (~20ns)
 → RAM       (~100ns)
```

---

### Cache Hierarchy

산술 / 논리 연산 성능의 핵심 구조. Cache Miss 발생 시 연산 대기 시간 급증.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### CPU 사용률 확인

```bash
top
mpstat -P ALL 1
```

### IPC / Branch Miss 분석

```bash
perf stat
```

확인 항목:

| 지표 | 의미 |
|------|------|
| instructions | 실행된 명령어 수 |
| cycles | 소비된 클럭 수 |
| branches | 분기 명령어 수 |
| branch-misses | 분기 예측 실패 수 |

> **branch-misses / branches 비율이 높으면** → 복잡한 조건 로직 최적화 필요

### CPU Hotspot 분석

```bash
perf top
```

### Java 연산 Hotspot 분석

```bash
jfr
```

또는

```bash
async-profiler
```

### JIT Vectorization 확인

```bash
# JVM 옵션 추가
-XX:+PrintCompilation
```

### Kubernetes Resource 상태 확인

```bash
kubectl top pod
```

---

## 요약

```
Arithmetic / Logical Operation
 ├── ALU                  → 산술 / 논리 / 비교 — 연산의 물리적 실행 주체
 ├── Branch Prediction    → 조건 분기 예측 — 실패 시 Pipeline Flush
 ├── Pipeline             → 명령어 단계 분리 — 분기 실패 시 성능 급락
 ├── SIMD / Vectorization → AVX / AVX2 / AVX-512 — 단일 명령 × 다중 데이터
 ├── FPU                  → 소수점 연산 전용 — 금융 / AI 계산 핵심
 ├── Register             → CPU 최고속 저장소 — 연산 데이터 임시 보관
 └── Integer Overflow     → 금액 계산 범위 초과 → 금융 사고 직결
```

> FinTech 결제 시스템에서 산술 / 논리 연산은 단순한 CPU 동작이 아니라,  
> **잔액 계산 · 승인 판단 · 암호화 · FDS 룰 검사까지 모든 비즈니스 로직의 물리적 최소 실행 단위**이다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*