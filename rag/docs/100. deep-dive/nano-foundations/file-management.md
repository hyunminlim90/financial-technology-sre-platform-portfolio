# 파일 관리 (File Management)

> 정독: 0회

## 1. 이 기술이 무엇인가

파일 관리(File Management)는:

> 운영체제가 파일의 생성·수정·삭제·권한·저장 위치를 **통제하고 유지하는 관리 메커니즘**

### 핵심 역할

운영체제는 단순히 데이터를 저장하는 것이 아니라 다음을 지속적으로 관리합니다.

- 어디 저장할지
- 누가 접근 가능한지
- 얼마나 사용 중인지
- 어떤 블록을 점유하는지
- 삭제 시 어떻게 회수할지

### 관리 대상

| 대상 | 설명 |
|------|------|
| 파일 | 데이터 개체 |
| 디렉터리 | 파일 계층 구조 |
| inode/MFT | 메타데이터 |
| block allocation | 저장 공간 |
| permission | 접근 권한 |
| journal | 무결성 기록 |

> 파일 관리는 **스토리지 공간의 질서 유지 시스템**입니다.

---

## 2. 시스템 어디에서 등장하는가

파일 관리는 운영체제 커널 내부의 핵심 기능입니다.

### 주요 위치

| 계층 | 역할 |
|------|------|
| Application | 파일 요청 |
| System Call | open/read/write |
| VFS | 파일시스템 추상화 |
| Filesystem | ext4/xfs/ntfs |
| Block Layer | block I/O |
| Storage Device | SSD/HDD |

### Linux 구조

```
Application
    → syscall
    → VFS
    → filesystem
    → block layer
    → SSD/HDD
```

### Kubernetes 환경

파일 관리는 다음 영역에서 매우 중요합니다.

- container image layer
- volume mount
- persistent volume
- log file rotation

### 데이터베이스 환경에서 특히 중요한 항목

- WAL file
- data file
- fsync
- checkpoint
- lock file

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **Disk / Storage I/O**이며, 실제로는 복합적으로 작용합니다.

| 자원 | 영향 |
|------|------|
| Disk | block allocation |
| Memory | page cache / inode cache |
| CPU | metadata 처리 |
| Network | distributed filesystem |

### 특히 중요한 부분 — Metadata I/O

작은 파일이 많으면 데이터보다 **메타데이터 접근 비용이 더 커질 수 있습니다.**

대표 사례:
- millions of tiny files
- container image layer
- log rotation storm

---

## 4. 왜 중요한가

파일 관리는 **스토리지 시스템의 안정성과 일관성을 유지하는 핵심 계층**입니다.

### 없으면 발생하는 문제

- 파일 충돌
- 권한 붕괴
- block leak
- filesystem corruption
- 데이터 유실

### 운영체제 핵심 역할

운영체제는 논리 파일과 실제 물리 저장 공간 사이를 **계속 동기화**해야 합니다.

### 특히 중요한 이유

현대 시스템은 컨테이너, 로그, DB, 이미지 레이어, snapshot 등으로 인해 파일 생성/삭제가 매우 빈번합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

파일 관리 문제는 운영 장애로 직결됩니다.

### 대표 장애 유형

| 유형 | 설명 |
|------|------|
| inode exhaustion | 디스크 용량은 남았는데 inode 고갈로 파일 생성 실패 |
| filesystem corruption | 갑작스런 power loss |
| journal replay failure | boot 실패 가능 |
| permission issue | `Permission denied` — 권한 잘못 설정 |
| open file leak | fd 고갈 |
| log explosion | 로그 파일 무한 증가 |
| orphaned block | 공간 회수 실패 |

### Kubernetes 환경에서 자주 발생하는 문제

| 문제 | 영향 |
|------|------|
| container log accumulation | disk full |
| volume mount issue | pod startup failure |
| inode exhaustion | node 장애 |

### DB 환경에서 매우 중요한 항목

- fsync stall
- WAL corruption
- journal replay
- file descriptor exhaustion

---

## 6. 핵심 메커니즘

핵심 메커니즘은 **파일 이름을 실제 저장 블록 주소로 변환하고 지속적으로 상태를 관리하는 구조**입니다.

### 기본 흐름

**1. 파일 요청**

```bash
open("/data/app.log")
```

**2. 파일시스템 lookup**

운영체제가 directory lookup → inode lookup 수행.

**3. 메타데이터 확인**

확인 항목: 권한, owner, size, block mapping.

**4. block I/O 수행**

실제 block device 접근.

**5. cache 동기화**

page cache, inode cache, journal 업데이트.

### 매우 중요한 개념

파일은 실제로 연속된 공간에 저장된다는 **보장이 없습니다.** 파일 관리 시스템이 여러 block 조각들을 논리적으로 하나의 파일처럼 추상화합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux 명령어

```bash
# 파일시스템 상태
df -h

# inode 상태
df -i

# 파일 열람 수
lsof

# mount 상태
mount

# block device 확인
lsblk

# inode 정보
stat file.txt

# filesystem 오류 확인
dmesg
```

### Kernel 관측 경로

```
/proc/sys/fs/
/proc/mounts
```

### Kubernetes 중요 영역

| 구성 요소 | 관련성 |
|-----------|--------|
| kubelet | volume 관리 |
| CSI | storage orchestration |
| container runtime | image layer |
| PV/PVC | persistent storage |

### Runtime 관측 포인트

- inode usage
- fsync latency
- open file count
- mount failure
- disk pressure
- journal replay

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*