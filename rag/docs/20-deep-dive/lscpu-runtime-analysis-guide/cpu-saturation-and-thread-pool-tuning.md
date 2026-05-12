# CPU Saturation과 Thread Pool 설정

## 개요

CPU Saturation은 CPU 사용률이 높은 상태만을 의미하지 않습니다.  
CPU가 처리할 수 있는 실행 능력보다 많은 `task_struct`가 실행을 요구하면서, **Runnable Queue 대기 증가, Context Switch 증가, Scheduler Overhead 증가, Cache Miss 증가가 함께 발생하는 상태**입니다.

Thread Pool 설정은 이 흐름에서 핵심 변수입니다.  
Blocking 구조와 Non-blocking 구조는 Thread Pool을 과도하게 설정했을 때 문제가 발생하는 원인이 다릅니다.

---

## 1. Blocking 구조와 Non-blocking 구조의 차이

| 구분 | Blocking 구조 | Non-blocking 구조 |
|------|-------------|-----------------|
| 대표 모델 | Thread-per-request | Event-loop |
| 대표 기술 | Spring MVC, Tomcat, JDBC | WebFlux, Netty, R2DBC |
| Thread 사용 방식 | 요청 하나를 Thread 하나가 전담 | 소수의 EventLoop Thread가 다수 요청 처리 |
| I/O 대기 시 | Thread가 Blocked / Sleeping 상태 | Thread가 대기하지 않고 다른 이벤트 처리 |
| 주요 위험 | Thread Pool 고갈, `task_struct` 폭증 | CPU Throttling, EventLoop 지연 |

---

## 2. Blocking 구조에서 과도한 Thread Pool이 위험한 이유

Blocking 구조에서는 하나의 요청이 하나의 Thread를 전담 점유합니다.

```
Request
  → Worker Thread 할당
  → DB / External API 호출
  → I/O 완료까지 대기
  → Response 반환
  → Thread Pool 복귀
```

I/O가 느려지면 Thread는 응답이 올 때까지 대기 상태가 됩니다.  
Kernel 관점에서는 `task_struct`가 Wait Queue로 이동하지만, Java Thread Pool 관점에서는 해당 Thread가 아직 작업을 완료하지 않았으므로 Pool에 반환되지 않습니다.

### 2.1 Thread Pool 고갈

```
DB / API 지연
  → Worker Thread 장시간 점유
  → Thread Pool 가용 Thread 감소
  → 신규 요청 대기
  → Timeout 또는 응답 지연
```

이 상황에서 Thread Pool 크기를 무리하게 늘리면 일시적으로 신규 요청을 더 받을 수는 있지만, Kernel 관점에서는 더 많은 `task_struct`를 관리해야 합니다.

### 2.2 task_struct 증가

Java Thread가 증가하면 Linux Kernel 수준에서 그에 대응하는 OS Thread, 즉 `task_struct`도 함께 증가합니다.

```
Thread Pool 크기 증가
  → Java Thread 증가
  → OS Thread 증가
  → task_struct 증가
  → Scheduler 관리 대상 증가
```

---

## 3. Blocking 구조에서 발생하는 연쇄 문제

### 3.1 Wait Queue ↔ Runnable Queue 이동 증가

```
Running
  → Blocking I/O 호출
  → Wait Queue 이동
  → I/O 완료
  → Runnable Queue 복귀
  → Running
```

많은 Thread가 동시에 I/O를 기다렸다가 동시에 깨어나면 Runnable Queue가 급격히 증가합니다.

### 3.2 Thundering Herd

I/O 응답이 한꺼번에 완료되면 Wait Queue에 있던 다수의 `task_struct`가 동시에 Runnable 상태로 복귀합니다.

```
다수의 I/O 완료
  → 다수의 task_struct Wake-up
  → Runnable Queue 급증
  → CFS Scheduler 부하 증가
```

CPU Core 수보다 훨씬 많은 task가 동시에 CPU 실행을 요구하게 됩니다.

### 3.3 Context Switch 증가

Runnable task가 많아지면 CPU는 실행 대상을 자주 교체해야 합니다.

Context Switch 시 저장·복구되는 정보는 다음과 같습니다.

- Program Counter
- Stack Pointer
- General Registers
- CPU Flags
- Memory Context

이 작업은 Kernel Mode에서 수행되며 CPU Cycle을 소비합니다.

### 3.4 Cache Miss 증가

Thread가 자주 교체되면 CPU Cache Locality가 약해집니다.

```
task_struct A 실행 → A의 데이터가 Cache에 적재
  → Context Switch
  → task_struct B 실행 → B의 데이터가 Cache에 없음 → Cache Miss 발생
```

Cache Miss가 증가하면 CPU는 RAM 접근을 기다리게 되고 실제 연산 효율이 저하됩니다.

---

## 4. Scheduler Overhead 증가

Linux CFS Scheduler는 Runnable 상태의 `task_struct`를 관리합니다.  
Runnable task가 많아질수록 다음 비용이 증가합니다.

- Runnable Queue 관리
- Red-Black Tree 탐색 및 재정렬
- `vruntime` 계산
- Context Switch 수행 및 CPU Context 저장·복구

```
Runnable task_struct 증가
  → Scheduler 관리 비용 증가
  → System CPU Time 증가
  → 비즈니스 로직 실행 시간 감소
```

CPU 사용률은 높지만 실제 처리량은 감소할 수 있습니다.

---

## 5. Blocking 구조의 CPU Saturation 전체 흐름

```
Blocking I/O 증가
  → Thread Pool 점유 증가
  → Thread Pool 크기 확장
  → Java Thread 증가
  → task_struct 증가
  → Wait Queue / Runnable Queue 이동 증가
  → Context Switch 증가
  → Cache Miss 증가
  → Scheduler Overhead 증가
  → CPU Saturation
```

---

## 6. Non-blocking 구조에서 과도한 Thread Pool이 위험한 이유

Non-blocking 구조의 기본 전제는 소수의 EventLoop Thread로 많은 요청을 처리하는 것입니다.

```
적은 수의 EventLoop Thread
  → 다수의 Socket / FD 이벤트 처리
  → Context Switch 최소화
  → Runnable Queue 안정화
```

이 구조에서는 Thread 수가 많을 필요가 없습니다.  
EventLoop Thread를 과도하게 늘리면 Non-blocking 구조의 장점이 약해집니다.

### 6.1 Runnable Queue의 인위적 증가

```
Logical CPU = 8, Thread = 200
  → 8개만 Running 가능
  → 192개는 Runnable Queue에서 대기
  → Scheduler가 불필요하게 많은 task 관리
```

### 6.2 Involuntary Context Switch 증가

Non-blocking 구조에서는 하나의 EventLoop Thread가 CPU를 오래 점유하며 이벤트를 빠르게 처리하는 것이 유리합니다.  
하지만 Thread가 너무 많으면 Scheduler가 공정성을 위해 강제로 실행 대상을 교체합니다.

```
EventLoop Thread 실행 중
  → 다른 Runnable Thread 다수 존재
  → Scheduler Preemption 발생
  → Context Switch 발생
  → EventLoop 연속 실행성 저하
```

### 6.3 Cache Locality 저하

EventLoop Thread가 자주 교체되면 CPU Cache에 유지되던 데이터가 무효화됩니다.

```
EventLoop A 실행 → Cache Warm-up
  → Context Switch
  → EventLoop B 실행 → Cache Miss 증가
```

Non-blocking 구조의 높은 CPU 효율이 약해집니다.

---

## 7. Non-blocking 구조와 Kubernetes CPU Throttling

Non-blocking 구조는 적은 수의 Thread가 CPU를 밀도 높게 사용합니다.  
이 특성은 Kubernetes CPU Limit과 결합될 때 CPU Throttling을 유발할 수 있습니다.

```
EventLoop가 짧은 시간에 CPU 집중 사용
  → CFS Quota 빠르게 소진
  → Container Throttling
  → EventLoop 정지
  → 전체 요청 지연
```

EventLoop Thread가 멈추면 신규 요청뿐 아니라 기존 연결의 후속 처리도 함께 지연됩니다.

---

## 8. CPU Burst

CPU Burst는 Linux CFS Bandwidth Control에서 **순간적인 CPU 사용량 증가를 완화**하기 위한 기능입니다.  
이전 주기에서 사용하지 않은 CPU 시간을 일정 범위 내에서 축적하고, 피크 구간에 사용할 수 있게 합니다.

```
이전 주기에서 CPU Quota 일부 미사용
  → Burst 여유분 축적
  → 순간 피크 발생
  → Burst 사용
  → Throttling 완화
```

Non-blocking 서버처럼 짧은 시간에 CPU를 집중적으로 사용하는 구조에서 Tail Latency 안정화에 도움이 됩니다.

---

## 9. Blocking vs Non-blocking 과도한 Thread Pool 비교

| 구분 | Blocking 구조 | Non-blocking 구조 |
|------|-------------|-----------------|
| Thread를 늘리는 이유 | I/O 대기 중인 Thread 공백을 메우기 위해 | 일반적으로 늘릴 이유가 적음 |
| 주요 문제 | Thread Pool 고갈, `task_struct` 폭증 | Runnable Queue 증가, EventLoop 효율 저하 |
| 주요 Queue | Wait Queue ↔ Runnable Queue 이동 | Runnable Queue 내부 경쟁 |
| 주요 비용 | Context Switch, Memory, Stack, Scheduler Overhead | Scheduler Overhead, Cache Miss, CPU Throttling |
| 임계점 | Thread 수가 수백~수천 개로 증가할 때 | Core 수보다 과도하게 많아질 때 |
| 해결 방향 | Timeout, Bulkhead, Backpressure, Pool 제한 | Core 수 기반 Thread 설정, CPU Limit 조정 |

---

## 10. SRE 관점 주요 지표

| 지표 | 의미 |
|------|------|
| JVM Thread Count | Java Thread 증가 여부 |
| Native Thread Count | OS Thread / `task_struct` 증가 여부 |
| Runnable Thread 수 | CPU 경쟁 상태 |
| Waiting Thread 수 | I/O 또는 Lock 대기 상태 |
| Context Switches | 실행 전환 비용 |
| Run Queue Length | CPU 대기 task 수 |
| CPU `sy` | Kernel Scheduler 작업 비중 |
| CPU `us` | Application 로직 실행 비중 |
| CPU Throttling | cgroup CPU Quota 제한 영향 |
| Cache Miss Rate | Context Switch에 따른 Cache 효율 저하 |
| P99 / P999 Latency | 사용자 관점 지연 |

---

## 11. 운영 대응 방향

### 11.1 Blocking 구조

Thread Pool을 무작정 늘리는 것은 CPU Saturation을 악화시킵니다.  
아래 순서로 대응합니다.

1. Timeout 설정
2. Circuit Breaker 적용
3. Bulkhead 적용
4. DB Connection Pool 제한
5. Bounded Queue 사용
6. Backpressure 적용
7. Thread Pool 크기 제한
8. 필요 시 Non-blocking 전환 검토

### 11.2 Non-blocking 구조

EventLoop Thread 수를 Core 수에 맞추는 것이 핵심입니다.  
아래 순서로 대응합니다.

1. EventLoop Thread 수를 Logical CPU 기준으로 설정
2. Blocking 작업을 EventLoop에서 제거
3. JDBC 등 Blocking 호출은 별도 Worker Pool로 격리
4. CPU Limit을 평균이 아닌 피크 기준으로 설정
5. CPU Throttling 지표 확인
6. CPU Burst 또는 Limit 제거 전략 검토
7. Backpressure 적용

---

## 12. 핵심 정리

### Blocking 구조의 CPU Saturation 흐름

```
Blocking I/O
  → Thread Pool 점유
  → Thread 증가
  → task_struct 증가
  → Context Switch 증가
  → Scheduler Overhead 증가
  → CPU Saturation
```

### Non-blocking 구조의 Thread 과다 설정 흐름

```
Non-blocking + Thread 과다 설정
  → Runnable Queue 증가
  → Preemption 증가
  → Cache Miss 증가
  → Scheduler Overhead 증가
  → CPU 효율 저하
```

**Blocking 구조**: Thread 수 증가를 제한하고, I/O 병목을 Timeout·Bulkhead·Backpressure로 제어합니다.  
**Non-blocking 구조**: Thread 수를 Logical CPU Core 수에 가깝게 유지합니다.

CPU Saturation을 방지하려면 **Thread Pool 크기, I/O 방식, Runnable Queue 길이, Context Switch, CPU Throttling**을 함께 관리해야 합니다.

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*