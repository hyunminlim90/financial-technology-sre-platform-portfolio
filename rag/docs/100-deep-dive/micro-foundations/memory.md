# 메모리 (Memory)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**메모리(Memory)** 는:

> 데이터와 명령어를 일시적 또는 영구적으로 **유지하는 저장 공간**

컴퓨터는 단순 계산만 하지 않는다. 반드시 기억하고 / 읽고 / 수정하고 / 다시 전달해야 한다.

즉 메모리는 **연산 장치(CPU/GPU)가 작업하기 위한 데이터 보관 공간**이다.

---

## 2. 시스템 어디에서 등장하는가

메모리는 **시스템 전체**에 존재한다.

### CPU 내부
초고속 임시 메모리: `Register`, `L1/L2/L3 Cache`

### Main Memory
실행 중 데이터 저장: `DRAM(RAM)`

예: JVM Heap, Thread Stack, OS Buffer

### GPU
대규모 병렬 데이터 저장: `VRAM`, `HBM`

### Storage
영구 저장 메모리: `SSD`, `NVMe`

### Network Device
패킷 임시 저장: `NIC Buffer`, `DMA Ring Buffer`

### Kubernetes / Container
- Pod Memory
- Page Cache
- cgroup memory

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

핵심 영향 자원: **Memory** 자체. 하지만 실제로는 CPU, Disk, PCIe, NUMA 모두 연결된다.

| 연결 자원 | 영향 |
|----------|------|
| CPU | 대부분의 대기 시간이 메모리 접근 대기에서 발생 |
| Disk | RAM 부족 시 swap / page fault 발생 |
| Network | 패킷 처리도 메모리 버퍼 사용 |
| NUMA | 멀티소켓 환경에서 메모리 위치가 latency 결정 |

---

## 4. 왜 중요한가

현대 시스템 병목 상당수는 **연산보다 메모리**다.

> CPU는 매우 빠르다. 하지만 **데이터 공급이 느리면 CPU는 기다린다.**

### FinTech에서 특히 중요한 이유

| 이유 | 설명 |
|------|------|
| Low Latency | 수 ms 단위 응답, 짧은 GC pause, 빠른 cache hit 필요 |
| Throughput | 대규모 TPS 처리 시 메모리 bandwidth 부족하면 병목 발생 |
| Data Integrity | 결제 상태는 정확히 저장되고 정확히 읽혀야 함 |
| Availability | 메모리 부족 → OOM → GC Storm → Pod Kill 직결 |

---

## 5. 실제 장애와 어떤 관련이 있는가

### OOM Killer
RAM 부족 시 JVM Kill → Pod Restart → API 장애 발생

### Memory Leak
메모리 반환 실패 시 Heap 증가 → GC 증가 → Eventually OOM

### Swap Thrashing
RAM 부족으로 SSD swap 사용 시 Latency 폭증 → TPS 감소

### Cache Miss Explosion
CPU cache hit 감소 시 Memory Stall → IPC 감소 발생

### NUMA Remote Access
다른 소켓 메모리 접근 시 Memory latency 증가 → CPU 효율 감소

### GC Pause
Heap 과도 증가 시 Stop-The-World → API Timeout 발생

### Page Fault Storm
메모리 페이지 재적재 증가 시 Disk I/O 증가 → 응답 지연 발생

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

### Memory Hierarchy
CPU는 가까운 메모리 접근일수록 빠르다:

```
Register
→ L1 Cache
→ L2 Cache
→ L3 Cache
→ RAM
→ SSD/NVMe
```

### Memory Access Latency 대략적 접근 시간

| 계층 | 대략적 속도 |
|------|-----------|
| Register | < 1ns |
| L1 Cache | ~1ns |
| RAM | ~100ns |
| NVMe | ~100μs |
| Network | ~1ms 이상 |

### Cache Locality
성능의 핵심. CPU cache는 자주 쓰는 데이터를 저장한다.

- **Spatial Locality:** 근처 데이터 재사용
- **Temporal Locality:** 최근 데이터 재사용

### Volatile vs Non-Volatile

- **Volatile:** 전원 OFF 시 데이터 소멸. 예: `Register`, `Cache`, `RAM`
- **Non-Volatile:** 전원 OFF 후 유지. 예: `SSD`, `NVMe`

### Data Movement Cost
현대 시스템 병목은 연산 자체보다 **메모리 이동**인 경우가 많다

예: `SSD → RAM → Cache → Register`

### Virtual Memory
OS는 가상 주소를 실제 물리 메모리에 매핑한다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Memory Usage
```bash
free -h
vmstat 1
```

### NUMA
```bash
numactl --hardware
```

### Page Cache
```bash
cat /proc/meminfo
```

### Swap
```bash
swapon --show
```

### OOM
```bash
dmesg | grep -i oom
```

### CPU Cache 관련
```bash
lscpu
perf stat
```

### JVM
```bash
jstat -gc
jcmd GC.heap_info
```

### Kubernetes
```bash
kubectl top pod
kubectl describe pod
```
확인 포인트: `memory request/limit`, `OOMKilled`, `eviction`

### Disk-backed Memory 영향
```bash
iostat -x 1
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*