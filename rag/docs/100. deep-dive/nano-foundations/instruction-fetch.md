# Instruction Fetch (명령어 인출)
## **Micro Foundations — 범용 컴퓨터 구조 / 시스템 관점**

> 정독: 0회

## 1. 이 기술이 무엇인가

**Instruction Fetch**는:

> CPU가 다음에 실행할 기계어 명령어를 **메모리(RAM)에서 읽어오는 과정**

명령어 사이클의 **첫 번째 단계**이며:

```
Fetch → Decode → Execute → Write-back
```

중 가장 최초의 관문이다.

CPU는 스스로 생각해서 움직이지 않는다. 항상 **"다음 명령어 어디 있지?"**를 먼저 찾아야 한다. 그 과정이 Instruction Fetch다.

> **핵심:** Instruction Fetch는 **CPU 실행 엔진에 '다음 작업 지시서'를 공급하는 메커니즘**이다.

---

## 2. 시스템 어디에서 등장하는가

Instruction Fetch는 **CPU가 명령어를 실행하는 모든 순간** 항상 발생한다.

### 운영체제
- scheduler 실행, interrupt 처리, syscall 처리

### 애플리케이션 실행
- 함수 호출, loop, 조건문, 계산

### 네트워크 처리
- TCP stack, packet parsing, routing

### 스토리지 처리
- filesystem, journaling, block I/O

### 가상화 / 컨테이너
- hypervisor, container runtime, orchestration agent

> **핵심:** Instruction Fetch는 **컴퓨터 시스템 전체의 실행 흐름 시작점**이다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **CPU + Memory**이다.

### CPU 영향
- Fetch Unit 자체가 CPU 내부 구성 요소
- Instruction 공급이 멈추면 CPU 연산 유닛(ALU)이 놀게 된다

### Memory 영향
- Instruction은 RAM에서 가져온다
- 영향 요소: memory latency, cache hierarchy, memory bandwidth

### Cache 영향
- 현대 CPU는 대부분 **L1 Instruction Cache**에서 명령어를 읽는다
- Cache miss 발생 시 → RAM 접근 대기 발생

### Disk 영향 (간접)
- 프로그램 최초 로딩 시 disk → RAM 이동 필요

### Network 영향 (간접)
- distributed execution, remote storage, network filesystem 환경에서 간접 영향 존재

> **핵심:** Instruction Fetch 성능은 **CPU가 얼마나 빠르게 명령어를 공급받는가** 문제다.

---

## 4. 왜 중요한가

CPU는 **명령어가 없으면 아무 일도 못 한다.**

> **Fetch 실패 = 실행 중단**

현대 CPU 성능의 상당 부분은 **"어떻게 명령어를 끊김 없이 공급할 것인가"** 문제에 투자된다.

다음은 모두 **Instruction Fetch 최적화**와 연결된다:

- instruction cache
- prefetch
- branch prediction
- pipeline
- superscalar

또한 Fetch는 latency · IPC · pipeline efficiency · CPU utilization 전체에 **직접 영향**을 준다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 1) Instruction Cache Miss
```
CPU가 필요한 명령어를 cache에서 못 찾음 → RAM 기다림 발생
  ↓
latency 증가 → IPC 감소 → throughput 저하
```

### 2) Branch Misprediction
```
CPU가 미래 명령 흐름 예측 실패 → 잘못 Fetch한 명령 폐기
  ↓
pipeline flush → wasted cycles
```

### 3) Context Switch 폭증
```
OS가 task를 자주 바꿈 → instruction cache invalidation, fetch locality 붕괴
  ↓
CPU efficiency 하락 → scheduler overhead 증가
```

### 4) Memory Latency Spike
```
RAM 응답 느려짐 → Instruction Fetch stall
  ↓
CPU utilization은 높지 않은데 느림 → stalled cycles 증가
```

### 5) CPU Throttling
```
K8s/cgroup quota 제한
  ↓
fetch pipeline 진행 지연 → event processing 밀림
```

> **핵심:** Instruction Fetch 문제는 CPU가 계산을 못 하는 문제가 아니라 **"다음 작업을 제때 못 받는 문제"**다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Fetch 흐름

```
PC → MAR → RAM → MBR → IR
```

| 구성 요소 | 의미 |
|-----------|------|
| **Program Counter (PC)** | 다음 명령어 주소 기억 — Fetch 시작점 |
| **Instruction Register (IR)** | 가져온 명령 저장 — Decode 단계 입력값 |
| **MAR** | 메모리 주소 전달 |
| **MBR (MDR)** | 메모리 데이터 임시 저장 |
| **Address Bus** | 메모리 주소 전달선 |
| **Data Bus** | 명령어/데이터 전달선 |

### Fetch 최적화 메커니즘

| 메커니즘 | 의미 |
|----------|------|
| **Instruction Cache (I-Cache)** | 명령어 전용 cache — Fetch 성능 핵심 |
| **Prefetch** | 미래 명령어 미리 읽기 |
| **Pipeline** | 현재 명령 Decode 중 다음 명령 Fetch 동시 진행 |
| **Branch Prediction** | 다음 실행 경로 예측 후 미리 Fetch |
| **Fetch Stall** | 명령 공급 실패 — CPU pipeline 정지 상태 |

> **핵심:** 현대 CPU는 실행 자체보다 **명령어를 끊김 없이 공급하는 문제**를 매우 중요하게 다룬다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

Instruction Fetch 자체는 하드웨어 내부 동작이라 직접 보이지 않는다. 하지만 **결과는 관측 가능**하다.

### Linux

**perf**
```bash
perf stat
perf top
```
관찰: instructions, cycles, stalled-cycles, cache-misses, branch-misses

**CPU pipeline 상태**
```bash
perf record
perf report
```

**Context switch**
```bash
vmstat 1
pidstat -w
```

---

### Runtime

관찰 포인트:
- excessive thread switching
- scheduler delay
- instruction locality 붕괴

---

### Kubernetes

| 관찰 영역 | 명령어 | 관찰 |
|-----------|--------|------|
| **CPU throttling** | `kubectl top pod` | quota 초과, 처리 지연 |
| **Pod 상태** | `kubectl describe pod` | noisy neighbor, quota 제한 |

> **핵심:** Instruction Fetch 병목은 보통 **CPU stall·cache miss·latency 증가** 형태로 나타난다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*