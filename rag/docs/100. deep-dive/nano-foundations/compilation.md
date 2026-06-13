# 컴파일레이션 (Compilation)

> 정독: 0회

## 1. 이 기술이 무엇인가

컴파일레이션(Compilation)은:

> 소스 프로그램을 특정 타깃 플랫폼에서 실행 가능한 바이너리로 변환하는 과정

**입력:** 소스 코드  

**출력:** 목적 파일(Object File), 실행 파일(Executable), 라이브러리(Library)

컴파일러는 단순히 문법을 번역하는 것이 아니라, **타깃 CPU + 타깃 OS + ABI** 규칙을 반영하여 실제 실행 가능한 기계어를 생성합니다.

---

## 2. 시스템 어디에서 등장하는가

컴파일레이션은 프로그램이 실행되기 전 단계에서 등장합니다.

```
소스 프로그램
↓
컴파일레이션
↓
목적 파일
↓
링킹 (Linking)
↓
실행 파일
↓
운영체제 적재
↓
프로세스 실행
```

즉, **개발 단계**와 **빌드 단계**의 핵심 공정입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

컴파일 시점에는 **CPU**, **Memory**, **Disk** 영향이 큽니다.

| 자원 | 역할 |
|------|------|
| CPU | 구문 분석, 최적화, 기계어 생성 수행. 대형 프로젝트일수록 사용량 증가 |
| Memory | AST, IR, 심볼 테이블, 최적화 데이터 저장 |
| Disk | Object File, Library, Executable, Debug Symbol 생성 및 저장 |

> Network는 일반적으로 직접 관련이 없습니다.

---

## 4. 왜 중요한가

컴파일레이션은 사람이 작성한 논리를 CPU가 실행 가능한 형태로 바꾸는 유일한 과정입니다.

이 과정에서 다음이 상당 부분 결정됩니다.

- 성능
- 메모리 사용량
- 실행 파일 크기

컴파일러는 정렬, 패딩, 함수 호출 규약, 레지스터 사용까지 결정합니다. 따라서 같은 소스 코드라도 **컴파일러**, **옵션**, **타깃 플랫폼**에 따라 결과가 달라질 수 있습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

| 장애 유형 | 원인 | 결과 |
|-----------|------|------|
| ABI 불일치 | Library A와 Application B를 서로 다른 ABI로 컴파일 | Crash, Memory Corruption |
| 잘못된 타깃 지정 | ARM용으로 컴파일 후 x86 서버에서 실행 | `Exec Format Error` |
| 최적화 관련 장애 | `-O0` / `-O3` 차이 | Race Condition 노출, Undefined Behavior 발생 |
| 구조체 레이아웃 차이 | ABI 또는 플랫폼 차이로 레이아웃 불일치 | 네트워크 프로토콜 오류, 파일 포맷 오류, 메모리 해석 오류 |

---

## 6. 핵심 메커니즘

현재 문맥에서는 다음 흐름이 가장 중요합니다.

```
소스 프로그램
↓
컴파일러
↓
ABI 적용
↓
비트 필드 레이아웃 생성
↓
바이너리 생성
```

아래 선언을 예로 들면:

```c
struct Status {
    unsigned int mode : 3;
    unsigned int flag : 1;
};
```

컴파일러는 비트 폭, 정렬, 패딩, 부호 규칙을 분석한 뒤, ABI + 타깃 CPU + 타깃 OS 규칙을 적용하여 비트 위치, 메모리 배치, 구조체 크기를 결정합니다.

즉, 컴파일레이션은 다음 변환 단계입니다.

```
논리적 비트 필드 구조
↓
물리적 메모리 구조
```

지금까지 등장한 아래 항목은 모두 컴파일 단계에서 결정됩니다.

- Bit-field Physical Layout
- Bit-field Data Alignment
- Bit-field Padding Rule
- Sign Encoding

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 컴파일러 확인
gcc --version
clang --version

# 바이너리 정보 확인
file app
# 출력 예: ELF 64-bit executable

# ELF 헤더 확인
readelf -h app

# 심볼 확인
nm app

# 디스어셈블
objdump -d app
```

빌드 시스템(`make`, `cmake`, `ninja`, `bazel` 등)이 컴파일 과정을 호출합니다.

### Kubernetes

Kubernetes에서는 직접 컴파일하지 않으며, 보통 다음 단계에서 수행됩니다.

- CI/CD 파이프라인
- Docker Build
- Container Image Build

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*