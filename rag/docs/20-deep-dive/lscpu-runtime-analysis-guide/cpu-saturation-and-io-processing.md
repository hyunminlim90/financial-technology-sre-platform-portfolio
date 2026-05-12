# CPU Saturation과 I/O 처리 구조

## 1. 개요

CPU Saturation은 단순히 CPU 사용률이 높은 상태만을 의미하지 않는다. CPU가 처리할 수 있는 실행 능력보다 더 많은 `task_struct`가 CPU 실행을 요구하면서, Runnable Queue 대기, Context Switch 증가, Scheduler Overhead 증가, Cache Miss 증가가 함께 발생하는 상태를 의미한다.

| 구분 | 의미 |
|------|------|
| CPU Utilization | CPU가 특정 시간 동안 얼마나 사용되었는지 나타내는 비율 |
| CPU Saturation | CPU 실행 능력보다 더 많은 작업이 대기하면서 지연이 발생하는 상태 |

CPU 사용률이 100%에 도달하지 않아도 Runnable Queue가 지속적으로 증가하거나 CPU Pressure가 높다면 Saturation 상태로 볼 수 있다.

---

## 2. CPU Saturation 핵심 연쇄 구조

```
Thread 증가  →  task_struct 증가
  ↓ Runnable Queue 증가
  ↓ Context Switch 증가  →  Cache Miss 증가
  ↓ Scheduler Overhead 증가
  ↓ CPU Saturation  →  Throughput 감소
```

CPU가 바쁘게 동작하지만 실제 비즈니스 로직 처리량은 오히려 감소하는 상태다.

---

## 3. Runnable Queue vs Wait Queue

| 구분 | Runnable Queue | Wait Queue |
|------|---------------|------------|
| `task_struct` 상태 | `TASK_RUNNING` | `TASK_INTERRUPTIBLE` / `TASK_UNINTERRUPTIBLE` |
| 의미 | CPU 실행 준비 완료 | I/O, Lock, Timer 등 이벤트 대기 |
| 병목 원인 | CPU Saturation | DB, Disk, Network, Lock 등 외부 자원 |
| Scheduler 대상 | 예 | 아니오 (이벤트 완료 후 Runnable 복귀) |

---

## 4. Load Average와 task_struct

Load Average에는 아래 상태의 `task_struct`가 포함된다.

| 포함 상태 | 의미 |
|----------|------|
| `TASK_RUNNING` | Running 또는 Runnable 상태 |
| `TASK_UNINTERRUPTIBLE` | Disk I/O 등 D 상태 |

Load Average 증가는 CPU 실행 대기 task 증가 또는 Uninterruptible I/O 대기 task 증가를 의미할 수 있다.

---

## 5. Thread-per-request와 Blocking I/O 문제

### Thread-per-request 구조

요청 수가 증가하면 Java Thread와 `task_struct` 수도 함께 증가한다.

```
1,000 Requests  →  1,000 Java Threads  →  1,000 task_struct
  ↓ Scheduler가 관리해야 하는 실행 단위 급증
```

### Blocking I/O와 Thread Pool 점유

```
Java Thread 실행
  ↓ Blocking I/O 요청 (DB, 외부 API, File 등)
  ↓ task_struct → Wait Queue 이동 (CPU 사용 없음)
  ↓ Java Thread → Thread Pool에 반환되지 않음
  ↓ I/O 완료 이벤트 → Runnable Queue 복귀
```

Kernel 관점에서는 CPU를 사용하지 않지만, Application 관점에서는 Thread Pool을 계속 점유한다. CPU 사용률이 낮아도 Thread Pool 고갈로 신규 요청 처리가 지연될 수 있다.

### 일부 느린 I/O의 전체 영향

```
느린 I/O 요청 증가
  ↓ Worker Thread 장시간 점유
  ↓ 사용 가능한 Thread 감소
  ↓ 가벼운 요청도 Thread 할당 대기
  ↓ 전체 응답 지연
```

### Thread Pool 크기 증가의 위험

Blocking I/O 병목이 해소되지 않은 상태에서 Thread 수만 늘리면 아래 문제가 연쇄적으로 발생한다.

```
Thread Pool 크기 증가
  ↓ task_struct 증가  →  Context Switch 증가
  ↓ Memory 사용량 증가  →  Cache Miss 증가
  ↓ Scheduler Overhead 증가
```

I/O가 완료된 다수의 Thread가 동시에 Runnable Queue로 복귀하면 CPU Saturation이 발생할 수 있다.

---

## 6. Scheduler Overhead와 Cache Miss

### Scheduler Overhead 발생 비용

| 비용 항목 | 설명 |
|----------|------|
| Runnable Queue 관리 | Red-Black Tree 탐색 및 재정렬 |
| vruntime 계산 | 각 task의 누적 실행 시간 갱신 |
| Context Switch | CPU Context 저장 및 복구 (Kernel Mode) |
| Kernel Mode 진입 | Scheduler는 Kernel 영역에서만 실행 가능 |

```
Runnable task_struct 증가
  ↓ CFS 관리 비용 증가  →  System CPU Time(sy) 증가
  ↓ 비즈니스 로직 실행 시간 감소
```

### Context Switch와 Cache Miss

```
task_struct A 실행  →  A의 데이터가 L1/L2 Cache에 적재
  ↓ Context Switch
task_struct B 실행  →  B의 데이터가 Cache에 없음  →  Cache Miss
  ↓ RAM 접근 대기  →  CPU Stall 증가
```

---

## 7. Non-blocking I/O와 I/O 처리 구조

### Blocking vs Non-blocking 비교

| 항목 | Blocking I/O | Non-blocking I/O |
|------|-------------|-----------------|
| I/O 대기 중 Thread | 점유됨 | 다른 작업 수행 가능 |
| Thread Pool 압박 | 큼 | 상대적으로 작음 |
| task_struct 수 | 증가하기 쉬움 | 적게 유지 가능 |
| Context Switch | 증가 가능 | 상대적으로 적음 |

### Non-blocking 처리 흐름

```
I/O 요청
  ↓ Kernel에 관심 이벤트 등록
  ↓ Thread는 다른 작업 수행
  ↓ I/O 완료 이벤트 수신  →  Callback / Event Handler 실행
```

### 네트워크 I/O vs 디스크 I/O

| I/O 종류 | Non-blocking 처리 방식 |
|---------|----------------------|
| Network Socket I/O | epoll 기반 이벤트 처리에 적합 |
| RDBMS + JDBC | Driver가 Blocking → Event Loop 차단 가능 |
| RDBMS + R2DBC | Non-blocking Driver → WebFlux/Netty와 결합 가능 |
| File I/O | epoll만으로는 제한적, io_uring 또는 전용 Pool 필요 |

---

## 8. epoll과 File Descriptor

Linux Kernel은 I/O 통로를 File Descriptor(FD)로 관리한다. 소켓, 파일, 파이프 등은 모두 FD로 표현된다.

```
Socket 생성  →  FD 할당
  ↓ epoll_ctl()로 관심 이벤트 등록
  ↓ epoll_wait()로 준비된 이벤트 수신
```

| 항목 | 의미 |
|------|------|
| FD | 감시할 I/O 통로 |
| Event Type | `EPOLLIN`, `EPOLLOUT` 등 |
| User Data | 이벤트 발생 시 Application으로 전달할 정보 |

epoll은 데이터가 준비된 FD만 Ready List에 등록하므로 많은 연결을 효율적으로 처리할 수 있다.

### 네트워크 패킷 수신 흐름

```
Packet 도착  →  NIC DMA로 Memory 기록
  ↓ Hardware Interrupt 발생
  ↓ Kernel Interrupt Handler 실행
  ↓ Socket Buffer 업데이트  →  epoll Ready List 갱신
  ↓ Event Loop가 이벤트 처리
```

트래픽이 많아지면 Softirq 처리 비용이 증가하여 CPU 사용률 상승의 원인이 될 수 있다.

---

## 9. io_uring

io_uring은 Linux Kernel 5.1부터 도입된 비동기 I/O 인터페이스다. 기존 epoll/read/write 기반 방식보다 시스템 콜 오버헤드를 줄이기 위해 설계되었다.

| Queue | 역할 |
|-------|------|
| Submission Queue (SQ) | Application이 Kernel에 요청할 I/O 작업 등록 |
| Completion Queue (CQ) | Kernel이 완료된 I/O 결과 기록 |

공유 Ring Buffer를 통해 Application과 Kernel이 I/O 요청과 완료 결과를 교환한다. SQPOLL 모드에서는 Kernel Polling Thread가 SQ를 감시하므로 시스템 콜 빈도를 추가로 줄일 수 있다.

### io_uring vs 전용 Blocking Thread Pool

| 구분 | io_uring | 전용 Blocking Thread Pool |
|------|----------|--------------------------|
| 계층 | Kernel I/O 인터페이스 | Application 구조 |
| 목적 | 시스템 콜 및 I/O 오버헤드 감소 | Blocking 작업 격리 |
| Thread 수 | 적은 Thread 유지 가능 | Pool 크기만큼 Thread 추가 |
| 주요 대상 | File I/O, Network I/O | JDBC, Blocking SDK, Legacy API |
| 위험 | Kernel 버전 및 보안 이슈 고려 필요 | Context Switch 및 Pool 고갈 가능 |

---

## 10. JDBC vs R2DBC

| 항목 | JDBC | R2DBC |
|------|------|-------|
| I/O 방식 | Blocking | Non-blocking |
| Event Loop와 사용 | Event Loop 차단 위험 | Event Loop Thread 점유 최소화 |
| 적합한 구조 | Spring MVC + Thread Pool | Spring WebFlux + Netty |

WebFlux 기반 서비스에서 JDBC를 직접 사용하면 Event Loop가 Blocking되어 전체 처리량이 크게 저하될 수 있다. R2DBC, Non-blocking Client, 또는 전용 Blocking Thread Pool로 실행 흐름을 분리해야 한다.

---

## 11. SRE 관점 주요 지표

| 지표 | 의미 |
|------|------|
| CPU Utilization | CPU 사용률 |
| CPU Pressure (PSI) | CPU 대기 압력 |
| Run Queue Length | Runnable task 수 |
| Load Average | Running/Runnable 및 D 상태 task 누적 |
| Context Switches | 실행 전환 빈도 |
| System CPU (`sy`) | Kernel 작업 비중 |
| User CPU (`us`) | Application 작업 비중 |
| Softirq CPU | 네트워크 패킷 처리 비용 |
| Open FD Count | 열린 File Descriptor 수 |
| Thread Count | JVM / OS Thread 수 |
| Thread Pool Active Count | 사용 중인 Thread 수 |
| DB Connection Pool Usage | DB 병목 여부 |
| P99 / P999 Latency | 사용자 관점 응답 지연 |

### 진단 명령어

```bash
vmstat 1
top -H -p <PID>
pidstat -w -p <PID> 1
mpstat -P ALL 1
ss -s
lsof -p <PID> | wc -l
perf stat -e context-switches,cpu-migrations,cache-misses,cache-references -p <PID>
cat /proc/pressure/cpu
```

---

## 12. 최종 정리

### CPU Saturation 연쇄 구조 (Blocking 방식)

```
Thread-per-request + Blocking I/O
  ↓ Thread Pool 점유  →  task_struct 증가
  ↓ Runnable Queue 증가  →  Context Switch 증가
  ↓ Cache Miss 증가  →  Scheduler Overhead 증가
  ↓ CPU Saturation  →  Throughput 감소
```

### CPU 효율 향상 구조 (Non-blocking 방식)

```
Event-loop + Non-blocking I/O
  ↓ 적은 수의 task_struct 유지
  ↓ Context Switch 감소  →  Scheduler Overhead 감소
  ↓ CPU 효율 증가  →  Throughput 향상
```

| 항목 | 내용 |
|------|------|
| CPU Saturation 핵심 원인 | Runnable task_struct 과다, Context Switch 증가, Scheduler Overhead |
| Thread Pool 크기 증가의 한계 | Blocking I/O 병목 미해소 시 Scheduling 비용만 증가 |
| epoll 역할 | 준비된 FD만 감시하여 많은 연결을 효율적으로 처리 |
| io_uring 역할 | Kernel 수준 I/O 인터페이스, 시스템 콜 오버헤드 감소 |
| WebFlux에서 JDBC 사용 위험 | Event Loop Blocking으로 전체 처리량 저하 |
| 핵심 모니터링 대상 | Run Queue Length, Context Switch, System CPU, Load Average, P99 Latency |

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*