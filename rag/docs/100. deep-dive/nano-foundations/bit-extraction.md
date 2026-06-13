# 비트 추출 (Bit Extraction)

> 정독: 0회

## 1. 이 기술이 무엇인가

**비트 추출(Bit Extraction)** 은:

> 정수 데이터 내부의 특정 비트만 선택적으로 분리해서 읽어내는 연산

보통 다음 연산으로 수행됩니다.

```
value & mask
```

**예시:**

```
value = 0b10110100
mask  = 0b00000100

value & mask = 0b00000100
```

특정 비트만 남기고 나머지는 모두 제거합니다.

<details>
<summary>Deep Dive</summary></br>

Operand(피연산자) [[M]](../../100-deep-dive/micro-foundations/operand.md)  
Target Bit(타겟 비트) [[M]](../../100-deep-dive/micro-foundations/target-bit.md)  
Bit Masking(비트 마스킹) [[M]](../../100-deep-dive/micro-foundations/bit-masking.md)  
Bit Test Operation(비트 테스트 연산) [[M]](../../100-deep-dive/micro-foundations/bit-test-operation.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

비트 추출은 시스템 전체에서 매우 많이 사용됩니다.

| 영역 | 사용 사례 |
|------|-----------|
| OS Kernel | page state |
| Runtime | GC mark bit |
| CPU | status flag |
| Network | TCP flag |
| Filesystem | permission bit |
| Driver | device register |
| Hypervisor | VM state |
| Scheduler | task state |
| Security | capability flag |
| Locking | mutex state |

locked, dirty, marked, readable, writable, interrupt enabled 같은 상태를 읽을 때 사용됩니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 영향이 큰 자원은 **CPU** 입니다. ALU, register, bitwise execution, branch prediction과 직접 연결됩니다.

두 번째는 **Memory** 입니다. metadata 압축, cache 효율, memory footprint 감소와 강하게 연결되기 때문입니다.

---

## 4. 왜 중요한가

비트 추출은:

> 매우 작은 비용으로 상태를 판별할 수 있게 해줍니다.

하나의 64bit 정수 안에 lock state, gc mark, dirty state, permission, version 등을 동시에 저장하고, 필요한 비트만 **1 instruction 수준 비용**으로 읽을 수 있습니다.

이 방식이 없으면 메모리 사용량 증가, cache miss 증가, metadata object 증가, pointer chasing 증가가 발생합니다. 대규모 런타임/커널에서는 매우 중요합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 잘못된 Bit Position
잘못된 비트 위치 추출 시 잘못된 상태 판별, invalid runtime behavior, corrupted metadata가 발생할 수 있습니다.

### Atomicity 문제
멀티스레드 환경에서 동시 read-modify-write 충돌 시 race condition, stale state, inconsistent flag가 발생할 수 있습니다.

### Wrong Mask
mask 설계 오류 시 원하지 않는 비트 노출, permission bypass, lock corruption이 발생할 수 있습니다.

### Branch Misbehavior
잘못된 추출 결과로 wrong branch execution, invalid state transition이 발생할 수 있습니다.

### Hardware Register Bug
device register bit extraction 실패 시 interrupt failure, device malfunction, kernel panic이 발생할 수 있습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

**핵심:**
> 원본 데이터에서 특정 비트만 살아남게 만드는 것

### 핵심 흐름

```
1) Operand 준비
   └─ 원본 데이터: value = 0b10110100

2) Mask 준비
   └─ 특정 위치만 1: mask = 0b00000100

3) AND 연산
   └─ result = value & mask

4) 결과 해석
   ├─ result == 0      → target bit = 0
   └─ result != 0      → target bit = 1
```

### 핵심 구조

```
Operand
  ↓
Bitmask
  ↓
AND
  ↓
Target Bit 추출
  ↓
State 판별
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux Kernel 권한 비트

```bash
ls -l
chmod
# 내부적으로 bit extraction 사용
```

### TCP/IP Stack

TCP header의 SYN, ACK, FIN, RST를 비트 추출로 판별합니다.

```bash
tcpdump
wireshark
```

### CPU Flags

CPU status register 내부의 Zero Flag, Carry Flag, Overflow Flag 추출에 사용됩니다.

```bash
gdb
# info registers
```

### Runtime / GC

GC metadata의 marked, forwarded, locked 등을 bit extraction으로 읽습니다.

### Device Driver

MMIO register에서 interrupt state, device ready, error state 추출을 많이 수행합니다.

### perf / eBPF

```bash
perf stat
perf record
# 커널 bitwise hotspot 관측 가능
```

### Kubernetes 환경

K8s 상위 레벨보다 kernel, cgroup, container runtime, network stack 내부에서 많이 사용됩니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*