# 구문 및 의미 분석 (Syntactic and Semantic Analysis)

> 정독: 0회

## 1. 이 기술이 무엇인가

구문 및 의미 분석은:

> 컴파일러 프론트엔드(Frontend)의[[ 핵심 단계로, 소스코드가 언어 규칙에 맞게 작성되었는지 검증하고 컴퓨터가 이해할 수 있는 내부 표현으로 변환하는 과정

```
소스코드
↓
구문 및 의미 분석
↓
중간 표현 (IR)
↓
최적화
↓
코드 생성
```

즉, **인간이 작성한 텍스트를 컴파일러가 이해하는 논리 구조**로 변환하는 단계입니다.

<details>
<summary>Deep Dive</summary></br>

Compiler Front-end(컴파일러 전반부) [[M]](../../100-deep-dive/micro-foundations/compiler-front-end.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

컴파일러 내부의 가장 앞부분(Frontend)에서 등장합니다.

```
Source Code
↓
Lexical Analysis
↓
Syntax Analysis
↓
Semantic Analysis
↓
Intermediate Representation (IR)
↓
Optimization
↓
Code Generation
```

컴파일러 구조상 위치:

```
컴파일러
├─ Frontend
│  ├─ Lexical Analysis
│  ├─ Syntax Analysis
│  └─ Semantic Analysis
├─ Optimizer
└─ Backend
```

---

## 3. 어떤 자원에 가장 영향이 큰가

주요 자원은 **CPU**와 **Memory**입니다.

| 자원 | 역할 |
|------|------|
| CPU | 문법 검사, 타입 검사, 심볼 탐색, AST 생성 수행 |
| Memory | Token, AST, 심볼 테이블(Symbol Table), IR 유지. 대형 프로젝트일수록 증가 |

> Network 영향은 거의 없으며, Disk는 결과 저장 시에만 사용됩니다.

---

## 4. 왜 중요한가

이 단계에서 대부분의 개발자 오류가 발견됩니다.

```c
int a = "hello";  // 타입 불일치
int a = 10        // 세미콜론 누락
```

컴파일러는 코드 생성 단계까지 가지 않고 구문 오류 또는 의미 오류를 즉시 보고합니다.

> 즉, **잘못된 프로그램을 기계어로 생성하지 않도록 차단**하는 역할입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

| 오류 유형 | 예시 | 원인 |
|-----------|------|------|
| Syntax Error | `if (x > 0 {` | 괄호 불일치 |
| Undefined Symbol | `result = value + unknown;` | `unknown` 미정의 |
| Type Error | `int a; char* b; a = b;` | 타입 불일치 |
| Scope Error | 블록 외부에서 내부 변수 참조 | `x` 사용 불가 |

이러한 오류는 모두 **코드 생성 이전에 발견**됩니다.

---

## 6. 핵심 메커니즘

현재 문맥에서는 3단계만 이해하면 충분합니다.

### ① Lexical Analysis (토큰 생성)

소스코드를 의미 단위로 분해합니다.

```
int mode = 3;
↓
int | mode | = | 3 | ;
```

결과: **Token Stream** 생성

### ② Syntax Analysis (문법 구조 확인)

Token Stream이 문법에 맞는지 검사합니다.

```c
int mode = 3;   // 정상
int = mode 3;   // 비정상
```

정상이라면 **AST (Abstract Syntax Tree)** 생성

### ③ Semantic Analysis (논리적 의미 확인)

문법은 맞더라도 논리적 의미가 맞는지 검사합니다.

```c
int a;
string b;
a = b;   // 문법은 맞지만 타입이 다름 → 오류
```

최종 결과물 흐름:

```
AST
↓
Semantic Validation
↓
IR
```

| 단계 | 검사 내용 |
|------|-----------|
| 구문 분석 | 형태가 맞는가 |
| 의미 분석 | 논리가 맞는가 |

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 전처리 결과 확인 (토큰/구문 전 단계)
gcc -E file.c

# AST 확인 (Clang 계열)
clang -Xclang -ast-dump -fsyntax-only file.c

# 의미 분석 오류 확인
gcc file.c
clang file.c
```

### Kubernetes

이 단계는 Kubernetes에서 직접 나타나지 않습니다. 컨테이너 이미지가 만들어지기 이전, 빌드 단계에서 수행됩니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*