# 낸드 플래시 메모리 (NAND Flash Memory)

> 정독: 0회

## 1. 이 기술이 무엇인가

낸드 플래시 메모리는:

> **전원이 꺼져도 데이터를 유지하는 비휘발성 반도체 저장 메모리**

### 핵심 특징

데이터를 **전자(전하)를 반도체 셀 내부에 가두는 방식**으로 저장합니다.

### NAND 구조 특징

셀(Cell)을 **직렬(Series) 구조로 연결**하여 매우 높은 저장 밀도를 확보합니다. 이것이 현대 SSD가 수백 GB에서 수십 TB까지 대용량 구현이 가능한 핵심 기반입니다.

### 핵심 특성

| 특성 | 의미 |
|---|---|
| 비휘발성 | 전원 OFF 후에도 데이터 유지 |
| 고집적 | 매우 높은 저장 밀도 |
| 블록 기반 | erase-before-write 구조 |
| 반도체 기반 | 기계적 부품 없음 |

---

## 2. 시스템 어디에서 등장하는가

낸드 플래시는 현대 거의 모든 저장장치 내부에 존재합니다.

### 주요 사용 영역

| 영역 | 사용 형태 |
|---|---|
| SSD | 주 저장장치 |
| NVMe SSD | 고속 서버 스토리지 |
| USB | 이동식 저장장치 |
| SD Card | 모바일 저장 |
| Smartphone | 내장 저장소 |
| Cloud Storage | 분산 저장 계층 |
| Embedded System | 펌웨어 저장 |

### 데이터센터 환경

다음 대부분 내부적으로 NAND Flash 기반입니다:

- Enterprise SSD / NVMe Array / Distributed Storage
- Ceph OSD / Database Storage / Object Storage Cache

### Kubernetes 환경

주로 다음에서 사용됩니다:

- Persistent Volume
- Node Local Storage
- Container Image Layer

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Disk I/O 성능**입니다.

| 자원 | 영향 |
|---|---|
| Disk | 핵심 영향 |
| CPU | FTL / ECC 처리 |
| Memory | page cache / mapping |
| Network | distributed storage sync |

### NAND 특유 영향

특히 **랜덤 I/O 성능**과 **latency 특성**에 결정적인 영향을 미칩니다.

---

## 4. 왜 중요한가

현대 시스템 성능 대부분은 CPU보다 **Storage latency**에 의해 제한되는 경우가 많습니다.

### NAND Flash 등장 이후 가능해진 것

- 고속 DB / VM 대량 실행 / Container runtime
- Distributed storage / AI dataset loading / Real-time analytics

### 핵심 이유

기계적 이동 없이 **전자적 접근만으로 데이터 읽기/쓰기가 가능**하기 때문입니다.

### HDD 대비 차이

| 항목 | HDD | NAND Flash |
|---|---|---|
| 탐색 시간 | ms 단위 | μs 단위 |
| 랜덤 I/O | 매우 느림 | 매우 빠름 |
| 기계 부품 | 존재 | 없음 |
| 진동 영향 | 큼 | 적음 |

---

## 5. 실제 장애와 어떤 관련이 있는가

낸드 플래시는 매우 빠르지만 수명과 내부 관리 문제가 존재합니다.

### 핵심 장애 특성

| 장애 유형 | 설명 |
|---|---|
| Program/Erase Cycle Limit | 셀은 무한 쓰기 불가능 |
| Bit Error 증가 | 시간 경과 시 전하 누설 발생 가능 |
| Wear-out | 특정 블록 수명 고갈 |
| Garbage Collection Stall | 정리 작업 중 latency 증가 |
| Read Disturb | 지속 읽기 시 인접 셀 영향 가능 |

### 실제 시스템 증상

| 증상 | 원인 |
|---|---|
| latency spike | GC |
| SSD 성능 저하 | wear 증가 |
| I/O timeout | NAND error |
| data corruption | ECC failure |
| node instability | NVMe reset |
| DB 응답 저하 | write amplification |

> **데이터센터 중요 포인트:** 고성능 SSD도 쓰기 패턴이 나쁘면 급격히 성능 저하가 가능합니다.

---

## 6. 핵심 메커니즘

핵심 메커니즘은 **NAND는 overwrite가 불가능**하다는 점입니다.

### NAND 저장 구조

| 단위 | 설명 |
|---|---|
| Cell | 비트 저장 |
| Page | 읽기 / 쓰기 단위 |
| Block | 삭제 단위 |

NAND는 **Page 단위 write**, **Block 단위 erase** 구조를 가집니다.

### 결과적으로 발생하는 쓰기 과정

```
기존 데이터 직접 overwrite 불가
→ 새 위치에 기록
→ 이전 위치 invalid 처리
→ 나중에 block 전체 erase
```

### 그래서 SSD 내부에 반드시 필요한 것

SSD Controller가 다음을 수행합니다:

- FTL / Garbage Collection / Wear Leveling
- ECC / Address Mapping

> **핵심 의미:** Flash SSD 성능은 단순 NAND 속도가 아니라 **컨트롤러와 FTL 품질**에 매우 크게 좌우됩니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

| 도구 | 용도 |
|---|---|
| `lsblk` | 장치 확인 |
| `nvme list` | NVMe 상태 |
| `smartctl -a /dev/nvme0n1` | SMART 상태 |
| `iostat -x 1` | I/O 성능 |

### 중요한 관측 항목

| 항목 | 의미 |
|---|---|
| wear level | 수명 상태 |
| media error | NAND 오류 |
| temperature | 발열 |
| write amplification | 내부 쓰기 증가 |
| latency | 응답 시간 |
| utilization | 장치 포화도 |

### Kubernetes

- **etcd latency** — SSD 품질이 매우 중요
- **Local PV** — 노드 SSD 성능 영향 큼
- **Container Runtime** — image layer unpacking 속도 영향

### Database 환경

특히 다음에서 SSD 성능이 중요합니다:

- WAL / fsync / compaction / checkpoint

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*