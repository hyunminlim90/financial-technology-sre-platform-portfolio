# Java Thread와 Netty Event Loop Thread 구조 비교

## 1. 개요

일반 Java Thread와 Netty Event Loop Thread는 모두 OS 관점에서 **Software Thread**이며, `java.lang.Thread` 기반으로 실행됩니다. 운영체제 Scheduler에 의해 Logical CPU에 배치된다는 점도 동일합니다.

그러나 두 방식은 **실행 모델과 자원 관리 방식**에서 근본적인 차이를 가집니다.

### 공통 특성

| 공통 요소 | 설명 |
|-----------|------|
| Java Thread 기반 | `java.lang.Thread` 사용 |
| OS Thread 사용 | Kernel Scheduler 대상 |
| Logical CPU 실행 | CPU 스케줄링 대상 |
| Register / Stack 보유 | 독립 실행 흐름 |
| Context Switching 발생 가능 | Scheduler 제어 대상 |

### 공통 실행 계층

```
Java Thread / Netty EventLoop Thread
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

## 2. 일반 Java Thread 모델

### 실행 구조

일반 Java Thread는 **Thread-per-Task** 또는 **Thread-per-Request** 방식으로 동작합니다. 각 Thread가 하나의 작업을 전담하여 처리합니다.

```java
new Thread(() -> {
    processRequest();
}).start();
```

### 특징

| 특징 | 설명 |
|------|------|
| 작업 중심 모델 | Thread가 작업을 직접 전담 |
| Blocking 가능 | I/O 대기 시 Thread 정지 |
| 독립 실행 흐름 | Thread별 Stack 유지 |
| 요청 수 비례 Thread 증가 | 요청 증가 시 Thread 수 증가 |

### Blocking 구조

```java
socket.read();  // 데이터가 도착할 때까지 Thread 대기
```

I/O 작업 중에는 Thread가 Blocked 상태로 전환되며, CPU를 사용하지 않은 채 대기합니다.

### Thread-per-Request의 문제

요청 수가 증가할수록 다음 문제가 발생합니다.

| 문제 | 설명 |
|------|------|
| Context Switching 증가 | Thread 전환 비용 증가 |
| Memory 사용 증가 | Thread별 Stack 메모리 증가 |
| Scheduler Overhead 증가 | CPU 관리 비용 증가 |
| CPU Cache 효율 저하 | Cache Miss 증가 |

```
More Requests → More Threads → More Context Switching
```

---

## 3. Netty Event Loop 모델

### 실행 구조

Netty는 **Event-Driven / Non-Blocking** 모델을 사용합니다.

```
EventLoop Thread
      ↓
Selector Polling
      ↓
Event Detection
      ↓
Pipeline Execution
```

### 핵심 구성 요소

```
EventLoop
    ↓
  Channel
    ↓
 Pipeline
    ↓
  Handler
```

### 처리 대상 이벤트

| 이벤트 | 설명 |
|--------|------|
| Read Event | 데이터 수신 |
| Write Event | 데이터 송신 |
| Connect Event | 연결 생성 |
| Close Event | 연결 종료 |

### Selector 기반 다중 연결 처리

Netty는 Java NIO `Selector`를 사용합니다.

```java
selector.select();  // 하나의 Thread가 다수의 Channel 이벤트를 감시
```

이벤트가 발생한 Channel만 골라 처리하므로, 하나의 Thread로 수만 개의 연결을 처리할 수 있습니다.

### Event-Driven 처리 흐름

```
Event 발생
    ↓
Handler 실행
    ↓
다시 Event 대기
```

### Pipeline 구조

```
Event
    ↓
ChannelPipeline
    ↓
Handler 1
    ↓
Handler 2
    ↓
Handler 3
```

### 핵심 특징

| 특징 | 설명 |
|------|------|
| 적은 Thread 수 | CPU Core 수 기반 생성 (일반적으로 Core × 2) |
| 다수 연결 처리 | 수만 개 Channel 처리 가능 |
| Non-Blocking | I/O 대기 최소화 |
| Event-Driven | 이벤트 발생 시에만 작업 수행 |
| Long-Lived Thread | 지속 실행 구조 |

### Channel과 EventLoop 관계

하나의 Channel은 하나의 EventLoop에 고정 연결됩니다.

```
Channel → Assigned EventLoop (변경되지 않음)
```

이를 통해 동일 Channel에 대한 동기화 비용을 줄입니다.

---

## 4. 일반 Thread vs Netty Event Loop 비교

| 항목 | 일반 Java Thread | Netty Event Loop |
|------|-----------------|-----------------|
| 실행 모델 | Thread-per-Task | Event-Driven |
| I/O 방식 | Blocking 가능 | Non-Blocking |
| Thread 수 | 요청 수 비례 증가 | Core 수 기반 고정 |
| 연결 처리 | Thread별 전담 | Event 기반 공유 |
| Context Switching | 많음 | 상대적으로 적음 |
| 확장성 | 제한적 | 매우 높음 |

### I/O 처리 방식 비교

```
일반 Thread              Netty Event Loop
    ↓                          ↓
 I/O Wait                Event Polling
    ↓                          ↓
 Blocked             Ready Event만 처리
```

---

## 5. Event Loop 사용 시 주의사항

### Blocking 작업 금지

Event Loop 내부에서 무거운 작업이 실행되면 해당 Loop에 연결된 **모든 Channel이 지연**됩니다.

**금지 패턴:**
```java
Thread.sleep(1000);        // Blocking
heavyCpuComputation();     // CPU 장시간 점유
blockingJdbcQuery();       // Blocking I/O
```

**결과:**
```
EventLoop Stall → All Assigned Channels Delayed
```

### EventLoop 운영 원칙

| 원칙 | 설명 |
|------|------|
| Non-Blocking 유지 | EventLoop 정지 방지 |
| 짧은 작업 수행 | 빠른 Event 처리 |
| Heavy Task 분리 | 별도 Executor(Worker Pool) 사용 |
| CPU Bound 작업 최소화 | Loop 응답성 유지 |

### Heavy Task 분리 구조

```
EventLoop
    ↓
Worker Pool (별도 Thread Pool)
    ↓
Heavy Task Execute
```

---

## 6. 운영(SRE) 관점

### 일반 Thread 모델 모니터링 항목

| 문제 | 설명 |
|------|------|
| Thread Explosion | 과도한 Thread 수 증가 |
| CPU Throttling | Scheduler 부하 증가 |
| Memory 증가 | Stack 메모리 증가 |
| Context Switching 증가 | 전반적인 성능 저하 |

### Netty 운영 모니터링 항목

| 항목 | 설명 |
|------|------|
| Blocking 감시 | EventLoop Stall 방지 |
| EventLoop CPU Usage | Loop 과부하 감시 |
| Backpressure 관리 | Queue 폭주 방지 |
| Handler 실행 시간 | 짧은 실행 시간 유지 확인 |

---

## 7. Spring WebFlux와의 관계

Spring WebFlux는 Netty Event Loop 기반으로 동작합니다.

```
HTTP Request
      ↓
Netty EventLoop
      ↓
Reactive Pipeline
      ↓
Non-Blocking Processing
```

---

## 8. 핵심 정리

| 구성 요소 | 일반 Java Thread | Netty Event Loop |
|-----------|-----------------|-----------------|
| 실행 모델 | Thread-per-Task | Event-Driven |
| 실행 방식 | Blocking 가능 | Non-Blocking |
| Thread 수 | 많음 | 적음 |
| 연결 처리 | Thread별 전담 | Event 기반 공유 |
| Context Switching | 많음 | 적음 |
| 확장성 | 제한적 | 높음 |

### 결론

```
General Thread   =  Task-Centric Execution (Thread가 작업을 직접 전담)
Netty EventLoop  =  Event-Driven Non-Blocking Execution (이벤트 감지 후 처리)
```

Netty Event Loop는 **적은 수의 Thread**, **Non-Blocking I/O**, **Event 기반 처리**, **지속적인 Event Polling**을 통해 높은 Throughput과 확장성을 달성합니다.

이 구조는 Spring WebFlux, Reactive System, 고성능 네트워크 서버, 대규모 동시 연결 처리 환경의 핵심 기반 개념입니다.

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*