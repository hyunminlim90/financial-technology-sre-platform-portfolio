# 중앙 처리 장치 (Central Processing Unit, CPU)

> 정독: 0회

## 1. 이 기술이 무엇인가

CPU는:

> 프로그램의 명령어를 실제로 실행하는 **하드웨어 연산 장치**

**핵심 역할:**

- 명령어 읽기
- 명령어 해석
- 연산 수행
- 메모리 접근 제어
- 실행 흐름 제어

```
Software Logic → Instruction → CPU Execution → Physical State Change
```

**대표 구성 요소:**

| 구성 요소 | 역할 |
|----------|------|
| ALU | 산술/논리 연산 |
| Control Unit | 명령 흐름 제어 |
| Register | 초고속 임시 저장 |
| Cache | CPU 내부 고속 메모리 |
| Core | 독립 실행 유닛 |

---

## 2. 시스템 어디에서 등장하는가

CPU는 **모든 실행 시스템의 중심**입니다.

| 영역 | CPU 역할 |
|------|----------|
| OS Kernel | syscall 처리 |
| Runtime | instruction execution |
| Database | query execution |
| Network Stack | packet processing |
| Kubernetes | scheduling/control |
| JVM/VM | bytecode/native execution |

**프로그램 실행 흐름:**

```
Application → Runtime → OS → CPU → Memory / Device
```

> 최종 연산은 반드시 **CPU에서 수행**됩니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

### CPU 자체

대표 영향 요소:

- core count
- clock speed
- IPC
- cache size
- branch prediction

### Memory (매우 중요)

CPU는 메모리 접근 속도에 크게 영향받습니다. 대표 병목:

- cache miss
- memory latency
- NUMA latency

> 현대 시스템은 CPU 자체보다 **memory stall 문제 비중**이 큽니다.

### Network / Disk

I/O 처리 시 CPU 사용 증가 발생:

- packet parsing
- interrupt handling
- encryption / compression

---

## 4. 왜 중요한가

모든 소프트웨어는 결국 **CPU 시간(CPU Time)을 소비**합니다.

> 모든 추상화의 최종 실행 지점 = **CPU**

| 이유 | 설명 |
|------|------|
| 성능 | 처리량 결정 |
| latency | 응답 속도 결정 |
| concurrency | 동시 처리량 결정 |
| scheduling | 실행 순서 결정 |
| runtime stability | overload 영향 |

> CPU saturation은 **전체 서비스 장애**로 이어질 수 있습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

**대표 사례:**

| 장애 | 원인 |
|------|------|
| CPU 100% | infinite loop |
| load average 폭증 | runnable thread accumulation |
| latency 증가 | CPU starvation |
| GC stall | excessive CPU consumption |
| context switching overload | too many threads |
| packet loss | NIC interrupt overload |
| scheduler delay | CPU contention |

**대표 장애 패턴:**

```
CPU saturation
→ scheduler delay
→ thread waiting
→ latency increase
→ timeout
→ retry storm
→ cascading failure
```

---

## 6. 핵심 메커니즘

### (1) Fetch → Decode → Execute

CPU 기본 실행 사이클입니다:

```
Fetch → Decode → Execute  (반복)
```

### (2) Register

CPU 내부 초고속 저장 공간입니다:

- RAM보다 훨씬 빠름
- 현재 연산 상태 저장
- 함수 호출/연산 중간값 유지

### (3) Cache

CPU와 RAM 속도 차이를 완화합니다.

| Cache | 특징 |
|-------|------|
| L1 | 매우 빠름 |
| L2 | 중간 |
| L3 | 여러 코어 공유 |

### (4) Context Switching

CPU가 실행 대상을 바꾸는 과정입니다. 비용 발생 요소:

- register save/restore
- cache invalidation
- scheduler overhead

### (5) Interrupt

외부 이벤트 발생 시 CPU 흐름을 중단 후 처리합니다:

- network packet
- disk completion
- timer event

### (6) Branch Prediction

CPU 성능 핵심 최적화입니다. 잘못 예측 시:

```
pipeline flush → performance penalty
```

### (7) Multi-core / SMT

현대 CPU는 다음 기반으로 동작합니다:

- multiple cores
- hyper-threading
- simultaneous execution

> CPU 하나가 아니라 **여러 실행 흐름을 병렬 처리**합니다.

---

## 7. Linux / Runtime / K8s에서 관측 방법

### Linux

```bash
top
htop
mpstat
vmstat
sar
pidstat
perf
```

**핵심 지표:**

| 지표 | 의미 |
|------|------|
| us | user cpu |
| sy | kernel cpu |
| wa | iowait |
| cs | context switch |
| run queue | 실행 대기 thread |
| load average | runnable accumulation |

### Runtime

관측 대상:

- thread contention
- scheduler delay
- GC CPU usage
- JIT compilation overhead
- lock contention

### Kubernetes

**관측 항목:** cpu request, cpu limit, throttling, node pressure, container cpu usage

```bash
kubectl top
kubectl describe node
kubectl describe pod
```

> **CPU throttling**은 container runtime 환경에서 매우 흔한 성능 문제입니다.