# 명령어 실행 사이클 (Instruction Execution Cycle)

> 정독: 0회

## 1. 이 기술이 무엇인가

명령어 실행 사이클은:

> CPU가 명령어를 메모리에서 가져오고, 해석하고, 실제 연산하고, 결과를 저장하는 **최하단 하드웨어 실행 루프**

### 핵심 특징

| 특징 | 설명 |
|---|---|
| hardware-driven | 물리 CPU 중심 |
| clock synchronized | 클록 기반 |
| instruction-oriented | 명령 단위 실행 |
| state-transforming | 시스템 상태 변경 |
| continuous repetition | 지속 반복 |

> **핵심 정의:** fundamental CPU execution loop

---

## 2. 시스템 어디에서 등장하는가

| 위치 | 역할 |
|---|---|
| physical CPU core | instruction execution |
| pipeline stages | execution flow |
| register subsystem | operand storage |
| memory subsystem | data fetch/store |
| cache hierarchy | instruction/data acceleration |

### 전체 흐름

```
native machine code
  → instruction fetch
  → decode
  → execute
  → memory access
  → write-back
```

명령어 실행 사이클은 **final physical execution layer of all software**입니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접 영향: **CPU + Memory subsystem**

| 자원 | 영향 |
|---|---|
| CPU core | instruction execution |
| Registers | operand/state storage |
| L1/L2/L3 cache | instruction/data locality |
| RAM | memory access |
| Bus | data transfer |
| Branch predictor | control flow efficiency |

실제 성능은 instruction throughput, cache hit ratio, branch prediction accuracy, memory latency에 크게 좌우되므로 **CPU pipeline efficiency**가 특히 중요합니다.

---

## 4. 왜 중요한가

모든 소프트웨어는 결국 **instruction execution cycles**로 환원됩니다. 아무리 고급 추상화라도 최종적으로는 register operation, ALU computation, memory access, branch control으로 분해됩니다.

> **all computation ultimately becomes hardware instruction cycles**

CPU 성능은 사실상 **how efficiently instruction cycles are utilized** 문제입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

| 문제 | 설명 |
|---|---|
| pipeline stall | 실행 중단 |
| cache miss | 메모리 지연 |
| branch misprediction | pipeline flush |
| memory bottleneck | RAM 대기 증가 |
| instruction starvation | fetch 지연 |
| register pressure | spilling 증가 |
| CPU throttling | clock 감소 |

CPU는 매우 빠르지만 RAM은 상대적으로 느립니다. cache miss, NUMA remote access, random memory access 등이 발생하면 CPU가 대기 상태에 빠지는 **pipeline stalls caused by memory latency**가 특히 중요합니다.

분기 예측 실패 시 speculative execution 폐기, pipeline flush, instruction refill이 발생하는 **branch misprediction penalties**도 중요합니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Fetch

CPU는 **Program Counter(PC) / Instruction Pointer(IP)**가 가리키는 주소에서 명령어를 가져옵니다:

| 위치 | 속도 |
|---|---|
| L1 instruction cache | 매우 빠름 |
| L2/L3 cache | 중간 |
| RAM | 느림 |

### Decode

가져온 이진 명령어를 **hardware-understandable micro-operations**으로 변환합니다:

| 구성 | 역할 |
|---|---|
| instruction decoder | opcode 해석 |
| control unit | 제어 신호 생성 |
| microcode engine | 복합 명령 처리 |

### Execute

**ALU/FPU/register execution** 실제 연산 단계입니다:

| 연산 | 예시 |
|---|---|
| arithmetic | add/subtract |
| logical | and/or/xor |
| floating point | FP operations |
| vector operations | SIMD |
| address calculation | memory offset |

### Memory Access

필요 시 **load/store operations**이 발생합니다: heap access, stack access, pointer dereference, cache interaction

### Write-back

최종 결과를 **architectural state**에 저장합니다: register update, memory update, flag update, cache coherence interaction

### CPU Pipeline

현대 CPU는 단일 명령 순차 처리 대신 **instruction pipelining**을 사용합니다:

| 단계 | 설명 |
|---|---|
| Fetch | 명령 인입 |
| Decode | 해석 |
| Execute | 연산 |
| Memory | 메모리 접근 |
| Write-back | 결과 반영 |

동시에 여러 명령이 각 단계에서 병렬 처리됩니다.

### Superscalar Execution

현대 CPU는 **multiple instructions per clock cycle**이 가능합니다: multiple ALUs, multiple execution ports, out-of-order execution

### Out-of-Order Execution

**independent instructions executed before earlier stalled instructions** 메커니즘으로 pipeline utilization 증가, stall 감소, throughput 증가 효과를 가집니다.

### Branch Prediction

분기문은 pipeline 방해 요소입니다. CPU는 **predict future execution path**를 시도하며, 실패 시 pipeline flush, speculative rollback, latency 증가가 발생합니다.

### Speculative Execution

CPU는 미래 경로를 추측해 **execute instructions before certainty**합니다. 정확하면 성능 증가, 실패하면 rollback이 발생합니다.

### Cache Hierarchy

| 계층 | 속도 |
|---|---|
| L1 | fastest |
| L2 | fast |
| L3 | shared |
| RAM | slow |

### Clock Synchronization

모든 execution cycle은 **clock-driven state transitions**입니다. transistor switching, register latching, bus synchronization이 clock edge 기준으로 수행됩니다.

### Memory Ordering

멀티코어 환경에서는 **instruction visibility ordering**이 중요합니다: memory fence, cache coherence, atomic operations

### Physical State Mutation

Instruction cycle 결과는 **real hardware state mutation**입니다. 실제로 transistor switching, capacitor charging, register bit transitions, cache line mutation이 발생합니다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**대표 도구:** `perf`, `perf stat`, `perf top`, `vmstat`, `mpstat`

| 항목 | 의미 |
|---|---|
| cycles | CPU cycles |
| instructions | executed instructions |
| IPC | instructions per cycle |
| branch misses | prediction failure |
| cache misses | memory bottleneck |
| stalled cycles | pipeline waiting |

> 특히 중요: **IPC (Instructions Per Cycle)** — CPU 효율 핵심 지표

### Runtime

| 현상 | 의미 |
|---|---|
| high allocation rate | memory traffic 증가 |
| synchronization overhead | fence/stall 증가 |
| GC pause | pipeline interruption |
| deoptimization | instruction churn |

### Kubernetes

| 현상 | 원인 |
|---|---|
| CPU throttling | cycle 제한 |
| noisy neighbor | cache contention |
| NUMA imbalance | remote memory latency |
| tail latency spike | memory stall |
| pod instability | CPU starvation |

멀티테넌트 환경에서는 cache contention, memory bandwidth competition, branch predictor pollution이 발생할 수 있으므로 **microarchitectural resource contention**이 특히 중요합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*