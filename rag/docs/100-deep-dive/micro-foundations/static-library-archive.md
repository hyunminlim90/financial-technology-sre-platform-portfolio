# 정적 라이브러리 아카이브 (Static Library Archive)

> 정독: 0회

## 1. 이 기술이 무엇인가

정적 라이브러리 아카이브(Static Library Archive)는:

> 여러 목적 코드(`.o` / `.obj`), 심볼 인덱스, 메타데이터를 하나의 파일로 묶은 링크용 바이너리 저장 단위

**대표 확장자:**

| 플랫폼 | 확장자 |
|--------|--------|
| Linux/Unix | `.a` |
| Windows | `.lib` |

> **핵심 특징:** static libraries are not executable processes

정적 라이브러리는:
- 실행 파일 아님
- 프로세스 아님
- 로딩 대상 아님

이며 **input material for the linker**입니다.

---

## 2. 시스템 어디에서 등장하는가

```
source
→ compilation
→ relocatable object files
→ static library archive
→ linker
→ executable
```

**주요 사용 영역:**

| 영역 | 역할 |
|------|------|
| operating systems | reusable kernel modules |
| embedded systems | firmware packaging |
| system libraries | libc-like packaging |
| game engines | shared engine logic |
| distributed build systems | modular binary reuse |

> 정적 라이브러리는 **binary-level reusable module packaging** 역할을 수행합니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**가장 영향 큰 자원: Disk + CPU**

| 자원 | 영향 |
|------|------|
| Disk | archive storage |
| CPU | linker symbol resolution |
| Memory | link-time extraction |
| Network | artifact distribution |

> **특히 대규모 CI에서는:** large static libraries can significantly increase link-time CPU and disk usage

---

## 4. 왜 중요한가

정적 라이브러리는 **the standard reusable binary distribution unit in native compilation toolchains**입니다.

**핵심 이유:**

| 이유 | 설명 |
|------|------|
| modular reuse | 기능 재사용 |
| build scalability | 대규모 빌드 지원 |
| binary distribution | 소스 없이 배포 가능 |
| selective linking | 필요한 모듈만 포함 |
| deterministic builds | 정적 종속성 확보 |

특히 중요한 개념:

> **only referenced object modules are extracted into final executables**

즉 라이브러리 전체가 아니라:
- 필요한 object module만
- 링크 시 추출
- 실행 파일에 복사

됩니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

**대표 장애:**

| 장애 | 원인 |
|------|------|
| undefined symbol | 라이브러리 누락 |
| duplicate symbols | 동일 함수 중복 |
| ABI mismatch | 컴파일 옵션 불일치 |
| architecture mismatch | x86 vs ARM 충돌 |
| static bloat | 실행 파일 과대화 |

**대표 오류:**
- `undefined reference`
- `multiple definition`
- `archive has no index`
- `incompatible library format`

**실무에서 자주 발생:**

| 상황 | 결과 |
|------|------|
| library order incorrect | link failure |
| stale static library | runtime instability |
| incompatible compiler flags | relocation errors |
| mixed architectures | invalid executable |

> **대규모 시스템에서는:** static library dependency management becomes a critical build engineering concern

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Archive Structure

정적 라이브러리는 단순 zip 파일이 아닙니다.

| 구성 요소 | 역할 |
|----------|------|
| object modules | machine code fragments |
| symbol index | lookup acceleration |
| archive headers | module metadata |

> **a static library is a structured binary archive optimized for linker traversal**

---

### Object Module Packaging

라이브러리는 **a container of relocatable object code**입니다.

| 항목 | 의미 |
|------|------|
| machine instructions | executable logic |
| relocation metadata | unresolved addresses |
| symbol tables | exported/imported names |

object file의 특성이 그대로 유지됩니다.

---

### Symbol Index

라이브러리 내부에는 **symbol lookup index**가 존재합니다.

| 역할 | 설명 |
|------|------|
| fast lookup | 함수 검색 |
| linker acceleration | 링크 속도 향상 |
| module discovery | object selection |

링커는 다음을 수행합니다:

```
scan unresolved symbols
→ search archive index
→ extract matching object module
```

---

### Selective Extraction

링커는 라이브러리 전체를 포함하지 않습니다.

> **only required object modules are copied into the executable**

예:

| 라이브러리 | 실제 포함 |
|-----------|---------|
| 100 modules | 3 referenced modules |

> **static linking is demand-driven module extraction**

---

### Copy-In Linking

링크 완료 후: **required library machine code becomes physically embedded inside the executable**입니다.

| 특징 | 의미 |
|------|------|
| self-contained executable | 외부 의존 감소 |
| larger binary size | 파일 증가 |
| runtime independence | library 없어도 실행 가능 |

---

### Relocation Preservation

라이브러리 내부 object module들은 **still remain relocatable before final executable binding** 상태입니다.

즉:
- 주소 미확정
- relocation table 유지
- symbol dependency 유지

> 최종 주소 결정은 **performed only during final linking**

---

### Static vs Dynamic Libraries

| 항목 | Static Library | Dynamic Library |
|------|---------------|-----------------|
| link time | copied into executable | external reference |
| runtime dependency | 없음 | 필요 |
| executable size | 큼 | 작음 |
| startup dependency | 낮음 | 높음 |
| update flexibility | 낮음 | 높음 |

> 정적 라이브러리는 **build-time integration model**입니다.

---

### Archive Tooling

**대표 도구:**

| 플랫폼 | 도구 |
|--------|------|
| Unix/Linux | `ar` |
| GNU | `ranlib` |
| Windows | `lib.exe` |

예:
```bash
ar rcs libmath.a add.o sub.o mul.o
```

이 과정에서 수행되는 작업:
- object aggregation
- symbol indexing
- archive metadata generation

---

### Build System Importance

대형 시스템은 **modular binary reuse through static libraries**를 기반으로 동작합니다.

| 영역 | 사례 |
|------|------|
| kernels | subsystem packaging |
| databases | storage engine reuse |
| game engines | rendering/audio modules |
| embedded firmware | reusable hardware drivers |

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

내부 object module 목록 확인:
```bash
ar -t libexample.a
```

exported symbols 확인:
```bash
nm libexample.a
```

symbol metadata 확인:
```bash
readelf -s libexample.a
```

**기타 도구:** `ranlib`, `objdump`, `ld`

---

### Runtime

정적 라이브러리는 런타임에 직접 존재하지 않습니다. 이미 **its selected machine code has been embedded into the executable image** 상태입니다.

| 상태 | 의미 |
|------|------|
| library archive | gone |
| executable image | active |
| process memory | machine code loaded |

---

### Kubernetes

직접 노출은 적지만 **container binaries frequently contain statically linked library code**입니다.

```
source
→ object files
→ static libraries
→ executable
→ container image
→ pod
```

**실무 장애:**

| 문제 | 결과 |
|------|------|
| static link mismatch | image build failure |
| incompatible archive | pod startup crash |
| oversized static binaries | container bloat |
| architecture mismatch | invalid image |

> 특히 **multi-stage build**에서 중요합니다.

---

### Observability

| 관측 항목 | 의미 |
|----------|------|
| archive size | dependency weight |
| symbol count | exported API scale |
| extracted modules | actual runtime footprint |
| link duration | build scalability |
| binary growth | static dependency expansion |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*