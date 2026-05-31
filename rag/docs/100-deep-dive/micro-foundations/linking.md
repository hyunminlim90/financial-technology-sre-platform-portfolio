# 링킹 (Linking)

> 정독: 0회

## 1. 이 기술이 무엇인가

링킹(Linking)은:
 
> 컴파일이 끝나 생성[[된 여러 목적 파일(Object File)과 라이브러리를 결합하여 최종 실행 파일(Executable File)을 만드는 과정

컴파일러는 소스 파일을 각각 번역할 뿐이고, 여러 파일을 하나의 프로그램으로 묶는 것은 **링커(Linker)** 의 역할입니다.

| 단계 | 역할 | 결과물 |
|------|------|--------|
| 컴파일 | 소스 파일 번역 | 목적 파일 |
| 링킹 | 목적 파일 결합 | 실행 파일 |

<details>
<summary>Deep Dive</summary></br>

Compilation(컴파일레이션) [[M]](../../100-deep-dive/micro-foundations/compilation.md)  
End of Compile-time Phase(컴파일 타임의 종료) [[M]](../../100-deep-dive/micro-foundations/end-of-compile-time-phase.md)  
Code Generation(코드 생성) [[M]](../../100-deep-dive/micro-foundations/code-generation.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

빌드 파이프라인의 **컴파일 종료 → 실행 파일 생성** 사이를 연결하는 단계입니다.

```
Source Code
↓
Compilation
↓
Object Files (.o / .obj)
↓
★ Linking
↓
Executable File
↓
Loading
↓
Runtime
```

---

## 3. 어떤 자원에 가장 영향이 큰가

주요 자원은 **CPU**, **Memory**, **Disk**입니다.

| 자원 | 역할 |
|------|------|
| CPU | 심볼 검색, 주소 계산, 재배치 연산 수행 |
| Memory | 목적 파일과 라이브러리를 로드하여 분석 |
| Disk | `a.out`, `main.exe`, `app` 등 실행 파일 생성 |

> Network 영향은 거의 없습니다.

---

## 4. 왜 중요한가

컴파일만 끝나서는 프로그램을 실행할 수 없습니다.

```c
int main() {
    printf("hello");
}
```

컴파일 단계에서는 `printf`가 존재한다는 것만 알고, 실제 주소와 구현은 모릅니다. 링커가 `printf` 호출부와 실제 구현을 연결해야 실행 가능합니다.

따라서 링킹이 실패하면 **컴파일 성공 / 실행 파일 생성 실패**가 발생할 수 있습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

| 장애 유형 | 원인 | 결과 |
|-----------|------|------|
| Undefined Reference | 함수를 호출했는데 구현을 찾지 못함 | `undefined reference to foo` |
| Duplicate Symbol | 동일 심볼이 여러 곳에 존재 | `multiple definition of foo` |
| ABI 불일치 | 서로 다른 ABI·컴파일 옵션으로 빌드 | Crash, Segmentation Fault, 메모리 오염 |
| 라이브러리 누락 | 실행 파일 생성은 성공했으나 필요한 공유 라이브러리 없음 | 실행 실패 |

---

## 6. 핵심 메커니즘

링킹의 핵심은 두 가지입니다.

### ① Symbol Resolution (심볼 연결)

```
main.o → foo() 호출
util.o → foo() 구현
```

링커가 `foo` 호출부와 `foo` 구현을 연결합니다.

### ② Relocation (주소 재배치)

컴파일 단계에서 각 목적 파일은 자신이 0번지에서 시작한다고 가정하고 생성됩니다.

```
main.o → 0x1000
util.o → 0x3000
```

링커는 최종 위치를 정하고 기계어 내부 주소를 수정합니다.

> **핵심 사실:** 컴파일은 번역이고, **링킹은 연결**이다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 목적 파일 확인
ls *.o

# 심볼 확인
nm main.o

# 실행 파일 생성
gcc main.o util.o -o app

# 라이브러리 의존성 확인
ldd app
# 출력 예: libc.so, libpthread.so, libm.so

# ELF 정보 확인
readelf -a app

# 런타임 로더 확인 (공유 라이브러리 매핑 확인)
cat /proc/<pid>/maps
```

### Kubernetes

주로 Container Image 빌드 및 CI/CD 파이프라인에서 간접적으로 등장합니다. Pod가 실행될 때는 이미 링킹이 끝난 실행 파일이 사용됩니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*