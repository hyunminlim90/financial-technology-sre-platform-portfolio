# Lane (레인)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**Lane** 은 PCIe와 같은 고속 직렬 인터커넥트에서 데이터를 송수신하는 **최소 물리 전송 단위**이다.

### 1 Lane의 물리 구조

하나의 Lane은 다음으로 구성된다.

| 방향 | 구성 | 선 수 |
|------|------|-------|
| TX (송신) | 차동 쌍 (Differential Pair) | 2선 |
| RX (수신) | 차동 쌍 (Differential Pair) | 2선 |

```
1 Lane  =  TX 2선 + RX 2선  =  총 4개 물리 신호선
```

---

## 2. 시스템 어디에서 등장하는가

현대 서버 대부분의 고속 장치 연결에 사용된다.

### PCIe NIC

```
CPU  ↔  PCIe Lane  ↔  10G / 25G / 100G NIC
```

### NVMe SSD

```
CPU  ↔  PCIe Lane  ↔  NVMe SSD
```

### GPU / FPGA

```
CPU  ↔  PCIe x16  ↔  GPU / FPGA
```

### CPU Socket 간 인터커넥트

```
Socket 0  ↔  UPI / Infinity Fabric Lane  ↔  Socket 1
```

<details>
<summary>Deep Dive</summary></br>

PCIe NIC(PCI Express Network Interface Card) [[M]](../../100-deep-dive/micro-foundations/pcie-nic.md)

</details></br>

## 3. 어떤 자원에 가장 영향이 큰가

Lane은 **I/O Throughput** 에 가장 직접적인 영향을 준다.

| 자원 | 영향 |
|------|------|
| Network | NIC 대역폭 |
| Disk | NVMe 처리량 |
| Memory | DMA Traffic |
| CPU | Interrupt / NUMA |

---

## 4. 왜 중요한가

Lane 수는 장치가 사용할 수 있는 **최대 물리 대역폭**을 결정한다.

### Link Width 구성

| 구성 | Lane 수 | 주요 용도 |
|------|---------|-----------|
| PCIe x1 | 1 Lane | 저속 장치 |
| PCIe x4 | 4 Lane | NVMe SSD |
| PCIe x8 | 8 Lane | 고속 NIC |
| PCIe x16 | 16 Lane | GPU / 초고속 NIC |

### Link Downgrade 발생 시 영향

```
PCIe x16  →  x8 downgrade
 ├── Throughput 감소
 ├── Tail Latency 증가
 └── Packet 처리 지연
```

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. PCIe Link Downshift

| 원인 | 설명 |
|------|------|
| Lane Signal Error | 신호 품질 저하 |
| 접촉 불량 | 슬롯 / 커넥터 물리적 문제 |
| Thermal 문제 | 과열로 인한 Link 강등 |
| PCB 손상 | 기판 손상 |

```
x16 → x8 → x4  (자동 하향 조정)
```

**증상**
- NVMe 속도 저하
- NIC 처리량 감소
- 결제 응답 지연 증가

---

### 5-2. Lane Saturation

고속 NIC + NVMe 동시 사용 시 **PCIe Total Lane 부족** 발생 가능.

```
Lane 부족
 → 장치 병목
 → Queue 증가
 → Interrupt 증가
 → CPU SoftIRQ 증가
```

---

### 5-3. NUMA Cross Traffic

NIC는 Socket 0에 연결, 애플리케이션은 Socket 1 CPU에서 실행 시 발생.

```
NIC (Socket 0)  →  Application (Socket 1)
 → UPI / Infinity Fabric Traffic 증가
 → Remote Memory Access 증가
 → P99 Latency 증가
```

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Differential Signaling

각 Lane은 **차동 신호(Differential Pair)** 기반.

- **목적**: Noise 감소, Signal Integrity 확보
- 두 선의 전압 차이로 신호 판별 → 외부 EMI에 강인

---

### Full-Duplex

송신과 수신을 **동시에** 수행.

```
TX  ↔  RX  (동시 동작)
```

---

### Link Width

여러 Lane을 묶어 대역폭 확장. x1 / x4 / x8 / x16 구성 가능.

---

### Link Training

부팅 시 장치와 CPU 간 협상 수행.

```
장치 ↔ CPU
 → Lane 수 협상
 → 속도 협상
 → 안정성 검증
 → 문제 발생 시 자동 Downshift
```

---

### DMA (Direct Memory Access)

NIC / NVMe가 **PCIe Lane을 통해 Memory에 직접 접근**.

```
NIC / NVMe  ↔  PCIe Lane  ↔  Memory  (CPU Copy 불필요)
```

---

### NUMA Locality

특정 PCIe Slot은 특정 CPU Socket에 물리적으로 연결됨. **NUMA Alignment** 가 I/O 성능에 직결.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### PCIe Link 상태 확인

```bash
lspci -vv
```

확인 항목:

| 항목 | 의미 |
|------|------|
| LnkCap | 장치가 지원하는 최대 Speed / Width |
| LnkSta | 현재 실제 동작 Speed / Width |
| Speed | Gen3 / Gen4 / Gen5 |
| Width | x4 / x8 / x16 |

> **LnkCap ≠ LnkSta** 이면 Link Downshift 발생 중

### PCIe Device 확인

```bash
lspci
```

### NUMA 확인

```bash
numactl --hardware
```

### NIC 통계 확인

```bash
ethtool -S eth0
```

### NVMe 상태 확인

```bash
nvme list
nvme smart-log /dev/nvme0
```

### Interrupt 확인

```bash
cat /proc/interrupts
```

### SoftIRQ 상태 확인

```bash
cat /proc/softirqs
```

### Kubernetes Node Topology 확인

```bash
kubectl describe node
```

---

## 요약

```
Lane
 ├── 물리 구조         → TX 2선 + RX 2선 = 4선 / Lane
 ├── Differential      → 차동 신호 → Noise 제거
 ├── Full-Duplex       → TX / RX 동시 동작
 ├── Link Width        → x1 / x4 / x8 / x16 → 대역폭 결정
 ├── Link Training     → 부팅 시 속도 / Width 협상
 ├── DMA               → CPU 개입 없는 직접 메모리 접근
 └── NUMA Locality     → Slot → Socket 물리 연결 → Alignment 중요
```

> FinTech 결제 시스템에서 Lane은 단순한 물리 배선이 아니라,  
> **NIC · NVMe · GPU의 최대 처리량과 지연을 결정하는 I/O 대역폭의 물리적 최소 단위**이다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*