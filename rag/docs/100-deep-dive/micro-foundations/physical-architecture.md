# 물리적 아키텍처 (Physical Architecture)

> 정독: 0회

## 1. 이 기술이 무엇인가

**물리적 아키텍처(Physical Architecture)** 는:

> 시스템을 구성하는 실제 하드웨어 부품들이 어떤 구조로 배치·연결·상호작용하는지를 정의한 물리적 시스템 구조

### 핵심 의미

논리 설계가 **무엇이 존재해야 하는가**를 정의한다면, 물리적 아키텍처는 **그것이 실제 하드웨어로 어떻게 배치되는가**를 정의합니다.

### 포함 대상

| 구성 요소 | 예시 |
|---|---|
| 연산 장치 | CPU, GPU, accelerator |
| 메모리 | DRAM, SRAM, NUMA node |
| 저장 장치 | SSD, HDD, NAND flash |
| 네트워크 | NIC, switch, cable |
| 버스 | PCIe, memory bus, NVMe |
| 전원/냉각 | PSU, VRM, thermal design |

물리적 아키텍처는 **실제 성능·지연·전력·장애 특성을 결정하는 하드웨어 배치 구조**입니다.

---

## 2. 시스템 어디에서 등장하는가

물리적 아키텍처는 모든 컴퓨팅 환경의 최하단에 존재합니다.

### 단일 서버

```
CPU
  → memory controller
    → DRAM
      → PCIe bus
        → NVMe SSD
          → NIC
```

### 스토리지 장치 내부

```
Host Interface
  → SSD Controller
    → DRAM Cache
      → NAND Channel
        → NAND Die/Plane/Block/Page
```

### 네트워크 장비

```
PHY
  → MAC
    → switching ASIC
      → port
        → cable/fiber
```

### 데이터센터

```
Rack
  → ToR switch
    → aggregation switch
      → spine switch
        → backbone link
```

### 클라우드 / Kubernetes

상위에서는 논리 객체로 보이지만 실제로는 물리 노드, NIC, SSD, rack topology, power domain, availability zone 위에 배치됩니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

물리적 아키텍처는 CPU / Memory / Network / Disk 전체에 직접 영향을 줍니다.

- **CPU**: core topology, cache hierarchy, NUMA distance, thermal throttling
- **Memory**: memory channel 수, DRAM latency, NUMA locality, bandwidth ceiling
- **Network**: NIC speed, PCIe lane, switch topology, cable/fiber quality
- **Disk**: SSD controller 구조, NAND channel 수, PCIe generation, queue depth, thermal design

> 핵심: 같은 소프트웨어라도 **물리적 아키텍처 차이로 성능과 장애 양상이 완전히 달라질 수 있습니다.**

---

## 4. 왜 중요한가

물리적 아키텍처는 시스템의 실제 한계를 결정합니다.

| 항목 | 영향 |
|---|---|
| 최대 처리량 | bus, channel, lane 수 |
| 지연 시간 | 거리, 계층, hop 수 |
| 안정성 | 이중화, 전원, 냉각 |
| 확장성 | rack/network/storage 구조 |
| 장애 범위 | failure domain |

상위 추상화는 물리 구조를 숨기지만 **물리적 제약 자체를 제거하지는 못합니다.**

**예시**
- CPU는 충분해도 메모리 대역폭이 병목
- SSD는 빠르지만 PCIe lane 부족
- 네트워크는 10Gbps지만 switch uplink oversubscription
- Pod는 정상이나 같은 노드의 disk pressure로 장애

---

## 5. 실제 장애와 어떤 관련이 있는가

물리적 아키텍처 문제는 운영 장애의 근본 원인이 될 수 있습니다.

### 대표 장애 유형

**NUMA Misplacement**
- CPU와 메모리가 다른 NUMA node에 배치되어 latency 증가

**PCIe Bottleneck**
- SSD/NIC가 같은 PCIe root complex를 공유하여 대역폭 경합

**Thermal Throttling**
- 냉각 한계로 CPU/SSD 성능 저하

**Single Point of Failure**
- 전원, NIC, switch, disk 경로가 이중화되지 않음

**Rack / AZ Failure Domain**
- 같은 물리 구역에 서비스가 몰려 장애 전파

### Kubernetes 환경

| 증상 | 물리적 원인 |
|---|---|
| Pod latency 증가 | node NUMA / disk / NIC 병목 |
| PV I/O 지연 | 실제 storage backend 병목 |
| network timeout | switch/NIC/link 문제 |
| node NotReady | 전원/디스크/커널/네트워크 장애 |

> SRE 관점에서는 **논리 배치와 물리 배치를 함께 봐야 합니다.**

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심 메커니즘은 **논리 명령이 실제 하드웨어 경로를 따라 실행된다**는 것입니다.

### 스토리지 예시

```
write(file)
  → filesystem
    → block layer
      → NVMe driver
        → PCIe bus
          → SSD controller
            → NAND channel
              → NAND page program
```

### 네트워크 예시

```
send(packet)
  → TCP/IP stack
    → NIC driver
      → PCIe DMA
        → NIC PHY
          → cable/fiber
            → switch port
```

### 메모리 예시

```
load/store
  → virtual address translation
    → memory controller
      → DRAM channel
        → row/column access
```

상위 명령은 단순하지만 **실제 실행 경로는 물리 장치·버스·채널·토폴로지를 통과**합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# CPU / NUMA
lscpu
numactl --hardware

# PCIe 토폴로지
lspci -tv

# 블록 장치
lsblk
nvme list

# 디스크 성능
iostat -x 1

# 네트워크 장치
ip link
ethtool <interface>

# 하드웨어 이벤트
dmesg
journalctl -k
```

### Runtime 관측 포인트

- CPU throttling
- IO wait
- disk queue depth
- NIC drop
- NUMA remote access
- PCIe error
- thermal event

### Kubernetes

```bash
# 노드 상태
kubectl describe node
kubectl top node
```

**장애 신호**: DiskPressure, MemoryPressure, NetworkUnavailable, NodeNotReady, Pod eviction

### 중요한 관측 관점

> Pod/Service/PV 같은 논리 객체가 **어느 물리 노드·디스크·NIC·rack 위에서 동작하는지** 반드시 확인해야 합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*