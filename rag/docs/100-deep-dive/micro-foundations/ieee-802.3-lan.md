# IEEE 802.3 LAN (이더넷 기반 유선 LAN)
## 1. IEEE 802.3 LAN이란 무엇인가

> 정독: 0회

IEEE 802.3 LAN은:

> IEEE 802.3 이더넷(Ethernet) 표준 기반으로 구축된 유선 로컬 네트워크(Local Area Network) 환경 전체를 의미

**"유선 이더넷 기반의 물리 네트워크 인프라 시스템"**

여기에는 다음이 모두 포함됩니다:

- 랜선(UTP/Fiber)
- NIC
- 스위치
- Ethernet Frame
- MAC Address
- 전기/광 신호 규칙

### 핵심 구조

```
Computer
   ↕
  NIC
   ↕
Ethernet Cable
   ↕
 Switch
   ↕
Another Computer
```

> IEEE 802.3 LAN은 **Ethernet Frame들이 이동하는 실제 물리 네트워크 공간**입니다.

---

## 2. 시스템 어디에서 등장하는가

IEEE 802.3 LAN은 거의 모든 유선 네트워크 환경에서 등장합니다.

| 환경 | 사용 여부 |
|------|----------|
| 사무실 LAN | 사용 |
| 데이터센터 | 사용 |
| 서버랙 네트워크 | 사용 |
| NAS 환경 | 사용 |
| Kubernetes Node 통신 | 사용 |
| Hypervisor VM 네트워크 | 사용 |

### 전체 흐름

```
Application
    → TCP/IP Stack
    → Ethernet Frame
    → NIC
    → IEEE 802.3 LAN
    → Switch
    → Destination Host
```

운영체제, NIC, 스위치, 케이블 전체가 IEEE 802.3 LAN의 일부입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

IEEE 802.3 LAN은 압도적으로 **Network 자원**과 연결됩니다.

| 자원 | 영향도 |
|------|--------|
| Network | 매우 큼 |
| CPU | 큼 |
| Memory | 중간 |
| Disk | 낮음 |

### Network 영향

대표 연결 항목: Throughput, Packet Loss, Latency, Switching, MTU, Link Speed

### CPU 영향

NIC는 일부 오프로딩을 수행하지만, 다음은 CPU와 연결됩니다:

- Packet Processing
- Interrupt Handling
- TCP/IP Stack

특히 **PPS(Packet Per Second)가 높으면 CPU 사용량이 증가**합니다.

### Memory 영향

대표 연결 항목: NIC Ring Buffer, DMA Buffer, Socket Buffer (패킷 버퍼 메모리와 연결)

---

## 4. 왜 중요한가

현대 서버·클라우드·인터넷 인프라 대부분이 Ethernet 기반이기 때문입니다.

> **현대 데이터센터의 실제 물리 네트워크 기반은 대부분 IEEE 802.3 LAN이다.**

| 역할 | 설명 |
|------|------|
| 서버 연결 | LAN 기반 |
| 스위칭 | MAC 기반 전달 |
| 고속 통신 | 1G~400G+ |
| 데이터센터 Fabric | Ethernet 기반 |
| VM/Container 통신 | Ethernet 기반 |
| NAS/SAN 연결 | Ethernet 기반 |

> IEEE 802.3 LAN은 **현대 유선 네트워크의 표준 인프라**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 네트워크 장애 상당수가 IEEE 802.3 LAN 계층과 연결됩니다.

| 장애 | 원인 |
|------|------|
| CRC Error | 케이블 손상 |
| Packet Loss | NIC/Switch 문제 |
| Link Down | 물리 연결 장애 |
| Duplex Mismatch | 속도 협상 문제 |
| Broadcast Storm | L2 Loop |
| MAC Flapping | 스위치 문제 |
| MTU Mismatch | Jumbo Frame 불일치 |
| RX/TX Drop | Buffer Overflow |

> **애플리케이션 장애처럼 보여도 실제로는 Ethernet 물리 계층 문제인 경우가 많다.**

**예시:**
- API Timeout → Packet Loss
- DB Replication 지연 → NIC 문제
- Kubernetes Node NotReady → Link Failure

---

## 6. 핵심 메커니즘

### 6-1. Ethernet Frame이 실제 전송 단위다

```
TCP/IP Packet
    → Ethernet Frame 캡슐화
    → LAN 전송
```

실제 LAN 위를 이동하는 것은 **Ethernet Frame**입니다.

### 6-2. MAC Address 기반으로 L2 전달이 이루어진다

```
Destination MAC
    → MAC Table 조회
    → 특정 Port 전달
```

IP 이전 단계에서 MAC 기반 전달이 이루어집니다.

### 6-3. 현대 Ethernet은 Full Duplex 기반이다

| 구분 | 특징 |
|------|------|
| 과거 | 공유 선로 → 충돌 발생 → CSMA/CD 필요 |
| 현대 | Switch 기반 → 1:1 링크 → 충돌 없음 |

현대 Ethernet은 거의 항상 **Full Duplex**입니다.

### 6-4. NIC가 Ethernet Frame을 하드웨어 처리한다

```
NIC
    → MAC 검사
    → CRC 검사
    → DMA로 Memory 전달
```

NIC는 Frame Parsing, Hardware Filtering, Error Detection을 직접 수행합니다.

### 6-5. 물리 매체 품질이 성능에 직접 영향 준다

| 요소 | 영향 |
|------|------|
| UTP 품질 | Signal Integrity |
| Fiber 품질 | Optical Loss |
| Switch Buffer | Packet Drop |
| Link Speed | Throughput |
| Cable Length | Error 증가 |

---

## 7. Linux / Runtime / Kubernetes에서 관측 방법

### NIC 상태 확인

```bash
ip link
ethtool eth0
```

**관측 가능:** Link Speed, Duplex, MTU, RX/TX 상태

### Packet Error 확인

```bash
ip -s link
ifconfig
```

**관측 가능:** Dropped Packet, CRC Error, RX/TX Error

### Ethernet Frame 캡처

```bash
tcpdump
wireshark
```

**관측 가능:** MAC Address, VLAN, EtherType, Frame Length

### Switch / Bridge 상태

```bash
bridge link
brctl show
```

**확인 가능:** Linux Bridge, veth, Virtual Switching

### Kubernetes

K8s Pod 네트워크도 최종적으로 Ethernet 기반입니다.

```
Pod
    → veth
    → Linux Bridge / OVS
    → NIC
    → IEEE 802.3 LAN
```

### Observability 도구

현대 시스템에서는 다음으로 Ethernet 상태를 추적합니다:

- tcpdump
- Wireshark
- ethtool
- eBPF
- NIC telemetry

### 대표 메트릭

| 메트릭 | 의미 |
|--------|------|
| RX/TX Throughput | 네트워크 처리량 |
| Packet Loss | 프레임 유실 |
| CRC Error | 비트 손상 |
| Link Utilization | 링크 사용률 |
| PPS | Packet 처리량 |
| Broadcast Rate | L2 Flooding |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*