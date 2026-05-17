# Register Storage (레지스터 저장)
## **Micro Foundations — 범용 컴퓨터 구조 / 시스템 관점**

> 정독: 0회

## 1. 이 기술이 무엇인가

**Register Storage**는:

> CPU가 연산 결과나 읽어온 데이터를 **초고속 레지스터 내부에 임시로 저장하는 과정**

CPU는 연산을 끝낼 때마다 결과를 어딘가 저장해야 한다. 그 저장 위치가 **Register File**이다.

**레지스터의 특성:**

| 특성 | 설명 |
|------|------|
| **속도** | 매우 빠름 (나노초 이하 접근) |
| **용량** | 매우 작음 |
| **휘발성** | 전원 차단 시 소멸 |
| **위치** | CPU 내부 직접 연결 |

> **핵심:** Register Storage는 **CPU가 다음 연산을 이어가기 위해 현재 상태를 잠시 붙잡아 두는 과정**이다.

---

## 2. 시스템 어디에서 등장하는가

레지스터 저장은 **거의 모든 CPU 명령 실행의 마지막 단계**에 등장한다.

**대표 사례:**

- ALU 연산 결과 저장
- 메모리 load 결과 저장
- branch 결과 상태 저장
- status flag 저장
- function call 상태 저장

**예시:**

```
ADD R1, R2  →  결과를 R3에 저장
RAM에서 읽은 값  →  Register에 저장
```

현대 CPU pipeline에서 **execution stage · write-back stage**의 핵심 구성 요소다.

> **결론:** CPU는 **계속 저장하면서 계산**한다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

압도적으로 **CPU**에 영향이 크다.

### CPU 영향
- 특히 Register File, Pipeline, Execution Unit, Context Switching, Scheduler에 영향이 매우 크다

### Memory 영향 (간접)
- **RAM 접근을 줄이기 위해 Register를 최대 활용**하기 때문에 간접적으로 매우 중요
- Register access는 RAM보다 훨씬 빠르다 → 레지스터 활용 효율이 CPU 성능 핵심

> **핵심:** Register Storage는 **CPU의 초고속 작업 메모리**다.

---

## 4. 왜 중요한가

CPU 연산 자체보다 중요한 것 중 하나가 **"연산 결과를 얼마나 빠르게 유지하느냐"**이다. 매번 RAM 접근하면 너무 느리기 때문이다.

현대 CPU 성능 대부분은 **"데이터를 레지스터 안에 얼마나 오래 유지하느냐"**에 좌우된다.

다음은 모두 **register 활용 극대화** 목적이다:

- compiler optimization
- register allocation
- out-of-order execution

---

## 5. 실제 장애와 어떤 관련이 있는가

### 1) Register Pressure
```
동시에 필요한 데이터가 많음 → 레지스터 부족
  ↓
RAM spill 발생 → 성능 급감
```

### 2) Context Switching Overhead
```
프로세스 전환 시 register backup/restore 필수
  ↓
스레드 많아질수록 Register Storage 비용 증가
```

### 3) Pipeline Stall
```
다음 명령이 아직 write-back 안 끝난 register 결과 대기
  ↓
성능 저하 발생
```

### 4) Dependency Hazard
```
앞 연산 결과가 register에 아직 반영 안 됨
  ↓
stall → forwarding 필요
```

### 5) Thermal / Power 문제
```
Register File은 CPU 내부에서도 매우 고전력 영역
  ↓
register access 최적화, physical register 관리 중요
```

> **핵심:** Register Storage 병목은 **CPU 전체 throughput 저하로 직결**된다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

| 메커니즘 | 의미 |
|----------|------|
| **Register File** | 레지스터 집합체 |
| **Write-back** | 연산 결과 최종 저장 단계 |
| **Latching** | 클록 타이밍에 맞춰 비트 상태 고정 |
| **RegWrite Signal** | 쓰기 허용 신호 |
| **Pipeline Register** | 단계 간 임시 저장 |
| **Forwarding / Bypassing** | write-back 전에 다음 연산으로 직접 전달 |
| **Register Renaming** | 논리 register 충돌 회피 — 현대 CPU 핵심 |
| **Physical vs Architectural Register** | CPU 내부 실제 구현 분리 |
| **Status Register / Flags** | 연산 상태 저장 |

> **핵심:** Register Storage는 **CPU가 현재 계산 상태를 유지하는 핵심 메커니즘**이다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

Register Storage는 CPU 내부이기 때문에 직접적으로는 거의 보이지 않는다. 하지만 **간접 관측이 가능**하다.

### Linux

**perf**
```bash
perf stat
perf top
```
관찰: IPC, stalled cycles, context switch, branch miss

**Context Switching**
```bash
vmstat
pidstat
```
관찰: `cs`(context switch) 증가 시 → register backup/restore 증가를 의미

**CPU Stall 분석**
```bash
perf record
perf report
```

---

### Runtime

관찰 포인트:
- IPC 저하
- context switch 빈도
- stalled cycles 증가

---

### Kubernetes

| 관찰 영역 | 명령어 | 관찰 |
|-----------|--------|------|
| **CPU throttling** | `kubectl top pod` | pipeline 효율 저하, register dependency delay 증가 |

> **핵심:** Register Storage 문제는 주로 **CPU stall·context switching·IPC 저하**로 드러난다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*