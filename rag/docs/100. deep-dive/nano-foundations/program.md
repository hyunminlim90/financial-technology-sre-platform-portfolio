# Program
## 1. 프로그램이란 무엇인가

프로그램(Program)은:

> 특정 목적을 달성하기 위해 미리 정해 둔 **명령어, 규칙, 절차의 정적 집합**

**"컴퓨터가 어떤 순서로 무엇을 해야 하는지 적어 둔 실행 계획"**

프로그램은 컴퓨터에만 한정되지 않습니다.

- 행사 진행 순서 / 방송 편성표
- 기계 동작 절차 / 세탁기 코스
- 컴퓨터 실행 코드

모두 넓은 의미의 프로그램입니다.

**컴퓨터 시스템에서의 구체화:**

```
Source Code
  ↓
Executable / Script / Bytecode
  ↓
Program
  ↓
Process
```

| 개념 | 설명 |
|------|------|
| **Program** | 실행 전 정적 계획 |
| **Process** | 실행 중인 동적 상태 |

<details>
<summary>Deep Dive</summary></br>

Instruction(명령어) [[M]](../../100-deep-dive/micro-foundations/instruction.md)  
Constraints(제약 조건) [[M]](../../100-deep-dive/micro-foundations/constraints.md)  
Control Flow(제어 흐름) [[M]](../../100-deep-dive/micro-foundations/control-flow.md)  
Executable File(실행 파일) [[M]](../../100-deep-dive/micro-foundations/executable-file.md)

</details></br>

## 2. 시스템 어디에서 등장하는가

프로그램은 시스템의 목적을 정의하는 **소프트웨어 계층**에서 등장합니다.

```
User Goal
  ↓
Program
  ↓
Operating System
  ↓
Hardware
```

**컴퓨터 내부 실행 흐름:**

```
Program on Storage
  ↓
Loader
  ↓
Main Memory
  ↓
Process
  ↓
CPU Execution
```

프로그램은 실행 전에는 디스크나 이미지 내부에 정적으로 존재하고, 실행 시 운영체제에 의해 메모리에 적재됩니다.

```
Storage → Main Memory → CPU Cache → CPU Register
```

---

## 3. 어떤 자원에 가장 영향이 큰가

프로그램은 실행되기 전에는 자원을 거의 사용하지 않습니다. 하지만 실행되어 프로세스가 되면 **모든 주요 자원 사용 패턴을 결정**합니다.

| 자원 | 영향 |
|------|------|
| CPU | 명령어 실행, 연산량, 분기 흐름 |
| Memory | 코드, 데이터, 힙, 스택 적재 |
| Disk | 파일 읽기/쓰기, 실행 파일 로딩 |
| Network | 외부 통신, 요청/응답 처리 |
| Cache | 명령어/데이터 접근 패턴 |

> **프로그램의 구조가 시스템 자원 사용 패턴을 결정합니다.**

| 프로그램 유형 | 주요 자원 |
|--------------|-----------|
| 연산 중심 | CPU 사용량 증가 |
| 대용량 데이터 처리 | Memory 사용량 증가 |
| 파일 처리 | Disk IO 증가 |
| 통신 중심 | Network 사용량 증가 |

---

## 4. 왜 중요한가

프로그램은 하드웨어를 **목적 있는 시스템으로 바꾸는 설계도**입니다.

프로그램이 있어야 다음과 같은 의미 있는 흐름이 만들어집니다.

```
입력 → 처리 → 저장 → 출력
```

**운영 관점에서 프로그램 이해가 필요한 판단 기준:**

- 코드 경로 문제인가? 실행 파일 문제인가?
- 프로세스 상태 문제인가? 자원 제한 문제인가?
- OS / 커널 문제인가? 배포된 버전 문제인가?

> 프로그램은 장애 분석에서 **"무엇을 하도록 설계되었는가"** 를 확인하는 출발점입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. 잘못된 프로그램 로직

```
잘못된 조건
  ↓
잘못된 제어 흐름
  ↓
오류 응답 / 무한 루프 / 데이터 불일치
```

### 5-2. 무한 루프

```
Loop 탈출 실패
  ↓
CPU Saturation
  ↓
응답 지연
```

### 5-3. 메모리 누수

```
Allocation 증가
  ↓
Memory Pressure
  ↓
OOM Kill
```

### 5-4. 잘못된 실행 파일 또는 버전

```
Wrong Binary / Wrong Config / Wrong Version
  ↓
Runtime Failure
```

### 5-5. 외부 자원 대기

```
Program calls external resource
  ↓
Wait / Block
  ↓
Latency 증가
```

### 5-6. 예외 경로 폭증

```
Error 발생
  ↓
Retry / Rollback / Logging
  ↓
CPU / IO / Network 부하 증가
```

---

## 6. 핵심 메커니즘 요약

### 6-1. 프로그램은 실행 전 정적 계획이다

```
Program = Instructions + Rules + Data
```

실행되기 전에는 단순히 저장된 설계도입니다.

### 6-2. 컴퓨터 프로그램은 실행 가능한 형태로 저장된다

Executable File, Script, Bytecode, Library — OS 또는 Runtime이 **해석하거나 실행할 수 있는 형식**이어야 합니다.

### 6-3. 실행되면 프로세스가 된다

```
Program → Process
```

프로세스가 되면 PID, Virtual Memory, File Descriptor, Register State, Stack, Heap, Scheduling State가 생성됩니다.

### 6-4. 프로그램의 실행 순서는 제어 흐름이다

```
Instruction 1 → Instruction 2 → Branch / Call / Return → Next Instruction
```

하드웨어 레벨에서는 **Program Counter**가 이 흐름을 이끕니다.

### 6-5. 프로그램은 OS를 통해 하드웨어를 사용한다

```
Program
  ↓
System Call
  ↓
Kernel
  ↓
Hardware
```

예시: 파일 읽기, 네트워크 송신, 메모리 할당, 프로세스 생성

### 6-6. 프로그램 문제는 실행 상태에서 드러난다

```
Program 설계
  ↓
Process 실행
  ↓
CPU / Memory / Disk / Network 사용
  ↓
장애 또는 정상 처리
```

---

## 7. Linux / Runtime / K8s 에서 관측 방법

### Linux

```bash
# 프로그램 파일 확인
ls -l ./app

# 파일 타입 확인
file ./app

# 실행 권한 부여
chmod +x ./app

# 실행 중 프로세스 확인
ps aux

# 원본 실행 파일 확인
readlink /proc/<PID>/exe

# 실행 인자 확인
cat /proc/<PID>/cmdline

# 환경 변수 확인
cat /proc/<PID>/environ

# 시스템 콜 확인
strace -p <PID>
```

### Runtime

| 지표 | 핵심 질문 |
|------|-----------|
| Exit Code / Logs | 어디서 실패했는가? |
| Error Path / Stack Trace | 어떤 경로로 실행되었는가? |
| Thread State / Memory Usage | 현재 어떤 상태인가? |
| CPU Usage / Open File Descriptor | 자원을 얼마나 소비하는가? |
| Configuration | 어떤 입력을 받았는가? |

### Kubernetes

```bash
# Pod 실행 상태
kubectl get pod

# 실행 명령 확인 (Image, Command, Args, Entrypoint, Restart Count, Exit Code)
kubectl describe pod

# 로그 확인
kubectl logs <pod>

# 컨테이너 내부 프로세스 확인
kubectl exec -it <pod> -- ps aux
```

**대표 Pod 상태:**

| 상태 | 의미 |
|------|------|
| `CrashLoopBackOff` | 반복 비정상 종료 |
| `Error` | 실행 오류 |
| `Completed` | 정상 종료 |
| `OOMKilled` | 메모리 초과로 강제 종료 |
| `RunContainerError` | 컨테이너 시작 자체 실패 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*