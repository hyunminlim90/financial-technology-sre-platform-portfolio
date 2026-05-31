# 보조 기억 장치 (Secondary Storage Device)

> 정독: 0회

## 1. 이 기술이 무엇인가

보조 기억 장치(Secondary Storage Device)는:

> 전원이 꺼져도 데이터가 유지되는 비휘발성 대용량 저장 장치

**주요 역할:**

- 운영체제 저장
- 파일 영구 저장
- 데이터베이스 저장
- 로그 저장
- 애플리케이션 저장
- 컨테이너 이미지 저장

**대표 장치:**

- SSD (NVMe SSD, SATA SSD)
- HDD
- eMMC
- UFS
- USB Storage

**핵심 특징:**

CPU가 직접 실행하지 않고 반드시 **RAM을 경유하여 접근**합니다.

> 즉, **Persistence(영속성)** 중심 계층입니다.

---

## 2. 시스템 어디에서 등장하는가

### 기본 계층 구조

```
Application
→ Filesystem
→ Block Layer
→ Device Driver
→ Secondary Storage Device
```

### 운영체제 부팅

부팅 시 Kernel Image, Root Filesystem, Init System 모두 보조 기억 장치에서 읽어옵니다.

### 데이터베이스

DB 파일 전체가 저장됩니다:

- WAL
- Redo Log
- Data File
- Index

### Kubernetes

등장 위치:

- Persistent Volume
- Container Image Layer
- etcd storage
- Node filesystem
- CSI backend

```
Pod → OverlayFS → Block Device → SSD
```

### 가상화 환경

VM Disk 자체가 보조 기억 장치 기반입니다:

- `qcow2`
- `vmdk`
- `raw image`

### Cloud 환경

Cloud block storage 전부 논리적 보조 기억 장치입니다:

- EBS
- Persistent Disk
- Managed Disk

---

## 3. 어떤 자원에 가장 영향이 큰가

### Disk 영향 — 절대적

- latency
- throughput
- IOPS
- queue depth
- fsync
- random write

### CPU 영향 — 상당히 큼

- interrupt / queue handling
- filesystem journaling
- checksum / compression / encryption

### Memory 영향 — 매우 큼

- page cache
- write buffer
- DMA
- filesystem cache

### Network 영향 — 로컬에서는 적음

단, 아래 환경에서는 매우 중요:

- SAN / NAS
- Ceph
- iSCSI
- EBS

> **핵심:** 현대 시스템 병목의 상당수가 보조 기억 장치 I/O입니다.

---

## 4. 왜 중요한가

### 데이터 영속성 제공

RAM은 휘발성이지만 보조 기억 장치는 **데이터 지속성(Persistence)** 을 제공합니다.

### 운영체제 기반

OS 자체가 저장되며, 없으면 boot/filesystem/application 모두 불가합니다.

### 대용량 저장 & 비용 효율

RAM보다 훨씬 큰 용량을 훨씬 저렴한 GB당 가격으로 제공합니다.

### 장애 복구 핵심

로그/백업/WAL 기반 recovery가 가능합니다.

### 현대 시스템 핵심 병목

CPU는 매우 빠르지만 storage latency는 상대적으로 느립니다. 그래서 async I/O, caching, batching, queueing 등이 중요합니다.

> **핵심:** 보조 기억 장치는 시스템의 영속성 기반입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 대표 장애 유형

| 장애 | 설명 |
|---|---|
| Disk Full | `No space left on device` |
| SSD Wear Out | P/E cycle 소진 |
| High Disk Latency | tail latency 증가 |
| Filesystem Corruption | unexpected shutdown |
| I/O Saturation | queue overload |
| Device Timeout | NVMe timeout |
| Write Amplification 증가 | SSD 성능 급락 |
| HDD Mechanical Failure | bad sector / seek error / rotation issue |

### Kubernetes 장애

| 장애 | 영향 |
|---|---|
| Node DiskPressure | Pod eviction 발생 |
| etcd latency 증가 | control plane 불안정 |
| Container startup delay | image pull/storage bottleneck |

### Database 장애 — 매우 치명적

- fsync stall
- WAL flush delay
- transaction latency explosion

### SRE 관점 핵심

> CPU 사용률이 정상이어도 **storage latency 하나로 전체 시스템 붕괴**가 가능합니다.

---

## 6. 핵심 메커니즘

**논리 블록 요청을 물리 저장 동작으로 변환**하는 과정입니다.

### 전체 흐름

```
1단계  Application         write() / fsync() 호출
2단계  Filesystem          inode update → journal write → block allocation
3단계  Block Layer         LBA 기반 I/O request 생성
4단계  Device Driver       NVMe command / SATA command 생성
5단계  Storage Controller  queue handling → cache management → address translation
```

### SSD 내부 핵심

FTL(Flash Translation Layer)이 LBA → PBA 변환을 수행하며, 추가로:

- wear leveling
- garbage collection
- bad block handling

을 처리합니다.

### HDD 내부 핵심

- head seek
- platter rotation
- magnetic write/read

> **핵심:** 보조 기억 장치는 단순 저장 공간이 아니라 **독립 컨트롤러와 복잡한 내부 firmware를 가진 자체 I/O 시스템**입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 장치 확인
lsblk

# 파일시스템 확인
df -h

# 디스크 사용량
du -sh

# I/O 성능
iostat -x 1

# 디바이스 상태
smartctl -a
nvme smart-log

# mount 상태
mount
findmnt

# block queue 확인
cat /sys/block/nvme0n1/queue/*
```

### Runtime 관측 포인트

- fsync latency
- disk wait
- write throughput
- queue depth
- page cache hit ratio

### Kubernetes

```bash
# PV/PVC 상태
kubectl get pv,pvc

# Node storage 상태
kubectl describe node

# DiskPressure 확인
kubectl get nodes
```

CSI 관측 포인트:

- attach / detach
- mount
- filesystem issue

### SRE 핵심 메트릭

| 메트릭 | 의미 |
|---|---|
| `await` | I/O 평균 대기 시간 |
| `svctm` | 실제 서비스 시간 |
| `util%` | 디스크 사용률 |
| `iowait` | CPU I/O 대기 비율 |
| `fsync latency` | fsync 응답 지연 |
| `queue depth` | 동시 I/O 요청 수 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*