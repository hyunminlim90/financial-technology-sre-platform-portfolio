# 프로토콜 제어 정보 (PCI)

> 정독: 0회

## 1. 이 기술이 무엇인가

프로토콜 제어 정보(PCI, Protocol Control Information)는:

> 데이터를 안전하고 정확하게 전송하기 위해 **프로토콜이 추가하는 제어용 메타데이터**

### 핵심 특징

PCI는 실제 사용자 데이터(Payload)가 아닙니다. 대신 다음과 같은 네트워크 운영 정보를 담습니다:

- 주소 지정
- 순서 제어
- 오류 검출
- 연결 상태
- 흐름 제어

### PCI의 물리적 형태

PCI는 보통 **Header** 와 **Trailer** 형태로 존재합니다.

### 대표 예시

| 계층 | PCI 예시 |
|---|---|
| Ethernet | MAC Address, FCS |
| IP | Source / Destination IP |
| TCP | Port, Sequence Number |
| HTTP | Method, Header Field |

### 핵심 관계

```
PCI + Payload = PDU
```

---

## 2. 시스템 어디에서 등장하는가

PCI는 모든 데이터 통신 계층에 존재합니다.

### 등장 위치

| 영역 | PCI 존재 여부 |
|---|---|
| Ethernet Frame | 있음 |
| IP Packet | 있음 |
| TCP Segment | 있음 |
| TLS Record | 있음 |
| HTTP Message | 있음 |
| VXLAN Tunnel | 있음 |
| Kubernetes Overlay Network | 있음 |

### 실제 데이터 흐름

```
Application Data
↓
TCP Header 추가
↓
IP Header 추가
↓
Ethernet Header 추가
↓
Transmission
```

### 클라우드 / 분산 시스템

다음 모두 PCI를 사용합니다:

- Service Mesh
- Load Balancer
- CDN
- Gateway
- VPN
- Overlay Network
- Kubernetes CNI

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Network**이지만, 실제로는 **CPU 영향도 매우 큽니다.**

### 자원별 영향

| 자원 | 영향 |
|---|---|
| Network | packet size 증가 |
| CPU | header parsing |
| Memory | buffering |
| Disk | 거의 간접적 |

### Network 영향이 큰 이유

PCI는 다음을 유발합니다:

- 네트워크 전송량 증가
- 패킷 크기 증가
- MTU 영향
- 캡슐화 증가

### CPU 영향이 큰 이유

수신 시스템은 모든 PCI를 읽고, 해석하고, 검증해야 합니다. 특히 다음 작업이 CPU 사용량 증가 원인이 됩니다:

- checksum
- sequence validation
- routing lookup
- NAT translation

---

## 4. 왜 중요한가

PCI는 **데이터 통신 자체를 가능하게 합니다.**

### PCI가 없으면 불가능한 것

- 어디로 보낼지 모름
- 누가 보냈는지 모름
- 순서 복구 불가
- 오류 검출 불가
- 재전송 불가
- 연결 상태 추적 불가

### 핵심 의미

PCI는 **데이터 전송을 위한 제어 로직의 실체**입니다.

### 성능 측면 중요성

PCI는 Throughput 감소, Goodput 감소, MTU fragmentation 증가의 원인이 되기도 합니다.

### 특히 현대 클라우드 환경

다중 캡슐화로 PCI 오버헤드가 커집니다:

```
Ethernet → VXLAN → IP → TCP → TLS → HTTP
```

---

## 5. 실제 장애와 어떤 관련이 있는가

### 대표 장애 유형

| 장애 유형 | 설명 |
|---|---|
| MTU 초과 | 헤더 증가 → fragmentation 발생 |
| Header Corruption | 패킷 폐기 발생 |
| Checksum Error | NIC 또는 커널에서 드롭 |
| Sequence Mismatch | TCP 재조립 실패 |
| NAT Translation 문제 | 세션 연결 실패 |
| Encapsulation Overhead 증가 | Goodput 급감 |

### Kubernetes 환경 대표 문제

```
VXLAN Header 추가 → MTU 감소 → Fragmentation 증가 → 성능 저하
```

### 실제 현상

| 현상 | 원인 |
|---|---|
| retransmission 증가 | corrupted PCI |
| packet drop | invalid checksum |
| throughput 저하 | header overhead |
| pod network 불안정 | overlay encapsulation |
| API timeout | fragmentation 증가 |

---

## 6. 핵심 메커니즘

핵심 메커니즘은 **각 계층이 자신의 제어 정보를 Payload 앞뒤에 추가하는 것**입니다.

### 기본 흐름

**송신측**
```
Payload
↓
TCP PCI 추가
↓
IP PCI 추가
↓
Ethernet PCI 추가
↓
Transmission
```

**수신측**
```
Ethernet PCI 제거
↓
IP PCI 제거
↓
TCP PCI 제거
↓
Payload 복원
```

### 중요한 특징

현재 계층은 **상위 계층 전체를 단순 Payload로 취급**합니다.

### PCI와 Payload 관계

| 현재 계층 | Payload로 보는 대상 |
|---|---|
| TCP | Application Data |
| IP | TCP Segment |
| Ethernet | IP Packet |

> **핵심 구조:** PCI는 제어 정보, Payload는 실제 데이터입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

| 도구 | 용도 |
|---|---|
| `tcpdump` | 패킷 분석 |
| `tcpdump -vv` | 상세 헤더 확인 |
| `ethtool -S eth0` | NIC 통계 |
| `ip link` | MTU 확인 |

**Wireshark** 는 가장 대표적인 PCI 관측 도구로, 다음 헤더를 확인할 수 있습니다:

- Ethernet Header
- IP Header
- TCP Header
- TLS Header
- VXLAN Header

### Kubernetes

- **Pod MTU 확인** — `kubectl exec`
- **Overlay Network 확인** — 다음 CNI들이 추가 PCI를 사용합니다: Calico, Flannel, Cilium, Weave

### Runtime / Infra에서 관측하는 항목

| 항목 | 의미 |
|---|---|
| MTU | 최대 프레임 크기 |
| MSS | TCP payload 최대 크기 |
| checksum error | 헤더 손상 |
| retransmission | sequence 문제 |
| fragmentation | MTU 초과 |
| encapsulation depth | overlay 계층 수 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*