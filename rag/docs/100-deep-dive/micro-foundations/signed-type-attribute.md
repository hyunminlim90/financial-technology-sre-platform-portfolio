# 부호 있는 타입 속성 (Signed Type Attribute)

> 정독: 0회

## 1. 이 기술이 무엇인가

부호 있는 타입 속성(Signed Type Attribute)은:

> 정수 데이터가 음수, 0, 양수를 모두 표현하도록 지정하는 타입 속성

대표적으로 다음과 같은 타입이 해당됩니다.

- `signed char`
- `signed short`
- `signed int`
- `signed long`

언어에 따라 기본 정수 타입이 `signed`로 동작하기도 합니다.

핵심은 다음 한 문장입니다.

**동일한 비트 패턴을 음수까지 포함한 정수 체계로 해석하도록 지정하는 속성**

---

## 2. 시스템 어디에서 등장하는가

**CPU 정수 연산**
- 덧셈, 뺄셈, 곱셈, 나눗셈, 비교

**메모리 데이터 저장**
- 정수 변수, 구조체 멤버, 배열 원소

**비트 필드**
- `signed bit-field`

**운영체제**
- 프로세스 상태값, 오차값, 카운터, 시간 차이

**컴파일러**
- 부호 확장(Sign Extension), 산술 시프트(Arithmetic Shift) 생성 시 사용

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **CPU**입니다. CPU가 `signed` 값을 다룰 때 부호 해석, 부호 확장, 음수 연산, 비교 연산을 수행해야 하기 때문입니다.

그 다음은 **Memory**입니다. 메모리에 저장된 비트는 동일하지만 해석 방식이 달라집니다.

| 비트 패턴 | unsigned 해석 | signed 해석 |
|---|---|---|
| `11111111` | 255 | -1 |

---

## 4. 왜 중요한가

컴퓨터는 `0`과 `1`만 저장할 수 있습니다. 하지만 실제로는 `-10`, `-5`, `0`, `5`, `10` 같은 음수를 사용해야 합니다. 이를 가능하게 만드는 것이 **Signed Type Attribute**입니다.

또한 정수 연산, 조건 비교, 오버플로우 처리의 의미가 모두 달라집니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 음수 → 양수 오해석

`-1`을 네트워크로 전송했을 때 수신 시스템이 `unsigned`로 해석하면 `4294967295` 같은 값으로 읽힐 수 있습니다.

### 비교 오류

```
signed_value < unsigned_value
```

자동 형 변환 과정에서 예상과 다른 비교 결과가 발생할 수 있습니다.

### 비트 필드 오류

```c
signed int flag : 1;
```

| 개발자 기대 | 실제 결과 |
|---|---|
| 0, 1 | 0, -1 |

### 프로토콜 파싱 오류

```
패킷 헤더 필드를 signed로 읽음
↓
음수 발생
↓
길이 계산 실패
↓
프로토콜 오류
```

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 핵심 1 — 2의 보수 해석

`signed` 타입은 MSB(Most Significant Bit)를 포함한 전체 비트 패턴을 **2의 보수(Two's Complement)** 규칙으로 해석합니다.

| 비트 패턴 | unsigned | signed |
|---|---|---|
| `1111` (4bit) | 15 | -1 |

### 핵심 2 — 표현 범위 축소

N비트 signed의 범위: `-2^(N-1)` ~ `2^(N-1) - 1`

| 비트 폭 | signed 범위 |
|---|---|
| 4bit | -8 ~ 7 |
| 8bit | -128 ~ 127 |
| 16bit | -32768 ~ 32767 |

### 핵심 3 — Sign Extension

CPU는 `signed` 데이터를 더 넓은 레지스터로 읽을 때 **Sign Extension**을 수행합니다.

```
8bit:   11111111
        ↓ 32bit 확장
32bit:  11111111 11111111 11111111 11111111
        ↓
        -1 유지
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux Kernel

커널 구조체 멤버 `state`, `error`, `delta`, `offset` 등

### Device Driver

하드웨어 레지스터 해석

### 컴파일러 출력

```bash
gcc
clang    # 생성 어셈블리에서 sign extension / zero extension 차이 확인 가능
```

### 디버거

```bash
gdb
lldb     # 같은 비트 패턴을 signed / unsigned로 다르게 출력 가능
```

### Kubernetes

Kubernetes 자체보다는 CPU, Compiler, Kernel, Driver, Protocol 영역에서 중요합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*