# 서비스 데이터 단위 (SDU)

> 정독: 0회

## 1. 이 기술이 무엇인가

서비스 데이터 단위(SDU, Service Data Unit)는:
 
> **상위 계층이 하위 계층에게 전달한 순수 데이터 단위**를 의미

### 핵심 정의

어떤 계층이 데이터를 처리할 때, 상위 계층에서 내려온 데이터로 **아직 현재 계층의 제어 정보(PCI)가 붙기 전 상태**를 SDU라고 부릅니다.

SDU는 **현재 계층 관점에서의 입력 데이터**입니다.

### SDU와 PCI 관계

현재 계층은 SDU + PCI를 결합하여 PDU를 생성합니다.

```
PDU = PCI + SDU
```

### 예시

**TCP 계층**
```
Application Data  →  SDU
TCP Header        →  PCI
결과              →  TCP Segment (PDU)
```

**IP 계층**
```
TCP Segment  →  SDU
IP Header    →  PCI
결과         →  IP Packet (PDU)
```

---

## 2. 시스템 어디에서 등장하는가

SDU는 **모든 계층 간 데이터 전달 지점**에서 등장합니다.

### 등장 위치

| 계층 | SDU 예시 |
|---|---|
| Application | User Data |
| TCP | Application Payload |
| IP | TCP Segment |
| Ethernet | IP Packet |
| VXLAN | Inner Ethernet Frame |

### OSI / TCP-IP 공통 개념

SDU는 다음 모든 계층형 네트워크 구조에서 사용됩니다:

- OSI 모델 / TCP-IP 모델
- SDN / Overlay Network
- MPLS / VPN

### 클라우드 / K8s 환경

다음 모두 내부적으로 SDU 개념을 사용합니다:

- CNI / Service Mesh / Envoy
- Overlay Tunnel (VXLAN / GRE / IPSec)

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Network**이지만, 실제 구현에서는 **CPU/Memory 영향도 큽니다.**

### 자원별 영향

| 자원 | 영향 |
|---|---|
| Network | packet size 변화 |
| CPU | encapsulation parsing |
| Memory | packet buffering |
| Disk | 거의 없음 |

### CPU 영향이 큰 이유

계층마다 다음 작업이 발생합니다:

- SDU 추출
- PCI 결합
- checksum 계산
- segmentation
- reassembly

### Memory 영향

패킷은 계층마다 buffer queue를 통과하므로, socket buffer / kernel buffer / NIC ring buffer 사용량 증가와 연결됩니다.

---

## 4. 왜 중요한가

SDU는 **계층 간 데이터 전달의 기준 단위**입니다.

### 중요 이유

계층형 네트워크 구조는 다음을 위해 SDU 개념을 사용합니다:

- 계층 분리 및 독립성 유지
- 프로토콜 교체 가능
- 확장성 확보

### 핵심 철학

현재 계층은 상위 계층 내부 구조를 알 필요 없이, **오직 SDU라는 데이터 덩어리만 처리합니다.**

### 이 구조 덕분에 가능한 것

- Ethernet → Wi-Fi 변경 가능
- IPv4 → IPv6 변경 가능
- TCP → QUIC 변경 가능
- Overlay 추가 가능 — 상위 계층 영향 최소화

---

## 5. 실제 장애와 어떤 관련이 있는가

### 대표 문제

| 문제 유형 | 설명 |
|---|---|
| MTU 초과 | SDU가 너무 크면 fragmentation 발생 |
| TCP Segmentation 증가 | CPU 부하 증가 |
| Reassembly 실패 | 패킷 손실 발생 |
| Overlay Encapsulation 증가 | SDU 크기 감소 |
| Jumbo Frame 불일치 | packet drop 발생 |

### Kubernetes 환경 대표 문제

Overlay Network 사용 시 실제 사용 가능한 SDU 크기가 감소합니다.

```
1500 MTU
- VXLAN Header
- IP Header
- UDP Header
→ 실제 Payload 감소
```

### 실제 현상

| 현상 | 원인 |
|---|---|
| packet fragmentation | oversized SDU |
| retransmission | reassembly 실패 |
| throughput 감소 | encapsulation 증가 |
| latency 증가 | fragmentation overhead |
| pod network issue | MTU mismatch |

---

## 6. 핵심 메커니즘

핵심 메커니즘은 **상위 계층의 PDU가 하위 계층의 SDU가 된다**는 것입니다.

### 핵심 흐름

```
Application
  Application Data = SDU
       ↓
TCP
  TCP Header + SDU = TCP Segment (PDU)
       ↓
IP
  TCP Segment → IP 계층 SDU
       ↓
Ethernet
  IP Packet → Ethernet 계층 SDU
```

### 전체 구조

```
상위 PDU
↓
하위 SDU
↓
PCI 추가
↓
새로운 PDU 생성
```

> **핵심 특징:** SDU는 절대적인 데이터 종류가 아니라, **현재 계층 관점의 데이터 단위**입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

| 도구 | 용도 |
|---|---|
| `ip link` | MTU 확인 |
| `ss -t` | TCP segmentation 확인 |
| `tcpdump` | packet capture |
| `ethtool -k eth0` | NIC offloading 확인 |

**Wireshark** 로 계층별 SDU / PCI / PDU 구조를 확인할 수 있습니다.

### Kubernetes

- **Pod MTU 확인** — `kubectl exec`
- **Overlay Network** — 다음 환경에서 SDU 감소 발생: VXLAN / Geneve / GRE / IPSec

### 관측 포인트

| 항목 | 의미 |
|---|---|
| MTU | 최대 PDU 크기 |
| MSS | TCP SDU 제한 |
| fragmentation | SDU 초과 |
| reassembly | SDU 복원 |
| encapsulation depth | SDU 감소 원인 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*