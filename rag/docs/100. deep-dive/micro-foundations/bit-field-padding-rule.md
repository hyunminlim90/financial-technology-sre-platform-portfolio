# 비트 필드 패딩 규칙 (Bit-field Padding Rule)

> 정독: 0회

## 1. 이 기술이 무엇인가

비트 필드 패딩 규칙(Bit-field Padding Rule)은:

> 컴파일러[[가 비트 필드 구조체를 메모리에 배치할 때, CPU 아키텍처의 정렬(Alignment) 규칙과 ABI 요구사항을 만족시키기 위해 사용하지 않는 비트 또는 바이트 공간을 자동으로 삽입하는 규칙

패딩(Padding)은 실제 데이터가 아닙니다. 접근 불가, 이름 없음, 의미 없음인 공간입니다.

주요 목적은 다음 두 가지입니다.

- 정렬 규칙 준수
- 메모리 접근 성능 유지

<details>
<summary>Deep Dive</summary></br>

Target Compiler(타깃 컴파일러) [[M]](../../100-deep-dive/micro-foundations/target-compiler.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

패딩은 거의 모든 저수준 데이터 구조에서 등장합니다.

**구조체(Structure)**

```c
struct Header {
    unsigned int a : 3;
    unsigned int b : 5;
};
```

**하드웨어 레지스터**
- NIC, GPU, Storage Controller, UART, SPI, I2C

**네트워크 프로토콜**
- IPv4 Header, TCP Header, Protocol Flags

**운영체제 커널**
- Task Descriptor, Memory Descriptor, Device State

비트 필드 + 구조체 + 하드웨어 친화적 레이아웃이 사용되는 곳이라면 거의 항상 등장합니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Memory**와 **CPU**입니다.

| 자원 | 영향 |
|---|---|
| **Memory** | 패딩은 데이터를 저장하지 않지만 공간을 차지 (예: 실제 36비트 → 실제 크기 64비트) |
| **CPU** | 패딩이 있으면 정렬된 접근 가능. 정렬이 깨지면 추가 Load / Store / Shift / Merge 발생 |

---

## 4. 왜 중요한가

패딩은 공간 낭비처럼 보일 수 있지만 실제 목적은 **성능과 호환성**입니다.

CPU는 일반적으로 2바이트, 4바이트, 8바이트, 16바이트 경계를 기준으로 데이터를 읽습니다. 컴파일러가 패딩을 삽입하지 않으면 Misaligned Access가 발생할 수 있습니다.

결과적으로 CPU 접근 비용 증가, 캐시 효율 저하, ABI 불일치 위험이 생깁니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### ABI 불일치

동일한 구조체라도 컴파일러, CPU 아키텍처, ABI가 다르면 패딩 위치가 달라질 수 있습니다.

결과: 데이터 해석 오류, 필드 값 왜곡, 프로토콜 파싱 실패

### 직렬화 오류

구조체 전체를 그대로 전송하는 경우 패딩이 포함된 상태로 전송될 수 있습니다. 결과적으로 송신 측과 수신 측의 구조체 레이아웃 불일치가 발생합니다.

### 성능 문제

패딩을 제거하려고 무리하게 Packed 구조를 사용하면 Unaligned Access가 증가할 수 있습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

패딩은 크게 세 가지 상황에서 생성됩니다.

**① 워드 내부 패딩** — 현재 워드의 남은 공간이 부족한 경우

```
현재 워드 마감
↓
패딩 채움
↓
다음 워드 시작
```

예: 남은 공간 4비트 + 새 필드 8비트 → 4비트 패딩 + 다음 워드 이동

**② Tail Padding** — 구조체 끝에서 발생

```
총 사용량 : 18비트
정렬 단위 : 32비트
→ 14비트 패딩 추가
```

**③ 강제 패딩** — 일부 언어 및 ABI에서 명시적으로 현재 워드 종료 및 다음 경계 이동을 강제

```c
int : 0;   // 현재 워드 종료, 다음 경계 이동
```

핵심은 다음입니다.

> **패딩은 데이터가 아니다. 정렬 규칙을 만족시키기 위해 컴파일러가 삽입하는 공간이다.**

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### 컴파일러 / 코드

```c
sizeof()     // 구조체 크기 확인
offsetof()   // 멤버 위치 확인
```

### 디버거

```bash
gdb
lldb         # 메모리 덤프 / 구조체 레이아웃 확인 가능
```

### Linux Kernel

Device Driver, Network Stack, Scheduler, Memory Manager에서 자주 등장합니다.

### Kubernetes

직접 관측되지는 않습니다. 주로 Compiler, Kernel, CPU 계층의 문제입니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*