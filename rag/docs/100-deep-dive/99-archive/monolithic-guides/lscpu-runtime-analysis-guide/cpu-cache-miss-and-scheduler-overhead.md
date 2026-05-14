# CPU Cache Miss와 Scheduler Overhead (E2E 분석 적용됨)

## 1. 개요

CPU Cache Miss는 CPU가 필요한 데이터를 Cache에서 찾지 못하고 RAM까지 접근해야 하는 상황입니다.

Cache Miss가 증가하면 CPU는 메모리 응답을 기다리는 시간이 늘어나고, 이는 Scheduler Overhead 증가와 CPU Saturation으로 이어질 수 있습니다.

이 문서는 Hardware → OS Kernel → Runtime → Application 전 계층에 걸쳐 실제 동작하는 메커니즘 실체를 기준으로 Cache Miss와 Scheduler Overhead의 연쇄 구조를 분석합니다.

---

## 2. CPU Cache 계층 구조

CPU는 RAM보다 훨씬 빠르게 동작합니다. 자주 사용하는 데이터를 빠르게 접근하기 위해 CPU 내부에 Cache 계층이 존재합니다.

| 계층 | 구분 | 특징 | 접근 지연 (근사값) |
|------|------|------|-------------------|
| L1 Cache | Hardware | 속도가 가장 빠르고 용량이 가장 작음 (수십 KB) | ~4 cycle |
| L2 Cache | Hardware | L1보다 크고 약간 느림 (수백 KB) | ~12 cycle |
| L3 Cache | Hardware | 여러 Core가 공유하는 경우가 많음 (수 MB ~ 수십 MB) | ~40 cycle |
| RAM | Hardware | 용량은 크지만 CPU 접근 기준으로 매우 느림 | ~200 cycle |

### Hardware 계층 메커니즘 실체

**Cache Line**: Cache는 Byte 단위가 아닌 Cache Line(보통 64 Byte) 단위로 데이터를 로드합니다. 단일 변수 접근도 Cache Line 전체를 RAM에서 읽어오게 됩니다.

**Cache Associativity**: Direct-mapped, Set-associative, Fully-associative 구조에 따라 동일 Cache Set에 대한 경합 양상이 달라집니다. 다수 Thread가 같은 Cache Set에 접근하면 Cache Thrashing이 발생합니다.

**Cache Line Thrashing**: 여러 CPU Core가 동일한 Cache Line을 공유하면서 각자 쓰기(write)를 수행하면, MESI 프로토콜에 의해 Cache Line이 Invalidate → Fetch를 반복합니다. False Sharing이 대표적인 원인입니다.

**NUMA (Non-Uniform Memory Access)**: 다중 소켓 서버에서 CPU Socket은 로컬 메모리에 빠르게 접근하지만 원격 소켓의 메모리는 NUMA Interconnect를 경유합니다. 원격 메모리 접근 시 RAM 접근보다 추가 지연이 발생하여 Cache Miss 효과가 배가됩니다.

**CPU Pipeline Stall**: Cache Miss 발생 시 CPU Out-of-Order Execution이 독립적인 명령어를 선실행하려 시도하지만, 의존 관계가 있는 명령어는 메모리 응답 대기로 Pipeline Stall이 발생합니다.

**Branch Misprediction**: Cache Miss와 함께 자주 동반되는 현상으로, CPU가 분기 예측에 실패하면 Pipeline을 비우고 재실행합니다. Cache Miss로 인한 데이터 부재 상태에서 분기 예측까지 실패하면 Stall이 중첩됩니다.

**CPU Frequency Scaling (C-state / P-state)**: CPU가 idle 상태가 되면 전력 절약을 위해 C-state(수면 상태)로 진입합니다. Cache Miss로 인한 CPU Stall이 길어지면 C-state 전환 빈도가 증가하고, 복귀 시 추가 지연(wake-up latency)이 발생합니다. P-state는 CPU 주파수 조절로, Turbo Boost 해제 시 클럭당 처리량이 감소합니다.

**TLB (Translation Lookaside Buffer)**: 가상 주소를 물리 주소로 변환하는 캐시입니다. Context Switch 시 TLB Flush가 발생하고, 새 프로세스의 주소 변환이 모두 Cache Miss로 시작됩니다. HugePage(THP: Transparent HugePage)를 사용하면 TLB Entry 하나가 더 큰 메모리 범위를 커버하여 TLB Miss를 줄일 수 있습니다.

**Memory Bandwidth Saturation**: Cache Miss가 증가하면 CPU와 RAM 사이의 메모리 버스 사용량이 증가합니다. 여러 Core에서 동시에 Cache Miss가 발생하면 메모리 버스가 포화 상태에 이르러 전체 시스템 처리량이 급감합니다.

### SRE 분석 도구 / 관찰 키워드

```bash
# Cache Miss / Hit 분석
perf stat -e cache-references,cache-misses,LLC-loads,LLC-load-misses -p <PID>

# Cache Miss 비율 계산
# cache-misses / cache-references * 100

# False Sharing (Cache Line Thrashing) 분석
perf c2c record -p <PID>
perf c2c report

# TLB Miss 분석
perf stat -e dTLB-load-misses,dTLB-store-misses,iTLB-load-misses -p <PID>

# CPU Pipeline Stall / Branch Misprediction
perf stat -e cycles,instructions,branch-misses,stalled-cycles-frontend,stalled-cycles-backend -p <PID>

# NUMA 메모리 접근 분포 확인
numactl --hardware
numastat -p <PID>

# CPU Frequency Scaling 상태
cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq
turbostat --interval 1

# Memory Bandwidth Saturation
perf stat -e memory-bandwidth -a
# 또는 Intel MLC (Memory Latency Checker) 사용
```

---

## 3. Cache Hit와 Cache Miss

| 구분 | 의미 | 결과 |
|------|------|------|
| Cache Hit | 필요한 데이터가 Cache에 존재 | 즉시 연산 수행 가능 |
| Cache Miss | 필요한 데이터가 Cache에 없음 | RAM 접근 필요, 지연(CPU Stall) 발생 |

### Cache Miss 발생 흐름 — 계층별 실체

```
[Application Layer]
  CPU 명령어 실행 (load/store instruction)
    ↓
[Hardware Layer - L1 Cache]
  L1 Cache 조회 → Miss
    ↓
[Hardware Layer - L2 Cache]
  L2 Cache 조회 → Miss
    ↓
[Hardware Layer - L3 Cache]
  L3 Cache 조회 → Miss
    ↓
[Hardware Layer - Memory Controller]
  RAM 접근 요청 (메모리 버스 트랜잭션 발생)
    ↓
[Hardware Layer - CPU Pipeline]
  데이터 도착까지 CPU Pipeline Stall
  (Out-of-Order Execution으로 독립 명령어 선실행 시도)
    ↓
[Hardware Layer - Cache]
  Cache Line 단위(64 Byte)로 데이터 로드
    ↓
[Hardware Layer / OS Kernel]
  데이터 도착 후 연산 재개
  NUMA 원격 노드라면 추가 Interconnect 지연 포함
```

**CPU 사용률이 높게 표시되더라도** 실제로는 메모리 응답 대기 시간(Off-CPU는 아니지만 Stall 상태)이 실행 시간을 차지하는 상태입니다. `perf stat`의 `instructions per cycle (IPC)` 값이 낮으면 Cache Miss 또는 Pipeline Stall이 원인임을 의심해야 합니다.

### SRE 분석 도구 / 관찰 키워드

```bash
# IPC 확인 (낮을수록 Stall 많음)
perf stat -e cycles,instructions -p <PID>
# instructions / cycles = IPC (2 이상이면 양호, 0.5 이하면 Memory-bound 의심)

# Flame Graph로 Off-CPU / On-CPU 구분
perf record -g --call-graph dwarf -p <PID>
perf script | stackcollapse-perf.pl | flamegraph.pl > flame.svg

# eBPF 기반 Off-CPU 분석
offcputime-bpfcc -p <PID> 10
```

---

## 4. Context Switch와 Cache Miss의 관계

Context Switch가 자주 발생하면 CPU Cache 효율이 저하됩니다.

### OS Kernel 계층 메커니즘 실체

**task_struct**: Linux Kernel은 각 프로세스/스레드를 `task_struct` 구조체로 관리합니다. Context Switch 시 현재 `task_struct`의 레지스터 상태를 저장하고 다음 `task_struct`의 상태를 복원합니다(`switch_to()` 매크로 호출).

**Cache 무효화 패턴**: 각 `task_struct`는 실행 중 자신의 데이터(Stack, 지역 변수, 힙 데이터)를 Cache에 적재합니다. Context Switch로 다른 `task_struct`가 실행되면 기존 Cache 내용은 새 `task_struct`에 적합하지 않으므로 Cache Miss가 발생합니다.

**TLB Flush**: 프로세스 간 Context Switch(Thread 간 전환과 달리)에서는 TLB Flush가 발생합니다. ASID(Address Space ID)를 지원하는 아키텍처에서는 TLB Flush 없이 태그로 구분하지만, 엔트리 고갈 시 결국 Flush됩니다.

**시스템 콜 오버헤드**: Context Switch 자체가 `schedule()` → `context_switch()` → `switch_mm()` → `switch_to()` 경로를 타는 Kernel 코드 실행입니다. User Mode → Kernel Mode 전환(Trap) 비용이 수반되며, Spectre/Meltdown 완화 패치(IBRS, IBPB, STIBP) 적용 환경에서는 이 비용이 더 큽니다.

**Futex (Fast Userspace Mutex)**: 경쟁 없는 경우 User Space에서 atomics으로 처리되지만, 경쟁 발생 시 `futex()` 시스템 콜 → Kernel의 Futex Hash Table 조회 → Wait Queue 등록 → Sleeping 상태 전환 → Context Switch가 발생합니다. Lock Contention이 심한 애플리케이션에서 Cache Miss와 Futex 대기가 결합되어 성능이 급격히 저하됩니다.

```
task_struct A 실행  →  A의 데이터가 L1/L2/L3 Cache에 적재
  ↓ Context Switch (schedule() → switch_to())
  ↓ TLB Flush (프로세스 간 전환 시)
task_struct B 실행  →  B의 데이터가 Cache에 없음  →  Cache Miss 증가
  ↓ A 재실행 시 A의 Cache Line 상당 부분 Evict 상태
  →  Cache Miss 재발생
```

### SRE 분석 도구 / 관찰 키워드

```bash
# Context Switch 전체 확인
vmstat 1  # cs 항목

# Thread별 Context Switch 상세
pidstat -w -p <PID> 1
# cswch/s: 자발적 (I/O 대기 등), nvcswch/s: 비자발적 (Time Slice 소진)

# Kernel 함수 호출 추적 (Context Switch 경로)
perf trace -e sched:sched_switch -p <PID>

# Futex 경합 분석
perf trace -e syscalls:sys_enter_futex -p <PID>
/proc/PID/status 의 voluntary_ctxt_switches / nonvoluntary_ctxt_switches
```

---

## 5. Runnable task_struct 증가와 Cache Miss 연쇄 구조

Runnable 상태의 `task_struct`가 과도하게 많아지면 각 `task_struct`가 CPU에서 연속으로 실행되는 시간이 짧아집니다.

### OS Kernel 계층 메커니즘 실체

**CFS (Completely Fair Scheduler)**: Linux의 기본 스케줄러. `vruntime`(가상 실행 시간)을 기준으로 Red-Black Tree에서 가장 작은 `vruntime`을 가진 `task_struct`를 다음 실행 대상으로 선택합니다.

**Time Slice (Scheduling Latency)**: CFS는 고정된 Time Slice가 아닌 Scheduling Latency(`/proc/sys/kernel/sched_latency_ns`, 기본 6ms~24ms)를 Runnable Task 수로 나눠 각 Task의 실행 시간을 결정합니다. Runnable Task가 많을수록 각 Task의 실행 시간이 짧아집니다.

**최소 실행 단위 (min_granularity)**: `sched_min_granularity_ns`(기본 0.75ms)보다 짧게 실행되지는 않도록 보장하지만, Task가 과다하면 이 보장도 실질적 Cache 활용에는 부족합니다.

**Runqueue / Wait Queue**:
- **Runqueue**: Runnable 상태(`TASK_RUNNING`)의 `task_struct`를 보관하는 Per-CPU Red-Black Tree
- **Wait Queue**: I/O 대기 등으로 Sleeping(`TASK_INTERRUPTIBLE`, `TASK_UNINTERRUPTIBLE`) 상태인 `task_struct` 보관

**cgroup CPU Throttling**: 컨테이너 환경에서는 cgroup v2의 CFS Bandwidth Control이 CPU 사용량을 제한합니다. `cpu.max`에 설정된 Quota를 소진하면 Throttling 발생 → 해당 cgroup의 모든 Task가 Runqueue에서 제거되고 Period 갱신까지 대기합니다. 이 과정에서 Cache Warm-up이 무효화됩니다.

```
Runnable task_struct 증가
  ↓ CFS Scheduling Latency 분할 → 각 Task 실행 시간 단축
  ↓ Cache Warm-up 불충분 상태에서 Context Switch 발생
  ↓ Cache Locality 저하
  ↓ Cache Miss 증가
  ↓ CPU Stall 증가
```

### SRE 분석 도구 / 관찰 키워드

```bash
# Run Queue Length 확인
vmstat 1  # r 항목 (실행 중 + Runnable 대기 Task 수)

# Load Average (Runnable + D 상태 Task 누적)
uptime
/proc/loadavg

# Per-CPU Runqueue 상태
mpstat -P ALL 1

# cgroup CPU Throttling 확인
cat /sys/fs/cgroup/cpu.stat  # throttled_time, nr_throttled
cat /sys/fs/cgroup/cpu.max   # Quota/Period

# Kubernetes에서 Throttling 확인
kubectl top pods
# cadvisor 메트릭: container_cpu_cfs_throttled_seconds_total
```

---

## 6. 구조별 Cache Miss 발생 패턴

### Thread-per-request 구조

요청 수가 증가할수록 Thread 수가 증가하고, 각 Thread는 서로 다른 Stack, 지역 변수, 요청 데이터를 사용합니다.

**OS Kernel 계층**: Thread 생성 시 `clone()` 시스템 콜이 호출되어 새로운 `task_struct`가 생성됩니다. 각 Thread는 고유 Stack을 가지며 이 Stack 데이터는 Cache Line을 독점적으로 사용합니다.

**Runtime 계층 (JVM 예시)**: JVM Thread는 OS Thread(`NPTL` 기반 pthread)에 1:1 매핑됩니다. Thread별 TLAB(Thread-Local Allocation Buffer)이 할당되며, Thread 수가 증가할수록 Heap 내 TLAB 분산도 증가하여 GC 시 Cache 효율이 저하됩니다. JIT Compiler(C1/C2)의 Compiled Method 캐시(Code Cache)도 다수 Thread에서 동시 접근 시 I-Cache Pressure가 증가합니다.

**Application 계층**: Connection Pool Exhaustion 상태에서 신규 Thread가 Connection 획득을 위해 대기(Blocking)하면, 대기 Thread가 Stack에 올려둔 데이터가 Cache에서 Evict된 후 재실행 시 Cache Miss가 발생합니다.

```
요청 증가  →  Thread 증가  →  task_struct 증가
  ↓ 각 Thread의 Stack / TLAB / 요청 데이터가 Cache를 분산 점유
  ↓ Context Switch 증가  →  Cache Miss 증가
  ↓ JVM: TLAB 분산 → Minor GC 빈도 증가 → Safepoint 증가
```

**JVM Safepoint**: GC 등 JVM 전역 작업을 위해 모든 Thread를 안전한 상태로 만드는 지점입니다. Thread 수가 많을수록 모든 Thread가 Safepoint에 도달하는 시간(Time to Safepoint)이 증가하고, 이 대기 시간 동안 CPU는 Stall 상태가 됩니다.

### SRE 분석 도구 / 관찰 키워드

```bash
# JVM Thread 수 / Safepoint 확인
jstat -gcutil <PID> 1000
-Xlog:safepoint*  # JVM 옵션으로 Safepoint 로그 출력

# Thread별 CPU 사용 확인
top -H -p <PID>

# JVM TLAB 통계
-Xlog:gc+tlab*

# Connection Pool 상태 (HikariCP 예시)
# hikaricp_connections_pending 메트릭 모니터링
```

### Blocking I/O 구조

I/O 대기 중 Thread가 Sleeping 상태로 전환되고, 다른 `task_struct`가 CPU를 점유합니다.

**OS Kernel 계층**: `read()`, `write()` 등 Blocking 시스템 콜 실행 시 Kernel은 해당 `task_struct`를 `TASK_INTERRUPTIBLE` 상태로 전환하고 Wait Queue에 등록합니다. I/O 완료 시 IRQ Handler → SoftIRQ(ksoftirqd) → Wait Queue에서 `task_struct` Wake-up → Runqueue 삽입이 일어납니다.

**Hardware 계층**: NIC로부터 패킷 수신 시 하드웨어 인터럽트(IRQ)가 발생하고, DMA 완료 신호가 CPU에 전달됩니다. 이후 TCP/IP Stack 처리는 SoftIRQ(NET_RX_SOFTIRQ)가 담당합니다. 네트워크 트래픽 폭증 시 SoftIRQ가 CPU를 과점할 수 있습니다.

**Socket Buffer (sk_buff)**: Kernel은 각 TCP 연결에 대해 sk_buff 구조체로 패킷 데이터를 관리합니다. 연결 수가 많아지면 sk_buff 관련 메모리가 Cache를 압박합니다.

**TCP Backlog / SYN Queue**: 새 TCP 연결 요청은 SYN Queue에 임시 보관되고, 완전히 수립된 연결은 Accept Queue(Backlog)로 이동합니다. Queue가 포화되면 SYN Drop 또는 SYN Cookie가 발동합니다. Application이 `accept()`를 충분히 빠르게 호출하지 않으면 Backlog가 쌓이고, Blocking 상태 Thread가 증가합니다.

**Dirty Page Writeback**: 쓰기 I/O가 많은 경우 Kernel의 Page Cache에 Dirty Page가 누적됩니다. `pdflush` / `kworker`가 주기적으로 Writeback을 수행하며, Writeback 중 I/O 대기 Thread의 Cache 데이터가 Evict될 수 있습니다.

```
Thread 실행  →  Blocking I/O 요청 (read/write syscall)
  ↓ task_struct → TASK_INTERRUPTIBLE → Wait Queue 등록
  ↓ 다른 task_struct 실행 (Cache 교체 / TLB 상태 변경)
  ↓ IRQ → SoftIRQ → Wait Queue Wake-up
  ↓ task_struct → TASK_RUNNING → Runqueue 삽입
  ↓ I/O 완료 후 재실행 시 → Cache Miss 가능성 높음
```

### SRE 분석 도구 / 관찰 키워드

```bash
# I/O 대기 확인
iostat -x 1
/proc/diskstats

# SoftIRQ CPU 점유 확인
mpstat -P ALL 1  # %soft 항목
/proc/softirqs

# sk_buff / TCP 상태 확인
ss -s
cat /proc/net/sockstat

# TCP Backlog / SYN Queue 확인
ss -lnt  # Recv-Q: Accept Queue 대기 수
cat /proc/sys/net/core/somaxconn
cat /proc/sys/net/ipv4/tcp_max_syn_backlog

# Dirty Page Writeback 상태
cat /proc/vmstat | grep -E 'dirty|writeback'
```

---

## 7. Cache Miss가 Scheduler Overhead를 증가시키는 구조

### OS Kernel 계층 메커니즘 실체

**Time Slice 소진 증가**

Cache Miss로 작업 시간이 길어지면 CFS에서 할당된 실행 시간(`sched_slice()` 반환값)을 모두 소진하는 경우가 늘어납니다. Kernel은 Timer IRQ(`hrtimer`) 기반으로 주기적으로 `scheduler_tick()`을 호출하여 현재 Task가 Time Slice를 소진했는지 검사합니다. 소진 시 `TIF_NEED_RESCHED` 플래그를 설정하고, 다음 Kernel 복귀 시점에 `schedule()`이 호출됩니다.

```
Cache Miss 증가  →  명령어 처리 시간 증가  →  Time Slice 소진 빈도 증가
  ↓ TIF_NEED_RESCHED 플래그 설정 빈도 증가
  ↓ Preemption (비자발적 Context Switch) 증가
  ↓ Scheduler 개입 빈도 증가
```

**Runnable Queue 관리 부담 증가**

Runnable `task_struct`가 많을수록 CFS는 Red-Black Tree에서 더 많은 실행 후보를 관리해야 합니다. 삽입(`__enqueue_entity()`)과 선택(`__pick_first_entity()`) 연산은 O(log N)이지만, N이 수백~수천에 이르면 Scheduler 자체가 CPU 자원을 소비합니다.

**Context Switch 후 Runnable Queue 재정렬 증가**

Context Switch 이후 `task_struct`는 증가한 `vruntime` 기준으로 Runnable Queue에 재삽입됩니다. 전환 빈도가 높을수록 삽입, 선택, 재정렬 작업이 증가하여 Scheduler 함수 자체가 CPU 프로파일링에 상위에 위치하게 됩니다.

**PSI (Pressure Stall Information)**: Linux 4.20+에서 제공하는 자원 압력 지표입니다. `some`은 일부 Task가 자원 대기 중인 비율, `full`은 전체 Task가 대기 중인 비율을 나타냅니다. Cache Miss 증가 → CPU `full` PSI 상승 패턴을 관찰할 수 있습니다.

### SRE 분석 도구 / 관찰 키워드

```bash
# Scheduler 함수 CPU 점유 분석
perf top -g  # schedule(), context_switch() 비중 확인
perf report  # Kernel 함수별 CPU 사용률

# PSI 확인
cat /proc/pressure/cpu
cat /proc/pressure/memory
cat /proc/pressure/io

# Scheduler 통계
cat /proc/schedstat
cat /proc/sched_debug  # 모든 CPU의 Runqueue 상태

# 비자발적 Context Switch 비율
pidstat -w -p <PID> 1  # nvcswch/s 높으면 Time Slice 소진 빈번
```

---

## 8. Scheduler Overhead와 CPU Saturation

Scheduler는 Kernel 코드로, CPU에서 실행됩니다. Scheduler 실행 빈도가 높아질수록 실제 비즈니스 로직이 사용할 수 있는 CPU 시간이 줄어듭니다.

### OS Kernel / Hardware 계층 메커니즘 실체

**System CPU Time (%sys)**: `top`, `mpstat`에서 표시되는 `%sys`는 Kernel 모드에서 소비된 CPU 시간입니다. Scheduler, 시스템 콜 처리, IRQ 처리 등이 포함됩니다. `%sys`가 높다는 것은 Kernel Overhead가 크다는 신호입니다.

**IRQ / SoftIRQ 경합**: 높은 네트워크 트래픽이나 디스크 I/O에서 IRQ, SoftIRQ가 CPU를 과점하면 Scheduler 실행 기회 자체가 지연됩니다. RPS(Receive Packet Steering) / RFS(Receive Flow Steering)를 통해 NIC 인터럽트를 여러 CPU에 분산시켜 SoftIRQ 부하를 분산할 수 있습니다.

**Application 계층 — Backpressure / Retry Storm**: 하위 서비스의 지연이 상위 서비스의 Thread를 Blocking 상태로 유지시키면, 상위 서비스에 Runnable Task가 쌓이고 Cache Miss + Scheduler Overhead가 연쇄 발생합니다. Retry Storm은 실패한 요청을 즉시 재시도함으로써 Runnable Task 수를 폭증시키는 패턴입니다.

**Circuit Breaker**: Retry Storm 방지를 위해 일정 임계치 이상 실패 시 요청을 차단합니다. Circuit Breaker 없이 Retry가 반복되면 Thread Pool이 포화되고, 각 Thread의 Cache 데이터가 Evict된 상태에서 재실행되어 Cache Miss가 배가됩니다.

**Serialization / Deserialization 비용**: JSON, Protobuf 등의 직렬화/역직렬화는 임시 객체를 대량 생성합니다. JVM 환경에서 이 객체들이 Eden Space를 빠르게 채우면 Minor GC 빈도가 증가하고, GC Safepoint에서 모든 Thread가 중단되어 Cache Warm-up이 무효화됩니다.

**Direct Memory / Off-Heap**: Netty 등의 고성능 프레임워크는 JVM Heap 외부의 Off-Heap 메모리(DirectByteBuffer)를 사용합니다. Off-Heap 데이터도 CPU Cache에 올라가므로, Off-Heap 사용량이 크면 Heap 데이터와 Cache를 경쟁합니다.

```
Cache Miss 증가
  ↓ 작업 시간 증가  →  Context Switch 증가
  ↓ Scheduler Overhead 증가 (%sys 상승)
  ↓ IRQ / SoftIRQ 경합 → %irq / %soft 상승
  ↓ 비즈니스 로직 실행 시간 감소
  ↓ CPU Saturation  →  Throughput 감소 / Latency 증가
```

### SRE 분석 도구 / 관찰 키워드

```bash
# CPU 사용 분해 확인
mpstat -P ALL 1
# %usr: 유저 모드, %sys: Kernel 모드, %irq: 하드 IRQ, %soft: SoftIRQ, %idle: 유휴

# RPS/RFS 설정 확인 (NIC 인터럽트 분산)
cat /sys/class/net/<eth>/queues/rx-0/rps_cpus

# JVM GC / Safepoint 분석
-Xlog:gc*,safepoint*::time,uptime
jstat -gcutil <PID> 1000
async-profiler --event cpu,alloc -f profile.html -d 30 <PID>

# Off-Heap / Direct Memory 확인
jcmd <PID> VM.native_memory
NMT (Native Memory Tracking) 활성화: -XX:NativeMemoryTracking=summary
```

---

## 9. Thrashing

실제 작업보다 실행 흐름 전환과 스케줄링 관리에 과도한 CPU 자원이 소비되는 상태입니다.

### 전 계층 메커니즘 실체

**Hardware**: Cache Line이 지속적으로 교체(Evict)되어 유효 Cache 활용률이 극히 낮아집니다. IPC(Instructions Per Cycle)가 0.1~0.3 수준으로 떨어지며, `perf stat`에서 `stalled-cycles-backend`가 압도적으로 높게 나타납니다.

**OS Kernel**: Runnable Queue가 항상 포화 상태이며, `schedule()` 함수가 `perf top`에서 상위에 위치합니다. `vmstat`의 `cs`(Context Switch) 값이 수만~수십만/초에 이릅니다.

**Runtime (JVM)**: GC Overhead Limit이 반복 초과되거나(`OutOfMemoryError: GC overhead limit exceeded`), Safepoint에 소요되는 시간 비율이 전체 실행 시간의 10% 이상을 차지합니다. ClassLoader Leak이나 Finalization Queue 누적은 별도 Memory 압박을 유발하여 GC 부하를 가중시킵니다.

**Application**: Connection Pool Exhaustion 상태에서 요청을 처리할 Thread가 없고, 대기 중인 Thread들이 Runqueue를 점유합니다. 이 상태에서 신규 요청이 Retry를 반복하면 Runnable Task 수가 폭증합니다.

```
Thread 과다 (Thread-per-request 또는 Retry Storm)
  ↓ Context Switch 과다
  ↓ Cache Locality 붕괴  →  Cache Miss 폭증
  ↓ CPU Stall 증가  →  IPC 급락
  ↓ Scheduler Overhead 증가 (%sys 급등)
  ↓ Throughput 감소 (CPU 사용률은 100% 근접)
  ↓ Latency 폭증 (P99가 수 초 이상으로 증가)
```

CPU 사용률은 높지만 서비스 처리량은 낮은 상태가 지속됩니다.

### SRE 분석 도구 / 관찰 키워드

```bash
# Thrashing 복합 지표 한눈에 확인
vmstat 1
# r 높음(Runnable 과다) + cs 높음(Context Switch 과다) + sy 높음(Kernel 시간 과다) 동시 발생

# IPC 확인 (0.5 이하 → Memory-bound Thrashing 의심)
perf stat -e cycles,instructions -a sleep 5

# JVM GC Overhead
jstat -gcutil <PID> 1000  # GCT(GC 누적 시간) 비율 확인
-Xlog:gc*

# ClassLoader Leak / Finalization Queue
jcmd <PID> GC.class_stats
jmap -histo <PID> | grep Finalizer
```

---

## 10. Non-blocking 구조와 Cache 효율

Event-loop 기반 Non-blocking 구조는 적은 수의 `task_struct`로 많은 요청을 처리합니다.

### 전 계층 메커니즘 실체

**Hardware**: 소수의 EventLoop Thread가 CPU에 고정적으로 실행되면 해당 Thread의 데이터가 Cache에 지속적으로 유지됩니다. IPC가 높게 유지되고 Cache Miss Rate가 낮습니다.

**OS Kernel**: `epoll_wait()` 시스템 콜로 단일 Thread가 수천~수만 개의 파일 디스크립터를 비동기로 감시합니다. I/O 이벤트 발생 시 `epoll`이 해당 `task_struct`를 Wake-up하여 Runnable Queue에 삽입합니다. I/O 대기 중 Thread는 Sleep 상태이므로 CPU를 점유하지 않습니다. Context Switch는 EventLoop Thread 수에 비례하여 극히 적습니다.

**Runtime (JVM — Project Loom / Virtual Thread)**: Java 21+의 Virtual Thread는 OS Thread(`carrier thread`)에 M:N으로 매핑됩니다. Blocking I/O 발생 시 Virtual Thread는 Unmount되어 Heap에 저장되고, Carrier Thread는 다른 Virtual Thread를 실행합니다. 실제 OS `task_struct` 수는 CPU Core 수에 근접하게 유지되어 Cache Locality가 보존됩니다.

**Application**: Netty, Spring WebFlux, Node.js, Go Runtime(goroutine + netpoller)이 이 구조를 채택합니다. Backpressure 구현이 필수적으로 요구되며, 처리 속도보다 요청 유입 속도가 빠를 경우 Event Queue가 메모리를 과점하거나 OOM이 발생할 수 있습니다.

**JNI Critical Section**: JVM과 Native 코드(JNI) 혼용 시 JNI Critical Section 내에서 GC가 차단됩니다. Non-blocking 구조에서도 JNI 사용이 많으면 Safepoint 지연이 발생합니다.

```
적은 수의 EventLoop Thread (CPU Core 수에 근접)
  ↓ Context Switch 최소화
  ↓ TLB Flush 최소화
  ↓ Cache Locality 유지  →  Cache Miss Rate 낮음
  ↓ IPC 높음  →  CPU 효율 증가  →  Throughput 증가
```

### SRE 분석 도구 / 관찰 키워드

```bash
# EventLoop Thread 수 / CPU Affinity 확인
top -H -p <PID>
taskset -cp <TID>

# epoll 이벤트 추적
strace -e epoll_wait,epoll_ctl -p <PID>

# Virtual Thread (Java 21+) 분석
-Djdk.tracePinnedThreads=full  # Pinned(블락) Virtual Thread 추적
jcmd <PID> Thread.dump_to_file -format=json threads.json

# Netty Off-Heap 메모리 (Direct Memory)
-Dio.netty.leakDetection.level=advanced
PooledByteBufAllocator.DEFAULT.metric()  # 메트릭 확인
```

---

## 11. SRE 관점 주요 지표

| 지표 | 의미 | 관련 계층 |
|------|------|-----------|
| Context Switch Rate | `task_struct` 전환 빈도 | OS Kernel |
| Run Queue Length | CPU 대기 task 수 | OS Kernel |
| Load Average | Runnable 및 D 상태 task 누적 수 | OS Kernel |
| CPU Usage (%usr / %sys) | 유저 / Kernel 모드 CPU 사용률 | OS Kernel / Hardware |
| CPU %irq / %soft | 하드 IRQ / SoftIRQ CPU 비중 | Hardware / OS Kernel |
| IPC (Instructions Per Cycle) | CPU 효율 (낮을수록 Stall 많음) | Hardware |
| Cache Miss Rate | Cache 효율 저하 여부 | Hardware |
| TLB Miss Rate | 주소 변환 Cache 효율 | Hardware |
| Memory Bandwidth Utilization | 메모리 버스 포화 여부 | Hardware |
| PSI (cpu/memory/io) | 자원 압력 지표 | OS Kernel |
| Throughput | 실제 처리량 | Application |
| P99 Latency | 지연 시간 변동성 | Application |
| GC Overhead / Safepoint Time | JVM GC 및 중단 시간 비율 | Runtime (JVM) |
| cgroup CPU Throttled Time | 컨테이너 CPU Quota 소진 | OS Kernel |

### 확인 명령어 종합

```bash
# Context Switch 전체 확인
vmstat 1

# Thread별 CPU 사용 확인
top -H -p <PID>

# Context Switch 상세 확인
pidstat -w -p <PID> 1

# Hardware Cache Miss / IPC / Pipeline Stall 분석
perf stat -e cache-references,cache-misses,LLC-load-misses,\
cycles,instructions,stalled-cycles-backend,branch-misses \
-p <PID> sleep 10

# TLB Miss 분석
perf stat -e dTLB-load-misses,dTLB-store-misses -p <PID>

# False Sharing (Cache Line Thrashing) 분석
perf c2c record -p <PID> && perf c2c report

# PSI (자원 압력 지표)
cat /proc/pressure/cpu
cat /proc/pressure/memory

# CPU 사용 계층 분해
mpstat -P ALL 1  # %usr, %sys, %irq, %soft

# Scheduler 함수 비중
perf top -g

# SoftIRQ 상세
watch -n 1 cat /proc/softirqs

# NUMA 메모리 접근 분포
numastat -p <PID>

# cgroup CPU Throttling
cat /sys/fs/cgroup/cpu.stat

# JVM 종합 분석
async-profiler -e cpu,alloc,lock -f profile.html -d 30 <PID>
jstat -gcutil <PID> 1000
```

---

## 12. 최종 정리

CPU Cache Miss는 단순한 메모리 접근 지연이 아니라, Hardware → OS Kernel → Runtime → Application 전 계층에 걸쳐 Scheduler Overhead와 CPU Saturation으로 이어질 수 있는 핵심 원인입니다.

```
[Hardware]
  Cache Line Miss → RAM 접근 → CPU Pipeline Stall
  Branch Misprediction / NUMA Remote Access / Memory Bandwidth Saturation
  TLB Flush (Context Switch 시) → 주소 변환 Miss 연쇄
    ↓
[OS Kernel]
  Runnable task_struct 증가 (Thread-per-request / Retry Storm)
    ↓ Context Switch 증가 (schedule() / switch_to() / switch_mm())
    ↓ Cache Locality 저하 → Cache Miss 증가
    ↓ CPU Stall 증가 → 작업 시간 증가 → Time Slice 소진 빈도 증가
    ↓ CFS Red-Black Tree 관리 부담 증가
    ↓ Scheduler Overhead 증가 (%sys 상승)
    ↓ IRQ / SoftIRQ 경합 → %irq / %soft 상승
    ↓ cgroup CPU Throttling → Cache Warm-up 무효화
    ↓
[Runtime - JVM 예시]
  TLAB 분산 → Minor GC 빈도 증가
    ↓ Safepoint 증가 → 전체 Thread 중단 → Cache 무효화
    ↓ JIT C1/C2 Code Cache I-Cache Pressure 증가
    ↓
[Application]
  Connection Pool Exhaustion → Thread 대기 증가
    ↓ Backpressure 부재 → Retry Storm → Runnable Task 폭증
    ↓ Serialization 오버헤드 → 임시 객체 폭증 → GC 부하 증가
    ↓
CPU Saturation  →  Throughput 감소 / P99 Latency 폭증  →  Thrashing
```

SRE 관점에서는 CPU 사용률 단일 지표만 관찰하는 것이 아니라, 아래 지표를 복합적으로 모니터링해야 합니다.

- **Hardware**: IPC, Cache Miss Rate, TLB Miss Rate, Memory Bandwidth
- **OS Kernel**: Context Switch Rate, Runqueue Length, %sys / %irq / %soft, PSI, cgroup Throttled Time
- **Runtime**: GC Overhead, Safepoint Time, TLAB Allocation Rate
- **Application**: Throughput, P99 Latency, Connection Pool 대기 수, Retry Rate

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*