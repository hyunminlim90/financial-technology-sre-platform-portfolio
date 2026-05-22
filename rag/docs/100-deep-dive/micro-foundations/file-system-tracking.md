# 파일 시스템 트래킹 (File System Tracking)

> 정독: 0회

## 1. 이 기술이 무엇인가

파일 시스템 트래킹(File System Tracking)은:

> 운영체제가 파일의 실제 저장 위치와 연결 구조를 **추적·해석·탐색하는 메커니즘**

### 핵심 역할

운영체제는 파일 이름, 디렉터리 경로, inode/MFT, block pointer, extent tree 등을 따라가며 **파일 데이터가 실제로 어느 블록에 저장되어 있는지 찾아냅니다.**

### 핵심 개념

파일은 실제 디스크에서 연속적으로 저장된다는 **보장이 없습니다.** 따라서 운영체제는 **메타데이터 포인터 체인을 추적**해야 합니다.

---

## 2. 시스템 어디에서 등장하는가

파일 시스템 트래킹은 **파일 접근이 일어나는 거의 모든 순간** 동작합니다.

### 주요 위치

| 계층 | 역할 |
|------|------|
| Application | 파일 요청 |
| VFS | 파일 추상화 |
| Filesystem | inode/extents 추적 |
| Block Layer | block I/O 생성 |
| Storage Device | 실제 데이터 읽기 |

### 대표 흐름

```
open()
  → path lookup
  → inode lookup
  → block mapping lookup
  → storage read
```

### Kubernetes 환경에서 특히 중요한 항목

- container image layer
- OverlayFS
- Persistent Volume
- etcd storage
- log file tracking

### 데이터베이스 환경에서 매우 중요한 항목

- WAL segment
- SSTable
- DB page mapping
- checkpoint file

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **Disk I/O + Metadata Lookup**입니다.

| 자원 | 영향 |
|------|------|
| Disk | block seek/read |
| Memory | inode cache / dentry cache |
| CPU | path traversal |
| Network | distributed filesystem lookup |

### 특히 중요한 부분

작은 파일이 많을 경우, 실제 데이터보다 **메타데이터 탐색 비용이 더 커질 수 있습니다.**

대표 사례:
- container image layer
- millions of log files
- package repository
- object index tree

---

## 4. 왜 중요한가

파일 시스템 트래킹은 **논리 파일 이름과 실제 물리 저장 위치를 연결하는 핵심 메커니즘**입니다.

### 이것이 없으면

운영체제는 다음이 불가능해집니다.

- 파일 위치 탐색
- 데이터 복원
- block mapping

### 매우 중요한 이유

현대 스토리지는 fragmentation, wear leveling, copy-on-write, extent allocation 등으로 인해 **데이터 위치가 지속적으로 변합니다.** 따라서 운영체제는 항상 **현재 유효한 데이터 위치를 추적**해야 합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

파일 시스템 트래킹 문제는 실제 운영 장애로 직결됩니다.

### 대표 장애 유형

| 유형 | 설명 |
|------|------|
| Metadata Corruption | inode/extents 손상 → file inaccessible, I/O error |
| Orphaned Block | block pointer 유실 |
| Directory Corruption | path traversal 실패 |
| Extent Tree Damage | 대용량 파일 손상 가능 |
| Filesystem Fragmentation | tracking overhead 증가 |
| Slow Metadata Lookup | small file 폭증 시 latency 증가 |

### Kubernetes 환경에서 자주 발생하는 문제

| 문제 | 영향 |
|------|------|
| OverlayFS metadata overload | container slowdown |
| inode pressure | pod 장애 |
| volume corruption | mount failure |

### SSD 환경에서 매우 중요한 점

FTL 주소 변환과 filesystem block tracking은 **별개 계층**입니다.

---

## 6. 핵심 메커니즘

핵심 추적 흐름:

```
파일 이름 → inode → block mapping → physical storage
```

### 실제 흐름

**1. Path Lookup**

```
/data/report.txt
```

운영체제가 directory tree 탐색.

**2. inode Lookup**

inode 획득 후 확인 항목: size, permission, block pointer, extent info.

**3. Block Tracking**

inode 내부 포인터를 따라가며 논리 블록과 물리 주소를 매핑.

```
logical block 0  →  LBA 9000
logical block 1  →  LBA 12000
```

**4. Fragment Tracking**

파일이 조각난 경우, 여러 extent를 연결하여 하나의 논리 파일처럼 재조립.

**5. Storage Read**

최종 block I/O 수행.

### 매우 중요한 개념

파일 시스템은 파일 자체를 저장한다기보다 **파일 위치 정보를 계속 추적·관리**합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux 명령어

```bash
# inode 정보
stat file.txt

# filesystem 구조
df -h

# inode 사용량
df -i

# extent 정보
filefrag file.txt

# mount 상태
mount

# open file 추적
lsof

# block device
lsblk
```

### Kernel 관측 경로

```
/proc/fs/
/proc/mounts
```

### Kubernetes 중요 영역

| 구성 요소 | 관련성 |
|-----------|--------|
| kubelet | volume path tracking |
| OverlayFS | layered filesystem |
| CSI | storage mapping |
| container runtime | image filesystem |

### Runtime 관측 포인트

- inode cache hit ratio
- dentry cache
- metadata latency
- filesystem fragmentation
- extent lookup cost
- mount lookup failure

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*