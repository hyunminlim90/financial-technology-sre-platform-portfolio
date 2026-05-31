# 언어 및 기계 독립성 (Language and Machine Independence)

> 정독: 0회

## 1. 이 기술이 무엇인가

언어 및 기계 독립성은:

> 컴파일러 내부에서 코드가 특정 프로그래밍 언어나 특정 CPU에 종속되지 않는 상태를 의미

원래 어떤 언어로 작성되었는지, 어떤 CPU에서 실행될 것인지를 잠시 잊어버리고, **프로그램의 순수한 논리, 제어 흐름, 데이터 흐름**만 다루는 단계입니다.

현대 컴파일러에서는 주로 **IR(Intermediate Representation)** 단계에서 구현됩니다.

---

## 2. 시스템 어디에서 등장하는가

```
Source Code
↓
Front-end
↓
IR
↓
★ Machine-Independent Optimization
↓
Optimized IR
↓
Back-end
↓
Machine Code
```

현재 문맥에서는 **Front-end → IR → Optimizer** 사이에서 가장 중요하게 등장합니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

직접적으로는 **CPU**와 **Memory**에 가장 영향이 있습니다.

| 자원 | 역할 |
|------|------|
| CPU | Data Flow Analysis, Control Flow Analysis, Dead Code Elimination, Loop Optimization 등 수행 |
| Memory | AST, IR Node, CFG, SSA, Symbol Table 등 저장 |

> Network 영향은 거의 없으며, Disk는 목적 파일 생성 전까지는 대부분 메모리 내부에서 동작합니다.

---

## 4. 왜 중요한가

현대 컴파일러가 존재할 수 있는 이유 중 하나입니다. 언어 및 기계 독립성이 없다면 언어마다, CPU마다 별도 컴파일러가 필요합니다.

| 구조 | 필요한 변환기 수 |
|------|----------------|
| 독립성 없음 (5개 언어 × 5개 CPU) | 25개 |
| 독립성 있음 (5개 언어 → IR, IR → 5개 CPU) | 10개 |

결과적으로 **재사용성, 확장성, 이식성**이 크게 향상됩니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

런타임 장애와 직접 연결되지는 않지만, 컴파일러 품질과 매우 밀접합니다.

| 문제 유형 | 원인 | 결과 |
|-----------|------|------|
| 최적화 버그 | 잘못된 데이터 흐름 분석 | 잘못된 기계어 생성 |
| 플랫폼 포팅 실패 | 독립성이 깨진 코드 | x86 정상 / ARM 오류 |
| 최적화 미적용 | 최적화 패스 누락 | CPU 사용량 증가, 실행 성능 저하, 실행 파일 크기 증가 |

---

## 6. 핵심 메커니즘

> **핵심 사실:** IR은 언어도 모르고 CPU도 모른다.

서로 다른 언어로 작성된 동일한 로직이 Front-end를 통과하면 동일한 IR이 됩니다.

```c
// C
a = b + c;

// Rust / Go 등 동일한 로직
a = b + c;
```

두 코드 모두 아래와 같은 IR로 변환됩니다.

```
t1 = load b
t2 = load c
t3 = add t1, t2
store t3 -> a
```

이 시점에서 C인지, Rust인지, Go인지 알 필요가 없습니다. `x86`, `ARM`, `RISC-V`도 아직 모릅니다.

Optimizer는 다음만 분석합니다.

- 이 계산이 중복인가?
- 이 루프가 비효율적인가?
- 이 값이 사용되는가?

```
Language Independence
+
Machine Independence
=
IR 기반 최적화 가능
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

실행 중에는 존재하지 않으며, 컴파일 과정에서만 존재합니다.

### Linux

```bash
# LLVM IR 출력
clang -S -emit-llvm file.c
# 결과: file.ll (LLVM IR 텍스트 형식)

# GCC IR 덤프
gcc -fdump-tree-all file.c
```

### Kubernetes

CI Build, Container Build, Image Build 과정에서만 나타납니다. 실행 중인 Pod에서는 이미 사라진 단계입니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*