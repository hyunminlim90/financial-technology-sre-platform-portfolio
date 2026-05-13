## CPU 동기화 기술(Thread Synchronization)이란?

**여러 CPU Core 또는 Software Thread가 동일한 [공유 메모리(shared memory)](../20-deep-dive/shared-memory-and-synchronization.md)에 접근할 때, 데이터 무결성(Data Integrity)을 유지하기 위해 [실행 순서와 접근을 제어](../20-deep-dive/execution-order-and-synchronization.md)하는 메커니즘**입니다.

즉:

```text
동시에 같은 데이터를 수정하여
상태가 꼬이지 않도록 제어하는 기술
```

</br>

## 왜 필요한가?

현대 시스템은 다음 환경으로 동작합니다:

- Multi-core CPU
- Multi-thread Runtime
- Concurrent Processing

예를 들어 CPU Core A와 Core B가 동시에 **동일 메모리 주소(shared memory)** 를 수정하려 하면:

- Race Condition
- 데이터 손상
- 상태 불일치

가 발생할 수 있습니다.

따라서 **누가 먼저 접근하고, 누가 기다릴지**를 제어해야 합니다.

## CPU 동기화 기술의 대표 예시

| 기술 | 설명 |
|---|---|
| **Mutex** | Lock 획득 실패 시 Thread를 Sleep(Blocked) 상태로 전환 |
| **Spinlock** | Lock 획득 전까지 CPU를 계속 사용하며 반복 확인 (Busy Waiting) |
| **CAS** (Compare-And-Swap) | 특정 값 비교 후 변경을 CPU가 원자적으로 수행 |
| **Atomic Operation** | 중간에 끊기지 않는 단일 연산 보장 |
| **Memory Barrier** | CPU 명령어 재정렬(Reordering) 방지 및 메모리 가시성 보장 |

## Atomic Operation (원자적 연산)

Atomic Operation은 **중간에 끊기지 않는 하나의 연산 단위**입니다.

예: "현재 값이 10이면 11로 변경" 작업을 읽기 → 계산 → 쓰기로 나누지 않고, **CPU가 하나의 연산처럼 처리**합니다.

이를 통해 동시 수정 충돌(Race Condition)을 줄일 수 있습니다.

## Memory Barrier (메모리 배리어)

현대 CPU는 성능 최적화를 위해 **명령어 순서 재배치(Reordering)** 를 수행할 수 있습니다.

하지만 동시성 환경에서는 **다른 Core가 최신 메모리 상태를 보지 못하는 문제**가 발생할 수 있습니다.

Memory Barrier는 다음을 강제하는 메커니즘입니다:

```text
메모리 기록 순서 보장
+
다른 CPU Core에 메모리 변경 내용 전파
```

## 왜 "CPU 동기화"라고 부르나?

동기화 문제는 단순히 Java 문법 수준의 문제가 아니라, 실제로는 다음과 연결되기 때문입니다:

- CPU Cache
- Memory Ordering
- Atomic Instruction
- Core 간 메모리 가시성

즉 **Software Thread 동기화**는 결국 **CPU / Memory 하드웨어 동기화** 위에서 동작합니다.

## Business Domain과 연결되는 이유

동기화 기술은 단순 성능 최적화 도구가 아닙니다. 최종 목적은 **비즈니스 데이터 무결성 보호**입니다.

예:
- 중복 결제 방지
- 이중 승인 방지
- 잔액 불일치 방지
- Session 상태 보호

| 계층 | 역할 |
|---|---|
| **CPU 동기화 기술** | 실행 순서와 메모리 접근 제어 |
| **Business Domain** | 실제 서비스 데이터 무결성 보호 |

## 실무 관점 핵심

Lock / CAS / Atomic 연산 등은 CPU 자원을 효율적으로 쓰기 위한 기술인 동시에, 결국 **서비스 신뢰성(Reliability)과 데이터 정합성(Consistency)** 을 지키기 위한 핵심 메커니즘입니다.

특히 FinTech / Payment 시스템에서는 다음이 최우선 원칙입니다:

```text
No Duplicate Payment
No Double Approval
No Inconsistent State
```

## 핵심 요약

> CPU 동기화 기술(Thread Synchronization)은  
> 여러 CPU Core와 Software Thread가 공유 데이터를 동시에 처리하는 환경에서,  
> **데이터 무결성과 서비스 신뢰성을 유지하기 위해**  
> 실행 순서와 메모리 접근을 제어하는 기술입니다.

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*