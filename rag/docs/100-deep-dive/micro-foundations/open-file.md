# 오픈 파일 (Open File / 활성 파일)

> 정독: 0회

## 1. 이 기술이 무엇인가

**오픈 파일(Open File)** 은:

> 프로세스가 `open()` 시스템 콜을 수행한 뒤, 커널 메모리 내부에 생성되는 **실행 중인 파일의 런타임 상태 객체**

리눅스/유닉스 커널에서는 일반적으로 `struct file` 객체로 표현됩니다.

### 핵심 구분

| 구분 | 성격 | 주요 정보 |
|------|------|-----------|
| **inode** | 정적 메타데이터 | 파일 권한, 파일 크기, 데이터 블록 위치 |
| **open file (struct file)** | 실행 중 상태 | 현재 읽는 위치(offset), open flags, access mode, runtime I/O state |

> **핵심:** inode = 파일 자체 정보 / open file = 현재 열려 있는 실행 상태

---

## 2. 시스템 어디에서 등장하는가

거의 **모든 I/O 경로**에서 등장합니다.

| 대상 | 예시 |
|------|------|
| **일반 파일** | `open("/var/log/app.log")` |
| **장치 파일** | `open("/dev/sda")`, `open("/dev/null")` |
| **네트워크 소켓** | 소켓도 내부적으로 file abstraction 사용 |
| **Pipe / FIFO** | `pipe()` 역시 open file 기반 |
| **Terminal** | `/dev/tty` |
| **컨테이너 내부** | stdout/stderr, volume, socket, pipe 모두 open file 상태 사용 |
| **Database / Web Server** | 고성능 서버는 수만~수십만 open file 사용 가능 |

---

## 3. 어떤 자원에 가장 영향이 큰가

주요 영향: **Kernel Memory + File Descriptor Resource**

| 자원 | 영향 | 설명 |
|------|------|------|
| **Memory** | 매우 큼 | open file마다 struct file, fd table, dentry, inode reference 유지 필요 |
| **CPU** | 있음 | syscall 처리, fd lookup, VFS dispatch 발생 |
| **Disk** | 있음 | 실제 file I/O 발생 시 storage 접근 |
| **Network** | 있음 | socket fd 증가 시 network I/O 증가 |

> **핵심:** open file은 커널이 유지하는 **실시간 I/O 상태 객체**입니다.

---

## 4. 왜 중요한가

운영체제 **I/O 모델의 핵심**입니다.

**이유 1. 상태 유지**
offset 관리로 `read()` 반복 호출 시 어디까지 읽었는지 유지 가능.

**이유 2. 권한 관리**
각 open instance별 read only, write only, append, non-blocking 독립 유지 가능.

**이유 3. 동시성 지원**
같은 inode를 여러 프로세스가 동시에 open 가능.

**이유 4. VFS dispatch 핵심**
driver/file system 연결 지점 역할.

**이유 5. 리소스 관리 핵심**
운영체제는 **열린 파일 수**를 매우 중요하게 관리합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### Too many open files — 대표적 장애

증상: `EMFILE: Too many open files`

원인: fd leak, socket leak, close 누락

### fd exhaustion

프로세스 fd limit 초과. (`ulimit -n` 으로 확인)

### 삭제된 파일이 공간 차지

파일을 삭제했는데 disk 용량이 줄어들지 않는 현상.

원인: 다른 프로세스가 해당 파일을 open 상태로 유지 중.

### 기타 장애

| 장애 | 설명 |
|------|------|
| **stale file handle** | NFS/distributed FS에서 발생 가능 |
| **container fd leak** | container runtime 내부 fd leak 발생 가능 |
| **log rotation 문제** | process가 old file descriptor를 계속 유지 |

> **핵심:** open file 상태는 실제 **런타임 리소스 소비와 직접 연결**됩니다.

---

## 6. 핵심 메커니즘

### 1. open() 호출
`fd = open(...)` 발생 시 inode lookup, permission check, struct file 생성 수행.

### 2. file descriptor 생성
프로세스 fd table에 등록.

| fd | 용도 |
|----|------|
| 0 | stdin |
| 1 | stdout |
| 2 | stderr |
| 3+ | 일반 fd |

### 3. struct file 생성
커널 메모리 내부에 runtime object 생성.

### 4. inode reference 연결
`struct file → inode 참조` 연결 유지.

### 5. file offset 유지
`read(fd, ...)` 호출할 때마다 offset이 이동합니다.

### 6. file_operations 연결
`f_op` 를 통해 filesystem, device driver, socket layer로 dispatch.

### 7. reference counting
여러 fd가 동일 open file을 공유 가능. (예: `dup()`, `fork()`)

### 8. close()
reference count가 0이 되면 struct file 해제 및 resource 반환.

### 구조 흐름

```
Process
    → File Descriptor Table
    → struct file (open file)
    → inode
    → filesystem / driver
    → hardware
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

| 목적 | 명령어 |
|------|--------|
| 열린 파일 확인 | `lsof` |
| 프로세스 fd 확인 | `ls -l /proc/<pid>/fd` |
| fd limit 확인 | `ulimit -n` |
| system-wide limit | `cat /proc/sys/fs/file-max` |
| open file 사용량 | `cat /proc/sys/fs/file-nr` |
| 삭제된 열린 파일 확인 | `lsof \| grep deleted` |

### Kubernetes

| 목적 | 방법 |
|------|------|
| container fd leak | `kubectl exec` 후 `lsof` 실행 |
| log rotation 문제 | container stdout/stderr fd 유지 문제 확인 |
| sidecar/socket leak | service mesh 환경에서 fd 증가 모니터링 |

### systemd

service별 `LimitNOFILE` 설정이 중요합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*