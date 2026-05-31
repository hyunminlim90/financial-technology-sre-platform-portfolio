# Server (서버)
## FinTech 결제 시스템 SRE 관점 — Java E2E Foundations

> 정독: 0회

## 목차

1. [Stack Context 식별](#1-stack-context-식별)
2. [E2E 계층별 서버 실행 흐름](#2-e2e-계층별-서버-실행-흐름)
3. [Server Resource Flow — 핵심 메커니즘](#3-server-resource-flow--핵심-메커니즘)
4. [Resource Flow Correlation](#4-resource-flow-correlation)
5. [Top-Down 분석: 증상 → 서버 내부 원인 추적](#5-top-down-분석-증상--서버-내부-원인-추적)
6. [Bottom-Up 분석: 하드웨어/커널 특성 → Java 서버 성능 영향](#6-bottom-up-분석-하드웨어커널-특성--java-서버-성능-영향)
7. [장애 및 Saturation 패턴](#7-장애-및-saturation-패턴)
8. [Linux / JVM / Kubernetes 실무 관측](#8-linux--jvm--kubernetes-실무-관측)

---

## 1. Stack Context 식별

### Server가 연결되는 스택 계층

| 스택 | 서버 역할 | 핵심 실행 메커니즘 |
|------|---------|-----------------|
| Java / JVM | 결제 Application Server | Thread Pool, GC, JIT, Heap |
| Spring Boot | HTTP API Server | Embedded Tomcat, DispatcherServlet |
| Netty | Non-blocking I/O Server | EventLoop, Channel Pipeline, sk_buff |
| WebFlux | Reactive API Server | Reactor, Non-blocking Socket |
| MySQL | Database Server | InnoDB, Connection Thread, Buffer Pool |
| Kafka | Event Streaming Server | Network Thread, I/O Thread, Page Cache |
| Redis | In-memory Cache Server | Single Thread Event Loop, jemalloc |
| Kubernetes | Container Orchestration | Node, Pod, kubelet, cgroup |

### 서버가 등장하는 핵심 실행 흐름

```
클라이언트 결제 요청
→ Load Balancer (Nginx/AWS ALB)
→ Spring Boot API Server (Java: Tomcat / Netty)
→ Spring WebFlux / MVC Handler
→ JPA → MySQL Server (Database)
→ Redis Server (Cache)
→ Kafka Server (Event Streaming)
→ 결제 응답 반환
```

### 핵심 자원 포커스

```
Server(Java) → Network + CPU + Memory + Scheduler
```

서버는 네트워크 요청을 받아 CPU에서 처리하고, 메모리에 상태를 유지하며,
OS Scheduler가 요청 처리 Thread를 물리 Core에 배분하는 전체 흐름이다.

---

## 2. E2E 계층별 서버 실행 흐름

### 2-1. Hardware / Physical Layer

결제 요청이 서버에 닿는 물리 경로:

```
클라이언트 패킷 도착
    │
    ▼
NIC (Physical: PHY + MAC)
    │ DMA → DRAM (NIC Ring Buffer)
    ▼
PCIe Bus
    │
    ▼
CPU Core (DRAM → L3 → L2 → L1 → ALU)
    │
    ├─ 결제 로직 연산 (CPU Core)
    ├─ Heap 객체 (DRAM)
    └─ DB/Kafka 기록 (NVMe)
```

| 물리 자원 | 결제 서버에서의 역할 |
|----------|-------------------|
| NIC | 결제 요청 패킷 수신/송신 |
| DRAM | JVM Heap, Thread Stack, Page Cache |
| CPU Core | 요청 처리, GC, JIT 컴파일 |
| NVMe | DB WAL, Kafka Log Segment |
| PCIe | NIC/NVMe ↔ DRAM DMA 전송 |

### 2-2. OS Kernel Layer

Java 서버 프로세스가 커널 자원을 소비하는 경로:

**요청 수신 경로:**

```
NIC 인터럽트 (IRQ)
    → SoftIRQ (NET_RX)
    → Kernel Network Stack
    → TCP 처리 → sk_buff 생성
    → Socket Receive Buffer
    → Java Process: read() / accept() System Call
    → JVM Thread 활성화
```

**핵심 커널 메커니즘:**

| 메커니즘 | 서버에서의 역할 |
|---------|-------------|
| TCP Backlog / SYN Queue | 연결 요청 대기열. 고트래픽 시 SYN Drop 위험 |
| epoll | Java NIO / Netty의 Non-blocking 이벤트 감지 |
| Socket Buffer (sk_buff) | 패킷 수신 후 커널 임시 저장 |
| CFS Scheduler | Java Thread → Physical Core 배분 |
| cgroup | Pod CPU/Memory 제한 (Kubernetes 환경) |
| IRQ Affinity | NIC 인터럽트를 특정 Core에 배분 |

**TCP 연결 처리 흐름:**

```
SYN 수신
    → SYN Queue 적재
    → SYN-ACK 송신
    → ACK 수신
    → Accept Queue 이동
    → Java: ServerSocketChannel.accept()
    → Netty Channel 등록 → epoll_ctl
```

### 2-3. JVM Runtime Layer

Java 서버 요청 처리의 런타임 핵심:

```
요청 수신 (Socket Read)
    │
    ├─ Tomcat BIO/NIO: Thread Pool에서 Worker Thread 할당
    │     └─ OS Thread 1:1 매핑
    │
    └─ Netty / WebFlux: EventLoop Thread
          └─ Non-blocking: 하나의 Thread가 다수 채널 처리
```

**JVM 서버 자원 소비:**

| JVM 구성 | 결제 서버 역할 | 물리 자원 |
|---------|-------------|---------|
| Thread Pool (Tomcat) | 요청당 Thread 할당 | CPU Core + Stack DRAM |
| JVM Heap | 요청 처리 객체 (DTO, Entity) | DRAM |
| TLAB | Thread별 빠른 Heap 할당 | L1/L2 Cache |
| G1GC / ZGC | 사용 완료 객체 회수 | CPU + DRAM Bandwidth |
| JIT (C1/C2) | 핫 결제 로직 Native 컴파일 | CPU + Code Cache |
| DirectByteBuffer | Netty Zero-Copy 소켓 버퍼 | Off-Heap DRAM |
| Code Cache | JIT 컴파일 코드 저장 | L2/L3 Cache + DRAM |

**Safepoint와 결제 처리:**

```
GC Safepoint 요청
    → 모든 Java Thread 현재 지점에서 중단 대기
    → 결제 처리 Thread 포함 전체 정지
    → GC 완료 후 재개
    → 이 구간 동안 결제 요청 응답 없음 → P100 Latency = STW 시간
```

### 2-4. Framework / Middleware Layer

**Spring Boot + Tomcat (Blocking I/O 모델):**

```
HTTP 요청 수신
    │
    └─ Tomcat Connector
         ├─ Acceptor Thread: accept() 루프
         ├─ Poller Thread: NIO Selector
         └─ Worker Thread Pool (기본 200개)
              │
              └─ DispatcherServlet
                   └─ @RestController → 결제 서비스 → DB/Kafka
```

**Thread Pool 포화 시:**

```
Worker Thread 전부 점유 (DB 응답 대기 중)
    → 신규 요청 → Tomcat Queue (acceptCount) 적재
    → Queue 초과 → 연결 거부 (Connection Refused)
    → 결제 요청 실패
```

**Spring WebFlux + Netty (Non-blocking 모델):**

```
HTTP 요청 수신 (Netty)
    │
    └─ EventLoop Thread (CPU Core 수와 동일, 기본 2×코어)
         └─ Channel Pipeline
              └─ HttpServerCodec → WebFlux Handler
                   └─ Reactor Mono/Flux Chain
                        ├─ Non-blocking DB (R2DBC)
                        └─ Non-blocking Kafka
```

**WebFlux Blocking 혼입 시:**

```
EventLoop Thread에서 Blocking 호출 (JDBC 등)
    → EventLoop Thread 점유
    → 다른 연결의 이벤트 처리 불가
    → 전체 서버 응답 지연
```

**Kafka Broker 서버 내부:**

```
Producer 요청 수신
    │
    └─ Network Thread (요청 파싱)
         └─ Request Queue
              └─ I/O Thread (Page Cache 기록)
                   └─ Page Cache (DRAM)
                        └─ Flush → NVMe (비동기)
```

**MySQL Server 내부:**

```
Connection 수신
    │
    └─ One-Thread-Per-Connection 모델
         └─ Query Parser → Optimizer → InnoDB
              └─ InnoDB Buffer Pool (DRAM)
                   ├─ Hit  → 즉시 반환
                   └─ Miss → NVMe I/O 발생
```

### 2-5. Application / Business Layer

결제 서버 처리 흐름의 자원 소비:

| 결제 처리 단계 | 실행 위치 | 자원 소비 |
|-------------|---------|---------|
| TLS Termination | Netty Pipeline | CPU (RSA/ECDHE) |
| JWT 검증 | Spring Security Filter | CPU (HMAC-SHA256) |
| JSON 파싱 | Jackson ObjectMapper | CPU + Heap |
| 결제 유효성 검사 | Business Layer | CPU (Branch) |
| 잔액 조회 | Redis / MySQL | Network + DRAM/NVMe |
| 승인 처리 | InnoDB Transaction | CPU + NVMe (fsync) |
| 이벤트 발행 | Kafka Producer | Network + Page Cache |
| 응답 직렬화 | Jackson | CPU + Heap/DirectMemory |

---

## 3. Server Resource Flow — 핵심 메커니즘

### 3-1. TCP Backlog & SYN Queue

결제 트래픽 Burst 시 연결 수락 경로의 병목:

```
대량 SYN 패킷 수신
    │
    └─ SYN Queue (net.ipv4.tcp_max_syn_backlog)
         │
         ├─ Queue 여유 있음 → SYN-ACK 응신
         └─ Queue 포화 → SYN Drop → Client Retransmission
              │
              └─ 결제 연결 지연 (수 초)

ACK 수신 후:
    └─ Accept Queue (somaxconn / backlog)
         │
         ├─ 여유 있음 → Java accept() 처리
         └─ 포화 → RST 송신 → 결제 연결 실패
```

**커널 파라미터:**

```bash
sysctl net.ipv4.tcp_max_syn_backlog   # SYN Queue 크기
sysctl net.core.somaxconn             # Accept Queue 크기
sysctl net.ipv4.tcp_syn_retries       # SYN 재전송 횟수
```

### 3-2. epoll & Non-blocking I/O

Netty의 결제 서버 이벤트 처리 내부:

```
Netty Boss EventLoop
    └─ epoll_wait() (무한 루프)
         │
         └─ 이벤트 발생 (읽기 가능, 연결 수립 등)
              │
              └─ Worker EventLoop에 Channel 등록
                   └─ epoll_ctl(EPOLL_CTL_ADD, fd, EPOLLIN)
                        │
                        └─ epoll_wait() → 데이터 도착 알림
                             └─ read() → DirectByteBuffer
                                  └─ Channel Pipeline 처리
```

**epoll vs select 차이 (서버 규모 영향):**

| 방식 | 동시 연결 10만 | 오버헤드 |
|------|-------------|---------|
| select/poll | O(N) 전체 검사 | 연결 수 증가 시 CPU 폭증 |
| epoll | O(1) 이벤트 기반 | 연결 수 무관, 이벤트 발생 시만 처리 |

결제 서버는 동시 수천~수만 연결을 유지하므로 epoll이 필수다.

### 3-3. Thread Pool Saturation (Tomcat)

Spring MVC + Tomcat의 결제 서버 Thread 포화:

```
결제 요청 증가
    │
    └─ Worker Thread Pool (maxThreads: 200)
         │
         ├─ 여유 Thread 있음 → 즉시 처리
         └─ 모든 Thread 점유 (DB/Redis 응답 대기 중)
              │
              └─ 신규 요청 → acceptCount Queue (기본 100)
                   │
                   ├─ Queue 여유 있음 → 대기
                   └─ Queue 포화 → Connection Refused
                        → 결제 요청 실패
```

**Thread 수 vs 처리량 트레이드오프:**

```
Thread 수 증가 → Context Switch 증가 → CPU 오버헤드 증가
Thread 수 감소 → Queue 대기 증가 → Latency 증가

최적 Thread 수 (CPU-bound):
    Physical Core 수 × (1 + Wait Time / CPU Time)
```

### 3-4. Connection Pool (HikariCP)

결제 서버의 DB 연결 자원 관리:

```
결제 요청 → DB 조회 필요
    │
    └─ HikariCP.getConnection()
         │
         ├─ Pool에 유휴 Connection 있음 → 즉시 반환
         └─ 모든 Connection 사용 중
              │
              └─ connectionTimeout 대기 (기본 30초)
                   │
                   ├─ 대기 중 Connection 반환 → 처리
                   └─ Timeout → SQLException
                        → 결제 처리 실패
```

**Connection Pool 포화 원인:**

- DB 응답 지연 (NVMe saturation, Lock wait)
- 결제 TPS 급증
- Connection Leak (트랜잭션 미종료)

### 3-5. GC와 서버 응답 지연

결제 서버의 GC 영향:

```
결제 요청 처리 중 Young GC 발생 (G1GC)
    → STW: 수 ms
    → 결제 처리 Thread 일시 정지
    → 이 구간의 요청 응답 지연
    → P95/P99 급증

Full GC / Mixed GC 발생 (Old Gen 압박)
    → STW: 수십 ms ~ 수 초
    → 모든 결제 처리 중단
    → Client Timeout 발생
    → Retry Storm
```

**ZGC 사용 시 (Java 15+):**

```
ZGC: Concurrent GC
    → STW < 1ms (대부분의 작업이 Concurrent)
    → Load Barrier 오버헤드: ~5~10% CPU 증가
    → 결제 서버 P99 안정화에 유리
```

---

## 4. Resource Flow Correlation

### 서버 자원 연결 구조

```
결제 요청 TPS 증가
    │
    ├─► Network
    │       │
    │       ├─► NIC 수신 증가 → IRQ Storm (특정 Core 집중)
    │       ├─► Socket Buffer 증가 → DRAM 압박
    │       └─► SYN Queue / Accept Queue 포화
    │
    ├─► CPU
    │       │
    │       ├─► Thread 경합 → Context Switch 증가
    │       ├─► GC Thread CPU 점유
    │       ├─► JIT 컴파일 (초기 워밍업 시)
    │       └─► TLS 연산 (RSA/ECDHE) 증가
    │
    ├─► Memory
    │       │
    │       ├─► Heap 증가 → GC 빈도 증가
    │       ├─► Thread Stack 증가 (Tomcat 모델)
    │       └─► DirectMemory 증가 (Netty)
    │
    └─► Disk
            │
            ├─► DB Connection 증가 → MySQL fsync 증가
            ├─► Kafka Produce → Page Cache → NVMe Flush
            └─► 로그 기록 → I/O 증가
```

### 자원 트레이드오프

| 설계 선택 | 절약 자원 | 추가 소비 자원 | FinTech 트레이드오프 |
|---------|---------|-------------|-------------------|
| Tomcat BIO → Netty NIO | Thread(Memory) | CPU(epoll 처리) | 고동시성에서 Netty 유리 |
| Heap 증대 | GC 빈도 감소 | DRAM 증가, Page Cache 감소 | Heap × 1.5~2배 DRAM 필요 |
| Connection Pool 증대 | 대기 시간 감소 | DB Server 부하 증가 | DB max_connections 고려 |
| ZGC 사용 | STW Latency 감소 | CPU 5~10% 증가 | 결제 P99 안정화에 유리 |
| Virtual Thread (Loom) | Thread 생성 비용 | ForkJoinPool 복잡성 | Blocking I/O 많은 결제에 유리 |

---

## 5. Top-Down 분석: 증상 → 서버 내부 원인 추적

### 증상 1: 결제 API 응답 시간 급증 (P99 > 2초)

```
증상: P99 2000ms, 정상: 200ms
    │
    ├─► GC 로그 확인
    │       └─► Full GC 발생: STW 1.5초
    │                └─► 원인: Old Gen 압박 (Heap 부족)
    │                         JVM Heap 증설 또는 ZGC 전환 필요
    │
    ├─► Thread Pool 확인
    │       └─► Tomcat Worker Thread 전부 점유
    │                └─► 원인: DB 응답 지연으로 Thread Blocking
    │                         HikariCP maxPoolSize 확인
    │
    └─► CPU Throttling 확인 (Kubernetes)
             └─► cgroup throttled_time 증가
                      └─► 원인: Pod CPU Limit 과소 설정
```

### 증상 2: 결제 서버 Connection Refused

```
증상: 일부 결제 요청 Connection Refused
    │
    ├─► ss -lntp 확인 → Listen 포트 정상
    ├─► Tomcat 로그 확인 → "connection refused" 없음
    │
    └─► 커널 파라미터 확인
             └─► netstat -s | grep overflow
                  → 'times the listen queue of a socket overflowed' 증가
                       │
                       └─► 원인: Accept Queue (somaxconn) 포화
                                sysctl net.core.somaxconn 증설 필요
                                server.tomcat.accept-count 증설 필요
```

### 증상 3: 결제 서버 CPU 100%

```
증상: CPU 100%, TPS 감소
    │
    ├─► perf top 확인
    │       └─► sha256_transform, RSA 함수 상위 → TLS 연산 과다
    │                └─► 원인: TLS Session Reuse 미설정
    │                         ECDHE 키 재사용 설정 필요
    │
    ├─► async-profiler Flamegraph 확인
    │       └─► GC 관련 함수 상위
    │                └─► 원인: Heap 부족 → GC Spin
    │
    └─► mpstat -P ALL 확인
             └─► 특정 Core 100% (SoftIRQ)
                      └─► 원인: NIC IRQ Affinity 미설정
                               irqbalance 또는 수동 IRQ 배분 필요
```

### 증상 4: 결제 Timeout 간헐적 발생

```
증상: 매 5분마다 일부 결제 Timeout
    │
    ├─► 결제 서버 GC 로그 → Concurrent GC 정상
    ├─► MySQL slow log → 없음
    │
    └─► Kafka Producer 확인
             └─► linger.ms 설정으로 배치 대기 발생
                  → 결제 이벤트 발행 지연 누적
                       └─► 원인: Kafka Producer 배치 설정 과다
                                linger.ms 축소 또는 0으로 설정
```

---

## 6. Bottom-Up 분석: 하드웨어/커널 특성 → Java 서버 성능 영향

### NIC IRQ Affinity → Netty EventLoop 성능

```
NIC 수신 패킷 → IRQ 발생
    → 모든 IRQ가 CPU 0에 집중 (기본 설정)
    → CPU 0: SoftIRQ 처리 + Netty EventLoop 혼재
    → CPU 0 포화 → 패킷 처리 지연

해결:
    → IRQ Affinity 설정: NIC IRQ → CPU 0~3
    → Netty EventLoop → CPU 4~11
    → 분리로 간섭 제거
```

```bash
# IRQ 번호 확인
cat /proc/interrupts | grep eth0

# IRQ Affinity 설정 (CPU 0~3에 NIC IRQ 배분)
echo "0f" > /proc/irq/<irq_number>/smp_affinity
```

### NUMA 토폴로지 → JVM Thread 스케줄링

```
서버: 2-Socket NUMA (Socket 0, Socket 1 각 32 Core)

JVM이 NUMA 인식 없이 실행 시:
    → Socket 0의 Thread가 Socket 1의 DRAM 접근
    → Remote NUMA Access: ~200ns (Local: ~100ns)
    → Heap 객체 접근 지연 증가
    → 결제 처리 Latency 상승

해결:
    java -XX:+UseNUMA PaymentApplication
    → 각 NUMA Node 로컬 DRAM에서 TLAB 할당
```

### Physical Core 수 → 서버 Thread 설계

```
Physical Core: 16
Hyper-Threading: 32 Logical

Tomcat maxThreads 설정:
    → CPU-bound 결제 로직: 16~32 Thread
    → I/O-bound (DB 대기 많음): 100~200 Thread
    → 너무 많은 Thread → Context Switch 오버헤드

Netty EventLoop:
    → Physical Core 수 = EventLoop 수 (기본 2×코어 = 32)
    → CPU-bound 작업 없으면 16개로 축소 권장
```

### TCP Socket Buffer → 결제 Throughput

```
기본 Socket Buffer:
    net.core.rmem_default = 212992 (208KB)
    net.core.wmem_default = 212992 (208KB)

결제 서버 고처리량 환경:
    sysctl net.core.rmem_max = 134217728   # 128MB
    sysctl net.core.wmem_max = 134217728
    sysctl net.ipv4.tcp_rmem = "4096 87380 134217728"
    sysctl net.ipv4.tcp_wmem = "4096 87380 134217728"

Socket Buffer 증대 시:
    → Burst 수신 시 Drop 감소
    → DRAM 소비 증가 (연결 수 × Buffer 크기)
```

---

## 7. 장애 및 Saturation 패턴

### 7-1. Thread Pool Exhaustion + Cascading Failure

**트리거:** DB 응답 지연 → Tomcat Thread 고갈

```
MySQL 응답 지연 (NVMe I/O 포화)
    → Tomcat Worker Thread가 DB 응답 대기 중 Blocking
    → 모든 200개 Thread 점유
    → 신규 결제 요청 → acceptCount Queue 대기
    → Queue 포화 → Connection Refused
    → Load Balancer Health Check 실패
    → 결제 서버 Traffic 제거
    → 다른 서버로 트래픽 집중
    → 연쇄 Thread Pool Exhaustion
    → 전체 결제 서비스 불가 (Cascading Failure)
```

**관측:**
```bash
# Tomcat Thread 상태
jstack <pid> | grep -c "BLOCKED\|WAITING"

# DB 연결 대기
jstack <pid> | grep "HikariCP"

# Accept Queue 상태
ss -lntp | grep 8080
netstat -s | grep overflow
```

### 7-2. GC Storm + OOMKilled

**트리거:** 결제 Batch 처리 중 대량 객체 생성

```
Spring Batch: 100만 건 결제 정산 처리
    → 대량 Entity 객체 생성 → Heap 급증
    → Young GC 빈도 증가 (매 초)
    → Old Gen 압박 → Full GC 반복
    → 각 Full GC: STW 2~5초
    → 결제 처리 주기적 완전 중단
    → Heap 부족 시 OOM Killer 발동
    → JVM Kill → Pod Restart
    → 처리 중 결제 상태 불일치 위험
```

**관측:**
```bash
jstat -gcutil <pid> 1000   # GC 통계
jcmd <pid> GC.heap_info    # 현재 Heap
jmap -histo <pid> | head -20  # 객체 분포
```

### 7-3. Netty EventLoop Blocking

**트리거:** WebFlux 핸들러에서 Blocking 코드 실행

```java
// 문제 코드: EventLoop Thread에서 JDBC 직접 호출
@GetMapping("/payment")
public Mono<Payment> getPayment() {
    return Mono.fromCallable(() ->
        jdbcTemplate.queryForObject(sql, Payment.class)  // Blocking!
    );
    // subscribeOn 미지정 → EventLoop Thread에서 실행
}
```

```
EventLoop Thread (CPU Core 4개 담당)
    → JDBC 호출 → Thread Block (DB 응답 대기 중)
    → 해당 EventLoop가 담당하는 모든 Channel 처리 중단
    → 수천 개 결제 연결 응답 불가
    → P99 급증 → Client Timeout
```

**해결:**
```java
return Mono.fromCallable(() -> jdbcTemplate.queryForObject(sql, Payment.class))
           .subscribeOn(Schedulers.boundedElastic());  // Blocking 전용 Thread Pool
```

### 7-4. SYN Flood / Accept Queue Overflow

**트리거:** 결제 트래픽 Burst 또는 DDoS

```
대량 SYN 수신 (결제 피크 타임)
    → SYN Queue 포화 (tcp_max_syn_backlog 기본 128)
    → SYN Drop 발생
    → 클라이언트: SYN 재전송 (1초, 3초, 7초...)
    → 결제 연결 수립 지연 3~7초
    → 결제 앱 Timeout → 결제 실패
```

**관측 및 해결:**
```bash
# 현재 상태 확인
netstat -s | grep "SYNs to LISTEN"
ss -lnt | grep 8080

# 해결: Queue 크기 증설
sysctl -w net.ipv4.tcp_max_syn_backlog=65536
sysctl -w net.core.somaxconn=65536

# Spring Boot 설정
server.tomcat.accept-count=1000
```

### 7-5. Connection Leak → Pool Exhaustion

**트리거:** 트랜잭션 예외 처리 누락

```java
// 문제 코드: 예외 발생 시 Connection 반환 안 됨
Connection conn = dataSource.getConnection();
try {
    // 결제 처리
    processPayment(conn);
    // 예외 발생 → conn.close() 미호출
} catch (Exception e) {
    log.error("결제 실패", e);
    // conn이 반환되지 않고 누수
}
```

```
결제 처리 중 예외 발생 반복
    → HikariCP Connection 점진적 누수
    → Pool 사용 가능 Connection 감소
    → connectionTimeout 대기 증가
    → 결제 P99 증가
    → Pool 완전 고갈 → 결제 전면 불가
```

---

## 8. Linux / JVM / Kubernetes 실무 관측

### 8-1. 서버 프로세스 / 포트 상태

```bash
# 서버 프로세스 확인
ps -ef | grep java
pstree -p <pid>

# 포트 Listen 상태
ss -lntp | grep 8080
netstat -lntp

# TCP 연결 상태 분포
ss -ant | awk '{print $1}' | sort | uniq -c | sort -rn
```

### 8-2. 서버 부하 / CPU

```bash
# 전체 시스템 부하
top -H -p <pid>      # Thread별 CPU
mpstat -P ALL 1      # Core별 CPU
pidstat -u 1 -p <pid>

# Context Switch
vmstat 1
pidstat -w 1

# CPU Flamegraph
./asprof -d 30 -f flamegraph.html <pid>
perf top
```

### 8-3. 네트워크 / 소켓

```bash
# Socket Buffer 상태
ss -m         # Socket별 메모리
ss -s         # 전체 통계

# TCP 재전송 / Drop
netstat -s | grep -i retran
netstat -s | grep overflow

# NIC 통계
sar -n DEV 1
ethtool -S eth0 | grep -i drop

# SYN Queue / Accept Queue
cat /proc/net/tcp   # 상세 TCP 상태
```

### 8-4. JVM 서버 상태

```bash
# Thread 상태 분석
jstack <pid> | grep -E "BLOCKED|WAITING|TIMED_WAITING" | wc -l
jstack <pid> > thread_dump.txt

# GC 모니터링
jstat -gcutil <pid> 1000
jstat -gccause <pid> 1000

# Heap 분석
jcmd <pid> GC.heap_info
jmap -histo:live <pid> | head -30

# JFR 수집
jcmd <pid> JFR.start \
  duration=120s \
  settings=profile \
  filename=server.jfr

# HikariCP 연결 상태 (Micrometer)
# hikaricp.connections.active
# hikaricp.connections.pending
# hikaricp.connections.timeout.total
```

### 8-5. Kubernetes 서버 관측

```bash
# Pod 상태
kubectl get pods -n payment -o wide
kubectl describe pod <pod> -n payment

# 자원 사용량
kubectl top pods -n payment --containers
kubectl top nodes

# CPU Throttling 확인
cat /sys/fs/cgroup/cpu/kubepods/pod<uid>/<container>/cpu.stat
# nr_throttled, throttled_time 확인

# 이벤트 (OOMKilled, Eviction)
kubectl get events -n payment --sort-by='.lastTimestamp' | tail -20

# 로그
kubectl logs <pod> -n payment --tail=100
kubectl logs <pod> -n payment -c <container> --previous
```

### 8-6. PSI (Pressure Stall Information)

```bash
# 서버 자원 압박 상태
cat /proc/pressure/cpu     # CPU 대기 시간 비율
cat /proc/pressure/memory  # Memory 압박
cat /proc/pressure/io      # I/O 압박

# PSI 해석:
# some: 일부 Task 대기 중
# full: 모든 Task 대기 중 (심각)
```

### 8-7. eBPF 기반 서버 분석

```bash
# 서버 Off-CPU 분석 (어디서 Thread가 대기하는가)
/usr/share/bcc/tools/offcputime -p <pid> 30

# Run Queue Latency (스케줄링 대기)
/usr/share/bcc/tools/runqlat

# TCP 연결 추적
/usr/share/bcc/tools/tcplife
/usr/share/bcc/tools/tcpretrans

# 서버 System Call 분석
/usr/share/bcc/tools/syscount -p <pid>
strace -c -p <pid>   # 간단한 경우
```

---

## Summary: Java 결제 서버 핵심 자원 축

| 서버 계층 | 결제 KPI 영향 | 대표 장애 패턴 | 핵심 관측 지표 |
|---------|------------|-------------|-------------|
| TCP Stack (Kernel) | 연결 수립 지연 | SYN Queue Overflow | `netstat -s` overflow |
| epoll / NIO | 동시 연결 처리량 | EventLoop Blocking | `jstack` Thread 상태 |
| Thread Pool (Tomcat) | 요청 처리 TPS | Pool Exhaustion | Active/Blocked Thread 수 |
| JVM Heap / GC | P99 Latency | STW, OOMKilled | `jstat -gcutil`, JFR |
| HikariCP | DB 응답 시간 | Connection Leak, Timeout | Pool Active/Pending |
| cgroup (K8s) | CPU 할당량 | Throttling, Pod Kill | `cpu.stat` throttled_time |
| NIC / IRQ | 수신 처리 속도 | IRQ Storm, Packet Drop | `ethtool -S`, `perf top` |

> 결제 서버의 P99 Latency 이상은 애플리케이션 코드가 아니라 **TCP Backlog 포화, Tomcat Thread Pool 고갈, GC STW, cgroup CPU Throttling, NIC IRQ 편중** 중 하나 이상에서 기인하는 경우가 많다. 각 계층을 순서대로 배제하는 방식으로 원인을 좁혀야 한다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*