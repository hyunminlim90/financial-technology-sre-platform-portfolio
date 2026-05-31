# 언어 프로세서 (Language Processor)

> 정독: 0회

## 1. 이 기술이 무엇인가

**언어 프로세서(Language Processor)** 는:

> 사람이 작성한 프로그램을 컴퓨터가 실행 가능한 형태로 변환하거나 실행하는 시스템 소프트웨어

언어 프로세서는 하나의 제품이 아니라 **범주**입니다.

### 대표 종류

| 종류 | 역할 |
|------|------|
| Compiler | 소스코드 → 기계어 |
| Interpreter | 소스코드 → 즉시 실행 |
| Assembler | 어셈블리어 → 기계어 |
| Linker | 여러 오브젝트 파일 결합 |
| Loader | 실행파일 메모리 적재 |

### 핵심

**인간 언어와 CPU 기계어 사이를 연결하는 계층**입니다.

---

## 2. 시스템 어디에서 등장하는가

프로그램이 실행되기 전 과정 전체에서 등장합니다.

### 전체 흐름

```
Source Code
    ↓
Language Processor
    ↓
Machine Code
    ↓
CPU Execution
```

### 실제 상세 흐름

```
Editor
  ↓
Compiler
  ↓
Object File
  ↓
Linker
  ↓
Executable
  ↓
Loader
  ↓
Memory
  ↓
CPU
```

### 등장 영역

| 영역 | 역할 |
|------|------|
| 운영체제 | Loader |
| 개발도구 | Compiler |
| 빌드시스템 | Translation |
| 런타임 | Interpretation |
| 펌웨어 | Code Generation |
| 임베디드 | Cross Compilation |

---

## 3. 어떤 자원에 가장 영향이 큰가

### 1순위: CPU + Memory + Disk

| 자원 | 역할 |
|------|------|
| CPU | 문법 분석, 최적화, 코드 생성 수행 |
| Memory | AST, IR, Symbol Table 보관 |
| Disk | 소스 파일, 오브젝트 파일, 실행 파일 생성 |

Network 영향은 상대적으로 적으며, 빌드 서버나 원격 컴파일 환경 정도에서 사용됩니다.

---

## 4. 왜 중요한가

언어 프로세서가 없다면 **CPU가 이해하는 기계어를 사람이 직접 작성**해야 합니다.

### 추상화 제공

개발자는 다음 수준에서 개발 가능합니다:

- 자료구조
- 함수
- 객체
- 모듈

### 최적화 수행

- 불필요한 계산 제거
- 상수 계산
- 루프 최적화
- 레지스터 할당

### ABI 적용

다음은 언어 프로세서가 결정합니다:

- 구조체 배치
- 함수 호출 규약
- 레지스터 사용 규칙
- 비트 필드 할당

---

## 5. 실제 장애와 어떤 관련이 있는가

### ABI 불일치

A 컴파일러와 B 컴파일러가 서로 다른 레이아웃 생성 시:

- 데이터 손상
- 함수 호출 실패
- 라이브러리 충돌

### 최적화 버그

컴파일러 최적화 오류 시:

- 잘못된 코드 생성
- 데이터 손실
- 비정상 동작

### 아키텍처 불일치

```
ARM용 바이너리  →  x86 서버 실행 불가
```

### Alignment 문제

잘못된 레이아웃 생성 시:

- 성능 저하
- 예외 발생

---

## 6. 핵심 메커니즘

비트 필드 문맥에서 가장 중요한 부분입니다.

### Step 1 — 개발자 선언

```
field A : 1bit
field B : 3bit
field C : 4bit
```

### Step 2 — 언어 프로세서 해석

```
A  →  offset 0
B  →  offset 1
C  →  offset 4
```

### Step 3 — ABI 적용

ABI마다 규칙 확인:

- `32bit ABI`
- `64bit ABI`
- `ARM ABI`
- `x86 ABI`

### Step 4 — 메모리 레이아웃 생성

```
Word
 ├─ A  (offset 0, width 1)
 ├─ B  (offset 1, width 3)
 └─ C  (offset 4, width 4)
```

### Step 5 — 접근 코드 생성

실제 필드 접근은 Load → Shift → Mask → Store 조합으로 변환됩니다.

```
field B 읽기
    ↓
실제 기계어: load → shift → and
```

### 핵심 원칙

> 비트 필드 자체를 CPU가 이해하는 것이 아니라, **언어 프로세서가 비트 연산 코드로 변환**하는 것이 핵심입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

대표 도구:

- `gcc`
- `clang`
- `ld`
- `as`
- `objdump`
- `readelf`

```bash
# 어셈블리 생성
gcc -S main.c

# 기계어 확인
objdump -d app
```

### Runtime

관측 대상:

- `JIT Compiler`
- `Bytecode Translator`
- `Optimizer`

### Kubernetes

직접 등장하지는 않지만 CI/CD 파이프라인 전체에서 사용됩니다:

```
Git
 ↓
Build
 ↓
Compiler
 ↓
Binary
 ↓
Container
 ↓
Kubernetes
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*