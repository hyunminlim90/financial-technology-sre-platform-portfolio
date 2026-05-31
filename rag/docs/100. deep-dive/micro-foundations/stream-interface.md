# 스트림 인터페이스 (Stream Interface)

> 정독: 0회

## 1. 이 기술이 무엇인가

스트림 인터페이스(Stream Interface)는:

> 데이터를 고정 크기 블록이 아니라 연속적인 바이트 흐름(Byte Stream)으로 다루게 하는 입출력 추상화 방식

**핵심 특징:**

- 연속적 데이터 흐름
- 블록 경계 은폐
- 순차적 읽기/쓰기 중심
- 동일한 I/O API 사용
- 하드웨어 구조 추상화

상위 계층은 "몇 번째 블록"을 직접 다루지 않고, **"몇 바이트 읽고 쓸 것인가"** 만 다룹니다.

**대표 인터페이스:**

- file stream
- socket stream
- pipe
- stdin / stdout
- TCP stream

> **핵심:** 스트림 인터페이스는 데이터를 연속 바이트 흐름으로 추상화합니다.

<details>
<summary>Deep Dive</summary></br>

Data(데이터) [[M]](../../100-deep-dive/micro-foundations/data.md)  
Logical Block(논리 블록) [[M]](../../100-deep-dive/micro-foundations/logical-block.md)  
Byte Stream(바이트 스트림) [[M]](../../100-deep-dive/micro-foundations/byte-stream.md)  
I/O Abstraction(입출력 추상화) [[M]](../../100-deep-dive/micro-foundations/io-abstraction.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

### 파일 I/O

`read()` / `write()` 기반 파일 접근.

### 네트워크

TCP 자체가 대표적인 stream interface.

### 프로세스 간 통신

- pipe
- FIFO
- Unix socket

### 표준 입출력

- `stdin`
- `stdout`
- `stderr`

### 스토리지 계층

```
VFS → page cache → stream abstraction
```

### Runtime 환경

많은 runtime이 내부적으로 stream 기반으로 동작합니다:

- file descriptor
- async stream
- buffered I/O

### Kubernetes

등장 위치:

- container logs
- stdout streaming
- kubectl exec
- CRI runtime logging

### Linux 계층

```
User Space
→ System Call
→ VFS
→ Stream-like I/O abstraction
```

> **핵심:** 스트림 인터페이스는 파일/네트워크/IPC 전체를 관통하는 공통 I/O 추상화입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

### Disk 영향 — 파일 stream에서 매우 큼

- sequential read
- buffered write
- page cache

### Network 영향 — socket stream에서 절대적

- TCP buffer
- socket queue
- flow control

### Memory 영향 — 매우 큼

**버퍼링(Buffering)** 이 핵심 메커니즘이기 때문입니다:

- page cache
- socket buffer
- ring buffer
- userspace buffer

### CPU 영향 — 상당히 큼

- copy operation
- interrupt handling
- syscall transition
- context switching

> **핵심:** 스트림 인터페이스는 메모리 버퍼링 기반 I/O 추상화입니다.

---

## 4. 왜 중요한가

### 하드웨어 복잡성 은폐

상위 계층은 SSD block size, network packet size, DMA alignment 같은 물리 조건을 몰라도 됩니다.

### 동일한 프로그래밍 모델 제공

파일이든 네트워크든 `read()` / `write()` 중심으로 처리 가능합니다.

### 버퍼링 최적화 가능

커널이 자동으로 수행합니다:

- batching
- caching
- readahead
- writeback

### 비동기 I/O 기반

현대 고성능 I/O의 핵심입니다:

- epoll
- io_uring
- async socket

### 대규모 데이터 처리 기반

streaming log, media streaming, DB replication, distributed event stream 등 전부 stream 기반입니다.

> **핵심:** 스트림 인터페이스는 하드웨어 제약을 숨긴 범용 데이터 흐름 추상화입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 대표 장애 유형

| 장애 | 설명 |
|---|---|
| Buffer Overflow | 버퍼 과다 사용 |
| Backpressure Failure | consumer가 producer 속도 못 따라감 |
| Blocking I/O Stall | read/write block |
| Socket Buffer Saturation | network stream 정체 |
| Partial Read/Write 처리 실패 | stream 특성 미이해 |
| Page Cache Explosion | memory pressure 발생 |
| Stream Truncation | 중간 데이터 손실 |

### Kubernetes 장애

| 장애 | 원인 |
|---|---|
| Container Log Explosion | stdout stream 과다 |
| Streaming API Timeout | watch stream disconnect |
| Network Stream Congestion | service latency 증가 |

### Storage 장애

- Writeback Stall — dirty page 과다
- fsync latency 증가 — stream flush 병목

### SRE 핵심 포인트

> CPU보다 **buffer saturation / queue congestion / stream backpressure** 가 병목 원인인 경우가 많습니다.

---

## 6. 핵심 메커니즘

**바이트 흐름 추상화 + 버퍼링**이 핵심입니다.

### 전체 흐름

```
1단계  Application    write(fd, buffer, size) 호출
2단계  커널           userspace buffer 복사
3단계  커널           kernel buffer / page cache 저장
4단계  커널           하부 물리 규격에 맞게 변환
                       (block I/O / network packet / DMA transaction)
5단계  Device Driver  실제 hardware command 생성
```

### 중요한 핵심

상위 계층은 **연속된 stream** 만 보지만, 하부에서는 실제로 다음이 발생합니다:

- block split
- packet segmentation
- DMA chunk
- queue scheduling

### Stream vs Block

| 구분 | 특성 |
|---|---|
| Stream | 연속 바이트 흐름 |
| Block | 고정 크기 단위 저장 |

현대 OS는 **stream abstraction 위에서 block device를 제어**합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 파일 descriptor 확인
ls /proc/<pid>/fd

# 열린 stream 확인
lsof

# pipe/socket 확인
ss -tulnp

# I/O tracing
strace

# page cache 상태
free -h
vmstat

# stream buffering 관측
iotop
pidstat -d
```

### Runtime 관측 포인트

- blocking I/O
- async stream
- event loop
- socket backlog
- stream latency

### Kubernetes

```bash
# container log stream
kubectl logs -f

# exec stream
kubectl exec -it

# API watch stream
kubectl get pod -w
```

Network stream 관측 포인트:

- TCP retransmission
- socket queue
- stream timeout

> **핵심:** 현대 Linux/K8s I/O 대부분은 stream abstraction 기반입니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*