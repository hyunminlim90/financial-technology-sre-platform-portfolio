# 실행 가능 상태 관리 (Executable State Management)

> 정독: 0회

## 1. 이 기술이 무엇인가

실행 가능 상태 관리는:

> 프로그램이 실행 중일 때 필요한 모든 실행 컨텍스트를 메모리 안에서 일관되고 유효한 상태로 유지·추적·갱신하는 **런타임 관리 체계**

| 요소 | 역할 |
|------|------|
| Program Counter | 현재 실행 위치 |
| Stack Frame | 함수 실행 상태 |
| Heap Object | 동적 데이터 상태 |
| Thread Context | 스레드 실행 상태 |
| Register State | CPU 연산 상태 |

> 프로그램이 "지금 어디까지 실행되었고, 어떤 데이터를 가지고 있으며, 다음에 무엇을 실행해야 하는가"를 유지하는 **런타임 상태 관리 시스템**입니다.

---

## 2. 시스템 어디에서 등장하는가

**실행 위치:**

```
Program
→ Runtime
→ Execution Engine
→ State Management
→ CPU Execution
```

**대표 등장 영역:**

| 계층 | 역할 |
|------|------|
| JVM Runtime | managed execution state |
| CLR Runtime | runtime context management |
| OS Scheduler | process/thread state |
| CPU Microarchitecture | register execution state |
| Async Runtime | coroutine/task state |

---

## 3. 어떤 자원에 가장 영향이 큰가

### Memory (가장 직접적)

실행 상태 대부분이 메모리에 존재합니다:

- stack frame
- heap object
- thread stack
- local variable
- runtime metadata

### CPU (매우 중요)

CPU는 다음을 계속 변경합니다:

- PC register
- execution context
- branch state
- scheduling state

### Disk (간접 영향)

- swap
- heap dump
- checkpoint
- runtime persistence

### Network (분산 런타임에서 중요)

- distributed session state
- RPC execution context
- tracing context propagation

---

## 4. 왜 중요한가

**실행 연속성과 안정성을 유지**하기 때문입니다.

상태 관리가 실패하면 다음이 발생할 수 있습니다:

- 실행 위치 손실
- 데이터 손상
- 스레드 붕괴
- 메모리 오염
- 프로세스 비정상 종료

| 항목 | 영향 |
|------|------|
| execution continuity | 실행 지속성 |
| memory integrity | 메모리 무결성 |
| thread safety | 스레드 안정성 |
| recovery capability | 복구 가능성 |
| runtime stability | 런타임 안정성 |

---

## 5. 실제 장애와 어떤 관련이 있는가

**대표 장애:**

| 장애 | 원인 |
|------|------|
| stack overflow | stack state explosion |
| memory corruption | invalid state update |
| deadlock | synchronization state conflict |
| thread leak | orphan runtime state |
| OOM | heap state exhaustion |
| invalid pointer access | corrupted execution state |
| GC failure | unreachable state tracking failure |
| process crash | runtime context corruption |

특히 다음 환경에서 중요합니다:

- 고동시성 시스템
- async runtime
- VM runtime
- container runtime
- distributed transaction runtime

---

## 6. 핵심 메커니즘

### (1) Program Counter(PC) 관리

PC는 **current instruction location**을 유지합니다. 실행 흐름 유지의 핵심입니다.

### (2) Stack Frame 관리

메서드/함수 호출 및 리턴 시:

```
호출: new frame push
리턴: frame pop
```

frame 내부 포함 항목:

- local variable
- operand stack
- return address

### (3) Heap Object 상태 유지

runtime은 다음을 계속 추적합니다:

- object allocation
- object reachability
- object lifecycle

GC와 직접 연결됩니다.

### (4) Thread Context 관리

runtime은 다음 상태를 유지합니다:

- running
- blocked
- waiting
- terminated

### (5) Execution Continuity 유지

runtime은 **instruction sequence continuity**를 유지해야 합니다.

> 이전 상태와 다음 상태가 끊기면 안 됩니다.

### (6) Synchronization State 관리

runtime이 다음 상태를 추적합니다:

- monitor
- mutex
- semaphore
- lock ownership

### (7) GC와 상태 관리 연동

GC는 다음을 분석합니다:

```
reachable state  vs  unreachable state
```

> 실행 상태 관리 없이는 **GC도 불가능**합니다.

### (8) Runtime Metadata 유지

실행 엔진이 유지하는 메타데이터:

- type metadata
- method table
- symbol table
- execution metadata

### (9) Context Switching 연동

OS scheduler와 연결됩니다. 스레드 전환 시:

```
register state save → stack state restore → PC restore
```

---

## 7. Linux / Runtime / K8s에서 관측 방법

### Linux

```bash
top -H
ps -L
perf top
```

**관측 대상:** thread state, context switch, stack usage, runtime execution state

### Runtime 도구

| 도구 | 역할 |
|------|------|
| thread dump | thread state |
| heap dump | object state |
| async-profiler | runtime execution state |
| flame graph | execution continuity |
| JFR | runtime event tracking |

### Kubernetes

| 현상 | 의미 |
|------|------|
| CrashLoopBackOff | unstable runtime state |
| OOMKill | heap state overflow |
| high restart count | runtime recovery failure |
| CPU throttling | execution state pressure |
| thread explosion | unmanaged runtime state |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*