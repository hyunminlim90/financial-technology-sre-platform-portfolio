# 시스템 콜 인터페이스 (System Call Interface, SCI)

> 정독: 0회

## 1. 이 기술이 무엇인가

시스템 콜 인터페이스는:

> 사용자 프로그램이 **운영체제 커널 기능을 호출하기 위한 표준 진입 인터페이스**

애플리케이션은 직접 하드웨어를 제어하지 못합니다. 대신 시스템 콜 인터페이스를 통해 커널에게 작업을 요청합니다.

**대표 시스템 콜:**

| 기능 | 예시 |
|------|------|
| 파일 I/O | open, read, write |
| 프로세스 제어 | fork, exec |
| 메모리 관리 | mmap, brk |
| 네트워크 | socket, connect |
| 동기화 | futex |
| 디바이스 제어 | ioctl |

**핵심:** User Mode → Kernel Mode 진입, 보호된 자원 접근, 커널 서비스 요청

---

## 2. 시스템 어디에서 등장하는가

시스템 콜 인터페이스는 거의 모든 애플리케이션 실행 과정에서 등장합니다.

**등장 위치:** 파일 읽기/쓰기, 네트워크 통신, 프로세스 생성, 스레드 생성, 메모리 매핑, 디스크 접근, 컨테이너 런타임, Kubernetes node process, Terraform/OpenTofu binary, 데이터베이스, 웹 서버

애플리케이션 대부분은 결국 시스템 콜을 반복적으로 호출합니다.

```
Application
 → Runtime Library
   → System Call
     → Kernel
       → Hardware
```

---

## 3. 어떤 자원에 가장 영향이 큰가

시스템 콜은 커널 자원 전체와 연결됩니다.

| 자원 | 영향 |
|------|------|
| CPU | mode switch 발생 |
| Memory | mmap, page allocation |
| Disk | file I/O |
| Network | socket I/O |
| Scheduler | process/thread control |

특히 중요한 부분은 Context Switch, Kernel Entry/Exit, I/O Latency, Blocking Behavior입니다.

---

## 4. 왜 중요한가

시스템 콜 인터페이스는 **사용자 공간과 커널 공간의 경계**입니다.

없다면 프로그램이 직접 디스크를 제어하거나, RAM을 직접 오염시키거나, 다른 프로세스 메모리를 침범하여 시스템 전체가 붕괴될 수 있습니다.

운영체제는 시스템 콜 인터페이스를 통해 권한 검증, 메모리 보호, 자원 격리, 하드웨어 접근 통제를 수행합니다.

**성능 측면에서도 매우 중요합니다.** 시스템 콜은 단순 함수 호출보다 훨씬 비쌉니다.

이유: CPU privilege level 전환, pipeline flush 가능성, kernel context 진입, scheduler 개입 가능성

---

## 5. 실제 장애와 어떤 관련이 있는가

### Excessive System Call

시스템 콜 과다 발생 시 CPU usage 급증, syscall overhead 증가, latency 증가가 나타납니다.

```bash
strace -c
```

### Blocking I/O 문제

read/write/socket syscall 대기로 thread blocking, request stall, throughput 저하가 발생할 수 있습니다.

### File Descriptor Exhaustion

open/socket 반복 후 close 누락 시 `Too many open files` 오류가 발생합니다.

```bash
lsof
ulimit -n
```

### Kernel Panic / Driver Issue

잘못된 kernel interaction으로 driver crash, invalid syscall path가 발생할 수 있습니다.

### Context Switch 폭증

thread/process 과다로 syscall, scheduler, futex가 증가하여 CPU saturation, latency spike가 발생합니다.

### I/O Wait 증가

디스크/네트워크 syscall 대기로 `wa%`가 상승합니다.

```bash
top
iostat
vmstat
```

---

## 6. 핵심 메커니즘

### 1) User Mode 실행

프로세스는 기본적으로 User Mode에서 실행됩니다. 직접 디스크, NIC, kernel memory에 접근할 수 없습니다.

### 2) Runtime Library 호출

프로그램이 `read()`, `write()`, `socket()`, `malloc()` 등을 호출하면 런타임 라이브러리 함수로 진입합니다.

### 3) System Call Number 준비

라이브러리가 syscall number, argument, pointer 등을 CPU register에 적재합니다.

### 4) syscall instruction 실행

| Architecture | 명령 |
|-------------|------|
| x86_64 | syscall |
| old x86 | int 0x80 |
| ARM | svc |

이 순간 **User Mode → Kernel Mode**, privilege level 상승이 발생합니다.

### 5) Kernel Handler 진입

커널이 syscall number 확인, permission 검증, memory validation, resource check를 수행합니다.

### 6) 실제 하드웨어 작업 수행

disk read, network send, memory allocation, scheduler operation 등을 실행합니다.

### 7) 결과 반환 후 User Mode 복귀

커널이 return value 저장, CPU state 복원, User Mode 복귀를 수행합니다. 프로세스는 정상 실행 흐름을 계속 진행합니다.

---

## 7. Linux/Runtime/K8s에서 관측 방법

### Linux

```bash
# 시스템 콜 추적
strace -p <PID>

# syscall 통계
strace -c

# Kernel Event 추적
perf trace

# Context Switch 관측
vmstat 1
pidstat -w

# File Descriptor 상태
lsof
cat /proc/<PID>/fd/*

# Process System Call 상태
cat /proc/<PID>/syscall
```

### Kubernetes 환경

```bash
# 컨테이너 내부 syscall tracing
kubectl exec -it <pod> -- strace <binary>
```

**보안 정책으로 syscall 제한 가능:** seccomp, AppArmor, SELinux

seccomp 차단 시 허용되지 않은 syscall이 발생하면 `Operation not permitted` 오류가 반환됩니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*