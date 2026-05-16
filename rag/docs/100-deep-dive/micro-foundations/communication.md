# Communication (통신)
## Micro Foundations — 범용 시스템 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

Communication(통신)은:

> 서로 다른 시스템이나 구성 요소가 데이터를 주고받으며 상태를 전달하고 상호작용하는 과정

핵심은 단순한 데이터 이동이 아니다.  
통신은:

- 요청 (Request)
- 응답 (Response)
- 동기화 (Synchronization)
- 상태 전달 (State Sharing)
- 협업 (Coordination)

을 가능하게 만든다.

즉:

> **Communication은 "시스템 간 의미 전달 행위"이다.**

---

## 2. 시스템 어디에서 등장하는가

통신은 거의 모든 컴퓨팅 시스템에 존재한다.

**컴퓨터 내부**
- CPU ↔ Memory
- Process ↔ Process
- Thread ↔ Thread

**운영체제**
- IPC
- Signal
- Shared Memory
- Pipe

**네트워크 시스템**
- Client ↔ Server
- Service ↔ Service
- Region ↔ Region

**분산 시스템**
- Consensus
- Replication
- Event Streaming
- Cluster Coordination

즉 현대 시스템은:

> **통신 없이 존재할 수 없다.**

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적 영향:

> **Network + Memory**

### Network

통신의 핵심 자원. 영향 요소:

- latency
- bandwidth
- packet loss
- congestion

### Memory

통신 데이터는 메모리 버퍼를 거친다. 예:

- socket buffer
- queue
- shared memory
- message buffer

### CPU

통신 자체도 연산이다. 예:

- serialization
- encryption
- checksum
- protocol parsing
- compression

### Disk

간접적으로 영향. 예:

- persistent queue
- WAL
- message durability
- retry log

---

## 4. 왜 중요한가

현대 시스템은:

> **독립 실행보다 협업 실행이 기본**

이다. 즉:

- 웹 서비스
- 클라우드
- Kubernetes
- 금융 시스템
- 분산 DB

모두 통신 위에서 작동한다.

통신이 멈추면:

- 데이터 동기화 실패
- 요청 처리 실패
- 상태 불일치
- 장애 전파

가 발생한다. 즉:

> **통신은 시스템의 "신경망"이다.**

---

## 5. 실제 장애와 어떤 관련이 있는가

통신 장애는 매우 흔하고 위험하다.

### 1) Timeout

응답 지연. 결과:

- retry 폭증
- thread/blocking 증가
- cascading failure

### 2) Packet Loss

데이터 일부 유실. 결과:

- retransmission
- throughput 저하
- latency 증가

### 3) State Desynchronization

통신 실패로 상태 불일치 발생. 예:

- replica lag
- cache inconsistency
- split brain

### 4) Congestion

통신량 과다. 결과:

- queue 폭증
- connection saturation
- 서비스 전체 지연

### 5) Protocol Mismatch

서로 다른 규칙 사용. 결과:

- parsing failure
- incompatible message
- handshake failure

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심은:

> **통신은 "약속(protocol)" 위에서만 성립한다.**

즉 시스템끼리는:

- 데이터 형식
- 순서
- 오류 처리
- 응답 방식

을 미리 합의해야 한다.

또 매우 중요한 핵심:

> **통신은 항상 실패 가능성을 가진다.**

즉:

- 유실 가능
- 지연 가능
- 중복 가능
- 순서 변경 가능

그래서 현대 시스템은:

- retry
- timeout
- ack
- checksum
- idempotency

같은 보호 메커니즘을 사용한다.

또 하나 중요:

> **통신 비용은 연산 비용보다 비싼 경우가 많다.**

분산 시스템에서는:

- CPU 연산보다
- 네트워크 RTT

가 더 큰 병목이 되는 경우가 많다. 그래서:

- batching
- async communication
- local caching

같은 최적화가 등장한다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**연결 상태**

```bash
ss
netstat
lsof -i
```

**네트워크 상태**

```bash
ip
ethtool
sar -n
iftop
```

**패킷 관측**

```bash
tcpdump
wireshark
```

**IPC 관측**

```bash
ipcs
strace
```

### Runtime

관찰 포인트:

- request latency
- timeout rate
- retry count
- queue depth
- connection pool saturation

### Kubernetes

**Pod 통신**

- pod-to-pod RTT
- DNS lookup latency
- service discovery

**Cluster 통신**

- API server latency
- node communication
- overlay network 상태

**Service Mesh**

관찰 포인트:

- mTLS handshake
- sidecar latency
- traffic retry

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*