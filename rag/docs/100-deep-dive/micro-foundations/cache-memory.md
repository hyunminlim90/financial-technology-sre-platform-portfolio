# 캐시 메모리 (Cache Memory)

## Micro Foundations — CPU와 DRAM 사이의 초고속 완충 계층 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

캐시 메모리는:

> **CPU가 자주 쓰는 데이터와 명령어를 DRAM보다 훨씬 가까운 곳에 임시 보관하는 초고속 메모리 계층**

핵심 목적은 **CPU가 느린 DRAM을 매번 기다리지 않게 하는 것**이다.

> **Cache Memory = CPU 앞단의 초고속 임시 보관소**

<details>
<summary>Deep Dive</summary></br>

Data(데이터) [[M]](../../100-deep-dive/micro-foundations/data.md)  
Instruction(명령어) [[M]](../../100-deep-dive/micro-foundations/instruction.md)  
Dynamic Random Access Memory(DRAM) [[M]](../../100-deep-dive/micro-foundations/dram.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

캐시 메모리는 CPU와 메인 메모리 사이에 위치한다.

| 계층 | 위치 | 특징 |
|------|------|------|
| L1 Cache | CPU 코어 내부 | 가장 빠름, 가장 작음 |
| L2 Cache | 코어 근처 | L1보다 큼, 조금 느림 |
| L3 Cache | 여러 코어 공유 | 가장 큼, DRAM 직전 방어선 |
| DRAM | CPU 외부 | 크지만 느림 |

```
CPU Register → L1 Cache → L2 Cache → L3 Cache → DRAM
```

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 영향은 **CPU + Memory**이다.

특히 다음에 영향을 준다:

- CPU 대기 시간
- 메모리 접근 지연
- cache hit / miss
- memory bandwidth
- NUMA 성능
- 멀티코어 coherence 비용

> **캐시 효율이 낮으면 CPU는 일을 안 하는 것이 아니라 DRAM을 기다리며 멈춘다.**

---

## 4. 왜 중요한가

CPU는 매우 빠르고 DRAM은 상대적으로 느리다. 매번 DRAM에서 데이터를 가져오면 **연산보다 대기 시간이 더 커진다.**

캐시 메모리는 자주 쓸 데이터를 CPU 가까이에 둠으로써 이 문제를 줄인다.

결과:

- latency 감소
- throughput 증가
- CPU pipeline stall 감소
- tail latency 완화

---

## 5. 실제 장애와의 관련성

캐시 메모리는 직접적인 장애보다 **성능 장애의 원인**으로 나타난다.

### 1) Cache Miss 폭증
필요한 데이터가 캐시에 없으면 DRAM까지 내려간다.
결과: **CPU stall, IPC 감소, p99 latency 증가**

### 2) False Sharing
서로 다른 코어가 같은 캐시 라인 안의 다른 변수를 수정하면 **cache line invalidate** 가 반복된다.
결과: **cache ping-pong, coherence traffic 증가, 멀티코어 성능 급락**

### 3) Poor Locality
메모리를 무작위로 접근하면 캐시 효율이 떨어진다.
결과: **DRAM 접근 증가, CPU 사용률 대비 처리량 저하, tail latency 증가**

### 4) NUMA와 결합된 캐시 비효율
원격 NUMA 메모리 접근이 많아지면 → L3 miss 이후 remote DRAM 접근 → **interconnect traffic 증가, latency spike** 발생.

---

## 6. 핵심 메커니즘

### A. Cache Hit / Cache Miss

| 상태 | 의미 |
|------|------|
| Cache Hit | 필요한 데이터가 캐시에 있음 |
| Cache Miss | 캐시에 없어서 DRAM까지 감 |

이 차이가 성능을 크게 가른다.

### B. 지역성 (Locality)

캐시가 효과적인 이유는 프로그램이 보통 지역성을 갖기 때문이다.

| 종류 | 의미 |
|------|------|
| 시간 지역성 | 방금 쓴 데이터를 다시 쓸 가능성 |
| 공간 지역성 | 인접한 주소를 곧 쓸 가능성 |

### C. 캐시 라인 단위 이동

CPU는 데이터를 1바이트씩 가져오지 않고 **64바이트 캐시 라인 단위**로 가져온다.
배열처럼 연속된 데이터 접근은 캐시에 유리하다.

### D. 캐시 계층 구조

L1 → L2 → L3 순서로 찾고, 없으면 DRAM으로 내려간다.

- **L1:** 가장 빠름
- **L2:** 중간
- **L3:** DRAM 직전 최후 방어선

### E. Write-back / Dirty Bit

데이터를 수정해도 매번 DRAM에 즉시 쓰지 않을 수 있다.
대신 캐시 라인에 **dirty bit = 1** 로 표시하고, 캐시에서 밀려날 때 DRAM에 반영(write-back)한다.

### F. Cache Coherence

멀티코어에서는 코어마다 캐시가 있기 때문에, **한 코어가 수정한 값을 다른 코어 캐시가 모를 수 있다.**
이를 해결하기 위해 **MESI** 같은 coherence protocol이 필요하다.

### G. Prefetch

CPU는 앞으로 쓸 것 같은 데이터를 **미리 캐시에 올린다.**
예측이 맞으면 성능이 향상되고, 틀리면 캐시 오염이 생길 수 있다.

---

## 7. Linux / Runtime / K8s에서의 관측

### 캐시 정보 확인

```bash
lscpu
getconf LEVEL1_DCACHE_LINESIZE
```

### 성능 카운터 확인

```bash
perf stat    # cache-references, cache-misses, LLC-load-misses,
             # branch-misses, stalled-cycles 관측
```

### NUMA 확인

```bash
numactl --hardware    # NUMA 구성 확인
numastat              # 원격 메모리 접근 분석
```

### Kubernetes

K8s에서는 다음 형태로 간접적으로 나타난다:

- CPU 사용률은 높지 않은데 latency 증가
- 특정 Pod의 p99 latency 상승
- noisy neighbor 영향
- NUMA 배치 불균형
- CPU pinning 미흡으로 cache locality 저하

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*