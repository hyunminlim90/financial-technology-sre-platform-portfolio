# Computing Unit (연산 장치)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**Computing Unit(연산 장치)** 는 입력 데이터를 받아 산술 / 논리 연산을 수행하고 **결과를 생성하는 실행 하드웨어**이다.

### 핵심 역할

- **계산 수행** — 산술 / 논리 연산
- **명령 실행** — Instruction 해석 및 처리
- **데이터 변환** — 입력 → 출력 변환
- **제어 흐름 처리** — 분기 / 루프 처리

> **실제 연산을 수행하는 물리적 실행 주체**

<details>
<summary>Deep Dive</summary></br>

Arithmetic/Logical Operation(산술/논리 연산) [[M]](../../100-deep-dive/micro-foundations/arithmetic-logical-operation.md)

</details></br>

## 2. 시스템 어디에서 등장하는가

연산 장치는 시스템 전체에 존재한다.

### CPU

```
비즈니스 로직  /  OS 실행  /  스케줄링  /  트랜잭션 제어
```

### GPU

```
병렬 행렬 연산  /  AI / FDS  /  벡터 처리
```

### NIC

```
Packet Offload  /  Checksum  /  RSS
```

### Storage Controller

```
RAID 계산  /  Compression  /  Encryption
```

### FPGA / NPU

```
초저지연 연산  /  AI 추론  /  특화 가속
```

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 영향이 큰 자원: **CPU + Memory**

연산 장치는 데이터를 읽고 → 계산하고 → 결과를 저장해야 하기 때문.

| 자원 | 영향 |
|------|------|
| CPU | 연산 수행 — 클럭 / 코어 수 직결 |
| Memory | 데이터 공급 — 대역폭 / 레이턴시 |
| Cache | 성능 핵심 — Miss 증가 시 IPC 감소 |
| Power / Thermal | 클럭 유지 — 발열 시 Throttling |

---

## 4. 왜 중요한가

결제 시스템의 다음 항목이 **연산 장치 성능에 직접 연결**된다.

| 결제 시스템 지표 | 연결 연산 장치 |
|-----------------|---------------|
| 승인 속도 | CPU 처리 속도 |
| 정산 처리량 | CPU / GPU 병렬 연산 |
| FDS 응답 시간 | GPU Inference 속도 |
| 암호화 처리 | CPU / FPGA 오프로드 |

> **연산 장치 성능 = 시스템 처리 성능**

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. CPU Saturation

연산 장치 포화 시 발생.

```
CPU 포화
 → 응답 지연
 → Run Queue 증가
 → Timeout
 → 결제 승인 실패
```

---

### 5-2. Thermal Throttling

온도 상승 시 **Clock Frequency 자동 감소**.

```
발열 증가
 → Thermal Throttling
 → Clock Frequency 감소
 → IPC 감소
 → TPS 하락
```

---

### 5-3. Cache Miss 증가

메모리 접근 증가 시 연산 효율 저하.

```
Cache Miss 증가
 → Main Memory 접근 증가
 → 메모리 레이턴시 노출
 → IPC 감소
 → Tail Latency 증가
```

---

### 5-4. Power Delivery 문제

전력 불안정 시 하드웨어 수준 오류 발생.

```
전력 불안정
 → Machine Check Exception (MCE)
 → Kernel Panic
 → System Reset
```

---

### 5-5. NUMA Remote Access

잘못된 CPU / Memory 배치 시 발생.

```
연산 장치 (Socket 1)  →  Memory (Socket 0)
 → UPI Cross Traffic 증가
 → Remote Memory Latency 증가
 → P99 악화
```

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### ALU (Arithmetic Logic Unit)

실제 계산을 수행하는 연산 회로.

| 연산 종류 | 예시 |
|-----------|------|
| 산술 연산 | 덧셈, 뺄셈, 곱셈 |
| 비교 연산 | 대소 비교, 동등 비교 |
| 논리 연산 | AND, OR, XOR, NOT |

---

### Register

CPU 내부 초고속 저장소. 연산 데이터를 임시 저장하며, 가장 빠른 데이터 접근 단위.

```
Register  →  L1 Cache  →  L2 Cache  →  L3 Cache  →  RAM
  (최소 지연)                                      (최대 지연)
```

---

### Control Unit

명령어를 해석하고 실행 흐름을 제어. ALU / Register / Memory 간 데이터 이동을 조율.

---

### Pipeline

현대 CPU는 명령어를 여러 단계로 분리하여 겹쳐 실행.

```
Fetch → Decode → Execute → Memory → Write-back
         ↑ 각 단계를 동시에 다른 명령어가 점유
```

---

### SIMD (Single Instruction Multiple Data)

하나의 명령으로 여러 데이터를 동시에 연산.

| 확장 명령어 | 처리 폭 |
|-------------|---------|
| AVX | 256 bit |
| AVX2 | 256 bit (정수 포함) |
| AVX-512 | 512 bit |

---

### SIMT (Single Instruction Multiple Threads)

GPU 방식의 병렬 실행 구조. 동일 명령어를 수천 Thread에 동시 적용.

---

### Cache Hierarchy

연산 장치 성능의 핵심 구조.

| 레벨 | 속도 | 용량 | 위치 |
|------|------|------|------|
| L1 Cache | 최고속 | 수십 KB | Core 전용 |
| L2 Cache | 고속 | 수백 KB ~ 수 MB | Core 전용 |
| L3 Cache | 중속 | 수십 MB | Socket 공유 |
| RAM | 저속 | 수백 GB | NUMA Node |

---

### IPC (Instructions Per Cycle)

클럭당 처리 가능한 명령 수. **CPU 효율의 핵심 지표**.

```
성능 = Clock Frequency × IPC
       ↑ 주파수만이 아니라 명령 처리 효율이 함께 결정
```

---

### Offloading

특정 계산을 전용 장치로 이동하여 CPU 부하 절감.

| 오프로드 대상 | 장치 |
|---------------|------|
| 병렬 행렬 연산 | GPU |
| Packet 처리 | SmartNIC |
| 특화 AI 추론 | FPGA / NPU |

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### CPU 정보 확인

```bash
lscpu
```

### CPU 사용률 확인

```bash
mpstat -P ALL 1
```

### IPC / Pipeline 분석

```bash
perf stat
```

### Cache Miss 분석

```bash
perf top
perf record
```

### NUMA 상태 확인

```bash
numactl --hardware
```

### Thermal 상태 확인

```bash
sensors
```

### CPU Frequency 확인

```bash
cpupower frequency-info
```

### GPU 상태 확인

```bash
nvidia-smi
```

### Kubernetes Resource 상태 확인

```bash
kubectl top node
```

---

## 요약

```
Computing Unit
 ├── ALU              → 산술 / 논리 / 비교 연산 수행
 ├── Register         → CPU 내부 최고속 임시 저장소
 ├── Control Unit     → 명령 해석 및 실행 흐름 제어
 ├── Pipeline         → 명령어 단계 분리 → 겹쳐 실행
 ├── SIMD / SIMT      → 단일 명령 × 다중 데이터 / Thread 동시 처리
 ├── Cache Hierarchy  → L1 → L2 → L3 → RAM → Miss 시 Latency 급증
 ├── IPC              → 클럭당 명령 수 → 주파수만큼 중요한 효율 지표
 └── Offloading       → GPU / SmartNIC / FPGA → CPU 부하 분산
```

> FinTech 결제 시스템에서 연산 장치는 단순한 프로세서가 아니라,  
> **승인 속도 · 정산 처리량 · FDS 응답 · 암호화 성능을 결정하는 시스템 성능의 물리적 기반**이다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*