# 하이퍼스케일 데이터센터 (Hyperscale Data Center)

> 정독: 0회

## 1. 이 기술이 무엇인가

하이퍼스케일 데이터센터는:

> **수만~수십만 대 규모의 서버·스토리지·네트워크 장비를 초대형 단일 인프라로 통합하고, 클라우드·AI·대규모 인터넷 서비스를 지구 규모로 운영하기 위해 설계된 초대형 데이터센터**

주요 운영 조직: Amazon Web Services, Google Cloud, Microsoft Azure, Meta Platforms, Oracle Cloud Infrastructure

### 핵심 특징

일반 데이터센터와 가장 큰 차이는 규모, 자동화 수준, 네트워크 구조, 전력 밀도, 운영 방식, 장애 처리 방식입니다.

### 핵심 목표

> 대규모 트래픽·클라우드·AI 연산을 중단 없이 수평 확장 가능한 구조로 처리

---

## 2. 시스템 어디에서 등장하는가

현대 인터넷 서비스 대부분의 기반입니다.

### 클라우드 플랫폼

IaaS, PaaS, SaaS

### AI 인프라

LLM 학습, GPU Cluster, Distributed Training, Inference Serving

### CDN 및 글로벌 서비스

검색 엔진, 동영상 스트리밍, SNS, 메신저

### 대규모 데이터 처리

Big Data, Analytics, Stream Processing

### Kubernetes 플랫폼

Multi-cluster, Service Mesh, Massive Container Orchestration

### 금융·결제 플랫폼

초대형 API 처리, 글로벌 결제 네트워크, 실시간 트랜잭션 처리

---

## 3. 어떤 자원에 가장 영향이 큰가

전부 중요하지만 핵심은 **Network + Power + Compute Density**입니다.

| 자원 | 영향 영역 |
|------|-----------|
| CPU | VM, Container, AI Inference, Scheduling |
| Memory | Cache, In-memory DB, AI Tensor Memory |
| Network | Spine-Leaf Fabric, East-West Traffic, RDMA, 100G/400G Ethernet |
| Disk/Storage | Object Storage, Distributed FS, NVMe Cluster |

### 추가로 매우 중요한 자원

**전력(Power):** 핵심 중 핵심. 수십~수백 MW 소비.

**냉각(Cooling):** AI/GPU 시대에서 매우 중요. Liquid Cooling, Immersion Cooling 등을 사용합니다.

---

## 4. 왜 중요한가

현대 인터넷 대부분이 여기서 실행됩니다.

### 클라우드 기반 제공

대부분 서비스가 CSP 인프라 위에서 동작합니다.

### AI 인프라 중심

대규모 GPU 연산을 제공합니다.

### 글로벌 확장성

대륙 단위 서비스 운영이 가능합니다.

### 자동화 기반 운영

수만 대 이상은 수동 운영이 불가능합니다.

### 대규모 장애 복원

자동 Failover 및 Region 분산을 지원합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

하이퍼스케일 데이터센터 장애는 **전 세계 서비스 장애**로 이어질 수 있습니다.

### Power Failure

전력 계통 문제.

영향: Rack shutdown, Region outage

### Cooling Failure

냉각 실패.

영향: Thermal throttling, GPU shutdown, Emergency power-off

### Spine/Leaf Network 장애

DC 내부 네트워크 문제.

영향: massive packet loss, cluster partition, service unreachable

### BGP / WAN 장애

데이터센터 간 연결 문제.

영향: Region isolation, Cross-region failure

### Storage 장애

분산 저장 계층 문제.

영향: DB unavailable, object storage latency

### Control Plane 장애

예: Kubernetes API, SDN controller, orchestration failure

영향: 신규 pod 생성 실패, auto scaling failure

### AI Cluster 장애

GPU fabric 문제.

영향: distributed training stop, inference latency spike

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### ① Scale-Out 구조

수직 확장보다 **서버 수를 병렬 증가**시키는 구조. 수천~수만 노드 cluster가 예입니다.

### ② Virtualization

Hypervisor, VM, Container 기반의 핵심 추상화 계층입니다.

### ③ Distributed System

서비스를 분산 실행합니다.

예: Distributed DB, Distributed Cache, Distributed Queue

### ④ Spine-Leaf Network

현대 데이터센터 핵심 네트워크 구조.

특징: low latency, massive east-west traffic 처리

### ⑤ Software Defined Infrastructure

거의 모든 인프라가 소프트웨어 기반 제어를 사용합니다.

예: SDN, IaC, orchestration

### ⑥ Massive Automation

수동 운영이 불가능하므로 자동화가 필수입니다.

예: Auto Provisioning, Auto Scaling, Auto Healing

### ⑦ Region / Availability Zone 분리

대규모 장애 격리를 위한 구조입니다.

예: Multi-AZ, Multi-Region

### ⑧ Observability 기반 운영

초대규모 telemetry가 필수입니다.

예: metrics, tracing, logging, flow telemetry

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

**CPU / NUMA 확인**

```bash
lscpu
numactl
top
htop
```

**Network 확인**

```bash
ethtool
ss
ip link
tc
```

**Storage 확인**

```bash
iostat
nvme list
fio
```

**메모리 / AI GPU 확인**

```bash
free -h
vmstat
nvidia-smi
```

### Runtime

대규모 Runtime 최적화가 중요합니다.

예: thread scheduling, connection pooling, async I/O, memory fragmentation

### Kubernetes

핵심 플랫폼 중 하나입니다.

**주요 구성:** kubelet, container runtime, CNI, CSI, scheduler

**관측 명령어**

```bash
kubectl top
kubectl describe
kubectl get events
```

**중요 영역:** pod density, node pressure, network latency, cluster autoscaling

### 데이터센터 네트워크 관측

east-west traffic, overlay network, service mesh latency

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*