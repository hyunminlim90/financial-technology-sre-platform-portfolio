# CPU Pipeline(파이프라인)과 명령어 실행 구조 (E2E 분석 적용됨)

## 1. CPU Pipeline이란?

CPU Pipeline은 **명령어 실행 과정을 여러 단계(Stage)로 분할하여 동시에 처리하는 CPU 내부 실행 구조**입니다.

하나의 명령어를 완료한 후 다음 명령어를 처리하는 방식이 아니라, 서로 다른 단계에 여러 명령어를 동시에 배치하여 처리량(Throughput)을 증가시킵니다.

| 목적 | 설명 | 계층 |
|---|---|---|
| **Throughput 향상** | 단위 시간당 처리 명령어 증가 | Hardware |
| **CPU 자원 활용 극대화** | 유휴 실행 유닛 감소 | Hardware |
| **IPC 증가** | 사이클당 명령 처리량 증가 | Hardware / OS |
| **병렬 처리** | 여러 명령어 동시 진행 | Hardware / Runtime |

### 계층별 실제 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|---|---|---|---|
| **CPU Fetch/Decode** | Hardware | Branch Predictor가 PC(Program Counter) 기반으로 다음 명령어 주소 결정. Branch Target Buffer(BTB), Return Stack Buffer(RSB) 참조 | `perf stat -e branch-misses,branch-instructions` |
| **CPU Frequency Scaling** | Hardware + Kernel | P-state(성능 상태): CPU 전압/주파수 조합. C-state(전력 상태): 유휴 시 Halt/Sleep. intel_pstate / acpi-cpufreq 드라이버가 제어 | `cpupower frequency-info`, `/sys/devices/system/cpu/cpu*/cpufreq/`, `turbostat` |
| **NUMA 메모리 접근** | Hardware + Kernel | 멀티소켓 환경에서 원격 NUMA 노드 메모리 접근 시 추가 Latency(~100ns) 발생. Pipeline MEM Stage가 직접 영향을 받음 | `numactl --hardware`, `numastat`, `perf stat -e node-load-misses` |
| **Memory Bandwidth Saturation** | Hardware | 다수 코어가 동시에 메모리 버스를 경쟁할 때 Pipeline MEM Stage가 지속적으로 Stall. DDR 채널 포화 상태 | `pcm-memory`, `perf stat -e offcore_requests_outstanding.cycles_with_data_rd`, `ipmctl show -memoryresources` |
| **JIT Compilation (C1/C2)** | JVM Runtime | JVM이 Hot Method를 감지하면 Interpreter → C1(빠른 컴파일) → C2(최적화 컴파일) 순으로 승격. C2는 Loop Unrolling, Inlining, Escape Analysis 적용으로 Pipeline 효율 극대화 | `-XX:+PrintCompilation`, `jitwatch`, `perf record -g java` |

---

## 2. Classic 5-Stage Pipeline 구조

전통적인 RISC CPU는 다음 5단계 Pipeline 구조를 사용합니다.

| 단계 | 이름 | 역할 | 계층 |
|---|---|---|---|
| **IF** | Instruction Fetch | 명령어 읽기 | Hardware |
| **ID** | Instruction Decode | 명령어 해석 | Hardware |
| **EX** | Execute | 연산 수행 | Hardware |
| **MEM** | Memory Access | 메모리 접근 | Hardware / OS |
| **WB** | Write Back | 결과 저장 | Hardware |

### 단계별 상세

**IF — Instruction Fetch**

PC(Program Counter)를 기반으로 Instruction Cache(L1i) 또는 메모리에서 다음 명령어를 가져옵니다. Branch Predictor가 다음 명령어 주소 결정에 관여합니다.

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|---|---|---|---|
| **Branch Prediction** | Hardware | TAGE Predictor, BTB, RSB 기반 분기 예측. 예측 실패 시 Pipeline Flush + Re-fetch 발생 | `perf stat -e branch-misses`, `perf annotate` |
| **Instruction Cache Miss** | Hardware | L1i Cache Miss 시 L2 → L3 → RAM 순서로 Fetch 지연 발생 | `perf stat -e L1-icache-load-misses`, `perf stat -e iTLB-load-misses` |
| **TLB (Translation Lookaside Buffer)** | Hardware + Kernel | 가상 주소 → 물리 주소 변환 캐시. Process 다수 또는 mmap 과다 시 TLB Miss 증가. Context Switch 시 TLB Flush 발생 | `perf stat -e dTLB-load-misses,dTLB-store-misses`, `/proc/meminfo`의 `HugePages` 항목 |
| **HugePage / THP** | Kernel | THP(Transparent HugePage)로 4KB → 2MB 페이지 승격 시 TLB Miss 감소. 단, 승격 과정의 Compaction이 일시적 Stall 유발 | `/sys/kernel/mm/transparent_hugepage/enabled`, `grep AnonHugePages /proc/meminfo` |

**ID — Instruction Decode**

읽어온 명령어를 해석하여 어떤 연산인지, 어떤 Register를 사용하는지, 메모리 접근이 필요한지 분석합니다. x86에서는 CISC 명령어를 내부 Micro-Op(μOp)으로 분해하는 과정이 포함됩니다.

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|---|---|---|---|
| **Micro-Op Decode** | Hardware | x86 Complex Instruction → 여러 μOp 분해. Decode 병목 시 Issue Queue 미충족 | `perf stat -e uops_issued.any,uops_retired.stall_cycles` |
| **Register Renaming** | Hardware | RAW/WAW/WAR Hazard 제거를 위해 Physical Register File을 논리 레지스터에 매핑. Out-of-Order Execution의 전제 조건 | `perf stat -e int_misc.recovery_cycles` |

**EX — Execute**

실제 연산이 수행됩니다.

| 실행 유닛 | 역할 | 관련 메커니즘 |
|---|---|---|
| **ALU** | 정수 연산 | Add, Sub, Shift, Logic |
| **FPU** | 부동소수점 연산 | IEEE 754 Floating Point, SIMD(AVX/SSE) |
| **Branch Unit** | 분기 계산 | Conditional Jump, Indirect Branch |
| **Address Generator (AGU)** | 주소 계산 | Load/Store 주소 사전 계산 |

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|---|---|---|---|
| **Out-of-Order Execution** | Hardware | ROB(Reorder Buffer) + Reservation Station 기반. 독립 명령어를 순서 무관 실행 후 ROB에서 순서대로 Commit | `perf stat -e machine_clears.count` |
| **CPU Pipeline Stall** | Hardware | 의존성·자원 충돌로 Pipeline 진행이 멈추는 상태. Memory Latency, Branch Misprediction, Structural Hazard가 주요 원인 | `perf stat -e stalled-cycles-frontend,stalled-cycles-backend` |
| **SIMD / Vector Unit** | Hardware | AVX-512 등 벡터 연산 유닛. JVM C2 JIT가 Auto-Vectorization 적용 시 활용 | `perf stat -e fp_arith_inst_retired.256b_packed_single` |

**MEM — Memory Access**

Load/Store 명령은 LSU(Load/Store Unit)를 통해 처리됩니다. Cache Hit 여부에 따라 실행 속도가 크게 달라집니다.

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|---|---|---|---|
| **Cache 계층 (L1/L2/L3)** | Hardware | L1d(~4 cycle) → L2(~12 cycle) → L3(~40 cycle) → RAM(~200 cycle) 순서로 접근. Cache Miss는 Pipeline Stall의 직접 원인 | `perf stat -e cache-misses,cache-references`, `perf mem record` |
| **Cache Line Thrashing** | Hardware | 여러 Thread가 동일 Cache Line(64B)에 경쟁적으로 Write 시 MESI 프로토콜 무효화 연쇄 발생. False Sharing이 대표적 패턴 | `perf c2c record`, `perf c2c report` |
| **LSU (Load/Store Unit)** | Hardware | Load Buffer / Store Buffer를 통해 메모리 접근 순서 관리. Store-to-Load Forwarding으로 Stall 감소 | `perf stat -e ld_blocks.store_forward`, `perf mem` |
| **Page Cache & Page Fault** | Kernel | 파일 데이터를 메모리에 캐시. Minor Fault: 물리 매핑만 필요. Major Fault: Disk에서 실제 로드 필요 → MEM Stage 장기 Stall | `vmstat`의 `pgfault/pgmajfault`, `/proc/vmstat`, `sar -B` |
| **mmap** | App ↔ Kernel | 파일/장치를 프로세스 가상 주소에 직접 매핑. Page Fault 발생 시 Kernel이 실제 로드. mmap 과다 시 TLB Miss 증가 | `/proc/PID/maps`, `vmstat`의 `pgfault`, `perf stat -e dTLB-load-misses` |

**WB — Write Back**

최종 연산 결과를 Register에 기록합니다. ROB(Reorder Buffer)에서 명령어가 순서대로 Retire되며, 이후 다음 명령어가 해당 값을 사용할 수 있습니다.

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|---|---|---|---|
| **ROB Retire** | Hardware | 모든 이전 명령어가 Commit된 이후에만 WB 허용(순서 보장). Exception 발생 시 ROB Flush | `perf stat -e uops_retired.retire_slots` |
| **Store Buffer Drain** | Hardware | Store Buffer의 데이터가 실제 Cache에 반영되는 시점. Memory Ordering 정책(x86: TSO)에 따라 결정 | `perf stat -e machine_clears.memory_ordering` |

---

## 3. Pipeline 병렬 처리 구조

Pipeline은 여러 명령어를 서로 다른 단계에서 동시에 처리합니다.

```text
Cycle 1:  IF
Cycle 2:  ID  | IF
Cycle 3:  EX  | ID  | IF
Cycle 4:  MEM | EX  | ID  | IF
Cycle 5:  WB  | MEM | EX  | ID  | IF
```

CPU는 각 사이클마다 새로운 명령어를 투입할 수 있습니다.

### 계층별 병렬 처리 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|---|---|---|---|
| **커널 스케줄러 (CFS)** | Kernel | Completely Fair Scheduler: Red-Black Tree 기반 vruntime 관리. Runqueue / Wait Queue 관리. Context Switch 수행 | `/proc/schedstat`, `perf sched`, `pidstat -w`, `vmstat`의 `cs` 항목 |
| **Context Switch** | Kernel | 프로세스 전환 시 레지스터 상태 저장/복원, TLB Flush, Cache Warm-up 비용 발생. Pipeline 재충전 필요 | `pidstat -w`, `perf stat -e context-switches`, `/proc/PID/status`의 `voluntary_ctxt_switches` |
| **cgroup / CPU Throttling** | Kernel | cgroup v2 기반 CFS Bandwidth Control. CPU Quota(cpu.max) 소진 시 Container Throttling 발생 → 전체 Pipeline 활용률 급락 | `/sys/fs/cgroup/cpu.stat`의 `throttled_usec`, `kubectl top`, `cat /sys/fs/cgroup/cpu.max` |
| **IRQ (하드웨어 인터럽트)** | Hardware → Kernel | NIC 패킷 수신, Disk I/O 완료 시 CPU 실행 중단 → IRQ Handler 실행. 현재 실행 중인 Pipeline 흐름에 직접 개입 | `/proc/interrupts`, `mpstat`의 `%irq`, `perf stat -e irq:*` |
| **SoftIRQ** | Kernel (후처리) | IRQ 후처리를 Kernel Thread에 위임. TCP/IP Stack 처리, 패킷 수신. ksoftirqd 커널 스레드가 처리. 네트워크 트래픽 폭증 시 CPU 점유 급증 | `mpstat`의 `%soft`, `/proc/softirqs`, `sar -I ALL` |
| **Off-CPU Time** | Kernel | 프로세스가 CPU를 점유하지 않고 대기하는 시간(I/O, Lock, Sleep). Pipeline 활용률과 직결 | `offcputime-bpfcc`, `perf sched latency`, `bpftrace -e 'tracepoint:sched:sched_switch'` |

---

## 4. IPC (Instructions Per Cycle)

Pipeline 성능의 핵심 지표입니다.

```text
IPC = 사이클당 처리된 명령어 수
```

높은 IPC는 CPU 자원 활용률이 높다는 의미입니다.

### IPC에 영향을 미치는 계층별 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|---|---|---|---|
| **Branch Misprediction** | Hardware | 예측 실패 시 Pipeline Flush → 15~20 Cycle Penalty(Deep Pipeline 기준). IPC 직접 저하 | `perf stat -e branch-misses` → Miss Rate(%) 확인 |
| **Memory Latency** | Hardware | Cache Miss로 인한 MEM Stage 지연이 IPC 저하의 최대 요인. LLC Miss 시 ~200 cycle 대기 | `perf mem record`, `perf stat -e LLC-load-misses` |
| **PSI (Pressure Stall Information)** | Kernel | CPU/Memory/IO 자원 부족 압력 측정. some: 일부 task 대기 / full: 전체 task 대기 → IPC 급락 구간 탐지 | `/proc/pressure/cpu`, `/proc/pressure/memory`, `/proc/pressure/io` |
| **JVM Safepoint** | JVM Runtime | JVM이 GC, 디컴파일, 바이어스 락 해제 등을 위해 모든 Thread를 안전 지점에서 정지. Safepoint 진입 대기 중 CPU 낭비(Spin) | `-XX:+PrintSafepointStatistics`, `jstack`, `perf stat`의 `stalled-cycles` |
| **TLAB (Thread-Local Allocation Buffer)** | JVM Runtime | Thread마다 별도 힙 영역을 할당하여 객체 생성 시 동기화 비용 제거. TLAB 소진 시 느린 경로(slow path) 할당 → IPC 저하 | `-XX:+PrintTLAB`, `-Xlog:gc+tlab=debug`, `jstat -gc` |

---

## 5. Pipeline Hazard

Pipeline의 실행 흐름을 방해하는 요소를 **Hazard**라고 합니다.

| Hazard 종류 | 설명 | 주요 발생 계층 |
|---|---|---|
| **Data Hazard** | 데이터 의존성으로 인한 대기 | Hardware |
| **Control Hazard** | 분기 예측 실패로 인한 경로 불확실 | Hardware |
| **Structural Hazard** | 하드웨어 자원 충돌 | Hardware |

### 5-1. Data Hazard

앞선 명령어의 결과가 아직 준비되지 않았는데 뒤 명령어가 해당 값을 필요로 하는 상황입니다.

```text
Instruction 1: R1 = R2 + R3
Instruction 2: R4 = R1 + R5   ← R1 결과가 아직 없음

→ Result Not Ready → Pipeline Stall
```

**Data Forwarding:** 현대 CPU는 연산 결과를 Register Write 이전에 직접 다음 단계로 전달하여 Stall을 줄입니다.

```text
EX Result → Direct Forward → Next Instruction
```

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|---|---|---|---|
| **Data Forwarding** | Hardware | EX → EX, MEM → EX 경로로 결과를 직접 전달. 레지스터 Write 완료를 기다리지 않음 | `perf stat -e ld_blocks.no_sr` |
| **Memory Ordering Stall** | Hardware | Store Buffer의 Write가 완료되기 전에 동일 주소 Load 시 발생. x86 TSO(Total Store Order)에서도 일부 발생 | `perf stat -e machine_clears.memory_ordering` |
| **JNI Critical Section** | JVM Runtime | JNI Critical Section 진입 시 GC 일시 정지 불가. 장시간 유지 시 GC Stall → 전체 Thread Block | `jstack` (JNI 상태 확인), `-verbose:jni` |
| **Finalization Queue** | JVM Runtime | Finalizer 큐 누적 시 GC가 객체를 회수하지 못해 힙 압박 증가 → Major GC 빈도 증가 → Off-CPU Stall | `jstat -gcutil`, `jmap -finalizerinfo` |

### 5-2. Control Hazard

분기 명령으로 인해 다음 실행 경로가 불확실한 상황입니다.

```text
if (x > 0)
→ 실제 결과가 나오기 전까지 다음 명령어 확정 불가
```

현대 CPU는 **Branch Predictor**를 사용하여 Pipeline 중단을 최소화합니다.  
예측 실패 시:

```text
Wrong Prediction
→ Pipeline Flush (15~20 Cycle 낭비)
→ Re-fetch (Instruction Cache 재접근)
→ Restart
```

깊은 Pipeline 구조에서는 Flush 비용이 매우 커집니다.

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|---|---|---|---|
| **Branch Misprediction Penalty** | Hardware | Deep Pipeline(20+ Stage)에서 Flush 비용은 20 Cycle 이상. Miss Rate 1%도 IPC에 큰 영향 | `perf stat -e branch-misses,branches` |
| **Spectre / IBRS 완화** | Hardware + Kernel | Spectre 취약점 패치(IBRS, Retpoline)로 간접 분기 예측 비용 증가. Kernel 진입 시마다 분기 예측 상태 초기화 | `spectre-meltdown-checker`, `perf stat`의 `branch-misses` 증가 확인 |
| **Branchless 코드** | App | 조건 분기를 CMOV(Conditional Move) 등으로 대체하여 Prediction Miss 제거. JVM C2가 자동 적용하기도 함 | `perf annotate` (CMOV vs JMP 비교), `-XX:+OptimizeStringConcat` |
| **JVM Deoptimization** | JVM Runtime | C2가 잘못된 가정(타입 프로파일 변경 등)으로 최적화한 코드를 Interpreter로 롤백. Pipeline이 최적화되지 않은 경로로 전환 | `-XX:+PrintDeoptimization`, `jitwatch` |

### 5-3. Structural Hazard

여러 명령어가 동시에 동일 하드웨어 자원을 요구하는 상황입니다.

```text
Instruction A → Memory Access
Instruction B → Memory Access
→ 동일 LSU / 메모리 포트를 동시에 사용할 수 없음
```

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|---|---|---|---|
| **Memory Port 경합** | Hardware | 최신 CPU도 Load Port와 Store Port 수가 제한적. Out-of-Order도 포트 수 이상은 동시 처리 불가 | `perf stat -e mem_inst_retired.all_loads,mem_inst_retired.all_stores` |
| **FPU 처리량 한계** | Hardware | FPU는 ALU보다 처리량이 낮음. AI/ML 워크로드에서 FPU Contention 발생 가능 | `perf stat -e fp_arith_inst_retired.*` |
| **시스템 콜 (syscall)** | App → Kernel | User Mode → Kernel Mode 전환(Ring 3 → Ring 0). `int 0x80` / `syscall` 명령어. CPU 파이프라인 Flush 포함 | `strace`, `perf trace`, `syscount`, `/proc/PID/syscall` |
| **vDSO** | App (User-side) | `gettimeofday()`, `clock_gettime()` 등을 Kernel 전환 없이 처리. Kernel 메모리 일부를 User Space에 매핑하여 직접 읽기 | `/proc/PID/maps`의 `vdso` 항목, `perf stat` (syscall 목록 미등장 확인) |

---

## 6. Deep Pipeline

현대 CPU는 20~30 Stage 이상의 긴 Pipeline 구조를 사용합니다.

| 구분 | 내용 |
|---|---|
| **장점** | 높은 Clock Frequency 달성, 세분화된 병렬 처리 |
| **단점** | Branch Penalty 증가, Stall 영향 증가, 설계 복잡성 증가 |

### Deep Pipeline 계층별 영향

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|---|---|---|---|
| **CPU Frequency Scaling (P-state)** | Hardware + Kernel | Turbo Boost: 온도·전력 여유 시 최대 주파수 초과. Thermal Throttling: 과열 시 주파수 강제 저하 | `turbostat`, `cpupower monitor`, `/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq` |
| **C-state Latency** | Hardware + Kernel | 깊은 절전 상태(C6 등)에서 복귀 시 수백 μs 지연. 지연 민감 서비스에서 C-state를 의도적으로 제한 | `cpupower idle-info`, `/sys/devices/system/cpu/cpu*/cpuidle/`, `cyclictest` |
| **Branch Flush Cost** | Hardware | Stage 수가 많을수록 Flush 시 버려지는 명령어 증가. 20-stage 파이프라인에서 예측 실패 1회 = 20 Cycle 낭비 | `perf stat -e branch-misses` |
| **Speculative Execution 취약점** | Hardware + Kernel | Meltdown/Spectre 패치(KPTI, IBRS)로 Kernel 진입 비용 증가. Deep Pipeline에서 패치 비용이 더 두드러짐 | `grep . /sys/devices/system/cpu/vulnerabilities/*`, `perf stat` 비교 |

---

## 7. 현대 CPU 고급 기술

### Superscalar Architecture

여러 Pipeline을 동시에 운영하여 여러 명령어를 병렬로 실행합니다.

```text
Pipeline A (Integer ALU)
Pipeline B (Integer ALU)
Pipeline C (FPU / SIMD)
Pipeline D (Load/Store)
```

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|---|---|---|---|
| **Issue Queue / Dispatch** | Hardware | 각 실행 포트에 μOp 배분. Issue Width(Intel: 6-wide)가 최대 IPC 상한 결정 | `perf stat -e uops_dispatched.thread` |
| **Retirement Queue (ROB)** | Hardware | 비순차 실행 후 순서대로 Commit. ROB 크기(Intel Skylake: 224 entry)가 Out-of-Order 윈도우 결정 | `perf stat -e uops_retired.total_cycles` |
| **SMT (Hyper-Threading)** | Hardware | 하나의 물리 코어에 두 Thread의 아키텍처 상태를 유지. Pipeline 공유로 자원 활용률 향상. 단, Cache 경합 증가 | `lscpu`의 `Thread(s) per core`, `perf stat -M pipeine` |
| **RPS / RFS** | Kernel | Receive Packet Steering / Receive Flow Steering: NIC 인터럽트를 여러 CPU에 분산하여 특정 CPU의 Pipeline Stall 방지 | `/sys/class/net/ethX/queues/rx-N/rps_cpus`, `ethtool -l`, `/proc/net/softnet_stat` |

### Out-of-Order Execution

CPU가 명령어 순서를 일부 재배치하여 Stall을 줄이고 실행 유닛 활용률을 높입니다.

```text
Instruction A Stall (Cache Miss)
→ Independent Instruction B 먼저 실행
→ A의 Cache Miss 응답 도착 후 A 재개
```

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|---|---|---|---|
| **Memory Disambiguation** | Hardware | Load가 이전 Store를 추월 실행 가능한지 동적으로 판단. 오판 시 Machine Clear 발생 | `perf stat -e machine_clears.count` |
| **Prefetch Unit** | Hardware | HW Prefetcher가 메모리 접근 패턴을 학습하여 Cache에 선제 로드. 랜덤 접근 패턴에서는 효과 없음 | `perf stat -e hw_prefetch_retired.*` |
| **JVM C2 Reordering** | JVM Runtime | JIT가 Java Memory Model(JMM) 범위 내에서 명령어 재배치. volatile 필드는 메모리 배리어 삽입 | `-XX:+PrintOptoAssembly`, `hsdis` 디스어셈블러 |

---

## 8. Pipeline과 Cache / LSU 관계

Pipeline의 MEM 단계는 LSU와 직접 연결됩니다.

```text
Pipeline MEM Stage → LSU → L1d Cache → L2 → L3 → RAM
                              ↓
                         TLB (주소 변환)
                              ↓
                        Page Table Walk (Miss 시)
```

| 상태 | 영향 | 계층 |
|---|---|---|
| **L1d Cache Hit** | ~4 cycle → Pipeline 유지 | Hardware |
| **LLC Cache Hit** | ~40 cycle → 짧은 Stall | Hardware |
| **Cache Miss → RAM** | ~200 cycle → 심각한 Stall | Hardware |
| **Page Fault (Minor)** | 물리 매핑만 필요 → 수십 cycle | Kernel |
| **Page Fault (Major)** | Disk I/O 필요 → ms 단위 Stall | Kernel |

### Cache / LSU 계층별 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|---|---|---|---|
| **Cache Line Thrashing** | Hardware | 동일 Cache Line에 여러 Thread Write 경합 → MESI 상태 전이 반복(Modified→Invalid→Modified). False Sharing이 대표적 원인 | `perf c2c record && perf c2c report` |
| **NUMA 원격 접근** | Hardware + Kernel | 원격 NUMA 노드 메모리 접근 시 ~100ns 추가 Latency. JVM Heap을 원격 노드에 할당 시 지속적 Cache Miss | `numastat -c`, `numactl --membind=N`, `perf mem -t load record` |
| **Dirty Page Writeback** | Kernel | 변경된 Page Cache를 Disk에 기록. `vm.dirty_ratio` 초과 시 Write Throttle 발생 → 애플리케이션 Write Stall | `/proc/vmstat`의 `nr_dirty`, `sar -b`, `echo 1 > /proc/sys/vm/drop_caches` (주의 필요) |
| **Direct Memory / Off-Heap** | App / JVM | JVM Heap 외부 메모리 사용(ByteBuffer.allocateDirect, Unsafe). GC 대상 아님. 과도 사용 시 Native Memory OOM | `jmap -heap`의 NonHeap, `NativeMemoryTracking`, `/proc/PID/status`의 `VmRSS` |
| **THP Compaction** | Kernel | THP 자동 승격 과정에서 Memory Compaction 발생. 짧은 Latency Spike 유발 가능. 지연 민감 서비스에서는 `madvise` 모드 권장 | `/proc/vmstat`의 `compact_migrate_scanned`, `/sys/kernel/mm/transparent_hugepage/defrag` |

---

## 9. CPU 성능 결정 요소

```text
Clock Frequency × IPC × Pipeline Efficiency = CPU 성능
```

Pipeline 효율이 낮으면 높은 GHz에서도 성능이 저하될 수 있습니다.

### 성능 결정 계층별 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|---|---|---|---|
| **Memory Bandwidth Saturation** | Hardware | 다수 코어 동시 메모리 접근 시 DDR 채널 포화. 단일 코어 성능 대비 전체 처리량이 선형 이하로 증가 | `perf stat -e offcore_requests_outstanding.*`, `mlc --bandwidth_matrix` |
| **CPU Frequency Scaling** | Hardware + Kernel | Intel P-state / AMD P-state: BIOS 설정, `scaling_governor`(performance/powersave/schedutil)에 따라 동작 | `cpupower frequency-set -g performance`, `turbostat`, `perf stat`의 `GHz` 확인 |
| **OOM Killer** | Kernel | 메모리 부족 시 Kernel이 점수(oom_score) 기반으로 프로세스 강제 종료. 해당 CPU의 Pipeline 흐름 완전 중단 | `dmesg | grep oom`, `/proc/PID/oom_score`, `oom_killer_adj` |
| **Serialization / Deserialization 비용** | App / JVM | JSON/Protobuf 처리 중 메모리 접근 패턴이 비연속적 → Cache Miss 증가 → IPC 저하. CPU-bound 병목의 주요 원인 | `async-profiler` (CPU 프로파일), `perf record -g -F 999 java` |
| **ClassLoader Leak** | JVM Runtime | 클래스 재로드 시 MetaSpace 누수. MetaSpace 포화 → Full GC → 모든 Thread Stall | `-XX:MaxMetaspaceSize`, `jmap -clstats`, `jcmd PID VM.classloaders` |

---

## 10. JVM 및 서버 애플리케이션 관점

| 영역 | Pipeline 영향 | 계층 |
|---|---|---|
| **복잡한 조건문** | Branch Prediction 실패 → Pipeline Flush 증가 | Hardware / App |
| **비효율적 객체 접근** | LSU Stall 유발, Cache Miss 증가 | Hardware / JVM |
| **Lock 경합** | 동기화 비용 → Pipeline 진행 방해 | OS / App |
| **GC Pause** | JVM Thread 정지 → CPU Pipeline 유휴 | JVM Runtime |
| **System Call 폭증** | User/Kernel 모드 전환 비용 → IPC 저하 | OS / App |

### JVM / 서버 애플리케이션 계층별 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|---|---|---|---|
| **Connection Pool Exhaustion** | App | DB/HTTP Connection Pool 소진 시 Thread가 획득 대기. 대기 Thread는 CPU를 소비하지 않지만 요청 처리 지연 → 응답 스택 Stall | `HikariCP metrics`, `jdbc.connections.pending`, `thread dump` |
| **Backpressure** | App | Producer가 Consumer 처리 속도 초과 시 Queue 포화 → Thread Block → Off-CPU 급증. Reactive 시스템에서 중요 | `reactor.netty metrics`, `Micrometer executor.queued`, `jstack` |
| **Circuit Breaker** | App | 연속 실패 시 빠른 실패(Fast Fail)로 불필요한 Thread Stall 방지. Pipeline 자원이 불필요한 대기 없이 다른 요청에 사용 가능 | `Resilience4j metrics`, `circuitbreaker.state`, `/actuator/health` |
| **Retry Storm** | App | 연쇄 재시도로 Upstream에 부하 집중 → CPU 과부하 → Pipeline Stall 연쇄 발생. Exponential Backoff + Jitter로 완화 | `access log 분석`, `httpRequests_total` 메트릭 급증 패턴 |
| **Safepoint** | JVM Runtime | GC 외에도 바이어스 락 해제, 코드 디옵티마이제이션 등으로 발생. TTSP(Time To Safepoint) 지연 = 일부 Thread가 Safepoint 진입 지연 | `-XX:+PrintSafepointStatistics -XX:PrintSafepointStatisticsCount=1` |
| **Futex (Fast Userspace Mutex)** | OS / App | Java synchronized 내부는 OS futex 기반. 경합 없으면 User Space에서 해결(Kernel 전환 없음). 경합 시 futex_wait() 시스템 콜 → Context Switch | `perf trace -e futex`, `strace -e futex java`, `/proc/PID/status`의 `nonvoluntary_ctxt_switches` |

---

## 11. Pipeline 최적화 전략

| 전략 | 목적 | 적용 계층 |
|---|---|---|
| **Branch 단순화** | Control Hazard 감소 | Hardware / App |
| **Cache 친화적 데이터 구조** | Memory Stall 감소 | Hardware / App |
| **연속 메모리 사용** | Cache Hit 증가 | Hardware / App |
| **Lock 최소화** | Stall 감소 | OS / App |
| **Hot Path 최적화** | IPC 향상 | JVM / App |
| **Branchless 코드** | Prediction Miss 감소 | Hardware / App |
| **HugePage 적용** | TLB Miss 감소 | Kernel / App |
| **NUMA Locality 보장** | 원격 메모리 접근 감소 | Hardware / OS |
| **CPU Frequency Governor 설정** | 클럭 안정성 확보 | Kernel |
| **SoftIRQ / RPS 분산** | 특정 CPU 과부하 방지 | Kernel |

### 전략별 계층 상세

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|---|---|---|---|
| **Cache-Friendly 자료구조** | App | Array of Structs → Struct of Arrays 전환으로 Cache Line 활용률 향상. JVM에서는 연속 메모리 보장을 위해 Primitive Array 사용 | `perf stat -e cache-misses` 비교 |
| **NUMA Binding** | Kernel / App | `numactl --membind` 또는 JVM `-XX:+UseNUMA`로 Heap을 로컬 노드에 할당. 원격 접근 최소화 | `numastat`, `-XX:+UseNUMA`, `numactl --membind=0` |
| **Lock-Free 알고리즘** | App | CAS(Compare-And-Swap) 기반 자료구조. Futex 시스템 콜 없이 User Space에서 동기화 → Context Switch 제거 | `java.util.concurrent.atomic.*`, `LongAdder` vs `AtomicLong` |
| **io_uring / Ring Buffer** | App ↔ Kernel | io_uring의 SQ/CQ Ring Buffer로 시스템 콜 횟수 최소화. Kernel이 데이터 기록 → App이 읽기. NIC Rx/Tx Ring Buffer도 동일 원리 | `io_uring_setup` syscall 추적, `/proc/PID/fdinfo`의 uring 항목, `ethtool -S` |
| **eBPF Map** | Kernel → App | eBPF 프로그램이 Kernel 이벤트에 훅. Kernel 내부 통계를 Map에 기록 → User 레벨 도구가 읽어 분석. 애플리케이션 무수정 커널 관찰 | `bpftool map`, `bpftrace`, `Cilium`, `/sys/fs/bpf/` |

---

## 12. CPU 내부 구성 요소 연결 구조

```text
                    ┌──────────────────────────┐
                    │   Instruction Fetch (IF)  │
                    │  L1i Cache + Branch Pred  │
                    └─────────────┬────────────┘
                                  │
                    ┌─────────────▼────────────┐
                    │  Instruction Decode (ID)  │
                    │  CISC → μOp 변환          │
                    │  Register Rename (RAT)    │
                    └─────────────┬────────────┘
                                  │
          ┌───────────────────────▼──────────────────────┐
          │           Issue Queue / Scheduler             │
          │   (Out-of-Order: 독립 명령어 먼저 디스패치)    │
          └───────┬──────────────┬───────────────┬───────┘
                  │              │               │
        ┌─────────▼──┐  ┌────────▼────┐  ┌──────▼──────┐
        │  ALU / BRU  │  │   FPU/SIMD  │  │   AGU/LSU   │
        │  정수·분기   │  │  부동소수점  │  │  주소·메모리 │
        └─────────┬──┘  └────────┬────┘  └──────┬──────┘
                  │              │               │
          ┌───────▼──────────────▼───────────────▼───────┐
          │          ROB (Reorder Buffer) — 순서 Commit   │
          └───────────────────────┬──────────────────────┘
                                  │
                    ┌─────────────▼────────────┐
                    │      Write Back (WB)      │
                    │  Physical Register File   │
                    └─────────────┬────────────┘
                                  │
          ┌───────────────────────▼──────────────────────┐
          │         메모리 서브시스템                        │
          │  L1d → L2 → L3 → RAM(NUMA 로컬/원격)           │
          │  TLB → Page Table → Kernel (Page Fault 처리)   │
          └───────────────────────┬──────────────────────┘
                                  │
          ┌───────────────────────▼──────────────────────┐
          │              OS / Runtime 계층                 │
          │  cgroup CPU Quota · Scheduler (CFS)           │
          │  IRQ / SoftIRQ / Context Switch               │
          │  JVM: JIT(C1/C2) · GC · Safepoint · TLAB     │
          └──────────────────────────────────────────────┘
```

---

## 13. 전체 개념 정리

| 구성 요소 | 역할 | 계층 |
|---|---|---|
| **Pipeline** | 명령어 병렬 실행 구조 | Hardware |
| **IF / ID / EX / MEM / WB** | 5단계 실행 단계 | Hardware |
| **Stall** | Pipeline 중단 | Hardware |
| **Hazard** | 실행 충돌 요소 | Hardware |
| **IPC** | 사이클당 명령 처리량 | Hardware |
| **Superscalar** | 다중 Pipeline 구조 | Hardware |
| **Out-of-Order** | 실행 순서 재배치 | Hardware |
| **Data Forwarding** | Stall 감소를 위한 결과 직접 전달 | Hardware |
| **TLB / HugePage** | 가상-물리 주소 변환 가속 | Hardware + Kernel |
| **CFS / cgroup** | CPU 자원 스케줄링 및 격리 | Kernel |
| **IRQ / SoftIRQ** | 하드웨어 이벤트 처리 | Hardware + Kernel |
| **Page Fault / Writeback** | 메모리 관리 | Kernel |
| **NUMA** | 멀티소켓 메모리 지역성 | Hardware + Kernel |
| **JIT (C1/C2)** | 런타임 코드 최적화 | JVM Runtime |
| **Safepoint / GC** | JVM 메모리 관리 중단점 | JVM Runtime |
| **Connection Pool / Backpressure** | 애플리케이션 자원 흐름 제어 | Application |

---

## 핵심 결론

현대 CPU는 단순 순차 실행 장치가 아니라 **다단계 병렬 실행 구조를 기반으로 동작하는 고성능 처리 시스템**입니다.

```text
Branch Prediction Accuracy        (Hardware)
  + Cache Hit Ratio               (Hardware + Kernel)
  + TLB Hit Ratio                 (Hardware + Kernel)
  + Low Context Switch Cost       (Kernel)
  + NUMA Locality                 (Hardware + Kernel)
  + Low cgroup Throttling         (Kernel)
  + JIT Optimization Quality      (JVM Runtime)
  + Low GC / Safepoint Overhead   (JVM Runtime)
  + Low Lock Contention           (App)
  + Low Retry / Backpressure      (App)
  = High IPC
  = High CPU Pipeline Efficiency
  = High System Throughput
```

이는 대규모 트래픽 처리 / JVM 기반 서버 / Kubernetes 환경 / 고성능 네트워크 처리 / 금융 시스템의 Latency 및 처리량에 직접적인 영향을 미칩니다.

---

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*