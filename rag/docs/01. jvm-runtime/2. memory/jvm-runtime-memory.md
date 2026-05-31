# JVM Runtime — Memory Resource Analysis
> **FinTech Payment System · SRE Perspective**  
> E2E: Hardware / OS Kernel / Runtime / Framework / Application

> 정독: 0회

## 목차

1. [물리/가상 메모리 스펙 확인 지표](#1-물리가상-메모리-스펙-확인-지표)
2. [JVM Memory 실행 흐름 (E2E Memory Path)](#2-jvm-memory-실행-흐름-e2e-memory-path)
3. [JVM Memory 사용 메커니즘 및 메모리 모델 분석](#3-jvm-memory-사용-메커니즘-및-메모리-모델-분석)
4. [Memory 병목 및 위험 발생 지점](#4-memory-병목-및-위험-발생-지점)
5. [SRE 관점 모니터링 지표](#5-sre-관점-모니터링-지표)
6. [장애 시나리오](#6-장애-시나리오)
7. [튜닝 포인트](#7-튜닝-포인트)
8. [관련 Linux 명령어 및 분석 도구](#8-관련-linux-명령어-및-분석-도구)

---

## 1. 물리/가상 메모리 스펙 확인 지표

JVM이 실제로 메모리를 요청하고 사용하기 이전, 하드웨어와 OS 레이어에서 결정되는 물리적 한계치와 구조적 특성을 먼저 파악해야 합니다.

### 1-1. DIMM / Channel / NUMA 구조

| 항목 | 확인 명령어 | SRE 분석 포인트 |
|------|------------|----------------|
| DIMM 구성 | `dmidecode -t memory` | 슬롯별 속도(MT/s), 채널 구성 확인. 이중 채널 미구성 시 Bandwidth 절반 손실 |
| NUMA 토폴로지 | `numactl -H` / `lscpu` | JVM 힙이 Remote NUMA 노드에 할당되면 메모리 접근 레이턴시 2~4배 증가 |
| Memory Bandwidth | `numastat -m` | NUMA 간 `numa_miss` 누적 시 GC STW(Stop-The-World) 시간 증가 |
| HugePage | `cat /proc/meminfo \| grep Huge` | 2MB HugePage 사용 시 TLB Miss 감소 → JVM GC 및 메모리 스캔 성능 향상 |

### 1-2. Physical / Virtual Memory 구조

JVM 프로세스는 OS 가상 주소 공간을 통해 물리 메모리에 접근합니다.

| 메모리 유형 | JVM 연관 개념 | 분석 포인트 |
|------------|--------------|------------|
| RSS (Resident Set) | Heap + Non-Heap + Native | 실제 물리 RAM 점유량. OOMKiller 판단 기준 |
| VSZ (Virtual Size) | mmap 된 전체 가상 영역 | RSS와 차이가 크면 메모리 예약 과다. Max Direct Memory 초과 위험 |
| Page Cache | 파일 I/O 캐시 (로그, jar) | JVM 구동 시 jar 파일 mmap 후 Page Cache 점유. 과도 시 Heap 압박 |
| Anonymous Memory | Heap / Direct Buffer | GC 후 반환된 영역이 OS에 즉시 반환되지 않을 수 있음 (`madvise` 설정 의존) |

### 1-3. Container Memory Limit / cgroup 구조

> ⚠️ **K8s 환경에서는 물리 메모리가 충분해도 `cgroup memory.max` 초과 시 OOMKill이 발생합니다.**  
> JVM의 `-Xmx` 설정은 반드시 Container Limit의 70~80% 이하로 설정해야 합니다.

JVM RSS 총합 구성:

```
RSS = Heap(-Xmx) + Metaspace + Code Cache + Thread Stack(스레드 수 × -Xss) + Direct Buffer + JVM Internal Native
```

| 영역 | 설명 |
|------|------|
| `-Xmx` | Java Heap 최대 크기 (Container Limit의 50~70% 권장) |
| `MaxDirectMemorySize` | Off-Heap Direct Buffer 한도 (기본값 = `-Xmx`) |
| Metaspace | 클래스 메타데이터 (`MaxMetaspaceSize`로 제한 필수) |
| Thread Stack | 스레드 수 × `-Xss` (기본 512KB~1MB/thread) |
| Native Memory | JVM 내부 + JNI + Code Cache + GC 데이터 구조 |

### 1-4. Swap / THP / Page Size

| 설정 | 권장값 | JVM SRE 관점 |
|------|--------|-------------|
| `vm.swappiness` | **1 또는 0** | Swap 사용 시 GC STW 시간 수십 배 증가. 핀테크 결제 환경 절대 금지 |
| THP (Transparent Huge Page) | **`madvise` 또는 `never`** | THP 할당/해제 시 `kcompactd` 데몬 CPU 급등 → 핀테크 환경 `never` 권장 |
| Explicit HugePage | `-XX:+UseHugeTLBFS` | TLB Miss 감소, GC 스캔 성능 향상. NUMA 환경에서 `numactl`과 병행 |
| Page Size | 4KB / 2MB (HugePage) | G1GC의 Region 크기(1~32MB)와 HugePage 정렬 고려 |

---

## 2. JVM Memory 실행 흐름 (E2E Memory Path)

네트워크 패킷 수신부터 JVM Heap 할당 및 GC 반환까지의 전체 메모리 흐름을 계층별로 분석합니다.

### 2-1. 전체 E2E 메모리 흐름

| 계층 | 구성 요소 | 메모리 동작 | 분석 도구 |
|------|----------|------------|----------|
| **Hardware** | NIC / DMA / DRAM | DMA: NIC → RAM 직접 전송 (CPU 개입 없음). NUMA Local/Remote 결정 | `numastat`, `ethtool` |
| **MMU** | TLB / Page Table | 가상 주소 → 물리 주소 변환. TLB Miss 시 Page Walker 동작 → 레이턴시 증가 | `perf stat -e dTLB-miss` |
| **OS Kernel** | Socket Buffer / Page Cache | `recv_buffer` → Page Cache → User Space 복사 (Zero-Copy 미적용 시) | `/proc/net/sockstat`, `sar` |
| **JVM Runtime** | mmap / brk / malloc | JVM 기동 시 `mmap`으로 힙 예약. G1GC는 Region 단위로 commit/uncommit | `pmap -x`, `/proc/smaps` |
| **GC** | Eden → Survivor → Old | 객체 할당(Young Gen TLAB) → Minor GC → Promotion → Major GC → OS 반환 | `jstat -gcutil`, JFR |
| **Framework** | Spring / Netty / HikariCP | ThreadLocal, Connection Pool 객체, ByteBuf (Direct/Heap Buffer) | `async-profiler`, heap dump |
| **Application** | DTO / Domain Object | 객체 생성 → TLAB 할당 → GC Root 참조 해제 → GC 수거 | JFR allocation profiling |

### 2-2. JVM 기동 시 메모리 매핑 순서

```
① JVM 프로세스 fork
   └─ OS가 가상 주소 공간 할당 (VSZ 폭증, 물리 페이지 미할당)

② -Xms 크기만큼 mmap(MAP_ANONYMOUS)
   └─ Demand Paging: 물리 페이지는 실제 접근 시점에 할당

③ 첫 객체 할당 → Page Fault
   └─ OS가 물리 페이지 매핑 → RSS 점진적 증가

④ GC Region uncommit
   └─ madvise(MADV_FREE/DONTNEED) → OS에 물리 페이지 반환

⑤ Metaspace
   └─ 클래스 로딩 시 Native Heap에 직접 할당 (mmap 기반 청크 관리)
```

### 2-3. User Space / Kernel Space 메모리 경계

핀테크 결제 시스템에서 Zero-Copy 미적용 시 불필요한 Memory Copy가 발생합니다.

**일반 경로 (Copy 발생):**
```
NIC → sk_buff (Kernel) → Socket Recv Buffer (Kernel)
    → [copy] → User Space → ByteArrayInputStream (JVM Heap)
```

**Direct Buffer 경로 (Zero-Copy 가능):**
```
NIC → sk_buff → Direct Buffer (Off-Heap, Kernel 공유 가능)
    → Netty PooledDirect ByteBuf (Zero-Copy)
```

---

## 3. JVM Memory 사용 메커니즘 및 메모리 모델 분석

### 3-1. JVM 전체 메모리 구조 맵

| 메모리 영역 | 세부 구성 | 핀테크 SRE 분석 포인트 |
|------------|----------|----------------------|
| **Java Heap** | Eden / Survivor / Old Gen (G1: Region 기반) | 결제 객체 생성 폭증 시 Minor GC 빈도 증가 → P99 레이턴시 급등 |
| **Metaspace** | Class Metadata / Klass / Method | Spring 동적 프록시, Reflection 과다 시 누수. `MaxMetaspaceSize` 미설정 시 OOM |
| **Code Cache** | JIT Compiled Code | `ReservedCodeCacheSize` 초과 시 JIT 비활성화 → 성능 급락 |
| **Thread Stack** | 각 스레드당 `-Xss` (기본 512KB~1MB) | 스레드 10000개 × 1MB = 10GB. 가상 스레드(Project Loom) 도입 시 대폭 절감 |
| **Direct Buffer** | `ByteBuffer.allocateDirect()`, Netty Direct | GC 대상 외. 누수 시 Native OOM. `MaxDirectMemorySize` 제한 및 모니터링 필수 |
| **Mapped Buffer** | `FileChannel.map()` → mmap | 페이지 매핑 수 초과 시 `vm.max_map_count` 에러. Kafka/RocketMQ 환경 주의 |
| **JVM Internal Native** | GC 데이터 구조, Symbol Table, String Table | NMT(Native Memory Tracking)으로만 추적 가능. `RSS - Heap - Metaspace` = 이 영역 |

### 3-2. Heap 메모리 상세: G1GC Region 모델

핀테크 결제 시스템의 표준 GC인 G1GC의 메모리 모델을 상세히 분석합니다.

| Region 유형 | 역할 | 분석 포인트 |
|------------|------|------------|
| **Eden Region** | 새 객체 TLAB 할당 대상 | Minor GC(Young GC) 수거. Allocation Rate 높으면 빈번한 Young GC 유발 |
| **Survivor Region** | Minor GC 생존 객체 이동 | `-XX:MaxTenuringThreshold` 도달 시 Old 승격. Survivor 과소 시 Promotion Failure |
| **Old Region** | 장수 객체 | Mixed GC 또는 Full GC 수거. 80% 이상 점유 지속 시 Full GC 임박 |
| **Humongous Region** | Region 50% 초과 대형 객체 | 즉시 Old 배치 → GC 압박 유발. 결제 대용량 페이로드 주의 |
| **Free Region** | uncommit 후 OS 반환 대기 | `-XX:G1PeriodicGCInterval` 설정으로 주기적 반환 |

```
Region 크기 = 힙 크기 / 2048 (자동)
           또는 -XX:G1HeapRegionSize=8m (수동 설정)
```

### 3-3. TLAB (Thread-Local Allocation Buffer)

> 결제 트랜잭션 처리 스레드가 Eden에 직접 접근하면 동기화 오버헤드가 발생합니다.  
> TLAB은 각 스레드에 Eden의 일부를 전용으로 할당하여 **Lock-Free 객체 생성**을 가능하게 합니다.

- **TLAB 크기:** `힙 / 스레드 수 / 4` 수준으로 JVM 자동 조정
- **TLAB Miss:** 객체가 TLAB보다 크면 Eden에 직접 할당(동기화 필요) → `-XX:TLABSize`, `-XX:ResizeTLAB`
- **TLAB Waste:** TLAB 끝 미사용 공간 → Allocation Rate 모니터링으로 비효율 탐지

### 3-4. Direct Buffer / Mapped Buffer 메커니즘

| 유형 | 할당 경로 | 핀테크 위험 포인트 |
|------|----------|------------------|
| **Direct ByteBuffer** | `malloc/mmap` → Native Heap → GC PhantomReference로 해제 | PhantomReference 큐 지연 시 해제 안 됨. `System.gc()` 또는 `Cleaner` 직접 호출 필요 |
| **Netty PooledByteBuf** | `PooledByteBufAllocator` → jemalloc 유사 Arena 구조 | Arena 파편화 시 실제 사용량 < 할당량. `PooledByteBufAllocator` 메트릭 필수 |
| **MappedByteBuffer** | `FileChannel.map()` → mmap 시스템 콜 | `vm.max_map_count`(기본 65530) 초과 시 mmap 실패 → 즉시 서비스 장애 |

### 3-5. Metaspace 구조

- Metaspace는 Java Heap 외부 Native Memory에 존재 (PermGen 제거 후 JDK 8+)
- `VirtualSpaceList → Chunk → Block` 계층 구조로 클래스 메타데이터 관리
- 클래스 언로딩 시에만 반환 → Spring CGLIB 동적 프록시 과다 생성 시 누수 패턴 발생
- `CompressedClassPointers` 활성화 시 별도 `CompressedClassSpace` (기본 1GB) 사용

### 3-6. Cache Line / False Sharing

Cache Line 크기는 일반적으로 **64 bytes**. 핀테크 결제 처리에서 주요 성능 저하 원인입니다.

- **False Sharing:** 서로 다른 스레드가 같은 Cache Line 내 다른 필드를 수정 시 캐시 무효화 루프
- **JDK 8+:** `@Contended` 어노테이션으로 패딩 자동 삽입 (`-XX:-RestrictContended`)
- 결제 카운터, 상태 플래그 등 고빈도 업데이트 필드에 적용 필수

### 3-7. Copy-on-Write (CoW) / mmap / brk

| 메커니즘 | JVM 연관 동작 | 분석 포인트 |
|---------|-------------|------------|
| **CoW** | `fork()` 기반 프로세스 복제 (예: JVM heap dump) | Heap dump 시 Copy-on-Write로 일시적 메모리 2배 증가 가능 |
| **mmap** | 힙 예약, Direct Buffer, Jar 파일 로딩 | `/proc/<pid>/maps`에서 anonymous mmap 영역 크기 확인 |
| **brk** | 소량 Native 할당 (일부 JVM 내부) | glibc malloc arena 경합 시 brk 확장 지연 |

---

## 4. Memory 병목 및 위험 발생 지점

### 4-1. 병목/위험 매트릭스

| 병목 유형 | 위험도 | JVM 발생 원인 | 핀테크 영향 |
|----------|--------|-------------|------------|
| **Heap Exhaustion** | 🔴 Critical | 객체 폭증, 캐시 과다, 메모리 누수 | `java.lang.OutOfMemoryError` → 결제 서비스 전면 중단 |
| **GC STW (Full GC)** | 🔴 Critical | Old Gen 과다, Humongous 객체 | 수 초간 모든 트랜잭션 정지 → 결제 타임아웃 폭증 |
| **Native Memory OOM** | 🔴 Critical | Direct Buffer 누수, Thread 과다 | `Cannot reserve memory` → JVM 비정상 종료 |
| **Container OOMKill** | 🔴 Critical | RSS > `memory.max` (cgroup) | K8s Pod 강제 종료 → 진행 중 결제 롤백 필요 |
| **Swap Thrashing** | 🔴 Critical | 물리 메모리 부족, `swappiness > 0` | GC가 Swap된 힙 페이지 접근 시 수백ms 지연 |
| **Metaspace OOM** | 🟠 High | 클래스 누수, ClassLoader 미해제 | Metaspace 고갈 → JVM 재기동 필요 |
| **GC-induced Latency** | 🟠 High | Allocation Rate 과다, Promotion 실패 | P99 레이턴시 급등 → SLA 위반 |
| **Direct Buffer Exhaustion** | 🟠 High | `MaxDirectMemorySize` 초과 | Netty ByteBuf 할당 실패 → 네트워크 수신 불가 |
| **Memory Leak** | 🟠 High | ThreadLocal 미해제, 캐시 미만료 | 시간에 따라 점진적 OOM → 야간 피크 시간대 장애 |
| **Thread Stack Explosion** | 🟠 High | 스레드 풀 크기 제한 없음 | Native Memory 급증 → OOM 또는 성능 저하 |
| **NUMA Remote Access** | 🟡 Medium | JVM 힙이 Remote Node에 할당 | 메모리 접근 레이턴시 증가 → Tail Latency 상승 |
| **Memory Fragmentation** | 🟡 Medium | Netty Arena 파편화, malloc 파편화 | 실제 여유 메모리 있음에도 대형 객체 할당 실패 |
| **Page Cache Eviction** | 🟡 Medium | 메모리 압박으로 Page Cache 축소 | Jar 재로딩 시 Disk I/O → 클래스 로딩 지연 |
| **TLB Miss 폭증** | 🟡 Medium | HugePage 미사용, 힙 분산 | GC 스캔 속도 저하 → STW 시간 증가 |

### 4-2. Tail Latency 증가 원인 분석

> 핀테크 결제 환경에서 P99/P999 레이턴시 급등의 90% 이상은 아래 4가지 원인 중 하나에서 기인합니다.

| 원인 | 영향 크기 | 확인 방법 |
|------|---------|----------|
| **GC STW** | Young GC 수ms ~ Full GC 수초 | JFR GC 이벤트와 레이턴시 시계열 상관관계 |
| **Major Page Fault** | 수백ms (Swap 접근 시) | `vmstat`의 `si`(swap in) 지표 |
| **Safe Point Bias** | JVM Safe Point 도달 대기 시간 | `-XX:+PrintSafepointStatistics` |
| **Lock Inflation** | Thin Lock → Fat Lock 전환 | `jstack`, async-profiler lock profiling |

---

## 5. SRE 관점 모니터링 지표

### 5-1. 핵심 모니터링 지표 전체 목록

| 지표 | 계층 | 수집 방법 | 임계값 / 분석 포인트 |
|------|------|----------|-------------------|
| **RSS** | OS | `/proc/<pid>/status` | Container Limit의 80% 초과 시 경보. 지속 증가 = 누수 의심 |
| **VSZ** | OS | `/proc/<pid>/status` | RSS와 큰 차이 = Native 예약 과다 |
| **JVM Heap Usage** | JVM | `jstat -gcutil` / JMX | Old Gen 80% 이상 지속 시 Major GC 임박 |
| **GC Pause Time** | JVM | JFR / `gc.log (-Xlog:gc*)` | Young GC > 100ms, Full GC > 1s 즉시 경보 |
| **Allocation Rate** | JVM | `async-profiler alloc` / JFR | 초당 수 GB 할당 = GC 압박. 핫스팟 코드 경로 최적화 필요 |
| **Promotion Rate** | JVM | `jstat -gcnew` / JFR | 높음 = Survivor Space 부족 또는 수명 긴 임시 객체 |
| **Metaspace Usage** | JVM | `jcmd NMT` / JMX | 지속 증가 = ClassLoader 누수. MaxMetaspaceSize 80% 경보 |
| **Direct Buffer Usage** | JVM Native | JMX `BufferPoolMXBean` | `MaxDirectMemorySize`의 90% 초과 시 Netty 할당 실패 가능 |
| **Native Memory (NMT)** | JVM Native | `jcmd VM.native_memory` | Thread 영역 급증 = 스레드 폭발. Code Cache 한도 확인 |
| **Page Fault (Minor/Major)** | OS | `/proc/<pid>/stat` / `sar` | Major Fault 발생 = Swap 사용 중. 즉시 조사 필요 |
| **Swap In/Out** | OS | `vmstat 1` / `sar -W` | `si > 0` = 핀테크 환경 심각 경보. GC STW 폭발적 증가 |
| **PSI Memory** | OS | `/proc/pressure/memory` | `some > 10%`, `full > 0` = 메모리 압박 심각 |
| **Slab Usage** | OS Kernel | `slabtop -o` | dentry/inode 폭증 = Jar 파일 로딩 과다 |
| **NUMA Miss Rate** | HW | `numastat -m` | `numa_miss / numa_hit > 5%` = NUMA 바인딩 적용 검토 |
| **Dirty Page** | OS | `/proc/meminfo` | `Dirty` 값 과다 = writeback 지연, I/O 병목 |
| **Memory Fragmentation Ratio** | OS | `/proc/buddyinfo` | 고차 블록(order 8~10) 부족 = Huge Page 할당 실패 |
| **P95/P99 Latency** | App | APM / Micrometer | GC 이벤트와 시계열 상관관계. 결제 SLA 기준 100ms 이하 |
| **Netty ByteBuf 사용량** | Framework | `PooledByteBufAllocator` 메트릭 | `usedDirectMemory`, `usedHeapMemory` 실시간 추적 |

---

## 6. 장애 시나리오

### 6-1. Java Heap OOM

**시나리오:** 결제 트래픽 급증 → 결제 객체 생성 폭증 → Old Gen 포화 → Full GC 반복 → Heap OOM

**징후:**
- Old Gen 사용률 95%+ 지속
- Full GC 빈도 증가, GC 이후에도 메모리 미반환
- `java.lang.OutOfMemoryError: Java heap space`

**원인:**
- 결제 결과 캐시 무제한 증가
- Session 객체 미해제
- Static Collection 누수

**대응:**
```bash
# Heap Dump 수동 생성
jmap -dump:format=b,file=/tmp/heap.hprof <pid>

# JVM 옵션으로 자동 생성
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/var/dumps/
```
Eclipse MAT로 Dominator Tree 분석 → 누수 클래스 특정

### 6-2. Container OOMKill

**시나리오:** K8s 결제 Pod → Native Memory 누수 → RSS > `memory.limit` → cgroup OOMKiller → Pod Restart

> ⚠️ Container OOMKill은 Heap OOM과 달리 JVM이 인지하지 못한 채 프로세스가 종료됩니다.  
> 결제 트랜잭션이 롤백되지 않을 수 있어 **데이터 정합성 위험**이 존재합니다.

**확인:**
```bash
kubectl describe pod <pod-name>   # OOMKilled 상태 확인
dmesg | grep oom-killer           # 커널 OOM 로그
```

**원인 분석:** `-Xmx`와 Container Limit의 합산 오류. Native Memory(Metaspace + Direct Buffer + Thread)가 Limit 초과

**예방:**
- `-XX:+UseContainerSupport` (JDK 10+) 활성화
- `-XX:MaxRAMPercentage=75.0` 사용
- Container Limit = `-Xmx` × 1.4 이상 확보

### 6-3. GC-induced Latency 장애

**시나리오:** 결제 피크 시간(18:00~20:00) → Allocation Rate 폭증 → G1GC Mixed GC 빈발 → P99 > 500ms → SLA 위반

**원인 추적:**
1. JFR 수집 → GC 이벤트 타임스탬프와 P99 레이턴시 상관관계 확인
2. `async-profiler alloc` → 어느 코드에서 객체 폭증하는지 Flamegraph 특정
3. 대상 코드: 트랜잭션마다 새 DTO 생성, JSON 직렬화 시 중간 객체 폭증

**튜닝:**
```
-XX:G1HeapWastePercent=5
-XX:G1MixedGCCountTarget=8
Object Pool 도입 (결제 DTO 재사용)
```

### 6-4. Direct Buffer Exhaustion

**시나리오:** Netty 기반 결제 게이트웨이 → DirectBuffer 할당 후 미반환 → `MaxDirectMemorySize` 초과 → IOException

**원인:** `ByteBuf.release()` 누락, 비동기 처리 중 예외 발생 시 finally 블록 미처리

**확인:**
```
JMX: java.nio:type=BufferPool,name=direct
     → MemoryUsed / TotalCapacity 비율
```

**대응:**
```bash
# Netty 누수 감지 활성화 (운영 환경 일시적 사용)
-Dio.netty.leakDetection.level=PARANOID
```

### 6-5. Swap Storm

**시나리오:**
```
JVM Heap 80% → GC 시작 → GC가 Swap된 힙 페이지 접근
→ Major Page Fault → GC 시간 수십 배 증가
→ STW 수십 초 → 결제 시스템 전면 타임아웃
```

> 결제 환경에서 Swap은 반드시 비활성화(`vm.swappiness=0`) 또는 최소화해야 합니다.  
> Swap을 사용하는 JVM은 GC STW 시간이 수십 배 증가하여 사실상 서비스 불능 상태가 됩니다.

### 6-6. Memory Fragmentation

**시나리오:** Netty PooledByteBufAllocator Arena 파편화 → 대형 ByteBuf 할당 실패 → 결제 메시지 처리 불가

**확인:**
```bash
# Netty Arena 상태
PooledByteBufAllocator.DEFAULT.metric()

# OS 메모리 파편화
cat /proc/buddyinfo
```

### 6-7. Metaspace OOM

**시나리오:** Spring Boot 앱 장기 운영 → 동적 CGLIB 프록시 클래스 누적 → Metaspace 고갈 → `OutOfMemoryError: Metaspace`

**원인:** `MaxMetaspaceSize` 미설정, ClassLoader 미해제 (OSGi, Groovy 동적 로딩 환경)

**대응:**
```bash
# Metaspace 클래스 로딩 현황
jcmd <pid> VM.native_memory summary | grep Metaspace

# 클래스 로딩 통계
jstat -class <pid> 1000
```

---

## 7. 튜닝 포인트

### 7-1. JVM Heap / GC 튜닝

| JVM 옵션 | 권장값 (결제 서버 기준) | 근거 |
|---------|----------------------|------|
| `-Xms` / `-Xmx` | **동일하게 설정** (예: `-Xms8g -Xmx8g`) | 힙 리사이징 오버헤드 제거. 기동 시 전체 예약으로 Page Fault 사전 처리 |
| `-XX:+UseG1GC` | 기본값 (JDK 9+) | Region 기반 부분 회수로 STW 시간 예측 가능. 결제 환경 표준 |
| `-XX:MaxGCPauseMillis` | `100~200` (결제 SLA 기반) | G1GC 목표 Pause 시간. 너무 작으면 GC 빈도 증가 |
| `-XX:G1HeapRegionSize` | `8m~16m` (대형 결제 객체 고려) | Humongous 객체 최소화. 1MB 이상 객체 비율에 따라 조정 |
| `-XX:+AlwaysPreTouch` | **활성화 (운영 필수)** | JVM 기동 시 전체 힙 물리 페이지 커밋 → 런타임 Page Fault 제거 |
| `-XX:MaxMetaspaceSize` | `256m~512m` | 무제한 증가 방지. 설정 안 하면 Native OOM 위험 |
| `-XX:ReservedCodeCacheSize` | `256m~512m` | JIT 코드 캐시 부족 시 역컴파일 → 성능 급락 |
| `-XX:MaxDirectMemorySize` | Netty 사용량 × 2 (예: `2g`) | Direct Buffer 한도 명시. 미설정 시 `-Xmx`와 동일 → Container OOM 위험 |
| `-Xss` | `256k` (일반 스레드) | 스레드 수 × Xss = 스레드 스택 총량. 가상 스레드 도입 시 대폭 절감 |
| `-XX:+UseNUMA` | NUMA 서버 환경 | G1GC와 연동하여 Eden 영역을 NUMA Local에 우선 할당 |
| `-XX:+UseHugeTLBFS` | HugePage 사전 구성 시 | TLB Miss 감소 → GC 스캔 및 객체 접근 성능 향상 |
| `-XX:NativeMemoryTracking` | `summary` (운영) / `detail` (조사 시) | Native Memory 영역별 추적. 약 5~10% 오버헤드 |

### 7-2. OS 커널 튜닝

| 커널 파라미터 | 권장값 | JVM 연관 효과 |
|-------------|--------|-------------|
| `vm.swappiness` | **0 또는 1** | Swap 방지 → GC STW 안정화 |
| `vm.max_map_count` | `262144` 이상 | mmap 영역 수 한도. Kafka, JVM 클래스 로딩 mmap 포함 |
| `transparent_hugepage` | **`never` 또는 `madvise`** | THP 비활성화 → `kcompactd` CPU 스파이크 제거 |
| `vm.overcommit_memory` | `0` (기본값) | JVM 대형 힙 예약 가능. `=2` 설정 시 JVM 기동 실패 가능 |
| `kernel.numa_balancing` | **`0`** (NUMA 서버) | 자동 NUMA 밸런싱이 JVM 힙 페이지 이동 시 성능 저하 유발 |
| `net.core.rmem_max` | `134217728` (128MB) | Netty Socket Recv Buffer 확장. 결제 고부하 환경 |

### 7-3. NUMA 바인딩

```bash
# JVM 힙을 NUMA Node 0에 강제 바인딩
numactl --membind=0 --cpunodebind=0 java -XX:+UseNUMA -jar app.jar

# K8s 환경
# topologySpreadConstraints + CPU Manager policy=static + NUMA-aware 설정
```

### 7-4. Container Memory Limit 설계 공식

```
Container Memory Limit
  = Heap(-Xmx)
  + Metaspace (~256MB)
  + Direct Buffer (MaxDirectMemorySize)
  + Thread Stack (스레드 수 × -Xss)
  + Code Cache (~256MB)
  + JVM Internal (~200MB)
  + 여유 (20%)

예시: -Xmx=8g, 200 스레드, DirectMemory=2g
  → 8192 + 256 + 2048 + 200 + 256 + 200 + 20% ≈ 13GB
```

**권장 JVM 옵션 (Container 환경):**
```bash
-XX:+UseContainerSupport          # cgroup 메모리 제한 자동 인식 (JDK 10+)
-XX:MaxRAMPercentage=75.0         # Container Limit 대비 Heap 비율 동적 설정
-XX:+AlwaysPreTouch               # 기동 시 물리 페이지 확보
-XX:+ExitOnOutOfMemoryError       # OOM 시 즉시 종료 (좀비 프로세스 방지)
```

### 7-5. GC Policy 비교

| GC | 특징 | 핀테크 권장 여부 |
|----|------|---------------|
| **G1GC** | Region 기반, 예측 가능한 Pause | ✅ 표준 (JDK 9+ 기본값) |
| **ZGC** | 서브 밀리초 STW, JDK 15+ 운영 안정 | ✅ 초저지연 요구 시 (JDK 17+) |
| **Shenandoah** | Concurrent Compaction, Red Hat 주도 | 🟡 ZGC 대안. 벤치마크 후 결정 |
| **ParallelGC** | 높은 처리량, 긴 STW | ❌ 결제 환경 부적합 |
| **CMS** | Deprecated (JDK 14에서 제거) | ❌ 사용 금지 |

---

## 8. 관련 Linux 명령어 및 분석 도구

### 8-1. OS 레벨 메모리 진단 명령어

| 명령어 | 주요 옵션 / 예시 | JVM 분석 포인트 |
|--------|----------------|----------------|
| `free -h -w` | `free -h -w -s 5` | `available` 메모리 추이. `buff/cache` 비율 확인 |
| `vmstat 1` | `vmstat -SM 1 10` | `si/so`(swap), `bi/bo`(I/O), `cs`(context switch) 동시 확인 |
| `top / htop` | `top -p <pid> -d 1` | RES(RSS), VIRT(VSZ), %MEM 실시간 추이 |
| `smem` | `smem -r -s rss \| head -10` | PSS(Proportional Set Size): 공유 메모리 지분 포함 실제 점유량 |
| `pmap -x <pid>` | `pmap -x <pid> \| grep heap` | 힙/스택/Native 영역별 매핑 상세. Dirty Page 확인 |
| `numastat -m` | `numastat -p <pid>` | JVM 프로세스의 NUMA 노드별 메모리 분포 |
| `slabtop -o` | `slabtop -s c` | dentry/inode 슬랩 폭증 = Jar 파일 로딩 과다 |
| `sar -r 1` | `sar -r ALL 1 10` | `kbmemfree`, `kbavail`, `kbbuffers`, `kbcached` |
| `cat /proc/pressure/memory` | `watch -n 1 cat /proc/pressure/memory` | PSI `full > 0` = 즉각적인 메모리 위기 상태 |
| `/proc/<pid>/smaps_rollup` | `cat /proc/<pid>/smaps_rollup` | Rss/Pss/Shared/Private 요약. Native 영역 정밀 분석 |
| `cat /proc/meminfo` | `grep -E 'MemAvail\|Dirty\|Slab\|Huge'` | 시스템 전체 메모리 상태 개요 |
| `perf mem` | `perf mem -t load record -p <pid>` | NUMA Remote Access, Cache Miss 하드웨어 레벨 분석 |

### 8-2. JVM 전용 분석 도구

| 도구 | 사용 예시 | 분석 목적 |
|------|----------|----------|
| `jstat` | `jstat -gcutil <pid> 1000` | GC 통계 실시간 모니터링. S0/S1/Eden/Old/Meta 사용률 |
| `jcmd NMT` | `jcmd <pid> VM.native_memory summary` | Native Memory 영역별 상세 사용량. Thread/Code/Heap/Metaspace 분리 |
| `jmap` | `jmap -histo:live <pid>` | 클래스별 객체 수/크기. 누수 클래스 1차 탐지 |
| **JFR** | `jcmd <pid> JFR.start duration=60s filename=out.jfr` | GC 이벤트, Allocation Profiling, Safepoint 종합 분석. 저오버헤드 |
| **async-profiler** | `./profiler.sh -e alloc -d 30 -f alloc.html <pid>` | Allocation Hotspot Flamegraph. 객체 폭증 코드 경로 특정 |
| **Eclipse MAT** | heap dump 파일 분석 | Dominator Tree, Leak Suspects 보고서. Heap OOM 사후 분석 표준 |
| `VisualVM / JConsole` | JMX 연결 모니터링 | Heap/Non-Heap, GC 활동, Thread 상태 실시간 GUI |
| `perf mem` | `perf mem -t load record -p <pid>` | NUMA Remote Access, Cache Miss 하드웨어 레벨 분석 |
| **eBPF / bpftrace** | `bpftrace -e 'tracepoint:kmem:mm_page_alloc {}'` | 커널 페이지 할당 추적. GC와 OS 페이지 할당 상관관계 분석 |

### 8-3. NMT (Native Memory Tracking) 활성화 및 분석 절차

```bash
# 1. JVM 시작 옵션 추가
-XX:NativeMemoryTracking=summary   # 운영 환경 (오버헤드 ~5%)
-XX:NativeMemoryTracking=detail    # 조사 환경 (오버헤드 ~10%)

# 2. 베이스라인 설정
jcmd <pid> VM.native_memory baseline

# 3. 일정 시간 후 차이 비교
jcmd <pid> VM.native_memory summary.diff

# 출력 영역:
# Java Heap / Metaspace / Class / Thread / Code / GC / Compiler / Internal / Other
```

> NMT는 약 5~10%의 성능 오버헤드가 있으므로 운영 환경에서는 `summary` 레벨만 사용하고,  
> 누수 조사 시에만 일시적으로 `detail`로 전환합니다.

### 8-4. JFR 기반 메모리 분석 워크플로우

```bash
# 1. JFR 수집 (운영 환경 저오버헤드)
jcmd <pid> JFR.start name=mem_profile \
  settings=profile duration=120s filename=/tmp/mem.jfr

# 2. JFR 덤프 (실행 중 즉시 파일 저장)
jcmd <pid> JFR.dump name=mem_profile filename=/tmp/mem_dump.jfr

# 3. JFR 중지
jcmd <pid> JFR.stop name=mem_profile
```

**JDK Mission Control(JMC) 분석 항목:**
- `GC → Heap After GC` 추이 → Old Gen 증가 패턴
- `Memory → Allocation Profiling` → Hotspot 클래스 및 스택 추적
- `VM Operations → Safepoint` → STW 소요 시간
- `GC → Pause Phases` → GC 단계별 시간 분포

### 8-5. 빠른 진단 체크리스트

```bash
# --- Step 1: 시스템 전체 메모리 개요 ---
free -h -w
cat /proc/pressure/memory

# --- Step 2: JVM 프로세스 메모리 상세 ---
ps -eo pid,cmd,%mem,rss,vsz --sort=-rss | grep java | head -5
cat /proc/<pid>/smaps_rollup

# --- Step 3: GC 실시간 상태 ---
jstat -gcutil <pid> 1000 10

# --- Step 4: Native Memory 분석 ---
jcmd <pid> VM.native_memory summary

# --- Step 5: NUMA 분포 확인 ---
numastat -p <pid>

# --- Step 6: Swap 사용 여부 ---
vmstat -SM 1 5 | awk '{print $7, $8}'   # si, so 컬럼
```

---

*JVM Runtime Memory Resource Analysis | FinTech Payment SRE*

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*