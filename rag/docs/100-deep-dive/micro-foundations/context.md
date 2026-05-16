# Context (컨텍스트)
## Micro Foundations — 범용 시스템 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

Context(컨텍스트)는:

> 어떤 데이터나 작업이 현재 어떤 환경과 조건 속에서 실행되고 있는지를 설명하는 주변 정보의 집합

쉽게 말하면:

> 데이터 자체가 아니라  
> "그 데이터가 어떤 상황에 놓여 있는가"

를 의미한다.

예:

```
amount = 50000
```

이 숫자만 보면 의미가 불완전하다. 하지만:

```
currency = KRW
user     = user-1
permission = allowed
region   = KR
requestId = abc123
```

같은 주변 정보가 붙으면:

- 무엇인지
- 누가 요청했는지
- 허용되는지
- 어디서 왔는지

를 해석할 수 있다. 즉:

> **Context는 시스템의 "현재 실행 배경"이다.**

<details>
<summary>Deep Dive</summary></br>

Data(데이터) [[M]](../../100-deep-dive/micro-foundations/data.md)  
Task(작업) [[M]](../../100-deep-dive/micro-foundations/task.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

컨텍스트는 거의 모든 계층에 존재한다.

**CPU / OS**
- 실행 중 레지스터 값
- Program Counter
- Thread 상태
- Process 상태

**Runtime**
- 현재 요청 정보
- 인증 정보
- transaction scope
- trace 정보

**네트워크**
- source/destination
- protocol
- session state

**분산 시스템**
- trace id
- correlation id
- tenant 정보
- replication metadata

즉 시스템이:

> **"지금 어떤 상황인지" 판단해야 한다면 컨텍스트가 필요하다.**

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적 영향:

> **Memory + CPU**

### Memory

컨텍스트는 대부분 메모리에 유지된다. 예:

- thread-local data
- session state
- request metadata
- execution environment

### CPU

컨텍스트를 저장/복구하는 작업이 필요하다. 대표 예:

- Context Switching

### Network

분산 시스템에서는 컨텍스트를 전파(propagation)해야 한다. 예:

- trace id 전달
- auth metadata 전달
- distributed transaction context 전달

### Disk

간접 영향. 예:

- audit log
- trace log
- checkpoint
- snapshot

---

## 4. 왜 중요한가

컨텍스트가 없으면:

> **데이터는 의미를 잃는다.**

예:

```
10000
```

이라는 값은 금액인지, user id인지, timeout 값인지 알 수 없다.

또한 시스템은:

- 권한
- 실행 흐름
- 현재 상태
- 이전 기록

을 알아야 안전하게 동작한다. 즉:

> **Context는 시스템의 "판단 근거"이다.**

---

## 5. 실제 장애와 어떤 관련이 있는가

컨텍스트 문제는 매우 위험하다.

### 1) Context Loss

실행 중 컨텍스트 유실. 결과:

- 인증 정보 손실
- trace 연결 끊김
- transaction orphan 발생

### 2) Wrong Context

잘못된 컨텍스트 사용. 결과:

- 다른 사용자 데이터 접근
- 권한 오류
- 잘못된 요청 처리

### 3) Context Leak

이전 요청의 컨텍스트가 남음. 결과:

- 메모리 누수
- 데이터 오염
- security issue

### 4) Context Switching Overhead

컨텍스트 저장/복구 과다. 결과:

- CPU 낭비
- latency 증가
- throughput 감소

### 5) Distributed Context Break

분산 시스템에서 컨텍스트 전파 실패. 결과:

- observability 붕괴
- tracing 단절
- root cause 분석 실패

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심은:

> **Context는 "실행 환경의 기억"이다.**

즉 시스템은:

- 현재 누가 실행 중인지
- 무엇을 하고 있는지
- 어디까지 진행됐는지

를 계속 기억해야 한다.

또 매우 중요한 개념:

> **컨텍스트는 저장 → 전달 → 복구되어야 한다.**

**대표 사례**

**Context Switch**

CPU가 작업 전환 시:

- 현재 상태 저장
- 새 작업 상태 복구

를 수행.

**Distributed Context Propagation**

서비스 간 요청 이동 시:

- trace id
- auth metadata
- request metadata

를 함께 전달.

또 하나 중요한 핵심:

> **컨텍스트는 실행 흐름 전체를 연결한다.**

즉:

```
Request A
→ Service B
→ Queue C
→ Worker D
```

전체를 하나의 흐름으로 묶어주는 연결 정보가 Context다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**Process/Thread 상태**

```bash
ps
top
htop
```

**Context Switch 관찰**

```bash
vmstat
pidstat -w
sar -w
```

**스케줄링 상태**

```bash
cat /proc/<pid>/status
```

### Runtime

관찰 포인트:

- request scope
- session context
- execution metadata
- tracing context
- async propagation 상태

### Kubernetes

**Pod/Request 흐름**

관찰 포인트:

- request id
- trace id
- correlation id
- distributed tracing

**Cluster 레벨**

- service-to-service flow
- workload identity
- security context

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*