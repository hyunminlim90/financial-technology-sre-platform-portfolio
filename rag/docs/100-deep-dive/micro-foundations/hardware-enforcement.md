# Hardware Enforcement (하드웨어 강제)
## 1. 하드웨어 강제(Hardware Enforcement)란 무엇인가

> 정독: 0회

하드웨어 강제(Hardware Enforcement)는:

> 운영체제나 보안 정책이 정의한 규칙을 **CPU·MMU·칩셋 등의 하드웨어 회로 자체**가 직접 검사하고 물리적으로 강제하는 메커니즘입니다.

**"소프트웨어 규칙을 하드웨어가 절대적으로 집행하는 구조"**

핵심은 규칙 위반 여부를 소프트웨어가 판단하는 것이 아니라, **CPU 회로 자체가 즉시 차단**한다는 점입니다.

**대표 예시:**

| 기술 | 하드웨어 강제 내용 |
|------|------------------|
| User/Kernel Mode | 권한 분리 |
| NX Bit | 데이터 실행 금지 |
| Read-Only Page | 쓰기 차단 |
| SMEP/SMAP | 커널 보호 |
| Secure Boot | 부팅 무결성 |
| TPM | 키 보호 |
| IOMMU | DMA 제한 |

> 현대 보안·격리·안정성의 **최하단 기반**입니다.

<details>
<summary>Deep Dive</summary></br>

Operating System(운영체제) [[M]](../../100-deep-dive/micro-foundations/operating-system.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

하드웨어 강제는 CPU가 명령어를 실행하거나 메모리에 접근하는 **거의 모든 순간** 등장합니다.

**전체 흐름:**

```
Application
→ CPU Instruction Decode
→ MMU Permission Check
→ Hardware Rule Validation
→ 허용 or Fault 발생
```

메모리 접근, 커널 호출, 코드 실행, 장치 접근, 가상화 등 대부분의 핵심 경로에서 동작합니다.

**대표 등장 위치:**

| 구성 요소 | 역할 |
|-----------|------|
| CPU Privilege Ring | 권한 통제 |
| MMU | 메모리 접근 강제 |
| NX/XD Bit | 실행 차단 |
| IOMMU | 장치 DMA 제한 |
| TPM | 키 보호 |
| Secure Boot | 신뢰된 부팅 |

> 하드웨어 강제는 **"컴퓨터 전체의 최종 안전장치"** 입니다.

---

## 3. 어떤 자원(CPU / Memory / Network / Disk)에 가장 영향이 큰가

하드웨어 강제는 **CPU**와 **Memory**에 가장 직접적으로 연결됩니다.

| 자원 | 영향도 |
|------|--------|
| CPU | 매우 큼 |
| Memory | 매우 큼 |
| Network | 중간 |
| Disk | 중간 |

### CPU 영향

CPU는 권한 검사, 명령어 검증, 예외 처리, Ring 전환을 수행합니다. User Mode에서 Privileged Instruction 실행 시도 시 CPU가 즉시 차단합니다.

### Memory 영향

Page Permission, NX Bit, Kernel/User Separation, DMA Protection 등 **메모리 보호 핵심**과 연결됩니다.

### Network 영향

NIC DMA 제한, IOMMU, Secure Packet Processing 등과 간접적으로 연결됩니다.

### Disk 영향

Secure Boot, Disk Encryption Key Protection, TPM 기반 무결성 검증과 연결됩니다.

---

## 4. 왜 중요한가

**소프트웨어만으로는 절대적인 보안을 보장할 수 없기 때문**입니다.

소프트웨어는 버그, 취약점, 우회 공격이 가능할 수 있습니다. 하지만 하드웨어 강제는 **실리콘 회로 수준에서 물리적으로 차단**합니다.

> **CPU 자체가 "불가능"하게 만든다는 것이 핵심**입니다.

| 기능 | 효과 |
|------|------|
| Kernel Protection | OS 보호 |
| Memory Isolation | 프로세스 격리 |
| NX Enforcement | 코드 실행 차단 |
| DMA Protection | 장치 공격 차단 |
| Secure Boot | 루트킷 방지 |
| TPM | 키 탈취 방지 |

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 보안 사고·시스템 장애와 매우 밀접합니다.

**대표 사례:**

| 문제 | 하드웨어 강제 관련 |
|------|------------------|
| Segmentation Fault | MMU 차단 |
| General Protection Fault | 권한 위반 |
| Kernel Panic | 보호 실패 |
| Exploit 차단 | NX Bit 동작 |
| Secure Boot Failure | 무결성 검증 실패 |
| VM Escape 방어 | Hypervisor Protection |
| DMA Attack 차단 | IOMMU 동작 |

> 많은 "프로세스 크래시"는 사실 **시스템을 보호하기 위한 하드웨어 강제 결과**다.

Segfault, Protection Fault, Illegal Instruction 등은 **시스템 보호 동작**입니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 6-1. MMU가 메모리 접근을 하드웨어적으로 차단한다

```
CPU Memory Access
→ MMU Permission Check
→ 허용 or Fault
```

읽기 전용 페이지 Write, Kernel 영역 접근, Execute 금지 영역 실행 등을 MMU가 **즉시 차단**합니다.

### 6-2. CPU Privilege Ring이 권한을 강제한다

```
Ring 0 → Kernel
Ring 3 → User Application
```

User Mode에서는 `CR3 수정`, `Interrupt 제어`, `Page Table 수정` 같은 명령 자체가 **CPU Decoder 단계에서 거부**됩니다.

### 6-3. NX Bit가 "데이터 실행"을 차단한다

```
Heap / Stack → Execute 금지
```

버퍼 오버플로우, 쉘코드 실행 같은 공격을 **하드웨어가 직접 차단**합니다.

### 6-4. Fault 발생 시 CPU가 즉시 파이프라인을 Flush한다

```
Violation Detect
→ CPU Exception
→ Pipeline Flush
→ Kernel Exception Handler Jump
```

위반 시 현재 명령 중단, 실행 흐름 폐기, 커널 예외 처리가 **강제**됩니다.

### 6-5. 하드웨어 강제는 소프트웨어 우회가 매우 어렵다

| 방식 | 우회 가능성 |
|------|------------|
| 소프트웨어 검사 | 상대적으로 가능 |
| 하드웨어 강제 | 매우 어려움 |

> 하드웨어 강제는 **"신뢰의 최하단 계층"** 입니다.

---

## 7. Linux / Runtime / Kubernetes에서 어디서 관측되는가

### Linux Fault 확인

```bash
dmesg
journalctl
```

관측 가능: Segmentation Fault, General Protection Fault, Illegal Instruction

### CPU Security Feature 확인

```bash
lscpu
cat /proc/cpuinfo
```

확인 가능: NX, SMEP, SMAP, VT-x, AMD-V

### Memory Protection 상태

```bash
cat /proc/<pid>/maps
```

확인 가능: `r-xp`, `rw-p`, execute 권한

### Kernel Security

```bash
sysctl -a
```

확인 가능: ASLR, mmap restrictions, kernel protection settings

### Kubernetes

K8s도 하드웨어 강제 기반 위에서 동작합니다.

| 기능 | 연결 기술 |
|------|----------|
| Container Isolation | MMU + Namespace |
| VM Isolation | CPU Virtualization |
| Secure Node Boot | TPM/Secure Boot |
| Memory Protection | NX/MMU |

### Observability

현대 시스템에서는 eBPF, perf, auditd, tracing 등으로 보호 이벤트를 추적합니다.

**대표 메트릭:**

| 메트릭 | 의미 |
|--------|------|
| Page Fault | 메모리 위반 |
| Protection Fault | 권한 위반 |
| Illegal Instruction | 금지 명령 |
| NX Violation | 실행 차단 |
| Kernel Trap | 커널 예외 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*