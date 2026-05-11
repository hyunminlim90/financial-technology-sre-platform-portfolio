# Thread-per-request 구조와 CPU Saturation

## 1. 개요

Thread-per-request 구조는 하나의 클라이언트 요청(Request)에 대해 하나의 전용 Thread를 할당하여 처리하는 방식입니다. 대표적으로 Spring MVC + Tomcat 기반 구조에서 사용됩니다.

순차적인 처리 흐름을 유지하기 쉽다는 장점이 있지만, 동시 요청 수가 증가하는 환경에서는 아래 문제들이 연쇄적으로 발생할 수 있습니다.

- Context Switch 증가
- CPU Cache Miss 증가
- Scheduler Overhead 증가
- CPU Saturation

---

## 2. 기본 처리 흐름

```
Client Request
  ↓ Thread Pool에서 Thread 할당
  ↓ Controller → Service → DB / External API
  ↓ Response 반환
  ↓ Thread 반환 또는 종료
```

요청 하나가 완료될 때까지 동일한 Thread(`task_struct`)가 전체 흐름을 담당합니다.

### Linux Kernel 관점

Java Thread는 최종적으로 Linux Kernel의 `task_struct`로 실체화되어 CFS Scheduler에 의해 관리됩니다.

```
Java Thread
  ↓ JVM Native Layer → pthread_create() → clone()
  ↓ task_struct 생성
  ↓ CFS Scheduler 등록
  ↓ Logical CPU 실행
```

따라서 동시 요청 수가 증가하면 Runnable 상태의 `task_struct` 수도 함께 증가합니다.

---

## 3. Runnable task_struct 증가와 Scheduler Overhead

동시 요청이 증가할수록 Thread 수가 증가하고, CFS Scheduler는 더 많은 `task_struct` 중에서 다음 실행 대상을 선택해야 합니다.

```
동시 요청 1,000개
  ↓ 1,000 Java Threads  →  1,000 task_struct
  ↓ Runnable Queue 증가
  ↓ Scheduler 계산량 증가  →  Scheduler Overhead 증가
```

Runnable `task_struct`가 많아질수록 아래 비용이 증가합니다.

| 비용 항목 | 설명 |
|----------|------|
| vruntime 계산 | 각 task의 누적 실행 시간 갱신 |
| Red-Black Tree 탐색 및 재정렬 | 실행 대상 선택 및 재배치 |
| Context Switch 빈도 증가 | Register, PC, Stack Pointer 저장/복구 |
| Kernel Mode 진입 증가 | Scheduler는 Kernel 영역에서만 실행 가능 |

---

## 4. Blocking I/O와의 결합 문제

Thread-per-request 구조는 일반적으로 Blocking I/O와 함께 사용됩니다. I/O 대기 구간에서 Thread는 Sleeping 상태로 전환되고, I/O 완료 후 Runnable 상태로 복귀하는 과정이 반복됩니다.

```
Thread 실행
  ↓ Blocking I/O 요청 (DB Query, External API, File I/O 등)
  ↓ Sleeping 상태 전환 (CPU 점유 없음)
  ↓ I/O 완료 인터럽트 수신
  ↓ Runnable 복귀
  ↓ CFS가 Logical CPU에 재배치
```

Runnable ↔ Sleeping 전환이 반복될수록 Context Switch가 급격히 증가합니다.

---

## 5. Context Switch 증가와 CPU Cache Miss

Context Switch가 많아지면 CPU Cache 효율이 저하됩니다. 각 `task_struct`는 서로 다른 데이터와 메모리 영역을 사용하므로, 전환 시 기존 Cache 내용이 새 `task_struct`에 적합하지 않습니다.

```
task_struct A 실행  →  A의 데이터가 L1/L2 Cache에 적재
  ↓ Context Switch
task_struct B 실행  →  B의 데이터가 Cache에 없음  →  Cache Miss 발생
  ↓ RAM 접근 필요  →  CPU Stall 발생
```

Cache Miss가 증가하면 CPU는 RAM 응답을 기다리는 시간이 늘어나 작업 실행 시간이 길어집니다.

---

## 6. 메모리 사용량 증가

Java Thread는 독립적인 Stack Memory를 사용합니다. Thread 수 증가는 메모리 사용량 증가로 직결됩니다.

| Thread 수 | 예상 Stack 사용량 (Thread당 1MB 기준) |
|----------|--------------------------------------|
| 100 | 약 100MB |
| 1,000 | 약 1GB |
| 5,000 | 약 5GB |

---

## 7. CPU Saturation으로 이어지는 연쇄 구조

위 문제들이 결합되면 아래와 같은 연쇄 반응이 발생합니다.

```
트래픽 증가
  ↓ Thread 증가  →  task_struct 증가
  ↓ Runnable Queue 증가
  ↓ Context Switch 증가
  ↓ CPU Cache Miss 증가  →  CPU Stall 증가
  ↓ Scheduler Overhead 증가
  ↓ CPU Saturation  →  Throughput 감소
```

---

## 8. CPU Usage와 Throughput의 괴리

CPU Usage가 높다고 항상 시스템이 효율적으로 동작하는 것은 아닙니다.

| 지표 | 상태 | 의미 |
|------|------|------|
| CPU Usage | 100% | CPU가 지속적으로 점유됨 |
| Throughput | 감소 | 실제 요청 처리량 저하 |
| Context Switch | 급증 | 실행 흐름 교체 비용 증가 |
| System CPU Time | 증가 | Kernel 스케줄링 작업 비율 증가 |
| P99 Latency | 증가 | 응답 지연 심화 |

CPU는 점유되어 있지만 실제 비즈니스 로직 처리 효율은 낮아지는 상태입니다.

---

## 9. Event-loop 기반 Non-blocking 구조와 비교

Non-blocking 구조는 적은 수의 EventLoop Thread를 유지하면서 많은 요청을 처리합니다.

| 항목 | Thread-per-request (Blocking) | Event-loop (Non-blocking) |
|------|-------------------------------|--------------------------|
| Thread 수 | 요청 수에 비례하여 증가 | CPU Core 수 중심으로 고정 |
| Blocking I/O 대기 | Thread가 Sleeping 상태로 대기 | 대기 없이 다른 이벤트 처리 |
| Context Switch | 많음 | 적음 |
| CPU Cache 효율 | 낮음 | 높음 |
| Scheduler Overhead | 큼 | 작음 |

대표 기술: Netty, Spring WebFlux, Reactor

---

## 10. SRE 관점 주요 병목 지표

| 현상 | 원인 |
|------|------|
| CPU Usage 급증 | Runnable `task_struct` 과다 |
| Throughput 감소 | Scheduler Overhead가 실행 시간 잠식 |
| Context Switch 증가 | 과도한 Thread 경쟁 |
| Load Average 증가 | Runnable Queue 적체 |
| Cache Miss 증가 | Context Switch 과다로 Cache Locality 저하 |
| P99 Latency 증가 | CPU Stall 및 Scheduling 지연 |
| `unable to create new native thread` | Thread 생성 한도 도달 |

### 진단 명령어

```bash
# Thread별 CPU 사용 확인
top -H -p <PID>

# Context Switch 전체 확인
vmstat 1

# Thread별 스케줄링 통계
pidstat -wt 1

# CPU Core별 사용률 확인
mpstat -P ALL 1

# Hardware Cache Miss 분석
perf stat -e cache-misses,cache-references -p <PID>
```

---

## 11. 최종 정리

Thread-per-request 구조는 동시 요청 증가 시 아래 문제가 연쇄적으로 발생할 수 있습니다.

```
Thread 증가  →  task_struct 증가
  ↓ Context Switch 증가  →  Cache Miss 증가
  ↓ Scheduler Overhead 증가
  ↓ CPU Saturation  →  Throughput 감소
```

고동시성 환경에서는 아래 방향의 설계가 중요합니다.

| 방향 | 설명 |
|------|------|
| Non-blocking I/O 적용 | Blocking 대기 중 Thread 점유 제거 |
| Event-loop 기반 구조 | 적은 `task_struct`로 다수 요청 처리 |
| Thread Pool 크기 최적화 | CPU Core 수 대비 적정 Thread 수 유지 |
| Context Switch 최소화 | Cache Locality 유지로 CPU 효율 향상 |

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*