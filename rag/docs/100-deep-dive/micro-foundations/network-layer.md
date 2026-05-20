# 네트워크 계층 (Network Layer)

> 정독: 0회

## 1. 이 기술이 무엇인가

네트워크 계층(Network Layer)은:

> **서로 다른 네트워크 사이에서 데이터를 목적지까지 전달하기 위한 L3(Network Layer) 통신 계층**

핵심 기능은 **IP 주소 기반 식별**, **경로 선택(Routing)**, **패킷 전달(Packet Forwarding)** 입니다.

### 핵심 역할

| 계층 | 역할 |
|------|------|
| L2 (Data Link Layer) | 같은 LAN 내부 전달 |
| L3 (Network Layer) | 다른 네트워크까지 포함한 종단 간 전달 |

> L2 = 인접 노드 전달
> L3 = 전체 네트워크 경로 전달

<details>
<summary>Deep Dive</summary></br>

Internetwork(인터넷워크) [[M]](../../100-deep-dive/micro-foundations/internetwork.md)  
Data(데이터) [[M]](../../100-deep-dive/micro-foundations/data.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

네트워크 계층은 거의 모든 인터넷 통신에서 등장합니다.

### OSI 위치

```
Application
    ↓
Transport (TCP/UDP)
    ↓
Network Layer (IP)  ← 현재 위치
    ↓
Data Link Layer (Ethernet)
    ↓
Physical Layer
```

### 실제 시스템 위치

Router, L3 Switch, Cloud Network, Kubernetes CNI, VPN, Internet Backbone, SDN, Firewall, NAT Gateway

### 데이터 흐름

```
TCP Segment → IP Packet → Ethernet Frame → Physical Signal
```

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **Network 자원**입니다.

특히 대역폭, 라우팅 경로, 패킷 손실, 지연(latency), 혼잡(congestion)에 영향을 줍니다.

### CPU 영향도 큼

다음이 모두 CPU 연산이기 때문입니다.

- Routing Lookup
- NAT
- Firewall Rule Match
- Packet Filtering
- Encapsulation

Router, Kubernetes Node, Firewall Appliance에서는 CPU 사용량과 밀접합니다.

---

## 4. 왜 중요한가

네트워크 계층은 **인터넷 전체 연결을 가능하게 만드는 핵심 계층**입니다.

L2만 있으면 같은 LAN 내부 통신만 가능합니다. L3는 서로 다른 네트워크 간 전달을 가능하게 만듭니다.

### ① 전역 주소 체계

IP 주소를 제공합니다. (IPv4, IPv6)

### ② 경로 선택

라우터가 목적지까지의 다음 경로를 결정합니다.

### ③ 네트워크 분리

사내망, 인터넷, 클라우드, 데이터센터를 논리적으로 분리 가능합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 대규모 장애 상당수가 L3 문제입니다.

### Routing Loop

패킷이 라우터 사이를 무한 순환.

증상: latency 증가, packet loss, TTL exceeded

### Blackhole Routing

특정 경로에서 패킷 폐기.

증상: 특정 서비스만 연결 실패

### MTU Mismatch

패킷 크기 불일치.

증상: TLS handshake 실패, VPN 불안정, intermittent timeout

### Packet Fragmentation 증가

과도한 조각화 발생.

증상: CPU 증가, throughput 감소, retransmission 증가

### NAT 문제

주소 변환 실패.

증상: outbound connection 실패, 세션 유실

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### ① IP Addressing

네트워크 계층은 출발지 IP와 목적지 IP를 사용합니다. IP는 **논리 주소(Logical Address)**로, MAC 주소와 다릅니다.

### ② Routing

라우터는 Destination IP를 보고 **다음 홉(Next Hop)**을 결정합니다.

### ③ Packet Forwarding

```
패킷 수신 → Routing Table Lookup → 다음 인터페이스로 전달
```

### ④ TTL (Time To Live)

라우팅 루프 방지 장치. 라우터 통과 시 `TTL -= 1`, 0이 되면 폐기합니다.

### ⑤ Fragmentation

MTU보다 큰 패킷은 분할 가능. 현대 네트워크에서는 가능하면 Fragmentation을 회피합니다.

### ⑥ Encapsulation

```
TCP Segment → IP Packet → Ethernet Frame
```

L3 패킷은 L2 안에 들어갑니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

**라우팅 테이블 확인 (가장 중요)**

```bash
ip route
```

**인터페이스 IP 확인**

```bash
ip addr
```

**패킷 확인 (IP Header 직접 관찰)**

```bash
tcpdump -n
```

**경로 추적 (라우터 홉 확인)**

```bash
traceroute <host>
ping <host>
```

**소켓 상태 확인**

```bash
ss
```

### Kubernetes

K8s 네트워크의 핵심도 결국 **Pod IP Routing**입니다.

**중요 요소:** Pod CIDR, Service CIDR, Overlay Network, VXLAN, BGP, kube-proxy, CNI Routing

Pod 간 통신 실패의 실제 원인이 Route 누락, Overlay encapsulation 오류, MTU mismatch, CNI routing failure인 경우가 많습니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*