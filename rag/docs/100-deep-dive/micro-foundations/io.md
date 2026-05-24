# 입출력 (Input/Output, I/O)

> 정독: 0회

## 1. 이 기술이 무엇인가

입출력(I/O)은:

> 실행 중인 프로그램이 CPU 내부 연산만 수행하는 것이 아니라, **외부 자원과 데이터를 교환하는 모든 과정**

| I/O 대상 | 예시 |
|----------|------|
| Disk I/O | 파일 읽기/쓰기 |
| Network I/O | TCP/UDP 통신 |
| Device I/O | 키보드/마우스 |
| Console I/O | 터미널 출력 |
| IPC | 프로세스 간 통신 |

핵심 구조:

```
Program ↔ OS Kernel ↔ Device
```

> 애플리케이션은 직접 하드웨어를 제어하지 않고 **운영체제를 통해** I/O를 수행합니다.

---

## 2. 시스템 어디에서 등장하는가

I/O는 거의 모든 런타임 시스템에서 등장합니다.

| 계층 | 역할 |
|------|------|
| Application | file/network 요청 |
| Runtime | buffer/event/thread 관리 |
| OS Kernel | syscall 처리 |
| Driver | 장치 제어 |
| Hardware | 실제 데이터 송수신 |

**대표 흐름:**

```
Application
→ System Call
→ Kernel
→ Driver
→ Device
```

---

## 3. 어떤 자원에 가장 영향이 큰가

### Disk (매우 중요)

- database
- logging
- persistence
- file system

> Disk latency는 **전체 응답속도**를 크게 결정합니다.

### Network (현대 분산 시스템 핵심)

- API
- RPC
- service mesh
- distributed system

> 네트워크 지연은 **서비스 전체 latency**로 이어집니다.

### Memory (I/O 버퍼로 연결)

I/O는 대부분 buffer를 사용합니다:

- socket buffer
- page cache
- queue
- stream buffer

메모리 부족 시 I/O throughput이 급격히 감소할 수 있습니다.

### CPU (간접적으로 중요)

- packet parsing
- interrupt handling
- encryption / compression
- syscall processing

---

## 4. 왜 중요한가

현대 시스템 대부분은 **CPU-bound보다 I/O-bound** 상태입니다.

CPU 연산보다 다음이 더 큰 병목이 됩니다:

- 디스크 대기
- 네트워크 대기
- external response waiting

| 시스템 | 핵심 병목 |
|--------|----------|
| DB | Disk I/O |
| API Server | Network I/O |
| Streaming | Network + Memory |
| Logging | Disk I/O |
| Kubernetes Control Plane | etcd I/O |

---

## 5. 실제 장애와 어떤 관련이 있는가

I/O 장애는 **실제 운영 장애의 핵심 원인**입니다.

| 장애 | 원인 |
|------|------|
| 응답 지연 | slow disk |
| timeout | network latency |
| thread starvation | blocking I/O |
| connection exhaustion | socket leak |
| packet drop | NIC overload |
| OOM | excessive buffering |
| cascading failure | downstream I/O blocking |

특히 대규모 시스템에서 다음 패턴이 매우 흔합니다:

```
slow I/O
→ thread blocking
→ queue accumulation
→ latency explosion
→ timeout
→ retry storm
→ cascading failure
```

---

## 6. 핵심 메커니즘

### (1) System Call

애플리케이션은 직접 하드웨어 접근이 불가합니다. 따라서 커널에 다음 syscall로 요청합니다:

```
read()  write()  send()  recv()  open()
```

### (2) Blocking I/O

I/O 완료까지 스레드가 대기합니다.

- 구조 단순
- 스레드 점유 발생
- 대규모 동시성에 비효율 가능

### (3) Non-blocking I/O

I/O 대기 동안 스레드가 멈추지 않습니다. 핵심 기술:

- epoll
- kqueue
- io_uring
- event loop

### (4) Buffering

CPU와 장치 속도 차이를 흡수합니다:

- page cache
- socket buffer
- write buffer

### (5) Interrupt / DMA

현대 I/O의 핵심입니다:

- **DMA:** CPU 개입 없이 메모리 직접 전송
- **Interrupt:** I/O 완료 이벤트 통지

> 이 메커니즘이 **고성능 네트워크/스토리지**의 기반입니다.

---

## 7. Linux / Runtime / K8s에서 관측 방법

### Linux

```bash
iostat
iotop
vmstat
sar
ss
netstat
dstat
pidstat
strace
lsof
```

**대표 지표:**

| 지표 | 의미 |
|------|------|
| await | I/O latency |
| iowait | CPU가 I/O 대기 중 |
| rx/tx | network throughput |
| queue depth | I/O backlog |
| fd usage | file descriptor 사용량 |

### Runtime

관측 대상:

- thread blocking
- event loop delay
- GC pause during I/O pressure
- socket backlog
- buffer usage

### Kubernetes

**관측 대상:**

- container fs usage
- network rx/tx
- cni latency
- ephemeral storage
- volume latency

```bash
kubectl top
kubectl describe pod
kubectl logs
kubectl exec
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*