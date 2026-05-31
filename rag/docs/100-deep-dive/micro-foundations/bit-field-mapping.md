# 비트 필드 매핑 (Bit Field Mapping)

> 정독: 0회

## 1. 이 기술이 무엇인가

**비트 필드 매핑(Bit Field Mapping)** 은:

> 논리적으로 선언된 비트 필드 멤버를 실제 워드(Word) 내부의 어느 비트 위치에 배치할지 결정하는 과정

### 전체 흐름

```
소스 코드
    ↓
비트 레이아웃
    ↓
실제 메모리 배치
```

### 예시

선언:

```
fieldA : 3bit
fieldB : 5bit
fieldC : 8bit
```

컴파일러의 매핑 결과:

```
fieldA  →  offset 0
fieldB  →  offset 3
fieldC  →  offset 8
```

이 결정 결과가 바로 **비트 필드 매핑**입니다.

<details>
<summary>Deep Dive</summary></br>

Bit Field Declaration(비트 필드 선언) [[M]](../../100-deep-dive/micro-foundations/bit-field-decalaration.md)  
Data Container(데이터 컨테이너) [[M]](../../100-deep-dive/micro-foundations/data-container.md)  
Bit Field Layout(비트 필드 레이아웃) [[M]](../../100-deep-dive/micro-foundations/bit-field-layout.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

비트 필드 매핑은 논리 구조 → 물리 비트 레이아웃이 필요한 모든 영역에서 등장합니다.

### 대표 사례

| 영역 | 사용 예 |
|------|---------|
| 운영체제 | Page Table |
| CPU | Status Register |
| Device Driver | MMIO Register |
| Network | TCP/IP Header |
| Storage | Filesystem Metadata |
| Firmware | Hardware Control Block |
| Hypervisor | VM Control Structure |

> 특히 **하드웨어 레지스터**와 **프로토콜 헤더**에서 매우 흔하게 사용됩니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

### 1순위: Memory + CPU

| 자원 | 영향 |
|------|------|
| Memory | 매핑 결과가 실제 메모리 레이아웃을 결정 |
| CPU | 매핑 정보 기반으로 Mask/Shift/Load/Store 코드 생성 |

간접적으로는 Network, Disk 프로토콜 구조에도 영향을 줍니다.

---

## 4. 왜 중요한가

비트 필드는 논리 구조만으로는 의미가 없습니다. 실제로 중요한 것은 **어느 비트에 저장되는가**입니다.

Power State, Mode, Priority, Error Flag가 있다고 해도 실제 위치가 정해지지 않으면 CPU는 접근할 수 없습니다.

```
논리 데이터
    ↓
실제 비트 위치
```

비트 필드 매핑은 이 두 계층을 연결하는 핵심 과정입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### ABI 불일치

가장 대표적인 장애입니다.

```
환경 A:  fieldA → offset 0,  fieldB → offset 3
환경 B:  fieldA → offset 3,  fieldB → offset 0
```

동일 데이터를 읽어도 **전혀 다른 값**으로 해석됩니다.

### 하드웨어 제어 실패

```
레지스터 명세:   bit 7 = enable
소프트웨어 매핑: bit 8 = enable
    ↓
장치 오동작
```

### 프로토콜 해석 실패

패킷 헤더의 비트 위치를 잘못 해석 시:

- 연결 실패
- 데이터 손상
- 통신 오류

### 컴파일러 변경

컴파일러 또는 ABI 변경 후 비트 레이아웃이 변화할 수 있습니다. 기존 바이너리와 호환되지 않을 수 있습니다.

---

## 6. 핵심 메커니즘

비트 필드 매핑의 핵심은 다음 흐름입니다:

```
멤버
 ↓
Offset
 ↓
Width
 ↓
Mask
 ↓
Shift
```

### 예시: fieldA : 3bit, fieldB : 5bit

컴파일러 결정:

| 필드 | Offset | Width |
|------|--------|-------|
| fieldA | 0 | 3 |
| fieldB | 3 | 5 |

**fieldB 읽기:**

```
(word >> 3) & 0x1F
```

**fieldB 쓰기:**

```
# 1. 기존 비트 제거
# 2. 값 shift
# 3. OR 삽입
```

> 비트 필드 매핑 = **Offset + Width 결정**이라고 이해하면 됩니다.

---

### 비트 필드 할당 vs 비트 필드 매핑

| 개념 | 의미 |
|------|------|
| 비트 필드 할당 (Allocation) | 어디에 배치할 것인가 **결정하는 과정** |
| 비트 필드 매핑 (Mapping) | 논리 멤버 → 실제 비트 위치를 연결한 **결과** |

실무에서는 거의 같은 맥락으로 사용되지만, 엄밀히는 Allocation은 배치 결정, Mapping은 배치 관계 확정으로 구분됩니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux Kernel

대표 사용처:

- `Page Flags`
- `Capability Bits`
- `Scheduler State`
- `Filesystem Flags`

커널은 내부적으로 offset, width 기반으로 플래그를 읽고 수정합니다.

### Device Driver

Control Register 매핑 예시:

| 필드 | Offset |
|------|--------|
| Enable | 0 |
| Reset | 1 |
| Interrupt | 2 |

이 정보 자체가 **비트 필드 매핑 결과**입니다.

### Network Stack

모든 프로토콜 디코딩은 비트 위치 매핑을 기반으로 수행됩니다:

- `TCP Header`
- `IPv4 Header`
- `IPv6 Header`

### Hypervisor

- `VM Control Block`
- `CPU Control Register`

### Kubernetes

직접 노출은 적지만 내부적으로:

- `cgroup flags`
- `namespace flags`
- `capability bits`

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*