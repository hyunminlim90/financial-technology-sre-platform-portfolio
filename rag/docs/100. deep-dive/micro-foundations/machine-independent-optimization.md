# 기계 독립적 최적화 (Machine-Independent Optimization)

> 정독: 0회

## 1. 이 기술이 무엇인가

기계 독립적 최적화는:

> 컴파일러가 중간 표현(IR)을 대상으로 수행하는 최적화 단계

핵심 특징은 CPU 종류, 운영체제, 레지스터 개수, ABI를 모르는 상태에서 **오직 프로그램의 논리만 분석**한다는 점입니다.

즉, **"무엇을 계산하는가"** 를 최적화하는 단계이지,  

**"어떤 CPU에서 실행되는가"** 를 최적화하는 단계가 아닙니다.

<details>
<summary>Deep Dive</summary></br>

Compiler(컴파일러) [[M]](../../100-deep-dive/micro-foundations/compiler.md)  
Intermediate Representation(중간 표현) [[M]](../../100-deep-dive/micro-foundations/intermediate-representation.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

```
Source Code
↓
Compiler Front-end
↓
Intermediate Representation (IR)
↓
★ Machine-Independent Optimization
↓
Optimized IR
↓
Code Generation
↓
Object File
```

컴파일러 구조상 위치:

```
Compiler
├─ Front-end
├─ Optimizer      ← 현재 위치
└─ Back-end
```

---

## 3. 어떤 자원에 가장 영향이 큰가

영향이 가장 큰 자원은 **CPU**와 **Memory**입니다.

| 자원 | 최적화 효과 |
|------|------------|
| CPU | 불필요한 연산 제거, 중복 계산 제거, 반복문 최적화로 사용량 감소 |
| Memory | 불필요한 변수·데이터 제거, 메모리 접근 횟수 감소 |
| Disk | 실행 파일 크기 감소 (간접 효과) |

> Network 영향은 거의 없습니다.

---

## 4. 왜 중요한가

최적화가 없다면 프로그램은 더 많은 연산, 더 많은 메모리 접근, 더 긴 실행 시간, 더 큰 바이너리를 가지게 됩니다.

예시: 동일한 계산을 세 번 반복하는 경우

```
a + b
a + b
a + b
↓
temp = a + b  (한 번만 계산)
```

결과: CPU 감소, 메모리 접근 감소, 실행 시간 감소

---

## 5. 실제 장애와 어떤 관련이 있는가

| 문제 유형 | 원인 | 결과 |
|-----------|------|------|
| CPU 과다 사용 | 최적화되지 않은 반복 계산 | CPU Saturation |
| 메모리 사용 증가 | 사용되지 않는 데이터 유지 | Memory Pressure |
| 실행 시간 증가 | 중복 연산 과다 | Latency 증가 |
| 바이너리 비대화 | 불필요한 코드 유지 | Instruction Cache 효율 저하 |

---

## 6. 핵심 메커니즘

핵심은 **IR을 더 좋은 IR로 바꾼다**는 것입니다. 아직 기계어는 생성되지 않습니다.

### ① Dead Code Elimination

사용되지 않는 코드를 제거합니다.

```
x = 10   // 이후 x를 전혀 사용하지 않음 → 제거
```

### ② Common Subexpression Elimination

중복 계산을 제거합니다.

```
(a + b)
(a + b)
↓
temp = (a + b)   // 한 번만 계산
```

### ③ Constant Folding

컴파일 시점에 계산 가능한 식을 미리 계산합니다.

```
10 * 20  →  200
```

### ④ Loop Optimization

반복문 내부의 동일 계산을 루프 밖으로 이동시킵니다.

### ⑤ Control Flow Optimization

불필요한 분기를 제거합니다.

```
if (true) { ... }  →  조건문 제거 가능
```

전체 흐름:

```
Source Logic
↓
IR
↓
더 효율적인 IR
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

최적화는 Compile Time에 끝나므로 직접 관측되지는 않습니다. 간접적으로 결과를 확인할 수 있습니다.

### Linux

```bash
# 실행 시간 비교
time ./program

# 바이너리 크기 확인
size binary

# 최적화 옵션 비교
gcc -O0 main.c   # 최적화 없음
gcc -O1 main.c
gcc -O2 main.c
gcc -O3 main.c   # 최대 최적화

# LLVM 계열
clang -O2 main.c
```

### Kubernetes

CI Build, Container Build, Image Build 과정에서만 수행됩니다. 실행 중인 Pod에서는 이미 완료된 단계입니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*