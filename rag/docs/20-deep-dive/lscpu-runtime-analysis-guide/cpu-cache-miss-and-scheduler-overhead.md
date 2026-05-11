# CPU Cache Miss와 Scheduler Overhead

## 1. 개요

CPU Cache Miss는 CPU가 필요한 데이터를 Cache에서 찾지 못하고 RAM까지 접근해야 하는 상황입니다.

Cache Miss가 증가하면 CPU는 메모리 응답을 기다리는 시간이 늘어나고, 이는 Scheduler Overhead 증가와 CPU Saturation으로 이어질 수 있습니다.

---

## 2. CPU Cache 계층 구조

CPU는 RAM보다 훨씬 빠르게 동작합니다. 자주 사용하는 데이터를 빠르게 접근하기 위해 CPU 내부에 Cache 계층이 존재합니다.

| 계층 | 특징 |
|------|------|
| L1 Cache | 속도가 가장 빠르고 용량이 가장 작음 |
| L2 Cache | L1보다 크고 약간 느림 |
| L3 Cache | 여러 Core가 공유하는 경우가 많음 |
| RAM | 용량은 크지만 CPU 접근 기준으로 매우 느림 |

---

## 3. Cache Hit와 Cache Miss

| 구분 | 의미 | 결과 |
|------|------|------|
| Cache Hit | 필요한 데이터가 Cache에 존재 | 즉시 연산 수행 가능 |
| Cache Miss | 필요한 데이터가 Cache에 없음 | RAM 접근 필요, 지연(CPU Stall) 발생 |

Cache Miss 발생 시 흐름은 다음과 같습니다.

```
CPU 명령어 실행
  ↓ Cache 조회
  ↓ Cache Miss 발생
  ↓ RAM 접근 요청
  ↓ 데이터 도착까지 CPU Stall
  ↓ 데이터 도착 후 연산 재개
```

CPU 사용률은 높게 표시될 수 있지만, 실제로는 메모리 응답 대기 시간이 실행 시간을 차지하는 상태입니다.

---

## 4. Context Switch와 Cache Miss의 관계

Context Switch가 자주 발생하면 CPU Cache 효율이 저하됩니다.

각 `task_struct`는 실행 중 자신의 데이터를 Cache에 적재합니다. Context Switch로 다른 `task_struct`가 실행되면 기존 Cache 내용은 새 `task_struct`에 적합하지 않으므로 Cache Miss가 발생합니다.

```
task_struct A 실행  →  A의 데이터가 Cache에 적재
  ↓ Context Switch
task_struct B 실행  →  B의 데이터가 Cache에 없음  →  Cache Miss 증가
```

이 현상이 반복되면 Cache Locality가 약해지고, RAM 접근 빈도가 증가합니다.

---

## 5. Runnable task_struct 증가와 Cache Miss 연쇄 구조

Runnable 상태의 `task_struct`가 과도하게 많아지면 각 `task_struct`가 CPU에서 연속으로 실행되는 시간이 짧아집니다. Cache를 충분히 활용하기 전에 CPU에서 내려오게 되어 Cache Miss가 증가합니다.

```
Runnable task_struct 증가
  ↓ Context Switch 증가
  ↓ Cache Locality 저하
  ↓ Cache Miss 증가
  ↓ CPU Stall 증가
```

---

## 6. 구조별 Cache Miss 발생 패턴

### Thread-per-request 구조

요청 수가 증가할수록 Thread 수가 증가하고, 각 Thread는 서로 다른 Stack, 지역 변수, 요청 데이터를 사용합니다. Thread 전환이 많아질수록 Cache 재사용 가능성이 낮아집니다.

```
요청 증가  →  Thread 증가  →  task_struct 증가
  ↓ Context Switch 증가  →  Cache Miss 증가
```

### Blocking I/O 구조

I/O 대기 중 Thread가 Sleeping 상태로 전환되고, 다른 `task_struct`가 CPU를 점유합니다. I/O 완료 후 Runnable로 복귀하면 이전에 적재했던 Cache 상태가 유지되지 않을 가능성이 높습니다.

```
Thread 실행  →  Blocking I/O 요청  →  Sleeping 상태
  ↓ 다른 task_struct 실행 (Cache 교체)
  ↓ I/O 완료  →  Runnable 복귀  →  Cache Miss 가능성 증가
```

---

## 7. Cache Miss가 Scheduler Overhead를 증가시키는 구조

### Time Slice 소진 증가

Cache Miss로 작업 시간이 길어지면 Time Slice를 모두 소진하는 경우가 늘어납니다. Scheduler가 개입하여 Context Switch를 수행해야 하는 빈도가 증가합니다.

```
Cache Miss 증가  →  작업 시간 증가  →  Time Slice 소진
  ↓ Preemption 증가  →  Context Switch 증가
```

### Runnable Queue 관리 부담 증가

Runnable `task_struct`가 많을수록 CFS는 Red-Black Tree에서 더 많은 실행 후보를 관리해야 합니다.

```
Runnable task_struct 증가
  ↓ CFS Red-Black Tree 관리 증가
  ↓ Scheduler Overhead 증가
```

### Context Switch 후 Runnable Queue 재정렬 증가

Context Switch 이후 `task_struct`는 증가한 `vruntime` 기준으로 Runnable Queue에 재삽입됩니다. 전환 빈도가 높을수록 삽입, 선택, 재정렬 작업이 증가합니다.

```
Context Switch 증가
  ↓ task_struct 재등록 증가
  ↓ Runnable Queue 재정렬 증가
  ↓ Scheduler CPU 사용 증가
```

---

## 8. Scheduler Overhead와 CPU Saturation

Scheduler는 Kernel 코드로, CPU에서 실행됩니다. Scheduler 실행 빈도가 높아질수록 실제 비즈니스 로직이 사용할 수 있는 CPU 시간이 줄어듭니다.

```
Cache Miss 증가
  ↓ 작업 시간 증가
  ↓ Context Switch 증가
  ↓ Scheduler Overhead 증가
  ↓ 비즈니스 로직 실행 시간 감소
  ↓ CPU Saturation
```

CPU 사용률이 높게 표시되더라도 실제 처리량(Throughput)은 오히려 감소할 수 있습니다.

---

## 9. Thrashing

실제 작업보다 실행 흐름 전환과 스케줄링 관리에 과도한 CPU 자원이 소비되는 상태입니다.

```
Thread 과다
  ↓ Context Switch 과다
  ↓ Cache Miss 증가
  ↓ Scheduler Overhead 증가
  ↓ Throughput 감소
```

CPU 사용률은 높지만 서비스 처리량은 낮은 상태가 지속됩니다.

---

## 10. Non-blocking 구조와 Cache 효율

Event-loop 기반 Non-blocking 구조는 적은 수의 `task_struct`로 많은 요청을 처리합니다. Context Switch가 줄어들어 Cache Locality를 유지하기 쉽습니다.

```
적은 수의 EventLoop Thread
  ↓ Context Switch 감소
  ↓ Cache Locality 유지
  ↓ Cache Miss 감소
  ↓ CPU 효율 증가
```

Netty, Spring WebFlux 같은 비동기 네트워크 서버의 성능 기반이 되는 구조입니다.

---

## 11. SRE 관점 주요 지표

| 지표 | 의미 |
|------|------|
| Context Switch Rate | `task_struct` 전환 빈도 |
| Run Queue Length | CPU 대기 task 수 |
| Load Average | Runnable 및 D 상태 task 누적 수 |
| CPU Usage | 전체 CPU 사용률 |
| CPU System Time | Kernel 작업(Scheduler 등)의 CPU 비중 |
| Cache Miss Rate | Cache 효율 저하 여부 |
| Throughput | 실제 처리량 |
| P99 Latency | 지연 시간 변동성 |

### 확인 명령어

```bash
# Context Switch 전체 확인
vmstat 1

# Thread별 CPU 사용 확인
top -H -p <PID>

# Context Switch 상세 확인
pidstat -w -p <PID> 1

# Hardware Cache Miss 분석
perf stat -e cache-references,cache-misses,cycles,instructions -p <PID>
```

---

## 12. 최종 정리

CPU Cache Miss는 단순한 메모리 접근 지연이 아니라 Scheduler Overhead와 CPU Saturation으로 이어질 수 있는 핵심 원인입니다.

```
Runnable task_struct 증가
  ↓ Context Switch 증가
  ↓ Cache Locality 저하  →  Cache Miss 증가
  ↓ CPU Stall 증가  →  작업 시간 증가
  ↓ Scheduler Overhead 증가
  ↓ CPU Saturation  →  Throughput 감소
```

SRE 관점에서는 CPU 사용률 단일 지표만 관찰하는 것이 아니라, Context Switch, Cache Miss Rate, Runqueue Length, System CPU Time, Throughput, P99 Latency를 함께 모니터링해야 합니다.

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*