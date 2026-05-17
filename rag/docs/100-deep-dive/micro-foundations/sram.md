# SRAM (Static Random Access Memory)

> 정독: 0회

SRAM(정적 임의 접근 메모리)은:

> 전원이 공급되는 동안 데이터를 리프레시(Refresh) 없이 유지할 수 있는 **초고속 휘발성 반도체 메모리**

현대 컴퓨터 시스템에서는 CPU 내부 캐시 메모리(L1/L2/L3)의 핵심 구현 소자로 사용됩니다.

> **"CPU 바로 옆에서 가장 자주 쓰는 데이터를 초고속으로 임시 저장하는 메모리"**

---

## 1. 이 기술이 무엇인가

**핵심 특징:** 매우 빠름 / 리프레시 불필요 / 비쌈 / 용량 작음 / 휘발성

**DRAM과의 핵심 차이:**

| 메모리 | 저장 방식 |
|--------|-----------|
| **SRAM** | 플립플롭 회로 상태 유지 |
| **DRAM** | 커패시터 전하 저장 |

SRAM은 보통 **6개의 트랜지스터(6T)** 를 이용한 플립플롭 구조로 1비트를 저장합니다.

---

## 2. 시스템 어디에서 등장하는가

SRAM은 시스템의 **가장 빠른 메모리 계층**에서 등장합니다.

```
Register
  ↓
L1 Cache   ← SRAM
  ↓
L2 Cache   ← SRAM
  ↓
L3 Cache   ← SRAM
  ↓
DRAM
  ↓
SSD
```

**대표 사용 위치:**

| 위치 | 역할 |
|------|------|
| CPU L1 Cache | 가장 빠른 명령어 / 데이터 저장 |
| CPU L2 / L3 Cache | DRAM 접근 완충 |
| NIC Buffer | 패킷 버퍼 |
| Router / Switch Buffer | 네트워크 큐 |
| Embedded System | 고속 제어 메모리 |

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향 |
|------|------|
| CPU | 가장 직접적 |
| Memory Latency | 매우 큼 |
| Memory Bandwidth | 큼 |
| Power Consumption | 영향 있음 |
| Silicon Area | 매우 큼 |

> **SRAM은 CPU가 DRAM을 기다리지 않게 만듭니다.**

SRAM이 없다면:

```
CPU → 매번 DRAM 접근 → 엄청난 Stall 발생
```

---

## 4. 왜 중요한가

**계층별 접근 속도 비교:**

| 계층 | 접근 시간 |
|------|-----------|
| Register | 수 ps ~ ns 이하 |
| L1 SRAM Cache | ~1ns |
| DRAM | 수십~100ns |
| SSD | μs ~ ms |

CPU는 DRAM보다 훨씬 빠르기 때문에, 이 차이를 그대로 두면 CPU 대부분의 시간이 **메모리 대기(Stall)** 에 사용됩니다.

SRAM 캐시는 자주 사용하는 데이터를 CPU 가까이에 저장하여 다음을 만들어냅니다.

```
Cache Hit 증가
  ↓
DRAM Access 감소
  ↓
CPU Stall 감소
```

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. Cache Miss 폭증

```
Cache Miss 증가
  ↓
DRAM Access 증가
  ↓
CPU Stall 증가
  ↓
Latency 증가
```

### 5-2. Random Access 병목

| 접근 패턴 | 캐시 효율 |
|----------|-----------|
| Sequential Access | Cache Friendly |
| Random Access | Cache Unfriendly |

### 5-3. Context Switch 증가

```
Context Switch
  ↓
Cache Pollution
  ↓
Cache Warm-up 증가
```

### 5-4. False Sharing

멀티코어 환경에서 동일 Cache Line을 여러 코어가 수정하면:

```
Cache Coherency Traffic 증가
  ↓
Performance Degradation
```

### 5-5. Branch Prediction 실패

```
Wrong Instruction Prefetch
  ↓
Pipeline Flush
  ↓
Cache Efficiency 감소
```

---

## 6. 핵심 메커니즘 요약

### 6-1. SRAM은 플립플롭 회로 기반이다

DRAM이 전하 저장 방식인 것과 달리, SRAM은 **교차 연결된 플립플롭 회로**로 상태를 유지합니다. 전원이 있는 동안 상태를 계속 유지할 수 있습니다.

### 6-2. SRAM은 Refresh가 필요 없다

| 메모리 | Refresh |
|--------|---------|
| DRAM | 전하 누설 → Refresh 필요 |
| SRAM | 회로 상태 유지 → Refresh 불필요 |

### 6-3. SRAM은 매우 빠르다

트랜지스터 스위칭만 수행하므로 DRAM처럼 전하 충전/방전 과정이 거의 필요하지 않습니다.

### 6-4. SRAM은 비싸고 작다

6T 구조로 인해 셀 크기가 크고 집적도가 낮아 가격이 비쌉니다. 대용량 메모리에는 부적합합니다.

### 6-5. SRAM은 캐시 메모리의 핵심이다

L1, L2, L3 Cache 모두 SRAM 계층입니다.

### 6-6. Cache Line 단위로 동작한다

SRAM 캐시는 보통 **64 Byte Cache Line** 단위로 데이터를 이동합니다. CPU는 필요한 1 Byte만이 아니라 주변 데이터까지 함께 가져와 **공간 지역성(Spatial Locality)** 을 활용합니다.

### 6-7. SRAM은 CPU 파이프라인 성능을 결정한다

```
현대 CPU 성능 ≈ 캐시 효율
```

CPU 속도가 빨라도 Cache Hit이 낮으면 전체 성능이 급격히 떨어집니다.

---

## 7. Linux / Runtime / K8s 에서 관측 방법

### Linux

SRAM 자체를 직접 보진 않지만 **캐시 효율을 간접 관측**합니다.

```bash
# CPU 캐시 정보 확인
lscpu

# 캐시 계층 확인
lscpu | grep cache

# CPU 성능 카운터 확인 (cache-misses, cache-references, branch-misses)
perf stat

# NUMA 및 메모리 접근 확인
numactl --hardware

# CPU 사용 패턴 확인
mpstat -P ALL 1
```

### Runtime

| 지표 | 핵심 질문 |
|------|-----------|
| Cache Hit Ratio / Miss Rate | CPU가 계산 중인가, 메모리를 기다리는가? |
| Memory Access Pattern | 접근 패턴이 캐시 친화적인가? |
| CPU Stall / IPC | 파이프라인이 효율적으로 동작하는가? |
| Branch Miss / Context Switch | 캐시 오염이 발생하고 있는가? |

### Kubernetes

K8s는 SRAM 자체를 직접 노출하지 않지만, CPU 캐시 영향은 간접적으로 드러납니다.

```bash
# 노드 CPU 상태
kubectl top node

# Pod CPU 사용량
kubectl top pod

# NUMA / CPU Pinning 환경 확인
kubectl describe node

# 컨테이너 내부에서 직접 분석
kubectl exec -it <pod> -- perf stat
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*