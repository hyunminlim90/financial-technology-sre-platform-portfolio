# Dynamic Random Access Memory (DRAM)
## 1. DRAM 이란 무엇인가

동적 임의 접근 메모리(DRAM)는:

> 컴퓨터 시스템에서 실행 중인 데이터와 프로그램 상태를 대용량으로 유지하는 **메인 메모리(Main Memory)**

핵심 특징은 다음 두 가지입니다.

1. **Random Access** — 어느 주소든 동일한 방식으로 접근 가능
2. **Dynamic** — 저장 전하가 자연 누설되므로 주기적인 Refresh 필요

DRAM은 물리적으로 **1 Transistor + 1 Capacitor (1T1C Cell)** 구조를 기반으로 동작합니다.

커패시터 내부 전하 존재 여부로:

| 상태 | 값 |
|------|-----|
| 전하 있음 | `1` |
| 전하 없음 | `0` |

을 표현합니다.

> 전하는 시간이 지나면 누설되므로, DRAM Controller가 지속적으로 **Refresh 사이클**을 수행해야 데이터가 유지됩니다.

<details>
<summary>Deep Dive</summary></br>

Computer(컴퓨터) [[M]](../../100-deep-dive/micro-foundations/computer.md)  
System(시스템) [[M]](../../100-deep-dive/micro-foundations/)  
Data(데이터) [[M]](../../100-deep-dive/micro-foundations/data.md)  
Computer Program(컴퓨터 프로그램) [[M]](../../100-deep-dive/micro-foundations/computer-program.md)  


</details></br>

## 2. 시스템 어디에서 등장하는가

DRAM은 시스템 전체의 **중앙 데이터 저장소** 역할을 수행합니다.

```
CPU Core
   ↓
L1/L2/L3 Cache (SRAM)
   ↓
Memory Controller
   ↓
DRAM
   ↓
SSD / Disk
```

운영체제, 런타임, 애플리케이션, 컨테이너, VM 모두 결국 DRAM 위에서 실행됩니다.

**DRAM을 소비하는 대표 예시:**

- 프로세스 메모리
- 커널 메모리
- 파일 캐시 (Page Cache)
- 네트워크 버퍼
- 컨테이너 메모리
- VM Guest Memory
- GPU Shared Memory Mapping

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향도 |
|------|--------|
| Memory | 매우 큼 |
| CPU | 캐시 미스 시 강한 영향 |
| Network | 버퍼링 시 간접 영향 |
| Disk | Page Cache와 연계 |
| Latency | 매우 큼 |

**CPU 속도 vs DRAM 접근 지연 비교:**

| 계층 | 지연 시간 |
|------|-----------|
| CPU Register | ~sub-ns |
| L1 Cache | ~1ns |
| L2/L3 Cache | ~수 ns |
| DRAM | ~50~100ns |
| SSD | ~수십 μs |
| Disk | ~ms |

> DRAM은 CPU 기준으로는 이미 **매우 느린 장치**입니다.

---

## 4. 왜 중요한가

DRAM은 시스템 전체 성능의 실질적인 **병목 지점** 중 하나입니다.

CPU는 매우 빠르지만, 실제 데이터는 대부분 DRAM에 있기 때문입니다.

현대 시스템 대부분은 **"연산 부족"** 보다 **"메모리 접근 대기"** 문제가 훨씬 큽니다. 이를 **Memory Wall** 이라고 부릅니다.

```
CPU: "계산할 준비 완료"
  ↓
데이터가 아직 DRAM에서 안 옴
  ↓
CPU Stall 발생
  ↓
파이프라인 정지
  ↓
Latency 증가
```

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. Memory Pressure

```
Free Memory 감소
  → Page Reclaim 증가
  → Swap 발생
  → IO 폭증
  → 시스템 Stall
```

### 5-2. OOM (Out Of Memory)

메모리 부족 시 **Kernel OOM Killer**가 프로세스를 강제 종료할 수 있습니다.

```bash
dmesg | grep -i oom
```

### 5-3. Cache Miss 폭증

```
CPU Cache Miss
  → DRAM Fetch 증가
  → Tail Latency 증가
```

특히 다음 워크로드에서 심합니다:
- 대규모 Hash Lookup
- Random Access Workload
- 비정형 메모리 접근

### 5-4. NUMA Latency

멀티소켓 서버에서 **다른 CPU 소켓의 DRAM 접근** 시 추가 레이턴시가 발생합니다.

### 5-5. GC / Runtime Pause 증가

대규모 메모리 스캔은 **DRAM 대역폭 + Cache Miss**를 동시에 유발합니다.

---

## 6. 핵심 메커니즘 요약

| # | 핵심 내용 |
|---|-----------|
| 1 | DRAM은 전하 저장소 — Bit = 전하 상태 |
| 2 | 전하는 자연적으로 누설됨 → Refresh 필요 |
| 3 | CPU는 DRAM보다 훨씬 빠름 → Cache 계층 필요 |
| 4 | 캐시 미스 발생 시 DRAM 접근 (L1 → L2 → L3 → DRAM) |
| 5 | 데이터 Locality가 매우 중요 — Random Access는 캐시 효율 붕괴 |

> **메모리 접근 패턴 = 성능**에 가깝습니다.

---

## 7. Linux / Runtime / K8s 에서 관측 방법

### Linux

```bash
# 메모리 상태
free -h

# 상세 메모리
cat /proc/meminfo

# NUMA 상태
numactl --hardware

# Page Fault
vmstat 1

# 메모리 압박
sar -r

# CPU Cache / DRAM 관측 (Hardware Counter)
perf stat -e cache-misses,cache-references
```

### Kubernetes

```bash
# Pod 메모리 사용량
kubectl top pod

# OOMKilled 확인
kubectl describe pod
```

**시스템 레벨 대표 증상:**

- 높은 Tail Latency
- CPU Utilization 낮은데 느림
- 갑작스러운 Stall
- Swap 증가
- OOMKill
- Node Memory Pressure

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*