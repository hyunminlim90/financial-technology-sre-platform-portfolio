# Virtual (가상)
## FinTech 결제 시스템 SRE 관점 — Java E2E Foundations

> 정독: 0회

## 목차

1. [Stack Context 식별](#1-stack-context-식별)
2. [E2E 계층별 가상화 실행 흐름](#2-e2e-계층별-가상화-실행-흐름)
3. [Virtual Resource Flow — 핵심 메커니즘](#3-virtual-resource-flow--핵심-메커니즘)
4. [Resource Flow Correlation](#4-resource-flow-correlation)
5. [Top-Down 분석: 증상 → 가상화 원인 추적](#5-top-down-분석-증상--가상화-원인-추적)
6. [Bottom-Up 분석: 가상화 특성 → Java 성능 영향](#6-bottom-up-분석-가상화-특성--java-성능-영향)
7. [장애 및 Saturation 패턴](#7-장애-및-saturation-패턴)
8. [Linux / JVM / Kubernetes 실무 관측](#8-linux--jvm--kubernetes-실무-관측)

---

## 1. Stack Context 식별

### Virtual(가상)이 연결되는 스택 계층

| 스택 | 가상화 연결 계층 | 주요 연결 메커니즘 |
|------|--------------|-----------------|
| Java / JVM | Runtime | Virtual Memory → Physical DRAM, JIT Code Cache, TLAB |
| Spring Boot / WebFlux | Framework | Thread → vCPU 스케줄링, Virtual Heap 할당 |
| Netty | Middleware | DirectByteBuffer (Virtual Address → Off-Heap DRAM) |
| Kafka | Middleware | Page Cache (Virtual Memory → Physical Block I/O) |
| MySQL / InnoDB | Storage | Buffer Pool (Virtual Address Space → DRAM) |
| Redis | Middleware | jemalloc Virtual Address, Fork-on-Write (COW) |
| Kubernetes | Platform | Pod → cgroup → vCPU → Physical Core |

### 가상화가 가장 강하게 연결되는 실행 흐름

```
결제 HTTP 요청
→ Kubernetes Pod (가상: cgroup 격리)
→ JVM Process (가상: Virtual Address Space)
→ Netty Event Loop (가상: vCPU Thread 스케줄링)
→ Spring WebFlux Handler (가상: Heap Object 할당)
→ JPA → MySQL (가상: InnoDB Buffer Pool, Virtual Disk)
→ Kafka Produce (가상: Page Cache → Virtual Block Device)
→ 응답 반환 (가상: Virtual Network Interface)
```

### 핵심 자원 포커스

```
Virtual(가상) → CPU Scheduler + Memory Manager + Network + Storage
```

가상화는 단일 자원 문제가 아니라 **모든 자원의 추상화 계층**이므로,
각 자원에서 발생하는 가상화 오버헤드를 동시에 분석해야 한다.

---

## 2. E2E 계층별 가상화 실행 흐름

### 2-1. Hardware / Physical Layer

물리 자원은 변하지 않는다. 가상화는 이 위에 추상화를 쌓는다.

```
Physical CPU Cores
Physical DRAM Chips
Physical NVMe NAND
Physical NIC (PHY/MAC)
    │
    ▼ (가상화 계층 시작)
Hypervisor (KVM/Xen) 또는 Container Runtime (containerd/runc)
```

**가상화 오버헤드 발생 지점:**

| 물리 자원 | 가상화 방식 | 오버헤드 |
|----------|-----------|---------|
| CPU Core | vCPU ↔ Physical Core 매핑, CFS 스케줄링 | Context Switch, Steal Time |
| DRAM | Virtual Address → Physical Page (TLB) | TLB Miss, Page Fault |
| NVMe | Virtual Disk → Block Device | I/O 추상화 계층 지연 |
| NIC | Virtual NIC → Physical NIC (VXLAN/OVS) | Encapsulation 오버헤드 |

### 2-2. OS Kernel Layer

Linux Kernel이 가상화의 핵심 엔진이다.

**Virtual Memory 관리:**

```
JVM Process Virtual Address Space (예: 64GB)
    │
    ├─ JVM Heap Region (Young + Old)
    ├─ JVM Code Cache (JIT 컴파일 코드)
    ├─ Stack (Thread별 Virtual Stack)
    ├─ DirectByteBuffer (Off-Heap)
    └─ Mapped Files (mmap: jar, config)
         │
         ▼
    Page Table (Virtual → Physical 매핑)
         │
         ├─ TLB Hit  → 즉시 Physical Address 반환 (~1ns)
         └─ TLB Miss → Page Table Walk (~100ns) → Physical DRAM
```

**cgroup 기반 자원 격리 (Kubernetes):**

```
Kubernetes Pod
    │
    └─ cgroup v2 계층
         ├─ cpu.cfs_quota_us / cpu.cfs_period_us → vCPU 할당량 제한
         ├─ memory.limit_in_bytes → 메모리 상한
         ├─ blkio.throttle → Block I/O 제한
         └─ net_cls / net_prio → 네트워크 우선순위
```

**핵심 커널 메커니즘:**

- **CFS Scheduler:** 각 Java Thread를 vCPU에 스케줄링. cgroup cpu.shares/quota로 물리 Core 시간 제한
- **Page Table / TLB:** JVM Heap의 모든 객체 접근 시 Virtual→Physical 변환 발생
- **mmap / Page Cache:** Kafka, MySQL의 파일 I/O가 Page Cache를 통해 Virtual Memory에 매핑
- **Copy-on-Write (COW):** Redis `BGSAVE`, Kafka Consumer Group Rebalance 시 Fork → COW 발생

### 2-3. JVM Runtime Layer

JVM 자체가 가상화 계층이다.

```
Java Source Code
    │ (javac)
    ▼
JVM Bytecode (.class)
    │
    ├─ Interpreter (초기 실행)
    ├─ C1 JIT (Tier 3: 경량 최적화)
    └─ C2 JIT (Tier 4: 최적화 컴파일 → Native Code)
         │
         ▼
    Physical CPU Instructions
```

**JVM 내부 가상화 메커니즘:**

| JVM 메커니즘 | 가상화 역할 | 물리 자원 매핑 |
|-------------|-----------|-------------|
| Heap (Virtual) | 객체 생성/소멸 추상화 | Physical DRAM Pages |
| TLAB | Thread별 Heap 격리 할당 | L1/L2 Cache 친화 DRAM |
| GC (G1/ZGC) | 메모리 재사용 자동화 | CPU + DRAM Bandwidth |
| JIT Code Cache | Bytecode → Native 변환 캐시 | L2/L3 Cache + DRAM |
| Safepoint | 모든 Thread 일시 정지 지점 | CPU 전체 stall |
| Class Loading | 동적 코드 로딩 추상화 | Metaspace (Off-Heap) |

**Virtual Thread (Java 21+, Project Loom):**

```
Virtual Thread (수백만 개 생성 가능)
    │
    └─ ForkJoinPool (Platform Thread = OS Thread)
         │
         └─ OS Thread → vCPU → Physical Core
```

기존 Platform Thread는 OS Thread와 1:1 매핑이었으나,
Virtual Thread는 M:N 모델로 Platform Thread 위에서 동작한다.

**FinTech 결제 서비스 영향:**
- Spring MVC + Virtual Thread: 결제 요청당 OS Thread 생성 불필요 → Thread 생성 비용 제거
- Blocking DB 호출 시: Virtual Thread가 unmount → Platform Thread 반환 → 다른 Virtual Thread 실행

### 2-4. Framework / Middleware Layer

**Spring WebFlux (Reactor) — 가상 실행 모델:**

```
HTTP 요청 수신 (Netty EventLoop)
    │
    └─ Reactor Publisher Chain
         ├─ Mono/Flux = 가상 실행 흐름 (실제 실행 시점은 subscribe)
         ├─ subscribeOn(Schedulers.boundedElastic()) → Thread Pool 전환
         └─ publishOn(Schedulers.parallel()) → CPU Core 할당 변경
```

Reactor의 Mono/Flux는 실행 흐름 자체를 가상화한다.
실제 실행은 `subscribe()` 시점에 스케줄러에 의해 결정된다.

**Kafka — Virtual Partition:**

```
Kafka Topic Partition (논리 단위)
    │
    ├─ Leader Replica (Physical: 특정 Broker NVMe)
    └─ Follower Replica (Physical: 다른 Broker NVMe)
         │
         ▼
Page Cache (Virtual Memory → Physical DRAM)
    │
    ▼
NVMe Flush (Physical I/O)
```

**MySQL InnoDB — Virtual Buffer Pool:**

```
SQL Query → InnoDB Buffer Pool (Virtual Memory, 물리 DRAM 점유)
    │
    ├─ Buffer Hit  → DRAM에서 즉시 반환
    └─ Buffer Miss → NVMe에서 Page 로드 (Physical I/O 발생)
         │
         ▼
    Redo Log fsync → NVMe (Physical IOPS 소비)
```

### 2-5. Application / Business Layer

결제 승인 로직이 소비하는 가상 자원:

| 결제 처리 단계 | 가상 자원 | 실제 물리 자원 소비 |
|-------------|---------|-----------------|
| HTTP 수신 | Virtual NIC (VXLAN) | Physical NIC + PCIe |
| JWT 검증 | JVM JIT Code | CPU Core (L2 Cache) |
| DB 조회 | InnoDB Buffer Pool | DRAM 또는 NVMe |
| 결제 상태 변경 | JVM Heap Object | Physical DRAM Page |
| Kafka Produce | Page Cache | DRAM → NVMe |
| 응답 송신 | Virtual Socket Buffer | Physical NIC |

---

## 3. Virtual Resource Flow — 핵심 메커니즘

### 3-1. Virtual Memory & Page Fault

Java 애플리케이션의 모든 메모리 접근은 Virtual Address를 통한다.

```
JVM이 새 객체 할당 요청
    │
    ├─ TLAB 내 공간 있음 → 즉시 Virtual Address 반환 (빠름)
    └─ TLAB 소진 → 새 TLAB 요청
         │
         └─ GC가 Heap 확장 시도
              │
              ├─ Page Table에 매핑된 Physical Page 있음 → 즉시 접근
              └─ 없음 → Page Fault 발생
                   │
                   └─ Kernel이 Physical Page 할당 + Page Table 업데이트
                        → Minor Page Fault: DRAM에서 처리 (수 μs)
                        → Major Page Fault: Swap에서 로드 (수 ms~수백 ms)
```

**FinTech 영향:** JVM Heap이 DRAM을 초과하여 Swap 발생 시 결제 P99 폭증

### 3-2. TLB (Translation Lookaside Buffer) Miss

```
JVM Thread가 Object 접근
    │
    ├─ TLB Hit  → Virtual→Physical 변환 완료 (~1ns)
    └─ TLB Miss → Page Table Walk 필요
         │
         ├─ L1/L2 Page Table Cache Hit → ~10ns
         └─ DRAM Page Table 접근 → ~100ns
              │
              └─ TLB에 등록 후 반환
```

**GC 이후 TLB Flush 발생:**
- GC가 객체를 이동(Compaction) 시 → Virtual→Physical 매핑 변경 → TLB Invalidate
- 다음 접근 시 TLB Miss 폭증 → Memory Latency 일시 급증 → Tail Latency 증가

### 3-3. cgroup CPU Throttling (가상 CPU 제한)

Kubernetes에서 가장 흔한 가상화 장애 원인이다.

```
Pod CPU Limit: 2 Core (= cpu.cfs_quota_us 200000 / period 100000)

결제 서비스 CPU 사용량 급증
    │
    └─ cgroup CPU Quota 소진
         │
         └─ Kernel CFS Scheduler: 해당 cgroup sleep 강제
              (cpu.cfs_period_us 만료까지 실행 불가)
              │
              └─ Java Thread → Off-CPU 상태
                   │
                   └─ 결제 처리 지연 → P99 증가
                        → Client Timeout → Retry
```

**핵심:** CPU가 물리적으로 남아있어도 cgroup quota 소진 시 Thread는 실행되지 못한다.

### 3-4. Virtual Network (VXLAN/Overlay) 오버헤드

Kubernetes Overlay Network의 패킷 처리 흐름:

```
결제 요청 패킷 (Pod A → Pod B)
    │
    ├─ Pod A: Virtual NIC (veth pair)
    │
    ├─ Node A: Linux Bridge / OVS
    │    └─ VXLAN Encapsulation: Inner IP + Outer UDP/IP 헤더 추가
    │         (오버헤드: ~50 bytes, MTU 문제 가능)
    │
    ├─ Physical Network: Encapsulated 패킷 전송
    │
    └─ Node B: VXLAN Decapsulation → Pod B Virtual NIC
```

**오버헤드 원인:**
- Encapsulation/Decapsulation: CPU 추가 소비
- MTU 축소: 1500 → ~1450 (VXLAN 헤더), 대형 패킷 Fragmentation 발생 가능
- netfilter/iptables: 각 패킷마다 규칙 평가

### 3-5. Virtual Disk (PV/PVC) I/O 경로

Kubernetes PersistentVolume이 MySQL WAL에 미치는 영향:

```
MySQL InnoDB fsync()
    │
    └─ VFS (Virtual Filesystem)
         │
         └─ Device Mapper / CSI Driver (가상 디스크 계층)
              │
              ├─ Local NVMe (빠름: ~100μs)
              └─ Network Storage (EBS/Ceph): ~1~10ms
                   │
                   └─ 물리 스토리지 클러스터
```

**가상 디스크 계층이 추가될수록 fsync Latency 증가 → DB Commit Latency 증가**

### 3-6. JVM Safepoint (전체 Thread 일시 정지)

GC 및 JIT 최적화 시 발생하는 JVM 내부 가상화 중단점:

```
G1GC Young GC 트리거
    │
    └─ JVM Safepoint 요청
         │
         └─ 모든 Java Thread: 현재 실행 중단, Safepoint 도달 대기
              │
              ├─ Safepoint 도달 빠름: ~수 ms
              └─ Safepoint 도달 느림: JNI, Bytecode Loop 길 경우 → ~수십 ms
                   │
                   └─ 결제 처리 완전 정지 → P99/P100 급증
```

---

## 4. Resource Flow Correlation

### 가상화 계층별 자원 오버헤드

```
결제 TPS 증가
    │
    ├─► vCPU / cgroup 제한
    │       │
    │       ├─► CPU Throttling (Quota 소진)
    │       ├─► Context Switch 증가 (Thread → vCPU 경쟁)
    │       └─► Steal Time 증가 (Hypervisor 환경)
    │
    ├─► Virtual Memory
    │       │
    │       ├─► TLB Miss 증가 (Heap 증가, GC 후 객체 이동)
    │       ├─► Page Fault 증가 (Heap 확장, Swap)
    │       └─► NUMA Remote Access (가상화로 NUMA 인식 부족 시)
    │
    ├─► Virtual Network (Overlay)
    │       │
    │       ├─► Encapsulation CPU 오버헤드
    │       ├─► MTU Fragmentation
    │       └─► iptables/netfilter 지연
    │
    └─► Virtual Disk
            │
            ├─► CSI/Device Mapper 추가 지연
            ├─► NVMe fsync 지연 (EBS: 네트워크 스토리지)
            └─► I/O Throttle (cgroup blkio)
```

### 자원 트레이드오프

| 가상화 기술 | 절약 자원 | 추가 소비 자원 | FinTech 트레이드오프 |
|-----------|---------|-------------|-------------------|
| Kubernetes cgroup | 자원 격리 보장 | CPU Throttling 위험 | Limit 설정 과소 시 P99 급증 |
| JVM Virtual Heap | 메모리 관리 자동화 | GC CPU + Safepoint STW | Heap 튜닝 필수 |
| Overlay Network | 네트워크 격리 | CPU (Encap/Decap) + Latency | 결제 레이턴시에 직접 영향 |
| Virtual Disk (EBS) | 스토리지 유연성 | fsync Latency 증가 | DB Commit 지연 → P99 증가 |
| Virtual Thread (Loom) | Thread 생성 비용 절감 | Scheduler 오버헤드 | Blocking I/O 많은 결제 서비스에 유리 |

---

## 5. Top-Down 분석: 증상 → 가상화 원인 추적

### 증상 1: 결제 API P99 주기적 급증

```
증상: 매 100ms마다 P99 Latency 급증
    │
    ├─► GC 로그 확인 → STW 없음
    ├─► perf stat 확인 → CPU 정상
    ├─► kubectl top pod 확인 → CPU Usage 정상
    │
    └─► cgroup throttling 확인
         cat /sys/fs/cgroup/cpu/.../cpu.stat
         → throttled_time 증가 확인
              │
              └─► 원인: cpu.cfs_quota_us 소진
                       (CPU Limit 너무 낮게 설정)
                       → 매 Period마다 Java Thread 강제 sleep
```

**해결 방향:** CPU Limit 상향 또는 Request/Limit 비율 조정

### 증상 2: 결제 DB 연결 지연

```
증상: MySQL Connection Timeout 간헐적 발생
    │
    ├─► iostat 확인 → NVMe util 낮음
    ├─► MySQL slow query 없음
    │
    └─► PersistentVolume 타입 확인
         → EBS (Network Storage) 사용 중
              │
              └─► 원인: Virtual Disk (EBS) fsync Latency
                       네트워크 스토리지 특성상 fsync ~3~10ms
                       결제 Commit 직렬화 → 누적 지연
```

**해결 방향:** `innodb_flush_log_at_trx_commit=2` 또는 Local NVMe 사용

### 증상 3: Kafka Consumer Lag 급증

```
증상: 결제 이벤트 Consumer Lag 증가
    │
    ├─► Consumer CPU 정상
    ├─► Network 정상
    │
    └─► Kafka Broker Page Cache 확인
         → Page Cache 부족 → Disk I/O 발생
              │
              └─► 원인: 같은 Node에 여러 Pod이 DRAM 경쟁
                       (Noisy Neighbor: 가상화 자원 경합)
                       Page Cache 축소 → Kafka I/O 증가
```

### 증상 4: 결제 서비스 간헐적 OOMKilled

```
증상: Pod OOMKilled 주기적 발생
    │
    ├─► kubectl describe pod → OOMKilled 확인
    ├─► JVM Heap 설정 확인 → -Xmx4g
    │
    └─► Container Memory Limit 확인 → 4GB
         │
         └─► 원인: JVM Off-Heap 메모리 미고려
                  Heap(4GB) + Metaspace + DirectMemory + Stack
                  = 실제 사용 ~5.5GB > Limit 4GB
                  → OOM Killer 발동
```

**해결 방향:** Container Limit = Heap + Off-Heap 여유분 (일반적으로 Heap × 1.5~2배)

---

## 6. Bottom-Up 분석: 가상화 특성 → Java 성능 영향

### cgroup CPU Quota → JVM Thread Starvation

```ini
# Kubernetes Pod 설정
resources:
  limits:
    cpu: "2"        # cpu.cfs_quota_us = 200000
  requests:
    cpu: "1"

# JVM이 Runtime.getRuntime().availableProcessors() 호출 시
# Physical Core 수(예: 32)를 반환할 수 있음 → Thread Pool 과대 설정
# GC Thread, JIT Thread가 cgroup quota 초과 소비
# → 결제 처리 Thread에 할당될 quota 감소
```

**Java 11+에서는 cgroup 인식:**
```bash
java -XX:ActiveProcessorCount=2 \
     -XX:+UseContainerSupport \
     PaymentApplication
# cgroup CPU Limit 기준으로 GC Thread, JIT Thread 수 자동 조정
```

### TLB Size → GC 후 Memory Latency

```
G1GC Compaction 발생
    │
    └─ 수천~수만 개 객체 이동 (Virtual Address 변경)
         │
         └─ TLB Invalidate (전체 또는 부분)
              │
              └─ 이후 첫 접근마다 TLB Miss
                   → Page Table Walk (~100ns)
                   → GC 직후 수 ms 간 처리량 저하
                   → 결제 P95 구간에 영향
```

**THP (Transparent HugePage) 영향:**
```bash
# THP enabled → JVM Heap에 2MB 페이지 적용
# → TLB Miss 감소 (페이지당 커버 범위 확대)
# → 하지만 Compaction 시 2MB 단위 이동 → 비용 증가

# FinTech 권장: THP=madvise (JVM이 직접 제어)
echo madvise > /sys/kernel/mm/transparent_hugepage/enabled
```

### Virtual Network MTU → Kafka Producer 성능

```
Physical MTU: 1500 bytes
VXLAN Overhead: 50 bytes
Effective MTU: 1450 bytes

Kafka Producer batch.size: 16384 bytes (기본값)
    → 여러 패킷으로 분할 전송
    → VXLAN Encapsulation × N회 발생
    → CPU 오버헤드 증가

# 해결: Jumbo Frame 설정 (MTU 9000)
# 또는 Kafka batch.size 축소
```

### Virtual Disk Latency → HikariCP Timeout

```
MySQL on EBS (Virtual Disk)
    fsync latency: P99 ~8ms

HikariCP connectionTimeout: 30000ms (기본)
HikariCP maxLifetime: 1800000ms

결제 TPS 증가 → DB Connection Hold 시간 증가
    → Virtual Disk latency 누적
    → HikariCP Pool 고갈
    → getConnection() timeout
    → 결제 500 Error
```

---

## 7. 장애 및 Saturation 패턴

### 7-1. cgroup CPU Throttling Cascade

**트리거:** 결제 트래픽 Burst + CPU Limit 과소 설정

```
결제 요청 급증
    → JVM Thread CPU 사용량 증가
    → cgroup cpu.cfs_quota_us 소진 (매 100ms period)
    → Java Thread 전체 sleep (Off-CPU)
    → 결제 처리 중단 (수십 ms)
    → P99 급증
    → Client Timeout
    → Retry 발생
    → 다음 Period에 Retry까지 처리 → Quota 더 빠르게 소진
    → Cascading Throttling
```

**관측:**
```bash
# cgroup throttling 확인
cat /sys/fs/cgroup/cpu/kubepods/pod<uid>/<container>/cpu.stat
# throttled_periods, throttled_time 확인

# eBPF로 Off-CPU 분석
/usr/share/bcc/tools/offcputime -p <pid> 30
```

### 7-2. JVM GC + Virtual Memory Pressure

**트리거:** 다수 Pod이 동일 Node에서 DRAM 경쟁

```
Node DRAM: 64GB
Pod A (결제 서비스): Heap 16GB + Off-Heap 4GB = 20GB
Pod B (Kafka): Page Cache 20GB
Pod C (MySQL): Buffer Pool 16GB
합계: 56GB (정상 범위)

Pod D 신규 배포 → Node 총 메모리 70GB 요구
    → Linux Kernel: Page Cache Eviction (Kafka 피해)
    → Kafka Broker: Page Cache Miss → NVMe I/O 증가
    → Consumer Lag 증가
    → 결제 이벤트 처리 지연
    → 결제 상태 불일치 위험
```

**관측:**
```bash
# Node 메모리 압박
cat /proc/pressure/memory   # PSI (Pressure Stall Information)
free -h                     # 가용 메모리 확인
vmstat 1                    # Swap 발생 여부
```

### 7-3. Virtual Network Packet Drop (Overlay)

**트리거:** 결제 트래픽 Burst + Overlay Network MTU 문제

```
결제 요청 대형 Payload (> 1450 bytes)
    → VXLAN Encapsulation → IP Fragmentation 발생
    → 일부 Fragment Drop (네트워크 장비 설정)
    → TCP Retransmission 발생
    → TLS Handshake 재시도
    → 결제 연결 지연 증가
    → P99 Latency 급증
```

**관측:**
```bash
# Overlay Network Drop
ip -s link show eth0         # RX/TX errors, drops
netstat -s | grep fragment   # IP Fragmentation 통계
cat /proc/net/softnet_stat   # SoftIRQ Drop
```

### 7-4. Virtual Thread (Loom) Pinning

**트리거:** Virtual Thread가 Platform Thread에 고착되는 상황

```java
// 문제 패턴: synchronized 블록 내에서 Blocking I/O
synchronized(lock) {
    // Virtual Thread가 여기서 block → Platform Thread Pin 발생
    jdbcConnection.executeQuery(sql);
}

// Virtual Thread가 unmount 불가 → Platform Thread 점유 지속
// ForkJoinPool Platform Thread 고갈
// → 다른 Virtual Thread 실행 불가
// → 결제 처리 Thread Starvation
```

**관측:**
```bash
# Virtual Thread Pinning 감지
-Djdk.tracePinnedThreads=full

# JFR로 Virtual Thread 분석
jcmd <pid> JFR.start duration=30s filename=vthread.jfr
```

### 7-5. JVM Safepoint Delay (가상화 환경 악화)

**트리거:** cgroup CPU Throttling 중 Safepoint 도달 지연

```
GC Safepoint 요청
    │
    └─ 일부 Java Thread: cgroup Throttling 중 (Off-CPU 상태)
         │
         └─ Throttling 해제될 때까지 Safepoint 도달 불가
              → Safepoint 대기 시간 수십~수백 ms
              → 모든 결제 처리 Thread 정지
              → 결제 P100 Latency = Safepoint 대기 시간
```

---

## 8. Linux / JVM / Kubernetes 실무 관측

### 8-1. cgroup / CPU Throttling

```bash
# Pod별 CPU Throttling 확인
cat /sys/fs/cgroup/cpu/kubepods/pod<uid>/<container>/cpu.stat
# 항목: nr_periods, nr_throttled, throttled_time

# cgroup quota 확인
cat /sys/fs/cgroup/cpu/kubepods/pod<uid>/<container>/cpu.cfs_quota_us
cat /sys/fs/cgroup/cpu/kubepods/pod<uid>/<container>/cpu.cfs_period_us

# PSI (Pressure Stall Information)
cat /proc/pressure/cpu
cat /proc/pressure/memory
cat /proc/pressure/io
```

### 8-2. Virtual Memory / TLB

```bash
# Page Fault 통계
vmstat 1           # si(swap-in), so(swap-out), pgfault
sar -B 1           # pgfault/s, majflt/s

# TLB Miss (perf)
perf stat -e \
  dTLB-load-misses,iTLB-load-misses,\
  dTLB-loads,iTLB-loads \
  -p <pid> sleep 10

# THP 상태
cat /sys/kernel/mm/transparent_hugepage/enabled
grep -i huge /proc/meminfo
```

### 8-3. JVM Virtual Memory 분석

```bash
# JVM Heap 및 메모리 구조
jcmd <pid> VM.native_memory summary
jcmd <pid> GC.heap_info

# GC 로그 분석 (Safepoint 포함)
java -Xlog:gc*:gc.log:time,uptime,level,tags \
     -Xlog:safepoint:safepoint.log:time,uptime \
     PaymentApplication

# JFR 기록
jcmd <pid> JFR.start \
  duration=60s \
  settings=profile \
  filename=payment.jfr

# async-profiler (CPU + Virtual Thread)
./asprof -d 30 \
  -e cpu \
  -f flamegraph.html \
  <pid>
```

### 8-4. Virtual Network

```bash
# Overlay Network 상태
ip link show
ip -s link        # RX/TX 통계

# VXLAN 인터페이스
ip -d link show flannel.1  # Flannel
ip -d link show vxlan.calico  # Calico

# MTU 확인
ip link show eth0 | grep mtu
cat /sys/class/net/eth0/mtu

# 패킷 Drop
netstat -s
cat /proc/net/softnet_stat

# eBPF Network 추적
/usr/share/bcc/tools/tcplife   # TCP 연결 수명
/usr/share/bcc/tools/tcpdrop   # Drop 이유
/usr/share/bcc/tools/tcpretrans # Retransmission
```

### 8-5. Virtual Disk / Storage

```bash
# PVC / 스토리지 타입 확인
kubectl get pv
kubectl describe pvc <name>

# Block I/O 지연 (가상 디스크 포함)
iostat -x 1

# fsync Latency 추적 (eBPF)
/usr/share/bcc/tools/biolatency -D  # 디스크별 분포
/usr/share/bcc/tools/ext4slower 10  # 10ms 이상 I/O
/usr/share/bcc/tools/fileslower 10  # 파일별 느린 I/O
```

### 8-6. Kubernetes 가상 자원 관측

```bash
# Node/Pod 자원 사용률
kubectl top nodes
kubectl top pods -n payment --containers

# Pod 이벤트 (OOMKilled, Eviction, Throttling)
kubectl describe pod <pod> -n payment
kubectl get events -n payment --sort-by='.lastTimestamp'

# Node 상세 (Allocatable vs Capacity)
kubectl describe node <node>

# cAdvisor 메트릭 (Prometheus)
# container_cpu_cfs_throttled_seconds_total
# container_memory_working_set_bytes
# container_oom_events_total
```

### 8-7. eBPF 기반 가상화 추적

```bash
# cgroup CPU Throttling 분석
/usr/share/bcc/tools/runqlat          # Run Queue 대기 시간
/usr/share/bcc/tools/offcputime -p <pid> 30  # Off-CPU 분석

# Virtual Memory 이벤트
/usr/share/bcc/tools/memleak -p <pid>  # 메모리 누수
/usr/share/bcc/tools/swapin            # Swap In 이벤트

# 전체 시스템 프로파일
/usr/share/bcc/tools/profile -F 99 -p <pid> 30  # CPU Flamegraph
```

---

## Summary: Virtual 자원이 FinTech 결제 시스템에 미치는 핵심 축

| 가상화 계층 | 결제 KPI 영향 | 대표 장애 패턴 | 핵심 관측 지표 |
|-----------|------------|-------------|-------------|
| cgroup CPU Quota | Latency, TPS | CPU Throttling, Thread Starvation | `cpu.stat` throttled_time |
| Virtual Memory / TLB | P99 Latency | TLB Miss 폭증, Page Fault, Swap | `perf stat` dTLB-misses |
| JVM Heap (Virtual) | GC Pause, OOM | STW, OOMKilled | `jstat -gc`, JFR |
| Overlay Network | 결제 연결 지연 | MTU Fragmentation, Packet Drop | `ip -s link`, tcpdrop |
| Virtual Disk (EBS) | DB Commit 지연 | fsync Latency, I/O Queue | `iostat -x`, biolatency |
| Safepoint | 전체 중단 | GC + Throttling 복합 STW 연장 | GC 로그 safepoint 대기 |
| Virtual Thread (Loom) | Thread 효율 | Pinning, Platform Thread 고갈 | JFR, tracePinnedThreads |

> 가상화는 자원 효율과 격리를 제공하지만, 각 추상화 계층은 반드시 오버헤드와 새로운 장애 유형을 동반한다. SRE는 결제 P99 이상 증상을 발견했을 때, 애플리케이션 코드가 아닌 **cgroup quota, TLB Miss, Overlay MTU, Virtual Disk fsync** 경로까지 추적해야 한다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*