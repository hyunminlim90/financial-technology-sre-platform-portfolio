# JVM 실행 엔진 (JVM Execution Engine)

> 정독: 0회

## 1. 이 기술이 무엇인가

JVM 실행 엔진은:

> 메모리에 적재된 바이트코드를 실제 CPU 연산으로 변환하고 실행하는 **런타임 실행 계층**

| 역할 | 설명 |
|------|------|
| bytecode execution | 바이트코드 실행 |
| instruction dispatch | 명령 분배 |
| native translation | 기계어 변환 |
| runtime optimization | 실행 중 최적화 |
| execution control | 스레드/메모리 실행 제어 |

> 정적인 `.class` 파일을 실제 물리적 CPU 실행 흐름으로 전환하는 **런타임 핵심 엔진**입니다.

---

## 2. 시스템 어디에서 등장하는가

**실행 흐름:**

```
Source Code
→ Bytecode
→ Class Loader
→ Runtime Data Area
→ Execution Engine
→ Native CPU Execution
```

**등장 위치:**

| 계층 | 역할 |
|------|------|
| JVM | execution core |
| Managed Runtime | runtime orchestration |
| VM Architecture | execution abstraction |
| Runtime System | dynamic execution |
| Operating System | process scheduling interaction |

> JVM 내부에서 실제 "프로그램 실행"을 담당하는 **중심 계층**입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

### CPU (가장 직접적)

실행 엔진은 다음을 지속 수행합니다:

- instruction decode
- JIT compilation
- native execution
- branch optimization

### Memory (매우 중요)

사용 영역:

- heap
- stack
- code cache
- metadata
- GC structures

특히 **JIT compiled code cache**와 **object allocation** 영향이 큽니다.

### Disk (초기 영향)

- class loading
- JAR reading
- native library loading

### Network (간접 영향)

- distributed systems
- RPC runtime
- remote class loading

---

## 4. 왜 중요한가

**프로그램의 실제 성능과 안정성을 결정**하기 때문입니다.

| 항목 | 영향 |
|------|------|
| latency | 응답 속도 |
| throughput | 처리량 |
| CPU efficiency | 연산 효율 |
| memory behavior | 메모리 사용 |
| startup time | 초기 실행 속도 |
| GC interaction | 메모리 안정성 |

> 동일한 바이트코드라도 실행 엔진 품질에 따라 **성능 차이가 크게 발생**합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

**대표 사례:**

| 장애 | 원인 |
|------|------|
| CPU Spike | excessive interpretation |
| warmup latency | delayed JIT optimization |
| memory pressure | excessive allocation |
| stop-the-world pause | GC interaction |
| code cache exhaustion | JIT compiled code overflow |
| thread contention | runtime scheduling bottleneck |
| degraded throughput | deoptimization/recompilation |

특히 다음 환경에서 중요합니다:

- API server
- fintech runtime
- low latency service
- high throughput backend

---

## 6. 핵심 메커니즘

### (1) 실행 엔진은 바이트코드를 직접 실행하지 않는다

실제 CPU(x86, ARM)는 기계어만 이해합니다. 따라서 반드시 변환이 필요합니다:

```
Bytecode → Execution Engine → Native Machine Code → CPU
```

### (2) Interpreter + JIT Hybrid 구조

현대 JVM의 핵심입니다.

```
초기: Interpreter          (빠른 시작)
이후: Hot Method Detection
    → JIT Compilation      (고성능 실행)
```

> startup latency와 runtime performance를 **동시에 해결**합니다.

### (3) HotSpot Detection

반복 실행 코드를 탐지합니다:

- loop
- frequently invoked method
- heavy arithmetic path

탐지 후 native optimization이 수행됩니다.

### (4) Dynamic Optimization

실행 중 실제 데이터 기반 최적화를 수행합니다.

| 최적화 | 설명 |
|--------|------|
| method inlining | 함수 호출 제거 |
| escape analysis | allocation 제거 |
| branch prediction optimization | 분기 최적화 |
| lock optimization | synchronization 개선 |
| dead code elimination | 불필요 코드 제거 |

> 실행 중 **실시간 최적화**가 발생합니다.

### (5) Runtime Deoptimization

가정이 깨지면 다음이 발생합니다:

```
optimized code → invalidated → interpreter fallback
```

> JVM은 항상 **재최적화 가능한 동적 시스템**입니다.

### (6) Code Cache

JIT 결과 저장 공간으로, 다음을 저장합니다:

- native compiled method
- optimized execution path

cache 부족 시 recompilation 증가 및 성능 저하가 발생할 수 있습니다.

### (7) Garbage Collector와 강하게 연결

실행 엔진은 allocation을 계속 발생시키고, GC는 다음을 수행합니다:

- object lifecycle
- heap reclamation
- memory compaction

> **Execution Engine ↔ GC는 강결합 관계**입니다.

### (8) Thread Scheduling Interaction

JVM thread는 OS thread와 연결됩니다:

```
JVM Runtime → OS Scheduler → CPU Core
```

---

## 7. Linux / Runtime / K8s에서 관측 방법

### Linux

```bash
top
perf top
pidstat -u -t
```

**관측 항목:** CPU hotspot, thread scheduling, native execution overhead

### Runtime 도구

| 도구 | 역할 |
|------|------|
| JFR | runtime profiling |
| jstack | thread state |
| jcmd | execution metrics |
| async-profiler | hotspot analysis |
| flame graph | execution path visualization |

### Kubernetes

| 현상 | 의미 |
|------|------|
| startup delay | interpreter phase |
| CPU throttling | JIT/GC pressure |
| OOMKill | heap/runtime pressure |
| pod warmup | tiered compilation |
| latency spike | deoptimization/GC |

> 짧게 생성되는 **pod workload**에서 warmup 영향이 특히 큽니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*