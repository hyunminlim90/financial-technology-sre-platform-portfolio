# 레지스터 (Register)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**레지스터(Register)** 는:

> CPU 내부에 존재하는 **초고속 임시 저장 공간**

CPU가 연산을 수행할 때 데이터, 주소, 명령어, 상태값 등을 가장 가까운 위치에서 즉시 접근하기 위해 사용한다.

### 핵심 특징

| 특징 | 설명 |
|------|------|
| 위치 | CPU Core 내부 |
| 속도 | 시스템 전체에서 가장 빠름 |
| 용량 | 매우 작음 |
| 역할 | 연산 직전 데이터 저장 |

즉, **ALU가 직접 사용하는 작업 공간**이다.

---

## 2. 시스템 어디에서 등장하는가

레지스터는 **CPU가 동작하는 모든 영역**에서 사용된다.

### CPU 연산
- 산술 연산
- 논리 연산
- 비교 연산

### 명령어 실행
- Program Counter
- Instruction Pointer

### 함수 호출
- Stack Pointer
- Frame Pointer

### Context Switch
- Thread State 저장
- Process State 복원

### 암호화 / Hash 연산
- `SHA`
- `AES`
- `TLS`

### JVM Runtime
- JIT Generated Native Code
- Hot Method Execution

### Kernel Scheduler
- Process Context Save/Restore

### Virtualization
- VM Context Switch
- vCPU State Save

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접 영향: **CPU**

레지스터는 **CPU 성능의 최전선**에 위치한다.

| 영역 | 영향 |
|------|------|
| IPC | 명령어 처리량 |
| Pipeline | 실행 효율 |
| Context Switch | 스케줄링 비용 |
| Cache Miss | Stall 증가 |

레지스터 부족 시:
- 메모리 접근 증가
- Pipeline Stall 증가
- Latency 증가

---

## 4. 왜 중요한가

CPU는 **레지스터 기반으로만 직접 연산 가능**하다.

> ALU는 메모리(RAM)를 직접 계산하지 않는다.
> 반드시 `RAM → Cache → Register` 흐름을 거쳐야 한다.

따라서 레지스터 효율은 CPU 처리량, 응답속도, 전력효율 전체에 영향을 미친다.

### FinTech에서 중요한 이유

결제 시스템은 낮은 latency, 높은 TPS, 짧은 timeout을 요구한다.

> ⚠️ **레지스터 효율 저하 = 직접적인 성능 저하**

---

## 5. 실제 장애와 어떤 관련이 있는가

### Context Switch 폭증
Thread 전환 시 Register Save / Register Restore 반복 발생

결과: CPU Overhead 증가, Tail Latency 증가

### Register Spill
레지스터 부족 시 Stack Memory 사용 증가 발생

결과: Memory Access 증가, Cache Miss 증가

### Pipeline Stall
필요 데이터가 레지스터에 준비 안 되면 CPU Idle / Execution Delay 발생

### Interrupt Storm
IRQ 폭증 시 Register Context Save 빈발 발생

결과: CPU Busy, Throughput 감소

### Virtualization Overhead
VM/vCPU 전환 시 Guest Register 상태 저장 반복

결과: Hypervisor Overhead 증가

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

### General Purpose Register (GPR)
일반 연산용 레지스터. 예: `RAX`, `RBX`, `RCX` (x86_64 기준)

### Program Counter (PC)
다음 실행 명령어 위치 저장

### Stack Pointer (SP)
현재 Stack 위치 저장

### Flag Register
비교/조건 결과 저장. 예: Zero Flag, Carry Flag

### Register Allocation
컴파일러/JIT가 어떤 변수를 어떤 레지스터에 배치할지 결정하는 과정

### Register Spill
레지스터 부족 시 RAM/Stack으로 임시 저장. 성능 저하 원인.

### Context Save/Restore
Thread/Interrupt 전환 시 레지스터 상태 백업/복원 수행

### SIMD Register
벡터 연산용 레지스터. 예: `AVX`, `SSE`

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### CPU Architecture 확인
```bash
lscpu
```

### Register 포함 Assembly 확인
```bash
objdump -d binary
```

### perf Assembly 분석
```bash
perf annotate
```

### Context Switch 확인
```bash
vmstat 1
pidstat -w 1
```

### CPU Pipeline 분석
```bash
perf stat
```

### JIT Assembly 확인
```bash
-XX:+PrintAssembly
```

### Flamegraph
```bash
async-profiler
```

### eBPF Context Switch 분석
```bash
# bcc-tools 대표 도구
offcputime
runqlat
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*