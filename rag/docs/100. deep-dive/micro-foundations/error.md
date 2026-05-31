# Error (오류)
## Micro Foundations — 범용 시스템 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

Error(오류)는:

> 시스템 내부 상태가 정상 규칙에서 벗어난 비정상 상태

중요한 점은:

> **Error는 원인(Fault) 자체가 아니다.**

전산학에서는 보통 다음처럼 구분한다.

```
Fault → Error → Failure
```

- **Fault (결함)** — 잠재된 문제 원인. 예: 버그, 잘못된 설계, 하드웨어 결함
- **Error (오류)** — 그 결함이 실제 실행되어 나타난 내부 비정상 상태. 예: 잘못된 메모리 값, invalid state, corrupted data, timeout 상태
- **Failure (장애)** — 그 Error를 통제하지 못해 외부 기능이 깨진 상태. 예: 서비스 다운, 응답 실패, 데이터 손상

즉:

> **Error는 시스템 내부에서 발생한 "비정상 상태의 현실화"이다.**

---

## 2. 시스템 어디에서 등장하는가

오류는 시스템 전체 모든 레이어에서 발생한다.

**하드웨어 레이어**
- 메모리 비트 반전
- 디스크 read/write failure
- CPU exception

**운영체제 레이어**
- process crash
- syscall failure
- permission violation

**런타임 레이어**
- invalid execution state
- resource exhaustion
- deadlock

**애플리케이션 레이어**
- invalid input
- state inconsistency
- concurrency issue

**데이터 레이어**
- corrupted record
- transaction conflict
- replication inconsistency

**네트워크 레이어**
- packet loss
- timeout
- protocol mismatch

즉:

> **Error는 특정 기술 문제가 아니라 시스템 상태가 정상 범위를 벗어난 현상 전체이다.**

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

Error는 특정 자원 하나만의 문제가 아니다. 다만 Error 유형에 따라 영향 영역이 달라진다.

### CPU 관련 Error

예: invalid instruction, divide by zero, scheduling anomaly

영향:

- process abort
- execution interruption

### Memory 관련 Error

예: memory corruption, invalid pointer, out-of-memory

영향:

- state corruption
- crash
- unpredictable behavior

### Disk 관련 Error

예: write failure, filesystem corruption, journal inconsistency

영향:

- data loss
- persistence failure

### Network 관련 Error

예: packet corruption, retransmission, timeout

영향:

- communication failure
- distributed inconsistency

핵심:

> **Error는 자원 자체보다 "정상 상태(State)가 깨졌다는 것"이 본질이다.**

---

## 4. 왜 중요한가

시스템은:

> **Error 없이 동작하지 않는다.**

현실 시스템에서는 하드웨어도 고장 나고, 네트워크도 깨지고, 코드도 완벽하지 않다.

중요한 것은:

> **Error가 발생하지 않게 하는 것보다 Error가 Failure로 번지지 않게 막는 것**

이다. 즉:

> **안정성(Stability)의 핵심은 Error containment(오류 격리)이다.**

또한:

> **Error는 시스템 건강 상태를 알려주는 조기 경보이기도 하다.**

예: timeout 증가, retry 증가, queue accumulation, checksum mismatch, replication lag

이런 Error 징후들은:

> **곧 더 큰 장애가 올 수 있다는 신호가 된다.**

---

## 5. 실제 장애와 어떤 관련이 있는가

대부분의 장애는:

> **Error를 제어하지 못해서 발생한다.**

**1) Memory Error → Service Failure**

```
메모리 누수 발생
  ↓ available memory 감소
  ↓ allocation failure
  ↓ OOM 발생
  ↓ 프로세스 종료
```

**2) Network Error → Cascading Failure**

```
외부 통신 timeout 증가
  ↓ retry storm 발생
  ↓ queue 증가
  ↓ resource exhaustion
  ↓ 전체 시스템 장애
```

**3) Data Error → Integrity Failure**

```
동시 업데이트 충돌
  ↓ inconsistent state 발생
  ↓ 잘못된 데이터 commit
  ↓ 금융 데이터 무결성 파괴
```

**4) Disk Error → Persistence Failure**

```
storage write failure 발생
  ↓ transaction journal 손상
  ↓ recovery 불가능 상태 진입
```

즉:

> **Failure는 보통 통제되지 않은 Error의 결과물이다.**

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

Error의 핵심 메커니즘은:

> **Detection → Isolation → Recovery**

이다.

### 1) Detection (감지)

시스템은 먼저 "현재 상태가 정상 범위를 벗어났는가?"를 감지해야 한다. 예:

- checksum mismatch
- invalid state
- timeout
- failed assertion
- health check failure

### 2) Isolation (격리)

오류가 퍼지지 않게 차단한다. 예:

- process isolation
- container isolation
- circuit breaking
- retry limitation

### 3) Recovery (복구)

정상 상태로 되돌린다. 예:

- restart
- rollback
- failover
- replay
- retry

즉:

> **현대 시스템은 Error 자체를 제거하는 것이 아니라,**  
> **Error를 통제 가능한 범위 안에 가두도록 설계된다.**

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**커널 오류 로그**

```bash
dmesg
journalctl -k
```

관찰: OOM, disk error, kernel panic, hardware fault

**프로세스 상태**

```bash
top
ps
htop
```

관찰: zombie process, stuck task, crash 상태

**시스템 로그**

```bash
journalctl
```

관찰: runtime failure, service restart, timeout

### Runtime

관찰 포인트:

- exception count
- retry count
- queue overflow
- timeout frequency
- invalid state transition

### Kubernetes

**Pod 상태**

```bash
kubectl get pod
kubectl describe pod
```

관찰: `CrashLoopBackOff`, `OOMKilled`, `ImagePullBackOff`

**이벤트**

```bash
kubectl get events
```

**로그**

```bash
kubectl logs
```

관찰: runtime error, startup failure, connection failure

**Node 상태**

```bash
kubectl describe node
```

관찰: `MemoryPressure`, `DiskPressure`, `NotReady`

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*