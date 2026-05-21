# TCP/IP 4계층 참조 모델 (TCP/IP 4-Layer Reference Model)

> 정독: 0회

## 1. 이 기술이 무엇인가

TCP/IP 4계층 참조 모델은:

> 인터넷 통신 기능을 4개의 논리 계층으로 분리하여, 프로토콜 역할과 데이터 흐름을 체계화한 **인터넷 표준 네트워크 아키텍처 모델**

현대 인터넷의 핵심 구조입니다.

### 핵심 목적

다음 문제를 계층별로 분리합니다.

- 애플리케이션 통신
- 데이터 전달 신뢰성
- 글로벌 라우팅
- 물리 네트워크 송수신

### 계층 구조

| 계층 | 이름 | 역할 | 주요 프로토콜 |
|------|------|------|--------------|
| 4계층 | Application Layer | 사용자 애플리케이션 통신 처리 | HTTP, DNS, SMTP, TLS |
| 3계층 | Transport Layer | 종단 간 데이터 전달 제어 | TCP, UDP |
| 2계층 | Internet Layer | IP 기반 글로벌 라우팅 | IP, ICMP |
| 1계층 | Network Access Layer | 실제 물리 네트워크 송수신 | Ethernet, Wi-Fi |

### 핵심 특징

> 각 계층은 자신의 역할만 수행하며, 상하 계층과 표준 인터페이스로 연결된다.

<details>
<summary>Deep Dive</summary></br>

IP Data Communication(IP 데이터 통신) [[M]](../../100-deep-dive/micro-foundations/ip-data-communication.md)  
Abstraction Layer(추상화 계층) [[M]](../../100-deep-dive/micro-foundations/abstraction-layer.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

현대 모든 네트워크 시스템에서 등장합니다.

- **사용자 단말**: browser, smartphone, desktop
- **서버 시스템**: web server, API server, DB server
- **클라우드 환경**: VM, VPC, Load Balancer
- **Kubernetes**: Pod networking, Service routing, Ingress traffic
- **데이터센터 네트워크**: spine-leaf, overlay networking
- **SaaS 플랫폼**: web application, streaming service, cloud storage
- **ISP / Backbone**: 인터넷 전체가 TCP/IP 구조 기반

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **Network 자원**입니다.

| 자원 | 영향도 | 예시 |
|------|--------|------|
| Network | 매우 큼 | routing, packet forwarding, congestion, retransmission |
| CPU | 중요 | TCP stack processing, encryption, checksum, packet parsing |
| Memory | 중요 | socket buffer, routing cache, conntrack table |
| Disk | 간접적 | packet logging, flow storage, pcap dump |

---

## 4. 왜 중요한가

인터넷 자체의 기반 구조이기 때문입니다.

- **역할 분리**: 복잡한 네트워크 문제를 계층별 분리 가능
- **상호 운용성**: OS·하드웨어·CSP가 달라도 통신 가능
- **독립적 진화 가능**: 특정 계층만 교체 가능 (예: Ethernet → Wi-Fi, IPv4 → IPv6 — 상위 애플리케이션은 유지 가능)
- **장애 분석 가능**: 계층별 원인 추적 가능
- **인터넷 확장 가능**: 수십억 장치 규모 확장 가능
- **클라우드·K8s 기반 제공**: 현대 클라우드 인프라 핵심 구조

---

## 5. 실제 장애와 어떤 관련이 있는가

실무에서 매우 중요합니다.

### 대표 장애 유형

| 계층 | 장애 예시 |
|------|----------|
| Application Layer | DNS failure, HTTP 500 |
| Transport Layer | TCP retransmission, connection timeout |
| Internet Layer | routing failure, packet loss |
| Network Access Layer | NIC failure, ARP issue, switch port down |

### 계층 분리의 핵심 가치

> 장애를 어느 계층에서 발생했는지 구조적으로 분리 분석 가능하게 만든다.

---

## 6. 핵심 메커니즘

핵심 메커니즘은 10개입니다.

| # | 메커니즘 | 설명 |
|---|----------|------|
| ① | Layered Architecture | 역할 기반 계층 분리 |
| ② | Encapsulation | 계층별 헤더 추가 |
| ③ | Decapsulation | 수신 측 헤더 제거 |
| ④ | End-to-End Communication | 종단 간 데이터 전달 |
| ⑤ | Port Multiplexing | 프로세스 단위 통신 분리 |
| ⑥ | IP Routing | 글로벌 경로 탐색 |
| ⑦ | Ethernet Delivery | 인접 노드 전달 |
| ⑧ | TCP Reliability | 재전송·순서 보장 |
| ⑨ | UDP Fast Delivery | 고속 비연결 전송 |
| ⑩ | Hardware Abstraction | 상위 계층과 물리망 분리 |

### 데이터 흐름

```
Application Data
    ↓
TCP/UDP Segment
    ↓
IP Packet
    ↓
Ethernet Frame
    ↓
Physical Signal
```

### 캡슐화 구조

```
[ Ethernet Header ]
    └── [ IP Header ]
            └── [ TCP/UDP Header ]
                    └── [ Application Data ]
```

각 계층은 **자신 역할만 수행**하고, 상위 데이터를 보호하며, 하위에 전달을 위임하는 구조로 동작합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 인터페이스 확인
ip addr
ip link

# 라우팅 확인
ip route

# TCP 상태
ss -ant

# 패킷 분석
tcpdump

# DNS 확인
dig

# 네트워크 통계
sar -n DEV
```

### Kubernetes

```bash
# Pod 네트워크
kubectl get pods -o wide

# Service 확인
kubectl get svc

# Ingress 확인
kubectl get ingress
```

- **CNI 상태**: Calico, Cilium, Flannel
- **kube-proxy**: iptables/ipvs 기반 트래픽 처리

### 클라우드 환경

- VPC
- Route Table
- NAT Gateway
- Security Group

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*