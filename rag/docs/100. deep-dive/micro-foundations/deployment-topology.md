# Deployment Topology (배치 토폴로지)
## **Micro Foundations — 범용 시스템/분산 인프라 관점**

> 정독: 0회

## 1. 이 기술이 무엇인가

**Deployment Topology(배치 토폴로지)**는:

> 시스템 컴포넌트들을 실제 인프라 공간 위에 **어떤 형태로 배치하고 연결할 것인가**를 정의한 구조

즉, 다음을 결정하는 **실제 운영 구조**다:

| 결정 항목 | 의미 |
|-----------|------|
| 어디에 배치할지 | 위치 |
| 몇 개를 배치할지 | 규모 |
| 어떻게 연결할지 | 통신 경로 |
| 어떻게 격리할지 | 장애 경계 |
| 장애 시 어떻게 살아남을지 | 생존 전략 |

> Architecture가 설계도라면, **Deployment Topology는 실제 도시 배치도**에 가깝다.

**예시:**

```
[ User ]
    │
    ▼
[ Load Balancer ]
   ┌───────┬───────┐
   ▼       ▼       ▼
[ WAS ] [ WAS ] [ WAS ]
   │       │       │
   └───────┬───────┘
           ▼
      [ Database Cluster ]
```

이 전체가 하나의 배치 토폴로지다.

---

## 2. 시스템 어디에서 등장하는가

Deployment Topology는 거의 **모든 인프라 계층**에서 등장한다.

### 물리 인프라 레벨
- 서버 랙 배치
- IDC 분산
- 전원 이중화
- 네트워크 스위치 구성

### 가상화 레벨
- VM placement
- Hypervisor cluster
- Virtual network segmentation

### 컨테이너/K8s 레벨
- Pod placement
- Node affinity
- Multi-zone deployment
- StatefulSet topology

### 클라우드 레벨
- Multi-AZ
- Multi-region
- Edge deployment
- CDN distribution

### 데이터베이스 레벨
- Primary/Replica
- Sharding
- Read replica topology

> **결론:** 배치 토폴로지는 **"컴포넌트를 현실 세계에 어떻게 배치할 것인가"**를 정의한다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

Deployment Topology는 모든 자원에 영향을 준다. 특히 **Network와 Failure Domain**에 가장 큰 영향을 준다.

### Network 영향
- **배치 위치 변화 시:** latency, bandwidth, packet loss, east-west traffic이 크게 달라진다
- **예:** 서울 ↔ 미국 리전 간 통신 → RTT 증가, synchronous delay 증가, timeout risk 증가

### CPU 영향
- **잘못된 배치 시:** 특정 node hotspot, uneven scheduling, noisy neighbor 발생

### Memory 영향
- **동일 노드에 memory-heavy workload 집중 시:** memory pressure, OOM risk 증가

### Disk 영향
- **동일 storage backend/SAN/disk rack 집중 시:** 하나의 physical fault가 전체 storage failure로 확산

---

## 4. 왜 중요한가

좋은 시스템이라도 **배치 토폴로지가 잘못되면 실제 운영 환경에서 쉽게 붕괴**한다.

현실 세계에서는 서버·스위치·네트워크·디스크·리전이 언제든 죽을 수 있기 때문이다.

> **배치 토폴로지는 "장애가 어디까지 전염되는가"를 결정한다.**

**좋은 토폴로지가 만드는 것:**
- fault isolation
- redundancy
- graceful degradation
- survivability

**나쁜 토폴로지가 만드는 것:**
- SPOF
- cascading failure
- correlated failure

> **핵심:** 배치 토폴로지는 **시스템의 실제 생존 구조**다.

---

## 5. 실제 장애와 어떤 관련이 있는가

배치 토폴로지 문제는 현실에서 매우 자주 장애 원인이 된다.

### 1) Hidden SPOF
```
논리적으로는 WAS 3대 구성처럼 보이지만,
실제론 동일 물리 서버 위 VM 3개
  ↓
물리 서버 1대 장애 → 전체 서비스 다운
```

### 2) Correlated Failure
```
동일 rack / 동일 power line / 동일 switch / 동일 AZ에 배치
  ↓
하나의 물리 fault가 다수 컴포넌트 동시 파괴
```

### 3) Network Partition
```
DB cluster node 간 packet loss
  ↓
split brain → replication lag → stale state
```

### 4) Latency Explosion
```
sync RPC across regions (멀리 떨어진 컴포넌트 배치)
  ↓
latency spike → timeout storm → retry amplification
```

### 5) Resource Concentration
```
특정 노드 집중 배치
  ↓
CPU hotspot → memory exhaustion → throttling
```

> **핵심:** 배치 토폴로지는 **"어디가 먼저 죽고, 어디까지 같이 죽는가"**를 결정한다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

Deployment Topology의 핵심은:

> **Failure Domain 분리**

**Failure Domain** = 같이 죽을 가능성이 있는 범위 (same server → same rack → same switch → same AZ → same region)

좋은 토폴로지는 **중요 컴포넌트를 서로 다른 failure domain에 분산**한다.

### 핵심 메커니즘

| 메커니즘 | 의미 |
|----------|------|
| **Redundancy** | 복제본 다중화 |
| **Load Distribution** | 부하 분산 |
| **Isolation** | 물리적/논리적 격리 |
| **Affinity / Anti-Affinity** | 같이 둘지 분리할지 제어 |
| **Failover** | 장애 시 자동 대체 |
| **Replication** | 상태 복제 |
| **Geographic Distribution** | 지역 분산 |
| **Blast Radius Reduction** | 장애 영향 범위 최소화 |

> **핵심 철학:** 좋은 토폴로지는 **"하나가 죽어도 전체는 살아남는다"**를 목표로 한다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux / Physical Layer

**네트워크 경로**
```bash
traceroute
mtr
ping
```

**NIC / Link 상태**
```bash
ip link
ethtool
```

**디스크 및 NUMA**
```bash
lsblk
numactl --hardware
```

---

### Runtime

관찰 포인트:
- connection pool locality
- RPC latency
- queue routing
- service dependency graph

---

### Kubernetes

Deployment Topology의 핵심 관측 지점.

| 관찰 영역 | 명령어 | 관찰 |
|-----------|--------|------|
| **Pod 배치 확인** | `kubectl get pods -o wide` | pod concentration, zone imbalance |
| **Node 확인** | `kubectl get nodes` | node hotspot, restart clustering |
| **Anti-affinity 확인** | `kubectl describe pod` | affinity 규칙 적용 여부 |
| **Topology spread 확인** | `kubectl get deployment -o yaml` | cross-zone traffic, spread 설정 |

---

### 클라우드 환경

관찰 포인트:
- AZ distribution
- region placement
- load balancer routing
- replication topology

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*