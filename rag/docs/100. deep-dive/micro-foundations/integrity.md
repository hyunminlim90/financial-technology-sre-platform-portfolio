# Integrity (무결성)
## Micro Foundations — 범용 시스템 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

Integrity(무결성)는:

> 데이터와 시스템 상태가 허가되지 않은 변경·누락·중복·손상 없이 항상 올바르고 일관되게 유지되는 성질

핵심은 단순 저장이 아니다. 중요한 것은:

- 값이 틀리지 않았는가
- 중간에 변조되지 않았는가
- 순서가 꼬이지 않았는가
- 일부만 반영되지 않았는가
- 시스템 전체가 동일한 사실을 보고 있는가

이다. 즉:

> **Integrity는 "데이터가 믿을 수 있는 상태인가"를 보장하는 성질이다.**

---

## 2. 시스템 어디에서 등장하는가

무결성은 시스템 전체에서 등장한다.

**저장 (Storage)**
- 파일 시스템
- 데이터베이스
- 로그 저장소
- 객체 스토리지

**전송 (Transmission)**
- 네트워크 패킷
- 메시지 큐
- API 요청/응답
- 복제 (replication)

**연산 (Computation)**
- 동시성 처리
- 상태 갱신
- transaction
- distributed update

**보안 (Security)**
- 인증 데이터
- 디지털 서명
- 권한 검증
- 위변조 탐지

**분산 시스템**
- leader election
- consensus
- replication consistency
- event ordering

즉:

> **무결성은 데이터가 움직이는 모든 경로에 존재한다.**

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

무결성은:

> **Storage + Network**

영향이 특히 크다.

### Disk / Storage

무결성 핵심 영역. 최종 상태(State)가 저장되는 곳이기 때문이다.

문제 예시: partial write, bit corruption, fsync 실패, replica divergence

결과:

- 데이터 오염
- 복구 불가능 상태

### Network

전송 중 무결성이 중요하다.

문제: packet corruption, packet loss, duplication, reorder

결과:

- 잘못된 상태 반영
- 이벤트 순서 꼬임

### Memory

임시 상태 무결성이 중요하다.

문제: race condition, stale state, shared memory corruption

### CPU

동시성 제어와 ordering 보장에 영향.

문제: instruction reordering, concurrent write collision

---

## 4. 왜 중요한가

안정성(Stability)은 **"시스템이 살아있는가"** 를 다룬다.

무결성(Integrity)은 **"시스템이 믿을 수 있는가"** 를 다룬다.

서버가 잠깐 죽는 것은 복구 가능할 수 있다. 하지만:

- 잔액이 틀림
- 중복 결제 발생
- 로그 순서 오염
- 거래 일부만 반영

같은 무결성 파괴는:

> **비즈니스 신뢰 자체를 붕괴시킨다.**

특히 금융·결제·인증 시스템은:

> **"틀리게 빠른 것"보다 "느려도 정확한 것"이 우선된다.**

---

## 5. 실제 장애와 어떤 관련이 있는가

무결성 장애는 보통:

> **"시스템은 살아있는데 데이터가 틀린 상태"**

로 나타난다.

### 1) Race Condition

동시 수정 충돌. 결과:

- 중복 차감
- 잘못된 잔액

### 2) Partial Update

일부만 반영. 예: DB는 성공, 메시지 발행 실패

결과:

- 시스템 간 상태 불일치

### 3) Duplicate Processing

같은 이벤트 중복 처리. 결과:

- 중복 결제
- 중복 정산

### 4) Lost Update

뒤늦은 write가 이전 값 덮어씀. 결과:

- 최신 상태 유실

### 5) Corrupted Transmission

전송 중 데이터 손상. 결과:

- 잘못된 payload 처리

### 6) Replica Divergence

복제 노드 상태 불일치. 결과:

- 읽을 때마다 다른 결과

### 7) Out-of-Order Event

이벤트 순서 뒤집힘. 결과:

- 상태 전이 오류

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

무결성의 핵심은:

> **"데이터가 언제 어디서 바뀌었는지 정확하게 통제하는 것"**

이다.

핵심 메커니즘:

- **Validation** — 입력 검증. 예: schema validation, type check, domain constraint
- **Atomicity** — 중간 상태 노출 방지. 예: all-or-nothing update
- **Consistency** — 시스템 규칙 유지. 예: constraint enforcement, invariant 유지
- **Isolation** — 동시 접근 충돌 방지. 예: locking, MVCC, serialization
- **Durability** — 확정 상태 영구 보존. 예: WAL, journaling, replication
- **Checksum / Hash** — 변조 탐지. 예: CRC, SHA, MAC
- **Ordering** — 순서 보장. 예: sequence number, offset, monotonic ordering
- **Idempotency** — 중복 실행 안전성. 예: duplicate request suppression
- **Consensus** — 분산 상태 합의. 예: quorum, Raft, Paxos

핵심 개념:

> **무결성은 "데이터를 저장하는 기술"이 아니라,**  
> **"데이터가 틀어지지 않게 통제하는 기술"이다.**

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**파일시스템 상태**

```bash
dmesg
journalctl
```

관찰: I/O error, filesystem corruption, write failure

**디스크 무결성**

```bash
smartctl -a /dev/sdX
```

관찰: media error, sector corruption

**RAID 상태**

```bash
cat /proc/mdstat
```

### Runtime

관찰 포인트:

- duplicate event count
- transaction rollback
- lock contention
- retry anomaly
- ordering violation

### Kubernetes

**상태 확인**

```bash
kubectl get events
kubectl describe pod
```

**저장소 상태**

```bash
kubectl get pvc
kubectl describe pvc
```

**재시작 반복**

```bash
kubectl get pods
```

관찰: `CrashLoopBackOff`, repeated restart → 중복 처리 위험 가능성

**분산 상태 확인**

관찰 포인트:

- replication lag
- leader failover
- stale read
- split brain

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*