# 플랫폼 서비스 자원 (Platform Service Resources / PaaS)

> 정독: 0회

## 1. 이 기술이 무엇인가

플랫폼 서비스 자원(PaaS)은:

> 애플리케이션 실행에 필요한 운영체제·런타임·미들웨어·데이터베이스·오케스트레이션·확장 기능을 클라우드 제공자가 관리형 형태로 제공하는 플랫폼 계층 자원

사용자는 물리 서버, 가상 머신, 운영체제, 패치, 네트워크 구성 같은 인프라 관리보다 **애플리케이션 코드와 서비스 로직**에 집중할 수 있습니다.

### 핵심 특징

PaaS는 IaaS 위에서 동작하며 다음을 수행합니다.

- 실행 환경 자동 제공
- 운영 자동화
- 확장 자동화
- 플랫폼 표준화

---

## 2. 시스템 어디에서 등장하는가

현대 클라우드 및 분산 시스템 대부분에서 등장합니다.

### 애플리케이션 플랫폼

application runtime, managed app service

### 관리형 데이터베이스

PostgreSQL, MySQL, Redis, Kafka

### 컨테이너 플랫폼

Kubernetes managed service, container orchestration

### 서버리스 플랫폼

function execution platform

### DevOps 플랫폼

CI/CD platform, deployment automation

### AI 플랫폼

managed training platform, inference platform

### API 플랫폼

API gateway, service routing

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

PaaS는 여러 자원을 통합 관리합니다.

| 자원 | 영향 | 주요 지표 |
|---|---|---|
| **CPU** | 중요 | application runtime, container scheduling, autoscaling |
| **Memory** | 중요 | runtime heap, cache, stateful middleware |
| **Network** | 매우 중요 | service communication, load balancing, ingress/egress routing |
| **Disk** | 중요 | persistent storage, managed database, object persistence |

> PaaS는 단일 자원이 아니라 컴퓨트·스토리지·네트워크를 통합 추상화한 플랫폼 계층입니다.

---

## 4. 왜 중요한가

현대 클라우드 운영 핵심 계층입니다.

### 인프라 추상화

OS·패치·서버 운영 숨김.

### 개발 생산성 향상

애플리케이션 중심 개발 가능.

### 자동 확장

트래픽 변화 대응 가능.

### 운영 자동화

배포·복구·확장 자동 처리.

### 표준화

동일 플랫폼 기반 운영 가능.

### 대규모 분산 시스템 운영 가능

수천 노드 규모 운영 단순화.

### 안정성 향상

관리형 운영 정책 적용 가능.

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 클라우드 장애 상당수가 **PaaS 계층**에서 발생합니다.

### Autoscaling Failure

자동 확장 실패. → latency 증가, overload

### Control Plane Failure

플랫폼 제어 계층 장애. → deployment failure, orchestration failure

### Runtime Failure

실행 환경 문제. → application crash

### Managed DB Failure

관리형 데이터베이스 장애. → transaction outage

### Container Scheduling Failure

컨테이너 배치 실패. → pod pending, service unavailable

### Configuration Drift

플랫폼 설정 불일치. → deployment inconsistency

### Service Discovery Failure

서비스 탐색 실패. → internal communication failure

### Platform API Failure

클라우드 API 장애. → infrastructure provisioning failure

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

### ① Runtime Abstraction

실행 환경 추상화. 사용자는 코드만 배포.

### ② Managed Infrastructure

OS·패치·업데이트 자동 관리.

### ③ Autoscaling

부하 기반 자동 확장/축소.

### ④ Orchestration

분산 서비스 자동 배치 및 제어.

### ⑤ Load Balancing

트래픽 분산 처리.

### ⑥ Service Discovery

서비스 위치 자동 탐색.

### ⑦ Declarative Management

원하는 상태 기반 운영.

### ⑧ Immutable Deployment

불변 배포 기반 운영.

### ⑨ Health Monitoring

상태 감시 및 자동 복구.

### ⑩ Multi-Tenant Isolation

사용자 간 논리적 격리.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

**프로세스 상태**

```bash
ps -ef
top
htop
```

**메모리 상태**

```bash
free -h
vmstat
```

**네트워크 상태**

```bash
ss -ant
ip route
```

**컨테이너 상태**

```bash
docker ps
crictl ps
```

### Kubernetes

**Pod 상태**

```bash
kubectl get pods
```

**Deployment 상태**

```bash
kubectl get deploy
```

**Autoscaling 상태**

```bash
kubectl get hpa
```

**Service 상태**

```bash
kubectl get svc
```

**이벤트 확인**

```bash
kubectl get events
```

**로그 확인**

```bash
kubectl logs
```

### Cloud 환경

관측 대상: platform metrics, autoscaling events, deployment history, orchestration state, service health, API latency

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*