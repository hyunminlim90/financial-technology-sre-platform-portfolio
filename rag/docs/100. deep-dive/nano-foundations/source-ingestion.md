# 소스 인입 (Source Ingestion)

> 정독: 0회

## 1. 이 기술이 무엇인가

소스 인입(Source Ingestion)은:

> 디스크에 저장된 source file을 읽고 compiler/runtime 내부 buffer에 적재한 뒤 분석 가능한 character stream 형태로 변환하여 lexer/parser에 공급하는 단계

**핵심:** source ingestion is the first executable boundary between storage and compiler pipeline

일반적으로 포함되는 작업:

| 작업 | 설명 |
|------|------|
| file open | 파일 핸들 확보 |
| encoding decode | UTF-8/UTF-16 처리 |
| buffering | 메모리 적재 |
| stream construction | 문자 스트림 생성 |
| preprocessing input | 전처리 입력 공급 |

---

## 2. 시스템 어디에서 등장하는가

소스 인입은 거의 모든 언어 처리 시스템의 시작점입니다.

| 영역 | 사용 위치 |
|------|----------|
| compiler | source loading |
| interpreter | script ingestion |
| IDE/LSP | realtime parsing |
| static analyzer | source scanning |
| transpiler | transformation input |
| build system | compilation bootstrap |

**대표 사례:**

| 시스템 | 역할 |
|--------|------|
| Clang | C/C++ source ingestion |
| Rustc | Rust parser input |
| TypeScript compiler | TS source loading |
| Babel | JS stream parsing |
| Python interpreter | script loading |

> **즉:** no parsing or compilation can begin before source ingestion completes

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

**가장 영향 큰 자원: Disk I/O + Memory**

이유: 소스 인입은 다음을 수행하기 때문입니다.

- filesystem access
- file buffering
- encoding transformation
- stream allocation

대규모 프로젝트에서는:

| 자원 | 영향 |
|------|------|
| Disk | source file read throughput |
| Memory | compiler buffers |
| CPU | decoding/token preparation |
| Network | remote source fetch |

특히 monorepo 환경에서는:

> **source ingestion latency can dominate incremental build startup time**

---

## 4. 왜 중요한가

소스 인입은 **compiler pipeline initialization boundary**입니다.

이 단계가 실패하면:

- lexer 실행 불가
- parser 실행 불가
- semantic analysis 불가
- build 자체 불가

**중요한 이유:**

| 이유 | 설명 |
|------|------|
| input integrity | 정확한 source 확보 |
| encoding correctness | 문자 해석 보장 |
| deterministic parsing | parser 안정성 |
| buffering efficiency | compile performance |
| dependency loading | module resolution |

> 특히 modern build system에서는: **efficient ingestion directly affects compilation scalability**

---

## 5. 실제 장애와 어떤 관련이 있는가

**대표 장애:**

| 장애 | 원인 |
|------|------|
| compile failure | missing source file |
| invalid encoding | UTF corruption |
| partial reads | filesystem issue |
| build slowdown | I/O bottleneck |
| parser corruption | malformed stream |
| dependency failure | unresolved include/import |

**실무에서 자주 발생:**

| 문제 | 결과 |
|------|------|
| broken symlink | source not found |
| inconsistent encoding | parser error |
| NFS latency | slow compile |
| container volume issue | missing files |
| corrupted cache | invalid incremental build |

CI/CD 환경에서는:

> **source ingestion instability propagates into full build pipeline failures**

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### File Loading

첫 단계: **compiler opens source files from filesystem**

일반 흐름:

```
filesystem
→ file descriptor
→ read buffer
→ compiler memory
```

| 요소 | 설명 |
|------|------|
| inode lookup | 파일 메타데이터 조회 |
| file descriptor | OS resource handle |
| page cache | kernel caching |
| buffered read | chunk loading |

---

### Character Stream Construction

파일은 내부적으로 **raw bytes**입니다. 컴파일러는 이를 **decoded character stream**으로 변환합니다.

| encoding | 의미 |
|----------|------|
| UTF-8 | 일반적 |
| UTF-16 | wide char |
| ASCII | legacy |
| Shift-JIS | locale-specific |

잘못된 인코딩은 다음을 유발합니다:
- lexer corruption
- invalid token generation
- parser failure

---

### Buffering

컴파일러는 source 전체를 **stream buffer or memory-mapped region**으로 유지합니다.

| 방식 | 특징 |
|------|------|
| buffered I/O | 일반적 |
| mmap | large source optimization |
| incremental buffering | streaming parser |

> 대규모 build에서는: **buffer management significantly affects compiler memory pressure**

---

### Stream Consumption

Lexer는 **consume source sequentially as character stream** 합니다.

```
source file
→ character stream
→ lexer
→ token stream
→ parser
→ AST
```

> source ingestion feeds lexer pipeline continuously

---

### Include / Import Resolution

현대 compiler ingestion은 단일 파일만 읽지 않습니다.

| 작업 | 의미 |
|------|------|
| include resolution | 헤더 탐색 |
| module loading | dependency import |
| path normalization | 경로 통합 |
| package resolution | module graph 구축 |

> **source ingestion often recursively expands dependency graphs**

---

### Incremental Ingestion

IDE/LSP에서는 매우 중요합니다. 현대 tooling은 **incrementally re-ingest modified source regions** 합니다.

목적:
- 빠른 autocomplete
- low-latency diagnostics
- realtime semantic updates

---

### Memory Mapping (mmap)

대규모 compiler에서 자주 사용합니다.

> **map file contents directly into virtual memory**

| 장점 | 설명 |
|------|------|
| reduced copy | 메모리 복사 감소 |
| OS page cache reuse | cache 효율 |
| fast random access | parser 최적화 |

---

### Source Normalization

인입 과정에서 수행되는 추가 처리:

| 처리 | 목적 |
|------|------|
| newline normalization | CRLF/LF 통합 |
| BOM stripping | encoding cleanup |
| whitespace handling | lexical consistency |
| macro preprocessing | pre-parser transform |

> 특히 C/C++ 계열은: **preprocessing itself becomes a major ingestion subphase**

---

### Frontend Boundary

소스 인입 이후부터 **compiler frontend analysis officially begins**입니다.

- ingestion → infrastructure
- frontend analysis → structural interpretation

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**관련 syscall:**
- `open()`
- `read()`
- `mmap()`
- `close()`

**관측 도구:**

| 도구 | 목적 |
|------|------|
| `strace` | file syscall tracing |
| `lsof` | open file 확인 |
| `perf` | I/O profiling |
| `iostat` | disk throughput |
| `vmstat` | page cache 상태 |

예:
```bash
strace -f gcc main.c
```

---

### Build Systems

| 시스템 | ingestion 역할 |
|--------|---------------|
| Make | source dependency scan |
| Ninja | parallel compile input |
| Bazel | distributed source loading |
| Gradle | source set ingestion |

> 대규모 build 병목: **filesystem latency frequently dominates frontend startup cost**

---

### Containers / Kubernetes

| 구성 | 영향 |
|------|------|
| bind mounts | source visibility |
| NFS volumes | ingestion latency |
| ephemeral storage | temp buffering |
| CI runners | compile startup |

**실무 사례:**

| 문제 | 결과 |
|------|------|
| slow PV | compile slowdown |
| overlayfs overhead | parser latency |
| remote repo fetch delay | build queue inflation |

---

### IDE / LSP Runtime

IDE는 지속적으로 ingestion을 수행합니다.

| 기능 | ingestion dependency |
|------|---------------------|
| live parsing | source reload |
| autocomplete | incremental ingestion |
| diagnostics | changed file streaming |
| symbol indexing | project scan |

> **modern IDE responsiveness strongly depends on ingestion efficiency**

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*