# 트랜잭션 (Transaction)

> 정독: 0회

## 1. 이 기술이 무엇인가

트랜잭션(Transaction)은:

> 하나의 논리적 작업을 구성하는 여러 연산을 **"전부 성공" 또는 "전부 실패"로 처리하는** **원자적 실행 단위**

### 핵심 특징

트랜잭션은 다음을 목표로 합니다.

- 부분 성공 허용 안 함
- 중간 상태 노출 방지
- 데이터 무결성 유지
- 장애 발생 시 복구 가능

### 대표 예시

- 계좌 이체
- 결제 승인
- 재고 차감
- 주문 생성
- 로그 기록
- 파일 시스템 journal 처리

### 핵심 결과

> 중간 실패가 발생해도  
> **시스템 상태가 깨지지 않도록 보장**합니다.

---

## 2. 시스템 어디에서 등장하는가

트랜잭션은 거의 모든 시스템 계층에서 등장합니다.

### Database — 가장 대표적

`INSERT`, `UPDATE`, `DELETE`, `COMMIT`, `ROLLBACK`

### Filesystem

journaling filesystem: `ext4`, `xfs`, `btrfs`

### Distributed System

- distributed commit
- saga
- message queue consistency

### Storage Stack

- WAL (Write Ahead Log)
- fsync
- flush ordering

### Runtime / Application Layer

- business operation consistency
- state transition protection

### Kubernetes / Infra

- etcd write consistency
- control plane state update

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 영향이 큰 자원: **Disk I/O**

특히 **durability 보장** 때문입니다.

commit 완료를 보장하려면 다음이 필요합니다.

- WAL 기록
- page flush
- fsync
- replication sync

| 자원 | 영향 항목 |
|---|---|
| **CPU** | lock management, MVCC visibility check, transaction scheduling |
| **Memory** | buffer cache, undo log, transaction state table |
| **Network** | replication, quorum, consensus (분산 트랜잭션 환경에서 큼) |

---

## 4. 왜 중요한가

**데이터 무결성**을 지키기 때문입니다.

### 트랜잭션 없을 경우

중간 실패 발생 가능:

```
잔고 차감 성공
입금 실패
```

결과: 데이터 손상, 중복 결제, 유실, 상태 불일치 발생 가능.

### 금융 시스템 핵심

payment / banking / inventory / settlement 영역에서는 필수.

### SRE 관점

> 트랜잭션은 **시스템 신뢰성의 핵심**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

| 장애 유형 | 설명 |
|---|---|
| **Partial Commit** | 일부만 반영되는 장애 |
| **Dirty Read** | 미완료 데이터 읽음 |
| **Lost Update** | 동시성 충돌로 데이터 유실 |
| **Double Spending** | 결제 중복 처리 |
| **Split Brain** | 분산 시스템 상태 분리 |
| **WAL Corruption** | 로그 손상 시 recovery 실패 |
| **fsync Delay** | disk latency 증가로 transaction stall 발생 |
| **Replica Lag** | replication delay로 consistency 깨질 수 있음 |

### Lock Contention

transaction 증가 시 deadlock, timeout, queue buildup 발생.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

핵심 메커니즘은 **7개**입니다.

| # | 메커니즘 | 설명 |
|---|---|---|
| 1 | **Atomicity** | 모두 성공 or 모두 취소 |
| 2 | **Commit** | 최종 반영 |
| 3 | **Rollback** | 실패 시 이전 상태 복원 |
| 4 | **Isolation** | 동시 실행 충돌 방지 |
| 5 | **Durability** | commit 이후 영구 보존 |
| 6 | **WAL (Write Ahead Log)** | 실제 데이터 수정 전 로그 먼저 기록. 장애 복구 핵심 |
| 7 | **Lock / MVCC** | 동시성 제어 핵심 메커니즘 |

### 핵심 흐름

```
BEGIN
→ operation 수행
→ log 기록
→ consistency 검증
→ commit or rollback
→ durability 보장
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Database 상태

```sql
SHOW PROCESSLIST;
SELECT * FROM pg_stat_activity;
```

### Lock 상태

```sql
SELECT * FROM pg_locks;
SELECT * FROM information_schema.innodb_locks;
```

### Disk Flush 상태

```bash
iostat
iotop
blktrace
```

### WAL / Journal 확인

```bash
ls pg_wal/
journalctl
```

### fsync latency

```bash
perf
fio
bpftrace
```

### Kubernetes

etcd transaction 상태:

```bash
etcdctl endpoint status
```

container filesystem I/O:

```bash
kubectl top pod
```

node disk pressure:

```bash
kubectl describe node
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*