# 인터넷워크 (Internetwork)

> 정독: 0회

## 1. 이 기술이 무엇인가

인터넷워크(Internetwork)는:

> **서로 독립적으로 운영되는 여러 네트워크를 라우터와 L3 프로토콜(IP)로 연결한 네트워크 간 통합 구조**

핵심은 **Network + Network + Network** 를 하나의 통신 체계로 연결하는 것입니다.

### 핵심 개념

LAN 하나는 독립 네트워크입니다.

예: 회사 내부망, 클라우드 VPC, 데이터센터 네트워크, 가정용 공유기 네트워크

인터넷워크는 이런 개별 네트워크들을 **L3 Routing**으로 연결합니다.

### Internet과의 관계

Internet은 **전 세계 규모의 거대한 Internetwork**입니다.

> Internetwork 개념의 최대 규모 구현체가 Internet입니다.

---

## 2. 시스템 어디에서 등장하는가

인터넷워크는 현대 인프라 거의 모든 곳에 존재합니다.

### 대표 등장 영역

Internet, Cloud Network, AWS VPC Peering, Kubernetes Multi-Cluster, VPN, Hybrid Cloud, MPLS WAN, SD-WAN, Enterprise Network

### 계층 위치

```
L7 Application
    ↓
L4 TCP/UDP
    ↓
L3 Internetworking  ← 현재 위치
    ↓
L2 Ethernet
    ↓
L1 Physical
```

### 핵심 장비

인터넷워크는 주로 Router, L3 Switch, Gateway, Firewall, NAT Device를 통해 구성됩니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **Network 자원**입니다.

특히 Routing, Latency, Throughput, Congestion, Packet Loss에 직접 영향을 줍니다.

### CPU 영향도 큼

다음이 모두 CPU 연산이기 때문입니다.

- Route Lookup
- NAT
- Packet Filtering
- Encapsulation
- Tunnel Processing

Router, Firewall, Kubernetes Node, Cloud Gateway에서 CPU 부하와 밀접합니다.

---

## 4. 왜 중요한가

인터넷워크가 없다면 **네트워크 간 통신 자체가 불가능**합니다.

### 핵심 의미

| 계층 | 역할 |
|------|------|
| L2 (Ethernet) | 동일 네트워크 내부 전달만 가능 |
| Internetwork | 서로 다른 네트워크 연결 가능 |

### 가능해지는 것

글로벌 인터넷, 클라우드 연결, VPN, 데이터센터 연동, 멀티 리전 통신, Kubernetes Cross-Cluster, Hybrid Infrastructure

---

## 5. 실제 장애와 어떤 관련이 있는가

대규모 네트워크 장애 상당수가 인터넷워크 구조 문제입니다.

### Routing Failure

라우팅 정보 오류.

증상: 특정 네트워크 unreachable, 일부 서비스만 연결 실패

### Asymmetric Routing

왕복 경로 불일치.

증상: Firewall 세션 꼬임, intermittent timeout

### NAT 문제

주소 변환 실패.

증상: 외부 연결 실패, 세션 유실

### BGP 장애

인터넷 백본 경로 오류.

증상: 글로벌 서비스 장애, 특정 국가 접속 불가

### MTU 문제

네트워크 간 MTU 불일치.

증상: TLS handshake 실패, VPN 불안정, fragmentation 증가

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### ① IP Addressing

인터넷워크의 기본 식별 체계. 모든 네트워크 장비는 **논리 주소(IP)** 기반으로 식별됩니다.

### ② Routing

라우터는 Destination IP를 기반으로 **다음 경로(Next Hop)**를 결정합니다.

### ③ Subnetting

큰 네트워크를 여러 개로 분리합니다.

```
10.0.0.0/8
  → 10.1.0.0/16
  → 10.2.0.0/16
```

### ④ Gateway

서로 다른 네트워크 사이 연결 지점. 기본 게이트웨이는 외부 네트워크로 나가는 출구입니다.

### ⑤ Encapsulation

네트워크 간 이동 시 IP Packet이 L2 프레임 안에 캡슐화됩니다.

각 네트워크 구간마다 **L2 Header는 바뀌고, L3 IP는 유지**됩니다.

### ⑥ Route Table

라우터 핵심 데이터 구조. `Destination Network → Next Hop` 매핑을 저장합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

**라우팅 테이블 확인 (가장 중요)**

```bash
ip route
```

**게이트웨이 확인**

```bash
ip route | grep default
```

**경로 추적 (라우터 홉 확인)**

```bash
traceroute <host>
```

**패킷 관찰 (IP Packet 직접 확인)**

```bash
tcpdump
```

**인터페이스 IP 확인**

```bash
ip addr
```

**소켓 상태 확인**

```bash
ss
ping <host>
```

### Kubernetes

K8s도 본질적으로 **거대한 Internetwork 구조**입니다.

**중요 요소:** Pod CIDR, Service CIDR, Overlay Network, VXLAN, BGP, CNI Routing, Multi-Cluster Networking

Pod 간 통신 실패의 실제 원인이 Route 누락, Overlay routing 오류, MTU mismatch, CNI 문제인 경우가 많습니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*