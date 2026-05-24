# 인스턴스 (Instance)

> 정독: 0회

## 1. 이 기술이 무엇인가

인스턴스는:

> 프로그램 실행 중 런타임이 메모리에 생성한 **실제 데이터 실체**

클래스·타입·구조체·템플릿 같은 "정의(Definition)"를 기반으로 생성되며, 실제 메모리를 점유하고 상태(State)를 가집니다.

| 개념 | 의미 |
|------|------|
| Definition | 설계 정보 |
| Instance | 실행 중 생성된 실체 |
| Runtime Object | 메모리 위 실제 데이터 |
| Stateful Entity | 값과 상태 보유 |

**핵심 구조:**

```
Type Definition
    ↓
Memory Allocation
    ↓
Runtime Instance
```

인스턴스는 일반적으로 다음 요소를 가집니다:

```
[ Metadata ]
[ Runtime State ]
[ User Data ]
```

<details>
<summary>Deep Dive</summary></br>

Instance Variables(인스턴스 변수) [[M]](../../100-deep-dive/micro-foundations/instance-variables.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

인스턴스는 거의 모든 런타임 시스템의 핵심입니다.

| 영역 | 역할 |
|------|------|
| Heap Memory | 동적 객체 저장 |
| Runtime Type System | 타입 연결 |
| GC System | 생명주기 추적 |
| Thread Execution | 데이터 처리 |
| Cache System | 상태 유지 |
| IPC/Network | 메시지 객체 생성 |

**대표 흐름:**

```
Request
→ Runtime Allocation
→ Instance Creation
→ Processing
→ Destruction/Reclaim
```

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 큰 영향은 **Memory / CPU**입니다.

### Memory

인스턴스는 실제 메모리를 점유합니다:

```
metadata + field data + alignment + runtime overhead
```

인스턴스 수가 많아질수록 다음이 증가합니다:

- heap growth
- memory fragmentation
- cache pressure
- GC overhead

### CPU

CPU는 지속적으로 다음을 수행합니다:

- allocation
- pointer traversal
- synchronization
- GC scanning
- reference resolution

> 인스턴스가 많을수록 **CPU의 런타임 관리 비용**도 증가합니다.

---

## 4. 왜 중요한가

인스턴스는 **"실행 가능한 데이터"의 핵심 단위**입니다.

코드는 정의만 제공하지만, **instance = actual runtime state**입니다.

| 이유 | 설명 |
|------|------|
| 상태 저장 | runtime data 유지 |
| 요청 처리 | request-specific data |
| 동시성 처리 | thread interaction |
| 메모리 관리 | GC lifecycle |
| 런타임 제어 | synchronization/ownership |

> 런타임 시스템은 결국 **"인스턴스들의 흐름"**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실무에서는 인스턴스 관리 문제가 **매우 흔한 장애 원인**입니다.

| 문제 | 원인 |
|------|------|
| Memory Leak | 참조 해제 실패 |
| GC Storm | excessive allocation |
| Heap Exhaustion | instance explosion |
| Cache Miss | poor memory locality |
| Lock Contention | shared instance synchronization |
| OOM | excessive retention |

특히 **too many short-lived instances** 문제가 중요합니다.

다음 객체들이 폭증할 경우:

- request object
- serialization buffer
- temporary collection
- parsing object

```
allocation rate 증가 → GC pause 증가 → CPU spike 발생
```

또 다른 문제는 **long-lived retained instance**입니다:

- cache retention
- static reference
- queue accumulation
- session leak

---

## 6. 핵심 메커니즘

### (1) Allocation

런타임은 메모리 공간을 확보하여 인스턴스를 생성합니다:

```
memory reservation
→ metadata initialization
→ field initialization
→ runtime registration
```

### (2) Runtime Identity

각 인스턴스는 다음을 가집니다:

- 메모리 위치
- runtime metadata
- type linkage

### (3) Reference-Based Access

대부분의 런타임은 다음 구조를 사용합니다:

```
reference → instance memory
```

> 변수는 실제 데이터 자체보다 **"위치 정보"를 들고 있는 경우**가 많습니다.

### (4) Lifecycle

인스턴스는 다음 생명주기를 가집니다:

```
created → referenced → used → unreachable → reclaimed
```

### (5) Shared vs Isolated State

| 유형 | 특징 |
|------|------|
| shared | 여러 실행 흐름 공유 |
| isolated | 독립 상태 유지 |

공유 인스턴스는 **synchronization 문제**가 발생할 수 있습니다.

### (6) Object Layout

일반적 메모리 구조:

```
[ Header ]
[ Runtime Metadata ]
[ Field Data ]
[ Padding ]
```

### (7) Allocation Path

현대 런타임은 allocation 성능 최적화를 위해 다음을 사용합니다:

- thread-local allocation
- bump pointer allocation
- region allocation

---

## 7. Linux / Runtime / K8s에서 관측 방법

### Runtime (직접 관측 대상)

| 항목 | 의미 |
|------|------|
| allocation rate | 객체 생성 속도 |
| heap occupancy | 인스턴스 점유량 |
| GC pause | reclaim 비용 |
| object histogram | 타입별 분포 |

**중요 관측 신호:** high allocation churn, large retained heap, millions of instances

**대표 도구:** `jmap`, `jcmd`, `jstat`, heap dump analyzer

### Linux (간접 관측)

```bash
top
vmstat
pidstat
perf
```

| 현상 | 의미 |
|------|------|
| RSS growth | heap 증가 |
| CPU spike | allocation/GC |
| page fault increase | memory pressure |
| cache miss | poor locality |

### Kubernetes

**대표 증상:** OOMKilled, memory limit exceeded, GC latency spike, container restart

> **memory request/limit** vs **runtime heap growth** 관리가 특히 중요합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*