# Data Transmission (데이터 전송)
## Micro Foundations — 범용 시스템 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

**Data Transmission** 은:

> 데이터를 한 위치에서 **다른 위치로 이동시키는 과정**

핵심은 단순 복사가 아니다. Transmission에는:

- 데이터 인코딩
- 이동
- 전달
- 수신
- 검증

과정 전체가 포함된다.

즉:

> **Transmission = "데이터 이동 자체"**

---

## 2. 시스템 어디에서 등장하는가

데이터 전송은 **시스템 전체**에서 발생한다.

### 하드웨어 내부
- `Register ↔ Cache`
- `Cache ↔ RAM`
- `RAM ↔ SSD`

### 시스템 내부
- `Process ↔ Process`
- Shared Memory
- IPC
- DMA

### 네트워크
- `Client ↔ Server`
- `Service ↔ Service`
- `Node ↔ Node`

### 분산 시스템
- Replication
- Event Streaming
- Consensus Communication

즉 현대 시스템은 **계속 데이터를 전송하면서 동작한다**.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접 영향받는 자원: **Network + Memory + Disk I/O**

| 자원 | Data Transmission과의 관계 |
|------|--------------------------|
| Network | 대표적 영향 영역. bandwidth / packet loss / RTT / congestion이 핵심 |
| Memory | Transmission 중 buffer allocation / copy operation / queue retention 발생 |
| Disk | 대용량 전송 시 disk read / disk write / sequential I/O 부하 증가 |
| CPU | packet processing / checksum / encryption / serialization 비용 처리 |

---

## 4. 왜 중요한가

현대 시스템은 대부분 **분산 시스템(distributed system)** 이다.

저장 위치, 연산 위치, 사용자 위치가 서로 다르다. 따라서:

> **전송이 멈추면 시스템 협업도 멈춘다.**

연산 자체보다 **데이터 이동 / 상태 전달 / 결과 전달** 이 더 중요한 경우도 많다.

예: API 응답, DB replication, storage synchronization, stream processing 모두 Transmission 기반.

---

## 5. 실제 장애와 어떤 관련이 있는가

Transmission 장애는 **시스템 전체 장애로 이어지기 쉽다**.

### 1) Packet Loss
전송 중 데이터 유실 시 retry 증가 → latency 증가 → timeout 발생

### 2) Network Congestion
대역폭 포화 시 throughput 감소 → queue 증가 → tail latency 폭증

### 3) Partial Transmission
일부만 전달 시 corrupted state → incomplete message → deserialization failure 가능

### 4) Replication Delay
분산 시스템 동기화 지연 시 stale state → eventual consistency 문제 → replica lag 발생

### 5) Serialization Overhead
전송 가능한 형태 변환 비용 증가 시 CPU 사용률 증가 → GC pressure → response delay 발생 가능

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심은 **"데이터는 그대로 이동하지 않는다"** 이다.

전송 전에 대부분 아래 과정을 거친다:
- `packetization`
- `framing`
- `encoding`
- `serialization`

즉:

> **메모리 내부 표현 ≠ 전송 표현**

### Transmission은 물리적 거리 영향을 받는다

| 위치 | 전송 비용 |
|------|---------|
| 같은 시스템 내부 | ns ~ μs |
| 원격 네트워크 | ms ~ seconds |

> **Transmission latency는 물리 법칙 영향을 받는다.**

### 전송은 Queue 기반으로 동작한다

전송 경로에는 항상 Queue가 존재한다:
- `NIC queue`
- `socket buffer`
- `broker queue`
- `kernel buffer`

그래서 전송 병목은 보통 **Queue saturation** 형태로 나타난다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### 네트워크 상태
```bash
ss -s
netstat -s
iftop
ip -s link
```

### 패킷 분석
```bash
tcpdump -i eth0
wireshark
```

### 대역폭 및 오류
```bash
sar -n DEV 1
ethtool -S eth0
```

### Runtime 관찰 포인트
- `transmission latency`
- `retry rate`
- `socket backlog`
- `serialization cost`
- `queue depth`

### Kubernetes Pod 간 전송
- `CNI traffic`
- `service mesh latency`
- `ingress/egress traffic`

### Kubernetes 분산 전송 상태
- `replication lag`
- `stream delay`
- `broker throughput`

### Observability 대표 지표
- `P95 / P99 latency`
- `packet retransmission`
- `timeout rate`
- `bandwidth usage`

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*