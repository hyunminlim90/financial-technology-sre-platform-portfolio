# CPU Saturation과 I/O 처리 모델 (E2E 분석 적용됨)

## 개요

**CPU Saturation**은 단순히 CPU 사용률이 높은 상태가 아닙니다.  
CPU가 실제로 처리할 수 있는 용량보다 많은 `task_struct`가 실행을 요구하면서, **대기·전환·스케줄링 비용이 증가하는 상태**를 의미합니다.

이 문제는 I/O 처리 모델과 직접 연결됩니다. 핵심 질문은 다음과 같습니다.

> **I/O 대기 중에 Java Thread와 `task_struct`가 Wait Queue로 이동하는가,  
> 아니면 Running/Runnable 상태를 유지하면서 다른 작업을 계속 처리할 수 있는가?**

### 개요 계층별 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **task_struct 상태 전이** | Kernel | `task_struct`의 상태(Running → Sleeping → Runnable)가 Runqueue / Wait Queue 사이를 이동. CFS가 vruntime 기반으로 스케줄링 결정 | `/proc/PID/status`의 `State` 항목, `perf sched`, `pidstat -w` |
| **CPU Saturation 판별** | Kernel | Run Queue Length 증가 + `%iowait` + Context Switch 급증 = Saturation 진입 신호. `Load Average`는 Running + Runnable + D-state(Uninterruptible Sleep) 합산 | `vmstat 1`의 `r` 항목, `uptime`, `/proc/pressure/cpu` |
| **cgroup CPU Throttling** | Kernel | Kubernetes Pod의 CPU Limit 소진 시 cgroup v2 CFS Bandwidth Control이 `task_struct`를 Runqueue에서 강제 제거. Throttling 상태에서는 CPU 여유가 있어도 실행 불가 | `/sys/fs/cgroup/cpu.stat`의 `throttled_usec`, `cadvisor`, `kubectl top` |
| **PSI (Pressure Stall Information)** | Kernel | CPU/Memory/IO 자원 부족 압력 정량화. `some`: 일부 Task 대기, `full`: 전체 Task 대기. Saturation 조기 탐지에 활용 | `/proc/pressure/cpu`, `/proc/pressure/memory`, `/proc/pressure/io` |

---

## 1. Linux Kernel 관점: task_struct 상태

Linux Kernel에서 Java Thread는 최종적으로 `task_struct`로 실체화됩니다.  
I/O 처리 방식에 따라 `task_struct`가 다음 중 어떤 상태에 머무는지가 결정됩니다.

| 상태 | 의미 | Kernel 구조 |
|------|------|------------|
| Running | Logical CPU 위에서 실제 실행 중 | CPU Runqueue |
| Runnable | 실행 가능하지만 CPU 할당 대기 중 | CFS Red-Black Tree |
| Sleeping / Blocked | I/O, Lock, Timer 등 이벤트 대기 중 | Wait Queue |
| Uninterruptible Sleep (D) | Disk I/O 등 중단 불가 대기 | Wait Queue (신호 무시) |
| Wait Queue | Sleeping 상태 `task_struct`가 대기하는 커널 구조 | `wait_queue_head_t` |
| Runnable Queue | Runnable 상태 `task_struct`가 CPU 할당을 기다리는 구조 | `rq` 구조체 |

### task_struct 계층별 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **CFS Scheduler** | Kernel | Completely Fair Scheduler: Red-Black Tree 기반 vruntime 관리. `task_struct`마다 vruntime 누적, 가장 작은 vruntime을 가진 Task를 다음 실행 대상으로 선택 | `/proc/schedstat`, `perf sched latency`, `pidstat -w` |
| **Context Switch** | Kernel | Running → Runnable 전환 시 Register 상태를 `task_struct.thread`에 저장하고 TLB Flush(PCID 미사용 시) 발생. Context Switch 자체가 CPU Saturation 지표 | `vmstat 1`의 `cs`, `pidstat -w`의 `cswch/s + nvcswch/s` |
| **Wait Queue Wake-up** | Kernel | I/O 완료(IRQ) 또는 Futex 해제 시 `wake_up()` 호출 → `task_struct`가 Wait Queue에서 Runqueue로 이동. 대기 Thread 수가 많을수록 Wake-up 폭풍 가능 | `perf trace -e sched:sched_wakeup`, `bpftrace` |
| **D-state (Uninterruptible Sleep)** | Kernel | Disk I/O 대기, NFS 응답 대기 등에서 발생. SIGKILL도 처리 불가. Load Average를 높이는 주범. 장시간 지속 시 Kernel 버그 또는 I/O 서브시스템 문제 | `ps aux | grep ' D '`, `cat /proc/PID/wchan`, `dmesg` |
| **Off-CPU Time** | Kernel | `task_struct`가 Runqueue에 없는 시간. I/O, Lock, Futex 대기 포함. On-CPU 프로파일링만으로는 탐지 불가 | `offcputime-bpfcc -p PID`, `perf sched latency`, `async-profiler` (wall-clock mode) |
| **OOM Killer** | Kernel | 메모리 부족 시 `oom_badness()` 점수 기반으로 `task_struct` 강제 종료. oom_score_adj 값이 높은 Process가 우선 대상 | `dmesg | grep oom-killer`, `/proc/PID/oom_score`, `oom_score_adj` |

---

## 2. Blocking I/O

### 개념

I/O 작업이 완료될 때까지 현재 Thread가 다음 코드를 실행하지 못하고 대기하는 방식입니다.

**대표 구현체**
- `java.io.InputStream`, `java.io.FileInputStream`
- `Socket.getInputStream()`
- JDBC 기반 DB 호출
- Blocking 방식 외부 API Client

### Kernel 흐름

```
Java Thread
  → read() / write() / JDBC Query 호출
  → syscall (User Mode → Kernel Mode, Ring 3 → Ring 0)
  → 데이터 미준비 또는 응답 대기 필요
  → task_struct.state = TASK_INTERRUPTIBLE / TASK_UNINTERRUPTIBLE
  → Wait Queue 이동
  → I/O 완료 → IRQ → Wake-up → Runnable Queue 복귀
  → CFS가 CPU 재할당
```

### Thread Pool에 미치는 영향

Blocking I/O 중인 Thread는 CPU를 사용하지 않더라도, **Java Thread Pool에 반환되지 않습니다.**

```
Blocking I/O 진행 중
  → Kernel: task_struct는 Wait Queue에 있음
  → Application: Java Thread는 요청 처리 중 상태 유지
  → Thread Pool 반환 불가
  → 신규 요청 → Thread Pool 고갈 → RejectedExecutionException
```

### Blocking I/O 계층별 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **시스템 콜 (read/write)** | App → Kernel | `read()` / `write()` syscall 진입 시 User Register 저장(`pt_regs`). 데이터 미준비 시 `task_struct`를 Wait Queue에 등록하고 `schedule()` 호출 | `strace -e read,write -p PID`, `perf trace`, `/proc/PID/syscall` |
| **Socket Buffer (sk_buff)** | Kernel | 네트워크 Blocking read 시 Kernel이 `sk_buff`(소켓 수신 버퍼)에 데이터 도착을 기다림. `SO_RCVBUF` 크기가 부족하면 패킷 드롭 발생 | `ss -tmn`의 `Recv-Q`, `netstat -s | grep buffer`, `/proc/net/sockstat` |
| **TCP Backlog / SYN Queue** | Kernel | Blocking Accept 서버에서 SYN Queue(`net.ipv4.tcp_max_syn_backlog`)와 Accept Queue(`backlog`)가 포화되면 신규 연결 거절. Blocking Accept Thread 수가 부족할 때 발생 | `ss -lnt`의 `Recv-Q`, `netstat -s | grep 'SYNs to LISTEN'`, `dmesg | grep 'TCP: drop open request'` |
| **IO Scheduler (blk-mq)** | Kernel | 파일/Disk Blocking I/O는 `blk-mq`(Multi-Queue Block Layer)를 통해 처리. I/O 요청이 큐에 쌓이면 `task_struct`는 D-state 진입 | `iostat -x 1`의 `await`, `%util`, `cat /sys/block/sda/queue/scheduler`, `blktrace` |
| **Dirty Page Writeback** | Kernel | Blocking write 후 Page Cache에 Dirty Page가 쌓이면 `vm.dirty_ratio` 초과 시 Write Stall 발생. 애플리케이션 Thread가 Writeback 완료를 기다리며 Block | `/proc/vmstat`의 `nr_dirty`, `sar -b`, `/proc/sys/vm/dirty_ratio` |
| **Futex (Lock 경합)** | OS / App | Java `synchronized`, `ReentrantLock` 경합 시 `futex_wait()` syscall → `task_struct` Wait Queue 진입. Lock 해제 시 `futex_wake()` → Wake-up | `perf trace -e futex`, `/proc/PID/status`의 `nonvoluntary_ctxt_switches`, `jstack` (BLOCKED 상태) |
| **Connection Pool Exhaustion** | App | JDBC Connection Pool 고갈 시 Thread가 Connection 획득 대기 → Blocking. HikariCP 기준 `connectionTimeout` 초과 시 Exception | `HikariCP metrics`의 `hikaricp.connections.pending`, `jstack`의 `WAITING` Thread, `actuator/metrics` |
| **JIT C1/C2와 Blocking** | JVM Runtime | Blocking I/O가 많은 Hot Path는 C2 JIT가 Compile하더라도 syscall 빈도를 줄이지 못함. JIT 최적화 효과가 I/O Latency보다 작음 | `-XX:+PrintCompilation`, `async-profiler` (syscall 비율 확인) |
| **CPU Frequency Scaling** | Hardware + Kernel | Blocking I/O 대기 중 CPU가 C-state(Deep Sleep)에 진입. 재요청 시 복귀 Latency(수백 μs) 발생. 지연 민감 서비스에서 C-state 제한 필요 | `cpupower idle-info`, `turbostat`, `/sys/devices/system/cpu/cpu*/cpuidle/` |

---

## 3. epoll 기반 Non-blocking I/O

### 개념

epoll은 Linux Kernel이 제공하는 **이벤트 기반 I/O 통지 메커니즘**입니다.  
애플리케이션은 관심 있는 File Descriptor(FD)를 Kernel에 등록하고, Kernel은 해당 FD에 이벤트가 발생하면 Event Loop에 알립니다.

**대표 구현체**
- Java NIO Selector
- Netty epoll transport
- Spring WebFlux (내부 Netty 기반)
- WebClient 비동기 네트워크 I/O

### FD 등록 흐름

```
Socket 생성
  → FD 할당 (O_NONBLOCK 설정)
  → epoll_ctl(EPOLL_CTL_ADD)로 관심 이벤트 등록
  → epoll_wait()로 준비된 이벤트 확인 (timeout 설정)
  → Ready List에서 이벤트 수신
  → Handler 실행 후 다음 epoll_wait()로 복귀
```

### Kernel 흐름

```
Event Loop Thread
  → FD를 epoll 관심 목록에 등록
  → epoll_wait() 호출 → task_struct는 Wait Queue에서 대기
  → Network Packet 도착
  → NIC DMA 전송 완료 → IRQ 발생
  → SoftIRQ: TCP/IP Stack 처리 → sk_buff 수신 버퍼 갱신
  → epoll Ready List 갱신 → task_struct Wake-up
  → Event Loop가 이벤트 수신 → Handler 실행
```

요청마다 Thread가 대기하지 않고, **소수의 Event Loop Thread가 다수의 연결을 처리**합니다.

### epoll의 한계

epoll은 네트워크 소켓 I/O에는 적합하지만, **일반 파일/디스크 I/O에는 제한**이 있습니다.

```
epoll이 파일 FD를 Ready로 판단
  → read() 호출
  → Page Cache Miss
  → Disk I/O 발생 (blk-mq 큐 진입)
  → Task Block 가능 (D-state 진입)
  → Event Loop Thread 자체가 Blocking
```

### epoll 계층별 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **IRQ / SoftIRQ와 epoll** | Hardware → Kernel | NIC 패킷 수신 완료 시 IRQ 발생 → `ksoftirqd`가 TCP/IP Stack 처리 → `sk_buff`를 소켓 수신 버퍼에 적재 → epoll Ready List 갱신 | `/proc/interrupts`, `mpstat`의 `%soft`, `/proc/softirqs` |
| **RPS / RFS** | Kernel | Receive Packet Steering: NIC 인터럽트를 여러 CPU에 분산. Receive Flow Steering: 동일 Flow를 동일 CPU로 유도 → CPU Cache 재사용 향상. epoll Event Loop와 결합 시 효율 극대화 | `/sys/class/net/ethX/queues/rx-N/rps_cpus`, `/proc/net/softnet_stat`, `ethtool -l` |
| **Socket Buffer (sk_buff)** | Kernel | epoll ET(Edge-Triggered) 모드에서 소켓 버퍼를 완전히 비우지 않으면 다음 이벤트 누락. `EWOULDBLOCK` 수신까지 반복 read 필요 | `ss -tmn`의 `Recv-Q/Send-Q`, `netstat -s | grep 'buffer errors'` |
| **epoll_wait와 Wakeup 폭풍** | Kernel | 다수의 FD가 동시에 Ready 상태가 되면 epoll_wait 반환이 지연되거나 Event Loop가 처리 부하를 받음. `EPOLLONESHOT` 또는 `EPOLLET`로 완화 | `perf trace -e syscalls:sys_enter_epoll_wait`, `strace -e epoll_wait` |
| **TCP Backlog / SYN Queue** | Kernel | epoll 기반 Accept Loop에서도 SYN Queue 포화 시 신규 연결 거절. `net.core.somaxconn`, `net.ipv4.tcp_max_syn_backlog` 조정 필요 | `ss -lnt`의 `Recv-Q`, `netstat -s | grep 'SYNs to LISTEN'` |
| **Page Cache / Major Fault** | Kernel | epoll로 파일 FD를 감시하더라도 Page Cache Miss 시 `read()` 내부에서 Major Page Fault → D-state 진입 → Event Loop Thread Block | `vmstat`의 `pgmajfault`, `/proc/vmstat`, `perf stat -e major-faults` |
| **Backpressure** | App | Event Loop 처리 속도보다 이벤트 도착 속도가 빠를 때 Queue 포화 → Backpressure 필요. Reactor 기반에서 `onBackpressureDrop()` / `onBackpressureBuffer()` 전략 선택 | `reactor.netty` metrics, `Micrometer executor.queued`, `perf trace` |
| **Circuit Breaker** | App | 하위 서비스 장애 시 Event Loop Thread가 응답 대기로 묶이는 것을 방지. Resilience4j Circuit Breaker가 빠른 실패(Fast Fail)로 Thread 자원 보호 | `Resilience4j metrics`의 `circuitbreaker.state`, `/actuator/health` |

---

## 4. io_uring

### 개념

io_uring은 Linux Kernel의 **고성능 비동기 I/O 인터페이스**입니다.  
기존 epoll/read/write 방식보다 시스템 콜 횟수와 User Mode / Kernel Mode 전환 비용을 줄이기 위해 설계되었습니다.

io_uring은 **공유 Ring Buffer** 구조를 사용합니다.

| Queue | 역할 | 구조 |
|-------|------|------|
| Submission Queue (SQ) | 애플리케이션이 Kernel에 요청할 I/O 작업을 등록 | User Space에서 직접 쓰기 |
| Completion Queue (CQ) | Kernel이 완료된 I/O 결과를 기록 | User Space에서 직접 읽기 |

### 기본 흐름

```
Application Thread
  → SQ에 I/O 요청 등록 (mmap 공유 영역, syscall 없이 가능)
  → io_uring_enter() 또는 SQPOLL 모드에서 자동 처리
  → Kernel이 요청을 비동기로 처리 (blk-mq / 네트워크 스택)
  → CQ에 완료 결과 기록
  → Application이 CQ에서 결과 확인 (syscall 없이 가능)
```

### SQPOLL 모드

```
Application Thread
  → SQ에 작업 기록 (syscall 없음)
  → Kernel Polling Thread(IORING_SETUP_SQPOLL)가 SQ 지속 감시
  → I/O 작업 수행
  → CQ에 결과 기록
  → Application이 CQ Poll (syscall 없음)
```

### io_uring 계층별 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **Ring Buffer (SQ/CQ)** | App ↔ Kernel | SQ/CQ Ring Buffer를 `mmap`으로 공유. `io_uring_enter()` 최소화 또는 SQPOLL로 시스템 콜 횟수를 대폭 감소 | `io_uring_setup` syscall 추적, `/proc/PID/fdinfo`의 uring 항목, `strace -e io_uring_enter` |
| **blk-mq와 io_uring** | Kernel | 파일/Disk I/O 요청이 `blk-mq` 큐에 직접 제출됨. epoll과 달리 Disk I/O도 비동기 처리 가능. D-state 진입 없이 완료 통보를 CQ로 수신 | `iostat -x 1`의 `await`, `blktrace`, `cat /sys/block/*/queue/scheduler` |
| **NIC Ring Buffer** | Hardware + Kernel | io_uring과 별개로 NIC 자체의 Rx/Tx Ring Buffer가 패킷 수신/송신에 사용. `ethtool -G`로 Ring 크기 조정 가능 | `ethtool -S ethX`의 `rx_dropped`, `ethtool -G ethX rx N` |
| **IRQ / SoftIRQ와 io_uring** | Hardware → Kernel | 네트워크 요청의 경우 NIC IRQ → SoftIRQ → TCP/IP Stack → CQ 완료 기록 흐름. CPU Affinity로 특정 Core에 SoftIRQ 집중 방지 | `mpstat`의 `%soft`, `/proc/softirqs`, `irqbalance` |
| **io_uring과 eBPF** | Kernel | eBPF 프로그램이 io_uring 이벤트에 훅하여 I/O 패턴 분석 가능. 애플리케이션 무수정 커널 관찰 | `bpftrace`, `bpftool prog`, `Pixie` |
| **NUMA와 io_uring** | Hardware + Kernel | SQPOLL 모드의 Kernel Thread가 특정 NUMA 노드에서만 실행될 때, 원격 노드 메모리 접근 → Latency 증가. `IORING_SETUP_SQ_AFF`로 CPU Affinity 설정 | `numastat`, `numactl --hardware`, `perf mem` |
| **Retry Storm** | App | io_uring 기반 클라이언트도 상위 서비스 장애 시 CQ 완료 없이 타임아웃 → 재시도 폭증 가능. Exponential Backoff + Jitter 필수 | `CQ 완료 지연 지표`, `perf trace -e io_uring_enter`, access log 분석 |
| **Direct Memory / Off-Heap** | App / JVM | io_uring의 Fixed Buffer 기능으로 User Space 버퍼를 Kernel에 사전 등록(`io_uring_register`). Zero-Copy 전송 가능. JVM에서는 `ByteBuffer.allocateDirect()` 활용 | `NativeMemoryTracking`, `/proc/PID/status`의 `VmRSS`, `perf mem` |

### io_uring의 장점 비교

| I/O 종류 | Blocking I/O | epoll | io_uring |
|----------|-------------|-------|---------|
| Network Socket I/O | Blocking | 적합 | 적합 |
| File / Disk I/O | Blocking | 제한적 | 적합 |
| 시스템 콜 비용 감소 | 없음 | 중간 | 강함 (SQPOLL 시 거의 없음) |
| Zero-Copy 지원 | 없음 | 없음 | Fixed Buffer로 가능 |
| D-state 회피 | 불가 | 불가 | 가능 |

---

## 5. Blocking I/O / epoll / io_uring 비교

| 구분 | Blocking I/O | epoll | io_uring |
|------|-------------|-------|---------|
| 대표 구현 | `java.io`, JDBC | Java NIO, Netty epoll | Netty io_uring transport |
| Kernel 대화 방식 | read/write 직접 호출 | FD 이벤트 감시 (`epoll_wait`) | SQ/CQ Ring Buffer 기반 비동기 |
| I/O 대기 중 task_struct 상태 | Wait Queue로 이동 | epoll_wait Wait Queue 대기 | Runnable 유지에 유리 (SQPOLL) |
| 네트워크 I/O | 가능하지만 Blocking | 적합 | 적합 |
| 파일/디스크 I/O | Blocking (D-state 가능) | 제한적 (Page Fault 시 Block) | 적합 (비동기 처리 가능) |
| 시스템 콜 비용 | 높음 | 중간 | 낮음 |
| Context Switch 발생 | 높음 | 낮음 | 최소 |
| 필요한 Thread 수 | 증가하기 쉬움 | 적게 유지 가능 | 적게 유지 가능 |
| SoftIRQ 부하 | 공통 | 공통 | 공통 |
| Zero-Copy 가능 여부 | 없음 | 없음 | Fixed Buffer로 가능 |

### 비교 계층별 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **syscall 비용 차이** | App → Kernel | Blocking: 매 I/O마다 syscall. epoll: `epoll_wait` 1회로 다수 FD 처리. io_uring: SQPOLL 시 syscall 거의 없음 | `perf stat -e syscalls:*`, `strace -c`, `perf trace --summary` |
| **Context Switch 차이** | Kernel | Blocking: Thread 수 × I/O 대기마다 Context Switch. epoll/io_uring: Event Loop Thread 수 × 이벤트 처리만 전환 | `vmstat 1`의 `cs`, `pidstat -w` |
| **Cache 효율 차이** | Hardware | Blocking Thread 다수 시 Context Switch → Cache 무효화 반복. Event Loop 소수 유지 시 L1/L2 Cache Warm 상태 유지 가능 | `perf stat -e cache-misses`, `perf c2c` |
| **Serialization / Deserialization 비용** | App / JVM | 모든 모델에서 JSON/Protobuf 처리 시 메모리 접근 패턴이 비연속 → Cache Miss 증가. Non-blocking이라도 Deserialization이 Event Loop Thread를 CPU-bound로 만들 수 있음 | `async-profiler`, `perf record -g java` |

---

## 6. JDBC와 R2DBC

### JDBC

JDBC는 **Blocking 방식**으로 동작합니다.

```
Java Thread
  → JDBC Query 실행
  → TCP syscall (connect/send/recv) 호출
  → DB 응답 대기 → task_struct Wait Queue 진입
  → Thread Blocked (Thread Pool 점유 유지)
  → DB 응답 도착 → IRQ → Wake-up → Thread 재개
```

WebFlux나 Netty Event Loop 내부에서 JDBC를 직접 호출하면, **Event Loop Thread 자체가 Blocking**되어 전체 처리량이 크게 저하됩니다.

### R2DBC

R2DBC는 **Reactive Streams 기반의 Non-blocking DB 접근 모델**입니다.

```
Event Loop Thread
  → R2DBC Query 요청
  → Non-blocking TCP Socket I/O로 DB 통신 (epoll 기반)
  → Thread 점유 최소화
  → DB 응답 이벤트 수신 → sk_buff 갱신 → epoll Ready
  → Event Loop Handler 실행
```

### JDBC / R2DBC 계층별 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **JDBC Connection Pool** | App | HikariCP: Connection 획득 대기 시 `LockSupport.parkNanos()` → `futex_wait()` → `task_struct` Block. `maximumPoolSize` 초과 요청은 `connectionTimeout`까지 대기 | `HikariCP metrics`의 `hikaricp.connections.pending`, `jstack` (WAITING Thread), `perf trace -e futex` |
| **R2DBC와 epoll** | App ↔ Kernel | R2DBC는 내부적으로 Netty epoll transport를 사용하여 DB와의 TCP 통신을 비동기로 처리. DB 응답을 `sk_buff` 수신 후 epoll 이벤트로 처리 | `ss -tnp`로 DB 연결 상태 확인, `reactor.netty` metrics |
| **Connection Pool Exhaustion (R2DBC)** | App | R2DBC Pool도 `maxSize` 초과 시 연결 획득 대기 발생. Reactive 방식이지만 Pool 고갈 시 `Mono.timeout()` 내에서 실패 | `r2dbc.pool.pending` metrics, `Micrometer` |
| **Backpressure (R2DBC)** | App | DB 응답 속도 < Consumer 처리 속도 불일치 시 Reactive Streams Backpressure 발생. `Flux.limitRate()` 또는 `onBackpressureBuffer()` 전략 필요 | `reactor.netty` metrics, Zipkin Trace latency |
| **Safepoint와 JDBC** | JVM Runtime | JDBC 처리 중 GC Safepoint 진입 시 모든 Thread 정지. 다량의 Blocking Thread가 있을 때 TTSP(Time To Safepoint) 지연 증가 | `-XX:+PrintSafepointStatistics`, GC log의 `application stopped time` |
| **Off-CPU Time (JDBC)** | Kernel | JDBC Thread가 DB 응답 대기 중인 시간 전체가 Off-CPU Time. On-CPU 프로파일로는 탐지 불가. Wall-clock 기반 프로파일링 필요 | `offcputime-bpfcc -p PID`, `async-profiler -e wall` |

---

## 7. WebClient와 Netty

Spring WebClient는 **Reactor Netty 기반**으로 동작합니다.

```
WebClient
  → Reactor Netty (Event Loop Group)
  → NioEventLoop / EpollEventLoop
  → epoll_wait() / Native epoll transport
  → Linux Kernel (sk_buff, IRQ, SoftIRQ)
```

### WebClient / Netty 계층별 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **Netty Event Loop와 task_struct** | App ↔ Kernel | Netty Event Loop Thread 수 = 기본적으로 CPU 코어 수 × 2. 각 Thread는 단일 `task_struct`로 실체화. Event Loop Thread가 Block되면 해당 Loop의 모든 Channel 처리 지연 | `top -H -p PID` (reactor-http-epoll 스레드), `pidstat -w` |
| **epoll과 sk_buff** | Kernel | WebClient 응답 수신 시 NIC → IRQ → SoftIRQ → `sk_buff` → Socket 수신 버퍼 적재 → epoll Ready. `SO_RCVBUF` 크기가 작으면 수신 버퍼 포화로 패킷 드롭 | `ss -tmn`의 `Recv-Q`, `/proc/net/sockstat`의 `sockets: used` |
| **THP (Transparent HugePage)** | Kernel | 대용량 응답 처리 시 메모리 할당이 빈번해지면 THP Compaction이 짧은 Latency Spike 유발. WebFlux High Throughput 환경에서 THP `madvise` 모드 권장 | `/proc/vmstat`의 `compact_migrate_scanned`, `/sys/kernel/mm/transparent_hugepage/defrag` |
| **Blocking 호출 혼용** | App | WebClient 핸들러 내에서 JDBC, `Thread.sleep()`, `synchronized` 등 Blocking 호출 시 Event Loop Thread 자체가 `futex_wait()` 또는 D-state 진입 | `jstack`의 Event Loop Thread 상태, `async-profiler -e wall` |
| **Retry Storm** | App | WebClient 타임아웃 + 자동 재시도 설정 결합 시 하위 서비스 장애 중 재시도 폭증. Reactor `retryWhen(Retry.backoff(...))` + Circuit Breaker 필수 | access log의 요청 폭증 패턴, `resilience4j.circuitbreaker.state` |
| **Serialization / Deserialization** | App / JVM | JSON 역직렬화(Jackson) 시 대량 객체 생성 → GC 압박 → Safepoint Stop-The-World. Event Loop Thread가 Deserialization에 CPU 낭비 시 신규 이벤트 처리 지연 | `async-profiler`의 Jackson 메서드 비율, `jstat -gcutil`, GC log |
| **Connection Pool (WebClient)** | App | WebClient도 내부적으로 Netty Connection Pool 사용. `maxConnections`, `pendingAcquireMaxCount` 초과 시 `PoolAcquireTimeoutException` | `reactor.netty.connection.provider.*` metrics, `actuator/metrics` |

---

## 8. 구현 계층 정리

| 계층 | 예시 | 실제 I/O 엔진 | 주요 메커니즘 |
|------|------|-------------|------------|
| Application | WebClient, Repository, Service | 추상화된 API | Connection Pool, Backpressure, Circuit Breaker |
| Framework | Spring WebFlux, Netty, Tomcat | Event Loop 또는 Worker Pool | Reactor Scheduler, epoll transport |
| Runtime | JVM, JNI | Native I/O 호출 | JIT(C1/C2), GC, Safepoint, TLAB |
| OS Interface | epoll, io_uring, read/write | Kernel I/O 인터페이스 | syscall, Futex, blk-mq |
| Kernel | task_struct, FD, Socket Buffer | 실제 상태 관리 | CFS, Wait Queue, IRQ, SoftIRQ, Page Cache |
| Hardware | NIC, Disk, CPU | 데이터 송수신 및 연산 | DMA, IRQ, Cache, NUMA, CPU Frequency |

### 계층 전환별 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **App → OS (syscall)** | App → Kernel | `syscall` 명령어로 Ring 3 → Ring 0 전환. `pt_regs`에 User 레지스터 저장. 빈번한 syscall은 `%sy` 상승 원인 | `strace -c`, `perf stat -e syscalls:*`, `mpstat`의 `%sys` |
| **OS → Hardware (DMA)** | Kernel → Hardware | Disk/NIC 데이터 전송 시 DMA Controller가 CPU 개입 없이 메모리에 직접 쓰기. 완료 시 IRQ 발생 | `/proc/interrupts`, `iostat -x`의 `await` |
| **Hardware → Kernel (IRQ)** | Hardware → Kernel | NIC/Disk 완료 신호 → CPU 실행 중단 → IRQ Handler → SoftIRQ → `task_struct` Wake-up | `mpstat`의 `%irq`, `perf stat -e irq:*` |
| **Kernel → JVM (Signal/Exception)** | Kernel → App | SIGSEGV, SIGTERM 등을 JVM의 Signal Handler가 수신하여 JVM 내부 처리. OOM Killer 발동 시 SIGKILL | `jstack`, `dmesg | grep oom`, `/proc/PID/status`의 `SigCgt` |
| **eBPF Map** | Kernel → App | eBPF 프로그램이 I/O 이벤트에 훅하여 Kernel 내부 통계를 Map에 기록. User 레벨 도구가 읽어 분석. 애플리케이션 무수정 관찰 | `bpftool map`, `bpftrace`, `Cilium`, `/sys/fs/bpf/` |

---

## 9. 네트워크 패킷 처리 흐름

```
Packet 도착
  → NIC DMA로 Rx Ring Buffer에 기록 (CPU 개입 없음)
  → Hardware Interrupt(IRQ) 발생 → CPU 실행 중단
  → IRQ Handler: DMA 완료 확인, NAPI 스케줄 등록
  → SoftIRQ (ksoftirqd 또는 NET_RX_SOFTIRQ)
  → TCP/IP Stack 처리: ip_rcv() → tcp_rcv()
  → sk_buff를 Socket 수신 버퍼에 적재
  → epoll Ready List 갱신 / io_uring CQ 기록
  → Event Loop / Application Handler 실행
```

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **NAPI (New API)** | Kernel | 고속 네트워크에서 IRQ 대신 Polling 방식으로 패킷 수신. IRQ → NAPI Poll 등록 → SoftIRQ에서 배치 처리. IRQ 폭증 방지 | `/proc/interrupts` (IRQ 감소 확인), `ethtool -S ethX`의 `rx_packets` |
| **SoftIRQ CPU 포화** | Kernel | 트래픽 폭증 시 `ksoftirqd` Thread의 CPU 점유 급증. 단일 CPU에서 SoftIRQ 처리가 집중되면 다른 `task_struct` 실행 지연 | `mpstat -P ALL 1`의 `%soft`, `/proc/softirqs`의 `NET_RX` 누적 |
| **RPS / RFS** | Kernel | RPS: 패킷 Hash 기반으로 수신 처리를 여러 CPU에 분산. RFS: 동일 Flow를 Application Thread가 실행 중인 CPU로 유도하여 Cache 재사용 | `/sys/class/net/ethX/queues/rx-N/rps_cpus`, `/proc/net/softnet_stat`의 `dropped` |
| **Socket Buffer 포화** | Kernel | 수신 버퍼(`SO_RCVBUF`) 포화 시 패킷 드롭. 송신 버퍼(`SO_SNDBUF`) 포화 시 send() Block | `ss -tmn`의 `Recv-Q`, `netstat -s | grep 'receive buffer errors'` |
| **TCP SYN Queue 포화** | Kernel | SYN Flood 또는 Accept 처리 지연 시 SYN Queue 포화 → 신규 연결 거절. `tcp_syncookies` 활성화로 완화 | `netstat -s | grep 'SYNs to LISTEN'`, `dmesg | grep 'TCP: drop open request from'` |
| **Cache Line Thrashing** | Hardware | 다수의 CPU가 동일 `sk_buff` 또는 소켓 구조체에 동시 Write 시 MESI 무효화 연쇄. 고속 네트워크에서 CPU 코어 간 경합 | `perf c2c record && perf c2c report` |

---

## 10. 파일/디스크 I/O 처리 흐름

```
File Read 요청
  → Page Cache 조회 (Kernel 관리 메모리)
  → Cache Hit  → 즉시 반환 (Disk I/O 없음)
  → Cache Miss → blk-mq I/O 요청 제출
                  → IO Scheduler (mq-deadline / none)
                  → Disk Controller DMA
                  → 완료 IRQ → Page Cache 갱신
                  → task_struct Wake-up → 데이터 반환
```

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **Page Cache** | Kernel | 파일 데이터를 메모리에 캐시. `free -m`의 `buff/cache` 항목. Page Cache 비율이 높을수록 Disk I/O 감소 | `free -m`, `vmstat`의 `pgmajfault`, `sar -r` |
| **blk-mq (IO Scheduler)** | Kernel | Multi-Queue Block Layer: SSD/NVMe를 위한 다중 큐 I/O 스케줄러. `none` (NVMe 권장), `mq-deadline`, `kyber` 선택 가능 | `cat /sys/block/sda/queue/scheduler`, `iostat -x 1`의 `await/util`, `blktrace` |
| **Dirty Page Writeback** | Kernel | write() 후 데이터가 Page Cache에 Dirty 상태로 남아 있다가 주기적으로 Disk에 기록. `vm.dirty_ratio` / `vm.dirty_background_ratio` 설정이 Write Stall 임계에 영향 | `/proc/vmstat`의 `nr_dirty`, `sar -b`, `iostat -x`의 `wkB/s` |
| **epoll과 파일 I/O 한계** | Kernel | 일반 파일 FD는 epoll이 항상 Ready로 간주 → `read()` 내부에서 Page Cache Miss 시 실제 Block 발생. D-state 진입 → Event Loop Thread Block | `vmstat`의 `b`(Blocked Task 수), `ps aux | grep ' D '` |
| **io_uring과 파일 I/O** | Kernel | 파일 I/O 요청을 SQ에 등록 후 Kernel이 비동기 처리. D-state 없이 CQ 완료 통보 수신. Fixed Buffer로 Zero-Copy 가능 | `/proc/PID/fdinfo`의 uring 항목, `strace -e io_uring_enter`, `perf trace` |
| **OOM Killer와 Page Cache** | Kernel | 메모리 부족 시 Kernel이 Page Cache를 먼저 회수. 회수 후에도 부족 시 OOM Killer 발동. 과도한 Page Cache 점유가 JVM Heap과 경쟁 | `dmesg | grep oom`, `free -m`의 `available`, `/proc/meminfo`의 `MemAvailable` |
| **THP (Transparent HugePage)** | Kernel | 대용량 파일 mmap 시 THP 자동 승격(4KB → 2MB). TLB Miss 감소 효과. 승격 과정의 Compaction이 짧은 Latency Spike 유발 | `/proc/vmstat`의 `thp_fault_alloc`, `/sys/kernel/mm/transparent_hugepage/enabled` |
| **NUMA와 Page Cache** | Hardware + Kernel | Page Cache가 원격 NUMA 노드에 할당된 경우 Disk I/O 완료 후 데이터를 로컬로 복사하는 추가 비용 발생 | `numastat -m`의 `FilePages`, `numactl --preferred=0` |

---

## 11. CPU Saturation으로 이어지는 연쇄

### Blocking I/O 기반 구조

```
Thread-per-request 모델
  → Blocking I/O (read/write syscall)
  → task_struct Wait Queue 이동 (D/S 상태)
  → Thread Pool 점유 유지 → Pool 고갈
  → 신규 요청: Thread 생성 or RejectedExecutionException
  → task_struct 수 증가 → Runnable Queue 증가
  → Context Switch 증가 (Register Save/Restore + TLB Flush)
  → CFS Scheduler Overhead 증가 (Red-Black Tree 연산 증가)
  → SoftIRQ / IRQ 처리 지연 (CPU 시간 경쟁)
  → Load Average 상승 (Running + Runnable + D-state)
  → CPU Saturation
```

### Non-blocking 구조

```
Event Loop 모델
  → epoll / io_uring 기반 비동기 I/O
  → task_struct 수 최소화 (코어 수 수준)
  → epoll_wait에서 이벤트 대기 (Wait Queue에 있지만 소수)
  → Context Switch 최소화 → Cache Warm 유지
  → SoftIRQ 처리 여유 확보 (RPS/RFS로 분산)
  → Load Average 안정화
  → CPU 효율 극대화
```

### CPU Saturation 계층별 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| **Run Queue 포화** | Kernel | Runnable `task_struct` 수가 CPU 코어 수를 초과. `vmstat`의 `r` 값이 지속적으로 코어 수 초과 시 Saturation | `vmstat 1`의 `r`, `sar -q`의 `runq-sz`, `perf sched latency` |
| **Context Switch 폭증** | Kernel | Thread 수 증가 → Context Switch 증가 → Register Save/Restore + TLB Flush 비용 누적 → 실제 연산 시간 감소 | `vmstat 1`의 `cs`, `perf stat -e context-switches` |
| **cgroup CPU Throttling** | Kernel | Kubernetes CPU Limit 초과 시 Throttling. CPU 여유가 있어도 Quota 소진 시 `task_struct` Runqueue 제거 → 응답 지연 급증 | `/sys/fs/cgroup/cpu.stat`의 `throttled_usec`, `kubectl top`, `cadvisor`의 `container_cpu_throttled_seconds_total` |
| **NUMA Imbalance** | Hardware + Kernel | 특정 NUMA 노드에 task_struct 집중 → 해당 노드 CPU 과부하 + 원격 메모리 접근 증가. `numabalance` Kernel 데몬이 자동 재배치 시도 | `numastat`, `perf stat -e node-load-misses`, `numactl --hardware` |
| **CPU Frequency Scaling** | Hardware + Kernel | High Load 시 Turbo Boost로 주파수 상승. 지속적 High Load 시 Thermal Throttling으로 주파수 강제 저하 → Saturation 심화 | `turbostat`, `cpupower monitor`, `/sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq` |
| **Memory Bandwidth Saturation** | Hardware | 다수 Thread가 동시에 메모리 접근 시 DDR 채널 포화. CPU는 Stall 상태가 되며 IPC 급락. `%iowait`보다 `stalled-cycles-backend` 지표가 더 직접적 | `pcm-memory`, `perf stat -e offcore_requests_outstanding.cycles_with_data_rd` |
| **JVM GC와 Saturation** | JVM Runtime | Full GC Stop-The-World 시 모든 JVM Thread가 Safepoint에서 정지 → task_struct들이 Runnable이지만 JVM이 멈춤 → 외부에서 보면 CPU 급락 후 급증 패턴 | GC log의 `GC pause`, `jstat -gcutil`, `perf stat`의 대기 구간 |

---

## 12. SRE 관점 주요 지표

| 지표 | 의미 | 임계 신호 | 계층 |
|------|------|---------|------|
| **Thread Count** | JVM/OS Thread 수 | 수백 이상 급증 | App / OS |
| **Runnable Thread 수** | CPU 경쟁 상태 여부 | 코어 수 초과 지속 | Kernel |
| **Waiting Thread 수** | I/O 또는 Lock 대기 상태 | Thread Pool 대비 높은 비율 | Kernel / App |
| **Context Switches** | 실행 전환 비용 | 초당 수만 이상 | Kernel |
| **System CPU (`sy`)** | Kernel 작업 비중 | 20% 이상 지속 | Kernel |
| **User CPU (`us`)** | 애플리케이션 작업 비중 | 80~90% 지속 | App |
| **Softirq CPU (`si`)** | 네트워크 패킷 처리 비용 | 특정 CPU에 집중 | Kernel / Hardware |
| **Open FD Count** | 열린 I/O 통로 수 | `ulimit -n` 근접 | OS |
| **DB Connection Pool Usage** | DB 병목 여부 | 90% 이상 지속 | App |
| **Run Queue Length (`r`)** | CPU 대기 중인 task 수 | 코어 수의 2배 이상 | Kernel |
| **Load Average** | Running + Runnable + D-state 누적 | 코어 수 초과 지속 | Kernel |
| **Cache Miss Rate** | Page Cache 효율 저하 여부 | pgmajfault 급증 | Kernel / Hardware |
| **CPU Throttled Time** | cgroup Quota 소진 비율 | `throttled_usec` 증가 | Kernel |
| **PSI cpu full** | 전체 Task CPU 대기 비율 | `full > 0` 지속 | Kernel |
| **P99 / P999 Latency** | 사용자 관점 지연 | SLA 기준 초과 | App |
| **Off-CPU Time** | Task가 CPU 없이 대기한 시간 | 응답 시간의 50% 이상 | Kernel |

---

## 13. 진단 명령어

```bash
# CPU 사용률 및 Context Switch 전반
vmstat 1

# Thread별 CPU 사용률 확인
top -H -p <PID>

# Process별 Context Switch 횟수 (자발적 / 비자발적)
pidstat -w -p <PID> 1

# CPU core별 사용률 (sy, softirq, iowait 포함)
mpstat -P ALL 1

# Run Queue 길이 및 Load Average
sar -q 1

# 소켓 연결 상태 요약 및 버퍼 상태
ss -s
ss -tmn

# TCP SYN Queue 상태
ss -lnt
netstat -s | grep -i syn

# 열린 File Descriptor 수
lsof -p <PID> | wc -l

# Context Switch, Cache Miss, syscall 하드웨어 카운터
perf stat -e context-switches,cpu-migrations,cache-misses,cache-references,syscalls:sys_enter_read -p <PID>

# CPU Pressure 지표 (PSI)
cat /proc/pressure/cpu
cat /proc/pressure/memory
cat /proc/pressure/io

# D-state(Uninterruptible Sleep) Task 확인
ps aux | awk '$8 ~ /D/ {print}'
cat /proc/<PID>/wchan

# Disk I/O 지연 및 활용률
iostat -x 1

# Page Cache 및 메모리 상태
free -m
cat /proc/vmstat | grep -E 'pgfault|pgmajfault|nr_dirty'

# Softirq 분포
cat /proc/softirqs
watch -n1 cat /proc/softirqs

# cgroup CPU Throttling 상태
cat /sys/fs/cgroup/cpu.stat

# epoll / io_uring syscall 추적
strace -e epoll_wait,epoll_ctl,io_uring_enter -p <PID>

# Off-CPU 분석
offcputime-bpfcc -p <PID> 30

# NIC 링 버퍼 상태
ethtool -S <iface> | grep -i drop
ethtool -l <iface>

# NUMA 메모리 분포
numastat -c
numactl --hardware
```

---

## 14. 핵심 정리

| 방식 | 특징 요약 | Kernel 상태 | 주요 성능 지표 |
|------|----------|------------|--------------|
| **Blocking I/O** | Thread가 I/O 완료까지 대기. `task_struct`가 Wait Queue로 이동. Thread Pool 점유. Thread 수 및 Context Switch 증가 가능 | TASK_INTERRUPTIBLE / TASK_UNINTERRUPTIBLE | Context Switch, Wait Thread 수, Off-CPU Time |
| **epoll** | FD 이벤트 기반 통지. 소수의 Event Loop Thread로 다수의 네트워크 연결 처리. Runnable Queue 길이 안정화. 네트워크 I/O에 적합 | epoll_wait 대기 중 TASK_INTERRUPTIBLE | Softirq CPU, Recv-Q, epoll_wait 호출 빈도 |
| **io_uring** | SQ/CQ 기반 비동기 I/O. 시스템 콜 오버헤드 최소화. 네트워크와 디스크 I/O 모두에 적합. `task_struct` 수와 Scheduler Overhead 감소에 유리 | Runnable 유지에 최적 (SQPOLL 시) | syscall 수, CQ 완료 Latency, D-state 부재 |

실무에서는 epoll이나 io_uring을 직접 구현하는 경우보다, **WebClient, Netty, R2DBC 같은 상위 라이브러리가 내부적으로 어떤 I/O 엔진을 사용하는지 이해하는 것**이 중요합니다.

---

> **결론**  
> I/O 처리 방식이 `task_struct`의 상태를 결정하고,  
> `task_struct`의 상태가 Runnable Queue, Wait Queue, Scheduler Overhead,  
> Context Switch 비용, SoftIRQ 부하, cgroup Throttling 여부,  
> 그리고 CPU Saturation 여부를 결정합니다.
>
> ```
> I/O 모델 선택
>   → task_struct 상태 분포 결정
>   → Context Switch / Scheduler Overhead 결정
>   → SoftIRQ / IRQ 처리 여유 결정
>   → cgroup Quota 소비 패턴 결정
>   → CPU Saturation 여부 결정
>   → P99 / P999 Latency 결정
> ```

---

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*