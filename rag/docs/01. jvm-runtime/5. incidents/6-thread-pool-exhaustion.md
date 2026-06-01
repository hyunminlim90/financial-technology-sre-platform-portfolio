# Fin-Tech 결제 시스템 SRE 관점의 JVM Runtime Thread Pool Exhaustion 장애 분석 가이드

## JVM 기반 금융 결제 시스템을 운영하는 SRE / Platform Engineer
### Linux Kernel (Cgroup v2), Kubernetes, JDK 21+, Generational ZGC, Virtual Threads

> 정독: 0회

## 목차

1. [Thread Pool Exhaustion 전체 구조](#1-thread-pool-exhaustion-전체-구조)
2. [하부 인프라 및 OS 커널 계층의 연동 기전](#2-하부-인프라-및-os-커널-계층의-연동-기전)
3. [JVM 내부 동시성 서브시스템별 고갈 역학](#3-jvm-내부-동시성-서브시스템별-고갈-역학)
4. [기술 발전 및 핀테크 아키텍처 채택 동향 (2026)](#4-기술-발전-및-핀테크-아키텍처-채택-동향-2026)
5. [관측성(Observability) 및 트러블슈팅 런북](#5-관측성observability-및-트러블슈팅-런북)
6. [2026 Production Baseline JVM Manifest](#6-2026-production-baseline-jvm-manifest)

---

## 1. Thread Pool Exhaustion 전체 구조

### 정의

**Thread Pool Exhaustion**은 애플리케이션이 보유한 스레드 풀의 가용 스레드가 모두 사용 중인 상태를 의미한다. 새로운 요청을 처리할 스레드가 존재하지 않으므로 응답 지연, 요청 대기, 요청 거부가 연쇄 발생한다.

> **핵심 원칙**: Thread Pool Exhaustion은 단순히 스레드 개수 부족 문제가 아니다. **왜 스레드가 반환되지 못하고 점유되고 있는지**를 파악하는 것이 진단의 핵심이다.

**대표 증상**

- 응답시간 급등 및 TPS 감소
- Request Timeout / Connection Refused
- `RejectedExecutionException`
- `java.lang.OutOfMemoryError: unable to create new native thread`

### 장애 전파 계층 구조

```
Application (Thread Pool 고갈)
        │
        ▼
JVM Thread Pool (가용 스레드 = 0)
        │
        ▼
Linux Scheduler (Context Switching 포화)
        │
        ▼
TCP Accept Queue (Backlog 전소)
        │
        ▼
Network Subsystem (Connection Drop / RST 패킷 반환)
```

### Thread 상태별 Pool 점유 유형

| Thread 상태 | 원인 | Pool 점유 여부 |
|---|---|---|
| `RUNNABLE` | 실제 연산 수행 중 | 정상 점유 |
| `TIMED_WAITING` | 외부 API / DB 응답 대기 | **비생산적 점유** |
| `WAITING` | `Future.get()`, `Object.wait()` 대기 | **비생산적 점유** |
| `BLOCKED` | `synchronized` 락 획득 대기 | **비생산적 점유 (Lock Contention)** |

---

## 2. 하부 인프라 및 OS 커널 계층의 연동 기전

JVM 내부 스레드 풀 고갈이 리눅스 커널 프로세스 스케줄링, 파일 디스크립터, 네트워크 서브시스템과 맺는 물리적 역학 관계다.

```
+-------------------------------------------------------------------------------------+
| [Container / Pod]  (Cgroup v2 및 OS 커널 커넥션 플로)                                   |
|                                                                                     |
|   +-------------------------------------------------------------------------+       |
|   | [JVM Process]  Tomcat Http Thread Pool / 인바운드 큐                       |       |
|   |                                                                         |       |
|   |  [Inbound TCP SYN]                                                      |       |
|   |       │                                                                 |       |
|   |       ▼                                                                 |       |
|   |  [Accept Queue / Backlog]                                               |       |
|   |       │                                                                 |       |
|   |       ▼                                                                 |       |
|   |  [Worker Threads (Full)] ─────────────────────► [Thread BLOCKED]        |       |
|   |                                                  (DB / 외부 기관 지연)     |       |
|   +──────────────────────────────────────────────────────┬──────────────────+       |
|                                                          │                         |
|                                                          ▼                         |
|                                     [Kernel Context Switching 포화]                 |
|                                       LWP 포화 / pids.max 한계 도달                   |
|                                                          │                         |
|                                                          ▼                         |
|                                     [Network Subsystem Backlog Overflow]           |
|                                       TCP ListenBacklog 전소                        |
|                                       SYN Cookies 발동 / Connection Dropped         |
+-------------------------------------------------------------------------------------+
```

### 2-1. 플랫폼 스레드와 리눅스 LWP 1:1 매핑 및 커널 한계선

Java 플랫폼 스레드는 리눅스 POSIX 스레드 라이브러리(NPTL)를 통해 OS의 **LWP(Light-Weight Process)와 1:1 매핑**되어 커널 스케줄러의 직접 통제를 받는다.

스레드 수가 증가할수록 다음 자원이 함께 증가한다.

| 자원 | 영향 |
|---|---|
| Thread Stack | `-Xss` × Thread 수 만큼 Native Memory 소비 |
| Kernel Scheduler Entry | 커널 스케줄링 자료 구조 항목 추가 |
| Context Switching Cost | CPU 유효 연산 시간 잠식 |

스레드 풀 상한선(`maxThreads`)을 과도하게 증설하면 커널 레벨의 `pids.max`(Cgroup 제한), `fs.file-max`, `nproc` 임계선과 충돌한다. JVM이 커널 한계선에 도달하면:

```
java.lang.OutOfMemoryError: unable to create new native thread
```

---

### 2-2. 커널 Context Switching 포화와 Involuntary CS 지배

**Voluntary vs Involuntary Context Switch 분기**

| 유형 | 발생 시점 | 고갈 단계 |
|---|---|---|
| `voluntary_context_switches` | 스레드가 락/I/O 대기로 **자발적으로** CPU 반납 | 풀 고갈 초기 급증 |
| `involuntary_context_switches` | 커널 스케줄러가 타임 슬라이스 만료로 **강제 축출** | 풀 완전 고갈 후 지배적 증가 |

수백 개의 스레드가 CPU 코어를 잡기 위해 경합하면 CPU 캐시 라인 파괴(Cache Line Invalidation)와 TLB 플러시 오버헤드가 누적되어, **CPU 유효 연산 타임이 커널 스케줄링 자체 비용(`sys CPU`)으로 소진**되는 연산 Saturation이 동반된다.

---

### 2-3. TCP ListenBacklog와 Accept Queue 병목

OS 커널은 외부로부터 들어오는 TCP SYN 패킷을 받아 3-Way Handshake를 마감한 후 완결된 커넥션을 **Accept Queue(Backlog)** 로 이주시킨다.

**풀 고갈 시 네트워크 유출 경로**

```
JVM Worker Thread Pool 완전 고갈
        ↓
애플리케이션 레이어에서 socket.accept() 시스템 콜 호출 불가
        ↓
커널 Accept Queue 적재 한계선 도달
(/proc/sys/net/core/somaxconn 및 listenBacklog)
        ↓
커널이 인바운드 결제 요청 패킷 Drop
또는 RST 패킷 반환
        ↓
인프라 전면 결제 Timeout + Connection Refused 에러 폭발
```

---

## 3. JVM 내부 동시성 서브시스템별 고갈 역학

### 3-1. `ThreadPoolExecutor` 내부 상태 전이와 Queue 설계의 함정

**작업 인입 시 고정 분기문 구조**

```
Task 인입 (execute())
        │
        ▼
현재 스레드 수 < corePoolSize ?
        ├── Yes → 신규 스레드 생성 (즉시 처리)
        └── No
                │
                ▼
        작업 큐(BlockingQueue) 수용 가능 ?
                ├── Yes → 큐에 태스크 적재 (대기)
                └── No
                        │
                        ▼
                현재 스레드 수 < maximumPoolSize ?
                        ├── Yes → 신규 스레드 생성 (임시 확장)
                        └── No
                                │
                                ▼
                        RejectedExecutionHandler 실행
                        (AbortPolicy / CallerRunsPolicy 등)
```

**핀테크 Queue 설계의 두 가지 함정**

| 설계 오류 | 현상 | 결과 |
|---|---|---|
| **무제한 Queue** (`new LinkedBlockingQueue<>()`) | 트래픽 폭증 시 2번 분기에서 태스크가 끝없이 적재 → `maximumPoolSize` 확장 절대 불발 | 결제 트랜잭션이 큐 내부에서 수십 초 대기 후 타임아웃. 태스크 객체 누적으로 Heap Memory Pressure 동반 |
| **과소 Queue** | 큐 포화 즉시 임시 스레드 폭발 생성 → `maximumPoolSize` 도달 | `RejectedExecutionException` 발생, 유입 요청 즉각 드롭 |

---

### 3-2. Tomcat Worker Thread Pool 고갈 기전

Tomcat `Http11NioProtocol` 구조에서 인바운드 커넥션 처리 흐름은 다음과 같다.

```
TCP Request 수신
        │
        ▼
Acceptor 스레드 (커넥션 감지)
        │
        ▼
Poller 스레드 (I/O 이벤트 감지, epoll 기반)
        │
        ▼
Executor (Tomcat Worker Thread Pool, 기본 200개)
        │
        ▼
비즈니스 로직 실행 (결제 연산)
```

**다운스트림 지연의 전사 메커니즘**

결제 아키텍처는 내부 연산보다 **외부 기관(카드사, 은행, 간편결제 API)과의 대외계 I/O 통신이 절대다수**를 차지한다. 외부 기관에 레이턴시 지연이 발생하는 순간:

```
외부 API 응답 지연 발생
        ↓
Tomcat Worker Thread → TIMED_WAITING (SOCKET_READ) 상태 전이
        ↓
해당 스레드가 풀로 반환되지 않고 영구 홀딩
        ↓
초당 수백 건 신규 결제 요청이 가용 스레드를 순식간에 잠식
        ↓
Thread Pool 완전 고갈 고착화
```

---

### 3-3. 중량 Monitor Contention과 BLOCKED 상태 연쇄 폭포

비즈니스 소스코드 내부의 `synchronized` 블록 또는 원장 제어용 `ReentrantLock` 구역에서 발생하는 락 경합이다.

**연쇄 락 폭포 현상**

```
스레드 A: 락 획득 후 외부 I/O 지연으로 수 초간 임계 영역 점유
        ↓
스레드 B, C, D ... (수백 개): 동일 락 획득 위해 일제히 임계 영역 진입 장벽 도달
        ↓
전체 스레드 → BLOCKED (on object monitor) 상태 전이
        ↓
스레드 풀 내 모든 스레드가 연산/I/O 없이
객체 헤더 Mark Word 모니터 동기화 큐에 묶임
        ↓
외형상 Thread Pool Exhaustion과 동일한 서비스 마비
```

---

### 3-4. Virtual Threads 환경의 Thread Pinning과 Carrier Thread Starvation

JDK 21+ 가상 스레드 아키텍처에서 발생하는 신종 고갈 패턴이다.

**정상 가상 스레드 동작**

```
Virtual Thread (I/O 블로킹 진입)
        │
        ▼
Carrier Thread에서 Unmount (분리)
        │
        ▼
Carrier Thread는 다른 Virtual Thread 처리 가능 (재사용)
```

**Thread Pinning 고착화 조건**

가상 스레드 내부 비즈니스 로직에서 다음 조건을 만나면 Carrier Thread에서 분리되지 못하고 **고착화(Pinning)** 된다.

| Pinning 유발 조건 | 설명 |
|---|---|
| `synchronized` 블록 내 I/O 블로킹 | JVM이 모니터 락 소유권을 Carrier Thread 수준에서 관리하므로 분리 불가 |
| JNI 네이티브 메서드 호출 중 I/O 블로킹 | 네이티브 스택 프레임이 Carrier Thread에 고착 |

**Carrier Thread Starvation 전개 경로**

```
대규모 트래픽 + Pinning 조건 코드 실행
        ↓
Virtual Thread들이 Carrier Thread를 반환하지 못하고 고착
        ↓
ForkJoinPool의 모든 Carrier Thread가 물리적 블로킹 상태
        ↓
새 Virtual Thread 생성은 가능하나 실제 실행 불가
        ↓
CPU 사용률 정상 / 응답시간 폭등 / Queue 증가
        ↓
시스템 전체 결빙 (Carrier Thread Starvation)
```

> **2026년 SRE 관점의 전환**: Tomcat 워커 스레드 풀 고갈이 아닌, **하부 ForkJoinPool Carrier Thread의 고착화**를 감시해야 한다.

---

## 4. 기술 발전 및 핀테크 아키텍처 채택 동향 (2026)

### 4-1. 전통적인 플랫폼 스레드 기반 WAS 모델의 한계

Tomcat의 **Thread-per-Request 모델**은 하나의 요청에 하나의 물리적 OS 스레드가 1:1 대응한다. I/O 블로킹 발생 시 스레드가 자원을 잡고 멍하게 대기하는 낭비가 심각했다.

대규모 트래픽 처리를 위해 스레드 풀을 1,000개 이상으로 늘리면:
- 커널 Context Switching 오버헤드 폭증
- 스레드 스택 자원(`-Xss`) 포화로 JVM 전체 붕괴

---

### 4-2. Reactive Programming(WebFlux / Netty)의 과도기적 도입

CPU 코어 수와 매핑되는 극소수의 **이벤트 루프(Event Loop) 스레드**만 가동하고, 모든 I/O 작업을 커널의 `epoll` 인터페이스를 통한 비동기 콜백 구조로 전환했다.

| 장점 | 단점 |
|---|---|
| Thread Pool 고갈 장애 유형 원천 소거 | 소스코드 가독성 극단적 저하 (Callback Hell) |
| 대량 커넥션을 저자원으로 처리 | 에러 발생 시 Stack Trace 기반 트레이싱 사실상 불가 |
| 처리량 대폭 향상 | 금융 결제 코어 시스템 전면 확산에 구조적 한계 |

무결성과 사후 정밀 추적이 생명인 금융 결제 코어 시스템 전체에 전면 확산되는 데는 한계가 있었다.

---

### 4-3. Virtual Threads 전면 표준화와 신종 Pinning 리스크 (2026 현재)

JDK 21~25 LTS에 이르러 금융권 아키텍처는 유저 스페이스에서 **수백만 개를 구동할 수 있는 경량 가상 스레드**를 전면 표준으로 안착시켰다.

**가상 스레드가 플랫폼 스레드 한계를 극복하는 원리**

| 구분 | 플랫폼 스레드 | 가상 스레드 |
|---|---|---|
| OS 매핑 | 1:1 (LWP) | N:M (Carrier Thread 위에서 다중화) |
| I/O 블로킹 시 | OS 스레드 전체 블로킹 | **Carrier Thread Unmount → 다른 Virtual Thread 처리** |
| 동시 생성 규모 | 수백~수천 개 한계 | **수백만 개 가능** |
| Stack 메모리 | `-Xss` × Thread 수 (Native) | **Heap Continuation 객체로 관리** |
| 전통적 풀 고갈 개념 | 발생 가능 | **아키텍처 레벨에서 소멸** |

단, `synchronized` 또는 JNI 코드 내 I/O 블로킹으로 인한 **Thread Pinning → Carrier Thread Starvation** 이라는 신종 고갈 리스크가 존재하므로, 기존 `synchronized` 코드를 `ReentrantLock`으로 전환하는 것이 2026년 현재 금융권 표준 마이그레이션 지침이다.

---

## 5. 관측성(Observability) 및 트러블슈팅 런북

### 5-1. SRE 실시간 탐지 지표 매트릭스 (Prometheus / Micrometer)

#### Tomcat / WAS 계층

| 지표 | 감시 대상 | 핵심 경보 발령선 |
|---|---|---|
| `tomcat_threads_current_threads` | 현재 생성된 WAS 워커 스레드 총량 | 최대 한계치 근접 시 Warning 발령 |
| `tomcat_threads_busy_threads` | 실질 비즈니스 연산/I/O 수행 중 활성 스레드 | `busy_threads / max_threads > 0.95` 조건이 **30초 이상 지속** 시 Thread Pool 고갈 즉각 확정 |

#### JVM 스레드 상태 계층

| 지표 | 감시 대상 | 핵심 경보 발령선 |
|---|---|---|
| `jvm_threads_states_threads{state="blocked"}` | BLOCKED 상태 스레드 수 | 전체 스레드의 **70% 상회** 시 중량 Monitor 락 경합 확정 |
| `jvm_threads_live` | 활성 스레드 총 수 | 비정상적 급증 시 Thread Leak 의심 |
| `jvm_threads_peak` | 최대 스레드 수 기록 | 커널 `nproc` / `pids.max` 한계선과 교차 확인 |

#### Virtual Thread 계층 (2026 표준)

| 지표 | 감시 대상 | 핵심 경보 발령선 |
|---|---|---|
| Pinned Thread Count | Thread Pinning 발생 수 | 지속 증가 시 `synchronized` 코드 검토 |
| Carrier Thread Usage | ForkJoinPool Carrier 스레드 사용량 | 포화 시 Carrier Thread Starvation 임박 |

---

### 5-2. 프로덕션 노이즈 최소화 진단 명령 명세

#### ① Thread Dump 추출 및 스레드 상태 전수 분석

서비스 레이턴시가 파괴되는 즉시 JVM 내부 스레드들의 전체 Stack Trace 지도를 디스크에 인쇄한다.

```bash
# 대상 결제 애플리케이션(PID 505)의 전체 스레드 덤프 스냅샷 주조
jcmd 505 Thread.print > /var/log/jvm_dumps/thread_exhaustion_dump.tdump
```

**결과 판독: 패턴 A — 다운스트림 외부 지연형 고갈**

```
"http-nio-8080-exec-73" #73 daemon prio=5
   java.lang.Thread.State: TIMED_WAITING (on object monitor)
     at java.net.SocketInputStream.socketRead0(Native Method)
     at sun.security.ssl.SSLSocketInputRecord.read(...)
     at com.fintech.payment.client.CardIssuerClient.authorize(CardIssuerClient.java:88)
```

수백 개의 스레드가 `TIMED_WAITING` 상태에서 Stack Trace 최하단 리프 노드가 `socketRead0` 또는 외부 RestTemplate 통신 구역을 지목하고 있다면 **외부 기관 지연이 원인**으로 즉각 확정한다.

**결과 판독: 패턴 B — 내부 Monitor 락 경합형 고갈**

```
"http-nio-8080-exec-105" #105 daemon prio=5
   java.lang.Thread.State: BLOCKED (on object monitor)
     at com.fintech.payment.service.LedgerService.updateBalance(LedgerService.java:42)
     - waiting to lock <0x00000007a1b2c3d4> (a java.lang.Object)
     owned by "http-nio-8080-exec-42" Id=42
```

`BLOCKED (on object monitor)` 상태와 함께 `owned by` 필드에 락 선점 스레드 ID가 명시되어 있다면 **락 고착화 코드**로 원인을 최종 확정한다.

---

#### ② Virtual Thread Pinning 실시간 감지

```bash
# JVM 기동 플래그에 추가하여 롤링 배포
-Djdk.tracePinnedThreads=short
```

Pinning 발생 즉시 표준 에러 로그에 다음 형식으로 Stack Trace 요약이 실시간 인쇄된다.

```
Thread[#42,ForkJoinPool-1-worker-1,5,CarrierThreads]
    com.fintech.payment.service.PaymentService.processSync(PaymentService.java:67) <== monitors:1
    com.fintech.payment.gateway.GatewayHandler.handle(GatewayHandler.java:34)
```

`<== monitors:1` 마커가 붙은 위치가 `synchronized` 락을 보유한 채 I/O 블로킹이 발생하는 **Pinning 원인 코드**다.

---

### 5-3. 장애 복구 런북 (Remediation)

#### 1단계: 서킷 브레이커 동적 강제 개방

스레드 풀 고갈을 유발하는 원인 다운스트림 외부 기관 API 호출 구역의 서킷 브레이커(Resilience4j 등)를 SRE 관리자 콘솔을 통해 강제로 **Open 상태로 전이**시킨다.

인바운드 요청을 진입 즉시 Fail-fast 처리하여 Tomcat 워커 스레드가 외부 I/O를 대기하지 않고 즉각 풀로 환수되도록 강제 격리한다.

#### 2단계: 트래픽 격리 및 Scale-Out

이미 스레드가 완전 포화되어 응답 불능 상태에 빠진 인스턴스는 즉각 로드밸런서 타깃 그룹에서 **격리(Isolate)** 처리하고, 정상 상태의 신규 노드를 수평 확장 투입하여 가용 용량을 수복한다.

#### 3단계: 근본 원인 코드 수정

| 고갈 패턴 | 수정 조치 |
|---|---|
| 외부 API 지연에 의한 스레드 홀딩 | Timeout 설정 강화, 서킷 브레이커 도입, 비동기 전환 |
| `synchronized` 락 고착화 | `synchronized` → `ReentrantLock` 전환 + 락 보유 시간 최소화 |
| 무제한 Queue 설계 | `LinkedBlockingQueue(capacity)` 유한 크기 명시 설정 |
| Virtual Thread Pinning | `synchronized` → `ReentrantLock` 전환 (JDK 24+ 일부 개선) |

---

## 6. 2026 Production Baseline JVM Manifest

2026년 현재 가상 스레드 시대를 맞이한 초고속 결제 MSA 노드에서 스레드 풀 고갈 및 가상 스레드 고착화 장애를 차단하기 위해 규격화된 기동 사양서다.

```bash
java \
  # ── 메모리 및 GC 고속 가동 사양 ─────────────────────────────────────────────────
  -Xms16g -Xmx16g \
  -XX:+AlwaysPreTouch \
  -XX:+UseZGC \
  -XX:+ZGenerational \

  # ── 네이티브 OS 스레드 무분별 증식 방지 격벽 ────────────────────────────────────
  -Xss1m \

  # ── 2026년 가상 스레드 가시성 및 모니터링 강제 조항 ──────────────────────────────
  -Djdk.trackAllThreads=true \
  -Djdk.virtualThreadScheduler.maxPoolSize=32 \

  # ── 가상 스레드 Pinning 현상 실시간 감지 추적 인쇄 ───────────────────────────────
  -Djdk.tracePinnedThreads=short \

  # ── Safepoint 지연 감시 및 스레드 매핑 정보 보존 자동화 ─────────────────────────
  -XX:+UnlockDiagnosticVMOptions \
  -XX:SafepointTimeoutDelay=100 \
  -XX:+SafepointTimeout \
  -Xlog:gc*,safepoint=info:file=/var/log/jvm_dumps/jvm_thread_core.log:time,uptime,pid:filecount=5,filesize=50M \
  -jar fintech-payment-worker.jar
```

### 핵심 플래그 공학적 조항 분석

| 플래그 | 목적 | 공학적 근거 |
|---|---|---|
| `-Xss1m` | 스레드 스택 크기 명시 | 플랫폼 스레드 과생성 시 Native Memory 포화 방지. 가상 스레드는 Heap Continuation으로 관리되므로 직접 영향 없음 |
| `-Djdk.trackAllThreads=true` | 가상 스레드 전수 추적 활성화 | 기본적으로 JVM이 가상 스레드 일부를 추적 제외하는 설정을 오버라이드하여 Thread Dump에 완전 포함 |
| `-Djdk.virtualThreadScheduler.maxPoolSize=32` | Carrier Thread 한계선 설정 | ForkJoinPool Carrier Thread 최대 수를 호스트 CPU 코어 수에 맞춰 명시적 제약. 미설정 시 임계 부하에서 커널 LWP가 무분별하게 과생성되어 Context Switching 효율 파괴 |
| `-Djdk.tracePinnedThreads=short` | Pinning 실시간 탐지 | `synchronized` 또는 JNI 호출로 Carrier Thread를 고착화하는 Pinning 발생 즉시 표준 에러 로그에 Stack Trace 요약 실시간 인쇄 → SRE 팀이 Carrier Thread Starvation 원인 코드를 인프라 단에서 즉각 특정 가능 |
| `-XX:SafepointTimeoutDelay=100` | Safepoint 지연 경보 | 100ms 초과 Safepoint 진입 지연 시 로그 기록 → Thread Pinning이나 장기 루프로 인한 STW 지연 원인 추적 |
| `-XX:+SafepointTimeout` | Safepoint 타임아웃 활성화 | `SafepointTimeoutDelay`와 함께 사용하여 Safepoint 지연 로그 활성화 |

---

## 운영 원칙 요약

> **Thread Pool Exhaustion은 스레드 개수 부족이 아닌, 스레드가 반환되지 못하는 구조적 설계 문제다.**
>
> 2026년 가상 스레드 환경에서는 전통적인 Tomcat 워커 스레드 풀 고갈 개념이 소멸되었으나, `synchronized`/JNI 코드로 인한 **Carrier Thread Starvation**이라는 신종 고갈 패턴을 새롭게 감시해야 한다.

**Thread Pool Exhaustion 분석 접근 순서**

```
Busy Thread 비율 확인 (tomcat_threads_busy_threads / max)
        ↓
Thread State 분포 분석 (BLOCKED / WAITING / TIMED_WAITING 비율)
        ↓
Thread Dump 수집 (jcmd Thread.print)
        ↓
패턴 분류
  ├── TIMED_WAITING + socketRead0 → 외부 API/DB 지연 → 서킷 브레이커 적용
  └── BLOCKED + owned by → Lock 경합 → synchronized → ReentrantLock 전환
        ↓
Virtual Thread 환경: tracePinnedThreads 로그 확인
        ↓
Pinning 원인 코드 특정 → synchronized 제거/전환
        ↓
서킷 브레이커 강제 개방 + 격리 인스턴스 Scale-Out 투입
```

**필수 동시 감시 영역**

- Thread Pool 사용률 (`busy_threads / max_threads`)
- 스레드 상태 분포 (`BLOCKED` / `WAITING` / `TIMED_WAITING`)
- Queue Size 추세 (지속 증가 여부)
- Context Switching 빈도 (`vmstat cs` 항목)
- TCP Backlog / Accept Queue 상태
- Virtual Thread Pinning 발생 수 (`tracePinnedThreads` 로그)
- 외부 의존성 레이턴시 (카드사 / 은행 / 간편결제 API 응답시간)

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*