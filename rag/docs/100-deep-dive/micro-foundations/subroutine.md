# 서브루틴 (Subroutine)

> 정독: 0회

## 1. 이 기술이 무엇인가

서브루틴은:

> 특정 작업을 수행하도록 분리된 실행 가능한 코드 단위

| 용어 | 의미 |
|------|------|
| Function | 값을 반환하는 서브루틴 |
| Procedure | 반환값 없는 서브루틴 |
| Method | 객체에 소속된 서브루틴 |
| Routine | 일반적 표현 |

**핵심 특징:** 독립적 실행 가능 / 입력 인자 사용 가능 / 결과 반환 가능 / 재사용 가능 / 호출 기반 실행

> 프로그램 제어 흐름 내부에서 호출 가능한 실행 단위

---

## 2. 시스템 어디에서 등장하는가

거의 모든 실행 환경에서 등장합니다.

| 계층 | 사용 형태 |
|------|-----------|
| 컴파일러 | function symbol |
| CPU ISA | CALL / RET |
| 운영체제 | syscall wrapper |
| 런타임 | execution frame |
| 인터프리터 | function object |
| VM | method dispatch |
| 커널 | kernel routine |
| 라이브러리 | API function |

실제로 프로그램 대부분은 서브루틴 호출 체인으로 구성됩니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**가장 영향이 큰 자원: CPU + Stack Memory**

| 자원 | 영향 |
|------|------|
| CPU | call/return 수행 |
| Stack Memory | frame 생성 |
| Register | argument/return 저장 |
| Instruction Cache | code locality |
| Branch Predictor | 함수 분기 예측 |

> 서브루틴 호출 비용 = 제어 흐름 전환 비용

---

## 4. 왜 중요한가

복잡한 논리를 독립 실행 단위로 분리하기 위해 필요합니다.

서브루틴이 없으면: 코드 중복 증가 / 유지보수 어려움 / 실행 흐름 혼란 / 테스트 어려움이 발생합니다.

현대 시스템의 거의 모든 추상화의 기반입니다.

| 기술 | 내부 기반 |
|------|-----------|
| API | function call |
| RPC | remote subroutine |
| syscall | kernel routine |
| callback | deferred routine |
| event loop | handler routine |

---

## 5. 실제 장애와 어떤 관련이 있는가

| 문제 | 원인 |
|------|------|
| stack overflow | 과도한 재귀 |
| infinite recursion | 종료 조건 없음 |
| call overhead 증가 | 과도한 함수 분할 |
| ABI mismatch | 호출 규약 불일치 |
| invalid return | stack corruption |
| crash backtrace 손상 | frame 파괴 |

```
재귀 서브루틴 무한 호출 → stack exhaustion
호출 규약 mismatch → register corruption
```

> 잘못된 서브루틴 호출 = 제어 흐름 파괴

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 호출(Call)

호출 시 CPU가 수행하는 순서:

```
현재 실행 위치 저장
→ 서브루틴 주소 이동
→ 새 실행 문맥 시작
```

| ISA | 명령 |
|-----|------|
| x86 | CALL |
| ARM | BL |
| RISC-V | JAL |

### 반환(Return)

서브루틴 종료 시 원래 실행 위치로 복귀합니다.

| ISA | 명령 |
|-----|------|
| x86 | RET |
| ARM | RET |
| RISC-V | JR ra |

### 스택 프레임 생성

호출 시 새 stack frame이 생성됩니다.

| 구성 | 역할 |
|------|------|
| local variable | 지역 변수 |
| return address | 복귀 주소 |
| saved register | 레지스터 저장 |
| argument | 함수 인자 |

> 서브루틴 실행 컨텍스트 저장소

### Calling Convention

| 규약 | 특징 |
|------|------|
| cdecl | caller cleanup |
| stdcall | callee cleanup |
| SysV ABI | Linux x86_64 표준 |

인자 전달 위치 / 반환값 저장 위치 / register ownership / stack cleanup 주체를 정의합니다.

### Parameter Passing

| 방식 | 특징 |
|------|------|
| register | 빠름 |
| stack | 범용 |
| memory reference | 큰 데이터 전달 |

현대 CPU는 가능하면 register를 우선 사용합니다.

### Return Value

| ISA | register |
|-----|----------|
| x86_64 | RAX |
| ARM64 | X0 |

### 재귀(Recursion)

호출마다 새로운 stack frame이 생성됩니다.

```
깊은 recursion = stack 증가
```

### Inline Optimization

컴파일러는 작은 서브루틴을 호출 없이 코드에 직접 삽입할 수 있습니다.

- **효과:** call overhead 제거 / branch 감소 / cache locality 개선
- **단점:** binary size 증가

### Dynamic Dispatch

실행 시점에 실제 서브루틴을 결정합니다.

예: virtual dispatch / interface call / dynamic binding

### Tail Call

마지막 호출이면 현재 frame을 재사용할 수 있어 recursion stack 증가를 방지합니다.

### Reentrant Routine

동시에 여러 실행 흐름이 호출해도 상태 충돌이 없는 서브루틴입니다. 멀티스레드 환경의 핵심 개념입니다.

### Pure Routine

부수효과 없는 서브루틴으로, 입력이 동일하면 출력이 동일하고 외부 상태를 변경하지 않습니다. 최적화에 매우 유리합니다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**대표 도구:** `gdb` / `perf` / `objdump` / `nm` / `readelf`

```bash
# call graph 확인
perf record -g

# 서브루틴 단위 disassembly
objdump -d
```

### Runtime

| 요소 | 관련 |
|------|------|
| function call | routine dispatch |
| coroutine | suspended routine |
| async runtime | callback routine |
| VM | invocation engine |

### Crash 분석

```bash
gdb core
bt   # backtrace = 서브루틴 호출 체인
```

### Kubernetes

| 현상 | 관련 |
|------|------|
| stack overflow | recursive routine |
| CPU spike | hot routine |
| latency 증가 | excessive call depth |
| native crash | invalid routine execution |

특히 profiling에서 중요합니다.

### Observability

| 도구 | 의미 |
|------|------|
| flamegraph | hot subroutine |
| perf callgraph | 호출 트리 |
| eBPF uprobes | 함수 추적 |
| tracing | routine latency |

실무 성능 분석의 핵심입니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*