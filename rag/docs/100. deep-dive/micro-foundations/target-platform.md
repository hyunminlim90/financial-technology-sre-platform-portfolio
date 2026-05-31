# 타깃 플랫폼 (Target Platform)

> 정독: 0회

## 1. 이 기술이 무엇인가

타깃 플랫폼(Target Platform)은:

> 컴파일된 프로그램이 실제로 실행되는 최종 환경

단순히 CPU만 의미하는 것이 아니라 다음 요소들의 조합입니다.

```
CPU 아키텍처 (ISA)
+
운영체제 (OS)
+
ABI
+
런타임 환경
```

다음은 서로 다른 타깃 플랫폼의 예시입니다.

| 플랫폼 | 설명 |
|--------|------|
| x86-64 + Linux | 일반 서버 환경 |
| ARM64 + Linux | ARM 기반 서버 / 임베디드 |
| x86-64 + Windows | Windows 서버 환경 |
| ARM64 + macOS | Apple Silicon 환경 |
| RISC-V + RTOS | 경량 실시간 시스템 |
| ARM Cortex-M + Bare Metal | MCU 환경 |

> 동일한 소스 프로그램이라도 타깃 플랫폼이 바뀌면 생성되는 실행 파일도 달라질 수 있습니다.

<details>
<summary>Deep Dive</summary></br>

Compilation Process(컴파일레이션 프로세스) [[M]](../../100-deep-dive/micro-foundations/compilation-process.md)  
Executable File(실행 파일) [[M]](../../100-deep-dive/micro-foundations/executable-file.md)  
Runtime Execution(런타임 실행) [[M]](../../100-deep-dive/micro-foundations/runtime-execution.md)  
Process Runtime Environment(프로세스 런타임 환경) [[M]](../../100-deep-dive/micro-foundations/process-runtime-environment.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

타깃 플랫폼은 컴파일과 실행의 기준점입니다.

```
소스 프로그램
↓
타깃 컴파일러
↓
타깃 플랫폼용 실행 코드 생성
↓
타깃 플랫폼에 배포
↓
실행
```

타깃 플랫폼은 전 과정에 영향을 줍니다.

- 컴파일 단계
- 링크 단계
- 로딩 단계
- 실행 단계

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 큰 영향을 받는 자원은 **CPU**와 **Memory**입니다.

### CPU

타깃 CPU가 결정하는 것:

- 명령어 집합 (ISA)
- 레지스터 크기 및 개수
- 엔디언
- 원자 연산 지원 여부

### Memory

타깃 플랫폼이 결정하는 것:

- 포인터 크기
- 정렬 규칙
- 패딩 규칙
- 페이지 크기
- 주소 공간 크기

> Network와 Disk는 상대적으로 상위 계층의 영향을 많이 받습니다.

---

## 4. 왜 중요한가

소스 프로그램의 실제 물리적 형태를 결정하기 때문입니다. 동일한 코드라도 다음이 달라질 수 있습니다.

- 자료형 크기
- 메모리 레이아웃
- 정렬 / 패딩
- 호출 규약

예를 들어 32비트 플랫폼과 64비트 플랫폼에서는 포인터 크기, 구조체 크기, 메모리 사용량이 달라질 수 있습니다.

비트 필드 문맥에서는 특히 중요합니다. 비트 필드 배치 순서, 패딩 삽입 방식, 정렬 규칙이 타깃 플랫폼의 ABI 영향을 받기 때문입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 구조체 크기 불일치

한 플랫폼에서 `sizeof(struct) = 8`인데, 다른 플랫폼에서는 `sizeof(struct) = 12`가 될 수 있습니다.

결과: 데이터 손상, 직렬화 오류, 프로토콜 오류

### 엔디언 문제

플랫폼마다 Little Endian / Big Endian이 다를 수 있습니다.

결과: 잘못된 값 해석, 패킷 오류, 파일 포맷 오류

### ABI 불일치

라이브러리와 실행 파일이 다른 ABI 기준으로 빌드되면 함수 호출 실패, 메모리 손상, 크래시가 발생할 수 있습니다.

### 비트 필드 오류

타깃 플랫폼마다 비트 필드 배치 방식과 패딩 방식이 다를 수 있습니다.

결과: 레지스터 제어 실패, 하드웨어 오동작

---

## 6. 핵심 메커니즘

> **핵심 사실:** 비트 필드의 물리 배치는 소스 코드가 아니라 **타깃 플랫폼이 결정**한다.

아래 선언은 비트 위치, 패딩 삽입 여부를 직접 지정하지 않습니다.

```c
struct Config {
    unsigned int mode : 3;
    unsigned int flag : 1;
};
```

최종 결정 과정:

```
소스 프로그램
↓
타깃 컴파일러
↓
타깃 플랫폼 ABI 확인
↓
정렬 규칙 결정
↓
패딩 규칙 결정
↓
비트 필드 배치 결정
↓
실제 메모리 이미지 생성
```

정리하면:

| 구분 | 의미 |
|------|------|
| 소스 프로그램 | 논리 명세 |
| 타깃 플랫폼 | 물리 규격 |

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# CPU 확인
uname -m
lscpu
# 출력 예: x86_64, aarch64, riscv64

# ABI 확인
file executable
# 출력 예: ELF 64-bit LSB executable

# 엔디언 확인
lscpu | grep Endian
```

### Runtime

실행 중인 프로세스는 타깃 플랫폼 규칙을 그대로 따릅니다.

관측 대상: 메모리 레이아웃, 포인터 크기, 시스템 콜, 스레드 모델

### Kubernetes

노드 아키텍처(`amd64` / `arm64`)에 따라 컨테이너 이미지도 달라져야 합니다.

```bash
# 노드 아키텍처 확인
kubectl get nodes -o wide
kubectl describe node
```

멀티 아키텍처 이미지를 사용하지 않으면 다음이 발생할 수 있습니다.

- ImagePull 성공
- Container Start 실패
- `Exec format error`

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*