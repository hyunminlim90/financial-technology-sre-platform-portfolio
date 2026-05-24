# 가비지 컬렉션 (Garbage Collection, GC)

> 정독: 0회

## 1. 이 기술이 무엇인가

가비지 컬렉션(GC)은:

> 런타임이 더 이상 사용되지 않는 메모리 객체를 **자동으로 탐지하고 회수하는 메모리 관리 시스템**

핵심 목적: **unused memory reclamation**

| 기능 | 설명 |
|------|------|
| 객체 추적 | 살아있는 객체 판별 |
| 메모리 회수 | unreachable 객체 제거 |
| 공간 재정리 | fragmentation 감소 |
| 메모리 재사용 | allocation 가능 상태 유지 |

> GC는 **런타임 메모리 안정성을 유지하는 핵심 자원 관리 메커니즘**입니다.

---

## 2. 시스템 어디에서 등장하는가

GC는 **관리형 런타임(Managed Runtime)** 환경에서 등장합니다.

| 영역 | 역할 |
|------|------|
| Heap Memory | 객체 저장 공간 |
| Runtime Engine | 객체 상태 추적 |
| Allocation System | 메모리 할당 |
| Reference Graph | 객체 연결 구조 |
| Thread System | GC thread 실행 |
| Memory Manager | reclamation 수행 |

**실행 흐름:**

```
object allocation
→ object usage
→ reference loss
→ unreachable detection
→ garbage collection
→ memory reuse
```

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 영향이 큰 자원은 **Memory / CPU**입니다.

### Memory

GC는 heap memory 전체를 관리합니다:

- heap occupancy
- fragmentation
- object retention
- allocation space

> GC 목적 자체가 **memory exhaustion prevention**입니다.

### CPU

GC는 메모리를 추적하기 위해 CPU를 지속 사용합니다:

- reference scanning
- marking / copying
- compaction
- metadata update

특히 **large object graph** 환경에서는 CPU 사용량이 급격히 증가할 수 있습니다.

---

## 4. 왜 중요한가

GC는 **런타임 안정성의 핵심**입니다.

GC가 없으면 **allocation only without reclamation** 상태가 됩니다. 결과: heap exhaustion, process crash, OOM, allocation failure

| 이유 | 설명 |
|------|------|
| 자동 메모리 관리 | 수동 해제 제거 |
| 메모리 안정성 | leak 위험 감소 |
| 장기 실행 유지 | long-running process 지원 |
| 개발 생산성 | manual free 제거 |
| runtime survivability | 메모리 고갈 방지 |

---

## 5. 실제 장애와 어떤 관련이 있는가

실무 장애와 **매우 강하게 연결**됩니다.

| 장애 | 원인 |
|------|------|
| OOM | heap exhaustion |
| GC Pause | stop-the-world |
| CPU Spike | excessive GC scanning |
| Latency Spike | compaction overhead |
| Memory Leak | retained reference |
| Allocation Failure | fragmented heap |

특히 **GC overhead saturation** 문제가 중요합니다:

```
high allocation rate
+ retained objects
+ insufficient reclaim
↓
continuous GC cycle
↓
CPU exhaustion → latency increase → throughput collapse
```

---

## 6. 핵심 메커니즘

### (1) Reachability Analysis

GC 핵심 기준: **reachable?**

```
Root Set (stack / global / runtime references)
↓
참조 그래프 탐색
↓
도달 불가능 객체 판별
```

### (2) Mark Phase

GC는 살아있는 객체를 표시합니다: **mark reachable objects**

### (3) Sweep Phase

도달 불가능 객체를 제거합니다: **reclaim unreachable memory**

### (4) Compact Phase

메모리를 압축합니다: **move surviving objects**

목적: fragmentation 감소, contiguous free space 확보

### (5) Generational Management

현대 GC 핵심 개념: **most objects die young**

| 영역 | 특징 |
|------|------|
| Young Generation | 짧은 수명 객체 |
| Old Generation | 장수 객체 |

두 영역을 분리 관리합니다.

### (6) Stop-The-World

GC 수행 중 일부 런타임은 애플리케이션 실행을 멈춥니다:

- latency spike
- request stall

### (7) Concurrent Collection

현대 GC는 pause 감소를 위해 다음을 사용합니다:

- concurrent marking
- background collection
- parallel scanning

### (8) Allocation Pressure

GC 빈도는 allocation 속도에 직접 영향받습니다:

```
high object churn → frequent GC → CPU overhead increase
```

---

## 7. Linux / Runtime / K8s에서 관측 방법

### Runtime

| 항목 | 의미 |
|------|------|
| GC frequency | 수집 빈도 |
| pause time | 정지 시간 |
| heap usage | 메모리 점유 |
| promotion rate | old generation 이동 |
| allocation rate | 객체 생성 속도 |

**대표 도구:** `jstat`, `jmap`, `jcmd`, gc logs, runtime profiler

**대표 관측 신호:** full GC increase, promotion failure, heap expansion, allocation stall

### Linux (간접 관측)

```bash
top
vmstat
pidstat
sar
perf
```

| 현상 | 의미 |
|------|------|
| high CPU | GC scanning |
| RSS growth | heap expansion |
| context switch 증가 | GC thread activity |
| page fault 증가 | memory pressure |

### Kubernetes

**대표 장애:** OOMKilled, container restart, latency spike, probe timeout

```
heap growth
→ GC pressure
→ pause increase
→ health check failure
→ pod restart
```

> **container memory limit** 환경에서는 GC 튜닝이 매우 중요합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*