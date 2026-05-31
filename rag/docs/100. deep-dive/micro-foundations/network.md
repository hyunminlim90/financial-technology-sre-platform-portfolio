# Network (네트워크)
## Micro Foundations — 범용 시스템 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

**Network** 는:

> 서로 떨어진 시스템들이 데이터를 교환할 수 있도록 **연결된 통신 구조**

핵심은 단순 연결이 아니다. 네트워크는:

- 데이터 전달
- 시스템 간 협업
- 상태 공유
- 요청/응답
- 분산 처리

를 가능하게 하는 **기반 인프라**다.

즉:

> **네트워크 = "시스템 사이의 데이터 이동 공간"**

<details>
<summary>Deep Dive</summary></br>

System(시스템) [[M]](../../100-deep-dive/micro-foundations/system.md)  
Data(데이터) [[M]](../../100-deep-dive/micro-foundations/data.md)  
Communication(통신) [[M]](../../100-deep-dive/micro-foundations/communication.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

**현대 시스템 대부분은 네트워크 위에서 동작**한다.

### 사용자 영역
- 웹 브라우저
- 모바일 앱
- IoT 장치

### 서버 인프라
- 서버 ↔ 서버
- VM ↔ VM
- Container ↔ Container

### 분산 시스템
- DB Replication
- Event Streaming
- Cluster Coordination

### 클라우드 인프라
- Multi-region
- CDN
- Load Balancer
- Service Mesh

즉 현대 컴퓨팅은 거의 항상 **Networked Computing** 형태라고 볼 수 있다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 자원: **Network I/O**. 하지만 실제로는 여러 자원이 동시에 연결된다.

| 자원 | Network와의 관계 |
|------|---------------|
| Network | 핵심 자원. bandwidth / packet throughput / RTT / congestion 영향 받음 |
| CPU | packet parsing / encryption / checksum / routing / compression 등에 사용 |
| Memory | 전송 중 데이터가 socket buffer / NIC queue / kernel buffer에 저장 |
| Disk | log streaming / replication / persistent queue 때문에 I/O도 중요 |

---

## 4. 왜 중요한가

현대 시스템 대부분은 **단일 컴퓨터가 아니라 연결된 시스템들의 집합**이다.

API 호출, DB 접근, 파일 다운로드, 메시지 전달, 클라우드 통신 모두 **네트워크 기반**이다.

> **네트워크가 느리면 전체 시스템도 느려진다.**

심지어 CPU 성능보다 `network latency`, `packet loss`, `congestion`이 실제 사용자 경험을 더 크게 좌우하는 경우도 많다.

---

## 5. 실제 장애와 어떤 관련이 있는가

네트워크 장애는 **매우 다양한 형태**로 나타난다.

### 1) Packet Loss
데이터 일부 유실 시 retransmission 증가 → timeout → latency 폭증

### 2) Congestion
전송량이 통로 용량 초과 시 queue 증가 → throughput 감소 → jitter 증가

### 3) Network Partition
시스템 간 연결 단절. 분산 시스템에서 매우 위험.

결과: split brain → stale state → consensus failure 발생 가능

### 4) High Latency
왕복 시간 증가 시 사용자 응답 지연 → retry storm → cascading failure 가능

### 5) DNS / Routing 문제
데이터 목적지 탐색 실패 시 특정 서비스만 장애 / intermittent failure / regional outage 발생 가능

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심은 **"네트워크는 완벽한 통로가 아니다"** 이다.

네트워크에서는 유실(loss), 지연(latency), 순서 변경(reordering), 중복(duplication) 모두 발생 가능하다.

그래서 시스템은:

> **신뢰할 수 없는 네트워크 위에서 신뢰성을 소프트웨어적으로 만든다.**

### 네트워크 속도는 메모리/CPU보다 압도적으로 느리다

| 계층 | 대략적 속도 |
|------|-----------|
| Register | ps / ns |
| RAM | ns |
| SSD | μs / ms |
| Network | ms |

> **분산 시스템의 대부분 비용은 연산보다 네트워크 대기 시간이다.**

### 네트워크는 "공유 자원"이다

여러 시스템이 동시에 사용하므로 congestion 발생 가능, noisy neighbor 영향 가능하다.

그래서 `QoS`, `traffic shaping`, `rate limiting` 같은 개념이 등장한다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### 연결 상태
```bash
ss -s
netstat -s
ip addr
ip route
```

### 트래픽 상태
```bash
iftop
nload
sar -n DEV 1
```

### 패킷 분석
```bash
tcpdump -i eth0
wireshark
```

### NIC 상태
```bash
ethtool eth0
ip -s link
```

### Runtime 관찰 포인트
- `request latency`
- `retry rate`
- `timeout`
- `queue backlog`
- `connection pool 상태`

### Kubernetes Pod 통신
- `CNI`
- `kube-proxy`
- `overlay network`

### Kubernetes Service 레벨
- `ingress latency`
- `east-west traffic`
- `service mesh RTT`

### Kubernetes 클러스터 레벨
- `DNS latency`
- `API server connectivity`
- `node-to-node traffic`

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*