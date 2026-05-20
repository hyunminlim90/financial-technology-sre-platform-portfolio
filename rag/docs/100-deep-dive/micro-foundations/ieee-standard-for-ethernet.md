# IEEE Standard for Ethernet (IEEE 이더넷 표준)
## 1. IEEE Standard for Ethernet이란 무엇인가

> 정독: 0회

IEEE Standard for Ethernet은:

> IEEE 802.3 위원회가 정의한 전 세계 유선 이더넷(Ethernet) 네트워크의 공식 기술 표준 명세서

**"전 세계 Ethernet 장비들이 서로 호환되도록 만드는 공식 하드웨어 네트워크 설계 법전"**

이 표준은 다음을 정의합니다:

- 랜선 전기 신호 규칙
- 광신호 규칙
- Ethernet Frame 구조
- MAC 동작 방식
- 링크 속도
- 충돌 제어
- PHY/NIC 동작

> Ethernet 세계의 **공통 언어**이자 **공통 물리 규칙**입니다.

<details>
<summary>Deep Dive</summary></br>

IEEE 802.3 Ethernet Working Group [[M]](../../100-deep-dive/micro-foundations/ieee-802.3-ethernet-working-group.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

IEEE Ethernet 표준은 거의 모든 유선 네트워크 장비에 존재합니다.

| 장비 | 적용 여부 |
|------|----------|
| NIC | 적용 |
| Switch | 적용 |
| Router Interface | 적용 |
| Server | 적용 |
| NAS | 적용 |
| Hypervisor Network | 적용 |
| Datacenter Fabric | 적용 |

### 전체 흐름

```
IEEE 802.3 Specification
        ↓
NIC/Switch 제조
        ↓
Ethernet Frame 처리
        ↓
    LAN 통신
        ↓
TCP/IP 서비스 제공
```

운영체제, 네트워크 카드, 스위치, 광모듈, 데이터센터 네트워크 전체가 **IEEE 802.3 규격** 위에서 동작합니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

IEEE Ethernet 표준은 **Network 자원**과 가장 직접적으로 연결됩니다.

| 자원 | 영향도 |
|------|--------|
| Network | 매우 큼 |
| CPU | 중간 |
| Memory | 중간 |
| Disk | 낮음 |

### Network 영향

대표 연결 항목: Link Speed, MTU, Frame Format, Error Detection, PHY Signaling

### CPU 영향

CPU는 다음과 연결됩니다:

- Packet Processing
- Interrupt Handling
- TCP/IP Stack 처리

특히 **고속 Ethernet(100G/400G)에서는 CPU 부담이 매우 커질 수 있습니다.**

### Memory 영향

대표 연결 항목: DMA Buffer, NIC Ring Buffer, Packet Queue (네트워크 버퍼 메모리와 연결)

---

## 4. 왜 중요한가

전 세계 유선 네트워크 호환성을 보장하기 때문입니다.

> Intel NIC, Cisco Switch, Linux Server, Cloud Datacenter가
> 서로 통신 가능한 이유는 **IEEE 802.3 규격을 공유**하기 때문이다.

| 역할 | 설명 |
|------|------|
| 장비 호환성 | 제조사 간 상호 통신 |
| 표준 속도 정의 | 1G/10G/100G |
| Frame 규격 통일 | Ethernet Frame |
| Error Detection | CRC/FCS |
| 물리 신호 통일 | PHY 규격 |
| 데이터센터 네트워크 기반 | Ethernet Fabric |

> IEEE Ethernet 표준은 **글로벌 유선 네트워크 생태계의 공통 규칙**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 네트워크 장애 상당수가 Ethernet 표준 규격과 연결됩니다.

| 장애 | 원인 |
|------|------|
| CRC Error | 신호 손상 |
| Duplex Mismatch | 협상 실패 |
| MTU Mismatch | Frame 크기 충돌 |
| Link Negotiation 실패 | PHY 문제 |
| Packet Corruption | Physical Layer 오류 |
| Broadcast Storm | L2 Loop |
| Link Flap | 케이블/포트 문제 |
| Speed Downgrade | Auto-negotiation 문제 |

> **애플리케이션 장애의 근본 원인이 실제로는 Ethernet 물리 계층 규격 문제인 경우가 많다.**

**예시:**
- Kubernetes Node 간 Timeout
- DB Replication 지연
- Storage Packet Loss
- VM Migration 실패

---

## 6. 핵심 메커니즘

### 6-1. Ethernet 표준은 L1/L2 규칙을 정의한다

| 계층 | 역할 |
|------|------|
| L1 Physical | 전기/광 신호 |
| L2 Data Link | Ethernet Frame |

> IEEE 802.3은 **"비트를 어떻게 실제 선로 위에서 이동시킬 것인가"** 를 정의합니다.

### 6-2. Ethernet Frame 구조도 표준 일부다

```
Destination MAC
Source MAC
Type/Length
Payload
FCS
```

Frame 포맷도 표준화되어 있습니다.

### 6-3. PHY 규격이 물리 신호를 정의한다

| 규격 | 의미 |
|------|------|
| 1000BASE-T | 1Gbps UTP |
| 10GBASE-SR | 10G Fiber |
| 400GBASE-FR4 | 400G Optical |

전압, 광신호, 주파수, 인코딩 등이 정의됩니다.

### 6-4. Auto-negotiation도 Ethernet 표준 일부다

NIC와 스위치는 다음을 자동 수행합니다:

- 속도 협상
- Duplex 협상
- Flow Control 협상

결과 예: `1Gbps / Full Duplex` 같은 링크 상태가 결정됩니다.

### 6-5. 현대 데이터센터는 Ethernet 기반 Fabric이다

```
       Server
         ↕
Top-of-Rack Switch
         ↕
    Spine Switch
         ↕
  Datacenter Fabric
```

클라우드 네트워크 대부분이 Ethernet 기반입니다.

---

## 7. Linux / Runtime / Kubernetes에서 관측 방법

### NIC 정보 확인

```bash
ethtool eth0
```

**관측 가능:** Link Speed, Duplex, Auto-negotiation

### Interface 상태 확인

```bash
ip link
```

**확인 가능:** MTU, MAC Address, Link State

### Error 상태 확인

```bash
ip -s link
```

**관측 가능:** RX/TX Error, CRC Error, Dropped Packet

### Ethernet Frame 캡처

```bash
tcpdump
wireshark
```

**관측 가능:** Ethernet Header, VLAN Tag, EtherType, Frame Length

### Kubernetes

K8s Pod 네트워크도 최종적으로 Ethernet 기반입니다.

```
Pod
    → veth
    → Linux Bridge / OVS
    → NIC
    → Ethernet Network
```

Kubernetes도 **IEEE Ethernet 표준** 위에서 동작합니다.

### Observability 도구

현대 시스템에서는 다음으로 Ethernet 상태를 추적합니다:

- ethtool
- tcpdump
- Wireshark
- eBPF
- NIC telemetry

### 대표 메트릭

| 메트릭 | 의미 |
|--------|------|
| Link Speed | 링크 속도 |
| Packet Loss | 패킷 손실 |
| CRC Error | 물리 오류 |
| RX/TX Throughput | 처리량 |
| PPS | Packet Rate |
| Link Utilization | 사용률 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*