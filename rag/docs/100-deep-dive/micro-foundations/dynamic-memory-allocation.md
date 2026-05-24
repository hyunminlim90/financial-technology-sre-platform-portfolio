# 동적 메모리 할당 (Dynamic Memory Allocation)

> 정독: 0회

## 1. 이 기술이 무엇인가

동적 메모리 할당은:

> 프로그램 실행 중(Runtime)에 필요한 순간에 메모리 공간을 **실시간으로 확보하고 사용하는 메모리 관리 방식**

핵심: **memory is allocated during execution**

프로그램은 실행 전에는 다음을 알 수 없습니다:

- 실제 사용자 수
- 데이터 크기
- 객체 개수
- 요청량

따라서 런타임 중 상황에 맞춰 메모리를 동적으로 확보해야 합니다.

| 대상 | 설명 |
|------|------|
| 객체(Object) | 실행 중 생성 |
| 배열(Array) | 가변 크기 |
| 버퍼(Buffer) | I/O 처리 |
| 캐시(Cache) | 동적 데이터 |
| 스레드 구조체 | 실행 흐름 |

> 동적 메모리 할당은 실행 시간에 메모리 자원을 **유연하게 생성·사용·회수하는 런타임 핵심 메커니즘**입니다.

---

## 2. 시스템 어디에서 등장하는가

동적 메모리 할당은 거의 모든 런타임 시스템에서 등장합니다.

| 영역 | 사용 목적 |
|------|----------|
| Heap | 객체 저장 |
| Runtime Engine | 실행 상태 관리 |
| Thread Runtime | 스택/버퍼 생성 |
| Network Runtime | packet buffer |
| File I/O | read/write buffer |
| Cache System | temporary storage |

**대표 흐름:**

```
runtime request
→ memory allocation
→ object/data usage
→ memory reclaim
```

운영체제 레벨에서는 virtual memory, page allocation, allocator, kernel memory manager와 연결됩니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **Memory**입니다. 하지만 실제로는 연쇄 영향이 발생합니다:

| 자원 | 영향 |
|------|------|
| CPU | allocation bookkeeping |
| Memory | heap usage |
| Disk | swap 발생 가능 |
| Network | buffer allocation |

특히 **CPU 영향도 큽니다**. 다음이 계속 발생하기 때문입니다:

- allocator synchronization
- GC scanning
- fragmentation management
- metadata update

---

## 4. 왜 중요한가

현대 소프트웨어는 대부분 **동적 메모리 할당에 의존**합니다.

| 이유 | 설명 |
|------|------|
| 유연성 | 실행 중 크기 변화 대응 |
| 확장성 | 동시 요청 증가 대응 |
| 효율성 | 필요한 만큼만 사용 |
| 동적 데이터 처리 | runtime object generation |
| 대규모 시스템 운영 | elastic workload 대응 |

동적 할당이 없다면 **fixed-size execution only**가 됩니다.

> 현대 서버·브라우저·DB·AI·K8s 시스템은 사실상 **동적 메모리 할당 없이는 동작 불가능**합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실무 장애와 **매우 강하게 연결**됩니다.

| 장애 | 원인 |
|------|------|
| OOM | excessive allocation |
| Memory Leak | unreleased object |
| Fragmentation | non-contiguous free space |
| GC Pause | allocation pressure |
| Swap Thrashing | memory exhaustion |
| Allocation Contention | allocator lock competition |

특히 **allocation rate** 문제가 중요합니다. 객체 생성 속도가 지나치게 빠르면:

```
heap pressure 증가 → GC 빈도 증가 → CPU 상승 → latency spike
```

> 대규모 트래픽 시스템에서는 **allocation behavior = latency behavior**가 되는 경우도 많습니다.

---

## 6. 핵심 메커니즘

### (1) Allocation Trigger

동적 할당은 런타임 요청으로 시작됩니다:

- object creation
- buffer expansion
- cache insertion

### (2) Heap Allocation

대부분의 동적 메모리는 **heap memory**에 생성됩니다.

힙 특징: variable size, shared access, runtime managed

### (3) Allocator

런타임 내부에는 allocator가 존재합니다.

| 기능 | 설명 |
|------|------|
| free block search | 빈 공간 탐색 |
| alignment | 메모리 정렬 |
| metadata update | 상태 기록 |
| pointer return | 주소 반환 |

### (4) Memory Fragmentation

객체 생성/삭제 반복 시 small free spaces가 여기저기 생깁니다.

```
총 메모리는 충분해도  →  large contiguous block unavailable
```

### (5) Reclamation

동적 할당은 반드시 회수와 연결됩니다.

| 방식 | 설명 |
|------|------|
| manual free | explicit release |
| garbage collection | automatic reclaim |
| reference counting | usage tracking |

### (6) Thread Local Allocation

현대 런타임 핵심입니다. 멀티스레드 환경에서 **allocator lock contention 감소** 목적입니다.

### (7) Allocation Metadata

메모리 할당 시 내부적으로 기록합니다:

- object size
- allocation state
- ownership
- type metadata

### (8) Virtual Memory

실제 물리 RAM보다 먼저 **virtual address space** 위에서 관리됩니다.

```
runtime + virtual memory + allocator + reclamation  →  결합된 메커니즘
```

---

## 7. Linux / Runtime / K8s에서 관측 방법

### Runtime

| 항목 | 의미 |
|------|------|
| allocation rate | 객체 생성 속도 |
| heap growth | 메모리 증가 |
| GC frequency | 회수 압박 |
| fragmentation | 메모리 단편화 |
| allocation stall | 할당 지연 |

**대표 도구:** `jstat`, `jmap`, `jcmd`, heap dump, runtime profiler

**대표 증상:** rapid heap expansion, high allocation churn, GC storm

### Linux

```bash
top
htop
vmstat
free
smem
pmap
sar
```

| 지표 | 의미 |
|------|------|
| RSS | 실제 메모리 사용 |
| Page Fault | 메모리 접근 실패 |
| Swap Usage | RAM 부족 |
| Cache Pressure | reclaim 증가 |

### Kubernetes

**대표 현상:** OOMKilled, memory throttling, restart loop, eviction

```bash
kubectl top pod
kubectl describe pod
```

**관측 도구:** metrics-server, Prometheus

> **memory request vs limit** 관리가 핵심입니다. 동적 할당량이 limit 초과 시 **container termination**이 발생합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*