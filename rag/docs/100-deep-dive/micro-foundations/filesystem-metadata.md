# 파일시스템 메타데이터 (Filesystem Metadata)

> 정독: 0회

## 1. 이 기술이 무엇인가

파일시스템 메타데이터는:

> **파일의 실제 내용(data) 이외에, 파일을 저장·관리·추적하기 위한 제어 정보 집합**

### 핵심 역할

운영체제가 다음을 수행하기 위해 사용합니다:

- 파일 위치 찾기 / 파일 권한 관리
- 디렉토리 탐색 / 블록 매핑 / 무결성 유지

### 메타데이터에 포함되는 대표 정보

| 항목 | 설명 |
|---|---|
| 파일 크기 | byte 단위 크기 |
| 권한 | read / write / execute |
| 소유자 | uid / gid |
| timestamp | 생성 / 수정 / 접근 시간 |
| 블록 위치 | 실제 저장 블록 주소 |
| 링크 수 | hard link count |
| 파일 타입 | regular / directory / socket |

### 핵심 구분

| 구분 | 의미 |
|---|---|
| 데이터 (Data) | 실제 파일 내용 |
| 메타데이터 (Metadata) | 파일 관리 정보 |

---

## 2. 시스템 어디에서 등장하는가

파일시스템 메타데이터는 거의 모든 스토리지 계층에 존재합니다.

### 등장 위치

| 계층 | 역할 |
|---|---|
| Filesystem | inode / MFT 관리 |
| Block Layer | block mapping |
| SSD | logical block translation |
| OS Kernel | VFS cache |
| Database | WAL / index metadata |
| Object Storage | object index |

### 대표 구현체

**Linux** — ext4 → inode / XFS → inode + allocation metadata / btrfs → tree metadata

**Windows** — NTFS → MFT

**Cloud Storage** — object index / chunk map / replication metadata

### Kubernetes

다음 영역에서 메타데이터가 매우 중요합니다:

- etcd / PersistentVolume / Container Image Layer / CSI Storage

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Disk I/O**이지만, 실제로는 모든 계층과 연결됩니다.

| 자원 | 영향 |
|---|---|
| Disk | 핵심 |
| Memory | inode cache / page cache |
| CPU | metadata traversal |
| Network | distributed metadata sync |

### 특히 중요한 부분

메타데이터 접근은 **작은 random I/O를 매우 많이 발생**시키는 경우가 많습니다. 따라서 고성능 스토리지 환경에서도 inode lookup, directory traversal, journal update가 병목이 될 수 있습니다.

---

## 4. 왜 중요한가

파일 데이터보다 **메타데이터 손상이 시스템 전체 장애로 이어지는 경우가 많습니다.**

### 이유

파일시스템은 **메타데이터를 기반으로 실제 데이터 위치를 해석**하기 때문입니다.

### 메타데이터 손상 시 발생 가능한 문제

- 파일 위치 상실 / 디렉토리 손상 / mount 실패
- orphan inode / journal recovery 반복

### 실제 영향

| 장애 | 영향 |
|---|---|
| inode corruption | 파일 접근 실패 |
| journal corruption | recovery loop |
| directory metadata failure | 전체 디렉토리 손상 |
| allocation bitmap error | block overlap |
| superblock corruption | mount 불가 |

> **데이터센터 환경:** 특히 **메타데이터 IOPS 성능**이 스토리지 체감 성능을 좌우합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 대표 장애 유형

| 장애 유형 | 설명 |
|---|---|
| Journal Corruption | 비정상 종료 시 metadata inconsistency |
| Inode Corruption | 파일 위치 정보 손상 |
| Metadata Exhaustion | inode 부족 |
| Directory Fragmentation | lookup latency 증가 |
| SSD Metadata Write Stall | journal flush 지연 |

### 실제 시스템 증상

| 증상 | 원인 |
|---|---|
| mount 실패 | superblock 손상 |
| 파일 사라짐 | inode corruption |
| fsck 반복 | metadata inconsistency |
| 높은 iowait | metadata random I/O |
| etcd latency | sync metadata flush |
| container startup 지연 | overlay metadata traversal |

> **매우 중요한 특징:** 파일 데이터보다 **메타데이터가 훨씬 더 자주 갱신**되는 경우가 많습니다.

---

## 6. 핵심 메커니즘

핵심 메커니즘은 **파일 이름 → 메타데이터 → 블록 주소** 변환 과정입니다.

### 실제 흐름

```
사용자 요청: cat file.txt
↓
directory metadata 탐색
↓
inode 조회
↓
block mapping 확인
↓
block device read
↓
SSD logical block 접근
↓
NAND physical page 접근
```

> **매우 중요한 구조:** 파일 내용은 **메타데이터가 없으면 찾을 수 없습니다.**

### Filesystem 핵심 구조

| 구조 | 역할 |
|---|---|
| Superblock | 파일시스템 전체 정보 |
| Inode | 파일 메타데이터 |
| Directory Entry | 이름 ↔ inode 연결 |
| Journal | metadata consistency |
| Bitmap | free block 관리 |

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

| 도구 | 용도 |
|---|---|
| `df -i` | inode 사용량 |
| `dumpe2fs` | filesystem 상태 |
| `stat file.txt` | inode 조회 |
| `filefrag` | block mapping |

`dmesg` 에서 다음을 확인할 수 있습니다:

- inode error / journal recovery / metadata checksum error

### 성능 관측

| 도구 | 용도 |
|---|---|
| `iostat -x 1` | metadata-heavy workload |
| `slabtop` | inode cache |

### Kubernetes

| 영역 | 영향 |
|---|---|
| etcd | small sync write |
| overlayfs | image metadata |
| CSI | volume metadata |
| container runtime | layer lookup |

### 실제 병목 예시

- 수백만 small file
- massive directory traversal
- image unpack
- log rotation

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*