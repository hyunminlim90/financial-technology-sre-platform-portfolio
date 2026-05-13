# RequestDispatcher: Servlet Container 내부 전달 구조 (E2E 분석 적용됨)

## 개요

`RequestDispatcher`는 Servlet 스펙에서 제공하는 내부 요청 전달 인터페이스다.

클라이언트가 보낸 하나의 HTTP 요청을 서버 내부의 다른 자원으로 전달하거나,
다른 자원의 실행 결과를 현재 응답에 포함할 때 사용된다.

```
Client Request
    ↓
Servlet Container
    ↓
Servlet / DispatcherServlet
    ↓
RequestDispatcher
    ↓
Target Resource
```

DispatcherServlet이 외부 HTTP 요청을 Spring MVC 내부로 진입시키는 Front Controller라면,
RequestDispatcher는 Servlet Container 내부에서 자원 간 실행 흐름을 전달하는 구조다.

---

## 1. RequestDispatcher의 계층적 위치

RequestDispatcher는 Java Application 객체이면서 Servlet Container가 제공하는 Servlet API 계층에 속한다.

```
Application Layer
    └── Servlet / Spring MVC Code

Servlet Container Layer
    └── RequestDispatcher 구현체
        └── Tomcat 기준 ApplicationDispatcher

JVM Layer (Runtime)
    ├── Heap
    │   ├── HttpServletRequest
    │   ├── HttpServletResponse
    │   └── RequestDispatcher 구현 객체
    ├── Metaspace
    │   └── 대상 Servlet / JSP 클래스 메타데이터
    └── Thread Stack
        └── Worker Thread Stack Frame

OS Kernel Layer
    ├── task_struct (Worker Thread에 대응하는 커널 실행 단위)
    ├── Runqueue / Wait Queue
    ├── VFS (Virtual File System)
    └── Socket Buffer (sk_buff)

Hardware Layer
    └── Logical CPU / Physical Core
        ├── L1 / L2 / L3 Cache
        ├── TLB (Translation Lookaside Buffer)
        └── CPU Pipeline
```

### 핵심 구분

Kernel이 스케줄링하는 대상은 RequestDispatcher 객체가 아니라,
해당 객체의 메서드를 실행하는 Worker Thread의 `task_struct`다.

RequestDispatcher 자체는 `task_struct` 생성 단위가 아니며,
기존 Worker Thread의 실행 흐름 위에서 메서드 호출로 처리된다.

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| Application | Thread Dump, `ApplicationDispatcher.invoke` 호출 스택 |
| JVM Runtime | JVM Heap Dump, `-XX:+PrintGCDetails`, JFR |
| OS Kernel | `strace`, `/proc/PID/status`, `pidstat -w`, `vmstat cs` |
| Hardware | `perf stat -e cache-misses`, `perf stat -e dTLB-load-misses` |

---

## 2. RequestDispatcher의 생성 및 획득

RequestDispatcher는 일반적인 Spring Singleton Bean처럼 애플리케이션 전체에서 하나만 공유되는 객체가 아니다.
요청 처리 중 특정 경로나 이름을 기준으로 Servlet Container에서 조회하거나 생성한다.

```java
RequestDispatcher dispatcher =
        request.getRequestDispatcher("/target");
```

또는 ServletContext를 통해 이름 기반으로 가져올 수 있다.

```java
RequestDispatcher dispatcher =
        getServletContext().getNamedDispatcher("targetServlet");
```

Tomcat에서는 내부적으로 `ApplicationDispatcher` 계열 객체가 사용된다.

```
request.getRequestDispatcher("/target")
    ↓
Servlet Container
    ↓
Path Resolution
    │   ├── URL Decode
    │   ├── Context Path 분리
    │   └── Servlet Mapping 조회
    ↓
ApplicationDispatcher 준비
```

### 경로 해석 단계의 계층별 동작

| 계층 | 동작 메커니즘 |
|------|--------------|
| Application | Servlet Mapping 규칙 매칭 (exact, prefix, extension) |
| JVM Runtime | `ConcurrentHashMap` 기반 Mapping 캐시 조회 |
| OS Kernel | 일반적으로 System Call 불필요. 메모리 내 조회 |
| Hardware | L1/L2 Cache 히트 여부에 따라 매핑 조회 비용 결정 |

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| Application | Servlet Container 로그 (`FINE`/`TRACE` 레벨) |
| JVM Runtime | JFR Method Profiling |
| OS Kernel | `/proc/PID/syscall` (System Call 발생 여부 확인) |

---

## 3. RequestDispatcher의 핵심 기능

RequestDispatcher는 두 가지 주요 메서드를 제공한다.

- `forward(request, response)`
- `include(request, response)`

두 메서드는 모두 서버 내부에서 다른 자원을 실행한다는 공통점이 있지만,
제어권과 응답 처리 방식이 다르다.

---

## 4. forward()

`forward()`는 현재 요청의 제어권을 서버 내부의 다른 자원으로 전달한다.

```
Servlet A
    ↓
RequestDispatcher.forward()
    ↓
Servlet B / JSP / Static Resource
```

### 특징

- 클라이언트 URL은 변경되지 않는다.
- 서버 내부에서만 대상 자원이 변경된다.
- 기존 `HttpServletRequest`, `HttpServletResponse` 객체가 그대로 전달된다.
- 응답이 이미 커밋되기 전에 호출해야 한다.
- `forward()` 이후에는 현재 Servlet에서 응답을 계속 작성하지 않는 것이 원칙이다.

### forward() 전 계층 실행 흐름

```
Worker Thread (task_struct Running 상태)
    ↓
Servlet A.service()                          ← JVM Stack Frame 추가
    ↓
request.getRequestDispatcher("/target")      ← Heap에서 객체 조회
    ↓
RequestDispatcher.forward(request, response) ← JVM Stack Frame 추가
    │
    ├── Filter Chain 재진입 여부 결정
    │   └── DispatcherType.FORWARD 판별
    ├── Request Attribute 설정
    │   └── javax.servlet.forward.* / jakarta.servlet.forward.*
    ├── Response Buffer 상태 확인
    │   └── committed 여부 체크 → IllegalStateException 방지
    └── Target Servlet 호출
    ↓
TargetServlet.service()                      ← JVM Stack Frame 추가
    ↓
Response 작성
```

### 계층별 동작

| 계층 | 동작 메커니즘 |
|------|--------------|
| Application | Servlet A → ApplicationDispatcher → Servlet B 메서드 호출 연쇄 |
| JVM Runtime | Thread Stack에 Frame 누적. Heap의 동일 Request/Response 참조 유지 |
| OS Kernel | 동적 자원 호출 시 추가 System Call 없음. Blocking I/O 발생 시 `task_struct` Wait Queue 이동 |
| Hardware | CPU Pipeline 연속 실행. L1/L2 Cache는 동일 Thread 컨텍스트 유지 |

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| Application | Thread Dump (`ApplicationDispatcher.doForward` 호출 위치) |
| JVM Runtime | JFR Stack Trace, `-Xss` 설정 (Stack 깊이 제한) |
| OS Kernel | `strace -p <PID>` (System Call 발생 여부), `/proc/PID/wchan` (대기 원인) |
| Hardware | `perf record -g` (CPU 호출 스택 샘플링) |

---

## 5. include()

`include()`는 다른 자원의 실행 결과를 현재 응답에 포함한다.

```
Servlet A
    ↓
RequestDispatcher.include()
    ↓
Servlet B / JSP 실행
    ↓
Servlet A로 복귀
    ↓
Response 계속 작성
```

### 특징

- 호출한 Servlet의 실행 흐름으로 다시 돌아온다.
- 공통 영역, JSP 조각, 내부 컴포넌트 결과를 응답에 포함할 때 사용된다.
- 기존 Request/Response 객체를 공유한다.
- 포함된 자원은 응답 상태 코드나 헤더를 변경하는 데 제한이 있다.

### include() 전 계층 실행 흐름

```
Worker Thread (task_struct Running 상태)
    ↓
Servlet A.service()                           ← JVM Stack Frame 유지
    ↓
RequestDispatcher.include(request, response)  ← JVM Stack Frame 추가
    │
    ├── DispatcherType.INCLUDE 판별
    ├── Response Wrapper 적용
    │   └── 상태 코드 / 헤더 변경 차단 (IncludedResponse 래핑)
    ├── Request Attribute 설정
    │   └── javax.servlet.include.* / jakarta.servlet.include.*
    └── IncludedServlet.service() 호출
    ↓
IncludedServlet.service()                     ← JVM Stack Frame 추가
    ↓
Response Body 일부 작성
    ↓
복귀 (Stack Frame 반환)
    ↓
Servlet A.service() 계속 실행
```

### 계층별 동작

| 계층 | 동작 메커니즘 |
|------|--------------|
| Application | Servlet A → ApplicationDispatcher → IncludedServlet → 복귀 |
| JVM Runtime | include 완료 후 Stack Frame 반환. Heap의 동일 Request/Response 참조 공유 |
| OS Kernel | 동적 자원은 추가 System Call 없음. Response 버퍼 플러시 시 `write()` 발생 |
| Hardware | 복귀 과정에서 동일 CPU 실행 컨텍스트 유지 |

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| Application | Thread Dump (`ApplicationDispatcher.include` 호출 위치) |
| JVM Runtime | JFR Method Profiling, Thread Stack 깊이 모니터링 |
| OS Kernel | `strace` (write 시스템 콜 빈도), `/proc/PID/fdinfo` |

---

## 6. forward()와 include() 비교

| 구분 | forward() | include() |
|------|-----------|-----------|
| 목적 | 제어권 전달 | 응답 일부 포함 |
| 호출 후 흐름 | 대상 자원 중심으로 처리 | 호출한 자원으로 복귀 |
| URL 변경 | 없음 | 없음 |
| Request 객체 | 동일 객체 공유 | 동일 객체 공유 |
| Response 객체 | 동일 객체 공유 | Wrapper로 헤더/상태 변경 제한 |
| Thread 생성 | 없음 | 없음 |
| task_struct 생성 | 없음 | 없음 |
| System Call | 동적 자원: 없음 / 정적 자원: 발생 | 동적 자원: 없음 / 정적 자원: 발생 |
| JVM Stack 증가 | forward 대상 자원까지 Frame 증가 | include 대상 자원 실행 후 Frame 반환 |
| 주요 사용 | 내부 라우팅, JSP 전달 | 공통 화면 조각 포함 |

---

## 7. task_struct 관점의 핵심

RequestDispatcher 호출은 Kernel 수준에서 새로운 실행 단위를 만들지 않는다.

다음 작업은 발생하지 않는다.

```
new Thread()          → Java 수준 Thread 객체 생성 없음
Thread.start()        → JVM Thread 시작 없음
pthread_create()      → POSIX Thread 생성 없음
clone()               → Linux clone() 시스템 콜 없음
task_struct 생성      → Kernel 실행 단위 추가 없음
```

실행 흐름은 기존 Worker Thread 안에서 이어진다.

```
Worker Thread
    ↓
Linux task_struct A (상태: TASK_RUNNING)
    ↓
Servlet A.service()
    ↓
RequestDispatcher.forward()
    ↓
TargetServlet.service()
    ↓
(모든 과정이 동일한 task_struct A에서 실행)
```

따라서 RequestDispatcher 자체로 인한 Kernel Thread 생성 비용이나 Context Switch 비용은 발생하지 않는다.

### Blocking I/O 발생 시 task_struct 상태 전이

대상 자원 내부에서 Blocking I/O가 발생하면,
기존 Worker Thread의 `task_struct`가 Wait Queue로 이동한다.

```
task_struct 상태 전이:
    TASK_RUNNING
        ↓ (Blocking I/O 진입)
    TASK_INTERRUPTIBLE 또는 TASK_UNINTERRUPTIBLE
        ↓ (I/O 완료 시 IRQ/SoftIRQ가 Wake-up)
    TASK_RUNNING (Runqueue 재진입)
```

이때 CFS(Completely Fair Scheduler)는 해당 `task_struct`를 Runqueue에서 제거하고
다른 `task_struct`를 CPU에 스케줄링한다.

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| OS Kernel | `/proc/PID/status` (`State` 필드: S=Sleeping, R=Running), `pidstat -w` (Context Switch 횟수), `perf sched` |
| OS Kernel | `/proc/PID/wchan` (어떤 Kernel 함수에서 대기 중인지 확인) |
| Hardware | `perf stat -e context-switches` |

---

## 8. JVM Stack 관점

`forward()` 또는 `include()`는 메서드 호출 흐름으로 이어진다.
따라서 JVM Stack에는 호출 프레임이 추가된다.

### forward() Stack 구조

```
Thread Stack (위가 최근 Frame)
    ├── TargetServlet.service()
    ├── ApplicationDispatcher.invoke()
    ├── ApplicationDispatcher.doForward()
    ├── ApplicationDispatcher.forward()
    ├── ServletA.service()
    └── Container Worker Invocation (HttpProcessor 등)
```

### include() Stack 구조 (include 실행 중)

```
Thread Stack (위가 최근 Frame)
    ├── IncludedServlet.service()
    ├── ApplicationDispatcher.invoke()
    ├── ApplicationDispatcher.doInclude()
    ├── ApplicationDispatcher.include()
    ├── ServletA.service()
    └── Container Worker Invocation
```

include 완료 후 IncludedServlet 관련 Frame이 반환되고 Servlet A 실행이 재개된다.

### Stack 깊이와 JIT Compilation

JVM의 JIT Compiler(C1/C2)는 자주 호출되는 메서드 체인을 Inline 최적화할 수 있다.
RequestDispatcher 내부의 얕은 메서드들은 C2 Compiler에 의해 인라인되어 실제 Stack Frame 생성 비용이 줄어들 수 있다.
단, 깊은 forward 체인은 Inline 한계를 초과하여 실제 Frame이 누적된다.

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| Application | Thread Dump (호출 스택 전체 확인) |
| JVM Runtime | `-Xss` (Stack 크기 설정), `-XX:+PrintCompilation` (JIT 인라인 여부), JFR Method Profiling |
| JVM Runtime | `-XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining` |
| OS Kernel | `pstack <PID>` (네이티브 스택 확인) |

---

## 9. Heap 관점

`HttpServletRequest`와 `HttpServletResponse` 객체는 Heap에 존재한다.
RequestDispatcher는 새로운 요청 객체를 만드는 것이 아니라
기존 Request/Response 객체를 대상 자원으로 전달한다.

```
Heap (Young Generation / Eden)
    ├── HttpServletRequest 객체 (요청 당 1개)
    ├── HttpServletResponse 객체 (요청 당 1개)
    ├── RequestDispatcher 구현 객체 (ApplicationDispatcher)
    ├── Request Wrapper 객체 (forward/include 시 생성)
    └── Response Wrapper 객체 (include 시 생성)

Thread Stack
    └── request / response 참조값 보관 (Stack Frame별 로컬 변수)
```

여러 Stack Frame은 동일한 Request/Response 인스턴스를 참조한다.

### Wrapper 객체 할당과 GC 영향

`include()` 실행 시 Response Wrapper(`ApplicationHttpResponse` 등)가 추가로 생성된다.
이 객체들은 단명(short-lived) 객체로 Young Generation에 할당되며,
요청 처리 완료 후 Minor GC 대상이 된다.

과도한 include 호출은 Young Generation Allocation Rate를 높여
Minor GC 빈도를 증가시킬 수 있다.

### TLAB (Thread-Local Allocation Buffer)

JVM은 각 Thread에 TLAB을 할당하여 Heap 할당 시 동기화 비용을 제거한다.
RequestDispatcher 내부의 Wrapper 객체 생성도 TLAB에서 이루어진다.
TLAB가 소진되면 Eden 영역에서 새 TLAB를 할당하며 이 시점에 짧은 동기화가 발생할 수 있다.

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| JVM Runtime | JVM Heap Dump (`jmap -dump`), JFR Memory Profiling |
| JVM Runtime | `-XX:+PrintTLAB` (TLAB 할당 통계), GC 로그 (`-Xlog:gc*`) |
| JVM Runtime | `jstat -gcnew <PID>` (Young Generation 상태) |
| OS Kernel | `/proc/PID/smaps` (Heap 영역 메모리 매핑 확인) |

---

## 10. Metaspace 관점

대상 Servlet 또는 JSP가 실행되려면 해당 클래스 메타데이터가 JVM에 로드되어 있어야 한다.

```
TargetServlet.class
    ↓
ClassLoader (WebAppClassLoader)
    ↓
Metaspace (클래스 메타데이터 저장)
    ↓
TargetServlet.service() 실행 가능
```

JSP의 경우 최초 호출 시 다음 과정이 추가로 발생한다.

```
JSP 파일
    ↓
JspServlet 처리
    ↓
Java 소스 코드 생성 (_jsp.java)
    ↓
javac / ECJ 컴파일 (CPU / File I/O 발생)
    ↓
.class 파일 생성 (File Write System Call)
    ↓
ClassLoader 로드
    ↓
Metaspace 적재
    ↓
JIT Warmup (C1 → C2 단계적 컴파일)
```

### ClassLoader Leak 위험

Servlet Container가 WebApp을 재배포할 때 WebAppClassLoader가 교체된다.
이때 이전 ClassLoader가 GC 대상이 되지 못하면 Metaspace에 클래스 메타데이터가 누적된다.
RequestDispatcher를 통해 자주 호출되는 JSP나 동적 클래스가 많을수록
ClassLoader Leak 발생 시 Metaspace 고갈 위험이 높아진다.

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| JVM Runtime | `jcmd <PID> VM.classloader_stats` (ClassLoader 현황) |
| JVM Runtime | `-XX:MaxMetaspaceSize` (Metaspace 상한 설정), `-Xlog:class+load` |
| JVM Runtime | JFR Class Loading Event, VisualVM ClassLoader 탭 |
| OS Kernel | `/proc/PID/maps` (Metaspace 영역 확인) |

---

## 11. User Mode와 Kernel Mode 관점

동적 Servlet으로 `forward`/`include`하는 경우 대부분 JVM 내부 메서드 호출로 처리된다.

```
User Mode
    └── Java Method Invocation (System Call 없음)
        └── CPU Ring 3에서 실행
```

하지만 대상 자원이 정적 파일이거나, JSP 컴파일, 파일 읽기, 네트워크 응답 쓰기 등이 포함되면
Kernel과 상호작용한다.

```
정적 자원 / 파일 I/O / 네트워크 I/O
    ↓
System Call (User Mode → Kernel Mode 전환)
    │   ├── open() / read() / sendfile()
    │   ├── write() / send()
    │   └── CPU Ring 3 → Ring 0 전환 (Trap)
    ↓
Kernel Mode 실행
    ↓
User Mode 복귀
```

### vDSO (Virtual Dynamic Shared Object)

타임스탬프 조회(`clock_gettime`) 등 일부 Kernel 데이터 접근은 vDSO를 통해
Mode 전환 없이 User Space에서 직접 읽는다.
RequestDispatcher 내부의 Latency 측정이나 타임아웃 계산이 이 경로를 사용할 수 있다.

### Mode 전환 비용

User Mode → Kernel Mode 전환은 레지스터 저장, 스택 전환, TLB 고려 등의 비용이 발생한다.
RequestDispatcher가 정적 자원을 반복 전달하는 구조라면 이 비용이 누적될 수 있다.

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| OS Kernel | `strace -c -p <PID>` (System Call 종류 및 횟수 집계) |
| OS Kernel | `perf trace` (System Call 추적), `/proc/PID/syscall` (현재 System Call) |
| OS Kernel | `mpstat` (`%sys` 항목: Kernel Mode CPU 사용률) |
| Hardware | `perf stat -e cpu-clock,task-clock` |

---

## 12. 정적 자원으로 전달되는 경우

정적 HTML, 이미지, CSS, JS 파일로 `forward`되는 경우
Servlet Container는 파일 시스템에서 데이터를 읽어 응답해야 한다.

```
RequestDispatcher.forward("/static/page.html")
    ↓
Container Static Resource Handler (DefaultServlet 등)
    ↓
VFS (Virtual File System)
    ↓
Page Cache 확인
    ├── Cache Hit: 메모리에서 읽기 (Minor Page Fault 가능)
    └── Cache Miss: Disk I/O 발생 (Major Page Fault)
    ↓
read() 또는 sendfile() System Call
    ↓
Socket Buffer (sk_buff) 적재
    ↓
TCP/IP Stack 처리 (SoftIRQ)
    ↓
NIC Tx Ring Buffer
    ↓
클라이언트 전송
```

### sendfile()과 Zero-Copy

`sendfile()` System Call은 Kernel 공간에서 파일 데이터를 Socket Buffer로 직접 복사한다.
User Space 버퍼를 거치지 않으므로 메모리 복사 횟수가 줄어든다.
Tomcat의 `DefaultServlet`은 조건에 따라 `sendfile()`을 사용한다.

### Page Cache와 Dirty Page Writeback

정적 자원 읽기는 Page Cache를 통해 이루어진다.
Page Cache Hit 시 Disk I/O 없이 메모리에서 응답 데이터를 읽는다.
메모리 압박 시 `kswapd`가 Page Cache를 회수하여 이후 요청에서 Cache Miss가 발생할 수 있다.

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| OS Kernel | `free -m` (buff/cache 항목), `vmstat` (pgfault / pgmajfault) |
| OS Kernel | `/proc/vmstat` (Page Fault 상세), `cachestat` (BCC tools) |
| OS Kernel | `strace -e trace=read,sendfile,write -p <PID>` |
| OS Kernel | `iostat -x` (Disk I/O 대기율 `%await`) |
| Hardware | `perf stat -e dTLB-load-misses` (Page Cache 매핑 TLB Miss) |

---

## 13. 동적 자원으로 전달되는 경우

다른 Servlet, JSP, Spring Controller 유사 경로로 전달되는 경우
주된 작업은 JVM 내부 메서드 호출이다.

```
Servlet A
    ↓
RequestDispatcher.forward()
    ↓
Filter Chain 재진입 (DispatcherType.FORWARD 필터 적용)
    ↓
Servlet B.service()
    ↓
(Servlet B 내부 로직 실행)
```

이 경우 RequestDispatcher 자체는 User Mode에서 실행된다.
그러나 Servlet B 내부에서 DB, 외부 API, 파일 I/O를 수행하면
해당 지점에서 System Call과 Blocking이 발생한다.

### JIT Compilation과 Safepoint

JVM의 JIT Compiler(C2)는 자주 실행되는 `forward` 경로를 컴파일하고 최적화한다.
Safepoint는 JVM이 GC, 클래스 재정의 등을 수행하기 위해 모든 Thread를 안전한 지점에서 정지시키는 메커니즘이다.
`forward()` 호출 중 Safepoint 요청이 발생하면 해당 Thread는 다음 Safepoint 지점까지 실행 후 일시 정지된다.
이로 인해 특정 요청에서 예상치 못한 지연이 관찰될 수 있다.

### Serialization / Deserialization 비용

대상 자원으로 전달되는 Request Attribute에 복잡한 객체가 담겨 있고,
이를 직렬화/역직렬화하는 로직이 포함된 경우 CPU 비용이 발생한다.
특히 Reflection 기반 직렬화(`ObjectOutputStream`, Jackson 등)는 CPU 집약적이다.

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| Application | APM (Pinpoint, Jaeger), Span 단위 Latency 측정 |
| JVM Runtime | `-XX:+PrintSafepointStatistics`, JFR Safepoint Event |
| JVM Runtime | JFR Method Profiling (CPU 시간 상위 메서드 확인) |
| OS Kernel | `perf top` (CPU 사용 함수 실시간 확인) |

---

## 14. Blocking I/O 발생 시 흐름

대상 자원 내부에서 Blocking I/O가 발생하면 기존 Worker Thread 전체가 대기 상태가 된다.

```
Worker Thread / task_struct A (TASK_RUNNING)
    ↓
Servlet A.service()
    ↓
RequestDispatcher.forward()
    ↓
Servlet B.service()
    ↓
JDBC Query / Blocking HTTP Client / File I/O
    ↓
System Call (read, recv, epoll_wait 등)
    ↓
Kernel: task_struct A → Wait Queue (TASK_INTERRUPTIBLE)
    ↓
I/O 완료 시 IRQ / SoftIRQ 발생
    ↓
IRQ Handler → Wait Queue에서 task_struct A Wake-up
    ↓
Runqueue 재진입 → CPU 재할당 → 실행 재개
```

RequestDispatcher가 별도의 Thread를 생성하지 않았기 때문에,
호출한 쪽과 대상 자원은 동일한 Worker Thread의 생명주기를 공유한다.

### Connection Pool Exhaustion

Blocking I/O 대기 중인 Worker Thread가 증가하면 Thread Pool 고갈이 발생한다.
이때 신규 요청을 처리할 Worker Thread가 없어 요청 큐가 쌓이고
최종적으로 Connection Refused 또는 Request Timeout이 발생한다.

```
Blocking I/O 증가
    ↓
Worker Thread 점유 증가 (Thread Pool 소진)
    ↓
신규 요청 수락 불가 (TCP Backlog 소진 가능)
    ↓
SYN Queue / Accept Queue 포화
    ↓
클라이언트 Connection Timeout
```

### Off-CPU Time

Blocking I/O로 인해 Thread가 CPU를 사용하지 않는 시간을 Off-CPU Time이라 한다.
On-CPU Profiling(샘플링 기반)으로는 Off-CPU Time이 관찰되지 않으며,
별도의 Off-CPU Profiling 도구가 필요하다.

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| Application | Thread Dump (BLOCKED / WAITING 상태 Thread 확인) |
| JVM Runtime | JFR Thread State, `jstack` |
| OS Kernel | `pidstat -w` (Context Switch 횟수), `/proc/PID/wchan` |
| OS Kernel | `offcputime` (BCC tools, Off-CPU Flame Graph 생성) |
| OS Kernel | `ss -s` (Socket 상태), `netstat -s` (TCP 통계) |
| Hardware | `perf stat -e context-switches` |

---

## 15. Blocking 시 자원 상태

| 계층 | 상태 |
|------|------|
| Java Thread | Thread Pool에 반환되지 않음 (점유 유지) |
| JVM Stack | Servlet A, RequestDispatcher, Servlet B 호출 프레임 유지 |
| JVM Heap | Request / Response 객체 유지 (GC 대상 아님) |
| OS Kernel | `task_struct`가 Wait Queue로 이동 (TASK_INTERRUPTIBLE) |
| OS Kernel | Futex 기반 Lock 대기 시 TASK_INTERRUPTIBLE 또는 TASK_UNINTERRUPTIBLE |
| CPU | 해당 `task_struct`는 CPU 미사용. CFS가 다른 `task_struct` 스케줄링 |
| Thread Pool | Worker Thread 점유 상태 유지 (신규 요청 처리 불가) |
| Socket | TCP Receive Buffer (sk_buff) 데이터 대기 가능 |

### TCP Backlog / SYN Queue 영향

Worker Thread가 고갈되면 `accept()`를 호출하는 속도가 저하된다.
Kernel의 SYN Queue(SYN_RECV 상태 연결 보관)와 Accept Queue(3-way handshake 완료 연결 보관)가 포화되면
신규 SYN 패킷이 Drop된다.

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| OS Kernel | `ss -lnt` (Listen Socket 상태, Recv-Q 확인) |
| OS Kernel | `/proc/net/tcp` (SYN Queue 상태) |
| OS Kernel | `netstat -s` (`SYNs to LISTEN sockets dropped` 항목) |
| OS Kernel | `sysctl net.core.somaxconn`, `net.ipv4.tcp_max_syn_backlog` |

---

## 16. RequestDispatcher 단계에서 CPU를 사용하는 작업

RequestDispatcher 실행 중에는 다음 작업이 CPU를 사용한다.

```
RequestDispatcher
    ↓
Path Resolution          (URL 파싱, 컨텍스트 경로 분리)
    ↓
Mapping Lookup           (Servlet 매핑 테이블 조회, 해시맵 연산)
    ↓
DispatcherType 판별      (FORWARD / INCLUDE / REQUEST 구분)
    ↓
Request Attribute 설정   (HashMap put 연산, javax/jakarta.servlet.forward.* 키)
    ↓
Response Committed 확인  (버퍼 상태 플래그 체크)
    ↓
Filter Chain 재진입      (DispatcherType 기반 필터 목록 재구성)
    ↓
Request/Response Wrapper 생성  (Heap 할당, 객체 초기화)
    ↓
Target Servlet 호출      (메서드 디스패치)
    ↓
JSP 처리 시 JspServlet 실행
    │   ├── 최초 호출: JSP 컴파일 (CPU 집약적)
    └── 이후 호출: 컴파일된 Servlet 실행
```

### CPU Pipeline 관점

RequestDispatcher 내부의 단순한 조건 분기(DispatcherType 체크, committed 플래그 확인)는
CPU Branch Predictor가 예측 가능한 패턴이면 파이프라인 스톨(Pipeline Stall)이 최소화된다.
그러나 매핑 테이블 조회에서 캐시 미스가 빈번하면 메모리 접근 지연이 발생한다.

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| JVM Runtime | JFR Method Profiling (CPU 상위 메서드), `-XX:+PrintCompilation` |
| OS Kernel | `perf top` (CPU 핫 함수), `perf record -g` |
| Hardware | `perf stat -e branch-misses` (Branch Misprediction) |
| Hardware | `perf stat -e cache-misses,cache-references` (L1/L2/L3 Cache Miss) |
| Hardware | `perf stat -e instructions,cycles` (IPC: Instruction Per Cycle) |

---

## 17. forward 시 Request Attribute

Servlet 스펙은 `forward` 상황에서 내부 경로 정보를 Request Attribute로 보존한다.

| 속성 (javax.servlet) | 속성 (jakarta.servlet) | 의미 |
|---------------------|----------------------|------|
| `javax.servlet.forward.request_uri` | `jakarta.servlet.forward.request_uri` | 원본 요청 URI |
| `javax.servlet.forward.context_path` | `jakarta.servlet.forward.context_path` | 원본 컨텍스트 경로 |
| `javax.servlet.forward.servlet_path` | `jakarta.servlet.forward.servlet_path` | 원본 Servlet 경로 |
| `javax.servlet.forward.path_info` | `jakarta.servlet.forward.path_info` | 원본 경로 추가 정보 |
| `javax.servlet.forward.query_string` | `jakarta.servlet.forward.query_string` | 원본 쿼리 스트링 |

이 속성들은 Heap의 `HttpServletRequest` 객체 내부 `HashMap`에 저장된다.
대상 자원이 원래 요청 경로를 참조할 때 사용된다.

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| Application | Request Attribute 덤프 (디버그 로그, Filter에서 Attribute 목록 출력) |
| JVM Runtime | Heap Dump 후 `HttpServletRequest` 객체 내부 Attribute 맵 확인 |

---

## 18. include 시 Request Attribute

`include` 상황에서도 포함 대상 경로 정보가 Request Attribute로 제공된다.

| 속성 (javax.servlet) | 속성 (jakarta.servlet) | 의미 |
|---------------------|----------------------|------|
| `javax.servlet.include.request_uri` | `jakarta.servlet.include.request_uri` | 포함 대상 URI |
| `javax.servlet.include.context_path` | `jakarta.servlet.include.context_path` | 포함 대상 컨텍스트 경로 |
| `javax.servlet.include.servlet_path` | `jakarta.servlet.include.servlet_path` | 포함 대상 Servlet 경로 |
| `javax.servlet.include.path_info` | `jakarta.servlet.include.path_info` | 포함 대상 경로 추가 정보 |
| `javax.servlet.include.query_string` | `jakarta.servlet.include.query_string` | 포함 대상 쿼리 스트링 |

`include()` 실행 중에는 두 세트의 Attribute가 동시에 존재할 수 있다.
`javax.servlet.forward.*`와 `javax.servlet.include.*`가 동일 Request 객체에 공존하는 경우,
중첩된 내부 전달 구조임을 의미한다.

---

## 19. Response Commit 제약

`forward()`는 응답이 커밋되기 전에 호출되어야 한다.

```
Response Buffer 작성
    ↓
Buffer Flush (write() System Call 또는 버퍼 자동 플러시)
    ↓
Response Committed (상태 코드 + 헤더 + 일부 Body 전송 확정)
    ↓
forward() 호출
    ↓
IllegalStateException 발생
```

### 응답 커밋의 Kernel 수준 의미

Response가 커밋된다는 것은 Kernel의 TCP Send Buffer(Socket Send Buffer)에
데이터가 기록되어 네트워크로 전송이 시작된 상태다.
이 시점 이후에는 HTTP 상태 코드나 헤더를 수정할 수 없으며,
`forward()`도 호출할 수 없다.

```
Java Response Buffer
    ↓ (flush)
Socket Send Buffer (sk_buff)
    ↓
TCP Segmentation / IP Fragmentation
    ↓
NIC Tx Ring Buffer
    ↓
클라이언트로 전송 (응답 커밋 완료)
```

### Dirty Page Writeback과의 구분

응답 커밋은 네트워크 소켓 전송을 의미하며,
파일 시스템의 Dirty Page Writeback(수정된 Page Cache를 Disk에 반영하는 과정)과는 별개다.
정적 자원 서빙 시 Page Cache에서 읽은 데이터를 Socket으로 전달하는 과정이 포함될 수 있다.

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| Application | `response.isCommitted()` (커밋 여부 확인), 예외 로그 확인 |
| OS Kernel | `ss -nt` (TCP Socket 상태), `netstat -s` (TCP 전송 통계) |
| OS Kernel | `/proc/net/sockstat` (Socket 사용 현황) |

---

## 20. Recursive Forward 위험

잘못된 내부 라우팅은 무한 `forward`를 만들 수 있다.

```
Servlet A
    ↓ forward("/b")
Servlet B
    ↓ forward("/a")
Servlet A
    ↓ forward("/b")
...
```

### 계층별 영향

```
JVM Stack Frame 누적
    ↓
Thread Stack 소진 (-Xss 한계 초과)
    ↓
StackOverflowError (JVM 수준 오류)

또는

Servlet Container 재귀 감지 (Tomcat: MAX_DISPATCH_DEPTH 제한)
    ↓
IllegalStateException / 500 Internal Server Error
```

Tomcat은 내부적으로 디스패치 깊이를 제한하여 무한 재귀를 방지한다.
기본 제한값을 초과하면 Container 차원에서 오류를 반환한다.

### CPU Pipeline Stall과 재귀

깊은 재귀 호출은 CPU의 Return Stack Buffer(RSB)를 초과하여
리턴 주소 예측 실패(Branch Misprediction의 일종)를 유발할 수 있다.

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| Application | 에러 로그 (`StackOverflowError`, `IllegalStateException`) |
| JVM Runtime | Thread Dump (동일 호출 패턴 반복 확인), `-Xss` 증가로 임시 완화 |
| OS Kernel | `dmesg` (JVM 크래시 시 코어 덤프 로그) |
| Hardware | `perf stat -e branch-misses` |

---

## 21. Request Attribute 오염 주의

Request Attribute는 하나의 요청 흐름 안에서 공유된다.

```
Servlet A
    ↓ request.setAttribute("user", value)
    ↓
RequestDispatcher.forward()
    ↓
Servlet B
    ↓ request.getAttribute("user")
    ↓ (또는 request.setAttribute("user", anotherValue) → Attribute 덮어쓰기)
```

### Thread-safety 관점

`HttpServletRequest`는 단일 Thread에서 처리되는 것을 전제로 설계되었다.
`forward()` / `include()`는 동일 Thread에서 실행되므로 Thread-safety 문제는 없다.
그러나 Attribute에 담긴 Mutable 객체를 여러 자원이 수정하면 예측 불가능한 상태가 된다.
특히 Singleton Bean의 필드나 공유 컬렉션 객체를 Attribute로 전달하는 것은 위험하다.

### Attribute 저장의 Heap 영향

`setAttribute()`는 내부적으로 `HashMap.put()`이다.
키-값 쌍 증가 시 HashMap 리사이즈(rehash)가 발생할 수 있으며,
이때 새로운 배열 할당으로 Young Generation 압박이 증가한다.

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| Application | 디버그 로그 (Attribute 키-값 출력), 코드 리뷰 |
| JVM Runtime | Heap Dump (Request 객체 내부 Attribute Map 크기 확인) |
| JVM Runtime | JFR Allocation Profiling (HashMap 리사이즈 빈도) |

---

## 22. DispatcherServlet과 RequestDispatcher의 차이

| 구분 | DispatcherServlet | RequestDispatcher |
|------|-------------------|-------------------|
| 소속 | Spring MVC | Servlet API / Container |
| 역할 | 외부 요청을 Controller로 라우팅 | 서버 내부 자원으로 forward / include |
| 객체 성격 | Singleton Servlet (Spring Bean) | 경로/이름 기반 Dispatcher 객체 |
| 실행 주체 | Worker Thread | 동일 Worker Thread |
| Thread 생성 | 없음 | 없음 |
| task_struct 생성 | 없음 | 없음 |
| 주요 메서드 | `doDispatch()` | `forward()`, `include()` |
| 진입점 | 외부 HTTP 요청 (클라이언트 → NIC → TCP Stack → Servlet Container) | 서버 내부 (동일 요청 흐름 안) |
| Kernel 진입 | Worker Thread의 `task_struct`가 처리 | 동일 `task_struct` 위에서 메서드 호출 |
| Filter 재진입 | DispatcherType.REQUEST 필터 | DispatcherType.FORWARD / INCLUDE 필터 |

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| Application | Thread Dump (`DispatcherServlet.doDispatch` vs `ApplicationDispatcher.forward`) |
| OS Kernel | `strace` (DispatcherServlet 진입 시점의 System Call 패턴 확인) |

---

## 23. RequestDispatcher와 Redirect의 차이

`RequestDispatcher.forward()`는 서버 내부 전달이다.
Redirect는 클라이언트에게 새로운 URL로 다시 요청하라고 응답하는 방식이다.

| 구분 | Forward | Redirect |
|------|---------|----------|
| 처리 위치 | 서버 내부 | 클라이언트 왕복 |
| HTTP 요청 수 | 1회 | 2회 |
| 브라우저 URL | 변경 없음 | 변경됨 |
| Request 객체 | 동일 객체 유지 | 새 요청 객체 생성 |
| Thread | 동일 요청 Thread | 새 요청에서 다시 할당 |
| task_struct | 동일 task_struct | 새 요청에서 새 task_struct 할당 |
| System Call | 동적 자원: 없음 | 응답 write() + 새 요청 수신 처리 |
| Network RTT | 없음 (서버 내부) | 클라이언트 ↔ 서버 왕복 RTT 추가 |
| TCP 연결 | 유지 | 기존 연결 재사용 또는 신규 연결 |
| 사용 예 | 내부 JSP 전달, Spring View 렌더링 | 로그인 후 다른 URL 이동 (PRG 패턴) |

```
Forward:
Client → Server → Internal Resource
(단일 TCP 연결, 단일 HTTP 요청)

Redirect:
Client → Server
Client ← HTTP 302 (Location 헤더)
Client → New URL (새 HTTP 요청, TCP 연결 재사용 또는 신규)
```

### Redirect의 Kernel 수준 흐름

```
302 응답 write()                   ← System Call
    ↓
TCP Send Buffer (sk_buff)
    ↓
클라이언트 수신 후 새 요청 전송
    ↓
NIC IRQ 발생
    ↓
SoftIRQ (TCP/IP Stack 처리)
    ↓
Accept Queue에 새 연결 등록
    ↓
Worker Thread가 새 요청 수락
```

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| OS Kernel | `ss -nt` (TIME_WAIT 소켓: Redirect로 인한 소켓 증가 확인) |
| OS Kernel | `netstat -s` (TCP 연결 통계), `/proc/net/sockstat` |
| Hardware | Network Latency 측정 (RTT 증가 여부) |

---

## 24. SRE 관점의 장애 포인트

| 장애 유형 | 원인 | 계층별 영향 | 결과 |
|-----------|------|------------|------|
| Recursive Forward | A → B → A 반복 | JVM Stack 소진, CPU Branch Misprediction 증가 | StackOverflowError, HTTP 500 |
| Response Committed Error | forward 전 응답 확정 | Socket Send Buffer에 이미 데이터 전송 | IllegalStateException |
| Blocking I/O 전파 | Target Resource 내부 Blocking | task_struct Wait Queue 이동, Thread Pool 점유 | Worker Thread 고갈, 요청 큐 포화 |
| Connection Pool Exhaustion | Blocking Thread 누적으로 Thread Pool 소진 | TCP Backlog / Accept Queue 포화 | Connection Refused, Client Timeout |
| Attribute 오염 | Request Attribute 이름 충돌, Mutable 객체 공유 | Heap의 동일 객체 수정 | 잘못된 요청 데이터 처리 |
| JSP 최초 컴파일 | 최초 접근 시 JSP → Java → .class 컴파일 | CPU 집중, File I/O (write System Call), Metaspace 로드 | 첫 요청 Latency 급증 (Cold Start) |
| ClassLoader Leak | WebApp 재배포 시 이전 ClassLoader 미해제 | Metaspace 누적, GC 대상 제외 | OutOfMemoryError: Metaspace |
| 과도한 내부 라우팅 | 과도한 forward / include 단계 | JVM Stack Frame 누적, Response Wrapper 객체 Young Gen 압박 | Latency 증가, Minor GC 빈도 상승 |
| Off-CPU Time 누적 | Blocking I/O 대기 중 CPU 미사용 | task_struct Wait Queue 체류 시간 증가 | Throughput 저하, P99 Latency 상승 |
| TLB Miss 증가 | 다수 mmap, 과도한 Metaspace 확장 | Page Table Walk 비용 증가 | CPU Cycle 낭비, Latency 증가 |

---

## 25. SRE 관점 주요 지표

| 지표 | 의미 | 관찰 도구 |
|------|------|-----------|
| Servlet Execution Time | forward / include 포함 전체 처리 시간 | APM Span, JFR |
| Active Thread Count | 현재 요청을 처리 중인 Worker Thread 수 | JMX (`ThreadPool.activeCount`), `/metrics` |
| Waiting Thread Count | Blocking I/O 또는 Lock 대기 Thread 수 | Thread Dump, JFR Thread State |
| Off-CPU Time | Thread가 CPU를 사용하지 않는 시간 | `offcputime` (BCC), async-profiler |
| Response Committed Error Count | 잘못된 forward 호출 시점 | 애플리케이션 에러 로그 |
| StackOverflowError Count | Recursive Forward 가능성 | JVM 에러 로그, APM 예외 집계 |
| P99 / P999 Latency | 내부 라우팅 지연 포함 사용자 체감 지연 | Prometheus + Grafana, APM |
| System CPU (`%sys`) | 파일 I/O, Socket I/O 증가 여부 | `mpstat`, `sar` |
| Minor GC Frequency | Response Wrapper 등 단명 객체 할당 압박 | JFR, `jstat -gcnew` |
| Metaspace Usage | JSP 컴파일, ClassLoader Leak 누적 | JMX (`MemoryMXBean`), `jcmd VM.native_memory` |
| JVM Allocation Rate | Request Wrapper, Attribute 객체 생성량 | JFR Allocation Profiling |
| Context Switch Count | Blocking I/O 발생 시 `task_struct` 전환 빈도 | `vmstat cs`, `pidstat -w` |
| TCP Backlog Drop | Thread Pool 고갈 시 신규 연결 Drop | `netstat -s` (`SYNs to LISTEN sockets dropped`) |
| Page Fault (Major) | 정적 자원 Page Cache Miss (Disk I/O 발생) | `vmstat pgmajfault`, `/proc/vmstat` |
| TLB Miss Rate | Metaspace / mmap 확장으로 TLB 효율 저하 | `perf stat -e dTLB-load-misses` |
| Thread Dump | ApplicationDispatcher.invoke 호출 위치 확인 | `jstack`, JFR, APM Thread Dump |

---

## 26. Thread Dump에서의 관찰

Tomcat 환경에서는 내부 `forward` / `include` 과정에서 다음과 같은 호출 스택이 보인다.

```
javax.servlet 환경:
    org.apache.catalina.core.ApplicationDispatcher.invoke
    org.apache.catalina.core.ApplicationDispatcher.doForward
    org.apache.catalina.core.ApplicationDispatcher.forward
    javax.servlet.RequestDispatcher.forward

jakarta.servlet 환경:
    org.apache.catalina.core.ApplicationDispatcher.invoke
    org.apache.catalina.core.ApplicationDispatcher.doForward
    org.apache.catalina.core.ApplicationDispatcher.forward
    jakarta.servlet.RequestDispatcher.forward
```

Thread Dump에서 위 호출이 보이면 현재 요청이 Servlet Container 내부 전달 과정에 있음을 의미한다.

### Thread Dump 분석 시 확인 항목

| 확인 항목 | 의미 |
|-----------|------|
| Thread 상태 `RUNNABLE` + `ApplicationDispatcher.invoke` | 정상 실행 중 (CPU 사용) |
| Thread 상태 `WAITING` + JDBC / HTTP Client | Blocking I/O 대기 중 (Off-CPU) |
| Thread 상태 `BLOCKED` + `synchronized` | Lock 경합 (Contention) |
| 동일 Stack 패턴 다수 Thread | 동일 내부 자원에 대한 병렬 요청 집중 |
| `ApplicationDispatcher.doForward` 깊이 반복 | Recursive Forward 가능성 |

### JVM Safepoint와 Thread Dump

`jstack`으로 Thread Dump를 수집하는 행위 자체가 JVM Safepoint를 트리거한다.
모든 Thread가 Safepoint에 도달할 때까지 잠시 정지되므로,
운영 중 빈번한 Thread Dump 수집은 지연을 유발할 수 있다.
JFR의 Thread State 이벤트는 Safepoint 없이 연속적으로 Thread 상태를 기록한다.

| 계층 | 관찰 도구 / 키워드 |
|------|-------------------|
| Application | `jstack <PID>` (Thread Dump), APM Thread Dump 자동 수집 |
| JVM Runtime | JFR Thread State, `-XX:+PrintSafepointStatistics` (Safepoint 지연 확인) |
| OS Kernel | `kill -3 <PID>` (SIGQUIT으로 Thread Dump 트리거) |

---

## 27. 전체 흐름 요약

```
Client Request
    ↓
NIC 수신 → IRQ 발생 → SoftIRQ (TCP/IP Stack)
    ↓
Tomcat Acceptor Thread (accept() System Call)
    ↓
Worker Thread 할당 (Thread Pool)
    ↓
Linux task_struct → TASK_RUNNING
    ↓
Servlet A / DispatcherServlet
    ↓
RequestDispatcher 획득 (Path Resolution, Mapping Lookup)
    ↓
forward() 또는 include()
    │
    ├── Filter Chain 재진입 (DispatcherType 기반)
    ├── Request Attribute 설정 (Heap: HashMap.put)
    ├── Response Buffer 상태 확인
    └── Target Resource 실행
    ↓
동일 Worker Thread 유지
    ↓
동일 task_struct 유지
    ↓
Response 작성
    ↓
Socket Send Buffer (sk_buff) 기록 (write() System Call)
    ↓
NIC Tx Ring Buffer → 클라이언트 전송
```

### 경우별 Kernel 개입 수준

| 시나리오 | Kernel 개입 | System Call |
|----------|-------------|-------------|
| 동적 Servlet으로 forward | 없음 (User Mode 유지) | 없음 |
| 정적 파일로 forward | Page Cache + Socket 쓰기 | `read()` / `sendfile()`, `write()` |
| JSP 최초 호출 | JSP 컴파일 (파일 I/O) | `open()`, `write()`, `read()` |
| JDBC Blocking | I/O 대기 | `recv()` 또는 `epoll_wait()` |
| Blocking I/O 완료 | IRQ → SoftIRQ → Wake-up | (Kernel 내부 처리) |

---

## 28. 핵심 정리

RequestDispatcher는 Servlet Container 내부에서 요청을 다른 자원으로 전달하거나
다른 자원의 응답 내용을 포함시키는 Servlet API 인터페이스다.

```
RequestDispatcher
    ↓
새 Thread 생성 없음       → Thread Pool 추가 점유 없음
    ↓
새 task_struct 생성 없음  → Kernel 스케줄링 단위 추가 없음
    ↓
동일 Worker Thread에서 메서드 호출 흐름 유지
    ↓
동일 JVM Stack 위에서 Frame 누적
    ↓
동일 Heap의 Request / Response 객체 참조
```

`forward()`는 제어권을 대상 자원으로 넘기는 내부 전달이고,
`include()`는 대상 자원의 결과를 현재 응답에 포함한 뒤 원래 흐름으로 복귀하는 방식이다.

동적 자원으로 전달될 때는 대부분 User Mode의 JVM 메서드 호출로 처리되지만,
정적 자원, 파일 I/O, JSP 최초 컴파일, 외부 I/O가 포함되면
System Call과 Kernel Mode 전환이 발생한다.

### SRE 관점 핵심 결론

RequestDispatcher 자체의 비용은 경로 해석, 매핑 조회, Request Attribute 처리,
Response Wrapper 생성, Filter Chain 재진입, Target Resource 실행 비용으로 구성된다.

장애는 RequestDispatcher 자체보다 다음 원인에서 발생한다.

| 원인 | 계층 | 핵심 지표 |
|------|------|-----------|
| Target Resource 내부 Blocking I/O | OS Kernel (Wait Queue), Thread Pool | Waiting Thread Count, Off-CPU Time |
| Recursive Forward | JVM Stack, CPU Branch Predictor | StackOverflowError Count, P99 Latency |
| Response Commit 이후 forward | Socket Send Buffer | IllegalStateException Count |
| JSP Cold Start (최초 컴파일) | CPU, File I/O, Metaspace | P99 Latency (첫 요청), System CPU |
| ClassLoader Leak | JVM Metaspace | Metaspace Usage, OOME |
| Connection Pool Exhaustion | TCP Backlog, Accept Queue | TCP SYN Drop, Active Thread Count |
| Off-CPU Time 누적 | task_struct Wait Queue 체류 | Off-CPU Flame Graph, Throughput 저하 |

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*