# IEEE 802.3 이더넷 아키텍처 규격 (Ethernet Architecture Specifications)
## 1. IEEE 802.3 이더넷 아키텍처 규격이란 무엇인가

> 정독: 0회

IEEE 802.3 이더넷 아키텍처 규격은:

> Ethernet 네트워크에서 데이터가 소프트웨어 영역에서 실제 전기/광 신호로 변환되어 전달되기까지의 **전체 구조와 계층 분리 규칙을 정의한 설계 체계**

즉 단순히 랜선 규격·프레임 포맷만 정의하는 것이 아니라:

```
상위 데이터
→ MAC 처리
→ PHY 변환
→ 전기/광 신호 출력
```

까지의 전체 흐름을 정의합니다.

핵심은:

> **"Ethernet 네트워크 내부 구조 전체의 공식 설계도"**

입니다.

---

## 2. 시스템 어디에서 등장하는가

IEEE 802.3 아키텍처는 거의 모든 유선 네트워크 시스템에서 등장합니다.

| 환경 | 사용 |
|---|---|
| PC Ethernet | 사용 |
| 서버 NIC | 사용 |
| 데이터센터 스위치 | 사용 |
| Hypervisor Network | 사용 |
| Kubernetes Node | 사용 |
| Cloud Infrastructure | 사용 |
| NAS/SAN 일부 | 사용 |

**실제 흐름:**

```
Application
→ TCP/IP
→ MAC
→ PHY
→ Cable/Fiber
→ Switch
→ Other Node
```

즉 Ethernet 아키텍처는 OS · NIC · PHY Chip · Switch ASIC · Cable 전체를 연결합니다.

---

## 3. 어떤 자원(CPU / Memory / Network / Disk)에 가장 영향이 큰가

IEEE 802.3 아키텍처는 **Network 자원**과 가장 직접 연결됩니다.

| 자원 | 영향도 |
|---|---|
| Network | 매우 큼 |
| CPU | 중간 |
| Memory | 중간 |
| Disk | 거의 없음 |

**Network 영향**

```
Ethernet Signaling · Link Negotiation · Frame Delivery · Switching · PHY Encoding
```

즉 실제 네트워크 전송 기반입니다.

**CPU 영향**

```
Packet Processing · Interrupt · DMA · Checksum Offload
```

고속 NIC에서는 CPU와 직접 연결됩니다.

**Memory 영향**

```
NIC Ring Buffer · DMA Memory · RX/TX Queue
```

즉 프레임 버퍼 처리와 연결됩니다.

---

## 4. 왜 중요한가

현대 유선 네트워크 전체가 이 아키텍처 위에서 동작하기 때문입니다.

> 전 세계 Ethernet 장비들이 서로 호환되어 통신 가능한 이유는  
> **IEEE 802.3 아키텍처 규격을 공유하기 때문이다.**

| 효과 | 설명 |
|---|---|
| 상호 호환성 | 제조사 무관 연결 |
| 계층 분리 | MAC/PHY 독립 |
| 확장성 | 속도 업그레이드 가능 |
| 안정성 | 표준 기반 통신 |
| 대규모 네트워크 구축 | 데이터센터 가능 |

**매우 중요한 핵심**

IEEE 802.3의 가장 위대한 설계 철학:

> **MAC과 PHY를 분리했다**

즉 위는 데이터 처리, 아래는 물리 신호 처리를 **독립적으로 설계**할 수 있습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 네트워크 장애 상당수가 이 아키텍처 계층 문제와 연결됩니다.

| 장애 | 설명 |
|---|---|
| PHY Link Failure | 물리 링크 실패 |
| Duplex Mismatch | 링크 협상 실패 |
| CRC Error | 프레임 손상 |
| Auto Negotiation Failure | 속도 협상 실패 |
| Packet Drop | Queue Overflow |
| MTU Mismatch | 프레임 크기 문제 |
| Bad Cable | PHY 오류 |
| Switch ASIC Fault | 프레임 전달 장애 |

**매우 중요한 실무 포인트**

> 애플리케이션 장애처럼 보여도  
> **실제로는 Ethernet PHY/MAC 계층 문제인 경우가 매우 많다.**

예를 들어 Kubernetes Node Disconnect · API Timeout · DB Replication Delay · VM Network Loss의 실제 원인이 CRC 증가 · PHY 불안정 · Duplex 문제인 경우가 많습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 6-1. Ethernet은 계층적으로 분리된다

```
LLC
↓
MAC
↓
RS
↓
MII
↓
PHY
↓
MDI
↓
Cable
```

즉 Ethernet은 단일 기능이 아니라 **여러 계층의 협력 구조**입니다.

### 6-2. MAC은 데이터 전달 규칙 담당

| 기능 | 설명 |
|---|---|
| MAC Address | 주소 처리 |
| Frame 생성 | 프레임 포장 |
| CRC/FCS | 오류 검출 |
| Frame 송신 | L2 전달 |

즉 **MAC = 논리적 Ethernet 처리 계층**입니다.

### 6-3. PHY는 실제 물리 신호 변환 담당

| 기능 | 설명 |
|---|---|
| Encoding | 비트 → 전기신호 |
| Decoding | 전기신호 → 비트 |
| Link Training | 링크 협상 |
| Signal Recovery | 신호 복원 |

즉 **PHY = 실제 물리 통신 엔진**입니다.

### 6-4. MII는 MAC↔PHY 인터페이스다

MII(Media Independent Interface)는 MAC 칩과 PHY 칩 사이의 **표준 내부 연결 인터페이스**입니다.

대표 확장: `MII` · `GMII` · `RGMII` · `SGMII` · `XGMII`

### 6-5. Media Independence가 핵심 철학이다

> MAC은 그대로 유지하고 **PHY만 교체 가능**

| PHY 종류 | 매체 |
|---|---|
| Copper PHY | 랜선 |
| Fiber PHY | 광케이블 |

덕분에 상위 소프트웨어는 변경 없이 속도만 증가시킬 수 있습니다.

### 6-6. NIC 내부에 MAC/PHY가 존재한다

```
OS Driver
↓
MAC Controller
↓
PHY Chip
↓
RJ45/Fiber
```

즉 NIC는 **IEEE 802.3 아키텍처의 실제 구현체**입니다.

---

## 7. Linux / Runtime / Kubernetes에서 어디서 관측되는가

**NIC 인터페이스 확인**

```bash
ip link
```

관측 가능: NIC 상태 · MTU · MAC

**PHY/Link 상태 확인**

```bash
ethtool eth0
```

관측 가능: Speed · Duplex · PHY Link · Auto Negotiation

**Driver 상태 확인**

```bash
ethtool -i eth0
```

관측 가능: Driver · Firmware · Bus Info

**Frame 분석**

```bash
tcpdump -e
```

관측 가능: Ethernet Header · MAC Address · VLAN Tag

**Linux PHY Layer**

```bash
dmesg
```

대표 로그: Link Up · Link Down · PHY Reset

**Kubernetes**

K8s도 결국 Ethernet 기반입니다.

```
Pod → veth → Bridge → NIC(MAC/PHY) → Ethernet → Switch
```

즉 Overlay 아래 **실제 Ethernet PHY가 존재**합니다.

**Observability**

현대 운영에서는 `NIC Metrics` · `Switch Telemetry` · `eBPF` · `PHY Diagnostics` 등으로 Ethernet 아키텍처 상태를 추적합니다.

| 메트릭 | 의미 |
|---|---|
| CRC Error | PHY/Signal 오류 |
| RX/TX Drop | Queue 문제 |
| Link Flap | PHY 불안정 |
| FCS Error | Frame 손상 |
| Negotiation Failure | 링크 협상 실패 |
| Packet Loss | 전송 실패 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*