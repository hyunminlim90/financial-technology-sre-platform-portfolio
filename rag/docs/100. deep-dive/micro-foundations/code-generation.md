# 코드 생성 (Code Generation)

> 정독: 0회

## 1. 이 기술이 무엇인가

코드 생성(Code Generation)은:

> 컴파일러가 분석[[과 최적화를 끝낸 중간 표현(IR)을 실제 CPU가 실행할 수 있는 기계어(Machine Code)로 변환하는 단계

```
Source Code
↓
Parsing
↓
Semantic Analysis
↓
Intermediate Representation (IR)
↓
Optimization
↓
★ Code Generation
↓
Object File
```

> 즉, **추상적인 프로그램이 실제 CPU 명령어로 바뀌는 최초의 단계**입니다.

<details>
<summary>Deep Dive</summary></br>

Compiler(컴파일러) [[M]](../../100-deep-dive/micro-foundations/compiler.md)  
Syntactic and Semantic Analysis(구문 및 의미 분석) [[M]](../../100-deep-dive/micro-foundations/syntactic-and-semantic-analysis.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

빌드 파이프라인 내부에서 등장합니다.

```
소스코드
↓
컴파일러 프론트엔드
↓
IR
↓
최적화
↓
★ 코드 생성
↓
목적 파일
↓
링킹
↓
실행 파일
```

코드 생성이 끝나면 CPU 명령어, 구조체 레이아웃, 데이터 배치가 결정됩니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

주요 영향 자원은 **CPU**와 **Memory**입니다.

| 자원 | 역할 |
|------|------|
| CPU | `ADD`, `SUB`, `MUL`, `MOV`, `LOAD`, `STORE` 등 실제 CPU 명령어 생성 |
| Memory | Alignment, Padding, Offset 등 구조체 배치 결정 |

> Disk와 Network는 직접 영향이 적습니다.

---

## 4. 왜 중요한가

같은 프로그램이라도 **코드 생성 품질**에 따라 CPU 사용량, 메모리 사용량, 실행 속도, 바이너리 크기가 크게 달라집니다.

같은 알고리즘이라도 컴파일러에 따라 생성 명령어 수가 달라질 수 있습니다.

또한 `x86`, `ARM`, `RISC-V`는 서로 명령어가 다르므로, 코드 생성 단계가 없으면 CPU가 프로그램을 이해할 수 없습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

| 장애 유형 | 원인 | 결과 |
|-----------|------|------|
| ABI 불일치 | 라이브러리와 프로그램이 서로 다른 ABI 사용 | Crash, 메모리 오염, Segmentation Fault |
| 잘못된 구조체 레이아웃 | Alignment·Padding 차이 | 데이터 깨짐, 프로토콜 오류 |
| 잘못된 CPU 타깃 | ARM용으로 생성 후 x86에서 실행 시도 | 실행 불가, `Illegal Instruction` |
| 최적화 관련 문제 | 특정 최적화 옵션 적용 | Race Condition, Undefined Behavior 노출 |

---

## 6. 핵심 메커니즘

코드 생성 단계에서 중요한 것은 3가지입니다.

### ① Instruction Selection

추상 연산을 실제 CPU 명령어로 변환합니다.

```
a + b  →  ADD
```

### ② Register Allocation

변수를 CPU 레지스터에 배치합니다. CPU는 레지스터 → 캐시 → RAM 순으로 빠르므로, **어떤 값을 레지스터에 둘 것인가**를 결정합니다.

### ③ Data Layout Generation

현재 문맥에서 가장 중요한 단계입니다. 구조체에 대해 크기, 오프셋, 정렬, 패딩을 결정합니다.

```c
struct Data {
    char a;
    int b;
};
```

코드 생성 단계에서의 실제 배치:

```
a       → offset 0
padding → 3 bytes
b       → offset 4
```

비트 필드라면 비트 위치, 비트 폭, 패딩 비트까지 결정됩니다.

> **핵심 사실:** 코드 생성 단계는 **ABI 규칙을 실제 데이터 배치로 구체화**하는 단계다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 어셈블리 확인
gcc -S main.c
# 결과: main.s

# 목적 파일 생성
gcc -c main.c
# 결과: main.o

# 기계어 확인
objdump -d main.o

# 심볼 확인
nm main.o

# 구조체 레이아웃 확인
pahole binary
readelf -a binary
```

### Kubernetes

코드 생성은 직접 보이지 않습니다. 하지만 Pod에서 실행되는 모든 프로세스는 이미 코드 생성이 끝난 결과물입니다.

```
Container Image
↓
실행 파일
↓
코드 생성 결과
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*