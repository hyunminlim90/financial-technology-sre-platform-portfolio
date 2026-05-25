# 컴파일러 중간 표현 (Compiler Intermediate Representation, Compiler IR)

> 정독: 0회

## 1. 이 기술이 무엇인가

컴파일러 중간 표현(IR)은:

> 고급 소스 언어와 실제 CPU 기계어 사이에서 컴파일러 내부가 사용하는 **중간 실행 구조체**

### 핵심 특징

| 특징 | 설명 |
|---|---|
| compiler-internal | 컴파일러 내부 전용 |
| machine-independent | 하드웨어 독립 |
| optimization-friendly | 최적화 가능 |
| graph-oriented | 그래프 기반 구조 |
| transformable | 단계적 변환 가능 |

IR은 단순 문자열이 아니며, 보통 graph, tree, SSA form, control-flow structure 같은 형태로 존재합니다.

> **핵심 정의:** optimization-oriented intermediate program representation

---

## 2. 시스템 어디에서 등장하는가

| 위치 | 역할 |
|---|---|
| compiler frontend | AST → IR 변환 |
| optimizer | optimization 수행 |
| JIT engine | runtime optimization |
| register allocator | register planning |
| backend | machine code emission |

### 전체 흐름

```
source code → AST → IR → optimization → lower IR → machine code
```

런타임 환경에서는 다음과 같이 동작하기도 합니다:

```
bytecode → runtime IR → native machine code
```

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접 영향: **CPU + Memory**

| 자원 | 영향 |
|---|---|
| CPU | optimization computation |
| Memory | graph/SSA storage |
| Cache | generated code locality |
| Branch Prediction | optimized control flow |
| Register Usage | execution efficiency |

IR 최적화는 매우 복잡한 분석, 그래프 변환, 데이터 흐름 추적을 수행하므로 **runtime optimization cost**가 특히 중요합니다.

---

## 4. 왜 중요한가

IR의 핵심 목적은 **portable optimization layer**를 제공하는 것입니다.

IR이 없으면 고급 언어 구조와 CPU 기계어 생성이 직접 연결되어 다음 문제가 발생합니다:

- 최적화 복잡도 증가
- 플랫폼 종속성 증가
- 유지보수 비용 증가

IR은 **machine-independent transformation space**를 제공하여 optimization, analysis, scheduling, register planning 등을 CPU 독립적으로 수행할 수 있게 합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

| 문제 | 설명 |
|---|---|
| excessive optimization | CPU spike |
| JIT thrashing | 반복 재컴파일 |
| huge IR graphs | memory pressure |
| deoptimization storms | 성능 급락 |
| register spill explosion | cache miss 증가 |
| invalid optimization assumptions | runtime rollback |
| CFG explosion | compile latency 증가 |

JIT 기반 시스템에서는 profiling → IR generation → optimization → recompilation이 반복되므로 CPU 사용량이 급증할 수 있습니다.

또한 large control-flow complexity는 compile latency, memory usage, instruction cache pressure를 증가시킵니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### AST → IR Transformation

소스코드는 먼저 **abstract syntax tree**로 변환된 후, **lowered intermediate representation**으로 재구성됩니다. 이 단계에서 expression, branch, loop, variable flow가 정규화됩니다.

### Control Flow Graph (CFG)

IR은 보통 **control-flow graph** 형태를 가집니다. CFG는 basic block, branch edge, loop edge 관계를 표현하며, 이를 통해 **global flow optimization**이 가능해집니다.

### SSA Form

**Static Single Assignment Form**은 IR의 핵심 구조입니다.

> SSA 핵심 규칙: **one assignment per variable version**

```
x1 = 10
x2 = x1 + 1
```

| 효과 | 설명 |
|---|---|
| data-flow clarity | 데이터 흐름 명확화 |
| optimization simplicity | 최적화 단순화 |
| dependency tracking | 의존성 추적 |
| dead-code elimination | 죽은 코드 제거 |

### Optimization Passes

IR 위에서 수행되는 대표 최적화:

| 최적화 | 의미 |
|---|---|
| constant folding | 상수 계산 |
| dead code elimination | 불필요 코드 제거 |
| common subexpression elimination | 중복 연산 제거 |
| loop invariant motion | 루프 외부 이동 |
| inline expansion | 함수 인라인 |
| strength reduction | 연산 단순화 |

> **IR is optimization substrate**

### High-Level IR → Low-Level IR

IR은 여러 단계로 내려가며, 점점 hardware-awareness가 증가합니다:

| 단계 | 목적 |
|---|---|
| HIR | 고수준 의미 유지 |
| MIR | 중간 최적화 |
| LIR | 기계어 근접 |
| Machine IR | 실제 CPU 대응 |

### Register Allocation

후반 단계에서 **virtual registers → physical registers** 매핑이 수행됩니다. 여기서 실패하면 **register spill**이 발생하여 RAM 접근 증가, cache miss 증가, 성능 저하로 이어집니다.

### Runtime JIT IR

JIT 환경에서는 **runtime-generated IR**이 매우 중요합니다. 실행 중 profiling, hot path detection, speculative optimization이 수행된 후 **optimized native machine code**가 생성됩니다.

### Deoptimization

**Speculative optimization rollback**은 JIT의 핵심 위험 요소입니다. JIT는 가정을 기반으로 최적화하며, 가정이 깨지면 다음이 발생합니다:

1. optimized IR 폐기
2. interpreter fallback
3. recompilation

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**대표 도구:** `perf`, `top`, `pidstat`, `perf record`, `perf report`

| 현상 | 의미 |
|---|---|
| CPU burst | optimization/JIT |
| compile threads | runtime compilation |
| branch miss | poor CFG optimization |
| cache miss | bad register allocation |
| high context switching | compile contention |

### Runtime

| 메트릭 | 의미 |
|---|---|
| compilation count | JIT 횟수 |
| deoptimization count | optimization rollback |
| code cache usage | generated native code |
| compile queue length | JIT backlog |
| IR graph size | optimization complexity |

> 중요 항목: **runtime compilation pressure**

### Kubernetes

| 현상 | 원인 |
|---|---|
| startup CPU spike | JIT optimization |
| pod warmup latency | IR generation |
| unstable latency | deoptimization |
| autoscaling jitter | compile bursts |
| memory increase | IR/code cache |

대규모 서비스에서는 초기 트래픽 → hot path stabilization → JIT convergence 과정이 성능에 큰 영향을 줍니다.

> 특히 중요: **cold-runtime optimization overhead**

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*