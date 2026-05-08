## Java Thread → CPU Core → RAM 까지의 전체 처리 흐름과 동작 구조 (가시성 문제)

## 개요

```java
counter.value++;
```

이 한 줄의 코드는 내부적으로 다음 전체 계층을 거칩니다:

```text
Java 코드
  ↓
JVM Runtime
  ↓
OS Scheduler
  ↓
Physical CPU Core
  ↓
CPU Cache (L1 / L2 / L3)
  ↓
Memory Controller
  ↓
Physical RAM
```

## 1. 전체 계층 구조

| 계층 | 역할 |
|---|---|
| **Application Code** | 논리적 의도 생성 |
| **Software Thread** | 코드 실행 주체 |
| **JVM Heap** | 논리적 공유 메모리 |
| **OS Scheduler** | Thread ↔ CPU Core 매핑 |
| **CPU Core** | 실제 계산 수행 |
| **CPU Cache** | 고속 임시 저장 (L1/L2/L3) |
| **Memory Barrier** | 실행 순서 강제 |
| **Physical RAM** | 최종 데이터 저장 |

## 2. Software Thread와 JVM Heap

코드를 실행하는 주체는 **Software Thread**입니다.

```java
class Counter {
    int value = 0;
}
Counter counter = new Counter();
```

이 객체는 **JVM Heap**에 저장되며, 여러 Thread가 동시에 접근 가능합니다.

```text
Thread A ─┐
Thread B ─┼──→ Shared JVM Heap (counter 객체)
Thread C ─┘
```

## 3. Java Thread와 CPU Core의 관계

Java Thread는 **논리적인 실행 단위**입니다.  
실제 CPU Core를 할당하는 것은 **OS Scheduler**입니다.

```text
Java Thread
    ↓
Native Thread (OS Thread)
    ↓
CPU Core 배치 (OS Scheduler 결정)
```

OS Scheduler 배치 예시:

```text
Thread A → CPU Core 1
Thread B → CPU Core 3
Thread C → 대기
```

## 4. `count++`의 실제 CPU 처리 과정

```java
count++;
```

이 단순해 보이는 코드는 실제로 다음 단계로 분리됩니다:

```text
1. RAM에서 값 읽기
2. CPU Register에 적재
3. +1 계산 (ALU)
4. 결과를 Register에 저장
5. Cache에 반영
6. RAM에 반영
```

## 5. CPU Cache 구조와 문제

현대 CPU는 RAM 접근 속도가 느리기 때문에 **L1/L2/L3 Cache**를 사용합니다.

```text
CPU Core
   ↓
L1 Cache  (~1ns)
   ↓
L2 Cache  (~5ns)
   ↓
L3 Cache  (~20ns)
   ↓
RAM       (~100ns)
```

**문제:** CPU Core마다 Cache가 독립적으로 존재합니다.

```text
CPU Core 1 Cache → value = 5
CPU Core 2 Cache → value = 3   ← 같은 객체인데 다른 값
```

이것이 **Visibility Problem(메모리 가시성 문제)** 입니다.

## 6. 동기화가 필요한 이유

| 문제 | 설명 |
|---|---|
| **Race Condition** | 여러 Thread가 동시에 같은 값을 읽고 수정해 최종값이 잘못됨 |
| **Visibility Problem** | Core별 Cache 불일치로 Thread마다 다른 값을 보는 상태 |
| **Atomicity 부재** | `count++` 같은 연산이 중간에 끊길 수 있음 |

## 7. `synchronized` 내부 실행 흐름

```java
synchronized(lock) {
    count++;
}
```

실제 내부 처리 순서:

```text
1. Lock 획득
2. 다른 Thread 접근 차단
3. Memory Barrier 실행 (실행 순서 강제)
4. CPU Cache 동기화
5. count 읽기 → 계산 → 저장
6. Cache Flush (RAM 반영)
7. Memory Barrier 실행
8. Lock 해제
```

## 8. 동기화 기술의 핵심 역할

| 역할 | 의미 |
|---|---|
| **실행 순서 제어** | 임계 영역 내 순차 실행 보장 |
| **메모리 가시성 보장** | 모든 Thread가 동일한 최신 값을 보도록 강제 |
| **원자성 보장** | 연산이 중간에 끊기지 않도록 보장 |
| **Cache 동기화** | Core별 Cache 불일치 해소 |
| **Happens-Before 보장** | 작업 간 시간적 선후 관계 강제 |

**주요 동기화 기술:** `synchronized` / `volatile` / `Lock` / `CAS` / `AtomicOperation` / `Memory Barrier`

## 9. 전체 처리 흐름 요약

```text
[Step 1] 개발자 코드 작성
         counter.value++

[Step 2] Software Thread 실행
         Thread가 JVM Heap의 counter 객체 접근

[Step 3] OS Scheduling
         OS가 Thread를 물리 CPU Core에 배치

[Step 4] CPU 연산
         CPU가 Cache / RAM에서 값 읽기

[Step 5] 값 계산 및 저장
         ALU 연산 후 Cache에 반영

[Step 6] 메모리 계층 동기화
         Cache Flush → RAM 반영
         Memory Barrier로 순서 강제
```

## 핵심 요약

`counter.value++` 한 줄은 실제로 다음 전체 파이프라인을 의미합니다:

```text
Software Thread가 코드를 실행
  → OS가 CPU Core를 할당
  → CPU가 Cache / RAM에서 값을 읽고
  → ALU가 계산하고
  → Cache → RAM 순서로 값을 반영
  → 동기화 기술이 순서와 가시성을 보장
```

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*