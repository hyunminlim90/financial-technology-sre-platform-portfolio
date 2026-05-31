# IEEE 802.3 프로토콜 (IEEE 802.3 Protocol)
## 1. IEEE 802.3 프로토콜이란 무엇인가

> 정독: 0회

IEEE 802.3 프로토콜은:

> 유선 Ethernet 네트워크에서 노드들이 데이터를 안전하게 전달하기 위해 사용하는 **L1/L2 기반 통신 규칙 집합**

즉 데이터를 어떻게 포장할지 · 누구에게 보낼지 · 오류를 어떻게 검출할지 · 네트워크 선로를 어떻게 사용할지를 정의합니다.

핵심은:

**"유선 Ethernet 통신의 실제 동작 규칙"** 입니다.

<details>
<summary>Deep Dive</summary></br>

Ethernet(이더넷) [[M]](../../100-deep-dive/micro-foundations/ethernet.md)  
Network(네트워크) [[M]](../../100-deep-dive/micro-foundations/network.md)  
Node(노드) [[M]](../../100-deep-dive/micro-foundations/node.md)  
Data(데이터) [[M]](../../100-deep-dive/micro-foundations/data.md)  
IEEE 802.3 Ethernet Architecture Specifications(이더넷 아키텍처 규격) [[M]](../../100-deep-dive/micro-foundations/ieee-802.3-ethernet-architecture-specifications.md)  
Physical Layer(물리 계층) [[M]](../../100-deep-dive/micro-foundations/physical-layer.md)  
Data Link Layer(데이터 링크 계층) [[M]](../../100-deep-dive/micro-foundations/data-link-layer.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

IEEE 802.3 프로토콜은 거의 모든 유선 LAN 환경에서 등장합니다.

| 환경 | 사용 여부 |
|---|---|
| PC LAN | 사용 |
| 서버 네트워크 | 사용 |
| 데이터 센터 | 사용 |
| 스위치 환경 | 사용 |
| Hypervisor Network | 사용 |
| Kubernetes Node Network | 사용 |
| NAS/SAN 일부 | 사용 |

**실제 흐름:**

```
Application Data
→ TCP/IP
→ Ethernet Frame
→ NIC
→ Twisted Pair / Fiber
→ Switch
→ Other Node
```

즉 IEEE 802.3 프로토콜은 **NIC와 스위치 사이에서 실제로 동작**합니다.

---

## 3. 어떤 자원(CPU / Memory / Network / Disk)에 가장 영향이 큰가

IEEE 802.3 프로토콜은 **Network 자원**과 가장 직접 연결됩니다.

| 자원 | 영향도 |
|---|---|
| Network | 매우 큼 |
| CPU | 중간 |
| Memory | 중간 |
| Disk | 거의 없음 |

**Network 영향**

```
Ethernet Frame · MAC Delivery · Link Negotiation · CRC/FCS · Duplex · MTU
```

즉 물리 Ethernet 전달 자체입니다.

**CPU 영향**

```
Interrupt · Packet Processing · Checksum 처리 · Driver 처리
```

특히 고속 NIC에서는 CPU 처리량과 직접 연결됩니다.

**Memory 영향**

```
RX/TX Ring Buffer · DMA Buffer · NIC Queue
```

즉 패킷 버퍼 처리와 연결됩니다.

---

## 4. 왜 중요한가

현대 유선 네트워크 거의 전체가 IEEE 802.3 기반이기 때문입니다.

> 오늘날 서버·클라우드·데이터센터 대부분은  
> **IEEE 802.3 Ethernet 기반으로 연결된다.**

| 영역 | 설명 |
|---|---|
| LAN | 내부 통신 |
| Data Center | 서버 연결 |
| Cloud | Node 통신 |
| Storage | NAS/SAN 일부 |
| Kubernetes | Node Networking |

즉 **현대 인프라의 물리적 네트워크 기반**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 네트워크 장애 상당수가 IEEE 802.3 계층 문제입니다.

| 장애 | 설명 |
|---|---|
| CRC Error | 프레임 손상 |
| Duplex Mismatch | 속도 협상 실패 |
| Link Down | 물리 연결 실패 |
| MTU Mismatch | 프레임 크기 불일치 |
| Broadcast Storm | L2 폭주 |
| Packet Drop | 버퍼 초과 |
| Cable Fault | 랜선 문제 |
| NIC Failure | NIC 장애 |

**매우 중요한 실무 포인트**

> 애플리케이션 장애처럼 보여도  
> **실제로는 Ethernet 링크 품질 문제인 경우가 많다.**

예를 들어 API Timeout · DB Replication Delay · Kubernetes Node Disconnect · Storage Latency의 실제 원인이 CRC 증가 · NIC Error · Cable Fault인 경우가 매우 많습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 6-1. 데이터는 Ethernet Frame으로 이동한다

```
Destination MAC
Source MAC
EtherType
Payload
FCS
```

즉 IEEE 802.3의 기본 전송 단위는 **Frame**입니다.

### 6-2. MAC Address 기반으로 전달한다

```
Frame 생성 → Destination MAC 기록 → Switch 전달 → 대상 NIC 수신
```

즉 L2 기반 전달입니다.

### 6-3. 오류 검출(FCS/CRC)을 수행한다

```
Frame 수신 → CRC 계산 → 값 불일치 → Frame Drop
```

즉 깨진 프레임을 **하드웨어 수준에서 제거**합니다.

### 6-4. 현대 Ethernet은 Full Duplex 기반이다

- **과거**: CSMA/CD 기반 충돌 환경
- **현대**: Switch 기반 Full Duplex

즉 현재는 충돌 자체가 거의 없습니다.

### 6-5. NIC가 IEEE 802.3 프로토콜을 실제 집행한다

| 기능 | 수행 주체 |
|---|---|
| Frame 생성 | NIC |
| CRC 계산 | NIC |
| MAC 검사 | NIC |
| DMA 전달 | NIC |
| Link Negotiation | NIC |

즉 NIC는 **IEEE 802.3 프로토콜의 실질적 실행 장치**입니다.

### 6-6. Auto Negotiation도 중요하다

협상 항목: `1G / 10G` · `Half / Full Duplex` · `Flow Control`

즉 링크 연결 시 속도와 모드를 자동 협상합니다.

---

## 7. Linux / Runtime / Kubernetes에서 어디서 관측되는가

**NIC 상태 확인**

```bash
ip link
```

관측 가능: Link 상태 · MTU · MAC Address

**Ethernet 세부 상태 확인**

```bash
ethtool eth0
```

관측 가능: Speed · Duplex · Auto Negotiation · CRC Error · Link Detected

**인터페이스 통계 확인**

```bash
ip -s link
```

관측 가능: RX/TX Packet · Error · Drop

**Frame 캡처**

```bash
tcpdump -e
wireshark
```

관측 가능: Ethernet Header · Source/Destination MAC · VLAN Tag · EtherType

**Driver/NIC 상태**

```bash
dmesg
```

대표 로그: Link Up/Down · NIC Reset · Driver Error

**Kubernetes**

K8s Node 통신도 결국 Ethernet 기반입니다.

```
Pod → veth → Bridge → NIC → Ethernet → Switch → Other Node
```

즉 Kubernetes Overlay 아래에도 **실제 Ethernet 네트워크가 존재**합니다.

**Observability**

현대 운영에서는 `NIC Metrics` · `Switch Telemetry` · `eBPF` · `Packet Capture` 등으로 IEEE 802.3 상태를 추적합니다.

| 메트릭 | 의미 |
|---|---|
| CRC Error | 프레임 손상 |
| RX/TX Drop | 패킷 유실 |
| Link Flap | 링크 불안정 |
| Duplex Mismatch | 협상 실패 |
| Retransmission | 재전송 증가 |
| Broadcast Rate | L2 트래픽 증가 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*