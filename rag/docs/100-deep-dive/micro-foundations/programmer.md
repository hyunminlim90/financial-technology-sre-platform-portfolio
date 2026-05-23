# 프로그래머 (Programmer)

> 정독: 0회

## 1. 이 기술이 무엇인가

프로그래머는:

> 컴퓨터 시스템이 수행할 논리와 동작을 프로그래밍 언어로 설계·구현·검증하는 **소프트웨어 실행 구조의 설계 주체**

### 핵심 역할

프로그래머는 다음을 수행합니다.

- 알고리즘 정의
- 데이터 구조 설계
- 상태 전이 정의
- 자원 제어
- 오류 처리
- 동시성 제어
- 입출력 흐름 설계

### 중요한 점

> 프로그래머는 단순히 코드만 작성하지 않습니다.

실제로는 다음까지 고려하여 시스템 행동을 설계합니다.

- CPU / Memory / Disk / Network
- OS / Runtime / Concurrency
- Failure / Latency / Consistency

---

## 2. 시스템 어디에서 등장하는가

프로그래머는 시스템 전체 생명주기에 등장합니다.

**대표 영역:**

- application development
- operating system
- distributed system
- runtime system
- embedded system
- database engine
- network stack
- kernel subsystem
- compiler
- cloud infrastructure
- SRE platform
- automation platform

### 실제 시스템 흐름

프로그래머의 설계 결과는 다음 흐름으로 연결됩니다.

```
source code
→ compiler
→ runtime
→ kernel
→ hardware
```

### 운영 환경 연결

프로그래머의 설계는 다음 운영 특성까지 결정합니다.

- latency / throughput / scalability
- fault tolerance / memory pressure
- deadlock / GC pause / I/O bottleneck

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

정답은 **모든 시스템 자원**입니다.

| 자원 | 프로그래머가 결정하는 항목 |
|---|---|
| **CPU** | 알고리즘 복잡도, synchronization, busy loop, scheduling pressure |
| **Memory** | object lifecycle, allocation pattern, cache locality, memory fragmentation |
| **Disk** | I/O pattern, buffering, batching, fsync strategy |
| **Network** | serialization, connection lifecycle, retry storm, packet amplification |

---

## 4. 왜 중요한가

모든 소프트웨어 시스템의 **실제 동작 원인을 정의**하기 때문입니다.

| 이유 | 설명 |
|---|---|
| **1. 시스템 행동 정의** | 시스템은 프로그래머가 작성한 논리대로 동작 |
| **2. 자원 소비 패턴 결정** | CPU / Memory / Disk / Network 사용 형태 결정 |
| **3. 장애 가능성 결정** | 설계, 상태 관리, 동시성 처리, I/O 처리, 오류 처리에서 발생 |
| **4. 확장성 결정** | scale-out 가능 여부 결정 |
| **5. 운영 복잡도 결정** | 관측성(observability)과 운영 난이도까지 영향 |

---

## 5. 실제 장애와 어떤 관련이 있는가

### CPU Saturation

원인: `infinite loop`, `inefficient algorithm`, `lock contention`

### Memory Leak

원인: `object retention`, `reference cycle`, `cache misuse`

### Disk Bottleneck

원인: `sync write abuse`, `excessive fsync`, `small random I/O`

### Network Failure

원인: `retry storm`, `connection leak`, `timeout misconfiguration`

### Concurrency Failure

원인: `deadlock`, `race condition`, `starvation`

### Distributed Failure

원인: `inconsistent state transition`, `duplicate processing`, `partial failure handling mistake`

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

핵심 메커니즘은 **8개**입니다.

| # | 메커니즘 | 설명 |
|---|---|---|
| 1 | **Problem Modeling** | 현실 문제를 계산 가능한 구조로 변환 |
| 2 | **State Definition** | memory / transaction / connection / runtime state 정의 |
| 3 | **Control Flow Construction** | branch / loop / async flow / event handling 실행 흐름 정의 |
| 4 | **Data Structure Selection** | 성능과 메모리 구조 결정 |
| 5 | **Concurrency Design** | 멀티스레드 / 멀티코어 환경 제어 |
| 6 | **Runtime Interaction** | OS / runtime / JVM / kernel과 상호작용 |
| 7 | **I/O Coordination** | Disk / Network / device 흐름 제어 |
| 8 | **Failure Handling** | 예외 / timeout / retry / recovery 정의 |

### 핵심 흐름

```
Human Logic
→ Source Code
→ Compiler
→ Runtime
→ System Call
→ Kernel
→ Hardware Execution
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

프로그래머의 설계 결과는 시스템 전체에서 관측됩니다.

### CPU 사용률

```bash
top
htop
pidstat
perf
```

### 메모리 사용

```bash
free -h
vmstat
pmap
```

### I/O 패턴

```bash
iostat
iotop
blktrace
```

### 네트워크 패턴

```bash
ss
netstat
tcpdump
iftop
```

### 시스템 콜 추적

```bash
strace
```

### 런타임 상태

```bash
jstack
jcmd
jmap
```

### Kubernetes

프로그래머 설계 결과는 다음 형태로 나타납니다.

- pod restart
- OOMKill
- CPU throttle
- network retry
- readiness failure

```bash
kubectl top pod
kubectl describe pod
kubectl logs
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*