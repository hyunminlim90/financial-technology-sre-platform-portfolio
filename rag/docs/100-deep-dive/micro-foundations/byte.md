# 바이트 (Byte)

> 정독: 0회

바이트(Byte)는:

> **컴퓨터가 데이터를 저장·전송·주소 지정할 때 사용하는 기본 데이터 단위**

**핵심:** 1 Byte = 8 Bit

즉, 비트(Bit)를 실제로 의미 있게 묶어서 사용하는 **최소 실용 단위**입니다.

---

## 1. 이 기술이 무엇인가

비트(Bit)는 0 또는 1, 단 하나만 표현 가능합니다.

하지만 실제 시스템은 문자, 숫자, 메모리 주소, 명령어, 파일, 패킷 등을 처리해야 합니다.

그래서 컴퓨터는 **8개의 Bit를 묶어 1 Byte**라는 표준 단위를 만들었습니다.

즉, **Byte는 컴퓨터의 실질적인 데이터 처리 기본 단위**입니다.

### 핵심 표현 범위

8비트는 2⁸ = **256가지 상태** 표현 가능합니다. 즉, **0 ~ 255** 범위를 표현합니다.

---

## 2. 시스템 어디에서 등장하는가

Byte는 사실상 컴퓨터 시스템 전체에 등장합니다.

대표 영역:

- CPU Register
- Cache Line
- DRAM
- SSD
- Network Packet
- File System
- Process Address Space
- Virtual Memory / Page
- Instruction Encoding
- DMA / TCP/IP

즉, **모든 데이터 유통의 기본 규격**입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향 |
|------|------|
| Memory | 매우 큼 |
| Disk | 매우 큼 |
| Network | 매우 큼 |
| CPU Cache | 매우 큼 |

데이터 이동 자체가 Byte 기반이기 때문에 **모든 자원에 직접 영향**을 줍니다.

---

## 4. 왜 중요한가

컴퓨터는 모든 데이터를 **Byte 단위로 저장·전송·주소 지정**합니다.

메모리 주소, 캐시 전송, 파일 크기, 네트워크 패킷 모두 Byte 기준입니다.

- **캐시 라인:** 보통 64 Byte 단위 이동
- **가상 페이지:** 보통 4KB = 4,096 Byte
- **메모리 용량:** 16GB RAM도 결국 Byte 총량

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. Memory Overflow

Byte 계산 오류 시 **버퍼 초과(Buffer Overflow)** 가 발생할 수 있습니다. 대표적인 보안 취약점입니다.

### 5-2. Cache Miss 증가

데이터 구조가 Cache Line 경계를 비효율적으로 사용하면 **CPU Stall**이 증가할 수 있습니다.

### 5-3. Network MTU 초과

패킷 Byte 크기 초과 시 **Fragmentation**이 발생합니다.

### 5-4. Disk I/O 증폭

불필요한 작은 Byte 단위 쓰기 반복 시 **I/O Amplification**이 발생할 수 있습니다.

### 5-5. Page Fault 증가

메모리 접근 패턴이 비효율적이면 **불필요한 Page Loading**이 증가합니다.

### 5-6. Integer Overflow

Byte 계산 시 **32bit / 64bit overflow** 문제가 발생할 수 있습니다.

---

## 6. 핵심 메커니즘

### 6-1. 컴퓨터는 Byte 단위로 주소를 지정한다

메모리 주소는 Byte 기준입니다.

```
0x1000  →  1 Byte 위치
0x1001  →  1 Byte 위치
0x1002  →  1 Byte 위치
```

### 6-2. Cache도 Byte 단위로 이동한다

CPU는 보통 **64 Byte Cache Line** 단위로 데이터를 이동합니다.

즉, 1 Byte를 읽어도 실제로는 64 Byte가 이동할 수 있습니다.

### 6-3. Page도 Byte 기반이다

대표 단위: **4KB Page = 4 × 1,024 = 4,096 Byte**

### 6-4. 문자도 결국 Byte다

ASCII 기준으로 `'A'`도 1 Byte에 저장됩니다.

### 6-5. CPU Register도 Byte 크기를 가진다

64-bit Register = 64 ÷ 8 = **8 Byte** 저장 가능

### 6-6. 모든 저장 장치는 Byte 총량이다

1TB SSD도 결국 엄청난 Byte 저장 공간입니다.

### 6-7. 네트워크도 Byte 단위로 전송한다

TCP/IP 패킷도 **Byte 배열**입니다.

### 6-8. 운영체제 메모리 관리도 Byte 기반이다

운영체제는 Page, Segment, Heap, Stack 등을 결국 **Byte 범위**로 관리합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 메모리 단위 확인
free -h

# 프로세스 메모리 (RSS / VSZ 모두 Byte 기반)
ps aux
top
htop

# 파일 크기
ls -lh
du -sh

# 블록 장치
lsblk

# 네트워크 Byte 통계
ifconfig
ip -s link
sar -n DEV
```

### Runtime

주요 지표 (모두 Byte 기반):

- Heap Size
- RSS
- Buffer Size
- Cache Size
- Allocation Rate

### Kubernetes

```bash
# 컨테이너 메모리 사용량
kubectl top pod
```

```yaml
# Pod 메모리 제한
resources:
  limits:
    memory: "2Gi"
```

> **OOMKilled** — 메모리 Byte 제한 초과 시 발생

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*