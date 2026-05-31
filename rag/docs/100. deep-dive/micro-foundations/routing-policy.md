# 라우팅 정책 (Routing Policy)

> 정독: 0회

## 1. 이 기술이 무엇인가

라우팅 정책(Routing Policy)은:

> **라우터와 AS가 어떤 경로(Route)를 선택하고, 어떤 경로를 거부하며, 어떤 트래픽을 우선 처리할지 결정하는 L3 제어 정책 체계**

단순히 "도달 가능한 경로"를 찾는 것이 아니라 비용, 보안, 성능, 사업 계약, 트래픽 엔지니어링까지 반영하여 경로를 제어합니다.

---

## 2. 시스템 어디에서 등장하는가

라우팅 정책은 주로 다음 환경에서 등장합니다.

### ISP Backbone

Transit 선택, Peering 제어, 국제망 우선순위

### 데이터센터 네트워크

Multi-Homing, ECMP, MPLS TE

### 클라우드 네트워크

VPC Routing, Hybrid Connectivity, Direct Connect

### 대규모 기업망

지사 연결, WAN 최적화, VPN 우선순위

특히 **BGP Border Router**에서 핵심적으로 동작합니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **Network 자원**입니다.

특히 Latency, Packet Loss, Bandwidth Utilization, Congestion, Transit Cost에 직접 영향을 줍니다.

### CPU 영향

BGP Decision Process, Route Filtering, Prefix Matching, ACL Evaluation 등이 Router CPU에 영향을 줍니다.

### Memory 영향

대규모 ISP Router는 **Full Routing Table** 저장이 필요합니다. 현재 인터넷 Full Table은 매우 큽니다.

---

## 4. 왜 중요한가

라우팅 정책이 없으면 인터넷은 **단순 최단 경로 기반 네트워크**가 됩니다. 실제 인터넷은 그렇지 않습니다.

### 현실 인터넷에서 중요한 요소

Transit 비용, AS 계약, 국가 정책, DDoS 방어, QoS, SLA, 우회 경로, 장애 회피

### 핵심 목적

라우팅 정책은 **인터넷 트래픽 흐름을 의도적으로 제어하기 위한 정책 엔진**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

인터넷 대규모 장애 상당수가 라우팅 정책 문제입니다.

### BGP Route Leak

잘못된 정책으로 비정상 경로 광고 발생.

증상: 대규모 우회, latency 급증, 특정 서비스 unreachable

### Prefix Hijacking

잘못된 Prefix 광고.

증상: 트래픽 탈취, MITM 가능성, 대규모 장애

### Blackhole 정책 오류

정상 트래픽 폐기.

증상: 특정 국가/ISP 접속 불가

### Routing Loop

정책 충돌.

증상: TTL 감소, 패킷 폐기, 네트워크 불안정

### Congestion 유발

비효율 경로 선택.

증상: Throughput 저하, Queue 증가, Packet Drop

### Asymmetric Routing

왕복 경로 불일치.

증상: Firewall Session 문제, NAT 문제, TCP 성능 저하

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심은 **BGP 속성(Attribute) 기반 경로 선택**입니다.

### ① Prefix Filtering

특정 Prefix 허용/차단.

예: `10.0.0.0/8 reject`

### ② Local Preference

AS 내부 우선 경로 지정. 값이 높을수록 우선합니다.

### ③ AS Path

AS 경유 길이 기반 선택. 짧은 경로를 선호할 수 있습니다.

### ④ MED (Multi Exit Discriminator)

다중 연결 시 어느 Entry를 사용할지 유도합니다.

### ⑤ Community

BGP Tag 기반 정책 전달.

예: Blackhole, Low Priority, Regional Route

### ⑥ Import Policy

외부 경로 수신 시 필터링합니다.

### ⑦ Export Policy

외부로 어떤 경로를 광고할지 제어합니다.

> **핵심 포인트:** 라우팅 정책은 "패킷 전달"이 아니라 **"경로 선택 기준"을 제어**하는 시스템입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

일반 Linux Host는 직접 BGP를 운영하는 경우는 적지만 Route Table, Policy Routing, ECMP 등 일부를 관측할 수 있습니다.

**Route 확인**

```bash
ip route
```

**Policy Routing 확인**

```bash
ip rule
```

**경로 우회 확인**

```bash
traceroute <host>
mtr <host>
```

### 데이터센터 / Router OS

실제 정책은 JunOS, Cisco IOS, FRRouting, BIRD 등에서 운영됩니다.

### Kubernetes

K8s 자체보다 **Cluster 외부 연결 영역**에서 중요합니다.

**등장 영역:** CNI BGP, MetalLB, Calico BGP, Multi Cluster Routing

Calico는 BGP 기반 Pod Route 광고가 가능합니다.

### Cloud

Cloud 환경에서 매우 중요합니다.

예: AWS Transit Gateway, Direct Connect, Cloud WAN, Azure ExpressRoute

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*