# Data Storage
## 1. 데이터 저장이란 무엇인가

데이터 저장(Data Storage)은:

> 시스템이 처리한 결과나 입력 데이터를 나중에도 다시 사용할 수 있도록 **기억 장치에 기록하고 유지하는 과정**

**"데이터를 사라지지 않게 보관하는 것"**

컴퓨터 시스템은 데이터를 계속 생성하고 처리합니다. 데이터 저장은 그 중 **결과를 유지하는 단계**입니다.

```
Input → Processing → Storage → Reuse / Output
```

**저장의 형태 예시:**

- 메모리에 임시 저장
- 파일로 저장 / SSD에 영구 저장
- 데이터베이스 저장
- 로그 저장 / 캐시 저장

---

## 2. 시스템 어디에서 등장하는가

데이터 저장은 **메모리 계층 구조(Memory Hierarchy)** 전체에서 등장합니다.

```
Fast / Small / Expensive
  ↓  Register
  ↓  L1/L2/L3 Cache
  ↓  DRAM
  ↓  SSD
  ↓  HDD
  ↓  Object Storage
Slow / Large / Cheap
```

**운영체제 관점의 저장 흐름:**

```
Application
  ↓
System Call
  ↓
File System
  ↓
Block Layer
  ↓
Storage Device
```

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향 |
|------|------|
| Disk / SSD | 가장 직접적 영향 |
| Memory | Buffer / Cache / Page Cache |
| CPU | 압축, 암호화, 파일시스템 처리 |
| Network | 원격 저장소 / 분산 스토리지 |
| Cache | Write Buffer / Metadata Cache |

> 저장은 단순히 디스크만의 문제가 아닙니다. CPU 계산, Memory Buffer, Kernel Cache, IO Queue, Storage Controller, Filesystem Metadata가 **모두 개입**합니다.

---

## 4. 왜 중요한가

처리만 하고 저장하지 않으면 전원 종료, 프로세스 종료, 시스템 장애 시 데이터가 모두 사라질 수 있습니다.

> 저장은 **시스템의 기억 기능**이자 **신뢰성(Reliability)의 핵심**입니다.

**운영 관점의 핵심 판단 기준:**

- 데이터가 안전하게 저장되는가
- 저장 속도가 충분한가
- 장애 시 복구 가능한가
- 저장 일관성이 유지되는가
- 데이터 유실 가능성이 있는가

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. Disk IO Bottleneck

```
Write Queue 증가
  ↓
IO Wait 증가
  ↓
Application Stall
```

### 5-2. fsync / Flush 지연

```
Write
  ↓
Flush
  ↓
Disk Sync Wait
```

특히 **로그 시스템과 데이터베이스**에서 중요합니다.

### 5-3. Memory Pressure

```
Dirty Page 증가
  ↓
Memory Pressure
  ↓
Writeback 폭증
```

### 5-4. Storage Full

```
Disk 100%
  ↓
Write 실패
  ↓
Application Error
```

### 5-5. 데이터 손상 (Corruption)

```
Unexpected Power Loss
  ↓
Incomplete Write
  ↓
Corrupted Data
```

### 5-6. Write Amplification

```
Small Random Write
  ↓
Garbage Collection
  ↓
SSD Latency 증가
```

### 5-7. 분산 저장 지연

```
Storage Request
  ↓
Network RTT
  ↓
Replication Delay
```

---

## 6. 핵심 메커니즘 요약

### 6-1. 저장은 메모리 계층 전체에서 발생한다

| 계층 | 특성 |
|------|------|
| Register / Cache | 빠름, 소용량, 휘발성 |
| DRAM | 빠름, 중용량, 휘발성 |
| SSD | 중간, 대용량, 비휘발성 |
| HDD | 느림, 대용량, 비휘발성 |

### 6-2. DRAM은 빠르지만 휘발성이다

```
Power Off  →  DRAM Data Loss
```

영구 저장은 **SSD / HDD**가 담당합니다.

### 6-3. 저장은 결국 비트 상태 변화다

| 장치 | 물리 원리 |
|------|-----------|
| DRAM | 전하 저장 |
| SSD | NAND Flash 전자 상태 변경 |
| HDD | 자기장 방향 변경 |

### 6-4. CPU는 직접 SSD를 다루지 않는다

```
Application → Kernel → Filesystem → Block Layer → Device Driver → SSD/HDD
```

### 6-5. 캐시가 저장 성능을 크게 좌우한다

| 정책 | 동작 |
|------|------|
| **Write-Through** | 즉시 메모리/디스크에 반영 |
| **Write-Back** | 캐시에 먼저 저장 후 나중에 반영 |

### 6-6. 저장은 상대적으로 느린 작업이다

| 계층 | 속도 |
|------|------|
| CPU | ns 이하 |
| SSD | μs ~ ms |

그래서 **Buffer, Queue, Batch, Cache, Async Write** 같은 최적화가 중요합니다.

### 6-7. 저장은 일관성과 성능의 균형 문제다

| 목표 | 방법 |
|------|------|
| 빠른 저장 | Delay Write / Cache / Async Flush |
| 안전한 저장 | Immediate Flush / Sync Write / Replication |

> 두 목표는 종종 충돌합니다.

---

## 7. Linux / Runtime / K8s 에서 관측 방법

### Linux

```bash
# 디스크 사용량 확인
df -h

# 디스크 IO 확인 (await, util, svctm)
iostat -x 1

# 프로세스별 IO 확인
iotop

# 메모리 캐시 확인 (buff/cache)
free -h

# Dirty Page 확인 (Dirty, Writeback)
cat /proc/meminfo

# 파일시스템 사용 상태
mount
lsblk

# 열린 파일 확인
lsof
```

### Runtime

| 지표 | 핵심 질문 |
|------|-----------|
| Write Latency / Flush Time | 저장이 느린가? |
| Queue Length / Retry Count | 병목이 발생하고 있는가? |
| Buffer Usage / Cache Hit Ratio | 캐시가 효율적으로 작동하는가? |
| Disk Usage / Storage Throughput | 데이터 유실 가능성이 있는가? |

### Kubernetes

```bash
# PVC 상태 확인
kubectl get pvc

# 볼륨 마운트 확인
kubectl describe pod

# Pod 디스크 사용량 확인
kubectl exec -it <pod> -- df -h

# 노드 디스크 상태 확인
kubectl top node

# 로그 저장 상태 확인
kubectl logs <pod>

# 이벤트 확인
kubectl describe pod
```

**대표 상태:**

| 상태 | 의미 |
|------|------|
| `Evicted` | 디스크 또는 메모리 부족으로 Pod 축출 |
| `DiskPressure` | 노드 디스크 압박 조건 발동 |
| `ReadOnlyFilesystem` | 파일시스템 쓰기 불가 상태 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*