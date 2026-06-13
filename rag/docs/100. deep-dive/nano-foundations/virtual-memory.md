# 가상 메모리 (Virtual Memory)

> 정독: 0회

가상 메모리(Virtual Memory)는:

> **운영체제(OS)와 MMU가 협력하여 프로세스에게 실제 DRAM보다 훨씬 크고, 연속적이며, 독립된 메모리 공간이 존재하는 것처럼 제공하는 메모리 추상화 기술**

**핵심:**
"실제 물리 메모리"와 "프로세스가 바라보는 메모리"를 분리한다

---

## 1. 이 기술이 무엇인가

프로세스는 실제 DRAM 주소를 직접 사용하지 않습니다. 대신 **가상 주소(Virtual Address)** 를 사용합니다.

CPU 내부의 MMU가 `가상 주소 → 실제 물리 주소`로 실시간 변환합니다.

즉, 가상 메모리는 **"메모리 주소를 가상화하는 시스템"** 입니다.

### 핵심 구조

```
Process
↓
Virtual Address
↓
MMU
↓
Page Table
↓
Physical Address
↓
DRAM
```

---

## 2. 시스템 어디에서 등장하는가

가상 메모리는 현대 운영체제 전체에서 등장합니다.

대표 영역:

- Process Address Space
- Page / Page Table / MMU / TLB
- Heap / Stack
- mmap / Shared Memory / Swap
- Fork / ELF Loader
- Container Runtime / Hypervisor

즉, **현대 프로세스 실행 구조의 핵심 기반**입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향도 |
|------|--------|
| Memory | 매우 큼 |
| CPU | 매우 큼 |
| Disk | 큼 |
| Network | 간접 영향 |

특히 **Page Fault, TLB Miss, Swap, Memory Pressure**는 시스템 성능을 크게 흔듭니다.

---

## 4. 왜 중요한가

가상 메모리가 없다면 프로세스 격리 불가, 메모리 보호 불가, 큰 프로그램 실행 불가, 메모리 파편화 심화, 멀티태스킹 어려움이 발생합니다.

즉, **현대 운영체제 자체가 사실상 불가능**합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. Page Fault 폭증

메모리에 없는 페이지 접근 시 **Page Fault**가 발생합니다. 너무 많아지면 CPU가 계속 대기 상태에 들어갑니다.

### 5-2. Swap Thrashing

DRAM 부족 시 Page In / Page Out이 반복됩니다.

**결과:** SSD I/O 폭증 + 시스템 응답 정지 가능 — 대표적인 **Thrashing** 상태입니다.

### 5-3. OOM (Out Of Memory)

가상 메모리 관리 실패 시 **OOM Killer**가 발동될 수 있습니다. Linux에서 흔하게 발생합니다.

### 5-4. TLB Miss 증가

주소 변환 캐시(TLB)가 부족하면 주소 변환 비용이 증가하여 **CPU Stall**이 증가합니다.

### 5-5. Segmentation Fault

허용되지 않은 가상 주소 접근 시 **Segmentation Fault**가 발생합니다.

### 5-6. Memory Leak

Heap이 계속 증가하면 **Virtual Memory Pressure**가 증가합니다.

---

## 6. 핵심 메커니즘

### 6-1. 프로세스는 실제 DRAM 주소를 모른다

프로세스는 가상 주소만 사용합니다. `0x7fff1234` 같은 주소는 실제 DRAM 주소가 아닙니다.

### 6-2. MMU가 주소를 변환한다

```
Virtual Address → MMU → Page Table → Physical Address
```

### 6-3. 메모리는 Page 단위로 관리된다

대표 기본 단위: **4KB Page = 4 × 1,024 = 4,096 Byte**

### 6-4. 실제 DRAM은 조각나 있어도 된다

프로세스는 연속 메모리처럼 보지만, 실제 DRAM은 조각난 Physical Frame일 수 있습니다. MMU가 이를 숨겨줍니다.

### 6-5. 필요한 페이지만 DRAM에 올린다

프로그램 전체를 올리지 않습니다. 필요한 Page만 **Page In**하고, 나머지는 SSD Swap에 있을 수 있습니다.

### 6-6. TLB가 매우 중요하다

페이지 테이블 조회는 느리므로 MMU 내부 SRAM 캐시인 **TLB**를 사용합니다.

TLB Miss가 많으면 주소 변환 자체가 병목됩니다.

### 6-7. Page Fault는 정상이다

모든 Page Fault가 장애는 아닙니다. **Minor Page Fault**는 정상 동작입니다.

하지만 **Major Page Fault**가 증가하면 실제 Disk I/O가 발생합니다.

### 6-8. 가상 메모리는 보안 시스템이기도 하다

각 프로세스는 **독립된 Address Space**를 가집니다.

즉, **프로세스 간 메모리 직접 침범이 불가능**합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 가상 메모리 상태
free -h
vmstat
sar -B

# 프로세스 주소 공간
cat /proc/<pid>/maps

# 페이지 상태
cat /proc/meminfo

# Page Fault
ps -eo pid,maj_flt,min_flt,cmd

# Swap 상태
swapon -s

# OOM 로그
dmesg | grep -i oom
```

### Runtime

주요 관측 대상:

- Heap Growth
- RSS
- VSZ
- Allocation Rate
- Page Fault
- Swap Usage

### Kubernetes

```bash
# OOMKilled 확인
kubectl describe pod

# Container Memory Usage
kubectl top pod
```

```yaml
# Memory Limit
resources:
  limits:
    memory: "2Gi"
```

> **cgroup 메모리 제한** — 컨테이너도 결국 Virtual Memory 기반으로 동작합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*