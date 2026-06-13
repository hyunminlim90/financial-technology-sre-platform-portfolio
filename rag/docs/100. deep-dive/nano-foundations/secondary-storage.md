# Secondary Storage (보조기억장치 / 스토리지)
## **Micro Foundations — 범용 시스템/인프라 관점**

> 정독: 0회

## 1. 이 기술이 무엇인가

**Secondary Storage**는:

> 전원이 꺼져도 데이터가 사라지지 않도록 시스템의 상태(State)와 데이터를 장기 보존하는 **비휘발성 저장 계층**

이다.

쉽게 말하면:

- **RAM**은 "작업대"
- **Secondary Storage**는 "창고"

CPU는 직접 Storage를 빠르게 다루지 못한다. 반드시:

```
Storage → RAM → CPU
```

순서로 데이터를 가져와야 한다.

> **핵심:** 보조기억장치는 시스템의 **'기억'과 '영속성(Durability)'**을 담당한다.

---

## 2. 시스템 어디에서 등장하는가

보조기억장치는 거의 모든 시스템의 **최하단 데이터 계층**에 존재한다.

### 운영체제
- 파일 시스템, 커널 이미지, 로그, swap

### 데이터베이스
- transaction log, WAL, data file, index

### 메시지 시스템
- Kafka commit log, durable queue, replay storage

### 클라우드 인프라
- block storage, object storage, distributed storage

### 백업/아카이브
- snapshot, backup, archive, disaster recovery storage

> **핵심:** 시스템이 **"잊지 않아야 하는 데이터"**는 결국 Storage로 내려간다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Disk / Storage I/O**이다.

### 핵심 병목

Storage는 CPU보다 매우 느리고, RAM보다 훨씬 느리다:

```
CPU Register << CPU Cache << RAM << SSD << HDD
```

Latency 차이는 수십~수백만 배까지 벌어진다.

### 자원별 영향

| 자원 | 영향 항목 |
|------|-----------|
| **Disk** | read/write latency, IOPS, throughput, queue depth |
| **CPU** | filesystem 처리, checksum, compression, encryption |
| **Memory** | page cache, buffer cache, filesystem cache |
| **Network** | replication traffic, storage synchronization, distributed filesystem |

> **핵심:** Storage 성능은 대부분 **"I/O 대기 시간"** 문제로 나타난다.

---

## 4. 왜 중요한가

Storage는 **시스템의 영속성과 무결성을 최종 보장하는 계층**이다.

CPU나 RAM은 빨라도 **휘발성**이다. 전원 차단 시 RAM 데이터는 사라지지만, Storage 데이터는 남는다.

따라서 다음과 같은 핵심 상태는 반드시 Storage에 기록된다:

- 금융 원장
- 주문 데이터
- 트랜잭션 로그
- 사용자 데이터

또한 Storage는 **시스템 병목의 핵심 원인**이 되기도 한다. CPU가 빠르더라도 disk flush · sync write · random read가 느리면 전체 시스템이 멈춘다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 1) Disk Full
```
디스크 사용률 100%
  ↓
DB write 실패 → transaction abort → filesystem error → 서비스 중단
```

### 2) I/O Bottleneck
```
Storage 처리 속도 < 요청량
  ↓
I/O wait 증가 → latency spike → request timeout
```

### 3) SSD Wear-Out
```
SSD 수명 고갈
  ↓
write failure → read corruption → storage offline
```

### 4) HDD Mechanical Failure
```
회전 디스크 손상
  ↓
bad sector → data loss → rebuild storm
```

### 5) Storage Replication Lag
```
복제 지연 발생
  ↓
stale read → inconsistency → failover corruption
```

### 6) Write Amplification
```
과도한 작은 write 발생
  ↓
SSD 성능 저하 → GC pressure 증가 → latency 불안정
```

> **핵심:** Storage 장애는 성능 문제를 넘어서 **데이터 무결성 자체를 위협**한다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Memory Hierarchy

```
CPU Register
     ↓
  CPU Cache
     ↓
    RAM
     ↓
Secondary Storage
```

아래로 갈수록: 느려짐 · 커짐 · 저렴해짐 · 영속성 증가

### SSD vs HDD

| 구분 | 특징 | 문제 |
|------|------|------|
| **SSD** | 빠름, low latency, random access 강함, 반도체 기반 | write endurance 제한 |
| **HDD** | 대용량 저렴, 기계식 | seek latency, mechanical failure |

### 핵심 메커니즘

| 메커니즘 | 의미 |
|----------|------|
| **Sequential I/O** | 연속 데이터 접근 — 빠름 |
| **Random I/O** | 흩어진 위치 접근 — 느림 |
| **Buffering** | page cache / write buffer로 직접 write 최소화 |
| **fsync / durability** | 데이터를 실제 Storage에 강제 기록 — 안전하지만 느림 |
| **Replication** | Storage 복제 — durability, failover, redundancy 목적 |
| **RAID** | 다중 디스크 결합 — redundancy, performance, fault tolerance |
| **WAL / Commit Log** | append-only 로그 선기록 후 상태 반영 — 무결성과 복구 핵심 |

> **핵심:** Storage 시스템은 속도보다 **"데이터를 잃지 않는 것"**이 우선인 경우가 많다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**디스크 사용량**
```bash
df -h
```
관찰: disk fullness, mount status

**I/O 상태**
```bash
iostat
iotop
vmstat
```
관찰: I/O wait, read/write throughput, queue depth

**블록 디바이스**
```bash
lsblk
blkid
```

**디스크 오류**
```bash
dmesg
journalctl
```
관찰: I/O error, filesystem corruption, device timeout

---

### Runtime

관찰 포인트:
- fsync latency
- storage queue backlog
- cache hit ratio
- flush delay
- WAL latency
- checkpoint duration

---

### Kubernetes

| 관찰 영역 | 명령어 | 관찰 |
|-----------|--------|------|
| **볼륨 상태** | `kubectl get pv` / `kubectl get pvc` | 볼륨 바인딩 및 용량 상태 |
| **Pod Storage 사용량** | `kubectl describe pod` | 마운트 상태, 볼륨 오류 |
| **노드 디스크 압박** | `kubectl describe node` | DiskPressure |
| **Storage 이벤트** | `kubectl get events` | 스토리지 관련 이벤트 |

> **핵심:** Storage 문제는 CPU 사용률보다 먼저 **I/O wait·latency·queue 증가**로 나타나는 경우가 많다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*