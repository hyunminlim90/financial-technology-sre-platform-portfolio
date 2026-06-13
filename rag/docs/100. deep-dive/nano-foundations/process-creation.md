# 프로세스 생성 (Process Creation)

> 정독: 0회

## 1. 이 기술이 무엇인가

프로세스 생성(Process Creation)은:

> 디스크에 존재하던 실행 파일을 운영체제가 메모리에 적재하고 독립적인 실행 컨텍스트를 생성하며 CPU 스케줄링 대상으로 등록하는 단계

**핵심:** process creation establishes an isolated runtime execution environment

프로세스는 일반적으로 다음을 포함합니다:

| 구성 | 설명 |
|------|------|
| virtual address space | 독립 메모리 공간 |
| execution context | CPU 실행 상태 |
| process metadata | PID, permissions |
| resource tables | file/socket handles |
| runtime memory regions | stack/heap/code |

<details>
<summary>Deep Dive</summary></br>

[[M]](../../100-deep-dive/micro-foundations/)

</details></br>

## 2. 시스템 어디에서 등장하는가

프로세스 생성은 모든 운영체제의 핵심 기능입니다.

| 영역 | 사용 위치 |
|------|----------|
| operating system kernel | task creation |
| container runtime | isolated workload launch |
| shells | command execution |
| application servers | worker spawning |
| schedulers | job execution |
| distributed systems | service startup |

**대표 환경:**

| 시스템 | 역할 |
|--------|------|
| Linux kernel | fork/exec |
| Windows kernel | CreateProcess |
| containerd | container process launch |
| Kubernetes | pod container execution |
| systemd | service management |

> **즉:** all executable software ultimately depends on operating system process creation

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**가장 영향 큰 자원: Memory + CPU**

이유: 프로세스 생성 시 다음이 발생하기 때문입니다.

- virtual memory allocation
- page table construction
- executable mapping
- scheduler registration

| 자원 | 영향 |
|------|------|
| Memory | address space allocation |
| CPU | scheduler/context initialization |
| Disk | executable loading |
| Network | remote image fetch/container pull |

> **특히 대규모 시스템에서는:** frequent process creation can become a major kernel scheduling overhead

---

## 4. 왜 중요한가

프로세스 생성은 **the runtime activation boundary of executable software**입니다.

프로세스가 생성되지 않으면:

- 실행 불가
- 메모리 사용 불가
- CPU 스케줄링 불가
- 런타임 시작 불가

**중요한 이유:**

| 이유 | 설명 |
|------|------|
| isolation | 독립 주소 공간 |
| security boundary | 권한 분리 |
| scheduling | CPU 실행 단위 |
| resource ownership | 메모리/FD 소유 |
| runtime initialization | 실행 기반 구축 |

> **운영체제 관점 핵심:** the process is the primary resource management unit of modern operating systems

---

## 5. 실제 장애와 어떤 관련이 있는가

프로세스 생성 문제는 시스템 장애로 직결됩니다.

**대표 장애:**

| 장애 | 원인 |
|------|------|
| fork failure | memory exhaustion |
| exec failure | missing executable |
| OOM kill | insufficient memory |
| zombie process accumulation | unreaped child |
| process storm | runaway spawning |
| scheduler overload | excessive process count |

**실무에서 매우 중요:**

| 문제 | 결과 |
|------|------|
| excessive forking | CPU saturation |
| container restart loops | orchestration instability |
| memory fragmentation | allocation failure |
| process leaks | PID exhaustion |
| startup latency | slow deployment |

> **특히 Kubernetes에서는:** container startup instability often originates from process creation failures

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Executable Loading

첫 단계: **the operating system loads executable contents into memory**

| 작업 | 설명 |
|------|------|
| executable parsing | ELF/PE/Mach-O 분석 |
| segment mapping | code/data mapping |
| permission setup | RX/RW pages |
| loader initialization | runtime bootstrap |

```
disk executable
→ kernel loader
→ virtual memory mapping
→ process image creation
```

---

### Virtual Address Space Creation

운영체제는 **create an isolated virtual address space per process** 합니다.

| 영역 | 의미 |
|------|------|
| text segment | executable code |
| data segment | global/static data |
| heap | dynamic allocation |
| stack | function frames |
| shared libraries | runtime dependencies |

> processes do not directly share raw memory by default

---

### Process Control Block (PCB)

커널 핵심 자료구조: **PCB stores process execution metadata**

| 정보 | 설명 |
|------|------|
| PID | process identifier |
| register state | CPU context |
| scheduling info | execution priority |
| memory mappings | address space |
| file descriptors | opened resources |

---

### PID Allocation

프로세스 생성 시 **the kernel allocates a unique process identifier** 합니다.

PID는 다음의 핵심 기준입니다:
- scheduler 관리
- signal routing
- process tracking

---

### Runtime Initialization

프로세스 생성 이후 **runtime infrastructure initialization begins**입니다.

| 작업 | 설명 |
|------|------|
| runtime bootstrap | VM startup |
| heap initialization | allocator setup |
| thread creation | execution workers |
| JIT initialization | compiler startup |
| GC startup | memory governance |

---

### fork / exec Model

Linux 핵심 모델입니다.

```
fork()
→ duplicate process context
→ exec()
→ replace process image
```

**fork 특징:**

| 특징 | 설명 |
|------|------|
| copy-on-write | 초기 memory sharing |
| duplicated descriptors | inherited resources |
| scheduler duplication | runnable entity 생성 |

**exec 특징:**

> replace current process image with new executable

---

### Memory Mapping

프로세스 생성 시 핵심: **executables are mapped into virtual memory regions**

대표 syscall: `mmap()`

효과:
- lazy loading
- shared library reuse
- efficient startup

---

### Scheduler Registration

생성된 프로세스는 **becomes a schedulable kernel entity**입니다.

커널은 다음을 수행합니다:
- runnable queue 등록
- CPU time allocation
- context switching

---

### Context Switching

CPU가 프로세스 간 실행 상태를 전환하는 동작입니다.

과도한 process 수는 다음을 유발합니다:
- cache invalidation
- scheduler overhead
- latency 증가

---

### Runtime Boundary

> **process creation marks the transition from static storage state to active execution state**

| 이전 | 이후 |
|------|------|
| executable file | running process |
| static bytes | active instructions |
| disk artifact | scheduled entity |

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**관련 syscall:**
- `fork()`
- `execve()`
- `clone()`
- `waitpid()`

**관측 도구:**

| 도구 | 목적 |
|------|------|
| `ps` | process state |
| `top`/`htop` | runtime activity |
| `pstree` | parent-child tree |
| `strace` | syscall tracing |
| `pidstat` | scheduling metrics |

예:
```bash
strace -f ./app
```

---

### Runtime / VM

| 항목 | 의미 |
|------|------|
| VM bootstrap | runtime startup |
| heap initialization | allocator start |
| thread pool startup | worker provisioning |
| JIT startup | code cache init |
| GC startup | memory governance |

> 특히 **startup latency** 분석이 중요합니다.

---

### Kubernetes

K8s에서는 **container startup fundamentally depends on Linux process creation**입니다.

```
pod scheduling
→ container runtime
→ OCI execution
→ process creation
→ runtime bootstrap
```

**실무 주요 장애:**

| 문제 | 결과 |
|------|------|
| CrashLoopBackOff | startup failure |
| image pull delay | slow process activation |
| OOM on startup | container termination |
| PID exhaustion | node instability |
| slow cold start | deployment latency |

---

### Observability

| 관측 항목 | 의미 |
|----------|------|
| process count | scheduler pressure |
| RSS/VSZ | memory footprint |
| startup time | runtime initialization |
| page faults | lazy loading |
| context switches | CPU overhead |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*