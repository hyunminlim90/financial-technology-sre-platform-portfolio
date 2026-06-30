# Software Development Tech Stack (소프트웨어 개발 기술 스택)

> 정독: 1회

## 1. 무엇인가

소프트웨어 개발 기술 스택(Software Development Tech Stack)은 하나의 소프트웨어 시스템을 구축, 배포, 운영하기 위해 사용하는 기술들의 집합이다.

기술 스택은 특정 제품이나 프레임워크를 의미하는 것이 아니라 다음과 같은 영역의 기술 조합을 의미한다.

- 사용자 인터페이스
- 애플리케이션 로직
- 데이터 저장소
- 네트워크 통신
- 런타임 환경
- 배포 플랫폼
- 운영 도구
- 모니터링 도구

> 기술 스택은 소프트웨어 시스템의 구현 및 운영 기반을 구성한다.

---

## 2. 시스템 어디에서 등장하는가

기술 스택은 소프트웨어 시스템 전체에 존재한다.

일반적으로 다음 계층으로 구성된다.

```
Client Layer
        │
        ▼
Application Layer
        │
        ▼
Service Layer
        │
        ▼
Data Layer
        │
        ▼
Infrastructure Layer
```

> 기술 스택은 특정 계층만 담당하는 것이 아니라 전체 시스템 구현에 필요한 기술 선택의 집합이다.

---

## 3. 어떤 자원에 가장 영향이 큰가

기술 스택은 특정 자원 하나에만 영향을 주지 않는다.

선택된 기술에 따라 다음 자원의 사용 방식이 결정된다.

| 자원 | 영향 |
|------|------|
| CPU | 연산 방식, 동시성 모델 |
| Memory | 메모리 관리 방식 |
| Network | 통신 프로토콜, 연결 관리 |
| Disk | 저장 방식, 데이터 구조 |

**예시:**

| 처리 구조 | 결과 |
|-----------|------|
| 동기 처리 구조 | Thread 사용량 증가 |
| 비동기 처리 구조 | Event Loop 사용 증가 |
| 메모리 기반 저장소 | Memory 사용량 증가 |
| 디스크 기반 저장소 | Disk I/O 증가 |

---

## 4. 왜 중요한가

기술 스택은 시스템의 다음 특성에 직접적인 영향을 준다.

- 성능
- 확장성
- 유지보수성
- 운영 복잡도
- 장애 대응 난이도
- 개발 생산성

> 동일한 기능을 제공하더라도 기술 스택에 따라 운영 특성과 장애 양상이 달라질 수 있다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 장애는 기술 스택의 구조적 특성과 밀접한 관련이 있다.

**대표 사례:**

| 자원 | 장애 유형 |
|------|-----------|
| CPU | Thread Pool 고갈, Context Switching 증가, Lock Contention |
| Memory | Heap Leak, Direct Memory Exhaustion, Metaspace 증가 |
| Network | Connection Pool 고갈, Socket Timeout, Network Congestion |
| Storage | Disk Saturation, Transaction Delay, Storage Latency 증가 |

> 기술 스택은 장애의 직접 원인이 아니라 장애가 발생하는 **구조와 특성**을 결정한다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

E2E SRE 문서를 읽는 관점에서는 기술 스택을 다음과 같이 이해하면 충분하다.

**① 기술 스택은 계층 구조를 가진다**

```
Application
    ↓
Runtime
    ↓
Operating System
    ↓
Infrastructure
```

**② 장애는 계층을 따라 전파된다**

```
Application Issue
        ↓
Runtime 영향
        ↓
OS 영향
        ↓
Infrastructure 영향
```

**③ 각 기술은 특정 자원을 사용한다**
- CPU
- Memory
- Network
- Disk

**④ 운영 시에는 기술 자체보다 자원 사용 특성이 중요하다**

> "어떤 기술인가" 보다 "**어떤 자원을 사용하는가**"를 이해하는 것이 중요하다.

---

## 7. Linux / Runtime / Kubernetes 에서 어디서 관측되는가

기술 스택 자체는 관측 대상이 아니다. 기술 스택이 사용하는 **자원과 동작 결과**가 관측된다.

### Linux

| 구분 | 내용 |
|------|------|
| 관측 항목 | Process, Thread, CPU, Memory, Network, Disk |
| 대표 도구 | `top`, `htop`, `vmstat`, `iostat`, `ss`, `netstat` |

### Runtime

| 구분 | 내용 |
|------|------|
| 관측 항목 | GC, Heap, Thread, Lock, Class Loading, Connection Pool |
| 대표 도구 | JFR, Thread Dump, Heap Dump, Metrics |

### Kubernetes

| 구분 | 내용 |
|------|------|
| 관측 항목 | Pod, Container, CPU Usage, Memory Usage, Network Traffic, Restart Count |
| 대표 명령 | `kubectl top`, `kubectl describe`, `kubectl logs` |

---

## E2E SRE 문서 관점 핵심 정리

> 기술 스택은 소프트웨어 시스템을 구축하기 위해 선택한 기술들의 집합이다.

중요한 것은 특정 제품이나 프레임워크 이름이 아니라 다음을 이해하는 것이다.

- 어떤 계층에 존재하는가
- 어떤 자원을 사용하는가
- 어떤 장애 특성을 가지는가
- 어떻게 관측되는가

**E2E SRE 관점에서는** 기술 자체보다 **CPU · Memory · Network · Disk** 사용 특성과 장애 전파 경로를 이해하는 것이 핵심이다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*