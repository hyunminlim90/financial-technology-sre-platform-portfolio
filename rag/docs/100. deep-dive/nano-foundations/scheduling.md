# 스케줄링 (Scheduling)

> 정독: 0회

## 1. 이 기술이 무엇인가

스케줄링은:

> **운영체제가 제한된 CPU 코어를 여러 실행 흐름에 배분하는 제어 메커니즘**

실제 스케줄링 대상은 대부분 프로세스보다 **스레드(thread)**입니다.

### 핵심 역할

| 역할 | 설명 |
|------|------|
| 실행 순서 결정 | 누가 먼저 CPU 사용 |
| 실행 시간 배분 | 얼마나 오래 실행 |
| 우선순위 적용 | 중요 작업 우대 |
| 공정성 유지 | starvation 방지 |
| context switch 수행 | 실행 흐름 교체 |

결국 **CPU 코어 점유권을 시간 단위로 관리**하는 시스템입니다.

<details>
<summary>Deep Dive</summary></br>

Operating System(운영체제) [[M]](../../100-deep-dive/micro-foundations/operating-system.md)  
Limited Physical Processor Resources(제한된 물리적 프로세서 자원) [[M]](../../100-deep-dive/micro-foundations/limited-physical-processor-resources.md)  
Thread(스레드) [[M]](../../100-deep-dive/micro-foundations/thread.md)  
Resource Allocation(자원 할당) [[M]](../../100-deep-dive/micro-foundations/resource-allocation.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

스케줄링은 거의 모든 실행 계층에 존재합니다.

| 계층 | 역할 |
|------|------|
| OS Kernel | CPU scheduler |
| Hypervisor | vCPU scheduling |
| Container Runtime | cgroup CPU quota |
| Database | worker scheduling |
| Runtime | thread pool scheduling |
| NIC | packet queue scheduling |
| Disk I/O | I/O scheduler |

특히 **운영체제 커널의 scheduler subsystem**이 핵심입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 대상은 **CPU 자원**이지만, 실제 영향은 광범위합니다.

| 자원 | 영향 |
|------|------|
| CPU | core utilization |
| Memory | cache locality |
| Network | packet processing delay |
| Disk | I/O wait latency |

> **과도한 scheduling은 cache 효율을 파괴**합니다.

---

## 4. 왜 중요한가

현대 시스템은 **실행 흐름 수 >> CPU 코어 수** 상태입니다.

누가 언제 실행되는지 결정하지 않으면 시스템 전체가 비효율화됩니다.

### 스케줄링 품질이 직접 영향을 주는 요소

| 요소 | 영향 |
|------|------|
| latency | 응답 속도 |
| throughput | 처리량 |
| fairness | 공정성 |
| tail latency | 지연 폭증 |
| CPU efficiency | 자원 활용 |
| cache locality | 메모리 효율 |

---

## 5. 실제 장애와 어떤 관련이 있는가

| 문제 | 설명 |
|------|------|
| CPU starvation | 특정 작업 굶주림 |
| Run queue explosion | runnable thread 과다 |
| Context switch storm | switch 과다 |
| CPU thrashing | scheduler 과부하 |
| Priority inversion | 낮은 우선순위가 자원 점유 |
| Scheduler latency | dispatch 지연 |
| CPU throttling | quota 제한 |
| Noisy neighbor | shared CPU contention |

> 실무에서는 **CPU 사용률보다 scheduler pressure가 더 중요한 경우**가 많습니다.

예를 들어 runnable thread 500개에 core가 8개이면, 대부분의 thread는 Ready 상태에서 대기하게 됩니다.

---

## 6. 핵심 메커니즘

### Ready Queue

실행 가능한 thread들은 **ready queue에서 대기**합니다. scheduler는 여기서 다음 실행 대상을 선택합니다.

### Dispatch

scheduler 결정 이후 **실제 CPU core에 thread를 배치**하는 단계입니다. dispatch 시 register, PC, stack pointer를 복원합니다.

### Context Switch

현재 thread 실행 상태를 저장한 후 다른 thread 상태를 복원합니다.

**저장 대상:**

| 요소 | 설명 |
|------|------|
| register set | CPU state |
| PC | instruction 위치 |
| stack pointer | stack 위치 |
| flags | CPU 상태 |

**발생 비용:**

| 문제 | 영향 |
|------|------|
| cache flush | locality 감소 |
| TLB miss | memory latency 증가 |
| pipeline reset | execution stall |

과도한 context switch는 **CPU 낭비**로 이어집니다.

### Time Slice

scheduler는 CPU 사용 시간을 **작은 단위(보통 수 ms)로 분할**합니다. slice 종료 시 preemption이 가능합니다.

### Preemptive Scheduling

현대 OS의 기본 방식으로, 현재 thread가 실행 중이어도 **강제로 CPU를 회수**할 수 있습니다. fairness와 responsiveness 유지가 핵심 이유입니다.

### Non-Preemptive Scheduling

과거 방식으로, thread가 자발적으로 CPU를 반납하기 전까지 계속 실행됩니다. CPU monopolization이 발생할 수 있습니다.

### Scheduling Policy

| 정책 | 특징 |
|------|------|
| FCFS | 단순 순서 |
| Round Robin | 시분할 |
| Priority Scheduling | 우선순위 기반 |
| MLFQ | 동적 priority |
| CFS | Linux 현대 scheduler |

### Linux CFS

현대 Linux의 핵심 scheduler로, **완벽한 공정 실행 시간 분배**를 목표로 합니다.

| 개념 | 설명 |
|------|------|
| vruntime | 가상 실행 시간 |
| red-black tree | runnable 관리 |
| fairness | CPU 균등 배분 |

실행이 적은 thread를 우선 선택합니다.

### CPU Affinity

thread를 특정 core에 유지하여 **cache locality를 유지**합니다. migration을 줄이면 cache hit 증가, latency 감소 효과가 있습니다.

### Load Balancing

멀티코어 환경에서 scheduler는 **core별 runnable imbalance를 조정**합니다. 일부 core만 과부하 되는 상황을 방지합니다.

### I/O Bound vs CPU Bound

| 유형 | 특징 |
|------|------|
| CPU Bound | 계속 계산 |
| I/O Bound | 자주 sleep |

I/O bound thread는 짧게 자주 실행하는 경향이 있습니다.

### Blocking

thread가 disk, network, mutex 대기 시 **Blocked 상태로 전환**되어 CPU 점유권을 반납합니다.

### Wakeup

I/O 완료 후 scheduler가 thread를 다시 Ready Queue에 투입합니다.

### Scheduler Tick

주기적 timer interrupt로 scheduler가 실행 시간을 측정하고 preemption 여부를 판단합니다.

### Real-Time Scheduling

RT scheduler는 **deadline 보장을 우선**시합니다. 일반 fairness보다 deterministic latency가 중요합니다.

### Priority Inversion

낮은 priority thread가 lock을 보유할 때 높은 priority thread가 대기하게 됩니다. 실시간 시스템의 핵심 장애 원인입니다.

### CPU Run Queue

run queue 길이가 증가하면 **CPU contention이 증가**함을 의미하는 매우 중요한 지표입니다.

### Hypervisor Scheduling

VM의 vCPU도 결국 physical core 위에서 scheduling됩니다. 따라서 **steal time**이 발생할 수 있습니다.

### NUMA Scheduling

대형 서버에서 scheduler는 memory locality를 고려해야 합니다. remote NUMA access는 **latency 증가**를 유발합니다.

---

## 7. Linux/Runtime/K8s에서 관측 방법

### Linux

```bash
# run queue
uptime
vmstat 1

# context switch
vmstat 1
pidstat -w

# scheduler 정보
cat /proc/schedstat

# per-thread 상태
top -H
htop

# CPU affinity
taskset -p <pid>

# real-time priority
chrt -p <pid>

# perf scheduler trace
perf sched

# eBPF tracing
bcc
bpftrace
```

### Runtime

| 현상 | 의미 |
|------|------|
| runnable 증가 | CPU 부족 |
| blocking 증가 | I/O bottleneck |
| lock contention | scheduler delay |
| thread explosion | oversubscription |

> **thread 수 ≠ 실제 병렬성**

코어보다 thread가 많으면 scheduling overhead가 증가합니다.

### Kubernetes

K8s도 결국 Linux scheduler 기반으로 동작합니다.

| 요소 | 설명 |
|------|------|
| cpu request | scheduler reservation |
| cpu limit | quota 제한 |
| cgroup | CPU control |
| throttling | 실행 제한 |

```bash
# pod 리소스 확인
kubectl top pod
kubectl describe pod

# 노드 레벨 관측
top
mpstat
pidstat
```

CPU throttling 발생 시 Ready 상태인데도 실행이 불가능한 상황이 생길 수 있습니다.

### Observability 핵심 지표

| 지표 | 의미 |
|------|------|
| load average | runnable pressure |
| context switch/sec | scheduler overhead |
| run queue length | CPU contention |
| CPU steal | hypervisor contention |
| throttled time | cgroup 제한 |
| latency percentile | scheduling 영향 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*