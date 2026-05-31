# 물리 (Physical)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**물리(Physical)** 는:

> 가상화되거나 추상화된 개념이 아니라, **실제로 존재하는 하드웨어 자원과 공간 자체**

즉, 실제 서버, 실제 메모리 칩, 실제 디스크, 실제 네트워크 케이블, 실제 전력, 실제 냉각 장비 같은 **현실 세계의 물리적 실체**를 뜻한다.

SRE 관점에서:

> **모든 가상화(VM/Container/Cloud)의 최종 기반**이 바로 물리다.

---

## 2. 시스템 어디에서 등장하는가

물리는 **시스템 전체**에 존재한다.

### Compute
- 서버 본체
- 반도체 칩
- 연산 회로
- 전력 회로

### Memory
- DRAM 칩
- 메모리 슬롯
- 메모리 버스

### Storage
- SSD NAND
- NVMe Controller
- 디스크 플래터

### Network
- 광케이블
- 스위치
- NIC / PHY
- 라우터

### Datacenter
- Rack
- Cooling
- UPS
- Power Distribution

### Cloud / Virtualization
클라우드도 결국 **실제 데이터센터의 물리 장비 위**에서 동작한다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

물리는 **모든 자원의 실체 자체**다. CPU도, Memory도, Disk도, Network도 모두 물리다.

| 영역 | 물리적 한계 |
|------|-----------|
| Compute | 실제 연산 능력 한계 |
| Memory | 실제 장착된 DRAM 용량 |
| Network | 실제 광케이블/NIC 속도 |
| Storage | 실제 SSD Controller 및 NAND 성능 |
| Power/Thermal | 전력 공급 및 냉각 한계 |

---

## 4. 왜 중요한가

현대 시스템은 VM, Container, Kubernetes, Cloud 같은 추상화 위에서 동작한다. 하지만 실제 성능과 안정성은 **최종적으로 물리 한계에 의해 결정**된다.

- vCPU 많아도 실제 물리 코어 부족 가능
- Container 많아도 실제 RAM 부족 가능
- Pod 많아도 실제 NIC bandwidth 부족 가능

> ⚠️ **가상 자원은 무한처럼 보여도, 물리 자원은 유한**하다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### Power Failure
실제 전력 장애 시 Node Shutdown / Data Loss / Service Outage 발생

### Thermal Issue
냉각 실패 시 Throttling → Hardware Failure → Random Reboot 발생

### Physical NIC Failure
실제 포트/케이블 문제 시 Packet Loss / Network Partition 발생

### Disk Wear-out
SSD NAND 수명 종료 시 I/O Error → DB Corruption 발생

### Memory Bit Error
실제 DRAM 셀 오류 시 Kernel Panic / Silent Corruption 발생

### Rack / Datacenter Failure
물리 시설 장애 시 Entire Zone Outage 발생

### Overcommit Collapse
가상 자원 과할당 시 CPU Steal → OOM → Massive Latency 발생

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

### Virtual ≠ Infinite
가상화는 **물리 자원을 논리적으로 분할**하는 기술이다. 실제 기반은 항상 물리다.

### Physical Resource Constraint
모든 시스템에는 전력 한계, 열 한계, 공간 한계, bandwidth 한계, latency 한계가 존재한다.

### Physical Locality
실제 물리 거리도 중요하다. 예: `NUMA`, `Rack Distance`, `Cross-Region Latency`

> 물리적으로 멀수록 **Latency 증가**한다.

### Hardware Failure is Real
소프트웨어는 재시작 가능하지만 SSD 수명 종료, Fan 고장, Power 장애는 **실제 장비 교체** 필요.

### Physical → Logical Abstraction
현대 시스템 구조:

```
Physical Hardware
→ Hypervisor
→ VM
→ Container
→ Application
```

SRE는 **논리 장애 뒤의 물리 원인**까지 추적해야 한다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Physical CPU / NUMA
```bash
lscpu
numactl --hardware
```

### Physical Memory
```bash
dmidecode -t memory
```

### Physical Disk
```bash
lsblk
nvme list
```

### Physical NIC
```bash
lspci
ethtool eth0
```

### Thermal / Power
```bash
sensors
ipmitool sensor
```

### Hardware Error
```bash
dmesg
journalctl -k
```

### Kubernetes 대표 관측 포인트
- `Node Pressure`
- `CPU Throttling`
- `OOMKilled`
- `DiskPressure`
- `NetworkUnavailable`

### Virtualization 환경
`top` 에서 아래 지표 확인 가능:
- `steal time`
- `load imbalance`

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*