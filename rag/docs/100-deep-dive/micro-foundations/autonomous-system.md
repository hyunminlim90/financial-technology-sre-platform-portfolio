# 자율 시스템 (AS - Autonomous System)

> 정독: 0회

## 1. 이 기술이 무엇인가

자율 시스템(AS)은:

> **하나의 관리 조직이 독자적인 라우팅 정책으로 운영하는 대규모 네트워크 집합**

인터넷은 **수많은 AS들이 서로 연결된 구조** 로 이루어집니다.

### 핵심 구성 요소

AS는 일반적으로 ISP, 클라우드 사업자, 통신사, 대형 데이터센터, 글로벌 서비스 기업이 운영합니다.

### 핵심 식별자

각 AS는 **ASN (Autonomous System Number)**을 가집니다.

| ASN | 조직 |
|-----|------|
| AS15169 | Google |
| AS16509 | AWS |
| AS9644 | KT |

---

## 2. 시스템 어디에서 등장하는가

AS는 인터넷 백본과 대규모 네트워크에서 등장합니다.

### 대표 등장 영역

ISP, Internet Backbone, Cloud Provider, CDN, IX(Internet Exchange), Global Routing Infrastructure

### 네트워크 구조 위치

```
Internet
    ↓
Autonomous Systems
    ↓
IP Routing
    ↓
LAN/Subnet
```

### 핵심 장비

AS는 주로 Core Router, Edge Router, Border Router, Route Reflector로 구성됩니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **Network 자원**입니다.

특히 Internet Reachability, Global Routing, Traffic Engineering, Peering, Backbone Bandwidth에 직접 영향을 줍니다.

### CPU/Memory 영향도 큼

BGP 라우팅은 Route Table 계산, Path Selection, Prefix 관리를 수행합니다.

대규모 AS에서는 수십만~수백만 Route Prefix를 처리하므로 Router의 CPU, TCAM, Memory 사용량이 매우 중요합니다.

---

## 4. 왜 중요한가

AS는 **인터넷 전체의 라우팅 기본 단위**입니다.

인터넷은 단일 네트워크가 아니라 **독립 운영 네트워크들의 연결 구조**입니다.

AS가 존재하기 때문에 글로벌 인터넷 확장, 조직별 독립 운영, 정책 기반 라우팅, 인터넷 사업자 간 연결이 가능합니다.

### AS가 없으면 발생하는 문제

- 글로벌 라우팅 불가능
- 인터넷 규모 확장 어려움
- 정책 기반 제어 불가능
- ISP 간 독립성 상실

---

## 5. 실제 장애와 어떤 관련이 있는가

인터넷 대규모 장애 상당수가 AS/BGP 문제입니다.

### BGP Route Leak

잘못된 경로 광고.

증상: 특정 국가 접속 불가, 글로벌 우회 경로 발생, latency 폭증

### BGP Hijacking

악의적 Prefix 탈취.

증상: 트래픽 탈취, MITM 가능성, 서비스 장애

### Peering Failure

AS 간 연결 실패.

증상: 특정 ISP만 접속 불가, 해외 연결 장애

### Prefix Misconfiguration

잘못된 Route Announcement.

증상: 일부 네트워크 unreachable, blackhole routing

### DDoS

대규모 AS 트래픽 과부하.

증상: 국제망 congestion, packet loss 증가

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### ① ASN (Autonomous System Number)

AS 식별 번호. 전 세계 유일합니다.

예: AS15169, AS16509, AS9644

### ② BGP (Border Gateway Protocol)

AS 간 라우팅 프로토콜. BGP는 **어떤 AS 경로를 통해 목적지까지 갈지 결정**합니다.

### ③ Prefix Advertisement

AS는 자신이 소유한 IP Prefix를 광고합니다.

예: `203.0.113.0/24`

### ④ AS Path

패킷이 거치는 AS 경로 정보.

```
AS64500 → AS3356 → AS15169
```

### ⑤ Peering / Transit

AS 간 연결 방식.

- **Peering**: 상호 직접 연결
- **Transit**: 상위 ISP를 통한 연결

### ⑥ Policy Routing

AS는 단순 최단경로만 사용하지 않습니다. 비용, 계약, 트래픽 제어, 지역 정책을 기반으로 경로를 선택합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

일반 서버에서는 직접 AS를 다루는 경우는 적지만, 다음 명령어로 AS 경로를 확인할 수 있습니다.

**BGP 정보 확인**

```bash
whois <IP>
```

**경로 추적 (AS 경로 분석)**

```bash
traceroute <host>
mtr <host>
```

### Kubernetes / Cloud

Multi-Region, Hybrid Cloud, Bare Metal Kubernetes, BGP CNI 환경에서 특히 중요합니다.

**Kubernetes에서 실제 사용 사례:** Calico BGP, MetalLB BGP, Data Center Fabric

Node 간 외부 라우팅 실패의 실제 원인이 BGP session down, Route advertisement 실패, Prefix 누락인 경우가 있습니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*