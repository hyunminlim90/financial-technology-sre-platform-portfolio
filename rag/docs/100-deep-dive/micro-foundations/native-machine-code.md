# 네이티브 기계어 코드 (Native Machine Code)

> 정독: 0회

## 1. 이 기술이 무엇인가

네이티브 기계어 코드는:

> 실제 CPU가 직접 실행 가능한 **최하단 이진 명령어 집합**

### 핵심 특징

| 특징 | 설명 |
|---|---|
| hardware-specific | CPU 종속 |
| binary encoded | 이진 인코딩 |
| directly executable | 직접 실행 가능 |
| ISA-dependent | ISA 의존 |
| lowest executable form | 최하단 실행 형태 |

### 대표 ISA

| ISA | 플랫폼 |
|---|---|
| x86-64 | Intel / AMD |
| ARM64 (AArch64) | ARM |
| RISC-V | Open ISA |
| POWER | Enterprise Systems |

> **핵심 정의:** final hardware-executable instruction representation

---

## 2. 시스템 어디에서 등장하는가

| 위치 | 역할 |
|---|---|
| ahead-of-time compiler | native executable generation |
| JIT compiler | runtime native generation |
| kernel execution | CPU instruction dispatch |
| code cache | generated native storage |
| process memory | executable code pages |

### 전체 흐름

```
source code → IR/bytecode → native machine code → CPU execution
```

네이티브 코드는 **CPU-visible executable layer**입니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접 영향: **CPU + Instruction Cache + Memory**

| 자원 | 영향 |
|---|---|
| CPU | instruction execution |
| L1/L2 cache | instruction locality |
| Memory | executable pages |
| TLB | address translation |
| Branch predictor | control flow prediction |

네이티브 코드는 pipeline utilization, branch prediction, cache hit ratio, register usage에 직접 영향을 주므로 **instruction-level execution efficiency**가 특히 중요합니다.

---

## 4. 왜 중요한가

네이티브 코드는 **the only form directly understood by physical CPUs**입니다.

CPU는 source code, bytecode, IR, AST를 직접 실행하지 못하며, 오직 **ISA-compliant machine instructions**만 실행 가능합니다.

동일한 로직이라도 네이티브 코드 품질에 따라 결과가 달라집니다:

| 상태 | 결과 |
|---|---|
| optimized native code | high throughput |
| poor native generation | cache miss / branch stall |
| unstable native code | latency jitter |

---

## 5. 실제 장애와 어떤 관련이 있는가

| 문제 | 설명 |
|---|---|
| bad branch prediction | CPU stall |
| instruction cache miss | 성능 저하 |
| excessive code generation | memory pressure |
| poor register allocation | CPU 효율 감소 |
| deoptimization fallback | latency spike |
| incompatible ISA build | 실행 실패 |
| code cache exhaustion | runtime instability |

네이티브 코드는 CPU pipeline 구조와 직접 연결되므로 **CPU pipeline inefficiency**가 특히 중요합니다. branch misprediction, pipeline flush, cache refill, execution stall이 발생할 수 있습니다.

JIT 환경에서는 recompilation, speculative optimization, deoptimization이 반복되는 **runtime-generated code instability**도 중요합니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### ISA Dependency

네이티브 코드는 **strictly tied to physical ISA**입니다:

| CPU | 실행 가능 여부 |
|---|---|
| x86 code on x86 CPU | 가능 |
| ARM code on x86 CPU | 불가능 |
| RISC-V code on ARM CPU | 불가능 |

### Binary Instruction Encoding

네이티브 코드는 **binary-encoded hardware instructions**입니다:

| 구성 | 설명 |
|---|---|
| opcode | 연산 종류 |
| register specifier | 레지스터 대상 |
| immediate value | 즉시 값 |
| memory addressing mode | 메모리 접근 방식 |

### Instruction Decoder

CPU 내부에서는 machine code → micro-operations 변환이 발생합니다:

| 단계 | 설명 |
|---|---|
| fetch | instruction fetch |
| decode | instruction decode |
| dispatch | execution scheduling |
| execute | ALU/FPU execution |
| retire | commit |

### Register Allocation

네이티브 코드 생성의 핵심은 **mapping logical variables to physical registers**입니다. 레지스터 부족 시 spilling, stack usage 증가, memory traffic 증가가 발생합니다.

### Memory Addressing

네이티브 코드는 **real memory addressing operations**을 수행합니다. heap access, stack access, pointer arithmetic, offset calculation 등이 실제 주소 연산으로 변환됩니다.

### Executable Memory Pages

네이티브 코드는 보통 **executable memory regions**에 저장됩니다:

| 속성 | 설명 |
|---|---|
| RX permission | executable/readable |
| page-aligned | page boundary alignment |
| instruction cached | I-cache 대상 |

### Calling Convention

네이티브 코드는 반드시 **platform ABI rules**를 따라야 합니다:

| 항목 | 설명 |
|---|---|
| argument passing | 인자 전달 |
| stack layout | 스택 구조 |
| return registers | 반환 규칙 |
| caller/callee save | 레지스터 보존 |

### Native Code Generation

JIT/AOT는 IR → target ISA lowering을 수행합니다. 포함 단계: instruction selection, scheduling, peephole optimization, register allocation

### Runtime Interaction

네이티브 코드는 **hardware state mutation**을 직접 수행합니다: register state, cache state, memory contents, synchronization primitives를 직접 변경합니다.

### Memory Allocation Path

객체 생성 시 **native allocation routines**가 호출됩니다: heap pointer movement, metadata initialization, alignment enforcement, memory zeroing

### Synchronization Instructions

멀티스레드 환경에서는 **atomic hardware instructions**가 중요합니다:

| 명령 | 목적 |
|---|---|
| compare-and-swap | atomic update |
| memory fence | ordering guarantee |
| atomic increment | synchronization |

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**대표 도구:** `perf`, `perf top`, `perf record`, `objdump`, `readelf`, `gdb`

| 항목 | 의미 |
|---|---|
| instruction cycles | CPU 사용 |
| branch misses | branch prediction 실패 |
| cache misses | locality 문제 |
| stalled cycles | execution bottleneck |
| retired instructions | 실제 실행량 |

> 특히 중요: **hardware performance counters**

### Runtime

| 메트릭 | 의미 |
|---|---|
| compiled code size | generated native code |
| code cache usage | executable memory |
| deoptimization count | rollback 발생 |
| recompilation frequency | runtime churn |

### Kubernetes

| 현상 | 원인 |
|---|---|
| CPU spike | native recompilation |
| latency jitter | deoptimization |
| pod warmup delay | JIT stabilization |
| memory increase | code cache |
| noisy neighbor effects | cache contention |

멀티테넌트 환경에서는 cache contention, branch predictor pollution, NUMA locality 문제 등이 발생할 수 있으므로 **CPU microarchitectural contention**이 특히 중요합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*