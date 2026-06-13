# 클라우드 서비스 제공업체 (CSP - Cloud Service Provider)

> 정독: 0회

## 1. 이 기술이 무엇인가

클라우드 서비스 제공업체(CSP)는:

> **대규모 데이터센터와 네트워크 인프라를 운영하며, 컴퓨팅·스토리지·네트워크·플랫폼·소프트웨어 자원을 인터넷을 통해 서비스 형태로 제공하는 조직**

대표적으로 Amazon Web Services, Microsoft Azure, Google Cloud 등이 존재합니다.

### 핵심 특징

CSP는 단순 호스팅 업체가 아닙니다. 다음 전체를 통합 운영합니다.

물리 서버, 스토리지, 글로벌 백본망, 가상화 플랫폼, API 기반 제어 시스템, 데이터센터 전력/냉각, 보안 체계, 글로벌 DNS/CDN

<details>
<summary>Deep Dive</summary></br>

Hyperscale Data Center(하이퍼스케일 데이터센터) [[M]](../../100-deep-dive/micro-foundations/hyperscale-data-center.md)  
Backbone Network(백본망) [[M]](../../100-deep-dive/micro-foundations/backbone-network.md)  
Compute Resources(컴퓨트 자원) [[M]](../../100-deep-dive/micro-foundations/compute-resources.md)  
Cloud Storage Resources(클라우드 스토리지 자원) [[M]](../../100-deep-dive/micro-foundations/cloud-storage-resources.md)  
Virtual Network Resources(가상 네트워크 자원) [[M]](../../100-deep-dive/micro-foundations/virtual-network-resources.md)  
Platform Service Resources(플랫폼 서비스 자원) [[M]](../../100-deep-dive/micro-foundations/platform-service-resources.md)  
Software Service Resources(소프트웨어 서비스 자원) [[M]](../../100-deep-dive/micro-foundations/software-service-resources.md)  
Internetwork(인터넷워크) [[M]](../../100-deep-dive/micro-foundations/internetwork.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

현대 대부분의 인터넷 서비스에서 등장합니다.

### 기업 서비스

금융 시스템, 전자상거래, SaaS, AI 플랫폼, 게임 서버

### 인프라 운영

VM, Kubernetes, Object Storage, CDN, Database

### 글로벌 서비스

Multi Region, Edge Network, Global Load Balancing

### 개발 환경

CI/CD, Artifact Registry, IaC, Monitoring

### AI/ML

GPU Cluster, Distributed Training, Vector Database

---

## 3. 어떤 자원에 가장 영향이 큰가

사실상 **모든 핵심 컴퓨팅 자원**에 영향을 줍니다.

| 자원 | 영향 영역 |
|------|-----------|
| CPU | VM Compute, Container Runtime, AI Inference, Distributed Compute |
| Memory | Cache, In-memory DB, AI Model Serving |
| Disk | Block Storage, Object Storage, Distributed Filesystem |
| Network | Global Backbone, VPC, SDN, CDN, Inter-Region Replication |

Network는 특히 매우 중요합니다.

---

## 4. 왜 중요한가

CSP는 **현대 인터넷 서비스 인프라의 표준 실행 환경**이 되었습니다.

### 인프라 추상화

물리 서버 직접 구매 없이 VM, Storage, Network 생성이 가능합니다.

### 글로벌 확장

수 분 내 Multi Region, Global Deployment가 가능합니다.

### 탄력성

Auto Scaling, Elastic Resource Allocation을 지원합니다.

### 운영 자동화

API 기반 인프라 관리가 가능합니다.

### 대규모 네트워크 운영

자체 Backbone 기반으로 낮은 latency, 고속 replication, 글로벌 traffic distribution이 가능합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

현대 대규모 장애 상당수가 CSP와 연관됩니다.

### Region 장애

특정 리전 장애.

증상: 서비스 대규모 다운, API 실패, Storage 장애

### Control Plane 장애

Cloud API 실패.

증상: 새 리소스 생성 불가

### Network Partition

Region 간 연결 문제.

증상: Replication 실패, Cluster Split, Timeout 증가

### IAM 장애

인증 시스템 문제.

증상: 서비스 접근 불가, API 인증 실패

### Storage 장애

Object/Block Storage 문제.

증상: DB 장애, Image Pull 실패, Backup 실패

### Backbone 장애

Cloud Global Network 문제.

증상: Cross-region latency 증가, CDN 장애, 글로벌 서비스 품질 저하

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### ① Virtualization

하나의 물리 서버를 다수의 논리적 서버(VM)로 분할합니다.

### ② Multi-Tenant 구조

다수 고객이 동일 물리 인프라를 공유하되 논리적으로 완전 분리됩니다.

### ③ API 기반 인프라 제어

모든 리소스를 API, IaC로 생성/삭제 가능합니다.

### ④ Software Defined Network (SDN)

Cloud 내부 네트워크는 대부분 **소프트웨어 기반 네트워크 제어**를 사용합니다.

### ⑤ Distributed Storage

데이터는 다중 서버, 다중 AZ, 다중 Region에 분산 저장됩니다.

### ⑥ HyperScale Backbone

대형 CSP는 자체 글로벌 광망을 운영합니다.

특징: 낮은 latency, 고속 replication, AS 내부 최적화

### ⑦ Elastic Scaling

부하 증가 시 VM, Container, Network 확장이 자동 수행 가능합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

Cloud VM 대부분이 Linux 기반입니다.

**주요 확인 명령어**

```bash
ip addr
ip route
ss
top
iostat
ethtool
```

**Cloud Metadata 확인**

```bash
curl <metadata endpoint>
```

### Runtime

Cloud는 Runtime 계층의 핵심 기반입니다.

예: Hypervisor, Container Runtime, Service Mesh

### Kubernetes

현대 CSP의 핵심 서비스 중 하나입니다.

**대표 서비스:** EKS, GKE, AKS

**관측 영역:** CNI, Ingress, Load Balancer, Cluster Autoscaler, CSI Storage

### Cloud Native

특히 중요한 영역입니다.

IaC, GitOps, Observability, Multi Cluster

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*