# Runtime Environment (런타임 환경)
## 1. 런타임 환경(Runtime Environment)이란 무엇인가

> 정독: 0회

런타임 환경(Runtime Environment)은:

> 프로그램이 단순한 정적 파일 상태를 벗어나 실제 메모리 위에서 실행(Runtime)될 때, 프로그램이 정상적으로 동작할 수 있도록 실행 엔진·메모리·라이브러리·시스템 인터페이스 등을 제공하는 실행 기반 환경

```
프로그램 코드
→ 메모리 적재
→ 런타임 환경이 실행 관리
→ CPU 위에서 실제 동작
```

> **핵심:** 런타임은 "프로그램이 살아 움직이는 동안" 필요한 실행 기반 인프라입니다.

### 대표 기능

- 메모리 관리
- 코드 실행
- 스레드 실행
- 라이브러리 연결
- 시스템 호출(System Call) 연결
- 예외 처리
- 입출력(I/O)
- 네트워크 인터페이스 연결

<details>
<summary>Deep Dive</summary></br>



</details></br>

## 2. 시스템 어디에서 등장하는가

런타임 환경은 운영체제와 애플리케이션 사이의 **중간 계층**에서 등장합니다.

```
Hardware
→ Kernel
→ Operating System
→ Runtime Environment
→ Application
→ End User
```

- 운영체제가 CPU/Memory/Disk/NIC 같은 물리 자원을 제공하고
- 런타임 환경이 그것을 추상화하여 프로그램 실행에 연결하며
- 애플리케이션은 런타임 위에서 동작합니다.

### 대표 예시

| 영역 | 런타임 역할 |
|------|------------|
| 웹 브라우저 | JS 실행 |
| 게임 엔진 | 스크립트 실행 |
| 모바일 앱 | 앱 실행 환경 |
| 서버 플랫폼 | 요청 처리 |
| AI/데이터 플랫폼 | 모델 실행 |
| CLI 프로그램 | 프로세스 실행 |

> 현대 소프트웨어 대부분은 사실상 "런타임 위의 프로그램"입니다.

---

## 3. 어떤 자원(CPU / Memory / Network / Disk)에 가장 영향이 큰가

런타임은 거의 모든 시스템 자원과 직접 연결됩니다.

| 자원 | 영향도 |
|------|--------|
| CPU | 매우 큼 |
| Memory | 매우 큼 |
| Network | 중간~큼 |
| Disk | 중간 |

### CPU 영향

런타임은 다음으로 CPU를 직접 사용합니다.

- 코드 실행
- 스케줄링
- 인터프리팅
- JIT/동적 최적화
- 이벤트 루프
- 스레드 실행

**예시:**
```
사용자 요청 증가
→ 런타임 스레드 증가
→ CPU Context Switching 증가
→ CPU Saturation
```

### Memory 영향

런타임은 메모리를 직접 관리합니다.

- Stack
- Heap
- Object Allocation
- Buffer
- Cache

> 런타임은 대부분의 메모리 사용 패턴을 결정합니다.

### Network 영향

- Socket API
- Connection 관리
- Request 처리
- Serialization

### Disk 영향

- 로그 기록
- 파일 I/O
- 캐시 저장
- 임시 파일 처리

---

## 4. 왜 중요한가

실제 애플리케이션 성능과 안정성 대부분이 런타임 특성에 의해 결정되기 때문입니다.

> 같은 코드라도 런타임 특성에 따라 성능/메모리/지연이 완전히 달라질 수 있다.

| 런타임 기능 | 시스템 영향 |
|------------|------------|
| 메모리 관리 | OOM 여부 |
| 스레드 모델 | 동시성 처리 |
| 이벤트 처리 | Latency |
| GC/메모리 회수 | Pause |
| 실행 엔진 | CPU 효율 |
| IO 처리 방식 | Throughput |

> 현대 서버 시스템에서는 런타임 이해 없이 장애 분석이 거의 불가능합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

런타임은 실제 장애의 중심에 있는 경우가 매우 많습니다.

| 장애 | 런타임 관련 원인 |
|------|----------------|
| OOM | Heap 과다 사용 |
| CPU 100% | Busy Loop / 과도한 Thread |
| Latency 증가 | Runtime Queue 정체 |
| 서비스 멈춤 | Deadlock |
| 메모리 누수 | Object 미해제 |
| 요청 지연 | Event Loop Blocking |
| Crash | Runtime Exception |
| Pod Restart | Runtime Memory 폭증 |

> 애플리케이션 장애 ≒ 런타임 자원 관리 문제인 경우가 매우 많습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 6-1. 런타임은 OS 위의 "실행 관리자"이다

```
OS          = 자원 제공자   (CPU / Memory / Disk / Network)
Runtime     = 실행 관리자   (코드 실행 / 메모리 관리 / 동시성 처리)
Application = 비즈니스 로직
```

### 6-2. 런타임은 메모리와 실행 흐름을 직접 관리한다

| 영역 | 설명 |
|------|------|
| Stack | 함수 호출 |
| Heap | 동적 메모리 |
| Thread | 실행 흐름 |
| Event Queue | 이벤트 처리 |
| Timer | 비동기 작업 |
| Buffer | 네트워크/IO 임시 저장 |

> 런타임은 "프로그램이 실제로 어떻게 움직이는가"를 결정합니다.

### 6-3. 런타임은 하드웨어를 직접 만지지 않는다

```
Application
→ Runtime
→ System Call
→ Kernel
→ Hardware
```

런타임은 직접 디스크/NIC 제어, 직접 CPU 스케줄링을 하지 않으며, 반드시 운영체제 → 커널 → 시스템 콜을 통해 접근합니다.

### 6-4. 컨테이너/K8s 환경에서도 런타임이 핵심이다

컨테이너는 단순 격리일 뿐, 실제 Pod 내부에서는 다음이 동작합니다.

```
Container
→ Runtime Process
→ Thread
→ Memory
→ Network Connection
```

Kubernetes 장애의 상당수가 런타임 문제입니다.

- Memory Leak
- GC Pause
- Event Queue Blocking
- Excessive Thread
- Connection Pool Exhaustion

---

## 7. Linux / Runtime / Kubernetes에서 어디서 관측되는가

### Linux

**대표 관측 도구:**

```bash
top / htop / ps / pidstat / vmstat / strace / lsof
```

**확인 가능 항목:**

- CPU 사용률
- Thread 증가
- Memory 증가
- System Call 폭증
- File Descriptor 누수

### Process 관점

```
PID
└── Runtime Process
    ├── Thread
    ├── Heap
    ├── Stack
    ├── Socket
    └── File Descriptor
```

> 런타임은 결국 하나 이상의 프로세스로 관측됩니다.

### Memory 관측

```bash
pmap
smem
cat /proc/<pid>/status
```

확인 가능: Heap 증가 / RSS 증가 / Virtual Memory 증가

### Kubernetes

**대표 관측 명령:**

```bash
kubectl top pod
kubectl describe pod
kubectl logs
kubectl exec
```

**관측 가능 항목:**

- OOMKilled
- Restart Count 증가
- CPU Throttling
- Memory Saturation

### Observability

현대 시스템에서는 다음 도구로 런타임 상태를 추적합니다.

- Prometheus
- Grafana
- OpenTelemetry
- eBPF
- Tracing

### 대표 메트릭

| 메트릭 | 의미 |
|--------|------|
| Heap Usage | 메모리 사용량 |
| Thread Count | 동시 실행 흐름 |
| GC Pause | 메모리 회수 지연 |
| Event Queue Depth | 요청 정체 |
| Request Latency | 처리 지연 |
| CPU Usage | 실행 부하 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*