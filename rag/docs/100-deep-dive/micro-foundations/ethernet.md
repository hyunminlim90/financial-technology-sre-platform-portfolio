# Ethernet (이더넷)
## 1. 이더넷(Ethernet)이란 무엇인가

> 정독: 0회

이더넷(Ethernet)은:

> 컴퓨터와 네트워크 장비들이 유선 케이블을 통해 데이터를 주고받기 위한 전 세계 표준 유선 네트워크 기술

**"유선 네트워크 세계의 공통 통신 언어"**

| 역할 | 설명 |
|------|------|
| 물리 신호 전달 | 전기/광 신호 전송 |
| 장비 식별 | MAC Address |
| 데이터 캡슐화 | Ethernet Frame |
| LAN 통신 | 로컬 네트워크 연결 |
| 스위칭 기반 전달 | MAC 기반 경로 결정 |

> Ethernet은 **데이터를 실제 케이블 위로 운반하는 물리 네트워크 기술**입니다.

<details>
<summary>Deep Dive</summary></br>

Computer(컴퓨터) [[M]](../../100-deep-dive/micro-foundations/computer.md)  
Network (네트워크) [[M]](../../100-deep-dive/micro-foundations/network.md)  
Twisted Pair Cable(트위스티드 페어 케이블) [[M]](../../100-deep-dive/micro-foundations/twisted-pair-cable.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

Ethernet은 거의 모든 유선 네트워크 환경에서 등장합니다.

| 환경 | Ethernet 사용 |
|------|--------------|
| PC LAN | 사용 |
| 사무실 네트워크 | 사용 |
| 서버 인프라 | 사용 |
| 데이터센터 | 사용 |
| NAS/SAN | 사용 |
| Hypervisor Network | 사용 |
| Kubernetes Node 통신 | 사용 |

### 전체 흐름

```
Application
    → TCP/IP Stack
    → Ethernet Frame
    → NIC
    → Switch
    → Ethernet LAN
    → Destination Host
```

Ethernet은 운영체제 아래, TCP/IP 아래, 물리 케이블 위에서 동작하는 **실제 네트워크 기반**입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

Ethernet은 압도적으로 **Network 자원**과 연결됩니다.

| 자원 | 영향도 |
|------|--------|
| Network | 매우 큼 |
| CPU | 큼 |
| Memory | 중간 |
| Disk | 낮음 |

### Network 영향

대표 연결 항목: Throughput, Latency, Packet Loss, Link Speed, MTU

### CPU 영향

대표 연결 항목: Interrupt 처리, Packet Processing, TCP/IP Stack 처리

특히 **고속 Ethernet에서는 PPS(Packet Per Second)가 CPU 사용량에 큰 영향**을 줍니다.

### Memory 영향

대표 연결 항목: NIC Ring Buffer, DMA Buffer, Socket Buffer (패킷 처리용 메모리 버퍼와 연결)

---

## 4. 왜 중요한가

현대 인터넷·클라우드·데이터센터 대부분이 Ethernet 기반이기 때문입니다.

> **현대 서버 인프라의 실제 물리 네트워크 기반은 대부분 Ethernet이다.**

| 역할 | 설명 |
|------|------|
| 서버 연결 | 데이터센터 네트워크 |
| 스위칭 | MAC 기반 전달 |
| VM/Container 통신 | Virtual Ethernet |
| 스토리지 연결 | NAS/iSCSI |
| 클라우드 네트워크 | Ethernet Fabric |

> Ethernet은 **현대 디지털 인프라의 혈관**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 네트워크 장애 상당수가 Ethernet 계층과 연결됩니다.

| 장애 | 원인 |
|------|------|
| CRC Error | 케이블/PHY 손상 |
| Packet Loss | NIC/Switch 문제 |
| Link Down | 물리 연결 장애 |
| Duplex Mismatch | 속도 협상 실패 |
| MTU Mismatch | Frame 크기 충돌 |
| Broadcast Storm | L2 Loop |
| RX/TX Drop | Buffer Overflow |
| Link Flap | 포트 불안정 |

> **애플리케이션 장애처럼 보여도 실제로는 Ethernet 물리 계층 문제인 경우가 많다.**

**예시:** API Timeout, DB Replication 지연, Kubernetes Node 통신 실패, Storage IO 지연 등이 Ethernet 문제에서 시작될 수 있습니다.

---

## 6. 핵심 메커니즘

### 6-1. Ethernet은 L1/L2 계층 기술이다

| 계층 | 역할 |
|------|------|
| L1 | 전기/광 신호 |
| L2 | Ethernet Frame/MAC |

> Ethernet은 **"비트를 실제 네트워크로 전달하는 기술"** 입니다.

### 6-2. Ethernet은 MAC Address 기반으로 동작한다

```
Destination MAC
    → Switch Lookup
    → 특정 Port 전달
```

IP 이전 단계에서 MAC 기반 전달이 이루어집니다.

### 6-3. Ethernet Frame이 실제 전송 단위다

```
Ethernet Header
    + Payload
    + CRC/FCS
```

실제 케이블 위를 흐르는 것은 **Ethernet Frame**입니다.

### 6-4. 현대 Ethernet은 Switch 기반 Full Duplex 구조다

| 구분 | 특징 |
|------|------|
| 과거 | 공유 선로 → 충돌 발생 → CSMA/CD 필요 |
| 현대 | Switch 기반 → 1:1 연결 → 충돌 없음 |

현대 Ethernet은 **고속 스위칭 기반**입니다.

### 6-5. NIC가 Ethernet 처리를 하드웨어 수준에서 수행한다

```
Ethernet Signal
    → NIC
    → Frame Decode
    → CRC Check
    → DMA Memory 전달
```

NIC는 MAC 검사, CRC 검사, DMA 전송 등을 **하드웨어 처리**합니다.

---

## 7. Linux / Runtime / Kubernetes에서 관측 방법

### NIC 상태 확인

```bash
ip link
ethtool eth0
```

**관측 가능:** Link Speed, Duplex, MTU, Link State

### Packet/Error 상태 확인

```bash
ip -s link
ifconfig
```

**관측 가능:** RX/TX Error, CRC Error, Dropped Packet

### Ethernet Frame 캡처

```bash
tcpdump
wireshark
```

**관측 가능:** Source MAC, Destination MAC, VLAN, EtherType

### Switch / Bridge 확인

```bash
bridge link
brctl show
```

**관측 가능:** Linux Bridge, Virtual Ethernet, L2 Topology

### Kubernetes

K8s 네트워크도 결국 Ethernet 기반입니다.

```
Pod
    → veth
    → Linux Bridge / OVS
    → NIC
    → Ethernet Network
```

Kubernetes 내부 네트워크도 **Ethernet 위에서 동작**합니다.

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
| Link Utilization | 링크 사용률 |
| Packet Loss | 패킷 유실 |
| CRC Error | 물리 오류 |
| PPS | Packet 처리량 |
| RX/TX Throughput | 네트워크 처리량 |
| MTU Error | 프레임 크기 문제 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*