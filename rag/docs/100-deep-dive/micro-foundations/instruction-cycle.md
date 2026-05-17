# Instruction Cycle (명령어 사이클)
## **Micro Foundations — 범용 컴퓨터 구조/시스템 관점**

> 정독: 0회

## 1. 이 기술이 무엇인가

**Instruction Cycle**은:

> CPU가 기계어 명령어를 읽고, 해석하고, 실행하고, 결과를 반영하는 **반복 실행 루프**

쉽게 말하면 CPU는:

```
"다음 명령 뭐지?" → 읽기 → 해석 → 실행 → 결과 저장 → 다음 명령 이동
```

을 초당 수십억 번 반복한다.

운영체제, 데이터베이스, 웹 서버, 메시지 큐 등 모든 소프트웨어는 최종적으로 **0과 1의 기계어 명령어**로 변환된다. CPU는 결국 이 명령어들을 처리하는 기계다.

> **핵심:** Instruction Cycle은 **컴퓨터가 실제로 움직이는 최하단 실행 메커니즘**이다.

<details>
<summary>Deep Dive</summary></br>

Instruction(명령어) [[M]](../../100-deep-dive/micro-foundations/instruction.md)  
Instruction Fetch(명령어 인출) [[M]](../../100-deep-dive/micro-foundations/instruction-fetch.md)  
Instruction Decode(명령어 해독) [[M]](../../100-deep-dive/micro-foundations/instruction-decode.md)  
Execute(실행) [[M]](../../100-deep-dive/micro-foundations/execute.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

Instruction Cycle은 **CPU가 살아 있는 모든 순간** 항상 존재한다.

### 운영체제
- process scheduling, interrupt handling, syscall execution

### 애플리케이션 실행
- business logic, loop, condition, memory access

### 데이터 처리
- serialization, encryption, compression, checksum

### 네트워크 처리
- packet parsing, TCP stack, routing

### 스토리지 처리
- filesystem, journaling, block I/O

> **핵심:** 컴퓨터의 모든 동작은 결국 **Instruction Cycle들의 집합**이다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **CPU + Memory**이다.

### CPU 영향
- **Instruction Cycle 자체가 CPU의 존재 목적이다**
- 영향 요소: clock frequency, IPC, pipeline efficiency, branch prediction, context switching

### Memory 영향
- **명령어와 데이터는 RAM에서 가져온다**
- 영향 요소: cache miss, memory latency, memory bandwidth

### Disk 영향 (간접)
- 프로그램/데이터를 RAM으로 로드할 때 발생
- 예: executable load, page fault, swap

### Network 영향 (간접)
- 네트워크 패킷 처리 자체가 CPU 명령어 실행이다

> **핵심:** Instruction Cycle 성능은 **CPU가 얼마나 기다리지 않고 계속 일할 수 있는가**에 달려 있다.

---

## 4. 왜 중요한가

Instruction Cycle은 **시스템 성능의 가장 근본적인 단위**이다.

모든 고성능 시스템 최적화는 결국 **"CPU가 덜 기다리게 만들기"** 문제로 귀결된다.

다음은 모두 **Instruction Cycle 낭비 감소**를 목표로 한다:

- cache 사용
- batching
- async I/O
- zero-copy
- pipeline optimization
- branch reduction

또한 latency · throughput · scalability · power efficiency 모두 **CPU 명령 처리 효율**과 연결된다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 1) CPU Saturation
```
명령어 처리량 초과
  ↓
run queue 증가 → latency spike → throttling
```

### 2) Context Switch Storm
```
스레드 과다 생성 → CPU가 실제 작업보다 상태 저장/복원에 시간을 더 씀
  ↓
cache invalidation → scheduler overhead → throughput collapse
```

### 3) Cache Miss Explosion
```
CPU cache에 데이터 없음
  ↓
CPU가 RAM 기다리느라 멈춤
```

### 4) Branch Misprediction
```
분기 예측 실패
  ↓
pipeline flush → wasted cycles
```

### 5) Interrupt Storm
```
과도한 인터럽트 발생
  ↓
정상 instruction flow 붕괴 → kernel overhead 증가
```

### 6) CPU Throttling
```
OS/K8s가 CPU quota 제한
  ↓
instruction execution 지연 → event loop stall → timeout 증가
```

> **핵심:** Instruction Cycle 문제는 CPU 사용률만이 아니라 **"CPU가 얼마나 비효율적으로 일하는가"** 문제다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 기본 4단계

```
Fetch → Decode → Execute → Write-back → (반복)
```

| 단계 | 의미 |
|------|------|
| **Fetch** | 명령어 가져오기 |
| **Decode** | 명령 의미 해석 |
| **Execute** | 실제 연산 수행 |
| **Write-back** | 결과 저장 |

### CPU 핵심 구성 요소

| 구성 요소 | 의미 |
|-----------|------|
| **Register** | CPU 내부 초고속 저장 공간 — Instruction Cycle 중심 축 |
| **Program Counter (PC)** | 다음 실행 명령 위치 기억 |
| **Instruction Register (IR)** | 현재 명령 저장 |
| **ALU** | 산술/논리 연산 수행 |
| **CPU Flags** | 연산 결과 상태 저장 (zero, carry, overflow, sign) |

### 현대 CPU 최적화 메커니즘

| 메커니즘 | 의미 |
|----------|------|
| **Pipeline** | 여러 명령어를 동시에 다른 단계에서 처리 |
| **Superscalar** | 여러 instruction 동시 실행 |
| **Branch Prediction** | 다음 분기 미리 예측 |
| **Out-of-Order Execution** | 순서 바꿔 실행 가능한 것 먼저 처리 |
| **Cache Hierarchy** | L1 → L2 → L3 → RAM — CPU가 RAM 기다리지 않게 함 |
| **Context Switch** | 현재 실행 상태 저장 후 다른 task 전환 — 비용 큼 |

> **핵심:** 현대 CPU 아키텍처는 **Instruction Cycle을 가능한 끊기지 않게 유지하는 방향**으로 진화했다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**CPU 상태**
```bash
top
htop
mpstat
vmstat
```
관찰: CPU usage, run queue, context switch, system/user time

**Context Switch**
```bash
vmstat 1
pidstat -w
```
관찰: cs (context switch 횟수)

**CPU 성능 분석**
```bash
perf stat
perf top
```
관찰: cycles, instructions, cache miss, branch miss

**인터럽트 상태**
```bash
cat /proc/interrupts
```

---

### Runtime

관찰 포인트:
- thread contention
- scheduler delay
- GC pause
- lock contention
- instruction stall

---

### Kubernetes

| 관찰 영역 | 명령어 | 관찰 |
|-----------|--------|------|
| **CPU throttling** | `kubectl top pod` | CPU quota 초과 여부 |
| **Cgroup 제한** | cgroup 메트릭 | CPU quota, throttled time |
| **노드 압박** | `kubectl describe node` | CPUPressure |

> **핵심:** Instruction Cycle 문제는 상위 레벨에서는 보통 **CPU saturation·latency·throttling** 형태로 드러난다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*