# 커널 공간 (Kernel Space / 커널 영역)

> 정독: 0회

## 1. 이 기술이 무엇인가

**커널 공간(Kernel Space)** 은:

> 운영체제 커널과 핵심 시스템 코드만 접근 가능한 **특권 메모리 영역**

### 핵심 특징

커널 공간은 다음이 가능한 영역입니다.

- CPU 최고 권한 (Ring 0)
- 하드웨어 직접 제어
- 전체 메모리 접근
- 디바이스 제어
- 인터럽트 처리

### 유저 공간 vs 커널 공간

| 구분 | 설명 | 예시 |
|------|------|------|
| **유저 공간 (User Space)** | 일반 애플리케이션 실행 영역 | browser, database, terminal, game |
| **커널 공간 (Kernel Space)** | 운영체제 핵심 실행 영역 | scheduler, memory manager, filesystem, network stack, device driver |

> **핵심:** 커널 공간은 운영체제가 하드웨어를 직접 통제하는 **특권 실행 메모리 영역**입니다.

---

## 2. 시스템 어디에서 등장하는가

운영체제 핵심 **전체**에 등장합니다.

| 영역 | 주요 요소 |
|------|-----------|
| **프로세스 스케줄링** | scheduler, context switching |
| **메모리 관리** | page table, virtual memory, MMU control |
| **파일 시스템** | VFS, inode, page cache |
| **네트워크** | TCP/IP stack, socket layer |
| **블록 I/O** | request queue, block layer, bio structure |
| **디바이스 드라이버** | NVMe driver, NIC driver, USB driver |
| **인터럽트 처리** | ISR, softirq, IRQ handler |

### Kubernetes와의 관계

K8s 자체는 user space이지만, container runtime, cgroup, namespace, overlayfs 등은 커널 공간 기능을 사용합니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

모든 자원에 영향을 미치며, 특히 **CPU + Memory** 영향이 가장 핵심입니다.

| 자원 | 영향 | 설명 |
|------|------|------|
| **CPU** | 매우 큼 | 커널 모드 진입 시 privilege switch, syscall handling, interrupt handling 발생 |
| **Memory** | 매우 큼 | page cache, slab allocator, kernel object 등 유지 |
| **Network** | 큼 | TCP stack 대부분 커널 공간에서 동작 |
| **Disk** | 큼 | block I/O subsystem 존재 |

---

## 4. 왜 중요한가

운영체제 **전체의 핵심**입니다.

**이유 1. 하드웨어 보호**
유저 프로그램이 직접 장치를 제어하지 못하도록 차단합니다.

**이유 2. 시스템 안정성**
잘못된 프로그램이 RAM overwrite, disk corruption, device crash를 유발하지 못하게 보호합니다.

**이유 3. 자원 공유**
CPU·메모리·디스크를 여러 프로세스에 안전하게 분배합니다.

**이유 4. 추상화 제공**
하드웨어 차이를 숨기고 `read()`, `write()`, `socket()` 같은 공통 인터페이스를 제공합니다.

**이유 5. 보안 핵심**
권한 분리의 핵심 기반입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 주요 장애 유형

| 장애 | 설명 |
|------|------|
| **Kernel Panic** | 커널 공간 오류 시 시스템 전체 중단 가능 |
| **Driver Crash** | 잘못된 driver로 인한 memory corruption, deadlock, IRQ storm |
| **High System CPU** | `sy%`, `wa%`, `softirq` 증가로 커널 공간 과부하 발생 |
| **Interrupt Storm** | 인터럽트 과다 발생 시 CPU 마비 가능 |
| **Memory Leak** | kernel object 누수 시 slab growth, OOM 발생 |
| **Hung Task** | 커널 lock 경합 발생 |
| **Filesystem Corruption** | kernel I/O path 오류 시 발생 |

### Kubernetes

node freeze의 대부분은 커널 레벨 문제일 가능성이 있습니다.
- cgroup bug
- overlayfs issue
- NIC driver issue

---

## 6. 핵심 메커니즘

### 1. User Mode ↔ Kernel Mode 전환
시스템 콜 발생 시 **Ring 3 → Ring 0** 전환.

### 2. System Call
유저 공간이 커널 서비스를 요청하는 공식 경로. (`read()`, `write()`, `open()`, `mmap()`)

### 3. MMU 보호
MMU가 user page / kernel page 권한을 검사합니다.

### 4. Virtual Memory Mapping
모든 프로세스 주소 공간 상단에 **kernel mapping**이 공유 존재합니다.

### 5. Kernel Object 관리
커널 공간은 `inode`, `file object`, `socket`, `task_struct`를 관리합니다.

### 6. Interrupt Handling
하드웨어 이벤트를 처리합니다.

### 7. DMA 연동
장치 ↔ 메모리 직접 전송을 수행합니다.

### 8. Context Switching
프로세스 상태 전환을 수행합니다.

### 핵심 구조 흐름

```
User Space
    → System Call
    → Kernel Mode
    → Kernel Space Execution
    → Hardware Control
    → Return to User Space
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

| 목적 | 명령어 | 비고 |
|------|--------|------|
| CPU 사용률 | `top`, `htop` | `us%` = user / `sy%` = kernel |
| 시스템 콜 추적 | `strace` | |
| 인터럽트 상태 | `cat /proc/interrupts` | |
| 커널 로그 | `dmesg`, `journalctl -k` | |
| 메모리 상태 | `slabtop`, `cat /proc/meminfo` | |
| 커널 모듈 | `lsmod`, `modinfo` | |
| block I/O 상태 | `iostat`, `iotop` | |
| softirq 상태 | `cat /proc/softirqs` | |
| syscall 통계 | `perf`, `bcc/eBPF` | |

### Kubernetes

| 목적 | 명령어 |
|------|--------|
| cgroup 상태 | `/sys/fs/cgroup` |
| namespace 상태 | `lsns` |
| container runtime | `crictl`, `ctr` |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*