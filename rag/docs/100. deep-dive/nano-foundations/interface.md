# 인터페이스 (Interface)

> 정독: 0회

## 1. 이 기술이 무엇인가

**인터페이스(Interface)** 는:

> 서로 다른 시스템이나 계층이 정해진 규칙으로 상호작용하기 위한 접점과 계약 규격

인터페이스는 입력 형식, 출력 형식, 호출 규칙, 데이터 구조, 오류 처리 방식을 정의하며, **내부 구현은 숨기고 상호작용 규칙만 노출**합니다.

### 인터페이스의 본질

인터페이스는 **무엇을 제공하는가(What)** 를 정의하고, 구현(Implementation)은 **어떻게 동작하는가(How)** 를 담당합니다.

---

## 2. 시스템 어디에서 등장하는가

인터페이스는 컴퓨터 시스템 전체에 존재합니다.

### 주요 계층

**사용자 인터페이스** (인간 ↔ 시스템)
- 예: CLI, GUI, Shell, Terminal

**시스템 콜 인터페이스** (User Space ↔ Kernel Space)
- 예: `open()`, `read()`, `write()`

**파일시스템 인터페이스** (VFS ↔ Filesystem)
- 예: ext4, XFS, ZFS

**드라이버 인터페이스** (Kernel ↔ Device Driver)
- 예: block operations, network operations

**하드웨어 인터페이스** (Driver ↔ Hardware)
- 예: PCIe, NVMe, SATA, USB

**네트워크 인터페이스** (Host ↔ Network)
- 예: Ethernet, TCP/IP, RDMA

### Kubernetes 환경

인터페이스 계층이 매우 많습니다.

- Container Runtime Interface (CRI)
- Container Network Interface (CNI)
- Container Storage Interface (CSI)

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

인터페이스 자체는 특정 자원 하나만 다루지 않지만, **자원 접근 규칙과 경계**에 매우 큰 영향을 줍니다.

- **CPU**: syscall transition, interrupt handling, context switch
- **Memory**: buffer structure, shared memory, DMA mapping
- **Disk**: filesystem API, block interface, storage queue
- **Network**: socket interface, packet buffer, NIC queue

> 핵심: 인터페이스는 자원 자체보다 **자원 접근 방식과 흐름을 정의**합니다.

---

## 4. 왜 중요한가

인터페이스는 **시스템 확장성과 독립성을 가능하게 하는 핵심 구조**입니다.

**구현 교체 가능**: 상위 인터페이스가 유지되면 하부 구현(HDD → SSD, SATA → NVMe, ext4 → XFS)을 변경해도 애플리케이션 수정이 불필요합니다.

**느슨한 결합**: 각 계층이 내부 구현을 몰라도 됩니다.

**표준화**: 다양한 제조사와 시스템이 동일 규격을 사용할 수 있습니다.

**안정성**: 권한 경계와 접근 제어가 가능합니다.

> 인터페이스가 있어야 **복잡한 시스템을 계층적으로 분리**할 수 있습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실무 장애 상당수는 인터페이스 경계 문제입니다.

### 대표 장애 유형

**API Contract Mismatch** — 요청/응답 형식 불일치

**Kernel/User ABI 문제** — 커널 버전 변경 후 binary incompatibility

**Driver Interface Mismatch** — firmware와 driver 버전 충돌

**Storage Interface Timeout** — NVMe queue timeout

**Network Interface Saturation** — NIC queue overflow

**Kubernetes CNI/CSI Failure** — plugin interface 불일치

**Syscall Bottleneck** — 과도한 syscall transition

### 실제 SRE 관점

서비스 응답 지연이라는 증상의 실제 원인이 **syscall/interface boundary overhead**인 경우도 존재합니다.

> **시스템 장애는 인터페이스 경계에서 자주 발생**합니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

인터페이스 핵심 메커니즘은 **표준화된 호출 규칙**입니다.

### 기본 구조

상위 계층은 인터페이스 규칙만 사용하고, 하위 계층은 실제 구현을 수행합니다.

### 예시 흐름

```
Application
  → read()
    → System Call Interface
      → VFS Interface
        → Block Interface
          → Device Driver Interface
            → Hardware Interface
              → Device
```

각 경계마다 호출 규약, 데이터 형식, 상태 코드, 동기화 규칙이 존재합니다.

### 핵심 특징

**구현 은닉** — 상위는 내부 구조를 알 필요 없음

**계약 기반 동작** — 인터페이스만 맞으면 구현 교체 가능

**계층 분리** — 각 계층 독립 개발 가능

> 인터페이스는 **시스템 간 상호작용 규칙 자체**입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 시스템 콜 인터페이스
strace

# 네트워크 인터페이스
ip link
ifconfig
ethtool

# 블록 인터페이스
lsblk
blkid

# 드라이버 인터페이스
sysfs
/proc
/sys

# Device Interface
lspci
lsusb
```

### Runtime 관측 포인트

- syscall latency
- API response
- queue interface
- IPC interface
- socket interface

### Kubernetes

| 인터페이스 | 역할 |
|---|---|
| CRI | container runtime interface |
| CNI | network interface layer |
| CSI | storage interface layer |
| API Server | cluster-wide control interface |

### 핵심 관측 포인트

> Kubernetes는 **인터페이스 기반 시스템들의 집합**입니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*