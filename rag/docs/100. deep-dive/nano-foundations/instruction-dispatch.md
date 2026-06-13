# 인스트럭션 디스패치 (Instruction Dispatch)

> 정독: 0회

## 1. 이 기술이 무엇인가

인스트럭션 디스패치는:

> 실행 엔진이 명령어를 읽고, 해석하고, 실제 연산 경로로 전달하여 CPU 실행 흐름을 제어하는 과정

| 단계 | 역할 |
|------|------|
| Fetch | 명령어 가져오기 |
| Decode | 의미 해석 |
| Dispatch | 실행 경로 전달 |
| Execute | 실제 연산 수행 |

> 디스패치는 "다음 명령을 어떤 실행 경로로 처리할지 결정하고 전달하는 **실행 제어 메커니즘**"입니다.

---

## 2. 시스템 어디에서 등장하는가

인스트럭션 디스패치는 거의 모든 실행 시스템에 존재합니다.

**위치:**

```
Program
→ Instruction Stream
→ Execution Engine
→ Dispatch Loop
→ CPU Execution Unit
```

**등장 계층:**

| 계층 | 역할 |
|------|------|
| CPU | machine instruction dispatch |
| JVM | bytecode dispatch |
| Interpreter | opcode dispatch |
| OS Scheduler | thread dispatch |
| GPU Runtime | kernel dispatch |

> 디스패치는 "실행 흐름 제어"가 존재하는 **모든 시스템**에 존재합니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

### CPU (가장 직접적)

다음이 모두 CPU 중심입니다:

- instruction execution
- branch prediction
- pipeline utilization
- execution unit selection

### Memory (매우 중요)

명령 디스패치 과정에서 다음에 접근이 발생합니다:

- instruction cache
- stack
- runtime metadata
- opcode table

### Disk (간접 영향)

- executable loading
- class loading
- paging

### Network (직접 영향 거의 없음)

다만 distributed runtime, remote execution에서는 간접 연결됩니다.

---

## 4. 왜 중요한가

**실행 성능과 CPU 효율을 결정하는 핵심 메커니즘**입니다.

| 영역 | 영향 |
|------|------|
| 실행 속도 | instruction throughput |
| CPU 효율 | pipeline utilization |
| latency | dispatch overhead |
| runtime optimization | JIT/inlining |
| branch efficiency | prediction success |
| power efficiency | execution scheduling |

> 디스패치 효율이 곧 런타임 성능입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

**대표 사례:**

| 장애 | 원인 |
|------|------|
| CPU Spike | inefficient dispatch loop |
| branch misprediction | excessive branching |
| interpreter bottleneck | repeated opcode decode |
| context switching overhead | excessive dispatch |
| JIT warmup delay | interpreted execution 지속 |
| instruction cache miss | poor execution locality |

특히 다음 환경에서 매우 중요합니다:

- high-frequency trading
- low-latency runtime
- JVM runtime
- database engine

---

## 6. 핵심 메커니즘

### (1) Fetch → Decode → Dispatch → Execute

```
Instruction Fetch → Decode → Dispatch → Execute → Next Instruction
```

이 루프가 **프로그램 실행의 본질**입니다.

### (2) Instruction Pointer 기반 실행

현재 실행 위치는 **PC Register / Instruction Pointer**가 관리합니다.

디스패치는 "다음 명령 위치"를 기준으로 계속 진행됩니다.

### (3) Decode 단계

명령어 의미를 해석합니다.

| Opcode | 의미 |
|--------|------|
| add | 덧셈 |
| load | 메모리 로드 |
| jump | 흐름 이동 |
| call | 함수 호출 |

Decode 결과에 따라 실행 경로가 달라집니다.

### (4) Dispatch 단계

해석된 명령을 실제 실행 유닛으로 전달합니다:

- ALU
- FPU
- Load/Store Unit
- Branch Unit

> 디스패치는 **실행 유닛 선택 과정**입니다.

### (5) Interpreter Dispatch

가상 머신에서는 바이트코드 디스패치가 발생합니다.

```
Bytecode → Opcode Decode → Runtime Handler → Native Execution
```

JVM은 바이트코드를 dispatch합니다.

### (6) JIT 최적화

JIT는 반복 디스패치 비용을 줄입니다.

**기존:**

```
Fetch → Decode → Dispatch → Execute  (반복)
```

**최적화 후:**

```
Native Code Direct Execution
```

> 디스패치 비용 제거가 핵심 목적 중 하나입니다.

### (7) Branch Prediction과 연결

현대 CPU는 다음 명령을 예측합니다. 예측 실패 시:

- pipeline flush
- execution stall

이 발생합니다. 디스패치 구조는 **CPU pipeline과 직접 연결**됩니다.

### (8) Context Switch와 연결

프로세스/스레드 전환도 dispatch 문제입니다.

OS Scheduler는 다음을 dispatch합니다:

- 어떤 thread를
- 어느 CPU core에
- 언제 실행할지

---

## 7. Linux / Runtime / K8s에서 관측 방법

### Linux

```bash
perf top
perf record
vmstat 1
```

**관측 항목:** context switch, CPU stall, branch miss, instruction rate

### Runtime 도구

| 도구 | 역할 |
|------|------|
| profiler | hotspot 분석 |
| JFR | runtime dispatch |
| flame graph | execution path |
| perfasm | native dispatch |

### Kubernetes

| 현상 | 의미 |
|------|------|
| CPU throttling | dispatch delay |
| latency spike | execution stall |
| high context switch | scheduler pressure |
| pod slowdown | runtime contention |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*