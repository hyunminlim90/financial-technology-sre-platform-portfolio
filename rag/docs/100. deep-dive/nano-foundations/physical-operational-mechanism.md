# 물리적 동작 메커니즘 (Physical Operational Mechanism)

> 정독: 0회

## 1. 이 기술이 무엇인가

**물리적 동작 메커니즘(Physical Operational Mechanism)** 은:

> 상위 계층의 논리 명령을 실제 하드웨어 물리 법칙과 전기적 동작으로 실행하는 최하단 실행 메커니즘

### 핵심 관점

상위 계층은 `read`, `write`, `send`, `allocate` 같은 논리 명령만 사용하지만, 실제 하드웨어 내부에서는 전압 변화, 전자 이동, 자성 변화, 버스 신호 전송, 회로 스위칭, DMA 이동 같은 물리 동작이 발생합니다.

물리적 동작 메커니즘은 **논리 세계를 실제 물리 세계에 구현하는 과정**입니다.

### 대표 사례

| 영역 | 물리적 동작 |
|---|---|
| SSD | 전자 포획/소거 |
| HDD | 자기장 방향 변경 |
| DRAM | capacitor charge refresh |
| NIC | electrical signal transmission |
| CPU | transistor switching |
| GPU | parallel arithmetic switching |

---

## 2. 시스템 어디에서 등장하는가

물리적 동작 메커니즘은 모든 하드웨어 계층의 최하단에 존재합니다.

### 스토리지

상위 명령 `4KB write` 수신 시 실제 내부에서 수행하는 것들:

- FTL mapping
- NAND page allocation
- voltage pump activation
- electron trapping
- ECC generation

### 네트워크

상위 명령 `send(packet)` 수신 시 실제 내부에서 수행하는 것들:

- DMA transfer
- NIC queue scheduling
- interrupt signaling
- PHY electrical encoding

### CPU

상위 명령 `instruction execution` 수신 시 실제 내부에서 수행하는 것들:

- transistor switching
- clock synchronization
- micro-op dispatch
- cache line movement

### 메모리

상위 명령 `memory allocation` 수신 시 실제 내부에서 수행하는 것들:

- DRAM row activation
- capacitor charge sensing
- refresh cycle

### Kubernetes / Cloud

직접 보이지는 않지만, 최종적으로는 **모든 추상화가 결국 물리 하드웨어 동작으로 귀결**됩니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

물리적 동작 메커니즘은 모든 하드웨어 자원 자체에 직접 영향을 미칩니다.

- **Disk / Storage**: 물리 매체 특성이 시스템 성능과 수명에 직접 영향 (NAND wear, erase cycle, garbage collection, seek latency)
- **CPU**: instruction pipeline, cache hierarchy, branch prediction, clock timing
- **Memory**: latency, refresh overhead, NUMA locality
- **Network**: PHY encoding, NIC queue, DMA throughput, interrupt coalescing

> 핵심: 물리적 동작 메커니즘은 **실제 성능·전력·발열·수명·안정성**을 결정합니다.

---

## 4. 왜 중요한가

상위 시스템은 아무리 우아해도 **최종적으로는 물리 법칙 위에서만 동작 가능**합니다.

중요한 이유는 실제 성능 결정, 하드웨어 수명 결정, 장애 발생 원인 형성, 전력 소비 결정, 열(thermal) 발생, IO latency 형성입니다.

**예시**: 논리적으로 빠른 SSD라도 실제 내부에서 write amplification, NAND GC, thermal throttling이 발생하면 상위 IO latency가 급증할 수 있습니다.

### Physical Constraints

소프트웨어는 물리 한계를 절대 초월할 수 없습니다.

- memory bandwidth limit
- PCIe lane limit
- NAND endurance
- DRAM latency wall
- network propagation delay

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 대규모 장애 상당수는 **최하단 물리 동작 메커니즘 이상**에서 시작됩니다.

### 대표 사례

**SSD Garbage Collection Stall**
- 내부 GC 발생 시 write latency spike 발생
- 결과: DB timeout, API latency, pod eviction 가능

**NAND Wear-out**
- P/E cycle 한계 도달 시 ECC 증가, read retry 증가, IO error 발생

**DRAM Error**
- bit flip, row hammer, ECC correction

**NIC 문제**
- packet drop, DMA stall, RX queue overflow

**CPU Thermal 문제**
- thermal throttling으로 throughput 감소, scheduling delay, tail latency 증가

### SRE 핵심

SRE는 상위 증상 뒤의 **최하단 물리 원인까지 추적**해야 합니다.

> 상위에서는 API timeout처럼 보이지만 실제 원인은 SSD GC, NUMA imbalance, NIC interrupt storm일 수 있습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심 메커니즘은 **논리 명령을 실제 물리 상태 변화로 변환**하는 것입니다.

### 스토리지 흐름 예시

```
write(file)
  → filesystem
    → block layer
      → SSD controller
        → FTL mapping
          → NAND program operation
            → electron trapping
```

### Address Translation

논리 주소(LBA)와 실제 물리 위치(physical NAND page)는 다릅니다.

```
Logical Address != Physical Location
```

SSD는 wear leveling, garbage collection, bad block avoidance를 위해 계속 주소 재배치를 수행합니다.

### ECC (Error Correction Code)

물리 비트 오류가 발생할 수 있으므로 하드웨어가 실시간 오류 복구를 수행합니다.

### DMA (Direct Memory Access)

CPU 직접 개입을 최소화하기 위해 디바이스 ↔ 메모리 직접 전송을 수행합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 디스크 상태
iostat
nvme smart-log
smartctl

# 블록 계층
lsblk
blktrace
fio

# 메모리 물리 상태
dmidecode
numactl -H

# CPU 물리 상태
lscpu
turbostat

# 네트워크 물리 상태
ethtool
sar -n DEV
```

### Runtime

관측 대상: IO wait, syscall latency, page fault, DMA overhead

### Kubernetes

직접 드러나진 않지만 아래 현상으로 관측됩니다.

- node pressure
- disk throttling
- network drop
- storage latency spike

### 주요 관측 지표

| 영역 | 지표 |
|---|---|
| SSD | latency, wear level |
| CPU | thermal, throttling |
| NIC | queue drop |
| DRAM | ECC error |
| IO | await, util |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*