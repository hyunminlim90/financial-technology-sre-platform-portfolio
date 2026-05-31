# 비트 필드 멤버 값 (Bit Field Member Value)

> 정독: 0회

## 1. 이 기술이 무엇인가

**비트 필드 멤버 값(Bit Field Member Value)** 은:

> 비트 필드 멤버가 표현하는 실제 데이터 값

### 예시

```c
struct Status {
    unsigned int mode : 3;
};

mode = 5;  // 5 가 비트 필드 멤버 값
```

### 핵심

일반 정수값과 동일한 개념이지만, **비트 폭(Width)에 의해 표현 가능한 범위가 제한**됩니다.

---

## 2. 시스템 어디에서 등장하는가

### 하드웨어 레지스터

- `장치 상태`
- `장치 설정`
- `제어 플래그`

### 네트워크 프로토콜

- `TCP Flag`
- `IPv4 Version`
- `DSCP`
- `ECN`

### 운영체제

- `페이지 속성`
- `권한 플래그`
- `프로세스 상태`

### 파일 포맷

- `압축 옵션`
- `버전 정보`
- `상태 플래그`

---

## 3. 어떤 자원에 가장 영향이 큰가

### 1순위: Memory

제한된 비트 공간 안에 값을 압축 저장하기 때문입니다.

### 추가 영향

| 자원 | 영향 |
|------|------|
| CPU | Mask, Shift, Encode, Decode 연산 필요 |
| Network | 패킷 헤더 크기에 영향 |
| Disk | 저장 포맷 크기에 영향 |

---

## 4. 왜 중요한가

비트 필드 시스템에서 실제로 의미를 가지는 것은 **비트 필드 멤버 값**입니다.

`폭 3비트`라는 선언 자체는 구조일 뿐이고, 실제 의미는 `0 ~ 7` 중 **어떤 값이 저장되어 있는가**입니다.

| 개념 | 의미 |
|------|------|
| Bit Field Member | 저장 공간 |
| Bit Field Member Value | 저장된 실제 정보 |

---

## 5. 실제 장애와 어떤 관련이 있는가

### 범위 초과 (Overflow)

```
width = 3bit  →  저장 가능 범위: 0 ~ 7

value = 12 저장 시
    ↓
일부 비트가 잘려나감  →  잘못된 값 저장
```

### 디코딩 오류

```
송신:  priority = 5  인코딩
수신:  offset 해석 오류
    ↓
다른 값으로 복원
```

### ABI 불일치

시스템마다 비트 배치 규칙 차이 발생 시:

- 동일한 멤버 값이 다른 값으로 해석됨

### 하드웨어 제어 실패

레지스터 설정값 오류 시:

- 장치 오동작

---

## 6. 핵심 메커니즘

### 전체 흐름

```
Data Field
    ↓
Bit Field Member
    ↓
Bit Field Member Value  ← 현재 단계
    ↓
Bit Field Encoding
    ↓
Bit Pattern
    ↓
Memory
```

### 단계별 예시

**비트 필드 멤버 선언**

```c
unsigned mode : 3;
```

**비트 필드 멤버 값**

```
mode = 5
```

**이진 표현**

```
5  →  101
```

**인코딩 (offset 위치로 이동)**

```
101 << offset
```

**최종 워드 저장**

```
00010100  (예시)
```

**읽을 때 복원**

```
Mask → Shift → Decode  →  5
```

---

### 핵심 구분

| 개념 | 계층 |
|------|------|
| 비트 필드 멤버 값 | 논리적 데이터 |
| 비트 패턴 | 물리적 표현 |

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

직접 보이는 경우는 드물며 대부분 내부 구현입니다.

### Linux Kernel

- `Page Flags`
- `Process Flags`
- `Permission Flags`

### Network Stack

- `TCP Flags`
- `IPv4 Header Fields`

### Device Driver

- `Control Register Value`
- `Status Register Value`

### Kubernetes

직접 사용하지는 않지만 아래 계층에서 사용됩니다:

- `Kernel`
- `NIC Driver`
- `TCP/IP Stack`
- `Container Runtime`

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*