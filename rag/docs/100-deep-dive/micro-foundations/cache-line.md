# 캐시 라인 (Cache Line)
## Micro Foundations — CPU와 DRAM 사이의 최하단 데이터 유통 블록 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

캐시 라인은:

> **CPU 캐시(L1/L2/L3)가 데이터를 관리하고 이동시키는 최소 하드웨어 전송 단위**

핵심은 **CPU는 데이터를 1바이트씩 다루지 않고, 보통 64바이트 단위로 묶어서 이동**시킨다는 것이다.

즉 DRAM ↔ Cache ↔ CPU 사이에서 **64B 덩어리 단위로 이동하는 패키지**가 캐시 라인이다.

> **현대 CPU 대부분: Cache Line Size = 64 Bytes**

<details>
<summary>Deep Dive</summary></br>

Cache Memory(캐시 메모리) [[M]](../../100-deep-dive/micro-foundations/cache-memory.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

거의 모든 메모리 접근 경로에 등장한다.

| 위치 | 의미 |
|------|------|
| L1 Cache | 코어 바로 옆 초고속 캐시 |
| L2 Cache | 코어 근처 중간 캐시 |
| L3 Cache | 여러 코어 공유 캐시 |
| DRAM Controller | 메모리 fetch 대상 |
| SMP / NUMA | 멀티코어 coherence 기준 |
| DMA | 메모리 블록 이동 |
| NIC / SSD Buffer | alignment 기준 |

> **CPU는 메모리를 바이트 단위로 다루는 것이 아니라 캐시 라인 단위로 유통**한다.

---

## 3. 어떤 자원에 가장 영향이 큰가

핵심 영향 자원은 **CPU + Memory**이다.

특히 다음과 직결된다:

- cache miss
- memory latency
- memory bandwidth
- cache coherence
- NUMA traffic

> **캐시 라인은 CPU 성능의 핵심 단위**이다.

---

## 4. 왜 중요한가

CPU는 DRAM보다 압도적으로 빠르다.

| 자원 | 속도 |
|------|------|
| Register | 매우 빠름 |
| L1 Cache | 매우 빠름 |
| DRAM | 훨씬 느림 |

그래서 CPU는 자주 쓰는 데이터를 캐시에 미리 올려두는데, 이때 **데이터 이동의 최소 단위가 Cache Line**이다.

> **캐시 라인은 CPU 성능 최적화의 핵심 단위**이다.

---

## 5. 실제 장애와의 관련성

### 1) Cache Miss 폭증
cache line이 없으면 → DRAM 접근 증가 → **latency 증가, IPC 감소** 발생.

### 2) False Sharing
논리적으로 독립된 변수라도 **같은 cache line 안에 있으면** CPU는 동일 데이터로 간주한다.
결과: **invalidate storm, coherence traffic, cache ping-pong** 발생 가능.

### 3) NUMA 병목
원격 NUMA node cache line fetch 시 → **remote latency 증가, interconnect traffic 증가** 가능.

### 4) Tail Latency 증가
cache miss가 반복되면 → **p99 latency 급등** 가능. 고성능 분산 시스템에서 매우 중요.

---

## 6. 핵심 메커니즘

### A. CPU는 64B 단위로 가져온다

주소 `0x1000` 의 4바이트만 필요해도 CPU는 `0x1000 ~ 0x103F` 까지 **통째로 fetch**한다.

### B. Spatial Locality

프로그램은 보통 인접 메모리를 연속 접근한다.

```
array[0] → array[1] → array[2]
```

그래서 CPU는 **주변 데이터까지 한꺼번에 가져오는 것이 이득**이라고 판단한다. 이것이 **Spatial Locality**이다.

### C. Tag 비교

각 cache line은 다음을 가진다:

- data
- tag (이 데이터가 원래 어느 메모리 주소 출신인가)
- status bits

CPU는 **요청 주소 vs tag** 를 비교하여 **cache hit / cache miss** 를 판별한다.

### D. Dirty Bit

CPU가 cache line을 수정할 때 DRAM까지 즉시 내리지 않을 수 있다.
대신 **dirty bit = 1** 로 마킹하고, eviction 시 **write-back** 을 수행한다.

> **cache coherence의 핵심 상태 정보**이다.

### E. Cache Coherence

코어마다 자기 L1 cache와 cache line copy를 가진다.

문제: **코어 A가 수정하면 코어 B의 copy는 오래된 데이터**가 된다.

따라서 다음과 같은 coherence 시스템이 존재한다:

- invalidate
- snooping
- MESI protocol

### F. False Sharing

| 변수 | 크기 |
|------|------|
| A | 8B |
| B | 8B |

논리적으로 독립이어도 **같은 64B cache line 안에 있으면** 한 코어가 수정할 때마다 다른 코어의 line이 invalidate된다.

결과: **CPU cache ping-pong** 발생. 고성능 시스템에서 매우 치명적.

### G. Cache Line Alignment

이를 방지하기 위해 시스템 엔지니어링에서는 다음을 수행한다:

- 64B alignment
- padding
- cache isolation

핵심 목적: **서로 다른 스레드의 데이터가 동일 cache line을 공유하지 않게** 하는 것.

---

## 7. Linux / Runtime / K8s에서의 관측

### Linux perf

```bash
perf stat    # cache-misses, LLC-load-misses, cache-references 관측
```

### CPU Cache 정보 확인

```bash
lscpu
getconf LEVEL1_DCACHE_LINESIZE    # cache line size 확인
```

### NUMA 분석

```bash
numastat    # 원격 cache traffic 분석
```

### eBPF / perf flamegraph

cache miss hotspot 분석 가능.

### Kubernetes

간접 영향이 다음 형태로 관측된다:

- CPU throttling
- latency spike
- noisy neighbor
- NUMA imbalance

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*