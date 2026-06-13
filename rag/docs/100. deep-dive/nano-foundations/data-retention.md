# 데이터 보존 (Data Retention)
## Micro Foundations — 범용 시스템 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

**데이터 보존(Data Retention)** 은:

> 시스템이 생성하거나 저장한 데이터를 일정 시간 동안 **유실·손상 없이 유지하는 능력과 정책**

핵심은 단순 저장(Storage)이 아니다. 중요한 것은:

- 시간이 지나도 유지되는가
- 장애 이후에도 복구 가능한가
- 원래 상태(State)가 깨지지 않는가
- 필요한 기간 동안 살아남는가

즉:

> **Retention = "데이터 생존성(Survivability)"**

---

## 2. 시스템 어디에서 등장하는가

데이터 보존은 **거의 모든 시스템 계층**에 존재한다.

### 메모리 계층
현재 실행 중 상태를 **짧게** 보존: `RAM`, `Cache`, `Register`, `Shared Memory`

### 저장소 계층
장기 데이터 보존 담당: `SSD`, `HDD`, `Distributed Storage`, `Object Storage`

### 네트워크 계층
전송 중 데이터 보존: `Packet Buffer`, `Queue`, `Stream`, `Message Broker`

### 애플리케이션 계층
논리적 상태 보존: `Session`, `Transaction State`, `Event Log`, `Snapshot`, `Checkpoint`

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향: **Memory + Disk**. 하지만 실제로는 모든 자원과 연결된다.

| 자원 | 데이터 보존과의 관계 |
|------|------------------|
| Memory | 상태/캐시/버퍼/세션 유지. 부족 시 데이터 손실, OOM, State corruption 발생 가능 |
| Disk | 보존의 핵심 자원. Retention 기간 증가 시 사용량·I/O·백업·복제 비용 증가 |
| Network | 복제(Replication)와 백업 과정에서 대량 데이터 전송 및 동기화 트래픽 증가 |
| CPU | 보존 자체보다 압축·암호화·checksum·replication validation 과정에서 사용 증가 |

---

## 4. 왜 중요한가

데이터 보존이 깨지면 **시스템의 기억이 사라진다**는 의미가 된다.

시스템은 결국 **"상태(State)"** 기반으로 움직인다:

- 사용자 잔액
- 주문 상태
- 세션 상태
- 로그 기록
- 이벤트 순서
- 트랜잭션 기록

이 상태들이 유지되어야 **시스템 정합성**이 살아있다.

> ⚠️ Retention 실패는 곧: 데이터 유실 / 정합성 붕괴 / 복구 불가능 / 감사 추적 불가 / 비즈니스 신뢰 붕괴로 이어진다.

---

## 5. 실제 장애와 어떤 관련이 있는가

Retention 문제는 실제로 **매우 자주 장애 원인**이 된다.

### 1) 메모리 기반 상태 유실
프로세스 재시작 시 세션 소멸, 작업 상태 유실, in-memory queue 손실 발생

### 2) 디스크 손상
`bit rot`, `filesystem corruption`, `SSD wear-out`, `RAID rebuild 실패` 등으로 데이터 보존 실패

### 3) 로그 보존 실패
Retention policy 오설정 시 감사 로그 삭제 → 장애 분석 불가능 → 추적 불가 발생 가능

### 4) 복제 지연
Distributed System에서 `replica lag`, `partial commit`, `stale state`로 인해 최신 상태 보존 실패

### 5) 전원 장애
Flush 전에 전원 차단 시 `write loss`, `journal corruption`, `partial write` 발생 가능

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심은 **"데이터는 보존 계층을 이동한다"** 는 점이다.

### 일반적인 보존 계층 흐름

```
CPU / Register
    ↓
Cache
    ↓
RAM
    ↓
Persistent Storage
    ↓
Backup / Replica
```

| 특성 | 의미 |
|------|------|
| 속도 빠를수록 | 보존 기간 짧음, 휘발성 강함 |
| 속도 느릴수록 | 보존 기간 길어짐, 안정성 높음 |

즉 시스템은 항상 **속도 ↔ 보존성** 사이에서 트레이드오프를 가진다.

### Persistence ≠ Durability

> - **Persistence:** RAM에 남아있다 (프로세스 살아있는 동안 유지)
> - **Durability:** 전원이 꺼져도 살아남는다 (영구 저장)

둘은 다르다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### 메모리 상태
```bash
free -h
vmstat 1
top
htop
```

### 디스크 상태
```bash
df -h
iostat -x 1
smartctl -a /dev/sda
```

### 파일시스템 및 로그
```bash
dmesg
journalctl
```

### Runtime 관찰 포인트
- `heap retention`
- `object lifetime`
- `cache retention`
- `memory leak`
- `checkpoint state`

### Kubernetes Persistent Volume
```bash
kubectl get pv,pvc
kubectl get storageclass
kubectl describe statefulset <name>
```

### Kubernetes 보존 관련 장애 포인트
- `Eviction`
- `Volume detach 실패`
- `Node crash`
- `etcd corruption`

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*