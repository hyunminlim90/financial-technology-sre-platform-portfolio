# 메모리 추적 (Memory Tracking)

> 정독: 0회

## 1. 이 기술이 무엇인가

메모리 추적은:

> 시스템이 현재 사용 중인 메모리 블록들의 상태를 **지속적으로 기록·관리·감시하는 메커니즘**

**추적 대상:** 어떤 메모리가 할당되었는가, 누가 소유 중인가, 아직 사용 중인가, 해제되었는가, 접근 가능한가, 누수가 발생했는가

> **핵심: 메모리 상태의 실시간 관리와 생명주기 추적**

메모리 추적은 운영체제, 런타임, 메모리 allocator, 가비지 컬렉터, 디버거, 프로파일러 등에서 매우 중요합니다.

---

## 2. 시스템 어디에서 등장하는가

메모리 추적은 거의 모든 런타임 환경에서 등장합니다.

| 영역 | 메모리 추적 역할 |
|------|----------------|
| Heap allocator | allocation/free 기록 |
| Garbage Collector | live object 추적 |
| Virtual Memory | page 상태 추적 |
| Kernel | page frame 관리 |
| Container Runtime | memory accounting |
| Kubernetes | cgroup memory tracking |
| Debugger | leak detection |
| Profiler | allocation profiling |

실행 중인 시스템은 항상 메모리 상태를 추적합니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **메모리**이지만, 실제로는 다음 전체에 영향을 줍니다.

| 자원 | 영향 |
|------|------|
| Memory | allocation metadata |
| CPU | tracking overhead |
| Cache | metadata access |
| Disk | swap/page management |
| Scheduler | memory pressure 영향 |

특히 중요한 부분은 allocation metadata, GC scanning, page tracking, reference tracking, memory accounting입니다.

---

## 4. 왜 중요한가

메모리 추적이 없다면 시스템은 어떤 메모리가 사용 중인지, 어떤 메모리가 해제 가능한지, 누수가 발생했는지, 누가 메모리를 점유 중인지를 알 수 없습니다.

결과로 memory leak, dangling pointer, double free, invalid access, OOM 등이 발생할 수 있습니다.

현대 시스템에서 memory isolation, process protection, garbage collection, container limit enforcement 모두 **메모리 추적 기반으로 동작**합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### Memory Leak

해제되지 않는 메모리가 증가하여 RSS 증가, Heap 증가, OOM으로 이어집니다.

### Dangling Pointer

이미 해제된 메모리에 접근하는 use-after-free가 발생합니다. 매우 위험한 보안 문제입니다.

### Double Free

같은 메모리를 두 번 해제하면 allocator corruption, heap corruption, crash가 발생할 수 있습니다.

### GC Overhead 폭증

추적 대상 객체가 과다하면 mark phase 증가, scan cost 증가, pause 증가가 발생합니다.

### Page Table Explosion

매우 많은 메모리 매핑 시 page metadata 증가, TLB pressure 증가가 발생할 수 있습니다.

### Container Memory Accounting 문제

Kubernetes/cgroup 환경에서 실제 사용량과 accounting mismatch, memory reclaim 문제가 발생할 수 있습니다.

---

## 6. 핵심 메커니즘

### 1) 메모리 할당 발생

프로그램이 `malloc`, `new`, `mmap`, stack growth 등을 수행합니다.

### 2) Tracking Metadata 생성

allocator/runtime/kernel이 메타데이터를 기록합니다.

| 정보 | 의미 |
|------|------|
| Base Address | 시작 주소 |
| Size | 블록 크기 |
| Allocation State | 사용 여부 |
| Permission | 접근 권한 |
| Reference Info | 참조 상태 |

### 3) Runtime 접근 추적

실행 중 pointer, object reference, page access, stack usage 등을 추적합니다.

### 4) Reachability 분석

GC 기반 시스템에서 stack, register, global root부터 추적을 시작합니다. 도달 불가능한 객체를 garbage로 판정합니다.

### 5) Memory Reclaim 준비

추적 시스템이 해당 메모리가 더 이상 사용되지 않음을 판정합니다.

### 6) 회수 수행

free, GC sweep, page reclaim, slab reclaim 등을 수행합니다.

---

## 7. Linux/Runtime/K8s에서 관측 방법

### Linux

```bash
# 프로세스 메모리 상태
cat /proc/<PID>/status

# 메모리 매핑 상태
cat /proc/<PID>/maps

# 상세 메모리 통계
cat /proc/<PID>/smaps

# 시스템 전체 메모리
free -h
vmstat
sar -r

# Slab 추적 (커널 메모리)
slabtop

# Page 상태
cat /proc/meminfo

# Memory Leak 분석
valgrind
asan
heaptrack

# Page Fault 추적
vmstat 1
perf stat

# OOM 기록
dmesg | grep -i oom
```

### Kubernetes

```bash
# pod 메모리 사용량
kubectl top pod
kubectl describe pod

# cgroup 메모리 accounting
cat /sys/fs/cgroup/memory.current
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*