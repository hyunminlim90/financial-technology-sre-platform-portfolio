# 자바 가상 머신 명령어 세트(Java Virtual Machine Instruction Set)

> 정독: 0회

## 1. 이 기술이 무엇인가

JVM 명령어 세트는:

> JVM이 실행하는 **표준화된 바이트코드 명령 체계**

자바 소스코드는 컴파일 이후 `.class` 파일 내부의 바이트코드로 변환되며, JVM은 이 바이트코드를 명령어 단위로 실행합니다.

| 특징 | 설명 |
|------|------|
| 가상 명령어 체계 | 실제 CPU 명령어가 아님 |
| 플랫폼 독립성 | 어떤 OS/CPU에서도 동일 동작 |
| 바이트코드 기반 | `.class` 내부 저장 |
| 스택 기반 구조 | Operand Stack 중심 연산 |
| Opcode 중심 | 1-byte 명령 체계 |

**JVM 명령어 세트는 JVM이 이해하는 실행 언어입니다.**

---

## 2. 시스템 어디에서 등장하는가

```
Source Code
→ Compiler
→ Bytecode (.class)
→ JVM Instruction Set
→ Execution Engine
→ Native Machine Code
→ CPU
```

| 계층 | 역할 |
|------|------|
| Compiler | 바이트코드 생성 |
| Class Loader | 메모리 로딩 |
| Bytecode Verifier | 안전성 검증 |
| Execution Engine | 명령 실행 |
| JIT Compiler | 네이티브 코드 변환 |

**JVM 명령어 세트는 컴파일 이후와 실제 CPU 실행 사이에 존재합니다.**

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

### CPU

가장 직접적인 영향이 있습니다. 명령 해석, JIT 컴파일, 연산 수행, 메서드 호출, 분기 처리 모두 CPU 중심입니다.

### Memory

JVM 명령어는 Operand Stack, Local Variable Table, Heap 객체, Method Area를 지속적으로 접근합니다.

### Disk

`.class` 파일 로딩, JAR 읽기, Dynamic Class Loading 등에서 간접 영향이 있습니다.

### Network

원격 클래스 로딩, RPC 실행, 분산 시스템 처리에서 간접적으로 연결됩니다.

---

## 4. 왜 중요한가

실제 JVM 성능과 동작 방식이 이 명령어 세트 위에서 결정됩니다.

| 영역 | 영향 |
|------|------|
| 실행 속도 | JIT 최적화 |
| 메모리 사용 | Stack/Heap 접근 |
| GC 영향 | 객체 생성 패턴 |
| CPU 효율 | Instruction Hotspot |
| 보안 | Bytecode Verification |
| 이식성 | 플랫폼 독립 실행 |

**JVM 런타임의 핵심 실행 단위입니다.**

---

## 5. 실제 장애와 어떤 관련이 있는가

| 장애 | 관련 메커니즘 |
|------|-------------|
| CPU Spike | Hot Method 반복 실행 |
| GC 폭증 | excessive object creation |
| StackOverflowError | stack instruction recursion |
| VerifyError | 잘못된 bytecode |
| JIT Thrashing | 과도한 recompilation |
| Warm-up 지연 | interpreted execution |

---

## 6. 핵심 메커니즘

### (1) Opcode 기반 실행

모든 JVM 명령은 Opcode 기반입니다.

| Opcode | 의미 |
|--------|------|
| `iload` | int 로드 |
| `istore` | int 저장 |
| `iadd` | int 덧셈 |
| `invokevirtual` | 메서드 호출 |
| `return` | 반환 |

실제 `.class` 내부는 이런 명령 시퀀스로 구성됩니다.

### (2) 스택 기반 실행 구조

JVM은 Register 기반이 아니라 **Stack 기반**입니다.

```
load
→ push stack
→ execute
→ pop result
```

예시: `iload_1` → `iload_2` → `iadd` 순서로 값을 로드하고 Stack에 push한 뒤 덧셈을 수행합니다.

### (3) Operand Stack

각 메서드는 실행 시 **Operand Stack**과 **Local Variables**를 가집니다. 연산은 대부분 Operand Stack 위에서 수행됩니다.

### (4) Bytecode Verification

JVM은 실행 전에 명령어 안정성을 검사합니다.

| 검사 | 의미 |
|------|------|
| 타입 일치 | Type Safety |
| Stack 상태 | Stack Underflow 방지 |
| 접근 권한 | Illegal Access 방지 |
| Jump 안정성 | 잘못된 분기 차단 |

이 단계가 **JVM 보안의 핵심**입니다.

### (5) 인터프리터 vs JIT

```
Bytecode
→ Interpreter
→ Hotspot Detection
→ JIT Compilation
→ Native Execution
```

초기에는 명령어를 하나씩 해석 실행하다가, 반복 코드 발견 시 Native Code로 컴파일합니다.

### (6) 메서드 호출 명령

| 명령 | 의미 |
|------|------|
| `invokevirtual` | 일반 메서드 |
| `invokespecial` | 생성자/private |
| `invokestatic` | static 호출 |
| `invokeinterface` | interface 호출 |

이 호출 구조가 런타임 다형성을 만듭니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

직접 Opcode를 보진 않지만 실행 결과를 관측합니다.

```bash
top
perf top
strace -p <pid>
```

관측 대상: CPU 사용률, syscall 빈도, thread activity

### Runtime

| 영역 | 도구 |
|------|------|
| Bytecode | `javap` |
| JIT 상태 | JIT logs |
| Method Hotspot | profiler |
| Stack 상태 | thread dump |
| Class Loading | runtime metrics |

```bash
javap -c MyClass
```

### Kubernetes

| 메트릭 | 의미 |
|--------|------|
| CPU Limit 초과 | bytecode execution hotspot |
| Memory 증가 | object allocation 증가 |
| GC pause | runtime pressure |
| Pod Restart | runtime crash |
| OOMKilled | memory exhaustion |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*