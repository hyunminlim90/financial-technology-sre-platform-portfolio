# 애플리케이션 소프트웨어 (Application Software)

> 정독: 0회

애플리케이션 소프트웨어(Application Software)는:

> **사용자가 특정 목적의 작업을 수행할 수 있도록 운영체제(OS) 위에서 실행되는 최상위 사용자 지향 소프트웨어**

**쉽게 말하면:**
"사람이 실제로 사용하는 프로그램"

예: 웹 브라우저, 메신저, 게임, 문서 작성기, DBMS, ERP, 영상 편집기

---

## 1. 이 기술이 무엇인가

컴퓨터 시스템은 크게 다음 구조로 동작합니다.

```
Hardware
↓
Operating System
↓
Application Software
↓
User
```

애플리케이션은 운영체제가 제공하는 **CPU / Memory / File / Network 기능**을 활용해 실제 업무와 서비스를 수행합니다.

즉, **사용자의 목적을 실현하는 소프트웨어**입니다.

---

## 2. 시스템 어디에서 등장하는가

애플리케이션은 컴퓨터 시스템 **최상단**에 위치합니다.

대표 영역:

- Browser / Game / Database
- Office Tool / Messenger / Media Player
- ERP / CRM / IDE
- Monitoring Tool / AI Software

실제로 사용자가 체감하는 **"서비스"** 대부분이 애플리케이션입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향 |
|------|------|
| CPU | 매우 큼 |
| Memory | 매우 큼 |
| Disk | 큼 |
| Network | 큼 |

애플리케이션 종류별 핵심 자원:

| 애플리케이션 | 핵심 자원 |
|-------------|-----------|
| 게임 | GPU / CPU |
| DBMS | Memory / Disk |
| 브라우저 | CPU / Memory |
| 영상 편집 | CPU / Disk |
| 메신저 | Network |

---

## 4. 왜 중요한가

하드웨어와 운영체제만으로는 사용자가 원하는 실제 작업을 수행할 수 없습니다.

애플리케이션이 있어야 문서 작성, 인터넷 사용, 게임, 금융 거래, 데이터 분석, 영상 편집이 가능합니다.

즉, **컴퓨터의 실제 존재 이유를 수행하는 계층**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. Memory Leak

애플리케이션이 메모리를 회수하지 않으면 OOM, Swap, 성능 저하가 발생합니다.

### 5-2. CPU Saturation

무한 루프나 과도한 연산 시 **CPU 100%** 상태가 발생할 수 있습니다.

### 5-3. Disk I/O Bottleneck

로그 폭증이나 대량 저장 시 **Disk Wait**가 증가합니다.

### 5-4. Network Overload

과도한 요청 발생 시 Packet Queue가 증가하여 **Latency**가 증가합니다.

### 5-5. Crash / Segmentation Fault

잘못된 메모리 접근 시 **Segmentation Fault**가 발생할 수 있습니다.

### 5-6. Deadlock

멀티스레드 경쟁 시 **프로세스 정지**가 발생할 수 있습니다.

### 5-7. Resource Exhaustion

파일 핸들/소켓 과다 사용 시 **Too many open files** 오류가 발생할 수 있습니다.

---

## 6. 핵심 메커니즘

### 6-1. 애플리케이션은 직접 하드웨어를 제어하지 못한다

애플리케이션은 **User Mode**에서 실행됩니다. DRAM, Disk, NIC, CPU Register 등을 직접 제어하지 못합니다.

### 6-2. 운영체제에게 요청한다

필요한 작업은 **System Call**을 통해 요청합니다.

```c
read()   write()   send()
recv()   open()    mmap()
```

### 6-3. 운영체제가 커널 모드에서 처리한다

```
Application → System Call → Kernel → Hardware → Result Return
```

### 6-4. 애플리케이션은 프로세스로 실행된다

실행 파일은 DRAM에 적재된 후 **Process**가 됩니다.

### 6-5. 애플리케이션은 독립된 주소 공간을 가진다

각 애플리케이션은 **독립 Virtual Address Space**를 가지며, MMU가 이를 보호합니다.

### 6-6. 애플리케이션은 결국 데이터 처리 시스템이다

```
Input → Processing → Storage → Output
```

### 6-7. 애플리케이션 성능은 OS 자원 관리에 의존한다

CPU Scheduling, Virtual Memory, Page Cache, Network Stack, File System의 영향을 받습니다.

### 6-8. 현대 애플리케이션은 대부분 네트워크 기반이다

대부분 **Client ↔ Server** 구조를 사용합니다. 즉, **Network I/O 중심 시스템**입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 프로세스 목록
ps aux
top
htop

# 실행 파일 확인
ls -l /proc/<pid>/exe

# 열린 파일 / 소켓
lsof

# 메모리 사용량
pmap <pid>
cat /proc/<pid>/maps

# 시스템 콜 추적
strace
```

### Runtime

주요 관측 대상:

- Thread
- Heap
- GC
- Connection Pool
- Buffer
- Queue
- Event Loop

### Kubernetes

```bash
# 애플리케이션 컨테이너 상태
kubectl get pod

# 로그 확인
kubectl logs <pod>

# 리소스 사용량
kubectl top pod
```

애플리케이션 장애 상태:

- `CrashLoopBackOff`
- `OOMKilled`
- `Error`

> **컨테이너 = 격리된 애플리케이션 실행 환경**
> 컨테이너는 결국 **애플리케이션 프로세스 격리 기술**입니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*