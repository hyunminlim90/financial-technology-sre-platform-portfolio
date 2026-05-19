# 메모리 관리 장치 (Memory Management Unit / MMU)

> 정독: 0회

메모리 관리 장치(MMU)는:

> **CPU가 사용하는 가상 주소를 실제 DRAM 물리 주소로 변환하고, 메모리 접근 권한과 보호를 하드웨어 레벨에서 통제하는 CPU 내부 제어 장치**

**쉽게 말하면:**
"CPU와 DRAM 사이의 실시간 주소 번역기 + 보안 관문"

현대 컴퓨터에서는 **CPU가 메모리에 접근할 때 거의 반드시 MMU를 거친다**고 보면 됩니다.

<details>
<summary>Deep Dive</summary></br>

Virtual Address(가상 주소) [[M]](../../100-deep-dive/micro-foundations/virtual-address.md)  
Physical Address(물리 주소) [[M]](../../100-deep-dive/micro-foundations/physical-address.md)  

</details></br>

## 1. 이 기술이 무엇인가

현대 프로그램은 실제 DRAM 주소를 직접 사용하지 않습니다. 대신 **Virtual Address(가상 주소)** 를 사용합니다.

이때 MMU가 `Virtual Address → Physical Address` 변환을 수행합니다.

즉, **CPU 연산 세계 ↔ 실제 DRAM 세계**를 연결하는 하드웨어 브리지입니다.

### MMU 핵심 역할

1. 주소 변환
2. 메모리 보호
3. 프로세스 격리
4. Page Fault 감지
5. 접근 권한 검사

---

## 2. 시스템 어디에서 등장하는가

MMU는 현대 운영체제 기반 시스템 거의 전체에서 등장합니다.

대표 영역:

- 프로세스 실행
- 가상 메모리
- 페이지 테이블
- 컨텍스트 스위칭
- mmap / 파일 매핑
- 공유 메모리
- 컨테이너 / VM / Hypervisor
- NUMA

즉, **현대 OS 메모리 구조의 핵심 하드웨어**입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향도 |
|------|--------|
| Memory | 절대적 |
| CPU | 매우 큼 |
| Cache | 매우 큼 |
| Disk | 큼 |
| Network | 간접 영향 |

특히 **주소 변환 속도**, **TLB 효율**, **Page Fault**에 직접 영향을 줍니다.

---

## 4. 왜 중요한가

MMU가 없다면 모든 프로그램이 실제 DRAM 주소에 직접 접근해야 합니다.

그러면 메모리 침범, 보안 붕괴, 커널 오염, 프로세스 충돌이 발생할 수 있습니다.

MMU는 **"가상 메모리 세계를 실제 물리 메모리로 안전하게 연결"** 합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. TLB Miss

MMU는 주소 변환 캐시인 **TLB**를 사용합니다.

TLB Miss 증가 시 Page Table 탐색이 증가합니다.

**결과:** 메모리 접근 지연 증가

### 5-2. Page Fault

MMU가 해당 페이지가 존재하지 않음을 감지하면 **Page Fault Exception**이 발생하고 OS가 개입합니다.

심하면 디스크 I/O가 발생합니다.

### 5-3. Segmentation Fault

프로세스가 허용되지 않은 메모리 접근을 시도하면 MMU가 차단합니다.

**결과:** `SIGSEGV` (Segmentation Fault) 발생

### 5-4. Swap Thrashing

DRAM 부족으로 Page Fault가 폭증하면 OS가 계속 Swap을 수행합니다.

**결과:** 시스템 전체 멈춤 수준의 지연 가능

### 5-5. Context Switch 비용

프로세스 변경 시 Page Table이 교체되어 **TLB Flush**가 발생할 수 있습니다.

즉, **주소 변환 캐시 무효화**입니다.

### 5-6. NUMA Remote Access

MMU가 매핑한 페이지가 다른 NUMA Node에 존재하면 **Remote Memory Access**가 발생합니다.

**결과:** Latency 증가

---

## 6. 핵심 메커니즘

### 6-1. CPU는 가상 주소를 사용한다

프로세스는 Physical Address를 알지 못하며, 항상 **Virtual Address**를 사용합니다.

### 6-2. MMU가 실시간 주소 변환을 한다

MMU는 `Virtual Page Number → Physical Frame Number` 변환을 수행합니다.

즉, **가상 메모리 세계 ↔ 실제 DRAM 연결**입니다.

### 6-3. Page Table을 참조한다

주소 변환의 기준은 **Page Table**이며, OS Kernel이 관리합니다.

### 6-4. TLB는 MMU 내부 주소 캐시다

주소 변환은 매우 비쌉니다. 그래서 MMU 내부에 **TLB**라는 SRAM 기반 초고속 주소 변환 캐시가 존재합니다.

### 6-5. MMU가 메모리 보호를 수행한다

페이지마다 **R/W/X 권한**이 존재하며 MMU가 검사합니다.

읽기 전용 페이지에 쓰기를 시도하면 예외가 발생합니다.

### 6-6. MMU가 Page Fault를 발생시킨다

필요한 페이지가 DRAM에 없으면:

```
MMU → CPU Exception → Kernel 진입
```

이후 OS가 `SSD → DRAM` 데이터를 로딩합니다.

### 6-7. MMU는 프로세스 격리의 핵심이다

각 프로세스는 **독립 Page Table**을 가집니다.

따라서 **다른 프로세스의 메모리에 직접 접근할 수 없습니다.**

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 페이지 정보
cat /proc/meminfo

# 프로세스 메모리 매핑
cat /proc/<pid>/maps

# Page Fault 확인
vmstat 1
# 또는
sar -B

# HugePage 상태
cat /proc/meminfo | grep Huge

# NUMA 상태
numastat

# TLB 관련 CPU 정보
lscpu

# MMU 관련 Kernel 로그
dmesg
```

### Runtime

주요 관측 대상:

- TLB Miss
- Page Fault
- Minor / Major Fault
- Virtual Memory Size
- Resident Set Size
- Working Set

### Kubernetes

K8s도 결국 **Linux MMU 기반**으로 동작합니다.

```bash
# OOMKilled 확인
kubectl describe pod
```

```yaml
# 메모리 제한
resources:
  limits:
    memory: "2Gi"

# HugePages
resources:
  limits:
    hugepages-2Mi: 1Gi
```

> **NUMA / CPU Pinning** — 고성능 워크로드에서 중요

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*