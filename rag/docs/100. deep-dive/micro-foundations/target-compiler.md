# 타깃 컴파일러 (Target Compiler)

> 정독: 0회

## 1. 이 기술이 무엇인가

타깃 컴파일러(Target Compiler)는:

> 소스 코드를 최종 실행 환경(Target Environment)의 CPU, 운영체제, ABI[[ 규격에 맞는 실행 코드로 변환하는 컴파일러

```
소스코드
↓
타깃 컴파일러
↓
타깃 환경 전용 실행 코드
```

컴파일러는 단순히 문법을 번역하는 도구가 아니라, 아래 요소들을 코드에 반영하는 **시스템 규격 집행기**입니다.

- CPU 구조
- 메모리 모델
- ABI
- 정렬 규칙
- 명령어 집합 (ISA)

<details>
<summary>Deep Dive</summary></br>

Source Program(소스 프로그램) [[M]](../../100-deep-dive/micro-foundations/source-program.md)  
Target Platform(타깃 플랫폼) [[M]](../../100-deep-dive/micro-foundations/target-platform.md)  
Target Processor(타깃 프로세서) [[M]](../../100-deep-dive/micro-foundations/target-processor.md)  
Target Operating System(타깃 운영체제) [[M]](../../100-deep-dive/micro-foundations/target-operating-system.md)  
Application Binary Interface(애플리케이션 이진 인터페이스) [[M]](../../100-deep-dive/micro-foundations/application-binary-interface.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

타깃 컴파일러는 프로그램이 생성되는 모든 환경에서 등장합니다.

| 분야 | 예시 |
|------|------|
| 운영체제 개발 | Linux Kernel, RTOS, Device Driver |
| 임베디드 시스템 | ARM MCU, 자동차 ECU, IoT 장치 |
| 서버 프로그램 | x86-64 서버, ARM 서버 |
| 네트워크 장비 | Router, Switch, Firewall |
| 하드웨어 제어 소프트웨어 | NIC Driver, Storage Driver, GPU Driver |

> **요약:** 실행 코드가 생성되는 모든 곳에 존재합니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 큰 영향을 받는 자원은 **CPU**와 **Memory**입니다.

### CPU

타깃 ISA에 맞는 명령어를 생성합니다.

- x86-64 Instruction
- ARM64 Instruction
- RISC-V Instruction

### Memory

타깃 ABI에 따라 아래 항목이 달라집니다.

- 구조체 크기
- 정렬 (Alignment)
- 패딩 (Padding)
- 비트 필드 배치

> Network와 Disk는 직접적인 대상이 아니라, CPU와 Memory를 통해 간접적으로 영향을 받습니다.

---

## 4. 왜 중요한가

같은 소스 코드라도 **타깃 CPU**, **타깃 ABI**, **타깃 OS**가 달라지면 결과물이 달라질 수 있습니다.

```c
struct Status {
    unsigned int a : 3;
    unsigned int b : 5;
};
```

컴파일러는 타깃 환경에 따라 다음을 결정합니다.

- 비트 필드 순서
- 패딩 위치
- 정렬 방식

따라서 **실행 결과**, **메모리 구조**, **바이너리 형식** 모두 타깃 컴파일러에 의해 결정됩니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### ABI 불일치

가장 흔한 문제입니다. A 시스템에서 컴파일한 코드를 B 시스템에서 해석하면 다음이 달라질 수 있습니다.

- 구조체 크기
- 패딩
- 비트 필드 위치

### 프로토콜 오류

네트워크 패킷 헤더를 x86 기준으로 작성하고 ARM 기준으로 해석하면 필드 위치가 달라질 수 있습니다.

### 하드웨어 제어 오류

레지스터 매핑 코드가 타깃 칩 ABI를 고려하지 않으면 다음이 발생할 수 있습니다.

- 잘못된 비트 기록
- 잘못된 장치 제어

### 성능 문제

타깃 CPU 특성을 고려하지 않은 코드 생성 시 다음이 발생합니다.

- 불필요한 메모리 접근
- 추가 정렬 비용
- 캐시 비효율

---

## 6. 핵심 메커니즘

타깃 컴파일러는 크게 4가지를 결정합니다.

### ① 명령어 생성

`ADD`, `SUB`, `MOV`, `LOAD`, `STORE` 등을 타깃 ISA에 맞게 생성합니다.

### ② 데이터 레이아웃

구조체 크기와 멤버 위치를 결정합니다.

### ③ 정렬 규칙

Alignment와 Padding을 결정합니다.

### ④ 비트 필드 규칙

Bit Field Layout, Bit Field Padding, Bit Field Alignment를 결정합니다.

> **핵심 사실:**
> 비트 필드 구조체의 실제 메모리 모습은 소스 코드가 아니라, **타깃 컴파일러가 최종 결정**한다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 컴파일러 확인
gcc -v
clang -v

# 타깃 정보 확인
gcc -dumpmachine
# 출력 예: x86_64-linux-gnu, aarch64-linux-gnu

# ELF 바이너리 확인
file binary
readelf -h binary
```

확인 가능 항목: CPU Architecture, ABI, Endianness

### Kubernetes

직접 보이지는 않지만, `amd64` 이미지와 `arm64` 이미지를 잘못 배포하면 다음 오류가 발생합니다.

```
Exec format error
```

### Runtime

프로세스 자체는 **타깃 컴파일러가 만든 결과물**을 실행하는 단계입니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*