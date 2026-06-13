# 네트워크 처리율 (Network Throughput)

> 정독: 0회

## 1. 이 기술이 무엇인가

네트워크 처리율은:

> 일정 시간 동안 네트워크를 통해 **실제로 성공적으로 전달된 데이터 양**

단위: `bps` / `Mbps` / `Gbps`

### 핵심 특징

처리율은 이론적 최대 속도가 아니라 **실제 전달 성공량**입니다.

| 개념 | 의미 |
|------|------|
| Bandwidth | 이론적 최대 용량 |
| Throughput | 실제 전달량 |
| Goodput | 순수 사용자 데이터량 |

```
Bandwidth ≥ Throughput ≥ Goodput
```

<details>
<summary>Deep Dive</summary></br>

Network(네트워크) [[M]](../../100-deep-dive/micro-foundations/network.md)  
Goodput(유효 처리율) [[M]](../../100-deep-dive/micro-foundations/goodput.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

네트워크가 존재하는 모든 시스템에서 등장합니다.

| 영역 | 처리율 영향 |
|------|------------|
| 데이터센터 백본망 | East-West Traffic |
| 클라우드 네트워크 | VM 간 통신 |
| Kubernetes | Pod Networking |
| CDN | 콘텐츠 전송 |
| DB Replication | 데이터 복제 |
| Object Storage | 파일 업로드 |
| 스트리밍 | 영상 전송 |
| AI Cluster | GPU Node Sync |

### 실제 흐름

```
Application
→ TCP/IP Stack
→ NIC
→ Switch
→ Router
→ Remote Host
```

> 전체 구간 성능의 결과가 처리율입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **Network 자원**입니다. 하지만 실제로는 4개 자원이 모두 관여합니다.

| 자원 | 영향 |
|------|------|
| Network | 링크 속도, 패킷 손실 |
| CPU | 패킷 처리, checksum |
| Memory | socket buffer |
| Disk | 저장 장치 I/O 병목 |

### 특히 중요한 요소

- **NIC 처리 성능**: 패킷 PPS(Packet Per Second) 처리 능력
- **CPU Interrupt 처리**: 네트워크 인터럽트 폭증 시 처리율 저하
- **TCP Buffer**: 버퍼 부족 시 throughput 감소
- **Storage Write Speed**: 디스크가 느리면 수신 데이터 저장 병목 발생

---

## 4. 왜 중요한가

처리율은 **시스템이 실제로 얼마나 데이터를 처리할 수 있는가**를 나타내는 핵심 지표입니다.

- **사용자 체감 성능 결정**: 다운로드 속도, 스트리밍 품질, API 응답 속도
- **병목 분석 가능**: 어느 계층이 제한 요소인지 식별 가능
- **인프라 설계 기준**: NIC 선택, Spine-Leaf 설계, Queue 설계, Load Balancer 설계
- **확장성 판단 가능**: 트래픽 증가 대응 가능 여부 확인

---

## 5. 실제 장애와 어떤 관련이 있는가

처리율 저하는 대규모 서비스 장애로 이어집니다.

| 장애 유형 | 영향 |
|-----------|------|
| Packet Loss 증가 | 재전송 증가 → 처리율 감소 |
| NIC Saturation | 네트워크 카드 포화 |
| Buffer Overflow | 패킷 드롭 발생 |
| TCP Congestion | 혼잡 제어로 throughput 급감 |
| MTU 문제 | Fragmentation 증가 |
| Queue Bottleneck | 패킷 대기열 폭증 |
| CPU SoftIRQ 과부하 | 패킷 처리 지연 발생 |
| Kubernetes Overlay 병목 | VXLAN encapsulation overhead 증가 |

---

## 6. 핵심 메커니즘

핵심 메커니즘은 7개입니다.

| # | 메커니즘 | 설명 |
|---|----------|------|
| ① | Bandwidth | 링크의 이론적 최대 용량 (1 Gbps, 10 Gbps, 100 Gbps) |
| ② | Throughput | 실제 성공 전달량 |
| ③ | Goodput | 헤더·재전송 제외 순수 데이터량 |
| ④ | Protocol Overhead | 헤더가 처리율 일부를 사용 (Ethernet, IP, TCP, TLS Header) |
| ⑤ | Retransmission | 패킷 손실 시 재전송 발생 → 처리율 감소 |
| ⑥ | Congestion Control | TCP가 혼잡 시 전송 속도 자동 감소 (Slow Start, Congestion Avoidance) |
| ⑦ | Buffering | 네트워크 버퍼 크기가 throughput에 영향 (Socket Buffer, NIC Queue, Ring Buffer) |

### 처리율 저하 주요 원인

| 원인 | 영향 |
|------|------|
| Packet Loss | 재전송 증가 |
| High Latency | ACK 지연 |
| Small Window | 전송 제한 |
| CPU Saturation | 패킷 처리 실패 |
| Disk Bottleneck | 저장 병목 |
| MTU Mismatch | Fragmentation |

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 인터페이스 처리율
sar -n DEV

# NIC 통계
ethtool -S eth0

# 패킷 드롭 확인
ip -s link

# TCP 상태
ss -s

# 네트워크 사용량
iftop
nload

# 재전송 확인
netstat -s
```

### Kubernetes

```bash
# Pod 네트워크 사용량
kubectl top pod

# Node 트래픽 확인
kubectl top node

# CNI 상태 확인
kubectl get pods -n kube-system
```

- **Overlay Network**: VXLAN/Geneve overhead 분석

### 관측 핵심 지표

| 지표 | 의미 |
|------|------|
| throughput | 실제 처리량 |
| bandwidth utilization | 링크 사용률 |
| retransmission | 재전송 |
| packet loss | 패킷 손실 |
| PPS | 초당 패킷 수 |
| queue length | 큐 길이 |
| latency | 지연 시간 |
| jitter | 지연 편차 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*