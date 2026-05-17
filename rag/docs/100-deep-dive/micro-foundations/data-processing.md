# Data Processing
## 1. 데이터 처리란 무엇인가

데이터 처리(Data Processing)는:

> 입력된 원본 데이터(raw data)를 정해진 규칙과 절차에 따라 변형·계산·정렬하여 **의미 있는 정보(Information)로 바꾸는 과정**

**"의미 없는 비트 열을 사용 가능한 정보로 바꾸는 과정"**

**예시 1 — 문자 변환:**

```
입력:  01000001
처리:  ASCII 규칙 적용
출력:  'A'
```

**예시 2 — 결제 처리:**

```
입력:  결제 요청 데이터
처리:  검증 + 계산 + 상태 판별
출력:  승인 / 거절 / 오류
```

컴퓨터 시스템의 본질은 결국 **Input → Processing → Output** 입니다.

---

## 2. 시스템 어디에서 등장하는가

데이터 처리는 시스템 전체에서 등장합니다.

```
User / Sensor / Network / Disk
  ↓
Input Data
  ↓
Main Memory
  ↓
CPU Processing
  ↓
Result / Storage / Output
```

**컴퓨터 내부 데이터 흐름:**

```
Disk / Network
  ↓
DRAM
  ↓
CPU Cache
  ↓
Register
  ↓
ALU
  ↓
Result Store
```

> 운영체제, 데이터베이스, 네트워크 장비, 브라우저, 컨테이너 런타임까지 모두 결국 **데이터 처리 시스템**입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

| 처리 유형 | 주요 자원 |
|----------|-----------|
| 계산 중심 | CPU |
| 대용량 데이터 | Memory |
| 파일 처리 | Disk IO |
| 통신 처리 | Network |
| 실시간 이벤트 | CPU + Network |
| 압축 / 암호화 | CPU |
| 캐시 처리 | Memory + Cache |

> **데이터 처리 방식이 시스템 자원 사용 패턴을 결정합니다.**

```
랜덤 메모리 접근 증가  →  Cache Miss 증가  →  DRAM Stall 증가
대량 파일 읽기         →  Disk IO 증가
대량 네트워크 패킷     →  Network Interrupt 증가
```

---

## 4. 왜 중요한가

CPU, 메모리, 네트워크, 디스크는 결국 데이터를 **읽고(Read), 변형하고(Process), 저장하고(Store), 전송하기(Transfer)** 위해 존재합니다.

**운영 관점에서 데이터 처리 흐름을 이해해야 분석할 수 있는 것들:**

- 어디서 병목이 발생하는가
- 어떤 자원이 포화되는가
- 왜 레이턴시가 증가하는가
- 어떤 계층에서 데이터가 지연되는가

> 데이터 처리는 **시스템 전체의 실제 작업(load)의 본질**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. CPU Saturation

```
Data Processing 증가
  ↓
ALU 연산 증가
  ↓
CPU Usage 증가
  ↓
Latency 증가
```

### 5-2. Memory Pressure

```
Large Working Set
  ↓
Memory Usage 증가
  ↓
Swap / OOM
```

### 5-3. Cache Miss 증가

```
Random Access
  ↓
Cache Miss
  ↓
DRAM Access 증가
  ↓
CPU Stall
```

### 5-4. IO Bottleneck

```
Disk Wait 증가
  ↓
IO Latency 증가
  ↓
Application Stall
```

### 5-5. Network Saturation

```
Packet Processing 증가
  ↓
NIC Queue 증가
  ↓
Drop / Retransmission
```

### 5-6. Backpressure

```
Input Rate > Processing Rate
  ↓
Queue Buildup
  ↓
Latency Explosion
```

> 분산 시스템에서 매우 흔한 장애 패턴입니다.

---

## 6. 핵심 메커니즘 요약

### 6-1. 데이터 처리는 결국 ALU 연산이다

```
Register → ALU → Result
```

| 연산 유형 | 예시 |
|----------|------|
| 산술 연산 | `+` `-` `*` `/` |
| 논리 연산 | `AND` `OR` `XOR` `Compare` |

### 6-2. 데이터는 메모리 계층을 이동한다

```
Disk → DRAM → Cache → Register → ALU
```

CPU는 **Register 내부 데이터만 직접 계산**할 수 있습니다.

### 6-3. 캐시 효율이 성능을 크게 좌우한다

| 계층 | 접근 속도 |
|------|-----------|
| L1 Cache | 수 ns 이하 |
| DRAM | 수십~수백 ns |
| Disk | μs ~ ms |

> **Cache Hit 증가 = 처리 속도 증가**

### 6-4. 처리 결과는 상태를 바꾼다

데이터 처리는 다음과 같은 **시스템 상태 전이(State Transition)** 를 만들어냅니다.

- 메모리 값 변경
- 상태 레지스터 변경
- 파일 상태 변경
- 네트워크 응답 생성

### 6-5. 처리 속도보다 입력 속도가 빠르면 병목이 생긴다

```
Incoming Data > Processing Capacity
  ↓
Queue 증가 / Memory 증가 / Latency 증가 / Timeout 증가
```

### 6-6. 데이터 처리 = 제어 흐름 + 데이터 흐름

| 흐름 | 의미 |
|------|------|
| **Control Flow** | 무엇을 할 것인가 |
| **Data Flow** | 무엇을 처리할 것인가 |

둘이 결합되어 실제 프로그램 실행이 됩니다.

---

## 7. Linux / Runtime / K8s 에서 관측 방법

### Linux

```bash
# CPU 사용량
top
htop
mpstat

# 메모리 사용량
free -h
vmstat

# IO 병목 확인
iostat
iotop

# 컨텍스트 스위치 / 큐 상태 (r: run queue, cs: context switch, wa: IO wait)
vmstat 1

# CPU 성능 분석
perf top
perf stat

# 시스템 콜 흐름 확인
strace -p <PID>
```

### Runtime

| 지표 | 핵심 질문 |
|------|-----------|
| Throughput / Latency | 처리 속도가 입력 속도를 따라가는가? |
| Queue Length / Blocking Time | 데이터가 어디서 병목되는가? |
| Error Rate / Retry Count | 오류 경로가 과도하게 실행되는가? |
| Memory Growth / CPU Saturation | 자원 소비가 증가하고 있는가? |

### Kubernetes

```bash
# Pod 자원 사용량
kubectl top pod

# 노드 자원 사용량
kubectl top node

# Pod 상태 확인
kubectl get pod

# 이벤트 / 재시작 / OOM / CPU Throttling 확인
kubectl describe pod

# 로그 기반 처리 흐름 확인
kubectl logs <pod>
```

**대표 상태:**

| 상태 | 의미 |
|------|------|
| `OOMKilled` | 메모리 초과로 강제 종료 |
| `CPU Throttling` | CPU 한도 초과로 처리 지연 |
| `CrashLoopBackOff` | 반복 비정상 종료 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*