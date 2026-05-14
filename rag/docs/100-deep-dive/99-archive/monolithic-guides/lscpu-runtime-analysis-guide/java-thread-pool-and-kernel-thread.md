# Java Thread, Thread Pool, Kernel Thread 실행 구조

---

## 1. 개요

Java Thread는 JVM 위에서 독립적으로 실행되는 프로그램 실행 흐름입니다.

`new Thread(...)`, Thread Pool의 Worker Thread, Kafka Consumer Thread, Netty EventLoop Thread 등은 모두 Java Thread 기반 실행 흐름입니다.

Java Thread가 실제 CPU에서 실행되기 위해서는 **운영체제의 Kernel Thread와 연결**되어야 합니다.

```
Java Thread
      ↓
OS Kernel Thread
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

## 2. Java Thread 생성과 실행 흐름

### 단계별 흐름

#### 1단계: Thread 객체 생성

```java
Thread thread = new Thread(task);
```

JVM Heap에 Java Thread 객체가 생성됩니다. 아직 실제 실행은 시작되지 않습니다.

#### 2단계: start() 호출

```java
thread.start();
```

JVM이 운영체제에 실제 실행 가능한 Thread 생성을 요청합니다.

#### 3단계: Kernel Thread와 1:1 매핑

현대 HotSpot JVM에서 일반 Java Thread는 OS Kernel Thread와 **1:1로 매핑**됩니다.

```
Java Thread 1  ↔  Kernel Thread 1
```

#### 4단계: CPU 실행

Kernel Scheduler가 Kernel Thread를 Logical CPU에 배치하면 실제 실행이 시작됩니다.

```
Kernel Thread → Kernel Scheduler → Logical CPU → Physical Core → ALU / LSU / Register / Cache
```

### Java Thread 주요 특징

| 항목 | 설명 |
|------|------|
| 실행 단위 | JVM 위의 독립 실행 흐름 |
| OS 매핑 | Kernel Thread와 1:1 매핑 |
| Stack | Thread마다 독립 Stack 보유 |
| Heap | 같은 Process 내 Thread들이 공유 |
| 비용 | 생성 / 삭제 / Context Switching 비용 존재 |

---

## 3. Stack과 Heap 구조

### 메모리 구조

```
JVM Process
├── Heap             ← 모든 Thread가 공유 (객체 저장)
├── Thread A Stack   ← Thread A 전용
├── Thread B Stack   ← Thread B 전용
└── Thread C Stack   ← Thread C 전용
```

### Thread Stack 저장 정보

| 정보 | 설명 |
|------|------|
| Method Call Frame | 메서드 호출 정보 |
| Local Variables | 지역 변수 |
| Return Address | 복귀 주소 |
| Stack Pointer | 현재 Stack 위치 |

Heap에 생성된 객체(`new Order()` 등)는 여러 Thread가 동시에 참조할 수 있습니다.

---

## 4. OS Kernel Thread

CPU는 Java Thread를 직접 실행하지 않습니다. **실제 CPU에 스케줄링되는 대상은 OS Kernel Thread**입니다.

### Kernel Thread의 역할

| 역할 | 설명 |
|------|------|
| CPU 스케줄링 대상 | Kernel Scheduler가 실행 순서를 결정 |
| Register 상태 보관 | Context Switch 시 CPU 상태 저장 |
| Stack 관리 | 실행 상태 유지 |
| CPU 점유 | Logical CPU에 배치되어 실행 |
| 시스템 호출 수행 | I/O, 파일, 네트워크 등 커널 기능 사용 |

### 1:1 매핑의 의미

```
Java Thread A ↔ Kernel Thread A
Java Thread B ↔ Kernel Thread B
Java Thread C ↔ Kernel Thread C
```

구현이 단순하고 OS Scheduler를 직접 활용할 수 있다는 장점이 있지만, Thread 수가 많아질수록 Kernel Thread 수도 비례하여 증가합니다.

---

## 5. Thread 과다 생성 문제

### new Thread 방식의 문제

```java
new Thread(() -> {
    processTask();
}).start();
```

작업마다 Thread를 생성하고 종료하는 이 방식은 다음 문제를 유발합니다.

| 문제 | 설명 |
|------|------|
| 생성/삭제 비용 | Thread 생성 시마다 JVM/OS 자원 필요 |
| 자원 고갈 | 요청 수 증가 시 Thread 폭증 |
| 최대 수 통제 불가 | Thread 수 예측 및 제한 어려움 |
| 운영 안정성 저하 | 부하 상황에서 시스템 전체 성능 저하 가능 |

### Thread 과다 시 공통 문제

| 문제 | 설명 |
|------|------|
| Context Switching 증가 | CPU가 실행 대상을 자주 교체 |
| Stack Memory 증가 | Thread마다 Stack 메모리 필요 |
| Scheduler Overhead 증가 | OS가 관리해야 할 실행 단위 증가 |
| Cache 효율 저하 | CPU 교체로 Cache Miss 증가 |
| Latency 증가 | 관리 비용이 실제 작업 비용을 초과 |

### Context Switching 흐름

```
Thread A 실행
      ↓
Register / PC / Stack Pointer 저장
      ↓
Thread B 상태 복원
      ↓
Thread B 실행
```

---

## 6. Thread Pool

Thread Pool은 미리 일정 개수의 Thread를 생성해 두고, 작업이 들어올 때마다 **기존 Thread를 재사용**하는 구조입니다.

```java
ExecutorService executor = Executors.newFixedThreadPool(10);

executor.submit(() -> {
    processTask();
});
```

### Thread Pool 구조

```
Thread Pool
├── Worker Thread 1
├── Worker Thread 2
├── Worker Thread 3
└── Task Queue
```

### 실행 흐름

```
Thread Pool 생성 → Worker Thread 생성 → Kernel Thread와 1:1 매핑 → 작업 대기
      ↓
Task Queue에서 작업 수신 → 작업 실행 → Thread 종료하지 않고 재사용
```

### new Thread vs Thread Pool 비교

| 구분 | new Thread | Thread Pool |
|------|-----------|-------------|
| 생성 방식 | 작업마다 생성 | 미리 생성 또는 제한적 생성 |
| 생명 주기 | 작업 완료 후 종료 | 작업 완료 후 대기 |
| Kernel Thread | 매번 생성/삭제 | 기존 Kernel Thread 재사용 |
| 비용 | 높음 | 낮음 |
| Thread 수 제어 | 어려움 | 가능 |
| 실무 적합성 | 낮음 | 높음 |

### Thread Pool의 장점

| 장점 | 설명 |
|------|------|
| 재사용성 | Thread 생성/삭제 비용 감소 |
| 자원 제한 | 최대 Thread 수 제어 가능 |
| 안정성 | 과도한 Thread 증가 방지 |
| 처리량 향상 | 작업 대기열 기반 처리 |
| 운영 예측성 | 부하 상황에서 동작 예측 가능 |

---

## 7. Thread Pool 크기 설계

Thread Pool 크기 100 설정은 곧 Kernel Thread를 최대 100개까지 사용하겠다는 의미입니다.

```
Thread Pool Size 100  ≈  Java Worker Thread 100  ≈  Kernel Thread 100
```

Physical Core 수보다 지나치게 큰 Thread Pool은 Context Switching과 Scheduler Overhead를 증가시킵니다.

### 작업 특성별 Thread 수 기준

| 작업 유형 | 특성 | Thread 수 기준 |
|-----------|------|----------------|
| CPU Bound | ALU/FPU 지속 사용 (암호화, 압축, 대량 계산) | Physical Core 또는 Logical CPU 수 근접 |
| I/O Bound | 외부 대기 많음 (DB, API, 파일 I/O) | CPU Bound보다 더 많은 Thread 가능 |

CPU Bound 작업에서 Thread 수가 Core 수를 크게 초과하면 Context Switching 비용으로 오히려 성능이 저하됩니다.

---

## 8. Java Thread 계층과 특수 목적 Thread

모든 특수 목적 Thread는 `java.lang.Thread` 기반입니다.

```
java.lang.Thread
├── Worker Thread          (Thread Pool 작업 실행)
├── Netty EventLoop Thread (Non-Blocking Network I/O)
├── Kafka Consumer Thread  (Poll Loop + Message Processing)
├── Scheduler Thread       (주기 작업)
└── JVM GC Thread          (Heap 메모리 관리)
```

### 특수 목적 Thread별 특성

| Thread 종류 | 실행 방식 | 주요 주의점 |
|-------------|-----------|-------------|
| Worker Thread | Task Queue 기반 | Pool 크기, Queue 크기 |
| Netty EventLoop Thread | Event-Driven, Non-Blocking | Blocking 작업 금지 |
| Kafka Consumer Thread | Poll Loop | Poll 지연 → Rebalance |
| GC Thread | JVM 내부 시스템 | STW로 전체 Thread 정지 가능 |

GC Stop-the-World 발생 시 일반 Java Thread, Netty EventLoop Thread, Kafka Consumer Thread 모두 일시 정지될 수 있습니다.

---

## 9. CPU 내부 실행 흐름

Thread가 CPU에 배치되면 다음 하드웨어 자원을 사용합니다.

| 구성 요소 | 역할 |
|-----------|------|
| Register | 현재 실행 데이터 저장 |
| ALU / FPU | 연산 수행 |
| LSU | 메모리 Load / Store |
| L1/L2/L3 Cache | 고속 데이터 접근 |
| Pipeline | 명령어 단계별 실행 |
| Branch Predictor | 분기 예측 |

---

## 10. Thread 상태

| 상태 | 설명 |
|------|------|
| NEW | 생성되었지만 아직 시작되지 않음 |
| RUNNABLE | 실행 중 또는 CPU 대기 |
| BLOCKED | Monitor Lock 대기 |
| WAITING | 무기한 대기 |
| TIMED_WAITING | 시간 제한 대기 |
| TERMINATED | 실행 종료 |

---

## 11. 운영(SRE) 관점

### 주요 모니터링 지표

| 지표 | 의미 |
|------|------|
| Thread Count | JVM Thread 수 |
| Runnable Thread 수 | CPU 실행 대기 Thread |
| Context Switch | Thread 전환 비용 |
| CPU Usage | 전체 CPU 사용률 |
| Load Average | 실행 대기 작업 수 |
| GC Pause | JVM 정지 시간 |
| Queue Length | Thread Pool 작업 대기열 |
| Rejected Task Count | Thread Pool 포화 여부 |

### 주요 장애 패턴

| 패턴 | 원인 | 결과 |
|------|------|------|
| Thread 폭증 | new Thread 반복 생성 | Memory / Scheduler Overhead 증가 → 서비스 지연 |
| Thread Pool 포화 | 작업 유입 속도 > 처리 속도 | Queue 증가 → 응답 지연 → RejectedExecutionException |
| Context Switching 증가 | Thread 수 과다 | CPU Core 경쟁 → 실제 처리량 감소 |
| Blocking 작업 혼재 | CPU 작업과 Blocking I/O가 같은 Pool 사용 | 중요 작업 지연 → Tail Latency 증가 |

### 운영 설계 원칙

| 원칙 | 설명 |
|------|------|
| new Thread 직접 생성 지양 | Thread 폭증 방지 |
| Thread Pool 사용 | Thread 재사용 및 자원 제한 |
| CPU Bound와 I/O Bound 분리 | 병목 전파 방지 |
| Pool 크기 제한 | Kernel Thread 과다 생성 방지 |
| Queue 크기 관리 | 무한 대기열 방지 |
| Rejection Policy 정의 | 포화 시 동작 명확화 |
| Thread Dump 분석 | Deadlock / Blocking / Starvation 확인 |
| GC 영향 함께 관찰 | STW로 인한 Thread 정지 확인 |

### Thread Dump 확인 항목

| 항목 | 설명 |
|------|------|
| Thread Name | Thread 식별 |
| Thread State | RUNNABLE, WAITING, BLOCKED 등 |
| Stack Trace | 현재 실행 위치 |
| Lock 정보 | Deadlock / Contention 확인 |
| Pool 이름 | 소속 Thread Pool 확인 |

---

## 12. 전체 실행 계층

```
Application Logic
        ↓
Java Thread / Worker Thread / EventLoop / Consumer Thread
        ↓
       JVM
        ↓
 OS Kernel Thread
        ↓
  Kernel Scheduler
        ↓
    Logical CPU
        ↓
  Hardware Thread
        ↓
   Physical Core
        ↓
ALU / LSU / Register / Cache / Pipeline
```

---

## 13. 핵심 정리

| 구성 요소 | 설명 |
|-----------|------|
| Java Thread | JVM의 기본 실행 흐름 |
| new Thread | 작업마다 Thread 직접 생성 |
| Thread Pool | Thread를 재사용하는 실행 구조 |
| Worker Thread | Thread Pool 내부 작업 실행 Thread |
| Kernel Thread | OS가 관리하는 실제 CPU 스케줄링 단위 |
| 1:1 Mapping | Java Thread 하나가 Kernel Thread 하나와 연결 |
| Context Switch | CPU 실행 Thread 교체 |
| Stack | Thread별 독립 메모리 |
| Heap | JVM Thread들이 공유하는 객체 저장 영역 |

### 결론

```
Java Thread = JVM 실행 흐름 + OS Kernel Thread와 1:1 매핑 + CPU 스케줄링 대상

Thread Pool = Worker Thread 재사용 + Kernel Thread 재사용 + 자원 사용량 제한 + 운영 안정성 향상
```

실무 Java 서버에서는 `new Thread(...)` 직접 생성보다, **목적별 Thread Pool을 구성**하고 CPU Bound / I/O Bound / Network I/O / Kafka Consumer / GC 영향을 분리하여 관찰하는 것이 중요합니다.

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*