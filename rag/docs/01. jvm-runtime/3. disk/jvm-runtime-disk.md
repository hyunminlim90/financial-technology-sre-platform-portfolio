# JVM Runtime — Disk Resource Analysis
> **FinTech Payment System · SRE Perspective**  
> E2E: Hardware / OS Kernel / Runtime / Framework / Application

> 정독: 0회

## 목차

1. [물리/가상 Disk 및 Storage 스펙 확인 지표](#1-물리가상-disk-및-storage-스펙-확인-지표)
2. [JVM Disk 실행 흐름 (E2E I/O Path)](#2-jvm-disk-실행-흐름-e2e-io-path)
3. [JVM Disk 사용 메커니즘 및 I/O 실행 모델 분석](#3-jvm-disk-사용-메커니즘-및-io-실행-모델-분석)
4. [Disk 병목 및 위험 발생 지점](#4-disk-병목-및-위험-발생-지점)
5. [SRE 관점 모니터링 지표](#5-sre-관점-모니터링-지표)
6. [장애 시나리오](#6-장애-시나리오)
7. [튜닝 포인트](#7-튜닝-포인트)
8. [관련 Linux 명령어 및 분석 도구](#8-관련-linux-명령어-및-분석-도구)

---

## 1. 물리/가상 Disk 및 Storage 스펙 확인 지표

JVM 프로세스의 I/O 특성을 파악하기 전, 하드웨어 레이어의 물리적 한계치와 구성을 먼저 확정해야 합니다.

### 1-1. 디스크 유형별 특성 및 JVM 연관성

| 디스크 유형 | 순차 Throughput | Random IOPS | JVM 연관 워크로드 |
|------------|----------------|-------------|-----------------|
| **HDD (7200rpm)** | ~150 MB/s | ~100 IOPS | GC 로그, 아카이브 로그 (비실시간) |
| **SATA SSD** | ~500 MB/s | ~80,000 IOPS | 일반 애플리케이션 로그, Heap Dump |
| **NVMe SSD** | ~3,500 MB/s | ~500,000 IOPS | DB WAL, JVM GC 로그, JFR 실시간 기록 |
| **NVMe (PCIe 4.0)** | ~7,000 MB/s | ~1,000,000 IOPS | 핀테크 결제 DB, 실시간 감사 로그 |

```bash
# 디스크 타입 및 인터페이스 확인
lsblk -d -o NAME,ROTA,TRAN,SIZE,MODEL
# ROTA=0: SSD, ROTA=1: HDD / TRAN: nvme, sata, sas

# NVMe 상세 정보
nvme list
nvme id-ctrl /dev/nvme0 | grep -E 'mn|sn|fr'
```

### 1-2. I/O Scheduler 확인

JVM의 I/O 패턴(주로 순차 쓰기 + 랜덤 fsync)에 따라 적합한 스케줄러가 달라집니다.

```bash
# 현재 I/O 스케줄러 확인
cat /sys/block/nvme0n1/queue/scheduler
# [none] mq-deadline kyber bfq

# Queue Depth 확인
cat /sys/block/nvme0n1/queue/nr_requests   # 기본 64~256
cat /sys/block/nvme0n1/queue/nr_hw_queues  # NVMe: CPU 수만큼 존재
```

| 스케줄러 | 특성 | JVM 권장 환경 |
|---------|------|-------------|
| **none** | 커널 병합만, 순서 없음 | NVMe SSD (고성능 스토리지) |
| **mq-deadline** | 읽기 우선, 기아 방지 | SATA SSD, 혼합 워크로드 |
| **bfq** | 프로세스별 공정 배분 | 공유 서버, 멀티 테넌시 환경 |
| **kyber** | 레이턴시 목표 기반 | 저지연 요구 NVMe 환경 |

### 1-3. Filesystem 구조

| Filesystem | JVM 특화 특성 | 핀테크 권장 여부 |
|-----------|-------------|----------------|
| **XFS** | 대형 파일, 병렬 I/O 최적. journaling 오버헤드 낮음 | ✅ 권장 (로그, Heap Dump) |
| **EXT4** | 범용. `dir_index` B-tree로 대량 소형 파일 처리 | ✅ 권장 (일반 환경) |
| **ZFS** | CoW + 압축 + 체크섬. 메모리 요구량 높음 | 🟡 ZFS ARC가 JVM 힙과 메모리 경합 주의 |
| **tmpfs** | RAM 기반. JVM 임시 파일 최적 | ✅ `/tmp`, JVM 소켓 파일 |

```bash
# 마운트 옵션 확인 (noatime 여부 등)
findmnt -D
cat /proc/mounts | grep -E 'xfs|ext4'
```

### 1-4. IOPS / Throughput / Queue Depth / Latency 스펙

```bash
# fio로 실제 디스크 성능 측정 (사전 벤치마크)
# 4K 랜덤 쓰기 (JVM 로그/WAL 유사 패턴)
fio --name=randwrite --ioengine=libaio --iodepth=32 \
    --rw=randwrite --bs=4k --direct=1 \
    --size=4g --numjobs=4 --runtime=60 \
    --group_reporting --filename=/dev/nvme0n1

# 512B~4K 순차 쓰기 (GC 로그 유사 패턴)
fio --name=seqwrite --ioengine=libaio --iodepth=8 \
    --rw=write --bs=512 --direct=0 \
    --size=2g --numjobs=1 --runtime=30
```

### 1-5. Container / K8s Storage 구조

| 계층 | 구성 요소 | JVM 분석 포인트 |
|------|----------|----------------|
| **OverlayFS** | 컨테이너 레이어 파일시스템 | 쓰기 시 CoW 발생 → JVM 로그 쓰기 지연 |
| **emptyDir** | Pod 임시 볼륨 | JVM Heap Dump, GC 로그 기본 경로. 노드 디스크 공유 |
| **PVC + CSI** | 영구 볼륨 | StorageClass IOPS 한도 확인 필수 |
| **tmpfs emptyDir** | `medium: Memory` | JVM 소켓/임시 파일 I/O 최적화 |

```bash
# K8s StorageClass IOPS 한도 확인
kubectl get storageclass -o yaml | grep -E 'iops|throughput|type'

# PVC 상태 및 용량 확인
kubectl describe pvc <pvc-name>
```

---

## 2. JVM Disk 실행 흐름 (E2E I/O Path)

### 2-1. 전체 E2E I/O 흐름

JVM에서 발생하는 주요 Disk I/O 경로를 계층별로 정리합니다.

```
Application (Java Code)
  │  FileWriter.write() / Logger.info() / RandomAccessFile
  ▼
Framework (Logback / Log4j2 / JDBC)
  │  AsyncAppender 버퍼 / PreparedStatement / FileChannel
  ▼
JVM Runtime
  │  FileOutputStream → FileDescriptor → write() syscall
  │  NIO FileChannel → pwrite64() syscall
  │  MappedByteBuffer → mmap + page fault
  ▼
OS Kernel - VFS (Virtual File System)
  │  sys_write → vfs_write → inode → address_space
  ▼
Page Cache (Kernel Buffer)
  │  Dirty Page 축적 → pdflush/writeback 스레드 → 디스크 전송
  │  (fsync() 호출 시 즉시 flush 강제)
  ▼
Block Layer (blk-mq)
  │  bio 생성 → I/O Scheduler → dispatch queue
  ▼
Device Driver (NVMe / AHCI)
  │  DMA 전송 요청 → NVMe Command Queue
  ▼
Physical Disk (NVMe / SATA SSD)
  └─ NAND Flash 쓰기 완료 → Interrupt → DMA 완료 → syscall 반환
```

### 2-2. JVM 주요 I/O 경로별 상세 흐름

#### ① 로그 쓰기 경로 (Logback/Log4j2 → Disk)

```
결제 트랜잭션 로그 발생
  → Logback AsyncAppender (내부 BlockingQueue 버퍼링)
  → 백그라운드 스레드: OutputStreamAppender → FileOutputStream
  → FileOutputStream.write() → write() syscall
  → VFS → Page Cache (Dirty Page 생성)
  → OS writeback 또는 RollingFileAppender 닫힘 시 flush
  → Block Layer → NVMe
```

> ⚠️ **동기(Blocking) Appender 사용 시:** 결제 로직 스레드가 직접 write() syscall 호출 → Disk 지연이 결제 레이턴시에 직접 영향

#### ② Heap Dump 경로

```
OOM 트리거 또는 jmap -dump 명령
  → JVM HeapDumper (STW 동반)
  → HprofWriter → FileOutputStream (Buffered)
  → 수 GB 순차 쓰기 → Page Cache → Block Layer
  → NVMe 순차 I/O (최대 Throughput 사용)
```

> ⚠️ **Heap Dump 중 STW + 대용량 I/O 동시 발생** → 결제 서비스 완전 중단 위험

#### ③ JFR (Java Flight Recorder) 기록 경로

```
JVM 이벤트 발생 (GC, 메서드 호출, I/O 등)
  → JFR 이벤트 버퍼 (Thread-Local + Global Buffer)
  → 주기적으로 .jfr 파일에 flush (기본 1초)
  → FileChannel.write() → Page Cache → Block Layer
```

#### ④ JDBC / DB 연결 경로 (WAL 관점)

```
결제 DB 쓰기 (INSERT/UPDATE)
  → JDBC → TCP → DB 서버
  → DB WAL (Write-Ahead Log) → fsync()
  → Block Layer → NVMe (DB 서버의 디스크)
  → ack 반환 → JDBC commit 완료 → 결제 트랜잭션 완료
```

> JVM 자체의 Disk가 아닌 DB 서버 Disk 성능이 결제 레이턴시를 결정하는 핵심 변수

### 2-3. DMA / Interrupt / bio / blk-mq 구조

```
JVM write() syscall
  │
  ├─ Page Cache 히트 (Buffered I/O) → 즉시 반환, 나중에 writeback
  │
  └─ Page Cache 미스 또는 fsync() / O_DIRECT
       │
       ▼
  bio (Block I/O 구조체) 생성
  → blk-mq (Multi-Queue Block Layer)
     └─ 하드웨어 큐(NVMe: CPU 수만큼) → NVMe Command Queue
        └─ DMA: CPU 개입 없이 NVMe ↔ RAM 직접 전송
           └─ 완료 인터럽트 → softirq → bio 완료 → syscall 반환
```

---

## 3. JVM Disk 사용 메커니즘 및 I/O 실행 모델 분석

### 3-1. JVM 주요 I/O 메커니즘 분류

| 메커니즘 | JVM/Java API | 발생 워크로드 | Disk 특성 |
|---------|-------------|-------------|----------|
| **Buffered Write** | `FileOutputStream`, `BufferedWriter` | 애플리케이션 로그 | Page Cache 경유. 순차 쓰기. 낮은 레이턴시 |
| **fsync / fdatasync** | `FileChannel.force()` | WAL, 커밋 로그, JVM 종료 시 | Page Cache → Disk 강제 flush. 높은 레이턴시 |
| **Direct I/O** | `O_DIRECT` (JNA/JNI) | DB 엔진 직접 구현 | Page Cache 우회. 정확한 성능 측정 가능 |
| **mmap** | `FileChannel.map()` | 메모리 맵 파일, 일부 DB | Page Fault 기반 지연 I/O. TLB/Page Table 관리 |
| **NIO FileChannel** | `FileChannel.write()` | JDBC 드라이버, Netty, Kafka | Scatter/Gather I/O (`writev`) 지원 |
| **Zero-Copy (sendfile)** | `FileChannel.transferTo()` | 정적 파일 서빙, 로그 전송 | Kernel Space 내 복사. CPU 개입 없음 |
| **io_uring** | (JDK 19+ 실험적, Loom) | 미래 JVM I/O 모델 | 비동기 Kernel I/O. syscall 오버헤드 최소 |

### 3-2. Page Cache와 JVM의 관계

JVM의 대부분의 Disk I/O는 Page Cache를 경유합니다.

```
JVM write() 호출
  → Kernel Page Cache에 Dirty Page 생성 (즉시 반환)
  → pdflush/wb 스레드가 비동기로 Disk에 flush
  → flush 시점: dirty_background_ratio 초과 또는 dirty_expire_centisecs 만료

JVM read() 호출
  → Page Cache 히트: Disk 접근 없이 즉시 반환 (μs 단위)
  → Page Cache 미스: Disk Read → Page Cache 적재 → 반환 (ms 단위)
```

**JVM에서 Page Cache 효율이 중요한 시나리오:**
- 같은 설정 파일 / jar 파일 반복 읽기 → Page Cache 히트 시 재로딩 지연 없음
- GC 로그 반복 Append → Page Cache에 머문 상태로 순차 쓰기 → 효율적
- Heap Dump 생성 → 대용량 순차 쓰기로 기존 Page Cache Eviction 유발 → 위험

### 3-3. fsync 메커니즘 (핀테크 결제 핵심)

```java
// Java에서 fsync 유발 경로
FileChannel channel = FileChannel.open(path, WRITE, SYNC);
channel.force(true);   // fdatasync(false) / fsync(true)
```

```
fsync() 호출
  → VFS: inode의 모든 Dirty Page를 writeback 요청
  → Block Layer: bio 디스패치
  → NVMe: 내부 캐시까지 flush (FUA: Force Unit Access)
  → 완료 후 syscall 반환 (수 μs ~ 수 ms)
```

> **결제 시스템에서 fsync는 내구성(Durability)의 핵심**  
> DB 커밋, 감사 로그 기록 완료의 보장은 fsync 완료에 의존합니다.  
> NVMe SSD: ~50~200μs / SATA SSD: ~1~5ms / HDD: ~5~15ms

### 3-4. Logback / Log4j2 I/O 모델

핀테크 결제 시스템에서 로그는 가장 빈번한 Disk I/O 발생원입니다.

```
[동기 Appender (위험)]
결제 스레드 → OutputStreamAppender.append()
→ write() syscall → Page Cache → (flush 대기 시) 레이턴시 증가
→ 결제 P99 레이턴시에 직접 영향

[비동기 Appender (권장)]
결제 스레드 → AsyncAppender.append()
→ BlockingQueue에 이벤트 적재 (즉시 반환)
→ 백그라운드 Worker Thread → write() syscall
→ 결제 스레드는 Disk I/O와 분리
```

**Logback AsyncAppender 핵심 설정:**

| 설정 | 기본값 | 권장값 | 설명 |
|------|--------|--------|------|
| `queueSize` | 256 | 4096~16384 | 큐 포화 시 로그 드롭 또는 블로킹 |
| `discardingThreshold` | 80 | 0 | 큐 80% 이상 시 WARN/INFO 드롭 (결제 감사 로그 위험) |
| `neverBlock` | false | true (감사 로그 제외) | 큐 포화 시 블로킹 방지 |
| `includeCallerData` | false | false 유지 | Stack trace 수집 = 고비용 I/O |

### 3-5. HikariCP / JDBC와 Disk의 관계

JVM 애플리케이션에서 JDBC를 통한 DB 접근은 결국 DB 서버의 Disk I/O로 귀결됩니다.

```
HikariCP 커넥션 풀
  → getConnection() 대기 (connectionTimeout)
  → PreparedStatement.executeUpdate() (결제 INSERT)
  → TCP 전송 → DB 서버 수신
  → DB 서버: WAL write → fsync → ack
  → JVM: commit 완료

병목 지점: DB 서버의 WAL fsync 시간 = 결제 커밋 레이턴시
```

### 3-6. JVM GC와 Disk I/O 상관관계

| GC 이벤트 | Disk 영향 | 분석 포인트 |
|----------|----------|------------|
| **Minor GC** | 없음 (Young Gen, RAM 내) | Disk 영향 없음 |
| **Major/Full GC** | GC 로그 기록 (`-Xlog:gc*:file=gc.log`) | GC 로그 파일 쓰기 지연 시 GC 완료 지연 가능 |
| **Heap Dump** | 수 GB 순차 쓰기 (STW 동반) | Disk Throughput 포화 → 전체 서비스 영향 |
| **JFR 기록** | 1초 간격 소량 순차 쓰기 | 정상 운영 중 미미한 영향 |
| **GC 로그 회전** | RollingFileAppender 동작 | 회전 시점에 fsync 발생 가능 |

---

## 4. Disk 병목 및 위험 발생 지점

### 4-1. 병목/위험 매트릭스

| 병목 유형 | 위험도 | JVM 발생 원인 | 핀테크 영향 |
|----------|--------|-------------|------------|
| **fsync Stall** | 🔴 Critical | DB WAL flush, 감사 로그 강제 동기화 | DB 커밋 지연 → 결제 타임아웃 직접 유발 |
| **Disk Full** | 🔴 Critical | 로그 폭증, Heap Dump 생성, 임시 파일 | JVM 로그 쓰기 실패 → IOException → 서비스 중단 |
| **IOPS Saturation** | 🔴 Critical | 대량 랜덤 쓰기 (DB), 로그 + Heap Dump 동시 | 모든 I/O 요청 큐잉 → Await 폭증 → 결제 레이턴시 급등 |
| **inode Exhaustion** | 🔴 Critical | 소형 로그 파일 수백만 개 (일별 롤링) | 용량 남아도 파일 생성 불가 → 로그 쓰기 실패 |
| **Disk Queue Saturation** | 🟠 High | 동시 대량 I/O (Heap Dump + GC + 로그) | Queue Depth 한계 도달 → 추가 I/O 대기 → STW 연장 |
| **High Await** | 🟠 High | IOPS 한계, 스케줄러 부적절 | await > 10ms → P99 결제 레이턴시 수백ms 증가 |
| **Page Cache Eviction** | 🟠 High | Heap Dump 대용량 쓰기로 Cache 교체 | jar/설정파일 재로딩 시 Major Page Fault → 성능 저하 |
| **Flush Delay** | 🟠 High | dirty_ratio 초과, writeback 지연 | 갑작스러운 대량 flush → I/O 버스트 → 레이턴시 급등 |
| **Write Amplification** | 🟡 Medium | NVMe NAND 특성 + 소형 랜덤 쓰기 | SSD 수명 단축, 실효 IOPS 감소 |
| **OverlayFS CoW 지연** | 🟡 Medium | 컨테이너 첫 쓰기 시 CoW 발생 | JVM 로그 첫 쓰기 시 지연. 볼륨 마운트로 회피 |
| **Journal Contention** | 🟡 Medium | EXT4 Journaling 모드에서 동시 fsync | 로그 + DB 동시 fsync 경합 → 순차 처리 지연 |
| **StorageClass Throttling** | 🟡 Medium | Cloud PVC IOPS 한도 (AWS GP2: 3 IOPS/GB) | 볼륨 크기 대비 낮은 IOPS 한도 → 자동 Throttling |
| **Compaction Overhead** | 🟡 Medium | RocksDB/LevelDB 사용 JVM 앱 | Compaction I/O와 서비스 I/O 경합 |

### 4-2. Tail Latency 증가 원인 분석

| 원인 | 레이턴시 범위 | JVM 연관 시나리오 |
|------|------------|-----------------|
| **fsync 완료 대기** | 50μs ~ 5ms | DB WAL flush, 감사 로그 commit |
| **Disk Queue 포화** | 1ms ~ 100ms+ | Heap Dump 동시 발생, GC 로그 + 서비스 로그 경합 |
| **Major Page Fault** | 수ms ~ 수백ms | Heap Dump 후 Page Cache 재적재, Swap 사용 시 |
| **NVMe Thermal Throttling** | 수ms ~ 수십ms | 장시간 고부하 쓰기 후 SSD 온도 상승 |
| **GC STW + Disk Write 중첩** | GC 시간 + Disk 대기 | Full GC 중 GC 로그 쓰기 → STW 시간 연장 |

---

## 5. SRE 관점 모니터링 지표

### 5-1. 핵심 모니터링 지표 전체 목록

| 지표 | 계층 | 수집 방법 | 임계값 / 분석 포인트 |
|------|------|----------|-------------------|
| **IOPS (r/s, w/s)** | Block | `iostat -x 1` | 디바이스 스펙 대비 사용률. 90% 이상 = 포화 임박 |
| **Throughput (MB/s)** | Block | `iostat -x 1` | 순차 쓰기 한계 도달 시 모든 I/O 큐잉 |
| **Await (ms)** | Block | `iostat -x 1` | **NVMe: < 1ms, SATA SSD: < 5ms** 초과 시 경보 |
| **svctm (ms)** | Block | `iostat -x 1` | 디바이스 실제 처리 시간. await >> svctm = 큐 대기 |
| **%util** | Block | `iostat -x 1` | **80% 이상 지속 = 포화 상태.** NVMe는 100%여도 병렬 처리 |
| **Queue Depth (aqu-sz)** | Block | `iostat -x 1` | NVMe: 32~64 정상. 지속 증가 = IOPS 한계 |
| **Dirty Pages (kB)** | OS | `/proc/meminfo` | `Dirty > dirty_ratio × RAM` 시 쓰기 블로킹 |
| **IO Wait (%iowait)** | CPU | `top`, `iostat` | **iowait > 10%** 지속 시 Disk 병목으로 CPU 낭비 |
| **PSI IO** | OS | `/proc/pressure/io` | `some > 5%`, `full > 0` = I/O 압박 심각 |
| **Page Cache Hit Ratio** | OS | `/proc/vmstat` | `pgpgin/pgpgout` 비율. 낮으면 Disk 직접 접근 증가 |
| **Flush Rate** | OS | `/proc/vmstat` | `pdflush_work` 빈도. 과도하면 Dirty Page 폭주 |
| **Read/Write Latency P99** | Block | `biolatency` (BCC) | **결제 환경: < 1ms 목표** |
| **Inode Usage (%)** | FS | `df -i` | **90% 이상 = 위험.** 소형 파일 다수 환경 필수 모니터링 |
| **Filesystem Usage (%)** | FS | `df -h` | **85% 이상 경보.** Heap Dump 공간 확보 필수 |
| **GC 로그 파일 크기/속도** | JVM | 파일시스템 모니터링 | 이상 증가 = GC 폭주 신호 |
| **Heap Dump 디스크 공간** | JVM | `df -h` + 알람 | `-Xmx` × 1.5 이상의 여유 공간 확보 필수 |
| **JFR 파일 크기** | JVM | 파일시스템 모니터링 | 지속 증가 = JFR 파일 로테이션 설정 확인 |
| **Log 디렉토리 크기** | App | `du -sh /var/log/app` | 일별 로그 증가량 추세 분석, 보존 정책 적용 |

---

## 6. 장애 시나리오

### 6-1. Disk Full — 로그 폭증으로 인한 결제 서비스 중단

**시나리오:**
```
결제 트래픽 급증 → 로그 발생량 10배 증가
→ /var/log 파티션 100% 도달
→ Logback FileAppender IOException
→ AsyncAppender 큐 포화 → 결제 스레드 블로킹
→ 결제 API 응답 중단
```

**징후:**
```bash
df -h         # /var/log: 100%
df -i         # inode 90%+ (소형 로그 파일)
dmesg | grep "No space left"
```

**대응:**
```bash
# 즉시 대용량 파일 탐색
du -sh /var/log/app/* | sort -rh | head -20
find /var/log -name "*.log" -size +1G

# Heap Dump 파일 확인 (수 GB)
find / -name "*.hprof" 2>/dev/null

# 불필요한 파일 제거 후 서비스 재기동 전 로그 레벨 조정
```

**예방:**
- 로그 디렉토리 별도 파티션 분리
- Logback `RollingFileAppender` + `maxHistory` + `totalSizeCap` 설정
- Heap Dump 경로(`-XX:HeapDumpPath`)는 별도 대용량 파티션 지정

### 6-2. fsync Latency 폭증 — 결제 커밋 타임아웃

**시나리오:**
```
결제 피크 시간 → DB WAL fsync 요청 급증
→ NVMe Queue 포화 → await 10ms → 50ms → 200ms
→ DB 커밋 응답 지연 → JDBC timeout 초과
→ HikariCP 커넥션 timeout → 결제 실패 폭증
```

**확인:**
```bash
iostat -x 1 | awk '/nvme/{print $1, $10, $13}'  # await, %util

# biolatency로 레이턴시 분포 확인
biolatency -D 5 1   # 5초 동안 블록 I/O 레이턴시 분포

# fsync 시스템 콜 빈도 확인
strace -c -p <db-pid> 2>&1 | grep fsync
```

**대응:**
- WAL 전용 NVMe 분리 배치
- `innodb_flush_log_at_trx_commit=2` (MySQL, 내구성 vs 성능 트레이드오프)
- NVMe 스케줄러 `none` 또는 `mq-deadline` 전환

### 6-3. inode Exhaustion — 파일 생성 불가 장애

**시나리오:**
```
Spring Batch 배치 결과 파일 / 임시 결제 영수증 파일 수백만 개 누적
→ df -h: 50% 여유 있음에도 touch 명령 실패
→ "No space left on device" (inode 고갈)
→ JVM 로그 파일 생성 불가 → 로그 유실
```

**확인:**
```bash
df -i /var/app
# Filesystem  Inodes  IUsed   IFree  IUse%
# /dev/nvme0  6553600 6553600     0   100%

# 어느 디렉토리에 소형 파일이 집중되었는지 확인
find /var/app -xdev -printf '%h\n' | sort | uniq -c | sort -rn | head -20
```

**대응:**
- 임시 파일 디렉토리 주기적 정리 CronJob 설정
- EXT4의 경우 `mkfs.ext4 -N <inode수>` 로 inode 수 사전 확대
- XFS는 동적 inode 할당 → 덜 취약

### 6-4. Heap Dump 생성으로 인한 서비스 연쇄 장애

**시나리오:**
```
결제 서버 OOM → -XX:+HeapDumpOnOutOfMemoryError 트리거
→ 8GB Heap Dump 파일 생성 시작 (STW 동반)
→ /data 파티션 Throughput 100% → 다른 서비스 I/O 대기
→ 인접 Pod의 로그 쓰기 지연 → 결제 게이트웨이 P99 급등
→ 노드 전체 I/O 포화 → 다중 서비스 동시 장애
```

**대응:**
```bash
# Heap Dump 경로를 전용 볼륨으로 분리
-XX:HeapDumpPath=/mnt/heap-dumps/

# K8s: Heap Dump용 emptyDir 전용 마운트
volumes:
  - name: heap-dumps
    emptyDir:
      sizeLimit: 20Gi
```

### 6-5. Container OverlayFS 쓰기 지연

**시나리오:**
```
K8s Pod 기동 → JVM 로그 파일 첫 쓰기
→ OverlayFS CoW: 상위 레이어에 파일 복사 후 쓰기
→ 파일 크기가 클수록 CoW 지연 증가
→ Logback 초기화 지연 → 기동 로그 유실
```

**대응:**
```yaml
# K8s: 로그 디렉토리를 emptyDir로 마운트하여 OverlayFS 우회
volumeMounts:
  - name: app-logs
    mountPath: /var/log/app
volumes:
  - name: app-logs
    emptyDir: {}
```

### 6-6. NVMe Thermal Throttling

**시나리오:**
```
결제 피크 + Heap Dump 동시 발생
→ NVMe 장시간 100% 쓰기 → 온도 상승
→ Thermal Throttling 발동 → 실효 IOPS 50% 이하로 감소
→ await 10배 증가 → 결제 시스템 전체 레이턴시 폭등
```

**확인:**
```bash
nvme smart-log /dev/nvme0 | grep -E 'temperature|throttle|warning'
# Critical Warning: 0x4 (Thermal Throttle)
```

---

## 7. 튜닝 포인트

### 7-1. Filesystem 선택 및 Mount Option

```bash
# JVM 운영 환경 권장 마운트 옵션 (EXT4)
/dev/nvme0n1p1 /var/app ext4 \
    defaults,noatime,nodiratime,data=writeback,barrier=0 0 0
#   noatime: 파일 접근 시 atime 갱신 안 함 → 읽기 시 불필요한 쓰기 제거
#   data=writeback: 메타데이터만 journaling, 데이터는 비동기 (성능↑, 충돌 시 일부 데이터 손실 가능)
#   barrier=0: write barrier 비활성화 (배터리 백업 있는 환경에서만)

# XFS 권장 마운트 옵션
/dev/nvme0n1p2 /var/log xfs \
    defaults,noatime,logbsize=256k,largeio 0 0
```

### 7-2. I/O Scheduler 튜닝

```bash
# NVMe: none (커널 병합만, 디바이스 자체 큐 활용)
echo none > /sys/block/nvme0n1/queue/scheduler

# SATA SSD: mq-deadline (읽기 우선, 기아 방지)
echo mq-deadline > /sys/block/sda/queue/scheduler

# 영구 설정 (udev rule)
echo 'ACTION=="add|change", KERNEL=="nvme[0-9]*", ATTR{queue/scheduler}="none"' \
    > /etc/udev/rules.d/60-io-scheduler.rules
```

### 7-3. Queue Depth 튜닝

```bash
# NVMe Queue Depth 확인 및 조정
cat /sys/block/nvme0n1/queue/nr_requests   # 기본 64~256
echo 1024 > /sys/block/nvme0n1/queue/nr_requests  # 고부하 환경

# Read-ahead 최적화 (순차 읽기 많은 경우)
blockdev --setra 4096 /dev/nvme0n1   # 512 byte 단위, 4096 = 2MB
```

### 7-4. Dirty Page 관련 커널 파라미터

```bash
# /etc/sysctl.conf

# 결제 서버 (낮은 레이턴시 우선)
vm.dirty_background_ratio = 5     # 기본 10: RAM의 5%에서 background writeback 시작
vm.dirty_ratio = 10               # 기본 20: RAM의 10%에서 쓰기 블로킹 (낮을수록 flush 빈번)
vm.dirty_expire_centisecs = 1000  # 기본 3000: Dirty Page 10초 후 만료 (기본 30초 → 단축)
vm.dirty_writeback_centisecs = 100 # 기본 500: writeback 스레드 1초마다 실행

# 로그 서버 (높은 처리량 우선)
vm.dirty_background_ratio = 10
vm.dirty_ratio = 30
```

> ⚠️ `dirty_ratio`를 너무 낮게 설정하면 Heap Dump 생성 시 쓰기 블로킹 빈도가 증가합니다.

### 7-5. JVM 로그 / I/O 튜닝

```xml
<!-- Logback: AsyncAppender 최적화 -->
<appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
    <queueSize>8192</queueSize>
    <discardingThreshold>0</discardingThreshold>  <!-- 감사 로그: 절대 드롭 금지 -->
    <neverBlock>true</neverBlock>                 <!-- 일반 로그: 큐 포화 시 드롭 허용 -->
    <includeCallerData>false</includeCallerData>
    <appender-ref ref="FILE"/>
</appender>

<!-- RollingFileAppender: 일별 롤링 + 용량 제한 -->
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>/var/log/app/payment-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
        <maxFileSize>500MB</maxFileSize>
        <maxHistory>7</maxHistory>
        <totalSizeCap>10GB</totalSizeCap>
    </rollingPolicy>
</appender>
```

### 7-6. JVM Heap Dump 경로 분리

```bash
# Heap Dump 전용 볼륨 (대용량 NVMe, 서비스 디스크와 분리)
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/mnt/heap-dumps/
-XX:+ExitOnOutOfMemoryError   # OOM 후 좀비 방지

# GC 로그 전용 경로
-Xlog:gc*:file=/var/log/gc/gc-%p-%t.log:time,uptime,level,tags:filecount=5,filesize=100m
```

### 7-7. K8s StorageClass / PVC 최적화

```yaml
# 고성능 StorageClass (AWS EBS gp3 예시)
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: fast-nvme
provisioner: ebs.csi.aws.com
parameters:
  type: gp3
  iops: "16000"        # gp3: 최대 16,000 IOPS
  throughput: "1000"   # MB/s
  fsType: xfs

---
# JVM 로그 + Heap Dump 분리 PVC
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: heap-dump-pvc
spec:
  storageClassName: fast-nvme
  accessModes: [ReadWriteOnce]
  resources:
    requests:
      storage: 50Gi   # -Xmx × 2 이상 확보
```

### 7-8. Direct I/O 고려 (고급)

```java
// Java에서 O_DIRECT 사용 (JNA 또는 custom JNI)
// 일반적으로 DB 엔진(RocksDB 등)에서 사용
// JVM 애플리케이션에서는 드물지만 극한 레이턴시 최적화 시 적용

// NIO FileChannel + O_DIRECT 효과 (Page Cache 우회)
// → writeback 대기 없이 NVMe에 직접 쓰기
// → Dirty Page 관련 flush 지연 제거
// → 단, JVM 버퍼 정렬 요구사항 준수 필요 (512B 또는 4K 정렬)
```

### 7-9. io_uring (JDK 미래 방향)

```
현재 상태 (2024 기준):
- JDK 19+: Project Loom Virtual Thread에서 io_uring 실험적 지원
- Netty: io_uring transport 지원 (netty-transport-classes-io_uring)
- 효과: syscall 오버헤드 감소 → 고동시성 I/O 레이턴시 개선

Netty io_uring 적용:
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-transport-classes-io_uring</artifactId>
</dependency>
```

---

## 8. 관련 Linux 명령어 및 분석 도구

### 8-1. OS 레벨 Disk 진단 명령어

| 명령어 | 주요 옵션 / 예시 | JVM 분석 포인트 |
|--------|----------------|----------------|
| `iostat -x 1` | `iostat -xz 1 \| grep -v '^$'` | `await`, `%util`, `r/s`, `w/s` 실시간 추이 |
| `iotop -oP` | `iotop -oP -d 1` | 프로세스별 I/O 사용량. JVM 프로세스 식별 |
| `vmstat 1` | `vmstat -SM 1 10` | `bi`(block in), `bo`(block out), `wa`(iowait) |
| `sar -d 1` | `sar -d 1 10` | 디바이스별 tps, 읽기/쓰기 속도 시계열 |
| `dstat -dD nvme0n1` | `dstat -dD nvme0n1 1` | 특정 디바이스 실시간 I/O |
| `lsblk -d` | `lsblk -d -o NAME,ROTA,TRAN,SIZE,SCHED` | 디스크 타입, 스케줄러 한눈에 확인 |
| `df -h && df -i` | 동시 실행 | 용량 + inode 동시 확인 (inode 고갈 탐지) |
| `du -sh` | `du -sh /var/log/app/* \| sort -rh` | 로그 디렉토리 크기 랭킹 |
| `filefrag -v` | `filefrag -v /var/log/app/payment.log` | 파일 단편화 확인 (단편 수 많으면 랜덤 I/O) |
| `smartctl -a` | `smartctl -a /dev/nvme0` | NVMe 건강 상태, wear level, 온도 |
| `nvme smart-log` | `nvme smart-log /dev/nvme0` | Thermal Throttle, 쓰기량, 수명 확인 |
| `cat /proc/diskstats` | `watch -n 1 cat /proc/diskstats` | 커널 raw I/O 통계 (iostat의 원본 데이터) |

### 8-2. 고급 I/O 분석 도구 (BCC/eBPF)

| 도구 | 사용 예시 | 분석 목적 |
|------|----------|----------|
| **biolatency** | `biolatency -D 10 1` | 블록 I/O 레이턴시 히스토그램. P99 분포 확인 |
| **biosnoop** | `biosnoop -Q` | 블록 I/O 요청별 PID, 레이턴시, 크기. JVM 프로세스 추적 |
| **bitesize** | `bitesize` | I/O 요청 크기 분포. 소형 랜덤 I/O 탐지 |
| **fileslower** | `fileslower 10` | 10ms 이상 느린 파일 I/O 실시간 표시 |
| **ext4slower** | `ext4slower 5` | EXT4 I/O 5ms 이상 느린 요청 추적 |
| **xfsslower** | `xfsslower 5` | XFS I/O 5ms 이상 느린 요청 추적 |
| **blktrace** | `blktrace -d /dev/nvme0n1 -o trace` | 블록 레이어 전체 이벤트 추적 (상세 분석) |
| **blkparse** | `blkparse -i trace` | blktrace 결과 파싱 및 분석 |
| **fio** | `fio --name=test --rw=randrw` | 디스크 성능 벤치마크. 현재 한계치 측정 |
| **perf** | `perf trace -e block:* -p <pid>` | JVM 프로세스의 블록 I/O 이벤트 추적 |
| **iowatcher** | `iowatcher -t trace.blktrace.0 -o out.svg` | blktrace 결과 시각화 |

### 8-3. JVM 전용 I/O 분석 방법

```bash
# 1. JVM 프로세스의 fd(파일 디스크립터) 확인
lsof -p <pid> | grep -E 'REG|DIR'    # 열린 파일 목록
ls -la /proc/<pid>/fd | wc -l        # 열린 fd 수
cat /proc/<pid>/limits | grep "open files"  # fd 한도

# 2. JVM 프로세스 I/O 통계
cat /proc/<pid>/io
# rchar: 읽은 총 바이트 (Page Cache 포함)
# wchar: 쓴 총 바이트 (Page Cache 포함)
# syscr: read() 시스템 콜 횟수
# syscw: write() 시스템 콜 횟수
# read_bytes: 실제 Disk에서 읽은 바이트
# write_bytes: 실제 Disk에 쓴 바이트
# cancelled_write_bytes: 취소된 쓰기 (truncate 등)

# 3. JVM I/O 관련 syscall 추적
strace -c -f -p <pid> 2>&1 | grep -E 'write|read|fsync|open'

# 4. GC 로그 파일 쓰기 속도 모니터링
watch -n 1 'ls -la /var/log/gc/*.log'
```

### 8-4. 빠른 Disk 진단 체크리스트

```bash
# === Step 1: 용량 및 inode 확인 ===
df -h && df -i

# === Step 2: 실시간 I/O 부하 ===
iostat -xz 1 5

# === Step 3: I/O 압박 지표 ===
cat /proc/pressure/io

# === Step 4: 어떤 프로세스가 I/O 유발 ===
iotop -oP -d 1 -n 5

# === Step 5: Dirty Page 상태 ===
cat /proc/meminfo | grep -E 'Dirty|Writeback'

# === Step 6: JVM 프로세스 I/O 상세 ===
PID=$(pgrep -f payment-service)
cat /proc/$PID/io

# === Step 7: 블록 레이어 레이턴시 분포 ===
biolatency 5 1

# === Step 8: 느린 파일 I/O 실시간 탐지 ===
fileslower 10

# === Step 9: NVMe 건강 상태 ===
nvme smart-log /dev/nvme0 | grep -E 'temperature|throttle|wear'

# === Step 10: 로그 디렉토리 크기 및 파일 수 ===
du -sh /var/log/app && find /var/log/app -type f | wc -l
```

### 8-5. Disk I/O 성능 기준 빠른 참조표

| 디스크 유형 | await 정상 | await 경보 | IOPS 한계 | JVM 적합 워크로드 |
|-----------|-----------|-----------|----------|-----------------|
| **NVMe PCIe 4.0** | < 0.1ms | > 1ms | ~1M IOPS | DB WAL, 실시간 감사 로그 |
| **NVMe PCIe 3.0** | < 0.2ms | > 2ms | ~500K IOPS | GC 로그, JFR, 로컬 DB |
| **SATA SSD** | < 1ms | > 5ms | ~80K IOPS | 일반 로그, 설정 파일 |
| **HDD** | < 10ms | > 30ms | ~100 IOPS | 아카이브 로그만 허용 |
| **AWS EBS gp3** | < 1ms | > 5ms | 최대 16K IOPS | K8s PVC, 운영 환경 |
| **AWS EBS io2** | < 0.5ms | > 2ms | 최대 64K IOPS | K8s 고성능 DB |

---

*JVM Runtime Disk Resource Analysis | FinTech Payment SRE*

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*