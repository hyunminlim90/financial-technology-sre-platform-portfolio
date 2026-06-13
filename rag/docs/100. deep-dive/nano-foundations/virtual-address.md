# Virtual Address (가상 주소, VA)
## 1. 가상 주소(Virtual Address, VA)란 무엇인가

> 정독: 0회

가상 주소(Virtual Address, VA)는:

> 실행 중인 프로세스가 바라보는 **논리적인 메모리 주소**

즉 프로그램은 실제 DRAM의 물리 위치를 직접 다루지 않고:

"나만의 독립된 메모리 공간"

이 존재한다고 착각하며 동작합니다.

실제로는:

```
Virtual Address (VA)
→ MMU 변환
→ Physical Address (PA)
→ 실제 DRAM 접근
```

구조입니다.

핵심은:

> 프로그램은 "가상 주소"만 사용하고,  
> 실제 물리 메모리 위치는 **운영체제와 MMU**가 숨깁니다.

---

## 2. 시스템 어디에서 등장하는가

가상 주소는 거의 모든 현대 운영체제와 프로세스 실행 구조의 핵심입니다.

**전체 흐름:**

```
Application
→ Virtual Address 사용
→ CPU Memory Access
→ MMU
→ Page Table 참조
→ Physical Address 변환
→ DRAM 접근
```

즉 애플리케이션, 런타임, 프로세스, 스레드 모두 가상 주소 위에서 동작합니다.

**대표 등장 위치:**

| 영역 | 가상 주소 사용 |
|------|--------------|
| Process Memory | 전체 |
| Heap | 사용 |
| Stack | 사용 |
| Shared Library | 사용 |
| mmap | 사용 |
| Container Process | 사용 |
| Kubernetes Pod 내부 앱 | 사용 |

즉 현대 컴퓨터 시스템은 사실상 **"가상 주소 기반 시스템"** 입니다.

---

## 3. 어떤 자원(CPU / Memory / Network / Disk)에 가장 영향이 큰가

가상 주소는 특히 **Memory**와 **CPU**에 매우 큰 영향을 줍니다.

| 자원 | 영향도 |
|------|--------|
| Memory | 매우 큼 |
| CPU | 매우 큼 |
| Disk | 중간 |
| Network | 낮음 |

### Memory 영향

가상 주소는 Virtual Memory, Paging, Address Space, Heap/Stack, Memory Isolation 전체를 구성합니다. 즉 **메모리 관리의 핵심**입니다.

### CPU 영향

CPU는 메모리 접근 시마다 `VA → MMU → PA 변환`을 수행합니다. 그래서 **TLB**, **Page Walk**, **Cache** 등이 매우 중요해집니다.

### Disk 영향

메모리가 부족하면 **Page Out → SSD/HDD Swap 저장**이 발생합니다. 즉 가상 메모리는 디스크와도 연결됩니다.

---

## 4. 왜 중요한가

현대 운영체제 안정성과 멀티태스킹의 핵심이기 때문입니다.

가상 주소가 없다면:

- 프로세스끼리 메모리 충돌
- 보안 붕괴
- 메모리 파편화 심화
- 프로그램 실행 위치 제한

등이 발생합니다.

**가상 주소 도입 효과:**

| 효과 | 설명 |
|------|------|
| 메모리 격리 | 프로세스 보호 |
| 보안 | 다른 프로세스 접근 차단 |
| 연속 주소 공간 제공 | 프로그래밍 단순화 |
| Swap 가능 | RAM 초과 사용 가능 |
| 프로세스 독립성 | 실행 위치 무관 |

> **현대 운영체제는 가상 주소 없이는 거의 성립 불가능하다.**

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 서버 장애 상당수가 가상 메모리와 연결됩니다.

**대표 사례:**

| 장애 | 원인 |
|------|------|
| OOM | Virtual Memory 부족 |
| Swap Thrashing | 과도한 Page In/Out |
| High Page Fault | 메모리 압박 |
| Segmentation Fault | 잘못된 VA 접근 |
| Kernel OOM Kill | Address Space 압박 |
| Latency 증가 | Major Page Fault |
| Container Memory Pressure | VA→PA 관리 문제 |

특히 중요한 개념:

> **메모리 사용량 ≠ 실제 물리 메모리 사용량**

프로세스는 매우 큰 가상 주소 공간을 가질 수 있지만, 실제 DRAM에는 일부만 올라와 있을 수 있습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 6-1. 프로그램은 실제 물리 주소를 모른다

프로그램이 사용하는 주소 (예: `0x7fff12345678`) 는 대부분 **가상 주소**입니다.

즉 애플리케이션은 실제 DRAM 칩 위치나 실제 메모리 배치를 전혀 모릅니다.

### 6-2. MMU가 VA → PA 변환을 수행한다

```
CPU
→ Virtual Address 생성
→ MMU
→ Page Table 조회
→ Physical Address 변환
→ DRAM 접근
```

즉 **MMU(Memory Management Unit)** 가 핵심 하드웨어입니다.

### 6-3. Page Table이 주소 변환 지도다

운영체제는 Virtual Page ↔ Physical Frame 매핑 정보를 **Page Table**에 저장합니다.

> Page Table은 "가상 주소 지도"입니다.

### 6-4. 모든 프로세스는 독립 Address Space를 가진다

```
Process A → 독립 VA 공간
Process B → 독립 VA 공간
```

즉 같은 주소값 `0x1000`이라도 서로 다른 물리 메모리를 가리킬 수 있습니다. 이것이 **프로세스 격리의 핵심**입니다.

### 6-5. RAM보다 큰 메모리 사용이 가능한 이유

가상 메모리는 당장 필요 없는 페이지를 SSD/HDD Swap 영역으로 내리고, 필요 시 다시 가져옵니다.

```
DRAM 부족
→ Page Out
→ Disk Swap 저장
→ 필요 시 Page In
```

> 가상 주소 체계는 **"메모리 확장 환상"** 을 제공합니다.

---

## 7. Linux / Runtime / Kubernetes에서 어디서 관측되는가

### Linux Memory 관측

**대표 확인 명령:**

```bash
free -h
vmstat
top
htop
```

관측 가능: Swap 사용량, Memory Pressure, Page Fault

**Process Address Space 확인:**

```bash
cat /proc/<pid>/maps
pmap <pid>
```

확인 가능: Heap, Stack, mmap, Shared Library, Virtual Address Layout

**Page Fault 관측:**

```bash
vmstat
sar -B
perf stat
```

확인 가능: Minor Fault, Major Fault, Swap Activity

### Kubernetes

컨테이너도 가상 메모리 기반입니다.

```bash
kubectl top pod
kubectl describe pod
```

관측 가능: OOMKill, Memory Pressure, Node Memory 부족

### Observability

현대 시스템에서는 Prometheus, eBPF, perf, tracing 등으로 메모리 동작을 추적합니다.

**대표 메트릭:**

| 메트릭 | 의미 |
|--------|------|
| RSS | 실제 물리 메모리 |
| VSZ | 가상 주소 공간 크기 |
| Page Fault | 메모리 접근 실패 |
| Swap In/Out | 디스크 메모리 교체 |
| OOM Count | 메모리 부족 |
| Cache Miss | 메모리 계층 병목 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*