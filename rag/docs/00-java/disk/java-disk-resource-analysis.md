# Java Disk Resource Analysis
## FinTech 결제 시스템 SRE 관점 | E2E Disk I/O 분석

> 정독: 0회
> 
> 관점: SRE / Platform Engineering / Payment Reliability  
> 범위: Hardware → Block Layer → Filesystem → Page Cache → JVM Runtime → Framework → Application  
> 목적: Java 기반 결제 시스템의 Disk I/O 자원을 계층별로 분석하여 신뢰성·정합성·성능 확보

---

## 목차

1. [물리/가상 Disk 및 Storage 스펙 확인 지표](#1-물리가상-disk-및-storage-스펙-확인-지표)
2. [Java Disk E2E I/O 실행 흐름](#2-java-disk-e2e-io-실행-흐름)
3. [Java Disk 사용 메커니즘 및 I/O 실행 모델](#3-java-disk-사용-메커니즘-및-io-실행-모델)
4. [Disk 병목 및 위험 발생 지점](#4-disk-병목-및-위험-발생-지점)
5. [SRE 관점 모니터링 지표](#5-sre-관점-모니터링-지표)
6. [장애 시나리오](#6-장애-시나리오)
7. [튜닝 포인트](#7-튜닝-포인트)
8. [관련 Linux 명령어 및 분석 도구](#8-관련-linux-명령어-및-분석-도구)

---

## 1. 물리/가상 Disk 및 Storage 스펙 확인 지표

### 1.1 디스크 타입별 성능 특성

Java 결제 시스템에서 디스크 타입은 로그 기록, DB WAL, Heap Dump 생성, GC 로그 등 I/O 패턴과 직결된다.

```text
디스크 타입 성능 비교:
  HDD (SATA):
    IOPS: 약 100~200 (랜덤 4K)
    Latency: 5~10ms
    용도: 아카이브, 저빈도 접근 로그
    결제 시스템: 부적합 (WAL, 트랜잭션 로그용)

  SATA SSD:
    IOPS: 약 10,000~100,000
    Latency: 0.1~0.5ms
    용도: DB 데이터 파일, 로그
    결제 시스템: 중간 부하 환경에 적합

  NVMe SSD (PCIe Gen 4):
    IOPS: 약 500,000~1,000,000+
    Latency: 0.02~0.1ms
    Bandwidth: 최대 7GB/s
    용도: DB WAL, Kafka 브로커, 고처리량 결제 로그
    결제 시스템: 고빈도 트랜잭션 환경 필수
```

| 확인 항목 | 명령어 | SRE 분석 이유 |
|---------|-------|------------|
| 디스크 타입/회전여부 | `lsblk -d -o name,rota,tran,size` | ROTA=0(SSD), ROTA=1(HDD). NVMe vs SATA 구분 |
| I/O Scheduler | `cat /sys/block/<dev>/queue/scheduler` | NVMe: none 또는 mq-deadline 권장 |
| Queue Depth | `cat /sys/block/<dev>/queue/nr_requests` | NVMe는 높은 Queue Depth에서 병렬 처리 효율 증가 |
| SMART 상태 | `smartctl -a /dev/<dev>` | 디스크 건강 지표, Reallocated Sectors 증가 시 교체 |
| NVMe 상태 | `nvme smart-log /dev/nvme0` | 온도, Critical Warning, Media Error 확인 |
| 파일시스템 마운트 | `findmnt -o target,fstype,options` | noatime, nodiratime, data=ordered 등 성능 옵션 확인 |

---

### 1.2 RAID / SAN / Distributed Storage 구조

```text
결제 시스템 스토리지 구성:

로컬 NVMe (고성능 우선):
  단일 NVMe → 최저 Latency, SPOF 위험
  NVMe RAID-10 → 성능 + 이중화, DB WAL 분리 구성 가능

SAN (Storage Area Network):
  Fiber Channel / iSCSI → 네트워크 Latency 추가 (0.1~1ms)
  결제 시스템: Network Latency가 DB 쓰기 Latency에 직접 반영
  → 전용 SAN 패브릭 + Jumbo Frame(9000 MTU) 권장

Distributed Storage (Kubernetes PVC 기반):
  Ceph RBD / Longhorn / OpenEBS
  → 복제 인수(replication factor)에 따라 쓰기 증폭 발생
  → 결제 DB용 PVC: StorageClass에 WaitForFirstConsumer + IOPS Limit 명시

Kubernetes CSI 구조:
  StorageClass → PVC → PV → CSI Driver → 실제 볼륨
  → ephemeral-storage: 컨테이너 로컬 디스크 (노드 디스크 공유, 경쟁 위험)
  → PVC: 결제 DB, WAL 파일 등 영구 데이터 분리 저장 필수
```

---

### 1.3 Filesystem 구조

| 파일시스템 | 특성 | 결제 시스템 적합성 |
|---------|-----|---------------|
| XFS | 대형 파일 고성능, 메타데이터 성능 우수, 저널링 지원 | 결제 로그, DB 데이터 파일에 적합 |
| EXT4 | 범용, 안정적, ordered 저널링 기본 | 일반 용도, 소규모 결제 환경 |
| ZFS | CoW, 스냅샷, 데이터 무결성 검증 | 데이터 정합성 중요 환경, 운영 복잡도 주의 |
| tmpfs | RAM 기반 임시 파일시스템 | JVM 임시 파일, 소켓 파일 (휘발성) |

```bash
# XFS 권장 마운트 옵션 (결제 서버)
/dev/nvme0n1p1 /data xfs defaults,noatime,nodiratime,logbsize=256k,allocsize=1m 0 0

# EXT4 권장 옵션
/dev/sdb1 /data ext4 defaults,noatime,data=ordered,barrier=1 0 0
```

---

### 1.4 Block Device / blk-mq / Queue Depth 구조

```text
Linux Block Layer 구조:
  Application (Java JVM)
    ↓ syscall (read/write/fsync)
  VFS (Virtual Filesystem)
    ↓
  Filesystem (XFS/EXT4)
    ↓
  Page Cache
    ↓ (Cache Miss 또는 Direct I/O)
  Block Layer
    ├── bio 구조체 생성 (Block I/O 요청)
    ├── I/O Scheduler (mq-deadline, kyber, none)
    └── blk-mq (Multi-Queue Block I/O)
          → CPU별 Hardware Queue 분산
          → NVMe: HW Queue 수 = CPU 수 (완전 병렬)
    ↓
  NVMe / SCSI Driver
    ↓
  DMA: 물리 디스크로 데이터 전송 (CPU 개입 없음)
    ↓
  IRQ / MSI-X: 완료 통지 → softirq → I/O 완료 처리
```

---

## 2. Java Disk E2E I/O 실행 흐름

### 2.1 결제 트랜잭션의 전체 Disk I/O 경로

```text
[결제 승인 요청 처리]
  ↓
Controller / Service Layer
  ↓
Repository (Spring Data JPA / MyBatis)
  ↓ JDBC PreparedStatement.executeUpdate()
  ↓
HikariCP → DB Connection → TCP Socket → DB Server
  ↓ (DB Server 내부)
  DB Server Application (MySQL/PostgreSQL)
    → 쿼리 파싱 → 실행 계획
    → Buffer Pool (InnoDB) 또는 Shared Buffer (PostgreSQL)
      Hit: 메모리에서 처리 (I/O 없음)
      Miss: Disk Read (B-Tree 탐색 → Page I/O)
    → WAL/Redo Log 기록 (fsync 또는 O_DSYNC)
      → JVM 관점: DB Server의 디스크 I/O를 TCP Latency로 경험
    → Commit: fsync 완료 후 Ack
  ↓ DB 응답 수신 (TCP)
JVM: ResultSet 파싱 → Java 객체 생성 (Heap)
  ↓
[결제 완료 — JVM 직접 Disk I/O 영역]
  ↓
로그 기록 (Logback / Log4j2)
  → AsyncAppender → ArrayBlockingQueue → FileAppender
  → BufferedOutputStream → write() syscall
  → Kernel Page Cache (Dirty Page)
  → 비동기 pdflush/kworker → 물리 디스크 기록
  ↓
Heap Dump / GC Log (장애 시)
  → -XX:+HeapDumpOnOutOfMemoryError → 수 GB .hprof 파일 → 디스크 풀 위험
  → -Xlog:gc*:file=/log/gc.log → 지속적 GC 로그 쓰기
  ↓
JVM Crash Log / hs_err_pid.log
  → JVM Crash 시 자동 생성
```

---

### 2.2 계층별 I/O 흐름 상세

```text
Application Layer (Java):
  java.io.FileOutputStream.write()
  java.nio.FileChannel.write()
  java.nio.MappedByteBuffer (mmap)
    ↓ JNI → Native Method

JVM / JNI Layer:
  write() syscall → User Space → Kernel Space 전환
  read() syscall → Kernel Space → User Space 복사

OS / VFS Layer:
  VFS (가상 파일시스템): 파일시스템 추상화
  → inode 조회 → Dentry 캐시 → 실제 파일시스템 디스패치
  → Page Cache 조회 (Hit: 즉시 반환, Miss: Block I/O 요청)

Page Cache Layer:
  Dirty Page: 메모리에 기록, 아직 디스크 미반영
  Clean Page: 디스크와 동기화된 상태
  pdflush / kworker: Dirty Page를 비동기적으로 디스크에 기록
  → vm.dirty_background_ratio 초과 시 백그라운드 플러시 시작
  → vm.dirty_ratio 초과 시 write() 호출 Thread 블로킹 (Write Stall)

Block Layer:
  bio 구조체: Block I/O 요청 단위
  blk-mq: Multi-Queue 기반 I/O 스케줄링
  → CPU별 Software Queue → Hardware Queue → 드라이버
  DMA: CPU 개입 없이 메모리 ↔ 디스크 직접 전송

Interrupt Layer:
  I/O 완료 시 NVMe MSI-X → softirq → I/O 완료 콜백
  → 대기 Thread 깨움 (Wait Queue → Runqueue)
```

---

## 3. Java Disk 사용 메커니즘 및 I/O 실행 모델

### 3.1 Java I/O API와 커널 I/O 매핑

```text
java.io (전통적 Blocking I/O):
  FileInputStream / FileOutputStream
    → BufferedInputStream / BufferedOutputStream (8KB 기본 버퍼)
    → 내부적으로 read() / write() syscall
    → 버퍼 미사용 시: 1 byte 쓰기 = 1 syscall → Disk I/O 폭발적 증가

java.nio (Non-blocking / Direct I/O):
  FileChannel.read() / write()
    → 내부적으로 pread64() / pwrite64() syscall
    → ByteBuffer 방향에 따라 Heap Buffer 또는 Direct Buffer 사용
    → Direct Buffer: JVM Heap 복사 없이 Kernel 버퍼로 직접 전달

MappedByteBuffer (Memory-Mapped I/O):
  FileChannel.map() → mmap() syscall
    → 파일 내용이 가상 주소 공간에 매핑
    → 파일 접근 = 메모리 접근 (Page Fault 시 Disk I/O)
    → 대형 로그 파일 분석, Kafka 로그 세그먼트에서 활용
    → 주의: MappedByteBuffer 언매핑 어려움, GC와 연동 복잡

FileChannel.force() / fsync:
  force(true) → fsync() syscall → Disk에 데이터+메타데이터 강제 기록
  force(false) → fdatasync() syscall → 데이터만 강제 기록 (메타데이터 제외)
  → 결제 감사 로그, WAL 파일에서 데이터 정합성 보장 필수
  → fsync Latency = 디스크 응답 시간 (NVMe: ~0.02ms, HDD: ~5ms)
```

---

### 3.2 Page Cache와 Dirty Page 메커니즘

```text
Java write() → Page Cache → 물리 디스크 흐름:

write() syscall
  → Kernel Page Cache (Dirty Page 마킹)
  → write() 즉시 반환 (JVM Thread 미블로킹)
  → kworker/pdflush 백그라운드 플러시:
      vm.dirty_background_ratio (기본 10%): 전체 메모리의 10% 초과 시 플러시 시작
      vm.dirty_expire_centisecs (기본 3000 = 30초): Dirty Page 최대 보관 시간

Dirty Page Write Stall (Write Throttle):
  vm.dirty_ratio (기본 20%) 초과 시:
    → write() syscall이 블로킹됨 (JVM Thread 정지)
    → 결제 처리 Thread가 로그 기록에서 막힘
    → P99 Latency 급증

결제 시스템 영향:
  Logback FileAppender (동기 모드)가 Write Stall 구간과 겹치면
    → 결제 처리 Thread가 로그 쓰기에서 수십 ms 정지
    → AsyncAppender로 분리해야 결제 로직과 로그 I/O 격리 가능
```

---

### 3.3 로그 프레임워크 I/O 모델 (결제 시스템 핵심)

```text
Logback FileAppender (동기 모드):
  결제 처리 Thread → log.info() → FileAppender.append()
    → OutputStreamWriter.write() → BufferedOutputStream (8KB)
    → 버퍼 가득 참 → write() syscall → Kernel Page Cache
  문제: 결제 처리 Thread가 I/O 완료를 기다림
       → Disk 부하 시 결제 P99 Latency 직접 영향

Logback AsyncAppender (권장):
  결제 처리 Thread → log.info() → ArrayBlockingQueue (256 기본)
    → 즉시 반환 (Thread 미블로킹)
    → 별도 Worker Thread: Queue → FileAppender → write()
  주의: Queue 고갈 시 기본 설정에서 이벤트 Drop (20% 남을 때부터)
        → neverBlock=false (기본): 큐 풀 시 Thread 블로킹 (결제 Thread 영향)
        → neverBlock=true: 큐 풀 시 로그 Drop (결제 처리 Thread 보호 우선)

Log4j2 AsyncLogger (LMAX Disruptor):
  Ring Buffer 기반 → Lock-free → 가장 높은 로그 처리량
  → 결제 고처리량 환경(수천 TPS)에서 Logback AsyncAppender 대비 유리
  → 설정: log4j2.asyncLoggerRingBufferSize=262144 (512K 이벤트)

결제 시스템 권장 로그 I/O 전략:
  1. AsyncAppender 또는 AsyncLogger 필수
  2. 결제 감사 로그(Audit Log): 별도 파일 + fdatasync 주기 설정
  3. GC 로그: 별도 볼륨 분리 (결제 로그 볼륨과 경쟁 방지)
  4. Heap Dump: 별도 볼륨 지정 (-XX:HeapDumpPath=/dump/volume)
```

---

### 3.4 JVM Heap Dump / GC Log I/O 특성

```text
Heap Dump (-XX:+HeapDumpOnOutOfMemoryError):
  OOM 발생 → JVM이 Heap 전체를 .hprof 파일로 덤프
  → Heap 4GB → .hprof 약 4GB+ 파일
  → 쓰기 속도: Disk Write Throughput에 종속
     NVMe: ~수십 초, HDD: ~수 분
  → Disk 여유 공간이 Heap 크기 이상 필요
  → 결제 서버 디스크 풀 → 덤프 실패 → 장애 원인 분석 불가

  대응:
    HeapDump 전용 볼륨 마운트
    -XX:HeapDumpPath=/mnt/heapdump/
    사후 자동 S3 업로드 + 로컬 파일 삭제 스크립트

GC 로그 (-Xlog:gc*:file=/log/gc.log:time,level,tags:filecount=10,filesize=50m):
  filecount + filesize 로 로테이션 설정 필수
  → 미설정 시 GC 로그 파일이 무제한 증가 → Disk Full
  → 결제 처리 로그와 같은 볼륨 공유 시 결제 로그 기록 실패

JIT Compilation Log (-XX:+PrintCompilation):
  운영 환경 비활성화 권장 (대량 로그 → I/O 부하)
  → 진단 필요 시 단기간만 활성화 후 비활성화

hs_err_pid<PID>.log (JVM Crash Log):
  JVM Crash 시 JVM 현재 디렉토리에 생성
  → 결제 서버 JVM이 /app 디렉토리에서 실행 시 같은 위치에 생성
  → 쓰기 권한 및 디스크 여유 공간 확인 필요
```

---

### 3.5 Sequential / Random I/O 패턴

```text
Java 결제 시스템 I/O 패턴 분류:

Sequential I/O (순차):
  - 결제 로그 기록 (FileAppender): 파일 끝에 append → 순차 쓰기
  - GC 로그 쓰기: 순차 쓰기
  - Heap Dump: 대용량 순차 쓰기
  - Kafka 로그 세그먼트 기록: 순차 쓰기 (Producer)
  → Throughput(MB/s) 중요, NVMe/SSD에서 최대 활용

Random I/O (랜덤):
  - DB 인덱스 B-Tree 탐색: 랜덤 읽기 (Page 단위)
  - JPA/Hibernate 지연 로딩: 랜덤 DB 읽기
  - Kafka Consumer 오프셋 조회: 랜덤 읽기
  → IOPS 중요, HDD에서 심각한 병목
  → NVMe로도 Random IOPS 포화 가능 (결제 TPS 극대화 시)

Write Amplification (쓰기 증폭):
  SSD/NVMe에서 발생
  → Filesystem 블록 크기 < SSD 내부 Page 크기
  → 작은 쓰기가 큰 단위 지우기/재쓰기를 유발
  → 결제 감사 로그: 작은 레코드 반복 쓰기 → Write Amplification 주의
  → 대응: Filesystem 블록 크기와 SSD Page 크기 정렬, 배치 쓰기
```

---

### 3.6 mmap과 Kafka / Java NIO 파일 처리

```text
Kafka Consumer (결제 후처리):
  LogSegment 파일 → MappedByteBuffer(mmap)로 접근
  → 파일 내용이 가상 주소 공간에 매핑
  → Broker의 Page Cache에서 Consumer에게 Zero-copy 전달
     (sendfile() 또는 transferTo())
  → Consumer JVM: TCP 수신 → Kernel 버퍼 → User Space 복사 (최소 1회)

Java NIO FileChannel.transferTo():
  sendfile() syscall 기반 Zero-copy
  → 파일 데이터를 User Space 복사 없이 Socket으로 직접 전달
  → 결제 첨부파일, 영수증 파일 전송에 활용 가능

Direct I/O (O_DIRECT):
  Java에서 직접 지원 없음 (JNA/JNI 필요)
  → Page Cache 우회 → 물리 디스크 직접 읽기/쓰기
  → DB 엔진(MySQL, PostgreSQL)이 Buffer Pool 직접 관리 시 사용
  → Java 애플리케이션에서는 일반적으로 사용 안 함
  → 결제 DB의 DB Server 설정에서 innodb_flush_method=O_DIRECT 검토
```

---

## 4. Disk 병목 및 위험 발생 지점

### 4.1 IOPS Saturation

```text
발생 경로:
  결제 TPS 증가 → 결제 로그 + DB WAL + Kafka 기록 동시 증가
    → 단일 디스크 IOPS 한계 도달
    → I/O 요청이 Disk Queue에 누적
    → await 급증 (수 ms → 수십 ms)
    → 결제 처리 Thread가 I/O 완료를 기다리며 Block
    → Thread Pool 고갈 → 결제 실패

IOPS 포화 징후:
  iostat -xz: %util 90% 이상 지속
  iostat -xz: await 급증 (NVMe: 0.1ms → 5ms+, HDD: 5ms → 50ms+)
  iostat -xz: aqu-sz (평균 큐 길이) 증가

결제 영향:
  IOPS 포화 → Disk I/O Latency 급증 → DB 응답 지연 → 결제 Timeout
  IOPS 포화 → Kafka WAL 기록 지연 → Producer acks 대기 → 결제 이벤트 발행 지연
```

---

### 4.2 Disk Queue Saturation과 High Await

```text
Disk Queue (I/O Queue Depth):
  blk-mq Software Queue → Hardware Queue → 물리 디스크 컨트롤러
  NVMe 기본 Queue Depth: 1024 (매우 높음)
  SATA SSD 기본: 32

Queue Saturation 발생:
  I/O 요청 속도 > 디스크 처리 속도
  → Software Queue에 bio 구조체 누적
  → await 증가: 디스크 처리 시간(svctm) + 큐 대기 시간
  → 실질 디스크 처리 시간(svctm)은 짧아도 큐 대기로 Latency 급증

결제 영향:
  GC 로그 + 결제 로그 + DB I/O 동시 폭주 시
    → Disk Queue 포화 → 모든 I/O Latency 동반 증가
    → GC 관련 파일 쓰기가 결제 DB I/O를 지연시키는 I/O 경쟁 발생
```

---

### 4.3 fsync Stall (결제 시스템 치명적)

```text
fsync() / fdatasync() 동작:
  Kernel Page Cache의 Dirty Page를 물리 디스크에 강제 기록 후 반환
  → Disk Latency에 직접 종속: NVMe ~0.02ms, HDD ~5~10ms

fsync Stall 발생 경로:
  DB Server (MySQL InnoDB):
    innodb_flush_log_at_trx_commit=1 (기본): Commit마다 WAL fsync
    → WAL 파일 fsync Latency = 결제 Commit Latency
    → NVMe 사용 시 ~0.05ms, HDD 사용 시 ~5ms (100배 차이)

  결제 감사 로그 (Audit Log):
    감사 로그에 fsync 적용 시 로그 기록마다 Disk 동기화 필요
    → 결제 처리 Thread가 fsync 완료를 기다림
    → 로그 기록 Thread 분리 + 배치 fsync로 완화 가능

fsync Storm:
  GC Full GC 이후 대량 메모리 해제 → Page Cache 재배치
  → Dirty Page 대량 flush 발생 → fsync 경쟁 → 전체 I/O Latency 상승
```

---

### 4.4 Dirty Page Flush Delay와 Write Stall

```text
Dirty Page 누적 → Write Stall 전파:

  vm.dirty_background_ratio = 10 (기본)
    → 메모리의 10% 이상 Dirty Page 시 kworker 백그라운드 플러시 시작
    → 결제 로그 write() 속도 > Disk 처리 속도이면 누적

  vm.dirty_ratio = 20 (기본)
    → 메모리의 20% 이상 Dirty Page 시 write() 호출 Thread 블로킹
    → 결제 처리 Thread가 로그 write()에서 블로킹 → Latency 급증
    → I/O Wait % 급증 (CPU %iowait)

결제 시스템 조정 방향:
  결제 서버 (낮은 Latency 우선):
    vm.dirty_background_ratio=3 → 더 자주 플러시 (Latency 안정)
    vm.dirty_ratio=10 → Write Stall 임계치 낮춤
  로그 서버 (높은 Throughput 우선):
    vm.dirty_background_ratio=15 → 배치 플러시 (Throughput 극대화)
```

---

### 4.5 Journal Contention (EXT4/XFS)

```text
Journaling 파일시스템:
  EXT4 ordered 모드: 데이터 기록 후 메타데이터 저널 기록
  EXT4 writeback 모드: 메타데이터 저널 독립 기록 (빠르지만 복구 위험)
  XFS: 메타데이터 전용 저널, 데이터는 직접 기록

Journal Contention 발생:
  결제 로그 파일 생성/삭제 반복 (로테이션)
    → inode 할당/해제 → 저널 기록 경쟁
    → 다른 파일 I/O Latency 증가

결제 영향:
  로그 로테이션이 빈번한 시스템에서 저널 경쟁으로 DB 파일 I/O 지연 가능
  → 결제 로그와 DB 데이터를 별도 볼륨(별도 파티션)에 분리 필수
```

---

### 4.6 Heap Dump로 인한 Disk Full (결제 시스템 치명적)

```text
발생 시나리오:
  결제 서버 OOM → -XX:+HeapDumpOnOutOfMemoryError 트리거
    → Heap 8GB → .hprof 파일 생성 시작
    → Disk 여유 공간 8GB 미만 → 덤프 미완성 or Disk Full
    → Disk Full → 결제 로그 기록 실패 → 감사 로그 유실
    → 다른 프로세스 write() 실패 (DB WAL 기록 실패)
    → DB 데이터 손상 위험

대응:
  HeapDump 전용 별도 볼륨 (최소 Heap 크기 × 2 이상)
  덤프 완료 후 자동 압축 + 원격 전송 + 로컬 삭제 스크립트
  Alert: 덤프 볼륨 사용률 > 60%
```

---

## 5. SRE 관점 모니터링 지표

### 5.1 Disk I/O 핵심 지표

| 지표 | 설명 | 임계 기준 | 확인 방법 |
|------|------|---------|---------|
| `%util` | 디스크 포화도 (1초 중 I/O 처리 비율) | 90% 이상 지속 시 포화 | `iostat -xz 1` |
| `await` | I/O 요청 평균 대기 시간 (ms) | NVMe: 1ms 초과, SSD: 5ms 초과 시 조사 | `iostat -xz 1` |
| `svctm` | 실제 디스크 서비스 시간 (ms) | await-svctm 큰 경우 Queue 대기 병목 | `iostat -xz 1` |
| `aqu-sz` | 평균 I/O 큐 길이 | 1.0 이상 지속 시 포화 의심 | `iostat -xz 1` |
| `r/s`, `w/s` | 초당 읽기/쓰기 IOPS | 디스크 스펙 IOPS의 80% 초과 시 경고 | `iostat -xz 1` |
| `rkB/s`, `wkB/s` | 읽기/쓰기 처리량 (KB/s) | 디스크 스펙 Throughput의 80% 초과 | `iostat -xz 1` |

---

### 5.2 Filesystem / Page Cache 지표

| 지표 | 설명 | 임계 기준 | 확인 방법 |
|------|------|---------|---------|
| Dirty Page | 디스크 미기록 메모리 데이터 | vm.dirty_ratio 50% 초과 지속 시 조사 | `/proc/meminfo Dirty` |
| Filesystem 사용률 | 볼륨 사용량 | 80% 초과 시 경고, 90% 초과 시 즉시 조치 | `df -h` |
| Inode 사용률 | 파일 개수 한계 | 80% 초과 시 경고 | `df -i` |
| Page Cache Hit Rate | Page Cache에서 읽기 성공률 | 낮으면 Disk Read 증가 → IOPS 소비 | `sar -B 1` pgpgin/pgpgout |
| IO Wait (`%iowait`) | I/O 완료 대기로 CPU 유휴 비율 | 5% 이상 지속 시 조사 | `iostat 1`, `mpstat` |
| PSI IO | I/O 압박으로 인한 Task 정지 비율 | some > 5% 지속 시 조사 | `/proc/pressure/io` |

---

### 5.3 Java / JVM 특화 Disk 지표

| 지표 | 설명 | 확인 방법 |
|------|------|---------|
| GC 로그 파일 크기 | GC 로그 파일 증가율 | `du -sh /log/gc.log*`, 자동 로테이션 설정 확인 |
| Heap Dump 볼륨 여유 공간 | OOM 시 덤프 가능 여부 | `df -h /mnt/heapdump` |
| 결제 로그 쓰기 속도 | AsyncAppender Queue 상태 | JMX: `ch.qos.logback.core:type=default,name=appender` |
| Logback AsyncAppender 큐 사용량 | 큐 풀 발생 여부 | `-Dlogback.debug=true` 또는 JMX |
| FileChannel.force() Latency | fsync 호출 소요 시간 | JFR `FileWrite` + `FileForce` 이벤트 |
| Socket Write / File Write | TCP + 파일 I/O Latency | JFR `SocketWrite`, `FileWrite` 이벤트 |

---

### 5.4 Kafka / DB 관련 Disk 지표

| 지표 | 설명 | 임계 기준 |
|------|------|---------|
| Kafka Log Segment 크기 | 브로커 디스크 사용량 | `kafka_log_log_size` 증가 추이 |
| Kafka ISR 동기화 지연 | Follower 복제 지연 | Replica Lag 지속 증가 시 Disk I/O 병목 의심 |
| DB WAL 플러시 Latency | Commit Latency 직결 | MySQL: `innodb_os_log_fsyncs`, PostgreSQL: `wal_write_time` |
| DB Buffer Pool Hit Rate | Disk Read 빈도 | InnoDB Buffer Pool Hit < 95% 시 Disk IOPS 증가 |
| DB Temp File 사용량 | Sort/Hash Join 임시 파일 | MySQL: `Created_tmp_disk_tables`, PostgreSQL: `temp_files` |

---

## 6. 장애 시나리오

### 6.1 Disk Full (결제 로그 볼륨)

```text
시나리오:
  결제 트래픽 급증 → 결제 로그 대량 생성
    → Logback 로그 로테이션 미설정 또는 로테이션 실패
    → /var/log 볼륨 100% 도달
    → FileAppender write() 실패 → IOException
    → 감사 로그(Audit Log) 기록 실패
    → 금융 감사 요건 위반

결제 영향:
  결제 처리 자체는 지속될 수 있으나 감사 로그 유실
  → 컴플라이언스 위반, 장애 후 원인 분석 불가
  → 같은 볼륨에 GC 로그, Heap Dump가 있으면 JVM도 영향

분석:
  df -h → 100% 볼륨 확인
  du -sh /var/log/* | sort -rh | head -20 → 대용량 파일 탐색
  find /var/log -name "*.log" -size +100M → 대형 로그 파일 탐색

대응:
  즉시: 오래된 로그 파일 압축/삭제
  근본: 볼륨 분리 (결제 로그, GC 로그, HeapDump 각각 별도 볼륨)
  예방: Logback maxFileSize + maxHistory + totalSizeCap 설정
        Prometheus Disk 사용률 Alert (80% 경고, 90% 긴급)
```

---

### 6.2 inode Exhaustion

```text
시나리오:
  결제 처리 중 임시 파일 대량 생성 (예: 배치 처리 중간 결과, JAR explode)
    → 파일 개수 제한(inode) 도달
    → 용량은 여유 있으나 새 파일 생성 불가
    → Java: IOException: No space left on device (inode 고갈 메시지)
    → 결제 처리 중 임시 파일 생성 실패 → 처리 오류

분석:
  df -i → Inode 사용률 확인 (Use% 100%이면 고갈)
  find / -xdev -printf '%h\n' | sort | uniq -c | sort -k1 -rn | head → 파일 밀집 디렉토리
  find /tmp -name "*.tmp" -mmin +60 | wc -l → 오래된 임시 파일 수

대응:
  /tmp 정리: find /tmp -name "*.tmp" -mmin +60 -delete
  Java 임시 파일 관리: Files.createTempFile() 사용 후 반드시 deleteOnExit() 또는 명시적 삭제
  XFS 사용 시 inode 자동 확장 옵션 활용 (inode64 마운트 옵션)
```

---

### 6.3 fsync Latency Explosion (결제 Commit 지연)

```text
시나리오:
  결제 DB 서버의 NVMe 디스크가 과부하
    → WAL fsync Latency: 정상 0.05ms → 장애 시 50ms+
    → MySQL InnoDB: Commit마다 WAL fsync 대기
    → 결제 Commit Latency: 0.1ms → 100ms+ (1000배 증가)
    → 결제 API Timeout (외부 PG 연동 5초 이내 응답 요건 위반)

원인:
  NVMe Thermal Throttling (과열로 성능 저하)
  다른 I/O(결제 로그 쓰기, Heap Dump)와 경쟁
  SAN Storage 네트워크 혼잡

분석:
  iostat -xz 1: await 급증 확인
  nvme smart-log: Temperature, Percentage Used, Media Errors
  biolatency (eBPF): Block I/O Latency 분포 히스토그램
  JFR SocketWrite: DB 응답 대기 시간 측정

대응:
  단기: innodb_flush_log_at_trx_commit=2 (1초마다 fsync, 1초치 트랜잭션 유실 위험)
  근본: WAL 전용 NVMe 볼륨 분리, NVMe 냉각 개선
  Kafka 유사 패턴: log.flush.interval.messages=1 → 성능 저하, 완화 필요
```

---

### 6.4 Heap Dump 생성 중 Disk Full → 데이터 손상

```text
시나리오:
  결제 서버 OOM 발생 → Heap Dump 시작
    → Heap 8GB 덤프 중 디스크 잔여 5GB
    → 덤프 중단 → 불완전 .hprof 파일
    → 같은 볼륨에 DB WAL 파일 → WAL 쓰기 실패
    → DB 크래시 또는 트랜잭션 유실

결제 영향:
  진행 중 결제 트랜잭션 손실 (DB WAL 기록 실패)
  결제 데이터 정합성 훼손 위험
  장애 원인 분석 불가 (덤프 미완성)

대응:
  HeapDump 전용 볼륨 (최소 max(Heap) × 2 이상 여유)
  DB WAL, HeapDump, 결제 로그 완전 분리 3볼륨 구성
  OOM Alert → HeapDump 볼륨 여유 공간 자동 확인 후 덤프 활성화
```

---

### 6.5 AsyncAppender Queue 포화 → 로그 Drop

```text
시나리오:
  결제 트래픽 급증 → 초당 로그 수 증가
    → Logback AsyncAppender Queue(기본 256) 포화
    → neverBlock=false(기본): 결제 처리 Thread가 로그 기록에서 블로킹
    → 결제 P99 Latency 증가
    또는
    → neverBlock=true: 로그 Drop (결제 감사 로그 유실 위험)

분석:
  JMX: AsyncAppender QueueSize / DiscardedCount
  결제 로그 타임라인 gap (특정 시간대 로그 공백)
  Disk await 증가 → AsyncAppender Worker Thread가 write()에서 블로킹

대응:
  AsyncAppender queueSize 증가 (256 → 8192)
  Log4j2 AsyncLogger (LMAX Disruptor) 전환 → Lock-free, 더 높은 처리량
  결제 감사 로그: 별도 Appender + 전용 볼륨 + 절대 Drop 금지 설정
  일반 로그: 일부 Drop 허용 (neverBlock=true)으로 결제 Thread 보호
```

---

### 6.6 Container Volume Saturation (Kubernetes)

```text
시나리오:
  결제 서비스 Pod의 PVC 용량 초과
    → 결제 로그 파일 생성 실패
    → Persistent Volume의 IOPS 한계 도달
    → StorageClass IOPS 제한 (예: AWS gp2: 3 IOPS/GB)
    → 결제 DB Pod와 같은 노드의 로컬 디스크 경쟁

분석:
  kubectl describe pvc → Capacity, Access Mode, StorageClass
  kubectl top pod → I/O 사용량 (cadvisor 지원 시)
  /sys/fs/cgroup/io.stat → 컨테이너별 I/O 통계
  node-problem-detector: 노드 Disk Pressure 이벤트

대응:
  결제 DB PVC: StorageClass io1/io2 (IOPS 고정형) 사용
  StorageClass에 allowVolumeExpansion: true 설정 (온라인 확장 지원)
  로그 볼륨과 DB 볼륨 분리된 StorageClass 사용
  결제 DB: ephemeral-storage 사용 금지 (노드 재시작 시 유실)
```

---

## 7. 튜닝 포인트

### 7.1 Filesystem 선택 및 마운트 옵션

```bash
# 결제 로그 볼륨 (XFS 권장, 순차 쓰기 최적화)
/dev/nvme1n1 /payment-logs xfs \
  defaults,noatime,nodiratime,logbsize=256k,allocsize=4m \
  0 0

# DB 데이터 볼륨 (XFS, DB 직접 I/O 활용)
/dev/nvme2n1 /data/mysql xfs \
  defaults,noatime,nodiratime,logbsize=256k \
  0 0

# HeapDump 전용 볼륨 (EXT4도 가능, 순차 대용량 쓰기)
/dev/sdb1 /heapdump ext4 \
  defaults,noatime,data=writeback \
  0 0

# 핵심 마운트 옵션:
# noatime: 파일 접근 시간 기록 비활성화 → Read I/O 시 불필요한 쓰기 제거
# nodiratime: 디렉토리 접근 시간 기록 비활성화
# logbsize=256k: XFS 저널 버퍼 크기 증가 → 쓰기 성능 향상
# data=writeback: EXT4에서 데이터 저널링 비활성화 → 성능 향상 (DB 서버 자체 WAL 보유 시)
# data=ordered: EXT4 기본, 데이터 무결성 + 성능 균형
```

---

### 7.2 I/O Scheduler

```bash
# NVMe (권장: none 또는 mq-deadline)
echo none > /sys/block/nvme0n1/queue/scheduler
# 이유: NVMe는 자체 병렬 큐 보유, OS 스케줄러 오버헤드 제거

# SSD (권장: mq-deadline)
echo mq-deadline > /sys/block/sdb/queue/scheduler
# 이유: 읽기 Latency 우선, 쓰기 배치 처리

# 영구 설정 (udev rules)
echo 'ACTION=="add|change", KERNEL=="nvme[0-9]*", ATTR{queue/scheduler}="none"' \
  > /etc/udev/rules.d/60-scheduler.rules

# Queue Depth (NVMe)
echo 1024 > /sys/block/nvme0n1/queue/nr_requests  # 병렬 처리 최대화
```

---

### 7.3 Dirty Page 설정 (결제 서버 최적화)

```bash
# /etc/sysctl.conf 또는 /etc/sysctl.d/99-payment.conf

# 결제 서버 (낮은 Write Latency 우선)
vm.dirty_background_ratio=3    # 3%에서 백그라운드 플러시 시작 (더 자주 플러시)
vm.dirty_ratio=10              # 10%에서 Write Stall 트리거 (임계치 낮춤)
vm.dirty_expire_centisecs=1000 # 10초마다 강제 플러시 (기본 30초보다 빈번)
vm.dirty_writeback_centisecs=200 # 2초마다 플러시 확인

# Swap 억제
vm.swappiness=1

# 적용
sysctl -p /etc/sysctl.d/99-payment.conf
```

---

### 7.4 JVM 로그 / 덤프 I/O 설정

```bash
# GC 로그 설정 (로테이션 필수)
-Xlog:gc*:file=/log/gc/gc.log:time,level,tags:filecount=10,filesize=50m

# Heap Dump 설정 (전용 볼륨)
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/mnt/heapdump/

# JVM Crash Log 위치 지정
-XX:ErrorFile=/log/crash/hs_err_pid%p.log

# Thread Dump (운영 환경, jcmd 활용)
# jcmd <pid> Thread.print > /tmp/threaddump-$(date +%Y%m%d%H%M%S).txt

# JFR 연속 기록 (운영 환경 1~2% 오버헤드)
-XX:StartFlightRecording=disk=true,maxsize=1g,maxage=1d,\
  dumponexit=true,filename=/log/jfr/recording.jfr
```

---

### 7.5 Logback I/O 최적화

```xml
<!-- logback-spring.xml -->

<!-- 결제 로그 AsyncAppender (로그 Drop 불허) -->
<appender name="ASYNC_PAYMENT" class="ch.qos.logback.classic.AsyncAppender">
  <appender-ref ref="PAYMENT_FILE"/>
  <queueSize>8192</queueSize>
  <neverBlock>false</neverBlock>      <!-- 결제 감사 로그: Drop 금지 -->
  <discardingThreshold>0</discardingThreshold>
</appender>

<!-- 일반 로그 AsyncAppender (Drop 허용) -->
<appender name="ASYNC_APP" class="ch.qos.logback.classic.AsyncAppender">
  <appender-ref ref="APP_FILE"/>
  <queueSize>4096</queueSize>
  <neverBlock>true</neverBlock>       <!-- 일반 로그: Drop 허용, Thread 보호 우선 -->
</appender>

<!-- RollingFileAppender (자동 로테이션) -->
<appender name="PAYMENT_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
  <file>/log/payment/payment.log</file>
  <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
    <fileNamePattern>/log/payment/payment.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
    <maxFileSize>500MB</maxFileSize>
    <maxHistory>30</maxHistory>
    <totalSizeCap>20GB</totalSizeCap>  <!-- 전체 용량 상한 -->
  </rollingPolicy>
</appender>
```

---

### 7.6 Kubernetes StorageClass / PVC 최적화

```yaml
# 결제 DB 전용 StorageClass (고성능 IOPS 고정형)
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: payment-db-storage
provisioner: ebs.csi.aws.com
parameters:
  type: io2
  iops: "10000"      # IOPS 고정 설정
  throughput: "500"  # MB/s
  encrypted: "true"  # 결제 데이터 암호화
reclaimPolicy: Retain          # 실수로 PVC 삭제해도 데이터 보존
allowVolumeExpansion: true     # 온라인 용량 확장 지원
volumeBindingMode: WaitForFirstConsumer

---
# 결제 서비스 PVC
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: payment-db-pvc
spec:
  storageClassName: payment-db-storage
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 500Gi

---
# 로그 전용 PVC (저비용, 높은 Throughput)
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: payment-log-pvc
spec:
  storageClassName: gp3-throughput  # 로그용 범용 스토리지
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 100Gi
```

---

### 7.7 DB I/O 최적화 (Java 연동 관점)

```text
MySQL InnoDB 설정 (결제 서버):
  innodb_flush_method=O_DIRECT         # OS Page Cache 우회, InnoDB Buffer Pool 직접 관리
  innodb_flush_log_at_trx_commit=1     # Commit마다 WAL fsync (데이터 무결성 최우선)
  innodb_log_file_size=4G              # WAL 파일 크기 증가 → 플러시 빈도 감소
  innodb_io_capacity=20000             # NVMe IOPS에 맞게 조정
  innodb_io_capacity_max=40000         # 폭발적 I/O 허용치
  innodb_buffer_pool_size=메모리의 70~80%  # Page Cache 대체, Disk Read 최소화

PostgreSQL 설정:
  wal_level=replica                    # WAL 레벨 (복제 필요 시)
  fsync=on                             # 반드시 on (결제 데이터 무결성)
  synchronous_commit=on                # Commit마다 WAL fsync
  checkpoint_completion_target=0.9     # Checkpoint 분산 (I/O 스파이크 완화)
  max_wal_size=4GB                     # WAL 파일 증가 허용 → Checkpoint 빈도 감소

HikariCP 연동 설정 (Java):
  connectionTimeout=3000               # Disk I/O 지연 고려 (ms)
  idleTimeout=600000
  maxLifetime=1800000
  keepaliveTime=60000                  # DB 연결 keepalive (Disk I/O 지연과 무관)
```

---

## 8. 관련 Linux 명령어 및 분석 도구

### 8.1 Disk I/O 기본 분석 도구

| 명령어 | 분석 항목 | 사용 예 |
|-------|---------|-------|
| `iostat -xz 1` | IOPS, Throughput, await, %util 실시간 | `iostat -xz 1 10` (10회 수집) |
| `iotop -oP` | 프로세스별 I/O 사용량 (실시간) | `iotop -oP -d 2` |
| `vmstat 1` | bi/bo (Block In/Out), wa (I/O Wait) | `vmstat 1 30` |
| `sar -d 1` | 디스크별 I/O 통계 시계열 | `sar -d 1 60` |
| `dstat --disk --io --top-io` | 실시간 I/O 상위 프로세스 | `dstat -d --top-io 1` |
| `lsblk -d -o name,rota,tran,size` | 디스크 타입, 인터페이스 확인 | `lsblk -d -o name,rota,tran,size,model` |
| `df -h` / `df -i` | 볼륨 사용량 / Inode 사용량 | `df -h && df -i` |
| `du -sh /* 2>/dev/null` | 디렉토리별 디스크 사용량 | `du -sh /log/* | sort -rh | head -20` |
| `filefrag -v /log/payment.log` | 파일 단편화(Fragmentation) 정도 | `filefrag -v <file>` |
| `smartctl -a /dev/sda` | 디스크 SMART 상태 (건강 지표) | `smartctl -H /dev/nvme0` |
| `nvme smart-log /dev/nvme0` | NVMe 상태 (온도, 에러, 수명) | `nvme smart-log /dev/nvme0` |
| `findmnt` | 마운트 옵션 확인 | `findmnt -o target,fstype,options` |
| `fio` | 디스크 성능 벤치마크 | 아래 예시 참고 |

```bash
# fio 벤치마크 예시 (결제 서버 디스크 IOPS 측정)
# 랜덤 읽기 IOPS 측정
fio --name=rand-read --ioengine=libaio --iodepth=32 \
    --rw=randread --bs=4k --size=10g --numjobs=4 \
    --time_based --runtime=30 --output-format=json \
    --filename=/data/testfile

# 순차 쓰기 Throughput 측정 (결제 로그 패턴)
fio --name=seq-write --ioengine=libaio --iodepth=1 \
    --rw=write --bs=128k --size=10g --numjobs=1 \
    --fsync=1 --time_based --runtime=30 \
    --filename=/log/testfile
```

---

### 8.2 블록 I/O 심층 분석 도구

| 도구 | 분석 항목 | 사용 예 |
|-----|---------|-------|
| `blktrace` + `blkparse` | Block 계층 I/O 요청 추적 (요청 → 완료 타임라인) | `blktrace -d /dev/nvme0n1 -o trace` |
| `iowatcher` | blktrace 결과 시각화 | `iowatcher -t trace.blktrace.0 -o io.svg` |
| `biosnoop` (BCC) | Block I/O 요청별 Latency 추적 | `biosnoop -Q` |
| `biolatency` (BCC) | Block I/O Latency 분포 히스토그램 | `biolatency -D 10` |
| `ext4slower` / `xfsslower` (BCC) | Filesystem 느린 I/O 추적 | `xfsslower 10` (10ms 이상 I/O) |
| `fileslower` (BCC) | 파일 I/O 느린 작업 추적 | `fileslower 10` |
| `cachestat` (BCC) | Page Cache Hit/Miss 비율 실시간 | `cachestat 1` |

```bash
# eBPF - fsync Latency 분포 측정 (결제 감사 로그 분석)
bpftrace -e '
  tracepoint:syscalls:sys_enter_fsync { @start[tid] = nsecs; }
  tracepoint:syscalls:sys_exit_fsync
  /@start[tid]/
  {
    @fsync_us = hist((nsecs - @start[tid]) / 1000);
    delete(@start[tid]);
  }
  interval:s:10 { print(@fsync_us); clear(@fsync_us); }
'

# 프로세스별 write() 호출 빈도 및 크기 추적
bpftrace -e '
  tracepoint:syscalls:sys_enter_write
  /pid == <JVM_PID>/
  {
    @write_bytes = hist(args->count);
    @write_count = count();
  }
  interval:s:5 { print(@write_bytes); print(@write_count); }
'

# Page Cache Miss (Major Fault) 추적
bpftrace -e '
  software:major-faults:1 /pid == <JVM_PID>/ {
    @[kstack] = count();
  }
  interval:s:10 { print(@); clear(@); }
'
```

---

### 8.3 JVM 특화 Disk 분석 도구

```bash
# JFR - 파일 I/O 이벤트 수집
jcmd <pid> JFR.start name=disk_analysis \
  duration=60s \
  settings=profile \
  filename=/tmp/disk.jfr

# JFR 파일 I/O 이벤트 추출
jfr print --events FileWrite,FileRead,FileForce \
  /tmp/disk.jfr | head -100

# async-profiler - Wall-clock 모드 (I/O 대기 포함)
./profiler.sh -e wall -d 30 \
  -f /tmp/wall.html <pid>
# → 파일 I/O 대기(FileOutputStream.write 등) 포함 스택 확인

# 파일 I/O 프로파일링 (I/O 이벤트 기반)
./profiler.sh -e read -d 30 -f /tmp/read.html <pid>
./profiler.sh -e write -d 30 -f /tmp/write.html <pid>

# lsof - JVM이 열고 있는 파일 확인
lsof -p <pid> | grep -v socket | head -50
lsof -p <pid> | wc -l  # 열린 파일 총 수

# strace - JVM의 파일 관련 syscall 추적
strace -p <pid> -e trace=read,write,fsync,open,close \
  -T -f 2>&1 | head -200
# -T: 각 syscall 소요 시간 출력

# /proc 기반 JVM 프로세스 I/O 통계
cat /proc/<pid>/io
# rchar: 읽기 바이트 수 (Page Cache 포함)
# wchar: 쓰기 바이트 수
# syscr: read() syscall 횟수
# syscw: write() syscall 횟수
# read_bytes: 실제 디스크에서 읽은 바이트
# write_bytes: 실제 디스크에 쓴 바이트
# cancelled_write_bytes: 쓰기 취소 바이트 (Page Cache로 흡수된 것)
```

---

### 8.4 Prometheus / Grafana 기반 모니터링 설정

```yaml
# node_exporter 수집 지표 활용

# Disk I/O 핵심 지표
- node_disk_io_time_seconds_total      # %util 계산용
- node_disk_read_bytes_total           # Read Throughput
- node_disk_written_bytes_total        # Write Throughput
- node_disk_reads_completed_total      # Read IOPS
- node_disk_writes_completed_total     # Write IOPS
- node_disk_read_time_seconds_total    # Read Await 계산용
- node_disk_write_time_seconds_total   # Write Await 계산용
- node_disk_io_now                     # 현재 진행 중 I/O 수 (Queue Depth)

# Prometheus Alert Rules 예시
groups:
  - name: disk_alerts
    rules:
      - alert: DiskUtilizationHigh
        expr: rate(node_disk_io_time_seconds_total[5m]) > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Disk 사용률 90% 초과 - 결제 I/O 병목 위험"

      - alert: DiskAwaitHigh
        expr: |
          rate(node_disk_read_time_seconds_total[5m]) /
          rate(node_disk_reads_completed_total[5m]) > 0.01
        for: 2m
        annotations:
          summary: "Disk Read Await 10ms 초과 - DB 성능 영향"

      - alert: DiskSpaceLow
        expr: |
          (node_filesystem_avail_bytes / node_filesystem_size_bytes) < 0.2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "디스크 여유 공간 20% 미만 - 결제 로그 볼륨 확인"

      - alert: InodeUsageHigh
        expr: |
          (node_filesystem_files_free / node_filesystem_files) < 0.2
        for: 5m
        annotations:
          summary: "Inode 사용률 80% 초과 - 파일 생성 실패 위험"

      - alert: IOPressureHigh
        expr: node_pressure_io_stalled_seconds_total > 5
        for: 2m
        annotations:
          summary: "PSI IO Pressure 높음 - I/O 병목으로 결제 지연 가능"
```

---

## 부록: 결제 시스템 Disk I/O 이상 징후 빠른 진단표

| 증상 | 가능한 원인 | 첫 번째 확인 명령어 |
|------|-----------|----------------|
| 결제 Commit Latency 급증 | DB WAL fsync 지연, Disk IOPS 포화 | `iostat -xz 1`, `nvme smart-log` |
| 결제 로그 기록 실패 | Disk Full, Inode 고갈, Write Stall | `df -h && df -i`, `/proc/meminfo Dirty` |
| P99 Latency 급증 + CPU iowait 상승 | Dirty Page Write Stall, IOPS 포화 | `iostat -xz`, `cat /proc/pressure/io` |
| Heap Dump 생성 실패 | HeapDump 볼륨 Full | `df -h /mnt/heapdump` |
| Kafka Producer 지연 | 브로커 디스크 I/O 포화 | Kafka Broker: `iostat`, `biolatency` |
| GC 로그 파일 무한 증가 | filecount/filesize 미설정 | `ls -lh /log/gc*`, Logback 설정 확인 |
| AsyncAppender 로그 Drop | 큐 포화, Disk I/O 병목 | JMX AsyncAppender DiscardedCount |
| Pod 재시작 후 로그 유실 | PVC 미사용, emptyDir 사용 | `kubectl describe pod`, PVC 마운트 확인 |
| DB 느린 쿼리 증가 | Buffer Pool Miss → Disk Read 증가 | MySQL: `SHOW STATUS LIKE 'Innodb_buffer_pool_reads'` |

---

*작성 기준: Java 21, Spring Boot 3.x, Logback/Log4j2, HikariCP, Kubernetes 환경, Linux Kernel 5.x*  
*관점: FinTech 결제 시스템 SRE — 정확성 > 안정성 > 관측 가능성 > 성능*  
*원칙: 결제 감사 로그는 절대 Drop 금지 / DB WAL과 결제 로그는 반드시 분리 볼륨*

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*