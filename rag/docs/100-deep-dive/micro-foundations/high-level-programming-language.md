# 고수준 프로그래밍 언어 (High-Level Programming Language)

> 정독: 0회

## 1. 이 기술이 무엇인가

고수준 프로그래밍 언어는:

> 인간이 이해하기 쉬운 문법과 추상화 구조를 사용하여 **컴퓨터 시스템의 동작을 기술하는 프로그래밍 언어**

### 핵심 특징

고수준 언어는 다음을 목표로 합니다.

- 사람이 읽기 쉬움
- 하드웨어 세부사항 숨김
- 운영체제 / CPU 차이 추상화
- 복잡한 메모리 제어 단순화
- 생산성과 유지보수성 향상

### 대표 요소

변수, 함수, 클래스, 조건문, 반복문, 타입 시스템, 모듈

### 대표 언어

C, C++, Java, Python, Go, Rust, Kotlin, Swift

---

## 2. 시스템 어디에서 등장하는가

고수준 언어는 거의 모든 소프트웨어 시스템의 출발점입니다.

### Application Layer

- 웹 서버
- 데이터베이스
- 게임 / AI
- 운영 도구

### System Software

- 운영체제 일부
- 런타임 / 컴파일러
- 드라이버 일부

### Cloud / Infra

- Kubernetes
- Container Runtime
- IaC 도구
- Observability Agent

### Embedded / Hardware Control

일부 언어는 펌웨어 / 임베디드까지 확장됩니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

고수준 언어 자체는 **CPU와 Memory 구조에 가장 직접적인 영향**을 줍니다.

| 자원 | 영향 항목 |
|---|---|
| **CPU** | 실행 모델, 함수 호출 구조, 스레드 모델, JIT/AOT, 최적화 방식 |
| **Memory** | stack/heap 구조, object allocation, GC 여부, ownership, pointer model |
| **Disk** | binary size, serialization, logging |
| **Network** | async runtime, concurrency model, socket abstraction |

---

## 4. 왜 중요한가

현대 시스템 대부분이 **고수준 언어 기반으로 구축**되기 때문입니다.

### 핵심 이유

고수준 언어는 다음을 가능하게 합니다.

- 복잡한 시스템 구현
- 유지보수 가능한 코드베이스
- 대규모 협업
- 플랫폼 이식성 제공
- 안전성 향상

### 현대 시스템 특징

실제 서비스 장애의 상당수는 다음과 연결됩니다.

- 언어 런타임 특성
- 메모리 모델
- concurrency model
- GC
- async execution

### SRE 관점

고수준 언어를 이해해야 latency, memory leak, CPU spike, deadlock, thread starvation 원인 분석이 가능합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

| 장애 유형 | 원인 |
|---|---|
| **Memory Leak** | 객체 해제 실패 |
| **CPU Spike** | 비효율적 연산 구조 |
| **Deadlock** | 동시성 모델 문제 |
| **Race Condition** | 공유 상태 동기화 실패 |
| **GC Pause** | runtime memory management 영향 |
| **Stack Overflow** | 재귀 / stack frame 과다 사용 |
| **OOM** | heap growth 실패 |
| **Async Saturation** | event loop / blocking 충돌 |
| **Serialization Bottleneck** | object encoding 비용 증가 |

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

핵심 메커니즘은 **7개**입니다.

| # | 메커니즘 | 설명 |
|---|---|---|
| 1 | **Source Code** | 인간이 작성하는 텍스트 기반 코드 |
| 2 | **Compiler / Interpreter** | 코드를 실행 가능한 형태로 변환 |
| 3 | **Runtime** | memory management, thread scheduling, exception handling 등 실행 환경 제공 |
| 4 | **Abstraction** | 하드웨어 세부사항 숨김 |
| 5 | **Type System** | 데이터 구조와 제약 정의 |
| 6 | **Memory Model** | 메모리 접근 규칙 정의 |
| 7 | **Concurrency Model** | thread, async, actor, coroutine 등 동시 실행 구조 정의 |

### 핵심 흐름

```
Source Code
→ compiler / interpreter
→ runtime
→ operating system
→ CPU execution
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Process 실행 상태

```bash
ps
top
htop
```

### Memory 사용량

```bash
pmap
smem
vmstat
```

### CPU profiling

```bash
perf
flamegraph
```

### Binary 분석

```bash
file
ldd
nm
objdump
```

### Runtime 상태

언어별 runtime metrics 확인: heap, thread, GC, scheduler

### Container 환경

```bash
kubectl top pod
```

### OOM 여부

```bash
kubectl describe pod
dmesg
```

### System Call 관측

```bash
strace
ltrace
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*