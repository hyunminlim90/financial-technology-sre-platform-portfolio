# 컴파일러 전반부 (Compiler Front-end)

> 정독: 0회

## 1. 이 기술이 무엇인가

컴파일러 전반부(Front-end)는:

> 소스코드를 분석하여 컴파일러가 내부적으로 처리할 수 있는 중간 표현(IR)으로 변환하는 단계

```
소스코드
↓
컴파일러 전반부
↓
중간 표현 (IR)
```

전반부가 수행하는 것:

- 문법 검사
- 의미 검사
- 타입 검사
- 스코프 검사

> 아직 CPU 명령어 생성이나 ABI 적용은 수행하지 않습니다.

<details>
<summary>Deep Dive</summary></br>

Source Program(소스 프로그램) [[M]](../../100-deep-dive/micro-foundations/source-program.md)  
Syntactic and Semantic Analysis(구문 및 의미 분석) [[M]](../../100-deep-dive/micro-foundations/syntactic-and-semantic-analysis.md)  
Compiler(컴파일러) [[M]](../../100-deep-dive/micro-foundations/compiler.md)  
Machine-Independent Optimization(기계 독립적 최적화) [[M]](../../100-deep-dive/micro-foundations/machine-independent-optimization.md)  
Intermediate Representation(중간 표현) [[M]](../../100-deep-dive/micro-foundations/intermediate-representation.md)  
[[M]](../../100-deep-dive/micro-foundations/)

</details></br>

## 2. 시스템 어디에서 등장하는가

전체 컴파일러 구조에서 가장 앞부분에 위치합니다.

```
Source Code
↓
Compiler Front-end
↓
Intermediate Representation (IR)
↓
Optimizer
↓
Compiler Back-end
↓
Object File
↓
Linker
↓
Executable
```

---

## 3. 어떤 자원에 가장 영향이 큰가

주요 자원은 **CPU**와 **Memory**입니다.

| 자원 | 역할 |
|------|------|
| CPU | 토큰 생성, AST 생성, 타입 검사, 스코프 검사 수행. 대규모 프로젝트일수록 사용량 증가 |
| Memory | Token Stream, AST, Symbol Table, Type Information, IR 유지. 프로젝트 규모에 비례 |

> Network 사용은 거의 없으며, Disk는 분석 결과 저장 시에만 사용됩니다.

---

## 4. 왜 중요한가

전반부는 잘못된 프로그램을 가장 먼저 차단하는 단계입니다. 아래 오류는 모두 전반부에서 발견됩니다.

```c
int a = "hello";          // 타입 불일치
value = unknown_variable; // 미정의 심볼
if (x > 0                 // 괄호 누락
```

> 즉, **잘못된 프로그램이 기계어로 생성되는 것을 방지**하는 역할입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

| 오류 유형 | 예시 | 결과 |
|-----------|------|------|
| 문법 오류 | `if (a > 0` | 구문 분석 실패 |
| 타입 오류 | `integer = string;` | 의미 분석 실패 |
| 선언되지 않은 식별자 | `result = unknown;` | 심볼 탐색 실패 |
| 중복 정의 | `int value;` 두 번 선언 | 심볼 충돌 |

이러한 문제들은 모두 **코드 생성 이전에 제거**됩니다.

---

## 6. 핵심 메커니즘

전반부는 크게 3개의 내부 단계로 구성됩니다.

### ① Lexical Analysis (토큰 분해)

소스코드를 토큰으로 분해합니다.

```
int count = 10;
↓
int | count | = | 10 | ;
```

결과: **Token Stream** 생성

### ② Syntax Analysis (문법 검사)

토큰이 언어 문법에 맞는지 검사합니다.

```c
int count = 10;   // 정상
int = count 10;   // 비정상
```

결과: **AST (Abstract Syntax Tree)** 생성

### ③ Semantic Analysis (의미 검사)

AST가 논리적으로 올바른지 검사합니다.

```c
integer = string;  // 문법은 맞지만 타입 규칙 위반
```

결과: **Semantic Error** 발생

전반부 전체 결과물 흐름:

```
Source Code
↓
Token
↓
AST
↓
Semantic Validation
↓
IR
```

> 전반부가 끝난 시점에는 CPU 종류, 운영체제 종류, ABI, 레지스터 구조가 **아직 고려되지 않습니다.**

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

전반부는 빌드 단계에서만 존재하며, 런타임에서는 존재하지 않습니다.

### Linux

```bash
# 전처리 결과 확인
gcc -E file.c

# AST 확인 (Clang 계열)
clang -Xclang -ast-dump -fsyntax-only file.c

# 의미 분석 오류 확인
gcc file.c
clang file.c
```

### Kubernetes

CI Pipeline, Build Server, Container Build 단계에서만 등장합니다. 실행 중인 Pod 내부에서는 이미 종료된 단계입니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*