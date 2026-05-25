# 고급 소스 언어 (High-Level Source Language)

> 정독: 0회

## 1. 이 기술이 무엇인가

고급 소스 언어는(**human-readable abstract programming language**):

> 개발자가 CPU 명령어, 레지스터, 메모리 주소, 전기 신호를 직접 다루지 않고도 시스템 논리를 작성할 수 있게 만드는 계층

핵심 목적은 **hardware abstraction**입니다.

### 대표 특징

| 특징 | 설명 |
|------|------|
| human-readable | 사람이 읽기 쉬움 |
| abstracted | 하드웨어 추상화 |
| structured | 구조적 문법 |
| portable | 플랫폼 독립적 |
| compiler-friendly | 컴파일 가능 |

> 핵심 정의: **abstract executable logic description language**

---

## 2. 시스템 어디에서 등장하는가

### 등장 위치

| 위치 | 역할 |
|------|------|
| source repository | 원본 코드 저장 |
| compiler frontend | 입력 데이터 |
| build pipeline | 빌드 대상 |
| IDE/editor | 개발 환경 |
| CI/CD | 자동 빌드 |
| static analysis | 코드 검사 |
| runtime origin | 실행 논리 원천 |

### 전체 흐름

```
source code
→ lexer
→ parser
→ AST
→ semantic analysis
→ IR/bytecode/native code
→ execution engine
```

> 즉 고급 소스 언어는 **logical origin of executable systems**입니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

직접 영향: **CPU + Memory**

| 자원 | 영향 |
|------|------|
| CPU | generated instruction efficiency |
| Memory | allocation pattern |
| Cache | locality behavior |
| Network | serialization/protocol logic |
| Disk | I/O behavior |

특히 중요한 것은 **runtime behavior generation**입니다.

고급 소스 언어 구조가 다음을 결정합니다:

- 객체 생성 패턴
- 메모리 사용 패턴
- 동시성 구조
- 네트워크 호출 패턴

---

## 4. 왜 중요한가

핵심 이유: **complexity abstraction**

하드웨어는 다음 수준으로 동작합니다:

- register
- instruction
- memory offset
- branch jump

하지만 고급 언어는 **human-scale logical expression**을 제공합니다.

즉 개발자는 다음에 집중할 수 있습니다:

- 비즈니스 로직
- 데이터 구조
- 제어 흐름
- 알고리즘

또한 **large-scale software maintainability**를 가능하게 합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 대표 장애

| 장애 | 설명 |
|------|------|
| memory leak | 객체 해제 실패 |
| race condition | 동시성 오류 |
| infinite loop | CPU 고갈 |
| recursion overflow | stack overflow |
| deadlock | 락 교착 |
| null dereference | invalid memory access |
| resource exhaustion | 자원 고갈 |
| excessive allocation | GC pressure 증가 |

> 핵심: **runtime problems originate from source-level logic**

### 소스 구조 → 런타임 영향

| 소스 구조 | 런타임 영향 |
|----------|-----------|
| 무한 객체 생성 | heap explosion |
| 비효율 loop | CPU saturation |
| 잘못된 synchronization | contention |
| 과도한 recursion | stack exhaustion |

즉 런타임 장애의 상당수는 **high-level logic structure**에서 시작됩니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Lexical Analysis

소스코드는 먼저 **token stream**으로 분해됩니다.

| 원본 | 토큰 |
|------|------|
| if | keyword |
| + | operator |
| variableName | identifier |

즉: **text → structured symbols** 변환입니다.

### Syntax Analysis

토큰은 **abstract syntax tree (AST)**로 구성됩니다.

AST는 다음을 표현합니다:

- expression hierarchy
- control flow
- scope structure

핵심 목적: **structural program representation**

### Semantic Analysis

이 단계에서 다음 검사가 수행됩니다:

- type correctness
- symbol resolution
- scope validity
- inheritance validity

즉: **logical correctness verification**

### Intermediate Representation (IR)

많은 컴파일러는 다음 단계를 거칩니다:

```
source language → intermediate representation
```

IR 목적: **optimization-friendly executable model**

### Code Generation

최종적으로 **machine-oriented executable transformation**이 수행됩니다.

출력은 다음이 될 수 있습니다:

- bytecode
- native machine code
- IR graph

### Abstraction Layer

고급 소스 언어의 핵심은 **hardware abstraction boundary**입니다.

즉 다음을 개발자로부터 숨깁니다:

- register management
- memory addressing
- instruction encoding

### Runtime Semantics

소스 문법은 단순 텍스트가 아닙니다. 실제로는 **runtime behavior specification**입니다.

| 소스 구조 | 런타임 의미 |
|----------|-----------|
| object creation | heap allocation |
| function call | stack frame creation |
| loop | repeated instruction dispatch |
| async/task | scheduler interaction |

### Compilation Boundary

고급 소스 언어는 **compile-time abstraction domain**입니다.

컴파일 이후부터는 bytecode / IR / machine code 세계로 넘어갑니다.

### Static vs Dynamic Information

고급 언어 단계에서는 **logical structure is static**입니다.

실제 메모리 주소나 객체 실체는 아직 존재하지 않습니다. 즉 다음은 런타임 이후 결정됩니다:

- 객체 생성 위치
- 실제 heap address
- runtime thread state

### Runtime Dependency

고급 언어 자체는 실행되지 않습니다. 반드시 **compiler + runtime environment**가 필요합니다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

직접 관측보다 **compiled runtime behavior** 형태로 나타납니다.

대표 도구: `perf`, `strace`, `top`, `vmstat`, `pidstat`

| 현상 | 원인 |
|------|------|
| high CPU | inefficient algorithm |
| memory growth | allocation-heavy code |
| syscall explosion | I/O-heavy logic |
| context switching | concurrency design |
| cache miss | poor data locality |

### Runtime

| 항목 | 의미 |
|------|------|
| allocation rate | 객체 생성 패턴 |
| thread count | concurrency structure |
| stack depth | call hierarchy |
| GC frequency | allocation behavior |
| lock contention | synchronization structure |

> 즉: **source-level design manifests as runtime metrics**

### Kubernetes

| 현상 | 소스 구조 영향 |
|------|--------------|
| pod OOMKill | excessive allocation |
| CPU throttling | inefficient computation |
| startup latency | initialization complexity |
| scaling instability | blocking architecture |
| memory fragmentation | allocation pattern |

특히 중요: **application runtime characteristics**

고급 소스 언어의 구조가 다음을 결정합니다:

- pod resource usage
- scaling behavior
- GC pressure
- startup characteristics

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*