# 데이터 캡슐화 및 역캡슐화 흐름 (Data Encapsulation and Decapsulation Flow)

> 정독: 0회

## 1. 이 기술이 무엇인가

데이터 캡슐화 및 역캡슐화는:

> TCP/IP 계층 구조에서 데이터가 계층을 이동하면서 **각 프로토콜 헤더가 추가되거나 제거되는 흐름**

### 캡슐화 (Encapsulation)

송신 측에서 상위 계층 데이터가 하위 계층으로 내려가며 각 계층 헤더를 추가하는 과정입니다.

### 역캡슐화 (Decapsulation)

수신 측에서 하위 계층 프레임이 상위 계층으로 올라가며 각 계층 헤더를 제거하는 과정입니다.

---

## 2. 시스템 어디에서 등장하는가

모든 네트워크 통신에서 등장합니다.

| 영역 | 사용 |
|------|------|
| 웹 통신 | HTTP over TCP/IP |
| DNS 조회 | UDP/IP |
| 클라우드 네트워크 | Overlay/Underlay |
| Kubernetes Pod 통신 | CNI + VXLAN |
| VPN | Tunnel Encapsulation |
| Service Mesh | Sidecar Proxy |
| CDN | Edge Routing |
| 데이터센터 네트워크 | Ethernet/IP/TCP |

### 실제 흐름

```
Application
    ↓
Transport
    ↓
Internet
    ↓
Network Access
    ↓
Physical Signal
```

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **Network + CPU**입니다.

| 자원 | 영향 | 예시 |
|------|------|------|
| Network | 헤더 추가로 패킷 크기 증가 | TCP Header, IP Header, Ethernet Header, Tunnel Header |
| CPU | 프로토콜 처리 비용 발생 | checksum 계산, segmentation, routing lookup, encryption, packet parsing |
| Memory | 패킷 버퍼·socket buffer 사용량 증가 | — |
| Disk | 패킷 캡처·네트워크 로그 저장 시 영향 | — |

---

## 4. 왜 중요한가

현대 네트워크는 계층 분리 구조로 동작합니다.

캡슐화/역캡슐화가 없다면:

- 서로 다른 프로토콜 공존 불가
- 라우팅 불가
- TCP 신뢰성 제어 불가
- Ethernet 전송 불가
- 인터넷 상호 운용성 붕괴

### 중요한 이유

- **계층 독립성 유지**: 각 계층은 자기 역할만 수행
- **프로토콜 조합 가능**: HTTP → TCP → IP → Ethernet
- **네트워크 장비 역할 분리**

| 장비 | 해석 계층 |
|------|----------|
| Switch | Ethernet |
| Router | IP |
| L4 LB | TCP/UDP |
| Proxy | HTTP |

- **장애 분석 가능**: 문제가 어느 계층인지 분리 가능

---

## 5. 실제 장애와 어떤 관련이 있는가

실무 네트워크 장애 대부분은 캡슐화 구조와 연결됩니다.

| 장애 유형 | 원인 |
|-----------|------|
| MTU 초과 | 헤더 추가로 패킷 크기가 커져 fragmentation 발생 |
| VXLAN/GRE 터널 문제 | Overlay encapsulation 실패 |
| TCP checksum 오류 | 세그먼트 손상 발생 |
| NAT 문제 | IP/Port 변환 실패 |
| Reverse Proxy 문제 | HTTP 계층 헤더 손실 |
| TLS 종료 위치 문제 | L7/L4 계층 처리 충돌 |
| Kubernetes CNI 문제 | Pod overlay packet encapsulation 실패 |

---

## 6. 핵심 메커니즘

> 계층마다 자신의 제어 정보를 헤더에 추가하고, 수신 측은 이를 역순으로 해석한다.

### 송신 흐름 (Encapsulation)

**① Application Layer** — 원본 데이터 생성

- HTTP Request, DNS Query, SMTP Message

**② Transport Layer** — TCP/UDP 헤더 추가

```
추가 정보: Source Port, Destination Port, Sequence Number, ACK, Window Size
결과: TCP Segment
```

**③ Internet Layer** — IP 헤더 추가

```
추가 정보: Source IP, Destination IP, TTL, Protocol Type
결과: IP Packet
```

**④ Network Access Layer** — Ethernet Header/Trailer 추가

```
추가 정보: Source MAC, Destination MAC, EtherType, CRC
결과: Ethernet Frame
```

**⑤ Physical Layer** — 비트 신호로 변환되어 전송

---

### 수신 흐름 (Decapsulation)

**① NIC** — Frame 수신 → MAC 주소 확인 → CRC 검증

**② IP Stack** — IP Header 제거 → Destination IP 확인 → Routing 처리

**③ TCP/UDP Stack** — Port 확인 → 재조립 → 순서 검증 → 재전송 처리

**④ Application** — 최종 원본 데이터 전달

---

### 핵심 데이터 형태 변화

```
# 송신
Data → Segment → Packet → Frame → Bits

# 수신
Bits → Frame → Packet → Segment → Data
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 패킷 확인
tcpdump

# 인터페이스 확인
ip addr
ip link

# MTU 확인
ip link show

# TCP 상태 확인
ss -ant

# 네트워크 통계
netstat -s
```

### Kubernetes

```bash
# Pod 네트워크 확인
kubectl get pods -o wide

# CNI Overlay 확인
kubectl get nodes

# VXLAN 인터페이스 확인 (노드 내부)
ip link

# Service Routing 확인
iptables -t nat -L
```

### 자주 보는 장애 지표

| 지표 | 의미 |
|------|------|
| retransmission | 재전송 증가 |
| fragmentation | 패킷 분할 |
| packet drop | 패킷 손실 |
| checksum error | 헤더 손상 |
| MTU mismatch | 캡슐화 크기 초과 |
| connection reset | TCP 연결 중단 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*