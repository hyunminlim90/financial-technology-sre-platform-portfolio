# 비트 필드 (Bit Field)

> 정독: 0회

## 1. 이 기술이 무엇인가

**비트 필드(Bit Field)** 는:

> 하나의 정수 데이터 내부를 여러 개의 비트 구간으로 나누어, 각 구간에 서로 다른 의미의 데이터를 저장하는 구조

### 예시

```
32비트 정수:
[ 3bit 상태 ][ 1bit 에러 ][ 4bit 우선순위 ][ 24bit reserved ]
```

### 핵심 특징

하나의 정수 안에 여러 상태와 값을 **압축 저장**합니다.

### 사용 목적

- 메모리 절약
- 캐시 효율 증가
- 빠른 상태 판별

<details>
<summary>Deep Dive</summary></br>

Word(워드) [[M]](../../100-deep-dive/micro-foundations/word.md)  
Bit Field Member(비트 필드 멤버) [[M]](../../100-deep-dive/micro-foundations/bit-field-member.md)  
Bit Segment(비트 세그먼트) [[M]](../../100-deep-dive/micro-foundations/bit-segment.md)  
Bit Field Member Value(비트 필드 멤버 값) [[M]](../../100-deep-dive/micro-foundations/bit-field-member-value.md)  
Bit Field Encoding(비트 필드 인코딩) [[M]](../../100-deep-dive/micro-foundations/bit-field-encoding.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

비트 필드는 시스템 전반에 매우 광범위하게 존재합니다.

### 대표 영역

| 영역 | 사용 사례 |
|------|-----------|
| CPU | status register |
| OS Kernel | process flags |
| Filesystem | inode flags |
| Network | packet header |
| Device Driver | hardware control register |
| Hypervisor | VM state |
| Runtime | object metadata |
| Database | compact state encoding |
| GPU | command flags |

### 대표 예시

- `permission bit`
- `TCP flag`
- `interrupt flag`
- `GC state`
- `page state`
- `lock state`

---

## 3. 어떤 자원에 가장 영향이 큰가

### 1순위: Memory

비트 필드는 본질적으로 **메모리 공간 압축 기술**이기 때문입니다.

### 추가 영향

| 자원 | 영향 |
|------|------|
| CPU | bitwise operation 증가 |
| Cache | cache locality 개선 |
| Network | packet size 감소 |
| Disk | metadata compactness 증가 |

> 특히 **Cache efficiency** 향상 효과가 매우 큽니다.

---

## 4. 왜 중요한가

비트 필드는 **대규모 시스템의 메모리 밀도를 극단적으로 높이는 핵심 기법**입니다.

### 예시: boolean 8개 저장

| 방식 | 소비 메모리 |
|------|-------------|
| 일반 저장 | 8 byte 이상 |
| 비트 필드 사용 | 1 byte |

대규모 객체 수백만 개 환경에서는 heap size, cache miss, memory bandwidth 차이가 매우 커집니다.

> 특히 OS kernel, runtime, network stack, DB engine 같은 저수준 시스템에서 중요합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 잘못된 마스크

잘못된 bit offset 사용 시:

- wrong flag read
- corrupted state
- invalid branch

### Endianness 문제

네트워크/디바이스 환경에서:

- bit order mismatch
- packet corruption

### Overflow

필드 크기 초과 값 저장 시:

```
3bit field에 15 저장  →  truncation 발생
```

### Race Condition

멀티스레드 환경에서 동일 워드의 비트 필드 동시 수정 시:

- lost update
- flag corruption

### ABI/Layout 문제

컴파일러마다 packing, alignment, ordering 차이 발생 가능.

결과: binary incompatibility

---

## 6. 핵심 메커니즘

핵심은 **Shift + Mask** 조합입니다.

### A. 공간 구획

```
32비트:
[0~2]   state
[3]     error
[4~7]   priority
[8~31]  reserved
```

각 비트 위치에 의미를 부여합니다.

---

### B. 값 저장

`priority = 5` 저장 예시:

```
# shift
5 << 4

# OR로 삽입
word |= shifted_value
```

특정 위치에 값을 삽입합니다.

---

### C. 값 추출

`priority` 읽기 예시:

```
# mask 적용
word & 0b11110000

# reverse shift
>> 4
```

원래 숫자를 복원합니다.

---

### D. 값 수정

```
# 1. clear
word &= ~mask

# 2. insert
word |= new_value
```

---

### E. 핵심 흐름

```
Load
  ↓
Mask
  ↓
Shift
  ↓
Bitwise Operation
  ↓
Store
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux Kernel

매우 광범위하게 사용:

- `page flag`
- `inode flag`
- `process state`
- `scheduler state`
- `interrupt state`

### Network Stack

TCP/IP header 비트 필드 기반:

- `SYN`
- `ACK`
- `FIN`
- `RST`

### Device Register

하드웨어 레지스터 대부분이 비트 필드 기반 제어입니다.

### Runtime

runtime metadata:

- `GC mark`
- `lock state`
- `age`
- `ownership flag`

### Kubernetes

직접 드러나진 않지만 내부적으로:

- `cgroup flag`
- `namespace capability`
- `container state`

### 관측 도구

- `hexdump`
- `tcpdump`
- `Wireshark`
- `perf`
- kernel source
- `objdump`
- disassembly

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*