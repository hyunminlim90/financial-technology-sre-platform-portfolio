# IP 데이터 통신 (IP Data Communication)

> 정독: 0회

## 1. 이 기술이 무엇인가

IP 데이터 통신은:

> 인터넷 프로토콜(IP)을 기반으로 데이터를 패킷 단위로 분할하여 네트워크를 통해 목적지까지 전달하는 통신 방식

### 핵심 3가지

- IP 주소 기반 식별
- 패킷 기반 전송
- 라우터 기반 경로 전달

### 데이터 단위

IP 데이터 통신의 기본 전송 단위는 **IP Packet (IP Datagram)**입니다.

### 핵심 특징

- packet switching
- connectionless delivery
- best-effort forwarding

### 프로토콜 위치

인터넷 프로토콜 스위트 기준 **Internet Layer (L3)**에 위치합니다.

<details>
<summary>Deep Dive</summary></br>

Internet Protocol(인터넷 프로토콜) [[M]](../../100-deep-dive/micro-foundations/internet-protocol.md)  
Data(데이터) [[M]](../../100-deep-dive/micro-foundations/data.md)  
IP Packet(IP 패킷) [[M]](../../100-deep-dive/micro-foundations/ip-packet.md)  
Network(네트워크) [[M]](../../100-deep-dive/micro-foundations/network.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

사실상 현대 모든 네트워크 시스템에서 등장합니다.

### 사용자 단말

browser, smartphone, desktop

### 서버 시스템

web server, API server, database node

### 클라우드 인프라

VM communication, VPC routing, NAT gateway

### Kubernetes

Pod networking, Service traffic, Cluster routing

### 데이터센터 네트워크

spine-leaf routing, overlay tunnel

### 인터넷 백본망

ISP routing, BGP transit, AS interconnection

### CDN / Edge Network

전부 IP 데이터 통신 기반.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Network 자원**입니다.

| 자원 | 영향 | 주요 지표 |
|---|---|---|
| **Network** | 매우 큼 | bandwidth, latency, packet loss, routing efficiency |
| **CPU** | 중요 | packet processing, routing lookup, checksum verification, NAT translation |
| **Memory** | 중요 | socket buffer, routing table, conntrack table |
| **Disk** | 간접적 | packet capture, traffic logging, flow analytics |

---

## 4. 왜 중요한가

현대 인터넷의 핵심 데이터 전달 메커니즘입니다.

### 글로벌 통신 가능

전 세계 시스템 연결 가능.

### 확장성

수십억 장치 규모 확장 가능.

### 네트워크 독립성

서로 다른 네트워크 간 연결 가능.

### 패킷 기반 효율성

대역폭 공유 가능.

### 장애 허용성

경로 우회 가능.

### 클라우드 기반 구조 지원

VM·Container·SaaS 전부 기반 제공.

> IP 데이터 통신은 현대 인터넷의 표준 데이터 전달 메커니즘이다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### Packet Loss

패킷 유실. → timeout, retransmission 증가

### High Latency

지연 시간 증가. → response delay, API degradation

### Routing Failure

라우팅 오류. → unreachable network

### MTU Mismatch

패킷 크기 불일치. → fragmentation, intermittent failure

### Congestion

트래픽 혼잡. → queue overflow, packet drop

### TTL Expired

라우팅 루프. → packet discard

### NAT Failure

주소 변환 오류. → external communication failure

### Asymmetric Routing

비대칭 경로. → firewall/session 문제

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

### ① Packetization

데이터를 패킷 단위로 분할.

### ② IP Addressing

출발지/목적지 식별.

### ③ Routing

최적 경로 선택.

### ④ Forwarding

다음 홉으로 전달.

### ⑤ TTL Control

무한 루프 방지.

### ⑥ Fragmentation

MTU 초과 시 분할.

### ⑦ Reassembly

수신 측 재조립.

### ⑧ Stateless Delivery

패킷 독립 처리.

### ⑨ Best-Effort Delivery

도착 보장 없음.

### ⑩ Layered Encapsulation

TCP/UDP 데이터 캡슐화.

### 패킷 구조 핵심

```
[ Ethernet Header ]
    └── [ IP Header ]
            └── [ TCP/UDP Header ]
                    └── [ Payload ]
```

### IP 헤더 핵심 정보

source IP, destination IP, TTL, protocol, checksum

### 핵심 동작 흐름

```
Application Data
    ↓
TCP/UDP
    ↓
IP Packet
    ↓
Ethernet Frame
    ↓
Physical Signal
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

**IP 주소 확인**

```bash
ip addr
```

**라우팅 테이블**

```bash
ip route
```

**패킷 흐름 분석**

```bash
tcpdump
```

**연결 상태**

```bash
ss -ant
```

**ICMP 테스트**

```bash
ping
```

**경로 추적**

```bash
traceroute
mtr
```

### Kubernetes

**Pod IP 확인**

```bash
kubectl get pods -o wide
```

**Service IP 확인**

```bash
kubectl get svc
```

**CNI 네트워크 확인** (예: Calico, Cilium, Flannel)

**kube-proxy / iptables** — Service routing 분석.

**Node-to-Node Traffic** — Overlay network 확인.

### 클라우드 환경

관측 대상: VPC Route Table, NAT Gateway, Internet Gateway, Transit Gateway

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*