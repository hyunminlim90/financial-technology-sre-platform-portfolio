# 비트 필드 이미지 데이터 저장 (Bit-field Image Data Storage)

> 정독: 0회

## 1. 이 기술이 무엇인가

비트 필드 이미지 데이터 저장(Bit-field Image Data Storage)은:

> 비트 필드 멤버 값이 인코딩되어 생성된 최종 비트 패턴을 실제 CPU 레지스터나 메모리에 기록하는 과정

비트 필드 관점에서는 단순한 "값 저장"이 아닙니다. 이미 존재하는 워드 내부의 일부 비트만 변경해야 하므로 저장 과정에서 추가적인 비트 조작이 필요합니다.

```
Bit Field Member Value
→ Encoding
→ Bit Pattern 생성
→ Word 갱신
→ Memory / Register 저장
```

---

## 2. 시스템 어디에서 등장하는가

주로 다음 영역에서 등장합니다.

**CPU 제어 레지스터**
- Control Register, Status Register, Flag Register

**운영체제 커널**
- Page Table, Process State, Permission Flag

**장치 드라이버**
- NIC Register, Storage Controller Register, GPU Register

**네트워크 프로토콜**
- IPv4 Header, TCP Header, VLAN Header

**임베디드 시스템**
- GPIO Register, Interrupt Controller, Timer Register

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **CPU**와 **Memory**입니다. 비트 필드 저장은 대부분 Read → Modify → Write 과정을 수행하므로 CPU 연산과 Memory 접근이 동시에 발생합니다.

Network나 Disk는 직접적인 대상은 아닙니다. 다만 네트워크 패킷 헤더나 저장 장치 제어 레지스터를 구성할 때 간접적으로 사용됩니다.

---

## 4. 왜 중요한가

비트 필드는 하나의 워드를 여러 데이터가 공유합니다.

```
Word
┌──────────┬──────────┬──────────┬──────────┐
│   Flag   │   Mode   │ Priority │ Reserved │
└──────────┴──────────┴──────────┴──────────┘
```

여기서 `Mode`만 변경하고 싶어도 `Flag`, `Priority`, `Reserved`는 유지해야 합니다. 단순 저장을 수행하면 **기존 데이터 파괴**가 발생할 수 있습니다.

그래서 비트 필드 저장은 **선택적 갱신**이라는 특징을 가집니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 동시성 문제 (Lost Update)

가장 유명한 문제입니다.

```
CPU 1  →  Flag 변경  (Read-Modify-Write)
CPU 2  →  Mode 변경  (Read-Modify-Write)
```

동일 워드에 대해 동시에 Read-Modify-Write가 수행되면 **Lost Update**가 발생할 수 있습니다.

### 레지스터 오염

잘못된 마스크 사용 시 목표 비트 외 영역까지 덮어쓸 수 있습니다. 결과: 장치 오동작, 시스템 불안정

### ABI 불일치

레이아웃 해석이 다르면 저장 위치가 달라져 잘못된 필드 갱신이 발생할 수 있습니다.

### 경쟁 상태 (Race Condition)

멀티코어 환경에서 Atomic 보장 없이 비트 필드를 수정하면 데이터 손상이 발생할 수 있습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

비트 필드 저장의 핵심은 **Read-Modify-Write**입니다.

예를 들어 `Word = 10110011`에서 3~4번 비트만 변경한다고 가정합니다.

**① Read**

```
Memory → Register

10110011
```

**② Modify**

```
기존 영역 제거  →  Mask 적용
새 값 삽입      →  Shift + OR

결과: 10101011
```

**③ Write**

```
Register → Memory
```

CPU는 비트 하나만 저장하지 않습니다. 대부분의 경우 **워드 전체**를 다시 기록합니다. 이것이 비트 필드 저장의 핵심입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

직접적으로 보이는 경우는 적지만 다음 영역에서 확인할 수 있습니다.

### Linux Kernel

커널 내부 `flags`, `state`, `permission` 관련 구조체

### Device Driver

```bash
ethtool
lspci -vv      # 장치 레지스터 갱신 확인
```

### 디버거 / 메모리 덤프

```bash
gdb
hexdump
xxd
```

### 성능 분석

비트 필드 저장이 많은 경우 `perf`에서 read-modify-write, memory access, cache activity 등으로 간접 확인 가능합니다.

```bash
perf
```

### Kubernetes

Kubernetes 레벨보다는 Kernel, Driver, Runtime, Hardware Interface 영역에서 훨씬 자주 등장합니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*