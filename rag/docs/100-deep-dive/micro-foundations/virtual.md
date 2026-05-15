# 가상 (Virtual)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**가상(Virtual)** 은:

> 하나의 실제 물리 자원을 논리적으로 분리·추상화하여 **여러 개처럼 보이게 만드는 기술**

핵심은:

> "실제로는 하나인데, 소프트웨어적으로 **여러 개처럼 동작하게 만드는 것**"

### 예시

- 하나의 서버 → 여러 VM
- 하나의 저장장치 → 여러 Volume
- 하나의 네트워크 → 여러 Virtual Network
- 하나의 프로세스 공간 → 여러 Container

즉, 가상은 **실체를 숨기고 논리적 독립성을 제공하는 추상화 계층**이다.

---

## 2. 시스템 어디에서 등장하는가

가상화는 **현대 시스템 거의 모든 계층**에 존재한다.

### Compute Virtualization
- VM
- Container
- Sandbox

### Memory Virtualization
- Virtual Memory
- Paging
- Address Space Isolation

### Storage Virtualization
- Virtual Disk
- SAN
- Volume
- Overlay FS

### Network Virtualization
- VPC
- Overlay Network
- VXLAN
- SDN

### Runtime Virtualization
- JVM
- WASM
- Language Runtime

### Cloud Virtualization
- AWS EC2
- Kubernetes Node
- Serverless

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가상화는 **모든 자원을 논리적으로 분할·격리·공유**한다.

| 자원 | 가상화 영향 |
|------|-----------|
| CPU | 누가 언제 실제 연산 자원을 사용할지 소프트웨어가 결정 |
| Memory | 각 VM/Container가 독립 메모리처럼 보이도록 관리 |
| Storage | 논리 디스크가 실제 물리 저장소에 매핑 |
| Network | 논리 네트워크가 실제 물리 네트워크 위에서 동작 |

---

## 4. 왜 중요한가

현대 클라우드와 Kubernetes는 **가상화 없이는 존재 자체가 어려운** 수준이다.

### 가상화의 핵심 가치

| 가치 | 의미 |
|------|------|
| Resource Efficiency | 하나의 물리 장비를 여러 서비스가 공유 가능 |
| Isolation | 서비스끼리 서로 영향 최소화 |
| Elastic Scaling | 즉시 생성 / 즉시 삭제 / 즉시 확장 가능 |
| Portability | 논리 환경을 쉽게 이동 가능 (VM Migration, Container Relocation) |
| Automation Friendly | Infrastructure as Code와 매우 잘 맞음 |

---

## 5. 실제 장애와 어떤 관련이 있는가

가상화는 편리하지만, **추상화 계층이 추가되므로 새로운 장애 유형**이 생긴다.

### Resource Contention
논리적으로 분리되어 보여도 실제 물리 자원은 공유 중

결과: Latency Spike, Noisy Neighbor

### Oversubscription
실제 자원보다 더 많이 가상 할당

결과: Queueing, Throttling, Saturation

### Hypervisor Failure
가상화 관리 계층 자체 장애 시 다수 VM 동시 영향

### Overlay Network Complexity
가상 네트워크 문제는 추적 어려움

예: MTU mismatch, VXLAN encapsulation issue

### Storage Abstraction Delay
논리 디스크와 실제 디스크 사이 계층 증가 시 I/O latency 증가

### Virtualization Drift
논리 설정과 실제 상태 불일치 발생 가능

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

### Abstraction
가상의 핵심은 실제 구현을 숨기고 **논리 인터페이스만 제공**하는 것

### Resource Sharing
가상 환경은 물리 자원을 여러 사용자에게 분배한다

### Isolation
가상화는 충돌 감소 / 보안 분리 / 장애 격리를 목표로 함

### Mapping
모든 가상 자원은 결국 **어딘가의 물리 자원에 매핑**된다

```
Virtual → Logical
Physical → Actual
```

### Scheduler / Allocator 존재
가상 환경에는 항상 Scheduler, Resource Manager, Allocator가 존재하며, 누구에게 / 언제 / 얼마나 자원을 줄지 결정한다.

### Illusion of Independence
가상 환경은 **"독립적인 것처럼 보이게 만드는 기술"**이다. 하지만 실제로는 공유된 전력, 공유된 메모리, 공유된 네트워크, 공유된 디스크 위에서 움직인다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Virtual Machine
```bash
virsh list
virt-manager
```

### Container
```bash
docker ps
crictl ps
```

### Namespace / Isolation
```bash
lsns
systemd-cgls
```

### cgroup
```bash
cat /sys/fs/cgroup/cpu/kubepods/.../cpu.cfs_quota_us
cat /sys/fs/cgroup/memory/.../memory.limit_in_bytes
```

### Virtual Network
```bash
ip link
bridge link
```

### Virtual Filesystem
```bash
mount
```

### Kubernetes 대표 추상화 계층
- `Pod`
- `Service`
- `Overlay Network`
- `PersistentVolume`
- `Namespace`

### Cloud 논리 자원
Instance, Volume, VPC, Load Balancer 모두 가상 자원이다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*