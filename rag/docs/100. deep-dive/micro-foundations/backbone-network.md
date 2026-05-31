# 백본망 (Backbone Network / 중추망)

> 정독: 0회

## 1. 이 기술이 무엇인가

백본망(Backbone Network)은:

> **대규모 ISP·클라우드·통신사가 구축한 초고속 장거리 핵심 네트워크 인프라**

인터넷워크 전체에서 도시 ↔ 도시, 국가 ↔ 국가, 데이터센터 ↔ 데이터센터, AS ↔ AS 간의 대용량 트래픽을 전달합니다.

### 계층 관점

백본망은 주로 다음 위에서 동작합니다.

- L1 (광섬유 물리망)
- L2 (고속 Ethernet/MPLS)
- L3 (BGP/IP Routing)

<details>
<summary>Deep Dive</summary></br>

Internet Service Provider(인터넷 서비스 제공업체) [[M]](../../100-deep-dive/micro-foundations/internet-service-provider.md)  
Cloud Service Provider(클라우드 서비스 제공업체) [[M]](../../100-deep-dive/micro-foundations/cloud-service-provider.md)  
Telecommunications Carrier(통신 사업자) [[M]](../../100-deep-dive/micro-foundations/telecommunications-carrier.md)  
Network(네트워크) [[M]](../../100-deep-dive/micro-foundations/network.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

백본망은 인터넷 핵심 인프라 전체에 등장합니다.

### ISP 내부

KT, SKB, LGU+, NTT, AT&T 등의 전국/국제망

### 클라우드 사업자

AWS, Google Cloud, Microsoft Azure 전용 글로벌 광망 운영

### 데이터센터 간 연결

East-West DC Traffic, Replication, CDN, Backup

### 인터넷 교환 지점(IX)

IX, Peering Fabric, Transit Interconnect

### 해저 광케이블

국가 간 국제망의 핵심입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **Network 자원**입니다.

특히 Throughput, Latency, Jitter, Congestion, Packet Loss에 절대적 영향을 줍니다.

### Router CPU 영향

BGP 계산, MPLS 처리, ACL 처리

### Router Memory 영향

Full Route Table, FIB/TCAM, MPLS Label Table

---

## 4. 왜 중요한가

백본망은 **인터넷의 실제 물리적 전송 기반**입니다.

백본망이 없으면 국가 간 통신, 대륙 간 패킷 전달, CDN 동작, 클라우드 연결, 글로벌 서비스가 모두 불가능합니다.

### 대규모 트래픽 처리

Streaming, AI Cluster, CDN, 금융망, 클라우드 등 대용량 트래픽을 수용합니다.

### 인터넷 안정성

백본망 품질은 인터넷 품질, 글로벌 latency, 국제망 속도에 직접 영향을 줍니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

인터넷 대형 장애 상당수가 백본망 문제입니다.

### 광케이블 절단

대표 사례: 해저 케이블 절단, 건설 작업 중 광선로 손상

증상: 국제망 latency 증가, 특정 국가 접속 불가, 우회 경로 폭증

### Backbone Congestion

백본 링크 포화.

증상: throughput 감소, packet loss 증가, RTT 급증

### Core Router 장애

코어 라우터 다운.

증상: 대규모 지역 인터넷 장애

### BGP Instability

Backbone AS 간 정책 충돌.

증상: route flap, route convergence 지연, global reachability 문제

### DDoS

백본 대역폭 자체 공격.

증상: ISP 전체 품질 저하, 국제망 장애

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### ① Core Router 기반 L3 전달

백본망 핵심은 **초고속 L3 Packet Forwarding**입니다. 수 Tbps급 처리를 수행합니다.

### ② 광 기반 전송

주 전송 매체: DWDM, Fiber Optic

빛 파장 분리(DWDM)를 통해 **단일 광섬유에서 다중 채널 동시 전송**이 가능합니다.

### ③ BGP 기반 AS 연결

AS 간 글로벌 경로 제어를 수행합니다.

핵심: Transit, Peering, Policy Routing

### ④ MPLS 기반 Traffic Engineering

대규모 ISP는 MPLS를 사용합니다.

목적: QoS, Fast Reroute, VPN, TE

### ⑤ Redundancy

백본망은 항상 **다중 경로(Redundant Path)**로 구성됩니다.

이유: 단일 장애점 제거, 해저케이블 장애 대비, Core Router 장애 대비

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

일반 Linux/K8s는 백본망 자체를 운영하지는 않지만 간접적으로 영향을 받습니다.

### Linux

**RTT 관측**

```bash
ping <host>
mtr <host>
traceroute <host>
```

**경로 확인**

```bash
ip route
```

**NIC 통계**

```bash
ethtool
sar -n DEV
ss
```

### Kubernetes

K8s 내부보다는 **Cluster 외부 네트워크 품질**에 영향을 받습니다.

**영향 영역:** Multi Region Cluster, Service Mesh, Cross Region Replication, CDN, API Gateway

백본 장애 시 외부 API timeout, 이미지 Pull 실패, Cross-region replication 지연, DNS latency 증가 등이 발생합니다.

### Cloud

Cloud는 사실상 자체 백본망 사업자 수준입니다.

예: AWS Global Backbone, Google Private Fiber, Azure WAN

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*