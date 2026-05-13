# WAS(Tomcat), JVM, OS Kernel, 실행 파일 바이너리의 실행 구조

## 개요

Java 기반 웹 애플리케이션은 다음 계층을 따라 실행됩니다.

```
Spring Application
  → WAS / Servlet Container
  → JVM
  → OS Kernel
  → Hardware
```

Spring Boot 환경에서 WAS는 일반적으로 내장 Tomcat을 의미합니다.  
기술적으로 WAS는 상위 개념이고, Tomcat은 WAS를 구현한 구체적인 제품입니다.

---

## 1. WAS와 Tomcat의 관계

### WAS의 역할

WAS(Web Application Server)는 웹 애플리케이션을 실행하기 위한 서버 소프트웨어 계층입니다.

- HTTP 요청 수신
- Servlet Container 관리
- Thread Pool 관리
- Request Dispatch
- Session 관리
- Filter / Listener / Servlet 실행
- 애플리케이션과 OS 사이의 실행 환경 제공

### Tomcat의 역할

Tomcat은 Java Servlet 규격을 구현한 대표적인 오픈소스 WAS입니다.  
Spring Boot에서 `spring-boot-starter-web`을 사용하면 기본적으로 내장 Tomcat이 함께 실행됩니다.

```
Spring Boot Application
  → Embedded Tomcat
  → Servlet Container
  → DispatcherServlet
```

---

## 2. WAS와 OS Kernel의 관계

WAS는 직접 CPU, 메모리, 네트워크, 디스크를 제어하지 않습니다.  
WAS는 JVM 위에서 실행되는 Java 소프트웨어이며, 실제 하드웨어 자원 사용은 OS Kernel을 통해 수행됩니다.

```
WAS / Tomcat
  → JVM
  → System Call
  → Linux Kernel
  → CPU / RAM / NIC / Disk
```

---

## 3. WAS와 Kernel의 주요 상호작용

### 3.1 네트워크 연결

```
Client
  → NIC
  → Linux Kernel Network Stack
  → Socket Buffer
  → Accept Queue
  → Tomcat Acceptor Thread
```

**관련 System Call**: `socket()`, `bind()`, `listen()`, `accept()`, `read()`, `write()`, `epoll_wait()`

### 3.2 Thread 생성과 task_struct

```
Java Thread.start()
  → JVM Native Layer
  → pthread_create()
  → clone()
  → Linux task_struct 생성
```

Linux Kernel은 생성된 `task_struct`를 CFS Scheduler를 통해 Logical CPU에 배치합니다.

### 3.3 Blocking I/O와 Wait Queue

Tomcat Worker Thread가 JDBC, 외부 API, 파일 I/O 같은 Blocking 호출을 수행하면 해당 Thread는 응답을 기다립니다.

```
Worker Thread
  → Blocking I/O
  → System Call
  → task_struct → Wait Queue 이동
  → I/O 완료 후 Runnable Queue 복귀
```

이때 Java Thread는 Thread Pool에 반환되지 않습니다.  
CPU를 사용하지 않더라도 **Java Thread, JVM Stack, Kernel Stack, task_struct는 유지**됩니다.

### 3.4 메모리 관리

**관련 System Call**: `mmap()`, `brk()`, `munmap()`

| 영역 | 설명 |
|------|------|
| Heap | Java 객체 저장 |
| Metaspace | 클래스 메타데이터 저장 |
| Thread Stack | Thread별 메서드 호출 정보 저장 |
| Native Memory | JVM 외부 메모리, Direct Buffer, Metaspace 등 |
| Kernel Memory | task_struct, Kernel Stack 등 |

---

## 4. WAS는 Java로 구현됩니다

Tomcat, Jetty, Undertow 같은 WAS는 대부분 Java로 구현되어 있습니다.  
Tomcat이 실행되기 위해서는 JVM이 필요하며, JVM은 주로 C/C++로 구현되어 있습니다.

---

## 5. 계층별 구현 언어

| 계층 | 구성 요소 | 주요 구현 언어 |
|------|----------|-------------|
| Application | Spring Controller, Service | Java |
| WAS | Tomcat, Servlet Container | Java |
| Runtime | JVM, JIT, GC | C/C++ |
| OS Kernel | Linux Kernel | C |
| Hardware | CPU, RAM, NIC, Disk | 전자 회로 |

---

## 6. Java 코드가 하드웨어에서 실행되는 흐름

Java 코드는 CPU가 직접 실행할 수 없습니다. 다음 과정을 거쳐 하드웨어 명령으로 변환됩니다.

```
.java source code
  → javac
  → .class bytecode
  → JVM ClassLoader
  → Interpreter / JIT Compiler
  → Native Machine Code
  → CPU 실행
```

---

## 7. JVM의 역할

JVM은 Java Bytecode를 실행하기 위한 Runtime입니다.

- Class Loading
- Bytecode Verification
- Interpreter 실행
- JIT Compilation
- Heap 관리 / GC 수행
- Thread 관리
- Native Memory 관리
- System Call 연결

---

## 8. JIT Compiler와 Code Cache

JVM은 반복적으로 실행되는 Bytecode를 Native Machine Code로 변환합니다.

```
Java Bytecode
  → JIT Compiler
  → Native Machine Code
  → Code Cache 저장
  → CPU 직접 실행
```

JIT이 생성한 기계어는 JVM의 Code Cache 영역에 저장되며, CPU가 직접 실행 가능한 메모리 영역입니다.

---

## 9. java -jar 실행 시 전체 흐름

```
Shell / systemd
  → fork()
  → execve("/usr/bin/java")
  → Linux Kernel이 JVM 바이너리 로드
  → JVM 프로세스 시작
  → Heap / Metaspace 확보
  → ClassLoader가 app.jar 로딩
  → Spring Boot 초기화
  → Tomcat 초기화
  → DispatcherServlet 생성
  → Worker Thread Pool 준비
  → HTTP Port bind/listen
  → Request 대기
```

---

## 10. execve()의 역할

`execve()`는 현재 프로세스의 실행 이미지를 새로운 실행 파일로 교체하는 System Call입니다.

```
execve()
  → ELF Binary 분석
  → .text / .data / .rodata 섹션 매핑
  → 동적 라이브러리 로딩
  → Entry Point 설정
  → CPU가 JVM 실행 시작
```

---

## 11. 실행 파일 바이너리

실행 파일 바이너리는 CPU가 실행할 수 있는 기계어 명령어와 실행에 필요한 데이터를 담은 파일입니다.  
Linux에서는 **ELF(Executable and Linkable Format)** 형식을 사용합니다.

```
/usr/bin/java
  └── ELF Executable Binary (JVM 자체)
```

Java 애플리케이션을 실행하기 전에, 먼저 JVM이라는 Native 실행 파일이 실행됩니다.

---

## 12. ELF 바이너리의 주요 섹션

| 섹션 | 역할 |
|------|------|
| `.text` | CPU가 실행할 기계어 명령어 |
| `.data` | 초기화된 전역 변수 |
| `.bss` | 초기화되지 않은 전역 변수 |
| `.rodata` | 읽기 전용 상수 데이터 |
| Header | Entry Point, 섹션 정보, 로딩 정보 |

`.text`는 파일 확장자가 아닌 **ELF 바이너리 내부의 섹션 이름**입니다.

---

## 13. .text 섹션의 권한

Kernel은 `.text` 섹션을 메모리에 올릴 때 다음 권한을 부여합니다.

| 권한 | 허용 여부 |
|------|----------|
| Read | 가능 |
| Execute | 가능 |
| Write | 불가 |

실행 중인 코드가 임의로 변경되는 것을 방지하기 위해 Write 권한은 부여하지 않습니다.

---

## 14. .data와 .text의 차이

| 구분 | `.text` | `.data` |
|------|---------|---------|
| 내용 | 기계어 코드 | 초기화된 전역 변수 |
| 권한 | Read + Execute | Read + Write |
| 변경 가능 여부 | 일반적으로 불가 | 가능 |
| CPU 관점 | 실행할 명령어 | 명령어가 참조할 데이터 |

---

## 15. JVM 바이너리와 Java Bytecode의 차이

### JVM 바이너리 (`/usr/bin/java`)
- C/C++로 작성된 JVM을 컴파일한 결과물
- Linux Kernel이 `execve()`로 직접 실행
- CPU가 직접 실행할 수 있는 Native Machine Code
- ELF 바이너리 형식

### Java Bytecode (`.class` 파일)
- Java Compiler가 생성한 JVM용 명령어
- CPU가 직접 실행 불가
- JVM이 해석하거나 JIT Compiler로 Native Code로 변환

---

## 16. JVM 실행과 Java 코드 실행의 이중 구조

```
1단계: JVM 실행
  Linux Kernel
    → /usr/bin/java ELF Binary 실행
    → CPU가 JVM의 .text 섹션 실행

2단계: Java Application 실행
  JVM
    → .class Bytecode 로딩
    → Interpreter 또는 JIT Compiler
    → Native Code 실행
```

`java -jar`는 먼저 JVM이라는 Native 프로그램을 실행하고, 그 JVM이 다시 Java 애플리케이션을 실행하는 **이중 구조**입니다.

---

## 17. DispatcherServlet과 실행 구조의 연결

```
DispatcherServlet.class
  → ClassLoader
  → Metaspace에 클래스 메타데이터 적재
  → Spring Container가 DispatcherServlet 객체 생성
  → Heap에 Singleton 객체 저장
```

요청이 들어오면 Worker Thread가 이 Singleton 객체의 메서드를 실행합니다.

```
HTTP Request
  → Tomcat Worker Thread
  → task_struct TASK_RUNNING
  → DispatcherServlet.doDispatch()
  → Controller 호출
```

---

## 18. 요청 처리 중 실제 CPU 실행 주체

CPU를 사용하는 대상은 `DispatcherServlet` 객체가 아니라 **Worker Thread**입니다.

```
DispatcherServlet  =  Heap 객체 (실행 대상)
Worker Thread      =  Java 실행 흐름 (실행 주체)
task_struct        =  Linux Kernel 실행 단위
Logical CPU        =  실제 실행 위치
```

---

## 19. System Call 관점에서 WAS 실행

| WAS 동작 | 관련 System Call |
|----------|----------------|
| 서버 포트 열기 | `socket()`, `bind()`, `listen()` |
| 연결 수락 | `accept()` |
| 네트워크 읽기/쓰기 | `read()`, `write()`, `send()`, `recv()` |
| 이벤트 대기 | `epoll_wait()` |
| Thread 생성 | `clone()` |
| 메모리 할당 | `mmap()`, `brk()` |
| 파일 읽기 | `open()`, `read()`, `stat()` |
| Lock 대기 | `futex()` |

---

## 20. SRE 관점의 병목 지점

| 병목 | 설명 |
|------|------|
| Thread Pool 고갈 | Worker Thread가 Blocking I/O로 반환되지 않음 |
| CPU Saturation | Runnable `task_struct`가 과도하게 증가 |
| Context Switch 증가 | Thread 수가 많아져 CPU 실행 대상이 자주 교체됨 |
| System CPU 증가 | System Call, Kernel Scheduling, Network 처리 비용 증가 |
| Native Memory 증가 | Thread Stack, Direct Buffer, Metaspace 증가 |
| GC 증가 | 요청별 객체 생성량 증가 |
| Load Average 증가 | Runnable 또는 Uninterruptible task 증가 |
| Accept Queue 포화 | WAS가 연결을 충분히 빠르게 accept하지 못함 |

---

## 21. 전체 End-to-End 흐름

```
java -jar app.jar
  → Linux Kernel execve()
  → /usr/bin/java ELF Binary 로드
  → JVM .text 섹션 실행
  → JVM Heap / Metaspace 확보
  → Spring Boot Class Loading
  → Singleton Bean 생성
  → DispatcherServlet Heap에 생성
  → Tomcat Connector 초기화
  → socket() / bind() / listen()
  → Worker Thread Pool 생성 (clone() → task_struct 생성)
  → HTTP Request 도착
  → Kernel Network Stack
  → Tomcat Worker Thread 활성화
  → task_struct → Logical CPU 배치
  → DispatcherServlet.doDispatch()
  → Controller / Service 실행
  → 필요 시 Blocking I/O 또는 DB 접근
  → Response 반환
```

---

## 22. 핵심 정리

```
Spring Controller / DispatcherServlet  =  Heap의 Singleton 객체 (요청마다 재생성되지 않음)
Tomcat Worker Thread                   =  요청을 실제 처리하는 Java 실행 흐름
JVM Native Layer                       =  Java → OS 연결 계층
Linux task_struct                      =  Worker Thread의 Kernel 실행 단위
CFS Scheduler                          =  task_struct를 Logical CPU에 배치
Logical CPU → Physical Core            =  실제 명령어 실행 위치
```

**실행 계층 요약**

```
Spring Controller / DispatcherServlet
  → Tomcat Worker Thread
  → JVM Native Layer
  → Linux task_struct
  → CFS Scheduler
  → Logical CPU
  → Physical Core
```

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*