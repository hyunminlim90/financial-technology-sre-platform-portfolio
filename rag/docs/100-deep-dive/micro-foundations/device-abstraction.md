# 장치 추상화 (Device Abstraction)

> 정독: 0회

## 1. 이 기술이 무엇인가

장치 추상화(Device Abstraction)는:

> 서로 완전히 다른 하드웨어 장치를 운영체제가 일관된 방식으로 제어할 수 있도록 표준 인터페이스 뒤로 숨기는 구조

**핵심 목적:**

- 하드웨어 복잡성 은폐
- 제조사별 차이 제거
- 공통 API 제공
- 커널 구조 표준화
- 하드웨어 독립성 확보

상위 계층은 SSD 내부 구조, HDD 회전 방식, NIC 전기 신호, USB 컨트롤러 차이를 몰라도 됩니다.

대신 `read` / `write` / `open` / `ioctl` 같은 **공통 인터페이스만 사용**합니다.

> **핵심:** 장치 추상화는 하드웨어를 표준화된 소프트웨어 객체로 변환하는 것입니다.

---

## 2. 시스템 어디에서 등장하는가

### 장치 유형별 등장 위치

| 장치 유형 | 대표 예시 |
|---|---|
| Storage | SSD, HDD, NVMe, SATA, RAID |
| Network | Ethernet NIC, Wi-Fi, Virtual NIC |
| Input Device | keyboard, mouse, touch |
| GPU | framebuffer, DRM, rendering device |

### Linux Kernel

대표 계층:

- VFS
- Block Layer
- Network Stack
- TTY Layer
- Device Model
- Device Driver 계층 — 장치 추상화 핵심 구현체

### Kubernetes

K8s도 내부적으로 장치 추상화를 활용합니다:

- CSI (Container Storage Interface)
- CNI (Container Network Interface)
- GPU Device Plugin

### Virtualization

- virtio
- vhost
- SR-IOV abstraction

> **핵심:** 현대 운영체제 대부분은 장치 추상화 기반 구조입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

장치 종류에 따라 달라지지만, 핵심 영향은 **I/O 전체**입니다.

### Disk 영향 — 매우 큼

- block I/O
- queue scheduling
- page cache
- DMA

### Network 영향 — NIC abstraction 매우 중요

- packet queue
- interrupt moderation
- offloading

### CPU 영향 — 드라이버 처리 비용 존재

- interrupt handling
- syscall transition
- polling
- queue processing

### Memory 영향 — 중요

- DMA buffer
- ring buffer
- kernel buffer
- page cache

> **핵심:** 장치 추상화는 모든 I/O 자원 흐름의 공통 제어 계층입니다.

---

## 4. 왜 중요한가

### 하드웨어 독립성 확보

상위 계층은 장치 종류를 몰라도 됩니다.

### 커널 구조 단순화

커널 core가 삼성 SSD / 인텔 SSD / USB storage를 별도로 처리할 필요가 없습니다.

### 확장성 확보

새 장치 추가 시 **driver만 추가**하면 됩니다.

### 이식성 확보

같은 애플리케이션이 노트북 / 서버 / VM / 클라우드에서 동일하게 동작 가능합니다.

### 가상화 기반 제공

현대 클라우드의 핵심입니다:

- virtual block device
- virtual network device

### Kubernetes 핵심 기반

CSI / CNI 전부 abstraction 기반입니다.

> **핵심:** 장치 추상화는 운영체제가 하드웨어 다양성을 통제하기 위한 핵심 구조입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 대표 장애 유형

| 장애 | 설명 |
|---|---|
| Driver Failure | kernel panic / DMA corruption / interrupt storm |
| Device Timeout | NVMe timeout |
| Queue Saturation | block queue overflow |
| IRQ 문제 | interrupt imbalance 발생 |
| Device Compatibility Failure | 펌웨어/드라이버 mismatch |

### Kubernetes 장애

| 장애 | 영향 |
|---|---|
| CSI Failure | volume mount 실패 |
| CNI Failure | pod network 단절 |
| GPU Plugin Failure | device allocation 실패 |
| Virtualization 장애 | virtio mismatch |

### Storage 장애

- Block Device Hang — I/O stuck 상태
- Filesystem Freeze — 하부 device timeout 전파

### SRE 관점 핵심

> 많은 장애가 상위 application 문제가 아니라 **장치 추상화 계층 문제**입니다.

---

## 6. 핵심 메커니즘

**공통 인터페이스 + 드라이버 바인딩**이 핵심입니다.

### 전체 흐름

```
1단계  상위 계층        read() / write() 호출
2단계  커널 추상화      VFS / block layer / network stack 진입
3단계  커널             장치 종류 확인
4단계  커널             해당 device driver 함수 호출
5단계  Device Driver    실제 hardware command 생성
                         (NVMe queue command / SATA command / NIC DMA descriptor)
6단계  Device Controller 실행
```

### 핵심 구조

> 상위 계층은 공통 인터페이스만 알고, **하부 driver가 실제 물리 장치를 처리**합니다.

### Device File과의 관계

Linux에서 `/dev/*` 는 장치 추상화의 결과물입니다:

```
/dev/sda
/dev/nvme0n1
```

### Driver와의 관계

장치 추상화의 실제 실행 주체는 **device driver**입니다.

### Stream/Block abstraction

장치 추상화는 **block abstraction** 과 **stream abstraction** 둘 다 제공 가능합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 장치 목록
lsblk

# 장치 파일
ls /dev

# 드라이버 확인
lspci -k

# kernel device log
dmesg

# block layer 확인
cat /proc/devices

# sysfs device model
ls /sys/block
ls /sys/class
```

### Runtime 관측 포인트

- file descriptor
- async I/O
- epoll
- DMA buffer

### Kubernetes

```bash
# CSI 상태
kubectl get csinode

# device plugin
kubectl describe node

# container runtime device mapping
crictl inspect
```

> **핵심:** Linux/Kubernetes 대부분의 I/O는 장치 추상화 계층 위에서 동작합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*