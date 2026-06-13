# 비트 필드 선언 (Bit Field Declaration)

> 정독: 0회

## 1. 이 기술이 무엇인가

**비트 필드 선언(Bit Field Declaration)** 은:

> 하나의 데이터 컨테이너(Word) 안에서 어떤 필드가 몇 비트를 사용할 것인지 컴파일러에게 명시하는 선언

### 예시

```c
struct Status {
    unsigned int enabled : 1;
    unsigned int mode    : 3;
    unsigned int level   : 4;
};
```

```
enabled  →  1비트
mode     →  3비트
level    →  4비트
```

### 핵심

> 값을 저장하는 행위가 아니라 **비트 레이아웃을 정의하는 행위**입니다.

---

## 2. 시스템 어디에서 등장하는가

비트 필드 선언은 **"정해진 비트 레이아웃을 코드로 표현해야 하는 곳"** 에서 사용됩니다.

### 하드웨어 제어

- `CPU Register`
- `NIC Register`
- `Storage Controller Register`
- `Device Register`

### 운영체제

- `Page Flags`
- `Process State`
- `Filesystem Metadata`

### 네트워크

- `TCP Header`
- `IP Header`
- `Protocol Header`

### 임베디드 시스템

- `MCU Register`
- `Sensor Register`
- `Control Register`

---

## 3. 어떤 자원에 가장 영향이 큰가

### 1순위: Memory

비트 필드 선언의 목적은 **메모리 밀도 증가**입니다.

### 예시: 상태값 8개 저장 비교

| 방식 | 계산 | 필요 공간 |
|------|------|-----------|
| 일반 변수 | 8 × 1 byte | 8 byte |
| 비트 필드 | 8 × 1 bit | 1 byte |

CPU는 이후 Mask, Shift, Load, Store 연산을 수행해야 하므로 CPU 영향도 있지만, 본질적으로는 **메모리 최적화 기술**입니다.

---

## 4. 왜 중요한가

비트 필드 선언은 논리적 데이터 구조와 물리적 비트 구조를 연결합니다.

```
논리적 데이터 구조
        ↓
물리적 비트 구조
```

### 예시

전원 상태, 에러 상태, 우선순위, 모드를 각각 별도 변수로 저장하면 공간 낭비가 발생합니다. 비트 필드 선언은 몇 비트를 사용할지 명확히 정의하여 다음을 개선합니다:

- 메모리 사용량
- 캐시 효율
- 데이터 밀도

또한 **하드웨어 명세**와 **프로토콜 명세**를 코드로 그대로 표현할 수 있습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### ABI 불일치

가장 대표적인 문제입니다.

```
컴파일러 A / 컴파일러 B  →  서로 다른 방식으로 배치
    ↓
비트 위치 불일치  →  데이터 해석 실패
```

### 하드웨어 제어 오류

```
실제:  bit 0 = enable
선언:  bit 1 = enable  (잘못 선언)
    ↓
장치 동작 실패
```

### 프로토콜 오류

패킷 헤더의 비트 구조를 잘못 선언 시:

- Flag, Length, Version 오해석
- 통신 실패
- 데이터 손상

### 구조체 레이아웃 비호환

구조체 레이아웃을 고정이라고 가정했는데 컴파일러 변경 시:

```
비트 배치 변경  →  기존 데이터와 비호환
```

---

## 6. 핵심 메커니즘

### 1단계 — 프로그래머 선언

```c
unsigned int mode : 3;
```

### 2단계 — 컴파일러 수집

```
이름 : mode
폭   : 3 bit
```

### 3단계 — 비트 필드 할당 수행

```
mode
  offset = 1
  width  = 3
```

### 4단계 — 비트 필드 매핑 수행

```
실제 워드의 1~3번 비트에 배치
```

### 5단계 — 코드 생성

```
읽기:  Shift + Mask
쓰기:  Clear + Shift + OR
```

### 핵심 흐름

```
비트 필드 선언
        ↓
비트 필드 할당
        ↓
비트 필드 매핑
        ↓
Mask/Shift 코드 생성
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux Kernel

직접 사용되는 영역:

- `Page Flags`
- `Process Flags`
- `Capability Flags`
- `Filesystem Flags`

### Device Driver

Interrupt Enable, DMA Enable, Reset Bit 등을 선언할 때 사용됩니다.

### Firmware

Control Register, Status Register 매핑에 사용됩니다.

### Network Stack

프로토콜 헤더 정의에서 사용됩니다:

- `IPv4`
- `TCP`
- `UDP`

### Kubernetes

직접 비트 필드 선언을 보지는 않지만 하부 계층에서 광범위하게 사용됩니다:

- `Linux Kernel`
- `Container Runtime`
- `Device Driver`