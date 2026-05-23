# 자바 클래스 파일 (Java Class File)

> 정독: 0회

## 1. 이 기술이 무엇인가

자바 클래스 파일(`.class`)은:

> 자바 소스코드를 JVM(Java Virtual Machine)이 실행할 수 있는 **표준 바이트코드(binary format)로 변환한 결과물**

### 핵심 특징

- 플랫폼 독립적
- JVM 명세 기반
- 바이너리 포맷
- 스택 기반 명령 구조
- 런타임 로딩 가능
- 동적 링크 가능

### 중요한 핵심

> `.class` 파일은 CPU가 직접 실행하는 기계어가 **아님**  
> 실행 대상은 **JVM**

### 실행 흐름

```
.java
→ javac
→ .class
→ JVM Class Loader
→ Bytecode Verification
→ Execution Engine
→ JIT Compiler
→ Native Machine Code
→ CPU
```

<details>
<summary>Deep Dive</summary></br>

Java Source File(자바 소스 파일) [[M]](../../100-deep-dive/micro-foundations/java-source-file.md)  
Java Source Code(자바 소스 코드) [[M]](../../100-deep-dive/micro-foundations/java-source-code.md)  
Java Virtual Machine(자바 가상 머신) [[M]](../../100-deep-dive/micro-foundations/java-virtual-machine.md)  
Runtime Execution(런타임 실행) [[M]](../../100-deep-dive/micro-foundations/runtime-execution.md)  
Java Virtual Machine Instruction Set(자바 가상 머신 명령어 세트) [[M]](../../100-deep-dive/micro-foundations/java-virtual-machine-instruction-set.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

클래스 파일은 JVM 기반 시스템 전반에서 등장합니다.

**대표 영역:**

- 서버 애플리케이션
- 배치 시스템
- CLI 도구
- JVM 기반 데이터 플랫폼
- Android 초기 Dalvik ecosystem
- distributed runtime
- plugin system
- dynamic loading system

### 런타임 위치

실행 시 다음 순서로 이동하며 사용됩니다.

```
Disk → Page Cache → JVM Metaspace → Heap → Code Cache
```

### 커널과 연결

클래스 파일 로딩 시 OS 메커니즘 사용:

- `file I/O`
- `mmap`
- `page fault`
- `page cache`

### Kubernetes

container image 내부 jar/class 형태로 포함됩니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 영향이 큰 자원: **CPU + Memory + Disk**

### CPU 영향 — 매우 큼

발생 원인:

- bytecode verification
- interpretation
- JIT compilation
- dynamic linking

### Memory 영향 — 중요

| 영역 | 설명 |
|---|---|
| **Metaspace** | 클래스 메타데이터 저장 |
| **Constant Pool** | symbolic 참조 저장 |
| **Method Metadata** | 메서드 정보 |
| **Code Cache** | JIT 컴파일 결과 |

### Disk 영향

초기 class loading 시 영향 발생. 특히 **startup latency**, **cold start**와 연결.

### Network 영향

일반적으로 직접 영향은 적으나, remote class loading / distributed artifact download 환경에서는 관련 가능.

---

## 4. 왜 중요한가

JVM 전체 실행 구조의 핵심 기반입니다.

| 이유 | 설명 |
|---|---|
| **1. 플랫폼 독립성** | 동일 `.class` 실행 가능 |
| **2. 동적 로딩** | 런타임 class loading 가능 |
| **3. 보안 검증** | bytecode verifier 제공 |
| **4. 런타임 최적화** | JIT compiler 최적화 기반 제공 |
| **5. 대규모 서버 운영** | 현대 JVM ecosystem 핵심 실행 단위 |

---

## 5. 실제 장애와 어떤 관련이 있는가

### ClassLoader Leak

대표적 메모리 장애 → **Metaspace OOM** 발생 가능.

### Corrupted Class File

손상된 bytecode 발생 시:

```
ClassFormatError
VerifyError
```

### Excessive Dynamic Class Generation

runtime proxy / framework / plugin 시스템에서:

- Metaspace 증가
- JIT pressure
- GC pressure

발생 가능.

### Startup Delay

대량 class loading 시:

- disk I/O 증가
- CPU spike
- page cache miss

발생 가능.

### JIT Compilation Spike

hot method 증가 시 CPU 사용량 증가.

### Container Memory Pressure

class metadata 증가 시 container memory 사용 증가.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

핵심 메커니즘은 **7개**입니다.

| # | 메커니즘 | 설명 |
|---|---|---|
| 1 | **Binary Structure** | `.class`는 엄격한 binary format 사용 |
| 2 | **Magic Number** | 맨 앞 4바이트: `0xCAFEBABE` |
| 3 | **Constant Pool** | 모든 symbolic metadata 저장 (class name, method name, field reference, string literal) |
| 4 | **Bytecode Instruction** | 1-byte opcode 기반 (`iload`, `invokevirtual`, `getfield`, `return`) |
| 5 | **Stack-based Execution** | register 기반이 아닌 operand stack 기반 실행 구조 |
| 6 | **Dynamic Linking** | 실행 중 실제 메모리 참조 연결 |
| 7 | **Verification** | JVM이 unsafe bytecode 차단 (stack overflow, invalid jump, illegal memory access, type mismatch) |

### 핵심 흐름

```
.class
→ Class Loader
→ Verification
→ Linking
→ Initialization
→ Bytecode Execution
→ JIT Optimization
→ Native Code
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### 클래스 파일 확인

```bash
file MyClass.class
```

### bytecode 디스어셈블

```bash
javap -c MyClass
```

### jar 내부 확인

```bash
jar tf app.jar
```

### JVM class loading 로그

```bash
-verbose:class
# 또는
-Xlog:class+load
```

### Metaspace 확인

```bash
jcmd VM.native_memory
```

### JIT 상태

```bash
-XX:+PrintCompilation
```

### Linux file I/O 추적

```bash
strace -f -e openat java ...
```

### mmap 확인

```bash
pmap <pid>
```

### Kubernetes

container image 내부 확인:

```bash
kubectl exec <pod> -- find / -name "*.class"
```

pod memory 증가 확인:

```bash
kubectl top pod
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*