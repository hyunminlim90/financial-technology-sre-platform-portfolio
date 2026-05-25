# 컴파일 타임 (Compile-time)

> 정독: 0회

## 1. 이 기술이 무엇인가

컴파일 타임은:

> 프로그램이 실제 실행되기 전에 문법 분석, 타입 검증, 최적화, 코드 생성, 메타데이터 생성, 바이너리 생성을 수행하는 단계

### 핵심 특징

| 특징 | 설명 |
|------|------|
| pre-runtime phase | 실행 이전 단계 |
| static analysis | 정적 검증 |
| code transformation | 코드 변환 |
| metadata generation | 실행 정보 생성 |
| optimization preparation | 최적화 기반 구축 |

**핵심 정의:**

> compile-time establishes executable structure before runtime begins

---

## 2. 시스템 어디에서 등장하는가

컴파일 타임은 다음 계층에서 등장합니다.

| 계층 | 역할 |
|------|------|
| parser | 문법 해석 |
| semantic analyzer | 타입/의미 검증 |
| optimizer | 코드 최적화 |
| IR builder | 중간 표현 생성 |
| linker | 심볼 연결 |
| code generator | 실행 코드 생성 |

### 관련 시스템

| 시스템 | 역할 |
|--------|------|
| compiler frontend | source analysis |
| compiler backend | machine/bytecode generation |
| build system | dependency orchestration |
| package system | artifact generation |

> compile-time defines runtime executable behavior ahead of execution

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**가장 영향 큰 자원: CPU + Disk + Build Memory**

컴파일은:

- AST 생성
- 타입 분석
- CFG 구성
- 최적화
- 코드 생성

등 CPU 집약적 작업 수행.

### 자원별 영향

| 자원 | 영향 |
|------|------|
| CPU | parsing/optimization/codegen |
| Memory | AST/IR graph 유지 |
| Disk | object/binary artifact 생성 |
| Network | distributed build/cache 시 영향 |

> 대규모 빌드에서는 compile-time scalability becomes infrastructure concern

---

## 4. 왜 중요한가

컴파일 타임은:

> **runtime predictability foundation**

컴파일 단계에서 미리:

- 오류 제거
- 타입 확정
- 구조 확정
- 최적화 준비

를 수행하기 때문에 런타임 부담 감소.

### 핵심 기능과 효과

| 기능 | 효과 |
|------|------|
| type checking | 안정성 증가 |
| optimization | 성능 향상 |
| dead code elimination | 실행 비용 감소 |
| symbol resolution | 빠른 실행 |
| layout calculation | predictable memory access |

> compile-time shifts complexity away from runtime

---

## 5. 실제 장애와 어떤 관련이 있는가

### 대표 문제

| 장애 | 원인 |
|------|------|
| build failure | syntax/type error |
| ABI mismatch | incompatible binary layout |
| invalid optimization | compiler bug |
| symbol resolution failure | linker issue |
| oversized binaries | poor optimization |
| runtime crash | compile-time assumption mismatch |

> incorrect compile-time assumptions can create runtime instability

잘못된 컴파일 타임 가정의 예:

- wrong alignment
- invalid inlining
- incorrect alias analysis
- unsafe optimization

### 운영 측면 문제

| 문제 | 결과 |
|------|------|
| long compile time | CI bottleneck |
| unstable incremental build | deployment delay |
| non-deterministic builds | reproducibility failure |

**SRE/플랫폼 관점:**

> build determinism and reproducibility are operational reliability issues

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Parsing

컴파일 타임 시작점:

> source text transformed into structured syntax representation

포함:
- tokenization
- syntax tree generation
- grammar validation

### Semantic Analysis

> meaning and type correctness verification

| 항목 | 설명 |
|------|------|
| type checking | 타입 검증 |
| symbol resolution | 이름 연결 |
| scope validation | 범위 검사 |
| constraint checking | 제약 확인 |

### Static Type Resolution

> determine data structure rules before execution

확정되는 것:
- field size
- alignment
- calling convention
- stack layout
- register usage hints

### Intermediate Representation (IR)

> source program transformed into compiler IR

IR은 다음의 기반:
- optimization
- analysis
- code transformation

### Optimization

| 최적화 | 설명 |
|--------|------|
| constant folding | 상수 계산 |
| dead code elimination | 불필요 코드 제거 |
| inlining | 함수 확장 |
| loop optimization | 반복 최적화 |
| vectorization | SIMD 활용 |

> compile-time optimization reduces future runtime cost

### Metadata Generation

| 메타데이터 | 역할 |
|-----------|------|
| type metadata | 타입 정보 |
| symbol table | 이름 연결 |
| debug info | 디버깅 |
| relocation info | 링크 지원 |
| runtime descriptors | 런타임 지원 |

### Binary/Object Generation

> compiler emits executable artifact

출력 대상:
- object files
- bytecode files
- shared libraries
- executable binaries

### Linking

> cross-module symbol integration

포함:
- external symbol resolution
- relocation
- address fixing
- dependency integration

### Ahead-of-Time Knowledge

> use future execution assumptions before execution actually occurs

런타임 전에 확정하는 것:
- 구조
- 타입
- 호출 규칙
- 메모리 배치

### Static vs Dynamic Resolution

| Compile-time | Runtime |
|--------------|---------|
| static certainty | dynamic behavior |
| ahead-of-execution | during execution |
| deterministic analysis | live system state |
| no actual execution | real resource mutation |

### Compile-time and Runtime Boundary

> compile-time defines executable possibility, runtime performs actual execution

- **컴파일 타임**: 설계와 검증
- **런타임**: 실제 물리 자원 사용

### Physical Hardware Impact

대규모 빌드에서 발생하는 것:
- high CPU usage
- large memory graphs
- disk-heavy artifact generation
- parallel scheduling

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**대표 관측 도구:** `perf`, `strace`, `time`, `top`, `vmstat`, `iostat`

| 항목 | 의미 |
|------|------|
| CPU saturation | optimization/codegen |
| memory growth | IR/AST expansion |
| disk writes | artifact generation |
| process fanout | parallel builds |
| syscall volume | filesystem activity |

### Build System / Runtime

| 항목 | 의미 |
|------|------|
| compile latency | build duration |
| incremental rebuild efficiency | 개발 생산성 |
| cache hit ratio | build optimization |
| artifact size | binary efficiency |
| symbol count | metadata 규모 |

> build reproducibility and determinism이 특히 중요

### Kubernetes / CI Infrastructure

| 현상 | 원인 |
|------|------|
| CI bottleneck | long compile time |
| node CPU exhaustion | parallel builds |
| ephemeral storage pressure | artifact explosion |
| cache invalidation storm | dependency churn |

> compile-time scalability becomes platform engineering problem

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*