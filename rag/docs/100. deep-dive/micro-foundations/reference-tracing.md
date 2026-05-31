# 참조 추적 (Reference Tracing)

> 정독: 0회

## 1. 이 기술이 무엇인가

참조 추적은:

> 메모리 관리 시스템이 현재 실행 중인 프로그램에서 **어떤 객체가 아직 접근 가능한 상태인지 추적하는 과정**

**핵심 목적: 현재 살아있는 객체(Live Object)와 더 이상 접근 불가능한 객체(Garbage)를 구분하는 것**

주로 Garbage Collector, Runtime Memory Manager, Heap Manager 내부에서 사용됩니다.

**대표 방식:**

| 방식 | 설명 |
|------|------|
| Reachability Analysis | 루트 기준 도달 가능 여부 |
| Mark and Sweep | live object marking |
| Tracing GC | 객체 그래프 순회 |
| Reference Counting | 참조 수 기반 추적 |

---

## 2. 시스템 어디에서 등장하는가

참조 추적은 주로 동적 메모리 관리 환경에서 등장합니다.

| 영역 | 역할 |
|------|------|
| Heap Memory | object lifecycle tracking |
| Garbage Collector | live object detection |
| Runtime Engine | stack/register scanning |
| Virtual Machine | object graph traversal |
| Memory Profiler | leak detection |

특히 동적 객체 생성, 힙 기반 메모리 모델, 자동 메모리 회수 시스템에서 핵심 역할을 합니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **메모리와 CPU**입니다.

| 자원 | 영향 |
|------|------|
| Memory | object metadata |
| CPU | graph traversal |
| Cache | pointer chasing |
| Scheduler | GC pause |
| NUMA | memory locality 영향 |

특히 대규모 힙에서는 **참조 그래프 순회 비용**이 상당히 커질 수 있습니다.

---

## 4. 왜 중요한가

참조 추적이 없다면 사용 종료 객체 식별 불가, 메모리 회수 불가, heap growth 지속, memory leak 증가 문제가 발생합니다.

특히 장기 실행 서버, 대규모 객체 그래프, 멀티스레드 시스템, 캐시 시스템에서는 필수입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### Memory Leak

객체가 실제로는 필요 없는데 어딘가에서 계속 참조 중이면 회수가 불가능합니다. 결과로 heap 증가, RSS 증가, OOM이 발생합니다.

### GC Pause 증가

heap 규모 증가 시 root scan 증가, graph traversal 증가, marking 증가가 발생하여 latency spike, throughput 감소로 이어질 수 있습니다.

### Object Retention

작은 참조 하나 때문에 거대한 객체 그래프 전체가 생존할 수 있습니다. 대표 예시로 cache retention, listener leak, static reference leak이 있습니다.

### Pointer Corruption

잘못된 참조 상태 발생 시 invalid traversal, corruption, dangling pointer가 발생할 수 있습니다.

### Cyclic Reference 문제

Reference Counting 기반 시스템에서 서로 참조만 하고 외부 접근이 없는 상태가 발생할 수 있습니다.

---

## 6. 핵심 메커니즘

### 1) Root Set 확보

GC/runtime이 stack, CPU register, global object, static region, thread local storage 등에서 시작 참조를 확보합니다. 이것을 **Root Set**이라고 합니다.

### 2) Object Graph Traversal

Root에서 시작하여 object field, pointer, reference slot 등을 따라 객체 그래프를 순회합니다.

| 알고리즘 | 특징 |
|---------|------|
| DFS | stack 기반 |
| BFS | queue 기반 |
| Concurrent traversal | runtime 병렬 수행 |

### 3) Reachability 판정

순회 중 도달 가능한 객체는 **Live Object**, 도달 불가능한 객체는 **Garbage**로 판정합니다.

### 4) Marking 수행

live object에 mark bit, live flag, color marking 등을 기록합니다.

### 5) Sweep/Reclaim 연계

mark되지 않은 객체를 reclaim하여 free list 반환, page reclaim을 수행합니다.

```
Reference Tracing → Reachability → Mark → Reclaim
```

### 6) Write Barrier / Memory Barrier

멀티스레드 환경에서는 객체 참조 변경, concurrent mutation이 발생할 수 있습니다. 따라서 write barrier, read barrier, memory fence를 사용하여 추적 상태를 동기화합니다.

---

## 7. Linux/Runtime/K8s에서 관측 방법

### Linux

```bash
# Heap 사용량
top
htop
ps aux

# Virtual Memory 상태
cat /proc/<PID>/smaps

# Memory Map 분석
pmap <PID>

# OOM 발생 기록
dmesg | grep -i oom
```

### Runtime Heap Dump

주요 관측 항목: live object count, retained size, object graph, root reference chain

### GC Trace 분석

| 지표 | 의미 |
|------|------|
| mark time | tracing 시간 |
| sweep time | reclaim 시간 |
| live set size | 생존 객체 |
| allocation rate | allocation 속도 |

### Kubernetes

```bash
# container memory pressure
kubectl top pod
kubectl describe pod

# cgroup 메모리 상태
cat /sys/fs/cgroup/memory.current
cat /sys/fs/cgroup/memory.events
```

### eBPF 기반 메모리 추적

allocation hotspot, leak path, page reclaim, allocator latency 등을 관측할 수 있습니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*