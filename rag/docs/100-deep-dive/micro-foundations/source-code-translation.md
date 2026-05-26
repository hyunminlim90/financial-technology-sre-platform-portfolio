# 소스 코드 번역 (Source Code Translation)

> 정독: 0회

## 1. 이 기술이 무엇인가

소스 코드 번역(Source Code Translation)은:

> 사람이 작성한 고수준 코드, 컴파일러 내부 분석, 중간 표현(IR), 목적 코드(Object Code), 실행 파일(Binary)로 이어지는 전체 변환 공정

출력 형태는 크게 두 가지입니다:

| 출력 | 의미 |
|------|------|
| native machine code | CPU 직접 실행 |
| virtual machine bytecode | VM/JIT 기반 실행 |

> **핵심 목적:** convert abstract logic into executable computational instructions

---

## 2. 시스템 어디에서 등장하는가

소스 코드 번역은 거의 모든 소프트웨어 시스템의 시작점입니다.

| 영역 | 역할 |
|------|------|
| compiler toolchain | code translation |
| build systems | artifact generation |
| CI/CD pipelines | automated compilation |
| operating systems | executable preparation |
| VM runtimes | bytecode production |
| container images | packaged binaries |

**현대 시스템 흐름:**

```
source code
→ translation
→ executable artifact
→ process creation
→ runtime execution
```

> **즉:** 소스 코드 번역은 실행 시스템 전체의 출발점입니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**가장 영향 큰 자원: CPU + Memory**

이유: 컴파일러는 내부적으로 다음과 같은 복잡한 계산을 수행하기 때문입니다.

- AST 생성
- IR 생성
- 최적화
- 데이터 흐름 분석
- 레지스터 할당

대규모 코드베이스에서는:

| 자원 | 영향 |
|------|------|
| CPU | parsing/optimization |
| Memory | AST/IR graph storage |
| Disk | object/binary output |
| Network | distributed build cache |

> **특히:** optimization phases are often CPU-intensive workloads

---

## 4. 왜 중요한가

소스 코드 번역은 **the transformation boundary between software intent and executable computation**입니다.

**중요한 이유:**

| 이유 | 설명 |
|------|------|
| correctness | 의미 보존 |
| performance | 최적화 수행 |
| portability | 다양한 아키텍처 대응 |
| safety | 타입/문법 검증 |
| deployment | 실행 파일 생성 |

컴파일 품질은 다음에 직접 영향을 줍니다:
- 실행 속도
- 메모리 효율
- 바이너리 크기
- 런타임 안정성

---

## 5. 실제 장애와 어떤 관련이 있는가

컴파일 단계 문제는 운영 장애로 직결됩니다.

**대표 장애:**

| 장애 | 원인 |
|------|------|
| build failure | syntax/type error |
| invalid binary | broken translation |
| ABI mismatch | incompatible linkage |
| runtime crash | undefined behavior |
| optimization bug | incorrect codegen |
| architecture incompatibility | wrong target |

**실무에서 자주 발생:**

| 문제 | 결과 |
|------|------|
| stale build artifacts | deployment inconsistency |
| incorrect optimization | intermittent crashes |
| symbol mismatch | loader failure |
| incompatible instruction set | illegal instruction |
| corrupted object files | linker failure |

> **CI/CD 관점에서는:** source translation failures block the entire deployment pipeline

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Source → AST

```
source code
→ lexical analysis
→ parsing
→ abstract syntax tree
```

AST는 다음을 구조화합니다:
- 코드 구조
- 연산 관계
- 제어 흐름

---

### Semantic Analysis

문법 이후: **semantic validation ensures logical correctness**

| 항목 | 의미 |
|------|------|
| type safety | 타입 일치 |
| symbol resolution | 변수/함수 연결 |
| scope validation | 유효 범위 |
| declaration rules | 선언 무결성 |

---

### IR Construction

컴파일러는 **transform high-level syntax into architecture-independent intermediate representation** 합니다.

**IR 목적:**

| 목적 | 설명 |
|------|------|
| optimization | 최적화 |
| portability | 플랫폼 독립 |
| analysis | 데이터 흐름 추적 |
| code generation | 기계어 생성 준비 |

**대표 IR 형태:**

| 형태 | 의미 |
|------|------|
| SSA | static single assignment |
| CFG | control flow graph |
| DAG | dependency graph |

---

### Optimization Pipeline

컴파일러는 IR 위에서 최적화를 수행합니다.

| 최적화 | 설명 |
|--------|------|
| dead code elimination | 불필요 코드 제거 |
| constant folding | 상수 계산 |
| inlining | 함수 병합 |
| loop optimization | 반복 최적화 |
| register allocation | 레지스터 효율화 |

> **핵심 목표:** maximize execution efficiency before runtime

---

### Code Generation

최종 단계:

```
IR
→ architecture-specific instruction generation
```

x86, ARM, RISC-V 같은 실제 ISA에 맞춰 기계어를 생성합니다.

| 결과 | 의미 |
|------|------|
| object code | 목적 파일 |
| executable binary | 실행 파일 |
| shared library | 동적 라이브러리 |
| bytecode | VM instruction stream |

---

### Linking

컴파일만으로 끝나지 않습니다. 링커(Linker)가 **resolve symbols and combine binary modules** 합니다.

| 역할 | 설명 |
|------|------|
| symbol resolution | 함수 연결 |
| relocation | 주소 보정 |
| library binding | 라이브러리 연결 |
| executable assembly | 최종 바이너리 생성 |

---

### Static vs Dynamic Translation

**Static Translation:** translation completed before execution

특징:
- 빠른 실행
- 실행 전 검증 완료
- 플랫폼 종속 가능성

**Dynamic Translation:** translation occurs during runtime

예:
- JIT compilation
- dynamic optimization
- runtime specialization

---

### Translation Boundary

> **소스 코드 번역은 the boundary where abstract logic becomes executable representation 입니다.**

| 이전 | 이후 |
|------|------|
| human-readable logic | executable structure |
| abstract syntax | machine instructions |
| source semantics | runtime artifacts |

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**대표 도구:**

| 도구 | 의미 |
|------|------|
| `gcc` / `clang` | compilation |
| `ld` | linker |
| `objdump` | disassembly |
| `readelf` | ELF metadata |
| `nm` | symbol inspection |
| `strings` | string extraction |

**빌드 산출물:**
- `*.o`
- `*.a`
- `*.so`
- `*.out`

---

### Runtime

| 요소 | 의미 |
|------|------|
| generated binaries | executable image |
| code cache | JIT output |
| relocation | address binding |
| symbol loading | runtime linking |

프로세스 메모리 매핑 확인:
```bash
cat /proc/<pid>/maps
```

---

### Kubernetes

> translated binaries become container runtime artifacts

**K8s 흐름:**

```
source
→ build
→ binary
→ container image
→ pod runtime
```

**실무 장애:**

| 문제 | 결과 |
|------|------|
| broken build | image generation failure |
| wrong architecture | pod crash |
| incompatible libraries | startup failure |
| corrupted binary | CrashLoopBackOff |

**대표 에러:**
- `exec format error`
- `undefined symbol`
- `segmentation fault`
- `illegal instruction`

---

### Observability

| 관측 항목 | 의미 |
|----------|------|
| compile latency | build performance |
| binary size | deployment cost |
| optimization level | runtime performance |
| cache hit ratio | distributed builds |
| symbol resolution | linkage integrity |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*