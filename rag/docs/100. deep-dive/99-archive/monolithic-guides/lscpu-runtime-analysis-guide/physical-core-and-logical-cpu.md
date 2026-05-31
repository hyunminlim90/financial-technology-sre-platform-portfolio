# Physical Core와 Logical CPU의 계층 구조

## 1. 개요

현대 CPU 아키텍처에서 Physical Core와 Logical CPU는 단순히 개수 차이가 아니라, 서로 다른 계층의 개념이다.

| 계층 | 관점 |
|------|------|
| Physical Layer | 실제 하드웨어 자원 |
| Logical Layer | 운영체제가 관리하는 실행 단위 |

이 구분은 CPU 구조 이해, 운영체제 스케줄링, Kubernetes CPU 자원 관리, JVM Thread Pool 설계, 고성능 서버 최적화의 기반이 되는 개념이다.

---

## 2. Physical Core

Physical Core는 CPU 내부에 실제로 존재하는 물리적 연산 실행 유닛이다.

### 내부 구성 요소

| 구성 요소 | 역할 |
|-----------|------|
| ALU | 정수 연산 |
| FPU | 부동소수점 연산 |
| LSU | 메모리 접근 |
| Pipeline | 명령어 실행 |
| Branch Predictor | 분기 예측 |
| L1/L2 Cache | 고속 캐시 |

Physical Core는 이러한 하드웨어 자원의 집합으로, 실제 연산을 수행한다.

---

## 3. Logical CPU

Logical CPU는 운영체제 커널이 스케줄링 단위로 관리하는 논리적 실행 단위다.

Linux에서는 다음과 같이 표현된다.

```
cpu0, cpu1, cpu2, cpu3, ...
```

운영체제는 Logical CPU를 기준으로 다음 작업을 수행한다.

- 스레드 스케줄링
- 프로세스 실행 배치
- CPU Affinity 적용
- Load Balancing

---

## 4. Hardware Thread

Hardware Thread는 Physical Core 내부에서 독립적인 실행 상태(Context)를 유지하는 실행 흐름 단위다.

### 독립 유지 요소

| 상태 | 설명 |
|------|------|
| Register Set | 레지스터 상태 |
| Program Counter | 실행 위치 |
| Thread Context | 실행 문맥 |
| Pipeline State 일부 | 실행 상태 일부 |

---

## 5. Hyper-Threading / SMT

현대 CPU는 하나의 Physical Core에서 여러 Hardware Thread를 동시에 실행할 수 있다.

| 제조사 | 기술 이름 |
|--------|-----------|
| Intel | Hyper-Threading |
| AMD | SMT (Simultaneous Multithreading) |

```
1 Physical Core
  ├── Hardware Thread 0 → Logical CPU 0
  └── Hardware Thread 1 → Logical CPU 1
```

운영체제는 각 Hardware Thread를 독립적인 Logical CPU로 인식한다.

---

## 6. 세 계층의 관계 정리

Physical Core, Hardware Thread, Logical CPU는 각각 다른 계층에 위치한다.

```
Physical Core        ← 하드웨어 자원 (실제 연산 유닛)
  ├── Hardware Thread 0  ← 실행 상태 유지 단위
  └── Hardware Thread 1

Logical CPU 0        ← OS가 인식하는 스케줄링 단위
Logical CPU 1
```

| 구성 요소 | 계층 | 성격 |
|-----------|------|------|
| Physical Core | Hardware | 연산 자원 |
| Hardware Thread | Hardware Execution Context | 실행 상태 |
| Logical CPU | OS Logical Layer | 스케줄링 단위 |

### Physical Core ≠ Hardware Thread인 이유

SMT를 지원하는 CPU에서는 1개의 Physical Core가 2개 이상의 Hardware Thread를 가질 수 있다. 따라서 두 개념은 1:1로 대응되지 않는다.

---

## 7. Physical Core 내부의 자원 공유 구조

동일 Physical Core의 Hardware Thread들은 일부 자원을 공유하고, 일부 상태는 독립 유지한다.

### 공유 자원

| 자원 | 설명 |
|------|------|
| ALU / FPU | 실행 유닛 |
| Pipeline | 명령 실행 구조 |
| Cache 일부 | L1/L2 캐시 일부 |
| Memory Bandwidth | 메모리 접근 대역폭 |

### 독립 유지 상태

| 요소 | 설명 |
|------|------|
| Register Set | 스레드별 레지스터 값 |
| Program Counter | 각 스레드의 실행 위치 |
| Thread Context | 실행 문맥 전체 |

---

## 8. SMT의 장점과 한계

### 장점

SMT는 Pipeline의 유휴 슬롯을 다른 Hardware Thread가 활용하여 CPU 자원 사용률을 높인다.

| 효과 | 설명 |
|------|------|
| Pipeline Utilization 증가 | 빈 실행 슬롯 활용 |
| Throughput 증가 | 동시 작업 처리량 향상 |
| Memory Wait 숨김 | 한 Thread의 메모리 대기 중 다른 Thread 실행 |

### 한계

SMT는 Physical Core의 성능을 2배로 만들지 않는다.

```
1 Physical Core + SMT ≠ 2 Independent Physical Cores
```

공유 자원(ALU, Pipeline, Memory Bandwidth) 경쟁이 존재하기 때문이다.

---

## 9. 성능 간섭 (Resource Contention)

동일 Physical Core의 SMT Thread들은 공유 자원 경쟁으로 서로 성능에 영향을 줄 수 있다.

| 영향 | 설명 |
|------|------|
| ALU 경쟁 | 연산 처리 지연 |
| Cache 충돌 | Cache Miss 증가 |
| Pipeline 경합 | IPC 감소 |
| Memory Bandwidth 경쟁 | Throughput 감소 |

이를 **Noisy Neighbor** 문제라고 부른다.

---

## 10. Linux Scheduler의 Core-Aware 배치 전략

Linux Scheduler는 가능한 경우 서로 **다른 Physical Core**에 스레드를 우선 배치한다. 동일 Physical Core의 SMT Thread 간 자원 경쟁을 피하기 위함이다.

### 배치 예시 (4 Core / 8 Thread 시스템)

```
Physical Core 0 → cpu0, cpu1
Physical Core 1 → cpu2, cpu3
Physical Core 2 → cpu4, cpu5
Physical Core 3 → cpu6, cpu7
```

Thread 2개를 실행할 경우:

```
권장:  cpu0 + cpu2  (서로 다른 Physical Core)
지양:  cpu0 + cpu1  (동일 Physical Core의 SMT Thread)
```

---

## 11. Kubernetes에서의 CPU 계층 이해

Kubernetes의 CPU Resource는 일반적으로 Logical CPU 기준으로 동작한다.

```yaml
resources:
  requests:
    cpu: "1"
  limits:
    cpu: "2"
```

- `cpu: "1"` → Logical CPU 1개에 해당하는 CPU 시간 보장

### 동일 CPU 할당이라도 성능이 다를 수 있는 경우

| 경우 | 성능 |
|------|------|
| 독립 Physical Core 할당 | 자원 경쟁 없음, 높은 처리량 |
| SMT Thread 공유 상태 | 공유 자원 경쟁, 처리량 가변 |

Kubernetes는 기본적으로 Physical Core 단위 배치를 보장하지 않는다. 저지연이 요구되는 환경에서는 CPU Manager Policy(`static`)와 함께 Guaranteed QoS 설정을 통해 전용 Core 할당이 가능하다.

---

## 12. JVM 및 서버 애플리케이션 설계 영향

### Thread Pool 크기 설정

Logical CPU 수만 기준으로 Thread Pool을 과도하게 늘리면 다음 문제가 발생한다.

```
Thread Pool 과대 설정
  → Context Switching 증가
  → SMT 자원 경쟁 증가
  → Throughput 감소
```

SMT 환경에서는 Physical Core 수를 Thread Pool 상한의 기준으로 고려하는 것이 유리한 경우가 있다.

### Netty / WebFlux Event Loop

Event Loop 수는 일반적으로 Logical CPU 수를 기준으로 설정된다. 단, 저지연이 요구되는 환경에서는 SMT 여부 및 Physical Core 수까지 고려하여 조정하기도 한다.

---

## 13. CPU Affinity와 Pinning

고성능 또는 저지연 환경에서는 특정 Thread를 특정 Logical CPU 또는 Physical Core에 고정하는 방식을 사용한다.

| 목적 | 설명 |
|------|------|
| Cache Locality 유지 | 동일 Core에서 반복 실행 시 Cache Hit 증가 |
| SMT 자원 경쟁 감소 | 다른 Thread와 Core 공유 방지 |
| Context Switch 감소 | CPU 이동 최소화 |
| Latency 안정화 | 실행 위치 고정으로 지연 분산 감소 |

```bash
# 특정 프로세스를 cpu0, cpu2에 고정
taskset -c 0,2 java -jar app.jar
```

---

## 14. Linux CPU Topology 확인

### CPU 구조 전체 확인

```bash
lscpu
```

주요 출력 항목:

| 항목 | 의미 |
|------|------|
| `Socket(s)` | 물리 CPU 패키지 수 |
| `Core(s) per socket` | 소켓당 Physical Core 수 |
| `Thread(s) per core` | Core당 Hardware Thread 수 (2이면 SMT 활성) |
| `CPU(s)` | 총 Logical CPU 수 |

### 상세 구조 확인

```bash
cat /proc/cpuinfo
# physical id: 소켓 번호
# core id: Physical Core 번호
# processor: Logical CPU 번호
```

동일한 `physical id` + `core id`를 가진 `processor` 항목이 2개이면 해당 Physical Core에 SMT가 적용된 것이다.

---

## 15. 전체 계층 구조

```
Application Thread
  ↓
Kernel Scheduler
  ↓
Logical CPU (OS 스케줄링 단위)
  ↓
Hardware Thread (실행 상태 유지)
  ↓
Physical Core (하드웨어 자원)
  ↓
ALU / FPU / Pipeline / LSU / Cache
```

---

## 16. 구성 요소 요약

| 구성 요소 | 계층 | 역할 |
|-----------|------|------|
| Physical Core | Hardware | 실제 연산 자원 집합 |
| Hardware Thread | Hardware Execution Context | 독립 실행 상태 유지 단위 |
| Logical CPU | OS Logical Layer | 커널 스케줄링 단위 |
| Scheduler | Kernel | 실행 배치 및 부하 분산 |
| SMT | CPU Architecture | Physical Core 내 다중 실행 흐름 지원 |

---

## 17. 성능 최적화 핵심 요소

```
Physical Core 수 기반 설계
+ SMT 구조 인식 (자원 공유 고려)
+ Kernel Scheduling 이해
+ Cache Locality 유지
+ Resource Contention 최소화
= High Throughput + Stable Latency
```

이 원칙은 다음 환경의 설계와 직접 연결된다.

- Kubernetes CPU Resource 및 QoS 설계
- JVM Thread Pool 크기 결정
- Netty / WebFlux Event Loop 설정
- 금융 시스템 저지연 처리
- 대규모 트래픽 처리 아키텍처 설계

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*