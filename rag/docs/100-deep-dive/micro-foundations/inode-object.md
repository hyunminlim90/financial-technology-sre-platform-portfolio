# 아이노드 객체 (inode Object / struct inode)

> 정독: 0회

## 1. 이 기술이 무엇인가

**아이노드 객체(inode object)** 는:

> 파일의 실제 데이터가 아니라, 파일 자체의 메타데이터와 저장 위치 정보를 관리하는 **커널 내부의 핵심 파일 관리 구조체**

리눅스/유닉스 계열 파일 시스템의 핵심 구성 요소입니다.

### 아이노드가 관리하는 대표 정보

- 파일 타입
- 권한 (permission)
- UID / GID
- 파일 크기
- 생성 / 수정 시간
- 링크 수 (link count)
- 데이터 블록 위치
- 장치 번호 (device number)

### 중요한 점

**파일 이름(filename)은 inode에 저장되지 않습니다.**

파일 이름은 **디렉터리 엔트리(directory entry)** 가 관리합니다.

### 핵심 관계

```
filename → inode number → inode object → data block
```

---

## 2. 시스템 어디에서 등장하는가

파일 시스템 거의 **모든 동작**에서 등장합니다.

| 대상 | 설명 |
|------|------|
| **일반 파일** | text file, binary, database file, log file 모두 inode 존재 |
| **디렉터리** | 디렉터리도 inode를 가짐 |
| **장치 파일** | `/dev/nvme0n1`, `/dev/sda`, `/dev/null` 모두 inode 기반 |
| **네트워크 파일 시스템** | NFS, CephFS, GlusterFS도 inode 기반 메타데이터 구조 사용 |
| **컨테이너 filesystem** | overlayfs, ext4, xfs 모두 inode 사용 |

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 큰 영향: **Disk + Memory**

| 자원 | 영향 | 설명 |
|------|------|------|
| **Disk** | 매우 큼 | metadata I/O, directory lookup, file open에 직접 영향 |
| **Memory** | 매우 큼 | inode 많이 열리면 slab memory·dentry cache 증가 |
| **CPU** | 증가 가능 | 수백만 파일 탐색, recursive scan 시 CPU 증가 |

> **핵심:** 파일 open 성능 상당수는 **inode lookup 비용**의 영향을 받습니다.

---

## 4. 왜 중요한가

파일 시스템 핵심 메타데이터 구조이기 때문입니다.

**이유 1. 파일 식별의 기준**
커널은 filename보다 inode 중심으로 파일을 관리합니다.

**이유 2. 실제 데이터 위치 관리**
inode가 data block 위치를 기억합니다.

**이유 3. hard link 가능 이유**
여러 filename이 동일 inode를 참조할 수 있습니다.

**이유 4. 권한·보안 관리**
permission enforcement의 핵심입니다.

**이유 5. filesystem consistency 핵심**
inode 손상 시 filesystem 전체 문제로 이어질 수 있습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### inode exhaustion — 매우 흔함

증상: `No space left on device` 오류인데 실제 disk 용량은 남아 있음.

원인: inode 부족

발생 사례:
- 작은 로그 파일 수백만 개
- cache file 폭증
- container tmp file 누수

### Metadata bottleneck

파일 수가 매우 많으면 open latency 증가, directory lookup 저하.

### Filesystem corruption

inode metadata 손상 시 mount failure, orphan inode 발생 → `fsck` 필요.

### Kubernetes

| 증상 | 원인 |
|------|------|
| container log 폭증 | inode exhaustion |
| overlayfs 문제 | metadata lookup 비용 증가 |

> **핵심:** 대규모 시스템에서는 데이터 크기보다 **inode 개수가 먼저 병목**이 되는 경우가 많습니다.

---

## 6. 핵심 메커니즘

### 1. inode number
파일마다 고유한 inode 번호 존재. (`ls -i` 로 확인)

### 2. directory entry lookup
디렉터리는 **filename → inode number** 매핑을 관리합니다.

### 3. inode cache
커널이 메모리에 inode cache를 유지합니다. filesystem 성능의 핵심입니다.

### 4. inode → data block mapping
inode 내부 포인터가 direct block, indirect block 등으로 실제 데이터 위치를 가리킵니다.

### 5. special file handling
장치 파일 inode는 **major/minor number**를 가지며 드라이버 연결에 사용됩니다.

### 6. VFS abstraction
모든 filesystem이 VFS inode 구조로 통합됩니다. ext4, xfs, btrfs 모두 공통 inode 인터페이스를 사용합니다.

### 전체 흐름

```
filename
    → directory entry
    → inode number
    → inode object
    → filesystem metadata
    → data block lookup
    → storage I/O
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

| 목적 | 명령어 |
|------|--------|
| inode 번호 확인 | `ls -i` |
| inode 사용량 | `df -i` |
| 파일 metadata 확인 | `stat file.txt` |
| inode cache 상태 | `slabtop` |
| open file 확인 | `lsof` |
| filesystem 타입 확인 | `mount`, `lsblk -f` |

### Kubernetes

| 목적 | 방법 |
|------|------|
| node inode 상태 | `kubectl describe node` (DiskPressure 관련 확인) |
| container filesystem 확인 | `df -i` |
| overlayfs inode 증가 | container/image layer 많을 때 주의 필요 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*