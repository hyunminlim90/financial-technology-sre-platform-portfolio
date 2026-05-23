# 소프트웨어 아키텍처 설계 (Software Architecture Design)

> 정독: 0회

## 1. 이 기술이 무엇인가

소프트웨어 아키텍처 설계는:

> 소프트웨어 시스템을 구성하는 컴포넌트, 데이터 흐름, 실행 구조, 자원 사용 방식, **의존성 방향을 정의하는 상위 구조 설계**

### 핵심 목표

아키텍처 설계는 다음을 결정합니다.

- 시스템을 어떻게 분리할 것인가
- 컴포넌트 간 통신을 어떻게 할 것인가
- 상태(State)를 어디서 관리할 것인가
- 장애를 어떻게 격리할 것인가
- 확장을 어떻게 할 것인가
- 동시성을 어떻게 제어할 것인가

### 핵심 특징

> 아키텍처는 코드 자체보다  
> **시스템의 구조와 규칙을 정의**합니다.

### 대표 요소

Layer, Module, Service, Interface, API, Message Flow, Data Model, Dependency Direction, Runtime Boundary

---

## 2. 시스템 어디에서 등장하는가

아키텍처 설계는 모든 소프트웨어 시스템에 존재합니다.

### Application System

웹 서비스, 게임 서버, 결제 시스템, AI 플랫폼

### Operating System

scheduler, virtual memory, filesystem, driver subsystem

### Distributed System

microservice, event-driven architecture, message queue, service mesh

### Cloud / Kubernetes

control plane, node architecture, container runtime, ingress structure

### Database System

storage engine, transaction layer, replication architecture

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

아키텍처 설계는 **모든 시스템 자원 사용 방식 전체에 영향**을 줍니다.  
그 중 특히 큰 것은 **CPU, Memory, Network**입니다.

| 자원 | 결정 항목 |
|---|---|
| **CPU** | thread model, scheduling pressure, lock contention, parallelism |
| **Memory** | object lifecycle, cache locality, heap growth, memory fragmentation |
| **Network** | RPC 구조, retry, timeout, backpressure (분산 시스템에서 특히 중요) |
| **Disk** | transaction model, WAL, batching, persistence policy |

---

## 4. 왜 중요한가

> 아키텍처는 **시스템의 장기적 안정성과 확장성을 결정**하기 때문입니다.

### 초기 구조가 중요한 이유

잘못된 아키텍처는 다음을 유발합니다.

- 병목 / 복잡도 폭증
- 장애 전파
- 배포 실패
- 확장 한계

### 코드보다 더 장기 영향

코드는 일부 수정 가능하지만, dependency structure / state model / communication pattern은 **시스템 전체를 지배**합니다.

### SRE 관점

운영 안정성 대부분은 **아키텍처 결정의 결과**입니다.

```
synchronous chain 과다
global lock
shared DB bottleneck
no isolation boundary
→ 장애 전파 발생
```

---

## 5. 실제 장애와 어떤 관련이 있는가

| 장애 유형 | 원인 |
|---|---|
| **Cascading Failure** | 서비스 간 동기 호출 체인으로 장애 전파 |
| **Single Point of Failure** | 중앙 컴포넌트 장애 시 전체 중단 |
| **Memory Explosion** | 잘못된 object lifecycle |
| **Deadlock** | 동시성 구조 실패 |
| **Network Saturation** | chatty architecture |
| **Retry Storm** | 무제한 retry 구조 |
| **Thundering Herd** | 동시 wake-up 구조 문제 |
| **Slow Query Amplification** | shared database bottleneck |
| **Deployment Coupling** | 강결합 구조로 롤백 불가능 |

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

핵심 메커니즘은 **8개**입니다.

| # | 메커니즘 | 설명 |
|---|---|---|
| 1 | **Component Separation** | 기능별 책임 분리 |
| 2 | **Dependency Direction** | 의존성 방향 통제. 순환 의존 방지 중요 |
| 3 | **Interface Abstraction** | 구현체 숨김 |
| 4 | **State Management** | 상태 저장 위치와 범위 정의 |
| 5 | **Communication Model** | sync / async / event-driven 구조 결정 |
| 6 | **Concurrency Model** | 동시 실행 방식 정의 |
| 7 | **Failure Isolation** | 장애 격리 경계 정의 |
| 8 | **Scalability Structure** | 수평 확장 가능 구조 정의 |

### 핵심 흐름

```
Requirements
→ architecture design
→ module structure
→ runtime interaction
→ resource usage pattern
→ operational behavior
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

아키텍처는 코드 자체보다 **런타임 동작 패턴**으로 관측됩니다.

### Process 구조

```bash
ps
pstree
systemctl
```

### Thread / CPU 구조

```bash
top
htop
perf
```

### Memory 구조

```bash
pmap
vmstat
smem
```

### Network topology

```bash
ss -tulpn
netstat
tcpdump
```

### Kubernetes 구조

```bash
kubectl get pods
kubectl describe svc
kubectl get ingress
```

### Distributed tracing

request flow, latency propagation, retry chain 관측 가능.

### Storage behavior

```bash
iostat
iotop
blktrace
```

### Runtime dependency behavior

connection pool, queue depth, GC pause, scheduler delay 등에서 드러납니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*