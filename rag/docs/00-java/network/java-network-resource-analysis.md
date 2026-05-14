# Java FinTech 결제 시스템 — Network 자원 E2E 분석

> 정독: 0회

> 관점: SRE / Platform Engineering / Payment Reliability  </br>
> 범위: Hardware NIC → OS Kernel → JVM Runtime → Framework → Application  </br>
> 목적: Java 기반 결제 시스템의 네트워크 자원 소비 메커니즘, 병목 지점, 장애 패턴, 튜닝 전략을 E2E 관점에서 정리한다.  </br>

---

## 목차

1. [물리/가상 Network 및 NIC 스펙 확인 지표](#1-물리가상-network-및-nic-스펙-확인-지표)
2. [Java 결제 시스템 Network 실행 흐름 (E2E)](#2-java-결제-시스템-network-실행-흐름-e2e)
3. [Java 스택 Network 사용 메커니즘 및 실행 모델](#3-java-스택-network-사용-메커니즘-및-실행-모델)
4. [Network 병목 및 위험 발생 지점](#4-network-병목-및-위험-발생-지점)
5. [SRE 관점 모니터링 지표](#5-sre-관점-모니터링-지표)
6. [장애 시나리오](#6-장애-시나리오)
7. [튜닝 포인트](#7-튜닝-포인트)
8. [관련 Linux 명령어 및 분석 도구](#8-관련-linux-명령어-및-분석-도구)

---

## 1. 물리/가상 Network 및 NIC 스펙 확인 지표

### 1.1 NIC / vNIC / SR-IOV 구조

```text
물리 서버 (Bare Metal):
  Physical NIC (e.g., Intel X710, Mellanox ConnectX-5)
    → PCI-e Bus → CPU NUMA Node
    → DMA Engine: 패킷 수신 시 CPU 개입 없이 Kernel Ring Buffer에 직접 기록

가상화 환경 (VM / Cloud):
  VM vNIC (virtio-net)
    → Hypervisor Virtual Switch (OVS, SR-IOV)
    → Physical NIC
  SR-IOV(Single Root I/O Virtualization):
    Physical NIC → VF(Virtual Function) 다수 생성
    → VM이 VF를 직접 점유 → Hypervisor 우회 → 지연 최소화
    → 핀테크 결제 시스템에서 latency SLA가 엄격한 경우 SR-IOV 사용

Kubernetes / Container:
  Pod eth0 (veth pair)
    → CNI (Calico, Cilium, Flannel)
    → Linux Bridge / IPVLAN / MACVLAN
    → Host NIC
  Overlay Network (VXLAN / Geneve):
    추가 헤더(50 bytes) → 유효 MTU 감소 → MSS 조정 필요
```

| 환경 | NIC 유형 | 주요 확인 항목 |
|------|---------|--------------|
| Bare Metal | Physical NIC | Speed, Duplex, Ring Buffer, IRQ affinity |
| VM | virtio-net / vmxnet3 | Paravirtualization 오버헤드, Steal Time |
| SR-IOV | VF (Virtual Function) | VF 수, Queue 수, QoS 설정 |
| Kubernetes | veth + CNI | CNI Plugin 종류, Overlay 오버헤드, MTU 설정 |

### 1.2 Bandwidth / MTU / Queue / Ring Buffer 구조

```text
NIC Ring Buffer (하드웨어 수신 큐):
  NIC 수신 패킷 → DMA → Kernel Ring Buffer (sk_buff 배열)
  Ring Buffer 고갈 → NIC Packet Drop (ethtool -S eth0 | grep drop)

RX Queue 구조:
  NIC RX Queue → IRQ → CPU 코어 → SoftIRQ(ksoftirqd) → Netdev Backlog Queue
  → TCP/IP Stack → Socket Receive Buffer

TX Queue 구조:
  Socket Send Buffer → Kernel TX Queue (qdisc)
  → NIC TX Ring Buffer → DMA → 네트워크 전송
```

| 항목 | 기본값 | 핀테크 권장 | 확인 명령어 |
|------|--------|-----------|-----------|
| NIC Ring Buffer (RX) | 256~512 | 4096 이상 | `ethtool -g eth0` |
| MTU | 1500 bytes | Overlay: 1450 / Jumbo: 9000 | `ip link show eth0` |
| TCP Receive Buffer | 4~87KB | 4~16MB | `sysctl net.ipv4.tcp_rmem` |
| TCP Send Buffer | 4~87KB | 4~16MB | `sysctl net.ipv4.tcp_wmem` |
| netdev_max_backlog | 1000 | 10000~30000 | `sysctl net.core.netdev_max_backlog` |

### 1.3 RSS / RPS / XPS / IRQ Affinity

```text
RSS (Receive Side Scaling):
  NIC 하드웨어가 RX Queue를 여러 개 생성
  각 RX Queue를 다른 CPU 코어에 바인딩 (IRQ affinity)
  → 패킷 수신 부하를 여러 코어에 분산
  → Java 결제 서버에서 NIC 인터럽트가 단일 CPU에 집중되면 SoftIRQ 포화 발생

RPS (Receive Packet Steering):
  소프트웨어 RSS (NIC가 RSS를 지원하지 않을 때)
  /sys/class/net/eth0/queues/rx-0/rps_cpus 설정

XPS (Transmit Packet Steering):
  TX 큐와 CPU 코어 매핑
  /sys/class/net/eth0/queues/tx-0/xps_cpus 설정

IRQ Affinity:
  /proc/irq/<IRQ번호>/smp_affinity_list
  특정 CPU 코어에 NIC IRQ 고정 → NUMA-aware 설정 권장
```

**SRE 확인 명령어:**
```bash
# IRQ 분산 현황
cat /proc/interrupts | grep eth0
# RSS 큐 수 확인
ethtool -l eth0
# SoftIRQ per CPU
mpstat -P ALL 1 | grep -i soft
```

### 1.4 TCP/IP Stack 및 Virtual/Container Network 구조

```text
Linux TCP/IP Stack (패킷 수신 경로):
  NIC → DMA → Ring Buffer (sk_buff)
    → IRQ → ksoftirqd (SoftIRQ NET_RX)
    → GRO(Generic Receive Offload): 소형 패킷 병합
    → Netfilter (iptables/nftables): 방화벽, NAT, conntrack
    → TCP/IP Stack: 재조립, ACK 생성, 혼잡 제어
    → Socket Receive Buffer
    → epoll_wait() 깨움 → Java EventLoop / Selector

Kubernetes 패킷 경로 (추가 오버헤드):
  Pod eth0 (veth)
    → iptables DNAT (kube-proxy) 또는 eBPF (Cilium)
    → 노드 간: VXLAN/Geneve Encapsulation/Decapsulation
    → 상대 Node NIC → 상대 Pod veth

Service Mesh (Istio/Envoy Sidecar):
  Pod outbound traffic
    → iptables redirect (port 15001) → Envoy Sidecar → 목적지
  수신 traffic
    → iptables redirect (port 15006) → Envoy Sidecar → App
  추가 지연: Envoy 처리 시간 + 두 번의 localhost 통신
```

---

## 2. Java 결제 시스템 Network 실행 흐름 (E2E)

### 2.1 Spring MVC / Tomcat (Thread-per-request) 전체 흐름

```text
[외부 클라이언트 / PG사 → 결제 API 서버]

① 패킷 수신 (Hardware → Kernel)
  NIC 패킷 수신
    → DMA → Kernel Ring Buffer (sk_buff)
    → IRQ 발생 → CPU 코어 깨움
    → SoftIRQ (NET_RX) → ksoftirqd
    → GRO → TCP/IP Stack 처리
    → TCP Receive Buffer (Socket Buffer) 적재
    → epoll ready list에 FD 추가

② 연결 수락 (Kernel → Tomcat)
  epoll_wait() 반환 → Tomcat Acceptor Thread 깨움
  accept() syscall → 새 Socket FD 생성
  → Worker Thread Pool에서 Thread 할당 (BIO/NIO 방식)

③ 요청 처리 (Java Runtime)
  Worker Thread:
    read() syscall → Kernel TCP Buffer → User Buffer (JVM Heap)
    → HTTP 파싱 (Tomcat Coyote)
    → DispatcherServlet → Controller → Service
    → DB 호출: getConnection(HikariCP) → JDBC Socket
    → 외부 PG API 호출: HttpClient / RestTemplate

④ 응답 전송 (Java → Kernel → NIC)
  write() syscall → User Buffer → Kernel TCP Send Buffer
  → TCP Segmentation (TSO if enabled) → NIC TX Queue
  → DMA → 네트워크 전송
```

### 2.2 WebFlux / Netty (Event Loop, Non-blocking) 전체 흐름

```text
[외부 클라이언트 → WebFlux 결제 API]

① 패킷 수신 (Hardware → Kernel) [동일]

② EventLoop 처리
  Netty Boss EventLoop: accept() → Child EventLoop에 Channel 등록
  Worker EventLoop Thread (CPU 코어 수 × 2):
    epoll_wait() 반환 (Edge-triggered)
    → ChannelPipeline 처리
      → HttpServerCodec (HTTP 디코딩)
      → HttpObjectAggregator
      → WebFlux HttpHandler

③ Reactive 처리 (Mono/Flux 체인)
  Mono<ServerResponse> = handler.handle(request)
    → Schedulers.boundedElastic(): Blocking 작업 (JDBC 등) 격리
    → Schedulers.parallel(): CPU 연산
    → 외부 PG 호출: WebClient (Non-blocking HTTP)
      → Reactor Netty HTTP Client
      → EventLoop Thread에서 비동기 소켓 I/O

④ 응답 전송
  Mono 체인 완료 → ServerResponse 직렬화
  → Netty write() → Kernel Send Buffer → NIC
```

### 2.3 Virtual Thread (JDK 21+) 흐름

```text
[HTTP 요청 → Virtual Thread 기반 처리]

Virtual Thread 생성 (수십만 개 가능):
  요청마다 Virtual Thread 생성 (Heap 저장, 수 KB)
    → ForkJoinPool Carrier Thread (OS Thread, CPU 코어 수)

Blocking I/O 처리:
  Virtual Thread가 SocketInputStream.read() 호출
    → JVM이 Non-blocking으로 변환 (java.net 패키지 재구현)
    → Continuation을 Heap에 직렬화 (unmount)
    → Carrier Thread는 다른 Virtual Thread 실행
    → epoll/io_uring 이벤트 완료 시 Continuation 복원 → 재스케줄

Pinning 위험:
  synchronized 블록 + JDBC 호출
    → Carrier Thread 고정 → Virtual Thread 장점 상실
  해결: ReentrantLock 교체, JDBC Driver 교체
```

### 2.4 Java에서 외부 PG/카드사 API 호출 흐름

```text
결제 서버 → 외부 PG API 호출

RestTemplate / HttpClient:
  connect() syscall → TCP 3-way Handshake
    → SYN → SYN-ACK → ACK
  TLS Handshake (상호 인증):
    → ClientHello → ServerHello → Certificate
    → Key Exchange → Finished
    → 약 2 RTT (TLS 1.2) 또는 1 RTT (TLS 1.3)
  HTTP Request 전송:
    write() → Kernel Send Buffer → NIC
  응답 대기:
    epoll_wait() / read() Blocking
  응답 수신 → JSON 파싱 → 결제 처리

비용 항목:
  - TCP 연결 생성: ~1 RTT (Connection Pool로 재사용)
  - TLS Handshake: 1~2 RTT (Session Resumption으로 단축)
  - 외부 API 처리 시간: PG사 SLA 의존
  - 직렬화/역직렬화: Jackson CPU 비용
```

---

## 3. Java 스택 Network 사용 메커니즘 및 실행 모델

### 3.1 Connection Pool (HikariCP)

```text
HikariCP 내부 동작:
  getConnection()
    → Thread-local Fast Path (ConcurrentBag)
    → 공유 Pool 탐색
    → 없으면 connectionTimeout 동안 대기
    → 초과 시 SQLTimeoutException

TCP 수준:
  Pool의 Connection = TCP Keep-alive 소켓
  idle 연결: TCP_KEEPIDLE(7200초 기본) → DB 방화벽이 끊을 수 있음
  → keepaliveTime 설정으로 주기적 유효성 확인 필요

Connection Validation:
  connectionTestQuery / isValid() → DB로 ping SQL 전송
  → 추가 TCP Round-trip 발생
```

| 설정 | 역할 | 핀테크 권장 |
|------|------|-----------|
| `maximumPoolSize` | 최대 연결 수 | CPU 코어 수 × 2 ~ 3 |
| `connectionTimeout` | 연결 획득 대기 시간 | 3000ms 이하 |
| `keepaliveTime` | 유휴 연결 유효성 확인 주기 | 60000ms |
| `maxLifetime` | 연결 최대 수명 | 1800000ms (30분) |

### 3.2 HTTP/1.1 vs HTTP/2 vs gRPC

```text
HTTP/1.1 (RestTemplate, URLConnection):
  요청마다 TCP 연결 재사용 (Keep-Alive)
  Head-of-Line Blocking: 응답 도착 전 다음 요청 불가
  → 결제 처리량 제한 요소

HTTP/2 (WebClient, OkHttp):
  단일 TCP 연결에서 다중 스트림 (Multiplexing)
  Header Compression (HPACK)
  → 외부 PG API와 HTTP/2 협상 필요
  → TLS 위에서 동작 (ALPN 협상: h2)

gRPC (Protobuf + HTTP/2):
  내부 서비스 간 통신 (결제 MSA)
  Bidirectional Streaming 지원
  Connection Reuse: 하나의 TCP 연결로 다중 RPC
  → Envoy/Istio 사이드카와 통합 용이
  → Binary 직렬화 → 네트워크 대역폭 절감

Java gRPC 구현체: grpc-java (Netty 기반)
  NettyChannelBuilder → EventLoop 기반
  → Blocking Stub (Platform Thread) / Async Stub (Callback) / Reactive (grpc-reactor)
```

### 3.3 TLS Handshake 및 세션 재사용

```text
Java TLS 구현: JSSE (Java Secure Socket Extension)
  SunJSSE: 순수 Java 구현
  Conscrypt: BoringSSL (Google, Android) JNI 바인딩 → 성능 우수

TLS 1.2 (2 RTT):
  ClientHello → ServerHello + Certificate → ClientKeyExchange → Finished

TLS 1.3 (1 RTT, 0-RTT 재연결 가능):
  ClientHello → ServerHello + Finished → Application Data
  → 결제 API에서 TLS 1.3 강제 권장

Session Resumption:
  TLS Session ID / Session Ticket 활용
  → Handshake RTT 절감
  → HttpClient: SSLSessionContext 크기 설정
  → Netty: SslContextBuilder에서 세션 캐시 설정

CPU 부하:
  TLS 1.2 RSA 4096: ~5ms / 연결 (CPU 집약적)
  TLS 1.3 ECDHE: ~1ms / 연결
  AES-NI CPU 명령어: JVM이 자동 활용 (JIT Intrinsics)
  → 고TPS 환경에서 TLS CPU 오버헤드 측정 필요
```

### 3.4 epoll / io_uring (Java 연결)

```text
Java NIO Selector (epoll 래핑):
  Selector.open() → epoll_create1() syscall
  SelectionKey 등록 → epoll_ctl()
  selector.select() → epoll_wait()
  → 단일 Thread에서 다수 Channel 감시

Netty의 epoll 최적화:
  NioEventLoop: Java NIO Selector (epoll) 사용
  EpollEventLoop (Linux): JNI를 통해 epoll 직접 호출
    → Selector 오버헤드 제거, ET(Edge-triggered) 사용
    → io.netty:netty-transport-native-epoll 의존성 추가

io_uring (Java, 실험적):
  JDK 19+: java.nio가 일부 io_uring 활용 (Linux)
  Netty io_uring: netty-incubator-transport-native-io_uring
    → SQ/CQ Ring을 통한 배치 I/O → syscall 횟수 감소
    → 핀테크 고TPS 환경에서 검토 가치 있음
    → 커널 5.10 이상 안정적

확인:
  strace -e epoll_wait,epoll_ctl -p <PID>
  JFR SocketRead/Write 이벤트로 I/O 지연 측정
```

### 3.5 TCP Keepalive / Timeout / Retry

```text
TCP Keepalive (Java):
  java.net.Socket.setKeepAlive(true)
  → OS 수준 TCP_KEEPIDLE / TCP_KEEPINTVL / TCP_KEEPCNT 설정 의존
  → JVM이 OS 값을 상속 (개별 제어 어려움)
  → HikariCP keepaliveTime으로 애플리케이션 수준 관리 권장

HttpClient (JDK 11+) Timeout:
  connectTimeout: TCP 연결 타임아웃
  HttpRequest.timeout(): 요청 전체 타임아웃

WebClient (Reactor Netty) Timeout:
  .responseTimeout(Duration)
  HttpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ...)

Retry 설계 (결제 시스템):
  멱등성 보장된 API만 재시도 (GET, POST with idempotency key)
  Exponential Backoff + Jitter:
    delay = min(cap, base * 2^attempt) + random(0, jitter)
  Circuit Breaker (Resilience4j)와 연동
    → Open 상태에서 재시도 차단 → Retry Storm 방지

RestTemplate Retry:
  Spring Retry @Retryable 또는 RetryTemplate
  WebClient: Flux.retryWhen(Retry.backoff(...))
```

### 3.6 Nagle Algorithm / TCP 버퍼 / Backpressure

```text
Nagle Algorithm:
  TCP_NODELAY 미설정 시 소형 패킷을 묶어서 전송
  → 결제 API 응답 지연 유발 가능
  Java: socket.setTcpNoDelay(true)
  Netty: ChannelOption.TCP_NODELAY, true
  → 결제 시스템: 항상 TCP_NODELAY 활성화 권장

Socket Buffer:
  SO_SNDBUF / SO_RCVBUF:
    Java: socket.setSendBufferSize() / setReceiveBufferSize()
    Netty: ChannelOption.SO_SNDBUF / SO_RCVBUF
  BDP (Bandwidth-Delay Product):
    최적 버퍼 = Bandwidth × RTT
    → 고RTT 환경(해외 PG API)에서 버퍼 부족 시 처리량 제한

Backpressure (WebFlux/Reactor):
  Publisher가 Consumer 처리 속도 초과 시
    → request(n)으로 처리 가능 개수 제어
    → onBackpressureBuffer(): 메모리 버퍼링
    → onBackpressureDrop(): 초과 드롭
  TCP 수준 Backpressure:
    Receive Buffer 가득 참 → TCP Window Size 0 전송 (Zero Window)
    → 송신 측 대기 → RTT 증가
```

### 3.7 Kafka Producer / Consumer 네트워크 동작

```text
Kafka Producer (결제 이벤트 발행):
  RecordAccumulator: 배치 버퍼 (linger.ms, batch.size)
  Sender Thread → NetworkClient → Selector (NIO epoll)
    → TCP Socket → Kafka Broker

네트워크 비용:
  batch.size: 큰 배치 → 높은 처리량, 낮은 TPS 응답성
  linger.ms: 대기 시간 (결제 이벤트: 0~5ms 권장)
  compression.type: lz4/snappy → CPU↑, 네트워크↓
  acks=all: ISR 전체 복제 후 ACK → 지연 증가

Kafka Consumer (결제 후처리):
  poll(timeout): Fetch 요청 → Broker TCP 연결
  max.poll.records: 한 번에 처리할 레코드 수
  fetch.min.bytes / fetch.max.wait.ms: 배치 크기 제어
  Heartbeat Thread: 별도 Thread → session.timeout.ms 유지
  → GC STW > session.timeout.ms → Rebalance 트리거
```

### 3.8 DNS Resolution

```text
Java DNS 조회:
  InetAddress.getByName() → OS DNS 스택 → /etc/resolv.conf
  JVM DNS Cache: networkaddress.cache.ttl (기본 30초)
    → 너무 짧으면 DNS 재조회 빈도 증가 (외부 PG IP 변경 대응)
    → 너무 길면 IP 변경 반영 지연

Kubernetes DNS 문제:
  ndots:5 기본 설정 → 5개 점이 없는 도메인은 search domain 탐색
    → pg-api.payments.svc.cluster.local, pg-api.payments.svc, ...
    → 실패 후 원래 도메인 조회 → 최대 5회 DNS 쿼리 발생
  해결: ndots:2 설정 또는 FQDN(후미 점) 사용
  CoreDNS 병목: 고TPS 환경에서 CoreDNS Pod CPU 포화 가능

확인:
  strace -e openat,connect -p <PID> | grep resolv
  tcpdump port 53
```

---

## 4. Network 병목 및 위험 발생 지점

### 4.1 NIC Saturation

```text
원인:
  NIC Bandwidth 포화 (10G NIC에서 트래픽 10Gbps 초과)
  RX/TX Queue 수 부족 → 특정 CPU에 IRQ 집중

결제 영향:
  패킷 수신 지연 → 결제 요청 처리 지연
  NIC Drop → TCP 재전송 → P99 Latency 급증

확인:
  ethtool -S eth0 | grep -i drop
  sar -n DEV 1 | grep eth0
  iftop, nethogs
```

### 4.2 Socket Buffer Exhaustion

```text
Receive Buffer 고갈:
  Java 애플리케이션이 read()를 늦게 호출
    → Socket Receive Buffer 가득 참
    → TCP Window Size 0 광고 → 송신 측 중단
    → 결제 응답 지연

Send Buffer 고갈:
  외부 API가 응답을 느리게 읽음
    → Write 불가 → Java Thread Block
    → Worker Thread 고갈

확인:
  ss -ant | awk '{print $2, $3}' | sort | uniq -c
  ss -i | grep -A1 "ESTAB" | grep rcv
```

### 4.3 SYN Backlog / Accept Queue Saturation

```text
결제 트래픽 급증 (이벤트, 세일) 시:
  SYN Queue (tcp_max_syn_backlog) 고갈
    → SYN Drop → 클라이언트 재전송(3초 간격)
    → 결제 요청 최초 연결 지연

  Accept Queue (somaxconn, Tomcat backlog) 고갈
    → 3-way Handshake 완료 후 accept() 대기 중 Drop
    → 연결 유실 → 결제 실패

Java/Tomcat 설정:
  server.tomcat.accept-count (= listen backlog)
  server.tomcat.max-connections

확인:
  ss -lnt  # Recv-Q: Accept Queue 대기 수
  netstat -s | grep -i "listen"
  nstat TcpExtListenDrops
```

### 4.4 Ephemeral Port Exhaustion

```text
원인:
  결제 서버 → 외부 PG API: 연결마다 로컬 포트 사용
  TIME_WAIT 상태 연결이 누적 (기본 60초 유지)
    → 로컬 포트 28,000개 기본 → 고TPS에서 고갈

증상:
  connect() 실패 → EADDRNOTAVAIL 에러
  → Java: java.net.ConnectException: Cannot assign requested address

확인:
  ss -s | grep TIME-WAIT
  cat /proc/sys/net/ipv4/ip_local_port_range
  ss -ant | grep TIME-WAIT | wc -l

해결:
  ip_local_port_range = 10000 65000 (확장)
  tcp_tw_reuse = 1 (TIME_WAIT 소켓 재사용)
  Connection Pool 사용 (신규 연결 최소화)
```

### 4.5 SoftIRQ Saturation

```text
원인:
  고PPS(Packets Per Second) 트래픽
  NIC 인터럽트가 단일 CPU에 집중

증상:
  mpstat에서 특정 코어 %soft 30% 이상
  Java Worker Thread CPU 부족 → 결제 처리 지연

확인:
  mpstat -P ALL 1
  cat /proc/net/softnet_stat  # 2번째 열: dropped
  watch -n1 "cat /proc/softirqs | grep -E 'NET_RX|NET_TX'"
```

### 4.6 TLS CPU Overhead

```text
원인:
  고TPS 결제 환경에서 TLS Handshake CPU 비용 누적
  RSA 4096 비트: 단일 Handshake ~2~5ms CPU
  Connection Pool 없는 경우 요청마다 Handshake

증상:
  CPU 사용률 증가
  TLS Handshake 시간 P99 증가
  GC 압박 (ByteBuffer 할당)

해결:
  TLS 1.3 전환 (ECDHE 기반, AES-NI 활용)
  Conscrypt 라이브러리 (BoringSSL JNI)
  Session Resumption 활성화
  Connection Pool로 TLS 연결 재사용
  AES-NI 활성 확인: grep aes /proc/cpuinfo
```

### 4.7 Tail Latency 증가 원인

```text
Java 결제 시스템에서 Tail Latency(P99/P999) 증가 원인:

1. GC STW
   → 수십 ms GC 동안 모든 요청 처리 중단
   → Kafka Consumer session timeout 초과 → Rebalance
   → 외부 PG API read timeout 초과

2. TCP Retransmission
   → Timeout 기반 재전송: 200ms~3초 지연
   → Fast Retransmit: 3 dup-ACK 기반, 더 빠름

3. Virtual Thread Pinning
   → synchronized + Blocking → Carrier Thread 고정
   → 결제 처리 대기열 누적

4. DNS 재조회 지연
   → Kubernetes ndots 설정으로 최대 5회 DNS 쿼리

5. cgroup CPU Throttling
   → Kubernetes CPU limit 초과 → Thread Runnable이나 CPU 미할당
   → P99 Latency 직접 증가

6. Context Switch 누적
   → Worker Thread 수 > CPU 코어 수 → Runqueue 지연
```

---

## 5. SRE 관점 모니터링 지표

### 5.1 핵심 지표 전체 목록

| 카테고리 | 지표 | 도구 | 임계 기준 |
|--------|------|------|---------|
| **결제** | 결제 성공률, P95/P99 Latency | Prometheus + Grafana | P99 < 2초, 성공률 > 99.9% |
| **Bandwidth** | NIC RX/TX throughput | sar -n DEV, ifstat | NIC 대역폭 70% 이하 |
| **PPS** | Packets Per Second | sar -n DEV, ethtool -S | NIC 성능 한계 이하 |
| **TCP Retransmission** | TcpRetransSegs | nstat, /proc/net/snmp | 증가 추세 시 즉시 조사 |
| **RTT** | 외부 PG API RTT | Trace Span, tcpping | SLA 기준 (통상 200ms 이하) |
| **Socket State** | ESTABLISHED, TIME_WAIT, CLOSE_WAIT | ss -s | TIME_WAIT 급증, CLOSE_WAIT 누적 |
| **Connection Count** | TCP 연결 수 | ss -s | 증가 추세 모니터링 |
| **SYN Backlog** | SynDrops, SYN Queue | nstat TcpExtTCPReqQFullDrop | 0 이상 시 즉시 조사 |
| **Accept Queue** | ListenDrops | nstat TcpExtListenDrops | 0 이상 시 즉시 조사 |
| **SoftIRQ** | %soft per CPU | mpstat -P ALL | 단일 코어 20% 이상 |
| **RX/TX Drop** | RX-DRP, TX-DRP | ip -s link, ethtool -S | 증가 추세 시 Ring Buffer 조정 |
| **NIC Error** | RX-ERR, TX-ERR | ip -s link | 0 유지 목표 |
| **TCP Reset** | TcpOutRsts | nstat | 급증 시 연결 강제 종료 원인 조사 |
| **TLS Handshake** | Handshake 시간 | OTel Span, JFR SocketWrite | TLS 1.3: 1 RTT 이하 |
| **Event Loop Delay** | EventLoop busy time | Netty eventloop 메트릭 | 1ms 이상 지속 시 경고 |
| **Ephemeral Port** | TIME_WAIT 수 | ss -s | ip_local_port_range 범위 대비 70% |
| **PSI** | CPU/Memory/IO Pressure | /proc/pressure/* | some avg10 > 10% |

### 5.2 Java / JVM 특화 네트워크 지표

| 지표 | 수집 방법 | 의미 |
|------|---------|------|
| `hikaricp_active_connections` | Micrometer | DB 연결 사용량 |
| `hikaricp_pending_threads` | Micrometer | DB 연결 대기 Thread 수 |
| `hikaricp_connection_timeout_total` | Micrometer | 연결 획득 실패 (결제 실패 직결) |
| `http_client_requests_seconds` | Micrometer | 외부 API 호출 지연 분포 |
| `kafka_consumer_lag` | JMX / Micrometer | 미처리 메시지 수 |
| JFR `SocketRead` | Java Flight Recorder | Socket 읽기 지연 시간 |
| JFR `SocketWrite` | Java Flight Recorder | Socket 쓰기 지연 시간 |
| JFR `VirtualThreadPinned` | Java Flight Recorder | Virtual Thread Pinning 발생 |
| OTel Span `db.query` | OpenTelemetry | DB 쿼리 실제 소요 시간 |
| OTel Span `http.request` | OpenTelemetry | 외부 HTTP 호출 전체 시간 |

### 5.3 Prometheus 쿼리 예시

```promql
# 결제 P99 Latency
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket{uri="/api/payment"}[5m]))

# TCP Retransmission 증가율
rate(node_netstat_Tcp_RetransSegs[5m])

# DB Connection Pool 포화도
hikaricp_active_connections / hikaricp_size_max

# SoftIRQ 비율 (코어별)
rate(node_softnet_dropped_total[1m])

# TIME_WAIT 수
node_sockstat_TCP_tw
```

---

## 6. 장애 시나리오

### 6.1 Connection Timeout (외부 PG API)

```text
증상: 결제 승인 요청에서 ConnectTimeoutException 급증

원인 분석:
  1. 외부 PG API 서버 과부하 (PG사 장애)
  2. 네트워크 경로 문제 (방화벽, NAT, 라우팅)
  3. 결제 서버의 Ephemeral Port 고갈
  4. DNS 해석 실패 → 잘못된 IP 연결 시도

진단:
  OTel Trace에서 외부 API Span 시간 확인
  tcpping <PG_IP> <PORT>  # TCP 수준 연결 테스트
  nstat TcpActiveOpens (연결 시도 수)
  ss -ant | grep SYN-SENT  # 대기 중인 연결 시도

대응:
  Circuit Breaker Open (Resilience4j)
  Fallback: 결제 대기 상태 처리 (결제 실패가 아닌 보류)
  Retry: Exponential Backoff + Jitter
```

### 6.2 TIME_WAIT 폭증 → Ephemeral Port 고갈

```text
증상: ConnectException: Cannot assign requested address
     ss -s에서 TIME_WAIT 수만 개

원인:
  결제 서버 → 외부 PG API: 요청마다 새 TCP 연결
  (Connection Pool 미사용 또는 Pool 크기 부족)
  TIME_WAIT: 연결 종료 후 60초 유지 (MSL × 2)

진단:
  ss -s | grep TIME-WAIT
  cat /proc/sys/net/ipv4/ip_local_port_range
  ss -ant state time-wait | awk '{print $5}' | cut -d: -f1 | sort | uniq -c

대응 (단기):
  sysctl -w net.ipv4.tcp_tw_reuse=1
  sysctl -w net.ipv4.ip_local_port_range="10000 65000"

대응 (근본):
  HttpClient / RestTemplate에 Connection Pool 설정
  WebClient: Reactor Netty 기본 Pool 확인
```

### 6.3 Kafka Consumer Lag 급증 (GC → Rebalance 연계)

```text
증상:
  kafka_consumer_lag 급증
  Consumer Group Rebalance 빈발
  결제 후처리 (원장, 알림) 지연

원인 연쇄:
  G1GC Mixed GC 발생 → STW 300ms
    → Kafka Consumer poll() 미호출
    → session.timeout.ms (기본 10초) 이전이면 유지
    → GC가 반복되어 누적 > session.timeout.ms → Rebalance

  또는:
    max.poll.interval.ms < 비즈니스 처리 시간 → Rebalance

진단:
  JFR GC Pause 시간 vs Kafka session.timeout.ms 비교
  kafka-consumer-groups.sh --describe
  Trace Span에서 Kafka commit 지연 확인

대응:
  ZGC/Shenandoah 전환 → GC Pause < 10ms
  session.timeout.ms 증가 (단, Rebalance 감지 지연)
  max.poll.interval.ms 증가
  Consumer 처리 로직 경량화
```

### 6.4 Kubernetes CNI / Overlay Network 병목

```text
증상:
  Pod 간 통신 Latency 급증
  결제 MSA 내부 호출 P99 악화

원인:
  VXLAN Encapsulation 오버헤드
  CNI Plugin CPU 포화 (kube-proxy iptables 규칙 수천 개)
  MTU Mismatch: 1500 MTU + VXLAN 50byte 헤더 → 단편화 발생

진단:
  ping -M do -s 1450 <Pod IP>  # MTU 확인
  ip route show  # 라우팅 테이블
  iptables -t nat -L -n | wc -l  # NAT 규칙 수
  conntrack -L | wc -l  # Connection tracking 수

대응:
  MTU 1450 (VXLAN) 또는 1400 (IPSec) 설정
  Cilium eBPF 모드: iptables 우회, 성능 향상
  kube-proxy 대체: IPVS 모드 (iptables보다 빠름)
```

### 6.5 Service Mesh (Istio/Envoy) Latency 폭증

```text
증상:
  결제 내부 서비스 간 통신 Latency 수 ms → 수십 ms
  Envoy Sidecar CPU 포화

원인:
  iptables redirect 오버헤드 (인바운드/아웃바운드 각 1회)
  Envoy가 처리하는 트래픽 급증
  mTLS Handshake 오버헤드 (서비스 간 매 연결)

진단:
  istioctl proxy-config cluster <pod>
  kubectl exec -it <pod> -c istio-proxy -- curl localhost:15000/stats
  Envoy stats: downstream_cx_active, upstream_rq_pending_total

대응:
  Envoy HTTP/2 Connection Pool (단일 TCP로 다중 RPC)
  mTLS Session Resumption
  PeerAuthentication 정책 최적화
  Ambient Mesh (Sidecar 없는 Istio): ztunnel 활용
```

### 6.6 MTU Mismatch (패킷 단편화)

```text
증상:
  특정 크기 이상의 요청(예: 카드 정보 전체 전송)에서 지연
  TCP 재전송 증가

원인:
  VXLAN(+50byte) / IPSec(+80byte) 오버헤드로 유효 MTU 감소
  Java HttpClient가 큰 패킷 전송 시 단편화 발생
  PMTUD (Path MTU Discovery) 실패: ICMP Fragmentation Needed 차단

진단:
  ping -M do -s 1400 <목적지>  # MTU 탐지
  tracepath <목적지>  # 경로 MTU 확인
  tcpdump -i eth0 'ip[6:2] & 0x1fff != 0'  # 단편화 패킷 탐지

대응:
  Kubernetes: CNI MTU 명시 설정
  Netty: ChannelOption을 통한 SO_SNDBUF 조정
  MSS Clamping: iptables -t mangle -A FORWARD -p tcp --tcp-flags SYN,RST SYN -j TCPMSS --clamp-mss-to-pmtu
```

### 6.7 DNS Failure (외부 PG 도메인)

```text
증상:
  결제 요청에서 UnknownHostException
  특정 PG사 연동만 간헐적 실패

원인:
  CoreDNS Pod 재시작 / OOM
  JVM DNS 캐시 만료 중 DNS 서버 장애
  외부 PG IP 변경 + JVM TTL 너무 긴 경우

진단:
  kubectl logs -n kube-system -l k8s-app=kube-dns
  tcpdump port 53 on <결제서버>
  java: sun.net.inetaddr.ttl 확인

대응:
  CoreDNS 리소스 증가 (CPU limit 조정)
  JVM DNS TTL: networkaddress.cache.ttl=30
  외부 DNS 직접 조회 설정 (CoreDNS forward 설정)
  Fallback: IP 직접 연결 설정 (PG사 IP 고정인 경우)
```

---

## 7. 튜닝 포인트

### 7.1 OS / Kernel 튜닝

```bash
# TCP 연결 관련
sysctl -w net.ipv4.tcp_max_syn_backlog=8192
sysctl -w net.core.somaxconn=32768
sysctl -w net.ipv4.ip_local_port_range="10000 65000"
sysctl -w net.ipv4.tcp_tw_reuse=1
sysctl -w net.ipv4.tcp_fin_timeout=30

# 버퍼 크기
sysctl -w net.core.rmem_max=16777216
sysctl -w net.core.wmem_max=16777216
sysctl -w net.ipv4.tcp_rmem="4096 87380 16777216"
sysctl -w net.ipv4.tcp_wmem="4096 65536 16777216"

# 백로그 및 SoftIRQ
sysctl -w net.core.netdev_max_backlog=30000
sysctl -w net.core.netdev_budget=600
sysctl -w net.ipv4.tcp_congestion_control=bbr  # BBR 혼잡 제어

# TIME_WAIT 관련
sysctl -w net.ipv4.tcp_max_tw_buckets=2000000
```

### 7.2 NIC 튜닝

```bash
# Ring Buffer 증가
ethtool -G eth0 rx 4096 tx 4096

# Interrupt Coalescing (배치 처리)
ethtool -C eth0 rx-usecs 50 tx-usecs 50

# IRQ Affinity (NUMA-aware)
for irq in $(cat /proc/interrupts | grep eth0 | awk '{print $1}' | tr -d ':'); do
  echo 0f > /proc/irq/$irq/smp_affinity  # CPU 0-3에 분산
done

# Offload 확인
ethtool -k eth0 | grep -E "tx-checksumming|rx-checksumming|tcp-segmentation-offload|generic-receive-offload"
```

### 7.3 Java / JVM 네트워크 튜닝

```java
// Netty 서버 최적화
ServerBootstrap bootstrap = new ServerBootstrap()
    .option(ChannelOption.SO_BACKLOG, 32768)
    .option(ChannelOption.SO_REUSEADDR, true)
    .childOption(ChannelOption.TCP_NODELAY, true)       // Nagle 비활성화
    .childOption(ChannelOption.SO_KEEPALIVE, true)
    .childOption(ChannelOption.SO_SNDBUF, 65536)
    .childOption(ChannelOption.SO_RCVBUF, 65536)
    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
        new WriteBufferWaterMark(32 * 1024, 64 * 1024)); // Backpressure 제어

// Netty Native epoll 활성화 (Linux)
// io.netty:netty-transport-native-epoll 의존성 추가
EventLoopGroup bossGroup = new EpollEventLoopGroup(1);
EventLoopGroup workerGroup = new EpollEventLoopGroup();
bootstrap.channel(EpollServerSocketChannel.class);

// HttpClient (JDK 11+) 튜닝
HttpClient httpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofMillis(1000))
    .executor(Executors.newVirtualThreadPerTaskExecutor())  // JDK 21+
    .version(HttpClient.Version.HTTP_2)  // HTTP/2
    .build();
```

### 7.4 Spring / WebFlux 네트워크 튜닝

```yaml
# Spring Boot application.yml
server:
  tomcat:
    accept-count: 1000          # Listen Backlog
    max-connections: 8192       # 최대 동시 연결
    threads:
      max: 400                  # Worker Thread 최대 수
      min-spare: 50
  netty:
    connection-timeout: 5000ms

# WebClient 튜닝
spring:
  webflux:
    netty:
      worker-threads: 8
```

```java
// Reactor Netty WebClient 튜닝
HttpClient httpClient = HttpClient.create()
    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
    .option(ChannelOption.TCP_NODELAY, true)
    .responseTimeout(Duration.ofMillis(3000))
    .doOnConnected(conn -> conn
        .addHandlerLast(new ReadTimeoutHandler(3))
        .addHandlerLast(new WriteTimeoutHandler(3)));

WebClient.builder()
    .clientConnector(new ReactorClientHttpConnector(httpClient))
    .build();
```

### 7.5 HikariCP 튜닝

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20         # CPU 코어 수 × 2~3
      minimum-idle: 5
      connection-timeout: 3000      # 3초
      idle-timeout: 600000          # 10분
      max-lifetime: 1800000         # 30분
      keepalive-time: 60000         # 1분 (방화벽 연결 끊김 방지)
      connection-test-query: SELECT 1
      data-source-properties:
        socketTimeout: 5000         # TCP Read Timeout
        tcpKeepAlive: true
```

### 7.6 TLS 최적화

```java
// Conscrypt (BoringSSL) 적용
Security.insertProviderAt(Conscrypt.newProvider(), 1);

// TLS 1.3 전용 설정
SSLContext sslContext = SSLContext.getInstance("TLS");
// 또는 Netty SslContext
SslContext nettySSLContext = SslContextBuilder.forClient()
    .protocols("TLSv1.3")
    .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
    .sslProvider(SslProvider.OPENSSL)  // OpenSSL/BoringSSL JNI
    .sessionCacheSize(10000)           // Session 캐시
    .sessionTimeout(86400)             // 24시간
    .build();
```

### 7.7 Kafka 네트워크 튜닝

```properties
# Producer
linger.ms=5
batch.size=65536
buffer.memory=67108864
compression.type=lz4
acks=1  # 결제 이벤트: acks=all 권장
max.block.ms=5000
request.timeout.ms=30000

# Consumer
fetch.min.bytes=1
fetch.max.wait.ms=500
max.poll.records=500
session.timeout.ms=30000
heartbeat.interval.ms=3000
max.poll.interval.ms=300000
```

### 7.8 Kubernetes CNI 최적화

```yaml
# Calico MTU 설정
apiVersion: projectcalico.org/v3
kind: FelixConfiguration
spec:
  mtu: 1440  # VXLAN 오버헤드 감안

# Cilium eBPF 모드 (kube-proxy 교체)
# values.yaml
kubeProxyReplacement: strict
nativeRoutingCIDR: "10.0.0.0/8"
bpf:
  masquerade: true

# Pod MTU 명시
# (CNI 플러그인별 설정 방법 상이)
```

---

## 8. 관련 Linux 명령어 및 분석 도구

### 8.1 소켓 및 연결 분석

```bash
# 소켓 상태 요약
ss -s

# ESTABLISHED 연결 상세 (프로세스 포함)
ss -antp state established

# TIME_WAIT 수
ss -ant state time-wait | wc -l

# 특정 포트 연결 상태
ss -antp '( dport = :8080 or sport = :8080 )'

# 소켓 내부 상태 (Send-Q, Recv-Q, RTT)
ss -i state established dst <PG_IP>

# 레거시 (ss 권장)
netstat -antp | grep -E 'TIME_WAIT|CLOSE_WAIT' | wc -l
```

### 8.2 네트워크 인터페이스 및 NIC 분석

```bash
# NIC 스펙 확인
ethtool eth0

# Ring Buffer 현황
ethtool -g eth0

# Offload 기능
ethtool -k eth0

# NIC 통계 (드롭, 에러)
ethtool -S eth0 | grep -E 'drop|error|miss'

# 인터페이스 트래픽
ip -s link show eth0
sar -n DEV 1 10

# 실시간 대역폭 모니터링
iftop -i eth0
```

### 8.3 TCP 통계

```bash
# TCP 전체 통계 (재전송, 에러)
nstat -az | grep -E 'Tcp|Udp'

# 재전송 증가율 모니터링
watch -n1 'nstat TcpRetransSegs'

# 상세 TCP 통계
cat /proc/net/snmp | grep Tcp
cat /proc/net/netstat | grep TcpExt

# SYN/Accept Queue 드롭
nstat TcpExtTCPReqQFullDrop TcpExtListenDrops TcpExtListenOverflows
```

### 8.4 패킷 캡처

```bash
# 특정 IP와의 통신 캡처
tcpdump -i eth0 host <PG_IP> -w /tmp/payment.pcap

# TLS Handshake 캡처 (ClientHello, ServerHello)
tcpdump -i eth0 'tcp port 443 and (tcp[((tcp[12:1] & 0xf0) >> 2):1] = 0x16)'

# DNS 쿼리 캡처
tcpdump -i any port 53 -nn

# TCP RST 패킷
tcpdump -i eth0 'tcp[tcpflags] & tcp-rst != 0'

# tshark (Wireshark CLI)
tshark -i eth0 -Y 'tcp.analysis.retransmission' -T fields -e frame.time -e ip.dst
```

### 8.5 커널 파라미터 및 시스템 분석

```bash
# SoftIRQ per CPU
mpstat -P ALL 1

# 네트워크 SoftIRQ 상세
cat /proc/softirqs | grep -E 'NET_RX|NET_TX'

# SoftIRQ 드롭
cat /proc/net/softnet_stat

# 네트워크 커널 파라미터
sysctl -a | grep -E 'net.ipv4.tcp|net.core'

# FD 사용량 (Java 프로세스)
lsof -p <PID> | grep -c 'IPv'
ls /proc/<PID>/fd | wc -l

# Connection tracking
conntrack -L 2>/dev/null | wc -l
```

### 8.6 eBPF / BCC Tools

```bash
# TCP 연결 지연 측정 (connect() → ACK)
/usr/share/bcc/tools/tcpconnlat -p <PID>

# TCP 재전송 추적
/usr/share/bcc/tools/tcpretrans

# TCP 연결 생명주기
/usr/share/bcc/tools/tcplife

# TCP RTT 분포
/usr/share/bcc/tools/tcprtt

# 소켓 연결 추적 (Java 프로세스)
/usr/share/bcc/tools/tcpconnect -p <PID>

# Runqueue Latency (네트워크 처리 지연)
/usr/share/bcc/tools/runqlat

# SoftIRQ 처리 시간
bpftrace -e 'tracepoint:irq:softirq_entry { @start[tid] = nsecs; }
             tracepoint:irq:softirq_exit  { @lat = hist(nsecs - @start[tid]); delete(@start[tid]); }'

# Java SocketRead/Write 추적 (uprobe)
bpftrace -p <PID> -e 'uprobe:/proc/<PID>/root/usr/lib/jvm/java/lib/server/libjvm.so:Java_java_net_SocketInputStream_socketRead0 { printf("read ts=%d\n", nsecs); }'
```

### 8.7 성능 분석 (perf)

```bash
# 네트워크 관련 syscall 통계
perf stat -e 'syscalls:sys_enter_read,syscalls:sys_enter_write,syscalls:sys_enter_epoll_wait' -p <PID> sleep 10

# epoll_wait 호출 빈도
perf trace -e 'syscalls:sys_enter_epoll_wait' -p <PID>

# NIC 인터럽트 발생 위치
perf stat -e 'irq:irq_handler_entry' -a sleep 5

# CPU Flamegraph (네트워크 처리 포함)
perf record -F 99 -g -p <PID> sleep 30
perf script | stackcollapse-perf.pl | flamegraph.pl > flame.svg
```

### 8.8 Java 특화 분석 도구

```bash
# JFR 수집 (네트워크 이벤트 포함)
jcmd <PID> JFR.start name=net duration=60s filename=/tmp/net.jfr settings=profile

# JFR 분석 (JMC GUI 또는 커맨드라인)
java -jar jmc.jar &

# async-profiler (네트워크 I/O off-CPU 포함)
./profiler.sh -e wall -d 60 -f /tmp/wall.html <PID>

# Thread Dump (네트워크 대기 Thread 확인)
jcmd <PID> Thread.print | grep -A5 'SocketInputStream\|SocketOutputStream'

# 네이티브 메모리 (Direct Buffer 포함)
jcmd <PID> VM.native_memory summary

# Heap Dump + MAT (Socket 누수 분석)
jcmd <PID> GC.heap_dump /tmp/heap.hprof
```

### 8.9 Kafka 네트워크 분석

```bash
# Consumer Lag 확인
kafka-consumer-groups.sh --bootstrap-server <broker> --describe --group payment-consumer

# Producer 통계 (JMX)
jconsole &  # kafka.producer:type=producer-metrics,client-id=<id>

# 브로커 네트워크 통계
kafka-topics.sh --describe --bootstrap-server <broker>
```

### 8.10 Kubernetes / Service Mesh 분석

```bash
# Istio Envoy 통계
kubectl exec -it <pod> -c istio-proxy -- curl -s localhost:15000/stats | grep -E 'cx_active|rq_pending'

# Envoy 업스트림 연결
istioctl proxy-config cluster <pod>.<namespace>

# Pod 간 네트워크 레이턴시 측정
kubectl exec -it <source-pod> -- ping -c 100 <dest-pod-ip>

# CNI 정보 확인
kubectl get cm -n kube-system kube-proxy -o yaml | grep mode
kubectl get pods -n kube-system | grep -E 'cilium|calico|flannel'

# iptables NAT 규칙 수
iptables -t nat -L -n | wc -l

# conntrack 현황
conntrack -L 2>/dev/null | head -20
conntrack -S  # 통계
```

---

## 부록: Java 결제 시스템 Network 장애 분석 순서

```text
결제 Latency / 실패율 증가 감지
  ↓
1. OTel Trace → 느린 Span 특정
   (외부 PG API? DB? 내부 서비스?)
  ↓
2. JFR SocketRead/Write 이벤트 → TCP I/O 지연 확인
  ↓
3. ss -s → TIME_WAIT, CLOSE_WAIT, Recv-Q/Send-Q 확인
  ↓
4. nstat TcpRetransSegs → 재전송 증가 여부
  ↓
5. mpstat %soft → SoftIRQ 포화 여부
  ↓
6. ethtool -S eth0 | grep drop → NIC 수준 드롭 여부
  ↓
7. nstat TcpExtListenDrops → Accept Queue 드롭 여부
  ↓
8. Kubernetes: kubectl top pod → cgroup CPU Throttling
   /sys/fs/cgroup/cpu.stat throttled_usec 확인
  ↓
9. eBPF tcpretrans, tcpconnlat → 커널 수준 지연 증거 수집
  ↓
10. 최근 배포 / CNI 변경 / PG사 공지 확인
```

---

*이 문서는 Java FinTech 결제 시스템의 Network 자원을 E2E 관점에서 SRE가 분석하기 위한 Base Knowledge로 관리됩니다.  
내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*