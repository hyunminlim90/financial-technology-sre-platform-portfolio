# Memory Protection (메모리 보호)
## 1. 메모리 보호(Memory Protection)란 무엇인가

> 정독: 0회

메모리 보호(Memory Protection)는:

> 운영체제(OS)와 CPU 하드웨어(MMU)가 협력하여 프로세스가 허용되지 않은 메모리 영역에 접근하거나 수정하는 것을 차단하는 **보호 메커니즘**

**"프로세스마다 메모리 영토를 강제로 분리하고 보호하는 기술"**

**핵심 구조:**

```
Process A  → 자기 메모리만 접근 가능
Process B  → 자기 메모리만 접근 가능
Kernel     → 보호된 Supervisor 영역
```

> 현대 운영체제의 안정성과 보안은 **메모리 보호 위에서 성립**합니다.

---

## 2. 시스템 어디에서 등장하는가

메모리 보호는 CPU의 **메모리 접근 모든 순간**에 등장합니다.

**전체 흐름:**

```
Application
→ Virtual Address 접근
→ MMU
→ Page Table Permission 검사
→ 허용 시 접근
→ 위반 시 Fault 발생
```

프로세스 실행, 파일 읽기, 네트워크 처리, 스택/Heap 사용 등 **모든 메모리 접근마다** 동작합니다.

**대표 등장 위치:**

| 구성 요소 | 역할 |
|-----------|------|
| MMU | 권한 검사 |
| Page Table | 보호 플래그 저장 |
| CPU Exception Handler | Fault 처리 |
| Kernel | 프로세스 종료/신호 처리 |
| NX Bit | 실행 방지 |
| User/Supervisor Bit | 커널 보호 |

> 메모리 보호는 **"메모리 접근의 실시간 보안 필터"** 입니다.

---

## 3. 어떤 자원(CPU / Memory / Network / Disk)에 가장 영향이 큰가

메모리 보호는 특히 **CPU**와 **Memory**에 직접 연결됩니다.

| 자원 | 영향도 |
|------|--------|
| Memory | 매우 큼 |
| CPU | 매우 큼 |
| Network | 낮음 |
| Disk | 낮음 |

### Memory 영향

Address Space Isolation, Page Permission, Read/Write Control, Execute Control 전체를 담당합니다. 즉 **메모리 안정성 핵심**입니다.

### CPU 영향

CPU는 메모리 접근 시마다 `VA 접근 → MMU Permission Check → 접근 허용/차단`을 수행합니다. Page Fault, Protection Fault, Context Isolation과 직접 연결됩니다.

---

## 4. 왜 중요한가

현대 컴퓨터 시스템이 **멀티태스킹과 보안을 유지**할 수 있는 핵심 기반이기 때문입니다.

메모리 보호가 없다면: 앱끼리 메모리 침범, 악성코드 커널 탈취, 시스템 전체 충돌, 데이터 오염이 발생합니다.

> **메모리 보호가 없으면 현대 운영체제의 안정성은 거의 불가능하다.**

| 보호 기능 | 목적 |
|-----------|------|
| Process Isolation | 프로세스 분리 |
| Kernel Protection | OS 보호 |
| Read-Only Protection | 코드 변조 방지 |
| NX Protection | 악성 코드 실행 방지 |
| User/Supervisor Separation | 권한 분리 |

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 시스템 장애와 보안 이벤트 상당수가 메모리 보호와 연결됩니다.

**대표 사례:**

| 장애/문제 | 원인 |
|-----------|------|
| Segmentation Fault | 보호 영역 침범 |
| Kernel Panic | 커널 메모리 손상 |
| Buffer Overflow | 메모리 경계 초과 |
| Stack Corruption | 잘못된 쓰기 |
| Access Violation | Permission 위반 |
| Exploit 공격 | NX 우회 시도 |
| Process Kill | Fault 발생 |
| Page Fault Storm | 잘못된 접근 반복 |

> Segmentation Fault는 메모리 보호 시스템이 불법 접근을 **차단했다는 의미**다.  
> 즉 Segfault는 단순 오류가 아니라 **시스템 보호 성공 결과**입니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 6-1. Page Table Entry(PTE)에 권한 비트가 존재한다

메모리 보호 핵심은 **Page Table Entry(PTE)** 입니다.

| 플래그 | 의미 |
|--------|------|
| R/W | 읽기/쓰기 허용 |
| U/S | User/Supervisor 구분 |
| NX/XD | 실행 금지 |
| Present | 메모리 존재 여부 |

> 메모리 접근 시 **MMU가 PTE 권한 비트를 검사**한다.

### 6-2. MMU가 실시간으로 권한 위반을 차단한다

```
CPU Memory Access
→ MMU Permission Check
→ 허용 or Fault
```

메모리 보호는 소프트웨어가 아니라 **하드웨어 레벨에서 즉시 강제**됩니다.

### 6-3. User Mode는 Kernel Memory 접근 불가다

```
User Space ≠ Kernel Space 접근 가능
```

일반 프로그램은 Kernel Code, Kernel Data, Device Control Memory 등을 직접 수정할 수 없습니다.

### 6-4. NX Bit가 데이터 실행을 차단한다

```
Data Memory → Execute 금지
```

Stack, Heap, Buffer 등에 악성 코드를 삽입해도 **실행이 차단**됩니다. 이것이 현대 보안 핵심 중 하나입니다.

### 6-5. Fault 발생 시 OS가 프로세스를 종료한다

```
Illegal Memory Access
→ MMU Detect
→ CPU Exception
→ Kernel Signal
→ Process Kill
```

OS는 메모리 보호 위반 프로세스를 **강제 종료**합니다.

---

## 7. Linux / Runtime / Kubernetes에서 어디서 관측되는가

### Linux Process Memory

**대표 확인 명령:**

```bash
cat /proc/<pid>/maps
pmap <pid>
```

관측 가능: Read/Write 권한, Execute 영역, Shared Library Mapping

**메모리 권한 예시:**

```
r-xp  → Read + Execute
rw-p  → Read + Write
```

즉 메모리 영역별 권한이 존재합니다.

### Fault / Crash 확인

```bash
dmesg
journalctl
```

확인 가능: Segmentation Fault, General Protection Fault, Page Fault

### Kubernetes

컨테이너도 메모리 보호 위에서 동작합니다.

```bash
kubectl logs
kubectl describe pod
```

관측 가능: Segfault, OOMKill, CrashLoopBackOff

### Observability

현대 시스템에서는 eBPF, perf, tracing, crash dump 등으로 메모리 보호 이벤트를 추적합니다.

**대표 메트릭:**

| 메트릭 | 의미 |
|--------|------|
| Page Fault Rate | 접근 오류 |
| Segfault Count | 보호 위반 |
| OOM Kill Count | 메모리 압박 |
| Kernel Exception | 권한 오류 |
| NX Violation | 실행 금지 위반 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*