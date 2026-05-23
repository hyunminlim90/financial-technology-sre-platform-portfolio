# 런타임 실행(Runtime Execution)

> 정독: 0회

## 1. 이 기술이 무엇인가

런타임 실행은:

> 프로그램이 실제 메모리에 적재되어 CPU가 명령을 수행하고 시스템 자원을 사용하며 동작하는 **실행 단계**

즉, **정적인 코드가 동적인 실행 상태로 전환되는 과정**입니다.

| 요소 | 의미 |
|------|------|
| 코드 로딩 | 실행 파일 적재 |
| 명령 실행 | CPU 연산 |
| 메모리 사용 | Heap/Stack 점유 |
| 스레드 실행 | 병렬 처리 |
| I/O 수행 | Disk/Network 접근 |
| 상태 변화 | 데이터 수정 |

런타임 실행은 **"실제 시스템 동작"** 자체입니다.

---

## 2. 시스템 어디에서 등장하는가

런타임 실행은 컴파일 이후 **실제 운영 단계**에서 등장합니다.

```
Source Code
→ Compilation
→ Executable/Bytecode
→ Runtime Loading
→ CPU Execution
→ I/O & Memory Operations
```

| 계층 | 역할 |
|------|------|
| Runtime Environment | 실행 관리 |
| OS | 프로세스/스레드 관리 |
| Kernel | 자원 제어 |
| Hardware | 실제 연산 |

**런타임은 소프트웨어와 하드웨어가 실제로 만나는 단계입니다.**

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

모든 자원에 직접 영향이 있습니다.

### CPU

런타임 동안 CPU는 명령 실행, 함수 호출, 스레드 스케줄링, 인터럽트 처리를 수행합니다. CPU 사용량은 런타임 구조에 의해 결정됩니다.

### Memory

런타임 중 객체 생성, Stack Frame 생성, Cache 사용, Heap 점유 등이 발생합니다.

### Disk

파일 읽기/쓰기, 로그 기록, DB 접근, 바이너리 로딩 등 I/O 실행 시 사용됩니다.

### Network

분산 시스템에서 매우 중요합니다. API 호출, TCP 연결, 메시지 송수신, 서비스 간 통신 등에 사용됩니다.

---

## 4. 왜 중요한가

실제 장애와 성능 문제는 대부분 런타임에서 발생합니다. 설계가 좋아도 런타임 실행이 불안정하면 시스템은 실패합니다.

| 영역 | 영향 |
|------|------|
| 성능 | 응답 속도 |
| 안정성 | 장애 여부 |
| 확장성 | 처리량 |
| 메모리 효율 | OOM 여부 |
| 동시성 | Race Condition |
| 운영성 | 관측 가능성 |

---

## 5. 실제 장애와 어떤 관련이 있는가

| 장애 | 런타임 원인 |
|------|-----------|
| CPU Spike | 무한 루프/과도한 연산 |
| OOM | 메모리 누수 |
| Deadlock | 스레드 교착 |
| GC Pause | 메모리 압박 |
| FD Exhaustion | 자원 반환 실패 |
| Latency 증가 | Blocking I/O |
| 장애 전파 | Runtime Backpressure 실패 |

> 운영 장애는 대부분 **런타임 상태 이상**입니다.

---

## 6. 핵심 메커니즘

### (1) 프로그램 로딩

실행 시작 시 실행 파일, 라이브러리, 메타데이터가 메모리에 적재됩니다.

포함 작업: 주소 공간 생성, Stack 생성, Heap 초기화, 코드 매핑

### (2) 실행 흐름 생성

CPU가 실제 명령을 수행합니다.

| 요소 | 역할 |
|------|------|
| Program Counter | 현재 실행 위치 |
| Stack Frame | 함수 호출 상태 |
| Thread | 실행 단위 |
| Scheduler | CPU 할당 |

### (3) 동적 메모리 관리

런타임 동안 메모리가 계속 변화합니다. 객체 생성, 해제, 캐시 적재, 버퍼 사용 등이 발생하며 잘못 설계되면 Memory Leak, Fragmentation, OOM이 발생합니다.

### (4) 런타임 디스패치

실행 중 어떤 코드를 호출할지 결정합니다.

- 함수 호출
- Virtual Dispatch
- Dynamic Linking
- Interface Resolution

현대 런타임은 이를 최적화합니다.

### (5) I/O와 System Call

런타임은 결국 OS 자원을 사용합니다.

```
Runtime
→ System Call
→ Kernel
→ Device Driver
→ Hardware
```

대표 System Call: `open`, `read`, `write`, `socket`, `mmap`

### (6) 최적화

현대 런타임은 실행 중 최적화를 수행합니다.

- JIT Compilation
- Inline Expansion
- Escape Analysis
- Dead Code Elimination

실행 중 코드 구조 자체가 변경될 수 있습니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

| 영역 | 명령 |
|------|------|
| 프로세스 | `ps` |
| CPU | `top` / `htop` |
| Memory | `free` / `vmstat` |
| I/O | `iostat` |
| System Call | `strace` |
| Thread | `top -H` |

```bash
top
strace -p <pid>
```

### Runtime

| 영역 | 의미 |
|------|------|
| Heap | 객체 상태 |
| Stack | 호출 상태 |
| Thread Dump | 스레드 상태 |
| GC | 메모리 회수 |
| JIT | 최적화 상태 |

### Kubernetes

K8s에서는 런타임 실행이 컨테이너 내부에서 발생합니다.

| 대상 | 의미 |
|------|------|
| Pod CPU | 실행 부하 |
| Pod Memory | Heap/Cache 사용 |
| OOMKilled | 메모리 실패 |
| Restart Count | 런타임 충돌 |
| Liveness Probe | 실행 상태 |
| Logs | 런타임 이벤트 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*