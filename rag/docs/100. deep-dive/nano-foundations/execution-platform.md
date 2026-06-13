# 실행 플랫폼 (Execution Platform)

> 정독: 0회

## 1. 이 기술이 무엇인가

실행 플랫폼은:

> 프로그램이 실제로 실행될 수 있도록 제공되는 **전체 실행 기반 환경**

단순히 운영체제만 의미하지 않습니다. 다음 요소들이 함께 결합된 실행 기반 전체를 의미합니다.

CPU 아키텍처, 운영체제 커널, 메모리 시스템, 런타임 라이브러리, 시스템 콜 인터페이스, 실행 엔진, 프로세스 모델, 스레드 모델, 파일 시스템, 동적 로더, 네트워크 스택

> **프로그램이 실행 가능한 전체 환경**이 실행 플랫폼입니다.

---

## 2. 시스템 어디에서 등장하는가

실행 플랫폼은 프로그램 실행의 모든 순간에 등장합니다.

| 영역 | 실행 플랫폼 역할 |
|------|----------------|
| 프로세스 생성 | 메모리 공간 제공 |
| 스레드 실행 | 스케줄링 수행 |
| 파일 I/O | 시스템 콜 처리 |
| 네트워크 | 소켓 스택 제공 |
| 메모리 관리 | 가상 메모리 관리 |
| 동적 링크 | 런타임 라이브러리 로딩 |
| 실행 엔진 | 기계어 실행 |
| 컨테이너 | namespace/cgroup 기반 실행 |
| Kubernetes | container runtime 기반 실행 |

프로그램은 실행 플랫폼 없이는 동작할 수 없습니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

실행 플랫폼은 시스템 전체 자원과 연결됩니다.

| 자원 | 영향 |
|------|------|
| CPU | 스케줄링/실행 |
| Memory | 가상 메모리/할당 |
| Disk | 실행 파일/I/O |
| Network | socket stack |
| Kernel | syscall handling |

특히 핵심 영향은 CPU scheduling, memory allocation, syscall overhead, process isolation, runtime overhead입니다.

---

## 4. 왜 중요한가

실행 플랫폼은 **하드웨어와 소프트웨어 사이의 복잡성을 숨깁니다.**

없다면 프로그램은 직접 CPU instruction 작성, memory mapping 제어, disk controller 제어, NIC register 제어, interrupt 처리를 해야 합니다.

실행 플랫폼은 이를 추상화하여 애플리케이션이 **논리 구현에 집중**할 수 있게 합니다.

또한 portability, isolation, security, resource management, concurrency control의 핵심 기반입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### Runtime Dependency Missing

필요한 runtime library 누락 시 `library not found` 오류가 발생합니다.

### ABI 호환성 문제

CPU/OS/runtime mismatch로 `illegal instruction`, `segmentation fault`, `symbol resolution failure`가 발생할 수 있습니다.

### Dynamic Linking Failure

`undefined symbol` 오류가 발생합니다. shared library mismatch, loader incompatibility가 원인입니다.

### System Call Compatibility 문제

Kernel version mismatch로 `syscall unsupported`, runtime crash가 발생할 수 있습니다.

### Memory Management 문제

실행 플랫폼의 allocator/runtime 문제로 memory leak, fragmentation, OOM이 발생할 수 있습니다.

### Scheduler 병목

thread 폭증으로 context switch 증가, CPU saturation, latency spike가 발생합니다.

### Container Runtime 문제

Kubernetes/container 환경에서 cgroup limit, namespace isolation, runtime shim failure 등이 발생할 수 있습니다.

---

## 6. 핵심 메커니즘

### 1) 실행 파일 로드

디스크의 실행 파일이 메모리로 적재됩니다. code segment, data segment, heap, stack이 생성됩니다.

### 2) 가상 주소 공간 생성

운영체제가 process, page table, virtual memory를 구성합니다.

### 3) Runtime Library 연결

동적 로더가 shared library와 runtime dependency를 연결합니다. (예: `libc.so`)

### 4) 실행 엔진 활성화

CPU가 명령 실행을 시작하거나, interpreter, JIT compiler, VM runtime 등이 활성화됩니다.

### 5) Scheduler 개입

스레드들이 CPU core를 경쟁합니다. 운영체제가 dispatch, context switch, timeslice allocation을 수행합니다.

### 6) System Call Interface 연결

프로그램이 file, network, memory, device에 접근 시 kernel로 진입합니다.

### 7) Hardware Execution 수행

최종적으로 instruction fetch → decode → execute → writeback이 physical core에서 수행됩니다.

---

## 7. Linux/Runtime/K8s에서 관측 방법

### Linux

```bash
# 프로세스 실행 환경
ps
top
htop

# 메모리 레이아웃 (heap, stack, shared library, mmap region 확인)
cat /proc/<PID>/maps

# Dynamic Library 확인
ldd <binary>

# 시스템 콜 관측
strace

# CPU Scheduling 상태
pidstat
vmstat
schedstat

# Runtime Dependency
ldconfig -p

# 커널 ABI 확인
uname -a
```

### Kubernetes

Kubernetes 실행 플랫폼 구성 요소: container runtime, OCI runtime, cgroup, namespace, overlay filesystem

```bash
kubectl describe pod
crictl ps
ctr containers ls
```

### Container Runtime

대표 실행 플랫폼 구성 요소: containerd, runc, CRI-O, Docker Engine

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*