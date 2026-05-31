# 컴파일러 (Compiler)

> 정독: 0회

## 1. 이 기술이 무엇인가

컴파일러(Compiler)는:

> 사람이 작성한 고수준 소스코드를 CPU가 실행할 수 있는 기계어로 변환하는 시스템 소프트웨어

```
소스코드
↓
컴파일러
↓
목적 파일 (Object File)
```

컴파일러는 단순 번역기가 아니라, 다음을 수행하는 빌드 시스템의 핵심 엔진입니다.

- 문법 검사
- 의미 분석
- 최적화
- 기계어 생성
- 데이터 레이아웃 결정

---

## 2. 시스템 어디에서 등장하는가

빌드 파이프라인 초반부에 등장합니다.

```
Source Code
↓
Compiler
↓
Object File
↓
Linker
↓
Executable File
↓
Loader
↓
Runtime
```

Source Program → Compilation → Code Generation → Object File 전 과정을 담당하는 주체가 컴파일러입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

컴파일 시점에는 **CPU**, **Memory**, **Disk** 영향이 큽니다.

| 자원 | 역할 |
|------|------|
| CPU | 구문 분석, 최적화, 기계어 생성 수행. 대형 프로젝트일수록 사용량 급증 |
| Memory | AST, IR, 심볼 테이블, 최적화 데이터 유지. 대형 코드베이스에서는 수 GB 이상 사용 가능 |
| Disk | `.o`, `.obj`, `.a`, `.so`, `.exe`, ELF 등 결과 파일 생성 |

> Network 영향은 거의 없습니다.

---

## 4. 왜 중요한가

컴파일러는 프로그램 성능, 메모리 사용량, 실행 파일 크기를 결정합니다.

같은 소스코드라도 **컴파일러 종류**, **컴파일 옵션**, **타깃 플랫폼**에 따라 결과가 달라집니다.

- `GCC`, `Clang`, `MSVC`는 동일 코드에서도 다른 기계어를 생성할 수 있습니다.
- `x86`, `ARM`, `RISC-V`용 실행 파일은 서로 다르며, 컴파일러가 해당 CPU에 맞게 생성해야 합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

| 장애 유형 | 원인 | 결과 |
|-----------|------|------|
| 컴파일 실패 | Syntax Error, Type Error, Undefined Identifier | 빌드 중단 |
| ABI 불일치 | 라이브러리와 ABI 다름 | Crash, Segmentation Fault, 데이터 손상 |
| 잘못된 최적화 | Undefined Behavior 존재 시 `-O2`/`-O3` 적용 | 이상 동작 발생 |
| 잘못된 타깃 설정 | ARM용으로 생성 후 x86 서버에서 실행 시도 | `Illegal Instruction`, 실행 실패 |

---

## 6. 핵심 메커니즘

컴파일러는 3개 계층으로 이해할 수 있습니다.

### ① Frontend

소스코드를 이해합니다.

- 역할: 문법 분석, 의미 분석, 오류 검출
- 결과: IR 생성

### ② Optimizer

IR을 최적화합니다.

- 불필요한 연산 제거
- 중복 제거
- 루프 최적화

### ③ Backend

현재 문맥에서 가장 중요한 계층입니다.

- 역할: 기계어 생성, 레지스터 할당, ABI 적용

이 단계에서 다음이 결정됩니다.

- Alignment
- Padding
- Structure Layout
- Bit-field Layout

> **핵심 사실:** 컴파일러는 소스코드를 기계어로 바꾸는 것뿐 아니라,
> **ABI 규칙에 맞는 실제 데이터 배치까지 결정**한다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 컴파일러 확인
gcc --version
clang --version

# 목적 파일 생성
gcc -c main.c

# 어셈블리 생성
gcc -S main.c

# 목적 파일 기계어 확인
objdump -d main.o

# 심볼 확인
nm main.o

# ELF 확인
readelf -a app
```

### Kubernetes

컴파일러는 직접 보이지 않습니다. 하지만 컨테이너 내부 프로세스는 결국 컴파일러가 생성한 바이너리를 실행합니다.

```
Container Image
↓
Executable Binary
↓
Compiler Output
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*