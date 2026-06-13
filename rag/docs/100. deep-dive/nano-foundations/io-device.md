# 입출력 장치 (I/O Device, Input/Output Device)

> 정독: 0회

## 1. 이 기술이 무엇인가

입출력 장치(I/O Device)는:

> CPU·메모리(Host) 외부에서 **데이터 입력·출력·저장을 수행하는 모든 하드웨어 장치**

**대표 예시:**
- SSD / HDD / NVMe
- NIC (Network Interface Card)
- Keyboard / Mouse
- GPU
- USB Device
- Monitor
- Printer

**핵심 특징:**
입출력 장치는 CPU처럼 직접 프로그램 실행을 하지 않고, 특정 기능에 특화되어 있으며, 운영체제 커널과 디바이스 드라이버를 통해 제어됩니다.

**핵심 구조:**
```
Application
  → Kernel
    → Device Driver
      → Device Controller
        → I/O Device
```

---

## 2. 시스템 어디에서 등장하는가

모든 컴퓨터 시스템에서 등장합니다.

| 영역 | 장치 |
|---|---|
| **Storage System** | SSD, HDD, NVMe, RAID Controller |
| **Network System** | Ethernet NIC, Wi-Fi Adapter, SmartNIC |
| **Input System** | keyboard, touchpad, USB HID |
| **Output System** | display controller, audio device, printer |
| **Cloud / Datacenter** | storage array, SAN, NVMe-oF, hardware load balancer |
| **Kubernetes / Server** | CSI storage, CNI NIC device, GPU passthrough, SR-IOV NIC |

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

장치 종류에 따라 다릅니다.

| 장치 | 주요 영향 자원 |
|---|---|
| **Storage Device** | Disk + Memory + CPU |
| **Network Device** | Network + CPU |
| **GPU** | PCIe bandwidth + Memory |

**공통 핵심:** 모든 I/O 장치는 CPU와 메모리 자원을 사용하여 호스트와 데이터를 교환합니다.

특히 중요한 요소:
- DMA
- interrupt
- queue
- bus bandwidth
- cache coherence

---

## 4. 왜 중요한가

컴퓨터 시스템은 결국 CPU 연산 결과를 외부 세계와 연결해야 합니다.

**입출력 장치가 없다면:**
- 데이터 저장 불가
- 네트워크 통신 불가
- 사용자 입력 불가
- 화면 출력 불가

**현대 시스템에서 중요한 이유:** 성능 병목 대부분이 I/O 장치에서 발생합니다.

- SSD latency
- NIC packet loss
- PCIe saturation
- storage queue congestion

> **핵심:** I/O 장치는 컴퓨터 시스템과 외부 세계를 연결하는 실제 데이터 통로입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

**Storage Device 장애:**
- disk timeout
- I/O error
- fsync stall
- NVMe reset
- disk queue saturation

**Network Device 장애:**
- packet drop
- RX/TX overflow
- NIC reset
- interrupt imbalance

**USB / Peripheral 장애:**
- device disconnect
- driver hang
- firmware issue

**Kubernetes 장애:**

| 증상 | 원인 |
|---|---|
| node not ready | 스토리지/NIC 장애 영향 |
| pod network failure | NIC/CNI 문제 |
| volume mount failure | CSI + storage device 문제 |

**Datacenter 장애:**
- PCIe bus failure
- RAID controller failure
- SAN latency spike

> **핵심:** I/O 장치 문제는 애플리케이션이 아니라 노드 전체 장애로 확대될 수 있습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심 메커니즘은 6개입니다.

**1. Device Controller**
모든 I/O 장치는 컨트롤러(controller)를 통해 제어됩니다: NVMe controller / NIC controller / USB controller

**2. Device Register**
커널이 장치 상태를 제어합니다: status register / control register / data register

**3. MMIO / Port I/O**
CPU가 장치와 통신하는 방식

**4. Interrupt**
장치 완료 시 CPU 통보

**5. DMA**
장치 ↔ 메모리 직접 전송. CPU 부담 감소

**6. Device Driver**
커널과 장치 사이 중간 계층

```
Kernel ↔ Driver ↔ Hardware
```

**전체 흐름:**
```
User Process
  → System Call
    → Kernel I/O Subsystem
      → Device Driver
        → Device Controller
          → Physical I/O Device
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 장치 목록
lsblk
lspci
lsusb

# 디바이스 파일
ls /dev

# 장치 상태
dmesg
journalctl -k

# interrupt 상태
cat /proc/interrupts

# block device 정보
cat /sys/block/*

# network device 정보
ip link
ethtool
```

### Runtime

```bash
# container device mapping
docker inspect
```

GPU/NIC passthrough — runtime device plugin을 통해 확인합니다.

### Kubernetes

```bash
# node device 확인
kubectl describe node
```

**device plugin:**
- GPU plugin
- SR-IOV plugin

**CSI/CNI** — 스토리지·네트워크 장치 연결 상태를 확인합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*