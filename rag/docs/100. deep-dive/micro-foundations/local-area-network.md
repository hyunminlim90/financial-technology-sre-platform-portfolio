# 근거리 통신망 (LAN, Local Area Network)
## 1. 근거리 통신망(LAN)이란 무엇인가

> 정독: 0회

LAN(Local Area Network)은:

> 비교적 가까운 물리적 공간 안에서 여러 장치들을 서로 연결한 네트워크

**"집·사무실·학교·데이터센터 내부처럼 가까운 공간 안의 장치들을 연결한 네트워크"**

| 환경 | LAN 여부 |
|------|---------|
| 집 공유기 네트워크 | LAN |
| 회사 내부망 | LAN |
| 학교 컴퓨터실 | LAN |
| 서버랙 내부 네트워크 | LAN |
| 데이터센터 내부망 | LAN |

> LAN은 **"내부 네트워크 공간"** 입니다.

---

## 2. 시스템 어디에서 등장하는가

LAN은 거의 모든 컴퓨터 환경의 기본 네트워크 구조입니다.

### 대표 구조

```
PC / Server / Printer
         ↕
  Switch / Wi-Fi AP
         ↕
        LAN
         ↕
       Router
         ↕
      Internet
```

내부 장치끼리는 LAN으로 연결되고, 외부 인터넷은 Router를 통해 연결됩니다.

### 대표 구성 요소

| 구성 요소 | 역할 |
|----------|------|
| NIC | 네트워크 인터페이스 |
| Switch | 내부 패킷 전달 |
| Wi-Fi AP | 무선 연결 |
| Router | 외부망 연결 |
| Ethernet Cable | 물리 연결 |

---

## 3. 어떤 자원에 가장 영향이 큰가

LAN은 **Network 자원**과 가장 직접 연결됩니다.

| 자원 | 영향도 |
|------|--------|
| Network | 매우 큼 |
| CPU | 중간 |
| Memory | 중간 |
| Disk | 간접 영향 |

### Network 영향

대표 연결 항목: 내부 통신 속도, Latency, Throughput, Packet Delivery

### CPU 영향

대표 연결 항목: Packet Processing, Interrupt Handling, TCP/IP 처리

특히 **고속 LAN(10G/25G/100G)은 CPU 부하에도 영향**을 줍니다.

### Disk 간접 영향

대표 연결 항목: NAS, SAN, Distributed Storage (네트워크 스토리지 성능과 연결)

---

## 4. 왜 중요한가

현대 시스템 대부분이 LAN 위에서 동작하기 때문입니다.

> **클라우드·Kubernetes·사내 시스템·DB Cluster 대부분은 LAN 기반 내부망 위에서 동작한다.**

| 역할 | 설명 |
|------|------|
| 내부 장치 연결 | PC/Server 통신 |
| 자원 공유 | 프린터/NAS |
| 내부 서비스 통신 | API/DB |
| Cluster 구성 | Kubernetes/VM |
| 고속 내부망 | Low Latency |

> LAN은 **내부 디지털 인프라의 기본 연결 구조**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 장애 상당수가 LAN 문제와 연결됩니다.

| 장애 | 원인 |
|------|------|
| 내부 API Timeout | Switch 문제 |
| DB Replication 지연 | LAN 혼잡 |
| Kubernetes Node 통신 실패 | L2 문제 |
| Packet Loss | NIC/Switch 오류 |
| Broadcast Storm | Loop 발생 |
| ARP 문제 | L2 충돌 |
| VLAN 오설정 | 네트워크 단절 |

> **인터넷은 정상이어도 LAN 내부 문제가 발생하면 서비스 전체가 장애 상태가 될 수 있다.**

**예시:** 데이터센터 내부망 장애, VM Cluster 분리, Kubernetes Node Partition 등

---

## 6. 핵심 메커니즘

### 6-1. LAN은 제한된 물리 공간 네트워크다

| 범위 | 예시 |
|------|------|
| 집 | Home LAN |
| 사무실 | Office LAN |
| 빌딩 | Campus LAN |
| 데이터센터 | Datacenter LAN |

**"근거리 내부망"** 입니다.

### 6-2. Ethernet과 Wi-Fi가 LAN의 핵심 기술이다

| 기술 | 역할 |
|------|------|
| Ethernet | 유선 LAN |
| Wi-Fi | 무선 LAN |

둘 다 LAN 기술입니다.

### 6-3. Switch가 LAN의 핵심 장비다

```
MAC Address 학습
    → 목적지 Port 전달
    → 내부 통신 최적화
```

Switch가 LAN 트래픽을 제어합니다.

### 6-4. LAN 내부는 매우 빠르고 지연이 낮다

| 특징 | 설명 |
|------|------|
| Low Latency | 짧은 거리 |
| High Throughput | 고속 링크 |
| Local Routing 최소화 | 내부 전달 |

내부 통신 최적화 구조입니다.

### 6-5. 인터넷과 LAN은 다르다

| 구분 | LAN | Internet/WAN |
|------|-----|-------------|
| 범위 | 내부망 | 광역망 |
| 관리 주체 | 개인/회사 | ISP |
| 속도 | 매우 빠름 | 상대적으로 느림 |
| 목적 | 내부 연결 | 외부 연결 |

> 인터넷은 **수많은 LAN들이 연결된 거대한 WAN**입니다.

---

## 7. Linux / Runtime / Kubernetes에서 관측 방법

### 네트워크 인터페이스 확인

```bash
ip addr
ip link
```

**관측 가능:** NIC, LAN IP, MTU, Link 상태

### ARP/L2 상태 확인

```bash
arp -a
ip neigh
```

**관측 가능:** MAC Address, L2 Neighbor

### Switch/LAN 상태 확인

```bash
ethtool eth0
```

**관측 가능:** Link Speed, Duplex, Auto-negotiation

### Packet 확인

```bash
tcpdump
wireshark
```

**관측 가능:** Ethernet Frame, Broadcast, ARP, VLAN

### Kubernetes

K8s 내부 네트워크 대부분도 LAN 기반입니다.

```
  Pod
   ↕
  Node
   ↕
 Switch
   ↕
  LAN
   ↕
Other Node
```

Cluster 내부 통신도 **LAN 위에서 동작**합니다.

### Observability 도구

현대 시스템에서는 다음으로 LAN 상태를 추적합니다:

- Prometheus
- eBPF
- Switch telemetry
- NetFlow
- tcpdump

### 대표 메트릭

| 메트릭 | 의미 |
|--------|------|
| Latency | 내부 지연 |
| Packet Loss | 패킷 유실 |
| Throughput | 처리량 |
| Broadcast Rate | 브로드캐스트 |
| Error Rate | NIC/Switch 오류 |
| Link Utilization | 링크 사용률 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*