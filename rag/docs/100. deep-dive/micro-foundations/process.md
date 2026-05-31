# 프로세스 (Process)

> 정독: 0회

## 1. 이 기술이 무엇인가

프로세스는:

> 실행 중인 프로그램의 독립 실행 단위

단순 파일이 아니라 실제 메모리와 CPU 자원을 가진 실행 실체입니다.

| 구성 요소 | 설명 |
|-----------|------|
| PID | 프로세스 식별자 |
| 가상 주소 공간 | 독립 메모리 |
| 코드 세그먼트 | 실행 명령어 |
| 힙 | 동적 메모리 |
| 스택 | 함수 문맥 |
| 레지스터 상태 | CPU 실행 상태 |
| 파일 디스크립터 | 열린 파일/소켓 |
| 스레드 | 실제 실행 흐름 |

> 프로세스 = 프로그램 + 실행 상태 + 자원 집합

<details>
<summary>Deep Dive</summary></br>

Running State(실행 상태) [[M]](../../100-deep-dive/micro-foundations/running-state.md)  
Program(프로그램) [[M]](../../100-deep-dive/micro-foundations/program.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

프로세스는 운영체제 전체의 기본 실행 단위입니다.

| 계층 | 역할 |
|------|------|
| OS Kernel | process scheduling |
| CPU Scheduler | 실행 순서 관리 |
| Virtual Memory | 독립 주소 공간 |
| Runtime | application execution |
| Container Runtime | isolated process group |
| System Call Layer | kernel interaction |

| 프로그램 | 프로세스 |
|----------|----------|
| 브라우저 실행 | browser process |
| DB 서버 실행 | database process |
| 웹 서버 실행 | worker/master process |
| shell 실행 | shell process |

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**가장 직접적 영향: CPU + Memory**

| 자원 | 영향 |
|------|------|
| CPU | scheduling/context switch |
| Memory | address space/heap/stack |
| Disk | executable loading/page fault |
| Network | socket ownership |

프로세스 수 증가 시: context switch 증가 / memory pressure 증가 / scheduler overhead 증가 발생 가능

---

## 4. 왜 중요한가

시스템 격리와 멀티태스킹의 핵심 단위입니다.

| 문제 | 결과 |
|------|------|
| 메모리 충돌 | corruption |
| 실행 상태 오염 | instability |
| 권한 분리 실패 | security issue |
| 장애 전파 | system-wide crash |

프로세스 구조 덕분에 프로그램 격리 / 사용자 분리 / 자원 제한 / 장애 격리 / 보안 경계 구현이 가능합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

| 문제 | 원인 |
|------|------|
| OOMKill | excessive process memory |
| Zombie Process | unreaped child |
| Fork Bomb | uncontrolled creation |
| Context Switch Storm | too many runnable processes |
| Deadlock | process synchronization issue |
| Process Hang | blocked syscall |
| CPU Saturation | runaway process |

**대표 현상:** load average 증가 / scheduler latency 증가 / RSS 급증 / process state stuck / swap 증가

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 프로세스 ≠ 프로그램

| 개념 | 의미 |
|------|------|
| 프로그램 | 디스크 파일 |
| 프로세스 | 실행 중 상태 |

하나의 프로그램 파일로 여러 프로세스를 생성할 수 있습니다.

### 독립 가상 주소 공간

각 프로세스는 독립 virtual memory space를 가집니다.

| 영역 | 역할 |
|------|------|
| Text | 실행 코드 |
| Data | 전역/static |
| Heap | dynamic allocation |
| Stack | function frame |

프로세스 간 직접 메모리 접근은 불가능합니다.

### PCB

운영체제 핵심 데이터 구조로, 다음 정보를 저장합니다.

| 정보 | 내용 |
|------|------|
| PID | 식별자 |
| register state | CPU 상태 |
| scheduling info | 우선순위 |
| memory mapping | 주소 공간 |
| open files | FD 목록 |
| signal state | signal 관리 |

### Process State

| 상태 | 의미 |
|------|------|
| Running | CPU 사용 중 |
| Ready | 실행 대기 |
| Sleeping | event wait |
| Stopped | 중단 |
| Zombie | 종료 후 미회수 |

scheduler가 상태 관리를 수행합니다.

### Context Switch

CPU가 다른 프로세스로 전환 시 현재 register/context를 저장 후 복원합니다.

비용 발생: cache invalidation / TLB flush / scheduler overhead. 과도하면 성능이 저하됩니다.

### Process Creation

| 시스템 | 호출 |
|--------|------|
| Unix/Linux | fork + exec |
| Windows | CreateProcess |

- **fork:** 부모 프로세스 복제
- **exec:** 새 executable image 로드

### Copy-On-Write

초기에는 메모리 page를 공유하다가, 쓰기 발생 시 새 page를 복사합니다. fork 최적화의 핵심입니다.

### IPC

프로세스는 기본적으로 격리되므로, 통신 필요 시 다음 방식을 사용합니다.

| 방식 | 설명 |
|------|------|
| Pipe | stream |
| Socket | network IPC |
| Shared Memory | memory sharing |
| Message Queue | queued IPC |
| Signal | async notification |

### Privilege Boundary

프로세스는 user mode에서 실행되며, kernel 접근 시 system call이 필요합니다.

### Scheduler

CPU time slice 배분 / fairness 유지 / priority 관리를 수행합니다.

### Threads

| 개념 | 특징 |
|------|------|
| Process | 독립 주소 공간 |
| Thread | 동일 주소 공간 공유 |

thread는 프로세스 내부 실행 흐름입니다.

### Signals

| Signal | 의미 |
|--------|------|
| SIGKILL | 강제 종료 |
| SIGTERM | 정상 종료 요청 |
| SIGSEGV | invalid memory access |
| SIGSTOP | 중단 |

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

```bash
# 프로세스 목록
ps -ef

# 실시간 상태
top
htop

# 프로세스 상세
cat /proc/<pid>/status

# 메모리 매핑
cat /proc/<pid>/maps

# open fd
ls -l /proc/<pid>/fd

# thread 상태
ps -T -p <pid>

# scheduler 상태
pidstat
sar -q
```

### Runtime

| 현상 | 관련 |
|------|------|
| heap growth | process memory |
| thread explosion | runtime scheduling |
| GC pause | runtime memory |
| deadlock | synchronization |

### Kubernetes

container의 본질은 **격리된 프로세스 집합**입니다.

| 요소 | 의미 |
|------|------|
| container | process namespace |
| pod | grouped processes |
| cgroup | resource limit |
| namespace | isolation |
| OOMKill | memory enforcement |

| 현상 | 원인 |
|------|------|
| CrashLoopBackOff | repeated process failure |
| OOMKilled | cgroup limit 초과 |
| PID exhaustion | too many processes |
| high load average | runnable overload |

### Observability

| 도구 | 목적 |
|------|------|
| ps | process list |
| top/htop | runtime status |
| perf | CPU profiling |
| strace | syscall tracing |
| lsof | file/socket |
| eBPF | scheduler/process tracing |

| 지표 | 의미 |
|------|------|
| RSS | resident memory |
| CPU usage | execution load |
| context switch | scheduler pressure |
| thread count | concurrency |
| fd count | resource usage |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*