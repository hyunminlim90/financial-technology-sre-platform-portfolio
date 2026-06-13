# 프로세싱 코어 (Processing Core)

> 정독: 0회

프로세싱 코어는:

> **CPU 내부에서 독립적으로 명령어를 실행하고 연산을 수행하는 실제 실행 엔진**

**쉽게 말하면:**

"실제로 프로그램을 돌리는 CPU 내부의 작업자"

---

## 1. 이 기술이 무엇인가

프로세싱 코어는 CPU 내부의 **독립 실행 단위**입니다.

각 코어는 자체적으로 다음을 수행할 수 있습니다.

- 명령어 인출 (Fetch)
- 해독 (Decode)
- 실행 (Execute)
- 결과 기록 (Write Back)

즉, **코어 하나 = 작은 CPU 하나**에 가깝습니다.

현대 CPU는 보통 **멀티코어(Multi-Core)** 구조를 사용합니다.

---

## 2. 시스템 어디에서 등장하는가

컴퓨터 구조 내부에서 다음과 같은 흐름으로 등장합니다.

```
Program
↓
Process
↓
Thread
↓
OS Scheduler
↓
Processing Core   ← 실제 실행
```

### 메모리 계층과의 연결

```
Register
↓
L1 Cache
↓
L2 Cache
↓
L3 Cache
↓
DRAM
```

코어는 위 계층을 사용하며 연산을 수행합니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향 |
|------|------|
| CPU | 절대적 |
| Memory | 매우 큼 |
| Cache | 매우 큼 |
| Network | 간접 영향 |
| Disk | 간접 영향 |

특히 **CPU 연산 처리량**의 핵심입니다.

---

## 4. 왜 중요한가

컴퓨터 시스템에서 실제 계산은 모두 코어가 수행합니다.

- 데이터 압축
- 암호화
- JSON 파싱
- DB 쿼리 처리
- 네트워크 패킷 처리
- AI 연산

모두 코어가 계산합니다. 즉, **코어 성능 = 시스템 처리 성능**에 매우 가깝습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. CPU Saturation

코어 사용률이 100%에 가까워지면:

```
Runnable Queue 증가 → 응답 지연 증가
```

### 5-2. Context Switch 폭증

코어 수보다 실행 스레드가 많아지면 **Context Switch**가 증가합니다.

**결과:** Cache Pollution → CPU Stall 증가

### 5-3. Single Core Bottleneck

특정 작업이 단일 스레드만 사용하면 코어 하나만 과부하될 수 있습니다.

전체 CPU 사용률은 낮아도 **실제 서비스는 느려질 수 있습니다.**

### 5-4. Cache Miss 증가

코어가 필요한 데이터를 캐시에서 못 찾으면 DRAM 접근이 증가합니다.

**결과:** 코어가 연산 대신 대기

### 5-5. NUMA 병목

멀티소켓 서버에서는 다른 CPU의 메모리에 접근하는 경우가 발생할 수 있습니다.

**결과:** Remote Memory Latency 증가

---

## 6. 핵심 메커니즘

### 6-1. 코어는 독립 실행 엔진이다

각 코어는 자체적으로 Control Unit, ALU, Register, Pipeline을 가집니다.

즉, **각 코어가 동시에 서로 다른 작업을 수행할 수 있습니다.**

### 6-2. 코어는 인출-실행 루프를 수행한다

코어 내부에서는 계속 다음 사이클이 반복됩니다.

```
Fetch → Decode → Execute → Write Back
```

즉, **프로그램 실행의 실제 물리적 주체**입니다.

### 6-3. 코어는 캐시와 강하게 결합된다

코어는 L1/L2 캐시와 거의 붙어 있습니다. DRAM은 너무 느리기 때문입니다.

### 6-4. 코어 수 증가 = 병렬 처리 증가

멀티코어 시스템에서는 Core 0, 1, 2, 3이 동시에 작업할 수 있습니다.

즉, **병렬 처리량이 증가**합니다.

### 6-5. SMT (Hyper-Threading)는 논리 프로세서다

현대 CPU는 다음 구조를 사용할 수 있습니다.

```
1 Physical Core → 2 Logical Processor
```

즉, **유휴 연산 자원 활용을 극대화**합니다.

### 6-6. 코어는 캐시 미스에 매우 민감하다

코어 속도는 매우 빠르지만 DRAM 접근은 느립니다.

따라서 **Cache Hit Ratio**가 매우 중요합니다.

### 6-7. 스케줄러는 코어에 작업을 배치한다

운영체제는 어떤 스레드를 어느 코어에서 실행할지 결정합니다.

즉, **OS Scheduler ↔ Core**는 매우 밀접한 관계입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 코어 개수 확인
lscpu

# 논리 프로세서 확인
nproc

# CPU 토폴로지 확인
lstopo

# 실시간 코어 사용률
htop
# 또는
top

# 스케줄링 상태 확인
mpstat -P ALL 1

# Context Switch 확인 (cs 항목)
vmstat 1
```

### Runtime

주요 관측 포인트:

- CPU Usage
- IPC
- Context Switch
- Run Queue
- Cache Miss
- Thread Affinity

> **핵심 질문:** 코어가 실제 연산 중인가? 아니면 메모리 대기 중인가?

### Kubernetes

```bash
# 노드 CPU 상태
kubectl top node

# Pod CPU 사용량
kubectl top pod

# NUMA / Topology 확인
kubectl describe node
```

```yaml
# CPU 제한 확인
resources:
  limits:
    cpu: "2"
```

> **CPU Pinning** — 고성능 환경에서는 **CPU Manager Static Policy** 사용 가능

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*