# 입출력 추상화 (I/O Abstraction)

> 정독: 0회

## 1. 이 기술이 무엇인가

입출력 추상화(I/O Abstraction)는:

> 서로 다른 하드웨어 장치들의 복잡한 물리적 차이를 운영체제가 숨기고, 상위에는 통일된 I/O 인터페이스만 제공하는 구조

대표적으로 운영체제는 다음과 같이 서로 완전히 다른 장치들을:

- SSD
- HDD
- NIC (Network Interface Card)
- Terminal
- Pipe
- Socket

아래의 동일한 인터페이스로 접근 가능하게 만듭니다:

- `open()`
- `read()`
- `write()`
- `close()`

> **핵심:** 상위 소프트웨어는 "무슨 장치인지"보다 "동일한 방식으로 어떻게 읽고 쓸 것인가"에만 집중합니다.

---

## 2. 시스템 어디에서 등장하는가

운영체제 거의 모든 I/O 경계면에서 등장합니다.

| 위치 | 설명 |
|---|---|
| **VFS (Virtual File System)** | 핵심 위치. 모든 장치를 file-like object로 추상화 |
| **Device Driver Layer** | 장치별 실제 구현 숨김 |
| **Network Stack** | socket abstraction 제공 |
| **Storage Stack** | block abstraction 제공 |
| **Terminal / Pipe** | stream abstraction 제공 |
| **Container Runtime** | container stdout/stderr도 동일한 I/O abstraction 사용 |

**Kubernetes 대표 예시:**
- container logs
- exec stream
- CSI socket
- kubelet CRI communication
- network socket
- overlayfs I/O

> **핵심:** 운영체제 전체는 I/O abstraction 위에 구축되어 있습니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

모든 자원과 연결되며, 특히 **Disk + Network + Memory** 영향이 매우 큽니다.

**Disk 영향**
- filesystem
- page cache
- block I/O
- storage queue

**Network 영향**
- socket
- TCP/IP stack
- packet queue
- NIC driver

**Memory 영향** (매우 중요)
- page cache
- kernel buffer
- DMA buffer
- socket buffer

→ 전부 abstraction layer 내부에서 관리됩니다.

**CPU 영향** (높음)
- syscall
- interrupt
- context switch
- polling
- queue management

> **핵심:** I/O abstraction은 커널 자원 관리 전체와 직접 연결됩니다.

---

## 4. 왜 중요한가

운영체제 확장성과 유지보수의 핵심이기 때문입니다.

**하드웨어 독립성 확보**
상위 애플리케이션 수정 없이 HDD → SSD, SATA → NVMe, 물리 NIC 교체가 가능합니다.

**표준 인터페이스 제공**
모든 장치를 동일 API로 접근할 수 있습니다.

**커널 구조 단순화**
VFS가 device-specific 코드를 몰라도 됩니다.

**드라이버 교체 가능**
device driver만 변경할 수 있습니다.

**다형성 제공**
동일한 `read()` / `write()` 호출이 file, socket, pipe, block device에 대해 서로 다르게 동작합니다.

**보안 및 권한 통제 가능**
커널이 모든 I/O 경계를 통제합니다.

> **핵심:** I/O abstraction은 운영체제의 하드웨어 독립성을 보장하는 핵심 구조입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실무에서 매우 중요합니다.

**Driver Bug**
I/O abstraction 아래 계층 오류. 대표 증상: kernel panic, storage timeout, NIC reset

**Page Cache Stall**
상위는 단순 `write()` 호출했지만 실제로는 dirty page flush 지연 발생 가능

**Filesystem Hang**
block layer congestion

**Socket Backpressure**
network abstraction 내부 병목

**Device Queue Saturation**
NVMe queue overflow

**Kubernetes 장애 사례**

- *container log 폭주*: stdout stream → runtime → filesystem 병목
- *CSI storage timeout*: storage abstraction layer stall
- *overlayfs latency*: filesystem abstraction overhead

**중요한 실무 포인트**

애플리케이션은 단순히 `write(fd)`만 호출했더라도, 실제 아래에서는 다음이 전부 관여합니다:

```
VFS → page cache → scheduler → block layer → driver → controller → storage firmware
```

> **핵심:** I/O abstraction은 단순 API가 아니라 커널 전체 I/O pipeline의 통제 구조입니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

**핵심 흐름:**

```
User Process
  → System Call
    → VFS
      → File Object
        → I/O Abstraction
          → Device-specific Operation
            → Driver
              → Hardware
```

**핵심 1: File Descriptor 기반 추상화**
process는 `fd = open(...)` 만 사용합니다.

**핵심 2: VFS 다형성**
동일한 `read(fd)` 호출이 대상별로 내부 분기됩니다:
- regular file
- socket
- pipe
- block device

**핵심 3: `file_operations`**
Linux 핵심 구조인 `struct file_operations` 내부 함수 포인터:
- `read`
- `write`
- `mmap`
- `ioctl`
- `poll`

**핵심 4: stream ↔ block 변환**
상위는 byte stream을 사용하고, 하위 storage는 logical block을 사용합니다. 커널이 중간 변환을 수행합니다.

**핵심 5: Device Driver Binding**
device마다 다른 실제 구현이 연결됩니다.

> **핵심:** I/O abstraction의 핵심은 "공통 인터페이스 + 내부 동적 분기"입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 장치 확인
ls /dev

# block device 확인
lsblk

# 열린 file object 확인
lsof

# driver 확인
lspci -k

# syscall 추적
strace

# block layer 관측
iostat
iotop
blktrace
```

### Runtime

container runtime 내부에서 동작하는 I/O abstraction layer:
- stdout pipe
- overlayfs
- unix socket

### Kubernetes

| 인터페이스 | 역할 |
|---|---|
| **CSI** (Container Storage Interface) | storage abstraction |
| **CNI** (Container Network Interface) | network abstraction |
| **CRI** (Container Runtime Interface) | runtime abstraction |

**Observability 도구:**
- `perf`
- `bcc/eBPF`
- `ftrace`
- `sar`
- `pidstat`

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*