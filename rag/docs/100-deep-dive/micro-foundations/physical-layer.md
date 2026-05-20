# 물리 계층 (Physical Layer)
## 1. 물리 계층(Physical Layer)이란 무엇인가

> 정독: 0회

물리 계층은:

> **디지털 데이터(0과 1)를 실제 물리 신호(전기/광 신호)로 변환하여 전송하는 네트워크의 최하단 계층(L1)**

OSI 7계층에서 가장 아래에 위치합니다.

### 핵심 역할

물리 계층은 **"비트를 실제 세상으로 내보내는 역할"**을 담당합니다.

전압, 전류, 빛, 전파 같은 실제 물리 현상으로 데이터를 변환합니다.

> **매우 중요한 포인트**
> 물리 계층은 데이터의 의미를 이해하지 않습니다.
> HTTP인지, 이미지인지, 동영상인지, 금융 데이터인지 모릅니다.
> 오직 **0과 1의 신호 전달**만 담당합니다.

<details>
<summary>Deep Dive</summary></br>



</details></br>

## 2. 시스템 어디에서 등장하는가

물리 계층은 실제 하드웨어가 존재하는 모든 통신 환경에서 등장합니다.

### 유선 Ethernet

```
NIC → PHY Chip → RJ45 Port → UTP Cable → Switch
```

### 광 네트워크

```
NIC → Optical Transceiver → Fiber Cable
```

### Wi-Fi

```
Wireless NIC → RF Signal → Antenna
```

### 데이터센터

특히 중요합니다. Spine-Leaf Network, ToR Switch, 100GbE, 400GbE 모두 물리 계층 기술 위에서 동작합니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

물리 계층은 특히 **Network 자원**에 직접적인 영향을 줍니다.

하지만 실제로는 CPU, Memory, Storage 성능에도 간접 영향을 줍니다.

### 왜 그런가?

물리 계층 문제가 발생하면 Packet Loss, Retransmission, Latency 증가가 발생하기 때문입니다.

결국 다음으로 이어집니다.

- CPU Retry 증가
- Buffer 증가
- TCP Congestion
- Application Timeout

### 대표 영향 요소

| 요소 | 영향 |
|------|------|
| Link Speed | 처리량 |
| Duplex Mode | 충돌 여부 |
| Signal Quality | Bit Error |
| Cable Quality | Packet Loss |
| Optical Power | Link Stability |

---

## 4. 왜 중요한가

물리 계층은 **모든 네트워크 통신의 실제 기반**입니다.

상위 계층은 결국 물리 계층이 안정적으로 비트를 전달한다는 가정 위에서만 동작 가능합니다.

### 매우 중요한 이유

상위 계층은 TCP 재전송, HTTP Retry, Application Recovery 같은 보정이 가능합니다.

하지만 **물리 계층 자체가 불안정하면 전체 시스템이 흔들립니다.**

### 실제 운영에서 매우 중요

SRE/Infra 관점에서는 **"애플리케이션 장애처럼 보였는데 실제 원인은 L1 장애"**인 경우가 많습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

물리 계층 장애는 실제 운영에서 매우 흔합니다.

### 케이블 불량

원인: 접촉 불량, 손상, 노이즈

증상: packet loss, CRC error, link flap

### Duplex Mismatch

원인: Server = Full Duplex / Switch = Half Duplex

증상: collision 증가, throughput 급감, latency 증가

### 광모듈 문제

원인: 광 출력 부족, 오염, 케이블 굽힘

증상: intermittent disconnect, unstable link

### EMI (전자기 간섭)

특히 산업 환경에서 발생합니다.

증상: bit corruption, CRC 증가

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

물리 계층의 핵심은 **Encoding, Clock Synchronization, Signal Transmission**입니다.

### ① Encoding

디지털 비트를 전기/광 신호 패턴으로 변환합니다.

```
10110010
   ↓
전압 변화 / 빛 펄스
```

### ② Clock Synchronization

송신자와 수신자가 **비트 경계(Bit Boundary)**를 동일하게 인식해야 합니다.

그래서 Preamble, Clock Recovery 같은 메커니즘이 존재합니다.

### ③ Signal Integrity

멀리 갈수록 신호는 약해집니다. 그래서 증폭, Equalization, Error Detection이 중요합니다.

> **매우 중요한 관점**
> 물리 계층은 **"논리(Logical)가 아니라 물리(Physical)"**입니다.
> 전압, 전류, 주파수, 광세기, 노이즈 같은 실제 전자공학 세계와 직접 연결됩니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

#### 매우 중요한 지표

**Link State 확인**

```bash
ip link
# state UP / state DOWN
```

**Speed / Duplex 확인**

```bash
ethtool eth0
# Speed: 10000Mb/s
# Duplex: Full
```

**Error 통계 확인**

```bash
ethtool -S eth0
# CRC Error, Frame Error, RX Drop, TX Error
```

### Kubernetes

K8s 자체보다 **Node NIC 상태**가 중요합니다.

CNI packet loss, Node NotReady, Pod timeout의 원인이 실제론 L1 장애일 수 있습니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*