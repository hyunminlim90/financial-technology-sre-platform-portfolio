# Virtual Machine Monitor (VMM, 가상 머신 모니터)
## 1. 가상 머신 모니터(VMM)란 무엇인가

> 정독: 0회

가상 머신 모니터(Virtual Machine Monitor, VMM)는:

> 하나의 물리 하드웨어 위에서 여러 개의 독립된 가상 머신(VM)을 생성·실행·격리·제어하는 가상화 제어 계층

"하나의 물리 서버를 여러 개의 독립 컴퓨터처럼 분할하는 제어 시스템"

그리고: **VMM = Hypervisor**

**핵심 역할:**

| 역할 | 설명 |
|---|---|
| CPU 가상화 | 여러 VM에 CPU 분배 |
| Memory 가상화 | VM별 메모리 격리 |
| Device 가상화 | 가상 NIC/Disk 제공 |
| Isolation | VM 간 침범 차단 |
| Scheduling | 물리 자원 중재 |

> VMM은 "가상 컴퓨터들의 운영 관리자"입니다.

---

## 2. 시스템 어디에서 등장하는가

VMM은 물리 하드웨어와 게스트 운영체제 사이에 위치합니다.

**전체 구조:**

```
Physical Hardware
↓
VMM / Hypervisor
↓
Guest OS
↓
Applications
```

현대 CPU에서는:

```
VMX Root Mode     → VMM 실행
VMX Non-Root Mode → Guest OS 실행
```

**대표 등장 위치:**

| 계층 | 역할 |
|---|---|
| Hypervisor Layer | VM 통제 |
| VM Scheduler | CPU 분배 |
| Virtual Memory Manager | 메모리 가상화 |
| Virtual Device Layer | NIC/Disk 에뮬레이션 |
| VM Exit Handler | 민감 명령 가로채기 |

> VMM은 물리 시스템 전체의 "가상화 관리자"입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

VMM은 사실상 모든 핵심 자원과 강하게 연결됩니다.

| 자원 | 영향도 |
|---|---|
| CPU | 매우 큼 |
| Memory | 매우 큼 |
| Network | 매우 큼 |
| Disk | 매우 큼 |

**CPU 영향**

vCPU Scheduling, Context Switch, VM Exit / Entry, CPU Time Slice 관리를 수행합니다. 즉 CPU 가상화 핵심입니다.

**Memory 영향**

- Guest Physical Memory
- Nested Paging
- Memory Isolation
- Ballooning
- Huge Page

**Network 영향**

- Virtual NIC
- vSwitch
- SR-IOV
- Packet Routing

**Disk 영향**

- Virtual Disk
- Snapshot
- Thin Provisioning
- Storage Virtualization

---

## 4. 왜 중요한가

현대 클라우드·서버 인프라 대부분이 VMM 기반이기 때문입니다.

| 환경 | VMM 사용 |
|---|---|
| AWS EC2 | 사용 |
| Azure VM | 사용 |
| GCP VM | 사용 |
| VMware Cluster | 사용 |
| 사내 VM 인프라 | 사용 |

> 현대 데이터센터는 사실상 VMM 기반 위에서 동작한다.

**대표 효과:**

| 효과 | 설명 |
|---|---|
| 서버 통합 | 물리 서버 절감 |
| Isolation | VM 격리 |
| Snapshot | 빠른 복구 |
| Migration | 라이브 이동 |
| Resource Sharing | 자원 효율화 |
| Cloud Computing | 핵심 기반 |

---

## 5. 실제 장애와 어떤 관련이 있는가

| 장애 | 원인 |
|---|---|
| VM Freeze | Hypervisor Stall |
| CPU Ready 증가 | vCPU 경쟁 |
| Ballooning | Memory Pressure |
| Storage Latency | Shared IO 병목 |
| VM Exit Storm | 과도한 Trap |
| Noisy Neighbor | 자원 경쟁 |
| Live Migration 실패 | Hypervisor 문제 |
| VM Escape | 보안 취약점 |

**특히 중요한 점:**

> 가상화 환경에서는 "내 VM 문제"가 아니라 Hypervisor 계층 병목인 경우가 많다.

---

## 6. 핵심 메커니즘

### 6-1. Guest OS는 자신이 진짜 머신이라고 착각한다

```
Guest OS
→ 자신이 Ring 0이라고 생각
→ 실제론 VMM 아래에서 제한됨
```

Guest OS는 "가상 하드웨어 환상" 위에서 동작합니다.

### 6-2. 민감 명령은 VM Exit로 가로채진다

```
Guest OS privileged instruction
→ VM Exit 발생
→ VMM 제어권 획득
→ 검사/에뮬레이션
→ VM Entry 복귀
```

하드웨어가 특권 명령, Page Table 변경, IO 접근 등을 VMM으로 강제 전달합니다.

### 6-3. VMM은 CPU를 시간 분할한다

```
Physical CPU 8 Core
↓
VM1: 4 vCPU
VM2: 2 vCPU
VM3: 6 vCPU
```

실제 CPU 시간을 스케줄링합니다.

### 6-4. 메모리도 가상화된다

```
Guest Virtual Address
→ Guest Physical Address
→ Host Physical Address
```

주소 변환이 한 단계 더 추가됩니다. 이것이 EPT, NPT, Nested Paging 같은 기술입니다.

### 6-5. VMM은 VM 간 완전 격리를 목표로 한다

```
VM1 메모리 ≠ VM2 메모리
```

하나의 VM 장애가 다른 VM으로 퍼지지 않도록 설계됩니다.

---

## 7. Linux / Runtime / Kubernetes에서 관측 방법

**Hypervisor 확인**

```bash
lscpu
systemd-detect-virt
# 확인: KVM, VMware, Hyper-V, Xen
```

**KVM 상태 확인**

```bash
lsmod | grep kvm
virsh list
# 관측: VM 상태, Hypervisor 모듈
```

**VM 성능 관측**

```bash
top
htop
vmstat
iostat
# 관측: CPU Ready, IO Wait, Memory Pressure
```

**Kubernetes**

K8s Node 자체가 VM인 경우가 많습니다.

```
Cloud VM
→ Linux
→ Container Runtime
→ Kubernetes Pod
```

> Kubernetes 아래에도 VMM 계층이 존재하는 경우가 많습니다.

**Observability**

현대 시스템에서는 libvirt, Prometheus, eBPF, hypervisor telemetry 등으로 VM 상태를 추적합니다.

| 메트릭 | 의미 |
|---|---|
| CPU Ready Time | vCPU 대기 |
| VM Exit Rate | Trap 빈도 |
| Balloon Usage | 메모리 압박 |
| IO Wait | Storage 병목 |
| Packet Drop | Virtual NIC 병목 |
| Context Switch | 스케줄링 비용 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*