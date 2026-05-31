# 비트별 논리 연산 (Bitwise Operation)

> 정독: 0회

## 1. 이 기술이 무엇인가

**비트별 논리 연산(Bitwise Operation)** 은:

> 정수 데이터를 숫자 전체로 처리하지 않고, 각 비트 위치를 독립적으로 연산하는 CPU 수준의 논리 연산

### 대표 연산

| 연산 | 의미 |
|------|------|
| AND `&` | 둘 다 1이면 1 |
| OR `\|` | 하나라도 1이면 1 |
| XOR `^` | 서로 다르면 1 |
| NOT `~` | 비트 반전 |
| SHIFT `<< >>` | 비트 이동 |

### 예시

```
  10110010
& 00000100
──────────
  00000000
```

### 핵심 특징

- 각 비트가 서로 독립적으로 처리됨
- Carry(자리올림) 개념이 없음

<details>
<summary>Deep Dive</summary></br>

Operand(피연산자) [[M]](../../100-deep-dive/micro-foundations/operand.md)  
Arithmetic Operation(산술 연산) [[M]](../../100-deep-dive/micro-foundations/arithmetic-operation.md)  
Bit Index(비트 인덱스) [[M]](../../100-deep-dive/micro-foundations/bit-index.md)  
Parallel Logical Operation(병렬 논리 연산) [[M]](../../100-deep-dive/micro-foundations/parallel-logical-operation.md)  
Bit Field(비트 필드) [[M]](../../100-deep-dive/micro-foundations/bit-field.md)  
Boolean Operation(부울 연산) [[M]](../../100-deep-dive/micro-foundations/boolean-operation.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

비트별 연산은 시스템 거의 모든 저수준 계층에 등장합니다.

### 대표 영역

| 영역 | 사용 사례 |
|------|-----------|
| CPU | status flag |
| Kernel | process state |
| Runtime | GC metadata |
| Filesystem | permission |
| Network | TCP/IP flags |
| Hypervisor | VM state |
| Driver | device register |
| Cryptography | hash/crypto |
| Compression | bit packing |
| Database | bitmap index |

### 대표 상태 플래그

- `readable`
- `writable`
- `locked`
- `dirty`
- `marked`
- `interrupt-enabled`

---

## 3. 어떤 자원에 가장 영향이 큰가

### 1순위: CPU

ALU, register, branch unit, instruction pipeline 과 직접 연결됩니다.

### 2순위: Memory

compact metadata, bitmap, state packing, cache locality 와 연결됩니다.

---

## 4. 왜 중요한가

비트별 연산은 **아주 작은 비용으로 상태를 매우 빠르게 처리**하게 해줍니다.

### 예시

64bit 정수 하나에 **64개의 독립 상태 저장** 가능.

### 장점

- 메모리 절약
- 매우 빠른 연산
- cache 효율 증가
- branch 최적화
- low-level atomic control 가능

> 커널 / 런타임 / 네트워크 / 스토리지에서 핵심 기반 기술입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 잘못된 비트 연산

mask 오류 시 발생 가능:

- wrong state
- corrupted metadata
- invalid permission

### Atomicity 문제

멀티스레드 환경에서 `read-modify-write` 경쟁 발생 시:

- lost update
- race condition

### Lock 상태 손상

lock bit 잘못 수정 시:

- deadlock
- double unlock
- spin corruption

### Device Register 오염

driver/kernel 수준에서:

- interrupt disable
- hardware malfunction
- kernel panic

### Network Flag 문제

TCP flag 오염 시:

- invalid packet
- connection failure

---

## 6. 핵심 메커니즘

핵심은 **비트 단위 독립 연산**입니다.

### A. AND (`&`)

**목적:** 특정 비트만 추출/검사

| A | B | Result |
|---|---|--------|
| 1 | 1 | 1 |
| 1 | 0 | 0 |
| 0 | 1 | 0 |
| 0 | 0 | 0 |

```
value  = 10110010
mask   = 00000100
result = 00000000
```

**용도:** bit masking, bit extraction, permission check

---

### B. OR (`|`)

**목적:** 특정 비트를 1로 설정

| A | B | Result |
|---|---|--------|
| 0 | 0 | 0 |
| 1 | 0 | 1 |
| 0 | 1 | 1 |
| 1 | 1 | 1 |

```
value  = 10110010
mask   = 00000100
result = 10110110
```

**용도:** flag enable, permission set

---

### C. XOR (`^`)

**목적:** 비트 반전(toggle)

| A | B | Result |
|---|---|--------|
| 0 | 0 | 0 |
| 1 | 1 | 0 |
| 1 | 0 | 1 |
| 0 | 1 | 1 |

```
value  = 10110010
mask   = 00000100
result = 10110110
```

**용도:** toggle, parity, checksum

---

### D. NOT (`~`)

**목적:** 전체 비트 반전

```
  10110010
↓
  01001101
```

---

### E. SHIFT (`<<`, `>>`)

**목적:** 비트 위치 이동

```
1 << 3 → 00001000
```

**용도:** mask 생성, multiply/divide optimization, address calculation

---

### 핵심 흐름

```
Operand Load
     ↓
ALU Bitwise Logic
     ↓
Per-bit Independent Processing
     ↓
Result Register
     ↓
Store or Branch
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux Kernel

매우 광범위하게 사용:

- `task flags`
- `page flags`
- `inode flags`
- `scheduler state`

### Filesystem Permission

```bash
chmod 755
```

내부적으로 permission bit 조작.

### TCP/IP Stack

TCP header 비트 플래그:

- `SYN`
- `ACK`
- `FIN`
- `RST`

### Runtime

runtime metadata:

- `GC mark`
- `lock state`
- `object metadata`

### CPU 상태 플래그

- `ZF` (Zero Flag)
- `CF` (Carry Flag)
- `OF` (Overflow Flag)
- `SF` (Sign Flag)

### Kubernetes

직접 노출보다는 다음 내부에서 광범위 사용:

- kernel
- cgroup
- networking stack
- container runtime

### 관측 도구

- `perf`
- `eBPF`
- `ftrace`
- hardware counters
- kernel tracepoint

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*