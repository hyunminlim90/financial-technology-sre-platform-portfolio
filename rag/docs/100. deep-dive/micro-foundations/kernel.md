# 커널 (Kernel)

> 정독: 0회

## 1. 이 기술이 무엇인가

**커널(Kernel)** 은:

> CPU·메모리·디스크·네트워크·입출력 장치 같은 하드웨어 자원을 직접 제어하고 관리하는 **운영체제(OS)의 핵심 실행부**

운영체제에서 가장 중요한 부분이며 다음을 담당합니다.

- 하드웨어 제어
- 프로세스 관리
- 메모리 관리
- 파일 시스템 관리
- 입출력 처리
- 네트워크 처리

### 핵심 위치

```
Application
     ↕ System Call
   Kernel
     ↕ Device Driver
  Hardware
```

### 핵심 특징

일반 프로그램은 하드웨어를 직접 제어할 수 없습니다. 반드시 **System Call → Kernel** 경로를 거쳐야 합니다.

---

## 2. 시스템 어디에서 등장하는가

커널은 시스템 전체에서 항상 동작합니다.

| 영역 | 주요 요소 |
|------|-----------|
| 프로세스 실행 | scheduler, context switch, process lifecycle |
| 메모리 관리 | virtual memory, page table, page cache, NUMA |
| 파일 시스템 | VFS, ext4, xfs, overlayfs |
| 네트워크 | TCP/IP stack, socket, packet routing |
| 스토리지 | block layer, I/O scheduler, NVMe driver |

### Kubernetes와의 관계

Kubernetes 자체도 결국 **Linux Kernel 위에서 실행**됩니다.

- `cgroup`
- `namespace`
- `overlayfs`
- `iptables`
- `eBPF`

위 항목들 모두 커널 기능입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

커널은 **시스템 전체 자원**에 영향을 줍니다.

### CPU — 매우 중요
- scheduler, interrupt, syscall, softirq

### Memory — 매우 중요
- page cache, slab allocator, virtual memory, reclaim

### Disk — 스토리지 I/O 직접 관리
- block layer, fsync, writeback

### Network — 패킷 처리 담당
- TCP stack, NIC queue, routing

> **핵심:** 커널은 시스템 전체 자원 관리의 중심입니다.

---

## 4. 왜 중요한가

커널 문제는 애플리케이션 문제가 아니라 **시스템 전체 문제**로 이어집니다.

### 커널이 중요한 이유

**하드웨어 독점 제어**
- 모든 장치 접근의 경유지

**시스템 안정성 결정**
- deadlock, panic, memory corruption, scheduler stall에 직접 영향

**성능 결정**
- I/O latency, network throughput, memory efficiency, CPU utilization 모두 커널 영향

**컨테이너·클라우드 기반**
- 현대 인프라 핵심 기능이 커널 기반 (cgroup, namespace, eBPF, io_uring)

> **핵심:** 커널은 운영체제의 기능 집합이 아니라 **시스템 전체 실행 기반**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

| 장애 유형 | 설명 |
|-----------|------|
| **Kernel Panic** | 커널 치명 오류. 시스템 즉시 중단. |
| **OOM Killer** | 메모리 부족 시 프로세스 강제 종료. |
| **Soft Lockup / Hard Lockup** | CPU hang 상태. |
| **I/O Stall** | block queue 정체. |
| **Network SoftIRQ Saturation** | 패킷 처리 병목. |
| **Filesystem Corruption** | metadata 손상. |
| **Scheduler Latency** | 프로세스 응답 지연. |

### Kubernetes Node Failure

실제 원인이 커널 문제인 경우가 많습니다.
- cgroup 문제
- kernel memory pressure
- network stack 문제

> **핵심:** 커널 문제는 **노드 전체 장애로 확산**됩니다.

---

## 6. 핵심 메커니즘

### 1. System Call
유저 프로그램 → 커널 진입. (예: `read()`, `write()`, `fork()`, `mmap()`)

### 2. Scheduler
CPU 분배 담당.

### 3. Virtual Memory
가상 주소 공간 관리.

### 4. VFS (Virtual File System)
모든 파일 시스템 통합 인터페이스.

### 5. Block I/O Subsystem
스토리지 입출력 관리.

### 6. Network Stack
TCP/IP 패킷 처리.

### 7. Device Driver
하드웨어 제어 담당.

### 전체 흐름

```
Application
    → System Call
    → Kernel Subsystem
    → Driver
    → Hardware
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

| 목적 | 명령어 |
|------|--------|
| 커널 버전 | `uname -a` |
| 커널 로그 | `dmesg`, `journalctl -k` |
| CPU scheduler 상태 | `top`, `htop`, `pidstat` |
| memory 상태 | `free -h`, `vmstat` |
| block I/O 상태 | `iostat`, `blktrace` |
| network stack 상태 | `ss`, `netstat`, `ethtool` |
| syscall tracing | `strace` |
| kernel tracing | `perf`, `ftrace`, `bcc/eBPF` |

### Runtime

컨테이너 격리(`cgroup`, `namespace`)는 커널 기능 기반.

### Kubernetes

- **node pressure:** `kubectl describe node`
- **kubelet interaction:** kubelet ↔ kernel API 사용
- **container runtime:** containerd / CRI-O 모두 커널 의존
- **CNI / CSI:** network/storage 모두 kernel path 사용

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*