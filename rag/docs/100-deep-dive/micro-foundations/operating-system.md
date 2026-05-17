# 운영체제 (Operating System / OS)

> 정독: 0회

운영체제(OS)는:

> **컴퓨터 하드웨어 자원을 직접 통제·관리하면서 응용 프로그램과 사용자에게 안전하고 일관된 실행 환경을 제공하는 최상위 핵심 시스템 소프트웨어**

**쉽게 말하면:**
"CPU, DRAM, SSD, NIC 같은 물리 하드웨어를 실제로 지휘하는 중앙 관리자"

<details>
<summary>Deep Dive</summary></br>

Computer(컴퓨터) [[M]](../../100-deep-dive/micro-foundations/computer.md)  
Hardware(하드웨어) [[M]](../../100-deep-dive/micro-foundations/hardware.md)  
Resource Management(자원 관리) [[M]](../../100-deep-dive/micro-foundations/resource-management.md)  
Application Software(애플리케이션 소프트웨어) [[M]](../../100-deep-dive/micro-foundations/application-software.md)  
End User(엔드 유저) [[M]](../../100-deep-dive/micro-foundations/end-user.md)  
Runtime Environment(런타임 환경) [[M]](../../100-deep-dive/micro-foundations/runtime-environment.md)  
User Space(유저 공간) [[M]](../../100-deep-dive/micro-foundations/user-space.md)  

</details></br>

## 1. 이 기술이 무엇인가

운영체제는 **하드웨어 ↔ 프로그램 사이의 중재 계층**입니다.

프로그램은 CPU를 직접 제어하지 못하며, 대신 운영체제에 요청합니다.

- 메모리 달라
- 파일 읽어라
- 네트워크 보내라
- 디스크 저장해라
- 프로세스 실행해라

운영체제가 **실제 하드웨어 자원 사용 권한**을 통제합니다.

### 운영체제의 핵심 역할

1. CPU 관리
2. 메모리 관리
3. 저장장치 관리
4. I/O 장치 관리
5. 프로세스 격리
6. 보안 / 권한 관리
7. 시스템 안정성 유지

---

## 2. 시스템 어디에서 등장하는가

운영체제는 컴퓨터 시스템 전체, 사실상 **모든 프로그램 실행의 기반**에 등장합니다.

대표 영역:

- 프로세스 실행
- 메모리 할당
- 파일 읽기 / 쓰기
- 네트워크 통신
- 디스크 I/O
- 컨테이너 / VM
- 스케줄링
- 시스템 콜
- 인터럽트 처리

즉, **OS 없는 현대 컴퓨터는 사실상 불가능**에 가깝습니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

운영체제는 **모든 핵심 자원**에 직접 영향을 줍니다.

| 자원 | 영향도 |
|------|--------|
| CPU | 절대적 |
| Memory | 절대적 |
| Disk | 절대적 |
| Network | 절대적 |

운영체제는 **하드웨어 자원 배분의 총관리자**입니다.

---

## 4. 왜 중요한가

운영체제가 없다면 프로그램들이 하드웨어를 직접 충돌하며 사용해야 합니다.

그러면 메모리 침범, 시스템 충돌, 데이터 오염, 디스크 손상, CPU 독점, 보안 붕괴가 발생할 수 있습니다.

운영체제는 **"하드웨어 자원의 질서 유지"** 를 담당합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. CPU 스케줄링 문제

프로세스 폭증 시 Context Switch가 증가하여 **CPU Cache Pollution**이 발생할 수 있습니다.

**증상:** 시스템 지연 증가, Load Average 급증, 응답성 저하

### 5-2. 메모리 부족

운영체제가 메모리 회수에 실패하거나 Swap을 과다 사용하면 **Page Fault**가 폭증합니다.

**결과:** 시스템 전체 느려짐

### 5-3. OOM (Out Of Memory)

DRAM 부족 시 **OOM Killer**가 프로세스를 강제 종료합니다.

### 5-4. 디스크 병목

파일 시스템 I/O 대기 증가 시 **I/O Wait**가 증가하여 CPU가 디스크 응답 대기 상태에 들어갑니다.

### 5-5. 인터럽트 폭증

NIC/Storage 인터럽트 과다 시 **SoftIRQ Saturation**이 발생합니다.

**결과:** 네트워크 처리 지연

### 5-6. 파일 시스템 손상

Crash 중 Write가 중단되면 **Filesystem Corruption**이 발생할 수 있습니다.

---

## 6. 핵심 메커니즘

### 6-1. 운영체제는 커널(Kernel)이 핵심이다

운영체제의 핵심 실체는 **Kernel**입니다. 커널은 **하드웨어 직접 제어 권한**을 보유합니다.

### 6-2. 프로세스는 직접 하드웨어 접근을 못한다

프로그램은 CPU/DRAM/NIC를 직접 제어할 수 없습니다. 반드시 **System Call**을 통해 OS에 요청해야 합니다.

```c
read()   write()   open()
fork()   mmap()    socket()
```

### 6-3. 운영체제가 CPU 시간을 배분한다

CPU 코어는 제한적입니다. **OS Scheduler**가 누가 CPU를 사용할지 결정합니다.

즉, **멀티태스킹의 핵심 관리자**입니다.

### 6-4. 운영체제가 가상 메모리를 만든다

프로세스는 실제 DRAM 주소를 알지 못합니다.

OS + MMU가 `Virtual Address → Physical Address` 변환을 수행합니다.

### 6-5. 운영체제가 파일 시스템을 관리한다

SSD 내부는 사실상 raw block 저장소입니다. OS가 파일, 디렉토리, 권한, inode, journal 등을 구성합니다.

### 6-6. 인터럽트 기반으로 동작한다

하드웨어 장치는 이벤트 발생 시 **Interrupt**를 CPU에 전달하고 OS가 처리합니다.

- 키보드 입력
- NIC 패킷 도착
- 디스크 완료
- 타이머 이벤트

### 6-7. 운영체제는 자원 격리를 담당한다

OS는 프로세스 간 메모리 침범을 차단합니다.

핵심: **Protection, Isolation, Permission**

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# CPU
top
htop
mpstat
pidstat

# 메모리
free -h
vmstat
cat /proc/meminfo

# 프로세스
ps -ef
pstree

# 스케줄링
pidstat -w

# 디스크 I/O
iostat -x
iotop

# 네트워크
ss -s
sar -n DEV
ethtool

# 인터럽트
cat /proc/interrupts

# 가상 메모리
cat /proc/<pid>/maps
```

### Runtime

주요 관측 대상:

- Thread
- Context Switch
- Page Fault
- System Call
- File Descriptor
- Scheduler Delay

### Kubernetes

K8s도 결국 **Linux Kernel 위**에서 동작합니다.

```bash
# 핵심 관측
kubectl top node
kubectl top pod

# OOMKilled 확인
kubectl describe pod
```

**cgroup 기반 자원 통제:**

- CPU quota
- Memory limit
- IO limit

> **Container Runtime**도 결국 OS Process입니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*