## Task/Work Queue와 Worker Thread 구조

Task/Work Queue는 **실행 대기 중인 작업(Task)을 저장하고, Worker Thread가 이를 가져가 처리하는 Queue 구조**입니다.

| 개념 | 의미 |
|---|---|
| **Task** | 실행되어야 하는 작업 단위 |
| **Work Queue** | Task가 대기하는 Queue |
| **Worker Thread** | Queue에서 Task를 가져와 실행하는 Software Thread |

Java 기준으로 Task는 보통 `Runnable` / `Callable` 같은 실행 가능한 객체입니다.

## 기본 Thread Pool 구조

일반적인 Thread Pool 구조에서는 여러 Worker Thread가 하나의 Queue를 함께 바라봅니다.

```text
Producer
  ↓
Task / Work Queue
  ↓
Worker Thread 1
Worker Thread 2
Worker Thread 3
```

여러 Worker Thread가 공유 Queue에서 작업을 하나씩 가져가 실행합니다.

Queue는 공유 자원이므로, 여러 Worker Thread가 동시에 작업을 꺼내려 하면 내부적으로 **Lock / CAS / Atomic 연산** 등이 사용될 수 있습니다.

## Lock Contention이 발생할 수 있는 이유

Worker Thread 수가 많아지면 작업을 처리하는 시간보다 **Queue에서 Task를 가져오기 위한 경쟁**이 증가할 수 있습니다.

```text
Worker Thread 증가
→ Queue 접근 경쟁 증가
→ Lock Contention 증가
→ Context Switch / CPU Cache Miss 증가
→ Throughput 저하
```

> Worker Thread를 무작정 늘린다고 항상 성능이 좋아지는 것은 아닙니다.

## Netty / WebFlux는 왜 다른가?

Netty / WebFlux 같은 Event Loop 기반 Runtime은 일반적인 단일 공유 Queue 구조와 다르게 동작합니다.

Netty는 보통 **Event Loop Thread마다 자신의 전용 Task Queue**를 가집니다.

```text
Event Loop Thread 1 ─── Task Queue 1
Event Loop Thread 2 ─── Task Queue 2
Event Loop Thread 3 ─── Task Queue 3
```

이 구조는 여러 Thread가 하나의 Queue를 두고 경쟁하는 상황을 줄여 **Lock Contention을 낮추는 데 유리**합니다.

## Work-Stealing 구조

일부 Runtime은 Work-Stealing 구조를 사용합니다.

**대표 예:** Java `ForkJoinPool`

기본적으로 각 Worker Thread가 자신의 Queue를 가지고, 어떤 Thread가 일이 없으면 다른 Thread의 Queue에서 작업을 가져와 처리할 수 있습니다.

```text
Worker 1 ─── Queue 1  (작업 많음)
Worker 2 ─── Queue 2  (작업 없음)
                ↓
Worker 2가 Queue 1의 작업 일부를 가져와 처리
```

이 방식의 목표:
- 평소에는 Queue 경쟁 감소
- 부하 불균형 시 작업 분산

## Queue 구조 비교 (SRE 관점)

| 구조 | 특징 | 위험 |
|---|---|---|
| **Single Shared Queue** | 구조 단순 | Worker 간 Queue 경합 가능 |
| **Per-thread Queue** | Lock Contention 감소 | 특정 Thread Queue 쏠림 가능 |
| **Work-Stealing Queue** | 부하 분산 가능 | 구현 복잡도 증가 |

특히 Event Loop 기반 Runtime에서는 특정 Event Loop Queue에 무거운 작업이 몰리면:

```text
전체 CPU 사용률은 낮아 보여도
특정 요청의 Tail Latency가 증가
```

할 수 있습니다.

## 핵심 요약

- **Task**는 실행 단위 객체이고, **Worker Thread**는 이를 실행하는 Software Thread입니다.
- **Queue**는 Task를 대기시키는 공유 구조이며, Runtime에 따라 Single Queue / Per-thread Queue / Work-Stealing Queue로 구현될 수 있습니다.

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*