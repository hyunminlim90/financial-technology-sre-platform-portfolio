# 메모리 할당 (Memory Allocation)

> 정독: 0회

## 1. 이 기술이 무엇인가

메모리 할당은:

> 실행 중인 프로그램이 데이터를 저장할 수 있도록 **메모리 공간을 확보받는 과정**

프로그램은 실행 중 지속적으로 메모리를 요구합니다. 예: 변수 생성, 객체 생성, 배열 생성, 버퍼 생성, 스택 프레임 생성, 파일 캐시 생성, 네트워크 버퍼 생성

운영체제와 런타임은 이 요청에 대응하여 메모리 공간을 할당합니다.

> **핵심: 필요한 크기의 메모리 블록을 확보하고 주소를 반환하는 과정**

---

## 2. 시스템 어디에서 등장하는가

메모리 할당은 거의 모든 실행 흐름에서 발생합니다.

| 영역 | 메모리 할당 발생 |
|------|----------------|
| 함수 호출 | stack frame |
| 객체 생성 | heap allocation |
| 네트워크 | socket buffer |
| 파일 I/O | page cache/buffer |
| 데이터베이스 | buffer pool |
| 컨테이너 | process memory |
| Kubernetes | pod memory |
| 커널 | kernel slab/page allocation |

프로그램 실행 자체가 메모리 할당의 연속입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **메모리(RAM)**이지만, 실제로는 다음 전체와 연결됩니다.

| 자원 | 영향 |
|------|------|
| Memory | heap/stack/page |
| CPU | allocator/GC/page fault |
| Disk | swap/page-in/page-out |
| Cache | locality 영향 |
| Scheduler | memory pressure 영향 |

특히 중요한 항목: Heap Growth, Page Fault, Fragmentation, Swap, GC Pause, Allocation Rate

---

## 4. 왜 중요한가

메모리 할당은 **프로그램 실행의 기반**입니다. 메모리가 없다면 변수 생성, 함수 실행, 데이터 저장, 네트워크 처리가 불가능합니다.

메모리 할당 방식은 시스템 성능 전체를 결정합니다.

| 문제 | 결과 |
|------|------|
| Allocation 과다 | CPU 증가 |
| Fragmentation | 메모리 낭비 |
| Leak | OOM |
| Swap | Latency 폭증 |
| Poor Locality | Cache miss 증가 |

> **메모리 할당은 성능 + 안정성 + 확장성 모두와 연결됩니다.**

---

## 5. 실제 장애와 어떤 관련이 있는가

### Out Of Memory (OOM)

가장 대표적인 문제로, `Cannot allocate memory` 또는 `OOMKilled`가 발생합니다. Kubernetes에서도 매우 흔합니다.

### Memory Leak

해제되지 않는 메모리가 누적되어 RSS 증가, Heap 증가, Swap 증가로 이어지다 결국 OOM이 발생합니다.

### Fragmentation

메모리가 충분해 보여도 연속된 큰 블록이 부족하여 allocation이 실패할 수 있습니다.

### Excessive Allocation

짧은 객체를 과도하게 생성하면 allocator contention, CPU usage 증가, cache miss 증가가 발생합니다.

### Swap Storm

RAM 부족 시 page out / page in이 반복되어 latency가 폭증하고 system freeze 수준의 성능 저하가 발생합니다.

### Page Fault 폭증

필요한 page가 RAM에 없을 때 disk I/O가 발생하여 execution stall이 일어납니다.

---

## 6. 핵심 메커니즘

### 1) Allocation Request 발생

프로그램이 메모리가 필요한 상태에 진입합니다. (`malloc`, `new`, stack growth, `mmap` 등)

### 2) Allocator 개입

런타임 allocator 또는 kernel allocator가 free block 탐색, size alignment, metadata 관리를 수행합니다.

### 3) Virtual Address 할당

프로세스 가상 주소 공간에 새로운 memory region, heap block, stack frame이 생성됩니다.

### 4) Physical Memory Mapping

MMU와 page table이 virtual address → physical page 매핑을 수행합니다.

### 5) 실제 RAM 사용 시작

CPU가 해당 주소에 접근 시 cache line load, RAM access, write operation이 수행됩니다.

### 6) Lifetime 관리

메모리는 이후 free, garbage collection, stack pop, process termination 등으로 회수됩니다.

---

## 7. Linux/Runtime/K8s에서 관측 방법

### Linux

```bash
# 프로세스 메모리 상태
top
htop
ps aux

# 상세 메모리 맵
cat /proc/<PID>/maps

# 메모리 사용량 (VmRSS, VmSize 등)
cat /proc/<PID>/status

# 시스템 메모리 상태
free -h
vmstat

# Page Fault 관측
vmstat 1
sar -B

# NUMA/Physical Memory
numactl --hardware

# Slab Allocator 상태
slabtop

# OOM 기록
dmesg | grep -i oom
```

### Kubernetes

```bash
# pod 메모리 사용량
kubectl top pod
kubectl describe pod

# cgroup Memory Limit
cat /sys/fs/cgroup/memory.max
# 또는
cat /sys/fs/cgroup/memory/memory.limit_in_bytes
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*