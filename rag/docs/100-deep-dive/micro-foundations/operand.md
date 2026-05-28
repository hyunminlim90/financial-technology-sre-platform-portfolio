# 피연산자 (Operand)

> 정독: 0회

## 1. 이 기술이 무엇인가

**피연산자(Operand)** 는:

> CPU 연산의 실제 대상 데이터

**예시:**

```
a & b
```

- `a` = 피연산자
- `b` = 피연산자
- `&` = 연산자(operator)

> 연산자가 처리하는 실제 데이터 값이 피연산자입니다.

피연산자는 레지스터 값, 메모리 값, 즉시값(immediate value), 주소값, 비트 스트림 등이 될 수 있습니다.

---

## 2. 시스템 어디에서 등장하는가

피연산자는 모든 CPU 명령어에 존재합니다.

| 영역 | 사례 |
|------|------|
| Arithmetic | ADD / SUB / MUL |
| Bitwise | AND / OR / XOR |
| Memory Access | LOAD / STORE |
| Branch | CMP |
| SIMD | vector operand |
| Runtime | object metadata |
| Kernel | page flags |
| Networking | packet header |
| Cryptography | binary block |

**예시:**

```asm
ADD R1, R2
; R1 = operand, R2 = operand
```

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 영향이 큰 자원은 **CPU + Register + Memory** 입니다.

피연산자는 실제 연산 데이터이므로 register loading, cache access, ALU execution, memory fetch 모두 연결됩니다.

> **operand fetch latency** 가 CPU 성능에 매우 중요합니다.

---

## 4. 왜 중요한가

CPU는 본질적으로:

> 피연산자를 처리하는 기계

입니다. 덧셈, 비교, 비트 연산, 주소 계산, 암호화, branch 모두 피연산자 기반입니다.

**시스템 최적화에서의 영향:**

| 요소 | 영향 |
|------|------|
| Operand Size | register pressure |
| Operand Location | cache miss |
| Operand Alignment | memory efficiency |
| Operand Dependency | pipeline stall |

---

## 5. 실제 장애와 어떤 관련이 있는가

### Invalid Operand
잘못된 operand 사용 시 invalid instruction, crash가 발생할 수 있습니다.

### Memory Corruption
손상된 operand address 사용 시 segmentation fault, invalid memory access가 발생할 수 있습니다.

### CPU Stall
operand fetch가 느리면 pipeline stall이 발생합니다.

### Cache Miss 폭증
operand가 메모리에 흩어져 있으면 latency 증가, throughput 감소가 발생할 수 있습니다.

### Register Pressure
operand가 너무 많으면 register spill, stack memory 사용 증가가 발생할 수 있습니다.

### Branch Misprediction
comparison operand 패턴이 불안정하면 branch predictor 효율이 저하될 수 있습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

**핵심:**
> CPU는 피연산자를 읽고 연산 결과를 다시 저장합니다.

### 핵심 흐름

```
1) Operand Fetch
   └─ CPU는 먼저 operand를 확보
      출처: register / memory / immediate value

2) Register Loading
   └─ 메모리 operand라면 LOAD 수행 후 register 적재

3) ALU Execution
   └─ 예: AND R1, R2
      ALU가 R1, R2를 operand로 사용

4) Result Generation
   └─ 연산 결과 생성

5) Writeback
   └─ 결과를 register 또는 memory에 저장
```

### Bitmask 관점 핵심

```c
flags & MASK
// flags = operand
// MASK  = operand
// &     = operator
```

CPU는 두 operand의 비트를 ALU 내부에서 비교합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Assembly 분석

```bash
objdump
gdb
```

### CPU Instruction Trace

```bash
perf
perf record
```

### Register 상태 확인

```bash
gdb
# info registers
```

### Cache/Operand 분석

```bash
perf stat
perf mem
```

### SIMD/Vector Operand 확인

```bash
perf top
```

### Kernel Bit Flags

```bash
cat /proc
# 내부적으로 operand 기반 bitwise 처리가 매우 많음
```

### Kubernetes 환경

K8s 자체는 고수준 플랫폼이지만 내부적으로 etcd, kernel, container runtime, scheduler 모두 operand 기반 CPU instruction 위에서 동작합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*