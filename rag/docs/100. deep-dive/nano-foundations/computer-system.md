# Computer System

> 정독: 0회

컴퓨터 시스템(Computer System)은:

> 입력(Input)된 데이터를 프로그램의 제어 흐름(Control Flow)에 따라 처리(Data Processing)하고, 저장(Data Storage)하며, 최종적으로 출력(Data Output)하는 **하드웨어와 소프트웨어의 유기적 결합체**

**"데이터를 받아서, 규칙에 따라 계산하고, 결과를 저장·출력하는 전체 구조"**

---

## 1. 이 기술이 무엇인가

컴퓨터 시스템은 단일 장치가 아닙니다. 다음 요소들이 함께 움직이는 **전체 집합체**입니다.

- CPU / Memory / Storage / Network
- Operating System / Program
- Input / Output Device

현대 시스템 대부분은 **Von Neumann Architecture(폰 노이만 구조)** 를 기반으로 동작합니다.

> 핵심 개념: **명령어와 데이터가 동일한 메모리 공간(DRAM)에 존재**

**시스템 전체 흐름:**

```
Input
  ↓
Main Memory
  ↓
CPU Processing
  ↓
Storage / Output
```

---

## 2. 시스템 어디에서 등장하는가

컴퓨터 시스템은 사실상 **모든 디지털 환경 자체**입니다.

| 시스템 | 설명 |
|--------|------|
| 노트북 | 개인 컴퓨터 시스템 |
| 서버 | 대규모 처리 시스템 |
| 스마트폰 | 모바일 컴퓨터 시스템 |
| 라우터 | 네트워크 처리 시스템 |
| Kubernetes Node | 분산 컴퓨터 시스템 |
| 클라우드 데이터센터 | 거대한 분산 시스템 |

**운영체제 관점의 전체 구조:**

```
Hardware
  ↓
Kernel
  ↓
Runtime
  ↓
Application
  ↓
User / Network
```

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 역할 |
|------|------|
| CPU | 연산 |
| Memory | 실행 상태 유지 |
| Disk | 영구 저장 |
| Network | 외부 통신 |
| Cache | 속도 완충 |
| Bus | 데이터 이동 |

> 컴퓨터 시스템은 **자원들의 협력 구조**입니다. 실제 병목은 대부분 **자원 간 속도 차이**에서 발생합니다.

```
CPU (매우 빠름)
  ↓
DRAM (상대적으로 느림)
  ↓
SSD (훨씬 느림)
  ↓
Network RTT (더 느림)
```

이 속도 차이를 관리하는 것이 **시스템 설계의 핵심**입니다.

---

## 4. 왜 중요한가

컴퓨터 시스템을 이해하면 다음을 이해할 수 있습니다.

- 왜 병목이 생기는가
- 왜 레이턴시가 증가하는가
- 왜 CPU는 놀고 있는데 느린가
- 왜 메모리가 부족한가
- 왜 네트워크가 막히는가

> 모든 인프라 문제는 **컴퓨터 시스템 내부 상호작용 문제**입니다. 운영체제, Kubernetes, 데이터베이스, 네트워크도 결국 컴퓨터 시스템 위에서 동작합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. CPU Bottleneck

```
Too Many Instructions
  ↓
CPU Saturation
  ↓
Latency 증가
```

### 5-2. Memory Pressure

```
Working Set 증가
  ↓
DRAM 부족
  ↓
Swap / OOM
```

### 5-3. Disk IO Stall

```
Slow Storage
  ↓
IO Wait 증가
  ↓
System Stall
```

### 5-4. Network Congestion

```
Packet Queue 증가
  ↓
Retransmission
  ↓
Tail Latency 증가
```

### 5-5. Context Switching 폭증

```
Too Many Threads
  ↓
Context Switch 증가
  ↓
Cache Pollution
  ↓
CPU Stall
```

### 5-6. Cache Miss

```
Random Access 증가
  ↓
Cache Miss 증가
  ↓
DRAM Access 증가
```

### 5-7. System Load Explosion

```
Input Rate > Processing Capacity
  ↓
Queue Explosion
  ↓
Latency Cascade
```

---

## 6. 핵심 메커니즘 요약

### 6-1. 컴퓨터 시스템은 데이터 이동 시스템이다

```
Disk → Memory → Cache → Register → ALU
```

최하단 관점에서 시스템은 **데이터 이동 + 연산**입니다.

### 6-2. CPU는 Register 데이터만 직접 계산한다

```
DRAM → Cache → Register → ALU
```

CPU는 DRAM을 직접 계산하지 않습니다.

### 6-3. 메모리 계층 구조가 핵심이다

| 계층 | 특성 |
|------|------|
| Register / L1 Cache | 빠름, 작음, 비쌈 |
| L2 / L3 Cache | 중간 |
| DRAM | 느림, 큼, 저렴 |
| SSD | 더 느림, 더 큼 |

속도 차이를 줄이기 위해 **계층 구조**를 사용합니다.

### 6-4. 운영체제가 자원을 통제한다

OS 커널은 CPU Scheduling, Memory Management, IO Control, Process Isolation을 담당합니다.

> 운영체제는 **시스템 자원 관리자**입니다.

### 6-5. 프로그램은 결국 제어 흐름이다

```
Instruction Fetch → Decode → Execute → Next PC  (반복)
```

### 6-6. 모든 성능 문제는 자원 경합이다

CPU, Memory, Disk, Network 중 하나가 처리 속도를 따라가지 못할 때 병목이 발생합니다.

### 6-7. 시스템은 상태(State)를 유지한다

Register State, Memory State, File State, Socket State, Process State — 이 상태들의 총합이 **시스템 동작을 결정**합니다.

---

## 7. Linux / Runtime / K8s 에서 관측 방법

### Linux

```bash
# CPU 상태
top
htop
mpstat

# 메모리 상태
free -h
vmstat

# IO 상태
iostat -x 1

# 프로세스 상태
ps aux
top

# 네트워크 상태
ss -s
sar -n DEV 1

# 시스템 콜 흐름
strace

# 성능 분석
perf stat
perf top
```

### Runtime

| 지표 | 핵심 질문 |
|------|-----------|
| Throughput / Latency | 시스템 어디에서 병목이 생기는가? |
| Queue Length / IO Wait | 어떤 자원이 포화되어 있는가? |
| CPU Usage / Memory Usage | 자원 사용이 한계에 도달했는가? |
| Retry / Error Rate | 오류가 연쇄적으로 발생하는가? |

### Kubernetes

```bash
# Pod 상태
kubectl get pod

# 자원 사용량
kubectl top pod
kubectl top node

# 이벤트 확인
kubectl describe pod

# 로그 확인
kubectl logs <pod>

# 노드 상태 확인
kubectl describe node
```

**대표 노드 상태:**

| 상태 | 의미 |
|------|------|
| `MemoryPressure` | 노드 메모리 압박 조건 발동 |
| `DiskPressure` | 노드 디스크 압박 조건 발동 |
| `PIDPressure` | 노드 프로세스 수 한계 도달 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*