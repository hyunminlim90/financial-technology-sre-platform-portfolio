# 자원 할당 (Resource Allocation)

> 정독: 0회

## 1. 이 기술이 무엇인가

자원 할당은:

> **제한된 시스템 자원을 실행 중인 작업에게 배분하는 과정**

| 자원 | 예 |
|------|-----|
| CPU | 코어 실행 시간 |
| Memory | 힙, 스택, 페이지 |
| Disk | 파일, 블록 I/O |
| Network | 소켓, 대역폭, 버퍼 |
| Kernel Object | 파일 디스크립터, 락, 세마포어 |

> **자원 할당 = 누가 어떤 자원을 얼마나, 언제 사용할 수 있는지 결정하는 시스템 제어 과정**

---

## 2. 시스템 어디에서 등장하는가

자원 할당은 운영체제와 런타임 전체에 존재합니다.

| 계층 | 자원 할당 예 |
|------|-------------|
| CPU Scheduler | 스레드에 CPU 시간 배정 |
| Memory Manager | 프로세스에 메모리 페이지 배정 |
| Heap Allocator | 객체/버퍼용 메모리 블록 배정 |
| I/O Scheduler | 디스크 요청 순서 결정 |
| Network Stack | 송수신 버퍼, 큐, 대역폭 배정 |
| Container Runtime | CPU/Memory quota 적용 |
| Hypervisor | vCPU, guest memory 배정 |

실행 중인 시스템은 계속해서 **요청 → 판단 → 할당 → 사용 → 회수** 흐름을 반복합니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

자원 할당은 모든 자원에 영향을 주지만, 가장 핵심은 **CPU + Memory**입니다.

| 자원 | 영향 |
|------|------|
| CPU | 실행 시간, 스케줄링, 문맥 전환 |
| Memory | 할당, 회수, 페이지 폴트, OOM |
| Network | 버퍼, 큐, 패킷 드롭 |
| Disk | I/O 대기, 큐 적체, 지연 증가 |

특정 자원 하나만 부족해도 전체 요청 흐름이 느려질 수 있습니다.

---

## 4. 왜 중요한가

자원은 항상 제한되어 있지만 실행 흐름은 많습니다. **요청 수 > 실제 처리 가능한 자원량**이 되는 순간, 시스템은 반드시 할당 정책을 가져야 합니다.

| 이유 | 설명 |
|------|------|
| 안정성 | 특정 작업의 자원 독점 방지 |
| 공정성 | 여러 작업에 자원 분배 |
| 성능 | 처리량과 지연 시간 최적화 |
| 격리 | 프로세스/컨테이너 간 영향 제한 |
| 장애 방어 | 과부하 전파 차단 |

> **자원 할당은 시스템이 무너지지 않도록 자원 사용권을 통제하는 핵심 메커니즘입니다.**

---

## 5. 실제 장애와 어떤 관련이 있는가

| 장애 | 원인 |
|------|------|
| CPU Saturation | 실행 가능한 스레드가 CPU보다 많음 |
| OOM / OOMKill | 메모리 할당량 초과 |
| Thread Starvation | 특정 스레드가 CPU를 못 받음 |
| I/O Starvation | 디스크/네트워크 요청이 큐에서 대기 |
| Queue Backlog | 처리량보다 유입량이 많음 |
| Throttling | 할당량 제한으로 실행 지연 |
| Deadlock | 자원을 서로 기다림 |
| Priority Inversion | 낮은 우선순위 작업이 자원 점유 |

**대표 연쇄 흐름:**

```
자원 부족 → 대기열 증가 → 지연 증가 → 타임아웃 → 재시도 증가 → 부하 증폭
```

실제 운영에서 이 연쇄는 장애 전파의 핵심 원인이 됩니다.

---

## 6. 핵심 메커니즘

### CPU 할당

CPU는 스레드에게 직접 "소유"되는 것이 아니라, 짧은 시간 단위로 배정됩니다.

```
Ready Queue → Scheduler → Dispatch → Running
```

CPU 할당 실패 또는 지연이 발생하면 run queue 증가, context switch 증가, latency 증가, throughput 감소가 나타납니다.

### 메모리 할당

프로그램이 객체, 배열, 버퍼, 스택 프레임 등을 만들면 메모리 관리자가 공간을 배정합니다.

| 요소 | 설명 |
|------|------|
| Base Address | 할당된 블록의 시작 주소 |
| Size | 할당 크기 |
| Permission | 읽기/쓰기/실행 권한 |
| Lifetime | 사용 후 회수 시점 |

메모리 할당 실패는 OOM, 페이지 폴트, GC 압박으로 연결됩니다.

### I/O 자원 할당

디스크와 네트워크도 동시에 무한 처리할 수 없습니다.

| 자원 | 제한 |
|------|------|
| Disk | IOPS, throughput, queue depth |
| Network | bandwidth, socket buffer |
| File | file descriptor |
| Device | driver queue |

I/O 자원 부족 시 CPU가 남아 있어도 서비스는 느려질 수 있습니다.

### 자원 회수

할당된 자원은 사용 후 반드시 회수되어야 합니다.

| 자원 | 회수 방식 |
|------|-----------|
| CPU | time slice 종료 |
| Stack | 함수 반환 |
| Heap | free 또는 GC |
| File Descriptor | close |
| Socket | close |
| Lock | unlock |

회수 실패는 memory leak, FD leak, connection leak, lock leak으로 이어집니다.

### 공정성

운영체제는 **처리량 + 응답성 + 공정성**을 함께 고려합니다. 공정성이 깨지면 starvation이 발생합니다.

### 우선순위

| 작업 | 이유 |
|------|------|
| 인터럽트 처리 | 지연 허용 낮음 |
| 실시간 작업 | deadline 중요 |
| 커널 작업 | 시스템 안정성 |
| 사용자 작업 | 일반 처리 |

잘못된 우선순위는 priority inversion을 만들 수 있습니다.

### 격리와 제한

컨테이너와 가상화 환경에서는 자원 할당이 더 명시적입니다.

| 기술 | 역할 |
|------|------|
| cgroup | CPU/Memory 제한 |
| namespace | 자원 시야 격리 |
| quota | 사용량 상한 |
| limit | 강제 제한 |
| request | 예약 기준 |

컨테이너의 자원 할당은 **Linux 커널 자원 제어 위에서 동작**합니다.

### Backpressure

자원 부족이 감지되면 상위 계층으로 압력을 전달해야 합니다.

- CPU 부족 → 요청 제한
- Memory 부족 → 큐 제한
- I/O 부족 → rate limit

Backpressure가 없으면 무제한 요청 수용 → 큐 폭증 → 메모리 증가 → 장애로 이어집니다.

---

## 7. Linux/Runtime/K8s에서 관측 방법

### Linux

```bash
# CPU 할당 관측
top
htop
mpstat -P ALL 1
vmstat 1
pidstat -w

# 메모리 할당 관측
free -h
cat /proc/meminfo
cat /proc/<pid>/smaps
pmap <pid>

# I/O 할당 관측
iostat -x 1
iotop
ss -s
ss -m

# 프로세스 자원
ulimit -a
lsof -p <pid>
cat /proc/<pid>/limits
```

### Runtime

| 현상 | 의미 |
|------|------|
| thread pool queue 증가 | CPU 또는 worker 부족 |
| heap 증가 | 메모리 할당 증가 |
| GC time 증가 | 메모리 회수 압박 |
| connection pool 고갈 | 외부 자원 부족 |
| request timeout 증가 | 자원 대기 증가 |

### Kubernetes

| 항목 | 의미 |
|------|------|
| requests.cpu | CPU 예약 기준 |
| limits.cpu | CPU 사용 상한 |
| requests.memory | 메모리 예약 기준 |
| limits.memory | 메모리 강제 상한 |
| QoS Class | 자원 보장 수준 |

```bash
kubectl top pod
kubectl top node
kubectl describe pod
```

**주요 현상:**

| 현상 | 의미 |
|------|------|
| OOMKilled | memory limit 초과 |
| CPU Throttling | CPU limit에 막힘 |
| Pending Pod | 노드에 자원 부족 |
| Eviction | 노드 자원 압박 |
| CrashLoopBackOff | 자원 부족으로 반복 실패 가능 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*