# 데이터 링크 계층 (Data Link Layer, Layer 2)
## 1. 데이터 링크 계층(Data Link Layer)이란 무엇인가

> 정독: 0회

데이터 링크 계층(L2)은:

> 물리적으로 연결된 장치들 사이에서 데이터를 안정적으로 전달하기 위한 L2 프로토콜 계층

"물리 네트워크 위에서 데이터를 안전하게 전달하기 위한 근거리 통신 제어 계층"

OSI 7계층 기준:

| 계층 | 역할 |
|---|---|
| L1 | 물리 신호 전달 |
| L2 | 프레임 전달/MAC 제어 |
| L3 | IP 라우팅 |

즉 L2는 **"직접 연결된 이웃 장치 간의 안전한 데이터 전달"** 을 담당합니다.

<details>
<summary>Deep Dive</summary></br>

Node(노드) [[M]](../../100-deep-dive/micro-foundations/node.md)  
Data(데이터) [[M]](../../100-deep-dive/micro-foundations/data.md)  


</details></br>

## 2. 시스템 어디에서 등장하는가

데이터 링크 계층은 Ethernet/Wi-Fi 기반 네트워크 거의 모든 곳에 등장합니다.

**대표 흐름:**

```
Application
→ TCP/IP
→ Ethernet Frame (L2)
→ NIC
→ Switch
→ Other Host
```

**대표 등장 환경:**

| 환경 | L2 사용 |
|---|---|
| Ethernet LAN | 사용 |
| Wi-Fi | 사용 |
| Switch Network | 사용 |
| VLAN | 사용 |
| Hypervisor Bridge | 사용 |
| Kubernetes Node Network | 사용 |

즉 현대 내부망 대부분은 **L2 기반**입니다.

---

## 3. 어떤 자원(CPU / Memory / Network / Disk)에 가장 영향이 큰가

데이터 링크 계층은 **Network 자원**과 가장 직접 연결됩니다.

| 자원 | 영향도 |
|---|---|
| Network | 매우 큼 |
| CPU | 중간 |
| Memory | 중간 |
| Disk | 거의 없음 |

**Network 영향**

```
Ethernet Frame · MAC Delivery · VLAN · Switching · Broadcast
```

즉 네트워크 내부 전달 자체입니다.

**CPU 영향**

```
Packet Processing · Interrupt 처리 · Frame Validation
```

특히 고속 NIC에서는 CPU 부하와 연결됩니다.

**Memory 영향**

```
NIC Buffer · DMA Buffer · Packet Queue
```

즉 프레임 버퍼 처리와 연결됩니다.

---

## 4. 왜 중요한가

네트워크 내부 통신 대부분이 L2 위에서 동작하기 때문입니다.

> 같은 LAN 내부 장치들이 빠르고 안정적으로 통신할 수 있는 이유는  
> **L2(Data Link Layer)가 존재하기 때문이다.**

**대표 역할:**

| 역할 | 설명 |
|---|---|
| 프레이밍 | 데이터 포장 |
| MAC 전달 | 이웃 장치 식별 |
| 오류 검출 | CRC/FCS |
| 흐름 제어 | 속도 조절 |
| 스위칭 | L2 전달 |

즉 L2는 **근거리 네트워크의 실질적인 전달 계층**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 네트워크 장애 상당수가 L2 문제와 연결됩니다.

**대표 사례:**

| 장애 | 원인 |
|---|---|
| Broadcast Storm | L2 Loop |
| MAC Flapping | 스위치 경로 불안정 |
| VLAN Misconfiguration | 네트워크 단절 |
| CRC Error | Frame 손상 |
| ARP 문제 | MAC 학습 실패 |
| STP Loop | 무한 프레임 순환 |
| Packet Drop | Buffer Overflow |

**특히 중요한 점:**

> IP 계층(L3) 문제처럼 보여도  
> 실제로는 **L2(Frame/MAC/Switch) 문제인 경우가 많다.**

대표 사례: Kubernetes Node 간 통신 실패 · VM Network 불안정 · DB Cluster 단절 · API Timeout

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 6-1. 데이터는 "프레임(Frame)" 단위로 이동한다

```
Ethernet Header + Payload + FCS
```

즉 L2의 기본 전송 단위는 **Frame**입니다.

### 6-2. MAC Address 기반으로 전달된다

```
Destination MAC
Source MAC
```

Switch는 MAC 주소를 기반으로 포트를 결정합니다.  
즉 **"같은 LAN 내부 전달"** 을 담당합니다.

### 6-3. L2는 직접 연결된 구간만 담당한다

- **L2** = 근거리 이웃 통신
- **L3** = 다른 네트워크로 라우팅

### 6-4. 오류 검출(FCS/CRC)을 수행한다

```
Frame 수신 → CRC 검사 → 오류 발견 시 Drop
```

즉 깨진 프레임을 제거합니다.

### 6-5. Switch가 대표적인 L2 장비다

```
Frame 수신 → MAC Table 조회 → 특정 Port 전달
```

즉 Switch는 L2 기반 장비입니다.

### 6-6. VLAN도 L2 기술이다

VLAN은 하나의 물리 네트워크를 **논리적으로 분리**하는 기술입니다.  
즉 **L2 네트워크 분할 기술**입니다.

---

## 7. Linux / Runtime / Kubernetes에서 어디서 관측되는가

**MAC 주소 확인**

```bash
ip link
```

관측 가능: MAC Address · MTU · NIC 상태

**ARP/L2 Neighbor 확인**

```bash
arp -a
ip neigh
```

관측 가능: MAC 학습 상태 · Neighbor 정보

**Bridge/VLAN 확인**

```bash
bridge link
bridge vlan
```

관측 가능: Linux Bridge · VLAN 정보

**Ethernet Frame 캡처**

```bash
tcpdump -e
wireshark
```

관측 가능: Source MAC · Destination MAC · VLAN Tag · EtherType

**Switch/L2 상태 확인**

```bash
ethtool eth0
```

관측 가능: Link 상태 · Duplex · Error 상태

**Kubernetes**

K8s 내부 네트워크도 결국 L2 기반입니다.

```
Pod → veth → Linux Bridge → Switch → Other Node
```

즉 `veth` · `bridge` · `overlay network` 모두 L2와 연결됩니다.

**Observability**

현대 시스템에서는 `tcpdump` · `Wireshark` · `eBPF` · `switch telemetry` · `bridge metrics` 등으로 L2 상태를 추적합니다.

**대표 메트릭:**

| 메트릭 | 의미 |
|---|---|
| CRC Error | Frame 오류 |
| Broadcast Rate | 브로드캐스트 |
| MAC Table Usage | MAC 학습 상태 |
| Packet Drop | 프레임 유실 |
| Link Flap | 링크 불안정 |
| STP Event | 루프 제어 이벤트 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*