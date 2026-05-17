# Store (스토어 / 메모리 저장)
## **Micro Foundations — 컴퓨터 구조 / 메모리 계층 / 시스템 성능 관점**

> 정독: 0회

## 1. 이 기술이 무엇인가

**Store**는:

> CPU 내부 데이터를 **외부 메모리(RAM)에 기록하는 행위**

즉, 레지스터 안의 결과를 시스템 전체가 공유하는 메모리 공간으로 내려보내는 과정이다.

반대 개념과 비교:

| 동작 | 방향 |
|------|------|
| **Load** | RAM → Register |
| **Store** | Register → RAM |

> **핵심:** Store는 **CPU 내부 상태를 외부 메모리 세계에 반영하는 과정**이다.

---

## 2. 시스템 어디에서 등장하는가

Store는 **거의 모든 시스템**에서 등장한다.

### CPU 명령어 실행
- 변수 저장, 배열 수정, 포인터 기록, 함수 결과 반영

### 운영체제 커널
- page table 수정, process state 저장, interrupt state 반영

### 네트워크 스택
- packet buffer 기록, socket queue 적재, DMA memory update

### 데이터베이스
- transaction buffer write, WAL buffer 기록, shared memory 상태 갱신

### 멀티코어 시스템
- shared state synchronization, lock variable update, atomic operation

> **결론:** Store는 **시스템 상태(State)를 실제로 바꾸는 핵심 동작**이다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Memory**이다.

| 자원 | 영향 |
|------|------|
| **Memory** | cache hierarchy, RAM latency, memory bandwidth — 가장 핵심 |
| **CPU** | Store 완료 전 pipeline stall 가능 |
| **Cache** | Store 대부분은 L1/L2 cache 먼저 기록 |
| **NUMA** | 멀티소켓 환경에서 원격 메모리 store 비용 큼 |
| **Disk** | Dirty page flush 시 RAM → Disk writeback 발생 (간접) |

> **핵심:** Store는 **메모리 계층 전체의 부하를 유발하는 핵심 쓰기 동작**이다.

---

## 4. 왜 중요한가

CPU는 계산만 하는 기계가 아니다. 진짜 중요한 건 **"결과를 시스템 상태로 반영하는 것"**이다.

다음은 모두 **Store를 포함**한다:

- 잔액 차감
- 주문 상태 변경
- 로그 기록
- 락 획득

> **Store가 없다면 시스템 상태 변화가 존재하지 않는다.**

또한 성능 관점에서도 중요하다. **CPU 속도 ≫ RAM 속도**이기 때문에 Store 설계가 잘못되면:

- latency 증가
- memory contention
- cache coherency 폭발
- pipeline stall

이 발생한다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 1) Store Buffer Saturation
```
CPU 내부 store queue 가득 참
  ↓
pipeline stall → IPC 감소 → latency spike
```

### 2) Cache Coherency Storm
```
멀티코어 환경 — 여러 코어가 같은 메모리 수정
  ↓
MESI invalidation 폭증 → cache ping-pong → throughput 급락
```

### 3) False Sharing
```
다른 변수인데 같은 cache line 공유
  ↓
store만으로도 성능 폭락 가능
```

### 4) Memory Ordering 문제
```
Store 순서 재배치 발생
  ↓
race condition → stale read → distributed inconsistency
```

### 5) NUMA Penalty
```
원격 메모리 store 발생
  ↓
cross-socket latency 증가
```

### 6) Dirty Page 폭증
```
대량 store → dirty memory 증가
  ↓
writeback storm → disk flush spike
```

> **핵심:** Store는 단순 저장이 아니라, **시스템 consistency와 성능의 핵심 위험 지점**이다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 기본 Store 흐름

```
Register → Store Buffer → Cache → RAM
```

실제 RAM까지 즉시 가지 않을 수도 있다.

### 핵심 메커니즘

| 메커니즘 | 의미 |
|----------|------|
| **Effective Address Calculation** | ALU가 저장 주소 계산 (base address + offset) |
| **Store Buffer** | RAM 쓰기 완료까지 기다리지 않고 임시 queue에 넣고 다음 명령 수행 — pipeline stall 방지 목적 |
| **Cache Write** | 실제 대부분의 store는 L1 cache에 먼저 기록 |
| **Memory Barrier** | 멀티코어 환경에서 store 순서 보장 — fence/barrier/synchronization 필요 |

> **핵심:** 현대 CPU의 Store는 즉시 RAM 기록이 아니라, **버퍼·캐시·재정렬을 동반하는 비동기 시스템**이다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**Dirty Memory**
```bash
cat /proc/meminfo | grep -E "Dirty|Writeback"
```
관찰: Dirty(미반영 데이터), Writeback(flush 중인 데이터)

**Store stall / CPU 분석**
```bash
perf stat
perf top
vmstat
sar
```

**NUMA**
```bash
numastat
numactl --hardware
```

---

### Runtime

관찰 포인트:
- store queue saturation
- cache coherency overhead
- dirty page 증가 추이

---

### Kubernetes

| 관찰 영역 | 증상 |
|-----------|------|
| **CPU throttling** | latency spike |
| **Memory pressure** | dirty page 폭증 |
| **I/O burst** | flush storm, writeback spike |

**DB 시스템에서 대량 store 발생 시:**

```
WAL flush 증가 → fsync spike → checkpoint storm
```

> **핵심:** 운영 환경에서는 store 자체보다 **store로 인해 발생하는 flush와 coherency 비용**이 더 위험하다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*