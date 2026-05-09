# Hyper-Threading(HT)과 Throughput 최적화

## 1. Hyper-Threading(HT) 개요

Hyper-Threading(HT)은 하나의 **Physical Core**에서 여러 실행 흐름(Hardware Thread)을 동시에 처리할 수 있도록 하는 CPU 아키텍처 기술입니다.

- **Intel**: Hyper-Threading(HT)
- **AMD**: SMT(Simultaneous Multithreading)

### 핵심 목적

| 목적 | 설명 |
|------|------|
| CPU 자원 활용률 증가 | 유휴 실행 자원 감소 |
| Pipeline 활용 극대화 | Stall 시간 활용 |
| Throughput 향상 | 단위 시간당 처리량 증가 |
| Memory Wait 숨김 | 메모리 대기 시간 활용 |

---

## 2. HT의 핵심 개념

HT는 Physical Core 수를 늘리는 기술이 **아닙니다**.

```
1 Physical Core  ≠  2 Independent Physical Cores
```

HT는 하나의 Physical Core 내부에서 여러 실행 흐름을 유지하여, **사용되지 않는 실행 자원을 효율적으로 활용**하는 기술입니다.

### Physical Core와 Hardware Thread 관계

```
1 Physical Core
├── Hardware Thread 0  →  Logical CPU 0  (OS 인식)
└── Hardware Thread 1  →  Logical CPU 1  (OS 인식)
```

운영체제는 Hardware Thread를 독립적인 Logical CPU로 인식합니다.

---

## 3. 하드웨어 자원 공유/독립 구조

### 공유되는 자원

동일 Physical Core의 Hardware Thread들은 다음 자원을 **공유**합니다.

| 공유 자원 | 설명 |
|-----------|------|
| ALU / FPU | 연산 유닛 |
| Pipeline | 명령 실행 구조 |
| Cache 일부 | 메모리 계층 |
| Memory Bandwidth | 메모리 접근 대역폭 |

### 독립적으로 유지되는 상태

각 Hardware Thread는 다음 상태를 **독립적으로** 유지합니다.

| 독립 상태 | 설명 |
|-----------|------|
| Register Set | 실행 상태 |
| Program Counter | 명령 실행 위치 |
| Thread Context | 실행 문맥 |

---

## 4. Throughput과 Latency

### Throughput

단위 시간당 처리되는 작업량을 의미합니다.

| 영역 | Throughput 의미 |
|------|-----------------|
| CPU | 초당 처리 명령 수 |
| 서버 | 초당 요청 처리 수 |
| 네트워크 | 초당 데이터 처리량 |

### Latency

개별 작업 하나가 완료되기까지 걸리는 시간을 의미합니다.

> HT는 **Throughput 향상** 기술이며, 개별 작업의 Latency를 직접 절반으로 줄이는 기술이 **아닙니다**.

---

## 5. HT의 동작 원리

### CPU Stall 문제

CPU는 다음 상황에서 유휴 상태(Stall)가 됩니다.

| 원인 | 설명 |
|------|------|
| Cache Miss | 메모리 대기 |
| Branch Misprediction | Pipeline Flush |
| I/O Wait | 외부 장치 대기 |
| Dependency Stall | 데이터 의존성 |

### HT의 Stall 해소 방식

한 Hardware Thread가 Stall 상태일 때, 다른 Hardware Thread가 실행됩니다.

```
Thread A: Memory Access → Memory Wait (Stall)
                                  ↓
Thread B:                   Execute (유휴 Pipeline 활용)
```

이를 통해 Pipeline과 실행 유닛의 활용률을 높여 Throughput을 향상시킵니다.

### 실제 효과

| 요소 | 효과 |
|------|------|
| Pipeline Utilization | 증가 |
| CPU Idle Time | 감소 |
| Throughput | 증가 |
| Resource Utilization | 향상 |

---

## 6. HT 성능 향상 폭

일반적으로 **10% ~ 30%** 수준의 성능 향상이 관찰됩니다.  
단, 워크로드 특성에 따라 효과가 크게 달라집니다.

---

## 7. 워크로드별 HT 효과

### HT에 유리한 워크로드

#### Memory Bound Workload
메모리 접근 대기 시간이 많은 작업. Stall 시간을 다른 Thread가 활용할 수 있습니다.

| 예시 | 특징 |
|------|------|
| 데이터베이스 조회 | Cache Miss 많음 |
| 네트워크 서버 | I/O Wait 존재 |
| 대규모 데이터 처리 | Memory Access 빈번 |

#### Mixed Execution Workload
서로 다른 실행 유닛을 사용하는 작업이 혼재할 때, 자원을 병렬로 활용할 수 있습니다.

| 작업 유형 | 사용 유닛 |
|-----------|-----------|
| Integer 연산 | ALU |
| Floating Point 연산 | FPU |

---

### HT에 불리한 워크로드

#### Compute Bound Workload
CPU 연산 유닛을 지속적으로 최대로 사용하는 작업.

| 예시 | 특징 |
|------|------|
| 수치 계산 | ALU/FPU 지속 사용 |
| 암호화 연산 | 연산 집중 |
| Scientific Computing | SIMD 집중 사용 |

#### Resource Contention 발생

Compute Bound 환경에서는 동일 Core의 두 Hardware Thread가 동일한 자원을 두고 경쟁합니다.

```
Thread A  +  Thread B
        ↓
  Shared ALU/FPU Contention
```

이로 인해 다음 문제가 발생할 수 있습니다.

| 문제 | 설명 |
|------|------|
| IPC 감소 | Pipeline 경합 |
| Cache Miss 증가 | Cache 충돌 |
| Throughput 증가 제한 | 자원 포화 |
| Latency 증가 | 실행 대기 증가 |

---

## 8. HT 성능이 저하될 수 있는 경우

특정 환경에서는 HT가 오히려 성능 저하를 유발할 수 있습니다.

| 원인 | 설명 |
|------|------|
| Resource Saturation | 실행 유닛 포화 |
| Cache Contention | Cache 경쟁 |
| Context Switching 증가 | 스케줄링 비용 |
| Memory Bandwidth 경쟁 | 메모리 병목 |

---

## 9. 실제 시스템에서의 HT 고려사항

### JVM 및 서버 애플리케이션

| 시스템 | HT 관련 특성 |
|--------|--------------|
| Netty / WebFlux | I/O Wait 존재 + 비동기 처리 → HT 이점 활용 가능 |
| Kafka / Redis | 메모리 접근 패턴, Polling 구조에 따라 영향 상이 |
| 금융 시스템 (저지연) | SMT 비활성화를 선택하는 경우 있음 |

금융 시스템에서 SMT를 비활성화하는 이유:

| 목적 | 설명 |
|------|------|
| Resource Isolation | 자원 독점 |
| Latency Stability | 응답 시간 안정화 |
| Cache Predictability | Cache 충돌 감소 |

---

### Kubernetes CPU 설계

Kubernetes는 일반적으로 **Logical CPU** 기준으로 스케줄링합니다.

```yaml
resources:
  limits:
    cpu: "2"   # Logical CPU 2개 기준
```

동일한 Logical CPU 수라도 실제 성능은 구조에 따라 다릅니다.

| 구조 | 특성 |
|------|------|
| 2 Independent Physical Cores | 높은 성능 안정성 |
| 1 Physical Core + 2 SMT Threads | 자원 경쟁 가능성 존재 |

---

### SMT 제어 방법

| 방법 | 명령 / 설정 |
|------|-------------|
| SMT 비활성화 | BIOS/UEFI SMT Disable |
| CPU Pinning | `taskset`, `cset`, `numactl` |

#### Linux에서 SMT 확인

```bash
lscpu
# Thread(s) per core: 2  →  SMT 활성화 상태
```

---

## 10. 전체 실행 흐름

```
Application Thread
        ↓
  Kernel Scheduler
        ↓
    Logical CPU
        ↓
  Hardware Thread
        ↓
  Shared Physical Core
        ↓
  ALU / Pipeline / Cache
```

---

## 11. 핵심 정리

| 구성 요소 | 역할 |
|-----------|------|
| Physical Core | 실제 연산 자원 |
| Hardware Thread | 실행 컨텍스트 |
| Logical CPU | OS 실행 단위 |
| SMT / HT | 다중 실행 흐름 지원 |
| Throughput | 단위 시간당 처리량 |
| Latency | 개별 작업 응답 시간 |
| Resource Contention | 공유 자원 경쟁 |

### 결론

Hyper-Threading은 Physical Core 수를 증가시키는 기술이 아니라,  
**유휴 실행 자원을 활용하여 CPU 자원 효율과 Throughput을 향상시키는 기술**입니다.

```
HT/SMT  =  Resource Utilization Optimization
```

성능 효과는 워크로드 특성에 따라 결정됩니다.

```
Memory Bound / I/O Bound  →  HT 효과 높음
Compute Bound             →  HT 효과 제한적, 경우에 따라 역효과
```

따라서 **Kubernetes CPU 설계**, **JVM Thread Pool 구성**, **Netty Event Loop 최적화**,  
**금융 시스템 저지연 처리**, **고성능 서버 튜닝** 시에는  
SMT 구조와 워크로드 특성을 함께 고려해야 합니다.

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*