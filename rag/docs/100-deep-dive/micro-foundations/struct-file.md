# 파일 구조체 객체 (`struct file`)

> 정독: 0회

## 1. 이 기술이 무엇인가

`struct file`은:

> 리눅스/유닉스 커널 내부에서 **현재 열린(opened) 파일의 실시간 런타임 상태를 관리하는 커널 객체**

### 핵심 특징

`struct file`은 다음을 담당합니다.

- `open()` 시 생성
- `close()` 시 제거
- 커널 메모리에 존재
- 프로세스별 상태 유지
- 열린 파일의 동적 상태 관리

### 중요한 구분

| 구분 | 성격 | 주요 정보 |
|------|------|-----------|
| **`struct inode`** | 정적 메타데이터 | 권한, 파일 크기, 소유자, 디스크 블록 위치 |
| **`struct file`** | 실행 중 상태 (Runtime State) | 현재 offset, open mode, non-blocking 여부, 현재 read/write 상태 |

> **핵심:** inode는 "파일 자체" / struct file은 "현재 열린 실행 상태"

---

## 2. 시스템 어디에서 등장하는가

**VFS(Virtual File System) 핵심**에 존재합니다.

| 대상 | 설명 |
|------|------|
| **일반 파일** | `open("/tmp/a.txt")` 호출 시 생성 |
| **장치 파일** | `open("/dev/nvme0n1")` 시 생성 |
| **socket** | 네트워크 소켓도 내부적으로 file abstraction 사용 |
| **pipe** | pipe/fifo 역시 file object 기반 |
| **epoll** | fd 기반 이벤트 시스템도 연결 |
| **container runtime** | container filesystem 접근 시 사용 |
| **Kubernetes** | overlayfs, container log, volume mount 등에서 커널 내부 사용 |

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 영향 큰 자원: **Memory + I/O synchronization**

| 자원 | 영향 | 설명 |
|------|------|------|
| **Memory** | 매우 큼 | `struct file` 객체가 kernel heap, slab allocator 내부에 존재 |
| **CPU** | 있음 | fd lookup 시 syscall path, lock handling, reference counting 발생 |
| **Disk** | 있음 | 실제 block I/O와 연결 |
| **Network** | 있음 | socket file object 존재 |

---

## 4. 왜 중요한가

운영체제 **입출력의 핵심**입니다.

**이유 1. 열린 상태 유지**
현재 어디 읽는 중인지, 어떤 모드인지, 비동기인지 관리 가능.

**이유 2. 동시 접근 분리**
같은 파일을 여러 프로세스가 **독립 상태로 사용** 가능하게 만듦.

**이유 3. VFS 핵심 추상화**
모든 장치를 file interface로 통합.

**이유 4. driver 연결 핵심**
`f_op`을 통해 실제 driver 함수와 연결.

**이유 5. lifecycle 관리**
reference counting 기반 자원 해제 수행.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 주요 장애 유형

| 장애 | 설명 |
|------|------|
| **FD Leak** | `Too many open files` 발생 — 대표적 장애 |
| **Reference Count Bug** | close 안 되면 객체 해제 실패 |
| **File Offset Corruption** | 동시 접근 시 offset 문제 가능 |
| **Blocking 상태 문제** | non-blocking 설정 오류 시 hang, latency spike 발생 |
| **Driver Crash** | 잘못된 `f_op` 연결 시 kernel crash 가능 |
| **Stale File Handle** | NFS/Distributed FS 환경에서 자주 발생 |
| **container log fd leak** | Kubernetes 환경에서 발생 가능 |

> **핵심:** 운영체제의 많은 I/O 장애는 **열린 파일 상태 관리 실패**와 연결됩니다.

---

## 6. 핵심 메커니즘

### 1. `open()`
호출 시 `struct file` 생성.

### 2. File Descriptor 연결
프로세스 fd table이 `fd → struct file` 매핑.

### 3. inode 연결
`struct file`은 `struct inode`를 참조.

### 4. Offset 유지
`f_pos` 에 현재 위치 저장.

### 5. Mode / Flag 유지
`f_flags`, `f_mode` 관리.

### 6. Driver Function 연결
`f_op`이 핵심. 실제 `f_op->read`, `f_op->write`, `f_op->ioctl` 호출됨.

### 7. Reference Counting
`f_count`로 생명주기 관리.

### 8. `close()`
reference count가 0이 되면 객체 제거.

### 핵심 흐름

```
open()
    → struct file 생성
    → fd table 연결
    → runtime state 유지
    → I/O 수행
    → close()
    → reference count 감소
    → 객체 제거
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

| 목적 | 명령어 |
|------|--------|
| 열린 파일 조회 | `lsof` |
| 프로세스 fd 확인 | `ls -l /proc/<pid>/fd` |
| fd 제한 확인 | `ulimit -n` |
| system-wide file 상태 | `cat /proc/sys/fs/file-nr` |
| socket 포함 조회 | `ss -anp` |
| slab object 상태 | `slabtop` |
| syscall 추적 | `strace` |

### Kubernetes

| 목적 | 방법 |
|------|------|
| container fd 상태 | `crictl inspect` |
| node fd exhaustion | `Too many open files` 형태로 자주 발생 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*