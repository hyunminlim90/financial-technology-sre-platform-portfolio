# 중간 표현 (Intermediate Representation, IR)

> 정독: 0회

## 1. 이 기술이 무엇인가

중간 표현(IR)은:

> 컴파일러가 소스코드를 분석한 뒤 내부적으로 사용하는 표준화된 중간 형태

- 소스코드도 아니고
- 기계어도 아닙니다

```
Source Code
↓
Compiler Front-end
↓
IR
↓
Optimizer
↓
Optimized IR
↓
Code Generation
↓
Machine Code
```

현재 문맥에서 IR은 **컴파일러 내부 공용 언어**라고 이해하면 충분합니다.

<details>
<summary>Deep Dive</summary></br>

Compiler(컴파일러) [[M]](../../100-deep-dive/micro-foundations/compiler.md)  
Source Program(소스 프로그램) [[M]](../../100-deep-dive/micro-foundations/source-program.md)  
Syntactic and Semantic Analysis(구문 및 의미 분석) [[M]](../../100-deep-dive/micro-foundations/syntactic-and-semantic-analysis.md)  
Machine-Independent Optimization(기계 독립적 최적화) [[M]](../../100-deep-dive/micro-foundations/machine-independent-optimization.md)  
Language and Machine Independence(언어 및 기계 독립성) [[M]](../../100-deep-dive/micro-foundations/language-and-machine-independence.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

컴파일러 내부, Front-end와 Back-end 사이에 존재합니다.

```
Source Code
↓
Lexical Analysis
↓
Syntax Analysis
↓
Semantic Analysis
↓
IR 생성
↓
Machine-Independent Optimization
↓
Code Generation
↓
Object File
↓
Linking
↓
Executable
```

컴파일러 구조상 위치:

```
Compiler
├─ Front-end
├─ IR          ← 현재 위치
├─ Optimizer
└─ Back-end
```

---

## 3. 어떤 자원에 가장 영향이 큰가

IR은 컴파일 시점 데이터 구조이며, 주로 **CPU**와 **Memory**에 영향을 줍니다.

| 자원 | 역할 |
|------|------|
| CPU | 데이터 흐름 분석, 제어 흐름 분석, 최적화 알고리즘 수행 |
| Memory | IR Node, CFG, SSA, Symbol 정보, Type 정보 저장 |

> Network 영향은 거의 없으며, Disk는 목적 파일 생성 직전까지만 간접 영향이 있습니다.

---

## 4. 왜 중요한가

IR이 없다면 언어마다, CPU마다, 최적화마다 별도 구현이 필요합니다.

| 구조 | 필요한 번역기 수 |
|------|----------------|
| IR 없음 (5개 언어 × 5개 CPU) | 25개 |
| IR 있음 (5개 언어 → IR, IR → 5개 CPU) | 10개 |

IR을 사용하면 `언어 → IR`과 `IR → CPU`로 분리되어 구현 비용이 크게 줄어듭니다. 그래서 현대 컴파일러는 사실상 모두 IR 중심 구조입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

IR은 런타임 장애와 직접 연결되지는 않습니다. 대신 다음과 연결됩니다.

| 문제 유형 | 결과 |
|-----------|------|
| 잘못된 최적화 | 잘못된 기계어 생성 |
| 최적화 실패 | CPU·메모리 사용량 증가, 실행 속도 저하 |
| SSA 분석 오류 | 잘못된 데이터 흐름 추론 |

이러한 문제는 모두 IR 단계에서 시작됩니다.

---

## 6. 핵심 메커니즘

> **핵심 사실:** IR은 기계어가 아니다.

IR은 하드웨어 독립적, 운영체제 독립적, 언어 독립적 구조입니다.

소스코드와 IR의 차이:

```c
// 소스코드
a = b + c;
```

```
// IR
t1 = load b
t2 = load c
t3 = add t1, t2
store t3 -> a
```

아직 `x86 ADD`, `ARM ADD`, `RISC-V ADD` 같은 실제 CPU 명령어가 아닙니다.

IR의 핵심 역할 세 가지:

### ① 최적화 대상

```
IR  →  Optimizer  →  Optimized IR
```

### ② 하드웨어 독립성 유지

ISA, ABI, Register, Alignment, Padding을 아직 고려하지 않습니다.

### ③ 백엔드 입력

```
Optimized IR  →  Code Generation  →  Machine Code
```

> 현재 문맥에서 기억해야 할 핵심: IR은 **최적화를 위한 작업 공간**이다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

IR은 런타임에 존재하지 않으며, 컴파일 중에만 존재합니다.

### Linux

```bash
# LLVM IR 출력
clang -emit-llvm -S file.c
# 결과: file.ll (LLVM IR 텍스트 형식)

# GCC IR 덤프
gcc -fdump-tree-all file.c
```

### Kubernetes

CI Build, Container Build, Image Build 과정에서만 존재합니다. 실행 중인 Pod에서는 이미 사라진 단계입니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*