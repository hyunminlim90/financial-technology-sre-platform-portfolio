# 블록 특수 파일 (Block Special File)

> 정독: 0회

## 1. 이 기술이 무엇인가

블록 특수 파일(Block Special File)은:

> 스토리지 장치를 파일 시스템 내부의 특수 파일 객체로 표현한 커널 장치 노드

**대표 위치:**

```
/dev/sda
/dev/nvme0n1
/dev/vda
```

**일반 파일과 차이점:**

| 구분 | 설명 |
|---|---|
| 목적 | 실제 데이터 저장이 아닌 하드웨어 장치 연결용 |
| inode | 내부 구조가 다름 |
| 식별자 | device identifier 보유 |
| 연결 | driver와 연결됨 |

파일처럼 보이지만 실제로는 **block device driver 연결 창구**입니다.

Linux의 **"Everything is a file"** 철학의 핵심 구현체 중 하나입니다.

> **핵심:** Block Special File은 스토리지 장치를 파일 시스템에 연결하는 커널 장치 객체입니다.

<details>
<summary>Deep Dive</summary></br>

Secondary Storage Device(보조 기억 장치) [[M]](../../100-deep-dive/micro-foundations/secondary-storage-device.md)  
inode Object(아이노드 객체) [[M]](../../100-deep-dive/micro-foundations/inode-object.md)  
Device File(장치 파일) [[M]](../../100-deep-dive/micro-foundations/device-file.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

### Linux Device Model

대표 위치: `/dev`

### Filesystem Mount

```bash
mount /dev/nvme0n1p1 /data
```

filesystem이 block special file을 대상으로 mount를 수행합니다.

### Boot 과정

부팅 시 root filesystem mount / initramfs / bootloader handoff 전부 block special file을 사용합니다.

### Storage Stack

```
Application
→ VFS
→ filesystem
→ block special file
→ block layer
→ device driver
→ SSD/HDD
```

### LVM / RAID / dm

- `/dev/mapper/*`
- md device
- dm-crypt

### Virtualization

- `/dev/vda`
- `/dev/xvda`

### Kubernetes

container runtime도 내부적으로 block device를 사용합니다:

- CSI
- PV
- overlayfs lower storage

> **핵심:** Linux 스토리지 계층 대부분은 최종적으로 block special file 기반입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

### Disk 영향 — 매우 큼

filesystem / mount / block I/O / journal / page cache 전부 block device 기반입니다.

### CPU 영향 — 있음

- syscall
- block layer
- interrupt
- queue scheduling

### Memory 영향 — 중요

- page cache
- bio buffer
- request queue
- DMA memory

### Network 영향 — 네트워크 스토리지에서 중요

iSCSI / NVMe-oF / Ceph RBD 등 결국 block device 형태로 노출 가능합니다.

> **핵심:** Block Special File은 스토리지 I/O의 커널 진입점 역할을 합니다.

---

## 4. 왜 중요한가

### 하드웨어를 파일처럼 사용 가능

```bash
cat /dev/nvme0n1
dd if=/dev/sda
```

### filesystem 독립성 제공

filesystem은 SSD 종류 / controller 차이 / vendor 차이를 몰라도 됩니다.

### mount 가능 기반 제공

filesystem은 block special file을 대상으로 mount를 수행합니다.

### 가상 장치 생성 가능

- loop device
- device mapper
- LVM
- dm-crypt

### UNIX I/O 모델 통합

모든 I/O를 `open` / `read` / `write` / `close` 형태로 통합 가능합니다.

> **핵심:** Block Special File은 스토리지 장치를 UNIX 파일 모델에 통합하는 핵심 구조입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 대표 장애 유형

| 장애 | 설명 |
|---|---|
| Device Missing | `/dev/nvme0n1` 없음 |
| Major/Minor mismatch | driver mapping 오류 |
| Mount Failure | block device 인식 실패 |
| Filesystem Corruption | block device read failure 발생 가능 |
| Device Busy | `device is busy` |
| Permission 문제 | container/runtime 접근 실패 |
| udev 문제 | 장치 노드 자동 생성 실패 |
| Multipath 충돌 | storage mapping 혼선 발생 |

### Kubernetes 장애

| 장애 | 원인 |
|---|---|
| PV mount 실패 | CSI가 device node 생성 실패 |
| container startup 실패 | block device attach 실패 |
| Virtualization 장애 | virtio block device 인식 실패 |

> **핵심:** 많은 storage 장애는 filesystem 이전 단계인 **block device 계층**에서 발생합니다.

---

## 6. 핵심 메커니즘

**inode + major/minor number + driver binding** 이 핵심입니다.

### 일반 파일 vs Block Special File

| 구분 | inode 저장 내용 |
|---|---|
| 일반 파일 | data block pointer |
| Block Special File | major number / minor number / device type |

### Major / Minor Number

| 번호 | 역할 | 예시 |
|---|---|---|
| Major Number | 어떤 driver 계열인지 식별 | NVMe / SCSI / loop device |
| Minor Number | driver 내부의 개별 장치 식별 | disk / partition |

### I/O 흐름

```
1단계  Application    read("/dev/nvme0n1")
2단계  VFS            inode 확인
3단계  VFS            inode type: b (block special file) 확인
4단계  VFS            major/minor number 추출
5단계  커널           해당 block driver lookup
6단계  커널           block layer 진입
7단계  Block Layer    bio/request 생성
8단계  Driver         → hardware 전달
```

> **중요:** block special file 자체가 데이터를 저장하는 것이 아닙니다. 핵심은 **커널 내부 block device 객체 연결**입니다.

### Block Device vs Character Device

| 구분 | 특성 | 예시 |
|---|---|---|
| Block Device | random access 가능 / block 단위 처리 / page cache 사용 가능 | `/dev/sda` |
| Character Device | stream 기반 / sequential I/O / block cache 없음 | `/dev/tty` |

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# block device 확인
lsblk

# device node 확인 (b = block special file)
ls -l /dev
# 출력 예: brw-rw---- 1 root disk ...

# major/minor 확인
ls -l /dev/nvme0n1
# 출력 예: 259, 0

# device metadata
udevadm info

# block layer 정보
cat /proc/devices

# sysfs
ls /sys/block
```

### Runtime

container runtime도 block device를 사용합니다:

- overlayfs
- writable layer
- image layer

### Kubernetes

```bash
# node device 상태
kubectl describe node

# container 내부 device mapping
crictl inspect
```

PV 연결 시 CSI가 block device attach를 수행합니다.

### Cloud

EBS / Persistent Disk / Azure Disk 모두 최종적으로 Linux block device로 노출됩니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*