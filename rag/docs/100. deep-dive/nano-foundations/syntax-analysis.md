# 구문 분석 (Syntax Analysis / Parsing)

> 정독: 0회

## 1. 이 기술이 무엇인가

구문 분석(Syntax Analysis)은:

> 컴파일러나 인터프리터가 소스코드를 토큰(Token) 단위로 분해하고 언어 문법 규칙에 맞는지 검사하며, 구조화된 내부 표현(AST 등)으로 재구성하는 단계

**핵심:**

> parsing converts flat token sequences into hierarchical program structure

### 대표 입력/출력

| 단계 | 데이터 |
|------|--------|
| 입력 | token stream |
| 출력 | AST / parse tree |

---

## 2. 시스템 어디에서 등장하는가

구문 분석은 거의 모든 실행 환경에서 등장합니다.

| 영역 | 사용 목적 |
|------|-----------|
| compiler frontend | source structure validation |
| interpreter | executable syntax parsing |
| query engine | SQL parsing |
| config loader | YAML/JSON parsing |
| shell | command parsing |
| browser engine | HTML/CSS/JS parsing |

### 대표 시스템

| 시스템 | 예 |
|--------|----|
| compiler | C/C++/Rust/Go frontend |
| VM runtime | bytecode verification |
| DBMS | SQL parser |
| Kubernetes | YAML parser |
| API gateway | routing config parser |

> parsing is foundational infrastructure for structured execution

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**가장 영향 큰 자원: CPU + Memory**

구문 분석은:

- 문자열 순회
- 토큰 생성
- 트리 생성
- 상태 머신 전이
- 재귀 구조 분석

을 수행하기 때문입니다.

### 자원별 영향

| 자원 | 영향 |
|------|------|
| CPU | grammar evaluation |
| Memory | AST allocation |
| Disk | source loading |
| Network | 거의 없음 |

대규모 빌드 시스템에서는 parsing CPU usage와 AST memory pressure가 매우 커질 수 있습니다.

---

## 4. 왜 중요한가

> all later compilation and execution depend on correct structural interpretation

구문 분석이 실패하면:

- 타입 분석 불가
- 최적화 불가
- 코드 생성 불가
- 실행 불가

### 중요 이유

| 이유 | 설명 |
|------|------|
| structural validation | 문법 무결성 |
| semantic preparation | 의미 분석 준비 |
| optimization foundation | IR 생성 기반 |
| safety | malformed input 차단 |
| deterministic execution | 실행 구조 고정 |

> syntax analysis establishes executable structural correctness

---

## 5. 실제 장애와 어떤 관련이 있는가

### 대표 사례

| 장애 | 원인 |
|------|------|
| startup failure | invalid config syntax |
| deployment failure | malformed YAML |
| compiler crash | parser bug |
| query rejection | SQL syntax error |
| API routing failure | broken config grammar |

### Kubernetes 대표 사례

```yaml
spec:
 containers:
  - name: api
   image: invalid-indent
```

```
→ indentation parsing failure
→ deployment reject
```

### CI/CD에서

| 문제 | 결과 |
|------|------|
| malformed manifest | rollout abort |
| broken JSON | API rejection |
| invalid DSL | pipeline failure |

### 보안 측면

> parser correctness is critical security boundary

잘못된 parser는 다음의 원인이 될 수 있습니다:

- injection vulnerability
- malformed input bypass
- parser differential attack

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Token Stream

구문 분석 입력은 **structured lexical tokens**입니다.

예:

```
int x = 10;

↓

[TYPE][IDENTIFIER][ASSIGN][NUMBER][SEMICOLON]
```

### Grammar Validation

parser 핵심 역할:

> verify token ordering against formal grammar rules

| 규칙 | 의미 |
|------|------|
| expression grammar | 연산 구조 |
| declaration grammar | 선언 구조 |
| scope grammar | 블록 구조 |
| control-flow grammar | if/loop 규칙 |

### Parse Tree / AST Generation

| 구조 | 목적 |
|------|------|
| parse tree | full grammar structure |
| AST | simplified executable structure |

> AST removes syntactic noise while preserving semantic structure

예: `a + b * c` → AST

```
+
├── a
└── *
    ├── b
    └── c
```

### Recursive Descent / LR / LL Parsing

| 방식 | 특징 |
|------|------|
| recursive descent | simple recursive parser |
| LL parser | top-down parsing |
| LR parser | bottom-up parsing |
| shift-reduce | stack-based grammar resolution |

### Compiler Frontend Pipeline

```
source text
→ lexical analysis
→ token stream
→ syntax analysis
→ AST
→ semantic analysis
→ IR generation
```

### Syntax vs Semantic Analysis

| 구분 | 역할 |
|------|------|
| syntax analysis | 문법 구조 검사 |
| semantic analysis | 의미/타입 검사 |

문법은 맞지만 의미는 틀린 경우:

```
1 + "abc"
```

parser는 통과 가능. → semantic analyzer가 실패 처리.

### Error Recovery

실무 parser 중요 기능:

> continue parsing after syntax error detection

IDE/editor는 syntax error 발생 후에도 AST를 최대한 유지해야 다음이 가능합니다:

- auto-complete
- syntax highlighting
- lint
- incremental compile

### Incremental Parsing

현대 시스템 핵심 개념: 전체 재파싱 대신 **re-parse only modified syntax regions** 사용.

IDE / LSP / browser engine의 핵심 기술.

### Parser Output as Compiler Foundation

parser 결과(AST)는 이후 다음 단계에 사용됩니다:

| 단계 | 사용 |
|------|------|
| semantic analysis | 타입 검증 |
| optimization | CFG/IR 생성 |
| code generation | machine code |
| static analysis | lint/security |

> AST becomes the foundational executable structure for later stages

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Compiler Toolchain

대표 도구: `gcc`, `clang`, `rustc`, `go build`

에러 예:
```
syntax error near unexpected token
```

### Kubernetes

| 대상 | parser |
|------|--------|
| YAML manifest | YAML parser |
| Helm template | template parser |
| CRD schema | OpenAPI parser |

대표 장애:
```
error converting YAML to JSON
```

### Linux Shell

쉘 자체도 parser 기반.

```
unexpected EOF while looking for matching quote
```
→ shell syntax parser failure

### Databases

```sql
SELECT * FORM users;
```
→ syntax error

### Runtime Engines

| 영역 | 예 |
|------|----|
| JSON parser | API input |
| protobuf parser | RPC decoding |
| config parser | application boot |
| expression parser | rule engine |

### Observability

실무에서 자주 보이는 로그:

| 로그 | 의미 |
|------|------|
| `syntax error` | grammar violation |
| `parse failure` | malformed structure |
| `unexpected token` | token mismatch |
| `invalid indentation` | YAML structure issue |
| `malformed input` | parser rejection |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*