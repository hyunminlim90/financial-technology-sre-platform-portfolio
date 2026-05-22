# 운영체제 커널 계층 (Operating System Kernel Layer)

> 정독: 0회

## 1. 이 기술이 무엇인가

**운영체제 커널 계층(Kernel Layer)** 은:

> CPU, Memory, Disk, Network 같은 물리 자원을 직접 제어하고 관리하는 운영체제의 핵심 실행 계층

### 핵심 역할

커널은 하드웨어 제어, 프로세스 실행, 메모리 보호, 파일 시스템 관리, 네트워크 처리, 디바이스 제어를 담당합니다.

커널은 **최고 권한(privileged mode)으로 실행되며 하드웨어 접근 권한을 독점**합니다.

### 상위 계층과의 관계

```
Application
  → System Call
    → Kernel
      → Hardware
```

상위 소프트웨어는 하드웨어를 직접 제어하지 않으며, **모든 실제 자원 제어는 커널이 수행**합니다.

---

## 2. 시스템 어디에서 등장하는가

커널은 시스템 전체의 중앙에 존재합니다.

- **CPU 관리**: process scheduling, thread execution, interrupt handling, context switching
- **메모리 관리**: virtual memory, page table, page cache, memory protection
- **스토리지 관리**: filesystem, block layer, I/O scheduler, device driver
- **네트워크 관리**: TCP/IP stack, socket layer, routing, packet processing
- **디바이스 제어**: NIC, SSD, GPU, USB, keyboard/mouse — 모두 커널 드라이버를 통해 제어

### Kubernetes 환경

Container/Kubernetes도 결국 Linux kernel 기능 위에서 동작합니다.

예: cgroup, namespace, overlay filesystem, iptables, eBPF

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

커널은 모든 시스템 자원에 직접 영향을 줍니다.

- **CPU**: scheduler, interrupt, syscall, context switch
- **Memory**: page allocation, swap, cache, NUMA
- **Network**: socket buffer, packet queue, TCP retransmission, NIC interrupt
- **Disk**: filesystem, block cache, I/O queue, flush/sync

> 핵심: 커널은 **시스템 전체 성능과 안정성의 핵심 병목 지점**이 될 수 있습니다.

---

## 4. 왜 중요한가

커널은 **모든 하드웨어 접근의 유일한 통제 계층**이기 때문에 중요합니다.

커널이 없다면 애플리케이션이 메모리 충돌, 디스크 직접 접근, CPU 독점, 장치 오작동을 일으켜 시스템 전체가 불안정해집니다.

### 커널의 핵심 가치

| 기능 | 목적 |
|---|---|
| 자원 보호 | 프로세스 격리 |
| 자원 분배 | CPU/Memory 공정 사용 |
| 추상화 | 하드웨어 복잡성 은닉 |
| 안정성 | 오류 격리 |
| 보안 | 권한 통제 |

### SRE 관점 중요성

> 운영 장애 상당수는 애플리케이션 문제가 아니라 **커널 레벨 병목/자원 문제**인 경우가 많습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

커널 문제는 시스템 전체 장애로 이어질 수 있습니다.

### 대표 장애 유형

**CPU Soft Lockup** — 커널 스케줄러 정지

**OOM Killer** — 메모리 부족 시 커널이 프로세스 강제 종료

**Disk I/O Stall** — block layer 또는 filesystem 병목

**Kernel Panic** — 커널 자체 오류로 시스템 중단

**Network Packet Drop** — socket buffer overflow 또는 NIC queue 병목

**Interrupt Storm** — NIC/storage interrupt 과다 발생

**cgroup Misconfiguration** — container 자원 제한 오류

### Kubernetes 환경

| 증상 | 실제 커널 원인 |
|---|---|
| Pod eviction | memory pressure |
| node freeze | IO wait 증가 |
| high latency | scheduler contention |
| packet loss | kernel network queue overflow |
| container crash | OOM killer |

> **애플리케이션 장애처럼 보이는 문제 상당수가 실제로는 커널 자원 관리 문제**입니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심 메커니즘은 **User Space 요청을 Kernel Space가 실제 하드웨어 작업으로 변환**하는 구조입니다.

### 기본 흐름

```
Application
  → System Call
    → Kernel
      → Driver
        → Hardware
```

### 파일 읽기 예시

```
read()
  → VFS
    → filesystem
      → block layer
        → NVMe driver
          → SSD
```

### 네트워크 전송 예시

```
send()
  → socket layer
    → TCP/IP stack
      → NIC driver
        → NIC hardware
```

### 메모리 할당 예시

```
malloc()
  → virtual memory subsystem
    → page allocation
      → physical memory mapping
```

커널은 **논리 요청을 실제 하드웨어 동작으로 번역하는 실행 계층**입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 프로세스
ps
top
htop

# 메모리
free -h
vmstat
cat /proc/meminfo

# CPU 스케줄링
uptime
mpstat
pidstat

# 디스크
iostat -x
iotop

# 네트워크
ss -tulnp
sar -n DEV
ethtool

# 커널 로그
dmesg
journalctl -k

# 인터럽트
cat /proc/interrupts
```

### Runtime 관측 포인트

- syscall latency
- context switch
- CPU steal
- page fault
- IO wait
- socket backlog
- kernel memory pressure

### Kubernetes

```bash
# 노드 상태
kubectl describe node
kubectl top node

# cgroup 확인
cat /sys/fs/cgroup/*

# container runtime
crictl ps
crictl stats
```

### 중요한 관점

> Kubernetes는 커널 기능(namespace/cgroup/network stack)을 **오케스트레이션하는 상위 제어 계층**입니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*