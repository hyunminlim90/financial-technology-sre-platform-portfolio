# 레벨 1 캐시 메모리 (Level 1 Cache Memory / L1 Cache)

> 정독: 0회

L1 캐시는:

> CPU 코어 내부에 직접 붙어 있는 가장 빠른 캐시 메모리 계층입니다.

CPU가 연산할 때 필요한 명령어와 데이터를:

- DRAM까지 내려가지 않고
- CPU 바로 옆에서 즉시 공급

하기 위해 존재합니다.

> **쉽게 말하면:**
> "CPU가 지금 당장 사용할 데이터를 가장 가까운 곳에 미리 두는 초고속 메모리"

<details>
<summary>Deep Dive</summary></br>

Processing Core(프로세싱 코어) [[M]](../../100-deep-dive/micro-foundations/processing-core.md)  
Cache Memory(캐시 메모리) [[M]](../../100-deep-dive/micro-foundations/cache-memory.md)  

</details></br>

## 1. 이 기술이 무엇인가

L1 캐시는 **CPU 코어 내부 최상위 SRAM 캐시 계층**입니다.

### 특징

| 항목 | 특징 |
|------|------|
| 위치 | CPU 코어 내부 |
| 속도 | 가장 빠름 |
| 용량 | 매우 작음 |
| 구현 | SRAM 기반 |
| 접근 시간 | 약 1~4 CPU Cycle |
| 공유 여부 | 보통 코어 독점 |

**핵심 목적:** CPU가 DRAM 대기 때문에 멈추지 않게 하는 것

---

## 2. 시스템 어디에서 등장하는가

L1 캐시는 CPU 파이프라인 바로 옆에 존재합니다.

### 메모리 계층 구조

```
Register
↓
L1 Cache   ← 현재 주제
↓
L2 Cache
↓
L3 Cache
↓
DRAM
↓
SSD
```

CPU가 명령어를 실행할 때 **1순위로 L1 캐시를 확인**합니다.
즉, CPU와 가장 가까운 데이터 공급 창고입니다.

### L1I 와 L1D 분리

현대 CPU는 보통 L1 캐시를 둘로 나눕니다.

| 종류 | 역할 |
|------|------|
| L1I Cache | 명령어 저장 |
| L1D Cache | 데이터 저장 |

명령어 흐름과 데이터 흐름을 분리하여 CPU 파이프라인 충돌을 줄입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향도 |
|------|--------|
| CPU Pipeline | 매우 큼 |
| Memory Latency | 매우 큼 |
| IPC | 매우 큼 |
| DRAM Access | 직접 감소 |
| Power Efficiency | 영향 있음 |

**핵심:** L1 Hit 여부가 CPU 실행 속도를 결정합니다.

---

## 4. 왜 중요한가

CPU는 매우 빠르지만, DRAM은 상대적으로 느립니다.

| 계층 | 접근 시간 |
|------|-----------|
| Register | 거의 즉시 |
| L1 Cache | ~1ns 이하 |
| DRAM | 수십~100ns |

L1 캐시가 없으면:

```
명령어 하나 실행할 때마다
DRAM 접근
↓
CPU Stall 폭증
```

L1 캐시는 이를 막아 **CPU 연산 흐름 연속성**을 유지합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. L1 Cache Miss 증가

필요한 데이터가 L1에 없으면:

```
L1 Miss → L2 조회 → L3 조회 → DRAM 접근
```

**결과:** Latency 증가, CPU Stall 증가

### 5-2. Cache Thrashing

작은 L1 공간에 너무 많은 데이터가 들어오면 기존 캐시 데이터가 지속 제거됩니다.

**결과:** Hit Ratio 급락

### 5-3. False Sharing

멀티코어 환경에서 동일 Cache Line을 서로 수정하면 Cache Coherency Traffic이 증가합니다.  
특히 **L1D 캐시**에서 심각합니다.

### 5-4. Branch Prediction 실패

CPU는 미래 명령어를 미리 L1I에 적재합니다. 분기 예측 실패 시:

```
잘못 가져온 명령어 폐기
↓
Pipeline Flush
```

### 5-5. Context Switch 증가

프로세스 전환 시 기존 L1 캐시 데이터가 무효화됩니다.

**결과:** 새 프로세스 캐시 Warm-up 필요

---

## 6. 핵심 메커니즘

### 6-1. L1 캐시는 SRAM 기반이다

L1 캐시는 DRAM이 아닌 **SRAM**으로 구현됩니다. 이유는 압도적으로 빠르기 때문입니다.

### 6-2. CPU는 항상 L1을 먼저 본다

```
Register → L1 → L2 → L3 → DRAM
```

**L1 Hit가 성능 핵심**입니다.

### 6-3. L1은 매우 작다

보통 **32KB ~ 128KB** 수준입니다.  
빠를수록 회로 면적과 비용이 증가하기 때문입니다.

### 6-4. L1은 코어별 독점이다

각 CPU 코어마다 자기 전용 L1이 존재합니다.

```
Core 0의 L1  ≠  Core 1의 L1
```

### 6-5. L1I 와 L1D 분리는 매우 중요하다

명령어와 데이터를 분리함으로써 **Instruction Fetch**와 **Data Load/Store**가 동시에 수행 가능합니다.  
이는 CPU 파이프라인 효율을 크게 높입니다.

### 6-6. Cache Line 단위로 동작한다

L1 캐시는 보통 **64 Byte Cache Line** 단위로 동작합니다.

필요한 데이터 하나만이 아니라 주변 데이터도 함께 적재하며,  
이를 **Spatial Locality 활용**이라고 합니다.

### 6-7. CPU 파이프라인과 직접 연결된다

L1은 사실상 CPU 실행 엔진의 일부에 가깝습니다.

```
L1 Miss  =  CPU Pipeline 정지 위험
```

---

## 7. Linux / Runtime / K8s 에서 어디서 관측되는가

### Linux

```bash
# CPU 캐시 정보 확인
lscpu

# 캐시 상세 정보
lscpu | grep cache

# L1/L2/L3 구조 확인 (hwloc 패키지)
lstopo
```

```bash
# Cache Miss 분석
perf stat
```

주요 항목:
- `cache-references`
- `cache-misses`
- `L1-dcache-load-misses`
- `branch-misses`

```bash
# CPU 성능 병목 분석
perf top
```

### Runtime

주요 관측 포인트:

- Cache Hit Ratio
- IPC
- CPU Stall
- Branch Miss
- Memory Access Pattern

> **핵심 질문:** CPU가 계산 중인가? 아니면 메모리를 기다리는가?

### Kubernetes

K8s는 L1 자체를 직접 노출하진 않지만 간접 영향은 매우 큽니다.

```bash
# CPU 사용량 확인
kubectl top pod

# 노드 성능 상태
kubectl top node

# CPU Pinning / NUMA 환경
kubectl describe node

# 컨테이너 내부 성능 분석
kubectl exec <pod> -- perf stat
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*