# 커널 수준 입출력 (Kernel-level I/O)

> 정독: 0회

## 1. 이 기술이 무엇인가

커널 수준 입출력(Kernel-level I/O)은:

> 운영체제 커널이 CPU 특권 모드(Kernel Mode)에서 하드웨어 장치와 직접 통신하며 수행하는 **입출력 처리 메커니즘**

**핵심 역할:**
- 하드웨어 직접 제어
- 메모리 보호
- 시스템 안정성 유지
- 장치 접근 중재

**중요한 구조 — User Mode vs Kernel Mode:**

| 구분 | 설명 | 제한/가능 사항 |
|---|---|---|
| **User Mode** | 일반 애플리케이션 실행 영역 | MMIO 접근 불가, DMA 제어 불가, interrupt 제어 불가, device register 접근 불가 |
| **Kernel Mode** | 운영체제 핵심 실행 영역 | device register 접근, page table 수정, DMA 설정, interrupt 처리, scheduler 제어 |

> **핵심:** 실제 하드웨어 입출력은 반드시 커널을 통해서만 수행됩니다.

---

## 2. 시스템 어디에서 등장하는가

모든 시스템 I/O 경로에 등장합니다.

| 영역 | 구성 요소 |
|---|---|
| **Storage** | filesystem, block layer, NVMe driver, page cache, DMA engine |
| **Network** | socket syscall, TCP/IP stack, NIC driver, interrupt handling |
| **Process Runtime** | file read/write, stdout/stderr, IPC, pipe |
| **Kubernetes** | container runtime I/O, CSI storage attach, CNI packet flow, kubelet log write |
| **Database** | WAL flush, fsync, mmap page fault, async I/O |

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

직접 영향은 CPU + Memory + Disk + Network 전체에 걸치며, 핵심은 **CPU privilege transition**과 **Memory coordination**입니다.

| 자원 | 영향 내용 |
|---|---|
| **CPU** | syscall 진입, context switch, interrupt handling, softirq, polling 전부 CPU 관여 |
| **Memory** | page cache, DMA buffer, socket buffer, slab allocator (커널 공간 메모리) |
| **Disk** | storage I/O 실제 집행 |
| **Network** | packet RX/TX 처리 핵심 |

> **핵심:** Kernel-level I/O는 CPU와 Memory를 중심으로 Disk/Network I/O를 통제합니다.

---

## 4. 왜 중요한가

운영체제 안정성과 보안의 핵심입니다.

**이유 1. 하드웨어 보호**
유저 프로세스가 직접 장치에 접근하면 memory corruption, filesystem corruption, device deadlock이 발생할 수 있습니다.

**이유 2. 멀티프로세스 보호**
커널이 접근 권한, synchronization, isolation을 통제합니다.

**이유 3. 성능 최적화**
커널이 중앙에서 queue scheduling, caching, DMA, interrupt moderation을 수행할 수 있습니다.

**이유 4. 장치 추상화**
상위는 `read()` / `write()` / `send()` / `recv()` 만 사용하고, 실제 장치 차이는 커널 내부에서 해결합니다.

> **핵심:** Kernel-level I/O는 보안·안정성·성능 최적화를 위한 운영체제 핵심 제어 구조입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

**대표 장애 유형:**

| 장애 유형 | 증상 |
|---|---|
| **syscall storm** | 과도한 syscall 발생 → CPU kernel time 급증 |
| **interrupt storm** | NIC/SSD interrupt 폭증 → softirq saturation 발생 |
| **kernel lock contention** | I/O queue lock 경합 |
| **page cache pressure** | dirty page 과다 누적 |
| **DMA issue** | DMA timeout 또는 memory corruption |
| **driver bug** | kernel panic 가능 |
| **storage timeout** | `blk_update_request I/O error` |
| **network backlog overflow** | packet drop 증가 |

**Kubernetes 장애:**

| 증상 | 원인 |
|---|---|
| node NotReady | storage I/O stall |
| etcd fsync latency | control plane 불안정 |
| container runtime hang | overlayfs 또는 disk queue 문제 |

> **핵심:** Kernel-level I/O 문제는 애플리케이션 오류가 아니라 시스템 전체 장애로 확대되기 쉽습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심 메커니즘은 7단계입니다.

**1. System Call Transition**
유저 모드에서 `read()` / `write()` 호출 → CPU가 User Mode → Kernel Mode 전환

**2. File Descriptor Lookup**
커널이 `struct file`, `inode`, driver mapping 조회

**3. Page Cache / Buffer**
커널 메모리 buffer 사용

**4. Queueing**
요청 큐 적재 — block queue, socket queue

**5. Driver Dispatch**
driver의 `file_operations` 함수 호출

**6. DMA Transfer**
장치 ↔ 메모리 직접 복사

**7. Interrupt Completion**
장치 완료 후 interrupt 발생 → 커널이 완료 처리

**전체 흐름:**
```
User Process
  → System Call
    → Kernel Mode Transition
      → VFS/File Object
        → Driver Dispatch
          → DMA
            → Physical Device
              → Interrupt Completion
                → Return to User Mode
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# syscall 추적
strace

# kernel CPU usage (sy 증가 확인)
top
pidstat

# interrupt
cat /proc/interrupts

# softirq
cat /proc/softirqs

# block layer
iostat -x 1

# kernel logs
dmesg
journalctl -k

# device driver
lspci -k
lsmod

# DMA / IRQ
cat /proc/iomem
```

### Runtime

```bash
# container I/O
iotop

# overlayfs activity
mount | grep overlay
```

### Kubernetes

```bash
# node pressure
kubectl describe node

# kubelet log
journalctl -u kubelet
```

**CSI/CNI timeout** — storage/network I/O 문제 확인의 핵심 관측 대상입니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*