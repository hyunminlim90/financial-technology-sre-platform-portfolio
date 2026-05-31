# 부울 연산 (Boolean Operation)

> 정독: 0회

## 1. 이 기술이 무엇인가

부울 연산(Boolean Operation)은:

> 참(True, 1)과 거짓(False, 0)만을 입력으로 받아 논리적 결과를 계산하는 연산

컴퓨터 내부의 모든 디지털 회로는 결국 0과 1만 처리하므로, 부울 연산은 현대 컴퓨팅의 가장 기초적인 연산 체계입니다.

대표 연산은 다음과 같습니다.

| 연산 | 의미 |
|---|---|
| AND | 모두 참일 때만 참 |
| OR | 하나라도 참이면 참 |
| NOT | 참/거짓 반전 |
| XOR | 서로 다를 때만 참 |

---

## 2. 시스템 어디에서 등장하는가

부울 연산은 시스템 전체에 존재합니다.

**CPU**
- ALU, 제어 회로, 상태 플래그 계산

**메모리**
- 주소 계산, 권한 비트 확인, 상태 플래그 저장

**운영체제**
- 프로세스 상태, 파일 권한, 인터럽트 제어

**네트워크**
- TCP Flag, IP Header, NIC Register

**저장장치**
- 파일시스템 상태, 블록 할당 비트맵, RAID 상태 정보

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 자원은 **CPU**입니다. 부울 연산은 CPU 내부의 논리 게이트에서 수행됩니다.

간접적으로는 **Memory** 영향도 큽니다. 대부분의 상태 정보가 0과 1 형태로 저장되기 때문입니다.

- 파일 권한
- 프로세스 상태
- 페이지 권한
- TCP 플래그

---

## 4. 왜 중요한가

부울 연산은 컴퓨터가 "판단"을 가능하게 만드는 기반입니다.

조건 만족 여부, 권한 존재 여부, 패킷 정상 여부, 에러 발생 여부 같은 모든 판단은 최종적으로 True / False 결정으로 환원됩니다.

또한 산술 연산조차 부울 연산 위에 구축됩니다. 덧셈기(Adder), 뺄셈기(Subtractor), 비교기(Comparator) 모두 AND, OR, XOR, NOT 게이트 조합으로 구성됩니다.

```
부울 연산
↓
논리 게이트
↓
산술 회로
↓
CPU
↓
소프트웨어
```

---

## 5. 실제 장애와 어떤 관련이 있는가

### 조건 판단 오류

인증 성공 여부, 권한 체크, 접속 허용 여부의 판단 로직에 오류가 생기면 권한 우회, 인증 실패, 서비스 장애로 이어집니다.

### 플래그 계산 오류

TCP SYN, ACK, FIN 해석 오류 시 연결 실패 및 세션 비정상 종료가 발생할 수 있습니다.

### 하드웨어 상태 오판

에러 비트, 장치 활성 비트, 인터럽트 비트를 오판하면 장치 오동작 및 장애 탐지 실패로 이어집니다.

### 비트 플립 (Bit Flip)

메모리 오류로 `0 → 1` 또는 `1 → 0` 변경이 발생하면 권한 변경, 데이터 손상, 시스템 크래시로 이어질 수 있습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

지금까지 정리한 개념들의 관계를 연결하면 다음과 같습니다.

```
Boolean Algebra
↓
Boolean Operation
↓
Logic Gate
↓
Bitwise Operation
↓
Bit Masking
↓
Bit Test
↓
Bit Manipulation
```

핵심은 **입력 비트 → 논리 규칙 → 출력 비트**입니다.

**AND**

```
1 AND 1 = 1
1 AND 0 = 0
0 AND 1 = 0
0 AND 0 = 0
```

**OR**

```
1 OR 1 = 1
1 OR 0 = 1
0 OR 1 = 1
0 OR 0 = 0
```

**XOR**

```
1 XOR 1 = 0
1 XOR 0 = 1
0 XOR 1 = 1
0 XOR 0 = 0
```

CPU는 이런 규칙을 수십억 개의 트랜지스터 조합으로 구현합니다.

### Boolean Operation vs Bitwise Operation

| 개념 | 설명 |
|---|---|
| **Boolean Operation** | 논리 규칙 자체 (이론) |
| **Bitwise Operation** | 부울 연산을 여러 비트에 적용한 하드웨어 구현 형태 |

즉, Boolean Operation이 이론이고, Bitwise Operation은 그 이론의 하드웨어 적용 형태입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

직접 "부울 연산" 자체는 보이지 않습니다. 대신 결과가 보입니다.

### Linux 권한 / 프로세스 상태

```bash
chmod          # 권한 비트 계산
ls -l          # 권한 비트 확인
ps             # 프로세스 상태 플래그 해석
top
```

### 네트워크

```bash
tcpdump
wireshark      # TCP 플래그 확인 (SYN, ACK, FIN, RST)
```

### CPU

```bash
lscpu          # CPU 기능 플래그 확인 (sse, avx, aes, vmx)
```

### Kubernetes

Node Condition, Pod Condition, Taint, Toleration, Feature Gate 등이 내부적으로 다수의 Boolean 상태를 기반으로 동작합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*