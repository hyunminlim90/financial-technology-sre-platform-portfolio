# System Component (시스템 컴포넌트)
## **Micro Foundations — 범용 시스템/분산 인프라 관점**

> 정독: 0회

## 1. 이 기술이 무엇인가

**System Component(시스템 컴포넌트)**는:

> 시스템 아키텍처를 구성하는 **독립적인 기능 단위(Unit)**

이다. 쉽게 말하면 **"전체 시스템을 이루는 하나의 블록"**이다.

컴포넌트는 다음을 가진다:

| 속성 | 의미 |
|------|------|
| **Role** | 역할 |
| **Responsibility** | 책임 |
| **Boundary** | 경계 |
| **Interface** | 인터페이스 |

**예시:** API Gateway, Application Server, Database, Cache, Queue, Load Balancer, Storage, DNS, Authentication Service — 모두 시스템 컴포넌트다.

> **핵심:** 컴포넌트는 **"독립적으로 존재 가능하며, 다른 컴포넌트와 규격화된 방식으로 협력하는 단위"**이다.

---

## 2. 시스템 어디에서 등장하는가

System Component는 시스템 **전체**에 존재한다.

### 하드웨어 레벨
- CPU, NIC, SSD, Memory Module, Server Node

### 운영체제 레벨
- Process Scheduler, Virtual Memory Manager, File System, Network Stack

### 미들웨어 레벨
- DBMS, Kafka, Redis, MQ

### 애플리케이션 레벨
- Auth Service, Payment Service, Order Service

### 클라우드/인프라 레벨
- Kubernetes Node, API Gateway, Service Mesh, Object Storage

> **결론:** 컴포넌트는 **"시스템을 이루는 최소 운영 단위"**이다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

컴포넌트는 **유형에 따라 자원 특성이 다르다.**

| 유형 | 예시 | 특징 |
|------|------|------|
| **CPU 중심** | encryption, compression, analytics engine | 연산 집약적(CPU-bound) |
| **Memory 중심** | cache, in-memory DB, buffering system | 메모리 집약적(Memory-bound) |
| **Network 중심** | gateway, proxy, message broker | 네트워크 집약적(Network-bound) |
| **Disk 중심** | database, object storage, logging system | I/O 집약적(IO-bound) |

현실에서는 대부분 **혼합형**이다.

> **핵심:** 컴포넌트는 **각자 다른 병목(Bottleneck) 특성**을 가진다.

---

## 4. 왜 중요한가

현대 시스템은 "거대한 단일 프로그램"이 아니라 **"수많은 컴포넌트의 협력체"**이다.

따라서 안정성·확장성·장애 격리·성능·운영성 모두 **컴포넌트 구조에 의해 결정**된다.

**좋은 컴포넌트 구조:**
- fault isolation 가능
- independent scaling 가능
- replacement 가능
- observability 가능

**나쁜 구조가 만드는 것:**
- tight coupling
- SPOF
- cascading failure
- deployment lock-in

> **결론:** 시스템 품질은 **컴포넌트 분리 수준**에 크게 좌우된다.

---

## 5. 실제 장애와 어떤 관련이 있는가

컴포넌트는 **장애의 기본 단위**다.

### 1) SPOF (Single Point of Failure)
```
모든 요청이 단 하나의 DB 컴포넌트에 의존
  ↓
DB failure → 전체 시스템 failure
```

### 2) Resource Exhaustion
```
cache memory exhaustion / queue disk full / gateway connection exhaustion
  ↓
timeout → retry storm → cascading failure
```

### 3) Interaction Fault
```
A waits B → B waits C → C overloaded
  ↓
chain timeout → thread starvation → distributed instability
```

### 4) Misplaced Responsibility
```
하나의 컴포넌트가 너무 많은 역할 수행
  ↓
scaling difficulty → unstable deployment → debugging nightmare
```

### 5) No Isolation
```
한 컴포넌트의 CPU spike가 전체 node 자원 고갈
  ↓
noisy neighbor problem → multi-service instability
```

> **핵심:** 좋은 시스템은 **"컴포넌트 일부 장애"와 "시스템 전체 장애"를 분리**한다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

컴포넌트 설계의 핵심은:

> **"경계(Boundary)"**

좋은 컴포넌트는 책임이 명확하고, 인터페이스가 명확하며, 내부 구현이 숨겨지고, 독립적으로 운영 가능해야 한다.

### 핵심 메커니즘

| 메커니즘 | 의미 |
|----------|------|
| **Encapsulation** | 내부 구현 숨김 |
| **Interface Contract** | 입출력 규격 고정 |
| **Loose Coupling** | 컴포넌트 의존 최소화 |
| **Independent Scaling** | 독립 확장 가능 |
| **Fault Isolation** | 장애 격리 가능 |
| **Stateless / Stateful Separation** | 상태 관리 분리 |
| **Resource Isolation** | CPU/Memory/Network 격리 |
| **Observability** | 상태 추적 가능 |

> **핵심 철학:** 컴포넌트는 **"독립적으로 실패 가능해야 하며, 독립적으로 복구 가능해야 한다."**

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

컴포넌트는 운영 환경 전체에서 관측된다.

### Linux

**Process 단위**
```bash
ps
top
htop
```
관찰: CPU usage, memory usage, thread count

**Network 단위**
```bash
ss -ant
ip addr
ethtool
```
관찰: socket state, connection count, NIC saturation

**Storage 단위**
```bash
iostat
df
iotop
```
관찰: disk latency, queue depth, fs usage

---

### Runtime

관찰 포인트:
- thread pool
- queue depth
- GC pressure
- connection pool

---

### Kubernetes

컴포넌트는 보통 **Pod · Deployment · StatefulSet · Service** 형태로 관측된다.

| 관찰 영역 | 명령어 | 관찰 |
|-----------|--------|------|
| **상태 확인** | `kubectl get pods` / `kubectl describe pod` | restart count, readiness, liveness |
| **자원 확인** | `kubectl top pod` / `kubectl top node` | throttling, OOM |
| **상호작용 확인** | `kubectl logs` / `kubectl get svc` | traffic flow |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*