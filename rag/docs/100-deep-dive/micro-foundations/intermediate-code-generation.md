# 중간 코드 생성 (Intermediate Code Generation)

> 정독: 0회

## 1. 이 기술이 무엇인가

중간 코드 생성(IR Generation)은:

> 컴파일러 전반부의 마지막 단계입니다. 구문 분석과 의미 분석이 완료된 소스코드를 컴파일러 내부의 표준 형식인 IR(Intermediate Representation)로 변환

```
AST
↓
IR
```

이 단계가 끝나면 원래 코드가 어떤 언어로 작성되었는지에 대한 정보는 대부분 사라지고, 컴파일러가 최적화하기 쉬운 형태의 중간 코드만 남게 됩니다.

---

## 2. 시스템 어디에서 등장하는가

```
Source Code
↓
Lexical Analysis
↓
Syntax Analysis
↓
Semantic Analysis
↓
AST
↓
★ Intermediate Code Generation
↓
IR
↓
Optimizer
↓
Code Generation
↓
Object File
```

컴파일러 구조상 위치: **Compiler Front-end → AST → IR Generation → IR → Machine-Independent Optimization** 구간에 해당합니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

주요 자원은 **CPU**와 **Memory**입니다.

| 자원 | 역할 |
|------|------|
| CPU | AST 순회, 제어 흐름 생성, 데이터 흐름 생성, SSA 변환 등 수행 |
| Memory | IR Node, Basic Block, CFG, SSA Variable, Symbol Table 등 저장 |

> Disk는 최종 목적 파일 생성 전까지 영향이 크지 않으며, Network는 관련 없습니다.

---

## 4. 왜 중요한가

현대 컴파일러 최적화의 출발점입니다. 컴파일러는 소스코드를 직접 최적화하지 않고 IR을 통해 최적화합니다.

```
Source Code  →  IR  →  Optimization
```

IR이 존재하기 때문에 다음 최적화가 가능해집니다.

- Dead Code Elimination
- Loop Optimization
- Constant Folding
- Data Flow Analysis

또한 **언어 독립성**과 **기계 독립성**을 확보할 수 있습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

일반 애플리케이션 장애와 직접 연결되지는 않지만, 컴파일러 자체 품질에는 매우 중요합니다.

| 문제 유형 | 원인 | 결과 |
|-----------|------|------|
| 잘못된 IR 생성 | IR 변환 오류 | 잘못된 최적화, 잘못된 기계어 생성 |
| SSA 생성 오류 | 변수 추적 실패 | 잘못된 코드 제거 |
| 제어 흐름 생성 오류 | 분기 구조 오류 | 조건문 오동작, 분기 오류 |

연쇄 영향:

```
IR 생성 오류 → 최적화 오류 → 실행 결과 오류
```

---

## 6. 핵심 메커니즘

### ① AST를 IR로 변환

계층 구조인 AST를 선형 명령어로 변환합니다.

```c
x = a + b * c;
```

AST (계층 구조):

```
=
├── x
└── +
    ├── a
    └── *
        ├── b
        └── c
```

IR (선형 명령어):

```
t1 = b * c
t2 = a + t1
x  = t2
```

### ② 복잡한 식을 단순 연산으로 분해

복잡한 계산을 작은 단위로 나눠 최적화기가 분석하기 쉽게 만듭니다.

```
(a + b) * (c + d)
↓
t1 = a + b
t2 = c + d
t3 = t1 * t2
```

### ③ SSA 형태 생성

현대 컴파일러의 핵심입니다. 모든 변수에 고유한 버전 번호를 부여하여 값의 생성 시점을 정확히 추적합니다.

```
// 일반 코드
x = 1
x = x + 1
x = x + 1

// SSA 형태
x1 = 1
x2 = x1 + 1
x3 = x2 + 1
```

SSA 덕분에 Data Flow Analysis, Dead Code Elimination, Constant Propagation이 매우 쉬워집니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

실행 중에는 존재하지 않으며, 빌드 시점에만 존재합니다.

### Linux

```bash
# LLVM IR 텍스트 출력
clang -S -emit-llvm file.c
# 결과: file.ll

# LLVM IR 바이너리 출력
clang -emit-llvm -c file.c

# GCC IR 덤프
gcc -fdump-tree-all file.c
```

### Kubernetes

CI Pipeline, Build Stage, Container Build 과정에서만 존재합니다. 실행 중인 Pod에는 존재하지 않습니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*