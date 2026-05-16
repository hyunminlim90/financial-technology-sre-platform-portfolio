# Environment (환경)
## Micro Foundations — 범용 시스템 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

Environment(환경)은:

> 데이터와 태스크가 실제로 존재하고 실행될 수 있도록  
> 자원과 규칙을 제공하는 전체 공간

이다.

쉽게 말하면:

> **시스템이 살아 움직일 수 있는 "무대"**

이다.

중요한 점은:

> **Environment는 단순 장소가 아니라 실행 조건 전체**

라는 것이다. 즉 환경에는:

- 하드웨어 자원
- 운영 규칙
- 실행 제한
- 연결 구조
- 보안 정책
- 네트워크 조건
- 저장 구조

같은 것들이 모두 포함된다.

---

## 2. 시스템 어디에서 등장하는가

환경(Environment)은 시스템 거의 모든 계층에 존재한다.

**하드웨어 환경**
- CPU 구조
- 메모리 용량
- 디스크 구성
- 네트워크 장비

**운영체제 환경**
- 프로세스 관리
- 파일 시스템
- 가상 메모리
- 스케줄링 정책

**런타임 환경**
- 실행 규칙
- 메모리 격리
- 라이브러리 로딩
- 실행 컨텍스트

**인프라 환경**
- VM
- Container
- Cluster
- Cloud Region
- Network Topology

**서비스 환경**
- Dev
- Test
- Stage
- Production

즉:

> **모든 시스템은 특정 Environment 안에서만 존재 가능하다.**

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

Environment는 특정 자원 하나가 아니라:

> **모든 자원의 사용 규칙 자체**

에 영향을 준다. 하지만 핵심은:

> **CPU + Memory + Network + Storage를 어떻게 분배하고 제한하느냐**

이다.

### CPU
- 실행 가능량 결정
- 스케줄링 제한
- 동시 처리량 결정

### Memory
- 주소 공간 제공
- 격리 범위 결정
- 상태 유지 공간 제공

### Network
- 연결 가능 범위 결정
- 통신 지연 결정
- 라우팅 및 격리 수행

### Disk / Storage
- 데이터 보존 환경 제공
- persistence 보장
- write/read 성능 결정

---

## 4. 왜 중요한가

같은 태스크라도:

> **어떤 환경에서 실행되느냐에 따라 완전히 다른 결과**

가 나온다. 예:

- 메모리 부족 환경
- 느린 네트워크 환경
- 제한된 CPU 환경
- 불안정한 디스크 환경

에서는 동일한 프로그램도 다르게 동작한다. 즉:

> **환경은 시스템의 행동 방식 자체를 결정한다.**

특히 분산 시스템에서는:

- 환경 차이
- 설정 차이
- 네트워크 차이
- 시간 차이

가 장애 원인이 되는 경우가 매우 많다.

---

## 5. 실제 장애와 어떤 관련이 있는가

환경(Environment) 문제는 대형 장애의 핵심 원인이다.

### 1) 환경 불일치 (Environment Drift)

개발 환경과 운영 환경이 다름. 결과:

- 운영에서만 장애 발생
- dependency mismatch
- runtime crash

### 2) 자원 고갈

환경 자체의 CPU/RAM 부족. 결과:

- OOM Kill
- throttling
- latency spike

### 3) 환경 격리 실패

환경 간 영향 전파. 결과:

- noisy neighbor
- shared resource contention
- cascading failure

### 4) 네트워크 환경 문제

환경 간 연결 실패. 결과:

- timeout
- packet loss
- service unreachable

### 5) 환경 상태 오염

설정이나 상태가 꼬임. 결과:

- inconsistent behavior
- deployment failure
- unstable recovery

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심은:

> **Environment는 실행 규칙과 자원 경계를 정의한다.**

즉 시스템은:

> **Environment 안에서만 존재 가능하다.**

환경이 제공하는 것:

- **Resource** — CPU, Memory, Storage, Network
- **Isolation** — 서로 다른 작업 분리, 충돌 최소화
- **Scheduling Rule** — 누가 얼마나 실행되는가
- **Security Boundary** — 접근 가능한 범위, 권한 제한
- **Runtime Constraint** — 실행 제한, quota, timeout

중요한 점:

> **Environment는 Task보다 상위 개념이다.**

관계는:

```
Environment 위에서
  Task가 실행되고
  Context가 유지되며
  Data가 처리되고
  State가 생성된다
```

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**환경 변수**

```bash
env
printenv
```

**시스템 자원**

```bash
top
free -h
vmstat
iostat
```

**커널 환경**

```bash
uname -a
sysctl -a
```

**파일 시스템 환경**

```bash
mount
df -h
```

### Runtime

관찰 포인트:

- runtime memory limit
- task isolation
- execution quota
- runtime configuration
- dependency resolution

### Kubernetes

**환경 구성**
- Namespace
- Node
- Pod
- Network Policy
- Resource Limit

**관찰 명령**

```bash
kubectl describe
kubectl top
kubectl get nodes
kubectl get events
```

**핵심 관찰 포인트**
- resource pressure
- pod eviction
- throttling
- node condition
- scheduling failure

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*