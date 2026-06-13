# 컴퓨트 자원 (Compute Resources)

> 정독: 0회

## 1. 이 기술이 무엇인가

컴퓨트 자원은:

> **프로그램 실행과 데이터 연산을 수행하기 위해 시스템이 제공하는 CPU·메모리·GPU 등의 연산 자원**

클라우드·가상화·컨테이너 환경에서는 물리 하드웨어를 논리적으로 분할하여 제공합니다.

### 핵심 구성 요소

**CPU / vCPU** — 연산 처리 자원.

예: instruction execution, thread scheduling, arithmetic operation

**Memory / RAM** — 실행 중 데이터 저장 공간.

예: process memory, page cache, runtime heap

**GPU / vGPU** — 병렬 연산 자원.

예: AI inference, AI training, rendering, vector processing

### 핵심 특징

컴퓨트 자원은 **실행 가능한 연산 능력 자체**를 의미합니다. 스토리지나 네트워크와 구분되는 개념입니다.

---

## 2. 시스템 어디에서 등장하는가

현대 IT 시스템 거의 모든 계층에서 등장합니다.

### 물리 서버

Bare Metal Server, NUMA System, Multi-core CPU

### 가상화 환경

Hypervisor, Virtual Machine, vCPU allocation

### 클라우드 플랫폼

EC2, Compute Engine, Virtual Instance

### 컨테이너 플랫폼

Kubernetes, Container Runtime, cgroup 기반 자원 제한

### AI 플랫폼

GPU Cluster, Tensor Processing, Distributed Compute

### 분산 시스템

distributed scheduler, batch processing, stream processing

---

## 3. 어떤 자원에 가장 영향이 큰가

핵심은 **CPU + Memory**입니다. AI 환경에서는 GPU 비중이 매우 커집니다.

| 자원 | 영향 요소 |
|------|-----------|
| CPU | core count, clock, cache, context switching |
| Memory | memory capacity, bandwidth, latency, NUMA locality |
| GPU | tensor core, VRAM, parallel throughput |
| Network | east-west traffic, RDMA, cluster synchronization |
| Disk | swap, checkpoint, model loading |

---

## 4. 왜 중요한가

컴퓨트 자원은 **서비스 처리 능력 자체**입니다.

### 서비스 성능 결정

API 처리량, latency, throughput에 직접 영향을 줍니다.

### 확장성 결정

horizontal scaling, workload scheduling의 기반입니다.

### 비용 결정

클라우드 비용 대부분이 compute 기반입니다. (vCPU-hour, GPU-hour)

### AI 처리 가능 여부 결정

LLM·추론·학습은 compute 집약적입니다.

### 멀티테넌시 안정성 결정

자원 격리 실패 시 **noisy neighbor** 문제가 발생합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### CPU Saturation

CPU 사용률 과도 증가.

영향: latency 증가, timeout, scheduler delay

### Memory Exhaustion

메모리 부족.

영향: OOM, swap storm, process kill

### GPU Resource Exhaustion

GPU 부족.

영향: inference failure, CUDA allocation error

### Hypervisor Overcommit

가상화 자원 과할당.

영향: VM 성능 저하, noisy neighbor

### cgroup Limit 문제

컨테이너 제한 충돌.

영향: throttling, container restart

### NUMA Imbalance

메모리 locality 깨짐.

영향: cache miss 증가, latency spike

### Compute Scheduling Failure

스케줄링 실패.

영향: pod pending, cluster imbalance

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### ① Virtualization

물리 자원을 논리적으로 분할합니다.

예: vCPU, vRAM, virtual NUMA

### ② Scheduler

누가 CPU를 얼마나 사용할지 결정합니다.

예: Linux CFS, hypervisor scheduler, Kubernetes scheduler

### ③ Context Switching

CPU가 여러 작업을 번갈아 수행합니다. 과도하면 성능 저하가 발생합니다.

### ④ Memory Management

paging, cache, huge pages, NUMA가 핵심 메커니즘입니다.

### ⑤ Resource Isolation

멀티테넌시의 핵심입니다.

예: cgroup, namespace, quota

### ⑥ Horizontal Scaling

컴퓨트 부족 시 **서버 수 증가**로 대응합니다.

### ⑦ Hardware Acceleration

특정 연산에 전용 하드웨어를 사용합니다.

예: GPU, TPU, SmartNIC, DPU

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

**CPU 확인**

```bash
top
htop
mpstat
pidstat
```

**Memory 확인**

```bash
free -h
vmstat
numastat
```

**NUMA 확인**

```bash
lscpu
numactl --hardware
```

**GPU 확인**

```bash
nvidia-smi
```

### Runtime

- **Process Scheduling**: thread pool, async runtime, event loop
- **Memory Runtime**: heap, garbage collection, allocator

### Kubernetes

**Resource Request / Limit 설정**

```yaml
resources:
  requests:
    cpu: "2"
    memory: "4Gi"
```

**주요 관측 명령어**

```bash
kubectl top pod
kubectl top node
kubectl describe pod
```

**주요 상태:** CPU throttling, OOMKilled, Pending, Eviction

### 하이퍼바이저 계층

예: KVM, Xen, Hyper-V

관측: vCPU steal time, overcommit ratio

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*