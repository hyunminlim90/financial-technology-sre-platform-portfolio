# Software Thread와 프로그램 실행 흐름(Execution Flow)

## 1. Software Thread 개요

Software Thread는 운영체제(OS) 또는 런타임(JVM 등)이 관리하는 **독립적인 프로그램 실행 흐름(Execution Flow)**입니다.

- Thread는 하나의 **Process 내부**에서 실행되는 최소 실행 단위입니다.
- 각 Thread는 독립적인 실행 상태와 실행 위치를 가집니다.

```
Software Thread  =  Independent Execution Flow
```

---

## 2. Process와 Thread 관계

Process는 실행 중인 프로그램 전체를 의미하며, Thread는 그 내부의 실행 단위입니다.

### Process 구조

```
Process
├── Code Segment        (공유)
├── Heap Memory         (공유)
├── Shared Resources    (공유)
├── Thread 1            (독립 실행 흐름)
├── Thread 2
└── Thread 3
```

### 공유/독립 자원

| 구분 | 자원 | 설명 |
|------|------|------|
| **공유** | Heap Memory | 객체 및 데이터 |
| **공유** | Code Segment | 실행 코드 |
| **공유** | File Descriptor | 파일 및 소켓 |
| **공유** | Process Address Space | 가상 메모리 공간 |
| **독립** | Program Counter | 현재 실행 위치 |
| **독립** | Stack | 함수 호출 상태 |
| **독립** | Register Context | CPU 상태 |
| **독립** | Thread State | 실행 상태 |

---

## 3. Thread의 실행 상태 구성 요소

각 Thread가 독립적으로 유지하는 구성 요소입니다.

| 구성 요소 | 설명 |
|-----------|------|
| Program Counter | 현재 실행 중인 명령어 위치 |
| Stack | 함수 호출 정보, 지역 변수, 반환 주소, 임시 데이터 |
| Register Context | CPU 실행 상태 |
| Control Flow | 다음 실행 경로 |
| Execution State | Running, Waiting 등 실행 상태 |

### Thread별 Stack 구조

```
Process
├── Shared Heap
├── Thread A Stack
├── Thread B Stack
└── Thread C Stack
```

Stack은 Thread 간 독립적으로 유지되므로, 각 Thread는 서로 다른 함수 호출 상태와 지역 변수를 가집니다.

### 독립적 실행 흐름 예시

```java
// Thread A
processPayment();

// Thread B
sendNotification();
```

각 Thread는 서로 다른 코드 경로를 독립적으로 진행합니다.

---

## 4. Thread State

Thread는 다음 실행 상태를 가질 수 있습니다.

| 상태 | 설명 |
|------|------|
| Running | CPU에서 실행 중 |
| Ready | 실행 대기 (CPU 배정 기다림) |
| Waiting | 특정 이벤트 대기 |
| Blocked | Lock 또는 I/O 대기 |
| Terminated | 종료 상태 |

---

## 5. Kernel Scheduler와 Context Switching

### Scheduler의 역할

운영체제 Scheduler는 Software Thread를 Logical CPU에 배치합니다.

```
Software Thread
      ↓
Kernel Scheduler
      ↓
  Logical CPU
```

### Context Switching

Scheduler가 CPU 실행 대상 Thread를 교체하는 과정입니다.

**저장/복원 대상 상태:**

| 상태 | 설명 |
|------|------|
| Program Counter | 실행 위치 |
| Register State | CPU 상태 |
| Stack Pointer | Stack 위치 |
| Thread State | 실행 상태 |

**전환 흐름:**

```
Thread A 실행
    ↓
Context Save (Thread A 상태 저장)
    ↓
Context Restore (Thread B 상태 복원)
    ↓
Thread B 실행
```

---

## 6. Software Thread vs Hardware Thread

두 개념은 서로 다른 계층에 속합니다.

| 구분 | Software Thread | Hardware Thread |
|------|-----------------|-----------------|
| 관리 주체 | OS / Runtime | CPU 하드웨어 |
| 개념 계층 | 논리적 실행 흐름 | CPU 내부 실행 컨텍스트 |
| 구성 요소 | PC, Stack, Register Context, State | Register Set, PC, Hardware Context |

### 전체 계층 구조

```
Application Logic
      ↓
Software Thread
      ↓
Kernel Scheduler
      ↓
  Logical CPU
      ↓
Hardware Thread
      ↓
Physical Core
      ↓
ALU / Cache / Pipeline
```

---

## 7. Thread 구현 방식: User Thread vs Kernel Thread

### User Thread

런타임 또는 사용자 라이브러리가 관리하는 Thread입니다.

| 환경 | 예시 |
|------|------|
| JVM | Virtual Thread |
| Go | Goroutine |
| 기타 | Green Thread |

### Kernel Thread

운영체제가 직접 관리하는 Thread입니다. Linux의 pthread가 대표적입니다.

---

## 8. JVM Thread 모델

### 일반 Java Thread

```
Java Thread
    ↓
Native Thread (OS Thread)
    ↓
Kernel Scheduler
```

### Virtual Thread (JDK 21+)

많은 실행 흐름을 적은 수의 OS Thread 위에서 처리합니다.

```
Virtual Thread (다수)
    ↓
Carrier Thread (소수의 OS Thread)
    ↓
Kernel Scheduler
```

---

## 9. 멀티스레딩의 목적

| 목적 | 설명 |
|------|------|
| 동시 작업 처리 | Parallelism |
| 응답성 향상 | Responsiveness |
| Throughput 증가 | 처리량 향상 |
| 자원 활용 증가 | CPU Idle 감소 |

### Thread 동기화

Thread들은 Heap Memory를 공유하기 때문에 동기화가 필요합니다.

| 기법 | 설명 |
|------|------|
| `synchronized` | JVM Monitor |
| `Lock` | 명시적 락 |
| `volatile` | 메모리 가시성 |
| CAS | Lock-Free 연산 |

---

## 10. 서버 애플리케이션 관점

### Thread Pool

과도한 Thread 수는 다음 문제를 유발합니다.

- Context Switching 증가
- Cache Miss 증가
- Scheduler Overhead 증가

### Non-Blocking Architecture

WebFlux, Netty 같은 구조는 **적은 수의 Thread로 많은 연결**을 처리합니다.

| 특성 | 설명 |
|------|------|
| Event Loop | 단일 실행 흐름 유지 |
| Non-Blocking | I/O 대기 최소화 |
| Task Queue | 작업 흐름 제어 |

---

## 11. 핵심 정리

| 구성 요소 | 역할 |
|-----------|------|
| Software Thread | 독립 실행 흐름 |
| Program Counter | 현재 실행 위치 |
| Stack | 함수 호출 상태 |
| Register Context | CPU 실행 상태 |
| Scheduler | CPU 실행 배치 |
| Logical CPU | OS 실행 단위 |
| Hardware Thread | CPU 실행 컨텍스트 |
| Physical Core | 실제 연산 자원 |

### 결론

Software Thread는 **독립적인 실행 위치(PC)**, **독립적인 Stack**, **독립적인 실행 상태**를 가지며, 운영체제 Scheduler를 통해 CPU에서 실행됩니다.

```
Software Thread
      ↓
Kernel Scheduler
      ↓
  Logical CPU
      ↓
Hardware Thread
      ↓
Physical Core
```

이 구조는 JVM Thread 모델, Virtual Thread, Netty Event Loop, Kubernetes 기반 서버 아키텍처, 금융 시스템 저지연 처리, 대규모 트래픽 처리 환경의 핵심 기반 개념입니다.

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*