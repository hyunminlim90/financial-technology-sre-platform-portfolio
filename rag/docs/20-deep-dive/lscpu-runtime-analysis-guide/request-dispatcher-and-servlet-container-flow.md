# RequestDispatcher: 전 계층 동작 메커니즘 분석

## 개요

`RequestDispatcher`는 Servlet 스펙이 정의하는 서버 내부 요청 전달 인터페이스다. 클라이언트가 보낸 단일 HTTP 요청을 서버 내부의 다른 자원으로 전달하거나(`forward`), 다른 자원의 실행 결과를 현재 응답에 포함(`include`)하는 데 사용된다. 이 문서는 Hardware부터 Application까지 전 계층에서 실제로 발생하는 메커니즘 실체를 분석한다.

---

## 전 계층 메커니즘 분석표

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 관찰 키워드 |
|------|--------|--------------|---------------------------|
| **Worker Thread 획득** | Container → App | HTTP 요청 수신 시 Tomcat NIO Connector가 Acceptor Thread로 연결을 수락. Poller가 OP_READ 이벤트를 감지하고 Executor Thread Pool에서 Worker Thread를 할당. task_struct가 Running 상태로 전환되어 Servlet 코드 진입 | `/proc/PID/status`의 Threads 항목, `jstack` ThreadPool 상태, Tomcat `maxThreads` 설정, `ps -eLf`로 task_struct 수 확인 |
| **시스템 콜 — 요청 수신** | Hardware → Kernel → App | NIC에서 패킷 수신 시 DMA로 sk_buff 할당. IRQ 발생 후 SoftIRQ(NET_RX)가 TCP/IP 스택을 처리. `epoll_wait()` 반환으로 User Mode 복귀. Accept Queue(ESTABLISH 상태)에서 소켓 디스크립터 반환 | `/proc/interrupts`, `mpstat`의 `%irq` / `%soft`, `ss -s`로 Accept Queue 상태, `netstat -s`의 `TCPBacklogDrop` |
| **TCP Backlog / SYN Queue** | Kernel | 연결 요청 급증 시 SYN Queue(half-open)와 Accept Queue(fully established) 포화 발생. `tcp_syncookies`가 비활성화된 경우 SYN Drop 발생. forward 처리 지연으로 Accept Queue 소진 가능 | `ss -lnt`의 `Recv-Q`, `netstat -s \| grep -i listen`, `/proc/sys/net/ipv4/tcp_max_syn_backlog`, `/proc/sys/net/core/somaxconn` |
| **sk_buff 관리** | Kernel | 수신된 HTTP 요청 데이터는 sk_buff 링크드 리스트로 구성. Kernel이 소켓 수신 버퍼(`SO_RCVBUF`)에 적재하고 JVM이 `read()` 시스템 콜로 복사. forward 처리 중 응답 데이터는 송신 버퍼(`SO_SNDBUF`)를 통해 전송 | `ss -tm`으로 소켓 버퍼 사용량, `/proc/net/sockstat`, `sysctl net.core.rmem_max`, `perf trace -e net:*` |
| **하드웨어 인터럽트 (IRQ)** | Hardware → Kernel | NIC 패킷 수신 완료, Disk I/O 완료 시 CPU에 IRQ 신호 전달. CPU 실행 중단 후 IRQ Handler 수행. forward 대상이 정적 파일인 경우 Disk I/O 완료 IRQ가 발생 | `/proc/interrupts`, `mpstat -I ALL`, `perf stat -e irq:*`, `sar -I ALL` |
| **소프트 인터럽트 (SoftIRQ)** | Kernel | NET_RX SoftIRQ가 sk_buff를 TCP/IP 스택으로 전달. 트래픽 폭증 시 `ksoftirqd` 커널 스레드의 CPU 사용률 급증. forward 요청 부하 증가 시 SoftIRQ CPU 점유 증가 | `mpstat`의 `%soft`, `/proc/softirqs`, `sar -I ALL`, `watch -d cat /proc/softirqs` |
| **RPS / RFS** | Kernel | 멀티 코어 환경에서 NIC 수신 패킷을 여러 CPU로 분산(RPS). RFS는 패킷을 처리하는 Worker Thread가 실행 중인 CPU로 라우팅하여 Cache Miss 감소. Tomcat Worker Thread 부하와 NIC 처리 CPU 불일치 시 Cache Thrashing 발생 | `cat /sys/class/net/eth0/queues/rx-0/rps_cpus`, `ethtool -l eth0`, `/proc/net/rps_dev_flow_table_cnt` |
| **System Call — forward/include 경로** | App → Kernel | 동적 Servlet으로 forward 시 JVM 내부 메서드 호출만 발생(User Mode). 정적 파일로 forward 시 `open()`, `read()`, `sendfile()` 시스템 콜 발생. 응답 소켓 쓰기 시 `write()` 또는 `sendfile()` 호출 | `strace -p PID -e trace=file,network`, `perf trace`, `/proc/PID/syscall`로 현재 시스템 콜 확인 |
| **vDSO** | App (User-side) | `clock_gettime()` 등 시간 조회 호출은 Kernel 전환 없이 vDSO를 통해 처리. forward 처리 중 타임스탬프 빈번 호출 시 시스템 콜 오버헤드 없이 처리 | `perf stat` 시스템 콜 목록 미등장 확인, `/proc/PID/maps`의 `vdso` 항목, `ltrace` |
| **Context Switch** | Kernel | RequestDispatcher.forward/include 자체는 동일 task_struct에서 실행되므로 Context Switch 미발생. 단, 대상 자원 내부에서 Blocking I/O 발생 시 task_struct가 Wait Queue로 이동하고 다른 task_struct로 Context Switch 발생 | `vmstat`의 `cs` 항목, `pidstat -w -p PID`, `perf stat -e context-switches`, `/proc/PID/status`의 `voluntary_ctxt_switches` |
| **task_struct 상태 전이** | Kernel | forward/include 실행 중 Blocking I/O 미발생 시 task_struct는 TASK_RUNNING 유지. JDBC, 외부 HTTP, 파일 I/O 진입 시 `TASK_INTERRUPTIBLE` 상태로 전환되어 Wait Queue 이동. I/O 완료 IRQ 후 Runqueue로 복귀 | `/proc/PID/status`의 `State`, `ps aux`의 `STAT` 항목, `strace`로 Blocking 시스템 콜 확인, Thread Dump의 `WAITING`/`BLOCKED` 상태 |
| **JVM Thread — Worker Thread 점유** | JVM / Container | forward/include 동안 동일 Worker Thread가 계속 점유. Thread Pool에 반환 불가. Blocking I/O가 전파되면 Pool의 모든 Worker Thread가 점유 상태로 고갈 가능. Connection Pool Exhaustion과 연계 | Tomcat Manager App, `jstack`의 `WAITING` 상태 스레드 수, `Thread Dump`에서 `ApplicationDispatcher.invoke` 호출 위치 확인 |
| **Connection Pool Exhaustion** | App | forward 대상 Servlet에서 JDBC Connection Pool 사용 시, Worker Thread 점유 상태에서 Connection 대기가 겹쳐 이중 고갈 발생. HikariCP 기준 `connectionTimeout` 초과 시 예외 발생 | HikariCP `HikariPoolMXBean.getActiveConnections()`, `jmx_exporter`, Micrometer `hikaricp.connections.active`, `jstack`에서 `HikariPool.getConnection` 대기 확인 |
| **JVM Stack Frame** | JVM | forward()는 JVM Thread Stack에 호출 프레임을 누적. 순서: Container Worker Invocation → ServletA.service() → ApplicationDispatcher.forward() → TargetServlet.service(). Recursive Forward 시 Stack Frame 무한 증가로 StackOverflowError 발생 | `jstack`의 Thread Stack Depth, `-Xss` 설정 확인, `java.lang.StackOverflowError` 로그, APM 트레이스의 호출 깊이 |
| **JVM Heap — Request/Response 객체** | JVM | HttpServletRequest, HttpServletResponse 객체는 Heap에 단일 인스턴스로 존재. forward/include 시 새 객체를 생성하지 않고 동일 참조를 전달. Request Wrapper(RequestFacade, ApplicationHttpRequest) 객체는 forward 시 추가 할당 | `jstat -gc PID`, `jmap -histo PID`, Heap Dump에서 `ApplicationHttpRequest` 인스턴스 수, `jvm.gc.live.data.size` 메트릭 |
| **TLAB (Thread-Local Allocation Buffer)** | JVM | Worker Thread별로 TLAB에서 소규모 객체(Request Attribute, Wrapper 객체 등)를 빠르게 할당. TLAB 소진 시 Eden 영역에서 재할당. forward/include 요청 빈도가 높을 경우 TLAB Refill 빈도 증가 | `-Xlog:tlab`, `jstat -gcnew PID`의 `TT`(Tenuring Threshold), `perf stat -e jvm:tlab_*` |
| **GC — Minor GC 압력** | JVM | 각 HTTP 요청마다 Request Attribute 객체, Request Wrapper, 내부 라우팅 중간 객체가 단명 객체로 생성. 요청 처리량 증가 시 Eden 영역 소진 속도 증가, Minor GC 빈도 상승 | `jstat -gcutil PID 1000`, `GC log`의 `[GC pause (young)]`, `jvm.gc.pause` 메트릭, Heap Profiler |
| **JIT Compilation (C1/C2)** | JVM | ApplicationDispatcher.forward(), TargetServlet.service() 등 반복 호출 메서드는 C1(클라이언트 컴파일러)에서 최적화 후 C2(서버 컴파일러)가 인라이닝 및 루프 최적화 적용. 초기 요청에서 인터프리터 실행으로 Latency 높음 | `-XX:+PrintCompilation`, `jitwatch`, `-XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining`, `async-profiler`의 `itimer` 모드 |
| **Safepoint** | JVM | GC, 클래스 언로딩, Thread Dump 수집 시 Safepoint 진입 대기 발생. forward 처리 중 Safepoint 대기가 길어지면 전체 Worker Thread가 일시 정지. `Time to Safepoint` 지표가 높을 경우 장기 루프 또는 JNI 구간 확인 | `-XX:+PrintSafepointStatistics`, `-Xlog:safepoint`, JVM 로그의 `Application time` vs `Stop-the-world time`, `jfr` |
| **ClassLoader / Metaspace** | JVM | forward 대상 Servlet이나 JSP의 클래스 메타데이터가 Metaspace에 로드되어 있어야 실행 가능. JSP 최초 접근 시 Servlet 코드 변환 → javac 컴파일 → ClassLoader 로드 순서로 초기 Latency 급증. 웹 애플리케이션 재배포 시 이전 ClassLoader가 GC 대상이 되지 않으면 Metaspace Leak 발생 | `-XX:MetaspaceSize`, `-XX:MaxMetaspaceSize`, `jcmd PID VM.metaspace`, `jmap -clstats PID`, Heap Dump에서 ClassLoader 참조 확인 |
| **CPU 캐시 / Cache Line** | Hardware | 동일 Worker Thread가 forward 흐름 전체를 실행하므로 L1/L2 캐시에 코드 및 데이터가 유지됨. 단, Blocking I/O 후 다른 CPU 코어에서 재스케줄 시 Cache Miss 증가. 다수 Worker Thread가 동일 Request Attribute Map을 경합하면 Cache Line Thrashing 발생 가능 | `perf stat -e cache-misses,cache-references`, `perf c2c` (Cache Line 공유 분석), `numactl --hardware` |
| **NUMA 메모리 접근** | Hardware + Kernel | NUMA 환경에서 Worker Thread가 재스케줄되어 원래 CPU 노드와 다른 노드에서 실행될 경우 Remote Memory 접근 발생. HttpServletRequest, HttpServletResponse 객체가 원래 노드의 메모리에 할당되어 있다면 원격 접근 Latency 증가 | `numastat -p PID`, `perf stat -e node-load-misses,node-store-misses`, `numactl --localalloc`, `/proc/PID/numa_maps` |
| **TLB / HugePage** | Hardware + Kernel | 요청 처리 중 Heap 객체 접근 시 TLB를 통해 가상-물리 주소 변환. Context Switch 시 TLB Flush 발생하여 다음 요청 초기 Cache Miss 증가. Transparent HugePage(THP) 활성화 시 TLB 엔트리 수 감소로 Miss 완화 가능 | `perf stat -e dTLB-load-misses,dTLB-store-misses`, `/proc/meminfo`의 `AnonHugePages`, `cat /sys/kernel/mm/transparent_hugepage/enabled`, `hugeadm --pool-list` |
| **CPU Branch Misprediction** | Hardware | ApplicationDispatcher 내부의 forward/include 상태 분기, Request Attribute 존재 여부 분기, Filter Chain 적용 여부 분기 등에서 Branch Misprediction 발생 가능. 고빈도 경로에서 C2 JIT가 분기 예측 최적화 적용 | `perf stat -e branch-misses,branch-instructions`, `async-profiler`의 CPU 핫스팟 분석, `-XX:+PrintCompilation`으로 인라이닝 확인 |
| **CPU Frequency Scaling (C-state / P-state)** | Hardware + Kernel | 요청 간 유휴 시간에 CPU가 낮은 P-state로 전환. 요청 급증 시 P-state 복귀 지연으로 초기 처리 Latency 증가. C-state 깊이가 깊을수록 복귀 비용 증가 | `cpupower frequency-info`, `turbostat`, `/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq`, `perf stat -e power:cpu_frequency` |
| **Memory Bandwidth Saturation** | Hardware | 다수 Worker Thread가 동시에 forward 처리하며 Heap 객체를 대량 접근할 경우 메모리 버스 포화. 특히 JSP 응답 버퍼, Request Attribute Map, Filter Chain 객체 반복 생성 시 메모리 대역폭 압박 | `perf stat -e mem-loads,mem-stores`, `pcm-memory` (Intel PCM), `sar -r`, `numastat`의 `numa_miss` |
| **Blocking I/O — Wait Queue** | Kernel | forward 대상 Servlet 내부에서 JDBC, 외부 HTTP Client, 파일 읽기 등 Blocking I/O 진입 시 `read()` / `connect()` 시스템 콜이 Blocking. task_struct가 `TASK_INTERRUPTIBLE` 상태로 Wait Queue에 진입. I/O 완료 시 IRQ → Wake-up → Runqueue 복귀 | `strace -p PID`의 Blocking 시스템 콜, `iotop`, `/proc/PID/wchan`(대기 중인 Kernel 함수), Off-CPU Flame Graph |
| **Off-CPU Time** | Kernel + JVM | RequestDispatcher 호출 후 대상 자원의 I/O 대기 시간은 On-CPU가 아닌 Off-CPU Time으로 측정됨. CPU Profiler로는 관찰 불가. forward 체인에서 발생하는 숨겨진 지연 식별에 Off-CPU 분석 필수 | `async-profiler -e wall -t`, `bpftrace`의 `offcputime.bt`, `perf record -e sched:sched_switch`, BCC의 `offcputime` |
| **IO Scheduler (blk-mq)** | Kernel | forward 대상이 정적 파일이거나 JSP 최초 컴파일 시 Disk I/O 발생. 블록 레이어에서 blk-mq가 I/O 요청을 큐잉하고 스케줄링. SSD 기준 NVMe의 멀티 큐(Multi-Queue) 구조로 병렬 처리 | `iostat -xz 1`, `blktrace`, `/sys/block/sda/queue/scheduler`, `cat /proc/diskstats`, `iotop -o` |
| **Dirty Page Writeback** | Kernel | forward 처리 중 로그 파일 쓰기, JSP 컴파일 결과 캐시 저장 등이 Dirty Page를 생성. `pdflush` / `kworker`가 주기적으로 Dirty Page를 Disk에 Writeback. 과도한 Dirty Page 누적 시 동기 Writeback으로 I/O Latency 급증 | `/proc/vmstat`의 `nr_dirty`, `sar -B`의 `pgpgin/pgpgout`, `sysctl vm.dirty_ratio`, `iostat`의 쓰기 대기 시간 |
| **Page Cache & Page Fault** | Kernel | 정적 파일 forward 시 Page Cache에 파일 데이터가 캐시되어 있으면 Disk I/O 없이 처리(Minor Fault). 캐시 미존재 시 `read()` 후 Disk에서 로드(Major Fault). JSP 최초 컴파일 결과도 Page Cache 적용 | `free -m`의 `buff/cache`, `vmstat`의 `pgfault` / `pgmajfault`, `/proc/vmstat`의 `pgpgin`, `fincore`로 파일 Cache 여부 확인 |
| **mmap — 정적 파일 전달** | App ↔ Kernel | Tomcat DefaultServlet이 정적 파일 전달 시 내부적으로 `sendfile()` 또는 `mmap()` + `write()` 사용 가능. `mmap` 사용 시 Page Fault를 통해 필요한 페이지만 로드. `sendfile()`은 Kernel 내부에서 직접 Socket으로 전송하여 User Space 복사 생략 | `/proc/PID/maps`의 파일 매핑 영역, `strace`의 `sendfile()` 호출, `vmstat`의 `pgfault`, `perf stat -e dTLB-load-misses` |
| **cgroup / CPU Throttling** | Kernel | Kubernetes 또는 컨테이너 환경에서 Tomcat Worker Thread가 cgroup CPU Quota를 소진하면 Throttling 발생. forward 처리 중 CPU 집약적 작업(JSP 컴파일, 직렬화 등) 시 Quota 소진으로 처리 지연 | `/sys/fs/cgroup/cpu.stat`의 `throttled_usec`, `cat /sys/fs/cgroup/cpu.max`, `kubectl top pod`, `cadvisor`의 `container_cpu_cfs_throttled_seconds_total` |
| **커널 스케줄러 (CFS)** | Kernel | CFS(Completely Fair Scheduler)가 vruntime 기반으로 Worker Thread들의 CPU 시간을 공정하게 분배. 다수 Worker Thread 경합 시 스케줄 대기 발생. forward 체인 전체가 동일 task_struct에서 실행되므로 스케줄 우선순위가 전체 체인에 영향 | `/proc/schedstat`, `perf sched latency`, `pidstat -u -w`, `vmstat`의 `r`(Runqueue 대기) |
| **PSI (Pressure Stall Information)** | Kernel | CPU, Memory, I/O 자원 부족 압력을 측정. forward 처리 중 Blocking I/O 전파로 Worker Thread 전체가 대기 상태일 때 `full` PSI 지표 상승. Kubernetes의 자원 부족 조기 감지에 활용 | `/proc/pressure/cpu`, `/proc/pressure/memory`, `/proc/pressure/io`, `sar --all`로 종합 확인 |
| **시그널 (Signal)** | Kernel → App | 비정상 Recursive Forward로 Worker Thread가 무한 루프에 빠진 경우 `SIGTERM` / `SIGKILL`로 강제 종료. JVM 내부에서 `SIGSEGV`는 HotSpot이 Catch하여 처리(NullPointerException 등 변환). forward 처리 중 Timeout 구현에 `SIGALRM` 또는 Thread interrupt 사용 가능 | `kill -l`, `/proc/PID/status`의 `SigPnd`, `SigBlk`, `strace -e signal`, JVM 로그의 Signal Handler |
| **Serialization / Deserialization 비용** | App | forward 시 Request Attribute에 복잡한 객체를 담아 전달할 경우, 대상 자원에서 역직렬화 비용 발생 가능. Session에 저장된 객체의 직렬화도 forward 흐름에서 간접 발생. 대형 Mutable 객체 공유 시 Deep Copy 또는 동기화 비용 발생 | `async-profiler`의 CPU Flame Graph에서 직렬화 메서드 확인, Heap Dump에서 중간 직렬화 버퍼 확인 |
| **Backpressure** | App / Container | 상위 forward 체인이 하위 Servlet의 처리 속도보다 빠르게 요청을 전달하면 Thread Pool 고갈로 자연적 Backpressure 발생. 명시적 Backpressure 구현 없이 forward 체인이 깊어질수록 Thread 점유 기간 증가 | Tomcat `maxThreads` vs 활성 스레드 수, `jstack`의 WAITING 스레드 비율, Response Time P99 증가 추세 |
| **Circuit Breaker** | App | forward 대상 자원이 외부 의존성(DB, API)에 연결된 경우, 반복 실패 시 Circuit Breaker가 열려 즉각 실패를 반환하여 Worker Thread 고갈 방지. Resilience4j, Hystrix 등 적용 | Resilience4j `CircuitBreaker.getState()`, `resilience4j.circuitbreaker.state` 메트릭, APM의 Circuit Open 이벤트 |
| **Retry Storm** | App | forward 대상 자원 실패 시 재시도 로직이 포함된 경우, 다수 Worker Thread가 동시에 재시도하면 Retry Storm 발생. 동일 Worker Thread에서 forward 체인이 재시도 루프와 결합될 경우 Thread 점유 시간 기하급수적 증가 | APM의 retry 횟수 메트릭, `jstack`에서 retry sleep 상태 확인, Error Rate 대비 요청 수 비율 |
| **Response Committed 제약** | Servlet Container | `response.flushBuffer()` 또는 일정 크기 이상 Body 작성 후 forward() 호출 시 응답이 이미 커밋된 상태. Tomcat은 `IllegalStateException` 또는 내부 경고를 발생. 응답 Buffer 크기는 `server.tomcat.max-http-response-header-size` 관련 설정에 의존 | `response.isCommitted()` 로그, Tomcat `catalina.log`의 `IllegalStateException`, APM의 500 에러 추적 |
| **Filter Chain 재진입** | Servlet Container | forward/include 시 대상 경로에 매핑된 Filter Chain이 재진입될 수 있음. `DispatcherType.FORWARD`, `DispatcherType.INCLUDE`에 매핑된 Filter는 추가 실행. 불필요한 Filter 재진입은 처리 시간 증가 | Servlet 스펙 `DispatcherType` 설정 확인, Filter 내부 로그로 호출 횟수 추적, APM Span의 Filter 구간 확인 |
| **eBPF Map / 커널 관찰** | Kernel → App | eBPF 프로그램으로 `ApplicationDispatcher.forward()` 진입, Blocking 시스템 콜 발생, Wait Queue 이동 시점을 Kernel 수준에서 무수정 관찰 가능. Worker Thread의 Off-CPU 구간 정밀 측정 | `bpftrace`, `bcc`의 `offcputime`, `Cilium`, `Pixie`, `kubectl trace`, `perf probe`로 JVM 내부 함수 추적 |

---

## 계층별 요약

### Hardware Layer
- NIC DMA, IRQ, SoftIRQ(NET_RX)로 패킷 수신
- CPU 캐시(L1/L2/L3), TLB, Branch Predictor, NUMA, Memory Bandwidth가 forward 처리 성능에 영향
- CPU C-state/P-state 전환 지연이 초기 요청 Latency에 영향

### OS Kernel Layer
- task_struct는 forward/include 전 과정에서 단일 인스턴스로 유지
- Blocking I/O 진입 시 Wait Queue 이동, I/O 완료 IRQ 후 Runqueue 복귀
- CFS Scheduler, cgroup Throttling, PSI, TCP 버퍼(sk_buff), IO Scheduler(blk-mq), Page Cache, Dirty Writeback이 처리 전반에 개입

### JVM Runtime Layer
- Worker Thread가 JVM Thread Stack에 forward 호출 프레임을 누적하며 실행
- 단명 객체(Request Attribute, Wrapper)의 TLAB 할당, Minor GC 압력 발생
- JIT(C1/C2) 최적화, Safepoint, Metaspace/ClassLoader가 처리 효율에 영향

### Application Layer
- forward/include는 새 Thread를 생성하지 않음 — 동일 Worker Thread, 동일 task_struct
- Request/Response 객체는 Heap에서 단일 인스턴스로 공유
- 장애는 Recursive Forward, Response 커밋 후 forward, Blocking I/O 전파, Connection Pool Exhaustion, Filter Chain 재진입 과다에서 발생

---

## SRE 핵심 장애 시나리오

| 장애 유형 | 계층 | 관찰 방법 |
|-----------|------|-----------|
| Worker Thread Pool 고갈 | JVM / Kernel | `jstack` WAITING 스레드 수, Tomcat Active Threads, `/proc/PID/status` Threads |
| Recursive Forward → StackOverflowError | JVM | `java.lang.StackOverflowError` 로그, APM 호출 깊이 |
| Blocking I/O 전파 → Off-CPU 증가 | Kernel | Off-CPU Flame Graph, `/proc/PID/wchan`, `strace` Blocking 시스템 콜 |
| JSP 최초 컴파일 → P99 Latency 급증 | JVM / Kernel | `Metaspace` 증가, `blktrace` Disk I/O, 첫 요청 APM Span 길이 |
| Response Committed 후 forward | App | `IllegalStateException` 로그, `response.isCommitted()` 확인 |
| cgroup CPU Throttling | Kernel | `/sys/fs/cgroup/cpu.stat`의 `throttled_usec`, `cadvisor` |
| TCP Accept Queue 포화 | Kernel | `ss -lnt`의 `Recv-Q`, `netstat -s`의 `TCPBacklogDrop` |
| Connection Pool Exhaustion | App | HikariCP `activeConnections`, `jstack`에서 Pool 대기 확인 |

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*