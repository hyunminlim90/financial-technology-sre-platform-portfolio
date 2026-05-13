# CPU 동기화에서 "실행 순서와 접근 제어"의 의미 (E2E 분석 적용됨)

## CPU 동기화에서 실행 순서가 중요한 이유

CPU 동기화 기술은 단순히 **"한 번에 한 스레드만 접근하게 막는 기술"** 이 아닙니다.

진짜 핵심은:

```text
공유 메모리에 대한 실행 순서와 시간적 선후 관계를 강제하는 것
```

즉 동기화는 다음을 제어합니다:

- 누가 먼저 실행되는가
- 어떤 작업이 먼저 메모리에 반영되는가
- 어떤 결과를 다른 스레드가 언제 볼 수 있는가

---

## 1. 실행 순서(Execution Order)란 무엇인가?

멀티스레드 환경에서 Thread A / B / C가 동시에 공유 메모리(Heap / RAM)를 수정하면 데이터가 꼬일 수 있습니다.

```java
count++;
```

이 연산은 실제로 3단계 작업입니다:

```text
1. 값 읽기  (LOAD)
2. 계산     (ADD)
3. 저장     (STORE)
```

여러 스레드가 동시에 실행되면:

```text
A 읽기 → 5
B 읽기 → 5
A 저장 → 6
B 저장 → 6   ← 최종 결과가 7이 아니라 6
```

**실행 순서가 섞인 것**이 문제의 본질입니다.

### 계층별 메커니즘 실체

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **CPU Pipeline** | Hardware | Superscalar 실행, Out-of-Order Execution, ROB(Reorder Buffer) 내 명령어 재배치 | `perf stat -e instructions,cycles`, `perf annotate` |
| **CPU Cache** | Hardware | L1/L2/L3 캐시 계층에서 `count` 값 복사본 유지, Cache Line(64바이트) 단위 읽기/쓰기 | `perf stat -e cache-misses,cache-references` |
| **NUMA** | Hardware + Kernel | 멀티소켓 환경에서 Thread A(Node 0)와 Thread B(Node 1)가 서로 다른 물리 메모리를 바라볼 수 있음 | `numactl --hardware`, `numastat` |
| **OS 스케줄러** | Kernel (CFS) | `task_struct`의 `vruntime` 기반 Red-Black Tree 스케줄링, Context Switch로 실행 순서 결정 | `/proc/schedstat`, `perf sched`, `pidstat -w`, `vmstat cs` 항목 |
| **JVM TLAB** | Runtime (JVM) | Thread-Local Allocation Buffer에서 `count` 객체 로컬 할당 → Heap 반영 시점이 스레드마다 다름 | `-XX:+PrintTLAB`, `jstat -gc`, `async-profiler` |
| **JIT 컴파일** | Runtime (JVM) | C1/C2 컴파일러가 `count++` 루프를 레지스터 캐시 최적화로 변환 → 실제 메모리 반영 지연 | `-XX:+PrintCompilation`, `jitwatch`, `perf record -g` |

---

## 2. 동기화가 제어하는 두 가지 실행 순서

### 2-1. 상호 배제(Mutual Exclusion)에 의한 순서

여러 스레드가 동시에 실행되더라도 **임계 영역(Critical Section)** 만큼은 반드시 한 줄로 서서 순차 실행되도록 강제합니다.

```text
Thread A → Lock 획득
Thread B → 대기
Thread C → 대기
```

결과적으로 다음 순서가 만들어집니다:

```text
A 작업 완료
    ↓
B 작업 시작
    ↓
C 작업 시작
```

> 이 순서를 강제하는 목적은 **Race Condition 방지**입니다.

### 계층별 메커니즘 실체 — 상호 배제

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **CPU Atomic** | Hardware | `LOCK CMPXCHG` 명령어, `MFENCE` / `LFENCE` / `SFENCE` 명령어로 Bus Lock 또는 Cache Lock 수행 | `perf stat -e bus-cycles`, `objdump -d` |
| **Futex** | Kernel + App | Fast Userspace Mutex: 경합 없을 시 User Space에서 CAS(Compare-And-Swap)로 즉시 처리, 경합 시에만 `futex()` 시스템 콜 → Kernel Wait Queue에 등록 | `strace -e futex`, `perf trace -e futex`, `/proc/PID/syscall` |
| **Spinlock** | Kernel | Kernel 내부 임계 영역 보호. `spin_lock()`은 Context Switch 없이 CPU를 바쁜 대기(Busy Wait)로 점유 → 짧은 임계 영역에서만 적합 | `perf stat -e cpu-clock` (스핀 중 CPU 사용률 높음), `lockstat` |
| **Mutex (POSIX)** | OS / Kernel | `pthread_mutex_lock()` → `futex(FUTEX_WAIT)` → Kernel이 `task_struct`를 Wait Queue에 삽입 → Unlock 시 `futex(FUTEX_WAKE)` → CFS 스케줄러가 재스케줄 | `strace -e futex,clock_nanosleep`, `pstack`, `jstack` |
| **Java synchronized** | JVM + OS | Biased Locking → Thin Lock(CAS) → Inflated Lock(OS Mutex) 3단계 에스컬레이션. `monitorenter` / `monitorexit` 바이트코드 | `-XX:+PrintBiasedLockingStatistics`, `jstack` (BLOCKED 스레드) |
| **Java ReentrantLock** | JVM (AQS) | `AbstractQueuedSynchronizer`의 CLH Queue 기반 공정/비공정 Lock. Condition Queue로 `wait()`/`signal()` 구현 | `jstack` (parking 스레드 확인), `async-profiler` |
| **Off-CPU Time** | Kernel | Lock 대기 중 스레드는 CPU를 점유하지 않음 → On-CPU 프로파일링에 보이지 않는 병목. Off-CPU 분석 필수 | `offcputime-bpfcc`, `async-profiler -e wall`, `bpftrace` |

### 2-2. 메모리 가시성과 Happens-Before 순서

**CPU와 컴파일러의 최적화 문제:**

CPU와 컴파일러는 성능 향상을 위해 **Instruction Reordering(명령어 재정렬)** 을 수행합니다.

```java
data = new Object();
ready = true;
```

개발자의 기대 순서:
```text
1. data 생성
2. ready = true
```

CPU가 최적화하면:
```text
1. ready = true   ← 순서 역전
2. data 생성
```

그 결과 다른 Thread가 `ready=true`를 먼저 보고 아직 생성되지 않은 `data`에 접근하는 상황이 발생할 수 있습니다.

### 계층별 메커니즘 실체 — 메모리 가시성

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **CPU Out-of-Order** | Hardware | Store Buffer / Load Buffer에 의해 쓰기 순서가 다른 코어에서 다르게 관찰될 수 있음 (x86: TSO, ARM: Weak Ordering) | `perf stat -e mem-loads,mem-stores`, `perf mem` |
| **Cache Coherency** | Hardware | MESI 프로토콜: Modified/Exclusive/Shared/Invalid 상태 전이로 L1 캐시 간 일관성 유지. `ready=true` 쓰기가 다른 코어의 캐시에 Invalidation 전파 | `perf stat -e cache-misses`, `perf c2c` (Cache-to-Cache 전송 분석) |
| **Cache Line Thrashing** | Hardware | 두 스레드가 같은 Cache Line(64바이트) 내 다른 변수를 동시에 쓸 때 False Sharing 발생 → 과도한 Coherency Traffic | `perf c2c`, `perf stat -e LLC-load-misses` |
| **컴파일러 재정렬** | Runtime (JIT/GCC) | JVM C2 컴파일러, GCC `-O2` 이상: Dead Store Elimination, Loop Hoisting으로 메모리 쓰기 순서 변경 | `-XX:+PrintOptoAssembly`, `objdump -d` |
| **Java volatile** | JVM | `volatile` 필드 접근 시 `StoreLoad` Memory Barrier 삽입 → Store Buffer Flush 강제 | `javap -c` (getstatic/putstatic 확인), JITwatch |
| **JNI Critical Section** | JVM + Native | `GetPrimitiveArrayCritical()` 호출 중 GC Safepoint 진입 불가 → GC 지연 및 다른 스레드 Hang | `-XX:+PrintSafepointStatistics`, `async-profiler -e cpu` |

---

## 3. Happens-Before 관계

동기화 기술은 이런 문제를 막기 위해 **Happens-Before** 관계를 강제합니다.

```text
"A 작업은 반드시 B 작업보다 먼저 메모리에 반영되어야 한다"
```

**예시:**

```text
Thread A              Thread B
1. 데이터 쓰기
2. Lock 해제
                      1. Lock 획득
                      2. 데이터 읽기
```

동기화 메커니즘은 다음 순서를 절대적으로 보장합니다:

```text
[데이터 쓰기]
     ↓
[Lock 해제]
     ↓
[Lock 획득]
     ↓
[데이터 읽기]
```

### 계층별 메커니즘 실체 — Happens-Before

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **Memory Barrier (HW)** | Hardware | `MFENCE`: Store+Load 양방향 Barrier. `SFENCE`: Store 순서 강제. `LFENCE`: Load 순서 강제. Lock 해제 시 암묵적 `MFENCE` 포함 | `objdump -d` (mfence 명령어 확인), `perf stat -e mem-stores` |
| **Kernel Mutex / Unlock** | Kernel | `mutex_unlock()` 내부에서 `smp_mb__before_atomic()` 호출 → 메모리 배리어 삽입 후 Wait Queue의 다음 `task_struct` Wake-up | `ftrace`, `perf trace`, `trace-cmd record -e lock:*` |
| **Futex Happens-Before** | Kernel | `FUTEX_WAKE` 호출 시 Kernel이 Wait Queue에서 `task_struct` 꺼내 CFS Runqueue에 enqueue → 다음 스케줄링 틱에 실행 | `strace -e futex`, `/proc/PID/wchan` (대기 상태 확인) |
| **Java Monitor** | JVM | `monitorexit` 바이트코드 → JVM이 `StoreLoad` Barrier 삽입 → `monitorenter` 이후 모든 읽기는 최신 값 관찰 보장 | `jstack` (BLOCKED/WAITING 스레드), `-XX:+PrintSafepointStatistics` |
| **Java volatile Happens-Before** | JVM | `volatile write` → `volatile read` 간 Happens-Before. JMM(Java Memory Model) 명세에 의해 보장 | `javap -c` (putfield/getfield에 Barrier 확인), JITwatch |
| **Safepoint** | JVM | JVM이 GC / Deoptimization 등을 위해 모든 스레드를 안전한 실행 지점(Safepoint)에서 멈춤. Happens-Before 맥락에서 Safepoint 자체가 전역 메모리 동기화 지점 역할 | `-XX:+PrintSafepointStatistics`, `-XX:+LogVMOutput`, `async-profiler` |

---

## 4. 메모리 배리어(Memory Barrier)

Happens-Before 관계를 실제 CPU 수준에서 강제하는 것이 **Memory Barrier / Memory Fence**입니다.

```text
CPU에게 "이 순서는 절대로 바꾸지 마라"
라고 명령하는 하드웨어 레벨 동기화 장치
```

### 계층별 메커니즘 실체 — Memory Barrier

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **x86 TSO** | Hardware | x86 아키텍처는 Total Store Order 메모리 모델 적용. Store→Load 재정렬만 허용, 나머지는 암묵적 보장. `LOCK` prefix가 Full Barrier 역할 | `objdump -d` (lock xchg, lock cmpxchg 확인) |
| **ARM Weak Order** | Hardware | ARM은 Relaxed 메모리 모델. `DMB ISH`(Data Memory Barrier Inner Shareable) 명령어로 명시적 배리어 삽입 필요. JVM이 ARM 타겟 시 자동 삽입 | `perf stat` on ARM 인스턴스, `aarch64` 바이너리 `objdump` |
| **Store Buffer Flush** | Hardware | CPU Store Buffer에 누적된 쓰기를 L1 캐시에 강제로 반영. `SFENCE` 또는 `MFENCE`로 트리거 | `perf mem -t store`, `perf c2c` |
| **CPU Pipeline Stall** | Hardware | Memory Barrier 실행 중 파이프라인 후속 명령어 실행 중단 → IPC(Instructions Per Cycle) 저하 | `perf stat -e stalled-cycles-frontend,stalled-cycles-backend` |
| **Branch Misprediction** | Hardware | 동기화 코드 주변의 조건 분기(Lock 보유 여부 체크 등)에서 예측 실패 시 파이프라인 플러시 → 수십 사이클 낭비 | `perf stat -e branch-misses,branch-instructions` |
| **JVM Barrier 삽입** | JVM (JIT) | JVM은 플랫폼별로 적절한 Barrier 명령어 선택: x86은 `LOCK ADDL`, AArch64는 `DMB ISH`. `volatile` 필드 / `synchronized` 블록 경계에 자동 삽입 | `-XX:+PrintOptoAssembly`, JITwatch (어셈블리 뷰) |
| **Memory Bandwidth Saturation** | Hardware | 과도한 Barrier + Cache Miss 조합 시 메모리 버스 대역폭 포화 → 전체 스레드 처리량 급감 | `perf stat -e mem-loads,mem-stores`, `numastat -m`, Intel VTune Memory Access |

---

## 5. 락 획득 전후의 차이

### 락 획득 전 — 경합(Competition) 상태

Thread A / B / C가 동시에 Lock을 요청하면 OS Scheduler / CPU Timing / Interrupt 등에 따라 먼저 획득하는 스레드가 달라질 수 있습니다.

```text
락 획득 전까지는 경합(Contention) 상태
```

### 계층별 메커니즘 실체 — 락 획득 전

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **CPU 타이밍 경합** | Hardware | TSC(Time Stamp Counter) 기반 실행 타이밍, IRQ 인터럽트 발생 시점에 따라 CAS 성공 스레드가 달라짐 | `perf stat -e context-switches`, `/proc/interrupts` |
| **IRQ 영향** | Hardware → Kernel | NIC 수신 인터럽트, Timer IRQ가 Lock 시도 직전 발생하면 해당 CPU 점유 → 다른 스레드가 먼저 Lock 획득 가능 | `/proc/interrupts`, `mpstat -I ALL`, `perf stat -e irq:*` |
| **SoftIRQ 영향** | Kernel (후처리) | 네트워크 트래픽 폭증 시 `ksoftirqd` 가 CPU를 장시간 점유 → Lock 경합 스레드 스케줄 지연 | `mpstat`의 `%soft` 항목, `/proc/softirqs`, `sar -I ALL` |
| **CFS 스케줄러** | Kernel | 경합 스레드들의 `vruntime` 비교하여 가장 작은 값의 `task_struct`를 Runqueue에서 선택 | `/proc/schedstat`, `perf sched latency`, `pidstat -w` |
| **Connection Pool Exhaustion** | App | 애플리케이션 레벨의 DB Connection Pool / Thread Pool 자원 고갈 → 스레드가 Pool 잠금 대기 → Lock 경합 가중 | `jstack` (WAITING on pool lock), Micrometer/Prometheus `hikaricp.connections.pending` |
| **CPU Frequency Scaling** | Hardware + Kernel | C-state(절전) 상태의 CPU가 Lock 신호 수신 후 P-state(고성능) 전환 지연 → 경합 해소 지연 | `cpupower frequency-info`, `turbostat`, `/sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq` |

### 락 획득 후 — 엄격한 순서 강제

일단 Lock을 획득한 순간부터는 엄격한 실행 순서가 강제됩니다:

```text
A 작업 완료
    ↓
B 작업 시작   ← 절대적인 선후 관계 형성
```

이것이 **Synchronization의 본질**입니다.

### 계층별 메커니즘 실체 — 락 획득 후

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **Context Switch** | Kernel | Lock 해제 → Wake-up → CFS Runqueue enqueue → Context Switch 발생 시 레지스터 저장(`task_struct`의 `thread` 필드), TLB Flush(프로세스 간), 캐시 워밍업 비용 발생 | `vmstat cs`, `pidstat -w`, `perf sched switch` |
| **Critical Section 직렬화** | OS / Kernel | 임계 영역 내 실행은 완전히 직렬화됨. 병렬성 상실 → 암달의 법칙에 따라 전체 처리량 상한선 결정 | `perf lock`, `lockstat`, `BCC lockstat` |
| **TLAB 동기화** | JVM | 임계 영역 내 객체 할당 시 TLAB 잔량 부족하면 새 TLAB 할당 → Heap Lock 경합 → GC 압박 | `-XX:+PrintTLAB`, `jstat -gc`, `GC 로그 (-Xlog:gc*)` |
| **Finalization Queue** | JVM | 임계 영역에서 참조 해제된 객체가 `finalize()` 구현 시 Finalizer Queue에 적재 → 별도 Finalizer 스레드가 순서대로 처리 → GC 지연 | `jstat -gcutil`, `-XX:+PrintGCDetails`, `jmap -histo` |
| **Direct Memory / Off-Heap** | JVM + OS | 임계 영역 내 `ByteBuffer.allocateDirect()` 사용 시 `mmap()` 시스템 콜 → Kernel 가상 주소 공간 매핑. Lock 해제 후 다른 스레드가 동일 Off-Heap 영역 접근 | `perf trace -e mmap`, `/proc/PID/maps`, `vmstat pgfault` |

---

## 6. 동기화의 핵심 목표 — 결정성(Determinism)

| 상태 | 설명 |
|------|------|
| **동기화 없음** | 실행 결과가 매번 달라짐 → Non-Deterministic |
| **동기화 있음** | 누가 먼저 실행될지는 몰라도 최종 결과는 항상 동일 → Deterministic |

### 계층별 메커니즘 실체 — Determinism 보장 구조

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **Atomic CAS** | Hardware | `LOCK CMPXCHG` 명령어: 비교-교환을 단일 원자 연산으로 수행. Non-Deterministic 경합을 Deterministic 직렬 실행으로 변환 | `perf stat -e instructions`, `objdump -d` |
| **Kernel Wait Queue** | Kernel | 경합 실패 스레드를 `wait_queue_head_t`에 FIFO 또는 Priority 순으로 등록 → Wake-up 순서 결정성 확보 | `ftrace (function_graph)`, `bpftrace -e 'kfunc:__wake_up*'` |
| **THP (Transparent HugePage)** | Kernel | 2MB HugePage로 TLB 엔트리 수 감소 → 동기화 코드 실행 중 TLB Miss로 인한 불확실한 지연 완화 → 더 예측 가능한 레이턴시 | `/proc/meminfo`의 `AnonHugePages`, `cat /sys/kernel/mm/transparent_hugepage/enabled`, `perf stat -e dTLB-load-misses` |
| **HugePage TLB** | Hardware + Kernel | 4KB Page → 2MB HugePage 전환 시 TLB 커버리지 512배 증가 → Page Walk 빈도 감소 → 동기화 코드 레이턴시 안정화 | `perf stat -e dTLB-load-misses,dTLB-store-misses`, `/proc/PID/smaps`의 `THPeligible` |
| **JVM C2 최적화 제한** | JVM | Determinism 보장을 위해 `synchronized` / `volatile` 코드에서 C2 컴파일러의 Lock Coarsening / Elision 적용 여부 결정 | `-XX:+PrintInlining`, `-XX:+PrintEliminateLocks`, JITwatch |
| **ClassLoader Leak** | JVM | 동기화 코드에서 클래스 로딩 발생 시 `ClassLoader` 참조가 누수되면 Metaspace 증가 → `OutOfMemoryError: Metaspace` → 비결정적 장애 | `jmap -clstats`, `-XX:+TraceClassLoading`, `jstat -gc`의 `M` 컬럼 |

---

## 7. SRE 관점에서의 실행 순서

로그(Log) / 트레이스(Trace) / 이벤트(Event)들이 바로 실행 순서의 기록입니다.

**문제 상황:**

```text
결제 요청(A)이 먼저 들어왔는데
취소 요청(B)이 먼저 처리됨
→ 데이터 정합성 오류 발생
```

**원인:**

- 동기화 실패
- 메시지 순서 역전
- 메모리 가시성 문제
- 비동기 처리 순서 오류

→ **논리적인 실행 순서가 깨진 것**

### 계층별 메커니즘 실체 — SRE 장애 분석

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **Socket Buffer / sk_buff** | Kernel | 네트워크 패킷이 NIC에서 수신되면 `sk_buff` 구조체로 Kernel 소켓 수신 버퍼에 적재. 결제/취소 메시지가 서로 다른 패킷으로 도착 시 버퍼 처리 순서가 IRQ 타이밍에 의존 | `ss -tmn` (수신 버퍼 크기), `netstat -s`, `tcpdump` |
| **TCP Backlog / SYN Queue** | Kernel | 동시 연결 폭증 시 `SYN Queue`(미완성 연결) 또는 `Accept Queue`(완성 연결) 오버플로우 → 연결 요청 드롭 또는 재전송 → 요청 도달 순서 역전 | `ss -lnt` (Recv-Q/Send-Q), `/proc/net/tcp`, `netstat -s | grep "SYNs to LISTEN"` |
| **RPS / RFS** | Kernel | Receive Packet Steering / Receive Flow Steering: NIC 하드웨어 큐에서 소프트웨어 큐로 패킷을 CPU 코어에 분산. 잘못된 설정 시 동일 Flow 패킷이 다른 CPU에서 처리 → 순서 역전 | `/sys/class/net/eth0/queues/rx-*/rps_cpus`, `ethtool -S`, `perf stat -e softirq:*` |
| **IO Scheduler (blk-mq)** | Kernel | 디스크 I/O 요청이 `blk-mq`의 멀티 Queue에 분산 처리 → 결제 로그와 취소 로그가 다른 Queue에서 처리될 시 디스크 반영 순서 역전 가능 | `iostat -x`, `cat /sys/block/sda/queue/scheduler`, `blktrace`, `bpftrace -e 'kprobe:blk_mq_submit_bio*'` |
| **Dirty Page Writeback** | Kernel | Kernel Page Cache의 더티 페이지가 비동기 `pdflush` / `writeback` 스레드로 디스크에 반영. 시스템 메모리 압박 시 Writeback이 지연 → 결제 상태 영속화 지연 | `/proc/vmstat`의 `nr_dirty`, `nr_writeback`, `iotop`, `vmstat -d` |
| **Backpressure** | App | 메시지 큐 소비자가 처리 속도를 따라가지 못할 때 생산자에게 압력을 전달하지 않으면 큐가 쌓이며 처리 순서 지연 발생. Reactive Streams의 `request(n)` 메커니즘 | Kafka `consumer_lag`, RabbitMQ `messages_unacknowledged`, Grafana queue depth 그래프 |
| **Circuit Breaker** | App | 다운스트림 서비스 장애 시 Circuit Breaker Open → 결제 요청은 즉시 실패, 취소 요청은 재시도 → 처리 순서 역전 | Resilience4j `circuitbreaker.state`, `circuitbreaker.buffered-calls`, Prometheus `resilience4j_*` |
| **Retry Storm** | App | 타임아웃 발생 시 다수 클라이언트가 동시에 재시도 → 서버 동기화 코드 Lock 경합 폭증 → 처리 순서 완전 비결정적 | `jstack` (BLOCKED 스레드 수), Prometheus `http_server_requests_seconds_count`, `rate()` 함수 |
| **Serialization / Deserialization 비용** | App | JSON/Protobuf 직렬화 중 GC Pause 또는 CPU 포화 발생 시 메시지 처리 지연 → 늦게 직렬화 완료된 요청이 먼저 처리된 요청보다 Lock을 늦게 획득 | `async-profiler -e cpu` (직렬화 스택 확인), `jstat -gcutil`, `perf stat -e cache-misses` |
| **OOM Killer** | Kernel | 메모리 압박 시 Kernel이 `oom_score`가 높은 프로세스를 강제 종료 → 결제 서비스 프로세스 종료 → 미처리 요청 유실 및 순서 파괴 | `dmesg | grep -i "oom"`, `/proc/PID/oom_score`, `journalctl -k | grep oom` |

---

## 핵심 요약

CPU 동기화에서 **"실행 순서를 제어한다"** 는 의미는 단순히 "한 명씩 들어가게 한다"를 넘어서:

```text
1. 공유 메모리 접근을 순차화하고
2. 시간적 선후 관계를 보장하며
3. 메모리 반영 순서를 강제하고
4. 결과를 결정적 상태로 유지하는 것
```

을 의미하며, 이를 실현하는 실제 메커니즘은 Hardware부터 Application까지 전 계층에 걸쳐 있습니다.

### 전체 계층 요약

| 계층 | 핵심 메커니즘 | 장애 시 증상 | 주요 관찰 도구 |
|------|-------------|------------|--------------|
| **Hardware** | Cache Line, MESI, Store Buffer, Pipeline, Branch Predictor, NUMA | Cache Miss 급증, IPC 저하, 메모리 대역폭 포화 | `perf stat`, `perf c2c`, `numastat` |
| **OS Kernel** | Futex, Spinlock, IRQ, SoftIRQ, CFS 스케줄러, Wait Queue, blk-mq, OOM Killer | Context Switch 폭증, %soft 증가, Run Queue 적체 | `vmstat`, `mpstat`, `perf sched`, `ftrace` |
| **Runtime (JVM)** | JIT C1/C2, TLAB, Safepoint, Biased Lock, AQS, Finalizer, ClassLoader | BLOCKED 스레드 증가, GC Pause, Metaspace 누수 | `jstack`, `async-profiler`, `jstat`, JITwatch |
| **Application** | Connection Pool, Backpressure, Circuit Breaker, Retry Storm, 직렬화 | 요청 순서 역전, 처리 지연 급증, 장애 전파 | Prometheus, Grafana, Kafka lag, tracing |

> **동기화의 본질은 혼란스러운 동시 실행을 논리적인 시간 순서로 변환하는 것이며,
> 그 보장은 CPU 마이크로아키텍처에서 애플리케이션 프레임워크까지 모든 계층의 협력으로 이루어집니다.**

---

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*