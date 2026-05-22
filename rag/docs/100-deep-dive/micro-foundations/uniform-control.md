# 일관된 제어 (Uniform Control)

> 정독: 0회

## 1. 이 기술이 무엇인가

일관된 제어(Uniform Control)는:

> 서로 다른 하드웨어와 I/O 대상을 운영체제가 **동일한 인터페이스와 규칙으로 제어하는 구조**

> **핵심:** "장치 종류와 관계없이 동일한 방식으로 접근 가능하게 만든다"

**대표 예시** — 아래 장치들이 모두 동일한 시스템 콜로 접근 가능합니다:

| 장치 | 접근 인터페이스 |
|---|---|
| SSD, HDD | `open()` / `read()` / `write()` / `close()` |
| Socket, Pipe | `open()` / `read()` / `write()` / `close()` |
| Terminal, GPU device file, NIC | `open()` / `read()` / `write()` / `close()` |

> 일관된 제어는 **하드웨어 종류별 제어 차이를 커널 내부로 격리하는 구조**입니다.

---

## 2. 시스템 어디에서 등장하는가

운영체제 전체 I/O 구조에서 등장하며, 핵심 위치는 **VFS (Virtual File System)**입니다.

**전체 흐름:**
```
Application
  → System Call
    → VFS
      → File Object
        → file_operations
          → Driver
            → Physical Device
```

**등장 위치:**

| 영역 | 구성 요소 |
|---|---|
| **Storage** | block device, filesystem, block layer |
| **Network** | socket, TCP/IP stack, NIC driver |
| **IPC** | pipe, FIFO, unix domain socket |
| **Device Access** | `/dev/*`, character device, block device |

**Kubernetes** 내부도 동일 철학 적용:

| 인터페이스 | 역할 |
|---|---|
| **CRI** | Container Runtime Interface |
| **CSI** | Container Storage Interface |
| **CNI** | Container Network Interface |

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

모든 I/O 자원에 영향을 미치며, CPU + Memory + Disk + Network 전부 직접 연결됩니다.

**CPU 영향**
- syscall 처리
- interrupt
- queue scheduling
- polling
- context switch

**Memory 영향**
- page cache
- kernel buffer
- socket buffer
- DMA memory

**Disk 영향**
- block I/O
- storage queue
- flush
- filesystem sync

**Network 영향**
- packet queue
- TCP buffer
- NIC ring buffer

> **핵심:** 일관된 제어는 커널 전체 I/O 자원 관리 구조와 직접 연결됩니다.

---

## 4. 왜 중요한가

운영체제 확장성과 유지보수의 핵심입니다.

**하드웨어 독립성**
애플리케이션 수정 없이 HDD → SSD, SATA → NVMe, physical NIC → virtual NIC 교체가 가능합니다.

**개발 단순화**
개발자는 `read(fd)` / `write(fd)` 만 사용하면 되고, 장치별 명령을 몰라도 됩니다.

**드라이버 교체 가능**
하위 구현만 변경할 수 있습니다.

**다형성 제공**
동일한 시스템 콜이 file, socket, pipe, block device에서 서로 다르게 동작합니다.

**운영체제 구조 단순화**
커널 코어가 device-specific 코드를 몰라도 됩니다.

> **핵심:** 일관된 제어는 운영체제 전체의 인터페이스 표준화 구조입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

> **중요:** 상위에서는 동일 API로 보이지만, 하위 장애 원인은 완전히 다를 수 있습니다.

**`write()` latency 증가 시 원인 후보:**
- page cache flush stall
- NVMe queue congestion
- filesystem lock
- disk firmware timeout

**상위는 동일 read/write지만, 실제 네트워크 아래에서는:**
- NIC queue drop
- TCP retransmission
- interrupt storm

**container stdout hang 시 동일 stream interface 아래에서:**
- pipe congestion
- runtime logging issue
- filesystem saturation

**block I/O error — 상위는 단순 file write 실패처럼 보이지만 실제는:**
- controller reset
- SSD media error
- PCIe link issue

**핵심 실무 포인트:**

```
Uniform Interface ≠ Uniform Behavior
```

상위 인터페이스는 동일하지만 latency, queue depth, buffering, ordering, durability는 장치별로 완전히 다릅니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

> **핵심:** 동일 인터페이스 호출을 커널 내부에서 device-specific 구현으로 **동적 분기**한다

**Linux 핵심 구조: `struct file_operations`**

```
read(fd)
  → struct file
    → f_op->read
      → device-specific implementation
```

**동일한 `write(fd)` 호출이지만 내부는 완전히 다릅니다:**

| 대상 | 내부 동작 |
|---|---|
| **Regular File** | page cache → filesystem → block I/O |
| **Socket** | socket buffer → TCP/IP stack → NIC queue |
| **Pipe** | kernel pipe buffer |
| **Block Device** | block layer → storage driver → controller queue |

**핵심 메커니즘 요약:**

| 구성 요소 | 역할 |
|---|---|
| **VFS** | 공통 인터페이스 제공 |
| **File Object** | 열린 객체 상태 유지 |
| **File Descriptor** | process별 접근 핸들 |
| **file_operations** | 동적 함수 분기 |
| **Device Driver** | 실제 하드웨어 제어 |

> **핵심:** 일관된 제어의 본질은 "공통 인터페이스 + 내부 동적 구현 분기"입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 열린 FD 확인
lsof

# device file 확인
ls /dev

# block device
lsblk

# syscall 추적
strace

# driver 확인
lspci -k

# I/O 관측
iostat
iotop
blktrace

# network 관측
ss
ethtool
ip -s link
```

### Runtime

container runtime 내부도 동일 구조로 동작합니다:
- stdout/stderr
- pipe
- socket
- overlayfs

→ 전부 uniform interface 기반

### Kubernetes

**대표 abstraction interface:**

| 인터페이스 | 설명 |
|---|---|
| **CRI** | Container Runtime Interface |
| **CSI** | Container Storage Interface |
| **CNI** | Container Network Interface |

**Observability 도구:**
- `perf`
- `ftrace`
- `bcc/eBPF`
- `sar`
- `pidstat`

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*