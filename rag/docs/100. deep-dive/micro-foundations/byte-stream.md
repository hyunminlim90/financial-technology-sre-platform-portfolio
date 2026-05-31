# 바이트 스트림 (Byte Stream)

> 정독: 0회

## 1. 이 기술이 무엇인가

바이트 스트림(Byte Stream)은:

> 데이터를 구조체, 레코드, 블록 같은 고정 형식으로 보지 않고, **0번부터 N번까지 이어지는 연속적인 바이트(byte) 흐름**으로 취급하는 데이터 전달 모델

**핵심 특징:** 데이터 내부 경계(boundary)를 커널이나 하드웨어가 강제하지 않습니다.

즉, 상위 계층이 원하는 크기로 자유롭게 입출력 가능합니다:

```
1 byte 읽기 가능
17 byte 쓰기 가능
1024 byte 읽기 가능
```

> **핵심:** 바이트 스트림은 "데이터를 연속된 바이트 흐름으로 다루는 운영체제 표준 I/O 추상화 모델"입니다.

---

## 2. 시스템 어디에서 등장하는가

### 등장 위치

| 위치 | 설명 |
|---|---|
| 일반 파일 I/O | `read()` / `write()` |
| 네트워크 소켓 | TCP socket은 대표적인 byte stream |
| Pipe / FIFO | 프로세스 간 통신도 byte stream 기반 |
| Terminal | stdin / stdout도 byte stream |
| VFS 계층 | Linux VFS 핵심 I/O 모델 |
| Container | stdout / stderr도 byte stream |

### Kubernetes

다음 모두 내부적으로 byte stream을 사용합니다:

- container logs
- exec stream
- API streaming
- TCP traffic
- CSI socket communication

> **핵심:** Unix/Linux 대부분의 I/O는 byte stream 모델 기반입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

### Memory 영향 — 매우 큼

page cache / socket buffer / kernel buffer / pipe buffer 전부 byte stream 임시 저장에 사용됩니다.

### Network 영향 — 핵심적

TCP 자체가 byte stream 프로토콜입니다.

### Disk 영향 — 중요

filesystem I/O 대부분이 byte stream 기반이지만, 실제 저장 시에는 변환이 발생합니다:

```
byte stream → block I/O 변환
```

### CPU 영향 — 높음

- syscall overhead
- copy overhead
- buffer management
- polling

> **핵심:** byte stream은 커널 I/O buffer subsystem과 강하게 연결됩니다.

---

## 4. 왜 중요한가

### 하드웨어 경계 제거

상위 계층은 sector / page / flash block 같은 물리 구조를 몰라도 됩니다.

### 통일된 I/O 모델

`read()` / `write()` 동일 API로 file / socket / pipe / terminal / device 모두 처리 가능합니다.

### 다형성 기반

VFS + file object + file_operations 구조를 가능하게 만듭니다.

### 유연한 데이터 처리

애플리케이션이 임의 크기로 처리 가능합니다.

### page cache 최적화 가능

커널이 내부적으로 block 정렬을 수행합니다.

> **핵심:** byte stream은 상위 소프트웨어를 하부 저장장치 물리 구조로부터 격리합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 대표 장애 유형

| 장애 | 설명 |
|---|---|
| Partial Read / Partial Write | `read()`가 요청한 크기보다 적게 반환 가능 |
| Buffer Overflow | stream 처리 오류 |
| Backpressure 문제 | consumer보다 producer가 빠름 |
| Socket Buffer Full | network congestion 발생 |
| Page Cache Dirty Explosion | write stream 과다 발생 |
| Disk Flush 지연 | byte stream이 block flush 대기 |
| Pipe Deadlock | stream consumer 멈춤 |

### Kubernetes 장애

| 장애 | 원인 |
|---|---|
| container logging 폭주 | stdout byte stream 폭증 |
| API streaming stall | watch stream 문제 |
| sidecar proxy congestion | stream backpressure |

> **핵심:** 대부분의 Linux I/O 병목은 실제로 byte stream 흐름 제어 문제입니다.

---

## 6. 핵심 메커니즘

### 전체 흐름

```
User Space
→ Byte Stream
→ Kernel Buffer / Page Cache
→ Block I/O
→ Storage
```

### write() 흐름

```
1단계  process        write(fd, buf, size)
2단계  커널           연속된 byte stream으로 취급
3단계  커널           page cache에 적재 (일반적으로 4KB page 단위 관리)
4단계  flush 시점     byte stream → logical block 변환
5단계  Block Layer    block I/O 생성
6단계  Device Driver  → storage 전달
```

### read() 흐름

```
storage block read → page cache → byte stream 반환
```

### 중요한 특징

커널은 내부적으로 **block 기반 저장**을 하지만, 상위에는 **stream 기반 인터페이스**를 제공합니다.

### TCP Stream 특징

TCP는 메시지 경계가 없습니다:

```
send 100B × 10번
→ recv는 임의 크기로 도착 가능
```

> **핵심:** byte stream에는 메시지 경계 개념이 없습니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 열린 stream 확인
lsof

# pipe/socket 확인
ss -tulpn

# stream syscall 추적
strace -e read,write

# page cache 상태
cat /proc/meminfo

# dirty page 확인
grep Dirty /proc/meminfo
```

### Runtime

container runtime 전부 stream 기반입니다:

- stdout pipe
- stderr pipe
- unix socket
- overlayfs I/O

### Kubernetes

```bash
# container logs (내부적으로 stream)
kubectl logs

# exec session (stream multiplexing 사용)
kubectl exec

# API watch (continuous byte stream 기반)
kubectl get pod -w
```

CSI communication은 unix domain socket stream을 사용합니다.

### Observability 도구

```bash
strace    # syscall 추적
perf      # 성능 분석
ss        # socket 상태
tcpdump   # 네트워크 패킷
iotop     # I/O 사용량
pidstat   # 프로세스별 통계
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*