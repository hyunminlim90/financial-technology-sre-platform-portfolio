# 물리 (Physical)
## FinTech 결제 시스템 SRE 관점 — Java E2E Foundations

> 정독: 0회

## 목차

1. [Stack Context 식별](#1-stack-context-식별)
2. [E2E 계층별 물리 자원 흐름](#2-e2e-계층별-물리-자원-흐름)
3. [Physical Resource Flow — 핵심 메커니즘](#3-physical-resource-flow--핵심-메커니즘)
4. [Resource Flow Correlation](#4-resource-flow-correlation)
5. [Top-Down 분석: 증상 → 물리 원인 추적](#5-top-down-분석-증상--물리-원인-추적)
6. [Bottom-Up 분석: 물리 특성 → Java 성능 영향](#6-bottom-up-분석-물리-특성--java-성능-영향)
7. [장애 및 Saturation 패턴](#7-장애-및-saturation-패턴)
8. [Linux / JVM / Kubernetes 실무 관측](#8-linux--jvm--kubernetes-실무-관측)

---

## 1. Stack Context 식별

### 물리(Physical)가 연결되는 스택 계층

| 스택 | 물리 연결 계층 | 주요 연결 메커니즘 |
|------|-------------|-----------------|
| Java / JVM | Runtime | Heap → DRAM, GC → CPU/Cache, TLAB → Cache Line |
| Spring Boot / WebFlux | Framework | Thread Pool → CPU Core, Netty Event Loop → NIC/CPU |
| Netty | Middleware | DirectMemory → DRAM, DMA → NIC Buffer |
| Kafka | Middleware | Page Cache → DRAM/NVMe, Flush → NVMe IOPS |
| MySQL | Storage | InnoDB Buffer Pool → DRAM, WAL fsync → NVMe |
| Redis | Middleware | jemalloc → DRAM, Persistence → NVMe |
| Kubernetes | Platform | cgroup → Physical CPU Core, Node → Physical Server |

### 물리가 가장 강하게 연결되는 실행 흐름

```
결제 HTTP 요청
→ NIC (Physical: PHY/MAC)
→ DMA (Physical: PCIe Bus)
→ Kernel sk_buff (Physical: DRAM)
→ Netty Event Loop (Physical: CPU Core + L1/L2 Cache)
→ Spring WebFlux Handler (Physical: CPU + Heap DRAM)
→ JPA/MySQL I/O (Physical: NVMe + DRAM Buffer Pool)
→ Kafka Produce (Physical: Page Cache → NVMe)
→ 응답 반환 (Physical: NIC → Network)
```

---

## 2. E2E 계층별 물리 자원 흐름

### 2-1. Physical / Hardware Layer

결제 요청이 처음 닿는 물리 계층이다.

```
[클라이언트]
    │ 광케이블 / 이더넷
    ▼
[NIC: Physical PHY + MAC]
    │ PCIe Bus (DMA)
    ▼
[DRAM: NIC Ring Buffer / sk_buff]
    │ PCIe → CPU
    ▼
[CPU Core: ALU + L1/L2/L3 Cache]
    │
    ▼
[NVMe SSD: NAND Flash + Controller]
```

**물리 자원별 역할:**

| 물리 자원 | FinTech 결제에서의 역할 |
|----------|----------------------|
| CPU Core | 승인 로직, 암호화(TLS/HMAC), GC, JIT |
| L1/L2 Cache | JIT 컴파일 코드, 핫 객체, Stack Frame |
| L3 Cache | InnoDB Buffer Pool 일부, Heap 객체 |
| DRAM | JVM Heap, Off-Heap DirectMemory, Page Cache |
| NVMe | WAL(Redo Log), InnoDB Data File, Kafka Log Segment |
| NIC | 결제 패킷 수신/송신, TLS Handshake |
| PCIe Bus | DMA 전송 (NIC↔DRAM, NVMe↔DRAM) |

### 2-2. OS Kernel Layer

물리 자원을 추상화하여 Java 프로세스에 제공한다.

- **CPU Scheduler (CFS):** Physical Core를 Java Thread에 할당
- **Memory Manager:** Physical DRAM Page를 JVM Virtual Address에 매핑
- **Block I/O (blk-mq):** NVMe Controller로 I/O Queue 전달
- **Network Stack:** NIC DMA 수신 → `sk_buff` → Socket Buffer → JVM

```
Physical DRAM Page
    → Virtual Memory (Page Table / TLB)
    → JVM Heap Address Space
    → Object Allocation (TLAB)
```

### 2-3. JVM Runtime Layer

물리 자원을 JVM이 어떻게 소비하는가:

| JVM 구성 | 물리 자원 매핑 |
|---------|-------------|
| JVM Heap | DRAM (Young Gen + Old Gen) |
| TLAB | L1/L2 Cache 친화적 연속 할당 |
| JIT (C1/C2) | CPU Core 점유, Code Cache → L2/L3 |
| GC (G1/ZGC) | CPU Core(GC Thread) + DRAM Bandwidth |
| DirectMemory | DRAM (Off-Heap, GC 외부) |
| Stack Frame | CPU Register + L1 Cache |

### 2-4. Framework / Middleware Layer

**Netty (Physical 관점):**

```
NIC Interrupt → SoftIRQ → Kernel → Netty Event Loop Thread
    → CPU Core 고정(affinity 가능)
    → DirectByteBuffer (Off-Heap DRAM)
    → Zero-Copy (DMA → DirectMemory, CPU Copy 최소화)
```

**Spring WebFlux:**
- Reactive Pipeline은 Event Loop Thread 위에서 동작
- CPU Core 수 = Event Loop 수 (물리 코어와 1:1 권장)
- Blocking 발생 시 → Physical Core 낭비

**MySQL/InnoDB (Physical 관점):**

```
InnoDB Buffer Pool → DRAM (물리 메모리 직접 점유)
Redo Log fsync    → NVMe (동기 I/O, IOPS 직접 의존)
Double Write Buffer → NVMe Sequential Write
```

### 2-5. Application / Business Layer

결제 승인 로직은 물리 계층에서 다음과 같이 소비된다:

| 결제 처리 단계 | 물리 자원 소비 |
|-------------|-------------|
| TLS Handshake | CPU (RSA/ECDHE 연산) + L2/L3 Cache |
| JWT 검증 | CPU (HMAC-SHA256) + L1 Cache |
| 결제 요청 파싱 (JSON) | CPU (Deserialization) + Heap DRAM |
| DB 조회 (잔액 확인) | DRAM (Buffer Pool) or NVMe (Cache Miss) |
| 승인 로직 | CPU (Branch + ALU) + L1/L2 Cache |
| Kafka Produce | DRAM (Page Cache) → NVMe Flush |
| 응답 직렬화 | CPU (Serialization) + DirectMemory |

---

## 3. Physical Resource Flow — 핵심 메커니즘

### 3-1. Cache Line & DRAM Bandwidth

JVM에서 객체 접근은 결국 물리 Cache Line(64byte) 단위로 발생한다.

```
Java Object 접근
    → CPU가 Virtual Address → Physical Address 변환 (TLB)
    → L1 Cache Hit?  → 즉시 반환 (~1ns)
    → L2 Cache Hit?  → ~4ns
    → L3 Cache Hit?  → ~40ns
    → DRAM Access    → ~100ns (10~40배 느림)
    → NVMe Access    → ~100μs (100,000배 느림)
```

**FinTech 영향:** GC 이후 Heap 객체 재배치 → Cache Miss 증가 → Throughput 저하

### 3-2. NUMA (Non-Uniform Memory Access)

멀티소켓 서버에서 JVM Heap이 NUMA를 무시하면:

```
CPU Socket 0 (JVM Thread)
    → NUMA Local DRAM 접근: ~100ns
    → NUMA Remote DRAM (Socket 1) 접근: ~200ns (2배)
```

**실제 문제:** Kubernetes Node가 멀티소켓 서버이고 JVM이 NUMA-unaware로 실행 시, GC Thread가 Remote NUMA 접근하여 STW 연장

### 3-3. PCIe Bus & DMA

Netty의 Zero-Copy는 물리 PCIe + DMA 경로를 활용한다:

```
일반 경로 (Copy 발생):
NIC → DRAM (Kernel) → CPU Copy → DRAM (User Space) → CPU Copy → JVM Heap

Zero-Copy 경로 (DirectMemory):
NIC → DMA → DRAM (DirectByteBuffer) → NIC 송신
CPU Copy 없음
```

**PCIe Bottleneck:** NIC가 PCIe x16이지만 x8로 동작 중이면 → NIC Throughput 절반

### 3-4. NVMe Physical I/O

MySQL WAL fsync의 물리 경로:

```
InnoDB redo log write()
    → Page Cache (DRAM 버퍼)
    → fsync() 호출
    → blk-mq Queue → NVMe Controller
    → NAND Flash 기록
    → 완료 인터럽트 → fsync() 반환
```

**IOPS 한계:** NVMe SSD는 최대 수십만 IOPS. 결제 트래픽 폭증 시 fsync queue 적체 → Commit Latency 증가 → API P99 증가

### 3-5. CPU Thermal Throttling

결제 트래픽 피크 시:

```
CPU 부하 증가
    → 발열 증가
    → TjMax 근접 (보통 100°C)
    → Intel P-state / AMD Cool'n'Quiet 개입
    → Clock 자동 하향 (예: 3.8GHz → 2.5GHz)
    → IPC 동일, 주파수 감소 → 실질 연산량 감소
    → TPS 감소 + P99 증가
```

### 3-6. JVM GC & Physical Memory Bandwidth

G1GC / ZGC의 물리 메모리 관점:

```
Young GC (Minor GC):
    Eden → Survivor → Old 객체 복사
    = DRAM 읽기 + 쓰기 (Memory Bandwidth 소비)

ZGC Load Barrier:
    모든 Object 참조 시 barrier 실행
    = 추가 CPU 명령 + Cache 오염 가능

STW (Stop-The-World):
    GC Thread가 Physical Core 점유
    = Application Thread 정지
    = 결제 처리 일시 중단
```

---

## 4. Resource Flow Correlation

### 물리 자원 간 상호 의존성

```
결제 TPS 증가
    │
    ├─► CPU 부하 증가
    │       │
    │       ├─► Cache Miss 증가 (Heap 증가 시)
    │       ├─► Branch Misprediction (복잡한 승인 로직)
    │       └─► Thermal Throttling (지속 고부하)
    │
    ├─► DRAM 사용 증가
    │       │
    │       ├─► GC 빈도 증가
    │       ├─► Page Cache 감소 (MySQL Buffer Pool 경합)
    │       └─► NUMA Remote Access 증가
    │
    ├─► NVMe I/O 증가
    │       │
    │       ├─► fsync Latency 증가
    │       ├─► SSD Write Amplification
    │       └─► WAL Queue 적체
    │
    └─► NIC 부하 증가
            │
            ├─► SoftIRQ 증가 (특정 Core 집중)
            ├─► Ring Buffer Drop (패킷 손실)
            └─► DMA 경쟁 (PCIe Bandwidth)
```

### 자원 트레이드오프

| 트레이드오프 | 절약 자원 | 추가 소비 자원 |
|------------|---------|-------------|
| DirectMemory (Off-Heap) 사용 | GC CPU/시간 절약 | DRAM (GC 외부 관리) |
| CPU Pinning (NUMA aware) | NUMA Remote Latency | CPU 유연성 감소 |
| Large Heap | GC 빈도 감소 | DRAM 점유 증가, Page Cache 감소 |
| NVMe RAID | I/O Throughput 증가 | PCIe Lane 추가 소비 |
| ZGC | STW 감소 | CPU Overhead 증가 (Barrier) |

---

## 5. Top-Down 분석: 증상 → 물리 원인 추적

### 증상 1: 결제 API P99 급증

```
증상: P99 Latency 300ms → 2000ms
    │
    ├─► GC 로그 확인 → STW 없음
    ├─► CPU 사용률 확인 → 85%
    ├─► perf stat → instructions/cycle 감소
    │       └─► Cache Miss 증가 확인
    │               └─► 물리 원인: Heap 증가로 L3 Cache 초과
    │                           DRAM Bandwidth 포화
    │
    └─► sensors 확인 → CPU 온도 92°C
            └─► 물리 원인: Thermal Throttling
                        클럭 3.6GHz → 2.8GHz 하향
```

### 증상 2: 결제 Timeout 발생 (DB 연관)

```
증상: MySQL Commit Timeout 증가
    │
    ├─► iostat 확인 → NVMe util 95%
    │       └─► blk-mq queue depth 증가 확인
    │               └─► 물리 원인: NVMe IOPS 한계 도달
    │                           Write Queue 적체
    │
    └─► InnoDB fsync latency 증가
            └─► 물리 원인: NAND Flash Write Latency
                        (SSD 내부 GC / Wear Leveling 발생)
```

### 증상 3: Network Packet Loss

```
증상: TLS Handshake 실패 증가
    │
    ├─► netstat -s → RcvbufErrors 증가
    ├─► cat /proc/interrupts → 특정 CPU Core에 NIC IRQ 집중
    │       └─► 물리 원인: IRQ Affinity 미설정
    │                   SoftIRQ가 단일 Core 점유
    │
    └─► ethtool -S eth0 → rx_missed_errors 증가
            └─► 물리 원인: NIC Ring Buffer Full
                        DMA 처리 지연
```

---

## 6. Bottom-Up 분석: 물리 특성 → Java 성능 영향

### 물리 특성이 JVM에 미치는 영향

**Cache Line 크기 (64 byte) → JVM Object Layout:**

```java
// False Sharing 발생 예시 (물리적으로 같은 Cache Line)
class PaymentCounter {
    volatile long approvedCount;  // Cache Line 공유
    volatile long rejectedCount;  // 같은 Cache Line → 경합 발생
}

// 해결: @Contended (Cache Line 패딩)
@Contended
class PaymentCounter {
    volatile long approvedCount;  // 독립 Cache Line
    volatile long rejectedCount;  // 독립 Cache Line
}
```

**NUMA 토폴로지 → JVM Heap 할당:**

```bash
# NUMA 인식 JVM 실행
java -XX:+UseNUMA \
     -XX:+UseParallelGC \
     -Xmx32g \
     PaymentApplication
```

`-XX:+UseNUMA` 활성화 시 각 NUMA Node 로컬 메모리에서 TLAB 할당 → Remote Access 감소

**NVMe Latency 특성 → MySQL 튜닝:**

```ini
# innodb_flush_log_at_trx_commit=1 (기본값)
# = 매 Commit마다 fsync → NVMe IOPS 직접 소비

# NVMe 고성능 환경
innodb_flush_log_at_trx_commit = 2
# = 1초 단위 fsync → IOPS 절약 (내구성 일부 감소)
```

**Physical Core 수 → Thread Pool 설계:**

```
Physical Core 수: 32
Hyper-Threading: 2x = 64 logical

Netty Event Loop: 32 (Physical Core 수 기준)
Spring WebFlux Worker: 32
MySQL Connection Pool: 100 (I/O Wait 많음, Core 수 초과 가능)
```

---

## 7. 장애 및 Saturation 패턴

### 7-1. CPU Physical Saturation

**트리거:** 결제 트래픽 Burst + TLS 연산 집중

```
Physical Core 100% 점유
    → CFS Scheduler Run Queue 증가
    → Java Thread Context Switch 증가
    → JVM Safepoint 지연 (STW 연장)
    → 결제 승인 Latency P99 증가
    → Client Timeout → Retry Storm
    → Retry가 CPU 부하 추가 증가
    → Cascading Failure
```

**관측:**
```bash
mpstat -P ALL 1        # Core별 사용률
perf stat -a sleep 5   # IPC, Cache Miss
pidstat -u 1 -p <pid>  # Java 프로세스 CPU
```

### 7-2. DRAM Saturation (OOM / GC Storm)

**트리거:** Heap 급증 (대형 결제 Batch 처리, Memory Leak)

```
Heap 증가 → Young GC 빈도 증가
    → Old Gen 압박 → Full GC 발생
    → STW 수 초 발생
    → 결제 처리 완전 정지
    → OOM Killer 발생 → JVM Kill
    → Pod Restart → 결제 서비스 일시 불가
```

**관측:**
```bash
jstat -gc <pid> 1000      # GC 통계
jcmd <pid> GC.heap_info   # 현재 Heap 상태
dmesg | grep -i oom       # OOM Killer 기록
```

### 7-3. NVMe I/O Saturation

**트리거:** 결제 Commit 폭증 + Kafka Flush 동시 발생

```
NVMe Write Queue 적체
    → MySQL fsync 대기 시간 증가
    → InnoDB Commit Latency 증가
    → DB Connection Hold 시간 증가
    → HikariCP Connection Pool 고갈
    → 결제 요청 Connection 대기
    → API Timeout 증가
    → P99 Latency 급증
```

**관측:**
```bash
iostat -x 1              # NVMe util, await, w_await
cat /sys/block/nvme0n1/queue/scheduler  # I/O Scheduler
perf trace -e block:*    # Block I/O 이벤트
```

### 7-4. NIC / PCIe Saturation

**트리거:** 결제 트래픽 Micro-burst

```
NIC Ring Buffer 초과
    → Packet Drop 발생
    → TCP Retransmission 증가
    → TLS Handshake 재시도
    → 연결 지연 증가
    → 결제 요청 실패율 증가
```

**관측:**
```bash
ethtool -S eth0 | grep -i drop  # NIC Drop 카운터
cat /proc/net/softnet_stat       # SoftIRQ Drop
sar -n DEV 1                     # 네트워크 Throughput
```

### 7-5. Thermal Throttling

**트리거:** 데이터센터 냉각 이상 + CPU 지속 고부하

```
CPU 온도 → TjMax 근접
    → P-state 강제 하향
    → 클럭 감소 (예: 3.8GHz → 2.5GHz)
    → JIT 컴파일 속도 감소
    → 결제 처리 TPS 감소
    → SLA 위반 가능
```

**관측:**
```bash
sensors                          # CPU 온도
turbostat --interval 1           # 실시간 클럭/온도
dmesg | grep -i throttl          # 커널 Throttling 로그
cat /proc/cpuinfo | grep MHz     # 현재 동작 클럭
```

---

## 8. Linux / JVM / Kubernetes 실무 관측

### 8-1. Physical CPU

```bash
# 물리 Core / NUMA 구조 확인
lscpu
numactl --hardware

# Core별 사용률 (Steal Time 포함)
mpstat -P ALL 1

# 클럭 / 온도 실시간
turbostat --interval 1
sensors

# CPU 성능 카운터 (Cache Miss, IPC)
perf stat -a -e \
  cache-misses,cache-references,\
  instructions,cycles,\
  branch-misses \
  sleep 10
```

### 8-2. Physical Memory

```bash
# DRAM 장착 정보
dmidecode -t memory

# 현재 사용 현황
free -h
cat /proc/meminfo

# NUMA 메모리 분포
numastat -m

# Page Fault / Swap
vmstat 1
sar -B 1
```

### 8-3. NVMe / Disk

```bash
# NVMe 목록 및 상태
nvme list
nvme smart-log /dev/nvme0

# I/O 사용률 및 Latency
iostat -x 1

# blk-mq Queue 상태
cat /sys/block/nvme0n1/queue/nr_requests
ls /sys/block/nvme0n1/mq/
```

### 8-4. NIC / Network

```bash
# NIC 물리 정보
ethtool eth0
lspci | grep -i net

# Ring Buffer 크기
ethtool -g eth0

# 패킷 통계 (Drop 포함)
ethtool -S eth0
ip -s link

# SoftIRQ 통계
cat /proc/net/softnet_stat
cat /proc/interrupts | grep eth
```

### 8-5. JVM (Physical 연관)

```bash
# GC 통계 (물리 메모리 소비)
jstat -gc <pid> 1000
jstat -gcutil <pid> 1000

# Heap 덤프 분석
jcmd <pid> GC.heap_info
jmap -histo <pid>

# JFR 기록 (CPU + Memory + I/O)
jcmd <pid> JFR.start \
  duration=60s \
  filename=payment.jfr

# async-profiler (CPU Flamegraph)
./asprof -d 30 -f flamegraph.html <pid>
```

### 8-6. Kubernetes (물리 자원 관점)

```bash
# Node 물리 자원 사용률
kubectl top nodes
kubectl describe node <node-name>

# Pod 물리 자원 사용률
kubectl top pods -n payment

# OOMKilled / Eviction 확인
kubectl describe pod <pod> | grep -A5 "OOMKilled\|Eviction"

# cgroup 물리 자원 제한 확인
cat /sys/fs/cgroup/cpu/kubepods/.../cpu.cfs_quota_us
cat /sys/fs/cgroup/memory/kubepods/.../memory.limit_in_bytes

# PSI (Pressure Stall Information)
cat /proc/pressure/cpu
cat /proc/pressure/memory
cat /proc/pressure/io
```

### 8-7. eBPF 기반 물리 자원 추적

```bash
# Off-CPU 분석 (물리 Core 미사용 구간)
/usr/share/bcc/tools/offcputime -p <pid> 30

# Run Queue Latency
/usr/share/bcc/tools/runqlat

# Block I/O Latency (NVMe)
/usr/share/bcc/tools/biolatency

# Network 패킷 추적
/usr/share/bcc/tools/tcplife
/usr/share/bcc/tools/tcpdrop
```

---

## Summary: Physical 자원이 FinTech 결제 시스템에 미치는 핵심 축

| 물리 자원 | 결제 시스템 KPI 영향 | 대표 장애 패턴 |
|----------|-------------------|-------------|
| CPU Core / Clock | Latency, TPS | Thermal Throttling, Saturation |
| L1/L2/L3 Cache | P99 Latency | Cache Miss (GC 후 재배치) |
| DRAM Bandwidth | Throughput, GC 속도 | OOM, Memory Bandwidth 포화 |
| NUMA 토폴로지 | GC STW, Thread Latency | Remote NUMA Access 증가 |
| NVMe IOPS | DB Commit Latency | fsync Queue, Wear Leveling |
| NIC / PCIe | 네트워크 Latency | Ring Buffer Drop, IRQ Storm |
| Power / Thermal | 전체 시스템 안정성 | Throttling, Random Reboot |

> SRE는 애플리케이션 지표(P99, Error Rate)가 이상할 때, 반드시 **물리 계층(온도, IOPS, Cache Miss, NUMA)** 까지 추적해야 한다. 가상화 추상화 뒤에 있는 물리 한계가 결제 시스템의 실제 성능 천장이다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*