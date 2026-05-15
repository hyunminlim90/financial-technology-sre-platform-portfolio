# ALU (Arithmetic Logic Unit, 산술 논리 장치)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**ALU(Arithmetic Logic Unit)** 는 CPU 내부에서 실제 산술 계산과 논리 판단을 수행하는 **실행 회로**이다.

```
CPU가 "무엇을 할지" 결정한다면,
ALU는 "실제로 계산을 수행하는 하드웨어 실행 엔진"이다.
```

### 수행하는 대표 연산

| 연산 종류 | 예시 |
|-----------|------|
| 산술 연산 | 덧셈 / 뺄셈 / 곱셈 / 나눗셈 |
| 논리 연산 | AND / OR / XOR / NOT |
| 비교 연산 | == / < / >= |

---

## 2. 시스템 어디에서 등장하는가

ALU는 사실상 **모든 컴퓨팅 경로**에 등장한다.

### JVM

```
객체 비교  /  Hash 계산  /  if 조건  /  반복문
 → 모두 ALU 기반 실행
```

### 결제 시스템

```
잔액 계산  /  수수료 계산  /  승인 여부 판단  /  한도 비교
 → 모두 ALU 산술 / 논리 연산
```

### 암호화 처리

```
SHA  /  AES  /  Checksum
 → 대량 비트 연산 → ALU / FPU 기반
```

### DB 엔진

```
Aggregation  /  Sorting  /  Join Compare
 → 대량 비교 / 산술 연산
```

### GPU / FDS

```
GPU 내부에도 대량의 ALU 존재
 → 행렬 연산 수행 → Fraud Detection Inference
```

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향: **CPU**

ALU는 CPU Core 내부 실행 유닛이기 때문. 하지만 **Memory 영향도 매우 크다**.

| 자원 | 영향 |
|------|------|
| CPU | ALU 직접 실행 주체 |
| Cache | Hit / Miss → ALU 대기 여부 결정 |
| Memory | Stall 발생 시 ALU가 유휴 상태 |
| NUMA | Remote Access → ALU Pipeline Stall |

> **ALU 성능은 Memory 공급 속도에 의존한다.**  
> Cache Miss / NUMA Remote Access 발생 시 ALU가 데이터를 기다리며 유휴 상태가 된다.

---

## 4. 왜 중요한가

결제 시스템의 모든 비즈니스 로직은 결국 **ALU 연산의 집합**이기 때문이다.

| 비즈니스 로직 | 실제 내부 동작 |
|---------------|----------------|
| 잔액 계산 | 산술 연산 |
| 승인 여부 판단 | 비교 / 논리 연산 |
| Retry 판단 | 논리 연산 |
| 암호화 | 비트 연산 |
| 정산 집계 | 대량 산술 연산 |

> **결제 시스템 전체 = ALU 연산의 연속적 조합**

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. CPU Saturation

대량 계산 발생 시 ALU 가동률 급증.

```
대량 연산 증가
 → ALU Busy 증가
 → CPU 사용률 증가
 → Run Queue 증가
 → 응답 지연
```

---

### 5-2. Pipeline Stall

ALU가 사용할 데이터가 아직 메모리에서 도착하지 않은 경우 발생.

```
Cache Miss / NUMA Access / Memory Latency
 → 데이터 미도착
 → CPU Pipeline Stall
 → ALU 유휴 대기
 → IPC 감소
```

---

### 5-3. Branch Misprediction

논리 조건이 복잡하거나 불규칙할 때 발생.

```
if / else 분기 예측 실패
 → Pipeline Flush
 → 실행 취소 및 재시작
 → IPC 감소
 → Tail Latency 증가
```

> 조건 패턴이 불규칙할수록 Misprediction 증가

---

### 5-4. Integer Overflow

금액 계산에서 정수 범위 초과 시 발생.

```
잔액 누적 계산
 → Integer 범위 초과
 → 값 역전 / 음수 전환
 → 잔액 계산 오류
 → 정산 오류
```

> **FinTech에서 Integer Overflow는 금융 사고로 직결될 수 있음**

---

### 5-5. Thermal Throttling

ALU 연산 과다 시 CPU 발열 증가.

```
ALU 연산 과다
 → CPU 발열 증가
 → Thermal Throttling
 → Clock 하락
 → TPS 감소 / Latency 증가
```

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Register

ALU가 직접 읽는 초고속 저장 공간. Register 접근이 가장 빠르다.

```
Register  (< 1ns)
 → L1 Cache  (~1ns)
 → L2 Cache  (~5ns)
 → L3 Cache  (~20ns)
 → RAM       (~100ns)
```

---

### Pipeline

명령어를 여러 단계로 나누어 병렬 처리. ALU는 Pipeline 내부의 **Execute 단계**를 담당.

```
Fetch → Decode → [Execute / ALU] → Memory → Write-back
                       ↑
               ALU가 실제 연산 수행
```

---

### Branch Prediction

논리 연산 결과를 CPU가 미리 예측하여 Pipeline을 유지. 실패 시 성능 급락.

```
if (잔액 >= 결제금액)
 → CPU 예측 후 선제 실행
 → 성공: 그대로 진행
 → 실패: Pipeline Flush → 재실행 비용 발생
```

---

### Cache Hierarchy

ALU 성능의 핵심 공급 구조. Cache Miss 발생 시 ALU가 데이터를 기다리며 유휴 상태.

| 레벨 | 속도 | 용량 | 위치 |
|------|------|------|------|
| L1 Cache | 최고속 | 수십 KB | Core 전용 |
| L2 Cache | 고속 | 수백 KB ~ 수 MB | Core 전용 |
| L3 Cache | 중속 | 수십 MB | Socket 공유 |
| RAM | 저속 | 수백 GB | NUMA Node |

---

### FPU (Floating Point Unit)

소수점 계산 전용 연산 유닛. ALU와 별도로 존재하며 금융 계산에서 중요.

```
환율 계산  /  이자 계산  /  Risk Score 계산
 → 모두 FPU 기반
```

---

### SIMD / Vector Unit

동일 ALU 연산을 여러 데이터에 동시에 수행.

| 명령어 셋 | 처리 폭 | 주요 용도 |
|-----------|---------|-----------|
| AVX | 256 bit | 부동소수점 벡터 연산 |
| AVX2 | 256 bit | 정수 포함 확장 |
| AVX-512 | 512 bit | AI / HPC 대규모 연산 |

---

### IPC (Instructions Per Cycle)

클럭당 ALU가 얼마나 효율적으로 동작하는지를 나타내는 핵심 지표.

```
성능 = Clock Frequency × IPC
             ↑
  주파수가 같아도 IPC가 낮으면 처리 효율 저하
```

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
| IPC | cycles 대비 instructions 비율 |
| branch-misses | 분기 예측 실패 수 |

> **IPC가 낮고 branch-misses가 높으면** → 조건 분기 로직 최적화 필요

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

### FlameGraph — 연산 집중 함수 시각화

```
async-profiler 결과
 → FlameGraph 렌더링
 → ALU 연산 집중 함수 식별
```

### Kubernetes CPU 상태 확인

```bash
kubectl top pod
```

---

## 요약

```
ALU (Arithmetic Logic Unit)
 ├── 산술 연산          → 덧셈 / 뺄셈 / 곱셈 — 잔액 / 정산 계산
 ├── 논리 연산          → AND / OR / NOT — 승인 판단 / Retry 결정
 ├── 비교 연산          → == / < / >= — 한도 / 잔액 비교
 ├── Pipeline           → Execute 단계 담당 — Stall 시 ALU 유휴
 ├── Branch Prediction  → 분기 예측 실패 → Pipeline Flush → IPC 감소
 ├── FPU                → 소수점 연산 전용 — 환율 / 이자 계산
 ├── SIMD / Vector Unit → AVX / AVX-512 — 단일 명령 × 다중 데이터
 ├── IPC                → 클럭당 ALU 효율 — 주파수만큼 중요한 지표
 └── Integer Overflow   → 금액 범위 초과 → 정산 오류 → 금융 사고
```

> FinTech 결제 시스템에서 ALU는 단순한 계산 회로가 아니라,  
> **잔액 계산 · 승인 판단 · 암호화 · 정산 집계까지 모든 비즈니스 로직이 물리적으로 실행되는 최소 연산 단위**이다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*