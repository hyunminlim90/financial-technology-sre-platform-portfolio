# Communication Mechanism (통신 메커니즘)
## **Micro Foundations — 범용 시스템 관점**

> 정독: 0회

## 1. 이 기술이 무엇인가

**Communication Mechanism(통신 메커니즘)**은:

> 시스템과 시스템, 프로세스와 프로세스, 컴포넌트와 컴포넌트 사이에서 데이터와 상태(State)를 **어떤 방식과 규칙으로 교환할 것인가**를 정의하는 실행 구조와 기술적 수단

핵심은 단순 통신이 아니다:

> **"통신을 어떤 규칙으로 안전하게 수행할 것인가"**

통신 메커니즘은 다음을 모두 포함한다:

| 영역 | 예시 |
|------|------|
| 요청/응답 | request/response, acknowledgment |
| 전파 | event propagation, stream transmission |
| 전달 | message delivery, ordering |
| 제어 | retry, timeout, flow control |
| 동기화 | synchronization |

> **핵심:** Communication은 "행위", Communication Mechanism은 **"행위를 가능하게 하는 구조와 규칙"**이다.

---

## 2. 시스템 어디에서 등장하는가

통신 메커니즘은 시스템 **전체**에 존재한다.

### 프로세스 내부
- thread signaling
- shared memory coordination
- lock/wakeup

### 운영체제 레벨
- pipe
- socket
- IPC
- signal

### 서버 내부
- process-to-process communication
- local RPC
- UNIX domain socket

### 네트워크 레벨
- TCP
- UDP
- QUIC

### 서비스 간 통신
- REST
- RPC
- message queue
- event bus

### 분산 시스템 레벨
- replication
- consensus
- leader election
- distributed coordination

> **결론:** 통신 메커니즘은 **분산 시스템의 신경계**다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Network + CPU**이지만, 실제로는 전 자원에 영향을 준다.

### CPU 영향
- **계산 항목:** serialization, deserialization, encryption, checksum, compression, protocol parsing
- **특히:** TLS, protobuf, JSON parsing 등은 CPU 사용량에 직접 영향

### Memory 영향
- **버퍼링 형태:** socket buffer, queue, send buffer, receive buffer
- **과부하 시:** memory accumulation, buffer explosion, queue buildup

### Network 영향
- **핵심 요소:** bandwidth, RTT, congestion, retransmission, packet loss

### Disk 영향
- **비동기 메시징 시스템에서:** WAL logging, durable queue, replication log
- **예:** Kafka는 "네트워크 시스템"이면서 동시에 "디스크 기반 시스템"

---

## 4. 왜 중요한가

현대 시스템 대부분은 **Single Machine → Distributed Computing**으로 진화했다.

따라서 시스템의 대부분 문제는 "계산"보다 **"통신"**에서 발생한다.

특히 분산 시스템에서의 핵심 문제:

- latency
- ordering
- retry
- partial failure
- timeout
- congestion

좋은 통신 메커니즘은:

- 결합도를 낮추고
- 안정성을 높이며
- 장애 전파를 차단하고
- 흐름을 통제한다

> **결론:** Communication Mechanism은 **분산 시스템 안정성의 핵심 제어 장치**다.

---

## 5. 실제 장애와 어떤 관련이 있는가

대부분의 대규모 장애는 통신 메커니즘 문제와 연결된다.

### 1) Cascading Failure
```
A → B → C → D  (동기식 호출 연쇄)
  ↓ D가 느려짐
C thread exhaustion → B timeout → A saturation
```

### 2) Retry Storm
```
timeout 발생 후 무제한 재시도
  ↓
원래 장애보다 retry traffic이 더 커짐 → network collapse
```

### 3) Queue Saturation
```
consumer 속도 < producer 속도
  ↓
backlog accumulation → memory pressure → disk overflow
```

### 4) Connection Explosion
```
대규모 connection 생성
  ↓
FD exhaustion → ephemeral port exhaustion → scheduler overload
```

### 5) Head-of-Line Blocking
```
하나의 느린 흐름이 전체 흐름을 차단
  ↓
HTTP/1.1, shared queue, serialized processing 등에서 발생
```

> **핵심:** 통신 장애는 데이터 손상보다 **흐름 정체와 전염**으로 더 위험하다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

Communication Mechanism의 핵심은:

> **"상대방이 언제든 느려지거나 죽을 수 있다"를 전제로 설계하는 것**

따라서 현대 시스템은 다음을 핵심 원칙으로 사용한다:

- blocking 최소화
- timeout 강제
- retry 제한
- queue buffering
- load shedding
- backpressure
- fault isolation

### 핵심 메커니즘

| 메커니즘 | 의미 |
|----------|------|
| **Request/Response** | 가장 기본적인 동기 메커니즘 |
| **Message Queue** | 속도 차이 완충 |
| **Event-Driven Flow** | 비동기 전파 기반 구조 |
| **Streaming** | 지속 연결 기반 흐름 |
| **Backpressure** | 과부하 시 upstream 감속 |
| **Timeout** | 무한 대기 차단 |
| **Circuit Breaker** | 장애 전파 차단 |
| **Retry with Limit** | 일시 장애 복구 |
| **Idempotency** | 중복 요청 안전성 확보 |

> **핵심 개념:** 좋은 통신 메커니즘은 빠른 메커니즘이 아니라, **장애가 전염되지 않는 메커니즘**이다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

통신 메커니즘 문제는 시스템 전반에 **흔적을 남긴다.**

### Linux

**Socket 상태**
```bash
ss -ant
netstat -s
```
관찰: connection growth, retransmission, TIME_WAIT explosion

**Network 상태**
```bash
sar -n DEV
iftop
tcpdump
```
관찰: throughput, packet drop, RTT, congestion

**Queue 상태**
```bash
ss -lnt
```
관찰: backlog overflow, accept queue saturation

---

### Runtime

관찰 포인트:
- timeout rate
- retry count
- queue depth
- connection pool exhaustion
- response latency

---

### Kubernetes

| 관찰 영역 | 명령어 | 관찰 |
|-----------|--------|------|
| **Service Communication** | `kubectl logs` / `kubectl describe svc` | timeout propagation, DNS failure, connection reset |
| **Pod Networking** | `kubectl top pod` | network saturation, packet retransmission |
| **Mesh / Gateway** | — | circuit breaker activation, retry amplification, upstream overflow |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*