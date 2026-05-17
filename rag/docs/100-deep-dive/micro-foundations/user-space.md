# User Space (유저 공간)
## 1. 유저 공간(User Space)이란 무엇인가

> 정독: 0회

유저 공간(User Space)은:

> 일반 애플리케이션과 사용자 프로그램들이 제한된 권한으로 실행되는 운영체제의 **격리된 실행 영역**

운영체제는 시스템 전체를 보호하기 위해 실행 영역을 크게 두 개로 분리합니다.

```
User Space
↕ (System Call)
Kernel Space
```

- **User Space** = 일반 프로그램 실행 영역
- **Kernel Space** = 운영체제 핵심 제어 영역

### 대표적으로 유저 공간에서 실행되는 것들

- 웹 브라우저
- 게임
- 메신저
- 데이터베이스 프로세스
- 웹 서버 프로세스
- 컨테이너 내부 애플리케이션
- CLI 프로그램

> **핵심:** 유저 공간은 "프로그램을 안전하게 실행시키기 위한 격리 영역"입니다.

---

## 2. 시스템 어디에서 등장하는가

유저 공간은 운영체제 구조에서 애플리케이션 계층 바로 아래에 존재합니다.

```
Hardware
→ Kernel Space
→ User Space
→ Application
→ End User
```

**실제 실행 흐름:**

```
Application Code
→ Runtime
→ User Space Process
→ System Call
→ Kernel
→ Hardware
```

- 애플리케이션은 유저 공간에서 실행되고
- 하드웨어 접근이 필요할 때만 시스템 콜(System Call)을 통해 커널에 요청합니다.

### 구성 요소별 실행 위치

| 구성 요소 | 실행 위치 |
|----------|----------|
| Web Server | User Space |
| DB Process | User Space |
| Container Process | User Space |
| Browser | User Space |
| AI Model Process | User Space |
| Kubernetes Pod 내부 앱 | User Space |

---

## 3. 어떤 자원(CPU / Memory / Network / Disk)에 가장 영향이 큰가

유저 공간은 사실상 모든 시스템 자원 사용의 시작점입니다.

| 자원 | 영향도 |
|------|--------|
| CPU | 매우 큼 |
| Memory | 매우 큼 |
| Network | 큼 |
| Disk | 큼 |

### CPU 영향

유저 공간 프로세스는 다음으로 CPU를 사용합니다.

- 코드 실행
- Thread 실행
- Event Loop 처리
- 사용자 요청 처리

```
User Request 증가
→ User Space Process 부하 증가
→ CPU Usage 증가
```

### Memory 영향

유저 공간은 독립적인 가상 메모리 공간을 가집니다.

- Stack
- Heap
- Shared Memory
- mmap 영역

> 대부분의 메모리 소비는 유저 공간에서 발생합니다.

### Network 영향

유저 공간 프로세스는 Socket 생성, TCP 연결, Request/Response 처리를 수행하며, 실제 패킷 송수신은 커널이 수행합니다.

### Disk 영향

유저 공간 프로그램은 파일 읽기/쓰기, 로그 저장, DB 접근 등을 요청하며, 실제 디스크 제어는 커널이 수행합니다.

---

## 4. 왜 중요한가

운영체제 안정성과 보안을 유지하는 핵심 구조이기 때문입니다.

만약 모든 프로그램이 디스크 직접 제어, 메모리 직접 수정, CPU 인터럽트 직접 처리를 할 수 있다면 시스템 전체가 매우 불안정해집니다.

> 일반 프로그램은 User Space에서 격리하고, Kernel만 하드웨어를 직접 통제한다.

### 핵심 효과

| 효과 | 설명 |
|------|------|
| 안정성 | 앱 하나 죽어도 OS 전체는 유지 |
| 보안 | 권한 제한 |
| 격리 | 프로세스 간 메모리 보호 |
| 멀티태스킹 | 여러 앱 동시 실행 가능 |
| 자원 통제 | Kernel이 중앙 관리 |

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 서버 장애 대부분은 유저 공간에서 시작됩니다.

| 장애 | 원인 |
|------|------|
| OOM | User Space Memory Leak |
| CPU 100% | Busy Loop |
| Thread 폭증 | User Space Scheduler 문제 |
| FD 고갈 | Socket/File 누수 |
| Crash | Segmentation Fault |
| Pod Restart | User Process 종료 |
| Latency 증가 | User Space Queue 정체 |

### 유저 공간 장애 vs 커널 장애

| 장애 유형 | 영향 범위 |
|----------|----------|
| User Space Process Crash | 특정 앱만 장애 |
| Kernel Panic | 시스템 전체 다운 |

> User Space 격리 구조 덕분에 앱 장애가 시스템 전체 붕괴로 이어지는 것을 막습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 6-1. User Space는 하드웨어 직접 접근 권한이 없다

```
User Space
→ System Call
→ Kernel Space
→ Hardware
```

디스크 접근, 네트워크 송신, 메모리 매핑, 프로세스 생성 등은 모두 커널에 요청해야 합니다.

### 6-2. System Call이 User ↔ Kernel 연결 통로다

| 작업 | System Call |
|------|------------|
| 파일 읽기 | `read()` |
| 파일 쓰기 | `write()` |
| 네트워크 송신 | `send()` |
| 프로세스 생성 | `fork()` |
| 메모리 할당 | `mmap()` |

> User Space는 직접 하드웨어를 건드리지 않고, System Call을 통해 커널에 요청합니다.

### 6-3. CPU는 User Mode와 Kernel Mode를 전환한다

| 모드 | 권한 |
|------|------|
| User Mode | 제한적 |
| Kernel Mode | 전체 권한 |

```
User Mode 실행
→ System Call 발생
→ Kernel Mode 전환
→ 작업 수행
→ User Mode 복귀
```

> CPU 자체가 보호 메커니즘을 제공합니다.

### 6-4. 프로세스는 서로 메모리를 직접 볼 수 없다

유저 공간 프로세스들은 독립 Virtual Memory, 독립 Address Space를 가집니다.

```
Process A  ≠  Process B 메모리 접근 가능
```

브라우저, 메신저, 게임이 서로 메모리를 직접 오염시키지 못하는 이유입니다.

### 6-5. 컨테이너도 결국 User Space 격리 기술이다

컨테이너는 Namespace, cgroup, Virtual Network 등으로 유저 공간 프로세스를 격리합니다.

> Kubernetes도 결국 **User Space Process Isolation을 대규모 자동화한 시스템**입니다.

---

## 7. Linux / Runtime / Kubernetes에서 어디서 관측되는가

### Linux Process

**대표 확인 도구:**

```bash
ps aux
top
htop
pstree
```

**관측 가능 항목:** User Space Process / CPU 사용률 / Memory 사용량 / Thread 수

### System Call 관측

```bash
strace
ltrace
```

확인 가능: `read()` / `write()` / `open()` / `send()` / `recv()`

> User → Kernel 요청 흐름이 보입니다.

### Memory 관측

```bash
cat /proc/<pid>/maps
pmap
smem
```

확인 가능: Heap / Stack / mmap / Shared Library

### Kubernetes

Pod 내부 애플리케이션은 모두 User Space Process입니다.

```bash
kubectl top pod
kubectl exec
kubectl logs
```

관측 가능: Memory Leak / CPU Saturation / Process Crash / Restart

### Observability

현대 시스템에서는 eBPF, tracing, metrics, profiling 등으로 User Space 활동을 추적합니다.

### 대표 메트릭

| 메트릭 | 의미 |
|--------|------|
| Process CPU | 프로세스 부하 |
| RSS Memory | 실제 메모리 사용 |
| FD Count | 파일 디스크립터 수 |
| Thread Count | 실행 흐름 수 |
| Syscall Rate | 커널 호출량 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*