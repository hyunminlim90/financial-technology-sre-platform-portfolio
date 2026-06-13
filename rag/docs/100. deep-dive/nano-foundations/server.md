# 서버 (Server)
## FinTech/SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**서버(Server)** 는:

> 네트워크를 통해 다른 시스템의 요청(Request)을 받아 **데이터·기능·서비스를 제공(Serve)하는 시스템**

핵심은:

> "무언가를 기다리고 있다가, 요청이 오면 처리해서 응답하는 역할"

즉 서버는 단순 컴퓨터가 아니라 **요청을 수신하고 / 처리하고 / 응답하는** 서비스 제공자(Service Provider)이다.

---

## 2. 시스템 어디에서 등장하는가

서버는 **거의 모든 IT 시스템의 중심 구성 요소**다.

### 웹 시스템
- Web Server
- API Server
- Application Server

예: 로그인, 결제, 상품 조회

### 데이터 시스템
- Database Server
- Cache Server
- Search Server

### 인프라 시스템
- DNS Server
- Proxy Server
- Gateway Server
- Monitoring Server

### 클라우드 / 플랫폼
- Kubernetes Node
- VM Host
- Hypervisor Host

### 메시징 / 이벤트
- Kafka Broker
- MQ Server
- Streaming Server

### AI / 분석 시스템
- Inference Server
- Model Serving System

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

서버는 **모든 컴퓨팅 자원을 종합적으로 사용하는 시스템**이다.

| 자원 | 서버에서의 역할 |
|------|-------------|
| Network | 외부 요청 수신이 서버의 본질 — 가장 직접적인 영향 |
| CPU | 요청 처리 중 계산, 로직 실행, 암호화, 압축 등 수행 |
| Memory | 동시 요청 상태 유지 (Session, Cache, Heap, Connection State) |
| Disk | 로그 저장, DB 기록, 파일 저장, 이벤트 영속화 |

---

## 4. 왜 중요한가

현대 서비스는 **서버 없이는 서비스 자체가 존재 불가**한 수준이다.

### 서버의 핵심 역할

| 역할 | 의미 |
|------|------|
| Centralized Processing | 복잡한 연산을 중앙에서 수행 |
| Shared Service Provider | 수많은 사용자에게 동일 기능 제공 |
| State Management | 인증/거래/세션 상태 및 데이터 정합성 유지 |
| Reliability | 24시간 서비스 지속 제공 |
| Scalability | 트래픽 증가 시 Scale-Up / Scale-Out 가능해야 함 |

---

## 5. 실제 장애와 어떤 관련이 있는가

SRE 관점에서 서버는 **장애의 중심 관찰 대상**이다.

### Resource Exhaustion
자원 고갈: CPU Saturation, Memory Exhaustion, Disk Full, Connection Exhaustion

### Network Failure
Packet Loss, Timeout, SYN Flood, Port Exhaustion

### Process Failure
Crash, Deadlock, Memory Leak, OOM

### Dependency Failure
서버는 다른 시스템에 의존한다. 예: DB, Redis, MQ, External API

### Overload
트래픽 폭증 시 Queue 증가 → Latency 증가 → Retry Storm 발생 가능

### Partial Failure
분산 시스템에서는 **일부 서버만 실패하는 상황**이 매우 흔하다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

### Listen → Process → Respond
서버의 가장 본질적인 구조:

```
요청 대기
→ 요청 수신
→ 처리
→ 응답 반환
```

### Stateful vs Stateless

- **Stateful:** 서버 내부가 상태 기억. 예: 로그인 세션
- **Stateless:** 요청 간 상태 미보관. 예: REST API

### Concurrency
서버는 **동시에 매우 많은 요청을 처리**해야 한다. 방법: Multi-thread, Event Loop, Async I/O, Queue

### Resource Sharing
서버 자원은 여러 사용자 / 여러 프로세스 / 여러 서비스가 공유한다.

### Isolation
현대 서버는 VM, Container, Namespace 등으로 격리 운영한다.

### Availability
서버는 **"죽지 않는 것"** 이 매우 중요하다. 그래서 Health Check, Restart, Failover, Replication을 사용한다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### 프로세스
```bash
ps -ef
```

### 포트 대기 상태
```bash
ss -lntp
netstat -lntp
```

### 시스템 부하
```bash
top
htop
```

### 로그
```bash
journalctl
tail -f /var/log/*
```

### Kubernetes
```bash
kubectl get pods
kubectl get svc
kubectl get deploy
kubectl top pod
```

### Runtime 관측 포인트

| Runtime | 관측 대상 |
|---------|---------|
| JVM | Heap, GC, Thread, Event Loop |
| Node.js | Event Loop Delay |
| Go | Goroutine, Scheduler |

### Observability 대표 서버 지표
- `RPS` / `TPS`
- `Latency`
- `Error Rate`
- `Saturation`
- `Availability`

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*