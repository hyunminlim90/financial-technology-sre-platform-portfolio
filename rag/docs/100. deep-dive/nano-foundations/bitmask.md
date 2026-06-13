# 비트마스크 (Bitmask)

> 정독: 0회

## 1. 이 기술이 무엇인가

**비트마스크(Bitmask)** 는:

> 정수 내부의 특정 비트만 선택적으로 읽거나 수정하기 위한 비트 연산 기법

**핵심:**

하나의 정수 내부 비트들을 상태 플래그처럼 사용하는 것

**예시:**

```
0000 0101
```

- bit 0 = ON
- bit 2 = ON

두 상태를 하나의 정수로 동시에 표현할 수 있습니다.

비트마스크는 주로 상태 플래그, 권한 제어, 메모리 최적화, CPU 제어, 런타임 메타데이터 등에 사용됩니다.

<details>
<summary>Deep Dive</summary></br>

Operand(피연산자) [[M]](../../100-deep-dive/micro-foundations/operand.md)  
Target Bit(타겟 비트) [[M]](../../100-deep-dive/micro-foundations/target-bit.md)  
Bit Extraction(비트 추출) [[M]](../../100-deep-dive/micro-foundations/bit-extraction.md)  
Bit Manipulation(비트 조작) [[M]](../../100-deep-dive/micro-foundations/bit-manipulation.md)  
Bitwise Operation(비트별 논리 연산) [[M]](../../100-deep-dive/micro-foundations/bitwise-operation.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

비트마스크는 시스템 전체에 매우 광범위하게 존재합니다.

| 영역 | 사용 사례 |
|------|-----------|
| OS Kernel | permission bits |
| CPU Register | flag register |
| MMU | page permission |
| Runtime | GC mark bits |
| File System | rwx permission |
| Network Stack | TCP flags |
| Scheduler | CPU affinity |
| Synchronization | lock state |
| Driver | device register |
| Kubernetes | resource flags / internal states |

**대표 사례:** `chmod 755` 는 실제로 rwx permission bitmask입니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 영향이 큰 자원은 **CPU + Memory** 입니다.

- ALU bitwise operation 사용
- 메모리 절약 효과 큼
- cache locality 향상
- branch 감소 가능

> 상태를 bool 여러 개 대신 비트 하나로 압축 가능

---

## 4. 왜 중요한가

비트마스크는 **극도로 빠르고 메모리 효율적**입니다.

| 방식 | 메모리 |
|------|--------|
| boolean 8개 | 최소 8 byte 이상 |
| bitmask | 1 byte |

CPU 입장에서 AND/OR/XOR는 매우 빠른 단일 ALU 연산입니다.

그래서 OS, DBMS, JVM/CLR, Hypervisor, Network stack 같은 저수준 시스템에서 매우 중요합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 잘못된 Permission
`chmod` 등에서 권한 비트 오류가 발생할 수 있습니다.

### Race Condition
멀티스레드 환경에서 bit flag update 충돌이 발생할 수 있습니다.

### Lock 상태 손상
잘못된 CAS/bit operation 시 deadlock, invalid state가 발생할 수 있습니다.

### GC Metadata Corruption
mark bit 손상 시 object leak, premature reclamation이 발생할 수 있습니다.

### Kernel Panic
page table permission bit 손상 시 invalid access, protection fault가 발생할 수 있습니다.

### CPU Flag 문제
condition flag corruption 발생 시 잘못된 branch, undefined behavior가 발생할 수 있습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

**핵심:** 특정 비트만 선택적으로 조작하는 것입니다.

### 상태를 비트로 표현

```
0000 0001  ->  READ
0000 0010  ->  WRITE
0000 0100  ->  EXECUTE
```

### 여러 상태를 하나에 압축

```
0000 0011  ->  READ + WRITE 동시 ON
```

### 특정 비트 읽기 (AND)

```c
flags & READ
// 0 아니면 ON, 0이면 OFF
```

### 특정 비트 켜기 (OR)

```c
flags |= WRITE
```

### 특정 비트 끄기

```c
flags &= ~WRITE
```

### 특정 비트 토글 (XOR)

```c
flags ^= EXECUTE
```

**핵심 포인트:**
> 비트마스크의 본질은 하나의 정수 내부 비트를 독립 상태 공간처럼 사용하는 것

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux Permission 확인

```bash
ls -l
chmod
```

### CPU Flags 확인

```bash
cat /proc/cpuinfo
```

### Process Flags 확인

```bash
ps
top
# 내부적으로 flag bit 사용
```

### Network TCP Flags 분석

```bash
tcpdump
wireshark
# SYN/ACK/FIN/RST 모두 bitmask 기반
```

### cgroup / namespace flags

Linux kernel 내부적으로 광범위하게 사용됩니다.

### Kubernetes 환경

K8s 자체도 내부적으로 condition flags, feature gates, resource state 등에 비트 플래그 개념을 사용합니다.

### Runtime / GC

런타임 내부에서 GC mark bit, object lock state, header metadata 등에 사용됩니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*