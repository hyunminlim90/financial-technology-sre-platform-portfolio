## 공유 메모리(Shared Memory)와 동기화 정리

공유 메모리(Shared Memory)는 **여러 개의 프로세스(Process)나 스레드(Software Thread)가 공유해서 읽고 쓸 수 있는 공용 메모리 공간**을 의미합니다.

</br>

## 1. 계층별 공유 메모리의 실체

### 1-1. 하드웨어 관점 (Main Memory / RAM)

하드웨어 관점에서 공유 메모리는 **여러 CPU 코어가 동시에 접근 가능한 물리적인 메인 메모리(RAM)** 입니다.

예시: DDR4 / DDR5 / Main Memory

```text
CPU Core 1
CPU Core 2
CPU Core 3
     ↓
  Shared RAM
```

### 1-2. 소프트웨어 관점 (JVM Heap)

소프트웨어 관점에서는 **JVM Heap 영역**이 대표적인 공유 메모리입니다.

모든 Java Thread가 동일한 Heap 객체를 함께 읽고 수정할 수 있기 때문입니다.

```java
class Counter {
    int value = 0;
}
```

위 객체가 Heap에 생성되면:

```text
Thread A → Counter.value 접근
Thread B → Counter.value 접근
Thread C → Counter.value 접근

→ 모든 스레드가 동일 객체를 공유
```

## 2. 왜 "공유 메모리"가 중요한가?

동기화(Synchronization)가 필요한 이유는 바로 이 공유 메모리 때문입니다.

### Stack 메모리 — 개별 공간 (동기화 불필요)

각 스레드는 자기만의 Stack 메모리를 가집니다.

```text
Thread A Stack  (자기 자신만 접근 가능)
Thread B Stack  (자기 자신만 접근 가능)
Thread C Stack  (자기 자신만 접근 가능)
```

`Local Variable` / `Method Parameter` / `Method Frame` 등은 안전합니다.

### Heap / Static 메모리 — 공유 공간 (동기화 필요)

반면 Heap이나 Static 영역은 **모든 Thread가 함께 접근 가능**합니다.

```text
Shared Object
Static Variable
Singleton Object
Cache
Connection Pool
```

등은 모두 공유 메모리이며, 여기서 문제가 발생합니다.

## 3. Race Condition (데이터 오염)

공유 메모리는 여러 스레드가 동시에 수정할 수 있기 때문에 Race Condition이 발생할 수 있습니다.

```java
count++;
```

이 코드는 실제로 3단계 작업입니다:

```text
1. count 읽기
2. +1 계산
3. 다시 저장
```

Thread A와 Thread B가 동시에 실행하면:

```text
A가 읽음 → 5
B가 읽음 → 5
A 저장  → 6
B 저장  → 6   ← 최종값이 7이 아니라 6
```

이것이 **Race Condition**입니다.

## 4. CPU 동기화와의 관계

공유 메모리에 여러 CPU Core가 접근하면 다음 두 가지 문제가 발생합니다.

### 4-1. [가시성 문제 (Visibility)](../20-deep-dive/thread-cpu-memory-flow.md)

```text
Thread A가 값을 변경했지만
Thread B CPU Cache에는 이전 값이 남아 있음

→ RAM 값 ≠ CPU Cache 값
```

### 4-2. 원자성 문제 (Atomicity)

```java
count++;  // 원자적(Atomic) 작업이 아님
```

```text
읽기 → 계산 → 저장
      ↑
  중간에 다른 스레드가 끼어들 수 있음
```

## 5. 동기화 기술이 필요한 이유

이 문제들을 해결하기 위해 CPU 동기화 기술과 Lock 메커니즘이 필요합니다.

**대표 기술:**

| 기술 | 분류 |
|---|---|
| `synchronized` | Java 내장 Lock |
| `ReentrantLock` | 명시적 Lock |
| `CAS` (Compare And Swap) | Lock-free 원자 연산 |
| `volatile` | 메모리 가시성 보장 |
| `AtomicInteger` | 원자적 정수 연산 |
| `Semaphore` | 접근 수 제한 |
| `Mutex` | 상호 배제 Lock |
| `Spin Lock` | Busy Waiting Lock |

## 6. Lock Contention (락 경합)

모든 스레드가 공유 메모리를 동시에 접근하려 하면 **"누가 먼저 사용할 것인가?"** 문제가 발생합니다.

Lock을 얻기 위해 경쟁하는 상황을 **Lock Contention**이라고 합니다.

```text
Thread A → Lock 대기
Thread B → Lock 사용 중
Thread C → Lock 대기
```

## 7. 전체 흐름 정리

```text
CPU Core
   ↓
Software Thread
   ↓
Shared Memory (Heap / RAM)
   ↓
Synchronization
   ↓
Lock / CAS / Atomic Operation
```

## 핵심 요약

공유 메모리란 **모든 스레드가 함께 사용하는 거대한 공용 데이터 저장소**입니다.

대표적으로 RAM / JVM Heap / Static 영역 등이 있으며,  
`Lock Contention` / `Race Condition` / `Visibility` / `Atomicity` 문제들이 모두 여기서 발생합니다.

> **공유 메모리 = 멀티스레드 전쟁터**

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*