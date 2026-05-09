# CPU Socket과 Multi-Socket CPU 구조

## 1. CPU Socket이란?

CPU Socket은 메인보드에서 CPU 패키지를 장착하는 물리적 인터페이스다. Multi-Socket 시스템에서는 여러 개의 CPU 패키지를 하나의 시스템에 동시에 장착할 수 있다.

```
Socket 0 → CPU 패키지 0
Socket 1 → CPU 패키지 1
```

---

## 2. 전체 계층 구조

```
Socket
  ↓
Physical Core
  ↓
Hardware Thread
  ↓
Logical CPU
  ↓
Kernel Scheduler
  ↓
Application Thread
```

---

## 3. Socket 내부 구성 요소

각 Socket은 다음 자원을 독립적으로 보유한다.

| 구성 요소 | 설명 |
|-----------|------|
| Physical Core | 실제 연산 유닛 |
| L1 / L2 / L3 Cache | CPU 내부 캐시 계층 |
| Memory Controller | 로컬 메모리 제어기 |
| NUMA Node | 로컬 메모리 영역 |
| PCIe Lane | I/O 연결 인터페이스 |

---

## 4. Socket 내부 구조 예시

```
Socket 0
  ├── Core 0
  │   ├── Hardware Thread 0 → cpu0
  │   └── Hardware Thread 1 → cpu1
  ├── Core 1
  │   ├── Hardware Thread 0 → cpu2
  │   └── Hardware Thread 1 → cpu3
  └── ...
```

---

## 5. 운영체제의 CPU 인식 방식

운영체제는 여러 Socket에 걸친 CPU 자원을 하나의 Logical CPU 집합으로 통합하여 관리한다.

### Logical CPU 계산

```
Total Logical CPUs = Σ (Physical Cores per Socket × Hardware Threads per Core)
```

### 계산 예시

| Socket | Physical Core 수 | Threads per Core | Logical CPU 수 |
|--------|-----------------|-----------------|----------------|
| Socket 0 | 4 | 2 | 8 |
| Socket 1 | 8 | 2 | 16 |
| **합계** | **12** | — | **24** |

Linux에서는 이를 `cpu0` ~ `cpu23`으로 나열하며, Socket 경계 없이 단일 풀로 스케줄링한다.

---

## 6. NUMA (Non-Uniform Memory Access)

Multi-Socket 시스템은 일반적으로 NUMA 구조를 사용한다. 각 Socket은 자체 로컬 메모리를 가지며, 다른 Socket의 메모리 접근은 인터커넥트를 경유하기 때문에 지연이 증가한다.

```
Socket 0 ↔ Local Memory 0    (낮은 지연)
Socket 1 ↔ Local Memory 1    (낮은 지연)

Socket 0 Core → Interconnect → Memory 1   (높은 지연)
```

### 메모리 접근 유형 비교

| 접근 유형 | 경로 | 지연 |
|-----------|------|------|
| Local Memory 접근 | Core → 동일 Socket Memory | 낮음 |
| Remote Memory 접근 | Core → Interconnect → 타 Socket Memory | 높음 |

### NUMA가 성능에 미치는 영향

| 영향 | 설명 |
|------|------|
| Memory Latency 증가 | Remote 접근 시 응답 지연 |
| Cache Coherency 비용 증가 | Socket 간 캐시 동기화 오버헤드 |
| Throughput 감소 | Interconnect 대역폭 병목 |
| Tail Latency 증가 | 응답 시간 불균형 발생 |

---

## 7. 비대칭 Socket 구성의 문제

Socket 간 Core 수 또는 클럭 특성이 다른 경우 추가적인 성능 문제가 발생할 수 있다.

### 비대칭 Core 구성 예시

```
Socket 0 → 4 Core (높은 Clock)
Socket 1 → 8 Core (낮은 Clock)
```

| 문제 | 설명 |
|------|------|
| Scheduler Load Imbalance | CPU 부하 불균형 |
| NUMA Imbalance | 메모리 접근 불균형 |
| Cache Locality 저하 | Remote 접근 비율 증가 |
| Latency Variance | 스레드 배치 위치에 따른 응답 시간 변동 |
| 실행 속도 불일치 | 동일 워크로드라도 Socket마다 실행 속도 차이 발생 |

---

## 8. Linux Scheduler의 NUMA 인식 스케줄링

현대 Linux Scheduler는 NUMA 구조를 인식하여 다음 목표를 기준으로 스레드를 배치한다.

| 목표 | 설명 |
|------|------|
| Local Memory 우선 사용 | NUMA Latency 감소 |
| CPU Locality 유지 | Cache 효율 증가 |
| Cross-Socket 이동 최소화 | Interconnect 비용 감소 |

---

## 9. NUMA 제어 도구

### numactl

특정 NUMA Node에 CPU와 메모리를 함께 고정하여 Remote 접근을 방지한다.

```bash
# cpu0이 속한 NUMA Node 0에서 실행, 메모리도 Node 0만 사용
numactl --cpunodebind=0 --membind=0 java -jar app.jar
```

### NUMA 구조 확인

```bash
numactl --hardware       # NUMA Node 구성 및 메모리 크기 확인
numactl --show           # 현재 NUMA 정책 확인
```

---

## 10. SRE 관점 주요 확인 명령어

### CPU 전체 구조 확인

```bash
lscpu
```

주요 출력 항목:

| 항목 | 의미 |
|------|------|
| `Socket(s)` | 물리 CPU 패키지 수 |
| `NUMA node(s)` | NUMA Node 수 |
| `Core(s) per socket` | Socket당 Physical Core 수 |
| `Thread(s) per core` | Core당 Hardware Thread 수 |
| `CPU(s)` | 총 Logical CPU 수 |

### 상세 토폴로지 확인

```bash
cat /proc/cpuinfo          # 각 Logical CPU의 physical id / core id 확인
numactl --hardware         # NUMA Node별 CPU 목록 및 메모리 크기
taskset -p <pid>           # 특정 프로세스의 CPU Affinity 확인
```

---

## 11. Kubernetes와 Multi-Socket

Kubernetes는 기본적으로 Logical CPU 기준으로 자원을 할당하며, Socket 경계를 인식하지 않는다.

### CPU Manager를 통한 전용 Core 할당

고성능 워크로드에서 특정 Physical Core를 전용 할당하려면 다음 조건이 필요하다.

```yaml
# Pod 설정
resources:
  requests:
    cpu: "4"
  limits:
    cpu: "4"   # requests == limits → Guaranteed QoS
```

```
Guaranteed QoS + CPU Manager Static Policy
  → 특정 Physical Core 전용 할당
  → SMT 경쟁 및 NUMA Cross-Socket 접근 최소화 가능
```

### NUMA 토폴로지 정책 (TopologyManager)

Kubernetes TopologyManager를 활성화하면 CPU와 메모리를 동일 NUMA Node에서 할당하도록 정책을 설정할 수 있다.

| 정책 | 설명 |
|------|------|
| `none` (기본) | NUMA 인식 없음 |
| `best-effort` | 가능한 경우 동일 NUMA Node 할당 |
| `restricted` | NUMA 정렬 불가 시 Pod 스케줄링 제한 |
| `single-numa-node` | 단일 NUMA Node 할당 강제 |

---

## 12. JVM 및 고성능 서버에서의 영향

### Thread Migration 문제

JVM Thread가 Scheduler에 의해 다른 Socket으로 이동하면 다음 문제가 발생한다.

```
Thread → 다른 Socket으로 이동
  → L1/L2 Cache Warmup 손실
  → Remote Memory 접근 발생
  → Latency 증가
```

### GC Thread와 NUMA

GC Thread가 여러 Socket에 분산되면 Heap 메모리 접근 시 Remote Memory 접근이 증가할 수 있다. JVM 옵션 `-XX:+UseNUMA`를 통해 NUMA 인식 Heap 할당을 활성화할 수 있다.

### Netty / Kafka

고성능 네트워크 시스템에서는 다음 전략을 사용한다.

| 전략 | 목적 |
|------|------|
| CPU Pinning | 특정 Socket의 Core에 Thread 고정 |
| NUMA-aware 메모리 할당 | Local Memory 접근률 극대화 |
| NIC와 동일 NUMA Node 사용 | PCIe 데이터 이동 시 Remote 접근 방지 |

---

## 13. 구성 요소 요약

| 구성 요소 | 역할 |
|-----------|------|
| Socket | CPU 패키지 장착 단위 |
| Physical Core | 실제 연산 유닛 |
| Hardware Thread | 하드웨어 실행 컨텍스트 |
| Logical CPU | 운영체제 스케줄링 단위 |
| NUMA Node | Socket에 연결된 로컬 메모리 영역 |
| Scheduler | Logical CPU 기준 실행 배치 관리 |

---

## 14. 성능 최적화 핵심 요소

```
Socket Topology 인식
+ NUMA 로컬 메모리 접근 극대화
+ Core 배분 균형 (대칭 구성 선호)
+ SMT 구조 고려
+ Scheduler NUMA-aware 배치 활용
= Stable Throughput + Low Latency
```

단순 Logical CPU 개수만이 아닌 Socket 구조와 NUMA 계층까지 고려한 설계가 필요한 환경은 다음과 같다.

- Kubernetes CPU Manager + TopologyManager 정책 설계
- JVM Thread Pool 및 GC 설정 (`-XX:+UseNUMA`)
- Netty / Kafka NUMA-aware 배포 구성
- 금융 시스템 저지연 Core Pinning
- 대규모 트래픽 처리 서버 아키텍처

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*