# CPU Load/Store Unit (LSU)와 메모리 접근 구조

## 1. Load/Store Unit (LSU)란?

Load/Store Unit(LSU)는 **CPU 내부에서 메모리 접근을 담당하는 하드웨어 실행 유닛**입니다.

ALU/FPU가 연산을 수행하려면 메모리에서 데이터를 읽어야 하고, 연산 결과는 다시 메모리에 기록되어야 합니다. 이 과정 전체를 LSU가 담당합니다.

| 역할 | 설명 |
|---|---|
| **Load** | 메모리 데이터를 CPU Register로 읽어옴 |
| **Store** | CPU Register 데이터를 메모리에 기록 |
| **Address Generation** | 메모리 주소 계산 |
| **Cache Access** | L1/L2/L3 Cache 접근 |
| **Memory Ordering** | 메모리 접근 순서 보장 |
| **Store Buffer Control** | 쓰기 버퍼 관리 |

</br>

## 2. CPU 내부 데이터 처리 흐름

```text
RAM
 ↓
L3 Cache
 ↓
L2 Cache
 ↓
L1 Cache
 ↓
Load/Store Unit (LSU)
 ↓
CPU Register
 ↓
ALU / FPU 연산 수행
 ↓
Register 결과 저장
 ↓
Load/Store Unit (LSU)
 ↓
Cache / RAM 기록
```

</br>

## 3. Load 동작 과정

Load는 메모리의 데이터를 CPU Register로 읽어오는 과정입니다.

```java
int value = counter.value;
```

### 3-1. Address Generation

LSU는 먼저 객체의 실제 메모리 주소를 계산합니다.

```text
counter 객체 주소 + value field offset
```

### 3-2. Cache 계층 탐색

LSU는 가장 가까운 Cache부터 순서대로 탐색합니다.

```text
L1 Cache → L2 Cache → L3 Cache → RAM
```

### 3-3. Cache Hit / Cache Miss

| 상태 | 설명 | 영향 |
|---|---|---|
| **Cache Hit** | 필요한 데이터가 Cache에 존재 | 빠른 접근 |
| **Cache Miss** | Cache에 데이터 없음 → RAM 접근 발생 | CPU Stall 주요 원인 |

Cache Miss 경로:
```text
L1 Miss → L2 Miss → L3 Miss → RAM Access
```

### 3-4. Register 적재

읽어온 데이터는 CPU Register에 적재된 후 ALU/FPU가 연산을 수행합니다.

```text
Memory → Register → ALU/FPU 연산
```

</br>

## 4. Store 동작 과정

Store는 CPU Register의 데이터를 메모리에 기록하는 과정입니다.

```java
counter.value = 10;
```

### 4-1. Store Buffer 저장

대부분의 CPU는 즉시 RAM에 기록하지 않고 **Store Buffer**에 임시 저장합니다.

```text
Register → Store Buffer
```

Store Buffer는 CPU 실행을 멈추지 않기 위한 비동기 쓰기 최적화 구조입니다.

### 4-2. Cache 및 RAM 반영

```text
Store Buffer
 ↓
L1 Cache
 ↓
L2 / L3 Cache
 ↓
RAM
```

</br>

## 5. Store Buffer와 메모리 가시성 문제

Store Buffer로 인해 멀티스레드 환경에서 **메모리 가시성 문제**가 발생할 수 있습니다.

```java
// Thread A
flag = true;

// Thread B
while (!flag) { }
```

Thread A가 값을 변경해도 Store Buffer → RAM 반영 이전에는 다른 CPU Core가 이전 값을 읽을 수 있습니다.

**해결 기술:**

| 기술 | 역할 |
|---|---|
| `volatile` | Store Buffer flush 및 가시성 보장 |
| `synchronized` | 메모리 배리어 생성 |
| `Lock` | 메모리 접근 순서 강제 |
| `Memory Barrier` | CPU 재정렬 방지 |

---

## 6. Cache Line과 메모리 접근 효율

### Cache Line

CPU는 메모리를 **64 Bytes** 단위(Cache Line)로 읽습니다.

### Spatial Locality

연속된 메모리 접근은 Cache 효율을 높입니다.

```java
int[] arr = new int[1000];  // 메모리 연속 배치 → Cache 친화적
```

### Pointer Chasing 문제

연결 리스트처럼 메모리가 흩어진 구조는 Cache Miss를 증가시킵니다.

```text
Node → Node → Node  (메모리 산재 → 지속적 RAM 접근 유발)
```

---

## 7. Memory Wall 문제

CPU 연산 속도와 메모리 접근 속도의 차이를 **Memory Wall**이라고 합니다.

| 구성 요소 | 접근 속도 |
|---|---|
| ALU 연산 | ~1 cycle |
| Register | ~1 cycle |
| L1 Cache | ~4 cycle |
| L2 Cache | ~12 cycle |
| L3 Cache | ~40 cycle |
| RAM | ~200 cycle |

연산 능력이 충분해도 메모리 데이터를 기다리며 **CPU Stall** 상태가 됩니다.

---

## 8. CPU Stall 주요 원인

| 원인 | 설명 |
|---|---|
| **Cache Miss** | RAM 접근 대기 |
| **Store Buffer Full** | 쓰기 버퍼 포화 |
| **Memory Ordering** | 메모리 순서 대기 |
| **False Sharing** | Cache Line 충돌 |
| **TLB Miss** | 주소 변환 실패 |

---

## 9. False Sharing 문제

서로 다른 스레드가 **동일 Cache Line을 수정**하면 Cache Coherency 비용이 증가합니다.

```text
Thread A → counter1 수정
Thread B → counter2 수정
→ 두 값이 동일 Cache Line에 존재하면 CPU가 충돌로 인식
```

결과:
```text
Cache Invalidation 증가 → 성능 저하
```

---

## 10. LSU 성능 최적화 전략

| 전략 | 목적 |
|---|---|
| 연속 메모리 사용 | Cache Hit 증가 |
| 객체 수 감소 | Pointer Chasing 감소 |
| 배열 기반 구조 활용 | Spatial Locality 향상 |
| Lock 최소화 | Memory Barrier 감소 |
| NUMA 고려 | 원격 메모리 접근 감소 |
| Cache Line Padding | False Sharing 방지 |

---

## 11. JVM과 LSU 관계

### JVM Heap 접근 구조

```text
Java Thread
 ↓
JVM Heap Object
 ↓
CPU Cache
 ↓
RAM
```

모든 Java 객체 접근은 결국 LSU를 통해 수행됩니다.

### `volatile` 동작

```java
volatile boolean flag;
```

CPU Cache 및 Store Buffer와 관련된 **메모리 가시성**을 보장합니다.

### `synchronized` 동작

```java
synchronized(lock) { }
```

**Memory Barrier**를 생성하여 CPU 재정렬과 Cache 불일치를 제어합니다.

---

## 12. 전체 개념 정리

| 항목 | 설명 |
|---|---|
| **LSU** | CPU의 메모리 접근 담당 유닛 |
| **Load** | 메모리 → Register |
| **Store** | Register → 메모리 |
| **Cache Hit** | 빠른 메모리 접근 |
| **Cache Miss** | RAM 접근 발생 → Stall |
| **Store Buffer** | 비동기 쓰기 최적화 |
| **Memory Wall** | CPU 대비 느린 메모리 |
| **CPU Stall** | 메모리 응답 대기 상태 |
| **False Sharing** | Cache Line 충돌 |
| **volatile / Lock** | 메모리 가시성 보장 |

---

## 핵심 결론

현대 CPU 성능 문제의 상당수는 연산 자체보다 **메모리 접근 지연**에서 발생합니다.

```text
메모리 접근 패턴 최적화
  → Cache 효율 향상
  → LSU 효율 향상
  → CPU Stall 감소
  → 전체 처리량 향상
```

이는 JVM / Kubernetes / 논블로킹 서버 / 대규모 트래픽 제어 설계까지 모두 연결되는 핵심 기반 개념입니다.

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*