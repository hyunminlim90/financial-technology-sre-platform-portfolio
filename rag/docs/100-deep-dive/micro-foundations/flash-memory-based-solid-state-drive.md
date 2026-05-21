# 플래시 메모리 기반 솔리드 스테이트 드라이브 (Flash SSD)

> 정독: 0회

## 1. 이 기술이 무엇인가

Flash SSD(Flash Memory-based Solid State Drive)는:

> **낸드 플래시 반도체를 사용하여 데이터를 저장하는 비휘발성 고속 저장 장치**

### 핵심 특징

기계식 회전 디스크(HDD)와 다르게 모터, 플래터, 헤드 이동이 없습니다. 오직 **반도체 셀 내부 전하 상태**로 비트를 저장합니다.

### 핵심 구성 요소

| 구성 요소 | 역할 |
|---|---|
| NAND Flash | 실제 데이터 저장 |
| SSD Controller | 주소 변환 및 관리 |
| DRAM Cache | 매핑 캐시 |
| Host Interface | PCIe / SATA / NVMe 연결 |

### 저장 특성

Flash SSD는 전원이 꺼져도 데이터를 유지할 수 있는 **비휘발성 저장장치**입니다.

<details>
<summary>Deep Dive</summary></br>

NAND Flash Memory(낸드 플래시 메모리) [[M]](../../100-deep-dive/micro-foundations/nand-flash-memory.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

Flash SSD는 거의 모든 현대 시스템 스토리지 계층에 존재합니다.

### 등장 영역

| 영역 | 사용 형태 |
|---|---|
| 개인 PC | NVMe SSD |
| 서버 | Enterprise SSD |
| Cloud Storage | Distributed SSD Pool |
| Kubernetes Node | Local Persistent Storage |
| Database System | WAL / Data File Storage |
| AI Infrastructure | Dataset Storage |
| CDN Cache | High-speed Cache Layer |

### 데이터센터 환경

하이퍼스케일 데이터센터에서는 수천~수만 개 SSD를 분산 스토리지로 결합하여 사용합니다.

### 클라우드 환경

다음 대부분이 SSD 기반입니다:

- VM Root Volume / Object Storage Cache / Database Volume
- Ceph / SAN / NVMe-oF / Distributed Filesystem

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Disk I/O**이지만, 실제로는 전체 시스템에 영향을 미칩니다.

| 자원 | 영향 |
|---|---|
| Disk | 핵심 영향 |
| CPU | interrupt / queue 처리 |
| Memory | page cache / buffer |
| Network | distributed storage traffic |

### CPU 영향이 있는 이유

고속 SSD는 CPU가 I/O 처리를 따라가지 못하는 상황까지 발생시킵니다. 특히 다음이 CPU 사용량 증가 원인입니다:

- NVMe queue / interrupt
- checksum / compression
- filesystem journal

---

## 4. 왜 중요한가

Flash SSD는 현대 시스템 성능을 결정하는 **핵심 저장소**입니다.

### HDD와 가장 큰 차이

Flash SSD는 **기계적 탐색 시간(Seek Time)이 거의 없습니다.**

### 결과적으로 가능한 것

- 고속 random I/O / 낮은 latency / 높은 IOPS
- 빠른 boot / DB 응답 / VM 시작 / container startup

### 현대 시스템에서 중요 이유

다음 시스템은 SSD 없이는 성능 확보가 매우 어렵습니다:

- Kubernetes / Distributed DB / AI Training
- Kafka / Elasticsearch / Ceph
- High-frequency trading / Payment system

> **핵심 의미:** Flash SSD는 현대 분산 시스템의 실질적인 저장 성능 기반입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

SSD는 매우 빠르지만 독특한 장애 특성이 존재합니다.

### 대표 장애 유형

| 장애 유형 | 설명 |
|---|---|
| Write Amplification | 실제 쓰기보다 내부 쓰기량 증가 |
| Garbage Collection Stall | 내부 정리 작업 중 latency 급증 |
| Wear-out | 셀 수명 고갈 |
| Thermal Throttling | 고온 시 속도 강제 저하 |
| DRAM Cache Failure | 성능 급감 |
| NAND Corruption | ECC 복구 실패 시 데이터 손상 |

### 실제 시스템 증상

| 현상 | 원인 |
|---|---|
| latency spike | GC stall |
| disk wait 증가 | SSD saturation |
| pod restart 지연 | storage bottleneck |
| DB timeout | high write amplification |
| node instability | NVMe reset |
| IOPS 급감 | thermal throttling |

### Kubernetes 환경

```
local SSD saturation
→ etcd latency 증가
→ cluster instability
```

### Database 환경

SSD 특유 현상으로 fsync latency spike, WAL flush delay, compaction bottleneck이 발생할 수 있습니다.

---

## 6. 핵심 메커니즘

핵심 메커니즘은 **논리 주소와 물리 NAND 위치가 직접 일치하지 않는다**는 것입니다.

### SSD 핵심 구조

OS는 논리 블록 주소(LBA)만 봅니다. 실제 내부에서는 SSD Controller가 다음과 같이 변환합니다:

```
LBA
↓
FTL (Flash Translation Layer)
↓
실제 NAND 물리 위치
```

### 왜 필요한가

NAND Flash는 다음 특성이 있기 때문입니다:

- overwrite 불가
- erase-before-write 필요
- block 단위 erase 필요

### SSD Controller 핵심 역할

| 기능 | 설명 |
|---|---|
| FTL | 주소 변환 |
| Wear Leveling | 셀 수명 균등화 |
| Garbage Collection | 불필요 블록 정리 |
| ECC | 오류 복구 |
| Queue Scheduling | I/O 처리 |

> **핵심 특징:** SSD는 단순 저장 장치가 아니라, 내부에 독립적인 프로세서와 운영 로직을 가진 시스템입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

| 도구 | 용도 |
|---|---|
| `lsblk` | 장치 확인 |
| `nvme list` | NVMe 상태 |
| `iostat -x 1` | 디스크 성능 |
| `cat /sys/block/nvme0n1/queue/nr_requests` | queue depth |
| `smartctl -a` | SSD 정보 |

### 핵심 관측 항목

| 항목 | 의미 |
|---|---|
| IOPS | 초당 I/O 처리 |
| latency | 응답 시간 |
| await | 평균 대기 |
| util | 장치 사용률 |
| queue depth | 대기 큐 |
| write amplification | 내부 쓰기 증가 |
| temperature | 발열 상태 |

### Kubernetes

- **Local PV** — SSD를 PersistentVolume으로 사용
- **etcd** — SSD latency가 매우 중요
- **CSI Storage** — 대부분 SSD 기반

### 분산 시스템에서 관측 포인트

| 시스템 | SSD 영향 |
|---|---|
| Kafka | fsync latency |
| Elasticsearch | segment merge |
| Ceph | replication I/O |
| PostgreSQL | WAL flush |
| Redis AOF | append latency |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*