# 애플리케이션 이진 인터페이스 (Application Binary Interface, ABI)

> 정독: 0회

## 1. 이 기술이 무엇인가

ABI(Application Binary Interface)는:

> 컴파일된 바이너리들[[이 런타임에서 서로 호환되기 위해 반드시 지켜야 하는 저수준 규약

**API**가 소스코드 수준의 약속이라면  

**ABI**는 CPU, 메모리, 레지스터, 스택, 실행 파일, 운영체제 수준의 약속입니다.

ABI는 다음을 정의합니다.

- 데이터 타입 크기
- 구조체 배치
- 정렬 (Alignment)
- 패딩 (Padding)
- 함수 호출 규약
- 시스템 콜 인터페이스

즉, **컴파일된 바이너리가 실제 하드웨어에서 어떻게 동작해야 하는가**를 정의하는 규격입니다.

<details>
<summary>Deep Dive</summary></br>

Compilation(컴파일레이션) [[M]](../../100-deep-dive/micro-foundations/compilation.md)  
Binary Executable File(이진 실행 파일) [[M]](../../100-deep-dive/micro-foundations/binary-executable-file.md)  
Binary Object File(이진 목적 파일) [[M]](../../100-deep-dive/micro-foundations/binary-object-file.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

ABI는 컴파일부터 실행까지 전 과정에 등장합니다.

```
소스 프로그램
↓
컴파일러
↓
ABI 적용
↓
실행 파일 생성
↓
OS Loader 적재
↓
프로세스 실행
```

특히 다음 영역에서 핵심 역할을 합니다.

- 구조체 레이아웃
- 함수 호출
- 라이브러리 호출
- 시스템 콜

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 영향이 큰 자원은 **CPU**와 **Memory**입니다.

| 자원 | ABI가 결정하는 것 |
|------|------------------|
| CPU | 사용할 레지스터, 인자 전달 방식, 반환값 위치 |
| Memory | 구조체 크기, 멤버 위치, 패딩, 정렬 |

> Disk는 실행 파일 포맷에 영향을 받고, Network에는 직접 영향이 거의 없습니다.

---

## 4. 왜 중요한가

ABI가 다르면 소스코드와 API가 동일해도 **바이너리 호환 불가**가 발생합니다.

예를 들어 Library A와 Application B가 서로 다른 ABI로 컴파일되면 구조체 해석 오류, 함수 호출 오류, 메모리 손상이 발생할 수 있습니다.

실무에서는 다음 변경 시 ABI 호환성이 매우 중요합니다.

- CPU 변경
- OS 변경
- 컴파일러 변경
- 라이브러리 변경

---

## 5. 실제 장애와 어떤 관련이 있는가

| 장애 유형 | 원인 | 결과 |
|-----------|------|------|
| 구조체 레이아웃 불일치 | 컴파일 환경 차이로 멤버 오프셋·패딩·전체 크기 상이 | 데이터 왜곡, 메모리 읽기 오류 |
| 함수 호출 규약 충돌 | Caller / Callee가 서로 다른 ABI 사용 | Segmentation Fault, Crash |
| 라이브러리 ABI 변경 | libA v1 → v2로 구조체 정의 변경 | 기존 바이너리 실행 실패 |
| 시스템 콜 ABI 불일치 | 다른 CPU ABI 또는 OS ABI 사용 | 시스템 콜 실패, 프로세스 비정상 종료 |

구조체 레이아웃 불일치 예시:

```c
struct Data {
    char a;
    int b;
};
```

컴파일 환경이 다르면 멤버 오프셋, 패딩 크기, 전체 크기가 달라질 수 있습니다.

---

## 6. 핵심 메커니즘

현재 비트 필드 문맥에서는 다음 관계가 가장 중요합니다.

```
타깃 CPU
+
타깃 OS
↓
ABI
↓
컴파일러
↓
비트 필드 레이아웃
```

아래 선언을 예로 들면:

```c
struct Status {
    unsigned int mode : 3;
    unsigned int flag : 1;
};
```

ABI가 결정하는 것:

- 비트 배치 순서
- 정렬 단위
- 패딩 삽입 여부
- 구조체 크기

특히 다음 세 가지는 ABI의 직접적인 영향을 받습니다.

- Bit-field Physical Layout
- Bit-field Data Alignment
- Bit-field Padding Rule

> **핵심 사실:** 비트 필드 구조체의 실제 메모리 모습은 언어 표준만으로 결정되지 않는다.  
> **ABI가 최종 결정**한다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# CPU ABI 확인
uname -m
# 출력 예: x86_64, aarch64

# ELF 헤더 확인 (Machine, Class, ABI)
readelf -h program

# 바이너리 정보 확인
file program
# 출력 예: ELF 64-bit LSB executable

# 라이브러리 의존성 확인
ldd program
```

구조체 크기 검증은 실무에서 다음으로 수행합니다.

```c
sizeof()
offsetof()
```

### Kubernetes

```bash
# ABI 적합성 확인
kubectl exec -it POD -- uname -m
kubectl exec -it POD -- file APP
```

실무에서 자주 보는 장애:

- ARM 이미지를 x86 노드에 배포 → `Exec format error`
- `glibc` ABI 불일치 → 프로그램 시작 실패

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*