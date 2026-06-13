# Data Path (데이터 경로)
## **Micro Foundations — 범용 컴퓨터 구조 / 시스템 관점**

> 정독: 0회

## 1. 이 기술이 무엇인가

**Data Path**는:

> CPU 내부에서 데이터가 실제로 이동하고, 연산되고, 저장되는 **물리적 실행 경로**

쉽게 말하면:

| 구성 | 역할 |
|------|------|
| **Control Unit** | 지휘관 |
| **Control Signal** | 명령서 |
| **Data Path** | 실제 작업 수행 부대 |

CPU 내부에서 데이터를 읽고 → ALU로 보내고 → 연산하고 → 결과를 다시 저장하는 **실질적인 하드웨어 흐름 전체**가 Data Path다.

> **핵심:** Data Path는 **CPU 내부 비트 데이터가 실제로 흐르는 물리적 도로망**이다.

<details>
<summary>Deep Dive</summary></br>

Data(데이터) [[M]](../../100-deep-dive/micro-foundations/data.md)  
Data Transfer(데이터 전송) [[M]](../../100-deep-dive/micro-foundations/data-transfer.md)  
Arithmetic Logical Operation(산술논리연산) [[M]](../../100-deep-dive/micro-foundations/arithmetic-logical-operation.md)  
Register Storage(레지스터 저장) [[M]](../../100-deep-dive/micro-foundations/register-storage.md)

</details></br>

## 2. 시스템 어디에서 등장하는가

Data Path는 **CPU 내부 거의 모든 연산**에 등장한다.

**대표 영역:** 산술 연산, 논리 연산, 메모리 읽기, 레지스터 이동, branch 계산, address 계산, cache access, pipeline forwarding

즉, **Instruction이 실제 실행되는 모든 순간** Data Path가 동작한다.

**기본 흐름 예시:**

```
Register → ALU → Register
Memory → Register → ALU
```

현대 CPU에서는 superscalar · out-of-order · SIMD/vector unit 같은 고성능 구조들도 모두 **Data Path 확장 형태**다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **CPU**이다.

### CPU 영향
- 특히 ALU, Register File, Internal Bus, Pipeline, Execution Unit에 영향이 매우 크다

### Memory 영향
- **Data Path는 메모리 데이터 이동까지 포함**하기 때문에 매우 중요
- 예: load/store, cache line fetch, write-back

### Cache 영향
- 실제 CPU 성능 대부분은 **Data Path가 캐시와 얼마나 빠르게 연결되느냐**에 좌우된다

### Network/Disk 영향
- 직접 영향은 적음
- 단, packet 처리·storage I/O가 결국 CPU instruction 흐름으로 처리되므로 간접 영향 존재

> **핵심:** Data Path는 **CPU 내부 데이터 이동 효율 자체**를 결정한다.

---

## 4. 왜 중요한가

CPU는 **데이터를 실제로 이동시켜야만 연산할 수 있다.** Control Unit이 아무리 훌륭해도 **Data Path가 병목이면 실행 성능은 무너진다.**

특히 현대 CPU는 수십 단계 pipeline · 여러 execution unit · out-of-order scheduling을 사용한다.

이때 핵심 문제:

> **"데이터를 어떻게 충돌 없이 빠르게 공급할 것인가"**

> **결론:** Data Path는 **CPU 성능의 실제 물리적 한계선**이다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 1) Pipeline Stall
```
데이터 준비 안 됨 → ALU가 놀게 됨
  ↓
IPC 감소 → CPU utilization 비효율
```

### 2) Register Dependency Hazard
```
앞 연산 결과를 기다려야 함
  ↓
Data Path 흐름 중단
```

### 3) Cache Miss
```
필요 데이터가 cache에 없음
  ↓
RAM fetch 대기 발생 (현대 CPU 최대 병목 중 하나)
```

### 4) Bus Contention
```
여러 execution unit이 동일 내부 버스 점유 경쟁
  ↓
latency 증가 → throughput 감소
```

### 5) Context Switching
```
OS가 task 교체 시 register backup/restore 발생
  ↓
Data Path 자체가 엄청난 오버헤드 처리
```

### 6) Thermal / Power Constraint
```
Data Path 활성화 증가 → 전력 증가 → 발열 증가
  ↓
CPU throttling
```

> **핵심:** Data Path 병목은 **CPU 전체 처리량 감소로 직결**된다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 핵심 구성 요소

| 구성 요소 | 의미 |
|-----------|------|
| **Register File** | 초고속 데이터 임시 저장소 |
| **ALU** | 실제 연산 수행 |
| **Internal Bus** | 데이터 이동 통로 |
| **Multiplexer (MUX)** | 데이터 경로 선택 스위치 |
| **Control Signal** | 어떤 경로를 열지 결정 |
| **Pipeline Register** | 단계 간 데이터 임시 보관 |

### 주요 Data Path 유형

| 경로 | 의미 |
|------|------|
| **Forwarding / Bypassing** | 불필요한 register write-back 없이 직접 다음 ALU로 전달 |
| **Load/Store Path** | 메모리 접근 전용 경로 |
| **Branch Data Path** | 분기 주소 계산 |
| **Write-back Path** | 연산 결과 저장 |

> **핵심:** Data Path는 **CPU 내부 데이터 이동 구조 전체**다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

Data Path 자체는 CPU 내부라 직접 보이지 않는다. 하지만 **결과는 관측 가능**하다.

### Linux

**perf**
```bash
perf stat
perf top
```
관찰: cycles, instructions, IPC, cache-misses, stalled-cycles

**CPU Pipeline Metrics**
```bash
perf record
perf report
```

**NUMA 영향**
- 메모리 거리 증가 시 → Data Path latency 증가

---

### Runtime

관찰 포인트:
- CPU stall 빈도
- cache miss rate
- latency spike

---

### Kubernetes

| 관찰 영역 | 명령어 | 관찰 |
|-----------|--------|------|
| **CPU throttling** | `kubectl top pod` | pipeline 효율 저하, execution path 지연 |
| **PMU / eBPF** | 하드웨어 counter | hardware performance counter 관측 |

> **핵심:** Data Path 문제는 주로 **CPU stall·cache miss·latency spike** 형태로 드러난다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*