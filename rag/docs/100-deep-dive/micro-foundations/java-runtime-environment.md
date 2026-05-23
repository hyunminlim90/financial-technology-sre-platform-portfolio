# 자바 런타임 환경 (JRE)

> 정독: 0회

## 1. 이 기술이 무엇인가

JRE(Java Runtime Environment)는:

> 자바 바이트코드(`.class`)를 실제 운영체제와 CPU 위에서 실행시키는 **런타임 실행 환경**

### 핵심 구성

JRE는 보통 다음 구성 요소를 포함합니다.

- **JVM** (Java Virtual Machine)
- **표준 클래스 라이브러리**
- **런타임 실행 모듈**
- **GC** (Garbage Collector)
- **JIT Compiler**

### 중요한 구분

| 구분 | 설명 | 포함 항목 |
|------|------|-----------|
| **JDK** | 개발 도구 포함 | `javac`, debugger, build tools |
| **JRE** | 실행 환경 중심 | "실행만 가능" |

### 핵심

> JRE는 **"자바 프로그램 실행 인프라"** 입니다.

---

## 2. 시스템 어디에서 등장하는가

JRE는 다음 환경에서 등장합니다.

- 서버 프로세스
- CLI 애플리케이션
- 배치 시스템
- 데이터 플랫폼
- 금융 시스템
- 게임 서버
- Android 이전 Java ecosystem

### 운영체제 관점

실제로는 **일반 프로세스(process)** 로 실행됩니다.

### 커널과 연결

JRE 내부 JVM은 다음을 위해 OS syscall을 사용합니다.

- `thread` 생성
- `memory allocation`
- `file I/O`
- `socket I/O`
- `epoll` / `select`
- `mmap`

### Kubernetes

container 내부 java process 형태로 동작합니다.

### Cloud / Infra

다음 항목과 직접 연결됩니다.

- JVM heap
- CPU quota
- cgroup memory
- GC pause

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 영향이 큰 자원: **CPU + Memory**

### CPU 영향 — 매우 큼

발생 원인:

- JIT compilation
- GC
- thread scheduling
- synchronization
- bytecode execution

### Memory 영향 — 핵심 자원

| 영역 | 설명 |
|------|------|
| **Heap** | 객체 저장 영역 |
| **Metaspace** | 클래스 메타데이터 |
| **Stack** | 스레드별 콜 스택 |
| **Direct Memory** | NIO 등 네이티브 버퍼 |

### Disk 영향

- class loading
- logging
- file I/O

### Network 영향

- socket buffer
- async I/O
- TLS

---

## 4. 왜 중요한가

현대 서버 런타임 핵심 중 하나입니다.

| 이유 | 설명 |
|------|------|
| **1. 플랫폼 독립성** | 동일 bytecode 실행 가능 |
| **2. 메모리 관리 자동화** | GC 제공 |
| **3. 런타임 최적화** | JIT compiler가 hot code 최적화 수행 |
| **4. 안정성** | sandbox, verifier, managed runtime 제공 |
| **5. 대규모 서버 운영** | 금융/플랫폼/대규모 트래픽 환경에서 광범위 사용 |

---

## 5. 실제 장애와 어떤 관련이 있는가

### OOM (Out Of Memory) — 대표 장애

```
java.lang.OutOfMemoryError
```

### GC Pause

STW(Stop-The-World) latency 발생.

### CPU Spike

JIT / GC / thread contention 영향.

### Metaspace Exhaustion

class loader leak 시 발생.

### File Descriptor Leak

native I/O 자원 고갈 가능.

### Container OOMKill

Kubernetes memory limit 초과 시 발생.

### Thread Explosion

native thread exhaustion 발생 가능.

### 핵심

> 대규모 Java 장애 상당수는  
> **"런타임 자원 관리 실패"** 와 연결됩니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

핵심 메커니즘은 **8개** 입니다.

| # | 메커니즘 | 설명 |
|---|----------|------|
| 1 | **Bytecode 실행** | `.class` bytecode를 JVM이 읽음 |
| 2 | **Class Loading** | 필요한 class 동적 적재 |
| 3 | **Verification** | bytecode 안전성 검사 |
| 4 | **Execution Engine** | 초기에는 interpreter 실행 |
| 5 | **JIT Compilation** | hot path를 native code로 변환 |
| 6 | **Heap Allocation** | 객체 heap 저장 |
| 7 | **Garbage Collection** | 불필요 객체 제거 |
| 8 | **Native Interface** | OS syscall 및 native library 연결 |

### 핵심 흐름

```
.class
→ class loader
→ verifier
→ execution engine
→ JIT optimization
→ native machine code execution
→ syscall
→ kernel
→ hardware
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### 프로세스 확인

```bash
ps -ef | grep java
```

### 메모리 확인

```bash
top
htop
```

### JVM 메모리

```bash
jcmd
jmap
jstat
```

### GC 상태

```bash
-Xlog:gc
```

### Thread 상태

```bash
jstack
```

### File Descriptor

```bash
lsof -p <pid>
```

### Native Memory

```bash
jcmd VM.native_memory
```

### Kubernetes

| 목적 | 명령어 |
|------|--------|
| pod memory 확인 | `kubectl top pod` |
| OOMKill 확인 | `kubectl describe pod` |
| container limit 확인 | `resources.limits.memory` |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*