# 입출력 제어 프로토콜 (I/O Control Protocol)

> 정독: 0회

## 1. 이 기술이 무엇인가

**입출력 제어 프로토콜(I/O Control Protocol)** 은:

> 운영체제 커널과 하드웨어 장치가 데이터와 명령을 주고받기 위해 사용하는 표준화된 통신 규약

### 핵심 역할

프로토콜은 명령 형식, 데이터 전송 방식, 큐 구조, 완료 통지 방식, 오류 코드, 메모리 매핑 규칙을 정의합니다.

### 대표 프로토콜

| 영역 | 프로토콜 |
|---|---|
| SSD | NVMe, AHCI |
| 네트워크 | Ethernet, TCP/IP |
| USB | USB protocol |
| GPU | PCIe 기반 command protocol |

I/O 프로토콜은 **소프트웨어 명령을 하드웨어가 이해 가능한 제어 규칙으로 변환**합니다.

---

## 2. 시스템 어디에서 등장하는가

입출력 제어 프로토콜은 **Kernel ↔ Device** 경계에서 등장합니다.

### 스토리지

```
Filesystem
  → Block Layer
    → NVMe/AHCI Protocol
      → SSD Controller
```

### 네트워크

```
Socket
  → TCP/IP Stack
    → NIC Driver
      → Ethernet Protocol
        → NIC Hardware
```

### USB

```
USB Driver
  → USB Host Controller Protocol
    → USB Device
```

### GPU

```
Graphics Driver
  → PCIe Command Queue
    → GPU Firmware
```

### Kubernetes / Cloud 환경

Container나 Pod도 결국 디스크 I/O, 네트워크 I/O 모두 **커널과 I/O 프로토콜 위에서 동작**합니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

입출력 제어 프로토콜은 특히 **Disk / Network 자원에 매우 큰 영향**을 줍니다.

- **Disk**: queue depth, latency, throughput, interrupt rate, parallel I/O
- **Network**: packet rate, DMA efficiency, interrupt coalescing, NIC queue scaling
- **CPU**: syscall, interrupt, polling, queue management 부하 발생
- **Memory**: DMA buffer, page cache, ring buffer, queue memory 사용량 증가

> 핵심: **프로토콜 구조 자체가 시스템 I/O 성능 한계를 결정**할 수 있습니다.

---

## 4. 왜 중요한가

I/O는 대부분 시스템 병목의 핵심입니다. CPU는 매우 빠르지만 디스크, 네트워크, 외부 장치는 상대적으로 느리기 때문에, **장치와 데이터를 얼마나 효율적으로 주고받는가**가 중요해집니다.

### 프로토콜 차이 예시

| 프로토콜 | 특징 |
|---|---|
| AHCI | HDD 중심, 단일 큐 |
| NVMe | SSD 병렬성 최적화 |
| SATA | 상대적으로 낮은 대역폭 |
| PCIe/NVMe | 고병렬·저지연 |

현대 시스템에서는 하드웨어 성능보다 **I/O 프로토콜 설계가 병목이 되는 경우**도 많습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

I/O 프로토콜 문제는 성능 저하와 시스템 장애를 직접 유발합니다.

### 대표 장애 유형

**Queue Saturation** — I/O queue가 가득 차 latency 증가

**Interrupt Storm** — 과도한 I/O interrupt 발생

**Timeout** — 장치 응답 지연으로 I/O timeout 발생

**DMA Failure** — 메모리 매핑 오류

**Driver/Protocol Mismatch** — 프로토콜 버전/드라이버 비호환

**NVMe Reset** — SSD controller timeout으로 장치 reset

**Packet Drop** — NIC queue overflow

### Kubernetes 환경

| 증상 | 실제 원인 |
|---|---|
| PVC latency 증가 | NVMe queue saturation |
| Pod timeout | storage I/O stall |
| API latency 증가 | NIC interrupt overload |
| node freeze | block layer congestion |

> **애플리케이션 지연의 근본 원인이 I/O 프로토콜 병목인 경우가 많습니다.**

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심 메커니즘은 **Queue 기반 비동기 명령 처리**입니다.

### 기본 흐름

```
Application
  → System Call
    → Kernel I/O Stack
      → Driver
        → I/O Protocol
          → Device
```

### NVMe 예시

```
1) Submission Queue   → 커널이 READ / WRITE command 기록
2) Doorbell Register  → 장치에 새 명령 존재 알림
3) Device Execution   → SSD controller가 NAND 작업 수행
4) Completion Queue   → 완료 결과 기록
5) Interrupt/MSI-X    → CPU에 완료 통지
```

현대 I/O는 동기식 1회 처리보다 **병렬 큐 기반 비동기 처리 중심**입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# NVMe 장치
nvme list

# 블록 장치 큐
cat /sys/block/nvme0n1/queue/*

# 디스크 성능
iostat -x 1

# I/O latency
iotop
pidstat -d

# 인터럽트
cat /proc/interrupts

# PCIe 장치
lspci

# 네트워크 큐
ethtool -l eth0
ethtool -S eth0
```

### Runtime 관측 포인트

- queue depth
- IO wait
- interrupt rate
- packet drop
- DMA throughput
- device reset
- timeout

### Kubernetes

```bash
# 노드 레벨 확인
kubectl top node
kubectl describe node
```

**Storage 문제**: PVC latency, CSI timeout, disk pressure, storage backend saturation

**Network 문제**: CNI latency, packet loss, kube-proxy delay

### 핵심 관측 포인트

> Pod 문제처럼 보이더라도 **실제 원인은 커널 I/O stack 또는 device protocol 병목**일 수 있습니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*