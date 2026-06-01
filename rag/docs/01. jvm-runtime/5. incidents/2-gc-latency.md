# JVM Runtime — GC Latency 장애 분석
## FinTech 결제 시스템 SRE 관점 | E2E GC Latency 계층 분석 (Hardware → Linux Kernel → JVM Runtime → Framework → Application)

> 정독: 0회

## 목차

1. [GC Latency란?](#1-gc-latency란)
2. [하부 인프라 및 Linux Memory Subsystem과의 연동 기전](#2-하부-인프라-및-linux-memory-subsystem과의-연동-기전)
3. [JVM GC별 Latency 유발 정밀 기전](#3-jvm-gc별-latency-유발-정밀-기전)
4. [Safepoint와 TTSP 미시 분석](#4-safepoint와-ttsp-미시-분석)
5. [GC 기술 진화 및 핀테크 채택 현황 (2026)](#5-gc-기술-진화-및-핀테크-채택-현황-2026)
6. [SRE 관점 모니터링 지표](#6-sre-관점-모니터링-지표)
7. [GC Latency 진단 절차 및 트러블슈팅 런북](#7-gc-latency-진단-절차-및-트러블슈팅-런북)
8. [2026 Production Baseline — 핀테크 특화 JVM 기동 Manifest](#8-2026-production-baseline--핀테크-특화-jvm-기동-manifest)

---

## 1. GC Latency란?

GC Latency는 가비지 컬렉션 수행 과정에서 발생하는 지연 시간이다. FinTech 결제 시스템에서 GC 지연이 증가하면 결제 승인 지연, Timeout 폭발, Tail Latency 급등으로 직결된다.

> **핵심:** GC Latency는 순수 JVM 내부 문제가 아니다. Hardware → Linux Kernel → JVM Runtime 전 계층의 상호작용을 통합적으로 분석해야 한다.

### 1.1 대표 증상

| 증상 | 설명 |
|------|------|
| API 응답시간 증가 | STW 동안 모든 비즈니스 스레드 정지 |
| 결제 승인 지연 | GC Pause가 결제 트랜잭션 처리 시간 초과 |
| Timeout 증가 | 외부 PG API, DB 커넥션 타임아웃 연쇄 발생 |
| TPS 감소 | GC 가동 중 처리량 저하 |
| Tail Latency 급등 | P99/P99.9 구간에서 수백ms~수초 스파이크 |

### 1.2 GC Latency 영향 계층 구조

```
Application Layer     ← 결제 트랜잭션 Timeout, TPS 감소
       │
JVM Runtime Layer     ← GC Pause (STW), Allocation Stall, TTSP
       │
Linux Kernel Layer    ← Page Fault, THP Defrag, OOM Killer, mmap
       │
Memory Subsystem      ← Virtual Memory, Physical Page 매핑
       │
Hardware              ← TLB Miss, NUMA Remote Access, Cache Miss
```

---

## 2. 하부 인프라 및 Linux Memory Subsystem과의 연동 기전

### 2.1 Virtual Memory 매핑과 Page Fault 오버헤드

JVM은 힙 메모리 확보 시 `mmap(2)` 시스템 콜로 가상 주소 공간만 예약한다. 실제 물리 메모리 페이지는 해당 주소에 **최초 쓰기 연산이 발생하는 순간** Page Fault 인터럽트를 통해 비로소 할당된다.

```
JVM Heap 확장 시
       │
mmap() → 가상 주소 공간 예약 (물리 메모리 할당 없음)
       │
GC 수행 중 미매핑 가상 주소 영역 접근
       │
Minor / Major Page Fault 대량 동시 발생
       │
커널 모드 전환 + 가상 메모리 테이블 갱신 블로킹
       │
GC 스레드 연산 속도 극단적 저하
       │
예측 불가능한 STW 연장 장애
```

| Page Fault 종류 | 발생 조건 | GC 영향 |
|----------------|---------|---------|
| Minor Page Fault | 물리 매핑만 필요 (페이지 존재) | 수십µs 지연 |
| Major Page Fault | Disk에서 실제 로드 필요 | 수ms~수십ms 지연 |

**완전 방어 설정:**
```bash
# JVM 기동 시 Heap 전체를 물리 메모리에 선제 고착
# -XX:+AlwaysPreTouch: 기동 시점에 0-byte 전체 쓰기 → Page Fault 런타임 개입 여지 완전 소거
java -Xms16g -Xmx16g -XX:+AlwaysPreTouch ...
```

**진단:**

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 |
|------|--------|------------|--------------|
| 메모리 맵 (mmap) | App ↔ Kernel | 가상 주소에 파일/장치 직접 매핑, Page Fault 시 Kernel이 실제 로드 | `/proc/PID/maps`, `vmstat pgfault` |
| Page Fault | Kernel 능동 관리 | Minor: 물리 매핑만 필요 / Major: Disk 로드 필요 | `vmstat pgfault/pgmajfault`, `/proc/vmstat` |
| TLB | Hardware + Kernel | 가상↔물리 주소 변환 캐시, mmap 과다 시 Miss 급증 | `perf stat -e dTLB-load-misses` |

```bash
# Page Fault 발생 추이 실시간 확인
vmstat 1 | awk '{print $9, $10}'   # pgfault, pgmajfault 열

# GC 시점과의 상관관계 확인
cat /proc/vmstat | grep -E 'pgfault|pgmajfault'
```

### 2.2 Transparent Huge Pages (THP) — 동기적 블로킹 위험

THP는 4KB 기본 페이지 대신 2MB 단위 페이지를 사용하여 TLB 히트율을 높이지만, FinTech 환경에서는 **GC 수행 중 치명적인 블로킹 원인**이 된다.

```
GC 스레드: 메모리 할당 / 해제 / Compaction 수행
       │
Linux 커널 khugepaged 데몬: THP 동적 단편화 제거(Defrag) 개입
       │
커널이 메모리 페이지 재배치 + 락 획득 시도 (동기적 블로킹)
       │
JVM 전체 스레드(GC 스레드 포함) 커널 내부에서 Stall
       │
수십ms → 수초 단위 GC Latency 급격 악화
       │
결제 트랜잭션 대규모 Timeout 도미노
```

**FinTech 프로덕션 필수 설정:**
```bash
# 호스트 레벨 THP 완전 비활성화 (영구 적용)
echo never > /sys/kernel/mm/transparent_hugepage/enabled
echo never > /sys/kernel/mm/transparent_hugepage/defrag

# 영구 적용 (재부팅 후에도 유지)
# /etc/rc.local 또는 systemd 서비스에 추가
```

**진단:**
```bash
# 현재 THP 설정 확인
cat /sys/kernel/mm/transparent_hugepage/enabled
# [always] madvise never  → always 또는 madvise이면 위험

# THP 관련 통계
cat /proc/vmstat | grep -E 'thp_|huge'

# khugepaged 활동 확인
cat /sys/kernel/mm/transparent_hugepage/khugepaged/pages_collapsed
```

### 2.3 Cgroup Memory Limit과 OOM Killer

Kubernetes Pod 환경에서 `memory.max` 제어선에 JVM 전체 메모리가 도달하면 커널 OOM Killer가 가동된다.

```
JVM 총 메모리 사용량 = Java Heap + Off-Heap
                              │
               ┌──────────────┴──────────────┐
               │                             │
          Java Heap                      Off-Heap
          (-Xmx 설정)                   ┌───────────────┐
                                       │ Metaspace     │
                                       │ Thread Stack  │
                                       │ Code Cache    │
                                       │ Direct Buffer │
                                       │ Native Library│
                                       └───────────────┘
       │
총합이 Container memory.limit 초과
       │
Linux OOM Killer 가동
       │
JVM 프로세스에 SIGKILL (Exit Code 137) 즉시 투사
       │
힙 덤프 / 에러 로그 생성 불가 → 완전 단절형 장애
```

**Off-Heap 메모리 구성 및 권장 여유 마진:**

| 구성 요소 | 대략적 크기 | 설정 방법 |
|----------|-----------|---------|
| Java Heap | `-Xmx` 설정값 | `-Xms16g -Xmx16g` |
| Metaspace | 수백MB | `-XX:MaxMetaspaceSize=512m` |
| Thread Stack | 스레드 수 × 512KB~1MB | `-Xss512k` |
| Code Cache | 256MB~512MB | `-XX:ReservedCodeCacheSize=512m` |
| Direct Buffer | 애플리케이션 의존 | `-XX:MaxDirectMemorySize=1g` |

```bash
# Container Memory Limit 대비 JVM 실제 사용량 확인
cat /sys/fs/cgroup/memory.current          # 현재 사용량 (cgroup v2)
cat /sys/fs/cgroup/memory.max              # 제한값

# JVM Native Memory 상세 추적
jcmd <PID> VM.native_memory summary

# OOM Kill 이력 확인
dmesg | grep -E 'oom|killed'
journalctl -k | grep -i 'oom'
```

---

## 3. JVM GC별 Latency 유발 정밀 기전

### 3.1 G1 GC — Latency 임계점 분석

#### ① Remembered Set (RSet)과 Write Barrier 오버헤드

G1 GC는 힙을 독립된 Region으로 분할 관리한다. Region 간 참조 추적을 위해 **Remembered Set(RSet)** 구조를 유지하며, 참조 관계 변경 시마다 JIT 주입 **Write Barrier**가 카드 테이블을 오염(Dirty)시킨다.

```
객체 참조 관계 변경 (결제 메시지 파싱, 대규모 Map 업데이트)
       │
Write Barrier 가동 → Card Table Dirty 마킹
       │
JVM 백그라운드 스레드: Card Table → RSet 동기화
       │
참조 관계 고밀도 / 변경 빈도 높을수록
       │
Minor GC 내 Update RS + Scan RS 시간 급증
       │
STW 강제 연장 (GC 로그: Update RS 수십ms 이상 기록)
```

**GC 로그에서 RSet 병목 판독:**
```
GC(42) Update RS: 45.2ms    ← 이 수치가 전체 Pause의 50% 이상이면 RSet 병목
GC(42) Scan RS:  18.7ms
```

#### ② Evacuation Failure → Full GC 전락

```
결제 트래픽 스파이크
       │
Allocation Rate > GC 수거 속도 (Allocation Rate Violation)
       │
Young → Survivor / Old Generation 복사(Evacuation) 공간 완전 고갈
       │
Evacuation Failure (To-space Exhaustion) 발생
       │
G1 GC: 멀티스레드 동시성 모드 즉시 중단
       │
단일 스레드 Full GC 강제 회귀 (Mark-Sweep-Compact)
       │
힙 전체(예: 16GB~32GB)를 단 하나의 스레드가 순차 정제
       │
수초~수십초 완전 STW → 실시간 결제 승인 네트워크 완전 파괴
```

**방어 설정:**
```bash
# Heap 여유 공간 최소 40% 유지
-XX:G1ReservePercent=20          # 예비 공간 확보
-XX:G1HeapWastePercent=5         # 낭비 허용 임계치

# Humongous Allocation 방지 (Region 크기의 50% 이상 단일 객체)
-XX:G1HeapRegionSize=32m         # Region 크기 조정

# GC 로그에서 Evacuation Failure 감지
grep "Evacuation Failure\|To-space exhausted" /var/log/gc.log
```

### 3.2 ZGC — Latency 임계점 및 극한 장애

#### ① Colored Pointers + Load Barrier 구조

ZGC는 STW를 1ms 미만으로 고착화하기 위해 두 가지 핵심 기술을 사용한다.

| 기술 | 동작 방식 | 목적 |
|------|---------|------|
| Colored Pointers | 64비트 가상 주소 상위 비트를 마킹 플래그로 활용 | 객체 상태를 포인터에 직접 인코딩 |
| Load Barrier | 객체 참조 시 CPU 레지스터 레벨에서 포인터 주소 동적 검증/갱신 | STW 없이 Concurrent Relocation 가능 |

```
GC 스레드 (백그라운드 Concurrent 작업)
    ├── Concurrent Mark
    ├── Concurrent Relocate      ← 객체 이동 (STW 없음)
    └── Concurrent Remap

애플리케이션 스레드 (동시 실행)
    └── 객체 참조 시 Load Barrier 실행
            → Colored Pointer 확인
            → 구 주소이면 새 주소로 자동 갱신
```

#### ② Allocation Spike → Allocation Stall (지표의 역설)

```
결제 트래픽 급증 → 단시간 대량 객체 생성 (Allocation Spike)
       │
ZGC 백그라운드 수거 속도 < 객체 생성 속도
       │
ZGC 최후 수단 발동:
  ├── Page Cache Flush
  └── Block Allocating Application Threads (비즈니스 스레드 강제 홀딩)
       │
┌────────────────────────────────────┐
│         지표의 역설 현상             │
│  GC 로그 STW:    < 1ms (정상)      │
│  실제 결제 Latency: 수백ms (비정상) │
└────────────────────────────────────┘
       │
GC Pause 지표는 깨끗하나 Timeout 경보 발령
→ GC 로그만 보면 원인 식별 불가
→ Application-level Latency 지표 병행 확인 필수
```

**진단:**
```bash
# ZGC Allocation Stall 이벤트 확인
grep "Allocation Stall\|Stall" /var/log/gc.log

# ZGC 통계 덤프
jcmd <PID> GC.heap_info

# JFR에서 Allocation Stall 이벤트 추출
jfr print --events jdk.ZAllocationStall latency_incident.jfr
```

---

## 4. Safepoint와 TTSP 미시 분석

### 4.1 Safepoint 구조

GC가 객체 메모리 주소를 안전하게 변경하려면, 모든 애플리케이션 스레드가 **객체 레퍼런스를 변경할 수 없는 일관된 지점(Safepoint)**에 도달해야 한다.

```
JVM: 글로벌 Safepoint 플래그 활성화
       │
모든 스레드: 주기적으로 인라이닝된 Polling Page 코드 확인
       │
Safepoint 도달 시: 스레드 정지 (연산 불가 상태)
       │
모든 스레드 정지 완료 → GC 수행 시작
       │
GC 완료 → 모든 스레드 재개
```

### 4.2 TTSP (Time To Safepoint) 오버헤드

**TTSP** = Safepoint 플래그 활성화 ~ 마지막 스레드가 Safepoint 도달까지의 시간

```
GC 수행 결정 → Safepoint 플래그 ON
       │
스레드 A: 즉시 Safepoint 도달 ✅ (대기 시작)
스레드 B: 즉시 Safepoint 도달 ✅ (대기 시작)
       ⋮
스레드 Z: Counted Loop 실행 중
          └─ for (long i = 0; i < 10_000_000; i++) { ... }
             → JVM: Counted Loop 내부에 Safepoint Polling 미주입 (표준 명세)
             → 루프 완료까지 Safepoint 진입 불가 (예: 300ms 소요)
       │
이미 멈춰 선 수백 개 스레드: 스레드 Z가 루프 마칠 때까지 전원 대기
       │
┌──────────────────────────────────────┐
│  실제 GC 마킹 연산:          2ms     │
│  TTSP (스레드 Z 루프 대기): 300ms    │
│  총 Application Stalled Time: 302ms  │
└──────────────────────────────────────┘
```

**핀테크 고위험 TTSP 유발 코드 패턴:**
- 대규모 금융 정산 루프 (`for (int i = 0; i < records.length; i++)`)
- 대용량 암호화 복호화 반복 연산
- 인덱스 기반 바운디드 루프의 장시간 처리

**필수 방어 설정:**
```bash
# 모든 Counted Loop에 강제 Safepoint Polling 주입
-XX:+UseCountedLoopSafepoints

# Safepoint 도달 지연 임계치 감시 (100ms 초과 시 경고 로그 출력)
-XX:SafepointTimeoutDelay=100
-XX:+SafepointTimeout

# GC 로그에서 TTSP 확인
# "Reaching: Xms" 항목이 전체 Pause의 상당 비율이면 TTSP 문제
grep "Reaching:" /var/log/gc.log
```

---

## 5. GC 기술 진화 및 핀테크 채택 현황 (2026)

### 5.1 과거 세대 → 현재 표준 진화 경로

| GC | 시대 | 특징 | FinTech 현황 |
|----|------|------|------------|
| Parallel GC | JDK 7~8 | 높은 Throughput, 힙 비례 수초 STW | ❌ 전면 퇴출 |
| CMS GC | JDK 7~8 | Concurrent Mark 지원, Compaction 부재 → 메모리 단편화 누적 → Concurrent Mode Failure | ❌ JDK 14 제거 |
| G1 GC | JDK 9~현재 | Region 기반, 예측 가능한 STW 목표(`-XX:MaxGCPauseMillis`) | ✅ 표준 MSA 인프라 기본값 |
| Generational ZGC | JDK 21~현재 | Young/Old 세대 분할 + Concurrent GC, STW < 1ms | ✅ 핀테크 코어 승인 시스템 절대 표준 |

### 5.2 G1 GC — 현재 표준 운영 사양

대다수 금융권 MSA 인프라의 기본 GC로 안착. 단, 아래 조건을 충족하는 엄격한 튜닝 하에서만 실전 가용된다.

```bash
-XX:+UseG1GC
-XX:MaxGCPauseMillis=50          # STW 목표: 50ms
-XX:G1HeapRegionSize=16m         # Region 크기 (Humongous Allocation 방지)
-XX:G1ReservePercent=20          # 예비 공간 20% 확보
-XX:InitiatingHeapOccupancyPercent=35  # Old Gen 35% 도달 시 Concurrent Mark 시작
```

운영 중 유지해야 할 **최소 힙 여유 공간: 40% 이상**

### 5.3 Generational ZGC — 2026년 핀테크 코어 표준

JDK 21 정식 도입 후 2026년 현재 안정성 완전 검증 완료. 기존 ZGC의 단점이었던 Allocation Spike 취약성과 높은 CPU 소모율을 세대 분할로 획기적으로 개선했다.

```
Generational ZGC 아키텍처:

Young Generation (수명 짧은 결제 컨텍스트 객체)
    └── 고속 Concurrent 수거 (빈번한 Minor Collection)
           │
Old Generation (장기 생존 객체)
    └── Major Collection (더 낮은 빈도)

핵심 개선:
  단일 세대 ZGC: Allocation Spike 시 전체 힙 압박
  Generational ZGC: Young Gen 빠른 분리 수거 → Allocation Stall 리스크 획기적 감소
```

**핀테크 가용성 보장 수치:**
- 힙 크기 16GB~128GB에서 STW < 1ms 영구 유지
- P99.99 Tail Latency < 5ms (결제 게이트웨이 코어 기준)
- 초당 수천 건 트랜잭션 처리 환경에서 검증 완료

---

## 6. SRE 관점 모니터링 지표

### 6.1 핵심 알람 임계치 매트릭스

| 지표 (Prometheus Metric) | 관측 대상 | 알람 기준선 |
|------------------------|---------|-----------|
| `jvm_gc_pause_seconds_max` | 단일 GC 이벤트 최대 STW | **Critical: > 200ms** |
| `jvm_gc_pause_seconds_sum` | 분당 누적 GC 점유 시간 | **Warning: 1분 누적 > 5s** |
| `safepoint_time_to_safepoint` | Safepoint 도달 대기 시간 | **Critical: > 100ms** |
| `jvm_memory_used_bytes{area="heap"}` | Heap 사용량 | **Warning: > 80%, Critical: > 90%** |
| `jvm_memory_committed_bytes` | Heap 예약량 | Xmx와 비교 |
| `container_memory_usage_bytes` | Container 전체 메모리 | **Critical: limit의 90% 초과** |
| `process_resident_memory_bytes` | JVM RSS | Off-Heap 누수 감시 |

### 6.2 GC 유형별 추가 지표

| 지표 | 관측 대상 | 비고 |
|------|---------|------|
| `jvm_gc_cpu_time_seconds_total` | GC 스레드 CPU 점유 | ZGC Allocation Spike 감지 |
| GC 로그 `Update RS` 시간 | RSet 갱신 오버헤드 | G1 GC 전용 |
| GC 로그 `Allocation Stall` | ZGC Allocation 블로킹 | ZGC 전용 |
| `vmstat pgmajfault` | Major Page Fault 발생 수 | AlwaysPreTouch 미적용 감지 |
| `dmesg oom` | OOM Killer 발동 | Exit Code 137 연계 |

### 6.3 GC Latency와 Application Latency 병행 관측 (필수)

```
ZGC Allocation Stall 장애 시:

  GC Pause 지표 (jvm_gc_pause_seconds_max): < 1ms  ← 정상처럼 보임
  Application P99 Latency (http_server_requests):  > 500ms ← 비정상

→ GC 지표만 단독 관측 시 원인 식별 불가
→ GC 로그 + Application Latency + Allocation Rate를 반드시 3종 연계 분석
```

---

## 7. GC Latency 진단 절차 및 트러블슈팅 런북

### 7.1 SRE 실시간 진단 파이프라인

```
[ALERT: GC Latency 급증 / API Timeout 증가]
         │
         ▼
Step 1: GC Pause 수치 확인
  jvm_gc_pause_seconds_max
         │
  ├─ Pause 명백히 높음 (> 200ms)
  │       │
  │       ▼
  │  Step 2: GC 로그 상세 파싱
  │    -Xlog:gc*,safepoint=info
  │       │
  │  ├─ Evacuation Failure / Full GC 기록
  │  │       └─► [G1 Heap 고갈 / Allocation Rate 과다]
  │  │              → Heap 증설 / G1ReservePercent 상향 / Scale-Out
  │  │
  │  ├─ Update RS / Scan RS 수치 비정상 높음
  │  │       └─► [RSet 오버헤드 / 객체 참조 고밀도]
  │  │              → G1HeapRegionSize 조정 / 객체 설계 개선
  │  │
  │  └─ Safepoint Reaching 시간 높음 (TTSP)
  │          └─► [Counted Loop 미종료 스레드]
  │                 → UseCountedLoopSafepoints 적용 / JFR 스레드 추적
  │
  └─ Pause 정상 (< 1ms), 그러나 API Latency 높음
           │
           ▼
  Step 3: ZGC Allocation Stall 확인
    grep "Allocation Stall" /var/log/gc.log
           │
  ├─ Stall 이벤트 존재
  │       └─► [ZGC Allocation Spike]
  │              → Generational ZGC 전환 / ZAllocationSpikeTolerance 상향 / Scale-Out
  │
  └─ Stall 없음
           │
           ▼
  Step 4: 커널 레벨 원인 확인
    ├─ vmstat pgmajfault 급증 → [Page Fault / AlwaysPreTouch 미적용]
    ├─ THP enabled 상태 → [THP Defrag 블로킹]
    └─ dmesg OOM → [Off-Heap 누수 / Container Limit 초과]
```

### 7.2 원인 유형별 진단 명령어

#### JFR — TTSP 격리 분석 (상시 탑재 권장, 오버헤드 < 1%)

```bash
# 프로덕션 JVM에서 최근 10분간 JFR 이벤트 동적 추출
jcmd <PID> JFR.dump name=global_monitoring filename=/var/log/jvm_dumps/latency_incident.jfr

# TTSP 원인 스레드 분석 (JDK Mission Control에서 분석)
# - Safepoint Begin / Safepoint Wait Blocked 이벤트 정렬
# - Counted Loop 구동 스레드를 소스코드 라인 단위로 특정

# ZGC Allocation Stall 이벤트 추출
jfr print --events jdk.ZAllocationStall /var/log/jvm_dumps/latency_incident.jfr

# GC 이벤트 전체 추출
jfr print --events jdk.GarbageCollection /var/log/jvm_dumps/latency_incident.jfr
```

#### GC 로그 파싱 — G1 GC Evacuation 지연 판독

```
[2026-05-28T14:30:22.123+0900][info][gc,start] GC(42) Garbage Collection (G1 Evacuation Pause) (young)
[2026-05-28T14:30:22.456+0900][info][gc,phases] GC(42) Pre Evacuate Collection Set:  0.2ms
[2026-05-28T14:30:22.789+0900][info][gc,phases] GC(42) Evacuate Collection Set:    320.5ms  ← ★ 병목
[2026-05-28T14:30:22.810+0900][info][gc,phases] GC(42) Post Evacuate Collection Set: 12.3ms
[2026-05-28T14:30:22.822+0900][info][gc       ] GC(42) Pause Young 4096M->2048M(8192M) 699.2ms
[2026-05-28T14:30:22.825+0900][info][safepoint] Safepoint "G1IncCollectionPause", Time: 701.5ms, Reaching: 2.3ms

판독 알고리즘:
  Evacuate Collection Set 시간이 전체 Pause의 대부분을 차지
  + 동 시점 vmstat pgmajfault 급증
  → "Page Fault / THP Defrag 간섭으로 인한 GC 스레드 Stall" 확정 판정
```

#### 커널 레벨 확인

```bash
# Page Fault 추이
vmstat 1 | awk '{print strftime("%H:%M:%S"), $9, $10}'

# THP 상태 확인
cat /sys/kernel/mm/transparent_hugepage/enabled

# OOM Kill 이력
dmesg | grep -E 'oom_kill|Out of memory' | tail -20

# JVM Off-Heap 상세 분석
jcmd <PID> VM.native_memory summary scale=MB

# Container 메모리 사용량 vs 한계
cat /sys/fs/cgroup/memory.current
cat /sys/fs/cgroup/memory.max
```

### 7.3 장애 복구 조치 (Remediation)

#### 1단계 — 트래픽 격리 (Mitigation)

```bash
# Readiness Probe 실패 전 선제 격리
# Kubernetes: 대상 인스턴스 트래픽 가중치 0으로 조정
kubectl patch svc <payment-service> -p '{"spec":{"selector":{"version":"stable"}}}'

# Pod 즉시 격리
kubectl label pod <pod-name> app=payment-isolated --overwrite
```

#### 2단계 — Scale-Out으로 Allocation Rate 압박 분산

```bash
kubectl scale deployment <payment-service> --replicas=<N>

# HPA 즉시 발동
kubectl patch hpa <payment-hpa> -p '{"spec":{"minReplicas":<N>}}'
```

#### 3단계 — 영구 플래그 수정 배포

```bash
# 커널 파라미터 영구 적용
echo never > /sys/kernel/mm/transparent_hugepage/enabled

# JVM 플래그 재배포 (다음 섹션 Manifest 참조)
# 핵심: AlwaysPreTouch + ZGenerational + UseCountedLoopSafepoints
```

---

## 8. 2026 Production Baseline — 핀테크 특화 JVM 기동 Manifest

2026년 현재 초저지연 결제 승인 시스템에 강제 고착화하여 배포하는 SRE 검증 완료 기동 명세다.

```bash
java \
  # ── 물리 메모리 선점 ──────────────────────────────────────────────
  -Xms16g -Xmx16g \
  -XX:+AlwaysPreTouch \
  # 기동 시 Heap 전체에 0-byte 쓰기 → 물리 페이지 100% 선점
  # 런타임 Page Fault 개입 여지 완전 소거

  # ── 2026년 핵심: Generational ZGC ─────────────────────────────────
  -XX:+UseZGC \
  -XX:+ZGenerational \
  # Young/Old 세대 분할 → 단기 결제 객체 고속 분리 수거
  # Allocation Stall 리스크 획기적 감소
  -XX:ZAllocationSpikeTolerance=5 \
  # 기본값(2)보다 높게 설정 → Allocation Spike 허용 범위 확대

  # ── TTSP 연장 방어 ─────────────────────────────────────────────────
  -XX:+UseCountedLoopSafepoints \
  # 모든 Counted Loop에 Safepoint Polling 강제 주입
  -XX:SafepointTimeoutDelay=100 \
  -XX:+SafepointTimeout \
  # 100ms 초과 TTSP 발생 시 즉시 경고 로그

  # ── Off-Heap 메모리 상한 고착 ─────────────────────────────────────
  -XX:MetaspaceSize=512m \
  -XX:MaxMetaspaceSize=512m \
  # Metaspace 가변 폭증 → Container OOM 방어

  # ── 관측성: JFR 상시 탑재 (오버헤드 < 1%) ──────────────────────────
  -XX:+FlightRecorder \
  -XX:StartFlightRecording=disk=true,dumponexit=true,maxage=1d,settings=profile \
  -XX:+UnlockDiagnosticVMOptions \
  -XX:+NativeMemoryTracking=summary \

  # ── GC / Safepoint 통합 로그 ─────────────────────────────────────
  -Xlog:gc*,safepoint=info:file=/var/log/jvm_dumps/gc_latency.log:time,uptime,pid:filecount=10,filesize=100m \

  -jar payment-gateway-core.jar
```

### 8.1 핵심 옵션 공학적 조항 요약

| 옵션 | 목적 | 방어하는 장애 |
|------|------|------------|
| `-XX:+AlwaysPreTouch` | 기동 시 물리 페이지 전체 선점 | 런타임 Page Fault → GC STW 연장 |
| `-XX:+ZGenerational` | Young/Old 세대 분할 수거 | Allocation Spike → Allocation Stall |
| `-XX:ZAllocationSpikeTolerance=5` | Spike 허용 범위 확대 | ZGC 조기 Stall 발동 |
| `-XX:+UseCountedLoopSafepoints` | 모든 루프에 Polling 주입 | Counted Loop로 인한 TTSP 지연 |
| `-XX:MaxMetaspaceSize=512m` | Metaspace 상한 고착 | Off-Heap 폭증 → Container OOM |
| `-XX:StartFlightRecording=...` | JFR 상시 탑재 | 장애 사후 원인 추적 불가 |

### 8.2 호스트 OS 필수 설정 체크리스트

```bash
# 1. THP 완전 비활성화 (필수)
echo never > /sys/kernel/mm/transparent_hugepage/enabled
echo never > /sys/kernel/mm/transparent_hugepage/defrag

# 2. 스왑 비활성화 (결제 서버 필수)
swapoff -a

# 3. vm.swappiness 최소화
sysctl -w vm.swappiness=1

# 4. 메모리 overcommit 제어
sysctl -w vm.overcommit_memory=1  # Always commit (JVM AlwaysPreTouch와 연계)
```

---

## 참고: GC 원인 판별 요약 흐름도

```
GC Latency 급증 / API Timeout 발생
         │
         ├─ STW 명백히 높음 (> 200ms)
         │       │
         │       ├─ Evacuation Failure ──► G1 Heap 고갈 → Heap 증설 / Scale-Out
         │       ├─ Update RS 높음 ──────► RSet 오버헤드 → RegionSize 조정
         │       ├─ TTSP 높음─────────────► Counted Loop → UseCountedLoopSafepoints
         │       └─ pgmajfault 급증 ──────► Page Fault → AlwaysPreTouch 적용
         │
         ├─ STW 정상 (< 1ms) + API Latency 높음
         │       └─ Allocation Stall ────► ZGC Spike → ZGenerational 전환
         │
         └─ STW 정상 + OOM Kill (Exit 137)
                 └─ Off-Heap 누수 ────────► Container Limit / NativeMemoryTracking 분석
```

---

## SRE 일일 체크리스트

```
[ ] jvm_gc_pause_seconds_max < 200ms
[ ] 분당 GC 누적 시간 < 5s
[ ] TTSP < 100ms
[ ] Heap 사용률 < 80%
[ ] Off-Heap 총합 + Heap < Container Memory Limit × 0.9
[ ] THP: /sys/kernel/mm/transparent_hugepage/enabled = never
[ ] AlwaysPreTouch 적용 여부 확인
[ ] ZGenerational 모드 활성화 확인
[ ] UseCountedLoopSafepoints 적용 여부 확인
[ ] JFR 상시 가동 확인 (jcmd <PID> JFR.check)
[ ] GC 로그 파일 로테이션 정상 동작 확인
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*