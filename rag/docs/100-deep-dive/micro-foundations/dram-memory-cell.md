# DRAM 메모리 셀 (DRAM Memory Cell)
## Micro Foundations — 메모리 최하단 물리 저장 단위 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

DRAM 메모리 셀은:

> **컴퓨터가 단 1비트(0 또는 1)를 저장하기 위한 가장 작은 물리 메모리 소자**

핵심은 **전하(Electron)를 저장하느냐, 비우느냐**이다.

| 구성 요소 | 역할 |
|-----------|------|
| Transistor | 스위치 |
| Capacitor | 전하 저장 |

> **1T1C (1 Transistor + 1 Capacitor) 구조**

---

## 2. 시스템 어디에서 등장하는가

모든 메인 메모리(RAM)의 실제 물리 기반이다.

| 계층 | 역할 |
|------|------|
| DIMM RAM | 실제 DRAM 칩 |
| Server Memory | 대용량 메모리 |
| NUMA Node | CPU 인접 DRAM |
| VM Memory | 결국 DRAM 위 |
| Page Cache | 결국 DRAM 위 |

> **운영체제가 관리하는 모든 메모리는 최종적으로 DRAM 셀 위에 존재**한다.

---

## 3. 어떤 자원에 가장 영향이 큰가

압도적으로 **Memory**이다.

특히 다음과 직결된다:

- memory latency
- bandwidth
- cache miss penalty
- NUMA access

CPU가 아무리 빨라도 **DRAM 접근이 느리면 전체 시스템이 느려진다.**

---

## 4. 왜 중요한가

현대 시스템 병목 상당수는 CPU 연산이 아니라 **메모리 접근 지연**이다.

- cache miss
- random memory access
- page fault
- NUMA remote access

CPU 내부는 ns 수준인 반면, DRAM 접근은 훨씬 느리고 disk/network는 더 느리다.

> **DRAM은 CPU와 외부 세계 사이의 핵심 병목 지점**이다.

---

## 5. 실제 장애와의 관련성

### 1) Memory Pressure
DRAM 부족 시 → **swap 발생, page reclaim, OOM killer** 가능.

### 2) NUMA Latency
원격 NUMA DRAM 접근 시 → **latency 증가, throughput 감소, tail latency 증가** 가능.

### 3) ECC Error
DRAM 셀 전하 오류 시 → **bit flip, data corruption, kernel panic** 가능.

### 4) Refresh 병목
대용량 DRAM에서 refresh overhead 증가 → **memory stall 증가** 가능.

### 5) Rowhammer
반복 접근으로 인접 셀 전하 간섭 발생 시 → **bit corruption, privilege escalation** 가능.

> **핵심:** DRAM 셀 안정성은 **시스템 신뢰성의 핵심**이다.

---

## 6. 핵심 메커니즘

### A. Capacitor가 실제 비트 저장

| 상태 | 의미 |
|------|------|
| 전하 있음 | 1 |
| 전하 없음 | 0 |

> **DRAM은 전하 저장 장치**이다.

### B. Transistor는 문 역할

Transistor는 **전하 흐름 제어 스위치**이다.
wordline이 켜지면 transistor가 열려 **capacitor ↔ bitline** 이 연결된다.

### C. Wordline / Bitline

| 신호선 | 역할 |
|--------|------|
| Wordline | 어느 행(row)을 열 것인가 제어 |
| Bitline | 실제 데이터가 흐르는 선 |

> **wordline = 선택 / bitline = 데이터 이동**

### D. Read는 Destructive

DRAM 읽기는 **읽는 순간 전하 일부가 빠져나간다.**
따라서 읽은 뒤 반드시 **다시 써줘야** 한다. 이것이 refresh가 필요한 이유이다.

### E. Refresh 필수

DRAM은 dynamic memory이다. 가만히 두면 **전하가 자연 누설(leakage)** 된다.
따라서 **주기적으로 다시 충전(refresh)** 해야 한다.

> **DRAM은 유지 비용이 필요한 메모리**이다.

### F. Cache와의 관계

CPU는 DRAM이 느리기 때문에 **L1 / L2 / L3 cache**를 사용한다.

```
CPU Register → L1 → L2 → L3 → DRAM
```

> **DRAM latency를 숨기기 위해 cache hierarchy가 존재**한다.

### G. Page의 물리 기반

운영체제 page(4KB)는 결국 **수많은 DRAM 셀의 묶음**이다.
virtual memory도 결국 **물리 DRAM 셀 위에서 동작**한다.

---

## 7. Linux / Runtime / K8s에서의 관측

### Linux 기본 도구

```bash
free -h               # 메모리 사용량 확인
vmstat                # 메모리 및 swap 상태
numactl --hardware    # NUMA 구성 확인
```

### NUMA 분석

```bash
numastat    # 원격 DRAM access 확인
```

### Memory Pressure

```bash
/proc/meminfo    # 상세 메모리 상태
sar -B           # 페이지 활동 통계
```

### ECC Error

```bash
dmesg        # 하드웨어 오류 로그
edac-util    # ECC 오류 확인
```

### Kubernetes

대표적인 증상:

- OOMKilled
- memory throttling
- eviction
- page cache reclaim

### Performance 분석

```bash
perf stat    # cache miss / memory stall 관측
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*