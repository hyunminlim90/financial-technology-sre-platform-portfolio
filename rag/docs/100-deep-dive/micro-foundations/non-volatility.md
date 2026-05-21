# 비휘발성 (Non-volatility)

> 정독: 0회

## 1. 이 기술이 무엇인가

비휘발성(Non-volatility)은:

> **전원이 완전히 차단되어도 저장된 데이터가 유지되는 특성**

### 핵심 의미

메모리 내부의 비트 상태가 **전원 공급 없이도 물리적으로 유지됨**을 의미합니다.

### 대표적인 비휘발성 저장장치

| 종류 | 특징 |
|---|---|
| NAND Flash | SSD, USB |
| NOR Flash | 펌웨어 저장 |
| HDD | 자기 기록 유지 |
| EEPROM | 설정 저장 |
| MRAM | 자기 기반 저장 |
| Optane / PMem | 고속 비휘발성 메모리 |

### 반대 개념

**휘발성(Volatile):** 전원이 꺼지면 데이터가 즉시 소멸합니다. 예: DRAM, SRAM(Cache)

---

## 2. 시스템 어디에서 등장하는가

비휘발성은 거의 모든 저장 계층의 기반입니다.

### 시스템 계층 위치

| 계층 | 역할 |
|---|---|
| CPU Cache | 휘발성 |
| DRAM | 휘발성 |
| SSD / HDD | 비휘발성 |
| Object Storage | 비휘발성 |
| Backup Storage | 비휘발성 |

### 실제 사용 영역

**운영체제 저장** — bootloader, kernel image, filesystem metadata

**데이터 저장** — database, logs, WAL, snapshots, object storage

**클라우드 인프라** — Persistent Volume, Ceph, SAN/NAS, Distributed Storage

### Kubernetes

비휘발성이 필요한 영역:

- PersistentVolume / etcd
- Database Storage / Container Image Layer

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Disk / Storage 계층**이지만, 실제로는 메모리 계층 전체와 연결됩니다.

| 자원 | 영향 |
|---|---|
| Disk | 핵심 |
| Memory | persistence boundary |
| CPU | cache flush / ordering |
| Network | distributed persistence replication |

### 중요한 아키텍처 경계

컴퓨터 시스템에서 **휘발성 메모리 ↔ 비휘발성 저장소** 경계가 매우 중요합니다.

| 위치 | 전원 OFF 시 |
|---|---|
| CPU Cache | 소멸 |
| DRAM | 소멸 |
| SSD | 유지 |
| HDD | 유지 |

---

## 4. 왜 중요한가

비휘발성이 없다면 **시스템 종료 순간 모든 데이터가 사라집니다.**

### 비휘발성 덕분에 가능한 것

- 파일 저장 / 운영체제 유지 / DB 영속성
- 로그 복구 / 장애 복원 / snapshot / backup
- distributed consensus

### 특히 중요한 영역

**Database** — commit durability 보장 핵심

**Filesystem** — metadata consistency 유지

**Distributed System** — crash recovery 기반

> **핵심 의미:** 비휘발성은 시스템 상태를 재부팅 이후에도 유지하는 기반입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

비휘발성은 데이터 안정성과 직접 연결됩니다.

### 대표 장애 유형

| 장애 유형 | 설명 |
|---|---|
| Power Loss Corruption | 전원 차단 중 write 중단 |
| Partial Write | 일부 페이지만 기록 완료 |
| FTL Metadata Corruption | SSD 내부 mapping 손상 |
| Write Cache Loss | flush 전 데이터 유실 |
| NAND Wear-out | 셀 수명 고갈 |
| Bit Rot | 장기 저장 중 bit error 증가 |

### 실제 시스템 증상

| 증상 | 원인 |
|---|---|
| filesystem corruption | sudden power loss |
| DB recovery 반복 | WAL flush 실패 |
| etcd instability | storage latency |
| SSD read-only mode | wear limit |
| kernel I/O error | media failure |

> **데이터센터 핵심 이슈:** 특히 **Durability**와 **Crash Consistency** 문제가 매우 중요합니다.

---

## 6. 핵심 메커니즘

핵심 메커니즘은 **전하 상태 또는 물리 상태를 전원 없이 유지**하는 것입니다.

### NAND Flash 기준

셀 내부에 전자를 저장하고, 절연막으로 격리하여 장시간 유지합니다.

| 동작 | 방식 |
|---|---|
| 읽기 (Read) | 전류 흐름 특성 측정 |
| 쓰기 (Program) | 전하 주입 |
| 삭제 (Erase) | 전하 제거 |

### 매우 중요한 특징

NAND는 **Overwrite 불가능**, **Erase 후 Write 필요** 구조입니다.

### 결과적으로 SSD 내부에 필수인 것

- FTL / Wear Leveling / ECC / Garbage Collection

### 시스템 레벨 핵심

OS 입장에서는 단순 block device처럼 보이지만, **내부는 매우 복잡한 상태 머신**입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

| 도구 | 용도 |
|---|---|
| `lsblk` | 블록 장치 확인 |
| `nvme list` | NVMe 상태 |
| `smartctl -a /dev/nvme0n1` | SMART 정보 |

### 관측 포인트

| 항목 | 의미 |
|---|---|
| media error | 저장 오류 |
| wear level | 수명 |
| unsafe shutdown | 비정상 종료 |
| temperature | 발열 |
| latency | 저장 지연 |
| flush behavior | durability 성능 |

### Filesystem 계층

`dmesg` 에서 다음을 확인할 수 있습니다:

- I/O error / ext4 corruption / xfs recovery

### Kubernetes

- **etcd latency** — 스토리지 안정성 핵심
- **PV 장애** — storage backend 상태 영향
- **StatefulSet** — persistent storage 의존

### Database Runtime

특히 다음에서 비휘발성 안정성이 중요합니다:

- fsync / WAL / journaling / checkpoint

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*