# 데이터 통신 (Data Communication)

> 정독: 0회

## 1. 이 기술이 무엇인가

데이터 통신(Data Communication)은:

> 두 개 이상의 디지털 시스템 사이에서 **데이터를 전송·교환하는 전체 통신 과정**

### 핵심 특징

데이터 통신은 다음을 포함합니다:

- 데이터 생성
- 데이터 전송
- 데이터 수신
- 오류 제어
- 흐름 제어
- 경로 전달

### 전달 대상

| 데이터 유형 | 예시 |
|---|---|
| 텍스트 | 메시지 |
| 파일 | 이미지 / 영상 |
| 제어 데이터 | API 요청 |
| 스트림 | 실시간 영상 |
| 센서 데이터 | IoT |
| AI 데이터 | Prompt / Token |

### 기본 흐름

```
Sender → Encoding → Transmission → Decoding → Receiver
```

---

## 2. 시스템 어디에서 등장하는가

데이터 통신은 현대 시스템 전체에 존재합니다.

### 대표 영역

| 환경 | 데이터 통신 형태 |
|---|---|
| 인터넷 | TCP/IP |
| 클라우드 | VM 간 통신 |
| Kubernetes | Pod Network |
| DB Cluster | Replication |
| CDN | Content Delivery |
| AI Cluster | GPU Synchronization |
| IoT | Sensor Transmission |
| 금융 시스템 | Transaction Messaging |

### 실제 시스템 흐름

```
Application
↓
Transport
↓
IP Network
↓
Physical Medium
↓
Remote System
```

### 분산 시스템

```
Service A ↔ Service B ↔ Cache ↔ DB ↔ MQ
```

모든 연결이 데이터 통신입니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Network**이지만, 실제로는 모든 자원과 연결됩니다.

### 자원별 영향

| 자원 | 영향 |
|---|---|
| Network | packet transmission |
| CPU | packet processing |
| Memory | buffering |
| Disk | persistence / logging |

### 특히 중요한 요소

**Network** — latency, throughput, packet loss

**CPU** — encryption, checksum, protocol parsing

**Memory** — socket buffer, packet queue

**Disk** — replication log, persistent queue

---

## 4. 왜 중요한가

데이터 통신은 **모든 분산 시스템의 기본 동작 기반**입니다.

### 중요한 이유

**시스템 연결 자체**
통신이 없으면 분산 시스템이 성립 불가합니다.

**클라우드 기반 구조 핵심**
현대 서비스는 대부분 네트워크 기반입니다.

**실시간 서비스 가능**
스트리밍, 금융, AI, CDN 모두 데이터 통신 기반입니다.

**확장성 제공**
수천~수만 노드 연결이 가능합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

데이터 통신 장애는 시스템 장애로 직접 이어집니다.

### 대표 장애 유형

| 장애 유형 | 설명 |
|---|---|
| Packet Loss | 데이터 유실 |
| High Latency | 응답 지연 증가 |
| Network Partition | 노드 간 연결 단절 |
| Congestion | 대역폭 포화 |
| DNS Failure | 서비스 접근 실패 |
| TCP Retransmission Storm | Goodput 급감 |
| MTU Mismatch | Fragmentation 증가 |

### 실제 현상

| 현상 | 원인 |
|---|---|
| API timeout | latency 증가 |
| 서비스 단절 | routing failure |
| DB replication lag | packet loss |
| streaming 끊김 | jitter 증가 |
| node unreachable | network partition |
| retry storm | unstable communication |

---

## 6. 핵심 메커니즘

### ① Encoding

데이터를 비트로 변환합니다.

### ② Encapsulation

프로토콜 헤더를 추가합니다.

### ③ Addressing

목적지를 식별합니다 (MAC / IP / Port).

### ④ Routing

목적지까지 경로를 선택합니다.

### ⑤ Error Detection

손상 여부를 검출합니다 (checksum / CRC).

### ⑥ Flow Control

송수신 속도를 조절합니다.

### ⑦ Retransmission

손실 데이터를 재전송합니다.

### 핵심 구조

```
Payload
↓
Encapsulation
↓
Transmission
↓
Routing
↓
Decapsulation
↓
Application Delivery
```

> **핵심 특징:** 데이터 통신은 단순 전송이 아니라, 신뢰성과 무결성을 유지하며 데이터를 종단 간 전달하는 과정입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

| 도구 | 용도 |
|---|---|
| `ss -s` | 네트워크 상태 |
| `netstat -s` | 네트워크 통계 |
| `ip link` / `ip addr` | 인터페이스 상태 |
| `tcpdump` | 패킷 캡처 |
| `iperf3` | 처리율 측정 |
| `ip route` | 라우팅 확인 |

### Runtime

관측 대상:

- request latency
- retransmission
- connection pool
- socket usage
- serialization delay

### Kubernetes

| 명령어 | 용도 |
|---|---|
| `kubectl exec` | Pod 통신 상태 |
| `kubectl get svc` | 서비스 연결 |
| `kubectl get networkpolicy` | 네트워크 정책 |
| `kubectl get pods -n kube-system` | CNI 상태 |

### 주요 관측 지표

| 지표 | 의미 |
|---|---|
| latency | 지연 시간 |
| throughput | 처리율 |
| goodput | 유효 처리율 |
| packet loss | 손실률 |
| retransmission | 재전송률 |
| RTT | 왕복 시간 |
| jitter | 지연 변동 |
| PPS | packet/sec |
| bandwidth | 최대 링크 용량 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*