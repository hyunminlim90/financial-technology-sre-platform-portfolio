# 데이터 버퍼 (Data Buffer)

> 정독: 0회

## 1. 이 기술이 무엇인가

데이터 버퍼는:

> 데이터를 임시로 저장하는 메모리 공간

시스템 내부에서는 CPU / 디스크 / 네트워크 / 프로세스 / 장치(Device) 사이에서 데이터가 이동합니다. 

이때 처리 속도가 서로 다르기 때문에 중간 완충 공간(buffer)이 필요합니다.

| 역할 | 설명 |
|------|------|
| 속도 차이 완화 | 빠른 생산자와 느린 소비자 사이 조절 |
| 일시 저장 | 데이터 임시 보관 |
| 처리량 향상 | I/O 대기 감소 |
| 배치 처리 | 작은 요청을 묶음 처리 |
| 비동기화 | 생산/소비 분리 |

> 흐름 제어를 위한 메모리 기반 staging 공간

---

## 2. 시스템 어디에서 등장하는가

데이터 버퍼는 거의 모든 시스템 계층에 존재합니다.

| 계층 | 버퍼 예시 |
|------|-----------|
| CPU | store buffer, line fill buffer |
| OS Kernel | page cache, socket buffer |
| Disk I/O | block buffer |
| Network | RX/TX ring buffer |
| Runtime | stream buffer |
| IPC | pipe buffer |
| GPU | frame buffer |
| Filesystem | write-back cache |
| Device Driver | DMA buffer |

> 버퍼 없이는 고성능 처리 자체가 불가능

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**가장 직접 영향: Memory + I/O**

| 자원 | 영향 |
|------|------|
| Memory | 버퍼 저장 공간 |
| Disk | read/write buffering |
| Network | packet buffering |
| CPU Cache | prefetch/locality |
| DMA | device-memory transfer |

버퍼 설계 실패는 I/O bottleneck으로 직결됩니다.

---

## 4. 왜 중요한가

컴퓨터 시스템은 모든 구성요소의 속도가 서로 다릅니다.

| 구성요소 | 속도 |
|----------|------|
| CPU | ns |
| DRAM | 수십 ns |
| SSD | μs |
| Network | μs~ms |
| HDD | ms |

버퍼가 없으면 CPU가 디스크를 대기 / 네트워크가 애플리케이션을 대기 / 장치가 메모리를 대기하는 상태가 됩니다.

> 버퍼는 throughput stabilization 장치

---

## 5. 실제 장애와 어떤 관련이 있는가

| 문제 | 원인 |
|------|------|
| packet drop | NIC buffer overflow |
| OOM | unbounded buffering |
| latency spike | queue backlog |
| TCP retransmission | receive buffer saturation |
| disk stall | dirty page accumulation |
| backpressure cascade | consumer slowdown |
| stream lag | producer-consumer imbalance |

consumer 처리 속도 < producer 유입 속도인 경우:

```
버퍼 증가 → 메모리 압박 → GC 증가 → swap → OOM
```

> buffer sizing 실패 = 시스템 불안정성

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Producer-Consumer 구조

| 역할 | 의미 |
|------|------|
| Producer | 데이터 생성 |
| Buffer | 임시 저장 |
| Consumer | 데이터 처리 |

```
producer → buffer → consumer
```

예시:

| 생산 속도 | 소비 속도 |
|-----------|-----------|
| 100MB/s | 40MB/s |

버퍼가 없다면 producer blocking이 발생합니다. 버퍼가 있으면 일시적 burst를 흡수할 수 있습니다.

### Ring Buffer

| 구성요소 | 의미 |
|----------|------|
| head | write 위치 |
| tail | read 위치 |
| circular memory | 순환 구조 |

lock-free 가능 / cache friendly / 고속 네트워크·NIC에서 핵심으로 사용됩니다.

### Double Buffering

| 버퍼 | 역할 |
|------|------|
| buffer A | 처리 중 |
| buffer B | 채우는 중 |

병렬성 향상을 목적으로 합니다.

### Page Cache

운영체제 핵심 버퍼로, 디스크 I/O를 메모리 버퍼에 캐싱합니다.

결과: read latency 감소 / write batching / disk access 감소. Linux 성능의 핵심입니다.

### Socket Buffer

TCP/UDP는 send buffer / receive buffer를 사용합니다.

```
network throughput ↔ socket buffer size
```

- **너무 작으면:** packet loss / retransmission
- **너무 크면:** memory pressure / latency 증가

### Backpressure

consumer가 느려지면 버퍼가 upstream에 압력을 전달합니다. stream processing / async runtime / event loop / messaging system에서 필수적인 메커니즘입니다.

### DMA Buffer

장치가 CPU 개입 없이 `DMA buffer ↔ device` 전송을 수행합니다. 고속 네트워크/스토리지의 핵심입니다.

### Zero-Copy

memory copy를 최소화하는 버퍼 최적화 기법입니다.

예: `sendfile` / `mmap` / `splice` / `io_uring`

### Buffer Overflow

버퍼 크기 초과 시 corruption / overwrite / crash / exploit이 발생할 수 있으며, 보안과도 직접 연결됩니다.

### Cache Locality

버퍼는 보통 연속 메모리 블록으로 구성됩니다.

이유: sequential access / cache hit 증가 / prefetch 효율 향상

### Queueing Theory

버퍼는 결국 queue system입니다.

| 요소 | 의미 |
|------|------|
| arrival rate | 유입 속도 |
| service rate | 처리 속도 |
| queue length | backlog |
| wait time | latency |

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**대표 관측 도구:** `free` / `vmstat` / `iostat` / `sar` / `ss` / `netstat` / `ethtool` / `ip -s link`

```bash
# 네트워크 버퍼
ss -m

# 커널 버퍼
cat /proc/meminfo
```

| 항목 | 의미 |
|------|------|
| Buffers | block I/O buffer |
| Cached | page cache |

### Network

```bash
# NIC ring buffer
ethtool -g eth0

# socket buffer
sysctl net.core.rmem_max
sysctl net.core.wmem_max
```

### Disk I/O

```bash
# page cache 관측
vmstat
sar -B
```

dirty page 증가 시 writeback delay / I/O stall이 발생할 수 있습니다.

### Runtime

| 영역 | 버퍼 사용 |
|------|-----------|
| stream | read/write buffer |
| async queue | event buffering |
| logging | log batching |
| GC | allocation buffer |

### Kubernetes

| 영역 | 영향 |
|------|------|
| container memory | buffer accumulation |
| network queue | packet buffering |
| ingress | request buffering |
| message queue | backlog |
| sidecar proxy | socket buffer |

memory leak처럼 보이는 buffer accumulation이 매우 흔합니다.

### Observability

| 지표 | 의미 |
|------|------|
| queue depth | backlog |
| socket drops | overflow |
| rx_missed_errors | NIC saturation |
| dirty pages | disk pressure |
| buffer cache hit ratio | cache efficiency |
| latency percentile | buffering delay |