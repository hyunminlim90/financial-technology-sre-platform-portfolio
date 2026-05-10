# Java Thread에서 task_struct의 생명주기

## 1. task_struct 생성 흐름

`new Thread()`는 JVM Heap에 Java 객체만 생성한다. `task_struct`는 `start()` 호출 이후에 생성된다.

```
thread.start()
  ↓ JVM: start0() 호출 (native method)
  ↓ JVM Native Layer (C++): JVM_StartThread
      - 이미 실행 중인 Thread인지 검증
      - Stack 크기 및 Entry Point 설정
  ↓ OS Library: pthread_create()
  ↓ Linux Kernel: clone() 시스템 콜
  ↓ task_struct 생성 (TID 부여)
  ↓ CFS Runqueue 등록 (RUNNABLE 상태)
```

| 단계 | 실행 주체 | 역할 |
|------|----------|------|
| `new Thread()` | JVM | JVM Heap에 Thread 객체 생성. OS 자원 없음 |
| `thread.start()` | JVM | Native Method `start0()` 호출 |
| `JVM_StartThread` | JVM Native (C++) | Stack 설정, Entry Point(`run()`) 연결 |
| `pthread_create()` | libc / pthread | OS Thread 생성 요청 |
| `clone()` | Linux Kernel | `task_struct` 생성, TID 부여 |
| CFS Runqueue 등록 | CFS Scheduler | RUNNABLE 상태로 스케줄링 대기 |

---

## 2. task_struct 실행 흐름

생성된 `task_struct`는 CFS Scheduler에 의해 Logical CPU에 배치되고, `run()` 내부의 로직이 Physical Core에서 실행된다.

```
task_struct (RUNNABLE)
  ↓ CFS Runqueue 등록
  ↓ vruntime 기준으로 실행 대상 선택
  ↓ Context Switch → Logical CPU에 하드웨어 컨텍스트 로드
  ↓ run() 실행 ← 실제 비즈니스 로직이 CPU 사이클을 소모하는 지점
  ↓ run() 종료
  ↓ task_struct 종료 처리
```

> `start()`는 커널 자원(`task_struct`)을 확보하는 과정이고,  
> `run()`은 확보된 자원 위에서 실제 연산이 수행되는 단계다.

---

## 3. 케이스별 동작과 task_struct 상태

### 3-1. 동기 처리

하나의 `task_struct`가 호출 체인 전체를 순서대로 실행한다.

```java
// 예시: 동기 서비스 호출
public void handle() {
    String data = repository.find();   // 같은 task_struct가 실행
    service.process(data);             // 같은 task_struct가 실행
}
```

```
task_struct (TID: 101) → RUNNING
  ↓ repository.find()     (DB I/O 대기 중 → SLEEPING)
  ↓ I/O 완료 신호         (→ RUNNABLE 복귀)
  ↓ service.process()     (→ RUNNING)
  ↓ 완료
```

| 상태 전환 | 원인 |
|----------|------|
| RUNNING → SLEEPING | I/O 대기 (DB, 외부 API 등) |
| SLEEPING → RUNNABLE | I/O 완료 인터럽트 수신 |
| RUNNABLE → RUNNING | CFS가 Logical CPU에 배치 |

---

### 3-2. 비동기 처리

`ExecutorService`, `@Async`, `CompletableFuture` 등은 실행 주체(`task_struct`)가 교체된다.

```java
// 예시: ExecutorService
executorService.submit(() -> longTask());
```

```
task_struct (TID: 101) → 작업 제출 후 다음 코드 진행 또는 대기
task_struct (TID: 202) → longTask() 실행  ← 실행 주체 교체
```

```java
// 예시: CompletableFuture
CompletableFuture.supplyAsync(() -> fetchData())
                 .thenApply(data -> process(data));
```

```
task_struct (TID: 101) → supplyAsync() 제출 후 반환
task_struct (TID: 202) → fetchData() 실행  (ForkJoinPool Worker)
task_struct (TID: 203) → process() 실행    (thenApply 콜백)
```

| 코드 형태 | 실행 주체 |
|----------|----------|
| `new Thread().start()` | 새로운 `task_struct` |
| `ExecutorService.submit()` | Pool Worker의 `task_struct` |
| `@Async` | Async Executor의 `task_struct` |
| `CompletableFuture.supplyAsync()` | ForkJoinPool Worker의 `task_struct` |

---

### 3-3. 병렬 처리

여러 `task_struct`가 동시에 서로 다른 Logical CPU에서 실행된다.

```java
// 예시: parallelStream
list.parallelStream().map(item -> process(item)).collect(...);
```

```
task_struct (TID: 101) → WAITING (완료 대기)
task_struct (TID: 202) → process(item1)  → Logical CPU 0
task_struct (TID: 203) → process(item2)  → Logical CPU 1
task_struct (TID: 204) → process(item3)  → Logical CPU 2
  ↓ 모든 작업 완료
task_struct (TID: 101) → RUNNABLE 복귀 → 결과 수집
```

```java
// 예시: CompletableFuture.allOf()
CompletableFuture<Void> all = CompletableFuture.allOf(taskA, taskB, taskC);
all.join();
```

```
task_struct (TID: 101) → join() 호출 후 WAITING
task_struct (TID: 202) → taskA 실행
task_struct (TID: 203) → taskB 실행
task_struct (TID: 204) → taskC 실행
  ↓ 전체 완료 신호
task_struct (TID: 101) → RUNNABLE 복귀
```

> WAITING 상태의 `task_struct`는 CPU를 점유하지 않는다.  
> Kernel이 완료 신호를 받으면 해당 `task_struct`를 RUNNABLE로 전환한다.

---

## 4. task_struct 종료 흐름

```
run() 종료
  ↓ JVM Native 종료 처리
  ↓ Linux Kernel: task_struct → EXIT_ZOMBIE 상태
  ↓ 부모 프로세스가 리소스 수거
  ↓ task_struct 메모리에서 완전 제거
```

---

## 5. Thread Pool에서의 task_struct 재사용

`clone()` 시스템 콜은 Stack 할당, 권한 체크, 초기화 등 비용이 크다. Thread Pool은 이 비용을 줄이기 위해 `task_struct`를 재사용한다.

```
Thread Pool 초기화
  ↓ Worker task_struct N개 생성 (clone() × N)

새 작업 요청
  ↓ 기존 task_struct에 Runnable(run() 로직)만 교체
  ↓ clone() 재호출 없음
  ↓ WAITING → RUNNABLE → RUNNING
```

| 방식 | task_struct 처리 |
|------|----------------|
| `new Thread().start()` 매번 생성 | 작업마다 `clone()` 호출, 종료 시 제거 |
| Thread Pool 사용 | 최초 1회 생성 후 재사용, `run()` 로직만 교체 |

---

## 6. 생명주기 전체 요약

```
new Thread()
  └─ JVM Heap에 객체 생성 (커널 자원 없음)

thread.start()
  └─ pthread_create() → clone() → task_struct 생성 → CFS Runqueue 등록

RUNNABLE
  └─ CFS가 vruntime 기준으로 Logical CPU에 배치

RUNNING
  └─ run() 실행 (Physical Core에서 실제 연산)

SLEEPING / WAITING
  └─ I/O 대기 또는 다른 task 완료 대기 (CPU 점유 없음)

RUNNABLE 복귀
  └─ I/O 완료 또는 완료 신호 수신

run() 종료
  └─ EXIT_ZOMBIE → 리소스 수거 → task_struct 제거
      (Thread Pool의 경우 제거 없이 WAITING 상태로 복귀)
```

| 상태 | 설명 | CPU 점유 |
|------|------|---------|
| RUNNABLE | CFS Runqueue 대기 중 | 없음 |
| RUNNING | Logical CPU에서 실행 중 | 있음 |
| SLEEPING | I/O 대기 | 없음 |
| WAITING | 다른 task 완료 대기 | 없음 |
| EXIT_ZOMBIE | 종료 후 리소스 수거 대기 | 없음 |

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*