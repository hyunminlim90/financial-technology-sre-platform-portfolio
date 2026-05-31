# 힙 영역 (Heap Area)

> 정독: 0회

## 1. 이 기술이 무엇인가

힙 영역은:

> 런타임 중 동적으로 생성되는 객체와 배열이 저장되는 **공유 메모리 영역**

핵심 역할: **dynamic object storage**

| 저장 대상 | 설명 |
|----------|------|
| 객체 인스턴스 | runtime object |
| 배열 | dynamic array |
| 객체 필드 값 | state data |
| 객체 메타데이터 연결 | type/runtime linkage |

**핵심 특징:** shared / dynamic / GC-managed

> 힙 영역은 **런타임 객체 생명 주기의 중심 메모리 공간**입니다.

<details>
<summary>Deep Dive</summary></br>

Run Time(실행 시간) [[M]](../../100-deep-dive/micro-foundations/run-time.md)  
Dynamic Memory Allocation(동적 메모리 할당) [[M]](../../100-deep-dive/micro-foundations/dynamic-memory-allocation.md)  
Instance(인스턴스) [[M]](../../100-deep-dive/micro-foundations/instance.md)  
Array Instance(배열 인스턴스) [[M]](../../100-deep-dive/micro-foundations/array-instance.md)  
Memory Loading(메모리 적재) [[M]](../../100-deep-dive/micro-foundations/memory-loading.md)  
Thread-Shared Data Area(스레드 공유 데이터 영역) [[M]](../../100-deep-dive/micro-foundations/thread-shared-data-area.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

힙 영역은 관리형 런타임 시스템 내부에서 등장합니다.

| 영역 | 역할 |
|------|------|
| Runtime Memory | 객체 저장 |
| Allocation System | new allocation |
| GC System | reclaim/compact |
| Thread Runtime | shared object access |
| Object Lifecycle | state persistence |

**실행 흐름:**

```
object allocation
→ heap placement
→ runtime usage
→ reference tracking
→ garbage collection
→ memory reclaim
```

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 영향이 큰 자원은 **Memory / CPU**입니다.

### Memory

힙 자체가 메모리 공간입니다:

- heap occupancy
- allocation capacity
- fragmentation
- retained objects

특히 **large heap / high object retention**은 시스템 메모리 압박을 증가시킵니다.

### CPU

힙 관리는 CPU를 지속 사용합니다:

- allocation
- reference scanning
- marking / compaction
- object movement

> GC 활동 증가 시 **CPU utilization spike**가 발생합니다.

---

## 4. 왜 중요한가

힙은 **런타임 객체 시스템의 핵심**입니다. 객체 기반 런타임 대부분은 **heap-centric runtime** 구조입니다.

| 이유 | 설명 |
|------|------|
| 객체 저장 | runtime state 유지 |
| 동적 메모리 | 실행 중 allocation 가능 |
| 공유 공간 | thread collaboration |
| GC 기반 | 자동 메모리 회수 |
| 장기 상태 유지 | method 종료 후에도 생존 가능 |

> **힙 안정성이 곧 런타임 안정성**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

힙은 실무 장애와 **매우 직접 연결**됩니다.

| 장애 | 원인 |
|------|------|
| OutOfMemory | heap exhaustion |
| Full GC Storm | excessive retained objects |
| Memory Leak | reference retention |
| GC Pause | large heap scan |
| Fragmentation | allocation failure |
| Latency Spike | compaction overhead |

특히 **heap growth without reclaim** 문제가 중요합니다:

```
object allocation rate  >  GC reclaim rate
↓
heap saturation
↓
allocation failure → OOM
```

또한 **large old generation**은 다음을 유발합니다:

- full GC 증가
- long pause
- response delay

---

## 6. 핵심 메커니즘

### (1) Dynamic Allocation

객체 생성 시 heap memory allocation이 수행됩니다:

```
calculate object size
→ reserve memory
→ attach metadata
→ initialize fields
```

### (2) Shared Memory

힙은 모든 스레드가 공유합니다. 따라서 **concurrent access** 문제가 발생할 수 있습니다:

- synchronization
- visibility
- memory consistency

### (3) Reachability

힙 객체는 참조 기반으로 생존 여부가 결정됩니다:

```
reachable object → survives
```

### (4) Garbage Collection

힙은 GC의 직접 관리 대상입니다:

```
mark → sweep → compact → copy → promotion
```

### (5) Generational Structure

현대 런타임은 힙을 세대별로 분리합니다:

| 영역 | 역할 |
|------|------|
| Young | 단기 객체 |
| Survivor | 생존 객체 |
| Old | 장수 객체 |

핵심 목적: **GC efficiency optimization**

### (6) Fragmentation

객체 삭제 후 메모리가 조각화될 수 있습니다:

```
free memory exists  but  allocation fails
```

따라서 compact 과정이 중요합니다.

### (7) Allocation Pressure

객체 생성 속도가 너무 빠르면 **allocation pressure**가 증가합니다:

- frequent GC
- CPU spike
- latency 증가

---

## 7. Linux / Runtime / K8s에서 관측 방법

### Runtime

| 항목 | 의미 |
|------|------|
| heap usage | 메모리 사용량 |
| young generation | 단기 객체 |
| old generation | 장기 객체 |
| allocation rate | 객체 생성 속도 |
| GC pause | 수집 정지 시간 |

**대표 도구:** `jstat`, `jmap`, `jcmd`, heap dump analyzer, runtime profiler

**대표 관측 신호:** heap expansion, promotion failure, retained object graph, full GC increase

### Linux (간접 관측)

```bash
top
vmstat
pidstat
sar
pmap
```

| 현상 | 의미 |
|------|------|
| RSS 증가 | heap growth |
| swap 사용 증가 | memory pressure |
| CPU spike | GC scanning |
| page fault 증가 | allocation pressure |

### Kubernetes

**대표 장애:** OOMKilled, restart loop, memory throttling, probe timeout

```
heap growth
→ GC pressure
→ memory limit exceed
→ container kill
```

> **container memory limit** 환경에서는 **heap sizing**이 매우 중요합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*