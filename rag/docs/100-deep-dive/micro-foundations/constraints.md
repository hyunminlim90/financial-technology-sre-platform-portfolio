# Constraints (제약 조건)
## Micro Foundations — 범용 시스템 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

Constraints(제약 조건)는:

> 시스템이 안정성과 무결성을 유지하기 위해 태스크와 데이터의 행동 범위를 제한하는 규칙과 한계선

쉽게 말하면:

> **"여기까지는 가능하지만 그 이상은 허용하지 않는다"**

를 정의하는 시스템의 안전 장치다.

제약 조건은 시스템에게:

- 얼마나 자원을 쓸 수 있는지
- 어디까지 접근 가능한지
- 어떤 형식만 허용되는지
- 언제 중단해야 하는지

를 강제한다. 즉:

> **Constraints는 시스템 붕괴를 막는 보호 경계이다.**

---

## 2. 시스템 어디에서 등장하는가

제약 조건은 시스템 전체에 존재한다.

**하드웨어**
- 전력 한계
- 발열 한계
- 메모리 크기
- 저장 용량

**운영체제**
- 프로세스 권한
- 메모리 보호
- CPU quota
- 파일 접근 제한

**런타임**
- heap limit
- execution timeout
- thread limit
- sandbox restriction

**네트워크**
- bandwidth limit
- rate limit
- connection limit
- packet size limit

**데이터 저장소**
- primary key
- unique constraint
- foreign key
- transaction rule

**분산 시스템**
- quorum 조건
- replication factor
- retry limit
- consistency level

즉:

> **모든 안정적인 시스템은 Constraints 위에서 운영된다.**

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

Constraints는:

> **모든 자원의 사용 범위를 결정한다.**

특히 핵심은:

> **CPU + Memory + Network**

이다.

### CPU

제약 예시:

- CPU quota
- scheduling slice
- execution limit

### Memory

제약 예시:

- max allocation
- address boundary
- memory isolation

### Network

제약 예시:

- timeout
- bandwidth cap
- connection throttling

### Disk

제약 예시:

- storage quota
- write limit
- IOPS cap

---

## 4. 왜 중요한가

제약 조건이 없으면:

> **하나의 잘못된 Task가 환경 전체를 붕괴**

시킬 수 있다. 예:

- 무한 메모리 사용
- 무한 retry
- 과도한 CPU 점유
- 폭주하는 network request

반대로 제약이 너무 강하면:

- 정상 작업도 실패
- throughput 감소
- latency 증가
- 과도한 throttling

이 발생한다. 즉:

> **Constraints는 안정성과 성능 사이 균형 장치이다.**

---

## 5. 실제 장애와 어떤 관련이 있는가

제약 조건 관련 장애는 매우 흔하다.

### 1) OOM Kill

메모리 제한 초과. 결과:

- 프로세스 강제 종료
- 서비스 다운

### 2) CPU Throttling

CPU quota 초과. 결과:

- latency 폭증
- request timeout

### 3) Connection Exhaustion

연결 수 제한 도달. 결과:

- 새로운 요청 거부
- service unavailable

### 4) Timeout Constraint

시간 제한 초과. 결과:

- retry storm
- cascading failure

### 5) Constraint Misconfiguration

잘못된 제한 설정. 결과:

- 정상 서비스 장애
- 과도한 eviction
- scheduling failure

### 6) Data Integrity Constraint Failure

정합성 규칙 위반. 결과:

- transaction rollback
- inconsistent state 방지

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심은:

> **Constraints는 자원의 무한 사용을 막기 위한 경계 시스템이다.**

시스템은 항상:

> **유한한 자원**

위에서 동작한다. 따라서 Environment는 CPU, Memory, Network, Storage를 보호하기 위해 제약을 강제한다.

제약 조건의 핵심 역할:

- **Resource Protection** — 환경 전체 보호
- **Isolation** — 다른 Task 영향 차단
- **Fairness** — 자원 독점 방지
- **Stability** — 폭주 상황 차단
- **Consistency** — 데이터 정합성 유지

중요한 점:

> **Constraints는 단순 제한이 아니라 시스템 생존 메커니즘이다.**

또한 제약 조건은 두 형태가 존재한다.

**정적 제약 (static)**

미리 정의된 제한. 예:

- memory limit
- storage quota

**동적 제약 (dynamic)**

상황에 따라 변함. 예:

- backpressure
- adaptive throttling
- circuit breaker

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**자원 제한 확인**

```bash
ulimit -a
```

**메모리 상태**

```bash
free -h
cat /proc/meminfo
```

**CPU 상태**

```bash
top
mpstat
vmstat
```

**프로세스 제한**

```bash
cat /proc/<pid>/limits
```

### Runtime

관찰 포인트:

- heap limit
- execution timeout
- queue limit
- retry threshold
- resource saturation

### Kubernetes

제약 조건이 매우 중요하게 사용된다.

**Resource Limit**
- cpu limit
- memory limit

**Scheduling Constraint**
- node selector
- affinity
- taint/toleration

**관찰 명령**

```bash
kubectl describe pod
kubectl top pod
kubectl get events
```

**핵심 관찰 포인트**
- `OOMKilled`
- throttling
- eviction
- pending scheduling
- resource pressure

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*