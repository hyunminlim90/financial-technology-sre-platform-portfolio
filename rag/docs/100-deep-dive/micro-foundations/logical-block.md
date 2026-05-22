# 논리 블록 (Logical Block)

> 정독: 0회

## 1. 이 기술이 무엇인가

논리 블록(Logical Block)은:

> 스토리지 장치를 일정한 크기의 번호 붙은 데이터 단위로 추상화

```
Storage Device
→ Logical Block 0
→ Logical Block 1
→ Logical Block 2
→ ...
→ Logical Block N
```

운영체제와 파일시스템은 SSD 내부의 NAND page, block, die, channel 같은 물리 구조를 직접 다루지 않고, **논리 블록 번호만 사용**합니다.

> **핵심:** 논리 블록 = 운영체제가 스토리지에 I/O를 요청할 때 사용하는 고정 크기 주소 단위

일반적으로 512B 또는 4KB 단위가 많이 쓰이며, 현대 시스템에서는 **4KB 정렬**이 중요합니다.

<details>
<summary>Deep Dive</summary></br>

Secondary Storage Device(보조 기억 장치) [[M]](../../100-deep-dive/micro-foundations/secondary-storage-device.md)  
Logical Block Addressing(논리 블록 주소 지정) [[M]](../../100-deep-dive/micro-foundations/logical-block-addressing.md)  
Device Abstraction(장치 추상화) [[M]](../../100-deep-dive/micro-foundations/device-abstraction.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

논리 블록은 **파일시스템 아래, 물리 스토리지 위**에 등장합니다.

```
Application
→ VFS
→ Filesystem
→ Block Layer
→ Logical Block / LBA
→ Device Driver
→ SSD/HDD Controller
→ Physical Media
```

파일시스템이 파일 데이터를 읽으려면 다음과 같이 요청합니다:

```
"이 파일의 데이터는 LBA 10500부터 8개 블록에 있다"
```

Kubernetes에서도 PV / PVC / CSI / container writable layer / node filesystem 모두 결국 하부에서는 **논리 블록 기반 I/O**로 변환됩니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 자원은 **Disk**입니다.

### Disk 영향 — 매우 큼

논리 블록 크기와 정렬은 다음에 영향을 줍니다:

- Disk latency
- IOPS
- Throughput
- write amplification
- filesystem efficiency
- page cache 효율

### CPU 영향 — 있음

블록이 작고 요청이 많으면 I/O request 처리 / interrupt / queue dispatch 비용이 증가합니다.

### Memory 영향 — 있음

page cache / buffer cache / DMA buffer가 블록 단위와 맞물려 동작합니다.

### Network 영향 — 네트워크 스토리지에서 중요

로컬 디스크에서는 직접 영향이 적지만, iSCSI / Ceph RBD / NVMe-oF 같은 네트워크 블록 스토리지에서는 논리 블록 I/O가 네트워크 패킷으로 운반됩니다.

---

## 4. 왜 중요한가

논리 블록은 **스토리지 추상화의 핵심 단위**입니다.

| 계층 | 논리 블록의 역할 |
|---|---|
| Filesystem | 논리 블록을 기준으로 파일 데이터를 배치 |
| 커널 Block Layer | 논리 블록을 기준으로 I/O를 생성 |
| Storage Controller | 논리 블록 주소를 물리 주소로 변환 |

논리 블록이 있어야 상위 소프트웨어는 **하드웨어 내부 구조를 몰라도** 데이터를 읽고 쓸 수 있습니다.

**정렬의 중요성:** 파일시스템 블록 / 메모리 페이지 / 스토리지 논리 블록이 모두 4KB 기준으로 맞으면 불필요한 read-modify-write가 줄어듭니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

논리 블록 관련 문제는 주로 **성능 저하**와 **데이터 손상 위험**으로 나타납니다.

### 대표 장애 유형

| 장애 | 원인 | 결과 |
|---|---|---|
| 블록 정렬 불일치 | partition alignment 오류 | write amplification 증가 → SSD latency 증가 |
| bad block / media error | 특정 LBA read 실패 | filesystem error |
| partition alignment 오류 | 파티션 비정렬 | random I/O 성능 저하 |
| block size mismatch | DB page / filesystem block / device block 불일치 | 전체 I/O 비효율 |

> **SRE 관점:** 애플리케이션이 느려 보이지만 실제 원인이 하부 logical block alignment, storage queue, LBA error인 경우가 있습니다.

---

## 6. 핵심 메커니즘

### 전체 흐름

```
파일 offset
→ filesystem block
→ logical block address (LBA)
→ block I/O request
→ device driver
→ physical address
```

**예시:** 애플리케이션이 파일의 8192번째 바이트를 읽을 때

```
1단계  Filesystem    8192번째 바이트가 몇 번째 파일시스템 블록인지 계산
2단계  Filesystem    해당 블록이 스토리지의 어떤 LBA에 매핑되는지 확인
3단계  Block Layer   해당 LBA를 대상으로 read request 생성
```

### SSD 내부 변환

```
LBA → FTL → PBA → NAND page/block
```

> **핵심:** 논리 블록은 운영체제가 보는 주소 단위이고, 실제 물리 위치는 장치 내부 컨트롤러가 별도로 관리합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 블록 크기 및 정렬 정보 확인
lsblk -o NAME,PHY-SeC,LOG-SeC,MIN-IO,OPT-IO,ALIGNMENT

# 블록 크기 확인
blockdev --getss /dev/nvme0n1      # logical sector size
blockdev --getpbsz /dev/nvme0n1   # physical block size
blockdev --getbsz /dev/nvme0n1    # filesystem block size

# 파티션 정렬 확인
fdisk -l /dev/nvme0n1
parted /dev/nvme0n1 align-check optimal 1

# I/O 상태 확인
iostat -x 1
```

### Kubernetes

Kubernetes에서 "논리 블록"을 직접 보는 경우는 적지만, Node 내부에서 다음 계층으로 관측합니다:

```
PV/PVC → CSI volume → node block device → filesystem → logical block I/O
```

```bash
# K8s 리소스 상태 확인
kubectl describe pv
kubectl describe pvc
kubectl describe node

# 노드 내부 추적
lsblk
iostat
findmnt
df -h
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*