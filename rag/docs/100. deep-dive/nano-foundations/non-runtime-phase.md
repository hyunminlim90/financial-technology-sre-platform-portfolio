# 비실행 시점 (Non-Runtime Phase)

> 정독: 0회

## 1. 이 기술이 무엇인가

비실행 시점(Non-Runtime Phase)은:

> 프로그램이 아직 실행되지 않았고, CPU가 해당 프로그램 명령을 수행하지 않으며, 프로세스가 살아 움직이지 않는 상태를 의미

이 단계에서 존재하는 것은 주로:

| 요소 | 상태 |
|------|------|
| source code | static text |
| executable artifact | stored binary |
| metadata | serialized |
| bytecode/native file | disk resident |
| type definitions | compile-time fixed |

**핵심:**

> non-runtime phase contains executable structure without active execution

---

## 2. 시스템 어디에서 등장하는가

비실행 시점은 다음 영역에서 등장합니다.

| 영역 | 설명 |
|------|------|
| source repository | source storage |
| build pipeline | compile/package stage |
| artifact repository | binary storage |
| container image | packaged executable |
| filesystem | static persistence |

### 관련 시스템

| 시스템 | 역할 |
|--------|------|
| compiler | executable generation |
| linker | symbol integration |
| package manager | dependency packaging |
| image builder | deployable artifact creation |
| storage subsystem | static persistence |

> non-runtime phase is static executable existence before activation

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**가장 영향 큰 자원: Disk + Build CPU + Artifact Storage**

비실행 시점의 핵심 활동은:

- compile
- package
- archive
- distribute
- persist

### 자원별 영향

| 자원 | 영향 |
|------|------|
| Disk | artifact persistence |
| CPU | compilation/build |
| Memory | compiler analysis |
| Network | artifact distribution |

### 런타임과의 차이

| Non-runtime | Runtime |
|-------------|---------|
| artifact manipulation | live execution |
| static persistence | active memory mutation |
| build workload | execution workload |

---

## 4. 왜 중요한가

비실행 시점은:

> **runtime correctness foundation**

런타임 안정성 대부분은:

- build correctness
- binary integrity
- metadata consistency
- dependency validity

에 의해 결정됩니다.

런타임 이전에:

- 타입 오류 제거
- ABI 검증
- 심볼 연결
- 메타데이터 생성
- 최적화 수행

을 끝냅니다.

### 중요한 이유

| 이유 | 설명 |
|------|------|
| deterministic execution | 예측 가능성 |
| safety | 타입 안정성 |
| deployability | 배포 가능성 |
| reproducibility | 재현 가능성 |
| optimization | 실행 성능 향상 |

> runtime quality is heavily constrained by non-runtime correctness

---

## 5. 실제 장애와 어떤 관련이 있는가

비실행 시점 문제는 실제 운영 장애로 직접 연결됩니다.

### 대표 사례

| 장애 | 원인 |
|------|------|
| invalid binary | corrupted build |
| missing symbols | link failure |
| runtime crash | compile mismatch |
| incompatible deployment | ABI incompatibility |
| startup failure | broken metadata |
| container boot failure | packaging issue |

> many runtime failures originate from non-runtime defects

### 문제별 결과

| 문제 | 결과 |
|------|------|
| wrong compile flags | illegal instruction |
| incompatible architecture | startup abort |
| broken dependency graph | runtime linkage failure |
| invalid metadata | loader crash |
| inconsistent builds | deployment drift |

### SRE 관점

| 영역 | 중요성 |
|------|--------|
| CI/CD | artifact integrity |
| supply chain | trusted builds |
| reproducibility | rollback reliability |
| immutable artifacts | deployment consistency |

> non-runtime integrity is production reliability concern

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Static Artifact State

> program exists as stored representation, not active execution

실행 파일 / 라이브러리 / 바이트코드 / 메타데이터가 저장되어 있을 뿐, 아직 CPU 실행은 없음.

### Compile-Time Validation

비실행 시점에서 수행:

| 작업 | 목적 |
|------|------|
| syntax validation | 문법 검증 |
| type checking | 타입 안정성 |
| symbol resolution | 참조 연결 |
| optimization | 실행 효율 향상 |
| metadata generation | runtime 지원 |

> non-runtime phase establishes executable correctness before execution

### Executable Packaging

비실행 시점 산출물:

| 형태 | 예 |
|------|----|
| object file | relocatable code |
| executable binary | runnable artifact |
| bytecode package | VM artifact |
| shared library | reusable runtime module |
| container image | deployment unit |

### Serialized Metadata

비실행 상태에서는:

> runtime structures do not yet exist physically

존재하는 것:
- serialized metadata
- static descriptors
- binary encoding
- layout definitions

| Runtime Object | Non-runtime Equivalent |
|----------------|------------------------|
| live heap object | metadata description |
| active thread | executable entry |
| allocated memory | binary declaration |

### Runtime Boundary

> runtime begins when executable artifacts become active process state

다음 순간부터 Runtime:

- process creation
- memory mapping
- loader activation
- instruction execution
- thread scheduling

### Loader Transition

비실행 → 실행 전환:

| 단계 | 설명 |
|------|------|
| disk artifact | static state |
| loader parse | activation |
| memory mapping | runtime entry |
| executable sections mapped | active execution state |

### Static vs Dynamic State

| Non-runtime | Runtime |
|-------------|---------|
| static artifact | active process |
| no execution | instruction execution |
| disk persistence | RAM residency |
| immutable structure | mutable state |
| no scheduler activity | active scheduling |

### No Active Instruction Cycle

비실행 시점에서는:

> no active CPU instruction execution exists for the program

- fetch 없음
- decode 없음
- execute 없음
- write-back 없음

프로그램은 단순 저장 상태.

### Memory Absence

> runtime heap objects do not exist during non-runtime phase

- 객체 없음
- 스레드 없음
- 힙 점유 없음
- 런타임 메타데이터 없음

존재하는 것: 파일 / 바이트열 / 정적 명세

### Deployment Perspective

현대 플랫폼에서 비실행 시점 산출물은:

- container image
- OCI artifact
- binary package
- immutable deployment unit

형태로 운영 환경에 배포됩니다.

> deployment systems primarily manipulate non-runtime artifacts

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**대표 관측 대상:**

| 대상 | 설명 |
|------|------|
| ELF binary | executable artifact |
| shared library | runtime dependency |
| filesystem inode | static storage |
| package metadata | deployment information |

**대표 도구:** `file`, `readelf`, `objdump`, `nm`, `strings`, `ldd`, `sha256sum`

**관측 가능:**

| 항목 | 의미 |
|------|------|
| architecture | ISA target |
| symbol table | linkage info |
| sections | executable layout |
| dependencies | shared library graph |
| metadata | runtime descriptors |

### Container / Kubernetes

K8s 대부분 배포 객체는 **non-runtime deployable artifacts**입니다.

| 요소 | 상태 |
|------|------|
| container image | static |
| OCI layer | static |
| Helm chart | static |
| YAML manifest | static |
| binary artifact | static |

실행 전까지는: CPU execution 없음 / heap 없음 / process 없음

### CI/CD

| 항목 | 의미 |
|------|------|
| build reproducibility | deterministic artifact |
| artifact integrity | tamper detection |
| checksum validation | consistency |
| dependency graph | supply-chain stability |

> artifact immutability is critical for operational reliability

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*