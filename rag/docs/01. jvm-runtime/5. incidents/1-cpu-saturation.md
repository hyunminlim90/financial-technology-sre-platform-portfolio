# JVM Runtime — CPU Saturation 장애 분석
## FinTech 결제 시스템 SRE 관점 | E2E CPU 계층 분석 (Hardware → Linux Kernel → JVM Runtime → Framework → Application)

> 정독: 0회

## 목차

1. [CPU Saturation이란?](#1-cpu-saturation이란)
2. [하부 인프라 및 커널 레이어의 CPU Saturation 연동 기전](#2-하부-인프라-및-커널-레이어의-cpu-saturation-연동-기전)
3. [JVM 내부 서브시스템별 CPU Saturation 유발 기전](#3-jvm-내부-서브시스템별-cpu-saturation-유발-기전)
4. [2026년 현재 최신 기술 동향 및 핀테크 적용 현황](#4-2026년-현재-최신-기술-동향-및-핀테크-적용-현황)
5. [SRE 관점 모니터링 지표](#5-sre-관점-모니터링-지표)
6. [CPU Saturation 진단 절차 및 트러블슈팅 런북](#6-cpu-saturation-진단-절차-및-트러블슈팅-런북)
7. [튜닝 및 운영 원칙](#7-튜닝-및-운영-원칙)

---

## 1. CPU Saturation이란?

CPU Saturation은 CPU 사용 가능 시간이 한계에 도달하여 애플리케이션 처리 지연이 발생하는 상태다.

> **핵심:** CPU 사용률 수치 자체보다 **CPU가 왜 소진되는가**를 계층별로 추적하는 것이 SRE의 본질적 역할이다.

### 1.1 대표 증상

| 증상 | 설명 |
|------|------|
| 응답 시간 증가 | P95/P99 Latency 급등 |
| API Timeout | 결제 트랜잭션 처리 초과 |
| TPS 감소 | 처리량 저하 |
| Queue 적체 | Thread Pool / Request Queue 대기 증가 |

### 1.2 CPU Saturation 발생 위치

CPU 포화는 단일 계층에서 발생하지 않는다. 아래 전 계층이 원인이 될 수 있다.

```
Application Layer     ← 무한 루프, CAS Spin, 비즈니스 로직 폭증
       │
JVM Runtime Layer     ← GC, JIT Compiler, Thread 경합, Lock Contention
       │
Linux Kernel Layer    ← CFS Scheduler, Context Switch, Cgroup Throttling
       │
Physical CPU Layer    ← NUMA Miss, Cache Coherency, Hardware Interrupt
```

> **Container 환경 주의:** CPU 사용률이 낮더라도 CPU Quota(cgroup `cpu.max`)를 모두 소진하면 Throttling이 발생하여 CPU Saturation과 동일한 지연 장애가 나타난다.

---

## 2. 하부 인프라 및 커널 레이어의 CPU Saturation 연동 기전

### 2.1 CFS 스케줄러 대역폭 제한 (CPU Throttling)

| 구성 요소 | 설명 |
|----------|------|
| `cpu.cfs_period_us` | CFS 주기 (기본값: 100ms) |
| `cpu.cfs_quota_us` | 해당 주기 내 허용 CPU 가동 시간 총량 |
| `cpu.max` (cgroup v2) | `quota / period` 로 표현된 CPU 상한 |

**핀테크 CPU Throttling 인과관계:**

```
결제 트래픽 스파이크
       │
JVM 활성 스레드 증가 → cfs_quota_us 소진
       │
커널이 Pod를 강제 유휴(Throttle) 상태로 전환
       │
물리 CPU 사용률 100% 미달이더라도
container.cpu.throttled_time 폭증
       │
애플리케이션 관점에서 CPU 자원 고갈과 동일한 처리 지연(Tail Latency) 발생
```

**진단:**
```bash
# cgroup Throttling 상태 확인
cat /sys/fs/cgroup/cpu.stat | grep -E 'throttled'

# Kubernetes 컨테이너 CPU Throttle 비율
container_cpu_cfs_throttled_periods_total / container_cpu_cfs_periods_total
```

### 2.2 NUMA 아키텍처 CPU 스케줄링 미스매치

멀티 소켓 하드웨어에서 커널 스케줄러가 JVM 고부하 스레드를 원격 NUMA 노드 CPU로 이주(Migration)시킬 때 Remote Memory Access 레이턴시가 누적된다.

```
로컬 NUMA 노드 메모리 접근:  ~80ns
원격 NUMA 노드 메모리 접근: ~160ns (2배 이상)
       │
Memory Stall Cycles 폭증
       │
CPU가 비즈니스 연산 대신 메모리 버스 동기화 대기(Spinning)에 사이클 낭비
       │
호스트 레벨 CPU 사용률 최상단 도달
```

**진단:**
```bash
# NUMA 구성 확인
numactl --hardware

# NUMA 미스 통계
numastat -p <JVM_PID>

# perf NUMA 이벤트
perf stat -e node-load-misses,node-store-misses -p <PID>
```

### 2.3 커널 Context Switching 폭증

Java Platform Thread는 Linux LWP(Light-Weight Process)와 1:1 매핑된다. 스레드 풀이 과도하게 확장된 시스템에서는 CPU 자원이 비즈니스 연산이 아닌 커널 모드 스케줄링 오버헤드로 소진된다.

| Context Switch 유형 | 발생 원인 | 영향 |
|--------------------|---------|------|
| `voluntary_context_switches` | Lock 대기, I/O 블로킹 등 스레드 자발적 양보 | sys CPU 완만한 증가 |
| `involuntary_context_switches` | 커널 스케줄러 강제 선점 (time slice 초과) | sys CPU 급격한 증가 |

```
스레드 수 과다 (수백~수천 개)
       │
CPU: 실제 비즈니스 처리보다
  └─ Thread 레지스터 저장/복원
  └─ 스케줄러 실행
에 더 많은 시간을 소비
       │
sys CPU 사용률 비정상 상승 (30% 이상)
```

**진단:**
```bash
# 프로세스별 Context Switch 수
pidstat -w -p <JVM_PID> 1

# 시스템 전체 Context Switch
vmstat 1 | awk '{print $12}'   # cs 열

# /proc 직접 확인
cat /proc/<PID>/status | grep -E 'voluntary|nonvoluntary'
```

### 2.4 커널-애플리케이션 인터페이스 계층 구조

CPU Saturation 진단 시 아래 인터페이스 계층이 어디서 CPU를 소모하는지 교차 확인이 필요하다.

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 |
|------|--------|------------|--------------|
| 시스템 콜 | App → Kernel | `syscall` 명령어, User Mode → Kernel Mode 전환 (Ring 3→0) | `strace`, `perf trace`, `/proc/PID/syscall` |
| 하드웨어 인터럽트 (IRQ) | Hardware → Kernel | NIC 패킷 수신, DMA 완료 신호 → IRQ Handler 실행 | `/proc/interrupts`, `mpstat %irq` |
| 소프트 인터럽트 (SoftIRQ) | Kernel 후처리 | TCP/IP Stack 처리, `ksoftirqd` 스레드 | `mpstat %soft`, `/proc/softirqs` |
| cgroup / CPU Throttling | Kernel 자원 격리 | CFS Bandwidth Control: Quota 소진 시 Container Throttling | `/sys/fs/cgroup/cpu.stat`, `cadvisor` |
| 커널 스케줄러 (CFS) | Kernel 능동 개입 | Red-Black Tree 기반 vruntime 관리, Runqueue / Wait Queue | `/proc/schedstat`, `perf sched`, `vmstat cs` |
| PSI (압력 지표) | Kernel 관찰 인터페이스 | CPU/Memory/IO 자원 부족 압력 측정 (`some` / `full`) | `/proc/pressure/cpu` |

---

## 3. JVM 내부 서브시스템별 CPU Saturation 유발 기전

### 3.1 가비지 컬렉터 (GC) CPU 오버헤드

#### ① G1 GC — Evacuation Failure 및 Full GC

```
Heap 단편화 심화 / Allocation Rate > GC 수거 속도
       │
Evacuation Phase (멀티스레드 힙 스캔 + Compaction)
       │
메모리 확보 실패 시
       ▼
Full GC 발동 (단일 스레드, Mark-Sweep-Compact)
       │
JVM 프로세스가 오직 메모리 회수만을 위해
하드웨어 CPU 코어 전체 사이클 점유
       │
결제 비즈니스 스레드 완전 결빙 상태 (STW)
```

**위험 상황 트리거:**
- Heap 부족 / Promotion 실패
- Allocation Rate 급증
- Old Generation 압박

#### ② ZGC — Allocation Spike 및 CPU 경합

ZGC는 STW를 1ms 미만으로 통제하기 위해 `Load Barrier` + `Concurrent Relocation`을 사용한다. GC 스레드와 애플리케이션 스레드가 **동일 CPU 코어를 공유**하며 동시 실행되므로, Allocation Spike 발생 시 GC 스레드가 비즈니스 연산 스레드의 CPU 사이클을 잠식한다.

```
트래픽 급증 → 대량 객체 생성 (Allocation Spike)
       │
ZGC: GC 작업 스레드 CPU 할당 우선순위 강제 상향
       │
비즈니스 연산 스레드 CPU 사이클 잠식
       │
증상: GC Pause는 정상, CPU 사용량 증가, Tail Latency 수백ms 증가
```

**진단:**
```bash
# GC 로그 활성화
-Xlog:gc*:file=/var/log/gc.log:time,uptime:filecount=10,filesize=50m

# GC CPU 점유 지표 (Prometheus / Micrometer)
jvm_gc_cpu_time_seconds_total
jvm_gc_pause_seconds_sum
```

### 3.2 JIT 컴파일러 CPU 오버헤드

#### ① C2 Compiler Queue 병목

JVM 기동 초기 또는 신규 결제 모듈 배포 직후, 다수의 바이트코드가 C2 컴파일러 큐에 동시 적재된다. C2는 탈출 분석(Escape Analysis), 루프 최적화 등 고도화된 그래프 기반 최적화를 수행하므로 무거운 CPU 연산이 수반된다.

```
서비스 시작 직후 / 대규모 배포
       │
Hot Method 증가 → Compiler Queue 증가
       │
C2 Compilation Thread CPU 점유 급등
       │
비즈니스 처리 스레드와 CPU 경합
```

#### ② Code Cache 포화 → 인터프리터 롤백

```
-XX:ReservedCodeCacheSize 협소 설정
       │
Code Cache 포화 (allocated ≈ max_size)
       │
JIT 컴파일 영구 중단 + 기존 최적화 코드 파기
       │
전면 Tier 0 (Interpreter Mode) 강제 롤백
       │
동일 결제 트랜잭션 처리에 필요한 CPU 사이클 10배 이상 증가
       │
CPU Saturation 고착화 장애
```

**진단:**
```bash
# Code Cache 상태 실시간 확인
jcmd <PID> VM.codecache

# 출력에서 확인할 항목:
# max_size vs allocated_size (99% 이상 = 위험)
# Compilation: disabled 플래그 활성화 여부
```

**즉시 조치:**
```bash
# JVM 플래그 상향 조정 (다음 배포 주기 적용)
-XX:ReservedCodeCacheSize=512m
-XX:InitialCodeCacheSize=512m
```

### 3.3 애플리케이션 동시성 구조 및 자원 경합

#### ① CAS Spinlock — 무한 루프 CPU 고착

`java.util.concurrent.atomic` 계열이 고경합 상황에서 연속 실패 시 스레드가 CPU를 점유한 채 루프를 도는 Spinning 상태가 된다.

```
대량 금융 원장 변경 → 단일 계좌/공통 버퍼 집중
       │
AtomicInteger, ConcurrentHashMap CAS 연속 실패
       │
스레드: Context Switch 없이 CPU 점유 채 무한 재시도
       │
CPU 코어 100% 포화 (의미 없는 루프 연산)
```

**대표 사례:** 결제 원장의 특정 계좌 잔액 변경이 수천 TPS로 집중될 경우

#### ② Heavy Monitor Contention (synchronized)

```
다수 스레드 → 동일 synchronized 블록 진입 시도
       │
락 획득 실패 → 커널 레벨 Wait Queue 진입/복귀 반복
       │
CPU sys 모드 오버헤드 동반 폭증
       │
involuntary_context_switches 급증
```

**진단:**
```bash
# Thread Dump에서 BLOCKED 상태 스레드 확인
jstack <PID> | grep -E 'BLOCKED|waiting to lock'

# Monitor Contention 통계
jcmd <PID> Thread.print

# Async-Profiler Lock 프로파일링
./asprof -e lock -d 30 -f lock_profile.html <PID>
```

---

## 4. 2026년 현재 최신 기술 동향 및 핀테크 적용 현황

### 4.1 Virtual Threads (Project Loom) — JDK 21~25 LTS

**전통적 Platform Thread 아키텍처의 CPU Saturation 원인:**
- Context Switching 오버헤드 (스레드 수 과다)
- OS 스레드 + 메모리 동반 고갈

**Virtual Thread 아키텍처 도입 후 변화:**

```
Virtual Thread (수십만 개 생성 가능)
       │
Carrier Thread (OS Thread, CPU Core 수에 비례)
       │
I/O 블로킹 발생 시: Virtual Thread → Carrier Thread Unmount
다른 Virtual Thread 즉시 탑재 → CPU 처리 밀도 극대화
```

**새로운 CPU Saturation 위험 — Carrier Thread Starvation:**

```
CPU 집중 연산 (JSON 파싱, 복호화, 대규모 계산) 수천 개 동시 유입
       │
Virtual Thread: I/O 대기 없음 → Unmount 기전 미동작
       │
소수의 Carrier Thread가 CPU 연산 작업에 완전 점유
       │
신규 Virtual Thread 스케줄 불가
       │
결제 처리 큐 폭발 (새로운 형태의 CPU Saturation)
```

**필수 격벽 설계:**
```java
// CPU 집중 작업은 별도 Platform Thread Pool로 격리
ExecutorService cpuBoundPool = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors(),
    Thread.ofPlatform().factory()
);

// Virtual Thread는 I/O 집중 작업에만 사용
ExecutorService ioPool = Executors.newVirtualThreadPerTaskExecutor();
```

**Carrier Thread Pinning 감지:**
```bash
# JVM 플래그로 Pinning 이벤트 감지
-Djdk.tracePinnedThreads=full

# JFR 이벤트
jcmd <PID> JFR.start duration=60s filename=pinning.jfr
jfr print --events jdk.VirtualThreadPinned pinning.jfr
```

### 4.2 GraalVM Native Image (AOT 컴파일) — 2026년 클라우드 네이티브 결제 노드 표준

| 구분 | 표준 JVM (JIT) | GraalVM Native Image (AOT) |
|------|--------------|---------------------------|
| JIT Warm-up CPU Saturation | ✅ 기동 초기 발생 | ❌ 런타임 컴파일러 서브시스템 부재 → 0으로 수렴 |
| 피크 트래픽 연산 효율 | JIT 최적화 완료 후 최고 성능 | PGO 미적용 시 특정 루프 연산 효율 다소 저하 |
| 기동 시간 | 수초 ~ 수십초 | 수십ms (결제 Pod 빠른 Scale-out 유리) |

**운영 원칙:** GraalVM Native Image 빌드 파이프라인에 PGO(Profile-Guided Optimization) 프로세스 필수 결합 → 피크 트래픽 시 순수 비즈니스 연산 CPU Saturation 선제 방어.

---

## 5. SRE 관점 모니터링 지표

### 5.1 OS / 커널 레벨

| 항목 | 주요 지표 / 명령어 | 임계치 (결제 서버) |
|------|-----------------|-----------------|
| CPU 사용률 분할 | `top`, `mpstat -P ALL 1` | `sys%` > 30% 시 Alert |
| CPU Throttling | `container_cpu_cfs_throttled_periods_total` | Throttle 비율 > 5% 시 Alert |
| Context Switch | `pidstat -w`, `vmstat cs` | 급증 추세 지속 시 Alert |
| NUMA Miss | `numastat`, `perf stat -e node-load-misses` | 증가 추세 감시 |
| SoftIRQ | `mpstat %soft` | 코어당 > 20% 시 Alert |
| PSI CPU Pressure | `/proc/pressure/cpu` | `full avg10 > 10` 시 Alert |

### 5.2 JVM 레벨

| 항목 | 주요 지표 | 임계치 |
|------|---------|--------|
| GC Pause Time | `jvm_gc_pause_seconds_sum` | > 200ms (ZGC 목표: < 10ms) |
| GC CPU 점유 | `jvm_gc_cpu_time_seconds_total` | 급증 추세 |
| Code Cache 사용률 | `jcmd VM.codecache` | `allocated / max > 90%` 시 Alert |
| Compiler Queue | JMX `java.lang:type=Compilation` | 큐 적체 지속 시 Alert |
| Thread Blocked | `jvm_threads_states{state="blocked"}` | 0 유지 목표 |
| Virtual Thread Pinning | JFR `jdk.VirtualThreadPinned` | 발생 즉시 Alert |
| Carrier Thread 점유 | JFR `jdk.VirtualThreadSubmitFailed` | 발생 즉시 Alert |

### 5.3 Application 레벨

| 항목 | 주요 지표 | 임계치 |
|------|---------|--------|
| P95/P99 Latency | `http_server_requests_seconds` | P99 > 500ms 시 Alert |
| TPS | `http_server_requests_total` rate | 기준선 대비 30% 이하 시 Alert |
| Thread Pool 대기 | `executor_pool_queue_remaining` | 0 수렴 시 Alert |

---

## 6. CPU Saturation 진단 절차 및 트러블슈팅 런북

### 6.1 SRE 실시간 진단 파이프라인

```
[ALERT: CPU Saturation 95%]
         │
         ▼
Step 1: 커널 CPU Mode 분할 쿼리
  top 또는 mpstat -P ALL 1
         │
  ├─ sys% 비정상 높음 (30% 이상)
  │       └─► [스레드 Context Switching 또는 Monitor Contention]
  │              → pidstat -w / jstack BLOCKED 분석
  │
  └─ user% 압도적 높음 (70% 이상)
           │
           ▼
Step 2: JVM GC CPU 점유 검증
  Prometheus: jvm_gc_pause / jvm_gc_cpu_time
           │
  ├─ GC 지표 폭증
  │       └─► [GC 메모리 고갈 압박형 Saturation]
  │              → GC 로그 분석 / Heap 튜닝
  │
  └─ GC 지표 정상, 순수 user CPU 폭증
           │
           ▼
Step 3: Async-Profiler Non-safepoint Sampling
  ./asprof -d 30 -f /var/log/jvm_dumps/cpu_flame.html <PID>
           │
  ├─ C2 Compiler Thread 최상단 점유
  │       └─► [Code Cache 전소 또는 JIT Warm-up 과부하]
  │              → jcmd VM.codecache 확인
  │
  └─ Business Logic 스레드 최상단 점유
           └─► [무한 루프 / CAS Spinlock / 결제 연산 폭증]
                  → Flame Graph 리프 노드 추적
```

### 6.2 원인 유형별 진단 명령어

#### Async-Profiler CPU Flame Graph

```bash
# 타깃 JVM 프로세스를 대상으로 30초간 CPU 사이클 샘플링
# (Non-safepoint 샘플링: jstack의 Safepoint Bias 오류 회피)
./asprof -d 30 -f /var/log/jvm_dumps/cpu_saturation_flame.html <PID>

# Flame Graph 판독 포인트:
# - 최상단 수평 폭이 가장 넓은 리프 노드 = CPU Saturation 직접 원인
# - GC 내부 스레드(ZGCTargetHint, G1ParScanThreadState) 면적 비율 확인
# - 비즈니스 로직(OrderValidator.validate() 등) 면적 비율 확인
```

#### Code Cache 상태 확인

```bash
# 런타임 JIT 컴파일러 상태 및 Code Cache 물리 할당 정밀 쿼리
jcmd <PID> VM.codecache

# 위험 판정 기준:
# max_size 대비 allocated_size ≥ 99%
# Compilation: disabled 플래그 활성화
# → 코드 캐시 고갈로 인한 인터프리터 롤백 확정
```

#### Context Switch 및 Lock 분석

```bash
# 시스템 전체 Context Switch 추이
vmstat 1 10

# JVM 프로세스 Context Switch 상세
pidstat -w -p <PID> 1

# Thread 상태 전체 덤프 (BLOCKED/WAITING 집중 확인)
jstack <PID> | grep -A 5 "BLOCKED\|waiting to lock"

# GC 관련 진단
jcmd <PID> GC.heap_info
jcmd <PID> VM.flags | grep -E 'HeapSize|GC|Code'
```

#### Virtual Thread Pinning 감지

```bash
# 런타임 JFR 수집
jcmd <PID> JFR.start duration=60s filename=/tmp/pinning.jfr

# Pinning 이벤트 분석
jfr print --events jdk.VirtualThreadPinned /tmp/pinning.jfr

# synchronized 블록 내 블로킹 I/O 추적
./asprof -e wall -d 30 -f wall_profile.html <PID>
```

### 6.3 장애 복구 조치 (Remediation)

#### 단기 긴급 조치 (Mitigation)

```bash
# 1. 원인 인스턴스를 라우팅에서 즉시 격리
kubectl cordon <node>
kubectl drain <node> --ignore-daemonsets

# 2. 컨테이너 수 강제 Scale-Out (코어당 결제 트랜잭션 밀도 분산)
kubectl scale deployment <payment-service> --replicas=<N>

# 3. CPU Throttling이 원인인 경우: Limit 일시 상향
kubectl patch deployment <payment-service> -p \
  '{"spec":{"template":{"spec":{"containers":[{"name":"app","resources":{"limits":{"cpu":"4"}}}]}}}}'
```

#### 런타임 파라미터 재배치 (다음 배포 주기 반영)

```bash
# Code Cache 상향 고착화
-XX:ReservedCodeCacheSize=512m
-XX:InitialCodeCacheSize=512m

# GC 튜닝 (저지연 우선)
-XX:+UseZGC
-XX:MaxGCPauseMillis=10
-Xms4g -Xmx4g

# Virtual Thread Pinning 감지 활성화
-Djdk.tracePinnedThreads=full

# JIT Warm-up 가속 (Tiered Compilation)
-XX:+TieredCompilation
-XX:ReservedCodeCacheSize=512m
```

---

## 7. 튜닝 및 운영 원칙

### 7.1 CPU Saturation 유형별 대응 매트릭스

| 유형 | 주요 원인 | 단기 조치 | 근본 해결 |
|------|---------|---------|---------|
| CPU Throttling | cgroup Quota 협소 | CPU Limit 상향 | Limit 재산정 + HPA 튜닝 |
| GC CPU 폭증 | Heap 부족 / Allocation Rate 과다 | Scale-Out | Heap 증설 / ZGC 전환 / 객체 생성 최적화 |
| Code Cache 고갈 | `ReservedCodeCacheSize` 협소 | 인스턴스 롤링 재시작 | 캐시 크기 상향 후 재배포 |
| CAS Spinlock | 고경합 공유 자원 집중 | 부하 분산 | 자료구조 분산 설계 (`LongAdder` 등) |
| Context Switch 과다 | 스레드 수 과다 | Thread Pool 축소 | Virtual Thread 전환 |
| Carrier Thread Starvation | Virtual Thread + CPU 집중 연산 혼재 | Platform Thread Pool로 격리 라우팅 | 격벽 설계 적용 |
| NUMA Miss | 스레드 NUMA 노드 간 이주 | `numactl` 바인딩 | JVM NUMA 인식 활성화 (`-XX:+UseNUMA`) |

### 7.2 JVM 시작 옵션 체크리스트

```bash
# GC: 저지연 우선 (FinTech 결제 서버)
-XX:+UseZGC
-XX:MaxGCPauseMillis=10
-Xms4g -Xmx4g                         # Heap 고정 (GC 예측성 향상)

# Code Cache
-XX:ReservedCodeCacheSize=512m
-XX:InitialCodeCacheSize=512m

# JIT
-XX:+TieredCompilation

# NUMA 인식
-XX:+UseNUMA

# Virtual Thread 진단
-Djdk.tracePinnedThreads=full

# GC 로그
-Xlog:gc*:file=/var/log/gc.log:time,uptime:filecount=10,filesize=50m
```

### 7.3 운영 진단 체크리스트 (SRE Daily)

```
[ ] container_cpu_cfs_throttled_periods_total 비율 < 5%
[ ] sys CPU 사용률 < 30%
[ ] GC STW Pause Time < 10ms (ZGC 기준)
[ ] GC CPU 사용 비율 < 전체 CPU의 10%
[ ] Code Cache: allocated / max < 90%
[ ] Thread BLOCKED 상태 수 = 0
[ ] Virtual Thread Pinning 이벤트 = 0
[ ] involuntary_context_switches 급증 추세 없음
[ ] P99 Latency < 500ms
[ ] CPU PSI full avg10 < 10
```

---

## 참고: CPU Saturation 원인 판별 요약 흐름도

```
CPU 사용률 급증
       │
       ├─ sys% 높음 ──► Context Switch / Monitor Contention / Kernel Overhead
       │                  도구: pidstat -w, vmstat, jstack BLOCKED
       │
       ├─ GC 지표 폭증 ──► GC 메모리 고갈 압박 (G1 Full GC / ZGC Allocation Spike)
       │                    도구: GC 로그, jcmd GC.heap_info, Async-Profiler
       │
       ├─ Compiler Thread 점유 ──► Code Cache 전소 / JIT Warm-up 과부하
       │                            도구: jcmd VM.codecache, Async-Profiler
       │
       ├─ Business Logic 점유 ──► 무한 루프 / CAS Spinlock / 결제 연산 폭증
       │                           도구: Async-Profiler Flame Graph
       │
       └─ CPU 사용률 정상 + 지연 발생 ──► CPU Throttling (cgroup Quota 고갈)
                                          도구: /sys/fs/cgroup/cpu.stat, cadvisor
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*