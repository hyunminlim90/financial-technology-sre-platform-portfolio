# 페이로드 (Payload)

> 정독: 0회

## 1. 이 기술이 무엇인가

페이로드(Payload)는:

> 통신 과정에서 필요한 프로토콜 제어 정보를 제외한, **실제로 전달하려는 순수 데이터 본문**

### 핵심 특징

페이로드는 다음을 의미합니다:

- 실제 사용자 데이터
- 애플리케이션 데이터
- 비즈니스 데이터
- 최종 목적 데이터

### 예시

| 프로토콜 | Payload |
|---|---|
| Ethernet | IP Packet |
| IP | TCP Segment |
| TCP | HTTP Data |
| HTTP | JSON / HTML / File |
| TLS | Encrypted Application Data |

### 핵심 개념

각 계층은 **상위 계층 전체를 자신의 Payload로 취급**합니다.

<details>
<summary>Deep Dive</summary></br>

Data Communication(데이터 통신) [[M]](../../100-deep-dive/micro-foundations/data-communication.md)  
Protocol Control Information(프로토콜 제어 정보) [[M]](../../100-deep-dive/micro-foundations/protocol-control-information.md)  
Service Data Unit(서비스 데이터 단위) [[M]](../../100-deep-dive/micro-foundations/service-data-unit.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

Payload는 네트워크와 시스템 전체에서 등장합니다.

### 대표 위치

| 영역 | Payload 의미 |
|---|---|
| TCP/IP | 실제 전송 데이터 |
| HTTP API | JSON Body |
| Streaming | 영상 데이터 |
| MQ | 메시지 본문 |
| DB Replication | 변경 데이터 |
| Kubernetes | API Object Data |
| AI System | Prompt / Token |
| Storage | File Content |

### 실제 흐름

**송신 측**
```
Application Data
↓
TCP Payload
↓
IP Payload
↓
Ethernet Payload
↓
Bit Stream
```

**수신 측**
```
Frame → Packet → Segment → Application Data
```

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Network + Memory**입니다.

### 자원별 영향

| 자원 | 영향 |
|---|---|
| Network | payload transmission |
| Memory | buffering |
| CPU | parsing / encryption |
| Disk | persistence |

### 특히 영향이 큰 요소

**Large Payload**
큰 payload는 네트워크 사용량 증가, fragmentation 증가, buffer pressure 증가를 유발합니다.

**Serialization / Deserialization**
JSON/XML parsing 비용이 발생합니다.

**Encryption**
TLS 처리 비용이 증가합니다.

**Compression**
CPU 사용량이 증가할 수 있습니다.

---

## 4. 왜 중요한가

Payload는 **시스템이 실제로 전달하려는 핵심 정보 그 자체**입니다.

### 중요한 이유

**비즈니스 가치 자체**
헤더가 아니라 payload가 실제 서비스 데이터입니다.

**Goodput 핵심 요소**
Goodput은 payload 전달량 기준입니다.

**네트워크 효율 판단 가능**
헤더 대비 payload 비율이 중요합니다.

**성능 최적화 핵심**
payload 크기 최적화는 latency 감소, bandwidth 절약, 비용 절감으로 이어집니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 대표 장애 유형

| 장애 유형 | 설명 |
|---|---|
| Oversized Payload | MTU 초과 → fragmentation 증가 |
| Huge Request Body | 메모리 압박 발생 |
| Malformed Payload | 파싱 실패 |
| Payload Explosion | JSON/XML 크기 폭증 |
| Serialization Error | 프로토콜 변환 실패 |
| Compression Failure | CPU saturation 유발 |
| Encryption Overhead | TLS 처리 지연 증가 |

### 실제 현상

| 현상 | 원인 |
|---|---|
| API timeout | huge payload |
| OOM | large request body |
| packet fragmentation | MTU 초과 |
| latency 증가 | serialization 비용 |
| CPU spike | compression / encryption |
| retry storm | payload corruption |

---

## 6. 핵심 메커니즘

### ① Encapsulation

각 계층은 상위 데이터 전체를 payload로 감싸서 전달합니다.

### ② Relative Payload

Payload는 절대 개념이 아닙니다.

| 계층 | Payload |
|---|---|
| TCP | Application Data |
| IP | TCP Segment |
| Ethernet | IP Packet |

### ③ MTU 제한

Payload가 너무 크면 fragmentation이 발생합니다.

### ④ Serialization

메모리 객체를 전송 가능한 바이트로 변환합니다.

- JSON
- Protobuf
- Avro

### ⑤ Compression

Payload를 압축할 수 있습니다.

- gzip
- zstd

### ⑥ Encryption

TLS는 payload를 암호화합니다.

> **핵심 포인트:** 헤더와 payload 비율이 네트워크 효율성을 직접 결정합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

| 도구 | 용도 |
|---|---|
| `tcpdump` | 패킷 캡처 |
| `wireshark` | Payload 분석 |
| `ss -tuln` | 소켓 상태 |
| `ip link` | MTU 확인 |

### Runtime

관측 대상:

- request body size
- response size
- serialization latency
- compression ratio
- buffer allocation

### Kubernetes

- **Ingress traffic** — `kubectl logs`
- **Service Mesh payload overhead** — Istio/Envoy metrics 확인
- **API Server Object Size** — 대형 manifest payload 분석

### 주요 관측 지표

| 지표 | 의미 |
|---|---|
| payload size | 실제 데이터 크기 |
| request body size | 요청 본문 |
| response size | 응답 크기 |
| fragmentation rate | 분할 비율 |
| serialization latency | 변환 시간 |
| compression ratio | 압축 효율 |
| retransmission | 재전송률 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*