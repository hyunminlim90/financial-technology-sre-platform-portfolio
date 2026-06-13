# 비트 필드 이미지 (Bit Field Image)

> 정독: 0회

## 1. 이 기술이 무엇인가

**비트 필드 이미지(Bit Field Image)** 는:

> 비트 필드에 저장된 값들이 인코딩 과정을 거쳐 실제 메모리나 레지스터 안에 존재하는 최종 비트 패턴 상태를 의미

### 핵심 공식

```
Bit Field Member
+ Bit Field Member Value
+ Layout
+ Encoding
        ↓
Bit Field Image
```

### 예시

```
Field A : 3bit,  A = 5
Field B : 2bit,  B = 2
```

메모리 내부에 생성되는 최종 비트 패턴:

```
10110
```

이 최종 비트 패턴이 **비트 필드 이미지**입니다.

<details>
<summary>Deep Dive</summary></br>

Bit Field Member Value(비트 필드 멤버 값) [[M]](../../100-deep-dive/micro-foundations/bit-field-member-value.md)  
Bit Field Mapping(비트 필드 매핑) [[M]](../../100-deep-dive/micro-foundations/bit-field-mapping.md)  
Data Container(데이터 컨테이너) [[M]](../../100-deep-dive/micro-foundations/data-container.md)  
CPU Register(CPU 레지스터) [[M]](../../100-deep-dive/micro-foundations/cpu-register.md)  
Bit Pattern(비트 패턴) [[M]](../../100-deep-dive/micro-foundations/bit-pattern.md)  
Bit Field Image Data(비트 필드 이미지 데이터) [[M]](../../100-deep-dive/micro-foundations/bit-field-image-data.md)  
Bit Field Layout(비트 필드 레이아웃) [[M]](../../100-deep-dive/micro-foundations/bit-field-layout.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

### CPU 레지스터

- `Status Register`
- `Control Register`
- `Flag Register`

### 운영체제 커널

- `Page Table Entry`
- `Permission Flags`
- `Process Flags`

### 네트워크 프로토콜

- `IPv4 Header`
- `TCP Header`
- `Ethernet Header`

### 장치 드라이버

- `Device Control Register`
- `Device Status Register`

### 파일 포맷

- `ELF Header`
- `PNG Header`
- `MP4 Header`

---

## 3. 어떤 자원에 가장 영향이 큰가

### 1순위: Memory

비트 필드 이미지는 **메모리에 실제 저장되는 최종 형태**이기 때문입니다.

### 추가 영향

| 자원 | 영향 |
|------|------|
| CPU | 로드, 스토어, 마스킹, 시프트 연산 수행 |
| Network | 패킷 전송 시 실제로 송수신되는 데이터 |
| Disk | 파일 헤더 저장 |

---

## 4. 왜 중요한가

개발자는 `mode = 5`를 봅니다. 하지만 CPU는 `00010100` 같은 비트 패턴만 봅니다.

```
논리적 데이터  ←→  실제 저장 데이터
```

이 두 계층을 연결하는 고리가 **비트 필드 이미지**입니다.

### 비트 필드 이미지가 필요한 분석 상황

- 메모리 덤프
- 패킷 캡처
- 레지스터 덤프
- 코어 덤프

위 상황에서는 항상 비트 필드 이미지를 해석해야 합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### ABI 불일치

```
시스템 A:  Field A → Offset 0,  Field B → Offset 3
시스템 B:  Field A → Offset 3,  Field B → Offset 0
```

같은 값을 저장해도 비트 필드 이미지가 달라집니다:

- 데이터 해석 실패

### 엔디언 문제

Little Endian과 Big Endian 환경 혼합 시:

```
동일한 데이터  →  다른 비트 이미지 생성
```

### 패딩 오해

```
개발자 예상:  총 8bit
실제:         12bit + Padding 존재
    ↓
프로토콜 오류
```

### 하드웨어 제어 오류

레지스터 이미지가 잘못 생성되면:

- 장치 동작 실패
- 인터럽트 오동작
- 통신 실패

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
Bit Field Image  ← 최종 결과물
```

### 단계별 예시

```
Field A:  Width=3,  Offset=0,  Value=5
Field B:  Width=2,  Offset=3,  Value=2
```

**인코딩**

```
A = 5  →  101
B = 2  →  10
```

**배치**

```
10 | 101
↓
10110
```

**최종 저장**

```
00010110
```

이 전체 패턴이 **Bit Field Image**입니다.

---

### 세 개념의 역할 구분

| 개념 | 역할 |
|------|------|
| Bit Field Layout | 배치 규칙 |
| Bit Field Encoding | 값 삽입 과정 |
| Bit Field Image | 삽입 완료 결과물 |

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

직접 관측 가능한 경로:

- `/proc`
- `/sys`
- 내부 커널 자료구조

### 디버깅 도구

```bash
gdb
objdump
hexdump
xxd
```

메모리 이미지를 직접 확인할 수 있습니다.

### 네트워크

```bash
tcpdump
wireshark
```

패킷 헤더 이미지를 확인할 수 있습니다.

### Kubernetes

직접 보이지는 않지만 아래 계층에서 지속적으로 사용됩니다:

- `Kernel`
- `Network Stack`
- `eBPF`
- `Container Runtime`
- `NIC Driver`

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*