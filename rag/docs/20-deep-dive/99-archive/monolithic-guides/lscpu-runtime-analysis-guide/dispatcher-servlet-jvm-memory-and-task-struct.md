# DispatcherServlet, Singleton, JVM Memory, task_struct 실행 구조

## 개요

Spring 기반 Java 애플리케이션에서 `DispatcherServlet`은 요청을 실제 Controller로 라우팅하는 핵심 진입 객체입니다.

`DispatcherServlet`은 요청마다 새로 생성되는 객체가 아니라, 애플리케이션 구동 시점에 한 번 생성되어 여러 Worker Thread가 공유하는 **Singleton 객체**입니다.

실제 요청을 처리하는 실행 흐름은 Worker Thread이며, Linux Kernel 수준에서는 각 Worker Thread가 `task_struct`로 실체화되어 스케줄링됩니다.

```
Client Request
  → Worker Thread / task_struct
  → DispatcherServlet Singleton
  → HandlerMapping
  → Controller
  → Service
  → Repository / External API
```

---

## 1. DispatcherServlet의 계층적 위치

`DispatcherServlet`은 Java/Spring 애플리케이션 계층의 객체입니다.

```
Application Layer  →  DispatcherServlet (Spring Bean, Singleton)
JVM Layer          →  Heap / Metaspace / JIT Compiled Code Cache
OS Kernel Layer    →  task_struct / CFS Scheduler / Wait Queue
Hardware Layer     →  Logical CPU / Physical Core / L1~L3 Cache
```

Kernel이 스케줄링하는 대상은 `DispatcherServlet`이 아니라, 해당 코드를 실행하는 **Worker Thread의 `task_struct`**입니다.

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| Application 계층 | App | DispatcherServlet → HandlerMapping → HandlerAdapter → Controller 호출 체인, Spring ApplicationContext가 Singleton Bean Registry 관리 | `jstack <pid>`, `jcmd <pid> Thread.print`, Spring Actuator `/actuator/threaddump` |
| JVM 계층 | JVM (Runtime) | JIT C1/C2 컴파일: 빈번 호출 메서드(doDispatch 등)를 Native Code로 변환, Code Cache 적재, TLAB에서 요청 객체 할당 | `-XX:+PrintCompilation`, `jcmd <pid> Compiler.queue`, `async-profiler -e cpu` |
| OS Kernel 계층 | Kernel | CFS `task_struct.se.vruntime` 기반 스케줄링, Worker Thread 수만큼 `task_struct` 존재, Blocking I/O 시 `TASK_UNINTERRUPTIBLE` 상태 전환 | `/proc/<pid>/status`, `ps -eLf`, `pidstat -u -t 1` |
| Hardware 계층 | Hardware | Logical CPU가 Worker Thread의 JIT 컴파일된 Native 명령어 실행, L1/L2 Cache에 핫 메서드 코드 캐싱 | `perf stat -e cpu-cycles,cache-misses -p <pid>`, `toplev.py` |

---

## 2. DispatcherServlet 생성 시점

`DispatcherServlet`은 요청마다 생성되지 않습니다. 애플리케이션 구동 시점에 한 번 생성됩니다.

```
Application Startup
  → Servlet Container 초기화
  → Spring ApplicationContext 생성
  → DispatcherServlet 생성
  → init() 호출
  → HandlerMapping / HandlerAdapter 준비
```

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| Servlet Container 초기화 | App → Kernel | `socket()` → `bind(8080)` → `listen()` → `epoll_create()` → `epoll_ctl()` 시스템 콜 순서로 수신 준비, TCP Backlog / SYN Queue 초기화 | `ss -tlnp`, `netstat -tlnp`, `strace -e socket,bind,listen,epoll_create java` |
| ApplicationContext 생성 | JVM + Kernel | ClassLoader가 JAR/class 파일을 `open()` / `read()` 시스템 콜로 로딩, Page Cache 조회 → Cache Miss 시 Disk I/O(blk-mq), Class Metadata → Metaspace 적재 | `strace -e openat,read -p <pid>`, `jcmd <pid> VM.classloading_statistics` |
| Singleton Bean 생성 | JVM | Spring이 `@Bean` / `@Component` 스캔 후 Heap에 객체 생성, 생성된 참조를 ApplicationContext Bean Registry에 등록, 초기 Heap 점유 발생 | `jmap -histo <pid>`, `-Xlog:gc*:time`, `jcmd <pid> GC.heap_info` |
| ClassLoader 동작 | JVM + Kernel | Bootstrap → Extension → Application ClassLoader 위임 모델, 중복 클래스 로딩 방지(ClassLoader Lock), ClassLoader Leak: 재배포 시 구 ClassLoader가 GC되지 않으면 Metaspace 누수 | `jcmd <pid> VM.classloader_stats`, Metaspace OOM 확인: `dmesg | grep oom` |
| HandlerMapping 준비 | JVM + App | `RequestMappingHandlerMapping`이 `@RequestMapping` 어노테이션 스캔 후 URL → Method 매핑 테이블 생성, Reflection API 호출로 Method 객체 캐싱 | Spring Actuator `/actuator/mappings`, `-XX:+TraceClassLoading` |

---

## 3. DispatcherServlet의 Singleton 구조

`DispatcherServlet` 인스턴스는 하나이며, 여러 Worker Thread가 동시에 해당 객체의 메서드를 실행할 수 있습니다.

```
Heap
  └── DispatcherServlet 객체 1개

Worker Thread A (task_struct A)  →  DispatcherServlet.doDispatch() 실행
Worker Thread B (task_struct B)  →  DispatcherServlet.doDispatch() 실행
Worker Thread C (task_struct C)  →  DispatcherServlet.doDispatch() 실행
```

객체는 하나이지만 실행 주체는 여러 Worker Thread입니다.

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| Heap 상의 Singleton | JVM | DispatcherServlet 객체는 Old Gen(Tenured) 영역에 장기 생존, GC Root로부터 강한 참조 체인 유지로 GC 대상 제외, 객체 헤더(Mark Word)에 Lock 상태 기록 가능 | `jmap -histo:live <pid>`, `jcmd <pid> GC.heap_info`, `java -verbose:gc` |
| 다중 Thread 동시 접근 | JVM + Kernel | 각 Worker Thread가 독립적 Stack Frame에서 `doDispatch()` 호출, 메서드 인수·지역변수는 Stack에 격리, Singleton 객체의 인스턴스 변수 접근 시 동시성 위험 | `jstack <pid> | grep doDispatch`, `async-profiler -e cpu` |
| CPU 병렬 실행 | Hardware | 각 Worker Thread의 `task_struct`가 별도 Logical CPU에 배치, 동일 메서드(JIT 컴파일된 Native Code)를 여러 Core가 동시 실행, LLC(L3 Cache) 공유로 Hot Code 캐시 효율 | `perf stat --per-core -e cpu-cycles`, `mpstat -P ALL 1` |
| JIT 컴파일 효과 | JVM (C1/C2) | `doDispatch()` 호출 횟수가 임계치(C1: 1500회, C2: 10000회) 초과 시 Tier 3 → Tier 4 컴파일, Inlining으로 HandlerMapping 조회 등 내부 호출 스택 평탄화 | `-XX:+PrintCompilation`, `jcmd <pid> Compiler.codebuffer_statistics`, `async-profiler` |

---

## 4. Spring에서 Singleton으로 관리되는 객체

Spring에서 기본적으로 Singleton Bean으로 관리되는 객체는 다음과 같습니다.

- `@Controller` / `@RestController`
- `@Service`
- `@Repository`
- `@Component`
- `DispatcherServlet`

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| Bean Registry 관리 | App (Spring) | `DefaultSingletonBeanRegistry`: Bean 이름 → 객체 참조 HashMap 보관, `getBean()` 호출 시 Registry에서 기존 인스턴스 반환, 최초 생성 시만 `createBean()` 실행 | Spring Actuator `/actuator/beans`, `applicationContext.getBeanDefinitionNames()` |
| Heap 점유 패턴 | JVM | Singleton Bean은 JVM 기동 후 Old Gen에 승격·장기 유지, Young Gen GC(Minor GC) 영향 없음, Heap Dump 시 Singleton이 대형 Object Graph의 Root 역할 | `jmap -dump:live,format=b,file=heap.hprof <pid>`, Eclipse MAT (Dominator Tree 분석) |
| Proxy 기반 AOP | JVM + App | `@Transactional` / `@Async` 등 적용 시 CGLIB Proxy 객체가 실제 Bean을 감싸 Heap에 추가 생성, Proxy 클래스 메타데이터가 Metaspace에 추가 적재 | `jcmd <pid> VM.classloader_stats`, Metaspace 사용량 모니터링 |
| ClassLoader Leak 위험 | JVM + Kernel | 재배포(Hot Deploy) 시 구 ApplicationContext가 GC되지 않으면 구 ClassLoader 참조 유지 → Metaspace 누수 반복 → `OutOfMemoryError: Metaspace` | `jcmd <pid> VM.classloading_statistics`, `-XX:+TraceClassUnloading`, `jmap -histo <pid> | grep ClassLoader` |

---

## 5. Singleton을 사용하는 이유

요청마다 객체를 생성하면 다음 문제가 발생합니다.

```
요청 증가
  → 객체 생성 증가
  → TLAB 소진 → Heap Lock 경합
  → Young Gen 조기 포화
  → Minor GC 빈도 증가
  → GC Pause 증가 (Stop-The-World)
  → Safepoint 진입 대기 시간 증가
  → Latency 증가
```

Singleton은 객체 생성 비용과 GC 부담을 줄입니다.

```
Singleton 객체 1개 생성 (Old Gen 장기 생존)
  → 여러 Thread가 공유
  → TLAB 소비 최소화
  → Young Gen 압박 감소
  → Minor GC 빈도 감소
  → Heap 안정성 증가
```

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| TLAB 할당 절감 | JVM | 각 Worker Thread가 TLAB(Thread-Local Allocation Buffer) 내에서 bump-the-pointer로 빠른 객체 할당, 요청마다 Controller/Service 객체 생성 시 TLAB 조기 소진 → Slow Path(Heap Lock) 빈도 증가 | `-XX:+PrintTLAB`, `-Xlog:gc+tlab=debug`, `jstat -gcnew <pid>` |
| GC Pause 영향 | JVM | Minor GC(Young Gen 수집): Stop-The-World(STW) 발생, 모든 JVM Thread가 Safepoint 도달까지 대기, 요청 객체 다발 생성 시 Minor GC 빈도 증가 → p99 Latency 악화 | `jstat -gcutil <pid> 1000`, `-Xlog:gc*:time`, Prometheus `jvm_gc_pause_seconds` |
| Safepoint 비용 | JVM | GC Safepoint 요청 시 모든 JVM Thread가 Safepoint Poll Point에서 정지 대기, Thread 수 많을수록 Time-to-Safepoint 증가, JNI Critical Section 보유 Thread는 즉시 진입 불가 → 전체 대기 연장 | `-XX:+PrintSafepointStatistics`, `-XX:GuaranteedSafepointInterval` |
| Allocation Rate | JVM + Kernel | 높은 Allocation Rate: TLAB 반복 소진 → `brk()` / `mmap()` 시스템 콜로 Heap 확장 → Page Fault 발생, 물리 메모리 매핑 지연 | `async-profiler -e alloc`, `/proc/PID/smaps` (heap 영역 크기 변화), `vmstat 1` (pgfault 항목) |

---

## 6. Singleton과 Thread Safety

Singleton 객체는 여러 Worker Thread가 동시에 접근합니다.  
따라서 **Singleton 객체 내부에 요청별 상태를 저장하면 데이터 경합이 발생**합니다.

**잘못된 구조 (인스턴스 변수에 요청 데이터 저장)**

```java
@Service
public class PaymentService {
    private long amount; // 여러 Thread가 동시에 덮어쓸 수 있음

    public void pay(long requestAmount) {
        this.amount = requestAmount;
    }
}
```

**올바른 구조 (요청 데이터는 지역 변수 또는 파라미터로 처리)**

```java
@Service
public class PaymentService {
    public void pay(long amount) {
        // amount는 Stack Frame의 지역 변수로 Thread 간 격리됨
    }
}
```

요청별 데이터는 Thread의 Stack Frame에 두고, Singleton 객체는 **Stateless 구조**로 유지해야 합니다.

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| 인스턴스 변수 경합 | Hardware + JVM | 여러 Core가 동일 Heap 주소(인스턴스 변수)에 동시 Write → Cache Line 무효화 전파(MESI 프로토콜), Cache Line Thrashing: 해당 변수가 속한 64B Cache Line을 Core 간 Ping-Pong | `perf c2c record + report`, `perf stat -e offcore_response` |
| Stack Frame 격리 | JVM + Hardware | 지역 변수·메서드 인수는 Worker Thread의 JVM Stack에 저장, 각 Thread의 Stack은 독립적 가상 주소 범위 → 다른 Thread 접근 불가, CPU Register에도 일시적으로 캐싱 | `jstack <pid>` (Thread별 Stack Trace), `/proc/<pid>/maps` (stack 영역) |
| Synchronized / Futex | JVM + Kernel | `synchronized` 블록 진입 시 Object Monitor 획득 시도, Biased → Thin → Fat Lock 에스컬레이션, Fat Lock은 `futex(FUTEX_WAIT)` 시스템 콜 → Off-CPU 전환 → Wait Queue 대기 | `jstack <pid> | grep BLOCKED`, `perf lock record + report`, `offcputime-bpfcc -p <pid>` |
| ThreadLocal 격리 | JVM | `ThreadLocal<T>`: 각 Thread의 `Thread.threadLocals` Map에 값 저장, Singleton에서 안전하게 요청별 컨텍스트 보관 가능, Thread Pool 재사용 시 이전 요청 데이터 잔류 위험(`ThreadLocal.remove()` 필수) | `jstack <pid>` (ThreadLocal 값 확인), `-XX:+PrintGCDetails` (ThreadLocal 누수 → Young Gen 압박) |
| Volatile / Memory Visibility | Hardware + JVM | `volatile` 변수: CPU Cache 우회 → Main Memory 직접 접근 강제, 가시성 보장이나 원자성 미보장, `AtomicLong` 등 CAS(Compare-And-Swap) 명령어 사용 시 Lock-Free 원자 연산 가능 | `perf stat -e cache-misses`, JMH(Java Microbenchmark Harness)로 동시성 성능 측정 |

---

## 7. JVM Memory 구조

| 영역 | 역할 | 메커니즘 실체 |
|------|------|--------------|
| Metaspace | 클래스 메타데이터, 메서드 정보, static 변수 참조 | Native Memory 영역, `-XX:MaxMetaspaceSize` 제한, ClassLoader 단위 해제 |
| Heap (Young Gen) | 신규 객체 할당, TLAB 관리 | Eden + Survivor(S0/S1), Minor GC 대상, bump-the-pointer 할당 |
| Heap (Old Gen) | 장기 생존 객체(Singleton Bean 등) | Major GC / Full GC 대상, G1 Region 기반 관리 |
| Stack | Thread별 메서드 호출 정보, 지역 변수 | Frame 단위 Push/Pop, `-Xss` 크기 제한, StackOverflowError |
| Code Cache | JIT 컴파일된 Native Code 저장 | `-XX:ReservedCodeCacheSize`, Code Cache 포화 시 JIT 비활성화 |
| Direct Memory | Off-Heap 버퍼 (`ByteBuffer.allocateDirect`) | `mmap()` 기반, GC 대상 아님, `Cleaner` 메커니즘으로 해제 |

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| Heap 확장 | JVM + Kernel | Heap 부족 시 `brk()` / `mmap()` 시스템 콜로 가상 주소 확장, 최초 접근 시 Page Fault → 물리 메모리 매핑, `-Xms` / `-Xmx` 범위 내에서 동적 조정 | `vmstat 1` (pgfault), `/proc/<pid>/maps` (heap 영역), `pmap -x <pid>` |
| Metaspace 관리 | JVM + Kernel | Metaspace는 Native Memory(`mmap()`)로 할당, ClassLoader가 언로드될 때 일괄 해제, `-XX:MaxMetaspaceSize` 미설정 시 무제한 성장 → OOM 위험 | `jcmd <pid> VM.native_memory`, `-XX:+PrintMetaspaceStatistics` |
| OOM Killer 개입 | Kernel | JVM 프로세스가 물리 메모리 한계 초과 시 Linux OOM Killer 개입 → `SIGKILL` 전송, `/proc/<pid>/oom_score_adj`로 우선순위 조정 가능 | `dmesg | grep -i "oom\|killed"`, `/proc/<pid>/oom_score` |
| Direct Memory | JVM + Kernel | `ByteBuffer.allocateDirect()` → `sun.misc.Unsafe.allocateMemory()` → `malloc()` → OS `mmap()`, GC 비관리 영역, Netty 등 고성능 I/O 라이브러리에서 Zero-Copy를 위해 활용, `Finalization Queue`를 통한 지연 해제 위험 | `-XX:MaxDirectMemorySize`, `jcmd <pid> VM.native_memory`, `NativeMemoryTracking=detail` |

---

## 8. DispatcherServlet의 메모리 위치

```
Metaspace    →  DispatcherServlet.class 메타데이터 (메서드 바이트코드, 상수 풀)
Code Cache   →  JIT 컴파일된 doDispatch() Native Code
Heap         →  DispatcherServlet 객체 인스턴스 (Old Gen)
Thread Stack →  doDispatch() 호출 Stack Frame (Worker Thread별 독립)
```

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| Metaspace 적재 | JVM + Kernel | ClassLoader가 `.class` 바이트코드를 `read()` 시스템 콜로 읽어 파싱, `ConstantPool` / `MethodTable` / `vtable` 구조 생성, Metaspace에 `InstanceKlass` 구조체로 저장 | `jcmd <pid> VM.classloading_statistics`, `-XX:+TraceClassLoading` |
| Code Cache 점유 | JVM (C1/C2) | `doDispatch()` JIT 컴파일 후 Code Cache에 Native Code 블록 저장, Code Cache 포화(`ReservedCodeCacheSize` 초과) 시 JIT 컴파일 중단 → Interpreter 모드 강제 → 성능 급락 | `jcmd <pid> Compiler.codebuffer_statistics`, `-XX:+PrintCodeCache` |
| Stack Frame 격리 | JVM + Hardware | 각 Worker Thread가 `doDispatch()` 진입 시 새 Stack Frame Push, Frame 내 지역 변수·Operand Stack은 Thread 독립적, CPU 레지스터에 임시 캐싱 | `jstack <pid>` (Frame 단위 Stack Trace), `perf record -g -p <pid>` |

---

## 9. Static Reference와 Singleton 접근

직접 구현한 Singleton에서는 static 참조가 Heap 객체를 가리킵니다.

```
Metaspace / Class Metadata
  └── static reference
          ↓
Heap (Old Gen)
  └── Singleton Instance
```

Spring에서는 Spring Container가 Singleton Bean의 객체 참조를 관리합니다.

```
Spring ApplicationContext (Heap)
  └── Bean Registry (HashMap)
          ↓
Heap (Old Gen)
  └── Singleton Bean Instance
```

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| Static 변수 GC Root | JVM | Static 변수는 Class Metadata(Metaspace)에 참조 저장 → GC Root로 인식 → Heap의 Singleton 인스턴스를 강하게 참조 → GC 대상 제외, Heap Dump 분석 시 Dominator Tree 최상단 | Eclipse MAT `Dominator Tree`, `jmap -histo:live <pid>` |
| Bean Registry 참조 체인 | App + JVM | `ApplicationContext` → `DefaultSingletonBeanRegistry.singletonObjects` (ConcurrentHashMap) → Singleton Bean 인스턴스, 애플리케이션 종료 전까지 강한 참조 유지 | Eclipse MAT `Path to GC Roots`, `jcmd <pid> GC.heap_info` |
| Static 남용으로 인한 Memory Leak | JVM | Static 컬렉션(Map, List)에 요청 데이터 누적 → GC 대상 제외로 Old Gen 지속 증가 → Full GC 빈도 증가 → STW 연장, Static ThreadLocal 미제거 시 Thread Pool 재사용 Thread에서 누수 | `jmap -histo <pid>` (size 증가 추이), `async-profiler -e alloc`, Prometheus `jvm_memory_used_bytes` |

---

## 10. Worker Thread와 DispatcherServlet 실행 관계

```
HTTP Request
  → NIC DMA 수신 → sk_buff 생성
  → TCP/IP Stack (SoftIRQ)
  → Socket Receive Buffer 적재
  → epoll_wait() 반환
  → Worker Thread 활성화
  → task_struct TASK_RUNNING
  → CFS RunQueue 배치
  → DispatcherServlet.doService()
  → DispatcherServlet.doDispatch()
  → HandlerMapping 조회
  → HandlerAdapter 실행
  → Controller 호출
```

`DispatcherServlet`은 실행 주체가 아닌 **실행 대상 객체**입니다.

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| 패킷 수신 → Thread 활성화 | Hardware + Kernel | NIC가 DMA로 메모리에 패킷 기록 → IRQ 발생 → SoftIRQ (`NET_RX_SOFTIRQ`) 처리 → `sk_buff`로 패킷 래핑 → TCP Stack 처리 → Socket Receive Buffer 적재 → `epoll_wait()` 반환 → Worker Thread Wakeup | `/proc/interrupts`, `mpstat` %soft 항목, `ethtool -S <nic>` (rx_packets) |
| task_struct 상태 전환 | Kernel | `epoll_wait()` 반환 시 `task_struct.state = TASK_RUNNING` → CFS RunQueue 삽입, `vruntime` 기준 스케줄링, Logical CPU 배치 후 JVM Worker Thread 코드 실행 | `/proc/<pid>/status` (State 항목), `ps -eLf` (T/S/R 상태), `perf sched latency` |
| Thread Pool → Worker 할당 | App + JVM | Tomcat NIO Connector: `Poller` Thread가 `epoll` 이벤트 감지 → `Executor` Thread Pool에서 Worker Thread 선택 → `SocketProcessor` 실행 → `DispatcherServlet.service()` 호출 | `jstack <pid> | grep "http-nio"`, Spring Actuator `/actuator/metrics/tomcat.threads.busy` |
| CFS 스케줄링 | Kernel | Worker Thread의 `task_struct.se.vruntime` 기준 Red-Black Tree 배치, Time Slice(~4ms) 소진 시 선점 → Context Switch, NUMA 인식 스케줄링으로 Local Memory 접근 우선 | `/proc/schedstat`, `vmstat 1` (cs 항목), `perf sched timehist` |

---

## 11. DispatcherServlet 실행 중 Kernel 관점

Kernel은 Java 객체를 직접 인식하지 않습니다. Kernel 관점에서는 다음과 같이 동작합니다.

```
task_struct
  → Logical CPU 점유
  → User Mode에서 JVM / JIT Code 실행
  → CPU Instruction 실행 (L1/L2 Cache 활용)
  → 필요 시 System Call → Kernel Mode 전환
```

`HandlerMapping`, JSON Binding, Validation, Interceptor 실행 등은 모두 Worker Thread의 CPU 시간을 소비합니다.

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| User Mode 실행 | Hardware + Kernel | JIT 컴파일된 Native Code가 CPU Ring 3(User Mode)에서 실행, System Call 없이 연속 실행 → CPU `us` 시간 증가, L1/L2 Cache Hit 시 고속 처리 | `mpstat 1` (us 항목), `perf stat -e instructions,cpu-cycles`, `toplev.py` |
| System Call 전환 | App → Kernel | I/O 필요 시 `read()` / `write()` / `epoll_wait()` 등 System Call → Ring 3 → Ring 0 전환 (Trap), CPU `sy` 시간 증가 | `mpstat 1` (sy 항목), `strace -cp <pid>` (System Call 통계), `/proc/<pid>/syscall` |
| CPU Pipeline 활용 | Hardware | OoO Execution Engine이 HandlerMapping 조회(순차 코드)를 파이프라인에서 투기 실행, Branch Predictor가 조건 분기(URL 매칭) 예측, 예측 실패 시 Pipeline Flush | `perf stat -e branch-misses,IPC`, `toplev.py` (Back-End Bound 분석) |
| vDSO 활용 | App (User-side) | Spring이 내부적으로 타임스탬프(`System.currentTimeMillis()`) 조회 시 `clock_gettime()` → vDSO 경유 → System Call 없이 Kernel 데이터 읽기 | `/proc/<pid>/maps` (vdso 항목), `perf stat` (clock_gettime 미등장 확인) |

---

## 12. Dispatcher 단계에서 CPU를 사용하는 작업

다음 작업들이 CPU를 소비합니다.

- URL Path Matching
- HandlerMapping 조회
- HandlerAdapter 선택
- Argument Resolver 실행
- JSON Deserialization (Serialization / Deserialization 비용)
- Validation
- Interceptor 실행
- Controller Method Invocation
- Response Serialization

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| JSON Deserialization | App + JVM + Hardware | Jackson `ObjectMapper.readValue()`: 바이트 파싱 → Java 객체 생성 → TLAB 할당 반복, 대용량 페이로드 시 CPU Bound + Young Gen 압박, Reflection 기반 필드 매핑 → Branch Misprediction 발생 가능 | `async-profiler -e cpu`, `perf stat -e branch-misses`, `-Xlog:gc+tlab` |
| HandlerMapping 조회 | App + JVM | `RequestMappingHandlerMapping`이 URL을 등록된 패턴 트리에서 매칭, AntPathMatcher vs PathPatternParser(Spring 5+), 패턴 복잡도에 따라 CPU 소비 증가 | `async-profiler -e cpu` (HandlerMapping 프레임 비율), Spring Actuator `/actuator/mappings` |
| Interceptor / Filter 비용 | App + JVM | `HandlerInterceptor.preHandle()` / `postHandle()` / `afterCompletion()`: 모든 요청에 실행, 인증/로깅 Interceptor 복잡도가 높으면 CPU Bound 병목 발생 | `async-profiler -e cpu` (Interceptor 프레임), `jfr` (Java Flight Recorder) |
| Serialization 비용 | App + JVM + Hardware | Response 직렬화: Jackson `ObjectMapper.writeValue()` → JSON 바이트 생성 → `HttpServletResponse.getOutputStream().write()` → System Call, 대형 응답 직렬화 시 CPU + Dirty Page Writeback 연계 가능 | `async-profiler -e cpu`, `strace -e write -cp <pid>` |

이 작업이 과도하면 **CPU Bound 병목**이 발생할 수 있습니다.

---

## 13. Dispatcher 내부에서 Blocking I/O가 발생하는 경우

Controller 또는 Service 내부에서 Blocking I/O가 발생하면 Worker Thread가 Wait Queue로 이동합니다.

**대표 사례**

- JDBC Query
- RestTemplate 호출
- FileInputStream
- Blocking 외부 SDK
- 동기식 HTTP Client

```
Worker Thread (task_struct)
  → DispatcherServlet.doDispatch()
  → Controller
  → Service
  → Blocking I/O (예: JDBC, RestTemplate)
  → System Call (read / write / epoll_wait)
  → task_struct.state = TASK_UNINTERRUPTIBLE
  → Wait Queue 이동 (CPU 반환)
  → I/O 완료 → IRQ → Wakeup → RunQueue 복귀
```

이때 `DispatcherServlet` 객체가 Blocking되는 것이 아니라, `DispatcherServlet`을 실행하던 **Worker Thread / task_struct가 Blocking**됩니다.

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| JDBC Blocking | App → Kernel | JDBC `executeQuery()` → Socket `read()` System Call → DB 응답 대기, `task_struct.state = TASK_UNINTERRUPTIBLE`, Thread Pool 점유 유지 → Connection Pool Exhaustion 연쇄 가능 | `ss -tp` (DB 연결 상태), `jstack <pid> | grep "Waiting on"`, `offcputime-bpfcc -p <pid>` |
| Connection Pool Exhaustion | App + Kernel | HikariCP Pool 고갈 시 대기 Thread가 `Condition.await()` → `futex(FUTEX_WAIT)` → Wait Queue, `connectionTimeout` 초과 시 `SQLTimeoutException`, Downstream 응답 지연이 Pool 고갈 가속 | Prometheus `hikaricp_pending_threads`, `hikaricp_connection_timeout_total`, `jstack <pid> | grep HikariCP` |
| RestTemplate Blocking | App → Kernel | 동기 HTTP Client: TCP `connect()` + `read()` System Call, 외부 API 지연 시 Worker Thread 전체 대기, Circuit Breaker(Resilience4j) 미적용 시 Retry Storm으로 Thread Pool 고갈 가속 | `ss -tnp`, `strace -e connect,read -p <pid>`, Prometheus `resilience4j_circuitbreaker_state` |
| Off-CPU Time 급증 | Kernel | Blocking 발생한 task_struct는 CPU 미사용, Off-CPU Time = 전체 응답 시간 - CPU Time, Blocking I/O 비율이 높을수록 Thread 수 증가 필요 → Context Switch 비용 증가 악순환 | `offcputime-bpfcc -p <pid> 30`, `bpftrace -e 'tracepoint:sched:sched_switch'` |
| sk_buff와 Socket Buffer | Kernel | TCP 데이터 수신 시 `sk_buff` 구조체로 패킷 래핑, Socket Receive Buffer(`sk_rcvbuf`) 포화 시 추가 패킷 Drop, `read()` 호출 전까지 Buffer에 누적 | `ss -tmn` (Recv-Q / Send-Q), `cat /proc/net/sockstat`, `sysctl net.core.rmem_max` |

---

## 14. Blocking 발생 시 자원 상태

| 계층 | 상태 | 메커니즘 실체 |
|------|------|--------------|
| Java Thread | 메서드 반환 전이므로 Thread Pool에 반환되지 않음 | Stack Frame 유지, Runnable 큐에서 제거 |
| JVM Stack | Controller / Service 호출 Stack Frame 유지 | 수십~수백 Frame 메모리 점유 유지 (`-Xss`) |
| Heap | Singleton 객체 유지, 요청 중간 객체도 GC 대상 아님 | Stack Frame에서 강한 참조 → GC Root |
| Kernel | task_struct가 Wait Queue로 이동, `TASK_UNINTERRUPTIBLE` | `/proc/<pid>/wchan` (대기 커널 함수) |
| CPU | 해당 task_struct는 CPU를 사용하지 않음 (Off-CPU) | `mpstat` us/sy 감소, `offcputime-bpfcc` 증가 |
| Thread Pool | 해당 Thread는 점유 상태 유지, 신규 요청 처리 불가 | `jstack` WAITING/BLOCKED 상태 Thread 증가 |
| Connection Pool | 대기 중인 DB/HTTP Connection 점유 유지 | `hikaricp_active_connections` 증가 |

---

## 15. 초기화 단계에서 발생하는 Kernel 동작

### 15.1 메모리 확보

```
JVM Startup
  → mmap() / brk()
  → Virtual Memory Area 생성
  → Page Table 설정
  → Page Fault로 물리 메모리 할당 (First-Touch)
```

**관련 System Call**: `mmap()`, `brk()`

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| Heap / Metaspace 확보 | JVM + Kernel | `mmap(MAP_ANONYMOUS)`: Heap, Code Cache, Metaspace 가상 주소 예약, `-Xms` 크기만큼 초기 `mmap()`, First-Touch 시 Page Fault → 물리 메모리 실제 할당, NUMA 환경에서 최초 접근 CPU의 NUMA Node 메모리에 배치 | `vmstat 1` (pgfault), `/proc/<pid>/smaps`, `numastat -p <pid>` |
| THP 적용 | Kernel | `khugepaged`: JVM Heap 영역의 연속 4KB Page를 2MB HugePage로 자동 승격, TLB Miss 감소 효과, GC Mark Phase에서 Heap 전체 순회 시 TLB 효율 향상 | `/sys/kernel/mm/transparent_hugepage/enabled`, `/proc/meminfo` (AnonHugePages), `perf stat -e dTLB-load-misses` |

### 15.2 클래스 및 설정 파일 로딩

```
ClassLoader
  → JAR / class 파일 접근
  → open() / read()
  → Page Cache 조회
  → Cache Miss 시 blk-mq I/O
  → Class Metadata 생성
  → Metaspace 적재
```

**관련 System Call**: `open()`, `read()`, `stat()`, `fstat()`

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| Page Cache 활용 | Kernel | 최초 기동 시: blk-mq를 통해 JAR 파일 Disk에서 읽기 → Page Cache 적재, 재기동 시: Page Cache Hit → Disk I/O 없이 클래스 로딩 가속 | `free -m` (buff/cache 증가 확인), `iostat -x 1` (기동 중 Disk 사용량), `vmstat 1` (pgmajfault) |
| ClassLoader Leak | JVM + Kernel | 재배포 시 구 ClassLoader가 Metaspace 메모리를 점유한 채 GC 미해제, Metaspace 점진적 증가 → `OutOfMemoryError: Metaspace`, Kubernetes Pod 재시작 반복 발생 | `jcmd <pid> VM.native_memory`, `-XX:+PrintMetaspaceStatistics`, Prometheus `jvm_memory_used_bytes{area="nonheap"}` |

### 15.3 Worker Thread Pool 생성

```
Java Thread.start()
  → JVM Native Layer
  → pthread_create()
  → clone(CLONE_THREAD | CLONE_VM | CLONE_SIGHAND)
  → task_struct 생성 (부모 프로세스의 mm_struct 공유)
  → CFS Scheduler RunQueue 등록
```

**관련 System Call**: `pthread_create()`, `clone()`

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| task_struct 생성 | Kernel | `clone()` 시스템 콜: `task_struct` 할당 → `mm_struct` 공유 (Thread 이므로 주소 공간 공유) → 독립 Stack 할당 → `sched_entity` 초기화 → CFS RunQueue 등록 | `cat /proc/<pid>/task/` (Thread 목록), `ps -eLf | grep java`, `ls /proc/<pid>/task/ | wc -l` |
| Thread Stack 할당 | Kernel + JVM | 각 Thread의 Kernel Stack: `alloc_thread_stack_node()`, JVM Stack: `mmap()` (`-Xss` 크기), Stack 영역 가상 주소 예약 → First-Touch 시 Page Fault로 물리 할당 | `/proc/<pid>/maps` (stack 영역), `ulimit -s` (스택 크기 제한) |
| CFS 등록 | Kernel | 신규 `task_struct`의 `vruntime = min_vruntime + sched_latency`, CFS Red-Black Tree에 삽입, Thread Pool 크기만큼 task_struct 상시 대기 (TASK_INTERRUPTIBLE) | `/proc/schedstat`, `perf sched record + report` |

### 15.4 동기화 처리

```
Thread 간 Lock 경합
  → futex(FUTEX_WAIT) / futex(FUTEX_WAKE)
  → Kernel 도움을 받아 대기 / 깨우기
  → task_struct Wait Queue ↔ RunQueue 이동
```

**관련 System Call**: `futex()`

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| Futex Fast Path | JVM + Hardware | Lock 미경합 시 User Space의 Futex 변수를 CAS(Compare-And-Swap)로 직접 획득 → System Call 없음, Kernel Mode 전환 비용 제로 | `perf stat -e cpu-cycles` (syscall 없이 높은 IPC 확인) |
| Futex Slow Path | JVM + Kernel | Lock 경합 시 `futex(FUTEX_WAIT)` System Call → `task_struct.state = TASK_INTERRUPTIBLE` → Wait Queue 삽입, Lock 해제 시 `futex(FUTEX_WAKE)` → 대기 Thread RunQueue 복귀 | `strace -e futex -cp <pid>`, `offcputime-bpfcc -p <pid>`, `perf lock record + report` |
| Spring Bean 초기화 Lock | App + Kernel | `DefaultSingletonBeanRegistry`의 `synchronized` 블록: Bean 최초 생성 시 경합 발생, `singletonObjects` Map 접근 직렬화 → `futex()` Slow Path 가능, 초기화 완료 후 Lock 경합 소멸 | `jstack <pid> | grep BLOCKED` (기동 중 확인), `strace -e futex` |

### 15.5 네트워크 포트 준비

```
Server Startup
  → socket()           TCP 소켓 생성
  → bind(8080)         포트 바인딩
  → listen()           SYN Queue / Accept Queue 초기화, TCP Backlog 설정
  → epoll_create()     epoll 인스턴스 생성
  → epoll_ctl()        소켓 FD 등록
  → epoll_wait()       이벤트 대기 (Poller Thread)
```

**관련 System Call**: `socket()`, `bind()`, `listen()`, `accept()`, `epoll_create()`, `epoll_ctl()`, `epoll_wait()`

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| TCP Backlog / SYN Queue | Kernel + App | `listen(backlog)`: SYN Queue(Half-Open 연결 대기) + Accept Queue(완료 연결 대기) 크기 설정, 큐 포화 시 신규 SYN 패킷 Drop → 클라이언트 Connection Timeout | `ss -ltn` (Recv-Q: Accept Queue 크기), `sysctl net.ipv4.tcp_max_syn_backlog`, `netstat -s | grep "SYN"` |
| epoll 이벤트 모델 | Kernel | `epoll_wait()`: 이벤트 준비된 FD만 반환 (Level-Triggered / Edge-Triggered), Poller Thread 1개가 수천 개 연결 관리, IRQ → SoftIRQ → `sk_buff` 처리 → epoll 이벤트 등록 체인 | `cat /proc/sys/fs/epoll/max_user_watches`, `strace -e epoll_wait -p <pid>` |
| RPS / RFS | Kernel | RPS(Receive Packet Steering): NIC IRQ를 여러 CPU에 분산, RFS(Receive Flow Steering): 패킷 처리 CPU를 Application Thread CPU와 동일하게 유도 → NUMA Local 접근, sk_buff 데이터 Cache Locality 향상 | `/sys/class/net/<nic>/queues/rx-*/rps_cpus`, `/proc/sys/net/core/rps_sock_flow_entries` |

---

## 16. Startup 단계 전체 흐름

```
java -jar app.jar
  → JVM Process 생성 (execve)
  → Heap / Metaspace 메모리 확보 (mmap, brk)
  → ClassLoader가 클래스 로딩 (openat, read) → Page Cache 활용
  → JIT Warm-up 대기 (Interpreter 모드 시작)
  → Spring ApplicationContext 생성
  → Singleton Bean 생성 → Heap(Old Gen) 적재
  → DispatcherServlet 생성 → HandlerMapping 초기화
  → Worker Thread Pool 준비 (clone × N)
  → Socket bind / listen (TCP Backlog 초기화)
  → epoll 등록 → Request 대기 (epoll_wait)
```

---

## 17. Request 처리 단계 전체 흐름

```
Client Request
  → NIC DMA 수신 → sk_buff 생성
  → IRQ 발생 → SoftIRQ(NET_RX_SOFTIRQ)
  → TCP/IP Stack 처리 (SYN Queue → Accept Queue)
  → Socket Receive Buffer 적재
  → epoll_wait() 반환 → Poller Thread 감지
  → Worker Thread 활성화 (task_struct TASK_RUNNING)
  → CFS Red-Black Tree → Logical CPU 배치
  → DispatcherServlet.doService() [JIT Native Code]
  → DispatcherServlet.doDispatch()
  → HandlerMapping 조회 (PathPattern 매칭)
  → HandlerAdapter → Argument Resolver → JSON Deserialization
  → Controller
  → Service → Business Logic
  → Repository (Blocking: JDBC / Non-blocking: R2DBC)
  → Response Serialization → write() System Call
  → TCP Send Buffer → NIC 송신
```

---

## 18. DispatcherServlet과 task_struct의 관계

```
DispatcherServlet  =  Heap(Old Gen)에 존재하는 Singleton 라우팅 객체
Worker Thread      =  DispatcherServlet을 실행하는 Java Thread (OS Thread 1:1)
task_struct        =  Worker Thread가 Kernel에서 실체화된 실행 단위
CFS Scheduler      =  task_struct.se.vruntime 기준으로 Logical CPU에 배치
```

```
Heap (Old Gen)
  └── DispatcherServlet  ← 실행 대상 (Singleton, GC Root 참조 유지)
          ↑ doDispatch() 호출
Java Worker Thread (JVM Stack 독립)
          ↓ 1:1 매핑
Linux task_struct (mm_struct 공유, 독립 Stack)
          ↓ vruntime 기준
CFS Scheduler (Red-Black Tree)
          ↓ Context Switch
Logical CPU (Physical Core + Hardware Thread)
```

### 계층별 동작 메커니즘

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구 / 키워드 |
|------|--------|--------------|----------------------|
| task_struct 수명 관리 | Kernel | Worker Thread Pool 생성 시 `clone()` → task_struct 할당, Thread Pool 유지 중 TASK_INTERRUPTIBLE 대기, 요청 처리 중 TASK_RUNNING, Blocking I/O 시 TASK_UNINTERRUPTIBLE | `/proc/<pid>/task/<tid>/status`, `ps -eLf` |
| Context Switch 비용 | Kernel + Hardware | Worker Thread 간 Context Switch: Register 저장/복원, TLB Flush(PCID 미사용 시), Cache Cold Start, 과도한 Thread 수 → Context Switch 빈도 증가 → `sy` CPU 증가 | `vmstat 1` (cs 항목), `pidstat -w 1`, `perf stat -e context-switches` |
| NUMA 배치 | Kernel + Hardware | CFS `select_task_rq_fair()`: Worker Thread를 Local NUMA Node CPU에 우선 배치, Heap 객체(Singleton Bean)도 동일 Node 메모리에 위치할수록 Cache Locality 향상 | `numastat -p <pid>`, `/proc/<pid>/numa_maps`, `numactl --hardware` |

---

## 19. 성능 병목 관점

| 병목 유형 | 원인 | 메커니즘 실체 | SRE 분석 도구 |
|----------|------|--------------|--------------|
| CPU `us` 증가 | HandlerMapping, JSON Deserialization/Serialization, Validation | JIT C2 미적용 초기 Warm-up, Branch Misprediction, TLAB 할당 반복 | `async-profiler -e cpu`, `perf stat -e branch-misses` |
| Thread Pool 고갈 | Controller / Service 내부 Blocking I/O | task_struct TASK_UNINTERRUPTIBLE, Connection Pool Exhaustion 연쇄 | `jstack <pid>`, `offcputime-bpfcc`, `hikaricp_pending_threads` |
| CPU `sy` 증가 | File I/O, Socket I/O, System Call 과다 | Ring 3 → Ring 0 전환 반복, Interrupt 처리 빈도 증가 | `mpstat 1` (sy), `strace -cp <pid>` |
| GC Pause 증가 | 요청별 객체 생성 과다, 잘못된 Singleton 구조 | Young Gen Minor GC, Safepoint STW, TLAB 조기 소진 | `jstat -gcutil`, Prometheus `jvm_gc_pause_seconds` |
| Lock 경합 | Singleton Bean 내부 공유 상태, Synchronized 과다 | Futex Slow Path, Off-CPU Time 증가, Biased → Fat Lock 에스컬레이션 | `perf lock record`, `offcputime-bpfcc`, `jstack` BLOCKED |
| Tail Latency 증가 | Filter/Interceptor 과다, GC Safepoint, Throttling | STW Safepoint 대기, cgroup CFS Throttling, Context Switch 누적 | P99 `jfr`, `/sys/fs/cgroup/cpu.stat`, `pidstat -w` |
| Metaspace OOM | ClassLoader Leak, 재배포 반복 | 구 ClassLoader GC 미해제, Metaspace 지속 증가 | `jcmd <pid> VM.native_memory`, `-XX:+PrintMetaspaceStatistics` |

---

## 20. SRE 관점 주요 지표

| 지표 | 의미 | 분석 도구 |
|------|------|----------|
| JVM Thread Count | Worker Thread 증가 여부, Thread Leak 탐지 | `jstack <pid> | grep "Thread" | wc -l`, JMX `java.lang:type=Threading` |
| Active Thread Count | Thread Pool 점유 상태 | Spring Actuator `/actuator/metrics/tomcat.threads.busy` |
| Runnable Thread 수 | CPU 경쟁 상태 (TASK_RUNNING 비율) | `jstack <pid> | grep RUNNABLE | wc -l` |
| Waiting Thread 수 | I/O 또는 Lock 대기 (Off-CPU Time) | `jstack <pid> | grep -E "WAITING|BLOCKED"`, `offcputime-bpfcc` |
| CPU `us` | JIT Native Code 실행 비중 | `mpstat 1` (us 항목), `perf stat -e instructions` |
| CPU `sy` | System Call / Kernel 처리 비중 | `mpstat 1` (sy 항목), `strace -cp <pid>` |
| GC Pause | Minor/Major GC STW 시간 | `jstat -gcutil <pid> 1000`, Prometheus `jvm_gc_pause_seconds` |
| Allocation Rate | 요청당 객체 생성량, TLAB 소진 빈도 | `async-profiler -e alloc`, `-Xlog:gc+tlab=debug` |
| Context Switch | task_struct 전환 비용, Thread 과다 여부 | `vmstat 1` (cs), `pidstat -w 1` |
| Load Average | TASK_RUNNING + TASK_UNINTERRUPTIBLE 누적 | `uptime`, `cat /proc/loadavg` |
| PSI (Pressure Stall) | CPU / Memory / IO 자원 부족 압력 | `/proc/pressure/cpu`, `/proc/pressure/memory`, `/proc/pressure/io` |
| P99 / P999 Latency | 사용자 관점 지연, Tail Latency 악화 탐지 | `jfr`, Prometheus Histogram, `async-profiler` |

---

## 21. 핵심 정리

```
DispatcherServlet  →  Heap(Old Gen)에 존재하는 Singleton 라우팅 객체
                      JIT C2로 컴파일된 doDispatch() Native Code 반복 실행
Worker Thread      →  DispatcherServlet 메서드를 실행하는 Java 실행 흐름
                      OS Thread 1:1 매핑, 독립 JVM Stack Frame 보유
task_struct        →  Worker Thread를 Kernel이 Logical CPU에 스케줄링하는 실행 단위
                      CFS vruntime 기준 Red-Black Tree 배치
```

**Blocking I/O 발생 시**: `DispatcherServlet` 객체가 멈추는 것이 아니라, `DispatcherServlet`을 실행하던 Worker Thread / `task_struct`가 `TASK_UNINTERRUPTIBLE` 상태로 Wait Queue에 이동하고, Thread Pool에 반환되지 않아 신규 요청 처리 능력이 감소합니다. Connection Pool Exhaustion 연쇄로 이어질 수 있습니다.

**Startup 단계**: JVM은 `mmap()`, `brk()`, `openat()`, `read()`, `clone()`, `futex()`, `socket()`, `bind()`, `listen()`, `epoll_create()`, `epoll_ctl()` 등 다양한 System Call을 통해 메모리 확보 → 클래스 로딩(Page Cache 활용) → Thread Pool 생성 → 네트워크 수신 준비를 순차 수행합니다.

**성능 최적화 핵심**: JIT Warm-up 완료(Tier 4 C2 컴파일) 전 트래픽 유입 억제, Singleton Stateless 설계 유지, Thread Pool 크기와 Connection Pool 크기 균형, Blocking I/O를 Non-blocking(WebFlux / Virtual Thread)으로 전환, PSI / GC Pause / Off-CPU Time 상시 모니터링.

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*