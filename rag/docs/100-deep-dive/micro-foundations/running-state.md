# 실행 상태 (Running State)

> 정독: 0회

## 1. 이 기술이 무엇인가

실행 상태는:

> 프로세스 또는 스레드가 실제 CPU 코어를 점유[(여기까지 진행됨 스케줄링)하여 명령어를 수행 중인 상태

메모리에 존재하는 것만으로는 실행 상태가 아닙니다.

| 상태 | 의미 |
|------|------|
| Ready | 실행 가능하지만 CPU 없음 |
| Running | CPU 점유 중 |
| Waiting/Blocked | 이벤트 대기 |
| Terminated | 종료 |

실행 상태에서는 실제로 명령어 fetch / decode / execute / memory access / register update가 CPU 내부에서 수행됩니다.

<details>
<summary>Deep Dive</summary></br>

Process(프로세스) [[M]](../../100-deep-dive/micro-foundations/process.md)  
Thread(스레드) [[M]](../../100-deep-dive/micro-foundations/thread.md)  
Processing Core(프로세싱 코어) [[M]](../../100-deep-dive/micro-foundations/processing-core.md)  
Scheduling(스케줄링) [[M]](../../100-deep-dive/micro-foundations/scheduling.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

실행 상태는 운영체제 전체의 핵심 동작 지점입니다.

| 계층 | 역할 |
|------|------|
| CPU Scheduler | 실행 대상 선택 |
| Kernel | context switch |
| Runtime | instruction execution |
| Thread System | runnable thread dispatch |
| Hypervisor | vCPU scheduling |
| Container Runtime | process execution |

특히 스케줄러와 CPU 코어 사이에서 핵심적으로 등장합니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**가장 직접적 영향: CPU**

| 자원 | 영향 |
|------|------|
| Memory | cache/TLB/page access |
| Disk | blocking I/O |
| Network | socket wait |
| Scheduler | run queue pressure |

실행 상태가 길어질수록: CPU utilization 증가 / thermal 증가 / scheduler pressure 증가 발생 가능

---

## 4. 왜 중요한가

실제 연산은 Running 상태에서만 수행됩니다. 프로세스가 메모리에 존재해도 CPU를 받지 못하면 실행되지 않고 단지 대기 상태입니다.

> 시스템 성능의 본질은 Running 상태 분배 문제

OS는 어떤 작업을 / 얼마나 오래 / 어느 CPU에서 / 어떤 우선순위로 실행시킬지 계속 결정합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

| 문제 | 원인 |
|------|------|
| CPU Saturation | runnable overload |
| Load Average 급증 | ready queue 적체 |
| Context Switch Storm | excessive scheduling |
| Soft Lockup | CPU monopolization |
| Starvation | 특정 task 미실행 |
| High System CPU | kernel execution overload |
| Run Queue Explosion | scheduler bottleneck |

> Ready 상태는 많지만 Running으로 못 올라가는 현상이 실제 장애의 핵심

| 현상 | 의미 |
|------|------|
| load average 상승 | runnable backlog |
| latency 증가 | CPU wait |
| request timeout | scheduler delay |
| throughput 감소 | execution starvation |

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Running 상태는 매우 짧다

현대 OS는 수 ms 단위로 CPU 제어권 교체를 수행합니다.

```
실행 → 중단 → 저장 → 복원 (반복)
```

### Scheduler

| 기능 | 설명 |
|------|------|
| process selection | 실행 대상 선택 |
| fairness | CPU 공정 분배 |
| priority | 우선순위 반영 |
| load balancing | CPU 분산 |

| 알고리즘 | 특징 |
|----------|------|
| Round Robin | 순환 |
| Priority Scheduling | 우선순위 |
| CFS | Linux fairness 기반 |

### Context Switch

실행 중이던 프로세스를 멈추고 다른 프로세스의 실행 상태를 복원합니다.

| 요소 | 설명 |
|------|------|
| Program Counter | 다음 실행 위치 |
| Stack Pointer | stack 위치 |
| Registers | CPU 상태 |
| Flags | CPU condition |

비용 발생: cache miss / TLB flush / pipeline disruption. 과도하면 성능이 저하됩니다.

### CPU Burst

CPU-intensive workload는 긴 Running 상태를 유지하는 경향이 있습니다. 반면 I/O workload는 잠깐 실행 후 곧바로 waiting으로 전환하는 패턴을 보입니다.

### State Transition

```
Ready → Running → Waiting → Ready (반복)
```

CPU는 동시에 제한된 task만 Running 가능합니다.

### Multi-Core

코어 수가 N개면 동시에 최대 N개의 task만 Running 가능합니다. 나머지는 Ready Queue에서 대기합니다.

### Kernel Mode / User Mode

| 모드 | 설명 |
|------|------|
| User Mode | application execution |
| Kernel Mode | syscall/interrupt |

system CPU 상승은 kernel running time 증가를 의미할 수 있습니다.

### Interrupt

timer interrupt / network interrupt / disk interrupt 발생 시 현재 Running 흐름을 중단하고 kernel handler를 실행합니다.

### Preemption

scheduler가 강제로 CPU를 회수할 수 있습니다. 프로세스 의사와 무관하게 실행권 박탈이 가능합니다.

### Run Queue

Ready 상태 task를 저장하는 구조입니다. run queue 길이가 길수록 CPU 부족 / latency 증가 / scheduler contention 증가가 발생합니다.

### Hyper-Threading

| 개념 | 설명 |
|------|------|
| Physical Core | 실제 연산 코어 |
| Logical CPU | SMT thread |

Running 상태는 logical CPU 기준으로 관리됩니다.

### CPU Affinity

프로세스를 특정 CPU에 묶을 수 있습니다.

이유: cache locality 유지 / NUMA 최적화 / latency 감소

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

```bash
# 실시간 상태
top
htop

# process state 확인
ps -eo pid,state,comm

# run queue 확인
vmstat 1

# scheduler 통계
pidstat -w
sar -q

# CPU usage
mpstat -P ALL 1

# context switch
pidstat -w

# interrupt
cat /proc/interrupts
```

**process state 코드:**

| 코드 | 의미 |
|------|------|
| R | Running/Runnable |
| S | Sleeping |
| D | Uninterruptible |
| Z | Zombie |

**vmstat 주요 필드:**

| 필드 | 의미 |
|------|------|
| r | runnable count |
| b | blocked task |

### Runtime

| 현상 | 의미 |
|------|------|
| runnable thread 증가 | CPU contention |
| event loop stall | execution starvation |
| high syscall time | kernel execution 증가 |
| GC pause | runtime CPU monopolization |

### Kubernetes

container 내부도 결국 Linux process scheduling 위에서 동작합니다.

| 현상 | 의미 |
|------|------|
| CPU throttling | cgroup 제한 |
| pod latency 증가 | runnable overload |
| node saturation | CPU exhaustion |
| noisy neighbor | shared CPU contention |

```bash
kubectl top pod
kubectl top node
```

| 요소 | 역할 |
|------|------|
| cpu request | scheduler reservation |
| cpu limit | throttling boundary |

CFS throttling 발생 시 Running 되고 싶어도 CPU 사용이 제한되는 상태가 발생합니다.

### Observability

| 도구 | 목적 |
|------|------|
| top/htop | runtime CPU |
| perf | CPU profiling |
| eBPF | scheduler tracing |
| pidstat | per-process CPU |
| vmstat | run queue |
| sar | system load |

| 지표 | 의미 |
|------|------|
| CPU utilization | running ratio |
| load average | runnable pressure |
| context switches | scheduler activity |
| run queue length | CPU contention |
| system/user CPU | execution location |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*