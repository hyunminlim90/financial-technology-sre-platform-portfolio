# 논리적 파일 (Logical File)

> 정독: 0회

## 1. 이 기술이 무엇인가

논리적 파일(Logical File)은:

> 운영체제가 사용자와 애플리케이션에게 제공하는 **최상위 데이터 추상화 단위**

### 핵심 의미

실제 하드웨어 내부에서는 데이터가 블록 단위로 분산 저장되고, 여러 물리 위치에 조각화되며, SSD 내부에서 주소 변환이 수행되고 있지만, 운영체제는 이를 **하나의 연속된 파일 객체**처럼 보이게 만듭니다.

### 논리적 파일 구성

| 구성 요소 | 설명 |
|-----------|------|
| Metadata | 파일 관리 정보 |
| Data | 실제 파일 내용 |

```
Logical File = Metadata + Data
```

### 사용자 관점 예시

`report.pdf`, `image.png`, `video.mp4`, `database.db` 모두 논리적 파일입니다.

<details>
<summary>Deep Dive</summary></br>

Operating System(운영체제) [[M]](../../100-deep-dive/micro-foundations/operating-system.md)  
End User(엔드 유저) [[M]](../../100-deep-dive/micro-foundations/end-user.md)  
Application Software(애플리케이션 소프트웨어) [[M]](../../100-deep-dive/micro-foundations/application-software.md)  
User Level(사용자 계층) [[M]](../../100-deep-dive/micro-foundations/user-level.md)  
Data Abstraction(데이터 추상화) [[M]](../../100-deep-dive/micro-foundations/data-abstraction.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

논리적 파일은 운영체제 파일시스템 전체에서 등장합니다.

### 등장 위치

| 계층 | 역할 |
|------|------|
| Application | 파일 open/read/write |
| OS Kernel | VFS 처리 |
| Filesystem | inode/MFT 관리 |
| Block Layer | logical block I/O |
| SSD | physical storage |
| NAND Flash | 실제 bit 저장 |

### 실제 흐름

```
애플리케이션: open("data.txt")
       ↓
      VFS
       ↓
   Filesystem
       ↓
  inode lookup
       ↓
logical block mapping
       ↓
  SSD/NAND access
```

### 클라우드 환경

논리적 파일은 VM disk image, container layer, log file, database file, object cache 등 거의 모든 저장 계층에 존재합니다.

### Kubernetes에서 특히 중요한 영역

- PersistentVolume
- OverlayFS
- Container Runtime
- etcd snapshot

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **Disk / Filesystem I/O**이며, 실제로는 전체 스택과 연결됩니다.

| 자원 | 영향 |
|------|------|
| Disk | 핵심 |
| Memory | page cache / inode cache |
| CPU | metadata traversal |
| Network | distributed file sync |

> 논리적 파일 접근은 단순 read/write처럼 보여도 내부적으로 metadata lookup, block mapping, cache traversal, journal update 등 다단계를 수행합니다.

---

## 4. 왜 중요한가

논리적 파일이 없다면 사용자가 직접 디스크 블록 주소를 다뤄야 합니다.

논리적 파일은 **물리 저장 구조를 완전히 추상화**하여 다음을 가능하게 합니다.

- 파일 이름 기반 접근
- 디렉토리 구조
- 권한 관리
- random access
- large file handling
- filesystem abstraction

결과적으로 애플리케이션은 SSD 내부 NAND 구조를 전혀 몰라도 파일을 사용할 수 있습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

논리적 파일은 파일시스템 장애와 직접 연결됩니다.

### 대표 장애 유형

| 유형 | 설명 |
|------|------|
| Metadata Corruption | 파일은 존재하지만 위치 추적 불가 |
| Block Mapping Failure | 파일 일부 손상 |
| Journal Failure | filesystem recovery 반복 |
| Sparse File Issue | 실제 공간과 논리 공간 불일치 |
| File Fragmentation | random I/O 증가 |

### 실제 시스템 증상

| 증상 | 원인 |
|------|------|
| 파일 접근 실패 | inode 손상 |
| read latency 증가 | fragmentation |
| zero-byte file | incomplete flush |
| orphan file | metadata inconsistency |
| mount failure | filesystem corruption |

### 데이터베이스 환경에서 특히 중요한 파일

- WAL file
- checkpoint file
- SSTable
- segment file

### 컨테이너 환경

overlay layer metadata 증가 시 container startup latency 증가 가능.

---

## 6. 핵심 메커니즘

핵심 변환 구조:

```
파일 이름 → 메타데이터 → 논리 블록 → 물리 저장
```

### 실제 흐름

1. **파일 이름 탐색** — directory entry 조회
2. **inode/MFT 조회** — 파일 속성과 block map 확인
3. **logical block access** — LBA 요청
4. **SSD 내부 변환** — FTL이 논리 주소 → NAND 물리 주소 변환
5. **실제 데이터 읽기** — NAND page read 수행

> 논리적 파일은 연속된 파일처럼 보이지만, 실제로는 매우 복잡한 계층 구조 위에 존재합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux 명령어

```bash
# 파일 메타데이터 확인
stat file.txt

# inode 확인
ls -i

# block mapping 확인
filefrag

# filesystem 사용량
df -h

# 열려 있는 파일
lsof

# page cache 관측
free -h
cat /proc/meminfo
```

### Kubernetes 중요 영역

| 영역 | 설명 |
|------|------|
| Container layer | OverlayFS |
| PersistentVolume | DB/log 저장 |
| etcd snapshot | cluster state |
| CSI volume | storage abstraction |

### Runtime 환경에서 특히 중요한 항목

- log rotation
- checkpoint file
- WAL durability
- image layer unpacking

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*