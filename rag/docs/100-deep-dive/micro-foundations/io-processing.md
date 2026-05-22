# 입출력 처리 (I/O Processing)

> 정독: 0회

## 1. 이 기술이 무엇인가

입출력 처리(I/O Processing)는:

> CPU·메모리·커널과 스토리지·네트워크·입력장치 같은 외부 장치 사이의 **데이터 이동과 동기화를 관리하는 커널 중심 처리 체계**

**핵심 역할:**
- 데이터 이동
- 요청 큐 관리
- 버퍼링
- 인터럽트 처리
- DMA 제어
- 동기화
- 스케줄링

**I/O 처리는 단순 복사가 아닙니다.** 실제로는 다음까지 포함합니다:
- 속도 차이 조정
- 병렬 처리 조정
- 큐 관리
- 메모리 보호
- 장치 상태 관리
- 오류 복구

> **현대 시스템 핵심:** 현대 시스템 병목 상당수는 CPU 연산보다 I/O 처리 지연에서 발생합니다. SSD saturation, network congestion, queue stall, fsync latency, interrupt storm 같은 현상은 전부 I/O Processing 영역입니다.

---

## 2. 시스템 어디에서 등장하는가

사실상 시스템 전체에 걸쳐 있습니다.

| 영역 | 구성 요소 |
|---|---|
| **Storage Stack** | filesystem, page cache, block layer, NVMe queue, SSD firmware |
| **Network Stack** | socket buffer, TCP queue, NIC ring buffer, packet scheduler |
| **Runtime** | file read/write, container overlayfs, image pull, logging pipeline |
| **Kubernetes** | CSI volume attach, container log write, etcd disk sync, network packet flow |
| **Database** | WAL write, checkpoint flush, buffer pool eviction |

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Disk + Network + Memory**입니다.

**Disk 영향** (가장 대표적)
- block queue, flush, fsync, journal, SSD latency 전부 I/O processing 내부 동작

**Network 영향**
- packet queue, retransmission, interrupt moderation, NIC ring exhaustion과 직접 연결

**Memory 영향** (I/O buffering 핵심)
- page cache, socket buffer, DMA buffer

**CPU 영향**
- I/O 자체보다 interrupt handling, syscall, softirq, polling에 사용

> **핵심:** I/O processing은 CPU보다 Disk/Network/Memory coordination 영향이 더 큽니다.

---

## 4. 왜 중요한가

현대 시스템 성능과 안정성의 핵심입니다.

**대규모 시스템 장애 상당수는 연산 부족보다 I/O 병목에서 시작됩니다.**

| 영역 | 대표 장애 |
|---|---|
| **Storage** | disk queue full, writeback congestion, fsync storm |
| **Network** | RX/TX queue saturation, packet loss, retransmission explosion |
| **Distributed System** | etcd latency spike, kafka disk flush delay, database checkpoint stall |

I/O 문제가 발생하면 latency 증가 → timeout 증가 → retry 폭증 → cascading failure로 이어집니다.

> **핵심:** 고성능 시스템은 결국 I/O 처리 최적화 문제로 수렴하는 경우가 많습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

**I/O Wait 증가**
`top` / `vmstat` / `iostat` 에서 관측. CPU는 idle인데 시스템이 느린 증상.
원인: disk latency, queue congestion, fsync stall

**database 전체 멈춤처럼 보임**
실제 원인: storage flush 지연, dirty page explosion
→ dirty page 누적 → writeback throttle → system stall

**interrupt storm**
NIC/SSD interrupt 과다 발생 → CPU softirq saturation

**queue saturation**
NVMe queue 또는 network ring queue overflow

**Kubernetes 장애:**

| 증상 | 원인 |
|---|---|
| etcd latency 증가 | control plane 전체 불안정 |
| container log I/O 폭증 | node disk saturation |
| CSI attach timeout | storage I/O delay |

> **핵심:** I/O 장애는 단순 저장장치 문제가 아니라 시스템 전체 latency cascade로 확대됩니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심 메커니즘은 6단계입니다.

**1. System Call**
프로세스가 `read()` / `write()` / `send()` / `recv()` 호출

**2. Page Cache / Buffer**
커널 메모리에 임시 저장

**3. Queueing**
요청 큐 적재 — block queue, NVMe submission queue, NIC ring buffer

**4. Scheduling / Merging**
커널이 요청을 병합, 재정렬, 우선순위 조정

**5. DMA**
장치 ↔ 메모리 직접 복사. CPU 개입 최소화

**6. Interrupt Completion**
완료 시 interrupt / softirq / polling 통해 완료 통보

**전체 흐름:**
```
User Process
  → System Call
    → Kernel Buffer/Page Cache
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
# I/O wait 확인
top
vmstat 1

# block I/O (핵심 지표: await / svctm / util / queue depth)
iostat -x 1

# block layer
lsblk
blktrace

# page cache
free -h
cat /proc/meminfo

# dirty page
cat /proc/vmstat

# interrupt
cat /proc/interrupts

# softirq
cat /proc/softirqs

# network queue
ethtool -S eth0
ss -tuln
sar -n DEV 1
```

### Runtime

```bash
# container overlayfs I/O
iotop

# container log write
ls /var/log/containers
```

### Kubernetes

```bash
# node I/O pressure
kubectl describe node

# volume attach/mount 상태
kubectl describe pod
```

**etcd disk latency** — control plane 핵심 관측 대상입니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*