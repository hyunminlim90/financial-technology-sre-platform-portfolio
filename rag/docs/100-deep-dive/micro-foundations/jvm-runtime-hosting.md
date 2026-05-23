# JVM 런타임 호스팅 (JVM Runtime Hosting)

> 정독: 0회

## 1. 이 기술이 무엇인가

JVM 런타임 호스팅은:

> 운영체제가 제공하는 프로세스·메모리·CPU 자원 위에서 JVM이 독립적인 실행 환경(Runtime Environment)을 구성하고, 그 내부에서 자바 애플리케이션의 실행 생애 주기를 관리하는 구조

**핵심 구성:**

| 구성 | 역할 |
|------|------|
| 운영체제(OS) | 물리 자원 제공 |
| JVM | 가상 실행 환경 구성 |
| 애플리케이션 | JVM 내부에서 실행 |

> JVM은 단순 실행기가 아니라 애플리케이션 실행 환경 자체를 호스팅합니다.

---

## 2. 시스템 어디에서 등장하는가

**위치:**

```
Hardware
→ Operating System
→ JVM Process
→ Runtime Data Area
→ Application Threads
→ Application Logic
```

**관여 요소:**

| 계층 | 역할 |
|------|------|
| OS Kernel | 프로세스/메모리 관리 |
| JVM Runtime | 가상 실행 환경 |
| Class Loader | 클래스 적재 |
| Execution Engine | 바이트코드 실행 |
| GC | 메모리 회수 |
| Thread System | 실행 흐름 관리 |

> JVM 런타임 호스팅은 OS와 애플리케이션 사이의 실행 환경 계층입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

### Memory (가장 중요)

JVM은 직접 다음을 관리합니다:

- Heap
- Stack
- Metaspace
- Direct Memory
- GC Region

### CPU (매우 중요)

CPU 사용 영역:

- bytecode execution
- JIT compilation
- GC execution
- thread scheduling
- synchronization

### Disk (간접 영향)

예시:

- class loading
- JAR reading
- logging
- heap dump
- GC log

### Network (애플리케이션 동작에 따라)

예시:

- socket I/O
- RPC
- distributed systems

---

## 4. 왜 중요한가

현대 자바 시스템의 안정성과 성능 대부분이 JVM 런타임 호스팅 품질에 의해 결정됩니다.

| 영역 | 영향 |
|------|------|
| 메모리 안정성 | GC/Heap 관리 |
| 성능 | JIT 최적화 |
| 동시성 | thread runtime |
| 장애 복구 | exception/runtime handling |
| 확장성 | runtime resource control |
| 이식성 | platform abstraction |

> JVM은 단순한 "실행기"가 아니라 운영 환경 관리자입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

**대표 장애:**

| 장애 | 원인 |
|------|------|
| OutOfMemoryError | Heap exhaustion |
| GC Pause | excessive allocation |
| CPU Spike | JIT/GC overload |
| Thread Exhaustion | thread explosion |
| Metaspace OOM | excessive class loading |
| Native Memory Leak | direct memory misuse |
| Container OOMKill | JVM/container mismatch |

특히 **Kubernetes 환경**에서는 다음 항목의 불일치가 매우 중요합니다:

- JVM Heap 설정
- cgroup memory limit
- GC 정책

---

## 6. 핵심 메커니즘

### (1) JVM은 OS 위의 하나의 프로세스

JVM 자체는 운영체제 입장에서 일반 프로세스입니다.

```bash
java -jar app.jar
```

실행 시 발생하는 것들:

- PID 생성
- virtual memory 확보
- thread 생성
- file descriptor 확보

### (2) Runtime Data Area 생성

JVM 시작 시 주요 메모리 구조가 생성됩니다.

| 영역 | 역할 |
|------|------|
| Heap | 객체 저장 |
| Stack | 메서드 실행 |
| Metaspace | 클래스 메타데이터 |
| PC Register | 실행 위치 |
| Native Stack | JNI/native call |

### (3) Class Loading Hosting

JVM은 `.class` 파일을 메모리에 적재합니다.

```
Load → Verify → Link → Initialize
```

이후 Runtime 내부에서 실행 가능한 상태가 됩니다.

### (4) Thread Runtime Hosting

JVM은 애플리케이션 thread를 관리합니다.

포함 항목:

- stack allocation
- monitor synchronization
- thread lifecycle
- blocking/wakeup

실제 OS thread와 연결됩니다.

### (5) Garbage Collection

역할:

- unused object detection
- heap compaction
- memory reclamation

> 메모리 생명주기를 JVM이 지속 관리합니다.

### (6) JIT Compilation

반복 실행 코드에 대해 수행:

```
Bytecode → profiling → hotspot detection → native compilation
```

> 런타임 중 동적으로 성능 최적화가 발생합니다.

### (7) Resource Boundary Management

| 자원 | 제한 방식 |
|------|----------|
| Memory | heap size |
| CPU | scheduler/cgroup |
| Thread | OS limit |
| FD | ulimit |

> JVM 런타임 호스팅은 OS 자원과 직접 연결됩니다.

---

## 7. Linux / Runtime / K8s에서 관측 방법

### Linux

```bash
ps -ef | grep java
top -H -p <pid>
cat /proc/<pid>/status
```

관측 항목: thread count, RSS memory, virtual memory, CPU usage

### Runtime 도구

| 도구 | 역할 |
|------|------|
| jstack | thread 상태 |
| jmap | heap 상태 |
| jstat | GC 상태 |
| jcmd | runtime diagnostics |
| JFR | runtime profiling |

### Kubernetes

```bash
kubectl top pod
kubectl describe pod
```

| 항목 | 의미 |
|------|------|
| Memory Limit | Heap 상한 영향 |
| CPU Limit | GC/JIT 영향 |
| OOMKilled | JVM memory mismatch |
| Restart Count | runtime instability |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*