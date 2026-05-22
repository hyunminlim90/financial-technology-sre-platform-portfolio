# 데이터 추상화 (Data Abstraction)

> 정독: 0회

## 1. 이 기술이 무엇인가

데이터 추상화(Data Abstraction)는:

> **복잡한 내부 구현을 숨기고 상위 계층에는 필요한 인터페이스와 의미만 제공하는 설계 원칙**

### 핵심 목적

상위 시스템이 다음과 같은 내부 구현 세부사항을 몰라도 동작할 수 있게 만드는 것입니다.

- 물리 저장 방식
- 메모리 구조
- 네트워크 전송 구조
- 하드웨어 제어 방식

### 핵심 특징

추상화는 **What 제공 / How 은닉** 구조입니다.

| 사용자 관점 | 실제 내부 |
|-------------|-----------|
| 파일 읽기 | 블록 I/O + page cache + SSD |
| socket send | TCP/IP stack + NIC DMA |
| virtual memory | page table + MMU |
| container | namespace + cgroup |

> 추상화는 단순 편의 기능이 아닌 **복잡도 제어 메커니즘**입니다.

---

## 2. 시스템 어디에서 등장하는가

데이터 추상화는 현대 컴퓨터 시스템 전체에 존재합니다.

### 대표 계층

| 계층 | 추상화 대상 |
|------|-------------|
| Application | logical object |
| File System | logical file |
| Virtual Memory | virtual address |
| Network | socket |
| Container | isolated process view |
| Cloud | virtual infrastructure |

### 스토리지 계층

사용자는 `document.txt`만 인식하지만, 실제 내부는 모두 숨겨집니다.

```
Logical File
     ↓
 File System      (inode, extent)
     ↓
 Block Device     (LBA)
     ↓
SSD Controller    (FTL)
     ↓
  NAND Cell       (NAND block)
```

### 네트워크 계층

애플리케이션은 `send()` / `recv()`만 사용하지만, 내부는 다음 계층을 거칩니다.

```
Socket API
     ↓
TCP/IP Stack
     ↓
 NIC Driver
     ↓
Physical Network
```

### Kubernetes 추상화

| 사용자 개념 | 실제 내부 |
|-------------|-----------|
| Pod | namespace + cgroup + veth |
| Service | iptables/ipvs/eBPF |
| PersistentVolume | distributed storage backend |

---

## 3. 어떤 자원에 가장 영향이 큰가

데이터 추상화는 **모든 자원 계층에 영향**을 줍니다.

| 자원 | 영향 | 설명 |
|------|------|------|
| CPU | 오버헤드 증가 가능 | 추상화 계층이 많을수록 syscall, context switch, translation, scheduling 비용 증가 |
| Memory | 매우 큼 | page cache, buffer cache, metadata cache, translation table |
| Network | 중요 | socket abstraction 뒤에서 TCP buffering, retransmission, congestion control 실행 |
| Disk | 매우 큼 | Logical Address → Physical Address Translation이 핵심 |

> 추상화는 편리성을 제공하지만 **추가적인 변환 비용**도 발생시킵니다.

---

## 4. 왜 중요한가

데이터 추상화는 **현대 시스템을 인간이 관리 가능하게 만드는 핵심 원리**입니다.

### 이것이 없으면

애플리케이션이 직접 NAND address, DMA descriptor, page table, interrupt, physical sector를 다뤄야 하며, 사실상 시스템 개발이 불가능해집니다.

### 핵심 가치

- 복잡도 감소
- 유지보수성 향상
- 계층 독립성
- 이식성
- 확장성
- 장애 격리

### 데이터 독립성 (Data Independence)

하부 구현 변경 시 상위 애플리케이션 영향을 최소화합니다.

| 변경 사례 | 결과 |
|-----------|------|
| HDD → SSD 교체 | 상위 application 수정 없이 유지 가능 |
| ext4 → XFS 변경 | 상위 application 수정 없이 유지 가능 |
| physical server → VM 이전 | 상위 application 수정 없이 유지 가능 |

---

## 5. 실제 장애와 어떤 관련이 있는가

추상화는 시스템 안정성을 높이지만, **추상화 아래 실제 물리 병목을 숨길 수도 있습니다.**

### 대표 장애 유형

| 유형 | 설명 |
|------|------|
| Hidden Disk Latency | 단순 file write처럼 보이지만 내부에서 SSD GC, NAND erase stall 발생 가능 |
| Page Cache Illusion | 메모리는 충분해 보이지만 실제는 page cache pressure |
| Network Virtualization Overhead | container networking 추상화 아래 veth, bridge, iptables 병목 발생 가능 |
| Storage Mapping Failure | 논리 주소는 정상처럼 보이지만 FTL corruption 발생 가능 |

### Kubernetes 환경에서 특히 중요한 사례

| 추상화 | 실제 장애 |
|--------|-----------|
| Service | iptables explosion |
| PVC | storage backend latency |
| Pod | node resource starvation |
| Overlay Network | MTU fragmentation |

> SRE 관점에서는 **추상화 뒤의 실제 물리 계층까지 추적 가능해야 함**이 매우 중요합니다.

---

## 6. 핵심 메커니즘

핵심은 **상위 계층은 하위 구현을 직접 알지 못한다**는 것입니다.

### 예시 1: 파일 추상화

```
open("data.txt")
       ↓
      VFS
       ↓
 inode lookup
       ↓
extent traversal
       ↓
 block mapping
       ↓
    SSD I/O
       ↓
   NAND read
```

### 예시 2: 메모리 추상화

프로세스는 연속된 메모리 공간처럼 보지만, 실제는 다음 구조입니다.

```
virtual page
     ↓
 page table
     ↓
physical frame
```

### 예시 3: 네트워크 추상화

```
socket.send()
       ↓
TCP segmentation
       ↓
    routing
       ↓
   NIC queue
       ↓
      DMA
       ↓
physical transmission
```

### 핵심 메커니즘 요약

추상화 계층은 **표준 인터페이스를 유지한 채 내부 구현을 변경 가능하게** 만드는 것입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux 명령어

```bash
# 파일 시스템 추상화
mount
df -h
lsblk

# VFS 계층
cat /proc/filesystems

# 가상 메모리 추상화
cat /proc/meminfo

# block abstraction
iostat
blktrace

# device abstraction
udevadm
```

### Runtime 관측 포인트

- syscall latency
- page cache hit ratio
- block I/O latency
- virtual memory usage
- filesystem metadata overhead

### Kubernetes 추상화 계층

| Kubernetes Object | 실제 구현 |
|-------------------|-----------|
| Pod | process isolation |
| Service | traffic redirection |
| Volume | storage backend |
| ConfigMap | mounted filesystem object |

### 관측 도구

```bash
# 상위 계층
kubectl describe
kubectl top

# 낮은 계층 추적
strace
perf
eBPF
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*