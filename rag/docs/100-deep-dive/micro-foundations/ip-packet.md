# IP 패킷 (IP Packet)
## 1. IP 패킷이란 무엇인가

> 정독: 0회

IP 패킷은:

> **네트워크 계층(L3)에서 데이터를 목적지까지 전달하기 위해 사용하는 표준 데이터 운반 단위**

핵심 역할은 **"출발지와 목적지의 IP 주소를 기반으로 여러 네트워크를 거쳐 데이터를 전달"** 하는 것입니다.

### 가장 중요한 차이점

| 계층 | 역할 |
|------|------|
| Ethernet (L2) | 같은 LAN 내부 전달 |
| IP (L3) | 서로 다른 네트워크 사이 전달 |

<details>
<summary>Deep Dive</summary></br>



</details></br>

## 2. 시스템 어디에서 등장하는가

IP 패킷은 인터넷 통신 거의 모든 곳에 등장합니다.

### OSI 관점

```
Application
    ↓
TCP/UDP
    ↓
IP Packet  ← 현재 위치 (L3)
    ↓
Ethernet Frame
    ↓
Physical Signal
```

### 실제 시스템 흐름

```
Browser → TCP → IP → Ethernet → NIC → Switch → Router → Internet
```

### 핵심 위치

IP 패킷은 특히 다음에서 매우 중요합니다.

- Router
- L3 Switch
- Cloud Network
- Kubernetes Overlay Network
- VPN
- Internet Backbone

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **Network 자원**입니다.

특히 Routing, Forwarding, MTU, Fragmentation, Packet Loss에 직접 영향을 줍니다.

### CPU 영향도 큼

다음이 모두 CPU 연산이기 때문입니다.

- IP Header 처리
- Routing Lookup
- NAT
- Firewall
- Checksum 계산

Router, Kubernetes Node, Firewall Appliance에서는 CPU 사용량과 직결됩니다.

---

## 4. 왜 중요한가

IP 패킷은 **인터넷 자체를 가능하게 만든 핵심 전달 단위**입니다.

### Ethernet만 있으면 안 되는 이유

Ethernet은 같은 LAN 내부밖에 전달하지 못합니다. IP는 여러 네트워크를 넘어 최종 목적지까지 전달합니다.

### ① 전 세계 주소 체계

IP 주소를 제공합니다.

예: `192.168.x.x` / `10.x.x.x` / `8.8.8.8`

### ② 라우팅 가능

Router가 목적지 IP를 읽고 다음 경로를 결정합니다.

### ③ 네트워크 분리 가능

사내망, 클라우드, 인터넷, 데이터센터를 논리적으로 분리 가능합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 운영 장애 대부분이 IP 계층과 연결됩니다.

### Routing Loop

패킷이 라우터 사이에서 무한 순환.

증상: latency 증가, packet loss, TTL exceeded

### MTU 문제

패킷 크기가 너무 큼.

증상: 특정 서비스만 느림, TLS handshake 실패, VPN 불안정

### Packet Fragmentation

패킷이 여러 조각으로 분해됨.

증상: 성능 저하, CPU 증가, retransmission 증가

### Packet Drop

원인: Queue Overflow, Congestion, Firewall, ACL

증상: timeout, connection reset, slow response

### TTL Exceeded

라우팅 오류 시 흔함.

증상: traceroute 실패, network unreachable

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### ① IP Addressing

IP 패킷은 출발지 IP와 목적지 IP를 가집니다. Router는 이를 기반으로 경로를 결정합니다.

### ② Routing

Router는 Destination IP를 보고 **next hop**을 결정합니다.

### ③ TTL (Time To Live)

패킷 무한 루프 방지 장치. 라우터 하나를 지날 때마다 `TTL -= 1`, 0이 되면 폐기합니다.

### ④ Encapsulation

패킷은 다음 순서로 포장됩니다.

```
TCP Segment → IP Packet → Ethernet Frame
```

### ⑤ Fragmentation

MTU보다 크면 패킷 분할이 발생할 수 있습니다. 현대 네트워크에서는 가능하면 회피합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

**라우팅 테이블 확인**

```bash
ip route
```

**IP 주소 확인**

```bash
ip addr
```

**패킷 캡처 (IP Header 직접 확인)**

```bash
tcpdump -n
```

**TTL 확인**

```bash
ping <host>
traceroute <host>
```

**MTU 확인**

```bash
ip link
```

**소켓 상태 확인**

```bash
ss
```

### Kubernetes

K8s 네트워크의 핵심은 결국 **Pod IP Routing**입니다.

**실제로 중요해지는 요소:** Pod CIDR, Service CIDR, Overlay Routing, VXLAN, BGP, kube-proxy, CNI

Pod 간 통신 불가 문제의 실제 원인이 Route 누락, MTU mismatch, Overlay encapsulation, Node routing failure인 경우가 매우 많습니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*