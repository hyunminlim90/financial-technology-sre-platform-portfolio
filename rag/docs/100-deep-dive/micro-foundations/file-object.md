# 파일 객체 (File Object)

> 정독: 0회

## 1. 이 기술이 무엇인가

파일 객체(File Object)는:

> 열린(opened) 파일의 실행 중 상태(runtime state)를 커널 메모리 안에서 관리하는 동적 커널 객체

Linux 커널에서는 대표적으로 `struct file` 구조체로 존재합니다.

**중요한 점:**

- 디스크에 저장된 정적 파일 자체가 아님
- `open()` 이후 생성되는 실행 인스턴스
- 프로세스별 접근 상태 관리
- read/write 연산 제어
- VFS 계층 핵심 객체

> **핵심:** inode는 파일의 정적 메타데이터, file object는 열린 파일의 동적 실행 상태입니다.

---

## 2. 시스템 어디에서 등장하는가

### open() 시스템 콜

```c
fd = open(...)
```

open 호출 시 kernel memory 내부에 file object가 생성됩니다.

### VFS 계층

모든 파일 I/O의 핵심으로, 아래 전부 file object 기반입니다:

- regular file
- block device
- character device
- socket
- pipe

### Process FD Table

프로세스의 `file descriptor → file object` 매핑 구조의 핵심입니다.

### 기타 등장 위치

| 위치 | 설명 |
|---|---|
| Network Socket | socket도 file object 사용 |
| Pipe / FIFO | IPC도 file object 기반 |
| Device Access | `/dev/nvme0n1`, `/dev/tty` |
| Container Runtime | stdout / overlayfs / unix socket / epoll fd |
| Kubernetes | node kernel 내부 I/O 전체 |

> **핵심:** Linux I/O 대부분은 결국 file object 기반입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

### Memory 영향 — 매우 큼

kernel object / reference counting / page cache linkage / fd table linkage 전부 메모리 기반입니다.

### Disk 영향 — 매우 중요

- read/write offset
- filesystem dispatch
- block I/O

### Network 영향 — socket file object 때문에 중요

### CPU 영향 — 높음

- syscall path
- locking
- VFS dispatch
- polling

> **핵심:** file object는 커널 I/O 실행 상태를 유지하는 핵심 메모리 객체입니다.

---

## 4. 왜 중요한가

### 상태(State) 유지

- current offset
- flags / mode
- async state

### 다형성(Polymorphism)

동일한 `read()` / `write()` 호출이 regular file / socket / device / pipe 모두 다르게 동작 가능합니다.

### VFS 추상화 핵심

상위 계층은 하부 객체 종류를 몰라도 됩니다.

### 공유 가능

fork 이후 동일 file object를 공유할 수 있습니다.

### page cache 연결

filesystem cache 연동의 핵심입니다.

### event/polling 기반

epoll / select / poll 전부 file object 기반입니다.

> **핵심:** file object는 Linux 전체 I/O 추상화의 중심 실행 객체입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 대표 장애 유형

| 장애 | 설명 |
|---|---|
| FD Leak | open 후 close 안 함 → `Too many open files` |
| Offset 공유 문제 | fork/thread 환경에서 unexpected file offset movement 발생 |
| File Lock 문제 | deadlock 가능 |
| Device Busy | file object reference 살아있음 |
| Unmount 실패 | 열린 file object 존재 → `target is busy` |
| Container 종료 안 됨 | fd reference 남아 있음 |
| Network 장애 | socket file object 누수 |
| Polling 폭주 | epoll/file descriptor 폭증 |

### Kubernetes 장애

| 장애 | 원인 |
|---|---|
| log fd leak | container runtime 문제 |
| volume unmount 실패 | open reference 존재 |

> **핵심:** 많은 Linux resource leak 문제는 실제로 **file object lifecycle 문제**입니다.

---

## 6. 핵심 메커니즘

**`FD → file object → inode/device`** 연결 구조가 핵심입니다.

### 핵심 객체 구분

| 객체 | 역할 | 포함 정보 |
|---|---|---|
| `inode` | 정적 메타데이터 | owner / permission / disk location |
| `file object` | 열린 상태 | current offset / open flags / operation dispatch |

### open() 흐름

```
1단계  process    open("/tmp/a")
2단계  VFS        inode lookup 수행
3단계  kernel     struct file 생성 (kernel memory)
4단계  fd table   fd 3 → struct file* 연결
```

### read() 흐름

```
1단계  process    read(fd, buf, size)
2단계  kernel     fd lookup → file object 획득
3단계  kernel     f_op dispatch: file->f_op->read(...)
```

**이 시점에 다형성 발생:**

| 파일 종류 | 호출 대상 |
|---|---|
| regular file | filesystem read 함수 |
| socket | network stack |
| block device | block driver |

### 핵심 필드

| 필드 | 역할 |
|---|---|
| `f_pos` | 현재 읽기/쓰기 오프셋 |
| `f_flags` | open 플래그 |
| `f_mode` | 접근 모드 |
| `f_op` | 가상 함수 테이블 (read / write / mmap / ioctl / poll / fsync) |
| `private_data` | driver별 추가 상태 |

### Reference Count

file object는 **참조 카운트 기반 lifecycle**으로 관리됩니다.

```
close() → reference count 감소 → 0이 되면 해제
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# process fd 확인
ls -l /proc/<pid>/fd

# 열린 파일 확인
lsof

# fd limit
ulimit -n

# kernel file usage
cat /proc/sys/fs/file-nr

# inode/file object 상태 (slab: file_cache / dentry / inode_cache)
slabtop
```

### Runtime

container runtime도 file object를 대량 사용합니다:

- unix socket
- overlayfs
- stdout pipe

### Kubernetes

| 컴포넌트 | file object 사용 |
|---|---|
| kubelet | 수많은 socket/file object |
| CSI | volume fd 유지 |
| container logs | stdout/stderr file object 기반 |
| network plugin | socket fd 기반 |

### Observability 도구

```bash
lsof      # 열린 파일 및 fd 확인
ss        # socket 상태
slabtop   # kernel slab 메모리
strace    # syscall 추적
perf      # 성능 분석
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*