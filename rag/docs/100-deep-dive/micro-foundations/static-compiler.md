# 정적 컴파일러 (Static Compiler)

> 정독: 0회

## 1. 이 기술이 무엇인가

정적 컴파일러(Static Compiler)는:

> 프로그램 실행 전에 source code를 분석하고 문법과 타입을 검증하며 최적화를 수행하고 실행 가능한 정적 산출물(binary/bytecode/object file)을 생성하는 시스템 소프트웨어

**핵심:**

> static compilation performs program translation ahead of execution

### 대표 산출물

| 형태 | 설명 |
|------|------|
| native executable | machine code binary |
| object file | relocatable binary |
| bytecode artifact | VM executable |
| shared library | reusable runtime module |

Compile-time(컴파일 타임) [[M]](../../100-deep-dive/micro-foundations/compile-time.md)  
High-Level Source Language(고급 소스 언어) [[M]](../../100-deep-dive/micro-foundations/high-level-source-language.md)  
Compiler Frontend Analysis(컴파일러 프론트엔드 분석) [[M]](../../100-deep-dive/micro-foundations/compiler-frontend-analysis.md)  

## 2. 시스템 어디에서 등장하는가

정적 컴파일러는 거의 모든 현대 플랫폼의 시작점입니다.

| 영역 | 역할 |
|------|------|
| build pipeline | executable generation |
| CI/CD | artifact production |
| package system | distributable binary |
| VM ecosystem | bytecode generation |
| embedded systems | firmware build |

### 대표 시스템

| 시스템 | 예 |
|--------|----|
| native compiler | GCC / Clang |
| VM compiler | JVM/.NET frontend |
| transpiler | TypeScript/Babel |
| kernel build | Linux kernel toolchain |
| container build | image assembly |

> static compilers establish executable program structure before runtime activation

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**가장 영향 큰 자원: CPU + Memory + Disk**

컴파일러는:

- parsing
- semantic analysis
- optimization
- IR transformation
- code generation

을 수행하기 때문입니다.

### 자원별 영향

| 자원 | 영향 |
|------|------|
| CPU | optimization workload |
| Memory | AST/IR graph allocation |
| Disk | artifact output |
| Network | dependency fetch/distribution |

대규모 프로젝트에서는 compile CPU saturation, linker memory exhaustion, cache invalidation 문제가 자주 발생합니다.

---

## 4. 왜 중요한가

정적 컴파일러는:

> **runtime correctness and executable integrity foundation**

컴파일러가 잘못되면:

- 잘못된 바이너리 생성
- ABI 깨짐
- 메모리 손상
- 런타임 크래시

가 발생합니다.

### 중요 이유

| 이유 | 설명 |
|------|------|
| syntax validation | 문법 안정성 |
| type safety | 타입 무결성 |
| optimization | 실행 성능 |
| executable generation | 배포 가능성 |
| deterministic structure | 구조 안정성 |

> the compiler defines executable structure before any runtime execution exists

---

## 5. 실제 장애와 어떤 관련이 있는가

### 대표 사례

| 장애 | 원인 |
|------|------|
| invalid binary | compiler bug |
| runtime crash | bad optimization |
| incompatible deployment | ABI mismatch |
| startup abort | broken metadata |
| illegal instruction | wrong target architecture |
| symbol resolution failure | linker/compiler inconsistency |

### CI/CD에서

| 문제 | 결과 |
|------|------|
| inconsistent builds | deployment drift |
| non-reproducible binaries | rollback failure |
| dependency mismatch | runtime incompatibility |

### 보안 측면

> compiler integrity is supply-chain security boundary

예:
- malicious compiler injection
- poisoned build artifact
- tampered dependency chain

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Frontend Phase

> convert source text into validated structural representation

| 단계 | 역할 |
|------|------|
| lexical analysis | token generation |
| syntax analysis | grammar validation |
| semantic analysis | type/rule validation |
| AST construction | structural representation |

### Static Validation

> detect structural and type errors before execution begins

런타임 이전에 제거하는 것:
- syntax errors
- type mismatches
- invalid references
- illegal operations

### Intermediate Representation (IR)

컴파일러 내부에서는:

> source code is transformed into optimization-friendly intermediate structures

| 구조 | 목적 |
|------|------|
| AST | syntax structure |
| CFG | control flow |
| SSA | optimization |
| low-level IR | code generation |

### Optimization Pipeline

| 최적화 | 목적 |
|--------|------|
| constant folding | compile-time evaluation |
| dead code elimination | unnecessary removal |
| inline expansion | call overhead reduction |
| register allocation | CPU efficiency |
| loop optimization | iteration efficiency |

> optimization transforms validated logic into hardware-efficient structure

### Backend / Code Generation

> generate executable binary representation

| 형태 | 설명 |
|------|------|
| native machine code | hardware executable |
| VM bytecode | virtual ISA executable |
| object file | linker input |
| assembly | ISA textual form |

### Static Artifact Production

컴파일러 산출물은 **non-runtime executable artifacts**입니다.

- 아직 실행되지 않았고
- 프로세스도 없으며
- 힙도 없음

존재하는 것: serialized executable structure / metadata / binary encoding

### Ahead-of-Time Translation

> translation occurs before program activation

**JIT와 차이:**

| Static Compiler | JIT |
|-----------------|-----|
| compile before execution | compile during execution |
| static artifact | dynamic code cache |
| no runtime profiling | runtime optimization |
| deterministic build | adaptive optimization |

### Linkage & Symbol Resolution

컴파일러 이후 연결되는 요소:

| 요소 | 목적 |
|------|------|
| symbol table | external reference |
| relocation | address fixup |
| linkage | module integration |

### Executable Metadata

정적 컴파일러는 실행 코드 외에도 생성합니다:

- debug info
- symbol metadata
- type descriptors
- relocation tables

이는 런타임 / 디버거 / 로더 / JIT가 활용.

### Runtime Preparation

> static compilers prepare executable structure but do not execute it

컴파일러는:
- 메모리 점유 안 함
- 객체 생성 안 함
- instruction execution 안 함

오직 구조 생성 / 규칙 검증 / 바이너리 생성 수행.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**대표 도구:** `gcc`, `clang`, `ld`, `objdump`, `readelf`, `nm`, `strip`

| 항목 | 의미 |
|------|------|
| ELF sections | executable layout |
| symbol table | linkage info |
| relocation entries | address fixup |
| architecture target | ISA compatibility |

### CI/CD

| 영역 | 관측 |
|------|------|
| build logs | compile status |
| artifact registry | binary storage |
| cache system | incremental builds |
| checksum validation | integrity |

**대표 장애:**
```
build failed
undefined symbol
linker error
illegal instruction
```

### Kubernetes

K8s 자체는 런타임 플랫폼이지만, 배포되는 대부분은 **compiler-generated static artifacts**입니다.

| 요소 | 생성 주체 |
|------|-----------|
| container binary | compiler |
| runtime library | compiler toolchain |
| OCI image contents | build system |

### Observability

| 지표 | 의미 |
|------|------|
| compile duration | build efficiency |
| binary size | optimization quality |
| symbol count | linkage complexity |
| memory during build | compiler IR pressure |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*