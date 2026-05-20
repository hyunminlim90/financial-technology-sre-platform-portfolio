# 라우터 (Router)

> 정독: 0회

## 1. 이 기술이 무엇인가

라우터(Router)는:

> **서로 다른 네트워크 사이에서 IP 패킷의 목적지 경로를 결정하고 다음 네트워크로 전달하는 3계층(L3) 네트워크 장비**

라우터의 핵심 역할은 **Routing(경로 결정)** 과 **Forwarding(패킷 전달)** 입니다.

### 처리 대상

라우터는 IP Packet을 기준으로 동작합니다. MAC 주소(L2)보다 **IP 주소(L3)**를 중심으로 판단합니다.

---

## 2. 시스템 어디에서 등장하는가

라우터는 **네트워크와 네트워크 사이의 경계**에서 등장합니다.

### 대표 위치

가정용 공유기, 기업 네트워크 게이트웨이, 데이터센터 Border Router, ISP Core Router, Cloud Edge Router, Internet Backbone

### 네트워크 흐름상 위치

```
Host
  ↓
Switch (L2)
  ↓
Router (L3)
  ↓
다른 Network
```

### 클라우드 환경

라우터 개념은 다음에도 존재합니다.

VPC Router, Virtual Router, SDN Router, Kubernetes CNI Routing, Service Mesh Egress

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **Network 자원**입니다.

특히 Throughput, Latency, Packet Forwarding, Bandwidth, Route Convergence에 직접 영향을 줍니다.

### CPU 영향

Routing 계산, ACL 검사, NAT, Firewall 처리, BGP 계산 등을 수행하므로 고성능 라우터에서는 CPU 사용량이 중요합니다.

### Memory 영향

라우팅 테이블 저장이 필요합니다. BGP Full Table, Prefix Table, ARP Cache, NAT Table 등이 메모리를 사용합니다.

---

## 4. 왜 중요한가

라우터는 **네트워크와 네트워크를 연결하는 핵심 장비**입니다.

라우터가 없으면 다른 서브넷 접근, 인터넷 연결, 글로벌 통신, AS 간 연결이 모두 불가능합니다.

### 인터넷 핵심 구조

인터넷은 **Router들의 연결 구조**라고 봐도 됩니다. 패킷은 여러 라우터를 거치며 다음 목적지로 전달됩니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

네트워크 장애 상당수가 라우터 문제입니다.

### Routing Loop

잘못된 경로 계산.

증상: TTL 감소, latency 급증, packet drop

### Route Blackhole

경로는 존재하지만 실제 전달 실패.

증상: Ping timeout, 특정 대역 unreachable

### Interface Down

포트 장애.

증상: packet loss, link down, failover 발생

### BGP Session Down

AS 간 연결 끊김.

증상: 해외망 장애, 특정 서비스 unreachable

### NAT Table Exhaustion

세션 폭증.

증상: 신규 연결 실패, timeout 증가

### CPU Saturation

라우터 CPU 과부하.

원인: DDoS, Route churn, ACL 폭증

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### ① Routing

목적지 IP 기준 경로 결정. 라우터는 Destination IP를 보고 다음 Hop을 결정합니다.

### ② Forwarding

결정된 출력 포트로 패킷을 전달합니다. 실제 패킷 이동 처리 단계입니다.

### ③ Routing Table

라우터 내부 경로 테이블.

```
10.0.0.0/24 → eth0
0.0.0.0/0   → gateway
```

### ④ Next Hop

다음 라우터 주소. 패킷은 최종 목적지까지 **Router → Router** 방식으로 전달됩니다.

### ⑤ TTL 처리

라우터 통과 시 `TTL -= 1` 수행. 0이 되면 폐기합니다. Routing Loop 방지 목적입니다.

### ⑥ Re-encapsulation

매 Hop마다 L2 Frame을 제거하고 새로운 L2 Frame을 생성합니다.

> **IP Packet은 유지되지만, Ethernet Frame은 매 구간마다 변경됩니다.**

### ⑦ ARP / Neighbor Resolution

다음 Hop MAC 주소 확인 시 IPv4는 ARP, IPv6는 Neighbor Discovery를 사용합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

**Routing Table 확인**

```bash
ip route
route -n
```

**Interface 상태 확인**

```bash
ip addr
ip link
ethtool
```

**ARP Cache 확인**

```bash
ip neigh
arp -a
```

**경로 추적**

```bash
traceroute <host>
mtr <host>
```

### Kubernetes

K8s 내부도 결국 라우팅 구조입니다.

**주요 위치:** CNI Routing, Pod-to-Pod Routing, Node Routing, Service Routing, Egress Gateway

**실제 사용 기술:** Calico, Cilium, kube-proxy, BGP CNI

Cross-node Pod 통신 실패의 실제 원인이 Route 누락, Overlay 문제, MTU mismatch, BGP session down인 경우가 있습니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*