# 논리 주소 (Logical Address)

> 정독: 0회

## 1. 이 기술이 무엇인가

논리 주소는:

> 프로세스와 CPU가 사용하는 가상 메모리 주소

실제 RAM 물리 위치가 아닙니다. 프로그램은 실제 DRAM 위치를 직접 모르며, 자신만의 독립 주소 공간만 보고, 0번지부터 시작하는 연속 공간처럼 인식합니다.

| 종류 | 의미 |
|------|------|
| 논리 주소 | 프로그램 관점 주소 |
| 물리 주소 | 실제 RAM 칩 주소 |

현대 시스템 대부분은 가상 메모리 기반 논리 주소 체계 위에서 동작합니다.

---

## 2. 시스템 어디에서 등장하는가

거의 모든 실행 환경에서 등장합니다.

| 계층 | 역할 |
|------|------|
| CPU | virtual address 생성 |
| MMU | 주소 변환 |
| OS kernel | page table 관리 |
| process runtime | 독립 주소 공간 사용 |
| allocator | virtual memory 기반 allocation |
| filesystem cache | virtual mapping |
| container runtime | process isolation |

> 프로세스 메모리 격리의 핵심 기반

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**가장 영향이 큰 자원: CPU + Memory subsystem**

| 요소 | 영향 |
|------|------|
| MMU | address translation |
| TLB | translation cache |
| page table | mapping lookup |
| RAM | physical access |
| CPU pipeline | memory latency |

주소 변환 실패 시: page fault / TLB miss / memory stall 발생 가능

---

## 4. 왜 중요한가

프로세스 독립성과 메모리 보호를 제공하기 위해 필요합니다.

| 문제 | 결과 |
|------|------|
| 프로세스 충돌 | memory corruption |
| 주소 재배치 불가 | swapping 불가능 |
| 보호 실패 | arbitrary overwrite |
| 멀티태스킹 붕괴 | system instability |

논리 주소 체계 덕분에 프로세스별 독립 공간 / 메모리 보호 / demand paging / swapping / shared memory / copy-on-write가 가능해집니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

| 문제 | 원인 |
|------|------|
| Segmentation Fault | invalid logical address |
| Page Fault Storm | excessive translation miss |
| TLB Miss 증가 | poor locality |
| OOM | virtual memory exhaustion |
| swap thrashing | excessive paging |
| kernel panic | invalid kernel mapping |

고성능 시스템에서는 **TLB 효율과 page locality**가 성능 핵심입니다.

**대표 현상:** latency spike / CPU stall / major page fault 증가 / swap I/O 폭증 / cache miss 증가

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 프로세스별 독립 주소 공간

각 프로세스는 자신만의 가상 주소 공간을 가집니다.

| 프로세스 | 논리 주소 0x1000 |
|----------|-----------------|
| A | 물리주소 X |
| B | 물리주소 Y |

논리 주소는 같아도 실제 물리 주소는 다를 수 있습니다.

### CPU는 논리 주소 생성

프로그램 실행 중 CPU instruction이 논리 주소를 생성합니다.

```
MOV RAX, [0x1000]
```

여기서 `0x1000`이 논리 주소입니다.

### MMU 주소 변환

| 단계 | 내용 |
|------|------|
| virtual address input | CPU 생성 |
| page table lookup | mapping 탐색 |
| physical address 생성 | RAM 접근 |

### Page 기반 변환

논리 주소는 보통 page number + offset으로 분리됩니다.

```
Virtual Address = VPN + Offset
```

MMU는 VPN → PFN 변환을 수행합니다.

### TLB

TLB는 주소 변환 캐시입니다. TLB가 없다면 모든 memory access마다 page table traversal이 필요합니다.

TLB miss가 많아지면: CPU stall / latency 증가 / throughput 감소 발생

### Page Fault

논리 주소 접근 시 해당 page가 RAM에 없는 상태면 page fault가 발생합니다.

```
page fault 발생
→ OS가 disk/swap에서 page 적재
→ page table 수정
→ instruction 재실행
```

### Memory Protection

각 page에는 권한이 존재합니다.

| 권한 | 의미 |
|------|------|
| Read | 읽기 |
| Write | 쓰기 |
| Execute | 실행 |

권한 위반 시 Segmentation Fault가 발생합니다.

### Shared Mapping

여러 프로세스가 동일 physical page를 공유할 수 있습니다.

예: shared library / mmap / shared memory IPC

### Copy-On-Write

초기에는 동일 physical memory를 공유하다가, 쓰기 발생 시 새 physical page를 복사합니다. fork 최적화의 핵심입니다.

### Address Space Layout

| 영역 | 역할 |
|------|------|
| text | code |
| data | global/static |
| heap | dynamic allocation |
| stack | function frame |

모두 논리 주소 공간 내부에 존재합니다.

### ASLR

프로세스마다 주소 배치를 랜덤화합니다. buffer overflow exploit 방어에 중요합니다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

```bash
# virtual memory 매핑 확인
cat /proc/<pid>/maps

# 상세 virtual memory 통계
cat /proc/<pid>/smaps

# page fault
vmstat
sar -B

# TLB/cache 분석
perf stat
perf top

# virtual memory 상태
free -h
```

### Runtime

| 현상 | 관련 |
|------|------|
| heap growth | virtual mapping 증가 |
| stack expansion | stack page 추가 |
| mmap allocation | file-backed mapping |
| GC compaction | logical remapping |

### Kubernetes

| 현상 | 관련 |
|------|------|
| container memory limit | cgroup memory |
| OOMKill | virtual memory pressure |
| swap issue | page reclaim |
| mmap-heavy workload | page cache pressure |

**memory overcommit + page reclaim** 조합이 매우 중요합니다.

### Observability

| 도구 | 목적 |
|------|------|
| pmap | process mapping |
| perf | TLB/cache 분석 |
| eBPF | page fault tracing |
| vmstat | memory pressure |
| sar | paging 분석 |

실무에서는 **major page fault 증가**가 매우 중요한 장애 신호입니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*