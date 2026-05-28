# 메서드 (Method)

> 정독: 0회

## 1. 이 기술이 무엇인가

메서드는:

> **객체와 결합된 실행 가능한 코드 블록**

**핵심: 객체의 상태(state)를 읽거나 변경하기 위한 실행 단위**

메서드는 일반적으로 입력(Parameter), 실행 코드, 반환값(Return Value)으로 구성됩니다.

**대표 역할:**

| 역할 | 설명 |
|------|------|
| 데이터 처리 | 객체 상태 변경 |
| 기능 실행 | 연산 수행 |
| 캡슐화 | 데이터 + 동작 결합 |
| 인터페이스 제공 | 외부 호출 진입점 |

<details>
<summary>Deep Dive</summary></br>

Instance(인스턴스) [[M]](../../100-deep-dive/micro-foundations/instance.md)  
Binding(바인딩) [[M]](../../100-deep-dive/micro-foundations/binding.md)  
Function Body(함수 보디) [[M]](../../100-deep-dive/micro-foundations/function-body.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

메서드는 거의 모든 애플리케이션 실행 흐름에서 등장합니다.

| 영역 | 역할 |
|------|------|
| Code Segment(Text Segment) | 메서드 기계어 저장 |
| Stack | 호출 프레임 저장 |
| Heap | 객체 상태 저장 |
| Runtime Engine | method dispatch |
| CPU Pipeline | instruction execution |

메서드는 **객체 상태와 CPU 실행 흐름을 연결하는 핵심 단위**입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향 |
|------|------|
| CPU | instruction execution |
| Stack Memory | call frame 생성 |
| Cache | instruction/data locality |
| Branch Predictor | indirect call prediction |
| Memory | parameter/object access |

특히 메서드 호출 깊이, virtual dispatch, recursion, dynamic binding은 CPU 성능에 직접 영향을 줍니다.

---

## 4. 왜 중요한가

현대 소프트웨어는 대부분 **메서드 호출의 연속**으로 실행됩니다. 프로그램 실행 흐름 자체가 method call → return → stack frame transition의 반복입니다.

또한 캡슐화, 다형성, 인터페이스, 동적 디스패치 같은 핵심 구조도 메서드 기반으로 구현됩니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### Stack Overflow

과도한 재귀 호출 시 stack exhaustion, stack overflow가 발생할 수 있습니다.

### Invalid Method Dispatch

잘못된 함수 포인터/vtable 접근 시 crash, illegal instruction, segmentation fault가 발생할 수 있습니다.

### Excessive Dynamic Dispatch

가상 호출이 많으면 branch prediction 실패, indirect jump overhead, instruction cache miss가 증가할 수 있습니다.

### Deadlock

메서드 내부 lock 순서 충돌 시 thread blocking, deadlock이 발생할 수 있습니다.

### High CPU Usage

무한 루프 메서드로 busy loop이 발생할 수 있습니다.

### Cache Locality 저하

메서드와 데이터가 분산되면 instruction cache miss, data cache miss가 증가할 수 있습니다.

---

## 6. 핵심 메커니즘

### 1) Method Call 발생

프로그램이 `object.method()` 형태 호출을 수행합니다.

### 2) Call Instruction 실행

CPU 수준에서 call, jump, branch 명령이 발생합니다.

```
save return address → move PC → execute target code
```

### 3) Stack Frame 생성

메서드 호출 시 새로운 실행 컨텍스트가 생성됩니다.

| 구성 | 설명 |
|------|------|
| local variables | 지역 변수 |
| parameters | 인자 |
| return address | 복귀 주소 |
| saved registers | 저장 레지스터 |

### 4) Program Counter 변경

CPU의 PC(Register)가 현재 메서드 주소로 이동하고, 이후 instruction fetch를 수행합니다.

### 5) Dynamic Dispatch

다형성 환경에서는 실제 메서드가 런타임에 결정됩니다. 핵심 메커니즘으로 vtable, function pointer, interface dispatch가 있습니다.

### 6) Object Access 수행

메서드 내부에서 object field read, object field write가 발생합니다. 즉 메서드 실행은 결국 **memory access + instruction execution** 조합입니다.

### 7) Return

메서드 종료 시 stack frame 제거, return address 복원, caller context 복귀를 수행합니다.

---

## 7. Linux/Runtime/K8s에서 관측 방법

### Linux

```bash
# Call Stack 확인
gdb
bt

# CPU Hotspot 분석 (어떤 메서드가 CPU를 가장 많이 사용하는지 확인)
perf top
perf record
perf report

# Stack Usage 관측
ulimit -s

# Process Stack Mapping
cat /proc/<PID>/maps

# Thread Stack 확인
cat /proc/<PID>/task/<TID>/maps
```

### Flame Graph 분석

주요 분석 대상: method hotspot, deep call chain, recursive call, lock contention

### Kubernetes

```bash
kubectl top pod
```

CPU spike, latency increase와 특정 메서드 hotspot을 연관하여 분석합니다.

### Sampling Profiler

주요 관측 항목: call frequency, stack depth, dispatch overhead, instruction hotspot