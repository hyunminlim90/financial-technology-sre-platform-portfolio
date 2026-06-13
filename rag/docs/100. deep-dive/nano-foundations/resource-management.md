# 자원 관리 (Resource Management)

> 정독: 0회

자원 관리(Resource Management)는:

> **운영체제(OS)가 CPU, 메모리, 디스크, 네트워크, 입출력 장치 같은 한정된 하드웨어 자원을 효율적·안전하게 분배하고 통제하는 시스템 관리 체계**

**핵심:**
"누가 어떤 자원을 얼마나 사용할 것인가"를 운영체제가 결정한다

---

## 1. 이 기술이 무엇인가

컴퓨터 자원은 무한하지 않습니다. CPU Core 수, DRAM 용량, SSD I/O 속도, Network 대역폭 모두 제한적입니다.

그런데 동시에 수십~수백 개의 프로세스가 실행됩니다.

그래서 운영체제는 다음을 수행합니다.

- 자원 할당
- 자원 회수
- 우선순위 조정
- 충돌 방지
- 보호 및 격리

즉, **자원 관리는 운영체제의 핵심 존재 이유**입니다.

---

## 2. 시스템 어디에서 등장하는가

자원 관리는 사실상 시스템 전체에 등장합니다.

대표 영역:

- Process Scheduling
- Virtual Memory / Page Cache
- File System / Network Stack
- DMA / Interrupt
- cgroup / Container Runtime / Hypervisor
- Kubernetes Scheduler / NUMA

즉, **모든 시스템 동작의 기반**입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향 |
|------|------|
| CPU | 매우 큼 |
| Memory | 매우 큼 |
| Disk I/O | 매우 큼 |
| Network | 매우 큼 |

운영체제는 결국 **이 4개 자원의 충돌과 경쟁을 조율**합니다.

---

## 4. 왜 중요한가

자원 관리가 없으면 CPU 독점, 메모리 침범, 디스크 충돌, 네트워크 혼잡, 시스템 다운이 발생합니다.

즉, **멀티태스킹 자체가 불가능**해집니다.

### 운영체제의 핵심 목표

| 목표 | 내용 |
|------|------|
| 효율성 | 자원을 놀리지 않음 |
| 공정성 | 특정 프로세스 독점 방지 |
| 안정성 | 프로세스 간 침범 차단 |
| 성능 최적화 | 처리량과 응답시간 개선 |

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. CPU Saturation

CPU 경쟁 과다 시 Run Queue가 증가하여 응답 지연이 발생합니다.

### 5-2. Memory Pressure

메모리 부족 시 **Swap, Page Fault, OOM**이 발생합니다.

### 5-3. Disk Bottleneck

과도한 I/O 경쟁 시 **I/O Wait**가 증가합니다. CPU는 놀고 있는데 시스템이 느려지는 현상입니다.

### 5-4. Network Congestion

대역폭 경쟁 시 **Packet Drop, Retransmission, Latency 증가**가 발생합니다.

### 5-5. Deadlock

자원을 서로 기다리면 **시스템 정지**가 발생할 수 있습니다.

### 5-6. Starvation

우선순위 불균형 시 **특정 프로세스가 영구 대기** 상태에 빠질 수 있습니다.

### 5-7. Thrashing

메모리 관리 실패 시 Page In/Page Out이 반복되어 **시스템이 먹통**이 됩니다.

### 5-8. Cache Contention

멀티코어 경쟁 시 **Cache Miss 증가 → Tail Latency 급증**이 발생합니다.

---

## 6. 핵심 메커니즘

### 6-1. CPU는 스케줄링된다

CPU는 공유 자원입니다. 운영체제가 누가 CPU를 사용할지 결정합니다.

대표 알고리즘: Round Robin, CFS, Priority Scheduling

### 6-2. 메모리는 가상화된다

프로세스는 **독립된 Address Space**를 사용하며, MMU가 `가상 주소 → 물리 주소` 변환을 수행합니다.

### 6-3. 디스크는 캐싱된다

운영체제는 **Page Cache, Buffer Cache**를 사용하여 SSD/HDD 접근을 최소화합니다.

### 6-4. 장치는 인터럽트 기반이다

I/O 장치는 완료 시 **Interrupt**를 발생시킵니다. CPU가 계속 polling하지 않도록 설계됩니다.

### 6-5. DMA가 CPU 부담을 줄인다

NIC/SSD는 **DMA**를 통해 DRAM에 직접 접근할 수 있습니다.

### 6-6. 자원은 격리된다

운영체제는 **Process Isolation, Permission, MMU Protection** 등으로 시스템을 보호합니다.

### 6-7. 모든 자원 관리에는 Queue가 있다

- Run Queue
- I/O Queue
- Network Queue
- Disk Queue

즉, **컴퓨터는 결국 Queue 시스템**입니다.

### 6-8. 자원 관리는 결국 병목 관리다

실무 핵심은 **CPU, Memory, Disk, Network** 중 어디가 포화됐는지 찾는 것입니다.

이것이 SRE/OS 운영의 핵심입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# CPU 상태
top
htop
mpstat
sar -u

# 메모리 상태
free -h
vmstat
sar -B

# 디스크 I/O
iostat
iotop
sar -d

# 네트워크 상태
sar -n DEV
iftop
ss -s

# Load Average
uptime

# 프로세스 스케줄링
ps -eo pid,pri,ni,cmd
```

### Runtime

주요 관측 대상:

- Thread Pool
- Heap Usage
- GC
- Buffer
- Connection Pool
- Queue

### Kubernetes

```bash
# Pod 자원 사용량
kubectl top pod

# Node Pressure
kubectl describe node

# OOMKilled 확인
kubectl describe pod
```

```yaml
# Resource Limit
resources:
  requests:
    cpu: "1"
    memory: "2Gi"
```

> **cgroup 기반 제한** — 컨테이너는 CPU / Memory / I/O 자원을 제한받습니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*