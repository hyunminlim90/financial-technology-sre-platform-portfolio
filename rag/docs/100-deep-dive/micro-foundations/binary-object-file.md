# 이진 목적 파일 (Binary Object File)

> 정독: 0회

## 1. 이 기술이 무엇인가

이진 목적 파일(Binary Object File)은:

> 컴파일은 완료되었지만 아직 실행 가능한 프로그램으로 결합[[되지 않은 중간 산출물

```
소스 코드
↓
컴파일
↓
목적 파일 (.o, .obj)
↓
링킹
↓
실행 파일
```

목적 파일은 이미 CPU가 이해할 수 있는 기계어를 포함하고 있지만, 외부 함수나 다른 모듈과의 연결이 완료되지 않은 상태입니다.

> 즉, **실행 가능 직전 단계의 바이너리 모듈**입니다.

<details>
<summary>Deep Dive</summary></br>

Compilation(컴파일레이션) [[M]](../../100-deep-dive/micro-foundations/compilation.md)  
End of Compile Time Phase(컴파일 타임의 종료) [[M]](../../100-deep-dive/micro-foundations/end-of-compile-time-phase.md)  
Binary Executable File(이진 실행 파일) [[M]](../../100-deep-dive/micro-foundations/binary-executable-file.md)  
Linking(링킹) [[M]](../../100-deep-dive/micro-foundations/linking.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

빌드 파이프라인의 **컴파일 단계와 링킹 단계 사이**에 존재합니다.

```
Source Program
↓
Compiler
↓
Object File
↓
Linker
↓
Executable File
↓
Loader
↓
Process
```

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 직접적인 자원은 **Disk**입니다. 목적 파일은 정적 파일로 저장됩니다.

| 자원 | 역할 |
|------|------|
| Disk | 목적 파일의 정적 저장소 |
| CPU / Memory | 목적 파일 내부의 기계어·데이터가 최종 실행 파일로 합쳐지므로 간접 영향 |
| Network | 직접 관련 없음. 빌드 서버·CI/CD·아티팩트 저장소에서 전송 대상이 될 수 있음 |

---

## 4. 왜 중요한가

대규모 소프트웨어는 수천 개의 소스 파일로 구성됩니다. 매번 전체를 다시 컴파일하면 비용이 매우 크기 때문에, 각 소스 파일을 독립적으로 컴파일하여 목적 파일을 생성합니다.

```
file1.c → file1.o
file2.c → file2.o
file3.c → file3.o
```

이후 링커가 목적 파일들과 라이브러리를 결합합니다.

즉, 목적 파일은 다음의 핵심 단위입니다.

- 분리 컴파일
- 증분 빌드
- 대규모 프로젝트 구성

---

## 5. 실제 장애와 어떤 관련이 있는가

| 장애 유형 | 원인 | 결과 |
|-----------|------|------|
| 심볼 미해결 (Undefined Symbol) | `main.o`에서 `calculate()`를 호출하는데 `calculate.o`가 링크되지 않음 | `Undefined Reference`, Link Error |
| ABI 불일치 | 서로 다른 ABI로 생성된 목적 파일 혼합 | 구조체 크기 불일치, 함수 호출 규약 충돌, 메모리 손상 |
| 아키텍처 불일치 | ARM 목적 파일과 x86 목적 파일 혼합 | 링크 실패, 빌드 실패 |
| 라이브러리 버전 충돌 | 버전이 다른 라이브러리와 결합 | 빌드는 성공하나 실행 시 Crash |

---

## 6. 핵심 메커니즘

목적 파일 생성 시점에 이미 다음이 완료된 상태입니다.

- ISA 적용
- ABI 적용
- 정렬 적용
- 패딩 적용
- 비트 필드 레이아웃 적용

아래 선언을 예로 들면:

```c
struct Status {
    unsigned int mode : 3;
    unsigned int flag : 1;
};
```

컴파일러는 목적 파일 생성 시점에 이미 구조체 크기, 비트 위치, 정렬 규칙, 패딩 규칙을 계산합니다.

즉, 비트 필드 물리 배치, 부호 규칙, 정렬, 패딩은 **목적 파일 단계에서 대부분 확정**됩니다.

다만 아직 결정되지 않은 것:

- 실제 메모리 주소
- 최종 함수 주소
- 최종 라이브러리 위치

이 작업은 링커가 수행합니다.

| 상태 | 내용 |
|------|------|
| 완료 | 기계어 생성 |
| 미완료 | 주소 결합 |

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 목적 파일 확인
file main.o
# 출력 예: ELF 64-bit relocatable
# 'relocatable' = 아직 주소가 확정되지 않음

# 섹션 확인 (.text, .data, .bss, .symtab, .rela.text 등)
readelf -S main.o

# 심볼 확인
nm main.o
# 'T' = 정의된 심볼, 'U' = 아직 정의되지 않은 외부 심볼

# 재배치 정보 확인
readelf -r main.o
```

### Kubernetes

목적 파일은 Kubernetes 런타임에서는 거의 보이지 않습니다. 컨테이너에는 보통 실행 파일과 공유 라이브러리만 포함되기 때문입니다.

목적 파일은 주로 다음 환경에서 관측됩니다.

- 개발 환경
- 빌드 서버
- CI 시스템

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*