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
Application Layer  →  DispatcherServlet
JVM Layer          →  Heap / Metaspace
OS Kernel Layer    →  task_struct / CFS Scheduler
Hardware Layer     →  Logical CPU / Physical Core
```

Kernel이 스케줄링하는 대상은 `DispatcherServlet`이 아니라, 해당 코드를 실행하는 **Worker Thread의 `task_struct`**입니다.

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

---

## 3. DispatcherServlet의 Singleton 구조

`DispatcherServlet` 인스턴스는 하나이며, 여러 Worker Thread가 동시에 해당 객체의 메서드를 실행할 수 있습니다.

```
Heap
  └── DispatcherServlet 객체 1개

Worker Thread A  →  DispatcherServlet.doDispatch() 실행
Worker Thread B  →  DispatcherServlet.doDispatch() 실행
Worker Thread C  →  DispatcherServlet.doDispatch() 실행
```

객체는 하나이지만 실행 주체는 여러 Worker Thread입니다.

---

## 4. Spring에서 Singleton으로 관리되는 객체

Spring에서 기본적으로 Singleton Bean으로 관리되는 객체는 다음과 같습니다.

- `@Controller` / `@RestController`
- `@Service`
- `@Repository`
- `@Component`
- `DispatcherServlet`

---

## 5. Singleton을 사용하는 이유

요청마다 객체를 생성하면 다음 문제가 발생합니다.

```
요청 증가
  → 객체 생성 증가
  → Heap 사용량 증가
  → GC 대상 증가
  → GC Pause 증가
  → Latency 증가
```

Singleton은 객체 생성 비용과 GC 부담을 줄입니다.

```
Singleton 객체 1개 생성
  → 여러 Thread가 공유
  → 객체 생성/소멸 비용 감소
  → Heap 안정성 증가
```

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

---

## 7. JVM Memory 구조

| 영역 | 역할 |
|------|------|
| Metaspace | 클래스 메타데이터, 메서드 정보, static 변수 정보 |
| Heap | 실제 객체 인스턴스 저장 |
| Stack | Thread별 메서드 호출 정보, 지역 변수 저장 |
| Native Memory | JVM 외부 메모리, Metaspace, Direct Memory 등 |

---

## 8. DispatcherServlet의 메모리 위치

```
Metaspace    →  DispatcherServlet.class 메타데이터
Heap         →  DispatcherServlet 객체 인스턴스
Thread Stack →  doDispatch() 호출 Stack Frame
```

- 클래스 설계도(메타데이터)는 Metaspace에 위치합니다.
- 실제 Singleton 객체 인스턴스는 Heap에 위치합니다.
- 요청 처리 중인 메서드 호출 정보는 각 Worker Thread의 Stack에 위치합니다.

---

## 9. Static Reference와 Singleton 접근

직접 구현한 Singleton에서는 static 참조가 Heap 객체를 가리킵니다.

```
Metaspace / Class Metadata
  └── static reference
          ↓
Heap
  └── Singleton Instance
```

Spring에서는 Spring Container가 Singleton Bean의 객체 참조를 관리합니다.

```
Spring ApplicationContext
  └── Bean Registry
          ↓
Heap
  └── Singleton Bean Instance
```

---

## 10. Worker Thread와 DispatcherServlet 실행 관계

```
HTTP Request
  → Worker Thread 할당
  → task_struct TASK_RUNNING
  → DispatcherServlet.doService()
  → DispatcherServlet.doDispatch()
  → HandlerMapping 조회
  → HandlerAdapter 실행
  → Controller 호출
```

`DispatcherServlet`은 실행 주체가 아닌 **실행 대상 객체**입니다.  
실행 주체는 Worker Thread이며, Kernel 수준에서는 해당 Worker Thread의 `task_struct`입니다.

---

## 11. DispatcherServlet 실행 중 Kernel 관점

Kernel은 Java 객체를 직접 인식하지 않습니다. Kernel 관점에서는 다음과 같이 동작합니다.

```
task_struct
  → Logical CPU 점유
  → User Mode에서 JVM Code 실행
  → CPU Instruction 실행
```

`HandlerMapping`, JSON Binding, Validation, Interceptor 실행 등은 모두 Worker Thread의 CPU 시간을 소비합니다.

---

## 12. Dispatcher 단계에서 CPU를 사용하는 작업

다음 작업들이 CPU를 소비합니다.

- URL Path Matching
- HandlerMapping 조회
- HandlerAdapter 선택
- Argument Resolver 실행
- JSON Deserialization
- Validation
- Interceptor 실행
- Controller Method Invocation
- Response Serialization

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
Worker Thread
  → DispatcherServlet.doDispatch()
  → Controller
  → Service
  → Blocking I/O
  → System Call
  → task_struct → Wait Queue 이동
```

이때 `DispatcherServlet` 객체가 Blocking되는 것이 아니라, `DispatcherServlet`을 실행하던 **Worker Thread / task_struct가 Blocking**됩니다.

---

## 14. Blocking 발생 시 자원 상태

| 계층 | 상태 |
|------|------|
| Java Thread | 메서드 반환 전이므로 Thread Pool에 반환되지 않음 |
| JVM Stack | Controller / Service 호출 Stack Frame 유지 |
| Heap | Singleton 객체 유지 |
| Kernel | task_struct가 Wait Queue로 이동 |
| CPU | 해당 task_struct는 CPU를 사용하지 않음 |
| Thread Pool | 해당 Thread는 점유 상태 유지 |

---

## 15. 초기화 단계에서 발생하는 Kernel 동작

### 15.1 메모리 확보

```
JVM Startup
  → mmap() / brk()
  → Virtual Memory Area 생성
  → Page Table 설정
  → Page Fault로 물리 메모리 할당
```

**관련 System Call**: `mmap()`, `brk()`

### 15.2 클래스 및 설정 파일 로딩

```
ClassLoader
  → JAR / class 파일 접근
  → open() / read()
  → Page Cache 조회
  → Class Metadata 생성
  → Metaspace 적재
```

**관련 System Call**: `open()`, `read()`, `stat()`, `fstat()`

### 15.3 Worker Thread Pool 생성

```
Java Thread.start()
  → JVM Native Layer
  → pthread_create()
  → clone()
  → task_struct 생성
  → CFS Scheduler 등록
```

**관련 System Call**: `pthread_create()`, `clone()`

### 15.4 동기화 처리

```
Thread 간 Lock 경합
  → futex()
  → Kernel 도움을 받아 대기/깨우기
```

**관련 System Call**: `futex()`

### 15.5 네트워크 포트 준비

```
Server Startup
  → socket()
  → bind(8080)
  → listen()
  → epoll 등록
  → Inbound Request 대기
```

**관련 System Call**: `socket()`, `bind()`, `listen()`, `accept()`, `epoll_create()`, `epoll_ctl()`, `epoll_wait()`

---

## 16. Startup 단계 전체 흐름

```
java -jar app.jar
  → JVM Process 생성
  → Heap / Metaspace 메모리 확보 (mmap, brk)
  → ClassLoader가 클래스 로딩 (open, read)
  → Spring ApplicationContext 생성
  → Singleton Bean 생성
  → DispatcherServlet 생성
  → Worker Thread Pool 준비 (clone)
  → Socket bind / listen
  → Request 대기 (epoll_wait)
```

---

## 17. Request 처리 단계 전체 흐름

```
Client Request
  → NIC Packet 수신
  → Kernel Network Stack 처리
  → Socket Buffer 적재
  → epoll / accept
  → Worker Thread 활성화
  → task_struct TASK_RUNNING
  → CFS가 Logical CPU에 배치
  → DispatcherServlet.doDispatch()
  → Controller
  → Service
  → Business Logic
  → Response
```

---

## 18. DispatcherServlet과 task_struct의 관계

```
DispatcherServlet  =  Heap에 존재하는 Singleton 객체
Worker Thread      =  DispatcherServlet을 실행하는 Java Thread
task_struct        =  Worker Thread가 Kernel에서 실체화된 실행 단위
CFS Scheduler      =  task_struct를 Logical CPU에 배치하는 Kernel Scheduler
```

```
Heap
  └── DispatcherServlet  ← 실행 대상
          ↑ 실행
Java Worker Thread
          ↓
Linux task_struct
          ↓
CFS Scheduler
          ↓
Logical CPU
```

---

## 19. 성능 병목 관점

| 병목 유형 | 원인 |
|----------|------|
| CPU 증가 | HandlerMapping, JSON Parsing, Validation, Serialization |
| Thread Pool 고갈 | Controller / Service 내부 Blocking I/O |
| System CPU 증가 | File I/O, Socket I/O, System Call 증가 |
| GC 증가 | 요청별 객체 생성 과다 |
| Lock 경합 | Singleton Bean 내부 공유 상태 또는 동기화 과다 |
| Latency 증가 | Dispatcher 전후 Filter / Interceptor 과다 |

---

## 20. SRE 관점 주요 지표

| 지표 | 의미 |
|------|------|
| JVM Thread Count | Worker Thread 증가 여부 |
| Active Thread Count | Thread Pool 점유 상태 |
| Runnable Thread 수 | CPU 경쟁 상태 |
| Waiting Thread 수 | I/O 또는 Lock 대기 |
| CPU `us` | Java 로직 실행 비중 |
| CPU `sy` | Kernel / System Call 비중 |
| GC Pause | 요청 객체 생성 및 Heap 압박 |
| Allocation Rate | 요청당 객체 생성량 |
| Context Switch | task_struct 전환 비용 |
| Load Average | 실행 대기 및 D 상태(Uninterruptible Sleep) task 누적 |
| P99 / P999 Latency | 사용자 관점 지연 |

---

## 21. 핵심 정리

```
DispatcherServlet  →  Heap에 존재하는 Singleton 라우팅 객체
Worker Thread      →  DispatcherServlet 메서드를 실행하는 Java 실행 흐름
task_struct        →  Worker Thread를 Kernel이 Logical CPU에 스케줄링하는 실행 단위
```

**Blocking I/O 발생 시**: `DispatcherServlet` 객체가 멈추는 것이 아니라, `DispatcherServlet`을 실행하던 Worker Thread / `task_struct`가 Wait Queue로 이동하고 Thread Pool에 반환되지 않습니다.

**Startup 단계**: JVM은 `mmap()`, `open()`, `read()`, `clone()`, `futex()`, `socket()`, `bind()`, `listen()` 등 다양한 System Call을 통해 메모리, 클래스, Thread, 네트워크 수신 준비를 수행합니다.

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*