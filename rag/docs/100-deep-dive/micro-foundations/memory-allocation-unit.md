# 메모리 할당 단위 (Memory Allocation Unit)

> 정독: 0회

## 1. 이 기술이 무엇인가

메모리 할당 단위는:

> 메모리 관리 시스템이 하나의 데이터 구조나 실행 문맥을 위해 확보하는 독립 메모리 블록

| 요소 | 의미 |
|------|------|
| 시작 주소 | 블록 시작 위치 |
| 크기 | 할당된 총 바이트 수 |
| 경계 | 접근 가능한 범위 |
| 수명 | 생성~해제 기간 |

**대표 사례:** 객체 / 배열 / 버퍼 / 스택 프레임 / 페이지(Page) / slab chunk / malloc block

> 메모리 관리의 최소 운영 단위

---

## 2. 시스템 어디에서 등장하는가

거의 모든 런타임 시스템에서 등장합니다.

| 계층 | 역할 |
|------|------|
| 운영체제 | page allocation |
| runtime allocator | heap allocation |
| compiler ABI | stack frame size |
| kernel | slab allocation |
| network stack | packet buffer |
| filesystem | page cache |
| database | buffer pool |

> Heap / Stack / Virtual Memory 관리의 핵심 기반

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**가장 영향이 큰 자원: Memory + CPU cache + TLB**

| 자원 | 영향 |
|------|------|
| RAM | allocation/deallocation |
| CPU cache | locality |
| TLB | page translation |
| MMU | protection |
| NUMA | remote access latency |

> 메모리 접근 효율과 단편화에 직접 영향

---

## 4. 왜 중요한가

모든 데이터는 결국 메모리 블록 형태로 존재합니다.

프로그램 내부의 객체 / 함수 문맥 / 버퍼 / 캐시 / 패킷 / 페이지 모두 메모리 할당 단위 위에 존재합니다.

| 중요성 | 이유 |
|--------|------|
| 성능 | cache locality |
| 안정성 | memory protection |
| 효율성 | fragmentation 감소 |
| 확장성 | allocator scalability |
| 보안 | overflow 방어 |

> 메모리 할당 전략이 전체 처리량을 좌우하기도 함

---

## 5. 실제 장애와 어떤 관련이 있는가

| 문제 | 원인 |
|------|------|
| OOM | excessive allocation |
| fragmentation | 비효율 블록 분산 |
| memory leak | deallocation 실패 |
| cache miss 증가 | poor locality |
| allocator contention | lock 경쟁 |
| stack overflow | frame 과다 증가 |
| heap corruption | overwrite |
| invalid free | allocator metadata 손상 |

고성능 시스템에서는 **allocation churn**이 심각한 CPU 오버헤드를 유발할 수 있습니다.

**대표 현상:** GC 폭증 / malloc latency 증가 / RSS 급증 / page fault 증가 / allocator spinlock contention

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Allocation Request

프로그램이 메모리 공간이 필요한 상태가 되면 allocator를 호출합니다.

예: 함수 호출 / 객체 생성 / 배열 생성 / 버퍼 생성

### Memory Block

할당 결과로 메모리 관리자가 연속 주소 공간을 확보해 반환합니다.

| 요소 | 설명 |
|------|------|
| base address | 시작 위치 |
| size | 총 크기 |
| alignment | 정렬 규칙 |
| metadata | allocator 관리 정보 |

### Alignment

| 타입 | 일반 정렬 |
|------|-----------|
| 4-byte | 4-byte boundary |
| 8-byte | 8-byte boundary |
| cache line | 64-byte boundary |

정렬 실패 시: cache penalty / unaligned access / additional memory cycle 발생 가능

### Heap Allocation

동적 메모리의 핵심으로, allocator가 heap segment 내부 블록을 관리합니다.

**대표 allocator:** buddy allocator / slab allocator / jemalloc / tcmalloc / ptmalloc

### Stack Allocation

| 항목 | 특징 |
|------|------|
| 속도 | 매우 빠름 (pointer move 수준) |
| 해제 | 자동 해제 (function return) |
| 수명 | call scope |

stack frame 자체가 메모리 할당 단위입니다.

### Page 단위 관리

OS는 실제로 page 단위로 메모리를 관리합니다.

| 종류 | 크기 |
|------|------|
| normal page | 4KB |
| huge page | 2MB / 1GB |

작은 allocation도 내부적으로 page 기반입니다.

### Fragmentation

| 종류 | 의미 |
|------|------|
| external fragmentation | 빈 공간 분산 |
| internal fragmentation | block 내부 낭비 |

메모리 효율을 크게 저하시킬 수 있습니다.

### Allocation Metadata

allocator는 블록마다 size / status / next pointer / owner info 등을 저장합니다. metadata corruption 시 heap corruption이 발생합니다.

### Lifetime Management

| 단계 | 내용 |
|------|------|
| allocation | 생성 |
| active use | 사용 |
| deallocation | 반환 |

수명 관리 실패 시: leak / dangling pointer / use-after-free 발생

### Cache Locality

연속 블록일수록 CPU cache 효율이 증가합니다. fragmentation이 심하면 cache miss 증가 / latency 증가 / throughput 감소가 발생합니다.

### NUMA 영향

다른 NUMA node 메모리 접근 시 remote memory latency가 증가할 수 있습니다. 대형 서버 환경의 SRE/infra에서 매우 중요합니다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

```bash
# 전체 메모리 상태
cat /proc/meminfo

# heap/stack 확인
cat /proc/<pid>/maps

# allocator 상태
pmap <pid>

# page fault
vmstat
sar -B

# NUMA
numactl --hardware
```

### Runtime

| 현상 | 관련 |
|------|------|
| GC pressure | heap block 증가 |
| allocation rate | object churn |
| fragmentation | allocator inefficiency |
| stack growth | recursion |

### Kubernetes

| 현상 | 관련 |
|------|------|
| OOMKill | excessive allocation |
| memory limit exceed | heap growth |
| container RSS 증가 | active blocks 증가 |
| latency spike | allocation contention |

**allocator behavior + cgroup memory limit** 조합이 매우 중요합니다.

### Observability

| 도구 | 목적 |
|------|------|
| perf | allocation hotspot |
| eBPF | malloc/free tracing |
| valgrind | leak detection |
| heap profiler | allocation analysis |
| flamegraph | allocation overhead |

실무에서는 allocation rate 분석이 매우 중요합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*