# 통신 프로토콜 (Communication Protocol)

> 정독: 0회

## 1. 이 기술이 무엇인가

통신 프로토콜은:

> 서로 다른 시스템이 데이터를 주고받기 위해 반드시 따라야 하는 **형식·의미·순서·오류 처리 규칙의 집합**

핵심은 다음을 표준화하는 것입니다:

- 데이터 형식
- 메시지 해석 방식
- 송수신 순서
- 오류 처리
- 재전송 조건
- 연결 관리

---

## 2. 시스템 어디에서 등장하는가

통신 프로토콜은 모든 네트워크 계층에 존재합니다.

### 네트워크 계층별 예시

| 계층 | 대표 프로토콜 |
|------|--------------|
| Application | HTTP, DNS, SMTP |
| Transport | TCP, UDP |
| Internet | IP, ICMP |
| Network Access | Ethernet, Wi-Fi |

### 실제 흐름

```
Application Data
→ TCP/UDP Protocol
→ IP Protocol
→ Ethernet Protocol
→ Physical Signal
```

> 통신 프로토콜은 데이터가 이동하는 모든 단계의 공통 규칙입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **Network 자원**입니다.

| 자원 | 영향 |
|------|------|
| Network | 패킷 전송, 지연, 손실, 대역폭 |
| CPU | 헤더 처리, 암호화, 체크섬, 재전송 제어 |
| Memory | 버퍼, 큐, 연결 상태 저장 |
| Disk | 로그, 패킷 캡처, 트래픽 기록 |

---

## 4. 왜 중요한가

통신 프로토콜이 없으면 서로 다른 시스템은 데이터를 해석할 수 없습니다.

- **상호 운용성**: 서로 다른 OS, 장비, 클라우드, 네트워크가 통신 가능해집니다.
- **표준화**: 데이터 구조와 처리 규칙이 통일됩니다.
- **오류 제어**: 손상, 유실, 중복, 순서 뒤바뀜을 처리할 수 있습니다.
- **계층 분리**: 각 프로토콜이 자기 계층의 역할만 수행할 수 있습니다.
- **장애 분석 가능**: 어느 계층의 프로토콜에서 문제가 발생했는지 분리할 수 있습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 네트워크 장애 대부분은 특정 프로토콜 계층에서 발생합니다.

| 장애 유형 | 증상 |
|-----------|------|
| DNS 장애 | 도메인 이름을 IP로 변환하지 못함 |
| TCP 장애 | 연결 실패, 재전송 증가, timeout 발생 |
| IP 장애 | 라우팅 실패, TTL 초과, MTU 문제 발생 |
| Ethernet 장애 | CRC error, frame drop, link down 발생 |
| TLS 장애 | 인증서 오류, handshake 실패 발생 |
| 프로토콜 불일치 | 클라이언트와 서버가 서로 다른 버전·형식을 기대할 때 통신 실패 |

---

## 6. 핵심 메커니즘

핵심 메커니즘은 8개입니다.

| # | 메커니즘 | 설명 |
|---|----------|------|
| ① | Syntax | 데이터 구조와 형식 규칙 (Header, Payload, Trailer) |
| ② | Semantics | 각 필드와 제어값의 의미 (ACK = 수신 확인, SYN = 연결 시작, TTL = 생존 시간) |
| ③ | Timing | 전송 순서와 시간 조건 (timeout, retransmission interval, flow control) |
| ④ | Header | 프로토콜 제어 정보를 담는 영역 |
| ⑤ | Payload | 실제 전달하려는 데이터 |
| ⑥ | Encapsulation | 상위 데이터를 하위 프로토콜이 감싸는 과정 |
| ⑦ | Decapsulation | 수신 측에서 헤더를 계층별로 해석하고 제거하는 과정 |
| ⑧ | State Management | 연결 상태 관리 (TCP connection state, session state, retry state) |

### Encapsulation 흐름

```
Application Data
→ TCP Segment
→ IP Packet
→ Ethernet Frame
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 인터페이스 확인
ip addr
ip link

# 라우팅 확인
ip route

# 연결 상태 확인
ss -ant

# DNS 확인
dig
nslookup

# 패킷 캡처
tcpdump
wireshark
```

### Kubernetes

```bash
# Pod 통신 확인
kubectl get pods -o wide

# Service 확인
kubectl get svc

# Ingress 확인
kubectl get ingress

# DNS/CoreDNS 확인
kubectl get pods -n kube-system
```

### 관측 핵심 지표

| 지표 | 의미 |
|------|------|
| latency | 응답 지연 |
| packet loss | 패킷 손실 |
| retransmission | 재전송 |
| connection timeout | 연결 실패 |
| error rate | 오류율 |
| handshake failure | 연결 협상 실패 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*