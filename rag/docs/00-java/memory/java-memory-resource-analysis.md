# Java Memory Resource Analysis
## FinTech 결제 시스템 SRE 관점 | E2E Memory 분석

> 정독: 0회
> 
> 관점: SRE / Platform Engineering / Payment Reliability  </br>
> 범위: Hardware → MMU → OS Kernel → JVM Runtime → Framework → Application  </br>
> 목적: Java 기반 결제 시스템의 메모리 자원을 계층별로 분석하여 신뢰성·정합성·성능 확보 </br>

---

## 목차

1. [물리/가상 메모리 스펙 확인 지표](#1-물리가상-메모리-스펙-확인-지표)
2. [Java Memory E2E 실행 흐름](#2-java-memory-e2e-실행-흐름)
3. [Java Memory 사용 메커니즘 및 메모리 모델](#3-java-memory-사용-메커니즘-및-메모리-모델)
4. [Memory 병목 및 위험 발생 지점](#4-memory-병목-및-위험-발생-지점)
5. [SRE 관점 모니터링 지표](#5-sre-관점-모니터링-지표)
6. [장애 시나리오](#6-장애-시나리오)
7. [튜닝 포인트](#7-튜닝-포인트)
8. [관련 Linux 명령어 및 분석 도구](#8-관련-linux-명령어-및-분석-도구)

---

## 1. 물리/가상 메모리 스펙 확인 지표

### 1.1 DIMM / Channel / NUMA 구조

결제 시스템 서버에서 메모리 물리 구성은 JVM의 GC 처리량과 Latency에 직접적인 영향을 준다.

```text
Physical Memory 구조:
  CPU Socket 0                CPU Socket 1
  ├── NUMA Node 0             ├── NUMA Node 1
  │   ├── L1/L2/L3 Cache      │   ├── L1/L2/L3 Cache
  │   └── Local DIMM Banks    │   └── Local DIMM Banks
  │       (예: 128GB)         │       (예: 128GB)
  └── QPI/UPI Interconnect ──→ └── Remote Memory Access (+수십 ns 지연)

DIMM 채널 구성 (Dual Channel 예시):
  Channel A: DIMM 0, DIMM 1
  Channel B: DIMM 2, DIMM 3
  → 대역폭: 채널 수 × DIMM 속도 (DDR4-3200 × 2채널 = 약 51.2 GB/s)
```

| 확인 항목 | 명령어 | SRE 분석 이유 |
|---------|-------|------------|
| DIMM 속도/슬롯 | `dmidecode -t memory` | 메모리 대역폭 한계 파악. GC Copy 속도에 직결 |
| NUMA 토폴로지 | `numactl -H` | JVM이 Remote NUMA 메모리 접근 시 추가 지연 발생 |
| NUMA 바인딩 상태 | `numastat -p <pid>` | JVM 프로세스의 Local/Remote 메모리 사용 비율 |
| CPU-메모리 거리 | `numactl --hardware` | NUMA 노드 간 메모리 접근 비용 |

**결제 시스템 NUMA 영향:**
JVM의 G1GC/ZGC가 메모리 Region을 할당할 때 NUMA-Aware 옵션(`-XX:+UseNUMA`)이 비활성화된 경우 Remote NUMA 접근이 빈번해져 GC Pause 시간이 증가하고 결제 처리 Tail Latency가 늘어난다.

---

### 1.2 Physical Memory / Virtual Memory 구조

```text
Virtual Address Space (64-bit Linux, JVM Process):
  0x0000000000000000
    ├── Text Segment (JVM 바이너리 코드)
    ├── Data/BSS Segment (전역 변수)
    ├── Heap (malloc/mmap 영역 - JVM Native Heap 포함)
    │   ├── JVM Java Heap (Eden/Survivor/Old, -Xmx 제한)
    │   ├── Metaspace (Native Memory)
    │   ├── Code Cache (JIT 컴파일 코드)
    │   └── Direct Buffer (ByteBuffer.allocateDirect)
    ├── Thread Stacks (각 Thread별 mmap, -Xss 크기)
    ├── Memory-Mapped Files (mmap, JAR 파일 등)
    └── Shared Libraries (libc, libpthread 등)
  0xFFFFFFFFFFFFFFFF

Physical Memory Mapping:
  가상 주소 → MMU (TLB 조회) → Page Table → 물리 프레임
  TLB Miss 시: Page Table Walk → 물리 주소 획득 → TLB 캐시
  Page Fault 시: 커널이 물리 프레임 할당 후 Page Table 갱신
```

**메모리 구조 요약:**

| 메모리 영역 | 관리 주체 | 특성 |
|-----------|---------|-----|
| Java Heap | JVM GC | `-Xms` / `-Xmx`로 크기 제한, GC 대상 |
| Metaspace | JVM + OS | Native Memory, `-XX:MaxMetaspaceSize`로 제한 가능 |
| Code Cache | JVM JIT | Native Memory, JIT 컴파일 코드 저장 |
| Thread Stack | JVM + OS | 각 Thread당 `-Xss` (기본 512KB~1MB) |
| Direct Buffer | JVM + OS | GC 비대상, `Cleaner` 콜백으로 해제 |
| JVM Internal | JVM | Symbol Table, GC metadata 등 |

---

### 1.3 Container Memory Limit / cgroup 구조

결제 시스템이 Kubernetes 환경에서 동작할 경우 cgroup 메모리 제한이 JVM 메모리 관리와 충돌하는 지점을 반드시 파악해야 한다.

```text
cgroup v2 메모리 제어 구조:
  /sys/fs/cgroup/
    └── kubepods/
        └── pod<uid>/
            └── <container-id>/
                ├── memory.max          # Hard Limit (초과 시 OOM Kill)
                ├── memory.high         # Soft Limit (초과 시 메모리 회수 압력)
                ├── memory.current      # 현재 사용량
                ├── memory.stat         # 상세 통계 (cache, rss, mapped_file 등)
                └── memory.events       # OOM 발생 카운터

JVM과 cgroup 연동 (JDK 8u191+, JDK 11+):
  -XX:+UseContainerSupport (JDK 11+ 기본 활성화)
  → JVM이 /sys/fs/cgroup/memory.max 를 읽어 Heap 크기 자동 조정
  → -XX:MaxRAMPercentage=75.0 (예: memory.max의 75%를 Xmx로 설정)
  → 미설정 시 JVM이 Node 전체 물리 메모리를 기준으로 Heap 설정 → OOM Kill 위험
```

**cgroup 메모리 제한 운영 예시:**

```yaml
# Kubernetes Pod spec
resources:
  requests:
    memory: "4Gi"     # 스케줄링 기준
  limits:
    memory: "8Gi"     # cgroup memory.max → OOM Kill 경계

# JVM 권장 설정
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=75.0   # 8Gi × 75% = 6Gi Heap
-XX:InitialRAMPercentage=50.0
# 나머지 ~2Gi: Metaspace + Code Cache + Direct Buffer + Thread Stacks
```

---

### 1.4 Swap / THP (Transparent Huge Pages) / Page Size

```text
Swap:
  결제 시스템에서 Swap 사용은 GC Pause 폭증으로 이어진다.
  GC가 Old Region을 스캔할 때 Swapped-out 페이지에 접근 시 Major Page Fault 발생
  → 수십 ms 추가 Pause → Kafka session.timeout 초과 → Rebalance

THP (Transparent Huge Pages):
  기본 Page Size: 4KB
  Huge Page: 2MB (THP) 또는 1GB (Explicit HugePage)
  THP 활성화 시:
    장점: TLB Miss 감소 (JVM Heap의 대형 Region 접근에 유리)
    단점: khugepaged 데몬이 메모리를 비동기적으로 병합/분리
          → 불규칙한 지연 발생, GC Stop-the-World 구간과 겹칠 경우 Pause 증폭

권장 설정 (결제 시스템):
  THP: /sys/kernel/mm/transparent_hugepage/enabled → madvise
  Swap: vm.swappiness=1 (최소화, 0은 완전 비활성화 아님)
  Explicit HugePage: /proc/sys/vm/nr_hugepages (JVM에서 -XX:+UseLargePages 활용)
```

---

## 2. Java Memory E2E 실행 흐름

### 2.1 결제 요청의 전체 메모리 경로

```text
[결제 HTTP 요청 수신]
  ↓
NIC → DMA → Kernel Ring Buffer (sk_buff, Kernel Memory)
  ↓
TCP/IP Stack → Socket Receive Buffer (Kernel Page Cache)
  ↓
epoll_wait() 반환 → Netty EventLoop Thread 깨움
  ↓
[User Space 진입]
  ↓
Netty DirectByteBuf (Off-Heap, Direct Memory 할당)
  → ByteBuffer.allocateDirect() → mmap(MAP_ANONYMOUS) → 물리 페이지 할당
  ↓
HTTP 파싱 → Java Heap 객체 생성 (TLAB 내 bump-pointer)
  → HttpRequest, PaymentRequestDTO 등 → Eden Region
  ↓
Business Logic 처리
  → DB 조회: HikariCP에서 Connection 획득 (Pool 내 Java 객체 참조)
  → SQL 실행: JDBC → TCP Socket → DB Server (Kernel Socket Buffer 경유)
  ↓
트랜잭션 처리 / 결제 승인
  → 결과 객체 생성: PaymentResult → Eden 또는 Old Region (수명에 따라)
  ↓
응답 직렬화 (Jackson)
  → Java String → byte[] (Heap) → Netty DirectByteBuf (Off-Heap)
  ↓
write() syscall → Kernel Socket Send Buffer
  ↓
NIC → DMA → 클라이언트
  ↓
[요청 완료 - 메모리 해제]
  → Eden 객체: Minor GC 시 회수
  → DirectByteBuf: reference count 0 → Cleaner 콜백 → munmap()
  → HikariCP Connection: Pool로 반환 (해제 아님)
```

---

### 2.2 계층별 메모리 흐름 상세

```text
Hardware Layer:
  DRAM DIMM → Memory Controller → CPU Cache Hierarchy (L1→L2→L3)
  Cache Line (64 bytes) 단위로 데이터 적재
  TLB: 가상→물리 주소 변환 캐시 (L1 TLB 약 64 entry, L2 TLB 약 1024 entry)

MMU Layer:
  JVM 가상 주소 접근
    → TLB Lookup (Hit: 수 사이클, Miss: Page Table Walk 수십 사이클)
    → Page Table: PGD → P4D → PUD → PMD → PTE → 물리 Frame
    → Minor Page Fault: 물리 프레임 미할당 → 커널이 프레임 할당 후 매핑
    → Major Page Fault: Swap에서 페이지 복원 (수 ms)

Kernel Memory Manager Layer:
  Buddy Allocator: 물리 페이지 블록 관리 (2^0 ~ 2^10 페이지 단위)
  Slab Allocator: 커널 내부 객체 (task_struct, sock, file 등) 캐싱
  Page Cache: 파일 I/O 캐싱 (JAR 로드, 로그 쓰기 등)
  → JVM 입장에서는 mmap/brk syscall로 물리 메모리 요청

JVM Runtime Layer:
  Java Heap (GC 관리):
    Eden → Survivor → Old (객체 수명에 따라 승격)
    TLAB: Thread별 Eden 선점 영역 (동기화 없는 할당)
  Native Memory (OS 직접 관리):
    Metaspace, Code Cache, Thread Stack, Direct Buffer

Framework Layer (Spring/Netty):
  Netty: DirectByteBuf Pool (PooledByteBufAllocator)
  Spring: BeanFactory, ApplicationContext (Heap 상주 장수 객체)
  HikariCP: Connection Pool (Heap 내 장수 객체)

Application Layer:
  결제 DTO, Entity, Response 객체 (단수명 객체 → Eden)
  Cache 객체 (Caffeine, Redis Client 연결 등 → Old 승격)
  ThreadLocal 변수 (스레드 수명과 동일)
```

---

## 3. Java Memory 사용 메커니즘 및 메모리 모델

### 3.1 Java Heap 구조 및 TLAB

```text
G1GC Heap 구조 (기본):
  Heap → Region 배열 (기본 2048개, Region 크기 1MB~32MB)
    ├── Eden Region (새 객체 할당)
    ├── Survivor Region (GC 생존 객체)
    ├── Old Region (장수 객체)
    └── Humongous Region (Region 크기 50% 초과 객체)
         → 결제 DTO 중 대형 List/Map은 Humongous로 직접 할당됨
         → Young GC로 회수 불가, Mixed GC 대상

TLAB (Thread-Local Allocation Buffer):
  Eden Space
    ├── Thread-A TLAB (선점 영역, 기본 약 1% of Eden)
    ├── Thread-B TLAB
    └── 공유 영역 (TLAB 재충전 시 할당)

  할당 흐름:
    new PaymentRequestDTO()
      → Thread-A의 TLAB 내 bump-pointer 이동 (단순 포인터 연산, 락 없음)
      → TLAB 소진 시: JVM 글로벌 락으로 새 TLAB 요청
      → Eden 소진 시: Minor GC 트리거

SRE 관점:
  결제 요청 TPS가 높을수록 TLAB 재충전 빈도 증가
  → JFR ObjectAllocationInNewTLAB 이벤트로 핫스팟 추적
  → Minor GC 빈도 증가 → P99 Latency 영향
```

---

### 3.2 GC Heap 메모리 메커니즘

#### G1GC 메모리 관리

```text
Minor GC (Young Collection):
  대상: Eden + Survivor Region
  방식: Evacuation (살아있는 객체를 다른 Region으로 복사)
  STW: 전 구간
  결제 영향: 수 ms 수준 정지

Mixed GC:
  대상: Young + Old Region 일부 (Garbage 비율 높은 순)
  전제: Concurrent Marking 완료 후 수행
  결제 영향: Minor GC보다 긴 Pause, 빈도 낮음

Full GC (최후 수단):
  방식: Serial 전체 압축
  STW: 수백 ms ~ 수 초
  결제 영향: 치명적, Kafka Rebalance, PG Timeout 유발

Concurrent Marking 단계:
  Initial Mark    → STW (짧음, Minor GC에 피기백)
  Concurrent Mark → 동시 실행 (CPU 일부 점유)
  Remark          → STW (SATB 보정)
  Cleanup         → STW (빈 Region 회수) + 동시 정리
```

#### ZGC 메모리 관리 (고성능 결제 환경)

```text
Colored Pointer 메커니즘:
  64bit 포인터 상위 비트를 메타데이터로 활용
    Bit 42: Marked0
    Bit 43: Marked1
    Bit 44: Remapped
    Bit 45: Finalizable

  Load Barrier:
    객체 참조 접근 시 Barrier 실행
    → 포인터 색상 확인 → 필요 시 재매핑 수행
    → 애플리케이션 실행 중 객체 이동 가능 (Concurrent Compaction)

STW 범위:
  Initial Mark (수 ms) → Concurrent Mark → Remark (수 ms) → Concurrent Compaction
  → Heap 수백 GB에도 Pause 10ms 이하 유지 가능
```

---

### 3.3 Off-Heap 메모리 메커니즘

#### Metaspace

```text
Metaspace (Native Memory):
  ClassLoader 단위로 메타데이터 관리
    ├── 클래스 구조 (필드, 메서드 시그니처)
    ├── 메서드 바이트코드
    ├── 상수 풀 (Constant Pool)
    └── 어노테이션 정보

  할당: mmap() → Metaspace Chunk → 개별 클래스 메타데이터
  해제: ClassLoader가 GC될 때 해당 ClassLoader의 모든 클래스 메타데이터 반환

Leak 패턴 (결제 시스템 주의):
  동적 프록시 생성 (Spring AOP, CGLIB) → 클래스 반복 생성
  JSP 재컴파일, 플러그인 Hot Deploy
  ClassLoader 참조를 Static/ThreadLocal로 보관
  → ClassLoader GC 불가 → Metaspace 지속 증가 → Native OOM
```

#### Direct Buffer

```text
Direct Buffer (Off-Heap):
  ByteBuffer.allocateDirect(size)
    → JVM이 OS에 mmap(MAP_ANONYMOUS) 요청
    → 물리 페이지 할당 (첫 접근 시 Page Fault)
    → JVM GC 대상 아님

  해제 메커니즘:
    DirectByteBuffer 객체 (Heap) → GC로 수거
    → Cleaner 콜백 실행 → Unsafe.freeMemory() → munmap()
    → 또는 명시적: ((DirectBuffer)buf).cleaner().clean()

  Netty PooledByteBufAllocator:
    Direct Memory를 미리 확보 후 Pool에서 재사용
    → 할당/해제 오버헤드 최소화
    → 누수 시: -Dio.netty.leakDetection.level=paranoid 로 추적

결제 시스템 영향:
  HTTP 요청/응답 버퍼 (Netty), Kafka Producer RecordAccumulator
  → Direct Memory 고갈 시 OutOfMemoryError: Direct buffer memory
  → -XX:MaxDirectMemorySize 설정으로 상한 제한
```

---

### 3.4 Thread Stack 메모리

```text
Platform Thread Stack:
  OS Thread 생성 (clone() syscall) 시 Native Memory에서 Stack 확보
  기본 크기: 512KB ~ 1MB (-Xss 설정)
  구성: 메서드 프레임 (지역 변수, 피연산자 스택, 리턴 주소)

메모리 영향:
  Thread 수 × Xss = Thread Stack Native Memory 총량
  예: 200 Threads × 1MB = 200MB Native Memory
  Thread Pool 크기가 클수록 Native Memory 증가

Virtual Thread Stack (JDK 21+):
  Continuation (Heap에 저장된 Stack 직렬화본)
  크기: 수 KB ~ (실제 Stack Frame 크기에 비례)
  Carrier Thread (소수, OS Thread): 실제 Stack은 수 MB
  → 수십만 Virtual Thread 생성 시에도 Heap 증가는 있으나 Native Stack은 최소화
  → Pinning 발생 시 Carrier Thread Stack 고정 (주의)
```

---

### 3.5 mmap / Page Cache / Copy-on-Write

```text
JAR 파일 로드:
  ClassLoader → mmap(jar_file) → Kernel Page Cache에 적재
  → JVM 가상 주소와 파일 내용 매핑
  → 첫 접근 시 Page Fault → Page Cache에서 물리 메모리 적재

Copy-on-Write (CoW):
  fork() 시 부모-자식 프로세스가 동일 물리 페이지 공유
  Java에서는 직접 사용 빈도 낮으나:
    JVM Agent, fork 기반 도구 연동 시 CoW 동작
    → 쓰기 발생 시 Page 복사 → 물리 메모리 증가

Memory-Mapped Buffer (MappedByteBuffer):
  FileChannel.map() → mmap() → 파일을 가상 주소 공간에 매핑
  → read()/write() 없이 배열처럼 파일 접근
  → 결제 로그, 감사 기록 파일 처리에 활용 가능
  → 단, 언매핑 어려움 (JDK 별도 해제 API 제한)
```

---

### 3.6 NUMA Locality와 False Sharing

```text
NUMA Locality (JVM):
  -XX:+UseNUMA 활성화 시:
    각 NUMA 노드에 별도 Eden 영역 생성
    Thread가 실행 중인 CPU의 NUMA Node에서 TLAB 할당
    → Local 메모리 접근 → Cache/Memory Latency 최소화

  미활성화 시:
    JVM이 단일 Heap을 NUMA 비고려로 할당
    → Thread가 Remote NUMA 접근 빈번 → 수십 ns 지연 누적
    → 결제 처리량이 높을수록 NUMA 최적화 효과 크다

False Sharing:
  CPU Cache Line = 64 bytes
  서로 다른 Thread가 동일 Cache Line 내 인접 변수에 쓰기
    → MESI 프로토콜: Cache Line 무효화/재전송 반복
    → 실질적 성능: 싱글 Thread 대비 수배 저하 가능

  Java 대응:
    @Contended (JDK 8+): 128 bytes padding 자동 삽입
    수동 padding: long p1, p2, p3, p4, p5, p6, p7; // 56 bytes

  결제 시스템 예:
    LongAdder (TPS 카운터): Cell 배열 분산 → False Sharing 회피
    Ring Buffer (LMAX Disruptor): Sequence 변수에 @Contended 적용
```

---

## 4. Memory 병목 및 위험 발생 지점

### 4.1 Heap Exhaustion

```text
발생 경로:
  결제 요청 폭증 → 객체 생성 속도 > GC 회수 속도
    → Eden 연속 고갈 → Minor GC 연속 트리거
    → Old Region 승격 누적 → Old Region 고갈
    → Mixed GC 빈도 증가 → Full GC 트리거
    → OutOfMemoryError: Java heap space

주요 원인:
  - 대형 ResultSet (결제 내역 전체 조회) → Heap 점유 급증
  - Cache 객체 무한 증가 (TTL 미설정 Caffeine 등)
  - 결제 요청 DTO가 Old Region으로 승격 (수명이 길어진 경우)
  - Leak: Static Collection에 결제 객체 지속 추가

확인 지표:
  jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes > 0.85
  jvm_gc_memory_promoted_bytes 급증
  jvm_gc_pause_seconds 증가
```

---

### 4.2 Native Memory Exhaustion

```text
발생 경로:
  Metaspace 누수 → ClassLoader Leak → Native OOM
  Thread 폭증 → Stack Native Memory 초과
  Direct Buffer 누수 → MaxDirectMemorySize 초과
  Code Cache 고갈 → JIT 중단 → Interpreter 회귀 → Latency 급증

징후:
  OutOfMemoryError: Metaspace
  OutOfMemoryError: Direct buffer memory
  OutOfMemoryError: unable to create new native thread
  Java compilation stopped (Code Cache 고갈 로그)

결제 영향:
  Code Cache 고갈 시 JIT 재컴파일 중단 → 핫 결제 코드가 Interpreter로 실행
  → CPU 사용률 급증 + Latency P99 수배 증가
```

---

### 4.3 Memory Leak 패턴

```text
결제 시스템에서 빈번한 Leak 패턴:

1. ThreadLocal Leak
   ThreadLocal<PaymentContext> 사용 후 remove() 미호출
   → Tomcat Worker Thread 재사용 시 이전 Context 잔류
   → Thread Pool Thread 수명 = JVM 수명이므로 누적 증가

2. Static Collection Leak
   static Map<String, PaymentSession> sessions
   → 세션 만료 후 제거 로직 없으면 영구 누적

3. Listener / Observer 미해제
   EventBus, Guava EventBus에 결제 이벤트 구독자 등록 후 미해제
   → GC Root로 참조 유지 → 연관 객체 전체 GC 불가

4. Inner Class 참조
   익명 클래스 또는 람다가 외부 클래스(결제 서비스) 참조 보유
   → 외부 객체 GC 불가
```

---

### 4.4 GC Pressure와 Tail Latency

```text
GC Pressure → 결제 Tail Latency 증가 경로:

Allocation Rate 증가
  → Minor GC 빈도 증가 (수백 ms마다)
  → Survivor Region 크기 부족 → 조기 Old 승격
  → Old Region 빠르게 채워짐
  → Mixed GC 빈도 증가 → STW 증가
  → Full GC 트리거 → 수백 ms ~ 수 초 정지

부가 영향:
  GC STW 동안 Kafka Consumer poll() 중단
    → session.timeout.ms 초과 → Consumer Group Rebalance
    → 결제 후처리 메시지 처리 중단
  GC STW 동안 외부 PG API 응답 대기 Thread 정지
    → Connection Timeout 발생
  GC STW 동안 HikariCP keepalive 실패 → Connection 단절

TTSP (Time To SafePoint) 지연:
  GC 자체 시간이 짧아도 TTSP가 길면 전체 STW 증가
  → 결제 처리 중인 Thread들이 SafePoint 도달 지연
  → Counted Loop, JNI Critical Section 등이 원인
```

---

### 4.5 Swap Thrashing

```text
발생 조건:
  JVM Heap(-Xmx) + Native Memory > 물리 메모리
  또는 다른 프로세스와 경쟁으로 JVM 페이지가 Swap 아웃

결제 영향:
  GC가 Old Region 스캔 중 Swap 아웃된 페이지 접근
    → Major Page Fault → 디스크 I/O → 수 ms ~ 수십 ms 지연
    → GC Pause 예측 불가 → 결제 SLA 위반

예방:
  vm.swappiness=1 (Swap 최대한 억제)
  mlockall() 또는 -XX:+AlwaysPreTouch (Heap 페이지 사전 확보)
  Container Memory Limit 적절 설정
```

---

### 4.6 Page Fault (Minor / Major)

```text
Minor Page Fault:
  물리 메모리는 있으나 Page Table 매핑 미존재
  → 커널이 물리 프레임 할당 후 매핑 (수 μs)
  → JVM 기동 초기, 새 Heap Region 확장 시 빈번
  → -XX:+AlwaysPreTouch로 기동 시 사전 매핑 (기동 시간 증가 대신 런타임 Page Fault 감소)

Major Page Fault:
  페이지가 Swap 아웃된 상태 → 디스크 I/O 필요 (수 ms)
  → 결제 시스템에서 발생 시 해당 Thread Latency 급증
  → 반드시 /proc/<pid>/status VmSwap 으로 확인

확인:
  /proc/<pid>/stat (minflt, majflt 필드)
  perf stat -e page-faults,major-faults
```

---

### 4.7 OOM Killer와 Container OOMKill

```text
Linux OOM Killer:
  시스템 메모리 고갈 시 커널이 oom_score 기준으로 프로세스 강제 종료
  JVM 프로세스는 큰 메모리 점유로 높은 oom_score 가질 수 있음
  /proc/<pid>/oom_score_adj로 조정 가능 (-1000: 완전 보호)

Container OOMKill:
  cgroup memory.max 초과 시 컨테이너 내 프로세스 즉시 Kill
  JVM OutOfMemoryError와 달리 핸들링 불가 (즉시 종료)
  → 진행 중인 결제 트랜잭션 데이터 유실 위험
  → Kubernetes: OOMKilled 상태로 Pod 재시작

OOMKill 방지 전략:
  -XX:MaxRAMPercentage=75.0 (Heap이 Container Limit의 75% 이하)
  나머지 25%: Metaspace + Code Cache + Direct Buffer + Thread Stacks
  Limit 여유 확보: requests < limits (버스트 허용)
  -XX:+ExitOnOutOfMemoryError (Heap OOM 시 즉시 종료 → 좀비 상태 방지)
  -XX:+HeapDumpOnOutOfMemoryError (진단용 Heap Dump 자동 생성)
```

---

## 5. SRE 관점 모니터링 지표

### 5.1 JVM 메모리 핵심 지표

| 지표 | 설명 | 임계 기준 | 도구 |
|------|------|---------|------|
| `jvm_memory_used_bytes{area="heap"}` | Java Heap 사용량 | 최대치의 80% 이상 지속 시 조사 | Micrometer/Prometheus |
| `jvm_memory_used_bytes{area="nonheap"}` | Metaspace + Code Cache 등 | 지속 증가 시 Leak 의심 | Micrometer/Prometheus |
| `jvm_gc_pause_seconds{action="end of major GC"}` | Full GC Pause 시간 | 발생 자체가 이상 신호 | Micrometer/Prometheus |
| `jvm_gc_pause_seconds{action="end of minor GC"}` | Minor GC Pause | 결제 Timeout의 10% 이하 권장 | Micrometer/Prometheus |
| `jvm_gc_memory_allocated_bytes` | 객체 할당 속도 | TPS 대비 비정상 증가 확인 | Micrometer/Prometheus |
| `jvm_gc_memory_promoted_bytes` | Old 영역 승격 속도 | 급증 시 메모리 누수 의심 | Micrometer/Prometheus |
| `jvm_classes_loaded` | 로드된 클래스 수 | 지속 증가 시 ClassLoader Leak | Micrometer/Prometheus |

---

### 5.2 OS/프로세스 메모리 지표

| 지표 | 설명 | 임계 기준 | 확인 방법 |
|------|------|---------|---------|
| RSS (Resident Set Size) | 실제 물리 메모리 점유량 | Container Limit의 80% 초과 시 경고 | `ps -o rss`, `/proc/<pid>/status` |
| VSZ (Virtual Size) | 가상 메모리 예약량 | RSS 대비 과도하게 크면 mmap 점검 | `ps -o vsz` |
| Swap Usage | Swap 사용량 | 0 이상이면 즉시 조사 | `vmstat`, `/proc/<pid>/status VmSwap` |
| Minor Page Fault | 물리 매핑 누락 | 기동 초기 외 급증 시 확인 | `/proc/<pid>/stat minflt` |
| Major Page Fault | Swap에서 페이지 복원 | 0 이상이면 즉시 조사 | `/proc/<pid>/stat majflt` |
| PSI Memory | 메모리 압박으로 인한 정지 비율 | some > 5% 지속 시 조사 | `/proc/pressure/memory` |

---

### 5.3 Direct Buffer / Native Memory 지표

| 지표 | 설명 | 확인 방법 |
|------|------|---------|
| Direct Buffer 사용량 | Off-Heap ByteBuffer 사용량 | `BufferPoolMXBean`, JFR DirectBufferStatisticsEvent |
| Direct Buffer 최대치 | `-XX:MaxDirectMemorySize` 설정값 | JVM 시작 옵션 확인 |
| Netty Direct Memory | Netty 할당 Direct Memory | `io.netty.allocator.usedDirectMemory` 메트릭 |
| Native Memory 총량 | JVM 전체 Native 사용량 | `jcmd <pid> VM.native_memory summary` |

---

### 5.4 Connection Pool / 애플리케이션 특화 지표

| 지표 | 설명 | 임계 기준 |
|------|------|---------|
| `hikaricp_active_connections` | 사용 중인 DB Connection | Pool 크기의 80% 이상 지속 시 경고 |
| `hikaricp_pending_threads` | Connection 대기 Thread 수 | 0보다 크면 즉시 조사 |
| `hikaricp_connection_timeout_total` | Connection 획득 실패 횟수 | 증가 시 결제 실패 직결 |
| `r2dbc_pool_pending` | R2DBC Connection 대기 수 | 0보다 크면 조사 |

---

### 5.5 GC 세부 지표 (JFR / JMX)

```text
JFR 이벤트 기반 메모리 분석:

ObjectAllocationInNewTLAB:
  → 어떤 클래스/메서드에서 객체 할당이 집중되는지 확인
  → 결제 DTO, 직렬화 중간 객체가 핫스팟일 경우 오브젝트 풀링 검토

GCHeapSummary:
  → GC 전후 Heap 사용량 변화
  → Minor GC 후에도 Old 사용량이 계속 증가하면 Leak 의심

PromoteObjectOutsidePLAB:
  → TLAB 외부에서 Old로 직접 승격된 대형 객체
  → Humongous Object 할당 빈도 확인

G1MMUViolation:
  → GC Pause가 MMU(Minimum Mutator Utilization) 목표 위반
  → 결제 Latency SLA와 비교 필요
```

---

## 6. 장애 시나리오

### 6.1 Java Heap OOM

```text
시나리오:
  결제 배치 처리 중 대용량 거래 내역 조회 (limit 없는 쿼리)
    → List<PaymentHistory> 수십만 건 → Heap 급증
    → Minor GC 반복 → Old 승격 → Old 고갈
    → Full GC 연속 → Heap 회수 불가
    → OutOfMemoryError: Java heap space

결제 영향:
  진행 중인 결제 요청 전체 실패 (Thread가 OOM 예외 수신)
  JVM 정지 상태가 되거나 계속 Full GC 반복으로 응답 불가 (GC Thrashing)

분석:
  -XX:+HeapDumpOnOutOfMemoryError 로 생성된 .hprof 파일
  → Eclipse MAT로 Dominator Tree 분석
  → 가장 많은 메모리 점유 객체 클래스/참조 체인 확인

대응:
  쿼리에 페이징 적용, Stream 방식으로 순차 처리
  Heap 크기 증설 (근본 원인 해결 병행 필수)
```

---

### 6.2 Metaspace OOM (ClassLoader Leak)

```text
시나리오:
  Spring Boot 운영 중 외부 플러그인 동적 로드 반복
  → Custom ClassLoader 생성 → 클래스 로드
  → ClassLoader 참조가 Cache에 보관 → GC 불가
  → Metaspace 지속 증가 → 한계 도달
  → OutOfMemoryError: Metaspace

결제 영향:
  JVM 재시작 필요 → 서비스 중단
  재시작 없으면 Full GC 반복으로 성능 저하

분석:
  jcmd <pid> VM.native_memory summary → Metaspace 증가 확인
  JFR ClassLoaderStatistics → ClassLoader별 로드 클래스 수
  Heap Dump → MAT → ClassLoader 참조 체인 추적

대응:
  ClassLoader 참조를 Static/ThreadLocal에 보관하지 않기
  -XX:MaxMetaspaceSize 설정으로 OOM 조기 감지 (무제한 증가 방지)
```

---

### 6.3 Container OOMKill

```text
시나리오:
  JVM Heap(-Xmx 4G) + Metaspace(512MB) + Direct Buffer(1G) + Code Cache(256MB) + Thread Stacks(200MB)
  = 약 6GB 실제 사용
  Container memory.max = 6GB (여유 없음)
  → 순간 Direct Buffer 증가 또는 GC 메타데이터 증가
  → cgroup memory.max 초과
  → OOMKill: JVM 즉시 종료

결제 영향:
  진행 중 결제 트랜잭션 상태 불명확 (DB 커밋 완료 여부 불확실)
  멱등성 키 기반 재처리 필요
  Kubernetes Pod 재시작 → Warm-up 구간 재진입 (JIT, Connection Pool 재수립)

분석:
  kubectl describe pod → OOMKilled 상태 확인
  dmesg | grep oom-kill
  /sys/fs/cgroup/memory.events → oom_kill 카운터

대응:
  Container Limit = JVM RSS 예상치 × 1.3 이상 여유 확보
  -XX:MaxRAMPercentage=70.0 (Container Limit의 70%만 Heap으로)
  -XX:MaxDirectMemorySize 명시 설정
```

---

### 6.4 GC-induced Latency (결제 승인 지연)

```text
시나리오:
  결제 TPS 증가 → 객체 할당 속도 증가
  → Minor GC 빈도: 30초마다 → 3초마다로 증가
  → Mixed GC 빈도 증가 → STW 100ms~500ms 발생
  → 이 구간 동안 Kafka Consumer poll() 미실행
  → session.timeout.ms(30초) 내 반복 발생 시 Rebalance 트리거
  → Rebalance 중 결제 후처리 메시지 처리 중단

결제 영향:
  결제 승인은 완료되나 후처리(포인트 적립, 알림 등) 지연
  Consumer Lag 누적 → 결제 내역 조회 정합성 일시 저하

분석:
  JFR GC Pause 이벤트 + Kafka Consumer Lag 시계열 비교
  GC Pause 시점과 Rebalance 시점 상관관계 확인
  jvm_gc_memory_allocated_bytes 급증 구간 식별

대응:
  Heap 증설 (Minor GC 빈도 감소)
  할당 핫스팟 최적화 (오브젝트 풀링, DTO 경량화)
  Kafka: max.poll.interval.ms 조정, GC에 강건한 heartbeat.interval.ms 설정
  ZGC/Shenandoah 전환 (Pause 최소화)
```

---

### 6.5 Direct Buffer Exhaustion

```text
시나리오:
  Netty WebFlux 기반 결제 API에서 응답 처리 중 DirectByteBuf 누수
  → PooledByteBufAllocator의 Direct Memory 사용량 지속 증가
  → -XX:MaxDirectMemorySize 도달
  → OutOfMemoryError: Direct buffer memory
  → Netty EventLoop 오류 → 결제 API 응답 불가

분석:
  io.netty.allocator.usedDirectMemory 메트릭 모니터링
  -Dio.netty.leakDetection.level=paranoid → 누수 위치 로그
  JFR DirectBufferStatisticsEvent

대응:
  Netty ByteBuf 사용 후 반드시 release() 호출
  try-with-resources 패턴 또는 ReferenceCountUtil.release()
  PooledByteBufAllocator 재사용 (신규 할당 최소화)
```

---

### 6.6 Thread Stack Explosion

```text
시나리오:
  결제 처리 중 재귀 호출 깊이 초과 (예: 결제 검증 로직 재귀 버그)
    → StackOverflowError → Thread 비정상 종료
  또는 Thread 폭증 (Virtual Thread 미사용 환경에서 요청마다 Thread 생성)
    → 수천 Thread × 1MB Stack = 수 GB Native Memory 고갈
    → OutOfMemoryError: unable to create new native thread

분석:
  /proc/<pid>/status → Threads 필드
  jcmd <pid> Thread.print → 활성 Thread 목록
  jvm_threads_live 메트릭 급증 확인

대응:
  재귀 로직을 반복(iteration)으로 변환
  Thread Pool 크기 상한 설정 (무한 생성 방지)
  Virtual Thread 도입 (JDK 21+) → Stack Native Memory 사용 최소화
  -Xss 최소화 (256KB ~ 512KB)로 Thread 수 확대 여유 확보
```

---

## 7. 튜닝 포인트

### 7.1 Heap Size 설정

```bash
# Container 환경 (권장)
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=70.0        # Container Limit의 70% → Heap
-XX:InitialRAMPercentage=50.0    # 초기 Heap (빠른 기동)
-XX:MinRAMPercentage=50.0

# 고정 설정 (비Container 또는 정밀 제어)
-Xms8g -Xmx8g                   # 초기=최대 설정 (GC 시 Heap 확장 오버헤드 제거)
-XX:+AlwaysPreTouch              # 기동 시 Heap 페이지 사전 확보 (런타임 Page Fault 방지)
```

---

### 7.2 GC Policy 선택

```bash
# G1GC (기본 권장, JDK 9+)
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200         # 목표 Pause 시간 (결제 Timeout의 10% 이하)
-XX:G1HeapRegionSize=16m         # 대형 Heap(32GB+) 시 Region 크기 증가
-XX:G1NewSizePercent=20          # Young Region 최소 비율
-XX:G1MaxNewSizePercent=40       # Young Region 최대 비율
-XX:+UseNUMA                     # NUMA 최적화

# ZGC (저지연 결제, JDK 15+)
-XX:+UseZGC
-XX:SoftMaxHeapSize=6g           # Soft Limit (ZGC가 이 이하로 유지 시도)
-XX:ZAllocationSpikeTolerance=5  # 할당 급증 허용 배수

# Shenandoah (JDK 12+)
-XX:+UseShenandoahGC
-XX:ShenandoahGCMode=adaptive
```

---

### 7.3 Native Memory 최적화

```bash
# Metaspace
-XX:MetaspaceSize=256m           # 초기 크기 (잦은 확장 방지)
-XX:MaxMetaspaceSize=512m        # 상한 (누수 조기 감지)

# Code Cache
-XX:ReservedCodeCacheSize=512m   # JIT 코드 저장소 (기본 240MB, 충분히 설정)
-XX:InitialCodeCacheSize=64m

# Direct Memory
-XX:MaxDirectMemorySize=2g       # Off-Heap ByteBuffer 상한

# Thread Stack
-Xss512k                         # Platform Thread Stack 크기 (최소화)

# Native Memory Tracking
-XX:NativeMemoryTracking=summary  # NMT 활성화 (오버헤드 약 5~10%)
```

---

### 7.4 HugePage / NUMA / THP

```bash
# Explicit HugePage (OS 설정 필요)
echo 2048 > /proc/sys/vm/nr_hugepages   # 2048 × 2MB = 4GB HugePage 예약
# JVM 설정
-XX:+UseLargePages
-XX:LargePageSizeInBytes=2m

# THP (결제 시스템 권장: madvise)
echo madvise > /sys/kernel/mm/transparent_hugepage/enabled
echo defer+madvise > /sys/kernel/mm/transparent_hugepage/defrag

# Swap 최소화
echo 1 > /proc/sys/vm/swappiness

# NUMA 바인딩 (JVM 프로세스)
numactl --localalloc --cpunodebind=0 java [JVM options] -jar app.jar
# JVM 내부 NUMA 최적화
-XX:+UseNUMA
```

---

### 7.5 JVM 메모리 압박 완화 (결제 특화)

```bash
# SafePoint 최적화 (TTSP 감소)
-XX:+UseCountedLoopSafepoints    # Counted Loop에서 SafePoint 폴링 활성화
-XX:GuaranteedSafepointInterval=0 # 정기 SafePoint 비활성화

# GC 로그 (운영 환경)
-Xlog:gc*:file=/var/log/app/gc.log:time,level,tags:filecount=10,filesize=50m

# 결제 요청 처리 최적화
# Humongous Object 발생 방지: 대형 컬렉션은 스트리밍 처리
# TLAB 크기 조정 (할당 빈도 높을 경우)
-XX:TLABSize=512k
-XX:+ResizeTLAB                  # 자동 조정 활성화

# OOM 대응
-XX:+ExitOnOutOfMemoryError      # OOM 즉시 종료 (좀비 방지)
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/app/heapdump.hprof
-XX:OnOutOfMemoryError="kill -9 %p"
```

---

### 7.6 Container Memory Limit 설계

```text
결제 서비스 Container Memory 설계 예시:
  Java Heap:       -XX:MaxRAMPercentage=70% → Container Limit × 0.70
  Metaspace:       약 256~512MB
  Code Cache:      약 256~512MB
  Direct Buffer:   약 512MB~2GB (Netty 사용량에 따라)
  Thread Stacks:   Thread 수 × Xss
  JVM Internal:    약 100~200MB

  Container Limit = Java Heap / 0.70 이상으로 설정
  (예: Heap 6GB → Limit 최소 8.6GB → 9GB 설정)

  requests: limit의 70% (버스트 허용)
  limits: 위 계산치 + 20% 여유
```

---

## 8. 관련 Linux 명령어 및 분석 도구

### 8.1 OS 메모리 분석 도구

| 명령어 | 분석 항목 | 사용 예 |
|-------|---------|-------|
| `free -h -w` | 전체 메모리 / Swap / Buffer/Cache | `free -h -w` |
| `vmstat 1 10` | Page Fault, Swap In/Out, 메모리 사용 추이 | `vmstat 1 10` |
| `sar -r 1 10` | 메모리 사용률 시계열 | `sar -r 1 10` |
| `top / htop` | 프로세스별 RSS, 메모리 사용률 | `top -p <pid>` |
| `smem -r` | PSS(비례 공유 메모리) 기반 실제 점유량 | `smem -r -p <process-name>` |
| `pmap -x <pid>` | 프로세스 메모리 매핑 상세 | `pmap -x <pid>` |
| `slabtop -o` | 커널 Slab 캐시 사용량 | `slabtop -o` |
| `numastat -p <pid>` | 프로세스 NUMA 노드별 메모리 | `numastat -p <pid>` |
| `cat /proc/meminfo` | 시스템 전체 메모리 상태 상세 | `grep -E 'Mem|Swap|Cached|Dirty' /proc/meminfo` |
| `cat /proc/<pid>/smaps` | 프로세스 매핑별 RSS/PSS/Swap 상세 | `grep -A5 'heap' /proc/<pid>/smaps` |
| `cat /proc/<pid>/status` | RSS, VmSwap, Thread 수 | `grep -E 'VmRSS|VmSwap|Threads' /proc/<pid>/status` |
| `cat /proc/pressure/memory` | PSI 메모리 압박 지표 | `cat /proc/pressure/memory` |

---

### 8.2 JVM 전용 분석 도구

| 도구 | 분석 항목 | 사용 예 |
|-----|---------|-------|
| `jmap -heap <pid>` | Heap 상태 요약 (사용량, GC 알고리즘) | `jmap -heap <pid>` |
| `jmap -histo <pid>` | 클래스별 인스턴스 수/크기 | `jmap -histo:live <pid> | head -30` |
| `jmap -dump:live,file=heap.hprof <pid>` | Heap Dump 생성 | MAT/VisualVM으로 분석 |
| `jcmd <pid> VM.native_memory summary` | Native Memory 영역별 사용량 | NMT 활성화 필요 |
| `jcmd <pid> GC.heap_info` | Heap 현재 상태 | `jcmd <pid> GC.heap_info` |
| `jcmd <pid> VM.flags` | JVM 옵션 확인 | `jcmd <pid> VM.flags` |
| `jcmd <pid> JFR.start` | JFR 녹화 시작 | `jcmd <pid> JFR.start duration=60s filename=rec.jfr` |
| `jcmd <pid> JFR.dump` | JFR 현재까지 데이터 덤프 | `jcmd <pid> JFR.dump filename=rec.jfr` |
| `jstat -gc <pid> 1000` | GC 통계 (Eden/Survivor/Old 사용량, GC 횟수) | `jstat -gc <pid> 1000 10` |
| `jstat -gcutil <pid>` | GC 사용률(%) | `jstat -gcutil <pid> 1000` |

---

### 8.3 JFR (Java Flight Recorder) 분석 항목

```text
JDK Mission Control (JMC) 또는 JFR Event Streaming API 로 분석:

메모리 관련 주요 이벤트:
  GarbageCollection        → GC 종류, STW 시간, 회수량
  GCHeapSummary            → GC 전후 Heap 사용량
  ObjectAllocationInNewTLAB → 핫 할당 클래스/메서드
  ObjectAllocationOutsideTLAB → TLAB 외 대형 객체 할당
  PromoteObjectOutsidePLAB → Old 승격 대형 객체
  MetaspaceChunkFreeListSummary → Metaspace 청크 상태
  DirectBufferStatistics   → Direct Buffer 사용량
  ClassLoaderStatistics    → ClassLoader별 클래스 수
  VirtualThreadPinned      → Carrier Thread Pinning 발생

사용 방법:
  # 30초 녹화
  jcmd <pid> JFR.start name=mem_analysis duration=30s filename=/tmp/mem.jfr settings=profile
  # 파일 분석
  jfr print --events GarbageCollection,ObjectAllocationInNewTLAB /tmp/mem.jfr
```

---

### 8.4 async-profiler

```bash
# Allocation Profiling (메모리 할당 핫스팟)
./profiler.sh -e alloc -d 30 -f alloc.html <pid>

# Wall-clock 프로파일링 (Off-CPU 시간 포함)
./profiler.sh -e wall -d 30 -f wall.html <pid>

# Lock 경합 프로파일링
./profiler.sh -e lock -d 30 -f lock.html <pid>

# 결과: 플레임그래프로 할당 핫스팟 시각화
# 결제 DTO 생성, Jackson 직렬화, HikariCP 연결 관리 등이 주요 핫스팟으로 나타남
```

---

### 8.5 eBPF 메모리 분석

```bash
# Off-CPU Time 분석 (메모리 대기, I/O 대기 포함)
/usr/share/bcc/tools/offcputime -p <pid> 30 > offcpu.txt
stackcollapse.pl offcpu.txt | flamegraph.pl > offcpu.svg

# Page Fault 추적
bpftrace -e 'kprobe:handle_mm_fault /pid == <pid>/ { @[kstack] = count(); }'

# mmap 호출 추적 (JVM Native Memory 할당 추적)
bpftrace -e 'tracepoint:syscalls:sys_enter_mmap /pid == <pid>/ { printf("mmap size=%d\n", args->len); }'

# OOM Killer 감지
bpftrace -e 'kprobe:oom_kill_process { printf("OOM Kill: pid=%d comm=%s\n", args->victim->pid, args->victim->comm); }'

# Memory Pressure 실시간 모니터링
cat /proc/pressure/memory
# some avg10=2.50 avg60=1.20 avg300=0.80 total=...
# full avg10=0.10 avg60=0.05 avg300=0.02 total=...
```

---

### 8.6 Heap Dump 분석 (Eclipse MAT)

```text
분석 절차:
  1. Heap Dump 생성
     jcmd <pid> GC.heap_dump /tmp/heap.hprof
     또는 자동: -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/

  2. Eclipse MAT에서 열기
     → Dominator Tree: 메모리를 가장 많이 점유하는 객체 계층
     → Leak Suspects: 자동 누수 의심 객체 탐지
     → OQL(Object Query Language): 특정 클래스 인스턴스 조회
       SELECT * FROM com.example.payment.PaymentSession
     → Retained Heap: 해당 객체가 해제될 경우 회수 가능한 총 메모리

  3. ClassLoader Leak 확인
     → ClassLoader Explorer: 각 ClassLoader별 로드 클래스 수
     → 정상: AppClassLoader에 몇 천 개
     → Leak 시: Custom ClassLoader가 수백~수천 개, 각각 클래스 보유
```

---

## 부록: 결제 시스템 Memory 이상 징후 빠른 진단표

| 증상 | 가능한 원인 | 첫 번째 확인 명령어 |
|------|-----------|----------------|
| P99 Latency 급증 | GC Pause, Swap, NUMA Remote | `jstat -gcutil <pid>`, `vmstat 1` |
| RSS 지속 증가 | Heap Leak, Metaspace Leak, Direct Buffer Leak | `jcmd VM.native_memory`, `jmap -histo:live` |
| OOM / OOMKill | Heap 과다, Container Limit 부족 | `dmesg | grep oom`, `kubectl describe pod` |
| Kafka Rebalance 빈발 | GC STW > session.timeout.ms | JFR GC Pause vs Rebalance 시간 상관 분석 |
| CPU 사용률 급증 (GC) | Full GC 반복 (GC Thrashing) | `jstat -gc 1000`, `jvm_gc_pause_seconds` |
| Thread 생성 실패 | Native Memory 부족 (Stack 고갈) | `/proc/<pid>/status Threads`, `ulimit -u` |
| JVM 기동 직후 Latency 불안정 | JIT Warm-up, 초기 Page Fault | JFR Compilation 이벤트, `-XX:+AlwaysPreTouch` |
| Metaspace OOM | ClassLoader Leak | JFR ClassLoaderStatistics, MAT |
| Direct Buffer OOM | Netty ByteBuf 누수 | `-Dio.netty.leakDetection.level=paranoid` |

---

*작성 기준: Java 21, G1GC/ZGC, Spring Boot 3.x, Netty 4.x, HikariCP, Kubernetes 환경*  
*관점: FinTech 결제 시스템 SRE — 정확성 > 안정성 > 관측 가능성 > 성능*

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*