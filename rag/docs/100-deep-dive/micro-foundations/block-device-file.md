# 블록 장치 파일 (Block Device File)

> 정독: 0회

## 1. 이 기술이 무엇인가

블록 장치 파일(Block Device File)은:

> 스토리지 장치를 파일 형태로 추상화하여 운영체제가 접근할 수 있게 만든 특수 파일 객체

리눅스/유닉스 계열에서는 SSD, HDD, NVMe, 가상 디스크, LVM, RAID 등을 모두 `/dev` 아래의 블록 장치 파일로 노출합니다.

**대표 예시:**

```
/dev/sda
/dev/sdb
/dev/nvme0n1
/dev/vda
```

이 파일은 일반 데이터 파일이 아니라 **커널 블록 계층(Block Layer)과 연결된 특수 인터페이스 객체**입니다.

### 핵심 특징

| 특징 | 설명 |
|---|---|
| 블록 단위 접근 | 고정 크기 블록 기반 I/O 수행 |
| Random Access 가능 | 특정 위치(LBA) 직접 접근 가능 |
| 커널 Block Layer 연결 | 실제 디바이스 드라이버까지 연결됨 |

<details>
<summary>Deep Dive</summary></br>

Secondary Storage Device(보조 기억 장치) [[M]](../../100-deep-dive/micro-foundations/secondary-storage-device.md)  
Stream Interface(스트림 인터페이스) [[M]](../../100-deep-dive/micro-foundations/stream-interface.md)  
Device Abstraction(장치 추상화) [[M]](../../100-deep-dive/micro-foundations/device-abstraction.md)  
Operating System(운영체제) [[M]](../../100-deep-dive/micro-foundations/operating-system.md)  
Block I/O(블록 입출력) [[M]](../../100-deep-dive/micro-foundations/block-io.md)  
Block Special File(블록 특수 파일) [[M]](../../100-deep-dive/micro-foundations/block-special-file.md)  
File Object(파일 객체) [[M]](../../100-deep-dive/micro-foundations/file-object.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

블록 장치 파일은 **파일시스템과 물리 스토리지 사이**에 위치합니다.

### 전체 경로

```
Application
→ VFS
→ Filesystem
→ Block Device File
→ Block Layer
→ Device Driver
→ SSD/HDD
```

### Linux 구조

위치: `/dev`

```bash
ls -l /dev/nvme0n1
```

출력의 `b` 타입 = **block device**

### Filesystem과 관계

```bash
mkfs.ext4 /dev/nvme0n1
mount /dev/nvme0n1 /data
```

여기서 ext4는 블록 장치 파일을 대상으로 메타데이터 구조를 생성합니다.

### Kubernetes 환경

Kubernetes에서 등장하는 위치:

- Persistent Volume
- CSI
- container runtime
- overlay filesystem
- local storage

```
Pod Volume → CSI → Block Device → NVMe SSD
```

### 가상화 환경

VM에서는 `/dev/vda`, `/dev/xvda` 같은 paravirtualized block device를 사용합니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

### Disk 영향 — 매우 큼

- read/write latency
- queue depth
- throughput
- random IOPS

모두 블록 장치 경유.

### CPU 영향 — 상당히 큼

- interrupt handling
- block scheduling
- request merging
- queue processing

### Memory 영향 — 매우 큼

- page cache
- DMA buffer
- bio structure
- request queue

### Network 영향 — 직접 영향은 적음

단, distributed storage 환경에서는 영향이 커짐:

- iSCSI
- Ceph RBD
- network block device

> **핵심:** 블록 장치 파일은 실제 스토리지 I/O의 진입점입니다.

---

## 4. 왜 중요한가

### 스토리지 추상화 핵심

OS는 물리 장치를 직접 다루지 않고 블록 장치 파일로 통합 관리합니다.

### Filesystem 표준화

ext4/XFS/btrfs는 하부 SSD 구조를 몰라도 블록 장치 인터페이스만 사용합니다.

### Device Independence

동일 인터페이스로 모두 처리 가능:

- HDD
- SATA SSD
- NVMe SSD
- Virtual Disk
- SAN Storage

### Storage Virtualization 기반

아래 기술들 전부 블록 장치 기반:

- LVM
- RAID
- dm-crypt
- multipath
- loopback
- Ceph block

> **핵심:** 블록 장치 파일은 스토리지 추상화의 표준 진입 인터페이스입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실무 Disk 장애 대부분이 여기 연결됩니다.

### 대표 장애 유형

| 장애 | 설명 |
|---|---|
| Device Timeout | I/O timeout |
| Block Queue Saturation | queue depth 초과 |
| Filesystem Hang | block request stuck |
| Device Disconnect | NVMe reset |
| Read-only Remount | filesystem corruption |
| Disk Latency Explosion | storage tail latency 증가 |

### Linux 실제 로그

```
blk_update_request
I/O error
Buffer I/O error
nvme timeout
EXT4-fs error
```

### Kubernetes 장애

| 장애 | 원인 |
|---|---|
| PVC Mount Failure | CSI ↔ block device failure |
| Pod Startup Failure | volume attach 실패 |
| Node DiskPressure | block I/O saturation |

### SRE 핵심 포인트

> 애플리케이션 장애처럼 보여도 실제 원인은 **block device queue saturation**인 경우 많음.

---

## 6. 핵심 메커니즘

파일 형태의 요청을 블록 I/O 요청으로 변환하는 과정입니다.

### 전체 흐름

```
1단계  Application         read() / write()
2단계  VFS                 → filesystem 진입
3단계  Filesystem          LBA 계산 수행
4단계  Block Device File   bio 생성
5단계  Block Layer         request merge → scheduling → queue dispatch
6단계  Device Driver       NVMe/SATA command 생성
7단계  SSD Controller      FTL mapping → NAND 접근
```

> **핵심:** 블록 장치 파일은 파일 시스템과 실제 하드웨어 사이의 **커널 I/O 경계 객체**입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 장치 확인
lsblk

# block device 확인
ls -l /dev

# UUID/filesystem
blkid

# mount 상태
mount
findmnt

# I/O 통계
iostat
sar -d

# queue 상태
cat /sys/block/nvme0n1/queue/*

# latency tracing
blktrace
biosnoop      # bcc/eBPF
```

### Runtime

관측 포인트:

- fsync latency
- queue depth
- disk wait
- IOPS
- bio merge
- write amplification

### Kubernetes

```bash
# 볼륨 확인
kubectl describe pv
kubectl describe pvc

# node device 확인
lsblk
```

CSI 관측 포인트:

- attach/detach
- mount failure
- filesystem issue

> **핵심:** 컨테이너 스토리지도 결국 block device 기반입니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*