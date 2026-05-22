# 추상화 인터페이스 (Abstraction Interface)

> 정독: 0회

## 1. 이 기술이 무엇인가

추상화 인터페이스(Abstraction Interface)는:

> **상위 계층이 하부 구현 세부사항을 몰라도 정해진 규칙만으로 시스템 자원을 사용할 수 있게 만드는 표준화된 접근 경계**

### 핵심 목적

- 상위 계층: **무엇을 할 것인가 (What)** 만 다룸
- 하위 계층: **어떻게 수행할 것인가 (How)** 를 담당

### 핵심 특징

- 구현 은닉
- 표준화
- 계층 분리
- 결합도 감소
- 하드웨어 독립성

### 대표 사례

| 인터페이스 | 하부 구현 |
|------------|-----------|
| `read()` | filesystem + block IO |
| `socket()` | TCP/IP stack |
| `malloc()` | virtual memory allocation |
| SQL | storage engine |
| Kubernetes API | distributed orchestration |

> 인터페이스는 **실제 기능 자체가 아니라 기능 접근 규약**입니다.

---

## 2. 시스템 어디에서 등장하는가

추상화 인터페이스는 시스템 전체에 존재합니다.

### 운영체제

| 인터페이스 | 의미 |
|------------|------|
| System Call | user ↔ kernel 경계 |
| POSIX API | 표준 OS 인터페이스 |
| VFS | 통합 파일시스템 인터페이스 |

### 스토리지

애플리케이션은 `open()` / `read()` / `write()` 만 호출합니다.

```
filesystem lookup
  → block mapping
  → SSD controller
  → NAND operation
```

### 네트워크

애플리케이션은 `send()` / `recv()` 만 호출합니다.

```
TCP buffering
  → routing
  → NIC queue
  → DMA
  → physical transmission
```

### Kubernetes

| Kubernetes Interface | 실제 구현 |
|----------------------|-----------|
| Pod API | Linux isolation |
| Service API | iptables/eBPF |
| Volume API | storage backend |

### Cloud

Virtual Machine API 뒤에는 hypervisor, CPU scheduling, storage virtualization이 존재합니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

추상화 인터페이스는 **모든 시스템 자원 접근 경로에 영향**을 줍니다.

### CPU
- syscall overhead
- context switch
- scheduling transition

### Memory
- page cache abstraction
- virtual memory mapping
- allocation metadata

### Network
- socket buffering
- protocol stack traversal
- packet queue handling

### Disk — 영향 매우 큼
- filesystem abstraction이 핵심

> 인터페이스는 편리성을 제공하지만, **추가 변환 계층과 오버헤드도 생성**합니다.

---

## 4. 왜 중요한가

추상화 인터페이스는 **현대 시스템의 호환성과 확장성을 가능하게 만드는 핵심 구조**입니다.

### 이것이 없으면

애플리케이션이 직접 SSD controller, DMA engine, page table, NIC register를 제어해야 하며, 결과적으로 유지보수 불가능, 하드웨어 종속, 플랫폼 파편화, 장애 위험 증가가 발생합니다.

### 핵심 가치
- 구현 독립성
- 하드웨어 독립성
- 표준화
- 계층 분리
- 교체 가능성
- 확장 가능성

### 핵심 개념: Contract vs Implementation

인터페이스는 **행동 계약만 정의**하고, 실제 구현은 자유롭게 변경 가능합니다.

**예시:** 동일한 `write()` 인터페이스 뒤에 HDD, SATA SSD, NVMe SSD, distributed storage 모두 연결 가능합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

> **인터페이스는 실제 병목을 숨길 수 있습니다.**

### Disk Slowdown

애플리케이션은 `write()` 만 호출하지만, 내부에서는 다음이 발생할 수 있습니다:

```
journal flush
  → block queue congestion
  → SSD GC stall
```

### Network Delay

논리: `socket send`

```
NIC queue overflow
  → packet retransmission
  → interrupt saturation
```

### Kubernetes 사례

| 논리 인터페이스 | 실제 문제 |
|-----------------|-----------|
| Service | iptables latency |
| PVC | backend storage stall |
| Ingress | conntrack saturation |
| Pod | cgroup throttling |

> SRE 관점에서는 **인터페이스 아래 실제 구현을 추적하는 능력**이 중요합니다.

---

## 6. 핵심 메커니즘

**표준 인터페이스를 통해 하위 복잡성을 숨긴 채 기능을 제공**하는 구조입니다.

### 예시 1: 파일 인터페이스

```
read(fd)
  → VFS
  → inode lookup
  → page cache
  → block IO
  → SSD access
```

### 예시 2: 메모리 인터페이스

```
malloc()
  → virtual memory allocation
  → page mapping
  → physical frame assignment
```

### 예시 3: 네트워크 인터페이스

```
socket.send()
  → TCP segmentation
  → routing
  → NIC queue
  → DMA transmission
```

### 핵심 구조
- 상위는 단순 인터페이스 사용
- 하위는 실제 물리 구현 수행

---

## 7. Linux / Runtime / K8s에서 관측 방법

### Linux

```bash
# system call interface
strace

# filesystem abstraction
mount
cat /proc/filesystems

# virtual device abstraction
lsblk

# network abstraction
ss -tulnp
ip addr
```

### Runtime 관측 대상
- syscall latency
- context switch
- page cache hit ratio
- IO wait
- socket buffer usage

### Kubernetes

| Interface | 실제 구현 |
|-----------|-----------|
| Pod Spec | Linux isolation |
| Service | traffic redirection |
| PVC | storage backend |

```bash
# 상위 관측
kubectl describe
kubectl logs
kubectl top

# 낮은 계층 추적
perf
bcc-tools
bpftrace
```

> 인터페이스만 보면 단순하지만, **실제 구현은 매우 복잡할 수 있습니다.**

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*