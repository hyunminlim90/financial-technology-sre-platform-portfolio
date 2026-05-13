# CPU Saturation과 I/O 처리 구조

## 1. 개요

CPU Saturation은 단순히 CPU 사용률이 높은 상태만을 의미하지 않는다.  
CPU가 처리할 수 있는 실행 능력보다 더 많은 `task_struct`가 CPU 실행을 요구하면서, Runnable Queue 대기, Context Switch 증가, Scheduler Overhead 증가, Cache Miss 증가가 함께 발생하는 상태를 의미한다.

| 구분 | 의미 |
|------|------|
| CPU Utilization | CPU가 특정 시간 동안 얼마나 사용되었는지 나타내는 비율 |
| CPU Saturation | CPU 실행 능력보다 더 많은 작업이 대기하면서 지연이 발생하는 상태 |

CPU 사용률이 100%에 도달하지 않아도 Runnable Queue가 지속적으로 증가하거나 CPU Pressure가 높다면 Saturation 상태로 볼 수 있다.

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Hardware | CPU Core의 물리적 실행 단위(ALU, FPU) 포화. Hyper-Threading 환경에서 논리 Core 수와 물리 Core 수 차이로 인한 실제 처리 능력 과대평가 가능 | `lscpu` (Thread(s) per core), `perf stat -e cpu-clock` |
| Kernel (Scheduler) | CFS가 관리하는 Runnable Queue(`rq`)에 `task_struct` 적체. `nr_running` 값이 CPU 수를 지속적으로 초과하는 상태 | `vmstat 1` (r 컬럼 = Runnable task 수), `/proc/schedstat` |
| Kernel (PSI) | PSI(Pressure Stall Information)가 CPU `some` / `full` 값 상승으로 Saturation 신호 발생 | `/proc/pressure/cpu` (`some`, `full` avg10/avg60/avg300) |
| Kernel (cgroup) | Kubernetes CPU Limit → cgroup v2 `cpu.max` (Quota/Period). Quota 소진 시 Container 내 모든 `task_struct` Throttle Queue로 이동 → CPU 미사용 상태에서도 Latency 급증 | `/sys/fs/cgroup/cpu.stat` (`throttled_usec`), `cadvisor` `container_cpu_cfs_throttled_seconds_total` |
| JVM | GC STW, JIT Safepoint, Biased Lock Revocation 등에서 모든 Worker Thread 일시 정지. CPU 사용률 급감 후 재개 시 Runnable Queue 순간 폭발 | `-Xlog:safepoint`, JFR `jdk.SafepointBegin/End`, GC log STW 시간 |

---

## 2. CPU Saturation 핵심 연쇄 구조

```
Thread 증가  →  task_struct 증가
  ↓ Runnable Queue 증가
  ↓ Context Switch 증가  →  Cache Miss 증가 (L1/L2/TLB Flush)
  ↓ Scheduler Overhead 증가  →  System CPU Time(sy) 증가
  ↓ CPU Pipeline Stall 증가  →  IPC(Instructions Per Cycle) 감소
  ↓ CPU Saturation  →  Throughput 감소 / Latency 증가
```

CPU가 바쁘게 동작하지만 실제 비즈니스 로직 처리량은 오히려 감소하는 상태다.

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Hardware (Pipeline Stall) | Context Switch 후 CPU Pipeline이 비워지고(flush) 새 `task_struct`의 명령어로 채워질 때까지 Stall 발생. Branch Misprediction도 Pipeline Stall 유발 → IPC 감소 | `perf stat -e stalled-cycles-frontend,stalled-cycles-backend`, `toplev` |
| Hardware (Cache) | Context Switch 시 L1/L2 Cache에 남은 이전 `task_struct` 데이터가 새 `task_struct`에 의해 교체됨 → Cache Cold Start. L3는 공유되나 동시 경쟁 시 Cache Line Thrashing 발생 | `perf stat -e cache-misses,cache-references,L1-dcache-load-misses` |
| Hardware (TLB) | Context Switch 시 TLB Flush 발생 → 이후 메모리 접근마다 Page Table Walk 필요. Thread 수 증가 → TLB Miss 빈도 증가 → Memory Latency 증가 | `perf stat -e dTLB-load-misses,dTLB-store-misses` |
| Kernel (CFS) | `task_struct` 수 증가 → CFS Red-Black Tree 크기 증가 → `vruntime` 갱신 및 탐색 비용 증가 → `sy` CPU 시간 증가 | `vmstat 1` (sy 컬럼), `pidstat -u 1` (각 프로세스 sy 비율) |
| Kernel (NUMA) | Thread가 NUMA Node 0에서 생성되었으나 메모리가 Node 1에 할당된 경우 Remote Memory Access(~100ns) 발생. Saturation 상태에서는 NUMA 불균형이 Latency를 더욱 증폭 | `numastat -p <pid>`, `numactl --hardware`, `perf stat -e node-load-misses` |
| JVM | GC Concurrent Phase에서 GC Thread들이 Worker Thread와 CPU 경합. G1/ZGC의 Concurrent Mark 중 CPU 사용률 일시 상승 | JFR `jdk.GCPhaseConcurrent`, GC log `[GC(Concurrent) cpu=...]` |

---

## 3. Runnable Queue vs Wait Queue

| 구분 | Runnable Queue | Wait Queue |
|------|---------------|------------|
| `task_struct` 상태 | `TASK_RUNNING` | `TASK_INTERRUPTIBLE` / `TASK_UNINTERRUPTIBLE` |
| 의미 | CPU 실행 준비 완료 | I/O, Lock, Timer 등 이벤트 대기 |
| 병목 원인 | CPU Saturation | DB, Disk, Network, Lock 등 외부 자원 |
| Scheduler 대상 | 예 | 아니오 (이벤트 완료 후 Runnable 복귀) |
| CPU 점유 | 예 (실행 중) 또는 대기 (Runnable) | 아니오 |
| Load Average 기여 | `TASK_RUNNING` 포함 | `TASK_UNINTERRUPTIBLE`만 포함 |

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel (Runnable Queue) | 각 CPU Core별 `struct rq` 존재. `rq->nr_running`이 핵심 지표. CFS는 `rq->cfs.tasks_timeline`(Red-Black Tree)에서 최소 `vruntime` task 선택 | `/proc/schedstat` (run_delay 합산), `perf sched latency` |
| Kernel (Wait Queue) | I/O 대기 시 `task_struct`가 `wait_queue_head_t`에 연결. I/O 완료 IRQ → `wake_up()` → Runnable Queue 재진입. `TASK_UNINTERRUPTIBLE`: SIGKILL도 즉시 처리 불가 | `ps aux` (D 상태 task 확인), `dmesg \| grep "blocked for more than"` |
| Kernel (Migration) | CPU 간 부하 불균형 시 `load_balance()`가 `task_struct`를 다른 CPU Runnable Queue로 이동. 이동 시 Cache Miss 유발 | `perf stat -e cpu-migrations -p <pid>`, `/proc/schedstat` (cpu_nr_migrations) |
| JVM (Off-CPU) | Java Thread가 `Object.wait()`, `LockSupport.park()`, `Thread.sleep()` 호출 시 Futex를 통해 `TASK_INTERRUPTIBLE` 상태로 전환. CPU 미점유이나 Thread Pool 슬롯은 유지 | `async-profiler -e wall` (Off-CPU Flame Graph), `jstack` (WAITING/TIMED_WAITING 상태 Thread) |

---

## 4. Load Average와 task_struct

Load Average에는 아래 상태의 `task_struct`가 포함된다.

| 포함 상태 | kernel 상태값 | 의미 |
|----------|-------------|------|
| `TASK_RUNNING` | 0 | Running 또는 Runnable 상태 (CPU 실행 중 or 대기) |
| `TASK_UNINTERRUPTIBLE` | 2 | Disk I/O, NFS 등 중단 불가 대기 (D 상태) |

Load Average는 1분 / 5분 / 15분 지수 이동 평균값이며, 값이 CPU 코어 수보다 지속적으로 높으면 Saturation 신호다.

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel | Load Average는 `calc_load()` 함수에서 5초마다 갱신. `avenrun[3]` 배열에 FSHIFT 고정소수점으로 저장 후 `/proc/loadavg`로 노출 | `cat /proc/loadavg`, `uptime`, `top` |
| Kernel (D 상태) | `TASK_UNINTERRUPTIBLE` task가 Load Average를 높이는 경우, CPU가 한가해도 Load Average는 높을 수 있음. Disk I/O 병목, NFS Hang, Kernel Lock 대기 등이 원인 | `ps aux \| awk '$8=="D"'`, `iotop -o`, `dmesg \| grep "INFO: task.*blocked"` |
| Kernel (IO Scheduler) | Disk I/O가 `TASK_UNINTERRUPTIBLE` 증가의 주원인. blk-mq(Multi-Queue Block Layer)가 I/O 요청을 처리. I/O Scheduler(mq-deadline, kyber, bfq) 선택에 따라 D 상태 task 대기 시간 결정 | `cat /sys/block/sda/queue/scheduler`, `iostat -x 1`, `blktrace -d /dev/sda` |
| Kernel (Dirty Page Writeback) | Page Cache의 Dirty Page가 일정 비율 초과 시 `kworker`/`pdflush` 스레드가 Disk에 기록. 이 과정에서 Write 요청한 task가 `TASK_UNINTERRUPTIBLE` 상태로 대기 가능 | `/proc/vmstat` (nr_dirty, nr_writeback), `sysctl vm.dirty_ratio`, `iostat -x` (await 값) |
| JVM | JVM Full GC나 대규모 Heap 할당 시 `mmap()`/`madvise()` syscall 내부에서 Page Fault 처리 중 Disk I/O 발생 가능. `TASK_UNINTERRUPTIBLE` → Load Average 기여 | GC log Major GC 발생 빈도, `vmstat 1` (si/so: swap in/out) |

---

## 5. Thread-per-request와 Blocking I/O 문제

### Thread-per-request 구조

요청 수가 증가하면 Java Thread와 `task_struct` 수도 함께 증가한다.

```
1,000 Requests  →  1,000 Java Threads  →  1,000 task_struct
  ↓ 각 Thread Stack: Native Memory ~512KB~1MB  →  총 ~500MB~1GB Native 사용
  ↓ Scheduler가 관리해야 하는 실행 단위 급증  →  CFS Red-Black Tree 비용 증가
  ↓ Context Switch 증가  →  Cache Miss / TLB Flush 증가
```

### 계층별 메커니즘 실체 (Thread 생성)

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| JVM | `new Thread()` → JVM이 `pthread_create()` 호출 → Kernel `clone()` syscall → 새 `task_struct` 생성. Thread Stack은 Native Memory(Off-Heap)에 할당 | `jcmd <pid> VM.native_memory` (Thread 항목), `pmap -x <pid>` |
| Kernel | `clone(CLONE_VM\|CLONE_FS\|CLONE_FILES\|CLONE_SIGHAND\|CLONE_THREAD)` 플래그로 Thread 생성. 프로세스 주소 공간 공유. `task_struct` 생성 비용은 `fork()`보다 낮음 | `strace -e clone -p <pid>`, `/proc/<pid>/task/` (task_struct 목록) |
| Kernel (Memory) | Thread Stack은 `mmap(MAP_ANONYMOUS\|MAP_STACK)` 으로 할당. 실제 물리 메모리는 접근 시점에 Page Fault로 할당(Lazy Allocation). `ulimit -s` 가 Stack 크기 제한 | `/proc/<pid>/maps` (stack 항목), `cat /proc/sys/kernel/threads-max` |
| OS (Limits) | 시스템 전체 `threads-max`, 프로세스별 `ulimit -u`(max user processes)가 Thread 수 상한. `ulimit -n`(open files)이 Thread별 Socket fd 수 제한 | `ulimit -a`, `/proc/sys/kernel/pid_max`, `cat /proc/<pid>/limits` |

---

### Blocking I/O와 Thread Pool 점유

```
Java Thread 실행
  ↓ Blocking I/O 요청 (DB Socket read, 외부 API, File read 등)
  ↓ read()/recv() syscall → Kernel Mode 진입
  ↓ 데이터 미준비 → task_struct → Wait Queue 이동 (TASK_INTERRUPTIBLE)
  ↓ CPU 미점유 / Java Thread → Thread Pool에 반환되지 않음 (슬롯 점유)
  ↓ I/O 완료 → NIC IRQ → SoftIRQ → TCP Stack → Socket Buffer 갱신
  ↓ epoll/select 이벤트 → task_struct Wake-up → Runnable Queue 복귀
```

### 계층별 메커니즘 실체 (Blocking I/O)

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel (Socket Buffer) | `recv()` 호출 시 Socket 수신 버퍼(`sk_buff`) 에 데이터 없으면 `task_struct`를 Socket의 `wait_queue`에 연결 후 `schedule()` 호출 → CPU 반환. `SO_RCVBUF` / `net.ipv4.tcp_rmem`이 버퍼 크기 결정 | `ss -tnmp` (Recv-Q), `sysctl net.ipv4.tcp_rmem` |
| Kernel (Futex) | Java `synchronized`, `ReentrantLock` 경합 시 `futex(FUTEX_WAIT)` syscall → `task_struct` Sleep. Lock 해제 시 `futex(FUTEX_WAKE)` → Wake-up. Uncontended Lock은 User Space에서만 처리(Kernel 미진입) | `perf trace -e futex -p <pid>`, `strace -e futex` |
| JVM (Off-CPU) | Blocking 동안 Worker Thread는 CPU 미사용(Off-CPU Time). CPU Profiler(Sampling)로는 탐지 불가. Wall-clock / Off-CPU Profiler 필요 | `async-profiler -e wall -d 60 -f offcpu.html <pid>`, Pyroscope, Datadog APM |
| JVM (Connection Pool) | JDBC Connection Pool(HikariCP) 고갈 시 Worker Thread가 `connectionTimeout`(기본 30초)까지 Futex Wait. 전체 Thread Pool 고갈 → 신규 요청 처리 불가 → 503 | HikariCP JMX `pendingThreads`, `activeConnections`, APM DB Connection Wait 시간 |
| JVM (Thread Stack) | Blocking 중에도 Thread Stack 전체 유지(수십~수백 Frame). Thread 수 × Stack Size 만큼 Native Memory 점유 지속 | `jcmd <pid> VM.native_memory` (Thread committed), `jstack \| grep -c "java.lang.Thread"` |

---

### 일부 느린 I/O의 전체 영향

```
느린 I/O 요청 증가 (DB Slow Query, 외부 API Timeout)
  ↓ Worker Thread 장시간 점유 (Off-CPU: Wait Queue)
  ↓ 사용 가능한 Thread 감소 (Thread Pool 슬롯 고갈)
  ↓ 가벼운 요청도 Thread 할당 대기 (acceptCount 큐 대기)
  ↓ 전체 응답 지연 → P99/P999 Latency 급증
  ↓ Timeout 증가 → Retry 증가 → Retry Storm 유발 가능
```

### 계층별 메커니즘 실체 (Cascading Failure)

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Application (Retry Storm) | 외부 서비스 지연 시 Client Retry + 짧은 Timeout 미설정 → 동시 요청 수 폭발적 증가. Circuit Breaker 미적용 시 Cascade Failure로 이어짐 | Resilience4j MBean, APM Error Rate / Retry 횟수 그래프 |
| Application (Circuit Breaker) | Circuit Breaker(CLOSED→OPEN) 전환 시 외부 호출 즉시 실패 반환 → Thread Pool 점유 시간 단축. Half-Open 상태에서 점진적 복구 | Resilience4j `circuitbreaker.calls.failed.rate`, Actuator `/actuator/circuitbreakers` |
| Application (Backpressure) | Thread Pool 고갈 시 Tomcat `acceptCount` 큐 → 큐 포화 시 TCP Accept Queue 적체 → 클라이언트 연결 지연. 명시적 Backpressure 없으면 OOM 또는 연결 거부로 귀결 | `ss -lntp` (Recv-Q 증가), Tomcat JMX `currentThreadsBusy == maxThreads` |
| Kernel (TCP Backlog) | Tomcat `acceptCount` 초과 시 Kernel TCP Accept Queue 적체. `net.core.somaxconn` 한계 도달 시 신규 SYN 패킷 DROP | `netstat -s \| grep "listen queue"`, `ss -lntp` Recv-Q, `dmesg \| grep "syn flood"` |

---

### Thread Pool 크기 증가의 위험

Blocking I/O 병목이 해소되지 않은 상태에서 Thread 수만 늘리면 아래 문제가 연쇄적으로 발생한다.

```
Thread Pool 크기 증가
  ↓ task_struct 증가  →  CFS 관리 비용 증가
  ↓ Context Switch 증가  →  Cache Miss / TLB Flush 증가
  ↓ Native Memory(Thread Stack) 증가  →  RSS 증가
  ↓ Memory Bandwidth 압박 증가  →  NUMA Remote Access 증가 가능
  ↓ I/O 완료 시 다수 task_struct 동시 Wake-up  →  Runnable Queue 순간 폭발
  ↓ CPU Saturation 가중
```

### 계층별 메커니즘 실체 (Thread Pool 과다)

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel (Context Switch) | 비자발적 Context Switch(nvcswch): CPU Time Slice 만료(CFS Timer IRQ) 시 발생. Thread 수 증가 → Time Slice 단축 → nvcswch 급증 | `pidstat -w -p <pid> 1` (nvcswch/s), `vmstat 1` (cs 컬럼) |
| Kernel (Memory Bandwidth) | Thread 수 증가 → 동시 메모리 접근 증가 → Memory Bus 경쟁 → Memory Bandwidth Saturation. Intel Xeon 기준 단일 소켓 Memory Bandwidth ~100~200GB/s. 초과 시 실행 대기 발생 | `perf stat -e LLC-load-misses`, `pcm-memory` (Intel PCM), `numactl --show` |
| Kernel (CPU Frequency) | Thread 폭증 후 CPU C-state(Deep Sleep)에서 깨어날 때 P-state(주파수) 복귀 지연. Latency-sensitive 워크로드에서는 `performance` Governor 권장 | `cpupower frequency-info`, `turbostat --Summary`, `/sys/devices/system/cpu/cpufreq/scaling_governor` |
| JVM (GC Pressure) | Thread 수 증가 → TLAB 총 사용량 증가 → Eden 소진 빈도 증가 → Minor GC STW 빈도 증가 | GC log Allocation Rate, `jstat -gcutil <pid> 1000` (YGC 빈도) |

---

## 6. Scheduler Overhead와 Cache Miss

### Scheduler Overhead 발생 비용

| 비용 항목 | Kernel 메커니즘 | 설명 |
|----------|----------------|------|
| Runnable Queue 관리 | CFS Red-Black Tree (`tasks_timeline`) | `task_struct` 삽입/삭제: O(log n). n 증가 시 탐색 비용 증가 |
| vruntime 계산 | `update_curr()` 함수 | 각 task의 누적 실행 시간 및 우선순위 반영 `vruntime` 갱신 |
| Context Switch | `__schedule()` → `context_switch()` | CPU 레지스터, FPU 상태 저장/복구. Kernel Mode에서만 실행 |
| Kernel Mode 진입 | Timer IRQ → Kernel Mode 전환 | 스케줄링 결정 자체가 CPU Cycle 소비 (`sy` 시간 증가) |
| Load Balancing | `load_balance()` | CPU 간 부하 균등화. `task_struct` Migration 시 Cache Miss 유발 |

```
Runnable task_struct 증가
  ↓ CFS 관리 비용 증가  →  System CPU Time(sy) 증가
  ↓ User CPU Time(us) 감소  →  비즈니스 로직 실행 시간 감소
  ↓ Throughput 감소
```

### 계층별 메커니즘 실체 (Scheduler Overhead)

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel (CFS) | `/proc/schedstat`의 `sched_yield_count`, `sched_switch` 등에서 Scheduler 활동 확인 가능. `sched_latency_ns`(기본 6ms~24ms) / `sched_min_granularity_ns` 파라미터 튜닝으로 Time Slice 조정 | `sysctl kernel.sched_latency_ns`, `/proc/sched_debug`, `perf sched record && perf sched latency` |
| Kernel (RT Throttling) | Java App에 SCHED_FIFO/SCHED_RR Real-Time 스케줄링 적용 시 `rt_throttle`에 의해 RT task 실행 시간 제한(기본 95%). Tomcat/Spring 환경에서 드물지만 주의 필요 | `/proc/sys/kernel/sched_rt_runtime_us`, `chrt -p <pid>` |
| Hardware (IPC) | Scheduler Overhead 증가 → `sy` 코드 실행 중 Branch Misprediction, Cache Miss 발생 → IPC 저하. IPC < 1.0이면 메모리 또는 파이프라인 병목 | `perf stat -e instructions,cycles -p <pid>` (IPC = instructions/cycles) |

---

### Context Switch와 Cache Miss

```
task_struct A 실행  →  A의 데이터가 L1/L2 Cache에 적재 (Hot Cache)
  ↓ Context Switch (레지스터 저장, CR3 갱신, TLB Flush)
task_struct B 실행  →  B의 데이터가 Cache에 없음  →  L1/L2/L3 Cache Miss
  ↓ Last Level Cache(L3) Miss → RAM 접근 (~100ns)
  ↓ CPU Pipeline Stall  →  실행 대기
  ↓ NUMA Remote Memory인 경우 ~300ns 추가 지연
```

### 계층별 메커니즘 실체 (Context Switch 비용)

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Hardware (TLB Flush) | `task_struct` 전환 시 CR3 레지스터 갱신 → TLB 전체 Flush (프로세스 전환 시). 동일 프로세스 내 Thread 전환은 주소 공간 공유로 TLB Flush 회피 가능(ASID 지원 CPU) | `perf stat -e dTLB-load-misses,iTLB-load-misses` |
| Hardware (L1/L2 Cache) | 32KB L1d Cache는 Context Switch 후 수백 us 내 재워밍 필요. 고빈도 Context Switch는 Cache를 항상 Cold 상태로 유지시켜 실효 IPC를 크게 낮춤 | `perf stat -e L1-dcache-load-misses,L1-icache-load-misses` |
| Hardware (Cache Line Thrashing) | 여러 Thread가 동일 Cache Line(64 Byte)을 동시에 쓸 때 False Sharing 발생. MESI Protocol에 의해 Cache Line 무효화 반복 → 성능 저하 | `perf c2c record && perf c2c report` (False Sharing 탐지) |
| Kernel (Voluntary vs Involuntary) | 자발적 CS(cswch): I/O 대기, Lock 대기 등 `schedule()` 직접 호출. 비자발적 CS(nvcswch): Time Slice 만료 시 Timer IRQ → 강제 전환. nvcswch 높으면 CPU Saturation 신호 | `pidstat -w 1 -p <pid>` (cswch/s, nvcswch/s 구분), `vmstat 1` cs |
| JVM (Safepoint) | GC STW, Deoptimization 등 Safepoint 진입 시 모든 Worker Thread가 안전 지점 도달 대기. 이 시간 동안 모든 Thread가 CPU 반환 후 재개 시 일제히 Runnable Queue 진입 → 순간 Saturation | `-Xlog:safepoint*`, JFR `jdk.SafepointBegin` `ttspTime`(Time To Safepoint) |

---

## 7. Non-blocking I/O와 I/O 처리 구조

### Blocking vs Non-blocking 비교

| 항목 | Blocking I/O | Non-blocking I/O |
|------|-------------|-----------------|
| I/O 대기 중 Thread | 점유됨 (Wait Queue) | 다른 작업 수행 가능 |
| Thread Pool 압박 | 큼 (I/O 시간 동안 슬롯 점유) | 상대적으로 작음 |
| task_struct 수 | 요청 수 비례 증가 | 소수 Event Loop Thread 유지 |
| Context Switch | 증가 (많은 task_struct) | 상대적으로 적음 |
| Kernel Interaction | `read()` Block → Wait Queue → Wake-up | `epoll_wait()` 이벤트 기반 처리 |
| CPU Saturation 위험 | I/O 완료 시 다수 Wake-up → 순간 Saturation | Event Loop이 순차 처리 → 부드러운 처리 |

### Non-blocking 처리 흐름

```
I/O 요청
  ↓ Socket을 Non-blocking 모드로 설정 (O_NONBLOCK)
  ↓ epoll_ctl(EPOLL_CTL_ADD): 관심 이벤트 등록
  ↓ Thread는 다른 작업 수행 (또는 epoll_wait() 대기)
  ↓ NIC IRQ → SoftIRQ → TCP Stack → sk_buff → Socket Buffer 갱신
  ↓ epoll 내부 Ready List 갱신
  ↓ epoll_wait() 반환 → Event Handler / Callback 실행
```

### 계층별 메커니즘 실체 (Non-blocking I/O)

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel (epoll) | `epoll_create1()` → epoll fd 생성. `epoll_ctl(EPOLL_CTL_ADD)`: 관심 fd를 Kernel 내부 `ep_item`(rb-tree)에 등록. `epoll_wait()`: Ready List가 비면 `task_struct`를 Sleep, 이벤트 발생 시 Wake-up. Edge Trigger(EPOLLET) vs Level Trigger(기본) 동작 차이 주의 | `strace -e epoll_ctl,epoll_wait -p <pid>`, `/proc/<pid>/fdinfo/<epoll_fd>` |
| Kernel (SoftIRQ) | 패킷 수신 시 NIC HW IRQ → `net_rx_action` SoftIRQ 예약 → `ksoftirqd` 또는 IRQ tail에서 실행 → TCP Stack → Socket Buffer → epoll Ready List. 트래픽 폭증 시 `ksoftirqd`가 CPU 독점 가능 | `mpstat -I ALL 1` (`%soft` 컬럼), `top` (ksoftirqd CPU 점유율) |
| Kernel (RPS/RFS) | 기본 설정에서 NIC IRQ가 단일 CPU에 집중 → SoftIRQ CPU 독점. RPS(Receive Packet Steering)로 여러 CPU에 패킷 처리 분산. RFS(Receive Flow Steering)로 Application이 실행 중인 CPU로 패킷 유도 | `/proc/irq/<N>/smp_affinity`, `ethtool -L eth0 combined <N>`, `/proc/sys/net/core/rps_sock_flow_entries` |
| JVM (Event Loop) | Netty/WebFlux의 Event Loop Thread는 `epoll_wait()` 호출 후 이벤트 처리. Blocking 호출이 Event Loop Thread에 진입하면 해당 CPU의 모든 I/O 처리 마비 | Netty Blocked Event Loop 경고 로그, `async-profiler` (Event Loop Thread CPU 점유율) |
| JVM (Reactor) | Project Reactor의 `Scheduler.boundedElastic()`은 Blocking 작업을 전용 Thread Pool에 격리. `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())` 패턴으로 Event Loop 보호 | Reactor Metrics (`reactor.scheduler.tasks.active`), JFR Thread Activity |

---

### 네트워크 I/O vs 디스크 I/O

| I/O 종류 | Non-blocking 처리 방식 | 주의사항 |
|---------|----------------------|---------|
| Network Socket I/O | epoll 기반 이벤트 처리에 최적 | SoftIRQ CPU 포화 주의 |
| RDBMS + JDBC | Driver가 Blocking Socket → Event Loop 차단 가능 | 전용 Blocking Thread Pool로 격리 필요 |
| RDBMS + R2DBC | Non-blocking Driver → Netty/WebFlux와 결합 가능 | DB Side Backpressure 처리 설계 필요 |
| File I/O | epoll은 일반 파일 fd에 비효율적 (항상 Ready 반환 가능) | io_uring 또는 전용 Pool 필요 |
| Object Storage (S3) | HTTP Client Non-blocking 처리 가능 | 응답 Streaming 시 Backpressure 설계 필요 |

---

## 8. epoll과 File Descriptor

Linux Kernel은 I/O 통로를 File Descriptor(FD)로 관리한다. 소켓, 파일, 파이프 등은 모두 FD로 표현된다.

```
Socket 생성  →  FD 할당 (open file table entry → inode)
  ↓ epoll_ctl(EPOLL_CTL_ADD, fd, EPOLLIN|EPOLLET)
  ↓ Kernel 내부 ep_item (rb-tree에 삽입, fd → epoll 연결)
  ↓ epoll_wait() 호출  →  Ready List 비어있으면 task_struct Sleep
  ↓ 패킷 수신 → Socket Buffer 갱신 → ep_item Wake-up Callback 호출
  ↓ Ready List에 fd 추가  →  epoll_wait() 반환  →  Application 처리
```

| 항목 | Kernel 구조체 | 의미 |
|------|-------------|------|
| FD | `struct file` (open file table) | 감시할 I/O 통로 |
| epoll instance | `struct eventpoll` | Ready List + rb-tree 관리 |
| 등록 항목 | `struct epitem` | fd별 관심 이벤트 및 User Data |
| Event Type | `EPOLLIN`, `EPOLLOUT`, `EPOLLET`, `EPOLLRDHUP` | 이벤트 종류 |

### 계층별 메커니즘 실체 (epoll / FD)

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel (FD 한계) | 프로세스별 `ulimit -n`(기본 1024~65536), 시스템 전체 `/proc/sys/fs/file-max`. Tomcat `maxConnections` + Worker Thread fd + epoll fd 합산이 한계 초과 시 `Too many open files` 오류 | `lsof -p <pid> \| wc -l`, `cat /proc/sys/fs/file-nr`, `ulimit -n` |
| Kernel (epoll Scalability) | epoll은 O(1) 이벤트 감지(Ready List 기반). `select`/`poll`은 O(n) fd 스캔. 수만 개의 연결에서도 효율적 | `strace -e epoll_wait -p <pid>` (각 호출 반환 이벤트 수), `/proc/<pid>/fdinfo/<epoll_fd>` (tfile 목록) |
| Kernel (Socket Buffer) | `EPOLLIN` 이벤트는 Socket 수신 버퍼에 데이터 존재 시 발생. 버퍼 크기(`SO_RCVBUF`)가 작으면 대용량 응답 처리 시 이벤트 반복 발생 → Syscall 오버헤드 증가 | `ss -tnmp` (Recv-Q, Send-Q), `sysctl net.core.rmem_max` |
| JVM (NIO) | Java NIO `Selector` → 내부적으로 `epoll_create/ctl/wait` 사용. Tomcat NIO의 Poller가 `Selector.select()` 호출 → `epoll_wait()`. `Selector` 등록 fd 수는 JVM 내부 `EPollSelectorImpl`에서 관리 | `jstack` (Poller Thread: `sun.nio.ch.EPollSelectorImpl.doSelect`) |

---

### 네트워크 패킷 수신 흐름

```
Packet 도착
  ↓ NIC가 DMA로 Rx Ring Buffer(sk_buff)에 패킷 기록
  ↓ NIC가 CPU에 Hardware IRQ 발생
  ↓ CPU 현재 실행 중단  →  IRQ Handler 진입 (Kernel Mode)
  ↓ IRQ Handler: 최소 처리 후 SoftIRQ(NET_RX_SOFTIRQ) 예약
  ↓ IRQ Handler 완료  →  SoftIRQ 처리 (ksoftirqd 또는 IRQ tail)
  ↓ net_rx_action(): sk_buff 처리 → TCP/IP Stack → Socket Buffer 갱신
  ↓ Socket의 epoll Wait Queue에 이벤트 전달
  ↓ epoll_wait() 중인 task_struct Wake-up → Runnable Queue 복귀
  ↓ Event Loop / Poller Thread가 이벤트 처리
```

트래픽이 많아지면 SoftIRQ 처리 비용이 증가하여 `%soft` CPU 사용률 상승의 원인이 된다.

### 계층별 메커니즘 실체 (패킷 수신)

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Hardware (DMA) | NIC가 CPU 개입 없이 직접 메모리에 패킷 기록(DMA). DMA 완료 후 IRQ 발생. NIC Rx Ring Buffer 오버플로우 시 패킷 DROP | `ethtool -S eth0` (rx_missed_errors, rx_fifo_errors), `ifconfig` (RX dropped) |
| Kernel (IRQ Affinity) | 기본 설정에서 모든 NIC IRQ가 CPU 0에 집중 → CPU 0 SoftIRQ 포화. `/proc/irq/<N>/smp_affinity_list`로 IRQ를 여러 CPU에 분산 | `cat /proc/irq/<N>/smp_affinity`, `irqbalance` 서비스 상태 |
| Kernel (GRO/GSO) | Generic Receive Offload(GRO): 연속 패킷을 하나로 합쳐 Kernel 처리 횟수 감소. Generic Segmentation Offload(GSO): 대용량 전송 시 NIC에서 분할. 고트래픽 환경에서 CPU 절감 | `ethtool -k eth0 \| grep offload`, `ethtool -K eth0 gro on` |
| Kernel (TCP Stack) | `sk_buff`가 TCP Stack을 통과하며 TCP Sequence, Checksum, ACK 처리. 이 과정이 SoftIRQ 시간의 주요 부분 차지 | `netstat -s` (TCP 통계), `perf trace -e net:*` |

---

## 9. io_uring

io_uring은 Linux Kernel 5.1부터 도입된 비동기 I/O 인터페이스다.  
기존 epoll/read/write 기반 방식보다 시스템 콜 오버헤드를 줄이기 위해 설계되었다.

```
Application                              Kernel
     │                                      │
     │  SQ Ring Buffer (공유 메모리)          │
     │─────────────── SQE 기록 ────────────►│
     │                                      │  I/O 처리
     │◄────────────── CQE 기록 ─────────────│
     │  CQ Ring Buffer (공유 메모리)          │
     │                                      │
     └── io_uring_enter() syscall 최소화 ────┘
            (SQPOLL 모드: 0 syscall 가능)
```

| Queue | 역할 | 구조 |
|-------|------|------|
| Submission Queue (SQ) | Application이 Kernel에 요청할 I/O 작업 등록 | 공유 Ring Buffer (mmap) |
| Completion Queue (CQ) | Kernel이 완료된 I/O 결과 기록 | 공유 Ring Buffer (mmap) |

### 계층별 메커니즘 실체 (io_uring)

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel (Ring Buffer) | `io_uring_setup()` syscall → SQ/CQ Ring Buffer를 `mmap()`으로 User Space와 공유. Application이 syscall 없이 SQE(Submission Queue Entry)를 기록 가능. `io_uring_enter()` 1회로 다수 I/O 요청 제출 | `strace -e io_uring_setup,io_uring_enter -p <pid>`, `/proc/<pid>/fdinfo/<uring_fd>` |
| Kernel (SQPOLL) | SQPOLL 모드: Kernel Thread(`io_sq_thread`)가 SQ를 폴링 → Application의 `io_uring_enter()` 불필요. 극단적 저지연 I/O에서 syscall 오버헤드 제거. CPU 하나를 Kernel Thread가 전담하는 Trade-off | `ps aux \| grep io_sq_thread`, CPU 코어 전용 할당 설정 필요 |
| Kernel (Fixed Buffer) | `IORING_OP_READ_FIXED`: 사전 등록된 User Space 버퍼로 직접 I/O. Kernel-User Copy 최소화. Zero-Copy에 가까운 성능 | io_uring Benchmark vs epoll 비교 |
| JVM | Java 21+ `Loom` Virtual Thread는 내부적으로 io_uring 또는 epoll을 활용하여 Blocking API를 Non-blocking으로 변환. JVM이 추상화하므로 직접 io_uring 사용은 드물지만 `netty-incubator-transport-io_uring` 존재 | Netty io_uring transport GitHub, `lsof -p <pid>` (io_uring fd 확인) |
| SRE | io_uring 사용 시 Kernel 버전(5.1+, 권장 5.10+), seccomp 정책, Kubernetes 보안 컨텍스트 확인 필요. CVE 이력 존재하므로 최신 Kernel 유지 필수 | `uname -r`, `sysctl kernel.io_uring_disabled` |

---

### io_uring vs 전용 Blocking Thread Pool

| 구분 | io_uring | 전용 Blocking Thread Pool |
|------|----------|--------------------------|
| 계층 | Kernel I/O 인터페이스 | Application 구조 설계 |
| 목적 | 시스템 콜 및 Kernel-User Copy 오버헤드 감소 | Blocking 작업을 Event Loop에서 격리 |
| Thread 수 | 소수 Thread로 높은 I/O Throughput 달성 | Pool 크기만큼 Thread 추가 필요 |
| Context Switch | 최소화 (소수 Thread 유지) | Blocking 해소 후에도 Pool Thread Context Switch 발생 |
| 주요 대상 | File I/O, Network I/O, Direct Disk I/O | JDBC, Blocking SDK, Legacy API |
| JVM 친화성 | 낮음 (직접 사용 어려움, Netty 라이브러리 활용) | 높음 (`Executors.newBoundedThreadPool()` 등) |
| 위험 | Kernel 버전 의존, 보안 이슈(CVE) | Context Switch, Pool 고갈, Retry Storm |

---

## 10. JDBC vs R2DBC

| 항목 | JDBC | R2DBC |
|------|------|-------|
| I/O 방식 | Blocking Socket (`read()` 대기) | Non-blocking Socket (epoll 이벤트 기반) |
| Thread 점유 | DB 응답 대기 중 Thread 점유 | I/O 대기 중 Thread 반환 가능 |
| Event Loop 영향 | Event Loop Thread 직접 호출 시 Blocking → 마비 | Event Loop Thread에서 안전하게 사용 가능 |
| 적합한 구조 | Spring MVC + Thread-per-request Pool | Spring WebFlux + Netty Event Loop |
| Connection Pool | HikariCP (Blocking 기반) | R2DBC Connection Pool (Non-blocking 기반) |
| Backpressure | 미지원 (Pool 고갈 = 장애) | Reactive Streams Backpressure 지원 |

### 계층별 메커니즘 실체 (JDBC vs R2DBC)

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel (JDBC Blocking) | JDBC Driver가 DB 서버와 TCP Socket 통신. `read()` syscall → DB 응답 없으면 Worker Thread가 `TASK_INTERRUPTIBLE` → Wait Queue. DB 응답 시 IRQ → Wake-up → Runnable 복귀 | `strace -e read,recv -p <pid>` (JDBC Socket read), Off-CPU Flame Graph |
| Kernel (R2DBC Non-blocking) | R2DBC Driver가 Non-blocking Socket 사용. `epoll`을 통해 DB 응답 이벤트 감지. Event Loop Thread는 DB 응답 대기 중 다른 요청 처리 가능 | `strace -e epoll_wait -p <pid>` (R2DBC epoll 활동), Event Loop Thread CPU 점유율 |
| JVM (WebFlux + JDBC 위험) | WebFlux Event Loop Thread에서 직접 JDBC 호출 시 해당 CPU의 모든 요청 처리 중단. `Schedulers.boundedElastic()`으로 반드시 격리. Virtual Thread(Loom) 환경에서는 JVM이 자동 비동기화 가능(JDK 21+) | Reactor `BlockHound` (Blocking 호출 탐지), Netty Event Loop Blocking 경고 로그 |
| JVM (Connection Pool Exhaustion) | HikariCP `maximumPoolSize` 초과 시 `connectionTimeout`까지 Futex Wait. R2DBC Pool 도 `maxSize` 초과 시 Subscriber 대기. 두 경우 모두 요청 지연 또는 오류 발생 | HikariCP JMX `pendingThreads`, R2DBC Pool Metrics, APM DB Connection Wait P99 |
| Application (Serialization) | DB 결과를 Java 객체로 역직렬화(ResultSet → POJO): 대용량 Result Set의 경우 Jackson/ObjectMapper 또는 ORM Mapping 비용이 CPU 사용률에 유의미한 영향. `@JsonView`, Lazy Loading 전략으로 완화 | `async-profiler -e cpu` (ObjectMapper CPU 점유율), JFR Allocation 분석 |

---

## 11. SRE 관점 주요 지표

| 지표 | 계층 | 의미 | 임계 신호 |
|------|------|------|---------|
| CPU Utilization (`us`/`sy`) | Hardware / Kernel | CPU 사용률 분해 | `sy` > 20%: Kernel Overhead 의심 |
| CPU Pressure (PSI `some`) | Kernel | CPU 대기 압력 | avg10 > 20%: Saturation 진입 |
| Run Queue Length | Kernel (CFS) | Runnable task 수 / CPU 수 | > 1.0 지속: CPU Saturation |
| Load Average | Kernel | Running/Runnable + D 상태 task | > CPU 수 × 2 지속: 병목 신호 |
| Context Switches (cs) | Kernel | 실행 전환 빈도 | 급격한 증가: Thread Pool 과다 또는 Lock 경합 |
| nvcswch (비자발적 CS) | Kernel | Time Slice 만료로 인한 강제 전환 | 높음: CPU Saturation 직접 신호 |
| System CPU (`sy`) | Kernel | Kernel 작업 비중 | 지속 상승: Scheduler/Syscall 오버헤드 |
| Softirq CPU (`%soft`) | Kernel | 네트워크 패킷 처리 비용 | > 5~10%: 트래픽 폭증 또는 RSS 미설정 |
| Cache Miss Rate | Hardware | L1/L2/L3 캐시 히트율 | LLC Miss Rate 상승: Memory 병목 |
| TLB Miss | Hardware | 가상→물리 주소 변환 실패 | `dTLB-load-misses` 상승: mmap 과다 또는 Thread 폭증 |
| IPC | Hardware | Cycle당 명령어 수 | < 1.0: Memory 또는 Pipeline 병목 |
| CPU Throttled Time | Kernel (cgroup) | Kubernetes CPU Limit 초과 시간 | 0 초과: K8s CPU Limit 조정 필요 |
| Open FD Count | Kernel | 열린 File Descriptor 수 | `ulimit -n`에 근접: FD 고갈 위험 |
| Thread Count | JVM / OS | JVM Thread 수 / task_struct 수 | 급격한 증가: Blocking I/O 증가 또는 Thread Leak |
| Thread Pool Active | JVM | 사용 중인 Worker Thread 수 | `busyThreads == maxThreads`: Pool 고갈 임박 |
| DB Connection Pool | JVM | 활성 DB Connection 수 | `pendingThreads > 0`: Connection Exhaustion 진입 |
| P99 / P999 Latency | Application | 사용자 관점 응답 지연 | 급격한 상승: Saturation 또는 Blocking 증가 |
| Off-CPU Time | JVM / Kernel | Blocking으로 CPU 미사용 시간 | Wall > CPU 시간: Blocking I/O 병목 |
| GC Pause (STW) | JVM | Stop-the-World GC 시간 | > 100ms: Latency Spike 직접 원인 |

### 진단 명령어

```bash
# CPU 전반
vmstat 1                                          # r(Runnable), sy, cs
mpstat -P ALL 1                                   # CPU별 %us, %sy, %soft, %irq
top -H -p <PID>                                   # Thread별 CPU 사용률

# Scheduler / Context Switch
pidstat -w -p <PID> 1                             # cswch/s, nvcswch/s 구분
perf sched record -p <PID> -- sleep 10
perf sched latency                                # Scheduler 대기 시간 분포
/proc/schedstat                                   # CPU별 Scheduler 통계

# CPU Pressure
cat /proc/pressure/cpu                            # PSI some/full avg10/60/300

# Cache / TLB / IPC
perf stat -e cache-misses,cache-references,\
  dTLB-load-misses,instructions,cycles,\
  branch-misses,stalled-cycles-backend -p <PID>

# cgroup Throttling
cat /sys/fs/cgroup/cpu.stat                       # throttled_usec
kubectl top pod                                    # K8s 수준 CPU 사용
crictl stats                                       # Container 수준

# Network SoftIRQ
mpstat -I ALL 1                                   # %soft 컬럼
cat /proc/softirqs                                # SoftIRQ 종류별 발생 수
ethtool -S eth0 | grep rx_missed                  # NIC Rx 오버플로우

# File Descriptor
lsof -p <PID> | wc -l
cat /proc/sys/fs/file-nr                          # 시스템 전체 FD 사용

# JVM
jstack <PID>                                      # Thread 상태 (BLOCKED/WAITING/RUNNABLE)
jstat -gcutil <PID> 1000                          # GC 빈도 및 STW 시간
async-profiler -e wall -d 60 -f offcpu.html <PID> # Off-CPU Flame Graph
jcmd <PID> VM.native_memory                       # Thread Stack Native Memory

# NUMA
numastat -p <PID>                                 # NUMA Remote Access 비율
numactl --hardware                                # NUMA 토폴로지 확인
```

---

## 12. 최종 정리

### CPU Saturation 연쇄 구조 (Blocking 방식)

```
Thread-per-request + Blocking I/O
  ↓ Thread Pool 점유 → task_struct 증가
  ↓ NIC IRQ → SoftIRQ(ksoftirqd) → TCP Stack → Socket Buffer
  ↓ I/O 완료 시 다수 task_struct 동시 Wake-up → Runnable Queue 폭발
  ↓ Context Switch 증가 → L1/L2/TLB Cache Flush 증가
  ↓ Cache Miss → Memory Bandwidth 압박 → NUMA Remote Access 증가
  ↓ CPU Pipeline Stall → IPC 저하
  ↓ Scheduler Overhead 증가 (sy CPU 시간 증가)
  ↓ cgroup CPU Throttling (Kubernetes 환경) → 전체 Latency 균일 상승
  ↓ GC STW → 모든 Worker Thread 일시 정지 → Latency Spike
  ↓ CPU Saturation → Throughput 감소 / P99 Latency 급증
```

### CPU 효율 향상 구조 (Non-blocking 방식)

```
Event Loop + Non-blocking I/O (epoll / io_uring)
  ↓ 소수 task_struct 유지 (Event Loop Thread)
  ↓ RPS/RFS로 SoftIRQ를 여러 CPU에 분산
  ↓ Context Switch 최소화 → Cache Hot 상태 유지
  ↓ CPU Pipeline Stall 감소 → IPC 향상
  ↓ Memory Bandwidth 여유 → NUMA 영향 감소
  ↓ cgroup Throttling 여유 확보
  ↓ Blocking 작업은 boundedElastic Thread Pool로 격리
  ↓ Backpressure + Circuit Breaker로 Retry Storm 방지
  ↓ CPU 효율 증가 → Throughput 향상 / Latency 안정
```

### 핵심 요약

| 항목 | 메커니즘 실체 | SRE 대응 |
|------|-------------|---------|
| CPU Saturation 핵심 원인 | Runnable `task_struct` 과다, CFS Red-Black Tree 비용 증가, Context Switch → Cache/TLB Flush | `vmstat r`, PSI, `perf sched` |
| Thread Pool 증가의 한계 | Blocking I/O 미해소 시 Native Memory 증가, Context Switch 증가, GC Pressure 가중 | Off-CPU Flame Graph, HikariCP `pendingThreads` |
| epoll 역할 | Ready List 기반 O(1) 이벤트 감지, SoftIRQ → Socket Buffer → Wake-up 체인 | `mpstat %soft`, `ethtool -S` RPS/RFS 설정 |
| io_uring 역할 | mmap 공유 Ring Buffer로 syscall 최소화, SQPOLL로 Kernel-side Polling | Kernel 버전 확인, `io_uring_enter` syscall 빈도 |
| WebFlux에서 JDBC 위험 | Event Loop Thread Blocking → 해당 CPU 처리 마비 | `BlockHound`, Reactor `boundedElastic` 격리 |
| K8s CPU Throttling | cgroup v2 Quota 소진 → 모든 Thread Throttle Queue → Latency 균일 상승 | `cpu.stat throttled_usec`, CPU Limit 조정 |
| 핵심 모니터링 | Run Queue, Context Switch, PSI, %soft, IPC, Throttled Time, P99 Latency | `vmstat`, `perf stat`, `/proc/pressure/cpu` |

---

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*