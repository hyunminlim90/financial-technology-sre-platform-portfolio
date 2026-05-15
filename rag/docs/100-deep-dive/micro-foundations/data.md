# 데이터 (Data)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**데이터(Data)** 는:

> 시스템이 저장·전송·연산하는 **의미 있는 디지털 정보**

컴퓨터 내부에서는 결국 **0과 1(bit)의 조합**이다.

### 데이터의 본질

데이터 자체는 숫자, 문자열, 패킷, 파일, 객체, 로그 등으로 보이지만, 하드웨어 레벨에서는 모두:

- `bit` / `byte`
- memory state
- electrical signal

### 대표 예시

| 영역 | 데이터 예시 |
|------|-----------|
| 결제 | 카드번호, 금액 |
| 네트워크 | TCP Packet |
| DB | Row / Column |
| JVM | Object |
| Kafka | Record |
| 로그 | Event Log |
| AI/FDS | Feature Vector |

---

## 2. 시스템 어디에서 등장하는가

데이터는 **시스템 전체를 관통**한다.

### Hardware Layer
- Register
- Cache
- RAM
- SSD
- NIC Buffer

### OS Kernel
- Page Cache
- `sk_buff`
- inode
- file descriptor

### Runtime
- JVM Heap
- Stack
- Direct Memory
- ByteBuf

### Application
- DTO
- JSON
- Entity
- Transaction Object

### Distributed System
- Replication Log
- Event Stream
- Message Queue

### Storage
- WAL
- SSTable
- Filesystem Block

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

데이터는 **Memory + Disk + Network** 영향이 가장 크다.

> 데이터는 저장되고 / 이동하고 / 복사되고 / 캐싱되고 / 직렬화되기 때문.

### 자원별 영향

| 자원 | 영향 |
|------|------|
| Memory | Object Allocation |
| Disk | Persistence I/O |
| Network | Packet Transfer |
| CPU | Parsing/Serialization |

### 데이터 크기 증가 시 함께 증가하는 것

- GC Pressure
- Network Latency
- Cache Miss
- Disk I/O
- Serialization Cost

---

## 4. 왜 중요한가

핀테크에서 데이터는 **실제 돈과 상태(State)를 의미**한다.

### 데이터 오류 발생 시 가능한 문제

> ⚠️ 중복 결제 / 잘못된 정산 / Ledger Corruption / Fraud Detection 실패

### 데이터 핵심 속성

| 속성 | 의미 |
|------|------|
| Integrity | 변조되지 않음 |
| Consistency | 상태 일관성 |
| Availability | 즉시 접근 가능 |
| Durability | 장애 후에도 유지 |

---

## 5. 실제 장애와 어떤 관련이 있는가

### Data Corruption
데이터 손상 시 잘못된 결제 상태 / Recovery 실패 발생 가능

### Serialization Explosion
대형 JSON/Object 유입 시 CPU Spike → Heap Pressure 발생 가능

### GC Pressure
객체 데이터 과다 생성 시 Young GC 증가 → STW 증가 발생

### Cache Miss
데이터 locality 부족 시 Memory Access Latency 증가 발생

### Network Saturation
대량 데이터 이동 시 Packet Drop → Retry 증가 발생 가능

### Disk Bottleneck
대규모 WAL/Log 기록 시 `fsync` latency 증가 → Queue Saturation 발생 가능

### Replication Lag
분산 데이터 복제 지연 시 Consistency Delay → Read Stale Data 발생 가능

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

### Bit / Byte
모든 데이터 기본 단위

### Serialization
Object → Binary/JSON 변환

### Deserialization
Binary → Object 복원

### Copy
데이터 이동 시 Kernel Copy / User Space Copy 비용 발생

### Cache Locality
CPU 가까운 데이터 접근 시 성능 향상

### Data in Motion
네트워크 이동 중 데이터

### Data in Use
현재 CPU/RAM에서 사용 중 데이터

### Data at Rest
디스크 저장 데이터

### Integrity Validation
대표: `SHA-256`, Checksum, Signature

### Buffering
입력 데이터 임시 저장. 대표: Ring Buffer, Queue, Page Cache

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Memory Usage
```bash
free -h
vmstat 1
```

### Disk I/O
```bash
iostat -x 1
```

### Network Traffic
```bash
sar -n DEV 1
```

### Socket Buffer
```bash
ss -m
```

### JVM Heap
```bash
jstat -gc
jmap -histo
```

### Kafka Data Flow
대표 지표: `consumer lag`, `throughput`, `queue depth`

### Kubernetes
```bash
kubectl top pod
```

### eBPF
```bash
# bcc-tools 대표 도구
biolatency
tcplife
offcputime
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*