# CPU Register(레지스터)와 연산 실행 구조 (E2E 분석 적용됨)

## 1. Register란?

Register는 CPU Core 내부에 존재하는 최상위 저장 계층이다. CPU는 연산을 수행하기 전에 반드시 데이터를 Register에 적재해야 하며, 대부분의 연산은 Register를 직접 대상으로 수행된다.

### Register의 목적

| 목적 | 설명 | 계층 |
|------|------|------|
| 연산 데이터 저장 | ALU/FPU 입력 데이터 보관 | Hardware |
| 연산 결과 저장 | 계산 결과 임시 저장 | Hardware |
| 명령 실행 제어 | 현재 실행 상태 유지 | Hardware |
| 메모리 주소 관리 | 데이터 및 명령어 위치 관리 | Hardware / OS Kernel |
| CPU 상태 저장 | 실행 상태 및 플래그 관리 (Context Switch 시 대상) | Hardware / OS Kernel |

### 계층별 실제 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **Register 접근 속도** | Hardware | sub-cycle 접근. CPU 내부 Register File에서 직접 읽기/쓰기. L1 Cache보다 빠름 | `perf stat -e stalled-cycles-backend` |
| **Register 상태 저장 (Context Switch)** | Kernel | `task_struct`의 `thread` 필드에 GPR 전체, PC, SP, Flags 저장. `schedule()` 함수가 `__switch_to()` 호출 | `pidstat -w`, `/proc/PID/status`의 `voluntary_ctxt_switches` |
| **JIT Register 최적화** | JVM Runtime | C2 JIT Compiler가 Hot Variable을 Register에 고정 배치. Escape Analysis로 Heap 할당을 Register/Stack으로 치환 | `-XX:+PrintOptoAssembly`, `async-profiler` |

---

## 2. 메모리 계층 구조에서의 위치

Register는 전체 메모리 계층의 최상위에 위치하며, 접근 속도가 가장 빠르고 용량이 가장 작다.

```
Register          ← 가장 빠름 / 가장 작음  (sub-cycle, ~수십 개)
  ↓
L1 Cache          ← ~4 cycle  / 32~64 KB
  ↓
L2 Cache          ← ~12 cycle / 256 KB ~ 1 MB
  ↓
L3 Cache          ← ~40 cycle / 수십 MB
  ↓
RAM               ← ~200 cycle / GB 단위
  ↓
Storage           ← 가장 느림 / 가장 큼  (μs ~ ms)
```

### Register 특징

| 항목 | 설명 | 관련 메커니즘 |
|------|------|-------------|
| 위치 | CPU Core 내부 | Register File, Physical Register File |
| 속도 | 가장 빠름 | sub-cycle 직접 접근 |
| 접근 지연 | 거의 없음 | ALU/FPU와 직결 |
| 용량 | 매우 작음 (수십 개 수준) | Register Pressure, Spill 발생 원인 |
| 접근 방식 | CPU 명령어가 직접 지정 | ABI(Calling Convention) 결정 |

### 계층별 메모리 계층 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **L1 Cache Miss → Register Stall** | Hardware | Register에 데이터가 없으면 LSU가 L1d → L2 → L3 → RAM 순서로 Fetch. 이 지연이 Pipeline MEM Stage를 Stall시킴 | `perf stat -e L1-dcache-load-misses,LLC-load-misses` |
| **NUMA 원격 메모리 접근** | Hardware + Kernel | 원격 NUMA 노드에서 데이터 로드 시 ~100ns 추가 Latency. Register에 적재되기까지 Pipeline이 멈춤 | `numastat -c`, `numactl --membind`, `perf mem -t load record` |
| **TLB Miss → Page Table Walk** | Hardware + Kernel | 가상 주소 → 물리 주소 변환 실패 시 Page Table Walk 발생. Register에 올릴 데이터 주소 확정 지연 | `perf stat -e dTLB-load-misses`, `/proc/meminfo`의 `HugePages` |
| **HugePage / THP** | Kernel | 2MB 페이지 사용 시 TLB Entry 수 감소 → TLB Miss 감소 → Register 적재 지연 완화 | `/sys/kernel/mm/transparent_hugepage/enabled`, `grep AnonHugePages /proc/meminfo` |
| **Page Cache / Page Fault** | Kernel | 파일 mmap 데이터를 Register에 적재 시 Major Fault 발생 가능. Disk I/O까지 기다리는 동안 Thread Block | `vmstat`의 `pgfault/pgmajfault`, `/proc/vmstat` |

---

## 3. CPU 연산과 Register

CPU는 메모리 데이터를 직접 연산하지 않는다. 반드시 Register를 경유해야 한다.

```
Memory
  ↓ Load (LSU → L1d Cache → Register)
Register          ← 연산 입력
  ↓
ALU / FPU 연산
  ↓
Register          ← 연산 결과
  ↓ Store (Register → L1d Cache → RAM)
Memory
```

이 구조는 x86, ARM 등 대부분의 현대 아키텍처에서 공통적으로 적용된다.

### 연산 계층별 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **Load / Store (LSU)** | Hardware | Load Buffer / Store Buffer가 Register ↔ Cache 사이 데이터 이동 관리. Store-to-Load Forwarding으로 Stall 감소 | `perf stat -e ld_blocks.store_forward`, `perf mem` |
| **시스템 콜과 Register** | App → Kernel | `syscall` 명령어 실행 시 User Register 상태가 Kernel Stack에 저장(`pt_regs` 구조체). 반환 시 복원. RAX에 syscall 번호 및 반환값 | `strace`, `perf trace`, `/proc/PID/syscall` |
| **IRQ와 Register** | Hardware → Kernel | IRQ 발생 시 CPU가 현재 실행 중인 명령어의 Register 상태를 자동으로 Kernel Stack에 Push. IRQ Handler 종료 후 복원 | `/proc/interrupts`, `mpstat`의 `%irq` |
| **Out-of-Order와 Physical Register** | Hardware | Register Renaming: 논리 레지스터를 Physical Register File에 매핑하여 WAW/WAR Hazard 제거. ROB가 순서대로 Commit | `perf stat -e uops_retired.retire_slots` |
| **Memory Bandwidth Saturation** | Hardware | 다수 코어가 동시에 메모리 접근 시 DDR 채널 포화 → Register 적재 지연 연쇄 발생 | `pcm-memory`, `perf stat -e offcore_requests_outstanding.cycles_with_data_rd` |

---

## 4. Register의 종류

### 4-1. General Purpose Register (GPR, 범용 레지스터)

일반 데이터 연산 및 메모리 주소 관리에 사용되는 레지스터다.

| 용도 | 설명 | 관련 메커니즘 |
|------|------|-------------|
| 정수 연산 | ADD, SUB, MUL 등의 입출력 | ALU, IPC |
| 메모리 주소 저장 | 포인터 값 보관 | LSU, TLB |
| 함수 인자 전달 | Calling Convention에 따른 인자 전달 | ABI, Stack Frame |
| 임시 데이터 저장 | 중간 계산 결과 보관 | Register Pressure |

#### x86-64 주요 범용 레지스터

| Register | 주요 역할 | ABI 용도 |
|----------|-----------|---------|
| RAX | 연산 결과, 반환값 | syscall 번호 / 반환값 |
| RBX | 일반 데이터 | Callee-saved |
| RCX | 반복 카운터 | 4번째 syscall 인자 |
| RDX | 데이터 연산 보조 | 3번째 함수 인자 |
| RSI | Source Pointer | 2번째 함수 인자 |
| RDI | Destination Pointer | 1번째 함수 인자 |
| RSP | Stack Pointer | 현재 Stack Top |
| RBP | Base Pointer | 스택 프레임 기준 |
| R8~R9 | 확장 범용 | 5~6번째 함수 인자 |
| R10~R15 | 확장 범용 | Caller/Callee-saved |

> 64-bit CPU는 Register 크기가 64-bit임을 의미하며, 한 번에 64-bit 정수 및 메모리 주소를 처리할 수 있다.

#### GPR 계층별 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **Register Renaming** | Hardware | 논리 레지스터(RAX 등)를 Physical Register File(~200개)에 동적 매핑. Out-of-Order Execution의 전제 조건 | `perf stat -e int_misc.recovery_cycles` |
| **Context Switch — task_struct** | Kernel | `task_struct`의 `thread.regs` 또는 `thread_struct`에 GPR 전체 저장/복원. `__switch_to_asm()` 어셈블리 루틴이 직접 처리 | `perf sched latency`, `pidstat -w` |
| **Calling Convention과 JNI** | JVM Runtime | JNI 호출 시 Java ABI → Native ABI 간 레지스터 변환 발생. Critical Section 유지 시 GC 정지 불가 | `jstack` (JNI 상태 확인), `-verbose:jni` |
| **Serialization / Deserialization 비용** | App / JVM | JSON/Protobuf 파싱 시 비연속 메모리 접근 → Register에 올릴 데이터가 Cache Miss → IPC 저하 | `async-profiler` CPU 프로파일, `perf record -g java` |

---

### 4-2. Special Purpose Register (특수 목적 레지스터)

CPU 제어 흐름 및 상태 관리에 사용되는 레지스터다.

#### Program Counter (PC)

다음에 실행할 명령어의 메모리 주소를 저장한다. Instruction Fetch 단계에서 참조된다.

```
PC → 다음 명령어 주소 → Instruction Fetch → Branch Predictor 참조
```

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **Branch Prediction과 PC** | Hardware | Branch Predictor(BTB, TAGE)가 PC를 기반으로 다음 명령어 주소를 예측. 예측 실패 시 Pipeline Flush + PC 복원 | `perf stat -e branch-misses,branch-instructions` |
| **Safepoint와 PC** | JVM Runtime | JVM이 Safepoint 요청 시 모든 Thread의 PC가 Safe Region 밖이면 대기(TTSP 발생). GC 외 Deoptimization, 바이어스 락 해제에도 사용 | `-XX:+PrintSafepointStatistics`, `jstack` |
| **vDSO와 PC** | App (User-side) | `clock_gettime()` 등이 vDSO를 통해 Kernel 전환 없이 실행. PC가 vDSO 매핑 영역을 가리키며 실행 | `/proc/PID/maps`의 `vdso` 항목, `perf stat` |

#### Instruction Register (IR)

현재 실행 중인 명령어를 저장한다. Instruction Decode 단계에서 참조된다.

#### Stack Pointer (SP)

현재 Stack의 최상단 주소를 가리킨다. 함수 호출, 로컬 변수 관리에 사용된다.

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **Stack Frame 관리** | Hardware / App | 함수 호출 시 RSP 감소(PUSH), 반환 시 RSP 증가(POP). 인자 7개 이상은 Stack으로 전달 | `perf record -g`(Call Graph), `async-profiler` |
| **Stack Overflow / 시그널** | Kernel | Stack이 `RLIMIT_STACK` 초과 시 Kernel이 SIGSEGV 전달. `sigaltstack()`으로 별도 Signal Stack 등록 가능 | `ulimit -s`, `/proc/PID/status`의 `SigCgt`, `dmesg` |
| **TLAB과 SP** | JVM Runtime | JVM의 Thread-Local Allocation Buffer는 SP와 유사한 포인터(top)로 관리. 객체 할당 = top 증가만으로 완료(Bump Pointer) | `-XX:+PrintTLAB`, `jstat -gc` |

#### Status Register / Flags Register

직전 연산 결과의 상태를 저장한다. 조건 분기 명령어가 이 값을 참조한다.

| 플래그 | 설명 | 관련 시나리오 |
|--------|------|-------------|
| Zero Flag (ZF) | 연산 결과가 0인 경우 설정 | JE / JNE 분기 |
| Carry Flag (CF) | 비트 자리올림 발생 시 설정 | 부호 없는 정수 오버플로우 |
| Overflow Flag (OF) | 부호 있는 정수 오버플로우 시 설정 | ArithmeticException 전제 |
| Sign Flag (SF) | 연산 결과가 음수인 경우 설정 | JS / JNS 분기 |

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **Flags와 Branch Prediction** | Hardware | 조건 분기 명령어가 Flags를 참조. Branch Predictor가 Flags 결과를 예측하여 Pipeline을 미리 진행 | `perf stat -e branch-misses` |
| **Flags와 Context Switch** | Kernel | Context Switch 시 RFLAGS 레지스터도 `task_struct`에 저장/복원 대상에 포함. 정확한 실행 재개를 위해 필수 | `perf sched`, `pidstat -w` |
| **Branchless 코드** | App / JVM | ZF·SF 등에 의존하는 조건 분기를 CMOV(Conditional Move)로 대체하여 Branch Misprediction 제거. JVM C2가 자동 적용 | `perf annotate` (CMOV vs JMP 비교) |

---

### 4-3. SIMD Register (벡터 레지스터)

여러 데이터를 하나의 명령어로 병렬 처리하는 벡터 연산에 사용된다.

| 기술 | 레지스터 | 크기 | 동시 처리 |
|------|---------|------|---------|
| SSE | XMM0~XMM15 | 128-bit | 4 × 32-bit float |
| AVX | YMM0~YMM15 | 256-bit | 8 × 32-bit float |
| AVX-512 | ZMM0~ZMM31 | 512-bit | 16 × 32-bit float |

```
1개의 AVX Register (256-bit)
  → 8개의 32-bit 정수 동시 연산 가능
  → 4개의 64-bit 부동소수점 동시 연산 가능
```

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **SIMD와 JIT Auto-Vectorization** | JVM Runtime | JVM C2 JIT가 루프를 Auto-Vectorization하여 AVX/SSE 명령어 생성. 배열 연산, String 처리에 자동 적용 | `-XX:+PrintOptoAssembly`, `hsdis` 디스어셈블러 |
| **SIMD와 Context Switch 비용** | Kernel | AVX-512 사용 시 ZMM 레지스터 32개 × 64B = 2KB 추가 저장. Context Switch 비용 급증. AVX-512 사용 여부에 따라 CPU Frequency도 저하 가능 | `perf stat -e context-switches`, `turbostat` (AVX 사용 시 주파수 확인) |
| **SIMD와 Cache Line** | Hardware | SIMD Load/Store는 64B Cache Line 경계 정렬 여부가 성능에 직결. 비정렬 접근 시 추가 Latency 발생 | `perf mem record`, `perf stat -e misalign_mem_ref.*` |
| **AVX Transition Penalty** | Hardware | SSE ↔ AVX 혼용 시 Upper Register 초기화를 위한 Penalty 발생(~70 cycle). JIT 코드와 JNI Native 코드 혼합 시 주의 | `perf stat -e other_assists.avx_to_sse` |

---

## 5. Register File

CPU 내부의 Register 전체 집합을 **Register File**이라고 한다. ALU/FPU는 Register File과 직접 연결되어 데이터를 읽고 쓴다.

```
Register File
  ├── GPR (RAX, RBX, ... R15)          — 정수 연산
  ├── SIMD (XMM0~15 / YMM / ZMM)       — 벡터 연산
  ├── PC (Program Counter)             — 명령어 포인터
  ├── SP (Stack Pointer)               — 스택 관리
  ├── RFLAGS                           — 상태 플래그
  └── Physical Register File (~200개)  — Register Renaming 대상
```

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **Physical Register File** | Hardware | 논리 레지스터(16개)보다 훨씬 많은 물리 레지스터(Intel: ~180~200개)를 통해 Out-of-Order Execution 지원. RAT(Register Alias Table)가 매핑 관리 | `perf stat -e uops_issued.any` |
| **Register File과 CPU Frequency Scaling** | Hardware + Kernel | P-state 전환 시 Register File 접근 Latency 변화 없음(전압/주파수 변화는 사이클 수에 영향). C-state 복귀 시 Register 상태 복원 필요 | `cpupower frequency-info`, `turbostat`, `/sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq` |
| **Register File과 SMT (HyperThreading)** | Hardware | 하나의 물리 코어에서 2개 Thread가 Physical Register File을 분할 사용. Register 수 부족 시 양 Thread 모두 성능 저하 | `lscpu`의 `Thread(s) per core`, `perf stat -M pipeline` |

---

## 6. Pipeline과 Register

Pipeline 각 단계는 Register를 통해 데이터를 주고받는다.

```
IF  (Instruction Fetch)     ← PC 참조 → Branch Predictor
  ↓
ID  (Instruction Decode)    ← IR 사용 → Register Renaming (RAT)
  ↓
EX  (Execute)               ← GPR 읽기, ALU/FPU 연산
  ↓
MEM (Memory Access)         ← LSU를 통한 Cache/RAM 접근
  ↓
WB  (Write Back)            ← 연산 결과를 Register에 기록 (ROB Commit)
```

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **Pipeline Stall과 Register Dependency** | Hardware | RAW(Read After Write) Hazard: 앞 명령어가 Register에 값을 쓰기 전에 다음 명령어가 읽으려 할 때 Stall 발생. Data Forwarding으로 완화 | `perf stat -e stalled-cycles-backend` |
| **Branch Misprediction과 Register 복원** | Hardware | 예측 실패 시 ROB를 Flush하고 Physical Register를 잘못된 상태에서 복원. Checkpoint 기반 복구 | `perf stat -e branch-misses`, `perf annotate` |
| **OOO Execution과 Register Commit** | Hardware | ROB(Reorder Buffer)가 명령어를 순서대로 Retire. Register에 최종 Write는 Retire 시점에만 수행(Precise Exception 보장) | `perf stat -e uops_retired.total_cycles` |
| **cgroup / CPU Throttling과 Pipeline** | Kernel | cgroup v2 CPU Quota 소진 시 Process가 Runqueue에서 제거 → Register 상태는 `task_struct`에 보존된 채 대기 | `/sys/fs/cgroup/cpu.stat`의 `throttled_usec`, `kubectl top` |
| **Off-CPU Time과 Register 보존** | Kernel | Thread가 I/O 대기, Lock 대기로 Block될 때 Register 전체가 Kernel Stack에 보존. Resume 시 그대로 복원 | `offcputime-bpfcc`, `perf sched latency` |

---

## 7. LSU와 Register의 관계

LSU(Load Store Unit)는 Register와 메모리(Cache/RAM) 사이의 데이터 이동을 담당한다.

```
Load:  RAM → L3 → L2 → L1d Cache → Register File
Store: Register File → L1d Cache → L2 → L3 → RAM
```

| 명령 | 방향 | Latency |
|------|------|---------|
| Load (L1 Hit) | Memory → Register | ~4 cycle |
| Load (LLC Hit) | Memory → Register | ~40 cycle |
| Load (RAM) | Memory → Register | ~200 cycle |
| Store | Register → Memory | Write Buffer 경유 |

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **Store-to-Load Forwarding** | Hardware | Store Buffer에 아직 남아 있는 값을 Load가 직접 참조. Register Write 완료를 기다리지 않고 진행 | `perf stat -e ld_blocks.store_forward` |
| **Cache Line Thrashing** | Hardware | 여러 Thread가 동일 64B Cache Line에 Write → MESI 무효화 연쇄 → Register에 올릴 데이터를 계속 재Fetch. False Sharing 대표 패턴 | `perf c2c record && perf c2c report` |
| **mmap과 LSU** | App ↔ Kernel | mmap 영역 접근 시 Page Fault 발생 → Kernel이 물리 페이지 매핑 → LSU가 실제 데이터를 Register에 적재 | `/proc/PID/maps`, `vmstat`의 `pgfault`, `perf stat -e dTLB-load-misses` |
| **Dirty Page Writeback** | Kernel | Register → Cache → RAM 경로에서 Dirty Page가 Disk로 Writeback되는 시점에 Write Stall 발생 가능 | `/proc/vmstat`의 `nr_dirty`, `sar -b` |
| **Direct Memory / Off-Heap** | JVM / App | `ByteBuffer.allocateDirect()`, `Unsafe` 사용 시 JVM Heap 외부 메모리를 LSU가 직접 접근. GC 대상 아님 | `NativeMemoryTracking`, `/proc/PID/status`의 `VmRSS` vs `VmHWM` |
| **NUMA와 LSU Latency** | Hardware + Kernel | 원격 NUMA 노드 메모리를 Load 시 Local 대비 2~3배 Latency. `-XX:+UseNUMA`로 JVM Heap을 로컬 노드에 고정 | `numastat`, `numactl --membind=0`, `perf mem -t load` |

---

## 8. Register Pressure와 Register Spilling

### Register Pressure

필요한 변수의 수가 사용 가능한 Register 수를 초과하는 상황이다.

```
활성 변수 수 > 사용 가능한 Register 수
→ Register Pressure 발생
→ Compiler / JIT가 Spill 결정
```

### Register Spilling

Register Pressure 발생 시 컴파일러(또는 JIT)는 일부 Register 값을 Stack 메모리에 임시 저장한다.

```
Register (Hot Variable)
  ↓ Spill (Store to Stack)
Stack Memory (L1d Cache → RAM)
  ↓ Reload 시
Register (다시 사용 가능)
```

| 영향 | 설명 | 계층 |
|------|------|------|
| 메모리 접근 증가 | LSU 사용 빈도 증가 | Hardware |
| Cache Miss 가능성 증가 | Spill된 데이터가 Cache에 없을 수 있음 | Hardware |
| Pipeline Stall 증가 | 메모리 대기로 IPC 감소 | Hardware |
| JIT 재컴파일 트리거 | Spill 과다 시 C2가 다른 Register 배치로 재최적화 | JVM Runtime |

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **JIT Register Allocation** | JVM Runtime | C2 JIT가 Graph Coloring 알고리즘으로 Register 배치 결정. Loop 내 변수 수가 많으면 Spill 증가 → 성능 저하 | `-XX:+PrintOptoAssembly`, `jitwatch` |
| **Spill과 Cache 영향** | Hardware | Stack Spill 데이터는 L1d에 캐시되지만 함수가 복잡할수록 Cache Eviction 발생 가능. L1d Miss 증가로 IPC 직접 저하 | `perf stat -e L1-dcache-load-misses`, `perf mem` |
| **SIMD Register Spill** | Hardware / JVM | AVX/SSE Register(XMM/YMM)도 Spill 대상. 크기가 크므로(16~64B) Spill 비용이 GPR 대비 훨씬 높음 | `perf stat -e stalled-cycles-backend` |
| **Safepoint와 Register 상태** | JVM Runtime | Safepoint 진입 시 JVM이 모든 Register에 있는 GC Root 정보를 OopMap으로 기록. Register 수 많을수록 OopMap 크기 증가 | `-XX:+PrintSafepointStatistics` |
| **ClassLoader Leak** | JVM Runtime | 클래스 재로드 시 JIT가 해당 클래스의 Register Allocation 결과(컴파일 코드)를 폐기. MetaSpace 누수 → Full GC → 모든 Register 상태 보존 후 Stall | `-XX:MaxMetaspaceSize`, `jmap -clstats` |

---

## 9. Context Switching과 Register

스레드 전환 시 현재 스레드의 Register 상태 전체를 저장하고, 다음 스레드의 Register 상태를 복원해야 한다.

### 저장 대상

| 항목 | 설명 | 저장 위치 |
|------|------|---------|
| General Purpose Registers | 모든 범용 레지스터 값 | `task_struct.thread` |
| Program Counter | 다음 실행 명령어 위치 | `task_struct.thread.ip` |
| Stack Pointer | 스택 상태 | `task_struct.thread.sp` |
| Flags Register | CPU 연산 상태 | `task_struct.thread.flags` |
| SIMD Registers (XMM/YMM/ZMM) | 벡터 레지스터 전체 | FPU State (`fxsave`/`xsave`) |

### Context Switch 흐름

```
Thread A 실행 중
  → Scheduler 개입 (타이머 IRQ 또는 자발적 yield)
  → Thread A Register 상태 저장 (task_struct + Kernel Stack)
  → TLB Flush (PCID 없는 경우) + Cache Warm-down
  → Thread B task_struct에서 Register 상태 복원
  → Thread B Pipeline 재충전 (Branch Predictor 재학습 필요)
  → Thread B 실행 재개
```

### Context Switch 비용 구성

```
Register Save 비용       (GPR: ~16 × 8B = 128B)
+ SIMD Register Save     (AVX-512 사용 시 +2KB)
+ Register Restore 비용
+ TLB Flush 비용         (PCID 미사용 시 전체 Flush)
+ Cache Warm-up 비용     (L1/L2 Cold Start)
+ Branch Predictor 재학습
= Context Switch 전체 비용
```

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **CFS Scheduler** | Kernel | Completely Fair Scheduler: Red-Black Tree 기반 vruntime 관리. Runqueue에서 다음 Thread 선택 후 `__switch_to()` 호출 | `/proc/schedstat`, `perf sched`, `pidstat -w` |
| **task_struct와 Register** | Kernel | `task_struct`의 `thread_struct`에 레지스터 전체 저장. `arch/x86/kernel/process_64.c`의 `__switch_to_asm` 구현 | `/proc/PID/status`, `crash`(커널 디버거) |
| **SIMD State Save (xsave)** | Hardware + Kernel | `xsave`/`xrstor` 명령어로 SSE/AVX 상태 저장/복원. AVX-512 활성화 시 저장 크기 ~2.5KB → Context Switch 비용 증가 | `perf stat -e context-switches` 비용 비교 |
| **Futex와 Context Switch** | OS / App | Java `synchronized`, `ReentrantLock` 경합 시 `futex_wait()` 시스템 콜 → Register 저장 후 Thread Block. 재획득 시 `futex_wake()` → Restore | `perf trace -e futex`, `strace -e futex java`, `/proc/PID/status`의 `nonvoluntary_ctxt_switches` |
| **cgroup과 Context Switch** | Kernel | CPU Quota 소진 시 cgroup Throttling → Thread가 Runqueue에서 제거. Register 상태는 `task_struct`에 보존. Quota 갱신 시 재진입 | `/sys/fs/cgroup/cpu.stat`의 `throttled_usec`, `cadvisor` |
| **PSI (Pressure Stall Information)** | Kernel | CPU Pressure some/full 지표로 Context Switch 과부하 수준 측정. `full` 상태는 전체 Thread가 대기 중 = Register가 모두 Kernel Stack에 묶인 상태 | `/proc/pressure/cpu`, `/proc/pressure/memory` |
| **Off-CPU Time** | Kernel | Context Switch 후 다시 CPU를 받기까지의 시간. Register는 Kernel Stack에 보존되어 있으나 실제 연산 없음 | `offcputime-bpfcc`, `perf sched latency` |

---

## 10. Function Call과 Register

현대 아키텍처의 Calling Convention은 함수 인자를 Register로 전달하여 메모리 접근 비용을 줄인다.

### x86-64 Linux (System V AMD64 ABI)

| 인자 순서 | Register | syscall 용도 |
|-----------|----------|------------|
| 1번째 인자 | RDI | RDI |
| 2번째 인자 | RSI | RSI |
| 3번째 인자 | RDX | RDX |
| 4번째 인자 | RCX | R10 (syscall에서 RCX는 RC에 사용) |
| 5번째 인자 | R8 | R8 |
| 6번째 인자 | R9 | R9 |
| 7번째 이상 | Stack 사용 | Stack |
| 반환값 | RAX | RAX (errno 포함) |

인자가 6개 이하인 경우 Stack 접근 없이 Register만으로 함수 호출이 완료된다.

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **시스템 콜 Register 규약** | App → Kernel | `syscall` 명령어 실행 시 RCX에 복귀 주소, R11에 RFLAGS 저장. Kernel 진입 후 `pt_regs` 구조체로 User Register 전체 보존 | `strace`, `perf trace`, `/proc/PID/syscall` |
| **인라인 함수와 Register 재사용** | JVM / App | JIT Inlining 시 Caller 함수의 Register를 Callee가 직접 사용. 함수 경계가 사라지므로 Register 사용 효율 극대화 | `-XX:+PrintInlining`, `jitwatch` |
| **Tail Call Optimization** | App / JVM | Tail Call 위치의 함수 호출을 JMP로 변환. Stack Frame 및 Register 저장/복원 생략 | `perf annotate` (JMP vs CALL 확인) |
| **Variadic 함수와 Register Dump** | App | 가변 인자 함수(`printf` 등)는 전달될 인자 수 불확실 → 모든 정수/SIMD 인자 레지스터를 Stack에 Dump. 호출 빈번 시 성능 영향 | `ltrace`, `perf stat` |
| **Connection Pool Exhaustion** | App | DB/HTTP Connection 획득 시도 시 Thread가 Lock 경합 → Register 상태 보존 후 Block. Pool 크기가 Register 사용 패턴 최적화와 무관하게 응답 지연 초래 | `HikariCP metrics`, `thread dump`, `jstack` |

---

## 11. JVM과 Register 최적화

JVM JIT Compiler는 런타임에 Register 최적화를 수행한다.

### Register Allocation

JIT Compiler는 자주 접근하는 변수(Hot Variable)를 가능한 한 Register에 유지하도록 코드를 재컴파일한다.

```
Hot Variable 감지 (Profiling)
  → C1 컴파일 (빠른 컴파일, 제한적 Register 최적화)
  → 임계치 도달 시 C2 컴파일 (Graph Coloring 기반 최적 Register Allocation)
  → 메모리 접근 감소
  → IPC 향상
```

### Escape Analysis

객체가 생성된 메서드 범위를 벗어나지 않는다고 판단되면, Heap 할당을 생략하고 Stack 또는 Register 수준에서 처리한다.

```
객체가 메서드 외부로 전달되지 않음 (Non-escaping)
  → Heap 할당 제거 (Scalar Replacement)
  → 필드를 개별 Register / Stack 변수로 분해
  → GC 부담 감소
  → TLAB 소비 감소
```

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **C1 / C2 JIT 컴파일** | JVM Runtime | C1: 빠른 컴파일, 기본 Register Allocation. C2: SSA(Static Single Assignment) 기반 최적화, SIMD 자동 생성. Tier 0~4 컴파일 레벨 | `-XX:+PrintCompilation`, `jitwatch`, `-XX:+PrintOptoAssembly` |
| **Deoptimization** | JVM Runtime | C2가 잘못된 타입 프로파일 기반으로 최적화한 코드를 Interpreter로 롤백. Register Allocation 결과가 버려지고 재컴파일 발생 | `-XX:+PrintDeoptimization`, `jitwatch` |
| **Safepoint와 Register OopMap** | JVM Runtime | JIT 생성 코드의 각 Safepoint 위치에서 어떤 Register가 GC Root(객체 참조)를 담고 있는지 OopMap으로 기록. GC가 이를 참조하여 Register 내 포인터를 정확히 업데이트 | `-XX:+PrintSafepointStatistics`, `GC log` |
| **TLAB와 Register** | JVM Runtime | TLAB top 포인터를 Register에 캐시하여 객체 할당을 Register 증가 연산 하나로 완료. TLAB 소진 시 느린 경로(slow path) 진입 | `-XX:+PrintTLAB`, `-Xlog:gc+tlab=debug`, `jstat -gc` |
| **JNI Critical Section** | JVM Runtime | `GetPrimitiveArrayCritical()` 호출 시 GC 정지 불가 상태 진입. 이 구간에서 Register 상태는 JNI 규약에 따라 관리되며 JVM이 개입 불가 | `jstack` (JNI 상태 확인), `-verbose:jni` |
| **Finalization Queue** | JVM Runtime | `finalize()` 보유 객체 누적 시 Finalizer Thread가 Register를 사용하며 지속 실행. GC가 객체를 즉시 회수 못해 힙 압박 증가 | `jmap -finalizerinfo`, `jstat -gcutil` |
| **Backpressure / Retry Storm** | App | Reactive 시스템에서 Backpressure 발생 시 Worker Thread들이 Queue 대기 → Context Switch 증가 → Register Save/Restore 비용 증가. Retry Storm은 CPU를 Register 연산보다 syscall 처리에 낭비 | `reactor.netty metrics`, `access log 분석` |

---

## 12. CPU 내부 전체 실행 흐름

```
                    ┌──────────────────────────────────┐
                    │   Branch Predictor               │
                    │  (BTB, TAGE, RSB — PC 기반 예측) │
                    └─────────────┬────────────────────┘
                                  │
                    ┌─────────────▼────────────────────┐
                    │   Instruction Fetch (PC 참조)     │
                    │   L1i Cache + iTLB               │
                    └─────────────┬────────────────────┘
                                  │
                    ┌─────────────▼────────────────────┐
                    │   Instruction Decode (IR 사용)    │
                    │   CISC → μOp 변환                 │
                    │   Register Renaming (RAT)         │
                    └─────────────┬────────────────────┘
                                  │
          ┌───────────────────────▼──────────────────────────┐
          │         Issue Queue / OOO Scheduler               │
          │   (독립 명령어 먼저 디스패치 — Register 가용 여부 확인)│
          └──────┬──────────────┬─────────────────┬──────────┘
                 │              │                 │
       ┌─────────▼───┐  ┌───────▼──────┐  ┌──────▼────────┐
       │  ALU / BRU  │  │  FPU / SIMD  │  │   AGU / LSU   │
       │ (GPR 읽기)   │  │ (XMM/YMM/ZMM│  │ (주소 계산 +  │
       │             │  │  읽기/쓰기)  │  │  Cache 접근)  │
       └─────────┬───┘  └───────┬──────┘  └──────┬────────┘
                 │              │                 │
          ┌──────▼──────────────▼─────────────────▼──────────┐
          │          ROB (Reorder Buffer)                      │
          │     순서대로 Commit → Register File Write Back     │
          └───────────────────────────────────────────────────┘
                                  │
          ┌───────────────────────▼──────────────────────────┐
          │             OS / Runtime 계층                      │
          │  Context Switch: task_struct에 Register 전체 저장  │
          │  IRQ / SoftIRQ: Kernel Stack에 Register Push      │
          │  Safepoint: OopMap으로 Register 내 GC Root 기록   │
          │  cgroup Throttle: Register 보존 후 대기            │
          └──────────────────────────────────────────────────┘
```

---

## 13. Linux 및 성능 분석 도구

### Context Switch 모니터링

```bash
vmstat 1            # 전체 시스템 Context Switch 수 (cs 항목)
pidstat -w 1        # 프로세스별 자발적 / 비자발적 Context Switch 수
perf sched latency  # Thread별 스케줄 지연 및 Context Switch 분포
```

### CPU 이벤트 분석

```bash
perf stat -e instructions,cycles,branch-misses,cache-misses,context-switches <command>
perf top                           # 실시간 Hot Function 분석
perf stat -e stalled-cycles-backend,stalled-cycles-frontend  # Pipeline Stall 분석
```

### Register 및 어셈블리 분석

```bash
objdump -d <binary>                # 어셈블리 코드 및 Register 사용 확인
perf annotate                      # 함수별 CPU 사이클 분포 및 명령어 수준 분석
gdb -ex "info registers" -p <PID>  # 실행 중 프로세스의 Register 상태 덤프
```

### JVM Register 최적화 분석

```bash
# JIT 컴파일 및 Register Allocation 확인
java -XX:+PrintCompilation -XX:+PrintOptoAssembly ...
java -XX:+PrintInlining -XX:+PrintEscapeAnalysis ...
java -XX:+PrintTLAB -XX:+PrintSafepointStatistics ...

# 어셈블리 수준 분석 (hsdis 플러그인 필요)
java -XX:+PrintAssembly -XX:PrintAssemblyOptions=intel ...
```

### 시스템 콜 및 Kernel 인터페이스

```bash
strace -e trace=all -p <PID>       # Register 기반 syscall 추적
perf trace -p <PID>                # 경량 syscall 추적
cat /proc/PID/syscall              # 현재 실행 중인 syscall 번호 및 Register 값
```

### SIMD 및 AVX 분석

```bash
turbostat                          # AVX-512 사용 시 CPU 주파수 저하 확인
perf stat -e other_assists.avx_to_sse  # SSE ↔ AVX Transition Penalty
perf stat -e fp_arith_inst_retired.256b_packed_single  # AVX 실제 활용률
```

---

## 14. 구성 요소 요약

| 구성 요소 | 역할 | 계층 |
|-----------|------|------|
| **Register** | CPU 내부 최상위 저장소, 연산 입출력 직접 담당 | Hardware |
| **GPR (범용 레지스터)** | 정수 연산, 주소 관리, 함수 인자 전달 | Hardware |
| **Program Counter (PC)** | 다음 실행 명령어 주소 저장, Branch Predictor 참조 | Hardware |
| **Stack Pointer (SP)** | 현재 Stack 위치 관리, 함수 Frame 기준 | Hardware |
| **Flags Register** | 연산 결과 상태 저장, 조건 분기 기준 | Hardware |
| **Register File** | CPU 내 전체 Register 집합 (논리 + Physical) | Hardware |
| **Physical Register File** | Register Renaming의 실제 저장소, OOO Execution 지원 | Hardware |
| **Register Spill** | Register 부족 시 Stack 메모리에 임시 저장 | Hardware / JVM |
| **SIMD Register (XMM/YMM/ZMM)** | 벡터 병렬 연산 전용 레지스터 | Hardware |
| **Register Allocation (JIT)** | C2 JIT의 Register 최적 배치 전략 | JVM Runtime |
| **Escape Analysis** | Heap 할당을 Register/Stack으로 치환 | JVM Runtime |
| **task_struct.thread** | Context Switch 시 Register 전체 저장 위치 | OS Kernel |
| **pt_regs** | syscall / IRQ 진입 시 User Register 보존 구조체 | OS Kernel |
| **OopMap** | Safepoint에서 Register 내 GC Root 위치 기록 | JVM Runtime |

---

## 15. 성능 최적화 핵심 요소

```
Efficient Register Allocation      (JVM C2 JIT Graph Coloring)
+ Low Register Spill               (Loop 단순화, 변수 수 감소)
+ Fast Register Access             (ALU/FPU 직결, sub-cycle)
+ Efficient Pipeline Flow          (Data Forwarding, OOO Execution)
+ Low Context Switch Cost          (Register Save/Restore 최소화)
+ SIMD Utilization                 (Auto-Vectorization, AVX 활용)
+ NUMA Locality                    (Register 적재 Latency 최소화)
+ Low Safepoint Overhead           (OopMap 크기 및 TTSP 최소화)
= High IPC
= High Throughput + Low Latency
```

이 원칙은 다음 환경의 성능 최적화와 직접 연결된다.

- JVM JIT 컴파일러의 Register Allocation 및 Escape Analysis
- Context Switching 비용 최소화 (논블로킹 서버 구조, Coroutine)
- SIMD 활용 고성능 수치 연산 및 Auto-Vectorization
- NUMA Locality 보장 (`-XX:+UseNUMA`, `numactl`)
- Safepoint 지연 최소화 (Long Running Native Method 제거)
- 금융 시스템 저지연 처리 (C-state 제한, CPU Frequency 고정)
- 대규모 트래픽 처리 아키텍처 설계 (Lock-Free, Off-CPU 최소화)

---

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*