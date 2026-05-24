# 필드 값 (Field Values)

> 정독: 0회

## 1. 이 기술이 무엇인가

필드 값은:

> 실행 중인 인스턴스 내부 필드(Field)에 **실제로 저장된 데이터**

```
Field       = storage slot
Field Value = actual runtime data
```

- **필드(Field):** 저장 공간 정의
- **필드 값(Field Value):** 그 공간에 기록된 실제 상태 데이터

**대표 예시 구조:**

```
Object
    ├─ Header
    ├─ Field A = 10
    ├─ Field B = true
    └─ Field C = reference address
```

필드 값은 실행 중 지속적으로 변경됩니다:

- counter 증가
- 상태 변경
- reference 교체
- cache update
- request state mutation

---

## 2. 시스템 어디에서 등장하는가

필드 값은 런타임 객체 시스템 전체에서 등장합니다.

| 영역 | 역할 |
|------|------|
| Heap Memory | 객체 상태 저장 |
| Runtime Object Model | 필드 상태 유지 |
| CPU Cache | 자주 접근되는 값 캐싱 |
| GC Traversal | reference 탐색 |
| Serialization | 상태 추출 |
| Network Payload | 데이터 전송 |

**실행 흐름:**

```
Instance Allocation
→ Field Initialization
→ Runtime Mutation
→ State Transition
→ GC Reclamation
```

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 영향이 큰 자원은 **Memory / CPU Cache**입니다.

### Memory

필드 값은 실제 메모리 공간을 사용합니다. 특히 다음은 heap 사용량을 크게 증가시킵니다:

- large fields
- deep references
- large arrays

필드 값 크기 증가 시 다음이 발생합니다:

- heap growth
- memory fragmentation
- retained memory 증가

### CPU

CPU는 지속적으로 필드 값을 읽고 씁니다:

- comparison / arithmetic
- state transition
- pointer dereference

> 필드 값 접근은 **CPU 실행의 핵심 경로**입니다.

### Cache

필드 값은 CPU cache locality에 직접 영향을 줍니다:

```
poor object layout → cache miss increase
```

---

## 4. 왜 중요한가

프로그램 실행의 본질은 결국 **field values changing over time**입니다.

| 이유 | 설명 |
|------|------|
| 상태 유지 | runtime state persistence |
| 비즈니스 로직 | 조건/연산 기반 |
| 동시성 제어 | shared mutable state |
| 객체 관계 | reference graph 구성 |
| GC 동작 | reference reachability |

> 필드 값은 **"실행 중 시스템 상태" 그 자체**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실무에서는 필드 값 관리 문제가 **매우 중요한 장애 원인**입니다.

| 문제 | 원인 |
|------|------|
| stale data | outdated field state |
| race condition | concurrent mutation |
| memory leak | retained references |
| cache inefficiency | scattered field access |
| excessive heap retention | deep object graph |
| serialization explosion | oversized field graph |

특히 **reference field retention** 문제가 중요합니다:

```
field value → reference → large object graph retained
```

결과: GC pressure 증가, heap retention 증가, OOM 발생 가능

또한 **mutable shared fields**는 다음을 유발할 수 있습니다:

- inconsistent state
- synchronization contention
- data corruption

---

## 6. 핵심 메커니즘

### (1) Direct Value Storage

기본 데이터형은 값 자체를 저장합니다: `integer`, `float`, `boolean` (bit pattern directly stored)

### (2) Reference Storage

참조형은 주소를 저장합니다:

```
field value = pointer/reference
```

실제 데이터는 외부 객체에 존재합니다.

### (3) Field Offset Access

런타임은 다음으로 필드 값에 접근합니다:

```
object base address + field offset
```

### (4) Runtime Mutation

필드 값은 실행 중 계속 변경됩니다:

- increment
- assignment
- reference replacement
- synchronization update

### (5) Object Graph Formation

참조 필드들은 객체 그래프를 형성합니다:

```
Object A → Object B → Object C
```

GC는 이 그래프를 탐색합니다.

### (6) Visibility & Synchronization

멀티스레드 환경에서는 다음이 중요합니다:

- field visibility
- memory ordering
- synchronization

> 필드 값은 단순 저장 데이터가 아니라 **동시성 시스템의 핵심 요소**입니다.

### (7) Alignment & Packing

런타임은 필드 배치를 최적화합니다:

- padding 최소화
- cache locality 향상
- access efficiency 향상

---

## 7. Linux / Runtime / K8s에서 관측 방법

### Runtime (직접 관측)

| 항목 | 의미 |
|------|------|
| object graph | 참조 구조 |
| retained heap | 유지된 필드 값 |
| allocation rate | 객체 생성량 |
| field-heavy objects | 대형 상태 객체 |

**대표 도구:** heap dump analyzer, `jmap`, `jcmd`, memory profiler

**대표 관측 신호:** large retained references, deep object graph, shared mutable state

### Linux (간접 관측)

```bash
top
vmstat
perf
pidstat
```

| 현상 | 의미 |
|------|------|
| RSS growth | retained field values |
| CPU spike | synchronization/cache miss |
| page pressure | oversized heap |
| cache miss increase | scattered access |

### Kubernetes

**대표 현상:** OOMKilled, high memory usage, GC pause spike, container restart

> **retained object graph**는 컨테이너 메모리 문제의 핵심 원인입니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*