# 이더넷 데이터 링크 프로토콜 (Ethernet Data Link Protocol)
## 1. 이더넷 데이터 링크 프로토콜이란 무엇인가

> 정독: 0회

이더넷 데이터 링크 프로토콜은:

> **유선 LAN 환경에서 인접한 네트워크 장치(Node)끼리 데이터를 안정적으로 전달하기 위한 L2(Data Link Layer) 통신 규칙**

핵심 역할은 **"데이터를 프레임(Frame)으로 포장하고, MAC 주소 기반으로 전달하며, 오류를 검출하는 것"** 입니다.

**매우 중요한 본질**:

이 프로토콜은 "같은 물리 네트워크 구간(Local Ethernet Segment) 안에서 누가 누구에게 데이터를 보낼지 결정"합니다.

인터넷 전체가 아니라 **같은 LAN 내부의 직접 연결된 장치들 사이 통신**을 담당합니다.

---

## 2. 시스템 어디에서 등장하는가

이더넷 데이터 링크 프로토콜은 거의 모든 유선 네트워크 환경에서 등장합니다.

### 대표 위치

```
Application
    ↓
TCP/IP
    ↓
Ethernet Data Link Protocol
    ↓
NIC
    ↓
Cable
```

즉, **IP Packet을 실제 Ethernet Frame으로 변환하는 계층**입니다.

### 등장 장비

NIC, Switch, Router Interface, Hypervisor Virtual Switch, Storage Network

### 데이터센터

특히 중요합니다. 서버 ↔ 스위치, 노드 ↔ 노드, Kubernetes Pod Network, Storage Replication 모두 Ethernet 기반인 경우가 많습니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **Network 자원**입니다.

하지만 실제 운영에서는 **CPU, Memory**에도 영향이 큽니다.

### 왜 그런가?

프레임 처리 과정에서 Interrupt, DMA, Buffer Allocation, Packet Parsing이 발생하기 때문입니다.

### 특히 중요한 요소

| 요소 | 영향 |
|------|------|
| Frame Size | Throughput |
| MTU | Fragmentation |
| Broadcast | CPU Load |
| Packet Loss | Retransmission |
| CRC Error | Network Stability |

---

## 4. 왜 중요한가

이 프로토콜은 **현대 LAN 통신의 기본 전달 메커니즘**입니다.

상위 계층(TCP/IP)은 결국 Ethernet Frame 전달 성공을 전제로 동작합니다.

### ① 로컬 전달 보장

MAC 주소 기반으로 같은 네트워크 내부에서 정확한 NIC로 전달합니다.

### ② 오류 검출

FCS/CRC를 통해 깨진 프레임을 제거합니다.

### ③ 네트워크 효율 유지

현대 Ethernet은 **Switch 기반 Full Duplex** 구조로 충돌 제거, 고속 전송, 병렬 통신을 가능하게 합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### CRC Error 증가

원인: 케이블 불량, EMI, 광모듈 문제

증상: packet corruption, throughput 저하, latency 증가

### MAC Table 문제

스위치에서 MAC Learning 실패, MAC Flapping 발생 가능.

증상: intermittent connectivity, broadcast storm

### MTU Mismatch

원인: 1500 vs 9000 Jumbo Frame 불일치

증상: fragmentation, packet drop

### Broadcast Storm

L2 Loop 발생 시.

증상: switch overload, network collapse, high CPU

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### ① Framing

데이터를 Ethernet Frame 형태로 포장합니다.

```
┌──────────────────┐
│ Destination MAC  │
├──────────────────┤
│ Source MAC       │
├──────────────────┤
│ Type / Length    │
├──────────────────┤
│ Payload          │
├──────────────────┤
│ FCS              │
└──────────────────┘
```

### ② MAC Addressing

각 NIC는 고유 MAC 주소를 가집니다. 스위치는 이를 기반으로 전달합니다.

### ③ Error Detection

FCS(CRC 기반)를 통해 비트 손상을 검출합니다.

> **중요**: 수정(Correction)이 아니라 **검출(Detection)**입니다. 깨지면 버립니다.

### ④ Switching

현대 Ethernet의 핵심. Switch가 MAC 주소 ↔ 포트를 학습하여 정확한 목적지로만 프레임을 전달합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

**MAC 주소 확인**

```bash
ip link
```

**Frame / Error 통계 확인**

```bash
ethtool -S eth0
# rx_crc_errors, rx_frame_errors, tx_errors
```

**ARP / MAC 확인**

```bash
ip neigh
```

**Frame 캡처 (Ethernet Header 직접 확인)**

```bash
tcpdump -e
```

**Bridge 링크 확인**

```bash
bridge link
```

### Kubernetes

특히 중요합니다. CNI, Overlay Network, VXLAN, veth Pair 모두 Ethernet/L2 개념 위에서 동작합니다.

Pod 통신 불가 문제의 실제 원인이 vSwitch, Bridge, MAC Learning, MTU 문제일 수 있습니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*