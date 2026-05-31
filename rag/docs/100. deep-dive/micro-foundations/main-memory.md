# Main Memory
## 1. 주기억장치란 무엇인가

주기억장치(Main Memory)는:

> 현재 실행 중인 프로그램과 데이터를 CPU가 빠르게 접근할 수 있도록 적재해 두는 **중앙 실행 메모리**

현대 컴퓨터에서는 일반적으로 **DRAM**이 주기억장치 역할을 담당합니다.

```
Storage
  ↓
Main Memory
  ↓
CPU Cache
  ↓
CPU Register
```

> **"디스크에 있던 프로그램과 데이터를 실행 중에 CPU 가까이 올려두는 작업 공간"**

| 구분 | 특성 |
|------|------|
| 디스크 | 크고 영구적이지만 느림 |
| 주기억장치 | 휘발성이지만 훨씬 빠름 |

---

## 2. 시스템 어디에서 등장하는가

주기억장치는 CPU와 보조기억장치 사이에 위치합니다.

```
CPU Core
  ↓
Register
  ↓
L1 / L2 / L3 Cache
  ↓
Main Memory (DRAM)
  ↓
SSD / HDD
```

**운영체제 관점에서 주기억장치가 사용되는 영역:**

- Process Virtual Memory
- Kernel Memory
- Page Cache
- Buffer Cache
- Network Buffer
- File System Cache

**프로그램 실행 흐름:**

```
Program File on Disk
  ↓
OS Loader
  ↓
Main Memory
  ↓
Process Address Space
  ↓
CPU Execution
```

> 주기억장치는 **정적인 프로그램 파일이 동적인 프로세스로 바뀌는 핵심 장소**입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향 |
|------|------|
| CPU | Cache Miss 발생 시 DRAM 접근 대기 |
| Memory | 프로그램, 데이터, 커널 상태 저장 |
| Disk | 부족하면 Swap / Page In / Page Out 발생 |
| Network | 송수신 버퍼와 패킷 처리에 사용 |
| Latency | 메모리 접근 지연이 전체 응답 시간에 영향 |

```
메모리 충분  →  Disk 접근 감소  →  Cache / Buffer 효율 증가  →  Latency 안정화
메모리 부족  →  Reclaim / Swap / OOM 증가  →  전체 시스템 불안정
```

---

## 4. 왜 중요한가

주기억장치는 컴퓨터가 **"현재 실행 중인 상태"를 보관하는 핵심 자원**입니다.

CPU는 연산 장치이고, 디스크는 영구 저장소입니다. 그 사이에서 주기억장치는 다음을 보관합니다.

- 실행 중인 코드 / 데이터
- 커널 상태 / 프로세스 상태
- 파일 캐시 / 네트워크 버퍼

> 주기억장치가 부족하거나 느려지면 **CPU가 빨라도 실제 작업은 느려지는** 현상이 발생합니다.

**운영 관점의 핵심 판단 기준:**

- CPU 병목인가? Memory Pressure인가?
- Disk IO로 밀려났는가?
- Page Cache가 부족한가?
- OOM 위험이 있는가?

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. Memory Pressure

```
Free Memory 감소
  ↓
Page Reclaim 증가
  ↓
Cache 축소
  ↓
Latency 증가
```

### 5-2. Swap 발생

```
DRAM 부족
  ↓
Swap Out
  ↓
다시 필요할 때 Swap In
  ↓
Disk IO 증가
  ↓
응답 지연 급증
```

### 5-3. OOM Kill

```
Memory Exhaustion
  ↓
OOM Killer
  ↓
Process Killed
```

Kubernetes에서는 `OOMKilled`, `CrashLoopBackOff` 상태로 나타납니다.

### 5-4. Page Cache 부족

```
Page Cache 감소
  ↓
Disk Read 증가
  ↓
IO Wait 증가
  ↓
Throughput 저하
```

### 5-5. Cache Miss / DRAM Stall

```
Cache Miss
  ↓
Main Memory Access
  ↓
CPU Stall
  ↓
Tail Latency 증가
```

---

## 6. 핵심 메커니즘 요약

### 6-1. 주기억장치는 실행 중인 상태를 저장한다

Program, Data, Kernel State, Process Context, Buffer, Cache — 모두 주기억장치에 올라갑니다.

### 6-2. CPU는 직접 디스크에서 실행하지 않는다

```
Disk → Main Memory → CPU Cache → CPU Register → Execution
```

### 6-3. 현대 주기억장치는 DRAM이다

DRAM은 대용량에 적합하지만 **휘발성**입니다. 전원이 꺼지면 내용이 소실되며, 영구 보관은 Storage가 담당합니다.

### 6-4. OS는 주기억장치를 페이지 단위로 관리한다

```
Virtual Address
  ↓
Page Table
  ↓
Physical Memory
```

기본 페이지 크기는 보통 **4KB**입니다.

### 6-5. CPU와 주기억장치 사이에는 캐시 계층이 있다

```
CPU Register → L1 Cache → L2 Cache → L3 Cache → Main Memory
```

데이터는 보통 **캐시 라인(≈ 64 Byte)** 단위로 이동합니다.

### 6-6. 메모리 부족은 디스크 병목으로 전이된다

```
Memory 부족
  ↓
Page Cache 감소
  ↓
Swap / Reclaim 증가
  ↓
Disk IO 증가
  ↓
전체 Latency 증가
```

---

## 7. Linux / Runtime / K8s 에서 관측 방법

### Linux

```bash
# 전체 메모리 상태
free -h

# 상세 메모리 정보
cat /proc/meminfo

# 메모리 압박 / Swap 확인
vmstat 1

# 프로세스별 메모리 (사용량 내림차순)
ps aux --sort=-%mem

# NUMA 메모리 구조
numactl --hardware

# OOM 로그 확인
dmesg | grep -i oom
```

**`/proc/meminfo` 핵심 항목:**

| 항목 | 의미 |
|------|------|
| `MemTotal` | 전체 물리 메모리 |
| `MemAvailable` | 실제 사용 가능한 메모리 |
| `Buffers` | 블록 장치 버퍼 |
| `Cached` | 파일 Page Cache |
| `SwapTotal` / `SwapFree` | Swap 전체 / 여유 |
| `Dirty` / `Writeback` | 디스크에 아직 안 쓴 데이터 |

**`vmstat` 핵심 항목:**

| 항목 | 의미 |
|------|------|
| `si` | Swap In |
| `so` | Swap Out |
| `free` | 여유 메모리 |
| `wa` | IO Wait |

### Runtime

| 지표 | 핵심 질문 |
|------|-----------|
| RSS / VSS | 실제 물리 메모리를 얼마나 쓰는가? |
| Heap Usage / Native Memory | 가상 주소 공간만 큰 것인가? |
| Allocation Rate | 할당 속도가 과도한가? |
| Page Fault | 메모리 누수가 있는가? |
| Thread Stack / Buffer Memory | 숨어있는 메모리 소비는 없는가? |

### Kubernetes

```bash
# Node 메모리
kubectl top node

# Pod 메모리
kubectl top pod

# Pod OOM 확인
kubectl describe pod

# Node Memory Pressure 확인
kubectl describe node
```

**대표 상태:**

| 상태 | 의미 |
|------|------|
| `OOMKilled` | 메모리 초과로 강제 종료 |
| `Evicted` | 노드 메모리 부족으로 축출 |
| `CrashLoopBackOff` | 반복 비정상 종료 |
| `MemoryPressure` | 노드 메모리 압박 조건 발동 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*