# 논리 인터페이스 (Logical Interface)

> 정독: 0회

## 1. 이 기술이 무엇인가

**논리 인터페이스(Logical Interface)** 는:

> 하부 하드웨어의 실제 물리 구조를 숨기고, 상위 계층에 일관된 제어 방식과 데이터 접근 규칙을 제공하는 추상화된 소프트웨어 인터페이스

### 핵심 목적

상위 시스템이 하드웨어의 실제 구현 방식을 몰라도 **동일한 방식으로 자원을 제어**할 수 있게 만드는 것입니다.

### 중요한 특징

논리 인터페이스는 물리 장치 자체가 아니라, 커널/런타임이 제공하는 논리 계층으로 표준화된 접근 규칙과 추상화된 제어 모델을 제공합니다.

### 대표 예시

| 영역 | 논리 인터페이스 |
|---|---|
| 스토리지 | LBA (Logical Block Address) |
| 파일 시스템 | VFS (Virtual File System) |
| 네트워크 | Socket Interface |
| 프로세스 | PID |
| 메모리 | Virtual Memory |
| Kubernetes | API Object |

> 논리 인터페이스는 물리 장치의 차이를 숨기고 **논리적으로 동일한 사용 경험**을 제공하는 데 목적이 있습니다.

<details>
<summary>Deep Dive</summary></br>

Physical Implementation(물리적 구현) [[M]](../../100-deep-dive/micro-foundations/physical-implementation.md)  
Physical Architecture(물리적 아키텍처) [[M]](../../100-deep-dive/micro-foundations/physical-architecture.md)  
Operating System Kernel Layer(운영체제 커널 계층) [[M]](../../100-deep-dive/micro-foundations/operating-system-kernel-layer.md)  
I/O Control Protocol(입출력 제어 프로토콜) [[M]](../../100-deep-dive/micro-foundations/io-control-protocol.md)  
Logical Block Addressing(논리 블록 주소 지정) [[M]](../../100-deep-dive/micro-foundations/logical-block-addressing.md)  
Device Driver(디바이스 드라이버) [[M]](../../100-deep-dive/micro-foundations/device-driver.md)  
Interface(인터페이스) [[M]](../../100-deep-dive/micro-foundations/interface.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

논리 인터페이스는 시스템 전체에 존재합니다.

### 스토리지

실제 SSD 내부에는 NAND page, erase block, FTL mapping, wear leveling 같은 복잡한 물리 구조가 존재하지만, 상위에는 **연속적인 블록 주소 공간(LBA)** 만 제공합니다.

### 파일 시스템

실제 디스크 내부는 비연속 블록일 수 있지만, 사용자에게는 **하나의 연속 파일**처럼 보입니다.

### 네트워크

실제 NIC 내부에는 DMA, descriptor ring, interrupt, queue 등이 존재하지만, 상위 애플리케이션은 `socket()`, `send()`, `recv()` 만 사용합니다.

### 메모리

실제 물리 RAM은 불연속 physical frame이지만, 상위 프로세스는 **연속 virtual address**로 인식합니다.

### Kubernetes

실제 클러스터에는 node, container, network namespace, cgroup 등 복잡한 구조가 존재하지만, 사용자는 Deployment, Service, PersistentVolume 같은 **논리 객체**만 사용합니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

논리 인터페이스는 모든 시스템 자원 계층에 영향을 줍니다.

- **Disk / Storage**: 물리 저장 구조와 논리 접근 구조의 차이가 매우 커서 가장 대표적인 영역
- **CPU**: address translation, syscall handling, abstraction layer dispatch 수행
- **Memory**: page table, inode cache, buffer cache, metadata cache 등에서 사용
- **Network**: socket abstraction, virtual interface, overlay network 등에서 사용

> 핵심: 논리 인터페이스는 복잡성을 줄이는 대신 **추상화 비용과 관리 비용**이 발생합니다.

---

## 4. 왜 중요한가

현대 시스템은 논리 인터페이스 없이는 운영이 불가능합니다.

주요 이유는 하드웨어 독립성, 이식성, 운영 단순화, 데이터 독립성, 드라이버 표준화, 분산 시스템 통합입니다.

**핵심 효과**: 하드웨어 다양성을 소프트웨어 일관성으로 통합합니다.

예를 들어 애플리케이션은 NVMe SSD인지, SATA SSD인지, Cloud Storage인지 알 필요가 없습니다.

### Contract Stability

하부 구현이 바뀌어도 **상위 인터페이스 계약은 유지**되어야 합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 운영 장애 상당수가 **논리 인터페이스 계층 불일치**에서 발생합니다.

### 대표 사례

**Device Mapping 오류**
- `/dev/sda`, `/dev/nvme0n1` 매핑 변화로 잘못된 mount, 데이터 손상, 부팅 실패 발생

**Filesystem Interface 문제**
- VFS와 실제 FS 드라이버 간 semantic mismatch 발생 시 corruption, stale metadata, inode inconsistency 가능

**Network Interface 문제**
- 논리 NIC(eth0, cni0, veth)와 실제 물리 NIC 상태 불일치

**Kubernetes 사례**
- 논리 Service는 정상인데 CNI 문제, overlay routing 문제, iptables drift 발생 가능

### SRE 핵심

SRE는 **논리 상태와 물리 상태의 불일치**를 매우 중요하게 분석합니다.

> 상위에서는 volume이 mounted로 보이는데 실제 SSD에서 timeout이 발생하는 경우가 대표적입니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심 메커니즘은 **하부 물리 구조를 상위에서 단순하고 안정적인 모델로 변환**하는 것입니다.

### 기본 흐름

```
Physical Resource
  → Driver
    → Kernel Abstraction
      → Logical Interface
        → User/Application
```

### 스토리지 예시

```
실제 물리 구조: NAND page, erase block, channel, die, plane
  → SSD Controller (FTL: physical page ↔ logical block 변환)
    → 커널 블록 디바이스: /dev/nvme0n1
      → 파일 시스템 논리 파일: /data/report.txt
```

상위는 물리 구현을 몰라도 동작합니다.

### Virtualization

논리 인터페이스 대부분은 virtual abstraction, indirection layer, mapping table 기반입니다.

| 논리 인터페이스 | 실제 물리 대상 |
|---|---|
| Virtual Memory | Physical RAM |
| File | Physical Block |
| Socket | NIC Queue |
| Container FS | Overlay Layers |

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 블록 디바이스
lsblk
blkid
fdisk -l

# 가상 파일 시스템
mount
cat /proc/filesystems

# Virtual Memory
cat /proc/meminfo
pmap

# Network Interface
ip link
ip addr
ethtool
```

### Runtime

관측 대상: file descriptor, virtual address space, socket abstraction, runtime IO layer

### Kubernetes

| Object | 역할 |
|---|---|
| Service | logical network endpoint |
| PVC | logical persistent storage |
| Ingress | logical routing interface |

실제 내부에는 overlay network, iptables, virtual ethernet, storage driver 등이 숨겨져 있습니다.

### 장애 분석 시 핵심

> **논리 인터페이스 정상 여부**와 **실제 물리 상태**를 반드시 분리해서 봐야 합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*