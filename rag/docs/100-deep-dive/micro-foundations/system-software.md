# System Software (시스템 소프트웨어)
## 1. 시스템 소프트웨어(System Software)란 무엇인가

> 정독: 0회

시스템 소프트웨어(System Software)는:

> 컴퓨터 하드웨어를 직접 제어·관리하고, 응용 프로그램(Application)이 안정적으로 실행될 수 있도록 실행 기반 환경을 제공하는 **핵심 소프트웨어 계층**

```
Hardware
↕
System Software
↕
Application
```

구조에서 **중간 관리자** 역할을 수행합니다.

### 대표 구성

| 구성 요소 | 역할 |
|----------|------|
| Operating System | 자원 관리 |
| Kernel | 하드웨어 직접 제어 |
| Device Driver | 장치 통신 |
| File System | 저장소 관리 |
| Runtime Loader | 실행 준비 |
| Utility Program | 유지보수 |
| Compiler/Interpreter | 코드 번역 |

> **핵심:** 시스템 소프트웨어는 "컴퓨터 전체를 운영하는 기반 계층"입니다.

---

## 2. 시스템 어디에서 등장하는가

시스템 소프트웨어는 하드웨어 바로 위, 애플리케이션 바로 아래에 존재합니다.

```
Hardware
→ Firmware
→ Kernel
→ Operating System
→ System Software Layer
→ Runtime
→ Application
→ End User
```

**실제 역할:**

```
CPU/Memory/NIC/Disk
→ OS/KERNEL 관리
→ System API 제공
→ Application 실행
```

하드웨어는 직접 사용하기 매우 복잡하고, 시스템 소프트웨어가 이를 추상화하여 일반 프로그램이 쉽게 사용할 수 있게 만듭니다.

### 기능별 역할

| 기능 | 시스템 소프트웨어 역할 |
|------|----------------------|
| 파일 저장 | File System |
| 네트워크 송신 | TCP/IP Stack |
| 메모리 분배 | Virtual Memory |
| 프로세스 실행 | Scheduler |
| 장치 접근 | Driver |
| 프로그램 실행 | Loader |

---

## 3. 어떤 자원(CPU / Memory / Network / Disk)에 가장 영향이 큰가

시스템 소프트웨어는 사실상 **모든 시스템 자원을 직접 통제**합니다.

| 자원 | 영향도 |
|------|--------|
| CPU | 매우 큼 |
| Memory | 매우 큼 |
| Disk | 매우 큼 |
| Network | 매우 큼 |

> 시스템 소프트웨어는 전체 컴퓨터 자원 관리의 중심입니다.

### CPU 영향

- Process Scheduling
- Interrupt Handling
- Context Switching
- System Call 처리

```
다수 프로세스 실행
→ Scheduler 동작
→ CPU Time 분배
```

### Memory 영향

- Virtual Memory
- Page Allocation
- Memory Protection
- Cache 관리

> 메모리 안정성 대부분을 담당합니다.

### Network 영향

- TCP/IP Stack
- Socket Layer
- Packet Routing
- NIC Driver

> 모든 네트워크 패킷은 시스템 소프트웨어를 통과합니다.

### Disk 영향

- File System
- Block I/O
- Page Cache
- Storage Scheduler

> 디스크 IO 흐름 전체를 통제합니다.

---

## 4. 왜 중요한가

현대 컴퓨터 시스템의 안정성과 성능이 대부분 시스템 소프트웨어 위에서 결정되기 때문입니다.

> 좋은 하드웨어만으로는 좋은 시스템이 만들어지지 않는다. System Software가 자원을 얼마나 효율적으로 통제하느냐가 핵심이다.

| 영역 | 시스템 소프트웨어 영향 |
|------|----------------------|
| 서버 성능 | Scheduler |
| 메모리 안정성 | VM 관리 |
| 네트워크 성능 | TCP Stack |
| 저장소 성능 | File System |
| 컨테이너 격리 | Namespace/cgroup |
| 클라우드 운영 | Kernel 기능 |

> 현대 클라우드/K8s/SRE 환경은 결국 Linux Kernel 기반 System Software 위에서 동작합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 대규모 장애 상당수가 시스템 소프트웨어 레벨과 연결됩니다.

| 장애 | 시스템 소프트웨어 원인 |
|------|----------------------|
| CPU Soft Lockup | Scheduler 문제 |
| OOM Killer | Memory 관리 |
| Disk IO Hang | File System 문제 |
| Packet Drop | Network Stack 문제 |
| Kernel Panic | Kernel Crash |
| Container Freeze | cgroup 문제 |
| High System CPU | Syscall 폭증 |
| TCP Timeout | TCP Stack 정체 |

> Application 문제처럼 보여도 실제로는 Kernel/System Layer 병목인 경우가 많습니다.

**대표 예시:**

```
앱 느림          → Disk IO Wait
API Timeout      → TCP Retransmission
Pod Restart      → OOM Killer
Latency 증가     → Context Switching 폭증
```

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 6-1. 시스템 소프트웨어는 하드웨어 추상화 계층이다

디스크 섹터 제어, CPU Interrupt, DMA, NIC Queue 등을 애플리케이션이 직접 다루는 것은 거의 불가능합니다.

```
Hardware Complexity
↓
System Software가 추상화
↓
Application은 단순 API만 사용
```

### 6-2. Kernel이 핵심 중심이다

| 기능 | 설명 |
|------|------|
| Process 관리 | 실행 흐름 제어 |
| Memory 관리 | 가상 메모리 |
| File System | 저장소 제어 |
| Network Stack | 패킷 처리 |
| Driver 관리 | 장치 통신 |
| Security | 권한 통제 |

> 커널은 컴퓨터 전체 자원의 중앙 통제자입니다.

### 6-3. Application은 System Software 위에서만 동작 가능하다

```
Application
→ Runtime
→ System Call
→ Kernel
→ Hardware
```

애플리케이션은 CPU 직접 접근, Memory 직접 통제, NIC 직접 제어를 하지 못하며, 반드시 시스템 소프트웨어 계층을 거칩니다.

### 6-4. 현대 클라우드/K8s도 결국 System Software 확장이다

컨테이너 핵심 기술인 Namespace, cgroup, OverlayFS, Netfilter는 모두 시스템 소프트웨어 기능입니다.

```
Kubernetes
→ Container Runtime
→ Linux Kernel
→ Hardware
```

---

## 7. Linux / Runtime / Kubernetes에서 어디서 관측되는가

### Linux

**대표 관측 도구:**

```bash
top / htop / vmstat / iostat / sar / dmesg
```

**관측 가능 항목:** System CPU / IO Wait / Interrupt / Context Switch / Memory Pressure

### Kernel 상태

```bash
cat /proc/*
sysctl -a
```

확인 가능: VM 상태 / TCP 상태 / Scheduler 상태 / FD Limit

### Network Stack

```bash
ss -s
netstat -s
ethtool
tcpdump
```

확인 가능: TCP Retransmission / Socket 상태 / NIC Queue / Packet Loss

### Kubernetes

K8s는 System Software 의존성이 매우 강합니다.

```bash
kubectl top
kubectl describe node
kubectl logs
```

관측 가능: Node Pressure / OOM / CPU Throttling / Network Issue

### Observability

현대 시스템에서는 Prometheus, eBPF, perf, tracing 등으로 시스템 소프트웨어 상태를 추적합니다.

### 대표 메트릭

| 메트릭 | 의미 |
|--------|------|
| System CPU | Kernel 사용량 |
| Context Switch | 스케줄링 부하 |
| IO Wait | 디스크 병목 |
| Page Fault | 메모리 압박 |
| TCP Retransmission | 네트워크 문제 |
| Interrupt Rate | 장치 부하 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*