# Hardware-enforced Security Policy (하드웨어 수립 보안 정책)
## 1. 하드웨어 수립 보안 정책이란 무엇인가

> 정독: 0회

하드웨어 수립 보안 정책(Hardware-enforced Security Policy)은:

> 운영체제나 하이퍼바이저가 정의한 보안 규칙을 CPU·MMU·칩셋 같은 하드웨어가 직접 이해하고 강제할 수 있도록 비트·레지스터·페이지 테이블 형태로 물리적으로 기록한 보안 정책 상태

```
소프트웨어의 보안 규칙
↓
하드웨어가 읽을 수 있는 비트 구조로 변환
↓
CPU/MMU가 실시간 강제 집행
```

**핵심:**

> "정책 선언"이 아니라, 실제 하드웨어가 집행 가능한 상태로 기록되어 있다는 점입니다.

**대표 예시:**

| 정책 | 하드웨어 표현 |
|---|---|
| Kernel 보호 | U/S Bit |
| Read-Only | R/W Bit |
| 실행 금지 | NX/XD Bit |
| 권한 레벨 | CPL / EL |
| DMA 제한 | IOMMU Table |
| Secure Boot | TPM/Signature State |

> 보안 정책이 실제 반도체 회로 판별 규칙으로 내려간 상태입니다.

<details>
<summary>Deep Dive</summary></br>

Operating System(운영체제)[[M]](../../100-deep-dive/micro-foundations/operating-system.md)  
Virtual Machine Monitor(가상 머신 모니터) [[M]](../../100-deep-dive/micro-foundations/virtual-machine-monitor.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

하드웨어 수립 보안 정책은 CPU가 명령어 실행, 메모리 접근, DMA 처리, 권한 전환 등을 수행하는 거의 모든 순간 등장합니다.

**전체 흐름:**

```
OS / Hypervisor
→ Security Rule 정의
→ Page Table / Control Register 설정
→ Hardware Policy 상태 생성
→ CPU/MMU가 실시간 검사
```

> 운영체제가 정책을 세우고, 하드웨어가 최종 집행합니다.

**대표 등장 위치:**

| 구성 요소 | 정책 저장 위치 |
|---|---|
| MMU | PTE Flag |
| CPU | Control Register |
| IOMMU | DMA Mapping Table |
| TPM | Secure Key State |
| Secure Boot Engine | Signature Verification State |

> 하드웨어 수립 보안 정책은 "시스템 전체 보호 규칙의 물리적 원장"입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

이 개념은 **CPU**와 **Memory**에 가장 직접적으로 연결됩니다.

| 자원 | 영향도 |
|---|---|
| CPU | 매우 큼 |
| Memory | 매우 큼 |
| Network | 중간 |
| Disk | 중간 |

**CPU 영향**

CPU는 권한 검사, 특권 명령 차단, Exception 발생, Pipeline Flush 등을 수행합니다. 즉 CPU가 정책 집행 엔진 역할을 합니다.

**Memory 영향**

- Page Permission
- NX Protection
- Address Space Isolation
- DMA Memory Protection

**Network 영향**

NIC DMA 제한, IOMMU, Device Isolation 등 간접 연결됩니다.

**Disk 영향**

Secure Boot, TPM, Disk Encryption Key 등과 연결됩니다.

---

## 4. 왜 중요한가

소프트웨어 선언만으로는 절대적인 보안을 보장할 수 없기 때문입니다.

**예시:**

> "유저 프로그램은 커널 메모리 접근 금지"라는 규칙이 있어도, CPU가 실제로 강제하지 않으면 우회될 수 있습니다.

그래서:

```
정책
→ Hardware-readable bit 형태로 변환
→ CPU/MMU가 직접 검사
```

구조가 필요합니다.

> 현대 시스템 보안은 "하드웨어가 강제 가능한 정책 상태" 위에서 성립합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

| 문제 | 관련 정책 |
|---|---|
| Segmentation Fault | PTE Permission 위반 |
| Kernel Panic | Privilege Violation |
| NX Fault | Execute 금지 위반 |
| DMA Attack 차단 | IOMMU Policy |
| VM Escape 방어 | Hypervisor Protection |
| Secure Boot Failure | Signature Policy 위반 |
| Privileged Instruction Fault | CPL 정책 위반 |

**특히 중요한 점:**

> 많은 "Crash"는 시스템이 보호 정책 위반을 탐지하고 강제로 차단한 결과다.

---

## 6. 핵심 메커니즘

### 6-1. 정책은 최종적으로 비트 상태로 존재한다

| 정책 의미 | 실제 하드웨어 상태 |
|---|---|
| Read Only | R/W=0 |
| Execute 금지 | NX=1 |
| User 접근 금지 | U/S=0 |
| 특권 레벨 | CPL=0~3 |

> 하드웨어는 "정책 문장"을 이해하지 못하고, 오직 비트 상태만 이해합니다.

### 6-2. OS가 정책을 "수립"하고 하드웨어가 "집행"한다

```
OS Kernel
→ Page Table 설정
→ Control Register 설정
→ Hardware Policy 상태 형성
→ CPU/MMU Enforcement
```

- **OS** = 정책 관리자
- **Hardware** = 절대 집행자

### 6-3. MMU가 메모리 정책 핵심 집행자다

```
Virtual Address Access
→ MMU
→ PTE Flag 검사
→ 허용 or Fault
```

MMU가 읽기/쓰기/실행 가능 여부를 실시간 검사합니다.

### 6-4. CPU Privilege Level이 특권 명령을 통제한다

```
Ring 0 / EL1+  →  Kernel
Ring 3 / EL0   →  User Application
```

User Mode에서는 Page Table 수정, Interrupt 제어, CR Register 수정 같은 명령 자체가 거부됩니다.

### 6-5. 정책 위반 시 하드웨어 Trap이 발생한다

```
Violation Detect
→ Hardware Exception
→ Pipeline Flush
→ Kernel Trap Handler
```

CPU가 실행 흐름을 강제로 중단합니다.

---

## 7. Linux / Runtime / Kubernetes에서 관측 방법

**Linux Memory Permission 확인**

```bash
cat /proc/<pid>/maps
# 관측: r-xp, rw-p, execute permission
```

**CPU Security Feature 확인**

```bash
lscpu
cat /proc/cpuinfo
# 확인: NX, SMEP, SMAP, VT-x, AMD-V
```

**Kernel Security 상태 확인**

```bash
sysctl -a
# 관측: ASLR, mmap protection, kernel hardening
```

**Fault / Trap 확인**

```bash
dmesg
journalctl
# 관측: Protection Fault, Segfault, NX Violation, Illegal Instruction
```

**Kubernetes**

K8s도 결국 Hardware-enforced Policy 위에서 동작합니다.

| 기능 | 하드웨어 정책 |
|---|---|
| Container Isolation | MMU + Namespace |
| VM Isolation | VT-x / AMD-V |
| Secure Node Boot | TPM/Secure Boot |
| Memory Protection | NX/U/S/RW Bit |

**Observability**

현대 시스템에서는 eBPF, perf, auditd, tracing 등으로 정책 위반 이벤트를 추적합니다.

| 메트릭 | 의미 |
|---|---|
| Page Fault | 접근 위반 |
| General Protection Fault | 권한 위반 |
| NX Fault | 실행 금지 위반 |
| Trap Count | Hardware Exception |
| Kernel Panic | 치명적 정책 위반 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*