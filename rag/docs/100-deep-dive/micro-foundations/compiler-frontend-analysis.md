# 컴파일러 프론트엔드 분석 (Compiler Frontend Analysis)

> 정독: 0회

## 1. 이 기술이 무엇인가

컴파일러 프론트엔드 분석(Compiler Frontend Analysis)은:

> 사람이 작성한 source code를 입력받아 문법 구조를 검증하고 의미적 오류를 검사하며 컴파일 가능한 내부 구조(AST/typed IR)로 변환하는 단계

프론트엔드는 일반적으로:

| 단계 | 역할 |
|------|------|
| lexical analysis | tokenization |
| syntax analysis | grammar parsing |
| semantic analysis | meaning/type validation |

로 구성됩니다.

> **핵심:** frontend analysis establishes structural correctness before code generation begins

High-Level Source Language(고급 소스 언어) [[M]](../../100-deep-dive/micro-foundations/high-level-source-language.md)  

## 2. 시스템 어디에서 등장하는가

컴파일러 프론트엔드는 거의 모든 언어 처리 시스템에 존재합니다.

| 영역 | 사용 위치 |
|------|----------|
| compiler | source validation |
| interpreter | syntax processing |
| IDE/LSP | realtime diagnostics |
| static analyzer | semantic validation |
| transpiler | syntax transformation |
| build system | compile pipeline |

**대표 사례:**

| 시스템 | 역할 |
|--------|------|
| GCC/Clang frontend | C/C++ parsing |
| Roslyn | C# frontend |
| TypeScript compiler | typed AST generation |
| Babel | JS AST transform |
| LLVM frontend | IR generation |

> **즉:** frontend analysis is the entry gate of executable program formation

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**가장 영향 큰 자원: CPU + Memory**

이유: 프론트엔드는 내부적으로 다음을 수행하기 때문입니다.

- token stream 생성
- grammar parsing
- AST graph construction
- symbol table generation
- semantic traversal

특히 대규모 프로젝트에서는:

| 자원 | 영향 |
|------|------|
| CPU | parsing workload |
| Memory | AST/symbol table allocation |
| Disk | source file reads |
| Network | remote dependency fetch |

IDE 환경에서는:

> **incremental frontend analysis becomes continuous runtime workload**

---

## 4. 왜 중요한가

프론트엔드는 **compiler correctness foundation**입니다.

이 단계가 실패하면:

- optimization 불가능
- code generation 불가능
- executable 생성 불가능

**핵심 역할:**

| 역할 | 설명 |
|------|------|
| syntax validation | 문법 무결성 |
| type validation | 타입 안정성 |
| symbol resolution | 이름 해석 |
| semantic correctness | 의미 검증 |
| structural representation | 내부 구조 생성 |

> **중요한 이유:** backend optimization depends entirely on frontend structural correctness

---

## 5. 실제 장애와 어떤 관련이 있는가

프론트엔드 문제는 빌드 실패로 직결됩니다.

**대표 사례:**

| 장애 | 원인 |
|------|------|
| compile failure | syntax error |
| unresolved symbol | missing declaration |
| type mismatch | semantic inconsistency |
| ambiguous grammar | parser conflict |
| invalid AST | parser bug |
| compiler crash | malformed syntax tree |

**실무에서 자주 보이는 문제:**

| 문제 | 결과 |
|------|------|
| dependency version drift | symbol mismatch |
| generated code corruption | parse failure |
| encoding issue | lexer failure |
| recursive macro explosion | parser memory exhaustion |

**IDE/LSP 장애:**

> frontend instability directly affects developer productivity

예:
- autocomplete failure
- false diagnostics
- symbol navigation collapse
- syntax highlighting corruption

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Lexical Analysis (Lexer)

첫 단계: **convert character stream into token stream**

- **입력:** raw source text
- **출력:** tokens

| 종류 | 예 |
|------|---|
| keyword | `if`, `class` |
| identifier | variable names |
| operator | `+`, `-`, `*` |
| literal | numbers, strings |
| delimiter | `{}`, `()`, `;` |

Lexer 역할:
- whitespace 제거
- comment 처리
- token classification

---

### Syntax Analysis (Parser)

두 번째 단계: **validate token ordering against language grammar**

Parser는 다음을 사용합니다:
- CFG (Context-Free Grammar)
- parsing table
- recursive descent
- LR/LL parsing

> **핵심:** parser transforms flat tokens into hierarchical program structure

---

### Abstract Syntax Tree (AST)

Parser 결과물 핵심: **AST represents structural program meaning**

| 특징 | 설명 |
|------|------|
| hierarchical | parent-child structure |
| semantic-friendly | meaning extraction |
| optimization-ready | compiler traversal 가능 |
| language-aware | grammar structure 보존 |

예:
```
BinaryExpression
 ├── Identifier(a)
 └── Literal(10)
```

---

### Semantic Analysis

문법 이후 핵심 단계: **validate program meaning beyond syntax correctness**

| 검증 | 목적 |
|------|------|
| type checking | 타입 안정성 |
| symbol resolution | 참조 확인 |
| scope validation | 접근 가능성 |
| overload resolution | 함수 선택 |
| control-flow validation | 경로 무결성 |

탐지 예:
- `int + string`
- undeclared variable
- invalid return type

---

### Symbol Table

프론트엔드 핵심 자료구조: **symbol table stores semantic program metadata**

| 정보 | 의미 |
|------|------|
| variable names | identifier mapping |
| types | data definition |
| scope | visibility |
| functions | callable entities |
| linkage info | external references |

---

### Type System Enforcement

> **frontend analysis enforces static program constraints**

즉 런타임 이전에 다음을 차단합니다:
- invalid operation
- illegal conversion
- unsafe assignment

---

### Frontend → IR Transition

프론트엔드 최종 목적: **produce validated intermediate representation for optimization/codegen**

```
source code
→ tokens
→ AST
→ semantic graph
→ IR
```

---

### Compile-time Only

프론트엔드는 **non-runtime structural analysis phase**입니다.

이 단계에서는:
- process execution 없음
- heap object lifecycle 없음
- runtime scheduling 없음

오직:
- 구조 검증
- 의미 분석
- 내부 표현 생성

만 수행합니다.

---

### Error Detection Boundary

프론트엔드 핵심 가치: **detect invalid program structure before executable generation**

달성하는 것:
- early failure
- deterministic validation
- safer runtime

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**대표 도구:**
- `gcc`
- `clang`
- `flex`
- `bison`
- `tree-sitter`

| 관측 항목 | 의미 |
|----------|------|
| parse error | syntax failure |
| semantic error | type failure |
| symbol resolution | linkage state |
| AST dump | internal structure |

예:
```bash
clang -Xclang -ast-dump
```

---

### IDE / LSP

| 기능 | frontend dependency |
|------|---------------------|
| autocomplete | symbol analysis |
| go-to-definition | AST traversal |
| syntax highlighting | lexical analysis |
| linting | semantic analysis |

> **modern developer tooling continuously executes frontend analysis**

---

### CI/CD

빌드 파이프라인에서:

| 단계 | 역할 |
|------|------|
| compile validation | syntax/type gate |
| static analysis | semantic verification |
| lint pipeline | style/structure |
| codegen validation | parser integrity |

**실패 예:**
- `build failed due to syntax error`
- `type resolution failure`
- `parser internal error`

---

### Kubernetes

K8s는 직접 frontend를 실행하지 않지만, 컨테이너 내부의 다음 구성요소가 frontend를 수행합니다:

- compiler toolchain
- build agents
- CI runners

| 구성 | 역할 |
|------|------|
| Jenkins agents | compile execution |
| GitHub Actions | build validation |
| Tekton | frontend pipeline |
| Bazel remote builds | distributed parsing |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*