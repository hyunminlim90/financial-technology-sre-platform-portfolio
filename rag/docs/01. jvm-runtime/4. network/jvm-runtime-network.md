# JVM Runtime — Network Resource Analysis
## FinTech 결제 시스템 SRE 관점 | E2E Network 계층 분석 (Hardware → OS Kernel → JVM Runtime → Framework → Application)

> 정독: 0회

## 목차

1. [물리/가상 Network 및 NIC 스펙 확인 지표](#1-물리가상-network-및-nic-스펙-확인-지표)
2. [JVM Network E2E 실행 흐름](#2-jvm-network-e2e-실행-흐름)
3. [JVM Network 사용 메커니즘 및 실행 모델](#3-jvm-network-사용-메커니즘-및-실행-모델)
4. [Network 병목 및 위험 발생 지점](#4-network-병목-및-위험-발생-지점)
5. [SRE 관점 모니터링 지표](#5-sre-관점-모니터링-지표)
6. [장애 시나리오](#6-장애-시나리오)
7. [튜닝 포인트](#7-튜닝-포인트)
8. [관련 Linux 명령어 및 분석 도구](#8-관련-linux-명령어-및-분석-도구)

---

## 1. 물리/가상 Network 및 NIC 스펙 확인 지표

### 1.1 NIC / vNIC / SR-IOV 구조

JVM 프로세스는 OS 네트워크 스택 위에서 동작하므로, 하위 물리 자원의 한계가 JVM의 소켓 성능에 직접 영향을 미친다.

| 구성 요소 | 설명 | SRE 확인 포인트 |
|-----------|------|----------------|
| Physical NIC | 10G/25G/100G 이더넷 카드 | `ethtool eth0` - Speed, Duplex |
| vNIC (virtio-net) | KVM/QEMU 가상화 환경의 가상 NIC | Guest-Host 간 추가 레이어로 인한 Latency 증가 |
| SR-IOV VF | 물리 NIC를 VM에 직접 패스스루 | CPU Steal Time 없이 Near-bare-metal 성능 |
| Container veth | Pod/Container 간 가상 이더넷 쌍 | `ip link show` - veth pair 확인 |
| Overlay NIC | Flannel/Calico/Cilium CNI 터널 | VXLAN/Geneve 헤더 오버헤드 (50~100 bytes) |

```bash
# NIC 기본 스펙 확인
ethtool eth0

# SR-IOV VF 수 확인
cat /sys/class/net/eth0/device/sriov_numvfs

# Container veth 확인
ip link show type veth
```

### 1.2 Bandwidth / MTU / Queue / Ring Buffer

JVM의 고빈도 소켓 I/O는 NIC 수준의 Queue Depth와 Ring Buffer 크기에 민감하게 반응한다.

| 항목 | 기본값 | FinTech 권장 | 확인 명령어 |
|------|--------|------------|-------------|
| MTU | 1500 bytes | 9000 (Jumbo Frame, 내부망) | `ip link show eth0` |
| Rx Ring Buffer | 256 ~ 512 | 4096 | `ethtool -g eth0` |
| Tx Ring Buffer | 256 ~ 512 | 4096 | `ethtool -g eth0` |
| NIC Queue 수 | 1 | CPU Core 수 | `ethtool -l eth0` |
| Bandwidth | 1G/10G | 25G 이상 (결제 클러스터) | `ethtool eth0 \| grep Speed` |

```bash
# Ring Buffer 확인 및 조정
ethtool -g eth0
ethtool -G eth0 rx 4096 tx 4096

# MTU 확인
ip link show eth0 | grep mtu

# NIC Queue 수 확인
ethtool -l eth0
```

### 1.3 TCP/IP Stack 구조

JVM의 `java.net.Socket` 및 `java.nio.SocketChannel`은 모두 OS TCP/IP 스택을 통해 동작한다.

```
JVM (Java Socket API / NIO Channel)
    ↓  JNI (glibc: connect / read / write / epoll_wait)
Kernel TCP/IP Stack
    ├── Socket Buffer (sk_rcvbuf / sk_sndbuf)
    ├── TCP Congestion Control (CUBIC / BBR)
    ├── Netfilter / iptables / nftables
    └── Network Device Driver
NIC Hardware (DMA, Ring Buffer)
```

### 1.4 Virtual Network / Overlay Network / CNI 구조

Kubernetes 환경에서 JVM 기반 Spring Boot Pod는 다음 네트워크 경로를 거친다:

```
[JVM Process in Pod]
    ↓
[veth (Pod Network Namespace)]
    ↓
[Linux Bridge / OVS / eBPF (Node-level)]
    ↓
[Overlay Tunnel: VXLAN(Flannel) / BGP(Calico) / eBPF(Cilium)]
    ↓
[Physical NIC → Upstream Switch → Target Node]
    ↓
[Target Pod veth → JVM Process]
```

**오버헤드 비교:**

| CNI | 추가 레이턴시 | CPU 오버헤드 | 비고 |
|-----|-------------|------------|------|
| Flannel (VXLAN) | ~0.05ms | 중간 | 커널 VXLAN 터널링 |
| Calico (BGP) | ~0.01ms | 낮음 | L3 라우팅, 오버레이 없음 |
| Cilium (eBPF) | ~0.005ms | 매우 낮음 | kube-proxy 대체, eBPF 최적화 |
| Istio Sidecar | +0.1~0.5ms | 높음 | Envoy 추가 홉 발생 |

### 1.5 RSS / RPS / XPS / IRQ Affinity

JVM 애플리케이션의 처리량은 NIC 인터럽트가 어떤 CPU 코어에 분산되느냐에 따라 성능 편차가 발생한다.

| 기술 | 계층 | 목적 |
|------|------|------|
| RSS (Receive Side Scaling) | Hardware NIC | 다중 RX Queue로 패킷을 여러 CPU에 분산 |
| RPS (Receive Packet Steering) | Kernel Software | RSS 미지원 NIC에서 소프트웨어로 분산 |
| XPS (Transmit Packet Steering) | Kernel Software | 송신 Queue를 CPU별로 바인딩 |
| IRQ Affinity | Kernel | 특정 인터럽트를 특정 CPU 코어에 고정 |

```bash
# IRQ 분산 확인
cat /proc/interrupts | grep eth0

# IRQ Affinity 설정
echo "ff" > /proc/irq/[IRQ_NUM]/smp_affinity

# RPS 활성화 (모든 CPU 사용)
echo "ffff" > /sys/class/net/eth0/queues/rx-0/rps_cpus
```

---

## 2. JVM Network E2E 실행 흐름

### 2.1 인바운드 패킷 흐름 (클라이언트 → JVM 결제 서버)

```
[Client Request]
    │
    ▼
[NIC] — DMA → [RX Ring Buffer]
    │
    ▼ (Hardware Interrupt → IRQ)
[CPU: Hard IRQ Handler]
    │  skb(socket buffer) 할당
    ▼
[SoftIRQ: NET_RX_SOFTIRQ]
    │  ksoftirqd 처리
    ▼
[Kernel TCP/IP Stack]
    ├── IP Layer (L3): 라우팅, 단편화
    ├── TCP Layer (L4): 재전송, 흐름제어, 혼잡제어
    └── Socket Receive Buffer (sk_rcvbuf) 적재
    │
    ▼
[File Descriptor / epoll]
    │  JVM: epoll_wait() → JNI → Java NIO Selector.select()
    ▼
[JVM NIO: SocketChannel.read()]
    │  Kernel → User Space 데이터 복사 (Zero-copy 미적용 시)
    ▼
[JVM Runtime: Thread/Virtual Thread 깨우기]
    │  OS Thread → JVM Thread → Carrier Thread (Virtual Thread)
    ▼
[Framework Layer: Netty/Tomcat/Undertow]
    │  ByteBuf / ByteBuffer 디코딩
    ▼
[Application Layer: Controller → Service → Repository]
    │  결제 비즈니스 로직 처리
    ▼
[아웃바운드 응답 전송 (역방향)]
```

### 2.2 아웃바운드 패킷 흐름 (JVM → DB / 외부 PG API)

```
[Application: DB Query / 외부 API 호출]
    │
    ▼
[Connection Pool: HikariCP / WebClient]
    │
    ▼
[JVM: SocketChannel.write() → JNI: write()/send()]
    │
    ▼
[Kernel: Socket Send Buffer (sk_sndbuf)]
    │  TCP Nagle Algorithm / TSO 처리
    ▼
[SoftIRQ: NET_TX_SOFTIRQ]
    │
    ▼
[NIC TX Ring Buffer → DMA → Wire]
```

### 2.3 JVM NIO Selector와 epoll 연동 구조

```
Java NIO Selector
    │  (내부적으로 EPollSelectorImpl 사용)
    ▼
epoll_create1() → epoll_fd 생성
    │
    ├── epoll_ctl(ADD, socketFd, EPOLLIN|EPOLLOUT)  ← 소켓 등록
    │
    └── epoll_wait() → 이벤트 발생 시 blocking 해제
            │
            ▼
        SocketChannel.read()  →  커널 버퍼 → JVM HeapByteBuffer 복사
```

**JVM 19+ Virtual Thread 환경:**
```
Virtual Thread가 SocketChannel.read()에서 I/O 대기 시:
    → Carrier Thread(OS Thread)에서 unmount
    → epoll 기반 park/unpark 메커니즘으로 대기
    → I/O 완료 시 다른 Carrier Thread에 재mount
    → OS Thread 블로킹 없이 수십만 Virtual Thread 동시 처리 가능
```

---

## 3. JVM Network 사용 메커니즘 및 실행 모델

### 3.1 I/O 실행 모델 비교

| 모델 | JVM 구현체 | 특징 | FinTech 적합성 |
|------|-----------|------|--------------|
| Thread-per-Request (BIO) | Tomcat BIO (Deprecated) | 1 Thread = 1 Connection | ❌ 스레드 폭발 위험 |
| NIO Selector (Non-blocking) | Tomcat NIO, Netty | epoll 기반 이벤트 루프 | ✅ 고연결 수 처리 |
| Reactor Pattern | Netty, Spring WebFlux | 소수 Thread로 대규모 I/O | ✅ 고처리량 |
| Virtual Thread (JVM 21+) | Project Loom | 기존 동기 코드 + 비동기 성능 | ✅ 코드 단순성 + 성능 |

### 3.2 Connection Pool (HikariCP / WebClient)

결제 시스템에서 Connection Pool은 가장 빈번한 병목 지점이다.

```
HikariCP 내부 흐름:

getConnection() 호출
    │
    ├── [ConcurrentBag에서 유휴 커넥션 획득] → 즉시 반환 (< 1ms)
    │
    └── [유휴 커넥션 없음]
            │
            ├── maximumPoolSize 미만 → 신규 커넥션 생성 (TCP Handshake + TLS)
            │
            └── maximumPoolSize 도달 → connectionTimeout 대기
                    │
                    └── 대기 초과 → SQLTransientConnectionException 발생
```

**핵심 파라미터:**

| 파라미터 | 기본값 | 결제 서버 권장값 |
|---------|--------|----------------|
| maximumPoolSize | 10 | CPU Core * 2 ~ 4 (DB 연결 수 협의) |
| minimumIdle | 10 | maximumPoolSize와 동일 (Cold Start 방지) |
| connectionTimeout | 30,000ms | 3,000ms (빠른 실패) |
| keepaliveTime | 0 (비활성) | 30,000ms (방화벽 세션 만료 방지) |
| maxLifetime | 1,800,000ms | 600,000ms (DB 방화벽 정책 맞춤) |

### 3.3 Event Loop / Reactor (Netty)

Netty는 JVM 기반 고성능 결제 서버의 핵심 네트워크 프레임워크다.

```
Netty Thread 구조:

Boss Group (NioEventLoopGroup)
    └── BossThread × 1
            │  accept() 처리 → 신규 연결 수락
            ▼
Worker Group (NioEventLoopGroup)
    ├── WorkerThread-0  →  [Channel-1, Channel-5, Channel-9 ...]
    ├── WorkerThread-1  →  [Channel-2, Channel-6, Channel-10 ...]
    └── WorkerThread-N  →  [Channel-N ...]
            │
            └── 각 WorkerThread: epoll 루프로 I/O 이벤트 처리
                    → ChannelPipeline에서 Handler 순차 실행
```

**주의:** WorkerThread에서 블로킹 작업(DB 쿼리 등) 수행 시 전체 채널 처리 지연 발생.  
→ 반드시 별도의 Business Logic Thread Pool로 오프로드 필요.

### 3.4 TLS Handshake

결제 시스템은 모든 통신에 TLS가 필수다. JVM의 TLS 처리 비용은 무시할 수 없다.

```
TLS 1.3 Handshake 흐름 (JVM JSSE / BouncyCastle):

Client → Server: ClientHello (지원 알고리즘, 랜덤값)
Server → Client: ServerHello + Certificate + Finished
Client → Server: Finished (대칭키 확립)
    │
    └── 1-RTT 완료 → Application Data 전송 시작

TLS 1.3 Session Resumption (0-RTT):
    └── Pre-shared Key (PSK) 사용 → 0 RTT로 즉시 데이터 전송 가능
```

**JVM TLS 처리 경로:**
```
javax.net.ssl.SSLEngine (JSSE)
    ├── Hardware 가속: AES-NI CPU 명령어 자동 활용 (JVM 자동 감지)
    ├── OpenSSL 연동: netty-tcnative (Boring SSL) 사용 시 성능 향상
    └── TLS Session Cache: SSLSessionContext (서버/클라이언트 양측)
```

### 3.5 HTTP/1.1 vs HTTP/2 vs gRPC

| 프로토콜 | 멀티플렉싱 | 헤더 압축 | JVM 구현 | FinTech 사용 패턴 |
|---------|-----------|---------|---------|-----------------|
| HTTP/1.1 | ❌ (Keep-Alive만) | ❌ | HttpURLConnection / Apache HC | 레거시 PG API 연동 |
| HTTP/2 | ✅ (스트림 다중화) | ✅ HPACK | OkHttp / WebClient | 내부 MSA 통신 |
| gRPC | ✅ (HTTP/2 기반) | ✅ + Protobuf | grpc-java | 고성능 서비스 간 통신 |

**HTTP/2 JVM 주의사항:**
- HTTP/2 연결 1개당 최대 동시 스트림 수: `SETTINGS_MAX_CONCURRENT_STREAMS` (기본 100)
- JVM에서 HTTP/2 활성화 시 `h2c` (cleartext) 또는 ALPN 협상 필요
- Spring Boot: `server.http2.enabled=true` + HTTP/2 지원 임베디드 서버 필요

### 3.6 Socket Buffer / Backlog Queue

```
TCP 연결 수립 흐름:

Client SYN → [SYN Queue (tcp_max_syn_backlog)] → SYN-ACK 전송
Client ACK → [Accept Queue (somaxconn, backlog)] → accept() 호출 대기

JVM ServerSocketChannel.bind(addr, backlog):
    └── backlog 파라미터가 OS의 Accept Queue 크기를 결정
        (단, 실제 크기는 min(backlog, net.core.somaxconn))
```

### 3.7 TCP Keepalive

JVM 소켓의 Keepalive는 OS 수준에서 동작한다.

```java
// Java Socket에서 TCP Keepalive 활성화
Socket socket = new Socket();
socket.setKeepAlive(true);

// JVM 21+: 세부 파라미터 설정 (Extended Socket Options)
socket.setOption(ExtendedSocketOptions.TCP_KEEPIDLE, 60);   // 60초 유휴 후 시작
socket.setOption(ExtendedSocketOptions.TCP_KEEPINTERVAL, 10); // 10초 간격
socket.setOption(ExtendedSocketOptions.TCP_KEEPCOUNT, 3);    // 3회 미응답 시 연결 종료
```

**결제 시스템 Keepalive 전략:**

| 연결 유형 | 권장 설정 |
|----------|---------|
| DB Connection (HikariCP) | keepaliveTime=30s (방화벽 세션 유지) |
| 외부 PG API | HTTP Keep-Alive + Idle timeout < 방화벽 timeout |
| Istio Sidecar | Envoy keepalive_time 설정 |

### 3.8 Timeout / Retry 정책

결제 시스템의 Timeout 계층화는 Tail Latency 방지의 핵심이다.

```
Timeout 계층 구조:

Application Timeout (비즈니스 정책)
    └── 결제 처리 최대 허용 시간: 예) 5,000ms
        │
        ├── HTTP Client Timeout (WebClient / RestTemplate)
        │       ├── connectTimeout: 1,000ms
        │       └── readTimeout: 3,000ms
        │
        ├── Connection Pool Timeout (HikariCP)
        │       └── connectionTimeout: 2,000ms
        │
        └── TCP/OS Level
                ├── TCP syn timeout (tcp_syn_retries)
                └── TCP RTO (Retransmission Timeout): 초기 200ms, 지수 백오프
```

### 3.9 Nagle Algorithm과 TCP_NODELAY

JVM 소켓은 기본적으로 Nagle Algorithm이 활성화되어 있다.

```java
// 결제 시스템 (저지연 필수): Nagle 비활성화
socket.setTcpNoDelay(true);  // TCP_NODELAY = true

// 데이터 수집 서버 (처리량 중시): Nagle 유지
socket.setTcpNoDelay(false); // 기본값
```

**영향:** Nagle 활성 상태에서 소규모 패킷(ACK 미수신 시)이 버퍼링되어 40ms 지연 발생 가능 (Delayed ACK와 조합 시).

### 3.10 Zero-Copy (JVM에서의 활용)

JVM에서 Zero-copy는 제한적으로 활용된다.

| 메커니즘 | JVM API | 동작 | 적용 사례 |
|---------|---------|------|---------|
| `sendfile()` | `FileChannel.transferTo()` | 파일 → 소켓 직접 전송 | 대용량 정산 파일 전송 |
| `mmap` | `MappedByteBuffer` | 파일을 메모리에 직접 매핑 | 대용량 로그 처리 |
| DirectBuffer | `ByteBuffer.allocateDirect()` | Off-heap 버퍼, GC 대상 아님 | Netty 네트워크 I/O |
| Netty Zero-copy | `CompositeByteBuf` | 버퍼 병합 시 메모리 복사 제거 | Netty 프로토콜 처리 |

### 3.11 DNS Resolution

JVM의 DNS 캐싱 정책은 MSA 환경에서 예상치 못한 장애를 유발할 수 있다.

```
JVM DNS 캐싱 동작:

InetAddress.getByName("payment-service")
    ├── JVM DNS Cache (networkaddress.cache.ttl) 확인
    │       ├── 기본값: 성공 캐시 = 30초 (보안 정책상), 실패 캐시 = 10초
    │       └── 보안 Manager 활성 시: 영구 캐시 (!)
    │
    └── 캐시 미스 시 → OS /etc/resolv.conf → DNS 서버 질의
            └── Kubernetes: CoreDNS 질의
                    └── ndots:5 설정 시 FQDN 탐색 오버헤드 발생
```

**JVM DNS 캐시 TTL 조정:**
```java
// Java Security 설정 (java.security 파일 또는 코드)
java.security.Security.setProperty("networkaddress.cache.ttl", "5");
java.security.Security.setProperty("networkaddress.cache.negative.ttl", "1");
```

---

## 4. Network 병목 및 위험 발생 지점

### 4.1 NIC Saturation

| 징후 | 확인 방법 | JVM 영향 |
|------|---------|---------|
| TX/RX 대역폭 포화 | `sar -n DEV 1` | SocketChannel write 블로킹, Latency 급증 |
| RX/TX Ring Buffer Full | `ethtool -S eth0 \| grep drop` | 패킷 드롭 → TCP 재전송 → Tail Latency |
| SoftIRQ 편중 | `mpstat -P ALL 1` | 특정 코어 100% → 패킷 처리 큐잉 |

### 4.2 JVM Socket Buffer Exhaustion

```
Socket Send Buffer (sk_sndbuf) 포화 흐름:

JVM SocketChannel.write() 호출
    ↓
Kernel Send Buffer에 데이터 적재
    ↓ (버퍼 포화 시)
write() syscall 블로킹 (BIO) 또는 OP_WRITE 이벤트 대기 (NIO)
    ↓
JVM Thread 블로킹 (BIO 모델) 또는 Netty Write Buffer High Watermark 도달
    ↓ 결제 처리 지연 또는 ChannelNotWritableException 발생
```

### 4.3 Ephemeral Port Exhaustion

결제 서버의 외부 API 대량 호출 시 발생하는 대표적 장애.

```
기본 포트 범위: 32768 ~ 60999 (약 28,231개)
고빈도 결제 서버: 초당 수천 건 외부 API 호출 → TIME_WAIT 포트 고갈

확인:
    ss -s | grep TIME-WAIT
    cat /proc/sys/net/ipv4/ip_local_port_range
```

**JVM 레벨 대응:**
- WebClient / RestTemplate Connection Pool 재사용
- `tcp_tw_reuse=1` 커널 파라미터 설정
- 포트 범위 확장: `net.ipv4.ip_local_port_range = 1024 65535`

### 4.4 HikariCP Connection Storm

```
서버 재시작 후 Connection Storm:

다수의 JVM Thread가 동시에 getConnection() 호출
    ↓
Pool이 비어있어 동시 다발적 TCP + TLS Handshake 발생
    ↓
DB 서버 max_connection 초과 또는 Connection 대기 timeout 폭발
    ↓ 결제 불가 상태 (CRITICAL)

대응: minimumIdle = maximumPoolSize (Pool Warm-up)
      initializationFailTimeout 설정으로 시작 시 검증
```

### 4.5 GC Stop-the-World와 TCP Timeout

JVM 고유의 위험 요소. GC pause 중 네트워크 처리가 완전 중단된다.

```
Full GC / G1 GC Mixed Pause (수백 ms ~ 수초)
    ↓
모든 JVM Thread Stop
    ↓
네트워크 I/O 처리 중단
    ↓
TCP ACK 미전송 → 상대방 측 TCP Timeout 카운트 시작
    ↓
RTO 초과 시 TCP 재전송 → 연결 끊김
    ↓ 결제 트랜잭션 롤백 필요
```

**모니터링:** GC Pause 시간과 TCP Retransmit 스파이크의 상관관계를 반드시 함께 추적.

### 4.6 SoftIRQ Saturation

고빈도 패킷 처리로 특정 CPU 코어의 SoftIRQ가 포화되면 JVM의 `epoll_wait()` 리턴이 지연된다.

```
확인:
    watch -n 1 'cat /proc/net/softnet_stat'
    # 2번째 열(dropped) 증가 = backlog 큐 포화
    # 3번째 열(time_squeeze) 증가 = ksoftirqd 처리 지연
```

### 4.7 Tail Latency 증가 원인 (JVM 특화)

| 원인 | 설명 | P99 영향 |
|------|------|---------|
| GC Pause | STW로 인한 전체 처리 중단 | 수백ms ~ 수초 |
| JIT 컴파일 | 초기 인터프리터 모드 실행 | 워밍업 전 P99 불안정 |
| Virtual Thread Pinning | synchronized + blocking I/O 조합 | Carrier Thread 고갈 |
| DNS 캐시 만료 | 재질의 중 요청 지연 | 수십ms |
| Connection Pool 대기 | Pool 고갈 시 connectionTimeout | 수초 |
| TLS Full Handshake | Session Reuse 실패 시 | 수십~수백ms |

---

## 5. SRE 관점 모니터링 지표

### 5.1 OS/커널 레벨 네트워크 지표

| 지표 | 수집 방법 | 임계치 (결제 서버) |
|------|---------|-----------------|
| Bandwidth Usage | `sar -n DEV 1` | NIC 대역폭의 70% 이하 유지 |
| PPS (Packets/sec) | `sar -n DEV 1` | NIC spec 기반 |
| TCP Retransmission Rate | `nstat -az TcpRetransSegs` | < 0.1% |
| RTT | `ping` / `tcprtt (bcc)` | < 1ms (동일 클러스터 내) |
| RTO | `ss -i` | 200ms ~ 1000ms |
| TIME_WAIT Count | `ss -s` | < 30,000 |
| SYN Backlog | `ss -lnt` | Recv-Q 지속 증가 감시 |
| Accept Queue | `ss -lnt` | Recv-Q > 0 지속 시 Alert |
| SoftIRQ Usage | `mpstat -P ALL` | `%soft` < 20% per core |
| RX/TX Drop | `ethtool -S eth0` | 0 유지 |
| NIC Error | `ip -s link` | 0 유지 |
| TCP Reset | `nstat -az TcpOutRsts` | 급증 시 Alert |

### 5.2 JVM 특화 네트워크 지표

| 지표 | 수집 방법 | 임계치 |
|------|---------|--------|
| HikariCP Active Connections | Micrometer `hikaricp.connections.active` | maximumPoolSize의 80% 이하 |
| HikariCP Pending Threads | Micrometer `hikaricp.connections.pending` | 0 유지 |
| HikariCP Connection Timeout Rate | Micrometer | 0 유지 |
| HTTP Client Error Rate (5xx) | Micrometer `http.client.requests` | < 0.01% |
| TLS Handshake Time | Micrometer / APM | < 50ms |
| Event Loop Lag (Netty) | Micrometer `netty.eventexecutor.tasks.pending` | < 100 |
| Virtual Thread Carrier Pinning | JVM Flight Recorder | 발생 0 목표 |
| GC Pause Time (STW) | JVM GC Log / Micrometer | < 200ms |
| JVM Thread Blocked | JVM MBean / Thread Dump | 0 유지 |
| DNS Resolution Time | Micrometer custom timer | < 5ms |

### 5.3 P95/P99 Latency 모니터링

```yaml
# Prometheus + Grafana 알림 예시
groups:
  - name: jvm-network-alerts
    rules:
      - alert: HighP99Latency
        expr: histogram_quantile(0.99, http_server_requests_seconds_bucket) > 0.5
        for: 1m
        annotations:
          summary: "P99 Latency {{ $value }}s 초과"

      - alert: HikariCPPoolExhaustion
        expr: hikaricp_connections_pending > 0
        for: 30s
        annotations:
          summary: "HikariCP 커넥션 대기 발생"

      - alert: TCPRetransmitHigh
        expr: rate(node_netstat_Tcp_RetransSegs[1m]) > 10
        for: 1m
        annotations:
          summary: "TCP 재전송 급증"
```

---

## 6. 장애 시나리오

### 시나리오 1: Connection Timeout — HikariCP Pool 고갈

**발생 상황:** 결제 트래픽 급증 → DB 응답 지연 → Pool 고갈

```
[트래픽 급증]
    ↓
DB 쿼리 응답 지연 (100ms → 2,000ms)
    ↓
HikariCP Active Connection 포화 (maximumPoolSize 도달)
    ↓
신규 getConnection() 호출이 connectionTimeout(30s)까지 대기
    ↓
Thread 스택 전체에 "Unable to acquire JDBC Connection" 예외 전파
    ↓
결제 API 500 에러 폭발
```

**대응:**
1. `connectionTimeout`을 3,000ms로 단축 → 빠른 실패(Fast Fail)
2. Circuit Breaker (Resilience4j) 적용
3. DB 쿼리 슬로우 로그 분석 → 인덱스 튜닝

### 시나리오 2: TIME_WAIT Explosion — 외부 PG API 호출

**발생 상황:** RestTemplate (Connection Pool 미사용) 기반 외부 결제망 호출

```
매 API 호출마다 신규 TCP 연결 생성/종료
    ↓
TIME_WAIT 소켓 누적 (기본 60초 대기)
    ↓
Ephemeral Port 고갈 (28,231개 초과)
    ↓
신규 TCP connect() 실패 → "Address already in use" 오류
    ↓
결제 처리 불가
```

**대응:**
1. WebClient / Apache HttpClient Connection Pool로 마이그레이션
2. `tcp_tw_reuse=1` 설정
3. 포트 범위 확장 및 `SO_REUSEADDR` 소켓 옵션 확인

### 시나리오 3: GC Pause → TCP Timeout → 결제 실패

**발생 상황:** JVM Heap 부족 → Full GC 발생

```
JVM Old Gen 포화 → Full GC (STW) 발생 (2~5초)
    ↓
모든 JVM Thread Suspend
    ↓
TCP ACK 전송 중단
    ↓
상대방(DB 서버 / Kafka) RTO 초과 → 연결 리셋(RST) 전송
    ↓
GC 완료 후 JVM 재개 → 소켓 오류 감지
    ↓
트랜잭션 롤백 + 결제 실패 기록
```

**대응:**
1. ZGC / Shenandoah 적용 (< 1ms STW 목표)
2. GC Pause 시간 모니터링 → Heap 튜닝
3. HikariCP `keepaliveTime` 설정으로 소켓 선제 유지

### 시나리오 4: Virtual Thread Pinning → Carrier Thread 고갈

**발생 상황:** JVM 21 Virtual Thread 환경에서 `synchronized` 블록 내 블로킹 I/O

```
Virtual Thread가 synchronized 블록 진입
    ↓
블로킹 I/O (JDBC, SSL) 발생
    ↓
Virtual Thread가 Carrier Thread(OS Thread)에 Pin (unmount 불가)
    ↓
Carrier Thread 수(= CPU Core 수)만큼 Pin 발생 시 전체 처리 중단
    ↓
새로운 Virtual Thread 스케줄 불가 → 결제 처리 큐 폭발
```

**대응:**
1. `synchronized` → `ReentrantLock` 교체
2. JVM 플래그: `-Djdk.tracePinnedThreads=full` 로 감지
3. JDBC 드라이버 Virtual Thread 호환성 확인 (PostgreSQL JDBC 42.7+)

### 시나리오 5: Istio/Envoy Sidecar Latency Explosion

**발생 상황:** 서비스 메쉬 환경에서 Envoy Sidecar 과부하

```
JVM App → 127.0.0.1:15001 (Envoy Outbound)
    ↓
Envoy: mTLS + Traffic Policy 처리 오버헤드
    ↓
Envoy Worker Thread 포화 → Request Queue 적체
    ↓
JVM 측 HTTP Client Timeout 발생
    ↓
결제 재시도 폭발 → Retry Storm
```

**대응:**
1. Envoy `concurrency` 설정 튜닝 (CPU Core 수 명시)
2. gRPC keepalive 파라미터 최적화
3. Istio `outlierDetection` 정책으로 비정상 인스턴스 즉시 제거

### 시나리오 6: MTU Mismatch — VXLAN 오버레이 환경

**발생 상황:** Kubernetes VXLAN CNI 환경에서 JVM 대용량 패킷 전송

```
JVM이 대용량 응답 전송 (정산 API: 수십 KB)
    ↓
VXLAN 헤더 추가 후 패킷 크기 > 물리 NIC MTU (1500 bytes)
    ↓
IP 단편화(Fragmentation) 또는 PMTUD Black Hole
    ↓
패킷 드롭 → TCP 재전송 반복
    ↓
결제 응답 지연 (수초 ~ 수십초)
```

**대응:**
1. Pod MTU = 물리 MTU - VXLAN 오버헤드 (50 bytes): `1450` 설정
2. `ip route show` 로 PMTU Discovery 동작 확인
3. Cilium eBPF 사용 시 VXLAN 없이 MTU 문제 회피

---

## 7. 튜닝 포인트

### 7.1 OS/커널 튜닝

```bash
# /etc/sysctl.conf 결제 서버 최적화 설정

# TCP 연결 버퍼
net.core.rmem_max = 134217728          # 128MB
net.core.wmem_max = 134217728
net.ipv4.tcp_rmem = 4096 87380 134217728
net.ipv4.tcp_wmem = 4096 65536 134217728

# 연결 수 및 포트
net.core.somaxconn = 65535
net.ipv4.tcp_max_syn_backlog = 65535
net.ipv4.ip_local_port_range = 1024 65535

# TIME_WAIT 최적화
net.ipv4.tcp_tw_reuse = 1
net.ipv4.tcp_fin_timeout = 15

# Keepalive
net.ipv4.tcp_keepalive_time = 60
net.ipv4.tcp_keepalive_intvl = 10
net.ipv4.tcp_keepalive_probes = 3

# 혼잡 제어 (저지연 우선)
net.ipv4.tcp_congestion_control = bbr
net.core.default_qdisc = fq

# SoftIRQ 처리량
net.core.netdev_max_backlog = 65536
net.core.netdev_budget = 600
```

### 7.2 JVM 시작 옵션 튜닝

```bash
# 결제 서버 JVM 옵션 예시

# GC: ZGC (저지연 우선, JVM 15+)
-XX:+UseZGC
-XX:MaxGCPauseMillis=10
-Xms4g -Xmx4g                          # Heap 고정 (GC 예측성 향상)

# Virtual Thread (JVM 21+)
--enable-preview                        # JVM 21 이전
-Djdk.tracePinnedThreads=full          # Pinning 감지

# 네트워크 DNS 캐시
-Dsun.net.inetaddr.ttl=5               # DNS TTL 5초 (K8s 환경)
-Dsun.net.inetaddr.negative.ttl=1

# TLS 성능 최적화
-Djsse.enableSNIExtension=true
-Djdk.tls.client.enableSessionTicketExtension=true  # TLS Session Resumption

# NIO Direct Buffer 추적
-XX:MaxDirectMemorySize=1g
-Djdk.nio.maxCachedBufferSize=262144

# JIT 최적화
-XX:+TieredCompilation
-XX:ReservedCodeCacheSize=256m
```

### 7.3 Spring Boot / Tomcat / Netty 튜닝

```yaml
# application.yml

server:
  # Tomcat NIO Connector
  tomcat:
    threads:
      max: 200          # Virtual Thread 사용 시 대폭 축소 가능
      min-spare: 20
    max-connections: 10000
    accept-count: 1000  # Backlog Queue
    connection-timeout: 3000
  
  # HTTP/2
  http2:
    enabled: true

spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 20
      connection-timeout: 3000
      keepalive-time: 30000
      max-lifetime: 600000
      idle-timeout: 300000

  # WebClient (Netty 기반)
  codec:
    max-in-memory-size: 10MB
```

### 7.4 Netty 튜닝 (결제 서버 직접 Netty 사용 시)

```java
// Netty ServerBootstrap 튜닝 예시
ServerBootstrap bootstrap = new ServerBootstrap()
    .group(bossGroup, workerGroup)
    .channel(NioServerSocketChannel.class)
    // TCP 레벨 옵션
    .option(ChannelOption.SO_BACKLOG, 65535)
    .option(ChannelOption.SO_REUSEADDR, true)
    // 채널(연결) 레벨 옵션
    .childOption(ChannelOption.TCP_NODELAY, true)      // Nagle 비활성화
    .childOption(ChannelOption.SO_KEEPALIVE, true)
    .childOption(ChannelOption.SO_RCVBUF, 87380)
    .childOption(ChannelOption.SO_SNDBUF, 65536)
    // Direct Buffer 풀 사용
    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

// Write Buffer Watermark (흐름 제어)
bootstrap.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
    new WriteBufferWaterMark(32 * 1024, 64 * 1024));
```

### 7.5 TLS Session Reuse

```java
// Spring Boot / Tomcat TLS Session Cache 설정
System.setProperty("javax.net.ssl.sessionCacheSize", "10000");
System.setProperty("javax.net.ssl.sessionTimeout", "86400"); // 24시간

// Netty TLS Session Resumption (BoringSSL 사용)
SslContextBuilder.forServer(certChain, privateKey)
    .sslProvider(SslProvider.OPENSSL)   // netty-tcnative
    .sessionCacheSize(10000)
    .sessionTimeout(86400)
    .build();
```

### 7.6 Kubernetes CNI 최적화

```yaml
# Cilium ConfigMap (eBPF 최적화)
apiVersion: v1
kind: ConfigMap
metadata:
  name: cilium-config
data:
  kube-proxy-replacement: "strict"      # kube-proxy 완전 대체
  enable-bpf-masquerade: "true"
  enable-host-reachable-services: "true"
  bpf-lb-sock: "true"                  # Socket-level LB (추가 홉 제거)
  MTU: "1450"                          # VXLAN 오버헤드 고려
```

---

## 8. 관련 Linux 명령어 및 분석 도구

### 8.1 소켓 및 연결 상태 분석

```bash
# 소켓 상태 요약
ss -s

# JVM 프로세스의 모든 TCP 연결 상세 (PID 포함)
ss -antp | grep java

# TCP 연결 상태별 카운트
ss -ant | awk '{print $1}' | sort | uniq -c | sort -rn

# HikariCP 연결 확인 (DB 포트 기준)
ss -antp | grep :5432

# TIME_WAIT 소켓 수 실시간 모니터링
watch -n 1 'ss -s | grep TIME-WAIT'

# 소켓 상세 정보 (RTT, 재전송 포함)
ss -i -t dst :443
```

### 8.2 패킷 레벨 분석

```bash
# JVM 프로세스의 네트워크 트래픽 캡처
tcpdump -i any -w /tmp/jvm-capture.pcap port 8080 or port 5432

# TLS Handshake 확인
tcpdump -i eth0 -nn 'tcp port 443 and (tcp[13] & 0x02 != 0)'

# TCP 재전송 패킷 필터
tcpdump -i any 'tcp[tcpflags] & tcp-syn != 0 and tcp[tcpflags] & tcp-ack != 0'

# tshark로 HTTP/2 스트림 분석
tshark -i any -d tcp.port==8080,http2 -T fields \
  -e frame.time -e http2.streamid -e http2.flags
```

### 8.3 커널 네트워크 통계

```bash
# TCP 통계 전체 (재전송, Reset 포함)
nstat -az | grep -E 'Tcp|Udp'

# 1초 간격 네트워크 통계
sar -n DEV 1 10

# SoftIRQ CPU 사용률
mpstat -P ALL 1 | grep -E 'CPU|soft'

# SoftIRQ 처리 큐 상태 (dropped 열 주목)
cat /proc/net/softnet_stat

# 커널 네트워크 파라미터 확인
sysctl -a | grep -E 'net.ipv4.tcp|net.core'
```

### 8.4 NIC 및 인터럽트 분석

```bash
# NIC 상세 통계 (drop, error 포함)
ethtool -S eth0 | grep -E 'drop|error|miss'

# 인터럽트 분산 확인
cat /proc/interrupts | grep eth0

# NIC Queue 구성
ethtool -l eth0

# NIC Offload 기능 확인
ethtool -k eth0
```

### 8.5 JVM 특화 분석 도구

```bash
# JVM Thread Dump (네트워크 대기 스레드 확인)
kill -3 <JVM_PID>
# 또는
jstack <JVM_PID> | grep -A 10 "BLOCKED\|WAITING"

# JVM 플라이트 레코더 (Virtual Thread Pinning, TLS 오버헤드)
jcmd <JVM_PID> JFR.start duration=60s filename=/tmp/jvm-recording.jfr
jfr print --events jdk.VirtualThreadPinned /tmp/jvm-recording.jfr

# JVM 소켓 사용 lsof
lsof -p <JVM_PID> -i TCP | head -50

# HikariCP 메트릭 실시간 확인 (Actuator)
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
curl http://localhost:8080/actuator/metrics/hikaricp.connections.pending
```

### 8.6 eBPF / BCC Tools (고급 분석)

```bash
# TCP RTT 분포 분석 (tcprtt)
tcprtt -m -T 1 -p 8080

# TCP 연결 이벤트 추적 (tcpconnect)
tcpconnect -p <JVM_PID>

# TCP 재전송 추적 (tcpretrans)
tcpretrans

# 소켓 레벨 지연 추적 (sockstat)
sockstat

# epoll 이벤트 추적 (JVM NIO Selector 내부)
trace 'sys_epoll_wait(int epfd, struct epoll_event *events, int maxevents, int timeout)'

# JVM 네트워크 I/O Flamegraph 생성
perf record -g -p <JVM_PID> sleep 30
perf script | stackcollapse-perf.pl | flamegraph.pl > /tmp/jvm-network-flame.svg
```

### 8.7 분산 환경 (Kubernetes / Istio) 분석

```bash
# Istio Envoy 사이드카 통계
istioctl proxy-config listeners <pod-name>
istioctl proxy-config clusters <pod-name>
kubectl exec -it <pod-name> -c istio-proxy -- \
  curl localhost:15000/stats | grep -E 'upstream_rq|retry|timeout'

# Pod 네트워크 Namespace에서 tcpdump
kubectl exec -it <pod-name> -- tcpdump -i eth0 -nn port 8080

# Cilium 연결 상태 확인
cilium endpoint list
cilium monitor --type drop

# CoreDNS 질의 지연 분석
kubectl logs -n kube-system -l k8s-app=kube-dns | grep -E 'SERVFAIL|timeout'
```

### 8.8 분석 도구 요약

| 분석 목적 | 도구 | 비고 |
|----------|------|------|
| 소켓 상태 | `ss`, `netstat` | `ss` 권장 (최신) |
| 패킷 캡처 | `tcpdump`, `tshark`, `Wireshark` | - |
| 대역폭 모니터링 | `iftop`, `sar -n DEV` | - |
| 성능 벤치마크 | `iperf3` | 네트워크 최대 처리량 측정 |
| 커널 통계 | `nstat`, `/proc/net/*` | - |
| SoftIRQ | `mpstat`, `softnet_stat` | - |
| NIC 상태 | `ethtool` | - |
| 연결 추적 | `conntrack -L` | NAT/방화벽 환경 |
| eBPF 추적 | `tcprtt`, `tcpretrans`, `tcpconnect` | bcc-tools 패키지 |
| JVM 전용 | `jstack`, `jcmd`, JFR, Micrometer | 결제 서버 필수 |
| APM | Datadog APM, Elastic APM, Pinpoint | 분산 추적 |
| 서비스 메쉬 | `istioctl`, Kiali, Jaeger | Istio 환경 |

---

## 참고: 결제 서버 네트워크 체크리스트 (SRE Daily)

```
[ ] TCP Retransmission Rate < 0.1%
[ ] HikariCP Pending = 0
[ ] TIME_WAIT 소켓 수 < 30,000
[ ] SoftIRQ per core < 20%
[ ] RX/TX Drop = 0
[ ] GC STW Pause < 200ms (ZGC 목표: < 10ms)
[ ] P99 Latency < 500ms
[ ] DNS 해석 시간 < 5ms
[ ] TLS Handshake Time < 50ms
[ ] Virtual Thread Pinning 이벤트 = 0
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*