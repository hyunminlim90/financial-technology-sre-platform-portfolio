# 바인딩 (Binding)

> 정독: 0회

## 1. 이 기술이 무엇인가

바인딩은:

> **프로그램의 추상적인 요소를 실제 실행 대상과 연결하는 과정**

**대표 연결 대상:**

| 추상 요소 | 연결 대상 |
|-----------|-----------|
| 변수 이름 | 메모리 주소 |
| 함수 호출 | 실제 코드 주소 |
| 객체 참조 | 실제 객체 메모리 |
| 인터페이스 메서드 | 실제 구현 메서드 |
| 심볼(Symbol) | 실행 가능한 기계어 위치 |

> **핵심: 추상적 식별자와 실제 실행 실체를 연결하는 런타임 또는 컴파일 시점 결합**

---

## 2. 시스템 어디에서 등장하는가

바인딩은 시스템 전체에서 등장합니다.

| 영역 | 바인딩 대상 |
|------|------------|
| Compiler | symbol binding |
| Linker | external symbol resolution |
| Loader | shared library binding |
| Runtime | dynamic dispatch |
| CPU | function jump target |
| Memory System | address binding |

**대표 흐름:**

```
identifier
→ symbol resolution
→ address mapping
→ execution target selection
```

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향 |
|------|------|
| CPU | branch execution |
| Instruction Cache | indirect jump locality |
| Memory | vtable/object lookup |
| Cache | pointer chasing |
| Runtime Metadata | dispatch overhead |

특히 dynamic dispatch, virtual call, interface lookup, symbol resolution은 CPU pipeline 효율에 직접 영향을 줍니다.

---

## 4. 왜 중요한가

바인딩은 **프로그램의 실제 실행 대상을 결정하는 핵심 메커니즘**입니다.

현대 시스템에서 바인딩은 다형성, 인터페이스, 동적 라이브러리, 플러그인 시스템, 런타임 확장성의 기반입니다.

또한 컴파일 타임 최적화, 런타임 유연성, ABI 호환성, 모듈화에도 핵심 역할을 수행합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### Symbol Resolution Failure

동적 라이브러리 심볼 실패 시 `undefined symbol`, linker error, runtime load failure가 발생할 수 있습니다.

### Invalid Dispatch

잘못된 함수 주소 바인딩 시 crash, segmentation fault, illegal instruction이 발생할 수 있습니다.

### Dynamic Dispatch Overhead

가상 호출이 많으면 branch prediction 실패, indirect branch 증가, CPU frontend stall이 발생할 수 있습니다.

### ABI Mismatch

라이브러리 바인딩 규약 불일치 시 memory corruption, stack corruption, runtime crash가 발생할 수 있습니다.

### Dangling Function Pointer

이미 제거된 코드 주소 호출 시 invalid jump, undefined behavior가 발생할 수 있습니다.

### Plugin/Shared Library Failure

런타임 binding 실패 시 `dlopen failure`, missing dependency 등이 발생할 수 있습니다.

---

## 6. 핵심 메커니즘

### 1) 호출 또는 참조 발생

프로그램이 method call, variable access, symbol lookup을 수행합니다.

### 2) Binding Type 결정

| 유형 | 시점 |
|------|------|
| Static Binding | compile/link time |
| Dynamic Binding | runtime |

### 3) Static Binding

컴파일 시점에 실행 대상 주소가 확정됩니다. 빠르고 최적화에 유리하며 예측 가능합니다.

### 4) Dynamic Binding

런타임에 실제 타입을 확인한 후 실행 코드를 결정합니다. 대표 메커니즘으로 vtable lookup, interface dispatch, function pointer resolution이 있습니다.

### 5) Address Resolution 수행

실행 엔진이 metadata lookup, symbol table lookup, offset calculation 등을 수행합니다.

### 6) Program Counter 변경

최종 함수 주소가 결정되면 CPU의 PC(program counter)가 해당 위치로 이동합니다.

### 7) 실제 코드 실행

이후 stack frame 생성 → instruction fetch → decode → execute가 수행됩니다.

---

## 7. Linux/Runtime/K8s에서 관측 방법

### Linux

```bash
# Shared Library Binding 확인
ldd <binary>

# Symbol Table 확인
nm
objdump
readelf

# Dynamic Linker 확인
ld.so

# Runtime Symbol Lookup 추적
dlopen
dlsym

# Call Graph 분석 (dispatch hotspot)
perf
gprof
flamegraph

# CPU Branch Miss 분석 (branch-misses, indirect branches)
perf stat

# Process Memory Mapping (shared library binding 확인)
cat /proc/<PID>/maps

# Runtime Loader Activity (symbol resolution, dynamic linking 추적)
strace
ltrace
```

### Kubernetes

Pod 내부 애플리케이션에서 shared library mismatch, runtime dependency failure, ABI incompatibility 등이 startup crash의 원인이 될 수 있습니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*