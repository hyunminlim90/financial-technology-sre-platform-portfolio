# PCIe (Peripheral Component Interconnect Express)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**PCIe(Peripheral Component Interconnect Express)** 는 다음을 연결하는 **고속 직렬 인터커넥트(High-Speed Serial Interconnect)** 규격이다.

```
CPU / Memory  ↔  고속 주변 장치 (NIC / NVMe / GPU)
```

현재 서버 시스템의 대부분의 고성능 장치는 **PCIe 기반**으로 연결된다.

### 대표 연결 장치

| 장치 | 용도 |
|------|------|
| NVMe SSD | 고속 스토리지 |
| NIC | 네트워크 인터페이스 |
| GPU | 병렬 연산 |
| FPGA | 특수 연산 가속 |
| RAID Controller | 스토리지 제어 |

<details>
<summary></summary></br>

High-Speed Serial Interconnect(고속 직렬 인터커넥트) [[M]](../../100-deep-dive/micro-foundations/high-speed-serial-interconnect.md)

</details></br>

## 2. 시스템 어디에서 등장하는가

PCIe는 **Hardware ↔ OS ↔ Runtime 전체 흐름**에 등장한다.

### E2E 흐름 — 네트워크 수신

```
NIC Packet
 → PCIe Bus
 → CPU Memory
 → Kernel Network Stack
 → Socket Buffer (sk_buff)
 → JVM Socket Read
 → Java Application
```

### E2E 흐름 — 디스크 기록

```
Java Write
 → Kernel I/O
 → NVMe Driver
 → PCIe Bus
 → NVMe SSD
```

> **핵심 정의**: CPU와 실제 I/O 장치 사이의 **핵심 데이터 통로**

---

## 3. 어떤 자원에 가장 영향이 큰가

PCIe는 **Network + Disk + Memory DMA** 에 가장 큰 영향을 준다.

| 자원 | 영향 |
|------|------|
| Network | NIC Throughput |
| Disk | NVMe IOPS |
| Memory | DMA / NUMA |
| CPU | Interrupt / SoftIRQ |

---

## 4. 왜 중요한가

### FinTech 결제 시스템의 핵심 지표

- **낮은 Latency**
- **높은 Throughput**
- **안정적 Tail Latency**

### PCIe 병목 발생 시 장애 전파

```
PCIe 병목
 ├── Packet Processing 지연   → 결제 승인 지연
 ├── NVMe Flush 지연          → DB Commit 지연
 ├── IRQ Saturation           → Kafka Throughput 감소
 └── DMA Latency 증가         → P99 증가
```

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. PCIe Bandwidth Saturation

100Gb NIC + 고속 NVMe 동시 사용 시 **PCIe Lane 부족** 발생 가능.

**증상**
- Network Throughput 감소
- Disk I/O Latency 증가
- Packet Drop 증가

---

### 5-2. Interrupt Storm

NIC가 과도한 IRQ를 발생시켜 **SoftIRQ CPU 100%** 상태 도달 가능.

**증상**
- Java Thread CPU starvation
- Event Loop Delay
- Tail Latency 증가

---

### 5-3. NUMA Remote Access

PCIe 장치가 Socket0에 연결됐으나 Application Thread가 Socket1 CPU에서 실행될 때 발생.

```
PCIe Device (Socket0 연결)
 → Application Thread (Socket1 실행)
 → UPI/QPI Cross Traffic 증가
 → DMA Access Latency 증가
```

---

### 5-4. PCIe AER Error

하드웨어 레벨 오류로 다음 두 종류가 발생 가능.

| 오류 유형 | 설명 |
|-----------|------|
| Correctable Error | 자동 복구 가능 |
| Uncorrectable Error | 드라이버/장치 리셋 필요 |

**증상**
- NVMe Timeout
- NIC Reset
- Kernel Driver Reset

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Lane

PCIe 데이터 통로 단위. Lane 수가 많을수록 대역폭 증가.

| 구성 | 용도 예시 |
|------|-----------|
| x1 | 저속 장치 |
| x4 | NVMe SSD |
| x8 | 고속 NIC |
| x16 | GPU |

---

### Generation

세대별 단방향 속도 (per lane).

| 세대 | 속도 |
|------|------|
| PCIe 3.0 | ~1 GB/s per lane |
| PCIe 4.0 | ~2 GB/s per lane |
| PCIe 5.0 | ~4 GB/s per lane |

---

### DMA (Direct Memory Access)

PCIe 장치가 **CPU 개입 없이** Memory에 직접 접근.

```
NIC → RAM 직접 기록  (CPU 오버헤드 감소)
```

---

### MSI / MSI-X

PCIe 장치의 Interrupt 전달 방식. **IRQ 분산**과 직접 연결.

- **MSI**: 단일 인터럽트 벡터
- **MSI-X**: 다중 인터럽트 벡터 → Queue별 IRQ 분산 가능

---

### NUMA Locality

PCIe 슬롯은 **특정 CPU Socket에 물리적으로 연결**됨. 장치와 처리 Thread 간 Locality가 성능에 매우 중요.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### PCIe Device 확인

```bash
lspci
lspci -vv
lspci -tv
```

### NUMA 확인

```bash
numactl --hardware
```

### IRQ 상태 확인

```bash
cat /proc/interrupts
```

### NIC Queue 확인

```bash
ethtool -l eth0
ethtool -S eth0
```

### NVMe 상태 확인

```bash
nvme list
iostat -x
```

### Kernel Log (AER / PCIe 오류)

```bash
dmesg | grep -i pcie
```

### Kubernetes Node 확인

```bash
kubectl describe node
```

확인 항목:
- **SR-IOV**
- **Device Plugin**
- **Topology Manager**
- **CPU Manager**

---

## 요약

```
PCIe
 ├── Lane / Generation    → 물리적 대역폭 상한선 결정
 ├── DMA                  → CPU 개입 없는 고속 메모리 전송
 ├── MSI/MSI-X            → IRQ 분산 → SoftIRQ 부하 조절
 ├── NUMA Locality        → Cross-Socket 접근 레이턴시 최소화
 └── AER Error            → 하드웨어 오류 감지 및 드라이버 리셋
```

> FinTech 결제 시스템에서 PCIe는 단순한 버스 규격이 아니라,  
> **NIC · NVMe · DMA · IRQ · NUMA가 교차하는 I/O 성능의 물리적 한계선**이다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*