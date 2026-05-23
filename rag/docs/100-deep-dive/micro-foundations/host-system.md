# 호스트 시스템 (Host System)

> 정독: 0회

## 1. 이 기술이 무엇인가

호스트 시스템은:

> 애플리케이션·런타임·운영체제가 실제로 실행되며, CPU·메모리·스토리지·네트워크 자원을 제공하는 **실제 컴퓨팅 실행 환경 전체**

### 포함 범위

호스트 시스템은 단순 하드웨어만 의미하지 않습니다. 일반적으로 다음 전체를 포함합니다.

- CPU
- DRAM
- Storage
- NIC (Network Interface Card)
- Firmware
- Kernel
- Device Driver
- Runtime Environment
- Process Scheduler
- Virtual Memory System

### 핵심 관점

> 모든 소프트웨어는 결국 **호스트 시스템의 자원 위에서 실행**됩니다.

### 실행 흐름

```
Application
→ Runtime
→ System Call
→ Kernel
→ Driver
→ Hardware Controller
→ Physical Device
```

---

## 2. 시스템 어디에서 등장하는가

모든 컴퓨팅 환경의 중심입니다.

**대표 환경:**

- Physical Server
- VM (Virtual Machine)
- Cloud Instance
- Kubernetes Node
- Embedded Device
- Hypervisor Host
- Edge System
- Developer Workstation

### 클라우드 환경

Cloud VM도 결국 물리 호스트 위에서 실행됩니다.

### Kubernetes

Pod 자체는 호스트가 아닙니다. Pod는 **호스트 OS와 커널을 공유**합니다.

즉, **Kubernetes Node**가 실제 Host System 역할을 수행합니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

정답은 **모든 물리 자원**입니다.

| 자원 | 수행 항목 |
|---|---|
| **CPU** | scheduling, interrupt handling, system call execution, runtime execution |
| **Memory** | page cache, heap, virtual memory, kernel memory 관리 |
| **Disk** | filesystem, block I/O, journaling, persistence |
| **Network** | packet processing, TCP/IP stack, socket management, NIC queue 처리 |

---

## 4. 왜 중요한가

모든 추상화의 **실제 실행 기반**이기 때문입니다.

| 이유 | 설명 |
|---|---|
| **1. 실제 연산 수행** | 모든 계산은 host CPU에서 수행 |
| **2. 실제 메모리 제공** | 모든 객체·프로세스·runtime state 저장 |
| **3. 실제 I/O 수행** | Disk / Network / device 트랜잭션 수행 |
| **4. 성능 한계 결정** | latency와 throughput 최종 결정 |
| **5. 장애 최종 발생 지점** | 실제 장애는 대부분 host resource에서 발생 |

---

## 5. 실제 장애와 어떤 관련이 있는가

### CPU Saturation

host CPU 고갈 시: latency 증가, scheduling delay, interrupt delay 발생.

### Memory Pressure

host memory 부족 시: OOMKill, swap pressure, page reclaim spike 발생.

### Disk Bottleneck

host storage 병목 시: fsync delay, page cache stall, queue congestion 발생.

### Network Saturation

NIC queue overflow 가능.

### Kernel-level Failure

```
kernel panic
driver crash
filesystem corruption
```

### Noisy Neighbor

공유 host 환경에서 resource contention 발생.

### Container Misunderstanding

container는 host kernel을 공유합니다. 따라서:

```
host issue
→ container issue
```

로 연결 가능.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

핵심 메커니즘은 **8개**입니다.

| # | 메커니즘 | 설명 |
|---|---|---|
| 1 | **Process Execution** | 프로세스 실행 환경 제공 |
| 2 | **Virtual Memory** | 가상 주소 → 물리 메모리 매핑 |
| 3 | **System Call Interface** | application ↔ kernel 연결 |
| 4 | **Device Driver Coordination** | 하드웨어 제어 연결 |
| 5 | **Scheduler** | CPU time slice 분배 |
| 6 | **I/O Processing** | Disk / Network / device 처리 |
| 7 | **Interrupt Handling** | 하드웨어 이벤트 처리 |
| 8 | **Resource Isolation** | 프로세스 / 컨테이너 격리 수행 |

### 핵심 흐름

```
Application
→ Runtime
→ Kernel
→ Driver
→ Controller
→ Hardware
→ Physical Bit Transition
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### CPU 상태

```bash
top
htop
mpstat
pidstat
```

### Memory 상태

```bash
free -h
vmstat
slabtop
```

### Disk 상태

```bash
iostat
iotop
lsblk
blktrace
```

### Network 상태

```bash
ss
ethtool
iftop
sar -n DEV
```

### Kernel 상태

```bash
dmesg
journalctl -k
```

### Device 상태

```bash
lspci
lsusb
udevadm
```

### Kubernetes

host node 확인:

```bash
kubectl get nodes
```

pod가 어느 host에서 실행되는지:

```bash
kubectl get pod -o wide
```

node resource pressure:

```bash
kubectl top node
```

container runtime 확인:

```bash
crictl info
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*