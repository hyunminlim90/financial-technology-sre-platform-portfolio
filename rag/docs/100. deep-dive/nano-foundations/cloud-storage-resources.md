# 클라우드 스토리지 자원 (Cloud Storage Resources)

> 정독: 0회

## 1. 이 기술이 무엇인가

클라우드 스토리지 자원은:

> 분산된 물리 저장 장치(SSD/HDD/NVMe)를 가상화 및 분산 저장 기술로 통합하여, 인터넷을 통해 데이터를 저장·조회·복제·백업할 수 있게 만든 대규모 저장 인프라 자원

### 핵심 역할

데이터를 저장 / 복제 / 보존 / 조회 / 백업 / 복구하는 역할을 수행합니다.

### 핵심 구성 대상

- 파일
- 이미지
- 로그
- 데이터베이스 데이터
- AI 모델
- 백업 데이터
- 객체(Object)
- 컨테이너 이미지

---

## 2. 시스템 어디에서 등장하는가

현대 시스템 거의 모든 영역에서 등장합니다.

### 클라우드 플랫폼

VM Disk, Snapshot, Object Storage

### Kubernetes

Persistent Volume, CSI, StatefulSet Storage

### 데이터베이스

WAL, transaction log, distributed storage

### AI 플랫폼

model checkpoint, dataset storage, vector storage

### CDN 및 미디어

image storage, video archive

### 백업 및 DR

snapshot, replication, disaster recovery

### 로그 및 Observability

log archive, telemetry retention

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

핵심은 **Disk + Network**입니다. 대규모 분산 환경에서는 Network 영향도 매우 큽니다.

| 자원 | 영향 | 주요 지표 |
|---|---|---|
| **Disk** | 매우 핵심 | IOPS, throughput, latency, durability |
| **Network** | 분산 저장 핵심 | replication traffic, storage synchronization, distributed consistency |
| **CPU** | 스토리지 엔진 처리 | compression, checksum, erasure coding |
| **Memory** | 캐시 성능 | page cache, buffer cache |

---

## 4. 왜 중요한가

스토리지는 **데이터 영속성(Persistence)의 핵심**입니다.

### 데이터 보존

전원 종료 후에도 데이터 유지.

### 장애 복구 기반

백업·복제·복구 핵심.

### 대규모 서비스 운영

클라우드 서비스 대부분 저장소 기반.

### AI 시대 핵심 인프라

대규모 모델·데이터셋 저장 필요.

### 분산 시스템 기반

현대 시스템 대부분 distributed storage 사용.

---

## 5. 실제 장애와 어떤 관련이 있는가

스토리지 장애는 **서비스 전체 장애**로 이어질 수 있습니다.

### Disk Failure

SSD/HDD 물리 고장. → data corruption, degraded mode

### Replication Failure

복제 실패. → data inconsistency, durability risk

### Network Partition

스토리지 노드 간 분리. → split brain, unavailable storage

### IOPS Saturation

디스크 처리 한계 도달. → latency spike, DB slowdown

### Metadata Server 장애

메타데이터 계층 장애. → filesystem inaccessible

### Object Storage 장애

오브젝트 조회 실패. → image/video/service failure

### Storage Controller Failure

스토리지 제어 계층 장애. → volume unavailable

### Snapshot / Backup 실패

복구 불가능 상태 발생 가능.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

### ① Persistence

데이터 영속성 유지. RAM과 가장 큰 차이.

### ② Replication

데이터 복제. 예: 3-replica, multi-AZ replication

### ③ Distributed Storage

여러 노드에 분산 저장. 예: Ceph, HDFS, distributed object storage

### ④ Object Storage

현대 클라우드 핵심 저장 방식.

- flat namespace
- metadata 기반 관리
- massive scalability

### ⑤ Block Storage

저지연 고성능 저장. 주요 사용: VM disk, DB storage

### ⑥ File Storage

POSIX 기반 공유 저장. 예: NFS, distributed filesystem

### ⑦ Erasure Coding

복제보다 효율적인 데이터 보호 기술. 스토리지 효율 향상.

### ⑧ Storage Virtualization

물리 디스크를 논리적으로 추상화. 예: volume abstraction, storage pool

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

**Disk 상태**

```bash
lsblk
df -h
mount
```

**I/O 성능**

```bash
iostat
iotop
fio
```

**NVMe 상태**

```bash
nvme list
smartctl
```

**Filesystem 상태**

```bash
dmesg
journalctl
```

### Runtime

스토리지 성능 직접 영향. 주요 지표: fsync latency, WAL flush, checkpoint

### Kubernetes

**주요 구성**

- PersistentVolume
- PersistentVolumeClaim
- StorageClass
- CSI Driver

**관측 명령어**

```bash
kubectl get pv
kubectl get pvc
kubectl describe pvc
```

**중요 상태**

- volume attach failure
- mount timeout
- storage pressure

### 분산 스토리지 시스템

예: Ceph, GlusterFS, Longhorn

관측 대상: replica state, rebalance, degraded cluster

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*