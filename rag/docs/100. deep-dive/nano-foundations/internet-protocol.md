# 인터넷 프로토콜 (Internet Protocol, IP)

> 정독: 0회

## 1. 이 기술이 무엇인가

인터넷 프로토콜(IP)은:

> **서로 다른 네트워크 사이에서 데이터를 목적지까지 전달하기 위한 L3(Network Layer) 표준 프로토콜**

IP의 핵심 역할은 **Addressing(주소 지정)**, **Routing 대상 제공**, **Packet 전달** 입니다.

### 핵심 처리 단위

IP가 다루는 데이터 단위는 **IP Packet**입니다.

IP Packet 내부에는 Source IP, Destination IP, TTL, Protocol 정보 등이 포함됩니다.

---

## 2. 시스템 어디에서 등장하는가

IP는 거의 모든 현대 네트워크 시스템의 중심입니다.

### 등장 위치

인터넷, 데이터센터, 클라우드, Kubernetes Cluster, VPN, SDN, WAN, 모바일 네트워크, 가정용 네트워크

### OSI 기준 위치

```
L7 Application
    ↓
L4 TCP/UDP
    ↓
L3 IP  ← 현재 위치
    ↓
L2 Ethernet
    ↓
L1 Physical
```

### 실제 네트워크 흐름

```
Application Data → TCP/UDP → IP Packet → Ethernet Frame → Physical Signal
```

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **Network 자원**입니다.

특히 Routing, Packet Forwarding, MTU, Fragmentation, Latency, Packet Loss에 영향을 줍니다.

### CPU 영향

IP 처리 시 Routing lookup, Checksum 처리, NAT, Firewall, Encapsulation 등을 수행하므로 고성능 네트워크에서는 CPU 부하가 중요합니다.

### Memory 영향

Routing Table, Conntrack, ARP Cache, Neighbor Table 등의 데이터 저장이 필요합니다.

---

## 4. 왜 중요한가

IP는 **전 세계 네트워크를 연결하는 공통 규칙**입니다.

IP가 없다면 글로벌 통신, 네트워크 간 연결, 인터넷, 라우팅이 모두 불가능합니다.

### 핵심 이유

| 계층 | 역할 |
|------|------|
| L2 (Ethernet) | 동일 네트워크 내부만 처리 가능 |
| L3 (IP) | 다른 네트워크까지 전달 가능 |

---

## 5. 실제 장애와 어떤 관련이 있는가

네트워크 장애 상당수가 IP 레벨 문제입니다.

### IP Routing 오류

증상: 특정 대역 unreachable, timeout, asymmetric routing

### MTU 문제

패킷 크기 초과.

증상: 특정 서비스만 느림, TLS handshake 실패, packet fragmentation 증가

### Packet Loss

증상: latency 증가, retransmission 증가, timeout 발생

### TTL Exceeded

Routing Loop 발생 시 등장.

증상: traceroute 중간 반복, ICMP TTL exceeded

### CIDR 오설정

잘못된 subnet mask.

증상: 일부 IP 통신 실패, routing 충돌

### NAT 문제

증상: outbound 실패, session timeout, connection exhaustion

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### ① IP Addressing

장치 식별용 논리 주소.

예: `192.168.0.10`, `10.0.0.5` (IPv4) / `2001:db8::1` (IPv6)

### ② Packetization

데이터를 IP Packet 형태로 포장합니다.

### ③ Routing

목적지 IP 기준 경로 선택. 라우터가 수행합니다.

### ④ TTL (Time To Live)

라우터 통과 시 `TTL -= 1`, 0이 되면 폐기합니다. Routing Loop 방지 목적입니다.

### ⑤ Fragmentation

패킷이 MTU보다 크면 분할 가능. 성능 저하 및 loss 증가를 유발할 수 있어 현대 시스템은 **PMTU Discovery**를 선호합니다.

### ⑥ Connectionless

IP 자체는 연결 상태와 세션을 저장하지 않습니다. **Best Effort Delivery** 방식입니다.

### ⑦ Unreliable Delivery

IP는 순서 보장, 재전송, 무결성 보장을 하지 않습니다. 신뢰성은 TCP가 담당합니다.

### ⑧ Encapsulation

```
L3 IP Packet  →  L2 Ethernet Frame 내부에 캡슐화되어 전달
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

**IP 확인**

```bash
ip addr
hostname -I
```

**Routing Table 확인**

```bash
ip route
route -n
```

**Neighbor Table 확인**

```bash
ip neigh
```

**Packet 확인**

```bash
tcpdump
wireshark
```

**MTU 확인**

```bash
ip link
```

### Kubernetes

K8s 네트워크도 IP 기반입니다.

**핵심 요소:** Pod IP, Service IP, Cluster CIDR, Node CIDR

Pod 간 통신 시 IP Routing, VXLAN, Overlay Network를 사용합니다.

**대표 장애:**

- Pod-to-Pod 통신 실패 → Route 누락, CNI 문제, MTU mismatch
- Service 접근 실패 → kube-proxy 문제, iptables/IPVS 오류
- Cross-node 장애 → Overlay encapsulation 실패, BGP session 문제

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*