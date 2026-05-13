# Linux Kernel의 task_struct, LWP, PID/TID 구조

## 1. 핵심 전제: Linux Kernel의 실행 단위

Linux Kernel은 **Process**와 **Thread**를 별개의 개념으로 분리하여 관리하지 않는다.

Kernel 내부에서 모든 실행 단위는 **task**라는 단일 개념으로 표현되며, 이를 `task_struct` 구조체로 관리한다.

| 사용자 관점 | Kernel 관점 |
|------------|------------|
| Process | 독립적인 task |
| Thread | 자원을 공유하는 task |

---

## 2. task_struct

`task_struct`는 Linux Kernel이 실행 단위를 표현하는 핵심 구조체이다.

CFS(Completely Fair Scheduler)는 이 `task_struct` 단위를 기준으로 CPU 시간을 할당한다.

### 주요 포함 정보

| 항목 | 설명 |
|------|------|
| Register Context | CPU 실행 상태 |
| Scheduling State | Runnable / Waiting 등 |
| Stack Pointer | 현재 Stack 위치 |
| Memory Mapping | 프로세스 주소 공간 정보 |
| File Descriptor | 열린 파일 정보 |
| PID / TID | 식별자 |
| Priority | 스케줄링 우선순위 |
| CPU Affinity | 실행 가능한 CPU 정보 |

---

## 3. Process와 Thread의 차이

Kernel 관점에서 Process와 Thread는 모두 `task_struct`이다. 차이는 **무엇을 공유하느냐**에 있다.

### Process
- 독립적인 주소 공간을 가지는 실행 단위
- Heap, Stack, File Descriptor, Address Space 모두 독립

### Thread (LWP)
- 동일 Process 내에서 자원을 공유하는 실행 단위
- Heap, Code 영역, File Descriptor를 공유
- **Stack만 개별적으로 보유**

### Clone Flags로 구분

Linux Kernel은 `clone()` 시스템 콜의 플래그 조합으로 Process/Thread 동작을 결정한다.

| Clone Flag | 의미 |
|------------|------|
| `CLONE_VM` | 메모리(주소 공간) 공유 |
| `CLONE_FILES` | File Descriptor 공유 |
| `CLONE_FS` | 파일 시스템 정보 공유 |
| `CLONE_SIGHAND` | Signal Handler 공유 |

공유 플래그가 많을수록 Thread처럼, 없을수록 Process처럼 동작한다.

---

## 4. Thread의 실체: LWP (Lightweight Process)

Linux Kernel 수준에서 Thread는 **LWP(Lightweight Process)** 로 표현된다.

```
Thread = 자원을 공유하는 task_struct = LWP
```

Thread Switching이 Process Switching보다 빠른 이유도 여기에 있다.

| 전환 유형 | 비용 |
|----------|------|
| Thread Switch | Register 교체 중심 |
| Process Switch | Register + Address Space 교체 (TLB Flush 등 포함) |

동일 Process 내 Thread들은 주소 공간을 공유하므로 Page Table 변경이 불필요하다.

---

## 5. 계층별 실행 구조

```
Application Layer    →  Java Thread / Go Routine
Runtime Layer        →  JVM / Go Runtime
Library Layer        →  POSIX Thread (pthread)
OS Kernel Layer      →  LWP (task_struct)
Scheduler Layer      →  CFS
Hardware Layer       →  Logical CPU → Physical Core
```

### Java Thread의 실체화 경로

```
java.lang.Thread
  ↓
JVM Internal C++ Layer
  ↓
pthread_create()
  ↓
LWP (task_struct)
  ↓
CFS Scheduling
  ↓
Logical CPU
```

Java Thread는 직접 CPU에 올라가지 않는다. 최종적으로 Linux Kernel 내부에서 `task_struct(LWP)` 형태로 실체화된 후 CFS에 의해 스케줄링된다.

### pthread와 LWP의 관계

| 개념 | 역할 |
|------|------|
| `pthread` | User-space Thread API (LWP 생성 인터페이스) |
| `LWP` | Kernel Scheduling Entity |

---

## 6. PID와 TID의 구조

Linux에서 PID와 TID는 명확히 구분된다.

### TID (Thread ID)
- 각 `task_struct`가 가지는 **고유 식별자**
- 모든 Thread는 독립적인 TID를 가진다

### PID (실제로는 TGID: Thread Group ID)
- 사용자 공간에서 흔히 말하는 PID는 실제로 **TGID**이다
- 동일 Process 내의 모든 Thread는 동일한 TGID를 공유한다

### Main Thread와의 관계

Linux에서 **Main Thread의 TID = Process의 TGID(PID)** 이다.

```
Main Thread    →  TID = 1000,  TGID = 1000
Worker Thread  →  TID = 1001,  TGID = 1000
Worker Thread  →  TID = 1002,  TGID = 1000
```

---

## 7. top / ps에서의 Thread 표시 방식

Linux가 모든 실행 단위를 `task_struct`로 관리하기 때문에, 옵션에 따라 표시 방식이 달라진다.

| 명령 | 표시 기준 | 출력 예 |
|------|----------|---------|
| `top` / `ps` | TGID 기준으로 묶어서 표시 | `PID 1000 (Java Process)` |
| `top -H` / `ps -eLf` | 각 task_struct를 개별 표시 | `TID 1000, 1001, 1002 ...` |

---

## 8. Context Switch와 task_struct

Context Switch 시 Kernel은 `task_struct` 내부 상태를 저장하고 복구한다.

**저장 대상:**
- Register Set
- Program Counter
- Stack Pointer
- Scheduling State

```
Context Switch = task_struct 교체
```

---

## 9. SRE 관점의 분석 포인트

`task_struct` 구조를 이해해야 아래 현상을 정확히 진단할 수 있다.

| 현상 | 원인 |
|------|------|
| Load Average 증가 | Runnable task 수 증가 |
| CPU Saturation | task_struct 간 과도한 CPU 경쟁 |
| Context Switch 급증 | Thread 수 과다 |
| Kafka Lag | Consumer Thread CPU 부족 |
| Netty Latency | Event Loop Scheduling 지연 |
| CPU Throttling | CFS Quota 초과 |

---

## 10. Java Thread Dump와 Linux TID 연결 (CPU 병목 추적)

```
# 1단계: CPU 사용량이 높은 TID 확인
top -H -p <PID>

# 2단계: TID를 16진수로 변환
printf "%x\n" 12345
→ 3039

# 3단계: Java Thread Dump에서 해당 nid 검색
nid=0x3039

# 4단계: 어떤 Java Thread가 CPU를 점유하는지 확인
→ Kafka Consumer / Netty Event Loop / GC Thread / Worker Thread 등
```

---

## 11. 전체 요약

### 실행 단위 계층 매핑

| 계층 | 실행 단위 |
|------|----------|
| Application | Java Thread / Go Routine |
| Runtime | JVM / Go Runtime |
| Library | pthread |
| Kernel | task_struct (LWP) |
| Scheduler | CFS |
| Hardware | Logical CPU / Physical Core |

### 핵심 정리

```
Thread
  = 자원을 공유하는 Lightweight Process (LWP)
  = Kernel Task
  = task_struct
```

```
task_struct
  ↓ CFS Scheduling
  ↓ Logical CPU
  ↓ Physical Core
```

Application 수준의 Java Thread는 최종적으로 Linux Kernel 내부에서 `task_struct(LWP)` 형태로 실체화되어 실제 CPU 연산으로 연결된다.

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*