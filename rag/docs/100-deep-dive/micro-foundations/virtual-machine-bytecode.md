# 가상 머신 바이트코드 (Virtual Machine Bytecode)

> 정독: 0회

## 1. 이 기술이 무엇인가

가상 머신 바이트코드(**virtual-machine executable intermediate instruction format**)는:

> 고급 언어 소스코드, 컴파일 결과물, 실행 엔진 입력 데이터 사이의 중간 실행 표현

### 핵심 특징

| 특징 | 설명 |
|---|---|
| platform independent | CPU 독립 |
| virtual instruction set | 가상 명령 체계 |
| runtime executable | 실행 가능 |
| compact binary format | 압축된 바이너리 |
| VM-oriented | 가상 머신 전용 |

즉, 바이트코드는 **hardware-independent executable instruction stream**입니다.

<details>
<summary>Deep Dive</summary></br>

High-Level Source Language(고급 소스 언어) [[M]](../../100-deep-dive/micro-foundations/high-level-source-language.md)  
Virtual Machine Executable File(가상 머신 실행 파일) [[M]](../../100-deep-dive/micro-foundations/virtual-machine-executable-file.md)  
Bytecode Stream(바이트코드 스트림) [[M]](../../100-deep-dive/micro-foundations/bytecode-stream.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

### 등장 위치

| 위치 | 역할 |
|---|---|
| compiler output | 컴파일 결과 |
| class/module file | 저장 포맷 |
| method area | 메모리 적재 |
| execution engine | 실행 입력 |
| interpreter | 명령 해석 |
| JIT compiler | native code 생성 |

### 전체 흐름

```
source code → compiler → bytecode → execution engine → native machine code → CPU execution
```

즉, 바이트코드는 **execution-engine input language**입니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **CPU + Memory**입니다.

| 자원 | 영향 |
|---|---|
| CPU | instruction decoding |
| Memory | code loading |
| Cache | instruction locality |
| Disk | class/module loading |
| Network | distributed code delivery |

특히 **instruction execution efficiency**가 중요합니다. 바이트코드 구조는 JIT 최적화, branch prediction, cache locality, instruction dispatch에 직접 영향합니다.

---

## 4. 왜 중요한가

핵심 목적은 **platform abstraction**입니다. Intel, AMD, ARM, RISC-V 같은 서로 다른 CPU 구조 차이를 숨겨 **single compilation, multiple platform execution**이 가능합니다.

또한 바이트코드는 **runtime optimization substrate** 역할도 수행합니다. 실행 엔진은 바이트코드를 분석하여 다음을 수행합니다.

- JIT optimization
- inlining
- dead code elimination
- speculative optimization

---

## 5. 실제 장애와 어떤 관련이 있는가

### 대표 장애

| 장애 | 설명 |
|---|---|
| invalid bytecode | 실행 실패 |
| verification failure | 로딩 거부 |
| excessive interpretation | CPU 증가 |
| JIT thrashing | 재컴파일 폭증 |
| code cache exhaustion | native cache 부족 |
| malformed instruction stream | 런타임 오류 |
| class loading storm | startup latency |

대표 현상은 **runtime instruction instability**입니다.

특히 **hotspot amplification**이 중요합니다. 반복 실행되는 바이트코드가 많아지면 다음이 발생합니다.

- JIT activity 증가
- native compilation 증가
- CPU burst 증가

또한 **verification overhead**도 중요합니다. 가상 머신은 실행 전 type safety, stack consistency, instruction validity를 검사합니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Opcode Structure

바이트코드는 `opcode + operands` 구조입니다. 무엇을 수행할지, 어떤 데이터를 사용할지를 바이너리 형태로 표현합니다.

### Stack-Based Execution

많은 VM 바이트코드는 **stack-oriented execution model**을 사용합니다.

```
push → operate → pop
```

이 구조는 instruction compactness, portability, interpreter simplicity를 제공합니다.

### Bytecode Verification

실행 전 **runtime safety verification**이 수행됩니다.

| 항목 | 의미 |
|---|---|
| type validity | 타입 안전 |
| stack integrity | 스택 무결성 |
| branch validity | 점프 검증 |
| instruction legality | 명령 적합성 |

즉, **unsafe execution prevention** 메커니즘입니다.

### Instruction Dispatch

실행 엔진은 **opcode dispatch loop**를 수행합니다.

```
fetch opcode → decode → execute → advance program counter
```

### Intermediate Representation (IR)

JIT 환경에서는 다음 변환이 일어납니다.

```
bytecode → IR → native machine code
```

IR은 **optimization-friendly internal representation**입니다.

### Native Compilation

핫스팟 감지 후 **runtime native code emission**이 수행됩니다. 즉, 바이트코드는 최종적으로 **CPU-executable machine instructions**로 변환됩니다.

### Constant Pool Resolution

바이트코드는 종종 직접 주소 대신 **symbolic references**를 사용합니다. 실행 중 method, field, class, string 참조가 실제 메모리 주소로 해석됩니다.

### Dynamic Linking

실행 중 **runtime symbol resolution**이 발생합니다. 클래스 연결, 메서드 연결, 인터페이스 연결이 동적으로 수행됩니다.

### Program Counter Progression

바이트코드는 **sequential instruction stream**입니다. 실행 엔진은 **program counter advancement**를 통해 다음 명령으로 이동합니다.

### Managed Runtime Integration

바이트코드는 단독 실행되지 않습니다. 반드시 **managed runtime environment** 안에서 동작하며 다음과 긴밀히 연결됩니다.

- GC
- thread system
- allocator
- metadata system
- verifier

### Runtime Metadata Dependency

바이트코드는 실행 중 **runtime metadata lookup**을 수행합니다.

예: `type metadata`, `method descriptors`, `virtual tables`, `object layout metadata`

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**대표 관측 명령어:** `perf`, `strace`, `top`, `pidstat`, `vmstat`

| 현상 | 의미 |
|---|---|
| CPU spike | interpretation/JIT |
| startup delay | class loading |
| cache miss | instruction locality 문제 |
| branch miss | dispatch inefficiency |
| RSS growth | metadata/code cache 증가 |

### Runtime

| 항목 | 의미 |
|---|---|
| loaded classes | 바이트코드 적재량 |
| compilation count | JIT activity |
| interpreter time | 해석 실행 비율 |
| code cache size | native 변환량 |
| verification time | 검증 비용 |

**중요 지표:** bytecode execution rate, JIT compilation rate, instruction dispatch cost

### Kubernetes

| 현상 | 원인 |
|---|---|
| pod startup latency | class/module loading |
| CPU burst | JIT compilation |
| memory growth | code cache |
| warmup delay | interpretation phase |

특히 **runtime warmup behavior**가 중요합니다. 초기 실행 시 verification, interpretation, profiling, JIT compilation이 동시에 진행됩니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*