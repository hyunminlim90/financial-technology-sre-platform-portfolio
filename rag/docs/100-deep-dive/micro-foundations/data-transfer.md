# Data Transfer (데이터 전송)
## **Micro Foundations — 범용 컴퓨터 구조 / 시스템 관점**

> 정독: 0회

## 1. 이 기술이 무엇인가

**Data Transfer**는:

> 시스템 내부에서 데이터 비트가 한 컴포넌트에서 다른 컴포넌트로 **실제로 이동하는 과정**

이다.

핵심은 **"연산"이 아니라 "이동"**이다.

CPU는 데이터를 읽고 → 옮기고 → 저장하고 → 다시 전달하는 과정을 끊임없이 반복한다.

**예시 — 모두 Data Transfer다:**

```
RAM → Register
Register → ALU
ALU → Register
Register → Cache
Cache → Memory
```

> **중요:** CPU는 대부분의 시간을 **연산보다 데이터 이동에 더 많이 사용**한다. 현대 시스템 성능 병목의 핵심 중 하나다.

---

## 2. 시스템 어디에서 등장하는가

Data Transfer는 **시스템 전체**에 등장한다.

### CPU 내부
- Register 이동, ALU 입력, pipeline stage 이동, cache line 전달

### Memory 계층
- RAM ↔ Cache, Cache ↔ CPU, DMA transfer

### Storage 계층
- SSD → RAM, page cache 이동

### Network 계층
- NIC buffer ↔ Kernel buffer, Kernel ↔ User space

> **결론:** 컴퓨터 시스템 전체는 **거대한 Data Transfer 구조**다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Memory**이다. Data Transfer의 핵심 비용이 **데이터 이동 자체**이기 때문이다.

| 자원 | 영향 |
|------|------|
| **CPU** | register access, cache access, pipeline movement, bus arbitration |
| **Memory** | 이동 비용 자체 — 가장 핵심 자원 |
| **Network** | 패킷 처리 = buffer 간 데이터 이동 |
| **Disk** | Storage I/O = SSD/HDD → RAM 데이터 이동 |

> **핵심:** Data Transfer는 **모든 컴퓨팅 자원을 연결하는 혈류 시스템**이다.

---

## 4. 왜 중요한가

현대 컴퓨터 성능 문제 대부분은 **연산 부족이 아니라 데이터 이동 비용 문제**다.

CPU 자체 연산은 매우 빠르다. 문제는 **"데이터를 가져오는 속도"**이다.

다음은 모두 **Data Transfer 병목**이다:

- cache miss
- NUMA remote access
- disk I/O
- network copy
- memory bandwidth saturation

> **결론:** 데이터 이동이 느리면 **ALU는 놀게 된다.** 현대 시스템 설계 핵심은 연산 최적화보다 **데이터 이동 최소화**인 경우가 많다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 1) Memory Bottleneck
```
메모리 대역폭 포화
  ↓
CPU stall → IPC 감소
```

### 2) Cache Miss Storm
```
필요 데이터가 cache에 없음
  ↓
RAM fetch 대기 폭증
```

### 3) Bus Contention
```
여러 컴포넌트가 동일 버스 점유 경쟁
  ↓
latency spike → throughput 감소
```

### 4) NUMA Penalty
```
다른 CPU socket memory 접근
  ↓
메모리 전송 지연 급증
```

### 5) Disk I/O Saturation
```
스토리지 queue 포화
  ↓
application blocking → timeout 증가
```

### 6) Excessive Copy Overhead
```
Kernel ↔ User copy 반복 (불필요한 메모리 복사)
  ↓
CPU 사용량 급증 → latency 증가
```

### 7) Network Buffer Overflow
```
NIC/Ring buffer 포화
  ↓
packet drop → retransmission
```

> **핵심:** 대규모 시스템 장애 상당수는 **Data Transfer 병목**에서 시작된다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 핵심 개념

| 메커니즘 | 의미 |
|----------|------|
| **Bus** | 데이터 이동 통로 |
| **Register Transfer** | CPU 내부 초고속 이동 |
| **Memory Load/Store** | RAM ↔ CPU 이동 |
| **DMA (Direct Memory Access)** | CPU 개입 없이 장치 ↔ RAM 직접 이동 |
| **Cache Line Transfer** | 보통 64-byte 단위 이동 |
| **Memory Bandwidth** | 초당 이동 가능한 데이터 양 |
| **Buffering** | 충격 완화용 임시 저장 공간 |
| **Zero-Copy** | 불필요한 메모리 복사 제거 — 현대 고성능 시스템 핵심 |
| **Clock Synchronization** | 전송은 클록 타이밍 기준으로 수행 |

### Latency vs Throughput

| 지표 | 의미 |
|------|------|
| **Latency** | 한 번 이동 시간 |
| **Throughput** | 초당 총 이동량 |

> **핵심:** Data Transfer는 **데이터를 얼마나 빠르고 충돌 없이 이동시키는가**의 문제다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**CPU Performance Counter**
```bash
perf stat
```
관찰: cache-misses, bus-cycles, stalled-cycles

**Memory Bandwidth**
```bash
vmstat
sar
numastat
```

**Disk I/O**
```bash
iostat
iotop
```

**Network Transfer**
```bash
iftop
ethtool
ss
sar -n DEV
```

**DMA / Interrupt**
```bash
cat /proc/interrupts
```

---

### Runtime

관찰 포인트:
- latency 증가
- bandwidth 포화
- copy overhead

---

### Kubernetes

| 관찰 영역 | 명령어 | 관찰 |
|-----------|--------|------|
| **CPU/Memory** | `kubectl top pod` / `kubectl top node` | CPU throttling, memory pressure |
| **Network** | 네트워크 메트릭 | network saturation |
| **Storage** | `kubectl describe pod` | storage latency |
| **eBPF** | eBPF 도구 | 고성능 transfer path 추적 |

> **핵심:** 운영 환경에서는 **Latency·Bandwidth·Copy Overhead** 형태로 드러난다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*