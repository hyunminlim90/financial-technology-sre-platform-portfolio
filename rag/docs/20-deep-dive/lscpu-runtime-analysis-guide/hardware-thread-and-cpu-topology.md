# Hardware Thread와 Physical Core / Logical CPU 구조

## 1. 개요

Hardware Thread는 Physical Core와 Logical CPU 사이를 연결하는 하드웨어 실행 단위다. 운영체제가 독립 실행 흐름으로 인식할 수 있는 최소 하드웨어 실행 컨텍스트(Hardware Execution Context)를 의미한다.

```
Physical Core
  ↓
Hardware Thread
  ↓
Logical CPU
  ↓
OS Scheduler
```

---

## 2. 세 계층의 정의

| 구성 요소 | 계층 | 정의 |
|-----------|------|------|
| Physical Core | Hardware Resource | 실제 연산을 수행하는 하드웨어 자원의 집합 |
| Hardware Thread | Hardware Execution Context | Physical Core 내부에서 독립 실행 상태를 유지하는 단위 |
| Logical CPU | OS Logical Layer | 운영체제 커널이 스케줄링 단위로 관리하는 논리적 실행 단위 |

---

## 3. Physical Core의 구성

Physical Core는 실제 연산 자원의 집합이다. Hardware Thread는 이 자원을 사용하는 실행 상태 단위다.

| 구성 요소 | 역할 |
|-----------|------|
| ALU | 정수 연산 |
| FPU | 부동소수점 연산 |
| LSU | 메모리 접근 |
| Pipeline | 명령어 실행 |
| L1/L2 Cache | 고속 캐시 |

---

## 4. Hardware Thread의 구조

### 독립 유지 상태

각 Hardware Thread는 다음 상태를 독립적으로 유지한다.

| 구성 요소 | 역할 |
|-----------|------|
| Register Set | 스레드 실행 상태 |
| Program Counter | 다음 명령어 위치 |
| Thread Context | 실행 문맥 전체 |
| Pipeline State 일부 | 실행 상태 일부 |

### 공유 자원

동일 Physical Core 내부의 Hardware Thread들은 다음 자원을 공유한다.

| 자원 | 설명 |
|------|------|
| ALU / FPU | 연산 실행 유닛 |
| Pipeline | 명령 실행 구조 |
| Cache 일부 | L1/L2 캐시 일부 |
| Memory Bandwidth | 메모리 접근 대역폭 |

---

## 5. SMT 구조

SMT(Simultaneous Multithreading)는 하나의 Physical Core에서 여러 Hardware Thread를 동시에 실행할 수 있게 하는 기술이다.

| 제조사 | 기술 이름 |
|--------|-----------|
| Intel | Hyper-Threading |
| AMD | SMT |

```
Physical Core
  ├── Hardware Thread 0 → Logical CPU 0
  └── Hardware Thread 1 → Logical CPU 1
```

운영체제는 각 Hardware Thread를 독립된 Logical CPU로 추상화하여 인식한다.

### Linux에서의 표현

```
processor : 0    → Logical CPU 0 (Hardware Thread 0)
processor : 1    → Logical CPU 1 (Hardware Thread 1)

physical id : 0  → 동일 CPU 패키지
core id     : 0  → 동일 Physical Core
```

`physical id`와 `core id`가 같은 두 `processor` 항목이 존재하면, 해당 Physical Core에 SMT가 적용된 것이다.

---

## 6. Hardware Thread의 목적

Hardware Thread의 핵심 목적은 **Physical Core 활용률 향상**이다.

단일 실행 흐름만 존재할 때 다음 상황에서 실행 유닛이 유휴 상태가 된다.

- Cache Miss 후 메모리 대기
- Pipeline Stall 발생
- Branch Misprediction 후 대기

SMT 구조에서는 한 Hardware Thread가 대기하는 동안 다른 Hardware Thread가 유휴 실행 유닛을 사용할 수 있다.

```
Hardware Thread 0: Memory Wait (Stall)
Hardware Thread 1: ALU 연산 실행  ← 유휴 유닛 활용
```

---

## 7. SMT의 장점과 한계

### 장점

| 효과 | 설명 |
|------|------|
| Pipeline Utilization 증가 | 유휴 실행 슬롯 활용 |
| Throughput 증가 | 병렬 실행 처리량 향상 |
| Memory Wait 숨김 | Stall 시간 동안 다른 Thread 실행 |
| CPU Utilization 향상 | 하드웨어 자원 사용률 극대화 |

### 한계

SMT는 Physical Core 수를 증가시키지 않는다. 공유 자원 경쟁이 존재하기 때문에 성능이 2배가 되지 않는다.

```
1 Physical Core + SMT ≠ 2 Independent Physical Cores
```

---

## 8. Resource Contention (자원 경쟁)

동일 Physical Core의 Hardware Thread들이 공유 자원을 동시에 사용하면 성능 간섭이 발생한다.

| 경쟁 자원 | 영향 |
|-----------|------|
| ALU / FPU | 연산 처리 지연 |
| Cache | Cache Miss 증가 |
| Pipeline | IPC 감소 |
| Memory Bandwidth | Throughput 감소 |

### Noisy Neighbor 문제

한 Hardware Thread의 자원 집중 사용이 동일 Physical Core의 다른 Hardware Thread 성능을 저하시킨다.

```
Thread 0: 높은 ALU 사용률
  → 공유 실행 유닛 점유
  → Thread 1: 실행 지연 발생
```

---

## 9. Kernel Scheduler와 Hardware Thread

Linux Scheduler는 Logical CPU 단위로 스레드를 배치한다. Hardware Thread는 직접 스케줄링 대상이 아니며, Logical CPU로 추상화된 형태로 Scheduler에 노출된다.

```
Application Thread
  ↓
Kernel Scheduler
  ↓
Logical CPU (Hardware Thread의 OS 추상화)
  ↓
Hardware Thread
  ↓
Physical Core
```

### Core-Aware 배치 전략

Linux Scheduler는 가능한 경우 서로 **다른 Physical Core**에 스레드를 우선 배치하여 SMT 자원 경쟁을 줄인다.

```
4 Core / 8 Thread 시스템:
  Physical Core 0 → cpu0, cpu1
  Physical Core 1 → cpu2, cpu3
  Physical Core 2 → cpu4, cpu5
  Physical Core 3 → cpu6, cpu7

Thread 2개 실행 시:
  권장: cpu0 + cpu2  (서로 다른 Physical Core)
  지양: cpu0 + cpu1  (동일 Physical Core의 SMT Thread)
```

---

## 10. Kubernetes에서의 Hardware Thread 영향

Kubernetes CPU Resource는 Logical CPU 기준으로 동작한다.

```yaml
resources:
  limits:
    cpu: "1"
```

동일한 `cpu: "1"` 할당이라도 실제 배치 위치에 따라 성능 차이가 발생한다.

| 배치 상황 | 특징 |
|-----------|------|
| 독립 Physical Core 할당 | 자원 경쟁 없음, 안정적 성능 |
| SMT Thread 공유 상태 | 공유 자원 경쟁 가능, 성능 가변 |

저지연이 요구되는 워크로드는 Kubernetes CPU Manager Policy(`static`) + Guaranteed QoS를 통해 전용 Physical Core 할당을 고려할 수 있다.

---

## 11. JVM 및 고성능 서버 설계 관점

### Thread Pool 크기

Logical CPU 수만 기준으로 Thread Pool을 설정하면 SMT 자원 경쟁이 심화될 수 있다. CPU 집약적(CPU-bound) 작업에서는 Physical Core 수를 Thread Pool 상한의 기준으로 고려하는 것이 유리한 경우가 있다.

### Netty / WebFlux Event Loop

Event Loop 수는 기본적으로 Logical CPU 수 기준으로 생성된다. 저지연 요구사항이 있는 경우 SMT 구조를 고려하여 조정할 수 있다.

### 금융 시스템 / DPDK 환경

극단적인 저지연이 요구되는 환경에서는 다음 전략을 사용한다.

| 전략 | 설명 |
|------|------|
| SMT 비활성화 | BIOS 수준에서 Hyper-Threading 끄기 |
| Core Pinning | 특정 Thread를 특정 Physical Core에 고정 |
| Isolated CPU | OS 스케줄링에서 해당 Core를 제외 (`isolcpus`) |

---

## 12. CPU Affinity 설정

특정 Thread를 특정 Logical CPU(Hardware Thread)에 고정하여 성능을 안정화한다.

| 목적 | 설명 |
|------|------|
| Cache Locality 유지 | 동일 Core 반복 실행으로 Cache Hit 증가 |
| SMT 간섭 감소 | 다른 Thread와 Core 공유 방지 |
| Context Switch 감소 | CPU 이동 최소화 |
| Latency 안정화 | 실행 위치 고정으로 지연 분산 감소 |

```bash
# 프로세스를 cpu0, cpu2에 고정 (서로 다른 Physical Core)
taskset -c 0,2 java -jar app.jar

# 현재 프로세스의 Affinity 확인
taskset -p <pid>
```

---

## 13. 전체 계층 구조

```
Application Thread
  ↓
Kernel Scheduler
  ↓
Logical CPU             ← OS 스케줄링 단위
  ↓
Hardware Thread         ← 독립 실행 상태 유지
  ↓
Physical Core           ← 실제 하드웨어 자원
  ↓
ALU / FPU / LSU / Cache / Pipeline
```

---

## 14. 구성 요소 요약

| 구성 요소 | 계층 | 역할 |
|-----------|------|------|
| Physical Core | Hardware Resource | 실제 연산 자원 집합 |
| Hardware Thread | Hardware Execution Context | 독립 실행 상태 유지 단위 |
| Logical CPU | OS Logical Layer | 커널 스케줄링 단위 |
| SMT | CPU Architecture | Physical Core 내 다중 실행 흐름 지원 |
| Scheduler | Kernel Layer | Logical CPU 기준 실행 배치 관리 |

---

## 15. 성능 최적화 핵심 요소

```
Physical Core 자원 효율적 사용
+ SMT 구조 인식 (자원 공유 고려)
+ Hardware Thread 간 Resource Contention 최소화
+ Cache Locality 유지
+ Scheduler Core-Aware 배치 활용
= High Throughput + Stable Latency
```

이 원칙은 다음 환경의 설계와 직접 연결된다.

- Kubernetes CPU Resource 및 QoS 설계
- JVM Thread Pool 크기 결정 (CPU-bound 기준)
- Netty / WebFlux Event Loop 튜닝
- 금융 시스템 저지연 처리 (SMT 비활성화, Core Pinning)
- 고성능 네트워크 처리 (DPDK, Kernel Bypass)

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*네 다음도 토스페이먼츠 실무급으로 바로 진행해주세요.