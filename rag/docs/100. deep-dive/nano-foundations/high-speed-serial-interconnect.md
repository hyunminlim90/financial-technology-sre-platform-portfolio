# High-Speed Serial Interconnect (고속 직렬 인터커넥트)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**High-Speed Serial Interconnect** 는 데이터를 소수의 고속 Lane으로 **직렬(Serial) 전송**하는 초고속 물리 인터커넥트 기술이다.

### 대표 기술

| 기술 | 주요 용도 |
|------|-----------|
| PCIe | NIC / NVMe / GPU 연결 |
| UPI / QPI | Intel 멀티 소켓 CPU 간 연결 |
| Infinity Fabric | AMD 멀티 소켓 CPU 간 연결 |
| SATA / SAS | 스토리지 연결 |
| USB / Thunderbolt | 외부 장치 연결 |

### 핵심 목적

> **고대역폭 + 저지연 + 신호 안정성 확보**

<details>
<summary>Deep Dive</summary></br>

Lane(레인) [[M]](../../100-deep-dive/micro-foundations/lane.md)

</details></br>

## 2. 시스템 어디에서 등장하는가

현대 서버 대부분의 핵심 통신이 이 구조 기반이다.

### CPU ↔ NVMe SSD

```
CPU  ↔  PCIe  ↔  NVMe SSD
```

### CPU ↔ NIC

```
CPU  ↔  PCIe  ↔  10G / 25G / 100G NIC
```

### CPU ↔ CPU (멀티 소켓)

```
CPU Socket 0  ↔  UPI / Infinity Fabric  ↔  CPU Socket 1
```

### GPU / FPGA 연결

```
CPU  ↔  PCIe  ↔  GPU / FPGA
```

---

## 3. 어떤 자원에 가장 영향이 큰가

특히 **I/O Throughput과 Latency** 에 가장 직접적인 영향.

| 자원 | 영향 |
|------|------|
| Network | NIC Throughput |
| Disk | NVMe IOPS |
| Memory | DMA / Data Movement |
| CPU | Interrupt / Copy / NUMA |

---

## 4. 왜 중요한가

### FinTech 결제 시스템의 핵심 요구사항

- **낮은 Latency**
- **높은 TPS**
- **안정적인 Packet 처리**
- **빠른 Storage Flush**

### 고속 직렬 인터커넥트 병목 발생 시 장애 전파

```
인터커넥트 병목
 ├── Packet Loss           → 결제 요청 유실
 ├── NVMe Queue Saturation → DB Commit 지연
 ├── Tail Latency 증가     → P99 악화
 ├── Retry 증가            → 중복 처리 위험
 └── Timeout 증가          → 결제 승인 실패
```

> **PCIe 대역폭 병목 = 전체 시스템 병목**

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. PCIe Bandwidth Saturation

100G NIC + NVMe SSD 동시 고부하 상황에서 발생.

**증상**
- NIC Throughput 감소
- NVMe Latency 증가
- IRQ 증가
- CPU SoftIRQ 급증

---

### 5-2. NUMA Remote Access

NIC는 Socket 0에 연결, 애플리케이션은 Socket 1 CPU에서 실행 시 발생.

```
NIC (Socket 0 연결)
 → Application Thread (Socket 1 실행)
 → UPI / QPI Cross Traffic 증가
 → Memory Latency 증가
 → Packet Processing Latency 증가
```

---

### 5-3. PCIe Link Degrade

PCIe x16 → x8 다운그레이드 발생 시.

| 원인 | 설명 |
|------|------|
| Signal Integrity 문제 | 케이블 / 슬롯 불량 |
| Lane Error | 물리적 Lane 손상 |
| Thermal Issue | 과열로 인한 Link 강등 |

**증상**
- Disk Throughput 감소
- GPU / NIC 성능 저하

---

### 5-4. Interrupt Storm

고속 NIC 부하 시 **MSI-X Interrupt 폭증** 발생 가능.

```
MSI-X Interrupt 폭증
 → CPU SoftIRQ Saturation
 → ksoftirqd CPU 100%
```

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Differential Signaling

두 개의 선(Line) 간의 차이로 신호 판별.

- **목적**: Noise 감소, Signal Integrity 확보
- 외부 전자기 간섭(EMI)에 강인

---

### Lane

PCIe 최소 전송 단위.

| 구성 | 의미 | 주요 용도 |
|------|------|-----------|
| x1 | 1 Lane | 저속 장치 |
| x4 | 4 Lane | NVMe SSD |
| x8 | 8 Lane | 고속 NIC |
| x16 | 16 Lane | GPU |

Lane 증가 시 대역폭 비례 증가.

---

### SerDes (Serializer / Deserializer)

```
병렬 데이터
 → 직렬 변환 (Serialize)
 → 고속 전송
 → 병렬 복원 (Deserialize)
```

---

### Embedded Clock

데이터 내부에 Clock 정보 포함. **별도 Clock Line 불필요** → 배선 단순화.

---

### DMA (Direct Memory Access)

PCIe 장치가 CPU 개입 없이 Memory에 직접 접근.

```
NIC / NVMe  ↔  Memory  (CPU 오버헤드 없이 직접 접근)
```

CPU 오버헤드 감소의 핵심 메커니즘.

---

### MSI / MSI-X Interrupt

PCIe 장치가 CPU에 작업 완료를 알리는 방식.

| 방식 | 특징 |
|------|------|
| MSI | 단일 인터럽트 벡터 |
| MSI-X | 다중 인터럽트 벡터 → Queue별 IRQ 분산 |

고속 NIC 환경에서 MSI-X 분산 설정이 매우 중요.

---

### NUMA Locality

PCIe Slot은 특정 CPU Socket에 물리적으로 연결됨. **NUMA Alignment** 가 I/O 성능에 직결.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### PCIe Device 확인

```bash
lspci
```

### PCIe Link 상태 확인

```bash
lspci -vv
```

확인 항목:
- **Link Width** (x8, x16 등)
- **Link Speed** (Gen3, Gen4, Gen5)
- **AER Error** (Advanced Error Reporting)

### NUMA 확인

```bash
numactl --hardware
```

### IRQ 분포 확인

```bash
cat /proc/interrupts
```

### NIC 통계 확인

```bash
ethtool -S eth0
```

### NVMe 상태 확인

```bash
nvme list
nvme smart-log
```

### SoftIRQ 상태 확인

```bash
cat /proc/softirqs
```

### Kubernetes Node 확인

```bash
kubectl describe node
```

확인 포인트:
- **NUMA Topology**
- **Device Topology**
- **CPU Manager / Topology Manager 설정**

---

## 요약

```
High-Speed Serial Interconnect
 ├── Differential Signaling  → 노이즈 제거 / 신호 안정성
 ├── Lane / SerDes           → 물리적 대역폭 단위
 ├── Embedded Clock          → 별도 Clock 선 없이 동기화
 ├── DMA                     → CPU 개입 없는 고속 메모리 전송
 ├── MSI / MSI-X             → IRQ 분산 → SoftIRQ 부하 조절
 └── NUMA Locality           → Cross-Socket 접근 최소화
```

> FinTech 결제 시스템에서 고속 직렬 인터커넥트는 단순한 데이터 통로가 아니라,  
> **NIC · NVMe · GPU · CPU 간 모든 I/O 흐름의 물리적 대역폭과 지연을 결정하는 핵심 인프라 계층**이다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*