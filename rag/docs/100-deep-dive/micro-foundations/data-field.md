# 데이터 필드 (Data Field)

> 정독: 0회

## 1. 이 기술이 무엇인가

**데이터 필드(Data Field)** 는:

> 특정 의미를 가진 데이터 단위

핵심은 비트(Bit), 바이트(Byte) 같은 물리적 저장 단위가 아니라, **의미(Semantics)를 가진 논리적 데이터 단위**라는 점입니다.

### 예시

```
user_id
name
age
balance
status
```

모두 데이터 필드입니다.

### 데이터 필드의 구성

| 구성 요소 | 예시 |
|-----------|------|
| Field Name | age |
| Data Type | Integer |
| Data Value | 35 |

---

## 2. 시스템 어디에서 등장하는가

데이터 필드는 거의 모든 컴퓨터 시스템에서 등장합니다.

### 데이터베이스

- `customer_id`
- `account_no`
- `amount`
- `created_at`

### 파일 포맷

- `file_size`
- `version`
- `checksum`

### 네트워크 프로토콜

- `source_port`
- `destination_port`
- `sequence_number`

### 운영체제

- `pid`
- `uid`
- `gid`
- `permission`

### 하드웨어

- `control_bit`
- `interrupt_flag`
- `error_code`

> **의미 있는 정보**가 존재하는 곳이면 항상 데이터 필드가 존재합니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

데이터 필드는 특정 자원보다 **데이터 구조 자체**에 영향을 줍니다.

### 주요 영향 자원

| 자원 | 영향 |
|------|------|
| Memory | 필드 정의가 메모리 레이아웃을 결정 |
| Network | 프로토콜 헤더의 필드 구조가 패킷 크기와 직렬화 방식을 결정 |
| Disk | 레코드 구조와 저장 형식을 결정 |

---

## 4. 왜 중요한가

컴퓨터는 비트, 바이트, 워드만 이해합니다. 반면 사람은 주문번호, 잔액, 상태코드, 사용자ID를 이해합니다.

데이터 필드는 이 두 계층을 연결합니다:

```
물리적 데이터
      ↓
업무 의미
```

### 예시

| 표현 | 의미 |
|------|------|
| `32비트 정수` | 단순한 데이터 |
| `balance` 필드 | 계좌 잔액이라는 의미 |

> 데이터 필드는 **데이터에 의미를 부여하는 기본 단위**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 필드 해석 오류

```
시스템 A:  amount  →  원화로 저장
시스템 B:  amount  →  달러로 해석
    ↓
금액 오류 발생
```

### 필드 누락

API 변경 시 required field가 누락 시:

- 서비스 장애

### 필드 타입 불일치

```
Integer 예상  →  실제 String 수신
    ↓
파싱 실패
```

### 프로토콜 버전 충돌

새 버전에서 field 추가 시:

```
구 버전 시스템  →  잘못된 필드 위치를 읽음
    ↓
통신 장애
```

---

## 6. 핵심 메커니즘

현재까지의 흐름에서 중요한 것은 **데이터 필드 ≠ 비트 필드**라는 점입니다.

| 개념 | 의미 |
|------|------|
| 데이터 필드 | 논리적 정보 단위 |
| 비트 필드 | 데이터 필드를 저장하기 위한 물리적 표현 방식 |

### 예시

`status`라는 데이터 필드가 있다고 가정할 때:

논리적으로는 `status` 하나지만, 물리적으로는:

```
bit 0  =  active
bit 1  =  locked
bit 2  =  verified
```

처럼 저장될 수 있습니다.

### 전체 계층 관계

```
Data Field
    ↓
Bit Field
    ↓
Bit Segment
    ↓
Bit Offset
    ↓
Bit Layout
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

`/proc`의 각 정보가 데이터 필드입니다:

- `PID`
- `UID`
- `GID`
- `Permission`
- `State`

### Network

TCP 헤더의 데이터 필드:

- `source_port`
- `destination_port`
- `window_size`

### Filesystem

inode의 데이터 필드:

- `owner`
- `permission`
- `timestamp`

### Kubernetes

YAML의 각 항목이 데이터 필드입니다:

- `podName`
- `namespace`
- `image`
- `replicas`
- `restartCount`