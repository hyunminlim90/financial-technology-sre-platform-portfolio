# Security Primitive (보안 프리미티브)
## 1. 보안 프리미티브(Security Primitive)란 무엇인가

> 정독: 0회

보안 프리미티브(Security Primitive)는:

> 보안 시스템을 구성하는 가장 기본적이고 더 이상 분해하기 어려운 원자적(Atomic) 보안 빌딩 블록

"모든 보안 시스템의 최하단 기초 부품"

현대 보안 시스템은 메모리 보호, 암호화, 인증, 격리, 권한 제어 같은 거대한 보안 구조를 직접 구현하지 않고, 이러한 프리미티브들을 조합해서 만듭니다.

**대표 예시:**

| 분야 | 보안 프리미티브 |
|---|---|
| CPU/MMU | NX Bit |
| 권한 제어 | Privilege Ring |
| 암호학 | SHA-256 |
| 암호화 | AES/RSA |
| 하드웨어 신뢰 | TPM/PUF |
| 메모리 보호 | Page Permission |

> 보안 프리미티브는 "보안 세계의 원자 단위"입니다.

---

## 2. 시스템 어디에서 등장하는가

보안 프리미티브는 시스템 거의 모든 계층에서 등장합니다.

**전체 흐름:**

```
Hardware Primitive
↓
OS Security Policy
↓
Hypervisor / Runtime
↓
Application Security
↓
User-facing Security
```

CPU, MMU, TPM, OS Kernel, Hypervisor, Cryptography, Secure Boot 전체가 프리미티브 기반 위에 구축됩니다.

**대표 등장 위치:**

| 계층 | 프리미티브 |
|---|---|
| CPU | Privilege Ring |
| MMU | NX / RW / US Bit |
| TPM | Hardware Root Key |
| Hypervisor | VM Isolation |
| Cryptography | Hash / Signature |
| Secure Boot | Signature Verification |

> 상위 보안 정책은 결국 하위 프리미티브 조합입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

보안 프리미티브는 **CPU**와 **Memory**에 가장 강하게 연결됩니다.

| 자원 | 영향도 |
|---|---|
| CPU | 매우 큼 |
| Memory | 매우 큼 |
| Network | 중간 |
| Disk | 중간 |

**CPU 영향**

CPU는 권한 검사, 명령 차단, 예외 처리, 암호 연산 가속 등을 수행합니다. 대표 연결: Privilege Ring, NX Bit, VM Exit

**Memory 영향**

- Memory Isolation
- Page Permission
- Execute Protection
- DMA Protection

**Network 영향**

TLS, Packet Authentication, Secure NIC DMA 등과 연결됩니다.

**Disk 영향**

Disk Encryption, Secure Boot, TPM Key Storage 등과 연결됩니다.

---

## 4. 왜 중요한가

보안 프리미티브가 무너지면 그 위 모든 보안 구조가 무너지기 때문입니다.

```
상위 보안 정책
↓
하드웨어 프리미티브 의존
↓
프리미티브 취약점 발생
↓
전체 보안 붕괴
```

**대표 사례:**

| 취약점 | 영향 |
|---|---|
| Meltdown | Privilege Isolation 붕괴 |
| Spectre | Speculative Execution 악용 |
| Rowhammer | Memory Isolation 우회 |
| Broken Hash | 인증 체계 붕괴 |

> 프리미티브는 "보안 신뢰의 최하단 기반"입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

| 문제 | 관련 프리미티브 |
|---|---|
| Kernel Escape | Privilege Isolation |
| Remote Code Execution | NX 우회 |
| VM Escape | Hypervisor Isolation |
| Data Corruption | Memory Protection 실패 |
| Secure Boot Bypass | Signature Primitive 문제 |
| TLS 공격 | Cryptographic Primitive 취약점 |
| DMA Attack | IOMMU Primitive 문제 |

**특히 중요한 점:**

> 보안 시스템은 가장 약한 프리미티브 수준까지밖에 안전하지 않다.

---

## 6. 핵심 메커니즘

### 6-1. 보안 프리미티브는 "원자적 보안 기능"이다

| 프리미티브 | 역할 |
|---|---|
| NX Bit | 실행 차단 |
| Ring Level | 권한 분리 |
| Hash Function | 무결성 |
| Signature | 신원 검증 |
| Encryption | 기밀성 |

하나하나는 단순하지만, 조합되면 거대한 보안 체계가 됩니다.

### 6-2. 하드웨어 프리미티브는 CPU가 직접 강제한다

```
CPU/MMU
→ Permission Check
→ Fault or Allow
```

NX Bit, Privilege Ring, Page Permission 등은 CPU가 직접 집행합니다.

### 6-3. 암호학 프리미티브는 신뢰의 기반이다

대표 예시: SHA-256, AES, RSA, ECDSA

TLS, VPN, Secure Boot, 인증 시스템 전체가 이 기반 위에 존재합니다.

### 6-4. 프리미티브는 조합되어 상위 정책이 된다

```
NX Bit
+ Privilege Ring
+ ASLR
+ Signature Verification
= 현대 OS 보안 구조
```

보안은 단일 기술이 아니라, 프리미티브 조합입니다.

### 6-5. 프리미티브 결함은 시스템 전체 위험이 된다

```
Speculative Execution Primitive 문제
→ Spectre/Meltdown
→ 전 세계 CPU 영향
```

하위 프리미티브 결함은 매우 치명적입니다.

---

## 7. Linux / Runtime / Kubernetes에서 관측 방법

**CPU Security Feature 확인**

```bash
lscpu
cat /proc/cpuinfo
# 확인: NX, VT-x, SMEP, SMAP
```

**Memory Protection 상태 확인**

```bash
cat /proc/<pid>/maps
# 확인: Execute Permission, Read/Write Protection
```

**Secure Boot / TPM 상태 확인**

```bash
dmesg
mokutil --sb-state
# 확인: Secure Boot 활성화, TPM 상태
```

**Cryptographic Primitive 확인**

```bash
openssl version
# 확인: TLS Algorithm, Crypto Backend
```

**Kubernetes**

K8s도 보안 프리미티브 위에서 동작합니다.

| 기능 | 기반 프리미티브 |
|---|---|
| Container Isolation | Namespace + MMU |
| TLS Communication | Crypto Primitive |
| Secure Node Boot | TPM + Signature |
| Secret Encryption | AES/RSA |

**Observability**

현대 시스템에서는 auditd, eBPF, perf, security telemetry 등으로 보안 이벤트를 추적합니다.

| 메트릭 | 의미 |
|---|---|
| Protection Fault | 메모리 위반 |
| NX Fault | 실행 차단 |
| TPM Event | Secure Boot 상태 |
| TLS Handshake Error | 암호 문제 |
| VM Exit Security Trap | Hypervisor 보호 이벤트 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*