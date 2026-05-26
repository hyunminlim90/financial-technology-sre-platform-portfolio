# 재배치 가능 목적 코드 (Relocatable Object Code)

> 정독: 0회

## 1. 이 기술이 무엇인가

재배치 가능 목적 코드(Relocatable Object Code)는:

> 컴파일 완료 상태, 기계어 생성 완료, 실행 논리 존재, 하지만 최종 주소 미확정 상태

**대표 확장자:**

| 플랫폼 | 확장자 |
|--------|--------|
| Linux/Unix | `.o` |
| Windows | `.obj` |

> **핵심 특징:** the code is executable in logic but not yet executable as a complete process image

---

## 2. 시스템 어디에서 등장하는가

```
source
→ compilation
→ relocatable object code
→ linking
→ executable
```

| 단계 | 결과 |
|------|------|
| compiler frontend | AST/type analysis |
| backend codegen | machine instructions |
| assembler | object module |
| linker | final executable |

**주로 등장하는 시스템:**

| 영역 | 역할 |
|------|------|
| compiler toolchain | intermediate binary module |
| operating systems | executable assembly |
| embedded systems | firmware linking |
| kernels | modular builds |
| large-scale software | incremental builds |

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**가장 영향 큰 자원: Disk + CPU**

이유: 재배치 가능 목적 코드는 다음을 수행하기 때문입니다.

- 디스크에 저장
- 링크 단계에서 분석
- 심볼 해석
- 주소 계산

| 자원 | 영향 |
|------|------|
| Disk | object file storage |
| CPU | symbol resolution and relocation |
| Memory | linker workspace |
| Network | distributed build transfer |

> **대규모 빌드 시스템에서는:** link-time processing of object files can become a major CPU bottleneck

---

## 4. 왜 중요한가

재배치 가능 목적 코드는 **the modular boundary between compilation and final executable construction**입니다.

**중요 이유:**

| 이유 | 설명 |
|------|------|
| modular compilation | 파일별 독립 빌드 |
| incremental build | 일부만 재컴파일 가능 |
| reusable libraries | object reuse |
| linker flexibility | 주소 재배치 가능 |
| scalable software builds | 대형 프로젝트 분할 |

핵심: **the compiler does not yet know final executable memory layout**

그래서 다음을 수행합니다:
- 상대 주소 사용
- relocation metadata 저장
- symbol reference 유지

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 빌드 장애 대부분이 여기서 발생합니다.

**대표 장애:**

| 장애 | 원인 |
|------|------|
| undefined reference | 외부 심볼 미존재 |
| duplicate symbol | 중복 정의 |
| relocation failure | 주소 계산 실패 |
| ABI mismatch | object format 충돌 |
| incompatible architecture | CPU 아키텍처 불일치 |

**대표 에러:**
- `undefined reference`
- `multiple definition`
- `relocation truncated`
- `file format not recognized`

**실무에서 자주 발생:**

| 상황 | 결과 |
|------|------|
| static library mismatch | link failure |
| architecture mixing | invalid executable |
| compiler option inconsistency | relocation corruption |
| symbol visibility issue | runtime crash |

> **대규모 CI/CD에서는:** object-level incompatibility is a common distributed build failure source

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Object Module Structure

재배치 가능 목적 코드는 단순 기계어 파일이 아닙니다.

| 구성 요소 | 역할 |
|----------|------|
| machine instructions | CPU instructions |
| data sections | static data |
| symbol table | symbol metadata |
| relocation table | unresolved address info |
| section metadata | binary layout info |

> **object code still contains unresolved structural metadata**

---

### Relative Addressing

컴파일 단계에서는 **final executable memory addresses are unknown**입니다.

그래서 object code 내부에서는 다음을 사용합니다:
- 상대 주소
- placeholder
- symbolic references

예: `call printf` 명령에서 `printf`의 절대 주소는 아직 미확정 상태입니다.

---

### Symbol Table

심볼 테이블은 **mapping metadata between symbolic names and code/data locations**입니다.

| 항목 | 의미 |
|------|------|
| function names | exported symbols |
| global variables | accessible data |
| offsets | local section positions |
| visibility | linkage scope |

링커는 이것을 기반으로 모듈을 연결합니다.

---

### Relocation Table

재배치 테이블은 **locations inside machine code that require final address patching**입니다.

| 항목 | 의미 |
|------|------|
| unresolved function calls | later patching |
| external data access | address completion |
| jump targets | relocation required |

링커는 **rewrite placeholder addresses into final executable addresses** 를 수행합니다.

---

### Section-Based Layout

Object file은 section 기반 구조입니다.

| section | 역할 |
|---------|------|
| `.text` | machine instructions |
| `.data` | initialized data |
| `.bss` | uninitialized globals |
| `.rodata` | read-only constants |
| `.symtab` | symbols |
| `.rel.text` | relocation metadata |

> **object files are partially assembled executable fragments**

---

### Linker Dependency

재배치 가능 목적 코드는 **cannot execute independently**입니다.

이유:
- entry point 미완성
- external references 존재
- 주소 미확정
- process image 미완성

> **linking is required before process loading**

---

### Position Flexibility

왜 relocatable인가?

> **the linker may place the module anywhere inside final executable memory layout**

| 가능 | 의미 |
|------|------|
| code movement | 위치 이동 가능 |
| address rewriting | 주소 수정 가능 |
| executable reshaping | 전체 재배치 가능 |

---

### Build Scalability

대형 시스템은 **compile independently → link later** 방식을 사용합니다.

| 장점 | 효과 |
|------|------|
| parallel compilation | 빌드 속도 향상 |
| incremental rebuild | 변경 최소화 |
| module isolation | 유지보수 향상 |
| distributed builds | 대규모 CI 가능 |

---

### Static vs Dynamic Link Input

재배치 가능 목적 코드는 static linking과 dynamic linking 양쪽 모두의 입력이 됩니다.

> **relocatable object code is the universal intermediate binary unit before final linking**

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**대표 도구 및 사용 예:**

relocation entries 확인:
```bash
readelf -r app.o
```

symbol table 확인:
```bash
nm app.o
```

machine instructions 확인:
```bash
objdump -d app.o
```

**기타 도구:** `file`, `ar`, `ld`

---

### Runtime

재배치 가능 목적 코드는 런타임 직접 실행 대상은 아닙니다. 하지만 런타임 executable 생성의 기반입니다.

| 상태 | 의미 |
|------|------|
| object file | non-runnable |
| executable | runnable |
| process image | runtime active |

---

### Kubernetes

K8s에서는 직접 노출은 적지만 CI/CD에서 중요합니다.

```
source
→ object files
→ linking
→ executable
→ container image
→ pod
```

**실무 문제:**

| 문제 | 결과 |
|------|------|
| incompatible object files | image build failure |
| architecture mismatch | pod crash |
| linker failure | CI pipeline stop |
| symbol collision | runtime instability |

> **특히 multi-arch build에서 중요:** mixing incompatible relocatable objects can produce invalid container binaries

---

### Observability

| 관측 항목 | 의미 |
|----------|------|
| object size | module complexity |
| relocation count | unresolved references |
| symbol count | dependency complexity |
| link time | build scalability |
| section layout | binary organization |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*