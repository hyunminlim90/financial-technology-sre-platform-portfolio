# PCIe NIC (PCI Express Network Interface Card)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**PCIe NIC** 는 PCIe Bus를 통해 CPU와 직접 연결되는 **고성능 네트워크 인터페이스 장치**이다.

일반 온보드 NIC와 달리 다음 특성을 가진다.

- **고대역폭**
- **저지연**
- **하드웨어 오프로드**
- **멀티큐 (Multi-Queue) 구조**

### 대표 속도

| 종류 | 속도 |
|------|------|
| 1GbE | 1 Gbps |
| 10GbE | 10 Gbps |
| 25GbE | 25 Gbps |
| 100GbE | 100 Gbps |

<details>
<summary>Deep Dive</summary></br>

PCIe Bus(PCI Express Bus) [[M]](../../100-deep-dive/micro-foundations/pcie-bus.md)

</details></br>

## 2. 시스템 어디에서 등장하는가

결제 시스템의 **모든 외부 통신 진입점**이다.

### API Gateway

```
Client
 → PCIe NIC
 → Kernel Network Stack
 → Nginx / Envoy / Gateway
```

### Kubernetes Node

```
NIC  →  CNI  →  Pod Network
```

### DB Cluster

```
DB Replication  →  PCIe NIC  →  TCP / RDMA
```

### Service Mesh

```
Istio / Envoy  →  Socket  →  NIC Queue
```

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접 영향: **Network Throughput**

하지만 실제로는 다음 자원과도 강하게 연결된다.

| 자원 | 영향 |
|------|------|
| CPU | Interrupt / SoftIRQ |
| Memory | DMA / Packet Buffer |
| Network | PPS / Bandwidth |
| NUMA | Remote Access Latency |

---

## 4. 왜 중요한가

### FinTech 결제 시스템의 핵심 요구사항

- **Burst Traffic 처리**
- **낮은 P99 Latency**
- **높은 TPS**
- **지속적 TLS 처리**

### NIC 병목 발생 시 장애 전파

```
NIC 병목
 ├── Connect Timeout       → 결제 요청 실패
 ├── Retry Storm           → 중복 처리 위험
 ├── Tail Latency 증가     → P99 악화
 ├── Packet Drop           → 데이터 유실
 └── TCP Retransmission    → 처리 지연 누적
```

> **고성능 NIC = CPU 절약 + 네트워크 안정성 확보**

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. Interrupt Storm

고속 Packet 유입 시 **MSI-X Interrupt 폭증** 발생 가능.

```
MSI-X Interrupt 폭증
 → 특정 CPU Core 100%
 → ksoftirqd Saturation
 → Application Latency 증가
```

---

### 5-2. RX Queue Overflow

NIC Ring Buffer 부족 시 **rx_dropped 증가** 발생.

```
Ring Buffer 포화
 → rx_dropped 증가
 → Packet Loss
 → TCP Retransmission 증가
```

---

### 5-3. NUMA Mismatch

NIC는 Socket 0, Application Thread는 Socket 1에서 실행 시 발생.

```
NIC (Socket 0)  →  Application (Socket 1)
 → Remote Memory Access 증가
 → P99 Latency 증가
 → CPU Cache Miss 증가
```

---

### 5-4. TCP Offload 문제

TSO / GRO / LRO 설정 충돌 시 발생.

**증상**
- Packet Reordering
- Latency Spike
- Throughput 감소

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### PCIe Direct Path

PCIe NIC는 PCH를 우회하여 CPU PCIe Lane에 직결.

```
CPU PCIe Lane  ↔  NIC  (PCH 우회 → Latency 감소)
```

---

### DMA (Direct Memory Access)

NIC가 CPU 개입 없이 Memory에 직접 Packet 복사.

```
NIC  ↔  Memory  (CPU 개입 없음 → CPU 사용량 절감)
```

---

### RSS (Receive Side Scaling)

수신 Packet을 여러 CPU Core에 분산 처리.

- **목적**: Interrupt 집중 방지, Core 간 부하 균등화

---

### MSI / MSI-X

NIC가 CPU에 작업 완료를 알리는 Interrupt 방식.

| 방식 | 특징 |
|------|------|
| MSI | 단일 인터럽트 벡터 |
| MSI-X | 다중 인터럽트 벡터 → Queue별 IRQ 분산 |

고속 NIC일수록 MSI-X 분산 설정이 중요.

---

### Ring Buffer

NIC 내부 Packet Queue. 가득 차면 **Packet Drop** 발생.

```
Ring Buffer 포화  →  rx_dropped 증가  →  Packet Loss
```

---

### Offload Engine

NIC가 직접 처리하여 CPU 부하를 절감.

| 기능 | 설명 |
|------|------|
| Checksum Offload | 패킷 무결성 검사 |
| TSO | TCP Segmentation Offload |
| GRO | Generic Receive Offload |
| LRO | Large Receive Offload |

---

### SR-IOV (Single Root I/O Virtualization)

물리 NIC를 여러 Virtual NIC로 분할. **Kubernetes / VM 성능 최적화** 핵심.

```
Physical NIC  →  VF (Virtual Function) × N  →  Pod / VM
```

---

### RDMA (Remote Direct Memory Access)

Kernel Bypass 기반 원격 메모리 직접 접근. **초저지연 시스템** 핵심 기술.

```
Host A Memory  ↔  RDMA NIC  ↔  Host B Memory  (Kernel 개입 없음)
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### NIC 정보 확인

```bash
ip link
ethtool eth0
```

### Driver / Firmware 확인

```bash
ethtool -i eth0
```

### NIC 통계 확인

```bash
ethtool -S eth0
```

확인 항목:

| 항목 | 의미 |
|------|------|
| rx_dropped | Ring Buffer 포화로 인한 Packet 손실 |
| tx_errors | 송신 오류 |
| queue stats | Queue별 처리량 및 오류 |

### Interrupt 확인

```bash
cat /proc/interrupts
```

### SoftIRQ 상태 확인

```bash
cat /proc/softirqs
```

### RSS Queue 확인

```bash
ethtool -l eth0
ethtool -x eth0
```

### TCP 상태 확인

```bash
ss -it
```

### PCIe 정보 확인

```bash
lspci -vv
```

### Kubernetes SR-IOV 확인

```bash
kubectl get sriovnetwork
```

---

## 요약

```
PCIe NIC
 ├── PCIe Direct Path   → PCH 우회 → CPU 직결 → Latency 감소
 ├── DMA                → CPU 개입 없는 Packet 복사
 ├── RSS                → 수신 Packet 다중 Core 분산
 ├── MSI / MSI-X        → IRQ 분산 → SoftIRQ 부하 조절
 ├── Ring Buffer        → 포화 시 rx_dropped → Packet Loss
 ├── Offload Engine     → Checksum / TSO / GRO → CPU 절감
 ├── SR-IOV             → 물리 NIC 가상화 → K8s / VM 최적화
 └── RDMA               → Kernel Bypass → 초저지연 통신
```

> FinTech 결제 시스템에서 PCIe NIC는 단순한 네트워크 카드가 아니라,  
> **Burst Traffic · P99 Latency · TPS · TLS 처리를 결정하는 외부 통신의 물리적 진입점**이다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*