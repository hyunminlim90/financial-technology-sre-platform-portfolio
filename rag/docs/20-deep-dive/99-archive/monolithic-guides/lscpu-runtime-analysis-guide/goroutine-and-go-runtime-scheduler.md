# Go Routine과 Go Runtime Scheduler 구조

## 1. Go Routine 개요

Go Routine은 Go Runtime이 관리하는 **경량 실행 흐름**입니다. OS Kernel Thread 자체가 아니라, Go Runtime 내부에서 생성되고 관리되는 사용자 수준의 실행 단위입니다.

```go
go processTask()
```

이 코드는 OS Kernel Thread를 직접 새로 만드는 것이 아니라, Go Runtime 내부에 실행할 작업 단위를 등록합니다.

```
Go Routine = Go Runtime이 관리하는 경량 실행 흐름
```

### 계층적 위치

```
Go Application
      ↓
Go Routine
      ↓
Go Runtime Scheduler
      ↓
   OS Thread
      ↓
Kernel Scheduler
      ↓
  Logical CPU
      ↓
Physical Core
```

Go Routine은 JVM 기술이 아닌 **Go 언어와 Go Runtime** 환경에 속하는 개념입니다.

### 계층별 메커니즘 실체 — Go Routine 생성

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **Go Runtime 내부 할당** | Go Runtime | `go` 키워드 → 내부적으로 `runtime.newproc()` 호출 → `G` 구조체 할당 → P의 Local Run Queue에 enqueue. OS Thread 생성 없음 | `runtime.NumGoroutine()`, `pprof goroutine`, `GODEBUG=schedtrace=1000` |
| **초기 Stack 할당** | Go Runtime | 초기 Stack 크기 2KB ~ 8KB(버전별 상이). Heap에서 연속 메모리 확보. OS Thread Stack(수 MB)과 대조적으로 매우 작음 | `go tool pprof -alloc_space`, `-memprofile` |
| **Stack 동적 확장** | Go Runtime | Stack 사용 시 Goroutine Stack Guard 체크 → 부족 시 `runtime.morestack()` 호출 → 더 큰 Stack 복사(Copying GC 방식) → 이전 Stack 해제 | `GODEBUG=gcstackbarrierall=1`, `runtime/pprof stack` |
| **OS 시스템 콜** | App → Kernel | `go` 키워드 자체는 시스템 콜 없음. 실제 OS Thread 생성이 필요한 경우(Blocking syscall 발생 시)에만 `clone()` 호출 → `task_struct` 생성 | `strace -e clone`, `/proc/PID/status`의 `Threads` 항목 |
| **cgroup 자원 격리** | Kernel | Go 프로세스 전체가 cgroup v2 하위에서 CPU/Memory 제한을 받음. Goroutine 수가 많아도 cgroup CPU Quota 소진 시 전체 Throttling | `/sys/fs/cgroup/cpu.stat`, `cat /sys/fs/cgroup/cpu.max`, `cadvisor` |

---

## 2. Go Routine의 구성 요소

| 구성 요소 | 설명 |
|-----------|------|
| 실행할 함수 | 실행 대상 함수 주소 |
| 실행 상태 | Running, Waiting 등 |
| Stack | 초기에는 작은 Stack으로 시작, 필요 시 동적 확장 |
| Scheduling Metadata | Runtime Scheduler가 관리하는 정보 |
| 대기 정보 | Channel, Mutex, I/O 대기 상태 등 |

### 계층별 메커니즘 실체 — G 구조체 내부

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **G 구조체 (runtime.g)** | Go Runtime | `stack.lo` / `stack.hi`: Stack 경계 포인터. `sched.pc`: 다음 실행 Program Counter. `sched.sp`: Stack Pointer. `atomicstatus`: Goroutine 상태 원자 변수 | `dlv` (Go 디버거), `runtime.Stack()`, `pprof goroutine` |
| **Goroutine 상태 머신** | Go Runtime | `_Gidle` → `_Grunnable` → `_Grunning` → `_Gwaiting` / `_Gsyscall` → `_Gdead`. 각 상태 전이는 원자 CAS 연산으로 보장 | `GODEBUG=schedtrace=1000` (상태 분포 관찰), `pprof goroutine?debug=2` |
| **대기 원인 기록** | Go Runtime | `waitreason` 필드: `waitReasonChanReceive`, `waitReasonSelect`, `waitReasonGCMarkTermination` 등 대기 원인 명시. 장애 시 병목 지점 특정에 활용 | `pprof goroutine?debug=2` (waitreason 항목), `go tool trace` |
| **CPU Cache 영향** | Hardware | G 구조체는 Heap에 할당. 수만 개 Goroutine 존재 시 G 구조체 순회 과정에서 L3 Cache Miss 증가 → GC Mark 단계 지연 | `perf stat -e cache-misses`, `perf mem` |

---

## 3. Java Thread와의 핵심 차이: M:N 매핑

### Java Platform Thread: 1:1 매핑

```
Java Thread 1 ↔ OS Thread 1
Java Thread 2 ↔ OS Thread 2
Java Thread 3 ↔ OS Thread 3
```

### Go Routine: M:N 매핑

많은 Go Routine을 적은 수의 OS Thread 위에서 실행합니다.

```
100,000 Go Routines
        ↓
  Go Runtime Scheduler
        ↓
    8 OS Threads
        ↓
    8 Logical CPUs
```

Go Runtime Scheduler가 중간에서 Go Routine을 OS Thread에 다중화합니다.

### 계층별 메커니즘 실체 — M:N 매핑 구조

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **OS Thread 생성 억제** | Go Runtime + Kernel | Java 1:1 모델은 Thread마다 `clone(CLONE_THREAD)` 호출 → `task_struct` 생성. Go는 M 수를 `GOMAXPROCS` + Blocking 보정 수준으로 최소화 | `cat /proc/PID/status`의 `Threads`, `pstree -p PID` |
| **Context Switch 비용 절감** | Go Runtime vs Kernel | Java Thread 전환: Kernel Context Switch → 레지스터 저장/복원, TLB Flush 가능성. Go Goroutine 전환: Go Runtime 내부에서 PC/SP만 교체 → Kernel 개입 없음 | `vmstat cs`, `perf stat -e context-switches` (Go 프로세스 비교) |
| **Thread Stack vs Goroutine Stack** | Hardware + OS | OS Thread Stack: `ulimit -s` 기본 8MB, mmap으로 예약. Go Stack: Heap에서 동적 할당, 초기 2~8KB. 대규모 동시성에서 메모리 사용량 차이 수십 배 | `cat /proc/PID/maps` (stack 영역), `/proc/PID/smaps`의 `Stack` 항목, `pmap PID` |
| **NUMA 영향** | Hardware + Kernel | OS Thread는 NUMA 노드에 물리적으로 바인딩 가능. Go Runtime은 P(Processor)를 특정 NUMA 노드에 고정하지 않아 원격 메모리 접근(Remote NUMA Access) 발생 가능 | `numactl --hardware`, `numastat`, `perf stat -e node-load-misses` |
| **CPU Frequency Scaling** | Hardware + Kernel | Java Thread 수 감소 시 OS가 C-state 진입 → 다음 스케줄 시 Wakeup 지연. Go의 M 수 최소화도 동일 영향. `performance` governor 설정으로 완화 | `cpupower frequency-info`, `turbostat`, `/sys/devices/system/cpu/cpu*/cpufreq/scaling_governor` |

---

## 4. Go Runtime Scheduler: G-P-M 모델

Go Scheduler는 G(Goroutine), P(Processor), M(Machine) 세 요소로 구성됩니다.

| 구성 요소 | 의미 | 역할 |
|-----------|------|------|
| G | Goroutine | 실행해야 할 경량 실행 흐름 |
| P | Processor | G를 실행하기 위한 Runtime 스케줄링 컨텍스트 (Runnable G Queue 보유) |
| M | Machine | 실제 OS Thread |

### 실행 구조

```
Runnable Goroutines
        ↓
P Local Run Queue
        ↓
  M (OS Thread)
        ↓
Kernel Scheduler
        ↓
  Logical CPU
        ↓
 Physical Core
```

M은 P를 보유해야 Go Routine을 실행할 수 있습니다.

### GOMAXPROCS

동시에 Go 코드를 실행할 수 있는 **P의 개수**를 결정합니다. 보통 Logical CPU 수를 기준으로 설정됩니다.

```
GOMAXPROCS=8  →  최대 8개의 P 사용
```

### 계층별 메커니즘 실체 — G-P-M 모델

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **P 구조체 (runtime.p)** | Go Runtime | `runq`: 256개 크기의 Lock-free 원형 큐 (Local Run Queue). `runqhead` / `runqtail`: 원자적 접근. P가 없는 M은 Go 코드 실행 불가 | `GODEBUG=schedtrace=1000` (runqueue 길이 관찰), `go tool trace` |
| **M 구조체 (runtime.m)** | Go Runtime + Kernel | `curg`: 현재 실행 중인 G 포인터. `p`: 보유 중인 P 포인터. `spinning`: Idle M이 새 G를 탐색 중인 상태. OS Thread에 1:1 대응 | `runtime.NumGoroutine()`, `/proc/PID/status`의 `Threads`, `strace -p PID` |
| **Global Run Queue** | Go Runtime | P Local Queue 외에 Global Run Queue(GRQ) 존재. 약 61번의 Local Queue 스케줄마다 1회 GRQ 참조하여 공정성 보장. GRQ 접근 시 Lock 필요 | `GODEBUG=schedtrace=1000`의 `runqueue` 항목, `go tool trace`의 scheduler latency |
| **GOMAXPROCS와 CPU 배분** | Kernel + Go Runtime | GOMAXPROCS=N → N개의 P. Container 환경에서 cgroup CPU Quota와 불일치 시 Throttling 발생. `automaxprocs` 라이브러리로 cgroup 인식 가능 | `/sys/fs/cgroup/cpu.max`, `uber-go/automaxprocs`, `kubectl top pod` |
| **Kernel CFS 스케줄러** | Kernel | 각 M(OS Thread)은 `task_struct`로 CFS Runqueue에서 `vruntime` 기반 스케줄. M이 많을수록 Context Switch 증가 | `/proc/schedstat`, `perf sched latency`, `pidstat -w` |
| **CPU Pipeline 활용** | Hardware | P가 같은 CPU Core에서 연속으로 G를 실행하면 Branch Predictor / L1 Cache Warm 상태 유지 → IPC 향상. P가 다른 Core로 이동하면 Cache Cold | `perf stat -e instructions,cycles` (IPC 측정), `perf stat -e cache-misses` |

---

## 5. Go Routine이 가벼운 이유

### Java Thread vs Go Routine 비교

| 항목 | Java Thread | Go Routine |
|------|-------------|------------|
| 생성 주체 | JVM + OS | Go Runtime |
| OS Thread 생성 | 필요 (1:1) | 일반적으로 불필요 (M:N) |
| Stack | 상대적으로 크고 고정 | 작게 시작 후 동적 확장 |
| Scheduling | OS Kernel 중심 | Go Runtime 중심 |
| Context Switch | Kernel 개입 필요 | Runtime 내부 전환 가능 |
| 생성 비용 | 높음 | 낮음 |

### Context Switch 비교

```
Java Thread 전환:
Thread A → Kernel Context Switch → Thread B

Go Routine 전환:
Goroutine A → Runtime Scheduling → Goroutine B
```

### 계층별 메커니즘 실체 — 경량성의 실체

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **Go Context Switch 내부** | Go Runtime | `runtime.mcall()` → 현재 G의 PC/SP를 `g.sched`에 저장 → 다음 G의 `g.sched`에서 PC/SP 복원 → `runtime.schedule()` 재진입. 레지스터 저장 최소화 | `go tool trace` (Goroutine switch 이벤트), `GODEBUG=schedtrace=1000` |
| **Kernel Context Switch 비용** | Kernel + Hardware | OS Thread 전환: 범용 레지스터 전체 저장, FPU 상태, CR3(Page Table Base) 유지, TLB Flush(프로세스 간). Go Goroutine 전환은 이 중 대부분 생략 | `perf stat -e context-switches`, `vmstat cs`, `sar -w` |
| **TLB 영향 없음** | Hardware | Goroutine 전환은 동일 프로세스 주소 공간 내 실행 흐름 변경. Page Table 변경 없음 → TLB Flush 발생 안 함 | `perf stat -e dTLB-load-misses` (프로세스 간 비교 기준) |
| **Stack 복사와 Cache** | Hardware + Go Runtime | Stack 확장 시 `runtime.morestack()` → 새 Stack 메모리 할당 → 기존 Stack 복사 → 포인터 패치. 복사 중 L1/L2 Cache Miss 일시 증가 | `perf stat -e cache-misses`, `go tool pprof -alloc_objects` |
| **Off-CPU Time** | Kernel | Goroutine이 Channel / Mutex / I/O 대기 시 Goroutine은 `_Gwaiting` 상태로 전환되어 CPU를 점유하지 않음. OS Thread는 다른 G 실행 → On-CPU 프로파일에 보이지 않는 병목 | `offcputime-bpfcc -p PID`, `async-profiler wall` 방식(Java 비교), `go tool trace` |
| **Memory Bandwidth** | Hardware | 수만 개 Goroutine의 Stack이 Heap에 산재 → GC Scan 시 메모리 랜덤 접근 → Memory Bandwidth 소비 증가. NUMA 환경에서 더욱 심화 | `perf stat -e mem-loads,mem-stores`, `numastat -m`, `numactl --hardware` |

---

## 6. 주요 Runtime 기능

### Blocking 처리

Go Runtime은 Blocking 상황을 감지하고 다른 Go Routine이 계속 실행될 수 있도록 조정합니다.

```
Goroutine A
    ↓
Blocking I/O 또는 System Call
    ↓
M이 차단될 수 있음
    ↓
Runtime이 다른 M/P 조합으로 다른 Goroutine 실행
```

### 계층별 메커니즘 실체 — Blocking 처리

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **Blocking Syscall 처리** | Go Runtime + Kernel | Blocking syscall(`read()`, `write()` 등) 진입 전 `runtime.entersyscall()` → G 상태 `_Gsyscall` 전환, P를 M에서 분리 → 다른 M이 P를 가져가 계속 실행. Syscall 복귀 시 `runtime.exitsyscall()` → P 재획득 시도 | `strace -e read,write,epoll_wait`, `/proc/PID/syscall`, `GODEBUG=schedtrace=1000`의 `syscall` 카운터 |
| **새 M(OS Thread) 생성** | Go Runtime + Kernel | Blocking syscall 중 P를 가져갈 M이 없으면 `runtime.newm()` → `clone()` 시스템 콜 → 새 `task_struct` 생성. M 수 급증은 OS Context Switch 증가 원인 | `strace -e clone`, `/proc/PID/status`의 `Threads`, `cat /proc/sys/kernel/threads-max` |
| **IRQ / SoftIRQ 영향** | Hardware → Kernel | Blocking I/O 완료 시 NIC IRQ 발생 → `ksoftirqd`가 `sk_buff` 처리 → `epoll` 이벤트 → Netpoller가 Goroutine Wake-up. IRQ 처리 지연 시 Goroutine 재개 지연 | `/proc/interrupts`, `mpstat -I ALL`의 `%irq/%soft`, `/proc/softirqs` |
| **Goroutine Preemption** | Go Runtime | Go 1.14+에서 Signal 기반 비동기 Preemption 도입. `SIGURG` 시그널로 실행 중인 Goroutine을 안전 지점에서 강제 전환 → Long-running G가 P를 독점하는 문제 해결 | `strace -e signal` (SIGURG 관찰), `GODEBUG=asyncpreemptoff=1` (비교 테스트) |
| **cgroup CPU Throttling** | Kernel | Blocking syscall에서 복귀 후 P 재획득 시도 시점에 cgroup CPU Quota 소진 상태이면 Throttled → Goroutine 재개 지연 발생 | `/sys/fs/cgroup/cpu.stat`의 `nr_throttled`, `throttled_usec`, Prometheus `container_cpu_cfs_throttled_seconds_total` |
| **OOM Killer** | Kernel | Goroutine 수 폭증 → Stack Heap 소비 증가 → 시스템 메모리 압박 → `oom_score` 기반 프로세스 강제 종료. OOM 발생 전 PSI(Pressure Stall Information)로 조기 감지 가능 | `dmesg | grep -i oom`, `/proc/PID/oom_score`, `/proc/pressure/memory`, `oom_score_adj 설정` |

### Network Poller

Go Runtime은 네트워크 I/O를 효율적으로 처리하기 위해 Network Poller를 사용합니다.

```
Goroutine → Network I/O 대기 → Runtime Netpoller 등록
                                        ↓
                              다른 Goroutine 실행
                                        ↓
                           I/O 준비 완료 시 해당 Goroutine 재개
```

### 계층별 메커니즘 실체 — Network Poller

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **epoll 기반 Netpoller** | Go Runtime + Kernel | Go Netpoller는 Linux `epoll_create1()` / `epoll_ctl()` / `epoll_wait()` 시스템 콜 래핑. 네트워크 FD를 Non-blocking 모드로 등록 → I/O 준비 시 Goroutine을 `_Grunnable`로 전환 | `strace -e epoll_create1,epoll_ctl,epoll_wait`, `/proc/PID/fdinfo`, `ss -s` |
| **Socket Buffer (sk_buff)** | Kernel | NIC에서 수신된 패킷이 `sk_buff` 구조체로 소켓 수신 버퍼에 적재 → `epoll` 이벤트 발생 → Netpoller가 감지 → 대기 Goroutine Wake-up. 소켓 버퍼 오버플로우 시 패킷 드롭 | `ss -tmn` (수신 버퍼 크기), `netstat -s | grep "receive errors"`, `cat /proc/net/sockstat` |
| **TCP Backlog / SYN Queue** | Kernel | 대규모 동시 연결 요청 시 `SYN Queue`(미완성) / `Accept Queue`(완성) 오버플로우 → Go HTTP 서버의 `net.Listener` Accept 처리 지연 → Goroutine 생성 지연 | `ss -lnt` (Recv-Q/Send-Q 확인), `/proc/sys/net/core/somaxconn`, `netstat -s | grep "SYNs to LISTEN"` |
| **RPS / RFS** | Kernel | NIC 다중 큐에서 수신 패킷을 CPU 코어에 분산(RPS). RFS는 패킷을 처리 애플리케이션이 실행 중인 CPU로 유도 → Goroutine이 실행 중인 CPU와 패킷 처리 CPU 일치 시 Cache Hit 향상 | `/sys/class/net/eth0/queues/rx-*/rps_cpus`, `ethtool -S eth0`, `cat /proc/net/rps_sock_flow_entries` |
| **Connection Pool Exhaustion** | App | Go HTTP 클라이언트의 `http.Transport` 내 `MaxIdleConnsPerHost` 초과 시 새 연결 생성 필요 → 연결 대기 Goroutine 누적 → Backpressure 없으면 Goroutine 폭증 | `pprof goroutine`의 `dialContext` 스택, `net/http/pprof`, Prometheus `go_goroutines` |
| **Serialization / Deserialization 비용** | App + Go Runtime | JSON `encoding/json` 사용 시 리플렉션 기반 처리로 CPU 사용률 급증 → GC 압박 → Goroutine STW 일시 증가. `protobuf` / `jsoniter` / `sonic` 등으로 완화 | `go tool pprof cpu` (`encoding/json` 스택 비율 확인), `benchstat`, `perf stat -e instructions` |

### Work Stealing

각 P는 Local Run Queue를 가집니다. 실행할 Go Routine이 없는 P는 다른 P의 Queue에서 Go Routine을 가져와 실행합니다.

```
P1 Queue: G1, G2, G3
P2 Queue: empty
→ P2가 P1에서 일부 G를 가져와 실행
```

### 계층별 메커니즘 실체 — Work Stealing

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **Lock-free Steal** | Go Runtime | `runtime.runqsteal()`: 피해자 P의 Local Queue 후반부 절반을 원자적 CAS 연산으로 가져옴. Lock 없이 구현되어 오버헤드 최소화 | `GODEBUG=schedtrace=1000`의 `steal` 카운터, `go tool trace`의 work steal 이벤트 |
| **Global Queue 참조** | Go Runtime | 61번의 Local Queue 스케줄마다 1회 Global Run Queue 참조 (공정성 보장). GRQ Lock 경합 발생 시 스케줄 지연 | `GODEBUG=schedtrace=1000`의 `runqueue` 항목, `go tool trace` |
| **Spinning M** | Go Runtime | Idle M이 새 G를 탐색하며 CPU를 일정 시간 점유(`spinning` 상태). CPU 사용률이 낮아 보이지만 실제로는 탐색 중 | `GODEBUG=schedtrace=1000`의 `spinning` 항목, `perf stat -e cpu-clock` |
| **NUMA 영향** | Hardware | Work Stealing으로 G가 다른 NUMA 노드의 P로 이동할 경우 이전 노드에서 할당된 메모리에 원격 접근(Remote NUMA) → 레이턴시 수배 증가 | `numastat -p PID`, `perf stat -e node-load-misses`, `numactl --cpunodebind=0 --membind=0` |
| **Cache Line Thrashing** | Hardware | Work Stealing 과정에서 여러 P가 동일 `runq` 배열 끝 부분을 동시에 접근 → False Sharing 발생 가능. Go Runtime은 캐시 정렬을 통해 완화 | `perf c2c`, `perf stat -e LLC-load-misses`, `cachegrind` |
| **Branch Misprediction** | Hardware | `runtime.schedule()` 내 `runqsteal` 여부, `timer` 처리 여부 등 분기가 빈번. Goroutine 수와 P 수에 따라 분기 패턴이 달라 예측 실패율 변동 | `perf stat -e branch-misses,branch-instructions` |

---

## 7. Go Routine 과다 생성 시 주의점

Go Routine은 가볍지만, 무제한 생성해도 안전하다는 의미는 아닙니다.

| 문제 | 설명 |
|------|------|
| 메모리 증가 | 각 Go Routine도 Stack과 Metadata 필요 |
| Scheduling Overhead | Runtime이 관리할 G 수 증가 |
| Channel 대기 누적 | 대기 중인 실행 흐름 증가 |
| GC 부담 | Stack 및 참조 스캔 대상 증가 |
| Backpressure 부족 | 작업 유입 제어 실패 |

### 계층별 메커니즘 실체 — 과다 생성 시 장애 경로

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **Heap 메모리 압박** | Go Runtime + Kernel | 각 Goroutine Stack(초기 2~8KB) + G 구조체 Heap 할당. 100만 Goroutine 시 수 GB 소비 가능. Kernel OOM Killer 발동 위험 | `runtime.ReadMemStats().StackSys`, `dmesg | grep oom`, `/proc/pressure/memory` |
| **GC Stop-The-World** | Go Runtime | Goroutine 수 증가 → Heap 스캔 대상 증가 → GC Mark 단계 연장 → STW(Stop-The-World) Pause 증가 → 전체 서비스 응답 지연 | `-gcflags="-m"`, `-Xlog:gc*` 상당 → `GODEBUG=gctrace=1`, `go tool pprof gc` |
| **GC Stack Scan** | Go Runtime | GC는 각 Goroutine의 Stack을 스캔하여 살아있는 포인터 추적. Goroutine 수 비례로 Scan 비용 증가. `_Gscan` 상태로 해당 G 일시 중단 | `GODEBUG=gctrace=1` (scan 시간 관찰), `go tool trace`의 GC 이벤트 |
| **Dirty Page Writeback** | Kernel | Stack Heap 증가로 Page Cache 압박 → Kernel `pdflush`/`writeback` 스레드가 Dirty Page를 디스크에 기록 → I/O 지연 발생 | `/proc/vmstat`의 `nr_dirty`, `nr_writeback`, `iotop`, `vmstat -d` |
| **Backpressure 미적용** | App | Channel 생산자-소비자 불균형 시 Goroutine이 Channel에서 무한 대기 → Goroutine Leak. `context.Context` 취소 미처리, `select` 없는 Channel 블로킹이 주요 원인 | `pprof goroutine`의 `chan receive` 스택, `go tool trace`, `goleak` 라이브러리 |
| **Retry Storm** | App | 외부 서비스 장애 시 Goroutine이 즉시 재시도 반복 → Goroutine 폭증 → 자기 자신도 OOM 위험. Exponential Backoff + Jitter 필수 | `pprof goroutine` 수 급증 관찰, Prometheus `go_goroutines` 알람 설정 |
| **Circuit Breaker** | App | 다운스트림 장애 시 Circuit Breaker 미적용이면 대기 Goroutine 무한 누적. `hystrix-go`, `gobreaker`, `resilience4go` 등으로 차단 필요 | Prometheus circuit breaker state metric, `pprof goroutine`의 dial/request 스택 비율 |
| **PSI (Pressure Stall Information)** | Kernel | Goroutine 증가 → 메모리/CPU 압박 → PSI 수치 상승. `full` 수치(전체 task 정지) 증가는 심각 장애 전조 | `/proc/pressure/cpu`, `/proc/pressure/memory`, `/proc/pressure/io`, Kubernetes PSI 기반 자원 부족 감지 |

---

## 8. Java Thread, Virtual Thread, Go Routine 비교

| 항목 | Java Platform Thread | Java Virtual Thread | Go Routine |
|------|---------------------|---------------------|------------|
| Runtime | JVM | JVM | Go Runtime |
| OS Thread 매핑 | 1:1 | M:N 유사 구조 | M:N |
| OS Thread | Thread마다 필요 | Carrier Thread 공유 | M 공유 |
| 생성 비용 | 높음 | 낮음 | 낮음 |
| 스케줄러 | OS Kernel 중심 | JVM 중심 | Go Runtime 중심 |
| 주 사용 목적 | 일반 Thread 실행 | 대량 동시성 | 대량 동시성 |

### 실행 계층 비교

```
Java Platform Thread        Java Virtual Thread         Go Routine

  Java Thread                Virtual Thread              Goroutine
       ↓                           ↓                        ↓
      JVM                    JVM Scheduler            Go Runtime Scheduler
       ↓                           ↓                        ↓
   OS Thread                 Carrier Thread              OS Thread (M)
       ↓                           ↓                        ↓
Kernel Scheduler            OS Thread                Kernel Scheduler
       ↓                           ↓                        ↓
      CPU                   Kernel Scheduler               CPU
                                   ↓
                                  CPU
```

### 계층별 메커니즘 실체 — 세 모델 비교

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **Java Platform Thread** | JVM + Kernel | `Thread.start()` → JVM이 `pthread_create()` → `clone(CLONE_THREAD)` → `task_struct` 생성. Kernel CFS가 직접 스케줄. Thread 수 = OS Thread 수 | `jstack`, `/proc/PID/status`의 `Threads`, `pidstat -t` |
| **Java Virtual Thread** | JVM (Loom) | `Thread.ofVirtual().start()` → JVM ForkJoinPool의 Carrier Thread 위에서 실행. `Continuation` 객체로 실행 상태 저장. Blocking 시 Carrier Thread에서 Unmount → 다른 Virtual Thread Mount | `jstack` (virtual thread 구분), `jcmd PID Thread.dump_to_file`, `-Djdk.tracePinnedThreads=full` |
| **Go Goroutine** | Go Runtime | `go func()` → `runtime.newproc()` → G 구조체 할당 → P Local Queue enqueue. M 수는 GOMAXPROCS + Blocking 보정. Go Runtime이 G-P-M 관리 | `pprof goroutine`, `GODEBUG=schedtrace=1000`, `go tool trace` |
| **Pinning 문제 (Virtual Thread)** | JVM | Virtual Thread가 `synchronized` 블록 또는 JNI 코드 진입 시 Carrier Thread에 고정(Pinning) → M:N 이점 소실 → Java 21에서 일부 해소, `ReentrantLock` 권장 | `-Djdk.tracePinnedThreads=full`, `jstack`의 `PINNED` 표시 |
| **Safepoint 영향** | JVM | Java Virtual Thread / Platform Thread 모두 JVM Safepoint(GC, Deoptimization)에서 전체 정지. Go는 Goroutine별 Async Preemption으로 Safepoint 분산. GC 전략 차이 | `-XX:+PrintSafepointStatistics` (Java), `GODEBUG=gctrace=1` (Go), `go tool trace` |
| **메모리 오버헤드 비교** | Hardware | Java Thread: ~1MB Stack(OS mmap). Java Virtual Thread: 수십KB Heap. Go Goroutine: 2~8KB Heap 초기 Stack. 10만 동시 실행 흐름 기준 메모리 차이 수십 배 | `pmap PID` (Java), `/proc/PID/smaps` 비교, `runtime.ReadMemStats()` (Go) |

---

## 9. 운영(SRE) 관점

### Java Platform Thread vs Go Routine 관찰 비교

| 관점 | Java Platform Thread | Go Routine |
|------|---------------------|------------|
| 실행 흐름 수 | Thread Count | Goroutine Count |
| OS Thread 수 | Thread 수와 유사 | Runtime이 제한적으로 관리 |
| 주요 병목 | Context Switch, Thread 수 | Blocking, Scheduler 지연, GC, Channel 대기 |
| CPU 실행 | Kernel Scheduler 중심 | Runtime Scheduler + Kernel Scheduler |

> Go Routine이 많다고 해서 OS Thread가 같은 수만큼 생성되는 것이 아닙니다. Java Platform Thread와 동일한 기준으로 관찰하면 안 됩니다.

### 주요 모니터링 지표

| 지표 | 의미 |
|------|------|
| Goroutine Count | 현재 Go Routine 수 |
| OS Thread Count | Runtime이 사용하는 OS Thread 수 |
| GOMAXPROCS | 동시에 실행 가능한 P 수 |
| Scheduler Latency | Runtime Scheduling 지연 |
| GC Pause | Go GC 정지 시간 |
| Block Profile | Blocking 지점 |
| Mutex Profile | Lock 경합 |
| CPU Profile | CPU 사용 위치 |

### 계층별 메커니즘 실체 — SRE 장애 분석

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **Goroutine Leak 탐지** | Go Runtime | 장애 상황에서 `go_goroutines` 지표가 완만히 증가하다 급증 → Channel 수신 대기 / Context 취소 미처리 / defer 누락 등이 원인. 프로파일로 어느 함수에서 누수 발생하는지 특정 | `pprof goroutine?debug=2`, `goleak` (테스트용), Prometheus `go_goroutines` 알람 |
| **Scheduler Latency 분석** | Go Runtime | G가 Runnable 상태로 P Local Queue에 있으나 실행되지 않는 시간. GOMAXPROCS 부족, GC STW, Blocking M 과다가 원인 | `go tool trace`의 scheduler latency histogram, `GODEBUG=schedtrace=1000` |
| **Off-CPU 병목 탐지** | Kernel + Go Runtime | Goroutine이 Mutex / Channel / Syscall 대기 중 CPU 미점유. 전통 CPU 프로파일에 보이지 않아 Off-CPU 분석 필수 | `offcputime-bpfcc -p PID`, `go tool trace`의 blocking 이벤트, `bpftrace -e 'kprobe:schedule*'` |
| **GC 압박 분석** | Go Runtime | Goroutine 폭증 → Heap 증가 → GC 빈도 증가 → STW Pause → 응답 지연 → 더 많은 요청 재시도 → 악순환. GC 튜닝(`GOGC`, `GOMEMLIMIT`)으로 완화 | `GODEBUG=gctrace=1`, `runtime.ReadMemStats()`, `go tool pprof -alloc_space`, Prometheus `go_gc_duration_seconds` |
| **cgroup Throttling 탐지** | Kernel | Container 환경에서 CPU Limit 설정 시 Go Runtime GOMAXPROCS가 cgroup을 인식 못해 초과 설정 → Throttling 발생. `uber-go/automaxprocs`로 자동 조정 | `/sys/fs/cgroup/cpu.stat`의 `nr_throttled`, `throttled_usec`, Prometheus `container_cpu_cfs_throttled_seconds_total` |
| **NUMA 편향 탐지** | Hardware + Kernel | Go Runtime이 NUMA 토폴로지를 인식하지 않아 Goroutine이 원격 NUMA 노드 메모리 접근 → 레이턴시 편차 증가. NUMA-aware 배포 또는 `numactl` 바인딩으로 완화 | `numastat -p PID`, `perf stat -e node-load-misses`, `numactl --cpunodebind=0 --membind=0 ./app` |
| **Mutex Profile 분석** | Go Runtime | Go의 `sync.Mutex` / `sync.RWMutex` 경합 지점 특정. `runtime/pprof`의 mutex profile 활성화 필요 | `runtime.SetMutexProfileFraction(1)`, `pprof mutex`, `go tool pprof -mutex` |

---

## 10. 전체 계층 구조

```
Go Application
      ↓
Goroutine 생성 (runtime.newproc)
      ↓
Go Runtime Scheduler
      ↓
  G-P-M Model
  (G: Goroutine, P: Processor / Local Run Queue, M: OS Thread)
      ↓
   OS Thread (task_struct / CFS Runqueue)
      ↓
Kernel Scheduler (CFS, vruntime, Red-Black Tree)
      ↓
  Logical CPU (Hyper-Threading)
      ↓
Hardware Thread
      ↓
Physical Core
      ↓
ALU / LSU / Register / L1-L2 Cache / Pipeline / Branch Predictor
```

### 계층별 메커니즘 실체 — 전체 경로

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|------|--------|--------------|---------------------|
| **Go Application → G 생성** | Go Runtime | `runtime.newproc()` → G 구조체 Heap 할당 → P Local Queue enqueue → 필요 시 M Wakeup | `pprof goroutine`, `runtime.NumGoroutine()` |
| **G → P → M 바인딩** | Go Runtime | `runtime.schedule()`: P의 Local Queue → Global Queue → Netpoller → Work Steal 순으로 G 탐색 → M이 G 실행 | `GODEBUG=schedtrace=1000`, `go tool trace` |
| **M → Kernel Scheduler** | Kernel | M(`task_struct`)이 CFS Runqueue에서 vruntime 기반 스케줄 → Logical CPU 할당 | `/proc/schedstat`, `perf sched`, `pidstat -w` |
| **Logical CPU → Physical Core** | Hardware | Hyper-Threading: 한 Physical Core의 두 Logical CPU가 Pipeline 자원 공유 → 동시 실행 시 IPC 저하 가능 | `lscpu | grep "Thread(s) per core"`, `perf stat -e instructions,cycles` |
| **Pipeline / Cache** | Hardware | G 실행 코드가 L1 Instruction Cache에 적재. 잦은 Goroutine 전환으로 I-Cache Eviction → Pipeline Stall 증가 | `perf stat -e L1-icache-load-misses`, `perf stat -e stalled-cycles-frontend` |

---

## 11. 핵심 정리

| 구성 요소 | 역할 |
|-----------|------|
| Go Routine | Go Runtime이 관리하는 경량 실행 흐름 |
| Go Runtime | Go 실행 흐름, GC, Scheduler 관리 |
| G | Goroutine 실행 단위 (PC/SP/Stack/상태 포함) |
| P | Scheduling Context (Lock-free Local Run Queue 보유) |
| M | OS Thread (task_struct에 대응) |
| M:N Mapping | 많은 Goroutine을 적은 OS Thread에 매핑, Blocking syscall 시 M 분리 |
| Work Stealing | 빈 P가 다른 P의 Local Queue 후반부를 CAS로 가져오는 방식 |
| GOMAXPROCS | 동시에 Go 코드를 실행할 수 있는 P 수 (cgroup 인식 주의) |
| Netpoller | epoll 기반 Network I/O 대기 관리, Goroutine 비동기 재개 |
| Async Preemption | SIGURG 기반 Goroutine 강제 전환 (Go 1.14+) |

### 결론

```
Goroutine = Go Runtime 내부 실행 단위 + M:N Scheduler 대상 + OS Thread보다 가벼운 실행 흐름
```

Go Routine은 OS Thread를 직접 대량 생성하지 않고, Go Runtime Scheduler가 많은 Go Routine을 적은 수의 OS Thread 위에서 실행하도록 관리합니다.

```
Many Goroutines → Go Runtime Scheduler (G-P-M) → Few OS Threads (task_struct) → Kernel CFS → Physical CPU
```

Go Routine의 경량성은 단순히 "작다"는 것이 아니라, **OS Kernel 개입 없는 전환**, **동적 Stack 확장**, **epoll 기반 비동기 I/O**, **Work Stealing 기반 부하 분산**이 결합된 전 계층 설계의 결과입니다. SRE 관점에서는 Goroutine 수, GC Pause, Off-CPU Time, cgroup Throttling, NUMA 접근 편향을 함께 관찰해야 실제 병목을 정확히 진단할 수 있습니다.

---

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*