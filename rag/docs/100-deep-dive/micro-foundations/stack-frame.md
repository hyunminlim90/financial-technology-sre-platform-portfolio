# 스택 프레임 (Stack Frame)

> 정독: 0회

## 1. 이 기술이 무엇인가

스택 프레임은:

> 함수 호출 시 생성되는 함수 전용 실행 메모리 영역

프로그램이 함수 호출을 수행하면 매개변수 / 지역 변수 / 반환 주소 / 레지스터 상태 등을 저장하기 위한 공간이 필요합니다.  

이때 스택(Stack) 메모리 위에 생성되는 실행 단위가 스택 프레임입니다.

| 특징 | 의미 |
|------|------|
| 함수 단위 생성 | 호출마다 독립 생성 |
| LIFO 구조 | 마지막 호출이 먼저 종료 |
| 임시 메모리 | 함수 종료 시 제거 |
| 실행 문맥 저장 | 호출 상태 보존 |

> 함수 실행 상태를 유지하는 런타임 메모리 컨텍스트

Subroutine(서브루틴) [[M]](../../100-deep-dive/micro-foundations/subroutine.md)  
Procedure Call(프로시저 호출) [[M]](../../100-deep-dive/micro-foundations/procedure-call.md)  

## 2. 시스템 어디에서 등장하는가

스택 프레임은 함수 호출이 존재하는 거의 모든 시스템에서 등장합니다.

| 계층 | 사용 |
|------|------|
| CPU 호출 규약 | call/return |
| 운영체제 | userspace thread stack |
| 런타임 | function invocation |
| 인터프리터 | execution frame |
| 컴파일러 | local variable layout |
| 디버거 | call stack tracing |

프로세스 / 스레드 / 코루틴 / 인터프리터 / VM 모두 내부적으로 스택 프레임을 사용합니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**가장 영향이 큰 자원: CPU + Memory**

| 자원 | 영향 |
|------|------|
| Stack Memory | frame 저장 |
| CPU Register | frame pointer |
| Cache | stack locality |
| Branch Predictor | call/return 흐름 |
| Memory Bus | stack access |

> 함수 호출 비용의 핵심 구성 요소

---

## 4. 왜 중요한가

함수 실행 문맥을 안전하게 분리하기 위해 필요합니다.

스택 프레임이 없다면: 지역 변수 충돌 / 반환 위치 손실 / 재귀 불가능 / 멀티 호출 불가능 상태가 됩니다.

스택 프레임은 **함수 실행 상태의 독립성과 복원성 보장**을 담당하며, 디버깅 / 예외 처리 / 스택 추적 / 컨텍스트 스위칭의 핵심 기반입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

| 문제 | 원인 |
|------|------|
| stack overflow | 재귀 과다 |
| stack corruption | 메모리 overwrite |
| invalid return | return address 손상 |
| segmentation fault | stack boundary 초과 |
| stack smashing | buffer overflow |
| deep recursion crash | frame 누적 |

```
무한 재귀 → stack frame 무한 증가 → stack overflow
지역 버퍼 overflow → return address overwrite → 보안 취약점
```

> stack corruption = control flow corruption

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 함수 호출 시 생성

`CALL` 발생 시 수행되는 순서:

```
반환 주소 저장
→ 이전 frame 저장
→ 새 frame 생성
→ 지역 변수 공간 확보
```

### 스택 구조

스택은 보통 **높은 주소 → 낮은 주소** 방향으로 성장합니다.

| 주소 | 내용 |
|------|------|
| 0x7fff1000 | 이전 frame |
| 0x7fff0fe0 | 현재 frame |
| 0x7fff0fc0 | 다음 frame |

### Frame Pointer

| 레지스터 | 역할 |
|----------|------|
| RBP / EBP | frame 기준점 |
| SP / RSP | stack top |

지역 변수 접근은 `frame_base + offset` 형태입니다.

### Return Address

`CALL` 시 CPU는 다음 실행 주소를 스택에 저장하고, `RET` 수행 시 저장된 return address로 복귀합니다.

### 지역 변수 저장

| offset | 데이터 |
|--------|--------|
| -8 | local A |
| -16 | local B |
| -24 | temp |

stack frame 내부 offset 기반 접근입니다.

### Calling Convention

| 규약 | 특징 |
|------|------|
| cdecl | caller cleanup |
| stdcall | callee cleanup |
| syscall ABI | kernel call 규약 |

인자 전달 / 레지스터 사용 / stack cleanup 규칙을 정의하는 CPU ABI의 핵심입니다.

### 재귀 호출

호출마다 새로운 stack frame이 생성되므로:

```
재귀 깊이 = stack usage 증가
```

### Stack Unwinding

오류 발생 시 frame chain을 역추적하여 복원을 수행합니다.

예: exception handling / panic unwind / crash backtrace

### Tail Call Optimization

일부 컴파일러는 현재 frame을 재사용하여 stack growth를 감소시킵니다. 특히 functional runtime에서 중요합니다.

### Stack vs Heap

| | Stack | Heap |
|--|-------|------|
| 할당 단위 | 함수 단위 | 동적 allocation |
| 해제 | 자동 해제 | 명시/GC 해제 |
| 속도 | 매우 빠름 | 상대적으로 느림 |
| 크기 | 제한 있음 | 큰 영역 가능 |

### Stack Alignment

| alignment | 목적 |
|-----------|------|
| 8-byte | 기본 alignment |
| 16-byte | SIMD 최적화 |

alignment가 깨지면 성능 저하 / ABI violation이 발생할 수 있습니다.

### Stack Canary

return address 보호용 값을 삽입하여, overflow 발생 시 canary corruption을 감지합니다.

### Call Stack

```
main
 └─ A
     └─ B
         └─ C
```

각 함수마다 독립적인 stack frame이 존재합니다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**대표 관측 도구:** `gdb` / `pstack` / `perf` / `addr2line` / `objdump`

```bash
# 프로세스 stack 영역 확인
cat /proc/<pid>/maps   # [stack] 영역 표시
```

### Crash 분석

```bash
gdb core
bt   # backtrace = stack frame chain 기반
```

### Runtime

| 구성요소 | 관련 |
|----------|------|
| function call | frame 생성 |
| exception | unwind |
| recursion | stack growth |
| coroutine | custom stack |

### Thread

각 thread는 독립 stack을 보유합니다.

```
thread 수 증가 = stack memory 증가
```

### Kubernetes

| 현상 | 관련 |
|------|------|
| stack overflow crash | container restart |
| deep recursion | pod OOM 유사 현상 |
| native crash | SIGSEGV |
| backtrace 분석 | crash debugging |

native stack overflow → container crash로 이어질 수 있습니다.

### Observability

| 도구 | 의미 |
|------|------|
| perf callgraph | frame tracing |
| flamegraph | stack aggregation |
| gdb bt | stack trace |
| core dump | frame recovery |

성능 분석의 핵심 기반입니다.