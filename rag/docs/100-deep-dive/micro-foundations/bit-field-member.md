# 비트 필드 멤버 (Bit Field Member)

> 정독: 0회

## 1. 이 기술이 무엇인가

**비트 필드 멤버(Bit Field Member)** 는:

> 하나의 워드(Word) 내부에서 특정 비트 구간만 점유하도록 정의된 논리적 데이터 영역

### 예시

```
32비트 워드 내부:
[ 1bit power ][ 3bit mode ][ 4bit level ]
```

각 영역이 각각 하나의 비트 필드 멤버입니다.

### 각 멤버의 구성

- **시작 위치 (offset)**
- **비트 폭 (width)**

즉, **비트 단위의 작은 변수**를 워드 내부에 압축 저장하는 구조입니다.

<details>
<summary>Deep Dive</summary></br>

Word(워드) [[M]](../../100-deep-dive/micro-foundations/word.md)  
Data Container(데이터 컨테이너) [[M]](../../100-deep-dive/micro-foundations/data-container.md)  
Bit Segment(비트 세그먼트) [[M]](../../100-deep-dive/micro-foundations/bit-segment.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

비트 필드 멤버는 저수준 시스템에서 매우 많이 등장합니다.

### 대표 영역

| 영역 | 사용 사례 |
|------|-----------|
| OS Kernel | process flags |
| CPU Register | control/status field |
| Device Driver | hardware register |
| Network | protocol header |
| Filesystem | inode metadata |
| Hypervisor | VM control field |
| Runtime | object metadata |
| Embedded System | compact device state |

### 대표 예시

- `TCP header flag`
- `page table entry`
- `interrupt control register`
- `GPU command field`
- `permission bit`

---

## 3. 어떤 자원에 가장 영향이 큰가

### 1순위: Memory

비트 필드 멤버의 핵심 목적은 **메모리 압축**이기 때문입니다.

### 추가 영향

| 자원 | 영향 |
|------|------|
| CPU | mask/shift operation 증가 |
| Cache | cache density 증가 |
| Network | packet compactness |
| Disk | metadata compactness |

> 특히 **cache locality 개선** 효과가 큽니다.

---

## 4. 왜 중요한가

비트 필드 멤버는 **매우 작은 상태 데이터를 고밀도로 저장**할 수 있게 해줍니다.

### 예시: 상태 플래그 저장 비교

다음 상태들을 각각 4바이트 `int`로 저장하면 매우 비효율적입니다:

- `lock`
- `dirty`
- `readable`
- `writable`
- `executable`

비트 필드 사용 시 **단 몇 비트만으로** 저장 가능합니다.

> kernel, runtime, packet parser, embedded firmware 같은 시스템에서 특히 중요합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 잘못된 Offset

offset 계산 오류 시:

- wrong field access
- invalid flag read
- corrupted state

### Width Overflow

```
3bit field에 15 저장  →  overflow/truncation 발생
```

### Compiler Layout Difference

컴파일러/ABI마다 packing, ordering, alignment 차이 발생 가능.

결과: binary incompatibility

### Race Condition

동일 워드 내 여러 field를 멀티스레드가 동시에 수정하면:

- lost update
- torn write

### Endianness 문제

네트워크/디바이스 환경에서:

- bit ordering mismatch
- protocol corruption

---

## 6. 핵심 메커니즘

핵심은 **Mask + Shift** 입니다.

### A. 워드 내부 구획

```
[0]     power
[1~3]   mode
[4~7]   level
```

각 구간이 독립적인 field입니다.

---

### B. 데이터 저장

`mode = 5` 저장 예시:

```
# shift
5 << 1

# OR로 삽입
word |= shifted
```

특정 비트 구간에 값을 삽입합니다.

---

### C. 데이터 읽기

`mode` 읽기 예시:

```
# mask 적용
word & 0b00001110

# reverse shift
>> 1
```

원래 정수값을 복원합니다.

---

### D. 수정 과정

```
Load
  ↓
Mask Clear
  ↓
Shift
  ↓
Insert
  ↓
Store
```

기존 영역을 제거한 후 새 값을 삽입하는 순서로 진행합니다.

---

### E. 핵심 특징

| 관점 | 특징 |
|------|------|
| 논리적 | 여러 개의 독립 변수 |
| 물리적 | 하나의 워드 |

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux Kernel

매우 광범위하게 사용:

- `page flags`
- `inode flags`
- `scheduler state`
- `CPU status field`

### Device Driver

하드웨어 레지스터에서 각 비트 구간별 기능 정의가 매우 흔합니다.

### Network Stack

TCP/IP header 비트 필드 기반:

- `version`
- `flags`
- `length`
- `type`

### Runtime

runtime metadata:

- `GC state`
- `lock state`
- `ownership`
- `mark bit`

### Kubernetes

직접 노출은 적지만 내부적으로:

- `cgroup flags`
- `namespace flags`
- `kernel capability`

### 관측 도구

- `hexdump`
- `tcpdump`
- `Wireshark`
- `objdump`
- `readelf`
- kernel source
- device datasheet

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*