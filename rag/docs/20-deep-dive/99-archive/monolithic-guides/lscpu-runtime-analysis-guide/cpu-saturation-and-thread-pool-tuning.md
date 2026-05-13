# CPU Saturation과 Thread Pool 설정 (E2E 분석 적용됨)

## 개요

CPU Saturation은 CPU 사용률이 높은 상태만을 의미하지 않습니다.  
CPU가 처리할 수 있는 실행 능력보다 많은 `task_struct`가 실행을 요구하면서, **Runnable Queue 대기 증가, Context Switch 증가, Scheduler Overhead 증가, Cache Miss 증가가 함께 발생하는 상태**입니다.

Thread Pool 설정은 이 흐름에서 핵심 변수입니다.  
Blocking 구조와 Non-blocking 구조는 Thread Pool을 과도하게 설정했을 때 문제가 발생하는 원인이 다릅니다.

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Hardware | CPU Pipeline이 Context Switch 시 Flush됨. Thread 수 증가 → TLB Miss 증가 → Page Table Walk 비용 증가. Cache Line Thrashing: 다수 Thread가 동일 Cache Line 경합 → MESI 무효화 반복 | `perf stat -e instructions,cycles,cache-misses,dTLB-load-misses`, `perf c2c record` (False Sharing 탐지) |
| Kernel (Scheduler) | CFS Red-Black Tree로 Runnable `task_struct` 관리. `nr_running` > CPU 수 지속 시 Saturation. 개별 Time Slice 단축 → nvcswch(비자발적 Context Switch) 급증 | `vmstat 1` (r, cs, sy), `/proc/schedstat`, `perf sched latency` |
| Kernel (cgroup) | Kubernetes CPU Limit → cgroup v2 `cpu.max` Quota/Period. Thread Pool 과다 → CPU Burst 여유 소진 → Quota 조기 고갈 → Throttling. CPU Burst(`cpu.cfs_burst_us`)로 순간 피크 완화 가능 | `/sys/fs/cgroup/cpu.stat` (`throttled_usec`), `cadvisor` `container_cpu_cfs_throttled_seconds_total` |
| Kernel (PSI) | PSI `some`: 일부 task 대기. `full`: 모든 task 대기. avg10 상승이 Saturation 조기 신호 | `/proc/pressure/cpu` (avg10/avg60/avg300) |
| JVM | Thread 수 증가 → TLAB 총 사용량 증가 → Eden 소진 빈도 증가 → Minor GC STW 증가. GC STW 재개 시 모든 Worker Thread 일제히 Runnable Queue 진입 → 순간 Saturation | `jstat -gcutil <pid> 1000`, `-Xlog:gc+tlab`, JFR `jdk.SafepointBegin` |

---

## 1. Blocking 구조와 Non-blocking 구조의 차이

| 구분 | Blocking 구조 | Non-blocking 구조 |
|------|-------------|-----------------|
| 대표 모델 | Thread-per-request | Event-loop |
| 대표 기술 | Spring MVC, Tomcat, JDBC | WebFlux, Netty, R2DBC |
| Thread 사용 방식 | 요청 하나를 Thread 하나가 전담 | 소수의 EventLoop Thread가 다수 요청 처리 |
| I/O 대기 시 | Thread가 `TASK_INTERRUPTIBLE` Sleep | Thread가 대기하지 않고 epoll 이벤트 처리 |
| Kernel 메커니즘 | `read()` Blocking → Wait Queue → IRQ Wake-up | `epoll_wait()` → Ready List 이벤트 기반 |
| 주요 위험 | Thread Pool 고갈, `task_struct` 폭증, Context Switch | CPU Throttling, EventLoop 지연, Cache Miss |
| task_struct 수 | 요청 수에 비례 | CPU Core 수 기준 소수 고정 |

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel (Thread 생성) | Java `new Thread()` → `pthread_create()` → `clone(CLONE_VM\|CLONE_THREAD\|...)` syscall → `task_struct` 생성. Thread Stack: `mmap(MAP_ANONYMOUS\|MAP_STACK)` → Native Memory 할당. Lazy Allocation: 실제 접근 시 Page Fault로 물리 메모리 할당 | `strace -e clone -p <pid>`, `/proc/<pid>/task/` (task_struct 목록), `cat /proc/sys/kernel/threads-max` |
| Kernel (epoll) | Non-blocking: `epoll_create1()` → `epoll_ctl(EPOLL_CTL_ADD)` → fd를 `ep_item`(rb-tree)에 등록. `epoll_wait()` → Ready List 비면 `task_struct` Sleep, 이벤트 발생 시 즉시 Wake-up | `strace -e epoll_ctl,epoll_wait -p <pid>`, `/proc/<pid>/fdinfo/<epoll_fd>` |
| JVM (Virtual Thread) | JDK 21+ Virtual Thread(Loom): Blocking syscall 시 Carrier Thread에서 자동 분리 → Platform Thread 미점유. `epoll` 기반으로 내부 구현. Thread-per-request 코드 변경 없이 Non-blocking 효과 | JFR `jdk.VirtualThreadPinned` (Carrier Thread 고정 탐지), `jcmd <pid> Thread.dump_to_file` |

---

## 2. Blocking 구조에서 과도한 Thread Pool이 위험한 이유

Blocking 구조에서는 하나의 요청이 하나의 Thread를 전담 점유합니다.

```
HTTP Request
  ↓  [TCP Accept Queue → Tomcat Acceptor → accept4() syscall]
  ↓ Thread Pool에서 Thread 할당 (ThreadPoolExecutor)
  ↓ Controller → Service → DB/API 호출
  ↓  [read() syscall → task_struct → TASK_INTERRUPTIBLE → Wait Queue]
  ↓  [DB 응답 → NIC IRQ → SoftIRQ → TCP Stack → Socket Buffer]
  ↓  [epoll 이벤트 → task_struct Wake-up → Runnable Queue 복귀]
  ↓ Response 반환
  ↓ Thread Pool 복귀
```

I/O가 느려지면 Thread는 응답이 올 때까지 대기 상태가 됩니다.  
Kernel 관점에서는 `task_struct`가 Wait Queue로 이동하지만, Java Thread Pool 관점에서는 해당 Thread가 아직 작업을 완료하지 않았으므로 Pool에 반환되지 않습니다.

### 2.1 Thread Pool 고갈

```
DB / API 지연
  ↓ Worker Thread 장시간 점유 (Off-CPU: Wait Queue)
  ↓ Thread Pool 가용 Thread 감소
  ↓ 신규 요청 → acceptCount 큐 대기
  ↓ acceptCount 포화 → TCP Accept Queue 적체
  ↓ Timeout 또는 연결 거부
```

### 계층별 메커니즘 실체 (Thread Pool 고갈)

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel (Socket Buffer) | DB 응답 대기 중 Worker Thread의 `task_struct`가 Socket `wait_queue_head_t`에 연결 후 `schedule()` 호출 → CPU 반환. `SO_RCVBUF` / `net.ipv4.tcp_rmem`이 수신 버퍼 크기 결정 | `ss -tnmp` (Recv-Q), `sysctl net.ipv4.tcp_rmem` |
| Kernel (TCP Backlog) | Tomcat `acceptCount` 초과 시 Kernel TCP Accept Queue 적체. `net.core.somaxconn` 한계 도달 시 SYN 패킷 DROP | `ss -lntp` (Recv-Q 증가), `netstat -s \| grep "listen queue"`, `dmesg \| grep "syn flood"` |
| Kernel (Futex) | HikariCP Connection Pool 고갈 시 Worker Thread가 `futex(FUTEX_WAIT)` syscall → `task_struct` Sleep. Lock 해제 시 `futex(FUTEX_WAKE)` → Wake-up. Uncontended Lock은 User Space Atomic만으로 처리 | `perf trace -e futex -p <pid>`, `strace -e futex -c -p <pid>` |
| JVM (Connection Pool Exhaustion) | HikariCP `maximumPoolSize` 초과 시 `connectionTimeout`(기본 30초)까지 Worker Thread Blocking. 전체 Thread Pool 고갈 → 신규 요청 503 | HikariCP JMX `pendingThreads`, `activeConnections`, APM DB Connection Wait P99 |
| JVM (Off-CPU) | Blocking 동안 Worker Thread CPU 미사용(Off-CPU Time). CPU Sampling Profiler 탐지 불가. Wall-clock / Off-CPU Profiler 필요 | `async-profiler -e wall -d 60 -f offcpu.html <pid>`, Pyroscope, Datadog Continuous Profiler |
| JVM (Thread Stack) | Blocking 중에도 Thread Stack 전체 유지. `maxThreads` × Stack Size(-Xss) 만큼 Native Memory 점유 지속. 1,000 Thread × 1MB = ~1GB Native Memory | `jcmd <pid> VM.native_memory` (Thread 항목), `pmap -x <pid>` (stack 영역) |

---

### 2.2 task_struct 증가

Java Thread가 증가하면 Linux Kernel 수준에서 그에 대응하는 OS Thread, 즉 `task_struct`도 함께 증가합니다.

```
Thread Pool 크기 증가
  ↓ Java Thread 증가  →  pthread_create() → clone() syscall
  ↓ OS Thread 증가  →  task_struct 증가
  ↓ Scheduler 관리 대상 증가  →  CFS Red-Black Tree 크기 증가
  ↓ Thread Stack  →  Native Memory(mmap) 증가
  ↓ TLAB 총 사용량 증가  →  Eden 소진 빈도 증가  →  Minor GC 증가
```

### 계층별 메커니즘 실체 (task_struct 증가)

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel (Memory) | Thread Stack: `mmap(MAP_ANONYMOUS\|MAP_STACK)`. 실제 물리 메모리는 첫 접근 시 Page Fault로 할당. `ulimit -s`가 개별 Stack 크기, `/proc/sys/kernel/threads-max`가 전체 Thread 수 제한 | `/proc/<pid>/maps` (stack 항목 수), `cat /proc/sys/vm/max_map_count` |
| Kernel (OOM Killer) | Thread Stack + JVM Heap + Metaspace + Direct Memory 합산이 cgroup Memory Limit 초과 시 Kernel OOM Killer가 JVM 프로세스 종료. `oom_score` 기반 대상 선택 | `dmesg \| grep -i oom`, `/proc/<pid>/oom_score`, K8s `OOMKilled` Pod 이벤트 |
| Kernel (THP) | Thread Stack 영역에 Transparent HugePage 적용 시 TLB Entry 절감 → TLB Miss 감소. 단, `khugepaged` 스캔으로 짧은 Latency Spike 가능 | `/sys/kernel/mm/transparent_hugepage/enabled`, `perf stat -e dTLB-load-misses` 변화 확인 |
| JVM (TLAB) | Thread 수 증가 → 각 Thread의 TLAB(Thread-Local Allocation Buffer) 총 크기 증가 → Eden 소진 빈도 증가 → Minor GC STW 빈도 증가 → Throughput 저하 | `jstat -gcutil <pid> 1000` (YGC 빈도), `-Xlog:gc+tlab`, Allocation Rate 모니터링 |
| JVM (Direct Memory) | Thread 증가 + NIO 사용 시 `ByteBuffer.allocateDirect()` → `mmap()` → Off-Heap Native Memory 증가. `-XX:MaxDirectMemorySize` 미설정 시 Native OOM 위험 | `jcmd <pid> VM.native_memory` (Direct 항목), `pmap -x <pid>` (anon 영역) |

---

## 3. Blocking 구조에서 발생하는 연쇄 문제

### 3.1 Wait Queue ↔ Runnable Queue 이동 증가

```
TASK_RUNNING
  ↓ Blocking I/O 호출 (read() syscall)
  ↓ 데이터 미준비 → TASK_INTERRUPTIBLE → Wait Queue 이동
  ↓ NIC IRQ → SoftIRQ → TCP Stack → Socket Buffer 갱신
  ↓ wake_up() → TASK_RUNNING → Runnable Queue 복귀
  ↓ CFS 스케줄링 → CPU 할당 → 실행 재개
```

많은 Thread가 동시에 I/O를 기다렸다가 동시에 깨어나면 Runnable Queue가 급격히 증가합니다.

### 계층별 메커니즘 실체 (Queue 이동)

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel (IRQ/SoftIRQ) | DB 응답 패킷: NIC DMA → Rx Ring Buffer → HW IRQ → `net_rx_action` SoftIRQ → TCP Stack → Socket Buffer → `wake_up()`. SoftIRQ(`ksoftirqd`)가 CPU를 과점하면 Wake-up 체인 전체가 지연됨 | `mpstat -I ALL 1` (`%soft`), `/proc/softirqs`, `ethtool -S eth0` (rx_missed_errors) |
| Kernel (RPS/RFS) | 기본 설정에서 모든 NIC IRQ가 CPU 0에 집중 → SoftIRQ 병목 → Wake-up 지연 → 전체 Response Time 상승. RPS(Receive Packet Steering)로 패킷 처리를 여러 CPU에 분산 | `/proc/irq/<N>/smp_affinity`, `ethtool -L eth0 combined <N>` |
| Kernel (run_delay) | Runnable Queue에 복귀한 `task_struct`가 실제 CPU를 받을 때까지 대기하는 시간(`run_delay`). Runnable Queue 과부하 시 `run_delay` 증가 → Response Latency 직접 상승 | `/proc/<pid>/sched` (run_delay 누적값), `perf sched latency` (avg/max 대기 시간) |
| JVM (cswch) | I/O 대기 후 Wake-up 시 자발적 Context Switch(cswch) 발생. I/O 완료 → Wake-up → CPU 할당 대기 → CPU 실행 재개의 전체 지연이 Off-CPU Time | `pidstat -w -p <pid> 1` (cswch/s), `async-profiler -e wall` |

---

### 3.2 Thundering Herd

I/O 응답이 한꺼번에 완료되면 Wait Queue에 있던 다수의 `task_struct`가 동시에 Runnable 상태로 복귀합니다.

```
다수의 I/O 완료 (DB 배치 응답, External API 동시 응답)
  ↓ 다수의 Socket에서 동시 epoll 이벤트 발생
  ↓ 다수의 task_struct 동시 Wake-up
  ↓ Runnable Queue 급증 (nr_running >> CPU 수)
  ↓ CFS Scheduler 부하 증가 (Red-Black Tree 동시 삽입)
  ↓ CPU Core 수보다 훨씬 많은 task가 동시 CPU 요구
  ↓ run_delay 급증 → Latency Spike
```

### 계층별 메커니즘 실체 (Thundering Herd)

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel (Scheduler) | 다수 `task_struct` 동시 Wake-up → CFS Red-Black Tree 동시 삽입 → `vruntime` 갱신 폭증 → `sy` CPU Time 급증. Load Balancing(`load_balance()`)이 task를 여러 CPU에 분산 시도하나 Migration 자체가 Cache Miss 유발 | `vmstat 1` (r 컬럼 순간 폭발, cs 급증), `perf sched latency` (max 대기 시간) |
| Kernel (NUMA) | Thundering Herd 발생 시 Scheduler가 원격 NUMA Node CPU에 task 배치 가능 → Remote Memory Access(~100~300ns) → Latency 추가 증폭 | `numastat -p <pid>` (remote 메모리 접근 비율), `perf stat -e node-load-misses` |
| Kernel (CPU Frequency) | Thundering Herd 후 CPU C-state(Deep Sleep)에서 Active 상태로 전환 시 P-state 주파수 상승에 수십~수백 μs 소요. `performance` Governor 설정 시 완화 가능 | `cpupower frequency-info`, `turbostat --Summary 1`, `/sys/devices/system/cpu/cpufreq/scaling_governor` |
| JVM (GC Safepoint) | GC STW 완료 후 모든 Worker Thread 일제히 Runnable 복귀 → Thundering Herd 패턴과 동일. `ttspTime`(Time To Safepoint)이 길면 일부 Thread가 오래 대기 후 한꺼번에 재개 | `-Xlog:safepoint*`, JFR `jdk.SafepointBegin` `ttspTime` 필드 |

---

### 3.3 Context Switch 증가

Runnable task가 많아지면 CPU는 실행 대상을 자주 교체해야 합니다.

Context Switch 시 저장·복구되는 정보는 다음과 같습니다.

| 항목 | Kernel 구조체 | 설명 |
|------|-------------|------|
| Program Counter | `task_struct->thread.ip` | 다음 실행 명령어 위치 |
| Stack Pointer | `task_struct->thread.sp` | Stack 상태 |
| General Registers | `pt_regs` 구조체 | 연산 중간 데이터 |
| CPU Flags | `pt_regs->flags` | ZF, CF 등 상태 플래그 |
| Memory Context | CR3 레지스터 (`mm_struct->pgd`) | 가상 메모리 페이지 테이블 포인터 |
| FPU / SIMD 상태 | `fpu->state` | 부동소수점 / 벡터 연산 상태 (Lazy Save) |
| FS/GS Segment | `thread.fsbase` | Thread-Local Storage(TLS) 베이스 주소 |

이 작업은 `__schedule()` → `context_switch()` 함수 체인으로 Kernel Mode에서만 수행되며 CPU Cycle을 소비합니다.

### 계층별 메커니즘 실체 (Context Switch)

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Hardware (TLB Flush) | 동일 프로세스 내 Thread 전환: 주소 공간 공유 → TLB Flush 없음(PCID/ASID 지원 CPU). 프로세스 간 전환: CR3 갱신 → TLB Flush → 이후 메모리 접근마다 Page Table Walk 필요 | `perf stat -e dTLB-load-misses,dTLB-store-misses,iTLB-load-misses` |
| Hardware (CPU Pipeline) | Context Switch 후 CPU Branch Predictor와 Pipeline이 새 `task_struct` 실행 경로로 재충전 필요 → Frontend/Backend Stall 증가 → IPC 저하 | `perf stat -e stalled-cycles-frontend,stalled-cycles-backend,branch-misses` |
| Hardware (Cache Line Thrashing) | 다수 Thread가 동일 Cache Line(64 Byte) 공유 쓰기 시 False Sharing 발생 → MESI Protocol 무효화 반복 → 성능 저하. Java의 공유 카운터, `AtomicLong`, Queue Head/Tail Pointer 등 주의 | `perf c2c record && perf c2c report` (Hot Cache Line 탐지), `-XX:+UseContendedPadding` |
| Kernel (nvcswch) | 비자발적 Context Switch(nvcswch): Timer IRQ에 의한 강제 전환. Blocking 구조에서 Thread 수 증가 → 개별 Time Slice 단축 → nvcswch 급증 = CPU Saturation 직접 신호 | `pidstat -w -p <pid> 1` (nvcswch/s 비율), `vmstat 1` (cs 컬럼) |
| JVM (Monitor/Lock) | `synchronized` 경합 시 `objectmonitor` → `futex(FUTEX_WAIT)` → Context Switch. Lock 경합이 Context Switch의 주요 원인 중 하나. Contention이 높은 Lock은 `ReentrantLock` + `tryLock()` 또는 Lock-free 구조로 교체 권장 | `jstack` (BLOCKED Thread 수), JFR `jdk.JavaMonitorWait`, `perf trace -e futex` |

---

### 3.4 Cache Miss 증가

Thread가 자주 교체되면 CPU Cache Locality가 약해집니다.

```
task_struct A 실행 → A의 데이터가 L1/L2 Cache에 적재 (Hot)
  ↓ Context Switch
task_struct B 실행 → B의 데이터가 Cache에 없음 → L1/L2 Miss → L3 Miss → RAM 접근
  ↓ RAM 접근 (~100ns) → CPU Pipeline Stall
  ↓ NUMA Remote Memory인 경우 추가 ~200ns 지연
  ↓ IPC(Instructions Per Cycle) 저하
  ↓ Memory Bandwidth 수요 증가 → Memory Bus 경합 → Memory Bandwidth Saturation
```

### 계층별 메커니즘 실체 (Cache Miss)

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Hardware (L1/L2/L3) | L1 Cache(32KB): Context Switch 후 Cold Start. 고빈도 Context Switch → L1/L2 항상 Cold 상태 유지. L3 Cache(공유): 다수 Thread 동시 경합 시 Cache Thrashing | `perf stat -e L1-dcache-load-misses,LLC-load-misses,LLC-store-misses` |
| Hardware (Memory Bandwidth) | 동시 Cache Miss 증가 → RAM 접근 폭증 → Memory Bus 경합 → Memory Bandwidth Saturation. Intel Xeon 단일 소켓 기준 ~100~200GB/s 한계 초과 시 CPU가 메모리 응답 대기 → Stall | `perf stat -e LLC-load-misses`, `pcm-memory` (Intel PCM), `numastat` (NUMA 메모리 접근 분포) |
| Hardware (NUMA) | Thread가 NUMA Node 0에 생성되었으나 메모리가 Node 1에 있는 경우 Remote Memory Access 발생. Context Switch 후 다른 NUMA Node CPU에 배치 시 Cache + NUMA 이중 패널티 | `numastat -p <pid>`, `numactl --hardware`, `perf stat -e node-load-misses` |
| Kernel (CPU Migration) | `load_balance()`가 부하 분산 목적으로 `task_struct`를 다른 CPU로 Migration → Cache 무효화 + TLB 재충전 비용. `taskset -c <cpu-list>` 또는 `numactl -C` 로 CPU 고정 시 Migration 방지 | `perf stat -e cpu-migrations -p <pid>`, `/proc/schedstat` (cpu_nr_migrations) |
| JVM (JIT Optimization) | JIT C2 Compiler가 Hot Method를 Native Code로 컴파일 후 CPU Cache에 상주. 과도한 Context Switch로 JIT Code Cache가 자주 교체되면 JIT 최적화 효과 감소. Code Cache 포화 시 JIT 중단 → Interpreter 실행 | `-XX:+PrintCompilation`, `jcmd <pid> VM.code_cache`, JFR `jdk.Compilation` |

---

## 4. Scheduler Overhead 증가

Linux CFS Scheduler는 Runnable 상태의 `task_struct`를 관리합니다.  
Runnable task가 많아질수록 다음 비용이 증가합니다.

| 비용 항목 | Kernel 메커니즘 | 설명 |
|----------|----------------|------|
| Runnable Queue 관리 | `struct rq`, `rq->nr_running` | 각 CPU Core별 Runnable Queue 크기 증가 |
| Red-Black Tree 탐색 | `tasks_timeline` rb-tree | 최소 `vruntime` task 선택: O(log n). n 증가 시 탐색 비용 증가 |
| `vruntime` 계산 | `update_curr()` 함수 | 각 task 누적 실행 시간 및 우선순위 반영 갱신 |
| Context Switch 수행 | `__schedule()` → `context_switch()` | 레지스터 저장/복구 + Kernel Mode 진입 비용 |
| Load Balancing | `load_balance()` | CPU 간 task Migration. Migration 시 Cache Miss 유발 |

```
Runnable task_struct 증가
  ↓ CFS Red-Black Tree 관리 비용 증가 (sy 시간 상승)
  ↓ 비즈니스 로직 실행 시간(us) 감소
  ↓ Throughput 감소 / P99 Latency 급증
```

### 계층별 메커니즘 실체 (Scheduler Overhead)

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel (CFS Tuning) | `sched_latency_ns`(기본 6~24ms): 전체 Runnable task를 한 번씩 실행하는 주기. Thread 수 증가 시 개별 Time Slice = `sched_latency_ns` / `nr_running` 으로 단축 → 전환 빈도 증가. `sched_min_granularity_ns`가 Time Slice 하한 | `sysctl kernel.sched_latency_ns`, `/proc/sched_debug`, `perf sched record && perf sched report` |
| Kernel (cgroup Throttling) | Blocking 구조에서 Thread 증가 + CPU 집중 사용 → `cpu.max` Quota 조기 소진 → 남은 Period 동안 전체 Throttle. Throttling 중 Runnable task들이 실행 불가 상태로 대기 → Latency 균일 상승 | `/sys/fs/cgroup/cpu.stat` (`nr_throttled`, `throttled_usec`), `kubectl describe node` |
| Kernel (PSI) | `sy` 증가 + Runnable Queue 과부하 → PSI `cpu.some` 상승. `cpu.full`: 모든 Task가 CPU를 기다리는 극단 상태 → Throughput 완전 정체 | `cat /proc/pressure/cpu`, Kubernetes PSI 기반 HPA 설정 |
| JVM (Safepoint Overhead) | Thread 수 증가 → Safepoint 도달 대기 시간(TTSP) 증가. Thread가 많을수록 모든 Thread를 안전 지점에 모으는 데 걸리는 시간 증가 → STW 실제 정지 시간 연장 | `-Xlog:safepoint*` (`ttspTime` 필드), JFR `jdk.SafepointBegin/End` |

---

## 5. Blocking 구조의 CPU Saturation 전체 흐름

```
Blocking I/O 증가 (DB Slow Query, 외부 API Timeout)
  ↓ Worker Thread 장시간 점유 (Off-CPU: Wait Queue)
  ↓ Thread Pool 가용 슬롯 감소 → Thread Pool 크기 확장 시도
  ↓ Java Thread 증가 → clone() syscall → task_struct 증가
  ↓ Thread Stack: mmap() → Native Memory 증가 → RSS 상승
  ↓ TLAB 총 사용량 증가 → Eden 소진 → Minor GC STW 증가
  ↓ Wait Queue / Runnable Queue 이동 빈도 증가
  ↓ I/O 완료 동시 Wake-up → Thundering Herd → Runnable Queue 급증
  ↓ Context Switch 증가 → TLB Flush + L1/L2 Cache Cold Start
  ↓ Cache Miss 증가 → RAM 접근 폭증 → Memory Bandwidth Saturation
  ↓ CPU Pipeline Stall → IPC 저하
  ↓ CFS Scheduler Red-Black Tree 관리 비용 증가 → sy 상승
  ↓ cgroup CPU Quota 조기 소진 → Container Throttling
  ↓ CPU Saturation → Throughput 감소 / P99 Latency 급증
```

---

## 6. Non-blocking 구조에서 과도한 Thread Pool이 위험한 이유

Non-blocking 구조의 기본 전제는 소수의 EventLoop Thread로 많은 요청을 처리하는 것입니다.

```
소수의 EventLoop Thread (= Logical CPU 수)
  ↓ epoll_wait() → 이벤트 발생 시 즉시 처리
  ↓ 동일 Thread가 CPU를 오래 점유 (Cache Hot 유지)
  ↓ Context Switch 최소화
  ↓ Runnable Queue 안정화
```

이 구조에서는 Thread 수가 많을 필요가 없습니다.  
EventLoop Thread를 과도하게 늘리면 Non-blocking 구조의 장점이 약해집니다.

### 6.1 Runnable Queue의 인위적 증가

```
Logical CPU = 8, EventLoop Thread = 200
  ↓ 8개만 동시 Running 가능
  ↓ 192개는 Runnable Queue에서 대기 (run_delay 발생)
  ↓ Scheduler가 불필요하게 200개 task 관리
  ↓ Time Slice 단축 → nvcswch 급증
```

### 계층별 메커니즘 실체 (Runnable Queue 인위적 증가)

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel (CFS) | 200개의 Runnable `task_struct`가 8 Core에 스케줄링됨. 개별 Time Slice = `sched_latency_ns` / 200 ≈ 0.03~0.12ms. 매우 짧은 Time Slice → 극도로 잦은 Context Switch | `/proc/sched_debug` (nr_running per CPU), `perf sched latency` (avg wait time) |
| Kernel (Memory) | 200개 Thread Stack: 200 × 512KB~1MB = 100~200MB Native Memory. 사용하지 않는 Thread라도 Stack 물리 메모리 점유(첫 접근 후 Page Fault로 할당된 페이지는 유지) | `pmap -x <pid>` (stack 영역 합산), `cat /proc/<pid>/status` (VmRSS) |
| JVM (Netty) | Netty의 `NioEventLoopGroup` 기본값: `availableProcessors × 2`. CPU 수에 맞게 설정하는 것이 최적. 과도한 설정은 Non-blocking 이점을 Blocking 구조 수준으로 저하 | Netty `workerGroup` 설정, JMX Thread Pool 크기 확인 |

---

### 6.2 Involuntary Context Switch 증가

Non-blocking 구조에서는 하나의 EventLoop Thread가 CPU를 오래 점유하며 이벤트를 빠르게 처리하는 것이 유리합니다.  
하지만 Thread가 너무 많으면 Scheduler가 공정성을 위해 강제로 실행 대상을 교체합니다.

```
EventLoop Thread 실행 중 (epoll 이벤트 처리)
  ↓ 다른 Runnable Thread 다수 존재 (Time Slice 경쟁)
  ↓ Timer IRQ → CFS Preemption 발생
  ↓ nvcswch(비자발적 Context Switch) 발생
  ↓ EventLoop Thread 강제 중단 → Runnable Queue 복귀
  ↓ EventLoop 연속 실행성 저하 → 이벤트 처리 지연
```

### 계층별 메커니즘 실체 (nvcswch 증가)

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel (Preemption) | CFS `sched_wakeup_granularity_ns`: Wake-up 후 현재 실행 중인 task를 선점하는 데 필요한 최소 `vruntime` 차이. Thread 수 증가 → 선점 빈도 증가. `sched_latency_ns` 및 `sched_min_granularity_ns` 조정으로 완화 가능 | `sysctl kernel.sched_wakeup_granularity_ns`, `/proc/sched_debug` |
| Kernel (CPU Affinity) | EventLoop Thread를 특정 CPU Core에 고정(`taskset`, `numactl`)하면 다른 Thread의 선점 경합 감소 + Cache Locality 향상. Kubernetes `cpuset` cgroup으로 CPU 고정 가능 | `taskset -c 0,1,2,3 java ...`, `/sys/fs/cgroup/cpuset.cpus` |
| JVM (Blocking in EventLoop) | EventLoop Thread에서 Blocking 작업(JDBC, `Thread.sleep()`, 동기 파일 I/O) 직접 실행 시 해당 CPU의 모든 이벤트 처리 즉시 마비. `Schedulers.boundedElastic()`으로 Blocking 작업 격리 필수 | Netty Blocked Event Loop 경고 로그, `async-profiler -e wall` (EventLoop Thread CPU 점유 분석), Reactor `BlockHound` |

---

### 6.3 Cache Locality 저하

EventLoop Thread가 자주 교체되면 CPU Cache에 유지되던 데이터가 무효화됩니다.

```
EventLoop A 실행 → Cache Warm-up (epoll 내부 구조, Socket Buffer, 처리 중인 요청 데이터)
  ↓ Context Switch (nvcswch: 다른 Runnable Thread에 의한 선점)
EventLoop B 실행 → Cache Miss 증가 (B의 데이터로 Cache 교체)
  ↓ EventLoop A 재실행 → Cache Miss 재발 (A 데이터가 교체됨)
  ↓ Non-blocking 구조의 높은 CPU 효율이 약화됨
```

### 계층별 메커니즘 실체 (Cache Locality 저하)

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Hardware (L1/L2) | EventLoop Thread가 처리하는 Socket FD, `ep_item`, `sk_buff` 등의 데이터가 L1/L2 Cache에 상주해야 고성능 유지. 잦은 Context Switch → Cache Eviction → 다음 실행 시 Cache Miss | `perf stat -e L1-dcache-load-misses,LLC-load-misses -p <pid>` |
| Hardware (IPC) | Cache Miss 증가 → CPU가 RAM 응답 대기 → Backend Stall 증가 → IPC 저하. Non-blocking 구조의 설계 목표인 IPC 향상 효과가 Thread 과다 설정으로 상쇄됨 | `perf stat -e instructions,cycles -p <pid>` (IPC = instructions / cycles) |
| Kernel (CPU Migration) | Thread 과다 → Scheduler `load_balance()`가 EventLoop Thread를 다른 NUMA Node CPU로 Migration → Cache + NUMA 이중 패널티. CPU Affinity 설정으로 Migration 방지 | `perf stat -e cpu-migrations -p <pid>`, `numastat` |

---

## 7. Non-blocking 구조와 Kubernetes CPU Throttling

Non-blocking 구조는 적은 수의 Thread가 CPU를 밀도 높게 사용합니다.  
이 특성은 Kubernetes CPU Limit과 결합될 때 CPU Throttling을 유발할 수 있습니다.

```
EventLoop가 짧은 시간에 CPU 집중 사용
  ↓ cgroup CFS Bandwidth: Quota = cpu.max 분자 값
  ↓ 짧은 Period 내 Quota 빠르게 소진
  ↓ 남은 Period 동안 Container 내 모든 task_struct Throttle Queue 이동
  ↓ EventLoop 완전 정지 (CPU 여유가 있어도)
  ↓ 진행 중인 연결 + 신규 요청 모두 지연
  ↓ Latency: P99/P999 균일 상승 (Throttling 특징적 패턴)
```

EventLoop Thread가 멈추면 신규 요청뿐 아니라 기존 연결의 후속 처리도 함께 지연됩니다.

### 계층별 메커니즘 실체 (CPU Throttling)

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel (CFS Bandwidth) | cgroup v2 `cpu.max` = `$QUOTA $PERIOD`. 기본 Period = 100ms. EventLoop가 Quota를 빠르게 소진하면 나머지 Period 동안 Throttle. Throttle 중 `task_struct`는 `TASK_RUNNING`이지만 CPU 배정 불가 | `/sys/fs/cgroup/cpu.stat` (`nr_throttled`, `throttled_usec`), `cat /sys/fs/cgroup/cpu.max` |
| Kernel (CPU Burst) | `cpu.cfs_burst_us` (cgroup v2 Burst): 이전 Period에서 미사용한 Quota를 최대 Burst 값까지 적립. 피크 구간에 적립된 Quota 추가 사용 가능 → Throttling 완화. Kubernetes 1.27+에서 `resources.requests.cpu` 설정 시 활용 가능 | `/sys/fs/cgroup/cpu.max.burst`, K8s `alpha.kubernetes.io/cfs-quota-period` annotation |
| Kernel (PSI) | Throttling 발생 시 PSI `cpu.some` / `cpu.full` 상승. `full` 상승은 모든 Task가 대기 중임을 의미 → Throughput 완전 정체 신호 | `/proc/pressure/cpu`, Prometheus `node_pressure_cpu_waiting_seconds_total` |
| Application (Throttling 감지) | Throttling은 CPU 사용률 그래프에 명확히 나타나지 않음. `throttled_usec` 증가 또는 P99/P999 Latency 균일 상승 패턴으로 간접 감지. APM에서 특정 구간 Latency가 배수로 상승하면 Throttling 의심 | Grafana `container_cpu_cfs_throttled_seconds_total` (cadvisor), Datadog `kubernetes.cpu.throttled` |

---

## 8. CPU Burst

CPU Burst는 Linux CFS Bandwidth Control에서 **순간적인 CPU 사용량 증가를 완화**하기 위한 기능입니다.  
이전 주기에서 사용하지 않은 CPU 시간을 일정 범위 내에서 축적하고, 피크 구간에 사용할 수 있게 합니다.

```
이전 Period에서 CPU Quota 일부 미사용
  ↓ Burst 여유분 축적 (cpu.cfs_burst_us 한도까지)
  ↓ 순간 피크 발생 (EventLoop 이벤트 폭증)
  ↓ Quota + Burst 여유분 함께 사용
  ↓ Throttling 발생 지연 또는 완화
  ↓ Tail Latency(P99/P999) 안정화
```

Non-blocking 서버처럼 짧은 시간에 CPU를 집중적으로 사용하는 구조에서 Tail Latency 안정화에 도움이 됩니다.

### 계층별 메커니즘 실체 (CPU Burst)

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel (CFS Burst) | `cpu.cfs_burst_us`: 최대 누적 가능한 Burst 시간. `runtime_burst` 변수로 Kernel 내부 관리. Burst 소진 후에는 일반 Quota 동작으로 복귀 | `cat /sys/fs/cgroup/cpu.max.burst` (cgroup v2), `/sys/fs/cgroup/cpu/cpu.cfs_burst_us` (cgroup v1) |
| Kernel (Period 조정) | CPU Throttling이 잦은 경우 Period 단축(`cpu.cfs_period_us`) 전략도 유효. Period를 10ms로 줄이면 Quota 소진 후 더 빠르게 복구. 단, Scheduler 오버헤드 증가 가능 | `echo "100000 10000" > /sys/fs/cgroup/cpu.max` (Period 10ms 예시) |
| Kubernetes | K8s에서 CPU Burst 활성화: `--cpu-cfs-quota-period` kubelet 설정 또는 Pod annotation. CPU Request 기반 Burst 자동 계산 가능 (K8s 1.27+ Feature Gate `CPUCFSQuotaPeriod`) | `kubelet --cpu-cfs-quota-period=10ms`, Kubernetes Pod `resources.requests.cpu` 설정 |

---

## 9. Blocking vs Non-blocking 과도한 Thread Pool 비교

| 구분 | Blocking 구조 | Non-blocking 구조 |
|------|-------------|-----------------|
| Thread를 늘리는 이유 | I/O 대기 중인 Thread 공백을 메우기 위해 | 일반적으로 늘릴 이유가 거의 없음 |
| 주요 위험 | Thread Pool 고갈, `task_struct` 폭증, Thundering Herd | Runnable Queue 인위적 증가, EventLoop 효율 저하 |
| 주요 Queue 이동 | Wait Queue ↔ Runnable Queue 빈번한 이동 | Runnable Queue 내부 경쟁 (대부분 Runnable 상태 유지) |
| 주요 비용 | Context Switch, Native Memory(Stack), Scheduler Overhead, Cache Miss | nvcswch(비자발적 CS), Cache Miss, CPU Throttling |
| Memory 비용 | Thread 수 × Stack Size → Native Memory 선형 증가 | 소수 Thread → Native Memory 최소 |
| GC 영향 | TLAB 증가 → Minor GC 빈도 증가 → STW Latency | Thread 수 적음 → GC 영향 낮음 |
| CPU Throttling 패턴 | Thread 폭증 후 CPU 집중 사용 → Quota 소진 | 상시 CPU 집중 사용 → Quota 조기 소진 |
| 임계점 | Thread 수 수백~수천 개 도달 시 | Core 수보다 수십 배 이상 과다 설정 시 |
| 해결 방향 | Timeout, Bulkhead, Circuit Breaker, Backpressure, Pool 크기 제한 | Core 수 기반 Thread 설정, CPU Limit 조정, CPU Burst 활용 |

---

## 10. SRE 관점 주요 지표

| 지표 | 계층 | 의미 | 임계 신호 |
|------|------|------|---------|
| JVM Thread Count | JVM | Java Thread 총 수 | 급격한 증가: Blocking I/O 증가 또는 Thread Leak |
| Native Thread Count | OS | `task_struct` 수 = `/proc/<pid>/task/` 항목 수 | JVM Thread Count와 비례해야 정상 |
| Runnable Thread 수 | JVM / Kernel | CPU 실행 경쟁 중인 Thread 수 | `busyThreads == maxThreads`: Pool 고갈 임박 |
| Waiting Thread 수 | JVM / Kernel | I/O 또는 Lock 대기 Thread 수 (`WAITING`, `BLOCKED`) | 다수: Blocking I/O 또는 Lock 경합 |
| Context Switches (cs) | Kernel | 실행 전환 횟수/초 | 수만/초 이상 지속: Saturation 또는 Lock 경합 |
| nvcswch | Kernel | 비자발적 CS (CPU Saturation 직접 신호) | 급증: Runnable Queue 과부하 |
| Run Queue Length (r) | Kernel | Runnable `task_struct` 수 | > CPU 코어 수 × 2 지속: CPU Saturation |
| CPU `sy` | Kernel | Kernel Scheduler 작업 비중 | > 20% 지속: Scheduler Overhead 의심 |
| CPU `us` | Application | App 로직 실행 비중 | 낮음 + sy 높음: Scheduling에 CPU 낭비 |
| Cache Miss Rate | Hardware | L1/L2/L3 Hit 실패율 | 급격한 상승: Context Switch 과다 또는 Thread 폭증 |
| IPC | Hardware | Cycle당 명령어 수 | < 1.0: Memory/Pipeline 병목 |
| CPU Throttling | Kernel (cgroup) | Quota 소진 시간 | `throttled_usec` 증가: CPU Limit 조정 필요 |
| Off-CPU Time | JVM / Kernel | Blocking으로 CPU 미사용 시간 비율 | Wall >> CPU 시간: Blocking I/O 병목 |
| P99 / P999 Latency | Application | 사용자 관점 응답 지연 | 급격한 상승: Saturation 또는 Throttling |

### 주요 진단 명령어

```bash
# Thread 수 / task_struct 수
ls /proc/<PID>/task | wc -l                           # OS Thread 수
jstack <PID> | grep -c "java.lang.Thread.State"       # JVM Thread 수
jstack <PID> | grep "BLOCKED\|WAITING" | wc -l        # 대기 Thread 수

# Context Switch / Scheduler
vmstat 1                                              # r, cs, sy, us 동시 관찰
pidstat -w -p <PID> 1                                 # cswch/s(자발) vs nvcswch/s(비자발)
perf sched record -p <PID> -- sleep 10
perf sched latency                                    # 평균/최대 Scheduler 대기 시간

# CPU / Cache / IPC
perf stat -e \
  instructions,cycles,\
  cache-misses,cache-references,\
  L1-dcache-load-misses,LLC-load-misses,\
  dTLB-load-misses,\
  stalled-cycles-backend,branch-misses \
  -p <PID>

# CPU Throttling (Kubernetes)
cat /sys/fs/cgroup/cpu.stat                           # nr_throttled, throttled_usec
kubectl top pod                                        # Pod CPU 사용량
# cadvisor 메트릭: container_cpu_cfs_throttled_seconds_total

# CPU Pressure
cat /proc/pressure/cpu                                # PSI some/full avg10/60/300

# Off-CPU / Wall-clock 프로파일링
async-profiler -e wall -d 60 -f offcpu.html <PID>     # Off-CPU Flame Graph

# JVM Memory (Thread Stack, Direct)
jcmd <PID> VM.native_memory                           # Thread Stack + Direct Memory
pmap -x <PID> | grep stack                            # 개별 Stack 크기

# Connection Pool
# HikariCP JMX: pendingThreads, activeConnections, idleConnections

# NUMA
numastat -p <PID>                                     # Remote 메모리 접근 비율
numactl --hardware                                    # NUMA 토폴로지
```

---

## 11. 운영 대응 방향

### 11.1 Blocking 구조

Thread Pool을 무작정 늘리는 것은 CPU Saturation을 악화시킵니다.  
아래 순서로 대응합니다.

| 순서 | 대응 | 계층 메커니즘 |
|------|------|-------------|
| 1 | Timeout 설정 | Blocking 시간 상한 → Off-CPU Time 제한 → Thread Pool 슬롯 조기 반환 |
| 2 | Circuit Breaker 적용 | 외부 서비스 지연 시 빠른 실패 반환 → Thread Pool 점유 시간 단축. Retry Storm 방지 | 
| 3 | Bulkhead 적용 | 서비스별 Thread Pool 분리 → 하나의 지연이 전체 Thread Pool 고갈 방지 |
| 4 | DB Connection Pool 제한 | HikariCP `maximumPoolSize` 제한 → Connection Exhaustion 시 빠른 실패 반환 |
| 5 | Bounded Queue 사용 | `acceptCount` 및 내부 큐 크기 제한 → 무한 대기 방지 |
| 6 | Backpressure 적용 | 수용 가능한 요청 수만 허용 → TCP Accept Queue 적체 방지 |
| 7 | Thread Pool 크기 제한 | `maxThreads` = DB Pool Size × 2 + 여유 (Little's Law 기반 산정) |
| 8 | 필요 시 Non-blocking 전환 검토 | JDBC → R2DBC, Spring MVC → WebFlux + Virtual Thread |

### 11.2 Non-blocking 구조

EventLoop Thread 수를 Core 수에 맞추는 것이 핵심입니다.  
아래 순서로 대응합니다.

| 순서 | 대응 | 계층 메커니즘 |
|------|------|-------------|
| 1 | EventLoop Thread 수 = Logical CPU 수 | nvcswch 최소화, Cache Locality 유지 |
| 2 | Blocking 작업 격리 | JDBC 등 → `Schedulers.boundedElastic()` / 전용 Worker Pool → EventLoop 보호 |
| 3 | EventLoop Blocking 탐지 | `BlockHound`, Netty Blocked EventLoop 경고 로그 모니터링 |
| 4 | CPU Limit = 피크 기준으로 설정 | 평균 기준 Limit 설정 시 피크 구간 Throttling → Tail Latency 급증 |
| 5 | CPU Throttling 지표 모니터링 | `throttled_usec` > 0: CPU Limit 상향 또는 Burst 설정 검토 |
| 6 | CPU Burst 설정 | 순간 피크 완화. `cpu.cfs_burst_us` 설정 또는 K8s CPU Burst 활성화 |
| 7 | Backpressure 적용 | Reactor `onBackpressureBuffer()`, `onBackpressureDrop()` 등으로 이벤트 폭주 제어 |

---

## 12. 핵심 정리

### Blocking 구조의 CPU Saturation 흐름

```
Blocking I/O 증가
  ↓ Thread Pool 점유 → clone() → task_struct 증가
  ↓ mmap(MAP_STACK) → Native Memory 증가 → RSS 상승
  ↓ TLAB 증가 → Minor GC STW 빈도 증가
  ↓ Wait Queue ↔ Runnable Queue 이동 빈도 증가
  ↓ Thundering Herd → Runnable Queue 급증
  ↓ Context Switch 증가 → TLB Flush + L1/L2 Cache Miss
  ↓ Memory Bandwidth Saturation → CPU Pipeline Stall → IPC 저하
  ↓ CFS Scheduler sy 증가 → cgroup Throttling
  ↓ CPU Saturation → Throughput 감소
```

### Non-blocking 구조의 Thread 과다 설정 흐름

```
EventLoop Thread 과다 설정
  ↓ Runnable Queue 인위적 증가
  ↓ nvcswch(비자발적 CS) 증가 → Cache/TLB Eviction
  ↓ Cache Miss 증가 → IPC 저하
  ↓ Scheduler Overhead 증가 → sy 상승
  ↓ CPU 집중 사용 → cgroup Quota 조기 소진 → Throttling
  ↓ CPU 효율 저하 → Non-blocking 이점 상쇄
```

### 핵심 요약

| 항목 | 메커니즘 실체 | SRE 대응 |
|------|-------------|---------|
| Blocking 구조 Thread 제한 | Thread 수 = `task_struct` 수. 증가 시 Context Switch, Native Memory, GC Pressure 연쇄 증가 | Off-CPU Flame Graph, HikariCP `pendingThreads`, Circuit Breaker |
| Non-blocking Thread 최소화 | EventLoop Thread = Logical CPU 수. 초과 시 nvcswch 증가, Cache Locality 저하 | `pidstat -w` nvcswch/s, `perf stat` IPC, CPU Affinity 설정 |
| Thundering Herd | 다수 I/O 동시 완료 → Runnable Queue 급증 → Scheduler 부하 + NUMA Migration | `perf sched latency` (max wait), NUMA Binding |
| K8s CPU Throttling | cgroup Quota 소진 → Throttle Queue → 전체 Latency 균일 상승 | `throttled_usec`, CPU Burst 설정, CPU Limit 상향 |
| Cache / IPC 저하 | Context Switch → L1/L2 Cache Cold → Memory Bandwidth 압박 → Pipeline Stall | `perf stat instructions,cycles,cache-misses`, `perf c2c` |
| GC 영향 | Thread 수 증가 → TLAB 총 증가 → Eden 소진 → STW. Safepoint 재개 시 Runnable 폭발 | `jstat -gcutil`, `-Xlog:safepoint`, GC Allocation Rate |

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*