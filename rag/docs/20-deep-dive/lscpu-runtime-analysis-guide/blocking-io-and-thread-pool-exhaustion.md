# Blocking I/O와 Thread Pool 고갈

## 1. 개요

Blocking I/O는 I/O 작업 완료를 기다리는 동안 Thread가 다음 코드를 실행하지 못하는 처리 방식입니다.

Linux Kernel 수준에서는 해당 Thread의 `task_struct`가 Runnable Queue에서 Wait Queue로 이동합니다. 그러나 Java Thread Pool 관점에서는 해당 Thread가 아직 작업을 완료하지 않았으므로 Pool에 반환되지 않습니다.

이 구조가 누적되면 CPU 여유가 있어도 새로운 요청을 처리할 Thread가 부족한 **Thread Pool 고갈** 상태가 발생할 수 있습니다.

---

## 2. Blocking I/O 발생 시 상태 전환

```
Java Thread 실행
  ↓ DB Query / 외부 API / 파일 I/O 요청
  ↓ Thread → Sleeping 상태 전환
  ↓ task_struct → Runnable Queue 제거 → Wait Queue 등록
  ↓ I/O 완료 이벤트 수신
  ↓ task_struct → Runnable Queue 복귀
  ↓ Thread 실행 재개
```

Blocked 상태의 `task_struct`는 삭제되지 않으며 아래 정보를 유지한 채 대기합니다.

| 유지 항목 | 설명 |
|----------|------|
| Program Counter | 재개 시 실행 위치 |
| Stack Pointer | Stack 상태 |
| Register Context | CPU 실행 상태 |
| Java Stack | 호출 스택 정보 |
| Scheduling 정보 | CFS 관련 메타 정보 |
| I/O 대기 정보 | 기다리는 이벤트 정보 |

---

## 3. CPU 관점과 Thread Pool 관점의 차이

| 관점 | 해석 |
|------|------|
| CPU / Scheduler | 해당 `task_struct`는 CPU를 사용하지 않으므로 다른 Runnable task를 실행할 수 있음 |
| Java Thread Pool | 해당 Thread는 요청 처리를 완료하지 않았으므로 Pool에 반환되지 않음 |

CPU는 비어 있더라도 Java Thread Pool은 고갈될 수 있습니다.

---

## 4. Thread Pool 고갈 구조

```
Thread Pool Max = 200

200개 요청 동시 유입
  ↓ 200개 Thread가 I/O 대기 (Sleeping)
  ↓ Thread Pool에 잔여 Thread 없음
  ↓ 신규 요청 처리 불가 → 대기 또는 Timeout
```

CPU 사용률이 낮아도 서버가 응답하지 못하는 상태가 됩니다.

### 일부 느린 I/O가 전체 요청에 미치는 영향

일부 느린 I/O 요청이 Thread를 장시간 점유하면 다른 요청의 Thread 할당도 지연됩니다.

```
느린 DB 요청 증가
  ↓ Worker Thread 장시간 점유
  ↓ 사용 가능한 Thread 감소
  ↓ 가벼운 요청도 Thread 할당 대기
  ↓ 전체 응답 지연
```

---

## 5. Thread Pool 크기와 발생 문제

### Thread Pool 크기를 늘릴 때의 위험

Blocking I/O 병목이 해결되지 않은 상태에서 Thread 수만 늘리면 아래 문제가 연쇄적으로 발생합니다.

```
Thread 수 증가  →  task_struct 증가
  ↓ Context Switch 증가
  ↓ Memory 사용량 증가  (Thread당 Stack: 약 1MB)
  ↓ CPU Cache Miss 증가
  ↓ Scheduler Overhead 증가
```

I/O 병목이 동시에 해소되어 다수의 Thread가 Runnable 상태로 복귀하면 CPU Saturation이 발생할 수 있습니다.

### Thread Pool 제한이 없는 경우

`Executors.newCachedThreadPool()` 또는 제한 없는 커스텀 Executor 사용 시 아래 문제가 발생할 수 있습니다.

```
요청 증가  →  Thread 계속 생성  →  task_struct 계속 생성
  ↓ Native Memory / Stack Memory 증가
  ↓ Context Switch 급증
  ↓ unable to create new native thread 가능
```

### CPU Core 대비 Thread 수가 과도한 경우

```
CPU Core = 8 / Thread Pool Max = 500

I/O 완료 후 다수 Thread Runnable 복귀
  ↓ 500개 task_struct가 CPU 경쟁
  ↓ Context Switch 증가  →  Scheduler Overhead 증가
```

---

## 6. Blocking I/O와 CPU Saturation 연쇄 구조

```
Blocking I/O 증가
  ↓ Thread Pool 점유 증가
  ↓ Thread 수 증가 또는 Pool 고갈
  ↓ I/O 완료 후 Runnable task_struct 급증
  ↓ Context Switch 증가  →  Cache Miss 증가
  ↓ Scheduler Overhead 증가
  ↓ CPU Saturation
```

---

## 7. Blocking vs Non-blocking I/O 비교

| 항목 | Blocking I/O | Non-blocking I/O |
|------|-------------|-----------------|
| I/O 대기 중 Thread | 점유됨 | 다른 작업 수행 가능 |
| Thread Pool 압박 | 큼 | 상대적으로 작음 |
| task_struct 수 | 증가하기 쉬움 | 적게 유지 가능 |
| Context Switch | 증가 가능 | 상대적으로 적음 |
| CPU Cache 효율 | 낮아질 수 있음 | 유지하기 쉬움 |
| 대표 구조 | Spring MVC + JDBC | WebFlux + Netty + 비동기 Client |

### Non-blocking I/O 처리 흐름

```
I/O 요청
  ↓ Thread 반환 또는 다른 이벤트 처리 계속 수행
  ↓ I/O 완료 이벤트 발생
  ↓ Callback / Event Handler 실행
```

---

## 8. 대표 장애 패턴

### CPU 사용률이 낮지만 응답이 느린 경우

```
DB / API 지연  →  Thread Pool 전체가 Blocking 대기
  ↓ 신규 요청에 할당할 Thread 없음
  ↓ 요청 대기 또는 Timeout
  ↓ CPU 사용률은 낮음
```

CPU 증설보다 Downstream I/O 병목, Connection Pool, Timeout 설정을 먼저 확인해야 합니다.

### CPU 사용률이 높지만 처리량이 낮은 경우

```
Thread 수 과다  →  Context Switch 증가
  ↓ Cache Miss 증가  →  Scheduler Overhead 증가
  ↓ CPU Saturation  →  Throughput 감소
```

Thread Pool 크기, Blocking 호출 비율, Runnable Thread 수, CPU Throttling을 함께 확인해야 합니다.

---

## 9. SRE 관점 진단 지표

| 지표 | 의미 |
|------|------|
| Thread Pool Active Count | 현재 사용 중인 Thread 수 |
| Thread Pool Queue Size | 처리 대기 중인 요청 수 |
| Runnable Thread 수 | CPU 경쟁 중인 task 수 |
| Waiting / Timed Waiting Thread 수 | I/O 또는 Lock 대기 Thread 수 |
| DB Connection Pool 사용률 | Downstream 병목 여부 |
| Context Switch Rate | Thread 전환 비용 |
| CPU Usage | CPU 포화 여부 |
| System CPU Time | Kernel 스케줄링 작업 비율 |
| Load Average | Runnable 및 D 상태 task 누적 |
| P99 Latency | 사용자 관점 응답 지연 |

---

## 10. 운영 대응 방향

| 대응 방향 | 설명 |
|----------|------|
| Timeout 설정 | 느린 I/O가 Thread를 장시간 점유하지 않도록 제한 |
| Circuit Breaker | 장애 Downstream 호출을 빠르게 차단 |
| Bulkhead | DB / API별 Thread Pool 또는 Semaphore 분리 |
| Bounded Queue | 무제한 대기열 증가 방지 |
| Thread Pool 크기 조정 | CPU Core 수와 I/O 특성에 맞게 조정 |
| Non-blocking Client 사용 | I/O 대기 중 Thread 점유 제거 |
| Backpressure 적용 | 시스템 처리량을 초과하는 유입 제어 |

---

## 11. 최종 정리

```
Blocking I/O
  ↓ Thread Pool 점유  →  사용 가능한 Thread 감소
  ↓ Pool 고갈 또는 Thread 증가
  ↓ Context Switch / Scheduler Overhead 증가
  ↓ CPU Saturation 가능
```

| 항목 | 내용 |
|------|------|
| Thread Pool 고갈 원인 | I/O 대기 중 Thread가 Pool에 반환되지 않음 |
| CPU 관점과의 차이 | CPU는 여유가 있어도 Thread Pool은 고갈 가능 |
| Thread 수 증가의 위험 | Context Switch, Cache Miss, Scheduler Overhead 연쇄 증가 |
| 핵심 모니터링 대상 | Thread Pool, DB Connection Pool, Timeout, Context Switch, Latency |
| 근본적 해결 방향 | Non-blocking I/O 적용, Bulkhead / Circuit Breaker, 적정 Thread Pool 크기 유지 |

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*