# 자바 클래스 파일 포맷 (Java Class File Format)

> 정독: 0회

## 1. 이 기술이 무엇인가

자바 클래스 파일 포맷은:

> JVM이 클래스를 로드·검증·링크·실행할 수 있도록 정의된 **표준 바이너리 구조**

`.java` 소스코드가 컴파일되면 `.class` 파일이 생성되며, 이 파일 내부에는 다음 정보가 포함됩니다.

| 포함 정보 | 설명 |
|-----------|------|
| 클래스 메타데이터 | 클래스 이름, 부모 클래스 |
| 필드 정보 | 변수 정의 |
| 메서드 정보 | 함수 정의 |
| 바이트코드 | JVM 명령어 |
| 상수 풀 | 문자열/참조 정보 |
| 접근 제어 | public/final/interface 등 |

> 클래스 파일 포맷은 JVM 실행을 위한 **표준 실행 컨테이너**입니다.

---

## 2. 시스템 어디에서 등장하는가

**위치:**

```
Java Source Code
→ javac Compiler
→ .class File
→ Class Loader
→ JVM Runtime
→ Execution Engine
```

**관여 계층:**

| 계층 | 역할 |
|------|------|
| Compiler | 클래스 파일 생성 |
| Filesystem | .class 저장 |
| Class Loader | 메모리 적재 |
| Bytecode Verifier | 무결성 검사 |
| Execution Engine | 바이트코드 실행 |

> 클래스 파일 포맷은 컴파일 결과물과 JVM 런타임 사이의 **표준 인터페이스**입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

### Disk (가장 직접적)

다음이 모두 디스크 기반입니다:

- `.class` / `.jar`
- library loading
- filesystem access

### Memory (매우 중요)

클래스 파일은 메모리 적재 후 다음으로 변환됩니다:

- Method Area
- Metaspace
- Constant Pool

### CPU

바이트코드 검증과 실행에 사용됩니다:

- verification
- linking
- JIT compilation

### Network (간접 영향)

- remote class loading
- distributed runtime
- dynamic module fetch

---

## 4. 왜 중요한가

자바 **플랫폼 독립성의 핵심**입니다.

| 영역 | 영향 |
|------|------|
| 플랫폼 독립성 | JVM 표준 실행 |
| 보안 | bytecode verification |
| 성능 | constant pool optimization |
| 런타임 안정성 | strict binary format |
| 동적 로딩 | runtime linking |

> JVM은 클래스 파일 포맷을 기반으로 전체 런타임을 구성합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

**대표 장애:**

| 장애 | 원인 |
|------|------|
| ClassFormatError | 잘못된 binary format |
| UnsupportedClassVersionError | JVM 버전 불일치 |
| VerifyError | invalid bytecode |
| NoClassDefFoundError | class loading 실패 |
| ClassCircularityError | 순환 참조 |
| LinkageError | linking mismatch |

실제 운영 환경에서 흔한 문제:

- 빌드 버전 충돌
- jar corruption
- incompatible dependency

---

## 6. 핵심 메커니즘

### (1) 클래스 파일은 바이너리 구조체

텍스트 파일이 아니며, 내부는 엄격한 binary layout입니다.

**기본 구조:**

```
Magic Number
Version
Constant Pool
Access Flags
This Class
Super Class
Interfaces
Fields
Methods
Attributes
```

### (2) Magic Number

파일 시작 식별자:

```
0xCAFEBABE
```

JVM은 이것으로 정상 class 여부 및 JVM binary format 여부를 확인합니다.

### (3) Constant Pool

가장 중요한 영역 중 하나입니다.

| 항목 | 예 |
|------|----|
| 문자열 | `"Hello"` |
| 클래스 이름 | `java/lang/Object` |
| 메서드 이름 | `println` |
| 필드 참조 | `System.out` |

실제 바이트코드는 값 자체보다 **Constant Pool Index를 참조**합니다.

### (4) Method + Bytecode

메서드 내부에는 실제 JVM 명령어가 저장됩니다.

```
aload_0
invokevirtual
return
```

JVM Execution Engine이 이를 해석 실행합니다.

### (5) Bytecode Verification

클래스 로딩 시 JVM은 검증을 수행합니다.

| 검사 | 의미 |
|------|------|
| type safety | 타입 안정성 |
| stack consistency | stack 무결성 |
| illegal jump | 잘못된 분기 차단 |
| invalid access | 접근 위반 차단 |

이 단계가 **JVM 보안 핵심**입니다.

### (6) Linking

클래스는 단독 실행되지 않습니다. 런타임 시:

```
symbolic reference → actual runtime reference
```

로 변환됩니다. Constant Pool 기반 동적 연결이 발생합니다.

### (7) Attributes 영역

클래스 파일에는 추가 메타데이터가 존재합니다.

| Attribute | 의미 |
|-----------|------|
| Code | bytecode |
| LineNumberTable | 디버깅 |
| Exceptions | 예외 정보 |
| Signature | generic type |
| RuntimeAnnotations | annotation metadata |

현대 프레임워크들은 이 metadata를 많이 사용합니다.

---

## 7. Linux / Runtime / K8s에서 관측 방법

### Linux

```bash
file MyClass.class
xxd MyClass.class
javap -verbose MyClass
```

### Runtime 도구

| 도구 | 역할 |
|------|------|
| javap | bytecode 분석 |
| jclasslib | class structure |
| ASM | bytecode manipulation |
| JFR | class loading trace |

### Kubernetes

직접 보진 않지만 간접 영향이 큽니다.

| 문제 | 영향 |
|------|------|
| huge dependency graph | startup delay |
| excessive class loading | metaspace 증가 |
| broken jar | pod startup failure |
| class version mismatch | runtime crash |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*