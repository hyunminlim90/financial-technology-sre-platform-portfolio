# Condition (조건)
## Micro Foundations — 범용 시스템 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

Condition(조건)은:

> 현재 상황을 평가하여  
> 시스템의 다음 행동을 결정하는 판별 기준

이다.

쉽게 말하면:

> **"지금 실행 가능한가?"**  
> **"다음 단계로 가도 되는가?"**

를 판단하는 기준이다.

Condition은 시스템에게:

- 실행 여부
- 분기 방향
- 허용/거부
- 성공/실패
- 정상/비정상

같은 결정을 내리게 만든다. 즉:

> **Condition은 시스템의 의사결정 스위치이다.**

---

## 2. 시스템 어디에서 등장하는가

Condition은 거의 모든 계층에서 등장한다.

**하드웨어**
- 연산 결과 플래그
- 비교 결과
- 인터럽트 상태

**운영체제**
- 프로세스 상태
- 자원 가용 여부
- 스케줄링 가능 여부

**애플리케이션**
- 입력 검증
- 권한 검사
- 상태 분기

**분산 시스템**
- health check
- quorum 상태
- replication 상태

**인프라**
- node readiness
- memory pressure
- network availability

즉:

> **시스템이 "판단"하는 모든 순간에 Condition이 존재한다.**

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

Condition 자체는:

> **제어 흐름(Control Flow)**

에 가장 직접적인 영향을 준다. 하지만 실제로는:

> **CPU + Memory**

와 강하게 연결된다.

### CPU

Condition 평가를 위해:

- 비교 연산
- 분기 연산
- 플래그 계산

이 수행된다.

### Memory

Condition은 상태(State)를 읽어야 한다. 예:

- 현재 값
- 이전 결과
- 메타데이터
- 상태 플래그

### Network

분산 환경에서는:

- 연결 가능 여부
- 응답 성공 여부
- timeout 여부

가 Condition이 된다.

### Disk

스토리지 환경에서는:

- write 가능 여부
- sync 완료 여부
- persistence 성공 여부

가 Condition 역할을 한다.

---

## 4. 왜 중요한가

Condition이 없다면 시스템은:

> **상황 판단 없이 무조건 직진**

하게 된다. 즉:

- 오류 상황 처리 불가
- 예외 대응 불가
- 안전성 부족
- 분기 제어 불가

상태가 된다.

현대 시스템은 거의 모두:

- If → Then
- Else
- Retry
- Fallback
- Abort

같은 구조 위에서 움직인다. 이 모든 흐름의 중심이:

> **Condition이다.**

---

## 5. 실제 장애와 어떤 관련이 있는가

Condition 문제는 매우 위험하다.

### 1) 잘못된 Condition

조건 판정 오류. 결과:

- 잘못된 승인
- 잘못된 차단
- 데이터 손상

### 2) Race Condition

동시에 상태가 바뀜. 결과:

- 중복 처리
- 정합성 깨짐
- lost update

### 3) Stale Condition

오래된 상태를 기준으로 판단. 결과:

- 잘못된 분기
- outdated decision
- inconsistent behavior

### 4) Infinite Retry Condition

종료 조건이 없음. 결과:

- CPU 폭주
- queue 적체
- resource exhaustion

### 5) Split Brain Condition

분산 시스템에서 서로 다른 조건 인식. 결과:

- 이중 처리
- 상태 충돌
- 데이터 corruption

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심은:

> **Condition은 "현재 상태를 평가하여 실행 흐름을 결정"한다.**

Condition의 기본 구조:

```
IF condition == true
THEN execute
ELSE branch
```

즉 시스템은:

- 상태(State)를 읽고
- Condition을 평가하고
- 실행 흐름(Task)을 결정

한다.

중요한 점:

> **Condition은 Context와 State에 의존한다.**

예:

- 현재 상태
- 실행 환경
- 권한 정보
- 시간 정보
- 자원 상황

등이 모두 판단 재료다.

또한 Condition은:

> **정적(static)이 아니라 실시간(dynamic)이다.**

즉:

- 메모리 사용량
- 네트워크 상태
- 사용자 상태
- 시스템 부하

변화에 따라 계속 달라진다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**시스템 상태 확인**

```bash
top
vmstat
uptime
```

**프로세스 상태**

```bash
ps
htop
```

**메모리 압박 상태**

```bash
free -h
cat /proc/meminfo
```

**디스크 상태**

```bash
iostat
df -h
```

### Runtime

관찰 포인트:

- task state
- retry state
- timeout condition
- queue saturation
- backpressure trigger

### Kubernetes

Condition이라는 용어가 실제 공식 필드로 존재한다.

**Node Condition**

예:

- `Ready`
- `MemoryPressure`
- `DiskPressure`
- `NetworkUnavailable`

**확인 명령**

```bash
kubectl describe node
```

**Pod 상태**

```bash
kubectl get pods
kubectl describe pod
```

**핵심 관찰 포인트**

- readiness
- liveness
- scheduling condition
- resource pressure
- eviction state

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*