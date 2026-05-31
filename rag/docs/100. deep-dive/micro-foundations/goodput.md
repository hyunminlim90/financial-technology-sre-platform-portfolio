# 유효 처리율 (Goodput)

> 정독: 0회

## 1. 이 기술이 무엇인가

유효 처리율(Goodput)은:

> 최종 애플리케이션까지 **실제로 성공적으로 전달된 순수 데이터(Payload)의 전송률**

### 핵심 특징

Goodput은 다음을 제외하고 실제 유효 데이터만 계산합니다:

- 프로토콜 헤더 제외
- 재전송 데이터 제외
- 중복 패킷 제외

### 관계 정리

```
Bandwidth ≥ Throughput ≥ Goodput
```

| 개념 | 의미 |
|------|------|
| Bandwidth | 이론적 최대 링크 용량 |
| Throughput | 실제 전체 전송량 |
| Goodput | 순수 사용자 데이터 전달량 |

### 핵심 수식

```
Goodput = Throughput − (Protocol Overhead + Retransmissions)
```

<details>
<summary>Deep Dive</summary></br>

Application Software(애플리케이션 소프트웨어) [[M]](../../100-deep-dive/micro-foundations/application-software.md)  
Payload(페이로드) [[M]](../../100-deep-dive/micro-foundations/payload.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

Goodput은 실제 사용자 경험이 존재하는 모든 시스템에서 중요합니다.

| 시스템 | Goodput 중요성 |
|--------|---------------|
| 파일 다운로드 | 체감 속도 |
| 스트리밍 | 영상 품질 |
| CDN | 콘텐츠 전달 효율 |
| Object Storage | 업로드 효율 |
| DB Replication | 복제 효율 |
| Kubernetes Service Mesh | 오버헤드 분석 |
| AI Distributed Training | GPU 동기화 효율 |
| API Gateway | 실제 payload 처리량 |

### 실제 흐름

```
Application Payload
→ TCP/IP Encapsulation
→ Network Transfer
→ Decapsulation
→ Final Payload Delivery
```

> Goodput은 마지막 단계의 성공 전달량입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **Network 자원**입니다. 하지만 실제 Goodput은 전체 시스템 상태의 영향을 받습니다.

| 자원 | 영향 |
|------|------|
| Network | 패킷 손실, 재전송 |
| CPU | packet processing |
| Memory | socket buffer |
| Disk | write bottleneck |

### 특히 중요한 요소

- **Packet Loss**: 재전송 증가 → Goodput 급감
- **TCP Congestion**: 혼잡 제어 발동 시 payload 전송량 감소
- **MTU 문제**: Fragmentation 발생
- **TLS 오버헤드**: 암호화 헤더 증가
- **Overlay Network**: VXLAN/Geneve encapsulation 증가

---

## 4. 왜 중요한가

Goodput은 **사용자가 실제로 체감하는 진짜 데이터 전달 효율**을 의미합니다.

- **실제 서비스 품질 결정**: 사용자가 느끼는 다운로드 속도는 Throughput이 아니라 Goodput
- **네트워크 품질 측정 가능**: 재전송과 오버헤드가 얼마나 큰지 파악 가능
- **비용 효율성 판단 가능**: 클라우드 네트워크 비용 대비 실제 payload 전달량 분석 가능
- **분산 시스템 효율 측정**: 클러스터 내부 통신 효율 확인 가능

---

## 5. 실제 장애와 어떤 관련이 있는가

Goodput 저하는 실제 서비스 장애 체감으로 직결됩니다.

| 장애 유형 | 영향 |
|-----------|------|
| Packet Retransmission 폭증 | 실제 payload 전달량 감소 |
| TCP Window Collapse | 전송 속도 급감 |
| Network Congestion | 혼잡으로 payload 전달 효율 감소 |
| MTU Mismatch | Fragmentation 증가 |
| Service Mesh Overhead | 사이드카 프록시 오버헤드 증가 |
| TLS Handshake 증가 | 암호화 처리 비용 증가 |
| Overlay Network 병목 | VXLAN encapsulation 비용 증가 |

### 실제 현상

| 현상 | 원인 |
|------|------|
| 다운로드 느림 | Goodput 감소 |
| 영상 끊김 | 재전송 증가 |
| API timeout | payload delivery 실패 |
| DB replication lag | effective throughput 감소 |

---

## 6. 핵심 메커니즘

핵심 메커니즘은 6개입니다.

| # | 메커니즘 | 설명 |
|---|----------|------|
| ① | Protocol Overhead | 캡슐화 과정에서 헤더 추가 (Ethernet, IP, TCP, TLS Header) |
| ② | Retransmission | 패킷 손실 시 동일 payload 재전송 → Throughput은 증가할 수 있지만 Goodput은 감소 |
| ③ | Congestion Control | TCP 혼잡 제어가 payload 전송률 제한 |
| ④ | Fragmentation | MTU 초과 시 패킷 분할 발생 → 효율 감소 |
| ⑤ | ACK Traffic | TCP ACK 역시 네트워크 자원 사용 |
| ⑥ | Encapsulation Depth | Overlay network가 많아질수록 오버헤드 증가 |

### Encapsulation Depth 예시

```
TCP/IP
→ VXLAN
→ IPSec
→ TLS
```

### Goodput 저하 주요 원인

| 원인 | 영향 |
|------|------|
| Packet Loss | 재전송 증가 |
| Small MTU | fragmentation |
| High RTT | ACK 지연 |
| Queue Overflow | packet drop |
| CPU Saturation | packet processing delay |
| Overlay Network | encapsulation overhead |
| Encryption | TLS overhead |

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 재전송 확인
netstat -s

# TCP 상태
ss -s

# NIC 통계
ethtool -S eth0

# 패킷 드롭
ip -s link

# 처리율 측정
iperf3
```

### Kubernetes

```bash
# Pod Network Usage
kubectl top pod

# CNI 상태
kubectl get pods -n kube-system
```

- **Service Mesh 분석**: Istio/Linkerd sidecar overhead 확인
- **Overlay Network 분석**: VXLAN/Geneve encapsulation 확인

### 주요 관측 지표

| 지표 | 의미 |
|------|------|
| throughput | 전체 처리량 |
| goodput | 순수 payload 처리량 |
| retransmission rate | 재전송률 |
| packet loss | 손실률 |
| RTT | 왕복 지연 |
| TCP window | 전송 가능 크기 |
| PPS | 초당 패킷 수 |
| MTU | 최대 전송 단위 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*