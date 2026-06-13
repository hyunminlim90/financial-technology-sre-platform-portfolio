# IEEE 802.3 Ethernet Working Group
## 1. IEEE 802.3 Ethernet Working Group이란 무엇인가

> 정독: 0회

IEEE 802.3 Ethernet Working Group은:

> 전 세계 유선 Ethernet 네트워크 기술 표준을 정의·개정·확장하는 공식 국제 표준 작업 그룹

**"Ethernet 세계의 기술 법과 규칙을 만드는 국제 표준 설계 조직"**

이 그룹은 다음을 표준화합니다:

- Ethernet Frame 구조
- PHY 신호 규격
- MAC 동작 규칙
- 링크 속도
- 광통신 규격
- PoE
- 고속 데이터센터 Ethernet

> 전 세계 NIC, Switch, Server가 서로 호환되는 이유는 **IEEE 802.3 표준을 공유**하기 때문이다.

---

## 2. 시스템 어디에서 등장하는가

IEEE 802.3 Working Group은 직접 패킷을 처리하는 시스템이 아니라, Ethernet 기술 전체의 규칙을 정의하는 **상위 표준 조직**입니다.

그러나 결과적으로 이 그룹의 규격은 다음 전체에 영향을 줍니다:

- NIC 칩셋
- 스위치 ASIC
- 광트랜시버
- 데이터센터 네트워크
- 서버 메인보드
- Hypervisor Network
- Kubernetes Node Network

### 전체 흐름

```
IEEE 802.3 Working Group
        ↓
  Ethernet 표준 정의
        ↓
    Vendor 구현
        ↓
  NIC/Switch 제작
        ↓
  OS Driver 지원
        ↓
실제 LAN/Datacenter 운영
```

> 현대 Ethernet 인프라의 **최상위 규칙 생성자**입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

IEEE 802.3 Working Group은 **Network 자원**과 가장 직접적으로 연결됩니다.

| 자원 | 영향도 |
|------|--------|
| Network | 매우 큼 |
| CPU | 중간 |
| Memory | 중간 |
| Disk | 낮음 |

### Network 영향

대표 연결 항목: Link Speed, Ethernet PHY, Frame Format, MAC Protocol, Optical Ethernet, Datacenter Fabric

### CPU 영향

고속 Ethernet 발전은 PPS 증가, Interrupt 증가, Packet Processing 증가로 이어져 CPU 아키텍처에도 영향을 줍니다.

대표 연결: RSS, NIC Offloading, DPDK, SmartNIC

### Memory 영향

대표 연결 항목: NIC DMA Buffer, Packet Queue, Jumbo Frame Buffer

---

## 4. 왜 중요한가

전 세계 Ethernet 호환성과 네트워크 진화를 유지하기 때문입니다.

> IEEE 802.3 Working Group이 없으면 제조사마다 Ethernet 규칙이 달라져 **서로 통신이 불가능**해진다.

| 역할 | 설명 |
|------|------|
| 글로벌 호환성 | 장비 상호 연결 |
| 속도 표준화 | 10M~800G+ |
| 광통신 규격 정의 | Fiber Ethernet |
| 데이터센터 네트워크 발전 | 초고속 Ethernet |
| PoE 표준화 | 전력+데이터 통합 |
| 자동차 Ethernet | 차량 네트워크 |

> 현대 인터넷 물리 네트워크의 **공통 언어**를 정의합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 네트워크 장애 상당수가 IEEE 802.3 규격과 연결됩니다.

| 장애 | 관련 규격 |
|------|----------|
| CRC Error | Physical Layer |
| Link Negotiation 실패 | Auto-negotiation |
| Duplex Mismatch | MAC/PHY 규칙 |
| Packet Corruption | Signal Encoding |
| Optical Loss | Fiber Standard |
| MTU 문제 | Ethernet Frame |
| Switch Interop 문제 | Vendor 규격 차이 |

> **대규모 데이터센터 장애도 결국 Ethernet 표준 규격 충돌이나 PHY 계층 문제에서 시작되는 경우가 있다.**

**예시:**
- 광모듈 호환 실패
- Auto-negotiation 오류
- 100G/400G 링크 불안정
- Switch ASIC Interoperability 문제

---

## 6. 핵심 메커니즘

### 6-1. IEEE 802.3은 Ethernet 규칙의 공식 원장이다

```
IEEE Working Group
    → 표준 정의
    → Vendor 구현
    → 전 세계 장비 호환
```

모든 Ethernet 장비는 이 규칙을 따라야 합니다.

### 6-2. L1/L2 규격을 정의한다

| 계층 | 정의 내용 |
|------|----------|
| L1 | 전기/광 신호 |
| L2 | Ethernet MAC |

케이블 신호, 광 인코딩, Frame 구조 등이 정의됩니다.

### 6-3. Ethernet 속도 진화도 여기서 결정된다

| 세대 | 속도 |
|------|------|
| Fast Ethernet | 100M |
| Gigabit Ethernet | 1G |
| 10GbE | 10G |
| 100GbE | 100G |
| 400GbE | 400G |
| 800GbE | 800G |

데이터센터 대역폭 진화의 핵심입니다.

### 6-4. 현대 AI/클라우드 인프라도 Ethernet 기반이다

```
  GPU Cluster
      ↕
400G/800G Ethernet Fabric
      ↕
Datacenter Spine Network
```

AI 인프라도 Ethernet 기반으로 진화 중입니다.

### 6-5. Ethernet 표준은 단순 "랜선" 수준이 아니다

현재 Ethernet은 다음 영역까지 확장되었습니다:

- AI Fabric
- HPC
- Automotive
- Storage Network
- Cloud Backbone

> **Ethernet은 현대 디지털 인프라의 핵심 물리 프로토콜입니다.**

---

## 7. Linux / Runtime / Kubernetes에서 관측 방법

### Ethernet Link 상태 확인

```bash
ethtool eth0
```

**관측 가능:** Speed, Duplex, Auto-negotiation

### NIC Driver 확인

```bash
lspci -vv
ethtool -i eth0
```

**관측 가능:** NIC Vendor, Driver, Firmware

### Packet/Error 상태 확인

```bash
ip -s link
```

**관측 가능:** CRC Error, RX/TX Drop, Frame Error

### Datacenter Ethernet 상태

```bash
lldpctl
```

**확인 가능:** Switch Neighbor, Port 정보, Link Capability

### Kubernetes

K8s 데이터 플레인도 Ethernet 기반입니다.

```
Pod
    → veth
    → Linux Bridge / OVS
    → NIC
    → Ethernet Fabric
```

Kubernetes 네트워크 역시 **IEEE Ethernet 규격** 위에서 동작합니다.

### Observability 도구

현대 시스템에서는 다음으로 Ethernet 상태를 추적합니다:

- ethtool
- tcpdump
- Wireshark
- NIC Telemetry
- Switch Telemetry

### 대표 메트릭

| 메트릭 | 의미 |
|--------|------|
| Link Utilization | 링크 사용률 |
| CRC Error | PHY 오류 |
| Packet Loss | 프레임 유실 |
| PPS | Packet 처리량 |
| Link Speed | Ethernet 속도 |
| Optical Power | 광신호 상태 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*