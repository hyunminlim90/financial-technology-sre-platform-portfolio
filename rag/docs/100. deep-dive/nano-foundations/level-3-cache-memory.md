# 레벨 3 캐시 메모리 (Level 3 Cache Memory / L3 Cache)

> 정독: 0회

L3 캐시는:

> CPU 내부에서 **모든 CPU 코어가 함께 공유하는 최종 고속 캐시 계층**

**쉽게 말하면:**

"DRAM까지 내려가기 전에 CPU 전체가 마지막으로 확인하는 거대한 공동 캐시 창고"

<details>
<summary>Deep Dive</summary></br>

Processing Core(프로세싱 코어) [[M]](../../100-deep-dive/micro-foundations/processing-core.md)  

</details></br>

## 1. 이 기술이 무엇인가

L3 캐시는 **Level 3 Cache Memory**이며, 흔히 **LLC (Last Level Cache)** 라고도 부릅니다.

### 특징

| 항목 | 특징 |
|------|------|
| 위치 | CPU 다이 내부 |
| 구조 | 모든 코어 공유 |
| 구현 | SRAM |
| 속도 | L1/L2보다 느림 |
| 용량 | 캐시 중 가장 큼 |
| 목적 | DRAM 접근 최소화 |

**핵심 역할:** 멀티코어 전체의 DRAM 병목 방어

---

## 2. 시스템 어디에서 등장하는가

### 메모리 계층 구조

```
Register
↓
L1 Cache
↓
L2 Cache
↓
L3 Cache   ← 현재 주제
↓
DRAM
↓
SSD
```

### CPU 데이터 탐색 흐름

```
L1 탐색 → L2 탐색 → L3 탐색 → DRAM 접근
```

즉, **L3는 DRAM 직전의 마지막 고속 방어선**입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향 |
|------|------|
| CPU Stall | 매우 큼 |
| DRAM Traffic | 매우 큼 |
| Memory Latency | 매우 큼 |
| Multi-Core Throughput | 매우 큼 |
| Cache Coherency | 매우 큼 |

특히 **멀티코어 시스템 전체 효율**에 큰 영향을 줍니다.

---

## 4. 왜 중요한가

멀티코어 환경에서는 여러 CPU 코어가 동시에 데이터를 요청합니다.

L3가 없다면 모든 코어가 DRAM으로 직접 접근하게 되어:

```
메모리 버스 병목 → Latency 증가 → CPU Stall 증가
```

L3는 이를 완화합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. L3 Cache Miss 폭증

L3에도 데이터가 없으면 DRAM 접근이 급증합니다.

**결과:** CPU 대기 증가 → Tail Latency 증가 → TPS 하락

### 5-2. 멀티코어 경쟁

L3는 공유 자원이므로 코어 수 증가와 메모리 사용량 증가 시 **L3 경쟁**이 증가합니다.

**결과:** Cache Eviction 증가

### 5-3. Cache Thrashing

Working Set이 L3보다 커지면 캐시 교체가 폭증합니다.

**결과:** 계속 DRAM 재접근

### 5-4. NUMA 환경 병목

멀티소켓 서버에서는 다른 CPU 소켓의 L3에 접근하는 경우가 발생할 수 있습니다.

**결과:** 원격 메모리 접근(Remote Access) 비용 증가

### 5-5. Context Switch 증가

프로세스가 자주 바뀌면 L3 캐시 워밍 데이터가 손실됩니다.

**결과:** Cache Warm-up 반복 → 성능 저하

---

## 6. 핵심 메커니즘

### 6-1. L3는 모든 코어가 공유한다

- L1/L2: 코어 전용 (Private)
- L3: CPU 전체 공유 (Shared)

즉, Core 0의 데이터를 Core 1이 재사용할 수 있습니다.

### 6-2. L3는 DRAM 병목을 줄인다

L3 Hit 발생 시 칩 내부에서 데이터를 해결합니다.

**효과:** 외부 메모리 버스 접근 감소

### 6-3. L3는 용량 중심 캐시다

| 계층 | 특징 |
|------|------|
| L1 | 속도 최우선 |
| L2 | 속도 + 용량 균형 |
| L3 | 용량 중심 |

L3는 **수십 MB ~ 수백 MB**까지 커질 수 있습니다.

### 6-4. L3도 SRAM 기반이다

L3 역시 **SRAM**으로 구현되어 DRAM보다 훨씬 빠릅니다.

다만 용량 증가 → 회로 거리 증가 → 접근 속도 감소로 인해 L1/L2보다는 느립니다.

### 6-5. L3는 Cache Line 단위로 관리한다

데이터는 **64 Byte Cache Line** 단위로 이동합니다.  
필요 데이터 주변까지 통째로 캐시에 적재합니다.

### 6-6. L3는 코어 간 데이터 공유를 돕는다

```
Core 0이 계산한 데이터 → L3 저장 → Core 1이 재사용
```

**효과:** 코어 간 중복 DRAM 접근 감소

### 6-7. L3는 Last Level Cache다

L3 Miss 발생 시 거의 무조건 DRAM에 접근합니다.

따라서 **L3 Hit Ratio**는 시스템 성능에 매우 중요합니다.

---

## 7. Linux / Runtime / K8s 에서 어디서 관측되는가

### Linux

```bash
# L3 캐시 크기 확인
lscpu | grep "L3 cache"

# CPU 캐시 토폴로지 확인
lstopo
```

```bash
# 캐시 미스 분석
perf stat
```

주요 항목:
- `cache-misses`
- `cache-references`
- `LLC-load-misses`
- `LLC-store-misses`

> LLC = Last Level Cache (L3)

```bash
# CPU 병목 분석
perf top
```

### Runtime

주요 관측 포인트:

- LLC Miss Ratio
- Working Set Size
- Memory Access Pattern
- NUMA Access
- CPU Stall
- IPC

> **핵심 질문:** 실행 데이터가 L3 안에 유지되는가?

### Kubernetes

K8s는 직접 L3를 보여주진 않지만 영향은 큽니다.

```bash
# Node CPU 상태
kubectl top node

# NUMA 구조 확인
kubectl describe node

# 컨테이너 내부 perf 분석
kubectl exec <pod> -- perf stat
```

> **CPU Pinning 환경** (Guaranteed QoS + CPU Manager Static Policy)에서 특히 중요합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*