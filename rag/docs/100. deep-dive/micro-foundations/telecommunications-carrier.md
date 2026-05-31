# 통신 사업자 (Telecommunications Carrier / Telco)

> 정독: 0회

## 1. 이 기술이 무엇인가

통신 사업자(Telco)는:

> **국가·대륙 규모의 유무선 통신 인프라를 직접 구축·운영하며, 음성·데이터·인터넷 트래픽을 전달하는 기간 통신 네트워크 운영 조직**

대표적으로 KT, SK Telecom, LG Uplus, AT&T, Verizon 등이 존재합니다.

### 핵심 특징

Telco는 단순 인터넷 판매 조직이 아닙니다. 광케이블, 이동통신 기지국, 주파수, 백본망, 코어 라우터, 교환기, 가입자망, 국제망을 직접 운영합니다.

> **인터넷과 이동통신의 물리적 기반 자체를 운영**하는 조직입니다.

---

## 2. 시스템 어디에서 등장하는가

거의 모든 인터넷·모바일 통신 시스템에서 등장합니다.

### 가입자망

FTTH, xDSL, LTE, 5G

### 데이터센터 연결

IDC 회선, 전용선, MPLS, Metro Ethernet

### 글로벌 인터넷 연결

국제망, 해저 광케이블, ISP 상호 연결

### 모바일 네트워크

기지국, 코어망, 이동성 제어

### 기업 네트워크

WAN, VPN, Carrier Ethernet

### 클라우드 연결

Dedicated Interconnect, Direct Connect, ExpressRoute

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 핵심은 **Network 자원**입니다.

특히 Bandwidth, Latency, Packet Loss, Routing Capacity에 직접 영향을 줍니다.

| 자원 | 영향 영역 |
|------|-----------|
| Network | 광회선, BGP, MPLS, RAN, Backbone |
| CPU | Routing Calculation, Packet Processing, Signaling |
| Memory | BGP Table, Session State, Flow Cache |
| Disk | Logging, Billing, CDR, Monitoring |

---

## 4. 왜 중요한가

Telco는 **인터넷워크와 이동통신의 실제 물리적 전달 기반**입니다.

### 인터넷 연결 제공

사용자는 Telco 없이는 공인 IP 획득과 인터넷 진입이 불가능합니다.

### 글로벌 트래픽 전달

대륙 간 데이터 이동을 지원합니다.

### 이동통신 제공

LTE/5G 기반 모바일 인터넷을 제공합니다.

### 국가 기간망 운영

금융·정부·클라우드·CDN 트래픽 전달의 기반입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

Telco 장애는 **대규모 인터넷 장애**로 이어질 수 있습니다.

### Backbone 장애

광케이블 절단·코어망 문제.

증상: 지역 인터넷 마비, 국제망 지연

### BGP 장애

잘못된 Route 광고.

증상: 특정 서비스 unreachable, 대규모 routing blackhole

### DNS 장애

Carrier DNS 장애.

증상: 도메인 접속 실패

### RAN 장애

기지국 장애.

증상: LTE/5G 불가, 모바일 데이터 끊김

### DDoS

대규모 공격.

증상: 회선 포화, packet loss 증가

### MPLS/VPN 장애

기업 전용망 문제.

증상: IDC 연결 실패, 금융망 장애

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### ① Access Network

가입자 연결 구간.

예: FTTH, LTE, 5G

### ② Backbone Network

대규모 고속 전달망.

예: 광케이블, Core Router, DWDM

### ③ BGP 기반 AS 연결

Telco는 대규모 AS를 운영하며 BGP로 다른 ISP, CSP, CDN과 연결합니다.

### ④ Routing Policy

트래픽 흐름 정책을 제어합니다.

예: Peering, Transit, Traffic Engineering

### ⑤ MPLS

Carrier 핵심 기술. 고속 forwarding, VPN 분리, QoS 제공이 특징입니다.

### ⑥ RAN + Core 구조

모바일망 핵심 구조.

구성: 기지국, EPC/5GC, Subscriber Control

### ⑦ SLA 기반 운영

Carrier는 엄격한 SLA를 운영합니다.

핵심 지표: Availability, Latency, Packet Loss, Jitter

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

**네트워크 상태 확인**

```bash
ip route
traceroute <host>
mtr <host>
ping <host>
ss
ethtool
```

**DNS 확인**

```bash
dig <domain>
nslookup <domain>
```

**MTU 문제 확인**

```bash
ip link
ping -M do <host>
```

### Runtime

Runtime은 Carrier Network의 영향을 직접 받습니다.

예: TCP Retransmission, Connection Timeout, RTT 증가

### Kubernetes

**영향 영역:** Ingress, Service Mesh, External LB, Cross Region

**관측 현상:** pod timeout, packet loss, node unreachable, DNS latency

### Cloud 환경

CSP와 Telco 연결이 중요합니다.

예: Direct Connect, ExpressRoute, Interconnect

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*