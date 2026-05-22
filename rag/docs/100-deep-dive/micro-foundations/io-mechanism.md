# I/O 메커니즘 (I/O Mechanism)

> 정독: 0회

## 1. 이 기술이 무엇인가

I/O 메커니즘(I/O Mechanism)은:

> CPU·메모리와 하드웨어 장치 사이에서 데이터를 이동시키기 위해 운영체제 커널·디바이스 드라이버·하드웨어 컨트롤러가 협력하여 수행하는 **입출력 처리 체계**

단순 데이터 이동이 아니라 다음을 포함한 전체 I/O 실행 구조입니다:
- 장치 제어
- 데이터 전송
- 동기화
- 버퍼링
- 인터럽트 처리
- DMA 제어
- 큐 관리

**핵심 구성 계층:**

```
Application
  → System Call
    → Kernel I/O Subsystem
      → Device Driver
        → DMA / Interrupt
          → Hardware Controller
            → Physical Device
```

---

## 2. 시스템 어디에서 등장하는가

모든 컴퓨터 시스템에서 등장합니다.

| 영역 | 구성 요소 |
|---|---|
| **Storage I/O** | SSD, HDD, NVMe, Filesystem, Database flush |
| **Network I/O** | TCP/IP, socket, NIC packet RX/TX |
| **Input Device** | keyboard, mouse, USB device |
| **GPU I/O** | framebuffer, DMA transfer |
| **Cloud / Kubernetes** | container log write, CSI volume attach, overlayfs, network packet processing |
| **Database** | WAL write, fsync, async disk flush |

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

I/O 종류마다 다르지만 일반적으로 **CPU + Memory + Disk/Network** 영향이 가장 큽니다.

| 자원 | 영향 내용 |
|---|---|
| **CPU** | syscall 처리, interrupt 처리, context switch, queue scheduling, protocol stack 처리 |
| **Memory** | page cache, socket buffer, DMA buffer, ring buffer (버퍼링으로 인한 큰 영향) |
| **Disk** | 스토리지 I/O 성능 직접 결정 |
| **Network** | 패킷 처리량 결정 |

> **핵심:** I/O 메커니즘은 CPU·Memory·Device 사이의 데이터 흐름 제어 시스템입니다.

---

## 4. 왜 중요한가

현대 시스템 성능 병목 대부분이 I/O에서 발생합니다.

**속도 차이 문제:**

| 대상 | 속도 |
|---|---|
| CPU | 나노초 (ns) 단위 |
| SSD | μs ~ ms |
| HDD | ms |
| Network RTT | μs ~ ms |

**따라서 핵심 문제:** 빠른 CPU 세계와 느린 물리 장치 세계를 어떻게 연결할 것인가

**I/O 메커니즘이 해결하는 것:**
- latency hiding
- buffering
- batching
- asynchronous processing
- interrupt coordination
- DMA offloading

> **핵심:** I/O 메커니즘은 시스템 전체 성능과 안정성을 결정하는 핵심 실행 구조입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

**대표 장애 유형:**

| 장애 유형 | 증상 |
|---|---|
| **Disk I/O saturation** | iowait 급증 |
| **interrupt storm** | NIC/SSD interrupt 폭증 |
| **queue congestion** | I/O queue 적체 |
| **page cache thrashing** | 메모리 압박 발생 |
| **DMA timeout** | 장치 응답 불능 |
| **fsync latency** | DB transaction stall |
| **packet drop** | network RX/TX overflow |

**Kubernetes 장애:**

| 증상 | 원인 |
|---|---|
| node freeze | disk queue stall |
| container startup delay | overlayfs I/O 문제 |
| etcd latency | control plane 전체 영향 |

> **핵심:** I/O 메커니즘 문제는 애플리케이션이 아니라 시스템 전체 장애로 확대될 수 있습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심 메커니즘은 6단계입니다.

**1. System Call**
유저 프로세스가 `read()` / `write()` / `send()` / `recv()` 호출 → 커널 진입 발생

**2. Buffering**
커널 메모리에 임시 저장 — page cache, socket buffer

**3. Queueing**
I/O 요청 큐 적재 — block queue, NIC ring queue

**4. DMA**
장치 ↔ 메모리 직접 전송. CPU 개입 최소화

**5. Interrupt**
장치 완료 시 CPU 통보

**6. Scheduling / Merging**
커널이 요청 최적화 — request merging, reordering, batching

**전체 흐름:**
```
User Process
  → System Call
    → Kernel I/O Subsystem
      → Buffer/Page Cache
        → Queue
          → Driver
            → DMA
              → Physical Device
                → Interrupt Completion
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# I/O 상태
iostat -x 1

# block queue
cat /sys/block/*/queue/*

# interrupt
cat /proc/interrupts

# softirq
cat /proc/softirqs

# process I/O
iotop
pidstat -d

# syscall tracing
strace

# kernel tracing
perf
blktrace
bcc/eBPF

# memory cache
free -h
vmstat
```

### Runtime

```bash
# container I/O
docker stats
crictl stats

# overlayfs
mount | grep overlay
```

### Kubernetes

```bash
# node condition
kubectl describe node

# kubelet log
journalctl -u kubelet
```

**CSI volume attach** — CSI 관련 I/O 확인의 핵심 관측 대상입니다.

**CNI network packet** — CNI packet path 확인에 활용합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*