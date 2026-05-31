# 하드웨어 (Hardware)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**하드웨어(Hardware)** 는:

> 실제 전기를 사용하여 데이터를 **저장·전송·연산하는 물리적 장치 전체**

쉽게 말하면 CPU, Memory(RAM), SSD/NVMe, NIC, Mainboard, Power Supply, GPU 같은 **실제 장비**들이다.

소프트웨어가 **무엇을 할지 결정하는 논리**라면, 하드웨어는 **그 논리를 실제 전기 신호로 수행하는 물리 계층**이다.

<details>
<summary>Deep Dive</summary></br>

Data(데이터) [[M]](../../100-deep-dive/micro-foundations/data.md)

</details></br>

## 2. 시스템 어디에서 등장하는가

하드웨어는 **시스템 전체의 가장 아래 계층**이다.

### Compute Layer
연산 담당: `CPU`, `GPU`, `NPU`, `FPGA`

### Memory Layer
데이터 임시 저장: `Register`, `Cache`, `RAM`

### Storage Layer
데이터 영구 저장: `SSD`, `NVMe`, `HDD`

### Network Layer
데이터 전송 담당: `NIC`, `PCIe Bus`, `Switch`, `Router`

### Power/Thermal Layer
전력과 열 안정성 담당: `VRM`, `PSU`, `Cooling`, `Fan`

### Platform Layer
전체 연결 구조 담당: `Mainboard`, `PCIe Lane`, `NUMA`, `Chipset`

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

하드웨어는 **모든 자원의 실체**다.

| 자원 | 하드웨어 구성 요소 |
|------|-----------------|
| CPU | ALU, Core, Register, Cache |
| Memory | DRAM, DIMM, Memory Controller |
| Network | NIC, PHY, PCIe, DMA Engine |
| Disk | NAND Flash, NVMe Controller, RAID Controller |

> **CPU/Memory/Network/Disk 자체가 하드웨어**이다.

---

## 4. 왜 중요한가

소프트웨어는 결국 **하드웨어 위에서만 실행 가능**하다.

### FinTech에서 특히 중요한 이유

결제 시스템은 Low Latency, High Availability, Data Integrity, Predictable Performance가 필수다.

그런데 실제 병목은 하드웨어 레벨에서 발생한다:

- CPU Saturation
- Memory Stall
- PCIe Bottleneck
- NIC Interrupt Storm
- Thermal Throttling
- NVMe Saturation

> ⚠️ **하드웨어 한계가 시스템 한계**가 되는 경우가 매우 많다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### CPU Thermal Throttling
온도가 너무 높아져 클럭 자동 하락

결과: TPS 감소, P99 증가, Timeout 증가

### ECC Memory Error
메모리 비트 오류 발생

결과: Kernel Panic, JVM Crash, Data Corruption

### NVMe Latency Spike
SSD 내부 GC 또는 Wear Leveling

결과: DB Commit 지연, Kafka Flush 지연, API Timeout

### NIC Interrupt Storm
패킷 인터럽트가 특정 코어에 집중

결과: SoftIRQ 폭증, CPU 100%, Network Latency 증가

### PCIe Link Downshift
`x16 → x8` 하향 동작

결과: GPU/NIC Throughput 감소, RDMA 성능 저하

### Power Delivery Instability
VRM/PSU 불안정

결과: Random Reboot, Machine Check Exception, 시스템 불안정

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

### Hardware = 전기 신호 시스템
모든 하드웨어는 결국 **전압 변화**를 이용한다.

### CPU
명령 실행. 내부 핵심: `ALU`, `Register`, `Cache`, `Pipeline`

### Memory
CPU가 바로 사용할 작업 공간. 속도 중요.

### Storage
영구 저장. 속도보다 **durability / persistence** 중요.

### NIC
네트워크 패킷 입출력 담당. DMA + Interrupt 기반 동작.

### Bus / Interconnect
컴포넌트 연결 통로. 예: `PCIe`, `UPI`, `Infinity Fabric`

### Hardware Hierarchy
속도가 느린 계층 접근이 많아질수록 **Latency 증가**:

```
Register
→ L1/L2/L3 Cache
→ RAM
→ NVMe
→ Network
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### CPU
```bash
top
mpstat -P ALL 1
lscpu
```

### Memory
```bash
free -h
vmstat 1
numactl --hardware
```

### Disk
```bash
iostat -x 1
nvme list
```

### PCIe / Bus
```bash
lspci
lspci -vv
```

### NIC
```bash
ethtool eth0
ip -s link
cat /proc/interrupts
```

### Thermal / Power
```bash
sensors
dmesg | grep -i thermal
```

### Kernel Hardware Error
```bash
dmesg
journalctl -k
```

### Kubernetes 대표 관측 지표
- `Node Pressure`
- `CPU Throttling`
- `NUMA Imbalance`
- `Network Packet Drop`
- `Disk I/O Wait`

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*