# Netty, WebFlux, Native I/O와 SRE 관점

## 개요

Java 기반 고성능 서버에서는 표준 Java I/O 외에도 Netty, WebFlux, Native Transport, epoll, io_uring, Direct Memory, eBPF 같은 기술을 계층별로 활용합니다.

각 기술은 동일한 계층에서 경쟁하는 것이 아니라, 아래와 같이 역할이 분리되어 있습니다.

```
Application Layer
  → Spring WebFlux / WebClient
  → Reactor Netty
  → Netty Native Transport
  → epoll / io_uring
  → Linux Kernel
  → NIC / Disk / CPU
```

---

## 1. Netty

### 역할

Netty는 Java 진영의 **비동기 이벤트 기반 네트워크 프레임워크**입니다.  
단순한 Socket Wrapper가 아니라, 대규모 연결 처리, 프로토콜 구현, 메모리 관리, EventLoop 기반 I/O를 제공하는 저수준 네트워크 엔진입니다.

### 적용 사례

- 고성능 TCP / HTTP / WebSocket 서버
- API Gateway, Proxy Server
- Custom Protocol Server
- 게임 서버, 채팅 서버, 메시징 시스템
- 데이터베이스 드라이버
- 네트워크 인프라 소프트웨어

### 핵심 구성 요소

| 구성 요소 | 설명 |
|----------|------|
| EventLoop | 적은 수의 Thread로 다수의 연결을 처리 |
| Channel | 네트워크 연결 단위를 추상화 |
| ChannelPipeline | 요청/응답 처리 단계를 체인 구조로 구성 |
| Codec | Byte 데이터와 객체 간 변환 처리 |
| ByteBuf | 고성능 Buffer 관리 |
| Native Transport | Linux epoll, io_uring 등 활용 가능 |

---

## 2. Spring WebFlux

### 역할

Spring WebFlux는 Spring 5부터 도입된 **Reactive Web Framework**입니다.  
Netty 같은 비동기 네트워크 엔진 위에서 HTTP API 서버를 구성할 수 있도록 Spring 추상화를 제공합니다.  
개발자가 Netty의 저수준 API를 직접 다루지 않고도 Non-blocking 서버를 개발할 수 있게 해줍니다.

### 적용 사례

- 비동기 HTTP API 서버
- 고동시성 API Gateway
- 외부 API 병렬 호출
- SSE(Server-Sent Events), WebSocket
- Streaming API
- MSA 간 Non-blocking 통신

### 핵심 구성 요소

| 구성 요소 | 설명 |
|----------|------|
| Mono | 0개 또는 1개의 비동기 결과를 표현 |
| Flux | 0개 이상의 비동기 스트림을 표현 |
| WebClient | Non-blocking HTTP Client |
| Reactor | Reactive Streams 기반 실행 모델 |
| Netty | WebFlux 기본 서버 엔진 |

---

## 3. Netty와 WebFlux의 관계

Netty와 WebFlux는 경쟁 관계가 아니라 **계층이 다릅니다**.

```
Spring WebFlux
  → Reactor Netty
  → Netty
  → epoll / io_uring / Java NIO
  → Linux Kernel
```

| 구분 | Netty | Spring WebFlux |
|------|-------|---------------|
| 계층 | Low-level Network Framework | High-level Web Framework |
| 주요 대상 | 네트워크 엔진, 프로토콜 개발 | HTTP API, 웹 서비스 개발 |
| 주요 추상화 | Channel, EventLoop, ByteBuf | Controller, Router, Mono, Flux |
| 난이도 | 높음 | 상대적으로 낮음 |
| 사용 사례 | Custom Protocol, Proxy, Gateway Engine | API Server, WebClient, Streaming |

일반적인 HTTP API 서버나 MSA 서비스는 WebFlux를 사용하는 것이 적합합니다.  
HTTP가 아닌 독자 프로토콜이나 네트워크 엔진 자체를 개발해야 하는 경우 Netty를 직접 사용합니다.

---

## 4. WebClient와 Netty

Spring WebClient는 **Reactor Netty 기반**으로 동작합니다.

```
WebClient
  → Reactor Netty
  → Netty EventLoop
  → Linux epoll / Java NIO / io_uring
```

WebClient는 외부 API 호출을 Non-blocking 방식으로 처리할 수 있습니다.  
단, WebClient 내부에서 Blocking 라이브러리나 JDBC를 직접 호출하면 EventLoop가 차단됩니다.

---

## 5. Java I/O 구현체 비교

| 구분 | 대표 API / 구현체 | 특징 |
|------|-----------------|------|
| Blocking I/O | `java.io`, JDBC | Thread가 I/O 완료까지 대기 |
| Java NIO | Selector, SocketChannel | Non-blocking 네트워크 I/O 지원 |
| Netty epoll | `EpollEventLoopGroup` | Linux epoll을 Native로 활용 |
| Netty io_uring | `IOUringEventLoopGroup` | SQ/CQ 기반 최신 Linux I/O 활용 |

---

## 6. Blocking I/O

I/O가 완료될 때까지 현재 Thread가 대기하는 방식입니다.

**대표 구현체**: `InputStream.read()`, `FileInputStream`, `SocketInputStream`, JDBC, Blocking SDK

```
Java Thread
  → Blocking I/O 호출
  → System Call
  → task_struct → Wait Queue 이동
  → I/O 완료 대기
  → Runnable Queue 복귀
  → 다음 코드 실행
```

구현이 단순하지만, 동시 요청이 많아지면 **Thread Pool 고갈**과 `task_struct` 증가를 유발할 수 있습니다.

---

## 7. epoll 기반 Non-blocking I/O

Linux Kernel의 **이벤트 기반 I/O 통지 메커니즘**입니다.  
Netty는 Linux 환경에서 Native epoll transport를 사용할 수 있습니다.

```
Socket FD 등록
  → epoll_ctl()
  → epoll_wait()
  → 이벤트 발생 FD 수신
  → EventLoop Handler 실행
```

소수의 EventLoop Thread로 다수의 연결을 처리하므로, **Runnable Queue 길이와 Context Switch 비용을 줄이는 데 유리**합니다.  
네트워크 Socket I/O에 강점이 있습니다.

---

## 8. io_uring 기반 I/O

Linux Kernel의 **고성능 비동기 I/O 인터페이스**입니다.  
epoll/read/write 방식 대비 System Call 횟수와 User Mode / Kernel Mode 전환 비용을 줄이기 위해 설계되었습니다.

| 구성 | 역할 |
|------|------|
| SQ (Submission Queue) | Application이 Kernel에 I/O 요청을 제출하는 Queue |
| CQ (Completion Queue) | Kernel이 완료 결과를 기록하는 Queue |

```
Application Thread
  → SQ에 I/O 요청 기록
  → Kernel이 I/O 처리
  → CQ에 완료 결과 기록
  → Application이 CQ에서 결과 확인
```

네트워크 I/O뿐 아니라 **파일/디스크 I/O까지 비동기 처리** 가능합니다.

---

## 9. epoll과 io_uring 비교

| 구분 | epoll | io_uring |
|------|-------|---------|
| 기본 모델 | 이벤트 통지 | 비동기 작업 제출/완료 |
| 주요 대상 | 네트워크 Socket I/O | 네트워크 + 파일/디스크 I/O |
| Kernel 통신 | `epoll_wait` 시스템 콜 | SQ/CQ 공유 Ring Buffer |
| System Call 비용 | 존재 | 줄일 수 있음 |
| 파일 I/O 처리 | 제한적 | 적합 |
| 대표 구현 | Netty epoll transport | Netty io_uring transport |

- **epoll**: 특정 FD에 데이터가 준비되었는지 통지받는 모델
- **io_uring**: I/O 작업을 Kernel에 비동기로 제출하고 완료 결과를 수신하는 모델

---

## 10. 파일 I/O와 네트워크 I/O의 차이

| 구분 | 네트워크 I/O | 파일/디스크 I/O |
|------|------------|--------------|
| 대기 원인 | 상대방 패킷 도착 대기 | Page Cache 또는 Disk 응답 대기 |
| epoll 적합성 | 높음 | 제한적 |
| Blocking 위험 | Driver/API에 따라 다름 | Page Cache Miss 시 Blocking 가능 |
| io_uring 적합성 | 높음 | 높음 |

epoll은 일반 파일 FD를 항상 Ready로 간주하는 경향이 있어 실제 디스크 접근 지연을 제거하지 못할 수 있습니다.  
io_uring은 파일 I/O 작업 자체를 Kernel에 비동기로 제출할 수 있어 디스크 I/O 처리에 더 적합합니다.

---

## 11. JDBC와 R2DBC

### JDBC

JDBC는 **Blocking 방식**으로 동작합니다.

```
Java Thread
  → JDBC Query 실행
  → DB 응답 대기
  → Thread Blocked (Thread Pool 점유 유지)
```

WebFlux나 Netty EventLoop 내부에서 JDBC를 직접 호출하면 **EventLoop Thread 자체가 차단**됩니다.

### R2DBC

R2DBC는 **Reactive Streams 기반의 Non-blocking DB 접근 모델**입니다.

```
EventLoop Thread
  → R2DBC Query 요청
  → Non-blocking Socket I/O로 DB 통신
  → Thread 점유 최소화
  → DB 응답 이벤트 수신 후 처리
```

WebFlux 기반 시스템에서 DB 접근까지 Non-blocking으로 유지하려면 R2DBC와 같은 Non-blocking Driver가 필요합니다.

---

## 12. Direct Memory와 Zero-copy

고성능 네트워크 처리에서는 JVM Heap 외에 **Direct Memory**를 활용하는 경우가 많습니다.

**대표 기술**
- `ByteBuffer.allocateDirect()`
- Netty ByteBuf (Pooled Direct Buffer)
- Zero-copy File Transfer (`sendfile`)

Kernel/User Space 간 불필요한 데이터 복사를 줄이고, GC 부담을 낮추는 효과가 있습니다.

---

## 13. Native Transport

Netty는 Java 표준 NIO 외에도 **Linux 전용 Native Transport**를 지원합니다.

**대표 구현체**
- `netty-transport-native-epoll`
- `netty-incubator-transport-io_uring`
- OpenSSL 기반 Native TLS
- Direct Buffer 기반 ByteBuf

Native Transport를 사용하면 Linux Kernel 기능을 더 직접적으로 활용할 수 있습니다.  
단, **Kernel 버전, 배포판, 라이브러리 호환성, 운영 안정성**을 함께 검토해야 합니다.

---

## 14. 결제 시스템에서의 적용 관점

결제 시스템에서는 성능보다 **정확성, 안정성, 정합성, 장애 복구 가능성**이 우선입니다.  
Native 기술은 모든 비즈니스 로직에 적용하기보다, 아래 계층에 전략적으로 적용하는 것이 일반적입니다.

| 계층 | 적용 가능 기술 |
|------|-------------|
| API Gateway | Netty epoll, WebFlux, Native TLS |
| 외부 API 호출 | WebClient, Reactor Netty |
| DB 접근 | R2DBC 또는 격리된 JDBC Worker Pool |
| 트랜잭션 로그 | Kafka, mmap 기반 Queue, Direct Buffer |
| TLS / 암호화 | OpenSSL Native, AES-NI 활용 |
| Observability | eBPF, perf, async-profiler |

비즈니스 로직은 유지보수성과 정합성을 우선하고, I/O 및 네트워크 계층에서 Native 최적화를 적용합니다.

---

## 15. SRE 관점에서의 역할

SRE는 epoll, io_uring, Native Transport를 직접 구현하기보다, 이 기술들이 **시스템 안정성과 성능에 미치는 영향을 관찰하고 제어**하는 역할을 수행합니다.

**주요 관심 지표**
- Thread 수 증가 여부
- Runnable Queue 증가 여부
- Context Switch 증가 여부
- System CPU 증가 여부
- Softirq 증가 여부
- Open FD 고갈 여부
- Direct Memory 사용량
- Native Memory Leak
- Kernel Version 호환성
- cgroup 제한 / CPU Throttling
- P99 / P999 Latency

---

## 16. eBPF와 SRE Observability

eBPF는 **Kernel 내부 이벤트를 안전하게 관찰**할 수 있는 기술입니다.

### SRE 활용 용도

- 네트워크 지연 추적
- TCP Retransmission 분석
- Disk I/O 지연 분석
- System Call Latency 분석
- Scheduler Latency 분석
- Container Network 관찰
- 보안 이벤트 탐지

### 대표 도구

| 도구 | 특징 |
|------|------|
| Cilium | Kubernetes 네트워크 정책 및 관찰 |
| Pixie | Kubernetes 환경 자동 계측 |
| bpftrace | 커스텀 Kernel 이벤트 추적 스크립트 |
| BCC tools | 다양한 사전 제작 eBPF 분석 도구 모음 |
| perf | Linux 성능 카운터 분석 |
| bpftool | eBPF 프로그램 및 맵 관리 |

eBPF는 애플리케이션 코드를 수정하지 않고 **Kernel 수준의 병목을 분석**하는 데 유용합니다.

---

## 17. Kernel Bypass와 DPDK

Kernel Bypass는 Linux Kernel 네트워크 스택을 우회하여 User Space 애플리케이션이 NIC 패킷을 직접 처리하는 방식입니다. 대표 기술은 **DPDK**입니다.

초저지연 네트워크 처리에 유리하지만, 다음과 같은 부담이 있습니다.

- 구현 복잡도 및 운영 난이도 증가
- Kernel 네트워크 스택 기능을 직접 대체해야 함
- 장애 분석 난이도 증가

결제 시스템에서는 HFT 수준의 Kernel Bypass보다 **Netty Native Transport, epoll, io_uring, eBPF 관찰** 수준이 더 현실적인 선택입니다.

---

## 18. SRE 주요 진단 지표

| 지표 | 의미 |
|------|------|
| Thread Count | JVM/OS Thread 수 |
| Runnable Thread 수 | CPU 경쟁 상태 |
| Context Switches | 실행 전환 비용 |
| Run Queue Length | CPU 대기 task 수 |
| CPU `us` | 애플리케이션 실행 비중 |
| CPU `sy` | Kernel 작업 비중 |
| Softirq | 네트워크 패킷 처리 비용 |
| Open FD Count | 열린 I/O 통로 수 |
| Direct Memory | Off-heap 메모리 사용량 |
| GC Pause | JVM Heap 관리 비용 |
| Native Memory | JVM 외부 메모리 사용량 |
| CPU Throttling | cgroup CPU 제한 영향 |
| P99 / P999 Latency | 사용자 관점 지연 |

---

## 19. 진단 명령어

```bash
# CPU 사용률 및 context switch 전반
vmstat 1

# Thread별 CPU 사용률
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

# CPU Pressure (PSI)
cat /proc/pressure/cpu

# I/O Pressure (PSI)
cat /proc/pressure/io
```

---

## 20. 핵심 정리

```
Spring WebFlux       ← HTTP API 추상화
  → Reactor Netty    ← Non-blocking HTTP 엔진
  → Netty            ← 네트워크 프레임워크
  → epoll / io_uring ← OS I/O 인터페이스
  → Linux Kernel
```

| 방식 | 특징 요약 |
|------|----------|
| **Blocking I/O** | Thread를 Wait Queue로 이동. Thread Pool 점유 유지 |
| **epoll** | 소수의 EventLoop Thread로 다수의 네트워크 FD 이벤트 처리 |
| **io_uring** | SQ/CQ 기반 비동기 처리. 네트워크 + 파일 I/O 모두 처리 가능. System Call 비용 감소 |

**결제 시스템 적용 원칙**: 비즈니스 로직은 유지보수성과 정합성 우선. Native 최적화는 Gateway, 외부 API 호출, TLS, 네트워크 I/O, Observability 계층에 전략적으로 적용.

**SRE 핵심 역할**: 저수준 기술의 직접 구현이 아니라, Thread, `task_struct`, FD, Kernel CPU, Softirq, Direct Memory, Latency 지표를 관찰하고 장애 전파를 제어하는 것.

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*