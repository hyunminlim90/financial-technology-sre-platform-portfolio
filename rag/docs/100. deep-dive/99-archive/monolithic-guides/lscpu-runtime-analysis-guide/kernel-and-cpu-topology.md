# Kernel과 Physical Core / Logical CPU 구조

## 1. Kernel

Kernel은 운영체제(OS)의 핵심 구성 요소로, 하드웨어 자원을 관리하고 소프트웨어에 실행 환경을 제공하는 시스템 계층이다.

### 주요 역할

| 역할 | 설명 |
|------|------|
| CPU 관리 | 스레드 및 프로세스 스케줄링 |
| 메모리 관리 | 가상 메모리 및 물리 메모리 관리 |
| 장치 제어 | 디스크, 네트워크, GPU 등 하드웨어 제어 |
| 시스템 호출 처리 | 사용자 프로그램과 하드웨어 연결 |
| 자원 추상화 | 하드웨어를 논리적 자원 형태로 제공 |

---

## 2. Physical Core와 Logical CPU 개요

Kernel은 CPU 하드웨어를 직접 노출하지 않고, **Logical CPU** 형태로 추상화하여 관리한다.

```
Physical Core
  └── SMT / Hyper-Threading
        └── Logical CPU (커널 스케줄링 단위)
```

---

## 3. Physical Core

Physical Core는 실제 연산을 수행하는 물리적 CPU 실행 유닛이다.

### 내부 구성 요소

| 구성 요소 | 역할 |
|-----------|------|
| ALU | 정수 연산 |
| FPU | 부동소수점 연산 |
| LSU | 메모리 접근 |
| L1/L2 Cache | 고속 데이터 저장 |
| Branch Predictor | 분기 예측 |
| Pipeline | 명령어 실행 |

---

## 4. Hyper-Threading / SMT

현대 CPU는 하나의 Physical Core에서 여러 실행 흐름을 동시에 처리할 수 있다.

| 제조사 | 기술 이름 |
|--------|-----------|
| Intel | Hyper-Threading |
| AMD | SMT (Simultaneous Multithreading) |

### 구조 예시

```
1 Physical Core
  → 2 Hardware Threads
    → 2 Logical CPUs
```

커널은 각 Hardware Thread를 독립적인 실행 단위(Logical CPU)로 인식한다.

---

## 5. Logical CPU

Logical CPU는 커널이 스케줄링 대상으로 사용하는 논리적 CPU 실행 단위다.

Linux에서는 다음과 같이 표현된다.

```
cpu0, cpu1, cpu2, cpu3, ...
```

각 Logical CPU는 스레드를 실행할 수 있는 **실행 컨텍스트(Execution Context)**를 의미한다.

---

## 6. Kernel의 Hardware Discovery

부팅 시 커널은 다음 CPU 하드웨어 정보를 탐지한다.

| 수집 정보 | 설명 |
|-----------|------|
| Physical Socket 수 | 실제 CPU 패키지 개수 |
| Core 수 | Physical Core 개수 |
| SMT 여부 | Hyper-Threading 지원 여부 |
| Logical CPU 수 | 총 실행 컨텍스트 수 |
| NUMA 정보 | 메모리 접근 구조 |

---

## 7. Linux CPU 인식 구조

Linux는 `/proc/cpuinfo`를 통해 CPU 구조 정보를 노출한다.

```
processor : 0
physical id : 0
core id : 0

processor : 1
physical id : 0
core id : 0
```

- `processor 0` → Logical CPU 0
- `processor 1` → 동일 Physical Core의 SMT Thread (Logical CPU 1)

두 Logical CPU가 동일한 `physical id`와 `core id`를 공유하면, 같은 Physical Core에 속한 SMT Thread임을 의미한다.

---

## 8. Kernel Scheduler

Kernel은 Scheduler를 통해 스레드를 Logical CPU에 배치한다.

### 스케줄링 대상

| 대상 | 설명 |
|------|------|
| Process | 실행 중인 프로그램 |
| Thread | 실제 실행 단위 |
| Task | Linux 내부 실행 엔티티 |

### 핵심 역할

| 역할 | 설명 |
|------|------|
| CPU 배치 | 어떤 Logical CPU에서 실행할지 결정 |
| 실행 시간 분배 | CPU Time Slice 할당 |
| 우선순위 관리 | 높은 우선순위 태스크 우선 실행 |
| Load Balancing | CPU 간 부하 분산 |
| Context Switching | 실행 대상 교체 |

---

## 9. Physical Core 우선 배치 전략

Linux Scheduler는 가능한 경우 서로 **다른 Physical Core**에 스레드를 먼저 배치한다.

**이유:** 동일 Physical Core의 SMT Thread 간에는 일부 하드웨어 자원이 공유되기 때문이다.

### 공유되는 자원

| 공유 자원 | 설명 |
|-----------|------|
| ALU/FPU | 실행 유닛 |
| Cache | 일부 캐시 계층 |
| Pipeline | 실행 파이프라인 |
| Memory Bandwidth | 메모리 접근 대역폭 |

### 배치 예시 (4 Core / 8 Thread CPU)

```
Physical Core 0 → cpu0, cpu1
Physical Core 1 → cpu2, cpu3
Physical Core 2 → cpu4, cpu5
Physical Core 3 → cpu6, cpu7
```

Thread 2개 실행 시 스케줄러 우선 배치:

```
권장:  cpu0 + cpu2  (서로 다른 Physical Core)
지양:  cpu0 + cpu1  (동일 Physical Core의 SMT Thread)
```

---

## 10. Context Switching

Context Switch는 Kernel이 CPU 실행 대상을 교체하는 동작이다.

### 저장되는 정보

| 정보 | 설명 |
|------|------|
| Register 상태 | CPU 레지스터 값 |
| Program Counter | 현재 실행 위치 |
| Stack Pointer | 스택 상태 |
| Memory Context | 가상 메모리 정보 |

### 동작 흐름

```
Thread A 실행
  → Context Save (Thread A 상태 저장)
  → Thread B Context Restore (Thread B 상태 복원)
  → Thread B 실행
```

### SMT와 Context Switching

SMT 구조에서는 Hardware Thread 간 일부 상태를 병렬로 유지할 수 있어, 특정 상황에서 Context Switching 비용이 감소할 수 있다.

---

## 11. Load Average

Linux Load Average는 Scheduler 대기 큐 상태를 나타내는 지표다.

> **Load Average = 실행 중(Running) + 실행 대기 중(Runnable) Task 수**

### 해석 기준 (8 Logical CPU 시스템)

| Load Average | 의미 |
|--------------|------|
| 4 | CPU 여유 있음 |
| 8 | CPU 포화 근접 |
| 16 | CPU 과부하 상태 |

---

## 12. CPU Affinity

CPU Affinity는 특정 Thread를 특정 Logical CPU에 고정하는 기능이다.

### 목적

| 목적 | 설명 |
|------|------|
| Cache Locality 유지 | Cache Hit 증가 |
| NUMA 최적화 | 메모리 접근 최적화 |
| Context Switch 감소 | CPU 이동 최소화 |
| 지연 시간 안정화 | Latency 감소 |

### Linux 사용 예시

```bash
taskset -c 0,2 java -jar app.jar
```

---

## 13. NUMA (Non-Uniform Memory Access)

대형 서버에서는 CPU Node마다 Local Memory가 존재하는 NUMA 구조가 사용된다.

```
CPU Node 0 ↔ Local Memory 0
CPU Node 1 ↔ Local Memory 1
```

- **Local Memory 접근**: 낮은 지연 시간
- **Remote Memory 접근**: 높은 지연 시간

Kernel은 NUMA 인식 스케줄링을 통해 가능한 경우 Local Memory 접근을 우선한다.

---

## 14. CFS (Completely Fair Scheduler)

Linux CFS는 CPU 시간을 공정하게 분배하는 스케줄러다.

### Kubernetes CPU Limit과의 연결

```
Kubernetes CPU Limit
  → cgroup
    → CFS Quota
      → CPU Time 제한
```

### CPU Throttling

CFS Quota가 소진되면 Kernel은 해당 Task의 실행을 강제로 중단한다.

```
CFS Quota 소진 (Exhausted)
  → Scheduler 실행 중단
  → CPU Throttling 발생
```

---

## 15. Kubernetes에서의 CPU 자원

Kubernetes의 CPU Resource는 **Logical CPU 기준**으로 동작한다.

```yaml
resources:
  requests:
    cpu: "2"
  limits:
    cpu: "4"
```

- `cpu: "2"` → Logical CPU 2개에 해당하는 CPU 시간 보장
- `cpu: "4"` → Logical CPU 4개에 해당하는 CPU 시간 상한

---

## 16. SRE 관점 핵심 모니터링 명령어

### CPU 구조 확인

```bash
lscpu
cat /proc/cpuinfo
```

### SMT 여부 확인

```bash
# lscpu 출력 중
Thread(s) per core: 2   # SMT 활성화
Thread(s) per core: 1   # SMT 비활성화
```

### CPU 사용률

```bash
top
htop
mpstat
```

### Context Switch 모니터링

```bash
vmstat
pidstat
```

### CPU Affinity 확인

```bash
taskset -p <pid>
```

---

## 17. JVM 및 고성능 서버 설계 관점

### Thread Pool 크기

Thread Pool 크기가 Logical CPU 수를 초과하면 다음 문제가 발생한다.

```
Thread Pool > Logical CPU 수
  → Context Switching 증가
  → CPU Cache Miss 증가
  → Throughput 감소
```

**권장**: Thread Pool 크기를 Logical CPU 수 기준으로 설정한다.

### Netty / WebFlux Event Loop

논블로킹 Event Loop는 Logical CPU 수 기반으로 동작한다.

```
EventLoop 수 ≈ Logical CPU 수
```

### CPU Pinning 활용 사례

저지연이 요구되는 환경에서는 특정 Core에 Thread를 고정하기도 한다.

| 영역 | 목적 |
|------|------|
| 금융 거래 시스템 | Latency 최소화 |
| DPDK | Polling 성능 극대화 |
| Kafka | CPU Cache 유지 |
| Redis | 단일 Thread 최적화 |

---

## 18. 전체 계층 구조

```
Application Thread
  ↓
Kernel Scheduler
  ↓
Logical CPU
  ↓
SMT / Hardware Thread
  ↓
Physical Core
  ↓
ALU / FPU / LSU / Cache / Pipeline
```

---

## 19. 구성 요소 요약

| 구성 요소 | 역할 |
|-----------|------|
| Kernel | 하드웨어 자원 관리 전반 |
| Scheduler | Logical CPU에 Thread 배치 결정 |
| Physical Core | 실제 연산을 수행하는 하드웨어 유닛 |
| Logical CPU | 커널이 스케줄링 단위로 관리하는 실행 컨텍스트 |
| SMT | 하나의 Physical Core 내 다중 실행 흐름 |
| Context Switch | CPU 실행 대상 교체 동작 |
| CPU Affinity | 특정 Thread를 특정 CPU에 고정 |
| Load Average | Scheduler 대기 큐의 포화도 지표 |
| CFS | Linux의 공정 CPU 시간 분배 스케줄러 |

---

## 20. 성능 최적화 핵심 요소

```
Kernel Scheduling
+ Logical CPU 구조 이해
+ SMT 효율적 활용
+ Cache Locality 유지
+ Context Switching 최소화
+ NUMA 인식 배치
= High Throughput + Low Latency
```

이 원칙은 다음 환경의 성능 최적화와 직접 연결된다.

- Kubernetes CPU Limit 튜닝
- JVM Thread Pool 크기 설정
- Netty / WebFlux Event Loop 설계
- 금융 시스템 저지연 처리
- 대규모 트래픽 처리 아키텍처

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*