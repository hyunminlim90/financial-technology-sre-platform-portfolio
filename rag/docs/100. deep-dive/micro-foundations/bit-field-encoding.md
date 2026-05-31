# 비트 필드 인코딩 (Bit Field Encoding)

> 정독: 0회

## 1. 이 기술이 무엇인가

**비트 필드 인코딩(Bit Field Encoding)** 은:

> 논리적 데이터 값을 정해진 비트 위치와 크기에 맞게 워드 내부의 비트 패턴으로 변환하는 과정

### 핵심 공식

```
값(Value)
+ 비트 오프셋(Bit Offset)
+ 비트 폭(Bit Width)
        ↓
최종 워드(Bit Pattern)
```

### 예시

```
mode   = 5
width  = 3bit
offset = 4
```

값 `101`을 `1010000` 형태의 비트 패턴으로 배치하는 과정이 인코딩입니다.

<details>
<summary>Deep Dive</summary></br>

Bit Field Member Value(비트 필드 멤버 값) [[M]](../../100-deep-dive/micro-foundations/bit-field-member-value.md)  
Bit Offset(비트 오프셋) [[M]](../../100-deep-dive/micro-foundations/bit-offset.md)  
Bit Width(비트 폭) [[M]](../../100-deep-dive/micro-foundations/bit-width.md)  
Bit Segment(비트 세그먼트) [[M]](../../100-deep-dive/micro-foundations/bit-segment.md)  
Bit Pattern(비트 패턴) [[M]](../../100-deep-dive/micro-foundations/bit-pattern.md)  
Bit Field Image(비트 필드 이미지) [[M]](../../100-deep-dive/micro-foundations/bit-field-image.md)  
Bit Field Mapping(비트 필드 매핑) [[M]](../../100-deep-dive/micro-foundations/bit-field-mapping.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

### 하드웨어 레지스터

- `제어 레지스터`
- `상태 레지스터`
- `인터럽트 마스크`

### 네트워크 프로토콜

- `IPv4 Header`
- `TCP Header`
- `UDP Header`

### 파일 포맷

- `ELF`
- `PNG`
- `JPEG`
- `MP4`

### 임베디드 시스템

- `장치 설정값`
- `센서 상태`
- `제어 플래그`

### 운영체제 커널

- `프로세스 상태`
- `페이지 테이블 플래그`
- `권한 정보`

---

## 3. 어떤 자원에 가장 영향이 큰가

### 1순위: Memory

비트 단위 공간 압축을 수행하기 때문입니다.

### 추가 영향

| 자원 | 영향 |
|------|------|
| CPU | Shift, AND, OR, XOR 연산 사용 |
| Network | 프로토콜 헤더 크기 감소 |
| Disk | 저장 포맷 압축 |

---

## 4. 왜 중요한가

비트 필드 인코딩이 없다면 1비트, 3비트, 5비트 정보를 각각 별도 변수로 저장해야 합니다.

### 비트 필드 인코딩 미사용 시 문제

- 메모리 사용량 증가
- 캐시 효율 저하
- 전송량 증가

### 비트 필드 인코딩 사용 시 효과

```
여러 개의 논리적 데이터 필드
        ↓
하나의 워드
```

- 메모리 절약
- 캐시 효율 향상
- 프로토콜 효율 향상

---

## 5. 실제 장애와 어떤 관련이 있는가

### 오프셋 불일치

```
송신 시스템:  offset = 4
수신 시스템:  offset = 5
    ↓
완전히 다른 값 해석
```

### 폭(Width) 불일치

```
송신:  priority = 3bit
수신:  priority = 4bit
    ↓
필드 경계 파손
```

### 엔디언 문제

Little Endian과 Big Endian 환경 혼합 시:

- 필드 해석 오류

### ABI 불일치

컴파일러마다 비트 필드 배치 규칙이 다를 수 있습니다.

- 구조체 공유 시 데이터 손상

---

## 6. 핵심 메커니즘

### 전체 흐름 관계

```
Data Field
    ↓
Bit Field Member
    ↓
Bit Field Declaration
    ↓
Bit Field Allocation
    ↓
Bit Field Mapping
    ↓
Bit Field Layout
    ↓
Bit Field Encoding  ← 현재 단계
```

### 인코딩 단계별 예시

```
field width  = 3bit
field offset = 5
value        = 6
```

**Step 1 — 값을 이진수로 변환**

```
6  →  110
```

**Step 2 — 오프셋만큼 이동**

```
110 << 5  =  11000000
```

**Step 3 — 마스킹 (필요 시)**

```
(value & mask)
```

**Step 4 — 기존 워드에 삽입**

```
word |= encodedValue
```

**Step 5 — 최종 워드 생성**

---

### 핵심 정의

> 인코딩 = 논리적 값 → 비트 레이아웃에 맞는 **물리적 비트 패턴 생성**

---

### 인코딩 vs 디코딩

| 방향 | 과정 |
|------|------|
| 인코딩 | 값 → 워드 |
| 디코딩 | 워드 → 값 |

항상 반대 방향으로 동작합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

대부분 내부 구현으로, 직접적으로는 잘 보이지 않습니다.

### Linux

내부에서 사용:

- `파일 권한 (permission)`
- `프로세스 상태 플래그`
- `페이지 속성 비트`

### Network

패킷 헤더 생성 시 비트 필드 인코딩:

- `Version`
- `Flags`
- `TTL`
- `Protocol`

### Device Driver

작성 시 사용:

- `Control Register`
- `Status Register`

### Kubernetes

사용자가 직접 보지는 않지만 다음 내부에서 계속 사용됩니다:

- `네트워크 패킷`
- `컨테이너 런타임`
- `커널 자료구조`

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*