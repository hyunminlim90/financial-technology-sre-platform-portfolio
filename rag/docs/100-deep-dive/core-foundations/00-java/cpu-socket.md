# CPU Socket — Java FinTech 결제 시스템 E2E Foundations

> 정독: 0회
> 
> 관점: SRE / Kernel / Runtime / Distributed Systems (FinTech Specialist)
> 스택: Java / Spring / Netty / JVM
> 키워드: CPU Socket
> 범위: Hardware → OS Kernel → JVM Runtime → Framework → Application
> 목적: Java 기반 결제 시스템에서 CPU Socket이 실행 흐름, 자원 배분, 장애 패턴에 어떻게 연결되는지 E2E로 분석한다.

---

## 목차

1. [Stack Context 식별](#1-stack-context-식별)
2. [CPU Socket 물리 구조와 실행 기반](#2-cpu-socket-물리-구조와-실행-기반)
3. [E2E 계층별 실행 흐름 분석](#3-e2e-계층별-실행-흐름-분석)
4. [핵심 메커니즘 Deep Dive](#4-핵심-메커니즘-deep-dive)
5. [Resource Flow Correlation](#5-resource-flow-correlation)
6. [Top-Down 분석: 애플리케이션 증상 → Hardware 원인](#6-top-down-분석-애플리케이션-증상--hardware-원인)
7. [Bottom-Up 분석: Hardware 특성 → Application 성능 영향](#7-bottom-up-분석-hardware-특성--application-성능-영향)
8. [장애 및 Saturation 시나리오](#8-장애-및-saturation-시나리오)
9. [SRE 관측 지표 및 분석 도구](#9-sre-관측-지표-및-분석-도구)
10. [튜닝 전략](#10-튜닝-전략)

---

## 1. Stack Context 식별

### 1.1 CPU Socket이 연결되는 Java 스택 계층

CPU Socket은 Hardware 계층에 존재하지만, Java 기반 결제 시스템에서는 아래 스택 전반에 걸쳐 실행 성능을 결정짓는다.

| 스택 | CPU Socket 연결 지점 |
|------|-------------------|
| **Java / JVM** | GC Thread 배치, TLAB 할당, JIT 컴파일 스레드, SafePoint |
| **Spring / Spring Boot** | Tomcat Worker Thread Pool, Bean 초기화 CPU 소비 |
| **Netty / WebFlux** | EventLoop Thread → Carrier Thread → CPU Core 바인딩 |
| **Kafka** | Consumer/Producer Network Thread, Partition 처리 Thread |
| **Kubernetes** | CPU Manager, Topology Manager, cgroup CPU 할당 |

### 1.2 가장 강하게 연결되는 자원

```text
CPU Socket
  ├── CPU (Core, Cache, Pipeline)          → 최강 연결
  ├── Memory (NUMA Node, Memory Controller) → 최강 연결
  ├── Network (NIC IRQ, NUMA-aware DMA)    → 강한 연결
  └── Scheduler (CFS, NUMA Balancer)       → 강한 연결
```

---

## 2. CPU Socket 물리 구조와 실행 기반

### 2.1 CPU Socket 내부 구조

```text
CPU Socket (예: Intel Xeon Scalable, AMD EPYC)
  ├── CPU Die(s)
  │    ├── Core 0 ~ Core N
  │    │    ├── L1 Cache (명령어 캐시 32KB + 데이터 캐시 32KB)
  │    │    ├── L2 Cache (256KB ~ 1MB per core)
  │    │    └── Execution Units (ALU, FPU, SIMD)
  │    └── L3 Cache (LLC: Last Level Cache, 소켓 내 공유)
  ├── Memory Controller (IMC: Integrated Memory Controller)
  │    └── DDR 채널 → DIMM → Memory Bank
  ├── PCIe Controller
  │    ├── NIC (NVMe, GPU 포함)
  │    └── PCIe Root Complex
  └── Socket-to-Socket Interconnect
       ├── Intel: UPI (Ultra Path Interconnect)
       └── AMD: Infinity Fabric
```

### 2.2 멀티 소켓 NUMA 구조

```text
2-Socket 서버 (전형적 핀테크 결제 서버 구성):

┌─────────────────────────────┐     QPI/UPI     ┌─────────────────────────────┐
│         Socket 0            │◄───────────────►│         Socket 1            │
│                             │                 │                             │
│  Core 0~N   L3 Cache        │                 │  Core 0~N   L3 Cache        │
│       ↕                     │                 │       ↕                     │
│  Memory Controller          │                 │  Memory Controller          │
│       ↕                     │                 │       ↕                     │
│  DIMM 0~M (Local Memory)    │                 │  DIMM 0~M (Local Memory)    │
│                             │                 │                             │
│  PCIe: NIC 0, NVMe 0        │                 │  PCIe: NIC 1, NVMe 1        │
└─────────────────────────────┘                 └─────────────────────────────┘

NUMA Node 0 (Socket 0 대응)    ←→    NUMA Node 1 (Socket 1 대응)

Local Memory Access: ~80ns
Remote Memory Access (QPI/UPI): ~150~200ns  (약 2배)
```

### 2.3 Cache 계층과 접근 비용

| 캐시 레벨 | 크기 (일반적) | 접근 레이턴시 | Java 관련성 |
|---------|------------|------------|-----------|
| L1 Cache | 32~64KB / core | ~1ns (4 cycles) | JIT 컴파일된 Hot Method, Stack Frame |
| L2 Cache | 256KB~1MB / core | ~4ns (12 cycles) | TLAB 버퍼, 소형 객체 |
| L3 Cache (LLC) | 8~64MB / socket | ~15ns (40 cycles) | Heap 활성 객체, GC Survivor |
| Remote L3 (Cross-socket) | 상대 소켓 L3 | ~40ns + UPI | GC 시 Cross-socket 접근 |
| Local DRAM | DIMM | ~80ns | Java Heap Old Gen, Off-Heap |
| Remote DRAM (NUMA) | 상대 소켓 DIMM | ~150~200ns | NUMA Remote Allocation 최악 경우 |

---

## 3. E2E 계층별 실행 흐름 분석

### 3.1 결제 요청 처리 E2E — CPU Socket 관점

```text
[결제 HTTP 요청 수신]
  ↓
① NIC (PCIe) → DMA → Kernel Ring Buffer
   CPU Socket 연결 지점:
     NIC가 Socket 0 PCIe에 연결된 경우
     → DMA Write → Socket 0 Local Memory
     → IRQ → Socket 0 CPU Core (IRQ Affinity 설정에 따라)

  ↓
② IRQ → ksoftirqd → TCP/IP Stack → Socket Buffer
   CPU Socket 연결 지점:
     SoftIRQ 처리가 Socket 0 Core에서 실행
     → sk_buff 객체 → Socket 0 Local Memory 할당
     → Socket 1 Worker Thread가 read() 시 Remote Memory Access 발생 가능

  ↓
③ Tomcat/Netty Worker Thread (JVM Thread)
   CPU Socket 연결 지점:
     Thread가 실행되는 Core의 NUMA Node가 결정
     → JVM TLAB 할당 → 해당 NUMA Node 메모리
     → Spring 처리 중 생성되는 DTO, Entity 객체 → NUMA Node 의존

  ↓
④ HikariCP getConnection() → JDBC → DB Socket I/O
   CPU Socket 연결 지점:
     DB Connection Pool Thread가 특정 Socket Core에 배치
     → TCP Socket 버퍼 Read/Write → NUMA Node 영향

  ↓
⑤ GC 실행 (G1GC / ZGC)
   CPU Socket 연결 지점:
     GC Thread가 Heap 전체를 스캔
     → Heap이 양쪽 NUMA Node에 분산된 경우 → Cross-socket Memory Access
     → UseNUMA 옵션 미설정 시 Remote DRAM 접근 빈발 → GC Pause 증가

  ↓
⑥ HTTP 응답 전송
   CPU Socket 연결 지점:
     write() syscall → Kernel Send Buffer (NUMA Node 의존)
     → NIC DMA → 네트워크 전송
```

### 3.2 Netty EventLoop + CPU Socket 실행 흐름

```text
Netty Worker EventLoop (JVM Thread, OS Thread 매핑):

CPU Socket 0                          CPU Socket 1
  Core 0                                Core 4
    └── EventLoop Thread 0                └── EventLoop Thread 4
          ├── epoll_wait()                      ├── epoll_wait()
          ├── ChannelPipeline 처리              ├── ChannelPipeline 처리
          └── WebFlux Mono/Flux                 └── WebFlux Mono/Flux

문제:
  EventLoop Thread 0이 Socket 0 Core에서 실행
    → 처리 중 Memory 접근
    → JVM Heap이 Socket 1 Memory에 할당된 경우
    → 매 객체 접근마다 UPI Interconnect 경유
    → 지연 누적 → EventLoop Delay → 결제 응답 지연
```

### 3.3 Kafka Consumer + CPU Socket 실행 흐름

```text
Kafka Consumer Thread:

poll() → Fetch 요청 → Broker 응답 수신
  → ConsumerRecord 역직렬화 (Jackson → CPU 집약적)
  → 결제 후처리 로직 실행

CPU Socket 관점:
  Consumer Thread가 실행되는 NUMA Node
    → ConsumerRecord 객체 할당 → 해당 NUMA Node 메모리
    → 역직렬화 CPU 연산 → L1/L2 Cache 사용

위험:
  GC STW 중 Consumer Thread 정지
    → GC Thread가 Heap 스캔 시 Cross-socket Remote Access
    → GC Pause 연장 → session.timeout.ms 초과 → Rebalance
```

---

## 4. 핵심 메커니즘 Deep Dive

### 4.1 NUMA Access — 가장 중요한 메커니즘

#### NUMA 접근 경로와 비용

```text
Thread (Core 0, Socket 0) 가 메모리 주소 X에 접근:

Case 1: Local Access
  Core 0 → L1 Miss → L2 Miss → L3 Hit 또는 DRAM (Socket 0 Local)
  경로: Core → IMC → Local DIMM
  Latency: ~80ns

Case 2: Remote Access
  Core 0 → L1 Miss → L2 Miss → L3 Miss → UPI → Socket 1 IMC → Remote DIMM
  경로: Core → IMC → UPI Interconnect → 상대 소켓 IMC → 원격 DIMM
  Latency: ~150~200ns

비율: Remote / Local ≈ 1.8 ~ 2.5배
```

#### JVM에서 NUMA Remote Access 발생 원인

```text
1. JVM Heap 기본 할당 동작:
   JVM이 시작 시 mmap()으로 Heap 예약
   → OS는 first-touch policy로 첫 접근 CPU의 NUMA Node에 페이지 할당
   → JVM Main Thread (보통 Socket 0) 가 먼저 접근
   → Heap 전체가 Socket 0 Memory에 집중 할당

2. Worker Thread가 Socket 1 Core에서 실행:
   → Heap 객체 접근 → Socket 0 Memory → Remote Access
   → 모든 new Object() → 모든 필드 접근 → UPI 경유

3. UseNUMA 옵션 미설정:
   → JVM이 NUMA 인지 없이 단일 Pool로 메모리 관리
   → Thread 실행 위치와 메모리 할당 위치 불일치 심화
```

#### UseNUMA 활성화 후 동작

```text
-XX:+UseNUMA 설정 시:

JVM이 NUMA Node 별로 Eden Region을 분리
  → Thread A (Socket 0 Core) → Eden Pool 0 → Socket 0 Memory에 TLAB 할당
  → Thread B (Socket 1 Core) → Eden Pool 1 → Socket 1 Memory에 TLAB 할당

장점:
  TLAB 할당이 항상 Local Memory → Remote Access 제거
  Minor GC 시 Local Survivor로 이동 → Local 유지

한계:
  Old Gen은 NUMA-aware 완벽 지원이 어려움 (G1GC 개선 중)
  GC Thread 자체가 Cross-socket 스캔 필요 → 완전 제거 불가
```

### 4.2 TLAB과 CPU Socket

```text
TLAB (Thread-Local Allocation Buffer):

각 Thread가 Eden Space의 일부를 선점
  → 객체 할당 시 동기화 없이 bump-pointer 이동

CPU Socket 연결:
  TLAB 자체는 Thread가 실행 중인 Core의 NUMA Node 메모리에 할당 (UseNUMA 시)
  → TLAB 내 객체 생성 → Local Memory → L1/L2 Cache 친화적

TLAB 재충전 (TLAB 소진 시):
  JVM Lock 획득 → 새 TLAB 요청
  → 이 시점에 다른 NUMA Node 메모리가 할당되면 Remote Access 시작
  → 빈번한 TLAB 재충전 (DTO 대량 생성) → NUMA 불균형 심화 가능
```

### 4.3 GC Thread와 Cross-socket Memory Access

```text
G1GC Young Collection (Minor GC):
  GC Thread들이 Eden Region 스캔 → Survivor로 복사

CPU Socket 관점:
  GC Thread 수 = -XX:ParallelGCThreads (기본: CPU 코어 수의 5/8)
  → GC Thread가 모든 코어에 분산 실행
  → Eden Region이 Socket 0 Memory에만 있으면:
       Socket 1 Core의 GC Thread → Remote Memory Read → UPI 사용
       → Memory Bandwidth 소비 증가 → GC Pause 연장

G1GC Full GC (Serial 압축):
  단일 Thread가 전체 Heap 순회
  → 2-Socket 서버에서 절반이 Remote Access
  → Full GC Pause 극단적 증가 가능
```

### 4.4 JIT 컴파일러와 L1/L2 Cache

```text
JIT C2 컴파일 결과물 (Native Code):
  Code Cache → JVM Native Memory
  → 실행 중 CPU L1/L2 Instruction Cache에 적재

CPU Socket 관점:
  Hot Method (결제 검증 로직, Jackson 직렬화 등)
    → L1 I-Cache (32KB): 매우 빈번 접근 코드
    → L2 Cache: 중간 빈도 코드
    → L3 Cache (소켓 공유): 덜 빈번한 코드

Cross-socket 위험:
  Thread가 Socket 0 → Socket 1로 Migration 시
    → 이전 Socket의 Cache는 무효
    → L3 Cache Miss → DRAM 재적재
    → JIT 컴파일된 Hot Method 재적재 비용 → Latency 스파이크

Deoptimization과 Cache Flush:
  Class Loading, 타입 변경 → JIT Deoptimization
  → Code Cache 해당 섹션 무효화
  → 모든 소켓의 관련 Cache Line 무효화 필요
  → Cache Coherency Traffic 증가
```

### 4.5 IRQ Affinity와 CPU Socket

```text
NIC → PCIe → CPU Socket 연결:

물리 서버:
  NIC가 Socket 0 PCIe Root Complex에 연결된 경우
    → NIC IRQ → 기본적으로 Socket 0 CPU에 할당
    → SoftIRQ (NET_RX) → Socket 0 ksoftirqd

문제 발생:
  Tomcat/Netty Worker Thread가 Socket 1 Core에서 실행
    → 패킷 수신: Socket 0 SoftIRQ → sk_buff → Socket 0 Memory
    → Thread 처리: Socket 1 Core → Socket 0 Memory sk_buff 접근
    → Remote Memory Read: 모든 패킷 데이터가 Cross-socket

NUMA-aware IRQ 최적화:
  NIC가 연결된 Socket의 CPU에 IRQ 바인딩
  Worker Thread도 같은 Socket에 affinity 설정
  → 패킷 데이터 → 처리 Thread → 동일 NUMA Node

RSS (Receive Side Scaling) + NUMA:
  NIC Multi-queue → 각 RX Queue를 Socket별 CPU에 바인딩
  → 수신 패킷을 처리할 Thread와 동일 NUMA Node에 배치
  → sk_buff Local Memory → Worker Thread Local Access
```

### 4.6 Context Switch와 CPU Migration

```text
Linux CFS Scheduler:
  Thread를 Runqueue에서 관리
  부하 분산(Load Balancing) → 덜 바쁜 Core로 Thread Migration 가능

CPU Socket 관점:
  Thread가 Socket 0 Core 2 → Socket 1 Core 5로 Migration 시:

  비용 1: Cache Miss
    L1/L2 Cache (Core 2의 캐시) → 완전 무효
    Socket 1에서 처음부터 Cache Warm-up 필요
    → 결제 처리 첫 몇 번의 요청: 높은 Cache Miss Rate

  비용 2: NUMA 메모리 불일치
    Thread가 사용하던 객체 → Socket 0 Local Memory에 존재
    Socket 1에서 해당 객체 접근 → Remote Access

  비용 3: TLB Miss
    Thread가 사용하던 TLB Entry → Socket 1에서 무효
    → Page Table Walk 필요 → 추가 Memory Access

Cross-socket Migration이 결제 처리에 미치는 영향:
  EventLoop Thread Migration → Netty Channel 처리 지연
  GC Thread Migration → GC Pause 연장
  Worker Thread Migration → Request 처리 P99 증가
```

### 4.7 False Sharing과 Cache Coherency (멀티 소켓)

```text
Cache Line = 64 bytes

False Sharing 발생:
  Thread A (Socket 0 Core): 변수 X 쓰기
  Thread B (Socket 1 Core): 변수 Y 쓰기 (X와 같은 Cache Line)

MESI 프로토콜 (Cross-socket):
  Thread A가 X를 씀 → Cache Line: Modified (Socket 0)
  Thread B가 Y에 접근 → Socket 1 L1에 없음
    → Cache Coherency Directory 조회
    → Socket 0에 Invalidate 신호
    → Socket 0 Cache Line → UPI → Socket 1
    → Thread B가 Y 수정 → Modified (Socket 1)
  Thread A가 X 재접근:
    → Socket 1 Invalidate 신호 → UPI → Socket 0 전달

반복 시:
  Cache Line이 Socket 간 계속 이동 (Cache Line Bouncing)
  UPI Bandwidth 소비
  실제 데이터 변경은 없어도 Cross-socket 트래픽 발생

Java 결제 시스템 위험 지점:
  AtomicLong 기반 결제 TPS 카운터
  공유 Queue (LinkedBlockingQueue 내부 head/tail)
  Executor 상태 변수 (ctl 필드)
  → @Contended 어노테이션으로 128byte 패딩 삽입 필요
```

### 4.8 SafePoint와 CPU Socket

```text
SafePoint 요청 메커니즘:
  JVM → 각 Thread의 폴링 주소에 접근 불가 페이지 설정 (mprotect)
  → Thread가 폴링 시 SIGSEGV → JVM Signal Handler → Thread 정지

CPU Socket 관점:
  Signal 전달: 모든 Thread에 SafePoint 신호 전파
  → Cross-socket Thread에는 IPI (Inter-Processor Interrupt) 경유
  → UPI를 통해 Signal 전달

TTSP (Time to Safepoint):
  Socket 1의 Thread가 SafePoint에 늦게 도달하는 경우:
    → JNI Critical Section에 있거나
    → Counted Loop에서 폴링 생략 중
    → SafePoint 폴링 주소 접근 자체가 Remote Memory Access
  → TTSP 증가 → 전체 STW 연장

결제 영향:
  TTSP 100ms → 이 동안 결제 처리 전면 중단
  외부 PG API read timeout (예: 200ms) 이내에 TTSP + GC 완료해야 함
```

---

## 5. Resource Flow Correlation

### 5.1 CPU Socket이 연결하는 자원 흐름

```text
CPU Socket
  │
  ├── CPU (Core, Pipeline, Cache)
  │     절약: Local Cache Hit 증가 → CPU 낭비 감소
  │     소비: Cross-socket MESI 프로토콜 → CPU Stall 증가
  │
  ├── Memory (NUMA Node, Bandwidth)
  │     절약: Local DRAM Access → 낮은 Latency, 높은 Bandwidth
  │     소비: Remote DRAM Access → UPI Bandwidth 소비 + Latency 2배
  │
  ├── Network (NIC IRQ, DMA)
  │     절약: NUMA-aware IRQ Affinity → sk_buff Local 할당 → 처리 효율
  │     소비: Cross-socket IRQ → 모든 패킷 처리에 UPI 추가 비용
  │
  └── Scheduler (CFS, NUMA Balancer)
        절약: Thread-to-NUMA 정렬 → Context Switch 비용 감소
        소비: Cross-socket Migration → Cache Miss + TLB Flush + Remote Access
```

### 5.2 트레이드오프 맵

| 선택 | 절약하는 자원 | 추가로 소비하는 자원 | 결제 시스템 영향 |
|------|------------|-----------------|--------------|
| IRQ를 Socket 0에 집중 | IRQ 설정 단순화 | Socket 0 SoftIRQ 포화 | 결제 수신 병목 |
| IRQ를 소켓 별 분산 | SoftIRQ 균등 | 설정 복잡도 증가 | Throughput 향상 |
| UseNUMA 비활성 | 설정 없음 | Remote Memory Access 빈발 | GC Pause 증가 |
| UseNUMA 활성 | DRAM Latency | GC NUMA 복잡도 증가 | 결제 처리량 향상 |
| Thread Affinity 고정 | Context Switch | 부하 불균형 가능성 | P99 안정, 일부 코어 과부하 |
| NUMA Balancer 활성 | 자동 최적화 | Migration 비용 발생 | 간헐적 Latency 스파이크 |

---

## 6. Top-Down 분석: 애플리케이션 증상 → Hardware 원인

### 6.1 증상: CPU 사용률 낮은데 결제 P99 Latency 높음

```text
관측: Grafana → CPU usage 40%, P99 Latency 500ms 이상

추적 경로:
  P99 Latency 높음
    ↓
  JFR SocketRead 지연? → 외부 PG API 문제 아님 확인
    ↓
  JFR GC Pause 확인 → GC는 짧음 (30ms 이내)
    ↓
  async-profiler wall-clock → Off-CPU Time 큰 구간 존재
    ↓
  perf stat -e cache-misses -p <PID> → LLC Cache Miss Rate 높음
    ↓
  numastat -p <PID> → numa_miss 카운터 높음 (Remote Access 빈발)
    ↓
  원인: JVM Heap이 Socket 0에 집중, Worker Thread 일부가 Socket 1 Core에서 실행
        → 모든 객체 접근이 Remote DRAM Access (150~200ns)
        → CPU Stall → 스레드 진행 느림 → P99 증가
```

### 6.2 증상: 특정 시간대에만 GC Pause 급증

```text
관측: jvm_gc_pause_seconds_max 급등 → 결제 실패율 증가

추적 경로:
  GC Pause 증가
    ↓
  -Xlog:gc* 로그 → Mixed GC 시간 급증
    ↓
  JFR GC 이벤트 → GC Thread 수행 시간 분포 확인
    ↓
  perf stat -e LLC-load-misses → GC 중 LLC Miss 급증
    ↓
  numastat → numa_miss 급증 (GC 실행 구간)
    ↓
  원인: GC Thread가 양쪽 Socket의 Heap 스캔
        → Socket 0 GC Thread가 Socket 1 Memory 스캔 (Remote Access)
        → UPI Bandwidth 포화 → Memory 응답 지연 → GC 완료 지연
        → UseNUMA 미설정 또는 G1GC Old Region NUMA 불균형
```

### 6.3 증상: EventLoop Thread 지연 (Netty)

```text
관측: reactor.netty 메트릭 → EventLoop busy time 증가
      결제 HTTP 응답 P95 지연

추적 경로:
  EventLoop Delay
    ↓
  JFR ThreadPark → park 원인 분석 → I/O가 아닌 CPU Stall
    ↓
  perf record -g -p <PID> → Flamegraph 분석
    → NUMA miss / LLC miss 구간 확인
    ↓
  taskset -cp <PID> → EventLoop Thread의 CPU 위치 확인
    ↓
  cat /proc/irq/<NIC IRQ>/smp_affinity_list → NIC IRQ 위치 확인
    ↓
  원인: NIC IRQ → Socket 0, EventLoop Thread → Socket 1
        → 수신 패킷 (sk_buff) → Socket 0 Memory
        → EventLoop Thread → 매 패킷 처리마다 Remote Access
        → Thread 진행 지연 → EventLoop Delay → 결제 응답 지연
```

---

## 7. Bottom-Up 분석: Hardware 특성 → Application 성능 영향

### 7.1 UPI Interconnect Bandwidth 한계 → Application 영향

```text
Intel UPI Bandwidth: 약 20~40 GB/s (방향당)

결제 시스템에서 UPI 소비 요인:
  1. JVM Heap Remote Access: 결제 요청당 수백 KB 객체 생성 → Cross-socket 시 UPI 소비
  2. GC Thread Cross-socket Scan: Heap 전체 스캔 → 수 GB UPI 트래픽
  3. False Sharing Cache Bouncing: 공유 카운터 변경마다 Cache Line 전송
  4. NIC → Socket 0, App → Socket 1: 모든 수신 패킷 UPI 경유

포화 시:
  UPI 대역폭 경합 → 모든 Remote Access 대기 증가
  → CPU Core가 Memory Stall 상태 (perf stat: stalled-cycles-backend)
  → Java Thread 진행 불가 → Latency 급증
  → 결제 처리량 감소 (Throughput 저하)

관측:
  perf stat -e offcore_requests_outstanding.all_data_rd -p <PID>
  pcm (Intel Performance Counter Monitor): UPI Bandwidth 실시간 확인
```

### 7.2 LLC Cache Size → GC 동작 영향

```text
L3 Cache (LLC) 크기 (예: 40MB / socket):

JVM Heap 활성 객체가 LLC에 들어오면:
  → GC Scan: DRAM 접근 없이 LLC에서 처리 가능
  → GC Pause 단축

Heap 활성 객체 > LLC:
  → GC가 DRAM 직접 접근
  → Local DRAM: ~80ns / 접근
  → Remote DRAM: ~160ns / 접근

결제 시스템 영향:
  결제 요청 1건당 활성 객체 크기 추산 필요
  TPS × 요청당 활성 객체 크기 vs LLC 크기 비교
  → LLC Footprint 초과 시 GC Pause 급증 구간 존재
  → Heap Size 튜닝 기준이 됨
```

### 7.3 Memory Controller Bandwidth → 결제 처리량 상한

```text
DDR5 Memory Bandwidth (예): 채널당 ~50 GB/s, 8채널 → 최대 400 GB/s

결제 시스템 병목:
  모든 Java 객체 접근 = Memory Bandwidth 소비
  고TPS 시 (수천 TPS):
    수신 패킷 역직렬화 (JSON → Java Object): 대량 메모리 쓰기
    GC Scan: 대량 메모리 읽기
    Kafka 메시지 역직렬화: 대량 메모리 쓰기

  Remote DRAM 접근 시:
    UPI를 통해 상대 소켓 Memory Controller로 요청 전달
    → 상대 소켓 Memory Bandwidth도 소비
    → 두 소켓의 Memory Bandwidth가 동시 압박

관측:
  perf stat -e mem_load_retired.l3_miss → DRAM 직접 접근 횟수
  Intel MLC (Memory Latency Checker): 실제 Bandwidth/Latency 측정
```

---

## 8. 장애 및 Saturation 시나리오

### 8.1 NUMA Remote Access 누적 → Tail Latency 폭증

```text
시나리오:
  결제 서버 2-Socket, JVM UseNUMA 미설정
  일반 부하: 500 TPS → P99 80ms (허용 범위)
  Burst 트래픽: 2000 TPS → P99 800ms (SLA 위반)

원인 분석:
  500 TPS:
    Worker Thread 일부가 Socket 1에서 실행
    NUMA Remote Access 발생하나 UPI Bandwidth 여유 있음
    → 지연 흡수 가능

  2000 TPS:
    모든 Worker Thread 활성화 → Socket 0, 1 모두 실행
    Socket 1 Thread → Heap (Socket 0 Memory) Remote Access 폭증
    UPI Bandwidth 포화 → 모든 Remote Access 대기
    → P99 급증 → Timeout → Circuit Breaker 작동
    → 결제 실패율 증가

연쇄 장애:
  UPI 포화
    → GC Thread도 Remote Access 대기
    → GC Pause 연장 (100ms → 500ms)
    → Kafka Consumer session.timeout 초과
    → Kafka Rebalance → 결제 후처리 중단
    → 결제 원장 기록 지연 → 정합성 위험
```

### 8.2 IRQ 집중 → SoftIRQ Saturation → 결제 수신 병목

```text
시나리오:
  결제 트래픽 급증 (이벤트/세일 피크)
  NIC 4개 RX Queue → 모두 Socket 0 CPU에 IRQ 집중 (IRQ Affinity 미설정)

발생 과정:
  패킷 수신 폭증
    → Socket 0 특정 Core의 SoftIRQ (NET_RX) 과부하
    → ksoftirqd CPU 점유율 100%
    → 패킷 처리 지연 → Kernel Receive Buffer 적체
    → TCP Receive Buffer 고갈 → TCP Zero Window 광고
    → 클라이언트 (PG사) 송신 중단
    → 결제 요청 처리 불가

관측:
  mpstat -P ALL 1 → 특정 Core %soft 90% 이상
  cat /proc/net/softnet_stat → dropped 카운터 증가
  cat /proc/interrupts → eth0 IRQ가 단일 Core에 집중

해결:
  ethtool -L eth0 rx 8 → NIC RX Queue 수 증가
  /proc/irq/<IRQ>/smp_affinity_list → Socket별 Core에 분산 배치
  RSS: 패킷 5-tuple Hash → 여러 Core에 자동 분산
```

### 8.3 Cross-socket GC → Kafka Rebalance → 결제 후처리 장애

```text
시나리오:
  G1GC Mixed GC 중 Cross-socket Memory Access 과다
  → GC Pause 400ms 발생
  → Kafka Consumer Thread 정지
  → session.timeout.ms = 30000ms 이내이나 max.poll.interval.ms 초과 가능성

장애 연쇄:
  GC Pause 400ms
    → Consumer poll() 미호출
    → Heartbeat Thread는 살아있으나 poll 중단
    → max.poll.interval.ms = 300000ms이나 처리 로직이 GC 후 지연 누적
    → 이후 GC Pause 반복 → 누적 지연 > max.poll.interval.ms
    → Kafka Group Coordinator: Consumer 제외 → Rebalance
    → Rebalance 중 파티션 재조정 → 결제 후처리 (원장, 알림) 수 초~수십 초 중단
    → 중복 메시지 처리 위험 (at-least-once) → 결제 원장 중복 기록 위험

해결:
  -XX:+UseNUMA 설정
  ZGC 전환 (Sub-ms GC Pause)
  Kafka max.poll.interval.ms 증가 + 처리 로직 경량화
```

### 8.4 False Sharing → 결제 카운터 병목

```text
시나리오:
  결제 성공/실패 AtomicLong 카운터를 단일 객체에 인접 선언

  class PaymentMetrics {
      AtomicLong successCount; // offset 16
      AtomicLong failCount;    // offset 24
      // 두 변수가 같은 Cache Line (64byte) 내 존재
  }

발생 과정:
  Socket 0 Thread: successCount.incrementAndGet() → CAS → Cache Line Modified (Socket 0)
  Socket 1 Thread: failCount.incrementAndGet() → CAS → Cache Line 요청
    → Socket 0에 Invalidate → UPI → Socket 1 전달
    → Socket 1 Thread가 수정 → Cache Line Modified (Socket 1)
  Socket 0 Thread 재접근:
    → Socket 1 Invalidate → UPI → Socket 0 전달

  반복 → Cache Line Bouncing → UPI 트래픽 → CPU Stall
  고TPS (수천 TPS) 시 → 카운터 업데이트 자체가 병목

측정:
  perf stat -e cache-misses,LLC-store-misses -p <PID>
  → 카운터 업데이트 구간에서 Cache Miss 급증

해결:
  @Contended 어노테이션 (128byte 패딩):
  @jdk.internal.vm.annotation.Contended
  class PaymentMetrics {
      AtomicLong successCount;
      AtomicLong failCount;
  }

  또는 LongAdder 사용:
  LongAdder successCount; // Cell 배열 분산 → 소켓별 로컬 누적 → 합산
  LongAdder failCount;
  → Cache Line Bouncing 완전 제거
```

---

## 9. SRE 관측 지표 및 분석 도구

### 9.1 NUMA 현황 관측

```bash
# NUMA 구성 전체 확인
numactl --hardware

# NUMA별 메모리 사용 현황
numastat

# 프로세스별 NUMA 접근 패턴 (Java PID)
numastat -p <PID>
# → numa_hit: Local Access 성공
# → numa_miss: Remote Access 발생 (이 값이 높으면 NUMA 문제)
# → interleave_hit: Interleave 정책으로 할당된 수

# NUMA 정책 확인
cat /proc/<PID>/numa_maps | head -50
# 각 메모리 영역의 NUMA Node 확인 가능

# 실시간 NUMA Miss Rate
watch -n1 'numastat -p <PID> | grep numa_miss'
```

### 9.2 Cache Miss 및 CPU 성능 카운터

```bash
# LLC (L3) Cache Miss Rate
perf stat -e LLC-loads,LLC-load-misses,LLC-stores,LLC-store-misses \
    -p <PID> sleep 10

# NUMA 원격 접근 카운터 (Intel 전용)
perf stat -e offcore_requests.all_data_rd \
           -e offcore_response.all_reads.llc_miss.remote_dram \
    -p <PID> sleep 10

# CPU Stall (Memory Stall 포함)
perf stat -e stalled-cycles-frontend,stalled-cycles-backend \
    -p <PID> sleep 10
# stalled-cycles-backend 높으면 Memory Latency가 원인

# Cache Miss Flamegraph
perf record -e LLC-load-misses -g -p <PID> sleep 30
perf script | stackcollapse-perf.pl | flamegraph.pl --title="LLC Miss" > llc_miss.svg
```

### 9.3 IRQ 분포 확인

```bash
# IRQ 현황 (NIC IRQ 분포)
cat /proc/interrupts | grep -E 'CPU|eth|ens|eno'

# NIC IRQ를 특정 CPU에 바인딩
cat /proc/irq/<IRQ번호>/smp_affinity_list

# 인터럽트 발생 빈도 실시간 모니터링
watch -n1 'cat /proc/interrupts | grep eth0'

# SoftIRQ per CPU (NET_RX 집중 확인)
mpstat -P ALL 1 5
# %soft 컬럼 확인: 특정 CPU가 30% 이상이면 집중 발생

# SoftIRQ 드롭
cat /proc/net/softnet_stat
# 컬럼2: dropped (netdev_max_backlog 초과 드롭)
# 컬럼3: time_squeeze (Budget 소진으로 처리 못한 수)
```

### 9.4 Thread Affinity 확인

```bash
# Java 프로세스 Thread별 CPU Affinity
taskset -cp <PID>

# 특정 Thread의 실행 CPU 이력
pidstat -t -p <PID> 1
# → CPU 컬럼: Thread가 실행된 CPU 번호 확인
# → 계속 변하면 Migration 빈발

# Thread CPU Migration 횟수
cat /proc/<PID>/task/<TID>/sched | grep nr_migrations

# perf로 Thread Migration 추적
perf stat -e migrations -p <PID> sleep 10
```

### 9.5 JVM NUMA 확인

```bash
# UseNUMA 설정 확인
jcmd <PID> VM.flags | grep NUMA

# JVM Native Memory (NUMA 별 Heap 분포 간접 확인)
jcmd <PID> VM.native_memory summary

# GC 로그에서 Cross-socket 영향 확인
# -Xlog:gc*:file=/tmp/gc.log:time,uptime,level,tags
# GC Pause 시간 분포 → numastat numa_miss와 상관관계

# JFR로 GC 중 Memory 접근 패턴
jcmd <PID> JFR.start duration=60s filename=/tmp/numa.jfr
# JMC에서 GC Pause vs Allocation Rate 분석
```

### 9.6 PSI (Pressure Stall Information)

```bash
# CPU Pressure (NUMA Miss로 인한 Stall 포함)
cat /proc/pressure/cpu
# some avg10: 일부 Task가 CPU 대기 중인 비율
# 결제 시스템: some avg10 > 10% 지속 시 조사 필요

# Memory Pressure (NUMA 불균형으로 인한 Swap/Reclaim)
cat /proc/pressure/memory

# Prometheus에서 PSI 확인
# node_pressure_cpu_waiting_seconds_total
# node_pressure_memory_waiting_seconds_total
```

### 9.7 async-profiler (NUMA/Cache 영향 분석)

```bash
# Wall-clock 프로파일링 (Off-CPU 포함: NUMA Stall 포함)
./profiler.sh -e wall -d 60 -f /tmp/wall.html <PID>

# Cache Miss 기반 프로파일링 (PMU 이벤트)
./profiler.sh -e cache-misses -d 60 -f /tmp/cache.html <PID>

# DRAM 접근 기반 프로파일링
./profiler.sh -e mem_load_retired.l3_miss -d 60 -f /tmp/dram.html <PID>
# → Flamegraph에서 DRAM 직접 접근이 많은 메서드 식별
# → Jackson, GC, HikariCP 등 핫스팟 확인
```

### 9.8 eBPF / bpftrace

```bash
# NUMA Miss 발생 위치 추적 (uprobe + perf_event)
bpftrace -e '
hardware:cache-misses:1000 {
    @[kstack] = count();
}'

# Thread CPU Migration 추적
bpftrace -e '
tracepoint:sched:sched_migrate_task
/comm == "java"/ {
    printf("Thread %s migrated from CPU %d to %d\n", args->comm, args->orig_cpu, args->dest_cpu);
}'

# GC 중 메모리 접근 패턴
bpftrace -e '
uprobe:/proc/<PID>/root/usr/lib/jvm/java/lib/server/libjvm.so:*G1CollectedHeap*collect* {
    @start = nsecs;
}
uretprobe:/proc/<PID>/root/usr/lib/jvm/java/lib/server/libjvm.so:*G1CollectedHeap*collect* {
    @gc_duration = hist(nsecs - @start);
}'
```

### 9.9 Intel PCM / AMD μProf (Cross-socket 전용)

```bash
# Intel PCM: UPI Bandwidth 실시간 확인
pcm.x 1  # 1초 간격

# UPI Bandwidth 확인 항목:
#   QPI0 (Socket 0 → Socket 1) Bandwidth
#   QPI1 (Socket 1 → Socket 0) Bandwidth
# → 결제 TPS 증가 시 UPI 사용량 상관관계 확인

# Intel MLC (Memory Latency Checker)
mlc --latency_matrix   # NUMA 간 Latency 측정
mlc --bandwidth_matrix # NUMA 간 Bandwidth 측정
```

---

## 10. 튜닝 전략

### 10.1 JVM NUMA 최적화

```bash
# JVM NUMA-aware 설정
-XX:+UseNUMA                    # NUMA-aware TLAB / Eden 할당
-XX:+UseParallelGC              # UseNUMA와 완전 호환 (G1도 부분 지원)

# G1GC + UseNUMA
-XX:+UseG1GC
-XX:+UseNUMA                    # G1GC JDK 14+에서 NUMA-aware Region 지원
-XX:G1HeapRegionSize=4m         # Region 크기 증가 → NUMA Node당 Region 집중화

# ZGC (NUMA-aware 내장)
-XX:+UseZGC                     # ZGC는 NUMA-aware 설계 (별도 옵션 불필요)

# GC Thread 수 조정 (Cross-socket 스캔 최소화)
-XX:ParallelGCThreads=<소켓당 코어 수>  # 단일 소켓 범위로 제한 고려
-XX:ConcGCThreads=<값>

# Heap 크기 (LLC 대비 최적화)
# 활성 객체 크기 ≤ LLC 크기 목표 → GC 중 DRAM 접근 최소화
```

### 10.2 OS / Kernel NUMA 튜닝

```bash
# NUMA Balancer (자동 Migration) 비활성화 (레이턴시 민감 환경)
sysctl -w kernel.numa_balancing=0
# 단: 수동 Affinity 설정 필요

# 프로세스를 특정 NUMA Node에 고정
numactl --cpunodebind=0 --membind=0 java -jar payment-service.jar
# → Java 프로세스가 Socket 0 CPU + Socket 0 Memory만 사용

# Interleave 정책 (메모리를 양쪽 소켓에 균등 분산)
numactl --interleave=all java -jar payment-service.jar
# → 단일 소켓 집중 방지 / NUMA 최적화 효과는 감소

# 특정 Thread에 CPU Affinity 설정
taskset -cp 0-15 <PID>   # Socket 0 Core 0~15에 고정
```

### 10.3 IRQ Affinity 최적화

```bash
# NIC RX Queue 수 증가
ethtool -L eth0 rx 8 tx 8

# NIC가 연결된 NUMA Node 확인
cat /sys/class/net/eth0/device/numa_node

# IRQ를 NIC 연결 NUMA Node의 CPU에 바인딩
# NIC가 Socket 0에 연결된 경우 (CPU 0~15가 Socket 0):
for IRQ in $(cat /proc/interrupts | grep eth0 | awk '{print $1}' | tr -d ':'); do
    echo "0-15" > /proc/irq/$IRQ/smp_affinity_list
done

# Worker Thread도 동일 Socket에 배치 (Kubernetes 외부 환경)
# numactl 또는 taskset으로 JVM 프로세스 고정

# RSS (Receive Side Scaling): NIC Queue → CPU 분산
ethtool -X eth0 equal 8   # 8개 Queue에 균등 분산
```

### 10.4 Java 코드 수준 False Sharing 방지

```java
// LongAdder 사용 (결제 TPS 카운터)
class PaymentMetrics {
    private final LongAdder successCount = new LongAdder();
    private final LongAdder failCount = new LongAdder();

    void recordSuccess() { successCount.increment(); }
    void recordFailure() { failCount.increment(); }

    long getSuccessCount() { return successCount.sum(); }
    long getFailCount() { return failCount.sum(); }
}

// @Contended (내부 라이브러리용)
// JVM 시작 옵션: -XX:-RestrictContended (JDK 9+)
@jdk.internal.vm.annotation.Contended
class PaymentCounter {
    long successCount;
    long failCount;
}

// Padding 직접 구현 (라이브러리 독립)
class PaddedAtomicLong extends AtomicLong {
    public volatile long p1, p2, p3, p4, p5, p6 = 7L;
    // 64byte × 2 = 128byte → Cache Line 격리
}
```

### 10.5 Kubernetes NUMA 정렬

```yaml
# kubelet 설정 (CPU Manager Static Policy)
# /var/lib/kubelet/config.yaml
cpuManagerPolicy: static
topologyManagerPolicy: single-numa-node  # 또는 restricted

---
# Pod 설정: Guaranteed QoS + CPU/Memory 정수 요청
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: payment-service
    resources:
      requests:
        cpu: "8"         # 정수 CPU 요청 → CPU Manager가 전용 Core 할당
        memory: "16Gi"
      limits:
        cpu: "8"         # requests = limits → Guaranteed QoS
        memory: "16Gi"
    # Topology Manager가 CPU + Memory + NIC를 동일 NUMA Node에 배치
```

### 10.6 Netty EventLoop NUMA 정렬

```java
// EventLoop Thread를 특정 NUMA Node에 고정
// (OS 수준 numactl + JVM 수준 조합)

// Netty EventLoopGroup 스레드 수를 NUMA Node당 코어 수로 설정
int coresPerNuma = Runtime.getRuntime().availableProcessors() / 2; // 2-socket 가정
EventLoopGroup workerGroup = new EpollEventLoopGroup(
    coresPerNuma,
    new DefaultThreadFactory("netty-worker", true)  // daemon thread
);

// 운영 환경: JVM 프로세스 자체를 numactl로 특정 Node에 고정
// → EventLoop Thread 전체가 해당 NUMA Node 내에서 실행
// → Heap 할당 (-XX:+UseNUMA) → 동일 Node 메모리
// → NIC IRQ도 동일 Node CPU → 패킷 처리 Local Access
```

---

## 부록: CPU Socket 장애 분석 순서 (Java 결제 시스템)

```text
결제 P99 Latency 증가 또는 Throughput 감소 감지
  ↓
1. CPU Usage 확인 (top, mpstat)
   → Usage 낮은데 Latency 높음 → NUMA/Cache 문제 의심
  ↓
2. numastat -p <PID> → numa_miss 카운터 확인
   → 높으면 Remote Memory Access 과다 → UseNUMA 확인
  ↓
3. perf stat -e LLC-load-misses -p <PID>
   → LLC Miss Rate 높으면 → DRAM 직접 접근 과다
  ↓
4. mpstat -P ALL 1 → %soft 분포 확인
   → 특정 Core %soft 높으면 → NIC IRQ 집중 → IRQ Affinity 조정
  ↓
5. pidstat -t -p <PID> 1 → Thread CPU Migration 확인
   → 잦은 Migration → NUMA Balancer 영향 또는 Affinity 미설정
  ↓
6. async-profiler -e cache-misses → Flamegraph 분석
   → Cache Miss 핫스팟 메서드 식별 (Jackson, GC, 공유 카운터 등)
  ↓
7. JFR GC Pause 확인 + numastat 상관관계
   → GC 중 numa_miss 급증 → Cross-socket GC Scan → UseNUMA + ZGC 검토
  ↓
8. Kubernetes: kubectl describe node → Topology Manager 설정 확인
   → CPU Manager Static Policy, single-numa-node 설정 여부
  ↓
9. pcm.x 1 → UPI Bandwidth 확인
   → UPI 포화 → Remote Access 과다 → NUMA 배치 전면 재검토
```

---

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*