# 하드웨어 컨트롤러 (Hardware Controller / Device Controller)

> 정독: 0회

## 1. 이 기술이 무엇인가

**하드웨어 컨트롤러(Device Controller)** 는:

> 운영체제와 실제 하드웨어 장치 사이에서 장치의 물리 동작을 직접 제어하는 **전용 제어 회로 및 임베디드 프로세서**

CPU가 장치의 모든 물리 동작을 직접 수행하지 않고, 컨트롤러가 중간에서 장치 제어를 전담합니다.

### 대표 예시

- SSD Controller
- NVMe Controller
- SATA Controller
- RAID Controller
- NIC (Network Interface Controller)
- USB Controller
- GPU Command Processor

### 핵심 역할

컨트롤러는 **커널 명령 → 장치 내부 실제 동작**으로 변환합니다.

예: NAND page program, packet transmission, DMA transfer, queue execution, interrupt signaling

---

## 2. 시스템 어디에서 등장하는가

거의 **모든 I/O 장치 내부**에 존재합니다.

### Storage

**SSD** — 내부에 SSD controller 존재.
- Flash Translation Layer (FTL), wear leveling, garbage collection, ECC

**HDD**
- spindle motor control, actuator arm positioning, sector read/write

### Network

**NIC controller**
- packet DMA, checksum offload, queue management, interrupt generation

### USB

**USB host controller**
- endpoint scheduling, transfer coordination

### GPU

**GPU command controller**
- command queue execution, memory scheduling

### Virtualization

- virtio controller
- SR-IOV virtual function controller

---

## 3. 어떤 자원에 가장 영향이 큰가

장치 종류에 따라 다르지만 일반적으로 **Disk + Network + Memory Bus** 영향이 큽니다.

| 자원 | 영향 | 주요 항목 |
|------|------|-----------|
| **Storage Controller** | 매우 큼 | latency, IOPS, throughput |
| **NIC Controller** | 큼 | packet throughput, PPS, network latency |
| **Memory** | 매우 중요 | DMA로 컨트롤러가 메모리 직접 접근 |
| **CPU** | 부담 감소 | checksum offload, DMA offload, queue offload |

> **핵심:** 컨트롤러는 CPU 대신 장치 내부 물리 작업을 수행합니다.

---

## 4. 왜 중요한가

현대 컴퓨터 성능 상당수가 **컨트롤러 품질**에 의해 결정됩니다.

**이유 1. CPU 보호**
CPU가 모든 장치 타이밍을 직접 제어하면 시스템 전체가 비효율적. 컨트롤러가 물리 제어를 대행.

**이유 2. 병렬성 제공**
현대 NVMe SSD는 다중 queue, 병렬 NAND channel, asynchronous completion을 지원 — 모두 컨트롤러가 수행.

**이유 3. 오류 처리**
컨트롤러가 ECC, retry, bad block handling 수행.

**이유 4. 성능 최적화**
NIC offload, storage caching, queue batching 등.

> **핵심:** 현대 I/O 성능은 CPU보다 **컨트롤러 구조 영향이 매우 큽니다**.

---

## 5. 실제 장애와 어떤 관련이 있는가

### Storage 장애

| 장애 | 증상 |
|------|------|
| SSD controller failure | I/O timeout, disk disappearance, filesystem readonly |
| RAID controller issue | array degraded, write cache loss |

### Network 장애

| 장애 | 증상 |
|------|------|
| NIC controller bug | packet drop, retransmission 증가, RX/TX queue stall |
| interrupt issue | CPU softirq 폭증, network latency 증가 |

### Firmware 문제

컨트롤러 firmware bug는 매우 위험합니다.
- SSD brick
- NVMe reset loop
- data corruption

### Kubernetes / Cloud

- **node not ready** — storage/network controller 문제 가능
- **container runtime timeout** — storage controller stall 가능

> **핵심:** 장치 장애 상당수는 실제 저장매체보다 **컨트롤러 문제인 경우가 많습니다**.

---

## 6. 핵심 메커니즘

### 1. Register Interface
컨트롤러는 레지스터(control / status / data register)를 제공하며, 드라이버가 MMIO 방식으로 접근합니다.

### 2. DMA Engine
컨트롤러가 메모리에 직접 접근하여 CPU 개입을 최소화합니다.

### 3. Queue Processing
- NVMe submission/completion queue
- NIC RX/TX ring

### 4. Interrupt Generation
작업 완료 시 interrupt를 발생시킵니다.

### 5. Firmware Execution
현대 컨트롤러는 내부 firmware를 실행하며, 사실상 작은 embedded system 수준입니다.

### 6. Address Translation
SSD controller는 **LBA → NAND physical page** 변환을 수행합니다. (FTL 핵심 역할)

### 전체 흐름

```
Application
    → Kernel
    → Device Driver
    → Controller Register
    → DMA / Queue Execution
    → Physical Device Operation
    → Interrupt Completion
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

| 목적 | 명령어 |
|------|--------|
| PCI 장치 확인 | `lspci` |
| 상세 장치 정보 | `lspci -vv` |
| NVMe controller 확인 | `nvme list` |
| block device 확인 | `lsblk` |
| interrupt 상태 | `cat /proc/interrupts` |
| driver ↔ controller mapping | `lspci -k` |
| 커널 로그 | `dmesg`, `journalctl -k` |

### Network

| 목적 | 명령어 |
|------|--------|
| NIC controller 정보 | `ethtool -i eth0` |

### Kubernetes

- **node storage issue** — `kubelet log`, `CSI log`, `dmesg` 확인
- **network issue** — CNI 문제처럼 보여도 실제 NIC controller 문제일 수 있음

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*