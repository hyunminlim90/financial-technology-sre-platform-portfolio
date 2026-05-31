# 객체 생명 주기 관리 (Object Lifecycle Management)

> 정독: 0회

## 1. 이 기술이 무엇인가

객체 생명 주기 관리는:

> 런타임이 객체의 생성부터 메모리 회수까지 **전체 상태를 추적·제어하는 메모리 관리 체계**

```
creation
→ initialization
→ reference tracking
→ synchronization
→ unreachable detection
→ reclamation
```

| 대상 | 설명 |
|------|------|
| 객체 생성 | 메모리 할당 |
| 객체 상태 | 필드 값 유지 |
| 참조 관계 | 객체 그래프 추적 |
| 동기화 상태 | lock/monitor |
| 메모리 회수 | garbage collection |

> 객체 생명 주기 관리는 **런타임 메모리 시스템의 핵심 운영 기능**입니다.

<details>
<summary>Deep Dive</summary></br>

Garbage Collection(가비지 컬렉션) [[M]](../../100-deep-dive/micro-foundations/garbage-collection.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

객체 생명 주기 관리는 거의 모든 런타임 시스템에서 등장합니다.

| 영역 | 역할 |
|------|------|
| Heap Memory | 객체 저장 |
| Allocation System | 객체 생성 |
| GC System | 객체 회수 |
| Thread Synchronization | lock 상태 관리 |
| Reference Tracking | reachability 분석 |
| Runtime Metadata | 객체 상태 기록 |

**실행 흐름:**

```
Allocation
→ Reference Linking
→ Runtime Usage
→ State Mutation
→ Unreachable Detection
→ Reclamation
```

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 영향이 큰 자원은 **Memory / CPU**입니다.

### Memory

객체는 heap memory를 지속적으로 점유합니다. 특히 다음은 heap pressure를 크게 증가시킵니다:

- high allocation rate
- large object graph
- retained references

대표 문제: heap expansion, fragmentation, GC overhead, allocation contention

### CPU

GC와 lifecycle tracking은 CPU를 지속 사용합니다:

- reference scanning
- marking / compaction
- synchronization
- metadata update

> 객체 생명 주기 관리는 **CPU와 메모리를 동시에 소비하는 핵심 런타임 기능**입니다.

---

## 4. 왜 중요한가

객체 기반 런타임에서 대부분의 문제는 결국 **object lifecycle mismanagement**로 연결됩니다.

| 이유 | 설명 |
|------|------|
| 메모리 안정성 | OOM 방지 |
| 성능 유지 | GC overhead 감소 |
| 동시성 안정성 | synchronization consistency |
| 자원 회수 | unused memory reclaim |
| 런타임 지속성 | long-running process 유지 |

> 객체 생명 주기 관리는 **runtime stability의 핵심**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실무에서 **매우 중요한 장애 영역**입니다.

| 장애 | 원인 |
|------|------|
| Memory Leak | reference retained |
| OOM | unreachable reclaim 실패 |
| GC Pause Spike | excessive live objects |
| CPU Spike | GC scanning overload |
| Fragmentation | allocation/reclaim imbalance |
| Lock Contention | synchronization pressure |

특히 **unreachable but still referenced** 상태가 중요합니다:

```
cache → map → object reference retained
```

결과: 객체 회수 불가 → heap growth → full GC 증가 → OOM 발생

또한 **high object churn**은 다음을 유발합니다:

- allocation storm
- excessive GC frequency
- latency spike

---

## 6. 핵심 메커니즘

### (1) Allocation

런타임은 heap에 객체 공간을 확보합니다:

```
allocate memory → attach metadata → initialize fields
```

### (2) Reachability Tracking

런타임은 객체 참조 그래프를 추적합니다:

```
Root → reachable objects
```

GC 핵심 기준: **reachable?**

### (3) Object States

| 상태 | 의미 |
|------|------|
| allocated | 생성됨 |
| referenced | 사용 중 |
| synchronized | lock 상태 |
| unreachable | 접근 불가 |
| reclaimed | 메모리 회수 |

### (4) Metadata Management

런타임은 객체 메타데이터를 지속 관리합니다:

- age
- lock state
- hash
- GC mark state

### (5) Garbage Collection

GC 핵심 흐름:

```
mark → sweep → compact → reclaim
```

### (6) Generational Lifecycle

많은 런타임은 **short-lived / long-lived objects**를 분리 관리합니다.

목적: GC 효율 향상, scanning 감소, pause time 감소

### (7) Synchronization Integration

객체 lifecycle은 synchronization과 연결됩니다:

- monitor state
- lock ownership
- thread contention

> 객체는 단순 데이터가 아니라 **런타임 관리 단위**입니다.

---

## 7. Linux / Runtime / K8s에서 관측 방법

### Runtime

| 항목 | 의미 |
|------|------|
| allocation rate | 객체 생성 속도 |
| live objects | 활성 객체 수 |
| retained heap | 유지 객체 |
| GC frequency | 회수 빈도 |
| pause time | GC stop duration |

**대표 도구:** `jstat`, `jmap`, `jcmd`, heap dump analyzer, runtime profiler

**대표 관측 신호:** heap growth, full GC increase, retained object graph, promotion failure

### Linux (간접 관측)

```bash
top
vmstat
pidstat
perf
sar
```

| 현상 | 의미 |
|------|------|
| RSS 증가 | live object 증가 |
| CPU spike | GC scanning |
| page fault 증가 | memory pressure |
| context switch 증가 | synchronization overhead |

### Kubernetes

**대표 현상:** OOMKilled, memory throttling, restart loops, latency spike

특히 **container memory limit** 환경에서는 lifecycle 문제가 치명적입니다:

```
object retention
→ heap growth
→ GC pressure
→ memory limit exceed
→ container kill
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*