# 물리 장치 (Physical Device)

> 정독: 0회

## 1. 이 기술이 무엇인가

물리 장치(Physical Device)는:

> 실제 하드웨어로 존재하는 입출력 장치

```
Physical Device = 실제 전기 회로, 컨트롤러, 버스 인터페이스, 레지스터를 가진 하드웨어 장치
```

**대표 예시:**
- SSD
- HDD
- NIC
- GPU
- 키보드 / 마우스
- USB 장치
- NVMe 컨트롤러
- SATA 컨트롤러

**물리 장치 vs 가상 장치:**

| 구분 | 설명 |
|---|---|
| **Physical Device** | 실제 하드웨어 |
| **Virtual Device** | 소프트웨어가 만들어낸 가상 장치 표현 |

---

## 2. 시스템 어디에서 등장하는가

물리 장치는 시스템 **최하단**에 위치합니다.

```
Application
  → System Call
    → VFS / Network Stack / I/O Subsystem
      → Device Driver
        → Physical Device
          → Electrical Signal / Storage Cell / Network Signal
```

**스토리지 경로:**
```
Filesystem
  → Block Layer
    → NVMe/SATA Driver
      → SSD/HDD Physical Device
```

**네트워크 경로:**
```
Socket API
  → TCP/IP Stack
    → NIC Driver
      → NIC Physical Device
        → Cable / Radio Signal
```

> **핵심:** 상위 계층의 추상화된 명령이 실제 물리 동작으로 바뀌는 최종 지점입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

물리 장치는 종류에 따라 영향 자원이 다릅니다.

| 장치 | 주요 영향 자원 |
|---|---|
| 스토리지 장치 (SSD/HDD) | Disk I/O |
| NIC | Network I/O |
| GPU | Compute + Memory Bandwidth |

**공통적으로 CPU와 Memory에도 영향을 줍니다:**
- interrupt 처리
- DMA buffer
- MMIO 접근
- driver queue 처리
- bus bandwidth 사용

---

## 4. 왜 중요한가

물리 장치는 **모든 추상화의 실제 실행 기반**입니다.

상위 계층에서는 파일, 소켓, 블록 장치, 스트림처럼 보이지만, 실제로는 최종적으로 물리 장치가 동작해야 합니다.

```
write()
  → page cache
    → block I/O
      → NVMe driver
        → SSD controller
          → NAND flash program operation
```

이 흐름의 최종 실행 주체가 물리 장치입니다.

**물리 장치 성능과 상태가 시스템 전체 성능을 제한합니다:**
- SSD latency
- NIC packet drop
- GPU memory error
- bus saturation
- thermal throttling
- firmware bug

→ 모두 상위 애플리케이션 장애로 보일 수 있습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

물리 장치 장애는 상위 계층에서 다양하게 나타납니다.

**대표 사례:**

| 물리 장치 장애 | 상위 계층 증상 |
|---|---|
| SSD failure | I/O error → filesystem read/write failure → application error |
| NIC failure | packet drop → TCP retransmission → service latency 증가 |
| GPU fault | workload crash → runtime error |
| USB/storage disconnect | device missing → mount failure |
| thermal throttling | latency spike → throughput drop |

**실무에서 중요한 점:**

> 상위 에러 메시지는 software 문제처럼 보여도, 원인은 physical device일 수 있습니다.

대표적인 물리 장치 관련 커널 메시지:
- `I/O error`
- `device timeout`
- `reset controller`
- `link down`
- `blk_update_request`
- `nvme timeout`
- `NETDEV WATCHDOG`

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

> **핵심:** Driver가 물리 장치의 레지스터와 큐를 제어한다

**전체 흐름:**
```
User Request
  → Kernel I/O Abstraction
    → Device Driver
      → MMIO / I/O Port
        → Device Register
          → Physical Operation
```

**스토리지 예시:**
```
write()
  → block I/O request
    → NVMe submission queue
      → doorbell register write
        → SSD controller
          → FTL mapping
            → NAND physical program
```

**네트워크 예시:**
```
send()
  → socket buffer
    → NIC driver
      → DMA descriptor
        → NIC transmit queue
          → electrical/optical/radio signal
```

**핵심 개념 정리:**

| 개념 | 설명 |
|---|---|
| **MMIO** | 장치 레지스터를 메모리 주소처럼 접근 |
| **DMA** | 장치가 CPU 대신 메모리와 직접 데이터 전송 |
| **Interrupt** | 장치가 작업 완료/오류를 CPU에 알림 |
| **Firmware** | 장치 내부 제어 소프트웨어 |
| **Queue** | 장치 명령 대기열 |

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 장치 목록
lspci
lsusb
lsblk
ip link

# 드라이버 연결 확인
lspci -k

# 커널 로그
dmesg
journalctl -k

# sysfs
ls /sys/class
ls /sys/block
ls /sys/bus/pci/devices

# 스토리지 상태
smartctl -a /dev/sda
nvme smart-log /dev/nvme0

# 네트워크 상태
ethtool eth0
ip -s link
```

### Kubernetes

노드 레벨에서 관측합니다:
- Node condition
- CSI volume attach 상태
- CNI/NIC 상태
- device plugin 상태
- kubelet log
- node dmesg

```bash
kubectl describe node
kubectl logs -n kube-system <csi-pod>
kubectl logs -n kube-system <cni-pod>
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*