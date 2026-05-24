# 객체 메타데이터 헤더 (Object Metadata Header)

> 정독: 0회

## 1. 이 기술이 무엇인가

객체 메타데이터 헤더는:

> 런타임이 객체를 관리하기 위해 **객체 메모리 앞부분에 저장하는 제어 정보 영역**

객체의 실제 데이터(Field) 이전에 위치하며, 런타임이 객체의 상태와 타입을 식별·추적·동기화하는 데 사용됩니다.

| 역할 | 설명 |
|------|------|
| 타입 식별 | 어떤 클래스/타입의 객체인지 |
| 동기화 상태 | lock/monitor 상태 |
| GC 상태 | mark/age/reference 상태 |
| 런타임 관리 | identity/hash/runtime flags |

**일반 구조:**

```
[ Object Header ]
    ├─ Runtime Metadata
    ├─ Type Pointer
    └─ Synchronization State

[ Object Fields ]
    ├─ instance field
    ├─ primitive data
    └─ reference data
```

<details>
<summary>Deep Dive</summary></br>

Java Virtual Machine(자바 가상 머신) [[M]](../../100-deep-dive/micro-foundations/java-virtual-machine.md)  
Instance(인스턴스) [[M]](../../100-deep-dive/micro-foundations/instance.md)  
Object Lifecycle Management(객체 생명 주기 관리) [[M]](../../100-deep-dive/micro-foundations/object-lifecycle-management.md)  
Heap Area(힙 영역) [[M]](../../100-deep-dive/micro-foundations/heap-area.md)  
Object Header(객체 헤더) [[M]](../../100-deep-dive/micro-foundations/object-header.md)  
Header Injection(헤더 주입) [[M]](../../100-deep-dive/micro-foundations/header-injection.md)  
Object Control Metadata(객체 제어 메타데이터) [[M]](../../100-deep-dive/micro-foundations/object-control-metadata.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

객체 기반 런타임에서는 거의 항상 존재합니다.

| 계층 | 위치 |
|------|------|
| Runtime Heap | 객체 시작 주소 |
| GC System | mark/sweep metadata |
| Synchronization | monitor/lock control |
| Type System | runtime type resolution |
| Memory Manager | allocation tracking |

**실행 흐름:**

```
Object Allocation
→ Header Initialization
→ Runtime Tracking
→ GC/Synchronization Interaction
```

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 영향이 큰 자원은 **Memory / CPU**입니다.

### Memory

객체마다 헤더가 추가되므로:

```
actual memory = object data + metadata header + alignment padding
```

작은 객체가 매우 많을 경우 다음 문제가 발생할 수 있습니다:

- memory overhead 증가
- cache locality 악화
- heap fragmentation 증가

### CPU

런타임은 객체 헤더를 지속적으로 읽습니다:

- type check
- locking
- monitor enter/exit
- GC marking
- identity lookup

> 객체 헤더 접근은 **매우 빈번한 CPU 경로**입니다.

---

## 4. 왜 중요한가

객체 헤더는 **런타임 제어의 핵심**입니다.

헤더가 없다면 런타임은 다음을 알 수 없습니다:

- 객체 타입
- lock 상태
- GC 상태
- synchronization ownership

| 이유 | 설명 |
|------|------|
| GC 동작 | 객체 생존 여부 추적 |
| synchronization | thread safety |
| runtime dispatch | type resolution |
| reflection/runtime inspection | metadata access |
| object identity | hash/identity 관리 |

> **runtime-managed object system**의 핵심 기반입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

객체 헤더 자체는 작은 구조지만, 대규모 시스템에서는 매우 중요합니다.

| 문제 | 원인 |
|------|------|
| excessive heap usage | object overhead explosion |
| GC pressure | tiny object proliferation |
| lock contention | monitor inflation |
| cache miss increase | poor object locality |
| allocation storm | metadata allocation overhead |

특히 **small object explosion**이 중요합니다.

다음과 같은 객체가 매우 많아지면:

- request wrapper
- temporary object
- serialization object
- message object

```
payload size << object overhead
```

상황이 발생할 수 있으며, 결과적으로 다음이 발생합니다:

- GC frequency 증가
- allocation latency 증가
- CPU cache efficiency 감소

---

## 6. 핵심 메커니즘

### (1) Runtime Metadata

객체 상태를 저장합니다:

- GC mark
- age
- lock state
- hash state

### (2) Type Pointer

객체가 어떤 타입인지 연결합니다:

```
object → class metadata → method table → runtime type info
```

### (3) Lock State

멀티스레드 동기화 상태를 저장합니다.

| 상태 | 의미 |
|------|------|
| unlocked | lock 없음 |
| lightweight lock | 경량 동기화 |
| heavyweight monitor | contention 발생 |
| biased/optimized state | runtime optimization |

### (4) GC Metadata

GC는 객체 헤더를 이용하여 다음을 수행합니다:

- mark
- age tracking
- forwarding
- relocation

### (5) Alignment

객체는 CPU alignment 규칙에 맞춰 정렬됩니다:

```
header + field + padding
```

### (6) Runtime Object Identity

런타임은 객체 헤더를 통해 객체 identity를 추적합니다:

- identity hash
- monitor ownership
- runtime state bits

---

## 7. Linux / Runtime / K8s에서 관측 방법

### Runtime (직접 관측 대상)

| 항목 | 의미 |
|------|------|
| object size | header 포함 크기 |
| heap histogram | object distribution |
| allocation rate | object churn |
| monitor contention | synchronization 상태 |

**대표 도구:** `jmap`, `jcmd`, `jstat`, `jstack`

**중요 관측 신호:**

- millions of tiny objects
- high allocation rate
- monitor contention
- GC pressure

### Linux (간접 관측)

```bash
top
perf
vmstat
pidstat
```

| 현상 | 의미 |
|------|------|
| high CPU in runtime | metadata traversal |
| memory growth | object proliferation |
| cache miss | poor locality |
| context switch spike | lock contention |

### Kubernetes

다음 형태로 간접적으로 나타납니다:

- OOMKilled
- high memory RSS
- GC latency increase
- CPU spike

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*