# Linux CFS Scheduler, Hardware Context 구조와 결제 시스템 병목 분석

## 1. CFS와 Hardware Context 개요

### CFS(Completely Fair Scheduler)

CFS는 Linux Kernel에서 사용하는 기본 CPU 스케줄링 알고리즘입니다. 실행 가능한 Thread들에게 CPU 실행 시간을 공정하게 분배합니다.

CFS가 실제로 스케줄링하는 단위는 **Logical CPU**입니다. Logical CPU는 Kernel에 노출된 독립 실행 컨텍스트이며, 그 실체는 Hardware Context입니다.

```
CFS = Linux Kernel Scheduler
Logical CPU = Kernel에 노출된 Hardware Context = 독립 실행 컨텍스트
```

### Hardware Context

Hardware Context는 CPU가 독립적으로 실행 흐름을 유지할 수 있도록 제공하는 하드웨어 상태 집합입니다.

| 구성 요소 | 설명 |
|-----------|------|
| Register Set | 연산 상태 저장 |
| Program Counter (PC/IP) | 다음 실행 명령어 주소 |
| Stack Pointer (SP) | Stack 위치 |
| CPU Flags | 상태 플래그 |
| APIC ID | CPU 식별 번호 |
| Interrupt State | 인터럽트 처리 상태 |

Kernel Scheduler는 ALU 개수가 아닌 **Hardware Context 개수**를 기준으로 실행 슬롯을 관리합니다. Hardware Context가 존재하면 Kernel은 이를 별도의 CPU 실행 단위로 다룹니다.

---

## 2. Hyper-Threading과 Hardware Context

### Hyper-Threading의 구조

Hyper-Threading(SMT)은 하나의 Physical Core 내부에 여러 Hardware Context를 제공하는 기술입니다.

```
1 Physical Core
├── Hardware Context 0  →  Logical CPU 0 (cpu0)
└── Hardware Context 1  →  Logical CPU 1 (cpu1)
```

Kernel은 이를 두 개의 독립된 Logical CPU로 인식합니다.

### 독립 요소와 공유 요소

| 구분 | 요소 | 설명 |
|------|------|------|
| **독립** | Register Set | Thread별 독립 상태 유지 |
| **독립** | Program Counter | 실행 위치 유지 |
| **독립** | APIC ID | CPU 식별 |
| **독립** | Interrupt Context | 인터럽트 상태 |
| **공유** | ALU / FPU | 정수 / 부동소수점 연산 유닛 |
| **공유** | Pipeline | 실행 파이프라인 |
| **공유** | L1 / L2 Cache | 코어 내부 캐시 |
| **공유** | Branch Predictor | 분기 예측기 |
| **공유** | Load/Store Unit | 메모리 접근 유닛 |

Logical CPU는 독립 실행 상태는 가지지만, **실제 연산 자원은 공유**합니다.

### HT ON / OFF 구조 비교

| 구분 | HT ON | HT OFF |
|------|-------|--------|
| Physical Core | 1개 | 1개 |
| Hardware Context | 2개 이상 | 1개 |
| Logical CPU | 2개 이상 | 1개 |
| 실행 자원 | 여러 Logical CPU가 공유 | 단일 Logical CPU가 독점 |
| 성능 특성 | 처리량 중심 | 예측 가능성 중심 |

```
HT OFF = Physical Core : Hardware Context : Logical CPU = 1 : 1 : 1
```

예시:

```
8 Physical Core / HT ON  → 16 Logical CPU
8 Physical Core / HT OFF → 8 Logical CPU
```

---

## 3. CFS 스케줄링 동작

### vruntime과 Thread 선택

CFS는 각 Thread의 CPU 사용량을 `vruntime`(가상 실행 시간)으로 관리합니다. CPU를 적게 사용한 Thread가 우선 실행됩니다.

```
Runnable Thread → Runqueue 등록 → vruntime 비교 → 가장 적게 실행된 Thread 선택
→ Logical CPU 배치 → Hardware Context 로드 → 실행
```

### Runqueue

각 Logical CPU는 Runqueue를 가집니다.

```
cpu0 runqueue
├── Thread A
├── Thread B
└── Thread C
```

Load Average 증가는 Runqueue 대기 증가와 직결됩니다. Hardware Context 수보다 Runnable Thread가 많아지면 CPU 대기가 발생합니다.

### SMT Sibling

Hyper-Threading으로 생성된 Logical CPU 쌍을 SMT Sibling이라고 합니다.

```
cpu0 ↔ cpu1   (동일 Physical Core)
cpu2 ↔ cpu3   (동일 Physical Core)
```

CFS는 Logical CPU를 독립 실행 단위처럼 다루지만, 실제로는 두 Logical CPU가 동일 Physical Core의 실행 자원을 공유합니다. 높은 부하 상황에서는 내부 자원 경쟁이 발생할 수 있습니다.

---

## 4. Context Switching

Context Switch는 현재 실행 중인 Thread의 Hardware Context를 저장하고, 다음 Thread의 Context를 복원하는 과정입니다.

```
Thread A 실행 → Register 저장 → Thread B Register 복원 → Thread B 실행
```

### 저장 항목

| 저장 항목 | 설명 |
|-----------|------|
| General Registers | 연산 데이터 |
| Program Counter | 실행 위치 |
| Stack Pointer | Stack 상태 |
| Flags Register | 상태 플래그 |
| SIMD / FPU 상태 | 벡터 연산 상태 |
| Thread Metadata | Scheduler 정보 |

### Context Switch 비용 요인

| 비용 요소 | 설명 |
|-----------|------|
| Register Save/Restore | CPU 상태 저장/복원 |
| Cache Pollution | 이전 Thread Cache 무효화 |
| Pipeline Flush | 명령 파이프라인 재구성 |
| TLB 영향 | 주소 변환 캐시 영향 |
| Scheduler 비용 | 다음 Thread 선택 |

---

## 5. HT ON / OFF 특성 비교

### HT OFF의 장점

| 장점 | 설명 |
|------|------|
| 성능 예측 가능성 증가 | SMT Sibling과의 자원 공유 경쟁 제거, Latency 변동성 감소 |
| Cache 경합 감소 | L1/L2 Cache를 동시에 사용하는 Logical CPU 제거 |
| 장애 분석 단순화 | Logical CPU 100% ≈ Physical Core 포화로 해석 단순화 |

### HT OFF의 단점

| 단점 | 설명 |
|------|------|
| 전체 처리량 감소 | Memory Stall / I/O Wait 중 유휴 실행 슬롯 활용 불가 |
| Logical CPU 수 감소 | Node Allocatable CPU 감소 |
| Context Switching 증가 가능 | Thread 수 동일 + Logical CPU 감소 → Runqueue 증가 |
| Lock Contention 악화 가능 | Lock 보유 Thread가 CPU 시간을 받지 못하면 해제 지연 |

### HT ON / OFF 병목 비교

| 항목 | HT ON | HT OFF |
|------|-------|--------|
| 주요 장점 | Throughput 향상 가능 | Latency 예측 가능성 증가 |
| 주요 병목 | SMT Sibling 자원 경쟁 | 전체 실행 슬롯 감소 |
| Cache 특성 | 공유 경합 가능 | 경합 감소 가능 |
| CPU 분석 | Logical CPU와 Core 성능 차이 고려 필요 | Logical CPU와 Core가 거의 일치 |
| 적합한 환경 | I/O Bound, 일반 웹 API | 저지연, 고정 성능 요구 |

### 환경별 HT 설정 기준

| 환경 | 권장 방향 |
|------|-----------|
| 일반 웹 API 서버 | HT ON 우선 |
| I/O Bound 서비스 | HT ON 우선 |
| Kafka Consumer | HT ON 우선, Lag/CPU 경합 관찰 |
| 저지연 금융 시스템 | HT OFF 또는 CPU Pinning 검토 |
| FDS Engine | HT OFF 또는 CPU Pinning 검토 |
| 고성능 연산 / Cache 민감 워크로드 | HT OFF 검토 |
| Settlement Batch | 워크로드별 벤치마크 필요 |

---

## 6. 결제 시스템 워크로드 특성

결제 시스템은 **I/O Bound 중심이지만, 일부 구간은 CPU Bound** 특성을 가집니다.

### 일반 요청 흐름

```
Client Request → API Gateway → Authentication / Authorization
→ Payment Core → External Payment Provider API
→ Database Transaction → Kafka Event Publish → Response
```

이 흐름의 상당 부분은 Network I/O와 DB I/O로 구성됩니다.

### I/O Bound 구간

| I/O 대상 | 예시 |
|----------|------|
| 외부 결제망 | 카드사, 은행, 간편결제사 |
| 내부 서비스 | 인증, 한도, 정산, 위험 평가 |
| Database | 결제 상태 저장, 이력 저장 |
| Message Broker | Kafka 이벤트 발행 |
| Cache | Redis 조회 및 갱신 |

### CPU Bound 구간

| 구간 | CPU 사용 원인 |
|------|--------------|
| 암호화 / 복호화 | AES, RSA, 서명 검증 |
| JSON / XML 파싱 | 직렬화 / 역직렬화 |
| FDS Rule Engine | 조건 평가, 패턴 매칭 |
| Risk Scoring | 점수 계산, 모델 추론 |
| 정산 Batch | 대량 집계, 계산 |
| 압축 / 해시 | 메시지 처리, 무결성 검증 |

### 계층별 워크로드 특성

| 계층 | 주요 작업 | 특성 |
|------|-----------|------|
| API Gateway | 인증, 라우팅, TLS, Rate Limit | Mixed |
| Payment Core | 상태 변경, 트랜잭션, 외부 연동 | I/O Bound |
| Fraud Detection | Rule 평가, 위험 점수 계산 | CPU Bound 가능 |
| Settlement | 대량 집계, 파일 생성, 검증 | CPU Bound / I/O Bound 혼합 |
| Event Pipeline | Kafka 발행/소비 | I/O Bound + Deserialize CPU |
| Database Layer | Transaction, Lock, Commit | I/O Bound |

---

## 7. 결제 시스템과 HT 설정

### 서비스별 HT 판단 기준

| 서비스 | 권장 방향 | 이유 |
|--------|-----------|------|
| Payment API | HT ON 우선 | 외부 API/DB 대기 시간이 많음 |
| Netty / WebFlux Gateway | HT ON 우선 | Network I/O 대기 활용 가능 |
| Kafka Consumer | HT ON 우선, 관찰 필요 | Poll/Deserialize 혼합 처리 |
| FDS Engine | HT OFF 또는 CPU Pinning | Core 자원 독점 필요 |
| Settlement Batch | 벤치마크 필요 | CPU/I/O Bound 혼합 |
| Low-latency Critical Path | HT OFF 검토 | P99/P999 안정성 우선 |

### HT OFF가 결제 시스템에 미치는 영향

**Kafka Consumer:**

```
Logical CPU 감소 → Poll 지연 → Deserialize 지연 → Commit 지연 → Consumer Lag 증가 → Rebalance 가능성
```

**GC:**

```
Logical CPU 감소 → GC Thread 실행 기회 감소 → GC Pause 또는 Heap 압박 증가 가능
```

단, Cache 경합 감소로 일부 GC 단계의 예측 가능성이 좋아질 수 있습니다.

**Netty EventLoop:**

HT OFF 후 Logical CPU 수가 줄어들면 EventLoop 수도 감소할 수 있으므로 처리량과 Latency를 함께 측정해야 합니다.

---

## 8. 운영 및 인프라 고려사항

### CPU Affinity

특정 Thread를 특정 Logical CPU에 고정합니다.

```bash
taskset -c 0,1 java -jar app.jar
```

| 효과 | 설명 |
|------|------|
| Cache Locality 향상 | 동일 Core Cache 재사용 |
| NUMA 최적화 | 메모리 접근 최적화 |
| Context Switch 감소 | CPU 이동 최소화 |

### NUMA 환경

멀티 소켓 환경에서는 Hardware Context가 특정 NUMA Node와 연결됩니다. 다른 NUMA Node 메모리에 접근하면 메모리 지연이 증가할 수 있습니다.

```
Socket 0: cpu0, cpu1 + local memory
Socket 1: cpu8, cpu9 + local memory
```

### Kubernetes CPU Limit

```yaml
resources:
  limits:
    cpu: "500m"   # 1 CPU의 50% 시간 사용 가능
```

```
Pod CPU Limit → cgroup quota → CFS Bandwidth Control → Logical CPU 실행 제한
```

HT OFF로 Logical CPU 수가 줄어들면 Node Allocatable CPU도 감소하므로 **CPU Request/Limit 재산정**이 필요합니다.

```
HT ON  : 16 Logical CPU Node → HT OFF : 8 Logical CPU Node
→ 동일 Pod 배치 전략 유지 시 CPU 자원 압박 증가 가능
```

### Thread Pool 조정

HT OFF 환경에서는 Logical CPU 수가 줄어드므로 Thread Pool 크기 재산정이 필요합니다.

- CPU Bound Worker Pool: Core 수 기준으로 축소 검토
- I/O Bound Worker Pool: 대기 시간이 많으므로 별도 측정 필요

### 운영 전략

| 전략 | 설명 |
|------|------|
| Critical Path 전용 Node | 결제 승인 핵심 경로를 별도 Node에 배치 |
| HT OFF Node Pool | 저지연 서비스 전용 Node Pool |
| HT ON Node Pool | 일반 API / I/O Bound 서비스 전용 Node Pool |
| CPU Pinning | 핵심 Thread/Core 고정 |
| Guaranteed QoS | Kubernetes 전용 CPU 할당 |
| CPU Limit 재조정 | Throttling 방지 |

---

## 9. Linux에서 CPU 구조 확인

```bash
lscpu
```

| 항목 | 의미 |
|------|------|
| CPU(s) | Logical CPU 개수 |
| Core(s) per socket | Socket당 Physical Core 수 |
| Thread(s) per core | Core당 Hardware Context 수 |
| Socket(s) | CPU Package 수 |

```bash
cat /proc/cpuinfo
```

| 필드 | 의미 |
|------|------|
| processor | Logical CPU 번호 |
| core id | 동일 Physical Core 식별 |
| physical id | Socket 식별 |
| apicid | APIC ID |

```bash
# SMT Sibling 확인
cat /sys/devices/system/cpu/cpu*/topology/thread_siblings_list
```

---

## 10. HT ON/OFF 결정 절차

### 결정 흐름

```
1. 워크로드 분류: CPU Bound / I/O Bound / Mixed
2. 핵심 SLO 확인: Throughput 중심 vs P99/P999 Latency 중심
3. 동일 조건 벤치마크: 동일 트래픽 / Pod 배치 / CPU Limit 조건에서 HT ON vs OFF 비교
4. 지표 비교: 아래 표 참조
5. 설정 결정: Node 단위 정책 + 워크로드 격리 전략 함께 고려
```

### SRE 판단 지표

| 지표 | 의미 |
|------|------|
| P99 / P999 Latency | 응답 지연 안정성 |
| Throughput (TPS) | 초당 처리량 |
| CPU Utilization | CPU 사용률 |
| Context Switch Rate | Thread 전환 비용 |
| Cache Miss | Cache 경합 여부 |
| Consumer Lag | Kafka 처리 지연 |
| CPU Throttling | cgroup Quota 제한 |
| Steal Time | VM 환경 CPU 대기 |
| GC Pause | JVM 정지 시간 |

---

## 11. 전체 실행 계층

```
Software Thread
      ↓
Kernel Thread
      ↓
CFS Scheduler
      ↓
Logical CPU
      ↓
Hardware Context
      ↓
Physical Core
      ↓
ALU / LSU / Cache / Pipeline
```

### Hyper-Threading 환경 전체 구조

```
Physical Core
├── Hardware Context 0  →  Logical CPU 0
└── Hardware Context 1  →  Logical CPU 1
```

Kernel은 Logical CPU를 독립 실행 단위처럼 다루지만, 실제 연산 자원(ALU, Pipeline, Cache 등)은 공유됩니다.

---

## 12. 핵심 정리

| 구성 요소 | 역할 |
|-----------|------|
| CFS | Linux CPU Scheduler |
| Logical CPU | Kernel이 인식하는 CPU 실행 슬롯 |
| Hardware Context | 독립 실행 상태 저장 구조 |
| Register Set | CPU 실행 상태 저장 |
| Runqueue | 실행 대기 Thread Queue |
| APIC ID | CPU 식별 번호 |
| Physical Core | 실제 연산 하드웨어 |
| Hyper-Threading | 하나의 Core에 여러 Hardware Context 제공 |
| SMT Sibling | HT로 생성된 Logical CPU 쌍 |
| vruntime | Thread의 CPU 사용량 추적 값 |

### 결론

```
Logical CPU = Kernel에 노출된 Hardware Context = 독립 실행 상태(Register Set)를 가진 실행 슬롯
```

CFS는 Software Thread를 Logical CPU에 배치하고, Context Switch 시 Register 상태를 저장/복원합니다. HT 환경에서는 실제 연산 자원이 공유되므로 자원 경쟁이 발생할 수 있습니다.

결제 시스템에 대한 HT 설정 결론:

```
I/O Bound Payment Core     → HT ON 우선 검토
CPU Bound / Low-Latency    → HT OFF 또는 CPU Pinning 검토
최종 결정                   → P99/P999 Latency, TPS, Throttling, Context Switch, Cache Miss 기준 벤치마크
```

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*