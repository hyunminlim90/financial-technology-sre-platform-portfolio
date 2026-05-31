# 비트 필드 이미지 데이터 (Bit Field Image Data)

> 정독: 0회

## 1. 이 기술이 무엇인가

**비트 필드 이미지 데이터(Bit Field Image Data)** 는:

> 비트 필드에 저장된 논리적 값들이 레이아웃(Layout), 인코딩(Encoding), 정렬(Alignment), 패딩(Padding)[[ 규칙을 거쳐 실제 메모리 또는 레지스터에 저장된 최종 비트 데이터를 의미

### 핵심 흐름

```
Bit Field Member Value
        ↓
Encoding
        ↓
Bit Field Image
        ↓
Bit Field Image Data  ← 최종 결과
```

### 비트 필드 이미지 vs 이미지 데이터

| 개념 | 의미 |
|------|------|
| Bit Field Image | 배치된 상태 |
| Bit Field Image Data | 그 상태를 구성하는 실제 비트 값 |

<details>
<summary>Deep Dive</summary></br>

Bit Field Structure(비트 필드 구조체) [[M]](../../100-deep-dive/micro-foundations/bit-field-structure.md)  
Bit Field Mapping(비트 필드 매핑) [[M]](../../100-deep-dive/micro-foundations/bit-field-mapping.md)  
Bit Field Member Value(비트 필드 멤버 값) [[M]](../../100-deep-dive/micro-foundations/bit-field-member-value.md)  
Bit Field Physical Layout(비트 필드 물리 배치) [[M]](../../100-deep-dive/micro-foundations/bit-field-physical-layout.md)  
Bit Field Sign Encoding(비트 필드 부호 인코딩) [[M]](../../100-deep-dive/micro-foundations/bit-field-sign-encoding.md)  
Bit Field Data Alignment(비트 필드 데이터 정렬) [[M]](../../100-deep-dive/micro-foundations/bit-field-data-alignment.md)  



Bit Field Padding Rule(비트 필드 패딩 규칙) [[M]](../../100-deep-dive/micro-foundations/bit-field-padding-rule.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

비트 필드 이미지 데이터는 실제 비트 단위 저장이 필요한 모든 영역에서 등장합니다.

### CPU

- `Control Register`
- `Status Register`
- `Flag Register`

### 운영체제

- `Page Table`
- `Permission Flags`
- `Memory Management Data`

### 네트워크

- `IPv4 Header`
- `TCP Header`
- `UDP Header`
- `Ethernet Frame`

### 장치 제어

- `Device Register`
- `Interrupt Controller`
- `DMA Controller`

### 파일 포맷

- `ELF`
- `PNG`
- `JPEG`
- `MP4`

---

## 3. 어떤 자원에 가장 영향이 큰가

### 1순위: Memory

비트 필드 이미지 데이터는 **실제 저장되는 데이터 자체**이기 때문입니다.

### 추가 영향

| 자원 | 영향 |
|------|------|
| CPU | 로드(Load), 스토어(Store), 마스킹(Masking), 시프트(Shift) |
| Network | 패킷 직렬화 / 역직렬화 |
| Disk | 파일 헤더 저장, 메타데이터 저장 |

---

## 4. 왜 중요한가

개발자가 보는 값과 CPU가 읽는 값은 다릅니다.

```
개발자:  mode = 5,  active = 1
CPU:     00001101
```

논리적 데이터를 실제 저장 데이터로 변환한 결과가 **비트 필드 이미지 데이터**입니다.

### 반드시 해석이 필요한 상황

- 메모리 덤프
- 패킷 분석
- 코어 덤프
- 레지스터 분석

---

## 5. 실제 장애와 어떤 관련이 있는가

### ABI 불일치

```
시스템 A:  Field A → Offset 0,  Field B → Offset 3
시스템 B:  Field A → Offset 3,  Field B → Offset 0
```

같은 값을 저장해도 비트 필드 이미지 데이터가 달라집니다:

- 상호 통신 실패

### 엔디언 문제

Little Endian과 Big Endian 혼용 시:

```
다른 비트 패턴 생성  →  프로토콜 오류
```

### 패딩 오해

실제 저장 데이터에 Reserved Bit, Padding Bit가 포함되어 있음:

- 데이터 파싱 실패

### 하드웨어 제어 실패

잘못된 이미지 데이터 생성 시:

- 레지스터 오동작
- 장치 동작 실패

---

## 6. 핵심 메커니즘

### 전체 흐름

```
Bit Field Declaration
        ↓
Bit Width
        ↓
Bit Offset
        ↓
Bit Field Layout
        ↓
Bit Field Member Value
        ↓
Bit Field Encoding
        ↓
Bit Field Image
        ↓
Bit Field Image Data  ← 최종 결과
```

### 단계별 예시

```
Field A:  Width=3,  Value=5
Field B:  Width=2,  Value=2
```

**인코딩**

```
A = 5  →  101
B = 2  →  10
```

**레이아웃 적용**

```
10 | 101  →  10110
```

**패딩 포함 최종 저장**

```
00010110
```

이 값이 **Bit Field Image Data**입니다.

---

### 세 개념의 역할 구분

| 개념 | 역할 |
|------|------|
| Bit Field Member Value | 논리적 값 |
| Bit Field Encoding | 변환 과정 |
| Bit Field Image Data | 실제 저장 결과 |

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux — 메모리 상태 확인

```bash
hexdump
xxd
od
```

### 프로세스 메모리

```
/proc/<pid>/mem
```

### 커널 정보

```
/proc
/sys
```

### 디버깅 도구

```bash
gdb
lldb
objdump
readelf
```

### 네트워크

```bash
tcpdump
wireshark
```

패킷 내부 비트 데이터를 직접 확인할 수 있습니다.

### Kubernetes

직접 보이지는 않지만 아래 계층에서 사용됩니다:

- `Linux Kernel`
- `Container Runtime`
- `eBPF`
- `CNI`
- `Network Stack`
- `Device Driver`