# 캐시 플러시 (Cache Flush)

> 정독: 0회

캐시 플러시(Cache Flush)는:

> **CPU 캐시에 존재하는 데이터를 하위 메모리 계층(DRAM 등)에 강제로 반영하고, 해당 캐시 라인을 비우거나 무효화하는 동기화(Synchronization) 메커니즘**

**쉽게 말하면:**
"캐시에만 있던 최신 데이터를 메인 메모리에 공식 반영한 뒤 캐시 상태를 정리하는 과정"

---

## 1. 이 기술이 무엇인가

CPU는 성능을 위해 데이터를 캐시 내부에서 먼저 수정합니다.

즉, DRAM보다 캐시가 최신 상태일 수 있습니다.

이 상태에서 다른 코어, DMA 장치, OS 커널이 DRAM 데이터를 읽으면 **오래된 값을 읽을 수 있습니다.**

따라서 `캐시 → DRAM` 강제 반영이 필요하며, 이 작업이 **Cache Flush**입니다.

---

## 2. 시스템 어디에서 등장하는가

특히 **CPU 외부 장치와 메모리를 공유할 때** 매우 중요합니다.

대표 등장 위치:

- 멀티코어 동기화
- DMA I/O
- NIC 패킷 처리
- SSD DMA
- 컨텍스트 스위칭
- 커널 메모리 관리
- 페이지 테이블 변경
- 캐시 일관성 유지

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향도 |
|------|--------|
| Memory | 절대적 |
| CPU | 매우 큼 |
| Cache | 매우 큼 |
| Network | 간접 영향 |
| Disk | 간접 영향 |

특히 **Memory Consistency**와 **Cache Coherency**에 핵심입니다.

---

## 4. 왜 중요한가

여러 코어와 장치가 동일 데이터를 공유할 때 다음과 같은 상태가 발생할 수 있습니다.

```
CPU 캐시 최신값  ≠  DRAM 값
```

이 상태를 방치하면 데이터 불일치, 오래된 값 읽기, 잘못된 연산, 패킷 오염, 파일 손상이 발생할 수 있습니다.

> **Flush는 성능보다 무결성 보장 목적이 강합니다.**

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. DMA 데이터 불일치

NIC/SSD가 DMA로 DRAM을 읽을 때 CPU 최신 데이터가 캐시에만 존재하면 장치가 오래된 데이터를 읽을 수 있습니다.

### 5-2. 멀티코어 데이터 가시성 문제

코어 A의 수정값이 캐시에만 존재하면 코어 B가 이전 값을 사용할 수 있습니다.

**결과:** 데이터 경쟁(Race Condition) 발생 가능

### 5-3. 컨텍스트 스위칭 비용 증가

프로세스 교체 시 캐시 상태 정리가 필요할 수 있습니다.

**결과:** 성능 저하 가능

### 5-4. TLB / Page Table Flush 비용

가상 메모리 매핑 변경 시 **TLB Flush**가 발생합니다.

**결과:** 페이지 변환 성능 하락 가능

### 5-5. Cache Flush Storm

멀티코어 공유 데이터가 많으면 Flush + Invalidate가 반복됩니다.

**결과:** 캐시 효율 급락

---

## 6. 핵심 메커니즘

### 6-1. Flush는 Write-Back과 연결된다

Dirty Line 존재 시 `캐시 → DRAM` 반영이 필요합니다.

즉, **Flush는 보통 Write-Back을 동반**합니다.

### 6-2. Flush 후 Invalidate 가능하다

데이터 반영 후 캐시 라인을 무효화할 수 있습니다.

즉, 다음 접근 시 강제 Cache Miss를 유도할 수 있습니다.

### 6-3. Flush는 성능 희생이다

Flush 발생 시 캐시 장점 일부를 포기합니다.  
빠른 캐시 데이터를 버리게 되기 때문입니다.

### 6-4. Flush는 일관성 확보 목적이다

핵심 목적은 **"모든 주체가 동일 데이터를 보게 만들기"**, 즉 **데이터 가시성(Visibility) 확보**입니다.

### 6-5. Flush와 Eviction은 다르다

| 항목 | Flush | Eviction |
|------|-------|----------|
| 목적 | 일관성 | 공간 확보 |
| 발생 | 명시적 / 동기화 | 자동 |
| 이유 | 무결성 | 캐시 부족 |
| 결과 | Write-Back + Invalidate 가능 | 교체 |

### 6-6. 멀티코어는 Coherency Protocol을 사용한다

현대 CPU는 **MESI Protocol** 등으로 자동 Flush/Invalidate 유사 동작을 수행합니다.

즉, **코어 간 캐시 상태를 자동으로 조율**합니다.

### 6-7. Flush는 메모리 장벽과 연결된다

메모리 순서 보장을 위해 **Memory Barrier(Fence)** 와 함께 사용됩니다.

즉, **"이전 쓰기를 먼저 보장"** 하는 역할입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# CPU 캐시 정보
lscpu

# 메모리 동기화 분석
perf stat

# NUMA 상태 확인
numactl --hardware

# 인터럽트 및 CPU 상태
mpstat -P ALL 1

# Page Table / TLB 관련
perf stat -e dTLB-load-misses
```

### Runtime

주요 관측 포인트:

- Cache Miss
- Memory Barrier
- Coherency Traffic
- NUMA Access
- IPC 감소
- CPU Stall

### Kubernetes

**멀티코어 Pod 환경** — 공유 메모리가 많으면 Coherency Traffic이 증가할 수 있습니다.

**NUMA Misalignment** — 다른 NUMA Node 접근 시 추가 Flush/Sync 비용이 증가할 수 있습니다.

**고성능 워크로드**에서 중요한 요소:

- CPU Pinning
- NUMA Affinity
- Shared Memory 최소화
- False Sharing 방지

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*