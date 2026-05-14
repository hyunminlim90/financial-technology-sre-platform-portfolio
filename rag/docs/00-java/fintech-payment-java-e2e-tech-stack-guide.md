# FinTech 결제 시스템 Java E2E 기술 스택 가이드

> 정독: 0회
> 
> 관점: SRE / Platform Engineering / Payment Reliability
> 범위: Hardware → OS Kernel → JVM Runtime → Concurrency → Network/Data Integration → Observability → 운영 전략
> 목적: 결제 시스템의 신뢰성, 정합성, 성능, 장애 분석 능력을 확보하기 위한 Java 기술 스택을 계층별로 정리한다.

---

## 1. 전체 E2E 구조

```text
[Payment Request]
  ↓
[API Gateway / Web Layer]
  ↓
[Java Runtime / JVM]
  ↓
[Thread / Virtual Thread / Event Loop]
  ↓
[Network I/O / DB I/O / Message Queue]
  ↓
[Kernel / Socket / File Descriptor / epoll / io_uring]
  ↓
[Observability / Tracing / Profiling]
  ↓
[SRE Operation / Incident Response]
```

결제 시스템에서 Java 기술 스택은 단순히 애플리케이션 코드를 실행하는 도구가 아니다.

결제 요청의 안정적인 수신, 외부 PG/카드사/은행 연동, DB 정합성 보장, 장애 감지, 지연 분석, 복구 전략까지 연결되는 E2E 실행 기반이다.

---

## 2. Runtime & Memory Architecture

### 2.1 JVM Memory

| 영역 | 역할 | 메커니즘 실체 | SRE 분석 도구 / 관찰 키워드 |
|------|------|--------------|--------------------------|
| Heap | Java 객체 저장 | Young(Eden + Survivor) / Old 세대 구조. TLAB(Thread-Local Allocation Buffer)를 통해 각 Thread가 Heap 일부를 선점하여 동기화 없이 객체 할당. Eden 고갈 시 Minor GC 트리거. | `jvm_memory_used_bytes`, `jvm_memory_committed_bytes`, JFR Allocation Profiling, `-Xlog:gc*` |
| Stack | Thread별 실행 프레임 | OS Thread 생성 시 Native Memory에서 Stack 공간 확보 (`-Xss` 설정). Frame 단위로 지역 변수, 피연산자 스택, 리턴 주소 저장. Stack Overflow는 재귀 깊이 초과 또는 대형 지역 변수 할당 시 발생. | `jvm_threads_live`, Thread Dump, `-Xss` 설정값 확인 |
| Metaspace | 클래스 메타데이터 저장 | Native Memory 영역. ClassLoader 단위로 클래스 메타데이터, 메서드 바이트코드, 상수 풀 저장. ClassLoader Leak 발생 시 Metaspace가 지속 증가하여 Native OOM 유발. | `jvm_memory_used_bytes{area="nonheap"}`, `jcmd <pid> VM.native_memory`, JFR ClassLoader 이벤트 |
| Code Cache | JIT 컴파일된 Native Code 저장 | C1/C2 Compiler가 Bytecode를 Native Code로 변환 후 저장. 가득 차면 JIT 컴파일이 중단되고 Interpreter 모드로 회귀하여 Latency 급증. | `jvm_compilation_time_seconds`, `CodeCache` JMX 지표, `-XX:ReservedCodeCacheSize` |
| Direct Memory | Off-heap Buffer | `ByteBuffer.allocateDirect()` / Netty `DirectByteBuf`가 사용. JVM GC 대상이 아니므로 명시적 해제 또는 GC에 의한 `Cleaner` 콜백으로만 반환. 누수 시 Native Memory 고갈로 `OutOfMemoryError: Direct buffer memory` 발생. | `-XX:MaxDirectMemorySize`, `BufferPoolMXBean`, JFR `DirectBufferStatisticsEvent` |

#### TLAB 동작 메커니즘

```text
Eden Space
  ├── Thread-A TLAB (선점 영역)
  ├── Thread-B TLAB (선점 영역)
  └── 공유 영역 (TLAB 재충전 대상)

객체 할당 흐름:
  new Object()
    → TLAB 내 bump-pointer 이동 (동기화 없음)
    → TLAB 소진 시 새 TLAB 요청 (JVM 락 필요)
    → Eden 소진 시 Minor GC
```

**SRE 관점:** 결제 요청 처리 중 DTO, Entity 객체가 대량 생성되는 경우 TLAB 재충전 빈도 증가로 Minor GC 간격이 짧아진다. JFR의 `ObjectAllocationInNewTLAB` 이벤트로 할당 핫스팟을 추적할 수 있다.

---

### 2.2 Garbage Collection

#### G1GC

G1GC는 Java 결제 시스템에서 기본적으로 사용되는 저지연 GC다.

**메커니즘 실체:**

```text
Heap → Region 배열 (기본 2048개)
  ├── Eden Region
  ├── Survivor Region
  ├── Old Region
  └── Humongous Region (Region 크기 50% 초과 객체)

Minor GC (Young Collection):
  Eden → Survivor / Old 이동 (STW)

Mixed GC:
  Young + Old Region 일부 수집 (STW)
  Concurrent Marking 후 Garbage 비율 높은 Region 선택

Full GC (최후 수단):
  Serial 방식 전체 압축 → STW 시간 수백 ms ~ 수 초
```

| 단계 | 동작 | STW 여부 |
|------|------|---------|
| Initial Mark | Old 객체 루트 마킹 | STW |
| Concurrent Mark | 전체 힙 참조 추적 | 동시 |
| Remark | 마킹 보정 (SATB) | STW |
| Cleanup | 빈 Region 회수 | STW (단시간) |
| Evacuation | 살아있는 객체 이동 | STW |

**SRE 확인 지표:**

| 지표 | 의미 | 임계 기준 |
|------|------|---------|
| `jvm_gc_pause_seconds` | GC STW 시간 | 결제 Timeout 설정값의 10% 이하 권장 |
| `jvm_memory_used_bytes` | 힙 사용량 | 80% 이상 지속 시 조사 필요 |
| `jvm_gc_memory_promoted_bytes` | Old 영역 승격량 | 급증 시 Memory Leak 의심 |
| `jvm_gc_memory_allocated_bytes` | 할당 속도 | 결제 TPS 대비 비정상 증가 확인 |

#### ZGC / Shenandoah

대용량 Heap과 낮은 Pause Time이 필요한 고성능 결제 처리 환경에서 사용한다.

**메커니즘 실체:**

| 항목 | ZGC | Shenandoah |
|------|-----|-----------|
| 객체 이동 방식 | Load Barrier + Colored Pointer로 참조 자동 보정 | Brooks Pointer(간접 참조 헤더)로 이동 중 참조 보정 |
| STW 범위 | Initial Mark, Remark만 STW (수 ms 수준) | 동일 구조, Concurrent Compaction 지원 |
| Heap 크기 | 수 TB까지 안정적 | 수백 GB 환경에 적합 |
| JDK 지원 | JDK 15+ Stable | JDK 12+, Red Hat 주도 |

**Colored Pointer (ZGC):**

```text
64bit 포인터 중 상위 비트를 메타데이터로 활용
  Bit 42: Marked0
  Bit 43: Marked1
  Bit 44: Remapped
  Bit 45: Finalizable

Load Barrier가 포인터 색상 확인 후 필요 시 재매핑 수행
→ 애플리케이션 실행 중 참조 갱신 가능
```

**SRE 관점:** 결제 시스템에서 GC Pause는 단순 성능 문제가 아니다. GC STW 동안 Kafka Consumer가 poll()을 호출하지 못해 session.timeout.ms를 초과하면 Rebalance가 트리거된다. 외부 PG API 응답 대기 중 GC가 발생하면 연결 Timeout이 발생할 수 있다.

---

### 2.3 JIT Compiler

JIT Compiler는 반복 실행되는 Bytecode를 Native Code로 변환하여 성능을 높인다.

**메커니즘 실체:**

```text
실행 흐름:
  Bytecode 로드
    → Interpreter 실행 (Profiling 데이터 수집)
    → 호출 횟수 임계치 도달 (CompileThreshold, 기본 10,000)
    → C1 Compiler: 빠른 최적화 (Tiered Compilation Level 1~3)
    → C2 Compiler: 고성능 최적화 (Level 4)
      - Inlining: 호출 비용 제거
      - Loop Unrolling: 루프 오버헤드 감소
      - Escape Analysis: Heap 할당을 Stack 할당으로 전환
      - Intrinsics: CRC32, AES 등 CPU 명령어 직접 매핑
```

| 구성 | 역할 | 트리거 조건 |
|------|------|-----------|
| Interpreter | Bytecode 즉시 실행 + Profiling | 초기 실행 |
| C1 Compiler | 빠른 최적화, Client 모드 | 호출 횟수 1,500 이상 |
| C2 Compiler | 고성능 최적화, Server 모드 | 호출 횟수 10,000 이상 |
| Code Cache | 컴파일된 Native Code 저장 | C1/C2 결과물 보관 |
| Deoptimization | 최적화 가정 실패 시 Interpreter 회귀 | 클래스 로딩, 타입 변경 등 |

**SRE 관점:**

- 서비스 기동 직후 Warm-up 미완료 구간에서 C1 → C2 전환이 진행되며 Latency가 불안정하다.
- 배포 직후 P99 Latency 스파이크는 JIT Warm-up이 원인일 수 있다.
- Canary 배포 시 Warm-up 구간(통상 5~10분)을 별도 고려해야 한다.
- JFR `Compilation` 이벤트로 컴파일 시간과 핫 메서드를 확인할 수 있다.

**SRE 분석 도구:**

| 도구 | 확인 항목 |
|------|---------|
| JFR Compilation Event | 컴파일 소요 시간, 컴파일된 메서드 목록 |
| `-XX:+PrintCompilation` | 실시간 JIT 컴파일 로그 |
| `jcmd <pid> Compiler.codecache` | Code Cache 현재 사용량 |
| `jstat -compiler` | 총 컴파일 횟수, 실패 횟수 |

---

### 2.4 SafePoint와 TTSP

SafePoint는 JVM이 모든 Java Thread를 안전하게 정지시킬 수 있는 지점이다.

**메커니즘 실체:**

```text
SafePoint 도달 메커니즘:
  JVM이 SafePoint 요청 (예: GC 필요)
    → 각 Thread의 SafePoint 폴링 주소에 접근 불가 페이지 설정
    → Thread가 폴링 시 SIGSEGV 발생 → JVM Signal Handler가 처리 → Thread 정지
    → TTSP(Time to Safepoint): 마지막 Thread가 SafePoint 도달까지 걸리는 시간
    → 전체 Thread 정지 완료 후 JVM 작업 수행 (GC, Thread Dump 등)
    → 작업 완료 후 Thread 재개
```

**SafePoint 사용 사례:**

- GC (모든 종류)
- Thread Dump 수집
- Deoptimization
- Class Redefinition (HotSwap)
- JVM 내부 상태 점검 (biased lock revocation 등)

**TTSP 지연 원인:**

| 원인 | 설명 | 확인 방법 |
|------|------|---------|
| JNI Critical Section | `GetPrimitiveArrayCritical` 호출 중 Thread가 SafePoint에 도달하지 못함 | JFR SafepointWait 이벤트 |
| 긴 루프 | 백오프 없는 루프에서 SafePoint 폴링 빈도 낮음 | `-XX:+UseCountedLoopSafepoints` |
| Counted Loop | 정수 카운터 루프는 기본적으로 SafePoint 폴링 생략 | 위 옵션으로 해결 |
| Native Method | Native 코드 실행 중 SafePoint 대기 | Thread Dump에서 상태 확인 |

| 항목 | 의미 |
|------|------|
| STW | 모든 Java Thread가 정지된 시간 |
| TTSP | 모든 Thread가 SafePoint에 도달하기까지 걸린 시간 |
| Safepoint Pause | TTSP + JVM 작업 시간 |

**SRE 관점:** TTSP가 길어지면 GC 작업 자체가 짧아도 전체 애플리케이션 정지 시간이 길어진다. 결제 시스템에서는 TTSP가 외부 API Timeout, 승인 지연, Kafka Rebalance로 이어질 수 있다.

**SRE 분석 도구:**

| 도구 | 확인 항목 |
|------|---------|
| JFR `SafepointWait` | TTSP 시간, 대기 Thread 정보 |
| `-Xlog:safepoint` | SafePoint 요청부터 완료까지 전체 타임라인 |
| JFR `SafepointBegin/End` | SafePoint 시작/종료 타임스탬프 |

---

### 2.5 ClassLoader와 Metaspace Leak

**메커니즘 실체:**

```text
ClassLoader 계층:
  Bootstrap ClassLoader (JVM 내장)
    → Platform ClassLoader (JDK 모듈)
      → Application ClassLoader (애플리케이션 클래스)
        → Custom ClassLoader (동적 플러그인, JSP 등)

Leak 발생 패턴:
  Custom ClassLoader로 클래스 로드
    → ClassLoader 참조를 Static 필드나 ThreadLocal에 보관
    → ClassLoader GC 불가 → 로드된 모든 클래스의 메타데이터가 Metaspace에 잔류
    → 반복 배포(Hot Deploy) 또는 JSP 재컴파일 시 누적 증가
    → Native OOM: Metaspace
```

**SRE 분석 도구:**

| 도구 | 확인 항목 |
|------|---------|
| `jcmd <pid> VM.native_memory` | Metaspace 실제 사용량 |
| JFR ClassLoaderStatistics | ClassLoader별 로드 클래스 수 |
| Heap Dump + MAT | ClassLoader 참조 체인 분석 |
| `-XX:+TraceClassLoading/Unloading` | 클래스 로드/언로드 로그 |

---

## 3. Concurrency & High Availability

### 3.1 Thread-per-request 모델

전통적인 Spring MVC / Servlet 기반 구조다.

```text
HTTP Request
  ↓
NIC → Kernel TCP Stack → Socket Buffer (sk_buff)
  ↓
epoll_wait() → Tomcat Acceptor
  ↓
Worker Thread 할당 (ThreadPoolExecutor)
  ↓
Servlet / DispatcherServlet
  ↓
Controller / Service
  ↓
DB 또는 외부 API 호출 (Blocking I/O)
```

**메커니즘 실체:**

| 구성 요소 | 실제 동작 |
|---------|---------|
| Tomcat Acceptor | `accept()` syscall로 연결 수락, Worker Thread에 위임 |
| Worker Thread | OS Thread(`clone()` syscall로 생성), Stack에 수 MB Native Memory 소비 |
| Blocking I/O | `read()` / `write()` syscall 호출 후 Thread가 Wait Queue로 이동 → Context Switch 발생 |
| Context Switch | `schedule()` 호출, CPU 레지스터 + TLB 상태 저장/복원, Cache Miss 유발 |

**OS 수준 Thread 상태:**

```text
Thread 상태 전이:
  RUNNABLE → Running (Scheduler 선택)
  Running → Blocked (I/O wait, Futex wait)
  Blocked → Runnable (I/O 완료 IRQ, Futex wake)

Futex (Fast Userspace Mutex):
  락 미경합 시 → User Space에서 atomic CAS로 처리 (syscall 없음)
  락 경합 시 → futex() syscall → Thread를 Wait Queue에 등록
  락 해제 시 → futex_wake() syscall → 대기 Thread 깨움
```

**SRE 위험 요소:**

| 위험 | 메커니즘 | 확인 지표 |
|------|---------|---------|
| Thread Pool Exhaustion | 모든 Worker Thread가 `futex_wait` 또는 I/O Block 상태 | `executor_active_threads`, Thread Dump |
| Context Switch 증가 | OS Scheduler 오버헤드 증가, L1/L2 Cache Miss 누적 | `vmstat cs`, `pidstat -w`, `perf stat -e context-switches` |
| Native Memory 증가 | Thread Stack 사용량 증가 (`-Xss` × Thread 수) | `jcmd VM.native_memory`, `/proc/<pid>/status VmRSS` |
| CPU Runqueue 지연 | Thread 수 > CPU 코어 수, Runnable 상태 누적 | `vmstat r`, `perf sched latency`, `/proc/schedstat` |
| False Sharing | 인접 Cache Line에 서로 다른 Thread가 쓰기, Cache Coherency 프로토콜(MESI) 교환 증가 | `perf stat -e cache-misses`, JFR |

---

### 3.2 Virtual Thread / Project Loom

Virtual Thread는 Java 21에서 정식 도입된 경량 Thread 모델이다.

**메커니즘 실체:**

```text
구조:
  Virtual Thread (수십만 개 생성 가능)
    → Carrier Thread (ForkJoinPool 기반 OS Thread, CPU 코어 수와 유사)

스케줄링:
  Virtual Thread가 Blocking 호출 진입
    → JVM이 Blocking 감지 (java.io / java.net / LockSupport.park 등)
    → Virtual Thread를 Continuation으로 직렬화하여 Heap에 저장
    → Carrier Thread에서 Virtual Thread 분리 (unmount)
    → Carrier Thread는 다른 Virtual Thread 실행
    → I/O 완료 시 Continuation 복원 → 사용 가능한 Carrier Thread에 재스케줄

Continuation 실체:
  Virtual Thread의 Stack Frame을 Heap에 직렬화한 구조
  Platform Thread Stack(수 MB) 대비 수 KB 수준
```

**Pinning 발생 조건 (Carrier Thread 고정):**

| 원인 | 설명 | 해결 방안 |
|------|------|---------|
| `synchronized` 블록 | Monitor Lock 보유 중 Blocking 시 Carrier Thread 고정 | `ReentrantLock`으로 교체 |
| Native Method | JNI 호출 중 Carrier Thread 고정 | 별도 Thread Pool로 격리 |
| JNI Critical Section | `GetPrimitiveArrayCritical` 호출 중 고정 | 사용 최소화 |
| JDBC Driver (구버전) | 동기식 Socket I/O 사용 시 Carrier Thread 점유 | R2DBC 또는 Virtual Thread 친화 Driver 사용 |

**SRE 확인 포인트:**

| 항목 | 확인 방법 |
|------|---------|
| Carrier Thread 사용률 | JFR `VirtualThreadPinned` 이벤트 |
| Pinning 발생 여부 | `-Djdk.tracePinnedThreads=full` |
| DB Driver Blocking 전파 | Thread Dump에서 Carrier Thread 상태 확인 |
| Virtual Thread 수 | JFR `ThreadStart/End`, `jcmd Thread.print` |
| Heap 사용량 증가 | Virtual Thread Continuation이 Heap에 적재됨 |

---

### 3.3 Lock-free 구조

고성능 결제 처리에서는 Lock 경합을 줄이는 것이 처리량과 지연 안정성을 결정한다.

**메커니즘 실체:**

```text
CAS (Compare-And-Swap):
  CPU 명령어: LOCK CMPXCHG (x86)
  동작: 메모리 값이 예상값과 일치할 때만 새 값으로 교체 (원자적)
  실패 시 재시도 (Spin Loop)
  → ABA 문제 가능: AtomicStampedReference로 해결

LMAX Disruptor:
  Ring Buffer (배열 기반) + CAS + 메모리 배리어
  Producer가 Sequence 선점 → Consumer가 이벤트 처리
  false sharing 방지: @Contended + 64byte padding
  컨텍스트 스위치 없이 Thread 간 이벤트 전달

LongAdder vs AtomicLong:
  AtomicLong: 단일 변수 CAS → 경합 시 Spin 증가
  LongAdder: Cell 배열 분산 → 각 Thread가 다른 Cell에 누적 → 합산 시 집계
  → 결제 TPS 카운터에 LongAdder가 더 적합

Cache Line과 False Sharing:
  CPU Cache Line = 64 bytes
  서로 다른 Thread가 같은 Cache Line 내 인접 변수에 쓰기
  → MESI 프로토콜에 의해 Cache Line 무효화/재전송 반복
  → @Contended 어노테이션으로 128byte 패딩 삽입하여 해결
```

**대표 기술:**

| 기술 | 역할 | 결제 시스템 적용 예 |
|------|------|-----------------|
| CAS / LOCK CMPXCHG | Atomic 연산 기반 동시성 제어 | 중복 결제 방지 카운터, 상태 전이 |
| LMAX Disruptor | Ring Buffer 기반 고성능 Event 처리 | 결제 이벤트 큐, 원장 기록 파이프라인 |
| LongAdder | 분산 카운터로 경합 완화 | 결제 TPS 측정, 성공/실패 카운터 |
| `@Contended` | Cache Line Padding으로 False Sharing 방지 | 공유 카운터 객체 |
| StampedLock | Read-heavy 구조에서 Optimistic Read 지원 | 환율 캐시, 설정 조회 |

**SRE 분석 도구:**

| 도구 | 확인 항목 |
|------|---------|
| JFR `JavaMonitorWait/Enter` | Monitor Lock 경합 빈도, 대기 시간 |
| JFR `ThreadPark` | LockSupport.park 발생 빈도 |
| `perf stat -e cache-misses` | False Sharing 의심 시 Cache Miss 측정 |
| Thread Dump (BLOCKED 상태 Thread) | Lock 보유 Thread 식별 |

---

## 4. Network & Data Integration

### 4.1 JDBC와 Connection Pool

결제 시스템에서 DB Connection Pool은 핵심 병목 지점이다.

**메커니즘 실체:**

```text
결제 요청 처리 중 DB I/O 흐름:
  Application (HikariCP)
    → getConnection() → Pool에서 Connection 획득
    → TCP Socket (keep-alive, 이미 연결된 상태)
    → SQL 전송: write() syscall → Kernel Send Buffer
    → NIC → 네트워크 → DB Server NIC
    → DB Server Kernel Receive Buffer → DB Process
    → 쿼리 실행 (B-Tree, Buffer Pool, WAL 기록)
    → 응답 전송: NIC → 네트워크
    → Application Kernel Receive Buffer → read() syscall
    → ResultSet 파싱

Connection Pool 내부:
  ConcurrentBag 구조 (HikariCP)
    → Thread-local 리스트에서 먼저 탐색 (빠른 경로)
    → 없으면 공유 리스트 탐색
    → 없으면 connectionTimeout까지 대기
    → 초과 시 SQLTimeoutException
```

**Connection Pool Exhaustion 전파 경로:**

```text
DB 쿼리 지연 (슬로우 쿼리, 락 경합)
  → Active Connection 수 증가
  → Pool 고갈 (Pending Threads 증가)
  → Worker Thread가 getConnection()에서 Block
  → Thread Pool 고갈
  → HTTP 요청 수락 불가 → 결제 실패
```

**주요 지표:**

| 지표 | 의미 | 임계 기준 |
|------|------|---------|
| `hikaricp_active_connections` | 사용 중인 DB 연결 수 | Pool 크기의 80% 이상 지속 시 경고 |
| `hikaricp_pending_threads` | 커넥션 대기 Thread 수 | 0보다 크면 즉시 조사 |
| `hikaricp_connection_timeout_total` | 커넥션 획득 실패 횟수 | 증가 시 결제 실패와 직결 |
| `hikaricp_connection_acquire_ms` | 커넥션 획득 소요 시간 | P99 기준 수십 ms 초과 시 조사 |

**SRE 분석 도구:**

| 도구 | 확인 항목 |
|------|---------|
| HikariCP JMX / Micrometer | Pool 상태 실시간 모니터링 |
| Thread Dump | `getConnection` 대기 Thread 확인 |
| `SHOW PROCESSLIST` (MySQL) | DB 서버 측 쿼리 실행 상태 |
| Slow Query Log | 쿼리 지연 원인 분석 |
| `perf stat`, `strace` | syscall 수준 I/O 지연 확인 |

---

### 4.2 WebFlux & Netty

WebFlux는 Reactive 기반의 Non-blocking Web Framework다.

**메커니즘 실체:**

```text
Request 처리 흐름:
  NIC 수신 → DMA → Kernel Ring Buffer
    → IRQ → ksoftirqd → sk_buff 생성 → TCP Stack
    → Socket Receive Buffer
    → epoll_wait() 깨움
    → Netty EventLoop Thread (CPU 코어당 1개)
      → ChannelPipeline Handler 체인 처리
      → WebFlux Router → Handler
        → Mono/Flux 체인 구성 (지연 실행)
        → Scheduler.boundedElastic() (Blocking 작업 전용) 또는
        → Scheduler.parallel() (Non-blocking 연산)
      → 응답 직렬화 → write() → Kernel Send Buffer → NIC
```

**Netty 내부 구조:**

| 구성 | 실제 동작 |
|------|---------|
| EventLoop | 단일 Thread, 1개의 Selector(epoll)를 무한 루프로 폴링 |
| Channel Pipeline | ChannelInboundHandler / ChannelOutboundHandler 체인 |
| ByteBuf | Direct Memory 기반 버퍼, Zero-copy 최적화 (`CompositeByteBuf`) |
| ChannelHandlerContext | Handler 간 이벤트 전파 컨텍스트 |

**Backpressure 메커니즘:**

```text
Publisher가 Subscriber 처리 속도 초과 시:
  Subscriber → request(n) 으로 처리 가능 개수 요청
  Publisher → n개 이하로 emit 제어
  초과 시 → onBackpressureBuffer() 버퍼링
          → onBackpressureDrop() 드롭
          → onBackpressureError() 에러

결제 시스템 적용:
  외부 PG API 응답 속도 < 내부 처리 속도 차이 발생 시
  Backpressure로 요청 속도 자동 조절 가능
```

**주의 사항:**

| 금지 패턴 | 영향 | 대안 |
|---------|------|------|
| EventLoop에서 `Thread.sleep()` | 전체 EventLoop 정지, 연결된 모든 Channel 지연 | `Mono.delay()` |
| EventLoop에서 JDBC 호출 | Blocking I/O가 EventLoop 점유 | `subscribeOn(Schedulers.boundedElastic())` |
| EventLoop에서 `synchronized` | Lock 대기 중 EventLoop 정지 | Reactive Lock 또는 별도 스케줄러 |
| EventLoop에서 파일 I/O | 동기 syscall이 EventLoop 블로킹 | `Schedulers.boundedElastic()` |

**SRE 분석 도구:**

| 도구 | 확인 항목 |
|------|---------|
| `reactor.netty.http.server.handler` 메트릭 | 요청 처리 시간 분포 |
| `netty_eventloop_*` 메트릭 | EventLoop Thread 사용률 |
| JFR `ThreadPark` | Reactive 체인 대기 시간 |
| Reactor 디버그 모드 (`Hooks.onOperatorDebug()`) | 체인 스택 추적 |

---

### 4.3 R2DBC

R2DBC는 관계형 DB에 대한 Non-blocking 접근 방식이다.

**메커니즘 실체:**

```text
R2DBC 쿼리 실행 흐름:
  Mono<Result> = connection.createStatement(sql).execute()
    → Non-blocking 방식으로 SQL 전송
    → TCP Socket에 write() 후 즉시 반환 (EventLoop Thread 미점유)
    → DB 응답 수신 시 epoll 이벤트 → 결과 emit
    → Reactive Chain에서 행 처리

Connection Pool (r2dbc-pool):
  PooledConnection 관리
  Max Size 초과 시 Mono가 대기 (Blocking 없이 backpressure 방식)
  Timeout 초과 시 에러 emit
```

**트랜잭션 관리:**

```text
R2DBC 트랜잭션:
  connection.beginTransaction()
    → Connection 단위 트랜잭션 (JDBC와 동일)
    → 단, Reactive Chain이 중단되면 커밋/롤백 누락 가능

Spring @Transactional + R2DBC:
  TransactionalOperator 또는 @Transactional(Reactive 지원)
  → Coroutine / Flux 경계에서 트랜잭션 Context 전파 필요
  → ThreadLocal 기반 트랜잭션 컨텍스트 사용 불가 (Reactor Context 사용)
```

**주의점:**

| 항목 | 설명 | 대응 방안 |
|------|------|---------|
| Driver 성숙도 | PostgreSQL(pgDriver), MySQL(r2dbc-mysql) 기능 차이 존재 | 사용 DB별 Driver 호환성 사전 검증 |
| Transaction 관리 | `TransactionalOperator` 명시 또는 `@Transactional` 적용 필요 | Spring Data R2DBC 활용 |
| Off-CPU 시간 증가 | Non-blocking 구조상 실제 처리 시간이 여러 스레드/스케줄러에 분산 | Tracing Span으로 실제 DB 대기 시간 측정 |
| Blocking 혼입 | Flux 체인 내 JDBC 호출 시 전체 구조의 Non-blocking 장점 상실 | `blockingWrapper`로 격리 |

**SRE 분석 도구:**

| 도구 | 확인 항목 |
|------|---------|
| `r2dbc_pool_acquired` | Pool에서 획득한 연결 수 |
| `r2dbc_pool_pending` | 연결 대기 요청 수 |
| OpenTelemetry Span | DB 쿼리 시작~완료 시간 |
| Reactor 디버그 모드 | 체인 내 에러 발생 위치 |

---

### 4.4 epoll / io_uring / Zero-copy

#### epoll

Linux의 고성능 I/O 이벤트 감시 메커니즘이다.

**메커니즘 실체:**

```text
epoll 동작:
  epoll_create1() → epoll 인스턴스 생성 (FD 반환)
  epoll_ctl(epfd, EPOLL_CTL_ADD, sockfd, event) → 관심 FD 등록
  epoll_wait(epfd, events, maxevents, timeout) → 이벤트 대기

이벤트 발생 경로:
  NIC 패킷 수신
    → DMA → Kernel Ring Buffer
    → IRQ → ksoftirqd (SoftIRQ)
    → sk_buff 생성 → TCP/IP Stack
    → Socket Receive Buffer 적재
    → Socket의 wait_queue 내 epoll callback 호출
    → epoll ready list에 FD 추가
    → epoll_wait() 반환 → EventLoop/Selector 깨움

Level-triggered vs Edge-triggered:
  LT(기본): 버퍼에 데이터 있으면 계속 이벤트 발생
  ET: 상태 변화 시에만 이벤트 발생, 반드시 전체 읽기 필요
  Netty: 기본적으로 Edge-triggered + 루프 읽기
```

**SRE 분석 도구:**

| 도구 | 확인 항목 |
|------|---------|
| `ss -s` | Socket 상태 요약 (ESTABLISHED, TIME_WAIT, CLOSE_WAIT 수) |
| `/proc/sys/net/ipv4/tcp_*` | TCP Backlog, SYN Queue 크기 설정 |
| `nstat -az` | 네트워크 통계 (RetransSegs, SynRetrans 등) |
| `perf trace -e 'syscalls:sys_enter_epoll_wait'` | epoll_wait 호출 빈도 |

#### TCP Backlog / SYN Queue

```text
TCP 3-way Handshake 중 연결 관리:
  SYN Queue (Incomplete Connection Queue):
    → SYN 수신 후 SYN+ACK 전송, ACK 대기 중인 연결 저장
    → `net.ipv4.tcp_max_syn_backlog` 크기 제한
    → 고갈 시 SYN Drop → 클라이언트 재시도 → 연결 지연

  Accept Queue (Complete Connection Queue):
    → 3-way Handshake 완료 후 `accept()` 호출 대기
    → `listen()` backlog 파라미터로 크기 결정
    → 고갈 시 신규 연결 Drop → 결제 요청 유실 가능

결제 트래픽 급증 시:
  Accept Queue 고갈 → 연결 Drop
  → 클라이언트 Connect Timeout
  → 결제 실패
```

**SRE 분석 도구:**

| 도구 | 확인 항목 |
|------|---------|
| `ss -lnt` | Listen 소켓의 Recv-Q (Accept Queue 대기 수) |
| `netstat -s \| grep overflow` | Accept Queue Overflow 횟수 |
| `/proc/sys/net/core/somaxconn` | Accept Queue 최대 크기 |
| `nstat TcpExtListenDrops` | Listen Drop 발생 횟수 |

#### io_uring

io_uring은 Linux 5.1 이상에서 제공되는 고성능 비동기 I/O 인터페이스다.

**메커니즘 실체:**

```text
구성:
  SQ Ring (Submission Queue): 애플리케이션이 I/O 요청 제출
  CQ Ring (Completion Queue): 커널이 완료 결과 기록
  → 두 링 모두 User/Kernel 공유 메모리 (mmap)

동작 흐름:
  io_uring_setup() → SQ/CQ Ring 생성
  io_uring_enter() → SQ 항목 커널에 제출 (선택적 syscall)
    → IORING_FEAT_SQPOLL: 커널 폴링 스레드가 SQ를 지속 감시
       → 애플리케이션이 syscall 없이 SQ에 요청 추가
       → 커널 스레드가 즉시 처리
  완료 시 CQ에 결과 기록
  애플리케이션이 CQ를 폴링하여 결과 수집

기존 epoll 대비 장점:
  Syscall 횟수 감소 (배치 제출 가능)
  User/Kernel Mode 전환 감소
  Zero-copy 지원 (IORING_OP_SEND_ZC)
  다중 I/O 유형 통합 (파일, 소켓, 파이프 등)
```

| 구성 | 역할 |
|------|------|
| SQ | 애플리케이션이 커널에 I/O 작업 제출 |
| CQ | 커널이 완료 결과 기록 |
| Ring Buffer | User/Kernel 공유 메모리로 복사 없는 통신 |
| SQPOLL | 커널 폴링 스레드로 syscall 제거 |

**SRE 분석 도구:**

| 도구 | 확인 항목 |
|------|---------|
| `/proc/<pid>/fdinfo` | io_uring FD 정보 |
| `strace -e io_uring_enter,io_uring_setup` | io_uring syscall 추적 |
| BPF / bpftrace | io_uring 완료 이벤트 추적 |
| 커널 버전 확인 (`uname -r`) | io_uring 기능별 최소 커널 버전 확인 |

#### Zero-copy

Zero-copy는 Kernel/User Space 간 데이터 복사를 줄이는 최적화다.

**메커니즘 실체:**

```text
기존 방식 (데이터 4회 복사):
  Disk → Kernel Page Cache (1)
  Kernel Page Cache → User Buffer (2, read() syscall)
  User Buffer → Kernel Socket Buffer (3, write() syscall)
  Kernel Socket Buffer → NIC (4, DMA)

sendfile() Zero-copy:
  Disk → Kernel Page Cache (1, DMA)
  Kernel Page Cache → NIC (2, DMA)
  → User Space 복사 없음, syscall 2회 → 1회

Netty Zero-copy:
  DirectByteBuf: JVM Heap 외부 Direct Memory 사용
    → write() 시 JVM Heap → Native Memory 복사 생략
  CompositeByteBuf: 여러 버퍼를 논리적으로 합성 (물리 복사 없음)
  FileRegion: sendfile() 기반 파일 전송
```

**SRE 분석 도구:**

| 도구 | 확인 항목 |
|------|---------|
| `perf trace -e syscalls:sys_enter_sendfile` | sendfile 호출 빈도 |
| `jcmd VM.native_memory` | Direct Memory 사용량 |
| `ss -m` | Socket Buffer 사용량 |

---

### 4.5 Serialization / Deserialization 비용

결제 API에서 JSON/Protobuf 직렬화 비용은 숨겨진 CPU 병목이 될 수 있다.

**메커니즘 실체:**

```text
JSON 직렬화 (Jackson):
  Java Object → Reflection → JSON 문자열 → byte[]
  → Reflection 비용: 첫 호출 시 높음, 이후 캐시
  → 큰 DTO (중첩 객체, 컬렉션) → CPU 및 GC 압박

Protobuf:
  IDL 기반 코드 생성 → Reflection 없음
  Binary 포맷 → 파싱 속도 빠름
  네트워크 전송량 감소

역직렬화 비용:
  외부 PG 응답 파싱
  Kafka 메시지 파싱 (Avro, JSON)
  → 결제 처리량이 높을수록 누적 CPU 비용 증가
```

**SRE 분석 도구:**

| 도구 | 확인 항목 |
|------|---------|
| JFR Method Profiling | 직렬화 메서드 CPU 시간 비율 |
| `async-profiler` | Jackson/Gson/Protobuf 관련 핫 메서드 |
| JMH Benchmark | 직렬화 라이브러리별 처리 성능 비교 |

---

### 4.6 Kafka Integration

**메커니즘 실체:**

```text
Kafka Producer (결제 이벤트 발행):
  PaymentEvent → Serializer → ProducerRecord
    → Partitioner (결제 ID 기반 파티션 선택)
    → RecordAccumulator (배치 버퍼)
    → Sender Thread → NetworkClient → TCP Send Buffer
    → Broker 수신 → ISR 복제 → Ack 반환

Kafka Consumer (결제 후처리):
  poll(timeout) → Fetch 요청 → Broker
    → ConsumerRecord 처리
    → commitSync() / commitAsync()
    → Heartbeat Thread → session.timeout.ms 유지

Consumer Group Rebalance 트리거:
  - GC STW > session.timeout.ms
  - poll() 간격 > max.poll.interval.ms
  - Consumer Pod 강제 종료
  → 재조정 중 메시지 처리 중단 → 결제 후처리 지연
```

**주요 지표:**

| 지표 | 의미 | 임계 기준 |
|------|------|---------|
| `kafka_consumer_lag` | 처리 미완료 메시지 수 | 급증 시 Consumer 처리 지연 |
| `kafka_commit_latency` | 오프셋 커밋 시간 | 지연 시 중복 처리 위험 |
| `kafka_rebalance_rate` | 리밸런스 발생 빈도 | 증가 시 Consumer 불안정 |

**SRE 분석 도구:**

| 도구 | 확인 항목 |
|------|---------|
| `kafka-consumer-groups.sh --describe` | Consumer Lag, 파티션별 오프셋 |
| Kafka JMX / Micrometer | Producer/Consumer 메트릭 |
| JFR | GC Pause와 Rebalance 시간 상관관계 |

---

## 5. Observability & SRE Operations

### 5.1 Metrics

**대표 도구:** Micrometer, Prometheus, Grafana, Spring Boot Actuator

**핵심 지표:**

| 영역 | 지표 | 메커니즘 연결 |
|------|------|------------|
| 결제 | 성공률, 실패율, 승인 지연 P95/P99 | Circuit Breaker 상태, 외부 API Span |
| JVM | Heap 사용량, GC Pause, Thread 수, ClassLoader 수 | TLAB 할당, Metaspace Leak |
| HTTP | RPS, P95/P99, 4xx/5xx 비율 | Thread Pool 상태, Connection 상태 |
| DB | Connection Pool Active/Pending, Query Latency P99 | HikariCP, 슬로우 쿼리 |
| Kafka | Consumer Lag, Commit Latency, Rebalance 횟수 | GC Pause 연관성 |
| System | CPU Utilization, Load Average, Context Switch/s | Runqueue 길이, SoftIRQ 비율 |
| Network | TCP Retransmission, Socket Buffer 사용률, FD 사용량 | sk_buff, Accept Queue |
| PSI | CPU/Memory/IO Pressure Stall | `/proc/pressure/cpu`, `/proc/pressure/memory` |

**PSI (Pressure Stall Information):**

```text
/proc/pressure/cpu:
  some avg10=0.50 avg60=0.30 avg300=0.20 total=...
  full avg10=0.00 ...

  some: 일부 Task가 CPU를 기다리며 정지한 비율
  full: 모든 실행 가능 Task가 CPU를 기다린 비율 (전체 정지)

결제 시스템에서 PSI CPU some > 10%가 지속되면
  → Runnable Thread가 CPU를 기다리는 상태
  → 결제 처리 지연 직결
```

---

### 5.2 Tracing

OpenTelemetry를 통해 결제 요청의 전체 경로를 추적한다.

```text
Client
  ↓
API Gateway (Span 1)
  ↓
Payment API (Span 2)
  ├── Risk/FDS 호출 (Span 3)
  ├── Ledger 기록 (Span 4)
  └── External PG/Card Company (Span 5)
        ↓
        DB / Kafka (Span 6, 7)
```

**메커니즘 실체:**

```text
W3C TraceContext:
  HTTP Header: traceparent: 00-{traceId}-{spanId}-{flags}
  → 서비스 간 Trace Context 전파

OpenTelemetry Java Agent (javaagent):
  JVM 바이트코드 조작 (ByteBuddy)
  → HTTP Client, JDBC, Kafka 등 자동 계측
  → Span 시작/종료 시점 자동 기록

Span 데이터:
  Trace ID, Span ID, Parent Span ID
  시작 시각, 종료 시각, 소요 시간
  Span 종류 (CLIENT, SERVER, PRODUCER, CONSUMER)
  Attributes (HTTP URL, DB SQL, Kafka Topic 등)
  Events (에러, 재시도 시점)
```

**Off-CPU Time 분석:**

```text
CPU에서 실행되지 않는 시간 (Off-CPU Time):
  - I/O 대기 (DB, 외부 API, Kafka)
  - Lock 대기 (Futex, Monitor)
  - GC STW
  - Scheduler 대기 (Runqueue)

On-CPU Time만으로 병목을 찾으면 I/O 지연 원인을 놓칠 수 있다.
async-profiler의 wall-clock 모드로 Off-CPU 포함 전체 시간 프로파일링 가능.
```

**SRE 관점:** Tracing은 느린 구간을 추측하는 도구가 아니라, 어느 Span에서 실제 시간이 소비되었는지 증명하는 도구다.

**SRE 분석 도구:**

| 도구 | 확인 항목 |
|------|---------|
| Jaeger / Tempo | Trace 시각화, 느린 Span 탐색 |
| OpenTelemetry Java Agent | 자동 계측, 수동 Span 추가 |
| `async-profiler -e wall` | Off-CPU Time 포함 프로파일링 |

---

### 5.3 JFR

JFR(Java Flight Recorder)은 운영 환경에서 JVM 내부 이벤트를 낮은 오버헤드로 기록한다.

**메커니즘 실체:**

```text
JFR 데이터 수집 구조:
  JVM 내부 이벤트 발생
    → Event Buffer (Thread-local, 동기화 없음)
    → Global Buffer (주기적 플러시)
    → .jfr 파일 또는 스트리밍 API

오버헤드: 일반적으로 1~2% CPU 미만

수집 방법:
  1. 시작 시 활성화: -XX:StartFlightRecording=...
  2. 실행 중 시작: jcmd <pid> JFR.start
  3. 지속 기록: dumponexit=true

분석 도구:
  JDK Mission Control (JMC): GUI 분석
  JFR Event Streaming API (JDK 14+): 실시간 처리
```

**분석 가능 항목:**

| 이벤트 | 분석 내용 | 결제 시스템 관련성 |
|------|---------|----------------|
| GC Pause | STW 시간, 원인, 회수량 | Kafka Rebalance, Timeout 원인 |
| ObjectAllocationInNewTLAB | 할당 핫스팟, 할당 스레드 | 결제 DTO 메모리 압박 |
| JavaMonitorWait/Enter | Lock 경합, 대기 시간 | 중복 결제 방지 Lock 분석 |
| ThreadPark | LockSupport.park 원인, 대기 시간 | Reactive 체인 지연 |
| SocketRead/Write | TCP I/O 지연 시간, 주소 | 외부 PG API 지연 추적 |
| FileRead/Write | 파일 I/O 지연 | 로그 쓰기 병목 |
| MethodProfiling | CPU 핫 메서드 | 직렬화, 비즈니스 로직 병목 |
| SafepointWait | TTSP 지연 원인 Thread | 승인 지연 원인 분석 |
| VirtualThreadPinned | Pinned Carrier Thread | Virtual Thread 병목 |

**장애 분석 활용:**

```text
P99 Latency 증가 감지
  ↓
JFR 수집 (jcmd JFR.dump)
  ↓
GC Pause 확인 → STW가 원인인가?
  ↓
Lock Contention 확인 → 특정 Monitor에 병목 있는가?
  ↓
Socket I/O 확인 → 외부 API 응답 지연인가?
  ↓
Thread Park 확인 → Reactive 체인 대기인가?
  ↓
CPU Hot Method 확인 → 직렬화/비즈니스 로직 병목인가?
```

---

### 5.4 eBPF

eBPF는 커널 수준 이벤트를 안전하게 관측하는 기술이다.

**메커니즘 실체:**

```text
eBPF 프로그램 동작:
  eBPF 프로그램 작성 (C 서브셋)
    → LLVM/Clang으로 eBPF Bytecode 컴파일
    → bpf() syscall로 커널에 로드
    → Verifier 검증 (무한루프, 메모리 범위 검사)
    → JIT 컴파일 (eBPF Bytecode → Native Code)
    → 훅 포인트 연결 (kprobe, tracepoint, XDP, TC 등)

데이터 공유:
  eBPF Map (Hash, Array, Ring Buffer 등)
  → 커널 eBPF 프로그램이 Map에 데이터 기록
  → User Space 도구가 Map 읽기

훅 포인트:
  kprobe/kretprobe: 커널 함수 진입/반환
  uprobe/uretprobe: User Space 함수 진입/반환
  tracepoint: 커널 정적 이벤트
  USDT: 애플리케이션 정적 추적 포인트
  XDP: NIC 수신 직후 패킷 처리
```

**활용 영역:**

| 영역 | 분석 내용 | 도구 |
|------|---------|------|
| Network | TCP Retransmission, RTT, SYN Drop, sk_buff 흐름 | `tcpretrans`, `tcplife`, `tcpconnlat` (BCC) |
| Scheduler | Runqueue Latency, Context Switch, Off-CPU Time | `runqlat`, `offcputime` (BCC) |
| Disk I/O | Block I/O Latency, blk-mq 요청 큐 지연 | `biolatency`, `biosnoop` (BCC) |
| Syscall | `read/write/connect/accept` 지연 분포 | `syscount`, `funclatency` (BCC) |
| CPU | Kernel/User Time 분리, CPU Frequency Scaling | `cpudist`, `hardirqs` (BCC) |
| JVM (uprobe) | GC Pause, TLAB 할당, Lock 취득 | `javaagent + USDT probe` |

**CPU Frequency Scaling (C-state / P-state):**

```text
P-state (Performance State):
  CPU가 주파수/전압을 동적으로 조절
  → 저부하 시 낮은 주파수 → 높은 부하 시 첫 처리에서 주파수 상승 지연
  → 결제 처리 지연 시 P-state 상태 확인 필요
  확인: /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq

C-state (Idle State):
  CPU가 유휴 시 깊은 절전 상태 진입
  → C6/C7 상태에서 깨어나는 데 수십 μs 지연 발생
  → 레이턴시 민감 시스템에서는 C-state 비활성화 고려
  확인: /sys/devices/system/cpu/cpu*/cpuidle/state*/usage
  비활성화: intel_idle.max_cstate=0 또는 BIOS 설정
```

**SRE 관점:** JVM 내부 지표만으로 원인을 찾을 수 없을 때, eBPF는 커널 수준에서 병목을 확인하는 보조 증거가 된다.

**SRE 분석 도구:**

| 도구 | 확인 항목 |
|------|---------|
| BCC Tools (`/usr/share/bcc/tools/`) | 네트워크, 스케줄러, 디스크 I/O 분석 |
| bpftrace | 원라이너 eBPF 스크립트 |
| Pixie | Kubernetes 환경 eBPF 기반 관측 |
| Cilium Hubble | 서비스 메쉬 네트워크 가시성 |

---

### 5.5 Chaos Engineering

결제 시스템에서 Chaos Engineering은 단순 장애 실험이 아니라 복구 가능성 검증이다.

**메커니즘 실체:**

```text
Circuit Breaker (Resilience4j):
  Closed → 정상 처리
    → 실패율 임계치 초과 시 → Open (차단)
      → 일정 시간 후 → Half-Open (일부 허용)
        → 성공 시 → Closed / 실패 시 → Open 복귀

  외부 PG API 장애 시:
    Circuit Breaker Open → Fallback 처리 (결제 대기, 에러 반환)
    → PG 복구 후 Half-Open → 점진적 재개

Retry Storm:
  여러 Client가 동시에 실패 후 동시에 재시도
  → 이미 과부하된 서버에 더 큰 부하 유발
  → Exponential Backoff + Jitter로 재시도 분산
  → Circuit Breaker와 함께 사용하여 차단

Timeout 설계:
  Connection Timeout: TCP 연결 자체 타임아웃 (네트워크 지연)
  Read Timeout: 연결 후 응답 대기 타임아웃 (처리 지연)
  Call Timeout: 전체 호출 시간 타임아웃
  → 외부 PG의 SLA에 맞춰 설계
```

**실험 예시:**

| 실험 | 검증 목적 | 메커니즘 관련 항목 |
|------|---------|----------------|
| DB 지연 주입 | Connection Pool 고갈 방어, Timeout 동작 | HikariCP Pending, Thread Dump |
| 외부 PG Timeout | Circuit Breaker 동작, Fallback 처리 | Resilience4j 메트릭, Trace Span |
| Kafka 지연 | Consumer Lag 대응, GC와의 상관관계 | Kafka Consumer Lag, GC Pause |
| Pod 강제 종료 | Graceful Shutdown, 재시작 시 Warm-up 소요 | JIT Warm-up, 연결 재수립 |
| Network Packet Loss | Retry/Timeout 정책 검증, TCP Retransmission | `nstat`, eBPF `tcpretrans` |
| CPU Throttling (cgroup) | P99 Latency 방어, PSI 대응 | `/sys/fs/cgroup/cpu.stat`, `kubectl top` |
| OOM 유발 | OOM Killer 동작, 복구 절차 | `/proc/<pid>/oom_score`, dmesg |

**cgroup CPU Throttling 메커니즘:**

```text
cgroup v2 CPU 제어:
  cpu.max = "quota period"
    예: "50000 100000" → 100ms 중 50ms만 CPU 사용 가능 (50% 제한)

  Quota 소진 시:
    → Container/Pod의 모든 Thread가 Throttle 상태
    → Runnable 상태이나 CPU 사용 불가
    → 결제 처리 P99 Latency 급증

CPU Burst:
  cpu.max.burst → 순간 피크 허용량 설정
  → 짧은 결제 처리 피크를 흡수 가능

확인:
  /sys/fs/cgroup/cpu.stat의 throttled_usec
  cadvisor container_cpu_throttled_seconds_total
```

---

## 6. Security & Supply Chain

### 6.1 SBOM

SBOM(Software Bill of Materials)은 사용 중인 라이브러리와 버전을 명세화한 소프트웨어 구성 목록이다.

**메커니즘 실체:**

```text
SBOM 생성 시점:
  빌드 시점 (Maven/Gradle 의존성 트리 분석)
    → CycloneDX / SPDX 형식으로 출력
    → 각 라이브러리: 이름, 버전, 라이선스, 해시, CVE 정보

Log4Shell 사례:
  CVE-2021-44228: log4j-core 2.14.1 이하
  → SBOM이 없으면 수천 개 서비스에서 취약 버전 탐색에 수 일 소요
  → SBOM이 있으면 log4j-core 포함 서비스 즉시 식별 가능
```

**필요성:**

- Log4Shell 같은 공급망 취약점 대응
- 취약 라이브러리 즉시 식별 및 패치 우선순위 결정
- 빌드 파이프라인 내 자동 보안 검증
- 금융 당국 감사 및 컴플라이언스 대응

---

### 6.2 CI/CD 보안

**권장 구성:**

```text
Source Code
  ↓
Dependency Scan (OWASP Dependency Check, Snyk)
  → 알려진 CVE 포함 라이브러리 탐지
  ↓
SAST (SonarQube, SpotBugs)
  → SQL Injection, 하드코딩 시크릿, 안전하지 않은 난수 탐지
  ↓
SBOM 생성 (CycloneDX Maven/Gradle Plugin)
  ↓
Container Image Scan (Trivy, Grype)
  → 베이스 이미지 취약점, 설치된 패키지 CVE 탐지
  ↓
Policy Gate (OPA, Kyverno)
  → Critical CVE 포함 시 배포 차단
  ↓
Deploy Approval
  → 검증된 아티팩트만 운영 환경 배포
```

**핀테크 관점:**

결제 시스템에서는 빠른 배포보다 검증된 배포가 우선이다. 취약점이 포함된 라이브러리는 기능 정상 여부와 무관하게 차단되어야 한다.

---

## 7. 결제 시스템 E2E 장애 분석 모델

### 7.1 요청 지연 발생 시 분석 순서

```text
1. 결제 성공률 / 실패율 확인 (Prometheus, Grafana)
  ↓
2. HTTP Latency P95/P99 확인 → Trace Span으로 느린 구간 특정
  ↓
3. JVM GC / Thread 상태 확인 (JFR, Thread Dump)
  ↓
4. DB Connection Pool 확인 (HikariCP Pending, 슬로우 쿼리)
  ↓
5. 외부 API Span 확인 (PG, 카드사 응답 시간)
  ↓
6. Kafka Consumer Lag 확인 (GC Pause와 상관관계)
  ↓
7. Kernel CPU / Network / FD 확인 (PSI, ss, nstat, eBPF)
  ↓
8. cgroup CPU Throttling 확인 (cpu.stat throttled_usec)
  ↓
9. 최근 배포 / 설정 변경 확인 (JIT Warm-up 고려)
```

### 7.2 주요 장애 패턴

| 장애 | 주요 원인 | 메커니즘 실체 | 확인 지표 |
|------|---------|------------|---------|
| 결제 승인 지연 | 외부 PG/API 지연 | TCP RTT 증가, 상대 서버 처리 지연 | Trace Span (외부 API), `tcplife` eBPF |
| Thread Pool 고갈 | Blocking I/O 증가 | Futex 대기, I/O Block, Context Switch 증가 | Thread Dump, `hikaricp_pending_threads` |
| DB Connection 고갈 | 슬로우 쿼리, Lock 경합 | DB 측 Lock Wait, Index 미사용 | HikariCP Pending, Slow Query Log |
| GC Pause | TLAB 할당 폭증, Old 승격 증가 | Minor GC 간격 단축, Mixed GC 빈도 증가 | `jvm_gc_pause_seconds`, JFR GC 이벤트 |
| Kafka Lag | Consumer 처리 지연, GC STW | GC STW > session.timeout.ms → Rebalance | Consumer Lag, GC Pause 상관관계 |
| CPU Saturation | Runnable Thread 증가, SoftIRQ 폭증 | Runqueue 누적, ksoftirqd CPU 점유 | Load Average, `mpstat %soft`, PSI |
| Network 지연 | TCP Retransmission, Buffer 고갈 | sk_buff 손실, Socket Send Buffer 고갈 | `nstat RetransSegs`, eBPF `tcpretrans` |
| FD 고갈 | 연결 누수 (TIME_WAIT 누적, Socket 미닫힘) | `ulimit -n` 초과 → `accept()` 실패 | `lsof -p <pid> \| wc -l`, `ss -s` |
| OOM | Heap 누수, Metaspace 누수, Direct Memory 누수 | Old 영역 고갈 → Full GC → OOM Killer | Heap Dump, `dmesg \| grep oom-kill` |
| Retry Storm | Circuit Breaker 미적용, 동시 재시도 | 실패한 서비스에 재시도가 집중 | 외부 API 호출 RPS 급증, 오류율 |

---

## 8. 기술 채택 전략

### 8.1 안정성 우선 원칙

결제 시스템에서는 최신 기술 도입보다 다음 기준이 우선이다.

```text
정확성 > 안정성 > 관측 가능성 > 성능 > 개발 편의성
```

| 원칙 | 설명 |
|------|------|
| 정확성 | 결제 금액, 승인 상태, 원장 기록의 정합성 (멱등성, 2PC, Saga 패턴) |
| 안정성 | 장애 시 복구 가능성, Circuit Breaker, Graceful Degradation |
| 관측 가능성 | 장애 원인을 신속하게 증명할 수 있는 Metrics, Trace, Log, Profiling |
| 성능 | 처리량, 지연 시간 (정확성과 안정성을 해치지 않는 범위) |
| 개발 편의성 | 코드 가독성, 프레임워크 추상화 |

---

### 8.2 권장 조합

| 계층 | 안정적 선택 | 고성능 선택 | 고려 사항 |
|------|-----------|-----------|---------|
| Web | Spring MVC / Tomcat | WebFlux / Netty | Blocking 코드 혼입 여부 |
| Thread | Platform Thread | Virtual Thread (JDK 21+) | Pinning, JDBC Driver 호환성 |
| DB | JDBC + HikariCP | R2DBC | Driver 성숙도, 트랜잭션 설계 복잡도 |
| GC | G1GC | ZGC / Shenandoah | Heap 크기, Pause 허용 시간 |
| I/O 이벤트 | epoll (Netty 기본) | io_uring | 커널 버전 (5.1+), 라이브러리 지원 |
| Observability | Prometheus + Log | OTel + JFR + eBPF | 운영 팀 역량 |
| 직렬화 | Jackson | Protobuf / Avro | 내외부 API 호환성 |
| Security | Dependency Scan | SBOM + Policy Gate | CI/CD 파이프라인 통합 |

---

## 9. 최종 요약

Java 기반 핀테크 결제 시스템은 다음 계층이 하나의 E2E 구조로 연결되어야 안정적으로 운영된다.

```text
Hardware (CPU P/C-state, Cache, NUMA, NIC DMA)
  ↓
OS Kernel (IRQ, SoftIRQ, Scheduler, epoll, sk_buff, cgroup)
  ↓
JVM (TLAB, GC, JIT, SafePoint, ClassLoader, Direct Memory)
  ↓
Thread / Virtual Thread / Event Loop (Futex, Pinning, Context Switch)
  ↓
Network / DB / Kafka I/O (Connection Pool, Backpressure, Circuit Breaker)
  ↓
Kernel Socket / FD / epoll / io_uring (TCP Stack, Accept Queue, Zero-copy)
  ↓
Metrics / Tracing / JFR / eBPF (Off-CPU, PSI, Safepoint, Span)
  ↓
SRE Incident Response (장애 분석 모델, Chaos Engineering, SBOM)
```

**핵심 정리:**

| 항목 | 내용 |
|------|------|
| GC Pause | Kafka session.timeout 초과 → Rebalance, 외부 PG Timeout으로 전파 가능 |
| Thread Pool 고갈 | CPU 사용률이 낮아도 Futex 대기 / I/O Block으로 서비스 장애 발생 |
| WebFlux EventLoop | Blocking 호출 혼입 시 전체 EventLoop 정지 → 연결 전체 지연 |
| R2DBC | E2E Non-blocking 구조 완성, 단 트랜잭션 설계와 디버깅 난이도 존재 |
| Virtual Thread Pinning | `synchronized` + Blocking → Carrier Thread 고정 → 처리량 저하 |
| JFR | JVM 내부 병목(GC, Lock, I/O, SafePoint, Allocation)의 핵심 분석 도구 |
| eBPF | Kernel 수준 병목(Scheduler, Network, Disk, Syscall)의 보조 증거 수집 |
| TTSP | GC 시간이 짧아도 SafePoint 도달 지연으로 전체 애플리케이션 정지 가능 |
| SBOM + Scan | 핀테크 CI/CD의 필수 보안 게이트, 공급망 취약점 대응 |
| 설계 원칙 | 성능보다 정합성, 멱등성, 복구 가능성이 우선 |

결제 시스템의 Java 기술 스택은 단순한 프레임워크 선택 문제가 아니라, Hardware, OS Kernel, JVM Runtime, Concurrency, I/O, Network, Observability, Security가 연결된 E2E 신뢰성 설계 문제다.