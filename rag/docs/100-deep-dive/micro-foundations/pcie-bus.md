# PCIe Bus (PCI Express Bus)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**PCIe Bus** 는 CPU와 고속 장치(NIC, NVMe SSD, GPU 등)를 연결하는 **고속 직렬 인터커넥트 기반 데이터 전송 체계**이다.

### 핵심 특징

- **Point-to-Point 구조** — 장치 전용 경로
- **전용 Lane 기반** — 공유 버스 없음
- **고대역폭 / 저지연**
- **Full-Duplex** — 송수신 동시 가능

### 대표 연결 대상

| 장치 | 예시 |
|------|------|
| Network | PCIe NIC |
| Storage | NVMe SSD |
| Accelerator | GPU / FPGA |
| Memory Extension | CXL Device |

---

## 2. 시스템 어디에서 등장하는가

현대 서버 대부분의 **핵심 I/O 경로**에서 등장한다.

### NIC Packet Flow

```
NIC
 ↔ PCIe Bus
 ↔ CPU Root Complex
 ↔ Memory
 ↔ Application
```

### NVMe Storage Flow

```
NVMe SSD
 ↔ PCIe Bus
 ↔ CPU
 ↔ Filesystem
 ↔ Database
```

### Kubernetes Node

```
PCIe NIC  ↔  CNI  ↔  Pod Network
```

### GPU / FPGA Acceleration

```
CPU  ↔  PCIe Bus  ↔  GPU / FPGA
```

---

## 3. 어떤 자원에 가장 영향이 큰가

PCIe Bus는 **전체 시스템 I/O Throughput** 에 가장 직접적인 영향을 준다.

| 자원 | 영향 |
|------|------|
| Network | NIC Throughput |
| Disk | NVMe IOPS |
| Memory | DMA Traffic |
| CPU | Interrupt / NUMA / SoftIRQ |

> **핵심 경로**: I/O Latency + Bandwidth 모두에 직결

---

## 4. 왜 중요한가

### FinTech 결제 시스템의 핵심 요구사항

- **매우 높은 Packet 처리량**
- **빠른 Transaction Logging**
- **낮은 P99 Latency**
- **안정적 Replication**

### PCIe Bus 병목 발생 시 장애 전파

```
PCIe Bus 병목
 ├── Packet Delay          → 결제 요청 지연
 ├── Disk Flush 지연       → Transaction Log 누락 위험
 ├── TCP Retransmission    → 처리 지연 누적
 ├── Queue 증가            → Backpressure 전파
 └── Tail Latency 증가     → P99 악화
```

> **PCIe 병목 = NIC + Storage + CPU 전체 병목**

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. PCIe Link Downshift

Signal Integrity / Thermal / Lane Error 문제로 자동 하향 발생.

```
PCIe Gen5 x16  →  Gen4 x8  (자동 하향 조정)
 ├── NIC Throughput 감소
 └── NVMe Latency 증가
```

| 원인 | 설명 |
|------|------|
| Signal Integrity 문제 | 슬롯 / 케이블 불량 |
| Thermal 문제 | 과열로 인한 Link 강등 |
| Lane Error | 물리적 Lane 손상 |

---

### 5-2. NUMA Cross Traffic

NIC는 Socket 0 연결, Application은 Socket 1에서 실행 시 발생.

```
NIC (Socket 0)  →  Application (Socket 1)
 → UPI / Infinity Fabric Traffic 증가
 → Remote Memory Access 증가
 → P99 Latency 증가
```

---

### 5-3. Interrupt Saturation

고속 NIC Packet 폭주 시 **MSI-X Interrupt 급증** 발생.

```
MSI-X Interrupt 증가
 → SoftIRQ Saturation
 → ksoftirqd CPU 급증
```

---

### 5-4. PCIe AER Error

PCIe Bus 오류 발생 시 Kernel 로그에 기록.

| 오류 유형 | 설명 |
|-----------|------|
| Correctable Error | 자동 복구 가능 |
| Uncorrectable Error | Device Reset 필요 |

심한 경우:
- Device Reset
- NIC Disconnect
- NVMe Timeout

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Root Complex

CPU 측 PCIe 제어기. CPU / Memory와 PCIe Fabric을 연결하는 최상위 노드.

```
CPU / Memory  ↔  Root Complex  ↔  PCIe Fabric  ↔  장치
```

---

### Lane

PCIe 최소 전송 단위. Lane 수에 따라 대역폭 비례 증가.

| 구성 | 주요 용도 |
|------|-----------|
| x1 | 저속 장치 |
| x4 | NVMe SSD |
| x8 | 고속 NIC |
| x16 | GPU / 초고속 NIC |

---

### Transaction Layer Packet (TLP)

PCIe 내부 데이터 전송 단위.

| 구성 요소 | 역할 |
|-----------|------|
| 주소 | 목적지 장치 식별 |
| Payload | 실제 데이터 |
| CRC | 무결성 검증 |

---

### DMA (Direct Memory Access)

PCIe Device가 CPU 개입 없이 Memory에 직접 접근.

```
Device  ↔  PCIe Bus  ↔  Memory  (CPU 개입 없음)
```

---

### MSI / MSI-X

PCIe Device의 작업 완료 Interrupt 전달 방식.

| 방식 | 특징 |
|------|------|
| MSI | 단일 인터럽트 벡터 |
| MSI-X | 다중 인터럽트 벡터 → Queue별 IRQ 분산 |

---

### PCIe Switch

PCIe 장치 분기 / 라우팅 수행. 멀티 장치 연결 시 필수 구성 요소.

```
Root Complex
 → PCIe Switch
   ├── NVMe SSD
   ├── NIC
   └── GPU
```

---

### Link Training

부팅 시 장치와 CPU 간 자동 협상 수행.

```
장치 ↔ CPU
 → Lane 수 협상
 → 속도 협상 (Gen3 / Gen4 / Gen5)
 → 안정성 검증
 → 문제 시 자동 Downshift
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### PCIe Device 목록 확인

```bash
lspci
```

### PCIe 상세 상태 확인

```bash
lspci -vv
```

확인 항목:

| 항목 | 의미 |
|------|------|
| LnkCap | 장치 지원 최대 Speed / Width |
| LnkSta | 현재 실제 동작 Speed / Width |
| Speed | Gen3 / Gen4 / Gen5 |
| Width | x4 / x8 / x16 |
| AER Error | Advanced Error Reporting 오류 기록 |

> **LnkCap ≠ LnkSta** 이면 Link Downshift 발생 중

### PCIe Topology 확인

```bash
lspci -tv
```

### NUMA 구조 확인

```bash
numactl --hardware
```

### Interrupt 확인

```bash
cat /proc/interrupts
```

### SoftIRQ 상태 확인

```bash
cat /proc/softirqs
```

### NVMe 상태 확인

```bash
nvme list
```

### NIC 상태 확인

```bash
ethtool -S eth0
```

### Kubernetes Device Topology 확인

```bash
kubectl describe node
```

---

## 요약

```
PCIe Bus
 ├── Root Complex        → CPU / Memory ↔ PCIe Fabric 연결 제어기
 ├── Lane                → 물리적 대역폭 단위 (x1 ~ x16)
 ├── TLP                 → 주소 + Payload + CRC 기반 내부 패킷
 ├── DMA                 → CPU 개입 없는 고속 메모리 접근
 ├── MSI / MSI-X         → IRQ 분산 → SoftIRQ 부하 조절
 ├── PCIe Switch         → 멀티 장치 분기 / 라우팅
 └── Link Training       → 부팅 시 Lane / 속도 / 안정성 자동 협상
```

> FinTech 결제 시스템에서 PCIe Bus는 단순한 확장 슬롯이 아니라,  
> **NIC · NVMe · GPU · DMA · IRQ가 교차하는 서버 I/O 성능의 물리적 고속도로**이다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*