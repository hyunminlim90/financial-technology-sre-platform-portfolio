# Java Main Thread와 OS Scheduler / CFS 실행 구조

## 1. Java Main Thread 개요

Java Main Thread는 JVM 프로세스가 시작될 때 가장 먼저 생성되는 Java Thread입니다.

`public static void main(String[] args)` 메서드를 실행하는 주체이며, 애플리케이션 초기화와 다른 Software Thread 생성의 출발점입니다.

```
java -jar app.jar
       ↓
JVM Process 시작
       ↓
Main Thread 생성
       ↓
main() 메서드 실행
```

```
Main Thread = JVM 프로세스의 최초 실행 흐름 + main() 실행 주체 + 다른 Software Thread 생성의 시작점
```

### 기본 특징

| 항목 | 설명 |
|------|------|
| 생성 시점 | JVM 프로세스 시작 시 |
| 개수 | JVM 프로세스당 기본 1개 |
| 실행 대상 | `main(String[] args)` |
| OS 관계 | Kernel Thread와 1:1 매핑 |
| 역할 | 애플리케이션 부팅, 초기화, 다른 Thread 생성 |

---

## 2. Main Thread의 역할

### 주요 수행 작업

| 역할 | 설명 |
|------|------|
| 애플리케이션 시작 | `main()` 메서드 실행 |
| 설정 로딩 | YAML, Properties, 환경변수 로딩 |
| 객체 초기화 | Spring Bean, 설정 객체 생성 |
| 런타임 초기화 | JVM, Framework, Container 초기화 |
| Thread 생성 | Netty, Kafka, Worker Pool 등 생성 |
| 종료 흐름 관리 | Shutdown Hook, ApplicationContext 종료 |

### Spring Boot에서 Main Thread 실행 흐름

```
main()
    ↓
SpringApplication.run()
    ↓
ApplicationContext 생성
    ↓
설정 파일 로딩 → Bean 등록
    ↓
Embedded Server 시작
    ↓
Netty / Tomcat Thread 생성
    ↓
Kafka Listener Container 시작
    ↓
Worker Pool 초기화
```

### Main Thread가 직접 모든 작업을 수행하지 않는 이유

| 문제 | 설명 |
|------|------|
| Blocking 전파 | 하나의 긴 작업이 전체 실행 흐름을 막음 |
| 확장성 부족 | 동시에 여러 작업 처리 어려움 |
| 관심사 혼합 | 초기화, I/O, 메시지 처리, 작업 실행이 한 흐름에 혼재 |
| 장애 격리 어려움 | 특정 작업 지연이 전체 시스템에 직접 영향 |

Main Thread는 초기화와 실행 흐름 구성을 담당하고, 실제 런타임 작업은 목적별 Thread에게 위임합니다.

---

## 3. Main Thread와 다른 Software Thread의 관계

Main Thread는 초기화 과정에서 여러 특수 목적 Thread를 생성합니다.

```
JVM Process
├── Main Thread              → Application Bootstrap
├── Netty EventLoop Thread   → Network I/O
├── Kafka Consumer Thread    → Poll / Process / Commit
├── Worker Thread            → Business Task
├── Scheduler Thread         → Periodic Task
└── GC Thread                → Heap Management
```

### Main Thread vs 특수 목적 Thread 비교

| 구분 | Main Thread | Netty / Kafka / Worker Thread |
|------|-------------|-------------------------------|
| 생성 시점 | JVM 시작 시 | 애플리케이션 초기화 중 생성 |
| 개수 | 기본 1개 | 설정에 따라 여러 개 |
| 주요 역할 | 부팅, 설정, 초기화 | I/O, 메시지 처리, 작업 실행 |
| 실행 성격 | 초기화 중심 | 런타임 처리 중심 |
| CPU 사용 시점 | 시작 시점 집중 | 서비스 운영 중 지속 |
| 장애 영향 | 초기화 실패 시 애플리케이션 시작 실패 | 특정 기능 지연 또는 장애 |

### Main Thread 종료와 JVM 생존

JVM 프로세스는 **Non-Daemon Thread가 하나라도 살아 있으면 계속 실행**됩니다.

| 상황 | 흐름 |
|------|------|
| 일반 CLI 프로그램 | `main()` 종료 → Non-Daemon Thread 없음 → JVM 종료 |
| 서버 애플리케이션 | Netty/Kafka Thread 유지 → JVM 계속 실행 |

---

## 4. Software Thread와 CPU 실행 구조

### 전체 실행 계층

```
Software Thread
       ↓
OS Kernel Thread
       ↓
Kernel Scheduler (Linux: CFS)
       ↓
Logical CPU
       ↓
Hardware Thread
       ↓
Physical Core
       ↓
ALU / LSU / Register / Cache / Pipeline
```

### Software Thread의 CPU 사용 방식

Java Thread는 직접 CPU를 점유하지 않습니다. Kernel Thread와 1:1 매핑된 이후 CFS로부터 실행 시간을 배정받아 Logical CPU에서 실행됩니다.

```
Java Software Thread → 1:1 매핑 → OS Kernel Thread → CFS 실행 시간 배정 → Logical CPU → Physical Core
```

### Logical CPU

Logical CPU는 Linux Kernel이 스케줄링 대상으로 인식하는 최소 CPU 실행 단위입니다. 특정 순간에 하나의 Kernel Thread만 실행할 수 있습니다.

---

## 5. Time Slicing과 Context Switching

### Time Slicing

여러 Thread가 제한된 CPU를 나누어 사용하도록 운영체제가 짧은 시간 단위로 실행 대상을 교체하는 방식입니다.

```
Thread A 실행 → Thread B 실행 → Thread C 실행 → Thread A 다시 실행 → ...
```

### Quantum Time (Time Slice)

하나의 Thread가 CPU를 연속으로 사용할 수 있는 시간 단위입니다.

```
Kernel Thread → Time Slice 부여 → Logical CPU 실행 → 시간 만료 → Context Switch
```

실제 CPU 실행 권한을 받는 주체는 Software Thread가 아니라, 1:1로 매핑된 **Kernel Thread**입니다.

### Context Switching

Time Slice가 끝나거나 더 높은 우선순위 작업이 필요하면 발생합니다.

```
Thread A 실행 중
      ↓
Register / PC / Stack Pointer 저장
      ↓
Thread B 상태 복원
      ↓
Thread B 실행
```

Context Switch가 많아지면 CPU가 실제 업무 처리보다 Thread 교체에 더 많은 비용을 사용합니다.

### Thread 상태와 CPU 사용

CPU를 실제로 경쟁하는 주요 대상은 **RUNNABLE 상태의 Thread**입니다.

| 상태 | CPU 사용 여부 |
|------|--------------|
| RUNNABLE | CPU 실행 중 또는 실행 대기 |
| BLOCKED | Lock 대기 (CPU 미사용) |
| WAITING | 이벤트 대기 (CPU 미사용) |
| TIMED_WAITING | 시간 제한 대기 (CPU 미사용) |
| TERMINATED | 종료 |

---

## 6. Linux CFS (Completely Fair Scheduler)

### CFS 개요

| 개념 | 설명 |
|------|------|
| Kernel Scheduler | 운영체제의 CPU 배치 기능 (개념) |
| CFS | Linux에서 사용하는 구체적인 스케줄링 알고리즘 |

CFS의 핵심 목표는 실행 가능한 Thread들에게 **CPU 사용 기회를 공정하게 배분**하는 것입니다.

### vruntime

CFS는 각 Thread가 CPU를 얼마나 사용했는지 `vruntime`으로 추적합니다.

```
vruntime 낮음 = CPU를 상대적으로 적게 사용
→ 다음 실행 대상으로 우선 선택
```

### Red-Black Tree

CFS는 실행 가능한 Thread들을 Red-Black Tree로 관리하여 `vruntime`이 가장 낮은 Thread를 효율적으로 선택합니다.

### CFS Time Slice 관련 커널 파라미터

CFS의 Time Slice는 고정값이 아니라 실행 가능한 Thread 수, 우선순위 등에 따라 **동적으로 결정**됩니다.

| 파라미터 | 설명 |
|----------|------|
| `kernel.sched_latency_ns` | 모든 Runnable Task가 한 번씩 실행될 목표 주기 |
| `kernel.sched_min_granularity_ns` | Task가 최소한 실행될 시간 단위 (Context Switch 과다 방지) |
| `kernel.sched_wakeup_granularity_ns` | Wakeup Preemption 민감도 |

**동적 계산 예시:**

```
sched_latency_ns = 24ms, Runnable Thread = 4개
→ 각 Thread 실행 시간 ≈ 24ms / 4 = 6ms

sched_latency_ns = 24ms, Runnable Thread = 100개
→ 24ms / 100 = 0.24ms → sched_min_granularity_ns 적용
```

**파라미터 확인 명령어:**

```bash
sysctl kernel.sched_latency_ns
sysctl kernel.sched_min_granularity_ns
sysctl kernel.sched_wakeup_granularity_ns
```

> 일부 배포판 또는 커널 설정에서는 해당 항목이 노출되지 않을 수 있습니다.
> CFS 관련 커널 파라미터는 시스템 전체에 영향을 주므로, 일반적인 운영 환경에서는 직접 수정하지 않는 것이 일반적입니다.

### Logical CPU 8개, RUNNABLE Thread 200개 예시

```
동시 실행 가능: 8개
실행 대기: 192개
→ CFS가 Time Slice로 200개 Thread에 순차적으로 CPU 시간 배분
```

---

## 7. CPU Throttling과 Kubernetes CPU Limit

### CFS Bandwidth Control

Kubernetes CPU Limit은 Linux cgroup의 CFS Bandwidth Control과 연결됩니다.

```
resources.limits.cpu
       ↓
cgroup
       ↓
CFS Bandwidth Control
       ↓
CPU Quota 제한
```

| 항목 | 설명 |
|------|------|
| Period | CPU 사용량을 계산하는 주기 |
| Quota | 해당 주기 안에서 사용할 수 있는 CPU 시간 |
| Throttling | Quota 소진 후 다음 Period까지 실행 제한 |

### CPU Limit 예시

```yaml
resources:
  limits:
    cpu: "500m"   # 1 CPU의 50% 시간 사용 가능
```

### Thread 수와 CPU Limit의 관계

Thread 수가 많더라도 cgroup Quota가 작으면 모든 Thread가 충분히 실행될 수 없습니다.

```
Thread 200개 + CPU Limit 500m → Quota 빠르게 소진 → Throttling 증가 → Latency 증가
```

### Time Slice vs CPU Quota 비교

| 구분 | Time Slice / Quantum | CPU Quota |
|------|---------------------|-----------|
| 목적 | Thread 간 CPU 교대 | cgroup 단위 CPU 사용량 제한 |
| 관리 주체 | CFS | cgroup / CFS Bandwidth |
| 적용 범위 | 개별 Thread | Process / Container 그룹 |
| 설정 성격 | 동적 계산 | 명시적 제한 |
| Kubernetes 연관 | 직접 설정 드묾 | `resources.limits.cpu` |

---

## 8. CPU Affinity

CPU Affinity는 특정 Thread 또는 Process를 특정 CPU에서만 실행되도록 제한하는 설정입니다.

```bash
taskset -c 0,1 java -jar app.jar
```

| 목적 | 설명 |
|------|------|
| Cache Locality 유지 | 같은 CPU에서 반복 실행으로 Cache 재활용 |
| Context 이동 감소 | CPU 간 이동 최소화 |
| Latency 안정화 | 실행 위치 예측 가능성 증가 |

---

## 9. Software Thread별 CPU 시간 사용

| Software Thread | CPU 시간 동안 수행하는 작업 | CPU 시간 부족 시 현상 |
|-----------------|----------------------------|-----------------------|
| Main Thread | 부팅, 설정, Bean 초기화 | 시작 지연 |
| Netty EventLoop | Socket Read/Write, Handler 실행 | 네트워크 응답 지연 |
| Kafka Consumer | Poll, Deserialize, Process, Commit | Consumer Lag 증가 |
| Worker Thread | 비즈니스 로직 처리 | 작업 대기열 증가 |
| GC Thread | Heap 탐색, 객체 회수 | GC 지연, STW 증가 |
| Scheduler Thread | 주기 작업 실행 | 배치/주기 작업 지연 |

---

## 10. 운영(SRE) 관점

### 주요 모니터링 지표

| 지표 | 의미 |
|------|------|
| Thread Count | JVM 전체 Thread 수 |
| RUNNABLE Thread 수 | CPU 경쟁 중인 Thread 수 |
| Context Switch Rate | Thread 교체 빈도 |
| CPU Usage | 실제 CPU 사용량 |
| CPU Throttling | cgroup Quota 제한 여부 |
| Load Average | 실행 대기 작업 수 |
| Run Queue Length | CPU 대기열 |
| GC Pause Time | JVM 정지 시간 |
| Kafka Consumer Lag | 메시지 소비 지연 |
| Netty EventLoop Latency | 이벤트 처리 지연 |
| Thread Pool Queue Length | Worker 처리 지연 |

### 주요 장애 패턴

| 패턴 | 원인 | 결과 |
|------|------|------|
| Thread 수 과다 | RUNNABLE Thread 증가 | Context Switch / Cache Miss 증가 → Latency 증가 |
| CPU Limit 과소 설정 | Quota 빠르게 소진 | CFS Throttling → Netty / Kafka / Worker 모두 지연 |
| EventLoop Blocking | EventLoop에서 Blocking 작업 수행 | Time Slice 내 다른 Channel 이벤트 처리 불가 |
| Kafka Consumer 지연 | CPU 시간 부족 또는 Blocking | Poll 지연 → Heartbeat 지연 → Rebalance |
| GC Thread CPU 경쟁 | Heap 압박 → GC Thread CPU 사용 증가 | Application Thread CPU 시간 감소 → Latency 증가 |

### SRE 우선 조정 대상

일반적인 운영 환경에서는 커널 파라미터보다 다음 항목을 먼저 조정합니다.

| 조정 대상 | 설명 |
|-----------|------|
| Kubernetes CPU Request/Limit | 컨테이너 CPU 보장/제한 |
| Thread Pool Size | 애플리케이션 Thread 수 |
| Queue Size | 작업 대기열 |
| Blocking 분리 | I/O 대기 격리 |
| Pod 분산 | Node 부하 분산 |

### 실전 확인 명령어

```bash
# CPU / Load 확인
top / htop / uptime
mpstat -P ALL 1

# Thread 단위 확인
top -H -p <PID>
ps -L -p <PID>

# Context Switch 확인
vmstat 1
pidstat -w -p <PID> 1

# JVM Thread Dump
jstack <PID>

# Thread Dump에서 Main Thread 확인
"main" #1 prio=5
```

```bash
# Kubernetes CPU Throttling 확인 (Prometheus)
container_cpu_cfs_throttled_periods_total
container_cpu_cfs_periods_total
container_cpu_cfs_throttled_seconds_total
```

---

## 11. 핵심 정리

| 구성 요소 | 역할 |
|-----------|------|
| Main Thread | JVM 시작 후 `main()` 실행 및 다른 Thread 생성 |
| Software Thread | 애플리케이션 실행 흐름 |
| Kernel Thread | OS가 CPU에 배치하는 실행 단위 |
| Logical CPU | Kernel Scheduler가 인식하는 CPU 실행 단위 |
| Physical Core | 실제 연산 자원 |
| CFS | Linux의 공정 스케줄링 알고리즘 |
| vruntime | CPU 사용량 추적 값 |
| Time Slice | Thread가 CPU를 사용하는 실행 시간 단위 |
| Context Switch | 실행 Thread 교체 |
| CPU Quota | cgroup 단위 CPU 사용 제한 |
| CPU Throttling | Quota 소진으로 실행 제한 |

### 결론

```
Software Thread → Kernel Thread → CFS Scheduling → Logical CPU → Physical Core
```

CPU Core 수보다 Thread 수가 많으면 CFS가 Time Slice를 통해 CPU 시간을 나누어 배분합니다. Thread 수 과다 또는 Kubernetes CPU Limit 과소 설정 시 다음 문제가 발생합니다.

```
Thread 경쟁 증가 + Context Switch 증가 + Cache Miss 증가 + CPU Throttling 증가
= Latency 증가 + Throughput 저하
```

JVM 기반 서버 운영에서는 Main Thread 자체보다, Main Thread가 생성한 **Netty EventLoop / Kafka Consumer / Worker Pool / GC Thread들이 제한된 CPU 자원을 어떻게 나누어 사용하는지** 관찰하는 것이 핵심입니다.

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*