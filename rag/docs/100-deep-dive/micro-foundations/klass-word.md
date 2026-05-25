# 클래스 워드 (Klass Word)

> 정독: 0회

## 1. 이 기술이 무엇인가

클래스 워드(Klass Word)는:

> 객체 메모리 내부에 저장되는 타입 메타데이터 포인터

**핵심 역할: object → type metadata linkage**

객체는 메모리에 단순 비트 데이터로 존재하지만, 런타임은 다음을 알아야 합니다.

- 이 객체가 어떤 타입인지
- 어떤 메서드를 호출해야 하는지
- 어떤 필드를 가지는지
- 어떤 상속 계층에 속하는지

이를 위해 객체 내부에는 **runtime type reference metadata**가 포함됩니다. 그것이 클래스 워드입니다.

### 일반적 구조

```
[ mark / control metadata ]
[ type metadata pointer   ]  ← 클래스 워드
[ payload                 ]
```

즉, 클래스 워드는 **object identity-to-type bridge**입니다.

---

## 2. 시스템 어디에서 등장하는가

### 대표 등장 위치

| 위치 | 역할 |
|---|---|
| Object Header | 타입 포인터 저장 |
| Runtime Type System | 타입 식별 |
| Dynamic Dispatch | 메서드 결정 |
| GC | 객체 구조 해석 |
| Reflection/RTTI | 런타임 타입 검사 |
| VM Metadata System | 클래스 정보 연결 |

### 대표 흐름

```
object reference → object header → klass/type metadata → runtime behavior resolution
```

특히 object-oriented runtime, managed runtime, dynamic dispatch runtime에서 핵심입니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Memory + CPU Cache**입니다.

| 자원 | 영향 |
|---|---|
| Memory | 객체당 메타데이터 공간 증가 |
| CPU | 타입 확인 비용 |
| Cache | pointer dereference 증가 |
| Branch Prediction | dynamic dispatch 영향 |
| GC | 객체 구조 탐색 |

특히 **pointer dereference locality**가 중요합니다. 클래스 워드는 **extra memory indirection**을 발생시킵니다.

---

## 4. 왜 중요한가

런타임은 객체를 타입 없이 다룰 수 없습니다. 다음 작업 모두가 **runtime type identity**에 의존합니다.

- method dispatch
- interface resolution
- polymorphism
- runtime type check
- reflection
- serialization

따라서 클래스 워드는 **runtime type governance anchor**입니다. 없으면 다음이 불가능합니다.

- virtual method dispatch
- type checking
- inheritance resolution
- runtime introspection

---

## 5. 실제 장애와 어떤 관련이 있는가

### 대표 문제

| 문제 | 설명 |
|---|---|
| invalid type pointer | 타입 식별 실패 |
| corrupted metadata | 런타임 붕괴 |
| bad dispatch | 잘못된 메서드 호출 |
| GC misinterpretation | 객체 구조 해석 실패 |
| heap corruption | 객체 그래프 붕괴 |

특히 위험한 것은 **invalid klass metadata reference**입니다. 대표 결과는 다음과 같습니다.

- crash
- segmentation fault
- invalid cast
- runtime corruption
- undefined behavior

또한 클래스 메타데이터 접근이 많아지면 다음이 발생할 수 있습니다.

- cache miss 증가
- branch miss 증가
- dispatch latency 증가

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Object-to-Type Mapping

객체는 payload만으로 타입을 알 수 없습니다. 따라서 `object → type metadata pointer` 구조를 가지며, 클래스 워드가 바로 이 포인터입니다.

### Runtime Dispatch

메서드 호출 시 런타임은 **klass metadata lookup**을 수행합니다. 이를 통해 method table, interface table, field layout 정보를 가져옵니다.

### Dynamic Type Checking

`instanceof`, `cast`, `reflection` 등의 런타임 타입 검사는 클래스 워드를 기반으로 수행됩니다.

### Metadata Indirection

객체 내부에는 전체 타입 정보가 들어있지 않습니다. 대신 **pointer-based metadata indirection**을 사용합니다.

```
object → klass pointer → shared type metadata
```

이 방식은 memory efficiency, metadata reuse, shared runtime governance를 가능하게 합니다.

### Shared Metadata Model

동일 타입 객체 수백만 개가 있어도 **single shared type metadata**만 유지합니다. 클래스 설계도 자체는 공유됩니다.

### Compressed Metadata Pointer

현대 런타임은 **compressed pointer encoding**을 자주 사용합니다. 목적은 object header reduction, cache efficiency, memory footprint 감소입니다.

```
compressed reference → runtime decode → full address reconstruction
```

### Metadata Resolution

클래스 워드는 직접 동작하지 않습니다. 실행 시 **klass metadata resolution**이 수행됩니다.

| 작업 | 설명 |
|---|---|
| virtual dispatch | 메서드 선택 |
| field offset lookup | 필드 위치 계산 |
| interface resolution | 인터페이스 매핑 |
| subtype checking | 상속 판별 |

### GC Interaction

GC는 객체 순회 시 **klass metadata**를 사용해 객체 크기, reference field 위치, 배열 구조 등을 파악합니다.

### Offset-Based Access

객체 접근은 보통 `base address + field offset` 방식입니다. 클래스 메타데이터는 **field offset map**을 제공합니다.

### Runtime Identity Stability

객체 payload는 바뀔 수 있어도 **type identity linkage**는 유지되어야 합니다. 즉, 클래스 워드는 **stable runtime type anchor** 역할을 합니다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

직접 보이지 않지만, 다음에서 간접 관측됩니다.

**대표 관측 명령어:** `perf`, `pmap`, `cat /proc/<pid>/maps`

| 현상 | 의미 |
|---|---|
| excessive metadata access | cache inefficiency |
| heap corruption | invalid type metadata |
| runtime crash | bad type resolution |

### Runtime

| 항목 | 의미 |
|---|---|
| object layout | header 구조 |
| class metadata | 타입 구조 |
| heap dump | 객체 타입 정보 |
| dispatch trace | 메서드 해석 |
| allocation profile | 타입별 객체 생성 |

**대표 도구:** heap analyzer, runtime inspector, metadata profiler, allocation tracer

### Kubernetes

| 현상 | 원인 |
|---|---|
| memory overhead 증가 | object header inflation |
| CPU overhead 증가 | dispatch/type resolution |
| GC latency 증가 | metadata traversal |
| pod OOM | metadata-heavy object density |

**관측 포인트:** heap usage, object count, allocation rate, GC metrics, CPU cache miss

특히 **object density vs metadata overhead** 비율이 중요합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*