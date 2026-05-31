# 사용자 계층 (User Level / User Space)

> 정독: 0회

## 1. 이 기술이 무엇인가

사용자 계층(User Space)은:

> **일반 애플리케이션이 실행되는 제한 권한 실행 영역**

운영체제는 시스템을 크게 **User Space**와 **Kernel Space**로 분리합니다.

### 핵심 특징

User Space 프로세스는 다음이 불가능합니다.

- CPU 직접 제어
- 물리 메모리 직접 접근
- 디스크 직접 제어
- NIC 직접 제어

대신 반드시 **System Call**을 통해 커널에게 요청해야 합니다.

### 대표 프로그램

모두 User Space에서 동작합니다.

- 웹 브라우저
- 데이터베이스
- AI inference process
- shell
- container runtime client
- API server
- monitoring agent

> 핵심 목적: **시스템 안정성과 격리**

---

## 2. 시스템 어디에서 등장하는가

User Space는 거의 모든 상위 소프트웨어의 실행 위치입니다.

### 시스템 계층 구조

```
Application
     ↓
 User Space
     ↓
System Call Boundary
     ↓
 Kernel Space
     ↓
  Hardware
```

### 주요 위치

| 계층 | 역할 |
|------|------|
| User Space | application execution |
| Kernel Space | resource control |
| Hardware | physical execution |

### 대표 상호작용

| 작업 | 실제 흐름 |
|------|-----------|
| 파일 읽기 | `read()` syscall |
| 네트워크 송신 | `send()` syscall |
| 메모리 요청 | `mmap()`/`brk()` |
| 프로세스 생성 | `fork()`/`exec()` |

### Kubernetes 환경에서 매우 중요한 항목

- container process
- kubelet interaction
- userspace networking
- service proxy
- observability agent

---

## 3. 어떤 자원에 가장 영향이 큰가

User Space는 사실상 **모든 시스템 자원의 소비 시작점**입니다.

| 자원 | 영향 | 대표 사례 |
|------|------|-----------|
| CPU | 매우 큼 | application execution, serialization, encryption, compression |
| Memory | 매우 큼 | heap, stack, mmap, page cache interaction |
| Network | 중요 | socket API → kernel TCP/IP stack |
| Disk | 중요 | application write → syscall → filesystem → block I/O |

### 특히 중요한 비용 — User Space ↔ Kernel Space 전환

이를 **syscall overhead** 또는 **context switch cost**라고 하며, 고성능 시스템에서 매우 중요합니다.

---

## 4. 왜 중요한가

User Space는 **사용자 로직과 운영체제 핵심 영역을 분리**하기 위해 존재합니다.

### 이것이 없으면

애플리케이션 하나의 버그가 다음으로 이어질 수 있습니다.

- kernel corruption
- memory overwrite
- filesystem corruption
- device crash

### 현대 시스템의 핵심 안정성 기반

운영체제는 User Space를 **sandbox처럼 격리**합니다. 특히 다음 환경에서 권한 격리가 절대적으로 중요합니다.

- multi-user system
- cloud infrastructure
- container platform
- fintech platform

---

## 5. 실제 장애와 어떤 관련이 있는가

User Space 문제는 운영 장애로 매우 자주 이어집니다.

### 대표 장애 유형

| 유형 | 설명 |
|------|------|
| Memory Leak | heap 지속 증가 → OOM, swap storm, container eviction |
| Infinite Loop | CPU 100% |
| Excessive Syscall | kernel transition overload |
| File Descriptor Leak | socket/file exhaustion |
| Userspace Crash | segmentation fault |
| Fork Bomb | process explosion |

### Kubernetes 환경에서 특히 중요한 문제

| 문제 | 영향 |
|------|------|
| container OOM | pod restart |
| FD exhaustion | connection failure |
| syscall overload | node slowdown |
| userspace CPU saturation | latency spike |

### 성능 병목

대부분의 실제 서비스 병목은 Kernel보다 **User Space에서 발생**합니다.

---

## 6. 핵심 메커니즘

핵심은 **User Space는 직접 하드웨어를 제어하지 못한다**는 것입니다.

### 실제 흐름

**파일 읽기**

```
Application
  → read()
  → syscall boundary
  → kernel filesystem
  → block I/O
  → storage
```

**네트워크 송신**

```
Application
  → socket API
  → kernel TCP/IP stack
  → NIC
```

**메모리 할당**

```
malloc()
  → virtual memory request
  → kernel page allocation
```

### 매우 중요한 개념

User Space는 **가상 메모리 기반**으로 동작합니다. 즉 프로세스마다 **독립 메모리 공간**을 보유합니다.

### Context Switch

커널 진입 시 **CPU privilege level 변경**이 발생하며, 이 비용이 고성능 시스템에서 매우 중요합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux 명령어

```bash
# 프로세스 관측
ps aux

# 메모리 관측
top
htop

# syscall 추적
strace

# open file
lsof

# virtual memory
cat /proc/<pid>/maps

# process statistics
cat /proc/<pid>/status

# CPU context switch
vmstat
pidstat
```

### Kubernetes 중요 관측 포인트

| 요소 | 관련성 |
|------|--------|
| container process | userspace execution |
| cgroup | resource isolation |
| namespace | process isolation |
| kubelet | userspace daemon |
| sidecar | additional userspace process |

### Runtime 관측 포인트

- syscall rate
- context switch/sec
- RSS memory
- userspace CPU
- FD usage
- thread count
- page fault
- userspace latency

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*