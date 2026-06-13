# Task (작업)
## Micro Foundations — 범용 시스템 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

Task(태스크)는:

> **시스템이 실제로 수행해야 하는 실행 단위**

쉽게 말하면:

> **"컴퓨터에게 맡겨진 일거리"**

이다.

예를 들면:

- 데이터 읽기
- 계산 수행
- 파일 저장
- 네트워크 전송
- 로그 기록
- 사용자 요청 처리

같은 것들이 모두 태스크다.

중요한 점은:

> **Task는 "실행 대상"이라는 점**

이다. 즉:

- 데이터(Data)는 재료
- 로직(Logic)은 규칙
- 상태(State)는 현재 상황
- 태스크(Task)는 실제 수행되는 일

이다.

---

## 2. 시스템 어디에서 등장하는가

태스크는 거의 시스템 전체에서 등장한다.

**운영체제(OS)**
- process 실행
- thread 실행
- scheduler 대상

**애플리케이션**
- 요청 처리
- 계산 작업
- 이벤트 처리
- background job

**분산 시스템**
- 메시지 소비
- 비동기 작업
- replication
- batch execution

**인프라**
- container workload
- VM 실행
- orchestration 작업

즉:

> **시스템이 움직인다는 것은 결국 Task들이 실행된다는 뜻이다.**

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

Task는 거의 모든 자원을 사용한다. 하지만 핵심은:

> **CPU + Memory**

이다.

### CPU

Task는 CPU 시간을 소비한다. 예:

- 계산
- 조건 판단
- 스케줄링
- 암호화
- 압축

### Memory

Task는 실행 중 상태와 데이터를 메모리에 유지한다. 예:

- stack
- heap
- buffer
- context

### Network

통신 기반 Task는 네트워크 자원을 사용한다. 예:

- API 호출
- replication
- packet transmission

### Disk

저장 기반 Task는 디스크 I/O를 사용한다. 예:

- logging
- persistence
- snapshot
- backup

---

## 4. 왜 중요한가

시스템은 결국:

> **Task를 얼마나 안정적이고 빠르게 처리하느냐**

로 성능이 결정된다. 즉:

- latency
- throughput
- scalability
- availability

모두 Task 처리 능력과 연결된다.

특히 현대 인프라에서는:

> **수백만 개의 Task가 동시에 존재**

할 수 있다. 따라서:

- 어떤 Task를 먼저 실행할지
- 얼마나 오래 CPU를 줄지
- 어떤 자원을 배정할지

결정하는 것이 핵심 운영 문제다.

---

## 5. 실제 장애와 어떤 관련이 있는가

Task 관련 장애는 매우 흔하다.

### 1) Task Starvation

특정 Task가 계속 실행 기회를 못 받음. 결과:

- 요청 timeout
- queue 적체
- latency 증가

### 2) Task Explosion

Task가 과도하게 생성됨. 결과:

- CPU saturation
- memory exhaustion
- scheduler overload

### 3) Blocking Task

하나의 Task가 오래 점유. 결과:

- 전체 처리 정체
- throughput 감소

### 4) Deadlock

Task들이 서로 자원을 기다림. 결과:

- 시스템 멈춤
- transaction hang

### 5) Orphan Task

Task가 종료되지 않고 남음. 결과:

- resource leak
- zombie workload
- memory leak

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심은:

> **Task는 "스케줄링되는 실행 흐름"이다.**

즉 Task는:

- 생성 (Create)
- 대기 (Wait)
- 실행 (Run)
- 중단 (Suspend)
- 종료 (Terminate)

상태를 반복한다.

운영체제와 런타임은:

> **제한된 CPU와 메모리를 여러 Task에게 분배**

한다. 따라서 중요한 개념:

- **Scheduling** — 누가 언제 실행되는가
- **Priority** — 어떤 Task가 더 중요한가
- **Concurrency** — 여러 Task를 동시에 처리하는 방식
- **Isolation** — Task끼리 서로 영향을 최소화
- **Context Switching** — Task 전환 시 실행 상태 저장/복구

또 중요한 점:

> **Task는 상태(State)와 컨텍스트(Context)를 가진다.**

즉:

- Task 자체는 일거리
- Context는 실행 환경
- State는 현재 진행 상황

이다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**프로세스/스레드**

```bash
ps
top
htop
```

**스케줄링 상태**

```bash
pidstat
vmstat
sar
```

**task 정보**

```bash
/proc/<pid>/task
```

**load 확인**

```bash
uptime
cat /proc/loadavg
```

### Runtime

관찰 포인트:

- active tasks
- pending queue
- worker utilization
- execution latency
- blocking duration

### Kubernetes

**Pod/Container**

Task 실행 단위로 간주 가능.

**Job/CronJob**

일회성 또는 주기성 Task.

**관찰 포인트**

```bash
kubectl top
kubectl describe
kubectl logs
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*