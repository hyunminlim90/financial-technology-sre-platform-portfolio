# IEEE 802.3 Frame (이더넷 프레임)
## 1. IEEE 802.3 프레임이란 무엇인가

> 정독: 0회

IEEE 802.3 프레임은:

> 유선 이더넷(Ethernet) 네트워크에서 장치들이 데이터를 주고받기 위해 사용하는 표준화된 데이터 링크 계층(L2) 전송 포맷

"랜선 위를 흐르는 데이터의 표준 비트 포장 규격"

컴퓨터가 네트워크 데이터를 전송할 때는 TCP 패킷, IP 패킷만 바로 보내는 것이 아니라, 최종적으로 IEEE 802.3 프레임 안에 담겨 NIC(Network Interface Card)를 통해 전송됩니다.

**핵심 구조:**

```
Application Data
→ TCP Segment
→ IP Packet
→ Ethernet Frame (IEEE 802.3)
→ 전기/광 신호
```

> IEEE 802.3 프레임은 실제 네트워크 케이블 위를 흐르는 하드웨어 비트 패키지입니다.

<details>
<summary>Deep Dive</summary></br>

Ethernet(이더넷) [[M]](../../100-deep-dive/micro-foundations/ethernet.md)  
Network(네트워크) [[M]](../../100-deep-dive/micro-foundations/network.md)  
IEEE 802.3 LAN(이더넷 기반 유선 LAN) [[M]](../../100-deep-dive/micro-foundations/ieee-802.3-lan.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

IEEE 802.3 프레임은 OS 네트워크 스택과 NIC 사이에서 등장합니다.

**전체 흐름:**

```
Application
→ TCP/IP Stack
→ Ethernet Frame 생성
→ NIC
→ Switch
→ Cable
→ 상대 NIC
```

서버, 스위치, 라우터, NAS, VM, Kubernetes Node 등 거의 모든 유선 네트워크 환경에서 사용됩니다.

**대표 등장 위치:**

| 구성 요소 | 역할 |
|---|---|
| OS Network Stack | 프레임 생성 |
| NIC | 프레임 송수신 |
| Switch | MAC 기반 전달 |
| Bridge/vSwitch | L2 중계 |
| Hypervisor | Virtual Ethernet 처리 |

> Ethernet Frame은 LAN 통신의 실제 운반 단위입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

IEEE 802.3 프레임은 압도적으로 **Network** 자원과 연결됩니다.

| 자원 | 영향도 |
|---|---|
| Network | 매우 큼 |
| CPU | 큼 |
| Memory | 중간 |
| Disk | 낮음 |

**Network 영향**

- NIC Throughput
- Switch Forwarding
- MTU
- Packet Loss
- CRC Error

**CPU 영향**

NIC는 일부를 하드웨어 처리하지만, Interrupt 처리, Packet Parsing, Network Stack 처리 등은 CPU와 연결됩니다. 특히 PPS(Packet Per Second)가 높아지면 CPU 부하가 커집니다.

**Memory 영향**

- NIC Ring Buffer
- DMA Buffer
- SKB(Socket Buffer)

---

## 4. 왜 중요한가

현대 네트워크 통신의 실제 물리 운반 단위이기 때문입니다.

> TCP/IP도 결국 Ethernet Frame 안에 들어가야 실제 네트워크를 이동할 수 있다.

**대표 역할:**

| 기능 | 설명 |
|---|---|
| MAC 기반 전달 | LAN 통신 |
| 에러 검출 | CRC/FCS |
| 데이터 캡슐화 | 상위 패킷 운반 |
| NIC 필터링 | 불필요 패킷 차단 |
| VLAN 분리 | 네트워크 격리 |

> Ethernet Frame은 "LAN 세계의 실제 운송 컨테이너"입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

| 장애 | 원인 |
|---|---|
| CRC Error | 케이블/전기 신호 문제 |
| Packet Drop | NIC Buffer Overflow |
| MTU Mismatch | 프레임 크기 불일치 |
| Broadcast Storm | L2 루프 |
| MAC Flapping | Switch 문제 |
| RX/TX Error | NIC 장애 |
| Fragmentation 증가 | Jumbo Frame 문제 |
| Packet Corruption | 물리 계층 오류 |

**특히 중요한 점:**

> TCP Timeout처럼 보여도 실제로는 Ethernet Frame 손상 문제일 수 있다.

---

## 6. 핵심 메커니즘

### 6-1. Ethernet Frame은 NIC가 직접 처리한다

```
Network Signal
→ NIC
→ Ethernet Frame Decode
→ MAC 검사
→ OS 전달 여부 결정
```

NIC가 하드웨어 수준에서 프레임을 먼저 검사합니다.

### 6-2. Destination MAC이 하드웨어 필터 역할을 한다

```
Destination MAC ≠ 내 NIC 주소
→ Hardware Drop
```

대부분의 불필요 패킷은 NIC 하드웨어 수준에서 제거됩니다. CPU까지 가지 않습니다.

### 6-3. FCS/CRC가 비트 손상을 검사한다

```
Frame Receive
→ CRC 검사
→ 오류 시 폐기
```

Ethernet은 하드웨어 수준에서 비트 무결성을 검사합니다.

### 6-4. MTU는 Ethernet Payload 최대 크기와 연결된다

| 유형 | 일반 크기 |
|---|---|
| Standard Ethernet | 1500 Byte |
| Jumbo Frame | 9000 Byte |

MTU는 Frame Payload 크기 제한과 연결됩니다.

### 6-5. Ethernet Frame은 스위치 기반 전달의 핵심이다

```
Destination MAC
→ MAC Table Lookup
→ 특정 Port 전달
```

Ethernet Frame은 L2 Switching 핵심 구조입니다.

---

## 7. Linux / Runtime / Kubernetes에서 관측 방법

**NIC 상태 확인**

```bash
ip link
ethtool eth0
# 관측: MTU, RX/TX Error, Link Speed
```

**Packet 통계 확인**

```bash
ifconfig
ip -s link
# 확인: Dropped Packet, CRC Error, RX/TX Count
```

**Ethernet Frame 캡처**

```bash
tcpdump
wireshark
# 관측: Source MAC, Destination MAC, EtherType, VLAN Tag, Frame Length
```

**Switch / Bridge 환경**

```bash
bridge link
brctl show
# 확인: Linux Bridge, Virtual Ethernet, L2 Topology
```

**Kubernetes**

K8s Pod 네트워크도 결국 Ethernet 기반입니다.

```
Pod
→ veth
→ Linux Bridge / vSwitch
→ NIC
→ Ethernet Frame
```

CNI 네트워크도 최종적으로 Ethernet Frame 위에서 동작합니다.

**Observability**

현대 시스템에서는 tcpdump, Wireshark, eBPF, NIC telemetry 등으로 프레임 상태를 추적합니다.

| 메트릭 | 의미 |
|---|---|
| RX/TX Packets | 프레임 처리량 |
| CRC Error | 비트 손상 |
| Dropped Frames | NIC 버퍼 문제 |
| Broadcast Count | L2 Flooding |
| MTU Error | 프레임 크기 문제 |
| Packet Per Second | PPS 부하 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*