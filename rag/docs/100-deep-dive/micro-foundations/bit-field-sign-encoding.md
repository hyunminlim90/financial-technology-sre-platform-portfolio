# 비트 필드 부호 인코딩 (Bit-field Sign Encoding)

> 정독: 0회

## 1. 이 기술이 무엇인가

비트 필드 부호 인코딩(Bit-field Sign Encoding)은:

> 비트 필드 멤버의 값을 저장할 때, 해당 필드가 `signed`인지 `unsigned`인지에 따라 값을 비트 패턴으로 변환하는 방식

```
논리적 정수 값
↓
부호 규칙 적용
↓
비트 패턴 생성
↓
비트 세그먼트 저장
```

예를 들어 3비트 필드가 있을 때:

| 타입 | 표현 범위 |
|---|---|
| `unsigned 3bit` | 0 ~ 7 |
| `signed 3bit` | -4 ~ 3 |

같은 3비트 공간이라도 **부호 규칙**에 따라 의미가 달라집니다.

<details>
<summary>Deep Dive</summary></br>

Bit Field Member Value(비트 필드 멤버 값) [[M]](../../100-deep-dive/micro-foundations/bit-field-member-value.md)  
Bit Field Image Data Storage(비트 필드 이미지 데이터 저장) [[M]](../../100-deep-dive/micro-foundations/bit-field-image-data-storage.md)  
Bit Field Member(비트 필드 멤버) [[M]](../../100-deep-dive/micro-foundations/bit-field-member.md)  
Sign Attribute(부호 속성) [[M]](../../100-deep-dive/micro-foundations/sign-attribute.md)  
Signed Type Attribute(부호 있는 타입 속성) [[M]](../../100-deep-dive/micro-foundations/signed-type-attribute.md)  
Unsigned Type Attribute(부호 없는 타입 속성) [[M]](../../100-deep-dive/micro-foundations/unsigned-type-attribute.md)  
Physical Bit Pattern(물리적 비트 패턴) [[M]](../../100-deep-dive/micro-foundations/physical-bit-pattern.md)  
Bit Field Sign Conversion Mechanism(비트 필드 부호 변환 메커니즘) [[M]](../../100-deep-dive/micro-foundations/bit-field-sign-conversion-mechanism.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

주로 다음 영역에서 등장합니다.

**CPU 제어 레지스터**
- 상태값, 오프셋, 설정값

**장치 드라이버**
- NIC, GPU, Storage Controller

**네트워크 프로토콜**
- TCP, IPv4, IPv6, VLAN

**운영체제 커널**
- Page Table, CPU State, Interrupt State

**펌웨어**
- Embedded Register, Sensor Register, Microcontroller Register

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **CPU**입니다. CPU가 값을 읽을 때 부호 확장(Sign Extension)을 수행해야 하기 때문입니다.

그 다음은 **Memory**입니다. 동일한 비트 폭이라도 `signed`와 `unsigned` 해석 방식이 달라집니다.

---

## 4. 왜 중요한가

비트 필드에서는 비트 폭이 매우 작습니다(1bit, 2bit, 3bit, 4bit 등). 따라서 부호 비트 사용 여부가 표현 가능한 값 범위를 크게 바꿉니다.

**unsigned 3bit**

```
000 = 0
111 = 7
```

**signed 3bit**

```
000 =  0
011 =  3
100 = -4
111 = -1
```

즉, **동일한 비트 패턴도 부호 규칙에 따라 다른 값**이 됩니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 값 해석 오류

가장 흔한 문제입니다.

| 비트 패턴 | unsigned 해석 | signed 해석 |
|---|---|---|
| `111` | 7 | -1 |

결과: 장치 설정 오류, 프로토콜 파싱 오류 발생 가능

### ABI 차이

컴파일러마다 `signed bit-field` 처리 방식이 다를 수 있습니다. 결과적으로 구조체 교환 실패가 발생할 수 있습니다.

### 범위 초과

`signed 3bit` 필드에 `5`를 저장하면 절삭(Truncation) 또는 예상치 못한 값이 저장될 수 있습니다.

### 레지스터 제어 실패

하드웨어가 `unsigned` 값을 기대하는데 `signed`로 선언하면 음수 해석이 발생할 수 있습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

비트 필드 전체 흐름은 다음과 같습니다.

```
Bit Field Declaration
↓
Bit Width 결정
↓
Bit Field Allocation
↓
Physical Layout 생성
↓
Sign Encoding 수행
↓
Bit Pattern 생성
```

부호 인코딩은 **Layout 이후**에 발생합니다.

예를 들어 `signed field : width 3`, `value = -3`이면 CPU는 2의 보수(Two's Complement)로 변환합니다.

```
+3  →  011
         ↓ (2의 보수 변환)
-3  →  101
```

나중에 읽을 때 MSB = 1이므로 CPU는 음수로 복원합니다.

핵심은 **Bit Width + Signed 여부**가 최종 비트 패턴을 결정한다는 점입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

직접적으로 보이는 경우는 드물지만 다음 영역에서 간접적으로 확인됩니다.

### Linux Kernel

커널 구조체 내부 `flags`, `state`, `mode` 필드

### Device Driver

```bash
ethtool
lspci -vv      # 장치 레지스터 값 해석
```

### 디버거 / 메모리 덤프

```bash
gdb
hexdump
xxd
```

### 네트워크 프로토콜 분석

```bash
tcpdump
wireshark
```

### Kubernetes

Kubernetes 자체보다는 Kernel, Driver, Container Runtime, Network Stack 내부 구현에서 많이 사용됩니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*