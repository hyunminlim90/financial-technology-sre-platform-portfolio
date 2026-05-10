# Go Routine과 Go Runtime Scheduler 구조

## 1. Go Routine 개요

Go Routine은 Go Runtime이 관리하는 **경량 실행 흐름**입니다. OS Kernel Thread 자체가 아니라, Go Runtime 내부에서 생성되고 관리되는 사용자 수준의 실행 단위입니다.

```go
go processTask()
```

이 코드는 OS Kernel Thread를 직접 새로 만드는 것이 아니라, Go Runtime 내부에 실행할 작업 단위를 등록합니다.

```
Go Routine = Go Runtime이 관리하는 경량 실행 흐름
```

### 계층적 위치

```
Go Application
      ↓
Go Routine
      ↓
Go Runtime Scheduler
      ↓
   OS Thread
      ↓
Kernel Scheduler
      ↓
  Logical CPU
      ↓
Physical Core
```

Go Routine은 JVM 기술이 아닌 **Go 언어와 Go Runtime** 환경에 속하는 개념입니다.

---

## 2. Go Routine의 구성 요소

| 구성 요소 | 설명 |
|-----------|------|
| 실행할 함수 | 실행 대상 함수 주소 |
| 실행 상태 | Running, Waiting 등 |
| Stack | 초기에는 작은 Stack으로 시작, 필요 시 동적 확장 |
| Scheduling Metadata | Runtime Scheduler가 관리하는 정보 |
| 대기 정보 | Channel, Mutex, I/O 대기 상태 등 |

---

## 3. Java Thread와의 핵심 차이: M:N 매핑

### Java Platform Thread: 1:1 매핑

```
Java Thread 1 ↔ OS Thread 1
Java Thread 2 ↔ OS Thread 2
Java Thread 3 ↔ OS Thread 3
```

### Go Routine: M:N 매핑

많은 Go Routine을 적은 수의 OS Thread 위에서 실행합니다.

```
100,000 Go Routines
        ↓
  Go Runtime Scheduler
        ↓
    8 OS Threads
        ↓
    8 Logical CPUs
```

Go Runtime Scheduler가 중간에서 Go Routine을 OS Thread에 다중화합니다.

---

## 4. Go Runtime Scheduler: G-P-M 모델

Go Scheduler는 G(Goroutine), P(Processor), M(Machine) 세 요소로 구성됩니다.

| 구성 요소 | 의미 | 역할 |
|-----------|------|------|
| G | Goroutine | 실행해야 할 경량 실행 흐름 |
| P | Processor | G를 실행하기 위한 Runtime 스케줄링 컨텍스트 (Runnable G Queue 보유) |
| M | Machine | 실제 OS Thread |

### 실행 구조

```
Runnable Goroutines
        ↓
P Local Run Queue
        ↓
  M (OS Thread)
        ↓
Kernel Scheduler
        ↓
  Logical CPU
        ↓
 Physical Core
```

M은 P를 보유해야 Go Routine을 실행할 수 있습니다.

### GOMAXPROCS

동시에 Go 코드를 실행할 수 있는 **P의 개수**를 결정합니다. 보통 Logical CPU 수를 기준으로 설정됩니다.

```
GOMAXPROCS=8  →  최대 8개의 P 사용
```

---

## 5. Go Routine이 가벼운 이유

### Java Thread vs Go Routine 비교

| 항목 | Java Thread | Go Routine |
|------|-------------|------------|
| 생성 주체 | JVM + OS | Go Runtime |
| OS Thread 생성 | 필요 (1:1) | 일반적으로 불필요 (M:N) |
| Stack | 상대적으로 크고 고정 | 작게 시작 후 동적 확장 |
| Scheduling | OS Kernel 중심 | Go Runtime 중심 |
| Context Switch | Kernel 개입 필요 | Runtime 내부 전환 가능 |
| 생성 비용 | 높음 | 낮음 |

### Context Switch 비교

```
Java Thread 전환:
Thread A → Kernel Context Switch → Thread B

Go Routine 전환:
Goroutine A → Runtime Scheduling → Goroutine B
```

Go Routine 전환은 많은 경우 OS Kernel 수준 Context Switch보다 비용이 낮습니다.

---

## 6. 주요 Runtime 기능

### Blocking 처리

Go Runtime은 Blocking 상황을 감지하고 다른 Go Routine이 계속 실행될 수 있도록 조정합니다.

```
Goroutine A
    ↓
Blocking I/O 또는 System Call
    ↓
M이 차단될 수 있음
    ↓
Runtime이 다른 M/P 조합으로 다른 Goroutine 실행
```

OS Thread가 차단되더라도 전체 Go Routine 실행이 멈추지 않도록 설계되어 있습니다.

### Network Poller

Go Runtime은 네트워크 I/O를 효율적으로 처리하기 위해 Network Poller를 사용합니다.

```
Goroutine → Network I/O 대기 → Runtime Netpoller 등록
                                        ↓
                              다른 Goroutine 실행
                                        ↓
                           I/O 준비 완료 시 해당 Goroutine 재개
```

적은 수의 OS Thread로 많은 네트워크 연결을 처리할 수 있는 이유입니다.

### Work Stealing

각 P는 Local Run Queue를 가집니다. 실행할 Go Routine이 없는 P는 다른 P의 Queue에서 Go Routine을 가져와 실행합니다.

```
P1 Queue: G1, G2, G3
P2 Queue: empty
→ P2가 P1에서 일부 G를 가져와 실행
```

이를 통해 CPU 활용률을 높입니다.

---

## 7. Go Routine 과다 생성 시 주의점

Go Routine은 가볍지만, 무제한 생성해도 안전하다는 의미는 아닙니다.

| 문제 | 설명 |
|------|------|
| 메모리 증가 | 각 Go Routine도 Stack과 Metadata 필요 |
| Scheduling Overhead | Runtime이 관리할 G 수 증가 |
| Channel 대기 누적 | 대기 중인 실행 흐름 증가 |
| GC 부담 | Stack 및 참조 스캔 대상 증가 |
| Backpressure 부족 | 작업 유입 제어 실패 |

---

## 8. Java Thread, Virtual Thread, Go Routine 비교

| 항목 | Java Platform Thread | Java Virtual Thread | Go Routine |
|------|---------------------|---------------------|------------|
| Runtime | JVM | JVM | Go Runtime |
| OS Thread 매핑 | 1:1 | M:N 유사 구조 | M:N |
| OS Thread | Thread마다 필요 | Carrier Thread 공유 | M 공유 |
| 생성 비용 | 높음 | 낮음 | 낮음 |
| 스케줄러 | OS Kernel 중심 | JVM 중심 | Go Runtime 중심 |
| 주 사용 목적 | 일반 Thread 실행 | 대량 동시성 | 대량 동시성 |

### 실행 계층 비교

```
Java Platform Thread        Java Virtual Thread         Go Routine

  Java Thread                Virtual Thread              Goroutine
       ↓                           ↓                        ↓
      JVM                    JVM Scheduler            Go Runtime Scheduler
       ↓                           ↓                        ↓
   OS Thread                 Carrier Thread              OS Thread
       ↓                           ↓                        ↓
Kernel Scheduler            OS Thread                Kernel Scheduler
       ↓                           ↓                        ↓
      CPU                   Kernel Scheduler               CPU
                                   ↓
                                  CPU
```

Java Virtual Thread는 Go Routine과 유사하게 런타임이 많은 경량 실행 흐름을 적은 수의 Carrier Thread 위에서 관리하는 방향으로 발전했습니다.

---

## 9. 운영(SRE) 관점

### Java Platform Thread vs Go Routine 관찰 비교

| 관점 | Java Platform Thread | Go Routine |
|------|---------------------|------------|
| 실행 흐름 수 | Thread Count | Goroutine Count |
| OS Thread 수 | Thread 수와 유사 | Runtime이 제한적으로 관리 |
| 주요 병목 | Context Switch, Thread 수 | Blocking, Scheduler 지연, GC, Channel 대기 |
| CPU 실행 | Kernel Scheduler 중심 | Runtime Scheduler + Kernel Scheduler |

> Go Routine이 많다고 해서 OS Thread가 같은 수만큼 생성되는 것이 아닙니다. Java Platform Thread와 동일한 기준으로 관찰하면 안 됩니다.

### 주요 모니터링 지표

| 지표 | 의미 |
|------|------|
| Goroutine Count | 현재 Go Routine 수 |
| OS Thread Count | Runtime이 사용하는 OS Thread 수 |
| GOMAXPROCS | 동시에 실행 가능한 P 수 |
| Scheduler Latency | Runtime Scheduling 지연 |
| GC Pause | Go GC 정지 시간 |
| Block Profile | Blocking 지점 |
| Mutex Profile | Lock 경합 |
| CPU Profile | CPU 사용 위치 |

---

## 10. 전체 계층 구조

```
Go Application
      ↓
Goroutine 생성
      ↓
Go Runtime Scheduler
      ↓
  G-P-M Model
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
      ↓
ALU / LSU / Register / Cache / Pipeline
```

---

## 11. 핵심 정리

| 구성 요소 | 역할 |
|-----------|------|
| Go Routine | Go Runtime이 관리하는 경량 실행 흐름 |
| Go Runtime | Go 실행 흐름, GC, Scheduler 관리 |
| G | Goroutine 실행 단위 |
| P | Scheduling Context (Runnable G Queue 보유) |
| M | OS Thread |
| M:N Mapping | 많은 Goroutine을 적은 OS Thread에 매핑 |
| Work Stealing | 실행 가능한 작업을 다른 P Queue에서 가져오는 방식 |
| GOMAXPROCS | 동시에 Go 코드를 실행할 수 있는 P 수 |
| Netpoller | Network I/O 대기 관리 |

### 결론

```
Goroutine = Go Runtime 내부 실행 단위 + M:N Scheduler 대상 + OS Thread보다 가벼운 실행 흐름
```

Go Routine은 OS Thread를 직접 대량 생성하지 않고, Go Runtime Scheduler가 많은 Go Routine을 적은 수의 OS Thread 위에서 실행하도록 관리합니다.

```
Many Goroutines → Go Runtime Scheduler → Few OS Threads → Kernel Scheduler → Physical CPU
```

Go Routine은 Java Thread의 하위 개념이 아니라, **Software Thread 모델을 비교하기 위한 별도 Runtime 계열의 대표 사례**입니다. 전통적인 Java Platform Thread의 1:1 모델, 최신 Java Virtual Thread의 M:N 유사 구조, Go Routine의 M:N 구조는 Software Thread 모델의 발전 방향을 보여주는 비교 기준입니다.

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*