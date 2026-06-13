# IEEE 802.3
## 1. IEEE 802.3이란 무엇인가

> 정독: 0회

IEEE 802.3은:

> 전 세계 유선 Ethernet 네트워크의 공식 기술 표준(Technical Standard)

**"유선 네트워크 세계의 공통 규칙 원장"**

이 표준은 다음이 서로 완벽하게 호환되도록 규칙을 정의합니다:

- 랜선(UTP)
- 광케이블
- NIC
- Switch
- Ethernet Frame
- MAC 통신

> 서로 다른 제조사의 장비가 랜선만 꽂으면 통신 가능한 이유는 **IEEE 802.3 표준을 공유**하기 때문이다.

---

## 2. 시스템 어디에서 등장하는가

IEEE 802.3은 거의 모든 유선 네트워크 환경에 등장합니다.

| 환경 | 사용 여부 |
|------|----------|
| PC LAN | 사용 |
| 서버 네트워크 | 사용 |
| 데이터센터 | 사용 |
| NAS/SAN | 사용 |
| Kubernetes Node | 사용 |
| Hypervisor Network | 사용 |
| Cloud Infrastructure | 사용 |

### 전체 흐름

```
Application
    → TCP/IP
    → Ethernet Frame
    → IEEE 802.3 PHY/MAC
    → NIC
    → Cable/Fiber
    → Switch
    → Destination
```

> IEEE 802.3은 **Ethernet 네트워크의 최하단 물리/링크 규칙**입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

IEEE 802.3은 **Network 자원**과 가장 직접 연결됩니다.

| 자원 | 영향도 |
|------|--------|
| Network | 매우 큼 |
| CPU | 중간 |
| Memory | 중간 |
| Disk | 낮음 |

### Network 영향

대표 연결 항목: Link Speed, Duplex, PHY Signal, Ethernet Frame, Packet Transmission

### CPU 영향

고속 Ethernet은 PPS 증가, Interrupt 증가, Packet Processing 증가를 유발합니다.

### Memory 영향

대표 연결 항목: DMA Buffer, NIC Ring Buffer, Packet Queue

---

## 4. 왜 중요한가

전 세계 Ethernet 장비의 **상호 호환성**을 보장하기 때문입니다.

> IEEE 802.3이 없으면 제조사마다 네트워크 규칙이 달라져 **서로 통신할 수 없다**.

| 역할 | 설명 |
|------|------|
| 물리 규격 통일 | 케이블/커넥터 |
| 데이터 포맷 통일 | Ethernet Frame |
| 속도 규격 정의 | 10M~800G |
| 전기/광 신호 정의 | PHY |
| MAC 통신 정의 | L2 전달 |
| 글로벌 호환성 | Vendor Interoperability |

> IEEE 802.3은 **현대 유선 네트워크의 국제 공통 언어**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 네트워크 장애 상당수가 IEEE 802.3 규격과 연결됩니다.

| 장애 | 원인 |
|------|------|
| CRC Error | PHY 신호 오류 |
| Link Down | Cable/PHY 문제 |
| Duplex Mismatch | Auto-negotiation 실패 |
| Packet Corruption | Frame 손상 |
| NIC Compatibility 문제 | 표준 구현 차이 |
| Optical Error | Fiber PHY 문제 |
| MTU 문제 | Frame 크기 충돌 |

> **애플리케이션 장애처럼 보이는 문제도 실제로는 IEEE 802.3 물리 계층 문제인 경우가 많다.**

**예시:** DB Replication 지연, Kubernetes Node 통신 불안정, API Timeout, Storage Latency 증가 등

---

## 6. 핵심 메커니즘

### 6-1. IEEE 802.3은 Ethernet 규칙 전체를 정의한다

| 영역 | 정의 내용 |
|------|----------|
| L1 | 전기/광 신호 |
| L2 | MAC/Ethernet Frame |

Ethernet 전체 기반 규칙입니다.

### 6-2. Ethernet Frame 규격을 정의한다

```
Destination MAC
Source MAC
EtherType
Payload
FCS
```

실제 네트워크 데이터 포맷입니다.

### 6-3. PHY(Physical Layer)를 정의한다

정의 항목: 케이블 종류, 전압 레벨, 광 신호, 인코딩 방식

> **비트를 실제 신호로 바꾸는 규칙**입니다.

### 6-4. 링크 속도도 IEEE 802.3이 정의한다

| 세대 | 속도 |
|------|------|
| 10BASE-T | 10 Mbps |
| Fast Ethernet | 100 Mbps |
| Gigabit Ethernet | 1 Gbps |
| 10GbE | 10 Gbps |
| 100GbE | 100 Gbps |
| 400GbE | 400 Gbps |

데이터센터 네트워크 발전의 핵심입니다.

### 6-5. 현대 Ethernet은 Switch 기반 Full Duplex 구조다

| 구분 | 특징 |
|------|------|
| 과거 | 공유 선로 → 충돌 발생 → CSMA/CD 필요 |
| 현대 | Switch 기반 → Point-to-Point 연결 → 충돌 없음 |

현대 Ethernet은 **고속 스위칭 기반 구조**입니다.

---

## 7. Linux / Runtime / Kubernetes에서 관측 방법

### NIC 상태 확인

```bash
ethtool eth0
```

**관측 가능:** Speed, Duplex, Auto-negotiation

### Ethernet 인터페이스 확인

```bash
ip link
```

**관측 가능:** NIC 상태, MTU, Link UP/DOWN

### Packet/Error 상태 확인

```bash
ip -s link
```

**관측 가능:** CRC Error, RX/TX Drop, Frame Error

### Packet 캡처

```bash
tcpdump
wireshark
```

**관측 가능:** Ethernet Frame, MAC Address, EtherType

### PCIe NIC 정보 확인

```bash
lspci -vv
```

**확인 가능:** NIC Vendor, PCIe Link Speed, Driver 정보

### Kubernetes

K8s 네트워크도 IEEE 802.3 기반입니다.

```
Pod
    → veth
    → Linux Bridge
    → NIC
    → Ethernet Switch
    → Other Node
```

Kubernetes 통신도 결국 **Ethernet 위에서 동작**합니다.

### Observability 도구

현대 시스템에서는 다음으로 Ethernet 상태를 추적합니다:

- ethtool
- tcpdump
- Wireshark
- NIC telemetry
- Switch telemetry

### 대표 메트릭

| 메트릭 | 의미 |
|--------|------|
| Link Speed | 링크 속도 |
| CRC Error | PHY 오류 |
| Packet Loss | 패킷 유실 |
| Throughput | 처리량 |
| PPS | Packet 처리량 |
| Retransmission | 재전송 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*