# 메모리 회수 (Memory Reclamation)

> 정독: 0회

## 1. 이 기술이 무엇인가

메모리 회수는:

> 더 이상 사용되지 않는 메모리 영역을 **시스템이 다시 사용 가능한 상태로 되돌리는 과정**

> **핵심 목적: 사용 종료된 메모리를 재사용 가능 상태로 복구**

**대표 방식:**

| 방식 | 특징 |
|------|------|
| Manual Reclamation | 개발자가 직접 해제 |
| Automatic Reclamation | 런타임/GC가 자동 회수 |

대표 API: `free()`, `delete`, `munmap()`, GC sweep

---

## 2. 시스템 어디에서 등장하는가

메모리 회수는 거의 모든 실행 환경에서 등장합니다.

| 영역 | 역할 |
|------|------|
| Heap Allocator | free block 반환 |
| Garbage Collector | garbage reclaim |
| Kernel VM | page reclaim |
| Slab Allocator | kernel object reclaim |
| Container Runtime | memory reclaim |
| Hypervisor | guest memory reclaim |

특히 Heap, Virtual Memory, Page Cache, Shared Memory, Object Lifecycle과 매우 밀접합니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **메모리**이지만, 실제로는 다음 전체와 연결됩니다.

| 자원 | 영향 |
|------|------|
| Memory | free/reuse |
| CPU | reclaim scanning |
| Cache | metadata traversal |
| Disk | swap reclaim |
| Scheduler | reclaim latency |

특히 GC 기반 시스템에서는 CPU 영향도 큽니다.

---

## 4. 왜 중요한가

메모리 회수가 없다면 메모리는 계속 증가만 합니다. 결과로 OOM, swap storm, allocator failure, system crash가 발생할 수 있습니다.

장기 실행 서버, DB, Kubernetes node, JVM, Browser, Cache server 같은 시스템에서 메모리 회수는 매우 중요합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### Memory Leak

가장 대표적인 장애로, 회수되지 않는 메모리가 증가하여 RSS 증가, Heap 증가, OOM Killer 발동으로 이어집니다.

### GC Pause 폭증

자동 회수 시스템에서 reclaim 대상 증가로 mark/sweep 증가, stop-the-world 증가가 발생할 수 있습니다.

### Fragmentation

회수 후 메모리 조각화가 발생합니다. 총 메모리는 충분한데 큰 연속 공간이 없는 문제가 생깁니다.

### Swap Thrashing

회수가 느리면 reclaim pressure 증가, swap 증가, disk I/O 폭증이 발생할 수 있습니다.

### Use-after-free

이미 회수된 메모리에 접근하면 segmentation fault, corruption, security vulnerability가 발생할 수 있습니다.

### Double Free

이미 회수한 메모리를 재해제하면 heap corruption이 발생할 수 있습니다.

---

## 6. 핵심 메커니즘

### 1) 메모리 사용 종료

프로그램이 pointer 제거, object reference 제거, stack frame 종료, explicit free 호출 등을 수행합니다.

### 2) Reclaim Candidate 식별

메모리 추적 시스템이 더 이상 접근 불가능함을 판정합니다.

| 방식 | 설명 |
|------|------|
| Reference Counting | 참조 수 0 |
| Reachability Analysis | root unreachable |
| Explicit Free | 직접 해제 요청 |

### 3) Allocation Metadata 업데이트

allocator/runtime/kernel이 `allocated → free` 상태 변경, ownership 제거, free list 등록을 수행합니다.

### 4) Free Pool 반환

회수된 메모리는 free list, slab cache, page pool 등으로 반환됩니다.

### 5) Coalescing 수행

인접한 free block을 병합합니다. 목적은 fragmentation 감소입니다.

### 6) 필요 시 Physical Reclaim

가상 메모리 시스템에서 page unmap, page reclaim, swap reclaim 등을 수행합니다.

---

## 7. Linux/Runtime/K8s에서 관측 방법

### Linux

```bash
# 시스템 메모리 상태
free -h
vmstat
sar -r

# 프로세스 메모리 증가
top
htop
ps aux

# OOM 기록
dmesg | grep -i oom

# Virtual Memory 상태
cat /proc/meminfo

# Slab reclaim 상태
slabtop

# Page reclaim 상태 (si: swap in, so: swap out, free: free memory)
vmstat 1

# 프로세스 메모리 매핑
cat /proc/<PID>/smaps
```

### Kubernetes

```bash
# pod/node 메모리 확인
kubectl top pod
kubectl describe node

# cgroup reclaim 상태
cat /sys/fs/cgroup/memory.current
cat /sys/fs/cgroup/memory.events
```

### GC 관측

주요 관측 항목: heap usage, GC frequency, reclaim time, pause time

### Page Cache Reclaim

```bash
cat /proc/meminfo
```

| 항목 | 의미 |
|------|------|
| Cached | reclaim 가능한 cache |
| Buffers | buffer memory |
| Active | active page |
| Inactive | reclaim candidate |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*