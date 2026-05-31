# 가상 네트워크 자원 (Virtual Network Resources)

> 정독: 0회

## 1. 이 기술이 무엇인가

가상 네트워크 자원은:

> 물리 네트워크 장비와 연결 구조를 소프트웨어적으로 추상화하여, 논리적인 네트워크 공간·경로·보안 정책을 동적으로 생성·제어할 수 있게 만든 네트워크 인프라 자원

### 핵심 목적

- 네트워크 격리
- 트래픽 제어
- 라우팅
- 보안 정책 적용
- 서비스 연결
- 분산 시스템 통신

### 핵심 특징

물리 스위치·라우터를 직접 조작하지 않고 API, 선언형 설정, 컨트롤러, 소프트웨어 정책으로 네트워크를 제어합니다.

---

## 2. 시스템 어디에서 등장하는가

현대 클라우드 및 분산 시스템 거의 모든 영역에서 등장합니다.

### 클라우드 인프라

VPC, Virtual Network, Subnet, NAT Gateway

### Kubernetes

Pod Network, CNI, Service Mesh, Ingress

### 데이터센터 네트워크

VXLAN, EVPN, overlay network

### 멀티테넌시 환경

tenant isolation, namespace segmentation

### 보안 인프라

security group, ACL, virtual firewall

### 하이브리드 클라우드

VPN, Direct Connect, SD-WAN

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

핵심은 **Network**입니다. 대규모 SDN 환경에서는 CPU 영향도 매우 큽니다.

| 자원 | 영향 | 주요 지표 |
|---|---|---|
| **Network** | 가장 중요 | packet forwarding, routing, encapsulation, bandwidth, latency |
| **CPU** | 가상 네트워크 처리 | virtual switch processing, overlay encapsulation, firewall filtering |
| **Memory** | 라우팅 및 연결 상태 저장 | flow table, conntrack, routing cache |
| **Disk** | 상대적으로 낮음 | network logs, telemetry, flow records |

---

## 4. 왜 중요한가

현대 클라우드는 **네트워크를 소프트웨어적으로 제어하는 구조** 위에서 동작합니다.

### 멀티테넌시 격리

사용자 간 네트워크 완전 분리.

### 자동화 가능

API 기반 네트워크 생성 가능.

### 대규모 확장성

수천~수만 VM/Container 연결 가능.

### 보안 정책 중앙화

정책 기반 제어 가능.

### 클라우드 핵심 기반

컴퓨트·스토리지 연결 핵심.

### Kubernetes 핵심 인프라

Pod-to-Pod 통신 기반.

---

## 5. 실제 장애와 어떤 관련이 있는가

현대 장애 상당수가 **네트워크 가상화 계층**에서 발생합니다.

### Routing Misconfiguration

잘못된 경로 설정. → blackhole, packet loss

### Overlay Network 장애

VXLAN/GRE 문제. → Pod communication failure

### Security Policy 오류

방화벽 정책 차단. → service unreachable

### NAT Gateway 장애

외부 통신 실패. → internet egress failure

### SDN Controller 장애

중앙 제어 계층 실패. → network provisioning failure

### IP Exhaustion

IP 부족. → Pod scheduling failure

### DNS Network 문제

이름 해석 실패. → distributed service outage

### MTU Mismatch

패킷 크기 불일치. → fragmentation, silent packet drop

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

### ① Network Virtualization

물리 네트워크 추상화. 논리 네트워크 생성 가능.

### ② SDN (Software Defined Networking)

제어 계층과 전달 계층 분리. 중앙 제어 기반 네트워크 운영.

### ③ Overlay Network

기존 물리망 위에 논리망 생성. 예: VXLAN, GRE

### ④ VPC Isolation

테넌트별 네트워크 격리.

### ⑤ Routing Table

논리 경로 제어.

### ⑥ Virtual Firewall

소프트웨어 기반 패킷 필터링.

### ⑦ NAT

사설망 ↔ 공인망 주소 변환.

### ⑧ Service Discovery

동적 서비스 연결. 예: DNS, internal registry

### ⑨ Container Networking

컨테이너 간 네트워크 연결. 예: CNI, overlay fabric

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

**네트워크 인터페이스**

```bash
ip addr
ip link
```

**라우팅**

```bash
ip route
route -n
```

**연결 상태**

```bash
ss -ant
netstat -ant
```

**방화벽**

```bash
iptables -L
nft list ruleset
```

**네트워크 캡처**

```bash
tcpdump
wireshark
```

### Kubernetes

**Pod Network**

```bash
kubectl get pods -o wide
```

**Service**

```bash
kubectl get svc
```

**Ingress**

```bash
kubectl get ingress
```

**CNI 상태** (예: Calico, Cilium, Flannel)

**DNS 상태**

```bash
kubectl get pods -n kube-system   # CoreDNS 확인
```

### Runtime / Cloud

관측 대상: VPC flow logs, SDN telemetry, packet drops, latency, east-west traffic

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*