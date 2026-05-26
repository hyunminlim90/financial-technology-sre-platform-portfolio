# 정적 링크 (Static Linking)

> 정독: 0회

## 1. 이 기술이 무엇인가

정적 링크(Static Linking)는:

> 목적 코드(Object File), 정적 라이브러리(Static Library), 심볼(Symbol)을 하나의 실행 가능한 바이너리로 통합

컴파일러는 일반적으로 `source file → object file` 단위로 번역합니다.

하지만 이 상태에서는:

- 함수 위치 미확정
- 외부 참조 미해결
- 메모리 주소 미배정

상태입니다.

**최종 결과:** a self-contained executable with resolved addresses

<details>
<summary>Deep Dive</summary></br>

Relocatable Object Code(재배치 가능 목적 코드) [[M]](../../100-deep-dive/micro-foundations/relocatable-object-code.md)  
Static Library Archive(정적 라이브러리 아카이브) [[M]](../../100-deep-dive/micro-foundations/static-library-archive.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

정적 링크는 컴파일 이후, 실행 파일 생성 직전에 등장합니다.

```
source
→ compilation
→ object files
→ static linking
→ executable binary
```

| 영역 | 역할 |
|------|------|
| compiler toolchain | executable assembly |
| operating systems | binary loading |
| embedded systems | standalone binaries |
| container images | dependency embedding |
| build systems | final artifact creation |

**대표 도구:**

| 플랫폼 | 도구 |
|--------|------|
| Linux | `ld` |
| LLVM ecosystem | `lld` |
| Windows | `link.exe` |
| GNU toolchain | gcc linker stage |

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**가장 영향 큰 자원: Disk + Memory**

이유: 정적 링크는 다음을 수행하기 때문입니다.

- 바이너리 병합
- 심볼 테이블 생성
- 재배치(Relocation)
- 실행 이미지 생성

| 자원 | 영향 |
|------|------|
| Disk | executable size 증가 |
| Memory | static library embedding |
| CPU | symbol resolution |
| Network | deployment artifact transfer |

> **대규모 시스템에서는:** static linking can significantly increase binary size

---

## 4. 왜 중요한가

정적 링크는 **the stage where fragmented program modules become one runnable address space**입니다.

**중요한 이유:**

| 이유 | 설명 |
|------|------|
| executable completeness | 단일 실행 파일 생성 |
| symbol resolution | 외부 참조 해결 |
| deployment simplicity | 의존성 감소 |
| startup reliability | 런타임 의존 최소화 |
| portability | standalone execution |

> 정적 링크 완료 후에는: **the operating system can directly load the executable image** 상태가 됩니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

링크 단계 문제는 빌드 실패 및 런타임 장애로 직결됩니다.

**대표 장애:**

| 장애 | 원인 |
|------|------|
| undefined reference | 심볼 미해결 |
| duplicate symbol | 중복 정의 |
| ABI mismatch | binary incompatibility |
| relocation overflow | 주소 계산 실패 |
| missing static library | 링크 실패 |

**실무에서 자주 발생:**

| 문제 | 결과 |
|------|------|
| wrong library version | build break |
| architecture mismatch | invalid binary |
| incompatible object files | linker error |
| oversized binaries | deployment inefficiency |
| incorrect relocation | runtime crash |

**대표 에러:**
- `undefined reference to symbol`
- `multiple definition of`
- `relocation truncated`

> **특히 대규모 CI/CD에서는:** link failures frequently block artifact generation pipelines

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Object Files

컴파일 결과는 보통 **partially translated binary modules**입니다.

| 상태 | 의미 |
|------|------|
| unresolved symbols | 아직 주소 미확정 |
| relocatable code | 이동 가능 코드 |
| separated modules | 독립 번역 상태 |

> **object files are not yet executable programs**

---

### Symbol Resolution

정적 링크 핵심: 링커는 **match symbol references to actual symbol definitions** 합니다.

| 참조 | 실제 정의 |
|------|----------|
| function call | external function body |
| global variable | actual memory location |
| library API | linked implementation |

심볼 테이블(Symbol Table)을 사용합니다.

---

### Relocation

컴파일 시점에는 **final memory addresses are unknown**입니다.

그래서 목적 코드는 다음을 포함합니다:
- 상대 주소
- placeholder
- relocation entries

링커는 **rewrite instruction addresses into final executable positions** 합니다.

---

### Static Library Inclusion

정적 링크에서는 **library code becomes embedded into the final executable**입니다.

| 특징 | 결과 |
|------|------|
| standalone execution | 외부 의존 감소 |
| larger executable | 파일 크기 증가 |
| independent deployment | 단독 실행 가능 |

**대표 확장자:**

| 플랫폼 | 확장자 |
|--------|--------|
| Linux | `.a` |
| Windows | `.lib` |

---

### Final Binary Layout

링커는 최종적으로 **construct a complete executable memory image** 합니다.

| 세그먼트 | 역할 |
|---------|------|
| text | machine instructions |
| data | initialized globals |
| rodata | read-only constants |
| bss | zero-initialized memory |
| symbol metadata | debugging/linking |

---

### Entry Point Resolution

링커는 **define the executable entry address** 합니다.

운영체제 로더는 이 주소로 점프하여 실행을 시작합니다.

---

### Static vs Dynamic Linking

**Static Linking:** dependencies resolved during build time

| 특징 | 결과 |
|------|------|
| standalone binary | 독립 실행 |
| larger size | 실행 파일 증가 |
| fewer runtime dependencies | 런타임 안정성 증가 |

**Dynamic Linking:** dependencies resolved during runtime

| 특징 | 결과 |
|------|------|
| smaller binaries | 경량화 |
| shared libraries | 메모리 공유 |
| runtime dependency risk | 버전 충돌 가능 |

---

### Address Space Preparation

정적 링크 완료 시: **the executable already contains resolved internal address relationships**입니다.

즉 런타임 전에 다음이 대부분 완료됩니다:
- 코드 배치
- 함수 연결
- 내부 주소 계산

---

### Build-Time Boundary

> **정적 링크는 the final build-time consolidation stage before runtime activation 입니다.**

| 이전 | 이후 |
|------|------|
| fragmented modules | unified executable |
| unresolved references | fixed symbol bindings |
| relocatable objects | runnable binary |

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**대표 도구:**

| 명령 | 역할 |
|------|------|
| `ld` | linker |
| `ar` | static library archive |
| `nm` | symbol inspection |
| `objdump` | binary disassembly |
| `readelf` | ELF metadata |
| `ldd` | shared dependency 확인 |

정적 링크 여부 확인:
```bash
ldd ./app
```

출력이 `not a dynamic executable`이면 완전 정적 링크 가능성이 높습니다.

---

### Runtime

| 요소 | 의미 |
|------|------|
| executable mapping | 메모리 적재 |
| relocation state | 주소 확정 상태 |
| loader activity | minimal dynamic dependency |
| symbol binding | 이미 완료됨 |

프로세스 메모리 매핑 확인:
```bash
cat /proc/<pid>/maps
```

---

### Kubernetes

K8s에서는 정적 링크가 매우 중요합니다.

```
statically linked executable
→ container image
→ pod startup
```

**장점:**

| 장점 | 의미 |
|------|------|
| minimal runtime deps | 컨테이너 단순화 |
| scratch images 가능 | 초경량 이미지 |
| predictable deployment | 안정적 배포 |

**실무 장애:**

| 문제 | 결과 |
|------|------|
| wrong architecture | CrashLoopBackOff |
| incompatible libc | startup failure |
| oversized binary | slow image pull |
| stripped symbols | debugging difficulty |

**대표 에러:**
- `exec format error`
- `cannot execute binary file`
- `segmentation fault`

---

### Observability

| 관측 항목 | 의미 |
|----------|------|
| binary size | artifact efficiency |
| symbol count | linkage complexity |
| relocation entries | address adjustments |
| startup latency | executable loading |
| dependency footprint | runtime isolation |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*