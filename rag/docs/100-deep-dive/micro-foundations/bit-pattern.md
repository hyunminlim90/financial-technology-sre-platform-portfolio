# 비트 패턴 (Bit Pattern)

> 정독: 0회

## 1. 이 기술이 무엇인가

비트 패턴(Bit Pattern)은:

> 메모리, 레지스터, 캐시, 디스크 등에 저장된 데이터의 실제 이진 표현

```
논리적 데이터
↓
인코딩
↓
0과 1의 배열
```

결과가 비트 패턴입니다.

예를 들어 정수 `5`는 `00000101`이라는 비트 패턴으로 표현될 수 있습니다.

중요한 점은 **비트 패턴 자체에는 의미가 없다**는 것입니다. 의미는 해석 규칙에 의해 결정됩니다.

---

## 2. 시스템 어디에서 등장하는가

비트 패턴은 컴퓨터의 모든 계층에서 등장합니다.

**CPU**
- 레지스터 값
- 명령어 코드
- 상태 플래그

**메모리**
- 정수, 실수, 포인터, 구조체, 객체

**네트워크**
- IP Header, TCP Header, TLS Record, HTTP Frame

**디스크**
- 파일, 메타데이터, 파일시스템 구조

**장치 제어**
- 장치 레지스터, DMA Descriptor, Control Block

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 자원은 **Memory**입니다. 비트 패턴은 결국 메모리나 레지스터에 저장되는 데이터의 실제 형태이기 때문입니다.

하지만 실질적으로는 CPU, Memory, Network, Disk 모든 자원과 관련됩니다. 컴퓨터가 처리하는 모든 데이터가 비트 패턴으로 존재하기 때문입니다.

---

## 4. 왜 중요한가

CPU는 정수, 문자, 실수, 객체, 패킷을 직접 이해하지 않습니다. CPU가 실제로 보는 것은 **비트 패턴**뿐입니다.

예를 들어 `01000001`이라는 동일한 비트 패턴도 해석 방식에 따라 다르게 읽힙니다.

| 해석 방식 | 결과 |
|---|---|
| ASCII | `A` |
| Unsigned Integer | `65` |
| Instruction | 특정 명령어 |

따라서

```
데이터 = 비트 패턴 + 해석 규칙
```

---

## 5. 실제 장애와 어떤 관련이 있는가

### Bit Flip

메모리 오류로 `00000101`이 `00000100`으로 바뀌면 데이터 의미가 완전히 달라집니다.

### 엔디언 문제

시스템 A(Little Endian)와 시스템 B(Big Endian) 간에 동일한 값이 서로 다른 비트 패턴으로 해석될 수 있습니다.

### ABI 불일치

구조체 레이아웃이 다르면 동일한 메모리 영역도 다른 데이터로 해석됩니다.

### 프로토콜 오류

```
패킷 헤더 비트 패턴 해석 실패
↓
통신 실패
```

### 명령어 손상

```
실행 파일 내부 비트 패턴 손상
↓
프로세스 크래시
↓
Illegal Instruction
```

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

현재까지 정리한 개념을 연결하면 다음과 같습니다.

```
Data Field
↓
Bit Field Member Value
↓
Bit Field Encoding
↓
Bit Field Image Data
↓
Bit Pattern
↓
CPU Register
↓
CPU Interpretation
```

예를 들어 논리적 데이터 `mode = 5`가 있다고 가정합니다.

```
인코딩:       5 → 101
레이아웃 적용: 00000101
최종 저장:    00000101
```

이 순간 `00000101`이 바로 **Bit Pattern**입니다.

CPU는 이후 다음 과정을 수행합니다.

```
Load
↓
Register
↓
Bitwise Operation
↓
Arithmetic Operation
```

즉, **비트 패턴 = CPU가 실제로 처리하는 데이터**입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### 메모리 확인

```bash
hexdump
xxd
od
```

### 바이너리 분석

```bash
objdump
readelf
nm
```

### 디버거 (gdb)

```bash
x/16xb   # 메모리의 실제 비트 패턴 확인 가능
```

### 네트워크

```bash
tcpdump
wireshark   # 패킷 내부 비트 패턴 확인 가능
```

### Kubernetes

직접 관측되지는 않지만 Container Image, ELF Binary, Network Packet, Kernel Object 모두 비트 패턴으로 저장됩니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*