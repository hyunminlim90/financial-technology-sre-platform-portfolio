# Write-back (라이트백)
## **Micro Foundations — 컴퓨터 구조 / 시스템 성능 관점**

> 정독: 0회

## 1. 이 기술이 무엇인가

**Write-back**은:

> 연산 결과나 변경된 데이터를 **최종 저장 위치에 반영하는 과정**

실제 시스템에서는 **두 가지 의미**로 사용된다.

### A. CPU 파이프라인의 Write-back

명령어 실행 후 ALU 결과를 **레지스터에 기록하는 단계**이다.

```
Execute 결과 → CPU 상태로 공식 확정
```

### B. 캐시 메모리의 Write-back

캐시에만 먼저 쓰고, **RAM 반영은 나중에 지연시키는 고성능 메모리 정책**이다.

> **핵심:** Write-back은 **"최종 상태 반영"**의 개념이다.

---

## 2. 시스템 어디에서 등장하는가

### CPU 내부
- 명령어 사이클 마지막 단계: `Execute → Write-back`
- 예: `R1 = R2 + R3` ALU 연산 후 결과를 R1에 기록

### 캐시 계층
- L1 / L2 / L3 cache에서 매우 중요

### 메모리 계층
- CPU ↔ Cache ↔ RAM 사이의 쓰기 정책 핵심

### 스토리지 계층
- RAID cache, SSD controller cache, filesystem page cache

### 네트워크 장비
- NIC buffer/cache에서도 유사 개념 사용

> **결론:** Write-back은 **모든 고성능 시스템의 지연 완화 핵심 전략**이다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 큰 영향은 **Memory**이다.

| 자원 | 영향 |
|------|------|
| **Memory** | cache hierarchy, RAM latency, memory bandwidth — 가장 핵심 |
| **CPU** | Write-back stall은 CPU pipeline 전체를 멈출 수 있음 |
| **Disk** | IOPS, fsync latency, journaling 성능에 직접 영향 |
| **Network** | packet buffer flush, DMA write-back (간접 영향) |

> **핵심:** Write-back은 **메모리와 저장장치 latency를 숨기기 위한 기술**이다.

---

## 4. 왜 중요한가

CPU는 엄청 빠르고, RAM/디스크는 상대적으로 느리다. 매번 즉시 RAM까지 쓰면 **CPU가 계속 대기(stall)**하게 된다.

Write-back은 **느린 저장 반영을 뒤로 미뤄 CPU 처리량을 유지**한다.

효과:

- throughput 증가
- pipeline stall 감소
- latency hiding
- bus contention 감소

> **결론:** Write-back은 **현대 CPU 성능 핵심 중 하나**다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 1) Dirty Data Loss
```
write-back 상태에서 RAM/디스크 반영 전 장애 발생
  ↓
데이터 유실 (전원 장애, kernel panic, SSD controller crash)
```

### 2) Cache Coherency 문제
```
멀티코어 환경 — 한 코어 cache만 최신 상태 가능
  ↓
stale data → race condition → consistency 문제
```

### 3) Write-back Storm
```
dirty page 폭증 → 한꺼번에 flush 발생
  ↓
I/O spike → latency 폭증 → 시스템 멈춤처럼 보임
```

### 4) Pipeline Stall
```
레지스터 write-back 충돌
  ↓
pipeline 정체
```

### 5) Store Buffer Saturation
```
CPU 내부 write queue 가득 참
  ↓
execute stall → throughput 감소
```

### 6) Filesystem Flush Latency
```
page cache flush 시 disk I/O saturation 발생
  ↓
application latency 증가
```

> **핵심:** Write-back은 빠르지만, **반영 지연으로 인해 consistency 위험**을 가진다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### A. CPU Pipeline Write-back 흐름

```
Execute 결과 → 내부 버스 → Register File 기록
```

| 핵심 요소 | 의미 |
|-----------|------|
| **RegWrite signal** | 쓰기 허용 신호 |
| **Latch** | 클록 타이밍에 맞춰 비트 고정 |
| **Clock edge** | 동기화 기준점 |
| **Pipeline register** | 단계 간 임시 보관 |

### B. Cache Write-back 흐름

```
CPU write → cache만 수정 → dirty bit 설정 → 나중에 RAM flush
```

| 핵심 요소 | 의미 |
|-----------|------|
| **Dirty Bit** | RAM과 cache 데이터가 다름을 표시 |
| **Flush** | dirty data를 실제 RAM에 반영 |
| **Eviction** | cache 공간 부족 시 dirty block을 RAM에 반영 후 제거 |

### Write-back vs Write-through

| 정책 | 방식 | 특징 |
|------|------|------|
| **Write-back** | cache 우선 기록, RAM은 나중 반영 | 빠름, consistency 위험 존재 |
| **Write-through** | cache + RAM 즉시 동시 기록 | 느림, consistency 보장 |

> **핵심:** Write-back은 **즉시 반영보다 성능 최적화를 우선하는 정책**이다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**Memory — Dirty/Writeback 상태**
```bash
vmstat
free
cat /proc/meminfo | grep -E "Dirty|Writeback"
```
관찰: Dirty(캐시에만 있는 미반영 데이터), Writeback(현재 flush 중인 데이터)

**디스크 flush 관측**
```bash
iostat
iotop
sar -d
```

**Page Cache write-back 튜닝**
```bash
sync
# /proc/sys/vm/* 파라미터 조정
```

---

### Runtime

관찰 포인트:
- dirty page 증가 추이
- flush spike 빈도
- CPU write-back stall (perf)

---

### Kubernetes

| 관찰 영역 | 명령어 | 관찰 |
|-----------|--------|------|
| **Pod latency** | `kubectl top pod` | 대량 flush 시 latency 증가 |
| **노드 I/O** | `kubectl describe node` | disk pressure, node I/O saturation |

> **핵심:** 운영 환경에서는 **Dirty page 증가와 flush spike** 형태로 자주 드러난다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*