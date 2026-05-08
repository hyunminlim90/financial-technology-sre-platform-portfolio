## Lock Contention(락 경합) 이란?

```
여러 Software Thread가
동일한 공유 자원(shared resource)을 보호하는
Lock(Mutex/Spinlock)을 경쟁하면서
대기하는 상태

Mutex: Lock 대기 중 Thread를 Sleep(Blocked) 상태로 전환하여 CPU 사용을 줄이는 방식
Spinlock: Lock 획득 전까지 반복 확인(Busy Waiting)하며 CPU를 계속 사용하는 방식
````

를 의미합니다.

## 왜 Lock이 필요한가?

Application / Runtime / OS 계층에서는 데이터 무결성(Data Integrity)을 위해:

```text
동일한 공유 자원(shared resource)
```

을 동시에 여러 Software Thread가 수정하지 못하도록
Lock을 사용합니다.

예를 들어 다음과 같은 자원들이 존재할 수 있습니다.

| 계층 (Layer) | 공유 자원 예시 | 설명 |
|---|---|---|
| Business Domain | [Session / Payment 상태](../20-deep-dive/business-state-consistency.md) | 동일 사용자·결제 상태 데이터를 여러 요청이 동시에 변경할 수 있는 구조 |
| Application | Heap 영역 내 객체 | Singleton Bean, 공유 객체 등 여러 Software Thread가 상태를 함께 사용하는 구조 |
| Application | [Task / Work Queue](../20-deep-dive/work-queue-and-worker-thread.md) | 실행 대기 중인 Task를 Queue에 저장하고, 여러 Worker Thread가 이를 공유하며 가져가 처리하는 구조 |
| Application | Local Cache (Map) | ConcurrentHashMap 등을 이용해 애플리케이션 수준에서 데이터를 공유·캐싱하는 구조 |
| Runtime / Reactive | Event Loop Queue | Netty/WebFlux 환경에서 Event Loop가 비동기 이벤트·Callback을 처리하는 Queue 구조 |
| Runtime / I/O | Network Buffer | 네트워크 패킷 데이터를 읽고 쓰기 위해 Runtime이 사용하는 메모리 Buffer |
| Middleware | Kafka Partition 상태 | Consumer Offset, Commit 상태, Fetch Buffer 등을 여러 Thread가 함께 관리하는 구조 |
| Infrastructure | DB Connection Pool | 여러 요청 Thread가 제한된 DB Connection 자원을 공유하는 구조 |
| OS / Kernel | File Handle / Buffer | 운영체제가 관리하는 파일 접근 권한 및 파일 I/O Buffer 구조 |
| Logging | Log Buffer / Async Appender | 여러 Thread의 로그 데이터를 비동기적으로 수집·기록하는 Buffer 구조 |

즉:

```text
공유 자원을 동시에 변경하면
Race Condition(데이터 충돌)
```

이 발생할 수 있기 때문에:

```text
Lock으로 접근 순서를 제어
```

하는 것입니다.

---

## Lock Contention은 언제 발생하나?

정상 상태에서는:

```text
Thread가 아주 짧게 Lock 사용
→ 작업 완료
→ 즉시 반환
```

됩니다.

하지만:

* Lock을 오래 점유하거나
* 너무 많은 Thread가 동시에 접근하거나
* 느린 I/O 작업 중에도 Lock을 유지하거나
* 과도한 synchronized / Mutex 사용

등이 발생하면:

```text
다른 Thread들이 Lock 획득 대기
```

상태가 됩니다.

이것이:

```text
Lock Contention
```

입니다.

---

## Lock Contention이 발생하면?

### 1. Thread Waiting 증가

```text
Runnable / Blocked Thread 증가
```

합니다.

즉:

```text
Thread가 실제 작업 대신
Lock 해제를 기다림
```

상태가 됩니다.

---

### 2. Context Switch 증가

Linux Scheduler(CFS)는:

```text
실행 가능한 다른 Thread 탐색
```

을 반복하게 됩니다.

이 과정에서:

```text
Context Switch 증가
Scheduler Overhead 증가
CPU Cache Miss 증가
```

등이 발생할 수 있습니다.

---

### 3. Throughput 감소

CPU 사용률은 높게 보일 수 있지만:

```text
실제 비즈니스 처리량(Throughput)
```

은 감소할 수 있습니다.

즉:

```text
CPU가 실제 연산보다
대기/전환 비용에 더 많은 시간 사용
```

할 수 있습니다.

---

### 4. Latency 증가

요청 처리 지연으로 인해:

```text
Request Queue 증가
Timeout 증가
Retry 증가
Latency Spike
```

등이 발생할 수 있습니다.

---

## 실무 예시

| 시스템      | 예시                                  |
| -------- | ----------------------------------- |
| Database | 동일 Row 업데이트 경쟁(Row Lock Contention) |
| JVM      | synchronized / ReentrantLock 과도 사용  |
| Kafka    | Partition 상태/Buffer 접근 경쟁           |
| Logging  | 다수 Thread의 동시 로그 기록                 |
| Cache    | 공유 Cache(Map) 업데이트 경쟁               |

---

## WebFlux / Event Loop 환경에서 왜 위험한가?

Event Loop 기반 Runtime은:

```text
적은 수의 Software Thread
```

로 동작합니다.

따라서:

```text
Lock 대기 시간 증가
→ Event Loop Stall
→ 전체 Runtime Latency 증가
```

로 이어질 수 있습니다.

특히:

* synchronized Block
* Blocking I/O inside Lock
* Shared Mutable State
* Global Lock 구조

등은 주의가 필요합니다.

---

## SRE 관점 핵심

Lock Contention은:

```text
CPU 성능 부족 문제가 아니라,

공유 자원 접근 경쟁으로 인해
병렬 처리 효율이 감소하는 문제
```

입니다.

즉:

```text
Scale-out을 해도
핵심 Lock 지점이 병목이면
전체 처리량은 제한될 수 있습니다.
```

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*