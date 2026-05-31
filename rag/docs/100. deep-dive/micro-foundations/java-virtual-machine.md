# 자바 가상 머신(Java Virtual Machine, JVM)

> 정독: 0회

## 1. 이 기술이 무엇인가

JVM은:

> 자바 바이트코드(`.class`)를 실행하기 위한 **가상 실행 환경(Runtime Execution Environment)**

실제 CPU가 아니라 소프트웨어로 구현된 가상 머신이며, 바이트코드 실행 엔진, 메모리 관리 시스템, 스레드 실행 환경을 포함합니다.

| 역할 | 설명 |
|------|------|
| 바이트코드 실행 | `.class` 실행 |
| 메모리 관리 | Heap/Stack 관리 |
| GC 수행 | 객체 회수 |
| 플랫폼 추상화 | OS/CPU 차이 은닉 |
| 스레드 실행 | 동시성 관리 |

**JVM은 자바 프로그램의 실제 런타임 운영 시스템입니다.**

<details>
<summary>Deep Dive</summary></br>

Java Class File Format(자바 클래스 파일 포맷) [[M]](../../100-deep-dive/micro-foundations/java-class-file-format.md)  
Instruction Dispatch(인스트럭션 디스패치) [[M]](../../100-deep-dive/micro-foundations/instruction-dispatch.md)  
Bytecode Interpretation(바이트코드 인터프리테이션) [[M]](../../100-deep-dive/micro-foundations/bytecode-interpretation.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

JVM은 컴파일 이후 **실제 실행 단계**에서 등장합니다.

```
Java Source Code
→ javac
→ Bytecode (.class)
→ JVM
→ Native Machine Code
→ OS Kernel
→ Hardware
```

| 계층 | 역할 |
|------|------|
| Application | 바이트코드 제공 |
| JVM | 실행 환경 |
| OS | 프로세스 관리 |
| Kernel | 자원 제어 |
| Hardware | 실제 연산 |

**JVM은 애플리케이션과 OS 사이의 런타임 계층입니다.**

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**Memory와 CPU 영향이 매우 큽니다.**

### Memory

가장 중요합니다. JVM은 직접 메모리를 관리합니다.

| 영역 | 역할 |
|------|------|
| Heap | 객체 저장 |
| Stack | 메서드 호출 |
| Metaspace | 클래스 메타데이터 |
| Code Cache | JIT 코드 저장 |

GC 정책에 따라 메모리 사용 패턴이 크게 달라집니다.

### CPU

JIT 컴파일, GC, Thread Scheduling, Bytecode Execution이 CPU 사용의 주요 원인입니다. GC 폭주 시 CPU 사용량이 급증할 수 있습니다.

### Disk

상대적으로 적지만 Class Loading, JAR 읽기, GC 로그, Heap Dump 등과 관련됩니다.

### Network

JVM 자체보다는 JVM 위에서 실행되는 애플리케이션이 주로 사용합니다. NIO, Socket, TLS 등의 네트워크 API는 JVM이 제공합니다.

---

## 4. 왜 중요한가

JVM은 자바 시스템의 성능, 안정성, 메모리 효율, GC 특성, 스레드 처리량, 장애 특성을 결정합니다.

| 문제 | JVM 관련 원인 |
|------|-------------|
| OOM | Heap 부족 |
| GC Pause | GC 설정 문제 |
| CPU Spike | GC/JIT 폭주 |
| Thread Explosion | 스레드 과다 |
| Metaspace OOM | 클래스 로딩 누수 |

---

## 5. 실제 장애와 어떤 관련이 있는가

| 장애 | JVM 원인 |
|------|---------|
| OutOfMemoryError | Heap 고갈 |
| Full GC 반복 | 메모리 압박 |
| Stop-The-World | GC 정지 |
| High CPU | GC/JIT 과부하 |
| Thread Leak | 스레드 누수 |
| ClassLoader Leak | Metaspace 증가 |

> 운영 환경에서는 **애플리케이션 문제와 JVM 문제를 분리해서** 봐야 합니다.

---

## 6. 핵심 메커니즘

### (1) 클래스 로딩

JVM은 `.class` 파일을 메모리에 적재합니다.

단계: Loading → Linking → Initialization

결과적으로 클래스 메타데이터가 런타임 메모리에 배치됩니다.

### (2) 런타임 데이터 영역

JVM은 자체 메모리 구조를 가집니다.

| 영역 | 특징 |
|------|------|
| Heap | 객체 저장 |
| Stack | 스레드별 호출 프레임 |
| PC Register | 현재 실행 위치 |
| Metaspace | 클래스 정보 |
| Native Stack | JNI/native 호출 |

### (3) 바이트코드 실행

JVM은 바이트코드를 두 가지 방식으로 실행합니다.

- **초기:** Interpreter 방식으로 실행
- **반복 코드:** JIT Compilation 수행 → Native Machine Code 생성

자주 실행되는 코드는 실제 CPU 명령으로 최적화됩니다.

### (4) Garbage Collection

JVM 핵심 기능으로, 참조 추적 → 사용하지 않는 객체 식별 → 메모리 회수 순으로 동작합니다.

**대표 개념:** Young Generation, Old Generation, Minor GC, Full GC

GC는 JVM 성능의 핵심입니다.

### (5) 스레드 관리

JVM은 멀티스레드를 관리하며 Thread Stack, Synchronization, Monitor Lock, Memory Visibility를 포함합니다. OS Thread와 연결됩니다.

### (6) OS와의 연결

JVM은 결국 OS 위에서 실행되는 하나의 프로세스입니다.

```
JVM
→ System Call
→ Kernel
→ CPU / Memory / Disk / Network
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

| 명령 | 의미 |
|------|------|
| `ps` | JVM 프로세스 |
| `top` / `htop` | CPU/메모리 |
| `pmap` | 메모리 맵 |
| `lsof` | 열린 파일 |
| `strace` | System Call |

```bash
ps -ef | grep java
```

### JVM Runtime

| 도구 | 역할 |
|------|------|
| `jstat` | GC 상태 |
| `jmap` | Heap 분석 |
| `jstack` | Thread Dump |
| `jcmd` | JVM 명령 |
| JFR | Runtime 이벤트 |

### Kubernetes

K8s에서는 JVM이 컨테이너 내부 프로세스로 실행됩니다.

| 대상 | 의미 |
|------|------|
| Pod Memory | Heap 사용량 |
| CPU Limit | GC 영향 |
| OOMKilled | 메모리 초과 |
| GC Log | GC 상태 |
| Thread Count | 동시성 상태 |

> **중요:** 컨테이너 메모리 제한과 JVM Heap 설정이 맞지 않으면 OOMKilled가 발생합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*