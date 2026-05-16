# Data Update (데이터 갱신)
## Micro Foundations — 범용 시스템 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

**Data Update** 는:

> 기존 상태(State)를 **새로운 값으로 변경하는 행위**

핵심은 단순 "쓰기(Write)"가 아니다. Update는:

- 기존 상태를 읽고
- 계산하고
- 새로운 상태로 교체하며
- 시스템의 현재 시점을 변경

하는 과정이다.

즉:

> **Update = "시스템의 현재 상태를 바꾸는 행위"**

---

## 2. 시스템 어디에서 등장하는가

Update는 **시스템 전체**에서 발생한다.

### 메모리 계층
- 변수 값 변경
- cache entry 수정
- page dirty marking

### 저장소 계층
- DB row update
- file overwrite
- metadata update

### 네트워크 계층
- distributed state sync
- replication state update
- session state update

### 애플리케이션 계층
- 사용자 상태 변경
- 주문 상태 변경
- 세션 갱신
- 설정 변경

### 분산 시스템
- leader state update
- consensus log append
- replica synchronization

즉 시스템은 본질적으로 **계속 상태를 Update 하면서 움직인다**.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접 영향받는 자원: **Memory + Disk**

| 자원 | Data Update와의 관계 |
|------|-------------------|
| Memory | dirty page 생성, cache invalidation, memory synchronization 발생 |
| Disk | write I/O, journal write, fsync, block allocation 등 발생 |
| Network | 분산 환경에서 replication / state propagation / consistency synchronization 트래픽 발생 |
| CPU | Update 자체보다 lock handling / consistency checking / synchronization 비용이 큼 |

---

## 4. 왜 중요한가

시스템은 결국 **현재 상태(State)** 로 동작한다.

Update가 없다면 상태 변화 없음 → 비즈니스 진행 불가 → 시스템이 정지된 것과 동일하다.

반대로 **잘못된 Update**는:

> ⚠️ 데이터 꼬임 / 정합성 붕괴 / 중복 처리 / 상태 불일치로 이어진다.

즉:

> **Update는 시스템의 "역사"를 바꾸는 행위**이다.

---

## 5. 실제 장애와 어떤 관련이 있는가

Update는 Read보다 **훨씬 위험**하다. 왜냐하면 **상태를 변경하기 때문**이다.

### 1) Race Condition
동시에 여러 Update 발생 시 값 덮어쓰기 → lost update → inconsistent state 발생

### 2) Lock Contention
동일 상태를 동시에 수정 시 latency 증가 → throughput 감소 → deadlock 가능

### 3) Partial Update
중간 실패 시 절반만 반영 → state corruption → orphan state 발생 가능

### 4) Delayed Update
Replica 또는 cache 갱신 지연 시 stale read → eventual consistency 문제 → 사용자 상태 불일치 발생

### 5) Write Amplification
작은 Update가 내부적으로 큰 쓰기 발생 시 SSD wear 증가 → IOPS 포화 → storage latency 증가

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심은 **Update는 "상태 전이(State Transition)"** 라는 점이다.

예:
```
PENDING  → APPROVED
ACTIVE   → INACTIVE
OLD VALUE → NEW VALUE
```

즉 Update는 단순 값 수정이 아니라 **시스템의 현재 세계관(State)을 바꾸는 행위**이다.

### Update는 Read보다 비싸다

| 이유 | 설명 |
|------|------|
| consistency 유지 필요 | 모든 계층 상태 일관성 보장 |
| synchronization 필요 | 동시 접근 제어 |
| durability 보장 필요 | 영구 저장 확인 |
| replication 필요 | 복제 전파 |

그래서 많은 시스템은 Read 최적화보다 **Write(Update) 최적화가 더 어렵다**.

### Update 이후 전체 계층 동기화

Update는 단일 행위가 아니라 **시스템 전체 상태 동기화 과정**에 가깝다:

```
CPU Cache
  ↓
RAM
  ↓
Filesystem Cache
  ↓
Storage
  ↓
Replica
  ↓
Backup
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Memory Dirty 상태
```bash
vmstat 1
cat /proc/meminfo
```
관찰 포인트: `Dirty`, `Writeback`

### Disk Write 상태
```bash
iostat -x 1
iotop
```

### Lock 관련 상태
```bash
pidstat -u 1
perf lock
```

### Runtime 관찰 포인트
- `lock contention`
- `update latency`
- `transaction conflict`
- `state synchronization`
- `flush frequency`

### Kubernetes Persistent Volume Write 상태
- `volume latency`
- `write throughput`
- `fsync delay`

### Kubernetes Distributed Update 상태
- `etcd write latency`
- `controller reconciliation`
- `state propagation`

### Observability 대표 지표
- `write latency`
- `commit latency`
- `lock wait`
- `replication lag`

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*