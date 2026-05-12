# CPU Saturation과 I/O 처리 모델

## 개요

**CPU Saturation**은 단순히 CPU 사용률이 높은 상태가 아닙니다.  
CPU가 실제로 처리할 수 있는 용량보다 많은 `task_struct`가 실행을 요구하면서, **대기·전환·스케줄링 비용이 증가하는 상태**를 의미합니다.

이 문제는 I/O 처리 모델과 직접 연결됩니다. 핵심 질문은 다음과 같습니다.

> **I/O 대기 중에 Java Thread와 `task_struct`가 Wait Queue로 이동하는가,  
> 아니면 Running/Runnable 상태를 유지하면서 다른 작업을 계속 처리할 수 있는가?**

---

## 1. Linux Kernel 관점: task_struct 상태

Linux Kernel에서 Java Thread는 최종적으로 `task_struct`로 실체화됩니다.  
I/O 처리 방식에 따라 `task_struct`가 다음 중 어떤 상태에 머무는지가 결정됩니다.

| 상태 | 의미 |
|------|------|
| Running | Logical CPU 위에서 실제 실행 중 |
| Runnable | 실행 가능하지만 CPU 할당 대기 중 |
| Sleeping / Blocked | I/O, Lock, Timer 등 이벤트 대기 중 |
| Wait Queue | Sleeping 상태 `task_struct`가 대기하는 커널 구조 |
| Runnable Queue | Runnable 상태 `task_struct`가 CPU 할당을 기다리는 구조 |

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
  → System Call (User Mode → Kernel Mode)
  → 데이터 미준비 또는 응답 대기 필요
  → task_struct → Wait Queue 이동
  → I/O 완료 후 Runnable Queue 복귀
```

### Thread Pool에 미치는 영향

Blocking I/O 중인 Thread는 CPU를 사용하지 않더라도, **Java Thread Pool에 반환되지 않습니다.**

```
Blocking I/O 진행 중
  → Kernel: task_struct는 Wait Queue에 있음
  → Application: Java Thread는 요청 처리 중 상태 유지
  → Thread Pool 반환 불가
```

Blocking I/O가 누적되면 Thread Pool이 고갈되고, 신규 요청을 처리할 Thread가 없어집니다.

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
  → FD 할당
  → epoll_ctl()로 관심 이벤트 등록
  → epoll_wait()로 준비된 이벤트 확인
```

epoll은 데이터가 준비된 FD만 Ready List에 등록하여, Event Loop가 불필요한 대기 없이 처리할 수 있도록 합니다.

### Kernel 흐름

```
Event Loop Thread
  → FD를 epoll 관심 목록에 등록
  → Thread는 다른 이벤트 처리 계속
  → Network Packet 도착
  → NIC Interrupt 발생
  → Kernel이 Socket Buffer 갱신
  → epoll Ready List 갱신
  → Event Loop가 이벤트 수신
  → Handler 실행
```

요청마다 Thread가 대기하지 않고, **소수의 Event Loop Thread가 다수의 연결을 처리**합니다.

### epoll의 한계

epoll은 네트워크 소켓 I/O에는 적합하지만, **일반 파일/디스크 I/O에는 제한**이 있습니다.

Linux에서 일반 파일 FD는 항상 Ready로 간주되는 경향이 있어, 실제 디스크 접근 시 Blocking이 발생할 수 있습니다.

```
epoll이 파일 FD를 Ready로 판단
  → read() 호출
  → Page Cache Miss
  → Disk I/O 발생
  → Thread Block 가능
```

---

## 4. io_uring

### 개념

io_uring은 Linux Kernel의 **고성능 비동기 I/O 인터페이스**입니다.  
기존 epoll/read/write 방식보다 시스템 콜 횟수와 User Mode / Kernel Mode 전환 비용을 줄이기 위해 설계되었습니다.

io_uring은 **공유 Ring Buffer** 구조를 사용합니다.

| Queue | 역할 |
|-------|------|
| Submission Queue (SQ) | 애플리케이션이 Kernel에 요청할 I/O 작업을 등록 |
| Completion Queue (CQ) | Kernel이 완료된 I/O 결과를 기록 |

### 기본 흐름

```
Application Thread
  → SQ에 I/O 요청 등록
  → Kernel이 요청 처리
  → CQ에 완료 결과 기록
  → Application이 CQ에서 결과 확인
```

### SQPOLL 모드

SQPOLL 모드에서는 Kernel Polling Thread가 SQ를 지속적으로 감시하여, 작업마다 시스템 콜을 명시적으로 호출하는 빈도를 줄일 수 있습니다.

```
Application Thread
  → SQ에 작업 기록
  → Kernel Polling Thread가 SQ 감시
  → I/O 작업 수행
  → CQ에 결과 기록
```

### io_uring의 장점

| I/O 종류 | epoll | io_uring |
|----------|-------|---------|
| Network Socket I/O | 적합 | 적합 |
| File / Disk I/O | 제한적 | 적합 |
| 시스템 콜 비용 감소 | 제한적 | 강함 |
| 공유 Queue 기반 처리 | 아니요 | 예 |

---

## 5. Blocking I/O / epoll / io_uring 비교

| 구분 | Blocking I/O | epoll | io_uring |
|------|-------------|-------|---------|
| 대표 구현 | `java.io`, JDBC | Java NIO, Netty epoll | Netty io_uring transport |
| Kernel 대화 방식 | read/write 직접 호출 | FD 이벤트 감시 | SQ/CQ 기반 비동기 요청 |
| I/O 대기 중 Thread 상태 | Wait Queue로 이동 | Event Loop 중심 처리 | Running/Runnable 유지에 유리 |
| 네트워크 I/O | 가능하지만 Blocking | 적합 | 적합 |
| 파일/디스크 I/O | Blocking | 제한적 | 적합 |
| 시스템 콜 비용 | 높음 | 중간 | 낮음 |
| 필요한 Thread 수 | 증가하기 쉬움 | 적게 유지 가능 | 적게 유지 가능 |

---

## 6. JDBC와 R2DBC

### JDBC

JDBC는 **Blocking 방식**으로 동작합니다.

```
Java Thread
  → JDBC Query 실행
  → DB 응답 대기
  → Thread Blocked (Thread Pool 점유 유지)
```

WebFlux나 Netty Event Loop 내부에서 JDBC를 직접 호출하면, **Event Loop Thread 자체가 Blocking**되어 전체 처리량이 크게 저하됩니다.

### R2DBC

R2DBC는 **Reactive Streams 기반의 Non-blocking DB 접근 모델**입니다.

```
Event Loop Thread
  → R2DBC Query 요청
  → Non-blocking Socket I/O로 DB 통신
  → Thread 점유 최소화
  → DB 응답 이벤트 수신 후 처리
```

R2DBC는 DB 통신을 Event Loop 기반 Non-blocking 구조에 맞추기 위한 방식입니다.

---

## 7. WebClient와 Netty

Spring WebClient는 **Reactor Netty 기반**으로 동작합니다.

```
WebClient
  → Reactor Netty
  → Event Loop
  → epoll / NIO / io_uring transport
  → Linux Kernel I/O
```

Linux 환경에서 Netty는 NIO 또는 Native epoll transport를 통해 네트워크 I/O를 처리합니다.  
WebClient 자체는 비동기 네트워크 I/O에 적합하지만, **내부에서 Blocking 라이브러리나 JDBC를 호출하면 Event Loop가 차단**될 수 있습니다.

---

## 8. 구현 계층 정리

| 계층 | 예시 | 실제 I/O 엔진 |
|------|------|-------------|
| Application | WebClient, Repository, Service | 추상화된 API |
| Framework | Spring WebFlux, Netty, Tomcat | Event Loop 또는 Worker Pool |
| Runtime | JVM, JNI | Native I/O 호출 |
| OS Interface | epoll, io_uring, read/write | Kernel I/O 인터페이스 |
| Kernel | task_struct, FD, Socket Buffer | 실제 상태 관리 |
| Hardware | NIC, Disk, CPU | 데이터 송수신 및 연산 |

---

## 9. 네트워크 패킷 처리 흐름

```
Packet 도착
  → NIC DMA로 메모리에 기록
  → Hardware Interrupt 발생
  → Kernel Interrupt Handler 실행
  → Softirq / TCP/IP Stack 처리
  → Socket Buffer 갱신
  → epoll Ready List 갱신
  → Event Loop Handler 실행
```

트래픽이 많아지면 **Softirq CPU 사용률이 증가**할 수 있으며, 이는 CPU Saturation 분석 시 중요한 지표입니다.

---

## 10. 파일/디스크 I/O 처리 흐름

```
File Read 요청
  → Page Cache 조회
  → Cache Hit → 즉시 반환
  → Cache Miss → Disk I/O 발생 → Blocking 가능
```

- **epoll**: 일반 파일 FD를 항상 Ready로 간주하는 경향이 있어, 실제 Disk I/O 지연을 제거하지 못할 수 있습니다.
- **io_uring**: 파일 I/O 요청 자체를 Kernel에 비동기로 제출하고, 완료 결과를 CQ로 수신하므로 디스크 I/O 처리에 더 적합합니다.

---

## 11. CPU Saturation으로 이어지는 연쇄

### Blocking I/O 기반 구조

```
Thread-per-request
  → Blocking I/O
  → Thread Pool 점유 증가
  → task_struct 수 증가
  → Wait Queue / Runnable Queue 이동 증가
  → Context Switch 증가
  → Scheduler Overhead 증가
  → CPU Saturation
```

### Non-blocking 구조

```
Event Loop
  → epoll 또는 io_uring
  → 적은 수의 task_struct 유지
  → Context Switch 감소
  → Scheduler Overhead 감소
  → CPU 효율 증가
```

---

## 12. SRE 관점 주요 지표

| 지표 | 의미 |
|------|------|
| Thread Count | JVM/OS Thread 수 |
| Runnable Thread 수 | CPU 경쟁 상태 여부 |
| Waiting Thread 수 | I/O 또는 Lock 대기 상태 |
| Context Switches | 실행 전환 비용 |
| System CPU (`sy`) | Kernel 작업 비중 |
| User CPU (`us`) | 애플리케이션 작업 비중 |
| Softirq CPU | 네트워크 패킷 처리 비용 |
| Open FD Count | 열린 I/O 통로 수 |
| DB Connection Pool Usage | DB 병목 여부 |
| Run Queue Length | CPU 대기 중인 task 수 |
| Load Average | Running/Runnable 및 D 상태(Uninterruptible Sleep) 누적 |
| Cache Miss Rate | Page Cache 효율 저하 여부 |
| P99 / P999 Latency | 사용자 관점 지연 |

---

## 13. 진단 명령어

```bash
# CPU 사용률 및 context switch 전반
vmstat 1

# Thread별 CPU 사용률 확인
top -H -p <PID>

# Process별 context switch 횟수
pidstat -w -p <PID> 1

# CPU core별 사용률 (sy, softirq 포함)
mpstat -P ALL 1

# 소켓 연결 상태 요약
ss -s

# 열린 File Descriptor 수
lsof -p <PID> | wc -l

# context switch, cache miss 등 하드웨어 카운터
perf stat -e context-switches,cpu-migrations,cache-misses,cache-references -p <PID>

# CPU Pressure 지표 (PSI)
cat /proc/pressure/cpu
```

---

## 14. 핵심 정리

| 방식 | 특징 요약 |
|------|----------|
| **Blocking I/O** | Thread가 I/O 완료까지 대기. `task_struct`가 Wait Queue로 이동. Thread Pool 점유. Thread 수 및 Context Switch 증가 가능 |
| **epoll** | FD 이벤트 기반 통지. 소수의 Event Loop Thread로 다수의 네트워크 연결 처리. Runnable Queue 길이 안정화. 네트워크 I/O에 적합 |
| **io_uring** | SQ/CQ 기반 비동기 I/O. 시스템 콜 오버헤드 감소. 네트워크와 디스크 I/O 모두에 적합. `task_struct` 수와 Scheduler Overhead 감소에 유리 |

실무에서는 epoll이나 io_uring을 직접 구현하는 경우보다, **WebClient, Netty, R2DBC 같은 상위 라이브러리가 내부적으로 어떤 I/O 엔진을 사용하는지 이해하는 것**이 중요합니다.

---

> **결론**  
> I/O 처리 방식이 `task_struct`의 상태를 결정하고,  
> `task_struct`의 상태가 Runnable Queue, Wait Queue, Scheduler Overhead,  
> 그리고 CPU Saturation 여부를 결정합니다.

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*