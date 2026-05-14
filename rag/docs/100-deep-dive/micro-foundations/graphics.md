# Graphics (그래픽)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**Graphics(그래픽)** 는 디지털 데이터를 화면의 픽셀(Pixel)로 변환하기 위한 **대규모 수학 연산 체계**이다.

### 핵심 연산

- **좌표 계산**
- **색상 계산**
- **벡터 / 행렬 연산**
- **병렬 픽셀 처리**

### 현대 시스템에서의 진화

```
Graphics Processing
 → Massive Parallel Computation
```

> **GPU의 본질은 그래픽 장치라기보다 대규모 병렬 연산 엔진에 가깝다.**

---

## 2. 시스템 어디에서 등장하는가

### 전통적 그래픽 영역

- 게임 렌더링
- 3D 모델링
- 영상 처리
- UI Rendering

### FinTech / Infra 영역으로의 확장

그래픽 기술의 핵심인 **병렬 행렬 연산**이 다음 영역으로 확장됨.

#### AI / FDS

```
Fraud Detection
 → Tensor 연산
 → GPU 병렬 처리
```

#### 암호화 / 수치 계산

```
대규모 해시 계산  /  통계 모델링  /  Risk Scoring
```

#### 데이터 분석

```
RAPIDS  /  CUDA Analytics  /  Spark GPU Acceleration
```

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 영향이 큰 자원: **Memory Bandwidth + Compute Throughput**

| 자원 | 영향 |
|------|------|
| CPU | GPU Offload |
| Memory | 매우 큼 — 대역폭이 성능 핵심 |
| PCIe | CPU ↔ GPU Data Transfer 병목 |
| Power / Thermal | 극도로 큼 |

> **GPU 시스템에서 메모리 대역폭이 성능의 핵심 변수**

---

## 4. 왜 중요한가

Graphics 기술은 **대규모 병렬 계산 구조**의 출발점이다.

### 기술 진화 흐름

```
그래픽 기술 발전
 → GPU 발전
 → AI / 병렬 컴퓨팅 발전
```

### 현재 그래픽 연산 구조를 기반으로 동작하는 영역

- **AI / FDS** — Fraud Detection Inference
- **ML Inference** — 실시간 Risk Scoring
- **HPC** — 대규모 수치 계산
- **Quant Analysis** — 금융 모델 병렬 분석

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. GPU Saturation

병렬 연산 Queue 포화 시 발생.

```
연산 Queue 포화
 → Inference Delay
 → Tail Latency 증가
 → Timeout
```

---

### 5-2. PCIe Transfer Bottleneck

GPU 자체 연산보다 **CPU ↔ GPU 데이터 이동**이 병목이 되는 경우.

```
CPU Memory  ↔  PCIe Bus  ↔  GPU Memory
 → Transfer 비용 과다
 → 전체 Throughput 감소
```

---

### 5-3. Thermal Throttling

그래픽 / AI 연산 시 발열 급증으로 **GPU Clock 자동 하락**.

```
전력 급증
 → 발열 증가
 → GPU Clock 하락
 → Inference 지연
 → P99 증가
```

---

### 5-4. VRAM OOM

GPU 메모리 부족 시 발생.

```
VRAM 포화
 → CUDA OOM
 → Model Load 실패
 → Process Crash
```

---

### 5-5. NUMA Mismatch

GPU가 연결된 CPU Socket과 Application Thread 위치가 다를 때 발생.

```
GPU (Socket 0)  →  Application Thread (Socket 1)
 → Remote Memory Access 증가
 → PCIe Transfer Latency 증가
```

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Pixel Rendering

그래픽의 본질은 **픽셀 색상 계산** 문제.

| 해상도 | 동시 계산 픽셀 수 |
|--------|-----------------|
| 1080p | 약 200만 픽셀 |
| 4K | 약 800만 픽셀 |
| 8K | 약 3,300만 픽셀 |

---

### Matrix / Vector Computation

GPU 핵심 연산은 **행렬(Matrix) + 벡터(Vector)** 연산. AI 모델 추론도 동일한 구조를 사용.

```
Pixel 색상 계산  =  행렬 곱 연산
AI Inference     =  행렬 곱 연산
```

---

### SIMD vs SIMT

| 구조 | 풀네임 | 설명 | 사용처 |
|------|--------|------|--------|
| SIMD | Single Instruction Multiple Data | CPU 벡터 연산 | CPU AVX / SSE |
| SIMT | Single Instruction Multiple Threads | GPU 병렬 Thread 실행 | CUDA / GPU |

---

### Massive Parallelism

GPU는 **수천 개 코어** 기반으로 동작. 복잡한 제어보다 **동일 연산 반복**에 최적화.

```
단순 연산 × 수천 Thread 동시 실행  →  그래픽 / AI 모두 동일 구조
```

---

### Frame Buffer

렌더링 결과를 저장하는 메모리 영역. GPU Memory 대역폭이 Frame Buffer 처리 속도를 결정.

---

### CUDA / Compute Kernel

현대 GPU는 그래픽을 넘어 **General Compute** 영역으로 확장됨.

```
Graphics 연산
 → CUDA Compute Kernel
 → AI Inference / 암호화 / 분석
```

---

### PCIe Interconnect

GPU는 CPU와 **PCIe Bus**를 통해 연결됨. Data Transfer 비용이 전체 성능의 핵심 변수.

```
CPU  ↔  PCIe Bus  ↔  GPU  (Transfer 비용 최소화가 핵심)
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### GPU 상태 확인

```bash
nvidia-smi
```

### GPU 사용률 모니터링

```bash
nvidia-smi dmon
```

### PCIe 연결 상태 확인

```bash
lspci
```

### GPU Process 확인

```bash
nvidia-smi pmon
```

### NUMA 구조 확인

```bash
numactl --hardware
```

### GPU Device 확인

```bash
ls /dev/nvidia*
```

### Kubernetes GPU Node 확인

```bash
kubectl describe node
```

### CUDA Runtime 확인

```bash
nvcc --version
```

---

## 요약

```
Graphics
 ├── Pixel Rendering         → 픽셀 색상 계산 = 행렬 연산의 시작
 ├── Matrix / Vector         → GPU와 AI 모두 동일한 연산 구조
 ├── SIMD / SIMT             → CPU 벡터 연산 vs GPU 병렬 Thread 실행
 ├── Massive Parallelism     → 수천 코어 동시 동작
 ├── Frame Buffer            → GPU Memory 대역폭이 처리 속도 결정
 ├── CUDA / Compute Kernel   → Graphics → General Compute 확장
 └── PCIe Interconnect       → CPU ↔ GPU Transfer 비용 핵심 병목
```

> FinTech 결제 시스템에서 Graphics는 화면을 그리는 기술이 아니라,  
> **Fraud Detection · Risk Scoring · 암호화 가속을 가능하게 하는 병렬 연산 패러다임의 기원**이다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*