# 메모리 관리자 (Memory Manager)

> 정독: 0회

## 1. 이 기술이 무엇인가

메모리 관리자는:

> 프로세스와 런타임에 메모리를 할당·추적·회수하는 시스템

| 기능 | 설명 |
|------|------|
| Allocation | 메모리 할당 |
| Mapping | 가상-물리 주소 매핑 |
| Protection | 접근 보호 |
| Reclamation | 사용 종료 메모리 회수 |
| Compaction | 파편화 완화 |
| Paging | 메모리 교체 |

> 운영체제와 런타임의 핵심 자원 관리 계층

<details>
<summary>Deep Dive</summary></br>

Process(프로세스) [[M]](../../100-deep-dive/micro-foundations/process.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

메모리 관리자는 거의 모든 실행 환경에 존재합니다.

| 계층 | 역할 |
|------|------|
| OS Kernel | virtual memory 관리 |
| MMU | address translation |
| Runtime | heap allocation |
| Allocator | malloc/free 계열 |
| GC system | garbage collection |
| Container runtime | memory isolation |
| Hypervisor | guest memory virtualization |

| 영역 | 관리 대상 |
|------|-----------|
| Stack | 함수 문맥 |
| Heap | 동적 객체 |
| Page Cache | 파일 캐시 |
| Shared Memory | IPC |
| Kernel Memory | 커널 데이터 |

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**가장 영향이 큰 자원: Memory + CPU**

| 자원 | 영향 |
|------|------|
| RAM | allocation/reclaim |
| CPU | page translation, GC |
| Disk | swap/page-in/out |
| Cache/TLB | translation overhead |

메모리 관리 실패 시: CPU stall / page fault 증가 / swap thrashing / OOM / latency spike 발생 가능

---

## 4. 왜 중요한가

유한한 메모리를 안정적으로 공유하기 위해 필요합니다.

| 문제 | 결과 |
|------|------|
| 메모리 충돌 | corruption |
| 사용 종료 메모리 누수 | exhaustion |
| 주소 보호 실패 | process contamination |
| 파편화 증가 | allocation failure |
| swap uncontrolled | severe latency |

> 성능 안정성의 상당 부분은 메모리 관리 품질에 좌우

---

## 5. 실제 장애와 어떤 관련이 있는가

| 문제 | 원인 |
|------|------|
| OOMKill | memory exhaustion |
| Memory Leak | unreleased allocation |
| Swap Thrashing | reclaim overload |
| Segmentation Fault | invalid access |
| GC Pause Spike | heap pressure |
| Allocation Failure | fragmentation |
| Kernel Panic | invalid kernel memory |

```
메모리 압박 → 전체 시스템 latency 증가
```

**대표 현상:** RSS 증가 / page fault 급증 / swap I/O 증가 / allocator contention / cache locality 악화

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 가상 메모리 기반 관리

프로세스는 독립된 논리 주소 공간을 사용합니다. 메모리 관리자는 virtual address 관리 / physical page 매핑 / page table 유지를 수행합니다.

### 메모리 할당

프로그램 요청 시 연속 메모리 블록을 확보합니다.

| 요청 | 예 |
|------|----|
| heap allocation | dynamic object |
| stack growth | function call |
| mmap | file mapping |

할당 시 Base Address / Size / Permission(RWX) 이 생성됩니다.

### Heap 관리

| 전략 | 특징 |
|------|------|
| First Fit | 빠름 |
| Best Fit | fragmentation 감소 |
| Buddy Allocator | page 관리 |
| Slab Allocator | kernel object 최적화 |

heap allocator는 빈 블록 탐색 및 재사용을 수행합니다.

### Page 기반 관리

| 종류 | 크기 |
|------|------|
| Standard Page | 4KB |
| Huge Page | 2MB / 1GB |

메모리 관리자는 page allocation / reclaim / migration / eviction을 수행합니다.

### MMU 협력

MMU는 논리 주소 → 물리 주소 변환을 수행하며, 메모리 관리자는 page table 구성 / mapping 변경 / permission 설정을 담당합니다.

### Protection

| 권한 | 의미 |
|------|------|
| Read | 읽기 |
| Write | 쓰기 |
| Execute | 실행 |

권한 위반 시 Segmentation Fault가 발생합니다.

### Fragmentation

| 종류 | 의미 |
|------|------|
| External Fragmentation | 빈 공간 분산 |
| Internal Fragmentation | block 내부 낭비 |

파편화 증가 시: allocation latency 증가 / large block allocation 실패 / cache locality 악화 발생

### Garbage Collection

| 단계 | 역할 |
|------|------|
| Mark | 살아있는 객체 탐색 |
| Sweep | garbage 회수 |
| Compact | 메모리 압착 |

GC 압력이 커지면: stop-the-world / latency spike / CPU 증가 발생 가능

### Paging / Swapping

자주 안 쓰는 page를 disk swap 영역으로 이동합니다. 과도하면 swap thrashing이 발생합니다.

### NUMA

NUMA 시스템에서는 CPU와 가까운 메모리 접근이 더 빠릅니다. 잘못된 allocation은 remote memory access 증가 / latency 증가를 유발합니다.

### Memory Reclaim

메모리 압박 시 page cache 회수 / inactive page 제거 / slab reclaim / swap out을 수행합니다.

### Copy-On-Write

초기에는 동일 physical page를 공유하다가, 쓰기 발생 시 새 page를 복사합니다. fork 최적화의 핵심입니다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

```bash
# 전체 메모리 상태
free -h

# 메모리 상세
cat /proc/meminfo

# 프로세스 메모리
cat /proc/<pid>/smaps

# paging/swap
vmstat
sar -B

# OOM 확인
dmesg | grep -i oom

# allocator/page 분석
perf
slabtop
pmap
```

### Runtime

| 현상 | 관련 |
|------|------|
| heap growth | allocator pressure |
| GC pause | reclaim |
| object churn | allocation frequency |
| memory leak | unreleased object |

### Kubernetes

| 요소 | 의미 |
|------|------|
| memory limit | cgroup limit |
| OOMKill | hard limit 초과 |
| eviction | node memory pressure |
| page cache reclaim | container pressure |
| memory overcommit | allocation risk |

**container memory limit + runtime heap sizing** 조합이 매우 중요합니다.

### Observability

| 지표 | 의미 |
|------|------|
| RSS | 실제 resident memory |
| VSZ | virtual size |
| Page Fault | translation miss |
| Swap In/Out | reclaim activity |
| Cache Hit Ratio | locality |
| GC Time | reclaim overhead |

| 도구 | 목적 |
|------|------|
| vmstat | memory pressure |
| sar | paging |
| perf | TLB/cache |
| eBPF | allocation tracing |
| pmap | mapping 분석 |
| slabtop | kernel slab |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*