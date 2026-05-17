# 캐시 이출 (Cache Eviction)

> 정독: 0회

캐시 이출(Cache Eviction)은:

> **CPU 캐시 공간이 가득 찼을 때, 새로운 캐시 라인을 적재하기 위해 기존 캐시 라인 일부를 캐시 밖으로 제거하는 하드웨어 메모리 관리 동작**

**쉽게 말하면:**
"새 데이터를 넣기 위해 기존 캐시 데이터를 밀어내는 과정"

---

## 1. 이 기술이 무엇인가

CPU 캐시는 매우 빠르지만 용량이 매우 작습니다.

따라서 새 데이터가 계속 유입되면 기존 데이터 일부를 제거해야 합니다.

이 제거 동작이 **Cache Eviction**입니다.

---

## 2. 시스템 어디에서 등장하는가

거의 모든 메모리 접근에서 등장할 수 있습니다. 특히 **캐시보다 큰 작업셋(Working Set)**에서 자주 발생합니다.

대표 사례:

- 큰 배열 순회
- 랜덤 메모리 접근
- 대규모 데이터 처리
- 멀티코어 병렬 처리
- 프로세스 전환 (Context Switch)
- 데이터베이스 스캔
- 파일 캐시 교체

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향도 |
|------|--------|
| Cache | 절대적 |
| Memory | 매우 큼 |
| CPU | 매우 큼 |
| Disk | 간접 영향 |
| Network | 간접 영향 |

특히 **Cache Hit Ratio**를 크게 좌우합니다.

---

## 4. 왜 중요한가

캐시 성능의 핵심은 **필요한 데이터가 얼마나 오래 캐시에 남아있는가**입니다.

Eviction이 과도하면 캐시 재사용에 실패하고:

```
DRAM 접근 증가 → CPU Stall 증가
```

> **Eviction 품질 = 캐시 효율**에 매우 가깝습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. Cache Thrashing

작업 데이터가 캐시보다 크면 계속 밀어내고 다시 가져오는 과정이 반복됩니다.

**결과:** Eviction 폭증

### 5-2. DRAM 접근 증가

Eviction 후 해당 데이터가 다시 필요하면 `Cache Miss → DRAM 재접근`이 발생합니다.

**결과:** 메모리 지연 증가

### 5-3. CPU Stall 증가

Eviction으로 캐시 히트율이 감소하면 CPU가 DRAM 대기 상태에 자주 들어갑니다.

### 5-4. Tail Latency 증가

대규모 서비스에서 갑작스러운 응답 지연은 **LLC Eviction 폭증**과 연결되는 경우가 많습니다.

### 5-5. Dirty Eviction 비용

수정된 캐시 라인(Dirty Line)이 축출되면 **Write-Back**이 발생합니다.

```
캐시 → 하위 계층(DRAM/L3)  (추가 전송 필요)
```

**결과:** 메모리 버스 부하 증가

---

## 6. 핵심 메커니즘

### 6-1. 캐시는 무한하지 않다

L1/L2/L3는 모두 제한된 슬롯(Set) 구조를 가집니다.  
따라서 새 라인 적재 시 기존 라인 제거가 필요합니다.

### 6-2. Eviction은 Cache Line 단위다

제거되는 최소 단위는 보통 **64 Byte Cache Line**입니다.  
데이터 일부만 제거되는 것이 아니라 라인 전체가 제거됩니다.

### 6-3. 희생양(Victim)을 선택한다

캐시 컨트롤러는 어떤 라인을 제거할지 결정해야 합니다.

| 정책 | 의미 |
|------|------|
| LRU | 가장 오래 안 쓴 라인 제거 |
| FIFO | 먼저 들어온 라인 제거 |
| Random | 임의 제거 |

실제 CPU는 **Pseudo-LRU** 류 근사 알고리즘을 많이 사용합니다.

### 6-4. Clean Line은 그냥 제거 가능하다

읽기만 한 데이터는 DRAM 원본과 동일합니다.  
따라서 그냥 버려도 문제가 없습니다.

### 6-5. Dirty Line은 Write-Back이 필요하다

수정된 데이터는 캐시가 최신본입니다.  
따라서 제거 전 하위 계층에 반영해야 합니다. 이 과정이 **Write-Back**입니다.

### 6-6. Eviction과 Line Fill은 같이 움직인다

```
새 데이터 필요
↓
Cache Miss
↓
Line Fill 시도
↓
공간 부족
↓
Eviction 발생
↓
새 라인 적재
```

즉, **Line Fill과 Eviction은 메모리 계층의 한 세트 동작**입니다.

### 6-7. 멀티코어에서는 Coherency 영향도 받는다

다른 코어가 같은 캐시 라인을 수정하면 **라인 무효화(Invalidate)**가 발생할 수 있습니다.

**결과:** 간접적 Eviction 효과 발생 가능

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 캐시 미스 확인
perf stat -e cache-misses,cache-references

# LLC 관련 분석
perf stat -e LLC-load-misses

# CPU 병목 분석
perf top

# NUMA 확인
numactl --hardware

# 캐시 구조 확인
lscpu
```

### Runtime

주요 관측 포인트:

- Cache Miss Rate
- LLC Miss
- Memory Stall
- IPC 감소
- Working Set Size
- Memory Bandwidth

### Kubernetes

**CPU 사용률은 낮은데 느린 경우** → 캐시 계층 병목 가능성

**Eviction 증가가 우려되는 상황:**

- 대형 Pod
- 메모리 초과 사용
- NUMA Remote Access
- CPU Migration

**CPU Affinity 중요** — 코어 이동이 많으면 기존 캐시 재사용에 실패할 수 있습니다.

**결과:** Eviction + Refill 증가

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*