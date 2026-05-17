# 가상 페이지 (Virtual Page)

> 정독: 0회

가상 페이지(Virtual Page)는:

> **운영체제와 CPU MMU가 프로세스 메모리를 안전하고 효율적으로 관리하기 위해 사용하는 고정 크기(보통 4KB)의 가상 메모리 블록**

**쉽게 말하면:**
"프로세스가 사용하는 가상의 메모리 조각 단위"

프로세스는 실제 DRAM 위치를 직접 알지 못하며, 대신 **가상 페이지 주소**를 사용합니다.

<details>
<summary>Deep Dive</summary></br>

Operating System(운영 체제) [[M]](../../100-deep-dive/micro-foundations/operating-system.md)  
Memory Management Unit(메모리 관리 장치) [[M]](../../100-deep-dive/micro-foundations/memory-management-unit.md)  
Process Address Space(프로세스 주소 공간) [[M]](../../100-deep-dive/micro-foundations/process-address-space.md)  
Byte(바이트) [[M]](../../100-deep-dive/micro-foundations/byte.md)  
Virtual Memory(가상 메모리) [[M]](../../100-deep-dive/micro-foundations/)  

</details></br>

## 1. 이 기술이 무엇인가

현대 컴퓨터는 실제 DRAM 주소를 프로그램에 직접 노출하지 않습니다. 대신 **가상 메모리(Virtual Memory)** 를 제공하며, 이 가상 메모리를 일정 크기(보통 4KB)로 나눈 최소 관리 단위가 **Virtual Page**입니다.

### 핵심 구조

```
Virtual Address Space
↓
Virtual Page
↓
Page Table
↓
Physical Page Frame
↓
DRAM
```

---

## 2. 시스템 어디에서 등장하는가

가상 페이지는 현대 운영체제의 거의 모든 메모리 관리에서 등장합니다.

대표 사례:

- 프로세스 실행
- 힙 / 스택 메모리
- mmap
- 파일 캐시
- 공유 메모리
- 페이지 캐시
- 컨테이너 메모리
- Swap

즉, **프로세스 메모리 관리의 핵심 단위**입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향도 |
|------|--------|
| Memory | 절대적 |
| CPU | 매우 큼 |
| Disk | 매우 큼 |
| Cache | 간접 영향 |
| Network | 간접 영향 |

특히 **메모리 주소 변환**과 **Page Fault**에 핵심입니다.

---

## 4. 왜 중요한가

가상 페이지가 없으면 프로세스가 실제 DRAM에 직접 접근해야 합니다.

그러면 보안 위험, 메모리 충돌, 단편화, 프로세스 침범이 발생할 수 있습니다.

가상 페이지는 이를 해결하여 **각 프로세스에게 독립 메모리 세계를 제공**합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. Page Fault

프로세스가 접근한 가상 페이지가 현재 DRAM에 없으면 **Page Fault**가 발생합니다.

**결과:** Disk I/O 발생 가능

### 5-2. Swap 폭증

DRAM 부족 시 페이지를 디스크로 내보냅니다(**Swap Out**).

과도하면 시스템이 매우 느려집니다.

### 5-3. TLB Miss 증가

주소 변환 캐시(TLB)에 없으면 Page Table 탐색이 필요합니다.

**결과:** 메모리 접근 지연 증가

### 5-4. NUMA 문제

페이지가 다른 NUMA Node의 DRAM에 존재하면 **Remote Memory Access**가 발생합니다.

**결과:** Latency 증가

### 5-5. Huge Memory Workload 문제

메모리 페이지 수가 너무 많으면 Page Table 자체 비용이 증가합니다.

**결과:** TLB Pressure 증가

---

## 6. 핵심 메커니즘

### 6-1. 프로세스는 가상 주소만 본다

프로세스는 실제 DRAM 주소를 알지 못하며, 항상 **Virtual Address**를 사용합니다.

### 6-2. MMU가 주소를 변환한다

CPU 내부 MMU가 `Virtual Page → Physical Page Frame` 변환을 수행합니다.

즉, **가상 주소 번역기**입니다.

### 6-3. Page Table이 매핑 지도다

Page Table은 "가상 페이지가 실제 어디 있는지"를 기록합니다.

```
Virtual Page 10  →  Physical Frame 200
```

### 6-4. Physical Frame은 실제 DRAM 조각이다

실제 데이터 저장 위치는 **Physical Page Frame**, 즉 DRAM 내부의 실제 저장 블록입니다.

### 6-5. 페이지 크기는 보통 4KB다

표준 시스템 대부분은 **4KB Page**를 사용합니다.

하지만 **HugePage**는 2MB / 1GB도 사용 가능합니다.

### 6-6. TLB는 주소 변환 캐시다

주소 변환은 비싸므로 최근 Page Table 결과를 캐싱합니다. 이 캐시가 **TLB**입니다.

### 6-7. Virtual Page는 격리의 핵심이다

각 프로세스는 자기만의 가상 주소 공간을 가집니다.

따라서 **다른 프로세스의 메모리에 직접 접근할 수 없습니다.**

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 페이지 크기 확인
getconf PAGE_SIZE

# 메모리 매핑 확인
cat /proc/<pid>/maps

# 페이지 통계
cat /proc/meminfo

# Page Fault 확인
vmstat 1
# 또는
sar -B

# NUMA 페이지 상태
numastat

# HugePage 확인
cat /proc/meminfo | grep Huge
```

### Runtime

주요 관측 포인트:

- Page Fault
- Minor Fault
- Major Fault
- TLB Miss
- Working Set
- Swap Usage
- NUMA Access

### Kubernetes

```bash
# Pod 메모리 사용량
kubectl top pod

# OOMKilled 확인
kubectl describe pod
```

```yaml
# HugePages 설정
resources:
  limits:
    hugepages-2Mi: 1Gi
```

> **NUMA / CPU Affinity** — 고성능 워크로드에서 중요

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*