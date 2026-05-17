# 레벨 2 캐시 메모리 (Level 2 Cache Memory / L2 Cache)

> 정독: 0회

L2 캐시는:

> CPU 메모리 계층 구조에서 **L1 캐시와 DRAM 사이를 연결하는 중간 완충 캐시 계층**

**쉽게 말하면:**

"L1 캐시에 없는 데이터를 DRAM까지 내려가기 전에 한 번 더 찾아보는 고속 중간 창고"

<details>
<summary>Deep Dive</summary></br>

Memory Hierarchy(메모리 계층 구조) [[M]](../../100-deep-dive/micro-foundations/memory-hierarchy.md)  

</details></br>

## 1. 이 기술이 무엇인가

L2 캐시는 **Level 2 Cache Memory**입니다.

### 특징

| 항목 | 특징 |
|------|------|
| 위치 | CPU 코어 내부 또는 바로 외곽 |
| 구현 | SRAM 기반 |
| 속도 | L1보다 느림 |
| 용량 | L1보다 큼 |
| 목적 | DRAM 접근 감소 |
| 역할 | L1 Miss 완충 |

**핵심 역할:** L1 캐시의 작은 용량을 보완하는 것입니다.

---

## 2. 시스템 어디에서 등장하는가

### 메모리 계층 구조

```
Register
↓
L1 Cache
↓
L2 Cache   ← 현재 주제
↓
L3 Cache
↓
DRAM
↓
SSD
```

### CPU 데이터 탐색 흐름

```
L1 탐색
↓
없으면 L2 탐색
↓
없으면 L3
↓
없으면 DRAM
```

즉, **L2는 DRAM으로 내려가기 전 마지막 고속 완충 지대**입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향 |
|------|------|
| CPU Stall | 매우 큼 |
| Memory Latency | 매우 큼 |
| DRAM Traffic | 직접 감소 |
| Cache Hit Ratio | 매우 큼 |
| IPC | 영향 큼 |

**핵심:** L2 Hit 여부가 DRAM 접근 횟수를 결정합니다.

---

## 4. 왜 중요한가

L1 캐시는 매우 빠르지만 용량이 작습니다. 보통 수십 KB 수준이기 때문에 실제 실행 중에는 **L1 Miss**가 계속 발생합니다.

이때 L2가 없으면 즉시 DRAM 접근이 발생하는데, DRAM은 매우 느립니다.

따라서 L2는 **L1과 DRAM 사이 속도 격차를 완충**합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. L2 Cache Miss 폭증

L2에도 데이터가 없으면:

```
DRAM 접근 증가 → CPU Stall 증가 → Latency 증가
```

### 5-2. Working Set 과대화

실행 중인 데이터가 L2 크기를 넘어서면 **Cache Thrashing**이 발생합니다.

**결과:** 캐시 교체 폭증 → 성능 급락

### 5-3. 멀티코어 경쟁

L2는 보통 코어별 독점 구조이지만 하위 L3와 공유로 연결됩니다.  
코어 간 데이터 이동이 많아지면 **Cache Coherency 비용**이 증가합니다.

### 5-4. Random Access 패턴

메모리 접근이 무작위적이면 **Spatial Locality**가 붕괴됩니다.

**결과:** L2 Hit 감소 → DRAM Access 증가

### 5-5. Context Switch 증가

프로세스 교체가 많아지면 기존 L2 캐시 데이터가 무효화됩니다.

**결과:** Cache Warm-up 반복

---

## 6. 핵심 메커니즘

### 6-1. L2는 SRAM 기반이다

L2도 L1처럼 **SRAM**으로 구현됩니다. DRAM보다 훨씬 빨라야 하기 때문입니다.

### 6-2. L2는 속도와 용량의 균형 지점이다

| 계층 | 속도 | 용량 |
|------|------|------|
| L1 | 매우 빠름 | 매우 작음 |
| L2 | 빠름 | 중간 |
| L3 | 느림 | 큼 |
| DRAM | 매우 느림 | 매우 큼 |

**L2는 중간 완충 계층**입니다.

### 6-3. L2는 보통 코어 전용이다

현대 CPU 대부분은 각 코어마다 독립 L2를 가집니다.

```
Core 0의 L2  ≠  Core 1의 L2
```

### 6-4. L2 접근은 L1보다 느리다

| 계층 | 접근 Cycle |
|------|-----------|
| L1 | 1~4 cycles |
| L2 | 10~20 cycles |
| DRAM | 수백 cycles |

L2도 느린 편은 아니지만 L1보다는 확실히 느립니다.

### 6-5. Cache Line 단위로 이동한다

L2는 데이터를 **64 Byte Cache Line** 단위로 관리합니다.  
필요한 데이터 주변까지 함께 이동합니다.

### 6-6. L2는 DRAM 병목을 강력히 방어한다

L2 Hit 발생 시 **수 ns 수준**의 접근으로 끝납니다.  
반면 DRAM은 수십~100ns 이상 걸립니다.

즉, **L2는 CPU Stall 방어의 핵심**입니다.

### 6-7. 메모리 계층은 단계적 방어 구조다

```
L1 → 실패 시 L2 → 실패 시 L3 → 실패 시 DRAM
```

캐시 계층 전체는 **DRAM 접근 최소화 시스템**입니다.

---

## 7. Linux / Runtime / K8s 에서 어디서 관측되는가

### Linux

```bash
# CPU 캐시 정보 확인
lscpu

# L2 크기 확인
lscpu | grep "L2 cache"

# 하드웨어 토폴로지 확인
lstopo
```

```bash
# Cache Miss 분석
perf stat
```

주요 항목:
- `cache-misses`
- `cache-references`
- `L1-dcache-load-misses`

```bash
# CPU 병목 분석
perf top
```

### Runtime

주요 관측 포인트:

- Cache Hit Ratio
- Memory Access Pattern
- CPU Stall
- IPC
- Branch Miss
- Working Set Size

> **핵심 질문:** 실행 데이터가 L2 안에 유지되는가?

### Kubernetes

K8s는 직접 L2를 보여주진 않지만 영향은 매우 큽니다.

```bash
# 노드 CPU 상태
kubectl top node

# Pod CPU 사용량
kubectl top pod

# NUMA / CPU 토폴로지 확인
kubectl describe node

# 컨테이너 내부 분석
kubectl exec <pod> -- perf stat
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*