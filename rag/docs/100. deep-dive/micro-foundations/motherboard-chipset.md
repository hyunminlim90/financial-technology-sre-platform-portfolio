# Motherboard Chipset (메인보드 칩셋)
## 1. 메인보드 칩셋(Motherboard Chipset)이란 무엇인가

> 정독: 0회

메인보드 칩셋(Motherboard Chipset)은:

> CPU와 주변 하드웨어 장치들 사이의 데이터 흐름과 시스템 제어를 담당하는 **메인보드 중앙 통합 제어 칩**

**CPU와 주변장치 사이를 연결하고 통제하는 하드웨어 관제탑**

현대 시스템에서는 USB, SATA, NVMe, PCIe 일부, Audio, LAN, TPM, BIOS/UEFI 연동 등을 관리합니다.

**핵심 구조:**

```
CPU
↕
Chipset (PCH / AMD Chipset)
↕
SSD / USB / NIC / Audio / Peripheral
```

> 칩셋은 **메인보드 전체 I/O 시스템의 중앙 허브**입니다.

---

## 2. 시스템 어디에서 등장하는가

칩셋은 CPU와 주변장치 사이의 **모든 하드웨어 경로**에서 등장합니다.

**현대 구조:**

```
CPU
├── Direct PCIe (GPU/NVMe 일부)
├── Direct Memory Controller (DRAM)
└── Chipset Link
        ├── USB
        ├── SATA
        ├── Additional PCIe
        ├── Audio
        ├── LAN
        └── TPM/UEFI
```

> 초고속 장치는 **CPU 직결**, 일반 I/O 장치는 **칩셋 경유** 구조입니다.

**대표 등장 위치:**

| 장치 | 연결 위치 |
|------|----------|
| GPU | 주로 CPU 직결 |
| DRAM | CPU 내부 Memory Controller |
| SATA SSD | Chipset |
| USB | Chipset |
| LAN | Chipset |
| Audio | Chipset |
| TPM | Chipset 연계 |

> 현대 칩셋은 **"주변장치 통합 관리자"** 역할입니다.

---

## 3. 어떤 자원(CPU / Memory / Network / Disk)에 가장 영향이 큰가

칩셋은 특히 **Disk·Network·I/O 계층**에 매우 큰 영향을 줍니다.

| 자원 | 영향도 |
|------|--------|
| Disk | 매우 큼 |
| Network | 매우 큼 |
| CPU | 중간 |
| Memory | 중간 |

### Disk 영향

SATA Controller, NVMe Lane 일부, RAID 기능, Storage Routing 등을 관리합니다. 즉 **SSD/HDD 성능과 직접 연결**됩니다.

### Network 영향

Onboard NIC, PCIe Lane Allocation, DMA Routing, IOMMU 등과 밀접하게 연결됩니다.

### CPU 영향

칩셋은 CPU와 DMI/Infinity Fabric 같은 인터커넥트로 연결됩니다. 대량 I/O → Chipset Link 포화 → CPU I/O Wait 증가가 발생할 수 있습니다.

### Memory 영향

현대 DRAM Controller는 대부분 CPU 내부에 있지만, DMA·IOMMU·Device Access 측면에서 메모리와도 연결됩니다.

---

## 4. 왜 중요한가

칩셋이 시스템의 **확장성·I/O 성능·보안 범위**를 결정하기 때문입니다.

> CPU 성능만으로 전체 시스템 성능이 결정되지 않는다.  
> **칩셋 구조가 I/O 병목과 확장성을 결정한다.**

| 요소 | 칩셋 영향 |
|------|----------|
| USB 개수 | 칩셋 |
| SATA 포트 수 | 칩셋 |
| 추가 PCIe Lane | 칩셋 |
| NVMe 확장성 | 칩셋 |
| IOMMU 지원 | 칩셋 |
| TPM/Secure Boot | 칩셋 연계 |

> 메인보드 플랫폼 차이는 대부분 **칩셋 차이**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 서버·워크스테이션 장애와 매우 밀접합니다.

**대표 사례:**

| 장애/문제 | 원인 |
|-----------|------|
| PCIe 장치 인식 실패 | Chipset Lane 문제 |
| NVMe 성능 저하 | DMI 병목 |
| USB 끊김 | Chipset I/O Saturation |
| NIC Packet Drop | DMA/IOMMU 문제 |
| Boot Failure | Firmware/Chipset 문제 |
| Storage Timeout | SATA Controller 문제 |
| VM Passthrough 실패 | IOMMU 설정 문제 |

> 대규모 I/O 시스템에서는 **칩셋 병목이 전체 서버 성능 병목**이 될 수 있다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 6-1. 현대 칩셋은 "I/O 집중 관리 허브"다

**과거:** Northbridge + Southbridge 구조

**현재:**
```
CPU 내부: Memory Controller, GPU PCIe
Chipset:  나머지 I/O 전체
```

고속 기능은 CPU로 흡수되었습니다.

### 6-2. CPU와 Chipset 사이에도 병목이 존재한다

```
CPU
↕ DMI / IF Link
Chipset
↕
Storage / USB / NIC
```

모든 Chipset I/O는 **하나의 링크를 공유**합니다. NVMe 다중 사용, USB 대량 사용, 고속 NIC 등이 동시에 몰리면 병목이 발생할 수 있습니다.

### 6-3. 칩셋은 DMA와 IOMMU 보안 핵심이다

```
NIC/GPU DMA 요청
→ IOMMU 검사
→ 허용된 Memory만 접근
```

장치의 **불법 메모리 접근을 차단**합니다.

### 6-4. 칩셋은 부팅 신뢰 체인(Root of Trust) 일부다

현대 시스템에서는 Secure Boot, TPM, Intel ME, AMD PSP 등과 연결됩니다. 즉 **시스템 부팅 무결성 일부를 담당**합니다.

### 6-5. PCIe Lane 배분도 칩셋 구조 영향이다

| 연결 | 특징 |
|------|------|
| CPU Direct PCIe | 가장 빠름 |
| Chipset PCIe | 공유 대역폭 |

> 같은 NVMe라도 **슬롯 위치에 따라 성능 차이**가 날 수 있습니다.

---

## 7. Linux / Runtime / Kubernetes에서 어디서 관측되는가

### PCIe / Hardware 구조 확인

```bash
lspci
lspci -tv
```

관측 가능: PCIe Topology, Chipset 연결 구조, NIC/GPU/NVMe 위치

### IOMMU 상태 확인

```bash
dmesg | grep -i iommu
```

확인 가능: DMA Protection, IOMMU 활성화 상태

### Storage 상태 확인

```bash
lsblk
nvme list
iostat
```

관측 가능: Storage Latency, IO Saturation, NVMe 상태

### Interrupt 확인

```bash
cat /proc/interrupts
```

확인 가능: NIC IRQ, Storage IRQ, Device Interrupt 분포

### Kubernetes / Server 환경

K8s Node에서도 칩셋 영향이 존재합니다.

| 영역 | 영향 |
|------|------|
| NVMe 성능 | Pod IO 성능 |
| NIC DMA | 네트워크 처리량 |
| PCIe Bandwidth | GPU/AI Workload |
| NUMA/IOMMU | VM/Container 성능 |

### Observability

현대 시스템에서는 perf, eBPF, iostat, PCIe telemetry 등으로 칩셋/I/O 상태를 추적합니다.

**대표 메트릭:**

| 메트릭 | 의미 |
|--------|------|
| PCIe Throughput | 버스 사용량 |
| IO Wait | 스토리지 병목 |
| IRQ Rate | 장치 인터럽트 |
| DMA Activity | 장치 메모리 접근 |
| NVMe Latency | 저장장치 지연 |
| Packet Drop | NIC 병목 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*