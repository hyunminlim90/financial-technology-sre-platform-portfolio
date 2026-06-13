# 타겟 비트 (Target Bit)

> 정독: 0회

## 1. 이 기술이 무엇인가

**타겟 비트(Target Bit)** 는:

> 비트 연산에서 실제로 읽거나 수정하려는 특정 비트 위치

**예시:**

```
flags = 0b10110100
```

여기서 아래 연산을 수행하면:

```
flags & 0b00000100
```

`0b00000100` 위치의 비트만 검사하게 되며, 이 비트가 **타겟 비트**입니다.

> 비트마스크 연산의 실제 목적 대상 비트가 타겟 비트입니다.

---

## 2. 시스템 어디에서 등장하는가

타겟 비트는 거의 모든 시스템 소프트웨어에서 등장합니다.

| 영역 | 사용 예 |
|------|---------|
| OS Kernel | page flags |
| Runtime | GC mark bit |
| Locking | mutex state |
| CPU | status flags |
| Filesystem | permission bits |
| Networking | TCP flags |
| Scheduler | task state |
| Security | access flags |
| Hypervisor | VM state |
| Device Driver | hardware register bit |

readable, writable, locked, dirty, marked, interrupt enabled 같은 상태들이 대부분 타겟 비트로 관리됩니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 영향이 큰 자원은 **CPU + Memory** 입니다.

ALU bitwise execution, register operation, cache access, memory metadata handling과 강하게 연결됩니다.

타겟 비트를 사용하면 boolean 여러 개를 1bit 단위로 압축할 수 있어 **메모리 효율성** 측면에서도 중요합니다.

---

## 4. 왜 중요한가

타겟 비트는:

> 시스템 상태를 최소 비용으로 표현하기 위해 매우 중요

합니다.

| 상태 | 1bit 사용 가능 |
|------|---------------|
| lock | yes |
| dirty | yes |
| active | yes |
| deleted | yes |
| readable | yes |

각 상태를 `4byte int`로 저장하면 메모리 낭비, cache pressure 증가, bandwidth 증가가 발생합니다.

대규모 시스템에서는 **수십억 개 객체 × 상태 플래그** 가 존재하므로 매우 중요합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 잘못된 Target Bit 조작
잘못된 비트 수정 시 lock corruption, invalid state, memory corruption이 발생할 수 있습니다.

### Race Condition
멀티스레드에서 동일 target bit을 동시에 수정하면 lost update, synchronization bug가 발생할 수 있습니다.

### Wrong Bit Offset
타겟 비트 위치 계산 오류 시 다른 상태 플래그가 파괴되어 시스템이 오동작할 수 있습니다.

### Atomicity 문제
비트 수정이 atomic하지 않으면 inconsistent metadata, corrupted runtime state가 발생할 수 있습니다.

### Security 문제
permission bit을 잘못 처리하면 privilege escalation, access bypass가 발생할 수 있습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

**핵심:**
> 원본 비트 데이터에서 특정 위치만 선택적으로 제어하는 것

### 핵심 흐름

```
1) Operand Load
   └─ 원본 정수 데이터를 읽음
      예: flags = 0b10110100

2) Bitmask 준비
   └─ 타겟 비트 위치만 1로 설정
      예: mask = 0b00000100

3) 타겟 비트 읽기 (AND)
   └─ flags & mask
      결과가 0b00000100이면 target bit = 1

4) 타겟 비트 켜기 (OR)
   └─ flags |= mask

5) 타겟 비트 끄기 (AND + NOT)
   └─ flags &= ~mask
```

### 핵심 구조

```
Operand
  ↓
Bitmask
  ↓
Target Bit 추출/수정
  ↓
State 결정
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux Kernel 권한 비트

```bash
chmod
# 내부적으로 target bit 조작 사용
```

### TCP Flags 확인

SYN, ACK, FIN, RST 모두 target bit 기반입니다.

```bash
tcpdump
wireshark
```

### CPU Status Register

대표 예: Zero Flag, Carry Flag, Overflow Flag

```bash
gdb
# info registers
```

### Runtime GC

GC mark state(marked / unmarked)를 비트 단위로 관리합니다.

### perf 분석

```bash
perf stat
# atomic/bitwise hotspot 관측 가능
```

### eBPF

커널 내부 bit flag 처리가 매우 많습니다.

### Kubernetes 환경

K8s 자체보다 container runtime, kernel, cgroup, networking stack 내부에서 많이 사용됩니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*