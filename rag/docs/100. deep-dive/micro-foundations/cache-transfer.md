# 캐시 전송 (Cache Transfer)

> 정독: 0회

캐시 전송은:

> **CPU 캐시 계층(L1/L2/L3)과 DRAM 사이에서 데이터를 캐시 라인(Cache Line) 단위로 복사·이동시키는 하드웨어 데이터 이동 과정**

**쉽게 말하면:**
"CPU가 필요한 데이터를 메모리 계층 사이에서 실어 나르는 작업"

---

## 1. 이 기술이 무엇인가

CPU는 연산할 때 필요한 데이터를 가장 가까운 캐시에서 먼저 찾습니다.

```
L1 → L2 → L3 → DRAM
```

이 과정에서 데이터가 없으면(Cache Miss) 하위 계층에서 상위 계층으로 데이터를 복사해야 합니다.

이 데이터 이동이 **Cache Transfer**입니다.

---

## 2. 시스템 어디에서 등장하는가

캐시 전송은 CPU가 메모리를 읽는 거의 모든 순간 발생합니다.

대표 사례:

- 프로그램 실행
- 배열 순회
- 함수 호출
- 파일 읽기
- 네트워크 패킷 처리
- DB 데이터 조회
- 반복문 연산

즉, **CPU ↔ Cache ↔ DRAM** 전체 데이터 유통망의 핵심입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향도 |
|------|--------|
| Memory | 절대적 |
| CPU | 매우 큼 |
| Cache | 매우 큼 |
| Disk | 간접 영향 |
| Network | 간접 영향 |

특히 **Memory Latency**를 직접 결정합니다.

---

## 4. 왜 중요한가

CPU는 매우 빠르지만 DRAM은 상대적으로 느립니다.

따라서 캐시 전송 효율이 낮으면 CPU가 계속 대기하게 됩니다.

> **캐시 전송 효율 = 실제 시스템 성능**에 매우 가깝습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. Cache Miss 폭증

필요한 데이터가 캐시에 없으면 DRAM 접근이 증가합니다.

**결과:** CPU Stall 증가

### 5-2. Memory Bandwidth 포화

캐시 전송이 너무 많아지면 메모리 버스에 과부하가 발생합니다.

**결과:** 전체 시스템 레이턴시 증가

### 5-3. Cache Thrashing

작업 데이터가 캐시보다 크면 캐시 교체(Eviction)가 반복됩니다.

**결과:** 전송 폭증

### 5-4. False Sharing

멀티코어 환경에서 서로 다른 코어가 같은 캐시 라인을 수정하면 캐시 동기화 전송이 증가합니다.

**결과:** 성능 급락 가능

### 5-5. NUMA Remote Access

다른 CPU 소켓의 메모리를 읽으면 캐시 전송 거리가 증가합니다.

**결과:** Remote Memory Latency 증가

---

## 6. 핵심 메커니즘

### 6-1. 캐시는 캐시 라인 단위로 움직인다

CPU는 보통 **64 Byte** 단위로 데이터를 이동합니다.

즉, 데이터 1바이트만 필요해도 64바이트 전체가 전송될 수 있습니다.

### 6-2. Cache Miss가 전송을 유발한다

```
L1 Miss → L2 탐색 → L3 탐색 → DRAM 접근 → Cache Line Fill
```

### 6-3. 캐시 라인 채우기 (Line Fill)

데이터 발견 시 하위 계층에서 상위 캐시로 복사됩니다. 이를 **Cache Line Fill**이라 부릅니다.

### 6-4. 캐시는 공간 지역성을 활용한다

인접 데이터도 곧 사용할 확률이 높기 때문에 주변 데이터까지 함께 가져옵니다.

### 6-5. Eviction은 캐시에서 밀어내는 과정이다

새 데이터를 넣기 위해 기존 캐시 라인을 제거합니다. 이를 **Cache Eviction**이라 합니다.

### 6-6. Write-Back은 수정 내용을 DRAM에 반영한다

캐시 내부 데이터 수정 시 나중에 DRAM에 기록합니다.

```
Cache → DRAM  (방향 전송)
```

### 6-7. 캐시 전송은 전체 메모리 성능 핵심이다

CPU 성능은 실제로 **얼마나 빠르게 캐시 전송이 일어나느냐**에 크게 좌우됩니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 캐시 미스 분석
perf stat -e cache-misses,cache-references

# 메모리 대역폭 확인
perf mem

# CPU 캐시 정보
lscpu

# NUMA 확인
numactl --hardware

# 메모리 병목 관측
vmstat 1
```

### Runtime

핵심 관측 포인트:

- Cache Miss Rate
- Memory Stall
- IPC 감소
- Memory Bandwidth
- LLC Miss
- NUMA Access

### Kubernetes

```bash
# Pod CPU/Memory 사용량
kubectl top pod
```

> **NUMA / CPU Pinning** — 고성능 워크로드에서 중요  
> **HugePages** — 메모리 전송 최적화에 사용 가능

### 성능 저하 패턴

CPU 사용률은 낮은데 응답이 느린 경우 → **메모리 계층 병목** 가능성 존재

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*