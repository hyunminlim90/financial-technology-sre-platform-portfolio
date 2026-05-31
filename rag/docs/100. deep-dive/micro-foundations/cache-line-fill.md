# 캐시 라인 채우기 (Cache Line Fill)

> 정독: 0회

캐시 라인 채우기(Cache Line Fill)는:

> **CPU가 필요한 데이터를 캐시에서 찾지 못했을 때(Cache Miss), 하위 계층(L2/L3/DRAM)으로부터 해당 데이터가 포함된 캐시 라인(보통 64B)을 통째로 읽어와 상위 캐시를 채우는 하드웨어 동작**

**쉽게 말하면:**
"CPU가 필요한 데이터를 아래 메모리 계층에서 끌어올려 캐시에 적재하는 과정"

---

## 1. 이 기술이 무엇인가

CPU는 연산 시 `L1 → L2 → L3 → DRAM` 순으로 데이터를 찾습니다.

L1 캐시에 데이터가 없으면 **Cache Miss**가 발생하고, CPU는 하위 계층에서 데이터를 가져와 L1 캐시를 채웁니다.

이 동작이 **Cache Line Fill**입니다.

---

## 2. 시스템 어디에서 등장하는가

거의 모든 CPU 연산에서 등장합니다. 특히 **처음 접근하는 데이터**에서 자주 발생합니다.

대표 사례:

- 배열 순회
- 파일 읽기
- 네트워크 패킷 처리
- 데이터베이스 조회
- 함수 호출
- 반복문
- 프로세스 전환 후 재실행

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향도 |
|------|--------|
| Memory | 절대적 |
| CPU | 매우 큼 |
| Cache | 매우 큼 |
| Disk | 간접 영향 |
| Network | 간접 영향 |

특히 **Memory Latency**와 **CPU Stall Time**에 직접 영향을 줍니다.

---

## 4. 왜 중요한가

CPU는 매우 빠르지만 DRAM 접근은 상대적으로 느립니다.

따라서 Cache Line Fill 빈도가 높아지면 CPU가 계속 메모리 대기 상태가 됩니다.

> **캐시 라인 채우기 효율 = 실제 CPU 활용 효율**에 매우 가깝습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. Cache Miss 폭증

데이터 지역성이 깨지면 Line Fill이 연속으로 발생합니다.

**결과:** CPU Stall 증가

### 5-2. 메모리 대역폭 포화

Line Fill이 과도하면 DRAM ↔ Cache 전송량이 폭증합니다.

**결과:** Memory Bandwidth Saturation 발생 가능

### 5-3. Tail Latency 증가

서비스 레이턴시가 튀는 경우 `L3 Miss → DRAM 접근 → Line Fill 대기`가 원인일 수 있습니다.

### 5-4. Context Switch 이후 성능 저하

프로세스 전환 후 기존 캐시 내용이 무효화되어 새 프로세스 데이터 재적재(Line Fill)가 대량 발생합니다.

### 5-5. NUMA 환경 문제

다른 NUMA Node 메모리를 읽으면 **Remote Line Fill**이 발생합니다.

**결과:** Latency 급증 가능

---

## 6. 핵심 메커니즘

### 6-1. CPU는 필요한 바이트만 가져오지 않는다

정수 4바이트가 필요해도 실제로는 **64바이트 캐시 라인 전체**를 가져옵니다.

### 6-2. 이유는 공간 지역성 때문이다

프로그램은 보통 인접 데이터를 연속으로 접근하는 패턴이 많습니다.  
따라서 주변 데이터까지 함께 가져오는 것이 훨씬 효율적입니다.

### 6-3. Line Fill은 계층적으로 일어난다

```
L1 Miss
↓
L2 탐색
↓
L3 탐색
↓
DRAM 접근
↓
64B Line Fill
↓
상위 캐시 적재
```

### 6-4. 캐시 히트를 만들기 위한 과정이다

한 번 Line Fill이 완료되면 다음 접근부터는 Cache Hit 가능성이 증가합니다.  
즉, **미래 연산 최적화** 성격도 가집니다.

### 6-5. DRAM 접근 자체가 비싸다

| 계층 | 대략적 지연 |
|------|------------|
| L1 | 수 사이클 |
| L2 | 수~십 사이클 |
| L3 | 수십 사이클 |
| DRAM | 수백 사이클 |

따라서 **DRAM Line Fill**은 매우 비쌉니다.

### 6-6. Sequential Access가 유리하다

연속 메모리 접근 시 한 번 Fill된 라인을 재사용할 수 있습니다.  
반대로 **랜덤 접근**은 Line Fill 폭증을 유발할 수 있습니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 캐시 미스 확인
perf stat -e cache-misses,cache-references

# 메모리 접근 병목 분석
perf mem

# CPU Stall 분석
perf top

# NUMA 확인
numactl --hardware

# CPU 캐시 구조 확인
lscpu
```

### Runtime

주요 관측 포인트:

- Cache Miss Rate
- LLC Miss
- Memory Stall
- IPC 감소
- DRAM Access 증가

### Kubernetes

**CPU 사용률은 낮은데 느린 경우** 가능한 원인:

- 메모리 계층 병목
- NUMA Misalignment

Pod가 다른 NUMA Node 메모리에 접근할 경우 **Remote Line Fill**이 증가할 수 있습니다.

**고성능 워크로드**에서 중요한 요소:

- CPU Pinning
- NUMA Affinity
- HugePages
- Memory Locality

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*