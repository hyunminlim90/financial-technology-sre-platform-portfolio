# Hardware Thread와 Physical Core / Logical CPU 구조 (E2E 분석 적용됨)

---

## 1. 개요

Hardware Thread는 Physical Core와 Logical CPU 사이를 연결하는 하드웨어 실행 단위다. 운영체제가 독립 실행 흐름으로 인식할 수 있는 최소 하드웨어 실행 컨텍스트(Hardware Execution Context)를 의미한다.

```
Physical Core
  ↓
Hardware Thread
  ↓
Logical CPU
  ↓
OS Scheduler
```

### 계층별 메커니즘 실체 — 개요

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **Physical Core 식별** | Hardware | `physical id` + `core id` 조합으로 Physical Core 구분. 동일 조합의 `processor` 항목이 2개면 SMT 활성화 상태 | `cat /proc/cpuinfo`, `lscpu`, `hwloc-info` |
| **Logical CPU 노출** | Kernel | Kernel이 각 Hardware Thread를 독립 `cpu_data` 구조체로 관리. `/sys/devices/system/cpu/cpuN/` 디렉터리로 노출 | `ls /sys/devices/system/cpu/`, `nproc`, `lscpu -e` |
| **OS Thread → Logical CPU 매핑** | Kernel (CFS) | `task_struct`의 `cpu` 필드에 현재 Logical CPU 번호 기록. CFS가 Runqueue에서 스케줄 시 이 값 갱신 | `/proc/PID/stat`의 38번째 필드(processor), `ps -o pid,psr` |
| **cgroup CPU 격리** | Kernel | `cpuset` cgroup으로 특정 Logical CPU 집합만 사용하도록 제한 가능. Kubernetes `CPU Manager Policy: static` 시 이 메커니즘 활용 | `/sys/fs/cgroup/cpuset/cpuset.cpus`, `kubectl describe node`의 `cpu manager policy` |

---

## 2. 세 계층의 정의

| 구성 요소 | 계층 | 정의 |
|-----------|------|------|
| Physical Core | Hardware Resource | 실제 연산을 수행하는 하드웨어 자원의 집합 |
| Hardware Thread | Hardware Execution Context | Physical Core 내부에서 독립 실행 상태를 유지하는 단위 |
| Logical CPU | OS Logical Layer | 운영체제 커널이 스케줄링 단위로 관리하는 논리적 실행 단위 |

### 계층별 메커니즘 실체 — 세 계층 실체

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **Physical Core** | Hardware | 독립된 ALU / FPU / LSU / Branch Predictor / L1-L2 Cache 보유. NUMA 토폴로지 상 특정 메모리 노드와 연결 | `lscpu`의 `Core(s) per socket`, `numactl --hardware`, `hwloc-ls` |
| **Hardware Thread** | Hardware | 각 Hardware Thread는 독립 Register Set / Program Counter / Thread ID Register 유지. Fetch/Decode 단계부터 명령어 스트림 분리 | `cat /proc/cpuinfo`의 `processor`, `core id`, `physical id` 비교 |
| **Logical CPU** | Kernel | Kernel은 각 Hardware Thread를 `cpu_info[]` 배열 원소로 관리. `per_cpu` 변수, Runqueue, IRQ Affinity 모두 Logical CPU 단위로 동작 | `/proc/cpuinfo`, `/sys/devices/system/cpu/cpuN/topology/`, `numactl -H` |
| **NUMA 토폴로지** | Hardware + Kernel | Physical Core는 NUMA 노드에 귀속. 노드 내 Core는 로컬 메모리에 빠르게 접근, 원격 노드 메모리는 QPI/UPI 인터커넥트 경유 → 레이턴시 수배 증가 | `numactl --hardware`, `numastat`, `cat /sys/devices/system/node/node*/cpulist` |

---

## 3. Physical Core의 구성

Physical Core는 실제 연산 자원의 집합이다. Hardware Thread는 이 자원을 사용하는 실행 상태 단위다.

| 구성 요소 | 역할 |
|-----------|------|
| ALU | 정수 연산 |
| FPU | 부동소수점 연산 |
| LSU | 메모리 접근 |
| Pipeline | 명령어 실행 |
| L1/L2 Cache | 고속 캐시 |

### 계층별 메커니즘 실체 — Physical Core 내부

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **ALU / FPU 실행 유닛** | Hardware | 현대 CPU는 ALU를 2~6개 이상 보유(Superscalar). Out-of-Order Execution으로 의존 관계 없는 명령어를 병렬 실행. SMT 시 두 Hardware Thread가 이 실행 유닛을 공유 | `perf stat -e instructions,cycles` (IPC 측정), `perf stat -e uops_executed.core` |
| **CPU Pipeline Stall** | Hardware | Load-Use Hazard: 메모리 Load 후 즉시 그 값을 사용하는 명령어가 있을 때 파이프라인 대기. Cache Miss 동반 시 수백 사이클 낭비 | `perf stat -e stalled-cycles-frontend,stalled-cycles-backend`, `toplev` (Top-Down 분석) |
| **Branch Misprediction** | Hardware | Branch Predictor가 분기 결과 예측 실패 시 파이프라인 Flush → 잘못 실행된 명령어 폐기 → 15~20 사이클 손실. SMT 시 두 Thread의 분기 패턴이 Branch Predictor 리소스를 경합 | `perf stat -e branch-misses,branch-instructions`, `perf report --sort=sym` |
| **L1/L2 Cache** | Hardware | L1 I-Cache(명령어, ~32KB) / L1 D-Cache(데이터, ~32KB) / L2 Cache(unified, ~256KB~1MB)는 Physical Core 전용. SMT의 두 Hardware Thread가 L1/L2 공유 → Capacity Miss 증가 | `perf stat -e L1-dcache-load-misses,L1-icache-load-misses,l2_rqsts.miss` |
| **Memory Bandwidth Saturation** | Hardware | 두 Hardware Thread가 동시에 메모리 집약적 작업 수행 시 L3 → DRAM 대역폭 포화 → 양쪽 Thread 모두 Memory Wait 증가 | `perf stat -e mem-loads,mem-stores`, `Intel MLC (Memory Latency Checker)`, `numastat -m` |
| **CPU Frequency Scaling** | Hardware + Kernel | P-state: 주파수/전압 조정으로 성능-전력 트레이드오프. C-state: 유휴 시 Core 절전. SMT Thread 중 하나가 활성이면 Core는 C0(활성) 유지, P-state는 부하에 따라 조정 | `turbostat`, `cpupower frequency-info`, `/sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq`, `perf stat -e power/energy-cores/` |

---

## 4. Hardware Thread의 구조

### 독립 유지 상태

각 Hardware Thread는 다음 상태를 독립적으로 유지한다.

| 구성 요소 | 역할 |
|-----------|------|
| Register Set | 스레드 실행 상태 |
| Program Counter | 다음 명령어 위치 |
| Thread Context | 실행 문맥 전체 |
| Pipeline State 일부 | 실행 상태 일부 |

### 공유 자원

동일 Physical Core 내부의 Hardware Thread들은 다음 자원을 공유한다.

| 자원 | 설명 |
|------|------|
| ALU / FPU | 연산 실행 유닛 |
| Pipeline | 명령 실행 구조 |
| Cache 일부 | L1/L2 캐시 일부 |
| Memory Bandwidth | 메모리 접근 대역폭 |

### 계층별 메커니즘 실체 — Hardware Thread 독립/공유 구조

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **독립 Register Set** | Hardware | 범용 레지스터(RAX~R15), SSE/AVX 레지스터, RFLAGS, RSP 등이 Hardware Thread별로 완전히 독립. Context Switch 없이 두 Thread가 동시에 다른 레지스터 값 유지 | `gdb register read`, `perf record --call-graph dwarf` (레지스터 기반 unwinding) |
| **공유 L1 D-Cache** | Hardware | 동일 Physical Core의 두 Hardware Thread가 L1 D-Cache를 공유. 서로 다른 데이터를 집중 접근하면 L1 Eviction 증가 → Capacity Miss 발생 | `perf stat -e L1-dcache-load-misses` (SMT on/off 비교), `perf c2c` |
| **Cache Line Thrashing** | Hardware | 두 Hardware Thread가 동일 Cache Line(64바이트) 내 다른 변수에 쓰기 접근 → False Sharing → MESI 프로토콜 Invalidation 폭증 → 성능 급락 | `perf c2c report`, `perf stat -e LLC-load-misses`, `valgrind --tool=cachegrind` |
| **공유 L2 Cache** | Hardware | L2 Cache(~256KB~1MB)도 Physical Core 내 두 Hardware Thread가 공유. 각 Thread의 Working Set이 L2를 초과하면 L3 접근 빈도 증가 | `perf stat -e l2_rqsts.miss`, `Intel VTune Memory Access 분석` |
| **ROB(Reorder Buffer) 분할** | Hardware | Out-of-Order 실행의 ROB를 SMT 시 두 Thread가 나눠 사용. 한 Thread의 Long-latency Load(Cache Miss)가 ROB를 점유하면 다른 Thread의 명령어 완료도 지연 | `perf stat -e cycle_activity.stalls_l3_miss`, `toplev --level 3` |
| **TLB 공유** | Hardware | ITLB / DTLB를 두 Hardware Thread가 공유. 한 Thread의 대용량 메모리 접근이 TLB를 오염시키면 다른 Thread의 TLB Miss 증가 | `perf stat -e dTLB-load-misses,iTLB-load-misses` (SMT 환경), `perf stat -e dtlb_load_misses.miss_causes_a_walk` |

---

## 5. SMT 구조

SMT(Simultaneous Multithreading)는 하나의 Physical Core에서 여러 Hardware Thread를 동시에 실행할 수 있게 하는 기술이다.

| 제조사 | 기술 이름 |
|--------|-----------|
| Intel | Hyper-Threading |
| AMD | SMT |

```
Physical Core
  ├── Hardware Thread 0 → Logical CPU 0
  └── Hardware Thread 1 → Logical CPU 1
```

운영체제는 각 Hardware Thread를 독립된 Logical CPU로 추상화하여 인식한다.

### Linux에서의 표현

```
processor : 0    → Logical CPU 0 (Hardware Thread 0)
processor : 1    → Logical CPU 1 (Hardware Thread 1)

physical id : 0  → 동일 CPU 패키지
core id     : 0  → 동일 Physical Core
```

`physical id`와 `core id`가 같은 두 `processor` 항목이 존재하면, 해당 Physical Core에 SMT가 적용된 것이다.

### 계층별 메커니즘 실체 — SMT 활성/비활성 경로

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **SMT 활성화 확인** | Hardware + Kernel | `thread siblings` 파일에서 동일 Physical Core의 Logical CPU 목록 확인. `cpu_topology_sibling_cpumask`로 Kernel이 관리 | `cat /sys/devices/system/cpu/cpu0/topology/thread_siblings_list`, `lscpu -e` |
| **SMT 런타임 비활성화** | Kernel | `/sys/devices/system/cpu/cpuN/online` 을 0으로 설정하면 해당 Logical CPU(sibling thread) 오프라인. BIOS 설정 없이 런타임 변경 가능 | `echo 0 > /sys/devices/system/cpu/cpu1/online`, `lscpu` 재확인 |
| **Kernel의 SMT 인식** | Kernel (CFS) | CFS Scheduler의 `sched_domain` 구조에서 SMT 레벨(`SD_SHARE_CPUCAPACITY`)을 인식. 같은 Physical Core 내 Logical CPU 간 부하 분산 시 성능 가중치 적용 | `cat /proc/sys/kernel/sched_domain/cpuN/domain0/name` (SMT 도메인 확인) |
| **IRQ Affinity와 SMT** | Kernel | NIC IRQ를 특정 Logical CPU에 고정 시 sibling Hardware Thread와 L1 Cache / TLB를 공유 → IRQ 처리와 애플리케이션 Thread 간 Cache 오염 주의 | `/proc/irq/N/smp_affinity_list`, `irqbalance`, `ethtool -l eth0` (NIC 큐 수 확인) |
| **Spectre / Meltdown 관련** | Hardware + Kernel | SMT 환경에서 동일 Physical Core의 두 Hardware Thread가 CPU 마이크로아키텍처 구조(BTB, L1 Cache)를 공유 → Side-Channel Attack 위험. 금융 보안 환경에서 SMT 비활성화 고려 | `cat /sys/devices/system/cpu/vulnerabilities/spectre_v2`, `mitigations=auto 커널 파라미터` |

---

## 6. Hardware Thread의 목적

Hardware Thread의 핵심 목적은 **Physical Core 활용률 향상**이다.

단일 실행 흐름만 존재할 때 다음 상황에서 실행 유닛이 유휴 상태가 된다.

- Cache Miss 후 메모리 대기
- Pipeline Stall 발생
- Branch Misprediction 후 대기

SMT 구조에서는 한 Hardware Thread가 대기하는 동안 다른 Hardware Thread가 유휴 실행 유닛을 사용할 수 있다.

```
Hardware Thread 0: Memory Wait (Stall)
Hardware Thread 1: ALU 연산 실행  ← 유휴 유닛 활용
```

### 계층별 메커니즘 실체 — 활용률 향상 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **Memory Stall 숨김** | Hardware | Thread 0이 L3 Cache Miss → DRAM 접근 대기(수백 사이클) 중 Thread 1의 명령어를 Fetch/Execute. 단일 Thread 대비 처리량 최대 30~40% 향상 가능 | `perf stat -e cycle_activity.stalls_mem_any` (Thread 0 스톨 측정), `toplev` |
| **IPC 향상 조건** | Hardware | SMT 효과는 Thread의 Memory Access 비율이 높을수록(I/O-bound) 효과적. ALU 집약적(CPU-bound) 작업은 실행 유닛 경합으로 오히려 IPC 감소 | `perf stat -e instructions,cycles` (IPC: instructions/cycles), Intel VTune Microarchitecture Exploration |
| **Pipeline Stage 분리** | Hardware | Fetch/Decode 단계에서 두 Thread의 명령어를 라운드로빈 또는 우선순위 기반으로 선택. Execute 단계에서 실행 유닛(ALU/FPU/LSU)에 동시 발행 | `perf stat -e uops_dispatched.thread`, `toplev --level 4` |
| **THP (Transparent HugePage) 효과** | Kernel + Hardware | 2MB HugePage 사용 시 TLB 엔트리 1개가 4KB Page 512개 커버. SMT 환경에서 공유 TLB 오염 감소 → 두 Hardware Thread 모두 TLB Hit 증가 | `/proc/meminfo`의 `AnonHugePages`, `cat /sys/kernel/mm/transparent_hugepage/enabled`, `perf stat -e dTLB-load-misses` 비교 |
| **HugePage TLB** | Hardware + Kernel | 1GB HugePage(PDPE1GB) 사용 시 TLB 1엔트리로 1GB 커버. 대용량 메모리 서버(결제 원장 In-Memory DB 등)에서 TLB Miss 근본 제거 | `grep -i hugepage /proc/meminfo`, `hugeadm --pool-list`, `perf stat -e dTLB-load-misses` |

---

## 7. SMT의 장점과 한계

### 장점

| 효과 | 설명 |
|------|------|
| Pipeline Utilization 증가 | 유휴 실행 슬롯 활용 |
| Throughput 증가 | 병렬 실행 처리량 향상 |
| Memory Wait 숨김 | Stall 시간 동안 다른 Thread 실행 |
| CPU Utilization 향상 | 하드웨어 자원 사용률 극대화 |

### 한계

SMT는 Physical Core 수를 증가시키지 않는다. 공유 자원 경쟁이 존재하기 때문에 성능이 2배가 되지 않는다.

```
1 Physical Core + SMT ≠ 2 Independent Physical Cores
```

### 계층별 메커니즘 실체 — 장점과 한계의 실체

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **Throughput 향상 실측** | Hardware | Memory-bound 워크로드(캐시 미스 많은 JSON 파싱, DB 스캔 등): SMT로 10~40% Throughput 향상 가능. CPU-bound(암호화, 행렬 연산): SMT 효과 거의 없거나 오히려 저하 | `perf bench mem memcpy` (SMT on/off 비교), `sysbench cpu`, `stress-ng --cpu` |
| **실행 유닛 경합 한계** | Hardware | 두 Hardware Thread 모두 ALU 집약적 작업 시 포트 경합 → 각 Thread IPC가 단일 Thread 대비 40~50% 수준으로 하락. 물리적 실행 유닛 수는 증가하지 않음 | `perf stat -e uops_executed.core,uops_issued.any` (포트 활용률 확인), `toplev -l3` |
| **JIT Compilation 간섭** | JVM + Hardware | JVM C2 JIT 컴파일러가 동일 Physical Core의 sibling Hardware Thread 위에서 실행 중인 경우 컴파일 작업이 애플리케이션 Thread의 L1/L2 Cache를 오염 → 순간적 응답 지연 | `-XX:+PrintCompilation`, `async-profiler -e cpu` (컴파일 스레드 비율 확인) |
| **GC Thread 간섭** | JVM + Hardware | JVM GC Worker Thread가 sibling Hardware Thread 위에서 동시 실행 시 애플리케이션 Thread와 Cache / Memory Bandwidth 경합 → GC 중 응답 지연 가중 | `jstat -gcutil`, `GC 로그 (-Xlog:gc*)`, `perf stat -e cache-misses` (GC 기간 비교) |
| **Memory Bandwidth 한계** | Hardware | 두 Thread 모두 메모리 집약적 작업 시 단일 Memory Controller 채널 대역폭 포화 → 양쪽 모두 지연. 단일 Thread 대비 처리량 개선 없는 경우도 발생 | `perf stat -e mem-loads,mem-stores`, `Intel MLC`, `numastat -m` |

---

## 8. Resource Contention (자원 경쟁)

동일 Physical Core의 Hardware Thread들이 공유 자원을 동시에 사용하면 성능 간섭이 발생한다.

| 경쟁 자원 | 영향 |
|-----------|------|
| ALU / FPU | 연산 처리 지연 |
| Cache | Cache Miss 증가 |
| Pipeline | IPC 감소 |
| Memory Bandwidth | Throughput 감소 |

### Noisy Neighbor 문제

한 Hardware Thread의 자원 집중 사용이 동일 Physical Core의 다른 Hardware Thread 성능을 저하시킨다.

```
Thread 0: 높은 ALU 사용률
  → 공유 실행 유닛 점유
  → Thread 1: 실행 지연 발생
```

### 계층별 메커니즘 실체 — Resource Contention

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **Noisy Neighbor — OS Thread 수준** | Kernel + Hardware | 다른 컨테이너의 OS Thread가 동일 Physical Core에 스케줄되면 L1/L2 Cache / TLB / Memory Bandwidth 경합. Kubernetes `cpu: limit` 설정만으로는 이 경합 제어 불가 | `perf stat -e cache-misses` (다른 컨테이너 부하 전후 비교), `taskset -p PID` (CPU 배치 확인) |
| **Noisy Neighbor — JVM 수준** | JVM + Hardware | JVM GC Thread / JIT Compiler Thread / Finalizer Thread가 동일 Physical Core의 sibling Hardware Thread 위에서 실행 시 애플리케이션 Thread Cache 오염 | `jstack` (GC/JIT 스레드 확인), `async-profiler`, `perf stat -e L1-dcache-load-misses` |
| **Cache Line Thrashing (False Sharing)** | Hardware | 동일 Physical Core의 두 Hardware Thread가 같은 Cache Line(64바이트) 내 인접 변수에 Write → MESI Invalidation → Coherency Traffic 폭증. Java `@Contended` 어노테이션, Go `padding` 구조체로 완화 | `perf c2c report`, `perf stat -e LLC-load-misses`, `-XX:+EnableContended` (Java) |
| **Memory Bandwidth Saturation** | Hardware | 결제 처리 Peak 시 두 Hardware Thread 모두 대용량 데이터 접근(결제 로그, 원장 조회) → 단일 Memory Controller 채널 포화 → 레이턴시 스파이크 | `perf stat -e mem-loads,mem-stores`, `Intel MLC`, `sar -r` |
| **IRQ 자원 경합** | Hardware → Kernel | NIC IRQ가 동일 Physical Core의 sibling Logical CPU에서 처리되면 애플리케이션 Thread의 Cache/TLB를 오염. IRQ를 격리된 Core에 고정하여 해결 | `/proc/irq/N/smp_affinity_list`, `irqbalance --policy=exact`, `mpstat -I ALL` |
| **CPU Frequency Scaling 간섭** | Hardware + Kernel | 한 Hardware Thread가 C-state 진입 시도해도 sibling Thread가 활성이면 Core는 C0 유지 → 전력 소비 증가, 예상치 못한 주파수 변동 | `turbostat --show Core,CPU,Avg_MHz,C1,C3,C6`, `cpupower idle-info` |

---

## 9. Kernel Scheduler와 Hardware Thread

Linux Scheduler는 Logical CPU 단위로 스레드를 배치한다. Hardware Thread는 직접 스케줄링 대상이 아니며, Logical CPU로 추상화된 형태로 Scheduler에 노출된다.

```
Application Thread
  ↓
Kernel Scheduler
  ↓
Logical CPU (Hardware Thread의 OS 추상화)
  ↓
Hardware Thread
  ↓
Physical Core
```

### Core-Aware 배치 전략

Linux Scheduler는 가능한 경우 서로 **다른 Physical Core**에 스레드를 우선 배치하여 SMT 자원 경쟁을 줄인다.

```
4 Core / 8 Thread 시스템:
  Physical Core 0 → cpu0, cpu1
  Physical Core 1 → cpu2, cpu3
  Physical Core 2 → cpu4, cpu5
  Physical Core 3 → cpu6, cpu7

Thread 2개 실행 시:
  권장: cpu0 + cpu2  (서로 다른 Physical Core)
  지양: cpu0 + cpu1  (동일 Physical Core의 SMT Thread)
```

### 계층별 메커니즘 실체 — Kernel Scheduler와 Hardware Thread

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **CFS sched_domain** | Kernel | CFS는 `sched_domain` 계층 구조로 SMT 레벨(`SD_SHARE_CPUCAPACITY`) → Physical Core 레벨(`SD_SHARE_PKG_RESOURCES`) → NUMA 레벨을 구분. 부하 분산 시 상위 도메인부터 우선 탐색 | `cat /proc/sys/kernel/sched_domain/cpu0/domain0/name`, `/proc/sys/kernel/sched_domain/cpu0/domain1/name` |
| **task_struct와 Logical CPU** | Kernel | `task_struct`의 `cpu` 필드: 현재 실행 중인 Logical CPU 번호. `cpus_allowed`: 허용된 Logical CPU 비트마스크(CPU Affinity). Scheduler가 Context Switch 시 갱신 | `/proc/PID/stat`의 38번 필드, `taskset -p PID`, `ps -o pid,psr,comm` |
| **Context Switch 비용** | Kernel + Hardware | Logical CPU 간 Context Switch 시 레지스터 저장/복원. 다른 Physical Core로 이동 시 추가로 L1/L2 Cache Cold Start, TLB Flush(PCID 미지원 시) 발생 | `vmstat cs`, `perf stat -e context-switches`, `perf stat -e dTLB-load-misses` (Core 이동 전후 비교) |
| **Runqueue per Logical CPU** | Kernel | 각 Logical CPU는 독립 CFS Runqueue 보유. SMT sibling Logical CPU 간 Load Balancing은 `sched_domain` SMT 레벨에서 처리. 불균형 시 Load Balance 트리거 | `/proc/schedstat`의 per-CPU 통계, `perf sched latency`, `sar -u ALL` |
| **IRQ 처리와 스케줄 간섭** | Kernel | NIC IRQ, Timer IRQ가 특정 Logical CPU에서 처리되면 해당 CPU의 Runqueue 스케줄 지연. `ksoftirqd` 실행 중 현재 CPU에서 스케줄되어야 할 Thread 지연 | `mpstat -I ALL`, `/proc/softirqs`, `perf trace -e irq:*` |
| **NUMA-Aware 스케줄** | Kernel | CFS NUMA Balancing: `task_struct`의 `numa_preferred_nid`로 Thread가 선호하는 NUMA 노드 추적. 원격 노드 메모리 접근 감지 시 Thread 또는 메모리를 이동 | `/proc/sys/kernel/numa_balancing`, `numastat -p PID`, `perf stat -e node-load-misses` |
| **Off-CPU Time 분석** | Kernel | Thread가 Runqueue에서 대기 중인 시간(Run Queue Latency)은 On-CPU 프로파일에 보이지 않음. SMT 포화 시 이 시간 급증 → Off-CPU 분석 필수 | `offcputime-bpfcc -p PID`, `runqlat-bpfcc`, `perf sched latency` |

---

## 10. Kubernetes에서의 Hardware Thread 영향

Kubernetes CPU Resource는 Logical CPU 기준으로 동작한다.

```yaml
resources:
  limits:
    cpu: "1"
```

동일한 `cpu: "1"` 할당이라도 실제 배치 위치에 따라 성능 차이가 발생한다.

| 배치 상황 | 특징 |
|-----------|------|
| 독립 Physical Core 할당 | 자원 경쟁 없음, 안정적 성능 |
| SMT Thread 공유 상태 | 공유 자원 경쟁 가능, 성능 가변 |

저지연이 요구되는 워크로드는 Kubernetes CPU Manager Policy(`static`) + Guaranteed QoS를 통해 전용 Physical Core 할당을 고려할 수 있다.

### 계층별 메커니즘 실체 — Kubernetes CPU 할당

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **cgroup CFS Bandwidth Control** | Kernel | `cpu.cfs_quota_us` / `cpu.cfs_period_us`로 컨테이너 CPU 사용량 제한. `cpu: "1"` = 100ms/100ms. Quota 소진 시 `nr_throttled` 증가 → 결제 처리 지연 | `/sys/fs/cgroup/cpu.stat`의 `nr_throttled`, `throttled_usec`, Prometheus `container_cpu_cfs_throttled_seconds_total` |
| **CPU Manager Policy: static** | Kubernetes + Kernel | Guaranteed QoS Pod에 정수 CPU 요청 시 Kubernetes CPU Manager가 `cpuset` cgroup으로 전용 Logical CPU 할당. 단, Logical CPU 1개 = Hardware Thread 1개 = SMT sibling 공유 | `cat /sys/fs/cgroup/cpuset/cpuset.cpus`, `kubectl describe node`의 `cpu manager policy`, `kubelet --cpu-manager-policy=static` |
| **SMT-Aware 할당 부재** | Kubernetes | 현재 Kubernetes CPU Manager는 SMT sibling을 쌍으로 할당하는 기능 없음. `cpu: "1"` 할당 시 단일 Hardware Thread만 할당될 수 있어 sibling은 다른 Pod에 할당 → Noisy Neighbor 위험 | `lscpu -e` (할당된 Logical CPU의 sibling 확인), `cat /sys/devices/system/cpu/cpuN/topology/thread_siblings_list` |
| **PSI (Pressure Stall Information)** | Kernel | CPU PSI `some` 수치 상승: 일부 Task가 Runnable이지만 CPU 획득 대기 중. SMT 포화 또는 cgroup Throttling이 원인일 수 있음 | `/proc/pressure/cpu`, Kubernetes PSI 기반 HPA, `cat /sys/fs/cgroup/cpu.pressure` (cgroup v2) |
| **OOM Killer와 메모리 배치** | Kernel | NUMA 환경에서 Pod의 메모리가 원격 NUMA 노드에 할당되면 해당 Pod의 Hardware Thread가 높은 메모리 레이턴시 경험. OOM 발생 시 `oom_score` 기반 종료 | `numastat -p PID`, `/proc/PID/numa_maps`, `dmesg | grep oom`, `numactl --membind=0` 고정 |

---

## 11. JVM 및 고성능 서버 설계 관점

### Thread Pool 크기

Logical CPU 수만 기준으로 Thread Pool을 설정하면 SMT 자원 경쟁이 심화될 수 있다. CPU 집약적(CPU-bound) 작업에서는 Physical Core 수를 Thread Pool 상한의 기준으로 고려하는 것이 유리한 경우가 있다.

### Netty / WebFlux Event Loop

Event Loop 수는 기본적으로 Logical CPU 수 기준으로 생성된다. 저지연 요구사항이 있는 경우 SMT 구조를 고려하여 조정할 수 있다.

### 금융 시스템 / DPDK 환경

극단적인 저지연이 요구되는 환경에서는 다음 전략을 사용한다.

| 전략 | 설명 |
|------|------|
| SMT 비활성화 | BIOS 수준에서 Hyper-Threading 끄기 |
| Core Pinning | 특정 Thread를 특정 Physical Core에 고정 |
| Isolated CPU | OS 스케줄링에서 해당 Core를 제외 (`isolcpus`) |

### 계층별 메커니즘 실체 — JVM 및 고성능 서버 설계

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **JVM Thread Pool과 SMT** | JVM + Hardware | CPU-bound 작업(암호화, 해시 계산)에서 Thread Pool = Logical CPU 수로 설정 시 SMT sibling 간 ALU/FPU 경합 → 각 Thread IPC 저하. Physical Core 수로 제한 시 경합 감소 | `perf stat -e instructions,cycles` (Thread 수 변화별 IPC 측정), `jstat -gcutil` |
| **JIT C2 컴파일러와 SMT** | JVM + Hardware | JVM C2 JIT 컴파일러 Thread가 활성 시 동일 Physical Core의 sibling Hardware Thread 위 애플리케이션 Thread 성능 저하. `-XX:CICompilerCount` 조정으로 컴파일 Thread 수 제어 | `-XX:+PrintCompilation`, `-XX:CICompilerCount=2`, `async-profiler`의 컴파일 스레드 CPU 비율 |
| **Safepoint와 Hardware Thread** | JVM + Kernel | JVM Safepoint(GC, Deoptimization) 진입 시 모든 JVM Thread가 안전 지점에서 정지. SMT sibling Thread가 애플리케이션 Thread의 Safepoint 도달을 지연시키는 경우 `time_to_safepoint` 증가 | `-XX:+PrintSafepointStatistics`, `-XX:+SafepointTimeout`, `async-profiler -e cpu` |
| **TLAB과 L1 Cache** | JVM + Hardware | 각 JVM Thread의 TLAB(Thread-Local Allocation Buffer)이 L1 D-Cache 내에서 연속 할당되면 Cache Hit 최대화. SMT 환경에서 두 Thread의 TLAB이 같은 Cache Line에 인접하면 False Sharing 발생 | `-XX:+PrintTLAB`, `perf c2c`, `jstat -gc` |
| **Netty Event Loop와 SMT** | App + Hardware | Netty의 NioEventLoop Thread 수 = `NettyRuntime.availableProcessors() * 2` = Logical CPU 수 * 2. SMT 환경에서 과다 생성 시 sibling 경합. 저지연 환경에서는 Physical Core 수로 제한 권장 | `netty.io/eventLoop.count` 설정, `perf stat -e context-switches` (Event Loop Thread 전환 측정) |
| **DPDK / Kernel Bypass** | Hardware + App | DPDK는 Logical CPU를 `isolcpus`로 OS에서 격리 후 DPDK PMD(Poll Mode Driver)가 독점 사용. SMT 비활성화 시 동일 Physical Core에서 인터럽트/OS 스케줄 완전 배제 → 결정적 레이턴시 달성 | `cat /proc/cmdline | grep isolcpus`, `numactl`, `dpdk-testpmd -l 2,4 --socket-mem 1024` |
| **Connection Pool Exhaustion** | App | Thread Pool 크기와 DB Connection Pool 크기 불일치 시 Thread가 Connection 대기 → Off-CPU 시간 증가. SMT 환경에서 Thread 수 증가가 실질 처리량 향상으로 이어지지 않는 경우 발생 | Prometheus `hikaricp.connections.pending`, `jstack`의 WAITING on pool, `offcputime-bpfcc` |

---

## 12. CPU Affinity 설정

특정 Thread를 특정 Logical CPU(Hardware Thread)에 고정하여 성능을 안정화한다.

| 목적 | 설명 |
|------|------|
| Cache Locality 유지 | 동일 Core 반복 실행으로 Cache Hit 증가 |
| SMT 간섭 감소 | 다른 Thread와 Core 공유 방지 |
| Context Switch 감소 | CPU 이동 최소화 |
| Latency 안정화 | 실행 위치 고정으로 지연 분산 감소 |

```bash
# 프로세스를 cpu0, cpu2에 고정 (서로 다른 Physical Core)
taskset -c 0,2 java -jar app.jar

# 현재 프로세스의 Affinity 확인
taskset -p <pid>
```

### 계층별 메커니즘 실체 — CPU Affinity 설정

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **taskset / sched_setaffinity** | Kernel | `sched_setaffinity()` 시스템 콜 → `task_struct`의 `cpus_allowed` 비트마스크 갱신. CFS가 이후 Runqueue 배치 시 허용 Logical CPU 내에서만 스케줄 | `taskset -c 0,2 -p PID`, `/proc/PID/status`의 `Cpus_allowed_list`, `strace -e sched_setaffinity` |
| **Cache Locality 효과** | Hardware | 동일 Logical CPU에 Thread 고정 시 L1/L2 Cache Warm 상태 유지. Context Switch가 발생해도 동일 Physical Core 내 sibling이면 L2 Cache 공유로 재warming 비용 감소 | `perf stat -e L1-dcache-load-misses` (Affinity 전후 비교), `perf stat -e cache-misses` |
| **NUMA Affinity** | Kernel + Hardware | `numactl --cpunodebind=0 --membind=0`으로 Thread와 메모리를 동일 NUMA 노드에 고정. 원격 메모리 접근 원천 차단 → 레이턴시 분산 감소 | `numastat -p PID`, `perf stat -e node-load-misses`, `numactl --hardware` |
| **isolcpus + nohz_full** | Kernel | 커널 부팅 파라미터 `isolcpus=2,4` 설정 시 해당 Logical CPU는 일반 스케줄에서 제외. `nohz_full=2,4`로 Timer Tick도 제거 → 완전 격리된 실행 환경. 금융 결제 엔진 저지연 처리에 적용 | `cat /proc/cmdline`, `cat /sys/devices/system/cpu/isolated`, `cyclictest -c 2` (레이턴시 측정) |
| **JVM Thread Affinity** | JVM + Kernel | JVM 기본 Thread Affinity 없음. `JNA` / `java-thread-affinity` 라이브러리로 JVM Thread를 특정 Logical CPU에 고정 가능. Critical Section Thread를 격리 Core에 고정 시 GC/JIT 간섭 차단 | `java-thread-affinity` 라이브러리, `taskset` + JVM `pid`, `perf stat` (Affinity 적용 Thread 측정) |
| **IRQ Affinity 분리** | Kernel | 애플리케이션 Thread가 고정된 Logical CPU에서 IRQ 처리를 제외. NIC IRQ를 전용 Core에 할당하고 RPS/RFS로 처리 Core 분산. 애플리케이션 Core의 Cache 오염 방지 | `/proc/irq/N/smp_affinity_list`, `irqbalance --banirq=N`, `ethtool -X eth0 equal N` |

---

## 13. 전체 계층 구조

```
Application Thread (JVM Thread / Go Goroutine / OS Thread)
  ↓
Kernel Scheduler (CFS / sched_domain / Runqueue)
  ↓
Logical CPU             ← OS 스케줄링 단위 (cpu_data, per_cpu)
  ↓
Hardware Thread         ← 독립 실행 상태 유지 (Register Set, PC, Thread Context)
  ↓
Physical Core           ← 실제 하드웨어 자원 (ALU/FPU/LSU/Cache/Pipeline)
  ↓
NUMA Node               ← 메모리 노드 귀속 (로컬 DRAM, QPI/UPI 인터커넥트)
  ↓
Memory Controller / DRAM
```

### 계층별 메커니즘 실체 — 전체 경로

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **Application → OS Thread** | OS / JVM / Go Runtime | JVM Platform Thread: `clone()` → `task_struct`. Go Goroutine: `runtime.newproc()` → G 구조체 → M(OS Thread). 실행 흐름이 어떤 형태든 최종적으로 Logical CPU에 매핑 | `ps -eLf`, `/proc/PID/task/`, `pstree -p PID` |
| **OS Thread → Logical CPU** | Kernel (CFS) | CFS가 `task_struct`를 Runqueue에서 꺼내 Logical CPU에 배치. Context Switch 발생 시 레지스터 저장/복원, `cpu` 필드 갱신 | `/proc/PID/stat`의 38번 필드, `perf sched switch`, `vmstat cs` |
| **Logical CPU → Hardware Thread** | Hardware | Logical CPU 번호가 곧 Hardware Thread 번호. OS의 추상화가 실제 하드웨어 Thread ID에 1:1 매핑 | `cat /proc/cpuinfo`의 `processor`, `apicid` 필드 |
| **Hardware Thread → Physical Core** | Hardware | 동일 `core id`를 가진 두 Hardware Thread가 ALU/FPU/Cache/Pipeline을 공유하며 동시 실행 | `cat /sys/devices/system/cpu/cpuN/topology/core_id`, `lscpu -e` |
| **Physical Core → NUMA Node** | Hardware + Kernel | Physical Core는 특정 NUMA 노드에 귀속. 로컬 메모리 접근: ~60ns. 원격 노드 접근: ~120ns+ | `numactl --hardware`, `/sys/devices/system/node/node0/cpulist`, `perf stat -e node-load-misses` |

---

## 14. 구성 요소 요약

| 구성 요소 | 계층 | 역할 |
|-----------|------|------|
| Physical Core | Hardware Resource | 실제 연산 자원 집합 (ALU/FPU/LSU/Cache/Pipeline) |
| Hardware Thread | Hardware Execution Context | 독립 Register Set / PC / Thread Context 유지 단위 |
| Logical CPU | OS Logical Layer | 커널 스케줄링 단위 (cpu_data, per_cpu, Runqueue) |
| SMT | CPU Architecture | Physical Core 내 다중 Hardware Thread 동시 실행 기술 |
| Scheduler | Kernel Layer | CFS / sched_domain 기반 Logical CPU 스케줄 배치 관리 |
| NUMA Node | Hardware Topology | Physical Core와 로컬 메모리의 물리적 귀속 단위 |

---

## 15. 성능 최적화 핵심 요소

```
Physical Core 자원 효율적 사용
+ SMT 구조 인식 (자원 공유 / 경합 분석)
+ Hardware Thread 간 Resource Contention 최소화
+ Cache Locality 유지 (L1/L2/TLB)
+ NUMA Affinity 적용 (로컬 메모리 접근 강제)
+ Scheduler Core-Aware 배치 활용 (sched_domain)
+ IRQ / SoftIRQ 격리 (전용 Core 할당)
= High Throughput + Stable Latency
```

### 계층별 메커니즘 실체 — 최적화 전략

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **SMT on/off 선택 기준** | Hardware + Ops | Memory-bound 워크로드(캐시 미스 많은 서비스): SMT 활성화 권장. CPU-bound + 저지연 요구(결제 엔진, HFT): SMT 비활성화 + Core Pinning 권장 | `perf stat -e instructions,cycles,cache-misses` (SMT on/off 비교), `cyclictest` (레이턴시 측정) |
| **THP 활성화** | Kernel + Hardware | 2MB Huge Page로 TLB 커버리지 512배 향상 → SMT 환경에서 공유 TLB 경합 감소 → 두 Hardware Thread 모두 TLB Hit 증가 | `echo always > /sys/kernel/mm/transparent_hugepage/enabled`, `perf stat -e dTLB-load-misses` 전후 비교 |
| **NUMA Balancing 튜닝** | Kernel | `kernel.numa_balancing=1` (자동 NUMA 밸런싱). 특정 워크로드에서 Thread 이동 오버헤드가 더 클 수 있어 비활성화 후 수동 `numactl` 바인딩이 유리한 경우도 있음 | `/proc/sys/kernel/numa_balancing`, `numastat -z`, `perf stat -e node-load-misses` 비교 |
| **Dirty Page Writeback 격리** | Kernel | 고성능 서버에서 Kernel `pdflush`/`writeback` I/O가 특정 Core의 Cache를 오염시키지 않도록 `isolcpus`로 격리 Core와 I/O 처리 Core 분리 | `/proc/vmstat`의 `nr_dirty,nr_writeback`, `iotop`, `blktrace` |
| **PSI 기반 조기 감지** | Kernel | SMT 포화 또는 cgroup Throttling 초기에 PSI `cpu.some` 수치 상승. 알람 설정으로 장애 전조 포착 가능 | `/proc/pressure/cpu`, `cat /sys/fs/cgroup/cpu.pressure`, Kubernetes PSI 기반 자원 부족 감지 |

이 원칙은 다음 환경의 설계와 직접 연결된다.

- Kubernetes CPU Resource 및 QoS 설계 (CPU Manager Policy, cpuset, PSI)
- JVM Thread Pool 크기 결정 (CPU-bound 기준 Physical Core 수 참조)
- Netty / WebFlux Event Loop 튜닝 (SMT 인식 Thread 수 조정)
- 금융 시스템 저지연 처리 (SMT 비활성화, Core Pinning, isolcpus, nohz_full)
- 고성능 네트워크 처리 (DPDK, Kernel Bypass, IRQ Affinity 분리, RPS/RFS)

---

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*