# Data Flow (데이터 흐름)
## **Micro Foundations — 범용 시스템 관점**

> 정독: 0회

## 1. 이 기술이 무엇인가

**Data Flow(데이터 흐름)**는:

> 데이터가 생성된 순간부터 이동·변환·가공·저장·폐기되기까지 시스템 내부를 따라 흐르는 **전체 경로와 과정**

핵심은:

- "데이터가 어디에 존재하는가"보다,
- **"데이터가 어떻게 움직이는가"**

이다.

데이터 흐름에는 다음이 모두 포함된다:

| 영역 | 예시 |
|------|------|
| 요청/응답 | request flow, acknowledgment |
| 전파 | event propagation, replication |
| 완충 | queue buffering, caching |
| 보존 | storage persistence |
| 복구 | retry |
| 동기화 | synchronization |

> **핵심:** Data Flow는 **시스템의 동적인 생명 활동**이다.

---

## 2. 시스템 어디에서 등장하는가

데이터 흐름은 시스템 **전체 계층**에서 등장한다.

### 사용자 입력 계층
- API request
- mobile app event
- sensor signal

### 네트워크 계층
- packet routing
- stream transmission
- RPC communication

### 애플리케이션 계층
- business processing
- validation
- transformation

### 메시징 계층
- queue
- broker
- event stream

### 데이터 저장 계층
- database write
- cache update
- replication

### 분산 시스템 계층
- service-to-service propagation
- distributed transaction
- event synchronization

> **결론:** 시스템은 결국 **데이터 흐름을 처리하는 거대한 이동 기계**다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

Data Flow는 사실상 **모든 자원을 동시에** 사용한다.

### CPU 영향
- **중간 처리:** parsing, serialization, compression, encryption, validation, transformation
- **흐름 증가 시:** CPU saturation, context switching increase, scheduling delay

### Memory 영향
- **경유 형태:** buffer, queue, cache, page cache
- **흐름 폭증 시:** queue accumulation, memory pressure, OOM

### Network 영향
- **영향 요소:** bandwidth, RTT, retransmission, packet loss
- **분산 시스템에서:** 네트워크 자체가 시스템의 일부가 된다

### Disk 영향
- **최종 persistence 단계:** WAL write, fsync, replication log, checkpoint
- **흐름 증가 시:** disk queue 증가, I/O bottleneck, write latency spike

> **핵심:** Data Flow는 시스템 자원을 "소비"하는 것이 아니라, **시스템 전체를 "관통"**한다.

---

## 4. 왜 중요한가

좋은 시스템은 좋은 코드보다 **좋은 데이터 흐름 구조**를 가진다.

대부분의 대규모 장애는 다음에서 시작되기 때문이다:

- 데이터 흐름 병목
- 흐름 증폭
- 흐름 정체
- 흐름 꼬임

**대표 예시:** retry storm, queue explosion, DB hotspot, event duplication, deadlock, cascading timeout

> **결론:** Data Flow는 **시스템 성능·안정성·확장성의 실제 본체**다.

---

## 5. 실제 장애와 어떤 관련이 있는가

Data Flow 문제는 매우 자주 장애로 이어진다.

### 1) Bottleneck
```
100대의 API 서버 → 단일 DB
  ↓
queue growth → latency spike → timeout cascade
```

### 2) Retry Amplification
```
실패한 요청을 무제한 재시도
  ↓
original load보다 retry load가 더 커짐 → network storm → CPU overload
```

### 3) Queue Explosion
```
consumer 처리 속도 < producer 유입 속도
  ↓
memory accumulation → disk spill → broker instability
```

### 4) Circular Flow
```
A → B → C → A (서비스들이 서로를 호출)
  ↓
deadlock → cascading timeout → distributed stall
```

### 5) Data Race
```
동일 데이터 흐름이 동시 수정
  ↓
state corruption → integrity violation
```

> **결론:** Data Flow 장애는 데이터 자체보다 **흐름 제어 실패**에서 발생한다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

Data Flow의 핵심은:

> **"속도 차이를 어떻게 흡수할 것인가"**

현실 시스템에서는 producer · consumer · network · storage 속도가 모두 다르다.

따라서 시스템은 buffering · queueing · batching · throttling · backpressure 같은 메커니즘으로 흐름을 제어한다.

### 핵심 메커니즘

| 메커니즘 | 의미 |
|----------|------|
| **Buffer** | 속도 차이 흡수 공간 |
| **Queue** | 순서 보장 및 흐름 평준화 |
| **Backpressure** | 과부하 시 upstream 감속 |
| **Retry** | 일시 오류 복구 |
| **Acknowledgment** | 전송 성공 확인 |
| **Persistence** | 흐름 상태 보존 |
| **Replication** | 데이터 유실 방지 |
| **Partitioning** | 병렬 흐름 분산 |

> **핵심 개념:** 좋은 데이터 흐름은 빠른 흐름이 아니라, **통제 가능한 흐름**이다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

Data Flow는 시스템 observability 전체에 **흔적을 남긴다.**

### Linux

**Network Flow**
```bash
iftop
nload
ss -s
sar -n DEV
```
관찰: throughput, retransmission, connection growth

**Queue 상태**
```bash
vmstat
netstat
```
관찰: backlog, socket queue, wait accumulation

**Disk Flow**
```bash
iostat
iotop
```
관찰: write queue, flush delay, storage bottleneck

---

### Runtime

관찰 포인트:
- request latency
- queue depth
- retry count
- timeout rate
- event lag

---

### Kubernetes

| 관찰 영역 | 명령어 | 관찰 |
|-----------|--------|------|
| **Pod Traffic** | `kubectl top pod` | traffic imbalance, flow hotspot |
| **Event / Queue Delay** | 메시징 시스템 | consumer lag, partition skew, backlog growth |
| **Service Communication** | `kubectl logs` / `kubectl describe svc` | retry storm, timeout propagation, service dependency delay |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*