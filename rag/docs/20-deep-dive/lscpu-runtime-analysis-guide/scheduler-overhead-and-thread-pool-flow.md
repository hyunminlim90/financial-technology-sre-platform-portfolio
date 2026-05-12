# Scheduler Overhead와 Thread Pool 실행 구조

## 1. 개요

Scheduler Overhead는 CPU가 실제 비즈니스 로직을 실행하는 시간보다, 실행할 `task_struct`를 선택하고 전환하는 Kernel 관리 작업에 더 많은 비용을 사용하는 상태를 의미합니다.

이 문제는 Thread-per-request 구조, Blocking I/O, 과도한 Thread Pool 크기와 결합될 때 심화됩니다.

---

## 2. Running vs Runnable 상태

| 상태 | 의미 |
|------|------|
| Running | 현재 Logical CPU를 점유하여 실제 명령어를 실행 중인 상태 |
| Runnable | 실행 준비는 완료되었지만 CPU를 할당받지 못해 대기 중인 상태 |

Logical CPU가 8개인 경우 동시에 Running 상태가 될 수 있는 `task_struct`는 최대 8개입니다. 반면 Runnable 상태의 `task_struct`는 수백~수천 개까지 누적될 수 있습니다.

---

## 3. CFS Runnable Queue와 Scheduler Overhead 발생 구조

CFS는 Runnable 상태의 `task_struct`를 `vruntime` 기준으로 정렬된 Red-Black Tree로 관리합니다. `vruntime`이 낮을수록 CPU를 적게 사용한 것으로 판단되어 우선 실행 후보가 됩니다.

### Scheduler Overhead 증가 원인

| 원인 | 설명 |
|------|------|
| Runnable `task_struct` 증가 | Red-Black Tree 관리 비용 증가 |
| Context Switch 증가 | PC, SP, Register, Memory Context 저장/복구 비용 (Kernel Mode에서 수행) |
| Runnable Queue 재등록/재정렬 | Context Switch 후 증가된 `vruntime` 기준으로 재배치 |

```
Runnable task_struct 증가
  ↓ Context Switch 빈도 증가
  ↓ vruntime 갱신 → Runnable Queue 재정렬 반복
  ↓ Scheduler가 사용하는 CPU 시간 증가
```

---

## 4. CPU Saturation으로 이어지는 연쇄 구조

```
Runnable task_struct 증가
  ↓ Context Switch 증가
  ↓ Scheduler Overhead 증가
  ↓ System CPU Time(sy) 증가
  ↓ 비즈니스 로직 실행 시간 감소
  ↓ Throughput 감소  →  CPU Saturation
```

CPU 사용률은 높게 표시되지만 실제 요청 처리 효율은 낮아지는 상태입니다.

### User CPU vs System CPU

| 항목 | 의미 | Scheduler Overhead와의 관계 |
|------|------|---------------------------|
| User CPU (`us`) | 애플리케이션 코드 실행에 사용된 CPU 시간 | 정상 범위 유지가 목표 |
| System CPU (`sy`) | Kernel 코드 실행에 사용된 CPU 시간 | Scheduler Overhead 증가 시 상승 |

---

## 5. Blocking I/O와 Scheduler Overhead

Blocking I/O 발생 시 `task_struct`는 Wait Queue로 이동하고, I/O 완료 후 Runnable Queue로 복귀합니다.

```
Running
  ↓ Blocking I/O 요청  →  Sleeping / Wait Queue 이동
  ↓ I/O 완료 이벤트  →  Runnable Queue 복귀
```

다수의 Thread가 I/O 완료 후 동시에 Runnable 상태로 복귀하면 Runnable Queue가 급격히 증가하고 Scheduler Overhead가 커집니다.

### Thread Pool과의 관계

Kernel 관점에서는 `task_struct`가 Wait Queue에 있더라도, Java Thread Pool 관점에서는 해당 Thread가 I/O 완료 전까지 Pool에 반환되지 않습니다.

```
Java Thread  →  Blocking I/O  →  task_struct Wait Queue 이동
                                  Java Thread Pool에는 반환되지 않음
```

Blocking I/O가 누적되면 Thread Pool 고갈로 이어집니다.

---

## 6. Thread Pool 크기 증가의 한계

Thread Pool 고갈 해결을 위해 Thread 수를 과도하게 늘리면 문제가 다른 형태로 전환됩니다.

```
Thread Pool 크기 증가
  ↓ Java Thread 증가  →  task_struct 증가
  ↓ Runnable task 증가 가능성 증가
  ↓ Context Switch 증가
  ↓ Scheduler Overhead 증가
```

Thread 수 증가는 I/O 대기 문제를 근본적으로 해결하지 못하고, CPU Scheduling 비용을 증가시킵니다.

---

## 7. Blocking vs Non-blocking 방식 비교

| 관점 | Blocking 방식 | Non-blocking 방식 |
|------|-------------|-----------------|
| Kernel `task_struct` | I/O 대기 시 Wait Queue 이동 | Event 기반 대기 |
| Java Thread | I/O 완료 전까지 점유 | 빠르게 반환 또는 다음 이벤트 처리 |
| Thread Pool | 고갈 가능성 큼 | 상대적으로 안정적 |
| Runnable Queue | I/O 완료 시 급증 가능 | 적은 Thread 수 유지 |
| Scheduler Overhead | 증가 가능 | 상대적으로 낮음 |

### Non-blocking 처리 흐름

```
I/O 요청
  ↓ Callback / Event 등록
  ↓ Thread 반환 또는 다음 이벤트 처리
  ↓ I/O 완료 시 이벤트 처리
```

적은 수의 Thread로 많은 I/O 요청을 처리할 수 있어 Runnable `task_struct` 수와 Context Switch 비용을 줄이는 데 유리합니다.

---

## 8. SRE 관점 주요 진단 지표

| 지표 | 의미 |
|------|------|
| `vmstat cs` | Context Switch 횟수 |
| `vmstat r` | Runnable task 수 |
| CPU `us` | User Mode CPU 사용률 |
| CPU `sy` | Kernel Mode CPU 사용률 |
| Load Average | Runnable 및 D 상태 task 누적 |
| Thread Count | JVM 전체 Thread 수 |
| Thread Pool Active Count | 현재 사용 중인 Thread 수 |
| Thread Pool Queue Size | 처리 대기 중인 작업 수 |
| Cache Miss Rate | Context Switch에 따른 Cache 효율 저하 |
| P99 Latency | 사용자 관점 응답 지연 |

### 진단 명령어

```bash
vmstat 1
top -H -p <PID>
pidstat -w -p <PID> 1
mpstat -P ALL 1
perf stat -e context-switches,cpu-migrations,cache-misses,cache-references -p <PID>
```

---

## 9. 최종 정리

```
Thread-per-request + Blocking I/O
  ↓ Thread Pool 점유  →  task_struct 증가
  ↓ Runnable Queue 증가
  ↓ Context Switch 증가  →  Scheduler Overhead 증가
  ↓ CPU Saturation  →  Throughput 감소
```

| 항목 | 내용 |
|------|------|
| Scheduler Overhead 핵심 원인 | Runnable `task_struct` 과다, Context Switch 증가 |
| Thread Pool 크기 증가의 한계 | task_struct 증가로 Scheduler 비용만 높아짐 |
| System CPU(`sy`) 증가 의미 | Kernel 스케줄링 작업 비율 증가 |
| 근본적 해결 방향 | Non-blocking I/O, Bulkhead, Timeout, Backpressure, Thread Pool 격리 |

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*