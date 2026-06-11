# 소프트웨어 (Software)

> 정독: 1회

소프트웨어(Software)는:

> 하드웨어가 어떤 동작을 수행해야 하는지 정의하는 **명령어·논리·데이터·제어 흐름의 집합**

즉 CPU가 무엇을 계산할지 · 메모리를 어떻게 사용할지 · 네트워크로 무엇을 보낼지 · 디스크에 무엇을 저장할지를 정의합니다.

핵심은:

**"하드웨어를 실제로 움직이게 만드는 논리적 제어 체계"**

---

## 2. 시스템 어디에서 등장하는가

소프트웨어는 컴퓨터 시스템 전체에 존재합니다.

| 계층 | 소프트웨어 예시 |
|---|---|
| Application | 브라우저/게임/메신저 |
| Runtime | VM/Interpreter |
| OS | Linux/Windows |
| Driver | NIC Driver |
| Firmware | BIOS/NIC Firmware |
| Hypervisor | VMM |
| Embedded | Router Software |

**실제 흐름:**

```
Application
→ OS
→ Driver
→ Hardware
```

즉 소프트웨어는 **하드웨어 위 전체를 덮고 있는 제어 계층**입니다.

---

## 3. 어떤 자원(CPU / Memory / Network / Disk)에 가장 영향이 큰가

소프트웨어는 시스템의 **모든 자원**을 제어합니다.

| 자원 | 영향도 |
|---|---|
| CPU | 매우 큼 |
| Memory | 매우 큼 |
| Network | 매우 큼 |
| Disk | 매우 큼 |

즉 **소프트웨어는 컴퓨터 자원 전체를 사용하고 제어**합니다.

**CPU 영향**

```
Instruction 실행 · Scheduling · Parallelism · Interrupt 처리
```

즉 CPU는 소프트웨어 명령을 실행합니다.

**Memory 영향**

```
Virtual Memory · Heap/Stack · Cache · Buffer
```

즉 소프트웨어는 메모리 위에서 살아 움직입니다.

**Network 영향**

```
TCP/IP · Ethernet · Socket · Packet Processing
```

즉 네트워크도 소프트웨어 로직 위에서 제어됩니다.

**Disk 영향**

```
File System · Database · Logging · Storage Engine
```

즉 디스크 접근도 소프트웨어가 관리합니다.

---

## 4. 왜 중요한가

하드웨어만으로는 컴퓨터가 아무것도 하지 못하기 때문입니다.

> CPU·Memory·NIC·Disk는 스스로 의미 있는 동작을 하지 못한다.  
> **소프트웨어가 있어야 시스템이 실제 기능을 수행한다.**

**현대 시스템의 본질**

현대 시스템은 결국 **하드웨어 위에서 수많은 소프트웨어 계층이 협력하는 구조**입니다.

```
Application
→ Runtime
→ OS
→ Driver
→ Firmware
→ Hardware
```

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 운영 장애 대부분은 결국 **소프트웨어 문제**입니다.

| 장애 | 설명 |
|---|---|
| Memory Leak | 메모리 누수 |
| Deadlock | 동기화 정지 |
| Crash | 프로세스 종료 |
| Infinite Loop | CPU 고갈 |
| Packet Storm | 네트워크 폭주 |
| File Corruption | 저장 오류 |
| Race Condition | 경쟁 상태 |
| Resource Exhaustion | 자원 고갈 |

**매우 중요한 실무 포인트**

> 하드웨어는 정상인데  
> **소프트웨어 논리 문제 때문에 시스템 전체가 멈추는 경우가 훨씬 많다.**

대표 사례: DB 장애 · Kubernetes 장애 · API Timeout · Payment Failure · Distributed System 장애

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 6-1. 소프트웨어는 결국 CPU 명령어로 실행된다

```
Source Code
→ Compile/Interpret
→ Machine Code
→ CPU Instruction
```

즉 소프트웨어 최종 목적지는 **CPU 실행**입니다.

### 6-2. 소프트웨어는 메모리 위에서 동작한다

```
Code Segment · Heap · Stack · Buffer
```

즉 **실행 중인 소프트웨어 = 메모리 안의 상태 집합**입니다.

### 6-3. 소프트웨어는 OS를 통해 하드웨어를 제어한다

```
Application
→ System Call
→ Kernel
→ Driver
→ Hardware
```

즉 일반 프로그램은 **직접 하드웨어를 만지지 못합니다**.

### 6-4. 소프트웨어는 계층적으로 구성된다

| 계층 | 역할 |
|---|---|
| Application | 사용자 기능 |
| Middleware | 연결/중재 |
| Runtime | 실행 환경 |
| OS | 자원 관리 |
| Driver | 장치 제어 |

즉 현대 시스템은 **소프트웨어 레이어 구조**입니다.

### 6-5. 소프트웨어는 상태(State)를 가진다

상태 예시: `Memory State` · `Connection State` · `File State` · `Session State` · `Cluster State`

즉 장애 분석 핵심은 **"현재 소프트웨어 상태가 무엇인가"** 입니다.

### 6-6. 소프트웨어는 결국 I/O 시스템이다

거의 모든 소프트웨어는 `Input → Processing → Output` 구조입니다.

| 입력 | 처리 | 출력 |
|---|---|---|
| HTTP Request | Business Logic | Response |
| Packet | Routing | Packet Forward |
| Query | DB Engine | Result |

---

## 7. Linux / Runtime / Kubernetes에서 어디서 관측되는가

**프로세스 확인**

```bash
ps -ef
top
htop
```

관측 가능: 실행 중인 소프트웨어 · CPU 사용량 · Memory 사용량

**메모리 상태 확인**

```bash
free -h
vmstat
```

관측 가능: Heap · Cache · Memory Pressure

**파일 상태 확인**

```bash
lsof
```

관측 가능: Open File · Socket · Device Handle

**네트워크 상태 확인**

```bash
ss -tulpn
netstat
```

관측 가능: Connection · Port · Session

**Runtime 관측**

```bash
strace
perf
```

관측 가능: System Call · CPU Hotspot · Runtime Behavior

**Kubernetes**

Kubernetes도 결국 소프트웨어 오케스트레이션 시스템입니다.

```
Container Runtime
→ kubelet
→ kube-proxy
→ CNI
→ API Server
```

즉 K8s 자체가 **거대한 소프트웨어 계층**입니다.

**Observability**

현대 운영에서는 `Metrics` · `Logs` · `Traces` · `Profiling` · `eBPF` 등으로 소프트웨어 상태를 관측합니다.

| 메트릭 | 의미 |
|---|---|
| CPU Usage | 실행 부하 |
| Memory Usage | 메모리 상태 |
| GC Pause | 런타임 정지 |
| Request Latency | 처리 지연 |
| Error Rate | 실패율 |
| Thread Count | 동시 실행량 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*