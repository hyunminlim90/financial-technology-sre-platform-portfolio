# Control Flow
## 1. 제어 흐름이란 무엇인가

제어 흐름(Control Flow)은:

> 프로그램이 실행될 때 **명령어가 어떤 순서로 실행되는지**를 의미

**"CPU가 다음에 어떤 명령어를 실행할지 결정되는 실행 경로"**

소스 코드에서는 `if`, `for`, `while`, `function call`, `return`, `exception`, `interrupt` 형태로 나타납니다.

| 관점 | 설명 |
|------|------|
| 추상적 | 코드의 실행 순서 |
| 물리적 | CPU가 다음에 가져올 명령어 주소(Program Counter)의 변화 |

---

## 2. 시스템 어디에서 등장하는가

제어 흐름은 프로그램 실행 전체에서 등장합니다.

```
Executable File
  ↓
Main Memory
  ↓
Process
  ↓
CPU Fetch / Decode / Execute
  ↓
Control Flow
```

**CPU 내부 흐름:**

```
Program Counter
  ↓
Instruction Fetch
  ↓
Decode
  ↓
Execute
  ↓
Next Program Counter
```

**운영체제 관점에서 제어 흐름이 바뀌는 상황:**

- System Call
- Interrupt
- Context Switch
- Signal
- Exception
- Scheduler Dispatch

> 제어 흐름은 애플리케이션 코드 안에만 있는 것이 아니라, **사용자 공간과 커널 공간을 오가는 실행 경로 전체**에 존재합니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향 |
|------|------|
| CPU | 분기, 함수 호출, 예외 처리, 파이프라인 흐름 |
| Memory | 명령어 fetch, stack frame, branch target 접근 |
| Cache | instruction cache, branch prediction 효율 |
| Disk | 직접 영향은 작음 |
| Network | 직접 영향은 작지만 이벤트 처리 흐름과 연계 |

```
제어 흐름이 예측 가능  →  CPU 빠르게 실행
제어 흐름이 자주 흔들림  →  CPU 파이프라인 효율 저하
```

조건 분기와 함수 호출이 많거나 예측이 어려우면 **Branch Misprediction, Instruction Cache Miss, Pipeline Flush**가 발생할 수 있습니다.

---

## 4. 왜 중요한가

```
Data + Control Flow = Program Execution
```

데이터가 아무리 있어도 CPU가 어떤 순서로 명령어를 실행할지 정해지지 않으면 프로그램은 동작하지 않습니다.

**운영 관점에서 제어 흐름으로 구분할 수 있는 것들:**

- 정상 경로인가? 예외 경로인가?
- 재시도 루프인가? 무한 루프인가?
- 블로킹 대기인가?
- 커널 진입이 많은가?
- 스케줄러에 의해 중단된 것인가?

> 장애 분석에서 제어 흐름은 **"프로그램이 지금 어디서 멈췄는가"**, **"왜 다음 단계로 가지 못하는가"** 를 추적하는 기준입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. 무한 루프

```
Loop Condition 오류
  ↓
Control Flow가 빠져나오지 못함
  ↓
CPU Usage 증가
  ↓
Latency 증가
```

### 5-2. Deadlock / Blocking

```
Thread A waits + Thread B waits
  ↓
Control Flow 정지
  ↓
처리량 감소
```

### 5-3. 예외 경로 폭증

```
Error
  ↓
Exception Path
  ↓
Logging / Rollback / Retry
  ↓
Latency 증가
```

### 5-4. 재시도 루프

```
Request Fail
  ↓
Retry → Fail → Retry Storm
  ↓
CPU / Network / Downstream 부하 증가
```

### 5-5. Branch Misprediction

```
Wrong Branch Prediction
  ↓
Pipeline Flush
  ↓
CPU Cycle 낭비
```

### 5-6. Context Switch에 의한 흐름 중단

```
Running
  ↓
Preempted
  ↓
Runnable
  ↓
Scheduled Again
```

---

## 6. 핵심 메커니즘 요약

### 6-1. 제어 흐름은 Program Counter가 이끈다

```
PC → Instruction Address → Fetch → Execute → Next PC
```

### 6-2. 기본은 순차 실행이다

```
Instruction N → N+1 → N+2
```

### 6-3. 분기는 PC 값을 바꾼다

조건문, 반복문, 함수 호출은 결국 PC를 다른 주소로 바꾸는 행위입니다.

```
if condition true  →  PC = branch target
```

### 6-4. 조건 분기는 상태 레지스터와 연결된다

```
Compare
  ↓
Status Register / Flags
  ↓
Conditional Branch
  ↓
Next PC 결정
```

### 6-5. 함수 호출은 Stack과 연결된다

```
call function
  ↓
return address 저장
  ↓
function 실행
  ↓
return → 이전 위치로 복귀
```

### 6-6. 커널도 제어 흐름을 바꾼다

```
User Mode
  ↓
System Call / Interrupt / Exception
  ↓
Kernel Mode
  ↓
Return to User Mode
```

### 6-7. 스케줄러는 프로세스의 제어 흐름을 멈추고 재개한다

```
Process A 실행
  ↓
Context Save
  ↓
Process B 실행
  ↓
Context Restore
  ↓
Process A 재개
```

---

## 7. Linux / Runtime / K8s 에서 관측 방법

### Linux

```bash
# 실행 중인 프로세스 확인
ps aux

# 프로세스 상태 확인
ps -eo pid,ppid,stat,comm

# 시스템 콜 흐름 추적 (read, write, futex, epoll_wait, connect 등)
strace -p <PID>

# CPU 샘플링
perf top
perf record -p <PID> && perf report

# 함수 호출 흐름 추적
gdb -p <PID>
# → backtrace

# 스케줄링 / 문맥 교환 (cs: context switches, r: run queue)
vmstat 1
```

**프로세스 상태 코드:**

| 코드 | 의미 |
|------|------|
| `R` | Running / Runnable |
| `S` | Sleeping |
| `D` | Uninterruptible Sleep |
| `Z` | Zombie |

### Runtime

| 지표 | 핵심 질문 |
|------|-----------|
| Call Stack / Stack Trace | 어떤 함수 호출에서 멈췄는가? |
| Thread State / Blocking Time | 어디서 기다리고 있는가? |
| Error Path / Retry Count | 어떤 분기가 반복되고 있는가? |
| Event Loop Delay / Lock Wait | 현재 실행 경로가 정상 경로인가? |

### Kubernetes

```bash
# Pod 실행 상태
kubectl get pod

# 로그로 실행 경로 확인
kubectl logs <pod>

# 컨테이너 내부 프로세스 상태
kubectl exec -it <pod> -- ps aux

# 반복 재시작 / 실행 명령 확인 (Command, Args, Entrypoint, Exit Code, Restart Count)
kubectl describe pod
```

**대표 Pod 상태:**

| 상태 | 의미 |
|------|------|
| `CrashLoopBackOff` | 반복 비정상 종료 |
| `OOMKilled` | 메모리 초과로 강제 종료 |
| `Error` | 실행 오류 |
| `Completed` | 정상 종료 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*