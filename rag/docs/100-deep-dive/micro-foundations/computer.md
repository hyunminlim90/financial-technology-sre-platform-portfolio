# Computer
## 1. 컴퓨터란 무엇인가

컴퓨터(Computer)는:

> 입력(Input)된 데이터를 프로그램 제어 흐름에 따라 연산·처리하고, 결과를 저장(Store) 및 출력(Output)하는 **디지털 전자 제어 시스템**

현대 컴퓨터 대부분은 **Von Neumann Architecture (폰 노이만 구조)** 를 기반으로 동작합니다.

핵심 특징:

1. 프로그램과 데이터를 동일 메모리에 저장
2. CPU가 명령어를 순차적으로 실행
3. Fetch → Decode → Execute 반복

> 컴퓨터의 본질은 **"명령어와 데이터를 읽고 계속 상태를 변경하는 기계"** 에 가깝습니다.

<details>
<summary>Deep Dive</summary></br>

Input Data(입력 데이터) [[M]](../../100-deep-dive/micro-foundations/input-data.md)  


</details></br>

## 2. 시스템 어디에서 등장하는가

컴퓨터는 모든 디지털 시스템의 **최상위 집합체**입니다.

```
[ Software Layer ]
  Application
  Runtime
  Operating System
        ↓
[ Hardware Layer ]
  CPU / Memory / Storage
  Network Device / I/O Controller / Bus
```

**실제 실행 흐름:**

```
사용자 입력
    ↓
OS
    ↓
CPU 실행
    ↓
Memory 접근
    ↓
Storage / Network / Display 출력
```

> 서버, 노트북, 스마트폰, 클라우드 노드, Kubernetes Worker Node 모두 결국 컴퓨터입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

컴퓨터는 단일 자원이 아니라 CPU, Memory, Disk, Network 전체를 통합 관리하는 시스템입니다.

| 자원 | 대표 병목 |
|------|-----------|
| CPU | 연산 부족 |
| Memory | Cache Miss / Stall |
| Disk | Random IO |
| Network | Packet Loss / Latency |

> 현대 시스템은 특히 **CPU 자체보다 Memory / IO 대기가 훨씬 큰 문제**인 경우가 많습니다.

---

## 4. 왜 중요한가

컴퓨터를 이해한다는 것은 **"모든 소프트웨어가 최종적으로 어떤 물리 시스템 위에서 실행되는가"** 를 이해하는 것입니다.

프로세스, 스레드, 컨테이너, VM, Kubernetes, Database, Runtime — 모두 결국 **CPU + Memory + IO** 위에서 동작합니다.

> **소프트웨어 문제처럼 보이는 현상도 실제로는 하드웨어 병목일 수 있습니다.**

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. CPU Saturation

```
CPU 사용률 100%
  → Run Queue 증가
  → Context Switch 증가
  → 응답 지연
```

### 5-2. Memory Stall

CPU는 한가한데 느린 경우:

```
Cache Miss
  → DRAM Access 대기
  → CPU Stall
```

### 5-3. Disk IO 병목

```
Disk Queue 증가
  → IO Wait 증가
  → 전체 시스템 지연
```

### 5-4. Network 병목

```
Packet Drop
  → Retransmission
  → Tail Latency 증가
```

### 5-5. Thermal Throttling

하드웨어 발열 시 **CPU Frequency 감소**가 발생할 수 있습니다.

---

## 6. 핵심 메커니즘 요약

### 6-1. 컴퓨터는 상태 머신(State Machine)이다

현재 상태(State) + 명령어(Instruction) → 다음 상태로 이동

### 6-2. CPU는 명령어를 반복 실행한다

```
1. Fetch
2. Decode
3. Execute
4. Write Back
5. Repeat
```

### 6-3. 메모리는 실행 상태 저장소이다

Program, Data, Stack, Heap, Kernel State — 모두 메모리에 존재합니다.

### 6-4. Storage는 영구 저장소이다

DRAM은 휘발성이므로 **SSD / HDD**가 영구 원장을 유지합니다.

### 6-5. 운영체제가 전체 자원을 통제한다

OS는 CPU Scheduling, Memory Management, IO Control, Process Isolation을 담당합니다.

### 6-6. 모든 성능 문제는 결국 자원 경쟁이다

```
여러 프로세스
  ↓
동일 자원 경쟁
  ↓
Queue 증가
  ↓
Latency 증가
```

---

## 7. Linux / Runtime / K8s 에서 관측 방법

### Linux

```bash
# CPU 상태
top
mpstat

# 메모리 상태
free -h
cat /proc/meminfo

# IO 상태
iostat

# 네트워크 상태
ss -s
sar -n DEV 1

# 전체 시스템 병목
vmstat 1
```

**vmstat 핵심 항목:**

| 항목 | 의미 |
|------|------|
| `r` | Run Queue |
| `si` / `so` | Swap IO |
| `wa` | IO Wait |

```bash
# 프로세스 상태
ps aux

# Context Switch
pidstat -w

# Hardware Counter
perf stat
```

### Kubernetes

```bash
# Node 자원 상태
kubectl top node

# Pod 자원 상태
kubectl top pod

# OOM / Restart 확인
kubectl describe pod
```

**시스템 레벨 장애 대표 증상:**

- CPU Saturation
- Memory Pressure
- IO Wait 증가
- Packet Drop
- 높은 Tail Latency
- Context Switch 폭증
- OOMKill
- Node Pressure

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*