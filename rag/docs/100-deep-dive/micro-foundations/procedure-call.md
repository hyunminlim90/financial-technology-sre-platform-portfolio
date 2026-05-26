# 프로시저 호출 (Procedure Call)

> 정독: 0회

## 1. 이 기술이 무엇인가

프로시저 호출은:

> 현재 실행 흐름을 중단하고 다른 코드 블록으로 실행 제어권을 전달한 뒤, 작업 완료 후 원래 위치로 복귀하는 실행 제어 메커니즘

| 요소 | 역할 |
|------|------|
| Caller | 호출자 |
| Callee | 피호출자 |
| CALL | 제어권 전달 |
| RET | 원래 위치 복귀 |
| Return Address | 복귀 위치 |
| Stack Frame | 실행 문맥 저장 |

> 프로그램 내부의 안전한 제어 흐름 전환 시스템

---

## 2. 시스템 어디에서 등장하는가

거의 모든 실행 환경에 존재합니다.

| 계층 | 등장 형태 |
|------|-----------|
| CPU ISA | CALL / RET |
| 컴파일러 | function invocation |
| 운영체제 | syscall wrapper |
| 런타임 | method dispatch |
| VM | invocation engine |
| 커널 | kernel routine call |
| 라이브러리 | API function call |

현대 프로그램 대부분은 프로시저 호출 체인으로 실행됩니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**핵심 영향 자원: CPU + Stack Memory**

| 자원 | 영향 |
|------|------|
| CPU | control transfer |
| Stack | frame allocation |
| Register | argument 저장 |
| Cache | instruction locality |
| Branch Predictor | return prediction |

> 호출 빈도가 높을수록 제어 흐름 비용 증가

---

## 4. 왜 중요한가

복잡한 프로그램을 독립 실행 단위로 분리하기 위해 필요합니다.

없다면: 코드 재사용 불가 / 제어 흐름 관리 어려움 / 지역 상태 격리 불가 / 재귀 구현 불가 / 라이브러리 구조 불가능

현대 소프트웨어 실행 구조의 핵심 기반입니다.

| 시스템 | 내부 구조 |
|--------|-----------|
| 웹 서버 | request handler call |
| DBMS | query routine |
| OS | syscall dispatch |
| 컴파일러 | parser routine |
| 런타임 | allocator routine |

---

## 5. 실제 장애와 어떤 관련이 있는가

| 문제 | 원인 |
|------|------|
| stack overflow | 깊은 호출 |
| infinite recursion | 종료 실패 |
| invalid return | stack corruption |
| segmentation fault | 잘못된 return address |
| ABI mismatch | calling convention 충돌 |
| branch misprediction | excessive indirect call |

```
무한 재귀 → stack frame 폭증 → 프로세스 crash
stack corruption → RET 시 잘못된 주소 복귀
```

> return address 손상 = 제어 흐름 파괴

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### CALL 명령

| ISA | 명령 |
|-----|------|
| x86 | CALL |
| ARM | BL |
| RISC-V | JAL |

CPU 수행 순서:

```
반환 주소 저장
→ PC/IP 변경
→ 대상 루틴 이동
```

### Return Address

CALL 직후 다음 실행 위치 주소를 저장합니다. stack / link register / return register를 사용하며, 이 주소가 `RET` 시 복귀 기준점이 됩니다.

### Stack Frame 생성

호출 시 새 실행 문맥이 생성됩니다.

| 구성 | 역할 |
|------|------|
| local variable | 지역 변수 |
| saved register | 보존 레지스터 |
| return address | 복귀 위치 |
| argument | 인자 |
| frame pointer | 이전 프레임 연결 |

### Argument Passing

| 방식 | 특징 |
|------|------|
| register | 빠름 |
| stack | 범용 |
| memory reference | 대용량 데이터 |

현대 ABI는 가능하면 register를 우선 사용합니다.

### Frame Pointer / Stack Pointer

| 레지스터 | 역할 |
|----------|------|
| SP | 현재 stack top |
| FP/BP | 현재 frame 기준점 |

모든 지역 변수 접근은 `FP/SP + offset` 기반입니다.

### Prologue / Epilogue

**Prologue (진입 시):**
```
stack 확보 → register 저장 → frame setup
```

**Epilogue (복귀 시):**
```
stack 복구 → register 복원 → return 수행
```

### RET 명령

```
stack에서 return address pop
→ PC/IP 복원
→ caller 복귀
```

> 호출 이전 실행 흐름 복원

### Calling Convention

| 규약 | 특징 |
|------|------|
| cdecl | caller cleanup |
| stdcall | callee cleanup |
| SysV ABI | Linux x86_64 표준 |

인자 전달 방식 / 반환값 위치 / stack cleanup 주체 / register ownership을 정의하는 호출자·피호출자 간 실행 계약(protocol)입니다.

### Recursive Call

호출마다 새로운 stack frame이 생성됩니다.

```
깊은 recursion = stack 사용량 증가
```

### Indirect Call

호출 대상이 실행 시점에 결정되는 방식입니다.

예: function pointer / virtual dispatch / callback / interface call

CPU branch prediction 난이도가 증가할 수 있습니다.

### Tail Call Optimization

마지막 호출 시 새 frame 생성 없이 기존 frame을 재사용합니다.

효과: stack growth 감소 / recursion 최적화

### Context Preservation

프로시저 호출의 본질:

> 현재 상태 보존 + 제어권 이동 + 안전한 복귀

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**대표 도구:** `gdb` / `perf` / `objdump` / `strace`

```bash
# call graph 생성
perf record -g

# CALL / RET 직접 확인
objdump -d

# 프로시저 호출 체인 확인
gdb
bt
```

### Runtime

| 요소 | 관련 |
|------|------|
| method invoke | procedure call |
| async task | callback call |
| coroutine | suspended routine |
| scheduler | routine dispatch |

### Profiling

| 도구 | 의미 |
|------|------|
| flamegraph | hot call path |
| perf | call overhead |
| eBPF uprobes | 함수 추적 |
| tracing | latency path |

### Kubernetes

| 현상 | 관련 |
|------|------|
| stack overflow | recursive call |
| CPU spike | excessive call |
| latency 증가 | deep call chain |
| native crash | invalid return |

고빈도 함수 호출 → CPU saturation은 실무에서 자주 발생합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*