# 비트 (bit)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**비트(bit)** 는:

> **Binary Digit** 의 약자이며, 컴퓨터가 처리하는 **가장 작은 정보 단위**

비트는 오직 두 가지 상태만 가진다:

| 값 | 의미 |
|----|------|
| 0 | Off / False / Low Voltage |
| 1 | On / True / High Voltage |

현대 컴퓨터 시스템의 CPU 연산, 메모리 저장, 네트워크 전송, 디스크 기록 모두 결국 **비트 단위로 동작**한다.

<details>
<summary>Deep Dive</summary></br>

연산(Operation/Computation) [[M]](../../100-deep-dive/micro-foundations/computation-operation.md)  
메모리(Memory) [[M]](../../100-deep-dive/micro-foundations/memory.md)  
네트워크(Network) [[M]](../../100-deep-dive/micro-foundations/network.md)  
디스크(Secondary Storage) [[M]](../../100-deep-dive/micro-foundations/secondary-storage.md)

</details></br>

## 2. 시스템 어디에서 등장하는가

비트는 사실상 **시스템 전체의 기초 단위**이다.

### CPU
- 레지스터(Register)
- ALU 연산
- 명령어 실행
- CPU Flags

<details>
<summary>Deep Dive</summary></br>

System(시스템) [[M]](../../100-deep-dive/micro-foundations/system.md)  
Register(레지스터) [[M]](../../100-deep-dive/micro-foundations/register.md)  
ALU(Arithmetic Logic Unit) [[M]](../../100-deep-dive/micro-foundations/arithmetic-logic-unit.md)  
Instruction Cycle(명령어 사이클) [[M]](../../100-deep-dive/micro-foundations/instruction-cycle.md)  
CPU Flags [[M]](../../100-deep-dive/micro-foundations/cpu-flags.md)

</details></br>

### Memory
- DRAM Cell
- Cache Line
- Page

<details>
<summary>Deep Dive</summary></br>

DRAM Memory Cell(DRAM 메모리 셀) [[M]](../../100-deep-dive/micro-foundations/dram-memory-cell.md)  
Cache Line(캐시 라인) [[M]](../../100-deep-dive/micro-foundations/cache-line.md)  
Virtual Page(가상 페이지) [[M]](../../100-deep-dive/micro-foundations/virtual-page.md)  

</details></br>

### Network
- Ethernet Frame
- TCP/IP Packet
- NIC Throughput

<details>
<summary>Deep Dive</summary></br>

IEEE 802.3 Frame(이더넷 프레임) [[M]](../../100-deep-dive/micro-foundations/ieee-802.3-frame.md)  
Internet Protocol Suite(인터넷 프로토콜 스위트) [[M]](../../100-deep-dive/micro-foundations/internet-protocol-suite.md)  
IP 패킷(IP Packet) [[M]](../../100-deep-dive/micro-foundations/ip-packet.md)  
Network Throughput(네트워크 처리율) [[M]](../../100-deep-dive/micro-foundations/network-throughput.md)  

</details></br>

### Disk / Storage
- SSD NAND Flash
- Filesystem Metadata
- Block Device

<details>
<summary>Deep Dive</summary></br>

Flash SSD(Flash Memory-based Solid State Drive) [[M]](../../100-deep-dive/micro-foundations/flash-memory-based-solid-state-drive.md)  
Filesystem Metadata(파일시스템 메타데이터) [[M]](../../100-deep-dive/micro-foundations/filesystem-metadata.md)  
Block Device File(블록 장치 파일) [[M]](../../100-deep-dive/micro-foundations/block-device-file.md)  

</details></br>

### Runtime / JVM
- Object Header
- Pointer
- Bit Mask
- GC Metadata

### Security
- Hash
- Encryption
- Signature
- TLS

### Kubernetes / Container
- cgroup bitmap
- CPU affinity
- NUMA mask

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

비트 자체는 **모든 자원의 공통 최소 단위**이다. 즉, 특정 자원 하나만의 개념이 아니다.

하지만 실무적으로는:

| 영역 | 대표 의미 |
|------|----------|
| CPU | 비트 연산 |
| Memory | 데이터 저장 단위 |
| Network | 대역폭 단위 |
| Disk | 저장 단위 |

특히 네트워크에서는 `Gbps = Gigabit per second` 처럼 **throughput 기준 단위**로 자주 사용된다.

---

## 4. 왜 중요한가

비트는 **디지털 시스템 전체의 물리적 기초**이다.

모든 데이터는 결국 **0과 1의 조합**으로 표현된다:

| 데이터 | 내부 표현 |
|--------|----------|
| 숫자 | Binary |
| 문자열 | ASCII / UTF-8 |
| 이미지 | Pixel Bit |
| 암호화 | Bitwise Operation |

### FinTech에서 중요한 이유

결제 시스템의 금액, 계좌, 승인 상태, 암호화 데이터 모두 **비트 수준 정확성**을 요구한다.

> ⚠️ **1bit 오류도 장애 가능**

---

## 5. 실제 장애와 어떤 관련이 있는가

### Bit Flip
메모리 오류로 특정 비트가 뒤집힘

원인: ECC 미적용, Cosmic Ray, 전압 불안정

결과: 데이터 손상, 계산 오류, JVM Crash

### Network Corruption
패킷 일부 비트 손상 시:
- TCP Retransmission
- TLS Failure
- Signature Mismatch

### Filesystem Corruption
디스크 비트 오류 발생 시:
- Checksum mismatch
- RAID rebuild
- Data corruption

### CPU Calculation Error
ALU 비트 연산 오류 발생 시:
- 금융 계산 오류
- Hash mismatch
- Cryptographic failure

### Serialization Error
Bit alignment 오류 시:
- Packet decode failure
- Protocol mismatch

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

### Binary Representation
모든 데이터는 binary 형태 저장

### Bitwise Operation
대표 연산: `AND`, `OR`, `XOR`, `NOT`, `Shift`

### Byte
`8 bits = 1 byte`

### Word Size
CPU 기본 처리 단위. 예: `32bit CPU`, `64bit CPU`

### Endianness
비트/바이트 저장 순서: Big Endian / Little Endian

### ECC (Error Correcting Code)
메모리 비트 오류 감지/복구

### Checksum / CRC
비트 손상 검증 메커니즘

### Bit Mask
특정 상태 플래그 제어 기법

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### CPU Architecture 확인
```bash
lscpu
```

### 메모리 ECC 확인
```bash
dmesg | grep -i ecc
```

### 네트워크 속도 확인
```bash
ethtool eth0
```

### Bit 단위 파일 확인
```bash
hexdump -C file.bin
```

### Binary 확인
```bash
xxd file.bin
```

### CPU Flags 확인
```bash
cat /proc/cpuinfo
```

### NUMA Bitmask 확인
```bash
numactl --hardware
```

### Kubernetes CPU Set 확인
```bash
cat /sys/fs/cgroup/cpuset.cpus
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*