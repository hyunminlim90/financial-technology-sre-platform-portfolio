# Executable File
## 1. 실행 파일이란 무엇인가

실행 파일(Executable File)은:

> 운영체제가 실행할 수 있는 형식으로 저장된 **프로그램의 정적 파일**

**"CPU가 실행할 명령어와 필요한 데이터가 OS 규격에 맞게 포장된 파일"**

| 개념 | 설명 |
|------|------|
| **Executable File** | 디스크에 저장된 정적 이미지 |
| **Process** | 메모리에 올라가 실행 중인 동적 상태 |

**대표적인 실행 파일 포맷:**

| OS | 포맷 |
|----|------|
| Linux | ELF |
| Windows | PE |
| macOS | Mach-O |

---

## 2. 시스템 어디에서 등장하는가

실행 파일은 보조기억장치에 저장되어 있다가, 실행 시 운영체제 로더에 의해 주기억장치로 적재됩니다.

```
Source Code
  ↓
Compile
  ↓
Link
  ↓
Executable File
  ↓
Loader
  ↓
Process
  ↓
CPU Execution
```

**시스템 내 위치:**

```
SSD / HDD
  ↓
File System
  ↓
Executable File
  ↓
OS Loader
  ↓
Main Memory
  ↓
Process Address Space
```

**컨테이너 환경에서도 본질은 동일합니다:**

```
Container Image
  ↓
Filesystem Layer
  ↓
Executable File
  ↓
Process
```

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향 |
|------|------|
| Disk | 실행 파일 읽기, 라이브러리 로딩 |
| Memory | 코드 / 데이터 섹션 적재 |
| CPU | 적재 후 명령어 실행 |
| Network | 직접 영향 없음, 프로그램 기능에 따라 발생 |
| Security | 실행 권한, 서명, 무결성 검증 |

```
실행 전  →  Disk 자원
실행 중  →  Memory + CPU 자원
```

---

## 4. 왜 중요한가

실행 파일은 소스 코드가 실제 시스템에서 실행 가능한 형태로 변환된 **결과물**이자 **배포된 코드의 물리적 실체**입니다.

**운영 관점에서 확인해야 할 사항:**

- 파일이 없는가?
- 실행 권한이 없는가?
- CPU 아키텍처가 다른가?
- 필요한 라이브러리가 없는가?
- 진입점이 잘못되었는가?
- 실행 파일이 손상되었는가?

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. 실행 권한 없음

```bash
Permission denied
```

실행 파일에 실행 권한이 없으면 프로세스로 시작되지 못합니다.

```bash
chmod +x ./app
```

### 5-2. 잘못된 아키텍처

x86_64 서버에서 ARM용 바이너리를 실행하면 실패합니다.

```
exec format error
```

### 5-3. 동적 라이브러리 누락

```
error while loading shared libraries
```

### 5-4. 잘못된 진입점 / 손상된 파일

```
cannot execute binary file
invalid ELF header
```

### 5-5. 컨테이너 시작 실패

```
CrashLoopBackOff
RunContainerError
exec format error
permission denied
no such file or directory
```

---

## 6. 핵심 메커니즘 요약

### 6-1. 실행 파일은 OS가 읽을 수 있는 포맷을 가진다

실행 파일은 단순한 바이트 덩어리가 아니라 **구조화된 바이너리**입니다.

**Linux ELF 구조:**

```
ELF Header
Program Header
.text
.rodata
.data
.bss
Dynamic Section
Section Header
```

### 6-2. Header는 실행 조건을 설명한다

헤더에는 CPU Architecture, OS / ABI, Entry Point, Program Header 위치, Section 정보가 포함됩니다. 운영체제 로더는 이 정보를 보고 실행 가능 여부를 판단합니다.

### 6-3. 각 섹션의 역할

| 섹션 | 역할 |
|------|------|
| `.text` | CPU가 실행할 기계어 명령어 (읽기 전용) |
| `.data` | 초기값이 있는 전역 / 정적 데이터 |
| `.bss` | 초기값 없거나 0으로 초기화되는 데이터 |
| `.rodata` | 읽기 전용 상수 데이터 |

### 6-4. 실행되면 프로세스 주소 공간으로 매핑된다

```
Executable File
  ↓
Kernel Loader
  ↓
Virtual Memory Mapping
  ↓
Code / Data Segment
  ↓
Heap / Stack 생성
  ↓
Program Counter = Entry Point
  ↓
Execution Start
```

### 6-5. 동적 링킹이 실행 실패의 핵심 원인이 될 수 있다

```
Executable
  ↓
Dynamic Linker
  ↓
Shared Libraries
  ↓
Process Start
```

라이브러리가 없거나 버전이 맞지 않으면 **실행 전에 실패**합니다.

---

## 7. Linux / Runtime / K8s 에서 관측 방법

### Linux

```bash
# 파일 종류 확인
file ./app
# → ELF 64-bit LSB executable, x86-64

# 실행 권한 확인 (x 권한 존재 여부)
ls -l ./app

# ELF 헤더 확인
readelf -h ./app

# 섹션 확인
readelf -S ./app

# 동적 라이브러리 의존성 확인
ldd ./app

# 실제 시스템 콜 추적
strace ./app

# 실행 중인 프로세스의 원본 확인
readlink /proc/<PID>/exe

# 메모리 매핑 확인
cat /proc/<PID>/maps
```

### Runtime

특정 런타임과 무관하게 다음을 확인합니다.

| 항목 | 핵심 질문 |
|------|-----------|
| 실행 명령 / Entry Point | 무엇을 실행하려 했는가? |
| Arguments / Environment Variables | 올바른 환경으로 실행했는가? |
| Exit Code | 정상 종료했는가? |
| Shared Library Path | 의존 라이브러리가 존재하는가? |
| File Permission | OS가 실행 가능한 포맷인가? |

### Kubernetes

```bash
# 컨테이너 시작 실패 확인
kubectl describe pod

# 컨테이너 로그 확인
kubectl logs <pod>

# 이미지 내부 실행 파일 확인
kubectl exec -it <pod> -- ls -l /path/to/app

# 이미지 아키텍처 확인 (Architecture, Os, Entrypoint, Cmd)
docker image inspect <image>
```

**대표 이벤트 / 상태:**

| 상태 / 에러 | 의미 |
|------------|------|
| `exec format error` | CPU 아키텍처 불일치 |
| `permission denied` | 실행 권한 없음 |
| `no such file or directory` | 실행 파일 경로 없음 |
| `CrashLoopBackOff` | 반복 비정상 종료 |
| `RunContainerError` | 컨테이너 시작 자체 실패 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*