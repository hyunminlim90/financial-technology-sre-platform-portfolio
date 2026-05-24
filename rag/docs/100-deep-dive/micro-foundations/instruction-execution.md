# 인스트럭션 익스큐션 (Instruction Execution)

> 정독: 0회

## 1. 이 기술이 무엇인가

인스트럭션 익스큐션은:

> 디코드(Decode)된 명령어를 실제 하드웨어 연산으로 수행하여 **시스템 상태를 변경하는 실행 단계**

| 항목 | 설명 |
|------|------|
| operation execution | 실제 연산 수행 |
| state transition | 시스템 상태 변화 |
| ALU activation | 산술논리장치 동작 |
| register update | 레지스터 값 변경 |
| memory modification | 메모리 상태 변경 |

> 명령어의 논리적 의미가 실제 CPU 연산으로 **물질화되는 단계**입니다.

---

## 2. 시스템 어디에서 등장하는가

**기본 위치:**

```
Fetch → Decode → Execute
```

**실행 계층:**

| 계층 | 역할 |
|------|------|
| CPU Core | native instruction execution |
| ALU/FPU | arithmetic execution |
| Runtime Engine | virtual instruction execution |
| Virtual Machine | bytecode execution |
| Operating System | system call execution |

> 모든 실행 시스템의 **중심 단계**입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

### CPU (가장 직접적)

instruction execution은 실제 연산 단계입니다. 관련 요소:

- ALU / FPU
- execution units
- scheduler
- reorder buffer

### Memory (매우 강하게 연결)

instruction execution은 대부분 **load/store operation**을 동반합니다:

- stack update
- heap access
- cache interaction
- pointer dereference

### Disk (간접적)

- file I/O
- paging
- storage syscall

### Network (I/O execution 단계에서 연결)

- socket send
- packet processing
- network syscall

---

## 4. 왜 중요한가

**실제 계산이 수행되는 단계**이기 때문입니다.

fetch/decode만 있고 execution이 없으면:

- 상태 변화 없음
- 계산 결과 없음
- 시스템 동작 없음

| 항목 | 영향 |
|------|------|
| throughput | 처리량 |
| latency | 응답 속도 |
| CPU efficiency | 연산 효율 |
| parallelism | 병렬성 |
| performance | 시스템 성능 |

> 실행 시스템의 **본질 자체**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

**대표 사례:**

| 장애 | 원인 |
|------|------|
| CPU saturation | excessive execution |
| execution stall | dependency wait |
| pipeline flush | wrong speculation |
| lock contention | synchronized execution |
| cache miss latency | memory access delay |
| thermal throttling | excessive execution load |
| deadlock | execution dependency cycle |

특히 다음 환경에서 중요합니다:

- high concurrency
- distributed runtime
- virtualization
- container environment

---

## 6. 핵심 메커니즘

### (1) Execute는 실제 상태 변경 단계

예시: `a = a + 1` 실행 시

```
register load → ALU add → register write-back
```

> 메모리/레지스터 값이 **실제 변경**됩니다.

### (2) ALU 중심 동작

대부분의 instruction execution은 **ALU (Arithmetic Logic Unit)**를 사용합니다.

대표 연산: `add`, `sub`, `and`, `or`, `shift`, `compare`

### (3) Memory Access Execution

실행 단계는 종종 **memory read/write**를 포함합니다:

- variable load
- object access
- stack operation
- heap update

### (4) Register Write-Back

execution 결과는 최종적으로 **register 또는 memory**에 기록됩니다.

이 단계가 완료되어야 다음 instruction이 결과를 사용할 수 있습니다.

### (5) Dependency 기반 실행 제한

```
Instruction B  depends on  Instruction A result
→ A 완료 전까지 B 실행 불가
```

> **execution dependency**가 성능을 제한합니다.

### (6) Pipeline Execution

현대 CPU는 multiple instructions를 동시에 실행합니다:

- superscalar
- out-of-order execution
- speculative execution

### (7) Virtual Machine Execution

JVM 환경에서는 다음 형태로 수행됩니다:

```
bytecode execution → native execution
```

VM execution engine이 native operation으로 변환합니다.

### (8) Branch Execution

분기 명령(`if`, `loop`, `jump`, `call`, `return`) 실행 시 발생 가능한 것들:

- PC 변경
- pipeline redirect
- speculative path flush

### (9) Exception/Trap 처리

실행 중 다음이 발생할 수 있습니다:

- divide by zero
- invalid memory access
- page fault

이 경우:

```
execution → interrupt/trap handler
```

로 전환됩니다.

---

## 7. Linux / Runtime / K8s에서 관측 방법

### Linux

```bash
perf stat
perf top
```

**관측 지표:** cycles, instructions, IPC, stalls, branch-misses

### Runtime 도구

| 도구 | 역할 |
|------|------|
| perf | CPU execution profiling |
| async-profiler | hotspot execution |
| flame graph | execution path |
| JFR | runtime execution analysis |

### Kubernetes

| 현상 | 의미 |
|------|------|
| CPU throttling | execution 제한 |
| pod latency 증가 | execution bottleneck |
| low throughput | execution stall |
| high context switching | execution overhead |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*