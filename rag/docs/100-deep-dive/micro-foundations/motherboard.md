# 마더보드(Motherboard)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**마더보드(Motherboard)** 는 다음 핵심 하드웨어를 연결하는 **메인 시스템 기판(Main System PCB)** 이다.

- CPU
- Memory
- NIC
- Storage
- PCIe Device
- Power Delivery

### 정식 용어

| 용어 | 설명 |
|------|------|
| Motherboard | 가장 일반적인 명칭 |
| Mainboard | 동의어 |
| System Board | 서버/엔터프라이즈 환경 |
| Planar Board | IBM 계열 환경 |

### 실제 서버 환경 구성 요소

실제 서버 환경에서는 다음 요소들이 모두 마더보드 위에 구성된다.

- **CPU Socket**
- **DIMM Slot**
- **PCIe Bus**
- **Chipset (PCH)**
- **VRM**

> **핵심 정의**: 모든 하드웨어 자원 간 데이터 흐름의 **물리적 연결 계층**

<details>
<summary>Deep Dive</summary></br>

PCIe(Peripheral Component Interconnect Express) [[M]](../../100-deep-dive/micro-foundations/pcie.md)
Power Delivery(전력 전달 계층) [[M]](../../100-deep-dive/micro-foundations/power-delivery.md)

</details></br>

## 2. 시스템 어디에서 등장하는가

마더보드는 **Hardware Layer 전체의 기반**이다.

### E2E 데이터 흐름

```
NIC
 → PCIe Bus
 → CPU Socket
 → Memory Controller
 → RAM
 → JVM Heap
 → Java Application
```

이 흐름 전체가 마더보드를 통과한다.

### 연관 영역

| 영역 | 관련성 |
|------|--------|
| CPU | Socket / VRM / Cache Coherency |
| Memory | DIMM / NUMA / Memory Channel |
| Network | PCIe NIC / IRQ |
| Storage | NVMe / SATA / PCIe Lane |
| Runtime | NUMA / CPU Locality |
| Kubernetes | CPU Pinning / NUMA Alignment |

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적으로 연결되는 자원: **CPU + Memory + PCIe I/O**

특히 다음 항목에 직접 영향을 미친다.

- **NUMA 구조**
- **Memory Bandwidth**
- **PCIe Throughput**
- **IRQ Locality**
- **Cache Coherency**

---

## 4. 왜 중요한가

### FinTech 결제 시스템의 핵심 지표

- **Tail Latency**
- **Throughput**
- **Consistency**

### 마더보드가 결정하는 물리적 경로

```
CPU ↔ Memory
CPU ↔ NIC
CPU ↔ NVMe
```

### 병목 발생 시 장애 영향

| 원인 | 결과 |
|------|------|
| NUMA Remote Access | P95/P99 Latency 증가 |
| PCIe Lane Saturation | GC 지연 |
| IRQ Imbalance | Packet Processing 지연 |
| Memory Channel 병목 | DB 응답 지연 |

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. NUMA Remote Access

멀티 소켓 서버에서 다음 경로로 접근 시 레이턴시 증가 발생.

```
CPU (Socket0) → Memory (Socket1)
```

**증상**
- Java GC 증가
- Kafka Throughput 감소
- Netty EventLoop Delay 증가

---

### 5-2. PCIe Lane Saturation

고속 NVMe + NIC 동시 사용 시 **PCIe Bus Saturation** 발생 가능.

**증상**
- Disk Latency 증가
- Packet Drop 증가
- Network Jitter 증가

---

### 5-3. IRQ Imbalance

NIC Interrupt가 특정 CPU Socket에 집중될 때 발생.

**증상**
- SoftIRQ 폭증
- 특정 CPU만 100%
- Tail Latency 증가

---

### 5-4. VRM Thermal Throttling

전력 공급 부족 또는 발열 발생 시 **CPU Frequency Downclock** 발생.

**증상**
- TPS 감소
- GC Time 증가
- 응답시간 증가

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### CPU Socket

마더보드 위 CPU 장착 인터페이스.

| 소켓 | 플랫폼 |
|------|--------|
| LGA1700 | Intel |
| SP5 | AMD EPYC |
| AM5 | AMD Desktop |

---

### NUMA (Non-Uniform Memory Access)

CPU Socket별 **Local Memory** 존재. Java / JVM / Kubernetes 성능과 매우 밀접.

---

### PCIe Bus

NIC / NVMe / GPU 연결 인터커넥트.

---

### Chipset (PCH)

저속 I/O 제어 담당.

- SATA
- USB
- 일부 PCIe Lane

---

### VRM (Voltage Regulator Module)

CPU 전압 안정화 회로. 고부하 서버 환경에서 특히 중요.

---

### IRQ Routing

NIC Interrupt가 어떤 CPU Core로 갈지 결정. **SoftIRQ 성능**과 직접 연결.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### CPU / NUMA 확인

```bash
lscpu
numactl --hardware
numastat
```

### PCIe Device 확인

```bash
lspci
lspci -tv
```

### IRQ 상태 확인

```bash
cat /proc/interrupts
```

### CPU Frequency 확인

```bash
cpupower frequency-info
```

### Thermal 상태 확인

```bash
sensors
```

### Kubernetes Node 확인

```bash
kubectl describe node
```

확인 항목:
- **CPU Manager**
- **Topology Manager**
- **NUMA Alignment**

---

## 요약

```
마더보드
 ├── CPU Socket        → NUMA / Cache Coherency
 ├── DIMM Slot         → Memory Bandwidth / NUMA
 ├── PCIe Bus          → NIC / NVMe / GPU 처리량
 ├── Chipset (PCH)     → 저속 I/O 제어
 ├── VRM               → CPU 전압 / Thermal 안정성
 └── IRQ Routing       → SoftIRQ / Tail Latency
```

> FinTech 결제 시스템에서 마더보드는 단순한 기판이 아니라,  
> **Latency, Throughput, Consistency를 결정하는 물리적 아키텍처 계층**이다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*