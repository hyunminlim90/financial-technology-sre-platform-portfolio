# 정적 실행 파일 (Static Executable)

> 정독: 0회

## 1. 이 기술이 무엇인가

정적 실행 파일(Static Executable)은:

> 컴파일, 링크, 바이너리 배치, 메타데이터 구성이 완료된 최종 실행 파일

**핵심 특징:**

| 요소 | 의미 |
|------|------|
| executable code | CPU 실행 명령 |
| static data | 초기 데이터 |
| binary format | ELF/PE/Mach-O |
| symbol metadata | 주소/섹션 정보 |
| entry point | 시작 실행 위치 |

이 파일은 **an inactive binary artifact before runtime activation** 상태입니다.

즉 아직:

- CPU 실행 없음
- 메모리 적재 없음
- 프로세스 없음
- 런타임 없음

<details>
<summary>Deep Dive</summary></br>

Source Code Translation(소스 코드 번역) [[M]](../../100-deep-dive/micro-foundations/source-code-translation.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

정적 실행 파일은 모든 시스템 소프트웨어 체인의 중심에 존재합니다.

| 영역 | 역할 |
|------|------|
| compiler toolchain | final output |
| linker | binary assembly |
| operating system loader | runtime loading |
| container image | packaged executable |
| deployment artifact | distributable binary |
| runtime bootstrap | process entry source |

**대표 포맷:**

| 플랫폼 | 포맷 |
|--------|------|
| Linux | ELF |
| Windows | PE |
| macOS | Mach-O |

> 현대 시스템 대부분은: **runtime execution begins from executable binary loading** 구조를 가집니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**가장 영향 큰 자원: Disk + Memory**

이유: 정적 실행 파일은 다음의 중심이기 때문입니다.

- 디스크 저장
- 메모리 매핑
- 코드 세그먼트 적재

| 자원 | 영향 |
|------|------|
| Disk | executable read |
| Memory | code/data mapping |
| CPU | instruction execution |
| Network | remote artifact distribution |

> **특히 대규모 시스템에서는:** binary size directly affects startup latency and deployment efficiency

---

## 4. 왜 중요한가

정적 실행 파일은 **the canonical deployment unit of executable software**입니다.

**중요한 이유:**

| 이유 | 설명 |
|------|------|
| reproducibility | 동일 실행 보장 |
| deployment portability | 배포 단위 |
| loader compatibility | OS 적재 가능 |
| startup initialization | 런타임 진입점 |
| security verification | signature/checksum |

> only structured executable formats can become runnable processes

즉 **실행 파일 포맷 자체가 운영체제와의 계약(contract) 역할**을 합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실행 파일 문제는 시스템 전체 장애로 직결됩니다.

**대표 장애:**

| 장애 | 원인 |
|------|------|
| executable corruption | binary damage |
| invalid format | unsupported architecture |
| missing shared libraries | unresolved dependencies |
| startup crash | invalid entry point |
| relocation failure | loader mismatch |
| permission denied | execution restriction |

**실무에서 자주 발생:**

| 문제 | 결과 |
|------|------|
| incompatible binary | immediate startup failure |
| oversized executable | slow deployment |
| broken linkage | runtime crash |
| architecture mismatch | illegal instruction |
| stripped symbols | debugging difficulty |

> **특히 컨테이너 환경에서는:** container startup failures frequently originate from executable incompatibility

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Compiler → Linker → Executable

```
source code
→ object files
→ linker
→ executable binary
```

- 컴파일러: 개별 코드 변환 수행
- 링커: 코드/라이브러리 결합 수행

---

### Binary Format Structure

실행 파일은 구조화된 바이너리입니다.

| 구성 | 역할 |
|------|------|
| header | loader metadata |
| text segment | executable instructions |
| data segment | initialized data |
| bss segment | zero-initialized memory |
| symbol table | debugging/linkage |
| relocation info | address resolution |

> 운영체제 로더는: **interpret executable metadata before memory mapping** 합니다.

---

### Entry Point

실행 파일에는 **a designated execution entry address**가 존재합니다.

운영체제는 다음을 수행합니다:
- executable load
- initial register setup
- instruction pointer jump

> **execution starts from the binary entry point**

---

### Memory Mapping

실행 파일은 메모리에 직접 복사되는 것이 아닙니다. 대부분 현대 OS는 **memory-map executable segments into virtual memory** 합니다.

| 효과 | 설명 |
|------|------|
| lazy loading | 필요한 페이지만 적재 |
| shared pages | 코드 공유 가능 |
| startup optimization | 빠른 실행 |

Linux 핵심 syscall: `mmap()`

---

### Static vs Dynamic Linking

**Static Linking:** all dependencies embedded into one executable

| 특징 | 결과 |
|------|------|
| standalone binary | 독립 실행 가능 |
| larger size | 파일 커짐 |
| deployment simplicity | 외부 의존 감소 |

**Dynamic Linking:** shared libraries resolved at runtime

| 특징 | 결과 |
|------|------|
| smaller binaries | 경량화 |
| shared memory usage | 라이브러리 공유 |
| runtime dependency risk | version mismatch 가능 |

---

### Loader Interaction

운영체제 로더(loader)는 **transform executable binaries into active process images**입니다.

| 작업 | 설명 |
|------|------|
| header parsing | format validation |
| address mapping | virtual memory setup |
| permission setup | RX/RW pages |
| relocation | address correction |
| runtime linker init | shared library resolution |

---

### Process Image Formation

실행 파일은 최종적으로 **a live process memory image**로 전환됩니다.

| 메모리 영역 | 역할 |
|------------|------|
| text | executable code |
| data | initialized globals |
| heap | dynamic allocation |
| stack | execution frames |
| shared libraries | runtime dependencies |

---

### Runtime Boundary

> **정적 실행 파일은 the boundary artifact between compile-time and runtime 입니다.**

| 이전 | 이후 |
|------|------|
| static binary | running process |
| disk artifact | scheduled execution |
| inactive bytes | active instructions |

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**대표 명령:**

| 명령 | 의미 |
|------|------|
| `file ./app` | executable type |
| `readelf -a ./app` | ELF metadata |
| `ldd ./app` | shared libraries |
| `objdump -d ./app` | machine code disassembly |
| `nm ./app` | symbols |

실행 권한 부여:
```bash
chmod +x app
```

---

### Runtime

| 요소 | 의미 |
|------|------|
| executable mapping | process image |
| loader activity | startup initialization |
| relocation | address adjustment |
| runtime bootstrap | VM/runtime init |

프로세스 메모리 매핑 확인:
```bash
cat /proc/<pid>/maps
```

---

### Kubernetes

K8s에서의 실행 흐름:

```
container image
→ executable binary
→ process creation
→ runtime startup
```

**실무 장애:**

| 문제 | 결과 |
|------|------|
| missing executable | container crash |
| wrong architecture | exec format error |
| invalid permissions | startup failure |
| broken dynamic linkage | runtime termination |

**대표 에러:**
- `exec format error`
- `no such file or directory`
- `permission denied`
- `illegal instruction`

---

### Observability

| 관측 항목 | 의미 |
|----------|------|
| startup latency | executable loading cost |
| RSS growth | mapped memory |
| page faults | lazy loading |
| relocation overhead | dynamic linking |
| binary size | deployment efficiency |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*