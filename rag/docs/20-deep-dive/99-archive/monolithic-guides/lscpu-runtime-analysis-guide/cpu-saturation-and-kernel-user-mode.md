# CPU Saturation, Context Switch, Blocking I/O, User Mode / Kernel Mode (E2E 분석 적용됨)

## 1. 개요

Linux 기반 시스템에서 CPU Saturation은 단순히 CPU 사용률이 높은 상태만을 의미하지 않습니다.

특히 Thread-per-request 기반 Blocking 구조에서는 실제 비즈니스 로직보다 아래 항목들이 CPU 자원을 과도하게 소비하면서 시스템 전체 Throughput이 급격히 저하될 수 있습니다.

- Context Switch
- Scheduler Overhead
- Mode Transition (User ↔ Kernel)
- CPU Cache Miss

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Hardware | CPU Pipeline이 Context Switch 시 Flush됨. Branch Misprediction 및 Cache Miss로 IPC(Instructions Per Cycle) 저하 발생. Hyper-Threading 환경에서 논리 Core 수가 실제 처리 능력을 과대평가하게 만들 수 있음 | `perf stat -e instructions,cycles` (IPC 계산), `perf stat -e stalled-cycles-backend` |
| Kernel (Scheduler) | CFS(Completely Fair Scheduler)가 Runnable Queue(`rq`)의 `task_struct`를 Red-Black Tree로 관리. `nr_running`이 CPU 수를 지속 초과하면 Saturation 상태. Scheduling 결정 자체가 `sy` CPU Time 소비 | `vmstat 1` (r, sy 컬럼), `/proc/schedstat`, `perf sched latency` |
| Kernel (cgroup) | Kubernetes CPU Limit → cgroup v2 `cpu.max`(Quota/Period). Quota 소진 시 Container 내 모든 `task_struct`가 Throttle Queue로 이동 → CPU 사용률이 낮아도 Latency 급증 | `/sys/fs/cgroup/cpu.stat` (`throttled_usec`), `cadvisor` `container_cpu_cfs_throttled_seconds_total` |
| Kernel (PSI) | PSI(Pressure Stall Information) `some`/`full` 값이 Saturation 조기 신호 제공. `full`은 모든 Task가 CPU를 기다리는 극단적 상태 | `/proc/pressure/cpu` (avg10/avg60/avg300 추이) |
| JVM | GC STW, JIT Safepoint, Biased Lock Revocation에서 모든 Worker Thread 일시 정지. 재개 시 Runnable Queue 순간 폭발 → 순간 CPU Saturation 유발 | `-Xlog:safepoint*`, JFR `jdk.SafepointBegin/End`, GC log STW 시간 |

---

## 2. Thread-per-request 모델

하나의 요청(Request)마다 하나의 Thread가 전담하여 처리하는 구조입니다. 대표 사례로는 Tomcat 기반 Spring MVC가 있습니다.

```
HTTP Request 도착
  ↓  [TCP Accept Queue → Tomcat Acceptor → accept4() syscall]
  ↓ Thread Pool에서 Thread 할당 (ThreadPoolExecutor.execute())
  ↓ Controller → Service → DB/API 호출
  ↓  [Blocking I/O 발생 → task_struct Wait Queue 이동]
  ↓ 응답 반환
  ↓ Thread Pool 복귀
```

요청 하나가 끝날 때까지 동일한 `task_struct`가 전체 흐름을 담당합니다.

### Blocking I/O에서의 문제

I/O 대기 구간에서 Thread는 실제 연산을 수행하지 못합니다. 그러나 아래 자원은 계속 유지됩니다.

- Thread Stack 메모리 (Native Memory, Off-Heap)
- `task_struct` (Kernel 메모리)
- Scheduler 관리 대상 (Wait Queue 항목)
- JVM Thread Pool 슬롯

동시 요청 수가 증가할수록 Runnable 및 Sleeping 상태의 `task_struct` 수가 급격히 증가합니다.

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel (Thread 생성) | `new Thread()` → JVM `pthread_create()` → `clone(CLONE_VM\|CLONE_THREAD\|...)` syscall → 새 `task_struct` 생성. Thread Stack은 `mmap(MAP_ANONYMOUS\|MAP_STACK)`으로 Native Memory에 할당 | `strace -e clone -p <pid>`, `/proc/<pid>/task/` (task_struct 목록), `cat /proc/sys/kernel/threads-max` |
| Kernel (Wait Queue) | Blocking I/O 시 `task_struct`가 Socket의 `wait_queue_head_t`에 연결 후 `schedule()` 호출 → CPU 반환. `TASK_INTERRUPTIBLE` 상태로 전환 | `ps aux \| awk '$8~/D\|S/'`, `jstack` (WAITING/BLOCKED Thread 수) |
| JVM (Thread Stack) | 각 Thread의 Stack은 `-Xss`(기본 512KB~1MB)만큼 Native Memory 점유. 1,000 Thread 시 최대 ~1GB Native Memory 소비. Blocking 중에도 해제되지 않음 | `jcmd <pid> VM.native_memory` (Thread 항목), `pmap -x <pid>` |
| JVM (TLAB) | Thread 수 증가 → 각 Thread의 TLAB(Thread-Local Allocation Buffer) 할당 증가 → Eden 영역 빠르게 소진 → Minor GC STW 빈도 증가 | `jstat -gcutil <pid> 1000` (YGC 빈도), `-Xlog:gc+tlab` |
| OS (Limits) | `ulimit -u`(max user processes), `/proc/sys/kernel/threads-max`가 Thread 수 상한. 초과 시 `OutOfMemoryError: unable to create native thread` | `ulimit -a`, `cat /proc/sys/vm/max_map_count` (mmap 수 제한) |

---

## 3. Context Switch

현재 실행 중인 `task_struct`의 CPU Context를 저장하고, 다음 `task_struct`의 CPU Context를 복구하여 실행 흐름을 전환하는 과정입니다.  
Kernel의 `__schedule()` → `context_switch()` 함수 체인으로 구현됩니다.

### Context Switch 시 저장/복구 항목

| 항목 | Kernel 구조체 | 역할 |
|------|-------------|------|
| Program Counter (PC) | `task_struct->thread.ip` | 다음 실행 명령어 위치 |
| Stack Pointer (SP) | `task_struct->thread.sp` | Stack 상태 |
| General Purpose Registers | `pt_regs` 구조체 | 연산 중간 데이터 |
| FLAGS Register | `pt_regs->flags` | CPU 상태 플래그 (ZF, CF 등) |
| Memory Context (CR3) | `mm_struct->pgd` | 가상 메모리 페이지 테이블 베이스 주소 |
| FPU / SIMD 상태 | `fpu->state` | 부동소수점 / 벡터 연산 상태 (Lazy Save) |
| FS/GS Segment | `task_struct->thread.fsbase` | Thread-Local Storage(TLS) 베이스 주소 |

### Context Switch 발생 비용

| 비용 항목 | Kernel 메커니즘 | 설명 |
|----------|----------------|------|
| Register Save / Restore | `switch_to()` 매크로 | 현재 task 레지스터 저장 후 다음 task 레지스터 복구 |
| CR3 갱신 (프로세스 전환 시) | `load_cr3()` | 페이지 테이블 포인터 교체 → TLB Flush 유발 |
| TLB Flush | CPU MMU | CR3 갱신 시 TLB 전체 무효화 (ASID 없는 경우). 이후 메모리 접근마다 Page Table Walk 필요 |
| Scheduler 실행 | `__schedule()` (Kernel Mode) | CFS Red-Black Tree에서 다음 `task_struct` 선택. O(log n) 탐색 비용 |
| Kernel Mode 진입 | Timer IRQ 또는 syscall Trap | 스케줄링 결정 자체가 Kernel Mode에서만 수행 가능 |
| CPU Cache 오염 | L1/L2/L3 Cache | 새로운 task 실행 시 이전 task 데이터 교체 → Cache Cold Start → RAM 접근 증가 |
| CPU Pipeline Flush | CPU Microarchitecture | Context Switch 후 Pipeline 재충전 필요 → Frontend/Backend Stall 증가 |

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Hardware (TLB) | 프로세스 간 Context Switch 시 CR3 갱신 → TLB Flush. Thread 간 전환(같은 프로세스)은 주소 공간 공유 → TLB Flush 없음(PCID/ASID 지원 CPU). Flush 후 Page Table Walk 비용 증가 | `perf stat -e dTLB-load-misses,dTLB-store-misses,iTLB-load-misses` |
| Hardware (Cache) | L1 Cache(32KB) 전환 후 Cold Start. Context Switch 빈도 × Cache 재워밍 비용 = 실효 CPU 낭비량. L3 Cache는 공유이나 다수 Thread 동시 경쟁 시 Cache Line Thrashing 발생 | `perf stat -e cache-misses,cache-references,L1-dcache-load-misses` |
| Hardware (Pipeline Stall) | Branch Predictor가 새 `task_struct` 실행 경로를 학습하기 전까지 Misprediction 증가 → Pipeline Flush → Frontend Stall. IPC < 1.0이면 Pipeline 또는 Memory 병목 | `perf stat -e stalled-cycles-frontend,stalled-cycles-backend,branch-misses` |
| Hardware (NUMA) | CPU Migration 시 다른 NUMA Node로 이동하면 Remote Memory Access(~100~300ns) 발생. Binding 없으면 Scheduler가 부하 분산 목적으로 Migration 수행 | `perf stat -e node-load-misses`, `numastat -p <pid>`, `numactl -C 0-3 java ...` |
| Kernel (CFS) | 자발적 CS(cswch): `schedule()` 직접 호출 — I/O 대기, Lock 대기, `sleep()`. 비자발적 CS(nvcswch): Timer IRQ에 의한 강제 전환 — CPU Saturation 직접 신호. `nvcswch` 급증은 Runnable Queue 과부하를 의미 | `pidstat -w -p <pid> 1` (cswch/s vs nvcswch/s 구분), `vmstat 1` (cs) |
| Kernel (FPU) | FPU 상태는 Lazy Save — Context Switch 시 즉시 저장하지 않고 실제 FPU 사용 시점에만 저장. Java의 double/float 연산 집약적 코드에서 FPU 저장/복구 오버헤드 발생 가능 | `perf stat -e fpu_exceptions` (드물지만 존재) |
| JVM (Safepoint) | GC STW, Deoptimization, Biased Lock Revocation에서 Safepoint 발생. 모든 Thread가 안전 지점 도달 대기(TTSP: Time To Safepoint). Safepoint 완료 후 일제히 재개 → Runnable Queue 순간 폭발 → nvcswch 급증 | `-Xlog:safepoint*`, JFR `jdk.SafepointBegin` `ttspTime` 필드, `jcmd <pid> VM.info` |
| JVM (Off-CPU) | Blocking 발생 시 Worker Thread는 CPU 미사용(Off-CPU Time). CPU Sampling Profiler로는 탐지 불가. Wall-clock Profiler 또는 Off-CPU Profiler로만 관찰 가능 | `async-profiler -e wall -d 60 -f offcpu.html <pid>`, Pyroscope, Datadog Continuous Profiler |

---

## 4. CPU Saturation과 Scheduler Overhead

Runnable `task_struct`가 과도하게 증가하면 Scheduler가 다음 실행 대상 선택 자체에 CPU를 소비하게 됩니다.

```
Runnable task_struct 증가
  ↓ CFS Red-Black Tree 탐색 비용 증가 (O(log n))
  ↓ Context Switch 증가 → TLB Flush + Cache Miss 증가
  ↓ sy CPU Time 증가 → us CPU Time 감소
  ↓ 비즈니스 로직 실행 시간 감소
  ↓ Throughput 감소 / P99 Latency 급증
```

| 현상 | 원인 | 계층 |
|------|------|------|
| CPU 사용률 100% | Context Switch 과다, sy 시간 급증 | Kernel (CFS) |
| Throughput 감소 | 실제 로직보다 스케줄링 비용 > 실행 비용 | Kernel / Hardware |
| 응답 지연 증가 | Runnable Queue 적체 → run_delay 증가 | Kernel (CFS) |
| Cache Miss 증가 | Thread 전환 과다로 L1/L2 Cache 오염 | Hardware |
| IPC 저하 | Pipeline Stall, Cache Miss, TLB Miss 복합 | Hardware |
| Load Average 상승 | TASK_RUNNING + TASK_UNINTERRUPTIBLE 합산 증가 | Kernel |

### Thrashing

극단적인 경우, CPU가 실제 비즈니스 로직보다 Thread 전환(Context Switch)에 대부분의 시간을 소비하는 상태가 됩니다. 이를 **Thrashing** 상태라고 합니다.

```
[Thrashing 진단 신호]
- vmstat: cs 수만 단위 지속, r > CPU 코어 수 × 2
- pidstat: nvcswch/s 급증 (비자발적 CS 주도)
- perf stat: cache-misses 비율 > 10%, IPC < 0.5
- /proc/pressure/cpu: full avg10 > 30%
- top: sy > 30%, us < 30% (Kernel이 App보다 CPU를 더 씀)
```

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel (Scheduler) | `sched_latency_ns`(기본 6~24ms) / `sched_min_granularity_ns`로 Time Slice 결정. Thread 수 증가 시 개별 Time Slice 단축 → 전환 빈도 증가. `sched_wakeup_granularity_ns` 조정으로 Wake-up 후 선점 최소화 가능 | `sysctl kernel.sched_latency_ns`, `/proc/sched_debug`, `perf sched record && perf sched report` |
| Kernel (Load Balancing) | CPU 간 부하 불균형 시 `load_balance()`가 `task_struct`를 다른 CPU로 Migration. Migration 자체가 Cache Miss 유발. `isolcpus` 또는 `taskset`으로 CPU 고정 시 Migration 방지 가능 | `perf stat -e cpu-migrations -p <pid>`, `/proc/schedstat` (cpu_nr_migrations) |
| Kernel (cgroup Throttling) | Kubernetes CPU Limit 설정 시 cgroup `cpu.max` (e.g. `100000 100000` = 1 Core). Quota 소진 → 남은 Period 동안 모든 Thread Throttle. `cpu.cfs_burst_us` 설정으로 순간 피크 완화 가능(cgroup v2) | `/sys/fs/cgroup/cpu.stat` (`nr_throttled`, `throttled_usec`), `kubectl describe node` (CPU 할당 확인) |
| Kernel (Memory Bandwidth) | Thread 수 증가 → 동시 메모리 접근 증가 → Memory Bus 경합 → Memory Bandwidth Saturation. Intel Xeon 단일 소켓 기준 ~100~200GB/s 한계 초과 시 CPU가 메모리 응답 대기 → Stall | `perf stat -e LLC-load-misses,LLC-store-misses`, `pcm-memory` (Intel PCM 도구) |
| Kernel (CPU Frequency) | Thread 폭증 후 CPU가 C-state(Deep Sleep)에서 복귀할 때 P-state(주파수) 상승에 수십~수백 μs 소요. 짧은 요청이 많은 환경에서 Frequency Scaling Latency가 응답 지연에 기여 | `cpupower frequency-info`, `turbostat --Summary 1`, `/sys/devices/system/cpu/cpu*/cpufreq/scaling_governor` (performance 권장) |
| JVM (GC) | Minor GC STW 중 모든 Worker Thread 정지 → 재개 시 Runnable Queue 순간 폭발 → Context Switch 급증. G1/ZGC의 Concurrent Phase는 GC Thread가 Worker Thread와 CPU 경합 | `-Xlog:gc*`, JFR `jdk.GCPhasePause`, GC 중 `vmstat` cs 변화 관찰 |
| JVM (JIT) | JIT C1/C2 Compilation Thread가 Worker Thread와 CPU 경합. 초기 Warm-up 기간에 Interpreter 실행 → CPU 효율 저하. Code Cache 포화 시 JIT 중단 → 영구 Interpreter 실행 | `-XX:+PrintCompilation`, JFR `jdk.Compilation`, `jcmd <pid> VM.code_cache` (CodeCache 사용률) |

---

## 5. User Mode와 Kernel Mode

CPU는 실행 권한 수준을 구분하여 동작합니다.

| 구분 | CPU Ring | 실행 영역 | 특징 |
|------|---------|----------|------|
| User Mode | Ring 3 | 일반 애플리케이션 코드 (Java, JVM, Spring 등) | 제한된 권한. Hardware 직접 접근 불가. System Call로만 Kernel 기능 요청 가능 |
| Kernel Mode | Ring 0 | Linux Kernel | Hardware 직접 제어. Scheduler 실행. Memory 관리. Interrupt Handler 실행 가능 |

```
User Mode (Ring 3)                    Kernel Mode (Ring 0)
─────────────────                     ──────────────────────
Java Application                      Linux Kernel
JVM (Interpreter / JIT)               CFS Scheduler
Spring Framework                      Memory Manager (mm_struct)
libc / glibc (버퍼링 계층)             IRQ/SoftIRQ Handler
                                       System Call Handler
         ↕  syscall / Trap (Ring 3 → Ring 0)
         ↕  Return from Kernel (Ring 0 → Ring 3)
```

### Mode Transition

애플리케이션이 OS 기능을 요청하면 `User Mode → Kernel Mode` 전환이 발생합니다.

| Transition 유형 | 트리거 | 비용 |
|----------------|-------|------|
| System Call | `syscall` 명령어 (x86-64) | ~100ns (간단한 syscall 기준) |
| Hardware IRQ | NIC, Disk 등 외부 장치 인터럽트 | 현재 실행 즉시 중단, Handler 진입 |
| Software Trap (Exception) | 0 나누기, Page Fault, 잘못된 메모리 접근 | 즉시 Kernel 개입 |
| Safepoint (JVM) | GC STW, Deoptimization | 모든 JVM Thread 정지 |

대표 System Call: `read()`, `write()`, `accept4()`, `clone()`, `epoll_wait()`, `futex()`, `mmap()`

### vDSO (Virtual Dynamic Shared Object)

일부 빈번한 Kernel 데이터 읽기는 실제 Mode 전환 없이 처리됩니다.

```
clock_gettime() 호출
  ↓ vDSO: Kernel이 Read-only 메모리 페이지를 User Space에 매핑
  ↓ User Mode에서 직접 시간 데이터 읽기 (syscall 없음)
  ↓ Mode Transition 비용 제로
```

대상: `gettimeofday()`, `clock_gettime()`, `getcpu()`, `time()`

### Context Switch와 Kernel Mode의 관계

Context Switch는 Scheduler가 수행하므로, Thread 교체 시 반드시 Kernel Mode 진입이 발생합니다.

```
User Mode 실행 중 (Java Worker Thread)
  ↓  [Timer IRQ 또는 syscall 발생]
  ↓ CPU Ring 3 → Ring 0 전환 (Trap Gate / IDT)
  ↓ Kernel Mode 진입: IRQ Handler 또는 syscall Handler 실행
  ↓ Scheduler 호출 필요 시: __schedule() 실행
  ↓ Context Switch 수행: 현재 task_struct 저장, 다음 task_struct 복구
  ↓ iret / sysretq 명령어로 Ring 0 → Ring 3 복귀
  ↓ 다음 task_struct User Mode 실행 재개
```

Runnable task가 많을수록 Mode Transition 비용도 함께 증가합니다.

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Hardware (Ring 전환) | x86-64의 `syscall` 명령어가 Ring 3 → Ring 0 전환 수행. `LSTAR` MSR에 등록된 Kernel Entry Point(`entry_SYSCALL_64`) 진입. 전환 시 RSP(Stack Pointer)를 Kernel Stack으로 교체 | `perf stat -e syscalls:sys_enter_* -p <pid>` (syscall 빈도), `strace -c -p <pid>` (syscall 종류/빈도 집계) |
| Hardware (Meltdown / Spectre 완화) | KPTI(Kernel Page Table Isolation) 활성화 시 User→Kernel 전환마다 페이지 테이블 교체 발생 → TLB Flush 추가 비용. Spectre v2 완화(Retpoline)는 Branch Target 예측 비용 증가 | `grep meltdown /proc/cpuinfo`, `perf stat -e iTLB-load-misses` (KPTI 영향 관찰) |
| Kernel (syscall 비용) | 단순 `getpid()`는 ~20ns, `read()`(데이터 있는 경우) ~100ns, `read()`(Blocking) → `task_struct` Sleep → 수ms~수초. Mode Transition 자체보다 Blocking 대기 비용이 압도적으로 큼 | `strace -T -p <pid>` (각 syscall 소요 시간), `perf trace -p <pid>` |
| Kernel (Futex) | Java `synchronized`, `ReentrantLock` 경합 시 `futex(FUTEX_WAIT)` syscall → Kernel Mode 진입 → `task_struct` Sleep. Uncontended Lock은 User Space Atomic 연산으로만 처리 (Kernel Mode 미진입). Lock 경합이 많을수록 Mode Transition 빈도 급증 | `perf trace -e futex -p <pid>`, `strace -e futex -c -p <pid>` (futex 비율 확인) |
| Kernel (libc/glibc) | Java I/O → JVM JNI → glibc 함수 → syscall 체인. glibc의 Buffered I/O(`fwrite`, `printf`)는 내부 버퍼로 syscall 횟수 감소. `malloc()`은 내부적으로 `brk()` 또는 `mmap()` syscall 사용 | `ltrace -p <pid>` (libc 함수 호출), `strace -e brk,mmap -p <pid>` |
| JVM (JNI) | JNI Critical Section(`GetPrimitiveArrayCritical`) 내부에서는 GC Safepoint 진입 불가 → STW 연장. JNI를 통한 Native 라이브러리 호출은 JVM 밖의 Native Thread로 실행 → JVM 모니터링 사각지대 발생 | JFR `jdk.JavaMonitorWait`, `jstack`으로 JNI 관련 BLOCKED Thread 탐지 |
| JVM (Direct Memory) | `ByteBuffer.allocateDirect()` → `mmap()` syscall로 Off-Heap Native Memory 할당. JVM Heap GC 대상이 아님. `-XX:MaxDirectMemorySize` 미설정 시 Native OOM 위험. `sun.misc.Cleaner`가 GC 시점에 `munmap()` 호출 | `jcmd <pid> VM.native_memory` (Direct Memory 항목), `pmap -x <pid>` (anon 영역) |

---

## 6. Blocking vs Non-blocking 구조 비교

| 항목 | Blocking (Thread-per-request) | Non-blocking (Event-loop) |
|------|-------------------------------|--------------------------|
| Thread 수 | 요청 수에 비례하여 증가 | CPU Core 수 중심으로 고정 |
| task_struct 수 | 요청 수 비례 | 소수 고정 |
| Context Switch | 매우 많음 (I/O 완료 시 다수 Wake-up) | 매우 적음 |
| Mode Transition | Thread당 빈번한 syscall | 이벤트당 1회 epoll_wait 반환 |
| Scheduler 부담 | 큼 (Red-Black Tree 관리 비용 증가) | 작음 |
| CPU Cache 효율 | 낮음 (잦은 Context Switch → Cache Cold) | 높음 (Event Loop Thread Cache Hot 유지) |
| TLB 효율 | 낮음 (Thread Migration 시 TLB Flush) | 높음 |
| Native Memory | Thread 수 × Stack Size 만큼 증가 | 소수 Thread → 최소화 |
| CPU 효율 (IPC) | 낮음 | 높음 |
| Blocking I/O 영향 | Thread Pool 고갈 → 503 오류 | Event Loop Blocking 시 전체 처리 마비 |

### Non-blocking 처리 흐름

```
I/O 요청
  ↓ epoll_ctl(EPOLL_CTL_ADD): fd를 epoll에 등록 (syscall 1회)
  ↓ Thread는 다른 이벤트 처리 계속 (CPU 미반환, 계속 실행)
  ↓ NIC IRQ → SoftIRQ → TCP Stack → Socket Buffer 갱신
  ↓ epoll 내부 Ready List 갱신 → epoll_wait() 반환
  ↓ Event Handler 실행 (동일 Thread에서 Callback 처리)
  ↓ I/O 완료 처리 후 다음 이벤트 대기
```

Blocked 상태의 `task_struct` 증가 자체를 최소화하는 구조입니다.

### Linux Scheduler 관점 비교

```
[Blocking 구조: CPU Saturation 연쇄]
Thread 수 = 요청 수  →  task_struct 급증
  ↓ Runnable Queue 적체  →  run_delay 증가
  ↓ nvcswch(비자발적 CS) 증가  →  Cache/TLB Flush 반복
  ↓ sy CPU Time 증가  →  us CPU Time 감소
  ↓ Memory Bandwidth 압박 (동시 Cache Miss → RAM 접근 폭증)
  ↓ IPC 저하  →  CPU Saturation
  ↓ cgroup Throttling (Kubernetes)  →  균일한 Latency 상승
  ↓ Throughput 감소

[Non-blocking 구조: CPU 효율 유지]
소수 task_struct 유지 (Event Loop Thread)
  ↓ Context Switch 최소화  →  Cache/TLB Hot 유지
  ↓ sy CPU Time 낮음  →  us CPU Time 높음
  ↓ IPC 높음  →  CPU 효율 극대화
  ↓ RPS/RFS로 SoftIRQ 여러 CPU에 분산
  ↓ Backpressure + Circuit Breaker로 Retry Storm 방지
  ↓ Throughput 높음 / Latency 안정
```

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel (epoll) | `epoll_create1()` → epoll fd 생성. `epoll_ctl(EPOLL_CTL_ADD)` → 관심 fd를 `ep_item`(rb-tree)에 등록. `epoll_wait()` → Ready List 비면 `task_struct` Sleep, 이벤트 발생 시 Wake-up. Edge Trigger(EPOLLET) 사용 시 반드시 `EAGAIN`까지 읽어야 이벤트 누락 없음 | `strace -e epoll_ctl,epoll_wait -p <pid>`, `/proc/<pid>/fdinfo/<epoll_fd>` |
| Kernel (RPS/RFS) | 기본 설정에서 모든 NIC IRQ가 CPU 0에 집중 → SoftIRQ 병목. RPS(Receive Packet Steering): 패킷을 여러 CPU에 분산 처리. RFS(Receive Flow Steering): Application 실행 CPU로 패킷 유도 → Cache Locality 향상 | `/proc/irq/<N>/smp_affinity`, `ethtool -L eth0 combined <N>`, `/proc/sys/net/core/rps_sock_flow_entries` |
| Kernel (SoftIRQ) | 패킷 폭증 시 `ksoftirqd` CPU 독점 → 다른 task 실행 방해. `net_rx_action`의 `budget`(기본 300 패킷/회) 초과 시 `ksoftirqd`에 처리 위임 | `mpstat -I ALL 1` (`%soft`), `top` (ksoftirqd CPU 점유), `cat /proc/softirqs` |
| JVM (Event Loop) | Netty/Reactor의 Event Loop Thread는 `epoll_wait()` 호출 후 이벤트 처리. Blocking 작업(JDBC, `Thread.sleep()` 등)이 Event Loop Thread에 진입하면 해당 CPU의 모든 I/O 처리 즉시 마비 | Netty Blocked Event Loop 경고 로그 (`Selector.select() took X ms`), `async-profiler` (Event Loop Thread CPU 점유 분석) |
| JVM (Virtual Thread) | JDK 21+ Virtual Thread(Loom): Blocking syscall 시 JVM이 자동으로 Carrier Thread에서 분리 → Platform Thread 미점유. 내부적으로 `epoll` 활용. Thread-per-request 코드 변경 없이 Non-blocking 효과 | `jcmd <pid> Thread.dump_to_file` (Virtual Thread 상태), JFR `jdk.VirtualThreadPinned` (Carrier Thread 고정 탐지) |
| Application (Connection Pool Exhaustion) | Blocking 구조에서 DB 응답 지연 시 HikariCP Pool 고갈 → Worker Thread 전체 Futex Wait → Thread Pool 고갈 → 신규 요청 503. Pool 크기 증가만으로는 근본 해결 불가 | HikariCP JMX `pendingThreads`, `connectionTimeout` 설정, APM DB Connection Wait P99 |
| Application (Circuit Breaker / Backpressure) | 외부 서비스 지연 → Worker Thread 점유 → Retry Storm → 전체 Thread Pool 고갈. Circuit Breaker(Resilience4j)로 빠른 실패 반환. Backpressure로 수용 가능한 요청 수만 허용 | Resilience4j MBean `circuitbreaker.state`, Actuator `/actuator/circuitbreakers` |

---

## 7. SRE 관점 핵심 지표

### vmstat

```bash
vmstat 1
```

| 항목 | 의미 | 임계 신호 |
|------|------|---------|
| `r` | Runnable task 수 (Runnable Queue 길이) | > CPU 코어 수 지속: Saturation |
| `b` | Uninterruptible Sleep task 수 (D 상태) | > 0 지속: Disk I/O 또는 Kernel Lock 병목 |
| `cs` | Context Switch 횟수/초 | 급격한 증가: Thread Pool 과다 또는 Lock 경합 |
| `us` | User Mode CPU 사용률 | 낮음 + sy 높음: Kernel Overhead 주도 |
| `sy` | Kernel Mode CPU 사용률 | > 20% 지속: Scheduler/Syscall Overhead 의심 |
| `wa` | I/O Wait (CPU가 I/O 완료를 기다리는 비율) | > 10%: Disk I/O 병목 |
| `si` / `so` | Swap In / Out | > 0: 메모리 부족, OOM Killer 위험 |

### Load Average 증가 원인

| 원인 | task_struct 상태 | 진단 방법 |
|------|----------------|---------|
| CPU 과부하 | `TASK_RUNNING` (Runnable) 증가 | `vmstat r`, `perf sched latency` |
| Disk I/O 병목 | `TASK_UNINTERRUPTIBLE` (D 상태) 증가 | `ps aux \| awk '$8=="D"'`, `iotop -o` |
| NFS Hang | `TASK_UNINTERRUPTIBLE` 지속 | `dmesg \| grep "INFO: task.*blocked"` |
| Kernel Lock 경합 | `TASK_UNINTERRUPTIBLE` 지속 | `perf trace -e futex` |

### CPU Saturation 징후

아래 항목이 동시에 나타날 경우 Scheduler 병목 가능성을 의심해야 합니다.

| 지표 | 도구 | 신호 |
|------|------|------|
| `cs` 값 비정상 증가 | `vmstat 1` | 수만/초 이상 지속 |
| `r` 값 지속 높음 | `vmstat 1` | > CPU 코어 수 × 2 지속 |
| `nvcswch` 급증 | `pidstat -w -p <pid> 1` | 비자발적 CS가 자발적 CS 초과 |
| Load Average 지속 증가 | `uptime` | > CPU 코어 수 × 1.5 지속 |
| `sy` 상승 | `vmstat 1` | > 20% 지속 |
| Cache Miss 증가 | `perf stat -e cache-misses` | Miss Rate > 5~10% |
| IPC 저하 | `perf stat -e instructions,cycles` | IPC < 1.0 |
| CPU Throttling | `/sys/fs/cgroup/cpu.stat` | `throttled_usec` 지속 증가 |
| PSI CPU some | `/proc/pressure/cpu` | avg10 > 20% |
| Throughput 저하 | APM, Prometheus | RPS 감소 + Latency 상승 동시 발생 |

### 전체 진단 명령어

```bash
# CPU 전반 실시간 모니터링
vmstat 1                                              # r, cs, sy, us 동시 관찰
mpstat -P ALL 1                                       # CPU별 %us, %sy, %soft, %irq
top -H -p <PID>                                       # Thread별 CPU 사용률

# Context Switch / Scheduler
pidstat -w -p <PID> 1                                 # cswch/s(자발) vs nvcswch/s(비자발)
perf sched record -p <PID> -- sleep 10
perf sched latency                                    # Scheduler 대기 시간 분포
cat /proc/schedstat                                   # CPU별 run_delay, nr_switches

# Mode Transition / syscall
strace -c -p <PID>                                    # syscall 종류/빈도/시간 집계
perf trace -p <PID>                                   # syscall + 하드웨어 이벤트 통합
strace -e futex -c -p <PID>                           # Futex 비율 (Lock 경합 진단)

# Hardware 성능 카운터
perf stat -e \
  instructions,cycles,\
  cache-misses,cache-references,\
  L1-dcache-load-misses,\
  dTLB-load-misses,dTLB-store-misses,\
  branch-misses,\
  stalled-cycles-frontend,stalled-cycles-backend \
  -p <PID>

# CPU Pressure
cat /proc/pressure/cpu                                # PSI some/full avg10/60/300

# cgroup Throttling (Kubernetes)
cat /sys/fs/cgroup/cpu.stat                           # nr_throttled, throttled_usec
kubectl top pod                                        # K8s 수준 CPU 사용량

# NUMA
numastat -p <PID>                                     # Remote Memory Access 비율
numactl --hardware                                    # NUMA 토폴로지

# CPU Frequency
cpupower frequency-info                               # 현재 Governor / 주파수
turbostat --Summary 1                                 # Core별 실시간 주파수, C-state

# JVM
jstack <PID>                                          # Thread 상태 (BLOCKED/WAITING/RUNNABLE)
jstat -gcutil <PID> 1000                              # GC STW 빈도 및 시간
async-profiler -e wall -d 60 -f offcpu.html <PID>     # Off-CPU Flame Graph
jcmd <PID> VM.native_memory                           # Thread Stack, Direct Memory 크기
```

---

## 8. 최종 정리

| 항목 | 메커니즘 실체 | SRE 대응 |
|------|-------------|---------|
| Thread-per-request 문제 | Blocking I/O 대기 중 `task_struct` 수 급증. Native Memory(Thread Stack) 비례 증가. GC Pressure 상승(TLAB 증가) | Off-CPU Flame Graph, `jcmd VM.native_memory`, HikariCP `pendingThreads` |
| Context Switch 비용 | Register Save/Restore, TLB Flush(CR3 갱신), Cache Cold Start, Pipeline Flush, Kernel Mode 진입(Ring 3→Ring 0). 비자발적 CS(nvcswch) 급증이 Saturation 직접 신호 | `pidstat -w` (nvcswch/s), `perf stat -e cache-misses,dTLB-load-misses` |
| CPU Saturation 원인 | Runnable task 과다 → CFS Red-Black Tree 비용 증가 → `sy` 시간 증가 → `us` 시간 감소 → Throughput 감소. NUMA Remote Access + Memory Bandwidth Saturation이 가중 | `vmstat r`, `perf sched latency`, `/proc/pressure/cpu` |
| Thrashing | Context Switch가 실제 로직보다 CPU를 더 소비하는 상태. `sy` > `us`, IPC < 0.5, PSI `full` 상승 | `perf stat -e instructions,cycles` (IPC), `vmstat sy` |
| Mode Transition | syscall / Timer IRQ / Page Fault 시 Ring 3→Ring 0. 전환 자체는 ~100ns이나 Blocking syscall은 ms~s 단위 대기. KPTI 활성화 환경에서 TLB Flush 추가 비용 | `strace -c -p <pid>` (syscall 분포), `perf trace` |
| Non-blocking 이점 | 소수 `task_struct`로 Context Switch 최소화. Cache/TLB Hot 유지. CPU Pipeline 효율 향상. IPC 상승 → Throughput 향상 | Event Loop Thread CPU 점유율, `perf stat` IPC 개선 확인 |
| K8s CPU Throttling | cgroup `cpu.max` Quota 소진 → 모든 Thread Throttle Queue → CPU 여유 있어도 Latency 급증. P99/P999 균일 상승 패턴이 Throttling 특징 | `/sys/fs/cgroup/cpu.stat throttled_usec`, `cadvisor`, CPU Request/Limit 비율 조정 |
| JVM 추가 고려 | GC STW → Safepoint → Runnable 폭발. JIT C2 Compilation이 Worker Thread와 CPU 경합. Virtual Thread(JDK 21+)로 Carrier Thread Blocking 최소화 가능 | `-Xlog:safepoint`, JFR `jdk.VirtualThreadPinned`, Code Cache 사용률 모니터링 |

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*