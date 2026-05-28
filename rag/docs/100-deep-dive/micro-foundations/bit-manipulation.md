# 비트 조작 (Bit Manipulation)

> 정독: 0회

## 1. 이 기술이 무엇인가

**비트 조작(Bit Manipulation)** 은:

> 정수 데이터 내부의 특정 비트만 선택적으로 변경하는 연산

### 핵심 특징

- 주변 비트는 유지
- 특정 비트만 수정
- 매우 작은 비용
- CPU 수준에서 직접 수행 가능

### 대표 연산

| 연산 | 목적 |
|------|------|
| OR `\|` | 비트를 1로 설정(Set) |
| AND `&` | 비트를 0으로 제거(Clear) |
| XOR `^` | 비트를 반전(Toggle) |
| NOT `~` | 전체 비트 반전 |

### 예시

```python
flags = flags | 0b00000100  # 특정 비트를 강제로 1로 설정
```

---

## 2. 시스템 어디에서 등장하는가

비트 조작은 시스템 전반에서 매우 많이 사용됩니다.

### 대표 영역

| 영역 | 사용 사례 |
|------|-----------|
| OS Kernel | process state |
| Runtime | GC state |
| CPU | status register |
| Filesystem | permission bit |
| Network | TCP flags |
| Hypervisor | VM metadata |
| Driver | device control register |
| Scheduler | task flags |
| Locking | mutex/spinlock state |
| Security | capability flags |

### 대표 상태

- `locked`
- `dirty`
- `marked`
- `ready`
- `interrupt-enabled`
- `writable`

---

## 3. 어떤 자원에 가장 영향이 큰가

### 1순위: CPU

ALU bitwise operation, register manipulation, branch control, flag update 와 직접 연결되기 때문입니다.

### 2순위: Memory

compact metadata, cache efficiency, reduced memory footprint 와 연결됩니다.

---

## 4. 왜 중요한가

비트 조작은 **아주 작은 메모리와 매우 낮은 CPU 비용으로 상태를 관리**하게 해줍니다.

### 예시

64bit 하나 안에 다음을 함께 저장 가능:

- `permission`
- `lock`
- `gc mark`
- `version`
- `status`

### 장점

- 메모리 절약
- cache locality 향상
- 빠른 상태 판별
- 빠른 상태 변경
- branch 최적화

> 커널/런타임 수준에서는 핵심 최적화 기술입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### Wrong Bit Update

잘못된 비트 수정 시 발생 가능한 문제:

- invalid state
- corrupted metadata
- wrong branch

### Concurrent Update 문제

멀티스레드에서 `read-modify-write` 충돌 시 발생 가능:

- lost update
- inconsistent state

### Atomicity 문제

비트 조작이 atomic하지 않으면:

- lock corruption
- race condition
- deadlock
- scheduling issue

### Permission Corruption

permission bit 손상 시:

- unauthorized access
- privilege escalation

### Device Register 문제

device control bit 오염 시:

- interrupt failure
- hardware instability
- kernel panic

---

## 6. 핵심 메커니즘

핵심은 **특정 비트만 수정하고 나머지는 유지**하는 것입니다.

### A. Bit Set (1로 설정)

**목표:** 특정 비트를 강제로 1로 변경

**사용:** `value | mask`

```python
value  = 0b10000001
mask   = 0b00000100
result = value | mask
# 결과: 0b10000101
```

> mask의 1 위치만 강제로 1이 됩니다.

---

### B. Bit Clear (0으로 제거)

**목표:** 특정 비트를 강제로 0으로 변경

**사용:** `value & ~mask`

```python
value  = 0b10000101
mask   = 0b00000100
result = value & ~mask
# 결과: 0b10000001
```

> mask의 target 위치만 0으로 차단됩니다.

---

### C. Bit Toggle (반전)

**목표:** 0 → 1, 1 → 0

**사용:** `value ^ mask`

```python
value  = 0b10000101
mask   = 0b00000100
result = value ^ mask
# 결과: 0b10000001
```

> mask의 1 위치만 반전됩니다.

---

### 핵심 흐름

```
Operand Load
     ↓
Mask 준비
     ↓
ALU Bitwise Operation
     ↓
Target Bit 수정
     ↓
Store Back
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux Kernel

task state는 bit flag 기반:

- `RUNNING`
- `SLEEPING`
- `INTERRUPTIBLE`

### Filesystem Permission

```
rwxr-xr-x
```

permission bit 조작.

### TCP/IP Stack

TCP flag:

- `SYN`
- `ACK`
- `FIN`
- `RST`

### Runtime Metadata

GC state:

- `marked`
- `forwarded`
- `locked`

### Device Register

MMIO register bit control이 매우 광범위하게 사용됩니다.

### Kubernetes

직접적 노출보다는 다음 내부에서 광범위하게 사용:

- kernel
- cgroup
- container runtime
- networking layer

### Observability

다음 도구에서 간접 관측 가능:

- `perf`
- `eBPF`
- hardware counters
- kernel trace

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*