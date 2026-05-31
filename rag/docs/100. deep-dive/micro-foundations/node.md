# 노드 (Node)
## 1. 노드(Node)란 무엇인가

> 정독: 0회

노드(Node)는:

> 네트워크 또는 시스템 안에서 데이터를 생성·처리·전달·저장할 수 있는 **하나의 연결 지점**

즉 컴퓨터 · 서버 · 스마트폰 · 스위치 · 라우터 · VM · 컨테이너 · Kubernetes Worker 까지 모두 노드가 될 수 있습니다.

핵심은:

> **"연결망 안에서 독립적으로 동작하는 하나의 점"**

이라는 것입니다.

---

## 2. 시스템 어디에서 등장하는가

노드는 거의 모든 컴퓨팅 시스템에서 등장합니다.

| 영역 | 노드 의미 |
|---|---|
| LAN | PC/스위치/공유기 |
| Internet | 서버/라우터 |
| Cloud | VM/Host |
| Kubernetes | Worker Node |
| Blockchain | 검증 참여 시스템 |
| Distributed System | 분산 서버 |
| Storage Cluster | 저장 노드 |
| AI Cluster | GPU 서버 |

**가장 중요한 개념**

노드는 단순 장비명이 아니라:

> "분산 시스템 안에서  
> **독립적으로 동작 가능한 단위**"

라는 추상화 개념입니다.

즉 물리 서버도 · VM도 · 컨테이너도 · Router도 노드가 될 수 있습니다.

---

## 3. 어떤 자원(CPU / Memory / Network / Disk)에 가장 영향이 큰가

노드는 모든 시스템 자원과 연결되지만, 특히 **Network 자원**과 가장 강하게 연결됩니다.

| 자원 | 영향도 |
|---|---|
| Network | 매우 큼 |
| CPU | 큼 |
| Memory | 큼 |
| Disk | 상황 의존 |

**왜 Network 영향이 큰가**

노드의 본질 자체가 **"다른 노드와 연결되어 통신하는 단위"** 이기 때문입니다.

즉 노드는 Packet 송수신 · RPC 호출 · Cluster Sync · Heartbeat · Replication 등을 수행합니다.

**CPU/Memory 영향**

노드는 결국 실행 단위입니다. 프로세스 실행 · 스케줄링 · 캐시 사용 · 메모리 관리 등과 연결됩니다.

**Disk 영향**

특히 DB Node · Storage Node · Kafka Broker · Ceph Node 같은 저장 중심 시스템에서는 Disk 영향이 매우 큽니다.

---

## 4. 왜 중요한가

현대 시스템은 거의 모두 **"여러 노드가 협력하는 구조"** 로 동작하기 때문입니다.

| 시스템 | 노드 구조 |
|---|---|
| Kubernetes | Multi Node Cluster |
| Cloud | Multi Host |
| Kafka | Broker Node |
| Cassandra | Distributed Node |
| Blockchain | Validation Node |
| CDN | Edge Node |

즉 **현대 인프라 = 노드들의 협력 시스템** 입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 운영 장애 상당수가 **노드 자체 문제** 또는 **노드 간 연결 문제**입니다.

| 장애 | 설명 |
|---|---|
| Node Failure | 서버 다운 |
| Node Partition | 네트워크 단절 |
| Split Brain | 클러스터 분리 |
| High Latency | 노드 응답 지연 |
| Packet Loss | 노드 통신 손실 |
| CPU Saturation | 노드 과부하 |
| Memory Exhaustion | OOM |
| Disk Failure | 저장 불능 |

**분산 시스템 핵심 문제**

> **"노드는 반드시 언젠가 죽는다."**

그래서 Failover · Replication · Consensus · Heartbeat · Retry 같은 기술이 등장합니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 6-1. 노드는 "독립 실행 단위"다

각 노드는 자체 CPU · 자체 Memory · 자체 Network Stack · 자체 OS를 가질 수 있습니다.

즉 **Node = 하나의 독립 시스템**입니다.

### 6-2. 노드는 서로 메시지를 주고받는다

```
Node A → Packet → Switch/Router → Node B
```

즉 노드 간 통신이 시스템의 핵심입니다.

### 6-3. 노드는 Address 기반으로 식별된다

| 계층 | 식별자 |
|---|---|
| L2 | MAC Address |
| L3 | IP Address |
| DNS | Hostname |
| Cluster | Node ID |

즉 네트워크는 노드 식별 체계 위에서 동작합니다.

### 6-4. 분산 시스템은 노드 상태를 계속 감시한다

대표 메커니즘: `Heartbeat` · `Health Check` · `Leader Election` · `Membership`

즉 **"누가 살아있는가"** 를 계속 추적합니다.

### 6-5. 노드 장애는 정상 상태로 간주된다

현대 분산 시스템 핵심 철학:

> 장애는 예외가 아니라  
> **기본적으로 발생한다고 가정한다.**

그래서 Redundancy · Multi Node · Quorum · Replica 구조를 사용합니다.

---

## 7. Linux / Runtime / Kubernetes에서 어디서 관측되는가

**Linux Host 자체가 노드다**

```bash
hostname
uname -a
ip addr
```

**네트워크 노드 확인**

```bash
ping
arp -a
ip neigh
```

관측 가능: Neighbor Node · MAC/IP 연결

**Cluster Node 상태**

```bash
ss
netstat
```

관측 가능: 연결 상태 · 포트 상태 · 세션 상태

**Kubernetes**

Kubernetes에서 Node는 핵심 개념입니다.

```bash
kubectl get nodes
```

| 구성 | 의미 |
|---|---|
| Control Plane Node | 클러스터 제어 |
| Worker Node | Pod 실행 |
| Edge Node | 외부 연결 |

**Distributed System**

대표 시스템: Kafka Broker · Cassandra Node · Elasticsearch Node · Redis Cluster Node

즉 대부분 **노드 기반 구조**입니다.

**Observability**

현대 운영에서는 `Node Exporter` · `Cluster Metrics` · `Health Check` · `Heartbeat` · `eBPF` 등으로 노드를 추적합니다.

| 메트릭 | 의미 |
|---|---|
| Node CPU | CPU 사용률 |
| Node Memory | 메모리 상태 |
| Node Network | 트래픽 |
| Node Latency | 응답 지연 |
| Node Availability | 생존 상태 |
| Packet Loss | 통신 품질 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*