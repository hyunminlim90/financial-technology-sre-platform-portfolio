# Computer Program
## 1. 컴퓨터 프로그램이란 무엇인가

컴퓨터 프로그램(Computer Program)은:

> 컴퓨터가 특정 목적을 수행하도록 작성된 **명령어와 데이터의 정적 집합**

**"CPU가 무엇을 어떤 순서로 실행해야 하는지 적어 둔 설계도"**

프로그램은 실행되기 전에는 보통 디스크에 파일 형태로 존재합니다.

```
Source Code
   ↓
Compiler / Interpreter / Runtime
   ↓
Executable / Bytecode / Script
   ↓
Storage
```

**핵심 구분:**

| 개념 | 설명 |
|------|------|
| **Program** | 정적인 파일 — 아직 실행되지 않은 명령어 묶음 |
| **Process** | 실행 중인 프로그램 — OS가 메모리에 올려 실행 중인 상태 |

---

## 2. 시스템 어디에서 등장하는가

프로그램은 컴퓨터 시스템의 **소프트웨어 계층**에서 등장합니다.

```
User
 ↓
Program
 ↓
Operating System
 ↓
CPU / Memory / Disk / Network
```

프로그램은 단독으로 실행되지 않습니다. 운영체제가 프로그램을 실행하면 다음 흐름으로 바뀝니다.

```
Program File
   ↓
Loader
   ↓
Process
   ↓
CPU Execution
```

**실제 시스템에서 등장하는 형태:**

- 실행 파일
- 스크립트
- 라이브러리
- 서비스 바이너리
- 컨테이너 이미지 내부 파일
- OS 명령어
- 데몬 프로세스의 원본 파일

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 프로그램과의 관계 |
|------|------------------|
| CPU | 명령어 실행 |
| Memory | 코드 / 데이터 / 스택 / 힙 적재 |
| Disk | 프로그램 파일 저장, 파일 IO |
| Network | 통신 로직 수행 |
| GPU / Device | 특수 연산 또는 장치 제어 |

> **프로그램의 구조 = 자원 사용 패턴**

| 프로그램 유형 | 주요 자원 |
|--------------|-----------|
| 계산 중심 | CPU 사용량 증가 |
| 대용량 데이터 | Memory 사용량 증가 |
| 파일 처리 | Disk IO 증가 |
| 통신 | Network IO 증가 |

---

## 4. 왜 중요한가

프로그램은 하드웨어를 실제 목적에 맞게 움직이게 만드는 **실행 규칙**입니다.

프로그램이 없으면 CPU, Memory, Disk, Network는 그냥 부품에 불과합니다. 프로그램이 있어야 다음과 같은 의미 있는 시스템 동작이 만들어집니다.

```
입력 → 처리 → 저장 → 출력
```

**운영 관점에서 프로그램을 이해해야 구분할 수 있는 것들:**

- 코드 문제인가
- 실행 환경 문제인가
- 자원 부족 문제인가
- OS 제한 문제인가
- 배포 / 설정 문제인가

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. 프로그램 실행 실패

```
파일 없음 / 권한 없음 / 라이브러리 없음 / 잘못된 실행 포맷
```

```bash
./app: Permission denied
./app: No such file or directory
exec format error
```

### 5-2. 비정상 종료

프로그램 내부 오류가 발생하면 프로세스가 종료됩니다.

- Segmentation Fault
- Illegal Instruction
- Abort
- Exit Code != 0

### 5-3. 메모리 누수

```
Memory Usage 증가
  ↓
Memory Pressure
  ↓
OOM Kill
```

### 5-4. CPU 과점

비효율적인 루프나 과도한 연산은:

```
CPU Saturation
  ↓
Run Queue 증가
  ↓
Latency 증가
```

### 5-5. IO 병목

프로그램이 디스크나 네트워크를 과도하게 사용하면:

- Disk IO Wait 증가
- Network Packet Drop
- Connection Timeout

---

## 6. 핵심 메커니즘 요약

### 6-1. 프로그램은 실행 전에는 정적 파일이다

Disk 위의 프로그램 파일은 아직 CPU가 실행 중인 상태가 아닙니다.

### 6-2. 실행되면 프로세스가 된다

```
Program → Process
```

운영체제가 프로그램을 메모리에 적재하고 PID, 가상 주소 공간, 파일 디스크립터, 권한 정보를 부여합니다.

### 6-3. CPU는 프로그램의 명령어를 실행한다

```
Instruction Fetch → Decode → Execute → Write Back
```

### 6-4. 프로그램은 메모리 영역으로 나뉘어 적재된다

```
Process Virtual Memory

+-------------------+
| Stack             |  ← 함수 호출 / 지역 변수
+-------------------+
| Heap              |  ← 동적 메모리
+-------------------+
| Data / BSS        |  ← 전역 / 정적 데이터
+-------------------+
| Text / Code       |  ← 실행 명령어
+-------------------+
```

### 6-5. 프로그램은 OS를 통해 하드웨어를 사용한다

프로그램은 직접 하드웨어를 제어하지 않고, 다음 경로를 통합니다.

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

---

## 7. Linux / Runtime / K8s 에서 관측 방법

### Linux

```bash
# 실행 파일 확인
file ./app

# 실행 권한 확인
ls -l ./app

# 동적 라이브러리 의존성
ldd ./app

# 실행 중인 프로세스 확인
ps aux

# 프로세스 메모리 맵
cat /proc/<PID>/maps

# 프로세스 상태
cat /proc/<PID>/status

# 시스템 콜 추적
strace ./app
```

### Runtime

모든 런타임은 결국 OS 프로세스 위에서 동작합니다. 공통적으로 다음을 관측합니다.

| 항목 | 설명 |
|------|------|
| Exit Code | 정상/비정상 종료 여부 |
| Error Log | 오류 내용 |
| Memory Usage | 메모리 점유량 |
| CPU Usage | CPU 사용률 |
| Thread Count | 스레드 수 |
| Open File Descriptor | 열린 파일 수 |
| GC / Allocator Metrics | 런타임 내부 지표 |

### Kubernetes

```bash
# 컨테이너 실행 상태
kubectl get pod

# 종료 원인 확인
kubectl describe pod

# 로그 확인
kubectl logs <pod>

# 실행 중 프로세스 확인
kubectl exec -it <pod> -- ps aux
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