# Connector (커넥터)
## **Micro Foundations — 범용 시스템/인프라 관점**

> 정독: 0회

## 1. 이 기술이 무엇인가

**Connector(커넥터)**는:

> 독립된 컴포넌트 사이를 연결하여 데이터, 신호, 전력, 제어 흐름이 **정해진 규격에 따라 이동할 수 있게 하는 매개체**

핵심은:

> **"분리된 것을 결합하되, 완전히 붙여버리지는 않는 것"**

즉 커넥터는:

- 연결한다
- 규격을 강제한다
- 교체 가능성을 만든다
- 장애 전파를 제어한다
- 컴포넌트 간 결합도를 조절한다

---

## 2. 시스템 어디에서 등장하는가

커넥터는 **모든 계층**에서 등장한다.

### 하드웨어 계층
- CPU socket, RAM slot, PCIe slot
- USB-C, RJ-45, power connector

### 네트워크 계층
- NIC port, switch port
- optical transceiver, cable connector

### 운영체제 계층
- socket, pipe, file descriptor
- system call interface

### 소프트웨어 계층
- API, driver, adapter
- client library, protocol interface

### 분산 시스템 계층
- RPC connector
- message broker connector
- database connector
- event connector

> **결론:** Connector는 **컴포넌트 경계**에서 등장한다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

커넥터는 주로 **Network + CPU + Memory**에 영향을 준다.

### Network 영향
- **소프트웨어 커넥터는 대부분 통신 경로를 만든다**
- 영향: latency, throughput, retransmission, connection count, timeout

### CPU 영향
- **커넥터는 데이터를 변환하거나 해석한다**
- 예: protocol parsing, serialization, encryption, checksum, compression

### Memory 영향
- **커넥터는 버퍼를 사용한다**
- 예: socket buffer, receive/send buffer, connection pool, message buffer

### Disk 영향
- **영속 커넥터에서는 디스크도 중요하다**
- 예: durable queue, WAL, commit log, replication log

> **핵심:** 커넥터는 단순 연결선이 아니라 **자원을 소비하는 실행 경계**다.

---

## 4. 왜 중요한가

현대 시스템의 품질은 다음으로 결정된다:

> **컴포넌트 자체의 품질 + 컴포넌트 연결 방식의 품질**

**커넥터가 잘 설계되면:**
- 컴포넌트 교체가 쉬워진다
- 장애 격리가 쉬워진다
- 확장이 쉬워진다
- 관측 가능성이 좋아진다
- 결합도가 낮아진다

**커넥터가 잘못 설계되면:**
- tight coupling
- timeout cascade
- retry storm
- connection exhaustion
- cascading failure

> **결론:** Connector는 **시스템의 확장성과 안정성을 결정하는 연결 경계**다.

---

## 5. 실제 장애와 어떤 관련이 있는가

커넥터 장애는 보통 **컴포넌트 간 장애 전파**로 나타난다.

### 1) Connection Exhaustion
```
커넥터가 connection pool을 관리하지 못함
  ↓
신규 요청 대기 → timeout 증가 → 전체 latency 증가
```

### 2) Timeout 누락
```
외부 컴포넌트 응답을 무한 대기
  ↓
thread/blocking resource 고갈 → cascading failure
```

### 3) Retry Storm
```
커넥터 레벨에서 재시도 정책이 과도함
  ↓
원래 트래픽보다 retry traffic이 커짐 → 하위 시스템 붕괴
```

### 4) Protocol Mismatch
```
양쪽 컴포넌트의 규격 불일치
  ↓
parsing error → data corruption → request failure
```

### 5) Physical Connector Fault
```
케이블, 포트, 슬롯, 전원 커넥터 이상
  ↓
packet loss → device disconnect → hardware unavailable
```

> **핵심:** 커넥터 문제는 "연결 실패"뿐 아니라 **"장애 전염"**으로 이어진다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

커넥터의 핵심은:

> **Interface Contract**

커넥터는 반드시 다음을 정의한다:

- 어떤 형식으로 보낼 것인가
- 언제까지 기다릴 것인가
- 실패하면 어떻게 할 것인가
- 중복 요청을 어떻게 처리할 것인가
- 연결을 몇 개까지 허용할 것인가
- 응답 순서를 보장할 것인가

### 핵심 메커니즘

| 메커니즘 | 의미 |
|----------|------|
| **Interface** | 입출력 규격 |
| **Protocol** | 통신 규칙 |
| **Adapter** | 서로 다른 규격 변환 |
| **Driver** | 하드웨어/소프트웨어 사이의 제어 계층 |
| **Timeout** | 무한 대기 차단 |
| **Retry Policy** | 일시 실패 복구 |
| **Circuit Breaker** | 장애 전파 차단 |
| **Pooling** | 연결 자원 재사용 |
| **Backpressure** | 과부하 시 흐름 제어 |

> **핵심 개념:** 좋은 커넥터는 연결을 만들 뿐 아니라 **연결 실패도 통제**한다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**네트워크 커넥터 상태**
```bash
ip link
ethtool <interface>
```
관찰: link up/down, speed, duplex, driver, CRC error, dropped packet

**소켓 연결 상태**
```bash
ss -ant
ss -s
```
관찰: ESTABLISHED, TIME_WAIT, SYN_RECV, connection count

**파일 디스크립터**
```bash
lsof
ulimit -n
```
관찰: FD exhaustion, socket leak

**장치 연결**
```bash
lspci
lsusb
dmesg
```
관찰: PCIe device, USB device, driver attach/detach, hardware error

---

### Runtime

관찰 포인트:
- connection pool usage
- timeout count
- retry count
- protocol error
- serialization error
- circuit breaker state

---

### Kubernetes

| 관찰 영역 | 명령어 | 관찰 |
|-----------|--------|------|
| **Service 연결** | `kubectl get svc` / `kubectl describe svc` | 서비스 연결 상태 |
| **Endpoint 연결** | `kubectl get endpoints` | endpoint 등록 여부 |
| **Pod 로그** | `kubectl logs <pod>` | connection refused, timeout, DNS failure, upstream error |
| **네트워크 정책** | `kubectl get networkpolicy` | 통신 허용/차단 경계 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*