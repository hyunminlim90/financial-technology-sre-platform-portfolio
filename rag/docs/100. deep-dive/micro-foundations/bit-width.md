# 비트 폭 (Bit Width)

> 정독: 0회

## 1. 이 기술이 무엇인가

**비트 폭(Bit Width)** 은:

> 특정 데이터가 차지하는 비트 수를 의미

### 예시

```
1비트  →  1개의 비트 사용
3비트  →  3개의 비트 사용
8비트  →  8개의 비트 사용
```

비트 필드 문맥에서는:

```c
unsigned mode : 3;
```

여기서 `3`이 바로 **비트 폭(Bit Width)** 입니다.

### 비트 폭이 결정하는 것

1. 얼마나 많은 비트를 사용할 것인가
2. 어떤 범위의 값을 표현할 수 있는가

---

## 2. 시스템 어디에서 등장하는가

비트 폭은 거의 모든 시스템 계층에서 등장합니다.

### CPU 레지스터

- `8bit`
- `16bit`
- `32bit`
- `64bit`
- `128bit`

### 하드웨어 레지스터

- `상태 플래그 : 1bit`
- `모드 값 : 3bit`
- `우선순위 : 4bit`

### 네트워크 프로토콜

- `IPv4 Version : 4bit`
- `TTL : 8bit`
- `Protocol : 8bit`

### 파일 포맷

- Flags, Version, Type, Length 등의 필드 크기 정의

### 운영체제

- `Page Flags`
- `Permission Bits`
- `CPU Status Register`

---

## 3. 어떤 자원에 가장 영향이 큰가

### 1순위: Memory

비트 폭 = **실제 저장 공간 크기**이기 때문입니다.

### 추가 영향

| 자원 | 영향 |
|------|------|
| CPU | Mask 생성, Shift 연산, Range 검사 |
| Network | 패킷 헤더 크기 |
| Disk | 저장 포맷 크기 |

---

## 4. 왜 중요한가

비트 폭은 **표현 가능한 값의 범위**를 결정합니다.

### 비트 폭별 표현 가능 값

| 비트 폭 | 표현 가능한 값 개수 | 범위 (부호 없는 정수) |
|---------|--------------------|-----------------------|
| 1bit | 2개 | 0 ~ 1 |
| 2bit | 4개 | 0 ~ 3 |
| 3bit | 8개 | 0 ~ 7 |
| 4bit | 16개 | 0 ~ 15 |
| 8bit | 256개 | 0 ~ 255 |

부호 없는 정수의 최대값:

```
최대값 = 2^n - 1
```

> 비트 폭은 **"얼마나 큰 값을 저장할 수 있는가"** 를 결정합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 범위 초과 (Overflow)

```
width = 3bit  →  표현 가능: 0 ~ 7

10 을 저장 시
    ↓
상위 비트가 잘려나감  →  잘못된 값 저장
```

### 프로토콜 해석 오류

```
송신:  Priority = 4bit
수신:  Priority = 3bit 로 해석
    ↓
데이터 깨짐
```

### 레지스터 설정 오류

```
하드웨어:  Mode = 2bit 만 허용
소프트웨어: 5 를 기록
    ↓
예상하지 못한 동작 발생
```

---

## 6. 핵심 메커니즘

### 전체 흐름

```
Bit Field Member
    ↓
Bit Width  ← 현재 단계
    ↓
Bit Offset
    ↓
Bit Field Layout
    ↓
Bit Field Encoding
    ↓
Bit Pattern
```

### 예시: `unsigned priority : 3`

**Bit Width = 3** 일 때 표현 가능한 값:

```
000 = 0
001 = 1
010 = 2
011 = 3
100 = 4
101 = 5
110 = 6
111 = 7
```

총 **2³ = 8개** 값을 표현할 수 있습니다.

### 마스크 생성 공식

컴파일러는 비트 폭을 보고 마스크를 생성합니다:

```
(1 << width) - 1
```

width = 3 이면:

```
(1 << 3) - 1  =  111b  =  0x7
```

### 비트 폭의 역할

```
Bit Width
    ↓
Mask 생성
    ↓
Encoding
    ↓
Decoding
```

비트 폭은 이 전체 과정의 **출발점**입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

직접 보이는 경우는 적으며, 주로 내부 자료구조에 존재합니다.

### Linux Kernel

- `Page Table Entry`
- `Permission Flags`
- `CPU Status Flags`

### Network Stack

- `IPv4 Header`
- `TCP Header`
- `TCP Flags`

### Device Driver

- `Control Register`
- `Status Register`

### Kubernetes

직접 노출되지는 않지만 다음 내부 구현에서 사용됩니다:

- `Kernel`
- `Network Stack`
- `Container Runtime`

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*