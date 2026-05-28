# 객체 접근 (Object Access)

> 정독: 0회

## 1. 이 기술이 무엇인가

객체 접근은:

> 실행 중인 프로그램이 객체의 데이터나 기능(Method)을 실제로 읽거나 수정하기 위해 **메모리 상의 객체 위치를 찾아 들어가는 과정**

**핵심: 참조(reference)를 실제 메모리 위치로 해석하여 데이터를 읽거나 쓰는 것**

**대표 동작:**

| 동작 | 의미 |
|------|------|
| Load | 객체 데이터 읽기 |
| Store | 객체 데이터 쓰기 |
| Dereference | 참조를 실제 주소로 해석 |
| Field Access | 객체 내부 필드 접근 |

```
reference → address → memory access
```

<details>
<summary>Deep Dive</summary></br>

Process(프로세스) [[M]](../../100-deep-dive/micro-foundations/process.md)  
Fields(필드) [[M]](../../100-deep-dive/micro-foundations/field.md)  
Method(메서드) [[M]](../../100-deep-dive/micro-foundations/method.md)  
Load(로드) [[M]](../../100-deep-dive/micro-foundations/load-operation.md)  
Store(스토어) [[M]](../../100-deep-dive/micro-foundations/store.md)  
Object Address(객체 주소) [[M]](../../100-deep-dive/micro-foundations/object-address.md)  
Reference Dereferencing(참조 역참조) [[M]](../../100-deep-dive/micro-foundations/reference-dereferencing.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

객체 접근은 거의 모든 런타임 연산에서 등장합니다.

| 영역 | 역할 |
|------|------|
| Heap Memory | 객체 저장 |
| CPU Pipeline | load/store 수행 |
| Cache Hierarchy | object caching |
| MMU | address translation |
| Runtime Engine | reference handling |

특히 동적 객체, 힙 기반 메모리 모델, 포인터 기반 구조, GC 기반 시스템에서 핵심입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향 |
|------|------|
| Memory | memory access 발생 |
| CPU Cache | cache hit/miss |
| TLB | address translation |
| CPU Pipeline | load/store stall |
| NUMA | remote memory latency |

> **객체 접근 성능은 CPU보다 메모리 지연(latency)에 더 크게 영향받는 경우가 많습니다.**

---

## 4. 왜 중요한가

현대 소프트웨어 대부분은 객체 그래프, 포인터 체인, 동적 메모리, 힙 기반 구조 위에서 동작합니다. 즉 실제 프로그램 실행 대부분은 **객체 접근의 연속**이라고 볼 수 있습니다.

객체 접근 효율이 나쁘면 cache miss 증가, latency 증가, CPU stall 증가, throughput 감소가 발생합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### Null Reference 접근

유효하지 않은 참조 접근 시 segmentation fault, access violation, null dereference가 발생할 수 있습니다.

### Cache Miss 폭증

객체가 메모리에 흩어져 있으면 pointer chasing이 증가하여 memory latency 증가, CPU idle 증가가 발생합니다.

### Dangling Pointer

이미 회수된 객체에 접근하면 corruption, crash, undefined behavior가 발생할 수 있습니다.

### TLB Miss 증가

대규모 힙에서 객체 접근이 분산되면 address translation overhead 증가, page walk 증가가 발생합니다.

### False Sharing

멀티스레드 환경에서 동일 cache line 경쟁으로 coherence traffic이 증가할 수 있습니다.

### NUMA Remote Access

다른 NUMA node 메모리 접근 시 remote memory latency가 증가할 수 있습니다.

---

## 6. 핵심 메커니즘

### 1) Reference 확보

프로그램은 직접 물리 주소를 다루지 않고 reference / pointer / handle 형태로 객체를 식별합니다.

### 2) Dereference 수행

실행 엔진/CPU가 `reference → actual address` 해석을 수행합니다. 이것이 역참조(dereference)입니다.

### 3) Address Translation

MMU가 virtual address / logical address를 physical address로 변환합니다. 여기서 page table lookup, TLB lookup이 수행됩니다.

### 4) Cache Access

CPU는 먼저 L1 → L2 → L3 cache를 확인합니다. cache hit이면 즉시 접근하고, cache miss이면 RAM에 접근합니다.

### 5) Memory Load / Store

| 동작 | 의미 |
|------|------|
| Load | 메모리 → 레지스터 |
| Store | 레지스터 → 메모리 |

### 6) Object Field Offset 계산

객체 내부 접근은 `base address + field offset` 형태로 수행됩니다. 즉 객체 접근은 결국 **address arithmetic** 기반입니다.

### 7) Memory Barrier / Synchronization

멀티스레드 환경에서는 visibility, ordering, cache coherence 보장이 필요합니다. 따라서 memory barrier, fence, atomic operation 등을 사용합니다.

---

## 7. Linux/Runtime/K8s에서 관측 방법

### Linux

```bash
# CPU Cache Miss 관측
perf stat
perf top

# Memory Access Profiling
perf record
perf report

# NUMA 접근 상태
numastat
numactl --hardware

# Virtual Memory Mapping
cat /proc/<PID>/maps

# Page Fault 관측 (si/so: swap, pgfault: page fault, majflt: major fault)
vmstat 1

# Cache/TLB 관측
perf stat -e cache-misses
perf stat -e dTLB-load-misses
```

### Kubernetes

```bash
# container memory 상태
kubectl top pod

# cgroup Memory Usage
cat /sys/fs/cgroup/memory.current
```

### Heap/Object Profiling

주요 관측 항목: object allocation, retained object, access hotspot, pointer graph, cache locality

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*