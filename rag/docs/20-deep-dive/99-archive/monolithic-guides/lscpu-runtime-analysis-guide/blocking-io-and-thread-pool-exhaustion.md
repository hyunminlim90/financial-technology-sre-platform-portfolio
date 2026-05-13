# Blocking I/O와 Thread Pool 고갈 (E2E 분석 적용됨)

## 1. 개요

Blocking I/O는 I/O 작업 완료를 기다리는 동안 Thread가 다음 코드를 실행하지 못하는 처리 방식이다.

OS Kernel 수준에서는 해당 Thread의 `task_struct`가 Runqueue에서 Wait Queue로 이동한다.
그러나 Java Thread Pool 관점에서는 해당 Thread가 아직 작업을 완료하지 않았으므로 Pool에 반환되지 않는다.

이 구조가 누적되면 CPU 여유가 있어도 새로운 요청을 처리할 Thread가 부족한 **Thread Pool 고갈** 상태가 발생한다.

### 전 계층 구조

```
Application Layer
    └── Java Thread Pool (Tomcat / Spring 등)
        └── Thread 상태: RUNNABLE / WAITING / TIMED_WAITING / BLOCKED

JVM Runtime Layer
    ├── JIT Compilation (C1 / C2)
    ├── Safepoint
    ├── TLAB (Thread-Local Allocation Buffer)
    └── GC (Young / Old Generation)

OS Kernel Layer
    ├── task_struct (각 Thread에 대응)
    ├── CFS Scheduler (Runqueue / Wait Queue)
    ├── System Call (read, recv, epoll_wait 등)
    ├── IRQ / SoftIRQ (I/O 완료 통보)
    ├── Socket Buffer (sk_buff)
    ├── TCP Backlog / SYN Queue / Accept Queue
    ├── Futex (Lock 대기)
    ├── cgroup (CPU / Memory / IO 자원 제한)
    └── PSI (Pressure Stall Information)

Hardware Layer
    ├── CPU Core / Logical CPU
    ├── L1 / L2 / L3 Cache
    ├── TLB (Translation Lookaside Buffer)
    ├── NIC (Network Interface Card)
    ├── DMA (Direct Memory Access)
    └── Disk (HDD / NVMe SSD)
```

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| Application | Thread Dump, JMX `ThreadPool.activeCount`, APM |
| JVM Runtime | JFR, `jstack`, `-XX:+PrintSafepointStatistics` |
| OS Kernel | `pidstat -w`, `vmstat cs`, `/proc/PID/status`, `strace` |
| Hardware | `perf stat -e context-switches,cache-misses`, `mpstat` |

---

## 2. Blocking I/O 발생 시 상태 전환

```
Java Thread 실행 (RUNNABLE)
    ↓
DB Query / 외부 API / 파일 I/O 요청
    ↓
System Call 진입 (User Mode → Kernel Mode)
    │   ├── 네트워크 I/O: recv() / read() / epoll_wait()
    │   ├── 파일 I/O: read() / pread64()
    │   └── DB (JDBC): Socket recv() 내부 호출
    ↓
Kernel: task_struct 상태 전이
    │   TASK_RUNNING → TASK_INTERRUPTIBLE (일반 Blocking)
    │                   또는 TASK_UNINTERRUPTIBLE (D 상태, Disk I/O 등)
    ↓
task_struct → Runqueue 제거 → Wait Queue 등록
    ↓
CFS Scheduler: 다른 TASK_RUNNING task를 CPU에 스케줄링
    ↓
I/O 완료 이벤트 발생
    │   ├── 네트워크: NIC → IRQ → SoftIRQ → sk_buff 수신 → Socket Wake-up
    │   └── 디스크: DMA 완료 → IRQ → Block Layer → Wait Queue Wake-up
    ↓
task_struct → Wait Queue 제거 → Runqueue 재진입 (TASK_RUNNING)
    ↓
Java Thread 실행 재개 (RUNNABLE)
```

Blocked 상태의 `task_struct`는 삭제되지 않으며 아래 정보를 유지한 채 대기한다.

| 유지 항목 | 설명 | 계층 |
|----------|------|------|
| Program Counter | 재개 시 실행 위치 | CPU Register |
| Stack Pointer | Stack 상태 | CPU Register / Kernel Stack |
| Register Context | CPU 실행 상태 | CPU Register File |
| Java Stack | 호출 스택 정보 | JVM Thread Stack (Heap 외부) |
| CFS 스케줄링 정보 | vruntime, load weight | Kernel Runqueue 메타 |
| I/O 대기 정보 | 기다리는 이벤트, Wait Queue 포인터 | Kernel Wait Queue |
| Futex 대기 정보 | Lock 대기 시 Futex uaddr, 큐 포인터 | Kernel Futex Hash Table |

### TASK_UNINTERRUPTIBLE (D 상태) 주의

D 상태 `task_struct`는 Signal로도 깨울 수 없다.
Disk I/O, NFS 마운트, 일부 Kernel Lock 대기 시 발생하며,
`load average`에는 포함되지만 CPU를 사용하지 않는다.
D 상태 Thread가 누적되면 `load average`가 높아도 CPU 사용률은 낮은 현상이 나타난다.

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| OS Kernel | `/proc/PID/status` (`State: D`), `ps aux` (`D` 상태 프로세스) |
| OS Kernel | `dmesg` (Disk / NFS 관련 에러), `iostat -x` (`%await` 항목) |
| OS Kernel | `/proc/pressure/io` (PSI IO 압력 지표) |
| Hardware | `iostat -d` (Disk I/O 처리량), `nvme smart-log` (NVMe 상태) |

---

## 3. CPU 관점과 Thread Pool 관점의 차이

| 관점 | 해석 |
|------|------|
| CPU / Kernel Scheduler | 해당 `task_struct`는 CPU를 사용하지 않으므로 다른 Runnable task를 실행할 수 있음 |
| Java Thread Pool | 해당 Thread는 요청 처리를 완료하지 않았으므로 Pool에 반환되지 않음 |

CPU는 비어 있어도 Java Thread Pool은 고갈될 수 있다.

### Off-CPU Time

Blocking I/O로 인해 Thread가 CPU를 사용하지 않는 시간을 **Off-CPU Time**이라 한다.
On-CPU 샘플링 프로파일러(`perf`, `async-profiler` CPU mode)는 이 시간을 관찰하지 못한다.
Off-CPU Time은 별도 계측 도구가 필요하다.

```
On-CPU Time:   Thread가 CPU에서 실제 실행 중인 시간
Off-CPU Time:  Thread가 I/O 대기, Lock 대기, Sleep 등으로 CPU 미사용 시간

Wall-clock Latency = On-CPU Time + Off-CPU Time
```

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| OS Kernel | `offcputime` (BCC tools) — Off-CPU Flame Graph 생성 |
| OS Kernel | `perf sched latency` — 스케줄링 대기 시간 분석 |
| JVM Runtime | async-profiler Wall-clock mode (`-e wall`) |
| JVM Runtime | JFR Thread State (`WAITING` / `BLOCKED` 시간 비율) |

---

## 4. Thread Pool 고갈 구조

```
Thread Pool Max = 200

200개 요청 동시 유입
    ↓
200개 Thread가 각각 System Call 진입
    ↓
200개 task_struct가 Wait Queue로 이동 (TASK_INTERRUPTIBLE)
    ↓
Thread Pool에 잔여 Thread 없음
    ↓
신규 요청 → Thread Pool Queue 대기 또는 Rejection
    ↓
Queue가 가득 차면 RejectedExecutionException 또는 HTTP 503
```

CPU 사용률이 낮아도 서버가 응답하지 못하는 상태가 된다.

### TCP 계층에서의 연쇄 영향

Thread Pool이 고갈되면 `accept()` System Call을 호출하는 속도가 저하된다.
Kernel의 Accept Queue(3-way handshake 완료 연결 대기)가 포화되면
신규 연결이 Drop된다.

```
Thread Pool 고갈
    ↓
accept() 호출 지연
    ↓
Accept Queue 포화 (net.core.somaxconn 한계)
    ↓
SYN Queue 포화 (net.ipv4.tcp_max_syn_backlog 한계)
    ↓
신규 SYN 패킷 Drop → 클라이언트 Connection Timeout
```

### 일부 느린 I/O가 전체 요청에 미치는 영향

```
느린 DB 요청 증가
    ↓
Worker Thread 장시간 점유 (task_struct → Wait Queue 체류)
    ↓
사용 가능한 Thread 감소
    ↓
가벼운 요청도 Thread Pool Queue에서 대기
    ↓
전체 응답 지연 (Latency 상승)
```

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| Application | JMX `ThreadPool.activeCount`, `ThreadPool.queueSize` |
| OS Kernel | `ss -lnt` (Listen Socket `Recv-Q` 항목 — Accept Queue 포화 여부) |
| OS Kernel | `netstat -s` (`SYNs to LISTEN sockets dropped`) |
| OS Kernel | `sysctl net.core.somaxconn`, `net.ipv4.tcp_max_syn_backlog` |
| OS Kernel | `/proc/net/sockstat` (Socket 사용 현황) |

---

## 5. Thread Pool 크기와 발생 문제

### Thread Pool 크기를 늘릴 때의 위험

Blocking I/O 병목이 해결되지 않은 상태에서 Thread 수만 늘리면 아래 문제가 연쇄적으로 발생한다.

```
Thread 수 증가
    ↓
task_struct 증가 → Native Memory (Kernel Stack: 8KB~16KB / Thread) 증가
    ↓
JVM Thread Stack 증가 (-Xss 기본 512KB~1MB / Thread)
    ↓
Context Switch 증가 (CFS 스케줄링 대상 task_struct 증가)
    ↓
TLB Flush 빈도 증가 (Context Switch 시 TLB 일부 무효화)
    ↓
CPU Cache Miss 증가 (각 Thread의 Working Set이 Cache에 경합)
    ↓
Cache Line Thrashing (다수 Thread가 동일 Cache Line 경합)
    ↓
Memory Bandwidth Saturation (Thread 간 메모리 접근 경합)
    ↓
Scheduler Overhead 증가 (CFS Red-Black Tree 탐색 비용 증가)
```

I/O 병목이 동시에 해소되어 다수의 Thread가 Runnable 상태로 복귀하면
CPU Saturation이 발생할 수 있다.

### NUMA 환경에서의 추가 위험

NUMA(Non-Uniform Memory Access) 환경에서 Thread 수가 증가하면
원격 NUMA 노드 메모리 접근 비율이 높아진다.

```
Thread 수 증가 → 원격 NUMA 노드 메모리 접근 증가
    ↓
메모리 접근 Latency 증가 (로컬 노드 대비 2~4배)
    ↓
Memory Bandwidth Saturation 가중
    ↓
CPU 대기 사이클 증가 (Memory Stall)
```

### Thread Pool 제한이 없는 경우

`Executors.newCachedThreadPool()` 또는 제한 없는 커스텀 Executor 사용 시 다음 문제가 발생한다.

```
요청 증가 → Thread 계속 생성 → task_struct 계속 생성
    ↓
Native Memory (Thread Stack) 급증
    ↓
JVM Direct Memory / Off-Heap 영역 압박
    ↓
Context Switch 급증 → Scheduler Overhead 급증
    ↓
OOM Killer 개입 가능 (Native Memory 고갈)
    또는
unable to create new native thread (ulimit nofile / nproc 한계)
```

### OOM Killer 개입

Native Memory(Thread Stack 포함)가 고갈되면 Kernel OOM Killer가 개입한다.
OOM Killer는 `oom_score`가 높은 프로세스를 SIGKILL로 종료한다.
JVM 프로세스는 Heap이 크면 `oom_score`가 높아 OOM Killer 대상이 될 수 있다.

```
Native Memory 고갈
    ↓
Kernel OOM Killer 활성화
    ↓
oom_score 기반 대상 프로세스 선정
    ↓
SIGKILL 전송 → JVM 프로세스 강제 종료
```

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| OS Kernel | `dmesg` (`Out of memory: Killed process`) |
| OS Kernel | `/proc/PID/oom_score`, `/proc/PID/oom_adj` |
| OS Kernel | `/proc/meminfo` (`MemAvailable` 항목) |
| OS Kernel | `ulimit -u` (최대 프로세스/Thread 수 제한) |
| JVM Runtime | `-Xss` (Thread Stack 크기 설정), Native Memory Tracking (`-XX:NativeMemoryTracking=detail`) |

### CPU Core 대비 Thread 수가 과도한 경우

```
CPU Core = 8 / Thread Pool Max = 500

I/O 완료 후 다수 Thread Runnable 복귀
    ↓
500개 task_struct가 8개 CPU Core 경쟁
    ↓
CFS: Red-Black Tree에서 vruntime 최소 task 선택 반복
    ↓
Context Switch 증가 → Cache Miss 증가
    ↓
Scheduler Overhead 증가 → CPU 실제 작업 시간 감소
    ↓
Throughput 감소 (CPU는 Scheduler 동작에 소비)
```

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| OS Kernel | `vmstat cs` (Context Switch 횟수), `pidstat -w` |
| OS Kernel | `/proc/schedstat` (스케줄러 통계) |
| OS Kernel | `perf sched` (스케줄링 지연 분석) |
| Hardware | `perf stat -e context-switches,cache-misses,instructions` |
| Hardware | `numactl --hardware` (NUMA 토폴로지), `numastat` (NUMA 접근 통계) |

---

## 6. Blocking I/O와 CPU Saturation 연쇄 구조

```
Blocking I/O 증가
    ↓
Thread Pool 점유 증가 (task_struct → Wait Queue)
    ↓
Thread 수 증가 또는 Pool 고갈
    ↓
I/O 완료 → IRQ → SoftIRQ → Wake-up → Runnable task_struct 급증
    ↓
Context Switch 증가
    ↓
TLB Flush 빈도 증가 → Page Table Walk 비용 증가
    ↓
Cache Miss 증가 (L1/L2/L3 Cache Invalidation)
    ↓
Cache Line Thrashing (다수 Thread의 공유 데이터 Cache 경합)
    ↓
Memory Bandwidth Saturation
    ↓
Scheduler Overhead 증가
    ↓
CPU Saturation (CPU는 스케줄링 / 캐시 처리에 소비)
```

### IRQ / SoftIRQ 부하 증가

I/O 완료 이벤트가 폭증하면 IRQ와 SoftIRQ 처리 자체가 CPU를 점유한다.
특히 네트워크 트래픽이 집중되면 `ksoftirqd` Kernel Thread가 특정 CPU Core를 독점할 수 있다.

```
NIC 패킷 수신 폭증
    ↓
IRQ Handler 실행 (CPU 실행 중단)
    ↓
SoftIRQ (NET_RX_SOFTIRQ) 처리 — ksoftirqd
    ↓
sk_buff 할당 → TCP/IP Stack 처리
    ↓
특정 CPU Core SoftIRQ 점유 → 다른 task 스케줄링 지연
```

RPS(Receive Packet Steering) / RFS(Receive Flow Steering)를 사용하면
IRQ 처리를 여러 CPU Core에 분산할 수 있다.

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| OS Kernel | `mpstat -I ALL` (`%irq`, `%soft` 항목), `/proc/interrupts` |
| OS Kernel | `/proc/softirqs` (SoftIRQ 종류별 횟수), `sar -I ALL` |
| OS Kernel | `/proc/sys/net/core/rps_cpus` (RPS 설정), `ethtool -l` (NIC Queue 수) |
| Hardware | `perf stat -e irq:*` (IRQ 이벤트 카운트) |

---

## 7. Blocking vs Non-blocking I/O 비교

| 항목 | Blocking I/O | Non-blocking I/O |
|------|-------------|-----------------|
| I/O 대기 중 Thread | 점유됨 (WAITING 상태) | 다른 작업 수행 가능 |
| task_struct 상태 | TASK_INTERRUPTIBLE (Wait Queue) | TASK_RUNNING (이벤트 루프 실행 중) |
| Thread Pool 압박 | 큼 | 상대적으로 작음 |
| task_struct 수 | 증가하기 쉬움 | 소수의 Event Loop Thread 유지 |
| Context Switch | 증가 가능 | 상대적으로 적음 |
| CPU Cache 효율 | 낮아질 수 있음 (Thread 교체로 Working Set 교체) | 동일 Thread 유지로 Cache Warm 상태 유지 |
| System Call 방식 | `read()` / `recv()` (Blocking) | `epoll_wait()` / `io_uring` |
| 대표 구조 | Spring MVC + JDBC + RestTemplate | WebFlux + Netty + 비동기 Client |

### Non-blocking I/O 처리 흐름 (epoll 기반)

```
I/O 요청 등록 (epoll_ctl: EPOLL_CTL_ADD)
    ↓
epoll_wait() 호출 (Event Loop Thread 대기)
    ↓
NIC 패킷 수신 → IRQ → SoftIRQ → sk_buff 처리
    ↓
Socket 수신 버퍼에 데이터 적재
    ↓
epoll_wait() 리턴 (Event 발생 통보)
    ↓
Callback / Event Handler 실행 (동일 Event Loop Thread)
    ↓
다음 epoll_wait() 호출
```

### io_uring 기반 Non-blocking I/O

`io_uring`은 SQ(Submission Queue)와 CQ(Completion Queue)를 User Space와 Kernel 간 공유 Ring Buffer로 구현한다.
System Call 횟수를 최소화하여 Blocking I/O와 비교해 Context Switch 비용을 크게 줄인다.

```
User Space: SQ에 I/O 요청 기록 (System Call 없이)
    ↓
io_uring_enter() System Call (일괄 제출)
    ↓
Kernel: 비동기 I/O 실행
    ↓
Kernel: CQ에 완료 결과 기록
    ↓
User Space: CQ 폴링 (System Call 없이 완료 확인)
```

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| OS Kernel | `strace -e epoll_wait,epoll_ctl,io_uring_enter` |
| OS Kernel | `/proc/PID/fdinfo` (io_uring 항목), `ethtool -S` (NIC Ring Buffer 통계) |
| JVM Runtime | async-profiler Wall-clock mode, JFR Socket Read/Write 이벤트 |
| Hardware | `perf stat -e syscalls:sys_enter_epoll_wait` |

---

## 8. 대표 장애 패턴

### CPU 사용률이 낮지만 응답이 느린 경우

```
DB / 외부 API 지연
    ↓
Thread Pool 전체가 Blocking 대기 (task_struct → Wait Queue)
    ↓
신규 요청에 할당할 Thread 없음 → Thread Pool Queue 적체
    ↓
요청 대기 또는 Timeout
    ↓
CPU 사용률은 낮음 (%user + %sys 낮음)
    ↓
load average는 높을 수 있음 (D 상태 task 포함)
```

CPU 증설보다 Downstream I/O 병목, Connection Pool 설정, Timeout 설정을 먼저 확인해야 한다.

### Connection Pool Exhaustion

DB Connection Pool이 고갈되면 Thread가 Connection 확보를 위해 추가로 대기한다.
Thread Pool 고갈과 Connection Pool 고갈이 동시에 발생하면 서비스 전체가 정지한다.

```
DB 응답 지연
    ↓
JDBC Connection 점유 시간 증가
    ↓
Connection Pool 고갈 (HikariCP: maximumPoolSize 소진)
    ↓
Thread가 Connection 대기 (connectionTimeout 초과 시 SQLException)
    ↓
Thread Pool도 고갈 → 신규 요청 처리 불가
```

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| Application | HikariCP 메트릭 (`pool.ActiveConnections`, `pool.PendingConnections`) |
| Application | APM DB Span Latency, Connection Wait Time |
| OS Kernel | `ss -nt` (ESTABLISHED 상태 DB Connection 수) |
| OS Kernel | `netstat -s` (TCP 재전송 통계 — DB 서버 네트워크 이상 여부) |

### CPU 사용률이 높지만 처리량이 낮은 경우

```
Thread 수 과다
    ↓
Context Switch 증가 → Scheduler Overhead 증가
    ↓
Cache Miss 증가 (Thread 교체 시 L1/L2 Cache Working Set 교체)
    ↓
Cache Line Thrashing (공유 자원에 대한 다수 Thread 경합)
    ↓
CPU Frequency Scaling 개입 가능
    │   └── 짧은 Burst 후 C-state 진입 반복 → P-state 전환 지연
    ↓
CPU Saturation → Throughput 감소
```

Thread Pool 크기, Blocking 호출 비율, Runnable Thread 수, CPU Throttling(cgroup)을 함께 확인해야 한다.

### Retry Storm

Timeout 또는 Circuit Breaker 미적용 상태에서 Downstream 장애 발생 시,
클라이언트가 재시도를 반복하여 Downstream에 더 큰 부하를 가한다.

```
Downstream 응답 지연
    ↓
클라이언트 Timeout → 재시도 (Retry)
    ↓
Downstream 부하 증가 → 응답 지연 악화
    ↓
더 많은 Thread가 Blocking 대기
    ↓
Thread Pool 고갈 가속
    ↓
Retry Storm (재시도가 장애를 심화)
```

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| Application | APM 재시도 횟수 메트릭, Circuit Breaker 상태 (Open / Half-Open / Closed) |
| OS Kernel | `netstat -s` (TCP 재전송 통계), `ss -s` (Socket 상태 분포) |
| OS Kernel | `/proc/net/tcp` (TIME_WAIT 소켓 수 — Retry로 인한 연결 증가) |

---

## 9. SRE 관점 진단 지표

| 지표 | 의미 | 관찰 도구 |
|------|------|-----------|
| Thread Pool Active Count | 현재 요청 처리 중인 Thread 수 | JMX, Micrometer, APM |
| Thread Pool Queue Size | 처리 대기 중인 요청 수 | JMX, Micrometer |
| Waiting / Timed Waiting Thread 수 | I/O 또는 Lock 대기 Thread 수 | Thread Dump, JFR |
| Off-CPU Time | Thread가 CPU를 사용하지 않는 시간 | `offcputime` (BCC), async-profiler wall |
| DB Connection Pool 사용률 | Downstream 병목 여부 | HikariCP 메트릭, APM |
| Runnable Thread 수 | CPU 경쟁 중인 task 수 | `pidstat`, Thread Dump |
| Context Switch Rate | Thread 전환 비용 | `vmstat cs`, `pidstat -w` |
| Load Average | Runnable + D 상태 task 누적 | `uptime`, `top`, `sar -q` |
| %iowait | CPU가 I/O 완료를 대기한 비율 | `mpstat`, `iostat` |
| %soft | SoftIRQ가 CPU를 점유한 비율 | `mpstat -I ALL` |
| TCP Accept Queue 포화 | Thread Pool 고갈 → accept() 지연 | `ss -lnt` (Recv-Q) |
| SYN Drop 횟수 | TCP Backlog 고갈 여부 | `netstat -s` |
| PSI CPU / IO / Memory | 자원 부족 압력 지표 | `/proc/pressure/*` |
| cgroup CPU Throttling | Container 환경 CPU Quota 소진 | `/sys/fs/cgroup/cpu.stat` |
| P99 / P999 Latency | 사용자 관점 응답 지연 | APM, Prometheus + Grafana |
| Cache Miss Rate | Thread 교체로 인한 캐시 효율 저하 | `perf stat -e cache-misses` |
| NUMA Remote Access 비율 | 원격 메모리 접근 증가 여부 | `numastat`, `perf stat -e node-load-misses` |

---

## 10. 운영 대응 방향

### Timeout 설정

느린 I/O가 Thread를 장시간 점유하지 않도록 제한한다.
Timeout은 Connection Timeout, Read Timeout, Statement Timeout을 계층별로 모두 설정해야 한다.

```
Connection Timeout  (TCP 연결 수립 제한)
    └── Read Timeout / Socket Timeout  (데이터 수신 제한)
        └── Statement Timeout  (JDBC 쿼리 실행 제한)
            └── Transaction Timeout  (전체 트랜잭션 제한)
```

### Circuit Breaker

장애 Downstream 호출을 빠르게 차단하여 Thread Pool 고갈을 방지한다.

```
정상 상태: CLOSED (모든 요청 통과)
    ↓ 실패율 임계치 초과
OPEN (모든 요청 즉시 차단, Thread 점유 없음)
    ↓ 일정 시간 후
HALF-OPEN (일부 요청 허용, 복구 여부 확인)
    ↓ 성공 시
CLOSED 복귀
```

### Bulkhead

DB, 외부 API 등 Downstream 별로 Thread Pool 또는 Semaphore를 분리한다.
하나의 Downstream 장애가 전체 Thread Pool을 고갈시키는 것을 방지한다.

```
기존 구조: 단일 Thread Pool → DB / API-A / API-B 모두 처리
    ↓ API-B 장애 시 전체 Thread Pool 고갈

Bulkhead 적용:
    Thread Pool-A → DB
    Thread Pool-B → API-A
    Thread Pool-C → API-B  ← 고갈되어도 다른 Pool 영향 없음
```

### Backpressure 적용

시스템 처리량을 초과하는 유입을 상위 계층에서 제어한다.

```
클라이언트 요청 유입 속도 > 처리 속도
    ↓
Thread Pool Queue 포화 감지
    ↓
상위 계층에 신호 전달 (HTTP 429 Too Many Requests)
    ↓
클라이언트 요청 속도 감소 (Back-off)
    ↓
Thread Pool 안정화
```

### Thread Pool 크기 조정

CPU Core 수와 I/O 특성에 맞게 조정한다.
Little's Law에 따라 적정 Thread 수를 추정할 수 있다.

```
Little's Law:
    Thread 수 = Throughput (RPS) × 평균 응답 시간 (초)

예: 100 RPS × 0.5초 = 50 Thread

I/O 집약적 서비스: CPU Core 수 × (1 + Wait Time / CPU Time)
CPU 집약적 서비스: CPU Core 수 + 1 (예비 1개)
```

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| Application | Resilience4j (Circuit Breaker, Bulkhead, Rate Limiter) |
| Application | HikariCP `connectionTimeout`, `maximumPoolSize` |
| OS Kernel | `sysctl net.ipv4.tcp_syn_retries` (Retry 횟수 제한) |
| OS Kernel | `tc` (Traffic Control — 네트워크 수준 Backpressure) |

---

## 11. 최종 정리

```
Blocking I/O
    ↓
System Call 진입 (read / recv / epoll_wait)
    ↓
task_struct → Wait Queue (TASK_INTERRUPTIBLE / TASK_UNINTERRUPTIBLE)
    ↓
Thread Pool 점유 (반환되지 않음)
    ↓
Pool 고갈 또는 Thread 수 증가
    ↓
I/O 완료 → IRQ → SoftIRQ → Wake-up → Runnable task_struct 급증
    ↓
Context Switch 증가 → TLB Flush → Cache Miss → Cache Line Thrashing
    ↓
Memory Bandwidth Saturation → Scheduler Overhead 증가
    ↓
CPU Saturation (실제 작업보다 스케줄링 / 캐시 처리에 CPU 소비)
```

| 항목 | 내용 |
|------|------|
| Thread Pool 고갈 원인 | I/O 대기 중 `task_struct`가 Wait Queue에 체류하며 Thread가 Pool에 반환되지 않음 |
| CPU 관점과의 차이 | CPU는 여유가 있어도 Thread Pool은 고갈 가능 (Off-CPU Time 누적) |
| Thread 수 증가의 위험 | Context Switch → TLB Flush → Cache Miss → Cache Line Thrashing → Memory Bandwidth Saturation → Scheduler Overhead 연쇄 증가 |
| NUMA 환경 추가 위험 | Thread 증가 시 원격 NUMA 노드 메모리 접근 비율 증가 → Latency 가중 |
| OOM Killer 위험 | 무제한 Thread 생성 시 Native Memory 고갈 → OOM Killer 개입 |
| Retry Storm 위험 | Timeout / Circuit Breaker 미적용 시 재시도가 Downstream 부하를 가중하고 Thread Pool 고갈 가속 |
| 핵심 모니터링 대상 | Thread Pool Active/Queue, Off-CPU Time, DB Connection Pool, Context Switch, PSI, P99 Latency |
| 근본적 해결 방향 | Non-blocking I/O 적용, Bulkhead / Circuit Breaker / Backpressure, 적정 Thread Pool 크기 유지, Timeout 계층별 설정 |

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*