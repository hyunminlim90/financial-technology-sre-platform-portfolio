# 필드 (Fields)

> 정독: 0회

## 1. 이 기술이 무엇인가

필드는:

> **객체 내부에 저장되는 상태 데이터 공간**

**대표 예시:** 숫자 값, 문자열, 참조 주소, 플래그 상태, 카운터, 포인터, 구조체 멤버

**핵심: 객체의 현재 상태를 저장하는 메모리 영역**

필드는 객체 인스턴스마다 독립적으로 존재합니다. 같은 클래스에서 생성된 객체라도 서로 다른 필드 값을 가질 수 있어, 서로 다른 상태를 유지할 수 있습니다.

<details>
<summary>Deep Dive</summary></br>

Object Layout(객체 레이아웃) [[M]](../../100-deep-dive/micro-foundations/object-layout.md)  
Store(스토어) [[M]](../../100-deep-dive/micro-foundations/store.md)  
Object State(객체의 상태) [[M]](../../100-deep-dive/micro-foundations/object-state.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

| 영역 | 역할 |
|------|------|
| Heap | 객체 상태 저장 |
| Stack | 참조 변수 저장 |
| CPU Register | load/store 대상 |
| Cache | hot field caching |
| Object Layout | memory offset 관리 |
| Runtime Metadata | field offset/type 관리 |

**실제 실행 흐름:**

```
object reference
→ dereference
→ field offset 계산
→ memory load/store
```

---

## 3. 어떤 자원에 가장 영향이 큰가

필드는 **메모리 중심 기술**입니다.

| 자원 | 영향 |
|------|------|
| Memory(RAM) | 객체 저장 |
| CPU Cache | locality 영향 |
| Memory Bus | field access traffic |
| CPU | load/store instruction |
| Heap | allocation density |

특히 객체 수 증가, 필드 수 증가, large object, random field access는 cache miss 증가의 원인이 됩니다.

---

## 4. 왜 중요한가

필드는 **프로그램의 상태(State)를 구성**합니다. 프로그램은 결국 필드 값을 읽고, 필드 값을 수정하며, 상태를 변화시키는 과정입니다.

| 역할 | 설명 |
|------|------|
| 상태 저장 | 객체 상태 유지 |
| 비즈니스 데이터 보존 | runtime data persistence |
| 객체 식별성 | 서로 다른 상태 유지 |
| 연산 입력 | method execution input |
| 상태 전이 | application behavior 변화 |

필드가 없으면 객체는 상태를 유지할 수 없습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### Cache Miss 증가

필드 배치가 비효율적이면 memory locality 악화, CPU stall 증가가 발생할 수 있습니다.

### False Sharing

멀티스레드 환경에서 서로 다른 스레드가 같은 cache line 내부 field를 수정하면 CPU cache contention이 발생할 수 있습니다.

### Memory Bloat

불필요한 필드 증가 시 heap expansion, GC pressure 증가, memory fragmentation 증가가 발생할 수 있습니다.

### Race Condition

동시 field update 시 inconsistent state, corruption이 발생할 수 있습니다.

### Null/Invalid Reference

참조 필드 손상 시 invalid dereference, segmentation fault, runtime exception이 발생할 수 있습니다.

### Hot Field Bottleneck

특정 필드 접근 과다 시 cache contention, lock contention이 발생할 수 있습니다.

---

## 6. 핵심 메커니즘

### 1) 객체 생성 시 필드 공간 확보

객체 생성 시 heap allocation이 발생하며, 이때 필드 공간도 함께 확보됩니다.

### 2) Object Layout 결정

컴파일러/런타임이 field size, alignment, offset을 결정합니다.

| Field | Offset |
|-------|--------|
| header | 0 |
| int a | 8 |
| pointer b | 16 |

### 3) Base Address 확보

객체 참조를 통해 object base address를 확보합니다.

### 4) Offset 계산

필드 접근 시 다음과 같이 주소를 계산합니다.

```
field_address = object_base + offset
```

### 5) Load/Store 수행

CPU가 LOAD, STORE, MOV instruction을 수행합니다.

### 6) Cache 접근

대부분의 field access는 L1/L2/L3 cache를 우선 조회하고, cache miss 시 RAM에 접근합니다.

### 7) 상태 변경 반영

필드 값 변경 시 cache update, memory synchronization, coherence protocol이 발생할 수 있습니다.

---

## 7. Linux/Runtime/K8s에서 관측 방법

### Linux

```bash
# 메모리 사용량
top
htop
free -m
vmstat

# 객체 메모리 분석
pmap <pid>

# CPU Cache 분석 (cache-misses, cache-references)
perf stat
perf record

# 구조체/객체 레이아웃 확인
pahole
objdump
readelf

# 메모리 접근 추적
perf mem
valgrind
```

### Kubernetes

필드 구조 비효율은 pod memory 증가, OOMKill, GC spike, latency 증가로 이어질 수 있습니다.