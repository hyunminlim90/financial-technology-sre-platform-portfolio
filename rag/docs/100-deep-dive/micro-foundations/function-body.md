# 함수 보디 (Function Body)

> 정독: 0회

## 1. 이 기술이 무엇인가

함수 보디는:

> 함수나 메서드 내부에 작성된 **실제 실행 코드 영역**

**핵심: 실제 연산 로직이 저장된 실행 가능한 코드 블록**

**일반적으로 포함되는 요소:**

| 구성 요소 | 설명 |
|-----------|------|
| 연산 코드 | 계산/조건/반복 |
| 제어 흐름 | if, loop, branch |
| 메모리 접근 | load/store |
| 함수 호출 | subroutine invocation |
| 반환 로직 | return |

함수 시그니처는 "무엇을 호출하는가"를 정의하고, 함수 보디는 **실제로 무엇을 수행하는가**를 정의합니다.

<details>
<summary>Deep Dive</summary></br>

Subroutine(서브루틴) [[M]](../../100-deep-dive/micro-foundations/subroutine.md)  
Method(메서드) [[M]](../../100-deep-dive/micro-foundations/method.md)  
Instruction(명령어) [[M]](../../100-deep-dive/micro-foundations/instruction.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

함수 보디는 런타임 실행 흐름 전체에서 등장합니다.

| 영역 | 역할 |
|------|------|
| Text Segment(Code Segment) | 기계어 저장 |
| CPU Pipeline | instruction execution |
| Stack | call frame 생성 |
| Runtime Engine | dispatch/execution |
| Instruction Cache | 코드 캐싱 |

**대표 흐름:**

```
function call
→ code address jump
→ stack frame creation
→ instruction execution
→ return
```

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향 |
|------|------|
| CPU | instruction execution |
| Instruction Cache | code locality |
| Stack Memory | local frame |
| Branch Predictor | control flow prediction |
| Memory | data access |

특히 branch complexity, loop depth, recursion, indirect call, memory access pattern은 CPU 효율에 직접 영향을 줍니다.

---

## 4. 왜 중요한가

실제 프로그램 동작은 **함수 보디 실행의 연속**입니다.

비즈니스 로직, 데이터 처리, 네트워크 처리, 파일 처리, 상태 변경 모두 함수 보디 내부에서 수행됩니다.

또한 함수 보디 구조는 성능, 메모리 사용량, CPU 효율, 캐시 효율, 장애 발생 가능성에 직접 영향을 줍니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### Infinite Loop

함수 보디 내부 무한 반복 시 CPU 100%, thread starvation이 발생할 수 있습니다.

### Stack Overflow

재귀 함수 보디 반복 시 stack exhaustion, process crash가 발생할 수 있습니다.

### Invalid Memory Access

잘못된 포인터 접근 시 segmentation fault, access violation이 발생할 수 있습니다.

### High Latency

복잡한 함수 보디로 long execution time, request delay가 발생할 수 있습니다.

### Branch Misprediction

조건 분기 과다 시 CPU pipeline flush, frontend stall이 증가할 수 있습니다.

### Cache Miss

코드 크기 과대 시 instruction cache miss, I-cache thrashing이 발생할 수 있습니다.

---

## 6. 핵심 메커니즘

### 1) Function Call 발생

프로그램이 call instruction을 수행합니다.

### 2) 실행 주소 결정

바인딩 과정을 통해 static dispatch 또는 dynamic dispatch 중 하나를 수행하여 실제 함수 보디 주소를 결정합니다.

### 3) Program Counter 이동

CPU의 PC(program counter)가 함수 보디 시작 주소로 이동합니다.

### 4) Stack Frame 생성

함수 실행을 위한 local variable 영역, parameter 영역, return address가 생성됩니다.

### 5) Instruction Fetch/Decode 수행

CPU가 instruction fetch → decode → execute를 반복 수행합니다. 즉 함수 보디는 실제로 **instruction stream**입니다.

### 6) Memory Access 수행

함수 보디 내부에서 object access, variable load/store, pointer dereference 등이 발생합니다.

### 7) Return 수행

함수 종료 시 stack frame 제거, return address 복원, caller context 복귀를 수행합니다.

---

## 7. Linux/Runtime/K8s에서 관측 방법

### Linux

```bash
# Call Stack 확인
gdb
bt

# CPU Hotspot 분석 (어떤 함수 보디가 CPU를 많이 사용하는지 분석)
perf top
perf record
perf report

# Instruction 분석 (기계어 레벨 함수 보디 확인)
objdump -d

# Symbol 확인
nm
readelf

# Stack 분석
pstack
cat /proc/<PID>/stack

# Cache/Branch 분석 (branch-misses, i-cache misses)
perf stat
```

### Flame Graph

주요 분석 대상: deep call chain, recursive execution, hotspot function

### Kubernetes

함수 보디 hotspot은 pod CPU spike, latency increase, timeout, autoscaling trigger의 원인이 될 수 있습니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*