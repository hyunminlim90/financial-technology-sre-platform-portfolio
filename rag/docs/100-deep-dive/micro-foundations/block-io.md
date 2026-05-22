# 블록 입출력 (Block I/O)

> 정독: 0회

## 1. 이 기술이 무엇인가

블록 입출력(Block I/O)은:

> 스토리지 장치와 데이터를 주고받을 때 고정 크기 블록 단위로 데이터를 처리하는 커널 기반 I/O 메커니즘

**핵심 특징:**

- 블록 단위 처리
- 임의 접근(Random Access)
- 주소 기반 I/O
- 큐 기반 처리
- 비동기 처리 가능

**주요 대상:** SSD, HDD, NVMe, SATA, RAID, SAN, Virtual Disk

블록 I/O는 파일 단위가 아니라 **논리 블록(LBA) 단위**로 동작합니다.

```
예: LBA 1000 ~ 1007 읽기
```

> **핵심:** Block I/O는 스토리지 장치와 커널 사이의 실제 데이터 트랜잭션 계층입니다.

<details>
<summary>Deep Dive</summary></br>

Secondary Storage Device(보조 기억 장치) [[M]](../../100-deep-dive/micro-foundations/secondary-storage-device.md)  
Data(데이터) [[M]](../../100-deep-dive/micro-foundations/data.md)  
Data Transfer(데이터 전송) [[M]](../../100-deep-dive/micro-foundations/data-transfer.md)  
Logical Block(논리 블록) [[M]](../../100-deep-dive/micro-foundations/logical-block.md)  
I/O Processing(입출력 처리) [[M]](../../100-deep-dive/micro-foundations/io-processing.md)  
Kernel-level I/O(커널 수준 입출력) [[M]](../../100-deep-dive/micro-foundations/kernel-level-io.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

### Linux Kernel 핵심 위치

VFS 아래 / filesystem 아래 / device driver 위

```
Application
→ System Call
→ VFS
→ Filesystem
→ Block Layer
→ Device Driver
→ SSD/HDD
```

### Filesystem 내부

ext4 / xfs / btrfs 모두 Block I/O를 생성합니다.

### Database

- WAL write
- page flush
- fsync

### Kubernetes

Persistent Volume 사용 시 핵심:

- CSI
- container writable layer
- overlayfs
- local PV

### Virtualization

- virtual disk
- qcow2
- virtio-blk

> **핵심:** 대부분의 영속 스토리지 접근은 최종적으로 Block I/O로 변환됩니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

### Disk 영향 — 매우 큼

- IOPS
- latency
- throughput
- queue depth

### CPU 영향 — 생각보다 큼

- interrupt 처리
- queue 관리
- I/O scheduling
- completion polling

### Memory 영향 — 중요

- page cache
- DMA buffer
- bio buffer
- request queue

### Network 영향 — 네트워크 스토리지에서 중요

- SAN / NAS
- Ceph
- iSCSI

> **핵심:** Block I/O는 스토리지 성능과 시스템 지연시간의 핵심 병목 지점입니다.

---

## 4. 왜 중요한가

현대 시스템 성능 대부분이 I/O에 의해 제한됩니다. CPU는 매우 빠르지만 SSD/HDD/network storage는 상대적으로 느리기 때문에 **I/O 효율 = 전체 시스템 효율**이 되는 경우가 많습니다.

### 중요한 이유

| 항목 | 설명 |
|---|---|
| 성능 | I/O 병목은 전체 서비스 latency 증가 |
| 안정성 | I/O hang 발생 시 DB freeze / pod stuck / filesystem freeze 가능 |
| 데이터 무결성 | flush/fsync 실패 시 corruption / journal replay / partial write 발생 가능 |
| 확장성 | 고부하 시스템에서 queue tuning / scheduler tuning / async I/O 필수 |

### SRE 관점

> 많은 장애가 CPU 부족이 아니라 **Block I/O saturation**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 대표 장애 유형

| 장애 | 현상 |
|---|---|
| I/O Wait 증가 | CPU idle인데 서비스 느림 |
| Disk Queue Saturation | queue depth 폭증 |
| fsync latency 증가 | DB TPS 급락 |
| NVMe timeout | storage hang |
| Filesystem corruption | journal replay 발생 |
| Dirty page 폭증 | writeback congestion 발생 |
| K8s Pod Freeze | Persistent Volume I/O stall |

### 환경별 주요 이슈

| 환경 | 특징 |
|---|---|
| HDD | random I/O가 매우 치명적 |
| SSD | GC(Garbage Collection) 순간 latency spike 발생 가능 |
| Cloud | EBS/Network storage throttling 자주 발생 |

**실제 현상 예:**

```
application timeout
→ 실제 원인: storage latency spike
```

> **핵심:** Block I/O 문제는 애플리케이션 장애처럼 보이는 경우가 매우 많습니다.

---

## 6. 핵심 메커니즘

**핵심 흐름:**

```
filesystem request → block layer → request queue → driver → hardware
```

### 전체 흐름

```
1단계  상위 요청       write(fd, buffer, 4096)
2단계  Filesystem      inode / extent / metadata 해석
3단계  Block I/O 생성  bio/request 객체 생성
                        (device / LBA / size / direction / memory address 포함)
4단계  Block Layer     merging → splitting → scheduling → queueing
5단계  Driver 전달     NVMe/SATA driver로 전달
6단계  Hardware Queue  submission queue / completion queue 등록
7단계  DMA 수행        device가 직접 memory 접근
8단계  Completion      interrupt로 I/O 완료 통지
```

### 핵심 용어

| 용어 | 설명 |
|---|---|
| `bio` | Linux block I/O request 객체 |
| `request queue` | 디바이스 처리 대기열 |
| `scheduler` | I/O ordering 결정 |
| `queue depth` | 동시 outstanding I/O 개수 |
| `DMA` | CPU 우회 메모리 전송 |

> **핵심:** Block I/O의 핵심은 요청 큐 기반 비동기 처리입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 블록 장치 확인
lsblk

# I/O 상태 (핵심: await / svctm / util / r/s / w/s)
iostat -x 1

# 프로세스 I/O
iotop

# block device queue
cat /sys/block/nvme0n1/queue/*

# scheduler 확인
cat /sys/block/nvme0n1/queue/scheduler

# kernel block trace
blktrace

# bio/request trace
bpftrace
perf
ftrace
```

### Runtime 관측 포인트

- page cache
- async I/O
- direct I/O
- mmap
- io_uring

### Kubernetes

```bash
# PV 상태
kubectl describe pv

# node disk pressure
kubectl describe node

# CSI 관련 로그
kubectl logs
```

container writable layer는 overlayfs 기반 Block I/O가 발생합니다.

### Cloud

- EBS burst limit
- IOPS throttling
- storage latency

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*