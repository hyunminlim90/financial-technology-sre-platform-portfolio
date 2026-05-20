# 인터넷 서비스 제공업체 (ISP, Internet Service Provider)

> 정독: 0회

## 1. 이 기술이 무엇인가

ISP는:

> **사용자와 인터넷워크를 연결하기 위해 네트워크 인프라와 인터넷 접속 서비스를 제공하는 조직**

ISP의 핵심 역할은 **인터넷 연결 제공**, **IP 주소 할당**, **패킷 전달**, **AS 운영**, **글로벌 네트워크 연결**입니다.

### 대표 ISP

대한민국 기준 예시: KT, SK Broadband, LG Uplus

<details>
<summary>Deep Dive</summary></br>

Internetwork(인터넷워크) [[M]](../../100-deep-dive/micro-foundations/internetwork.md)  
Backbone Network(백본망) [[M]](../../100-deep-dive/micro-foundations/backbone-network.md)  
Internet Access Service(인터넷 접속 서비스) [[M]](../../100-deep-dive/micro-foundations/internet-access-service.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

ISP는 인터넷워크 전체의 핵심 연결 지점입니다.

### 등장 위치

가정 인터넷, 기업 회선, 모바일 통신망, 데이터센터 Transit, Cloud Backbone, 국제망, 해저 광케이블망

### 네트워크 구조상 위치

```
User Network
    ↓
ISP Access Network
    ↓
ISP Core Network
    ↓
Internet Backbone
    ↓
Destination AS
```

### AS 관점

대부분 ISP는 **독립 Autonomous System(AS)**을 운영합니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **Network 자원**입니다.

특히 Internet Bandwidth, Latency, Backbone Throughput, Packet Loss, BGP Routing에 직접 영향을 줍니다.

### 물리 자원 영향

ISP는 실제로 광케이블, 해저케이블, Backbone Router, IX(Internet Exchange) 등을 운영합니다. 즉 **L1 ~ L3 전체 인프라**에 영향을 줍니다.

### CPU / Memory 영향

대규모 ISP Router는 BGP Full Table, MPLS, Traffic Engineering, ACL, NAT 처리가 필요합니다.

---

## 4. 왜 중요한가

ISP는 **사용자 네트워크와 글로벌 인터넷을 연결하는 핵심 사업자**입니다.

ISP가 없다면 인터넷 연결, 공인 IP 사용, 글로벌 라우팅, 외부 AS 접근이 모두 불가능합니다.

### 인터넷 구조 핵심

인터넷은 실제로 **수많은 ISP AS들의 상호 연결 구조**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

대규모 인터넷 장애 상당수가 ISP 또는 ISP 간 연결 문제입니다.

### ISP Backbone 장애

증상: 지역 전체 인터넷 장애, 특정 국가 unreachable

### BGP Route Leak

잘못된 경로 광고.

증상: 대규모 우회, latency 폭증, 서비스 unreachable

### DNS 장애

ISP DNS 문제 발생 시 웹사이트 접속 실패, name resolution 실패

### 해저케이블 장애

증상: 국제망 latency 증가, packet loss 증가

### ISP Congestion

백본 포화.

증상: Peak Time 속도 저하

### DDoS 공격

ISP Edge 장비 공격 가능.

증상: Transit Saturation, Packet Drop, Edge Router CPU 상승

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### ① Public IP Allocation

ISP는 사용자에게 **공인 IP 주소**를 할당합니다.

### ② Access Network

최종 사용자 연결망을 제공합니다.

예: FTTH, xDSL, Cable, LTE, 5G

### ③ Autonomous System 운영

ISP는 독립 AS를 운영합니다. AS 번호를 보유하고 자체 Routing Policy를 운영합니다.

### ④ BGP Peering

다른 ISP와 **BGP 기반 Route 교환**을 수행합니다.

### ⑤ Transit

상위 ISP를 통해 글로벌 인터넷에 연결합니다.

### ⑥ Backbone Network

대규모 고속 Core Network를 운영합니다. 보통 DWDM, MPLS, 100G/400G Ethernet을 사용합니다.

### ⑦ NAT / CGNAT

IPv4 부족 해결을 위해 **Carrier Grade NAT**을 사용할 수 있습니다.

### ⑧ Traffic Engineering

트래픽 흐름 최적화를 수행합니다.

예: MPLS, ECMP, QoS, Route Preference

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

**Public IP 확인**

```bash
curl ifconfig.me
```

**Default Gateway 확인**

```bash
ip route
```

**DNS 확인**

```bash
cat /etc/resolv.conf
```

**경로 추적 (중간 ISP Router 경유 확인)**

```bash
traceroute <host>
mtr <host>
```

**ASN 확인**

```bash
whois <IP>
```

### Kubernetes / Cloud

K8s 자체보다는 **Cluster 외부 연결 구간**에서 ISP 영향이 큽니다.

**주요 영향 영역:** Internet Egress, Cloud Transit, CDN Connectivity, Public API Access

특정 외부 API timeout의 실제 원인이 ISP Route 문제, 국제망 장애, BGP 경로 이상인 경우가 있습니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*