# 바이트코드 (Bytecode)

> 정독: 0회

## 1. 이 기술이 무엇인가

바이트코드(**virtual-machine-oriented intermediate instruction representation**)는:

> 고급 소스 언어를 직접 CPU 기계어로 만들기 전에, 가상 머신이 이해할 수 있는 공통 명령 체계로 변환한 중간 실행 표현

### 핵심 특징

| 특징 | 설명 |
|------|------|
| platform-independent | 플랫폼 독립 |
| instruction-based | 명령어 기반 |
| binary encoded | 바이너리 인코딩 |
| VM-executable | 가상 머신 실행 가능 |
| intermediate form | 중간 표현 |

> 핵심 정의: **portable executable intermediate instruction set**

---

## 2. 시스템 어디에서 등장하는가

### 등장 위치

| 위치 | 역할 |
|------|------|
| compiler backend | bytecode generation |
| executable artifact | 정적 저장 |
| loader | memory loading |
| method/code area | runtime residency |
| execution engine | interpretation/JIT |
| verifier | safety validation |

### 전체 흐름

```
source code
→ compiler
→ bytecode
→ runtime loading
→ execution engine
→ native execution
```

> 즉 바이트코드는 **execution-engine input abstraction layer**입니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접 영향: **CPU + Memory**

| 자원 | 영향 |
|------|------|
| CPU | interpretation/JIT |
| Memory | code residency |
| Cache | instruction locality |
| Disk | executable loading |
| Branch Prediction | control-flow efficiency |

특히 중요한 것은 **instruction execution overhead**입니다.

바이트코드는 직접 실행되지 않고 반드시 다음 중 하나를 거쳐야 합니다:

- 해석(interpreted)되거나
- 네이티브 코드로 변환(compiled)

---

## 4. 왜 중요한가

핵심 목적: **platform-neutral executable representation**

바이트코드가 없다면 다음 각각에 대해 컴파일 결과물을 따로 생성해야 합니다:

- CPU 아키텍처마다
- 운영체제마다

바이트코드는 **hardware-independent execution contract**를 제공합니다.

즉 다음 사이의 공통 인터페이스입니다:

- compiler
- runtime
- execution engine

또한 **runtime optimization compatibility**도 매우 중요합니다. JIT 최적화는 대부분 **bytecode-level profiling**에서 시작됩니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 대표 장애

| 장애 | 설명 |
|------|------|
| malformed bytecode | verification 실패 |
| invalid opcode | runtime crash |
| excessive interpretation | CPU 과부하 |
| huge code generation | memory 증가 |
| unstable hot paths | JIT thrashing |
| bytecode verification overhead | startup latency |
| dynamic loading explosion | metaspace/code cache 증가 |

특히 중요한 것은 **runtime translation overhead**입니다.

바이트코드는 직접 하드웨어가 실행하지 않으므로 다음 비용이 존재합니다:

- interpretation
- JIT compilation
- optimization
- deoptimization

또한 **dynamic code loading pressure**도 중요합니다.

대규모 시스템에서는 다음으로 인해 바이트코드 관련 메모리 사용량이 크게 증가할 수 있습니다:

- class/module loading
- reflection
- dynamic proxy
- runtime code generation

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Opcode-Based Structure

바이트코드는 **opcode-oriented instruction representation**입니다.

기본 구조: `opcode + operands`

| 구성 | 역할 |
|------|------|
| opcode | 수행할 연산 |
| operand | 추가 데이터 |
| constant reference | 외부 참조 |
| branch offset | 제어 흐름 |

### Stack-Oriented Execution

많은 VM 바이트코드는 **stack-machine execution model**을 사용합니다.

```
push → pop → arithmetic → invoke
```

### Intermediate Representation

바이트코드는 **runtime-friendly intermediate form**입니다. 소스 언어와 하드웨어 사이의 **translation boundary layer** 역할을 합니다.

### Verification

실행 전 **bytecode verification**이 수행됩니다.

| 항목 | 의미 |
|------|------|
| type safety | 타입 무결성 |
| stack consistency | 스택 상태 |
| legal control flow | 점프 안전성 |
| opcode validity | 명령 유효성 |

### Runtime Interpretation

인터프리터는 **instruction-by-instruction execution**을 수행합니다.

```
opcode fetch → decode → execute
```

위 사이클을 반복합니다.

### JIT Compilation

JIT는 **hot bytecode regions**을 탐지하여 **optimized native machine code**로 변환합니다.

### Symbolic Reference Resolution

바이트코드는 보통 **symbolic linking model**을 사용합니다.

즉 실제 메모리 주소 대신 다음을 참조합니다:

- class name
- method name
- field identifier

런타임 중 **dynamic symbolic resolution**이 발생합니다.

### Runtime Memory Interaction

바이트코드의 객체 생성 명령은 **runtime heap allocation**으로 연결됩니다:

- heap allocation
- object header setup
- metadata initialization
- memory residency

### Control Flow Representation

바이트코드는 **low-level control flow encoding**을 포함합니다:

- branch
- jump
- loop
- return
- exception transfer

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

대표 관측 도구: `perf`, `top`, `pidstat`, `vmstat`, `strace`

| 현상 | 의미 |
|------|------|
| high CPU | interpretation/JIT |
| cache miss | poor instruction locality |
| branch miss | control-flow complexity |
| startup delay | verification/loading |
| code cache growth | native translation 증가 |

### Runtime

| 항목 | 의미 |
|------|------|
| loaded classes/modules | bytecode loading 규모 |
| interpreted methods | 인터프리터 부하 |
| JIT compilation count | native translation |
| deoptimization count | unstable optimization |
| code cache usage | generated native code |

> 중요 항목: **runtime code translation activity**

### Kubernetes

| 현상 | 원인 |
|------|------|
| startup latency | verification/JIT |
| CPU burst | runtime compilation |
| pod warmup delay | bytecode interpretation |
| autoscaling jitter | delayed optimization |
| memory growth | metadata/code cache |

특히 중요한 것은 **cold-start execution overhead**입니다.

초기 구동 시 다음이 동시에 수행됩니다:

- bytecode loading
- verification
- interpretation
- profiling
- JIT compilation

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*