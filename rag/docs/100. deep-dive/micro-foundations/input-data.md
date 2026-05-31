# 입력 데이터 (Input Data)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**입력 데이터(Input Data)** 는:

> 시스템이 연산을 시작하기 위해 외부 또는 내부로부터 **받아들이는 원본 정보**

CPU/GPU/Runtime 입장에서는 계산 대상, 판단 대상, 처리 대상이 되는 모든 데이터가 입력 데이터다.

### 대표적인 입력 데이터 예시

| 영역 | 입력 데이터 |
|------|------------|
| 결제 | 카드번호, 금액, 승인 요청 |
| 네트워크 | TCP Packet |
| JVM | HTTP Request Body |
| DB | Query Parameter |
| AI/FDS | 사용자 행동 패턴 |
| OS | System Call Argument |

즉, **연산 이전 단계에 존재하는 모든 원재료**가 입력 데이터다.

<details>
<summary>Deep Dive</summary></br>

System(시스템) [[M]](../../100-deep-dive/micro-foundations/system.md)  
연산 (Operation / Computation) [[M]](../../100-deep-dive/micro-foundations/computation-operation.md)

</details></br>

## 2. 시스템 어디에서 등장하는가

입력 데이터는 **시스템 전체**에서 등장한다.

### Network Layer
- NIC Packet 수신
- TCP Stream
- TLS Payload

### Kernel Layer
- Socket Buffer (`sk_buff`)
- Page Cache
- Syscall Buffer

### Runtime Layer
- JVM Heap Object
- Netty ByteBuf
- Kafka Record

### Application Layer
- JSON
- Protobuf
- Payment Request DTO

### Storage Layer
- DB Row
- WAL Log
- Filesystem Block

### AI/FDS Layer
- Feature Vector
- Behavioral Data

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

입력 데이터는 **Memory + Network** 영향이 가장 크다.

> 입력 데이터는 Network 통해 들어오고 → Memory에 적재되고 → CPU가 처리되기 때문.

### 주요 자원 영향

| 자원 | 영향 |
|------|------|
| Network | 패킷 수신량 증가 |
| Memory | Buffer/Object 생성 |
| CPU | Parsing/Validation |
| Disk | Logging/Persistence |

### 데이터 크기 증가 시 증가하는 것

- Copy Cost
- Serialization Cost
- GC Pressure
- Cache Miss
- Network Latency

---

## 4. 왜 중요한가

입력 데이터 품질과 구조가 **전체 시스템 안정성과 성능을 결정**한다.

### FinTech에서 특히 중요한 이유

결제 시스템은 무결성, 순서, 정확성이 매우 중요하다.

잘못된 입력 데이터는 아래로 연결될 수 있다:

> ⚠️ 잘못된 승인 / 중복 결제 / Fraud Detection 실패 / Ledger Corruption

---

## 5. 실제 장애와 어떤 관련이 있는가

### Input Burst
갑작스러운 입력 증가(Micro-burst, Traffic Spike) 발생 시:
- NIC Queue Saturation
- CPU Saturation
- JVM GC 증가

### Oversized Payload
과도하게 큰 JSON 유입 시 Heap Pressure → GC 증가 → OOM 유발 가능

### Malformed Data
비정상 입력 발생 시:
- Parser Exception
- Serialization Failure
- Deserialization CPU Spike

### Input Queue Backlog
Kafka/Ingress Queue 적체 시 Consumer Lag → Tail Latency 증가 발생

### Packet Loss
입력 패킷 손실 시 Retry Storm / Duplicate Request 발생 가능

### Hash Validation Failure
입력 데이터 무결성 실패 시 Request Reject / Security Incident 가능

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

### NIC Receive Queue
입력 패킷 최초 수신 지점

### DMA
NIC → Memory 직접 복사. CPU 개입 최소화.

### Kernel Socket Buffer
입력 데이터 임시 저장. 대표: `sk_buff`

### Serialization / Deserialization
JSON ↔ Object 변환 과정. CPU 사용량이 큰 구간.

### Copy Overhead
입력 데이터 이동 시 복사 비용 발생:
- NIC → Kernel
- Kernel → User Space
- User Space → JVM Object

### Validation
입력 데이터의 Schema, Signature, Hash, Authorization 검증 필요

### Backpressure
입력 속도가 처리 속도 초과 시 Queue 증가 → Latency 증가 발생

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Network Input
```bash
sar -n DEV 1
ethtool -S eth0
```

### Socket Queue
```bash
ss -tuln
ss -m
```

### Packet Drop
```bash
netstat -s
```

### NIC Ring Buffer
```bash
ethtool -g eth0
```

### JVM Heap / Object 생성
```bash
jstat -gc
jfr
```

### Kafka Consumer Lag
대표 지표: `records-lag`, `consumer-delay`

### Kubernetes
```bash
kubectl top pod
kubectl describe pod
```

### eBPF
```bash
# bcc-tools 대표 도구
tcplife
tcpdrop
biolatency
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*