# Worker Thread와 Thread Pool 처리 흐름

## 1. 개요

### Worker Thread

Worker Thread는 Thread Pool 내부에서 Task Queue에 들어온 작업을 가져와 실행하는 Java Thread입니다. 작업 하나를 처리한 뒤 종료되지 않고, 다시 다음 작업을 기다립니다.

```
Worker Thread = Thread Pool 내부에서 재사용되는 작업 실행 Thread
```

### Thread Pool

Thread Pool은 일정 개수의 Thread를 미리 생성하거나 제한된 범위 안에서 생성하여 재사용하는 실행 구조입니다.

| 목적 | 설명 |
|------|------|
| Thread 재사용 | 생성/삭제 비용 감소 |
| 동시 실행 수 제한 | 시스템 과부하 방지 |
| 작업 대기열 제공 | 처리량 초과 시 작업 보관 |
| 운영 안정성 향상 | Thread 폭증 방지 |

> "Thread Pool"과 "Worker Thread Pool"은 대부분 같은 의미로 사용됩니다. Worker Thread Pool은 실제 비즈니스 작업 처리 역할을 강조한 표현입니다.

---

## 2. 내부 구조와 처리 흐름

### Thread Pool 내부 구조

```
Thread Pool
├── Task Queue          ← 실행 대기 작업 저장 (JVM Heap)
├── Worker Thread 1
├── Worker Thread 2
├── Worker Thread 3
└── Worker Thread N
```

### 기본 처리 흐름

```
작업 제출 → Task Queue 적재 → Worker Thread가 작업 가져옴 → 작업 실행 → 작업 완료 → Worker Thread 재대기
```

### Worker Thread 라이프사이클

| 단계 | 상태 | 설명 |
|------|------|------|
| Waiting | WAITING | Task Queue 대기, CPU 미사용 |
| Assignment | - | Task Queue에 작업 입력 시 깨어남 |
| Execution | RUNNABLE | Business Logic 실행 (DB, API, 계산 등) |
| Return | WAITING | 작업 완료 후 종료하지 않고 재대기 |

---

## 3. Worker Thread의 계층적 실체

Worker Thread도 Java Thread이며, 실행 중에는 OS Kernel Thread와 1:1 매핑됩니다.

```
Worker Thread → Java Thread → OS Kernel Thread → Kernel Scheduler → Logical CPU → Physical Core
                                                                                        ↓
                                                                          ALU / LSU / Register / Cache
```

---

## 4. Task Queue 동작

Task Queue는 Producer/Consumer 구조로 동작합니다.

```
Producer Thread → Task Queue → Worker Thread (Consumer)
```

**Producer가 될 수 있는 Thread:**

| Producer | 예시 |
|----------|------|
| Main Thread | 초기 작업 제출 |
| Netty EventLoop | 요청 처리 작업 제출 |
| Kafka Consumer | 메시지 처리 작업 제출 |
| Scheduler Thread | 주기 작업 제출 |

### Queue 크기에 따른 문제

**Queue가 너무 작은 경우:**

```
Worker 모두 사용 중 + Queue 가득 참 → 새 작업 거절
→ RejectedExecutionException / HTTP 503 가능 / Retry 증가
```

**Queue가 너무 큰 경우 (Unbounded Queue):**

```
작업 유입 속도 > 처리 속도 → Queue 계속 증가
→ Latency 증가 / Heap 사용량 증가 / GC 부하 증가 / 장애 감지 지연
```

Bounded Queue는 시스템이 처리 가능한 한계를 명확하게 드러냅니다.

---

## 5. Worker Thread가 처리하기 적합한 작업

| 작업 유형 | 설명 |
|-----------|------|
| Blocking DB 호출 | JDBC 기반 DB 접근 |
| 외부 API 호출 | Blocking HTTP Client 사용 시 |
| Disk I/O | 파일 읽기/쓰기 |
| CPU 연산 | 복잡한 계산, 암호화, 압축 |
| 비즈니스 로직 | 주문, 결제, 정산 처리 등 |

### CPU Bound vs I/O Bound Pool 분리

작업 성격에 따라 Thread Pool을 분리하는 것이 좋습니다.

| Pool 유형 | 적합한 작업 | 권장 Pool 크기 |
|-----------|-------------|----------------|
| CPU Bound Pool | CPU 연산 중심 | Physical Core / Logical CPU 수 기준 |
| I/O Bound Pool | 외부 대기 시간이 많은 작업 | CPU Bound Pool보다 크게 설정 가능 |

> I/O Bound Pool도 무제한 증가는 Context Switching과 메모리 사용량 증가를 유발합니다.

---

## 6. Blocking Offloading

Worker Thread가 중요한 이유 중 하나는 Blocking 또는 무거운 작업을 EventLoop 같은 민감한 Thread에서 분리하기 위해서입니다.

**문제 구조 (분리 없음):**

```
Netty EventLoop → Blocking DB 호출 → EventLoop 정지 → 네트워크 이벤트 처리 지연
```

**분리 구조 (Worker Pool 사용):**

```
Netty EventLoop → Task Queue에 작업 제출 → Worker Thread가 DB 호출 수행
                                                      Netty EventLoop는 다음 I/O 이벤트 처리
```

---

## 7. 다른 Software Thread와의 관계

| Thread 종류 | Worker Thread와의 관계 |
|-------------|------------------------|
| Main Thread | Thread Pool 생성 |
| Netty EventLoop | Blocking/Heavy 작업을 Worker에게 위임 |
| Kafka Consumer | 무거운 메시지 처리를 Worker에게 위임 가능 |
| Scheduler Thread | 실제 작업을 Worker에게 제출 가능 |
| GC Thread | Worker가 생성한 객체를 GC 대상에 포함 |

### Kafka Consumer와 Worker Pool 연계 시 주의사항

```
Kafka Consumer Thread → Poll → Task Queue → Worker Pool → Message Processing
```

| 항목 | 설명 |
|------|------|
| Offset Commit | 처리 완료 후 Commit 필요 |
| 순서 보장 | Partition 단위 순서 고려 |
| Backpressure | Queue 증가 시 Poll 속도 제어 |
| 중복 처리 | 재처리 대비 멱등성 필요 |

### Worker 작업 증가와 GC 부하

```
Worker 작업 증가 → 객체 생성 증가 → Heap 사용량 증가 → GC 빈도 증가
```

### Worker Pool과 Kubernetes CPU Limit

```
Worker Thread 100개 + CPU Limit 1 Core → RUNNABLE 경쟁 증가 → CFS Throttling 가능 → Latency 증가
```

---

## 8. Thread Pool 포화

```
All Worker Threads Busy + Queue Increasing = Thread Pool Saturation
```

### 포화 원인

| 원인 | 설명 |
|------|------|
| Downstream 지연 | DB, API, 파일 시스템 지연 |
| Worker 수 부족 | 처리량 부족 |
| CPU Limit 부족 | 실행 시간 부족 |
| Lock 경합 | BLOCKED Thread 증가 |
| GC Pause | Worker 실행 중단 |
| Queue 과다 | 오래된 작업 누적 |

---

## 9. Rejection Policy

Thread Pool이 더 이상 작업을 받을 수 없을 때의 처리 정책입니다.

| 정책 | 설명 |
|------|------|
| AbortPolicy | 예외 발생 (기본값) |
| CallerRunsPolicy | 작업을 제출한 Thread가 직접 실행 |
| DiscardPolicy | 작업 폐기 (예외 없음) |
| DiscardOldestPolicy | 가장 오래된 작업 폐기 후 재시도 |

### CallerRunsPolicy 주의점

Netty EventLoop에서 Worker Pool에 작업을 제출했을 때 CallerRunsPolicy가 발동하면 EventLoop가 Blocking 작업을 직접 수행하게 됩니다.

```
Worker Pool Full → CallerRunsPolicy → Netty EventLoop가 작업 직접 실행 → EventLoop 지연 가능
```

EventLoop와 함께 사용할 때는 이 정책을 피하거나 별도 처리가 필요합니다.

---

## 10. 구현 예시

### 기본 구현

```java
ExecutorService workerPool = Executors.newFixedThreadPool(10);

workerPool.submit(() -> {
    processBusinessLogic();
});
```

### 명시적 ThreadPoolExecutor (실무 권장)

```java
ThreadPoolExecutor workerPool = new ThreadPoolExecutor(
    10,                              // corePoolSize
    20,                              // maximumPoolSize
    60L, TimeUnit.SECONDS,           // keepAliveTime
    new ArrayBlockingQueue<>(1000),  // bounded queue
    new ThreadPoolExecutor.AbortPolicy()
);
```

Queue 크기와 Rejection Policy를 명확히 지정하는 것이 운영 안정성에 중요합니다.

---

## 11. 운영(SRE) 관점

### 주요 모니터링 지표

| 지표 | 의미 |
|------|------|
| Pool Size | 현재 Worker Thread 수 |
| Active Count | 실행 중인 Worker 수 |
| Queue Size | 대기 중인 작업 수 |
| Completed Task Count | 완료된 작업 수 |
| Rejected Task Count | 거절된 작업 수 |
| Task Execution Time | 작업 실행 시간 |
| Queue Waiting Time | Queue 대기 시간 |
| Thread State | RUNNABLE / BLOCKED / WAITING |
| CPU Throttling | 컨테이너 CPU 제한 여부 |
| GC Pause | Heap 압박 여부 |

### 주요 장애 패턴

| 증상 | 원인 | 판단 방향 |
|------|------|-----------|
| Queue Size 증가 | 작업 유입량 > 처리량 | Worker 수 부족 또는 Downstream 지연 |
| Active Count = Max Pool Size + Queue 증가 | Thread Pool 포화 | 포화 원인 확인 (DB, API, Lock, GC) |
| CPU 낮음 + Queue 증가 | Worker가 CPU를 사용하지 못함 | Blocking I/O 대기 / DB / API / Lock 대기 의심 |
| CPU 높음 + Queue 증가 | CPU Bound 작업 집중 또는 CPU Limit 부족 | Core 부족 / CFS Throttling / 계산 병목 의심 |

### Thread Dump 분석 포인트

| Worker 상태 | 해석 |
|-------------|------|
| RUNNABLE | CPU 실행 중 또는 대기 |
| BLOCKED | Lock 대기 |
| WAITING | Queue 대기 또는 조건 대기 |
| TIMED_WAITING | Timeout 기반 대기 |
| Deadlock | 상호 Lock 대기 |

---

## 12. 전체 처리 구조

```
Request / Message / Scheduled Event
              ↓
       Producer Thread
              ↓
          Task Queue
              ↓
      Worker Thread Pool
              ↓
       Business Logic
              ↓
  Result / Commit / Response
```

---

## 13. 핵심 정리

| 구성 요소 | 역할 |
|-----------|------|
| Worker Thread | 작업 실행 Thread |
| Thread Pool | Worker Thread 재사용 구조 |
| Task Queue | 실행 대기 작업 저장 |
| Producer Thread | 작업을 Queue에 넣는 Thread |
| Rejection Policy | 포화 시 동작 |
| Blocking Offloading | 민감한 Thread에서 무거운 작업 분리 |
| Active Count | 현재 실행 중인 Worker 수 |
| Queue Size | 대기량 및 지연 지표 |

### 결론

```
Worker Thread = Java Thread + Thread Pool 관리 + Task Queue 소비 + Business Logic 실행
```

Worker Thread Pool은 Netty EventLoop, Kafka Consumer, Scheduler Thread 등에서 발생한 무거운 작업을 분리하여 처리합니다.

```
적절한 Pool Size + Bounded Queue + 명확한 Rejection Policy + CPU / I/O 작업 분리
= 안정적인 Thread Pool 처리 구조
```

운영 관점에서는 Worker Thread 수뿐 아니라 **Queue 크기, Active Count, Rejected Count, CPU Throttling, GC Pause**를 함께 관찰해야 합니다.

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*