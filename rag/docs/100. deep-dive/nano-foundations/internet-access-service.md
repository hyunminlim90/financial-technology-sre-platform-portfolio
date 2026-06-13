# 인터넷 접속 서비스 (Internet Access Service)

> 정독: 0회

## 1. 이 기술이 무엇인가

인터넷 접속 서비스(Internet Access Service)는:

> **사용자 단말이 ISP 네트워크를 통해 전 세계 인터넷워크에 연결될 수 있도록 제공되는 네트워크 연결 서비스**

서비스 형태: FTTH, xDSL, Cable, LTE/5G, Dedicated Line, Enterprise WAN

### 핵심 제공 요소

인터넷 접속 서비스는 일반적으로 물리 회선, 공인 IP, DNS 접근, ISP Routing, 인터넷 Transit을 함께 제공합니다.

---

## 2. 시스템 어디에서 등장하는가

인터넷 접속 서비스는 인터넷 연결이 필요한 모든 환경에서 등장합니다.

### 개인 환경

가정 인터넷, 모바일 데이터, Wi-Fi 인터넷

### 기업 환경

사무실 회선, IDC 회선, VPN 회선, 전용선

### 클라우드 연결

Direct Connect, ExpressRoute, Carrier Peering

### 데이터센터

Transit ISP, Multi ISP 구성, BGP Edge Connectivity

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접 영향받는 자원은 **Network 자원**입니다.

**핵심 영향 요소:** Bandwidth, Latency, Packet Loss, Jitter, Availability

### CPU 간접 영향

네트워크 병목 시 Retry 증가, 암호화 재시도, Session 처리 증가가 발생할 수 있습니다.

### Memory 간접 영향

대규모 연결 시 Socket Buffer, Connection Queue가 증가할 수 있습니다.

---

## 4. 왜 중요한가

인터넷 접속 서비스는 **인터넷워크 진입 자체를 가능하게 하는 기반 서비스**입니다.

### 공인 인터넷 연결 제공

Public IP, NAT, Routing을 가능하게 합니다.

### 글로벌 Reachability 확보

전 세계 AS와 통신이 가능합니다.

### 서비스 품질 결정

인터넷 품질 대부분이 ISP 품질, Access Network 품질, Backbone 품질에 영향을 받습니다.

특히 금융 시스템, 클라우드 서비스, CDN, SaaS, 실시간 스트리밍, 게임, VoIP에서 중요합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### ISP 회선 장애

증상: 인터넷 완전 단절, External API 실패, DNS 실패

### Packet Loss 증가

증상: TCP Retransmission, Throughput 감소, Timeout 증가

### DNS 장애

ISP DNS 문제 발생 시 도메인 이름 해석 실패가 발생합니다.

### NAT 문제

대규모 CGNAT 환경에서 Session 문제, Connection Exhaustion이 발생할 수 있습니다.

### Last Mile 문제

가입자망 문제. 예: 광신호 감쇠, 무선 간섭, ONU 장애

### 국제망 병목

특정 국가 연결 느려짐. 원인: Transit 부족, Backbone Congestion

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### ① Access Network 연결

사용자 단말 ↔ ISP Edge 연결.

기술 예시: FTTH, DOCSIS, LTE, 5G

### ② IP 주소 할당

ISP는 Public IP 또는 Private IP를 할당합니다.

방법: DHCP, PPPoE, Static Assignment

### ③ DNS Resolver 제공

도메인 이름 → IP 변환을 제공합니다.

### ④ ISP Routing

ISP 내부 Backbone을 통해 패킷을 전달합니다.

### ⑤ Transit / Peering

다른 ISP/AS와 BGP 기반으로 연결합니다.

### ⑥ NAT

가정 환경에서는 일반적으로 **Private IP → Public IP 변환**을 수행합니다. 보통 공유기 또는 ISP CGNAT에서 처리합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

**IP 확인**

```bash
ip addr
```

**Route 확인**

```bash
ip route
```

**DNS 확인**

```bash
cat /etc/resolv.conf
```

**외부 연결 확인**

```bash
ping <host>
curl <url>
traceroute <host>
mtr <host>
```

**NIC 상태 확인**

```bash
ethtool
```

### Kubernetes

K8s에서는 **Cluster External Connectivity** 영역에서 중요합니다.

**영향 영역:** Image Pull, External API, DNS, Ingress, Service Mesh Egress

장애 시 Pod 외부 통신 실패, DNS timeout, Registry Pull 실패, API latency 증가 등이 발생합니다.

### Cloud

Cloud 환경에서도 인터넷 접속 서비스가 존재합니다.

예: Internet Gateway, NAT Gateway, Transit Gateway, Dedicated Interconnect

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*