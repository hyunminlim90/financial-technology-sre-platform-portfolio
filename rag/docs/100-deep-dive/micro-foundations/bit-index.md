# 비트 인덱스 (Bit Index)

> 정독: 0회

## 1. 이 기술이 무엇인가

비트 인덱스(Bit Index)는:

> 데이터 내부에서 개별 비트가 위치한 번호

예를 들어 8비트 데이터가 있다면 각 비트는 고유한 인덱스를 가집니다.

```
Bit Index

7  6  5  4  3  2  1  0
```

비트 인덱스는 다음을 결정합니다.

- 어느 비트를 읽을 것인가
- 어느 비트를 수정할 것인가
- 어느 비트가 어떤 의미를 가지는가

즉,

```
Bit + Position = Bit Index
```

---

## 2. 시스템 어디에서 등장하는가

비트 인덱스는 비트 단위 데이터가 존재하는 모든 곳에서 사용됩니다.

**CPU 레지스터**
- Status Register, Control Register, Flag Register

**운영체제**
- Page Table Entry, Permission Flag, Process State

**네트워크**
- IPv4 Header, TCP Header, TCP Flags

**장치 제어**
- NIC Register, Interrupt Controller, DMA Register

**파일 포맷**
- ELF Header, Filesystem Metadata, Protocol Header

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 자원은 **CPU**입니다. 비트 인덱스는 CPU가 비트 읽기, 쓰기, 검사, 변경을 수행할 때 사용되는 기준 좌표이기 때문입니다.

간접적으로 Memory, Network, Device I/O에서도 매우 중요합니다.

---

## 4. 왜 중요한가

컴퓨터는 비트 전체를 읽을 수는 있지만 "5번째 비트만 읽어라" 같은 요구도 처리해야 합니다. 이를 위해 비트 위치를 식별할 수 있어야 합니다.

예를 들어 `00010100`이 있을 때 **Bit Index 2**의 값은 `1`입니다.

비트 인덱스가 없으면 어느 비트를 검사해야 하는지, 어느 비트를 변경해야 하는지를 정의할 수 없습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 잘못된 비트 인덱스 사용

예를 들어 `Bit 3 = Enable`, `Bit 4 = Reset`인데 Bit 3 대신 Bit 4를 수정하면 Enable해야 할 장치가 Reset될 수 있습니다.

### 프로토콜 구현 오류

TCP 플래그 위치를 잘못 해석하면 ACK, SYN, FIN, RST 판단이 잘못됩니다.

### 권한 처리 오류

권한 비트 인덱스를 잘못 계산하면 읽기 가능, 쓰기 가능, 실행 가능 판단이 잘못될 수 있습니다.

### 커널/드라이버 오류

하드웨어 레지스터의 비트 위치를 잘못 사용하면 장치 오동작, 인터럽트 오류, DMA 오류가 발생할 수 있습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

지금까지 정리한 개념과 연결하면 다음 순서입니다.

```
Bit Pattern
↓
Bit Index
↓
Bit Offset
↓
Bit Mask
↓
Bit Extraction
↓
Bit Test
```

예를 들어 `00010100`이라는 비트 패턴이 있다고 가정합니다.

```
Index:  7  6  5  4  3  2  1  0
Value:  0  0  0  1  0  1  0  0
```

여기서 **Bit Index 4**를 읽고 싶다면 다음과 같이 처리합니다.

```
마스크 생성:  1 << 4  →  00010000

마스킹:  00010100
       & 00010000
       ----------
         00010000

결과:  Bit Index 4 = 1
```

즉, **Bit Index = 어느 비트를 대상으로 할 것인가를 지정하는 좌표**입니다.

### Bit Index vs Bit Offset

많은 사람이 혼동하는 개념입니다.

| 개념 | 의미 | 예시 |
|---|---|---|
| **Bit Index** | 개별 비트의 위치 번호 | Bit 0, Bit 1, Bit 2, ... |
| **Bit Offset** | 특정 데이터 필드가 시작하는 위치 | Field A, Offset=5, Width=3 |

예를 들어 `Field A`의 Offset이 5이고 Width가 3이라면 Bit Index 5, 6, 7을 사용합니다.

```
Bit Index  = 개별 비트 좌표
Bit Offset = 필드 시작 좌표
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### CPU / 디버거

```bash
gdb
info registers   # 레지스터 분석
```

### 커널

```bash
/proc/cpuinfo
```

### 네트워크

```bash
tcpdump
wireshark        # 패킷 플래그 비트 확인 가능
```

### 장치

```bash
ethtool          # NIC 기능 플래그 확인
```

### Kubernetes

직접 비트 인덱스를 보지는 않지만 Container Runtime, Kernel, eBPF, CNI, Device Driver 계층에서 사용됩니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*