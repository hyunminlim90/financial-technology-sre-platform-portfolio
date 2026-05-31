# 디바이스 드라이버 (Device Driver)

> 정독: 0회

## 1. 이 기술이 무엇인가

**디바이스 드라이버(Device Driver)** 는:

> 운영체제 커널 내부에서 특정 하드웨어 장치를 직접 제어하는 커널 소프트웨어 모듈

드라이버는 SSD, NIC(Network Interface Card), GPU, USB Controller, NVMe Controller, RAID Controller 같은 실제 하드웨어를 운영체제가 사용할 수 있게 만들며, **상위 커널 명령 ↔ 하드웨어 전기/레지스터 동작** 사이를 연결합니다.

### 핵심 역할

드라이버는 하드웨어 초기화, MMIO register 제어, DMA 설정, interrupt 처리, queue 관리, protocol command 생성을 수행합니다.

---

## 2. 시스템 어디에서 등장하는가

드라이버는 운영체제 커널 내부에 존재합니다.

### 전체 위치

```
User Space
  → System Call
    → VFS / Network Stack
      → Kernel Subsystem
        → Device Driver
          → Device Controller
            → Hardware
```

### 스토리지 예시

```
read()
  → VFS
    → Block Layer
      → NVMe Driver
        → NVMe Controller
          → NAND Flash
```

### 네트워크 예시

```
socket send()
  → TCP/IP Stack
    → NIC Driver
      → NIC Hardware
        → Ethernet Wire
```

### Kubernetes 환경

Kubernetes 자체도 결국 드라이버에 의존합니다.

예: NVMe SSD driver, Ethernet NIC driver, SR-IOV driver, GPU driver, CSI storage driver, CNI network driver

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

드라이버는 거의 모든 자원과 연결됩니다.

- **CPU**: interrupt 처리, queue polling, DMA completion, softirq, context switch 발생으로 영향 매우 큼
- **Memory**: DMA buffer, ring buffer, descriptor queue, page pinning 사용
- **Disk**: NVMe queue, SATA command, flush, fsync 등 스토리지 드라이버가 직접 제어
- **Network**: packet RX/TX, RSS, checksum offload, interrupt coalescing을 NIC driver가 제어

> 핵심: 드라이버는 **커널과 물리 장치 사이의 실제 성능 병목 지점 중 하나**입니다.

---

## 4. 왜 중요한가

드라이버는 **운영체제가 하드웨어를 실제로 사용 가능하게 만드는 계층**입니다. 드라이버가 없으면 SSD, NIC, GPU 모두 unusable 상태가 됩니다.

**하드웨어 추상화**: OS는 표준 인터페이스만 사용하고, 장치별 세부 차이는 드라이버가 흡수합니다.

**성능 최적화**: 드라이버 품질에 따라 latency, throughput, interrupt rate, queue efficiency가 달라집니다.

**안정성**: 커널 패닉 원인 상당수가 driver bug입니다.

> 드라이버는 **운영체제 안정성과 성능의 핵심 구성 요소**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실무에서 드라이버 문제는 매우 치명적입니다.

### 대표 장애 유형

**Kernel Panic** — 잘못된 메모리 접근

**Device Timeout** — 드라이버 queue hang

**Interrupt Storm** — 과도한 interrupt 발생

**DMA Corruption** — 잘못된 DMA mapping

**Packet Drop** — NIC driver overload

**Disk I/O Stall** — NVMe/SATA driver issue

**Filesystem Corruption** — flush ordering 문제

**Kubernetes Node NotReady** — NIC/storage driver failure

### 실제 SRE 관점

Application latency 증가처럼 보이는 증상의 실제 원인이 **NIC driver interrupt saturation**인 경우가 매우 많습니다.

고성능 환경 주요 이슈: IRQ imbalance, RSS misconfiguration, queue saturation, NUMA mismatch, driver firmware incompatibility

> **상위 서비스 장애처럼 보여도 실제 원인은 드라이버 레벨인 경우가 많습니다.**

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심 메커니즘은 **커널 명령을 하드웨어 제어 신호로 변환**하는 것입니다.

### 주요 흐름

```
1) 커널 요청 발생       → read(LBA 100)
2) Driver Queue 등록   → command descriptor 생성
3) MMIO Register 기록  → device register 조작
4) Hardware 실행       → controller가 DMA 수행
5) Interrupt 발생      → device completion signal 전달
6) Driver ISR 처리     → interrupt service routine 실행
7) Kernel Completion   → I/O 완료 처리
```

### 핵심 기술

| 기술 | 설명 |
|---|---|
| MMIO | Memory-Mapped I/O |
| DMA | CPU 우회 memory transfer |
| Interrupt | device → CPU notification |
| Queue Pair | submission/completion queue |
| ISR | interrupt handler |

> 드라이버는 **실제 하드웨어 프로토콜 집행 계층**입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 로드된 드라이버
lsmod

# PCI 장치와 드라이버
lspci -k

# NVMe 장치
nvme list

# 네트워크 드라이버
ethtool -i eth0

# 커널 로그
dmesg
journalctl -k

# 인터럽트 상태
cat /proc/interrupts

# Block Device Queue
cat /sys/block/nvme0n1/queue/*
```

### Runtime 관측 포인트

- IRQ usage
- softirq load
- queue depth
- packet drop
- DMA latency
- storage timeout

### Kubernetes

```bash
# Node kernel log
kubectl logs
journalctl

# CNI / CSI driver 상태 확인
kubectl get ds -A
```

관련 드라이버: CNI plugin, CSI storage driver, SR-IOV driver, GPU/NIC plugin

### 실무 중요 관측

```
node instability
  → driver issue
    → firmware mismatch
```

이 패턴은 실무에서 매우 흔하게 발생합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*