# 자바 소스 코드(Java Source Code)

> 정독: 0회

## 1. 이 기술이 무엇인가

자바 소스 코드는:

> **자바 언어 문법(JLS)에 따라 작성된 인간 친화적 텍스트 기반 프로그램 원본**

보통 `.java` 확장자를 가지며 클래스 정의, 메서드, 타입, 제어 흐름, 데이터 구조 등의 논리를 기술합니다.

| 특성 | 의미 |
|------|------|
| 고수준 언어 | 인간 중심 문법 |
| 정적 텍스트 | 실행 전 상태 |
| 컴파일 대상 | JVM 바이트코드 생성 |
| 플랫폼 독립적 | 특정 CPU에 직접 종속되지 않음 |

자바 소스 코드는 **"실행 이전의 논리 원본"** 입니다.

---

## 2. 시스템 어디에서 등장하는가

자바 시스템 생명주기의 가장 상위 단계에서 등장합니다.

```
Java Source Code
→ javac Compiler
→ Java Class File (.class)
→ JVM Runtime
→ Native Machine Code
→ CPU Execution
```

| 영역 | 역할 |
|------|------|
| 개발 | 논리 구현 |
| 빌드 | 컴파일 입력 |
| 테스트 | 검증 대상 |
| CI/CD | 빌드 아티팩트 생성 |
| 운영 | 장애 원인 분석 기준 |

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

직접적으로는 **Disk와 Memory**에 가장 먼저 영향을 줍니다.

### Disk

소스 코드는 파일 시스템에 저장되는 정적 파일 엔티티입니다. `.java` 파일, 프로젝트 구조, 패키지 디렉터리 형태로 존재합니다.

### CPU

컴파일 시 CPU 사용량에 영향을 줍니다. 구문 분석, 타입 검사, 바이트코드 생성, 최적화 과정을 거치며, 대규모 프로젝트에서는 컴파일 CPU 사용량이 매우 커질 수 있습니다.

### Memory

컴파일러는 소스 코드를 메모리에 로드하며 다음 내부 구조를 생성합니다: Token Stream, AST, Symbol Table, Type Metadata

### Network

직접 영향은 적지만 Git, CI/CD, 원격 빌드, 패키지 다운로드 등에서 네트워크 사용과 연결됩니다.

---

## 4. 왜 중요한가

자바 소스 코드는 시스템의 구조, 동작 방식, 성능 특성, 안정성, 확장성을 결정합니다.

운영 중 발생하는 대부분의 문제는 결국 **소스 코드 수준의 논리에서 시작**됩니다.

| 문제 | 코드 원인 |
|------|----------|
| Memory Leak | 객체 관리 오류 |
| Deadlock | 동기화 설계 실패 |
| CPU Spike | 비효율 반복 |
| 장애 전파 | 잘못된 의존성 |
| 데이터 손상 | 트랜잭션 처리 오류 |

---

## 5. 실제 장애와 어떤 관련이 있는가

| 장애 | 소스 코드 원인 |
|------|--------------|
| OutOfMemoryError | 객체 누수 |
| StackOverflowError | 재귀 오류 |
| Race Condition | 동시성 제어 실패 |
| File Descriptor Leak | close 누락 |
| 장애 전파 | 예외 처리 실패 |
| 성능 저하 | 비효율 자료구조 |

> 운영 장애는 대부분 **"실행 중인 소스 코드 논리의 결과"** 입니다.

---

## 6. 핵심 메커니즘

### (1) 정적 텍스트 파일

자바 소스 코드는 실행 전 상태의 정적 파일입니다.

**특징:** UTF 기반 문자 인코딩, JLS 문법 준수, 파일 시스템 객체, 인간 중심 구조

```java
class UserService {
    void createUser() {}
}
```

### (2) 컴파일 대상

소스 코드는 직접 실행되지 않으며 반드시 컴파일 과정을 거칩니다.

```
.java
→ Lexical Analysis
→ Syntax Analysis
→ Type Check
→ Bytecode Generation
→ .class
```

### (3) AST 생성

컴파일러는 소스 코드를 내부 구조로 변환합니다.

핵심 구조: Token, AST(Abstract Syntax Tree), Symbol Table

문자열을 기계가 처리 가능한 **구조적 모델로 변환**합니다.

### (4) 타입 시스템 검증

자바 컴파일러는 강한 타입 검사를 수행합니다.

검사 대상: 타입 일치, 접근 제한, 메서드 시그니처, 제네릭 규칙

컴파일 단계에서 많은 오류를 차단합니다.

### (5) 바이트코드 생성

컴파일 결과는 JVM용 표준 중간 코드입니다.

출력: `.class`, Constant Pool, Bytecode, Method Metadata

이후 JVM이 실행합니다.

### (6) 런타임 연결

실행 시 소스 코드 논리는 다음 구조로 이어집니다.

```
소스 코드
→ 바이트코드
→ JVM
→ Native Code
→ OS System Call
→ Kernel
→ Hardware
```

최종적으로 **실제 CPU와 메모리 사용으로 연결**됩니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

| 영역 | 예시 |
|------|------|
| 파일 시스템 | `.java` 파일 |
| 빌드 시스템 | `javac` |
| 프로세스 | `java` / `javac` |
| System Call | `open` / `read` / `write` |

```bash
find . -name "*.java"
ps -ef | grep javac
```

### Runtime

소스 코드는 런타임에서 클래스, 객체, 스레드, 메모리 구조로 변환됩니다.

| 영역 | 의미 |
|------|------|
| Heap | 객체 상태 |
| Stack | 메서드 호출 |
| GC | 객체 생명주기 |
| JIT | 실행 최적화 |

### Kubernetes

K8s에서는 소스 코드가 컨테이너 이미지의 원본이 됩니다.

```
Source Code
→ Build
→ JAR/WAR
→ Container Image
→ Pod Execution
```

| 대상 | 의미 |
|------|------|
| CI Pipeline | 컴파일 |
| Container Build | 패키징 |
| Pod Logs | 실행 결과 |
| Metrics | 코드 성능 영향 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*