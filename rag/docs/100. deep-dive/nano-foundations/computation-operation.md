# 연산 (Operation / Computation)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**연산(Operation / Computation)** 은:

> 입력 데이터를 특정 규칙(알고리즘)에 따라 처리하여 **새로운 결과를 만드는 과정**

컴퓨터 시스템 내부에서는 결국 데이터 읽기, 비교, 계산, 변환, 판단 모두 연산이다.

### 가장 기본적인 연산 종류

| 종류 | 설명 |
|------|------|
| 산술 연산 | `+`, `-`, `*`, `/` |
| 논리 연산 | `AND`, `OR`, `NOT` |
| 비교 연산 | `==`, `>`, `<` |
| 비트 연산 | `XOR`, `SHIFT` |
| 메모리 연산 | Load / Store |

즉, **컴퓨터가 실제로 일을 수행하는 행위 자체**가 연산이다.

<details>
<summary>Deep Dive</summary></br>

Input Data(입력 데이터) [[M]](../../100-deep-dive/micro-foundations/input-data.md)  
Algorithm(알고리즘) [[M]](../../100-deep-dive/micro-foundations/algorithm.md)

</details></br>

## 2. 시스템 어디에서 등장하는가

연산은 **시스템 전체**에 존재한다.

### CPU
- ALU 계산
- Branch 판단
- Register 처리

### GPU
- 행렬 계산
- 병렬 벡터 연산

### Network
- TCP Checksum
- TLS Encryption
- Packet Parsing

### Storage
- Checksum
- Compression
- Filesystem Metadata 처리

### JVM / Runtime
- GC
- JIT Compilation
- Object Allocation

### Kubernetes / Cloud
- Scheduling
- Resource Accounting
- cgroup 계산

### Security
- `SHA-256`
- `AES`
- Signature Validation

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접 영향: **CPU**

연산은 결국 **CPU Cycle 소비**를 의미한다. 하지만 실제 시스템에서는:

| 자원 | 연산과의 관계 |
|------|-------------|
| CPU | 실제 계산 수행 |
| Memory | 연산 데이터 공급 |
| Network | Packet 처리 연산 |
| Disk | I/O 처리 및 checksum 연산 |

> 연산은 CPU 중심이지만 **전체 시스템 자원과 연결**된다.

---

## 4. 왜 중요한가

서비스의 응답속도, 처리량, 비용, 안정성 모두 **연산 효율에 의해 결정**된다.

### FinTech에서 특히 중요한 이유

결제 시스템은 낮은 latency, 높은 TPS, 강한 consistency를 요구한다.

> ⚠️ **연산 지연 = 직접적인 결제 지연**

승인 로직, 암호화, FDS 판단, 정산 계산 모두 연산이다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### CPU Saturation
연산량 급증 시 CPU 100% / Load 증가 발생

### Tail Latency 증가
특정 연산이 오래 걸리면 P95/P99 증가 → Timeout 증가 발생

### Retry Storm
느린 연산 → 응답 지연 → 재시도 폭증

### GC Overhead
JVM 연산 중 Allocation 과다 시 GC 연산 증가 발생 가능

### Lock Contention
여러 Thread가 동일 연산 자원 경쟁

결과: Throughput 감소, Context Switch 증가

### SIMD 미사용
벡터화 실패 시 CPU 효율 저하 → 연산 throughput 감소 가능

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

### ALU
실제 산술/논리 연산 수행

### Register
연산 대상 데이터 저장

### CPU Cycle
연산 수행 기본 시간 단위

### Instruction
CPU가 수행하는 최소 명령

### Pipeline
여러 연산 단계 병렬 처리 구조

### Branch Prediction
조건 분기 예측 최적화

### SIMD
벡터 기반 병렬 연산

### Floating Point Unit (FPU)
실수 연산 전용 장치

### Parallel Processing
여러 코어/GPU 활용 동시 연산

### Offload
특정 연산을 `GPU`, `NIC`, `FPGA` 로 이전하는 구조

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### CPU 사용률
```bash
top
mpstat -P ALL 1
```

### Load Average
```bash
uptime
```

### Context Switch
```bash
vmstat 1
pidstat -w 1
```

### perf 연산 Hotspot 분석
```bash
perf top
```

### CPU Pipeline 분석
```bash
perf stat
```

### Flamegraph
```bash
async-profiler
```

### JVM 연산 분석
```bash
jfr
```

### Kubernetes CPU 사용량
```bash
kubectl top pod
```

### eBPF 기반 분석
```bash
# bcc-tools 대표 도구
offcputime
profile
runqlat
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*