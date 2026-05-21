# 인터넷 프로토콜 스위트 (Internet Protocol Suite)

> 정독: 0회

## 1. 이 기술이 무엇인가

인터넷 프로토콜 스위트는:

> 인터넷 통신을 위해 여러 계층의 네트워크 프로토콜들을 역할별로 체계화한 표준 통신 아키텍처 집합

일반적으로 **TCP/IP 스택**이라고 부릅니다.

### 핵심 목적

서로 다른 운영체제, 하드웨어, 네트워크, 데이터센터, CSP, ISP 환경에서도 통신 가능하도록 만드는 것입니다.

### 핵심 구성 계층

인터넷 프로토콜 스위트는 보통 **4계층 모델**로 설명합니다.

| 계층 | 역할 | 주요 프로토콜 |
|---|---|---|
| Application Layer | 사용자 애플리케이션 프로토콜 | HTTP, DNS, SMTP, TLS |
| Transport Layer | 프로세스 간 데이터 전달 제어 | TCP, UDP |
| Internet Layer | IP 기반 글로벌 라우팅 | IP, ICMP |
| Network Access Layer | 물리 네트워크 전달 | Ethernet, Wi-Fi |

<details>
<summary>Deep Dive</summary></br>

IP Data Communication(IP 데이터 통신) [[M]](../../100-deep-dive/micro-foundations/ip-data-communication.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

현대 모든 네트워크 시스템에서 등장합니다.

### 사용자 단말

PC, smartphone, IoT

### 서버 시스템

VM, bare metal, container node

### 데이터센터 네트워크

spine-leaf network, overlay network

### Kubernetes

Pod communication, Service routing, Ingress traffic

### 클라우드 플랫폼

VPC, Load Balancer, NAT Gateway

### 인터넷 서비스

web service, API platform, SaaS

### CDN / DNS / Edge

전부 TCP/IP 기반.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Network 자원**입니다.

| 자원 | 영향 | 주요 지표 |
|---|---|---|
| **Network** | 매우 큼 | packet transfer, routing, retransmission, congestion |
| **CPU** | 중요 | TCP processing, checksum, encryption, interrupt handling |
| **Memory** | 중요 | socket buffer, packet queue, connection tracking |
| **Disk** | 간접적 | packet logging, pcap dump, traffic analytics |

> 인터넷 프로토콜 스위트는 네트워크 자원을 계층적으로 분리하여 표준화된 방식으로 통신을 수행하게 만듭니다.

---

## 4. 왜 중요한가

현대 인터넷 자체의 기반입니다. 없으면 브라우저, API, Kubernetes, Cloud, SaaS, AI platform 전부 동작 불가능합니다.

### 상호 운용성

서로 다른 시스템 간 통신 가능.

### 계층 분리

문제를 역할별로 분리 가능.

### 표준화

전 세계 네트워크 공통 규칙 제공.

### 확장성

인터넷 규모 확장 가능.

### 장애 분리

계층별 원인 분석 가능.

### 운영 자동화 가능

라우팅·전송·애플리케이션 자동화 가능.

---

## 5. 실제 장애와 어떤 관련이 있는가

### DNS Failure

애플리케이션 계층 장애. → domain lookup 실패

### TCP Retransmission Explosion

전송 계층 문제. → latency 증가, throughput 감소

### MTU Mismatch

패킷 단편화 문제. → intermittent failure

### Packet Loss

네트워크 계층 장애. → timeout, service degradation

### Routing Failure

IP 경로 문제. → unreachable service

### SYN Flood

TCP 연결 자원 고갈. → connection exhaustion

### ARP Failure

L2 주소 해석 실패. → local communication failure

### TLS Handshake Failure

Application/Transport 문제. → secure connection failure

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

### ① Layered Architecture

기능을 계층별 분리.

### ② Encapsulation

계층별 헤더 추가.

### ③ Decapsulation

수신 측 헤더 제거.

### ④ IP Routing

목적지 기반 패킷 전달.

### ⑤ TCP Reliability

순서·재전송·흐름 제어.

### ⑥ UDP Fast Delivery

비연결 고속 전송.

### ⑦ Port-Based Multiplexing

프로세스별 통신 분리.

### ⑧ Socket Abstraction

애플리케이션 네트워크 인터페이스.

### ⑨ End-to-End Communication

종단 간 데이터 전달 보장.

### 캡슐화 핵심 구조

```
[ Ethernet ]
    └── [ IP ]
            └── [ TCP/UDP ]
                    └── [ Application Data ]
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

**인터페이스 확인**

```bash
ip addr
ip link
```

**라우팅 테이블**

```bash
ip route
```

**TCP 상태**

```bash
ss -ant
netstat -ant
```

**패킷 캡처**

```bash
tcpdump
```

**DNS 확인**

```bash
dig
nslookup
```

**네트워크 통계**

```bash
sar -n DEV
```

### Kubernetes

**Pod 네트워크**

```bash
kubectl get pods -o wide
```

**Service 확인**

```bash
kubectl get svc
```

**DNS 상태**

```bash
kubectl get pods -n kube-system   # CoreDNS 등
```

**Ingress 상태**

```bash
kubectl get ingress
```

**CNI 상태**

```bash
kubectl get pods -n kube-system   # Calico/Cilium/Flannel 등
```

### 네트워크 분석 핵심 도구

`tcpdump`, `wireshark`, `traceroute`, `mtr`, `ping`, `ss`, `iptables/nftables`

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*