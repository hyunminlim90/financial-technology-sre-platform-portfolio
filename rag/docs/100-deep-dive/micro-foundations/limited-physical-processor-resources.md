# 제한된 물리적 프로세서 자원

> 정독: 0회

## 1. 이 기술이 무엇인가

제한된 물리적 프로세서 자원은:

> **동시에 실행하려는 소프트웨어 실행 흐름 수가 실제 CPU 코어 수를 초과하는 상태**를 의미

핵심은 스레드는 많이 생성할 수 있지만, **실제 연산 가능한 물리 코어 수는 제한**된다는 점입니다.

| 항목 | 개수 |
|------|------|
| 활성 스레드 | 2,000 |
| 물리 코어 | 8 |
| 실제 동시 실행 가능 수 | 최대 8 |

> **대부분의 스레드는 실행 대기 상태에 머무릅니다.**

---

## 2. 시스템 어디에서 등장하는가

거의 모든 현대 시스템에서 등장합니다.

| 영역 | 설명 |
|------|------|
| OS Kernel | CPU scheduling |
| Database | worker thread 경쟁 |
| Web Server | request thread 경쟁 |
| Runtime | thread pool oversubscription |
| Kubernetes | CPU quota contention |
| Hypervisor | vCPU overcommit |
| NIC interrupt | softirq CPU 경쟁 |

**멀티스레드 시스템에서 항상 존재하는 근본 제약**입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **CPU**이지만, 연쇄 영향이 큽니다.

| 자원 | 영향 |
|------|------|
| CPU | core contention |
| Memory | cache thrashing |
| Network | packet delay |
| Disk | I/O scheduling delay |

> **CPU 부족은 전체 시스템 지연으로 전파**됩니다.

---

## 4. 왜 중요한가

현대 시스템 대부분의 병목은 CPU 자체보다 **CPU 경쟁 상태**에서 발생합니다.

| 현상 | 결과 |
|------|------|
| runnable 증가 | latency 증가 |
| context switch 증가 | CPU 낭비 |
| queue accumulation | 응답 폭증 |
| throttling | 처리량 감소 |

> **실행 흐름 수가 많다고 병렬성이 증가하는 것은 아닙니다.**

지나친 thread 생성이나 무제한 concurrency는 오히려 시스템을 느리게 만들 수 있습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

| 문제 | 설명 |
|------|------|
| CPU saturation | core 점유율 100% |
| Run queue explosion | runnable thread 폭증 |
| Scheduler latency | dispatch 지연 |
| Context switch storm | switch 과다 |
| CPU throttling | quota 제한 |
| Tail latency 증가 | 일부 요청 지연 폭증 |
| Soft lockup | scheduler starvation |
| Load average 급등 | 실행 대기 증가 |

대표적인 현상으로 **CPU 사용률은 낮은데 서비스는 느린** 경우가 있습니다. 이 경우 실제 원인은 scheduler contention, lock contention, runnable overload인 경우가 많습니다.

---

## 6. 핵심 메커니즘

### 물리 코어 수는 절대 제한됨

스레드는 논리적 실행 흐름이지만, **실제 명령어 실행은 물리 코어만 가능**합니다.

### 동시성 vs 병렬성

| 개념 | 의미 |
|------|------|
| 동시성 | 번갈아 실행 |
| 병렬성 | 실제 동시에 실행 |

thread 1,000개에 core가 8개이면 동시성은 가능하지만 **병렬성은 최대 8개**입니다.

### Ready Queue

실행 가능한 thread들은 **Ready Queue에서 대기**합니다. 코어가 부족하면 queue length가 증가합니다.

### Dispatch

scheduler가 실행 대상을 선택한 후 core를 할당합니다.

### Context Switch

실행 중인 thread를 교체할 때 다음 비용이 발생합니다: register save/restore, cache pollution, TLB 영향.

과도한 context switch는 **CPU 시간을 실제 연산 대신 관리 비용으로 소비**하게 만듭니다.

### Oversubscription

코어보다 thread 수가 과도하게 많은 상태입니다.

| core | thread |
|------|--------|
| 8 | 5,000 |

결과로 scheduler overload, cache miss 증가, latency 증가가 발생합니다.

### Time Slice

scheduler는 CPU 시간을 잘게 나눠 분배합니다. slice 종료 후 강제 preemption이 가능합니다.

### Hyper-Threading

논리 CPU 수 ≠ 물리 코어 수입니다.

| 물리 코어 | 논리 CPU |
|-----------|----------|
| 8 | 16 |

SMT/HT는 효율을 높이지만 **물리 코어 증가와 동일하지 않습니다.**

### CPU Saturation

다음 조건일 때 발생합니다: `Runnable thread > available CPU execution contexts`

이 순간 대기 증가, latency 증가, throughput 감소가 나타납니다.

### Load Average

Linux의 핵심 지표로, 단순 CPU 사용률이 아니라 **실행 대기 + uninterruptible wait을 포함**합니다.

| 상황 | 의미 |
|------|------|
| load 1 | 코어 1개 수준 |
| load 32 | 매우 높은 경쟁 |
| load > core 수 | saturation 가능성 |

### CPU Affinity

thread를 특정 코어에 유지하여 **cache locality를 유지**합니다. migration 과다 시 cache miss가 증가합니다.

### NUMA 영향

대형 서버에서 remote NUMA node 접근 시 memory latency가 증가합니다. scheduler는 NUMA locality를 고려해야 합니다.

### Interrupt Competition

CPU는 user thread, kernel thread, interrupt, softirq를 모두 처리합니다. 따라서 **실제 사용자 thread가 사용할 CPU는 더 적습니다.**

### cgroup CPU Limit

Kubernetes/container 환경에서는 CPU limit 때문에 **runnable 상태인데도 실행하지 못하는** 상황이 발생할 수 있습니다.

### Backpressure

CPU 부족 시 핵심 전략으로, 실행 흐름을 무제한 생성하지 않고 queue 제한, rate limit, bounded concurrency를 적용합니다.

### Work Queue Explosion

request 폭증 시 thread 폭증 → runnable 폭증 → scheduler collapse가 발생할 수 있습니다.

### Thread Pool Sizing

CPU bound 작업의 일반적 기준: `thread 수 ≈ core 수`. I/O bound는 더 많아질 수 있습니다.

---

## 7. Linux/Runtime/K8s에서 관측 방법

### Linux

```bash
# CPU 상태
top
htop
mpstat -P ALL 1

# run queue
vmstat 1
uptime

# context switch
pidstat -w
vmstat 1

# per-thread
top -H
ps -eLf

# scheduler trace
perf sched

# CPU affinity
taskset

# NUMA 상태
numactl --hardware
numastat

# interrupt 확인
cat /proc/interrupts
```

### Runtime

| 현상 | 의미 |
|------|------|
| runnable 증가 | CPU 경쟁 |
| thread 폭증 | oversubscription |
| queue 증가 | backpressure 부족 |
| lock wait 증가 | scheduler contention |

### Kubernetes

```bash
# pod 리소스 확인
kubectl top pod
kubectl describe pod

# CPU throttling 지표
container_cpu_cfs_throttled_seconds_total

# cgroup quota
cat /sys/fs/cgroup/cpu.max

# 노드 압박
kubectl top node
```

### Observability 핵심 지표

| 지표 | 의미 |
|------|------|
| CPU utilization | 사용률 |
| Run queue length | 경쟁 상태 |
| Context switches/sec | scheduler 비용 |
| Load average | saturation |
| CPU steal | hypervisor contention |
| CPU throttled time | cgroup 제한 |
| Scheduler latency | dispatch delay |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*