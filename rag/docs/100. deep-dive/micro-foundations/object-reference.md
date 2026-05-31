# 객체 참조 (Object Reference)

> 정독: 0회

## 1. 이 기술이 무엇인가

객체 참조(Object Reference)는:

> 런타임에 생성된 객체의 위치를 가리키는 주소 기반 식별 정보이며 실행 엔진과 메모리 관리 시스템이 객체를 추적·접근하기 위한 핵심 메커니즘

**핵심:** object references connect executable code to dynamically allocated memory objects

객체 참조는 일반적으로 다음 형태로 존재합니다:

| 형태 | 설명 |
|------|------|
| raw pointer | 실제 메모리 주소 |
| compressed pointer | 압축 주소 표현 |
| handle | 간접 참조 구조 |
| tagged pointer | 메타비트 포함 주소 |

<details>
<summary>Deep Dive</summary></br>

Runtime Environment(런타임 환경) [[M]](../../100-deep-dive/micro-foundations/runtime-environment.md)  
Memory Base Address(메모리 시작 주소) [[M]](../../100-deep-dive/micro-foundations/memory-base-address.md)  
Logical Address(논리 주소) [[M]](../../100-deep-dive/micro-foundations/logical-address.md)  
Execution Engine(실행 엔진) [[M]](../../100-deep-dive/micro-foundations/execution-engine.md)  
Memory Manager(메모리 관리자) [[M]](../../100-deep-dive/micro-foundations/memory-manager.md)  
Reference Tracing(참조 추적) [[M]](../../100-deep-dive/micro-foundations/reference-tracing.md)  
Object Access(객체 접근) [[M]](../../100-deep-dive/micro-foundations/object-access.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

객체 참조는 모든 동적 메모리 시스템에서 등장합니다.

| 영역 | 사용 위치 |
|------|----------|
| VM runtime | heap object access |
| GC system | object graph traversal |
| object-oriented runtime | instance linkage |
| memory allocator | allocation tracking |
| interpreter/JIT | object dereference |
| container runtime | metadata ownership |

**대표 환경:**

| 시스템 | 객체 참조 역할 |
|--------|--------------|
| managed runtime | heap access |
| GC engine | reachability analysis |
| scripting VM | dynamic object lookup |
| JIT compiler | optimized memory access |
| serializer | object graph traversal |

> **즉:** object references are the fundamental linkage mechanism of dynamic memory systems

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**가장 영향 큰 자원: Memory + CPU Cache**

이유: 객체 참조는 다음과 직접 연결되기 때문입니다.

- heap traversal
- pointer dereference
- cache lookup
- graph walking

| 자원 | 영향 |
|------|------|
| Memory | heap layout |
| CPU | pointer chasing |
| Cache | locality efficiency |
| Disk | serialization persistence |
| Network | distributed object encoding |

> **특히:** poor object reference locality causes severe cache miss amplification

---

## 4. 왜 중요한가

객체 참조는 **runtime object accessibility foundation**입니다.

참조가 없으면:

- 객체 접근 불가
- method dispatch 불가
- graph traversal 불가
- GC reachability 분석 불가

**중요한 이유:**

| 이유 | 설명 |
|------|------|
| dynamic memory access | 런타임 객체 접근 |
| object graph construction | 관계 형성 |
| garbage collection | 생존 객체 추적 |
| polymorphic dispatch | 동적 호출 |
| runtime navigation | 메모리 탐색 |

> **핵심:** runtime systems operate primarily through object references rather than raw object copies

---

## 5. 실제 장애와 어떤 관련이 있는가

객체 참조 문제는 매우 치명적입니다.

**대표 장애:**

| 장애 | 원인 |
|------|------|
| null dereference | invalid reference |
| dangling pointer | freed object access |
| memory leak | retained references |
| heap fragmentation | scattered allocations |
| cache thrashing | poor locality |
| GC pause amplification | excessive reference graph |

**실무에서 매우 중요:**

| 문제 | 결과 |
|------|------|
| accidental retention | heap explosion |
| cyclic references | unreclaimed memory |
| deep graph traversal | latency spike |
| pointer chasing | CPU stall |
| false sharing | cache contention |

GC 시스템에서는:

> **reference graph complexity directly affects garbage collection cost**

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Heap Allocation

객체 참조는 **generated when runtime allocates heap objects**입니다.

```
allocation request
→ heap region selection
→ object placement
→ reference generation
```

즉 참조는 객체 자체가 아니라, **객체 위치를 식별하는 정보**입니다.

---

### Reference vs Object

> **reference != object**

| 개념 | 의미 |
|------|------|
| object | 실제 heap data |
| reference | object location identifier |

변수 내부에는: **typically the reference is stored, not the full object**

---

### Dereferencing

실행 엔진 핵심 동작: **dereference converts a reference into actual memory access**

```
reference
→ memory address resolution
→ object header access
→ field access
```

CPU 레벨에서는 다음이 발생합니다:
- pointer load
- cache lookup
- memory fetch

---

### Object Header Access

객체 참조는 일반적으로 **object memory layout의 시작점**을 가리킵니다.

> reference usually targets object base address

이후 runtime은 다음에 접근합니다:
- type metadata
- synchronization metadata
- field offsets

---

### Pointer Chasing

객체 참조를 따라가는 과정을 **pointer chasing**이라고 합니다.

```
object A
→ reference to B
→ reference to C
→ reference to D
```

이 과정은 다음을 크게 증가시킬 수 있습니다:
- cache miss
- TLB miss
- memory latency

---

### Reference Graph

런타임 객체들은 **form directed object graphs through references**합니다.

GC는 이 그래프를 탐색하며, 대표 개념은 다음과 같습니다:

| 개념 | 의미 |
|------|------|
| root references | 시작점 |
| strong reference | 생존 유지 |
| weak reference | optional reachability |
| cyclic graph | 순환 구조 |

---

### Memory Locality

**좋은 경우:** related objects are spatially close in memory

**나쁜 경우:** references jump across fragmented heap regions

나쁜 경우 다음이 발생합니다:
- cache miss 증가
- CPU stall 증가
- latency 증가

---

### Compressed References

64bit 시스템에서 중요합니다. 목적: **reduce pointer memory overhead**

방법:
- alignment 활용
- lower bits 제거
- compressed offset 저장

| 효과 | 설명 |
|------|------|
| reduced heap footprint | 메모리 절약 |
| improved cache density | cache 효율 |
| lower bandwidth | memory traffic 감소 |

---

### Reference Safety

> **managed runtimes enforce reference integrity rules**

| 보호 | 목적 |
|------|------|
| null checks | invalid access 방지 |
| bounds checks | unsafe traversal 방지 |
| GC relocation updates | moved object 추적 |
| write barriers | GC consistency |

---

### Runtime Reachability

GC 핵심 메커니즘: **objects survive while reachable through references**

> **reference topology determines object lifetime**

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

직접적인 객체 참조는 user-space runtime 내부에 존재합니다.

| 도구 | 목적 |
|------|------|
| `perf` | pointer chasing 분석 |
| `valgrind` | invalid reference 탐지 |
| `gdb` | memory inspection |
| `pmap` | process memory layout |
| heap profilers | object graph 분석 |

> 대표 문제: cache misses caused by fragmented object references

---

### Runtime / VM

런타임에서 매우 중요합니다.

| 관측 항목 | 의미 |
|----------|------|
| heap dump | object graph |
| reference chains | retention path |
| GC roots | reachability |
| object relocation | compacting GC |
| reference density | memory efficiency |

> 실무에서 자주 분석: **which references are retaining large heap regions**

---

### Kubernetes

K8s 자체가 객체 참조를 직접 다루진 않지만, 컨테이너 내부 runtime은 heavily 사용합니다.

| 영역 | 영향 |
|------|------|
| high heap pressure | pod OOM |
| reference leaks | memory growth |
| GC overhead | CPU spike |
| fragmented heaps | latency increase |

> **SRE 관점 핵심:** bad object reference topology often manifests as GC latency or memory instability

---

### Observability

| 도구 | 역할 |
|------|------|
| heap dump analyzers | reference graph |
| flamegraphs | pointer traversal cost |
| GC logs | retention behavior |
| allocation profilers | object lifetime |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*