# Fault (결함)
## Micro Foundations — 범용 시스템 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

Fault(결함)는:

> 시스템 내부에 존재하는 잠재적인 설계 오류, 구현 버그, 물리적 손상, 또는 잘못된 상태의 원인

중요한 점은:

> **Fault ≠ 즉시 장애**

라는 것이다. 결함은 보통:

- 숨어 있다가
- 특정 조건에서 활성화되고
- 내부 오류(Error)를 만들고
- 결국 외부 장애(Failure)로 이어진다.

즉:

> **Fault는 시스템 내부에 잠복한 문제의 씨앗이다.**

---

## 2. 시스템 어디에서 등장하는가

결함은 시스템의 모든 계층에서 등장한다.

**하드웨어**
- 메모리 bit flip
- 디스크 배드 섹터
- 전원 불안정
- 네트워크 케이블 손상

**운영체제**
- scheduler bug
- kernel deadlock
- file system corruption

**런타임**
- memory leak
- resource exhaustion
- thread starvation

**네트워크**
- packet corruption
- routing inconsistency
- retransmission anomaly

**저장소**
- replication mismatch
- stale replica
- write corruption

**분산 시스템**
- split brain
- clock skew
- consensus inconsistency

즉:

> **Fault는 특정 기술의 문제가 아니라 시스템 어디에서든 발생 가능한 잠재 결점이다.**

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

결함은 모든 자원에 영향을 줄 수 있다. 하지만 주로:

> **Memory / Disk / Network**

에서 치명적인 형태로 나타난다.

### Memory

문제: memory leak, corruption, stale cache, bit flip

결과:

- invalid state
- OOM
- data corruption

### Disk

문제: write failure, sector corruption, journal inconsistency

결과:

- data loss
- broken recovery
- state divergence

### Network

문제: packet loss, duplicated transmission, delayed delivery

결과:

- inconsistent state
- retry storm
- split state

### CPU

문제: runaway loop, scheduling bug, race condition

결과:

- latency explosion
- starvation
- unstable execution

---

## 4. 왜 중요한가

현대 시스템은:

> **결함이 존재하는 것이 정상**

이라는 전제 위에서 설계된다. 왜냐하면:

- 하드웨어는 노후화되고
- 소프트웨어는 완벽할 수 없고
- 네트워크는 불안정하며
- 인간은 실수하기 때문이다.

따라서 핵심은:

> **결함을 0으로 만드는 것**

이 아니라,

> **결함이 전체 장애로 확대되지 않게 만드는 것**

이다. 즉:

> **Fault 관리의 핵심은 제거보다 격리와 완화에 있다.**

---

## 5. 실제 장애와 어떤 관련이 있는가

결함은 보통 다음 흐름으로 발전한다.

```
Fault   : 잠재 원인
  ↓
Error   : 내부 비정상 상태
  ↓
Failure : 외부 기능 실패
```

**1) Memory Leak Fault**

```
결함  : 메모리 반환 누락
  ↓
오류  : available memory 감소
  ↓
장애  : OOM Kill / 서비스 다운
```

**2) Network Fault**

```
결함  : packet loss 증가
  ↓
오류  : timeout accumulation
  ↓
장애  : API unavailable
```

**3) Storage Fault**

```
결함  : 디스크 corruption
  ↓
오류  : invalid block read
  ↓
장애  : DB crash / data loss
```

**4) Concurrency Fault**

```
결함  : synchronization 누락
  ↓
오류  : race condition
  ↓
장애  : duplicated transaction / corrupted state
```

**5) Clock Fault**

```
결함  : node time skew
  ↓
오류  : ordering inconsistency
  ↓
장애  : distributed consensus failure
```

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

Fault의 핵심은:

> **"결함은 반드시 발생한다"는 전제다.**

따라서 시스템은 **Fault-Free System** 이 아니라:

> **Fault-Tolerant System**

을 목표로 한다.

핵심 메커니즘:

- **Isolation** — 결함 확산 차단. 예: process isolation, container isolation, fault domain separation
- **Redundancy** — 대체 경로 확보. 예: replica, standby node, redundant power
- **Detection** — 빠른 결함 탐지. 예: health check, watchdog, heartbeat
- **Recovery** — 자동 복구. 예: restart, failover, re-election
- **Retry with Control** — 제어된 재시도. 예: backoff, retry budget
- **Validation** — 오염 상태 차단. 예: checksum, integrity verification
- **Graceful Degradation** — 부분 기능 유지. 예: read-only fallback, limited service mode
- **Containment** — 문제 범위 제한. 예: circuit breaker, rate limit, sandboxing

핵심 개념:

> **결함 자체보다 위험한 것은 결함의 전파이다.**

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**시스템 오류 로그**

```bash
dmesg
journalctl -xe
```

관찰: kernel error, I/O fault, memory corruption

**디스크 상태**

```bash
smartctl -a /dev/sdX
```

관찰: media error, reallocated sector

**메모리 오류**

```bash
edac-util -v
```

관찰: ECC correction, memory fault

**네트워크 오류**

```bash
netstat -s
sar -n DEV
```

관찰: retransmission, dropped packet

### Runtime

관찰 포인트:

- abnormal restart
- retry spike
- queue buildup
- unexpected latency
- resource leak

### Kubernetes

**Pod 상태**

```bash
kubectl get pods
kubectl describe pod
```

관찰: `CrashLoopBackOff`, `OOMKilled`, unhealthy probe

**노드 상태**

```bash
kubectl describe node
```

관찰: `MemoryPressure`, `DiskPressure`, `NotReady`

**이벤트 흐름**

```bash
kubectl get events --sort-by=.lastTimestamp
```

**Replica 상태**

```bash
kubectl get rs
kubectl get deploy
```

관찰: replica mismatch, failed rollout

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*