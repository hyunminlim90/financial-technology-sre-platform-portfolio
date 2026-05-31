# Netty Event Loop와 Network I/O 처리 구조

## 1. 개요

Netty는 **비동기(Asynchronous) 이벤트 기반(Event-Driven)** 네트워크 애플리케이션 프레임워크입니다.

핵심 설계 목적은 다음 네 가지를 동시에 달성하는 것입니다.

```
대량 네트워크 연결  +  낮은 Thread 수  +  높은 Throughput  +  낮은 Latency
```

이를 위해 **Event Loop 기반의 Non-Blocking I/O 구조**를 사용합니다.

### Netty의 핵심 역할

| 역할 | 설명 |
|------|------|
| 네트워크 연결 관리 | TCP / HTTP / WebSocket |
| 데이터 수신 | Socket Read |
| 데이터 송신 | Socket Write |
| 이벤트 처리 | Connection / Event Dispatch |
| 비동기 처리 | Non-Blocking Execution |

---

## 2. Event Loop 구조

### Event Loop의 역할

Event Loop는 지속적으로 이벤트를 감시하고 처리하는 실행 흐름입니다.

| 감시 이벤트 | 설명 |
|-------------|------|
| Accept Event | 연결 수락 |
| Read Event | 데이터 수신 |
| Write Event | 데이터 송신 |
| Close Event | 연결 종료 |

### 실행 흐름

```
EventLoop Thread
      ↓
Selector Polling
      ↓
Event Detection
      ↓
ChannelPipeline Execution
```

### OS 계층 구조

Netty EventLoop Thread는 `java.lang.Thread` 기반이며, OS 관점에서 일반 Software Thread와 동일하게 취급됩니다.

```
Netty EventLoop Thread
        ↓
    OS Thread
        ↓
 Kernel Scheduler
        ↓
   Logical CPU
        ↓
 Hardware Thread
        ↓
  Physical Core
```

---

## 3. Network I/O 처리 구조

### Network I/O 대상

Netty가 주로 처리하는 I/O 유형입니다.

| I/O 유형 | 설명 |
|----------|------|
| TCP Socket I/O | 핵심 처리 대상 |
| HTTP I/O | 주요 사용 사례 |
| WebSocket I/O | 실시간 연결 |
| External API I/O | 네트워크 기반 외부 호출 |

> 외부 REST API 호출(`WebClient` → HTTP Request → Remote Server)도 네트워크 기반이므로 Network I/O에 해당합니다.

### Inbound / Outbound 데이터 흐름

```
[Inbound]
NIC → Kernel Socket Buffer → Netty Read Event → Pipeline → Application Logic

[Outbound]
Application Logic → Pipeline → Socket Write → NIC
```

### Non-Blocking vs Blocking I/O 비교

```
Blocking 방식                  Non-Blocking 방식
    ↓                               ↓
 Read Call                    Event Polling
    ↓                               ↓
 Data Wait                  Ready Event만 처리
    ↓
  Blocked
```

### Selector 기반 다중 연결 감시

```java
selector.select();  // 하나의 Thread가 여러 Socket 이벤트를 감시
```

하나의 EventLoop가 다수의 Channel을 관리합니다.

```
EventLoop
├── Channel A
├── Channel B
├── Channel C
└── Channel N
```

Thread가 I/O 대기 상태로 멈추지 않기 때문에, 적은 수의 Thread로 대량의 연결을 처리할 수 있습니다.

---

## 4. EventLoop에서 수행하면 안 되는 작업

다음 작업을 EventLoop 내부에서 직접 수행하면 해당 Loop가 관리하는 **모든 Channel에 영향**이 발생합니다.

| 유형 | 특징 |
|------|------|
| Disk I/O | 대부분 Blocking |
| 전통적인 JDBC | Blocking 방식 |
| File System Access | Blocking 가능 |
| CPU Intensive Task | EventLoop 장시간 점유 |

### Disk I/O 문제

```java
Files.readAllBytes(path);  // Blocking
```

```
EventLoop → Disk Read Wait → Thread Blocked → All Assigned Channels Delayed
```

### JDBC Blocking 문제

```java
ResultSet rs = statement.executeQuery();  // 결과를 기다리는 동안 Thread 점유
```

### CPU Intensive 작업 문제

Heavy JSON Parsing, Complex Encryption, Large Compression 등의 작업이 EventLoop를 장시간 점유하면 I/O 이벤트 처리 자체가 지연됩니다.

### EventLoop Blocking 시 영향

| 영향 | 설명 |
|------|------|
| Request Delay | 응답 지연 |
| Timeout 증가 | 연결 실패 |
| Throughput 감소 | 처리량 감소 |
| Tail Latency 증가 | 응답 시간 불균형 |

---

## 5. Blocking / Heavy Task 분리 (Offloading)

무거운 작업은 반드시 별도 Worker Pool로 분리해야 합니다.

```
EventLoop
    ↓
Worker Thread Pool
    ↓
Blocking Task 수행
```

### ioRatio 설정

Netty는 EventLoop의 I/O 처리 비율을 조절할 수 있습니다.

| 작업 | 설명 |
|------|------|
| I/O Task | Read / Write / Event 처리 |
| Non-I/O Task | 사용자 로직 |

기본값 예시: I/O 50% / Non-I/O 50% (조정 가능)

---

## 6. Spring WebFlux / Reactive 환경과의 연계

### Spring WebFlux + Netty

```
HTTP Request
      ↓
Netty EventLoop
      ↓
Reactive Pipeline
      ↓
Non-Blocking Processing
```

### R2DBC (Non-Blocking DB)

Reactive 환경에서는 전통적인 JDBC 대신 R2DBC 사용이 권장됩니다.

```
Reactive DB Driver → Non-Blocking Query → EventLoop Friendly
```

---

## 7. 운영(SRE) 관점

### 주요 장애 패턴

| 증상 | 원인 |
|------|------|
| Low CPU + High Latency | EventLoop Blocking |
| Timeout 증가 | Blocking JDBC 사용 |
| Throughput 감소 | EventLoop 내 Disk I/O 수행 |
| EventLoop Saturation | CPU Intensive Task 점유 |

### 운영 핵심 원칙

| 원칙 | 설명 |
|------|------|
| EventLoop Blocking 금지 | 핵심 원칙 |
| Blocking I/O 분리 | Worker Pool 사용 |
| CPU Heavy Task 분리 | 별도 Executor 사용 |
| Non-Blocking Driver 사용 | WebClient / R2DBC 권장 |

---

## 8. 전체 처리 흐름

```
Client Connection
      ↓
     NIC
      ↓
Kernel Socket Buffer
      ↓
  Netty EventLoop
      ↓
    Pipeline
      ↓
 Reactive Logic
      ↓
 Response Write
```

---

## 9. 핵심 정리

| 구성 요소 | 역할 |
|-----------|------|
| EventLoop | 이벤트 감시 및 처리 |
| Network I/O | Socket 기반 데이터 송수신 |
| Selector | 다중 연결 감시 |
| Channel | 네트워크 연결 단위 |
| Pipeline | 이벤트 처리 체인 |
| Non-Blocking | Thread 대기 최소화 |
| Worker Pool | Blocking 작업 분리 실행 |

### 결론

```
Netty = Event-Driven + Non-Blocking + Network I/O Framework
```

**EventLoop 담당:**
- 네트워크 이벤트 감시
- Socket I/O 처리
- 다중 연결 관리
- Non-Blocking 실행

**Worker Pool로 분리해야 하는 작업:**
- Disk I/O
- Blocking JDBC
- 장시간 CPU 연산

```
EventLoop       → Fast Network I/O Only
Blocking/Heavy  → Offload to Worker Pool
```

이는 Spring WebFlux, Reactive System, Kubernetes 기반 고성능 서버, 금융 시스템 저지연 처리, 대규모 동시 연결 처리 환경의 핵심 설계 원칙입니다.

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*