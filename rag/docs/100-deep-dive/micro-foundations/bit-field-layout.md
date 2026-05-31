# 비트 필드 레이아웃 (Bit Field Layout)

> 정독: 0회

## 1. 이 기술이 무엇인가

**비트 필드 레이아웃(Bit Field Layout)** 은:

> 비트 필드 선언이 끝난 후 각 비트 필드 멤버가 실제 워드(Word) 안의 어느 비트 위치에 존재하는지 최종 확정된 배치도

### 전체 흐름

```
Bit Field Declaration
        ↓
Bit Field Allocation
        ↓
Bit Field Mapping
        ↓
Bit Field Layout  ← 최종 결과물
```

### 예시

선언:

```
fieldA : 2bit
fieldB : 3bit
fieldC : 1bit
```

최종 확정 레이아웃:

```
bit 0~1  →  fieldA
bit 2~4  →  fieldB
bit 5    →  fieldC
```

---

## 2. 시스템 어디에서 등장하는가

비트 필드 레이아웃은 **메모리 내부의 비트 위치가 중요한 곳**에서 등장합니다.

### CPU

- `Status Register`
- `Control Register`
- `Flag Register`

### 운영체제

- `Page Table Entry`
- `Page Flags`
- `Process Flags`
- `Permission Flags`

### 네트워크

- `IPv4 Header`
- `TCP Header`
- `IPv6 Header`

### 디바이스 드라이버

- `MMIO Register`
- `DMA Register`
- `Interrupt Register`

### 펌웨어

- `Hardware Control Block`
- `Device Configuration Block`

---

## 3. 어떤 자원에 가장 영향이 큰가

### 1순위: Memory

레이아웃은 **실제 비트 저장 위치**를 결정합니다.

### 2순위: CPU

CPU는 레이아웃 정보를 이용해 Load, Mask, Shift, Store를 수행합니다.

Network와 Disk는 프로토콜 구조, 메타데이터 구조를 정의할 때 간접적으로 영향을 받습니다.

---

## 4. 왜 중요한가

비트 필드 선언만으로는 fieldA, fieldB, fieldC가 실제 어디에 저장되는지 알 수 없습니다.

CPU가 이해하는 것은 `"fieldA를 읽어라"` 가 아니라 다음과 같은 물리적 위치 정보입니다:

```
Offset = 4
Width  = 3
```

### 레이아웃이 결정하는 것

| 항목 | 의미 |
|------|------|
| Offset | 몇 번째 비트부터 시작하는가 |
| Width | 몇 비트를 차지하는가 |
| Position | 워드 내 최종 물리 위치 |

> 레이아웃은 논리 구조 → 실제 비트 위치를 연결하는 **최종 설계도**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### ABI 불일치

```
시스템 A:  fieldA → bit 0~2,  fieldB → bit 3~7
시스템 B:  fieldA → bit 3~5,  fieldB → bit 0~2
```

동일 데이터라도 **완전히 다른 값**으로 해석됩니다.

### 프로토콜 파싱 오류

TCP/IP 헤더 해석 시 레이아웃을 잘못 가정 시:

- Version, Flags, Length 오인식
- 통신 실패

### 디바이스 드라이버 장애

```
레지스터 문서:  bit 5 = enable
코드 해석:      bit 4 = enable
    ↓
장치 동작 실패
```

### 크로스 플랫폼 장애

ARM, x86, RISC-V 간 ABI 차이로 레이아웃 차이 발생:

- 데이터 불일치

---

## 6. 핵심 메커니즘

비트 필드 레이아웃은 결국 다음 4개로 정의됩니다:

| 요소 | 의미 |
|------|------|
| Offset | 몇 번째 비트부터 시작하는가 |
| Width | 몇 비트를 차지하는가 |
| Ordering | 어떤 방향으로 배치하는가 (LSB→MSB 또는 MSB→LSB) |
| Padding | 정렬을 위해 삽입된 비어 있는 비트 |

### 실행 시 CPU 동작

```
Load
  ↓
Shift
  ↓
Mask
  ↓
Extract
```

### 예시: fieldB (offset=3, width=5)

```
(word >> 3) & 0x1F
```

### 핵심 원칙

> 레이아웃은 **Mask와 Shift가 무엇을 기준으로 동작할지**를 정의합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux Kernel

대표 사용처:

- `Page Table Entry`
- `Page Flags`
- `Capability Flags`
- `Task Flags`

커널 문서에는 종종 다음 형태의 레이아웃이 명시됩니다:

```
bit 0  → ...
bit 1  → ...
bit 2  → ...
```

### Device Driver

가장 많이 사용됩니다:

- `Control Register Layout`
- `Status Register Layout`

### Network Stack

- `IPv4 Header Layout`
- `TCP Header Layout`

### Hypervisor

- `VMCS`
- `VMCB`
- `CPU Control Structure`

### Kubernetes

직접 노출되지는 않지만 하부 계층에서 광범위하게 사용됩니다:

- `Kernel`
- `cgroup`
- `Namespace`
- `Driver`

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*