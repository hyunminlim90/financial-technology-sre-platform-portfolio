# 가상 머신 실행 파일 (Virtual Machine Executable File)

> 정독: 0회

## 1. 이 기술이 무엇인가

가상 머신 실행 파일(**virtual-machine executable binary artifact**)은:

> 고급 소스 언어 컴파일 결과, 바이트코드 패키지, 실행 가능한 정적 바이너리를 담는 파일 포맷

### 핵심 특징

| 특징 | 설명 |
|------|------|
| binary format | 바이너리 구조 |
| VM-targeted | 가상 머신 대상 |
| platform-independent | 플랫폼 독립 |
| persistent artifact | 디스크 영구 저장 |
| runtime-loadable | 런타임 적재 가능 |

> 핵심 정의: **persistent executable container for virtual-machine instructions**

---

## 2. 시스템 어디에서 등장하는가

### 등장 위치

| 위치 | 역할 |
|------|------|
| compiler output | 컴파일 결과 |
| build artifact | 빌드 산출물 |
| package system | 배포 단위 |
| class/module loader | 런타임 입력 |
| runtime linker | 심볼 연결 |
| execution engine | 바이트코드 공급원 |

### 전체 흐름

```
source code
→ compiler
→ VM executable file
→ class/module loader
→ memory loading
→ execution engine
→ native execution
```

> 즉 이 파일은 **static-to-runtime transition artifact**입니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접 영향: **Disk + Memory**

| 자원 | 영향 |
|------|------|
| Disk | 파일 저장 |
| Memory | runtime loading |
| CPU | verification/parsing |
| Network | artifact distribution |
| Cache | class/module caching |

특히 중요한 것은 **startup loading behavior**입니다.

실행 파일 크기와 구조는 다음에 직접 영향합니다:

- startup latency
- class loading time
- memory footprint
- verification overhead

---

## 4. 왜 중요한가

핵심 목적: **platform-independent executable distribution**

즉 다음 환경 차이를 숨깁니다:

- Windows / Linux / macOS
- ARM / x86

또한 **runtime execution portability**를 제공합니다.

실행 파일은 다음을 하나의 규격 안에 캡슐화합니다:

- instruction metadata
- symbolic references
- runtime descriptors
- executable bytecode

---

## 5. 실제 장애와 어떤 관련이 있는가

### 대표 장애

| 장애 | 설명 |
|------|------|
| corrupted executable | 실행 실패 |
| invalid magic number | 파일 거부 |
| version mismatch | runtime incompatibility |
| class/module loading failure | startup failure |
| dependency resolution failure | linking 오류 |
| malformed metadata | verification crash |
| oversized artifact | startup degradation |

특히 중요한 것은 **runtime loading instability**입니다.

다음은 startup 성능을 악화시킵니다:

- dependency explosion
- excessive module scanning
- large constant pool
- metadata inflation

또한 **runtime compatibility mismatch**도 매우 중요합니다.

실행 파일 규격과 런타임 버전이 맞지 않으면 다음이 발생합니다:

- verification failure
- unsupported bytecode version
- invalid symbol resolution

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### Binary Artifact Structure

가상 머신 실행 파일은 **structured executable binary format**입니다. 단순 바이트 나열이 아니며, 내부에는 다음이 구조적으로 배치됩니다:

- metadata
- instruction stream
- symbolic references
- descriptors

### Magic Number Verification

파일 시작부에는 **format identity signature**가 존재합니다.

런타임은 이를 검사하여 다음을 확인합니다:

- 정상 파일 여부
- 규격 일치 여부

### Constant Pool / Symbol Table

실행 파일 내부에는 **symbolic reference repository**가 존재합니다.

| 항목 | 의미 |
|------|------|
| class names | 타입 식별 |
| method names | 메서드 참조 |
| string literals | 문자열 상수 |
| field descriptors | 필드 메타데이터 |

실행 중 실제 메모리 주소로 해석됩니다.

### Bytecode Storage

핵심 실행 로직은 **virtual instruction stream** 형태로 저장됩니다.

실행 엔진은 이를 읽어 다음을 수행합니다:

- interpret
- optimize
- JIT compile

### Metadata Encoding

실행 파일은 단순 코드 저장소가 아닌 **runtime metadata container**입니다.

포함 정보:

- type descriptors
- inheritance metadata
- access flags
- interface tables

### Runtime Loading

실행 시 **loader subsystem**이 파일을 메모리로 적재합니다.

```
disk
→ binary parsing
→ verification
→ metadata creation
→ memory mapping
→ runtime linkage
```

### Verification Phase

런타임은 실행 전 **binary integrity verification**을 수행합니다.

| 항목 | 의미 |
|------|------|
| bytecode validity | 명령 검증 |
| type safety | 타입 무결성 |
| symbol consistency | 참조 일관성 |
| format correctness | 구조 적합성 |

### Dynamic Linking

실행 파일 내부 참조는 대부분 **symbolic references**입니다.

실행 중 다음으로 연결됩니다:

- 실제 메모리 주소
- 실제 타입
- 실제 메서드

### Runtime Metadata Materialization

파일 내부 메타데이터는 실행 중 **runtime metadata structures**로 변환됩니다.

즉 다음이 메모리에 새로 형성됩니다:

- class metadata
- virtual dispatch tables
- runtime descriptors

### Executable Lifecycle

가상 머신 실행 파일은 **persistent executable artifact**입니다.

다음 생명주기를 가집니다:

```
디스크 저장 → 로딩 → 검증 → 메모리 적재 → 실행 → 언로드
```

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

대표 관측 도구: `lsof`, `strace`, `perf`, `vmstat`, `iostat`

| 현상 | 의미 |
|------|------|
| high disk read | executable loading |
| page cache growth | binary caching |
| startup I/O burst | module loading |
| mmap activity | binary mapping |
| metadata parsing CPU | verification overhead |

### Runtime

| 항목 | 의미 |
|------|------|
| loaded classes/modules | 적재 수 |
| verification time | 검증 비용 |
| constant pool size | 메타데이터 크기 |
| code cache usage | native 변환량 |
| startup duration | 초기화 비용 |

> 중요 항목: **runtime loading overhead**

### Kubernetes

| 현상 | 원인 |
|------|------|
| slow pod startup | artifact loading |
| image bloat | excessive binaries |
| memory overhead | metadata inflation |
| cold start latency | verification + linking |
| node I/O spike | simultaneous loading |

특히 중요한 것은 **startup initialization amplification**입니다.

대규모 환경에서는 다음이 노드 I/O 병목을 유발할 수 있습니다:

- 수백 pod 동시 시작
- 대형 artifact loading
- metadata parsing

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*