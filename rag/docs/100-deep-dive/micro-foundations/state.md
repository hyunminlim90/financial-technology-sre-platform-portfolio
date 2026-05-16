# State (상태)
## Micro Foundations — 범용 시스템 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

State(상태)는:

> 특정 시점에 시스템이 기억하고 있는 현재 데이터와 조건의 총합

쉽게 말하면:

- 지금 메모리에 무엇이 들어있는가
- 현재 시스템이 어떤 상황인가
- 이전 연산 결과가 어떻게 남아있는가

를 나타내는 현재 모습이다.

즉:

> **State는 시스템의 "현재 기억"이다.**

---

## 2. 시스템 어디에서 등장하는가

상태는 거의 모든 시스템에 존재한다.

**하드웨어**
- Register 값
- Memory 값
- Cache 내용
- Device 상태

**운영체제**
- Process 상태
- Thread 상태
- File descriptor 상태
- Scheduler queue 상태

**애플리케이션**
- 사용자 로그인 상태
- 주문 상태
- 결제 상태
- 세션 상태

**분산 시스템**
- Replica synchronization 상태
- Cluster membership 상태
- Consensus 상태

즉 시스템이:

> **무언가를 "기억"한다면 그것은 상태(State)다.**

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 자원:

> **Memory + Disk**

### Memory

실행 중 상태의 핵심 저장소. 예:

- 변수
- 객체
- 세션
- cache

### Disk

상태를 영구 보존할 때 사용. 예:

- DB
- log
- snapshot
- WAL

### CPU

상태를 읽고 수정(State Mutation)할 때 사용.

### Network

분산 환경에서 상태 동기화에 사용. 예:

- replication
- distributed cache
- consensus traffic

---

## 4. 왜 중요한가

상태는 시스템의 "현재 현실"이다. 예:

```
balance = 70000
```

라는 상태가 존재하기 때문에:

- 결제 가능 여부 판단
- 한도 계산
- 정산 처리

가 가능하다.

즉 시스템은:

> 상태를 읽고  
> 상태를 바꾸고  
> 새 상태를 저장하는 기계

라고 볼 수 있다.

또 중요한 이유:

> **장애 대부분은 상태 관리 실패에서 발생한다.**

예:

- 상태 불일치
- stale state
- race condition
- split brain
- dirty write

---

## 5. 실제 장애와 어떤 관련이 있는가

상태 관련 장애는 매우 위험하다.

### 1) State Loss

메모리 기반 상태 유실. 결과:

- 로그인 세션 증발
- 주문 상태 손실
- 진행 중 작업 중단

### 2) Inconsistent State

시스템 간 상태 불일치. 예:

- A 서버 상태 ≠ B 서버 상태

결과:

- 중복 결제
- 재고 오류
- 데이터 충돌

### 3) Race Condition

동시 상태 수정 충돌. 결과:

- lost update
- dirty write
- 정합성 붕괴

### 4) Stale State

오래된 상태 사용. 결과:

- 잘못된 의사결정
- outdated cache 문제
- replication lag 문제

### 5) Split Brain

분산 시스템이 서로 다른 상태를 진실로 믿음. 결과:

- 데이터 파손
- cluster corruption

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심은:

> **상태는 시간이 흐르며 계속 변한다.**

즉 시스템은:

> **State Transition Machine**

으로 볼 수 있다. 예:

```
PENDING
→ PROCESSING
→ SUCCESS
→ FAILED
```

또 매우 중요한 개념:

> **상태는 "어디에 저장하느냐"가 중요하다.**

### Stateful

상태를 자기 내부에 저장.

- 장점: 빠름
- 단점: 장애 시 상태 유실 가능, scale-out 어려움

### Stateless

상태를 외부 저장소에 분리.

- 장점: 확장성 높음, 장애 복구 쉬움
- 단점: network/database 의존 증가

현대 분산 시스템은 대부분:

> **Compute와 State를 분리**

하려고 한다.

또 하나 중요한 핵심:

> **상태 동기화는 매우 비싸다.**

왜냐하면:

- network 필요
- lock 필요
- consistency 보장 필요

그래서:

- eventual consistency
- immutable event
- append-only log

같은 개념이 등장한다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**Process 상태**

```bash
ps
top
htop
```

**메모리 상태**

```bash
free
vmstat
cat /proc/meminfo
```

**파일 상태**

```bash
lsof
```

### Runtime

관찰 포인트:

- session count
- cache size
- queue state
- object lifecycle
- memory retention

### Kubernetes

**Pod 상태**

```bash
kubectl get pods
```

상태 예:

- Running
- Pending
- CrashLoopBackOff

**Stateful Workload**

예:

- StatefulSet
- PersistentVolume
- distributed storage

**Cluster 상태**

관찰 포인트:

- replica synchronization
- leader election
- etcd consistency

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*