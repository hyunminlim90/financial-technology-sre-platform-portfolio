# 정보 은닉 (Information Hiding)

> 정독: 0회

## 1. 이 기술이 무엇인가

정보 은닉(Information Hiding)은:

> 모듈 내부 구현 세부사항을 외부에 노출하지 않고, **외부에는 최소한의 인터페이스만 공개하는 설계 원칙**

**핵심 목적:**
- 복잡성 격리
- 변경 영향 최소화
- 계층 독립성 유지

**핵심 구조:**

| 공개 | 은닉 |
|---|---|
| "무엇을 할 수 있는가" (인터페이스) | 내부 알고리즘, 자료구조, 하드웨어 제어 방식, 메모리 구조, 최적화 전략 |

**운영체제 관점 예시:**

```c
read(fd, buf, size)
```

상위 프로세스는 아래를 전혀 몰라도 됩니다:
- NVMe queue 구조
- DMA 동작
- interrupt 처리
- page cache 내부 구현

→ 이 모든 복잡성은 **Kernel + Driver 내부에 은닉**됩니다.

---

## 2. 시스템 어디에서 등장하는가

사실상 현대 시스템 전체에 존재합니다.

**운영체제:**

| 위치 | 은닉 내용 |
|---|---|
| **VFS** | 파일시스템 구현 차이 (ext4 / xfs / btrfs 전부 동일 file API) |
| **Device Driver** | 하드웨어 제어 세부 구현 |
| **Socket Layer** | TCP/IP 내부 처리 |
| **Virtual Memory** | 물리 메모리 구조 |

**Kubernetes:**

| 인터페이스 | 은닉 내용 |
|---|---|
| **CRI** | container runtime 구현 차이 |
| **CSI** | storage vendor 차이 |
| **CNI** | network provider 차이 |

**Cloud / Infra:** hypervisor, storage backend, network fabric 모두 abstraction 뒤에 숨겨집니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

정보 은닉 자체는 특정 자원보다 **시스템 구조 전체**에 영향을 미치며, 모든 I/O 경로 설계와 간접적으로 연결됩니다.

| 자원 | 은닉 대상 |
|---|---|
| **CPU** | syscall abstraction, scheduler abstraction, interrupt handling |
| **Memory** | page cache, virtual memory, slab allocator 내부 구현 |
| **Disk** | filesystem internals, block scheduling, device queue |
| **Network** | packet segmentation, retransmission, congestion control |

> **핵심:** 정보 은닉은 리소스 사용 자체보다 **복잡성 통제 구조**에 더 큰 영향을 줍니다.

---

## 4. 왜 중요한가

현대 시스템 유지보수의 핵심 원칙입니다.

**변경 영향 최소화**
하위 구현 교체 시 상위 애플리케이션 수정을 최소화할 수 있습니다:
- HDD → NVMe
- iptables → eBPF
- Docker → containerd

**복잡성 격리**
하위 세부 구현이 application, middleware, user code로 전파되지 않습니다.

**안정성 증가**
계층 간 coupling이 감소합니다.

**유지보수성 향상**
문제 범위를 특정 계층 내부로 제한할 수 있습니다.

**확장성 증가**
새로운 장치나 구현을 추가할 수 있습니다.

> **핵심:** 정보 은닉은 시스템 변경 비용과 결합도를 줄이는 핵심 구조입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

> **중요:** 상위는 정상처럼 보여도 하위 구현 내부에서 심각한 문제가 발생할 수 있습니다.

**`write()` hang**

상위에서는 `write(fd)` 호출만 보이지만, 실제 내부 원인:
- dirty page congestion
- filesystem journal stall
- SSD firmware timeout

**socket timeout**

상위에서는 `recv()` timeout으로 보이지만, 실제 내부 원인:
- packet drop
- TCP retransmission
- NIC queue overflow

**filesystem corruption**

상위 API는 동일하지만, 실제 내부 원인:
- metadata journal corruption
- block remap failure
- controller reset

**핵심 실무 포인트:**

```
추상화가 장애를 제거하는 것은 아니다.
복잡성을 숨기는 것이다.
```

→ SRE/Kernel/Infra 관점에서는 **추상화 아래 계층까지 내려가 추적할 수 있어야** 합니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

> **핵심:** "인터페이스는 공개, 구현은 숨김"

**Linux 핵심 예시**

VFS는 공통 인터페이스(`read()` / `write()` / `open()`)를 제공하고, 실제 구현은 `struct file_operations` 포인터를 통해 동적 연결됩니다.

상위 프로세스는 대상이 ext4인지, xfs인지, NVMe인지, RAM disk인지 알 수 없습니다.

**Driver Layer**

상위는 block device만 인식하고, 실제 내부(PCIe MMIO, DMA queue, interrupt, firmware command)는 driver 내부에 은닉됩니다.

**Network Stack**

socket API 뒤에 routing, segmentation, retransmission, congestion control이 모두 숨겨집니다.

> **핵심:** 정보 은닉은 "계층 간 계약(interface)만 유지하고, 구현 세부사항은 내부로 봉인하는 구조"입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# syscall 확인 (abstraction 관측)
strace

# device abstraction
ls /dev

# mounted filesystem
mount
cat /proc/mounts

# block layer
lsblk
iostat
blktrace

# driver 확인
lspci -k

# kernel object
ls /sys
ls /proc
```

### Runtime

container runtime도 abstraction 기반으로 내부 구현을 은닉합니다:
- overlayfs
- namespaces
- cgroups

### Kubernetes

**상위 인터페이스:**

```bash
kubectl describe   # 상위 인터페이스 제공
kubectl logs       # 상위 인터페이스 제공
```

실제 내부(runtime, network, storage)는 CRI / CSI / CNI abstraction 아래 숨겨집니다.

**추상화 아래 계층 추적 (eBPF / tracing):**

```bash
perf
bcc
bpftrace
ftrace
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*