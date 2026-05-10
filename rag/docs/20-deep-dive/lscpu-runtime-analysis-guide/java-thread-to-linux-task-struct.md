# Java Thread에서 Linux task_struct까지: End-to-End 실행 구조

## 1. 전체 실행 흐름

`new Thread().start()`가 호출되면 JVM, pthread, Linux Kernel을 거쳐 최종적으로 `task_struct`라는 커널 실행 단위로 실체화된다.

```
Java Thread
  ↓ JVM Runtime
  ↓ pthread
  ↓ clone()
  ↓ task_struct / LWP
  ↓ CFS Scheduler
  ↓ Logical CPU
  ↓ Physical Core
```

---

## 2. Hardware 계층 구조

### 2-1. Physical Core

실제 연산을 수행하는 하드웨어 자원이다.

| 구성 요소 | 역할 |
|----------|------|
| ALU | 정수 연산 |
| FPU | 부동소수점 연산 |
| LSU | 메모리 Load/Store |
| Register | 실행 상태 저장 |
| Cache | 고속 데이터 접근 |
| Pipeline | 명령어 실행 |

### 2-2. Hardware Context (Hyper-Threading / SMT)

Hyper-Threading 또는 SMT가 활성화되면 하나의 Physical Core 안에 여러 Hardware Context가 존재한다.

```
Physical Core
├── Hardware Context 0
└── Hardware Context 1
```

각 Hardware Context는 독립적인 실행 상태를 가진다.

| 항목 | 설명 |
|------|------|
| Register Set | CPU 실행 상태 |
| Program Counter | 다음 명령어 주소 |
| Stack Pointer | Stack 위치 |
| APIC ID | CPU 식별자 |

### 2-3. Logical CPU

Linux Kernel은 Hardware Context를 Logical CPU로 인식한다.

```
Hardware Context  →  Logical CPU (cpu0, cpu1, cpu2 ...)
```

CFS Scheduler는 Logical CPU 단위로 실행 대상(task_struct)을 배치한다.

---

## 3. new Thread()와 start()의 차이

| 구분 | 동작 |
|------|------|
| `new Thread()` | JVM Heap에 Java 객체만 생성. OS Thread/task_struct 생성 없음 |
| `thread.start()` | JVM Native Layer → pthread → clone() → task_struct 생성 |

### start() 내부 흐름

`start()`는 내부적으로 Native Method를 호출한다.

```java
public synchronized void start() {
    start0(); // JVM 내부 Native Code로 연결
}

private native void start0();
```

---

## 4. JVM Native Layer의 역할

`start0()` 호출 이후 JVM Native Layer에서 수행하는 작업이다.

| 작업 | 설명 |
|------|------|
| Thread 상태 검증 | 이미 시작된 Thread인지 확인 |
| JavaThread 객체 생성 | JVM 내부 Thread 표현 생성 |
| Stack 설정 | Thread Stack 크기 및 속성 준비 |
| Entry Point 설정 | `run()` 메서드 실행 시작 지점 연결 |
| OS Thread 생성 요청 | `pthread_create()` 호출 |

---

## 5. pthread와 clone()

### 역할 비교

| 구분 | `pthread_create()` | `clone()` |
|------|-------------------|-----------|
| 계층 | User-space Library | System Call |
| 역할 | Thread 생성 API | Kernel에 task 생성 요청 |
| 호출 주체 | JVM / Native Runtime | pthread 내부 |
| 결과 | Thread 생성 요청 준비 | task_struct 생성 |

### 흐름

```
JVM Native Layer
  ↓ pthread_create()
  ↓ clone()
  ↓ Linux Kernel
  ↓ task_struct 생성
```

---

## 6. 1:1:1 매핑 관계

Java Platform Thread 기준으로 다음 관계가 성립한다.

```
Java Thread 1개  ≈  pthread 1개  ≈  task_struct(LWP) 1개
```

---

## 7. task_struct

Linux Kernel이 실행 단위를 표현하는 핵심 구조체다. Process와 Thread 모두 `task_struct`로 관리한다.

| 정보 | 설명 |
|------|------|
| 실행 상태 | RUNNING, SLEEPING 등 |
| Register Context | CPU 상태 |
| Stack 정보 | Kernel Stack / User Stack |
| Scheduling 정보 | Priority, vruntime 등 |
| PID / TID | 식별자 |
| Memory 정보 | Address Space 공유 여부 |
| File 정보 | File Descriptor 공유 여부 |
| Signal 정보 | Signal 처리 정보 |

### LWP (Lightweight Process)

Linux에서 Thread는 **자원을 공유하는 task_struct**, 즉 LWP로 표현된다.

```
Thread = LWP = 자원을 공유하는 task_struct
```

---

## 8. TGID 그룹핑

동일 프로세스에 속한 Thread들은 TGID(Thread Group ID)로 묶인다.

| 실행 단위 | TID | TGID |
|----------|-----|------|
| Main Thread | 1000 | 1000 |
| Worker Thread 1 | 1001 | 1000 |
| Worker Thread 2 | 1002 | 1000 |
| Kafka Consumer Thread | 1003 | 1000 |

사용자 공간에서 말하는 PID는 실제로 TGID에 해당한다.

---

## 9. CFS Scheduler

### 동작 방식

```
┌──────────────┐
│  동작 방식   │
└──────────────┘

Runnable task_struct
  ↓ CFS Runqueue에 등록
  ↓ vruntime 기준으로 실행 대상 선택
  ↓ Logical CPU에 배치
```

- **vruntime**: 각 task가 CPU를 사용한 누적 시간. 값이 낮을수록 우선 실행 후보
- **Runqueue**: 각 Logical CPU가 독립적으로 보유

```
cpu0 runqueue
├── task_struct A
├── task_struct B
└── task_struct C
```

---

## 10. 동기 vs 비동기 호출의 task_struct 분리

### 동기 호출

같은 Thread(task_struct)가 호출 체인 전체를 실행한다.

```
task_struct 101
  ↓ Controller
  ↓ Service
  ↓ Repository
```

### 비동기 호출 — 실행 단위 분리

| 코드 형태 | 실행 단위 |
|----------|----------|
| `new Thread().start()` | 새로운 task_struct |
| `ExecutorService.submit()` | Worker Thread의 task_struct |
| `@Async` | Async Executor의 task_struct |
| `CompletableFuture.supplyAsync()` | ForkJoinPool 또는 지정 Executor의 task_struct |
| `parallelStream()` | ForkJoinPool Worker의 task_struct |

```
task_struct 101
  ↓ Task Queue에 작업 제출
  ↓ task_struct 202가 작업 실행  ← 실행 주체 변경
```

### 대기 상태(WAITING)와 CPU 사용

```
task_struct 101  →  WAITING 상태 (CPU 점유 없음)
task_struct 202, 203  →  실행 중
작업 완료 신호  →  task_struct 101  →  RUNNABLE 복귀
```

---

## 11. task_struct 생명주기

### 일회성 Thread

```
new Thread().start()  →  task_struct 생성  →  run() 실행  →  작업 종료  →  task_struct 제거
```

### Thread Pool Worker

```
Thread Pool 생성  →  Worker task_struct 생성  →  작업 실행  →  WAITING 복귀  →  다음 작업 대기
```

Thread Pool 방식에서는 task_struct가 재사용된다. Linux Kernel은 `clone()` 요청 시 task_struct를 생성하며, 미리 Pool을 운영하지 않는다. 재사용 최적화는 사용자 공간의 Thread Pool이 담당한다.

---

## 12. task_struct 생성 제한

Kernel은 무제한 task 생성을 허용하지 않는다.

| 제한 | 설명 | 확인 방법 |
|------|------|----------|
| `threads-max` | 시스템 전체 Thread/task 상한 | `cat /proc/sys/kernel/threads-max` |
| `ulimit -u` | 사용자별 Process/Thread 수 제한 | `ulimit -u` |
| `cgroup pids.max` | 컨테이너/cgroup별 task 수 제한 | `cat /sys/fs/cgroup/pids.max` |
| 메모리 제한 | Stack, Kernel 구조체, Native Memory 부족 | — |

### 관련 오류

```
java.lang.OutOfMemoryError: unable to create new native thread
```

이 오류는 Java Heap 부족이 아닌 **Native Thread 생성 실패**인 경우가 많다.

---

## 13. cgroup CPU Quota와 Throttling

cgroup은 task 수뿐만 아니라 CPU 사용 시간도 제한한다.

```
cgroup
  ↓ CFS Bandwidth Control
  ↓ CPU Quota 소진
  ↓ Throttling (Runnable task가 있어도 실행 제한)
  ↓ Latency 증가
```

Thread 수가 많고 CPU Quota가 낮을수록 Throttling이 발생하기 쉽다.

---

## 14. SRE 관점 주요 장애 패턴

### Thread 생성 실패

```
pthread_create() → clone() → 제한 도달 → unable to create new native thread
```

확인 항목: `ulimit -u`, `cat /proc/sys/kernel/threads-max`, `cat /sys/fs/cgroup/pids.max`

### Context Switching 증가

```
task_struct 수 증가 → Runnable task 증가 → Context Switch 증가 → CPU 효율 저하
```

확인: `vmstat 1`, `pidstat -w -p <PID> 1`

### CPU Throttling

```
Thread 증가 + CPU Limit 낮음 → CFS Quota 빠르게 소진 → Throttling 증가
```

확인 지표: `container_cpu_cfs_throttled_periods_total`, `container_cpu_cfs_throttled_seconds_total`

### Load Average 증가

```
Runnable task 증가 → Runqueue 증가 → Load Average 증가
```

확인: `uptime`, `top`

---

## 15. Java Thread Dump와 task_struct 연결

CPU 병목을 일으키는 Java Thread를 특정하는 절차이다.

```bash
# 1단계: Thread별 CPU 사용량 확인
top -H -p <PID>

# 2단계: 높은 CPU 사용 TID 확인 (예: TID = 12345)

# 3단계: TID를 16진수로 변환
printf "%x\n" 12345
# → 3039

# 4단계: jstack에서 해당 nid 검색
jstack <PID> | grep -i "nid=0x3039" -A 30
```

---

## 16. 전체 계층 정리

| 계층 | 구성 요소 | 역할 |
|------|----------|------|
| Application | `Runnable` / `run()` | 실행할 비즈니스 로직 |
| JVM | Java Thread Object | Java 실행 흐름 표현 |
| JVM Native | JavaThread / Native Layer | OS Thread 생성 준비 |
| Library | pthread | POSIX Thread API |
| System Call | `clone()` | Kernel에 task 생성 요청 |
| Kernel | task_struct / LWP | 실제 스케줄링 단위 |
| Scheduler | CFS | CPU 시간 분배 |
| Hardware | Logical CPU | Kernel이 인식하는 CPU 실행 단위 |
| Hardware | Physical Core | 실제 연산 자원 |

### End-to-End 실행 경로

```
Java Thread
  ↓ pthread_create()
  ↓ clone()
  ↓ task_struct / LWP
  ↓ CFS Runqueue
  ↓ Logical CPU
  ↓ Physical Core
```

### 핵심 접점 요약

| 접점 | 설명 |
|------|------|
| Software Thread 실체화 | task_struct / LWP |
| Hardware 실행 단위 | Logical CPU / Hardware Context |
| 두 흐름을 연결하는 주체 | CFS Scheduler |

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*