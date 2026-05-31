# Power Delivery (전력 전달 계층)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**Power Delivery** 는 다음으로 이어지는 **전력 변환 및 공급 계층**이다.

```
PSU (Power Supply Unit)
 → Motherboard VRM
 → CPU / Memory / PCIe Device
```

### 핵심 목적

> 안정적인 전압과 전류를 **지속적으로 공급**하는 것

### 대표 구성 요소

| 구성 요소 | 역할 |
|-----------|------|
| VRM (Voltage Regulator Module) | 전압 변환 및 안정화 |
| PWM Controller | 전력 공급량 제어 |
| MOSFET | 고속 전력 스위칭 |
| Choke | 전류 평활화 |
| Capacitor | 전압 버퍼링 |

---

## 2. 시스템 어디에서 등장하는가

Power Delivery는 **Hardware Layer 전체**에 등장한다.

### 연결 대상

| 영역 | 연결 대상 |
|------|-----------|
| CPU | CPU Socket VRM |
| Memory | DIMM Power Rail |
| PCIe | NIC / NVMe / GPU |
| Storage | NVMe Controller |
| Network | High-speed NIC |

### E2E 전력 흐름

```
PSU
 → VRM
 → CPU Voltage Rail
 → CPU Core
 → JVM Thread
 → Java Application
```

> **핵심 정의**: 모든 연산의 **물리적 전력 기반**

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적으로 영향받는 자원: **CPU + Memory**

| 영향 범위 | 항목 |
|-----------|------|
| 직접 | CPU Frequency Stability |
| 직접 | Memory Signal Integrity |
| 직접 | PCIe Device Stability |
| 간접 | Network Throughput |
| 간접 | Disk IOPS |

---

## 4. 왜 중요한가

### FinTech 결제 시스템의 핵심 지표

- **24/365 안정성**
- **낮은 Tail Latency**
- **정합성 유지**

### Power Delivery 불안정 시 장애 전파

```
전력 불안정
 ├── CPU Downclock        → TPS 감소 / Latency 증가
 ├── ECC Error 증가       → Memory Corruption 위험
 ├── PCIe Reset           → NIC / NVMe 장애
 ├── Kernel Panic         → 서비스 중단
 └── System Reboot        → 전체 장애
```

> **전력 안정성 = 서비스 안정성**

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. Thermal Throttling

VRM 발열 증가 시 **CPU Frequency 강제 감소** 발생.

**증상**
- TPS 감소
- Java GC Time 증가
- API Latency 증가

---

### 5-2. Voltage Drop (Vdroop)

부하 급증 시 순간적인 전압 강하 발생.

```
CPU 0% → 100% 전환 순간  →  전압 순간 강하
```

심할 경우:
- 계산 오류
- Kernel Crash
- Machine Check Exception (MCE)

---

### 5-3. PCIe Device Reset

NIC / NVMe 전력 불안정 시 발생.

**증상**
- PCIe Link Reset
- NVMe Timeout
- Network Disconnect

---

### 5-4. Memory Instability

DIMM 전압 불안정 시 발생.

**증상**
- ECC Error 증가
- Memory Corruption
- JVM Crash

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### VRM (Voltage Regulator Module)

핵심 전압 변환 회로. 고전압을 CPU 동작 전압으로 강압.

```
12V  →  1.xV (CPU 동작 전압)
```

---

### PWM Controller

전력 공급량 제어. CPU Load 변화에 따라 실시간으로 전압 조절.

---

### MOSFET

고속 전력 스위칭 수행. **발열의 핵심 지점**.

---

### Multi-phase Power

부하를 여러 페이즈로 분산 공급.

| 장점 | 설명 |
|------|------|
| 발열 감소 | 단일 페이즈 집중 부하 방지 |
| 전압 안정성 증가 | 페이즈 간 전류 평활화 |

---

### OCP / OVP (보호 메커니즘)

| 보호 기능 | 의미 |
|-----------|------|
| OCP | Over Current Protection — 과전류 차단 |
| OVP | Over Voltage Protection — 과전압 차단 |

---

### Thermal Throttling

발열 임계치 초과 시 **CPU Frequency 강제 감소**. 하드웨어 자동 보호 메커니즘.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### CPU Frequency 확인

```bash
cpupower frequency-info
```

### Thermal 상태 확인

```bash
sensors
```

### Kernel Hardware Error 확인

```bash
dmesg
journalctl -k
```

확인 항목:
- **MCE (Machine Check Exception)**
- **PCIe Error**
- **Thermal Event**

### CPU Throttling 확인

```bash
turbostat
```

### Kubernetes Node 상태 확인

```bash
kubectl describe node
```

확인 포인트:
- Node reboot 이력
- CPU throttling 여부
- Hardware error 이벤트

### IPMI / BMC — 서버 전력·온도 확인

| 도구 | 설명 |
|------|------|
| IPMI | 일반 서버 관리 인터페이스 |
| iDRAC | Dell 전용 BMC |
| iLO | HPE 전용 BMC |
| Redfish | 표준 REST 기반 BMC API |

---

## 요약

```
Power Delivery
 ├── VRM / Multi-phase    → CPU 전압 안정화 / 발열 분산
 ├── PWM Controller       → 동적 전력 조절
 ├── MOSFET               → 스위칭 / 발열 집중 지점
 ├── OCP / OVP            → 하드웨어 보호 메커니즘
 └── Thermal Throttling   → 발열 임계치 초과 시 성능 강제 감소
```

> FinTech 결제 시스템에서 Power Delivery는 단순한 전원 공급 장치가 아니라,  
> **CPU Frequency · Memory 안정성 · PCIe 신뢰성을 결정하는 서비스 가용성의 물리적 기반**이다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*