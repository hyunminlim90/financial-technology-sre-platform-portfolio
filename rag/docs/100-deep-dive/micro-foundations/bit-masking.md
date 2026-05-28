# 비트 마스킹 (Bit Masking)

> 정독: 0회

## 1. 이 기술이 무엇인가

**비트 마스킹(Bit Masking)** 은:

> 원본 비트 데이터에서 특정 비트만 선택적으로 유지하거나 제거하는 비트 필터링 기법

**핵심:**

원하는 비트만 남기고 나머지는 제거하는 것

**대표 연산:**

| 연산 | 목적 |
|------|------|
| AND (`&`) | 특정 비트 추출 |
| OR (`\|`) | 특정 비트 설정 |
| XOR (`^`) | 특정 비트 반전 |
| NOT (`~`) | 비트 반전 |

**예시:**

```
value = 0b10110100
mask  = 0b00000100

value & mask = 0b00000100
```

특정 위치 비트만 유지하고 나머지는 제거됩니다.

---

## 2. 시스템 어디에서 등장하는가

비트 마스킹은 거의 모든 시스템 계층에서 사용됩니다.

| 영역 | 사례 |
|------|------|
| OS Kernel | page flag |
| Runtime | GC metadata |
| CPU | status register |
| Network | TCP flags |
| Filesystem | permission bits |
| Hypervisor | VM state |
| Device Driver | MMIO register |
| Scheduler | task state |
| Security | capability bits |
| Locking | mutex state |

dirty, locked, marked, readable, writable, interrupt enabled 등의 플래그 관리에 사용됩니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 영향이 큰 자원은 **CPU** 입니다. ALU bitwise execution, register operation, branch condition, flag evaluation과 직접 연결됩니다.

두 번째는 **Memory** 입니다. metadata 압축, cache efficiency 향상, memory footprint 감소 효과 때문입니다.

---

## 4. 왜 중요한가

비트 마스킹은:

> 최소 비용으로 상태를 저장하고 판별하게 해줍니다.

64bit 하나 안에 lock, dirty, gc mark, version, permission 등을 동시에 저장할 수 있습니다.

**장점:** 메모리 절약, cache locality 향상, branch cost 감소, pointer access 감소

대규모 시스템에서는 **수십억 개 객체 metadata** 를 관리하므로 매우 중요합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### Wrong Mask
mask 설계 오류 시 잘못된 상태 추출, invalid branch, corrupted metadata가 발생할 수 있습니다.

### Race Condition
멀티스레드에서 동시 비트 수정 시 lost update, inconsistent flag, synchronization failure가 발생할 수 있습니다.

### Permission Bug
permission bit masking 오류 시 unauthorized access, privilege escalation이 발생할 수 있습니다.

### Device Register Corruption
driver에서 register masking 오류 시 device malfunction, interrupt issue, kernel panic이 발생할 수 있습니다.

### Atomicity 문제
bit masking이 atomic하지 않으면 lock corruption, stale state, invalid transition이 발생할 수 있습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

**핵심:**
> 특정 비트만 선택적으로 유지하거나 수정하는 것

### 핵심 흐름

```
1) Operand 준비
   └─ 원본 데이터: value = 0b10110100

2) Mask 준비
   └─ 특정 위치만 1: mask = 0b00000100

3) AND 마스킹 (비트 추출)
   └─ result = value & mask
      → target bit만 유지, 나머지는 모두 0

4) OR 마스킹 (비트 설정)
   └─ value |= mask
      → 특정 비트를 1로 설정

5) AND + NOT 마스킹 (비트 제거)
   └─ value &= ~mask
      → 특정 비트를 0으로 제거
```

### 핵심 구조

```
Operand
  ↓
Bitmask 적용
  ↓
Bitwise Operation
  ↓
특정 Bit만 유지/수정
  ↓
State 결정
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux Permission

```bash
chmod
ls -l
# 내부적으로 permission bit masking 사용
```

### TCP/IP Stack

TCP Header의 SYN, ACK, FIN, RST 모두 masking 기반입니다.

```bash
tcpdump
wireshark
```

### CPU Status Register

대표 플래그: Zero Flag, Carry Flag, Overflow Flag

```bash
gdb
# info registers
```

### Runtime Metadata

GC 상태인 marked, forwarded, locked 등을 bit masking으로 처리합니다.

### Device Driver

MMIO register bit manipulation이 매우 많습니다.

### perf / eBPF

```bash
perf stat
perf record
# bitwise hotspot 추적 가능
```

### Kubernetes 환경

K8s 상위 레벨보다 kernel, cgroup, network stack, container runtime 내부에서 광범위하게 사용됩니다.