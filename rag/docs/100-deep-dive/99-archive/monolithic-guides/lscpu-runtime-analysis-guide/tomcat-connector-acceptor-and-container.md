# Tomcat Connector, Acceptor, Container 계층 구조 (E2E 분석 적용됨)

## 개요

Tomcat은 하나의 JVM 프로세스 안에서 동작하는 Java 기반 WAS입니다.

외부 HTTP 요청은 먼저 Tomcat의 `Connector`로 들어오고, 이후 Tomcat 내부의 `Container` 계층을 따라 최종 Servlet 또는 Spring `DispatcherServlet`까지 전달됩니다.

```
Client
  ↓  [TCP SYN → NIC IRQ → sk_buff → TCP Stack → Accept Queue]
Connector
  ↓  [Acceptor Thread / accept() syscall]
Acceptor / Poller / Processor
  ↓  [epoll_wait() / Selector → HTTP 파싱 → Tomcat Request 객체]
Adapter
  ↓  [Container 계층 진입 / Worker Thread 실행]
Engine
  ↓  [Mapper: Host Header 분석]
Host
  ↓  [Mapper: Context Path 분석]
Context
  ↓  [Mapper: Servlet URL Pattern 분석]
Wrapper
  ↓  [Servlet.service() → JIT Compiled Native Code]
Servlet / DispatcherServlet
```

---

## 1. Connector의 역할

Connector는 클라이언트와 Tomcat 내부 Servlet Container를 연결하는 네트워크 진입 계층입니다.

주요 역할은 다음과 같습니다.

- 지정된 포트에서 HTTP 요청 수신
- TCP 연결 관리
- HTTP 프로토콜 파싱
- Raw Byte 데이터를 Tomcat 내부 Request/Response 객체로 변환
- Servlet Container로 요청 전달

```
Network Byte Stream (sk_buff)
  ↓
[Kernel: NIC DMA → Ring Buffer → SoftIRQ → TCP/IP Stack]
  ↓
Connector
  ↓
HTTP Request Parsing
  ↓
Tomcat Request / Response 객체 (JVM Heap)
  ↓
Container
```

즉, Connector는 네트워크 계층과 Java Servlet 실행 계층 사이의 변환 지점입니다.

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Hardware | NIC가 패킷 수신 후 DMA로 Ring Buffer에 적재, HW IRQ 발생 | `ethtool -S eth0` (rx_missed_errors), `/proc/interrupts` |
| Kernel (IRQ) | CPU 실행 중단 → IRQ Handler → `net_rx_action` SoftIRQ 예약 | `mpstat -I ALL` (`%irq`, `%soft`), `/proc/softirqs` |
| Kernel (SoftIRQ) | `ksoftirqd` 또는 IRQ tail에서 `net_rx_action` 실행 → `sk_buff` 처리 → TCP Stack | `mpstat` `%soft` 급증, `sar -n EDEV` |
| Kernel (TCP Stack) | SYN Queue(incomplete connections) 처리, 3-way handshake 완료 후 Accept Queue(complete connections)로 이동 | `ss -lntp`, `netstat -s` (`SYNs to LISTEN sockets dropped`), `/proc/net/tcp` |
| Kernel → App | Tomcat `accept()` 시스템 콜로 Accept Queue에서 연결 꺼냄, fd 반환 | `strace -p <pid> -e accept4`, `/proc/net/sockstat` |
| JVM (Connector) | `ServerSocketChannel.accept()` → JVM NIO → OS fd 래핑 | `jstack` (Acceptor Thread 상태), JMX `Connector` MBean |

---

## 2. Connector의 내부 구성

```
Connector
  ├── Endpoint
  │   ├── Acceptor          ← accept() 시스템 콜, TCP 연결 수락
  │   └── Poller            ← epoll/Selector 기반 이벤트 감시
  ├── Processor             ← HTTP 프로토콜 파싱, Request 객체 생성
  └── Adapter               ← Container 계층 진입점
```

---

## 2.1 Endpoint

Endpoint는 실제 네트워크 연결을 처리하는 하위 구성 요소입니다.

역할은 다음과 같습니다.

- Server Socket 생성 (`ServerSocketChannel.bind()` → `bind()` syscall)
- Port Listen (`listen()` syscall, backlog 설정 → Kernel TCP Listen Queue 크기 결정)
- TCP 연결 수락
- Socket 이벤트 감시
- Worker Thread로 처리 위임

Tomcat NIO Connector에서는 `NioEndpoint`가 사용됩니다.

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel | `listen(fd, backlog)` syscall → TCP Listen Backlog 설정. `net.core.somaxconn` 및 `net.ipv4.tcp_max_syn_backlog` 커널 파라미터가 실제 Queue 크기 결정 | `sysctl net.core.somaxconn`, `ss -lntp` (Recv-Q = Accept Queue 대기 수) |
| Kernel | Accept Queue 포화 시 새 연결 DROP 또는 RST 발생 (SYN Cookie 동작 여부에 따라 다름) | `netstat -s \| grep "listen queue"`, `dmesg \| grep "syn flood"` |
| JVM | `NioEndpoint`가 `ServerSocketChannel` 생성, `selector` 등록 | JMX `maxConnections` 실시간 확인, Tomcat Manager `/status` |
| OS Thread | Acceptor, Poller 각각 별도 `task_struct`로 존재 | `ps -eLf \| grep tomcat`, `/proc/<pid>/task/` |

---

## 2.2 Acceptor

Acceptor는 새로운 TCP 연결을 수락하는 전용 스레드입니다.

주요 역할은 다음과 같습니다.

- 커널의 Accept Queue에서 연결을 꺼냄
- `accept()` / `accept4()` 시스템 콜 수행
- 새 `SocketChannel` 생성
- 직접 비즈니스 로직을 처리하지 않음
- 연결을 Poller 또는 후속 처리 계층으로 넘김

```
Kernel TCP Accept Queue
  ↓  [accept4() syscall: User Mode → Kernel Mode (Ring 3 → Ring 0)]
Tomcat Acceptor Thread (task_struct)
  ↓  [SocketChannel 생성, fd → JVM NIO Channel 래핑]
Poller 등록 (Selector.register())
```

Acceptor는 요청 본문을 처리하지 않습니다. 오직 새로운 연결을 받아 Tomcat 내부 처리 흐름으로 넘기는 역할만 수행합니다.

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel | `accept4()` syscall: User Mode → Kernel Mode 전환 (Trap), 새 fd 발급 | `strace -e accept4 -p <pid>`, `perf trace -p <pid>` |
| Kernel | Acceptor가 `accept()`를 호출할 때까지 Accept Queue에 완료된 연결이 대기. Queue 초과 시 신규 연결 거부 | `netstat -s \| grep "connections refused"`, `ss -lntp` (Recv-Q 확인) |
| OS Scheduling | Acceptor는 단일 `task_struct`. Accept Queue가 비면 `epoll_wait` 또는 `accept` 내부에서 Blocking → Wait Queue 이동, CPU 반환 | `pidstat -w -p <pid>` (cswch/s: 자발적 Context Switch) |
| JVM | Acceptor Thread는 JVM Thread → Native Thread → `task_struct`. `maxConnections` 초과 시 Acceptor 내부 `LimitLatch`에서 Blocking | `jstack` (Acceptor Thread: `WAITING` 또는 `RUNNABLE`), JMX `currentThreadsBusy` |
| SRE 병목 | Acceptor 자체 지연보다 Accept Queue 포화가 주요 원인. `acceptCount` 설정이 너무 작으면 연결 거부 발생 | Tomcat `acceptCount` 설정값, `netstat -s \| grep overflow` |

---

## 2.3 Poller

현대 Tomcat NIO 구조에서는 Acceptor가 연결을 받은 뒤 Poller가 Socket 이벤트를 감시합니다.

Poller는 Java NIO `Selector` → JVM NIO → OS `epoll` 구조와 연결됩니다.

```
SocketChannel (fd)
  ↓  [Selector.register(channel, OP_READ)]
Poller Thread (task_struct)
  ↓  [epoll_wait() syscall → Kernel이 이벤트 발생 시 Wake-up]
Read 가능 이벤트 감지
  ↓
Worker Thread Pool에 처리 위임 (Executor.execute())
```

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel | `epoll_ctl(EPOLL_CTL_ADD)`: Socket fd를 epoll 인스턴스에 등록. `epoll_wait()`: 이벤트 발생 시 반환. Kernel 내부 `ep_item` 구조체로 관리 | `strace -e epoll_ctl,epoll_wait -p <pid>`, `/proc/<pid>/fdinfo` |
| Kernel | Socket 수신 데이터가 `sk_buff`로 도달 → TCP Stack이 `socket receive buffer`에 적재 → `epoll` 이벤트 발생 → Poller wake-up | `ss -tnp` (Recv-Q: socket buffer에 쌓인 데이터), `sysctl net.ipv4.tcp_rmem` |
| Kernel | `SO_RCVBUF` / `net.ipv4.tcp_rmem` 설정이 Socket 수신 버퍼 크기 결정. 버퍼 포화 시 TCP Receive Window 0 → 클라이언트 전송 중단 | `ss -tnmp` (mem 컬럼), `netstat -s \| grep "receive buffer"` |
| OS Thread | Poller Thread는 `epoll_wait()` 호출 중 Blocking 상태. 이벤트 없으면 `task_struct`는 Wait Queue에 위치, CPU 미점유 | `pidstat -w` (minflt, majflt), `/proc/<pid>/status` (State: S) |
| JVM | Java NIO `Selector.select()` → JVM이 내부적으로 `epoll_wait()` syscall 호출. `selectedKeys()` 반환 후 이벤트 처리 | `jstack` (Poller Thread: `sun.nio.ch.EPollSelectorImpl`) |
| SRE 병목 | Poller 수가 부족하거나 이벤트 처리 루프가 느리면 Socket 이벤트 처리 지연 → Latency 증가. `RPS/RFS` 미설정 시 모든 패킷이 단일 CPU에서 처리되어 SoftIRQ 병목 발생 | `/proc/irq/<N>/smp_affinity`, `ethtool -L eth0 combined <N>` (RSS 큐 수 조정) |

---

## 2.4 Processor

Processor는 Socket에서 읽은 바이트 데이터를 HTTP 프로토콜에 맞게 해석합니다.

```
Raw HTTP Bytes (Socket read buffer)
  ↓  [read() / recv() syscall → User Space Buffer]
Processor (Worker Thread)
  ↓  [HTTP Method / URI / Header / Body 파싱]
  ↓  [JVM: byte[] → String 변환, Header Map 생성]
Tomcat Request 객체 생성 (JVM Heap)
```

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel | `read()` 또는 `recv()` syscall: Kernel Socket 수신 버퍼(sk_buff)에서 User Space Buffer로 데이터 복사. Zero-Copy 미사용 시 매 요청마다 복사 발생 | `strace -e read,recv -p <pid>`, `perf stat -e syscalls:sys_enter_read` |
| JVM (Memory) | Processor가 읽은 바이트는 JVM Heap의 `byte[]` 버퍼에 적재. HTTP 헤더 파싱 시 다수의 `String` 객체 생성 → Minor GC 유발 가능 | `jstat -gcutil <pid>`, GC log (`-Xlog:gc*`) |
| JVM (TLAB) | Worker Thread가 String/Header 객체를 TLAB(Thread-Local Allocation Buffer)에서 빠르게 할당. TLAB 소진 시 Eden 영역 직접 할당 or GC 유발 | `-XX:+PrintTLAB`, `jstat -gc` (Eden 사용률), GC Allocation Stall 모니터링 |
| JVM (JIT) | `Http11Processor.parse()` 메서드가 충분히 호출되면 JIT C2 컴파일러가 Native Code로 컴파일 → 이후 파싱 비용 대폭 감소. 초기 요청은 Interpreter 실행으로 느림 | `-XX:+PrintCompilation`, `jitwatch`, `perf record -g java` |
| CPU | HTTP 파싱 중 반복적인 `if-else` 분기 처리 → Branch Misprediction 발생 가능. JIT 최적화 후 인라이닝으로 완화됨 | `perf stat -e branch-misses -e branches`, `toplev` (Branch Misprediction Bound) |
| SRE 병목 | `processorCache` 설정으로 Processor 객체 재사용. 캐시 부족 시 매 요청 객체 생성 비용 발생. Keep-Alive 요청은 동일 Processor 재사용 | Tomcat JMX `processorCache`, GC log에서 단기 객체 과다 생성 여부 확인 |

---

## 2.5 Adapter

Adapter는 Connector 계층과 Container 계층을 연결합니다.

Processor가 해석한 요청을 Tomcat 내부 Container가 이해할 수 있는 형태로 전달합니다.

```
Processor (Tomcat 내부 Request 객체)
  ↓
CoyoteAdapter.service()
  ↓
[Request → HttpServletRequest 래핑]
  ↓
Container (Engine.pipeline.invoke())
```

즉, Adapter는 네트워크 프로토콜 처리 결과를 Servlet 실행 계층으로 넘기는 경계 지점입니다.

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| JVM | `CoyoteAdapter.service()` 호출 시점부터 동일 Worker Thread가 Container 계층 전체를 실행. 별도 스레드 전환 없음 | `jstack` (Worker Thread Stack: `CoyoteAdapter.service` 프레임 확인) |
| JVM (Heap) | `org.apache.coyote.Request` → `org.apache.catalina.connector.Request` 래핑. `HttpServletRequest` 인터페이스 노출 | Heap Dump 분석 (`jmap`, MAT): Request/Response 객체 과다 여부 확인 |
| JVM (Off-CPU) | Adapter → Container 전달 자체는 순수 Java 메서드 호출. Off-CPU Time은 이후 Blocking I/O, Lock 대기에서 발생 | `async-profiler -e wall` (Off-CPU Flame Graph), `perf sched` |

---

## 3. Container 계층 구조

Tomcat 내부의 Servlet 실행 영역은 Container 계층으로 구성됩니다.

```
Engine     (JVM Heap: StandardEngine 객체)
  ↓
Host       (JVM Heap: StandardHost 객체)
  ↓
Context    (JVM Heap: StandardContext 객체)
  ↓
Wrapper    (JVM Heap: StandardWrapper 객체)
```

각 계층은 모두 JVM Heap에 존재하는 Java 객체이며, Linux Kernel이 직접 스케줄링하는 대상이 아닙니다. Kernel은 이를 실행하는 Worker Thread의 `task_struct`를 스케줄링합니다.

---

## 4. Engine

Engine은 하나의 Tomcat Service 안에서 최상위 Container입니다.

```
Connector (CoyoteAdapter)
  ↓  [Engine.getPipeline().getFirst().invoke()]
Engine
  ↓  [StandardEngineValve: Host 선택 → Host.getPipeline().invoke()]
Host 선택
```

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| JVM | Engine은 `StandardEngine` 객체. `Pipeline`과 `Valve` 체인으로 구성. `StandardEngineValve`가 최종 Valve로 Host를 선택하여 위임 | `jstack` (Worker Thread Stack: `StandardEngineValve.invoke`) |
| JVM (JIT) | `Pipeline.invoke()` 호출이 반복되면 JIT C1/C2가 인라이닝 최적화 적용. Valve 체인이 짧을수록 JIT 최적화 효율 상승 | `-XX:+PrintInlining`, JMH 마이크로벤치마크 |
| OS Thread | Engine 처리 전 과정은 동일 Worker `task_struct` 내 실행. CFS 스케줄러가 `task_struct`의 `vruntime`을 갱신하며 CPU Time 할당 | `/proc/<pid>/schedstat` (run_delay 확인), `perf sched latency` |

---

## 5. Host

Host는 가상 호스트를 의미합니다. 하나의 Tomcat 인스턴스에서 여러 도메인을 처리할 수 있도록 분기합니다.

```
HTTP Host Header: api.example.com
  ↓  [Mapper.map(): Host Header → Host 객체 매핑]
Host 선택 (api.example.com → StandardHost 객체)
```

예시:

```
api.example.com   → Host A (StandardHost)
admin.example.com → Host B (StandardHost)
```

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| JVM | `Mapper` 객체가 Host Header 문자열을 내부 `MappingData`에 매핑. 내부적으로 배열 이진 탐색 사용 → O(log n) | Heap Dump: `MappingData` 객체 확인 |
| JVM | `defaultHost` 설정 시 매핑 실패한 요청이 기본 Host로 라우팅. 미설정 시 400 오류 가능 | Tomcat `server.xml` `defaultHost` 설정 확인 |
| OS Thread | Host 선택 자체는 메서드 호출. CPU 비용 미미. 단, 수백 개의 Host가 있을 경우 Mapper 탐색 비용 증가 가능 | `perf record -g` (Host 매핑 Hot Path 확인) |

---

## 6. Context

Context는 하나의 웹 애플리케이션 단위입니다. 일반적으로 WAR 하나 또는 Spring Boot 애플리케이션 하나가 하나의 Context에 대응됩니다.

역할은 다음과 같습니다.

- Context Path 관리
- Servlet Mapping 관리
- Filter 관리
- Listener 관리
- Web Application ClassLoader 관리
- 애플리케이션별 설정 관리

```
URI: /payment/approve
  ↓  [Mapper.map(): 최장 일치 (Longest Prefix Match)]
Context Path: /payment → StandardContext 객체
```

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| JVM (ClassLoader) | 각 Context는 독립 `WebappClassLoader`를 보유. 클래스 로딩 시 Parent Delegation 이후 Webapp 로컬 탐색. Context 리로드 시 기존 ClassLoader GC 대상이 되어야 함 | `jmap -clstats <pid>`, MAT (ClassLoader leak 탐지), `jcmd <pid> VM.classloaders` |
| JVM (ClassLoader Leak) | `ThreadLocal`, Static 필드, JDBC Driver 등이 이전 ClassLoader 참조를 유지하면 `Metaspace` / Heap 누수 발생. Context 리로드 반복 시 `OutOfMemoryError: Metaspace` | GC log에서 Metaspace 증가 추이, MAT에서 `WebappClassLoader` retained heap 분석 |
| JVM (Metaspace) | Context 로드 시 관련 클래스 메타데이터가 `Metaspace`에 적재. Context 언로드 후 GC가 ClassLoader를 수거해야 Metaspace 반환 | `jstat -gcmetacapacity`, `-XX:MetaspaceSize`, `-XX:MaxMetaspaceSize` |
| JVM (Filter Chain) | 요청마다 `FilterChain`을 통과. Filter 수가 많거나 개별 Filter 처리 시간이 길면 전체 요청 Latency 증가 | `jstack` (Filter 관련 Stack Frame 빈도), APM (Filter별 소요 시간) |
| JVM (JIT Safepoint) | Context 초기화 중 다수 클래스 로딩 → JIT Deoptimization 및 Safepoint 빈도 증가 가능. 초기 요청 응답 지연의 원인 | `-XX:+PrintSafepointStatistics`, `-Xlog:safepoint` (JDK 11+) |
| SRE 병목 | 최초 Context 로드 시 모든 클래스 초기화 + JIT Warm-up 필요. Kubernetes Rolling Update 시 readinessProbe 지연 원인 | Actuator `/actuator/health`, JVM Warm-up 전략 (초기 트래픽 조절) |

---

## 7. Wrapper

Wrapper는 하나의 Servlet 인스턴스를 관리하는 가장 하위 Container입니다.

역할은 다음과 같습니다.

- 특정 Servlet 인스턴스 관리
- Servlet 초기화 (`init()`)
- Servlet `service()` 호출
- Servlet Lifecycle 관리

```
StandardWrapper
  ↓  [StandardWrapperValve.invoke()]
  ↓  [ApplicationFilterChain 구성 → Filter 순차 실행]
  ↓  [Servlet.service() 호출]
DispatcherServlet.service()
  ↓
HandlerMapping → HandlerAdapter → Controller
```

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| JVM | `StandardWrapperValve.invoke()` 내부에서 `ApplicationFilterChain` 생성 후 Servlet 호출. `SingleThreadModel` 미사용 시 Servlet 인스턴스는 공유 → Thread-Safe 구현 필요 | `jstack` (Worker Thread Stack: `DispatcherServlet.doDispatch`) |
| JVM (JIT) | `DispatcherServlet.doDispatch()` 및 Controller 메서드가 JIT C2 컴파일 대상. 충분한 호출 횟수(기본 10,000회) 이후 최적화 완료 | `-XX:CompileThreshold`, `-XX:+PrintCompilation`, JFR `jdk.Compilation` 이벤트 |
| JVM (Serialization) | `@ResponseBody` / `@RestController`에서 Jackson `ObjectMapper`가 Java 객체 → JSON 직렬화. 대형 응답 객체 직렬화 시 CPU 비용 + Heap 사용 급증 | `async-profiler` (Jackson CPU 점유율), JFR Allocation 프로파일링 |
| JVM (Finalization) | `HttpServletResponse` 처리 후 버퍼 flush → `write()` syscall. Finalization Queue 지연이 있을 경우 Response 객체 해제 지연 가능 | `-XX:+PrintReferenceGC`, GC log에서 Finalization 시간 확인 |
| OS | Servlet 실행 중 Blocking I/O 발생 시 Worker Thread `task_struct`가 Wait Queue로 이동. CPU는 다른 `task_struct` 실행 | `pidstat -u -w -p <pid>` (iowait, cswch), `iotop` |

---

## 8. Tomcat 내부 라우팅 흐름

요청은 다음 순서로 전달됩니다.

```
Client Request
  ↓  [NIC → DMA → sk_buff → SoftIRQ → TCP Stack → Accept Queue]
Connector
  ↓  [accept4() syscall → SocketChannel 생성]
Acceptor
  ↓  [epoll_ctl() 등록]
Poller
  ↓  [epoll_wait() → 이벤트 감지 → Worker Thread 위임]
Processor
  ↓  [read() syscall → HTTP 파싱 → Tomcat Request 객체]
Adapter
  ↓  [CoyoteAdapter.service() → Container 진입]
Engine
  ↓  [StandardEngineValve → Mapper Host 매핑]
Host
  ↓  [StandardHostValve → Mapper Context 매핑]
Context
  ↓  [StandardContextValve → Filter Chain 진입]
Wrapper
  ↓  [StandardWrapperValve → ApplicationFilterChain → Servlet.service()]
Servlet / DispatcherServlet
  ↓
HandlerMapping → Controller → Business Logic
```

### Worker Thread Stack 실체 (JVM Thread Stack)

```
Thread Stack (Worker Thread)
  ├── Controller.method()
  ├── HandlerAdapter.handle()
  ├── DispatcherServlet.doDispatch()
  ├── ApplicationFilterChain.doFilter()
  ├── StandardWrapperValve.invoke()
  ├── StandardContextValve.invoke()
  ├── StandardHostValve.invoke()
  ├── StandardEngineValve.invoke()
  └── CoyoteAdapter.service()
```

---

## 9. Mapper의 역할

Tomcat 내부에는 요청을 어느 Host, Context, Wrapper로 보낼지 결정하는 `Mapper` 컴포넌트가 있습니다.

```
HTTP Host Header + Request URI
  ↓  [Mapper.map() 단일 호출로 Host/Context/Wrapper 동시 결정]
  ↓  [내부: 배열 이진 탐색 → O(log n)]
MappingData 객체에 결과 기록
  ↓
Host / Context / Wrapper 결정
```

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| JVM | `Mapper`는 불변에 가까운 읽기 전용 구조. 배포(Context 추가/제거) 시 `MapperListener`가 동적으로 갱신 | JMX `MBeanServer` (`Catalina:type=Mapper`), `jstack` (MapperListener 동작 확인) |
| JVM (Memory) | `MappingData`는 요청마다 Worker Thread TLAB에서 할당. 단명 객체로 Minor GC 대상 | GC log (Allocation Rate), `-Xlog:gc+heap` |
| CPU (Cache) | Mapper 내부 배열이 CPU L1/L2 캐시에 상주하면 탐색 비용 최소화. 매우 많은 Context/Wrapper 등록 시 Cache Miss 증가 가능 | `perf stat -e cache-misses -e L1-dcache-loads` |

---

## 10. Host Routing

Host Routing은 HTTP `Host` Header를 기반으로 수행됩니다.

```
Host: api.example.com
  ↓  [Mapper.map(): Host Header 문자열 → Host 배열 이진 탐색]
Host(api.example.com) 선택
```

일치하는 Host가 없으면 `defaultHost`로 요청이 전달될 수 있습니다.

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| JVM | Host 이름 비교는 `String.equalsIgnoreCase()` 또는 내부 최적화된 배열 비교. JIT 최적화 후 Intrinsic으로 처리 가능 | `-XX:+PrintInlining` (String 비교 Intrinsic 여부) |
| JVM | 잘못된 Host Header 또는 IP 직접 접근 시 defaultHost 또는 400 오류. 보안/라우팅 정책 검토 필요 | Access Log (Host Header 분포 분석), WAF/L7 LB 설정 확인 |

---

## 11. Context Routing

Context Routing은 URI의 앞부분인 Context Path를 기준으로 수행됩니다.

```
URI: /payment/approve
  ↓  [Mapper.map(): Longest Prefix Match 알고리즘]

등록된 Context:
  /
  /payment
  /payment-admin

→ /payment Context 선택 (최장 일치)
```

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| JVM | Longest Prefix Match는 정렬된 배열에서 이진 탐색 후 역방향 스캔으로 구현. Hot Path → JIT C2 최적화 대상 | `async-profiler` (Mapper Hot Path), JFR Method Profiling |
| JVM (Context 초기화) | Context 최초 활성화 시 `ServletContainerInitializer`, `@WebServlet`, Spring `DispatcherServlet` 등 초기화 수행. 초기화 완료 전 요청 수신 시 503 반환 가능 | Tomcat Access Log (503 분포), K8s readinessProbe 설정 |

---

## 12. Wrapper Routing

Wrapper Routing은 최종적으로 어떤 Servlet이 요청을 처리할지 결정합니다.

```
Servlet Mapping 우선순위:
  1. Exact Match      (/api/payment/approve → PaymentServlet)
  2. Path Match       (/api/* → ApiServlet)
  3. Extension Match  (*.jsp → JspServlet)
  4. Default Servlet  (/ → DefaultServlet)
```

Spring Boot에서는 대부분의 요청이 `DispatcherServlet`으로 매핑됩니다.

```
/* 또는 /
  ↓
DispatcherServlet
  ↓  [HandlerMapping → @RequestMapping URL 패턴 매핑]
Controller Method
```

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| JVM | `StandardWrapper`는 Servlet 인스턴스를 싱글톤으로 관리. `loadOnStartup` 설정으로 초기화 시점 제어 | `jstack` (Servlet `init()` 중 지연 확인) |
| JVM (Connection Pool) | Controller에서 DB 조회 시 JDBC Connection Pool 사용. Pool 고갈(Connection Pool Exhaustion) 시 Worker Thread Blocking → Thread Pool 전체 점유 | HikariCP MBean (`HikariPool-1`): `activeConnections`, `pendingThreads`, `connectionTimeout` |
| JVM (Backpressure) | Thread Pool 고갈 시 새 요청이 `acceptCount` 큐에서 대기 → 큐 포화 시 연결 거부. Backpressure 미구현 시 Cascade Failure 위험 | Tomcat `currentThreadsBusy` == `maxThreads`, `acceptCount` 큐 모니터링 |
| JVM (Circuit Breaker) | 외부 API 호출 시 Circuit Breaker(Resilience4j 등) 미적용 시 Timeout 동안 Worker Thread 점유 → Retry Storm 유발 | Resilience4j MBean, APM (외부 호출 Latency P99) |
| JVM (Retry Storm) | 외부 서비스 지연 시 Client Retry + Timeout 미조정으로 인해 동시 요청 수 폭발적 증가. Thread Pool 고갈 → 전체 서비스 장애 | `netstat -an \| grep ESTABLISHED \| wc -l`, APM Error Rate 급증 패턴 |
| OS | Blocking I/O 발생 시 Worker `task_struct`가 Wait Queue 이동. CPU는 미점유이나 Thread Pool 슬롯은 점유 상태 유지 | `pidstat -u -w`, Off-CPU Flame Graph (`async-profiler -e wall`) |

---

## 13. ClassLoader 격리

Tomcat은 각 Context마다 별도의 Web Application ClassLoader를 사용할 수 있습니다.

```
Context A
  └── WebappClassLoader A (독립 Metaspace 클래스 메타데이터)

Context B
  └── WebappClassLoader B (독립 Metaspace 클래스 메타데이터)

공유 ClassLoader
  └── Common ClassLoader (Tomcat lib, JDK 클래스)
```

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| JVM (Metaspace) | 각 Context의 클래스 메타데이터는 Metaspace에 별도 할당. Context 언로드 시 ClassLoader GC → Metaspace 반환. `MaxMetaspaceSize` 미설정 시 Native Memory 무한 증가 가능 | `jstat -gcmetacapacity`, `-XX:MaxMetaspaceSize`, `jcmd <pid> VM.metaspace` |
| JVM (ClassLoader Leak) | JDBC Driver, ThreadLocal, JUL LogManager 등이 이전 ClassLoader 참조 유지 시 GC 불가 → Context 재배포 반복 시 Metaspace OOM | MAT (Leak Suspects Report), Retained Heap 분석, `-XX:+TraceClassUnloading` |
| JVM (JNI) | JNI Critical Section 내에서 ClassLoader 전환 시 GC Safepoint 진입 불가 → Stop-the-World 연장 가능 | GC log STW 시간, `-Xlog:safepoint` |
| OS | ClassLoader가 네이티브 라이브러리(`System.loadLibrary()`) 로드 시 OS `dlopen()` syscall 호출. 라이브러리별로 별도 mmap 영역 생성 | `/proc/<pid>/maps` (so 파일 항목), `pmap -x <pid>` |

---

## 14. Pipeline과 Valve

Tomcat Container 계층은 Pipeline과 Valve 구조를 사용합니다.

```
Engine Pipeline
  [AccessLogValve → ErrorReportValve → StandardEngineValve]
  ↓
Host Pipeline
  [StandardHostValve]
  ↓
Context Pipeline
  [AuthenticatorValve → StandardContextValve]
  ↓
Wrapper Pipeline
  [StandardWrapperValve]
  ↓
ApplicationFilterChain (Servlet Filter: App 계층)
  ↓
Servlet.service()
```

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| JVM | 각 Valve의 `invoke()` 메서드는 동일 Worker Thread에서 순차 호출. Valve 수 증가 = 호출 스택 깊이 증가 | `jstack` (Stack Frame 수), `-XX:MaxInlineSize` 조정 |
| JVM (AccessLog Valve) | `AccessLogValve`는 각 요청 완료 후 File I/O 수행. 동기 파일 쓰기 시 Worker Thread Blocking 가능. Async Logger(Log4j2 Async) 권장 | `strace -e write -p <pid>` (log file write latency), `iostat -x` |
| JVM (Authenticator Valve) | Form/BASIC 인증 Valve 사용 시 세션 조회 → 동기 처리. 분산 환경에서 Session Replication Latency가 응답 시간에 포함 | APM (Authenticator Valve 소요 시간), Session Store (Redis) Latency 모니터링 |
| JVM vs OS | Valve는 JVM 내부 Java 객체. Servlet Filter와 달리 `web.xml` 외부(`server.xml`)에서 설정. 운영 중 추가/제거 불가 (재시작 필요) | `server.xml` Valve 설정 감사 |
| CPU (Branch) | Valve 체인 내 조건 분기 증가 시 Branch Misprediction 가능. JIT Inlining으로 대부분 완화 | `perf stat -e branch-misses` |

---

## 15. Thread-per-request 관점의 실행 구조

Thread-per-request 모델에서는 하나의 요청을 하나의 Worker Thread가 담당합니다.

```
HTTP Request
  ↓
Tomcat Worker Thread 할당 (ThreadPoolExecutor)
  ↓  [task_struct: TASK_RUNNING]
Connector / Processor / Adapter
  ↓
Container (Engine → Host → Context → Wrapper)
  ↓
Servlet.service() → Controller → Business Logic
  ↓
Response 반환 → Worker Thread 반환 (Thread Pool)
```

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| JVM (Thread Pool) | Tomcat은 내부적으로 `TaskQueue` 기반 `ThreadPoolExecutor` 사용. `maxThreads` 초과 시 `acceptCount` 큐 → 큐 포화 시 연결 거부 | JMX `currentThreadsBusy`, `currentThreadCount`, Prometheus `tomcat_threads_*` |
| OS Thread | 각 Worker Thread는 JVM Native Thread → Linux `task_struct`. CFS 스케줄러가 `vruntime` 기반으로 CPU 할당 | `/proc/<pid>/task/` (task_struct 목록), `ps -eLf`, `htop` (Thread 뷰) |
| OS (Context Switch) | Thread 수가 많을수록 비자발적 Context Switch 증가 (CPU Time Slice 만료). `maxThreads` 과도 증가 시 오히려 Throughput 감소 | `vmstat 1` (cs 컬럼), `pidstat -w -p <pid>` (nvcswch/s) |
| Kernel (cgroup) | Kubernetes Pod의 CPU Limit은 cgroup v2 `cpu.max` (Quota/Period)로 구현. Tomcat Thread들이 CPU Quota를 소진하면 전체 컨테이너 Throttling → 모든 요청 Latency 증가 | `/sys/fs/cgroup/cpu.stat` (`throttled_time`), `kubectl top pod`, `cadvisor` `container_cpu_cfs_throttled_seconds_total` |
| Kernel (CPU Frequency) | cgroup Throttling이 없더라도 CPU C-state(절전) / P-state(주파수) 변동으로 단기 성능 저하 가능. 고성능 서버는 `performance` Governor 권장 | `cpupower frequency-info`, `turbostat`, `/sys/devices/system/cpu/cpu*/cpufreq/scaling_governor` |

---

## 16. task_struct 관점

Tomcat Worker Thread는 Linux Kernel에서 `task_struct`로 관리됩니다.

```
Java Worker Thread
  ↓  [JVM Thread 생성: pthread_create()]
JVM Native Thread (POSIX Thread)
  ↓  [clone() syscall → Kernel]
Linux task_struct
  ↓  [CFS Scheduler: Red-Black Tree에 vruntime으로 삽입]
Logical CPU 실행
```

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel (Scheduler) | CFS는 `task_struct`의 `vruntime`을 Red-Black Tree로 관리. `vruntime`이 가장 작은 Task가 다음 실행. `sched_latency_ns` / `sched_min_granularity_ns` 파라미터로 Time Slice 조정 | `/proc/<pid>/sched` (`nr_switches`, `wait_sum`), `perf sched latency` |
| Kernel (NUMA) | Worker Thread가 NUMA Node 0에 생성되었으나 메모리가 Node 1에 있는 경우 Remote Memory Access 발생 → Latency 2~4배 증가 | `numactl --hardware`, `numastat -p <pid>`, `perf stat -e node-load-misses` |
| Kernel (CPU Cache) | Worker Thread가 동일 CPU Core에서 계속 실행되면 L1/L2 Cache Hit 높음. Context Switch 후 다른 Core에 배치되면 Cache Cold Start 발생 | `perf stat -e cache-misses`, `taskset -c` (CPU 고정), `isolcpus` 커널 파라미터 |
| Kernel (TLB) | Worker Thread의 가상 주소 → 물리 주소 변환을 TLB가 캐시. Process 수 증가 / mmap 영역 과다 시 TLB Miss 증가. Tomcat은 HugePage 직접 사용하지 않지만 JVM `-XX:+UseHugeTLBFS` 옵션으로 개선 가능 | `perf stat -e dTLB-load-misses`, `/proc/meminfo` (HugePages 항목) |
| JVM (Stack) | Worker Thread 스택은 JVM 설정(`-Xss`)에 따라 Native Memory에 할당. 기본 512KB~1MB. Thread 수 × Stack Size = Native Memory 사용량 | `pmap -x <pid>` (Stack 영역), `Native Memory Tracking: jcmd <pid> VM.native_memory` |

---

## 17. Context Switch 발생 여부

Tomcat 내부 Container 라우팅 자체는 일반적인 Java 메서드 호출이므로 `Engine → Host → Context → Wrapper` 통과만으로는 Kernel Context Switch가 발생하지 않습니다.

```
Context Switch / Kernel Mode 전환이 발생하는 경우:

상황                          | 메커니즘
-----------------------------|------------------------------------------
Socket read/write            | read()/write() syscall → Kernel Mode 전환
Blocking JDBC                | socket read() → task_struct Wait Queue 이동
File I/O                     | read()/write() → Page Cache Miss 시 Disk I/O
Lock 대기 (synchronized)     | Futex wait → task_struct Sleep 상태
Thread Pool 대기             | LockSupport.park() → Futex wait → Wait Queue
CPU Time Slice 만료          | CFS Timer IRQ → 강제 Context Switch (비자발적)
K8s CPU Throttling           | cgroup Quota 소진 → task_struct Throttle Queue
JVM Safepoint                | STW GC → 모든 Worker Thread 일시 정지
```

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel (Futex) | Java `synchronized`, `ReentrantLock` 등 경합 시 `futex(FUTEX_WAIT)` syscall → task_struct Block. Lock 해제 시 `futex(FUTEX_WAKE)` → Wake-up | `perf trace -e futex -p <pid>`, `strace -e futex`, Lock 경합 APM 추적 |
| Kernel (Off-CPU) | Blocking 발생 시 Worker Thread가 CPU를 반환하지만 Thread Pool 슬롯은 유지. Off-CPU Time이 길수록 Throughput 저하 | `async-profiler -e wall -d 60 -f off_cpu.html <pid>` (Off-CPU Flame Graph) |
| JVM (Safepoint) | GC Stop-the-World, Deoptimization, Biased Lock 해제 등에서 Safepoint 발생. 모든 Worker Thread가 안전 지점에 도달할 때까지 대기 → Latency Spike | `-Xlog:safepoint`, JFR `jdk.SafepointBegin/End`, `jcmd <pid> VM.log` |
| JVM (GC) | Young GC (Minor GC) 중 STW 발생. Worker Thread 전체 일시 정지 → Latency 99th/99.9th percentile 영향 | `-Xlog:gc*`, JFR, GC Pause 모니터링 (Prometheus `jvm_gc_pause_seconds`) |

---

## 18. JVM Memory 관점

```
JVM 메모리 레이아웃:

Heap
  ├── Connector 객체 (Request/Response 풀)
  ├── Engine / Host / Context / Wrapper 객체 (장기 생존)
  ├── Servlet / DispatcherServlet 객체 (Context 생명주기)
  ├── Request / Response 객체 (요청당 생성, Minor GC 대상)
  ├── Filter Chain, MappingData (단명 객체)
  └── Business Object (Controller 처리 결과)

Metaspace
  ├── Connector.class, StandardEngine.class, ...
  ├── WebappClassLoader 관리 클래스 메타데이터
  └── JIT Compiled Code Cache (Code Cache)

Native Memory (Off-Heap)
  ├── Worker Thread Stack (× maxThreads)
  ├── Direct ByteBuffer (NIO Socket Buffer)
  └── mmap 영역 (ClassLoader, JNI 라이브러리)
```

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| JVM (Heap) | 요청마다 생성되는 Request/Response, Header Map, String 객체가 Eden 영역 빠르게 소진 → Minor GC 빈도 증가. Allocation Rate가 높을수록 GC Pressure 상승 | `jstat -gcutil <pid> 1000`, GC log Allocation Rate, `async-profiler -e alloc` |
| JVM (Direct Memory) | NIO Socket Buffer는 `DirectByteBuffer` 사용. Off-Heap Native Memory에 할당. `-XX:MaxDirectMemorySize` 미설정 시 -Xmx와 동일하게 적용. 과다 사용 시 Native OOM 발생 | `jcmd <pid> VM.native_memory`, `pmap -x <pid>` (anon 영역), NMT Direct 항목 |
| JVM (Code Cache) | JIT C1/C2 컴파일 결과 저장. Code Cache 포화 시 JIT 중단 → 성능 급격히 저하 (`Compilation stopped` 경고). `-XX:ReservedCodeCacheSize` 조정 | JFR `jdk.CodeCacheFull`, `-XX:+PrintCodeCache`, `jcmd <pid> VM.code_cache` |
| JVM (TLAB) | 각 Worker Thread가 Eden 내 TLAB(Thread-Local Allocation Buffer)에서 객체 빠르게 할당. TLAB 소진 시 Eden 직접 할당 또는 Minor GC. Thread 수 증가 시 TLAB 총 사용량 증가 | `-XX:+PrintTLAB`, `-Xlog:gc+tlab`, TLAB 할당 실패 횟수 모니터링 |
| OS (Memory) | JVM 프로세스의 RSS(Resident Set Size)는 Heap + Metaspace + Native Memory + Stack 합산. Kubernetes `memory.limit` 초과 시 OOM Kill 발생 | `cat /proc/<pid>/status` (VmRSS, VmSwap), `kubectl top pod`, `oom_score_adj` |
| Kernel (OOM Killer) | Container 메모리 한계 초과 시 Kernel OOM Killer가 `oom_score`가 높은 프로세스(JVM) 종료. `oom_score_adj` 설정으로 우선순위 조정 가능 | `dmesg \| grep -i oom`, `/proc/<pid>/oom_score`, K8s `OOMKilled` Pod 이벤트 |
| Kernel (THP) | Transparent HugePage(THP)가 활성화되면 JVM Heap에 2MB 페이지 사용 → TLB Miss 감소. 단, `khugepaged` 스캔으로 인한 짧은 Latency Spike 가능 | `/sys/kernel/mm/transparent_hugepage/enabled`, `perf stat -e dTLB-load-misses` (HugePage 효과 확인) |

---

## 19. 요청 처리 중 Blocking I/O 발생

Servlet 또는 Controller 내부에서 JDBC, 외부 API, 파일 I/O 같은 Blocking 호출이 발생하면 해당 Worker Thread는 대기 상태가 됩니다.

```
Worker Thread (task_struct A)
  ↓
Controller
  ↓
JDBC Blocking Call (HikariCP → Socket → DB)
  ↓  [read() syscall → Kernel Socket 수신 대기]
  ↓  [task_struct A → Wait Queue 이동]
  ↓  [DB 응답 수신 → IRQ → SoftIRQ → TCP Stack → sk_buff → Socket Buffer]
  ↓  [epoll 이벤트 → task_struct A Wake-up → Runnable Queue 재진입]
Response 처리 재개
```

| 자원 | 상태 |
|------|------|
| Java Worker Thread | Thread Pool에 반환되지 않음 (슬롯 점유) |
| task_struct | Wait Queue에 위치 (TASK_INTERRUPTIBLE) |
| JVM Stack | 호출 프레임 전체 유지 (메모리 점유) |
| Heap 객체 | Request/Response/Connection 객체 유지 |
| CPU | 해당 task_struct 실행 안 함 (Off-CPU) |
| Tomcat Thread Pool | 슬롯 점유 → 가용 Thread 감소 |
| DB Connection Pool | 연결 점유 → Pool Exhaustion 위험 |

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| Kernel (Socket Buffer) | DB 서버 응답이 NIC → DMA → Rx Ring Buffer → SoftIRQ → TCP Stack → `sk_buff` → Socket Receive Buffer 경로로 도착. `SO_RCVBUF` 크기가 충분해야 고속 응답 처리 가능 | `ss -tnmp` (Socket Buffer 사용량), `sysctl net.ipv4.tcp_rmem` |
| Kernel (IRQ / SoftIRQ) | DB 응답 패킷 도착 시 NIC HW IRQ → `net_rx_action` SoftIRQ → TCP Stack 처리 → Socket Buffer 적재 → epoll 이벤트 발생 → Worker Thread Wake-up | `mpstat -I ALL` (`%soft` 급증 시 네트워크 트래픽 폭증), `/proc/softirqs` |
| JVM (Off-CPU) | Blocking 동안 Worker Thread는 CPU를 사용하지 않음 (Off-CPU Time). 전통적 CPU 프로파일러로는 보이지 않음. Wall-clock 프로파일러 필요 | `async-profiler -e wall`, `pyroscope`, Datadog APM (Off-CPU 분석) |
| Kernel (Futex / Lock) | HikariCP Connection Pool 경합 시 `futex(FUTEX_WAIT)` 발생. 다수 Thread가 동시에 Connection 요청 시 Lock Convoy 현상 가능 | `perf trace -e futex -p <pid>`, HikariCP `pendingThreads` 지표 |
| JVM (Connection Pool Exhaustion) | `maxPoolSize` 초과 시 Worker Thread가 `connectionTimeout`(기본 30초)까지 대기. 전체 Thread Pool 고갈로 이어지면 새 요청 503 오류 발생 | HikariCP MBean `pendingThreads`, `connectionTimeout` 설정, APM DB Connection Latency |

---

## 20. SRE 관점의 병목 지점

| 구간 | 병목 원인 | 메커니즘 실체 | 결과 | 분석 도구 |
|------|----------|-------------|------|----------|
| NIC / Kernel | NIC Rx Ring Buffer 오버플로우 | DMA 처리 지연, NIC 큐 포화 | 패킷 DROP, 재전송 폭증 | `ethtool -S eth0` (rx_missed_errors), `netstat -s` |
| SoftIRQ | 네트워크 패킷 폭증 | `ksoftirqd` CPU 점유 급증 | CPU SoftIRQ 포화, 전체 응답 지연 | `mpstat` `%soft` > 10%, `sar -n EDEV` |
| TCP Stack | SYN Queue / Accept Queue 포화 | `net.core.somaxconn` 부족, SYN Flood | 신규 연결 거부, 연결 타임아웃 | `ss -lntp` (Recv-Q), `netstat -s \| grep drop` |
| Connector | maxConnections 초과 | `LimitLatch` Blocking | Acceptor 정체, 신규 연결 수락 불가 | JMX `maxConnections`, `currentThreadsBusy` |
| Acceptor | accept 처리 지연 | Accept Queue 드레인 속도 부족 | 연결 수락 지연 | `netstat -s`, Tomcat Access Log 지연 |
| Poller | epoll 이벤트 처리 지연 | Poller Thread 부족 또는 처리 루프 지연 | Socket read/write 지연, Latency 증가 | `jstack` (Poller 상태), `ethtool -L` (RSS 큐 조정) |
| Processor | HTTP 파싱 비용 증가 | 대형 Header, 대용량 Body, JIT Cold | CPU 사용량 증가, TLAB 압박 | `async-profiler -e cpu`, GC Allocation Rate |
| Adapter / Container | Valve 비용, Mapper 탐색 | 복잡한 Valve 체인, 다수 Context | 요청 라우팅 지연 | `jstack` Hot Frame 분석, APM Valve 소요 시간 |
| Wrapper / Servlet | Blocking I/O, 비즈니스 로직 지연 | JDBC Block, 외부 API Timeout | Thread Pool 고갈, 503 오류 | Off-CPU Flame Graph, HikariCP `pendingThreads` |
| ClassLoader | 최초 클래스 로딩, Context 리로드 | Metaspace 할당, JIT Cold Start | 첫 요청 지연, Metaspace OOM | `jstat -gcmetacapacity`, MAT ClassLoader Leak |
| GC | Young/Full GC STW | Allocation Rate 과다, Heap 부족 | Latency Spike (99th/99.9th percentile) | GC log, JFR, `jstat -gcutil` |
| cgroup (Kubernetes) | CPU Throttling | `cpu.max` Quota 소진 | 모든 요청 Latency 증가, 균일한 Latency Spike | `/sys/fs/cgroup/cpu.stat`, `throttled_time`, cadvisor |
| NUMA | Remote Memory Access | Worker Thread와 메모리 NUMA Node 불일치 | Memory Latency 2~4배 증가 | `numastat -p <pid>`, `perf stat -e node-load-misses` |
| Kernel OOM | RSS 한계 초과 | JVM Heap + Native Memory 합산 초과 | Pod OOM Kill, 서비스 중단 | `dmesg \| grep oom`, K8s OOMKilled 이벤트 |

---

## 21. 주요 Tomcat 설정과 영향

| 설정 | 의미 | 하위 계층 메커니즘 | 영향 |
|------|------|--------------------|------|
| `maxThreads` | Worker Thread 최대 개수 | `task_struct` 수, Thread Stack Native Memory, CFS 스케줄링 대상 수 | 너무 작으면 처리 지연. 너무 크면 Context Switch 증가, Native Memory 압박 |
| `acceptCount` | Worker 부족 시 대기 가능한 연결 수 | Kernel Accept Queue + Tomcat 내부 큐 | 초과 시 연결 거부. 너무 크면 지연 요청 누적 |
| `connectionTimeout` | 연결 대기 시간 | Socket `SO_TIMEOUT`, Processor 읽기 대기 | 너무 길면 Slow Client가 Worker Thread 점유 |
| `maxConnections` | 동시에 유지 가능한 연결 수 | `LimitLatch` (Acceptor Blocking), epoll 등록 fd 수 | 초과 시 Acceptor Blocking, 신규 연결 수락 불가 |
| `keepAliveTimeout` | Keep-Alive 연결 유지 시간 | Socket 유지, Poller epoll 등록 유지 | 긴 값은 idle 연결 fd 점유, Poller 부담 증가 |
| `processorCache` | Processor 객체 재사용 수 | JVM Heap 객체 풀링, GC Pressure 완화 | 너무 작으면 매 요청 객체 생성. 너무 크면 Heap 고정 점유 |
| `minSpareThreads` | 최소 유지 Worker Thread 수 | 사전 생성 `task_struct`, JVM Thread Stack 사전 할당 | 너무 작으면 트래픽 급증 시 Thread 생성 지연 |

---

## 22. Spring Boot 내장 Tomcat 관점

Spring Boot는 Tomcat을 내장 서버로 실행합니다. 별도 프로세스가 아닌 동일 JVM 프로세스 내부에서 실행됩니다.

```
OS Process
  └── JVM
      ├── Spring Application Context (ApplicationContext)
      ├── Embedded Tomcat (TomcatEmbeddedWebappClassLoader)
      │   ├── Connector (NioEndpoint)
      │   ├── Engine / Host / Context / Wrapper
      │   └── Worker Thread Pool (ThreadPoolExecutor)
      ├── Spring Bean (Service, Repository, ...)
      └── JVM Worker Threads (task_struct × maxThreads)
```

### 계층별 메커니즘 실체

| 계층 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|-------------|----------------------|
| JVM (ClassLoader) | Spring Boot는 단일 `URLClassLoader` (fat jar) 사용. 표준 Tomcat의 다중 `WebappClassLoader`와 달리 ClassLoader 격리 없음. ClassLoader Leak 패턴은 단순하나 전체 JVM 영향 | `jcmd <pid> VM.classloaders`, `jmap -clstats` |
| JVM (Actuator) | Spring Boot Actuator `/actuator/metrics`가 JVM, Tomcat, HikariCP 등 실시간 지표 노출. Prometheus Micrometer와 연동 | `curl /actuator/metrics/tomcat.threads.busy`, Grafana Dashboard |
| JVM (Startup) | `SpringApplication.run()` → `TomcatEmbeddedWebServer.start()` → Connector 초기화 순서. Spring Context 초기화 완료 전 HTTP 요청 수신 시 503 가능 | K8s `readinessProbe` (`/actuator/health/readiness`), Spring Boot `ApplicationReadyEvent` |
| OS | Spring Boot 앱은 단일 Java 프로세스. `ulimit -n` (open files) 제한이 maxConnections + Worker Thread fd + 기타 fd 합산을 초과하지 않아야 함 | `ulimit -n`, `/proc/<pid>/fd \| wc -l`, `cat /proc/sys/fs/file-max` |

---

## 23. WebFlux / Netty와의 차이

| 구분 | Spring MVC + Tomcat | WebFlux + Netty |
|------|--------------------|--------------------|
| 기본 모델 | Thread-per-request (Blocking) | Event Loop (Non-blocking) |
| 요청 처리 단위 | Worker Thread 1개가 요청 전담 | 소수 Event Loop Thread가 다수 요청 처리 |
| Kernel 이벤트 처리 | epoll 기반이나 Thread Blocking 허용 | epoll 기반, Event Loop Thread Blocking 금지 |
| Blocking I/O 영향 | Worker Thread 점유 (Thread Pool 고갈) | Event Loop Thread 차단 시 전체 처리 마비 |
| task_struct 수 | maxThreads 수만큼 다수 task_struct 가능 | Event Loop 수(CPU 코어 수)의 소수 task_struct |
| Context Switch | Thread 수 비례하여 Context Switch 발생 | Context Switch 최소화 (소수 Thread) |
| JVM (Reactor) | 해당 없음 | Project Reactor `Scheduler`, `boundedElastic` Pool |
| 튜닝 핵심 | `maxThreads`, DB Pool Size, Timeout | Event Loop 보호, Non-blocking Driver, Backpressure |
| SRE 분석 | Off-CPU Flame Graph, Thread Pool 지표 | Reactor Context Propagation, Event Loop Latency |

---

## 24. 핵심 정리

Tomcat의 Connector는 외부 네트워크 요청을 받아 Servlet Container로 전달하는 진입 계층입니다.

```
Client Request
  ↓  [NIC DMA → sk_buff → SoftIRQ → TCP Stack → Accept Queue]
Connector
  ↓  [accept4() syscall → SocketChannel]
Acceptor / Poller
  ↓  [epoll_wait() → 이벤트 감지 → Worker Thread 위임]
Processor
  ↓  [read() syscall → HTTP 파싱 → JVM Heap Request 객체 생성]
Adapter
  ↓  [CoyoteAdapter.service() → Container 진입, 동일 Worker Thread]
Engine → Host → Context → Wrapper
  ↓  [Mapper: Host Header / URI / URL Pattern 매핑, JIT 최적화 메서드 호출]
Servlet.service() / DispatcherServlet
  ↓  [HandlerMapping → Controller → Business Logic]
  ↓  [Blocking I/O 발생 시 task_struct Wait Queue 이동, Off-CPU]
Response
```

모든 Container 계층(Engine, Host, Context, Wrapper)은 JVM Heap 내 Java 객체이며, Linux Kernel이 직접 스케줄링하는 대상은 이를 실행하는 Worker Thread의 `task_struct`입니다.

성능 장애는 단일 계층이 아닌 Hardware(NIC, CPU Cache, NUMA) → Kernel(IRQ, SoftIRQ, TCP Stack, cgroup, Scheduler) → JVM(GC, JIT, ClassLoader, TLAB, Connection Pool) → Application(Business Logic, Blocking I/O, Circuit Breaker) 전 계층에 걸쳐 연쇄적으로 발생합니다.

| 분석 대상 | 핵심 도구 |
|----------|----------|
| 네트워크 수신 | `ethtool -S`, `/proc/interrupts`, `mpstat %soft` |
| TCP 연결 상태 | `ss -lntp`, `netstat -s` |
| Kernel Scheduler | `/proc/schedstat`, `perf sched`, `vmstat cs` |
| cgroup Throttling | `/sys/fs/cgroup/cpu.stat`, `cadvisor`, `kubectl top` |
| JVM GC | GC log, `jstat -gcutil`, JFR |
| JVM Thread / Off-CPU | `jstack`, `async-profiler -e wall`, Off-CPU Flame Graph |
| JVM Memory | `jcmd VM.native_memory`, `jmap`, MAT |
| Connection Pool | HikariCP MBean, APM DB Connection Latency |
| CPU / Cache | `perf stat -e cache-misses,branch-misses`, `toplev` |
| NUMA | `numastat`, `numactl --hardware` |