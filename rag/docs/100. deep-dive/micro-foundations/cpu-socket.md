# CPU Socket

> 정독: 0회

## 1. 무엇인가

CPU Socket은 메인보드(Mainboard) 위에서 CPU 패키지와 전기적으로 연결되는 물리 인터페이스이다.

**역할:**

- CPU 전원 공급
- 메모리 버스 연결
- PCIe / NVMe / NIC 연결
- 인터럽트 및 시스템 버스 전달
- CPU ↔ 메인보드 신호 전달

**대표 규격:**

| 규격 | 특징 |
|------|------|
| LGA (Land Grid Array) | 메인보드 쪽에 핀 존재 |
| PGA (Pin Grid Array) | CPU 쪽에 핀 존재 |
| BGA (Ball Grid Array) | 메인보드에 납땜 고정 |

<details>
<summary>Deep Dive</summary></br>

마더보드(Motherboard) [[M]](../../100-deep-dive/micro-foundations/motherboard.md)

</details></br>

## 2. 시스템 어디에서 등장하는가

CPU Socket은 Hardware Layer에서 등장하지만 실제 영향은 OS Scheduler, NUMA, Interrupt Routing, Memory Access까지 연결된다.

**관련 계층:**

```text
Hardware
 └── CPU Socket
      ├── Physical CPU
      ├── Memory Controller
      ├── PCIe Lane
      └── NUMA Node

OS Kernel
 ├── CPU Scheduler
 ├── IRQ Affinity
 ├── NUMA Scheduler
 └── Memory Locality

Runtime / JVM
 ├── GC Thread Placement
 ├── Thread Affinity
 └── NUMA-aware Allocation

Application
 └── Latency / Throughput 영향
```

---

## 3. 가장 영향이 큰 자원

CPU Socket은 주로 아래 자원에 직접 영향을 준다.

| 자원 | 영향 |
|------|------|
| CPU | Core 간 스케줄링 |
| Memory | NUMA Remote Access |
| Network | NIC IRQ Routing |
| PCIe | NVMe / NIC 대역폭 |

특히 **NUMA Memory Access 영향이 가장 중요**하다.

---

## 4. 왜 중요한가

멀티 소켓 서버에서는 메모리가 Socket 단위로 분리되어 연결된다.

```text
CPU Socket 0
 └── Local Memory Bank 0

CPU Socket 1
 └── Local Memory Bank 1
```

**문제 상황:**

```text
Socket 0의 Thread
 → Socket 1 Memory 접근
```

발생 시:

- Memory Latency 증가
- Cache Coherency Traffic 증가
- QPI / UPI Interconnect 사용 증가
- Tail Latency 증가

**핀테크 결제 시스템에서의 영향:**

- P99 응답시간 증가
- GC Pause 증가
- Event Loop Delay
- Throughput 감소

---

## 5. 실제 장애와의 관련성

**대표 장애 패턴:**

| 장애 | 원인 |
|------|------|
| CPU Usage 낮은데 Latency 증가 | NUMA Remote Access |
| 특정 Core만 과부하 | IRQ 집중 |
| GC Time 증가 | Remote Memory Allocation |
| Event Loop Delay | CPU Migration |
| SoftIRQ Saturation | NIC Queue가 특정 Socket에 집중 |

**자주 발생하는 연쇄 패턴:**

```text
NIC IRQ
 → Socket 0 집중
 → SoftIRQ 집중
 → 특정 NUMA Node 메모리 압박
```

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### NUMA (Non-Uniform Memory Access)

가장 핵심적인 메커니즘이다.

```text
Local Memory Access  ≠  Remote Memory Access
```

Remote 접근 시 Interconnect(QPI / UPI)를 거치며 Latency가 증가한다.

### CPU Affinity

특정 프로세스 / 스레드를 특정 CPU Socket / Core에 고정한다.

관련 명령어: `taskset`, `numactl`

### IRQ Affinity

NIC Interrupt를 특정 CPU Core / Socket에 분산 배치한다.

관련 파일: `/proc/interrupts`, `/proc/irq/*`

### Cache Coherency

멀티 소켓 환경에서는 CPU 간 Cache 동기화 비용이 발생한다. 특히 아래 상황에서 영향이 크다.

- Lock Contention
- False Sharing
- Shared Queue

---

## 7. Linux / Runtime / Kubernetes에서 어디서 관측되는가

### Linux

```bash
# CPU / NUMA 구성 확인
lscpu
numactl --hardware

# NUMA 메모리 상태
numastat

# IRQ 분포 확인
cat /proc/interrupts

# CPU Affinity 확인
taskset -cp <pid>
```

### JVM

NUMA-aware 할당 옵션:

```text
-XX:+UseNUMA
```

GC Thread 배치에도 NUMA 구성이 영향을 준다.

### Kubernetes

| 기능 | 목적 |
|------|------|
| CPU Manager Static Policy | CPU Pinning |
| Topology Manager | NUMA 정렬 |
| Guaranteed QoS | CPU Isolation |

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*