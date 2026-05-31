# 비트 오프셋 (Bit Offset)

> 정독: 0회

## 1. 이 기술이 무엇인가

**비트 오프셋(Bit Offset)** 은:

> 데이터 컨테이너(Word/Register) 내부에서 특정 비트 필드나 비트 세그먼트가 시작되는 위치를 의미

### 예시

```
31                      0
|--------|----|--------|
 payload   mode  state
```

| 필드 | 비트 오프셋 |
|------|-------------|
| state | 0 |
| mode | 3 |

> **Bit Offset = 해당 필드의 시작 비트 번호**

---

## 2. 시스템 어디에서 등장하는가

비트 오프셋은 비트 단위 레이아웃이 존재하는 거의 모든 곳에서 등장합니다.

### 대표 영역

| 영역 | 예 |
|------|-----|
| CPU Register | Status Flag |
| MMU | Page Table Entry |
| Device Driver | Hardware Register |
| Network Protocol | TCP/IP Header |
| Filesystem | Metadata Layout |
| Runtime Metadata | State Field |
| Hypervisor | VM Control Block |

### 대표 사례: TCP Header

| 필드 | Offset |
|------|--------|
| SYN | 특정 비트 |
| ACK | 특정 비트 |
| FIN | 특정 비트 |

CPU 상태 레지스터도 동일한 구조입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

### 1순위: CPU + Memory

비트 오프셋은 Load → Shift → Mask → Store 과정의 **기준 좌표**가 되기 때문입니다.

### 영향 영역

| 자원 | 영향 |
|------|------|
| CPU | Shift 연산 |
| Memory | Layout |
| Cache | Compact Storage |
| Bus | Data Transfer |
| Network | Header Decode |
| Device | Register Access |

---

## 4. 왜 중요한가

핵심 이유: **CPU가 원하는 비트 필드를 정확히 찾아가기 위한 좌표**이기 때문입니다.

CPU는 `field A` 라는 개념을 이해하지 않습니다. CPU가 이해하는 것은 오직:

```
몇 번째 비트인가?
```

뿐입니다.

### 예시

```
offset = 12
width  = 4
```

CPU는 **12번째 비트부터 4비트 사용**이라고만 이해합니다.

따라서 비트 필드, 비트 세그먼트, 하드웨어 레지스터의 **모든 접근은 오프셋 기반**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 잘못된 Offset

가장 흔한 문제입니다.

```
실제:  mode = offset 4
코드:  offset 5 로 작성
결과:  잘못된 필드 읽기 발생
```

### Hardware Register 오작동

```
bit 7 = interrupt enable  인데
bit 8 을 수정
    ↓
전혀 다른 기능 활성화
```

### Protocol Decode 실패

TCP/IP Header Parsing 오류 시:

- 잘못된 Flag 해석
- 연결 실패

### ABI Mismatch

컴파일러마다 packing, alignment 규칙 차이로 offset이 변화할 수 있습니다.

결과: 바이너리 호환성 문제 발생

---

## 6. 핵심 메커니즘

비트 필드 접근의 핵심 공식은 **Load → Shift → Mask** 입니다.

### 예시: offset = 8, width = 4 인 필드 읽기

**Step 1 — 전체 워드 Load**

```
register = memory_value
```

**Step 2 — Offset 만큼 이동**

```
register >> 8
```

목표 필드가 하위 비트 영역으로 이동합니다.

**Step 3 — Width 만큼 추출**

```
(register >> 8) & 0b1111
```

원하는 필드만 추출됩니다.

---

### 핵심 공식 정리

```
Offset = 시작 위치
Width  = 길이
```

이 두 개만 알면 CPU는 어떤 비트 필드든 접근 가능합니다.

---

### 비트 필드 수정 시

```
# 1. 기존 영역 제거
value &= ~(mask << offset)

# 2. 새 값 삽입
value |= (newValue << offset)
```

> **Bit Offset은 추출과 수정의 기준 좌표**입니다.

---

### 핵심 흐름

```
Load (전체 워드)
      ↓
Shift (>> offset)
      ↓
Mask (& width_mask)
      ↓
Result
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux Kernel

대표 사용처:

- `page flags`
- `inode flags`
- `scheduler flags`
- `capability bits`

커널 코드에서는 다음 형태로 등장:

```c
#define FLAG_OFFSET 12
```

### Device Driver

Control Register 예시:

| 기능 | Offset |
|------|--------|
| Enable | 0 |
| Interrupt | 1 |
| Reset | 2 |

### Network Stack

모든 프로토콜에서 사용:

- `TCP/IP Header`
- `IPv4 Header`
- `IPv6 Header`

### Runtime

대표 사용처:

- `object metadata`
- `lock state`
- `GC metadata`
- `runtime flags`

### Kubernetes

직접 노출은 적지만 내부적으로:

- `cgroup state`
- `capability bit`
- `namespace flag`

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*