# 병렬 논리 연산 (Parallel Logical Operation)

> 정독: 0회

## 1. 이 기술이 무엇인가

병렬 논리 연산(Parallel Logical Operation)은:

> CPU가 하나의 워드(Word) 안에 있는 여러 비트를 서로 독립적으로 동시에 처리하는 연산 방식

대표적인 연산은 다음과 같습니다.

- AND
- OR
- XOR
- NOT

이 연산들은 각 비트 위치(Bit Index)를 독립적으로 계산합니다.

예를 들어:

```
  10110010
& 11001011
```

Bit 0 ↔ Bit 0, Bit 1 ↔ Bit 1, ..., Bit 7 ↔ Bit 7이 각각 독립적으로 계산됩니다.

---

## 2. 시스템 어디에서 등장하는가

병렬 논리 연산은 거의 모든 시스템 계층에서 사용됩니다.

**CPU**
- ALU, Control Register, Status Register

**운영체제**
- 프로세스 상태 플래그, 페이지 권한 비트, 인터럽트 마스크

**네트워크**
- TCP Flag, IP Header, NIC Register

**스토리지**
- Filesystem Metadata, Permission Bit, Block Allocation Bitmap

**하드웨어 제어**
- PCIe Device, GPU Register, DMA Controller, NIC Controller

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 자원은 **CPU**입니다. 병렬 논리 연산이 ALU 내부에서 수행되는 순수 CPU 연산이기 때문입니다.

간접적으로는 **Memory** 영향도 큽니다. 특히 다음 영역에서 메모리 사용량을 크게 줄일 수 있습니다.

- 플래그 압축
- 비트맵(Bitmap)
- 권한 정보 저장
- 상태 저장

---

## 4. 왜 중요한가

병렬 논리 연산은 CPU가 제공하는 가장 기본적이고 가장 빠른 데이터 처리 방식 중 하나입니다.

예를 들어 32개 또는 64개의 상태 플래그를 관리할 때, 각 플래그를 개별 변수로 관리하는 대신 **1개의 워드**에 압축하여 저장할 수 있습니다.

그 결과 읽기, 쓰기, 비교, 필터링을 한 번에 수행할 수 있습니다.

운영체제, 네트워크 스택, 장치 드라이버, CPU 제어 로직이 모두 이 원리를 활용합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 잘못된 마스크 사용

권한 비트 제거를 해야 하는데 잘못된 마스크를 사용하면 정상 권한까지 제거될 수 있습니다.

### 플래그 해석 오류

TCP Flag, Interrupt Flag, Device Flag를 잘못 해석하면 연결 실패, 장치 오동작, 인터럽트 폭주가 발생할 수 있습니다.

### 비트맵 손상

운영체제의 메모리 비트맵, 블록 비트맵, CPU 마스크가 손상되면 자원 할당 오류와 데이터 손상으로 이어질 수 있습니다.

### 하드웨어 제어 오류

레지스터의 특정 비트를 잘못 설정하면 NIC 비활성화, DMA 중단, 장치 Reset 같은 문제가 발생할 수 있습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

현재까지 정리한 개념을 연결하면 다음과 같습니다.

```
Bit Index
↓
Bit Mask
↓
Bitwise Operation
↓
Parallel Logical Operation
↓
Bit Test / Bit Extraction / Bit Manipulation
```

핵심은 **각 비트가 독립적으로 계산된다**는 것입니다.

예를 들어:

```
  10110110
& 11110000
----------
  10110000
```

각 비트 위치에서 `1 AND 1`, `0 AND 1`, `1 AND 1`, ... 이 동시에 계산됩니다. **Bit 0 결과가 Bit 1 결과에 영향을 주지 않습니다.**

### 산술 연산과의 비교

| 구분 | 연산 | 특징 |
|---|---|---|
| **산술 연산** | ADD, SUB, MUL, DIV | Carry / Borrow 전파 존재 |
| **병렬 논리 연산** | AND, OR, XOR, NOT | Carry 없음, Borrow 없음 |

즉, **비트 간 의존성 없음**이 가장 중요한 특징입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

직접 "병렬 논리 연산이 실행 중"이라고 보이지는 않습니다. 하지만 다음 영역에서 활용됩니다.

### Linux

```bash
ls -l          # 파일 권한 확인
chmod          # 권한 비트 설정
taskset        # CPU 마스크
/proc/interrupts  # 인터럽트 마스크
numactl        # NUMA / CPU Affinity
```

### 네트워크

```bash
tcpdump
wireshark      # TCP 플래그 해석
```

### Kubernetes

Node Affinity, CPU Set, NUMA Pinning, Container Resource Mask 등 내부 구현에서 활용됩니다.

### 하드웨어

```bash
lspci -vv
ethtool        # 장치 플래그와 기능 비트 확인
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*