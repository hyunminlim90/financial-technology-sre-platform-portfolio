# 메모리 세그먼트 (Memory Segment)

> 정독: 0회

## 1. 이 기술이 무엇인가

메모리 세그먼트는:

> 프로세스의 가상 주소 공간을 역할과 접근 권한 기준으로 분리한 메모리 영역

| 세그먼트 | 역할 |
|----------|------|
| Code/Text | 실행 코드 |
| Data | 전역/정적 데이터 |
| Heap | 동적 할당 메모리 |
| Stack | 함수 실행 문맥 |

**핵심 목적:** 메모리 보호 / 역할 분리 / 접근 권한 통제 / 안정적 실행

> 프로세스 메모리 공간의 구조적 분할 체계

---

## 2. 시스템 어디에서 등장하는가

운영체제와 CPU 메모리 관리 전반에서 등장합니다.

| 계층 | 역할 |
|------|------|
| 운영체제 | process layout |
| CPU/MMU | address protection |
| loader | segment mapping |
| linker | segment organization |
| runtime | heap/stack 관리 |
| executable format | ELF/PE/Mach-O section |

프로세스 실행 시 모든 메모리는 세그먼트 기반으로 조직됩니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**가장 영향이 큰 자원: Memory + MMU + CPU cache**

| 자원 | 영향 |
|------|------|
| RAM | segment allocation |
| MMU | protection check |
| TLB | address translation |
| CPU cache | locality |
| Virtual Memory | mapping 관리 |

> 메모리 보호와 주소 변환의 핵심 기반

---

## 4. 왜 중요한가

코드·데이터·실행 문맥을 안전하게 분리하기 위해 필요합니다.

없다면: 코드 덮어쓰기 가능 / stack corruption 증가 / 임의 실행 가능 / 프로세스 격리 붕괴 / 시스템 안정성 붕괴가 발생할 수 있습니다.

현대 OS는 **세그먼트 + 페이지 기반 보호 모델**을 사용합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

| 문제 | 원인 |
|------|------|
| Segmentation Fault | 잘못된 세그먼트 접근 |
| Stack Overflow | stack segment 초과 |
| Heap Corruption | heap overwrite |
| NX violation | executable 권한 위반 |
| Buffer Overflow | segment boundary 침범 |
| Invalid Access | unmapped memory 접근 |

```
stack 영역 초과 → segmentation fault
읽기 전용 code segment 수정 시도 → protection fault
```

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 프로세스 가상 주소 공간

운영체제는 프로세스마다 독립 가상 주소 공간을 제공합니다. 이 공간 내부가 code / data / heap / stack 등으로 분리됩니다.

### Code/Text Segment

| 속성 | 내용 |
|------|------|
| 권한 | Read + Execute |
| 저장 내용 | machine instruction |
| 변경 여부 | 보통 불가 |

현대 시스템은 **W^X 정책**을 사용합니다. 즉, write + execute 동시 부여를 금지합니다.

### Data Segment

| 영역 | 특징 |
|------|------|
| initialized data | 초기값 존재 |
| BSS | zero-initialized |

프로세스 생존 동안 유지됩니다.

### Heap Segment

런타임 중 크기 변화가 가능한 동적 메모리 영역입니다.

예: dynamic allocation / object allocation / buffer allocation

Heap allocator가 관리합니다.

### Stack Segment

| 요소 | 역할 |
|------|------|
| stack frame | 함수 문맥 |
| local variable | 지역 변수 |
| return address | 복귀 주소 |

LIFO 기반으로 동적 증가/감소합니다.

### MMU 검증

CPU 접근 시 MMU가 주소 유효성 / 권한 / mapping 존재 여부를 검사합니다. 실패 시 fault exception이 발생합니다.

### Base Address + Offset

모든 메모리 접근은 `segment base + offset` 형태로 실제 주소를 계산합니다.

### Virtual Address Translation

프로세스는 가상 주소만 사용하며, MMU가 실제 물리 주소 변환을 수행합니다.

### Segment Protection

| 보호 | 의미 |
|------|------|
| Read-only | 쓰기 금지 |
| NX | 실행 금지 |
| User/Kernel | privilege separation |

OS 안정성의 핵심입니다.

### Memory Layout

| 주소 영역 | 세그먼트 |
|-----------|----------|
| high addr | stack |
| middle | mmap/shared |
| lower | heap |
| lower | data |
| low addr | code |

### Fragmentation

특히 heap에서 메모리 단편화(fragmentation)가 발생할 수 있어 성능 저하의 원인이 됩니다.

### Shared Segment

일부 세그먼트는 공유 가능합니다.

예: shared library / shared memory / mapped file → 메모리 효율 향상

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

```bash
# 프로세스 세그먼트 확인
cat /proc/<pid>/maps

# segment usage 확인
pmap <pid>

# section/segment 구조 분석
readelf -S
objdump -h
```

| 영역 표시 | 의미 |
|-----------|------|
| r-xp | code |
| rw-p | data |
| heap | heap segment |
| stack | stack segment |

### Runtime

| 영역 | 관련 |
|------|------|
| allocator | heap 관리 |
| scheduler | stack 사용 |
| loader | segment mapping |

### Kubernetes

| 현상 | 관련 |
|------|------|
| OOMKill | heap 증가 |
| stack overflow | stack segment |
| crash | invalid memory access |
| memory leak | heap exhaustion |

container memory limit와 밀접하게 연관됩니다.

### Observability

| 도구 | 목적 |
|------|------|
| perf | memory fault |
| vmstat | VM 상태 |
| smaps | detailed mapping |
| eBPF | page fault tracing |

실무에서는 heap/stack/code 구분 분석이 매우 중요합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*