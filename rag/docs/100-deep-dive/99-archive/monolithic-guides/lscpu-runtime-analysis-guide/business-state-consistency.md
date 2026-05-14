# Business Domain 계층의 Session / Payment 상태 (E2E 분석 적용됨)

Lock(Mutex/Spinlock)은 단순한 [CPU 동기화 기술](../20-deep-dive/thread-synchronization.md)이 아니라 **비즈니스 데이터 무결성(Data Integrity)을 보호하기 위한 수단**입니다.

특히 아래와 같은 Business Domain 상태는 동시에 여러 요청(Request)이 접근할 수 있기 때문에 동시성 제어가 매우 중요합니다:

- Session
- Payment
- Order
- Balance
- Inventory

</br>

## 대표적인 예시

동일 결제(`paymentId`)에 대해 다음이 동시에 들어올 수 있습니다:

```text
Request A → 승인 처리
Request B → 승인 취소
Request C → 중복 승인 요청
```

적절한 동시성 제어가 없으면:

- 중복 결제
- 이중 승인
- 상태 불일치
- 데이터 손상

등이 발생할 수 있습니다.

### 계층별 동시 요청의 실제 경로

```
[Application] 동일 paymentId로 Request A/B/C 동시 수신
        ↓
[JVM Runtime] 각 요청이 별도 Thread로 스케줄링 → ThreadPoolExecutor 큐 적재
        ↓
[OS Kernel] 각 Thread는 task_struct로 표현 → CFS Runqueue에 병렬 등록
        ↓
[Hardware] 멀티코어 환경에서 실제로 동시 실행 → 공유 메모리 경합 발생
        ↓
[OS Kernel] Futex(Fast Userspace Mutex) 또는 DB Lock을 통해 직렬화 강제
```

### SRE 관찰 도구

| 도구 | 관찰 대상 |
|---|---|
| `perf stat -e context-switches -p <pid>` | 동시 요청 처리 중 Context Switch 빈도 |
| `cat /proc/<pid>/status \| grep Threads` | 동시 처리 중인 스레드 수 |
| `jstack <pid>` | BLOCKED 상태 스레드 및 대기 Lock 주소 확인 |
| DB slow query log | Lock 대기로 인한 지연 쿼리 감지 |

---

## 왜 Lock이 필요한가?

Business Domain 계층에서는 **"동일 상태를 동시에 변경하지 못하도록"** 보호해야 합니다.

예:

```text
현재 결제 상태: PENDING

Thread A → APPROVED 변경
Thread B → FAILED 변경

→ 동시 발생 시 최종 상태 불일치 가능
```

따라서 다음을 사용하여 동일 Business 상태 변경 순서를 제어합니다:

- DB Row Lock
- Distributed Lock
- Optimistic Lock
- `synchronized`
- CAS (Compare-And-Set)

### 계층별 Lock 필요성의 메커니즘 실체

#### Hardware 계층

- **CPU 아키텍처의 근본 문제**: 멀티코어 환경에서 각 코어는 독립적인 L1/L2 Cache를 보유. 동일 메모리 주소를 두 코어가 동시에 읽고 수정하면 **Cache Coherency** 위반 발생
- **Memory Ordering**: CPU는 Out-of-Order 실행으로 Store/Load 순서를 재배치할 수 있음. `MFENCE`, `LOCK` prefix 명령어로 순서 보장 강제
- **CAS(Compare-And-Swap)**: x86의 `CMPXCHG` 명령어. Lock-Free 자료구조의 원자적 상태 변경 기반. `LOCK CMPXCHG`로 버스 잠금 또는 Cache Line 잠금 수행
- **Cache Line Thrashing**: 동일 Cache Line(64byte)에 있는 결제 상태 필드를 여러 코어가 경합하면 MESI 프로토콜의 Invalid → Shared → Modified 전환이 반복되어 Cache Invalidation Storm 발생

#### OS Kernel 계층

- **Futex(Fast Userspace Mutex)**: `futex(2)` 시스템 콜 기반. 경합 없는 경우 커널 진입 없이 User Space에서 CAS로만 처리. 경합 발생 시에만 `futex_wait()`로 Kernel Wait Queue에 task_struct 등록
- **task_struct의 상태 전환**: Lock 획득 실패 시 해당 스레드의 task_struct 상태가 `TASK_RUNNING` → `TASK_INTERRUPTIBLE` (또는 `TASK_UNINTERRUPTIBLE`)로 전환 → CFS Runqueue에서 제거
- **Wait Queue**: `wait_queue_head_t` 구조체에 대기 중인 task_struct를 연결 리스트로 관리. Lock 해제 시 `wake_up()` 호출로 Runqueue 재등록
- **Priority Inversion**: 낮은 우선순위 스레드가 Lock을 보유한 채로 선점되면 높은 우선순위 스레드가 무기한 대기 → `PI_FUTEX`(Priority Inheritance Futex)로 완화 가능
- **Context Switch 비용**: Lock 대기로 인한 Context Switch는 TLB Flush, Cache 오염, 레지스터 저장/복원 비용 수반. `vmstat`의 `cs` 항목 증가로 관찰

#### JVM Runtime 계층

- **`synchronized` 내부 구조**: JVM Object Header의 Mark Word를 이용한 3단계 잠금. Biased Lock(단일 스레드) → Thin Lock(경합 낮음, CAS) → Inflated Lock(Heavy Monitor, OS Mutex 위임)
- **`java.util.concurrent.locks.ReentrantLock`**: AQS(AbstractQueuedSynchronizer) 기반. CAS로 `state` 필드를 원자적으로 변경. 실패 시 CLH Queue에 스레드 노드 등록 후 `LockSupport.park()` → Futex Wait
- **JIT 최적화와 Lock**: C2 JIT는 Lock Elision(잠금 제거), Lock Coarsening(잠금 병합) 최적화 수행. 단일 스레드 접근이 확인되면 `synchronized` 블록 자체를 제거 가능
- **Safepoint와 Lock**: GC Safepoint 도달 시 모든 Java 스레드가 Safepoint에 진입해야 함. Lock을 보유한 채 Safepoint에 진입한 스레드가 많을수록 GC STW(Stop-The-World) 지연 증가

#### SRE 관찰 도구

| 도구 | 관찰 대상 |
|---|---|
| `perf stat -e cache-misses,cache-references` | Cache Coherency 경합으로 인한 Miss 증가 |
| `vmstat 1 \| awk '{print $12}'` (cs 컬럼) | Context Switch 빈도 |
| `strace -e futex -p <pid>` | Futex 시스템 콜 발생 및 대기 상태 |
| JVM `-XX:+PrintSafepointStatistics` | Safepoint 진입 지연 (Lock 보유 스레드 수와 상관) |
| JVM `-XX:+PrintLockStatistics` (디버그 빌드) | Lock Inflation 및 경합 통계 |
| `perf c2c` | False Sharing 및 Cache Line 경합 감지 |

---

## SRE 관점에서 왜 중요한가?

Business Domain 계층의 Lock Contention은 단순 CPU 문제가 아니라 **서비스 신뢰성 문제**로 이어질 수 있습니다.

| 계층 | 영향 |
|---|---|
| Queue / Buffer 경합 | Throughput 저하 |
| DB Connection Pool 경합 | 응답 지연 |
| Payment 상태 경합 | 결제 실패 / 중복 승인 위험 |

> **어떤 공유 자원에서 Lock Contention이 발생했는가**에 따라 장애 심각도가 달라집니다.

### 계층별 Lock Contention의 전파 경로

#### Hardware 계층

- **Memory Bandwidth Saturation**: Lock 경합이 심화되면 모든 코어가 동일 Cache Line의 소유권을 요청하는 `MESI Invalidation` 메시지가 폭증 → QPI(QuickPath Interconnect) / UPI 버스 대역폭 포화
- **CPU Pipeline Stall**: `LOCK CMPXCHG` 명령이 Cache Miss와 겹치면 Memory Latency(~100ns)만큼 Pipeline Stall 발생 → IPC 급락
- **C-state 억제**: Lock Spin 대기 중인 스레드가 CPU를 점유한 채 바쁜 대기(Busy-Wait)를 수행하면 CPU가 C1/C2 저전력 상태로 진입하지 못해 전력 낭비 및 열 발생

#### OS Kernel 계층

- **Runqueue 불균형**: Lock 대기 스레드가 `TASK_UNINTERRUPTIBLE`로 전환되면 해당 코어의 Runqueue가 비어 Load Balancer가 과도한 스레드 이동 수행 → Cache Warm-Up 비용 반복 발생
- **Off-CPU Time 증가**: Lock 획득 실패로 스레드가 CPU에서 제거된 시간. `perf sched latency`와 eBPF off-cpu 분석으로 Lock 대기 시간 직접 측정 가능
- **cgroup CPU Throttling 연쇄**: Lock Contention으로 요청 처리가 지연되면 스레드 풀이 새 스레드를 추가 생성 → CPU 사용량 급증 → cgroup `cpu.max` Quota 소진 → Container Throttling 발생
- **OOM Killer 위험**: Lock 대기로 인한 요청 큐 적체 → 메모리 소비 증가 → `oom_score` 상승 → OOM Killer가 해당 컨테이너 프로세스 강제 종료

#### JVM Runtime 계층

- **Thread Pool Exhaustion**: Lock 대기로 스레드가 블로킹되면 `ThreadPoolExecutor`의 가용 스레드 고갈 → Rejection Policy 발동 (RejectedExecutionException)
- **Heap 압박**: 블로킹된 스레드의 스택 프레임과 요청 객체가 GC 수집 전까지 Heap을 점유 → Eden → Survivor → Old Gen 승격 가속 → Full GC 빈도 증가
- **Connection Pool Exhaustion**: DB Row Lock 대기 중인 스레드가 DB Connection을 보유한 채 대기 → HikariCP/DBCP의 Connection Pool 소진 → 후속 요청 전부 대기 또는 Timeout

#### Application 계층

- **Backpressure**: 처리 지연이 상위 서비스로 전파. 응답 대기 중인 요청이 큐에 적체되면 메모리 소비 증가 및 GC 압박
- **Circuit Breaker 발동**: Latency가 임계치를 초과하면 Resilience4j / Hystrix의 Circuit Breaker가 OPEN 상태로 전환 → 해당 서비스로의 요청 전면 차단
- **Retry Storm**: Timeout 후 클라이언트가 재시도 → Lock Contention 가중 → 추가 Timeout → Retry 반복의 악순환

#### SRE 관찰 도구

| 도구 | 관찰 대상 |
|---|---|
| `perf stat -e bus-cycles,cache-misses` | Cache Invalidation 폭증 징후 |
| `/proc/pressure/cpu` (PSI) | CPU 자원 부족으로 인한 Stall 압력 |
| `cat /sys/fs/cgroup/cpu.stat \| grep throttled` | cgroup Throttling 발생 여부 |
| `eBPF offcputime-bpfcc -p <pid>` | Off-CPU Time 분포 및 Lock 대기 시간 |
| `/proc/pressure/memory` | 메모리 압박 상태 (OOM 사전 감지) |
| JVM JFR + `jfr print --events ThreadPark` | Lock 대기(`LockSupport.park()`) 이벤트 |
| Prometheus `hikaricp_pending_threads` | DB Connection Pool 대기 스레드 수 |
| Application Metrics P99/P999 Latency | Lock Contention으로 인한 Latency Tail 증가 |

---

## Business Domain 락의 특징

이 계층의 Lock은 외부 API / DB Transaction / 결제 승인 / 정산 처리 등과 연결되는 경우가 많습니다.

따라서 **Lock 유지 시간이 상대적으로 길어질 수 있습니다.**

예:

```text
결제 승인 요청
→ PG 응답 대기
→ DB Commit 대기
→ 이 동안 동일 상태를 보호하기 위해 Lock 유지
```

이 경우:

```text
Lock Contention 증가
→ Request Queue 증가
→ Timeout 증가
→ 사용자 Latency 증가
```

### 계층별 Long-Held Lock의 동작 실체

#### Hardware 계층

- **Cache Line 장기 소유**: Lock 보유 스레드가 실행되는 코어가 해당 상태 변수의 Cache Line을 `Modified` 상태로 장기 보유. 다른 코어의 접근 시마다 `RFO(Request for Ownership)` 발생 → 버스 트래픽 증가
- **CPU Frequency Scaling**: Lock 대기로 CPU가 유휴 상태에 빠지면 P-state가 낮은 주파수로 전환. Lock 해제 후 최대 성능으로 복귀하는 데 P-state Ramp-up 지연(수 ms) 발생
- **NUMA 원격 접근**: Lock 보유 스레드와 대기 스레드가 서로 다른 NUMA 노드에서 실행될 경우, Lock 변수의 Cache Line이 원격 노드에 위치 → 접근 지연 2~4배 증가

#### OS Kernel 계층

- **`TASK_UNINTERRUPTIBLE` 장기 유지**: DB I/O 대기 중인 스레드는 `TASK_UNINTERRUPTIBLE`로 전환되어 Signal 수신도 불가. `D` 상태 스레드 증가는 I/O 또는 Lock 대기 병목의 직접 지표
- **Dirty Page Writeback 간섭**: DB Transaction의 Commit이 지연되면 InnoDB Buffer Pool의 Dirty Page가 누적 → `pdflush` / `kworker` 스레드의 Writeback 작업이 급증 → IO Scheduler(blk-mq) 큐 포화
- **blk-mq IO Scheduler**: DB Commit 시 WAL(Write-Ahead Log) fsync가 blk-mq 디스패치 큐에 삽입. 큐 포화 시 `io_schedule()` 호출로 추가 스레드가 Disk IO 대기 → D 상태 스레드 증가
- **Socket Buffer 대기**: PG(결제 대행사) 외부 API 대기 시 TCP 소켓이 `sk_buff` 수신을 기다리며 `TASK_INTERRUPTIBLE` 상태 유지. 네트워크 지연이 길어질수록 Lock 유지 시간 비례 증가

#### JVM Runtime 계층

- **Inflated Monitor 고착**: Lock 유지 시간이 길어지면 JVM이 Thin Lock에서 Inflated Monitor(Heavy Monitor)로 승격. Inflated 상태에서는 OS Mutex/Futex를 직접 사용하므로 커널 진입 비용이 매 Lock/Unlock 시마다 발생
- **JNI Critical Section**: 결제 SDK나 암호화 라이브러리가 JNI를 통해 네이티브 코드를 실행하는 경우, JNI Critical Section 내에서는 GC가 발생하지 않음. Lock 유지 중 JNI 호출이 포함되면 GC STW 지연 가중
- **Finalization Queue 누적**: Lock 대기 중 생성된 임시 객체들이 GC 수집 시 finalizer를 보유한 경우 Finalization Queue에 적체. ReferenceHandler 스레드가 소비하기 전까지 메모리 반환 지연
- **TLAB 소진 가속**: Lock 대기 중에도 HTTP Request 처리 프레임워크는 객체를 계속 할당. 스레드별 TLAB가 빠르게 소진되면 Eden 영역에서 직접 할당 시도 → Minor GC 빈도 증가

#### Application 계층

- **PG 응답 대기와 Timeout 설계**: PG API Timeout을 짧게 설정하면 Lock 해제 없이 예외 발생 → 상태 불일치. 길게 설정하면 Lock 점유 시간 증가 → Connection Pool 고갈
- **Distributed Lock TTL**: Redis `SETNX` 기반 Distributed Lock의 TTL이 실제 처리 시간보다 짧으면 Lock이 만료된 후 다른 노드가 동일 paymentId를 처리 → 중복 처리 위험
- **Serialization / Deserialization 비용**: PG 응답 JSON 파싱, DB 결과 매핑 등이 Lock 내부에서 수행되면 Deserialization 비용만큼 Lock 유지 시간 증가. 가능하면 Lock 외부로 이동 필요

#### SRE 관찰 도구

| 도구 | 관찰 대상 |
|---|---|
| `top` / `htop`에서 `D` 상태 스레드 | `TASK_UNINTERRUPTIBLE` 상태 (IO/Lock 대기) |
| `iostat -x 1` | blk-mq 큐 포화, await 시간 증가 |
| `cat /proc/sys/vm/dirty_ratio` + `vmstat`의 `bo` | Dirty Page Writeback 압박 |
| `ss -tp state established` | 외부 API 소켓 연결 상태 및 대기 |
| JVM `jstat -gcutil <pid> 1000` | Eden/Old GC 빈도 및 Lock 연관성 |
| JVM `-XX:+PrintGCDetails` + GC Log 분석 | Lock 기간 중 Minor/Full GC 발생 빈도 |
| Redis `MONITOR` + `SLOWLOG GET` | Distributed Lock 명령 지연 |
| `numastat -p <pid>` | NUMA 원격 접근으로 인한 Lock 변수 접근 지연 |

---

## 실무적으로 중요한 이유

결제/세션 계층의 동시성 문제는 성능 저하보다 **데이터 정합성(Data Consistency) 문제**가 더 치명적입니다.

CPU를 조금 더 쓰는 것보다 다음을 막는 것이 훨씬 중요합니다:

- 중복 결제
- 이중 승인
- 상태 불일치

특히 FinTech / Payment 환경에서는 다음이 최우선 원칙입니다:

```text
No Duplicate Payment
No Double Approval
No Inconsistent State
```

### 계층별 데이터 정합성 보호 메커니즘

#### Hardware 계층

- **원자적 명령어 보장**: `LOCK CMPXCHG`(x86), `LDREX/STREX`(ARM)는 하드웨어 수준에서 Read-Modify-Write를 원자적으로 수행. 중간에 다른 코어의 개입 불가
- **Memory Barrier**: `MFENCE`(Full Fence), `SFENCE`(Store Fence), `LFENCE`(Load Fence)로 Store/Load 재배치를 억제. JVM의 `volatile` 키워드가 최종적으로 이 명령들로 컴파일됨
- **Cache Coherency Protocol(MESI)**: Modified-Exclusive-Shared-Invalid 4상태로 멀티코어 간 데이터 일관성 강제. 쓰기 직전 다른 코어의 Cache Line을 Invalid로 전환하여 stale 읽기 방지

#### OS Kernel 계층

- **Spinlock vs Mutex 선택**: 커널 내부의 짧은 임계 구역은 Spinlock 사용(Context Switch 없음, 단 CPU 점유). 긴 임계 구역은 Mutex 사용(Context Switch 허용, CPU 반환). Payment 처리처럼 외부 IO가 포함된 경우 반드시 Mutex/Semaphore 계열 사용
- **RCU(Read-Copy-Update)**: 읽기 빈도가 쓰기보다 압도적으로 높은 Session 조회 등에 유효. 읽기는 완전 Lock-Free, 쓰기는 복사본 수정 후 포인터 원자 교체
- **POSIX Semaphore**: 프로세스 간 결제 상태 동기화가 필요한 경우 `sem_wait()` / `sem_post()` 사용. `/dev/shm` 기반 Named Semaphore로 멀티프로세스 간 공유 가능

#### JVM Runtime 계층

- **`volatile`의 실제 보장 범위**: JMM(Java Memory Model)에서 `volatile`은 가시성(Visibility)과 순서(Happens-Before) 보장. 복합 연산(read-modify-write)은 원자성 미보장 → `AtomicLong`, `AtomicReference` 필요
- **`StampedLock`의 낙관적 읽기**: Balance 조회처럼 읽기가 빈번한 경우 `StampedLock.tryOptimisticRead()`로 Lock 없이 읽기 시도. 쓰기 발생 시 stamp 무효화 감지 후 재시도
- **Idempotency Key**: 결제 요청에 고유 Idempotency Key를 부여하고 JVM 캐시(ConcurrentHashMap) 또는 Redis에 저장. 동일 요청 재시도 시 중복 처리를 Lock 없이 차단

#### Application 계층

- **DB Optimistic Lock**: JPA `@Version` 필드 기반. 커밋 시점에 버전 불일치 감지 → `OptimisticLockException` 발생 → 재시도 또는 실패 처리. Lock 유지 없이 정합성 보장 가능
- **DB Pessimistic Lock**: `SELECT FOR UPDATE`로 Row Lock 획득. 결제 상태 변경처럼 충돌 가능성이 높은 경우 사용. Lock 유지 중 다른 트랜잭션은 대기
- **Distributed Lock (Redis SETNX + Lua)**: 멀티 인스턴스 환경에서 동일 paymentId에 대한 중복 처리 방지. Lua 스크립트로 `SETNX + EXPIRE`를 원자적으로 실행하여 TTL 설정 누락 방지
- **Outbox Pattern**: 결제 상태 변경과 이벤트 발행을 동일 DB 트랜잭션으로 묶어 정합성 보장. 이벤트 유실 없이 외부 시스템에 상태 전파

#### SRE 관찰 도구

| 도구 | 관찰 대상 |
|---|---|
| `perf stat -e cache-misses,bus-cycles` | Cache Coherency 위반 빈도 |
| DB `SHOW ENGINE INNODB STATUS` | Lock Wait 현황, Deadlock 감지 |
| Redis `DEBUG SLEEP` + `SLOWLOG` | Distributed Lock 응답 지연 |
| JVM `-XX:+PrintGCApplicationStoppedTime` | GC STW 중 Lock 보유 상태 영향 |
| Application `OptimisticLockException` 발생 횟수 | 충돌 빈도 및 재시도 전략 유효성 |
| Prometheus `payment_duplicate_attempts_total` | 중복 결제 시도 횟수 (비즈니스 지표) |

---

## 핵심 요약

> Business Domain 계층의 Lock은 단순 성능 제어가 아니라,
> Session / Payment 상태의 **무결성과 신뢰성을 보호하기 위한 동시성 제어 메커니즘**입니다.

### 계층별 핵심 정리

| 계층 | 핵심 메커니즘 | 주요 위험 | SRE 관찰 지표 |
|---|---|---|---|
| **Hardware** | MESI Cache Coherency, LOCK CMPXCHG, Memory Barrier | Cache Line Thrashing, Memory Bandwidth Saturation | `cache-misses`, `bus-cycles` |
| **OS Kernel** | Futex, task_struct 상태 전환, Runqueue, blk-mq | Off-CPU Time 증가, D 상태 스레드, OOM | `futex` syscall, `/proc/pressure/*`, `D` 상태 |
| **JVM Runtime** | Inflated Monitor, AQS, JNI Critical Section, TLAB | Deoptimization, GC STW 지연, Thread Pool 고갈 | JFR ThreadPark, GC Log, `-XX:+PrintCompilation` |
| **Application** | DB Row Lock, Distributed Lock, Optimistic Lock | 중복 결제, Deadlock, Connection Pool 고갈 | `SHOW INNODB STATUS`, Redis SLOWLOG, P99 Latency |

```text
Lock 획득 실패
  → Off-CPU Time 증가 (OS Kernel: task_struct TASK_INTERRUPTIBLE)
  → Context Switch 비용 (TLB Flush, Cache Warm 손실)
  → Thread Pool 점진적 고갈 (JVM: ThreadPoolExecutor)
  → Connection Pool 고갈 (Application: HikariCP)
  → Request Queue 적체 → Backpressure 전파
  → Circuit Breaker OPEN → 서비스 부분 중단
  → Retry Storm → Lock Contention 재가중
```

이 전파 경로는 단순 성능 저하가 아니라 **결제 실패 / 서비스 불가(Outage)** 로 직결될 수 있으며, Hardware 레벨의 Cache 경합에서 시작해 Business Domain의 데이터 정합성 붕괴까지 이어지는 전 계층 장애 시나리오입니다.

---

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*