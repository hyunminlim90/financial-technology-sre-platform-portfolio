# Physical Fault (물리적 결함)
## **Micro Foundations — 범용 시스템 관점**

> 정독: 0회

## 1. 이 기술이 무엇인가

**Physical Fault(물리적 결함)**는:

> 하드웨어 장비나 물리적 전기·전자 구성 요소가 정상 규격대로 동작하지 못하는 **기계적·전기적 고장 상태**

핵심은:

- 소프트웨어 코드의 논리 문제가 아니라,
- **현실 세계 물리 장비 자체의 이상 현상**

이라는 점이다.

**대표 예시:**

| 유형 | 예시 |
|------|------|
| 메모리 | RAM bit flip |
| 저장장치 | SSD bad block, HDD head crash |
| 네트워크 | NIC failure |
| 전원 | PSU failure, power surge |
| 열 관리 | overheating |
| 물리 연결 | cable disconnection |
| 기판 | motherboard capacitor damage |

> **핵심:** Physical Fault는 "코드의 실패"가 아니라 **"물질의 실패"**다.

---

## 2. 시스템 어디에서 등장하는가

물리적 결함은 시스템 전체 인프라 **어디서든** 발생할 수 있다.

### CPU
- thermal runaway
- transistor degradation
- unstable clock

### Memory (RAM)
- bit flip
- ECC error
- unstable DIMM

### Disk / Storage
- SSD wear-out
- NAND degradation
- HDD bad sector

### Network
- NIC failure
- cable damage
- optical transceiver issue

### Power System
- PSU failure
- voltage fluctuation
- UPS battery degradation

### Cooling System
- fan failure
- airflow blockage
- overheating

### Datacenter Environment
- temperature anomaly
- humidity issue
- rack power imbalance

> **결론:** Physical Fault는 시스템을 구성하는 **실제 물리 장비 계층 전체**에서 발생 가능하다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

물리적 결함은 특정 자원을 **직접적으로 파괴하거나 오염**시킨다.

### CPU 영향
- **원인:** overheating, unstable execution, machine check exception
- **결과:** kernel panic, process crash, node instability

### Memory 영향
- **원인:** bit corruption, ECC failure, memory instability
- **결과:** corrupted state, invalid computation, unpredictable behavior

### Disk 영향
- **원인:** bad sector, write failure, controller failure
- **결과:** data corruption, I/O timeout, persistence failure

### Network 영향
- **원인:** packet loss, NIC malfunction, cable issue
- **결과:** retransmission spike, latency increase, connection instability

> **핵심:** Physical Fault는 논리 오류 이전에 **물리 자원 자체를 불안정**하게 만든다.

---

## 4. 왜 중요한가

물리적 결함은 **소프트웨어가 완벽해도 시스템을 무너뜨릴 수 있다.**

왜냐하면 모든 소프트웨어는 결국 **물리 장비 위에서 실행**되기 때문이다.

**예:**
```
완벽한 DB 코드 + 완벽한 transaction + 완벽한 concurrency control

  ↓ SSD controller failure 발생

write failure → fs corruption → DB crash
```

> **결론:** Physical Fault는 모든 소프트웨어 추상화 아래 존재하는 **현실 세계의 한계**다.

또한 물리적 결함은 **예측이 어렵다.** 다음과 같은 현실 물리 현상은 완벽 제어가 불가능하기 때문이다:

- aging
- heat
- voltage noise
- manufacturing defect
- cosmic radiation

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 대형 장애 상당수는 물리적 결함에서 **직접** 시작된다.

### 1) Disk Failure
```
SSD/HDD fault → I/O error → filesystem corruption → database unavailable → service outage
```

### 2) RAM Bit Flip
```
memory corruption → invalid computation → corrupted state → wrong transaction result → integrity violation
```

### 3) NIC Failure
```
network interface down → packet loss → service isolation → cluster partition → distributed system instability
```

### 4) Power Failure
```
power instability → abrupt shutdown → dirty state → recovery failure → data inconsistency
```

### 5) Cooling Failure
```
fan failure → temperature spike → CPU throttling → thermal shutdown → node crash
```

> **결론:** Physical Fault는 **소프트웨어 계층 전체를 아래에서 붕괴**시킬 수 있다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

물리적 결함의 핵심은:

> **"완전 예방"이 거의 불가능하다는 점**

이다. 그래서 현대 인프라는 **Fault Avoidance(결함 제거)** 보다 **Fault Tolerance(결함 감내)** 를 중심으로 설계된다.

### 핵심 메커니즘

| 메커니즘 | 예시 |
|----------|------|
| **Redundancy (다중화)** | dual PSU, RAID, replicated DB, multiple NIC |
| **Isolation (격리)** | faulty node quarantine, bad disk eviction, degraded mode |
| **Error Detection** | ECC memory, checksum, SMART monitoring |
| **Automatic Recovery** | failover, restart, replication recovery |
| **Health Monitoring** | temperature monitoring, disk health check, hardware telemetry |

> **핵심 개념:** Physical Fault는 제거 대상이라기보다, **흡수·격리·우회해야 하는 현실적 전제**다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

물리적 결함은 **운영체제 및 인프라 레벨**에서 관측된다.

### Linux

**디스크 오류**
```bash
dmesg
journalctl -k
smartctl
```
관찰: I/O error, bad sector, disk timeout

**메모리 오류**
```bash
dmesg
edac-util
```
관찰: ECC corrected error, memory controller issue

**CPU/하드웨어 오류**
```bash
mcelog
rasdaemon
```
관찰: machine check exception, hardware fault event

**온도 및 전력**
```bash
sensors
ipmitool
```
관찰: overheating, fan failure, PSU instability

**네트워크**
```bash
ethtool
ip -s link
```
관찰: dropped packets, CRC error, interface instability

---

### Kubernetes

| 증상 | 명령어 | 관찰 |
|------|--------|------|
| **Node NotReady** | `kubectl get nodes` | hardware instability, network isolation, disk pressure |
| **DiskPressure / MemoryPressure** | `kubectl describe node` | failing storage, unstable memory |
| **Pod Eviction** | — | node degradation, hardware resource instability |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*