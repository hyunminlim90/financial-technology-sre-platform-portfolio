# 자바 소스 파일 (Java Source File)

> 정독: 0회

## 1. 이 기술이 무엇인가

자바 소스 파일(`.java`)은:

> 개발자가 자바 언어 문법으로 작성한 **고수준 텍스트 기반 프로그램 정의 파일**

### 핵심 특징

- 사람이 읽고 수정 가능
- UTF 기반 문자 스트림
- 컴파일 대상 입력 파일
- 클래스/인터페이스 정의 포함
- 플랫폼 독립적 텍스트 포맷
- `javac` 입력 단위

### 핵심 역할

> `.java` 파일 자체는 실행되지 않습니다.  
> 반드시 `javac` compiler를 통해 `.class` bytecode로 변환되어야 합니다.

### 전체 흐름

```
.java
→ Lexical Analysis
→ Parsing
→ AST 생성
→ Semantic Analysis
→ Bytecode Generation
→ .class
→ JVM Execution
```

<details>
<summary>Deep Dive</summary></br>

Programmer(프로그래머) [[M]](../../100-deep-dive/micro-foundations/programmer.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

자바 개발 생태계 전반에서 등장합니다.

**대표 영역:**

- 서버 애플리케이션
- CLI 프로그램
- Android legacy ecosystem
- build system
- distributed platform
- data processing system
- JVM ecosystem

### 실제 위치

보통 다음 source tree 아래 존재합니다.

```
/src
/src/main/java
```

### 빌드 과정

빌드 도구가 다음을 수행합니다.

- source discovery
- dependency resolution
- compilation

### 컨테이너 환경

Kubernetes에서는 일반적으로 **source file 자체는 production container에 없음**.  
대부분 `.class` 또는 `.jar`만 포함됩니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

주요 영향 자원: **CPU + Memory + Disk**

### CPU 영향 — 컴파일 과정에서 큼

발생 원인:

- lexical analysis
- parsing
- type checking
- optimization
- bytecode generation

### Memory 영향

AST 생성과 symbol table 관리 때문에 중요. 대규모 코드베이스일수록 증가.

### Disk 영향

source scan 및 build artifact 생성과 연결.

### Network 영향

직접 영향은 적으나, distributed build / remote dependency fetch / CI/CD 환경에서는 관련 있음.

---

## 4. 왜 중요한가

JVM ecosystem의 **출발점**입니다.

| 이유 | 설명 |
|---|---|
| **1. 인간이 작성 가능한 레벨** | 고수준 추상화 제공 |
| **2. 플랫폼 독립적 개발** | 운영체제와 CPU 구조 분리 |
| **3. 정적 검증 가능** | 컴파일 단계에서 syntax / type / symbol reference 검사 |
| **4. 대규모 유지보수 가능** | module / class / package 단위 관리 |
| **5. 빌드 파이프라인 핵심** | CI/CD 전체 시작점 |

---

## 5. 실제 장애와 어떤 관련이 있는가

### Compilation Failure — 대표적 build failure

```
syntax error
missing symbol
type mismatch
```

### Dependency Conflict

import 또는 library version 충돌 발생 가능.

### Incremental Build Corruption

stale class 문제 발생 가능.

### Encoding Issue

UTF-8 / UTF-16 mismatch 시:

```
broken character
compile failure
```

### Massive Source Tree

대규모 monorepo에서:

- build latency 증가
- memory usage 증가
- CI slowdown

### Annotation Processing Overhead

compile-time code generation 과부하 가능.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

핵심 메커니즘은 **7개**입니다.

| # | 메커니즘 | 설명 |
|---|---|---|
| 1 | **Text-based Source** | `.java`는 순수 텍스트 파일 |
| 2 | **Lexical Analysis** | 문자 스트림 → token 변환 (`class`, `public`, `if`, identifier, operator) |
| 3 | **Parsing** | token → AST(Abstract Syntax Tree) 생성 |
| 4 | **Semantic Analysis** | type checking / symbol resolution / access validation 수행 |
| 5 | **Bytecode Generation** | AST → `.class` bytecode 생성 |
| 6 | **Constant Pool Preparation** | symbolic reference metadata 준비 |
| 7 | **Platform-independent Compilation** | 특정 CPU instruction 생성하지 않음. 출력은 JVM bytecode |

### 핵심 흐름

```
Source Text
→ Token Stream
→ AST
→ Semantic Validation
→ Bytecode
→ .class
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### source file 확인

```bash
find . -name "*.java"
```

### encoding 확인

```bash
file MyClass.java
```

### javac compilation

```bash
javac MyClass.java
```

### generated class 확인

```bash
ls *.class
```

### AST 수준 분석

```bash
javac -Xprint
```

### build process 확인

```bash
mvn compile
gradle build
```

### CI 로그

GitHub Actions / Jenkins / GitLab CI 등에서 다음 확인 가능:

- compile error
- source scan
- dependency resolution

### Kubernetes

보통 production pod 내부에는 없음. 다만 build container에서는 존재 가능.

source mount 확인:

```bash
kubectl exec <pod> -- find / -name "*.java"
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*