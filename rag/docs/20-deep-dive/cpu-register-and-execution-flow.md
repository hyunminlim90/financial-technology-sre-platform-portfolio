# CPU Register(레지스터)와 연산 실행 구조

## 1. Register란?

Register는 CPU Core 내부에 존재하는 최상위 저장 계층이다. CPU는 연산을 수행하기 전에 반드시 데이터를 Register에 적재해야 하며, 대부분의 연산은 Register를 직접 대상으로 수행된다.

### Register의 목적

| 목적 | 설명 |
|------|------|
| 연산 데이터 저장 | ALU/FPU 입력 데이터 보관 |
| 연산 결과 저장 | 계산 결과 임시 저장 |
| 명령 실행 제어 | 현재 실행 상태 유지 |
| 메모리 주소 관리 | 데이터 및 명령어 위치 관리 |
| CPU 상태 저장 | 실행 상태 및 플래그 관리 |

---

## 2. 메모리 계층 구조에서의 위치

Register는 전체 메모리 계층의 최상위에 위치하며, 접근 속도가 가장 빠르고 용량이 가장 작다.

```
Register          ← 가장 빠름 / 가장 작음
  ↓
L1 Cache
  ↓
L2 Cache
  ↓
L3 Cache
  ↓
RAM
  ↓
Storage           ← 가장 느림 / 가장 큼
```

### Register 특징

| 항목 | 설명 |
|------|------|
| 위치 | CPU Core 내부 |
| 속도 | 가장 빠름 |
| 접근 지연 | 거의 없음 (sub-cycle) |
| 용량 | 매우 작음 (수십 개 수준) |
| 접근 방식 | CPU 명령어가 직접 지정 |

---

## 3. CPU 연산과 Register

CPU는 메모리 데이터를 직접 연산하지 않는다. 반드시 Register를 경유해야 한다.

```
Memory
  ↓ Load
Register          ← 연산 입력
  ↓
ALU / FPU 연산
  ↓
Register          ← 연산 결과
  ↓ Store
Memory
```

이 구조는 x86, ARM 등 대부분의 현대 아키텍처에서 공통적으로 적용된다.

---

## 4. Register의 종류

### 4-1. General Purpose Register (GPR, 범용 레지스터)

일반 데이터 연산 및 메모리 주소 관리에 사용되는 레지스터다.

| 용도 | 설명 |
|------|------|
| 정수 연산 | ADD, SUB, MUL 등의 입출력 |
| 메모리 주소 저장 | 포인터 값 보관 |
| 함수 인자 전달 | Calling Convention에 따른 인자 전달 |
| 임시 데이터 저장 | 중간 계산 결과 보관 |

#### x86-64 주요 범용 레지스터

| Register | 주요 역할 |
|----------|-----------|
| RAX | 연산 결과, 반환값 |
| RBX | 일반 데이터 |
| RCX | 반복 카운터 |
| RDX | 데이터 연산 보조 |
| RSI | Source Pointer |
| RDI | Destination Pointer (첫 번째 함수 인자) |
| RSP | Stack Pointer |
| RBP | Base Pointer (스택 프레임 기준) |

> 64-bit CPU는 Register 크기가 64-bit임을 의미하며, 한 번에 64-bit 정수 및 메모리 주소를 처리할 수 있다.

---

### 4-2. Special Purpose Register (특수 목적 레지스터)

CPU 제어 흐름 및 상태 관리에 사용되는 레지스터다.

#### Program Counter (PC)

다음에 실행할 명령어의 메모리 주소를 저장한다. Instruction Fetch 단계에서 참조된다.

```
PC → 다음 명령어 주소 → Instruction Fetch
```

#### Instruction Register (IR)

현재 실행 중인 명령어를 저장한다. Instruction Decode 단계에서 참조된다.

#### Stack Pointer (SP)

현재 Stack의 최상단 주소를 가리킨다. 함수 호출, 로컬 변수 관리에 사용된다.

#### Status Register / Flags Register

직전 연산 결과의 상태를 저장한다. 조건 분기 명령어가 이 값을 참조한다.

| 플래그 | 설명 |
|--------|------|
| Zero Flag (ZF) | 연산 결과가 0인 경우 설정 |
| Carry Flag (CF) | 비트 자리올림 발생 시 설정 |
| Overflow Flag (OF) | 부호 있는 정수 오버플로우 시 설정 |
| Sign Flag (SF) | 연산 결과가 음수인 경우 설정 |

---

### 4-3. SIMD Register (벡터 레지스터)

여러 데이터를 하나의 명령어로 병렬 처리하는 벡터 연산에 사용된다.

| 기술 | 설명 |
|------|------|
| SSE | 128-bit SIMD 연산 |
| AVX | 256-bit 확장 벡터 연산 |
| AVX-512 | 512-bit 대규모 병렬 벡터 연산 |

```
1개의 AVX Register (256-bit)
  → 8개의 32-bit 정수 동시 연산 가능
  → 4개의 64-bit 부동소수점 동시 연산 가능
```

---

## 5. Register File

CPU 내부의 Register 전체 집합을 **Register File**이라고 한다. ALU/FPU는 Register File과 직접 연결되어 데이터를 읽고 쓴다.

```
Register File
  ├── RAX
  ├── RBX
  ├── RCX
  ├── RSP
  ├── RBP
  └── ... (XMM, YMM 등 SIMD 포함)
```

---

## 6. Pipeline과 Register

Pipeline 각 단계는 Register를 통해 데이터를 주고받는다.

```
IF  (Instruction Fetch)     ← PC 참조
  ↓
ID  (Instruction Decode)    ← IR 사용
  ↓
EX  (Execute)               ← GPR 읽기, ALU/FPU 연산
  ↓
MEM (Memory Access)         ← LSU를 통한 Cache/RAM 접근
  ↓
WB  (Write Back)            ← 연산 결과를 Register에 기록
```

---

## 7. LSU와 Register의 관계

LSU(Load Store Unit)는 Register와 메모리(Cache/RAM) 사이의 데이터 이동을 담당한다.

```
Load:  RAM → Cache → Register
Store: Register → Cache → RAM
```

| 명령 | 방향 |
|------|------|
| Load | Memory → Register |
| Store | Register → Memory |

---

## 8. Register Pressure와 Register Spilling

### Register Pressure

필요한 변수의 수가 사용 가능한 Register 수를 초과하는 상황이다.

```
활성 변수 수 > 사용 가능한 Register 수
→ Register Pressure 발생
```

### Register Spilling

Register Pressure 발생 시 컴파일러(또는 JIT)는 일부 Register 값을 Stack 메모리에 임시 저장한다.

```
Register
  ↓ Spill
Stack Memory (RAM)
  ↓ Reload 시
Register
```

| 영향 | 설명 |
|------|------|
| 메모리 접근 증가 | LSU 사용 빈도 증가 |
| Cache Miss 가능성 증가 | Spill된 데이터가 Cache에 없을 수 있음 |
| Pipeline Stall 증가 | 메모리 대기로 IPC 감소 |

---

## 9. Context Switching과 Register

스레드 전환 시 현재 스레드의 Register 상태 전체를 저장하고, 다음 스레드의 Register 상태를 복원해야 한다.

### 저장 대상

| 항목 | 설명 |
|------|------|
| General Purpose Registers | 모든 범용 레지스터 값 |
| Program Counter | 다음 실행 명령어 위치 |
| Stack Pointer | 스택 상태 |
| Flags Register | CPU 연산 상태 |

### Context Switch 흐름

```
Thread A 실행 중
  → Thread A Register 상태 저장 (커널 메모리)
  → Kernel Scheduler 실행
  → Thread B Register 상태 복원
  → Thread B 실행 재개
```

### Context Switch 비용 구성

```
Register Save 비용
+ Register Restore 비용
+ Cache / TLB Flush 영향
= Context Switch 전체 비용
```

Register 수가 많을수록(SIMD 포함 시 더욱) Context Switch 비용이 증가한다.

---

## 10. Function Call과 Register

현대 아키텍처의 Calling Convention은 함수 인자를 Register로 전달하여 메모리 접근 비용을 줄인다.

### x86-64 Linux (System V AMD64 ABI)

| 인자 순서 | Register |
|-----------|----------|
| 1번째 인자 | RDI |
| 2번째 인자 | RSI |
| 3번째 인자 | RDX |
| 4번째 인자 | RCX |
| 5번째 인자 | R8 |
| 6번째 인자 | R9 |
| 7번째 이상 | Stack 사용 |
| 반환값 | RAX |

인자가 6개 이하인 경우 Stack 접근 없이 Register만으로 함수 호출이 완료된다.

---

## 11. JVM과 Register 최적화

JVM JIT Compiler는 런타임에 Register 최적화를 수행한다.

### Register Allocation

JIT Compiler는 자주 접근하는 변수(Hot Variable)를 가능한 한 Register에 유지하도록 코드를 재컴파일한다.

```
Hot Variable 감지
  → Register Allocation 적용
  → 메모리 접근 감소
  → 성능 향상
```

### Escape Analysis

객체가 생성된 메서드 범위를 벗어나지 않는다고 판단되면, Heap 할당을 생략하고 Stack 또는 Register 수준에서 처리한다.

```
객체가 메서드 외부로 전달되지 않음 (Non-escaping)
  → Heap 할당 제거 (Scalar Replacement)
  → 필드를 개별 Register/Stack 변수로 분해
  → GC 부담 감소
```

---

## 12. CPU 내부 전체 실행 흐름

```
Branch Predictor (분기 예측)
  ↓
Instruction Fetch (PC 참조)
  ↓
Instruction Decode (IR 사용)
  ↓
Register 읽기 (Register File)
  ↓
ALU / FPU 연산
  ↓
LSU Load (필요 시: Cache → RAM)
  ↓
Register 결과 기록 (Write Back)
  ↓
LSU Store (필요 시: Register → Cache → RAM)
```

---

## 13. Linux 및 성능 분석 도구

### Context Switch 모니터링

```bash
vmstat 1        # 전체 시스템 Context Switch 수 (cs 항목)
pidstat -w 1    # 프로세스별 Context Switch 수
```

### CPU 이벤트 분석

```bash
perf stat <command>    # IPC, Cache Miss, Context Switch 등 종합 분석
perf top               # 실시간 Hot Function 분석
```

### 어셈블리 및 Register 사용 분석

```bash
objdump -d <binary>    # 어셈블리 코드 확인
perf annotate          # 함수별 CPU 사이클 분포 확인
```

---

## 14. 구성 요소 요약

| 구성 요소 | 역할 |
|-----------|------|
| Register | CPU 내부 최상위 저장소, 연산 입출력 직접 담당 |
| GPR (범용 레지스터) | 정수 연산, 주소 관리, 함수 인자 전달 |
| Program Counter (PC) | 다음 실행 명령어 주소 저장 |
| Stack Pointer (SP) | 현재 Stack 위치 관리 |
| Flags Register | 연산 결과 상태 저장 (조건 분기 기준) |
| Register File | CPU 내 전체 Register 집합 |
| Register Spill | Register 부족 시 Stack 메모리에 임시 저장 |
| SIMD Register | 벡터 병렬 연산 전용 레지스터 |
| Register Allocation | 컴파일러/JIT의 Register 최적 배치 전략 |

---

## 15. 성능 최적화 핵심 요소

```
Efficient Register Allocation (Register 낭비 최소화)
+ Low Register Spill (Stack 접근 최소화)
+ Fast Register Access (연산 직접 수행)
+ Efficient Pipeline Flow (Stall 최소화)
= High IPC
= High Throughput + Low Latency
```

이 원칙은 다음 환경의 성능 최적화와 직접 연결된다.

- JVM JIT 컴파일러의 Register Allocation 및 Escape Analysis
- Context Switching 비용 최소화 (논블로킹 서버 구조)
- SIMD 활용 고성능 수치 연산
- 금융 시스템 저지연 처리
- 대규모 트래픽 처리 아키텍처 설계

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*