# 논리 블록 주소 지정 (Logical Block Addressing, LBA)

> 정독: 0회

## 1. 이 기술이 무엇인가

**논리 블록 주소 지정(LBA)** 은:

> 스토리지 전체를 연속된 번호 기반 블록 배열로 다루는 표준 주소 체계

운영체제는 저장 장치를 `LBA 0`, `LBA 1`, `LBA 2`, ... `LBA N` 처럼 선형 공간으로 인식합니다.

### 핵심 특징

운영체제는 실제 NAND 위치, 디스크 플래터 위치, SSD 내부 구조, 채널/다이/플레인 구조를 알 필요 없이 오직 **논리 블록 번호(LBA)** 만 사용합니다.

LBA는 **복잡한 물리 저장 구조를 단순한 선형 주소 공간으로 추상화**합니다.

---

## 2. 시스템 어디에서 등장하는가

LBA는 스토리지 I/O 경로 전체에서 등장합니다.

### 주요 위치

```
Filesystem
  → Block Layer
    → Device Driver
      → NVMe/AHCI
        → SSD/HDD
```

### 파일 읽기 흐름

`read("data.log")` 호출 시:

```
1) 파일시스템       → inode/extents 조회
2) Block Layer     → 파일 오프셋을 LBA 범위로 변환
3) Driver          → NVMe/SATA command 생성
4) Device          → SSD controller가 LBA → 실제 NAND 위치(PBA) 매핑 수행
```

### Kubernetes 환경

PVC, filesystem volume, database page I/O, container image layer, etcd storage 모두 결국 LBA 기반입니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Disk / Storage I/O**입니다.

- **Disk**: random read/write, sequential I/O, seek pattern, queue depth
- **SSD 내부**: FTL mapping, wear leveling, garbage collection
- **CPU**: Block mapping과 interrupt 처리 발생
- **Memory**: page cache, block cache, I/O buffer 사용 증가

> 핵심: **LBA 접근 패턴이 스토리지 성능과 latency를 크게 결정**합니다.

---

## 4. 왜 중요한가

현대 스토리지 시스템의 거의 모든 I/O는 LBA 기반으로 동작합니다.

**하드웨어 독립성**: OS는 HDD, SATA SSD, NVMe SSD, RAID, SAN의 차이를 몰라도 됩니다.

**표준화**: 모든 block I/O를 동일한 주소 체계로 처리 가능합니다.

**SSD 내부 복잡성 은닉**: SSD 내부에서는 physical relocation, NAND remap, bad block replacement가 계속 일어나지만 OS는 이를 알 필요가 없습니다.

> LBA가 있기 때문에 **운영체제와 하드웨어가 느슨하게 결합 가능**합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

LBA 관련 문제는 실제 스토리지 장애와 매우 밀접합니다.

### 대표 장애 유형

**Bad Block** — 특정 LBA 읽기 실패

**Read Latency Spike** — 특정 LBA 영역에서 지연 급증

**SSD FTL Saturation** — LBA write 패턴 때문에 GC 폭증

**Write Amplification** — 랜덤 LBA write가 SSD 내부 erase 증가 유발

**Filesystem Corruption** — 잘못된 block mapping 발생

**RAID Rebuild Failure** — LBA consistency mismatch

**Alignment 문제** — 4KB alignment 불일치로 성능 저하

### Kubernetes / DB 장애 예시

| 증상 | 실제 원인 |
|---|---|
| DB latency 증가 | random LBA access 증가 |
| PVC I/O stall | SSD GC saturation |
| node disk pressure | excessive block write |
| etcd slow | sync write latency 증가 |

> **상위 애플리케이션 문제처럼 보여도 실제 원인은 하부 LBA I/O 패턴인 경우가 많습니다.**

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심 메커니즘은 **Logical Address → Physical Address 변환**입니다.

### HDD 시대

과거에는 CHS(Cylinder, Head, Sector) 기반이었으나 현대는 LBA 기반 선형 주소 체계를 사용합니다.

### SSD 내부 핵심

운영체제가 `Write LBA 5000` 명령을 전달하면, SSD controller 내부에서 `LBA 5000 → NAND Physical Page` 매핑이 수행됩니다.

SSD는 overwrite를 직접 할 수 없기 때문에 내부적으로 remap, relocate, erase, GC가 필수적으로 발생합니다. 운영체제는 이를 전혀 모른 채 **항상 동일한 LBA 공간만 사용**합니다.

### 핵심 포인트

```
LBA = 가상 주소 (논리적 위치)
PBA = 실제 물리 주소 (NAND 위치)
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 블록 장치 확인
lsblk

# 섹터 크기 확인
cat /sys/block/nvme0n1/queue/logical_block_size
cat /sys/block/nvme0n1/queue/physical_block_size

# 파티션 정렬
fdisk -l

# 파일시스템 매핑
filefrag

# 디스크 I/O 패턴
iostat -x
blktrace

# NVMe 정보
nvme list
nvme smart-log
```

### Runtime 관측 포인트

- random vs sequential I/O
- block alignment
- queue depth
- write amplification
- fsync latency

### Kubernetes

```bash
# PVC 기반 storage I/O
kubectl describe pvc

# Node disk 상태
kubectl top node
```

CSI / volume latency는 storage backend monitoring이 필요합니다.

### 핵심 관측 포인트

> Container 문제처럼 보이더라도 **실제 병목은 하부 block I/O와 LBA 패턴**일 수 있습니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*