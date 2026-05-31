# CPU L1 / L2 Cache와 메모리 계층 구조 (E2E 분석 적용됨)

## 1. CPU Cache란?

CPU Cache는 CPU와 메인 메모리(RAM) 사이에 위치하는 고속 메모리 계층이다.

CPU의 연산 속도는 RAM 접근 속도보다 훨씬 빠르기 때문에, RAM에만 의존할 경우 CPU가 데이터를 기다리는 대기 시간이 병목이 된다. 이를 해결하기 위해 CPU 내부에 자주 사용하는 데이터와 명령어를 임시 저장하는 Cache 계층이 존재한다.

### Cache의 목적

| 목적 | 설명 | 관련 계층 |
|------|------|-----------|
| 메모리 접근 속도 향상 | RAM 접근 빈도 최소화 | Hardware |
| CPU Stall 감소 | 메모리 대기 시간 감소 | Hardware |
| Pipeline 유지 | 실행 흐름 중단 방지 | Hardware |
| IPC 향상 | 사이클당 처리량 증가 | Hardware / OS |
| Context Switch 비용 완화 | 스레드 전환 시 Cache Warm-Up 비용 존재 | OS Kernel |
| GC 압박 완화 | 연속 메모리 구조로 GC 빈도 감소 | JVM Runtime |

### 계층별 Cache 필요성 실체

```
[Hardware]    CPU 클럭(~3GHz) vs RAM 지연(~100ns) → 수백 사이클 대기 발생
      ↓
[OS Kernel]   Page Table Walk, TLB Miss 시 추가 메모리 접근 → Cache 의존도 증가
      ↓
[JVM Runtime] GC Heap 객체 분산 배치 → Pointer Chasing → Cache Miss 연쇄
      ↓
[Application] 자료구조 선택(배열 vs 연결 리스트)이 Cache 효율을 직접 결정
```

### SRE 관찰 도구

| 도구 | 관찰 대상 |
|------|-----------|
| `lscpu` | L1d / L1i / L2 / L3 크기 및 공유 구조 확인 |
| `perf stat -e cache-misses,cache-references` | Cache Hit Ratio 측정 |
| `perf stat -e LLC-load-misses` | L3 Miss 빈도 (RAM 접근 빈도 근사치) |
| `valgrind --tool=cachegrind` | 함수별 Cache Miss 시뮬레이션 |

---

## 2. CPU 메모리 계층 구조

현대 CPU는 계층형 메모리 구조를 사용한다. 상위 계층일수록 속도가 빠르고 용량이 작으며, 하위 계층일수록 접근 지연이 크고 용량이 크다.

```
CPU Register        ← 가장 빠름 / 가장 작음
  ↓
L1 Cache
  ↓
L2 Cache
  ↓
L3 Cache
  ↓
Main Memory (RAM)
  ↓
Storage (SSD / Disk) ← 가장 느림 / 가장 큼
```

| 계층 | 속도 | 용량 | 접근 지연 | 관리 주체 |
|------|------|------|-----------|-----------|
| Register | 가장 빠름 | 가장 작음 | 거의 없음 | CPU / 컴파일러 |
| L1 Cache | 매우 빠름 | 수십 KB | ~1 ns (4 cycle) | Hardware |
| L2 Cache | 빠름 | 수백 KB ~ 수 MB | ~5 ns (12 cycle) | Hardware |
| L3 Cache | 보통 | 수 MB ~ 수십 MB | ~20 ns (40 cycle) | Hardware (Core 공유) |
| RAM | 느림 | 수 GB | ~60–100 ns (200+ cycle) | OS Kernel (Page Table) |
| Storage | 매우 느림 | 수백 GB ~ TB | 수십 μs 이상 | OS Kernel (VFS / blk-mq) |

### 계층별 메모리 계층 구조 동작 실체

#### Hardware 계층

- **Cache Controller**: 각 계층 간 데이터 이동을 자동으로 관리. CPU 코어 내부에 하드와이어드(hard-wired)로 구현
- **DRAM 접근 비용**: L3 Miss 후 DRAM 컨트롤러가 Row Activation → Column Select → Data Transfer 순서로 처리. CAS Latency, tRCD, tRP 등 타이밍 파라미터에 의해 지연 결정
- **Memory Bandwidth Saturation**: LLC Miss가 많아지면 DRAM 대역폭 포화 → 모든 코어의 메모리 접근 지연 증가. LPDDR5 기준 이론 대역폭 ~68GB/s이나 실제 유효 대역폭은 훨씬 낮음
- **NUMA(Non-Uniform Memory Access)**: 멀티소켓 서버에서 각 CPU 소켓이 로컬 DRAM을 보유. 원격 소켓의 DRAM 접근 시 QPI/UPI 인터커넥트를 경유 → 지연 2~4배 증가

#### OS Kernel 계층

- **Page Cache**: 파일 I/O 결과를 메모리에 캐싱. VFS 계층이 read() 시스템 콜 수행 시 먼저 Page Cache 탐색. Hit 시 디스크 접근 없이 반환
- **TLB(Translation Lookaside Buffer)**: 가상→물리 주소 변환 캐시. TLB Miss 시 Page Table Walk → L1/L2/L3 또는 RAM에서 Page Table Entry 탐색 → 추가 메모리 접근 발생
- **HugePage / THP**: 일반 4KB 페이지 대신 2MB(또는 1GB) 페이지 사용 시 TLB 엔트리 하나로 더 넓은 메모리 커버 → TLB Miss 감소 → 간접적으로 Cache 효율 향상

#### JVM Runtime 계층

- **GC Heap 레이아웃**: Eden, Survivor, Old Gen 각 영역이 연속 메모리 블록으로 할당됨. 하지만 객체 참조(포인터)는 Heap 전체에 분산되어 Pointer Chasing Cache Miss 유발
- **JIT 코드 Cache**: C1/C2가 컴파일한 네이티브 코드는 Code Cache(별도 Off-Heap 영역)에 저장. Code Cache 부족 시 컴파일된 메서드 Eviction → 재컴파일 또는 인터프리터 실행 전환

#### SRE 관찰 도구

| 도구 | 관찰 대상 |
|------|-----------|
| `numactl --hardware` | NUMA 노드 구성 및 메모리 분배 확인 |
| `numastat -p <pid>` | 프로세스의 로컬/원격 NUMA 접근 비율 |
| `/proc/meminfo`의 `HugePages_*`, `AnonHugePages` | HugePage/THP 활성화 및 사용량 |
| `perf stat -e node-load-misses,node-store-misses` | NUMA Miss (원격 접근) 빈도 |
| `free -m`의 `buff/cache` | Page Cache 점유 현황 |

---

## 3. L1 Cache

L1 Cache(Level 1 Cache)는 CPU Core 내부에 위치하는 가장 빠른 Cache 계층이다.

| 항목 | 설명 |
|------|------|
| 위치 | CPU Core 내부 |
| 속도 | 가장 빠름 (~1ns, ~4 cycle) |
| 용량 | 32KB ~ 128KB 수준 |
| 공유 여부 | Core 전용 (비공유) |
| 구조 | Set-Associative (보통 8-way) |

### L1 Cache 분리 구조

L1 Cache는 용도에 따라 두 영역으로 분리된다.

| 종류 | 역할 | 관련 Pipeline 단계 |
|------|------|-------------------|
| L1 I-Cache (Instruction Cache) | 명령어 저장 | Fetch 단계 |
| L1 D-Cache (Data Cache) | 데이터 저장 | Memory Access 단계 (LSU 경유) |

### 계층별 L1 Cache 동작 실체

#### Hardware 계층

- **Set-Associative 구조**: 메모리 주소를 Set Index(행), Tag(열)로 분해. 동일 Set에 들어오는 여러 주소를 Way 수만큼 저장 가능. 8-way L1이라면 동일 Set에 8개의 Cache Line 동시 보유
- **L1 I-Cache와 Pipeline**: Fetch 단계에서 PC(Program Counter) 주소로 I-Cache 탐색. Miss 시 L2 접근하는 동안 Pipeline이 Stall → Branch Predictor가 예측에 성공해야만 I-Cache를 미리 채울 수 있음
- **L1 D-Cache와 LSU**: Load 명령 실행 시 LSU(Load Store Unit)가 가상 주소로 DTLB 탐색 + L1 D-Cache 탐색을 병렬로 수행(VIPT: Virtually Indexed Physically Tagged). Hit 시 1~4 cycle 내 데이터 반환
- **Write Policy**: Write-Through(즉시 상위 계층 반영)와 Write-Back(Dirty bit 표시 후 지연 반영) 중 L1은 일반적으로 Write-Back 사용 → Cache Eviction 시 Dirty Line을 L2에 기록

#### OS Kernel 계층

- **Context Switch 시 L1 오염**: 스레드 전환 시 새로운 task_struct가 CPU를 점유하면 이전 스레드의 L1 I$/D$ 데이터가 점차 Evict됨. 복귀 시 Cold Miss 발생 → `perf sched latency`로 측정 가능한 실행 지연
- **CPU Affinity**: `taskset` 또는 cgroup `cpuset`으로 스레드를 특정 코어에 고정 시 L1 Warm 상태 유지 → Context Switch로 인한 L1 오염 최소화
- **Kernel 코드 경로**: 시스템 콜 진입(`syscall` / `sysenter`) 시 커널 코드가 L1 I-Cache를 일부 오염시킴. 시스템 콜 빈도가 높은 애플리케이션은 I-Cache 효율 저하 가능

#### JVM Runtime 계층

- **JIT Hot Method 배치**: C2 컴파일러가 자주 호출되는 메서드를 Code Cache 내 연속 주소에 배치하면 I-Cache 적중률 향상. 반대로 Code Cache 단편화가 심하면 메서드 간 점프마다 I-Cache Miss
- **TLAB(Thread Local Allocation Buffer)**: 객체 할당 시 스레드별 전용 TLAB 영역에서 bump-pointer 방식으로 순차 할당. 연속 주소 할당으로 D-Cache 효율 극대화. TLAB 소진 시 Eden 직접 접근 → 경합 및 Cache 오염 가능
- **Safepoint와 I-Cache**: JVM Safepoint Poll은 각 메서드 후단 또는 루프 후단에 삽입됨. Safepoint 체크 코드 자체가 I-Cache 공간을 소비

#### SRE 관찰 도구

| 도구 | 관찰 대상 |
|------|-----------|
| `perf stat -e L1-icache-load-misses` | L1 I-Cache Miss 빈도 |
| `perf stat -e L1-dcache-load-misses,L1-dcache-loads` | L1 D-Cache Miss 및 Hit Ratio |
| `perf record -e L1-dcache-load-misses -g ./app` | 콜스택 기반 L1 D-Cache Miss 핫스팟 |
| `taskset -c <cpu> <command>` | CPU 고정으로 L1 Warm 유지 실험 |
| JVM `-XX:+PrintTLAB` | TLAB 할당 패턴 및 소진 빈도 |

---

## 4. L2 Cache

L2 Cache(Level 2 Cache)는 L1 Cache Miss가 발생했을 때 데이터를 보완하는 중간 계층이다.

| 항목 | 설명 |
|------|------|
| 위치 | CPU Core 내부 또는 인접 영역 |
| 속도 | L1보다 느림 (~5ns, ~12 cycle) |
| 용량 | 256KB ~ 수 MB 수준 |
| 공유 여부 | 일반적으로 Core 전용 |
| 구조 | Set-Associative (보통 4~16-way) |
| 포함 정책 | Inclusive(L1 포함) 또는 Exclusive(L1과 별도) CPU마다 상이 |

### 계층별 L2 Cache 동작 실체

#### Hardware 계층

- **L1 Miss → L2 조회 비용**: L1 Miss 감지 후 L2 접근까지 수 ns 소요. 이 동안 Pipeline이 MEM 단계에서 정지 → Out-of-Order 실행으로 다른 독립 명령어를 계속 실행하여 Stall 은폐 시도
- **MSHR(Miss Status Holding Register)**: L1 Miss가 발생한 주소를 MSHR에 등록하고 L2 접근 응답을 기다림. MSHR 포화(동시 Miss 초과) 시 추가 메모리 접근 자체가 Block됨 → Memory-Level Parallelism 저하
- **Prefetch**: L2 Controller는 접근 패턴을 분석해 Stride Prefetcher, Stream Prefetcher로 미리 L2에 데이터 적재. 순차 배열 접근 시 효과적, 불규칙 접근(Pointer Chasing)에는 비효과적
- **Inclusive vs Exclusive 정책**: Intel은 주로 Inclusive(L2가 L1의 데이터도 포함) 사용 → L2 Eviction 시 L1 해당 Line도 동시 무효화. AMD Zen은 L1/L2 Exclusive 구조로 총 유효 캐시 용량 극대화

#### OS Kernel 계층

- **NUMA와 L2 Cache**: L2는 코어 전용이므로 NUMA 영향을 직접 받지 않음. 하지만 L2 Miss → L3 Miss 후 원격 NUMA 접근 시 지연이 L2 접근 대비 수십 배 증가
- **Huge Page와 L2 효율**: 2MB HugePage 사용 시 Page Table Walk 깊이 감소 → L2에 캐싱된 Page Table Entry 재사용률 향상 (L2는 Page Table Walker도 접근함)
- **IRQ 처리와 L2 오염**: 하드웨어 인터럽트(IRQ) 처리를 위해 커널 IRQ Handler 코드가 실행되면 해당 코어의 L2 I-Cache 일부가 오염됨. 고빈도 IRQ(NIC 패킷 수신 등) 환경에서 유의

#### JVM Runtime 계층

- **객체 크기와 L2 활용**: 평균 Java 객체 크기가 작으면(수십~수백 byte) L2 Cache Line(64byte) 하나에 여러 객체가 겹쳐 담기지 않음 → 객체 배열(Object array) 대신 원시 배열(int[], long[]) 사용 시 L2 활용도 향상
- **Direct Memory / Off-Heap**: `ByteBuffer.allocateDirect()` 또는 `Unsafe.allocateMemory()`로 할당한 Off-Heap 메모리는 JVM GC 대상이 아니며 연속 물리 주소 보장이 더 높음 → L2 Prefetcher가 스트라이드 패턴 학습 가능
- **Serialization / Deserialization 비용**: JSON/Protobuf 역직렬화 시 새 객체 대량 생성 → Eden 영역 빠른 소진 → Minor GC 발생 → L2 D-Cache 오염 (GC 코드 경로 실행)

#### SRE 관찰 도구

| 도구 | 관찰 대상 |
|------|-----------|
| `perf stat -e l2_rqsts.miss,l2_rqsts.references` | L2 Miss 및 전체 L2 요청 수 |
| `perf stat -e cycles:u,instructions:u` | IPC 측정 (L2 Miss 증가 시 IPC 하락 확인) |
| `perf mem record / report` | 메모리 접근 지연 및 Cache 계층별 히트 분포 |
| JVM `-XX:+UseNUMA` | NUMA 인식 Eden 할당 활성화 (L2 Local Miss 감소) |
| `numastat` | NUMA Miss 비율 (L2 → L3 → 원격 NUMA 연계) |

---

## 5. L3 Cache

L3 Cache(Level 3 Cache)는 여러 Core가 공유하는 대용량 Cache 계층이다.

| 항목 | 설명 |
|------|------|
| 위치 | CPU Package 내부 (Ring Bus 또는 Mesh 연결) |
| 속도 | L2보다 느림 (~20ns, ~40 cycle) |
| 용량 | 수 MB ~ 수십 MB 수준 |
| 공유 여부 | 여러 Core 공유 |
| 역할 | Core 간 데이터 공유, RAM 접근 감소, LLC(Last Level Cache) |

### 계층별 L3 Cache 동작 실체

#### Hardware 계층

- **LLC(Last Level Cache)**: L3 Miss가 곧 DRAM 접근을 의미. LLC Miss Rate는 Memory Bandwidth 소비와 직결되며, 포화 시 전체 코어의 메모리 접근 지연 증가
- **Ring Bus vs Mesh 구조**: Intel Skylake 이전은 Ring Bus로 모든 코어가 L3를 공유. Ice Lake 이후는 Mesh 구조로 코어 수 확장성 개선. 원거리 L3 Slice 접근 시 홉(hop) 수만큼 지연 추가
- **Cache Coherency와 L3**: MESI 프로토콜에서 L3는 Directory 역할을 겸함. 코어 A가 Modified 상태로 보유한 Cache Line을 코어 B가 읽으려 하면 L3 Directory가 코어 A에 Flush/Downgrade 요청
- **L3 Thrashing**: 여러 스레드가 서로 다른 대용량 데이터셋을 접근할 때 L3 용량을 초과 → 지속적인 Eviction/Load 반복 → Memory Bandwidth Saturation 유발

#### OS Kernel 계층

- **NUMA와 L3**: 멀티소켓 서버에서 L3는 소켓(NUMA 노드)당 독립적으로 존재. 다른 소켓의 L3 데이터는 QPI/UPI를 통해 접근 → Home Node 개념으로 어느 소켓의 L3가 해당 메모리를 담당하는지 결정
- **Context Switch와 L3**: CFS 스케줄러가 스레드를 다른 코어로 이동시키면 기존 코어의 L1/L2 데이터는 사라지지만 L3는 유지될 수 있음 (L3 공유 구조에서). `perf sched migrate` 이벤트로 스레드 이동 추적
- **IRQ Affinity**: `/proc/irq/<N>/smp_affinity`로 특정 코어에 IRQ 고정 시 해당 코어의 L3 사용 패턴 분리 가능. NIC SoftIRQ와 애플리케이션 스레드를 다른 L3 Slice에 배치하여 오염 방지

#### JVM Runtime 계층

- **G1 GC의 Region과 L3**: G1 GC는 Heap을 동일 크기 Region으로 분할. Region 크기가 L3보다 작으면 GC 수행 중 Region 스캔이 L3 Hit 가능성 높음. Eden Region이 L3 용량을 초과하면 Minor GC 시 LLC Miss 폭증
- **JNI Critical Section과 L3**: JNI 호출로 네이티브 라이브러리(예: 암호화, 압축)가 실행되면 해당 코드와 데이터가 L3를 점유 → Java Heap 접근 시 L3 Competition 증가
- **Off-CPU Time과 L3**: 스레드가 I/O 대기(TASK_INTERRUPTIBLE)로 전환되면 Off-CPU 상태가 됨. 재개 시 L3 데이터가 다른 스레드에 의해 Evict되어 있을 가능성 → Warm-Up 비용 발생

#### SRE 관찰 도구

| 도구 | 관찰 대상 |
|------|-----------|
| `perf stat -e LLC-load-misses,LLC-loads` | L3 Miss 비율 (LLC Hit Ratio 계산) |
| `perf stat -e LLC-store-misses` | L3 Write Miss (Write-Back Eviction 빈도) |
| `perf c2c record / report` | 코어 간 Cache Line 공유 패턴 및 False Sharing |
| `intel_cmt` (Intel CMT/RDT) | 코어/프로세스별 L3 점유량 측정 |
| `resctrl` (`/sys/fs/resctrl`) | Intel RDT로 프로세스별 L3 할당량 제어 |

---

## 6. Cache 접근 흐름

CPU가 데이터를 읽을 때 다음 순서로 탐색한다.

```
L1 Cache 탐색 (~1ns)
  → Hit: 즉시 반환 (Pipeline 유지)
  → Miss:
      L2 Cache 탐색 (~5ns)
        → Hit: 반환 후 L1에 적재 (L1 Fill)
        → Miss:
            L3 Cache 탐색 (~20ns)
              → Hit: 반환 후 L1/L2에 적재 (Cache Fill)
              → Miss:
                  DRAM 접근 (~100ns, NUMA 원격 시 ~200ns+)
                  → 반환 후 L3 → L2 → L1 순으로 적재
                  → Page Fault 시 OS Kernel 개입 필요
```

### 계층별 Cache 접근 흐름 동작 실체

#### Hardware 계층

- **Parallel Lookup**: 일부 CPU는 L1/L2를 병렬로 탐색해 지연 감소. VIPT(Virtually Indexed Physically Tagged) 구조로 TLB 변환과 Cache Index 계산을 동시 수행
- **Critical Word First**: DRAM에서 Cache Line 전체(64byte)를 채우기 전에 요청된 특정 워드를 먼저 CPU에 전달 → Pipeline Stall 최소화
- **Write Buffer**: Store 명령 실행 시 결과를 Write Buffer에 임시 저장 후 비동기로 Cache에 기록. Load가 Write Buffer를 참조(Store-to-Load Forwarding)하면 Cache Miss 없이 최신 값 획득 가능

#### OS Kernel 계층

- **Major Page Fault**: 요청 가상 주소에 대한 물리 페이지가 없거나 Swap Out된 경우 발생. `do_page_fault()` 커널 함수가 호출되어 물리 페이지 할당 + Page Table Entry 갱신 + TLB 업데이트 수행. Disk I/O가 필요한 경우 blk-mq 큐에 I/O 요청 삽입
- **Minor Page Fault**: 물리 페이지는 존재하지만 Page Table 매핑만 없는 경우. 디스크 접근 없이 Page Table Entry만 갱신. mmap, CoW(Copy-on-Write) Fork 시 빈번 발생
- **Transparent HugePage(THP)**: 커널이 자동으로 4KB 연속 페이지를 2MB HugePage로 승격. TLB Coverage 향상으로 Page Walk 빈도 감소 → L1/L2 D-Cache가 데이터 접근에 더 집중 가능

#### JVM Runtime 계층

- **JIT와 Cache 접근 최적화**: C2 컴파일러는 Loop Unrolling, Auto-Vectorization으로 반복 접근 패턴을 최적화. 배열 접근 시 경계 검사(Bounds Check) 제거(BCE)로 불필요한 분기 제거
- **GC와 Cache Fill**: Minor GC(Young Generation 수집) 수행 시 살아있는 객체를 Survivor 또는 Old Gen으로 복사 → 대량 Cache Line Fill 발생 → 기존 Hot Data Eviction 위험

#### SRE 관찰 도구

| 도구 | 관찰 대상 |
|------|-----------|
| `vmstat 1`의 `pgfault` / `pgmajfault` | Minor / Major Page Fault 빈도 |
| `perf stat -e dTLB-load-misses,iTLB-load-misses` | TLB Miss (D-Cache/I-Cache 접근 전 지연) |
| `perf mem record` | 메모리 접근 계층별(L1/L2/L3/RAM) 히트 분포 |
| `/proc/vmstat`의 `pgmajfault` | 시스템 전체 Major Fault 누적 |
| `cat /sys/kernel/mm/transparent_hugepage/enabled` | THP 활성화 여부 |

---

## 7. Cache Hit와 Cache Miss

### Cache Hit

요청한 데이터가 Cache에 존재하는 경우다. 즉시 데이터를 반환하며 Pipeline이 중단되지 않는다.

```
CPU 데이터 요청 → L1 Hit → 즉시 반환 (~1ns)
                             → Pipeline MEM 단계 완료
                             → 다음 명령어 Execute 지속
```

### Cache Miss

요청한 데이터가 Cache에 존재하지 않는 경우다. 하위 계층을 순차적으로 탐색하며, 최종적으로 RAM에 접근하면 높은 지연이 발생한다.

```
L1 Miss → L2 Miss → L3 Miss → DRAM 접근 → 높은 지연 발생
(~4 cycle)  (~12 cycle)  (~40 cycle)   (~200+ cycle)
```

### 계층별 Cache Miss의 동작 실체

#### Hardware 계층

- **Compulsory Miss(Cold Miss)**: 해당 Cache Line이 한 번도 적재된 적 없는 경우. 프로세스 시작, JVM Warm-Up 전, GC 후 첫 접근 시 발생
- **Capacity Miss**: Cache 용량이 작업 집합(Working Set) 크기보다 작아 반복 Eviction이 발생. L3 크기보다 큰 배열을 반복 순회하는 경우 전형적 사례
- **Conflict Miss**: Set-Associative에서 같은 Set에 매핑되는 주소가 Way 수를 초과할 때 발생. 특정 보폭(Stride)으로 접근하면 의도치 않게 동일 Set에 집중될 수 있음
- **CPU Pipeline Stall**: L1 Miss → Pipeline의 MEM 단계에서 데이터 대기. Out-of-Order 실행으로 독립 명령어를 계속 실행하여 숨기지만, 의존성 있는 명령어가 대기하면 Stall 불가피

#### OS Kernel 계층

- **Page Fault로 인한 Hard Miss**: Cache Miss가 Page Fault로 이어지면 커널 진입 → 물리 메모리 할당 → DMA(Disk) 접근까지 발생 가능. 수십 μs 이상 지연으로 순수 Cache Miss 비용과 수백 배 차이
- **NUMA Remote Miss**: L3 Miss 후 원격 NUMA 노드의 DRAM에 접근 시 Home Node Controller 경유. `numastat`으로 `numa_miss` 카운터 확인 가능
- **OOM Killer 유발 경로**: 반복적인 Page Fault가 물리 메모리 소진으로 이어지면 `oom_score` 기반으로 OOM Killer 발동 → 프로세스 강제 종료

#### JVM Runtime 계층

- **GC Compaction과 Cache Miss**: Old Gen Full GC 수행 시 Compaction(객체 이동)으로 모든 참조가 새 주소로 변경. 기존 Cache에 있던 모든 Hot Object 주소가 무효화 → 대규모 Cache Miss 발생
- **Finalization Queue**: Finalizer 보유 객체는 GC 후 즉시 수집되지 않고 `Finalizer` 스레드가 처리할 때까지 메모리 잔류. 이 객체들이 불필요하게 Cache를 점유

#### SRE 관찰 도구

| 도구 | 관찰 대상 |
|------|-----------|
| `perf stat -e cache-misses,cache-references -p <pid>` | 애플리케이션별 Cache Miss 비율 |
| `perf stat -e mem-load-retired.l1_miss,mem-load-retired.l2_miss,mem-load-retired.l3_miss` | 계층별 Miss 분포 |
| `eBPF hardirqs-bpfcc` | Hard Miss(Page Fault) 로 인한 지연 측정 |
| JVM `-Xlog:gc*` | GC Compaction 빈도 및 Duration |
| `/proc/pressure/memory` (PSI) | 메모리 Miss 누적으로 인한 시스템 압박 지표 |

---

## 8. Cache Miss와 CPU Stall

Cache Miss가 발생하면 CPU는 데이터를 받을 때까지 대기해야 한다.

```
Cache Miss
  → Memory Wait 발생
  → Pipeline Stall (MEM 단계 정지)
  → CPU 실행 효율 저하
  → IPC(Instructions Per Cycle) 감소
```

Cache Miss 빈도가 높을수록 IPC(Instructions Per Cycle)가 감소한다.

### 계층별 Cache Miss와 CPU Stall 동작 실체

#### Hardware 계층

- **Out-of-Order 실행의 한계**: ROB(Reorder Buffer)에 적재된 독립 명령어는 Cache Miss 중에도 실행 가능. 하지만 ROB가 Cache Miss를 기다리는 명령어로 채워지면(`ROB Full Stall`) 새 명령어 Fetch 자체가 중단됨
- **Memory Stall Cycles**: `perf stat`의 `cycle_activity.stalls_mem_any` 이벤트로 측정. 전체 사이클 대비 비율이 높으면 Memory-Bound 애플리케이션으로 분류
- **Hardware Prefetch의 효과**: Stream Prefetcher, Stride Prefetcher, 그리고 소프트웨어 `PREFETCHT0` 명령어로 Cache Miss를 예측하여 미리 데이터를 채움. 불규칙 접근 패턴에는 효과 없음
- **CPU Frequency Scaling(P-state)**: Cache Miss로 인한 메모리 대기 중 CPU는 낮은 P-state(낮은 주파수)로 전환하지 않음 (대기 중에도 Clock 구동). 반면 C-state(코어 유휴)는 Cache Miss Stall만으로는 진입하지 않음

#### OS Kernel 계층

- **Off-CPU Time**: Cache Miss가 Page Fault를 유발하면 스레드가 `TASK_UNINTERRUPTIBLE` 상태로 전환되어 CPU를 반환. 이 Off-CPU 시간은 `perf sched latency` 또는 eBPF off-cpu 분석으로 측정
- **cgroup CPU Throttling 연쇄**: Cache Miss Stall이 많은 스레드는 같은 CPU Time을 소비하면서 실제 유용한 작업 처리량이 낮음. 이로 인해 cgroup CPU Quota가 소진되면 Throttling → 응답 지연 악화

#### JVM Runtime 계층

- **JIT C2와 Memory Stall**: C2 JIT 최적화 후에도 Memory-Bound 병목이 남아 있으면 JIT 재컴파일이 성능을 개선하지 못함. `perf stat`으로 `stalls_mem_any` 비율 확인 후 자료구조 변경이 필요한 경우를 식별해야 함
- **Safepoint와 Stall**: GC Safepoint 도달 요청(`VM_Operation`) 시 Cache Miss가 많은 스레드는 Safepoint Poll 코드에 도달하는 시간이 길어짐 → STW 지연 증가

#### SRE 관찰 도구

| 도구 | 관찰 대상 |
|------|-----------|
| `perf stat -e cycle_activity.stalls_mem_any` | 메모리 대기로 인한 CPU Stall 비율 |
| `toplev.py --level 2` | Memory Bound vs Core Bound 분류 |
| `perf stat -e instructions,cpu-cycles` | IPC = instructions / cpu-cycles |
| `eBPF offcputime-bpfcc -p <pid>` | Page Fault 기반 Off-CPU Time 분포 |
| JVM `-XX:+PrintSafepointStatistics` | Safepoint 도달 지연 (Cache Miss 연관) |

---

## 9. Cache Line

CPU는 데이터를 개별 바이트 단위가 아닌 **Cache Line** 단위로 Cache에 적재한다.

- **일반적인 크기**: 64 Bytes (x86-64 표준)
- CPU가 특정 데이터에 접근하면, 해당 데이터를 포함한 64 Bytes 블록 전체를 Cache에 올린다.
- 이는 Spatial Locality를 활용하여 다음 접근 시 Cache Hit 가능성을 높인다.

### 계층별 Cache Line 동작 실체

#### Hardware 계층

- **Cache Line 정렬**: 64byte 정렬된 주소로 접근하면 단일 Cache Line으로 커버. 비정렬 접근 시 2개 Cache Line에 걸쳐 2회 접근 발생 (Cache Line Split)
- **Cache Line 상태 전환(MESI)**: Modified(쓰기 완료, Dirty) → 다른 코어 접근 시 Invalid로 전환 후 Write-Back. Exclusive(단독 소유, Clean) → 공유 요청 시 Shared로 전환. Shared(읽기 공유) → 쓰기 요청 시 RFO(Request For Ownership) 발송 후 Invalid로 전환
- **Cache Line Eviction Policy**: LRU(Least Recently Used) 근사 알고리즘으로 교체 대상 선정. 일부 CPU는 Random Replacement 사용. 교체 시 Dirty Line은 L2/L3에 Write-Back

#### OS Kernel 계층

- **Cache Line과 `struct` 정렬**: 커널 소스에서 `____cacheline_aligned` 매크로로 중요 자료구조를 Cache Line 경계에 정렬. `task_struct`, `kmem_cache` 등 핫 경로 자료구조에 적용
- **DMA와 Cache Coherency**: NIC/SSD의 DMA가 메모리를 직접 수정하면 CPU Cache의 해당 Line이 Stale 상태가 됨. IOMMU/Cache Flush 명령으로 일관성 유지. DMA-Coherent 매핑은 Cache Flush를 자동화

#### JVM Runtime 계층

- **`@Contended` 어노테이션**: JDK 8+에서 `sun.misc.Contended` 어노테이션이 붙은 필드는 JVM이 자동으로 128byte Padding을 삽입하여 별도 Cache Line에 배치. `ForkJoinPool`, `ConcurrentHashMap`의 Counter에 사용
- **객체 헤더와 Cache Line**: Java 객체는 최소 16byte(헤더 8byte + 최소 데이터)를 차지. 작은 객체 배열(int[], byte[])은 Cache Line 당 최대 64byte 유효 데이터 패킹 가능. 박싱된 배열(Integer[])은 참조 포인터만 배치되어 실제 데이터 접근 시 추가 Cache Miss

#### SRE 관찰 도구

| 도구 | 관찰 대상 |
|------|-----------|
| `perf c2c record -g -p <pid>` | Cache Line 단위 공유 패턴 및 False Sharing |
| `pahole -c <binary>` | 구조체 레이아웃 및 Padding 분석 |
| `valgrind --tool=cachegrind --cache-sim=yes` | Cache Line 단위 Miss 시뮬레이션 |
| JVM `-XX:ContendedPaddingWidth=128` | @Contended Padding 크기 조정 |

---

## 10. Locality (지역성)

### Spatial Locality (공간적 지역성)

인접한 메모리 주소의 데이터가 연속적으로 사용될 가능성이 높다는 특성이다.

```java
int[] arr = new int[1000];
// 배열 요소는 메모리에 연속 배치됨
// → Cache Line 단위 적재 시 인접 요소도 함께 올라옴
// → 순차 접근 시 Cache Hit율 높음
```

### Temporal Locality (시간적 지역성)

최근에 사용된 데이터가 다시 사용될 가능성이 높다는 특성이다.

```java
for (int i = 0; i < 1000; i++) {
    sum += value;
    // value는 반복적으로 접근됨
    // → Cache에 계속 유지될 가능성 높음
}
```

### 계층별 Locality 동작 실체

#### Hardware 계층

- **Spatial Locality와 Prefetcher**: 연속 배열 접근 시 Hardware Prefetcher가 다음 Cache Line을 자동 선반입. `__builtin_prefetch()`(GCC) 또는 `PREFETCHT0`(x86) 명령어로 소프트웨어 힌트 제공 가능
- **Temporal Locality와 LRU**: 자주 접근하는 Cache Line은 LRU 리스트 상위에 유지되어 Eviction 대상에서 제외. Hot Loop 내 변수가 Register Allocation 되지 않더라도 L1에 상주
- **Working Set 크기**: 애플리케이션의 실질적인 활성 데이터 집합이 L3 용량을 초과하면 Temporal Locality 효과가 소멸. 워킹셋 크기는 `perf stat -e LLC-load-misses` 비율로 간접 추정

#### OS Kernel 계층

- **Page Cache의 Temporal Locality**: 동일 파일을 반복 읽을 때 Kernel Page Cache가 Temporal Locality를 OS 수준에서 구현. `fadvise(POSIX_FADV_SEQUENTIAL)`로 커널에 순차 접근 힌트 제공 → Read-Ahead(Prefetch) 활성화
- **Scheduler Affinity와 Locality**: CFS가 스레드를 같은 코어에 재스케줄링하면 L1/L2 Temporal Locality 보존. `sched_setaffinity()` 또는 `numactl --cpunodebind`로 Affinity 고정 가능

#### JVM Runtime 계층

- **Object Allocation Order**: JVM은 Eden 영역에 순서대로 객체를 할당(Bump-Pointer). 동시에 생성된 객체들은 메모리상 인접하여 Spatial Locality 자연 형성. GC 이후 복사 과정에서 재배치되면 이 Locality 파괴
- **JIT Loop 최적화**: C2는 루프 내 배열 접근을 Loop Vectorization(SIMD)으로 변환 시 연속 메모리 접근 패턴을 컴파일러 수준에서 보장 → Spatial Locality 극대화

#### SRE 관찰 도구

| 도구 | 관찰 대상 |
|------|-----------|
| `perf stat -e mem-load-retired.l1_hit,mem-load-retired.l2_hit,mem-load-retired.l3_hit` | 계층별 Hit 분포 (Temporal Locality 효과 측정) |
| `dd iflag=nocache` / `fadvise` | Page Cache 강제 우회로 Baseline 측정 |
| JVM `-XX:+PrintGCDetails` | GC 후 객체 재배치로 인한 Locality 파괴 시점 |
| `perf record -e mem-loads --data` | 메모리 접근 주소 분포 시각화 |

---

## 11. Cache-Friendly vs Cache-Unfriendly 구조

### Cache-Friendly 구조

연속된 메모리 배치를 사용하는 구조는 Spatial Locality를 활용할 수 있다.

```java
int[] array    // 연속 메모리 배치 → Cache 효율 높음
long[] array
ByteBuffer     // Direct: Off-Heap 연속 메모리
```

### Cache-Unfriendly 구조 (Pointer Chasing 문제)

포인터 기반 구조는 각 노드가 메모리의 임의 위치에 분산되어 Spatial Locality를 활용하기 어렵다.

```java
LinkedList<Node>  // 각 Node가 분산된 메모리 주소에 위치
// Node → Node → Node (각 접근마다 Cache Miss 가능성)
```

### 계층별 Cache-Friendly/Unfriendly 동작 실체

#### Hardware 계층

- **AoS(Array of Structs) vs SoA(Struct of Arrays)**: AoS는 객체별로 모든 필드를 인접 배치. SoA는 동일 필드를 배열로 집합. SIMD 처리나 특정 필드만 접근하는 경우 SoA가 Cache 효율 우월
- **Pointer Chasing의 하드웨어 한계**: `Node → Node` 형태의 연결 리스트 순회는 다음 주소를 현재 접근 후에야 알 수 있어 Hardware Prefetcher가 패턴 학습 불가 → 매 노드 접근마다 Cache Miss 가능성
- **Memory Bandwidth와 비연속 접근**: 캐시 미스가 많은 비연속 접근은 DRAM의 Random Access 패턴을 유발 → Row Buffer Hit 감소 → 유효 메모리 대역폭 급락

#### OS Kernel 계층

- **Huge Page와 연속 메모리**: 2MB HugePage는 물리 메모리 연속성을 강화. THP 활성화 시 커널이 자동으로 연속 4KB 페이지를 2MB HugePage로 승격 → Prefetcher의 연속 접근 패턴 인식 향상
- **NUMA-aware 할당**: `numactl --localalloc` 또는 `mbind()`로 메모리를 현재 실행 코어와 동일 NUMA 노드에 할당 → 연속 접근 시에도 NUMA 원격 접근 방지

#### JVM Runtime 계층

- **Direct Memory / Off-Heap**: `ByteBuffer.allocateDirect()` 사용 시 GC Heap 외부의 연속 메모리 할당. GC에 의한 재배치가 없어 Spatial Locality 장기 유지. Netty의 `PooledByteBufAllocator`, Kafka의 Log Segment에서 활용
- **Value Types (Project Valhalla)**: JDK 미래 버전에서 Value Type 도입 시 객체 참조 대신 값을 배열에 직접 인라인 배치 가능 → Pointer Chasing 제거 → Cache 효율 근본 개선
- **ClassLoader Leak**: 동적으로 클래스를 로드하는 프레임워크(JSP, 플러그인 시스템)에서 ClassLoader가 해제되지 않으면 관련 클래스의 Code Cache 메모리 누수 → Code Cache 압박 → JIT 컴파일 메서드 Eviction

#### SRE 관찰 도구

| 도구 | 관찰 대상 |
|------|-----------|
| `perf mem record` + `perf mem report` | 메모리 접근 패턴 분포 (연속 vs 비연속) |
| `async-profiler -e mem` | 애플리케이션 메모리 접근 핫스팟 |
| JVM `-XX:+UseDirectByteBuffer` (설계 선택) | Off-Heap 사용 여부 확인 |
| `jmap -histo <pid>` | Heap 객체 분포 및 LinkedList 등 비효율 자료구조 탐지 |
| `cat /sys/kernel/mm/transparent_hugepage/defrag` | THP Defrag 정책 확인 |

---

## 12. Cache Pollution

불필요한 데이터가 Cache를 점유하여 실제 필요한 데이터가 교체(Evict)되는 현상이다.

```
불필요한 데이터 Cache 적재
  → 유용한 데이터 Evict (LRU 교체)
  → 이후 해당 데이터 접근 시 Cache Miss 발생
  → Cache 효율 저하 → IPC 하락 → Throughput 감소
```

잘못된 분기 예측(Speculative Execution)으로 인해 실행되지 않을 코드 경로의 데이터가 Cache에 올라오는 경우가 대표적인 원인 중 하나다.

### 계층별 Cache Pollution 동작 실체

#### Hardware 계층

- **Branch Misprediction과 I-Cache Pollution**: 분기 예측 실패로 잘못된 경로의 명령어가 L1 I-Cache에 적재됨. Pipeline Flush 후 올바른 경로의 명령어가 로드되면 Cold Miss 발생
- **Speculative Load와 D-Cache Pollution**: LSU가 투기적으로 실행한 Load 명령이 잘못된 주소의 데이터를 L1 D-Cache에 적재. ROB Flush 이후 해당 데이터는 불필요하지만 Cache에 잔류하여 유용한 데이터 Evict
- **비스트리밍 접근의 Pollution**: `_mm_stream_*` (Non-Temporal Store) 명령어를 사용하지 않는 대용량 데이터 쓰기는 사용 후 버릴 데이터를 Cache에 불필요하게 채움. NT Store로 Cache Bypass 가능

#### OS Kernel 계층

- **IRQ Handler와 I-Cache Pollution**: 고빈도 인터럽트(고속 NIC, 타이머) 처리를 위해 IRQ Handler 코드가 반복 실행되면 L1 I-Cache의 애플리케이션 코드 영역을 Evict. IRQ Affinity로 특정 코어 격리 가능
- **SoftIRQ와 L3 Pollution**: `ksoftirqd` 스레드가 TCP/IP 스택 처리 시 sk_buff 버퍼와 프로토콜 처리 코드가 L3를 점유. RPS(Receive Packet Steering) / RFS(Receive Flow Steering)로 애플리케이션과 동일 코어에서 처리 시 오히려 D-Cache Locality 향상 가능
- **Page Cache Eviction**: `drop_caches`(echo 3 > /proc/sys/vm/drop_caches) 또는 메모리 압박으로 Page Cache가 교체될 때 다음 파일 접근 시 Cold Miss 폭증

#### JVM Runtime 계층

- **GC와 Cache Pollution**: Minor/Full GC 수행 시 GC 스레드가 Heap 전체를 스캔하며 Large Capacity Cache Pollution 유발. G1 GC는 Region 단위로 수행 범위를 제한하여 Pollution 완화
- **Reflection / Dynamic Proxy**: `java.lang.reflect.*` 또는 런타임 바이트코드 생성(cglib, ByteBuddy)은 많은 클래스 메타데이터와 코드를 Code Cache에 적재 → 기존 Hot Method Eviction 가속
- **Deserialization 폭발**: 대용량 JSON/XML 역직렬화는 다수의 단명(Short-lived) 객체를 생성 → Eden 빠른 소진 → Minor GC → GC 코드가 L1/L2 오염

#### SRE 관찰 도구

| 도구 | 관찰 대상 |
|------|-----------|
| `perf stat -e branch-misses` | 분기 예측 실패 → I-Cache Pollution 연관성 |
| `mpstat -P ALL 1`의 `%irq`, `%soft` | IRQ/SoftIRQ로 인한 Cache Pollution 원인 |
| `/proc/net/softnet_stat` | SoftIRQ 처리 통계 (RPS/RFS 설정 근거) |
| JVM `-Xlog:gc*:file=gc.log` | GC 수행 빈도 및 Duration (Cache Pollution 주기) |
| `cat /proc/sys/vm/drop_caches` 전후 `perf stat` | Page Cache Eviction 영향 측정 |

---

## 13. Cache Coherency

멀티코어 환경에서 여러 Core가 동일 데이터를 각자의 L1/L2 Cache에 저장할 경우, 데이터 일관성 문제가 발생할 수 있다. CPU는 Cache Coherency 프로토콜을 통해 이를 관리한다.

| 프로토콜 | 설명 | 사용 CPU |
|----------|------|----------|
| MESI | Modified / Exclusive / Shared / Invalid 4상태 관리 | Intel (대부분) |
| MOESI | MESI에 Owned 상태를 추가 (Write-Back 최적화) | AMD |
| MESIF | MESI에 Forward 상태 추가 (공유 데이터 전달 최적화) | Intel QPI |

### 계층별 Cache Coherency 동작 실체

#### Hardware 계층

- **RFO(Request For Ownership)**: 코어가 Shared 상태 Cache Line을 수정하려 할 때 다른 모든 코어에 Invalid 요청 발송. 응답을 받은 후에야 Modified 상태로 전환하여 쓰기 수행. 고빈도 공유 데이터 쓰기 시 RFO Storm 발생
- **Snoop Traffic**: MESI 프로토콜의 Invalidation 메시지가 Ring Bus/Mesh를 통해 모든 코어에 전파. 코어 수 증가 시 Snoop Traffic 기하급수적 증가 → Bus Bandwidth Saturation 위험
- **Directory-based Coherency**: 코어 수가 많은 서버 CPU는 L3를 Directory로 활용. 각 Cache Line의 소유 코어 목록을 추적하여 Invalidation 대상을 선별 → Broadcast 방식 대비 트래픽 감소

#### OS Kernel 계층

- **Kernel Spinlock과 Coherency**: 커널의 Spinlock은 내부적으로 `LOCK XCHG` 또는 `LOCK CMPXCHG` 명령으로 구현. 이 명령이 실행될 때마다 해당 메모리의 Cache Line이 Modified 상태로 전환 → 다른 코어의 해당 Line Invalid화
- **Per-CPU 변수(`DEFINE_PER_CPU`)**: 커널은 각 코어 전용 변수를 별도 메모리 영역에 배치하여 Cache Coherency 트래픽 제거. 이 패턴을 애플리케이션의 ThreadLocal, Striped64 등에 유사하게 적용 가능
- **NUMA와 Coherency Domain**: NUMA 노드 간 Cache Coherency는 QPI/UPI 링크를 통해 유지. 원격 노드의 Cache Coherency 메시지는 로컬 대비 수배 지연 → NUMA-aware 메모리 할당으로 Coherency 트래픽 로컬화

#### JVM Runtime 계층

- **`volatile` 키워드와 Cache Flush**: JMM에서 `volatile` 쓰기는 `SFENCE` 또는 `MFENCE` 명령으로 컴파일되어 Store Buffer를 비우고 다른 코어에 최신 값 가시성 보장. 읽기는 `LFENCE`로 Load Buffer 비움
- **`AtomicLong` / `AtomicReference`**: `compareAndSet()` 내부에서 `LOCK CMPXCHG` 실행. 성공 시 해당 Cache Line Modified 상태 전환 → 경합 스레드들의 Cache Line Invalid화 연쇄
- **`LongAdder` / `Striped64`**: JDK 8+ `LongAdder`는 Thread별 Cell 배열로 쓰기를 분산. 각 Cell이 별도 Cache Line에 배치(Padding)되어 `AtomicLong` 대비 Cache Coherency 트래픽 대폭 감소

#### SRE 관찰 도구

| 도구 | 관찰 대상 |
|------|-----------|
| `perf c2c report` | Cache Line 단위 공유 패턴, RFO 발생 지점 |
| `perf stat -e cache-misses,bus-cycles` | Snoop Traffic 및 Bus 포화 징후 |
| `likwid-perfctr -g CACHE` | Cache Coherency 관련 하드웨어 카운터 상세 |
| JVM `-Djdk.internal.vm.ci.common.JVMCIRuntime.trace` | volatile/atomic 연산 패턴 추적 |

---

## 14. False Sharing

서로 다른 Thread가 **논리적으로 독립된 데이터**를 수정하더라도, 두 데이터가 **같은 Cache Line**에 위치하면 Cache Invalidation이 반복적으로 발생한다.

```
Thread A → counter1 수정 (Cache Line X의 byte 0~3)
Thread B → counter2 수정 (Cache Line X의 byte 4~7)

→ Thread A 수정 → Cache Line X 전체 Modified → Thread B의 Cache Line X Invalid화
→ Thread B 재로드 → 수정 → Thread A의 Cache Line X Invalid화
→ 반복적인 RFO + Cache Invalidation → Cache Line Thrashing
→ 성능 저하 (단일 스레드 대비 수배 느려질 수 있음)
```

### False Sharing 방지 방법

- 두 데이터를 서로 다른 Cache Line에 위치하도록 **Padding** 추가
- `@Contended` 어노테이션 활용 (Java)

### 계층별 False Sharing 동작 실체

#### Hardware 계층

- **Cache Line Thrashing의 실체**: False Sharing이 발생하면 해당 Cache Line이 Modified → Invalid → Modified를 밀리초 단위로 반복. 이 동안 해당 Cache Line에 대한 모든 Load가 최신 값을 얻기 위해 대기 → Memory Latency 수준으로 저하
- **MESIF Forward 상태**: Intel MESIF에서 Forward 상태의 코어가 다른 코어의 읽기 요청에 직접 데이터를 공급. False Sharing 환경에서 Forward 전환이 잦아져 Core-to-Core Latency 증가
- **Hyper-Threading**: 동일 물리 코어의 두 논리 스레드는 L1/L2 Cache를 완전 공유. False Sharing이 발생한 두 스레드가 동일 물리 코어에 배치되면 Core-to-Core 전송 없이 L1 Cache 수준에서 Invalidation 반복 → 특히 빠른 Thrashing 발생

#### OS Kernel 계층

- **`struct` 레이아웃과 False Sharing**: 커널의 `task_struct`, `kmem_cache_cpu` 등은 `____cacheline_aligned_in_smp` 매크로로 SMP 환경에서 Cache Line 정렬 강제. 애플리케이션 C/C++ 코드에서도 `__attribute__((aligned(64)))` 적용 권장
- **Lock-Free 자료구조의 함정**: Spinlock을 제거하고 원자적 연산으로 대체했더라도 동일 Cache Line에 여러 카운터가 배치되면 False Sharing으로 인해 성능이 Spinlock보다 나빠질 수 있음

#### JVM Runtime 계층

- **`@Contended` 내부 동작**: `-XX:-RestrictContended` 옵션 활성화 시 `sun.misc.Contended` 필드 앞뒤에 `-XX:ContendedPaddingWidth`(기본 128byte) 크기의 Padding이 삽입됨. 이는 Cache Line 크기(64byte)의 2배로 설정되어 Adjacent Sector Prefetch까지 고려
- **`ThreadLocalRandom`의 설계**: JDK 내부적으로 False Sharing을 피하기 위해 `ThreadLocalRandom`의 시드 필드를 `@Contended`로 선언. 멀티스레드 랜덤 생성 시 Cache Line Thrashing 없이 각 스레드가 독립적으로 동작

#### SRE 관찰 도구

| 도구 | 관찰 대상 |
|------|-----------|
| `perf c2c record -g -p <pid>` + `perf c2c report` | False Sharing 발생 주소 및 콜스택 |
| `perf stat -e mem-load-retired.l1_miss --per-thread` | 스레드별 L1 Miss 비율 불균형 (False Sharing 징후) |
| Intel VTune `Memory Access` 분석 | Cache Line 충돌 핫스팟 시각화 |
| JVM `-XX:+RestrictContended=false` | @Contended 동작 활성화 확인 |

---

## 15. CPU 내부 구성 요소와 Cache의 관계

### LSU와 Cache

LSU(Load Store Unit)는 메모리 접근 시 Cache를 우선 조회한다.

```
LSU 메모리 접근 요청
  → DTLB 조회 (가상→물리 주소 변환)
  ↓ TLB Miss 시 → Page Table Walk (L2/L3에서 PTE 탐색)
  → L1 D-Cache 조회
  → L2 Cache 조회
  → L3 Cache 조회
  → RAM 접근 (NUMA Local → Remote 순)
```

### Pipeline과 Cache

Pipeline의 MEM(Memory Access) 단계는 Cache 접근 결과에 직접 의존한다.

```
Cache Hit  → MEM 단계 ~4 cycle 완료 → Pipeline 유지 → 고 IPC 유지
Cache Miss → MEM 단계 정지 (~40~200+ cycle) → ROB 소진 → Pipeline Stall
```

### Branch Predictor와 Cache

잘못된 분기 예측은 실제로 실행되지 않을 경로의 데이터를 Cache에 올려 Cache Pollution을 유발한다.

```
잘못된 Speculative Execution
  → 불필요한 명령어 L1 I-Cache 적재 (I-Cache Pollution)
  → LSU가 투기적으로 불필요한 데이터 L1 D-Cache 적재 (D-Cache Pollution)
  → ROB Flush → 올바른 경로의 명령어/데이터 재로드 → Cold Miss 발생
```

### 계층별 CPU 구성 요소와 Cache 관계

#### Hardware 계층

- **ROB(Reorder Buffer)와 Cache**: ROB에 적재된 Load 명령이 Cache Miss를 기다리는 동안 ROB가 포화되면(`ROB Full Stall`) 새 명령어 Fetch가 중단됨. ROB 크기(Intel: ~352 entry, AMD: ~256 entry)가 메모리 지연을 허용하는 깊이를 결정
- **Store Buffer와 Cache**: Store 명령은 즉시 Cache를 수정하지 않고 Store Buffer에 적재. 이후 Retirement 시 Cache에 Commit. Load가 동일 주소를 Store Buffer에서 읽는 Store-to-Load Forwarding이 성공하면 Cache Miss 없이 최신 값 획득
- **DTLB와 L1 D-Cache 병렬 조회**: VIPT 구조에서 Page Offset(하위 비트)으로 Cache Set을 인덱싱하고, 동시에 TLB에서 물리 주소 상위 비트(Tag)를 얻어 Cache Line의 Tag와 비교. TLB Miss가 없는 한 Cache 접근 지연에 TLB 지연이 추가되지 않음

#### OS Kernel 계층

- **Context Switch 비용의 실체**: `schedule()` 함수 실행 시 현재 task_struct의 레지스터 저장, 새 task_struct 레지스터 복원, 그리고 `CR3` 레지스터 변경(프로세스 전환 시) → TLB Flush 발생. L1/L2는 점진적으로 오염, TLB는 즉시 무효화
- **Kernel Bypass(DPDK, RDMA)**: 고성능 네트워킹에서 NIC 드라이버를 User Space로 이동하여 IRQ, Context Switch, Cache Pollution 없이 직접 패킷 처리. NIC Rx Ring Buffer를 User Space에서 직접 폴링

#### JVM Runtime 계층

- **JIT C1/C2와 Cache 관계**: C1(빠른 컴파일, 낮은 최적화)으로 컴파일된 코드는 레지스터 활용이 낮아 메모리 접근이 많음 → Cache 의존도 높음. C2(느린 컴파일, 높은 최적화)는 Register Allocation, Loop Unrolling으로 Cache Miss 줄임
- **Safepoint와 Cache**: STW GC 발동 시 JVM은 모든 스레드가 Safepoint에 도달하길 기다림. 이 대기 시간 동안 CPU가 유휴 상태가 되어 C-state 진입 가능. GC 완료 후 재시작 시 P-state Ramp-Up 지연 + Cache Cold Miss 동시 발생

#### SRE 관찰 도구

| 도구 | 관찰 대상 |
|------|-----------|
| `perf stat -e cycle_activity.stalls_l1d_pending` | L1 D-Cache Miss로 인한 Stall |
| `perf stat -e cycle_activity.stalls_l2_pending` | L2 Miss로 인한 Stall |
| `vmstat 1`의 `cs` | Context Switch 빈도 |
| `perf sched latency -p <pid>` | Context Switch로 인한 스케줄링 지연 |
| JVM `-XX:+PrintCompilation` | C1→C2 컴파일 전환 시점 (Cache 효율 개선 구간) |

---

## 16. JVM과 Cache 효율

### Java 객체 분산 문제

Java 객체는 GC Heap에 분산 배치될 수 있어 메모리 연속성이 낮아질 수 있다.

```
[Initial Allocation - 연속]        [After GC - 분산]
Object A (주소 0x1000)              Object A (주소 0x1000, 이동됨)
Object B (주소 0x1010)     →GC→    Object C (주소 0x2000, 이동됨)
Object C (주소 0x1020)              Object A → B → C (포인터 추적 필요)
```

### Cache 친화적 구조 선택

| 구조 | Cache 효율 | 이유 | 계층 |
|------|-----------|------|------|
| `int[]`, `long[]` | 높음 | 연속 메모리 배치 | Hardware (Spatial Locality) |
| `ByteBuffer` (Direct) | 높음 | Off-Heap 연속 메모리, GC 재배치 없음 | Hardware + JVM |
| `ArrayList<Integer>` | 낮음 | 박싱으로 인한 포인터 분산 | Hardware (Pointer Chasing) |
| `LinkedList<T>` | 낮음 | 노드 분산 배치 | Hardware (Pointer Chasing) |
| `LongAdder` | 높음 | Cache Line Padding으로 False Sharing 방지 | Hardware + JVM |

### 계층별 JVM Cache 효율 동작 실체

#### Hardware 계층

- **박싱(Boxing)의 이중 Miss**: `Integer[]` 배열은 참조 포인터(8byte)를 담은 배열. 배열 접근 시 포인터 로드(1차 Cache Miss 가능) → 실제 Integer 객체 접근(2차 Cache Miss 가능)의 이중 구조. `int[]` 대비 실제 데이터 밀도가 8배 낮음
- **객체 헤더 오버헤드**: 모든 Java 객체는 최소 16byte 헤더(Mark Word 8byte + Class Pointer 8byte) 보유. 작은 데이터를 많이 담는 경우 Cache Line의 유효 데이터 비율 저하

#### OS Kernel 계층

- **GC와 Page Fault**: Full GC 후 Compaction으로 객체가 이동하면 해당 메모리 페이지의 Page Table Entry가 갱신됨. TLB Shootdown(다른 코어의 TLB Invalid화)이 발생하여 GC 직후 Page Fault + TLB Miss 동시 급증
- **NUMA-aware GC**: JVM `-XX:+UseNUMA` 옵션으로 NUMA 노드별 Eden 영역 분리. 각 스레드는 실행 중인 NUMA 노드의 Eden에 객체 할당 → NUMA Remote Access 없이 L1/L2 로컬 접근 유지

#### JVM Runtime 계층

- **TLAB와 Cache 친화성**: TLAB 내 Bump-Pointer 할당으로 동시에 생성된 객체들이 메모리상 인접 배치. 동일 메서드 내에서 함께 생성된 객체들이 같은 Cache Line에 위치할 가능성이 높아 Spatial Locality 자연 형성
- **G1 GC Humongous Object**: G1 GC에서 Region 크기의 50% 이상인 거대 객체는 Humongous Region에 직접 할당. 연속 Region 할당이 필요하여 메모리 단편화 유발 → Cache 연속성 저하
- **Finalization Queue의 Cache 영향**: GC 수행 중 Finalizer 보유 객체는 즉시 수집하지 않고 `java.lang.ref.Finalizer` 큐에 등록. 이 객체들의 메모리가 장기 잔류하면 Old Gen의 Cache 효율을 저하시킴

#### SRE 관찰 도구

| 도구 | 관찰 대상 |
|------|-----------|
| `async-profiler -e cache-misses -f output.html` | JVM 메서드별 Cache Miss 핫스팟 |
| JVM JFR + JMC Memory 탭 | TLAB 할당 패턴, Eden 소진 속도 |
| `jmap -histo:live <pid>` | Heap 자료구조 분포 (박싱 객체 비율) |
| `numastat -p <pid>` | NUMA Remote Access 비율 (UseNUMA 효과 확인) |
| JVM `-Xlog:gc+humongous=debug` | Humongous 객체 할당 빈도 및 크기 |

---

## 17. 고성능 시스템의 Cache 최적화 전략

Netty, Kafka, Redis 등 고성능 시스템에서 사용하는 대표 전략이다.

| 전략 | 목적 | 관련 계층 |
|------|------|-----------|
| Sequential Access | Spatial Locality 활용, Cache Hit 증가 | Hardware |
| Off-Heap Memory | JVM GC 영향 제거, 메모리 배치 직접 제어 | JVM Runtime |
| False Sharing 방지 | Cache Line 충돌 제거 | Hardware |
| Padding 사용 | 독립 데이터를 별도 Cache Line에 분리 | Hardware + JVM |
| Ring Buffer | 연속 메모리 구조로 Cache 효율 유지 | Hardware + Application |
| NUMA-aware 할당 | 로컬 DRAM 접근으로 L3 Miss 비용 최소화 | OS Kernel + Hardware |
| HugePage / THP | TLB Coverage 확장으로 Page Walk 감소 | OS Kernel |
| CPU Pinning | Cache Warm 상태 유지, Context Switch 감소 | OS Kernel |
| Non-Temporal Store | 일회성 대용량 쓰기 시 Cache Bypass | Hardware |

### 계층별 최적화 전략 동작 실체

#### Hardware 계층

- **Ring Buffer의 Cache 장점**: LMAX Disruptor, Kafka의 내부 구조에서 활용. 고정 크기 배열 기반 Ring Buffer는 메모리 주소가 연속적이고 예측 가능한 접근 패턴 → Hardware Prefetcher 최대 활용. 포인터 기반 큐 대비 Cache Miss 수십 배 감소
- **Non-Temporal Store(NT Store)**: `_mm_stream_si32()` 등의 NT Store 명령은 Cache를 거치지 않고 Write Combining Buffer를 통해 DRAM에 직접 기록. 대용량 초기화나 버퍼 복사 후 재사용하지 않는 경우 Cache Pollution 방지
- **Prefetch Distance 조정**: 소프트웨어 Prefetch 명령으로 수백 byte 앞을 미리 Cache에 적재. Prefetch 거리가 너무 짧으면 효과 없고, 너무 길면 Cache Pollution 발생. 메모리 접근 Latency / 처리량의 비율로 최적 거리 계산

#### OS Kernel 계층

- **`numactl --membind` + `--cpunodebind`**: 프로세스를 특정 NUMA 노드의 CPU와 메모리에 동시 고정. L3 Miss가 NUMA Remote Access로 이어지는 경로를 차단. Kafka Broker, Redis 등 메모리 집약 서비스에 유효
- **`isolcpus` 커널 파라미터**: 특정 CPU 코어를 일반 스케줄링 대상에서 제외. 지정 코어는 명시적으로 배치된 스레드만 실행 → IRQ, 커널 스레드에 의한 Cache 오염 최소화. 레이턴시 크리티컬 서비스에 적용
- **Huge Page 사전 할당**: `/sys/kernel/mm/hugepages/hugepages-2048kB/nr_hugepages`에 미리 HugePage 풀 확보. THP(동적 승격)보다 예측 가능한 성능. JVM `-XX:+UseLargePages` 옵션으로 활성화

#### JVM Runtime 계층

- **Netty의 `PooledByteBufAllocator`**: Thread-Local 메모리 풀에서 ByteBuf를 재사용. GC 없이 동일 메모리 주소 반복 사용 → Temporal Locality 극대화. Off-Heap 할당으로 GC Compaction에 의한 주소 변경 없음
- **Kafka의 Page Cache 전략**: Kafka는 JVM Heap 대신 OS Page Cache를 데이터 저장소로 사용. `mmap`으로 로그 파일에 직접 접근 → JVM GC 없이 OS가 관리하는 연속 메모리 활용 → I-Cache / D-Cache 오염 최소화
- **`LongAdder` vs `AtomicLong`**: 고경합 카운터에서 `LongAdder`는 스레드별 Cell에 분산하여 False Sharing과 CAS 경합을 동시에 제거. `sum()` 시에만 전체 합산. Prometheus Counter 등 고빈도 메트릭 카운팅에 적합

#### SRE 관찰 도구

| 도구 | 관찰 대상 |
|------|-----------|
| `numactl --hardware` + `numastat` | NUMA 구성 확인 및 Remote Access 비율 |
| `cat /proc/sys/kernel/numa_balancing` | NUMA Balancing 활성화 여부 |
| `perf stat -e cache-misses` (최적화 전/후 비교) | 최적화 효과 정량 측정 |
| JVM `-XX:+UseLargePages -XX:LargePageSizeInBytes=2m` | HugePage 활성화 |
| `ethtool -G <nic> rx <N>` | NIC Rx Ring Buffer 크기 조정 |
| `cat /proc/irq/*/smp_affinity_list` | IRQ Affinity 설정 확인 |

---

## 18. 주요 성능 지표

| 지표 | 의미 | 측정 도구 |
|------|------|-----------|
| Cache Hit Ratio | Cache 적중률 = 1 - (cache-misses / cache-references) | `perf stat` |
| Cache Miss Ratio | Cache Miss 비율 (낮을수록 좋음) | `perf stat` |
| Memory Stall Cycles | 메모리 대기로 낭비된 CPU 사이클 수 | `perf stat -e cycle_activity.stalls_mem_any` |
| IPC | 사이클당 명령 처리량 = instructions / cpu-cycles | `perf stat` |
| LLC Miss Rate | L3 Miss 비율 (RAM 접근 빈도 근사) | `perf stat -e LLC-load-misses` |
| Off-CPU Time | Lock/IO 대기로 CPU를 점유하지 않는 시간 | `eBPF offcputime` |
| NUMA Miss | 원격 NUMA 노드 메모리 접근 횟수 | `numastat` |
| TLB Miss Rate | 가상→물리 주소 변환 캐시 Miss 비율 | `perf stat -e dTLB-load-misses` |
| False Sharing 빈도 | 동일 Cache Line의 다중 코어 경합 | `perf c2c` |

---

## 19. Linux 및 SRE 관점 모니터링

### CPU Cache 구조 확인

```bash
lscpu
# Cache 항목에서 L1d / L1i / L2 / L3 크기 확인 가능
# NUMA 구성, Core 당 Cache 공유 여부 확인

lstopo  # hwloc 패키지
# CPU 토폴로지와 Cache 계층을 시각적으로 표시
```

### Hardware Counter 분석

```bash
perf stat -e cache-misses,cache-references,LLC-load-misses,LLC-loads,\
            L1-dcache-load-misses,L1-icache-load-misses,\
            dTLB-load-misses,iTLB-load-misses,\
            cycle_activity.stalls_mem_any,instructions,cpu-cycles \
            -p <pid>

# 해석:
# Cache Hit Ratio = 1 - (cache-misses / cache-references)
# IPC = instructions / cpu-cycles  (낮으면 Memory-Bound)
# LLC Miss = RAM 접근 빈도 근사치
```

### Cache Miss 핫스팟 분석

```bash
# 함수별 Cache Miss 위치
perf record -e LLC-load-misses -g -p <pid>
perf report

# False Sharing 분석
perf c2c record -g -p <pid>
perf c2c report

# 메모리 접근 계층별 분포
perf mem record -p <pid>
perf mem report
```

### NUMA 관련 분석

```bash
numactl --hardware                    # NUMA 노드 구성
numastat -p <pid>                     # 프로세스별 NUMA 접근 통계
cat /proc/<pid>/numa_maps             # 프로세스 메모리의 NUMA 노드 분포
```

### JVM 관련 Cache 분석

```bash
# JFR 기반 메모리 프로파일링
jcmd <pid> JFR.start duration=60s filename=cache_profile.jfr
jcmd <pid> JFR.stop

# Async-Profiler (Cache Miss 이벤트 기반)
./profiler.sh -e cache-misses -d 30 -f output.html <pid>

# GC 로그 (Cache Pollution 주기 파악)
-Xlog:gc*:file=gc.log:time,uptime,level,tags
```

---

## 20. CPU 내부 실행 흐름 전체 연결

```
Branch Predictor (분기 예측)
  ↓ 예측 실패 시 → I-Cache Pollution
  ↓
Instruction Fetch
  → L1 I-Cache 탐색 (~1ns)
  → Miss 시 L2 → L3 → RAM → Pipeline Stall
  ↓
Decode → µop 변환 → ROB/RS 적재
  ↓
Out-of-Order Execution
  ↓
LSU (메모리 접근 요청)
  → DTLB 조회 (병렬)
  → L1 D-Cache 탐색 (~1ns)
  → Miss 시 L2 → L3 → RAM (NUMA Local → Remote 순)
  → Page Fault 시 → OS Kernel do_page_fault() → blk-mq (Disk) 가능
  ↓
ALU / FPU 연산 완료
  ↓
ROB Commit → 아키텍처 레지스터 / Cache Write-Back
```

### 계층별 전체 흐름 실체

| 단계 | 계층 | 메커니즘 실체 | SRE 관찰 도구 |
|------|------|---------------|---------------|
| 분기 예측 | Hardware | BTB, TAGE Predictor | `perf stat -e branch-misses` |
| I-Cache Fetch | Hardware | L1 I-Cache, I-TLB | `perf stat -e L1-icache-load-misses` |
| 디코드/실행 | Hardware | ROB, RS, OoO 엔진 | `perf stat -e instructions,cpu-cycles` |
| 메모리 접근 | Hardware + OS | LSU, DTLB, L1/L2/L3, DRAM | `perf mem record/report` |
| Page Fault | OS Kernel | `do_page_fault()`, `blk-mq` | `vmstat pgmajfault` |
| GC 영향 | JVM Runtime | Minor/Full GC, Compaction | `jstat -gcutil <pid>` |
| Context Switch | OS Kernel | `schedule()`, `CR3` 변경 | `vmstat cs`, `perf sched` |

---

## 21. 구성 요소 요약

| 구성 요소 | 역할 | 관련 계층 |
|-----------|------|-----------|
| L1 Cache | Core 전용 최고속 Cache (I$/D$ 분리) | Hardware |
| L2 Cache | L1 Miss 보완, Prefetch 담당 | Hardware |
| L3 Cache | Core 간 공유, LLC, NUMA 로컬 최종 방어선 | Hardware |
| Cache Line | Cache 저장 및 전송 단위 (64 Bytes) | Hardware |
| Cache Hit | Cache에서 데이터 즉시 반환 | Hardware |
| Cache Miss | 하위 계층 탐색 후 RAM 접근, Pipeline Stall | Hardware |
| Spatial Locality | 인접 메모리 연속 접근 특성 | Hardware + Application |
| Temporal Locality | 최근 사용 데이터 재사용 특성 | Hardware + Application |
| False Sharing | 독립 데이터가 동일 Cache Line 공유로 발생하는 충돌 | Hardware + JVM |
| Cache Coherency | 멀티코어 간 Cache 데이터 일관성 유지 (MESI) | Hardware + OS Kernel |
| Cache Pollution | 불필요한 데이터로 인한 Cache 효율 저하 | Hardware + OS Kernel |
| TLB | 가상→물리 주소 변환 캐시. Miss 시 Page Walk | Hardware + OS Kernel |
| NUMA | 멀티소켓 원격 메모리 접근 지연 (L3 Miss 후 분기) | Hardware + OS Kernel |
| Context Switch | L1/L2 오염, TLB Flush 유발 | OS Kernel |
| TLAB | JVM 스레드별 순차 할당 버퍼, Spatial Locality 형성 | JVM Runtime |
| Off-Heap | GC 대상 외 연속 메모리, Cache 친화적 | JVM Runtime |
| JIT C1/C2 | 바이트코드 → 네이티브 코드, Cache 효율 최적화 | JVM Runtime |

---

## 22. 성능 최적화 핵심 요소

```
High Cache Hit Ratio
+ Low Memory Stall Cycles (cycle_activity.stalls_mem_any 낮음)
+ Efficient Data Locality (Spatial + Temporal)
+ Low Cache Contention (False Sharing 방지, @Contended 적용)
+ NUMA-aware 메모리 할당 (numactl / -XX:+UseNUMA)
+ HugePage / THP (TLB Coverage 확장)
+ Context Switch 최소화 (CPU Affinity, isolcpus)
+ GC 주기 최적화 (TLAB 크기, G1 Region 크기)
= High IPC
= High Throughput + Low Latency
```

이 원칙은 다음 환경의 성능 최적화와 직접 연결된다.

- JVM 기반 서버의 자료구조 및 메모리 배치 설계
- Kubernetes 환경에서의 CPU/NUMA Cache 활용
- Netty / Kafka / Redis 등 고성능 I/O 처리
- 금융 시스템 저지연 처리
- 대규모 트래픽 처리 아키텍처 설계

---

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*