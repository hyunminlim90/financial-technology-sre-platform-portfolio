# 인스턴스 변수 (Instance Variables)

> 정독: 0회

## 1. 이 기술이 무엇인가

인스턴스 변수는:

> 실행 중 생성된 개별 인스턴스 내부에 저장되는 **상태 데이터 영역**입니다.

| 특징 | 설명 |
|------|------|
| instance-owned | 인스턴스마다 독립 보유 |
| runtime state | 실행 중 상태 저장 |
| memory-resident | 실제 메모리 점유 |
| lifecycle-bound | 객체 생명주기와 함께 존재 |

**일반적 구조:**

```
Object
    ├─ Metadata/Header
    └─ Instance Fields
```

인스턴스 변수는 다음과 같은 실제 런타임 상태를 저장합니다:

- 숫자
- 문자열 참조
- 객체 참조
- 상태 플래그
- 카운터
- 버퍼 참조

<details>
<summary>Deep Dive</summary></br>

Field Values(필드 값) [[M]](../../100-deep-dive/micro-foundations/field-values.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

인스턴스 변수는 객체 기반 런타임 전체에서 등장합니다.

| 영역 | 역할 |
|------|------|
| Heap Memory | 객체 상태 저장 |
| Runtime Object Model | 객체 구조 정의 |
| GC System | 생명주기 추적 |
| Thread Execution | 상태 읽기/쓰기 |
| Serialization | 객체 데이터 추출 |
| Cache System | 상태 유지 |

**실행 흐름:**

```
Type Definition
→ Instance Allocation
→ Field Initialization
→ Runtime Mutation
```

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 큰 영향은 **Memory / CPU Cache**입니다.

### Memory

인스턴스 변수는 객체 크기를 직접 증가시킵니다:

```
object size = header + fields + padding
```

필드 수가 많아질수록 다음이 증가합니다:

- heap usage 증가
- object size 증가
- memory fragmentation 증가

특히 참조 타입 필드는 **pointer/reference storage**를 추가로 요구합니다.

### CPU Cache

CPU는 지속적으로 다음을 수행합니다:

- field read / field write
- pointer dereference
- cache fetch

> 필드 배치와 접근 패턴은 **CPU cache locality에 직접 영향**을 줍니다.

---

## 4. 왜 중요한가

인스턴스 변수는 **"실행 중 상태(State)"의 핵심**입니다.

코드는 로직만 정의하지만, **instance variables = actual runtime data**입니다.

| 이유 | 설명 |
|------|------|
| 상태 유지 | runtime data persistence |
| 객체 독립성 | instance isolation |
| 동시성 처리 | shared state coordination |
| 데이터 흐름 | runtime mutation |
| 메모리 구조 | object layout 결정 |

> 실행 중인 시스템은 결국 **fields changing over time**의 연속입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실무에서는 인스턴스 변수 설계가 **메모리/동시성 장애와 직접 연결**됩니다.

| 문제 | 원인 |
|------|------|
| excessive heap usage | oversized objects |
| memory leak | retained references |
| stale state | shared mutable fields |
| cache inefficiency | poor field layout |
| race condition | unsynchronized shared fields |
| GC pressure | large object graph |

특히 **reference retention** 문제가 중요합니다:

```
instance field → large object reference → retained heap explosion
```

또한 **shared mutable field**는 다음을 유발합니다:

- race condition
- inconsistent state
- synchronization overhead

---

## 6. 핵심 메커니즘

### (1) Field Layout

런타임은 필드를 메모리에 배치합니다:

```
[ Header ]
[ Primitive Fields ]
[ Reference Fields ]
[ Padding ]
```

### (2) Primitive vs Reference

**Primitive-like Data** — 값 자체를 저장합니다: `integer`, `float`, `boolean`

**Reference-like Data** — 다른 객체 위치를 저장합니다:

```
pointer/reference → external object
```

### (3) Object Graph

참조 필드는 객체 연결 구조를 형성합니다:

```
Object A → Object B → Object C
```

GC는 이 연결 구조를 탐색합니다.

### (4) Field Access

CPU는 다음으로 필드에 접근합니다:

```
base object address + field offset
```

필드 위치(offset)는 런타임이 계산합니다.

### (5) Mutation

인스턴스 변수는 실행 중 지속적으로 변경됩니다:

- counter update
- cache update
- state transition
- request mutation

### (6) Memory Alignment

필드 순서에 따라 **padding / alignment overhead**가 달라질 수 있습니다.

### (7) Shared State

여러 실행 흐름이 동일 객체를 공유하면 **field synchronization** 문제가 발생할 수 있습니다.

---

## 7. Linux / Runtime / K8s에서 관측 방법

### Runtime (직접 관측)

| 항목 | 의미 |
|------|------|
| object size | field 포함 크기 |
| retained heap | 참조 유지량 |
| allocation rate | 객체 생성량 |
| object graph | 참조 연결 구조 |

**대표 도구:** heap dump analyzer, `jmap`, `jcmd`, memory profiler

**대표 관측 신호:** large retained objects, deep reference graph, field-heavy objects

### Linux (간접 관측)

```bash
top
vmstat
perf
pidstat
```

| 현상 | 의미 |
|------|------|
| RSS growth | field-heavy heap |
| cache miss | poor locality |
| CPU spike | synchronization |
| page pressure | oversized objects |

### Kubernetes

**대표 현상:** OOMKilled, memory pressure, high GC pause, container restart

> **large object graph retention**은 컨테이너 메모리 문제의 핵심 원인 중 하나입니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*