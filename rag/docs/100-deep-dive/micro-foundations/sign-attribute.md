# 부호 속성 (Sign Attribute)

> 정독: 0회

## 1. 이 기술이 무엇인가

부호 속성(Sign Attribute)은:

> 정수형 데이터가 음수를 표현할 수 있는지 여부를 정의하는 타입의 속성

일반적으로 두 가지 형태가 존재합니다.

- `signed`
- `unsigned`

부호 속성은 단순한 문법이 아니라 다음 방식을 결정합니다.

- 비트 패턴 생성
- 비트 패턴 해석
- 산술 연산
- 비교 연산
- 범위 계산

동일한 비트 폭이라도 `signed`인지 `unsigned`인지에 따라 표현 가능한 값 범위가 달라집니다.

---

## 2. 시스템 어디에서 등장하는가

부호 속성은 거의 모든 컴퓨터 시스템에서 등장합니다.

**CPU 정수 연산**
- ADD, SUB, MUL, DIV

**메모리 데이터 표현**
- Integer, Counter, Index, Flag

**운영체제 커널**
- Process ID, Page Table Field, Device Register

**네트워크 프로토콜**
- Header Field, Length Field, Sequence Number

**하드웨어 레지스터**
- Control Register, Status Register, Configuration Register

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **CPU**입니다. CPU는 값을 읽을 때 부호 확장(Sign Extension) 또는 제로 확장(Zero Extension)을 수행합니다. 또한 비교, 덧셈, 뺄셈, 시프트의 결과도 부호 속성에 따라 달라집니다.

그 다음은 **Memory**입니다. 같은 비트 패턴이라도 해석 방식이 달라집니다.

---

## 4. 왜 중요한가

부호 속성은 **비트 패턴의 의미**를 결정합니다.

예를 들어 `11111111`이라는 8비트 패턴이 있다고 가정하면:

| 해석 방식 | 결과 |
|---|---|
| unsigned | 255 |
| signed | -1 |

물리적 비트는 동일하지만 의미는 완전히 다릅니다.

즉, **부호 속성 = 비트 패턴 해석 규칙**이라고 볼 수 있습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 값 오버플로우

```
unsigned 8bit

255 + 1 = 0
```

### 음수 해석 오류

비트 패턴 `11111111`을 한 시스템은 `255`로, 다른 시스템은 `-1`로 해석하면 데이터 오류 및 프로토콜 오류가 발생할 수 있습니다.

### 비트 필드 버그

특히 비트 필드에서 자주 발생합니다.

```
signed : 1
```

| 개발자 기대 | 실제 결과 |
|---|---|
| 0, 1 | 0, -1 |

### 장치 제어 오류

하드웨어 레지스터는 대부분 `unsigned` 값을 기대합니다. 잘못된 `signed` 사용 시 잘못된 제어 값으로 해석될 수 있습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

부호 속성은 **비트 패턴 생성**과 **비트 패턴 해석** 모두에 관여합니다.

**unsigned** — 모든 비트를 수치 표현에 사용

```
4bit: 1111  →  15
```

**signed** — 최상위 비트를 부호로 사용 (2의 보수 기준)

```
4bit: 1111  →  -1
```

따라서 **Bit Width + Sign Attribute**가 함께 있어야 표현 범위가 결정됩니다.

| 타입 | 표현 범위 |
|---|---|
| 4bit unsigned | 0 ~ 15 |
| 4bit signed | -8 ~ 7 |

비트 필드 흐름에서의 연결 순서는 다음과 같습니다.

```
Bit Width
↓
Sign Attribute
↓
Sign Encoding
↓
Bit Pattern
↓
Storage
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux Kernel

커널 구조체 내부 `flags`, `state`, `counter` 필드

### Device Driver

장치 레지스터 정의

### 컴파일러 출력 분석

```bash
gcc
clang    # 생성 코드 확인
```

### 디버거 / 메모리 덤프

```bash
gdb
hexdump
xxd
```

### Kubernetes

Kubernetes보다는 Kernel, Compiler, Driver, Hardware Interface 영역에서 훨씬 중요합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*