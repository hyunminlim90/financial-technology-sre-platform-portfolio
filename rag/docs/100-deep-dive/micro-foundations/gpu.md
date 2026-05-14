# GPU (Graphics Processing Unit)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**GPU** 는 대규모 병렬 연산 처리에 특화된 **하드웨어 가속 장치**이다.

원래 목적은 그래픽 렌더링이지만, 현재는 **GPGPU(General-Purpose GPU Computing)** 기술을 통해 다양한 연산에 활용된다.

### 주요 활용 영역

| 영역 | 설명 |
|------|------|
| AI Inference | 모델 추론 가속 |
| 암호화 연산 | TLS / Signature 대량 처리 |
| 데이터 분석 | 대규모 병렬 수치 계산 |
| 병렬 수치 계산 | 정산 / 리스크 분석 |

<details>
<summary>Deep Dive</summary></br>

Parallel Processing(병렬 처리) [[M]](../../100-deep-dive/micro-foundations/parallel-processing.md)
Graphics(그래픽) [[M]](../../100-deep-dive/micro-foundations/graphics.md)

</details></br>



## 2. 시스템 어디에서 등장하는가

FinTech 결제 시스템에서는 다음 영역에서 등장한다.

### FDS (이상금융거래탐지)

```
결제 요청
 → Feature 생성
 → GPU Inference
 → Fraud 판단
```

### AI 기반 리스크 분석

```
대규모 거래 데이터  ↔  GPU 병렬 연산
```

### 암호화 가속

```
대량 TLS / Signature 연산  →  GPU 가속 처리
```

### 데이터 분석 플랫폼

```
Spark / RAPIDS / CUDA 환경에서 대규모 분석 수행
```

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 큰 영향: **Memory Bandwidth + PCIe I/O**

| 자원 | 영향 |
|------|------|
| CPU | 연산 Offload |
| Memory | HBM / GDDR 고대역폭 사용 |
| PCIe | CPU ↔ GPU Data Transfer |
| Power / Thermal | 소비 전력 및 발열 매우 큼 |

> **핵심 병목**: CPU ↔ GPU Memory Copy 비용

---

## 4. 왜 중요한가

GPU는 CPU가 처리하기 어려운 **대규모 병렬 연산**을 매우 빠르게 수행한다.

### FinTech 환경에서의 주요 역할

- **실시간 Fraud Detection** — 결제 승인 시간에 직접 영향
- **AI Risk Scoring** — 거래 위험도 실시간 판단
- **대규모 정산 분석** — 배치 병렬 처리
- **암호화 처리** — TLS / Signature 대량 오프로드

> **낮은 Inference Latency = 결제 승인 시간 단축**

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. GPU Memory OOM

GPU VRAM 부족 시 발생.

```
VRAM 포화
 → CUDA OOM
 → Inference 실패
 → 모델 로딩 실패
```

---

### 5-2. PCIe Bottleneck

GPU 연산 자체는 빠르지만 **CPU ↔ GPU 데이터 복사**가 병목이 될 수 있음.

```
CPU ↔ PCIe Bus ↔ GPU  (Data Copy 비용 과다)
 → Latency 증가
 → Throughput 감소
```

---

### 5-3. Thermal Throttling

GPU 과열 시 **Clock 자동 감소** 발생.

```
GPU 과열
 → Clock Downclock
 → Inference 지연
 → P99 증가
```

---

### 5-4. NUMA Mismatch

GPU가 Socket 0에 연결, Application Thread는 Socket 1에서 실행 시 발생.

```
GPU (Socket 0)  →  Application (Socket 1)
 → Remote Memory Access 증가
 → PCIe Transfer Latency 증가
```

---

### 5-5. GPU Starvation

여러 프로세스가 GPU를 공유할 때 발생.

```
다중 프로세스 GPU 경합
 → Queue 증가
 → Kernel Launch Delay
 → Inference Timeout
```

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Massive Parallelism

GPU는 **수천 개 코어** 기반 병렬 처리. CPU와 구조 목적 자체가 다르다.

| 구분 | CPU | GPU |
|------|-----|-----|
| 코어 수 | 수십 개 | 수천 개 |
| 목적 | 직렬 고속 처리 | 병렬 대량 처리 |
| 적합 워크로드 | 복잡한 단일 작업 | 단순 반복 대량 작업 |

---

### SIMT (Single Instruction Multiple Threads)

동일한 명령어를 대량의 Thread에 동시에 적용.

```
하나의 Instruction  →  수천 Thread 동시 실행
```

---

### CUDA / Compute Kernel

GPU 연산은 **Kernel Launch** 형태로 실행. CPU가 GPU에 작업을 요청하는 단위.

---

### GPU Memory (HBM / GDDR)

GPU는 자체 고대역폭 메모리를 사용.

| 특징 | 설명 |
|------|------|
| 속도 | CPU DRAM 대비 매우 높은 대역폭 |
| 용량 | 제한적 (VRAM OOM 주의) |
| 종류 | HBM (고성능 서버 GPU) / GDDR (일반 GPU) |

---

### PCIe Data Transfer

GPU는 통상 **PCIe Bus**를 통해 CPU와 연결. Data Copy 비용이 성능의 핵심 변수.

```
CPU Memory  ↔  PCIe Bus  ↔  GPU Memory
```

---

### DMA

GPU가 CPU 개입 없이 Host Memory에 직접 접근 가능.

---

### GPUDirect RDMA

NIC와 GPU가 CPU를 우회하여 직접 통신.

```
NIC  ↔  GPU  (CPU 우회, PCIe 직결)
```

AI / HFT 환경에서 초저지연 달성의 핵심 기술.

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

### NUMA 확인

```bash
numactl --hardware
```

### GPU Process 확인

```bash
nvidia-smi pmon
```

### CUDA 정보 확인

```bash
nvcc --version
```

### Kubernetes GPU Node 확인

```bash
kubectl describe node
```

### GPU Device Plugin 확인

```bash
kubectl get pods -n kube-system
```

---

## 요약

```
GPU
 ├── Massive Parallelism   → 수천 코어 병렬 연산
 ├── SIMT                  → 단일 명령어 × 수천 Thread 동시 실행
 ├── CUDA Kernel           → CPU → GPU 작업 요청 단위
 ├── HBM / GDDR            → 고대역폭 자체 메모리 (VRAM OOM 주의)
 ├── PCIe Transfer         → CPU ↔ GPU Data Copy 병목 핵심
 ├── DMA                   → CPU 개입 없는 Host Memory 접근
 └── GPUDirect RDMA        → NIC ↔ GPU 직결 → CPU 우회 초저지연
```

> FinTech 결제 시스템에서 GPU는 단순한 그래픽 장치가 아니라,  
> **Fraud Detection · Risk Scoring · 암호화 가속을 결정하는 실시간 AI 연산의 핵심 가속 계층**이다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*