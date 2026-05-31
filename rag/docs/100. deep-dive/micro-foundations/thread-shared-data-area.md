# 스레드 공유 데이터 영역 (Thread-Shared Data Area)

> 정독: 0회

## 1. 이 기술이 무엇인가

스레드 공유 데이터 영역(Thread-Shared Data Area)은:

> 프로세스 내부에서 여러 실행 스레드가 공동으로 접근하는 메모리 영역

**핵심 특징: shared visibility + shared residency + shared mutation**

### 구성 요소

| 구성 요소 | 역할 |
|---|---|
| Heap | 동적 객체 저장 |
| Method/Metadata Area | 클래스/타입 정보 |
| Runtime Constant Pool | 상수 및 심볼 |
| Shared Cache | 공용 데이터 |
| Shared Buffers | I/O 및 IPC 버퍼 |

중요한 점은 **multiple execution contexts**가 동일 데이터를 동시에 접근한다는 것입니다.

즉, 스레드 공유 데이터 영역은 **concurrent runtime state space**입니다.

---

## 2. 시스템 어디에서 등장하는가

스레드 공유 데이터 영역은 거의 모든 멀티스레드 시스템에서 등장합니다.

### 대표 위치

| 계층 | 예시 |
|---|---|
| Runtime | heap/shared metadata |
| OS Process | shared address space |
| Web Server | connection pool |
| Database Engine | buffer cache |
| Network Stack | socket buffer |
| Kubernetes App | in-process shared state |

### 대표 흐름

```
Thread A → shared object ← Thread B
```

독립 실행 흐름은 분리되어 있어도 **runtime data**는 공동 사용됩니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Memory**입니다.

| 요소 | 영향 |
|---|---|
| cache coherence | CPU cache synchronization |
| lock contention | CPU waiting |
| heap residency | memory pressure |
| false sharing | cache inefficiency |
| synchronization metadata | runtime overhead |

멀티코어 시스템에서는 **cache synchronization traffic**이 매우 중요합니다. 공유 데이터는 다음을 유발할 수 있습니다.

- CPU cache invalidation
- memory barrier
- cache line bouncing

---

## 4. 왜 중요한가

현대 시스템 대부분은 **shared-state concurrency** 위에서 동작합니다.

### 중요 이유

| 목적 | 설명 |
|---|---|
| memory efficiency | 중복 데이터 제거 |
| inter-thread communication | 스레드 간 협업 |
| shared cache | 성능 최적화 |
| centralized runtime state | 공용 상태 유지 |
| high concurrency | 병렬 처리 |

다음 요소 모두 공유 데이터 영역에 존재할 수 있습니다.

- connection pool
- cache map
- session state
- message queue
- object graph

---

## 5. 실제 장애와 어떤 관련이 있는가

공유 데이터 영역은 동시성 장애의 핵심 발생 지점입니다.

### 대표 장애

| 장애 | 설명 |
|---|---|
| Race Condition | 동시 수정 충돌 |
| Deadlock | 락 교착 |
| Memory Corruption | 비정상 상태 |
| Visibility Issue | 변경 사항 미전파 |
| Heap Exhaustion | 공유 메모리 과다 사용 |
| GC Pressure | 객체 폭증 |
| Contention | 락 대기 증가 |

특히 중요한 것은 **shared mutable state**입니다. 여러 스레드가 동시에 read + write를 수행하면 다음 문제가 발생할 수 있습니다.

- consistency 붕괴
- stale read
- lost update

또한 공유 영역이 커질수록 **GC traversal cost**도 증가합니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Shared Address Space

프로세스 내부 스레드는 **same virtual address space**를 공유합니다. 동일 객체 주소를 여러 스레드가 참조할 수 있습니다.

### Private vs Shared

| 영역 | 특성 |
|---|---|
| Stack | thread-private |
| Register | thread-private |
| Heap | thread-shared |
| Metadata | usually shared |

실행 흐름은 독립적이지만 **runtime objects**는 공유됩니다.

### Reference-Based Sharing

스레드는 보통 **object reference**를 통해 공유 데이터에 접근합니다. 실제 데이터 복사가 아니라 **same memory residency**를 공동 참조합니다.

### Synchronization

공유 데이터 접근 시 **mutual exclusion**이 필요할 수 있습니다.

대표 메커니즘: `mutex`, `monitor`, `semaphore`, `rwlock`, `atomic operation`, `CAS`

### Memory Visibility

멀티코어 시스템에서는 **CPU cache inconsistency** 문제가 존재합니다. 한 CPU 코어의 변경 사항이 다른 코어에 즉시 보이지 않을 수 있습니다.

이를 위해 다음이 사용됩니다.

- memory barrier
- cache coherence protocol
- acquire/release ordering

### False Sharing

**false sharing**은 매우 중요한 개념입니다. 다른 데이터라도 같은 cache line에 존재하면 **cache invalidation storm**이 발생할 수 있습니다. 공유 데이터 배치 자체가 성능에 영향을 줍니다.

### Shared Heap Residency

공유 객체는 보통 **heap-resident**입니다. 따라서 다음 영향을 받습니다.

- allocation pressure
- GC traversal
- object retention

### Runtime Metadata Coordination

런타임은 공유 객체에 대해 다음을 추적합니다.

- lock state
- ownership
- reference graph
- GC mark state

즉 공유 데이터 영역은 **runtime-governed memory space**입니다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**대표 관측 명령어:** `top`, `htop`, `pmap`, `vmstat`, `perf`

**동시성 관련 관측:** `perf stat`, `perf top`, `pidstat -w`

| 지표 | 의미 |
|---|---|
| context switch | 스레드 전환 |
| mutex wait | 락 대기 |
| cache miss | 캐시 충돌 |
| RSS | 공유 메모리 상주 |
| page fault | 메모리 접근 |

### Runtime

| 항목 | 의미 |
|---|---|
| heap usage | 공유 객체 메모리 |
| lock contention | 경쟁 상태 |
| allocation rate | 객체 생성량 |
| GC pause | 공유 객체 정리 |
| thread dump | 스레드 상태 |

**shared object retention**은 메모리 누수 원인이 될 수 있습니다.

### Kubernetes

| 증상 | 원인 |
|---|---|
| CPU spike | contention |
| memory growth | retained shared objects |
| latency increase | lock waiting |
| OOMKilled | heap exhaustion |

**대표 관측:** `kubectl top pod`, `kubectl describe pod`

**Observability:** Prometheus, Grafana, eBPF, runtime profiler, flamegraph

특히 **thread contention metrics**가 중요합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*