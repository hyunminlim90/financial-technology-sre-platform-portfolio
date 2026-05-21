# 추상화 계층 (Abstraction Layer)

> 정독: 0회

## 1. 이 기술이 무엇인가

추상화 계층은:

> 복잡한 시스템을 역할별 독립 계층으로 분리하고, 각 계층의 내부 구현을 숨긴 채 **정의된 인터페이스만 노출하는 구조**

### 핵심 목적

- 복잡성 감소
- 역할 분리
- 독립적 개발
- 시스템 확장
- 변경 영향 최소화

### 핵심 특징

각 계층은 자신의 책임만 수행하고, 하위 내부 구현은 숨기며, 상위에는 결과만 제공합니다.

### 대표 사례

| 영역 | 예시 |
|------|------|
| 네트워크 | TCP/IP Layer |
| 운영체제 | User Space / Kernel Space |
| 클라우드 | IaaS / PaaS / SaaS |
| Kubernetes | Pod abstraction, Service abstraction |
| 하드웨어 | CPU ISA abstraction, Device driver abstraction |

> 추상화 계층은 복잡한 시스템을 독립적 기능 단위로 분리하는 **컴퓨터 과학의 핵심 구조 원리**입니다.

---

## 2. 시스템 어디에서 등장하는가

현대 모든 컴퓨팅 시스템에 등장합니다.

| 영역 | 예시 |
|------|------|
| 네트워크 스택 | Ethernet, IP, TCP, HTTP |
| 운영체제 | syscall, virtual memory, filesystem |
| 가상화 환경 | hypervisor, VM abstraction |
| Kubernetes | Pod, Service, Ingress |
| 클라우드 플랫폼 | VPC abstraction, object storage abstraction |
| 프로그래밍 언어 런타임 | JVM, CLR, Python VM |
| 데이터베이스 | SQL abstraction, storage engine abstraction |

---

## 3. 어떤 자원에 가장 영향이 큰가

특정 하나가 아니라 전체 자원 구조에 영향을 줍니다. 다만 **CPU와 Memory 영향이 특히 큽니다.**

| 자원 | 영향도 | 예시 |
|------|--------|------|
| CPU | 중요 | context switching, virtualization overhead, runtime abstraction |
| Memory | 매우 중요 | buffer abstraction, cache abstraction, virtual memory |
| Network | 중요 | protocol layering, SDN abstraction, overlay networking |
| Disk | 중요 | filesystem abstraction, block device abstraction |

> 추상화 계층은 모든 시스템 자원을 **논리적 인터페이스 뒤에 숨깁니다.**

---

## 4. 왜 중요한가

현대 시스템 확장의 핵심 원리입니다.

- **복잡성 감소**: 전체 시스템을 한 번에 이해할 필요 제거
- **독립적 변경 가능**: 특정 계층만 교체 가능
- **유지보수성 향상**: 영향 범위 축소 가능
- **확장성 확보**: 대규모 시스템 구성 가능
- **장애 격리 가능**: 계층별 문제 분리 가능
- **표준화 가능**: 인터페이스 기반 개발 가능
- **자동화 가능**: 인프라 추상화 → API 제어 가능

클라우드·Kubernetes·SDN·가상화 전부 추상화 계층 위에서 동작합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실무에서 매우 중요합니다.

| 장애 유형 | 설명 | 예시 |
|-----------|------|------|
| Abstraction Leak | 하위 구현 세부사항이 상위로 노출 | noisy neighbor, storage latency leak |
| Interface Mismatch | 계층 간 인터페이스 불일치 | MTU mismatch, API contract failure |
| Hidden Bottleneck | 추상화 내부 병목 은닉 | hypervisor CPU steal, overlay network overhead |
| Cascading Failure | 하위 계층 장애가 상위 전체로 전파 | network issue → database timeout → API failure |
| Resource Contention | 공유 자원 충돌 | VM CPU contention, storage IOPS saturation |
| State Desynchronization | 계층 상태 불일치 | SDN control plane inconsistency |

> 추상화는 복잡성을 숨기지만, 장애 발생 시 **숨겨진 내부 구조를 역추적해야 합니다.**

---

## 6. 핵심 메커니즘

핵심 메커니즘은 10개입니다.

| # | 메커니즘 | 설명 |
|---|----------|------|
| ① | Encapsulation | 내부 구현 숨김 |
| ② | Interface Exposure | 표준 인터페이스만 공개 |
| ③ | Layer Isolation | 계층 독립성 유지 |
| ④ | Loose Coupling | 강한 의존 제거 |
| ⑤ | Responsibility Separation | 역할 분리 |
| ⑥ | Hierarchical Composition | 계층적 구조 구성 |
| ⑦ | Virtualization | 물리 자원 논리화 |
| ⑧ | Resource Pooling | 공유 자원 집합화 |
| ⑨ | Hardware Independence | 하드웨어 변경 영향 최소화 |
| ⑩ | Replaceability | 특정 계층 교체 가능 |

### TCP/IP 계층 예시

```
Application
    ↓
Transport
    ↓
Internet
    ↓
Network Access
```

각 계층은 바로 아래 계층만 사용하고, 내부 구현은 비공개이며, 인터페이스 기반으로 통신합니다.

### 클라우드 계층 예시

```
SaaS
    ↓
PaaS
    ↓
IaaS
    ↓
Physical Infrastructure
```

### Kubernetes 계층 예시

```
Application
    ↓
Container
    ↓
Pod
    ↓
Node
    ↓
VM
    ↓
Physical Server
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# Virtual Memory
free -h
vmstat

# Filesystem Abstraction
mount
df -h

# Network Stack
ip addr
ss -ant

# Device Abstraction
lsblk
lspci
```

### 가상화

- **Hypervisor Layer**: KVM, Xen, VMware
- **Container Layer**: namespaces, cgroups

### Kubernetes

```bash
# Pod Abstraction
kubectl get pods

# Service Abstraction
kubectl get svc

# Ingress Abstraction
kubectl get ingress
```

- **Overlay Network**: Calico, Cilium

### 클라우드

- VPC
- virtual disk
- managed DB
- serverless runtime

---

## 8. 더 깊게 공부해야 하는 상위 키워드

`Abstraction` · `Layered Architecture` · `Encapsulation` · `Virtualization` · `Hypervisor` · `Software Defined Networking` · `Containerization` · `Kubernetes` · `Microservices` · `Distributed Systems`

---

## 한 줄 핵심 정리

> 추상화 계층(Abstraction Layer)은 복잡한 시스템의 내부 구현을 숨기고, **표준 인터페이스 기반의 독립 계층 구조로 분리한 컴퓨터 시스템 설계 원리**입니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*