# CPU Stall이란? (E2E 분석 적용됨)

```
CPU가 명령어를 계속 실행하지 못하고,

데이터 준비 지연,
메모리 접근(Cache Miss),
분기 처리(Branch Prediction 실패) 등을

기다리면서

실제 연산이 일시적으로 멈추거나 지연되는 상태
```

즉:

```
연산 진행이 중간에 계속 끊기는 상태
```

에 가깝습니다.

---

## 대표적인 원인

| 원인 | 설명 | 메커니즘 실체 |
|------|------|--------------|
| **Data Dependency** | 이전 명령어의 연산 결과가 아직 준비되지 않아 다음 연산이 대기하는 상태 | RAW (Read-After-Write) Hazard, Reservation Station 점유, ROB(Re-Order Buffer) 대기 |
| **Cache Miss** | 필요한 데이터가 CPU Cache(L1/L2/L3)에 없어 RAM에서 데이터를 가져오느라 지연되는 상태 | L1 Miss → L2 → LLC → DRAM 순차 패치, Memory Latency 누적, Cache Line(64B) 단위 로드 |
| **Branch Misprediction** | CPU의 분기 예측이 실패하여 잘못 실행한 명령어를 폐기하고 다시 실행하는 상태 | Branch Predictor(TAGE/BHB) 오판 → Pipeline Flush → ROB 전체 폐기 → 재실행 비용 (~15 cycles) |
| **Memory Latency** | 메모리 접근 자체가 느려 CPU가 데이터를 기다리는 상태 | DRAM RAS/CAS 타이밍, NUMA Remote 접근 (+50~100ns), Memory Bandwidth Saturation |
| **I/O Wait** | 디스크·네트워크·파일 시스템 응답을 기다리며 CPU 작업 진행이 지연되는 상태 | `task_struct.state = TASK_UNINTERRUPTIBLE`, blk-mq I/O Scheduler 대기, Dirty Page Writeback 경합 |
| **Lock Contention** | 여러 Software Thread가 동일 Lock(Mutex/Spinlock)을 경쟁하면서 대기하는 상태 | Futex `FUTEX_WAIT` 시스템 콜, Spinlock busy-wait(CPU 점유), Off-CPU Time 급증 |
| **CPU Throttling** | Linux CFS Quota 제한으로 Container 실행이 일시적으로 제한되는 상태 | cgroup v2 `cpu.max`, CFS Bandwidth `throttled_cfs_rq`, Period 소진 후 강제 Sleep |
| **Context Switch** | Scheduler가 실행 Thread를 교체하면서 발생하는 CPU 전환 비용 | `switch_to()`, Register 저장/복원, TLB Flush(PCID 없을 시), Cache Warmup 손실 |
| **NUMA Remote Access** | 다른 NUMA Node의 메모리에 접근하면서 메모리 지연이 증가하는 상태 | UPI/Infinity Fabric 홉 경유, Remote IMC → DRAM 패치, Cache Coherency Snoop 트래픽 |

---

## Data Dependency Stall

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| RAW Hazard 발생 | Hardware | 명령어 A의 결과를 명령어 B가 참조할 때, A가 Execute 단계를 완료하기 전까지 B는 Dispatch 불가, Reservation Station(RS)에서 Operand 대기 상태 유지 | `perf stat -e resource_stalls.any`, `toplev.py` (Back-End Bound 항목) |
| Out-of-Order 완화 | Hardware | OoO(Out-of-Order Execution) Engine이 독립적인 명령어를 먼저 실행하여 Stall 시간 은폐, ROB(Re-Order Buffer) 크기(예: Intel Sapphire Rapids 512 entries)가 은폐 깊이 결정 | `perf stat -e uops_dispatched_port`, `toplev.py --level 4` |
| Forwarding | Hardware | Execute 결과를 Write-Back 이전에 다음 명령어 ALU 입력으로 직접 전달(Data Forwarding/Bypassing), Forwarding 불가 시 Pipeline Bubble 발생 | `perf stat -e int_misc.recovery_cycles`, Microarchitecture Manual |
| JIT 코드 최적화 영향 | JVM (C2) + Hardware | JIT C2 컴파일러가 루프 언롤링(Loop Unrolling) / 벡터화(Auto-Vectorization)로 Data Dependency Chain 단축, Safepoint Poll 삽입 위치가 Dependency Chain 중간에 위치하면 Stall 가중 | `-XX:+PrintCompilation`, `-XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining` |

---

## Cache Miss Stall

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| Cache 계층 패치 경로 | Hardware | L1D Miss(~4 cycles) → L2 Miss(~12 cycles) → LLC Miss(~40 cycles) → Local DRAM(~80ns) → Remote DRAM(~160ns), Miss 발생 시 LFB(Line Fill Buffer)가 요청 대기열 관리 | `perf stat -e L1-dcache-load-misses,L2-cache-misses,LLC-load-misses` |
| Cache Line Thrashing | Hardware + Kernel | 여러 Core/Thread가 동일 64B Cache Line을 반복 Write → MESI 프로토콜 Invalidate 메시지 폭주 → Cache Ping-Pong, False Sharing: 다른 변수가 같은 Cache Line에 위치하여 불필요한 무효화 발생 | `perf c2c record + report` (Cache-to-Cache 전송 분석), `perf stat -e offcore_response` |
| THP(Transparent HugePage)와 Cache | Hardware + Kernel | 2MB HugePage 사용 시 TLB Miss 감소 효과, 단 대용량 Sparse 접근 시 2MB 단위 전체 Cache Eviction → LLC 오염, `khugepaged` 데몬이 4KB → 2MB 승격 수행 | `/sys/kernel/mm/transparent_hugepage/enabled`, `perf stat -e dTLB-load-misses`, `/proc/meminfo` (AnonHugePages) |
| Prefetcher 동작 | Hardware | HW Prefetcher가 Sequential / Stride 접근 패턴 감지 후 선제 로드, 불규칙(Random) 접근 패턴에서 Prefetcher 효과 없음 → Miss 급증, SW Prefetch 명령어(`PREFETCHT0`)로 명시적 보완 가능 | `perf stat -e hw_prefetch:*`, `toplev.py` (Memory Bound > L3 Bound) |
| JVM Heap 접근 패턴 | JVM + Hardware | GC 수행 중 Heap 전체 순회 → 랜덤 접근으로 LLC Miss 급증, G1 GC의 Card Table / Remembered Set 순회 시 Cache Miss 집중, TLAB 할당: 연속 주소 할당으로 Cache Friendly 유지 | `perf stat -e LLC-load-misses -p <java_pid>`, `-Xlog:gc*`, `jstat -gcnew` |
| Memory Bandwidth Saturation | Hardware + Kernel | DRAM 대역폭 한계 도달 시 모든 Cache Miss가 큐 대기 → Stall 시간 선형 증가, 다수 Core가 동시에 LLC Miss 유발 시 IMC(Integrated Memory Controller) 포화 | `perf stat -e uncore_imc/data_reads/,uncore_imc/data_writes/`, `numactl --hardware` (memory bandwidth 항목) |

---

## Branch Misprediction Stall

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| Branch Predictor 동작 | Hardware | TAGE(Tagged Geometric History Length) Predictor: 분기 히스토리 패턴 기반 예측, BTB(Branch Target Buffer): 간접 분기 목적지 캐시, RSB(Return Stack Buffer): 함수 반환 주소 예측 | `perf stat -e branch-instructions,branch-misses`, `perf report --sort=dso,symbol` |
| Pipeline Flush 비용 | Hardware | 예측 실패 감지(Execute 단계) → ROB 전체 Flush → 잘못 실행된 명령어 폐기 → Correct Path 재시작, 비용: ~15~20 Pipeline Cycle (현대 슈퍼스칼라 기준) | `perf stat -e int_misc.recovery_cycles`, `toplev.py` (Bad Speculation 항목) |
| Spectre/Meltdown 완화 영향 | Hardware + Kernel | Retpoline(간접 분기 완화), IBRS/IBPB(BTB 격리): Branch Predictor 예측 정확도 저하 및 추가 Stall 발생, Kernel/User 전환 시 STIBP 적용으로 SMT 형제 Thread 간 예측 공유 차단 | `dmesg | grep -E "Spectre|Meltdown|retpoline"`, `perf stat -e speculative_id_*` |
| JIT 컴파일 최적화 | JVM (C2) + Hardware | C2 JIT: 빈번 분기 경로 인라이닝(Inlining), `instanceof` 체크 최적화(Type Profile 기반), 메서드 가상 호출(virtual dispatch) → 인터페이스 호출 시 예측 실패율 높음 | `-XX:+PrintCompilation`, `-XX:+PrintInlining`, `async-profiler -e cpu` |
| JNI Critical Section | JVM + Hardware | JNI 호출 시 JVM Safepoint 진입 불가 구간(JNI Critical Section) 발생, GC가 Safepoint 대기 → 모든 JVM Thread Stall, Native 코드에서 분기 예측 패턴이 JIT 최적화 범위 밖 | `-XX:+PrintSafepointStatistics`, `jcmd <pid> Thread.print` |

---

## Memory Latency Stall

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| DRAM 접근 지연 구조 | Hardware | Row 활성화(RAS: ~15ns) → Column 선택(CAS: ~15ns) → 데이터 전송 → Precharge, 총 Local DRAM 지연 ~80ns, Bank Conflict 시 추가 지연, Row Buffer Miss 시 Precharge + RAS 재수행 | `perf stat -e uncore_imc/*`, `mlc` (Memory Latency Checker 도구) |
| NUMA Remote Access | Hardware + Kernel | Socket 0 Core → UPI Link → Socket 1 IMC → DRAM: 추가 ~50~80ns 지연, Snoop 메시지(MESIF/MOESI) 처리로 추가 RTT 발생, `task_numa_fault()`: Kernel이 접근 패턴 감지 후 Page 마이그레이션 수행 | `numastat -p <pid>`, `/proc/PID/numa_maps`, `perf stat -e node-load-misses,node-store-misses` |
| Memory Bandwidth Saturation | Hardware | 다수 Core 동시 LLC Miss → IMC 요청 큐 포화 → 대기 시간 비선형 증가, Memory-bound 워크로드에서 Core 추가가 성능 향상 없이 Stall만 증가 | `perf stat -e memory_bandwidth:*`, `emon` (Intel 전용), `pcm-memory` |
| CPU Frequency Scaling 영향 | Hardware + Kernel | C-state 전환(C0 → C6): Wakeup Latency ~수백 μs, I/O 이벤트 대기 중 Deep Sleep 진입 후 복귀 시 Memory Controller 재초기화 지연, `intel_pstate` 드라이버가 P-state 동적 조정 | `turbostat --interval 1`, `cpupower idle-info`, `/sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq` |
| JVM Direct Memory | JVM + Kernel + Hardware | `ByteBuffer.allocateDirect()` → `mmap()`/`malloc()` → OS First-Touch 정책으로 최초 접근 시 Page Fault 발생, Netty `PooledDirectByteBuf` 재사용 시 Local NUMA Node 고정 효과, Remote Node 할당 시 매 접근마다 추가 지연 | `/proc/PID/numa_maps` (anon 항목), `numastat -p <pid>`, `perf mem record` |

---

## I/O Wait Stall

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| 프로세스 상태 전환 | Kernel | `read()` / `write()` 시스템 콜 → 데이터 미준비 시 `task_struct.state = TASK_UNINTERRUPTIBLE` → Wait Queue 삽입, I/O 완료(IRQ) → `wake_up()` → RunQueue 복귀 | `ps aux` (D 상태 프로세스), `/proc/PID/wchan` (대기 커널 함수), `offcputime-bpfcc -p <pid>` |
| blk-mq I/O Scheduler | Kernel | Multi-Queue Block Layer(blk-mq): per-CPU Software Queue → Hardware Dispatch Queue, 스케줄러 정책(none/mq-deadline/kyber)에 따라 요청 병합 및 우선순위 결정, I/O Depth 초과 시 Queue 대기 | `iostat -x 1` (await / svctm), `cat /sys/block/<dev>/queue/scheduler`, `blktrace -d /dev/sda` |
| Dirty Page Writeback | Kernel | 메모리 Write → Page Cache에 Dirty Page 적재, `pdflush` / `kworker` 가 주기적으로 Disk Write(Writeback), Dirty Page 비율 (`dirty_ratio`) 초과 시 Application Write 자체가 Blocking → Stall | `/proc/vmstat` (nr_dirty, nr_writeback), `echo 3 > /proc/sys/vm/drop_caches`, `iotop -o` |
| Page Cache / Page Fault | Kernel | Minor Fault: 물리 주소 매핑만 추가, ~수 μs, Major Fault: Disk에서 실제 로드 필요, ~수 ms, mmap 기반 파일 접근 시 처음 접근 페이지마다 Major Fault 발생 → Stall 누적 | `vmstat 1` (pgfault/pgmajfault), `/proc/PID/stat` (majflt 항목), `perf stat -e major-faults` |
| io_uring 비동기 I/O | Kernel + App | Submission Queue(SQ) → Kernel 비동기 처리 → Completion Queue(CQ): 시스템 콜 없이 공유 Ring Buffer로 결과 수신, `IORING_OP_*` 연산으로 파일/네트워크 I/O 통합, Stall 없이 연속 요청 가능 | `/proc/PID/fdinfo` (uring 항목), `io_uring_setup` 시스템 콜 추적, `strace -e io_uring_*` |
| Serialization / Deserialization 비용 | App + Kernel | JSON/Protobuf 직렬화: CPU 집중 연산 + 메모리 할당 반복 → GC 압박, 대용량 페이로드 직렬화 중 I/O Write 블로킹 시 Thread가 Write Buffer 대기 → Off-CPU 전환 | `async-profiler -e cpu`, `perf record -g -p <pid>`, `jfr` (Java Flight Recorder) |

---

## Lock Contention Stall

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| Mutex / Futex 대기 | Kernel + App | `pthread_mutex_lock()` → Futex `FUTEX_WAIT` 시스템 콜 → `task_struct.state = TASK_INTERRUPTIBLE` → Wait Queue 삽입, Lock 해제 시 `FUTEX_WAKE` → 경쟁 Thread Wakeup, CPU는 타 Thread 실행 (Off-CPU Stall) | `offcputime-bpfcc -p <pid>`, `perf lock record + report`, `/proc/PID/syscall` |
| Spinlock Busy-Wait | Kernel / Hardware | Kernel 내부 Spinlock: Lock 획득 시까지 `PAUSE` 명령어 반복 (CPU 점유 유지), SMP 환경에서 Spinlock holder가 느리면 다수 Core가 동시에 Busy-Wait → CPU 사이클 낭비 (On-CPU Stall) | `perf stat -e cpu-cycles`, `mpstat -P ALL 1` (CPU 100% 확인), `lockstat` |
| JVM Monitor / Synchronized | JVM + Kernel | `synchronized` 블록 진입 시 Object Monitor 획득 시도, Biased Locking → Thin Lock → Fat Lock(OS Mutex) 순으로 에스컬레이션, Fat Lock 전환 시 `FUTEX_WAIT` 시스템 콜 발생 → Off-CPU | `jstack <pid> | grep BLOCKED`, `jcmd <pid> Thread.print`, `-XX:+PrintBiasedLockingStatistics` |
| JVM Safepoint와 Lock | JVM | GC / Deoptimization 시 Safepoint 요청: 모든 JVM Thread가 안전 지점 도달까지 대기, Lock 보유 Thread가 Safepoint 도달 지연 시 전체 JVM Thread Stall (Time-to-Safepoint 증가) | `-XX:+PrintSafepointStatistics`, `-XX:+SafepointTimeout`, `jcmd <pid> VM.info` |
| Connection Pool Exhaustion | App + Kernel | DB/HTTP Connection Pool 고갈 시 대기 Thread가 Pool Monitor `FUTEX_WAIT`, 대기 시간 초과 → Timeout Exception, NUMA Remote Node의 Thread가 Wakeup 후 추가 Latency 발생 | `ss -tp`, `jstack <pid> | grep WAITING`, `offcputime-bpfcc`, Prometheus `hikaricp_pending_threads` |
| False Sharing으로 인한 Cache Line Lock | Hardware | 다른 변수가 동일 64B Cache Line에 배치 → 한 Thread의 Write가 타 Core Cache Line 무효화 → 타 Core Reload 대기, Atomic 연산(CAS) 실패 반복 시 Contention Stall 가중 | `perf c2c record + report`, `perf stat -e lock:*`, 패딩 (`@Contended` 어노테이션) |

---

## CPU Throttling Stall

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| CFS Bandwidth Control | Kernel | cgroup v2 `cpu.max = quota period`, Period(기본 100ms) 내 Quota(CPU 사용 허용 시간) 소진 시 `throttle_cfs_rq()` 호출 → `cfs_bandwidth.throttled_cfs_rq`에 배치 → 다음 Period까지 실행 불가 | `/sys/fs/cgroup/cpu.stat` (throttled_usec, nr_throttled), `cat /sys/fs/cgroup/cpu.max` |
| Throttling 해제 시점 | Kernel | Period 리셋 타이머 (`hrtimer`) 만료 시 `unthrottle_cfs_rq()` 호출, Quota 충전 후 대기 RunQueue 복귀, 충전 전까지 Thread는 TASK_RUNNING 상태지만 RunQueue에서 제거된 상태 | `perf sched latency -p <pid>`, `systemd-cgtop`, `kubectl top pod` |
| CPU Burst | Kernel | `cpu.max.burst`: 단기 Quota 초과 허용(크레딧 방식), 일시적 트래픽 피크 시 Throttling 없이 처리 가능, 장기 평균은 `cpu.max` 준수 | `cat /sys/fs/cgroup/cpu.max.burst`, Kubernetes `alpha.kubernetes.io/cpu-burst` |
| Kubernetes 환경 | Kubelet + Kernel | Pod `resources.limits.cpu` → cgroup `cpu.max` 변환, Guaranteed QoS + CPU Manager Static Policy → cpuset 전용 할당으로 Throttling 회피 가능, BestEffort Pod는 Shared Pool에서 경쟁 | `kubectl describe pod` (CPU Limits), Prometheus `container_cpu_cfs_throttled_seconds_total`, `cadvisor` |
| Throttling과 Tail Latency | App + Kernel | Throttled Thread의 재개 시점이 비결정적 → p99/p999 Latency 급증, GC Thread Throttling 시 GC Pause 연장 → Stop-The-World 시간 증가 | Prometheus `jvm_gc_pause_seconds`, `-Xlog:gc*:time`, `async-profiler` |

---

## Context Switch Stall

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| Context Switch 수행 과정 | Kernel | `schedule()` → `pick_next_task_fair()` → `context_switch()` → `switch_mm()` (Address Space 교체) → `switch_to()` (Register 저장/복원, 범용/FPU/SSE 레지스터 포함), 총 비용 ~수 μs | `vmstat 1` (cs 항목), `pidstat -w 1`, `perf stat -e context-switches` |
| TLB Flush | Hardware + Kernel | Address Space 교체 시 TLB 전체 Flush (PCID 미지원 시), 이후 모든 메모리 접근이 TLB Miss → Page Table Walk(~수십 cycles) 반복, PCID(Process-Context Identifiers) 사용 시 Flush 생략 가능 | `perf stat -e dTLB-load-misses,dTLB-store-misses`, `cat /proc/cpuinfo | grep pcid` |
| Cache Warmup 손실 | Hardware | 새 Thread 실행 시 L1/L2 Cache Cold Start: 초기 다수 Cache Miss 발생, Cross-Socket Context Switch 시 LLC도 Cold → Remote Memory 접근 급증 | `perf stat -e cache-misses,LLC-load-misses`, `perf sched timehist` |
| Involuntary vs Voluntary | Kernel | Voluntary: Thread 스스로 `sched_yield()` / `sleep()` / `futex_wait()` 등으로 CPU 양보, Involuntary: Scheduler Time Slice(기본 ~4ms) 만료로 강제 교체, Involuntary 비율 높으면 CPU 경합 심각 | `/proc/PID/status` (voluntary_ctxt_switches, nonvoluntary_ctxt_switches), `pidstat -w` |
| JVM Thread와 Context Switch | JVM + Kernel | JVM Platform Thread(OS Thread 1:1 매핑) → Context Switch 비용 직접 부담, Virtual Thread(JDK 21+): Carrier Thread Pool에서 실행, Blocking 시 Unmount → 다른 Virtual Thread 실행 → OS Context Switch 최소화 | `jcmd <pid> Thread.print`, `perf stat -e context-switches -p <java_pid>`, `jfr` |
| Off-CPU Time | Kernel + App | Thread가 CPU를 사용하지 않는 시간 전체: I/O 대기 + Lock 대기 + Sleep + Scheduler 대기, Off-CPU 시간이 전체 응답 시간의 대부분을 차지하는 경우 CPU Profiler로는 탐지 불가 | `offcputime-bpfcc -p <pid> 30`, `bpftrace -e 'tracepoint:sched:sched_switch'`, `perf record -e sched:sched_switch` |

---

## NUMA Remote Access Stall

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| Remote Access 경로 | Hardware | Core → L1/L2 Miss → LLC Miss → UPI/Infinity Fabric 패킷 전송 → Remote Socket IMC → DRAM 패치 → 역방향 반환, 총 지연 ~130~160ns (Local ~80ns 대비 +50~100%) | `numactl --hardware` (distances 행렬), `perf stat -e node-load-misses`, `mlc --latency_matrix` |
| Cache Coherency Snoop | Hardware | MESIF(Intel) / MOESI(AMD) 프로토콜: Remote Core의 Modified Cache Line 존재 시 Snoop 요청 → 해당 Core가 Flush 후 전달, Directory-based Coherency 사용 시 Directory 조회 추가 | `perf stat -e offcore_response.demand_data_rd.l3_miss_remote_dram`, `pcm` |
| NUMA Balancing | Kernel | `numa_balancing` 활성화: 주기적으로 Hot Page Unmap → 재접근 시 `task_numa_fault()` 발생 → 접근 Node 감지 → `numa_migrate_pages()`로 Page 이동, 마이그레이션 중 일시적 Stall 발생 | `/proc/sys/kernel/numa_balancing`, `numastat` (numa_miss 항목), `perf stat -e numa:*` |
| NUMA + JVM Heap | JVM + Kernel + Hardware | `-XX:+UseNUMA`: Young Gen Eden을 NUMA Node별로 분산 할당, TLAB도 접근 Node 로컬에 배치, GC Thread가 원격 Node Heap 순회 시 Remote Access 급증 → GC Pause 연장 | `-XX:+UseNUMA`, `numastat -p <java_pid>`, `-Xlog:gc+heap=debug` |
| IRQ와 NUMA | Hardware + Kernel | NIC IRQ가 특정 Socket에 집중되면 SoftIRQ 처리 CPU가 편중 → Application Thread가 반대 Socket에 있을 경우 sk_buff 데이터가 Remote Memory에 위치 → 패킷 처리 시 NUMA Remote Access | `/proc/interrupts`, `cat /sys/class/net/<nic>/device/numa_node`, RPS/RFS 설정으로 완화 |
| numactl 제어 | Kernel + App | `numactl --cpunodebind=0 --membind=0`: CPU와 메모리를 동일 Node에 고정하여 Remote Access 원천 차단, `--interleave=all`: 대역폭 최대화 목적으로 Page 분산 배치 | `numactl --hardware`, `numastat -p <pid>`, `/proc/PID/numa_maps` |

---

## 구성 요소 요약

| 구성 요소 | 역할 | 핵심 메커니즘 | 관찰 인터페이스 |
|-----------|------|--------------|----------------|
| ROB (Re-Order Buffer) | Out-of-Order 명령어 완료 순서 보장 | RAW Hazard 대기, Pipeline Flush 시 전체 폐기 | `toplev.py`, `perf stat -e resource_stalls` |
| Branch Predictor | 분기 방향 사전 예측으로 Pipeline 유지 | TAGE/BHB 예측 실패 시 Pipeline Flush (~15 cycles) | `perf stat -e branch-misses` |
| Cache Hierarchy | L1/L2/LLC 계층적 데이터 캐시 | Miss 시 하위 계층 순차 패치, Cache Line Thrashing | `perf stat -e LLC-load-misses`, `perf c2c` |
| TLB | 가상-물리 주소 변환 캐시 | Context Switch 시 Flush, HugePage로 Miss 완화 | `perf stat -e dTLB-load-misses` |
| Futex | User Space Lock의 커널 중재 지점 | `FUTEX_WAIT` → Off-CPU, `FUTEX_WAKE` → Runqueue 복귀 | `offcputime-bpfcc`, `perf lock` |
| cgroup CFS Bandwidth | Container CPU 사용 제한 | Quota 소진 시 `throttled_cfs_rq` 배치 | `/sys/fs/cgroup/cpu.stat`, Prometheus |
| NUMA Interconnect | Socket 간 메모리 접근 경로 | UPI/Infinity Fabric 홉, Snoop 트래픽 | `numastat`, `perf stat -e node-load-misses` |
| blk-mq | 블록 I/O 다중 큐 스케줄링 | per-CPU SoftQ → HW Queue 디스패치 | `iostat -x`, `blktrace` |

---

## 성능 최적화 핵심 요소

```
Data Dependency 최소화 (루프 언롤링, 벡터화, JIT 인라이닝)
+ Cache Miss 감소 (데이터 구조 Cache-Friendly 설계, THP, HugePage)
+ Branch Misprediction 억제 (분기 단순화, Profile-Guided Optimization)
+ Memory Latency 단축 (NUMA Local 접근 강제, numactl, -XX:+UseNUMA)
+ I/O Stall 제거 (io_uring 비동기 I/O, Dirty Page Writeback 튜닝)
+ Lock Contention 완화 (Lock-Free 자료구조, Virtual Thread, Connection Pool 크기 조정)
+ Throttling 제거 (CPU Limits 조정, Guaranteed QoS, CPU Manager Static Policy)
+ Context Switch 최소화 (Thread Pool 크기 최적화, Virtual Thread, CPU Pinning)
+ NUMA Remote Access 차단 (numactl --membind, RPS/RFS, IRQ Affinity)
= Stable Throughput + Low Latency + Low Tail Latency
```

### 원인별 우선 탐지 도구

| Stall 원인 | 1차 탐지 | 2차 심층 분석 |
|-----------|---------|-------------|
| Data Dependency | `toplev.py` (Back-End Bound) | `perf stat -e resource_stalls.any` |
| Cache Miss | `perf stat -e LLC-load-misses` | `perf c2c`, `perf mem record` |
| Branch Misprediction | `perf stat -e branch-misses` | `toplev.py` (Bad Speculation), `perf report` |
| Memory Latency | `mlc --latency_matrix` | `perf stat -e node-load-misses`, `numastat` |
| I/O Wait | `iostat -x 1`, `iotop -o` | `blktrace`, `offcputime-bpfcc` |
| Lock Contention | `perf lock record + report` | `offcputime-bpfcc`, `jstack` |
| CPU Throttling | `/sys/fs/cgroup/cpu.stat` | `container_cpu_cfs_throttled_seconds_total` |
| Context Switch | `vmstat 1` (cs 항목) | `pidstat -w`, `offcputime-bpfcc` |
| NUMA Remote Access | `numastat` (numa_miss) | `perf stat -e node-load-misses`, `mlc` |

---

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*