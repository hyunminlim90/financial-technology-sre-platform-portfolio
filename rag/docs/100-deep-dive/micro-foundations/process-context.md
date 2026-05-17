# Process Context
## 1. 프로세스 컨텍스트란 무엇인가

> 정독: 0회

프로세스 컨텍스트(Process Context)는:

> 특정 프로세스가 실행되기 위해 필요한 **현재 실행 상태 전체**

**"이 프로세스가 어디까지 실행됐고, 어떤 메모리와 자원을 사용 중이며, 다시 실행하려면 무엇을 복원해야 하는지 기록한 실행 스냅샷"**

프로세스는 CPU를 계속 독점하지 못합니다. 운영체제는 여러 프로세스를 짧은 시간 단위로 번갈아 실행하며, 이때 다음 과정이 필요합니다.

```
현재 프로세스 상태 저장
  ↓
다음 프로세스 상태 복원
  ↓
CPU 실행 재개
```

이 저장·복원의 대상이 바로 **프로세스 컨텍스트**입니다.

---

## 2. 시스템 어디에서 등장하는가

프로세스 컨텍스트는 운영체제 커널의 **프로세스 관리 영역**에서 등장합니다.

```
Program
  ↓
Process
  ↓
Process Context
  ↓
Scheduler
  ↓
CPU Execution
```

**실제 흐름:**

```
User Space Process
  ↓
System Call / Interrupt / Timer
  ↓
Kernel Scheduler
  ↓
Context Switch
  ↓
Next Process
```

**프로세스 컨텍스트가 포함하는 상태:**

- CPU Register State
- Program Counter
- Stack Pointer
- Virtual Memory Address Space
- Page Table 정보
- Process State
- PID
- Open File Descriptors
- Signal State
- Scheduling 정보
- 권한 / Credential 정보

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향 |
|------|------|
| CPU | Context Switch 시 레지스터 저장/복원, 스케줄링 비용 |
| Memory | Page Table, Address Space, Kernel Metadata 유지 |
| Cache | 프로세스 전환 시 Cache Locality 약화 |
| TLB | 주소 변환 캐시 무효화 가능 |
| Disk | 직접 영향은 작지만 Blocked 상태와 연계 |
| Network | 직접 영향은 작지만 Socket 대기 상태와 연계 |

> 프로세스가 많아질수록 CPU가 실제 업무보다 **상태 저장/복원에 시간을 더 쓸 수 있습니다.**

---

## 4. 왜 중요한가

프로세스 컨텍스트는 운영체제가 여러 프로그램을 **동시에 실행하는 것처럼 보이게 만드는 핵심 구조**입니다.

단일 CPU 코어에서도 여러 프로세스가 동시에 도는 것처럼 보이는 이유:

```
OS Scheduler
  +
Process Context 저장/복원
  +
빠른 Context Switch
```

프로세스 컨텍스트가 없다면 프로세스 A를 멈췄다가 나중에 **정확히 같은 지점에서 재개**할 수 없습니다.

**운영 관점의 핵심 판단 기준:**

- CPU가 바쁜가? 아니면 Context Switch가 과도한가?
- 프로세스가 계산 중인가? 아니면 IO 대기 중인가?
- Latency가 코드 때문인가? 아니면 스케줄링/커널 오버헤드 때문인가?

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. Context Switch 폭증

```
Runnable Task 증가
  ↓
Scheduler 개입 증가
  ↓
Context Switch 증가
  ↓
CPU Cache Locality 저하
  ↓
Latency 증가
```

### 5-2. Run Queue 증가

```
Run Queue 증가
  ↓
CPU 할당 대기
  ↓
응답 지연
```

### 5-3. IO 대기 프로세스 증가

```
Running
  ↓
Blocked / Sleeping
  ↓
IO 완료 후 Runnable
```

IO가 느려지면 프로세스 컨텍스트는 계속 대기 상태로 남습니다.

### 5-4. Cache / TLB 오염

프로세스 전환이 잦으면:

- CPU Cache Locality 저하
- TLB 효율 저하
- DRAM 접근 증가

CPU 사용률이 높지 않아도 지연이 커질 수 있습니다.

### 5-5. 시스템 콜 과다

프로그램이 커널 진입을 자주 하면:

```
User Mode → Kernel Mode → User Mode
```

전환이 많아지고, 커널 컨텍스트 처리 비용이 증가합니다.

---

## 6. 핵심 메커니즘 요약

### 6-1. 프로세스는 실행 상태를 가진다

| 개념 | 설명 |
|------|------|
| Program | 정적 명령어 파일 |
| Process | 실행 중인 프로그램 |
| Process Context | 실행 상태 전체 |

### 6-2. CPU 레지스터 상태가 핵심이다

CPU는 다음 정보를 보고 이어서 실행합니다.

- Program Counter
- Stack Pointer
- General Registers
- Status Register

이 값들이 저장되어야 멈춘 지점에서 정확히 다시 실행할 수 있습니다.

### 6-3. 메모리 주소 공간도 컨텍스트다

```
Virtual Address Space
  ↓
Page Table
  ↓
Physical Memory
```

어떤 페이지 테이블을 쓰는지도 프로세스 컨텍스트의 일부입니다.

### 6-4. 커널 메타데이터도 컨텍스트다

커널이 프로세스 관리를 위해 유지하는 정보: PID, State, Priority, File Descriptors, Signal, Credentials, Scheduling Info

### 6-5. Context Switch는 공짜가 아니다

```
Register 저장
  ↓
Kernel 상태 갱신
  ↓
Page Table / Address Space 전환
  ↓
Register 복원
  ↓
CPU Cache / TLB 영향
```

이 비용이 누적되면 성능 저하가 발생합니다.

---

## 7. Linux / Runtime / K8s 에서 관측 방법

### Linux

```bash
# 프로세스 목록
ps aux

# 프로세스 상태 확인
ps -eo pid,ppid,stat,comm
```

**프로세스 상태 코드:**

| 코드 | 의미 |
|------|------|
| `R` | Running / Runnable |
| `S` | Sleeping |
| `D` | Uninterruptible Sleep |
| `Z` | Zombie |

```bash
# Context Switch 확인 (cs: 초당 context switch, r: run queue, b: blocked)
vmstat 1

# 프로세스별 Context Switch
pidstat -w 1

# CPU 스케줄링 지연
pidstat -u 1

# 프로세스 상세 컨텍스트 (State, Threads, voluntary/nonvoluntary_ctxt_switches)
cat /proc/<PID>/status

# 메모리 맵
cat /proc/<PID>/maps

# 열린 파일 디스크립터
ls -l /proc/<PID>/fd

# 시스템 콜 추적
strace -p <PID>
```

### Runtime

특정 스택과 무관하게 런타임에서 관측할 항목:

| 항목 | 핵심 질문 |
|------|-----------|
| Thread Count / CPU Usage | 실행 중인가? |
| Blocking Time / Lock Wait | 대기 중인가? |
| Allocation Rate | 메모리를 자주 요청하는가? |
| Event Loop Delay | 커널에 자주 들어가는가? |
| Scheduler Delay | CPU를 받지 못하는가? |

### Kubernetes

```bash
# Pod 상태
kubectl get pod

# Pod 내부 프로세스 확인
kubectl exec -it <pod> -- ps aux

# 자원 사용량
kubectl top pod

# OOM / Restart / 상태 전이 확인
kubectl describe pod

# Node CPU 압박 확인
kubectl top node
```

**연결되는 대표 Pod 상태:**

| 상태 | 의미 |
|------|------|
| `CrashLoopBackOff` | 반복 비정상 종료 |
| `OOMKilled` | 메모리 초과로 강제 종료 |
| `Error` | 실행 오류 |
| `Running` | 정상 실행 중 |
| `Pending` | 스케줄링 대기 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*