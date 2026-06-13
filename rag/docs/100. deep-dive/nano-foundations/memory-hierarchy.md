# 메모리 계층 구조 (Memory Hierarchy)

> 정독: 0회

메모리 계층 구조는:

> **CPU가 필요한 데이터를 가능한 가장 빠르게 공급받기 위해** 속도·용량·비용에 따라 기억장치를 계층적으로 배치한 구조입니다.

**핵심 목적:**

CPU가 DRAM이나 SSD를 기다리며 멈춰버리는 현상(Memory Wall)을 줄이는 것

---

## 1. 이 기술이 무엇인가

메모리 계층 구조는 **"빠르지만 비싼 메모리" ↔ "느리지만 큰 메모리"** 사이의 균형 구조입니다.

### 대표적인 계층

```
Register
↓
L1 Cache
↓
L2 Cache
↓
L3 Cache
↓
DRAM (Main Memory)
↓
SSD / HDD (Storage)
```

위로 갈수록 매우 빠르고, 매우 작고, 매우 비쌉니다.  
아래로 갈수록 느리고, 매우 크고, 저렴합니다.

---

## 2. 시스템 어디에서 등장하는가

메모리 계층 구조는 **CPU ↔ Memory ↔ Storage** 전체 데이터 유통망에 등장합니다.

특히 다음 모든 곳의 성능 핵심입니다.

- 프로그램 실행
- 데이터 처리
- DB 조회
- 파일 읽기
- 네트워크 패킷 처리
- 캐시 시스템

### 실제 데이터 이동 흐름

```
SSD → DRAM → L3 Cache → L2 Cache → L1 Cache → Register → ALU 연산
```

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향도 |
|------|--------|
| Memory | 절대적 |
| CPU | 매우 큼 |
| Disk | 매우 큼 |
| Network | 간접 영향 |

특히 **Memory Latency**를 지배합니다.

---

## 4. 왜 중요한가

현대 CPU는 엄청나게 빠르지만, DRAM은 상대적으로 느립니다.

| 계층 | 접근 속도 |
|------|-----------|
| CPU 연산 | 수 ns 이하 |
| DRAM | 수십~수백 ns |
| SSD | μs ~ ms |

데이터를 못 받으면 CPU는 아무것도 할 수 없습니다.

따라서:

> **"CPU 성능" = "메모리 계층이 얼마나 CPU를 안 굶기느냐"**

에 매우 가깝습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. Cache Miss 폭증

L1/L2/L3 캐시에 데이터가 없으면 DRAM 접근이 증가합니다.

**결과:** CPU Stall 증가

### 5-2. Memory Wall

CPU 속도는 빨라졌지만 메모리 속도 증가는 느립니다.

**결과:** CPU가 계속 메모리 대기 상태

### 5-3. Context Switch 폭증

프로세스/스레드 전환 시 기존 캐시 데이터가 무효화됩니다.

**결과:** Cache Pollution 발생

### 5-4. NUMA 병목

멀티소켓 서버에서는 다른 CPU의 메모리 접근이 더 느립니다.

**결과:** Remote Memory Access Latency 증가

### 5-5. Page Fault

DRAM에 필요한 페이지가 없으면 Disk Swap이 발생할 수 있습니다.

**결과:** 시스템 전체가 매우 느려짐

### 5-6. SSD I/O 병목

DRAM 부족 시 Swap이 증가합니다.

**결과:** Disk I/O 폭증

---

## 6. 핵심 메커니즘

### 6-1. CPU는 가장 가까운 메모리부터 찾는다

```
Register → L1 → L2 → L3 → DRAM → SSD
```

위 계층에서 찾으면 매우 빠르고, 아래로 갈수록 매우 느립니다.

### 6-2. 캐시는 SRAM 기반이다

L1/L2/L3는 **SRAM**을 사용합니다. 압도적으로 빠르기 때문입니다.

### 6-3. DRAM은 시스템 공식 메모리다

실행 중인 프로세스, 스택, 힙, 페이지 등이 저장됩니다.

### 6-4. SSD는 영구 저장 계층이다

SSD/HDD는 전원이 꺼져도 데이터가 유지됩니다. 즉, **실행 전 프로그램 저장소**입니다.

### 6-5. 지역성(Locality)이 핵심이다

캐시가 동작 가능한 이유는 두 가지 지역성에 있습니다.

**시간적 지역성:** 최근 사용 데이터는 곧 다시 사용될 가능성이 높다.  
**공간적 지역성:** 인접 데이터가 함께 사용될 가능성이 높다.

### 6-6. 캐시는 캐시 라인 단위로 움직인다

보통 **64 Byte** 단위로 이동합니다.  
데이터 하나만 가져오는 게 아니라 주변까지 함께 가져옵니다.

### 6-7. 메모리 계층은 전체 성능 구조다

성능은 단순 CPU GHz가 아니라 **메모리 접근 구조 전체**에 의해 결정됩니다.

즉, **CPU만 빠르면 되는 게 아닙니다.**

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# CPU 캐시 정보
lscpu

# NUMA 확인
numactl --hardware

# 메모리 상태
free -h

# 페이지 상태
vmstat 1

# 캐시/메모리 통계
cat /proc/meminfo

# Page Fault 확인
sar -B

# 캐시 미스 분석
perf stat
```

### Runtime

주요 관측 포인트:

- Cache Hit Ratio
- Cache Miss
- Memory Latency
- Page Fault
- Working Set
- Swap

### Kubernetes

```bash
# Pod 메모리 사용량
kubectl top pod

# Node 메모리 상태
kubectl top node

# OOMKilled 확인
kubectl describe pod
```

```yaml
# Memory Limit 설정
resources:
  limits:
    memory: "4Gi"
```

> **NUMA / HugePage** — 고성능 환경에서 사용

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*