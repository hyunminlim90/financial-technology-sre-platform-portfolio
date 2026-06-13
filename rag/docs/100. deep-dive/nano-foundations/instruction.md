# 명령어 (Instruction)
## FinTech/SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**명령어(Instruction)** 는:

> 산술논리 연산 장치(ALU)가 수행해야 할 **가장 작은 실행 단위의 작업 지시**

쉽게 말하면:

> "컴퓨터에게 지금 무엇을 하라고 시키는 한 줄의 행동 명령"

예: 읽어라 / 저장해라 / 비교해라 / 이동해라 / 계산해라 / 점프해라

즉, **모든 소프트웨어는 결국 수많은 명령어들의 흐름**이다.

---

## 2. 시스템 어디에서 등장하는가

명령어는 **컴퓨터 시스템 전체의 가장 밑바닥 실행 단위**다.

### 프로그램 실행
애플리케이션 실행 시 코드, 함수, 로직 모두 최종적으로 명령어로 변환됨

### 운영체제
OS Scheduler, Memory Manager, Network Stack 모두 명령어 실행 결과

### 데이터 처리
암호화, 압축, 정렬, 검색, 해시 계산 모두 명령어 흐름

### 네트워크 처리
패킷 수신 후 복사, 검사, 라우팅, 응답 역시 명령어 집합

### 가상화 / 컨테이너
Hypervisor, Kernel, Runtime 모두 명령어 실행 기반

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적 영향: **CPU(Compute 실행 자원)**

| 자원 | 명령어와의 관계 |
|------|-------------|
| CPU | 명령어 실행 주체 |
| Memory | 명령어와 데이터가 저장되는 공간 |
| Cache | 명령어 Fetch 속도에 큰 영향 |
| Branch Prediction | 명령 흐름 예측 실패 시 성능 급락 가능 |
| Disk | 프로그램 명령어 원본 저장 위치 |
| Network | 분산 시스템에서 원격 요청 자체가 새로운 명령 흐름 유발 |

---

## 4. 왜 중요한가

컴퓨터는 본질적으로 **명령어를 읽고 실행하는 기계**이다.

서비스, 데이터베이스, Kubernetes, AI, 클라우드 모두 결국 **명령어 실행 결과물**이다.

### 명령어 이해가 중요한 이유

| 이유 | 설명 |
|------|------|
| Performance Understanding | 성능 문제는 명령 실행 속도 / 명령 대기 시간 / 명령 병렬성 문제로 귀결됨 |
| Bottleneck Analysis | CPU가 왜 느린가 → 실제로는 명령 대기, 메모리 접근, 분기 실패, 캐시 미스 문제일 가능성 높음 |
| System Design | 효율적 시스템은 불필요한 명령 실행을 줄이는 방향으로 설계됨 |

---

## 5. 실제 장애와 어떤 관련이 있는가

SRE 관점에서 **명령어 수준 문제는 매우 중요**하다.

### CPU Saturation
명령어 실행량 폭증 시 CPU 100% → Latency 증가

### Instruction Stall
명령이 필요한 데이터를 기다림

원인: Cache Miss, Memory Latency

### Branch Misprediction
조건 흐름 예측 실패 시 Pipeline Flush → 성능 급락

### Infinite Loop
명령 흐름 종료 실패 시 CPU Burn → Hang

### Context Switching Overhead
실행 중인 명령 상태 저장/복원 반복 시 실제 작업보다 전환 비용 증가

### Lock Contention
동일 자원 접근 대기 시 명령 실행 정체

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

### Fetch → Decode → Execute
명령어 실행의 핵심 흐름:

```
Fetch   → 명령어 가져오기
Decode  → 무슨 작업인지 해석
Execute → 실제 연산 수행
```

즉: 읽고 → 이해하고 → 실행한다

### Sequential Execution
기본적으로 명령어는 순차 실행. 하지만 현대 시스템은 Pipeline, Out-of-Order, Speculative Execution 등으로 병렬 최적화 수행.

### Instruction Stream
프로그램은 **명령어들의 연속적인 흐름**이다.

### Data + Instruction Separation
실행에는 항상 **무엇을 할지(Instruction)** 와 **무엇으로 할지(Data)** 둘 다 필요.

### State Transition
명령어 실행은 결국 **시스템 상태 변경 과정**이다.

예: 메모리 값 변경 / 파일 생성 / 네트워크 송신 / 프로세스 생성

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### CPU Instruction Metrics
```bash
perf stat        # CPU cycle / instruction 비율 확인
perf top         # 실시간 명령어 Hotspot 확인
```

### Process Observation
```bash
top
htop
```

### Assembly Inspection
```bash
objdump -d <binary>
```

### System Call Trace
```bash
strace -p <pid>
```

### Profiling
```bash
perf record -p <pid>
perf report
```

### Kubernetes
직접 명령어는 보이지 않지만 아래 형태로 간접 관측:
- `CPU throttling`
- `container saturation`
- `latency spike`

### Runtime
런타임은 **고수준 코드 → 명령어 흐름**으로 변환 수행

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*