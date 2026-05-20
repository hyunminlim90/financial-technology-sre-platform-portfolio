# 트위스티드 페어 케이블 (Twisted Pair Cable)
## 1. 트위스티드 페어 케이블이란 무엇인가

> 정독: 0회

트위스티드 페어 케이블은:

> 두 가닥의 구리선을 서로 꼬아서(Twisted) 전자기 간섭(Noise)을 줄이도록 설계한 유선 통신 케이블

우리가 흔히 말하는 랜선, Ethernet Cable, UTP Cable 대부분이 이 구조입니다.

**"데이터 신호를 안정적으로 전달하기 위해 구리선을 서로 꼬아놓은 유선 통신 케이블"**

| 환경 | 사용 여부 |
|------|----------|
| 가정 LAN | 사용 |
| 사무실 네트워크 | 사용 |
| 서버실 | 사용 |
| 스위치 연결 | 사용 |
| 공유기 연결 | 사용 |
| 데이터센터 일부 구간 | 사용 |

> Ethernet 물리 계층(L1)의 **대표 전송 매체**입니다.

---

## 2. 시스템 어디에서 등장하는가

트위스티드 페어 케이블은 Ethernet 네트워크의 실제 물리 연결 구간에 등장합니다.

### 대표 흐름

```
Computer/NIC
      ↕
Twisted Pair Cable
      ↕
    Switch
      ↕
Router/Server
```

NIC 포트, 스위치 포트, 공유기 포트 사이를 실제로 연결합니다.

### 대표 규격

| 규격 | 최대 속도 |
|------|----------|
| Cat5e | 1Gbps |
| Cat6 | 10Gbps (짧은 거리) |
| Cat6a | 10Gbps |
| Cat7 | 고차폐 |
| Cat8 | 25G/40G 일부 지원 |

케이블 등급이 **네트워크 성능에 직접 영향**을 줍니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

트위스티드 페어 케이블은 **Network 자원**과 직접 연결됩니다.

| 자원 | 영향도 |
|------|--------|
| Network | 매우 큼 |
| CPU | 간접 영향 |
| Memory | 낮음 |
| Disk | 거의 없음 |

### Network 영향

대표 연결 항목: Link Speed, Signal Quality, Packet Error, Latency, Link Stability

### CPU 간접 영향

케이블 품질이 나쁘면 Packet Retransmission 증가, CRC Error 증가, Interrupt 증가로 **CPU 부담이 증가**할 수 있습니다.

---

## 4. 왜 중요한가

현대 Ethernet 네트워크 대부분이 이 케이블 위에서 동작하기 때문입니다.

> **Ethernet 네트워크의 실제 물리 신호 통로가 트위스티드 페어 케이블이다.**

| 역할 | 설명 |
|------|------|
| 전기 신호 전달 | Ethernet 비트 전송 |
| 노이즈 감소 | 전자기 간섭 감소 |
| 장거리 전송 안정화 | 신호 품질 유지 |
| 네트워크 속도 유지 | PHY 안정성 |
| 비용 효율 | Ethernet 대중화 핵심 |

> **Ethernet 인프라의 물리적 기반**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 네트워크 장애 상당수가 케이블 문제와 연결됩니다.

| 장애 | 원인 |
|------|------|
| CRC Error | 신호 손상 |
| Link Flap | 접촉 불량 |
| Packet Loss | 케이블 품질 문제 |
| 속도 저하 | Cat 규격 미달 |
| Duplex 협상 실패 | PHY 신호 불안정 |
| Intermittent Timeout | EMI 간섭 |
| RX/TX Error 증가 | 내부 선 손상 |

> **애플리케이션 장애처럼 보여도 실제로는 랜선 자체 불량인 경우가 존재한다.**

**예시:** Kubernetes Node 간 간헐적 통신 실패, DB Replication 끊김, VM Migration 실패, API Timeout 증가 등

---

## 6. 핵심 메커니즘

### 6-1. "Twisted" 구조가 핵심이다

```
두 구리선을 서로 꼬음
    → 자기장 상쇄
    → 노이즈 감소
```

전자기 간섭을 줄이기 위한 구조입니다.

### 6-2. Differential Signaling 기반이다

```
Signal A
Signal B (반대 위상)
    → 차이값으로 데이터 판별
```

외부 노이즈 영향을 줄입니다.

### 6-3. 8개의 선이 4쌍(Pair)으로 구성된다

```
2선 × 4쌍 = 총 8선
```

현대 **Gigabit Ethernet은 8개 선 전체를 사용**합니다.

### 6-4. PHY가 전기 신호를 해석한다

```
Digital Bit
    → PHY Encoding
    → Electrical Signal
    → Twisted Pair Cable
    → PHY Decode
    → Digital Bit
```

실제 전기 신호 통신입니다.

### 6-5. 차폐 여부가 환경에 영향을 준다

| 종류 | 특징 |
|------|------|
| UTP | 일반 환경 |
| STP | 전자파 강한 환경 |
| FTP | 일부 차폐 |

환경별 사용 목적이 다릅니다.

---

## 7. Linux / Runtime / Kubernetes에서 관측 방법

케이블 자체는 소프트웨어에서 직접 보이지 않지만, **NIC/PHY 상태로 간접 관측**됩니다.

### Link 상태 확인

```bash
ethtool eth0
```

**관측 가능:** Link Detected, Speed, Duplex

### Error 상태 확인

```bash
ip -s link
```

**관측 가능:** RX Error, TX Error, Dropped Packet

### NIC/PHY 상태 확인

```bash
ethtool -S eth0
```

**관측 가능:** CRC Error, PHY Error, Alignment Error

### PCIe NIC 확인

```bash
lspci -vv
```

**확인 가능:** NIC Vendor, Link Capability, Driver 정보

### Kubernetes

K8s Node 간 실제 물리 연결도 결국 케이블 기반입니다.

```
Pod
    → veth
    → NIC
    → Twisted Pair Cable
    → Switch
    → Other Node
```

Kubernetes 네트워크도 물리적으로는 **케이블 위에서 동작**합니다.

### Observability 도구

현대 시스템에서는 다음으로 케이블 상태 이상을 간접 추적합니다:

- ethtool
- switch telemetry
- NIC statistics
- packet drop metrics

### 대표 메트릭

| 메트릭 | 의미 |
|--------|------|
| CRC Error | 신호 손상 |
| Link Flap | 물리 연결 불안정 |
| Packet Loss | 전송 오류 |
| RX/TX Drop | PHY 문제 가능 |
| Retransmission | 링크 품질 저하 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*