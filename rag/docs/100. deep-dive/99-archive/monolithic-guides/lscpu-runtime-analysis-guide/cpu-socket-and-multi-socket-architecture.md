# CPU Socket과 Multi-Socket CPU 구조 (E2E 분석 적용됨)

---

## 1. CPU Socket이란?

CPU Socket은 메인보드에서 CPU 패키지를 장착하는 물리적 인터페이스다. Multi-Socket 시스템에서는 여러 개의 CPU 패키지를 하나의 시스템에 동시에 장착할 수 있다.

```
Socket 0 → CPU 패키지 0
Socket 1 → CPU 패키지 1
```

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| 물리 인터페이스 | Hardware | LGA / BGA 핀 배열, Socket 전기 신호 규격 (PCIe Gen 5 / UPI / Infinity Fabric), 전원 공급 VRM 위상 수 | `dmidecode -t processor`, `lshw -class processor` |
| CPU 패키지 인식 | Hardware → Kernel | BIOS/UEFI ACPI SRAT 테이블 파싱, CPU 토폴로지 열거, CPUID 명령어 (EAX=0x0B: Extended Topology) | `dmidecode -t 4`, `/sys/devices/system/cpu/cpu*/topology/` |
| OS 등록 | Kernel | `struct cpuinfo_x86` 로 각 Physical Package 등록, `cpu_possible_mask` / `cpu_online_mask` 설정 | `cat /proc/cpuinfo | grep "physical id"`, `lscpu` |
| 인터커넥트 | Hardware | Intel UPI (Ultra Path Interconnect) / AMD Infinity Fabric / QPI: Socket 간 Cache Coherency 메시지 전달, MESIF / MOESI 프로토콜 | `ipmitool sdr`, `perf stat -e offcore_*` |

---

## 2. 전체 계층 구조

```
Socket (Physical Package)
  ↓  [CPUID / ACPI SRAT]
Physical Core
  ↓  [SMT — Hyper-Threading]
Hardware Thread
  ↓  [cpu_possible_mask / cpu_online_mask]
Logical CPU
  ↓  [CFS RunQueue / task_struct]
Kernel Scheduler
  ↓  [clone() / pthread_create()]
Application Thread
```

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| Socket → Physical Core | Hardware | Die 내부 Mesh / Ring Bus 토폴로지, Core 간 L3 Cache 공유 도메인, Intel LLC (Last Level Cache) 슬라이스 분배 | `lstopo`, `hwloc-info`, `perf stat -e LLC-load-misses` |
| Physical Core → HW Thread | Hardware | SMT (Simultaneous Multi-Threading): 단일 Core의 파이프라인 자원(ROB, RS, FPU)을 두 Thread가 공유, Front-end 분리 (별도 Register File / PC) | `cat /sys/devices/system/cpu/smt/active`, `perf stat -e cpu-cycles` |
| HW Thread → Logical CPU | Kernel | `cpu_to_node()` 매핑, `per_cpu` 자료구조, `smp_processor_id()` | `/sys/devices/system/cpu/cpu*/topology/core_id` |
| Logical CPU → Scheduler | Kernel | CFS `rq` (RunQueue) per-CPU 구조체, `task_struct.se.vruntime`, Red-Black Tree 삽입/삭제 | `/proc/schedstat`, `perf sched latency` |
| Scheduler → App Thread | Kernel / App | `schedule()` → `context_switch()` → `switch_to()` 매크로, `task_struct` 교체, TLB Flush (PCID 최적화 시 생략 가능) | `vmstat` cs 항목, `pidstat -w`, `perf stat -e context-switches` |

---

## 3. Socket 내부 구성 요소

각 Socket은 다음 자원을 독립적으로 보유한다.

| 구성 요소 | 설명 | 메커니즘 실체 |
|-----------|------|--------------|
| Physical Core | 실제 연산 유닛 | Out-of-Order Execution Engine, ROB (Re-Order Buffer), Reservation Station, Branch Predictor (TAGE / BHB) |
| L1 / L2 Cache | Core 전용 캐시 | L1I/D (32~64 KB), L2 (256 KB~1 MB) Write-Back 정책, VIPT 구조 |
| L3 Cache (LLC) | Socket 공유 캐시 | Inclusive / Non-Inclusive 정책, Cache Slice 분산 (Intel: 해시 기반), MESIF 상태 머신 |
| Memory Controller | 로컬 메모리 제어기 | IMC (Integrated Memory Controller), DDR5 채널 병렬 처리, RAS / CAS 타이밍 제어 |
| NUMA Node | 로컬 메모리 영역 | ACPI SRAT 테이블로 정의, `pg_data_t` 구조체 (Linux Memory Zone 관리) |
| PCIe Lane | I/O 연결 인터페이스 | Root Complex per Socket, NIC / NVMe / GPU NUMA 배치 영향, P2P DMA 가능 여부 |
| UPI / Infinity Fabric | Socket 간 인터커넥트 | Cache Coherency Directory, Snoop 메시지 라우팅, 대역폭 및 지연 제한 요소 |

---

## 4. Socket 내부 구조 예시

```
Socket 0
  ├── Core 0
  │   ├── L1I Cache (32KB) / L1D Cache (48KB)
  │   ├── L2 Cache (1MB, Unified)
  │   ├── Hardware Thread 0 → cpu0  [Register File, PC, ROB 독립]
  │   └── Hardware Thread 1 → cpu1  [Register File, PC, ROB 독립]
  ├── Core 1
  │   ├── Hardware Thread 0 → cpu2
  │   └── Hardware Thread 1 → cpu3
  ├── ...
  ├── L3 Cache (LLC, 공유, 슬라이스 분산)
  ├── Memory Controller → DDR5 Channel 0 / 1
  └── UPI Link → Socket 1
```

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| Core 내 파이프라인 | Hardware | Branch Prediction → Fetch → Decode → Rename → Dispatch → Execute → Retire, CPU Pipeline Stall (데이터 의존성 / 캐시 미스 / Branch Misprediction 시 발생) | `perf stat -e branch-misses,branch-instructions`, `toplev.py` (Top-down Microarchitecture Analysis) |
| SMT 자원 경합 | Hardware | FPU / L1D / L2 공유로 인한 SMT 형제 Thread 간 노이즈 발생, Hyper-Threading 비활성화 고려 상황 (저지연 워크로드) | `perf stat --per-core`, `/sys/devices/system/cpu/smt/control` |
| L3 Cache 슬라이스 | Hardware + Kernel | Intel: 물리 주소 하위 비트 해싱으로 슬라이스 결정, NUMA Remote 접근 시 LLC 미스 후 메모리 패치, Cache Line Thrashing (다수 Thread가 동일 Cache Line 경합) | `perf c2c` (Cache-to-Cache 전송 분석), `perf stat -e LLC-loads,LLC-load-misses` |

---

## 5. 운영체제의 CPU 인식 방식

운영체제는 여러 Socket에 걸친 CPU 자원을 하나의 Logical CPU 집합으로 통합하여 관리한다.

### Logical CPU 계산

```
Total Logical CPUs = Σ (Physical Cores per Socket × Hardware Threads per Core)
```

### 계산 예시

| Socket | Physical Core 수 | Threads per Core | Logical CPU 수 |
|--------|-----------------|-----------------|----------------|
| Socket 0 | 4 | 2 | 8 |
| Socket 1 | 8 | 2 | 16 |
| **합계** | **12** | — | **24** |

Linux에서는 이를 `cpu0` ~ `cpu23`으로 나열하며, Socket 경계 없이 단일 풀로 스케줄링한다.

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| CPUID 열거 | Hardware → Kernel | CPUID leaf 0x0B (Extended Topology Enumeration), SMT Level / Core Level / Package Level 구분, `cpuinfo_x86.phys_proc_id` / `cpu_core_id` 저장 | `cpuid -1` (per-CPU 상세), `cat /proc/cpuinfo \| grep -E "physical id\|core id\|processor"` |
| ACPI 토폴로지 파싱 | BIOS → Kernel | MADT (Multiple APIC Description Table), SRAT (System Resource Affinity Table), APIC ID → NUMA Node 매핑 | `acpidump -n SRAT \| acpixtract`, `dmesg \| grep NUMA` |
| CPU Online/Offline | Kernel | `cpu_online_mask` 비트마스크, `__cpu_up()` → `cpu_notify()` 체인, CPU Hotplug 이벤트 시 `task_struct` 재배치 | `echo 0 > /sys/devices/system/cpu/cpu3/online` (CPU Offline), `nproc --all` |
| Scheduler Topology | Kernel | `sched_domain` 계층 (SMT → MC → NUMA), Load Balancing 주기 및 범위를 도메인별로 다르게 설정, `sched_group`으로 Core 그룹화 | `/proc/sys/kernel/sched_domain/`, `schedtool -r <pid>` |

---

## 6. NUMA (Non-Uniform Memory Access)

Multi-Socket 시스템은 일반적으로 NUMA 구조를 사용한다. 각 Socket은 자체 로컬 메모리를 가지며, 다른 Socket의 메모리 접근은 인터커넥트를 경유하기 때문에 지연이 증가한다.

```
Socket 0 ↔ Local Memory 0    (낮은 지연: ~80ns)
Socket 1 ↔ Local Memory 1    (낮은 지연: ~80ns)

Socket 0 Core → UPI/Infinity Fabric → Memory 1   (높은 지연: ~130~160ns)
```

### 메모리 접근 유형 비교

| 접근 유형 | 경로 | 지연 | 대역폭 |
|-----------|------|------|--------|
| Local Memory 접근 | Core → 동일 Socket IMC → DRAM | 낮음 (~80ns) | 최대 (단일 IMC 대역폭) |
| Remote Memory 접근 | Core → UPI/Fabric → 타 Socket IMC → DRAM | 높음 (~130~160ns) | 제한 (인터커넥트 병목) |
| L3 Cache Hit | Core → LLC Slice | 매우 낮음 (~40 cycles) | — |

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| NUMA 메모리 할당 | Kernel | `alloc_pages_node()`, `numa_node_id()`, `pg_data_t` per-node 구조체, Zone (DMA / Normal / Movable) 별 Free List 관리, 기본 정책: First-Touch (최초 접근 CPU의 로컬 Node) | `numastat -p <pid>`, `/proc/PID/numa_maps`, `numactl --hardware` |
| Remote Memory 접근 경로 | Hardware | Cache Miss → LLC Miss → Memory Controller Snoop → UPI Packet 전송 → Remote Socket IMC 요청 → DRAM Row 열기 → 데이터 반환, Memory Bandwidth Saturation 시 지연 급증 | `perf stat -e offcore_response.demand_data_rd.l3_miss_remote_dram`, `numactl --hardware` (distances 항목) |
| NUMA Balancing | Kernel | `task_numa_fault()`: Page Fault를 통한 접근 패턴 감지, `numa_migrate_pages()`: Hot Page를 접근 Node로 이동, `sched_numa_balancing` 활성화 시 주기적 Page Unmapping으로 재감지 | `/proc/sys/kernel/numa_balancing`, `numastat` (numa_hit / numa_miss), `perf stat -e numa:*` |
| THP (Transparent HugePage) NUMA 영향 | Kernel | 2MB HugePage 사용 시 TLB Miss 감소 효과, NUMA Remote 접근 시 2MB 단위 전체 Fetch로 대역폭 낭비 가능, `khugepaged` 데몬이 4KB → 2MB 승격 수행 | `/sys/kernel/mm/transparent_hugepage/enabled`, `/proc/meminfo` (AnonHugePages), `perf stat -e dTLB-load-misses` |

### NUMA가 성능에 미치는 영향

| 영향 | 설명 | 관련 메커니즘 |
|------|------|--------------|
| Memory Latency 증가 | Remote 접근 시 응답 지연 (+50~100%) | UPI / Infinity Fabric 홉 지연 |
| Cache Coherency 비용 증가 | Socket 간 캐시 동기화 오버헤드 | MESIF / MOESI 프로토콜 Snoop 트래픽 |
| Memory Bandwidth Saturation | Interconnect 대역폭 병목으로 전체 처리량 저하 | UPI 대역폭 한계 (약 40~80 GB/s per link) |
| Tail Latency 증가 | 응답 시간 불균형 발생 | Remote Snoop 대기 시간 비결정적 |
| False Sharing | 다른 Socket의 Thread가 동일 Cache Line 접근 시 Ping-Pong 발생 | Cache Line Thrashing, `perf c2c` |

---

## 7. 비대칭 Socket 구성의 문제

Socket 간 Core 수 또는 클럭 특성이 다른 경우 추가적인 성능 문제가 발생할 수 있다.

### 비대칭 Core 구성 예시

```
Socket 0 → 4 Core (높은 Clock: 3.8GHz, P-state P0)
Socket 1 → 8 Core (낮은 Clock: 2.5GHz, P-state P2)
```

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| Scheduler Load Imbalance | Kernel | CFS `load_balance()`: Socket 간 `sched_group` 부하 비교, `capacity_of(cpu)` 차이로 인한 잘못된 이동 결정, `imbalance_pct` 임계값 | `/proc/schedstat` (load_balance 실패 횟수), `perf sched`, `tuna --show-threads` |
| CPU Frequency Scaling | Hardware + Kernel | P-state (성능 상태): DVFS (Dynamic Voltage Frequency Scaling), `intel_pstate` / `acpi-cpufreq` 드라이버, C-state (유휴 상태): C0(활성) → C1(Halt) → C6(Deep Sleep), C-state 전환 시 Wakeup Latency 발생 (C6: ~수백 μs) | `cpupower frequency-info`, `turbostat`, `/sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq` |
| NUMA Imbalance | Kernel | `task_numa_fault()` 기반 마이그레이션이 비대칭 Socket에서 잘못된 방향으로 동작 가능, `numa_faults` 카운터 누적 분석 | `numastat`, `/proc/PID/numa_maps`, `perf stat -e node-load-misses` |
| Cache Locality 저하 | Hardware | Thread 이동 시 L1/L2 Cold Cache, Socket 이동 시 LLC도 Cold, Remote Memory 접근 비율 급증 | `perf stat -e cache-misses,LLC-load-misses`, `perf c2c` |
| 실행 속도 불일치 | Hardware + Kernel | 동일 워크로드 배치 위치에 따라 처리 시간 2배 이상 차이, Latency Variance (p99 악화) | `turbostat` (per-CPU frequency 실측), `perf stat --per-core -e cpu-clock` |

---

## 8. Linux Scheduler의 NUMA 인식 스케줄링

현대 Linux Scheduler는 NUMA 구조를 인식하여 다음 목표를 기준으로 스레드를 배치한다.

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| CFS vruntime 관리 | Kernel | `sched_entity.vruntime` (가상 실행 시간): 적게 실행된 Task 우선 선택, Red-Black Tree에 vruntime 기준 삽입, `min_vruntime` 기준 상대적 공정성 보장 | `/proc/PID/sched` (vruntime 항목), `perf sched timehist` |
| Context Switch | Kernel | `schedule()` → `pick_next_task_fair()` → `context_switch()` → `switch_mm()` (Address Space 교체) → `switch_to()` (Register 저장/복원), TLB Flush (PCID 없을 시 전체 Flush) | `vmstat` cs 항목, `pidstat -w 1`, `perf stat -e context-switches` |
| NUMA-aware 스케줄링 | Kernel | `sched_domain` SD_NUMA 플래그, `task_numa_fault()` → `task_numa_placement()`: 접근 빈도가 높은 Node에 Thread 이동 결정, `preferred_node` 설정 | `/proc/sys/kernel/numa_balancing`, `sched_setaffinity()`, `numastat -p <pid>` |
| Load Balancing 주기 | Kernel | SMT 도메인: ~1ms, Core 도메인: ~4ms, NUMA 도메인: ~32ms 주기로 `load_balance()` 호출, Cross-Socket 이동은 비용이 크므로 임계값 높게 설정 | `/proc/sys/kernel/sched_migration_cost_ns`, `/sys/kernel/debug/sched/` |
| Wakeup 최적화 | Kernel | `select_task_rq_fair()`: Wakeup 시 이전 실행 CPU(Last CPU) 또는 데이터 로컬 CPU 선택, `wake_affine` 로직: 캐시 친화성 vs 부하 분산 트레이드오프 | `perf sched record + report`, `ftrace: sched_wakeup 이벤트` |

| 스케줄링 목표 | 설명 | 관련 커널 메커니즘 |
|--------------|------|------------------|
| Local Memory 우선 사용 | NUMA Latency 감소 | `task_numa_placement()`, `preferred_node` |
| CPU Locality 유지 | Cache 효율 증가 | `wake_affine`, Last-CPU 선택 |
| Cross-Socket 이동 최소화 | Interconnect 비용 감소 | SD_NUMA 도메인 임계값, `sched_migration_cost_ns` |

---

## 9. NUMA 제어 도구

### numactl

특정 NUMA Node에 CPU와 메모리를 함께 고정하여 Remote 접근을 방지한다.

```bash
# cpu0이 속한 NUMA Node 0에서 실행, 메모리도 Node 0만 사용
numactl --cpunodebind=0 --membind=0 java -jar app.jar

# Interleave 정책: 모든 Node에 메모리를 분산 (대역폭 최대화)
numactl --interleave=all java -jar app.jar
```

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| `--membind` 적용 | Kernel | `set_mempolicy(MPOL_BIND, ...)` 시스템 콜 → `mmap` / `brk` 시 해당 Node에서만 Page 할당, 요청 Node 메모리 부족 시 OOM 발생 (다른 Node로 Fallback 없음) | `numastat -p <pid>`, `/proc/PID/numa_maps` |
| `--interleave` 적용 | Kernel | `set_mempolicy(MPOL_INTERLEAVE, ...)`: 페이지 단위 Round-Robin 할당, 다수 Socket의 메모리 대역폭을 골고루 활용, 특정 Node 집중 방지 | `numastat` (interleave_hit 항목) |
| CPU Affinity | Kernel | `sched_setaffinity()` 시스템 콜 → `task_struct.cpus_ptr` 마스크 설정, `sched_getaffinity()`로 현재 마스크 확인 | `taskset -p <pid>`, `/proc/PID/status` (Cpus_allowed) |

### NUMA 구조 확인

```bash
numactl --hardware       # NUMA Node 구성, 메모리 크기, Node 간 distance 행렬 확인
numactl --show           # 현재 프로세스의 NUMA 정책 확인
numastat                 # Node별 메모리 할당 통계 (hit/miss)
```

---

## 10. SRE 관점 주요 확인 명령어

### CPU 전체 구조 확인

```bash
lscpu
```

주요 출력 항목:

| 항목 | 의미 | 연관 메커니즘 |
|------|------|--------------|
| `Socket(s)` | 물리 CPU 패키지 수 | UPI/Fabric 토폴로지 |
| `NUMA node(s)` | NUMA Node 수 | `pg_data_t`, SRAT 테이블 |
| `Core(s) per socket` | Socket당 Physical Core 수 | `sched_domain` MC 레벨 |
| `Thread(s) per core` | Core당 Hardware Thread 수 | SMT, `sched_domain` SMT 레벨 |
| `CPU(s)` | 총 Logical CPU 수 | `cpu_online_mask` |
| `CPU MHz` / `CPU max MHz` | 현재/최대 동작 클럭 | P-state, Turbo Boost |
| `NUMA node0/1 CPU(s)` | Node별 Logical CPU 목록 | `cpumask_of_node()` |

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| CPU 토폴로지 상세 | Kernel / Hardware | `lstopo` (hwloc): Socket-NUMA-L3-Core-Thread 시각화, `/sys/devices/system/cpu/cpu*/topology/` 하위 파일 (physical_package_id, core_id, core_cpus_list) | `lstopo --of ascii`, `hwloc-calc --intersect NUMANode:0 core:all` |
| 동적 주파수 관찰 | Hardware + Kernel | `turbostat`: per-Core Busy%, 실제 MHz, C-state 분포 실시간 확인, CPU Frequency Scaling 드라이버 상태 | `turbostat --interval 1 --quiet`, `cpupower monitor` |
| IRQ 분산 확인 | Hardware + Kernel | Multi-Socket에서 IRQ가 한 Socket에 집중되면 SoftIRQ CPU 편중 발생, `/proc/interrupts`로 NIC IRQ 분산 상태 확인, `irqbalance` 데몬 동작 여부 | `/proc/interrupts`, `mpstat -I ALL 1`, `irqbalance --debug` |
| Off-CPU 분석 | Kernel | Thread가 CPU를 사용하지 않는 시간 (I/O 대기, Lock 경합, Sleep), `task_struct.state` TASK_INTERRUPTIBLE / TASK_UNINTERRUPTIBLE 전환 추적 | `offcputime-bpfcc -p <pid>`, `perf record -e sched:sched_switch`, `bpftrace` |

### 상세 토폴로지 확인

```bash
cat /proc/cpuinfo              # 각 Logical CPU의 physical id / core id / apicid 확인
numactl --hardware             # NUMA Node별 CPU 목록, 메모리 크기, distance 행렬
taskset -p <pid>               # 특정 프로세스의 CPU Affinity (cpus_allowed) 확인
lstopo --of ascii              # 전체 하드웨어 토폴로지 ASCII 시각화
cat /proc/PID/numa_maps        # 프로세스 메모리 매핑의 NUMA Node 분포
turbostat --interval 1         # Per-Core 주파수, C-state 분포 실시간 확인
perf stat -e cache-misses,LLC-load-misses,context-switches -p <pid>  # 핵심 성능 카운터
```

---

## 11. Kubernetes와 Multi-Socket

Kubernetes는 기본적으로 Logical CPU 기준으로 자원을 할당하며, Socket 경계를 인식하지 않는다.

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| cgroup CPU 제한 | Kernel | cgroup v2 `cpu.max` (Quota/Period), CFS Bandwidth Control: `cpu_quota_us` / `cpu_period_us`, Quota 소진 시 `cfs_bandwidth.throttled_cfs_rq`에 배치 → Throttling 발생 | `/sys/fs/cgroup/cpu.stat` (throttled_time), `kubectl top`, `cadvisor` |
| CPU Manager | Kubelet + Kernel | Static Policy: `cpuset` cgroup으로 특정 Physical Core 전용 할당, `sched_setaffinity()` 적용, SMT 형제 Thread 함께 할당 (또는 격리 옵션) | `/sys/fs/cgroup/<pod>/cpuset.cpus`, `kubectl describe node` (allocatable) |
| Guaranteed QoS | Kubelet | `requests == limits` 조건 만족 시 Guaranteed, CPU Manager Static Policy 적용 가능, BestEffort / Burstable은 shared pool에서 실행 | `kubectl get pod -o yaml \| grep qosClass`, `/proc/<pid>/cgroup` |
| CPU Throttling 탐지 | Kernel + 관찰 | `nr_throttled` 카운터 증가: CPU 집중 처리 중 Quota 소진 → 다음 Period까지 Sleep, Tail Latency 급증 원인 | `/sys/fs/cgroup/cpu.stat` (throttled_usec), Prometheus `container_cpu_cfs_throttled_seconds_total` |

### CPU Manager를 통한 전용 Core 할당

고성능 워크로드에서 특정 Physical Core를 전용 할당하려면 다음 조건이 필요하다.

```yaml
# Pod 설정
resources:
  requests:
    cpu: "4"
  limits:
    cpu: "4"   # requests == limits → Guaranteed QoS
```

```
Guaranteed QoS + CPU Manager Static Policy
  → cpuset cgroup으로 특정 Physical Core 전용 할당
  → SMT 경쟁 및 NUMA Cross-Socket 접근 최소화 가능
  → 단, NUMA Node 경계는 별도 TopologyManager 필요
```

### NUMA 토폴로지 정책 (TopologyManager)

Kubernetes TopologyManager를 활성화하면 CPU와 메모리를 동일 NUMA Node에서 할당하도록 정책을 설정할 수 있다.

| 정책 | 설명 | 내부 메커니즘 |
|------|------|--------------|
| `none` (기본) | NUMA 인식 없음 | cpuset 기반 할당만, NUMA 최적화 없음 |
| `best-effort` | 가능한 경우 동일 NUMA Node 할당 | NUMA Hint Provider에서 Hint 수집, 최적 Node 선택 시도 |
| `restricted` | NUMA 정렬 불가 시 Pod 스케줄링 제한 | Hint 불일치 시 `TopologyAffinityError` 반환 |
| `single-numa-node` | 단일 NUMA Node 할당 강제 | 모든 자원(CPU + Memory + Device)을 단일 Node 내로 제한 |

---

## 12. JVM 및 고성능 서버에서의 영향

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| JIT Compilation과 NUMA | JVM (C1/C2) + Kernel | C2 컴파일된 코드가 Code Cache에 배치, NUMA 환경에서 Code Cache가 특정 Node에 집중되면 Remote Fetch 발생, JVM `-XX:ReservedCodeCacheSize` 조정 | `jcmd <pid> Compiler.queue`, `-XX:+PrintCompilation`, `numastat -p <pid>` |
| Safepoint와 Thread 정지 | JVM | GC / Deoptimization / Biased Lock Revocation 시 Safepoint 요청: 모든 JVM Thread가 Safepoint Poll Point에서 정지, Multi-Socket에서 Thread가 분산될수록 Safepoint 도달 시간(Time-to-Safepoint) 증가 | `-XX:+PrintSafepointStatistics`, `jcmd <pid> Thread.print`, `jhsdb` |
| TLAB (Thread-Local Allocation Buffer) | JVM + Kernel | 각 Thread가 Heap Eden 영역의 TLAB 내에서 `bump-the-pointer`로 빠른 객체 할당, TLAB 소진 시 Heap Lock 경합 발생, NUMA 환경에서 TLAB 메모리가 Remote Node에 할당되면 성능 저하 | `-XX:+PrintTLAB`, `-Xlog:gc+tlab=debug`, `-XX:+UseNUMA`로 NUMA-aware TLAB 활성화 |
| GC Thread와 NUMA | JVM + Kernel | Parallel / G1 GC의 GC Thread가 여러 Socket에 분산 실행 시 Heap 메모리 Remote 접근 증가, `-XX:+UseNUMA`: NUMA-aware Heap 할당 (Young Gen을 Node별로 분산) | `-XX:+UseNUMA`, `-XX:+UseNUMAInterleaving`, `jstat -gcnew <pid>` |
| Direct Memory / Off-Heap NUMA | JVM / Kernel | `ByteBuffer.allocateDirect()` → `mmap()`/`malloc()` → OS Page 할당, First-Touch 정책으로 최초 접근 Thread의 Node에 배치, Netty `PooledDirectByteBuf`가 특정 Node에 집중 가능 | `/proc/PID/numa_maps` (anon 항목), `numastat -p <pid>` |
| Thread Migration 문제 | Kernel | Scheduler가 JVM Thread를 다른 Socket으로 이동 시 L1/L2 Cache Warmup 손실, Remote Memory 접근 발생, Latency 급증 | `perf stat -e migrations`, `/proc/PID/status` (voluntary_ctxt_switches), `taskset`으로 Affinity 고정 |
| Connection Pool Exhaustion | App + Kernel | DB/HTTP Connection Pool 고갈 시 Thread가 Futex 대기(`futex(FUTEX_WAIT)` 시스템 콜), Off-CPU Time 급증, NUMA Remote에 있는 Thread가 긴 대기 후 깨어나면 추가 Wakeup Latency 발생 | `offcputime-bpfcc`, `ss -tp`, `jstack <pid> \| grep WAITING` |
| Finalization Queue | JVM | `Object.finalize()` 등록 객체: Finalizer Thread가 별도 처리, Finalizer Queue 적체 시 메모리 해제 지연, NUMA Remote Node의 메모리 해제 지연으로 Old Gen 압박 | `jmap -histo <pid>`, `-XX:+PrintGCDetails`, `jcmd <pid> GC.run_finalization` |

### Thread Migration 문제

```
JVM Thread → Scheduler에 의해 다른 Socket으로 이동
  → L1/L2 Cache Warmup 손실 (Cold Start)
  → Remote Memory 접근 발생 (+50~100% Latency)
  → NUMA Fault 카운터 증가
  → Tail Latency 악화 (p99/p999)
```

### Netty / Kafka NUMA-aware 구성

| 전략 | 목적 | 구현 방법 |
|------|------|----------|
| CPU Pinning (taskset / numactl) | 특정 Socket Core에 Thread 고정, Cache Warmup 유지 | `numactl --cpunodebind=0 --membind=0 kafka-server-start.sh` |
| NIC와 동일 NUMA Node 사용 | PCIe DMA 데이터 이동 시 Remote 접근 방지 | `cat /sys/class/net/<nic>/device/numa_node`, IRQ Affinity를 NIC의 NUMA Node CPU에 고정 |
| RPS / RFS (Receive Packet Steering) | NIC 수신 패킷을 처리 Thread와 동일 CPU로 유도 | `/sys/class/net/<nic>/queues/rx-*/rps_cpus`, `/proc/sys/net/core/rps_sock_flow_entries` |
| NUMA-aware 메모리 할당 | Local Memory 접근률 극대화 | `numactl --membind=0`, Netty `UnpooledByteBufAllocator` → NUMA Node별 Pool 구성 |
| Backpressure 적용 | Downstream 처리 지연 시 Upstream 속도 조절, Retry Storm 방지 | Kafka `max.in.flight.requests.per.connection`, Netty `Channel.isWritable()` 확인 |

---

## 13. 구성 요소 요약

| 구성 요소 | 역할 | 핵심 메커니즘 | 관찰 인터페이스 |
|-----------|------|--------------|----------------|
| Socket | CPU 패키지 장착 단위 | UPI/Infinity Fabric 인터커넥트, PCIe Root Complex | `lscpu`, `dmidecode -t 4` |
| Physical Core | 실제 연산 유닛 | Out-of-Order Pipeline, Branch Predictor, L1/L2 Cache | `perf stat`, `toplev.py` |
| Hardware Thread | 하드웨어 실행 컨텍스트 | SMT 파이프라인 공유, Register File 독립 | `/sys/devices/system/cpu/smt/` |
| Logical CPU | 운영체제 스케줄링 단위 | `cpu_online_mask`, per-CPU `rq` (RunQueue) | `/proc/cpuinfo`, `nproc` |
| NUMA Node | Socket에 연결된 로컬 메모리 영역 | `pg_data_t`, ACPI SRAT, First-Touch 정책 | `numactl --hardware`, `numastat` |
| Scheduler | Logical CPU 기준 실행 배치 관리 | CFS vruntime, Red-Black Tree, NUMA-aware Load Balancing | `/proc/schedstat`, `perf sched` |
| IMC | 메모리 접근 제어 | DDR 채널 병렬화, RAS/CAS 타이밍, ECC | `ipmitool`, `mcelog` |
| cgroup | 컨테이너 자원 격리 | CFS Bandwidth, `cpuset`, `memory.numa_node` | `/sys/fs/cgroup/`, `systemd-cgtop` |

---

## 14. 성능 최적화 핵심 요소

```
Socket Topology 인식 (lscpu / lstopo / numactl --hardware)
+ NUMA 로컬 메모리 접근 극대화 (numactl --membind, -XX:+UseNUMA)
+ Core 배분 균형 (대칭 Socket 구성 선호, CPU Manager Static Policy)
+ SMT 구조 고려 (워크로드 특성에 따라 HT 활성화/비활성화)
+ CPU Frequency Scaling 인식 (P-state 고정, C-state 제한으로 Latency 안정화)
+ Scheduler NUMA-aware 배치 활용 (numa_balancing, sched_domain 튜닝)
+ IRQ / RPS / RFS 분산 (NIC NUMA Node 일치)
+ Off-CPU Time 최소화 (Lock 경합, I/O 대기, Connection Pool 고갈 탐지)
= Stable Throughput + Low Latency + Low Tail Latency
```

### 설계가 필요한 환경별 핵심 체크포인트

| 환경 | 핵심 체크포인트 | 주요 도구 |
|------|----------------|----------|
| Kubernetes CPU Manager + TopologyManager | Guaranteed QoS, `single-numa-node` 정책, cpuset 정렬 | `/sys/fs/cgroup/cpuset.cpus`, `cadvisor` |
| JVM 대규모 서비스 | `-XX:+UseNUMA`, TLAB 크기, Safepoint Time-to-Safepoint, GC Thread 분산 | `jcmd`, `-Xlog:gc*`, `numastat -p` |
| Netty / Kafka NUMA-aware 배포 | NIC NUMA Node 확인, IRQ Affinity 고정, RPS/RFS 설정 | `/sys/class/net/*/device/numa_node`, `irqbalance` |
| 금융 시스템 저지연 Core Pinning | C-state 비활성화 (`idle=poll`), P-state 최대 고정, HT 비활성화 고려 | `tuned-adm profile latency-performance`, `turbostat` |
| 대규모 트래픽 처리 서버 아키텍처 | Memory Bandwidth Saturation 측정, Interconnect 병목 확인, NUMA Imbalance 탐지 | `perf stat -e offcore_*`, `numastat`, `perf c2c` |

---

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*