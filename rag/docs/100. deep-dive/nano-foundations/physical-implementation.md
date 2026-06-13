# 물리적 구현 (Physical Implementation)

> 정독: 0회

## 1. 이 기술이 무엇인가

물리적 구현(Physical Implementation)은:

> **논리적 인터페이스가 실제 하드웨어 위에서 어떻게 동작하는지를 구성하는 최하단 물리·기계·전자적 실체**

### 핵심 의미

상위 계층은 파일, 소켓, 메모리, 프로세스 같은 **논리 개념**만 보지만, 실제 내부에서는 전기 신호, NAND 셀 전하, 디스크 블록, DMA 전송, 버스 인터럽트 같은 **물리 메커니즘**이 작동합니다.

> 물리적 구현은 **추상화 뒤에 숨겨진 실제 동작 계층**입니다.

### 논리 계층 vs 물리 구현 예시

| 논리 계층 | 물리 구현 |
|-----------|-----------|
| 파일 | 블록 + SSD page |
| 메모리 | DRAM cell |
| 네트워크 socket | NIC queue + DMA |
| Virtual Machine | physical CPU scheduling |
| Container | kernel namespace |

### 핵심

사용자는 논리 구조를 사용하지만, **실제 시스템 성능과 장애는 물리적 구현에서 결정되는 경우가 매우 많습니다.**

<details>
<summary>Deep Dive</summary></br>

Logical Interface(논리 인터페이스) [[M]](../../100-deep-dive/micro-foundations/logical-interface.md)  
Hardware(하드웨어) [[M]](../../100-deep-dive/micro-foundations/hardware.md)  
Physical Operational Mechanism(물리적 동작 메커니즘) [[M]](../../100-deep-dive/micro-foundations/physical-operational-mechanism.md)  
Physical Architecture(물리적 아키텍처) [[M]](../../100-deep-dive/micro-foundations/physical-architecture.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

물리적 구현은 시스템 전체에 존재합니다.

### Storage
- **논리:** Filesystem → Block Device → SSD Controller → NAND Cell

### CPU
- **논리:** Thread
- **실제:** CPU core scheduling / cache line / pipeline execution

### Memory
- **논리:** Virtual Address
- **실제:** Physical DRAM row/column / page frame / memory bus

### Network
- **논리:** TCP connection
- **실제:** NIC queue / interrupt / DMA / packet buffer

### Kubernetes
- **논리:** Pod
- **실제:** Linux process / namespace / cgroup / veth / iptables

> 현대 시스템 대부분은 **논리 인터페이스와 물리 구현이 분리된 구조**입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

물리적 구현은 **모든 하드웨어 자원에 직접 영향**을 줍니다.

### CPU — 영향 매우 큼
- cache miss
- NUMA latency
- branch prediction
- pipeline stall

### Memory — 영향 매우 큼
- DRAM access latency
- page fault
- memory fragmentation
- TLB miss

### Network
- packet drop
- NIC queue overflow
- interrupt storm
- MTU fragmentation

### Disk — 가장 대표적
특히 SSD 내부 FTL, garbage collection, wear leveling 같은 구현이 실제 성능을 크게 좌우합니다.

> 논리 인터페이스는 같아도 **물리 구현 차이로 성능과 안정성이 극단적으로 달라질 수 있습니다.**

---

## 4. 왜 중요한가

> **실제 병목과 장애가 대부분 물리 구현에서 발생하기 때문입니다.**

### 예시: 단순 `write()` 내부 흐름

```
write()
  → page cache
  → filesystem journal
  → block scheduler
  → SSD GC
  → NAND erase
```

논리 인터페이스만 보면 왜 느린지 알 수 없습니다.

### SRE 관점 핵심

운영 안정성 분석에서는 **추상화 아래 실제 구현을 추적하는 능력**이 매우 중요합니다.

| 논리 문제 | 실제 원인 |
|-----------|-----------|
| API latency | SSD write stall |
| DB timeout | disk queue saturation |
| Pod freeze | memory reclaim |
| packet retransmission | NIC buffer overflow |

### 핵심 가치
- 실제 병목 분석 가능
- 하드웨어 한계 이해 가능
- 시스템 튜닝 가능
- 장애 원인 역추적 가능

---

## 5. 실제 장애와 어떤 관련이 있는가

물리 구현은 실제 장애와 직접 연결됩니다.

### Storage Stall
SSD garbage collection pause 발생 시:
- write latency 급증
- fsync timeout
- DB stall

### Fragmentation
블록 파편화 심화 시:
- random I/O 증가
- seek amplification
- read latency 증가

### NAND Wear Out
SSD 수명 한계 접근 시:
- ECC correction 증가
- write error 증가
- read retry 증가

### Network Physical Issue
- CRC error / packet corruption
- link flap / duplex mismatch

### Memory Physical Issue
- ECC error / page corruption
- NUMA imbalance

### Kubernetes 환경

| Kubernetes 현상 | 실제 물리 원인 |
|-----------------|----------------|
| Pod restart | disk timeout |
| API slow | node IO wait |
| Container freeze | memory pressure |
| Network instability | NIC saturation |

> **추상화 계층만 보면 장애 원인이 숨겨질 수 있습니다.**

---

## 6. 핵심 메커니즘

논리 요청이 최하단 물리 장치 제어로 변환되는 과정입니다.

### 예시 1: 파일 읽기

```
read("file.txt")
  → VFS lookup
  → inode traversal
  → extent lookup
  → block request
  → SSD controller
  → NAND page read
```

### 예시 2: 메모리 접근

```
pointer dereference
  → virtual address
  → page table walk
  → physical frame access
  → DRAM row activation
```

### 예시 3: 네트워크 송신

```
send()
  → socket buffer
  → TCP segmentation
  → NIC queue
  → DMA transfer
  → PHY transmission
```

> 상위 추상화는 복잡성을 숨기지만, **복잡성이 사라진 것은 아닙니다.**

---

## 7. Linux / Runtime / K8s에서 관측 방법

### Linux

```bash
# block device
lsblk
blkid

# disk latency
iostat -x

# low-level IO tracing
blktrace
fio

# memory physical stats
numactl --hardware
cat /proc/meminfo

# CPU physical behavior
perf stat
perf top
```

### Runtime 관측 대상
- page fault
- syscall latency
- IO wait
- context switch
- cache miss

### Kubernetes

```bash
kubectl top nodes
kubectl describe node
```

특히 다음 항목을 확인합니다:
- disk pressure
- memory pressure
- network saturation
- CPU throttling

### eBPF 기반 추적

`bcc-tools` / `bpftrace`를 활용한 주요 관측 항목:
- block IO latency
- syscall delay
- TCP retransmission
- scheduler delay

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*